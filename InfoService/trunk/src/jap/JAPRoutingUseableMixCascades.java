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
import java.util.Observable;
import java.util.Observer;
import java.util.Vector;

import anon.infoservice.InfoServiceHolder;
import anon.infoservice.MixCascade;
import forward.server.ForwardServerManager;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/**
 * This class manages the useable mixcascades for the clients of the local forwarding server. So
 * they get always an up-to-date list of running and allowed mixcascades.
 */
public class JAPRoutingUseableMixCascades implements Observer, Runnable {
  
  /**
   * This is the update interval of the mixcascade list. The list update is done by fetching the
   * mixcascade list from the infoservices (via InfoServiceHolder.getMixCascades()). Then the
   * internal stored list (in ForwardCascadeDatabase) is updated by adding the new (allowed)
   * cascades and removing that ones, which are not in the list of currently running cascades.
   * So the client always gets an up-to-date list of available (and allowed) cascades. The
   * default update interval is 10 minutes.
   */
  private static final long MIXCASCADELIST_UPDATE_INTERVAL = 10 * 60 * (long)1000;
  
  
  /**
   * This stores the list of allowed mixcascades. This list is used, if the access to the
   * mixcascades shall be restricted for the clients of the local forwarding server. So if
   * access to all available mixcascades is enabled, this list is not used. The usage of
   * the hashtable has only comfort reasons. So accessing the cascades by the ID is much
   * easier.
   */
  Hashtable m_allowedMixCascades;
  
  /**
   * This stores, whether the access to all available mixcascades shall be allowed for the clients
   * of the local forwarding server (true) or it shall be restricted to some cascades (false).
   */
  boolean m_allowAllAvailableCascades;
  
  /**
   * This stores the list of currently running mixcascades. This list is updated once within an
   * update interval. The usage of the hashtable has only comfort reasons. So accessing the
   * cascades by the ID is much easier.
   */
  Hashtable m_currentlyRunningMixCascades;
     
  /**
   * This stores the instance of the update thread for the mixcascades. It updates the list of
   * currently running mixcascades and also the database of useable mixcascades for the clients
   * of the local forwarding server is updated. This thread is executed while we are in server
   * routing mode. If the thread is not running, this value is null. There can be only one
   * running instance of that thread at one time.
   */
  Thread m_updateMixCascadesListThread;
  
  
  /**
   * This creates a new instance of JAPRoutingUseableMixCascades. Some initialization is done
   * here. The new instance is configured for allowing access to all available mixcascades.
   */
  public JAPRoutingUseableMixCascades() {
    m_allowedMixCascades = new Hashtable();
    m_allowAllAvailableCascades = true;
    m_currentlyRunningMixCascades = new Hashtable();
    m_updateMixCascadesListThread = null;
  }
  

  /**
   * This is the observer implementation to observe the instance of JAPRoutingSettings. We handle
   * the messages about changes of the routing mode here. If the forwarding server is started, also
   * we start also the mixcascades management thread. If it is stopped, we stop also that thread.
   *
   * @param a_notifier The observed Object. This should always be JAPRoutingSettings at the moment.
   * @param a_message The reason of the notification. This should always be a JAPRoutingMessage
   *                  at the moment.
   */
  public void update(Observable a_notifier, Object a_message) {
    if (a_notifier == JAPModel.getModel().getRoutingSettings()) {
      try {
        /* message is from JAPRoutingSettings */
        if (((JAPRoutingMessage)(a_message)).getMessageCode() == JAPRoutingMessage.ROUTING_MODE_CHANGED) {
          synchronized (this) {
            if (JAPModel.getModel().getRoutingSettings().getRoutingMode() == JAPRoutingSettings.ROUTING_MODE_SERVER) {
              /* look, whether the update-thread is running */
              if (m_updateMixCascadesListThread == null) {
                /* we have to start it */
                startMixCascadesListUpdateThread();
              }
            }
            else {
              /* look, whether the update-thread is running */
              if (m_updateMixCascadesListThread != null) {
                /* we have to stop it */
                stopMixCascadesListUpdateThread();
              }
            }
          }
        }
      }
      catch (Exception e) {
      }
    }
  }

