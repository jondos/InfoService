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
   * Creates a new JAPRoutingConnectionClass structure.
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
    m_currentBandwidth = a_currentBandwidth;
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
  
}