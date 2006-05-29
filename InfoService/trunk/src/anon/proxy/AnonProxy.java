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
package anon.proxy;

import java.io.InterruptedIOException;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Vector;

import anon.AnonChannel;
import anon.AnonService;
import anon.AnonServiceEventListener;
import anon.AnonServiceFactory;
import anon.ErrorCodes;
import anon.NotConnectedToMixException;
import anon.client.AnonClient;
import anon.infoservice.ImmutableProxyInterface;
import anon.infoservice.MixCascade;
import anon.mixminion.MixminionServiceDescription;
import anon.shared.ProxyConnection;
import anon.tor.TorAnonServerDescription;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/**
 * This calls implements a proxy one can use for convienient access to the
 * provided anonymous communication primitives. Below you find an example which
 * creates a proxy which uses the AN.ON mix cascade for anonymous web surfing.
 * {@code AnonProxy theProxy=new AnonProxy(serverSocket,null);
 * theProxy.setMixCascade(new MixCascade(null, null, hostNameOfMixCascade,
 * portNumberOfMixCascade)); theProxy.start(); }
 */

final public class AnonProxy implements Runnable, AnonServiceEventListener
{
	public static final int E_BIND = -2;

	public final static int E_MIX_PROTOCOL_NOT_SUPPORTED = ErrorCodes.E_PROTOCOL_NOT_SUPPORTED;

	// ootte
	public final static int E_SIGNATURE_CHECK_FIRSTMIX_FAILED = ErrorCodes.E_SIGNATURE_CHECK_FIRSTMIX_FAILED;

	public final static int E_SIGNATURE_CHECK_OTHERMIX_FAILED = ErrorCodes.E_SIGNATURE_CHECK_OTHERMIX_FAILED;

	private static final int RECONNECT_INTERVAL = 5000;

	private AnonService m_Anon;

	private AnonService m_Tor;

	private AnonService m_Mixminion;

	private Vector m_anonServiceListener;

	private Thread threadRun;

	private volatile boolean m_bIsRunning;

	private ServerSocket m_socketListener;

	private ImmutableProxyInterface m_proxyInterface;

	/**
	 * Stores the MixCascade we are connected to.
	 */
	private MixCascade m_currentMixCascade;

	/**
	 * Stores the Tor params.
	 */
	private TorAnonServerDescription m_currentTorParams;

	/**
	 * Stores the Mixminion params.
	 */
	private MixminionServiceDescription m_currentMixminionParams;

	private boolean m_bAutoReconnect = false;

	/**
	 * Stores, whether we use a forwarded connection (already active, when
	 * AnonProxy is created) or not.
	 */
	private boolean m_forwardedConnection;

	/**
	 * Stores the maximum dummy traffic interval in milliseconds -> we need dummy
	 * traffic with at least that rate. If this value is -1, there is no need for
	 * dummy traffic on a forwarded connection on the server side. Tis value is
	 * only meaningful, if m_forwardedConnection is true.
	 */
	private int m_maxDummyTrafficInterval;

	/**
	 * Creates a new AnonProxy. This proxy uses as default only the AN.ON service.
	 * If you also want to use TOR and Mixminion you have to enable them by
	 * calling setTorParams() and setMixmininoParams().
	 *
	 * @see setTorParams()
	 * @see setMixminionParams()
	 *
	 * @param a_listener
	 *          A ServerSocket, where the AnonProxy listens for new requests (e.g.
	 *          from a web browser).
	 * @param a_proxyInterface
	 *          describes a proxy the AnonProxy should use to establish
	 *          connections to the anon servers (e.g. if you are behind some
	 *          firewall etc.)
	 */
	public AnonProxy(ServerSocket listener, ImmutableProxyInterface a_proxyInterface)
	{
		m_socketListener = listener;
		m_proxyInterface = a_proxyInterface;
		// HTTP
		m_Anon = AnonServiceFactory.getAnonServiceInstance(AnonServiceFactory.SERVICE_ANON);
		m_Anon.setProxy(a_proxyInterface);
		setDummyTraffic( -1);
		m_forwardedConnection = false;
		m_bAutoReconnect = false;
		m_anonServiceListener = new Vector();
		m_Anon.addEventListener(this);
		// SOCKS\uFFFD
	}

