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

import java.util.Enumeration;
import java.util.Vector;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import anon.AnonServerDescription;
import anon.crypto.JAPCertificate;
import anon.crypto.JAPCertificateStore;
import anon.crypto.JAPSignature;
import anon.util.XMLUtil;

/**
 * Holds the information for a mixcascade.
 */
public class MixCascade implements AnonServerDescription
{

	/**
	 * This is the ID of the mixcascade.
	 */
	private String mixCascadeId;

	/**
	 * Time (see System.currentTimeMillis()) when the mixcascade (first mix) has sent this HELO
	 * message.
	 */
	private long lastUpdate;

	/**
	 * The name of the mixcascade.
	 */
	private String m_strName;

	/**
	 * Holds the information about the interfaces (IP, Port) the mixcascade (first mix) is listening
	 * on.
	 */
	private Vector listenerInterfaces;

	/**
	 * Holds IDs of all mixes in the cascade.
	 */
	private Vector mixIds;

	/**
	 * Holds the current status of this cascade.
	 */
	private StatusInfo currentStatus;

	/**
	 * Holds the store for all certificates of this mixcascade.
	 */
	private JAPCertificateStore m_mixCascadeCertificates;

	/**
	 * Holds the XML structure for this mixcascade.
	 */
	private Element m_xmlStructure;

	/**
	 * Creates a new MixCascade from XML description (MixCascade node).
	 *
	 * @param mixCascadeNode The MixCascade node from an XML document.
	 */
	public MixCascade(Element mixCascadeNode) throws Exception
	{
		/* get the ID */
		mixCascadeId = mixCascadeNode.getAttribute("id");
		/* get the name */
		NodeList nameNodes = mixCascadeNode.getElementsByTagName("Name");
		if (nameNodes.getLength() == 0)
		{
			throw (new Exception("MixCascade: Error in XML structure."));
		}
		Element nameNode = (Element) (nameNodes.item(0));
		m_strName = nameNode.getFirstChild().getNodeValue();
		/* get the listener interfaces */
		NodeList networkNodes = mixCascadeNode.getElementsByTagName("Network");
		if (networkNodes.getLength() == 0)
		{
			throw (new Exception("MixCascade: Error in XML structure."));
		}
		Element networkNode = (Element) (networkNodes.item(0));
		NodeList listenerInterfacesNodes = networkNode.getElementsByTagName("ListenerInterfaces");
		if (listenerInterfacesNodes.getLength() == 0)
		{
			throw (new Exception("MixCascade: Error in XML structure."));
		}
		Element listenerInterfacesNode = (Element) (listenerInterfacesNodes.item(0));
		NodeList listenerInterfaceNodes = listenerInterfacesNode.getElementsByTagName("ListenerInterface");
		if (listenerInterfaceNodes.getLength() == 0)
		{
			throw (new Exception("MixCascade: Error in XML structure."));
		}
		listenerInterfaces = new Vector();
		for (int i = 0; i < listenerInterfaceNodes.getLength(); i++)
		{
			Element listenerInterfaceNode = (Element) (listenerInterfaceNodes.item(i));
			listenerInterfaces.addElement(new ListenerInterface(listenerInterfaceNode));
		}
		/* get the IDs of all mixes in the cascade */
		NodeList mixesNodes = mixCascadeNode.getElementsByTagName("Mixes");
		if (mixesNodes.getLength() == 0)
		{
			throw (new Exception("MixCascade: Error in XML structure."));
		}
		Element mixesNode = (Element) (mixesNodes.item(0));
		int nrOfMixes = Integer.parseInt(mixesNode.getAttribute("count"));
		NodeList mixNodes = mixesNode.getElementsByTagName("Mix");
		if ( (mixNodes.getLength() == 0) || (nrOfMixes != mixNodes.getLength()))
		{
			throw (new Exception("MixCascade: Error in XML structure."));
		}
		mixIds = new Vector();
		for (int i = 0; i < mixNodes.getLength(); i++)
		{
			Element mixNode = (Element) (mixNodes.item(i));
			mixIds.addElement(mixNode.getAttribute("id"));
		}
		/* get the LastUpdate timestamp */
		NodeList lastUpdateNodes = mixCascadeNode.getElementsByTagName("LastUpdate");
		if (lastUpdateNodes.getLength() == 0)
		{
			throw (new Exception("MixCascade: Error in XML structure."));
		}
		Element lastUpdateNode = (Element) (lastUpdateNodes.item(0));
		lastUpdate = Long.parseLong(lastUpdateNode.getFirstChild().getNodeValue());
		/* try to get the certificates from the Signature node */
		NodeList signatureNodes = mixCascadeNode.getElementsByTagName("Signature");
		/* create an empty certificate store */
		m_mixCascadeCertificates = new JAPCertificateStore();
		if (signatureNodes.getLength() > 0)
		{
			Element signatureNode = (Element) (signatureNodes.item(0));
			JAPCertificate[] certificates = JAPSignature.getAppendedCertificates(signatureNode);
			if (certificates != null)
			{
				for (int i = 0; i < certificates.length; i++)
				{
					/* the certificate path have been checked, before this mixcascade node was parsed */
					certificates[i].setEnabled(true);
					m_mixCascadeCertificates.addCertificate(certificates[i]);
				}
			}
		}
		/* save the xml structure */
		m_xmlStructure = mixCascadeNode;
		/* set the current status to a dummy value */
		currentStatus = StatusInfo.createDummyStatusInfo(mixCascadeId);
	}

