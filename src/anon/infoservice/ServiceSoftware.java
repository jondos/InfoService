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
package anon.infoservice;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Holds the information about the used software in a service.
 */
public class ServiceSoftware
{

	/**
	 * This is the version of the used software.
	 */
	private String version;

	/**
	 * Creates a new ServiceSoftware from XML description (Software node).
	 *
	 * @param softwareNode The Software node from an XML document.
	 */
	public ServiceSoftware(Element softwareNode) throws Exception
	{
		/* get the version */
		NodeList versionNodes = softwareNode.getElementsByTagName("Version");
		if (versionNodes.getLength() == 0)
		{
			throw (new Exception("ServiceSoftware: Error in XML structure."));
		}
		Element versionNode = (Element) (versionNodes.item(0));
		version = versionNode.getFirstChild().getNodeValue();
	}

	/**
	 * Creates a new ServiceSoftware from the version information.
	 *
	 * @param version The software version for this service.
	 */
	public ServiceSoftware(String version)
	{
		this.version = version;
	}

	/**
	 * Creates an XML node without signature for this ServiceSoftware.
	 *
	 * @param doc The XML document, which is the environment for the created XML node.
	 *
	 * @return The Software XML node.
	 */
	public Element toXmlNode(Document doc)
	{
		Element softwareNode = doc.createElement("Software");
		/* Create the child of Software (Version) */
		Element versionNode = doc.createElement("Version");
		versionNode.appendChild(doc.createTextNode(version));
		softwareNode.appendChild(versionNode);
		return softwareNode;
	}

	/**
	 * Returns the version of the used software.
	 *
	 * @return The version of the used software.
	 */
	public String getVersion()
	{
		return version;
	}

}
