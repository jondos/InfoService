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

/* Hint: This file may be only a copy of the original file which is always in the JAP source tree!
 * If you change something - do not forget to add the changes also to the JAP source tree!
 */
package anon.util;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.lang.reflect.Method;
import org.w3c.dom.Attr;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public class XMLUtil
{

	public static int parseElementAttrInt(Element e, String attr, int defaultValue)
	{
		int i = defaultValue;
		if (e != null)
		{
			try
			{
				Attr at = e.getAttributeNode(attr);
				i = Integer.parseInt(at.getValue());
			}
			catch (Exception ex)
			{
			}
		}
		return i;
	}

	public static int parseNodeInt(Node n, int defaultValue)
	{
		int i = defaultValue;
		String s = parseNodeString(n, null);
		if (s != null)
		{
			try
			{
				i = Integer.parseInt(s);
			}
			catch (Exception e)
			{
			}
		}
		return i;
	}

	public static boolean parseElementAttrBoolean(Element e, String attr, boolean defaultValue)
	{
		boolean b = defaultValue;
		if (e != null)
		{
			try
			{
				Attr at = e.getAttributeNode(attr);
				String tmpStr = at.getValue().trim();
				if (tmpStr == null)
				{
					return b;
				}
				if (tmpStr.equalsIgnoreCase("true"))
				{
					b = true;
				}
				else if (tmpStr.equalsIgnoreCase("false"))
				{
					b = false;
				}
			}
			catch (Exception ex)
			{
			}
		}

		return b;
	}

	public static boolean parseNodeBoolean(Node n, boolean defaultValue)
	{
		boolean b = defaultValue;
		try
		{
			String tmpStr = parseNodeString(n, null);
			if (tmpStr == null)
			{
				return b;
			}
			if (tmpStr.equalsIgnoreCase("true"))
			{
				b = true;
			}
			else if (tmpStr.equalsIgnoreCase("false"))
			{
				b = false;
			}
		}
		catch (Exception e)
		{
		}
		return b;
	}

	/** Gets the content of an Element or Text Node. The "content" of an Element Node is
	 * the text between the opening and closing Element Tag. The content of an attribute node
	 * is the value of the attributte. For all over nodes null is returned.
	 * @param n text node, element node or attribute node
	 * @param defaultValue value returned, if an error occured
	 * @return null if this node has no "content"
	 * @return defaultValue if an error occured
	 * @return "content" of the node
	 */
	public static String parseNodeString(Node n, String defaultValue)
	{
		String s = defaultValue;
		if (n != null)
		{
			try
			{
				if (n.getNodeType() == n.ELEMENT_NODE)
				{
					n = n.getFirstChild();
				}
				s = n.getNodeValue();
			}
			catch (Exception e)
			{
			}
		}
		return s;
	}

	public static Node getFirstChildByName(Node n, String name)
	{
		try
		{
			Node child = n.getFirstChild();
			while (child != null)
			{
				if (child.getNodeName().equals(name))
				{
					return child;
				}
				child = child.getNextSibling();
			}
		}
		catch (Exception e)
		{
		}
		return null;
	}

	// ootte
	public static Node getFirstChildByNameUsingDeepSearch(Node n, String name)
	{
		try
		{
			if (n.getNodeName().equals(name)) // found!
			{
				return n;
			}
			if (n.hasChildNodes()) // not found, but the Node has children ...
			{
				NodeList nl = n.getChildNodes();
				for (int i = 0; i < nl.getLength(); i++)
				{
					Node child = nl.item(i);
					Node tmp_result = getFirstChildByNameUsingDeepSearch(child, name);
					if (tmp_result != null)
					{
						return tmp_result;
					}
				}
			}
			else // Node has no children and is not the Node we are looking for
			{
				return null;
			}
		}
		catch (Exception e)
		{
		}
		return null;
	}

	// ootte
	public static Node getLastChildByName(Node n, String name)
	{
		try
		{
			Node child = n.getLastChild();
			while (child != null)
			{
				if (child.getNodeName().equals(name))
				{
					return child;
				}
				child = child.getPreviousSibling();
			}
		}
		catch (Exception e)
		{
		}
		return null;
	}

	public static void setNodeValue(Node n, String text)
	{
		n.appendChild(n.getOwnerDocument().createTextNode(text));
	}

	/** Stolen from Apache Xerces-J...*/
	public static Node importNode(Document doc, Node source, boolean deep) throws Exception
	{

		Node newnode = null;

		// Sigh. This doesn't work; too many nodes have private data that
		// would have to be manually tweaked. May be able to add local
		// shortcuts to each nodetype. Consider ?????
		// if(source instanceof NodeImpl &&
		//	!(source instanceof DocumentImpl))
		// {
		//  // Can't clone DocumentImpl since it invokes us...
		//	newnode=(NodeImpl)source.cloneNode(false);
		//	newnode.ownerDocument=this;
		//}
		//else
		int type = source.getNodeType();
		switch (type)
		{

			case Document.ELEMENT_NODE:
			{
				Element newelement = doc.createElement(source.getNodeName());
				NamedNodeMap srcattr = source.getAttributes();
				if (srcattr != null)
				{
					for (int i = 0; i < srcattr.getLength(); i++)
					{
						newelement.setAttributeNode(
							(Attr) importNode(doc, srcattr.item(i), true));
					}
				}
				newnode = newelement;
				break;
			}

			case Document.ATTRIBUTE_NODE:
			{
				newnode = doc.createAttribute(source.getNodeName());
				newnode.setNodeValue(source.getNodeValue());
				// Kids carry value
				break;
			}

			case Document.TEXT_NODE:
			{
				newnode = doc.createTextNode(source.getNodeValue());
				break;
			}

			case Document.CDATA_SECTION_NODE:
			{
				newnode = doc.createCDATASection(source.getNodeValue());
				break;
			}

			case Document.ENTITY_REFERENCE_NODE:
			{
				newnode = doc.createEntityReference(source.getNodeName());
				deep = false; // ????? Right Thing?
				// Value implied by doctype, so we should not copy it
				// -- instead, refer to local doctype, if any.
				break;
			}

			case Document.ENTITY_NODE:
			{
				/*Entity srcentity = (Entity) source;
				  Entity newentity = doc.createEntity(source.getNodeName());
				  newentity.setPublicId(srcentity.getPublicId());
				  newentity.setSystemId(srcentity.getSystemId());
				  newentity.setNotationName(srcentity.getNotationName());
				  // Kids carry additional value
				  newnode = newentity;*/
				throw new Exception("HIERARCHY_REQUEST_ERR");
				//break;
			}

			case Document.PROCESSING_INSTRUCTION_NODE:
			{
				newnode = doc.createProcessingInstruction(source.getNodeName(),
					source.getNodeValue());
				break;
			}

			case Document.COMMENT_NODE:
			{
				newnode = doc.createComment(source.getNodeValue());
				break;
			}

			case Document.DOCUMENT_TYPE_NODE:
			{
				/*	DocumentType doctype = (DocumentType) source;
				 DocumentType newdoctype =
				  doc.createDocumentType(
				  doctype.getNodeName(),
				  doctype.getPublicID(), doctype.getSystemID());
				 // Values are on NamedNodeMaps
				 NamedNodeMap smap = ( (DocumentType) source).getEntities();
				 NamedNodeMap tmap = newdoctype.getEntities();
				 if (smap != null)
				 {
				  for (int i = 0; i < smap.getLength(); i++)
				  {
				   tmap.setNamedItem( (EntityImpl) importNode(smap.item(i), true));
				  }
				 }
				 smap = ( (DocumentType) source).getNotations();
				 tmap = newdoctype.getNotations();
				 if (smap != null)
				 {
				  for (int i = 0; i < smap.getLength(); i++)
				  {
				   tmap.setNamedItem( (NotationImpl) importNode(smap.item(i), true));
				  }
				 }
				 // NOTE: At this time, the DOM definition of DocumentType
				 // doesn't cover Elements and their Attributes. domimpl's
				 // extentions in that area will not be preserved, even if
				 // copying from domimpl to domimpl. We could special-case
				 // that here. Arguably we should. Consider. ?????
				 newnode = newdoctype;
				 break;*/
				throw new Exception("HIERARCHY_REQUEST_ERR");

			}

			case Document.DOCUMENT_FRAGMENT_NODE:
			{
				newnode = doc.createDocumentFragment();
				// No name, kids carry value
				break;
			}

			case Document.NOTATION_NODE:
			{
				/*
				   Notation srcnotation = (Notation) source;
				   Notation newnotation = (Notation) doc.createNotation(source.getNodeName());
				   newnotation.setPublicId(srcnotation.getPublicId());
				   newnotation.setSystemId(srcnotation.getSystemId());
				   // Kids carry additional value
				   newnode = newnotation;
				   // No name, no value
				   break;*/
				throw new Exception("HIERARCHY_REQUEST_ERR");

			}

			case Document.DOCUMENT_NODE: // Document can't be child of Document
			default:
			{ // Unknown node type
				throw new Exception("HIERARCHY_REQUEST_ERR");
			}
		}

		// If deep, replicate and attach the kids.
		if (deep)
		{
			for (Node srckid = source.getFirstChild();
				 srckid != null;
				 srckid = srckid.getNextSibling())
			{
				newnode.appendChild(importNode(doc, srckid, true));
			}
		}

		return newnode;

	}

	/** Writes a XML-Document to an Output-Stream. Since writing was not standardzieds
	 * since JAXP 1.1 different Methods are tried
	 */
	public static String XMLDocumentToString(Document doc)
	{

		return XMLNodeToString(doc);
	}

	/** Writes a XML-Node to an Output-Stream. If node is a Document than the <XML> header is included.
	 * Since writing was not standardzieds
	 * until  JAXP 1.1 different Methods are tried
	 */
	public static String XMLNodeToString(Node node)
	{
		ByteArrayOutputStream out = null;
		try
		{
			out = new ByteArrayOutputStream();
		}
		catch (Throwable t3)
		{
			return null;
		}
		try //For JAXP 1.0.1 Reference Implementation (shipped with JAP)
		{
			Class c = Class.forName("com.sun.xml.tree.ParentNode");
			if (c.isInstance(node))
			{
				Document doc = null;
				if (node instanceof Document)
				{
					doc = (Document) node;
				}
				else
				{
					doc = node.getOwnerDocument();
				}
				Writer w = new OutputStreamWriter(out, "UTF8");
				//What we do here is acutally:
				//com.sun.xml.tree.XmlWriteContext context=
				//						((com.sun.xml.tree.XmlDocument)doc).createWriteContext(w,2);
				//((com.sun.xml.tree.ElementNode)node).writeXml(context);
				//We do that this way to avoid the need of JAXP1.0 for compilation!
				Class classXmlDocument = Class.forName("com.sun.xml.tree.XmlDocument");
				Class[] paramClasses = new Class[2];
				paramClasses[0] = Writer.class;
				paramClasses[1] = int.class;
				Method methodCreateWriteContext = classXmlDocument.getMethod("createWriteContext",
					paramClasses);
				Object params[] = new Object[2];
				params[0] = w;
				params[1] = new Integer(2);
				Object context = methodCreateWriteContext.invoke(doc, params);
				paramClasses = new Class[1];
				paramClasses[0] = Class.forName("com.sun.xml.tree.XmlWriteContext");
				Method methodWriteXml = node.getClass().getMethod("writeXml", paramClasses);
				params = new Object[1];
				params[0] = context;
				methodWriteXml.invoke(node, params);
				w.flush();
				return out.toString("UTF8");
			}
		}
		catch (Throwable t1)
		{
		}
		try
		{ //For JAXP 1.1 (for Instance Apache Crimson/Xalan shipped with Java 1.4)
			//This seams to be realy stupid and compliecated...
			//But if the do a simple t.transform(), a NoClassDefError is thrown, if
			//the new JAXP1.1 is not present, even if we NOT call saveXMLDocument, but
			//calling any other method within JAPUtil.
			//Dont no why --> maybe this has something to to with Just in Time compiling ?
			Object t =
				javax.xml.transform.TransformerFactory.newInstance().newTransformer();
			javax.xml.transform.Result r = new javax.xml.transform.stream.StreamResult(out);
			javax.xml.transform.Source s = new javax.xml.transform.dom.DOMSource(node);

			//this is to simply invoke t.transform(s,r)
			Class c = t.getClass();
			Method m = null;
			Method[] ms = c.getMethods();
			for (int i = 0; i < ms.length; i++)
			{
				if (ms[i].getName().equals("transform"))
				{
					m = ms[i];
					Class[] params = m.getParameterTypes();
					if (params.length == 2)
					{
						break;
					}
				}
			}
			Object[] p = new Object[2];
			p[0] = s;
			p[1] = r;
			m.invoke(t, p);
			return out.toString();
		}
		catch (Throwable t2)
		{
			return null;
		}
	}

}
