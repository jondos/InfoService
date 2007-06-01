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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import anon.util.IXMLEncodable;
import anon.util.XMLUtil;
import java.util.Enumeration;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import org.w3c.dom.Node;

/**
 * This class represents a XMLPaymentOptions structure.
 * @author Elmar Schraml
 */
public class XMLPaymentOptions implements IXMLEncodable
{
	private Vector m_currencies = new Vector();
	private Vector m_paymentOptions = new Vector(); //holds XMLPaymentOption objects
	private String m_acceptedCreditCards;

	public XMLPaymentOptions(String xml) throws Exception
	{
		Document doc = XMLUtil.toXMLDocument(xml);
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

		Element elem;

		for (int i = 0; i < m_currencies.size(); i++)
		{
			elem = a_doc.createElement("Currency");
			elem.appendChild(a_doc.createTextNode( (String) m_currencies.elementAt(i)));
			elemRoot.appendChild(elem);

		}

		for (int i = 0; i < m_paymentOptions.size(); i++)
		{
			try
			{
				XMLPaymentOption anOption = (XMLPaymentOption) m_paymentOptions.elementAt(i);
				elem = anOption.toXmlElement(a_doc);
				elemRoot.appendChild(elem);
			}
			catch (ClassCastException e)
			{
				//System.out.println(e.getMessage()); //just to check, message is normally null
			}
		}

		elem = a_doc.createElement("AcceptedCards");
		elem.appendChild(a_doc.createTextNode(m_acceptedCreditCards));
		elemRoot.appendChild(elem);

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
			m_currencies.addElement(currencies.item(i).getFirstChild().getNodeValue());
		}

		NodeList options = elemRoot.getElementsByTagName("PaymentOption");
		XMLPaymentOption anOption;
		for (int i = 0; i < options.getLength(); i++)
		{
			anOption = new XMLPaymentOption((Element) options.item(i));
			m_paymentOptions.addElement(anOption);
		}

		Node node = XMLUtil.getFirstChildByName(elemRoot, "AcceptedCards");
		m_acceptedCreditCards = XMLUtil.parseValue(node, "");
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

	public Enumeration getOptionHeadings(String a_language)
	{
		Vector optionHeadings = new Vector();

		for (int i = 0; i < m_paymentOptions.size(); i++)
		{
			try
			{
				XMLPaymentOption option = (XMLPaymentOption) m_paymentOptions.elementAt(i);
				optionHeadings.addElement(option.getHeading(a_language));
			}
			catch (Exception e)
			{
				LogHolder.log(LogLevel.EXCEPTION, LogType.PAY,
							  "Could not get payment option heading: " + e.getMessage());
			}
		}
		return optionHeadings.elements();
	}


	public Vector getAllOptions(){
		   return m_paymentOptions;
	}

	/**
	 * Gets a XMLPaymentOption object for the provided heading
	 * @param a_heading String
	 * @param a_language String
	 * @return XMLPaymentOption
	 */
	public XMLPaymentOption getOption(String a_heading, String a_language)
	{
		for (int i = 0; i < m_paymentOptions.size(); i++)
		{
			try
			{
				XMLPaymentOption option = (XMLPaymentOption) m_paymentOptions.elementAt(i);
				String heading = option.getHeading(a_language);
				if (heading.equalsIgnoreCase(a_heading))
				{
					return option;
				}

			}
			catch (Exception e)
			{
				LogHolder.log(LogLevel.EXCEPTION, LogType.PAY,
							  "Could not get payment option for heading: " + a_heading + " in language " +
							  a_language);
			}

		}
		return null;
	}

	public Vector getCurrencies()
	{
		return (Vector) m_currencies.clone();
	}

	public void setAcceptedCreditCards(String a_acceptedCreditCards)
	{
		m_acceptedCreditCards = a_acceptedCreditCards;
	}

	public String getAcceptedCreditCards()
	{
		return m_acceptedCreditCards;
	}
}
