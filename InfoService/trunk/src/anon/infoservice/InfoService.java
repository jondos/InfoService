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
import anon.crypto.JAPCertificateStore;
import anon.crypto.JAPCertificate;
import anon.crypto.JAPSignature;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/**
 * Holds the information for an infoservice.
 */
public class InfoService extends DatabaseEntry
{

	/**
	 * The time until a new InfoService (created by the JAP and not received from an infoservice)
	 * will be expired. The default is 30 days.
	 */
	/* don't remove the (long)-cast because the standard is only an int and so it wouldn't work
	 * with big values
	 */
	private static final long DEFAULT_EXPIRE_TIME = 30 * 24 * 3600 * (long) (1000);

	/**
	 * This is the ID of the infoservice.
	 */
	private String infoServiceId;

	/**
	 * Time (see System.currentTimeMillis()) when the JAP client will remove this infoservice
	 * entry from the database of all known infoservices.
	 */
	private long expire;

	/**
	 * The name of the infoservice.
	 */
	private String name;

	/**
	 * Holds the information about the interfaces (IP, Port) the infoservice is listening on.
	 */
	private Vector listenerInterfaces;

	/**
	 * Stores the number of the prefered ListenerInterface in the listenerInterfaces list. If we
	 * have to connect to the infoservice, this interface is used first. If there is an connection
	 * error, all other interfaces will be tested. If we can get the connection over another
	 * interface, this interface will be set as new preferedListenerInterface.
	 */
	private int preferedListenerInterface;

	/**
	 * Some information about the used infoservice software.
	 */
	private ServiceSoftware infoServiceSoftware;

	/**
	 * Creates a new InfoService from XML description (InfoService node).
	 *
	 * @param infoServiceNode The InfoService node from an XML document.
	 */
	public InfoService(Element infoServiceNode) throws Exception
	{
		/* get the ID */
		infoServiceId = infoServiceNode.getAttribute("id");
		/* get the name */
		NodeList nameNodes = infoServiceNode.getElementsByTagName("Name");
		if (nameNodes.getLength() == 0)
		{
			throw (new Exception("InfoService: Error in XML structure."));
		}
		Element nameNode = (Element) (nameNodes.item(0));
		name = nameNode.getFirstChild().getNodeValue();
		/* get the software information */
		NodeList softwareNodes = infoServiceNode.getElementsByTagName("Software");
		if (softwareNodes.getLength() == 0)
		{
			throw (new Exception("InfoService: Error in XML structure."));
		}
		Element softwareNode = (Element) (softwareNodes.item(0));
		infoServiceSoftware = new ServiceSoftware(softwareNode);
		/* get the listener interfaces */
		NodeList networkNodes = infoServiceNode.getElementsByTagName("Network");
		if (networkNodes.getLength() == 0)
		{
			throw (new Exception("InfoService: Error in XML structure."));
		}
		Element networkNode = (Element) (networkNodes.item(0));
		NodeList listenerInterfacesNodes = networkNode.getElementsByTagName("ListenerInterfaces");
		if (listenerInterfacesNodes.getLength() == 0)
		{
			throw (new Exception("InfoService: Error in XML structure."));
		}
		Element listenerInterfacesNode = (Element) (listenerInterfacesNodes.item(0));
		NodeList listenerInterfaceNodes = listenerInterfacesNode.getElementsByTagName("ListenerInterface");
		if (listenerInterfaceNodes.getLength() == 0)
		{
			throw (new Exception("InfoService: Error in XML structure."));
		}
		listenerInterfaces = new Vector();
		for (int i = 0; i < listenerInterfaceNodes.getLength(); i++)
		{
			Element listenerInterfaceNode = (Element) (listenerInterfaceNodes.item(i));
			listenerInterfaces.addElement(new ListenerInterface(listenerInterfaceNode));
		}
		/* set the first interface as prefered interface */
		preferedListenerInterface = 0;
		/* get the Expire timestamp */
		NodeList expireNodes = infoServiceNode.getElementsByTagName("Expire");
		if (expireNodes.getLength() == 0)
		{
			throw (new Exception("InfoService: Error in XML structure."));
		}
		Element expireNode = (Element) (expireNodes.item(0));
		expire = Long.parseLong(expireNode.getFirstChild().getNodeValue());
	}

