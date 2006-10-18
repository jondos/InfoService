package infoservice.agreement.simulator;

import infoservice.agreement.AgreementConstants;

import java.util.Date;

public class AgreementSimulator implements LogHolder
{
    Object lock = new Object();

    Semaphore sem = new Semaphore();

    NetworkSimulator sim = new NetworkSimulator(sem);

    public AgreementSimulator()
    {
        Logger.getInstance().init();
        // Logger.getInstance().log(this, "started");
        System.out.println("Simulator is started ...");
        for (int i = 0; i < 1; i++)
        {
            Date start = new Date();
            AInfoService.resetAll();
            System.gc();
            Logger.getInstance().step();
            String prefix = createGoodInfoServices(3, 0);
            createEvilInfoServices(3, 3);
            createGoodInfoServices(4, 6);
            AInfoService.initialize();
            AInfoService.startAgreement();

            // prefix = createEvilInfoServices(bad, prefix);
            prefix += "\t";
            // InfoService starter = new GoodInfoService("Starter", net, false);
            synchronized (lock)
            {
                try
                {
                    lock.wait();
                } catch (InterruptedException e)
                {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
            }
            Date end = new Date();
            Logger.getInstance().log(
                    this,
                    "needed " + ((end.getTime() - start.getTime()) / 1000)
                            + " seconds to reach agreement");
        }
        Logger.getInstance().log(this, "end");
        Logger.getInstance().flush();
        // A new Round will be staret

        Thread sleeper = new Thread()
        {
            public void run()
            {
                try
                {
                    Thread.sleep(AgreementConstants.AGREEMENT_PHASE_GAP);
                } catch (InterruptedException e)
                {
                    // not good, but nothing we can do about it now
                }
                System.err.println("STARTE NEUE .................. RUNDE!!!! "
                        + new Date().toLocaleString());
                AInfoService.startAgreement();
            }
        };
        sleeper.start();
    }

    private String createGoodInfoServices(int count, int start)
    {
        String prefix = "";
        String id = "Node ";
        for (int i = start; i < count + start; i++)
        {
            // prefix += "\t";
            AInfoService j = new GoodInfoService(id + i, prefix, i);
            j.setLock(lock);
            j.setNetwork(this.sim);
            j.setSemaphore(this.sem);
        }
        return prefix;

    }

    private String createEvilInfoServices(int c, int start)
    {
        String id = "Node ";
        for (int i = start; i < c + start; i++)
        {
            // prefix += "\t";
            EvilInfoService j = new EvilInfoService((id + i).toString(), " ", i);
            j.setLock(this.lock);
            j.setNetwork(this.sim);
            j.setSemaphore(this.sem);
        }
        return "";
    }

    public String getIdentifier()
    {
        return "Simulator";
    }

    public static void main(String[] args)
    {
        new AgreementSimulator();
    }

    public String getPrefix()
    {
        return "";
    }
}