	/**
	 * Creates a new AnonProxy with an already active mix connection.
	 *
	 * @param a_listener
	 *          A ServerSocket, where the AnonProxy listens for new requests (e.g.
	 *          from a web browser).
	 * @param a_proxyConnection
	 *          An already open connection to a mix (but not initialized, like
	 *          keys exchanged, ...).
	 * @param a_maxDummyTrafficInterval
	 *          The minimum dummy traffic rate the connection needs. The value is
	 *          the maximum dummy traffic interval in milliseconds. Any call of
	 *          setDummyTraffic(), will respect this maximum interval value ->
	 *          bigger values set with setDummyTraffic (especially -1) result in
	 *          that maximum dummy traffic interval value. If this value is -1,
	 *          there is no need for dummy traffic on that connection on the
	 *          server side.
	 */
	public AnonProxy(ServerSocket a_listener, ProxyConnection a_proxyConnection,
					 int a_maxDummyTrafficInterval)
	{
		m_socketListener = a_listener;
		m_Anon = new AnonClient(a_proxyConnection.getSocket()); // uups very nasty....
		m_forwardedConnection = true;
		m_bAutoReconnect = false;
		m_maxDummyTrafficInterval = a_maxDummyTrafficInterval;
		setDummyTraffic(a_maxDummyTrafficInterval);
		m_anonServiceListener = new Vector();
		m_Anon.addEventListener(this);
	}

	/**
	 * Sets a new MixCascade.
	 *
	 * @param newMixCascade
	 *          The new MixCascade we are connected to.
	 */
	public void setMixCascade(MixCascade newMixCascade)
	{
		m_currentMixCascade = newMixCascade;
		// m_AICom.setAnonServer(newMixCascade);
	}

	/** Retruns the current Mix cascade */
	public MixCascade getMixCascade()
	{
		return m_currentMixCascade;
	}

	/**
	 * Sets the parameter for TOR (anonymous SOCKS). If NULL TOR proxy is
	 * disabled.
	 *
	 * @param newTorParams
	 *          The new parameters for TOR.
	 * @see TorAnonServerDescription
	 */
	public void setTorParams(TorAnonServerDescription newTorParams)
	{
		m_currentTorParams = newTorParams;
	}

	public TorAnonServerDescription getTorParams()
	{
		return m_currentTorParams;
	}

	/**
	 * Sets the parameter for Mixminion (anonymous remailer). If NULL Mixminion
	 * proxy is disabled.
	 *
	 * @param newMixminionParams
	 *          The new parameters for Mixminion. If NULL the Mixminion proxy is
	 *          disabled.
	 * @see MixminionServiceDescription
	 */
	public void setMixminionParams(MixminionServiceDescription newMixminionParams)
	{
		m_currentMixminionParams = newMixminionParams;
	}

	public MixminionServiceDescription getMixminionParams()
	{
		return m_currentMixminionParams;
	}

	/**
	 * Changes the dummy traffic interval on the connection to the server. This
	 * method respects dummy traffic restrictions on a forwarded connection. If
	 * there is a minimum dummy traffic rate needed by the server, the dummy
	 * traffic interval gets never bigger than that needed rate on a forwarded
	 * connection (especially a interval value of -1 is ignored).
	 *
	 * @param a_interval
	 *          The interval for dummy traffic on the connection to the server in
	 *          milliseconds.
	 */
	public void setDummyTraffic(int a_interval)
	{
		if ( (!m_forwardedConnection) || (m_maxDummyTrafficInterval < 0))
		{
			/* no dummy traffic restrictions */
			( (AnonClient) m_Anon).setDummyTraffic(a_interval);
		}
		else
		{
			/* there are dummy traffic restrictions */
			if (a_interval >= 0)
			{
				/* take the smaller interval */
				( (AnonClient) m_Anon).setDummyTraffic(Math.min(a_interval, m_maxDummyTrafficInterval));
			}
			else
			{
				/*
				 * we need dummy traffic with a minimum rate -> can't disable dummy
				 * traffic
				 */
				( (AnonClient) m_Anon).setDummyTraffic(m_maxDummyTrafficInterval);
			}
		}
	}

