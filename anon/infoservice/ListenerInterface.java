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
import org.w3c.dom.NodeList;

/**
 * Saves the information about one listener-interface IP / hostname and port.
 */
public class ListenerInterface
{

	/**
	 * This is the host of this interface (hostname or IP).
	 */
	private String inetHost;

	/**
	 * This is the representation of the port of the ListenerInterface.
	 */
	private int inetPort;

	/**
	 * This describes the protocol type. It is just a comfort value.
	 */
	private String protocolType;

	/**
	 * This describes, whether we can reach this interface or not. If we can't get a connection
	 * to this interface, we set it to false and so there are no more connection trys. The
	 * interface still remains in the database because others may get a connection to this
	 * interface (causes can be firewalls, another network, ...). This is only meaningful for
	 * non-local (remote) interfaces.
	 */
	private boolean valid;

	/**
	 * Creates a new ListenerInterface from XML description (ListenerInterface node).
	 *
	 * @param listenerInterfaceNode The ListenerInterface node from an XML document.
	 */
	public ListenerInterface(Element listenerInterfaceNode) throws Exception
	{
		NodeList typeNodes = listenerInterfaceNode.getElementsByTagName("Type");
		if (typeNodes.getLength() == 0)
		{
			throw (new Exception("ListenerInterface: Error in XML structure."));
		}
		Element typeNode = (Element) (typeNodes.item(0));
		protocolType = typeNode.getFirstChild().getNodeValue();
		NodeList portNodes = listenerInterfaceNode.getElementsByTagName("Port");
		if (portNodes.getLength() == 0)
		{
			throw (new Exception("ListenerInterface: Error in XML structure."));
		}
		Element portNode = (Element) (portNodes.item(0));
		inetPort = Integer.parseInt(portNode.getFirstChild().getNodeValue());
		if ( (inetPort < 1) || (inetPort > 65535))
		{
			throw (new Exception("ListenerInterface: Port is invalid."));
		}
		NodeList hostNodes = listenerInterfaceNode.getElementsByTagName("Host");
		if (hostNodes.getLength() == 0)
		{
			throw (new Exception("ListenerInterface: Error in XML structure."));
		}
		Element hostNode = (Element) (hostNodes.item(0));
		inetHost = hostNode.getFirstChild().getNodeValue();
		valid = true;
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
		inetPort = port;
		inetHost = host;
		protocolType = "unknown";
		valid = true;
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
		protocolType = protocol;
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
		typeNode.appendChild(doc.createTextNode(protocolType));
		Element portNode = doc.createElement("Port");
		portNode.appendChild(doc.createTextNode(Integer.toString(inetPort)));
		Element hostNode = doc.createElement("Host");
		hostNode.appendChild(doc.createTextNode(inetHost));
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
		typeNode.appendChild(doc.createTextNode(protocolType));
		Element portNode = doc.createElement("Port");
		portNode.appendChild(doc.createTextNode(Integer.toString(inetPort)));
		Element hostNode = doc.createElement("Host");
		hostNode.appendChild(doc.createTextNode(inetHost));
		String ipString = null;
		try
		{
			InetAddress interfaceAddress = InetAddress.getByName(inetHost);
			ipString = interfaceAddress.getHostAddress();
		}
		catch (Exception e)
		{
			/* maybe inetHost is a hostname and no IP, but this solution is better than nothing */
			ipString = inetHost;
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
		return inetHost;
	}

	/**
	 * Get the port of this interface.
	 * @return The port of this interface.
	 */
	public int getPort()
	{
		return inetPort;
	}

	/**
	 * If we can't reach this interface, we call this function to prevent further connection trys.
	 */
	public void invalidate()
	{
		valid = false;
	}

	/**
	 * Get the validity of this interface.
	 * @return Whether this interface is valid or not.
	 */
	public boolean isValid()
	{
		return valid;
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
		String r_HostAndIp = inetHost;
		try
		{
			InetAddress interfaceAddress = InetAddress.getByName(inetHost);
			String ipString = interfaceAddress.getHostAddress();
			if (ipString.equals(inetHost))
			{
				/* inetHost is an IP, try to add the hostname */
				String hostName = interfaceAddress.getHostName();
				if ( (!hostName.equals(inetHost)) && (!hostName.equals("")))
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
