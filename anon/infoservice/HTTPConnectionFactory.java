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
package anon.infoservice;

import HTTPClient.Codecs;
import HTTPClient.HTTPConnection;
import HTTPClient.NVPair;

/**
 * This class creates all instances of HTTPConnection for the JAP client and is a singleton.
 */
public class HTTPConnectionFactory
{

	/**
	 * This value means, that there is no proxy server to use for the HTTP connection.
	 */
	final public static int PROXY_TYPE_NONE = 0;

	/**
	 * This is the value for using an HTTP proxy server.
	 */
	final public static int PROXY_TYPE_HTTP = 1;

	/**
	 * This is the value for using a SOCKS proxy server.
	 */
	final public static int PROXY_TYPE_SOCKS = 2;

	/**
	 * Stores the instance of HTTPConnectionFactory (Singleton).
	 */
	private static HTTPConnectionFactory httpConnectionFactoryInstance;

	/**
	 * Stores the username for the proxy authorization.
	 */
	private String proxyAuthUserName;

	/**
	 * Stores the password for the proxy authorization.
	 */
	private String proxyAuthPassword;

	/**
	 * Stores the communication timeout (sec) for new HTTP connections. If this value is zero, a
	 * connection never times out.
	 */
	private int m_timeout;

	/**
	 * This creates a new instance of HTTPConnectionFactory. This is only used for setting some
	 * values. Use HTTPConnectionFactory.getInstance() for getting an instance of this class.
	 */
	private HTTPConnectionFactory()
	{
		/* default is to use no proxy for all new HTTPConnection instances */
		setNewProxySettings(PROXY_TYPE_NONE, null, -1, null, null);
		m_timeout = 10;
	}

	/**
	 * Returns the instance of HTTPConnectionFactory (Singleton). If there is no instance,
	 * there is a new one created.
	 *
	 * @return The HTTPConnectionFactory instance.
	 */
	public static HTTPConnectionFactory getInstance()
	{
		if (httpConnectionFactoryInstance == null)
		{
			httpConnectionFactoryInstance = new HTTPConnectionFactory();
		}
		return httpConnectionFactoryInstance;
	}

	/**
	 * This method sets new settings for the proxy server. All HTTPConnection instances created
	 * after the call of this method will use them. Instances of HTTPConnection which already exist
	 * are not influenced by that call. The default after creating the instance of
	 * HTTPConnectionFactory is to use no proxy for all new instances of HTTPConnection.
	 *
	 * @param proxyType The type of the proxy (see the constants in this class).
	 * @param proxyHost IP address or hostname of the proxy server. If no hostname is supplied, the
	 *                  proxyType is set to PROXY_TYPE_NONE.
	 * @param proxyPort The port of the proxy server. The value must be between 1 and 65535. If it
	 *                  is not, the proxyType is set to PROXY_TYPE_NONE.
	 * @param proxyAuthUserName The username for the authorization. If the proxy server does not
	 *                          need authentication, take null. This value is only meaningful, if
	 *                          the proxyType is PROXY_TYPE_HTTP.
	 * @param proxyAuthPassword The password for the authorization. If the proxy server does not
	 *                          need authentication, take null. This value is only meaningful, if
	 *                          the proxyType is PROXY_TYPE_HTTP and proxyAuthUserName is not null.
	 */
	public void setNewProxySettings(int proxyType, String proxyHost, int proxyPort, String proxyAuthUserName,
									String proxyAuthPassword)
	{
		if (proxyHost == null)
		{
			proxyType = PROXY_TYPE_NONE;
		}
		if ( (proxyPort < 1) || (proxyPort > 65535))
		{
			proxyType = PROXY_TYPE_NONE;
		}
		synchronized (this)
		{
			/* don't allow to create new connections until we have changed all proxy attributes */
			if (proxyType == PROXY_TYPE_HTTP)
			{
				/* proxy authorization is only with HTTP proxies supported */
				this.proxyAuthUserName = proxyAuthUserName;
				this.proxyAuthPassword = proxyAuthPassword;
			}
			else
			{
				this.proxyAuthUserName = null;
				this.proxyAuthPassword = null;
			}
			/* set the new values for the proxy */
			if (proxyType == PROXY_TYPE_NONE)
			{
				HTTPConnection.setProxyServer(null, -1);
				HTTPConnection.setSocksServer(null, -1);
			}
			if (proxyType == PROXY_TYPE_HTTP)
			{
				HTTPConnection.setProxyServer(proxyHost, proxyPort);
				HTTPConnection.setSocksServer(null, -1);
			}
			if (proxyType == PROXY_TYPE_SOCKS)
			{
				HTTPConnection.setProxyServer(null, -1);
				HTTPConnection.setSocksServer(proxyHost, proxyPort);
			}
		}
	}