  /**
   * This changes the list of allowed mixcascades for the clients of the local forwarding server.
   * If we are in the restricted mode (accessing all available mixcascades is not enabled),
   * clients can only connect to the mixcascades specified her, which are also running at the
   * moment of the client connection. If JAPRoutingSettings is in server routing mode (mixcascade
   * management thread is running) and we are in the restricted mode, also the database of
   * useable mixcascades for the client is updated immediately. Attention: Calling this method
   * does not automatically activate the restricted mode. So you have to do this by the call
   * of the setAllowAllMixCascades() method explicitly.
   *
   * @param a_mixCascades Start of an enumeration of mixcascades, which shall be allowed for the
   *                      clients of the local forwarding server.
   */
  public void setAllowedMixCascades(Enumeration a_mixCascades) {
    synchronized (m_allowedMixCascades) {
      m_allowedMixCascades.clear();
      while (a_mixCascades.hasMoreElements()) {
        MixCascade currentMixCascade = (MixCascade)(a_mixCascades.nextElement());
        m_allowedMixCascades.put(currentMixCascade.getId(), currentMixCascade);
      }
    }
    synchronized (this) {
      if ((m_updateMixCascadesListThread != null) && (m_allowAllAvailableCascades == false)) {
        /* the mixcascades management thread is running and we are in restricted mode -> update
         * the useable cascades database
         */
        updateUseableCascadesDatabase();
      }
    }
  }
  
  /**
   * Returns a clone of the list of allowed mixcascades for the forwarding server.
   *
   * @return A clone of the allowed mixcascades list for the clients of the local forwarding
   *         server.
   */
  public Vector getAllowedMixCascades() {
    Vector resultValue = new Vector();
    synchronized (m_allowedMixCascades) {
      Enumeration allowedCascades = m_allowedMixCascades.elements();
      while (allowedCascades.hasMoreElements()) {       
        resultValue.addElement(allowedCascades.nextElement());
      }
    }
    return resultValue;
  }
  
  /**
   * This changes the restriction mode for the clients between no restriction (access to all
   * running mixcascades is allowed) or restriction to the list of allowed mixcascades, which
   * needs to be also running. If the forwarding server is running and the mode was changed,
   * also the database of useable mixcascades for the forwarding server is updated.
   *
   * @param a_allowAllAvailableCascades Whether access to all available mixcascades shall be
   *                                    granted (true) or only to set of explicitly allowed
   *                                    mixcascades (false).
   */
  public void setAllowAllAvailableMixCascades(boolean a_allowAllAvailableCascades) {
    synchronized (this) {
      if (m_allowAllAvailableCascades != a_allowAllAvailableCascades) {
        m_allowAllAvailableCascades = a_allowAllAvailableCascades;
        if (m_updateMixCascadesListThread != null) {
          /* the mode was changed and the management thread is running (-> forwarding server is
           * running), so update the database of useable mixcascades
           */
          updateUseableCascadesDatabase();
        }
      }
    }
  }
  
  /**
   * Returns the restriction mode. This method returns true, if the clients of the local
   * forwarding server have access to all available mixcascades or false, if they have only access
   * to a set of allowed mixcascades.
   *
   * @return Whether all mixcascades are allowed to access for the clients of the local forwarding
   *         server.
   */
  public boolean getAllowAllAvailableMixCascades() {
    boolean returnValue = false;
    synchronized (this) {
      returnValue = m_allowAllAvailableCascades;
    }
    return returnValue;
  }  
  
  /**
   * This is the implementation of the mixcascades management thread for the local forwarding
   * server. It fetches the currently running mixcascades once per update interval from the
   * infoservices and updates with that information the database of currently running and allowed
   * mixcascades for the clients of the local forwarding server.
   */
  public void run() {
    boolean stopThread = false;
    while (stopThread == false) {
      /* get all running mixcascades */
      Vector runningMixCascadesList = InfoServiceHolder.getInstance().getMixCascades();
      if (runningMixCascadesList == null) {
        /* handle communication errors like no cascades are currently running */
        runningMixCascadesList = new Vector();
      }
      synchronized (m_currentlyRunningMixCascades) {
        /* update the list of currently running mixcascades */
        m_currentlyRunningMixCascades.clear();
        Enumeration runningMixCascades = runningMixCascadesList.elements();
        while (runningMixCascades.hasMoreElements()) {
          MixCascade currentMixCascade = (MixCascade)(runningMixCascades.nextElement());
          m_currentlyRunningMixCascades.put(currentMixCascade.getId(), currentMixCascade);
        }
      }
      /* now update the database of useable mixcascades for the clients of the forwarding
       * server
       */
      updateUseableCascadesDatabase();
      synchronized (m_updateMixCascadesListThread) {
        stopThread = Thread.interrupted();
        if (stopThread == false) {
          try {
            m_updateMixCascadesListThread.wait(MIXCASCADELIST_UPDATE_INTERVAL);
          }
          catch (Exception e) {
            /* there was an exception, this should only be an interrupted exception */
            stopThread = true;
          }
        }
      }
    }
    /* the update mixcascades list thread was stopped -> clear the list of currently running
     * mixcascades and also th database of useable mixcascades for the clients of the local
     * forwarding server
     */
    synchronized (m_currentlyRunningMixCascades) {
      m_currentlyRunningMixCascades.clear();
    }
    ForwardServerManager.getInstance().getAllowedCascadesDatabase().removeAllCascades();
  }


