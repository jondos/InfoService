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
import anon.infoservice.AbstractMixCascadeContainer;
import anon.mixminion.MixminionServiceDescription;
import anon.shared.ProxyConnection;
import anon.tor.TorAnonServerDescription;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import anon.AnonServerDescription;
import anon.pay.IAIEventListener;
import anon.infoservice.IMutableProxyInterface;

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

	private ServerSocket m_socketListener;

	private ImmutableProxyInterface m_proxyInterface;

	private final Object THREAD_SYNC = new Object();
	private final Object SHUTDOWN_SYNC = new Object();

	/**
	 * Stores the MixCascade we are connected to.
	 */
	private AbstractMixCascadeContainer m_currentMixCascade = new DummyMixCascadeContainer();

	/**
	 * Stores the Tor params.
	 */
	private TorAnonServerDescription m_currentTorParams;

	/**
	 * Stores the Mixminion params.
	 */
	private MixminionServiceDescription m_currentMixminionParams;

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
	public AnonProxy(ServerSocket listener, ImmutableProxyInterface a_proxyInterface,
					 IMutableProxyInterface a_paymentProxyInterface)
	{
		if (listener == null)
		{
			throw new IllegalArgumentException("Socket listener is null!");
		}

		m_socketListener = listener;
		m_proxyInterface = a_proxyInterface;
		// HTTP
		m_Anon = AnonServiceFactory.getAnonServiceInstance(AnonServiceFactory.SERVICE_ANON);
		m_Anon.setProxy(a_proxyInterface);
		((AnonClient)m_Anon).setPaymentProxy(a_paymentProxyInterface);
		setDummyTraffic( -1);
		m_forwardedConnection = false;
		m_anonServiceListener = new Vector();
		m_Anon.removeEventListeners();
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
		if (a_listener == null)
		{
			throw new IllegalArgumentException("Socket listener is null!");
		}
		m_socketListener = a_listener;
		m_Anon = new AnonClient(a_proxyConnection.getSocket()); // uups very nasty....
		m_forwardedConnection = true;
		m_maxDummyTrafficInterval = a_maxDummyTrafficInterval;
		setDummyTraffic(a_maxDummyTrafficInterval);
		m_anonServiceListener = new Vector();
		m_Anon.removeEventListeners();
		m_Anon.addEventListener(this);
	}

	/**
	 * Sets a new MixCascade.
	 *
	 * @param newMixCascade
	 *          The new MixCascade we are connected to.
	 */
	public void setMixCascade(AbstractMixCascadeContainer newMixCascade)
	{
		if (newMixCascade == null)
		{
			m_currentMixCascade = new DummyMixCascadeContainer();
		}
		else
		{
			m_currentMixCascade = new EncapsulatedMixCascadeContainer(newMixCascade);
		}
		// m_AICom.setAnonServer(newMixCascade);
	}

	/** Returns the current Mix cascade */
	public MixCascade getMixCascade()
	{
		try
		{
			return m_currentMixCascade.getCurrentMixCascade();
		}
		catch (NullPointerException a_e)
		{
			return null;
		}
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
		try
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
		catch (ClassCastException a_e)
		{
			// this is a direct proxy!
		}
	}

	public int switchCascade(boolean a_bRetryOnError)
	{
		//synchronized (SHUTDOWN_SYNC)
		{
			synchronized (THREAD_SYNC)
			{
				AnonServerDescription cascade = m_currentMixCascade.getNextMixCascade();
				//fireConnecting(cascade);
				m_Anon.shutdown();
				while (threadRun.isAlive())
				{
					try
					{
						threadRun.interrupt();
						threadRun.join(1000);
					}
					catch (InterruptedException e)
					{
					}
				}
				threadRun = null;
				boolean bConnectionError = false;
				int ret = m_Anon.initialize(cascade);
				if (ret != ErrorCodes.E_SUCCESS)
				{
					if (ret == ErrorCodes.E_INTERRUPTED ||
						(! (a_bRetryOnError && m_currentMixCascade.isCascadeAutoSwitched()) &&
						 (!a_bRetryOnError || ret == E_SIGNATURE_CHECK_FIRSTMIX_FAILED ||
						  ret == E_SIGNATURE_CHECK_OTHERMIX_FAILED || ret == E_MIX_PROTOCOL_NOT_SUPPORTED)))
					{
						return ret;
					}
					else
					{
						bConnectionError = true;
					}
				}
				else
				{
					m_currentMixCascade.keepCurrentCascade(true);
				}
				LogHolder.log(LogLevel.DEBUG, LogType.NET, "AN.ON initialized");

				threadRun = new Thread(this, "JAP - AnonProxy");
				threadRun.setDaemon(true);
				threadRun.start();

				THREAD_SYNC.notifyAll();

				if (bConnectionError)
				{
					connectionError();
					return ErrorCodes.E_CONNECT;
				}
				return ErrorCodes.E_SUCCESS;
			}
		}
	}

	public int start(boolean a_bRetryOnError)
	{
		synchronized (THREAD_SYNC)
		{
			if (threadRun != null)
			{
				return ErrorCodes.E_SUCCESS;
			}

			boolean bConnectionError = false;
			m_numChannels = 0;
			LogHolder.log(LogLevel.DEBUG, LogType.NET, "Try to initialize AN.ON");
			if (m_Anon == null)
			{
				LogHolder.log(LogLevel.EMERG, LogType.NET,
							  " m_Anon is NULL - should never ever happen!");
				return ErrorCodes.E_INVALID_SERVICE;
			}
			MixCascade cascade = m_currentMixCascade.getNextMixCascade();
			/*
			if (cascade.getId().equals("Tor"))
			{
				LogHolder.log(LogLevel.NOTICE, LogType.NET, "Using Tor as anon service!");
				m_Anon = new DirectProxy(m_socketListener);
			}
			else
			{
				m_Anon = AnonServiceFactory.getAnonServiceInstance(AnonServiceFactory.SERVICE_ANON);
			}*/
			int ret = m_Anon.initialize(cascade);
			if (ret != ErrorCodes.E_SUCCESS)
			{
				if (ret == ErrorCodes.E_INTERRUPTED ||
					(!(a_bRetryOnError && m_currentMixCascade.isCascadeAutoSwitched()) &&
					 (!a_bRetryOnError || ret == E_SIGNATURE_CHECK_FIRSTMIX_FAILED ||
					  ret == E_SIGNATURE_CHECK_OTHERMIX_FAILED || ret == E_MIX_PROTOCOL_NOT_SUPPORTED)))
				{
					return ret;
				}
				else
				{
					bConnectionError = true;
				}
			}
			else
			{
				m_currentMixCascade.keepCurrentCascade(true);
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
			threadRun.start();

			if (bConnectionError)
			{
				connectionError();
				return ErrorCodes.E_CONNECT;
			}
			return ErrorCodes.E_SUCCESS;
		}
	}

	public void stop()
	{
		synchronized (SHUTDOWN_SYNC)
		//synchronized (THREAD_SYNC)
		{
			if (threadRun == null)
			{
				disconnected();
				return;
			}
			m_Anon.shutdown();
			if (m_Tor != null)
			{
				m_Tor.shutdown();
			}
			if (m_Mixminion != null)
			{
				m_Mixminion.shutdown();
			}

			while (threadRun.isAlive())
			{
				try
				{
					threadRun.interrupt();
					threadRun.join(1000);

				}
				catch (InterruptedException e)
				{
				}
			}
			/*
			synchronized (THREAD_SYNC)
			{
				THREAD_SYNC.notify();
			}*/
			m_Tor = null;
			m_Mixminion = null;
			threadRun = null;
			disconnected();
		}
	}

	public void run()
	{
		int oldTimeOut = 0;
		LogHolder.log(LogLevel.DEBUG, LogType.NET, "AnonProxy is running as Thread");

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
			while (!Thread.currentThread().isInterrupted())
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
		synchronized (THREAD_SYNC)
		{
			if (m_Anon.isConnected())
			{
				return true;
			}
			if (Thread.currentThread().isInterrupted())
			{
				return false;
			}
			if (!m_currentMixCascade.isReconnectedAutomatically())
			{
				stop();
				return false;
			}
			while (threadRun != null && m_currentMixCascade.isReconnectedAutomatically())
			{
				LogHolder.log(LogLevel.ERR, LogType.NET, "Try reconnect to Mix");
				int ret = m_Anon.initialize(m_currentMixCascade.getNextMixCascade());
				if (ret == ErrorCodes.E_SUCCESS)
				{
					m_currentMixCascade.keepCurrentCascade(true);
					return true;
				}
				try
				{
					THREAD_SYNC.wait(RECONNECT_INTERVAL);
				}
				catch (InterruptedException ex)
				{
					break;
				}
			}
			return false;
		}
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

	private void fireDisconnected()
	{
		synchronized(m_anonServiceListener)
		{
			Enumeration e = m_anonServiceListener.elements();
			while (e.hasMoreElements())
			{
				( (AnonServiceEventListener) e.nextElement()).disconnected();
			}
		}
	}

	private void fireConnecting(AnonServerDescription a_serverDescription)
	{
		synchronized(m_anonServiceListener)
		{
			Enumeration e = m_anonServiceListener.elements();
			while (e.hasMoreElements())
			{
				( (AnonServiceEventListener) e.nextElement()).connecting(
					a_serverDescription);
			}
		}
	}

	private void fireConnectionEstablished(AnonServerDescription a_serverDescription)
	{
		synchronized(m_anonServiceListener)
		{
			Enumeration e = m_anonServiceListener.elements();
			while (e.hasMoreElements())
			{
				( (AnonServiceEventListener) e.nextElement()).connectionEstablished(
					a_serverDescription);
			}
		}
	}

	private void fireConnectionError()
	{
		synchronized(m_anonServiceListener)
		{
			Enumeration e = m_anonServiceListener.elements();
			while (e.hasMoreElements())
			{
				( (AnonServiceEventListener) e.nextElement()).connectionError();
			}
		}
	}

	public void connecting(AnonServerDescription a_serverDescription)
	{
		LogHolder.log(LogLevel.DEBUG, LogType.NET, "AnonProxy received connecting.");
		fireConnecting(a_serverDescription);
	}


	public void connectionEstablished(AnonServerDescription a_serverDescription)
	{
		LogHolder.log(LogLevel.DEBUG, LogType.NET, "AnonProxy received connectionEstablished.");
		fireConnectionEstablished(a_serverDescription);
	}

	public void disconnected()
	{
		LogHolder.log(LogLevel.DEBUG, LogType.NET, "AnonProxy was disconnected.");
		fireDisconnected();
	}

	public void connectionError()
	{
		LogHolder.log(LogLevel.ERR, LogType.NET, "AnonProxy received connectionError");
		fireConnectionError();
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
		synchronized(m_anonServiceListener)
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
	}

	public synchronized void removeEventListener(AnonServiceEventListener l)
	{
		m_anonServiceListener.removeElement(l);
	}

	public boolean isConnected()
	{
		AnonService service = m_Anon;
		return service != null && service.isConnected();
	}

	public void addAIListener(IAIEventListener a_aiListener)
	{
		synchronized (THREAD_SYNC)
		{
			if (m_Anon instanceof AnonClient)
			{
				( ( (AnonClient) m_Anon).getPay()).getAIControlChannel().addAIListener(a_aiListener);
			}
		}
	}


	public void packetMixed(long a_totalBytes)
	{
		Enumeration e = m_anonServiceListener.elements();
		while (e.hasMoreElements())
		{
			( (AnonServiceEventListener) e.nextElement()).packetMixed(a_totalBytes);
		}
	}

	public void dataChainErrorSignaled()
	{
		LogHolder.log(LogLevel.ERR, LogType.NET, "Proxy has been nuked");
		m_currentMixCascade.keepCurrentCascade(false);
		m_Anon.shutdown();
		synchronized (m_anonServiceListener)
		{
			Enumeration e = m_anonServiceListener.elements();
			while (e.hasMoreElements())
			{
				( (AnonServiceEventListener) e.nextElement()).dataChainErrorSignaled();
			}
		}
		reconnect();
	}

	private class DummyMixCascadeContainer extends AbstractMixCascadeContainer
	{
		public MixCascade getNextMixCascade()
		{
			return null;
		}
		public MixCascade getCurrentMixCascade()
		{
			return null;
		}
		public void keepCurrentCascade(boolean a_bKeepCurrentCascade)
		{
		}
		public boolean isCascadeAutoSwitched()
		{
			return false;
		}

		public  boolean isReconnectedAutomatically()
		{
			return false;
		}
	}

	private class EncapsulatedMixCascadeContainer extends AbstractMixCascadeContainer
	{
		private AbstractMixCascadeContainer m_mixCascadeContainer;

		public EncapsulatedMixCascadeContainer(AbstractMixCascadeContainer a_mixCascadeContainer)
		{
			m_mixCascadeContainer = a_mixCascadeContainer;
		}

		public MixCascade getNextMixCascade()
		{
			return m_mixCascadeContainer.getNextMixCascade();
		}

		public MixCascade getCurrentMixCascade()
		{
			return m_mixCascadeContainer.getCurrentMixCascade();
		}

		public void keepCurrentCascade(boolean a_bKeepCurrentCascade)
		{
			m_mixCascadeContainer.keepCurrentCascade(a_bKeepCurrentCascade);
		}

		public boolean isCascadeAutoSwitched()
		{
			return m_mixCascadeContainer.isCascadeAutoSwitched();
		}
		public boolean isReconnectedAutomatically()
		{
			/* reconnect isn't supported with forwarded connections */
			return !m_forwardedConnection && m_mixCascadeContainer.isReconnectedAutomatically();
		}
	}
}


