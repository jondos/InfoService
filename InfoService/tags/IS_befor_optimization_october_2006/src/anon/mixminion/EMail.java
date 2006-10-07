/*
Copyright (c) 2005, The JAP-Team
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
package anon.mixminion;


public class EMail
{
    private String[] m_receiver = null;
    private String m_payload = null;
    private String m_sender = "";

    /**
     * The Constructor of an eMail, which scould be send over the MixMinion-Net
     * @param receiver, a list of receivers of this eMail
     * @param payload, the Payload of this eMail
     */
    public EMail(String[] receiver, String payload)
    {
        this.m_receiver = receiver;
        this.m_payload  = payload;
        
    }


    /**
     * @return the Receivers of this eMail
     */
    public String[] getReceiver()
    {
        return m_receiver;
    }

    /**
     * @return the Payload of this eMail
     */
    public String getPayload()
    {
        return m_payload;
    }


    public String toString()
    {
        String ret = "";
        for (int i=0; i<m_receiver.length; i++)
        {
            ret = ret + "[" + m_receiver[i] + "]\n";
        }
        ret = ret + m_payload;

        return ret;
    }
    
    public String getSender() {
    	return "";//m_sender;
    }
    
 
}
