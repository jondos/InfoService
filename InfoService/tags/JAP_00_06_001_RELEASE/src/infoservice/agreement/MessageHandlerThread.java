package infoservice.agreement;

import infoservice.agreement.interfaces.IAgreementHandler;
import infoservice.agreement.multicast.messages.RawMessage;

public class MessageHandlerThread extends Thread
{

    private boolean m_bRunning = true;

    private IAgreementHandler m_agreementHandler = null;

    private FifoQueue m_queue = null;

    /**
     * LERNGRUPPE Creates a new <code>MessageHandler</code>. When notified it
     * will pop a message off the given queue and call the given handler to
     * handle it.
     * 
     * @param a_handler
     *            The ArgreementHandler to handle the messages
     * @param a_queue
     *            A queue of messages
     */
    public MessageHandlerThread(IAgreementHandler a_handler, FifoQueue a_queue)
    {
        this.m_agreementHandler = a_handler;
        this.m_queue = a_queue;
    }

    /*
     * (non-Javadoc)
     * 
     * @see java.lang.Runnable#run()
     */
    public void run()
    {
        while (m_bRunning)
        {
            RawMessage msg = null;
            synchronized (m_queue)
            {
                msg = m_queue.pop();
            }
            while (msg != null)
            {
                m_agreementHandler.handleMessage(msg.getAgreementMessage());
                synchronized (m_queue)
                {
                    msg = m_queue.pop();
                }
            }
            synchronized (m_queue)
            {
                try
                {
                    m_queue.wait();
                } catch (InterruptedException e)
                {
                    // can be ignored
                }
            }
        }
    }

    /**
     * LERNGRUPPE Sets the m_bRunning member
     * 
     * @param a_bRunning
     *            the new value of m_bRunning
     */
    public void setRunning(boolean a_bRunning)
    {
        this.m_bRunning = a_bRunning;
    }

}
