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
package forward;

import java.net.Socket;

import anon.server.AnonServiceImpl;
import anon.server.impl.ProxyConnection;


/**
 * This is the implementation of some helper methods for the forwarding client and server. This
 * class is a singleton.
 */
public class ForwardUtils {

  /**
   * Stores the instance of ForwardUtils (Singleton).
   */
  private static ForwardUtils ms_fuInstance = null;


  /**
   * Stores the type of the current firewall. See the constants in anon.server.AnonServiceImpl.
   * The firewall proxy server is used for all new outgoing connections.
   * @see anon.server.AnonServiceImpl
   */
  private int m_firewallType;
  
  /**
   * Stores the hostname or IP address of the current firewall. The firewall / proxy server is used
   * for all new outgoing connections.
   */
  private String m_firewallHost;
  
  /**
   * Stores the port number of the current firewall. The firewall / proxy server is used for all
   * new outgoing connections.
   */
  private int m_firewallPort;
  
  /**
   * Stores the username, if the firewall / proxy server requires authentication.
   */
  private String m_firewallUserName;
  
  /**
   * Stores the password, if the firewall / proxy server requires authentication.
   */
  private String m_firewallPassword;


  /**
   * Returns the instance of ForwardUtils (Singleton). If there is no instance, there is a new
   * one created.
   *
   * @return The ForwardUtils instance.
   */
  public static ForwardUtils getInstance() {
    if (ms_fuInstance == null) {
      ms_fuInstance = new ForwardUtils();
    }
    return ms_fuInstance;
  }


  /**
   * This creates a new instance of ForwardUtils with disabled proxy settings.
   */
  private ForwardUtils() {
    m_firewallType = AnonServiceImpl.FIREWALL_TYPE_NONE;
  }


  /**
   * This changes the proxy settings for all new forwarding connections. Currently active
   * forwarding connections are not concerned.
   *
   * @param a_fwType The type of the proxy server, see anon.server.AnonServiceImpl.
   * @param a_fwHost The hostname of the proxy server.
   * @param a_fwPort The port number of the proxy server.
   * @param a_fwUserName The user name, if the proxy server requires authentication.
   * @param a_fwPassword The password, if the proxy server requires authentication.
   */
  public void setProxySettings(int a_fwType, String a_fwHost, int a_fwPort, String a_fwUserName, String a_fwPassword) {
    synchronized (this) {
      /* so the proxy data is always consistent */
      m_firewallType = a_fwType;
      m_firewallHost = a_fwHost;
      m_firewallPort = a_fwPort;
      m_firewallUserName = a_fwUserName;
      m_firewallPassword = a_fwPassword;
    }
  }

  /**
   * Creates a new connection to the specified target using the current firewall settings.
   *
   * @param a_host The hostname or IP address of the target.
   * @param a_port The port number of the connection target.
   *
   * @return The new connection or null, if there was an error connecting to that target.
   */
  public ProxyConnection createProxyConnection(String a_host, int a_port) {
    ProxyConnection proxyConnection = null;
    try {
      synchronized (this) {
        /* get consistent proxy server data */
        proxyConnection = new ProxyConnection(m_firewallType, m_firewallHost, m_firewallPort, m_firewallUserName, m_firewallPassword, a_host, a_port);
      }
    }
    catch (Exception e) {
    }
    return proxyConnection;
  }

  /**
   * Creates a new connection to the specified target using the current firewall settings.
   *
   * @param a_host The hostname or IP address of the target.
   * @param a_port The port number of the connection target.
   *
   * @return The new connection or null, if there was an error connecting to that target.
   */
  public Socket createConnection(String a_host, int a_port) {
    ProxyConnection proxyConnection = createProxyConnection(a_host, a_port);
    Socket newSocket = null;
    if (proxyConnection != null) {
      newSocket = proxyConnection.getSocket();
    }
    return newSocket;
  }
  
}