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
import java.util.Observable;
import java.util.Observer;
import java.util.Vector;

import anon.infoservice.InfoService;
import anon.infoservice.InfoServiceHolder;
import forward.server.ServerSocketPropagandist;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/**
 * This class manages the infoservices, where the registration of the local forwarding server is
 * tried.
 */
public class JAPRoutingRegistrationInfoServices implements Observer, Runnable {
  
  /**
   * If the automatic management of the registration infoservices (registrate at all available
   * infoservices) is enabled, this constant is the update interval of the infoservice list
   * in milliseconds. The list is fetched via the InfoServiceHolder's getInfoServices() command
   * and new discovered infoservices with a forwarder list are used for new registration
   * instances. The default is 10 minutes.
   */
  private static final long INFOSERVICELIST_UPDATE_INTERVAL = 10 * 60 * (long)1000;
  
  
  /**
   * This stores the list of infoservices, where the registration is tried, if we are not in the
   * automatic management mode.
   */
  Vector m_registrationInfoServices;
  
  /**
   * This stores, whether the automatic management mode for the registration at the infoservices
   * is used (true) or the manual mode with the stored list of registration infoservices is used
   * (false).
   */ 
  boolean m_registerAtAllAvailableInfoServices;
  
  /**
   * This stores, whether propaganda for the local forwarding server is running at the moment
   * (true) or not (false).
   */
  boolean m_propagandaIsRunning;
  
  /**
   * This stores the list of infoservice IDs, where the local forwarding server is already
   * registrated or it is tried to registrate.
   */
  Vector m_runningInfoServiceRegistrations;
  
  /**
   * This stores the instance of the automatic infoservice registration management thread. If it
   * is not running at the moment, this value is null.
   */
  Thread m_updateInfoServiceListThread;
  
  
  /**
   * This creates a new instance of JAPRoutingRegistrationInfoServices. Some initialization is
   * done here. The new instance is configured for automatic management mode.
   */
  public JAPRoutingRegistrationInfoServices() {
    m_registrationInfoServices = new Vector();
    m_registerAtAllAvailableInfoServices = true;
    m_propagandaIsRunning = false;
    m_runningInfoServiceRegistrations = new Vector();
    m_updateInfoServiceListThread = null;
  }
  

  /**
   * This is the observer implementation to observe the instance of JAPRoutingSettings. We handle
   * the messages about changes of the propaganda status here. If the propaganda is started and
   * we are in automatic infoservice registration management mode, also the management thread is
   * started, so if there appear new infoservices with a forwarder list, the registration of the
   * local forwarding server is done automatically there. If the propaganda is started and we are
   * in automatic management mode, also the management thread is halted.
   *
   * @param a_notifier The observed Object. This should always be JAPRoutingSettings at the moment.
   * @param a_message The reason of the notification. This should always be a JAPRoutingMessage
   *                  at the moment.
   */
  public void update(Observable a_notifier, Object a_message) {
    if (a_notifier == JAPModel.getModel().getRoutingSettings()) {
      try {
        /* message is from JAPRoutingSettings */
        if (((JAPRoutingMessage)(a_message)).getMessageCode() == JAPRoutingMessage.PROPAGANDA_INSTANCES_ADDED) {
          synchronized (this) {
            synchronized (m_runningInfoServiceRegistrations) {
              Enumeration startedPropagandists = ((Vector)(((JAPRoutingMessage)a_message).getMessageData())).elements();
              while (startedPropagandists.hasMoreElements()) {
                InfoService currentInfoService = (InfoService)(((ServerSocketPropagandist)(startedPropagandists.nextElement())).getInfoService());
                if (m_runningInfoServiceRegistrations.contains(currentInfoService.getId()) == false) {
                  m_runningInfoServiceRegistrations.addElement(currentInfoService.getId());
                }
              }
            }
          }
        }
        if (((JAPRoutingMessage)(a_message)).getMessageCode() == JAPRoutingMessage.START_PROPAGANDA_READY) {
          synchronized (this) {
            m_propagandaIsRunning = true;
            if (m_registerAtAllAvailableInfoServices == true) {
              /* start the management thread */
              startInfoServiceListUpdateThread();
            }
          }
        }
        if (((JAPRoutingMessage)(a_message)).getMessageCode() == JAPRoutingMessage.STOP_PROPAGANDA_CALLED) {
          synchronized (this) {
            /* first stop the infoservice list update thread, if it is running */
            if (m_registerAtAllAvailableInfoServices) {
              stopInfoServiceListUpdateThread();
            }         
            m_propagandaIsRunning = false;
            /* clear the list of running propagandists, it is important to do this after the
             * infoservice list update thread is stopped, else the thread may adds some more
             * entries to the list, which are not removed
             */
            synchronized (m_runningInfoServiceRegistrations) {
              m_runningInfoServiceRegistrations.removeAllElements();
            }
          }
        }                              
      }
      catch (Exception e) {
      }
    }
  }
  
