/*
 Copyright (c) 2000, The JAP-Team
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
package payxml;

import org.w3c.dom.Element;
import org.w3c.dom.Node;
import anon.util.XMLUtil;

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
	//~ Instance fields ********************************************************

	private java.sql.Timestamp m_Timestamp;
	private java.sql.Timestamp m_ValidTime;
	private int m_iCredit;
	private long m_lCreditMax;
	private long m_AccountNumber;

	//~ Constructors ***********************************************************

	public XMLBalance(int balance, long maxbalance,
					  java.sql.Timestamp timestamp, java.sql.Timestamp validTime,
					  long accountNumber
					  ) throws Exception
	{
		m_iCredit = balance;
		m_lCreditMax = maxbalance;
		m_Timestamp = timestamp;
		m_ValidTime = validTime;
		m_AccountNumber = accountNumber;

		m_theDocument = getDocumentBuilder().newDocument();
		Element elemRoot = m_theDocument.createElement("Balance");
		m_theDocument.appendChild(elemRoot);
		elemRoot.setAttribute("version", "1.0");
		Element elem = m_theDocument.createElement("AccountNumber");
		XMLUtil.setNodeValue(elem, Long.toString(accountNumber));
		elemRoot.appendChild(elem);
		elem = m_theDocument.createElement("CreditMax");
		XMLUtil.setNodeValue(elem, Long.toString(maxbalance));
		elemRoot.appendChild(elem);
		elem = m_theDocument.createElement("Credit");
		XMLUtil.setNodeValue(elem, Integer.toString(balance));
		elemRoot.appendChild(elem);
		elem = m_theDocument.createElement("Timestamp");
		XMLUtil.setNodeValue(elem, timestamp.toString());
		elemRoot.appendChild(elem);
		elem = m_theDocument.createElement("Validtime");
		XMLUtil.setNodeValue(elem, validTime.toString());
		elemRoot.appendChild(elem);
	}

	public XMLBalance(String xml) throws Exception
	{
		setDocument(xml);
		setValues();
	}

	/**
	 * Creates a Balance from  an existing XML docuemnt
	 *
	 * @param xml the node that represents the Balance
	 */
	public XMLBalance(Node xml) throws Exception
	{
		m_theDocument=getDocumentBuilder().newDocument();
		Node n=XMLUtil.importNode(m_theDocument,xml,true);
		m_theDocument.appendChild(n);
		setValues();
	}

	private void setValues() throws Exception
	{
		Element elemRoot = m_theDocument.getDocumentElement();
		if (!elemRoot.getTagName().equals("Balance"))
		{
			throw new Exception();
		}

		Element elem = (Element) XMLUtil.getFirstChildByName(elemRoot, "AccountNumber");
		String str = XMLUtil.parseNodeString(elem, null);
		m_AccountNumber = Long.parseLong(str);

		elem = (Element) XMLUtil.getFirstChildByName(elemRoot, "CreditMax");
		str = XMLUtil.parseNodeString(elem, null);
		m_lCreditMax = Long.parseLong(str);

		elem = (Element) XMLUtil.getFirstChildByName(elemRoot, "Credit");
		str = XMLUtil.parseNodeString(elem, null);
		m_iCredit = Integer.parseInt(str);

		elem = (Element) XMLUtil.getFirstChildByName(elemRoot, "Timestamp");
		str = XMLUtil.parseNodeString(elem, null);
		m_Timestamp = java.sql.Timestamp.valueOf(str);

		elem = (Element) XMLUtil.getFirstChildByName(elemRoot, "Validtime");
		str = XMLUtil.parseNodeString(elem, null);
		m_ValidTime = java.sql.Timestamp.valueOf(str);
	}

	//~ Methods ****************************************************************

	public long getAccountNumber()
	{
		return m_AccountNumber;
	}

	public int getCredit()
	{
		return m_iCredit;
	}

	public long getCreditMax()
	{
		return m_lCreditMax;
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
