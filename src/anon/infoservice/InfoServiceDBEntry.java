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

import java.io.IOException;
import java.util.Enumeration;
import java.util.Vector;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import HTTPClient.HTTPConnection;
import HTTPClient.HTTPResponse;
import anon.ErrorCodes;
import anon.crypto.JAPCertPath;
import anon.crypto.JAPCertificate;
import anon.crypto.JAPCertificateStore;
import anon.crypto.JAPSignature;
import anon.util.XMLUtil;
import anon.util.IXMLEncodable;
import anon.util.XMLParseException;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/**
 * Holds the information for an infoservice.
 */
public class InfoServiceDBEntry extends DatabaseEntry implements IXMLEncodable
{
	/**
	 * This is the ID of this infoservice.
	 */
	private String m_strInfoServiceId;

	/**
	 * The name of the infoservice.
	 */
	private String m_strName;

	/**
	 * Some information about the used infoservice software.
	 */
	private ServiceSoftware m_infoserviceSoftware;

	/**
	 * The ListenerInterfaces of all interfaces (internet-address and port) this
	 * infoservice is (virtually) listening on.
	 */
	private Vector m_listenerInterfaces;

	/**
	 * Stores the number of the prefered ListenerInterface in the listenerInterfaces list. If we
	 * have to connect to the infoservice, this interface is used first. If there is an connection
	 * error, all other interfaces will be tested. If we can get the connection over another
	 * interface, this interface will be set as new preferedListenerInterface.
	 */
	private int preferedListenerInterface;

	/**
	 * Stores whether this infoservice has a primary forwarder list (true) or not (false).
	 */
	private boolean m_bPrimaryForwarderList;

	/**
	 * Creates an XML node (InfoServices node) with all infoservices from the database inside.
	 * The Database does not need to the one registered in the central registry.
	 *
	 * @param a_doc The XML document, which is the environment for the created XML node.
	 * @param a_database the database that contains the InfoServiceDBEntries
	 *
	 * @return The InfoServices XML node.
	 */
	public static Element toXmlElement(Document a_doc, Database a_database)
	{
		Object dbentry;

		Element infoServicesNode = a_doc.createElement("InfoServices");
		Vector infoServices = a_database.getEntryList();
		Enumeration it = infoServices.elements();
		while (it.hasMoreElements())
		{
			dbentry = it.nextElement();
			if (dbentry instanceof InfoServiceDBEntry) {
				infoServicesNode.appendChild( ( (InfoServiceDBEntry) (dbentry)).toXmlElement(a_doc));
			}
		}
		return infoServicesNode;
	}

	/**
	 * Adds all infoservices, which are childs of the InfoServices node, to the database.
	 *
	 * @param infoServicesNode The InfoServices node.
	 * @param a_database the database that contains the InfoServiceDBEntries
	 */
	public static void loadFromXml(Element infoServicesNode, Database a_database)
	{
		NodeList infoServiceNodes = infoServicesNode.getElementsByTagName("InfoService");
		for (int i = 0; i < infoServiceNodes.getLength(); i++)
		{
			/* add all childs to the database */
			try
			{
				a_database.update(new InfoServiceDBEntry( (Element) (infoServiceNodes.item(i))));
			}
			catch (Exception e)
			{
				LogHolder.log(LogLevel.WARNING, LogType.MISC, "Could not load db entries from XML! " + e);
				/* if there was an error, it does not matter */
			}
		}
	}



