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

import java.net.InetAddress;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import anon.util.XMLUtil;

/**
 * Saves the information about one listener-interface IP / hostname and port.
 */
public final class ListenerInterface
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
	 * This describes the protocol type. It is just a comfort value.
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
	 *
	 * @param listenerInterfaceNode The ListenerInterface node from an XML document.
	 */
	public ListenerInterface(Element listenerInterfaceNode) throws Exception
	{
		Node typeNode = XMLUtil.getFirstChildByName(listenerInterfaceNode, "Type");
		m_strProtocolType = XMLUtil.parseNodeString(typeNode, null);
		if (m_strProtocolType == null)
		{
			throw (new Exception("ListenerInterface: Error in XML structure -- Type not specified"));
		}
		Node portNode = XMLUtil.getFirstChildByName(listenerInterfaceNode, "Port");
		m_iInetPort = XMLUtil.parseNodeInt(portNode, -1);
		if ( (m_iInetPort < 1) || (m_iInetPort > 65535))
		{
			throw (new Exception("ListenerInterface: Port is invalid."));
		}
		Node hostNode = XMLUtil.getFirstChildByName(listenerInterfaceNode, "Host");
		Node ipNode = XMLUtil.getFirstChildByName(listenerInterfaceNode, "IP");
		if (hostNode == null && ipNode == null)
		{
			throw (new Exception(
				"ListenerInterface: Error in XML structure -- Neither Host nor IP are given."));
		}
		//The value give in Host supersedes the one given by IP
		m_strInetHost = XMLUtil.parseNodeString(hostNode, null);
		if (m_strInetHost == null || m_strInetHost.length() == 0)
		{
			m_strInetHost = XMLUtil.parseNodeString(ipNode, null);
		}
		if (m_strInetHost == null || m_strInetHost.length() == 0)
		{
			throw (new Exception(
				"ListenerInterface: Error in XML structure -- Neither Host nor IP are given."));
		}
		m_bIsReachable = true;
	}

	/**
	 * Creates a new ListenerInterface from a hostname / IP address and a port.
	 *
	 * @param hostName The hostname or the IP address of this interface.
	 * @param port The port of this interface (1 <= port <= 65535).
	 */
	public ListenerInterface(String host, int port) throws Exception
	{
		if ( (port < 1) || (port > 65535))
		{
			throw (new Exception("ListenerInterface: Port is invalid."));
		}
		if (host == null)
		{
			throw (new Exception("ListenerInterface: Host is invalid."));
		}
		m_iInetPort = port;
		m_strInetHost = host;
		m_strProtocolType = "unknown";
		m_bIsReachable = true;
	}

	/**
	 * Creates a new ListenerInterface from a hostname / IP address, a port and a protocol
	 * information. The protocol information does not have any function. It is just a bonus
	 * infromation.
	 *
	 * @param hostName The hostname or the IP address of this interface.
	 * @param port The port of this interface (1 <= port <= 65535).
	 * @param protocol The protocol information.
	 */
	public ListenerInterface(String host, int port, String protocol) throws Exception
	{
		this(host, port);
		m_strProtocolType = protocol;
	}

	/**
	 * Creates an XML node without signature for this ListenerInterface.
	 *
	 * @param doc The XML document, which is the environment for the created XML node.
	 *
	 * @return The ListenerInterface XML node.
	 */
	public Element toXmlNode(Document doc)
	{
		Element listenerInterfaceNode = doc.createElement("ListenerInterface");
		/* Create the child nodes of ListenerInterface (Type, Port, Host) */
		Element typeNode = doc.createElement("Type");
		typeNode.appendChild(doc.createTextNode(m_strProtocolType));
		Element portNode = doc.createElement("Port");
		portNode.appendChild(doc.createTextNode(Integer.toString(m_iInetPort)));
		Element hostNode = doc.createElement("Host");
		hostNode.appendChild(doc.createTextNode(m_strInetHost));
		listenerInterfaceNode.appendChild(typeNode);
		listenerInterfaceNode.appendChild(portNode);
		listenerInterfaceNode.appendChild(hostNode);
		return listenerInterfaceNode;
	}

	/**
	 * Creates an XML node (old format) without signature for this ListenerInterface.
	 * This method is used by InfoService:MixCascadeDBEntry.
	 * @todo Remove this method. It is only needed for compatibility with JAP < 00.02.034.
	 *
	 * @param doc The XML document, which is the environment for the created XML node.
	 *
	 * @return The ListenerInterface XML node.
	 */
	public Element toOldXmlNode(Document doc)
	{
		Element listenerInterfaceNode = doc.createElement("ListenerInterface");
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
		String r_HostAndIp = m_strInetHost;
		try
		{
			InetAddress interfaceAddress = InetAddress.getByName(m_strInetHost);
			String ipString = interfaceAddress.getHostAddress();
			if (ipString.equals(m_strInetHost))
			{
				/* inetHost is an IP, try to add the hostname */
				String hostName = interfaceAddress.getHostName();
				if ( (!hostName.equals(m_strInetHost)) && (!hostName.equals("")))
				{
					/* we got the hostname via DNS, add it */
					r_HostAndIp = r_HostAndIp + " (" + hostName + ")";
				}
			}
			else
			{
				/* inetHost is a hostname, add the IP */
				r_HostAndIp = r_HostAndIp + " (" + ipString + ")";
			}
		}
		catch (Exception e)
		{
			/* can't resolve inetHost, maybe we are behind a proxy, return only inetHost */
		}
		return r_HostAndIp;
	}

}
