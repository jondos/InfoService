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
package proxy;

import java.io.InterruptedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import anon.AnonChannel;
import anon.AnonService;
import anon.AnonServiceFactory;
import anon.ErrorCodes;
import anon.NotConnectedToMixException;
import anon.ToManyOpenChannelsException;
import anon.tor.TorAnonServerDescription;
import jap.JAPModel;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

final public class AnonSocksProxy extends AbstractAnonProxy implements Runnable
{

	private AnonService m_Tor;
	private volatile boolean m_bIsRunning;
	private ServerSocket m_socketListener;
	private Thread m_threadRunLoop;
	private final static boolean USE_TOR = true;

	public AnonSocksProxy(ServerSocket listener)
	{
		m_socketListener = listener;
	}

	public synchronized int start()
	{
		try
		{
			if (USE_TOR)
			{
				m_Tor = AnonServiceFactory.getAnonServiceInstance("TOR");
				m_Tor.initialize(new TorAnonServerDescription(false));
			}
			else
			{
				m_Tor = AnonServiceFactory.getAnonServiceInstance("AN.ON");
			}
			m_threadRunLoop = new Thread(this, "JAP - SocksProxy");
			m_bIsRunning = true;
			m_threadRunLoop.start();
			return ErrorCodes.E_SUCCESS;
		}
		catch (Exception e)
		{
			return ErrorCodes.E_UNKNOWN;
		}
	}

	public synchronized void stop()
	{
		m_bIsRunning = false;
		try
		{
			if (m_threadRunLoop != null)
			{
				m_threadRunLoop.join();
			}
			m_threadRunLoop = null;
			m_Tor.shutdown();
		}
		catch (Exception e)
		{}
	}

	public void run()
	{
		int oldTimeOut = 0;
		LogHolder.log(LogLevel.DEBUG, LogType.NET, "SocksProxy:SocksProxy is running as Thread");

		try
		{
			oldTimeOut = m_socketListener.getSoTimeout();
		}
		catch (Exception e)
		{}
		try
		{
			m_socketListener.setSoTimeout(2000);
		}
		catch (Exception e1)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.NET,
						  "Could not set accept time out: Exception: " + e1.getMessage());
		}
		try
		{
			while (m_bIsRunning)
			{
				Socket socket = null;
				try
				{
					socket = m_socketListener.accept();
				}
				catch (InterruptedIOException e)
				{
					continue;
				}
				try
				{
					socket.setSoTimeout(0); //ensure that socket is blocking!
				}
				catch (SocketException soex)
				{
					socket = null;
					LogHolder.log(LogLevel.ERR, LogType.NET,
								  "JAPSocksProxy.run() Could not set non-Blocking mode for Channel-Socket! Exception: " +
								  soex);
					continue;
				}
				//2001-04-04(HF)
				AnonChannel newChannel = null;
				while (m_bIsRunning)
				{
					try
					{
						newChannel = null;
						newChannel = m_Tor.createChannel(AnonChannel.SOCKS);
						break;
					}
					catch (ToManyOpenChannelsException te)
					{
						LogHolder.log(LogLevel.ERR, LogType.NET,
									  "JAPAnonProxy.run() ToManyOpenChannelsExeption");
						Thread.sleep(1000);
					}
					catch (NotConnectedToMixException ec)
					{
						LogHolder.log(LogLevel.ERR, LogType.NET, "JAPAnonProxy.run() Connection to Mix lost");
						m_bIsRunning = false;
						break;
					}
				}
				if (newChannel != null)
				{
					try
					{
						new AnonProxyRequest(this, socket, newChannel);
					}
					catch (Exception e)
					{
						LogHolder.log(LogLevel.ERR, LogType.NET, "JAPSocksPrxoy.run() Exception: " + e);
					}
				}
			}
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.ERR, LogType.NET, "JAPPSocksServer:ProxyServer.run1() Exception: " + e);
		}
		try
		{
			m_socketListener.setSoTimeout(oldTimeOut);
		}
		catch (Exception e4)
		{}
		LogHolder.log(LogLevel.INFO, LogType.NET, "JAPSocksProxyServer stopped.");
		m_bIsRunning = false;
	}

}
