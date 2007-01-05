/*
 Copyright (c) 2000 - 2006, The JAP-Team
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
package anon.infoservice;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Hashtable;
import java.util.Locale;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import anon.util.Base64;
import anon.util.XMLParseException;
import anon.util.XMLUtil;

/**
 * Used to send messages to JAP.
 * @author Rolf Wendolsky
 */
public class MessageDBEntry extends AbstractDistributableDatabaseEntry
{
	public static final String XML_ELEMENT_CONTAINER_NAME = "Messages";
	public static final String XML_ELEMENT_NAME = "Message";

	public static final String HTTP_REQUEST_STRING = "/messages";
	public static final String HTTP_SERIALS_REQUEST_STRING = "/messageserials";

	private static final String XML_TEXT = "Text";
	private static final String XML_URL = "URL";
	private static final String XML_ATTR_LANG = "lang";

	private static final long TIMEOUT = 3600 * 1000L; // one hour

	private long m_serial;
	private long m_creationTimeStamp;
	private boolean m_bIsDummy;
	private String m_id;
	private Element m_xmlDescription;

	private Hashtable m_hashText = new Hashtable();
	private Hashtable m_hashUrl = new Hashtable();

	public MessageDBEntry(Element a_xmlElement) throws XMLParseException
	{
		super(System.currentTimeMillis() +  TIMEOUT);
		XMLUtil.assertNodeName(a_xmlElement, XML_ELEMENT_NAME);
		m_serial = XMLUtil.parseAttribute(a_xmlElement, XML_ATTR_SERIAL, Long.MIN_VALUE);
		m_id = XMLUtil.parseAttribute(a_xmlElement, XML_ATTR_ID, null);
		if (m_id == null)
		{
			throw new XMLParseException("No id given!");
		}
		NodeList textNodes = a_xmlElement.getElementsByTagName(XML_TEXT);
		String content, lang;

		for (int i = 0; i < textNodes.getLength(); i++)
		{
			content = XMLUtil.parseValue(textNodes.item(i), null);
			lang = XMLUtil.parseAttribute(textNodes.item(i), XML_ATTR_LANG, "en");
			if (content != null)
			{
				content = Base64.decodeToString(content);
				m_hashText.put(lang, content);
			}
		}
		if (m_hashText.size() == 0)
		{
			// if there is not text, this in interpreted as dummy message
			m_bIsDummy = true;
		}
		else
		{
			m_bIsDummy = false;

			textNodes = a_xmlElement.getElementsByTagName(XML_URL);
			for (int i = 0; i < textNodes.getLength(); i++)
			{
				content = XMLUtil.parseValue(textNodes.item(i), null);
				lang = XMLUtil.parseAttribute(textNodes.item(i), XML_ATTR_LANG, "en");
				if (content != null)
				{
					try
					{
						m_hashText.put(lang, new URL(content));
					}
					catch (MalformedURLException ex1)
					{
						// invalid url
						continue;
					}
				}
			}
		}

		m_creationTimeStamp = System.currentTimeMillis();
		m_xmlDescription = a_xmlElement;
	}

	public MessageDBEntry()
	{
		super(System.currentTimeMillis() + TIMEOUT);
		m_bIsDummy = false;
		m_serial = System.currentTimeMillis();
		m_creationTimeStamp = System.currentTimeMillis();
		m_id = "0";
		Document doc = XMLUtil.createDocument();
		Element elemTemp;

		m_xmlDescription = doc.createElement(XML_ELEMENT_NAME);
		elemTemp = doc.createElement(XML_TEXT);
		XMLUtil.setValue(elemTemp, Base64.encode("Das ist mein Text!! Halalü!".getBytes(), true));
		m_xmlDescription.appendChild(elemTemp);
		elemTemp = doc.createElement(XML_URL);
		XMLUtil.setAttribute(elemTemp, XML_ATTR_LANG, "de");
		XMLUtil.setValue(elemTemp, "http://anon.inf.tu-dresden.de/kosten.html");
		m_xmlDescription.appendChild(elemTemp);
		elemTemp = doc.createElement(XML_URL);
		XMLUtil.setAttribute(elemTemp, XML_ATTR_LANG, "en");
		XMLUtil.setValue(elemTemp, "http://anon.inf.tu-dresden.de/kosten_en.html");
		m_xmlDescription.appendChild(elemTemp);


		XMLUtil.setAttribute(m_xmlDescription, XML_ATTR_ID, m_id);
		XMLUtil.setAttribute(m_xmlDescription, XML_ATTR_LAST_UPDATE, m_creationTimeStamp);
		XMLUtil.setAttribute(m_xmlDescription, XML_ATTR_SERIAL, m_serial);
	}

	public URL getURL(Locale a_locale)
	{
		if (a_locale == null)
		{
			return null;
		}

		URL url = (URL)m_hashUrl.get(a_locale.getLanguage());

		if (url == null)
		{
			url = (URL)m_hashText.get("en");
		}

		return url;
	}

	public String getText(Locale a_locale)
	{
		if (a_locale == null)
		{
			return null;
		}
		String text = (String)m_hashText.get(a_locale.getLanguage());
		if (text == null)
		{
			text = (String)m_hashText.get("en");
		}
		return text;
	}

	public boolean isDummy()
	{
		return m_bIsDummy;
	}

	public long getVersionNumber()
	{
		return m_serial;
	}

	public String getId()
	{
		return m_id;
	}

	public String getPostFile()
	{
		return "/message";
	}

	public long getLastUpdate()
	{
		return m_creationTimeStamp;
	}

	public Element getXmlStructure()
	{
		return m_xmlDescription;
	}
}
