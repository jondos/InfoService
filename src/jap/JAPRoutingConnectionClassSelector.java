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

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import anon.util.XMLUtil;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/**
 * This class stores all known connection classes. Also the currently choosen one is stored here.
 */
public class JAPRoutingConnectionClassSelector {
  
  /**
   * This is the identifier of the 1xISDN connection class.
   */
  public static final int CONNECTION_CLASS_ISDN64 = 0;
  
  /**
   * This is the identifier of the 2xISDN connection class.
   */
  public static final int CONNECTION_CLASS_ISDN128 = 1;
  
  /**
   * This is the identifier of the DSL 128 kbit/sec upload connection class.
   */
  public static final int CONNECTION_CLASS_DSL128 = 2;

  /**
   * This is the identifier of the DSL 192 kbit/sec upload connection class.
   */
  public static final int CONNECTION_CLASS_DSL192 = 3;

  /**
   * This is the identifier of the DSL 256 kbit/sec upload connection class.
   */
  public static final int CONNECTION_CLASS_DSL256 = 4;

  /**
   * This is the identifier of the DSL 384 kbit/sec upload connection class.
   */
  public static final int CONNECTION_CLASS_DSL384 = 5;

  /**
   * This is the identifier of the DSL 512 kbit/sec upload connection class.
   */
  public static final int CONNECTION_CLASS_DSL512 = 6;

  /**
   * This is the identifier of the 1 Mbit upload connection class.
   */
  public static final int CONNECTION_CLASS_1MBIT = 7;

  /**
   * This is the identifier of the user-definable connection class.
   */
  public static final int CONNECTION_CLASS_USER = 8;

  
  /**
   * This table stores all connection classes.
   */
  private Hashtable m_connectionClasses;

  /**
   * This stores the identifier of the currently used connection class.
   */
  private int m_currentConnectionClass;
  
  
  /**
   * This creates a new instance of JAPRoutingConnectionClassSelector. Also all connection classes
   * are initialized here and the currently used connection class is set to a default value.
   */
  public JAPRoutingConnectionClassSelector() {
    m_connectionClasses = new Hashtable();
    m_connectionClasses.put(new Integer(CONNECTION_CLASS_ISDN64), new JAPRoutingConnectionClass(CONNECTION_CLASS_ISDN64, "routingConnectionClassIsdn64", 8000, 4000));
    m_connectionClasses.put(new Integer(CONNECTION_CLASS_ISDN128), new JAPRoutingConnectionClass(CONNECTION_CLASS_ISDN128, "routingConnectionClassIsdn128", 16000, 8000));
    m_connectionClasses.put(new Integer(CONNECTION_CLASS_DSL128), new JAPRoutingConnectionClass(CONNECTION_CLASS_DSL128, "routingConnectionClassDsl128", 16000, 8000));
    m_connectionClasses.put(new Integer(CONNECTION_CLASS_DSL192), new JAPRoutingConnectionClass(CONNECTION_CLASS_DSL192, "routingConnectionClassDsl192", 24000, 12000));
    m_connectionClasses.put(new Integer(CONNECTION_CLASS_DSL256), new JAPRoutingConnectionClass(CONNECTION_CLASS_DSL256, "routingConnectionClassDsl256", 32000, 16000));
    m_connectionClasses.put(new Integer(CONNECTION_CLASS_DSL384), new JAPRoutingConnectionClass(CONNECTION_CLASS_DSL384, "routingConnectionClassDsl384", 48000, 24000));
    m_connectionClasses.put(new Integer(CONNECTION_CLASS_DSL512), new JAPRoutingConnectionClass(CONNECTION_CLASS_DSL512, "routingConnectionClassDsl512", 64000, 32000));
    m_connectionClasses.put(new Integer(CONNECTION_CLASS_1MBIT), new JAPRoutingConnectionClass(CONNECTION_CLASS_1MBIT, "routingConnectionClass1Mbit", 125000, 62500));
    /* don't call setCurrentConnectionClass() here, because this constructor is called by the
     * constructor of JAPRoutingSettings, so JAPModel.getModel().getRoutingSettings()
     * does not work, when this constructor is called - nevertheless JAPRoutingSettings will get
     * the current connection class automatically, because it will explicitly ask for it
     */
    m_currentConnectionClass = CONNECTION_CLASS_DSL128;
  }

  
  /**
   * This changes the values of the user-defined connection class. Attention, this method only
   * changes the values of the class, if you want to use them in the routing system, you have
   * to call setCurrentConnectionClass explicitly. The user-defined connection class will only
   * occur in the list of all connection classes, after this method was called at least once.
   *
   * @param a_maxBandwidth The maximum usable bandwidth (bytes /sec) of the user-defined
   *                       connection class.
   * @param a_currentBandwidth The maximum bandwidth (bytes / sec) to use for the forwarding
   *                           server. If this bandwidth is bigger than the maximum bandwidht
   *                           for this connection class, it is lowered to that maximum value.
   */
  public void changeUserDefinedClass(int a_maxBandwidth, int a_currentBandwidth) {
    if (a_currentBandwidth > a_maxBandwidth) {
      a_currentBandwidth = a_maxBandwidth;
    }
    synchronized (m_connectionClasses) {
      m_connectionClasses.put(new Integer(CONNECTION_CLASS_USER), new JAPRoutingConnectionClass(CONNECTION_CLASS_USER, "routingConnectionClassUser", a_maxBandwidth, a_currentBandwidth));
    }
  }
  
