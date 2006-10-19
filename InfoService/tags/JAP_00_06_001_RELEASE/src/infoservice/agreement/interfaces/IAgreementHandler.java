package infoservice.agreement.interfaces;

/**
 * @author LERNGRUPPE Provides a set of methods for achieving an agreement
 *         between all infoservices.
 */

public interface IAgreementHandler
{

    /**
     * Handles incomming <code>IAgreementMessage</code>.
     * 
     * @param a_msg
     *            The given message.
     * @throws IllegalArgumentException
     */
    public void handleMessage(IAgreementMessage a_msg) throws IllegalArgumentException;

    /**
     * Starts the agreement protocol. Call <code>prepareAgreementsStart()</code>
     * befor and wait a minute.
     */
    public void startAgreementProtocol();

    /**
     * Gets the infoservice which belongs to this agreement handler.
     * 
     * @return
     */
    public IInfoService getInfoService();

    /**
     * This method will be called if a log entry times out. It is used for
     * getting status information only.
     * 
     * @param a_log
     *            The log entry.
     */
    public void notifyConsensusLogTimeout(IConsensusLog a_log);

    /**
     * This method will be called if an agreement times out. At this moment
     * there is knowledge about success or not. For commitment-scheme purpose it
     * will be called twice. One time in each commitment phase.
     */
    public void notifyAgreementTimeout();

    /**
     * This is needes for testing purposes only.
     * 
     * @param a_commonRandom
     *            The value of the current round number.
     */
    public void setLastCommonRandom(String a_commonRandom);

    /**
     * This activates the agreement protocol in passiv mode, whitch means that
     * incomming messages will be handled but we do not start our own
     * <code>InitMessage</code.
     */
    public void prepareAgreementsStart();

    /**
     * @return Whether the agreement is just running.
     */
    public boolean isAgreementRuns();
}