	/**
	 * Creates a new MixCascade from the hostName / IP and the port. The hostName and port are
	 * directly used for creating the ListenerInterface for this MixCascade. ID and the name
	 * are set to a generic value derived from the name and the port. The lastUpdate time is the
	 * current system time. One mixId is created, it is the same as the mixCascadeId. The current
	 * status is set to dummy value. Cause the infoservice does not know this mixCascadeId and the
	 * created mixId, you will never get a StatusInfo or a MixInfo other than the dummy one.
	 *
	 * @param hostName The hostname or IP address the mixcascade (first mix) is listening on.
	 * @param port The port the mixcascade (first mix) is listening on.
	 */
	public MixCascade(String hostName, int port) throws Exception
	{
		this(null,null,hostName,port);
	}

	/**
	 * Creates a new MixCascade from the hostName / IP and the port. The hostName and port are
	 * directly used for creating the ListenerInterface for this MixCascade. If ID and the name
	 * are not provided, than they are set to a generic value derived from the name and the port. The lastUpdate time is the
	 * current system time. One mixId is created, it is the same as the mixCascadeId. The current
	 * status is set to dummy value. Cause the infoservice does not know this mixCascadeId and the
	 * created mixId, you will never get a StatusInfo or a MixInfo other than the dummy one.
	 *
	 * @param name A human readable name of this cascade, which could be display on the UI. If null
	 * 				than it will be constructed from hostName and port.
	 * @param id The id of this cascade. If null than it will be constructed from hostName and port.
	 * @param hostName The hostname or IP address the mixcascade (first mix) is listening on.
	 * @param port The port the mixcascade (first mix) is listening on.
	 */
	public MixCascade(String name, String id, String hostName, int port) throws Exception
	{
		/* set a unique ID */
		if (id == null || id.length() == 0)
		{
			mixCascadeId = "c" + hostName + "%3A" + Integer.toString(port);
		}
		else
		{
			mixCascadeId = id;
			/* set a name */
		}
		if (name != null)
		{
			m_strName = name;
		}
		else
		{
			m_strName = hostName + ":" + Integer.toString(port);
			/* create the ListenerInterface */
		}
		listenerInterfaces = new Vector();
		listenerInterfaces.addElement(new ListenerInterface(hostName, port));
		/* set the lastUpdate time */
		lastUpdate = System.currentTimeMillis();
		/* create the mixIds and set one with the same ID as the mixcascade itself */
		mixIds = new Vector();
		mixIds.addElement(mixCascadeId);
		/* create an empty certificate store */
		m_mixCascadeCertificates = new JAPCertificateStore();
		/* there is no xml structure yet */
		m_xmlStructure = null;
		/* create a dummy current status */
		currentStatus = StatusInfo.createDummyStatusInfo(mixCascadeId);
	}

	/**
	 * Returns an XML node for this MixCascade. This structure includes a Signature node
	 * if the MixCascade information was received from an InfoService. If this is a user-
	 * defined MixCascade, there is no Signature node included.
	 *
	 * @param doc The XML document, which is the environment for the created XML node.
	 *
	 * @return The MixCascade XML node.
	 */
	public Element toXmlNode(Document doc)
	{
		if (m_xmlStructure == null)
		{
			// if there is no XML structure yet, create one
			m_xmlStructure = createXmlStructure(doc);
			return m_xmlStructure;
		}
		try
		{
			return ( (Element) (XMLUtil.importNode(doc, m_xmlStructure, true)));
		}
		catch (Exception e)
		{
			return null;
		}
	}

	/**
	 * Returns the ID of the mixcascade.
	 *
	 * @return The ID of this mixcascade.
	 */
	public String getId()
	{
		return mixCascadeId;
	}

	/**
	 * Returns the time (see System.currentTimeMillis()), when the mixcascade (first mix) has sent
	 * this MixCascade info to an InfoService.
	 *
	 * @return The send time of this MixCascade info from the mixcascade.
	 *
	 */
	public long getLastUpdate()
	{
		return lastUpdate;
	}

	/**
	 * Returns the name of the mixcascade.
	 *
	 * @return The name of this mixcascade.
	 */
	public String getName()
	{
		return m_strName;
	}

