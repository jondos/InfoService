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
import anon.crypto.JAPCertificateStore;
import anon.infoservice.ImmutableProxyInterface;
import anon.infoservice.MixCascade;
import anon.server.AnonServiceImpl;
import anon.server.impl.ProxyConnection;
import anon.tor.Tor;
import anon.tor.TorAnonServerDescription;
import jap.JAPModel;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

final public class AnonWebProxy extends AbstractAnonProxy implements Runnable
{
	public static final int E_BIND = -2;
	public final static int E_MIX_PROTOCOL_NOT_SUPPORTED = ErrorCodes.E_PROTOCOL_NOT_SUPPORTED;

	// ootte
	public final static int E_SIGNATURE_CHECK_FIRSTMIX_FAILED = ErrorCodes.E_SIGNATURE_CHECK_FIRSTMIX_FAILED;
	public final static int E_SIGNATURE_CHECK_OTHERMIX_FAILED = ErrorCodes.E_SIGNATURE_CHECK_OTHERMIX_FAILED;

	// ootte
	private final static boolean USE_TOR = true;

	private AnonService m_Anon;
	private AnonService m_Tor;
	private Thread threadRun;
	private volatile boolean m_bIsRunning;
	private ServerSocket m_socketListener;
	private ImmutableProxyInterface m_proxyInterface;

	/**
	 * Stores the MixCascade we are connected to.
	 */
	private MixCascade m_currentMixCascade;

	private boolean m_bAutoReconnect = false;

	/**
	 * Stores, whether we use a forwarded connection (already active, when AnonProxy is created) or
	 * not.
	 */
	private boolean m_forwardedConnection;

	/**
	 * Stores the maximum dummy traffic interval in milliseconds -> we need dummy traffic with at
	 * least that rate. If this value is -1, there is no need for dummy traffic on a forwarded
	 * connection on the server side. Tis value is only meaningful, if m_forwardedConnection is
	 * true.
	 */
	private int m_maxDummyTrafficInterval;

	public AnonWebProxy(ServerSocket listener, ImmutableProxyInterface a_proxyInterface)
	{
		m_socketListener = listener;
		m_proxyInterface=a_proxyInterface;
		//HTTP
		m_Anon = AnonServiceFactory.getAnonServiceInstance("AN.ON");
		m_Anon.setProxy(a_proxyInterface);
		setDummyTraffic( -1);
		m_forwardedConnection = false;
		//SOCKS\uFFFD
	}

	/**
	 * Creates a new AnonProxy with an already active mix connection.
	 *
	 * @param a_listener A ServerSocket, where the AnonProxy listens for new requests (e.g. from a
	 *                   web browser).
	 * @param a_proxyConnection An already open connection to a mix (but not initialized, like keys
	 *                          exchanged, ...).
	 * @param a_maxDummyTrafficInterval The minimum dummy traffic rate the connection needs. The
	 *                                  value is the maximum dummy traffic interval in milliseconds.
	 *                                  Any call of setDummyTraffic(), will respect this maximum
	 *                                  interval value -> bigger values set with setDummyTraffic
	 *                                  (especially -1) result in that maximum dummy traffic
	 *                                  interval value. If this value is -1, there is no need for
	 *                                  dummy traffic on that connection on the server side.
	 */
	public AnonWebProxy(ServerSocket a_listener, ProxyConnection a_proxyConnection,
						int a_maxDummyTrafficInterval)
	{
		m_socketListener = a_listener;
		m_Anon = new AnonServiceImpl(a_proxyConnection); //uups very nasty....
		m_forwardedConnection = true;
		m_bAutoReconnect = false;
		m_maxDummyTrafficInterval = a_maxDummyTrafficInterval;
		setDummyTraffic(a_maxDummyTrafficInterval);
	}

	/**
	 * Sets a new MixCascade.
	 *
	 * @param newMixCascade The new MixCascade we are connected to.
	 */
	public void setMixCascade(MixCascade newMixCascade)
	{
		m_currentMixCascade = newMixCascade;
//		m_AICom.setAnonServer(newMixCascade);
	}

