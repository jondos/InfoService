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

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import anon.util.XMLUtil;
import anon.util.IXMLEncodable;
import anon.util.XMLParseException;
import org.w3c.dom.Document;
import anon.crypto.XMLSignature;
import logging.LogHolder;
import logging.LogLevel;
import anon.crypto.JAPCertificate;
import anon.crypto.CertPath;
import logging.LogType;
import java.util.Enumeration;
import anon.crypto.SignatureVerifier;

/**
 * Holds the information of one single mix.
 */
public class MixInfo extends AbstractDatabaseEntry implements IDistributable, IXMLEncodable
{
	public static final String XML_ELEMENT_CONTAINER_NAME = "Mixes";
	public static final String XML_ELEMENT_NAME = "Mix";

  /**
   * This is the ID of the mix.
   */
  private String m_mixId;

  /**
   * Time (see System.currentTimeMillis()) when the mix has sent this HELO message.
   */
  private long m_lastUpdate;

  /**
   * The name of the mix.
   */
  private String m_name;

  /**
   * Some information about the location of the mix.
   */
  private ServiceLocation m_mixLocation;

  /**
   * Some information about the operator of the mix.
   */
  private ServiceOperator m_mixOperator;

  /**
   * Some information about the used mix software.
   */
  private ServiceSoftware m_mixSoftware;

  /**
   * Stores whether the mix is waiting for a cascade assignment. This value is only true, if the
   * mix is sending configure requests instead of HELO messages and if it is not already assigned
   * to a cascade. This value is only meaningful within the context of the infoservice.
   */
  private boolean m_freeMix;

  /**
   * Stores the XML structure for this mix.
   */
  private Element m_xmlStructure;

  /**
   * Stores the certificate for this mix.
   * The certificate is not set (null) if the MixInfo-Object is in the InfoService
   */
  private JAPCertificate m_mixCertificate;

  /**
   * Stores the certPath for this mix.
   * The CertPath is not set (null) if the MixInfo-Object is in the InfoService
   */
  private CertPath m_mixCertPath;

  /**
   * Stores the signature element for this mix.
   * The CertPath is not set (null) if the MixInfo-Object is in the InfoService
   */
  private XMLSignature m_mixSignature;

  /**
   * If this MixInfo has been recevied directly from a cascade connection.
   */
  private boolean m_bFromCascade;

  /**
   * Creates a new MixInfo from XML description (Mix node). The state of the mix will be set to
   * non-free (only meaningful within the context of the infoservice).
   *
   * @param a_mixNode The Mix node from an XML document.
   * @param a_bInfoService indicates if the application that calls this constructor is the IS
   */
  public MixInfo(boolean a_bInfoService, Element a_mixNode) throws XMLParseException {
	  this (a_bInfoService, a_mixNode, 0);
  }

  /**
   * Creates a new MixInfo from XML description (Mix node). The state of the mix will be set to
   * non-free (only meaningful within the context of the infoservice).
   *
   * @param a_mixNode The Mix node from an XML document.
   * @param a_expireTime forces a specific expire time; takes default expire time if <= 0
   * @param a_bInfoService indicates if the application that calls this constructor is the IS
   */
  public MixInfo(boolean a_bInfoService, Element a_mixNode, long a_expireTime) throws XMLParseException
  {
	  this(a_bInfoService, a_mixNode, a_expireTime, false);
  }

  public MixInfo(String a_mixID, CertPath a_certPath)
  {
	  super(Long.MAX_VALUE);
	  m_mixId = a_mixID;
	  m_name = a_mixID;
	  m_bFromCascade = true;
	  m_mixCertPath = a_certPath;
	  m_mixCertificate = a_certPath.getFirstCertificate();
	  m_lastUpdate = 0;
	  m_mixLocation = new ServiceLocation(null, m_mixCertificate);
	  m_mixOperator = new ServiceOperator(null, m_mixCertPath.getSecondCertificate());
	  m_freeMix = false;
  }