	/**
	 * Creates a new InfoService from XML description (InfoService node).
	 *
	 * @param infoServiceNode The InfoService node from an XML document.
	 * @exception XMLParseException if an error in the xml structure occurs
	 */
	public InfoServiceDBEntry(Element infoServiceNode) throws XMLParseException
	{
		super(System.currentTimeMillis() + Constants.TIMEOUT_INFOSERVICE_JAP);
		/* get the ID */
		m_strInfoServiceId = infoServiceNode.getAttribute("id");
		/* get the name */
		NodeList nameNodes = infoServiceNode.getElementsByTagName("Name");
		if (nameNodes.getLength() == 0)
		{
			throw (new XMLParseException("Name"));
		}
		Element nameNode = (Element) (nameNodes.item(0));
		m_strName = nameNode.getFirstChild().getNodeValue();
		/* get the software information */
		NodeList softwareNodes = infoServiceNode.getElementsByTagName("Software");
		if (softwareNodes.getLength() == 0)
		{
			throw (new XMLParseException("Software"));
		}
		Element softwareNode = (Element) (softwareNodes.item(0));
		m_infoserviceSoftware = new ServiceSoftware(softwareNode);
		/* get the listener interfaces */
		NodeList networkNodes = infoServiceNode.getElementsByTagName("Network");
		if (networkNodes.getLength() == 0)
		{
			throw (new XMLParseException("Network"));
		}
		Element networkNode = (Element) (networkNodes.item(0));
		NodeList listenerInterfacesNodes = networkNode.getElementsByTagName("ListenerInterfaces");
		if (listenerInterfacesNodes.getLength() == 0)
		{
			throw (new XMLParseException("ListenerInterfaces"));
		}
		Element listenerInterfacesNode = (Element) (listenerInterfacesNodes.item(0));
		NodeList listenerInterfaceNodes = listenerInterfacesNode.getElementsByTagName("ListenerInterface");
		if (listenerInterfaceNodes.getLength() == 0)
		{
			throw new XMLParseException("ListenerInterfaces");
		}
		m_listenerInterfaces = new Vector();
		for (int i = 0; i < listenerInterfaceNodes.getLength(); i++)
		{
			Element listenerInterfaceNode = (Element) (listenerInterfaceNodes.item(i));
			m_listenerInterfaces.addElement(new ListenerInterface(listenerInterfaceNode));
		}
		/* set the first interface as prefered interface */
		preferedListenerInterface = 0;
		/* get the Expire timestamp */
		NodeList expireNodes = infoServiceNode.getElementsByTagName("Expire");
		if (expireNodes.getLength() == 0)
		{
			throw new XMLParseException("Expire");
		}
		Element expireNode = (Element) (expireNodes.item(0));
		//setExpireTime(Long.parseLong(expireNode.getFirstChild().getNodeValue()));
		/* get the information, whether this infoservice keeps a list of JAP forwarders */
		NodeList forwarderListNodes = infoServiceNode.getElementsByTagName("ForwarderList");
		if (forwarderListNodes.getLength() == 0)
		{
			/* there is no ForwarderList node -> this infoservice doesn't keep a primary forwarder list
			 */
			m_bPrimaryForwarderList = false;
		}
		else
		{
			/* there is a ForwarderList node -> this infoservice keeps a primary forwarder list */
			m_bPrimaryForwarderList = true;
		}
	}

	/**
	 * Creates a new InfoServiceDBentry. The Name and ID are
	 * set to a generic value derived from the hostname and the port.
	 * The expire time is calculated by using the DEFAULT_EXPIRE_TIME constant.
	 * The software info is set to a dummy value. The "has forwarder list" value is set to false.
	 * That's no problem because such a user-defined infoservice shall only be the initial
	 * infoservice for getting the current list of working infoservices.
	 *
	 * @param a_listeners The listeners the infoservice is (virtually) listening on.
	 */
	public InfoServiceDBEntry(Vector a_listeners)
	{
		this(null, a_listeners);
	}

	/**
	 * Creates a new InfoServiceDBEntry from the hostName / IP and the port. The hostName and port are
	 * directly used for creating the ListenerInterface for this InfoService. The ID (if not given)
	 * is set to a generic value derived from the hostname and the port. If you supply a name for
	 * the infoservice then it will get that name, if you supply null, the name will be of the type
	 * "hostname:port". The expire time is calculated by using the DEFAULT_EXPIRE_TIME constant.
	 * The software info is set to a dummy value. The "has forwarder list" value is set to false.
	 * That's no problem because such a user-defined infoservice shall only be the initial
	 * infoservice for getting the current list of working infoservices.
	 *
	 * @param a_strName The name of the infoservice or null, if a generic name shall be used.
	 * @param a_listeners The listeners the infoservice is (virtually) listening on.
	 */
	public InfoServiceDBEntry(String a_strName, Vector a_listeners)
	{
		super(System.currentTimeMillis() + Constants.TIMEOUT_INFOSERVICE_JAP);

		Enumeration enumListeners = a_listeners.elements();
		m_listenerInterfaces = new Vector();
		while (enumListeners.hasMoreElements())
		{
			m_listenerInterfaces.addElement(enumListeners.nextElement());
		}

		m_strInfoServiceId = generateId();

		/* set a name */
		if (a_strName == null)
		{
			m_strName = generateId();
		}
		else
		{
			m_strName = a_strName;
		}

		m_bPrimaryForwarderList = false;
		m_infoserviceSoftware = new ServiceSoftware("unknown");
		preferedListenerInterface = 0;
	}

