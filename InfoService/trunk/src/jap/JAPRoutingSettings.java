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

import java.net.ServerSocket;
import java.util.Enumeration;
import java.util.Observable;
import java.util.Vector;
import anon.crypto.JAPCertificateStore;
import anon.infoservice.HTTPConnectionFactory;
import anon.infoservice.ListenerInterface;
import anon.infoservice.InfoService;
import anon.infoservice.MixCascade;
import anon.server.AnonServiceImpl;
import anon.server.impl.ProxyConnection;
import forward.ForwardUtils;
import forward.JAPThread;
import forward.client.ClientForwardException;
import forward.client.DefaultClientProtocolHandler;
import forward.client.ForwardConnectionDescriptor;
import forward.server.ForwardSchedulerStatistics;
import forward.server.ForwardServerManager;
import forward.server.ServerSocketPropagandist;
import proxy.AnonWebProxy;

/**
 * This class stores all routing settings. Observers of this class are notified, if the settings
 * are changed. They will get an instance of JapRoutingMessage with the detailed message of the
 * notification.
 */
public class JAPRoutingSettings extends Observable
{

  /**
   * In this mode routing is disabled.
   */
  public static final int ROUTING_MODE_DISABLED = 0;

  /**
   * In this mode, we are a client and use a forwarder to connect to the JAP servers.
   */
  public static final int ROUTING_MODE_CLIENT = 1;

  /**
   * This is the mode, when we are a server and provide forwarding for other clients.
   */
  public static final int ROUTING_MODE_SERVER = 2;

  /**
   * This stores the current routing mode. See the constants in this class.
   */
  private int m_routingMode;

  /**
   * Stores the port, where the local forwarding server will listen for clients.
   */
  private int m_serverPort;

  /**
   * Stores the maximum bandwidth, which can be provided for forwarding. The current bandwidth
   * for forwarding can be smaller than this value.
   */
  private int m_maxBandwidth;

  /**
   * Stores the current bandwidth, which is provided for forwarding.
   */
  private int m_bandwidth;

  /**
   * Stores the maximum number of simultaneously forwarded connections.
   */
  private int m_connections;

  /**
   * Stores the forwarded client connection.
   */
  private ProxyConnection m_forwardedConnection;

  /**
   * Stores the hostname/IP of the current forwarder.
   */
  private String m_forwarderHost;

  /**
   * Stores the port of the current forwarder.
   */
  private int m_forwarderPort;

  /**
   * Stores, whether connections to the infoservice needs also forwarding.
   */
  private boolean m_forwardInfoService;

  /**
   * Needed for synchronization if we open a new forwarded connection and have to shutdown an old
   * one.
   */
  private boolean m_waitForShutdownCall;

  /**
   * Stores the protocol handling instance for the forwarded client connection.
   */
  private DefaultClientProtocolHandler m_protocolHandler;

  /**
   * Stores the minimum dummy traffic rate, needed by the forwarded on the current forwarded
   * client connection.
   */
  private int m_maxDummyTrafficInterval;

  /**
   * Stores a list of infoservices (with a forwarder list), where the local forwarding server
   * shall be registered.
   */
  private Vector m_infoServiceRegistration;

  /**
   * Stores a list of ServerSocketPropagandists, which are currently running.
   */
  private Vector m_runningPropagandists;
  
  /**
   * Stores the instance of the startPropaganda Thread, if there is a currently running instance
   * (else this value is null).
   */
  private Thread m_startPropagandaThread;


  /**
   * This creates a new instance of JAPRoutingSettings. We are doing some initialization here.
   */
  public JAPRoutingSettings()
  {
    m_routingMode = ROUTING_MODE_DISABLED;
    /* random value for the forwarding server port between 1025 and 65535 */
    m_serverPort = ( (int) (Math.round(Math.abs(Math.random() *
      ( (double) 65535 - (double) 1025 + (double) 1))))) + 1025;
    /* set default values for bandwidth, ... */
    m_bandwidth = 4096;
    m_maxBandwidth = 16384;
    m_connections = m_bandwidth / JAPConstants.ROUTING_BANDWIDTH_PER_USER;
    m_forwardedConnection = null;
    m_forwardInfoService = false;
    m_forwarderHost = null;
    m_forwarderPort = -1;
    m_waitForShutdownCall = false;
    m_protocolHandler = null;
    m_maxDummyTrafficInterval = -1;
    m_infoServiceRegistration = new Vector();
    m_runningPropagandists = new Vector();
    m_startPropagandaThread = null;
  }

