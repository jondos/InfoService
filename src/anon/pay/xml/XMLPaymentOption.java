/*
 Copyright (c) 2000-2006, The JAP-Team
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
import anon.util.XMLUtil;
import anon.util.Util;

/**
 * This class represents a payment option sent by the Payment Instance.
 * @author Tobias Bayer
 */
public class XMLPaymentOption implements IXMLEncodable
{
	public static final String OPTION_ACTIVE = "active";
	public static final String OPTION_PASSIVE = "passive";
	public static final String EXTRA_TEXT = "text";
	public static final String EXTRA_LINK = "link";
	public static final String EXTRA_PHONE = "phone";

	private static final String EXCEPTION_WRONG_XML_STRUCTURE = "XMLPaymentOption wrong XML structure";

	/** Option name */
	private String m_name;

	/** Option type (active|passive)*/
	private String m_type;

	/** Generic option? Non-generic options are handled by JAP and do not need
	 * input fields for passive payment*/
	private boolean m_generic;

	/** This vector takes String[2] arrays while the first element is the heading
	 * and the second element is the language identifier. E.g.: {"Money Transfer", "en"}
	 */
	private Vector m_headings = new Vector();

	/** Same explanation as m_headings*/
	private Vector m_detailedInfos = new Vector();

	/** This vector takes String[3] arrays. First element: Extra payment info like account number.
	 * Second element: type. Third element: Language.*/
	private Vector m_extraInfos = new Vector();

	/**
	 * This vector takes input fields. First element: reference, second element: label, third element: language
	 */
	private Vector m_inputFields = new Vector();

	/** A link to an image */
	private String m_imageLink;

	/** JAP version since when the option does work (for non-generic options) */
	private String m_minJapVersion;

	public XMLPaymentOption(String xml) throws Exception
	{
		ByteArrayInputStream in = new ByteArrayInputStream(xml.getBytes());
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(in);
		setValues(doc.getDocumentElement());
	}

	public XMLPaymentOption()
	{
	}

	public XMLPaymentOption(String a_name, String a_type, boolean a_generic)
	{
		m_name = a_name;
		m_type = a_type;
		m_generic = a_generic;
	}

	public XMLPaymentOption(String a_name, String a_type, boolean a_generic, String a_japVersion)
	{
		m_name = a_name;
		m_type = a_type;
		m_generic = a_generic;
		m_minJapVersion = a_japVersion;
	}

	public XMLPaymentOption(String a_name, String a_type)
	{
		m_name = a_name;
		m_type = a_type;
		m_generic = true;
	}

	public void addHeading(String a_heading, String a_language)
	{
		m_headings.addElement(new String[]
							  {a_heading, a_language});
	}

	public void addDetailedInfo(String a_info, String a_language)
	{
		m_detailedInfos.addElement(new String[]
								   {a_info, a_language});
	}

	public void addExtraInfo(String a_info, String a_type, String a_language)
	{
		m_extraInfos.addElement(new String[]
								{a_info, a_type, a_language});
	}

	public void addInputField(String a_reference, String a_label, String a_language)
	{
		m_inputFields.addElement(new String[]
								 {a_reference, a_label, a_language});
	}

	public void setImageLink(String a_link)
	{
		m_imageLink = a_link;
	}

	public XMLPaymentOption(Element xml) throws Exception
	{
		setValues(xml);
	}

	public XMLPaymentOption(Document document) throws Exception
	{
		setValues(document.getDocumentElement());
	}

	public Element toXmlElement(Document a_doc)
	{
		Element elemRoot = a_doc.createElement("PaymentOption");
		elemRoot.setAttribute("name", m_name);
		elemRoot.setAttribute("type", m_type);
		elemRoot.setAttribute("generic", String.valueOf(m_generic));
		elemRoot.setAttribute("japversion", m_minJapVersion);

		Element elem;

		//Add headings
		for (int i = 0; i < m_headings.size(); i++)
		{
			String[] heading = (String[]) m_headings.elementAt(i);
			elem = a_doc.createElement("Heading");
			elem.setAttribute("lang", heading[1]);
			elem.appendChild(a_doc.createTextNode(heading[0]));
			elemRoot.appendChild(elem);
		}
		//Add detailed information
		for (int i = 0; i < m_detailedInfos.size(); i++)
		{
			String[] detailed = (String[]) m_detailedInfos.elementAt(i);
			elem = a_doc.createElement("DetailedInfo");
			elem.setAttribute("lang", detailed[1]);
			elem.appendChild(a_doc.createTextNode(detailed[0]));
			elemRoot.appendChild(elem);
		}

		//Add extra information
		for (int i = 0; i < m_extraInfos.size(); i++)
		{
			String[] extra = (String[]) m_extraInfos.elementAt(i);
			elem = a_doc.createElement("ExtraInfo");
			elem.setAttribute("type", extra[1]);
			if (extra[2] != null)
			{
				elem.setAttribute("lang", extra[2]);
			}
			elem.appendChild(a_doc.createTextNode(extra[0]));
			elemRoot.appendChild(elem);
		}

		//Add image link
		if (m_imageLink != null)
		{
			elem = a_doc.createElement("ImageLink");
			elem.appendChild(a_doc.createTextNode(m_imageLink));
			elemRoot.appendChild(elem);
		}

		//Add input fields
		for (int i = 0; i < m_inputFields.size(); i++)
		{
			String[] input = (String[]) m_inputFields.elementAt(i);
			elem = a_doc.createElement("input");
			elem.setAttribute("ref", input[0]);
			Element elem2 = a_doc.createElement("label");
			elem.appendChild(elem2);
			if (input[2] != null)
			{
				elem2.setAttribute("lang", input[2]);
			}
			elem2.appendChild(a_doc.createTextNode(input[1]));
			elemRoot.appendChild(elem);
		}

		return elemRoot;
	}

