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

/**
 * Stores a certification path with all included certificates.
 * Keep in mind, that this CertPath adds a new Certificate to the BEGINNING
 * of the Vector. That means the first Certificate is at the end of the Vector.
 * @see also gui.CertDetailsDialog, anon.crypto.XMLSignature
 * @author Robert Hirschberger
 */
public class CertPath
{
	/** the number of certificates included in this CertPath */
	private int m_certCount;
	/** the certificate class of the rootCerts that may verify this  CertPath */
	private int m_rootCertificateClass;
	/** the included certificates */
	private Vector m_certificates;

	/**
	 * Creates a new CertPath Object from a given Certificate
	 * @param firstCert The first certifiacte of the path
	 *                  (it will be on the lowest Level of the cert hierarchy)
	 */
	protected CertPath(JAPCertificate firstCert)
	{
		if(firstCert == null)
		{
			return;
		}
		m_certificates = new Vector();
		this.add(firstCert);
	}

	/*
	public CertPath(Vector a_certificateVector)
	{
		Enumeration certEnumerator = a_certificateVector.elements();
		m_certificates = new Vector();
		m_certCount = 0;
		while(certEnumerator.hasMoreElements())
		{
			this.add((JAPCertificate)certEnumerator.nextElement());
			m_certCount++;
		}
	}*/

	/**
	 * Adds a certificate to next higher level of this CertPath,
	 * if the cert is not already included
	 * @param a_certificate the certificate to add
	 */
	protected void add(JAPCertificate a_certificate)
	{
	    if(!m_certificates.contains(a_certificate))
		{
			m_certificates.insertElementAt(a_certificate, 0);
			m_certCount++;
		}
		/*Enumeration certificates = m_certificates.elements();
		while(certificates.hasMoreElements())
		{
			JAPCertificate cert = (JAPCertificate)certificates.nextElement();
			if(cert.equals(a_certificate))
			{
				return;
			}
		}
		m_certificates.add(0, a_certificate);
		m_certCount++;*/
	}

	/**
	 * Removes the specified certificate from this CertPath
	 * @param a_certificate the certificate to remove
	 */
	protected void remove(JAPCertificate a_certificate)
	{
		m_certificates.removeElement(a_certificate);
		m_certCount--;
    }

	/**
	 * Removes all certificates except the one on the lowest level
	 * of this certPath
	 */
	protected void removeAllButLast(){
		JAPCertificate firstCertificate = this.getFirstCertificate();
		m_certificates.removeAllElements();
		m_certCount = 0;
		this.add(firstCertificate);
	}

	/**
	 * Returns the top level certificate (it is the one that was last added)
	 * @return the last added certificate
	 */
	protected JAPCertificate getLatestAddedCertificate()
	{
		return (JAPCertificate)m_certificates.firstElement();
	}

	/**
	 * Returns the certificate from the lowest Level of this CertPath (the one
	 * that was added at first). If this CertPath is from a Mix this would be
	 * the Mix Certificate.
	 * @return the first added certificate
	 */
	public JAPCertificate getFirstCertificate()
	{
		return (JAPCertificate)m_certificates.lastElement();
	}

	/**
	 * Returns the certificate from the second lowest Level of this CertPath
	 * (the one that was added at Second).
	 * If this CertPath is from a Mix this would be the Operator Certificate.
	 * @return the second added certificate
	 */
	public JAPCertificate getSecondCertificate()
	{
		if(m_certificates.size() <= 1)
		{
			return null;
		}
		//return the Certificate at Index size-2 (size-1 would be the last one)
		return (JAPCertificate)m_certificates.elementAt(m_certificates.size()-2);
	}

	/**
	 * Sets the certificate class for the root certificates that can verify this
	 * Cert Path. This Method is usually called by the getVerifiedXml()-from the
	 * SignatureVerifier.
	 * It translates the document class from the SignatureVerifier to the
	 * certificate class from JAPCertificate
	 * @see also anon.crypto.SignatureVerifier.getVerifiedXml()
	 * @param a_documentClass a document class from the SignatureVerifier
	 */
	protected void setDocType(int a_documentClass)
	{
		switch(a_documentClass)
		{
			case SignatureVerifier.DOCUMENT_CLASS_MIX:
				m_rootCertificateClass = JAPCertificate.CERTIFICATE_TYPE_ROOT_MIX;
				break;
			case SignatureVerifier.DOCUMENT_CLASS_INFOSERVICE:
			    m_rootCertificateClass = JAPCertificate.CERTIFICATE_TYPE_ROOT_INFOSERVICE;
				break;
			case SignatureVerifier.DOCUMENT_CLASS_UPDATE:
				m_rootCertificateClass = JAPCertificate.CERTIFICATE_TYPE_UPDATE;
				break;
		}
	}