  /**
   * This changes the list of registration infoservices, which are used, if we are in manual
   * infoservice registration mode. If we are currently in the manual mode, also for the new
   * infoservices, where we are not registrated in the moment, a new propaganda instance is
   * started. But running instances are not stopped, even if the corresponding infoservice is
   * not in the new list any more. If we are automatic management mode at the moment, only the
   * internal manual infoservices list is updated, but there is no check done for new instances.
   *
   * @param a_infoServices Start of an enumeration of infoservices, which shall be used for
   *                       registration, if we are in manual management mode.
   */
  public void setRegistrationInfoServices(Enumeration a_infoServices) {
    synchronized (m_registrationInfoServices) {
      m_registrationInfoServices.removeAllElements();
      while (a_infoServices.hasMoreElements()) {
        m_registrationInfoServices.addElement(a_infoServices.nextElement());
      }
    }
    synchronized (this) {
      if (m_registerAtAllAvailableInfoServices == false) {
        /* we are in manual management mode -> start new propaganda instances, if necessary */      
        synchronized (m_runningInfoServiceRegistrations) {
          /* start propaganda instances for the new infoservices, where no registration process is
           * running at the moment
           */
          Enumeration newRegistrationInfoServices = m_registrationInfoServices.elements();
          while (newRegistrationInfoServices.hasMoreElements()) {
            InfoService currentInfoService = (InfoService)(newRegistrationInfoServices.nextElement());
            if (m_runningInfoServiceRegistrations.contains(currentInfoService.getId()) == false) {
              /* no propaganda instance for the current infoservice is running -> try to start a new
               * one, this is only done, if propaganda is running at the moment, so no problem, when
               * no propaganda is running
               */
              JAPModel.getModel().getRoutingSettings().addPropagandaInstance(currentInfoService);
              if (m_propagandaIsRunning == true) {
                /* we can add the new infoservice id to the propaganda list already yet */
                m_runningInfoServiceRegistrations.addElement(currentInfoService.getId());
              }
            }
          }
        }
      }
    }
  }
  
  /**
   * Returns a clone of the list of infoservices, which shall be used, if we are in manual
   * infoservice registration management mode.
   *
   * @return A clone of the manual configurated registration infoservices.
   */
  public Vector getRegistrationInfoServices() {
    Vector resultValue = new Vector();
    synchronized (m_registrationInfoServices) {
      resultValue = (Vector)(m_registrationInfoServices.clone());
    }
    return resultValue;
  }
  
  /**
   * This returns a list of infoservices, where the startPropaganda() method of JAPRoutingSettings
   * shall try the registration. If we are in automatic management mode, all already known
   * infoservices with a forwarder list (obtained from the InfoServiceDatabase via
   * InfoServiceHolder) are returned (maybe this list is not up-to-date, so new infoservices with
   * a forwarder list will be found after the first cycle of the management thread). If we are in
   * manual registration mode, the clone of the list of manual registration infoservices is
   * returned.
   *
   * @return The list of infoservices (depending on the management mode), where startPropaganda()
   *         shall try to registrate at.
   */
  public Vector getRegistrationInfoServicesForStartup() {
    Vector resultValue = new Vector();
    synchronized (this) {
      if (m_registerAtAllAvailableInfoServices == true) {
        /* return the list of all known infoservices with a forwarder list */
        resultValue = InfoServiceHolder.getInstance().getInfoservicesWithForwarderList();
      }
      else {
        /* return only the specified infoservices, stored in the internal list */
        synchronized (m_registrationInfoServices) {
          resultValue = (Vector)(m_registrationInfoServices.clone());
        }
      }
    }
    return resultValue;
  }
  
  /**
   * This changes the management mode between automatic infoservice registration and manual
   * infoservice registration. If the propaganda is running at the moment and the mode is
   * changed, also the automatic management thread is started or halted.
   *
   * @param a_registerAtAllAvailableInfoService Is this value is true, the automatic management
   *                                            mode is used, if it is false, the manual
   *                                            management mode is used.
   */
  public void setRegisterAtAllAvailableInfoServices(boolean a_registerAtAllAvailableInfoServices) {
    synchronized (this) {
      if (m_registerAtAllAvailableInfoServices != a_registerAtAllAvailableInfoServices) {
        /* this setting is changed */
        m_registerAtAllAvailableInfoServices = a_registerAtAllAvailableInfoServices;
        if (m_propagandaIsRunning == true) {
          /* we have to start or stop the management thread */
          if (a_registerAtAllAvailableInfoServices == true) {
            /* start the management thread */
            startInfoServiceListUpdateThread();
          }
          else {
            /* stop the management thread */
            stopInfoServiceListUpdateThread();
          }
        }
      }  
    }
  }
  