	/**
	 * Creates a new InfoService from the hostName / IP and the port. The hostName and port are
	 * directly used for creating the ListenerInterface for this InfoService. The ID is set to a
	 * generic value derived from the hostname, the port and the name (if it is not null). If you
	 * supply a name for the infoservice then it will get the name "(User) name", if you supply null,
	 * the name will be the same as the ID "(User) hostname:port". The expire time is calculated by
	 * using the DEFAULT_EXPIRE_TIME constant. The software info is set to a dummy value.
	 *
	 * @param hostName The hostname or IP address the infoservice is listening on.
	 * @param port The port the infoservice is listening on.
	 * @param a_strName The name of the infoservice or null, if a generic name (=ID) shall be
	 *                  used.
	 */
	public InfoService(String hostName, int port, String a_strName) throws Exception
	{
		/* set a unique ID */
		infoServiceId = "(User) " + hostName + ":" + Integer.toString(port);
		/* set a name */
		if (a_strName == null)
		{
			name = infoServiceId;
		}
		else
		{
			infoServiceId = infoServiceId + " " + a_strName;
			name = "(User) " + a_strName;
		}
		/* create the ListenerInterface and set it as prefered */
		listenerInterfaces = new Vector();
		listenerInterfaces.addElement(new ListenerInterface(hostName, port));
		preferedListenerInterface = 0;
		/* set the Expire time */
		expire = System.currentTimeMillis() + DEFAULT_EXPIRE_TIME;
		infoServiceSoftware = new ServiceSoftware("unknown");
	}

	/**
	 * Creates an XML node without signature for this InfoService.
	 *
	 * @param doc The XML document, which is the environment for the created XML node.
	 *
	 * @return The InfoService XML node.
	 */
	public Element toXmlNode(Document doc)
	{
		Element infoServiceNode = doc.createElement("InfoService");
		infoServiceNode.setAttribute("id", infoServiceId);
		/* Create the child nodes of InfoService (Name, Software, Network, Expire) */
		Element nameNode = doc.createElement("Name");
		nameNode.appendChild(doc.createTextNode(name));
		Element softwareNode = infoServiceSoftware.toXmlNode(doc);
		Element networkNode = doc.createElement("Network");
		Element listenerInterfacesNode = doc.createElement("ListenerInterfaces");
		Enumeration it = listenerInterfaces.elements();
		while (it.hasMoreElements())
		{
			ListenerInterface currentListenerInterface = (ListenerInterface) (it.nextElement());
			Element currentNode = currentListenerInterface.toXmlNode(doc);
			listenerInterfacesNode.appendChild(currentNode);
		}
		networkNode.appendChild(listenerInterfacesNode);
		Element expireNode = doc.createElement("Expire");
		expireNode.appendChild(doc.createTextNode(Long.toString(expire)));
		infoServiceNode.appendChild(nameNode);
		infoServiceNode.appendChild(softwareNode);
		infoServiceNode.appendChild(networkNode);
		infoServiceNode.appendChild(expireNode);
		return infoServiceNode;
	}

	/**
	 * Returns the ID of the infoservice.
	 *
	 * @return The ID of this infoservice.
	 */
	public String getId()
	{
		return infoServiceId;
	}

	/**
	 * Returns the time (see System.currentTimeMillis()), when the JAP client will remove this
	 * infoservice entry from the database of all known infoservices.
	 *
	 * @return The time, when this infoservice entry will become invalid in the JAP client database
	 * of all known infoservices.
	 *
	 */
	public long getExpireTime()
	{
		return expire;
	}

