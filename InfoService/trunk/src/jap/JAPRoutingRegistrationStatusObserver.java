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

import forward.server.ServerSocketPropagandist;

public class JAPRoutingRegistrationStatusObserver extends Observable implements Observer {

  public static final int STATE_DISABLED = 0;
  
  public static final int STATE_INITIAL_REGISTRATION = 1;
  
  public static final int STATE_NO_REGISTRATION = 2;
  
  public static final int STATE_SUCCESSFUL_REGISTRATION = 3;
  
  public static final int ERROR_NO_ERROR = 0;
  
  public static final int ERROR_NO_KNOWN_PRIMARY_INFOSERVICES = 1;
  
  public static final int ERROR_INFOSERVICE_CONNECT_ERROR = 2;
  
  public static final int ERROR_VERIFICATION_ERROR = 3;

  public static final int ERROR_UNKNOWN_ERROR = 4;

  /**
   * This is the list of all known propaganda instances.
   */
  private Vector m_propagandaInstances;

  private int m_currentState;
  
  private int m_currentErrorCode;


  /**
   * Creates a new instance of JAPRoutingRegistrationStatusObserver. We do only some
   * initialization here.
   */
  public JAPRoutingRegistrationStatusObserver() {
    m_propagandaInstances = new Vector();
    m_currentState = STATE_DISABLED;
    m_currentErrorCode = ERROR_NO_ERROR;
  }

  
  public int getCurrentState() {
    return m_currentState;
  }

  public int getCurrentErrorCode() {
    return m_currentErrorCode;
  }
  
  /**
   * This is the implementation of the observer of the propaganda instances and
   * JAPRoutingSettings. If the instances reach the state HALTED, they are not observed any more.
   *
   * @param a_notifier The propaganda instance, which has changed the state or the
   *                   JAPRoutingSettings instance.
   * @param a_message The notification message (should be null for propaganda instances and a
   *                  JAPRoutingMessage for the JAPRoutingSettings instance).
   */
  public void update(Observable a_notifier, Object a_message) {
    if (a_notifier.getClass().equals(ServerSocketPropagandist.class)) {
      synchronized (m_propagandaInstances) {
        if (m_propagandaInstances.contains(a_notifier)) {
          /* the notifier is in the list of known forwarding server propagandists */
          if (((ServerSocketPropagandist)a_notifier).getCurrentState() == ServerSocketPropagandist.STATE_HALTED) {
            /* Propagandist was halted -> remove it from the list and stop observing the
             * propagandist
             */
            a_notifier.deleteObserver(this);
            m_propagandaInstances.removeElement(a_notifier);
            updateCurrentState(false);
          }
        }
      }
    }
    try {
      if (a_notifier == JAPModel.getInstance().getRoutingSettings()) {
        /* message is from JAPRoutingSettings */
        boolean notifyObserversNecessary = false;
        if (((JAPRoutingMessage)(a_message)).getMessageCode() == JAPRoutingMessage.STOP_PROPAGANDA_CALLED) {
          synchronized (this) {
            if (m_currentState != STATE_DISABLED) {
              m_currentState = STATE_DISABLED;
              m_currentErrorCode = ERROR_NO_ERROR;
              notifyObserversNecessary = true;
            }
          }
        }
        if (((JAPRoutingMessage)(a_message)).getMessageCode() == JAPRoutingMessage.START_PROPAGANDA_BEGIN) {
          synchronized (this) {
            if (m_currentState != STATE_INITIAL_REGISTRATION) {
              m_currentState = STATE_INITIAL_REGISTRATION;
              m_currentErrorCode = ERROR_NO_ERROR;
              notifyObserversNecessary = true;
            }
          }
        }
        if (((JAPRoutingMessage)(a_message)).getMessageCode() == JAPRoutingMessage.PROPAGANDA_INSTANCES_ADDED) {
          /* update the propagandists in the infoservice registration table */
          updatePropagandaInstancesList((Vector)(((JAPRoutingMessage)a_message).getMessageData()));
          updateCurrentState(false);
        }
        if (((JAPRoutingMessage)(a_message)).getMessageCode() == JAPRoutingMessage.START_PROPAGANDA_READY) {
          updateCurrentState(true);
        }
        if (notifyObserversNecessary == true) {
          setChanged();
          notifyObservers(null);
        }
      }
    }
    catch (Exception e) {
      /* should not happen */
    }    
  }

