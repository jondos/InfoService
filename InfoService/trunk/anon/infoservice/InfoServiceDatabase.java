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

import java.util.Vector;
import java.util.Enumeration;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

/**
 * This is the implementation for the database of all infoservices.
 */
public class InfoServiceDatabase extends Database {

  /**
   * Stores the instance of InfoServiceDatabase (Singleton).
   */
  private static InfoServiceDatabase isdbInstance = null;


  /**
   * Returns the instance of InfoServiceDatabase (Singleton). If there is no instance,
   * there is a new one created. Also the included thread is been started.
   *
   * @return The InfoServiceDatabase instance.
   */
  public static InfoServiceDatabase getInstance() {
    if (isdbInstance == null) {
      isdbInstance = new InfoServiceDatabase();
      Thread isdbThread = new Thread(isdbInstance);
      isdbThread.setDaemon(true);
      isdbThread.start();
    }
    return isdbInstance;
  }


  /**
   * Creates a new instance of an InfoServiceDatabase.
   */
  private InfoServiceDatabase() {
    super();
  }

  /**
   * Updates an infoservice entry in the database. If the entry is an unknown or if it is newer
   * then the one stored in the database for this infoservice, the new entry is stored in the
   * database.
   *
   * @param newEntry The InfoService to update.
   */
  public void update(InfoService newEntry) {
    super.update(newEntry);
  }

  /**
   * Removes an infoservice from the database.
   *
   * @deleteEntry The infoservice to remove. If it is not in the database, nothing is done.
   */
  public void remove(InfoService deleteEntry) {
    super.remove(deleteEntry);
  }

  /**
   * Removes all infoservices from the database.
   */
  public void removeAll() {
    super.removeAll();
  }

  /**
   * Returns a snapshot of all infoservices we know.
   *
   * @return The Vector of all infoservices.
   */
  public Vector getInfoServiceList() {
    return getEntryList();
  }

  /**
   * Creates an XML node (InfoServices node) with all infoservices from the database inside.
   *
   * @param doc The XML document, which is the environment for the created XML node.
   *
   * @return The InfoServices XML node.
   */
  public Element toXmlNode(Document doc) {
    Element infoServicesNode = doc.createElement("InfoServices");
    Vector infoServices = getInfoServiceList();
    Enumeration it = infoServices.elements();
    while (it.hasMoreElements()) {
      infoServicesNode.appendChild(((InfoService)(it.nextElement())).toXmlNode(doc));
    }
    return infoServicesNode;
  }

  /**
   * Adds all infoservices, which are childs of the InfoServices node, to the database.
   *
   * @param infoServicesNode The InfoServices node.
   */
  public void loadFromXml(Element infoServicesNode) {
    NodeList infoServiceNodes = infoServicesNode.getElementsByTagName("InfoService");
    for (int i = 0; i < infoServiceNodes.getLength(); i++) {
      /* add all childs to the database */
      try {
        update(new InfoService((Element)(infoServiceNodes.item(i))));
      }
      catch (Exception e) {
        /* if there was an error, it does not matter */
      }
    }
  }

}
