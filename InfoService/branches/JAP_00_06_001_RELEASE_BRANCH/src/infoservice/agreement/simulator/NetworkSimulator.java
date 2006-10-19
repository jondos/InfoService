package infoservice.agreement.simulator;

import infoservice.agreement.interfaces.IAgreementMessage;
import infoservice.agreement.interfaces.IInfoService;
import infoservice.agreement.interfaces.INetworking;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

public class NetworkSimulator implements INetworking
{
    private Hashtable messages = new Hashtable();

    private Thread messenger;

    Semaphore sem;

    public void sendMessage(IInfoService is, IAgreementMessage message)
    {
        if (is == null)
            System.err.println("AAAARGS, my IS is null");

        Vector v = (Vector) this.messages.get(is);
        if (v == null)
        {
            v = new Vector();
            this.messages.put(is, v);
        }
        v.add(message);
    }

    public NetworkSimulator(Semaphore sem1)
    {
        this.sem = sem1;
        messenger = new Thread()
        {
            public void run()
            {
                while (true)
                {
                    try
                    {
                        Thread.sleep(500);
                    } catch (InterruptedException e)
                    {
                        // TODO Auto-generated catch block
                        e.printStackTrace();
                    }

                    sem.acquire();
                    Hashtable mine = new Hashtable();
                    mine.putAll(messages);
                    messages.clear();
                    sem.release();
                    Enumeration en = mine.keys();
                    while (en.hasMoreElements())
                    {
                        AInfoService is = (AInfoService) en.nextElement();
                        Vector v = (Vector) mine.get(is);
                        if (v != null)
                        {
                            for (int i = 0; i < v.size(); i++)
                            {
                                is.handleMessage((IAgreementMessage) v.elementAt(i));
                            }
                        }

                        // System.err.println("Message received.");
                    }
                    // messages.clear();
                    // sem.release();

                }
            }
        };
        messenger.start();
    }

}