  /**
   * Returns the current routing mode, see the constants in this class.
   *
   * @return The current routing mode.
   */
  public int getRoutingMode()
  {
    return m_routingMode;
  }

  /**
   * Changes the routing mode. This method also does everything necessary to change the routing
   * mode, like starting or shutting down the forwarding server, infoservice registrations or a
   * forwarded connection. Attention: The infoservice registration services are not started
   * automatically with the server. You have to call startPropaganda() explicitly.
   *
   * @param a_routingMode The new routing mode. See the constants in this class.
   *
   * @return True, if the change of the routing mode was successful, else false.
   */
  public boolean setRoutingMode(int a_routingMode)
  {
    boolean success = false;
    boolean notifyObservers = false;
    synchronized (this)
    {
      if (a_routingMode != m_routingMode)
      {
        if (m_routingMode == ROUTING_MODE_SERVER)
        {
          /* server was running, shut it down */
          ForwardServerManager.getInstance().shutdownForwarding();
          /* stop the propaganda */
          stopPropaganda();
        }
        if (m_routingMode == ROUTING_MODE_CLIENT)
        {
          if (getForwardInfoService() == true)
          {
            /* restore the original proxy settings for the infoservice */
            JAPController.getController().applyProxySettingsToInfoService();
          }
          JAPController.getController().setAnonMode(false);
          /* client was running, close the connection */
          m_forwardedConnection.close();
          m_forwardedConnection = null;
          m_protocolHandler = null;
        }
        m_routingMode = a_routingMode;
        if (a_routingMode == ROUTING_MODE_SERVER)
        {
          /* the server shall be started */
          ForwardServerManager.getInstance().startForwarding();
          ForwardServerManager.getInstance().setNetBandwidth(getBandwidth());
          ForwardServerManager.getInstance().setMaximumNumberOfConnections(getAllowedConnections());
          if (ForwardServerManager.getInstance().addListenSocket(getServerPort()) == false)
          {
            /* error while binding the socket -> shutdown the server */
            ForwardServerManager.getInstance().shutdownForwarding();
            m_routingMode = ROUTING_MODE_DISABLED;
          }
          else
          {
            /* everything ok */
            success = true;
          }
        }
        if (a_routingMode == ROUTING_MODE_CLIENT)
        {
          /* close an existing anon connection, if there is one */
          if (JAPController.getController().getAnonMode() == true)
          {
            m_waitForShutdownCall = true;
            JAPController.getController().setAnonMode(false);
            try
            {
              this.wait();
            }
            catch (Exception e)
            {
            }
            /* the shutdown from the existing connection is done */
            m_waitForShutdownCall = false;
          }
          /* try to connect to a forwarder */
          m_forwardedConnection = ForwardUtils.getInstance().createProxyConnection(m_forwarderHost,
            m_forwarderPort);
          if (m_forwardedConnection != null)
          {
            /* update the infoservice proxy settings, if it needs forwarding too */
            updateInfoServiceProxySettings();
            m_protocolHandler = new DefaultClientProtocolHandler(m_forwardedConnection);
            success = true;
          }
          else
          {
            /* there was a connection error */
            m_routingMode = ROUTING_MODE_DISABLED;
          }
        }
        if (a_routingMode == ROUTING_MODE_DISABLED)
        {
          /* nothing to do */
          success = true;
        }
        /* the routing mode was changed (maybe without success), notify observers */
        notifyObservers = true;
      }
      else
      {
        /* nothing to change */
        success = true;
      }
      if (notifyObservers == true)
      {
        setChanged();
        notifyObservers(new JAPRoutingMessage(JAPRoutingMessage.ROUTING_MODE_CHANGED));
      }
    }
    return success;
  }