	/**
	 * Creates an XML node without signature for this InfoService.
	 *
	 * @param doc The XML document, which is the environment for the created XML node.
	 *
	 * @return The InfoService XML node.
	 */
	public Element toXmlElement(Document doc)
	{
		Element infoServiceNode = doc.createElement("InfoService");
		infoServiceNode.setAttribute("id", m_strInfoServiceId);
		/* Create the child nodes of InfoService (Name, Software, Network, Expire) */
		Element nameNode = doc.createElement("Name");
		nameNode.appendChild(doc.createTextNode(m_strName));
		Element softwareNode = m_infoserviceSoftware.toXmlElement(doc);
		Element networkNode = doc.createElement("Network");
		Element listenerInterfacesNode = doc.createElement("ListenerInterfaces");
		Enumeration it = m_listenerInterfaces.elements();
		while (it.hasMoreElements())
		{
			ListenerInterface currentListenerInterface = (ListenerInterface) (it.nextElement());
			Element currentNode = currentListenerInterface.toXmlElement(doc);
			listenerInterfacesNode.appendChild(currentNode);
		}
		networkNode.appendChild(listenerInterfacesNode);
		Element expireNode = doc.createElement("Expire");
		expireNode.appendChild(doc.createTextNode(Long.toString(getExpireTime())));
		if (m_bPrimaryForwarderList == true)
		{
			/* if we hold a forwarder list, also append an ForwarderList node, at the moment this
			 * node doesn't have any childs
			 */
			Element forwarderListNode = doc.createElement("ForwarderList");
			infoServiceNode.appendChild(forwarderListNode);
		}
		infoServiceNode.appendChild(nameNode);
		infoServiceNode.appendChild(softwareNode);
		infoServiceNode.appendChild(networkNode);
		infoServiceNode.appendChild(expireNode);
		return infoServiceNode;
	}

	/**
	 * This is only for compatibility and will be rewritten next time.
	 * @todo rewrite this
	 * @return Returns an ID for the infoservice (IP:Port of the first listener interface).
	 */
	private String generateId()
	{
		return ( (ListenerInterface) (m_listenerInterfaces.firstElement())).getHost() + "%3A" +
			Integer.toString( ( (ListenerInterface) (m_listenerInterfaces.firstElement())).getPort());
	}

	/**
	 * Returns the ID of the infoservice.
	 *
	 * @return The ID of this infoservice.
	 */
	public String getId()
	{
		return m_strInfoServiceId;
	}

	/**
	 * Returns the name of the infoservice.
	 *
	 * @return The name of this infoservice.
	 */
	public String getName()
	{
		return m_strName;
	}

	/**
	 * Returns, whether this infoservice keeps a list of JAP forwarders (true) or not (false).
	 *
	 * @return Whethet this infoservice keeps a list of JAP forwarders.
	 */
	public boolean hasPrimaryForwarderList()
	{
		return m_bPrimaryForwarderList;
	}

	/**
	 * Returns a snapshot of all listener interfaces of this infoservice.
	 *
	 * @return A Vector with all listener interfaces of this infoservice.
	 */
	public Vector getListenerInterfaces()
	{
		Vector r_listenerInterfacesList = new Vector();
		Enumeration listenerInterfacesEnumeration = m_listenerInterfaces.elements();
		while (listenerInterfacesEnumeration.hasMoreElements())
		{
			r_listenerInterfacesList.addElement(listenerInterfacesEnumeration.nextElement());
		}
		return r_listenerInterfacesList;
	}

