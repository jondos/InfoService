package anon.pay.xml;

import java.io.ByteArrayInputStream;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import anon.util.XMLUtil;
import anon.util.IXMLEncodable;
import anon.crypto.IMyPrivateKey;
import anon.crypto.XMLSignature;
import anon.util.XMLParseException;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

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
	private java.sql.Timestamp m_flatEnddate;
	private long m_volumeBytesleft;
	private int m_balance;

	private Document m_docTheBalance = null;

	public XMLBalance(long accountNumber,
					  long deposit, long spent,
					  java.sql.Timestamp timestamp,
					  java.sql.Timestamp validTime,
					  int balance,
					  long volumeBytesleft,
					  java.sql.Timestamp flatEnddate,
					  IMyPrivateKey signKey)
	{
		m_lDeposit = deposit;
		m_lSpent = spent;
		m_Timestamp = timestamp;
		m_ValidTime = validTime;
		m_lAccountNumber = accountNumber;
		m_balance = balance;
		m_volumeBytesleft = volumeBytesleft;
		m_flatEnddate = flatEnddate;
		m_docTheBalance = XMLUtil.createDocument();
		m_docTheBalance.appendChild(internal_toXmlElement(m_docTheBalance));
		if (signKey != null) //might very well be null, when created by Database (which doesnt have access to the private key)
		{
			sign(signKey);
		}
	}

	public void sign(IMyPrivateKey signKey) //is public so we can create it first, and call sign later
	{
		try
		{
			XMLSignature.sign(m_docTheBalance, signKey);
		} catch (XMLParseException e)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.PAY, "Could not sign XMLBalance");
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

		elem = (Element) XMLUtil.getFirstChildByName(elemRoot, "BalanceInCent");
		str = XMLUtil.parseValue(elem, "0");
		m_balance = Math.max(0, Integer.parseInt(str));

		elem = (Element) XMLUtil.getFirstChildByName(elemRoot, "FlatrateEnddate");
		str = XMLUtil.parseValue(elem, "3000-01-01 00:00:00.00000000");
		m_flatEnddate = java.sql.Timestamp.valueOf(str);

		elem = (Element) XMLUtil.getFirstChildByName(elemRoot, "VolumeBytesLeft");
		str = XMLUtil.parseValue(elem, null);
		// @todo  downward compatibility; remove after some weeks
		if (str == null)
		{
			m_volumeBytesleft = (m_lDeposit - m_lSpent) / 1000;
		}
		else
		{
			m_volumeBytesleft = Long.parseLong(str);
		}
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
		XMLUtil.setValue(elem, Long.toString(m_lAccountNumber)); //Elmar: why cast to String, XMLUtil supports primitives?
		elemRoot.appendChild(elem);
		elem = a_doc.createElement("Deposit");
		XMLUtil.setValue(elem, Long.toString(m_lDeposit));
		elemRoot.appendChild(elem);
		elem = a_doc.createElement("Spent");
		XMLUtil.setValue(elem, Long.toString(m_lSpent));
		elemRoot.appendChild(elem);
		elem = a_doc.createElement("BalanceInCent");
		XMLUtil.setValue(elem,m_balance);
		elemRoot.appendChild(elem);
		elem = a_doc.createElement("FlatrateEnddate");
		XMLUtil.setValue(elem, m_flatEnddate.toString() );
		elemRoot.appendChild(elem);
		elem = a_doc.createElement("VolumeBytesLeft");
		XMLUtil.setValue(elem, m_volumeBytesleft);
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

	/**
	 * getCredit: returns the current credit of the user
	 * Implementation depends on the payment system used
	 * formerly returned the difference between cumulative spent and deposit bytes
	 * now returns volume_bytesleft
	 * return value will be compared to jap.pay.PaymentMainPanel WARNING_AMOUNT
	 *
	 * @return long: currently volume_bytesleft
	 */
	public long getCredit()
	{
		return m_volumeBytesleft;
	}

	public int getBalance()
	{
		return m_balance;
	}

	public long getVolumeBytesLeft()
	{
		return m_volumeBytesleft;
	}

	public java.sql.Timestamp getFlatEnddate()
	{
		return m_flatEnddate;
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
