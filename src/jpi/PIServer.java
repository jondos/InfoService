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

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import anon.crypto.MyDSAPrivateKey;
import anon.tor.tinytls.TinyTLSServer;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import java.net.InetAddress;

/**
 * Serversocket der Bezahlinstanz.
 *
 * @author Bastian Voigt
 */
public class PIServer implements Runnable
{
	private ServerSocket m_serverSocket;
	private boolean m_typeAI;
	private int m_listenPort;

	/**
	 * Konstruktor f\uFFFDr Serversocket. Es gibt zwei Arten von Serversockets:
	 * ein Socket f\uFFFDr die Verbindungen vom JAP, der andere Socket f\uFFFDr die
	 * Verbindungen von den Abrechnungsinstanzen. Wenn <b>typeai</b> gleich
	 * <b>true</b> wird ein Abrechnungsinstanz-Socket erzeugt.
	 *
	 * @param typeai Socket f\uFFFDr AI- oder JAP-Verbindungen? (true = AI)
	 */
	public PIServer(boolean typeai)
	{
		if (typeai)
		{
			m_listenPort = Configuration.getAIPort();
		}
		else
		{
			m_listenPort = Configuration.getJAPPort();
		}
		m_typeAI = typeai;
	}

	/**
	 * Erzeugt Socket, wartet auf eingehende Verbindungen und startet bei
	 * neuen Verbindungen eine {@link PIConnection} in einem neuen Thread.
	 */
	public void run()
	{
		try
		{
			LogHolder.log(LogLevel.DEBUG, LogType.PAY,
						  "PIServer starting up on port " + m_listenPort + ". TLS is on");
			TinyTLSServer tlssock = new TinyTLSServer(m_listenPort);
			tlssock.setDSSParameters(Configuration.getOwnCertificate(),
									 (MyDSAPrivateKey) Configuration.getPrivateKey());
			m_serverSocket = tlssock;
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, e);
			return;
		}

		Socket acceptedSocket;
		JPIConnection con;
		while (true)
		{
			try
			{
				LogHolder.log(LogLevel.DEBUG, LogType.PAY, "JPIServer: waiting for conn");
				acceptedSocket = m_serverSocket.accept();
				InetAddress remote = acceptedSocket.getInetAddress();
				String strRemote = "unknown";
				if (remote != null)
				{
					strRemote = remote.getHostAddress();
				}
				LogHolder.log(LogLevel.DEBUG, LogType.PAY,
							  "JPIServer: connection from: " + strRemote);

				con = new JPIConnection(acceptedSocket, m_typeAI);
				new Thread(con).start();
			}
			catch (IOException e)
			{
				LogHolder.log(LogLevel.ALERT, LogType.PAY, "Class Server died: " + e.getMessage());
				LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, e);
				return;
			}
		}
	}
}
