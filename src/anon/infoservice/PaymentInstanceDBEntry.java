/*
 Copyright (c) 2000 - 2004, The JAP-Team
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

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import anon.util.IXMLEncodable;
import anon.util.XMLParseException;
import anon.util.XMLUtil;
import java.util.Vector;
import java.util.Enumeration;

/** Holds the information of a payment instance for storing in the InfoService.*/
public class PaymentInstanceDBEntry extends AbstractDatabaseEntry implements IDistributable, IXMLEncodable
{
	/**
	 * This is the ID of this payment instance.
	 */
	private String m_strPaymentInstanceId;

	/**
	 * Stores the XML representation of this PaymentInstanceDBEntry.
	 */
	private Element m_xmlDescription;

	/**
	 * Stores the time when this payment instance entry was created by the origin payment instance.
	 *  This value is used to determine the more recent
	 * payment instance entry, if two entries are compared (higher version number -> more recent entry).
	 */
	private long m_creationTimeStamp;

	/** Creates a PaymentInstanceDBEntry which represents a payment instance.*/
	public PaymentInstanceDBEntry(Element elemRoot) throws XMLParseException
	{
		super(System.currentTimeMillis()+Constants.TIMEOUT_PAYMENT_INTERFACE);
		if (elemRoot == null)
		{
			throw new XMLParseException(XMLParseException.NODE_NULL_TAG);
		}

		/* store the XML representation */
		m_xmlDescription = elemRoot;

		/* get the ID */
		m_strPaymentInstanceId = elemRoot.getAttribute("id");

		/* get the creation timestamp */
		m_creationTimeStamp = XMLUtil.parseValue(XMLUtil.getFirstChildByName(elemRoot, "LastUpdate"),
												 -1L);
		if (m_creationTimeStamp == -1)
		{
			throw new XMLParseException("LastUpdate");
		}
	}

	public PaymentInstanceDBEntry(String id,String name,Vector listeners,long creationTime)
	{
		super(System.currentTimeMillis()+Constants.TIMEOUT_PAYMENT_INTERFACE);
		m_strPaymentInstanceId=id;
		m_creationTimeStamp=creationTime;
		Document doc=XMLUtil.createDocument();
		Element elemRoot=doc.createElement(getXmlElementName());
		doc.appendChild(elemRoot);
		elemRoot.setAttribute("id",m_strPaymentInstanceId);
		Element elemName=doc.createElement("Name");
		XMLUtil.setValue(elemName,name);
		elemRoot.appendChild(elemName);
		Element elemNet=doc.createElement("Network");
		elemRoot.appendChild(elemNet);
		Element elemListeners=doc.createElement("ListenerInterfaces");
		elemNet.appendChild(elemListeners);
		Enumeration enumer=listeners.elements();
		while(enumer.hasMoreElements())
		{
			ListenerInterface li=(ListenerInterface)enumer.nextElement();
			elemListeners.appendChild(li.toXmlElement(doc));
		}
		Element elemLastUpdate=doc.createElement("LastUpdate");
		XMLUtil.setValue(elemLastUpdate,m_creationTimeStamp);
		elemRoot.appendChild(elemLastUpdate);
		m_xmlDescription=elemRoot;
	}

	public String getId()
	{
		return m_strPaymentInstanceId;
	}

	/**
	 * Returns the time when this payment instance entry was created by the origin payment instance.
	 *
	 * @return A version number which is used to determine the more recent payment instance entry, if two
	 *         entries are compared (higher version number -> more recent entry).
	 */
	public long getVersionNumber()
	{
		return m_creationTimeStamp;
	}

	/**
	 * This returns the filename (InfoService command), where this PaymentInstanceDBEntry is posted at
	 * other InfoServices. It's always '/paymentinstance'.
	 *
	 * @return The filename where the information about this PaymentInstanceDBEntry is posted at other
	 *         InfoServices when this entry is forwarded.
	 */
	public String getPostFile()
	{
		return "/paymentinstance";
	}

	/**
	 * This returns the data, which are posted to other InfoServices. It's the whole XML structure
	 * of this PaymentInstanceDBEntry.
	 *
	 * @return The data, which are posted to other InfoServices when this entry is forwarded.
	 */
	public byte[] getPostData()
	{
		return (XMLUtil.toString(m_xmlDescription).getBytes());
	}

	/**
	 * Returns the name of the XML element constructed by this class.
	 *
	 * @return the name of the XML element constructed by this class
	 */
	public static String getXmlElementName()
	{
		return "PaymentInstance";
	}

	/**
	 * Creates an XML node for this PaymentInstance.
	 * @param a_doc The XML document, which is the environment for the created XML node.
	 * @return The InfoService XML node.
	 */
	public Element toXmlElement(Document a_doc)
	{
		Element returnXmlStructure = null;
		try
		{
			returnXmlStructure = (Element) (XMLUtil.importNode(a_doc, m_xmlDescription, true));
		}
		catch (Exception e)
		{
		}
		return returnXmlStructure;
	}
}
