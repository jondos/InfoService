package payxml;

import org.w3c.dom.*;

import org.xml.sax.*;


public class XMLTransCert extends XMLDocument
{
	//~ Public Fields  ******************************************************
	public static final String docStartTag = "<TransferCertificate version=\"1.0\">";
	public static final String docEndTag = "</TransferCertificate>";

	//~ Instance fields ********************************************************

	private String signature;
	private java.sql.Timestamp m_validTime;
	private long m_accountNumber;
	private long m_transferNumber;
		private long m_maxBalance;

	//~ Constructors ***********************************************************

	public XMLTransCert(long accountNumber, long transferNumber,
											long maxBalance, java.sql.Timestamp validTime)
		throws Exception
	{
		m_accountNumber = accountNumber;
		m_transferNumber = transferNumber;
		m_maxBalance = maxBalance;
		m_validTime = validTime;

		xmlDocument = docStartTag +
			"  <AccountNumber>" + accountNumber + "</AccountNumber>\n" +
			"  <TransferNumber>" + transferNumber + "</TransferNumber>\n" +
			"  <MaxBalance>" + maxBalance + "</MaxBalance>\n" +
			"  <ValidTime>" + validTime + "</ValidTime>\n" +
			docEndTag+"\n";
	}


	public XMLTransCert(String xml) throws Exception
	{
		setDocument(xml);
		setAccountNumber();
		setTransferNumber();
		setValidTime();
		setSignature();
	}


	public XMLTransCert(byte[] xml) throws Exception
	{
		setDocument(xml);
		setAccountNumber();
		setTransferNumber();
		setValidTime();
		setSignature();
	}

	//~ Methods ****************************************************************

	public long getAccountNumber()
	{
		return m_accountNumber;
	}


	public long getTransferNumber()
	{
		return m_transferNumber;
	}


	public java.sql.Timestamp getValidTime()
	{
		return m_validTime;
	}


	private void setAccountNumber() throws Exception
	{
		Element element = domDocument.getDocumentElement();
		if (!element.getTagName().equals("TransferCertificate")) {
			throw new Exception();
		}

		NodeList nl = element.getElementsByTagName("AccountNumber");
		if (nl.getLength() < 1) {
			throw new Exception();
		}
		element = (Element) nl.item(0);

		CharacterData chdata = (CharacterData) element.getFirstChild();
		m_accountNumber = Long.parseLong(chdata.getData());
	}


	private void setSignature()
	{
		signature = xmlDocument.substring(xmlDocument.indexOf("<Signature"),
				xmlDocument.indexOf("</Signature>") + 12
			);
	}


	private void setTransferNumber() throws Exception
	{
		Element element = domDocument.getDocumentElement();
		if (!element.getTagName().equals("TransferCertificate")) {
			throw new Exception();
		}

		NodeList nl = element.getElementsByTagName("TransferNumber");
		if (nl.getLength() < 1) {
			throw new Exception();
		}
		element = (Element) nl.item(0);

		CharacterData chdata = (CharacterData) element.getFirstChild();
		m_transferNumber = Long.parseLong(chdata.getData());
	}


	private void setValidTime() throws Exception
	{
		Element element = domDocument.getDocumentElement();
		if (!element.getTagName().equals("TransferCertificate")) {
			throw new Exception();
		}

		NodeList nl = element.getElementsByTagName("ValidTime");
		if (nl.getLength() < 1) {
			throw new Exception();
		}
		element = (Element) nl.item(0);

		CharacterData chdata = (CharacterData) element.getFirstChild();
		m_validTime = java.sql.Timestamp.valueOf(chdata.getData());
	}
}
