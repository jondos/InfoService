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
package anon.server;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetAddress;
import java.util.Enumeration;
import java.util.Vector;
import anon.AnonChannel;
import anon.AnonService;
import anon.AnonServiceEventListener;
import anon.ErrorCodes;
import anon.crypto.JAPCertificateStore;
import anon.infoservice.MixCascade;
import anon.server.impl.KeyPool;
import anon.server.impl.MuxSocket;
import anon.server.impl.ProxyConnection;
import anon.AnonServerDescription;
import anon.infoservice.ImmutableProxyInterface;
import anon.pay.Pay;
import anon.pay.BI;

final public class AnonServiceImpl implements AnonService
{
	//private static AnonServiceImpl m_AnonServiceImpl=null;
	private MuxSocket m_MuxSocket = null;
	private Vector m_AnonServiceListener;
	private ImmutableProxyInterface m_proxyInterface;
	private boolean m_bMixCertCheckEnabled;
	private JAPCertificateStore m_certsTrustedRoots;

	/**
	 * Stores the connection when we use forwarding.
	 */
	private ProxyConnection m_proxyConnection;
	private Pay m_Pay = null;

	public AnonServiceImpl()
	{
		this( (ImmutableProxyInterface)null);
	}

	public AnonServiceImpl(ImmutableProxyInterface a_proxyInterface)
	{
		m_AnonServiceListener = new Vector();
		m_bMixCertCheckEnabled = false;
		m_certsTrustedRoots = null;
		m_MuxSocket = MuxSocket.create();
		m_proxyConnection = null;
		m_proxyInterface = null;
		setProxy(a_proxyInterface);
	}

	/**
	 * This creates a new AnonServiceImpl with a forwarded connection.
	 *
	 * @param a_proxyConnection An active connection to the first mix of a cascade.
	 */
	public AnonServiceImpl(ProxyConnection a_proxyConnection)
	{
		/* call the default constructor */
		this(a_proxyConnection.getProxyInterface());
		m_proxyConnection = a_proxyConnection;
	}

	public int initialize(AnonServerDescription mixCascade)
	{
		try
		{
			int rc;
			if( (rc=connect( (MixCascade) mixCascade)) != ErrorCodes.E_SUCCESS)
				return rc;

			// start Payment (2004-10-20 Bastian Voigt)
			m_Pay = new Pay(m_MuxSocket);
			return ErrorCodes.E_SUCCESS;
		}
		catch (Exception e)
		{
		}
		return ErrorCodes.E_INVALID_SERVICE;
	}

	public int setProxy(ImmutableProxyInterface a_Proxy)
	{
		m_proxyInterface=a_Proxy;
		return ErrorCodes.E_SUCCESS;
	}

	private int connect(MixCascade mixCascade)
	{
		int ret = -1;
		if (m_proxyConnection == null)
		{
			ret = m_MuxSocket.connectViaFirewall(mixCascade, m_proxyInterface, m_bMixCertCheckEnabled,
												 m_certsTrustedRoots);
		}
		else
		{
			/* the connection already exists */
			ret = m_MuxSocket.initialize(m_proxyConnection, m_bMixCertCheckEnabled, m_certsTrustedRoots);
		}
		if (ret != ErrorCodes.E_SUCCESS)
		{
			return ret;
		}
		return m_MuxSocket.startService();
	}

	public void shutdown()
	{
		if(m_Pay!=null)
			m_Pay.shutdown(); // shutdown payment
		m_MuxSocket.stopService();
	}


	public AnonChannel createChannel(int type) throws ConnectException
	{
		return m_MuxSocket.newChannel(type);
	}

	public AnonChannel createChannel(String addr, int port) throws ConnectException
	{
		byte[] buff = new byte[13];
		AnonChannel c = null;
		try
		{
			c = createChannel(AnonChannel.SOCKS);
			InputStream in = c.getInputStream();
			OutputStream out = c.getOutputStream();
			buff[0] = 5;
			buff[1] = 1;
			buff[2] = 0;

			buff[3] = 5;
			buff[4] = 1;
			buff[5] = 0;
			buff[6] = 1;
			System.arraycopy(InetAddress.getByName(addr).getAddress(), 0, buff, 7, 4); //7,8,9,10
			buff[11] = (byte) (port >> 8);
			buff[12] = (byte) (port & 0xFF);
			out.write(buff, 0, 13);
			out.flush();
			int len = 12;
			while (len > 0)
			{
				len -= in.read(buff, 0, len);
			}
		}
		catch (ConnectException ec)
		{
			throw ec;
		}
		catch (Exception e)
		{
			throw new ConnectException("createChannel(): " + e.getMessage());
		}
		if (buff[3] != 0) // failure!
		{
			throw new ConnectException("SOCKS Server reports an error!");
		}
		return c;
	}

	public synchronized void addEventListener(AnonServiceEventListener l)
	{
		Enumeration e = m_AnonServiceListener.elements();
		while (e.hasMoreElements())
		{
			if (l.equals(e.nextElement()))
			{
				return;
			}
		}
		m_AnonServiceListener.addElement(l);
	}

	public synchronized void removeEventListener(AnonServiceEventListener l)
	{
		m_AnonServiceListener.removeElement(l);
	}

	//special local Service functions
	public void setDummyTraffic(int intervall)
	{
		m_MuxSocket.setDummyTraffic(intervall);
	}

	public void setEnableMixCertificationCheck(boolean b)
	{
		m_bMixCertCheckEnabled = b;
	}

	public boolean isMixCertificationCheckEnabled()
	{
		return m_bMixCertCheckEnabled;
	}

	public void setMixCertificationAuthorities(JAPCertificateStore trustedRoots)
	{
		m_certsTrustedRoots = trustedRoots;
	}

	public static void init()
	{
		KeyPool.start();
	}
}
