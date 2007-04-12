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

import anon.client.ChannelTable;
import anon.client.Multiplexer;
import anon.client.PacketCounter;
import anon.client.XmlControlChannel;
import anon.crypto.XMLSignature;
import anon.pay.xml.XMLChallenge;
import anon.pay.xml.XMLEasyCC;
import anon.pay.xml.XMLErrorMessage;
import anon.pay.xml.XMLPayRequest;
import anon.pay.xml.XMLResponse;
import anon.pay.xml.XMLPriceCertificate;
import anon.util.XMLUtil;
import java.util.Vector;
import java.util.Enumeration;
import anon.infoservice.IMutableProxyInterface;
import anon.IServiceContainer;
import anon.infoservice.MixCascade;
import java.util.Hashtable;
import anon.infoservice.MixInfo;

/**
 * This control channel is used for communication with the AI
 * (AccountingInstance or Abrechnungsinstanz in German) which lives in the first
 * mix. The AI sends a request when it wants a cost confirmation from us. This
 * thread waits for incoming requests and sends the requested confirmations to
 * the AI.
 *
 * @author Bastian Voigt, Tobias Bayer
 * @version 1.0
 */
public class AIControlChannel extends XmlControlChannel {

  //codes for AI events that can be fired
  private static final int EVENT_UNREAL = 1;

  /** How many milliseconds to wait before requesting a new account statement */
  private static final long ACCOUNT_UPDATE_INTERVAL = 90000;

  /**
   * Threshold for warning the user of too large number of transferred bytes in
   * cc (0-1)
   */
  private static final double DIFFERENCE_THRESHOLD = 0.0;

  private static long m_totalBytes = 0;

  private long m_lastBalanceUpdate = 0;

  private Vector m_aiListeners = new Vector();

  private IMutableProxyInterface m_proxys;

  private int m_diff = 0;

  private long m_lastDiffBytes = 0;

  private int m_prepaidBytes = 0;

  private PacketCounter m_packetCounter;

  private MixCascade m_connectedCascade;


  public AIControlChannel(Multiplexer a_multiplexer, IMutableProxyInterface a_proxy,
						  PacketCounter a_packetCounter, IServiceContainer a_serviceContainer,
						  MixCascade a_connectedCascade) {
    super(ChannelTable.CONTROL_CHANNEL_ID_PAY, a_multiplexer, a_serviceContainer);
    m_proxys = a_proxy;
    m_packetCounter = a_packetCounter;
	m_connectedCascade = a_connectedCascade;
  }

  public void addAIListener(IAIEventListener a_aiListener) {
    if (!m_aiListeners.contains(a_aiListener)) {
      m_aiListeners.addElement(a_aiListener);
    }
  }

