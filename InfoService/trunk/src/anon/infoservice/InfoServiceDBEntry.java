/*
 Copyright (c) 2000 - 2005, The JAP-Team
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

import java.util.Enumeration;
import java.util.NoSuchElementException;
import java.util.Vector;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import HTTPClient.HTTPConnection;
import HTTPClient.HTTPResponse;
import anon.crypto.SignatureVerifier;
import anon.util.BZip2Tools;
import anon.util.Base64;
import anon.util.IXMLEncodable;
import anon.util.XMLParseException;
import anon.util.XMLUtil;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

import java.util.zip.GZIPInputStream;
import java.io.ByteArrayInputStream;
import anon.crypto.SignatureCreator;

/**
 * Holds the information for an infoservice.
 */
public class InfoServiceDBEntry extends AbstractDatabaseEntry implements IDistributable, IXMLEncodable
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
	 * have to connect to the infoservice, this interface is used first. If there is a connection
	 * error, all other interfaces will be tested. If we can get the connection over another
	 * interface, this interface will be set as new preferedListenerInterface.
	 */
	private int m_preferedListenerInterface;

	/**
	 * Stores whether this infoservice has a primary forwarder list (true) or not (false).
	 */
	private boolean m_bPrimaryForwarderList;

	/**
	 * Describes whether this infoservice is a neighbour of our one.
	 * We send all messages only to neighbours, so we have less traffic.
	 * For now, every remote infoservice is a neighbour and it is only false for the local one.
	 */
	private boolean m_neighbour;

	/**
	 * Stores the XML representation of this InfoServiceDBEntry.
	 */
	private Element m_xmlDescription;

	/**
	 * Stores whether this InfoServiceDBEntry is user-defined within the JAP client (true) or was
	 * generated from the InfoService itself (false).
	 */
	private boolean m_userDefined;

	/**
	 * Stores the time when this infoservice entry was created by the origin infoservice or by the
	 * JAP client (if it is a user-defined entry). This value is used to determine the more recent
	 * infoservice entry, if two entries are compared (higher version number -> more recent entry).
	 */
	private long m_creationTimeStamp;

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
			if (dbentry instanceof InfoServiceDBEntry)
			{
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
	 * Returns the name of the XML element constructed by this class.
	 *
	 * @return the name of the XML element constructed by this class
	 */
	public static String getXmlElementName()
	{
		return "InfoService";
	}

	/**
	 * This is only for compatibility and will be rewritten next time.
	 * @todo rewrite this
	 * @param a_listenerInterface a ListenerInterface of this InfoService
	 * @return Returns an ID for the infoservice (IP:Port of the first listener interface).
	 */
	private static String generateId(ListenerInterface a_listenerInterface)
	{
		return a_listenerInterface.getHost() + "%3A" + a_listenerInterface.getPort();
	}

	/**
	 * Creates a new InfoService from XML description (InfoService node). The new entry will be
	 * created within the context of the JAP client (the timeout for infoservice entries within the
	 * JAP client is used).
	 *
	 * @param a_infoServiceNode The InfoService node from an XML document.
	 *
	 * @exception XMLParseException if an error in the xml structure occurs
	 */
	public InfoServiceDBEntry(Element a_infoServiceNode) throws XMLParseException
	{
		this(a_infoServiceNode, true);
	}

	/**
	 * Creates a new InfoService from XML description (InfoService node). The new entry will be
	 * created within the specified context. The context influcences the timeout within the database
	 * of all infoservices.
	 *
	 * @param a_infoServiceNode The InfoService node from an XML document.
	 * @param a_japClientContext Whether the new entry will be created within the context of the
	 *                           JAP client (true) or the context of the InfoService (false). This
	 *                           setting influences the timeout of the created entry within the
	 *                           database of all infoservices.
	 *
	 * @exception XMLParseException if an error in the xml structure occurs
	 */
	public InfoServiceDBEntry(Element a_infoServiceNode, boolean a_japClientContext) throws XMLParseException
	{
		this(a_infoServiceNode,
			 (a_japClientContext ? (System.currentTimeMillis() + Constants.TIMEOUT_INFOSERVICE_JAP) :
			  (System.currentTimeMillis() + Constants.TIMEOUT_INFOSERVICE)));
	}

	/**
	 * Creates a new InfoService from XML description (InfoService node).
	 *
	 * @param a_infoServiceNode The InfoService node from an XML document.
	 * @param a_timeout The timeout of the new InfoServiceDBEntry within the database of all
	 *                  InfoServices, see System.currentTimeMillis().
	 *
	 * @exception XMLParseException if an error in the xml structure occurs
	 */
	private InfoServiceDBEntry(Element a_infoServiceNode, long a_timeout) throws XMLParseException
	{
		super(a_timeout);

		if (a_infoServiceNode == null)
		{
			throw new XMLParseException(XMLParseException.NODE_NULL_TAG);
		}

		/* store the XML representation */
		m_xmlDescription = a_infoServiceNode;

		/* get the ID */
		m_strInfoServiceId = a_infoServiceNode.getAttribute("id");

		/* get the name */
		m_strName = XMLUtil.parseValue(XMLUtil.getFirstChildByName(a_infoServiceNode, "Name"), null);
		if (m_strName == null)
		{
			throw new XMLParseException("Name");
		}

		/* get the software information */
		m_infoserviceSoftware = new ServiceSoftware( (Element) XMLUtil.getFirstChildByName(a_infoServiceNode,
			ServiceSoftware.getXmlElementName()));

		/* get the listener interfaces */
		Node networkNode = XMLUtil.getFirstChildByName(a_infoServiceNode, "Network");
		if (networkNode == null)
		{
			throw new XMLParseException("Network");
		}
		Element listenerInterfacesNode =
			(Element) XMLUtil.getFirstChildByName(networkNode, "ListenerInterfaces");
		if (networkNode == null)
		{
			throw new XMLParseException("ListenerInterfaces");
		}
		NodeList listenerInterfaceNodes = listenerInterfacesNode.getElementsByTagName(
			ListenerInterface.getXMLElementName());
		if (listenerInterfaceNodes.getLength() == 0)
		{
			throw new XMLParseException(ListenerInterface.getXMLElementName());
		}
		m_listenerInterfaces = new Vector();
		for (int i = 0; i < listenerInterfaceNodes.getLength(); i++)
		{
			Element listenerInterfaceNode = (Element) (listenerInterfaceNodes.item(i));
			m_listenerInterfaces.addElement(new ListenerInterface(listenerInterfaceNode));
		}

		/* set the first interface as prefered interface */
		m_preferedListenerInterface = 0;

		/* get the creation timestamp */
		m_creationTimeStamp = XMLUtil.parseValue(XMLUtil.getFirstChildByName(a_infoServiceNode, "LastUpdate"),
												 -1L);
		if (m_creationTimeStamp == -1)
		{
			throw new XMLParseException("LastUpdate");
		}

		/* get the information, whether this infoservice keeps a list of JAP forwarders */
		if (XMLUtil.getFirstChildByName(a_infoServiceNode, "ForwarderList") == null)
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

		/* get the information, whether this infoservice was user-defined within the JAP client */
		if (XMLUtil.getFirstChildByName(a_infoServiceNode, "UserDefined") == null)
		{
			/* there is no UserDefined node -> this InfoServiceDBEntry was generated by an InfoService
			 * itself
			 */
			m_userDefined = false;
		}
		else
		{
			/* there is a UserDefined node -> this InfoServiceDBEntry was generated by the user within
			 * the JAP client
			 */
			m_userDefined = true;
		}

		/* at the moment every infoservice talks with all other infoservices */
		m_neighbour = true;
	}

	/**
	 * Creates a new InfoServiceDBEntry. The ID is set to a generic value derived from the host and
	 * the port of the first listener interface. If you supply a name for the infoservice then it
	 * will get that name, if you supply null, the name will be of the type "hostname:port". If the
	 * new infoservice entry is created within the context of the JAP client, the software info is
	 * set to a dummy value. If it is created within the context of the infoservice, the software
	 * info is set to the current infoservice version (see Constants.INFOSERVICE_VERSION).
	 *
	 * @param a_strName The name of the infoservice or null, if a generic name shall be used.
	 * @param a_listeners The listeners the infoservice is (virtually) listening on.
	 * @param a_primaryForwarderList Whether the infoservice holds a primary forwarder list.
	 * @param a_japClientContext Whether the new entry will be created within the context of the
	 *                           JAP client (true) or the context of the InfoService (false). This
	 *                           setting influences the timeout of the created entry within the
	 *                           database of all infoservices.
	 *
	 * @exception IllegalArgumentException if invalid listener interfaces are given
	 */
	public InfoServiceDBEntry(String a_strName, Vector a_listeners, boolean a_primaryForwarderList,
							  boolean a_japClientContext) throws IllegalArgumentException
	{
		this(a_strName, a_listeners, a_primaryForwarderList, a_japClientContext,
			 (a_japClientContext ? (System.currentTimeMillis() + Constants.TIMEOUT_INFOSERVICE_JAP) :
			  (System.currentTimeMillis() + Constants.TIMEOUT_INFOSERVICE)));
	}

	/**
	 * Creates a new InfoServiceDBEntry. The ID is set to a generic value derived from the host and
	 * the port of the first listener interface. If you supply a name for the infoservice then it
	 * will get that name, if you supply null, the name will be of the type "hostname:port". If the
	 * new infoservice entry is created within the context of the JAP client, the software info is
	 * set to a dummy value. If it is created within the context of the infoservice, the software
	 * info is set to the current infoservice version (see Constants.INFOSERVICE_VERSION).
	 *
	 * @param a_strName The name of the infoservice or null, if a generic name shall be used.
	 * @param a_listeners The listeners the infoservice is (virtually) listening on.
	 * @param a_primaryForwarderList Whether the infoservice holds a primary forwarder list.
	 * @param a_japClientContext Whether the new entry will be created within the context of the
	 *                           JAP client (true) or the context of the InfoService (false).
	 * @param a_expireTime The time when this InfoService will be removed from the database of all
	 *                     InfoServices.
	 *
	 * @exception IllegalArgumentException if invalid listener interfaces are given
	 */
	public InfoServiceDBEntry(String a_strName, Vector a_listeners, boolean a_primaryForwarderList,
							  boolean a_japClientContext, long a_expireTime) throws IllegalArgumentException
	{
		super(a_expireTime);

		if (a_listeners == null)
		{
			throw new IllegalArgumentException("No listener interfaces!");
		}

		Enumeration enumListeners = a_listeners.elements();
		m_listenerInterfaces = new Vector();
		while (enumListeners.hasMoreElements())
		{
			m_listenerInterfaces.addElement(enumListeners.nextElement());
			if (! (m_listenerInterfaces.lastElement() instanceof ListenerInterface))
			{
				throw new IllegalArgumentException("Invalid listener interface!");
			}
		}

		m_strInfoServiceId = generateId( (ListenerInterface) m_listenerInterfaces.firstElement());
		/* if it is a user-defined infoservice use a special id to avoid collisions with real
		 * infoservices
		 */
		if (a_japClientContext)
		{
			m_strInfoServiceId = "(user)" + m_strInfoServiceId;
		}

		/* set a name */
		m_strName = a_strName;
		if (m_strName == null)
		{
			/* create a name with information from the first listener interface */
			ListenerInterface firstListenerInterface = ( (ListenerInterface) m_listenerInterfaces.
				firstElement());
			m_strName = firstListenerInterface.getHost() + ":" +
				Integer.toString(firstListenerInterface.getPort());
		}

		m_bPrimaryForwarderList = a_primaryForwarderList;
		if (a_japClientContext)
		{
			m_infoserviceSoftware = new ServiceSoftware("unknown");
		}
		else
		{
			m_infoserviceSoftware = new ServiceSoftware(Constants.INFOSERVICE_VERSION);
		}

		m_preferedListenerInterface = 0;
		m_userDefined = a_japClientContext;
		m_creationTimeStamp = System.currentTimeMillis();

		/* locally created infoservices are never neighbours of our infoservice */
		m_neighbour = false;

		/* generate the XML representation for this InfoServiceDBEntry */
		m_xmlDescription = generateXmlRepresentation();
	}

	/**
	 * Generates the XML representation for this InfoServiceDBEntry. That XML representation is
	 * returnded when calling the toXmlElement method.
	 *
	 * @return The generated XML representation for this InfoServiceDBEntry.
	 */
	private Element generateXmlRepresentation()
	{
		Document doc = XMLUtil.createDocument();
		/* Create the InfoService element */
		Element infoServiceNode = doc.createElement(getXmlElementName());
		XMLUtil.setAttribute(infoServiceNode, "id", m_strInfoServiceId);
		/* Create the child nodes of InfoService */
		Element nameNode = doc.createElement("Name");
		XMLUtil.setValue(nameNode, m_strName);
		Element networkNode = doc.createElement("Network");

		Element listenerInterfacesNode = doc.createElement("ListenerInterfaces");
		Enumeration enumer = m_listenerInterfaces.elements();
		while (enumer.hasMoreElements())
		{
			ListenerInterface currentListenerInterface = (ListenerInterface) (enumer.nextElement());
			listenerInterfacesNode.appendChild(currentListenerInterface.toXmlElement(doc));
		}
		networkNode.appendChild(listenerInterfacesNode);
		Element lastUpdateNode = doc.createElement("LastUpdate");
		XMLUtil.setValue(lastUpdateNode, m_creationTimeStamp);
		/** create also an expire node for comptatibility with JAP/InfoService <= 00.03.043/IS.06.040
		 *  @todo remove it
		 */
		Element expireNode = doc.createElement("Expire");
		XMLUtil.setValue(expireNode, m_creationTimeStamp);
		infoServiceNode.appendChild(nameNode);
		infoServiceNode.appendChild(m_infoserviceSoftware.toXmlElement(doc));
		infoServiceNode.appendChild(networkNode);
		infoServiceNode.appendChild(lastUpdateNode);
		/** append also an expire node for comptatibility with JAP/InfoService <= 00.03.043/IS.06.040
		 *  @todo remove it
		 */
		infoServiceNode.appendChild(expireNode);
		if (m_bPrimaryForwarderList)
		{
			/* if we hold a forwarder list, also append an ForwarderList node, at the moment this
			 * node doesn't have any children
			 */
			Element forwarderListNode = doc.createElement("ForwarderList");
			infoServiceNode.appendChild(forwarderListNode);
		}
		if (m_userDefined)
		{
			/* if this is a user-defined InfoServiceDBEntry, add the UserDefined node (has no children)
			 */
			Element userDefinedNode = doc.createElement("UserDefined");
			infoServiceNode.appendChild(userDefinedNode);
		}

		/* sign the XML node */
		try
		{
			SignatureCreator.getInstance().signXml(SignatureVerifier.DOCUMENT_CLASS_INFOSERVICE,
				infoServiceNode);
		}
		catch (Exception a_e)
		{
			LogHolder.log(LogLevel.EXCEPTION, LogType.MISC, "Document could not be signed!");
		}
		return infoServiceNode;
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
	 * Sets the ID of the infoservice. Use with caution!
	 *
	 */
	public void setId(String id)
	{
		m_strInfoServiceId = id;
		m_xmlDescription = generateXmlRepresentation();
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
	 * Returns the time when this infoservice entry was created by the origin infoservice or by the
	 * JAP client (if it is a user-defined entry).
	 *
	 * @return A version number which is used to determine the more recent infoservice entry, if two
	 *         entries are compared (higher version number -> more recent entry).
	 */
	public long getVersionNumber()
	{
		return m_creationTimeStamp;
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
	 * Returns whether this InfoServiceDBEntry was generated by a user within the JAP client (true)
	 * or was generated by the InfoService itself (false).
	 *
	 * @return Whether this InfoServiceDBEntry is user-defined.
	 */
	public boolean isUserDefined()
	{
		return m_userDefined;
	}

	public void setUserDefined(boolean b)
	{
		m_userDefined = b;
		m_xmlDescription = generateXmlRepresentation();
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
	 * Creates an XML node for this InfoService.
	 * @param a_doc The XML document, which is the environment for the created XML node.
	 * @return The InfoService XML node.
	 */
	public Element toXmlElement(Document a_doc)
	{
		Element returnXmlStructure = null;
		try
		{
			returnXmlStructure = (Element) (XMLUtil.importNode(a_doc, m_xmlDescription, true));
		}
		catch (Exception e)
		{
		}
		return returnXmlStructure;
	}

	/**
	 * Compares this object to another one. This method returns only true, if the other object is
	 * also an InfoServiceDBEntry and has the same ID as this InfoServiceDBEntry.
	 *
	 * @param a_object The object with which to compare.
	 *
	 * @return True, if the object with which to compare is also an InfoServiceDBEntry which has the
	 *         same ID as this instance. In any other case, false is returned.
	 */
	public boolean equals(Object a_object)
	{
		boolean objectEquals = false;
		if (a_object != null)
		{
			if (a_object instanceof InfoServiceDBEntry)
			{
				objectEquals = this.getId().equals( ( (InfoServiceDBEntry) a_object).getId());
			}
		}
		return objectEquals;
	}

	/**
	 * Returns a hashcode for this instance of InfoServiceDBEntry. The hashcode is calculated from
	 * the ID, so if two instances of InfoServiceDBEntry have the same ID, they will have the same
	 * hashcode.
	 *
	 * @return The hashcode for this InfoServiceDBEntry.
	 */
	public int hashCode()
	{
		return (getId().hashCode());
	}

	/**
	 * This returns the filename (InfoService command), where this InfoServerDBEntry is posted at
	 * other InfoServices. It's always '/infoserver'.
	 *
	 * @return The filename where the information about this InfoServerDBEntry is posted at other
	 *         InfoServices when this entry is forwarded.
	 */
	public String getPostFile()
	{
		return "/infoserver";
	}

	/**
	 * This returns the data, which are posted to other InfoServices. It's the whole XML structure
	 * of this InfoServerDBEntry.
	 *
	 * @return The data, which are posted to other InfoServices when this entry is forwarded.
	 */
	public byte[] getPostData()
	{
		return (XMLUtil.toString(m_xmlDescription).getBytes());
	}

	/**
	 * Returns whether this infoservice is a neighbour of our one. This is only meaningful within
	 * the context of an infoservice. We send all messages only to neighbours, so we have less
	 * traffic. For now, every remote infoservice is a neighbour and it is only false for the local
	 * one.
	 *
	 * @return Whether this infoservice is a neighbour of our one or not.
	 */
	public boolean isNeighbour()
	{
		return m_neighbour;
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
	 * @todo Create the connections via a changing proxy interface!!
	 */
	private HTTPConnectionDescriptor connectToInfoService(HTTPConnectionDescriptor lastConnectionDescriptor)
	{
		int nextIndex = m_preferedListenerInterface;
		if (lastConnectionDescriptor != null)
		{
			int lastIndex = m_listenerInterfaces.indexOf(lastConnectionDescriptor.getTargetInterface());
			nextIndex = (lastIndex + 1) % (m_listenerInterfaces.size());
		}
		/* update the preferedListenerInterface */
		m_preferedListenerInterface = nextIndex;
		/* create the connection descriptor */
		ListenerInterface target = (ListenerInterface) (m_listenerInterfaces.elementAt(nextIndex));
		HTTPConnection connection = HTTPConnectionFactory.getInstance().createHTTPConnection(target);
		return new HTTPConnectionDescriptor(connection, target);
	}

	/**
	 * Fetches the specified XML document from the infoservice. If a ListenerInterface can't be
	 * reached, automatically a new one is chosen. If we can't reach the infoservice at all, an
	 * Exception is thrown. If the operation is interrupted via the Thread instance, this method
	 * ends immediately but also the interrupted flag of the Thread is set.
	 *
	 * @param a_httpRequest The structure with the HTTP request.
	 *
	 * @return The specified XML document.
	 */
	private Document getXmlDocument(final HttpRequestStructure a_httpRequest) throws Exception
	{
		/* make sure, that we are connected */
		int connectionCounter = 0;
		HTTPConnectionDescriptor currentConnectionDescriptor = null;
		while ( (connectionCounter < m_listenerInterfaces.size()) && (Thread.currentThread().isInterrupted() == false))
		{
			/* update the connectionCounter */
			connectionCounter++;
			/* get the next connection descriptor by supplying the last one */
			currentConnectionDescriptor = connectToInfoService(currentConnectionDescriptor);
			final HTTPConnection currentConnection = currentConnectionDescriptor.getConnection();

			/* use a Vector as storage for the the result of the communication */
			final Vector responseStorage = new Vector();
			/* we need the possibility to interrupt the infoservice communication, but also we need to
			 * know whether the operation was interupted by an external call of Thread.interrupt() or
			 * a timeout, thus it is not enough to catch the InteruptedIOException because that
			 * Exception is thrown in both cases, so we cannot distinguish the both -> solution make
			 * an extra Thread for the communication
			 */
			Thread communicationThread = new Thread(new Runnable()
			{
				public void run()
				{
					try
					{
						if (a_httpRequest.getRequestCommand() == HttpRequestStructure.HTTP_COMMAND_GET)
						{
							LogHolder.log(LogLevel.DEBUG, LogType.NET,
										  "InfoServiceDBEntry: getXmlDocument: Get: " +
										  currentConnection.getHost() + ":" +
										  Integer.toString(currentConnection.getPort()) +
										  a_httpRequest.getRequestFileName());
							responseStorage.addElement(currentConnection.Get(a_httpRequest.getRequestFileName()));
						}
						else
						{
							if (a_httpRequest.getRequestCommand() == HttpRequestStructure.HTTP_COMMAND_POST)
							{
								LogHolder.log(LogLevel.DEBUG, LogType.NET,
											  "InfoServiceDBEntry: getXmlDocument: Post: " +
											  currentConnection.getHost() + ":" +
											  Integer.toString(currentConnection.getPort()) +
											  a_httpRequest.getRequestFileName());
								String postData = "";
								if (a_httpRequest.getRequestPostDocument() != null)
								{
									postData = XMLUtil.toString(a_httpRequest.getRequestPostDocument());
								}
								responseStorage.addElement(currentConnection.Post(a_httpRequest.
									getRequestFileName(), postData));
							}
							else
							{
								throw (new Exception(
									"InfoServiceDBEntry: getXmlDocument: Invalid HTTP command."));
							}
						}
					}
					catch (Exception e)
					{
						LogHolder.log(LogLevel.ERR, LogType.NET,
									  "InfoServiceDBEntry: getXmlDocument: Connection to infoservice interface failed: " +
									  currentConnection.getHost() + ":" +
									  Integer.toString(currentConnection.getPort()) +
									  a_httpRequest.getRequestFileName() + " Reason: " + e.toString());
					}
				}
			});
			communicationThread.setDaemon(true);
			communicationThread.start();
			try
			{
				communicationThread.join();
				try
				{
					HTTPResponse response = (HTTPResponse) (responseStorage.firstElement());
					Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(response.
						getInputStream());
					/* fetching the document was successful, leave this method */
					return doc;
				}
				catch (NoSuchElementException e)
				{
					/* fetching the information was not successful -> do nothing */
				}
			}
			catch (InterruptedException e)
			{
				/* operation was interupted from the outside -> set the intterrupted flag for the Thread
				 * again, so the caller of the methode can evaluate it, also interrupt the communication
				 * thread, but don't wait for the end of that thread
				 */
				LogHolder.log(LogLevel.INFO, LogType.NET, "Current operation was interrupted.");
				Thread.currentThread().interrupt();
				communicationThread.interrupt();
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
		//System.out.println(XMLUtil.toString(doc));
		Element mixCascadesNode = (Element) (mixCascadesNodes.item(0));
		NodeList mixCascadeNodes = mixCascadesNode.getElementsByTagName("MixCascade");
		Vector mixCascades = new Vector();
		for (int i = 0; i < mixCascadeNodes.getLength(); i++)
		{
			Element mixCascadeNode = (Element) (mixCascadeNodes.item(i));
			/* check the signature */
			if (SignatureVerifier.getInstance().verifyXml(mixCascadeNode,
				SignatureVerifier.DOCUMENT_CLASS_MIX))
			{
				/* signature is valid */
				try
				{
					mixCascades.addElement(new MixCascade(mixCascadeNode, true));
				}
				catch (Exception e)
				{
					/* an error while parsing the node occured -> we don't use this mixcascade */
					LogHolder.log(LogLevel.EXCEPTION, LogType.MISC, "Error in MixCascade XML node.");
				}
			}
			else
			{
				LogHolder.log(LogLevel.INFO, LogType.MISC,
							  "Cannot verify the signature for MixCascade entry: " +
							  XMLUtil.toString(mixCascadeNode));
				try
				{
					mixCascades.addElement(new MixCascade(mixCascadeNode, false));
				}
				catch (Exception e)
				{
					/* an error while parsing the node occured -> we don't use this mixcascade */
					LogHolder.log(LogLevel.EXCEPTION, LogType.MISC, "Error in MixCascade XML node.");
				}

			}
		}
		return mixCascades;
	}

	/**
	 * Get a Vector of all payment instances the infoservice knows. If we can't get a connection with
	 * the infoservice, an Exception is thrown.
	 *
	 * @return The Vector of all payment instances.
	 */
	public Vector getPaymentInstances() throws Exception
	{
		Document doc = getXmlDocument(HttpRequestStructure.createGetRequest("/paymentinstances"));
		NodeList paymentInstancesNodes = doc.getElementsByTagName("PaymentInstances");
		if (paymentInstancesNodes.getLength() == 0)
		{
			throw (new Exception("InfoService: getPaymentInstances: Error in XML structure."));
		}
		Element paymentInstancesNode = (Element) (paymentInstancesNodes.item(0));
		NodeList paymentInstanceNodes = paymentInstancesNode.getElementsByTagName("PaymentInstance");
		Vector paymentInstances = new Vector();
		for (int i = 0; i < paymentInstanceNodes.getLength(); i++)
		{
			Element paymentInstanceNode = (Element) (paymentInstanceNodes.item(i));

			try
			{
				paymentInstances.addElement(new PaymentInstanceDBEntry(paymentInstanceNode));
			}
			catch (Exception e)
			{
				/* an error while parsing the node occured -> we don't use this payment instance */
				LogHolder.log(LogLevel.EXCEPTION, LogType.MISC,
							  "InfoService: getPaymentInstances: Error in PaymentInstance XML node.");
			}

		}
		return paymentInstances;
	}

	public PaymentInstanceDBEntry getPaymentInstance(String a_piID) throws Exception
	{
		Document doc = getXmlDocument(HttpRequestStructure.createGetRequest("/paymentinstance/" + a_piID));
		Element paymentInstance = (Element) XMLUtil.getFirstChildByName(doc, "PaymentInstance");
		return new PaymentInstanceDBEntry(paymentInstance);
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
			/* check the signature */
			if (SignatureVerifier.getInstance().verifyXml(infoServiceNode,
				SignatureVerifier.DOCUMENT_CLASS_INFOSERVICE))
			{
				/* signature is valid */
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
			else
			{
				LogHolder.log(LogLevel.ERR, LogType.MISC,
							  "Cannot verify the signature for InfoService entry: " +
							  XMLUtil.toString(infoServiceNode));
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
		/* check the signature */
		if (SignatureVerifier.getInstance().verifyXml(mixNode, SignatureVerifier.DOCUMENT_CLASS_MIX) == false)
		{
			/* signature is invalid -> throw an exception */
			throw (new Exception(
				"InfoServiceDBEntry: getMixInfo: Cannot verify the signature for Mix entry: " +
				XMLUtil.toString(mixNode)));
		}
		/* signature was valid */
		return new MixInfo(mixNode);
	}

	/**
	 * Get the StatusInfo for the cascade with the given ID. If we can't get a connection with the
	 * infoservice, an Exception is thrown.
	 *
	 * @param cascadeId The ID of the mixcascade to get the StatusInfo for.
	 * @param cascadeLength The length of the mixcascade (number of mixes). We need this for
	 *                      calculating the AnonLevel in the StatusInfo.
	 *
	 * @return The current StatusInfo for the mixcascade with the given ID.
	 */
	public StatusInfo getStatusInfo(String cascadeId, int cascadeLength) throws Exception
	{
		Document doc = getXmlDocument(HttpRequestStructure.createGetRequest("/mixcascadestatus/" + cascadeId));
		NodeList mixCascadeStatusNodes = doc.getElementsByTagName("MixCascadeStatus");
		if (mixCascadeStatusNodes.getLength() == 0)
		{
			throw (new Exception("InfoService: getStatusInfo: Error in XML structure."));
		}
		Element mixCascadeStatusNode = (Element) (mixCascadeStatusNodes.item(0));
		/* check the signature */
		if (SignatureVerifier.getInstance().verifyXml(mixCascadeStatusNode,
			SignatureVerifier.DOCUMENT_CLASS_MIX) == false)
		{
			/* signature is invalid -> throw an exception */
			throw (new Exception(
				"InfoServiceDBEntry: getStatusInfo: Cannot verify the signature for MixCascadeStatus entry: " +
				XMLUtil.toString(mixCascadeStatusNode)));
		}
		/* signature was valid */
		return new StatusInfo(mixCascadeStatusNode, cascadeLength);
	}

	/**
	 * Get the version String of the currently minimum required JAP version from the infoservice.
	 * This method is called to check, whether connection to the mixcascades are possible with the
	 * currently used JAP version. If we can't get a connection with the infoservice, an Exception
	 * is thrown.
	 *
	 * @return The version String (fromat: nn.nn.nnn) of the current JAP version.
	 */
	public String getNewVersionNumber() throws Exception
	{
		Document doc = getXmlDocument(HttpRequestStructure.createGetRequest("/currentjapversion"));
		Element japNode = (Element) (XMLUtil.getFirstChildByName(doc, JAPMinVersion.getXmlElementName()));
		/* verify the signature */
		if (SignatureVerifier.getInstance().verifyXml(japNode, SignatureVerifier.DOCUMENT_CLASS_UPDATE) == false)
		{
			/* signature is invalid -> throw an exception */
			throw (new Exception(
				"InfoServiceDBEntry: getNewVersionNumber: Cannot verify the signature for JAPMinVersion entry: " +
				XMLUtil.toString(japNode)));
		}
		/* signature was valid */
		JAPMinVersion minimumRequiredVersion = new JAPMinVersion(japNode);
		String versionString = minimumRequiredVersion.getJapSoftware().getVersion();
		if ( (versionString.charAt(2) != '.') || (versionString.charAt(5) != '.'))
		{
			throw (new Exception("InfoServiceDBEntry: getNewVersionNumber: Invalid version number format: " +
								 versionString));
		}
		return versionString;
	}

	/**
	 * Returns the JAPVersionInfo for the specified type. The JAPVersionInfo is generated from
	 * the JNLP files received from the infoservice. If we can't get a connection with the
	 * infoservice, an Exception is thrown.
	 *
	 * @param a_japVersionType Selects the JAPVersionInfo (release / development). See the constants
	 *                         in JAPVersionInfo.
	 *
	 * @return The JAPVersionInfo of the specified type.
	 */
	public JAPVersionInfo getJAPVersionInfo(int a_japVersionType) throws Exception
	{
		Document doc = null;
		if (a_japVersionType == JAPVersionInfo.JAP_RELEASE_VERSION)
		{
			doc = getXmlDocument(HttpRequestStructure.createGetRequest("/japRelease.jnlp"));
		}
		else if (a_japVersionType == JAPVersionInfo.JAP_DEVELOPMENT_VERSION)
		{
			doc = getXmlDocument(HttpRequestStructure.createGetRequest("/japDevelopment.jnlp"));
		}
		else
		{
			throw (new Exception("InfoServiceDBEntry: getJAPVersionInfo: Invalid version info requested."));
		}
		Element jnlpNode = (Element) (XMLUtil.getFirstChildByName(doc, JAPVersionInfo.getXmlElementName()));
		/* verify the signature */
		if (SignatureVerifier.getInstance().verifyXml(jnlpNode, SignatureVerifier.DOCUMENT_CLASS_UPDATE) == false)
		{
			/* signature is invalid -> throw an exception */
			throw (new Exception(
				"InfoServiceDBEntry: getJAPVersionInfo: Cannot verify the signature for JAPVersionInfo entry: " +
				XMLUtil.toString(jnlpNode)));
		}
		/* signature was valid */
		return new JAPVersionInfo(jnlpNode, a_japVersionType);
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
		String list = null;
		try
		{ //Compressed first
			Document doc = getXmlDocument(HttpRequestStructure.createGetRequest("/compressedtornodes"));
			Element torNodeList = doc.getDocumentElement();
			String strCompressedTorNodesList = XMLUtil.parseValue(torNodeList, null);
			list = new String(BZip2Tools.decompress(Base64.decode(strCompressedTorNodesList)));
		}
		catch (Exception e)
		{
		}
		if (list == null) //umcompressed...
		{
			try
			{
				Document doc = getXmlDocument(HttpRequestStructure.createGetRequest("/tornodes"));
				Element torNodeList = doc.getDocumentElement();
				list = XMLUtil.parseValue(torNodeList, null);
			}
			catch (Exception e)
			{
			}
		}
		if (list == null)
		{
			throw (new Exception(
				"InfoServiceDBEntry: getTorNodesList: Error while parsing the TOR nodes list XML structure."));
		}
		return list;
	}

	/**
	 * Get the list with the mixminion nodes from the infoservice. If we can't get a connection with the
	 * infoservice or the infoservice doesn't support the tor nodes list download, an Exception is
	 * thrown.
	 *
	 * @return The raw mixminion nodes list as it is distributed by the tor directory servers.
	 */
	public String getMixminionNodesList() throws Exception
	{
		String list = null;
		try
		{ //Compressed first
			Document doc = getXmlDocument(HttpRequestStructure.createGetRequest("/compressedmixminionnodes"));
			Element mixminionNodeList = doc.getDocumentElement();
			String strCompressedMixminionNodesList = XMLUtil.parseValue(mixminionNodeList, null);
			ByteArrayInputStream bin = new ByteArrayInputStream(Base64.decode(strCompressedMixminionNodesList));
			GZIPInputStream gzipIn = new GZIPInputStream(bin, 4096);
			StringBuffer sblist = new StringBuffer(200000);
			byte[] bout = new byte[4096];
			int len = 0;
			while ( (len = gzipIn.read(bout)) > 0)
			{
				sblist.append(new String(bout, 0, len));
			}
			list = sblist.toString();
		}
		catch (Exception e)
		{
		}

		if (list == null)
		{
			throw (new Exception(
				"InfoServiceDBEntry: getMixminionNodesList: Error while parsing the TOR nodes list XML structure."));
		}
		return list;
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
		if (!hasPrimaryForwarderList())
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
		return (Element) japForwarderNodes.item(0);
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
		if (!hasPrimaryForwarderList())
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
		return (Element) japForwarderNodes.item(0);
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

}
