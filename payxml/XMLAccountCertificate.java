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

import java.math.BigInteger;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import anon.crypto.MyRSAPublicKey;
import anon.util.Base64;
import anon.util.XMLUtil;
import anon.crypto.IMyPublicKey;
import org.w3c.dom.Document;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.*;
import org.w3c.dom.*;

/**
 * This class contains the functionality for creating and parsing XML account
 * certificates. It provides access to the public key {@link pubKey}, the
 * account number {@link accountNumber} and the timestamp {@link validTime}.
 * <strong>Note: This class does not perform any signing or signature
 * checking!</strong> This is done in {@link XMLSignature}, so if you want to
 * generate a signed XML certificate, utilize this class to sign it! The XML
 * account certificates have the following format:
 * <pre>
 *    &lt;?xml version="1.0"?&gt;
 *    &lt;AccountCertificate version="1.0"&gt;
 *      &lt;AccountNumber&gt;123456789012&lt;/AccountNumber&gt;
 *      &lt;BiID&gt;The BI ID&lt;BiID&gt;
 *      &lt;JapPublicKey&gt;
 *        ... public key xml data (see below)
 *      &lt;/JapPublicKey&gt;
 *      &lt;CreationTime&gt;YYYY-MM-DD&lt;/CreationTime&gt;
 *      &lt;Signature&gt;
 *        ... signature xml data (see below)
 *      &lt;/Signature&gt;
 *    &lt;/AccountCertificate&gt;
 * </pre>
 *
 * <ul>
 * <li>
 * The public key is stored in w3c xmlsig format (DSAKeyValue or RSAKeyValue)
 * inside the JapPublicKey tags
 * </li>
 * <li>
 * Ths signature is stored in {@link XMLSignature} format inside the Signature
 * tags
 * </li>
 * </ul>
 */
public class XMLAccountCertificate //extends XMLDocument
{

	//~ Instance fields ********************************************************

	private IMyPublicKey m_publicKey;
	private java.sql.Timestamp m_creationTime;
	private long m_accountNumber;
	private String m_biID;

	//~ Constructors ***********************************************************

	/**
	 * creates a new XML certificate (NOTE: It will NOT be signed!)
	 *
	 * @param publicKey public key
	 * @param accountNumber account number
	 * @param creationTime creation timestamp
	 * @param biID the signing BI's ID
	 */

	public XMLAccountCertificate(IMyPublicKey publicKey, long accountNumber,
								 java.sql.Timestamp creationTime, String biID
								 ) throws Exception
	{
		m_publicKey = publicKey;
		m_accountNumber = accountNumber;
		m_creationTime = creationTime;
		m_biID = biID;
	}

	/*		m_theDocument = getDocumentBuilder().newDocument();
	  Element elemRoot = m_theDocument.createElement("AccountCertificate");
	  elemRoot.setAttribute("version", "1.0");
	  m_theDocument.appendChild(elemRoot);
	  Element elemAccountNumber = m_theDocument.createElement("AccountNumber");
	  XMLUtil.setNodeValue(elemAccountNumber, Long.toString(accountNumber));
	  elemRoot.appendChild(elemAccountNumber);
	  Node elemJapKey = m_theDocument.createElement("JapPublicKey");
	  elemRoot.appendChild(elemJapKey);
	  Node elemKey = XMLUtil.importNode(m_theDocument, publicKey.getXmlEncoded().getDocumentElement(), true);
	  elemJapKey.appendChild(elemKey);
	  Element elemTmp = m_theDocument.createElement("CreationTime");
	  XMLUtil.setNodeValue(elemTmp, creationTime.toString());
	  elemRoot.appendChild(elemTmp);
	  elemTmp = m_theDocument.createElement("BiID");
	  XMLUtil.setNodeValue(elemTmp, biID);
	  elemRoot.appendChild(elemTmp);
	 }*/

	/**
	 * parses an existing XML certificate
	 *
	 * @param xml the certificate as string
	 */
	public XMLAccountCertificate(String xml) throws Exception
	{
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xml);
		setValues( (Node) doc.getDocumentElement());
	}

	/**
	 * Creates an AccountCertifcate from  an existing XML docuemnt
	 *
	 * @param xml the node that represents the AccountCertifcate
	 */
	public XMLAccountCertificate(Node xml) throws Exception
	{
		setValues(xml);
	}

	private void setValues(Node xml) throws Exception
	{
		// parse accountnumber
		Element elem = (Element) XMLUtil.getFirstChildByName(xml, "AccountNumber");
		m_accountNumber = XMLUtil.parseNodeInt(elem, 0);

		// parse biID
		elem = (Element) XMLUtil.getFirstChildByName(xml, "BiID");
		m_biID = XMLUtil.parseNodeString(elem, "UNKNOWN");

		// parse creation time
		elem = (Element) XMLUtil.getFirstChildByName(xml, "CreationTime");
		String timestamp = XMLUtil.parseNodeString(elem, "0");
		m_creationTime = java.sql.Timestamp.valueOf(timestamp);

		// parse publickey
		elem = (Element) XMLUtil.getFirstChildByName(xml, "JapPublicKey");
		if (elem != null)
		{
			m_publicKey = new XMLJapPublicKey(elem).getPublicKey();
		}
	}

	public Document getXmlDocument()
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
		Element elemRoot = doc.createElement("AccountCertificate");
		elemRoot.setAttribute("version", "1.0");

		Element elem = doc.createElement("AccountNumber");
		XMLUtil.setNodeValue(elem, Long.toString(m_accountNumber));
		elemRoot.appendChild(elem);

		elem = doc.createElement("BiID");
		XMLUtil.setNodeValue(elem, m_biID);
		elemRoot.appendChild(elem);

		elem = doc.createElement("CreationTime");
		XMLUtil.setNodeValue(elem, m_creationTime.toString());
		elemRoot.appendChild(elem);

		elem = doc.createElement("JapPublicKey");
		elemRoot.appendChild(elem);
		elem.setAttribute("version", "1.0");
		Document tmpDoc = m_publicKey.getXmlEncoded();
		try
		{
			elem.appendChild(XMLUtil.importNode(doc, tmpDoc.getDocumentElement(), true));
		}
		catch (Exception ex1)
		{
			return null;
		}
		return doc;
	}

/** @todo one of these two can be removed :-) */
	public String getXmlString()
	{
		return XMLUtil.XMLDocumentToString(getXmlDocument());
	}

	public String getXMLString()
	{
		return XMLUtil.XMLDocumentToString(getXmlDocument());
	}

	//~ Methods ****************************************************************

	public long getAccountNumber()
	{
		return m_accountNumber;
	}

	public java.sql.Timestamp getCreationTime()
	{
		return m_creationTime;
	}

	public IMyPublicKey getPublicKey()
	{
		return m_publicKey;
	}

	private void setAccountNumber(long accountNumber) throws Exception
	{
		m_accountNumber = accountNumber;
	}

	private void setCreationTime(java.sql.Timestamp creationTime) throws Exception
	{
		m_creationTime = creationTime;
	}

	private void setID(String biID) throws Exception
	{
		m_biID = biID;
	}

	private void setPublicKey(IMyPublicKey publicKey) throws Exception
	{
		m_publicKey = publicKey;
	}
}
