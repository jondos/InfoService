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
import payxml.util.Base64;

public class XMLChallenge extends XMLDocument
{
	//~ Constructors ***********************************************************

	public XMLChallenge(String xml) throws Exception
	{
		xmlDocument = xml;
		setDocument(xml);
	}

	public XMLChallenge(byte[] data) throws Exception
	{
		xmlDocument = "<Challenge><DontPanic>" +
			new String(Base64.encode(data)) + "</DontPanic></Challenge>";
		setDocument(xmlDocument);
	}

	//~ Methods ****************************************************************

	public byte[] getChallenge() throws Exception
	{
		Element element = domDocument.getDocumentElement();
		if (!element.getTagName().equals("Challenge"))
		{
			throw new Exception();
		}

		NodeList nl = element.getElementsByTagName("DontPanic");
		if (nl.getLength() < 1)
		{
			throw new Exception();
		}
		element = (Element) nl.item(0);

		CharacterData chdata = (CharacterData) element.getFirstChild();
		String challenge = "<" + element.getTagName() + ">" + chdata.getData() +
			"</" + element.getTagName() + ">";

		return challenge.getBytes();
	}
}
