/*
 Copyright (c) 2000 - 2003, The JAP-Team
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
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import anon.util.XMLUtil;

/**
 * Holds the information of the location of a service.
 */
public class ServiceLocation
{

	/**
	 * This is the city where the service is located.
	 */
	private String city;

	/**
	 * This is the state where the service is located.
	 */
	private String state;

	/**
	 * This is the country where the service is located.
	 */
	private String country;

	/**
	 * This is the longitude of the service location. Should be between -180.0 (west of Greenwich)
	 * and 180.0 (east of Greenwich).
	 */
	private String longitude;

	/**
	 * This is the latitude of the service location. Should be between -90.0 (South Pole) and 90.0
	 * (North Pole).
	 */
	private String latitude;

	/**
	 * Creates a new ServiceLocation from XML description (Location node).
	 *
	 * @param locationNode The Location node from an XML document.
	 */
	public ServiceLocation(Element locationNode) throws Exception
	{
		Node node;
	
		/* get the city */
		node = XMLUtil.getFirstChildByName(locationNode, "City");
		city = XMLUtil.parseNodeString(node, "");

		/* get the state */
		node = XMLUtil.getFirstChildByName(locationNode, "State");
		state = XMLUtil.parseNodeString(node, "");
		
		/* get the country */
		node = XMLUtil.getFirstChildByName(locationNode, "Country");
		country = XMLUtil.parseNodeString(node, "");

		/* get the longitude / latitude */
		Node positionNode = XMLUtil.getFirstChildByName(locationNode, "Position");
		positionNode = XMLUtil.getFirstChildByName(positionNode, "Geo");
		node = XMLUtil.getFirstChildByName(positionNode, "Longitude");
		longitude = XMLUtil.parseNodeString(node, "");
		node = XMLUtil.getFirstChildByName(positionNode, "Latitude");
		latitude = XMLUtil.parseNodeString(node, "");
	}

	/**
	 * Returns the city where the service is located.
	 *
	 * @return The city where the service is located.
	 */
	public String getCity()
	{
		return city;
	}

	/**
	 * Returns the state where the service is located.
	 *
	 * @return The state where the service is located.
	 */
	public String getState()
	{
		return state;
	}

	/**
	 * Returns the country where the service is located.
	 *
	 * @return The country where the service is located.
	 */
	public String getCountry()
	{
		return country;
	}

	/**
	 * Returns the longitude of the service location. Should be between -180.0 (west of Greenwich)
	 * and 180.0 (east of Greenwich).
	 *
	 * @return The longitude of the service location.
	 */
	public String getLongitude()
	{
		return longitude;
	}

	/**
	 * Returns the latitude of the service location. Should be between -90.0 (South Pole) and 90.0
	 * (North Pole).
	 *
	 * @return The latitude of the service location.
	 */
	public String getLatitude()
	{
		return latitude;
	}

}
