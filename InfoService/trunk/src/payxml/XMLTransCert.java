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

public class XMLTransCert extends XMLDocument
{
	//~ Instance fields ********************************************************

	//private String signature;
	private java.sql.Timestamp m_validTime;
	private long m_accountNumber;
	private long m_transferNumber;
	private long m_maxBalance;

	//~ Constructors ***********************************************************

	public XMLTransCert(long accountNumber, long transferNumber,
						long maxBalance, java.sql.Timestamp validTime) throws Exception
	{
		m_accountNumber = accountNumber;
		m_transferNumber = transferNumber;
		m_maxBalance = maxBalance;
		m_validTime = validTime;
		m_theDocument = getDocumentBuilder().newDocument();
		Element elemRoot = m_theDocument.createElement("TransferCertificate");
		elemRoot.setAttribute("version", "1.0");
		m_theDocument.appendChild(elemRoot);
		Element elem = m_theDocument.createElement("AccountNumber");
		XMLUtil.setNodeValue(elem, Long.toString(accountNumber));
		elemRoot.appendChild(elem);
		elem = m_theDocument.createElement("TransferNumber");
		XMLUtil.setNodeValue(elem, Long.toString(transferNumber));
		elemRoot.appendChild(elem);
		elem = m_theDocument.createElement("MaxBalance");
		XMLUtil.setNodeValue(elem, Long.toString(maxBalance));
		elemRoot.appendChild(elem);
		elem = m_theDocument.createElement("ValidTime");
		XMLUtil.setNodeValue(elem, validTime.toString());
		elemRoot.appendChild(elem);
	}

	public XMLTransCert(String xml) throws Exception
	{
		setDocument(xml);
		setValues();
	}

	/**
	 * Creates an TransCert from  an existing XML docuemnt
	 *
	 * @param xml the node that represents the TransCert
	 */
	public XMLTransCert(Node xml) throws Exception
	{
		m_theDocument=getDocumentBuilder().newDocument();
		Node n=XMLUtil.importNode(m_theDocument,xml,true);
		m_theDocument.appendChild(n);
		setValues();
	}

	private void setValues() throws Exception
	{
		setAccountNumber();
		setTransferNumber();
		setValidTime();
		//setSignature();
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
		Element element = m_theDocument.getDocumentElement();
		if (!element.getTagName().equals("TransferCertificate"))
		{
			throw new Exception();
		}
		element = (Element) XMLUtil.getFirstChildByName(element, "AccountNumber");
		String str = XMLUtil.parseNodeString(element, null);
		m_accountNumber = Long.parseLong(str);
	}

	/*private void setSignature()
	  {
	 signature = xmlDocument.substring(xmlDocument.indexOf("<Signature"),
		 xmlDocument.indexOf("</Signature>") + 12
		 );
	  }*/

	private void setTransferNumber() throws Exception
	{
		Element element = m_theDocument.getDocumentElement();
		if (!element.getTagName().equals("TransferCertificate"))
		{
			throw new Exception();
		}

		element = (Element) XMLUtil.getFirstChildByName(element, "TransferNumber");
		String str = XMLUtil.parseNodeString(element, null);
		m_transferNumber = Long.parseLong(str);
	}

	private void setValidTime() throws Exception
	{
		Element element = m_theDocument.getDocumentElement();
		if (!element.getTagName().equals("TransferCertificate"))
		{
			throw new Exception();
		}
		element = (Element) XMLUtil.getFirstChildByName(element, "ValidTime");
		String str = XMLUtil.parseNodeString(element, null);
		m_validTime = java.sql.Timestamp.valueOf(str);
	}
}