	/**
	 * Returns a String representation for this InfoService object. It's just the name of the
	 * infoservice.
	 *
	 * @return The name of this infoservice.
	 */
	public String toString()
	{
		return m_strName;
	}

	/**
	 * Creates a new HTTPConnection to a ListenerInterface from the list of all listener interfaces.
	 * The connection is created to the interface, which follows the interface described in
	 * lastConnectionDescriptor in the list (if it is the last in the list, we begin with the first
	 * again). If you supply null, the return value will be the interface referenced by
	 * preferedListenerInterface. The preferedListenerInterface will be set to the interface, the
	 * new connection points to. So if the connection is successful (--> the last call of this
	 * method has returned a connection to a valid interface), and you want connect again, the
	 * preferedListenerInterface references this last interface with a successful connection.
	 *
	 * @param lastConnectionDescriptor The HTTPConnectionDescriptor of the last connection try (the
	 *                                 last output of this function) or null, if you want a new
	 *                                 connection (connection to preferedListenerInterface is opened).
	 *
	 * @return HTTPConnectionDescriptor with a connection to the next ListenerInterface in the list
	 *         or to the preferedListenerInterface, if you supplied null.
	 */
	private HTTPConnectionDescriptor connectToInfoService(HTTPConnectionDescriptor lastConnectionDescriptor)
	{
		int nextIndex = preferedListenerInterface;
		if (lastConnectionDescriptor != null)
		{
			int lastIndex = m_listenerInterfaces.indexOf(lastConnectionDescriptor.getTargetInterface());
			nextIndex = (lastIndex + 1) % (m_listenerInterfaces.size());
		}
		/* update the preferedListenerInterface */
		preferedListenerInterface = nextIndex;
		/* create the connection descriptor */
		ListenerInterface target = (ListenerInterface) (m_listenerInterfaces.elementAt(nextIndex));
		HTTPConnection connection = HTTPConnectionFactory.getInstance().createHTTPConnection(target);
		return (new HTTPConnectionDescriptor(connection, target));
	}

	/**
	 * Fetches the specified XML document from the infoservice. If a ListenerInterface can't be
	 * reached, automatically a new one is chosen. If we can't reach the infoservice at all, an
	 * Exception is thrown.
	 *
	 * @param a_httpRequest The structure with the HTTP request.
	 *
	 * @return The specified XML document.
	 */
	private Document getXmlDocument(HttpRequestStructure a_httpRequest) throws Exception
	{
		/* make sure, that we are connected */
		int connectionCounter = 0;
		HTTPConnectionDescriptor currentConnectionDescriptor = null;
		while (connectionCounter < m_listenerInterfaces.size())
		{
			/* update the connectionCounter */
			connectionCounter++;
			/* get the next connection descriptor by supplying the last one */
			currentConnectionDescriptor = connectToInfoService(currentConnectionDescriptor);
			HTTPConnection currentConnection = currentConnectionDescriptor.getConnection();
			try
			{
				HTTPResponse response = null;
				if (a_httpRequest.getRequestCommand() == HttpRequestStructure.HTTP_COMMAND_GET)
				{
					LogHolder.log(LogLevel.DEBUG, LogType.NET,
								  "InfoService: getXmlDocument: Get: " + currentConnection.getHost() + ":" +
								  Integer.toString(currentConnection.getPort()) +
								  a_httpRequest.getRequestFileName());
					response = currentConnection.Get(a_httpRequest.getRequestFileName());
				}
				else
				{
					if (a_httpRequest.getRequestCommand() == HttpRequestStructure.HTTP_COMMAND_POST)
					{
						LogHolder.log(LogLevel.DEBUG, LogType.NET,
									  "InfoService: getXmlDocument: Post: " + currentConnection.getHost() +
									  ":" + Integer.toString(currentConnection.getPort()) +
									  a_httpRequest.getRequestFileName());
						String postData = "";
						if (a_httpRequest.getRequestPostDocument() != null)
						{
							postData = XMLUtil.XMLDocumentToString(a_httpRequest.getRequestPostDocument());
						}
						response = currentConnection.Post(a_httpRequest.getRequestFileName(), postData);
					}
					else
					{
						throw (new Exception("InfoService: getXmlDocument: Invalid HTTP command."));
					}
				}
				Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(response.
					getInputStream());
				/* fetching the document was successful, leave this method */
				return doc;
			}
			catch (IOException e)
			{
				/* connection with the current interface was not possible -> connect to another interface;
				 * any other exception is a serious error -> we throw it
				 */
				LogHolder.log(LogLevel.ERR, LogType.NET,
							  "InfoService: getXmlDocument: Connection to infoservice interface failed: " +
							  currentConnection.getHost() + ":" + Integer.toString(currentConnection.getPort()) +
							  a_httpRequest.getRequestFileName());
			}
		}
		/* all interfaces tested, we can't find a valid interface */
		throw (new Exception("Can't connect to infoservice. Connections to all ListenerInterfaces failed."));
	}

