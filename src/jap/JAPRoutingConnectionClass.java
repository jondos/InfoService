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
package jap;

import org.w3c.dom.Document;
import org.w3c.dom.Element;

import anon.util.XMLUtil;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/**
 * This is the implementation of a structure, which stores the paramters of a connection class,
 * like ISDN, DSL, ...
 */
public class JAPRoutingConnectionClass {
  
  /**
   * Stores an identifier for this connection class. See the constants in
   * JAPRoutingConnectionClassSelector.
   *
   * @see JAPRoutingConnectionClassSelector
   */
  private int m_connectionClassIdentifier;
  
  /**
   * Stores the name of the connection class. This name is displayed in the GUI, so it should be
   * an identifier for JAPMessages.
   */
  private String m_connectionClassName;
  
  /**
   * Stores the maximal possible bandwidth for this connection class in bytes/sec.
   */
  private int m_maximumBandwidth;
  
  /**
   * Stores the bandwidth (bytes/sec), which can be used for forwarding with this connection
   * class.
   */
  private int m_currentBandwidth;
  
  /**
   * Stores the number of connections, which can be forwarded simultaneously by the forwarding
   * server. It's limited by the current available bandwidth for forwarding.
   */
  private int m_simultaneousConnections;
  
  
  /**
   * Creates a new JAPRoutingConnectionClass structure. The number is depending on
   * a_currentBandwidth set to the maximum possible value according to
   * JAPConstants.ROUTING_BANDWIDTH_PER_USER.
   *
   * @param a_connectionClassIdentifier The identifier for this connection class. See the
   *                                    constants in JAPRoutingConnectionClassSelector.
   * @param a_connectionClassName The name for the connection class, which is displayed in the
   *                              GUI. So it should be an identifier for JAPMessages.
   * @param a_maximumBandwidth The maximum possible bandwidth for this connection class in
   *                           bytes/sec.
   * @param a_currentBandwidth The bandwidth which can be used for the forwarding server with
   *                           this connection class.
   */
  public JAPRoutingConnectionClass(int a_connectionClassIdentifier, String a_connectionClassName, int a_maximumBandwidth, int a_currentBandwidth) {
    m_connectionClassIdentifier = a_connectionClassIdentifier;
    m_connectionClassName = a_connectionClassName;
    m_maximumBandwidth = a_maximumBandwidth;
    m_currentBandwidth = a_currentBandwidth;
    m_simultaneousConnections = getMaxSimultaneousConnections();
  }
  
  
  /**
   * Returns the identifier for this connection class. See the constants in
   * JAPRoutingConnectionClassSelector.
   *
   * @return The identifier of this connection class.
   *
   * @see JAPRoutingConnectionClassSelector
   */
  public int getIdentifier() {
    return m_connectionClassIdentifier;
  }
  
  /**
   * Returns the maximum bandwidth of this connection class.
   *
   * @return The maximum bandwidth of this connection class.
   */
  public int getMaximumBandwidth() {
    return m_maximumBandwidth;
  }
  
  /**
   * Returns the current maximum bandwidth, which can be used for the forwarding server with this
   * connection class.
   *
   * @return The current maximum bandwidth in bytes/sec, which can be used for the forwarding
   *         server with this connection class.
   */
  public int getCurrentBandwidth() {
    return m_currentBandwidth;
  }
  
  /**
   * Changes the maximum bandwidth, which can be used for the forwarding server with this
   * connection class.
   *
   * @param a_currentBandwidth The new bandwidth limit (in bytes/sec), which can be used for the
   *                           forwarding server with this connection class.
   */
  public void setCurrentBandwidth(int a_currentBandwidth) {
    synchronized (this) {
      m_currentBandwidth = a_currentBandwidth;
    }
  }
  
  /**
   * Returns the maximum number of simultaneous connections, the forwarding server can handle
   * with the current bandwidth. The value depends on JAPConstants.ROUTING_BANDWIDTH_PER_USER.
   *
   * @return The maximum number of simultaneous forwarded connections, the server can handle with
   *         the current bandwidth.
   */
  public int getMaxSimultaneousConnections() {
    return (getCurrentBandwidth() / JAPConstants.ROUTING_BANDWIDTH_PER_USER);
  }

  /**
   * Returns the number of simultaneous connections, the forwarding server shall handle (if there
   * are enough requesting clients). The forwarding server will accept new forwarding connections
   * until this number is reached. Any further connection request will be dropped.
   *
   * @return The number of simultaneous forwarded connections, the server shall handle.
   */
  public int getSimultaneousConnections() {
    return m_simultaneousConnections;
  }
 
  /**
   * Changes the number of simultaneous connections, the forwarding server shall handle (if there
   * are enough requesting clients). The forwarding server will accept new forwarding connections
   * until this number is reached. Any further connection request will be dropped.
   *
   * @param a_simultaneousConnections The number of simultaneous forwarded connections, the server
   *                                  shall handle.
   */  
  public void setSimultaneousConnections(int a_simultaneousConnections) {
    synchronized (this) {
      m_simultaneousConnections = a_simultaneousConnections;
    }
  }
  
  /**
   * Returns the name of this connection class. If it is an identifier for JAPMessages, the String
   * after resolving the identifier is returned.
   *
   * @return The name of this connection class, which can be used in the GUI.
   */
  public String toString() {
    return JAPMessages.getString(m_connectionClassName);
  }

