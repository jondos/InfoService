package payxml;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import anon.crypto.JAPSignature;
import anon.util.IXMLEncodeable;
import anon.util.XMLUtil;

/**
 * This class holds a balance certificate. Can be converted to and from
 * XML
 *
 * @todo find a better internal representation for the signature
 */
public class XMLBalance implements IXMLEncodeable
{
	private long m_lAccountNumber;
	private java.sql.Timestamp m_Timestamp;
	private java.sql.Timestamp m_ValidTime;
	private long m_lDeposit;
	private long m_lSpent;
	private Document m_signature;

	public XMLBalance(long accountNumber,
					  long deposit, long spent,
					  java.sql.Timestamp timestamp,
					  java.sql.Timestamp validTime,
					  JAPSignature signer) throws Exception
	{
		m_lDeposit = deposit;
		m_lSpent = spent;
		m_Timestamp = timestamp;
		m_ValidTime = validTime;
		m_lAccountNumber = accountNumber;
		m_signature = null;

		if (signer != null)
		{
			Document doc = getXmlEncoded();
			signer.signXmlDoc(doc);
			Element elemSig = (Element) XMLUtil.getFirstChildByName(doc.getDocumentElement(), "Signature");
			m_signature = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
			Element elem = (Element) XMLUtil.importNode(m_signature, elemSig, true);
			m_signature.appendChild(elem);
		}
	}

	public XMLBalance(Document doc) throws Exception
	{
		setValues(doc.getDocumentElement());
	}

	public XMLBalance(String xmlDoc) throws Exception
	{
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xmlDoc);
		setValues(doc.getDocumentElement());
	}

	public XMLBalance(Element elemBalance) throws Exception
	{
		setValues(elemBalance);
	}

	private void setValues(Element elemRoot) throws Exception
	{
		if (!elemRoot.getTagName().equals("Balance") ||
			!elemRoot.getAttribute("version").equals("1.0"))
		{
			throw new Exception("Balance wrong XML format");
		}

		Element elem = (Element) XMLUtil.getFirstChildByName(elemRoot, "AccountNumber");
		String str = XMLUtil.parseNodeString(elem, null);
		m_lAccountNumber = Long.parseLong(str);

		elem = (Element) XMLUtil.getFirstChildByName(elemRoot, "Deposit");
		str = XMLUtil.parseNodeString(elem, null);
		m_lDeposit = Long.parseLong(str);

		elem = (Element) XMLUtil.getFirstChildByName(elemRoot, "Spent");
		str = XMLUtil.parseNodeString(elem, null);
		m_lSpent = Long.parseLong(str);

		elem = (Element) XMLUtil.getFirstChildByName(elemRoot, "Timestamp");
		str = XMLUtil.parseNodeString(elem, null);
		m_Timestamp = java.sql.Timestamp.valueOf(str);

		elem = (Element) XMLUtil.getFirstChildByName(elemRoot, "Validtime");
		str = XMLUtil.parseNodeString(elem, null);
		m_ValidTime = java.sql.Timestamp.valueOf(str);

		elem = (Element) XMLUtil.getFirstChildByName(elemRoot, "Signature");
		if (elem != null)
		{
			m_signature = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
			m_signature.appendChild(XMLUtil.importNode(m_signature, elem, true));
		}
	}

	public Document getXmlEncoded()
	{
		Document doc = null;
		try
		{
			doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		}
		catch (ParserConfigurationException ex)
		{
			return null;
		}
		Element elemRoot = doc.createElement("Balance");
		elemRoot.setAttribute("version", "1.0");
		doc.appendChild(elemRoot);

		Element elem = doc.createElement("AccountNumber");
		XMLUtil.setNodeValue(elem, Long.toString(m_lAccountNumber));
		elemRoot.appendChild(elem);
		elem = doc.createElement("Deposit");
		XMLUtil.setNodeValue(elem, Long.toString(m_lDeposit));
		elemRoot.appendChild(elem);
		elem = doc.createElement("Spent");
		XMLUtil.setNodeValue(elem, Long.toString(m_lSpent));
		elemRoot.appendChild(elem);
		elem = doc.createElement("Timestamp");
		XMLUtil.setNodeValue(elem, m_Timestamp.toString());
		elemRoot.appendChild(elem);
		elem = doc.createElement("Validtime");
		XMLUtil.setNodeValue(elem, m_ValidTime.toString());
		elemRoot.appendChild(elem);
		if (m_signature != null)
		{
			Element elemSig = null;
			try
			{
				elemSig = (Element) XMLUtil.importNode(doc, m_signature.getDocumentElement(), true);
				elemRoot.appendChild(elemSig);
			}
			catch (Exception ex1)
			{
			}
		}
		return doc;
	}

	public long getAccountNumber()
	{
		return m_lAccountNumber;
	}

	public long getDeposit()
	{
		return m_lDeposit;
	}

	public long getSpent()
	{
		return m_lSpent;
	}

	public long getCredit()
	{
		return m_lDeposit - m_lSpent;
	}

	public java.sql.Timestamp getTimestamp()
	{
		return m_Timestamp;
	}

	public java.sql.Timestamp getValidTime()
	{
		return m_ValidTime;
	}
}
