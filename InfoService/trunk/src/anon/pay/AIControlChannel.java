package anon.pay;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import anon.pay.xml.XMLChallenge;
import anon.pay.xml.XMLErrorMessage;
import anon.pay.xml.XMLPayRequest;
import anon.server.impl.MuxSocket;
import anon.server.impl.SyncControlChannel;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import anon.pay.xml.*;
import anon.util.*;
import java.sql.*;

/**
 * This control channel is used for communication with the AI (AccountingInstance or
 * Abrechnungsinstanz in German) which lives in the first mix.
 * The AI sends a request when it wants a cost confirmation from us. This thread
 * waits for incoming requests and sends the requested confirmations to the AI.
 *
 * @author Bastian Voigt
 * @version 1.0
 */
public class AIControlChannel extends SyncControlChannel
{
	public static final int CHAN_ID = 2;

	private MuxSocket m_MuxSocket;
	private Pay m_Pay;

	public AIControlChannel(Pay pay, MuxSocket muxSocket)
	{
		super(CHAN_ID, true);
		m_Pay = pay;
		m_MuxSocket = muxSocket;
	}

	/**
	 * proccessXMLMessage - this is called when a new request is coming in.
	 *
	 * @param docMsg Document
	 */
	public void proccessXMLMessage(Document docMsg)
	{
		Element elemRoot = docMsg.getDocumentElement();
		String tagName = elemRoot.getTagName();
		try
		{
			if (tagName.equals(XMLPayRequest.XML_ELEMENT_NAME))
			{
				processPayRequest(new XMLPayRequest(elemRoot));
			}
			else if (tagName.equals(XMLErrorMessage.XML_ELEMENT_NAME))
			{
				processErrorMessage(new XMLErrorMessage(elemRoot));
			}
			else if (tagName.equals(XMLChallenge.XML_ELEMENT_NAME))
			{
				processChallenge(new XMLChallenge(elemRoot));
			}
			else
			{
				throw new Exception("AIControlChannel received unknown message '" + tagName + "'");
			}
		}
		catch (Exception ex)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.PAY, ex);
			PayAccountsFile.getInstance().signalAccountError(
				new XMLErrorMessage(XMLErrorMessage.ERR_BAD_REQUEST, ex.getMessage())
				);
		}
	}

	/**
	 * processChallenge
	 *
	 * @param xMLChallenge XMLChallenge
	 */
	private void processChallenge(XMLChallenge chal) throws Exception
	{
		byte[] arbChal = chal.getChallengeForSigning();
		PayAccount acc = PayAccountsFile.getInstance().getActiveAccount();
		if (acc == null)
		{
			throw new Exception("Received Challenge from AI but ActiveAccount not set!");
		}
		byte[] arbSig = acc.getSigningInstance().signBytes(arbChal);
		XMLResponse response = new XMLResponse(arbSig);
		this.sendMessage(XMLUtil.toXMLDocument(response));
	}

	/**
	 * processErrorMessage
	 *
	 * @param msg XMLErrorMessage
	 */
	private void processErrorMessage(XMLErrorMessage msg)
	{
		PayAccountsFile.getInstance().signalAccountError(msg);
	}

	/**
	 * processPayRequest
	 *
	 * @param request XMLPayRequest
	 */
	private void processPayRequest(XMLPayRequest request)
	{
		XMLEasyCC cc = request.getCC();
		if (cc != null)
		{
			try
			{
				PayAccount currentAccount = PayAccountsFile.getInstance().getAccount(cc.getAccountNumber());
				long newBytes = currentAccount.updateCurrentBytes(m_MuxSocket);
				if ( (newBytes + currentAccount.getSpent()) < cc.getTransferredBytes())
				{
					// the AI wants us to sign an unrealistic number of bytes
					// @todo warn the user
					cc.setTransferredBytes(newBytes + currentAccount.getSpent());
				}
				cc.sign(currentAccount.getSigningInstance());
				this.sendMessage(XMLUtil.toXMLDocument(cc));
				currentAccount.addCostConfirmation(cc);
			}
			catch (Exception ex1)
			{
				// the account stated by the AI does not exist or is not currently active
				// @todo handle this exception
			}
		}
		Timestamp t = request.getBalanceTimestamp();
		if (t != null)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.PAY, "AI requested balance");
			PayAccount currentAccount = PayAccountsFile.getInstance().getActiveAccount();
			XMLBalance b = currentAccount.getBalance();
			if ( (b == null) || b.getTimestamp().before(t))
			{
				// balance too old, fetch a new one
				LogHolder.log(LogLevel.DEBUG, LogType.PAY, "Fetching new Balance from BI asynchronously");
				new Thread(new Runnable()
				{
					public void run()
					{
						PayAccount currentAccount = PayAccountsFile.getInstance().getActiveAccount();
						try
						{
							currentAccount.fetchAccountInfo();
							XMLBalance b = currentAccount.getBalance();
							AIControlChannel.this.sendMessage(XMLUtil.toXMLDocument(b));
						}
						catch (Exception ex)
						{
							LogHolder.log(LogLevel.DEBUG, LogType.PAY, ex);
						}
					}
				}).start();
			}
			else
			{
				LogHolder.log(LogLevel.DEBUG, LogType.PAY, "sending balance to AI");
				AIControlChannel.this.sendMessage(XMLUtil.toXMLDocument(b));
			}
		}
		if (request.isAccountRequest())
		{
			PayAccount currentAccount = PayAccountsFile.getInstance().getActiveAccount();
			/** @todo send notification to GUI - especially if currentAccount == null at this point */
			PayAccountsFile.getInstance().signalAccountRequest();
			if (currentAccount != null)
			{
				this.sendMessage(XMLUtil.toXMLDocument(currentAccount.getAccountCertificate()));
			}
		}
	}

}
