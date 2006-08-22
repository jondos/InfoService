/*
 Copyright (c) 2000 - 2006, The JAP-Team
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
package anon.infoservice;

import org.w3c.dom.Element;
import anon.util.XMLUtil;
import org.w3c.dom.Document;
import anon.util.IXMLEncodable;

/**
 * This class implements common methods that may be used by distributabe database entries.
 *
 * @author Rolf Wendolsky
 */
public abstract class AbstractDistributableDatabaseEntry extends AbstractDatabaseEntry
	implements IDistributable, IXMLEncodable
{
	public AbstractDistributableDatabaseEntry(long a_expireTime)
	{
		super(a_expireTime);
	}

	/**
	 * Returns the XML structure for this db entry.
	 *
	 * @return The XML node of this db entry
	 */
	public abstract Element getXmlStructure();

	/**
	 * This returns the data, which are posted to other InfoServices. It's the whole XML structure
	 * of this DBEntry.
	 *
	 * @return The data, which are posted to other InfoServices when this entry is forwarded.
	 */
	public final byte[] getPostData()
	{
		return (XMLUtil.toString(getXmlStructure()).getBytes());
	}

	/**
	 * Creates an XML node for this db entry.
	 * @param a_doc The XML document, which is the environment for the created XML node.
	 * @return The db entry XML node or null if an error occured
	 */
	public final Element toXmlElement(Document a_doc)
	{
		Element returnXmlStructure = null;
		try
		{
			returnXmlStructure = (Element) (XMLUtil.importNode(a_doc, getXmlStructure(), true));
		}
		catch (Exception e)
		{
		}
		return returnXmlStructure;
	}
}