  /**
   * Creates a new MixInfo from XML description (Mix node). The state of the mix will be set to
   * non-free (only meaningful within the context of the infoservice).
   *
   * @param a_mixNode The Mix node from an XML document.
   * @param a_expireTime forces a specific expire time; takes default expire time if <= 0
   * @param a_bFromCascade if this is a MixInfo node directly received from a cascade (it is stripped)
   * if true, the last update value is set to 0
   * @param a_bInfoService indicates if the application that calls this constructor is the IS
   */
  public MixInfo(boolean a_bInfoService, Element a_mixNode, long a_expireTime, boolean a_bFromCascade)
	  throws XMLParseException
  {
	  /* use always the timeout for the infoservice context, because the JAP client currently does
	   * not have a database of mixcascade entries -> no timeout for the JAP client necessary
	   */
	  super(a_expireTime <= 0 ? System.currentTimeMillis() + Constants.TIMEOUT_MIX : a_expireTime);
	  m_bFromCascade = a_bFromCascade;
	  /* get the ID */
	  m_mixId = XMLUtil.parseAttribute(a_mixNode, "id", null);
	  if (m_mixId == null)
	  {
		  throw (new XMLParseException(XMLParseException.NODE_NULL_TAG, "id"));
	  }

	  /* try to get the certificate and CertPath from the Signature node if the call is NOT the IS*/
	  if(!a_bInfoService)
	  {
	      try
		  {
			  m_mixSignature = SignatureVerifier.getInstance().getVerifiedXml(a_mixNode,
				  SignatureVerifier.DOCUMENT_CLASS_MIX);

			  if (m_mixSignature != null)
			  {
				  Enumeration appendedCertificates = m_mixSignature.getCertificates().elements();
				  /* store the first certificate (there should be only one) -> needed for verification of the
				   * MixCascadeStatus XML structure */
				  if (appendedCertificates.hasMoreElements())
				  {
					  m_mixCertPath = m_mixSignature.getCertPath();
					  m_mixCertificate = m_mixCertPath.getFirstCertificate();
				  }
				  else
				  {
					  LogHolder.log(LogLevel.DEBUG, LogType.MISC,
									"No appended certificates in the MixCascade structure.");
				  }
			  }
			  else
			  {
				  LogHolder.log(LogLevel.DEBUG, LogType.MISC,
								"No signature node found while looking for MixCascade certificate.");
			  }
		  }
		  catch (Exception e)
		  {
			  LogHolder.log(LogLevel.ERR, LogType.MISC,
							"Error while looking for appended certificates in the MixCascade structure: " +
							e.toString());
		  }
	  }

	  Element operatorNode = null;
	  Element locationNode = null;
	  if (!a_bFromCascade)
	  {
		  /* get the name */
		  NodeList nameNodes = a_mixNode.getElementsByTagName("Name");
		  if (nameNodes.getLength() == 0)
		  {
			  throw (new XMLParseException("Name"));
		  }
		  else
		  {
			  Element nameNode = (Element) (nameNodes.item(0));
			  m_name = nameNode.getFirstChild().getNodeValue();
		  }

		  /* get the location */
		  NodeList locationNodes = a_mixNode.getElementsByTagName("Location");
		  if (locationNodes.getLength() == 0)
		  {
			  throw (new XMLParseException("Location", m_mixId));
		  }
		  locationNode = (Element) (locationNodes.item(0));
		  /* get the operator */
		  NodeList operatorNodes = a_mixNode.getElementsByTagName("Operator");
		  if (operatorNodes.getLength() == 0)
		  {
			  throw (new XMLParseException("Operator", m_mixId));
		  }
		  operatorNode = (Element) (operatorNodes.item(0));

		  /* get the software information */
		  NodeList softwareNodes = a_mixNode.getElementsByTagName("Software");
		  if (softwareNodes.getLength() == 0)
		  {
			  throw (new XMLParseException("Software", m_mixId));
		  }
		  Element softwareNode = (Element) (softwareNodes.item(0));
		  m_mixSoftware = new ServiceSoftware(softwareNode);

		  /* get LastUpdate information */
		  NodeList lastUpdateNodes = a_mixNode.getElementsByTagName("LastUpdate");
		  if (lastUpdateNodes.getLength() == 0)
		  {
			  throw (new XMLParseException("LastUpdate", m_mixId));
		  }
		  Element lastUpdateNode = (Element) (lastUpdateNodes.item(0));
		  m_lastUpdate = Long.parseLong(lastUpdateNode.getFirstChild().getNodeValue());
	  }
	  else
	  {
		  m_lastUpdate = System.currentTimeMillis() - Constants.TIMEOUT_MIX;
	  }

	  m_mixLocation = new ServiceLocation(locationNode, m_mixCertificate);
	  //get the Operator Certificate from the CertPath
	  if (m_mixCertPath != null)
	  {
		  m_mixOperator = new ServiceOperator(operatorNode, m_mixCertPath.getSecondCertificate());
	  }
	  else
	  {
		  m_mixOperator = new ServiceOperator(operatorNode, null);
	  }

	  /* as default no mix is free, only if we receive a configuration request from the mix and it
	   * it is not already assigned to a cascade, this mix will be free
	   */
	  m_freeMix = false;
	  m_xmlStructure = a_mixNode;
  }

