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

/* Hint: This file may be only a copy of the original file which is always in the JAP source tree!
 * If you change something - do not forget to add the changes also to the JAP source tree!
 */

package anon.infoservice;

import java.util.StringTokenizer;
import java.net.InetAddress;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import anon.util.XMLUtil;
import anon.util.Util;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/**
 * Saves the information about a network server.
 */
public class ListenerInterface implements ImmutableListenerInterface
{
   /**
	 * This is the host of this interface (hostname or IP).
	 */
	private String m_strInetHost;

	/**
	 * This is the representation of the port of the ListenerInterface.
	 */
	private int m_iInetPort;

	/**
	 * This describes the protocol type.
	 */
	private String m_strProtocolType;

	/**
	 * This describes, whether we can reach this interface or not. If we can't get a connection
	 * to this interface, we set it to false and so there are no more connection trys. The
	 * interface still remains in the database because others may get a connection to this
	 * interface (causes can be firewalls, another network, ...). This is only meaningful for
	 * non-local (remote) interfaces.
	 */
	private boolean m_bIsReachable;

	/**
	 * Creates a new ListenerInterface from XML description (ListenerInterface node).
	 * @todo remove the ip host
	 * @param listenerInterfaceNode The ListenerInterface node from an XML document.
	 */
	public ListenerInterface(Element listenerInterfaceNode)
	{
		String strHostname;

		setProtocol(XMLUtil.parseNodeString(
				  XMLUtil.getFirstChildByName(listenerInterfaceNode, "Type"), null));

		setPort(XMLUtil.parseNodeInt(
				  XMLUtil.getFirstChildByName(listenerInterfaceNode, "Port"), -1));

		Node hostNode = XMLUtil.getFirstChildByName(listenerInterfaceNode, "Host");
		Node ipNode = XMLUtil.getFirstChildByName(listenerInterfaceNode, "IP");
		if (hostNode == null && ipNode == null)
		{
			throw (new IllegalArgumentException(
				"ListenerInterface: Error in XML structure -- Neither Host nor IP are given."));
		}
		//The value give in Host supersedes the one given by IP
		strHostname = XMLUtil.parseNodeString(hostNode, null);
		if (!isValidHostname(strHostname))
		{
			strHostname = XMLUtil.parseNodeString(ipNode, null);
			if (!isValidIP(strHostname))
			{
				throw (new IllegalArgumentException(
					"ListenerInterface: Error in XML structure -- Invalid Host and IP."));
			}
		}

		setHostname(strHostname);

		m_bIsReachable = true;
	}

	/**
	 * Creates a new ListenerInterface from a hostname / IP address and a port.
	 *
	 * @param a_hostname The hostname or the IP address of this interface.
	 * @param a_port The port of this interface (1 <= port <= 65535).
	 * @exception IllegalArgumentException if an illegal host name or port was given
	 */
	public ListenerInterface(String a_hostname, int a_port) throws IllegalArgumentException
	{
		this(a_hostname, a_port, PROTOCOL_TYPE_HTTP);
	}

	/**
	 * Creates a new ListenerInterface from a hostname / IP address, a port and a protocol
	 * information.
	 *
	 * @param a_hostname The hostname or the IP address of this interface.
	 * @param a_port The port of this interface (1 <= port <= 65535).
	 * @param a_protocol The protocol information. Invalid protocols are replaced by http.
	 * @exception IllegalArgumentException if an illegal host name, port or protocol was given
	 */
	public ListenerInterface(String a_hostname, int a_port, String a_protocol)
		throws IllegalArgumentException
	{
		setHostname(a_hostname);
		setPort(a_port);
		setProtocol(a_protocol);

		m_bIsReachable = true;
	}

	/**
	 * Returns if the given port is valid.
	 * @param a_port a port number
	 * @return true if the given port is valid; false otherwise
	 */
	public static boolean isValidPort(int a_port)
	{
		if ((a_port < 1) || (a_port > 65536))
		{
			return false;
		}
		return true;
	}

