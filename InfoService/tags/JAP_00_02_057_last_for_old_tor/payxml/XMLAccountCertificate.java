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
 * The public key is stored in {@link XMLJapPublicKey} format inside the
 * JapPublicKey tags
 * </li>
 * <li>
 * Ths signature is stored in {@link XMLSignature} format inside the Signature
 * tags
 * </li>
 * </ul>
 */
public class XMLAccountCertificate extends XMLDocument
{

	//~ Instance fields ********************************************************

	private MyRSAPublicKey m_publicKey;
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

	public XMLAccountCertificate(MyRSAPublicKey publicKey, long accountNumber,
								 java.sql.Timestamp creationTime, String biID
								 ) throws Exception
	{
		m_publicKey = publicKey;
		m_accountNumber = accountNumber;
		m_creationTime = creationTime;
		m_biID = biID;

		XMLJapPublicKey xmlkey = new XMLJapPublicKey(publicKey);
		m_theDocument = getDocumentBuilder().newDocument();
		Element elemRoot = m_theDocument.createElement("AccountCertificate");
		elemRoot.setAttribute("version", "1.0");
		m_theDocument.appendChild(elemRoot);
		Element elemAccountNumber = m_theDocument.createElement("AccountNumber");
		XMLUtil.setNodeValue(elemAccountNumber, Long.toString(accountNumber));
		elemRoot.appendChild(elemAccountNumber);
		Node elemKey = XMLUtil.importNode(m_theDocument, xmlkey.getDomDocument().getDocumentElement(), true);
		elemRoot.appendChild(elemKey);
		Element elemTmp = m_theDocument.createElement("CreationTime");
		XMLUtil.setNodeValue(elemTmp, creationTime.toString());
		elemRoot.appendChild(elemTmp);
		elemTmp = m_theDocument.createElement("BiID");
		XMLUtil.setNodeValue(elemTmp, biID);
		elemRoot.appendChild(elemTmp);
	}

	/**
	 * parses an existing XML certificate
	 *
	 * @param xml the certificate as string
	 */
	public XMLAccountCertificate(String xml) throws Exception
	{
		setDocument(xml);
		setValues();
	}

	/**
	 * Creates an AccountCertifcate from  an existing XML docuemnt
	 *
	 * @param xml the node that represents the AccountCertifcate
	 */
	public XMLAccountCertificate(Node xml) throws Exception
	{
		m_theDocument=getDocumentBuilder().newDocument();
		Node n=XMLUtil.importNode(m_theDocument,xml,true);
		m_theDocument.appendChild(n);
		setValues();
	}

	private void setValues() throws Exception
	{
		setAccountNumber();
		setCreationTime();
		setID();
		setPublicKey();
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

	public MyRSAPublicKey getPublicKey()
	{
		return m_publicKey;
	}

	private void setAccountNumber() throws Exception
	{
		Element element = m_theDocument.getDocumentElement();
		if (!element.getTagName().equals("AccountCertificate"))
		{
			throw new Exception();
		}
		element=(Element)XMLUtil.getFirstChildByName(element,"AccountNumber");
		String str=XMLUtil.parseNodeString(element,null);
		m_accountNumber = Long.parseLong(str);
	}

	/**
	 * Parses the xml data and sets our private member variable {@link
	 * creationTime}
	 */
	private void setCreationTime() throws Exception
	{
		Element element = m_theDocument.getDocumentElement();
		if (!element.getTagName().equals("AccountCertificate"))
		{
			throw new Exception();
		}
		element=(Element)XMLUtil.getFirstChildByName(element,"CreationTime");
		String str=XMLUtil.parseNodeString(element,null);
		m_creationTime = java.sql.Timestamp.valueOf(str);
	}

	/**
	 * Parses the xml data and sets our private member variables {@link
	 * m_biID}
	 */
	private void setID() throws Exception
	{
		CharacterData chdata;
		Element elemRoot = m_theDocument.getDocumentElement();
		if (!elemRoot.getTagName().equals("AccountCertificate"))
		{
			throw new Exception();
		}
		// set hostname
		Element elem=(Element)XMLUtil.getFirstChildByName(elemRoot,"BiID");
		m_biID = XMLUtil.parseNodeString(elem,null);
	}

	/**
	 * Parses the modulus and the exponent from the xml document, creates a
	 * {@link PublicKey} object from it and stores it in our member variable
	 * {@link pubKey}.
	 */
	private void setPublicKey() throws Exception
	{
		Element element = m_theDocument.getDocumentElement();
		if (!element.getTagName().equals("AccountCertificate"))
		{
			throw new Exception();
		}
		element=(Element)XMLUtil.getFirstChildByName(element,"JapPublicKey");
		XMLJapPublicKey tmpKey=new XMLJapPublicKey(element);
		m_publicKey = tmpKey.getRSAPublicKey();
	}
}