  /**
   * This returns the currently used connection class.
   *
   * @return The currently used connection class.
   */
  public JAPRoutingConnectionClass getCurrentConnectionClass() {
    JAPRoutingConnectionClass returnValue = null;
    synchronized (m_connectionClasses) {
      returnValue = (JAPRoutingConnectionClass)(m_connectionClasses.get(new Integer(m_currentConnectionClass)));
    }
    return returnValue;
  }
  
  /**
   * This changes the currently used connection class. Also the forwarding system is updated to
   * the bandwidth values of the new connection class, if you have specified a valid ID.
   *
   * @param a_connectionClass The ID of the new connection class. If this is not a valid ID,
   *                          nothing is done.
   */
  public void setCurrentConnectionClass(int a_connectionClass) {
    JAPRoutingConnectionClass newConnectionClass = null;
    synchronized (m_connectionClasses) {    
      newConnectionClass = (JAPRoutingConnectionClass)(m_connectionClasses.get(new Integer(a_connectionClass)));
      if (newConnectionClass != null) {
        /* the specified connection class exists */     
        m_currentConnectionClass = a_connectionClass;
        JAPModel.getInstance().getRoutingSettings().setMaxBandwidth(newConnectionClass.getMaximumBandwidth());
        JAPModel.getInstance().getRoutingSettings().setBandwidth(newConnectionClass.getCurrentBandwidth());
        JAPModel.getInstance().getRoutingSettings().setAllowedConnections(newConnectionClass.getSimultaneousConnections());
      }
    }
  }
  
  /**
   * Returns a Vector of all connection classes.
   *
   * @return The Vector with all connection classes.
   */ 
  public Vector getConnectionClasses() {
    Vector returnValue = new Vector();
    synchronized (m_connectionClasses) {
      Enumeration connectionClasses = m_connectionClasses.elements();
      while (connectionClasses.hasMoreElements()) {
        returnValue.addElement(connectionClasses.nextElement());
      }
    }
    return returnValue;
  }
  