	/**
	 * Returns if the given protocol is valid web protocol and can be recognized by
	 * recognizeProtocol().
	 * @param a_protocol a web protocol
	 * @see recognizeProtocol(String)
	 * @return true if the given protocol is valid web protocol; false otherwise
	 */
	public static boolean isValidProtocol(String a_protocol)
	{
		return recognizeProtocol(a_protocol) != null;
	}

	/**
	 * Returns if the given host name is valid.
	 * @param a_hostname a host name
	 * @return true if the given host name is valid; false otherwise
	 */
	public static boolean isValidHostname(String a_hostname)
	{
		return ((a_hostname != null) && a_hostname.length() > 0);
	}

	/**
	 * Returns if the given IP address is valid.
	 * @param a_ipAddress an IP address
	 * @return true if the given IP address is valid; false otherwise
	 */
	public static boolean isValidIP(String a_ipAddress)
	{
		StringTokenizer tokenizer;

		if ((a_ipAddress == null) || (a_ipAddress.indexOf('-') != -1))
		{
			return false;
		}

		tokenizer = new StringTokenizer(a_ipAddress,".");

		try
		{
			// test if the IP could be IPv4 or IPv6
			if ((tokenizer.countTokens() != 4) && (tokenizer.countTokens() != 16))
			{
				throw new NumberFormatException();
			}

			while (tokenizer.hasMoreTokens())
			{
				if (new Integer(tokenizer.nextToken()).intValue() > 255)
				{
					throw new NumberFormatException();
				}
			}
		}
		catch (NumberFormatException a_e)
		{
			return false;
		}

		return true;
	}

	/**
	 * Gets the protocol of this ListenerInterface.
	 * @return the protocol of this ListenerInterface
	 */
	public String getProtocol()
	{
		return m_strProtocolType;
	}

	/**
	 * Get the host (hostname or IP) of this interface as a String.
	 *
	 * @return The host of this interface.
	 */
	public String getHost()
	{
		return m_strInetHost;
	}

	/**
	 * Get the port of this interface.
	 * @return The port of this interface.
	 */
	public int getPort()
	{
		return m_iInetPort;
	}

	/**
	 * Tests if two ListenerInterface instances are equal.
	 * @param a_listenerInterface a ListenerInterface
	 * @return true if the two ListenerInterface instances are equal; false otherwise
	 */
	public boolean equals(ListenerInterface a_listenerInterface)
	{
		if (!getHost().equals(a_listenerInterface.getHost()) ||
			getPort() != a_listenerInterface.getPort() ||
			!getProtocol().equals(a_listenerInterface.getProtocol()))
		{
			return false;
		}
		return true;
	}

	/**
	 * Creates an XML node without signature for this ListenerInterface.
	 *
	 * @todo Remove the parts that contruct the tag <IP> and the ipnode respectivly.
	 *       They are used by InfoService:MixCascadeDBEntry
	 *       and are only needed for compatibility with JAP < 00.02.034.
	 *
	 * @param doc The XML document, which is the environment for the created XML node.
	 * @return The ListenerInterface XML node.
	 */
	public Element toXmlNode(Document doc)
	{
		Element listenerInterfaceNode = doc.createElement(
				  Util.getClassNameWithoutPackage(getClass()));
		/* Create the child nodes of ListenerInterface (Type, Port, Host) */
		Element typeNode = doc.createElement("Type");
		typeNode.appendChild(doc.createTextNode(m_strProtocolType));
		Element portNode = doc.createElement("Port");
		portNode.appendChild(doc.createTextNode(Integer.toString(m_iInetPort)));
		Element hostNode = doc.createElement("Host");
		hostNode.appendChild(doc.createTextNode(m_strInetHost));
		String ipString = null;
		try
		{
			InetAddress interfaceAddress = InetAddress.getByName(m_strInetHost);
			ipString = interfaceAddress.getHostAddress();
		}
		catch (Exception e)
		{
			/* maybe inetHost is a hostname and no IP, but this solution is better than nothing */
			ipString = m_strInetHost;
		}
		Element ipNode = doc.createElement("IP");
		ipNode.appendChild(doc.createTextNode(ipString));
		listenerInterfaceNode.appendChild(typeNode);
		listenerInterfaceNode.appendChild(portNode);
		listenerInterfaceNode.appendChild(hostNode);
		listenerInterfaceNode.appendChild(ipNode);
		return listenerInterfaceNode;
	}

