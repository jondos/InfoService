package anon.mixminion;

import java.util.Vector;

public class EMail
{
    private String[] m_receiver = null;
    private String m_payload = null;
    
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
     * @return the Receivers of this eMail
     */
    public Vector getReceiverAsVektor()
    {
        Vector v = new Vector();
        for (int i=0; i<m_receiver.length; i++) v.add(m_receiver[i]);
        return v;
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
}
