package infoservice.agreement;

import infoservice.agreement.interfaces.IAgreementHandler;

/**
 * @author LERNGRUPPE A thread which represents a timeout clock used for the
 *         agreement timeout.
 */
public class AgreementTimeOutThread extends Thread
{

    /**
     * Long vlue which holds the number of milliseconds.
     */
    protected static final long TIMEOUT = AgreementConstants.AGREEMENT_TIMEOUT;

    /**
     * The corresponding handler. He will be notified when timout occures.
     */
    private IAgreementHandler m_handler;

    /**
     * Holds threads status.
     */
    private boolean m_stopped = false;

    /**
     * Constructs a <code>AgreementTimeOutThread</code>.
     * 
     * @param a_handler
     *            The handler to be notified.
     */
    public AgreementTimeOutThread(IAgreementHandler a_handler)
    {
        this.m_handler = a_handler;
    }

    /**
     * Start the timeout.
     */
    public void run()
    {
        try
        {
            Thread.sleep(TIMEOUT);
        } catch (InterruptedException e)
        {
            e.printStackTrace();
        }
        if (!this.m_stopped)
        {
            this.m_handler.notifyAgreementTimeout();
        }
    }

    /**
     * Stop the timeout.
     * 
     * @param a_agreed
     */
    public void setStopped(boolean a_agreed)
    {
        this.m_stopped = a_agreed;
    }

}