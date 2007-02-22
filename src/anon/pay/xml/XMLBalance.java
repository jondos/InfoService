package anon.pay.xml;

import java.io.ByteArrayInputStream;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import anon.util.XMLUtil;
import anon.util.IXMLEncodable;
import anon.crypto.IMyPrivateKey;
import anon.crypto.XMLSignature;

/**
 * This class holds a balance certificate. Can be converted to and from
 * XML
 *
 * @todo find a better internal representation for the signature
 */
public class XMLBalance implements IXMLEncodable
{
	private long m_lAccountNumber;
	private java.sql.Timestamp m_Timestamp;
	private java.sql.Timestamp m_ValidTime;
	private long m_lDeposit;
	private long m_lSpent;
	private Document m_docTheBalance = null;

	public XMLBalance(long accountNumber,
					  long deposit, long spent,
					  java.sql.Timestamp timestamp,
					  java.sql.Timestamp validTime,
					  IMyPrivateKey signer) throws Exception
	{
		m_lDeposit = deposit;
		m_lSpent = spent;
		m_Timestamp = timestamp;
		m_ValidTime = validTime;
		m_lAccountNumber = accountNumber;
		m_docTheBalance = XMLUtil.createDocument();
		m_docTheBalance.appendChild(internal_toXmlElement(m_docTheBalance));
		if (signer != null)
		{
			XMLSignature.sign(m_docTheBalance, signer);
		}
	}

	public XMLBalance(Document doc) throws Exception
	{
		setValues(doc.getDocumentElement());
		m_docTheBalance = doc;
	}

	public XMLBalance(String xmlDoc) throws Exception
	{
		Document doc = XMLUtil.toXMLDocument(xmlDoc);
		setValues(doc.getDocumentElement());
		m_docTheBalance = doc;
	}

	public XMLBalance(Element elemBalance) throws Exception
	{
		setValues(elemBalance);
		m_docTheBalance = XMLUtil.createDocument();
		m_docTheBalance.appendChild(XMLUtil.importNode(m_docTheBalance, elemBalance, true));
	}

	private void setValues(Element elemRoot) throws Exception
	{
		if (!elemRoot.getTagName().equals("Balance") ||
			!elemRoot.getAttribute("version").equals("1.0"))
		{
			throw new Exception("Balance wrong XML format");
		}

		Element elem = (Element) XMLUtil.getFirstChildByName(elemRoot, "AccountNumber");
		String str = XMLUtil.parseValue(elem, null);
		m_lAccountNumber = Long.parseLong(str);

		elem = (Element) XMLUtil.getFirstChildByName(elemRoot, "Deposit");
		str = XMLUtil.parseValue(elem, null);
		m_lDeposit = Long.parseLong(str);

		elem = (Element) XMLUtil.getFirstChildByName(elemRoot, "Spent");
		str = XMLUtil.parseValue(elem, null);
		m_lSpent = Long.parseLong(str);

		elem = (Element) XMLUtil.getFirstChildByName(elemRoot, "Timestamp");
		str = XMLUtil.parseValue(elem, null);
		m_Timestamp = java.sql.Timestamp.valueOf(str);

		elem = (Element) XMLUtil.getFirstChildByName(elemRoot, "Validtime");
		str = XMLUtil.parseValue(elem, null);
		m_ValidTime = java.sql.Timestamp.valueOf(str);
	}

	private Element internal_toXmlElement(Document a_doc)
	{
		Element elemRoot = a_doc.createElement("Balance");
		elemRoot.setAttribute("version", "1.0");

		Element elem = a_doc.createElement("AccountNumber");
		XMLUtil.setValue(elem, Long.toString(m_lAccountNumber));
		elemRoot.appendChild(elem);
		elem = a_doc.createElement("Deposit");
		XMLUtil.setValue(elem, Long.toString(m_lDeposit));
		elemRoot.appendChild(elem);
		elem = a_doc.createElement("Spent");
		XMLUtil.setValue(elem, Long.toString(m_lSpent));
		elemRoot.appendChild(elem);
		elem = a_doc.createElement("Timestamp");
		XMLUtil.setValue(elem, m_Timestamp.toString());
		elemRoot.appendChild(elem);
		elem = a_doc.createElement("Validtime");
		XMLUtil.setValue(elem, m_ValidTime.toString());
		elemRoot.appendChild(elem);
		return elemRoot;
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

	public Element toXmlElement(Document a_doc)
	{
		try
		{
			return (Element) XMLUtil.importNode(a_doc, m_docTheBalance.getDocumentElement(), true);
		}
		catch (Exception e)
		{
			return null;
		}
	}

}
