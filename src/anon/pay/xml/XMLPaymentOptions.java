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
import java.util.Vector;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import anon.util.IXMLEncodable;

/**
 * This class represents a XMLPaymentOptions structure.
 * @author Tobias Bayer
 */
public class XMLPaymentOptions implements IXMLEncodable
{
	private Vector m_currencies = new Vector();
	private Vector m_paymentOptions = new Vector();

	public XMLPaymentOptions(String xml) throws Exception
	{
		ByteArrayInputStream in = new ByteArrayInputStream(xml.getBytes());
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);
		setValues(doc.getDocumentElement());
	}

	public XMLPaymentOptions()
	{
	}

	public XMLPaymentOptions(Element xml) throws Exception
	{
		setValues(xml);
	}

	public Element toXmlElement(Document a_doc)
	{
		Element elemRoot = a_doc.createElement("PaymentOptions");
		elemRoot.setAttribute("version", "1.0");

		Element elem = null;

		for (int i = 0; i < m_currencies.size(); i++)
		{
			elem = a_doc.createElement("Currency");
			elem.appendChild(a_doc.createTextNode( (String) m_currencies.elementAt(i)));
			elemRoot.appendChild(elem);

		}

		for (int i = 0; i < m_paymentOptions.size(); i++)
		{
			elem = ( (XMLPaymentOption) m_paymentOptions.elementAt(i)).toXmlElement(a_doc);
			elemRoot.appendChild(elem);
		}

		return elemRoot;
	}

	private void setValues(Element elemRoot) throws Exception
	{
		if (!elemRoot.getTagName().equals("PaymentOptions"))
		{
			throw new Exception("XMLPaymentOptions wrong XML structure");
		}

		NodeList currencies = elemRoot.getElementsByTagName("Currency");
		for (int i = 0; i < currencies.getLength(); i++)
		{
			m_currencies.addElement(currencies.item(i).getNodeValue());
		}

		NodeList options = elemRoot.getElementsByTagName("PaymentOption");
		for (int i = 0; i < options.getLength(); i++)
		{
			m_paymentOptions.addElement( (Element) options.item(i));
		}
	}

	public XMLPaymentOptions(Document document) throws Exception
	{
		setValues(document.getDocumentElement());
	}

	public void addOption(XMLPaymentOption a_option)
	{
		m_paymentOptions.addElement(a_option);
	}

	public void addCurrency(String a_currency)
	{
		m_currencies.addElement(a_currency);
	}
}
