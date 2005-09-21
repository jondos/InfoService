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

import java.io.ByteArrayInputStream;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import anon.crypto.JAPSignature;
import anon.util.XMLUtil;
import anon.util.IXMLEncodable;
import anon.crypto.IMyPrivateKey;
import anon.crypto.XMLSignature;
import anon.crypto.IMyPublicKey;

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
public class XMLEasyCC implements IXMLEncodable
{
	//~ Instance fields ********************************************************

	private String m_strAiName;
	private long m_lTransferredBytes;
	private long m_lAccountNumber;
	private static final String ms_strElemName = "CC";
	private Document m_docTheEasyCC;
	//~ Constructors ***********************************************************

	public static String getXMLElementName()
	{
		return ms_strElemName;
	}

	public XMLEasyCC(String aiName, long accountNumber, long transferred, JAPSignature signer) throws
		Exception
	{
		m_strAiName = aiName;
		m_lTransferredBytes = transferred;
		m_lAccountNumber = accountNumber;
		m_docTheEasyCC = XMLUtil.createDocument();
		m_docTheEasyCC.appendChild(internal_toXmlElement(m_docTheEasyCC));

		if (signer != null)
		{
			signer.signXmlDoc(m_docTheEasyCC);
		}
	}

	public XMLEasyCC(byte[] data) throws Exception
	{
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new
			ByteArrayInputStream(data));
		setValues(doc.getDocumentElement());
		m_docTheEasyCC = doc;
	}

	public XMLEasyCC(String xml) throws Exception
	{
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new
			ByteArrayInputStream(xml.getBytes()));
		setValues(doc.getDocumentElement());
		m_docTheEasyCC = doc;
	}

	public XMLEasyCC(Element xml) throws Exception
	{
		setValues(xml);
		m_docTheEasyCC = XMLUtil.createDocument();
		m_docTheEasyCC.appendChild(XMLUtil.importNode(m_docTheEasyCC, xml, true));
	}

	private void setValues(Element element) throws Exception
	{
		if (!element.getTagName().equals(ms_strElemName) ||
			!element.getAttribute("version").equals("1.0"))
		{
			throw new Exception("XMLEasyCC wrong xml structure or wrong version");
		}

		Element elem = (Element) XMLUtil.getFirstChildByName(element, "AiID");
		if (elem == null)
		{
			m_strAiName = "keinplan1";
		}
		else
		{
			m_strAiName = XMLUtil.parseValue(elem, "keinplan2");
		}
		elem = (Element) XMLUtil.getFirstChildByName(element, "AccountNumber");
		m_lAccountNumber = XMLUtil.parseValue(elem, 0l);
		elem = (Element) XMLUtil.getFirstChildByName(element, "TransferredBytes");
		m_lTransferredBytes = XMLUtil.parseValue(elem, 0l);

	}

	private Element internal_toXmlElement(Document a_doc)
	{
		Element elemRoot = a_doc.createElement(ms_strElemName);
		elemRoot.setAttribute("version", "1.0");
		Element elem = a_doc.createElement("AiID");
		XMLUtil.setValue(elem, m_strAiName);
		elemRoot.appendChild(elem);

		elem = a_doc.createElement("TransferredBytes");
		XMLUtil.setValue(elem, Long.toString(m_lTransferredBytes));
		elemRoot.appendChild(elem);

		elem = a_doc.createElement("AccountNumber");
		XMLUtil.setValue(elem, Long.toString(m_lAccountNumber));
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
	public synchronized void addTransferredBytes(long plusBytes)
	{
		m_lTransferredBytes += plusBytes;
		m_docTheEasyCC = XMLUtil.createDocument();
		m_docTheEasyCC.appendChild(internal_toXmlElement(m_docTheEasyCC));
	}

	/**
	 * setTransferredBytes. this makes the signature invalid!
	 *
	 * @param numBytes long
	 */
	public synchronized void setTransferredBytes(long numBytes)
	{
		m_lTransferredBytes = numBytes;
		m_docTheEasyCC = XMLUtil.createDocument();
		m_docTheEasyCC.appendChild(internal_toXmlElement(m_docTheEasyCC));
	}

	public boolean sign(IMyPrivateKey key)
	{
		try
		{
			XMLSignature.sign(m_docTheEasyCC, key);
			return true;
		}
		catch (Exception e)
		{
			return false;
		}
	}

	public boolean verify(IMyPublicKey key)
	{
		try
		{
			return XMLSignature.verifyFast(m_docTheEasyCC, key);
		}
		catch (Throwable t)
		{
			return false;
		}
	}

	public synchronized Element toXmlElement(Document a_doc)
	{
		try
		{
			return (Element) XMLUtil.importNode(a_doc, m_docTheEasyCC.getDocumentElement(), true);
		}
		catch (Exception e)
		{
			return null;
		}
	}
}
