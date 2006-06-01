/*
 * Copyright (c) 2006, The JAP-Team
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *   - Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of the University of Technology Dresden, Germany nor
 *     the names of its contributors may be used to endorse or promote
 *     products derived from this software without specific prior written
 *     permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package anon.client;

import java.io.InterruptedIOException;
import java.io.IOException;
import java.net.ConnectException;
import java.net.Socket;
import java.net.SocketException;
import java.security.SecureRandom;
import java.security.SignatureException;
import java.util.Enumeration;
import java.util.Observable;
import java.util.Observer;
import java.util.Vector;

import anon.AnonChannel;
import anon.AnonServerDescription;
import anon.AnonService;
import anon.AnonServiceEventListener;
import anon.ErrorCodes;
import anon.NotConnectedToMixException;
import anon.client.replay.ReplayControlChannel;
import anon.client.replay.TimestampUpdater;
import anon.infoservice.HTTPConnectionFactory;
import anon.infoservice.ImmutableProxyInterface;
import anon.infoservice.MixCascade;
import anon.pay.AIControlChannel;
import anon.pay.Pay;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/**
 * @author Stefan Lieske
 */
public class AnonClient implements AnonService, Observer
{

	private static final int LOGIN_TIMEOUT = 60000;

	private Multiplexer m_multiplexer;

	private ImmutableProxyInterface m_proxyInterface;

	private Object m_internalSynchronization;

	private Object m_internalSynchronizationForSocket;

	private Object m_internalSynchronizationForDummyTraffic;

	private SocketHandler m_socketHandler;

	private Vector m_eventListeners;

	private PacketCounter m_packetCounter;

	private DummyTrafficControlChannel m_dummyTrafficControlChannel;

	private int m_dummyTrafficInterval;

	private KeyExchangeManager m_keyExchangeManager;

	private Socket m_connectedSocket;

	private Pay m_paymentInstance;

	private boolean m_connected;

	public AnonClient()
	{
		m_socketHandler = null;
		m_multiplexer = null;
		m_proxyInterface = null;
		m_packetCounter = null;
		m_dummyTrafficControlChannel = null;
		m_dummyTrafficInterval = -1;
		m_keyExchangeManager = null;
		m_connectedSocket = null;
		m_paymentInstance = null;
		m_internalSynchronization = new Object();
		m_internalSynchronizationForSocket = new Object();
		m_internalSynchronizationForDummyTraffic = new Object();
		m_eventListeners = new Vector();
		m_connected = false;
	}

	public AnonClient(Socket a_connectedSocket)
	{
		this();
		m_connectedSocket = a_connectedSocket;
	}

	public int initialize(AnonServerDescription a_mixCascade)
	{
		if (! (a_mixCascade instanceof MixCascade))
		{
			return ErrorCodes.E_INVALID_SERVICE;
		}
		MixCascade mixCascade = (MixCascade) a_mixCascade;
		synchronized (m_internalSynchronization)
		{
			if (isConnected())
			{
				return ErrorCodes.E_ALREADY_CONNECTED;
			}
			Socket socketToMixCascade = null;
			if (m_connectedSocket != null)
			{
				socketToMixCascade = m_connectedSocket;
				m_connectedSocket = null;
			}
			else
			{
				try
				{
					socketToMixCascade = connectMixCascade(mixCascade, m_proxyInterface);
				}
				catch (InterruptedIOException a_e)
				{
					return ErrorCodes.E_INTERRUPTED;
				}
			}
			if (socketToMixCascade == null)
			{
				return ErrorCodes.E_CONNECT;
			}
			return initializeProtocol(socketToMixCascade);
		}
	}

	public int setProxy(ImmutableProxyInterface a_proxyInterface)
	{
		synchronized (m_internalSynchronization)
		{
			m_proxyInterface = a_proxyInterface;
		}
		return ErrorCodes.E_SUCCESS;
	}

	public void shutdown()
	{
		synchronized (m_internalSynchronizationForSocket)
		{
			if (m_socketHandler != null)
			{
				m_socketHandler.deleteObserver(this);
				m_socketHandler.closeSocket();
				m_socketHandler = null;
			}
		}
		synchronized (m_internalSynchronization)
		{
			m_multiplexer = null;
			m_connected = false;
			synchronized (m_internalSynchronizationForDummyTraffic)
			{
				if (m_dummyTrafficControlChannel != null)
				{
					m_dummyTrafficControlChannel.stop();
					m_dummyTrafficControlChannel = null;
				}
			}
			if (m_packetCounter != null)
			{
				m_packetCounter.deleteObserver(this);
				m_packetCounter = null;
			}
			if (m_keyExchangeManager != null)
			{
				m_keyExchangeManager.removeCertificateLock();
				m_keyExchangeManager = null;
			}
			if (m_paymentInstance != null)
			{
				m_paymentInstance = null;
			}
		}
	}

	public boolean isConnected()
	{
		return m_connected;
	}

