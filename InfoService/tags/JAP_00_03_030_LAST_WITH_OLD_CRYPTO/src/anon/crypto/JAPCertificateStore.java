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
package anon.crypto;

/* Hint: This file may be only a copy of the original file which is always in the JAP source tree!
 * If you change something - do not forget to add the changes also to the JAP source tree!
 */
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import anon.util.XMLUtil;
import anon.util.IXMLEncodable;
import anon.util.XMLParseException;

/**
 * Certificate store class. It contains the (root) certificates in a hashtable structure,
 * that are used within the verification process.
 */
final public class JAPCertificateStore implements IXMLEncodable
{
	private Hashtable m_HTCertStore = null;

	/**
	 * Creates a new certificate store.
	 */
	public JAPCertificateStore()
	{
		m_HTCertStore = new Hashtable();
	}

	/**
	 * Creates a certificate store by using a XML nodelist of certificates (including
	 * the status).
	 * e.g. retrieved XML structure <CertificateAuthorities>
	 * @param a_nlX509CertsPlusStatus The XML nodelist that contains the CAs.
	 * @exception XMLParseException if an error occurs while parsing the XML structure
	 */
	public JAPCertificateStore(NodeList a_nlX509CertsPlusStatus)
		throws XMLParseException
	{
		this();

		// if no certificates are given, return an empty certificate store
		if (a_nlX509CertsPlusStatus == null)
		{
			return;
		}

			for (int i = 0; i < a_nlX509CertsPlusStatus.getLength(); i++)
			{
			try
			{
				Node nodeCertAuth = a_nlX509CertsPlusStatus.item(i);
				Element elemEnabled = (Element) XMLUtil.getFirstChildByName(nodeCertAuth, "Enabled");
				Element elemKeyInfo = (Element) XMLUtil.getFirstChildByName(nodeCertAuth, "KeyInfo");
				Element elemX509Data = (Element) XMLUtil.getFirstChildByName(elemKeyInfo, "X509Data");
				Element elemX509Cert = (Element) XMLUtil.getFirstChildByName(elemX509Data, "X509Certificate");
				JAPCertificate cert = JAPCertificate.getInstance(elemX509Cert);
				cert.setEnabled(XMLUtil.parseNodeBoolean(elemEnabled, false));
				addCertificate(cert);
			}
			catch (Exception a_e)
		{
				// adding a certificate does not need to be successful, but it doesn`t matter if it is not
			}
		}
	}

	/* Duplicates this CertificateStore and including duplicates of the Certifcates*/
	public Object clone()
	{
		JAPCertificateStore certs = new JAPCertificateStore();
		Enumeration enumer = elements();
		while (enumer.hasMoreElements())
		{
			JAPCertificate cert = (JAPCertificate) enumer.nextElement();
			cert = (JAPCertificate) cert.clone();
			certs.m_HTCertStore.put(cert.getId(), cert);
		}
		return certs;
	}

	/**
	 * Adds a certificate to the store.
	 *
	 * @param a_cert The certificate to be added.
	 * @return true if successful, false otherwise
	 */
	public synchronized boolean addCertificate(JAPCertificate a_cert)
	{
		if (a_cert == null)
		{
			return false;
		}
		try
		{
			m_HTCertStore.put(a_cert.getId(), a_cert);
			return true;
		}
		catch (Exception e)
		{
			return false;
		}
	}

	/**
	 * Removes a certificate from the store.
	 *
	 * @param a_cert The certificate that is about to be removed.
	 * @return The removed certificate is returned.
	 */
	public JAPCertificate removeCertificate(JAPCertificate a_cert)
	{
		return (JAPCertificate) m_HTCertStore.remove(a_cert.getId());
	}

	/**
	 * Checks whether a given certificate does already exist in the store.
	 *
	 * @param a_cert The certificate.
	 * @return true if the certificate exists
	 * @return false otherwise
	 */
	public boolean contains(JAPCertificate a_cert)
	{
		return m_HTCertStore.containsKey(a_cert.getId());
	}

	/**
	 * Returns the size (= number of certificates) of the certificate store.
	 *
	 * @return size
	 */
	public int size()
	{
		return m_HTCertStore.size();
	}

	/**
	 * Returns the keys respective the ids of the hashtable structure.
	 * @see anon.crypto.JAPCertificateStoreId
	 * @return the ids of the certificates.
	 */
	public Enumeration keys()
	{
		return m_HTCertStore.keys();
	}

	/**
	 * Returns the certificates that are stored in the hashtable structure.
	 * @return Certificates
	 */
	public Enumeration elements()
	{
		return m_HTCertStore.elements();
	}

	/**
	 * Returns a Vector with the snapshot of all enabled certificates (JAPCertificate) in this
	 * JAPCertificateStore. The returned Vector is independent from this JAPCertificateStore,
	 * only the certificates are the same.
	 *
	 * @return The Vector with all enabled certificates.
	 */
	public Vector getAllEnabledCertificates()
	{
		Vector r_certificatesVector = new Vector();
		Enumeration certificatesEnumeration = m_HTCertStore.elements();
		while (certificatesEnumeration.hasMoreElements())
		{
			JAPCertificate currentCertificate = (JAPCertificate) (certificatesEnumeration.nextElement());
			if (currentCertificate.getEnabled() == true)
			{
				r_certificatesVector.addElement(currentCertificate);
			}
		}
		return r_certificatesVector;
	}

	/**
	 * Creates the trusted CA XML node
	 *
	 * @param a_doc The XML document, which is the environment for the created XML node.
	 * @return The trusted CAs XML node.
	 */
	public Element toXmlElement(Document a_doc)
	{
		Element r_elemCAs = a_doc.createElement("CertificateAuthorities");

		Enumeration enumer = elements();

		while (enumer.hasMoreElements())
		{
			Element elemCA = a_doc.createElement("CertificateAuthority");
			r_elemCAs.appendChild(elemCA);
			Element elemEnabled = a_doc.createElement("Enabled");
			elemCA.appendChild(elemEnabled);
			JAPCertificate cert = (JAPCertificate) enumer.nextElement();
			boolean bEnabled = cert.getEnabled();

			if (bEnabled)
			{
				elemEnabled.appendChild(a_doc.createTextNode("true"));
			}
			else
			{
				elemEnabled.appendChild(a_doc.createTextNode("false"));

			}
			Element elemKeyInfo = a_doc.createElement("KeyInfo");
			Element elemX509Data = a_doc.createElement("X509Data");
			elemCA.appendChild(elemKeyInfo);
			elemKeyInfo.appendChild(elemX509Data);
			elemX509Data.appendChild(cert.toXmlElement(a_doc));
		}

		return r_elemCAs;
	}

}
