package anon.pay;

import anon.server.impl.SyncControlChannel;
import org.w3c.dom.Document;
import anon.pay.xml.*;
import logging.LogLevel;
import logging.LogType;
import logging.LogHolder;
import anon.util.XMLUtil;
import java.sql.Timestamp;

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

	public AIControlChannel()
	{
		super(CHAN_ID, true);
	}

	/**
	 * proccessXMLMessage - this is called when a new request is coming in.
	 *
	 * @param docMsg Document
	 */
	public void proccessXMLMessage(Document docMsg)
	{
		XMLPayRequest request;
		try
		{
			 request = new XMLPayRequest(docMsg);
		}
		catch (Exception ex)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.PAY, "Error parsing AI request: " + ex.getMessage());
			// report errormessage back to AI..
			XMLErrorMessage err = new XMLErrorMessage(ex.getMessage());
			sendMessage(XMLUtil.toXMLDocument(err));
			return;
		}
		XMLEasyCC cc = request.getCC();
		if (cc != null)
		{
			try
		{
				PayAccount currentAccount = PayAccountsFile.getInstance().getAccount(cc.getAccountNumber());
				long newBytes = currentAccount.updateCurrentBytes();
				if ( (newBytes + currentAccount.getSpent()) < cc.getTransferredBytes())
				{
					// the AI wants us to sign an unrealistic number of bytes
					/** @todo warn the user */
					cc.setTransferredBytes(newBytes + currentAccount.getSpent());
				}
				cc.sign(currentAccount.getSigningInstance());
				this.sendMessage(XMLUtil.toXMLDocument(cc));
				currentAccount.addCostConfirmation(cc);
			}
			catch (Exception ex1)
			{
				// the account stated by the AI does not exist or is not currently active
				/** @todo handle this exception */
			}
		}
		Timestamp t = request.getBalanceTimestamp();
		if (t != null)
		{
			try
			{
				PayAccount currentAccount = PayAccountsFile.getInstance().getActiveAccount();
				XMLBalance b = currentAccount.getBalance();
				if ( (b == null) || b.getTimestamp().before(t))
				{
					Pay.getInstance().fetchAccountInfo(currentAccount.getAccountNumber());
					b = currentAccount.getBalance();
		}
				this.sendMessage(XMLUtil.toXMLDocument(b));
			}
			catch (Exception ex2)
			{
				/** @todo handle this exception */
	}
		}
	}

}
