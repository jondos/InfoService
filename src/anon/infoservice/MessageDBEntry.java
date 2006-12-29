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

import java.net.URL;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
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

	private static final String XML_HEADLINE = "Head";
	private static final String XML_TEXT = "Text";
	private static final String XML_URL = "URL";

	private static final long TIMEOUT = 3600 * 1000L; // one hour

	private long m_serial;
	private long m_creationTimeStamp;
	private boolean m_bIsDummy;
	private String m_id;
	private Element m_xmlDescription;

	private String m_text;
	private String m_headline;
	private URL m_url;

	public MessageDBEntry(Element a_xmlElement) throws XMLParseException
	{
		super(TIMEOUT);
		XMLUtil.assertNodeName(a_xmlElement, XML_ELEMENT_NAME);
		m_serial = XMLUtil.parseAttribute(a_xmlElement, XML_ATTR_SERIAL, Long.MIN_VALUE);
		m_id = XMLUtil.parseAttribute(a_xmlElement, XML_ATTR_ID, null);
		if (m_id == null)
		{
			throw new XMLParseException("No id given!");
		}
		m_text = XMLUtil.parseValue(XMLUtil.getFirstChildByName(a_xmlElement, XML_TEXT), null);
		m_headline = XMLUtil.parseValue(XMLUtil.getFirstChildByName(a_xmlElement, XML_HEADLINE), null);
		if (m_text == null || m_headline == null)
		{
			m_bIsDummy = true;
		}
		else
		{
			m_bIsDummy = false;
		}

		try
		{
			m_url = new URL(XMLUtil.parseValue(XMLUtil.getFirstChildByName(a_xmlElement, XML_URL), null));
		}
		catch (Exception ex)
		{
		}

		m_creationTimeStamp = System.currentTimeMillis();
		m_xmlDescription = a_xmlElement;

	}

	public MessageDBEntry()
	{
		super(TIMEOUT);
		m_bIsDummy = false;
		m_serial = System.currentTimeMillis();
		m_creationTimeStamp = System.currentTimeMillis();
		m_id = "0";
		Document doc = XMLUtil.createDocument();
		Element elemTemp;

		m_xmlDescription = doc.createElement(XML_ELEMENT_NAME);
		elemTemp = doc.createElement(XML_HEADLINE);
		XMLUtil.setValue(elemTemp, Base64.encode("Eine Überschrift...".getBytes(), true));
		m_xmlDescription.appendChild(elemTemp);
		elemTemp = doc.createElement(XML_TEXT);
		XMLUtil.setValue(elemTemp, Base64.encode("Das ist mein Text!!".getBytes(), true));
		m_xmlDescription.appendChild(elemTemp);
		elemTemp = doc.createElement(XML_URL);
		XMLUtil.setValue(elemTemp, "http://www.anon-online.de");
		m_xmlDescription.appendChild(elemTemp);

		XMLUtil.setAttribute(m_xmlDescription, XML_ATTR_ID, m_id);
		XMLUtil.setAttribute(m_xmlDescription, XML_ATTR_LAST_UPDATE, m_creationTimeStamp);
		XMLUtil.setAttribute(m_xmlDescription, XML_ATTR_SERIAL, m_serial);
	}

	public URL getURL()
	{
		return m_url;
	}

	public String getText()
	{
		return m_text;
	}

	public String getHead()
	{
		return m_headline;
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
