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
     * the current connection class automatically, because it will explicitly ask for in the
     * constructor
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
        JAPModel.getModel().getRoutingSettings().setMaxBandwidth(newConnectionClass.getMaximumBandwidth());
        JAPModel.getModel().getRoutingSettings().setBandwidth(newConnectionClass.getCurrentBandwidth());
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
      
}