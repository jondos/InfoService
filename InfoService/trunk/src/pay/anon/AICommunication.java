/*
 Copyright (c) 2000, The JAP-Team
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
package pay.anon;

import java.io.DataInputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import anon.AnonChannel;
import anon.AnonService;
import anon.infoservice.MixCascade;
import anon.infoservice.StatusInfo;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import pay.Pay;
import pay.PayAccount;
import pay.PayAccountsFile;
import payxml.XMLAccountInfo;
import payxml.XMLPayRequest;
import anon.util.XMLUtil;
/**
 * Die gesammt Kommunikation zwischen Pay und AI (welche ja ein Teil der Mix-Kaskade sind). L\uFFFDuft als eigener Thread
 * beim erzeugen wird eine AI Channel ge\uFFFDffnet. Beim starten des Threads wird festgestellt ob die AI bezahlt werden will etc.
 * danach soll in regelm\uFFFDssigem abstand eine EasyCC gesendet werden wenn gefordert (noch nicht implementiert)
 *
 * @author Bastian Voigt
 */

public class AICommunication extends Thread
{

	static int sendCCIntervall = 10000;
	static int sleepIntervall = 3000;
	static int packetSize = 998;

	protected long confirmedPackets;
	protected DataInputStream in;
	protected OutputStream out;
	protected MixCascade anonServer;
	private XMLPayRequest lastRequest;

	protected AnonChannel c;

	private InputStreamReader isr;
	private boolean running;

	public AICommunication(AnonService aService)
	{
		try
		{
			//c = ( (AnonServiceImpl) aService).getAIChannel();
		}
		catch (Exception ex)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.PAY,
						  "AICommunication Konstruktor getAIChannel wirft fehler");
		}
		//in = new DataInputStream(c.getInputStream());
		//out = c.getOutputStream();
	}

	public void setAnonServer(MixCascade anonServer)
	{
		this.anonServer = anonServer;
	}

	public long getLastTransferredBytes()
	{
		/**SK13 check if this is realy true!!*/
		StatusInfo s = anonServer.getCurrentStatus();
		if (s.getMixedPackets() > 0)
		{
			return (s.getMixedPackets() - confirmedPackets) * packetSize;
		}
		return -1;
	}

	public long countLastTransferredBytes()
	{
		/**SK13 check if this is realy true!!*/
		long trans = getLastTransferredBytes();
		StatusInfo s = anonServer.getCurrentStatus();
		if (s.getMixedPackets() > 0)
		{
			confirmedPackets = s.getMixedPackets();
		}
		return trans;
	}

	public void run()
	{

		if (anonServer == null)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.PAY,
						  "AnonServer nicht gesetzt bitte zuerst setAnonService aufrufen");
			return;
		}
		running = true;
		//send("<Hello>Hello AI</Hello>");
		lastRequest = processPayRequest();
		while (running)
		{
			if (anonServer != null && getLastTransferredBytes() > sendCCIntervall)
			{
				processCostConfirmationRequest(lastRequest);
			}
			lastRequest = processPayRequest(); // blockiert dies solange nichts kommt ?
			try
			{
				Thread.sleep(sleepIntervall);
			}
			catch (Exception ex)
			{}
		}
	}

	/**
	 * Sends a cost confirmation to the AI
	 *
	 * @param request XMLPayRequest
	 */
	public void processCostConfirmationRequest(XMLPayRequest request)
	{
		if (request != null && request.costConfirmsNeeded.equals(XMLPayRequest.TRUE))
		{
			;
		}
		//XMLEasyCC cc = Pay.create().addCosts(request.aiName,Pay.create().getUsedAccount(),countLastTransferredBytes());
//		send(getSignedCC(request));
	}

	/*	public String getSignedCC(XMLPayRequest request)
	 {
	  XMLEasyCC cc = Pay.getInstance().addCosts(request.aiName, Pay.getInstance().getUsedAccount(),
		 countLastTransferredBytes());
	  try
	  {
	   JAPSignature sig = new JAPSignature();
	   sig.initSign(Pay.getInstance().getAccount(Pay.getInstance().getUsedAccount()).getPrivateKey());
	   sig.signXmlDoc(cc.getDomDocument());
	   return XMLUtil.XMLDocumentToString(cc.getDomDocument());
	  }
	  catch (java.security.SignatureException e)
	  {
	   e.printStackTrace();
	  }
	  catch (java.lang.Exception e)
	  {
	   e.printStackTrace();
	  }
	  return null;
	 }*/

	public void end()
	{
		processCostConfirmationRequest(lastRequest);
		running = false;
		c.close();
	}

	public void send(String st)
	{
		try
		{
			byte[] buff = st.getBytes();
			out.write(buff, 0, buff.length);
		}
		catch (Exception ex)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.PAY, "AICommunication send ging nicht");
		}
	}

	public byte[] receive()
	{
		byte[] bytes;
		try
		{
			int length = in.readInt();
			bytes = new byte[length];
			in.read(bytes, 0, length);
		}
		catch (Exception ex)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.PAY, "AICommunication receive ging nicht");
			return null;
		}
		return bytes;
	}

	/**
	 * Methode zum senden eines AccountCertifikates und einer balance an die AI und empfangen
	 * einer Antwort von derselben.
	 * noch nicht getestet
	 */
	public XMLPayRequest processPayRequest()
	{
		XMLPayRequest request = null;
		byte[] answer = null;
		try
		{
			answer = receive();
			request = new XMLPayRequest(answer);
		}
		catch (Exception ex)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.PAY,
						  "AICommunication processPayRequest - da kam was falsches an und zwar: -" +
						  answer + "-");
			return null;
		}
		if (request.accounting)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.PAY, "AICommunication: calling AccountsFile...");
			PayAccountsFile accounts = PayAccountsFile.getInstance();
			PayAccount activeAccount = accounts.getActiveAccount();

			send(XMLUtil.XMLDocumentToString(activeAccount.getAccountCertificate().getXmlEncoded()));

			if (request.balanceNeeded.equals(XMLPayRequest.TRUE))
			{
				send(activeAccount.getAccountInfo().getXMLString());
				// hier soll nur aus der lokalen Datei gelesen werden
			}
			if (request.balanceNeeded.equals(XMLPayRequest.NEW))
			{
				XMLAccountInfo info = null;
				try
				{
					info = Pay.getInstance().fetchAccountInfo(activeAccount.getAccountNumber());
				}
				catch (Exception e)
				{}
				send(info.getXMLString());
				// hier soll die BI neu kontaktiert werden.
			}
			if (request.costConfirmsNeeded.equals(XMLPayRequest.TRUE))
			{
				; // hier CostConfirmations verschicken
			}

		}
		return request;
	}

