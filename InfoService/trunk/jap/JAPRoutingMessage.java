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
 * This is the message, which is send from JAPRoutingSettings to the observers. The message
 * code identifies the reason of the notification.
 */
public class JAPRoutingMessage
{

  /**
   * This message is sent after the routing mode was changed.
   */
  public static final int ROUTING_MODE_CHANGED = 1;

  /**
   * This message is sent after there was added a new forwarding server propaganda instance.
   * If this message is sent, the data must be the list of all propagandists.
   */
  public static final int PROPAGANDA_INSTANCES_ADDED = 2;
  
  /**
   * This message is sent, when JAPRoutingSettings.startPropaganda() is ready, which means
   * that all propaganda instances are started. The data must be the list of all started
   * propaganda instances.
   */    
  public static final int START_PROPAGANDA_READY = 3;

  /**
   * This message is sent, after JAPRoutingSettings.stopPropaganda() was called. So all
   * propaganda instances are stopped. Attention: This message can appear without a
   * prior START_PROPAGANDA_READY message, if the startPropaganda thread was interrupted
   * while starting all propaganda instances.
   */
  public static final int STOP_PROPAGANDA_CALLED = 4;
  
          
  /**
   * Stores the message code.
   */
  private int m_messageCode;

  /**
   * Stores some message data, which maybe was sent with the message.
   */
  private Object m_messageData;
        
        
  /**
   * This creates a new JAPRoutingMessage. The message data is set to null.
   *
   * @param a_messageCode The message code. See the constants in this class.
   */
  public JAPRoutingMessage(int a_messageCode)
  {
    m_messageCode = a_messageCode;
    m_messageData = null;
  }

  /**
   * This creates a new JAPRoutingMessage.
   *
   * @param a_messageCode The message code. See the constants in this class.
   * @param a_messageData The data to send with the message.
   */
  public JAPRoutingMessage(int a_messageCode, Object a_messageData) {
    m_messageCode = a_messageCode;
    m_messageData = a_messageData;
  }
    

  /**
   * This returns the message code of this JAPRoutingMessage. See the constants in this class.
   *
   * @return The message code.
   */
  public int getMessageCode()
  {
    return m_messageCode;
  }

  /**
   * Returns the message data, which was sent with the message. If there was no data sent with
   * the message, null is returned.
   *
   * @return The message data.
   */
  public Object getMessageData() {
    return m_messageData;
  }
  
}