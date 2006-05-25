package anon.pay;

import java.util.Enumeration;
import java.util.Vector;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import anon.crypto.JAPCertificate;
import anon.infoservice.ListenerInterface;
import anon.util.IXMLEncodable;
import anon.util.XMLUtil;

/**
 * This class represents a known BI with its unique name and x509 public
 * certificate (verifyer).
 * It can be converted to an XML structure, the structure is as follows:
 *
 * <BI version="1.0">
 *   <BIName> unique name </BIName>
 *   <ListenerInterfaces> ... </ListenerInterfaces>
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
	private Vector m_listenerInterfaces;

	private JAPCertificate m_cert;

	public static final String XML_ELEMENT_NAME = "BI";

	public BI(String a_biID, String a_biName, Vector a_listeners, JAPCertificate a_cert) throws
		Exception
	{
		ListenerInterface l = (ListenerInterface) a_listeners.firstElement();
		m_biID = a_biID;
		m_cert = a_cert;
		m_biName = a_biName;
		m_listenerInterfaces = a_listeners;
	}

	public BI(Element elemRoot) throws Exception
	{
		setValues(elemRoot);
	}

	/** constructs a BI object from a binary X509 certificate
	 * and some additional data
	 */
	public BI(byte[] barCert, String biName, Vector a_listeners)
	{
		JAPCertificate cert = JAPCertificate.getInstance(barCert);
		m_cert = cert;
		/** @todo does this work? i don't believe it... */
		m_biName = biName; //cert.getSubject().CN.toString();
		m_listenerInterfaces = a_listeners;

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
		m_listenerInterfaces = new Vector();
		String rootName = elemRoot.getTagName();
		if (!rootName.equals(XML_ELEMENT_NAME))
		{
			throw new Exception("BI wrong XML format");
		}
		Element elem = (Element) XMLUtil.getFirstChildByName(elemRoot, "BIID");
		m_biID = XMLUtil.parseValue(elem, null);

		elem = (Element) XMLUtil.getFirstChildByName(elemRoot, "BIName");
		m_biName = XMLUtil.parseValue(elem, null);

		elem = (Element) XMLUtil.getFirstChildByName(elemRoot, "ListenerInterfaces");
		NodeList listeners = elem.getChildNodes();
		for (int i = 0; i < listeners.getLength(); i++)
		{
			ListenerInterface l = new ListenerInterface( (Element) listeners.item(i));
			m_listenerInterfaces.addElement(l);
		}

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

		Element lElem = a_doc.createElement("ListenerInterfaces");
		elemRoot.appendChild(lElem);

		for (int i = 0; i < m_listenerInterfaces.size(); i++)
		{
			lElem.appendChild( ( (ListenerInterface) m_listenerInterfaces.elementAt(i)).toXmlElement(a_doc));
		}

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

	public Enumeration getListenerInterfaces()
	{
		return m_listenerInterfaces.elements();
	}
}
