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

import java.io.ByteArrayInputStream;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import anon.crypto.JAPSignature;
import anon.util.IXMLEncodable;
import anon.util.XMLUtil;


/**
 * XML structure for a easy cost confirmation (without mircopayment function) which is sent to the AI by the Jap
 * <CC version "1.0">
 *   <AiID> ... </AiID>
 * 	 <AccountNumber> ... </AccountNumber>
 * 	 <TransferredBytes>... </TransferredBytes>
 * 	 <Signature>
 *    ... Signature des Kontoinhabers
 *  </Signature>
 * </CC>
 * @author Grischan Gl&auml;nzel, Bastian Voigt
 */
public class XMLEasyCC implements IXMLEncodable //extends XMLDocument
{
	//~ Instance fields ********************************************************

	private String m_strAiName;
	private long m_lTransferredBytes;
	private long m_lAccountNumber;
	private Document m_signature;

	//~ Constructors ***********************************************************
	public XMLEasyCC(String aiName, long accountNumber, long transferred, JAPSignature signer) throws Exception
	{
		m_strAiName = aiName;
		m_lTransferredBytes = transferred;
		m_lAccountNumber = accountNumber;
		m_signature = null;

		if (signer != null)
		{
			Document doc = XMLUtil.toXMLDocument(this);
			signer.signXmlDoc(doc);
			Element elemSig = (Element) XMLUtil.getFirstChildByName(doc.getDocumentElement(), "Signature");
			m_signature = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
			Element elem = (Element) XMLUtil.importNode(m_signature, elemSig, true);
			m_signature.appendChild(elem);
		}
	}

	public XMLEasyCC(byte[] data) throws Exception
	{
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new ByteArrayInputStream(data));
		setValues(doc.getDocumentElement());
	}

	public XMLEasyCC(Element xml) throws Exception
	{
		setValues(xml);
	}

	private void setValues(Element element) throws Exception
	{
		if (!element.getTagName().equals("CC") ||
			!element.getAttribute("version").equals("1.0"))
		{
			throw new Exception("XMLEasyCC wrong xml structure or wrong version");
		}

		Element elem = (Element) XMLUtil.getFirstChildByName(element, "AiID");
		m_strAiName = XMLUtil.parseNodeString(elem, "");
		elem = (Element) XMLUtil.getFirstChildByName(element, "AccountNumber");
		m_lAccountNumber = XMLUtil.parseNodeLong(elem, 0);
		elem = (Element) XMLUtil.getFirstChildByName(element, "TransferredBytes");
		m_lTransferredBytes = XMLUtil.parseNodeLong(elem, 0);
	}

	public Element toXmlElement(Document a_doc)
	{
		Element elemRoot = a_doc.createElement("CC");
		elemRoot.setAttribute("version", "1.0");

		Element elem=a_doc.createElement("AiID");
		XMLUtil.setNodeValue(elem,m_strAiName);
		elemRoot.appendChild(elem);

		elem=a_doc.createElement("TransferredBytes");
		XMLUtil.setNodeValue(elem,Long.toString(m_lTransferredBytes));
		elemRoot.appendChild(elem);

		elem=a_doc.createElement("AccountNumber");
		XMLUtil.setNodeValue(elem,Long.toString(m_lAccountNumber));
		elemRoot.appendChild(elem);
		return elemRoot;
	}

	//~ Methods ****************************************************************

	public String getAIName()
	{
		return m_strAiName;
	}

	public long getAccountNumber()
	{
		return m_lAccountNumber;
	}

	public long getTransferredBytes()
	{
		return m_lTransferredBytes;
	}

	/** this makes the signature invalid! */
	public void addTransferredBytes(long plusBytes)
	{
		m_lTransferredBytes += plusBytes;
		m_signature = null;
	}

}
