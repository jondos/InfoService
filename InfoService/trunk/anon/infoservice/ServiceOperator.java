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
 * Holds the information about the operator of a service.
 */ 
public class ServiceOperator {

  /**
   * This is the name of the operator or organisation.
   */
  private String organisation;

  /**
   * This is the URL of the operators homepage.
   */
  private String url;
   
  /**
   * Creates a new ServiceOperator from XML description (Operator node).
   *
   * @param operatorNode The Operator node from an XML document.
   */      
  public ServiceOperator(Element operatorNode) throws Exception {    
    /* get the organisation name */
    NodeList organisationNodes = operatorNode.getElementsByTagName("Organisation");
    if (organisationNodes.getLength() == 0) {
      throw (new Exception("ServiceOperator: Error in XML structure."));
    }
    Element organisationNode = (Element)(organisationNodes.item(0));
    organisation = organisationNode.getFirstChild().getNodeValue();
    /* get the homepage url */
    NodeList urlNodes = operatorNode.getElementsByTagName("URL");
    if (urlNodes.getLength() == 0) {
      throw (new Exception("ServiceOperator: Error in XML structure."));
    }
    Element urlNode = (Element)(urlNodes.item(0));
    url = urlNode.getFirstChild().getNodeValue();
  }
  
  /**
   * Returns the name of the operator or organisation.
   *
   * @return The name of the operator or organisation.
   */  
  public String getOrganisation() {
    return organisation;
  }

  /**
   * Returns the URL of the operators homepage.
   *
   * @return The URL of the operators homepage.
   */  
  public String getUrl() {
    return url;
  }
   
}
