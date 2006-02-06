/*
Copyright (c) 2005, The JAP-Team
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
/*
* Created on Nov 30, 2005
*
*/
package anon.mixminion;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Calendar;
import java.util.Random;

import anon.crypto.AsymmetricCryptoKeyPair;
import anon.crypto.JAPCertificate;
import anon.crypto.PKCS12;
import anon.crypto.RSAKeyPair;
import anon.crypto.tinytls.TinyTLS;
import anon.mixminion.mmrdescription.MMRDescription;
import anon.mixminion.message.MixMinionCryptoUtil;
import anon.util.ByteArrayUtil;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/**
* @author Christin Wolfram
*/
public class FirstMMRConnection
{

	//Name of the proxy
	private static String OP_NAME = "JAPClient";
	private TinyTLS m_tinyTLS;
	private MMRDescription m_description;
	private InputStream m_istream;
	private OutputStream m_ostream;
	// supported Mixminion Transport Protocol (MMTP)
	private String m_protocol;
	private boolean m_bIsClosed = true;
	private long m_inittimeout;
	private Mixminion m_Mixminion;
	// blocksize of the message - as we know that 32k at once is too large for the mixminion-server
	private int m_blocksize;

	/**
	 * constructor
	 *
	 * creates a FOR from the description
	 * @param d
	 * description of the mixminion router
	 * @param a_Mixminion
	 * a Mixminion instance
	 */
	public FirstMMRConnection(MMRDescription d, Mixminion a_Mixminion)
	{
		m_inittimeout = 30000;
		m_protocol = "MMTP 0.3";
		// 1k blocksize
		m_blocksize = 1024;
		m_bIsClosed = true;
		m_description = d;
		m_Mixminion = a_Mixminion;
	}

	/**
	 * returns the description of the onion router
	 * @return
	 * OR description
	 */
	public MMRDescription getMMRDescription()
	{
		return m_description;
	}


	/**
	 * check if the connection to the first onion router is closed
	 * @return
	 *
	 */
	public boolean isClosed()
	{
		return m_bIsClosed;
	}


	/**
	 * sends a packet
	 * @param send
	 * 32k message as byte array
	 * @param sendtype
	 * type of Packet - "Junk" or normal "SEND"
	 * @return
	 * success of packet-transmission
	 */
	public boolean send(byte[] send, String sendtype) throws IOException
	{
		try
		{
			/**
			 * Create packet
			 */

			// "SEND" or "JUNK", CRLF = 6 octets
			String s = sendtype + "\r\n";
			byte [] init = new byte [6];
			init = s.getBytes();


			// message M = 32k octets
			int MESSAGE_LEN = 32*1024;
			if (send.length != MESSAGE_LEN)
			{
				return false;
			}
			byte [] message = send;


			// HASH(M|"JUNK") = 20 octets
			byte[] hash = MixMinionCryptoUtil.hash(ByteArrayUtil.conc(message, sendtype.getBytes()));


			/**
			 * Send packet
			 */

			//send initialization
			m_ostream.write(init);

			//send message (with defined blocksize)
			for (int i=0; i<message.length ;i=i+m_blocksize) {
				m_ostream.write(message, i, m_blocksize);
			}

			//send HASH
			m_ostream.write(hash);
			m_ostream.flush();

			LogHolder.log(LogLevel.DEBUG, LogType.MISC,
					  "MMRConnection " + m_description.getName() + " - Send a packet");

			/**
			 * Receive server answer
			 */
			if (sendtype.equals("SEND"))
				return this.receive(message, "RECEIVED");
			else
			{
				if (sendtype.equals("JUNK"))
					return this.receive(message, "RECEIVED JUNK");
				else
					return false;
			}

		}
		catch (InterruptedIOException ex)
		{
			return false;
		}

	}

	/**
	 * sends a Message-packet
	 * @param send
	 * 32k message as byte array
	 * @return
	 * success of packet-transmission
	 */
	public boolean sendMessage(byte[] message) throws IOException
	{
		// "SEND", CRLF = 6 octets
		String sendtype = "SEND";
		return this.send(message, sendtype);

	}


	/**
	 * sends a Junk-packet
	 * @return
	 * success of packet-transmission
	 */
	public boolean sendJunk() throws IOException
	{
		// "JUNK", CRLF = 6 octets
		String sendtype = "JUNK";

		// random junk message M = 32k octets
		int MESSAGE_LEN = 32*1024;
		byte [] message = new byte [MESSAGE_LEN];
		new Random().nextBytes(message);

		return this.send(message, sendtype);

	}