	/**
	 * Get a Vector of all mixcascades the infoservice knows. If we can't get a connection with
	 * the infoservice, an Exception is thrown.
	 *
	 * @return The Vector of all mixcascades.
	 */
	public Vector getMixCascades() throws Exception
	{
		Document doc = getXmlDocument(HttpRequestStructure.createGetRequest("/cascades"));
		NodeList mixCascadesNodes = doc.getElementsByTagName("MixCascades");
		if (mixCascadesNodes.getLength() == 0)
		{
			throw (new Exception("InfoService: getMixCascades: Error in XML structure."));
		}
		Element mixCascadesNode = (Element) (mixCascadesNodes.item(0));
		NodeList mixCascadeNodes = mixCascadesNode.getElementsByTagName("MixCascade");
		Vector mixCascades = new Vector();
		for (int i = 0; i < mixCascadeNodes.getLength(); i++)
		{
			Element mixCascadeNode = (Element) (mixCascadeNodes.item(i));
			boolean signatureValid = false;
			try
			{
				checkSignature(mixCascadeNode);
				signatureValid = true;
			}
			catch (Exception e)
			{
				LogHolder.log(LogLevel.EXCEPTION, LogType.MISC, e.getMessage());
			}
			if (signatureValid)
			{
				try
				{
					mixCascades.addElement(new MixCascade(mixCascadeNode));
				}
				catch (Exception e)
				{
					/* an error while parsing the node occured -> we don't use this mixcascade */
					LogHolder.log(LogLevel.EXCEPTION, LogType.MISC,
								  "InfoService: getMixCascades: Error in MixCascade XML node.");
				}
			}
		}
		return mixCascades;
	}

	/**
	 * Get a Vector of all infoservices the infoservice knows. If we can't get a connection with
	 * the infoservice, an Exception is thrown.
	 *
	 * @return The Vector of all infoservices.
	 */
	public Vector getInfoServices() throws Exception
	{
		Document doc = getXmlDocument(HttpRequestStructure.createGetRequest("/infoservices"));
		NodeList infoServicesNodes = doc.getElementsByTagName("InfoServices");
		if (infoServicesNodes.getLength() == 0)
		{
			throw (new Exception("InfoService: getInfoServices: Error in XML structure."));
		}
		Element infoServicesNode = (Element) (infoServicesNodes.item(0));
		NodeList infoServiceNodes = infoServicesNode.getElementsByTagName("InfoService");
		Vector infoServices = new Vector();
		for (int i = 0; i < infoServiceNodes.getLength(); i++)
		{
			Element infoServiceNode = (Element) (infoServiceNodes.item(i));
			boolean signatureValid = false;
			try
			{
				checkSignature(infoServiceNode);
				signatureValid = true;
			}
			catch (Exception e)
			{
				LogHolder.log(LogLevel.EXCEPTION, LogType.MISC, e.getMessage());
			}
			if (signatureValid)
			{
				try
				{
					infoServices.addElement(new InfoServiceDBEntry(infoServiceNode));
				}
				catch (Exception e)
				{
					/* an error while parsing the node occured -> we don't use this mixcascade */
					LogHolder.log(LogLevel.EXCEPTION, LogType.MISC,
								  "InfoService: getInfoServices: Error in InfoService XML node.");
				}
			}
		}
		return infoServices;
	}

