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
import java.security.SignatureException;
import java.util.Hashtable;
import java.util.Locale;

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import anon.util.Base64;
import anon.util.XMLParseException;
import anon.util.XMLUtil;
import anon.crypto.SignatureVerifier;

/**
 * Used to send messages to JAP.
 * @author Rolf Wendolsky
 */
public class MessageDBEntry extends AbstractDistributableDatabaseEntry implements IDistributable
{
	public static final String XML_ELEMENT_CONTAINER_NAME = "Messages";
	public static final String XML_ELEMENT_NAME = "Message";

	public static final String HTTP_REQUEST_STRING = "/messages";
	public static final String HTTP_SERIALS_REQUEST_STRING = "/messageserials";

	public static final String PROPERTY_NAME = "messageFileName";

	public static final String POST_FILE = "/message";

	private static final String XML_TEXT = "MessageText";
	private static final String XML_URL = "MessageURL";
	private static final String XML_ATTR_LANG = "lang";

	private static final long TIMEOUT = 7 * 24 * 60 * 60 * 1000L; // one week

	private int m_externalIdentifier;
	private long m_serial;
	private long m_lastUpdate;
	private boolean m_bIsDummy;
	private String m_id;
	private Element m_xmlDescription;

	private Hashtable m_hashText = new Hashtable();
	private Hashtable m_hashUrl = new Hashtable();

	public MessageDBEntry(Element a_xmlElement) throws XMLParseException, SignatureException
	{
		super(System.currentTimeMillis() +  TIMEOUT);
		XMLUtil.assertNodeName(a_xmlElement, XML_ELEMENT_NAME);
		if (SignatureVerifier.getInstance().getVerifiedXml(
			  a_xmlElement, SignatureVerifier.DOCUMENT_CLASS_INFOSERVICE) == null)
		{
			throw new SignatureException();
		}

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
		if (m_hashText.size() == 0 || m_hashText.get("en") == null)
		{
			// if there is not text (or no english text), this in interpreted as dummy message
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
						m_hashUrl.put(lang, new URL(content));
					}
					catch (MalformedURLException ex1)
					{
						// invalid url
						continue;
					}
				}
			}
		}

		m_lastUpdate = XMLUtil.parseValue(XMLUtil.getFirstChildByName(a_xmlElement, XML_LAST_UPDATE), -1L);
		if (m_lastUpdate == -1)
		{
			m_lastUpdate = System.currentTimeMillis();
			//throw (new Exception("JAPMinVersion: Constructor: No LastUpdate node found."));
		}

		m_xmlDescription = a_xmlElement;
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

	public int getExternalIdentifier()
	{
		return m_externalIdentifier;
	}

	public void setExternalIdentifier(int a_identifier)
	{
		m_externalIdentifier = a_identifier;
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
		return POST_FILE;
	}

	public long getLastUpdate()
	{
		return m_lastUpdate;
	}

	public Element getXmlStructure()
	{
		return m_xmlDescription;
	}
}
