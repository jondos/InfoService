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

/**
 * Certificate store class. It contains the (root) certificates in a hashtable structure,
 * that are used within the verification process.
 *
 */
final public class JAPCertificateStore
{
	private Hashtable m_HTCertStore = null;

	private JAPCertificateStore()
	{
		m_HTCertStore = new Hashtable();
	}

	/**
	 * Creates any empty certificate store.
	 *
	 * @return certificate store
	 */
	public static JAPCertificateStore getInstance()
	{
		return new JAPCertificateStore();
	}

	/**
	 * Creates the certificate store by using a given file name (string).
	 *
	 * @param a_strCertFileName The file name of the certificate which
	 * becomes the first entry of the store.
	 */
	/*	public JAPCertificateStore(String a_strCertFileName)
	 {
	  this();
	  InputStream in = null;
	  try
	  {
	   // First: Look for the named certificate in JAP.jar
	   in = Class.forName("JAP").getResourceAsStream(a_strCertFileName);
	  }
	  catch (Exception e)
	  {
	   try
	   {
		// Second: First step failed, so we look for a file named <strCertFileName>
		in = new FileInputStream(a_strCertFileName);
	   }
	   catch (Exception e1)
	   {
	   }
	  }
	  try
	  {
	   addCertificate(JAPCertificate.getInstance(in));
	  }
	  catch (Exception e2)
	  {
	 */
	/** @todo This exception handling is about to be removed...
	 *
	 */

	/**
	 *	Second step should never fail; if it happens:
	 *   Third: We try to find the root certificate via hard coded path
	 */
	/*			File file = new File("../certificates/japroot.cer");
	   try
	   {
		addCertificate(JAPCertificate.getInstance(file));
	   }
	   catch (Exception e3)
	   {
		// Third step also failed, give up
		LogHolder.log(LogLevel.ERR, LogType.MISC,
			 "JAPCertificateStore:constructor(String) finally failed!");

	   }
	  }
	  try
	  {
	   in.close();
	  }
	  catch (Throwable t2)
	  {
	  }
	  ;

	 }
	 */
	/**
	 * Creates a certificate store by using a XML nodelist of certificates (including
	 * the status).
	 * e.g. retrieved XML structure <CertificateAuthorities>
	 * @param a_nlX509CertsPlusStatus The XML nodelist that contains the CAs.
	 * @return null if not all certificates could be read
	 */
	public static JAPCertificateStore getInstance(NodeList a_nlX509CertsPlusStatus)
	{
		try
		{
			JAPCertificateStore js = getInstance();
			for (int i = 0; i < a_nlX509CertsPlusStatus.getLength(); i++)
			{
				Node nodeCertAuth = a_nlX509CertsPlusStatus.item(i);
				Element elemEnabled = (Element) XMLUtil.getFirstChildByName(nodeCertAuth, "Enabled");
				Element elemKeyInfo = (Element) XMLUtil.getFirstChildByName(nodeCertAuth, "KeyInfo");
				Element elemX509Data = (Element) XMLUtil.getFirstChildByName(elemKeyInfo, "X509Data");
				Element elemX509Cert = (Element) XMLUtil.getFirstChildByName(elemX509Data, "X509Certificate");
				JAPCertificate cert = JAPCertificate.getInstance(elemX509Cert);
				boolean bEnabled = XMLUtil.parseNodeBoolean(elemEnabled, false);
				if (!js.addCertificate(cert, bEnabled))
				{
					return null;
				}
			}
			return js;
		}
		catch (Exception e)
		{
			return null;
		}
	}

	/**
	 * Adds a certificate to the store (including status)
	 *
	 * @param a_cert The certificate to be added.
	 * @param a_bStatus The status that's being set for the certificate.
	 * @return true if successful
	 * @return false otherwise
	 */
	public synchronized boolean addCertificate(JAPCertificate a_cert, boolean a_bStatus)
	{
		if (a_cert == null)
		{
			return false;
		}
		a_cert.setEnabled(a_bStatus);
		try
		{
			m_HTCertStore.put(JAPCertificateStoreId.getId(a_cert), a_cert);
			return true;
		}
		catch (Exception e)
		{
			return false;
		}
	}

	/**
	 * Sets the certificate active or deactive.
	 *
	 * @param a_cert The certificate that is about to be activated.
	 * @param bEnabled status of cert
	 */
	public void enableCertificate(JAPCertificate a_cert, boolean bEnabled)
	{
		if (a_cert == null)
		{
			return;
		}
		a_cert.setEnabled(bEnabled);
		m_HTCertStore.put(JAPCertificateStoreId.getId(a_cert), a_cert);
	}

	/**
	 * Removes a certificate from the store.
	 *
	 * @param a_cert The certificate that is about to be removed.
	 * @return The removed certificate is returned.
	 */
	public JAPCertificate removeCertificate(JAPCertificate a_cert)
	{
		return (JAPCertificate) m_HTCertStore.remove(JAPCertificateStoreId.getId(a_cert));
	}

	/**
	 * Checks whether a given certificate does already exist in the store.
	 *
	 * @param a_cert The certificate.
	 * @return true if the certificate exists
	 * @return false otherwise
	 */
	public boolean checkCertificateExists(JAPCertificate a_cert)
	{
		return m_HTCertStore.containsKey(JAPCertificateStoreId.getId(a_cert));
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
	 * Returns the keys of the hashtable structure.
	 * @see anon.crypto.JAPCertificateStoreId
	 * @return the ids of the certificates.
	 */
	public Enumeration keys()
	{
		return m_HTCertStore.keys();
	}

	/**
	 * Returns the certificates that are stored in the hashtable structure.
	 *
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
  public Vector getAllEnabledCertificates() {
    Vector r_certificatesVector = new Vector();
    Enumeration certificatesEnumeration = m_HTCertStore.elements();
    while (certificatesEnumeration.hasMoreElements()) {
      JAPCertificate currentCertificate = (JAPCertificate)(certificatesEnumeration.nextElement());
      if (currentCertificate.getEnabled() == true) {
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
	public Element toXmlNode(Document a_doc)
	{
		Element r_elemCAs = a_doc.createElement("CertificateAuthorities");

		Enumeration enum = elements();

		while (enum.hasMoreElements())
		{
			Element elemCA = a_doc.createElement("CertificateAuthority");
			r_elemCAs.appendChild(elemCA);
			Element elemEnabled = a_doc.createElement("Enabled");
			elemCA.appendChild(elemEnabled);
			JAPCertificate cert = (JAPCertificate) enum.nextElement();
			boolean bEnabled = cert.getEnabled();

			if (bEnabled)
			{
				elemEnabled.appendChild(a_doc.createTextNode("true"));
			}
			else
			{
				elemEnabled.appendChild(a_doc.createTextNode("false"));

			}
			elemCA.appendChild(cert.toXmlNode(a_doc));
		}

		return r_elemCAs;
	}

}
