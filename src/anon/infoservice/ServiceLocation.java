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

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * Holds the information of the location of a service.
 */ 
public class ServiceLocation {

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
  public ServiceLocation(Element locationNode) throws Exception {    
    /* get the city */
    NodeList cityNodes = locationNode.getElementsByTagName("City");
    if (cityNodes.getLength() == 0) {
      throw (new Exception("ServiceLocation: Error in XML structure."));
    }
    Element cityNode = (Element)(cityNodes.item(0));
    city = cityNode.getFirstChild().getNodeValue();
    /* get the state */
    NodeList stateNodes = locationNode.getElementsByTagName("State");
    if (stateNodes.getLength() == 0) {
      throw (new Exception("ServiceLocation: Error in XML structure."));
    }
    Element stateNode = (Element)(stateNodes.item(0));
    state = stateNode.getFirstChild().getNodeValue();
    /* get the country */
    NodeList countryNodes = locationNode.getElementsByTagName("Country");
    if (countryNodes.getLength() == 0) {
      throw (new Exception("ServiceLocation: Error in XML structure."));
    }
    Element countryNode = (Element)(countryNodes.item(0));
    country = countryNode.getFirstChild().getNodeValue();
    /* get the longitude / latitude */
    NodeList positionNodes = locationNode.getElementsByTagName("Position");
    if (positionNodes.getLength() == 0) {
      throw (new Exception("ServiceLocation: Error in XML structure."));
    }
    Element positionNode = (Element)(positionNodes.item(0));
    NodeList geoNodes = positionNode.getElementsByTagName("Geo");
    if (geoNodes.getLength() == 0) {
      throw (new Exception("ServiceLocation: Error in XML structure."));
    }
    Element geoNode = (Element)(geoNodes.item(0));
    NodeList longitudeNodes = geoNode.getElementsByTagName("Longitude");
    if (longitudeNodes.getLength() == 0) {
      throw (new Exception("ServiceLocation: Error in XML structure."));
    }
    Element longitudeNode = (Element)(longitudeNodes.item(0));
    longitude = longitudeNode.getFirstChild().getNodeValue();
    NodeList latitudeNodes = geoNode.getElementsByTagName("Latitude");
    if (latitudeNodes.getLength() == 0) {
      throw (new Exception("ServiceLocation: Error in XML structure."));
    }
    Element latitudeNode = (Element)(latitudeNodes.item(0));
    latitude = latitudeNode.getFirstChild().getNodeValue();
  }
  
  /**
   * Returns the city where the service is located.
   *
   * @return The city where the service is located.
   */  
  public String getCity() {
    return city;
  }

  /**
   * Returns the state where the service is located.
   *
   * @return The state where the service is located.
   */  
  public String getState() {
    return state;
  }

  /**
   * Returns the country where the service is located.
   *
   * @return The country where the service is located.
   */  
  public String getCountry() {
    return country;
  }
  
  /**
   * Returns the longitude of the service location. Should be between -180.0 (west of Greenwich)
   * and 180.0 (east of Greenwich).
   *
   * @return The longitude of the service location.
   */
  public String getLongitude() {
    return longitude;
  } 
  
  /**
   * Returns the latitude of the service location. Should be between -90.0 (South Pole) and 90.0
   * (North Pole).
   *
   * @return The latitude of the service location.
   */
  public String getLatitude() {
    return latitude;
  } 
   
}