	/**
	 * Get the MixInfo for the mix with the given ID. If we can't get a connection with the
	 * infoservice, an Exception is thrown.
	 *
	 * @param mixId The ID of the mix to get the MixInfo for.
	 *
	 * @return The MixInfo for the mix with the given ID.
	 */
	public MixInfo getMixInfo(String mixId) throws Exception
	{
		Document doc = getXmlDocument(HttpRequestStructure.createGetRequest("/mixinfo/" + mixId));
		NodeList mixNodes = doc.getElementsByTagName("Mix");
		if (mixNodes.getLength() == 0)
		{
			throw (new Exception("InfoService: getMixInfo: Error in XML structure."));
		}
		Element mixNode = (Element) (mixNodes.item(0));
		/* if signature is invalid, exception is thrown */
		checkSignature(mixNode);
		return (new MixInfo(mixNode));
	}

	/**
	 * Get the StatusInfo for the cascade with the given ID. If we can't get a connection with the
	 * infoservice, an Exception is thrown.
	 *
	 * @param cascadeId The ID of the mixcascade to get the StatusInfo for.
	 * @param cascadeLength The length of the mixcascade (number of mixes). We need this for
	 *                      calculating the AnonLevel in the StatusInfo.
	 * @param a_additionalCertificates Additional certificates for checking the signature of a
	 *                                 StatusInfo (it is a certificate store with the certificates
	 *                                 of the MixCascade). So there is no need to append the
	 *                                 certificates to the StatusInfo and we can save some bytes
	 *                                 (but it is still possible to append certificates there).
	 *
	 * @return The current StatusInfo for the mixcascade with the given ID.
	 */
	public StatusInfo getStatusInfo(String cascadeId, int cascadeLength,
									JAPCertificateStore a_additionalCertificates) throws Exception
	{
		Document doc = getXmlDocument(HttpRequestStructure.createGetRequest("/mixcascadestatus/" + cascadeId));
		NodeList mixCascadeStatusNodes = doc.getElementsByTagName("MixCascadeStatus");
		if (mixCascadeStatusNodes.getLength() == 0)
		{
			throw (new Exception("InfoService: getStatusInfo: Error in XML structure."));
		}
		Element mixCascadeStatusNode = (Element) (mixCascadeStatusNodes.item(0));
		/* if signature is invalid, exception is thrown */
		try
		{
			/* try to verify the signature against the default certificates or skip all checkings,
			 * if the default JAPCertificateStore is null
			 */
			checkSignature(mixCascadeStatusNode);
		}
		catch (Exception e)
		{
			/* if the check was not successful, check the signature against the certificates from the
			 * MixCascade, if this is also not possible, throw the exception
			 */
			checkSignature(mixCascadeStatusNode, a_additionalCertificates);
		}
		return (new StatusInfo(mixCascadeStatusNode, cascadeLength));
	}

	/**
	 * Get the version String of the current JAP version from the infoservice. This function is
	 * called to check, whether updates of the JAP are available. If we can't get a connection with
	 * the infoservice, an Exception is thrown.
	 *
	 * @return The version String (fromat: nn.nn.nnn) of the current JAP version.
	 */
	public String getNewVersionNumber() throws Exception
	{
		Document doc = getXmlDocument(HttpRequestStructure.createGetRequest("/currentjapversion"));
		NodeList japNodes = doc.getElementsByTagName("Jap");
		if (japNodes.getLength() == 0)
		{
			throw (new Exception("InfoService: getNewVersionNumber: Error in XML structure."));
		}
		Element japNode = (Element) (japNodes.item(0));
		//signature check...
		JAPCertificate cert = InfoServiceHolder.getInstance().getCertificateForUpdateMessages();
		if (cert != null)
		{
			try
			{
				JAPSignature sig = new JAPSignature();
				sig.initVerify(cert.getPublicKey());
				if (!sig.verifyXML(japNode))
				{
					throw (new Exception("InfoService: getNewVersionNumber: Signature check failed!"));
				}

			}
			catch (Exception e)
			{
				throw (new Exception("InfoService: getNewVersionNumber: Signature check failed!"));
			}
		}
		NodeList softwareNodes = japNode.getElementsByTagName("Software");
		if (softwareNodes.getLength() == 0)
		{
			throw (new Exception("InfoService: getNewVersionNumber: Error in XML structure."));
		}
		Element softwareNode = (Element) (softwareNodes.item(0));
		ServiceSoftware currentJapSoftware = new ServiceSoftware(softwareNode);
		String versionString = currentJapSoftware.getVersion();
		if ( (versionString.charAt(2) != '.') || (versionString.charAt(5) != '.'))
		{
			throw (new Exception("InfoService: getNewVersionNumber: Error in XML structure."));
		}
		return versionString;
	}

