package infoservice.agreement.simulator;

public class Semaphore
{

    int count = 0;

    public synchronized void acquire()
    {
        while (count > 0)
        {
            try
            {
                wait();
            } catch (InterruptedException e)
            {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
        }
        this.count++;
    }

    public synchronized void release()
    {
        count--;
        notify();
    }
}
