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
			LogHolder.log(LogLevel.DEBUG, LogType.PAY, "Error parsing AI request: "+ex.getMessage());
			// report errormessage back to AI..
			XMLErrorMessage err = new XMLErrorMessage(ex.getMessage());
			sendMessage(XMLUtil.toXMLDocument(err));
			return;
		}
		XMLEasyCC cc = request.getCC();
		if(cc!=null)
		{
			// todo process CC
		}
		Timestamp t = request.getBalanceTimestamp();
		if(t!=null)
		{
			Pay.getInstance();
		}
	}
}
