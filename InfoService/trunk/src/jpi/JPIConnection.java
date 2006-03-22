/*
 Copyright (c) 2000 - 2004, The JAP-Team
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
package jpi;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.net.SocketException;

import anon.util.XMLUtil;
import jpi.util.HttpServer;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import anon.crypto.tinytls.TinyTLSServerSocket;
import anon.util.TimedOutputStream;

/**
 * Bedient eine Verbindung innerhalb eines Threads. Die Http-Requests werden
 * mit Hilfe der Klasse {@link HttpServer} geparst und die Kommandos an
 * {@link PICommandUser} (bei einer JAP-Verbindung) bzw. an {@link PICommandAI}
 * (bei einer AI-Verbindung} weitergereicht und deren Antworten zur\uFFFDckgesendet.
 *
 * @author Andreas Mueller, Bastian Voigt
 */
public class JPIConnection implements Runnable
{

	private boolean m_bIsAI;
//	private String aiName;

	/** the socket */
	private Socket m_socket;


	private DataInputStream m_inStream;
	private DataOutputStream m_outStream;

	/**
	 * Creates a new JPIConnection over a plaintext-socket
	 * @param socket Socket
	 * @param type AI- oder JAP-Verbindung (true = AI)
	 */
	public JPIConnection(Socket socket, boolean bIsAI) throws IOException
	{
		m_socket = socket;
		m_bIsAI = bIsAI;
	}


	public void run()
	{
		PICommand command = null;
		try
		{
			try
			{
				m_socket.setSoTimeout(30000);
			}
			catch(Exception e)
			{
			}
			if(m_socket instanceof TinyTLSServerSocket)
			{
				((TinyTLSServerSocket)m_socket).startHandshake();
			}
			m_inStream = new DataInputStream(m_socket.getInputStream());
			m_outStream = new DataOutputStream(new TimedOutputStream(m_socket.getOutputStream(),30000));
			HttpServer server = new HttpServer(m_inStream, m_outStream);
			PIRequest request;
			if (!m_bIsAI)
			{
				LogHolder.log(LogLevel.DEBUG, LogType.PAY,"Jap connected");
				command = (PICommand)new PICommandUser();
			}
			else
			{
				LogHolder.log(LogLevel.DEBUG, LogType.PAY,"AI connected");

				/* some ssl-stuff: verify AI-name */
				/*				try
					{
				 X509Certificate[] certs = ( (SSLSocket) m_sslSocket).getSession().getPeerCertificateChain();
					   aiName = certs[0].getSubjectDN().getName();
					}
					catch (Exception e)
					{
					 LogHolder.log(LogLevel.DEBUG, LogType.PAY,"Could not get the peer's name from the SSL certificate");
					 LogHolder.log(LogLevel.EXCEPTION, LogType.PAY,e);
					 //return;
					}*/
				command = (PICommand)new PICommandAI();
			}

			PIAnswer answer;
			while (true)
			{
				try
				{
					request = server.parseRequest();
					if (request == null)
					{
						break;
					}
					if(request.method.equals("GET")&&request.url.equals("/info"))
					{
						server.writeAnswer(200, JPIMain.getHTMLServerInfo());
						break;
					}
					answer = command.next(request);
					LogHolder.log(LogLevel.DEBUG, LogType.MISC, "Now sending answer: "+XMLUtil.toString(XMLUtil.toXMLDocument(answer.getContent())));
					server.writeAnswer(200, answer.getContent());
					if (answer.getType() == PIAnswer.TYPE_CLOSE)
					{
						break;
					}
				}
				catch (Exception e)
				{
					LogHolder.log(LogLevel.EXCEPTION, LogType.PAY,e);
					break;
				}
			}

			try
			{
				LogHolder.log(LogLevel.DEBUG, LogType.MISC, (m_bIsAI?"AI":"Jap")+" disconnected");
				m_socket.shutdownOutput(); // make sure all pending data is sent before closing
				m_socket.close();
			}
			catch (SocketException se)
			{
				LogHolder.log(LogLevel.DEBUG, LogType.PAY,"While closing http connection: " + se.getMessage());
			}

		}
		catch (Exception ie)
		{
			LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, ie);
			try{m_socket.close();}catch(Throwable t){};
		}
	}

}
