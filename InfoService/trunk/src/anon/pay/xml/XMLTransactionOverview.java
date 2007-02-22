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
import java.util.Enumeration;
import java.util.Vector;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import anon.util.IXMLEncodable;
import anon.util.XMLUtil;

/**
 * This class is used by JAP to ask the Payment Instance about
 * used transaction numbers. JAP has to send the structure without the
 * attributed "used" to the Payment Instance.
 * The PI will then fill in the attributes according to its database entries.
 *
 * <TransactionOverview version="1.0">
 *    <TransferNumber used="false">232343455</TransferNumber>
 *    <TransferNumber used="true" date="234358734908" amount="13242424">435675747</TransferNumber>
 * </TransactionOverview>
 *
 *
 * @author Tobias Bayer
 */
public class XMLTransactionOverview implements IXMLEncodable
{
	public static final Object XML_ELEMENT_NAME = "TransactionOverview";

	/** Contains transfer numbers, their "used" attribute and the used date. The data is
	 * represented as a String[] with first element: tan, second element: used, third element: date,
	 * fourth element: amount
	 **/
	private Vector m_tans = new Vector();

	public XMLTransactionOverview()
	{}

	public XMLTransactionOverview(String xml) throws Exception
	{
		Document doc = XMLUtil.toXMLDocument(xml);
		setValues(doc.getDocumentElement());
	}

	public XMLTransactionOverview(byte[] xml) throws Exception
	{
		Document doc = XMLUtil.toXMLDocument(xml);
		setValues(doc.getDocumentElement());
	}

	public XMLTransactionOverview(Document doc) throws Exception
	{
		setValues(doc.getDocumentElement());
	}

	public XMLTransactionOverview(Element element) throws Exception
	{
		setValues(element);
	}

	public int size()
	{
		return m_tans.size();
	}

	private void setValues(Element elemRoot) throws Exception
	{
		m_tans = new Vector();
		if (!elemRoot.getTagName().equals(XML_ELEMENT_NAME) ||
			!elemRoot.getAttribute("version").equals("1.1"))
		{
			throw new Exception("TransactionOverview wrong format or wrong version number");
		}
		NodeList nodesTans = elemRoot.getElementsByTagName("TransferNumber");
		for (int i = 0; i < nodesTans.getLength(); i++)
		{
			String tan = nodesTans.item(i).getFirstChild().getNodeValue();
			String used;
			if ( ( (Element) nodesTans.item(i)).getAttribute("used") != null)
			{
				used = ( (Element) nodesTans.item(i)).getAttribute("used");
			}
			else
			{
				used = "false";
			}
			String date;
			if ( ( (Element) nodesTans.item(i)).getAttribute("date") != null)
			{
				date = ( (Element) nodesTans.item(i)).getAttribute("date");
			}
			else
			{
				date = "0";
			}

			String amount;
			if ( ( (Element) nodesTans.item(i)).getAttribute("amount") != null)
			{
				amount = ( (Element) nodesTans.item(i)).getAttribute("amount");
			}
			else
			{
				amount = "0";
			}

			String[] line =
				{
				tan,
				used,
				date,
				amount
			};
			m_tans.addElement(line);
		}

	}

	public Element toXmlElement(Document a_doc)
	{
		Element elemRoot = a_doc.createElement("TransactionOverview");
		elemRoot.setAttribute("version", "1.1");

		Element elem;
		Enumeration tans = m_tans.elements();
		while (tans.hasMoreElements())
		{
			String[] line = (String[]) tans.nextElement();
			elem = a_doc.createElement("TransferNumber");
			elem.setAttribute("used", line[1]);
			elem.setAttribute("date", line[2]);
			elem.setAttribute("amount", line[3]);
			elem.appendChild(a_doc.createTextNode(line[0]));
			elemRoot.appendChild(elem);
		}
		return elemRoot;
	}

	/**
	 * Gets an vector of all transfer numbers
	 * @return Vector
	 */
	public Vector getTans()
	{
		return m_tans;
	}

	/**
	 * Returns if a specific transfer number is marked as "used".
	 * @param a_tan long
	 * @return boolean
	 */
	public boolean isUsed(long a_tan)
	{
		boolean used = false;
		for (int i = 0; i < m_tans.size(); i++)
		{
			String[] line = (String[]) m_tans.elementAt(i);
			if (line[0].equals(String.valueOf(a_tan)))
			{
				if (line[1].equalsIgnoreCase("true"))
				{
					used = true;
				}
			}
		}
		return used;
	}

	/**
	 * Sets a specific tan to used or not used
	 * @param a_tan long
	 * @param a_used boolean
	 * @param a_usedDate long
	 */
	public void setUsed(long a_tan, boolean a_used, long a_usedDate, long amount)
	{
		Enumeration e = m_tans.elements();

		while(e.hasMoreElements())
		{
			String[] line = (String[]) e.nextElement();
			if (line[0].equals(String.valueOf(a_tan)))
			{
				String tan = line[0];
				m_tans.addElement(new String[]
								  {tan, String.valueOf(a_used), String.valueOf(a_usedDate),
								  String.valueOf(amount)});
				m_tans.removeElement(line);
			}

		}
	}

	/**
	 * Adds a transfer number and sets its state to "not used".
	 * @param a_tan long
	 */
	public void addTan(long a_tan)
	{
		m_tans.addElement(new String[]
						  {String.valueOf(a_tan), "false", "0", "0"});
	}

}
