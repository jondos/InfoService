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

import org.w3c.dom.Element;
import anon.util.Base64;
import anon.util.XMLUtil;

public class XMLResponse extends XMLDocument
{
	private byte[] m_arbResponse;

	public XMLResponse(String xml) throws Exception
	{
		setDocument(xml);
		setResponse();
	}

	/*
	 public XMLResponse(byte[] data)
	 {
	  response = data;
	  createXmlDocument();
	 }
	 */
	private void setResponse() throws Exception
	{
		Element element = m_theDocument.getDocumentElement();
		if (!element.getTagName().equals("Response"))
		{
			throw new Exception();
		}
		String strBase64Response = XMLUtil.parseNodeString(element, null);
		m_arbResponse = Base64.decode(strBase64Response);
	}

	public byte[] getResponse()
	{
		return m_arbResponse;
	}

	/*private void createXmlDocument()
	  {
	 StringBuffer buffer = new StringBuffer(512);
	 buffer.append("<Response>");
	 buffer.append(Base64.encode(response));
	 buffer.append("</Response>");
	 xmlDocument = buffer.toString();
	  }*/
}
