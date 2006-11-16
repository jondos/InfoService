package infoservice.agreement.common;

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
            }
            catch (InterruptedException e)
            {
                //
            }
        }
        count++;
    }

    public synchronized void release()
    {
        count--;
        notify();
    }
}