  /**
   * Returns the currently configured forwarding server port.
   *
   * @return The currently configured forwarding server port.
   */
  public int getServerPort()
  {
    return m_serverPort;
  }

  /**
   * Configures the forwarding server port. If the forwarding server is currently running, we will
   * open the new port and the old one is closed. If there is an error while setting the new port,
   * nothing is changed, the old port keeps active. Attention: Any active client connection are not
   * influenced by the change of the forwarding port. They keep connected to the old port even
   * though the old port is closed. If the forwarding server isn't running at the moment, only the
   * configuration for the server port is changed.
   *
   * @param a_serverPort The port number for the forwarding server. The value must be between 1 and
   *                     65535.
   *
   * @return True, if the change of the port was successful or false, if not.
   */
  public boolean setServerPort(int a_serverPort)
  {
    boolean success = false;
    if ( (a_serverPort >= 1) && (a_serverPort <= 65535))
    {
      synchronized (this)
      {
        if (m_routingMode != ROUTING_MODE_SERVER)
        {
          /* server is not running, so simply change the port value */
          m_serverPort = a_serverPort;
          success = true;
        }
        else
        {
          if (m_serverPort != a_serverPort)
          {
            /* the server is running and the port differs from the old one , try to add the new port
             * and remove the old ones
             */
            ForwardServerManager.getInstance().removeAllServerManagers();
            success = ForwardServerManager.getInstance().addListenSocket(a_serverPort);
            if (success == true)
            {
              m_serverPort = a_serverPort;
              /* start the new propaganda (the old one is stopped automatically) */
              startPropaganda(false);
            }
            else {
              /* reopen the old port */
              ForwardServerManager.getInstance().addListenSocket(m_serverPort);
            }            
          }
        }
      }
    }
    return success;
  }

  /**
   * Returns the maximum bandwidth, which can be provided for forwarding. The current bandwidth
   * for forwarding can be smaller than this value.
   *
   * @return The maximum bandwidth we can provide for forwarding in bytes/sec.
   */
  public int getMaxBandwidth()
  {
    return m_maxBandwidth;
  }

  /**
   * Changes the maximum possible bandwidth (the current bandwidth can be smaller than that value).
   * If the new value is smaller than the current bandwidth value, the current bandwidth is also
   * lowered (and maybe this changes the number of possible connections).
   *
   * @param a_maxBandwidth The maximum bandwidth we can provide for forwarding in bytes/sec.
   */
  public void setMaxBandwidth(int a_maxBandwidth)
  {
    synchronized (this)
    {
      m_maxBandwidth = a_maxBandwidth;
      if (a_maxBandwidth < getBandwidth())
      {
        setBandwidth(a_maxBandwidth);
      }
    }
  }

  /**
   * Returns the current bandwidth, which is provided for forwarding.
   *
   * @return The current bandwidth for forwarding in bytes/sec.
   */
  public int getBandwidth()
  {
    return m_bandwidth;
  }

  /**
   * Changes the bandwidth, which is provided for forwarding. If the new value is bigger than the
   * maximum bandwidth value, the maximum value is also rised. Also the allowed connection number
   * is altered, if necessary (more connections than the bandwidth can support).
   *
   * @param a_bandwidth The bandwidth for forwarding in bytes/sec.
   */
  public void setBandwidth(int a_bandwidth)
  {
    synchronized (this)
    {
      m_bandwidth = a_bandwidth;
      if (a_bandwidth > m_maxBandwidth)
      {
        m_maxBandwidth = a_bandwidth;
      }
      ForwardServerManager.getInstance().setNetBandwidth(a_bandwidth);
      /* call setAllowedConnections with the current allowed connection number, that will alter the
       * allowed connection number, if necessary
       */
      setAllowedConnections(getAllowedConnections());
    }
  }

  /**
   * Returns the maximum number of clients, which can be forwarded with the current forwarding
   * bandwidth. This number depends on the bandwidth per user constant in JAPConstants.The number
   * of allowed forwarding connections can be smaller than this value.
   * @see JAPConstants.ROUTING_BANDWIDTH_PER_USER
   *
   * @return The maximum number of connections which can be forwarded with the current bandwidth.
   */
  public int getBandwidthMaxConnections()
  {
    return (getBandwidth() / JAPConstants.ROUTING_BANDWIDTH_PER_USER);
  }