  /**
   * Returns the ID of the mix.
   *
   * @return The ID of this mix.
   */
  public String getId() {
    return m_mixId;
  }

  /**
   * Returns if this MixInfo has been recevied directly from a cascade connection.
   * @return if this MixInfo has been recevied directly from a cascade connection
   */
  public boolean isFromCascade()
  {
	  return m_bFromCascade;
  }

  /**
   * Returns the time (see System.currentTimeMillis()), when the mix has sent this MixInfo to an
   * infoservice.
   *
   * @return The send time of this MixInfo from the mix.
   *
   */
  public long getLastUpdate() {
    return m_lastUpdate;
  }

  /**
   * Returns the time when this mix entry was created by the origin mix.
   *
   * @return A version number which is used to determine the more recent mix entry, if two
   *         entries are compared (higher version number -> more recent entry).
   */
  public long getVersionNumber() {
    return getLastUpdate();
  }

  /**
   * Returns the name of the mix.
   *
   * @return The name of this mix.
   */
  public String getName() {
    return m_name;
  }

  /**
   * Returns the certificate of the mix
   * For MixInfo-Objects in the InfoService the certificate is null
   * @return the certificate of the mix
   */
  public JAPCertificate getMixCertificate()
  {
	  return m_mixCertificate;
  }

  /**
   * Returns the CertPath of the mix
   * For MixInfo-Objects in the InfoService the CertPath is null
   * @return the CertPath of the mix
   */
  public CertPath getMixCertPath()
  {
	  return m_mixCertPath;
  }
  /**
   * Returns the location of the mix.
   *
   * @return The location information for this mix.
   */
  public ServiceLocation getServiceLocation() {
    return m_mixLocation;
  }

  /**
   * Returns information about the operator of this mix.
   *
   * @return The operator information for this mix.
   */
  public ServiceOperator getServiceOperator() {
    return m_mixOperator;
  }

  /**
   * Returns information about the used software in this mix.
   *
   * @return The software information for this mix.
   */
  public ServiceSoftware getServiceSoftware() {
    return m_mixSoftware;
  }

  /**
   * Returns whether the mix is waiting for a cascade assignment. This value is only true, if the
   * mix is sending configure requests instead of HELO messages and if it is not already assigned
   * to a cascade. The returned value is only meaningful within the context of the infoservice.
   *
   * @return Whether this mix is currently free and can be assigned to a mixcascade.
   */
  public boolean isFreeMix() {
    return m_freeMix;
  }

  /**
   * Changes the state of this mix (whether it is free or not). If the mix is free, it will appear
   * in the list of free mixes. This mixes can be assigned to new cascades. If the specified value
   * is false, it will not appear in the list and cannot be assigned to new cascades. This value
   * is only meaningful within the context of the infoservice.
   *
   * @param a_freeMix Whether to treat this mix as free (true) or not (false).
   */
  public void setFreeMix(boolean a_freeMix) {
    m_freeMix = a_freeMix;
  }

  /**
   * This returns the filename (InfoService command), where this mix entry is posted at other
   * InfoServices. It's '/helo' if the mix is not treated as free of '/configure' if this mix
   * is currently free and needs to be assigned to a mixcascade.
   *
   * @return The filename where the information about this mix is posted at other infoservices
   *         when this entry is forwarded.
   */
  public String getPostFile() {
    String postFileName = "/helo";
    if (isFreeMix()) {
      postFileName = "/configure";
    }
    return postFileName;
  }

  /**
   * This returns the data posted when this mix information is forwarded to other
   * infoservices. It's the XML structure of this mix as we received it.
   *
   * @return The data posted to other infoservices when this entry is forwarded.
   */
  public byte[] getPostData() {
    return XMLUtil.toString(m_xmlStructure).getBytes();
  }

  /**
   * Returns the XML structure for this mix entry.
   *
   * @return The XML node for this mix entry (Mix node).
   */
  public Element getXmlStructure() {
    return m_xmlStructure;
  }

  /**
   * Returns an XML node for this MixInfo. This structure includes a Signature node if the
   * MixInfo information was created by the corresponding Mix itself.
   *
   * @param a_doc The XML document, which is the environment for the created XML node.
   *
   * @return The MixInfo XML node.
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

}