	public void setAutoReConnect(boolean b)
	{
		if (!m_forwardedConnection)
		{
			/* reconnect isn't supported with forwarded connections */
			m_bAutoReconnect = b;
		}
	}

	public int start(boolean a_bRetryOnError)
	{
		boolean m_bConnectionError = false;
		m_numChannels = 0;
		LogHolder.log(LogLevel.DEBUG, LogType.NET, "Try to initialize AN.ON");
		if (m_Anon == null)
		{
			LogHolder.log(LogLevel.EMERG, LogType.NET,
						  " m_Anon is NULL - should never ever happen!");
			return ErrorCodes.E_INVALID_SERVICE;
		}
		int ret = m_Anon.initialize(m_currentMixCascade);
		if (ret != ErrorCodes.E_SUCCESS)
		{
			if (!a_bRetryOnError || ret == E_SIGNATURE_CHECK_FIRSTMIX_FAILED ||
				ret == E_SIGNATURE_CHECK_OTHERMIX_FAILED)
			{
				return ret;
			}
			else
			{
				m_bConnectionError = true;
			}
		}
		LogHolder.log(LogLevel.DEBUG, LogType.NET, "AN.ON initialized");
		if (m_currentTorParams != null)
		{
			m_Tor = AnonServiceFactory.getAnonServiceInstance(AnonServiceFactory.SERVICE_TOR);
			m_Tor.setProxy(m_proxyInterface);
			m_Tor.initialize(m_currentTorParams);
			LogHolder.log(LogLevel.DEBUG, LogType.NET, "Tor initialized");
		}
		if (m_currentMixminionParams != null)
		{
			m_Mixminion = AnonServiceFactory.getAnonServiceInstance(AnonServiceFactory.SERVICE_MIXMINION);
			m_Mixminion.setProxy(m_proxyInterface);
			m_Mixminion.initialize(m_currentMixminionParams);
		}
		threadRun = new Thread(this, "JAP - AnonProxy");
		threadRun.setDaemon(true);
		m_bIsRunning = true;
		threadRun.start();

		if (m_bConnectionError)
		{
			connectionError();
			return ErrorCodes.E_CONNECT;
		}
		return ErrorCodes.E_SUCCESS;
	}

	public void stop()
	{
		m_Anon.shutdown();
		if (m_Tor != null)
		{
			m_Tor.shutdown();
		}
		if (m_Mixminion != null)
		{
			m_Mixminion.shutdown();
		}
		m_bIsRunning = false;

		try
		{
			threadRun.join();
		}
		catch (Exception e)
		{
		}
		m_Tor = null;
		m_Mixminion = null;
	}

