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

import java.util.Vector;

import HTTPClient.AuthorizationInfo;
import HTTPClient.AuthorizationPrompter;
import HTTPClient.DefaultAuthHandler;
import HTTPClient.HTTPConnection;
import HTTPClient.NVPair;

/**
 * This class creates all instances of HTTPConnection for the JAP client and is a singleton.
 */
public class HTTPConnectionFactory
{
	/**
	 * Defines the HTTPConnection class for that this factory constructs instances.
	 */
	private static Class ms_HTTPConnectionClass = HTTPConnection.class;

	/**
	 * Stores the instance of HTTPConnectionFactory (Singleton).
	 */
	private static HTTPConnectionFactory ms_httpConnectionFactoryInstance;

	/**
	 * The HTTPConnections that were recently created by this factory object.
	 */
	private Vector m_vecHTTPConnections;

	/**
	 * Stores the communication timeout (sec) for new HTTP connections. If this value is zero, a
	 * connection never times out.
	 */
	private int m_timeout;

	/**
	 * The listener for the proxy used.
	 */
	private ImmutableProxyInterface m_proxyInterface;

	/**
	 * This creates a new instance of HTTPConnectionFactory. This is only used for setting some
	 * values. Use HTTPConnectionFactory.getInstance() for getting an instance of this class.
	 */
	private HTTPConnectionFactory()
	{
		/* default is to use no proxy for all new HTTPConnection instances */
		setNewProxySettings(null);
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
		if (ms_httpConnectionFactoryInstance == null)
		{
			ms_httpConnectionFactoryInstance = new HTTPConnectionFactory();

			try
			{
				// remove this module as it doesn`t work and as it interferes with our own implementation
				//HTTPConnection.removeDefaultModule(Class.forName("HTTPClient.AuthorizationModule"));
			}
			catch (Exception e)
			{
			}
		}
		return ms_httpConnectionFactoryInstance;
	}

	/**
	 * This method sets new settings for the proxy server. All HTTPConnection instances created
	 * after the call of this method will use them. Instances of HTTPConnection which already exist
	 * are not influenced by that call. The default after creating the instance of
	 * HTTPConnectionFactory is to use no proxy for all new instances of HTTPConnection.
	 *
	 * @param a_proxyInterface the listener interface of the proxy server; if it is set to null, no
	 *                        proxy is used
	 */
	public synchronized void setNewProxySettings(ImmutableProxyInterface a_proxyInterface)
	{
		m_proxyInterface = a_proxyInterface;

		if (a_proxyInterface == null || !a_proxyInterface.isValid())
		{
			m_proxyInterface = null;
			HTTPConnection.setProxyServer(null, -1);
			HTTPConnection.setSocksServer(null, -1);
			return;
		}

		/* don't allow to create new connections until we have changed all proxy attributes */
		if (a_proxyInterface.getProtocol() == ImmutableListenerInterface.PROTOCOL_TYPE_HTTP)
		{
			/* set the new values for the proxy */
			HTTPConnection.setProxyServer(a_proxyInterface.getHost(), a_proxyInterface.getPort());
			HTTPConnection.setSocksServer(null, -1);
		}
		else if (a_proxyInterface.getProtocol() == ImmutableListenerInterface.PROTOCOL_TYPE_SOCKS)
		{
			/** @todo check why this code is not used! */
			HTTPConnection.setProxyServer(null, -1);
			HTTPConnection.setSocksServer(a_proxyInterface.getHost(), a_proxyInterface.getPort());
			NVPair[] up = new NVPair[1];
			up[0] = new NVPair(a_proxyInterface.getAuthenticationUserID(),
							   a_proxyInterface.getAuthenticationPassword());
			AuthorizationInfo.addAuthorization(new AuthorizationInfo(a_proxyInterface.getHost(),
				a_proxyInterface.getPort(),
				"SOCKS5", "USER/PASS", up, null));
		}

	}

	/**
	 * Sets the communication timeout (sec) for new HTTP connections. If this value is zero
	 * or lower, a connection never times out. Instances of HTTPConnection which already exist,
	 * are not influenced by this method.
	 *
	 * @param a_timeout The new communication timeout.
	 */
	public synchronized void setTimeout(int a_timeout)
	{
		if (a_timeout < 0)
		{
			a_timeout = 0;
		}
		m_timeout = a_timeout;
	}

	/**
	 * Returns the communication timeout (sec) for new HTTP connections. If this value is zero, a
	 * connection never times out.
	 *
	 * @return The communication timeout for new connections.
	 */
	public synchronized int getTimeout()
	{
		return m_timeout;
	}