  private void updateCurrentState(boolean a_overwriteInitialRegistrationState) {
    synchronized (m_propagandaInstances) {
      synchronized (this) {
        if (((m_currentState == STATE_NO_REGISTRATION) || (m_currentState == STATE_SUCCESSFUL_REGISTRATION)) || ((m_currentState == STATE_INITIAL_REGISTRATION) && (a_overwriteInitialRegistrationState == true))) {
          /* we are in a state, where updating the current state is possible */
          int registrationStatus = STATE_NO_REGISTRATION;
          int registrationError = ERROR_NO_KNOWN_PRIMARY_INFOSERVICES;
          if (m_propagandaInstances.size() > 0) {         
            /* we have at least one propagandist -> we have tried at least one infoservice -> set
             * the error code to unknown error (minimum reachable error code with one running
             * propagandist)
             */
            registrationError = ERROR_UNKNOWN_ERROR;
            Enumeration runningPropagandists = m_propagandaInstances.elements();
            while ((registrationStatus != STATE_SUCCESSFUL_REGISTRATION) && (runningPropagandists.hasMoreElements())) {
              ServerSocketPropagandist currentPropagandist = (ServerSocketPropagandist)(runningPropagandists.nextElement());
              if (currentPropagandist.getCurrentState() == ServerSocketPropagandist.STATE_REGISTERED) {
                registrationStatus = STATE_SUCCESSFUL_REGISTRATION;
                registrationError = ERROR_NO_ERROR;
              }
              else {
                if ((currentPropagandist.getCurrentState() == ServerSocketPropagandist.STATE_CONNECTING) || (currentPropagandist.getCurrentState() == ServerSocketPropagandist.STATE_RECONNECTING)) {
                  if ((registrationError == ERROR_UNKNOWN_ERROR) && (currentPropagandist.getCurrentErrorCode() == ServerSocketPropagandist.RETURN_INFOSERVICE_ERROR)) {
                    registrationError = ERROR_INFOSERVICE_CONNECT_ERROR;
                  }
                  if (((registrationError == ERROR_UNKNOWN_ERROR) || (registrationError == ERROR_INFOSERVICE_CONNECT_ERROR)) && (currentPropagandist.getCurrentErrorCode() == ServerSocketPropagandist.RETURN_VERIFICATION_ERROR)) {
                    registrationError = ERROR_VERIFICATION_ERROR;
                  }
                }
              }
            }
          }  
          if ((registrationStatus != m_currentState) || (registrationError != m_currentErrorCode)) {
            m_currentState = registrationStatus;
            m_currentErrorCode = registrationError;
            setChanged();
            notifyObservers(null);
          }
        }
      }
    }
  }

  /**
   * Updates the list of all displayed propaganda instances. We add only new unknown instances
   * here, because removing of the old ones is done automatically, when they are stopped.
   *
   * @param a_newPropagandaInstancesList A Vector with propaganda instances. The new ones are
   *                                     added to the internal list.
   */
  private void updatePropagandaInstancesList(Vector a_newPropagandaInstancesList) {
    Enumeration propagandists = a_newPropagandaInstancesList.elements();
    synchronized (m_propagandaInstances) {
      while (propagandists.hasMoreElements()) {
        /* removing old propaganda instances is not done here, because they are removed
         * automatically, when they reach the status HALTED and notify us
         */
        ServerSocketPropagandist currentPropagandist = (ServerSocketPropagandist)(propagandists.nextElement());
        if (m_propagandaInstances.contains(currentPropagandist) == false) {
          /* observe the added propagandist, no problem also, if we already observe this
           * propagandist, then addObserver() does nothing
           */
          currentPropagandist.addObserver(this);
          if (currentPropagandist.getCurrentState() != ServerSocketPropagandist.STATE_HALTED) {
            /* add only the new propagandists to the list of all known propaganda instances */
            m_propagandaInstances.addElement(currentPropagandist);
          }
          else {
            /* the propagandist was stopped in the meantime -> don't add it and stop observing */
            currentPropagandist.deleteObserver(this);
          }
        }
      }
    }
  }

}
