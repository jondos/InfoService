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

import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import anon.AnonChannel;
import anon.AnonService;
import anon.AnonServiceFactory;
import anon.ErrorCodes;
import anon.NotConnectedToMixException;
import anon.ToManyOpenChannelsException;
import anon.crypto.JAPCertificateStore;
import anon.infoservice.MixCascade;
import anon.pay.AICommunication;
import anon.server.AnonServiceImpl;
import jap.JAPConstants;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import pay.Pay;
import tor.Circuit;
import tor.OnionRouter;
import tor.ordescription.ORDescription;
import tor.ordescription.ORList;
import logging.*;
import java.util.Vector;

final public class SocksProxy implements Runnable, IAnonProxy
{


	private Thread threadRun;
	private volatile boolean m_bIsRunning;
	private ServerSocket m_socketListener;
	private Circuit m_Circuit;

	public SocksProxy(ServerSocket listener)
	{
		m_socketListener = listener;
	}



	public int start()
	{
		try{
		ORList orl = new ORList();
		orl.updateList("moria.seul.org",9031);
		Vector ors=orl.getList();
		Vector orsToUse=new Vector(1);
		orsToUse.addElement("e");
		orsToUse.addElement("e");
		for(int i=0;i<ors.size();i++)
		{
			ORDescription od=(ORDescription)ors.elementAt(i);
			if(od.getName().equalsIgnoreCase("jap"))
				orsToUse.setElementAt(od,0);
			else if(od.getName().equalsIgnoreCase("moria1"))
				orsToUse.setElementAt(od,1);
		}
		LogHolder.log(LogLevel.DEBUG,LogType.MISC,"[TOR] Creating new circuit");
		m_Circuit = new Circuit(10,orsToUse);
		LogHolder.log(LogLevel.DEBUG,LogType.MISC,"[TOR] Connecting");
		m_Circuit.connect();

		threadRun = new Thread(this, "JAP - SocksProxy");
		threadRun.start();
		return E_SUCCESS;
		}
		catch(Exception e)
		{
			return ErrorCodes.E_UNKNOWN;
		}
	}

	public void stop()
	{
		m_bIsRunning = false;
		try
		{
			threadRun.join();
		}
		catch (Exception e)
		{}
	}

	public void run()
	{
		m_bIsRunning = true;
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
						newChannel = m_Circuit.createChannel(AnonChannel.SOCKS);
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
						new AnonProxyRequest(this,socket, newChannel);
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

	public synchronized void incNumChannels()
	{
	}

	public synchronized void decNumChannels()
	{
	}

	public synchronized void transferredBytes(int bytes)
	{
	}


}
