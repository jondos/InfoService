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
import java.net.Socket;
import anon.AnonChannel;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import anon.ToManyOpenChannelsException;
import anon.NotConnectedToMixException;

final class AnonProxyRequest implements Runnable
{
	InputStream m_InChannel;
	OutputStream m_OutChannel;
	InputStream m_InSocket;
	OutputStream m_OutSocket;
	Socket m_clientSocket;
	Thread m_threadResponse;
	Thread m_threadRequest;
	AnonChannel m_Channel;
	AnonProxy m_Proxy;
	volatile boolean m_bRequestIsAlive;
	int m_iProtocol;

	AnonProxyRequest(AnonProxy proxy, Socket clientSocket)
	{
		try
		{
			m_Proxy = proxy;
			m_clientSocket = clientSocket;
			m_clientSocket.setSoTimeout(1000); //just to ensure that threads will stop
			m_InSocket = clientSocket.getInputStream();
			m_OutSocket = clientSocket.getOutputStream();
			m_threadRequest = new Thread(this, "JAP - AnonProxy Request");
			m_threadRequest.start();
		}
		catch (Exception e)
		{
		}
	}

	public void run()
	{
		m_bRequestIsAlive = true;
		AnonChannel newChannel = null;
		//Check for type
		int firstByte = 0;
		try
		{
			firstByte = m_InSocket.read();
		}
		catch (Throwable t)
		{
			try
			{
				m_clientSocket.close();
			}
			catch (Throwable t1)
			{
			}
			return;
		}
		firstByte &= 0x00FF;
		for (; ; )
		{
			try
			{
				newChannel = null;
				if (firstByte == 4 || firstByte == 5) //SOCKS
				{
					newChannel = m_Proxy.createChannel(AnonChannel.SOCKS);
					m_iProtocol = ProxyListener.PROTOCOL_OTHER;
				}
				else
				{
					newChannel = m_Proxy.createChannel(AnonChannel.HTTP);
					m_iProtocol = ProxyListener.PROTOCOL_WWW;
				}
				break;
			}
			catch (ToManyOpenChannelsException te)
			{
				LogHolder.log(LogLevel.ERR, LogType.NET,
							  "AnonProxyRequest - ToManyOpenChannelsExeption");
				try
				{
					Thread.sleep(1000);
				}
				catch (InterruptedException ex)
				{
				}
			}
			catch (NotConnectedToMixException ec)
			{
				LogHolder.log(LogLevel.ERR, LogType.NET, "AnonProxyRequest - Connection to Mix lost");
				if (!m_Proxy.reconnect())
				{
					return;
				}
			}
			catch (Exception e)
			{
				LogHolder.log(LogLevel.ERR, LogType.NET,
							  "AnonProxyRequest - something was wrong with seting up a new channel Exception: " +
							  e);
				return;
			}

		}
		if (newChannel == null)
		{
			return;
		}
		m_InChannel = newChannel.getInputStream();
		m_OutChannel = newChannel.getOutputStream();
		m_Channel = newChannel;

		m_threadResponse = new Thread(new Response(), "JAP - AnonProxy Response");
		m_threadResponse.start();

		m_Proxy.incNumChannels();

		int len = 0;
		byte[] buff = new byte[1900];
		buff[0]=(byte)firstByte;
		int aktPos=1;
		try
		{
			for (; ; )
			{
				try
				{
					len = Math.min(m_Channel.getOutputBlockSize(), 1900);
					len-=aktPos;
					len = m_InSocket.read(buff, aktPos, len);
					len+=aktPos;
				}
				catch (InterruptedIOException ioe)
				{
					continue;
				}
				if (len <= 0)
				{
					break;
				}
				m_OutChannel.write(buff, 0, len);
				m_Proxy.transferredBytes(len, m_iProtocol);
				aktPos=0;
			}
		}
		catch (Exception e)
		{
		}
		m_bRequestIsAlive = false;
		try
		{
			m_Channel.close();
		}
		catch (Exception e)
		{}
		m_Proxy.decNumChannels();
	}

	final class Response implements Runnable
	{
		Response()
		{
		}

		public void run()
		{
			int len = 0;
			byte[] buff = new byte[2900];
			try
			{
				while ( (len = m_InChannel.read(buff, 0, 1000)) > 0)
				{
					int count = 0;
					for (; ; )
					{
						try
						{
							m_OutSocket.write(buff, 0, len);
							m_OutSocket.flush();
							break;
						}
						catch (InterruptedIOException ioe)
						{
							LogHolder.log(LogLevel.EMERG, LogType.NET,
										  "Should never be here: Timeout in sending to Browser!");
						}
						count++;
						if (count > 3)
						{
							throw new Exception("Could not send to Browser...");
						}
					}
					m_Proxy.transferredBytes(len, m_iProtocol);
				}
			}
			catch (Exception e)
			{}
			try
			{
				m_clientSocket.close();
			}
			catch (Exception e)
			{}
			try
			{
				Thread.sleep(500);
			}
			catch (Exception e)
			{}
			if (m_bRequestIsAlive)
			{
				try
				{
					m_threadRequest.interrupt();
				}
				catch (Exception e)
				{}
			}
		}

	}
}
