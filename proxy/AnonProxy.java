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
import anon.util.XMLUtil;
import pay.Pay;
import payxml.XMLAccountInfo;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import javax.xml.parsers.DocumentBuilderFactory;

final public class AnonProxy implements Runnable, IAnonProxy
{
	public static final int E_BIND = -2;
	public final static int E_MIX_PROTOCOL_NOT_SUPPORTED = ErrorCodes.E_MIX_PROTOCOL_NOT_SUPPORTED;

	// ootte
	public final static int E_SIGNATURE_CHECK_FIRSTMIX_FAILED = ErrorCodes.E_SIGNATURE_CHECK_FIRSTMIX_FAILED;
	public final static int E_SIGNATURE_CHECK_OTHERMIX_FAILED = ErrorCodes.E_SIGNATURE_CHECK_OTHERMIX_FAILED;

	// ootte

	private AnonService m_Anon;
	private Thread threadRun;
	private volatile boolean m_bIsRunning;
	private ServerSocket m_socketListener;
	private ProxyListener m_ProxyListener;
	private volatile int m_numChannels = 0;
	private AICommunication m_AICom;

	/**
	 * Stores the MixCascade we are connected to.
	 */
	private MixCascade m_currentMixCascade;

	private boolean m_bAutoReconnect = false;
	public AnonProxy(ServerSocket listener)
	{
		m_socketListener = listener;
		m_Anon = AnonServiceFactory.create();
		setFirewall(JAPConstants.FIREWALL_TYPE_HTTP, null, -1);
		setFirewallAuthorization(null, null);
		setDummyTraffic( -1);
		m_AICom = new AICommunication(m_Anon);
	}

	// methode zum senden eines AccountCertifikates und einer balance an die AI - oneway
	public void authenticateForAI()
	{
		String toAI = "";
		try
		{
			Pay pay = Pay.getInstance();
			toAI = pay.getAccount(pay.getUsedAccount()).getAccountCertificate().getXMLString();
			( (AnonServiceImpl) m_Anon).sendPayPackets(toAI);
		}
		catch (Exception ex)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.NET,
						  "AnonProxy: Fehler beim Anfordern des KontoZertifikates und/oder des Kontostandes");
		}
		LogHolder.log(LogLevel.DEBUG, LogType.NET,
					  "AnonProxy: l\uFFFDnge des zu verschickenden Certifikates : " + toAI.length());
		sendBalanceToAI();
	}

	// methode zum senden einer balance an die AI - oneway
	public void sendBalanceToAI()
	{
		LogHolder.log(LogLevel.DEBUG, LogType.NET, "AnonProxy: sending Balance to AI");
		try
		{
			Pay pay = Pay.getInstance();
			XMLAccountInfo info = pay.getAccount(pay.getUsedAccount()).getAccountInfo();

			// temporary code... TODO: remove DOM functionality from here (Bastian Voigt)
			Document doc = info.getDomDocument();
			Element elemRoot = doc.getDocumentElement();
			Element elemBalance = (Element) XMLUtil.getFirstChildByName(elemRoot, "Balance");

			Document balanceDoc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
			elemRoot =(Element) XMLUtil.importNode(balanceDoc, elemBalance, true);
			balanceDoc.appendChild(elemRoot);

			String strBalance = XMLUtil.XMLDocumentToString(balanceDoc);
			( (AnonServiceImpl) m_Anon).sendPayPackets(strBalance);
		}
		catch (Exception ex)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.NET,
						  "AnonProxy: Fehler beim Anfordern des KontoZertifikates und/oder des Kontostandes");
		}
	}

	/**
	 * Sets a new MixCascade.
	 *
	 * @param newMixCascade The new MixCascade we are connected to.
	 */
	public void setMixCascade(MixCascade newMixCascade)
	{
		m_currentMixCascade = newMixCascade;
		m_AICom.setAnonServer(newMixCascade);
	}

	public void setFirewall(int type, String host, int port)
	{
		if (type == JAPConstants.FIREWALL_TYPE_SOCKS)
		{
			type = AnonServiceImpl.FIREWALL_TYPE_SOCKS;
		}
		else
		{
			type = AnonServiceImpl.FIREWALL_TYPE_HTTP;
		}
		( (AnonServiceImpl) m_Anon).setFirewall(type, host, port);
	}

	public void setFirewallAuthorization(String id, String passwd)
	{
		( (AnonServiceImpl) m_Anon).setFirewallAuthorization(id, passwd);
	}

	public void setDummyTraffic(int msIntervall)
	{
		( (AnonServiceImpl) m_Anon).setDummyTraffic(msIntervall);
	}

	public void setAutoReConnect(boolean b)
	{
		m_bAutoReconnect = b;
	}

	public void setMixCertificationCheck(boolean enabled, JAPCertificateStore trustedRoots)
	{
		( (AnonServiceImpl) m_Anon).seteMixCertificationAuthorities(trustedRoots);
		( (AnonServiceImpl) m_Anon).setEnableMixCertificationCheck(enabled);
	}

	public int start()
	{
		m_numChannels = 0;
		int ret = m_Anon.connect(m_currentMixCascade);
		if (ret != ErrorCodes.E_SUCCESS)
		{
			return ret;
		}
		threadRun = new Thread(this, "JAP - AnonProxy");
		threadRun.start();
		return E_SUCCESS;
	}

	public void stop()
	{
		//m_AICom.end();
		m_Anon.disconnect();
		m_bIsRunning = false;
		try
		{
			threadRun.join();
		}
		catch (Exception e)
		{}
	}

	public void setProxyListener(ProxyListener l)
	{
		m_ProxyListener = l;
	}

	public void run()
	{
		m_bIsRunning = true;
		int oldTimeOut = 0;
		LogHolder.log(LogLevel.DEBUG, LogType.NET, "AnonProxy: AnonProxy is running as Thread");

		m_AICom.start();
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
				//2001-04-04(HF)
				AnonChannel newChannel = null;
				while (m_bIsRunning)
				{
					try
					{
						newChannel = null;
						newChannel = m_Anon.createChannel(AnonChannel.HTTP);
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
							int ret = m_Anon.connect(m_currentMixCascade);
							if (ret == ErrorCodes.E_SUCCESS)
							{
								break;
							}
							Thread.sleep(10000);
						}
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

	public synchronized void incNumChannels()
	{
		m_numChannels++;
		m_ProxyListener.channelsChanged(m_numChannels);
	}

	public synchronized void decNumChannels()
	{
		m_numChannels--;
		m_ProxyListener.channelsChanged(m_numChannels);
	}

	public synchronized void transferredBytes(int bytes)
	{
		m_ProxyListener.transferedBytes(bytes);
	}

}