	/**
	 * Returns the name of the infoservice.
	 *
	 * @return The name of this infoservice.
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * Returns a snapshot of all listener interfaces of this infoservice.
	 *
	 * @return A Vector with all listener interfaces of this infoservice.
	 */
	public Vector getListenerInterfaces()
	{
		Vector r_listenerInterfacesList = new Vector();
		Enumeration listenerInterfacesEnumeration = listenerInterfaces.elements();
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
		return name;
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
			int lastIndex = listenerInterfaces.indexOf(lastConnectionDescriptor.getTargetInterface());
			nextIndex = (lastIndex + 1) % (listenerInterfaces.size());
		}
		/* update the preferedListenerInterface */
		preferedListenerInterface = nextIndex;
		/* create the connection descriptor */
		ListenerInterface target = (ListenerInterface) (listenerInterfaces.elementAt(nextIndex));
		HTTPConnection connection = HTTPConnectionFactory.getInstance().createHTTPConnection(target);
		return (new HTTPConnectionDescriptor(connection, target));
	}

	/**
	 * Fetches the specified XML document from the infoservice. If a ListenerInterface can't be
	 * reached, automatically a new one is chosen. If we can't reach the infoservice at all, an
	 * Exception is thrown.
	 *
	 * @param infoServiceFile The document which you want to get.
	 *
	 * @return The specified XML document.
	 */
	private Document getXmlDocument(String infoServiceFile) throws Exception
	{
		/* make sure, that we are connected */
		int connectionCounter = 0;
		HTTPConnectionDescriptor currentConnectionDescriptor = null;
		while (connectionCounter < listenerInterfaces.size())
		{
			/* update the connectionCounter */
			connectionCounter++;
			/* get the next connection descriptor by supplying the last one */
			currentConnectionDescriptor = connectToInfoService(currentConnectionDescriptor);
			HTTPConnection currentConnection = currentConnectionDescriptor.getConnection();
			LogHolder.log(LogLevel.DEBUG, LogType.NET,
						  "InfoService: getXmlDocument: Get: " + currentConnection.getHost() + ":" +
						  Integer.toString(currentConnection.getPort()) + infoServiceFile);
			try
			{
				HTTPResponse response = currentConnection.Get(infoServiceFile);
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
							  infoServiceFile);
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
		Document doc = getXmlDocument("/cascades");
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
		Document doc = getXmlDocument("/infoservices");
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
					infoServices.addElement(new InfoService(infoServiceNode));
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
		Document doc = getXmlDocument("/mixinfo/" + mixId);
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
	 *
	 * @return The current StatusInfo for the mixcascade with the given ID.
	 */
	public StatusInfo getStatusInfo(String cascadeId, int cascadeLength) throws Exception
	{
		Document doc = getXmlDocument("/mixcascadestatus/" + cascadeId);
		NodeList mixCascadeStatusNodes = doc.getElementsByTagName("MixCascadeStatus");
		if (mixCascadeStatusNodes.getLength() == 0)
		{
			throw (new Exception("InfoService: getStatusInfo: Error in XML structure."));
		}
		Element mixCascadeStatusNode = (Element) (mixCascadeStatusNodes.item(0));
		/* if signature is invalid, exception is thrown */
		checkSignature(mixCascadeStatusNode);
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
		Document doc = getXmlDocument("/currentjapversion");
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
					//throw (new Exception("InfoService: getNewVersionNumber: Sginatrue check failed!"));
				}

			}
			catch (Exception e)
			{
				//throw (new Exception("InfoService: getNewVersionNumber: Sginatrue check failed!"));
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
		doc = getXmlDocument("/japRelease.jnlp");
	}
	if (japVersionType == JAPVersionInfo.JAP_DEVELOPMENT_VERSION)
	{
		doc = getXmlDocument("/japDevelopment.jnlp");
	}
	return (new JAPVersionInfo(doc, japVersionType));
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
	if (certificateStore != null)
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
			int errorCode = JAPCertPath.validate(a_nodeToCheck, signatureNode, certificateStore);
			if (errorCode != ErrorCodes.E_SUCCESS)
			{
				throw (new Exception("InfoService: signatureCheck: Signature is invalid. Errorcode: " +
									 Integer.toString(errorCode)));
			}
		}
	}
}

}
