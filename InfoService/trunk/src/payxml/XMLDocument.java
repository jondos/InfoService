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
import java.io.OutputStream;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;


public class XMLDocument
{
	//~ Static fields/initializers *********************************************

	public static final String XML_HEAD = "<?xml version=\"1.0\"?>\n";

	//~ Instance fields ********************************************************

	protected Document domDocument;
	protected DocumentBuilder builder;
	protected String xmlDocument;

	//~ Constructors ***********************************************************

	public XMLDocument()
	{
		builder = null;
		domDocument = null;
		xmlDocument = null;
	}

	//~ Methods ****************************************************************

	public byte[] getXMLByteArray()
	{
		return xmlDocument.getBytes();
	}


	public String getXMLString(boolean withHead)
	{
		if (withHead) {
			return XML_HEAD + xmlDocument;
		}

		return xmlDocument;
	}


	/**
	 * produces a canonical representation of the XML node and writes
	 * it to the output stream.
	 * See {@link http://www.w3.org/TR/2001/REC-xml-c14n-20010315} for an
	 * introduction to canonical XML
	 *
	 * @param node the DOM node that is to be made canonical
	 * @param o the output stream where the canonical string
	 *        will be written to
	 * @param bSiblings if this is set to true, the node's siblings will also
	 *                  be made canonical
	 *
	 * @return 0 if successful, a negative integer otherwise
	 *
	 * FIXME: Thread safe?
	 */
	public static int makeCanonical(Node node, OutputStream o, boolean bSiblings)
	{
		try {
			if (node == null) {
				return 0;
			}
			if (node.getNodeType() == node.ELEMENT_NODE) {
				Element elem = (Element) node;
				o.write('<');
				o.write(elem.getNodeName().getBytes());

				NamedNodeMap attr = elem.getAttributes();
				if (attr.getLength() > 0) {
					for (int i = 0; i < attr.getLength(); i++) {
						o.write(' ');
						o.write(attr.item(i).getNodeName().getBytes());
						o.write('=');
						o.write('\"');
						o.write(attr.item(i).getNodeValue().getBytes());
						o.write('\"');
					}
				}
				o.write('>');
				if (elem.hasChildNodes()) {
					if (makeCanonical(elem.getFirstChild(), o, true) == -1) {
						return -1;
					}
				}
				o.write('<');
				o.write('/');
				o.write(elem.getNodeName().getBytes());
				o.write('>');
				if (bSiblings &&
						(makeCanonical(elem.getNextSibling(), o, true) == -1)
				) {
					return -1;
				}
			} else if (node.getNodeType() == node.TEXT_NODE) {
				o.write(node.getNodeValue().getBytes());
				if (makeCanonical(node.getNextSibling(), o, true) == -1) {
					return -1;
				}

				return 0;
			} else if (node.getNodeType() == node.COMMENT_NODE) {
				if (makeCanonical(node.getNextSibling(), o, true) == -1) {
					return -1;
				}

				return 0;
			} else {
				return -1;
			}

			return 0;
		} catch (Exception e) {
			return -1;
		}
	}


	protected void setBuilder() throws Exception
	{
		if (builder == null) {
			builder = (DocumentBuilderFactory.newInstance()).newDocumentBuilder();
		}
	}


	/**
	 * Builds the internal DOM tree from xml string data
	 *
	 * @param data the xml data as byte array
	 */
	protected void setDocument(byte[] data) throws Exception
	{
		if (builder == null) {
			setBuilder();
		}

		ByteArrayInputStream bai = new ByteArrayInputStream(data);
		String tmp = new String(data);
		if (tmp.startsWith(XML_HEAD)) {
			xmlDocument = tmp.substring(XML_HEAD.length());
		} else {
			xmlDocument = tmp;
		}
		try {
			domDocument = builder.parse(bai);
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}


	protected void setDocument(String data) throws Exception
	{
		setDocument(data.getBytes());
	}
}