	/**
	 * Sets the communication timeout (sec) for new HTTP connections. If this value is zero, a
	 * connection never times out. Instances of HTTPConnection which already exist, are not
	 * influenced by this method.
	 *
	 * @param a_timeout The new communication timeout.
	 */
	public void setTimeout(int a_timeout)
	{
		synchronized (this)
		{
			m_timeout = a_timeout;
		}
	}

	/**
	 * Returns the communication timeout (sec) for new HTTP connections. If this value is zero, a
	 * connection never times out.
	 *
	 * @return The communication timeout for new connections.
	 */
	public int getTimeout()
	{
		int r_timeout = 0;
		synchronized (this)
		{
			r_timeout = m_timeout;
		}
		return r_timeout;
	}

	/**
	 * This method creates a new instance of HTTPConnection. The current proxy settings are used.
	 *
	 * @param target The ListenerInterface of the connection target.
	 *
	 * @return A new instance of HTTPConnection with a connection to the specified target and the
	 *         current proxy settings, when the instance is created.
	 */
	public HTTPConnection createHTTPConnection(ListenerInterface target)
	{
		HTTPConnection newConnection = null;
		synchronized (this)
		{
			/* get always the current proxy settings */
			newConnection = new HTTPConnection(target.getHost(), target.getPort());
			if (proxyAuthUserName != null)
			{
				/* set the proxy authorization if neccessary (only HTTP proxies) */
				String tmpPassword = null;
				if (proxyAuthPassword != null)
				{
					tmpPassword = Codecs.base64Encode(proxyAuthUserName + ":" + proxyAuthPassword);
				}
				else
				{
					tmpPassword = Codecs.base64Encode(proxyAuthUserName + ":");
				}
				NVPair authorizationHeader = new NVPair("Proxy-Authorization", "Basic " + tmpPassword);
				replaceHeader(newConnection, authorizationHeader);
			}
		}
		/* set some header infos */
		NVPair[] headers = new NVPair[2];
		headers[0] = new NVPair("Cache-Control", "no-cache");
		headers[1] = new NVPair("Pragma", "no-cache");
		replaceHeader(newConnection, headers[0]);
		replaceHeader(newConnection, headers[1]);
		newConnection.setAllowUserInteraction(false);
		/* set the timeout for all network operations */
		newConnection.setTimeout(getTimeout() * 1000);
		return newConnection;
	}

	/**
	 * An internal helper function to set the header information for the HTTP connection.
	 *
	 * @param connection The connection where the new headers are set.
	 * @param header The header information to set.
	 */
	private void replaceHeader(HTTPConnection connection, NVPair header)
	{
		NVPair headers[] = connection.getDefaultHeaders();
		if ( (headers == null) || (headers.length == 0))
		{
			/* create new header with one field and set it */
			headers = new NVPair[1];
			headers[0] = header;
			connection.setDefaultHeaders(headers);
		}
		else
		{
			int len = headers.length;
			for (int i = 0; i < len; i++)
			{
				if (headers[i].getName().equalsIgnoreCase(header.getName()))
				{
					/* if there is a header with the same name, replace it */
					headers[i] = header;
					connection.setDefaultHeaders(headers);
					return;
				}
			}
			/* no header with the same name found, add a field and set it */
			NVPair tmpHeaders[] = new NVPair[len + 1];
			for (int i = 0; i < len; i++)
			{
				tmpHeaders[i] = headers[i];
			}
			tmpHeaders[len] = header;
			connection.setDefaultHeaders(tmpHeaders);
		}
	}

}
