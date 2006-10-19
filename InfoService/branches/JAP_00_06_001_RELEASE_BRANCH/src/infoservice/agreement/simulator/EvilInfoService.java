package infoservice.agreement.simulator;

import infoservice.agreement.interfaces.IAgreementMessage;

import java.util.Date;
import java.util.Enumeration;
import java.util.Random;

public class EvilInfoService extends AInfoService
{

    private static long seed = new Date().getTime();

    private static Random rand = new Random(seed);

    public EvilInfoService(String id, String prefix, int nodeID)
    {
        super(id, prefix, nodeID);
        System.err.println("SEED: " + seed);
    }

    public void multicastMessage(IAgreementMessage message)
    {
        synchronized (infoservices)
        {
            Enumeration en = infoservices.keys();
            while (en.hasMoreElements())
            {
                AInfoService is = (AInfoService) infoservices.get(en.nextElement());
                if (is != this)
                {

                    if (rand.nextInt(10) < 5)
                    {
                        this.network.sendMessage(is, message);
                    } else
                    {
                        // message.setLastCommonRandom(new String("999999999"));
                        // this.network.sendMessage(is, message);
                    }
                }

            }
        }
    }

    public boolean isEvil()
    {
        return true;
    }

}
