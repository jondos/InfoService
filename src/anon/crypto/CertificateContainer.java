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
package anon.crypto;

import java.util.Vector;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import anon.util.XMLUtil;

public class CertificateContainer {  
  
  /**
   * Stores the name of the root node of the XML settings for this class.
   */
  private static final String XML_SETTINGS_ROOT_NODE_NAME = "CertificateContainer";


  private JAPCertificate m_certificate;

  private JAPCertificate m_parentCertificate;
    
  private int m_certificateType;
      
  private boolean m_enabled;

  private boolean m_certificateNeedsVerification;
    
  private boolean m_onlyHardRemovable;
  
  private Vector m_lockList;
  

  public static String getXmlSettingsRootNodeName() {
    return XML_SETTINGS_ROOT_NODE_NAME;
  }

  
  public CertificateContainer(JAPCertificate a_certificate, int a_certificateType, boolean a_certificateNeedsVerification) {
    m_certificate = a_certificate;
    m_certificateType = a_certificateType;
    m_certificateNeedsVerification = a_certificateNeedsVerification;
    m_parentCertificate = null;
    m_enabled = true;
    m_onlyHardRemovable = false;
    m_lockList = new Vector();
  }
  
  public CertificateContainer(Element a_certificateContainerNode) throws Exception {
    /* parse the whole CertificateContainer XML structure */
    Element certificateTypeNode = (Element)(XMLUtil.getFirstChildByName(a_certificateContainerNode, "CertificateType"));
    if (certificateTypeNode == null) {
      throw (new Exception("CertificateContainer: Constructor: No CertificateType node found."));
    }
    /* CertificateType node found -> get the value */
    m_certificateType = XMLUtil.parseValue(certificateTypeNode, -1);
    if (m_certificateType == -1) {
      throw (new Exception("CertificateContainer: Constructor: Invalid CertificateType value."));
    }
    Element certificateNeedsVerificationNode = (Element)(XMLUtil.getFirstChildByName(a_certificateContainerNode, "CertificateNeedsVerification"));
    if (certificateNeedsVerificationNode == null) {
      throw (new Exception("CertificateContainer: Constructor: No CertificateNeedsVerification node found."));
    }
    /* CertificateNeedsVerification node found -> get the value */
    m_certificateNeedsVerification = XMLUtil.parseValue(certificateNeedsVerificationNode, true);
    Element certificateEnabledNode = (Element)(XMLUtil.getFirstChildByName(a_certificateContainerNode, "CertificateEnabled"));
    if (certificateEnabledNode == null) {
      throw (new Exception("CertificateContainer: Constructor: No CertificateEnabled node found."));
    }
    /* CertificateEnabled node found -> get the value */
    m_enabled = XMLUtil.parseValue(certificateEnabledNode, false);
    Element certificateDataNode = (Element)(XMLUtil.getFirstChildByName(a_certificateContainerNode, "CertificateData"));
    if (certificateDataNode == null) {
      throw (new Exception("CertificateContainer: Constructor: No CertificateData node found."));
    }
    /* CertificateData node found -> get the certificate */
    m_certificate = JAPCertificate.getInstance(XMLUtil.getFirstChildByName(certificateDataNode, JAPCertificate.XML_ELEMENT_NAME));
    if (m_certificate == null) {
      throw (new Exception("CertificateContainer: Constructor: Invalid CertificateData value. Cannot get the certificate."));
    }
    /* initialize also some other values */
    m_parentCertificate = null;
    /* only hard removable certificates can be persistent */
    m_onlyHardRemovable = true;
    m_lockList = new Vector();
  }
  
  
  public JAPCertificate getCertificate() {
    return m_certificate;
  }

  public void setParentCertificate(JAPCertificate a_parentCertificate) {
    m_parentCertificate = a_parentCertificate;
  }
  
  public JAPCertificate getParentCertificate() {
    return m_parentCertificate;
  }
  
  public int getCertificateType() {
    return m_certificateType;
  }
  
  public boolean getCertificateNeedsVerification() {
    return m_certificateNeedsVerification;
  }
  
  public boolean isAvailable() {
    boolean returnValue = false;
    synchronized (this) {
      returnValue = ((!m_certificateNeedsVerification) || (m_parentCertificate != null)) && m_enabled;
    }
    return returnValue;
  }
  
  public boolean isEnabled() {
    return m_enabled;
  }
  
  public void setEnabled(boolean a_enabled) {
    m_enabled = a_enabled;
  }
    
  public void enableOnlyHardRemovable() {
    m_onlyHardRemovable = true;
  }
  
  public boolean isOnlyHardRemovable() {
    return m_onlyHardRemovable;
  }
  
  public Vector getLockList() {
    return m_lockList;
  }
  
  public CertificateInfoStructure getInfoStructure() {
    return (new CertificateInfoStructure(m_certificate, m_parentCertificate, m_certificateType, m_enabled, m_certificateNeedsVerification, m_onlyHardRemovable));
  }
  
  public Element getSettingsAsXml(Document a_doc) {
    Element certificateContainerNode = a_doc.createElement(XML_SETTINGS_ROOT_NODE_NAME);
    synchronized (this) {
      Element certificateTypeNode = a_doc.createElement("CertificateType");
      XMLUtil.setValue(certificateTypeNode, m_certificateType);
      Element certificateNeedsVerificationNode = a_doc.createElement("CertificateNeedsVerification");
      XMLUtil.setValue(certificateNeedsVerificationNode, m_certificateNeedsVerification);
      Element certificateEnabledNode = a_doc.createElement("CertificateEnabled");
      XMLUtil.setValue(certificateEnabledNode, m_enabled);
      Element certificateDataNode = a_doc.createElement("CertificateData");
      certificateDataNode.appendChild(m_certificate.toXmlElement(a_doc));
      certificateContainerNode.appendChild(certificateTypeNode);
      certificateContainerNode.appendChild(certificateNeedsVerificationNode);
      certificateContainerNode.appendChild(certificateEnabledNode);
      certificateContainerNode.appendChild(certificateDataNode);
    }
    return certificateContainerNode;
  }

} 