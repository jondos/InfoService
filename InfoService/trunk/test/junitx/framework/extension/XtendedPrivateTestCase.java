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
package junitx.framework.extension;

import java.io.ByteArrayInputStream;

import org.w3c.dom.Document;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Comment;

import junitx.framework.PrivateTestCase;

import anon.util.XMLUtil;
import anon.util.Util;
import anon.util.ResourceLoader;
import anon.util.IXMLEncodable;

/**
 * Extends the PrivateTestCase with useful functions and should be used instead of it.
 * @author Rolf Wendolsky
 */
public class XtendedPrivateTestCase extends PrivateTestCase
{
	private static final String XML_STRUCTURE_PATH = "documentation/xmlStructures/";
	private static final ResourceLoader ms_resourceLoader = new ResourceLoader(null);

	/**
	 * Creates a new test case.
	 * @param a_name the name of the test case
	 */
	public XtendedPrivateTestCase(String a_name)
	{
		super(a_name);
	}

	/**
	 * Loads an xml structure from the structures directory. All comments,
	 * empty lines and new lines are removed from the structure.
	 * @param a_filename the name of the xml structure file
	 * @throws Exception if an error occurs while loading the file
	 * @return an xml structure from the structures directory
	 */
	public static Node loadXMLNodeFromFile(String a_filename)
	throws Exception
	{
		Document doc = null;

		try
		{
			doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(
				new ByteArrayInputStream(
				ms_resourceLoader.loadResource(
				XML_STRUCTURE_PATH + a_filename)));
			XMLUtil.removeComments(doc);
		} catch (Exception a_e)
		{
		}

		return doc;
	}
	/**
	 * Writes an xml node to a file in the XML_STRUCTURE_PATH with the filename <class>.xml.
	 * @param a_XmlCreaterObject the object that created the node
	 * @param a_bLongClassName if true, the filename will be the class name plus package,
	 *                         if false, it will be the classname only
	 * @throws Exception if an error occurs
	 */
	public void writeXMLOutputToFile(IXMLEncodable a_XmlCreaterObject, boolean a_bLongClassName)
		throws Exception
	{
		Comment comment1, comment2;
		Element element;
		String filename;
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();

		if (a_bLongClassName)
		{
			filename = a_XmlCreaterObject.getClass().getName();
		}
		else
		{
			filename = Util.getShortClassName(a_XmlCreaterObject.getClass());
		}

		// set a comment
		comment1 = doc.createComment(
				  "This xml structure has been created by " +
				  a_XmlCreaterObject.getClass().getName() + ".");
		comment2 = doc.createComment( "The calling test class was " + getClass().getName() + ".");
		//doc.appendChild(comment);
		//doc.appendChild(doc.createComment("\n"));

		element = a_XmlCreaterObject.toXmlElement(doc);
		element.insertBefore(comment2, element.getFirstChild());
		element.insertBefore(comment1, element.getFirstChild());
		doc.appendChild(element);

		// write to file
		XMLUtil.writeXMLDocumentToFile(doc, XML_STRUCTURE_PATH + filename + ".xml");

	}


	/**
	 * Writes an xml node to a file in the XML_STRUCTURE_PATH with the filename <class>.xml.
	 * @param a_XmlCreaterObject the object that created the node
	 * @throws Exception
	 */
	public void writeXMLOutputToFile(IXMLEncodable a_XmlCreaterObject)
		throws Exception
	{
		writeXMLOutputToFile(a_XmlCreaterObject,false);
	}

}
