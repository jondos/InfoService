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
import java.util.Hashtable;
import java.util.Observable;
import java.util.Vector;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import anon.util.XMLUtil;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

public class CertificateStore extends Observable {
 
  /**
   * Stores the name of the root node of the XML settings for this class.
   */
  private static final String XML_SETTINGS_ROOT_NODE_NAME = "TrustedCertificates";

 
  private Hashtable m_trustedCertificates;

  private Hashtable m_lockTable;
  
  private int m_lockIdPointer;
  

  public static String getXmlSettingsRootNodeName() {
    return XML_SETTINGS_ROOT_NODE_NAME;
  }


  /**
   * Creates a new certificate store.
   */
  public CertificateStore() {
    m_trustedCertificates = new Hashtable();
    m_lockTable = new Hashtable();
    m_lockIdPointer = 0;
  }

  
  public Vector getAllCertificates() {
    Vector returnedCertificates = new Vector();
    synchronized (m_trustedCertificates) {
      Enumeration allCertificates = m_trustedCertificates.elements();
      while (allCertificates.hasMoreElements()) {
        returnedCertificates.addElement(((CertificateContainer)(allCertificates.nextElement())).getInfoStructure());
      }
    }
    return returnedCertificates;
  }
  
  public Vector getAvailableCertificatesByType(int a_certificateType) {
    Vector returnedCertificates = new Vector();
    synchronized (m_trustedCertificates) {
      Enumeration allCertificates = m_trustedCertificates.elements();
      while (allCertificates.hasMoreElements()) {
        CertificateContainer currentCertificateContainer = (CertificateContainer)(allCertificates.nextElement());
        if ((currentCertificateContainer.getCertificateType() == a_certificateType) && currentCertificateContainer.isAvailable()) {
          /* return only enabled certificates of the specified type */
          returnedCertificates.addElement(currentCertificateContainer.getInfoStructure());
        }
      }
    }
    return returnedCertificates;
  }
  
  public int addCertificateWithVerification(JAPCertificate a_certificate, int a_certificateType, boolean a_onlyHardRemovable) {
    int lockId = -1;
    if ((a_certificateType == JAPCertificate.CERTIFICATE_TYPE_MIX) || (a_certificateType == JAPCertificate.CERTIFICATE_TYPE_INFOSERVICE)) {
      /* only mix and infoservice certificates can be verified at the moment */
      synchronized (m_trustedCertificates) {
        if (m_trustedCertificates.containsKey(a_certificate.getId()) == false) {
          /* the certificate isn't already in this certificate store */
          CertificateContainer certificateContainer = new CertificateContainer(a_certificate, a_certificateType, true);
          m_trustedCertificates.put(a_certificate.getId(), certificateContainer);
          /* verify the new certificate against all enabled root certificates */
          Enumeration rootCertificates = getAvailableCertificatesByType(JAPCertificate.CERTIFICATE_TYPE_ROOT).elements();
          boolean verificationSuccessful = false;
          while (rootCertificates.hasMoreElements() && (verificationSuccessful == false)) {
            /* try to verify the certificate against the next root certificate */
            JAPCertificate currentRootCertificate = ((CertificateInfoStructure)(rootCertificates.nextElement())).getCertificate();
            verificationSuccessful = a_certificate.verify(currentRootCertificate);
            if (verificationSuccessful == true) {
              /* we have found the parent certificate */
              certificateContainer.setParentCertificate(currentRootCertificate);
            }
          }
          /* we have added one certificate */
          setChanged();            
        }
        if (a_onlyHardRemovable == false) {
          /* add a lock to the lock table */
          lockId = getNextAvailableLockId();
          m_lockTable.put(new Integer(lockId), a_certificate.getId());
          /* also add the new lock to the lock list of the certificate */
          ((CertificateContainer)(m_trustedCertificates.get(a_certificate.getId()))).getLockList().addElement(new Integer(lockId));
        }
        else {
          /* enable the only hard removable flag */
          ((CertificateContainer)(m_trustedCertificates.get(a_certificate.getId()))).enableOnlyHardRemovable();
        }
        /* notify the observers, only meaningful, if setChanged() was called */
        notifyObservers(null);  
      }
    }
    return lockId;         
  }
  