  /**
   * Returns the allowed number of simultaneously forwarded connections.
   *
   * @return The allowed number of forwarded connections.
   */
  public int getAllowedConnections()
  {
    return m_connections;
  }

  /**
   * Changes the allowed number of simultaneously forwarded connections. If the new value is
   * bigger than the maximum number of possible connections because of the bandwidth limit, the
   * number is set to the maximum number of possible connections.
   * @see getBandwidthMaxConnections()
   *
   * @param a_connections The new allowed number of forwarded connections.
   */
  public void setAllowedConnections(int a_connections)
  {
    synchronized (this)
    {
      if (a_connections > getBandwidthMaxConnections())
      {
        a_connections = getBandwidthMaxConnections();
      }
      m_connections = a_connections;
      ForwardServerManager.getInstance().setMaximumNumberOfConnections(a_connections);
    }
  }

  /**
   * This method sets new settings for the proxy server. All new connections created by the
   * routing server or the routing client after the call of this method will use them. Connections
   * which already exist are not influenced by that call. The default after creating the instance
   * of JAPRoutingSettings is to use no proxy for all new connections.
   *
   * @param a_proxyType The type of the proxy (see the constants in anon.server.AnonServiceImpl).
   * @param a_proxyHost IP address or hostname of the proxy server. If no hostname is supplied,
   *                    the proxyType is set to FIREWALL_TYPE_NONE.
   * @param a_proxyPort The port of the proxy server. The value must be between 1 and 65535. If it
   *                    is not, the proxyType is set to FIREWALL_TYPE_NONE.
   * @param a_proxyAuthUserName The username for the authorization. If the proxy server does not
   *                            need authentication, take null. This value is only meaningful, if
   *                            the proxyType is FIREWALL_TYPE_HTTP.
   * @param a_proxyAuthPassword The password for the authorization. If the proxy server does not
   *                            need authentication, take null. This value is only meaningful, if
   *                            the proxyType is FIREWALL_TYPE_HTTP and proxyAuthUserName is not
   *                            null.
   */
  public void setNewProxySettings(int a_proxyType, String a_proxyHost, int a_proxyPort,
                  String a_proxyAuthUserName, String a_proxyAuthPassword)
  {
    if (a_proxyHost == null)
    {
      a_proxyType = AnonServiceImpl.FIREWALL_TYPE_NONE;
    }
    if ( (a_proxyPort < 1) || (a_proxyPort > 65535))
    {
      a_proxyType = AnonServiceImpl.FIREWALL_TYPE_NONE;
    }
    ForwardUtils.getInstance().setProxySettings(a_proxyType, a_proxyHost, a_proxyPort,
      a_proxyAuthUserName, a_proxyAuthPassword);
  }

  /**
   * Changes the forwarder for the client routing mode. If the routing mode is changed to the
   * client routing mode, we use this settings to connect to the forwarder. This settings can
   * only be changed, if the routing mode is not ROUTING_MODE_CLIENT, so this is only possible
   * when there is no connection to a forwarder.
   *
   * @param a_forwarderHost The hostname/IP of a forwarder.
   * @param a_forwarderPort The port of a forwarder.
   */
  public void setForwarder(String a_forwarderHost, int a_forwarderPort)
  {
    synchronized (this)
    {
      if (m_routingMode != ROUTING_MODE_CLIENT)
      {
        m_forwarderHost = a_forwarderHost;
        m_forwarderPort = a_forwarderPort;
      }
    }
  }

  /**
   * Returns a String with the information about the current forwarder with IP, hostname (if DNS
   * is enabled) and the port in the format "IP (hostname) : port" or "hostname (IP) : port"
   * depending on which information was set as forwarder host. The information in brackets
   * is returned by the DNS system, if DNS is disabled or can't resolve the name, the information
   * in the brackets is omitted. If there is no forwarder set (or an invalid one), an empty String
   * is returned.
   *
   * @return A String representation of the current forwarder or an empty String.
   */
  public String getForwarderString() {
    String forwarderString = "";
    ListenerInterface forwarder = null;
    synchronized (this) {
      try {
        forwarder = new ListenerInterface(m_forwarderHost, m_forwarderPort);
      }
      catch (Exception e) {
        /* forwarder not set */
      }
    }
    if (forwarder != null) {
      forwarderString = forwarder.getHostAndIp() + " : " + Integer.toString(forwarder.getPort());
    }
    return forwarderString;
  }
      
