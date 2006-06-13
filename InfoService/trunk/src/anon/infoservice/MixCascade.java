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
import java.util.Vector;
import java.util.Observable;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;

import anon.AnonServerDescription;
import anon.crypto.JAPCertificate;
import anon.crypto.SignatureVerifier;
import anon.crypto.XMLSignature;
import anon.util.XMLUtil;
import anon.util.IXMLEncodable;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/**
 * Holds the information for a mixcascade.
 */
public class MixCascade extends AbstractDatabaseEntry implements IDistributable, AnonServerDescription,
		IXMLEncodable
{
	public static final String XML_ELEMENT_NAME = "MixCascade";
	public static final String XML_ELEMENT_CONTAINER_NAME = "MixCascades";

	/**
	 * This is the ID of the mixcascade.
	 */
	private String m_mixCascadeId;

	/**
	 * Time (see System.currentTimeMillis()) when the mixcascade (first mix) has sent this HELO
	 * message.
	 */
	private long m_lastUpdate;

	/**
	 * The name of the mixcascade.
	 */
	private String m_strName;

	/**
	 * Holds the information about the interfaces (IP, Port) the mixcascade (first mix) is listening
	 * on.
	 */
	private Vector m_listenerInterfaces;

	/**
	 * Holds IDs of all mixes in the cascade.
	 */
	private Vector m_mixIds;

	/**
	 * Stores the certificate for verifying the status messages of the mixcascade.
	 */
	private JAPCertificate m_mixCascadeCertificate;

	/**
	 * Stores the XML structure for this mixcascade.
	 */
	private Element m_xmlStructure;

	/**
	 * True, if this MixCascade is user defined, false if the Information comes from the
	 * InfoService. This value is only meaningful within the context of the JAP client.
	 */
	private boolean m_userDefined;

	/**
	 * True, if this MixCascade is a payment cascade.
	 */
	private boolean m_isPayment;

	/**
	 * True if the certificate of the cascade is signed by a root certificate.
	 */
	private boolean m_isCertified = true;

	public MixCascade(Element a_mixCascadeNode, boolean a_isCertified) throws Exception
	{
		this(a_mixCascadeNode);
		m_isCertified = a_isCertified;
	}

	public MixCascade(Element a_mixCascadeNode, boolean a_isCertified, long a_expireTime) throws Exception
	{
		this(a_mixCascadeNode, a_expireTime);
		m_isCertified = a_isCertified;
	}

	/**
	 * Creates a new MixCascade from XML description (MixCascade node).
	 *
	 * @param a_mixCascadeNode The MixCascade node from an XML document.
	 */
	public MixCascade(Element a_mixCascadeNode) throws Exception
	{
		this(a_mixCascadeNode, 0);
	}

	/**
	 * Creates a new MixCascade from XML description (MixCascade node).
	 *
	 * @param a_mixCascadeNode The MixCascade node from an XML document.
	 * @param a_expireTime forces a specific expire time; takes default expire time if <= 0
	 */
	public MixCascade(Element a_mixCascadeNode, long a_expireTime) throws Exception
	{
		/* use always the timeout for the infoservice context, because the JAP client currently does
		 * not have a database of mixcascade entries -> no timeout for the JAP client necessary
		 */
		super(a_expireTime <= 0 ? (System.currentTimeMillis() + Constants.TIMEOUT_MIXCASCADE) : a_expireTime);
		/* get the ID */
		m_mixCascadeId = a_mixCascadeNode.getAttribute("id");
		/* get the name */
		NodeList nameNodes = a_mixCascadeNode.getElementsByTagName("Name");
		if (nameNodes.getLength() == 0)
		{
			throw (new Exception("MixCascade: Error in XML structure."));
		}
		Element nameNode = (Element) (nameNodes.item(0));
		m_strName = nameNode.getFirstChild().getNodeValue();

		/* get payment info */
		Node payNode = XMLUtil.getFirstChildByName(a_mixCascadeNode, "Payment");
		if (payNode != null)
		{
			m_isPayment = XMLUtil.parseAttribute(payNode, "required", false);
		}
		else
		{
			m_isPayment = false;
		}

		/* get the listener interfaces */
		NodeList networkNodes = a_mixCascadeNode.getElementsByTagName("Network");
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
		m_listenerInterfaces = new Vector();
		for (int i = 0; i < listenerInterfaceNodes.getLength(); i++)
		{
			Element listenerInterfaceNode = (Element) (listenerInterfaceNodes.item(i));
			m_listenerInterfaces.addElement(new ListenerInterface(listenerInterfaceNode));
		}
		/* get the IDs of all mixes in the cascade */
		NodeList mixesNodes = a_mixCascadeNode.getElementsByTagName("Mixes");
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
		m_mixIds = new Vector();
		for (int i = 0; i < mixNodes.getLength(); i++)
		{
			Element mixNode = (Element) (mixNodes.item(i));
			m_mixIds.addElement(mixNode.getAttribute("id"));
		}
		/* get the LastUpdate timestamp */
		NodeList lastUpdateNodes = a_mixCascadeNode.getElementsByTagName("LastUpdate");
		if (lastUpdateNodes.getLength() == 0)
		{
			throw (new Exception("MixCascade: Error in XML structure."));
		}
		Element lastUpdateNode = (Element) (lastUpdateNodes.item(0));
		m_lastUpdate = Long.parseLong(lastUpdateNode.getFirstChild().getNodeValue());

		/* try to get the certificate from the Signature node */
		try
		{
			m_mixCascadeCertificate = null;
			XMLSignature documentSignature = XMLSignature.getUnverified(a_mixCascadeNode);
			if (documentSignature != null)
			{
				Enumeration appendedCertificates = documentSignature.getCertificates().elements();
				/* store the first certificate (there should be only one) -> needed for verification of the
				 * MixCascadeStatus XML structure */
				if (appendedCertificates.hasMoreElements())
				{
					m_mixCascadeCertificate = (JAPCertificate) (appendedCertificates.nextElement());
				}
				else
				{
					LogHolder.log(LogLevel.DEBUG, LogType.MISC,
								  "MixCascade: Constructor: No appended certificates in the MixCascade structure.");
				}
			}
			else
			{
				LogHolder.log(LogLevel.DEBUG, LogType.MISC,
							  "MixCascade: Constructor: No signature node found while looking for MixCascade certificate.");
			}
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.ERR, LogType.MISC,
						  "MixCascade: Constructor: Error while looking for appended certificates in the MixCascade structure: " +
						  e.toString());
		}

		/* get the information, whether this mixcascade was user-defined within the JAP client */
		if (XMLUtil.getFirstChildByName(a_mixCascadeNode, "UserDefined") == null)
		{
			/* there is no UserDefined node -> this MixCascade entry was generated by the corresponding
			 * mixcascade itself
			 */
			m_userDefined = false;
		}
		else
		{
			/* there is a UserDefined node -> this MixCascade entry was generated by the user within the
			 * JAP client
			 */
			m_userDefined = true;
		}

		/* store the xml structure */
		m_xmlStructure = a_mixCascadeNode;
	}

	/**
	 * Creates a new MixCascade from the hostName / IP and the port. The hostName and port are
	 * directly used for creating the ListenerInterface for this MixCascade. ID and the name
	 * are set to a generic value derived from the name and the port. The lastUpdate time is the
	 * current system time. One mixId is created, it is the same as the mixCascadeId. The current
	 * status is set to dummy value. Cause the infoservice does not know this mixCascadeId and the
	 * created mixId, you will never get a StatusInfo or a MixInfo other than the dummy one.
	 *
	 * @param a_hostName The hostname or IP address the mixcascade (first mix) is listening on.
	 * @param a_port The port the mixcascade (first mix) is listening on.
	 */
	public MixCascade(String a_hostName, int a_port) throws Exception
	{
		this(null, null, a_hostName, a_port);
	}

	/**
	 * Creates a new MixCascade from the hostName / IP and the port. The hostName and port are
	 * directly used for creating the ListenerInterface for this MixCascade. If ID and the name
	 * are not provided, than they are set to a generic value derived from the name and the port.
	 * The lastUpdate time is the current system time. One mixId is created, it is the same as the
	 * mixCascadeId. The current status is set to dummy value. Cause the infoservice does not know
	 * this mixCascadeId and the created mixId, you will never get a StatusInfo or a MixInfo other
	 * than the dummy one.
	 *
	 * @param a_name A human readable name of this cascade, which could be display on the UI. If
	 *               this value is null the name will be constructed from hostName and port.
	 * @param a_id The ID of this cascade. If null than it will be constructed from hostName and
	 *             port.
	 * @param a_hostName The hostname or IP address the mixcascade (first mix) is listening on.
	 * @param a_port The port the mixcascade (first mix) is listening on.
	 */
	public MixCascade(String a_name, String a_id, String a_hostName, int a_port) throws Exception
	{
		this(a_name, a_id,
			 new ListenerInterface(a_hostName, a_port, ListenerInterface.PROTOCOL_TYPE_RAW_TCP).toVector());
	}

	public MixCascade(String a_name, String a_id, Vector a_listenerInterfaces) throws Exception
	{
		/* use always the timeout for the infoservice context, because the JAP client currently does
		 * not have a database of mixcascade entries -> no timeout for the JAP client necessary
		 */
		super(System.currentTimeMillis() + Constants.TIMEOUT_MIXCASCADE);
		ListenerInterface listener = (ListenerInterface) a_listenerInterfaces.elementAt(0);
		String strHostName = listener.getHost();
		String strPort = Integer.toString(listener.getPort());
		/* set a unique ID */
		if ( (a_id == null) || (a_id.length() == 0))
		{
			m_mixCascadeId = "(user)" + strHostName + "%3A" + strPort;
		}
		else
		{
			m_mixCascadeId = a_id;
		}
		/* set a name */
		if (a_name != null)
		{
			m_strName = a_name;
		}
		else
		{
			m_strName = strHostName + ":" + strPort;
		}
		m_listenerInterfaces = a_listenerInterfaces;
		/* set the lastUpdate time */
		m_lastUpdate = System.currentTimeMillis();
		/* create the mixIds and set one with the same ID as the mixcascade itself */
		m_mixIds = new Vector();
		m_mixIds.addElement(m_mixCascadeId);
		/* some more values */
		m_userDefined = true;
		m_mixCascadeCertificate = null;
		m_xmlStructure = generateXmlRepresentation();
	}

	/**
	 * Returns an XML node for this MixCascade. This structure includes a Signature node if the
	 * MixCascade information was created by the corresponding mixcascade itself. If this is a
	 * userdefined MixCascade, there is no Signature node included.
	 *
	 * @param a_doc The XML document, which is the environment for the created XML node.
	 *
	 * @return The MixCascade XML node.
	 */
	public Element toXmlElement(Document a_doc)
	{
		Element importedXmlStructure = null;
		try
		{
			importedXmlStructure = (Element) (XMLUtil.importNode(a_doc, m_xmlStructure, true));
		}
		catch (Exception e)
		{
		}
		return importedXmlStructure;
	}

	/**
	 * Returns the ID of the mixcascade.
	 *
	 * @return The ID of this mixcascade.
	 */
	public String getId()
	{
		return m_mixCascadeId;
	}

	/**
	 * Returns the time (see System.currentTimeMillis()), when the mixcascade (first mix) has sent
	 * this MixCascade info to an InfoService. If this is a user-defined cascade, the creation time
	 * of this MixCasdade entry is returned.
	 *
	 * @return The send time of this MixCascade info from the mixcascade.
	 *
	 */
	public long getLastUpdate()
	{
		return m_lastUpdate;
	}

	/**
	 * Returns the time when this MixCascade entry was created by the origin mixcascade (or by the
	 * JAP client if it is a user-defined entry).
	 *
	 * @return A version number which is used to determine the more recent MixCascade entry, if two
	 *         entries are compared (higher version number -> more recent entry).
	 */
	public long getVersionNumber()
	{
		return getLastUpdate();
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
		return getName();
	}

	/**
	 * Compares this object to another one. This method returns only true, if the other object is
	 * also a MixCascade and has the same ID as this MixCascade.
	 *
	 * @param a_object The object with which to compare.
	 *
	 * @return True, if the object with which to compare is also a MixCascade which has the same ID
	 *         as this instance. In any other case, false is returned.
	 */
	public boolean equals(Object a_object)
	{
		boolean objectEquals = false;
		if (a_object != null)
		{
			if (a_object instanceof MixCascade)
			{
				objectEquals = this.getId().equals( ( (MixCascade) a_object).getId());
			}
		}
		return objectEquals;
	}

	/**
	 * Returns a hashcode for this instance of MixCascade. The hashcode is calculated from the ID,
	 * so if two instances of MixCascade have the same ID, they will have the same hashcode.
	 *
	 * @return The hashcode for this MixCascade.
	 */
	public int hashCode()
	{
		return (getId().hashCode());
	}

	/**
	 * Returns the number of interfaces (IP, Port) the mixcascade (first mix) is listening on.
	 *
	 * @return The number of listener interfaces.
	 */
	public int getNumberOfListenerInterfaces()
	{
		return m_listenerInterfaces.size();
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
		ListenerInterface returnedListener = null;
		if (i >= 0)
		{
			if (i < getNumberOfListenerInterfaces())
			{
				returnedListener = (ListenerInterface) (m_listenerInterfaces.elementAt(i));
			}
		}
		return returnedListener;
	}

	/**
	 * Returns the number of mixes in the cascade.
	 *
	 * @return the number of mixes in the cascade
	 */
	public int getNumberOfMixes()
	{
		return m_mixIds.size();
	}

	/**
	 * Returns the IDs of all mixes in the cascade.
	 *
	 * @return A snapshot of the list with all mix IDs within the cascade.
	 */
	public Vector getMixIds()
	{
		Vector mixIdList = new Vector();
		for (int i = 0; i < m_mixIds.size(); i++)
		{
			mixIdList.addElement(m_mixIds.elementAt(i));
		}
		return mixIdList;
	}

	/**
	 * Returns whether this MixCascade entry was generated by a user within the JAP client (true) or
	 * was generated by the original mixcascade itself (false).
	 *
	 * @return Whether this MixCascade entry is user-defined.
	 */
	public boolean isUserDefined()
	{
		return m_userDefined;
	}

	public void setUserDefined(boolean b)
	{
		m_userDefined = b;
		m_xmlStructure = generateXmlRepresentation();
	}

	/**
	 * Fetches the current status of the mixcascade from the InfoService. The StatusInfo is
	 * available by calling getCurrentStatus().
	 */
	public void fetchCurrentStatus()
	{
		synchronized (this)
		{
			int certificateLock = -1;
			if (m_mixCascadeCertificate != null)
			{
				/* add the cascade certificate temporary to the certificate store */
				certificateLock = SignatureVerifier.getInstance().getVerificationCertificateStore().
					addCertificateWithVerification(m_mixCascadeCertificate,
					JAPCertificate.CERTIFICATE_TYPE_MIX, false);
			}
			StatusInfo statusInfo =
				InfoServiceHolder.getInstance().getStatusInfo(getId(), getNumberOfMixes());
			if (statusInfo != null)
			{
				Database.getInstance(StatusInfo.class).update(statusInfo);
			}
			if (certificateLock != -1)
			{
				/* remove the lock on the certificate */
				SignatureVerifier.getInstance().getVerificationCertificateStore().removeCertificateLock(
					certificateLock);
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
		StatusInfo statusInfo = (StatusInfo)Database.getInstance(StatusInfo.class).getEntryById(getId());
		if (statusInfo == null)
		{
			statusInfo = StatusInfo.createDummyStatusInfo(getId());
		}
		return statusInfo;

	}

	/**
	 * This returns the filename (InfoService command), where this MixCascade entry is posted at
	 * other InfoServices. It's always '/cascade'.
	 *
	 * @return The filename where the information about this MixCascade entry is posted at other
	 *         InfoServices when this entry is forwarded.
	 */
	public String getPostFile()
	{
		return "/cascade";
	}

	/**
	 * This returns the data posted when this MixCascade information is forwarded to other
	 * infoservices. It's the XML structure of this MixCascade as we received it.
	 *
	 * @return The data posted to other infoservices when this entry is forwarded.
	 */
	public byte[] getPostData()
	{
		return XMLUtil.toString(m_xmlStructure).getBytes();
	}

	/**
	 * Returns the XML structure for this MixCascade entry.
	 *
	 * @return The XML node for this MixCascade (MixCascade node).
	 */
	public Element getXmlStructure()
	{
		return m_xmlStructure;
	}

	/**
	 * Returns the certificate appended to the signature of the MixCascade XML structure. If there
	 * is no appended certificate or this MixCascade is user-defined, null is returned.
	 *
	 * @return The certificate of this mixcascade or null, if there is no appended certificate.
	 */
	public JAPCertificate getMixCascadeCertificate()
	{
		return m_mixCascadeCertificate;
	}

	/**
	 * Creates an XML node without signature for this MixCascade.
	 *
	 * @return The MixCascade XML node.
	 */
	private Element generateXmlRepresentation()
	{
		Document doc = XMLUtil.createDocument();
		Element mixCascadeNode = doc.createElement(XML_ELEMENT_NAME);
		XMLUtil.setAttribute(mixCascadeNode, "id", getId());
		/* Create the child nodes of MixCascade (Name, Network, Mixes, LastUpdate) */
		Element nameNode = doc.createElement("Name");
		XMLUtil.setValue(nameNode, getName());
		Element networkNode = doc.createElement("Network");
		Element listenerInterfacesNode = doc.createElement("ListenerInterfaces");
		for (int i = 0; i < getNumberOfListenerInterfaces(); i++)
		{
			ListenerInterface currentListenerInterface = getListenerInterface(i);
			Element currentListenerInterfaceNode = currentListenerInterface.toXmlElement(doc);
			listenerInterfacesNode.appendChild(currentListenerInterfaceNode);
		}
		networkNode.appendChild(listenerInterfacesNode);
		Element mixesNode = doc.createElement("Mixes");
		XMLUtil.setAttribute(mixesNode, "count", getNumberOfMixes());
		Enumeration allMixIds = m_mixIds.elements();
		while (allMixIds.hasMoreElements())
		{
			Element mixNode = doc.createElement("Mix");
			XMLUtil.setAttribute(mixNode, "id", (String) (allMixIds.nextElement()));
			mixesNode.appendChild(mixNode);
		}
		Element lastUpdateNode = doc.createElement("LastUpdate");
		XMLUtil.setValue(lastUpdateNode, getLastUpdate());
		mixCascadeNode.appendChild(nameNode);
		mixCascadeNode.appendChild(networkNode);
		mixCascadeNode.appendChild(mixesNode);
		mixCascadeNode.appendChild(lastUpdateNode);
		if (isUserDefined())
		{
			/* if this is a user-defined MixCascade entry, add the UserDefined node (has no children) */
			Element userDefinedNode = doc.createElement("UserDefined");
			mixCascadeNode.appendChild(userDefinedNode);
			XMLUtil.setAttribute(mixCascadeNode, "userDefined", true);
		}
		return mixCascadeNode;
	}


	public boolean isCertified()
	{
		return m_isCertified;
	}

	public boolean isPayment()
	{
		return m_isPayment;
	}

}
