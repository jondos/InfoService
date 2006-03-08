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

import java.io.InterruptedIOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

import anon.crypto.MyDSAPrivateKey;
import anon.crypto.tinytls.TinyTLSServer;
import anon.infoservice.ListenerInterface;
import anon.util.ThreadPool;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/**
 * Serversocket der Bezahlinstanz.
 *
 * @author Bastian Voigt
 */
public class PIServer implements Runnable
{
	private ServerSocket m_serverSocket;
	private boolean m_typeAI;
	private ListenerInterface m_listener;
	private ThreadPool m_threadPool;

	/**
	 * Konstruktor f\uFFFDr Serversocket. Es gibt zwei Arten von Serversockets:
	 * ein Socket f\uFFFDr die Verbindungen vom JAP, der andere Socket f\uFFFDr die
	 * Verbindungen von den Abrechnungsinstanzen. Wenn <b>typeai</b> gleich
	 * <b>true</b> wird ein Abrechnungsinstanz-Socket erzeugt.
	 *
	 * @param typeai Socket f\uFFFDr AI- oder JAP-Verbindungen? (true = AI)
	 */
	public PIServer(boolean typeai, ListenerInterface a_listener)
	{
		m_listener = a_listener;
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
						  "PIServer starting up on port " + m_listener.getPort() + ". TLS is on");
			InetAddress addr = null;
			try
			{
				addr = InetAddress.getByName(m_listener.getHost());
			}
			catch (Exception ex2)
			{
			}
			TinyTLSServer tlssock = new TinyTLSServer(m_listener.getPort(), 150, addr);
			tlssock.setDSSParameters(Configuration.getOwnCertificate(),
									 (MyDSAPrivateKey) Configuration.getPrivateKey());
			m_serverSocket = tlssock;
		}
		catch (Exception e)
		{
			if (!m_typeAI)
			{
				LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, "PIServer (BI): Exception in run(): " + e);
			}
			return;
		}

		Socket acceptedSocket;
		JPIConnection con;
		if (!m_typeAI)
		{
			m_threadPool = new ThreadPool("JAP Server Thread", Configuration.getMaxJapConnections());
		}
		else
		{
			m_threadPool = new ThreadPool("AI Server Thread", Configuration.getMaxAiConnections());
		}
		while (true)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.PAY, "JPIServer: waiting for conn");
			try
			{
				acceptedSocket = m_serverSocket.accept();
				acceptedSocket.setSoTimeout(30000);
				try
				{
					InetAddress remote = acceptedSocket.getInetAddress();
					String strRemote = "unknown";
					if (remote != null)
					{
						strRemote = remote.getHostAddress();
					}
					LogHolder.log(LogLevel.DEBUG, LogType.PAY,
								  "JPIServer: connection from: " + strRemote);
				}
				catch (Throwable t)
				{
				}
				con = new JPIConnection(acceptedSocket, m_typeAI);
				m_threadPool.addRequest(con);
			}
			catch (InterruptedIOException exi)
			{
				continue;
			}
			catch (Throwable e)
			{
				try
				{
					LogHolder.log(LogLevel.ALERT, LogType.PAY,
								  "PIServer accept loop exception: " + e.getMessage());
					LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, e);
					Thread.sleep(100); //just to ensure the server will not consume 100% CPU...
				}
				catch (Exception ex)
				{
				}
			}
		}
	}
}