  /**
   * Changes, whether connections to the infoservice needs forwarding too.
   *
   * @param a_forwardInfoService True, if connections to the infoservice must be forwarded, else
   *                             false.
   */
  public void setForwardInfoService(boolean a_forwardInfoService)
  {
    synchronized (this)
    {
      if (m_forwardInfoService != a_forwardInfoService)
      {
        m_forwardInfoService = a_forwardInfoService;
        if ( (a_forwardInfoService == true) && (getRoutingMode() == ROUTING_MODE_CLIENT))
        {
          /* apply the proxy settings directly for the infoservice */
          updateInfoServiceProxySettings();
        }
        if ( (a_forwardInfoService == false) && (getRoutingMode() == ROUTING_MODE_CLIENT))
        {
          /* restore the original proxy settings for the infoservice */
          JAPController.getController().applyProxySettingsToInfoService();
        }
      }
    }
  }

  /**
   * Returns, whether connections to the infoservice are also forwarded.
   *
   * @return True, if connections to the infoservice are also forwarded, else false.
   */
  public boolean getForwardInfoService()
  {
    return m_forwardInfoService;
  }

  /**
   * This method updates the local proxy for the infoservice, if we are in client routing mode
   * and the infoservice needs to be forwarded. This method must be called, if the old HTTP
   * listener port is closed and a new one is opened. At the moment, we don't need this method
   * because changing the HTTP listener requires a restart of JAP.
   */
  public void httpListenerPortChanged()
  {
    synchronized (this)
    {
      if ( (getForwardInfoService() == true) && (getRoutingMode() == ROUTING_MODE_CLIENT))
      {
        /* update the infoservice proxy settings -> use the new local HTTP listener port */
        updateInfoServiceProxySettings();
      }
    }
  }

  /**
   * This method is always called, when a anon connection is closed. If it is a forwarded
   * connection, we update the internal status like the routing mode.
   */
  public void anonConnectionClosed()
  {
    synchronized (this)
    {
      if (getRoutingMode() == ROUTING_MODE_CLIENT)
      {
        /* we have to do something */
        if (m_waitForShutdownCall == true)
        {
          /* this is the shutdown of an old anon connection before the start of a new forwarded
           * connection -> don't do anything except notifying the startup thread for the new
           * forwarded connection
           */
          this.notify();
        }
        else
        {
          /* this is the shutdown of an existing forwarded connection */
          setRoutingMode(ROUTING_MODE_DISABLED);
        }
      }
    }
  }

  /**
   * Returns the anon proxy for the forwarded client connection.
   *
   * @param a_listener The ServerSocket listening for incoming requests e.g. the from the web
   *                   browser.
   *
   * @return The anon proxy for the forwarded connection or null, if we are not in the client
   *         routing mode.
   */
  public AnonWebProxy getAnonProxyInstance(ServerSocket a_listener)
  {
    AnonWebProxy anonProxy = null;
    synchronized (this)
    {
      if (getRoutingMode() == ROUTING_MODE_CLIENT)
      {
        anonProxy = new AnonWebProxy(a_listener, m_forwardedConnection, m_maxDummyTrafficInterval);
      }
    }
    return anonProxy;
  }

