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

import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Holds the information of one single mix.
 */
public class MixInfo
{

	/**
	 * This is the ID of the mix.
	 */
	private String mixId;

	/**
	 * Time (see System.currentTimeMillis()) when the mix has sent this HELO message.
	 */
	private long lastUpdate;

	/**
	 * The name of the mix.
	 */
	private String name;

	/**
	 * Some information about the location of the mix.
	 */
	private ServiceLocation mixLocation;

	/**
	 * Some information about the operator of the mix.
	 */
	private ServiceOperator mixOperator;

	/**
	 * Some information about the used mix software.
	 */
	private ServiceSoftware mixSoftware;

	/**
	 * Creates a new MixInfo from XML description (Mix node).
	 *
	 * @param mixNode The Mix node from an XML document.
	 */
	public MixInfo(Element mixNode) throws Exception
	{
		/* get the ID */
		mixId = mixNode.getAttribute("id");
		/* get the name */
		NodeList nameNodes = mixNode.getElementsByTagName("Name");
		if (nameNodes.getLength() == 0)
		{
			throw (new Exception("MixInfo: Error in XML structure."));
		}
		Element nameNode = (Element) (nameNodes.item(0));
		name = nameNode.getFirstChild().getNodeValue();
		/* get the location */
		NodeList locationNodes = mixNode.getElementsByTagName("Location");
		if (locationNodes.getLength() == 0)
		{
			throw (new Exception("MixInfo: Error in XML structure."));
		}
		Element locationNode = (Element) (locationNodes.item(0));
		mixLocation = new ServiceLocation(locationNode);
		/* get the operator */
		NodeList operatorNodes = mixNode.getElementsByTagName("Operator");
		if (operatorNodes.getLength() == 0)
		{
			throw (new Exception("MixInfo: Error in XML structure."));
		}
		Element operatorNode = (Element) (operatorNodes.item(0));
		mixOperator = new ServiceOperator(operatorNode);
		/* get the software information */
		NodeList softwareNodes = mixNode.getElementsByTagName("Software");
		if (softwareNodes.getLength() == 0)
		{
			throw (new Exception("MixInfo: Error in XML structure."));
		}
		Element softwareNode = (Element) (softwareNodes.item(0));
		mixSoftware = new ServiceSoftware(softwareNode);
		/* get LastUpdate information */
		NodeList lastUpdateNodes = mixNode.getElementsByTagName("LastUpdate");
		if (lastUpdateNodes.getLength() == 0)
		{
			throw (new Exception("MixInfo: Error in XML structure."));
		}
		Element lastUpdateNode = (Element) (lastUpdateNodes.item(0));
		lastUpdate = Long.parseLong(lastUpdateNode.getFirstChild().getNodeValue());
	}

	/**
	 * Returns the ID of the mix.
	 *
	 * @return The ID of this mix.
	 */
	public String getId()
	{
		return mixId;
	}

	/**
	 * Returns the time (see System.currentTimeMillis()), when the mix has sent this MixInfo to an
	 * InfoService.
	 *
	 * @return The send time of this MixInfo from the mix.
	 *
	 */
	public long getLastUpdate()
	{
		return lastUpdate;
	}

	/**
	 * Returns the name of the mix.
	 *
	 * @return The name of this mix.
	 */
	public String getName()
	{
		return name;
	}

	/**
	 * Returns the location of the mix.
	 *
	 * @return The location information for this mix.
	 */
	public ServiceLocation getServiceLocation()
	{
		return mixLocation;
	}

	/**
	 * Returns information about the operator of this mix.
	 *
	 * @return The operator information for this mix.
	 */
	public ServiceOperator getServiceOperator()
	{
		return mixOperator;
	}

	/**
	 * Returns information about the used software in this mix.
	 *
	 * @return The software information for this mix.
	 */
	public ServiceSoftware getServiceSoftware()
	{
		return mixSoftware;
	}

}
