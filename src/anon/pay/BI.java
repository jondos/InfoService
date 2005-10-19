package anon.pay;

import java.security.InvalidKeyException;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import anon.crypto.IMyPublicKey;
import anon.crypto.JAPCertificate;
import anon.crypto.JAPSignature;
import anon.util.IXMLEncodable;
import anon.util.XMLUtil;

/**
 * This class represents a known BI with its unique name and x509 public
 * certificate (verifyer).
 * It can be converted to an XML structure, the structure is as follows:
 *
 * <BI version="1.0">
 *   <BIName> unique name </BIName>
 *   <HostName> www.bezahlinstanz.de </HostName>
 *   <PortNumber> 1234 </PortNumber>
 *   <X509Certificate>
 *       ... the certificate (JAPCertificate)
 *   </X509Certificate>
 * </BI>
 *
 * @author Bastian Voigt
 * @version 1.0
 */
public class BI implements IXMLEncodable
{
	private String m_biID;
	private String m_biName;
	private String m_hostName;
	private int m_portNumber;

	private JAPSignature m_veryfire;
	private JAPCertificate m_cert;

	public static final String XML_ELEMENT_NAME = "BI";

	public BI(String a_biID, String a_biName, String a_hostName, int a_portNumber, JAPCertificate a_cert) throws
		Exception
	{
		m_biID = a_biID;
		m_cert = a_cert;
		m_biName = a_biName;
		m_hostName = a_hostName;
		m_portNumber = a_portNumber;
	}

	public BI(Element elemRoot) throws Exception
	{
		setValues(elemRoot);
	}

	/** constructs a BI object from a binary X509 certificate
	 * and some additional data
	 */
	public BI(byte[] barCert, String biName, String hostName, int portNumber)
	{
		JAPCertificate cert = JAPCertificate.getInstance(barCert);
		m_cert = cert;
		LogHolder.log(LogLevel.DEBUG, LogType.PAY,
					  "BI biName=" + biName + ", Host=" + hostName + ", Port=" + portNumber);

		/** @todo does this work? i don't believe it... */
		m_biName = biName; //cert.getSubject().CN.toString();
		//m_biID = biID;
		m_hostName = hostName;
		m_portNumber = portNumber;

		/*		m_veryfire = new JAPSignature();
		  try
		  {
		   m_veryfire.initVerify(cert.getPublicKey());
		  }
		  catch (InvalidKeyException ex)
		  {
		   m_veryfire = null;
		  }*/
	}

	private void setValues(Element elemRoot) throws Exception
	{
		String rootName = elemRoot.getTagName();
		if (!rootName.equals(XML_ELEMENT_NAME))
		{
			throw new Exception("BI wrong XML format");
		}
		Element elem = (Element) XMLUtil.getFirstChildByName(elemRoot, "BIID");
		m_biID = XMLUtil.parseValue(elem, null);

		elem = (Element) XMLUtil.getFirstChildByName(elemRoot, "BIName");
		m_biName = XMLUtil.parseValue(elem, null);

		elem = (Element) XMLUtil.getFirstChildByName(elemRoot, "HostName");
		m_hostName = XMLUtil.parseValue(elem, null);

		elem = (Element) XMLUtil.getFirstChildByName(elemRoot, "PortNumber");
		m_portNumber = XMLUtil.parseValue(elem, 0);

		elem = (Element) XMLUtil.getFirstChildByName(elemRoot, "TestCertificate");
		if (elem != null)
		{
			elem = (Element) XMLUtil.getFirstChildByName(elem, JAPCertificate.XML_ELEMENT_NAME);
			if (elem != null)
			{
				JAPCertificate cert = JAPCertificate.getInstance(elem);
				if (cert != null)
				{
					m_cert = cert;
				}
			}
		}
	}

	/** returns the BI's unique name (identifier) */
	public String getName()
	{
		return m_biName;
	}

	/** returns a JAPSignature object for veriying this BI's signatures */
	public JAPSignature getVerifier() throws InvalidKeyException
	{
		if (m_veryfire == null)
		{
			m_veryfire = new JAPSignature();
			m_veryfire.initVerify(m_cert.getPublicKey());
		}
		return m_veryfire;
	}

	/** returns the hostname of the host on which this BI is running */
	public String getHostName()
	{
		return m_hostName;
	}

	/** gets the port number */
	public int getPortNumber()
	{
		return m_portNumber;
	}

	/**
	 * toXmlElement
	 *
	 * @param a_doc Document
	 * @todo complete
	 * @return Element
	 */
	public Element toXmlElement(Document a_doc)
	{
		Element elemRoot = a_doc.createElement(XML_ELEMENT_NAME);
		elemRoot.setAttribute("version", "1.0");

		Element elem = a_doc.createElement("BIID");
		elemRoot.appendChild(elem);
		XMLUtil.setValue(elem, m_biID);

		elem = a_doc.createElement("BIName");
		elemRoot.appendChild(elem);
		XMLUtil.setValue(elem, m_biName);

		elem = a_doc.createElement("HostName");
		elemRoot.appendChild(elem);
		XMLUtil.setValue(elem, m_hostName);

		elem = a_doc.createElement("PortNumber");
		elemRoot.appendChild(elem);
		XMLUtil.setValue(elem, Integer.toString(m_portNumber));

		elem = a_doc.createElement("TestCertificate");
		elemRoot.appendChild(elem);
		elem.appendChild(m_cert.toXmlElement(a_doc));
		return elemRoot;
	}

	/**
	 * getCertificate
	 *
	 * @return IMyPublicKey
	 */
	public JAPCertificate getCertificate()
	{
		return m_cert;
	}

	public String toString()
	{
		return new String(m_biName);
	}

	public String getID()
	{
		return m_biID;
	}
}
