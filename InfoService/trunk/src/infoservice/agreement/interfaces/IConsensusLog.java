package infoservice.agreement.interfaces;

public interface IConsensusLog
{

    public abstract boolean isAgreed();

    public abstract void setAgreed(boolean a_agreed);

    public abstract boolean isComitted();

    public abstract void setComitted(boolean a_comitted);

    /**
     * Stop the time out and close the log entry.
     * 
     */
    public abstract void stopTimeout();

    /**
     * This methode have to be called if the timout occures.
     * 
     */
    public abstract void notifyTimeout();

    public abstract String getInitiatorId();

    public String getConsensusID();

    public abstract boolean isRejected();

    public abstract String getLastCommonRandom();

}