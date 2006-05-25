/*
 Copyright (c) 2000 - 2006, The JAP-Team
 All rights reserved.
 Redistribution and use in source and binary forms, with or without modification,
 are permitted provided that the following conditions are met:

 - Redistributions of source code must retain the above copyright notice,
  this list of conditions and the following disclaimer.

 - Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation and/or
  other materials provided with the distribution.

 - Neither the name of the University of Technology Dresden, Germany nor the names of its contributors
  may be used to endorse or promote products derived from this software without specific
  prior written permission.


 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS ``AS IS'' AND ANY EXPRESS
 OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS
 BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE
 */
package anon.pay;

import java.sql.Timestamp;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import anon.crypto.ByteSignature;
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
import anon.infoservice.ImmutableProxyInterface;

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
	/** How many milliseconds to wait before requesting a new account statement */
	private static final long BALANCE_MILLISECONDS = 90000;

	/** Threshold for warning the user of too large number of transferred bytes in cc (0-1)*/
	private static final double DIFFERENCE_THRESHOLD = 0.0;

	private MuxSocket m_MuxSocket;
	private boolean m_bFirstBalance;
	private static long m_totalBytes = 0;

	private long m_lastBalanceUpdate = 0;

	private Vector m_aiListeners = new Vector();

	private ImmutableProxyInterface[] m_proxys;

	private int m_diff = 0;
	private long m_lastDiffBytes = 0;

	public AIControlChannel(MuxSocket muxSocket, ImmutableProxyInterface[] a_proxys)
	{
		super(CHAN_ID, true);
		m_MuxSocket = muxSocket;
		m_bFirstBalance = true;
		m_proxys = a_proxys;
	}

	public void addAIListener(IAIEventListener a_aiListener)
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
			else if(tagName.equals(XMLEasyCC.getXMLElementName()))
			{
				processInitialCC(new XMLEasyCC(elemRoot));
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
		byte[] arbSig = ByteSignature.sign(arbChal, acc.getPrivateKey());
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
		if (request.isAccountRequest())
		{
			sendAccountCert();
		}

		XMLEasyCC cc = request.getCC();
		if (cc != null)
		{
			try
			{
				if (System.currentTimeMillis() - BALANCE_MILLISECONDS > m_lastBalanceUpdate)
				{
					// fetch new balance asynchronously
					LogHolder.log(LogLevel.DEBUG, LogType.PAY, "Fetching new Balance from BI asynchronously");
					Thread t=new Thread(new Runnable()
					{
						public void run()
						{
							PayAccount currentAccount = PayAccountsFile.getInstance().getActiveAccount();
							try
							{
								currentAccount.fetchAccountInfo(m_proxys);
							}
							catch (Exception ex)
							{
								LogHolder.log(LogLevel.DEBUG, LogType.PAY, ex);
							}
						}
					});
					t.setDaemon(true);
					t.start();
					m_lastBalanceUpdate = System.currentTimeMillis();
				}
				PayAccount currentAccount = PayAccountsFile.getInstance().getActiveAccount();
				if ( (currentAccount == null) || (currentAccount.getAccountNumber() != cc.getAccountNumber()))
				{
					throw new Exception("Received CC with wrong accountnumber");
				}

				long newBytes = currentAccount.updateCurrentBytes(m_MuxSocket);
				LogHolder.log(LogLevel.DEBUG, LogType.PAY,
							  "AI requests to sign " + newBytes + " transferred bytes");
				m_totalBytes = newBytes;
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
					m_diff += cc.getTransferredBytes() - oldSpent - newBytes;
					double percent = ( (double) m_diff) / ( (double) (toSign - m_lastDiffBytes));
					LogHolder.log(LogLevel.DEBUG, LogType.PAY,
								  "Percentage of excessive transferred bytes is: " + percent);
					if (percent > DIFFERENCE_THRESHOLD)
					{
						LogHolder.log(LogLevel.WARNING, LogType.PAY,
									  "Unrealistic number of bytes to be signed. Spent bytes: " +
									  oldSpent + " + new bytes we should sign: " + newBytes + " = " + toSign +
									  " < bytes in last CC: " + cc.getTransferredBytes()); ;
						this.fireAIEvent(EVENT_UNREAL, m_diff);
						m_diff = 0;
						m_lastDiffBytes = toSign;
					}
				}
				cc.setPIID(currentAccount.getAccountCertificate().getPIID());
				cc.sign(currentAccount.getPrivateKey());
				currentAccount.addCostConfirmation(cc);
				this.sendXMLMessage(XMLUtil.toXMLDocument(cc));
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
								currentAccount.fetchAccountInfo(m_proxys);
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
	}

	public void sendAccountCert()
	{
		PayAccount currentAccount = PayAccountsFile.getInstance().getActiveAccount();
		PayAccountsFile.getInstance().signalAccountRequest();
		if (currentAccount != null)
		{
			this.sendXMLMessage(XMLUtil.toXMLDocument(currentAccount.getAccountCertificate()));
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

	public static long getBytes()
	{
		return m_totalBytes;
	}

	private void processInitialCC(XMLEasyCC a_cc)
	{
		PayAccount currentAccount = PayAccountsFile.getInstance().getActiveAccount();
		if (a_cc.verify(currentAccount.getPublicKey()))
		{
			try
			{
				LogHolder.log(LogLevel.DEBUG, LogType.PAY, "AI has sent a valid last cost confirmation. Adding it to account.");
				currentAccount.addCostConfirmation(a_cc);
			}
			catch (Exception e)
			{
				LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, "Cannot add Cost confirmation: " + e);
			}
		}
		else
		{
			LogHolder.log(LogLevel.DEBUG, LogType.PAY, "AI has sent a INVALID last cost confirmation. Ignoring.");

		}
	}

}