	public void run()
	{
		m_bIsRunning = true;
		int oldTimeOut = 0;
		LogHolder.log(LogLevel.DEBUG, LogType.NET, "AnonProxy: AnonProxy is running as Thread");

		try
		{
			oldTimeOut = m_socketListener.getSoTimeout();
		}
		catch (Exception e)
		{
		}
		try
		{
			m_socketListener.setSoTimeout(2000);
		}
		catch (Exception e1)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.NET, "Could not set accept time out!" , e1);
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
					socket.setSoTimeout(0); // ensure that socket is blocking!
				}
				catch (SocketException soex)
				{
					socket = null;
					LogHolder.log(LogLevel.ERR, LogType.NET,
								  "Could not set non-Blocking mode for Channel-Socket!", soex);
					continue;
				}
				// 2001-04-04(HF)
				try
				{
					new AnonProxyRequest(this, socket);
				}
				catch (Exception e)
				{
					LogHolder.log(LogLevel.ERR, LogType.NET, e);
				}
			}
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.ERR, LogType.NET, e);
		}
		try
		{
			m_socketListener.setSoTimeout(oldTimeOut);
		}
		catch (Exception e4)
		{
		}
		LogHolder.log(LogLevel.INFO, LogType.NET, "JAPAnonProxyServer stopped.");
		m_bIsRunning = false;
	}

	AnonChannel createChannel(int type) throws NotConnectedToMixException, Exception
	{
		if (type == AnonChannel.SOCKS)
		{
			if (m_Tor != null)
			{
				return m_Tor.createChannel(AnonChannel.SOCKS);
			}
		}
		else if (type == AnonChannel.HTTP)
		{
			return m_Anon.createChannel(AnonChannel.HTTP);
		}
		else if (type == AnonChannel.SMTP)
		{
			if (m_Mixminion != null)
			{
				return m_Mixminion.createChannel(AnonChannel.SMTP);
			}
		}
		return null;
	}

	synchronized boolean reconnect()
	{
		if (m_Anon.isConnected())
		{
			return true;
		}
		if (!m_bAutoReconnect)
		{
			m_bIsRunning = false;
			return false;
		}
		while (m_bIsRunning && m_bAutoReconnect)
		{
			LogHolder.log(LogLevel.ERR, LogType.NET, "Try reconnect to Mix");
			int ret = m_Anon.initialize(m_currentMixCascade);
			if (ret == ErrorCodes.E_SUCCESS)
			{
				return true;
			}
			try
			{
				Thread.sleep(RECONNECT_INTERVAL);
			}
			catch (InterruptedException ex)
			{
			}
		}
		return false;
	}

	protected IProxyListener m_ProxyListener;

	protected volatile int m_numChannels = 0;

	public void setProxyListener(IProxyListener l)
	{
		m_ProxyListener = l;
	}

	protected synchronized void decNumChannels()
	{
		m_numChannels--;
		if (m_ProxyListener != null)
		{
			m_ProxyListener.channelsChanged(m_numChannels);
		}
	}

	protected synchronized void incNumChannels()
	{
		m_numChannels++;
		if (m_ProxyListener != null)
		{
			m_ProxyListener.channelsChanged(m_numChannels);
		}
	}

	protected synchronized void transferredBytes(long bytes, int protocolType)
	{
		if (m_ProxyListener != null)
		{
			m_ProxyListener.transferedBytes(bytes, protocolType);
		}
	}

	private void fireConnectionEstablished()
	{
		Enumeration e = m_anonServiceListener.elements();
		while (e.hasMoreElements())
		{
			( (AnonServiceEventListener) e.nextElement()).connectionEstablished();
		}
	}

	private void fireConnectionError()
	{
		Enumeration e = m_anonServiceListener.elements();
		while (e.hasMoreElements())
		{
			( (AnonServiceEventListener) e.nextElement()).connectionError();
		}
	}

	public void connectionEstablished()
	{
		LogHolder.log(LogLevel.DEBUG, LogType.NET, "AnonProxy received connectionEstablished.");
		fireConnectionEstablished();
	}

	public void connectionError()
	{
		LogHolder.log(LogLevel.ERR, LogType.NET, "AnonProxy received connectionError");
		this.fireConnectionError();
		new Thread()
		{
			public void run()
			{
				reconnect();
			}
		}.start();
	}

	public synchronized void addEventListener(AnonServiceEventListener l)
	{
		Enumeration e = m_anonServiceListener.elements();
		while (e.hasMoreElements())
		{
			if (l.equals(e.nextElement()))
			{
				return;
			}
		}
		m_anonServiceListener.addElement(l);
	}

	public synchronized void removeEventListener(AnonServiceEventListener l)
	{
		m_anonServiceListener.removeElement(l);
	}

	public boolean isConnected()
	{
		return m_Anon != null && m_Anon.isConnected();
	}

	public AnonClient getAnonService()
	{
		return (AnonClient) m_Anon;
	}

	public void packetMixed(long a_totalBytes)
	{
		Enumeration e = m_anonServiceListener.elements();
		while (e.hasMoreElements())
		{
			( (AnonServiceEventListener) e.nextElement()).packetMixed(a_totalBytes);
		}

	}

}