	/**
	 * Returns a String representation for this MixCascade object. It's just the name of the
	 * mixcascade.
	 *
	 * @return The name of this mixcascade.
	 */
	public String toString()
	{
		return m_strName;
	}

	/**
	 * Returns the number of interfaces (IP, Port) the mixcascade (first mix) is listening on.
	 *
	 * @return The number of listener interfaces.
	 */
	public int getNumberOfListenerInterfaces()
	{
		return listenerInterfaces.size();
	}

	/**
	 * Returns the ListenerInterface with the number i from the list of all listener interfaces
	 * (count starts with 0). If there is no ListenerInterface with this number, null is returned.
	 *
	 * @param i The number of the ListenerInterface.
	 *
	 * @return The ListenerInterface with the number i from the list of all listener interfaces of
	 * this MixCascade.
	 */
	public ListenerInterface getListenerInterface(int i)
	{
		if (i >= 0)
		{
			if (i < getNumberOfListenerInterfaces())
			{
				return (ListenerInterface) (listenerInterfaces.elementAt(i));
			}
		}
		return null;
	}

	/**
	 * Fetches the current status of the mixcascade from the InfoService. The StatusInfo is
	 * available by calling getCurrentStatus().
	 */
	public void fetchCurrentStatus()
	{
		synchronized (this)
		{
			/* be always consistent */
			currentStatus = InfoServiceHolder.getInstance().getStatusInfo(mixCascadeId, mixIds.size(),
				m_mixCascadeCertificates);
			if (currentStatus == null)
			{
				/* no status information available */
				currentStatus = StatusInfo.createDummyStatusInfo(mixCascadeId);
			}
		}
	}

	/**
	 * Returns the current status of this mixcascade. If there is no status available at the
	 * infoservice, a dummy StatusInfo (every value = -1) is returned. The current status is every
	 * time updated, when fetchCurrentStatus() is called.
	 *
	 * @return The current status of the mixcascade.
	 */
	public StatusInfo getCurrentStatus()
	{
		synchronized (this)
		{
			/* get only consistent values */
			return currentStatus;
		}
	}

	/**
	 * Resets the current status. This method should be called, if you stop the periodical call of
	 * fetchCurrentStatus(). Every call of getCurrentStatus() will then return a dummy StatusInfo
	 * (every value = -1) until you call fetchCurrentStatus() again.
	 */
	public void resetCurrentStatus()
	{
		synchronized (this)
		{
			/* be always consistent */
			currentStatus = StatusInfo.createDummyStatusInfo(mixCascadeId);
		}
	}

	/**
	 * Creates an XML node without signature for this MixCascade.
	 *
	 * @param doc The XML document, which is the environment for the created XML node.
	 *
	 * @return The MixCascade XML node.
	 */
	private Element createXmlStructure(Document doc)
	{
		Element mixCascadeNode = doc.createElement("MixCascade");
		mixCascadeNode.setAttribute("id", mixCascadeId);
		/* Create the child nodes of MixCascade (Name, Network, Mixes, LastUpdate) */
		Element nameNode = doc.createElement("Name");
		nameNode.appendChild(doc.createTextNode(m_strName));
		Element networkNode = doc.createElement("Network");
		Element listenerInterfacesNode = doc.createElement("ListenerInterfaces");
		Enumeration it = listenerInterfaces.elements();
		while (it.hasMoreElements())
		{
			ListenerInterface currentListenerInterface = (ListenerInterface) (it.nextElement());
			Element currentNode = currentListenerInterface.toXmlElement(doc);
			listenerInterfacesNode.appendChild(currentNode);
		}
		networkNode.appendChild(listenerInterfacesNode);
		Element mixesNode = doc.createElement("Mixes");
		mixesNode.setAttribute("count", Integer.toString(mixIds.size()));
		Enumeration it2 = mixIds.elements();
		while (it2.hasMoreElements())
		{
			Element mixNode = doc.createElement("Mix");
			mixNode.setAttribute("id", (String) (it2.nextElement()));
			mixesNode.appendChild(mixNode);
		}
		Element lastUpdateNode = doc.createElement("LastUpdate");
		lastUpdateNode.appendChild(doc.createTextNode(Long.toString(lastUpdate)));
		/* there is no signature node created, because we don't know any certificates if this is
		 * a user-defined mixcascade
		 */
		mixCascadeNode.appendChild(nameNode);
		mixCascadeNode.appendChild(networkNode);
		mixCascadeNode.appendChild(mixesNode);
		mixCascadeNode.appendChild(lastUpdateNode);
		return mixCascadeNode;
	}

	/*** Equals iff it has the same id... */
	public boolean equals(Object o)
	{
		if(o==null)
			return false;
		if(!(o instanceof MixCascade))
			return false;
		MixCascade c=(MixCascade)o;
		return getId().equals(c.getId());
	}
}
