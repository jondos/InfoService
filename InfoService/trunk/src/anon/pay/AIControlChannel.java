package anon.pay;

import java.sql.Timestamp;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import anon.pay.xml.XMLBalance;
import anon.pay.xml.XMLChallenge;
import anon.pay.xml.XMLEasyCC;
import anon.pay.xml.XMLErrorMessage;
import anon.pay.xml.XMLPayRequest;
import anon.pay.xml.XMLResponse;
import anon.server.impl.MuxSocket;
import anon.server.impl.SyncControlChannel;
import anon.util.XMLUtil;
import java.util.Vector;
import java.util.Enumeration;

/**
 * This control channel is used for communication with the AI (AccountingInstance or
 * Abrechnungsinstanz in German) which lives in the first mix.
 * The AI sends a request when it wants a cost confirmation from us. This thread
 * waits for incoming requests and sends the requested confirmations to the AI.
 *
 * @author Bastian Voigt, Tobias Bayer
 * @version 1.0
 */
public class AIControlChannel extends SyncControlChannel
{
	public static final int CHAN_ID = 2;
	private static final int EVENT_UNREAL = 1;

	private MuxSocket m_MuxSocket;
	private boolean m_bFirstBalance;

	private Vector m_aiListeners = new Vector();

	public AIControlChannel(MuxSocket muxSocket)
	{
		super(CHAN_ID, true);
		m_MuxSocket = muxSocket;
		m_bFirstBalance = true;
	}

	public void addAIListener(Object a_aiListener)
	{
		if (!m_aiListeners.contains(a_aiListener))
		{
			m_aiListeners.addElement(a_aiListener);
		}
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
			ex.printStackTrace();
			LogHolder.log(LogLevel.DEBUG, LogType.PAY, ex);
			PayAccountsFile.getInstance().signalAccountError(
				new XMLErrorMessage(XMLErrorMessage.ERR_INTERNAL_SERVER_ERROR,
									ex.getClass().getName() + ": " + ex.getMessage())
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
		this.sendXMLMessage(XMLUtil.toXMLDocument(response));
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
				// fetch new balance asynchronously
				LogHolder.log(LogLevel.DEBUG, LogType.PAY, "Fetching new Balance from BI asynchronously");
				new Thread(new Runnable()
				{
					public void run()
					{
						PayAccount currentAccount = PayAccountsFile.getInstance().getActiveAccount();
						try
						{
							currentAccount.fetchAccountInfo();
						}
						catch (Exception ex)
						{
							LogHolder.log(LogLevel.DEBUG, LogType.PAY, ex);
						}
					}
				}).start();

				PayAccount currentAccount = PayAccountsFile.getInstance().getActiveAccount();
				if ( (currentAccount == null) || (currentAccount.getAccountNumber() != cc.getAccountNumber()))
				{
					throw new Exception("Received CC with wrong accountnumber");
				}

				long newBytes = currentAccount.updateCurrentBytes(m_MuxSocket);
				LogHolder.log(LogLevel.DEBUG, LogType.PAY,
							  "AI requests to sign " + newBytes + " transferred bytes");

				// calculate number of bytes transferred with other cascades
				/*				long transferredWithOtherCascades = 0;
				 XMLAccountInfo info = currentAccount.getAccountInfo();
				 if(info!=null)
				 {
				  Enumeration enu=info.getCCs();
				  while(enu.hasMoreElements())
				  {
				   transferredWithOtherCascades +=
					((XMLEasyCC)enu.nextElement()).getTransferredBytes();
				  }
				 }*/
				XMLEasyCC myLastCC = currentAccount.getAccountInfo().getCC(cc.getAIName());
				long oldSpent = 0;
				if (myLastCC != null)
				{
					oldSpent = myLastCC.getTransferredBytes();
					LogHolder.log(LogLevel.DEBUG, LogType.PAY, "Transferred bytes of last CC: " + oldSpent);
				}
				if ( (newBytes + oldSpent) < cc.getTransferredBytes())
				{
					/** If Jap crashed during the last session, CCs may have been lost.*/
					long toSign = oldSpent + newBytes;
					LogHolder.log(LogLevel.WARNING, LogType.PAY,
								  "Unrealistic number of bytes to be signed. Spent bytes: " +
								  oldSpent + " + new bytes we should sign: " + newBytes + " = " + toSign +
								  " < bytes in last CC: " + cc.getTransferredBytes()); ;
					this.fireAIEvent(EVENT_UNREAL, cc.getTransferredBytes()-oldSpent-newBytes);
					//cc.setTransferredBytes(cc.getTransferredBytes()+newBytes);
				}
				cc.setPIID(currentAccount.getAccountCertificate().getPIID());
				cc.sign(currentAccount.getPrivateKey());
				this.sendXMLMessage(XMLUtil.toXMLDocument(cc));
				currentAccount.addCostConfirmation(cc);
			}
			catch (Exception ex1)
			{
				// the account stated by the AI does not exist or is not currently active
				// @todo handle this exception
				LogHolder.log(LogLevel.ERR, LogType.PAY, ex1);
			}
		}
		Timestamp t = request.getBalanceTimestamp();
		if (t != null || m_bFirstBalance == true)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.PAY, "AI requested balance");
			PayAccount currentAccount = PayAccountsFile.getInstance().getActiveAccount();
			if (currentAccount != null)
			{
			XMLBalance b = currentAccount.getBalance();
			if (m_bFirstBalance || (b == null) || ( (b.getTimestamp()).before(t)))
			{
				// balance too old, fetch a new one
				new Thread(new Runnable()
				{
					public void run()
					{
						PayAccount currentAccount = PayAccountsFile.getInstance().getActiveAccount();
						try
						{
							currentAccount.fetchAccountInfo();
							XMLBalance b = currentAccount.getBalance();
							AIControlChannel.this.sendXMLMessage(XMLUtil.toXMLDocument(b));
						}
						catch (Exception ex)
						{
							LogHolder.log(LogLevel.DEBUG, LogType.PAY, ex);
						}
					}
				}, "FetchAccountInfo").start();
			}
			else
			{
				LogHolder.log(LogLevel.DEBUG, LogType.PAY, "sending balance to AI");
				AIControlChannel.this.sendXMLMessage(XMLUtil.toXMLDocument(b));
			}
			m_bFirstBalance = false;
			}
		}
		if (request.isAccountRequest())
		{
			PayAccount currentAccount = PayAccountsFile.getInstance().getActiveAccount();
			/** @todo send notification to GUI - especially if currentAccount == null at this point */
			PayAccountsFile.getInstance().signalAccountRequest();
			if (currentAccount != null)
			{
				this.sendXMLMessage(XMLUtil.toXMLDocument(currentAccount.getAccountCertificate()));
			}
		}
	}

	private void fireAIEvent(int a_eventType, long a_additionalInfo)
	{
		LogHolder.log(LogLevel.DEBUG, LogType.PAY, "Firing AI event");
		Enumeration e = m_aiListeners.elements();
		while (e.hasMoreElements())
		{
			if (a_eventType == EVENT_UNREAL)
			{
				( (IAIEventListener) e.nextElement()).unrealisticBytes(a_additionalInfo);
			}
		}
	}

}