  public int addCertificateWithoutVerification(JAPCertificate a_certificate, int a_certificateType, boolean a_onlyHardRemovable) {
    int lockId = -1;
    synchronized (m_trustedCertificates) {
      if (m_trustedCertificates.containsKey(a_certificate.getId()) == false) {
        CertificateContainer newCertificateContainer = new CertificateContainer(a_certificate, a_certificateType, false);
        m_trustedCertificates.put(a_certificate.getId(), newCertificateContainer);
        if (a_certificateType == JAPCertificate.CERTIFICATE_TYPE_ROOT) {
          /* maybe with the new certificate we can activiate some other certificates already stored
           * in this certificate store
           */
          activateAllDependentCertificates(a_certificate);
        }
        /* we have added one certificate */
        setChanged();                    
      }
      if (a_onlyHardRemovable == false) {
        /* add a lock to the lock table */
        lockId = getNextAvailableLockId();
        m_lockTable.put(new Integer(lockId), a_certificate.getId());
        /* also add the new lock to the lock list of the certificate */
        ((CertificateContainer)(m_trustedCertificates.get(a_certificate.getId()))).getLockList().addElement(new Integer(lockId));
      }
      else {
        /* enable the only hard removable flag */
        ((CertificateContainer)(m_trustedCertificates.get(a_certificate.getId()))).enableOnlyHardRemovable();
      }
      /* notify the observers, only meaningful, if setChanged() was called */
      notifyObservers(null);        
    }
    return lockId;
  }

  public void removeCertificateLock(int a_lockId) {
    synchronized (m_trustedCertificates) {
      CertificateContainer lockedCertificate = null;
      try {
        lockedCertificate = (CertificateContainer)(m_trustedCertificates.get(m_lockTable.get(new Integer(a_lockId))));
      }
      catch (Exception e) {
        /* can only happen, if a wrong lock id was specified, locks marked as invalid are not
         * throwing this exception
         */
        LogHolder.log(LogLevel.ERR, LogType.MISC, "Error while removing certificate lock. There is no lock with ID " + Integer.toString(a_lockId) + ".");
      }
      if (lockedCertificate != null) {
        /* it is not an invalid lock -> remove the lock from the certificate */
        lockedCertificate.getLockList().removeElement(new Integer(a_lockId));
        if (lockedCertificate.isOnlyHardRemovable() == false) {
          /* check whether it was the last lock on this certificate */
          if (lockedCertificate.getLockList().size() == 0) {
            /* no more locks -> we can remove the certificate */
            removeCertificate(lockedCertificate.getCertificate().getId());
          }
        }
      }
      /* remove the lock from the lock table */
      m_lockTable.remove(new Integer(a_lockId));
    }
  }

  public void removeCertificate(String a_certificateId) {
    synchronized (m_trustedCertificates) {
      CertificateContainer certificateToRemove = (CertificateContainer)(m_trustedCertificates.get(a_certificateId));
      if (certificateToRemove != null) {
        /* the hashtable contains the specified certificate */
        if (certificateToRemove.getCertificateType() == JAPCertificate.CERTIFICATE_TYPE_ROOT) {
          /* deactivate all certificates which depend on the specified certificate */
          deactivateAllDependentCertificates(certificateToRemove.getCertificate());
        }
        /* mark all active locks on that certificate as invalid (set the value to an empty string) */
        Enumeration activeLocks = certificateToRemove.getLockList().elements();
        while (activeLocks.hasMoreElements()) {
          m_lockTable.put(activeLocks.nextElement(), "");
        }
        /* remove the specified certificate */
        m_trustedCertificates.remove(a_certificateId);
        /* we have removed one certificate -> notify the observers */
        setChanged();
        notifyObservers(null);  
      }
    }
  }
  
  public void removeAllCertificates() {
    synchronized (m_trustedCertificates) {
      /* mark all active locks as invalid (set the value to an empty string) */
      Enumeration activeLocks = m_lockTable.keys();
      while (activeLocks.hasMoreElements()) {
        m_lockTable.put(activeLocks.nextElement(), "");
      }
      if (m_trustedCertificates.size() > 0) {
        /* remove all certificates */
        m_trustedCertificates.clear();
        /* we have removed some certificates */
        setChanged();
      }
      /* notify the observers, only meaningful, if setChanged() was called */      
      notifyObservers();
    }
  }
  
  public void setEnabled(String a_certificateId, boolean a_enabled) {
    synchronized (m_trustedCertificates) {
      CertificateContainer specifiedCertificate = (CertificateContainer)(m_trustedCertificates.get(a_certificateId));
      if (specifiedCertificate != null) {
        /* the certificate exists -> change the state if necessary */
        if (specifiedCertificate.isEnabled() != a_enabled) {
          /* we have to change the state */
          specifiedCertificate.setEnabled(a_enabled);
          if (specifiedCertificate.getCertificateType() == JAPCertificate.CERTIFICATE_TYPE_ROOT) {
            /* update the dependent certificates */
            if (a_enabled == true) {
              activateAllDependentCertificates(specifiedCertificate.getCertificate());
            }
            else {
              deactivateAllDependentCertificates(specifiedCertificate.getCertificate());
            }
          }
          /* we have changed the state of a certificate */
          setChanged();
        }
      }
      /* notify the observers, only meaningful, if setChanged() was called */      
      notifyObservers();
    }
  }
     