  /**
   * proccessXMLMessage - this is called when a new request is coming in.
   *
   * @param docMsg
   *          Document
   */
  public void processXmlMessage(Document docMsg)
  {
	  Element elemRoot = docMsg.getDocumentElement();
	  //System.out.println(XMLUtil.toString(elemRoot));
	  String tagName = elemRoot.getTagName();
	  try
	  {
		  if (tagName.equals(XMLPayRequest.XML_ELEMENT_NAME))
		  {
			  XMLPayRequest theRequest = new XMLPayRequest(elemRoot);
			  processPayRequest(theRequest);
		  }
		  else if (tagName.equals(XMLErrorMessage.XML_ELEMENT_NAME))
		  {
			  getServiceContainer().keepCurrentService(false); // reconnect to another cascade if possible
			  processErrorMessage(new XMLErrorMessage(elemRoot));
		  }
		  else if (tagName.equals(XMLChallenge.XML_ELEMENT_NAME))
		  {
			  processChallenge(new XMLChallenge(elemRoot));
		  }
		  else if (tagName.equals(XMLEasyCC.getXMLElementName()))
		  {
			  processInitialCC(new XMLEasyCC(elemRoot));
		  }
		  else
		  {
			  LogHolder.log(LogLevel.WARNING, LogType.PAY,
							"Received unknown payment control channel message '" + tagName + "'");
		  }
	  }
	  catch (Exception ex)
	  {
		  LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, ex);
		  getServiceContainer().keepCurrentService(false); // reconnect to another cascade if possible
		  PayAccountsFile.getInstance().signalAccountError(
			  new XMLErrorMessage(XMLErrorMessage.ERR_BAD_REQUEST,
								  ex.getClass().getName() + ": " + ex.getMessage()));
	  }
  }
  /**
   * processChallenge
   *
   * @param xMLChallenge
   *          XMLChallenge
   */
  private void processChallenge(XMLChallenge chal) throws Exception
  {
	  byte[] arbChal = chal.getChallengeForSigning();
	  m_prepaidBytes = chal.getPrepaidBytes();
	  LogHolder.log(LogLevel.NOTICE, LogType.PAY, "Received " + m_prepaidBytes + " prepaid bytes.");

	  PayAccount acc = PayAccountsFile.getInstance().getActiveAccount();
	  if (acc == null)
	  {
		  throw new Exception("Received Challenge from AI but ActiveAccount not set!");
	  }
	  byte[] arbSig = ByteSignature.sign(arbChal, acc.getPrivateKey());
	  XMLResponse response = new XMLResponse(arbSig);
	  this.sendXmlMessage(XMLUtil.toXMLDocument(response));
  }

  /**
   * processErrorMessage
   *
   * @param msg
   *          XMLErrorMessage
   */
  private void processErrorMessage(XMLErrorMessage msg) {
    PayAccountsFile.getInstance().signalAccountError(msg);
  }

  /**
   * process a XMLPayRequest message, which might request a XMLAccountCertificate,
   * request a XMLBalance, or contain a XMLEasyCC which the AI asks the JAP to sign
   *
   * @param request
   *          XMLPayRequest
   */
  private void processPayRequest(XMLPayRequest request) {


	//if requested, send account certificate
	if (request.isAccountRequest())
	{
		sendAccountCert();
	}

	//if sent, process cost confirmation
    XMLEasyCC cc = request.getCC();
    if (cc != null) {
      try {
		  processCcToSign(cc);
      }
      catch (Exception ex1) {
        // the account stated by the AI does not exist or is not currently
        // active
        // @todo handle this exception
        LogHolder.log(LogLevel.ERR, LogType.PAY, ex1);
      }
    }
  }

	/**
	 * processCcToSign: to be called by processPayRequest
	 * (only the initial, old CC is sent as a naked XMLEasyCC,
	 * new CCs to sign are sent inside a XMLPayRequest)
	 *
	 */
	private void processCcToSign(XMLEasyCC cc) throws Exception
	{
	  if (System.currentTimeMillis() - ACCOUNT_UPDATE_INTERVAL > m_lastBalanceUpdate) {
		  // fetch new balance asynchronously
		  // Elmar: so we probably still work with the old PayAccount info this time?
		  LogHolder.log(LogLevel.DEBUG, LogType.PAY, "Fetching new Balance from BI asynchronously");
		  Thread t = new Thread(new Runnable()
		  {
			  public void run()
			  {
				  PayAccount currentAccount = PayAccountsFile.getInstance().getActiveAccount();
				  try
				  {
					  currentAccount.fetchAccountInfo(m_proxys, true);
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
	  //check CC for proper account number
	  if ((currentAccount == null) || (currentAccount.getAccountNumber() != cc.getAccountNumber())) {
		throw new Exception("Received CC with wrong accountnumber");
	  }

	  long newBytes = currentAccount.updateCurrentBytes(m_packetCounter);
	  LogHolder.log(LogLevel.DEBUG, LogType.PAY, "AI requests to sign " + newBytes + " transferred bytes");
	  m_totalBytes = newBytes;
	  // calculate number of bytes transferred with other cascades
	  /*
	   * long transferredWithOtherCascades = 0; XMLAccountInfo info =
	   * currentAccount.getAccountInfo(); if(info!=null) { Enumeration
	   * enu=info.getCCs(); while(enu.hasMoreElements()) {
	   * transferredWithOtherCascades +=
	   * ((XMLEasyCC)enu.nextElement()).getTransferredBytes(); } }
	   */


	  /** @todo implement this so that it works!!!
	  XMLEasyCC myLastCC = currentAccount.getAccountInfo().getCC( (String) cc.getPriceCertHashes().elements().
		  nextElement());
	  long oldSpent = 0;
	  if (myLastCC != null)
	  {
		  oldSpent = myLastCC.getTransferredBytes();
		  LogHolder.log(LogLevel.DEBUG, LogType.PAY, "Transferred bytes of last CC: " + oldSpent);
	  }
	  // check if bytes asked for in CC match bytes transferred
	  MixInfo firstMixInfo = m_connectedCascade.getMixInfo(0);
	  long prepayInterval = firstMixInfo.getPrepaidInterval() * 1024;
	  long expectedBytes = newBytes + oldSpent + prepayInterval;

	  if (expectedBytes < cc.getTransferredBytes())
	  {
		  // If Jap crashed during the last session, CCs may have been lost.
		  long toSign = oldSpent + newBytes;
		  m_diff += cc.getTransferredBytes() - oldSpent - newBytes;
		  double percent = ( (double) m_diff) / ( (double) (toSign - m_lastDiffBytes));
		  LogHolder.log(LogLevel.DEBUG, LogType.PAY, "Percentage of excessive transferred bytes is: " + percent);
		  if (percent > DIFFERENCE_THRESHOLD)
		  {
			  LogHolder.log(LogLevel.WARNING, LogType.PAY,
							"Unrealistic number of bytes to be signed. Spent bytes: " + oldSpent +
							" + new bytes we should sign: " + newBytes + " = " + toSign +
							" < bytes in last CC: " + cc.getTransferredBytes());
			  ;
			  this.fireAIEvent(EVENT_UNREAL, m_diff);
			  m_diff = 0;
			  m_lastDiffBytes = toSign;
		  }
	  }*/


	  //get pricecerts and check against hashes in CC
	  //get price certs from connected cascade
	  /**
	   * We don't even bother to check the hashes in the CC,
	   * the JAP just fills in the ones he knows from the Cascade
	   */
	  cc.setPriceCerts(m_connectedCascade.getPriceCertificateHashes());
	  cc.setPIID(currentAccount.getAccountCertificate().getPIID());
	  String cascadeId = m_connectedCascade.getId();
	  cc.setCascadeID(cascadeId);
	  cc.sign(currentAccount.getPrivateKey());
	  currentAccount.addCostConfirmation(cc);
	  //System.out.println("cc to be sent: "+XMLUtil.toString(XMLUtil.toXMLDocument(cc)));
	this.sendXmlMessage(XMLUtil.toXMLDocument(cc));
  }

  public boolean sendAccountCert()
  {
	  PayAccount currentAccount = PayAccountsFile.getInstance().getActiveAccount();
	  String message = null;
	  Vector priceCerts = m_connectedCascade.getPriceCertificates();
	  Vector mixIDs = m_connectedCascade.getMixIds();
	  String SKI, mixID;

	/*
	  Enumeration hashes = m_connectedCascade.getPriceCertificateHashes().elements();
	  while(hashes.hasMoreElements())
	  {
		  System.out.println((String)hashes.nextElement());
	  }*/


	  // test if this cascade is configured correctly

	  if (priceCerts.size() != mixIDs.size())
	  {
		  message = "Not all Mixes in cascade " + m_connectedCascade.getId() + " have price certs!";
	  }
	  else
	  {
		  for (int i = 0; i < mixIDs.size(); i++)
		  {
			  SKI = ( (XMLPriceCertificate) priceCerts.elementAt(i)).getSubjectKeyIdentifier();
			  mixID = (String) mixIDs.elementAt(i);
			  if (!SKI.equals(mixID))
			  {
				  message = "SKI in price certificate of cascade " + m_connectedCascade.getId() +
					  " differs from Mix ID! SKI:" + SKI + " MixID: " + mixID;
				  break;
			  }
		  }
	  }
	  if (message != null)
	  {
		  LogHolder.log(LogLevel.ERR, LogType.PAY, message);
		  getServiceContainer().keepCurrentService(false); // reconnect to another cascade if possible
		  PayAccountsFile.getInstance().signalAccountError(
			  new XMLErrorMessage(XMLErrorMessage.ERR_INVALID_PRICE_CERTS, message));
		  return false;
	  }


	  if (!PayAccountsFile.getInstance().signalAccountRequest())
	  {
		  return false;
	  }
	  if (currentAccount != null)
	  {
		  sendXmlMessage(XMLUtil.toXMLDocument(currentAccount.getAccountCertificate()));
	  }
	  return true;
  }

  private void fireAIEvent(int a_eventType, long a_additionalInfo) {
    LogHolder.log(LogLevel.DEBUG, LogType.PAY, "Firing AI event");
    Enumeration e = m_aiListeners.elements();
    while (e.hasMoreElements()) {
      if (a_eventType == EVENT_UNREAL) {
        ((IAIEventListener)e.nextElement()).unrealisticBytes(a_additionalInfo);
      }
    }
  }

  public static long getBytes() {
    return m_totalBytes;
  }

	/**
	 * processInitialCC: last step of connecting to a pay cascade:
	 * take last CC as sent by AI as base value for future CCs
	 * Also, send a CC for (bytes in last CC + prepay interval of cascade)
	 * to avoid triggering the cascade's hardlimit by starting to transfer bytes without prepaying
	 *
	 * @param a_cc XMLEasyCC: the last CC that the JAP sent to this Cascade, as returned from the AI
	 */
	private void processInitialCC(XMLEasyCC a_cc) {
		PayAccount currentAccount = PayAccountsFile.getInstance().getActiveAccount();
		String msg = "AI has sent a INVALID last cost confirmation.";
		if (a_cc.verify(currentAccount.getPublicKey()))
		{
			try
			{
				/**
				 * Check if the CC contains the correct price certs
				 */
				//get hashes from CC
				Enumeration priceCertHashesInCc = a_cc.getPriceCertHashes().elements();

				//compare number
				if (a_cc.getNrOfPriceCerts() != m_connectedCascade.getNrOfPriceCerts())
				{
					LogHolder.log(LogLevel.EXCEPTION, LogType.PAY,
								  "number of price certificates in cost confirmation does not match " +
								  "number of price certs in cascade");
					getServiceContainer().keepCurrentService(false); // reconnect to another cascade if possible
					PayAccountsFile.getInstance().signalAccountError(
						new XMLErrorMessage(XMLErrorMessage.ERR_INVALID_PRICE_CERTS,
											"AI sent CC will illegal number of price certs" +
											a_cc.getNrOfPriceCerts()));
					return;
				}

				//compare hashes
				Enumeration inCascade = m_connectedCascade.getPriceCertificateHashes().elements();
				String curCascadeHash;
				String curCcHash;
				int i = 0;
				while (inCascade.hasMoreElements()) //enough to use one enum in condition, since we already checked for equal size
				{
					curCascadeHash = ((String) inCascade.nextElement());
					curCcHash = (String) priceCertHashesInCc.nextElement();
					if (!curCascadeHash.equals(curCcHash))
					{
						String message = "AI sent CC with illegal price cert hash for mix " + (i+1) + "!";
						LogHolder.log(LogLevel.WARNING, LogType.PAY, message);
						getServiceContainer().keepCurrentService(false); // reconnect to another cascade if possible
						PayAccountsFile.getInstance().signalAccountError(
							new XMLErrorMessage(XMLErrorMessage.ERR_INVALID_PRICE_CERTS, message));
						return;
					}
					i++;
				}

				LogHolder.log(LogLevel.DEBUG, LogType.PAY,
							  "AI has sent a valid last cost confirmation. Adding it to account.");
				//no need to verify the price certificates of the last CC, since they might have changed since then
				currentAccount.addCostConfirmation(a_cc);

				//get Cascade's prepay interval
				long prepayInterval = m_connectedCascade.getMixInfo(0).getPrepaidInterval() * 1024 - m_prepaidBytes;
				if (prepayInterval > 0)
				{
					//send CC for up to <last CC + prepay interval> bytes
					a_cc.addTransferredBytes(prepayInterval);
					a_cc.sign(currentAccount.getPrivateKey());
					sendXmlMessage(XMLUtil.toXMLDocument(a_cc));
				}
				return;
			}
			catch (Exception e)
			{
				LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, msg, e);
			}
		}
		else
		{
			LogHolder.log(LogLevel.ERR, LogType.PAY, msg);
		}

		// an error occured; reconnect to another cascade if possible

		getServiceContainer().keepCurrentService(false);
		PayAccountsFile.getInstance().signalAccountError(
			new XMLErrorMessage(XMLErrorMessage.ERR_WRONG_DATA, msg));
	}
}
