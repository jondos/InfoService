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

import java.io.ByteArrayInputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import anon.util.XMLUtil;
/**
 * this class is one hell of a shhhhhhhrrrrrrrrrrrrrrrr
 */
public class XMLDocument
{
	//~ Static fields/initializers *********************************************

	public static final String XML_HEAD = "<?xml version=\"1.0\"?>\n";

	//~ Instance fields ********************************************************

	protected Document m_theDocument;
	//protected String xmlDocument;

	//~ Constructors ***********************************************************

	public XMLDocument()
	{
		m_theDocument = null;
		//xmlDocument = null;
	}

	//~ Methods ****************************************************************

	public byte[] getXMLByteArray()
	{
		return getXMLString().getBytes();
	}

	public String getXMLString()
	{
		return XMLUtil.XMLDocumentToString(m_theDocument);
	}


	protected DocumentBuilder getDocumentBuilder() throws Exception
	{
		return DocumentBuilderFactory.newInstance().newDocumentBuilder();
	}

	/**
	 * Builds the internal DOM tree from xml string data
	 *
	 * @param data the xml data as byte array
	 */
	protected void setDocument(byte[] data) throws Exception
	{
		ByteArrayInputStream bai = new ByteArrayInputStream(data);
		m_theDocument = getDocumentBuilder().parse(bai);
	}

	/**
	 * Builds the internal DOM tree from xml string data
	 *
	 * @param data the xml data as string
	 */
	protected void setDocument(String data) throws Exception
	{
		setDocument(data.getBytes());
	}

	public Document getDomDocument()
	{
		return m_theDocument;
	}
}