	/**
	 * Returns the JAPVersionInfo for the specified type. The JAPVersionInfo is generated from
	 * the JNLP files received from the infoservice. If we can't get a connection with the
	 * infoservice, an Exception is thrown.
	 *
	 * @param japVersionType Selects the JAPVersionInfo (release / development). Look at the
	 *                       Constants in JAPVersionInfo.
	 *
	 * @return The JAPVersionInfo of the specified type.
	 */
	public JAPVersionInfo getJAPVersionInfo(int japVersionType) throws Exception
	{
		Document doc = null;
		if (japVersionType == JAPVersionInfo.JAP_RELEASE_VERSION)
		{
			doc = getXmlDocument(HttpRequestStructure.createGetRequest("/japRelease.jnlp"));
		}
		if (japVersionType == JAPVersionInfo.JAP_DEVELOPMENT_VERSION)
		{
			doc = getXmlDocument(HttpRequestStructure.createGetRequest("/japDevelopment.jnlp"));
		}
		return (new JAPVersionInfo(doc, japVersionType));
	}

	/**
	 * Get the list with the tor nodes from the infoservice. If we can't get a connection with the
	 * infoservice or the infoservice doesn't support the tor nodes list download, an Exception is
	 * thrown.
	 *
	 * @return The raw tor nodes list as it is distributed by the tor directory servers.
	 */
	public String getTorNodesList() throws Exception
	{
		Document doc = getXmlDocument(HttpRequestStructure.createGetRequest("/tornodes"));
		Element torNodeList=doc.getDocumentElement();
		return XMLUtil.parseNodeString(torNodeList,null);
	}

	/**
	 * Posts a new forwarder to an infoservice with a JAP forwarder list. If we can't reach the
	 * infoservice or if it has not a forwarder list, an Exception is thrown.
	 *
	 * @param a_japForwarderNode The JapForwarder node of the "post forwarder to infoservice"
	 *                           XML structure.
	 *
	 * @return The JapForwarder node of the answer of the infoservice's addforwarder command.
	 */
	public Element postNewForwarder(Element a_japForwarderNode) throws Exception
	{
		if (hasPrimaryForwarderList() == false)
		{
			/* infoservice must have a forwarder list */
			throw (new Exception("InfoService: postNewForwarder: The InfoService " + getName() +
								 " has no forwarder list."));
		}
		/* infoservice has a forwarder list */
		Document doc = getXmlDocument(HttpRequestStructure.createPostRequest("/addforwarder",
			a_japForwarderNode.getOwnerDocument()));
		NodeList japForwarderNodes = doc.getElementsByTagName("JapForwarder");
		if (japForwarderNodes.getLength() == 0)
		{
			throw (new Exception("InfoService: postNewForwarder: Error in XML structure."));
		}
		return ( (Element) (japForwarderNodes.item(0)));
	}