  /**
   * Returns the connection offer from the forwarder. This method must be called exactly once,
   * after the connection to the forwarder is created. If there was an error or we are not in
   * client routing mode, an exception is thrown.
   *
   * @param a_certificateStore A certificate store with the trusted root certificates for checking
   *                           the signature of the mixcascades supported by the forwarder. If
   *                           this value is null, no certificate check is done -> this would
   *                           result in a serious security problem.
   *
   * @return The connection descriptor with the connection offer from the forwarder.
   */
  public ForwardConnectionDescriptor getConnectionDescriptor(JAPCertificateStore a_certificateStore) throws
    ClientForwardException
  {
    ForwardConnectionDescriptor connectionDescriptor = null;
    DefaultClientProtocolHandler protocolHandler = null;
    synchronized (this)
    {
      if (getRoutingMode() == ROUTING_MODE_CLIENT)
      {
        protocolHandler = m_protocolHandler;
      }
    }
    if (protocolHandler != null)
    {
      /* we are in client routing mode */
      try
      {
        connectionDescriptor = protocolHandler.getConnectionDescriptor(a_certificateStore);
      }
      catch (ClientForwardException e)
      {
        /* there was an exception, shutdown routing */
        setRoutingMode(ROUTING_MODE_DISABLED);
        throw (e);
      }
    }
    else
    {
      /* throw an exception, because we are not in client routing mode */
      throw (new ClientForwardException(ClientForwardException.ERROR_UNKNOWN_ERROR,
        "JAPRoutingSettings: getConnectionDescriptor: Not in client routing mode."));
    }
    synchronized (this)
    {
      /* store the needed dummy traffic interval */
      m_maxDummyTrafficInterval = connectionDescriptor.getMinDummyTrafficInterval();
    }
    return connectionDescriptor;
  }

  /**
   * This method must be called exactly once, after we have received the the connection offer
   * from the forwarder. If the call of this method doesn't throw an exception, everything is
   * ready for starting the anonymous connection. This method throws an exception, if there is
   * something wrong while sending our decision to the forwarder. At the moment this method
   * must be called within the forwarder dummy traffic interval, because dummy traffic is not
   * implemented within the used protocol -> dummy traffic is available after starting the JAP
   * AnonProxy on the forwarded connection.
   *
   * @param a_mixCascade The mixcascade from the connection offer we want to use.
   */
  public void selectMixCascade(MixCascade a_mixCascade) throws ClientForwardException
  {
    DefaultClientProtocolHandler protocolHandler = null;
    synchronized (this)
    {
      if (getRoutingMode() == ROUTING_MODE_CLIENT)
      {
        protocolHandler = m_protocolHandler;
      }
    }
    if (protocolHandler != null)
    {
      /* we are in client routing mode */
      try
      {
        protocolHandler.selectMixCascade(a_mixCascade);
      }
      catch (ClientForwardException e)
      {
        /* there was an exception, shutdown routing */
        setRoutingMode(ROUTING_MODE_DISABLED);
        throw (e);
      }
    }
    else
    {
      /* throw an exception, because we are not in client routing mode */
      throw (new ClientForwardException(ClientForwardException.ERROR_UNKNOWN_ERROR,
        "JAPRoutingSettings: selectMixCascade: Not in client routing mode."));
    }
  }

  /**
   * Returns the statistics instance of the forwarding scheduler. If we are not in server
   * routing mode, null is returned.
   *
   * @return The statistics instance of the scheduler or null, if no scheduler is running.
   */
  public ForwardSchedulerStatistics getSchedulerStatistics()
  {
    return ForwardServerManager.getInstance().getSchedulerStatistics();
  }

  /**
   * Returns the number of currently forwarded connections. If we are not in server routing mode,
   * 0 is returned.
   *
   * @return The number of currently forwarded connections.
   */
  public int getCurrentlyForwardedConnections()
  {
    return ForwardServerManager.getInstance().getCurrentlyForwardedConnections();
  }