  /**
   * Returns the current connection class settings (currently selected connection class, settings
   * of the single connection classes) for storage within an XML document.
   *
   * @param a_doc The context document for the connection class settings.
   *
   * @return An XML node (ConnectionClassSettings) with all connection class settings.
   */
  public Element getSettingsAsXml(Document a_doc) {
    Element connectionClassSettingsNode = a_doc.createElement("ConnectionClassSettings");
    Element connectionClassesNode = a_doc.createElement("ConnectionClasses");
    Element currentConnectionClassNode = a_doc.createElement("CurrentConnectionClass");
    synchronized (m_connectionClasses) {
      Enumeration connectionClasses = getConnectionClasses().elements();
      while (connectionClasses.hasMoreElements()) {
        connectionClassesNode.appendChild(((JAPRoutingConnectionClass)(connectionClasses.nextElement())).getSettingsAsXml(a_doc));
      }
      XMLUtil.setNodeValue(currentConnectionClassNode, Integer.toString(m_currentConnectionClass));
    }
    connectionClassSettingsNode.appendChild(connectionClassesNode);
    connectionClassSettingsNode.appendChild(currentConnectionClassNode);
    return connectionClassSettingsNode;
  }
  
  /**
   * This method loads all connection classes related settings from a prior created XML structure.
   * If there is an error while loading the settings, it is still tried to load as much settings
   * as possible.
   *
   * @param a_connectionClassSettingsNode The ConnectionClasses XML node, which was created by
   *                                      the getSettingsAsXml() method.
   *
   * @return True, if there was no error while loading the settings and false, if there was one.
   */
  public boolean loadSettingsFromXml(Element a_connectionClassSettingsNode) {
    /* store, whether there were some errors while loading the settings */
    boolean noError = true;
    /* get the ForwardingServer settings */
    Element connectionClassesNode = (Element)(XMLUtil.getFirstChildByName(a_connectionClassSettingsNode, "ConnectionClasses"));
    if (connectionClassesNode == null) {
      LogHolder.log(LogLevel.ERR, LogType.MISC, "JAPRoutingConnectionClassSelector: loadSettingsFromXml: Error in XML structure (ConnectionClasses node): Using default connection classes.");
      noError = false;
    }
    else {
      /* load the single connection classes, if there are some */
      NodeList connectionClassNodes = connectionClassesNode.getElementsByTagName("ConnectionClass");
      for (int i = 0; i < connectionClassNodes.getLength(); i++) {
        Element connectionClassNode = (Element) (connectionClassNodes.item(i));
        /* get the identifier */
        Element classIdentifierNode = (Element)(XMLUtil.getFirstChildByName(connectionClassNode, "ClassIdentifier"));
        if (classIdentifierNode == null) {
          LogHolder.log(LogLevel.ERR, LogType.MISC, "JAPRoutingConnectionClassSelector: loadSettingsFromXml: Error in XML structure (ClassIdentifier node): Skipping this connection class.");
          noError = false;
        }
        else {        
          try {
            /* XMLUtil.parseNodeInt() is not used, because we want to know, whether there is an error */
            int classIdentifier = Integer.parseInt(XMLUtil.parseNodeString(classIdentifierNode, "NOT_A_NUMBER"));
            JAPRoutingConnectionClass currentConnectionClass = null;
            synchronized (m_connectionClasses) {
              currentConnectionClass = (JAPRoutingConnectionClass)(m_connectionClasses.get(new Integer(classIdentifier)));
            }
            if ((currentConnectionClass != null) && (classIdentifier != CONNECTION_CLASS_USER)) {
              /* load the class settings */
              noError = currentConnectionClass.loadSettingsFromXml(connectionClassNode);
            }
            else {
              /* maybe it's the user-defined class, which doesn't exist yet */
              if (classIdentifier == CONNECTION_CLASS_USER) {
                /* load all values */
                Element maximumBandwidthNode = (Element)(XMLUtil.getFirstChildByName(connectionClassNode, "MaximumBandwidth"));
                if (maximumBandwidthNode == null) {
                  LogHolder.log(LogLevel.ERR, LogType.MISC, "JAPRoutingConnectionClassSelector: loadSettingsFromXml: Error in XML structure (MaximumBandwidth node for user-defined connection class " + Integer.toString(classIdentifier) + "): Skipping this class.");
                  noError = false;
                }
                else {
                  int maximumBandwidth = XMLUtil.parseNodeInt(maximumBandwidthNode, -1);
                  if (maximumBandwidth < 0) {
                    LogHolder.log(LogLevel.ERR, LogType.MISC, "JAPRoutingConnectionClassSelector: loadSettingsFromXml: Invalid MaximumBandwidth value for the user-defined connection class " + Integer.toString(classIdentifier) + "): Skipping this class.");
                    noError = false;
                  }
                  else {
                    Element useableBandwidthNode = (Element)(XMLUtil.getFirstChildByName(connectionClassNode, "UseableBandwidth"));
                    if (useableBandwidthNode == null) {
                      LogHolder.log(LogLevel.ERR, LogType.MISC, "JAPRoutingConnectionClassSelector: loadSettingsFromXml: Error in XML structure (UseableBandwidth node for user-defined connection class " + Integer.toString(classIdentifier) + "): Skipping this class.");
                      noError = false;
                    }
                    else {
                      int useableBandwidth = XMLUtil.parseNodeInt(useableBandwidthNode, -1);
                      if ((useableBandwidth < 0) || (useableBandwidth > maximumBandwidth)) {
                        LogHolder.log(LogLevel.ERR, LogType.MISC, "JAPRoutingConnectionClassSelector: loadSettingsFromXml: Invalid UseableBandwidth value for the user-defined connection class " + Integer.toString(classIdentifier) + ": Skipping this class.");
                        noError = false;
                      }
                      else {
                        changeUserDefinedClass(maximumBandwidth, useableBandwidth);
                      }
                    }
                  }
                }
              }
              else {
                /* the class wasn't found and it is also not the user-defined class */
                LogHolder.log(LogLevel.ERR, LogType.MISC, "JAPRoutingConnectionClassSelector: loadSettingsFromXml: The connection class " + Integer.toString(classIdentifier) + " is not known in the system. Skipping the entry.");
                noError = false;
              }
            }
          }
          catch (Exception e) {
            LogHolder.log(LogLevel.ERR, LogType.MISC, "JAPRoutingConnectionClassSelector: loadSettingsFromXml: Error while loading settings for a connection class. Skipping this class. (" + e.toString() + ")");
            noError = false;
          }
        }
      }
    }
    /* get the CurrentConnectionClass setting */
    Element currentConnectionClassNode = (Element)(XMLUtil.getFirstChildByName(a_connectionClassSettingsNode, "CurrentConnectionClass"));
    if (currentConnectionClassNode == null) {
      LogHolder.log(LogLevel.ERR, LogType.MISC, "JAPRoutingConnectionClassSelector: loadSettingsFromXml: Error in XML structure (CurrentConnectionClass node): Using default value.");
      noError = false;
    }
    else {
      try {
        /* XMLUtil.parseNodeInt() is not used, because we want to know, whether there is an error */
        int currentConnectionClass = Integer.parseInt(XMLUtil.parseNodeString(currentConnectionClassNode, "NOT_A_NUMBER"));
        synchronized (m_connectionClasses) {
          if (m_connectionClasses.get(new Integer(currentConnectionClass)) != null) {
            /* connection class exists -> everything ok */
            setCurrentConnectionClass(currentConnectionClass);
          }
          else {
            /* the specified connection class doesn't exist, use the default value for updating
             * the forwarding settings
             */
            setCurrentConnectionClass(m_currentConnectionClass);
            LogHolder.log(LogLevel.ERR, LogType.MISC, "JAPRoutingConnectionClassSelector: loadSettingsFromXml: The specified current connection class doesn't exist: Using default value.");
            noError = false;
          }  
        }       
      }
      catch (Exception e) {
        /* the invaild value for the current connection class, use the default value for updating
         * the forwarding settings
         */
        setCurrentConnectionClass(m_currentConnectionClass);
        LogHolder.log(LogLevel.ERR, LogType.MISC, "JAPRoutingConnectionClassSelector: loadSettingsFromXml: Invalid value of the current connection class setting: Using default value. (" + e.toString() + ")");
        noError = false;
      }
    }
    return noError;
  }   
     
}