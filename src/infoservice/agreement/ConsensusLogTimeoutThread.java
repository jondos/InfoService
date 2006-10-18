package infoservice.agreement;

import infoservice.agreement.interfaces.IConsensusLog;

/**
 * @author LERNGRUPPE A thread which represents a timeout clock used for the log
 *         entry timeout.
 */
public class ConsensusLogTimeoutThread extends Thread
{
    /**
     * Long vlue which holds the number of milliseconds.
     */
    protected static final long TIMEOUT = AgreementConstants.CONSENSUS_LOG_TIMEOUT;

    /**
     * The corresponding handler. He will be notified when timout occures.
     */
    private IConsensusLog m_logEntry;

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
    public ConsensusLogTimeoutThread(IConsensusLog a_handler)
    {
        this.m_logEntry = a_handler;
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
            this.m_logEntry.notifyTimeout();
        }
    }

    /**
     * Stop the timeout if called with <code>true</code> value.
     * 
     * @param a_agreed
     */
    public void setStopped(boolean a_agreed)
    {
        this.m_stopped = a_agreed;
    }

}
