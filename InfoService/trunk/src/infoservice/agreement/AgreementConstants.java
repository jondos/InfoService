package infoservice.agreement;

/**
 * @author LERNGRUPPE A set of used constants.
 */
public interface AgreementConstants
{

    /* Maximum time one phase of the agreement may take */
    long AGREEMENT_TIMEOUT = 30000;

    /*
     * The time to sleep after phase one to allow others to reach the next phase
     * too
     */
    long AGREEMENT_PHASE_GAP = 10000;

    /* Maximum time a single agreement (i.e. a broadcast) may take */
    long CONSENSUS_LOG_TIMEOUT = 20000;

    /* Minimum time between two agreement instances */
    // long MIN_AGREEMENT_INTERVAL = 1000 * 60 * 60 * 24; // 24 hours
    long MIN_AGREEMENT_INTERVAL = 300000;

    /* The start and default round number for the agreement protocol */
    String DEFAULT_COMMON_RANDOM = "0000000000";

}