	/**
	 * Changes the dummy traffic interval on the connection to the server. This method respects
	 * dummy traffic restrictions on a forwarded connection. If there is a minimum dummy traffic
	 * rate needed by the server, the dummy traffic interval gets never bigger than that needed
	 * rate on a forwarded connection (especially a interval value of -1 is ignored).
	 *
	 * @param a_interval The interval for dummy traffic on the connection to the server in
	 *                   milliseconds.
	 */
	public void setDummyTraffic(int a_interval)
	{
		if ( (m_forwardedConnection == false) || (m_maxDummyTrafficInterval < 0))
		{
			/* no dummy traffic restrictions */
			( (AnonServiceImpl) m_Anon).setDummyTraffic(a_interval);
		}
		else
		{
			/* there are dummy traffic restrictions */
			if (a_interval >= 0)
			{
				/* take the smaller interval */
				( (AnonServiceImpl) m_Anon).setDummyTraffic(Math.min(a_interval, m_maxDummyTrafficInterval));
			}
			else
			{
				/* we need dummy traffic with a minimum rate -> can't disable dummy traffic */
				( (AnonServiceImpl) m_Anon).setDummyTraffic(m_maxDummyTrafficInterval);
			}
		}
	}

	public void setAutoReConnect(boolean b)
	{
		if (m_forwardedConnection == false)
		{
			/* reconnect isn't supported with forwarded connections */
			m_bAutoReconnect = b;
		}
	}

	public void setMixCertificationCheck(boolean enabled, JAPCertificateStore trustedRoots)
	{
		( (AnonServiceImpl) m_Anon).setMixCertificationAuthorities(trustedRoots);
		( (AnonServiceImpl) m_Anon).setEnableMixCertificationCheck(enabled);
	}

	public int start()
	{
		m_numChannels = 0;
		int ret = m_Anon.initialize(m_currentMixCascade);
		if (ret != ErrorCodes.E_SUCCESS)
		{
			return ret;
		}
		if (USE_TOR)
		{
			m_Tor = AnonServiceFactory.getAnonServiceInstance("TOR");
			m_Tor.setProxy(m_proxyInterface);
			m_Tor.initialize(new TorAnonServerDescription(true));
			( (Tor) m_Tor).setCircuitLength(JAPModel.getTorMinRouteLen(), JAPModel.getTorMaxRouteLen());
		}
		else
		{
			m_Tor = m_Anon;
		}
		threadRun = new Thread(this, "JAP - AnonProxy");
		threadRun.start();
		return ErrorCodes.E_SUCCESS;
	}

	public void stop()
	{
//		m_AICom.end();
		m_Anon.shutdown();
		m_Tor.shutdown();
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
		LogHolder.log(LogLevel.DEBUG, LogType.NET, "AnonProxy: AnonProxy is running as Thread");

		//m_AICom.start();
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
								  "JAPAnonProxy.run() Could not set non-Blocking mode for Channel-Socket! Exception: " +
								  soex);
					continue;
				}
				//Check for type
				int firstByte = 0;
				try
				{
					firstByte = socket.getInputStream().read();
				}
				catch (Throwable t)
				{
					try
					{
						socket.close();
					}
					catch (Throwable t1)
					{
					}
					continue;
				}
				firstByte &= 0x00FF;
				//2001-04-04(HF)
				AnonChannel newChannel = null;
				while (m_bIsRunning)
				{
					try
					{
						newChannel = null;
						if (firstByte == 4 || firstByte == 5) //SOCKS
						{
							newChannel = m_Tor.createChannel(AnonChannel.SOCKS);
						}
						else
						{
							newChannel = m_Anon.createChannel(AnonChannel.HTTP);
						}
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
						if (!m_bAutoReconnect)
						{
							m_bIsRunning = false;
							break;
						}
						while (m_bIsRunning && m_bAutoReconnect)
						{
							LogHolder.log(LogLevel.ERR, LogType.NET,
										  "JAPAnonProxy.run() Try reconnect to Mix");
							int ret = m_Anon.initialize(m_currentMixCascade);
							if (ret == ErrorCodes.E_SUCCESS)
							{
								break;
							}
							Thread.sleep(10000);
						}
					}
					catch (Exception e)
					{
						LogHolder.log(LogLevel.ERR, LogType.NET,
							"JAPAnonPrxoy.run() something was wrong with seting up a new channel Exception: " +
									  e);
						break;
					}
				}
				if (newChannel != null)
				{
					try
					{
						newChannel.getOutputStream().write(firstByte);
						new AnonProxyRequest(this, socket, newChannel);
					}
					catch (Exception e)
					{
						LogHolder.log(LogLevel.ERR, LogType.NET, "JAPAnonPrxoy.run() Exception: " + e);
					}
				}
			}
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.ERR, LogType.NET, "JAPProxyServer:ProxyServer.run1() Exception: " + e);
		}
		try
		{
			m_socketListener.setSoTimeout(oldTimeOut);
		}
		catch (Exception e4)
		{}
		LogHolder.log(LogLevel.INFO, LogType.NET, "JAPAnonProxyServer stopped.");
		m_bIsRunning = false;
	}

}