	/**
	 * If we can't reach this interface, we call this function to prevent further connection trys.
	 */
	public void invalidate()
	{
		m_bIsReachable = false;
	}

	/**
	 * Get the validity of this interface.
	 * @return Whether this interface is valid or not.
	 */
	public boolean isValid()
	{
		return m_bIsReachable;
	}

	/**
	 * Returns a String equal to getHost(). If getHost() is an IP, we try to find the hostname
	 * and add it in brackets. If getHost() is a hostname, we try to find the IP and add
	 * it in brackets. If we can't resolve getHost() (IP or hostname), only getHost() without
	 * the additional information is returned.
	 *
	 * @return The host of this interface with additional information.
	 */
	public String getHostAndIp()
	{
		String hostAndIp = m_strInetHost;
		try
		{
			InetAddress interfaceAddress = InetAddress.getByName(m_strInetHost);
			if (isValidIP(m_strInetHost))
			{
				/* inetHost is an IP, try to add the hostname */
				String hostName = interfaceAddress.getHostName();
				if ( (!hostName.equals(m_strInetHost)) && (isValidHostname(hostName)))
				{
					/* we got the hostname via DNS, add it */
					hostAndIp = hostAndIp + " (" + hostName + ")";
				}
			}
			else
			{
				/* inetHost is a hostname, add the IP */
				hostAndIp = hostAndIp + " (" + interfaceAddress.getHostAddress() + ")";
			}
		}
		catch (java.net.UnknownHostException e)
		{
			/* can't resolve inetHost, maybe we are behind a proxy, return only inetHost */
		}
		return hostAndIp;
	}

	/**
	 * Transforms a given protocol into a valid protocol if recognized.
	 * @param a_protocol a protocol
	 * @return a valid protocol or null if not recognized
	 */
	private static String recognizeProtocol(String a_protocol)
	{
		String protocol = null;

		if (a_protocol != null)
		{
			if (a_protocol.equalsIgnoreCase(PROTOCOL_TYPE_HTTP))
			{
				protocol = PROTOCOL_TYPE_HTTP;
			}
			else if (a_protocol.equalsIgnoreCase(PROTOCOL_TYPE_HTTPS))
			{
				protocol = PROTOCOL_TYPE_HTTPS;
			}
			else if (a_protocol.equalsIgnoreCase(PROTOCOL_TYPE_SOCKS))
			{
				protocol = PROTOCOL_TYPE_SOCKS;
			}
		}

		return protocol;
	}

	/**
	 * Sets the protocol.
	 * @param a_protocol a protocol
	 */
	private void setProtocol(String a_protocol)
	{
		if (!isValidProtocol(a_protocol))
		{
			LogHolder.log(LogLevel.NOTICE, LogType.NET, "ListenerInterface: Invalid protocol " +
						  a_protocol + "!");
			m_strProtocolType = PROTOCOL_TYPE_HTTP;
		}
		else
		{
			m_strProtocolType = recognizeProtocol(a_protocol);
		}
	}

	/**
	 * Sets the port number.
	 * @param a_port a port number
	 */
	private void setPort(int a_port)
	{
		if (!isValidPort(a_port))
		{
			throw (new IllegalArgumentException("ListenerInterface: Port is invalid."));
		}
		m_iInetPort = a_port;
	}

	/**
	 * Sets the host name.
	 * @param a_hostname a host name
	 */
	private void setHostname(String a_hostname)
	{
		if (!isValidHostname(a_hostname))
		{
			throw (new IllegalArgumentException("ListenerInterface: Host is invalid."));
		}
		m_strInetHost = a_hostname;
	}
}