  /**
   * Returns the current setting of the management mode.
   *
   * @return True, if the automatic infoservice management mode is used or false, if the manual
   *         infoservice registration mode is used.
   */
  public boolean getRegisterAtAllAvailableInfoServices() {
    boolean returnValue = false;
    synchronized (this) {
      returnValue = m_registerAtAllAvailableInfoServices;
    }
    return returnValue;
  }

  /**
   * This is the implementation of the automatic infoservice registration management thread. It
   * looks periodically for new infoservices with a forwarder list
   * (via InfoServiceHolder.getInfoServices()), where the local forwarding server is not
   * registrated (or is tried to be registrated) at the moment. It creates the needed propaganda
   * instances for those new infoservices automatically. This thread is only running in the
   * automatic infoservice registration mode and when the propaganda is running
   * (see startPropaganda() and stopPropaganda() in JAPRoutingSettings).
   */
  public void run() {
    boolean stopThread = false;
    while (stopThread == false) {
      synchronized (m_updateInfoServiceListThread) {
        stopThread = Thread.interrupted();
        if (stopThread == false) {
          try {
            m_updateInfoServiceListThread.wait(INFOSERVICELIST_UPDATE_INTERVAL);
          }
          catch (Exception e) {
            /* there was an exception, this should only be an interrupted exception */
            stopThread = true;
          }
        }
      }
      if (stopThread == false) {
        /* get all running infoservices */
        Vector runningInfoServicesList = InfoServiceHolder.getInstance().getInfoServices();
        if (runningInfoServicesList != null) {
          /* communication was successful */
          Enumeration runningInfoServices = runningInfoServicesList.elements();
          while (runningInfoServices.hasMoreElements()) {
            InfoService currentInfoService = (InfoService)(runningInfoServices.nextElement());
            if (currentInfoService.hasPrimaryForwarderList()) {
              synchronized (m_runningInfoServiceRegistrations) {
                /* check, whether we are already registrated at this infoservice */
                if (m_runningInfoServiceRegistrations.contains(currentInfoService.getId()) == false) {
                  /* this is a new infoservice -> start a new propaganda instance and add the
                   * it to the running propagandists list, this needs no check, whether the
                   * propaganda is running because this thread only is running, when the
                   * propaganda is running also
                   */
                  JAPModel.getModel().getRoutingSettings().addPropagandaInstance(currentInfoService);
                  m_runningInfoServiceRegistrations.addElement(currentInfoService.getId());
                }  
              }
            }
          }
        }
      }  
    }
  }
  
  /**
   * This starts the automatic infoservice registration thread, if it is not already running.
   */
  private void startInfoServiceListUpdateThread() {
    synchronized (this) {
      if (m_updateInfoServiceListThread == null) {
        LogHolder.log(LogLevel.INFO, LogType.MISC, "JAPRoutingRegistrationInfoServices: startInfoServiceListUpdateThread: The infoservice registration management thread is started.");
        m_updateInfoServiceListThread = new Thread(this);
        m_updateInfoServiceListThread.setDaemon(true);
        m_updateInfoServiceListThread.start();
      }
      else {
        LogHolder.log(LogLevel.INFO, LogType.MISC, "JAPRoutingRegistrationInfoServices: startInfoServiceListUpdateThread: The infoservice registration management thread was already started.");
      }  
    }
  }
  
  /**
   * This stops the automatic infoservice registration thread, if it is running.
   */
  private void stopInfoServiceListUpdateThread() {
    LogHolder.log(LogLevel.INFO, LogType.MISC, "JAPRoutingRegistrationInfoServices: stopInfoServiceListUpdateThread: Shutdown the infoservice registration management thread...");
    synchronized (this) {
      if (m_updateInfoServiceListThread != null) {
        synchronized (m_updateInfoServiceListThread) {
          m_updateInfoServiceListThread.interrupt();
        }
        try {
          m_updateInfoServiceListThread.join();
          LogHolder.log(LogLevel.INFO, LogType.MISC, "JAPRoutingRegistrationInfoServices: stopInfoServiceListUpdateThread: Infoservice registration management thread halted.");
        }
        catch (Exception e) {
        }
        m_updateInfoServiceListThread = null;
      }
      else {
        LogHolder.log(LogLevel.INFO, LogType.MISC, "JAPRoutingRegistrationInfoServices: stopInfoServiceListUpdateThread: Infoservice registration management thread was not running.");        
      }
    }
  }
          
}