	/**
	 * Tries to find a verifying root certificate for the top level cert.
	 * After that we get the CertificateInfoStructure for this cert from the
	 * SignatureVerifier.
	 * @see also anon.crypto.SignatureVerifier.getCertificateInfoStructure()
	 * @param checkValidity shall the validity be checked?
	 * @return the CertificateInfoStructure for the verifing certificate,
	 *         null if there is none.
	 */
	private CertificateInfoStructure getVerifier(boolean checkValidity)
	{
		Vector rootCertificates = SignatureVerifier.getInstance().getVerificationCertificateStore().
			                                 getAvailableCertificatesByType(m_rootCertificateClass);
	    JAPCertificate lastCertificate = (JAPCertificate)this.getLatestAddedCertificate();
		CertificateInfoStructure verifyingCertificate = (CertificateInfoStructure)lastCertificate.getVerifier(rootCertificates.elements(), checkValidity);
		if(verifyingCertificate != null)
		{
			return verifyingCertificate;//SignatureVerifier.getInstance().getVerificationCertificateStore().getCertificateInfoStructure(verifyingCertificate);
		}
		//if there was no verifier in the available root certs, try to find a verifier in the unavailable ones
		rootCertificates = SignatureVerifier.getInstance().getVerificationCertificateStore().
			                                 getUnavailableCertificatesByType(m_rootCertificateClass);
		verifyingCertificate = (CertificateInfoStructure)lastCertificate.getVerifier(rootCertificates.elements(),checkValidity);
		if(verifyingCertificate != null)
		{
			return verifyingCertificate;//SignatureVerifier.getInstance().getVerificationCertificateStore().getCertificateInfoStructure(verifyingCertificate);
		}
		return null;
	}

	/**
	 * Returns the number of ceritificates in this CertPath
	 * @return the number of ceritificates in this CertPath
	 */
	public int getCertCount()
	{
		return m_certCount;
	}

	/**
	 * Creates an Enumeration of CertificateInfoStructures of the included certs.
	 * The first element of this Enumeration is the verifier of this CertPath if
	 * there is one. The isEnabled() field of the CIS is used to mark if the certs
	 * are verified.
	 * @return an Enumeration of CertificateInfoStructures of the included certs plus
	 *         the verifier as first element if ther is one.
	 */
	public Enumeration getCertificates()
	{
		boolean verifierFound = false;
	    Enumeration certificates = m_certificates.elements();
		Vector certificateIS = new Vector();
		//try to find a verifier
	    CertificateInfoStructure verifier = this.getVerifier(false);

	    if(verifier != null)
		{
			verifierFound = true;
			//if the verifying certificate is not already in the CertPath we add it to the top level
			if(!m_certificates.contains(verifier.getCertificate()))//.equals(this.getLatestAddedCertificate()))
			{
				certificateIS.addElement(verifier);
			}
		}
		if(certificates.hasMoreElements())
		{
			//Mark the first CIS, if there is no verifier for the CertsPath
			JAPCertificate cert = (JAPCertificate)certificates.nextElement();
			certificateIS.addElement(new CertificateInfoStructure(cert, null, 1,
					  verifierFound, false, false, false));
			while(certificates.hasMoreElements())
			{
				cert = (JAPCertificate)certificates.nextElement();
				certificateIS.addElement(new CertificateInfoStructure(cert, null, 1, true, false, false, false));
			}
		}
		return certificateIS.elements();
	}

	/**
	 * Creates a human readable List in String-Format using the CommonNames of
	 * the included certs. This is mainly used for debugging. To display a CertPath
	 * use a CertDetailsDialog and call the getCertificates()-Method
	 * @return a String representation of this CertPath object
	 */
	public String toString()
	{
		String certPath = new String("Certification Path (" + getCertCount() + "):");
		String tabs = new String();
		Enumeration certificates = m_certificates.elements();

	    while(certificates.hasMoreElements())
		{
			tabs += "\t";
			certPath += "\n" + tabs +
				        ((JAPCertificate)certificates.nextElement()).getSubject().getCommonName();
		}
		return certPath;
	}
}
