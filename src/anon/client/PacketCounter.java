/*
 * Copyright (c) 2006, The JAP-Team
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *   - Redistributions of source code must retain the above copyright notice,
 *     this list of conditions and the following disclaimer.
 *
 *   - Redistributions in binary form must reproduce the above copyright
 *     notice, this list of conditions and the following disclaimer in the
 *     documentation and/or other materials provided with the distribution.
 *
 *   - Neither the name of the University of Technology Dresden, Germany nor
 *     the names of its contributors may be used to endorse or promote
 *     products derived from this software without specific prior written
 *     permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package anon.client;

import java.util.Observable;
import java.util.Observer;


/** 
 * @author Stefan Lieske
 */
public class PacketCounter extends Observable implements Observer {

  /**
   * This counts the data-packets for the observers of this class.
   */
  private long m_processedDataPackets;
  
  /**
   * This counts the packets which have to be paid.
   */
  private long m_payPacketCounter;
  
  private Object m_internalSynchronization;
  
  
  public PacketCounter() {
    m_processedDataPackets = 0;
    m_payPacketCounter = 0;
    m_internalSynchronization = new Object();
  }
  
  
  public void update(Observable a_object, Object a_argument) {
    if (a_argument instanceof PacketProcessedEvent) {
      int code = ((PacketProcessedEvent)a_argument).getCode();
      synchronized (m_internalSynchronization) {
        switch (code) {
          case PacketProcessedEvent.CODE_DATA_PACKET_SENT: {
            m_processedDataPackets++;
            m_payPacketCounter++;
            setChanged();
            break;
          }
          case PacketProcessedEvent.CODE_DATA_PACKET_RECEIVED: {
            m_processedDataPackets++;
            m_payPacketCounter++;
            setChanged();
            break;
          }
          case PacketProcessedEvent.CODE_DATA_PACKET_DISCARDED: {
            /* packet have to be paid, but has no further use */
            m_payPacketCounter++;
            break;
          }
        }
        /* oberservers will get only a notification if setChanged() was
         * called
         */
        notifyObservers(new Long(m_processedDataPackets * (long)(MixPacket.getPacketSize())));
      }
    }
  }
  
  public long getAndResetBytesForPayment() {
    long paymentBytes = 0;
    synchronized (m_internalSynchronization) {
      paymentBytes = m_payPacketCounter * (long)(MixPacket.getPacketSize());
      m_payPacketCounter = 0;
    }
    return paymentBytes;
  }

  
}