  public Element getSettingsAsXml(Document a_doc) {
    Element trustedCertificatesNode = a_doc.createElement(XML_SETTINGS_ROOT_NODE_NAME);
    synchronized (m_trustedCertificates) {
      Enumeration allCertificates = m_trustedCertificates.elements();
      while (allCertificates.hasMoreElements()) {
        CertificateContainer currentCertificateContainer = (CertificateContainer)(allCertificates.nextElement());
        if (currentCertificateContainer.isOnlyHardRemovable()) {
          trustedCertificatesNode.appendChild(currentCertificateContainer.getSettingsAsXml(a_doc));
        }
      }
    }
    return trustedCertificatesNode;
  }
  
  public void loadSettingsFromXml(Element a_trustedCertificatesNode) {
    synchronized (m_trustedCertificates) {
      /* first remove all already stored certificates */
      removeAllCertificates();
      /* load the settings from the XML description */
      NodeList certificateContainerNodes = a_trustedCertificatesNode.getElementsByTagName(CertificateContainer.getXmlSettingsRootNodeName());
      for (int i = 0; i < certificateContainerNodes.getLength(); i++) {
        Element certificateContainerNode = (Element) (certificateContainerNodes.item(i));
        try {
          CertificateContainer currentCertificateContainer = new CertificateContainer(certificateContainerNode);
          /* add the certificate to the list of trusted certificates */
          if (currentCertificateContainer.getCertificateNeedsVerification() == true) {
            addCertificateWithVerification(currentCertificateContainer.getCertificate(), currentCertificateContainer.getCertificateType(), true);
          }
          else {
            addCertificateWithoutVerification(currentCertificateContainer.getCertificate(), currentCertificateContainer.getCertificateType(), true);
          }
          /* enable or disable the certificate */
          setEnabled(currentCertificateContainer.getCertificate().getId(), currentCertificateContainer.isEnabled());  
        }
        catch (Exception e) {
          LogHolder.log(LogLevel.ERR, LogType.MISC, "CertificateStore: loadSettingsFromXml: Error while loading a CertificateContainer. Skipping this entry. Error: " + e.toString() + " - Invalid container was: " + XMLUtil.toString(certificateContainerNode));
        }
      }
    }
  }

  
  private void activateAllDependentCertificates(JAPCertificate a_certificate) {
    synchronized (m_trustedCertificates) {
      Enumeration allCertificates = m_trustedCertificates.elements();
      while (allCertificates.hasMoreElements()) {
        CertificateContainer currentCertificateContainer = (CertificateContainer)(allCertificates.nextElement());
        if (currentCertificateContainer.getCertificateNeedsVerification() == true) {
          JAPCertificate parentCertificate = currentCertificateContainer.getParentCertificate();
          if (parentCertificate == null) {
            /* the current certificate needs verification but is not verified -> try to do it
             * with the specified certificate
             */
            if (currentCertificateContainer.getCertificate().verify(a_certificate)) {
              /* verification of the current certificate was successful */
              currentCertificateContainer.setParentCertificate(a_certificate);
            }
          }
        }
      }
    }
  }  
  
  private void deactivateAllDependentCertificates(JAPCertificate a_certificate) {
    synchronized (m_trustedCertificates) {
      Enumeration allCertificates = m_trustedCertificates.elements();
      while (allCertificates.hasMoreElements()) {
        CertificateContainer currentCertificateContainer = (CertificateContainer)(allCertificates.nextElement());
        if (currentCertificateContainer.getCertificateNeedsVerification() == true) {
          JAPCertificate currentParentCertificate = currentCertificateContainer.getParentCertificate();
          if (currentParentCertificate != null) {
            if (currentParentCertificate.equals(a_certificate)) {
              /* the current certificate depends on the specified certificate -> deactivate it */
              currentCertificateContainer.setParentCertificate(null);
            }
          }
        }
      }
    }
  }
  
  private int getNextAvailableLockId() {
    while (m_lockTable.containsKey(new Integer(m_lockIdPointer)) || (m_lockIdPointer == -1)) {
      m_lockIdPointer++;
    }
    return m_lockIdPointer;
  }

}