	public AnonChannel createChannel(int a_type) throws ConnectException
	{
		Multiplexer multiplexer = null;
		KeyExchangeManager keyExchangeManager = null;
		synchronized (m_internalSynchronization)
		{
			if (m_multiplexer == null)
			{
				/* not connected */
				throw (new NotConnectedToMixException(
					"AnonClient: createChannel(): The AN.ON client is currently not connected to a mixcascade."));
			}
			/* we don't want to block everything, if chain-creation is waiting
			 * for free resources -> only get the pointers to the necessary objects
			 * within the synchronized part
			 */
			multiplexer = m_multiplexer;
			keyExchangeManager = m_keyExchangeManager;
		}
		FixedRatioChannelsDescription channelsDescription = keyExchangeManager.
			getFixedRatioChannelsDescription();
		if (channelsDescription == null)
		{
			/* old protocol with unlimited channels */
			return (new SingleChannelDataChain(multiplexer.getChannelTable(), a_type,
											   keyExchangeManager.isChainProtocolWithFlowControl()));
		}
		return (new TypeFilterDataChain(new SequentialChannelDataChain(multiplexer.getChannelTable(),
			channelsDescription.getChainTimeout()), a_type));
	}

	public void addEventListener(AnonServiceEventListener a_eventListener)
	{
		synchronized (m_eventListeners)
		{
			m_eventListeners.addElement(a_eventListener);
		}

	}

	public void removeEventListener(AnonServiceEventListener a_eventListener)
	{
		synchronized (m_eventListeners)
		{
			m_eventListeners.removeElement(a_eventListener);
		}
	}

	public void update(Observable a_object, final Object a_argument)
	{
		if ( (a_object == m_socketHandler) && (a_argument instanceof IOException))
		{
			synchronized (m_eventListeners)
			{
				final Enumeration eventListenersList = m_eventListeners.elements();
				Thread notificationThread = new Thread(new Runnable()
				{
					public void run()
					{
						while (eventListenersList.hasMoreElements())
						{
							( (AnonServiceEventListener) (eventListenersList.nextElement())).connectionError();
						}
					}
				}, "AnonClient: ConnectionError notification");
				notificationThread.setDaemon(true);
				notificationThread.start();
			}
			/* shutdown everything */
			shutdown();
		}
		else if ( (a_object == m_packetCounter) && (a_argument instanceof Long))
		{
			synchronized (m_eventListeners)
			{
				final Enumeration eventListenersList = m_eventListeners.elements();
				Thread notificationThread = new Thread(new Runnable()
				{
					public void run()
					{
						while (eventListenersList.hasMoreElements())
						{
							( (AnonServiceEventListener) (eventListenersList.nextElement())).packetMixed( ( (
								Long) a_argument).longValue());
						}
					}
				}, "AnonClient: PacketMixed notification");
				notificationThread.setDaemon(true);
				notificationThread.start();
			}
		}
	}

	public void setDummyTraffic(int a_interval)
	{
		synchronized (m_internalSynchronizationForDummyTraffic)
		{
			m_dummyTrafficInterval = a_interval;
			if (m_dummyTrafficControlChannel != null)
			{
				m_dummyTrafficControlChannel.setDummyTrafficInterval(a_interval);
			}
		}
	}

	public Pay getPay()
	{
		return m_paymentInstance;
	}