	/**
	 * check answer from server and message-hash
	 * possibel answers: RECEIVE, REJECTED
	 *
	 * @param message
	 * 32k Message
	 * @param type
	 * "RECEIVED JUNK" or "RECEIVED"
	 * @return
	 * success of packet-transmission
	 */
	private boolean receive(byte[] message, String type)
	{
		try
		{
			BufferedInputStream in_buffstream = new BufferedInputStream(m_istream);

			//server answer - 10 octets
			byte[] receivedanswer = new byte[10];
			in_buffstream.read(receivedanswer, 0, 10);
			String str_receivedanswer = new String(receivedanswer, 0, 8);

			//hash - 20 octets
			byte[] receivedhash = new byte[20];
			in_buffstream.read(receivedhash, 0, 20);


			//"RECEIVED\r\n" - packet successfully received by server
			if (str_receivedanswer.equals("RECEIVED"))
			{
				//our hash - HASH(M|"RECEIVED") or HASH(M|"RECEIVED JUNK")
				byte[] messagehash =  new byte[20];
				messagehash = MixMinionCryptoUtil.hash(ByteArrayUtil.conc(message, type.getBytes()));

				//check HASH
				if (ByteArrayUtil.equal(receivedhash, messagehash))
				{
					LogHolder.log(LogLevel.DEBUG, LogType.MISC,
							  "MMRConnection " + m_description.getName() + " - Packet Transmission succeeded. Valid checksum.");
					return true;
				}
				else
				{
					LogHolder.log(LogLevel.DEBUG, LogType.MISC,
							  "MMRConnection " + m_description.getName() + " - Packet Transmission failed. Invalid checksum.");
					System.out.println("Hash nicht korrekt!");
					return false;
				}
			}
			else
			{
				//"REJECTED\r\n" - packet rejected by server
				if (str_receivedanswer.equals("REJECTED"))
				{
					//our hash - HASH(M|"REJECTED")
					byte[] messagehash =  new byte[20];
					messagehash = MixMinionCryptoUtil.hash(ByteArrayUtil.conc(message, "REJECTED".getBytes()));

					//check HASH
					if (ByteArrayUtil.equal(receivedhash, messagehash))
					{
						LogHolder.log(LogLevel.DEBUG, LogType.MISC,
								  "MMRConnection " + m_description.getName() + " - Packet Transmission rejected. Valid checksum.");
						return false;
					}
					else
					{
						LogHolder.log(LogLevel.DEBUG, LogType.MISC,
								  "MMRConnection " + m_description.getName() + " - Packet Transmission rejected. Invalid checksum.");
						return false;
					}
				}
				else
				{
					LogHolder.log(LogLevel.DEBUG, LogType.MISC,
							  "MMRConnection " + m_description.getName() + " - Packet Transmission failed. Invalid server answer.");
						return false;
					}
				}

			}
		catch (IOException e)
		{
			e.printStackTrace();
			return false;
		}
	}

	/**
	 * create client certificate for TinyTLS-connection
	 */
	private void createClientCert()
	{

		try
		{
			RSAKeyPair kp = RSAKeyPair.getInstance(new BigInteger(new byte[]
				{1, 0, 1}), new SecureRandom(), 1024, 100);

			JAPCertificate cert = JAPCertificate.getInstance(OP_NAME, kp, Calendar.getInstance(), 1);

			AsymmetricCryptoKeyPair kp2 = RSAKeyPair.getInstance(new BigInteger(new byte[]
				{1, 0, 1}), new SecureRandom(), 1024, 100);
			PKCS12 pkcs12cert = new PKCS12(OP_NAME + " <identity>", kp2, Calendar.getInstance(), 1);

			//sign cert with pkcs12cert
			JAPCertificate cert1 = cert.sign(pkcs12cert);
			JAPCertificate cert2 = JAPCertificate.getInstance(pkcs12cert.getX509Certificate());

			m_tinyTLS.setClientCertificate(new JAPCertificate[]
                       {cert1, cert2}, kp.getPrivate());
		}
		catch (Exception ex)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.TOR,
						  "Error while creating Certificates. Certificates are not used.");
		}

	}

	/**
	 * connects to the First MixminionRouter (MMR)
	 * @throws Exception
	 */
	public void connect() throws Exception
	{
		FirstMMRConnectionThread forct =
			new FirstMMRConnectionThread(m_description.getAddress(),
												 m_description.getPort(),
												 m_inittimeout,
												 m_Mixminion.getProxy());

//		//test: Verbindung auf localhost
//		FirstMMRConnectionThread forct =
//			new FirstMMRConnectionThread("127.0.0.1",
//												 48099,
//												 m_inittimeout,
//												 m_Mixminion.getProxy());
		
	   m_tinyTLS = forct.getConnection();
		m_tinyTLS.checkRootCertificate(false);
//		m_tinyTLS.setRootKey(m_description.getIdentityKey());

		//create client certificate
		this.createClientCert();

		m_tinyTLS.startHandshake();

		m_ostream = m_tinyTLS.getOutputStream();
		m_istream = m_tinyTLS.getInputStream();
		BufferedInputStream in_buffstream = new BufferedInputStream(m_istream);

		m_tinyTLS.setSoTimeout(5000);

		//MMTP-initialization
		m_ostream.write(m_protocol.concat("\r\n").getBytes());

		//10 octets server answer: supported protocol
		byte[] receivedanswer = new byte[10];
		in_buffstream.read(receivedanswer, 0, 10);
		String str_receivedanswer = new String(receivedanswer, 0, 8);

		//server supports requested mixminion transport protocol (MMTP)
		if (str_receivedanswer.equals(m_protocol))
		{
			LogHolder.log(LogLevel.DEBUG, LogType.MISC,
					  "MMRConnection " + m_description.getName() + " - Protocol supported: " + m_protocol);
			m_bIsClosed = false;
		}
		//server doesn't support protocol --> close connection
		else
		{
			LogHolder.log(LogLevel.DEBUG, LogType.MISC,
					  "MMRConnection " + m_description.getName() + " - Protocol not supported: " + m_protocol);
			this.close();
			m_bIsClosed = true;
		}

	}



	/**
	 * closes the connection to the router
	 *
	 */
	public void close()
	{
		try
		{
			if (!m_bIsClosed)
			{
				m_bIsClosed = true;
				m_tinyTLS.close();

			}
		}
		catch (Throwable t)
		{
		}
	}

}
