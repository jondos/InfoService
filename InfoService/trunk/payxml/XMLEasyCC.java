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

import org.w3c.dom.CharacterData;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.*;
import anon.util.XMLUtil;
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
public class XMLEasyCC extends XMLDocument
{
	//~ Instance fields ********************************************************

	private String m_strAiName;

	//private byte[] hash;
	private long m_lTransferredBytes;
	private long m_lAccountNumber;

	//~ Constructors ***********************************************************
	public XMLEasyCC(String aiName, long accountNumber, long transferred) throws Exception
	{

		m_strAiName = aiName;
		m_lTransferredBytes = transferred;
		m_lAccountNumber = accountNumber;
		m_theDocument=getDocumentBuilder().newDocument();
		Element elemRoot=m_theDocument.createElement("CC");
		elemRoot.setAttribute("version","1.0");
		m_theDocument.appendChild(elemRoot);

		Element elem=m_theDocument.createElement("AiID");
		XMLUtil.setNodeValue(elem,aiName);
		elemRoot.appendChild(elem);

		elem=m_theDocument.createElement("TransferredBytes");
		XMLUtil.setNodeValue(elem,Long.toString(transferred));
		elemRoot.appendChild(elem);

		elem=m_theDocument.createElement("AccountNumber");
		XMLUtil.setNodeValue(elem,Long.toString(accountNumber));
		elemRoot.appendChild(elem);
	}

	public XMLEasyCC(byte[] data) throws Exception
	{
		setDocument(data);
		setValues();
	}

	/**
	 * Creates an EaysCC from  an existing XML docuemnt
	 *
	 * @param xml the node that represents the EasyCC
	 */
	public XMLEasyCC(Node xml) throws Exception
	{
		m_theDocument=getDocumentBuilder().newDocument();
		Node n=XMLUtil.importNode(m_theDocument,xml,true);
		m_theDocument.appendChild(n);
		setValues();
	}

	private void setValues() throws Exception
	{
		Element element = m_theDocument.getDocumentElement();
		if (!element.getTagName().equals("CC"))
		{
			throw new Exception("XMLEasyCC wrong xml structure");
		}

		NodeList nl = element.getElementsByTagName("AiID");
		if (nl.getLength() < 1)
		{
			throw new Exception("XMLEasyCC wrong xml structure");
		}
		element = (Element) nl.item(0);

		CharacterData chdata = (CharacterData) element.getFirstChild();
		m_strAiName = chdata.getData();

		nl = element.getElementsByTagName("AccountNumber");
		if (nl.getLength() < 1)
		{
			throw new Exception("XMLEasyCC wrong xml structure");
		}
		element = (Element) nl.item(0);
		chdata = (CharacterData) element.getFirstChild();
		m_lAccountNumber = Long.parseLong(chdata.getData());

		nl = element.getElementsByTagName("TransferredBytes");
		if (nl.getLength() < 1)
		{
			throw new Exception("XMLEasyCC wrong xml structure");
		}
		element = (Element) nl.item(0);
		chdata = (CharacterData) element.getFirstChild();
		m_lTransferredBytes = Integer.parseInt(chdata.getData());

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

	public void addTransferredBytes(long plusBytes)
	{
		m_lTransferredBytes += plusBytes;
	}

}
