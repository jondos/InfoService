package anon.pay;

import anon.crypto.JAPSignature;
import anon.crypto.JAPCertificate;
import java.security.InvalidKeyException;
import anon.util.IXMLEncodable;
import org.w3c.dom.Element;
import org.w3c.dom.Document;
import anon.util.XMLUtil;
import anon.pay.xml.XMLJapPublicKey;
import anon.crypto.IMyPublicKey;
import logging.LogType;
import logging.LogHolder;
import logging.LogLevel;

/**
 * This class represents a known BI with its unique name and a public key (verifyer).
 * It can be converted to an XML structure, the structure is as follows:
 *
 * <BI version="1.0">
 *   <BIName> unique name </BIName>
 *   <HostName> www.bezahlinstanz.de </HostName>
 *   <PortNumber> 1234 </PortNumber>
 *   <JAPPublicKey>
 *       ... the public key (see XMLJapPublicKey)
 *   </JAPPublicKey>
 * </BI>
 *
 * @author Bastian Voigt
 * @version 1.0
 */
public class BI implements IXMLEncodable
{
	private String m_biName;
	private String m_hostName;
	private int m_portNumber;
	private IMyPublicKey m_publicKey;
	private JAPSignature m_veryfire;

	public BI(String biName, String hostName, int portNumber, IMyPublicKey publicKey) throws Exception
	{
		m_publicKey = publicKey;
		m_veryfire = new JAPSignature();
		m_veryfire.initVerify(publicKey);
		m_biName = biName;
		m_hostName = hostName;
		m_portNumber = portNumber;
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
		LogHolder.log(LogLevel.DEBUG, LogType.PAY, "BI HelloWorld biName="+biName+", Host="+hostName+", Port="+portNumber);

		/** @todo does this work? i don't believe it... */
		m_biName = biName;//cert.getSubject().CN.toString();
		m_hostName = hostName;
		m_portNumber = portNumber;

		m_veryfire = new JAPSignature();
		try
		{
			m_veryfire.initVerify(cert.getPublicKey());
		}
		catch (InvalidKeyException ex)
		{
			m_veryfire = null;
		}
	}

	private void setValues(Element elemRoot) throws Exception
	{
		if (elemRoot.getTagName().equals("BI"))
		{
			throw new Exception("BI wrong XML format");
		}
		Element elem = (Element) XMLUtil.getFirstChildByName(elemRoot, "BIName");
		m_biName = XMLUtil.parseNodeString(elem, null);

		elem = (Element) XMLUtil.getFirstChildByName(elemRoot, "HostName");
		m_hostName = XMLUtil.parseNodeString(elem, null);

		elem = (Element) XMLUtil.getFirstChildByName(elemRoot, "PortNumber");
		m_portNumber = XMLUtil.parseNodeInt(elem, 0);

		elem = (Element) XMLUtil.getFirstChildByName(elemRoot, XMLJapPublicKey.getXMLElementName());
		if (elem != null)
		{
			XMLJapPublicKey keyParser = new XMLJapPublicKey(elem);
			m_publicKey = keyParser.getPublicKey();
		}
	}

	/** returns the BI's unique name (identifier) */
	public String getName()
	{
		return m_biName;
	}

	/** returns a JAPSignature object for veriying this BI's signatures */
	public JAPSignature getVerifier()
	{
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
		Element elemRoot = a_doc.createElement("BI");
		elemRoot.setAttribute("version", "1.0");

		Element elem = a_doc.createElement("BIName");
		elemRoot.appendChild(elem);
		XMLUtil.setNodeValue(elem, m_biName);

		elem = a_doc.createElement("HostName");
		elemRoot.appendChild(elem);
		XMLUtil.setNodeValue(elem, m_hostName);

		elem = a_doc.createElement("PortNumber");
		elemRoot.appendChild(elem);
		XMLUtil.setNodeValue(elem, Integer.toString(m_portNumber));

		try
		{
			XMLJapPublicKey keyFormatter = new XMLJapPublicKey(m_publicKey);
			elemRoot.appendChild(keyFormatter.toXmlElement(a_doc));
		}
		catch (Exception ex)
		{
		}

		return elemRoot;
	}
}