	/**
	 * Posts the renew message for a forwarder to an infoservice with a JAP forwarder list. If we
	 * can't reach the infoservice or if it has not a forwarder list, an Exception is thrown.
	 *
	 * @param a_japForwarderNode The JapForwarder node of the "renew forwarder" XML structure.
	 *
	 * @return The JapForwarder node of the answer of the infoservice's renewforwarder command.
	 */
	public Element postRenewForwarder(Element a_japForwarderNode) throws Exception
	{
		if (hasPrimaryForwarderList() == false)
		{
			/* infoservice must have a forwarder list */
			throw (new Exception("InfoService: postRenewForwarder: The InfoService " + getName() +
								 " has no forwarder list."));
		}
		/* infoservice has a forwarder list */
		Document doc = getXmlDocument(HttpRequestStructure.createPostRequest("/renewforwarder",
			a_japForwarderNode.getOwnerDocument()));
		NodeList japForwarderNodes = doc.getElementsByTagName("JapForwarder");
		if (japForwarderNodes.getLength() == 0)
		{
			throw (new Exception("InfoService: postRenewForwarder: Error in XML structure."));
		}
		return ( (Element) (japForwarderNodes.item(0)));
	}

	/**
	 * Downloads a forwarder entry from the infoservice. If this infoservice has no forwarder list,
	 * it will ask an infoservice with such a list and returns the answer to us. If we can't reach
	 * the infoservice or if this infoservice doesn't know a forwarder, an Exception is thrown.
	 *
	 * @return The JapForwarder node of the answer of the infoservice's getforwarder command.
	 */
	public Element getForwarder() throws Exception
	{
		Document doc = getXmlDocument(HttpRequestStructure.createGetRequest("/getforwarder"));
		NodeList japForwarderNodes = doc.getElementsByTagName("JapForwarder");
		if (japForwarderNodes.getLength() == 0)
		{
			throw (new Exception("InfoService: getForwarder: Error in XML structure."));
		}
		Element japForwarderNode = (Element) (japForwarderNodes.item(0));
		/* look for a ErrorInformation node -> if this node exists, the call was not successful
		 * -> throw a Exception, so the InfoServiceHolder will try another infoservice, maybe there
		 * are some forwarders available
		 */
		NodeList errorInformationNodes = japForwarderNode.getElementsByTagName("ErrorInformation");
		if (errorInformationNodes.getLength() > 0)
		{
			Element errorInformationNode = (Element) (errorInformationNodes.item(0));
			throw (new Exception("InfoService: getForwarder: The infoservice returned error " +
								 errorInformationNode.getAttribute("code") + ": " +
								 errorInformationNode.getFirstChild().getNodeValue()));
		}
		/* no ErrorInformation node -> no error */
		return japForwarderNode;
	}

	/**
	 * Checks the signature of an XML node against the certificate store from InfoServiceHolder.
	 * This method returns only if the signature is valid or signature checking is disabled
	 * (certificate store is null). If the signature is invalid, an exception is thrown.
	 *
	 * @param a_nodeToCheck The node to verify.
	 */
	private void checkSignature(Element a_nodeToCheck) throws Exception
	{
		JAPCertificateStore certificateStore = InfoServiceHolder.getInstance().getCertificateStore();
		checkSignature(a_nodeToCheck, certificateStore);
	}

	/**
	 * Checks the signature of an XML node against a store of trusted certificates.
	 * This method returns only if the signature is valid or signature checking is disabled
	 * (the supplied certificate store was null). If the signature is invalid, an exception is thrown.
	 *
	 * @param a_nodeToCheck The node to verify.
	 * @param a_trustedCertificates The store of trusted certificates. If this is null, we accept all
	 *                              signatures.
	 */
	private void checkSignature(Element a_nodeToCheck, JAPCertificateStore a_trustedCertificates) throws
		Exception
	{
		if (a_trustedCertificates != null)
		{
			/* verify the signature */
			NodeList signatureNodes = a_nodeToCheck.getElementsByTagName("Signature");
			if (signatureNodes.getLength() == 0)
			{
				throw (new Exception("InfoService: signatureCheck: Signature node missing."));
			}
			else
			{
				Element signatureNode = (Element) (signatureNodes.item(0));
				int errorCode = JAPCertPath.validate(a_nodeToCheck, signatureNode, a_trustedCertificates);
				if (errorCode != ErrorCodes.E_SUCCESS)
				{
					throw (new Exception("InfoService: signatureCheck: Signature is invalid. Errorcode: " +
										 Integer.toString(errorCode)));
				}
			}
		}
	}

}
