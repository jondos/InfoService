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


/**
 * XML structure for a easy cost confirmation (without mircopayment function) which is sent to the AI by the Jap
 * @author Grischan Gl&auml;nzel
 */
public class XMLEasyCC extends XMLDocument
{
	//~ Public Fields  ******************************************************
		public static final String docStartTag = "<CC version=\"1.0\">";
		public static final String docEndTag = "</CC>";


	//~ Instance fields ********************************************************

	private String aiName;
	//private byte[] hash;
	private long transferredBytes;
	private long accountNumber;

	//~ Constructors ***********************************************************
	public XMLEasyCC(String aiName,long accountNumber,long transferred) throws Exception{

		this.aiName = aiName;
		this.transferredBytes = transferred;
		this.accountNumber = accountNumber;

		xmlDocument = docStartTag+"<AIName>"+aiName+"</AIName>"+
		"<Bytes>"+transferred+"</Bytes>"+
		"<Number>"+accountNumber+"</Number>"+docEndTag;
		setDocument(xmlDocument);

	}


	public XMLEasyCC(byte[] data) throws Exception
	{
		setDocument(data);

		Element element = domDocument.getDocumentElement();
		if (!element.getTagName().equals("CC")) {
			throw new Exception();
		}

		NodeList nl = element.getElementsByTagName("AIName");
		if (nl.getLength() < 1) {
			throw new Exception();
		}
		element = (Element) nl.item(0);

		CharacterData chdata = (CharacterData) element.getFirstChild();
		aiName = chdata.getData();

		nl = element.getElementsByTagName("Number");
		if (nl.getLength() < 1) {
			throw new Exception();
		}
		element = (Element) nl.item(0);
		chdata = (CharacterData) element.getFirstChild();
		accountNumber = Long.parseLong(chdata.getData());

		nl = element.getElementsByTagName("Bytes");
		if (nl.getLength() < 1) {
			throw new Exception();
		}
		element = (Element) nl.item(0);
		chdata = (CharacterData) element.getFirstChild();
		transferredBytes = Integer.parseInt(chdata.getData());

	}

	//~ Methods ****************************************************************

	public String getAIName()
	{
		return aiName;
	}


	public long getAccountNumber()
	{
		return accountNumber;
	}


	public long getTransferredBytes()
	{
		return transferredBytes;
	}

	public void addTransferredBytes(long plusBytes)
	{
			transferredBytes += plusBytes;
	}

}
