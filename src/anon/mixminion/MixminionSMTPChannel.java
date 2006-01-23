package anon.mixminion;

import java.io.IOException;
import java.util.Vector;

import anon.mixminion.message.Message;
import anon.server.impl.AbstractChannel;

/** This class implements a channel,which speaks SMTP*/
public class MixminionSMTPChannel extends AbstractChannel
{
    /** the current State of the SMTPChannel **/
    private int m_state = -1;

    /** a Receiver-List of the eMail **/
    private Vector m_receiver = new Vector();

    /** the Text of the eMail **/
    private String m_text = "";


    public MixminionSMTPChannel()
    {
        super();
        m_state = 0;

        try
        {
            String first = "220 127.0.0.1 SMTP JAP_MailServer\n";
            toClient(first);
        }
        catch (IOException e)
	{
            e.printStackTrace();
	}
    }


    protected void close_impl()
	{
        // TODO Automatisch erstellter Methoden-Stub

    }

    protected void toClient(String message) throws IOException
    {
//        System.out.print("(z->" + z + ") Sende zum Client: " + message);
        recv(message.getBytes(),0,message.length());
	}

    // 'send' empfängt die Daten vom eMail-Progi
    protected void send(byte[] buff, int len) throws IOException {
        String s = new String(buff, 0, len);
//        System.out.print("(z=" + z + ") empfange vom Client: " + s);

        // nach dem init //
        if (m_state==0)
        {
            if(s.toUpperCase().startsWith("HELO"))
            {
                m_state = 2;
                toClient("250 OK\n");
            }
            else if (m_state==0 && s.toUpperCase().startsWith("EHLO"))
	{
                m_state = 1;
                toClient("503\n");
	}
            else
            {
                //no HELO or EHLO !!! There is an Error!!!
                throw new RuntimeException("(State=" + m_state + ") Didn't understand this Command '" + s + "'");
            }
        }
        // nach dem EHLO //
        else if (m_state==1)
        {
            if(s.toUpperCase().startsWith("HELO"))
            {
                m_state = 2;
                toClient("250 OK\n");
            }
            else
            {
                // only HELO is supported
                throw new RuntimeException("(State=" + m_state + ") Didn't understand this Command '" + s + "'");
            }
        }
        // nach HELO/EHLO bevor MAILFROM
        else if (m_state==2)
        {
            if(s.toUpperCase().startsWith("MAIL FROM"))
            {
                m_receiver.removeAllElements(); // Empfänger-Liste leeren
                m_text = ""; // Text-Nachricht leeren
                m_state = 3;
                toClient("250 OK\n");
            }
            else
            {
                throw new RuntimeException("(State=" + m_state + ") Didn't understand this Command '" + s + "'");
            }
        }
        // Nach MAILFROM vor RCTPTO //
        else if (m_state==3)
        {
            if (s.toUpperCase().startsWith("RCPT TO"))
            {
				String rec=s.substring(s.indexOf('<')+1,s.indexOf('>'));// RCPT TO:<John@Smith.net> //
                m_receiver.addElement(rec);
                toClient("250 OK\n");
            }
            else if (s.toUpperCase().startsWith("DATA"))
            {
                m_state = 4;
                toClient("354 Start mail input; end with <CRLF>.<CRLF>\n");
            }
            else
            {
                throw new RuntimeException("(State=" + m_state + ") Didn't understand this Command '" + s + "'");
            }
        }
        // im DATA -> lesen der Nachricht //
        else if (m_state==4)
        {
            m_text = m_text + s;

            if (m_text.endsWith("\r\n.\r\n")) // wenn "." empfangen //
            {

                String[] rec = new String[m_receiver.size()];
	m_receiver.copyInto(rec);
                EMail eMail = new EMail(rec,m_text);

                Message m = new Message(eMail.getPayload().getBytes(), eMail.getReceiverAsVektor(), 4);
                boolean success = m.send();

                m_state = 5;
                if (success==true) toClient("250 OK\n");
                else toClient("554 Fehler beim Versenden der eMail zum MixMinionServer!\n");
            }
        }
        else if (m_state==5)
        {
            if (s.toUpperCase().startsWith("QUIT"))
            {
                m_receiver.addElement(s);
                toClient("221 Bye\n");
                m_state = 99;
            }
            else if(s.toUpperCase().startsWith("MAIL FROM"))
            {
                m_receiver.removeAllElements(); // Empfänger-Liste leeren
                m_text = ""; // Text-Nachricht leeren
                m_state = 3;
                toClient("250 OK\n");
            }
            else if(s.toUpperCase().startsWith("RSET"))
            {
                m_state = 2;
                toClient("250 OK\n");
            }
            else
            {
                throw new RuntimeException("(State=" + m_state + ") Didn't understand this Command '" + s + "'");
            }
        }
        else
	{
            // Zustand nicht möglich //
            throw new RuntimeException("(State=" + m_state + ") This State is not possible");
	}
    }


    public int getOutputBlockSize() {
        // TODO Automatisch erstellter Methoden-Stub
        return 1000;
    }

}
