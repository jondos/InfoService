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
	 * This is the IP address string for this interface.
	 */
	private String ipString;

	/**
	 * This is the hostname of this interface.
	 */
	private String hostName;

	/**
	 * This is the representation of the port of the ListenerInterface.
	 */
	private int inetPort;

	/**
	 * This describes the protocol type. It is just a comfort value.
	 */
	private String protocolType;

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
		NodeList ipNodes = listenerInterfaceNode.getElementsByTagName("IP");
		if (ipNodes.getLength() == 0)
		{
			throw (new Exception("ListenerInterface: Error in XML structure."));
		}
		Element ipNode = (Element) (ipNodes.item(0));
		String tmpIpString = null;
		if (ipNode.getFirstChild() != null)
		{
			/* it seems to be, that if you add an empty String as an child node, that no child node is
			 * added
			 */
			tmpIpString = ipNode.getFirstChild().getNodeValue();
		}
		NodeList hostNodes = listenerInterfaceNode.getElementsByTagName("Host");
		if (hostNodes.getLength() == 0)
		{
			throw (new Exception("ListenerInterface: Error in XML structure."));
		}
		Element hostNode = (Element) (hostNodes.item(0));
		String tmpHostName = hostNode.getFirstChild().getNodeValue();
		try
		{
			if ( (tmpIpString == null) || (tmpIpString.equals("")))
			{
				/* if we would use InetAddress.getByName(), we would get the address of the localhost ->
				 * throw an exception to skip the IP test
				 */
				throw (new Exception("ListenerInterface: No IP Address. Skip IP check."));
			}
			/* check the validity of the IP address */
			InetAddress interfaceAddress = InetAddress.getByName(tmpIpString);
			this.ipString = interfaceAddress.getHostAddress();
			this.hostName = interfaceAddress.getHostName();
		}
		catch (Exception e)
		{
			try
			{
				/* IP address is invalid -> try the hostname */
				InetAddress interfaceAddress = InetAddress.getByName(tmpHostName);
				this.ipString = interfaceAddress.getHostAddress();
				this.hostName = interfaceAddress.getHostName();
			}
			catch (Exception e2)
			{
				/* The address could not be resolved, maybe we use a proxy and don't have a DNS server.
				 * Believe that the hostName is correct.
				 */
				if (tmpIpString == "")
				{
					/* Can be, if we have stored a user defined interface and now load it again from XML
					 * file. We only have to do this with the IP because it is the prefered info. JAP tries
					 * always the IP first, if it is null, it takes the hostname.
					 */
					tmpIpString = null;
				}
				this.ipString = tmpIpString;
				this.hostName = tmpHostName;
			}
		}
	}

	/**
	 * Creates a new ListenerInterface from a hostname / IP address and a port. If you supply a
	 * hostname, there will be performed a lookup after the IP address. If you supply an IP address,
	 * there will be performed a lookup after the hostname. If lookup is not successful (because you
	 * are behind a proxy and don't have DNS), we believe that the hostName is ok (no problem too,
	 * if it is an IP address) and set the IP address to null.
	 *
	 * @param hostName The hostname or the IP address of this interface.
	 * @param port The port of this interface (1 <= port <= 65535).
	 */
	public ListenerInterface(String hostName, int port) throws Exception
	{
		if ( (port < 1) || (port > 65535))
		{
			throw (new Exception("ListenerInterface: Port is invalid."));
		}
		try
		{
			InetAddress interfaceAddress = InetAddress.getByName(hostName);
			this.ipString = interfaceAddress.getHostAddress();
			this.hostName = interfaceAddress.getHostName();
		}
		catch (Exception e)
		{
			/* The address could not be resolved, maybe we use a proxy and don't have a DNS server.
			 * Believe that the hostName is correct.
			 */
			this.ipString = null;
			this.hostName = hostName;
		}
		this.inetPort = port;
		this.protocolType = "unknown";
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
		/* Create the child nodes of ListenerInterface (Type, Port, IP, Host) */
		Element typeNode = doc.createElement("Type");
		typeNode.appendChild(doc.createTextNode(protocolType));
		Element portNode = doc.createElement("Port");
		portNode.appendChild(doc.createTextNode(Integer.toString(inetPort)));
		Element ipNode = doc.createElement("IP");
		String tmpIpString = ipString;
		if (tmpIpString == null)
		{
			tmpIpString = "";
		}
		ipNode.appendChild(doc.createTextNode(tmpIpString));
		Element hostNode = doc.createElement("Host");
		hostNode.appendChild(doc.createTextNode(hostName));
		listenerInterfaceNode.appendChild(typeNode);
		listenerInterfaceNode.appendChild(portNode);
		listenerInterfaceNode.appendChild(ipNode);
		listenerInterfaceNode.appendChild(hostNode);
		return listenerInterfaceNode;
	}

	/**
	 * Get the IP address of this interface as a String.
	 *
	 * @return The IP address String of this interface.
	 */
	public String getIpString()
	{
		return ipString;
	}

	/**
	 * Get the hostname of this interface as a String.
	 *
	 * @return The hostname of this interface.
	 */
	public String getHostName()
	{
		return hostName;
	}

	/**
	 * Get the port of this interface.
	 * @return The port of this interface.
	 */
	public int getPort()
	{
		return inetPort;
	}
}
