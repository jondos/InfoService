package payxml;

import org.w3c.dom.*;

import org.xml.sax.*;


/**
 * This class contains the functionality for creating and parsing XML balance
 * certificates. It provides access to the public key {@link pubKey}, the
 * account number {@link accountNumber} and the timestamp {@link validTime}.
 * <strong>Note: This class does not perform any signing or signature
 * checking!</strong> This is done in {@link XMLSignature}, so if you want to
 * generate a signed XML certificate, utilize this class to sign it! The XML
 * balance certificates have the following format:
 * <pre>
 *   &lt;?xml version="1.0"?&gt;
 *   &lt;Balance verion="1.0"&gt;
 *      &lt;AccountNumber&gt;123456789012&lt;/AccountNumber&gt;
 *      &lt;CreditMax&gt;1234&lt;/CreditMax&gt;  (an integer, number of kbytes)
 *      &lt;Credit&gt;123&lt;/Credit&gt;         (an integer, number of kbytes)
 *      &lt;Timestamp&gt;yyyy-mm-dd hh:mm:ss.fffffffff&lt;/Timestamp&gt;
 *      &lt;Validtime&gt;yyyy-mm-dd hh:mm:ss.fffffffff&lt;/Validtime&gt;
 *   &lt;/Balance&gt;
 * </pre>
 */
public class XMLBalance extends XMLDocument
{
	//~ Public Fields  ******************************************************
	public static final String docStartTag = "<Balance version=\"1.0\">";
	public static final String docEndTag = "</Balance>";



	//~ Instance fields ********************************************************

	private java.sql.Timestamp timestamp;
	private java.sql.Timestamp validTime;
	private int credit;
	private long creditMax;
	private long accountNumber;

	//~ Constructors ***********************************************************

	public XMLBalance(int balance, long maxbalance,
		java.sql.Timestamp timestamp, java.sql.Timestamp validTime,
		long accountNumber
	) throws Exception
	{
		this.credit = balance;
		this.creditMax = maxbalance;
		this.timestamp = timestamp;
		this.validTime = validTime;
		this.accountNumber = accountNumber;

		xmlDocument = docStartTag+"\n" + "  <AccountNumber>" +
			accountNumber + "</AccountNumber>\n" + "  <CreditMax>" +
			maxbalance + "</CreditMax>\n" + "  <Credit>" + balance +
			"</Credit>\n" + "  <Timestamp>" + timestamp + "</Timestamp>\n" +
			"  <Validtime>" + validTime + "</Validtime>\n" + docEndTag;

		setDocument(xmlDocument);
	}


	public XMLBalance(String xml) throws Exception
	{
		setDocument(xml);

		Element element = domDocument.getDocumentElement();
		if (!element.getTagName().equals("Balance")) {
			throw new Exception();
		}

		NodeList nl = element.getElementsByTagName("AccountNumber");
		if (nl.getLength() < 1) {
			throw new Exception();
		}

		Element elementChild = (Element) nl.item(0);
		CharacterData chdata = (CharacterData) elementChild.getFirstChild();
		accountNumber = Long.parseLong(chdata.getData());

		nl = element.getElementsByTagName("CreditMax");
		if (nl.getLength() < 1) {
			throw new Exception();
		}
		elementChild = (Element) nl.item(0);
		chdata = (CharacterData) elementChild.getFirstChild();
		creditMax = Long.parseLong(chdata.getData());

		nl = element.getElementsByTagName("Credit");
		if (nl.getLength() < 1) {
			throw new Exception();
		}
		elementChild = (Element) nl.item(0);
		chdata = (CharacterData) elementChild.getFirstChild();
		credit = Integer.parseInt(chdata.getData());

		nl = element.getElementsByTagName("Timestamp");
		if (nl.getLength() < 1) {
			throw new Exception();
		}
		elementChild = (Element) nl.item(0);
		chdata = (CharacterData) elementChild.getFirstChild();
		timestamp = java.sql.Timestamp.valueOf(chdata.getData());

		nl = element.getElementsByTagName("Validtime");
		if (nl.getLength() < 1) {
			throw new Exception();
		}
		elementChild = (Element) nl.item(0);
		chdata = (CharacterData) elementChild.getFirstChild();
		validTime = java.sql.Timestamp.valueOf(chdata.getData());
	}

	//~ Methods ****************************************************************

	public long getAccountNumber()
	{
		return accountNumber;
	}


	public int getCredit()
	{
		return credit;
	}


	public long getCreditMax()
	{
		return creditMax;
	}


	public java.sql.Timestamp getTimestamp()
	{
		return timestamp;
	}


	public java.sql.Timestamp getValidTime()
	{
		return validTime;
	}
}