// aus MuxSocket kopiert nach dem Test dieser Klasse soll die Methode von Mux Socket nach hier verschoben werden
	/*
	 public synchronized int sendPayPackets(byte[] xmlBytes) {
	  int bufferLength = (((int)((xmlBytes.length+4) / DATA_SIZE))+1)*DATA_SIZE;
	  byte[] outBuffer = new byte[bufferLength];
	  int len = xmlBytes.length;
	  try{
	   outBuffer[0]=(byte)(len>>24);
	   outBuffer[1]=(byte)(len>>16);
	   outBuffer[2]=(byte)(len>>8);
	   outBuffer[3]=(byte)(len);

	   System.arraycopy(xmlBytes,0,outBuffer,4,xmlBytes.length);
	   ai_cipherOut.encryptAES2(outBuffer);
	   //System.arraycopy(outBuffer,0,m_MixPacketSend,6,xmlZeiger);
	   //sendMixPacket();
	   m_Log.log(LogLevel.DEBUG,LogType.NET,"JAPMuxSocket: sendPayPackets: xmlbytes.length()= "+xmlBytes.length+" bufferLength = "+outBuffer.length);
	   for(int i=0;i<outBuffer.length;i+=DATA_SIZE){
	 m_MixPacketSend[0]=(byte)(0xFF);
	 m_MixPacketSend[1]=(byte)(0xFF);
	 m_MixPacketSend[2]=(byte)(0xFF);
	 m_MixPacketSend[3]=(byte)(0xFF);
	 m_MixPacketSend[4]=(byte)((CHANNEL_OPEN>>8)&(0xFF));
	 m_MixPacketSend[5]=(byte)((CHANNEL_OPEN>>8)&(0xFF));

	 System.arraycopy(outBuffer,i,m_MixPacketSend,6,DATA_SIZE);
	 sendMixPacket();
	 m_Log.log(LogLevel.DEBUG,LogType.NET,"JAPMuxSocket: sendPayPackets: Bytes Verschickt: "+i);
	   }

	  }catch (IndexOutOfBoundsException e){
	 m_Log.log(LogLevel.ERR,LogType.NET,"JAPMuxSocket: sendPayPacketError : IndexOutOfBounds: "+e);
	  }
	  catch (Exception ex){
	   m_Log.log(LogLevel.ERR,LogType.NET,"JAPMuxSocket: sendPayPacketError : "+ex);
	   return 0;
	  }

	  return ErrorCodes.E_SUCCESS;
	 }
	 */
}