  /**
   * Starts the propaganda instances, which register the local forwarding server at the specified
   * infoservices. Any running propaganda instances are stopped. If we are not in the server
   * routing mode, nothing is done.
   *
   * @param a_blocking Whether to wait until all propaganda instances are started and have tried
   *                   to connect to the infoservice (true) or return immediately (false).
   *
   * @return Always 0 ath the moment.
   */
  public int startPropaganda(boolean a_blocking) {
    /* create a lock for synchronizing the startPropaganda thread with the current one */
    final JAPRoutingSettingsPropagandaThreadLock masterThreadLock = new JAPRoutingSettingsPropagandaThreadLock();
    synchronized (this) {
      if (m_routingMode == ROUTING_MODE_SERVER) {
        /* we have to be in the server routing mode */
        /* stop the running propaganda instances */
        stopPropaganda();
        final Vector infoServiceList = (Vector) (m_infoServiceRegistration.clone());
        final Vector currentPropagandists = new Vector();
        m_runningPropagandists = currentPropagandists;
        m_startPropagandaThread = new Thread(new JAPThread() {
          public void run() {
            /* this is not synchronized */
            Enumeration infoServices = infoServiceList.elements();
            boolean stopRegistration = false;
            while ((infoServices.hasMoreElements()) && (stopRegistration == false)) {
              ServerSocketPropagandist currentPropagandist = new ServerSocketPropagandist(m_serverPort, (InfoService)(infoServices.nextElement()));
              synchronized (JAPModel.getModel().getRoutingSettings()) {
                stopRegistration = Thread.interrupted();
                if (stopRegistration == true) {
                  /* we were interrupted -> all propagandists except the current on were stopped
                   * -> stop the current one
                   */
                  currentPropagandist.stopPropaganda();
                }
                else {
                  /* we were not interrupted -> go on */
                  currentPropagandists.addElement(currentPropagandist);
                }
              }
            }
            synchronized (JAPModel.getModel().getRoutingSettings()) {
              /* remove the pointer to this thread, because we are at the end -> interrupting makes
               * no sense any more
               */
              m_startPropagandaThread = null;
            }
            /* we are at the end -> notify the master thread, if it is waiting at the lock */
            synchronized (masterThreadLock) {
              masterThreadLock.propagandaThreadIsReady();
              masterThreadLock.notify();
            }
          }
        });
        m_startPropagandaThread.setDaemon(true);
        m_startPropagandaThread.start();
      }
      else {
        /* we are not in server routing mode -> we are ready because nothing was to do */
        masterThreadLock.propagandaThreadIsReady();
      }
    }      
    synchronized (masterThreadLock) {
      if (a_blocking == true) {
        /* wait for the startPropaganda Thread */
        if (masterThreadLock.isPropagandaThreadReady() == false) {
          /* wait only, if it is not already at the end */
          try {
            masterThreadLock.wait();
          }
          catch (InterruptedException e) {
          }
        }
      }
    }
    return 0;
  }

  /**
   * Stops all running propaganda instances, which register the local forwarder at the specified
   * infoservices.
   */   
  public void stopPropaganda() {
    synchronized (this) {
      if (m_startPropagandaThread != null) {
        /* there is a thread starting new propagandist instances */
        try {
          /* interrupt the running startPropaganda Thread */
          m_startPropagandaThread.interrupt();
        }
        catch (Exception e) {
          /* should not happen */
        }
      }
      /* stop all running propagandists */
      while (m_runningPropagandists.size() > 0) {
        ((ServerSocketPropagandist)(m_runningPropagandists.firstElement())).stopPropaganda();
        m_runningPropagandists.removeElementAt(0);
      }
    }
  }

  /**
   * Changes the infoservices, where we register the local forwarding server. We try to register
   * at every of those infoservices. The propaganda instances are updated automatically.
   *
   * @param a_infoServices A Vector of InfoServices with a forwarder list (to infoservices without
   *                       a forwarder list, the connection is never successful).
   */
  public void setRegistrationInfoServices(Vector a_infoServices) {
    synchronized (this) {
      m_infoServiceRegistration = a_infoServices;
      /* update the propaganda instances */
      startPropaganda(false);
    }
  }

  
  /**
   * If the infoservice needs forwarding, this changes the infoservice proxy settings to the
   * JAP HTTP listener port (where JAP accept requests from browsers). So all infoservice requests
   * are forwarded by JAP to the forwarder, from there through the mixcascade and then to the
   * infoservice.
   */
  private void updateInfoServiceProxySettings()
  {
    synchronized (this)
    {
      if (getForwardInfoService() == true)
      {
        /* change the proxy settings for the infoservice */
        HTTPConnectionFactory.getInstance().setNewProxySettings(HTTPConnectionFactory.PROXY_TYPE_HTTP,
          "localhost", JAPModel.getHttpListenerPortNumber(), null, null);
      }
    }
  }
    
}
