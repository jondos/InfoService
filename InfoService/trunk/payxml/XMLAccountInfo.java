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
import anon.crypto.JAPSignature;
import org.w3c.dom.DocumentFragment;

/**
 * This class represents an XML AccountInfo structure.
 *
 * The following XML structure is used:
 * <pre>
 * &lt;AccountInfo&gt;
 *    &lt;Balance&gt;
 *       &lt;AccountNumber&gt; ..&lt;/AccountNumber&gt;
 *       &lt;Deposit&gt;...&lt;/Deposit&gt;
 *       &lt;Spent&gt;....&lt;/Spent&gt;
 *       &lt;Validtime&gt;...&lt;/Validtime&gt;
 *       &lt;Timestamp&gt;...&lt;/Timestamp&gt;
 *       &lt;Signature&gt; //Unterschrift der BI
 *          ...
 *       &lt;/Signature&gt;
 *    &lt;/Balance&gt;
 *    &lt;CostConfirmations&gt; //Kostenbest\u00E4tigungen, die
 *		                           von den einzelnen AI's
 *		                           abgerechnet wurden
 *       &lt;CC&gt;...&lt;/CC&gt;
 *       &lt;CC&gt;...&lt;/CC&gt;
 *    &lt;/CostConfirmations&gt;
 * &lt;/AccountInfo&gt;
 * </pre>
 *
 * @author Bastian Voigt
 */
public class XMLAccountInfo extends XMLDocument
{
	//~ Instance fields ********************************************************

	private java.sql.Timestamp m_Timestamp;
	private java.sql.Timestamp m_ValidTime;
	private long m_lDeposit;
	private long m_lSpent;
	private long m_AccountNumber;

	//~ Constructors ***********************************************************

	/**
	 * Creates an AccountInfo structure without CCs but with signed
	 * balance certificate.
	 *
	 * @param accountNumber long
	 * @param deposit long
	 * @param spent long
	 * @param timestamp Timestamp
	 * @param validTime Timestamp
	 * @param signer JAPSignature
	 * @throws Exception
	 */
	public XMLAccountInfo(long accountNumber,
						  long deposit, long spent,
						  java.sql.Timestamp timestamp,
						  java.sql.Timestamp validTime,
						  JAPSignature signer) throws Exception
	{
		m_lDeposit = deposit;
		m_lSpent = spent;
		m_Timestamp = timestamp;
		m_ValidTime = validTime;
		m_AccountNumber = accountNumber;

		// build balance dom document
		m_theDocument = getDocumentBuilder().newDocument();
		Element elemRoot = m_theDocument.createElement("AccountInfo");
		elemRoot.setAttribute("version", "1.0");
		m_theDocument.appendChild(elemRoot);
		Element elemBalance = m_theDocument.createElement("Balance");
		elemRoot.appendChild(elemBalance);
		elemBalance.setAttribute("version", "1.0");
		Element elem = m_theDocument.createElement("AccountNumber");
		XMLUtil.setNodeValue(elem, Long.toString(accountNumber));
		elemBalance.appendChild(elem);
		elem = m_theDocument.createElement("Deposit");
		XMLUtil.setNodeValue(elem, Long.toString(deposit));
		elemBalance.appendChild(elem);
		elem = m_theDocument.createElement("Spent");
		XMLUtil.setNodeValue(elem, Long.toString(spent));
		elemBalance.appendChild(elem);
		elem = m_theDocument.createElement("Timestamp");
		XMLUtil.setNodeValue(elem, timestamp.toString());
		elemBalance.appendChild(elem);
		elem = m_theDocument.createElement("Validtime");
		XMLUtil.setNodeValue(elem, validTime.toString());
		elemBalance.appendChild(elem);

		// append signature
		signer.signXmlNode(elemBalance);

		// add empty costconfirmations field
		Element elemCCs = m_theDocument.createElement("CostConfirmations");
		elemRoot.appendChild(elemCCs);
	}

	/**
	 * Creates an AccountInfo object from a string.
	 * Checks the signature
	 *
	 * @param xml String
	 * @param verifier JAPSignature must be initialized and ready to verify XML or
	 * null if you dont want to check
	 * @throws Exception on invalid xml format or invalid signature
	 */
	public XMLAccountInfo(String xml, JAPSignature verifier) throws Exception
	{
		setDocument(xml);
		setValues();

		// maybe check signature
		if (verifier != null)
		{
			if (!checkBalanceSignature(verifier))
			{
				throw new Exception("Invalid Signature");
			}
		}
	}

	//~ Methods ****************************************************************

	/**
	 * Adds a cost confirmation xml structure to the accountinfo.
	 *
	 * @param xmlCC Node
	 */
	public void addCC(Node xmlCC) throws Exception
	{
		Element elemRoot = m_theDocument.getDocumentElement();
		Element elemCCs = (Element) XMLUtil.getFirstChildByName(elemRoot, "CostConfirmations");
		Node myCC = XMLUtil.importNode(m_theDocument, xmlCC, true);
		elemCCs.appendChild(myCC);
	}

	/**
	 * Creates a Balance from  an existing XML docuemnt
	 *
	 * @param xml the node that represents the Balance
	 * @param verifier JAPSignature must be initialized and ready to verify XML
	 * @throws Exception on invalid xml format or invalid signature
	 */
	public XMLAccountInfo(Node xml, JAPSignature verifier) throws Exception
	{
		m_theDocument = getDocumentBuilder().newDocument();
		Node n = XMLUtil.importNode(m_theDocument, xml, true);
		m_theDocument.appendChild(n);
		setValues();
		if (!checkBalanceSignature(verifier))
		{
			throw new Exception("Invalid Signature");
		}
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
	}

	/**
	 * Checks the signature of the <Balance> element
	 *
	 * @param verifier JAPSignature must be initialized and ready to verify XML
	 * @return boolean
	 */
	private boolean checkBalanceSignature(JAPSignature verifier)
	{
		// check signature
		Element e = m_theDocument.getDocumentElement();
		Element elemBalance = (Element) XMLUtil.getFirstChildByName(e, "Balance");
		return verifier.verifyXML(elemBalance);
	}

	public long getAccountNumber()
	{
		return m_AccountNumber;
	}

	public long getDeposit()
	{
		return m_lDeposit;
	}

	public long getCreditMax()
	{
		return m_lSpent;
	}

	public java.sql.Timestamp getTimestamp()
	{
		return m_Timestamp;
	}

	public java.sql.Timestamp getValidTime()
	{
		return m_ValidTime;
	}

	public long getCredit()
	{
		return m_lDeposit - m_lSpent;
	}




	/**
	 * Neu: getBalance()..
	 * extrahiert ein DocumentFragment, das nur die Balance enth\uFFFDlt
	 */
	public DocumentFragment getBalance()
	{
		DocumentFragment fragment = m_theDocument.createDocumentFragment();
		Element elem = m_theDocument.getDocumentElement();
		elem = (Element) XMLUtil.getFirstChildByName(elem, "Balance");
		fragment.appendChild(elem);
		return fragment;
}
}