  /**
   * Returns the settings for this connection class (bandwidth settings) for storage within an XML
   * document.
   *
   * @param a_doc The context document for the connection class settings.
   *
   * @return An XML node (ConnectionClass) with all settings of this connection class.
   */
  public Element getSettingsAsXml(Document a_doc) {
    Element connectionClassNode = a_doc.createElement("ConnectionClass");
    Element classIdentifierNode = a_doc.createElement("ClassIdentifier");
    Element maximumBandwidthNode = a_doc.createElement("MaximumBandwidth");
    Element useableBandwidthNode = a_doc.createElement("UseableBandwidth");
    Element simultaneousConnectionsNode = a_doc.createElement("SimultaneousConnections");
    XMLUtil.setNodeValue(classIdentifierNode, Integer.toString(getIdentifier()));
    XMLUtil.setNodeValue(maximumBandwidthNode, Integer.toString(getMaximumBandwidth()));
    synchronized (this) {
      XMLUtil.setNodeValue(useableBandwidthNode, Integer.toString(getCurrentBandwidth()));
      XMLUtil.setNodeValue(simultaneousConnectionsNode, Integer.toString(getSimultaneousConnections()));
    }
    connectionClassNode.appendChild(classIdentifierNode);
    connectionClassNode.appendChild(maximumBandwidthNode);
    connectionClassNode.appendChild(useableBandwidthNode);
    connectionClassNode.appendChild(simultaneousConnectionsNode);
    return connectionClassNode;
  }
  
  /**
   * This method loads some settings for this connection class from a prior created XML structure.
   * But the identifier and the maximum bandwidth (both belong to the characteristics of this
   * connection class) are never changed, but checked, whether they match to this connection
   * class. If there is an error while loading the settings, it is still tried to load as much
   * settings as possible.
   *
   * @param a_connectionClassNode The ConnectionClass XML node, which was created by the
   *                              getSettingsAsXml() method.
   *
   * @return True, if there was no error while loading the settings and false, if there was one.
   */
  public boolean loadSettingsFromXml(Element a_connectionClassNode) {
    /* store, whether there were some errors while loading the settings */
    boolean noError = true;
    /* check, whether the class identifier and the maximum bandwidth matches, both values are
     * characteristic for this connection class and are not changed
     */
    try {
      if (XMLUtil.parseNodeInt(XMLUtil.getFirstChildByName(a_connectionClassNode, "ClassIdentifier"), m_connectionClassIdentifier + 1) != m_connectionClassIdentifier) {
        throw (new Exception("JAPRoutingConnectionClass: loadSettingsFromXml: The class identifer doesn't match to this class (class: " + Integer.toString(m_connectionClassIdentifier) + ")."));
      }
      if (XMLUtil.parseNodeInt(XMLUtil.getFirstChildByName(a_connectionClassNode, "MaximumBandwidth"), m_maximumBandwidth + 1) != m_maximumBandwidth) {
        throw (new Exception("JAPRoutingConnectionClass: loadSettingsFromXml: The maximum bandwidth doesn't match to this class (class: " + Integer.toString(m_connectionClassIdentifier) + ")."));
      }
    }
    catch (Exception e) {
      LogHolder.log(LogLevel.ERR, LogType.NET, "JAPRoutingConnectionClass: loadSettingsFromXml: Loading the settings for this connection class failed: " + e.toString());
      noError = false;
    }
    if (noError = true) {
      /* only load the settings, if everything is ok */
      synchronized (this) {
        Element useableBandwidthNode = (Element)(XMLUtil.getFirstChildByName(a_connectionClassNode, "UseableBandwidth"));
        if (useableBandwidthNode == null) {
          LogHolder.log(LogLevel.ERR, LogType.MISC, "JAPRoutingConnectionClass: loadSettingsFromXml: Error in XML structure (UseableBandwidth node for class " + Integer.toString(m_connectionClassIdentifier) + "): Using default value.");
          noError = false;
        }
        else {
          int useableBandwidth = XMLUtil.parseNodeInt(useableBandwidthNode, -1);
          if ((useableBandwidth < 0) || (useableBandwidth > getMaximumBandwidth())) {
            LogHolder.log(LogLevel.ERR, LogType.MISC, "JAPRoutingConnectionClass: loadSettingsFromXml: Invalid UseableBandwidth value for class " + Integer.toString(m_connectionClassIdentifier) + ": Using default value.");
            noError = false;
          }
          else {
            setCurrentBandwidth(useableBandwidth);
          }
        }
        Element simultaneousConnectionsNode = (Element)(XMLUtil.getFirstChildByName(a_connectionClassNode, "SimultaneousConnections"));
        if (simultaneousConnectionsNode == null) {
          LogHolder.log(LogLevel.ERR, LogType.MISC, "JAPRoutingConnectionClass: loadSettingsFromXml: Error in XML structure (SimultaneousConnections node for class " + Integer.toString(m_connectionClassIdentifier) + "): Using default value.");
          noError = false;
        }
        else {
          int simultaneousConnections = XMLUtil.parseNodeInt(simultaneousConnectionsNode, -1);
          if ((simultaneousConnections < 0) || (simultaneousConnections > getMaxSimultaneousConnections())) {
            LogHolder.log(LogLevel.ERR, LogType.MISC, "JAPRoutingConnectionClass: loadSettingsFromXml: Invalid SimultaneousConnections value for class " + Integer.toString(m_connectionClassIdentifier) + ": Using default value.");
            noError = false;
          }
          else {
            setSimultaneousConnections(simultaneousConnections);
          }
        }
      }    
    }
    return noError;
  }
  
}