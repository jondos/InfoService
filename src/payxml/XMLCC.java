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
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import anon.util.*;

/**
 * XML structure for a cost confirmation which is sent to the AI by the Jap
 */
public class XMLCC extends XMLDocument
{
	//~ Instance fields ********************************************************

	private String aiName;
	private byte[] hash;
	private int costs;
	private int tickPrice;
	private long accountNumber;

	//~ Constructors ***********************************************************

	public XMLCC(Element elemRoot) throws Exception
	{
		m_theDocument=getDocumentBuilder().newDocument();
		Node n=XMLUtil.importNode(m_theDocument,elemRoot,true);
		setValues();
	}

	public XMLCC(byte[] data) throws Exception
	{
		setDocument(data);
		setValues();
	}

	private void setValues() throws Exception
	{
		Element element = m_theDocument.getDocumentElement();
		if (!element.getTagName().equals("CC"))
		{
			throw new Exception();
		}

		NodeList nl = element.getElementsByTagName("ID");
		if (nl.getLength() < 1)
		{
			throw new Exception();
		}
		element = (Element) nl.item(0);

		CharacterData chdata = (CharacterData) element.getFirstChild();
		aiName = chdata.getData();

		nl = element.getElementsByTagName("AN");
		if (nl.getLength() < 1)
		{
			throw new Exception();
		}
		element = (Element) nl.item(0);
		chdata = (CharacterData) element.getFirstChild();
		accountNumber = Long.parseLong(chdata.getData());

		nl = element.getElementsByTagName("C");
		if (nl.getLength() < 1)
		{
			throw new Exception();
		}
		element = (Element) nl.item(0);
		chdata = (CharacterData) element.getFirstChild();
		costs = Integer.parseInt(chdata.getData());

		nl = element.getElementsByTagName("D");
		if (nl.getLength() < 1)
		{
			throw new Exception();
		}
		element = (Element) nl.item(0);
		chdata = (CharacterData) element.getFirstChild();
		hash = Base64.decode(chdata.getData());

		nl = element.getElementsByTagName("w");
		if (nl.getLength() < 1)
		{
			throw new Exception();
		}

		Element exponent = (Element) nl.item(0);
		chdata = (CharacterData) element.getFirstChild();
		tickPrice = Integer.parseInt(chdata.getData());
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

	public int getCosts()
	{
		return costs;
	}

	public byte[] getHash()
	{
		return hash;
	}

	public int getTickPrice()
	{
		return tickPrice;
	}
}
