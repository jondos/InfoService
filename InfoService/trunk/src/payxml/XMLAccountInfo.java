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

import java.util.Enumeration;
import java.util.Vector;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import anon.util.IXMLEncodeable;
import anon.util.XMLUtil;

/**
 * This class represents an XML AccountInfo structure.
 *
 *
 * The following XML structure is used:
 * <pre>
 * &lt;AccountInfo&gt;
 *    &lt;Balance&gt;
 *       &lt;AccountNumber&gt; ..&lt;/AccountNumber&gt;
 *       &lt;Deposit&gt;...&lt;/Deposit&gt;
 *       &lt;Spent&gt;....&lt;/Spent&gt;
 *       &lt;Validtime&gt;...&lt;/Validtime&gt;
 *       &lt;Timestamp&gt;...&lt;/Timestamp&gt;
 *       &lt;Signature&gt; //Unterschrift der BI
 *          ...
 *       &lt;/Signature&gt;
 *    &lt;/Balance&gt;
 *    &lt;CostConfirmations&gt; //Kostenbest\u00E4tigungen, die
 *		                           von den einzelnen AI's
 *		                           abgerechnet wurden
 *       &lt;CC&gt;...&lt;/CC&gt;
 *       &lt;CC&gt;...&lt;/CC&gt;
 *    &lt;/CostConfirmations&gt;
 * &lt;/AccountInfo&gt;
 * </pre>
 *
 * @author Bastian Voigt
 */
public class XMLAccountInfo implements IXMLEncodeable //extends XMLDocument
{
	//~ Instance fields ********************************************************

	/** the balance certificate */
	private XMLBalance m_balance = null;

	/**
	 * a collection of costconfirmations (one for each mixcascade
	 * that was used with this account)
	 */
	private Vector m_costConfirmations = new Vector();

	//~ Constructors ***********************************************************

	public XMLAccountInfo(XMLBalance bal) throws Exception
	{
		m_balance = bal;
	}

	/**
	 * Creates an AccountInfo object from a string.
	 */
	public XMLAccountInfo(String xml) throws Exception
	{
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(xml);
		setValues(doc.getDocumentElement());
	}

	public XMLAccountInfo()
	{}

	/**
	 * Creates a Balance from  an existing XML docuemnt
	 *
	 * @param xml the node that represents the Balance
	 * @param verifier JAPSignature must be initialized and ready to verify XML (or null)
	 * @throws Exception on invalid xml format or invalid signature
	 */
	public XMLAccountInfo(Element xml) throws Exception
	{
		setValues(xml);
	}

	//~ Methods ****************************************************************
	public Document getXmlEncoded()
	{
		Document doc = null;
		try
		{
			doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		}
		catch (ParserConfigurationException ex)
		{
			return null;
		}
		Element elemRoot = doc.createElement("AccountInfo");
		elemRoot.setAttribute("version", "1.0");
		doc.appendChild(elemRoot);
		Element elem;
		Element elemTmp;

		// add balance
		if (m_balance != null)
		{
			try
			{
				elemTmp = m_balance.getXmlEncoded().getDocumentElement();
				elem = (Element) XMLUtil.importNode(doc, elemTmp, true);
				elemRoot.appendChild(elem);
			}
			catch (Exception ex1)
			{
			}
		}

		// add CCs
		Element elemCCs = doc.createElement("CostConfirmations");
		elemRoot.appendChild(elemCCs);
		Enumeration enum = m_costConfirmations.elements();
		XMLEasyCC cc;
		while (enum.hasMoreElements())
		{
			try
			{
				cc = (XMLEasyCC) enum.nextElement();
				elemTmp = cc.getXmlEncoded().getDocumentElement();
				elem = (Element) XMLUtil.importNode(doc, elemTmp, true);
				elemCCs.appendChild(elem);
			}
			catch (Exception ex2)
			{
			}
		}

		return doc;
	}

	/**
	 * Adds a cost confirmation xml structure to the accountinfo.
	 * Note: If a cost confirmation for the same AI is already present
	 * it will be overwritten.
	 *
	 * @param xmlCC XMLEasyCC
	 */
	public void addCC(XMLEasyCC cc) throws Exception
	{
		String aiName = cc.getAIName();
		Enumeration enum = m_costConfirmations.elements();
		XMLEasyCC tmp;
		while(enum.hasMoreElements())
		{
			tmp = (XMLEasyCC) enum.nextElement();
			if(tmp.getAIName().equals(aiName))
			{
				m_costConfirmations.removeElement(tmp);
				break;
			}
		}
		m_costConfirmations.addElement(cc);
	}

	private void setValues(Element elemRoot) throws Exception
	{
		if (!elemRoot.getTagName().equals("AccountInfo") )
//			!elemRoot.getAttribute("version").equals("1.0"))
		{
			throw new Exception("XMLAccountInfo wrong XML structure");
		}
		Element elemBalance = (Element) XMLUtil.getFirstChildByName(elemRoot, "Balance");
		m_balance = new XMLBalance(elemBalance);

		// todo parse costconfirmations
		Element elemCCs = (Element) XMLUtil.getFirstChildByName(elemRoot, "CostConfirmations");
		Element elemCC = (Element) elemCCs.getFirstChild();
		while (elemCC != null)
		{
			m_costConfirmations.addElement(new XMLEasyCC(elemCC));
			elemCC = (Element) elemCC.getNextSibling();
		}
	}

	public XMLBalance getBalance()
	{
		return m_balance;
	}

	/**
	 * getCC - returns the cost confirmation with the specified aiName
	 *
	 * @param string String
	 * @return XMLEasyCC
	 */
	public XMLEasyCC getCC(String aiName)
	{
		Enumeration enum = m_costConfirmations.elements();
		XMLEasyCC current;
		while (enum.hasMoreElements())
		{
			current = (XMLEasyCC) enum.nextElement();
			if (current.getAIName().equals(aiName))
			{
				return current;
			}
		}
		return null;
	}
}