	protected void setValues(Element elemRoot) throws Exception
	{
		if (!elemRoot.getTagName().equals("PaymentOption"))
		{
			throw new Exception(EXCEPTION_WRONG_XML_STRUCTURE);
		}

		m_type = elemRoot.getAttribute("type");
		m_name = elemRoot.getAttribute("name");
		m_generic = XMLUtil.parseAttribute(elemRoot, "generic", true);
		m_minJapVersion = XMLUtil.parseAttribute(elemRoot, "japversion", Util.VERSION_FORMAT);

		NodeList nodesHeadings = elemRoot.getElementsByTagName("Heading");
		for (int i = 0; i < nodesHeadings.getLength(); i++)
		{
			String heading = XMLUtil.parseValue(nodesHeadings.item(i), null);
			String language = ( (Element) nodesHeadings.item(i)).getAttribute("lang");
			if (language == null || heading == null)
			{
				throw new Exception(EXCEPTION_WRONG_XML_STRUCTURE);
			}
			m_headings.addElement(new String[]
								  {heading, language});
		}

		NodeList nodesDetailed = elemRoot.getElementsByTagName("DetailedInfo");
		for (int i = 0; i < nodesDetailed.getLength(); i++)
		{
			String info = XMLUtil.parseValue(nodesDetailed.item(i), null);
			String language = ( (Element) nodesDetailed.item(i)).getAttribute("lang");
			if (language == null || info == null)
			{
				throw new Exception(EXCEPTION_WRONG_XML_STRUCTURE);
			}

			m_detailedInfos.addElement(new String[]
									   {info, language});
		}

		NodeList nodesExtra = elemRoot.getElementsByTagName("ExtraInfo");
		for (int i = 0; i < nodesExtra.getLength(); i++)
		{
			String info = XMLUtil.parseValue(nodesExtra.item(i), null);
			String language = ( (Element) nodesExtra.item(i)).getAttribute("lang");
			String type = ( (Element) nodesExtra.item(i)).getAttribute("type");
			if (language == null || info == null || type == null)
			{
				throw new Exception(EXCEPTION_WRONG_XML_STRUCTURE);
			}
			m_extraInfos.addElement(new String[]
									{info, type, language});
		}

		NodeList nodesInput = elemRoot.getElementsByTagName("input");

		for (int i = 0; i < nodesInput.getLength(); i++)
		{
			String label = XMLUtil.parseValue(nodesInput.item(i).getFirstChild(), null);
			String lang = ( (Element) nodesInput.item(i).getFirstChild()).getAttribute("lang");
			String ref = ( (Element) nodesInput.item(i)).getAttribute("ref");
			if (lang == null || label == null || ref == null)
			{
				throw new Exception(EXCEPTION_WRONG_XML_STRUCTURE);
			}
			m_inputFields.addElement(new String[]
									 {ref, label, lang});
		}

		try
		{
			String imageLink = XMLUtil.parseValue(XMLUtil.getFirstChildByName(elemRoot, "ImageLink").
												  getFirstChild(), "0");
			if (!imageLink.equals("0"))
			{
				m_imageLink = imageLink;
			}
		}
		catch (Exception e)
		{
			m_imageLink = null;
		}
	}

	public void setType(String a_type)
	{
		m_type = a_type;
	}

	public String getHeading(String a_langShort)
	{
		for (int i = 0; i < m_headings.size(); i++)
		{
			String[] heading = (String[]) m_headings.elementAt(i);
			if (heading[1].equalsIgnoreCase(a_langShort))
			{
				return heading[0];
			}
		}
		return getHeading("en");
	}

	public String getDetailedInfo(String a_langShort)
	{
		for (int i = 0; i < m_detailedInfos.size(); i++)
		{
			String[] detailedInfo = (String[]) m_detailedInfos.elementAt(i);
			if (detailedInfo[1].equalsIgnoreCase(a_langShort))
			{
				return detailedInfo[0];
			}
		}
		return getDetailedInfo("en");
	}

	public String getExtraInfo(String a_langShort)
	{
		for (int i = 0; i < m_extraInfos.size(); i++)
		{
			String[] extraInfo = (String[]) m_extraInfos.elementAt(i);
			if (extraInfo[2].equalsIgnoreCase(a_langShort))
			{
				return extraInfo[0];
			}
		}
		return null;
	}

	public String getType()
	{
		return m_type;
	}

	public String getName()
	{
		return m_name;
	}

	public String getExtraInfoType(String a_langShort)
	{
		for (int i = 0; i < m_extraInfos.size(); i++)
		{
			String[] extraInfo = (String[]) m_extraInfos.elementAt(i);
			if (extraInfo[2].equalsIgnoreCase(a_langShort))
			{
				return extraInfo[1];
			}
		}
		return getExtraInfoType("en");
	}

	public Vector getInputFields()
	{
		return (Vector) m_inputFields.clone();
	}

	public boolean isGeneric()
	{
		return m_generic;
	}

	public String getMinJapVersion()
	{
		return m_minJapVersion;
	}

	public boolean isNewer(XMLPaymentOption a_paymentOption)
	{
		if (m_minJapVersion == null)
		{
				return false;
		}
		if (a_paymentOption.getMinJapVersion() == null)
		{
			return true;
		}
		return (Util.convertVersionStringToNumber(m_minJapVersion) >
				Util.convertVersionStringToNumber(a_paymentOption.getMinJapVersion()));
	}

	public boolean worksWithJapVersion(String a_version)
	{
		if (m_minJapVersion != null)
		{
			if (Util.convertVersionStringToNumber(m_minJapVersion) >
				Util.convertVersionStringToNumber(a_version))
			{
				return false;
			}
		}
		return true;
	}
};