  /**
   * Updates the ForwardServerDatabase, which contains the currently useable mixcascades for the
   * clients of the local forwarding server. This method adds new and allowed cascades to that
   * database and removes all currently not running or not allowed cascades from there.
   */
  private void updateUseableCascadesDatabase() {
    synchronized (m_currentlyRunningMixCascades) {
      synchronized (m_allowedMixCascades) {
        /* we need exclusiv access while changing the mixcascade lists */
        boolean useAllAvailableCascades = m_allowAllAvailableCascades;
        /* clear the list of currently running mixcascades and rebuild it */
        Enumeration runningMixCascades = m_currentlyRunningMixCascades.elements();
        while (runningMixCascades.hasMoreElements()) {
          MixCascade currentMixCascade = (MixCascade)(runningMixCascades.nextElement());
          if (useAllAvailableCascades == true) {
            /* add / update this cascade in the database of all useable cascades */
            ForwardServerManager.getInstance().getAllowedCascadesDatabase().addCascade(currentMixCascade);
          }
          else {
            /* look, whether a cascade with this id is in the list of allowed mixcascades */
            if (m_allowedMixCascades.containsKey(currentMixCascade.getId()) == true) {
              /* add / update this cascade in the database of all useable cascades */
              ForwardServerManager.getInstance().getAllowedCascadesDatabase().addCascade(currentMixCascade);
            }
          }
        }
        /* now remove all cascades, which are not running or not allowed from the useable
         * cascades database of the forwarding server
         */
        Enumeration forwardingCascades = ForwardServerManager.getInstance().getAllowedCascadesDatabase().getEntryList().elements();
        while (forwardingCascades.hasMoreElements()) {
          MixCascade currentForwardingMixCascade = (MixCascade)(forwardingCascades.nextElement());
          if (m_currentlyRunningMixCascades.containsKey(currentForwardingMixCascade.getId())) {
            /* the mixcascade is currently running -> ok */
            if (useAllAvailableCascades == false) {
              /* we have to look, whether the mixcascade is allowed */
              if (m_allowedMixCascades.containsKey(currentForwardingMixCascade.getId()) == false) {
                /* the mixcascade is not allowed -> remove it */
                ForwardServerManager.getInstance().getAllowedCascadesDatabase().removeCascade(currentForwardingMixCascade.getId());
              }
            }
          }
          else {
            /* the mixcascade is not running any more -> remove it */
            ForwardServerManager.getInstance().getAllowedCascadesDatabase().removeCascade(currentForwardingMixCascade.getId());
          }  
        }  
      }
    }
  }
  
  /**
   * This starts the management thread for the useable mixcascades of the local forwarding server,
   * if it is not already running.
   */
  private void startMixCascadesListUpdateThread() {
    synchronized (this) {
      if (m_updateMixCascadesListThread == null) {
        LogHolder.log(LogLevel.INFO, LogType.MISC, "JAPRoutingUseableMixCascades: startMixCascadesListUpdateThread: The mixcascade management thread of the forwarding server is started.");
        m_updateMixCascadesListThread = new Thread(this);
        m_updateMixCascadesListThread.setDaemon(true);
        m_updateMixCascadesListThread.start();
      }
      else {
        LogHolder.log(LogLevel.INFO, LogType.MISC, "JAPRoutingUseableMixCascades: startMixCascadesListUpdateThread: The mixcascade management thread of the forwarding server was already started.");
      }  
    }
  }
  
  /**
   * This stops the management thread for the useable mixcascades of the local forwarding server,
   * if it is running.
   */
  private void stopMixCascadesListUpdateThread() {
    LogHolder.log(LogLevel.INFO, LogType.MISC, "JAPRoutingUseableMixCascades: stopMixCascadesListUpdateThread: Shutdown the mixcascade management thread of the forwarding server...");
    synchronized (this) {
      if (m_updateMixCascadesListThread != null) {
        synchronized (m_updateMixCascadesListThread) {
          m_updateMixCascadesListThread.interrupt();
        }
        try {
          m_updateMixCascadesListThread.join();
          LogHolder.log(LogLevel.INFO, LogType.MISC, "JAPRoutingUseableMixCascades: stopMixCascadesListUpdateThread: Mixcascade management thread of the forwarding server halted.");
        }
        catch (Exception e) {
        }
        m_updateMixCascadesListThread = null;
      }
      else {
        LogHolder.log(LogLevel.INFO, LogType.MISC, "JAPRoutingUseableMixCascades: stopMixCascadesListUpdateThread: The mixcascade management thread of the forwarding server was not running.");        
      }
    }
  }
          
}