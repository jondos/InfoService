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
 * This class is used for synchronization between the threads, when startPropaganda() is called.
 */
public class JAPRoutingSettingsPropagandaThreadLock {
  
  /**
   * Stores whether the propaganda thread has come to the end (true) or not (false).
   */
  private boolean m_propagandaThreadReady;
  
  
  /**
   * This constructs a new JAPRoutingSettingsPropagandaThreadLock. The propagandaThreadReady
   * value is set to false.
   */
  public JAPRoutingSettingsPropagandaThreadLock() {
    m_propagandaThreadReady = false;
  }
  
  
  /**
   * Sets the propagandaThreadReady value to the ready state (propaganda thread has come to the
   * end.
   */
  public void propagandaThreadIsReady() {
    m_propagandaThreadReady = true;
  }
  
  /**
   * Returns whether the propaganda thread has come to the end (registered the forwarding server
   * at all specified infoservices or was interrupted while registering).
   *
   * @return True, if the propaganda thread is ready or false, if it is still working.
   */
  public boolean isPropagandaThreadReady() {
    return m_propagandaThreadReady;
  }
  
}