	private Socket connectMixCascade(MixCascade a_mixCascade, ImmutableProxyInterface a_proxyInterface)
		throws InterruptedIOException
	{
		LogHolder.log(LogLevel.DEBUG, LogType.NET,
					  "AnonClient: connectMixCascade(): Trying to connect to MixCascade '" +
					  a_mixCascade.toString() + "'...");
		Socket connectedSocket = null;
		int i = 0;
		while ((i < a_mixCascade.getNumberOfListenerInterfaces()) && (connectedSocket == null) && (!Thread.currentThread().isInterrupted()))
		{
			/* try out all interfaces of the mixcascade until we have a connection */
			try
			{
				connectedSocket = HTTPConnectionFactory.getInstance().createHTTPConnection(a_mixCascade.
					getListenerInterface(i), a_proxyInterface).Connect();
			}
			catch (InterruptedIOException e)
			{
				/* Thread.interrupt() was called while connection-establishment
				 * -> stop all activities
				 */
				LogHolder.log(LogLevel.NOTICE, LogType.NET, "AnonClient: connectMixCascade(): Interrupted while connecting to MixCascade '" + a_mixCascade.toString() + "'.");
				throw e;
			}
			catch (Exception e)
			{
				LogHolder.log(LogLevel.ERR, LogType.NET, e);
			}
			i++;
		}
		if (connectedSocket != null)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.NET,
						  "AnonClient: connectMixCascade(): Connection to MixCascade '" +
						  a_mixCascade.toString() + "' successfully established - starting key-exchange...");
		}
		else
		{
			LogHolder.log(LogLevel.ERR, LogType.NET,
						  "AnonClient: connectMixCascade(): Failed to connect to MixCascade '" +
						  a_mixCascade.toString() + "'.");
		}
		return connectedSocket;
	}

	private int initializeProtocol(Socket a_connectedSocket)
	{
		try
		{
			synchronized (m_internalSynchronizationForSocket)
			{
				m_socketHandler = new SocketHandler(a_connectedSocket);
			}
			try
			{
				/* limit timeouts while login procedure */
				a_connectedSocket.setSoTimeout(LOGIN_TIMEOUT);
			}
			catch (SocketException e)
			{
				/* ignore it */
			}
			m_keyExchangeManager = new KeyExchangeManager(m_socketHandler.getInputStream(),
				m_socketHandler.getOutputStream());
		}
		catch (UnknownProtocolVersionException e)
		{
			LogHolder.log(LogLevel.ERR, LogType.NET, e);
			closeSocketHandler();
			return ErrorCodes.E_PROTOCOL_NOT_SUPPORTED;
		}
		catch (SignatureException a_e)
		{
			LogHolder.log(LogLevel.ERR, LogType.CRYPTO, a_e);
			closeSocketHandler();
			/** @todo Make this more transparent... */
			return ErrorCodes.E_SIGNATURE_CHECK_FIRSTMIX_FAILED;
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.ERR, LogType.NET, e);
			closeSocketHandler();
			return ErrorCodes.E_UNKNOWN;
		}
		m_multiplexer = new Multiplexer(m_socketHandler.getInputStream(), m_socketHandler.getOutputStream(),
										m_keyExchangeManager, new SecureRandom());
		m_socketHandler.addObserver(this);
		m_packetCounter = new PacketCounter();
		m_multiplexer.addObserver(m_packetCounter);
		m_packetCounter.addObserver(this);
		synchronized (m_internalSynchronizationForDummyTraffic)
		{
			m_dummyTrafficControlChannel = new DummyTrafficControlChannel(m_multiplexer);
			m_dummyTrafficControlChannel.setDummyTrafficInterval(m_dummyTrafficInterval);
		}
		/* maybe we have to start some more services */
		int errorCode = finishInitialization(m_multiplexer, m_keyExchangeManager, m_proxyInterface,
											 m_packetCounter);
		if (errorCode != ErrorCodes.E_SUCCESS)
		{
			shutdown();
			return errorCode;
		}
		try
		{
			/* try to set infinite timeout */
			a_connectedSocket.setSoTimeout(0);
		}
		catch (SocketException e)
		{
			/* ignore it */
		}
		synchronized (m_eventListeners)
		{
			final Enumeration eventListenersList = m_eventListeners.elements();
			Thread notificationThread = new Thread(new Runnable()
			{
				public void run()
				{
					while (eventListenersList.hasMoreElements())
					{
						( (AnonServiceEventListener) (eventListenersList.nextElement())).
							connectionEstablished();
					}
				}
			}, "AnonClient: ConnectionEstablished notification");
			notificationThread.setDaemon(true);
			notificationThread.start();
		}
		/* AnonClient successfully started */
		m_connected = true;
		return ErrorCodes.E_SUCCESS;
	}

	private int finishInitialization(Multiplexer a_multiplexer, KeyExchangeManager a_keyExchangeManager,
									 ImmutableProxyInterface a_proxyInterface, PacketCounter a_packetCounter)
	{
		if (a_keyExchangeManager.isProtocolWithTimestamp())
		{
			/* initialize replay-prevention */
			MixParameters[] mixesWithReplayDetection = a_keyExchangeManager.getMixParameters();
			if (a_keyExchangeManager.getFirstMixSymmetricCipher() != null)
			{
				/* the first mix doesn't need timestamps */
				mixesWithReplayDetection = new MixParameters[a_keyExchangeManager.getMixParameters().length -
					1];
				for (int i = 0; i < a_keyExchangeManager.getMixParameters().length - 1; i++)
				{
					mixesWithReplayDetection[i] = a_keyExchangeManager.getMixParameters()[i + 1];
				}
			}
			try
			{
				TimestampUpdater updater = new TimestampUpdater(mixesWithReplayDetection,
					new ReplayControlChannel(a_multiplexer));
			}
			catch (Exception e)
			{
				LogHolder.log(LogLevel.ERR, LogType.NET,
					"AnonClient: finishInitialization(): Fetching of timestamps failed - closing connection. Reason: " +
							  e.toString());
				return ErrorCodes.E_UNKNOWN;
			}
		}
		/* it seems that some payment things must be started even if payment is
		 * disabled
		 */
		AIControlChannel aiControlChannel = new AIControlChannel(a_multiplexer, a_proxyInterface,
			a_packetCounter);
		m_paymentInstance = new Pay(aiControlChannel);
		if (a_keyExchangeManager.isPaymentRequired())
		{
			aiControlChannel.sendAccountCert();
		}
		return ErrorCodes.E_SUCCESS;
	}

	private void closeSocketHandler()
	{
		synchronized (m_internalSynchronizationForSocket)
		{
			if (m_socketHandler != null)
			{
				/* we have to check for null because the socket could be closed by another
				 * thread in the meantime
				 */
				m_socketHandler.closeSocket();
				m_socketHandler = null;
			}
		}
	}

}
