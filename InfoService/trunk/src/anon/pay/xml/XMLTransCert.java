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
package anon.pay.xml;

import org.w3c.dom.*;
import anon.util.*;
import javax.xml.parsers.DocumentBuilderFactory;
import anon.crypto.JAPSignature;

public class XMLTransCert implements IXMLSignable
{
	//~ Instance fields ********************************************************

	//private String signature;
	private java.sql.Timestamp m_validTime;
	private long m_accountNumber;
	private long m_transferNumber;
	private long m_deposit;
	private Document m_signature;

	//~ Constructors ***********************************************************

	public XMLTransCert(long accountNumber, long transferNumber,
						long deposit, java.sql.Timestamp validTime) throws Exception
	{
		m_accountNumber = accountNumber;
		m_transferNumber = transferNumber;
		m_deposit = deposit;
		m_validTime = validTime;
		m_signature = null;
	}



	public XMLTransCert(String xml) throws Exception
	{
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xml);
		Element elemRoot = doc.getDocumentElement();
		setValues(elemRoot);
	}

	/**
	 * Creates an TransCert from  an existing XML docuemnt
	 *
	 * @param xml the node that represents the TransCert
	 */
	public XMLTransCert(Element xml) throws Exception
	{
		setValues(xml);
	}
	public XMLTransCert(Document xml) throws Exception
	{
		Element elemRoot = xml.getDocumentElement();
		setValues(elemRoot);
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


	private void setValues(Element elemRoot) throws Exception
	{
		if (!elemRoot.getTagName().equals("TransferCertificate"))
		{
			throw new Exception("XMLTransCert wrong xml structure");
		}

		Element element = (Element) XMLUtil.getFirstChildByName(elemRoot, "AccountNumber");
		String str = XMLUtil.parseNodeString(element, null);
		m_accountNumber = Long.parseLong(str);

		element = (Element) XMLUtil.getFirstChildByName(elemRoot, "TransferNumber");
		str = XMLUtil.parseNodeString(element, null);
		m_transferNumber = Long.parseLong(str);


		element = (Element) XMLUtil.getFirstChildByName(elemRoot, "ValidTime");
		str = XMLUtil.parseNodeString(element, null);
		m_validTime = java.sql.Timestamp.valueOf(str);

		element = (Element) XMLUtil.getFirstChildByName(elemRoot, "Signature");
		if (element != null)
		{
			m_signature = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
			m_signature.appendChild(XMLUtil.importNode(m_signature, element, true));
		}

	}


	/**
	 * getBaseUrl
	 *
	 * @return String
	 * @todo make this more dynamic
	 */
	public String getBaseUrl()
	{
		return "http://anon.inf.tu-dresden.de/pay/index.php";
	}

	/**
	 * toXmlElement
	 *
	 * @param a_doc Document
	 * @return Element
	 */
	public Element toXmlElement(Document a_doc)
	{
//		a_doc = getDocumentBuilder().newDocument();
		Element elemRoot = a_doc.createElement("TransferCertificate");
		elemRoot.setAttribute("version", "1.0");
		a_doc.appendChild(elemRoot);
		Element elem = a_doc.createElement("AccountNumber");
		XMLUtil.setNodeValue(elem, Long.toString(m_accountNumber));
		elemRoot.appendChild(elem);
		elem = a_doc.createElement("TransferNumber");
		XMLUtil.setNodeValue(elem, Long.toString(m_transferNumber));
		elemRoot.appendChild(elem);
		elem = a_doc.createElement("Deposit");
		XMLUtil.setNodeValue(elem, Long.toString(m_deposit));
		elemRoot.appendChild(elem);
		elem = a_doc.createElement("ValidTime");
		XMLUtil.setNodeValue(elem, m_validTime.toString());
		elemRoot.appendChild(elem);
		if (m_signature != null)
		{
			Element elemSig = null;
			try
			{
				elemSig = (Element) XMLUtil.importNode(a_doc, m_signature.getDocumentElement(), true);
				elemRoot.appendChild(elemSig);
			}
			catch (Exception ex1)
			{
			}
		}

		return elemRoot;
	}

	public void sign(JAPSignature signer) throws Exception
	{
		Document doc = XMLUtil.toXMLDocument(this);
		signer.signXmlDoc(doc);
		Element elemSig = (Element) XMLUtil.getFirstChildByName(doc.getDocumentElement(), "Signature");
		m_signature = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		Element elem = (Element) XMLUtil.importNode(m_signature, elemSig, true);
		m_signature.appendChild(elem);
	}

	public boolean verifySignature(JAPSignature verifier)
	{
		try{
		Document doc = XMLUtil.toXMLDocument(this);
		return verifier.verifyXML(doc.getDocumentElement());
		}
		catch(Exception e)
		{
			return false;
		}
	}

	/**
	 * isSigned
	 *
	 * @return boolean
	 */
	public boolean isSigned()
	{
		return (m_signature!=null);
	}

}