	/**
	 * This method creates a new instance of HTTPConnection. The current proxy settings are used.
	 *
	 * @param target The ListenerInterface of the connection target.
	 *
	 * @return A new instance of HTTPConnection with a connection to the specified target and the
	 *         current proxy settings.
	 */
	public synchronized HTTPConnection createHTTPConnection(ListenerInterface target)
	{
		HTTPConnection newConnection = null;
		synchronized (this)
		{
			newConnection = createHTTPConnectionInternal(target);
			if (m_proxyInterface != null && m_proxyInterface.isAuthenticationUsed())
			{
				DefaultAuthHandler.setAuthorizationPrompter(new AuthorizationPrompter()
				{
					public NVPair getUsernamePassword(AuthorizationInfo challenge)
					{
						return new NVPair(m_proxyInterface.getAuthenticationUserID(),
										  m_proxyInterface.getAuthenticationPassword());
					}

				});
			}
		}
		/* set some header infos */
		replaceHeader(newConnection, new NVPair("Cache-Control", "no-cache"));
		replaceHeader(newConnection, new NVPair("Pragma", "no-cache"));
		newConnection.setAllowUserInteraction(false);
		/* set the timeout for all network operations */
		newConnection.setTimeout(getTimeout() * 1000);
		return newConnection;
	}

	/**
	 * This method creates a new instance of HTTPConnection using the specified proxy settings
	 * (ignoring the default settings).
	 *
	 * @param target The ListenerInterface of the connection target.
	 * @param a_proxySettings The proxy settings to use for this single connection. If the proxy
	 *                        settings are null, no proxy is used.
	 *
	 * @return A new instance of HTTPConnection with a connection to the specified target and the
	 *         current proxy settings.
	 */
	public synchronized HTTPConnection createHTTPConnection(ListenerInterface target,
		ImmutableProxyInterface a_proxySettings)
	{
		/* tricky: change the global proxy settings, create the connection and restore the original
		 * proxy settings -> no problem because all methods are synchronized
		 */
		ImmutableProxyInterface oldProxySettings = m_proxyInterface;
		setNewProxySettings(a_proxySettings);
		HTTPConnection createdConnection = createHTTPConnection(target);
		setNewProxySettings(oldProxySettings);
		return createdConnection;
	}

	/**
	 * An internal helper function to set the header information for the HTTP connection.
	 *
	 * @param connection The connection where the new headers are set.
	 * @param header The header information to set.
	 */
	private static void replaceHeader(HTTPConnection connection, NVPair header)
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
			for (int i = 0; i < headers.length; i++)
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
			NVPair tmpHeaders[] = new NVPair[headers.length + 1];
			System.arraycopy(headers, 0, tmpHeaders, 0, headers.length);
			tmpHeaders[headers.length] = header;
			connection.setDefaultHeaders(tmpHeaders);
		}
	}

	/**
	 * This method is used to change the type of the created HTTPConnections.
	 * A call of this method deletes the current factory instance and can be used
	 * as a reset operation, too.
	 * This method is used for testing purposes. PLEASE DO NOT REMOVE IT!
	 * @param a_HTTPConnectionClass the class of generated HTTPConnections
	 */
	private static void setHTTPConnectionClass(Class a_HTTPConnectionClass)
	{
		if (HTTPConnection.class.isAssignableFrom(a_HTTPConnectionClass))
		{
			ms_httpConnectionFactoryInstance = null;
			ms_HTTPConnectionClass = a_HTTPConnectionClass;
		}
		else
		{
			throw new IllegalArgumentException("This is not a valid HTTPConnection class: "
											   + a_HTTPConnectionClass);
		}
	}

	/**
	 * Returns the recently created HTTPConnections.
	 * This method is used for testing purposes. PLEASE DO NOT REMOVE IT!
	 * @return Vector
	 */
	private Vector getCreatedHTTPConnections()
	{
		return m_vecHTTPConnections;
	}

	/**
	 * Creates an HTTPConnection with the listener settings.
	 * @param target the basic listener settings for this connection
	 * @return an HTTPConnection with the listener settings
	 */
	private HTTPConnection createHTTPConnectionInternal(ListenerInterface target)
	{
		HTTPConnection connection;

		if (ms_HTTPConnectionClass == HTTPConnection.class)
		{
			// create standard HTTPConnections the easy way for performance reasons
			connection = new HTTPConnection(target.getHost(), target.getPort());
		}
		else
		{
			// create a generic HTTPConnection, for example for testing purposes
			Class[] paramTypes = new Class[2];
			Object[] params = new Object[2];
			paramTypes[0] = String.class;
			paramTypes[1] = int.class;
			params[0] = target.getHost();
			params[1] = new Integer(target.getPort());
			try
			{
				connection = (HTTPConnection) ms_HTTPConnectionClass.getConstructor(
					paramTypes).newInstance(params);
			}
			catch (Exception a_e)
			{
				throw new IllegalArgumentException("Could not construct an HTTPConnection! " + a_e);
			}
			// save the created connection for testing purposes
			if (m_vecHTTPConnections == null)
			{
				m_vecHTTPConnections = new Vector();
			}
			m_vecHTTPConnections.addElement(connection);
		}
		return connection;
	}
}
