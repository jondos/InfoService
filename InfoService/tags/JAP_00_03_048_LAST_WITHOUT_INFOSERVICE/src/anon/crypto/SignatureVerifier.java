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

import java.util.Enumeration;
import java.util.Vector;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import anon.util.XMLUtil;

/**
 * Manages the verification of all signatures.
 */
public class SignatureVerifier {

  /**
   * This is the class for all documents coming from mixes (mixinfo, cascadeinfo, status).
   */
  public static final int DOCUMENT_CLASS_MIX = 1;

  /**
   * This is the class for all documents coming from infoservices (infoserviceinfo).
   */
  public static final int DOCUMENT_CLASS_INFOSERVICE = 2;

  /**
   * This is the class for all documents with JAP update specific stuff (WebStart files,
   * minimal JAP version).
   */
  public static final int DOCUMENT_CLASS_UPDATE = 3;


  /**
   * Stores the name of the root node of the XML settings for this class.
   */
  private static final String XML_SETTINGS_ROOT_NODE_NAME = "SignatureVerification";
  

  /**
   * Stores the instance of SignatureVerifier (Singleton).
   */
  private static SignatureVerifier ms_svInstance;


  /**
   * Stores all trusted certificates.
   */
  private CertificateStore m_trustedCertificates;

  /**
   * Stores whether signature checking is enabled or disabled. If this value is false, every
   * document is accept without checking the signature.
   */
  private boolean m_checkSignatures;


  /**
   * Creates a new instance of SignatureVerifier.
   */
  private SignatureVerifier() {
    m_trustedCertificates = new CertificateStore();
    m_checkSignatures = true;
  }

  /**
   * Returns the instance of SignatureVerifier (Singleton). If there is no instance, there is a
   * new one created.
   *
   * @return The SignatureVerifier instance.
   */
  public static SignatureVerifier getInstance() {
    synchronized (SignatureVerifier.class) {
      if (ms_svInstance == null) {
        ms_svInstance = new SignatureVerifier();
      }
    }
    return ms_svInstance;
  }

  public static String getXmlSettingsRootNodeName() {
    return XML_SETTINGS_ROOT_NODE_NAME;
  }


  public void setCheckSignatures(boolean a_checkSignaturesEnabled) {
    m_checkSignatures = a_checkSignaturesEnabled;
  }
  
  public boolean isCheckSignatures() {
    return m_checkSignatures;
  }
  
  public CertificateStore getVerificationCertificateStore() {
    return m_trustedCertificates;
  }

  /**
   * Verifies the signature of an XML document against the store of trusted certificates.
   * This methode returns true, if the signature of the document is valid, the signing
   * certificate can be derived from one of the trusted certificates (or is one of them) and
   * if all of the needed certificates in the path have the permission to sign documents of this
   * class. This method also returns true if SignatureCheck for the specified DocumentClass is disabled!
   *
   * @param a_rootNode The root node of the document. The Signature node must be one of the
   *                   children of the root node.
   * @param a_documentClass The class of the document. Look at the constants in this class.
   * @param a_signerId The ID of the signer of this document.
   *
   * @return True, if the signature (and appended certificate) could be verified against the
   *         trusted root certificates or false if not.
   */
  public boolean verifyXml(Element a_rootNode, int a_documentClass) {
    boolean verificationSuccessful = false;
    synchronized (m_trustedCertificates) {
      if (m_checkSignatures == false) {
        /* accept every document without testing the signature */
        verificationSuccessful = true;
      }
      else {
        /* get the direct useable certificates depending on the document type */
        Vector additionalCertificateInfoStructures = new Vector();
        switch (a_documentClass) {
          case DOCUMENT_CLASS_MIX: {
            additionalCertificateInfoStructures = m_trustedCertificates.getAvailableCertificatesByType(JAPCertificate.CERTIFICATE_TYPE_MIX);
            break;
          }
          case DOCUMENT_CLASS_INFOSERVICE: {
            additionalCertificateInfoStructures = m_trustedCertificates.getAvailableCertificatesByType(JAPCertificate.CERTIFICATE_TYPE_INFOSERVICE);
            break;
          }
          case DOCUMENT_CLASS_UPDATE: {
            additionalCertificateInfoStructures = m_trustedCertificates.getAvailableCertificatesByType(JAPCertificate.CERTIFICATE_TYPE_UPDATE);
            break;
          }
        }
        Vector additionalCertificates = new Vector();
        Enumeration additionalCertificatesEnumerator = additionalCertificateInfoStructures.elements();
        while (additionalCertificatesEnumerator.hasMoreElements()) {
          additionalCertificates.addElement(((CertificateInfoStructure)(additionalCertificatesEnumerator.nextElement())).getCertificate());
        }
        /* get the root certificates for verifying appended certificates */
        Vector rootCertificateInfoStructures = m_trustedCertificates.getAvailableCertificatesByType(JAPCertificate.CERTIFICATE_TYPE_ROOT);
        Vector rootCertificates = new Vector();
        Enumeration rootCertificatesEnumerator = rootCertificateInfoStructures.elements();
        while (rootCertificatesEnumerator.hasMoreElements()) {
          rootCertificates.addElement(((CertificateInfoStructure)(rootCertificatesEnumerator.nextElement())).getCertificate());
        }
        /* now we have everything -> verify the signature */
        try {
          if (XMLSignature.verify(a_rootNode, rootCertificates, additionalCertificates) != null) {
            /* verification of the signature was successful */
            verificationSuccessful = true;
          }
        }
        catch (Exception e) {
          /* this should only happen, if there is no signature child node */
        }
      }
    }
    return verificationSuccessful;
  }

  public Element getSettingsAsXml(Document a_doc) {
    Element signatureVerificationNode = a_doc.createElement(XML_SETTINGS_ROOT_NODE_NAME);
    synchronized (m_trustedCertificates) {
      Element checkSignaturesNode = a_doc.createElement("CheckSignatures");
      XMLUtil.setValue(checkSignaturesNode, m_checkSignatures);
      Element trustedCertificatesNode = m_trustedCertificates.getSettingsAsXml(a_doc);
      signatureVerificationNode.appendChild(checkSignaturesNode);
      signatureVerificationNode.appendChild(trustedCertificatesNode);    
    }
    return signatureVerificationNode;
  }
  
  public void loadSettingsFromXml(Element a_signatureVerificationNode) throws Exception {
    synchronized (m_trustedCertificates) {
      /* parse the whole SignatureVerification XML structure */
      Element checkSignaturesNode = (Element)(XMLUtil.getFirstChildByName(a_signatureVerificationNode, "CheckSignatures"));
      if (checkSignaturesNode == null) {
        throw (new Exception("SignatureVerifier: loadSettingsFromXml: No CheckSignatures node found."));
      }
      /* CheckSignatures node found -> get the value */
      m_checkSignatures = XMLUtil.parseValue(checkSignaturesNode, true);
      Element trustedCertificatesNode = (Element)(XMLUtil.getFirstChildByName(a_signatureVerificationNode, CertificateStore.getXmlSettingsRootNodeName()));
      if (trustedCertificatesNode == null) {
        throw (new Exception("SignatureVerifier: loadSettingsFromXml: No TrustedCertificates node found."));
      }
      /* TrustedCertificates node found -> load the certificates  */
      m_trustedCertificates.loadSettingsFromXml(trustedCertificatesNode);
    }  
  }

}
