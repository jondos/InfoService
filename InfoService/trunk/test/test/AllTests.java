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
package test;

import java.io.IOException;
import java.io.ByteArrayInputStream;

import org.w3c.dom.Document;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.Node;
import org.w3c.dom.Comment;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

import anon.util.ResourceLoader;
import anon.util.XMLUtil;
import anon.util.Util;

/**
 * This is the test suite which combines all other JUnit tests of the project.
 * It can be run from the command line with a graphical user interface.
 *
 * @author Rolf Wendolsky
 */
public class AllTests
{
	private static final String XML_STRUCTURE_PATH = "documentation/xmlStructures/";
	private static final ResourceLoader ms_resourceLoader = new ResourceLoader(null);

	/**
	 * The main function.
	 *
	 * @param a_Args (no arguments needed)
	 */
	public static void main(String[] a_Args)
	{
		junit.swingui.TestRunner.run(AllTests.class);
	}

	/**
	 * Returns the test suite that combines all other tests of the project.
	 *
	 * @return Test The test suite that combines all other tests of the project.
	 */
	public static Test suite()
	{
		TestSuite suite = new TestSuite(AllTests.class.getName());
		suite.addTest(anon.test.AllTests.suite());
		return suite;
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
	 * @param a_node a Node
	 * @param a_filename the file where the xml information is written to
	 * @param a_createrClass the class that created the node
	 * @param a_testClass the TestCase class that called this method
	 * @throws IOException
	 */
	public static void writeXMLNodeToFile(Node a_node, Class a_createrClass, Class a_testClass)
		throws IOException
	{
		if (!TestCase.class.isAssignableFrom(a_testClass))
		{
			throw new IllegalArgumentException("This method can only be called by a test case!");
		}

		Comment comment;

		// set a comment
		comment = a_node.getOwnerDocument().createComment(
				  "This xml structure has been created by " + a_createrClass + ".\n" +
				  "The calling test class was " + a_testClass + ".");
		a_node.insertBefore(comment, a_node.getFirstChild());

		// write to file
		XMLUtil.writeXMLNodeToFile(a_node, XML_STRUCTURE_PATH +
								   Util.getClassNameWithoutPackage(a_createrClass) + ".xml");
	}
}
