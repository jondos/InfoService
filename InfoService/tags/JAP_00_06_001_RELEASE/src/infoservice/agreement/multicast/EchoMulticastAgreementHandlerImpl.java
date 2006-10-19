package infoservice.agreement.multicast;

import infoservice.agreement.AgreementConstants;
import infoservice.agreement.AgreementMessageTypes;
import infoservice.agreement.AgreementTimeOutThread;
import infoservice.agreement.CommitmentMessage;
import infoservice.agreement.interfaces.IAgreementHandler;
import infoservice.agreement.interfaces.IAgreementMessage;
import infoservice.agreement.interfaces.IConsensusLog;
import infoservice.agreement.interfaces.IInfoService;
import infoservice.agreement.logging.FileLogger;
import infoservice.agreement.multicast.messages.AMessage;
import infoservice.agreement.multicast.messages.CommitMessage;
import infoservice.agreement.multicast.messages.ConfirmationMessage;
import infoservice.agreement.multicast.messages.EchoMessage;
import infoservice.agreement.multicast.messages.InitMessage;
import infoservice.agreement.multicast.messages.RejectMessage;

import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import java.text.DateFormat;

/**
 * @author LERNGRUPPE This class represents a dealer witch takes care about all
 *         things around the agreement for generating a common random number
 *         used as input for the wireing-algorithm.
 */
public class EchoMulticastAgreementHandlerImpl implements IAgreementHandler
{
    /**
     * Contains the reached agreement results (from infoservice1, infoservice2
     * ...) of the current phase. Notize: ther are two phases.
     */
    private Hashtable m_currentAgreementResults = new Hashtable();

    /**
     * Contains the reached agreement results of agreement phase one.
     */
    private Hashtable m_phaseOneAgreemetResults = new Hashtable();

    /**
     * The infoservice we are bundled. Needed for communications etc.
     */
    private IInfoService m_infoService;

    /**
     * A little logger to write out log informations to filesystem.
     */
    private FileLogger m_logger;

    /**
     * Holds the last agreed random number or an init value at start up.
     */
    private String m_lastCommonRandom;

    /**
     * Saves the log entries which hold the checked messages.
     */
    private Hashtable m_logHashTable = new Hashtable();

    /**
     * A thread used for giving time out signal.
     */
    private AgreementTimeOutThread m_agreementTimeOutThread = null;

    /**
     * The CommitmentMessage is not a part of the reliable broadcast protocol.
     * It is used by the implicit commitment scheme. It holds a commitment
     * message.
     */
    private CommitmentMessage m_commitmentMessage = null;

    /**
     * This is the current massage (proposal) which is actual performed by the
     * agreement protocol. Is is not part of the reliable broadcast protocol
     * itself. It's just the message.
     */
    protected String m_currentMessage = "";

    /**
     * Determines whether the agreement time out thrtead is just started.
     */
    private boolean m_agreementTimeoutThreadIsStarted = false;

    /**
     * Determines whether the agreement protocol is active runnig. Notize: The
     * passiv part is just running.
     */
    private boolean m_agreementIsStillRunning = false;

    /**
     * Indicates the current phase of the commitment scheme.
     */
    private int m_currentPhase = 0;

    /**
     * Creates an agereement handler witch runs the
     * multycast-agreement-protocol.
     *
     * @param a_infoService
     *            The infoservice belongs to.
     */
    public EchoMulticastAgreementHandlerImpl(IInfoService a_infoService)
    {
        this.m_infoService = a_infoService;
        this.m_logger = new FileLogger(this.m_infoService);
        this.m_lastCommonRandom = AgreementConstants.DEFAULT_COMMON_RANDOM;
    }

    /**
     * This is the main handle point for incomming messages according to this
     * agreement protocol.
     *
     * @param a_msg
     *            The incomming message.
     */
    public void handleMessage(IAgreementMessage a_msg) throws IllegalArgumentException
    {
        if (a_msg == null)
            return;
        if (this.m_agreementIsStillRunning)
        {
            switch (a_msg.getMessageType())
            {
                case AgreementMessageTypes.MESSAGE_TYPE_INIT:
                    handleInitMessage((InitMessage) a_msg);
                    break;
                case AgreementMessageTypes.MESSAGE_TYPE_ECHO:
                    handleEchoMessage((EchoMessage) a_msg);
                    break;
                case AgreementMessageTypes.MESSAGE_TYPE_COMMIT:
                    handleCommitMessage((CommitMessage) a_msg);
                    break;
                case AgreementMessageTypes.MESSAGE_TYPE_REJECT:
                    handleRejectMessage((RejectMessage) a_msg);
                    break;
                case AgreementMessageTypes.MESSAGE_TYPE_CONFIRMATION:
                    handleConfirmationMessage((ConfirmationMessage) a_msg);
                    break;
            }
        } else
        {
            debug(" ----- DISCARD INCOMMING MESSAGE CAUSE AGREEMENT IS OVER " + a_msg);
        }

    }

    /**
     * This method handles all incomming confirmation-messages. It maps the
     * massage on a
     * <code>CommitMessage</CommitMessage> if no agreement is reached yet and perform
     * handleCommitMessage(..).
     *
     * @param a_message
     *            The message.
     */
    private void handleConfirmationMessage(ConfirmationMessage a_message)
    {
        ConsensusLogEchoMulticast consensus = getConsensusLog(a_message);
        /* if we are agreed, there is no need in performing that */
        if (consensus.isAgreed())
            return;
        /*
         * Otherwise we can handle it like a common CommitMessage to reach an
         * agreement
         */
        a_message.changeIntoCommitMessage();
        this.handleCommitMessage(a_message);
    }

    /**
     * This method handles all incomming reject-messages. This messagetype will
     * be sent by nodes for receiving invalid init-messages. Especially in the
     * case of incorrect agreement-identifier <code>lastCommonRandom</code>.
     *
     * @param a_message
     *            The message.
     */
    private void handleRejectMessage(RejectMessage a_message)
    {
        /* create standard log */
        ConsensusLogEchoMulticast consensus = getConsensusLog(a_message);
        consensus.addRejectMessage(a_message);

        debug("A: handleRejectMessage: rejected=" + consensus.isRejected() + " restarted="
                + consensus.isRestarted());

        if (consensus.isRejected() && consensus.getLastCommonRandom() == null)
        {
            notifyBabylonianConfusion();
            return;
        }

        if (consensus.isRejected() && !consensus.isRestarted())
        {
            consensus.setRestarted(true);
            consensus.stopTimeout();
            this.m_lastCommonRandom = consensus.getLastCommonRandom();
            /*
             * We can reinit using the majority vote for the last common random
             * Create an InitMessage and broadcast it
             */
            InitMessage msg = new InitMessage(this.m_infoService.getIdentifier(),
                    this.m_currentMessage, this.m_lastCommonRandom);

            /* get the new log */
            getConsensusLog(msg);

            debug(" ----> I GOT A NEW CHANCE TO SEND InitMessage TO ALL " + msg);
            debug(" ----> SEND initmessage TO ALL " + msg);

            handleInitMessage(msg);
            this.m_infoService.multicastMessage(msg);
        }
    }

    /**
     * If an InitMessage received, it will be handled by two steps. At first we
     * start the agreement timeout thread, than we check whether the currend
     * agreement id is part of this message. If so, we create a log entry an
     * send a echo message, otherwise we send a reject message.
     *
     * @param a_message
     *            The InitMessage.
     */
    private void handleInitMessage(InitMessage a_message)
    {
        /* At first we start the AgreementTimeOutThread */
        if (this.m_agreementTimeOutThread == null && !this.m_agreementTimeoutThreadIsStarted)
        {
            this.m_agreementTimeoutThreadIsStarted = true;
            this.m_agreementTimeOutThread = new AgreementTimeOutThread(this);
            this.m_agreementTimeOutThread.start();
        }

        /*
         * Is InitMessages lastCommonRandom correct? If not, create
         * RejectMessage
         */
        if (!a_message.getLastCommonRandom().equals(this.m_lastCommonRandom))
        {
            RejectMessage msg = new RejectMessage(a_message, this.m_infoService.getIdentifier(),
                    this.m_lastCommonRandom);
            this.m_infoService.sendMessageTo(a_message.getInitiatorsId(), msg);

            debug(" ----> REJECT  InitMessage FROM " + a_message.getInitiatorsId() + " ("
                    + a_message.getLastCommonRandom() + "!=" + this.m_lastCommonRandom + ") "
                    + a_message);
            return;
        }
        /*
         * Create or get the corresponding consensus log entry for this
         * InitMessage and add it
         */
        ConsensusLogEchoMulticast consensus = getConsensusLog(a_message);
        /* try to add */
        if (!consensus.addInitMessage(a_message))
        {
            return;
        }
        // info("RECEIVED INIT MESSAGE FROM : " + a_message.getInitiatorsId());
        /* create Echo-Message */
        EchoMessage echoMessage = null;
        echoMessage = new EchoMessage(a_message, this.getInfoService().getIdentifier());

        /* return to sender */
        if (a_message.getInitiatorsId().equals(this.getInfoService().getIdentifier()))
            this.handleEchoMessage(echoMessage);
        this.m_infoService.sendMessageTo(echoMessage.getInitiatorsId(), echoMessage);

        debug(" ----> SEND EchoMessage TO NODE " + echoMessage.getInitiatorsId() + " " + a_message);
    }

    /**
     * If an EchoMessage received, it will be handled by two steps. At first we
     * add it in the log-hashtable and at second there is to decide whether
     * there are enough EchoMessages collected for executing a CommitMessage.
     *
     * @param a_message
     *            the EchoMessage
     */
    private void handleEchoMessage(EchoMessage a_message)
    {
        /*
         * If I'm not the initiator ... there is no need in handling this
         * message
         */
        if (!a_message.getInitiatorsId().equals(this.getInfoService().getIdentifier()))
        {
            debug(" ----> IGNORE EchoMessage (I'm not the sender.)");
            return;
        }

        ConsensusLogEchoMulticast consensus = getConsensusLog(a_message);
        if (consensus.isAgreed())
        {
            return;
        }

        if (!consensus.addEchoMessage(a_message))
        {
            return;
        }
        // info("RECEIVED ECHO MESSAGE FROM : " + a_message.getSenderId());
        CommitMessage commit = consensus.tryToCreateACommitMessage();
        if (commit == null)
        {
            return;
        }

        debug(" ----> SEND CommitMessage TO ALL  " + commit);

        handleCommitMessage(commit);
        this.m_infoService.multicastMessage(commit);
    }

    /**
     * Check and add a CommitMessage. If Ok, create and send a
     * ConfirmationMessage to all.
     *
     * @param a_message
     *            The message
     */
    private void handleCommitMessage(CommitMessage a_message)
    {
        ConsensusLogEchoMulticast consensus = getConsensusLog(a_message);

        // if the consensus is already agreed and confirmed -> do nothing
        if (consensus.isAgreed() || consensus.isComitted())
        {
            return;
        }

        if (!consensus.addCommitMessage(a_message))
        {
            return;
        }
        /* At this point we hold an agreement and should it add */
        this.m_currentAgreementResults.put(a_message.getInitiatorsId(), a_message.getProposal());

        debug(" ----> AGREEMENT REACHED " + this.m_currentAgreementResults.size() + " -> "
                + this.getInfoService().getNumberOfAllInfoservices() + " " + a_message);

        /* Our own */
        if (a_message.getInitiatorsId().equals(this.m_infoService.getIdentifier()))
        {
            consensus.setComitted(true);
        }
        // info("RECEIVED ECHO MESSAGE FROM : " + a_message.getSenderId());
        if (!consensus.isComitted())
        {
            /* Now we create a ConfirmationMessage for informing the others */
            ConfirmationMessage msg = new ConfirmationMessage(a_message, m_infoService
                    .getIdentifier());

            debug(" ----> SEND ConfirmationMessage TO ALL  " + msg);

            consensus.setComitted(true);
            this.getInfoService().multicastMessage(msg);

        }
        int count = 0;
        Enumeration en = m_logHashTable.elements();
        while (en.hasMoreElements())
        {
            ConsensusLogEchoMulticast l = (ConsensusLogEchoMulticast) en.nextElement();
            if (l.isComitted())
                count++;
        }
        info("HAVE " + count + " COMMITS, NEED " + m_infoService.getNumberOfAllInfoservices());
    }

    /**
     * Call this methode to initialize the agreement protocol.
     *
     * @param lastRandom
     *            Should be the common random number from the las agreement or
     *            an initial value shared by all infoservices.
     */
    public void startAgreementProtocol()
    {
        /*
         * First round. Create a CommitmentMessage
         */
        this.m_commitmentMessage = new CommitmentMessage();
        /* set concrete message (as string representation) for this round (!!!) */
        this.m_currentMessage = this.m_commitmentMessage.getHashValueAndRandomOne();
        /* Start first round */
        this.m_currentPhase = 1;
        /*
         * System.out.println(this.getInfoService().getIdentifier() + "> Start
         * agreement phase 1 now ... " + this.m_currentMessage);
         */
        sendReliableBroadcastMessage(this.m_currentMessage);
    }

    /**
     * Starts the reliable broadcast and performs the given message.
     *
     * @param a_message
     *            The message to send.
     */
    protected void sendReliableBroadcastMessage(String a_message)
    {
        /* Create an InitMessage and broadcast it */
        InitMessage msg = new InitMessage(this.m_infoService.getIdentifier(), a_message,
                this.m_lastCommonRandom);
        /* Add message to the consensus log */
        getConsensusLog(msg);

        debug(" ----> SEND initmessage TO ALL " + msg);

        handleInitMessage(msg);
        this.m_infoService.multicastMessage(msg);
    }

    /**
     * Initialize some variables and reset the time out thread.
     */
    public void prepareAgreementsStart()
    {
        this.m_agreementTimeOutThread = null;
        this.m_agreementTimeoutThreadIsStarted = false;
        this.m_agreementIsStillRunning = true;
        this.m_currentAgreementResults = new Hashtable();
        if (m_currentPhase != 1)
        {// we need the results in phase 2
            this.m_phaseOneAgreemetResults = new Hashtable();
        }
        m_logHashTable = new Hashtable();
        m_currentMessage = "";
    }

    /**
     * Gets the appropriate consensus log entry by consensus id or, if not yet
     * in the store, creates a new one and adds is to the store.
     *
     * @param a_msg
     *            The message, which contains all informations to construct a
     *            log id.
     * @return The appropriate consensus log or a new one.
     */
    private synchronized ConsensusLogEchoMulticast getConsensusLog(AMessage a_msg)
    {
        synchronized (this.m_logHashTable)
        {
            ConsensusLogEchoMulticast consensusLog = (ConsensusLogEchoMulticast) this.m_logHashTable
                    .get(a_msg.getConsensusId());

            if (consensusLog == null)
            {
                consensusLog = new ConsensusLogEchoMulticast(this, a_msg.getConsensusId(),
                        this.m_logger, a_msg.getInitiatorsId());
                this.m_logHashTable.put(a_msg.getConsensusId(), consensusLog);
            }
            return consensusLog;
        }
    }

    /**
     * This method is called by the <code>ConsensusLogTimeoutThread</code> at
     * the moment of expiration.
     *
     * @param consensus
     *            The log entry.
     */
    public void notifyConsensusLogTimeout(IConsensusLog consensus)
    {
        info(" ----- CONSENSUSLOG TIMOUT FOR " + consensus.getInitiatorId());
    }

    /**
     * If something really really bad happens, that is if the InfoServices
     * massively disagree, we need help
     */
    private void notifyBabylonianConfusion()
    {
        debug("BABYLONIAN CONFUSION!!");
        // TODO Send mail to an operator
    }

    /**
     * This method is called if the complete agreement timed out.
     */
    public void notifyAgreementTimeout()
    {
        this.m_agreementIsStillRunning = false;
        debug(" AGREEMENT TIMED OUT");

        // Just for debugging purposes
        info(" AGREEMENT TIMED OUT: " + DateFormat.getDateTimeInstance().format(new Date()));

        this.m_agreementTimeOutThread.setStopped(true);

        switch (m_currentPhase)
        {
            case 1:
            {
                /*
                 * Let's start phase 2 save the results fro phase 1
                 */
                this.m_phaseOneAgreemetResults = this.m_currentAgreementResults;
                this.prepareAgreementsStart();
                /* create the message for phase 2 */
                this.m_currentMessage = this.m_commitmentMessage.getConcatenation();
                // System.out.println(this.getInfoService().getIdentifier() + ">
                // Start agreement phase 2 now ... "
                // + this.m_currentMessage);
                /* set second round number and start */
                this.m_currentPhase = 2;

                /* Wait to give others the chance to reach the timeout too */
                new Thread()
                {
                    public void run()
                    {
                        try
                        {
                            Thread.sleep(AgreementConstants.AGREEMENT_PHASE_GAP);
                        } catch (InterruptedException e)
                        {
                            /* not good, but nothing we can do about it now */
                        }
                        // Just for debugging purposes
                        info("Start new round now!!!! " + DateFormat.getDateTimeInstance().format(new Date()));
                        sendReliableBroadcastMessage(m_currentMessage);
                    }
                }.start();

                break;
            }
            case 2:
            {
                /* Set the new common random value */
                Long newCommonRandom = this.checkTheResults();
                if (newCommonRandom != null)
                {
                    this.m_lastCommonRandom = newCommonRandom.toString();
                }
                /* reset phase to zero */
                this.m_currentPhase = 0;
                /* call the infoservice */
                m_infoService.notifyAgreement(newCommonRandom);
                break;
            }
        }
    }

    /**
     * Evaluate the results, combine the random numbers of all infoservices and
     * return this new common random value or null.
     *
     * @return New common random value or null.
     */
    private Long checkTheResults()
    {
        Hashtable results = new Hashtable();
        Object key = null;
        String value1 = null;
        String value2 = null;
        CommitmentMessage cmMessage = null;
        String hash = "";
        String randNumber = "";
        Enumeration en = this.m_phaseOneAgreemetResults.keys();
        while (en.hasMoreElements())
        {
            key = en.nextElement();
            if (this.m_currentAgreementResults.containsKey(key))
            {
                value1 = (String) this.m_phaseOneAgreemetResults.get(key);
                value2 = (String) this.m_currentAgreementResults.get(key);
                if (value1 == null || value2 == null)
                {
                    debug("SKIPPING null VALUE");
                    continue;
                }
                try
                {
                    cmMessage = new CommitmentMessage(value2);
                    randNumber = CommitmentMessage
                            .extractRandomOneFromHashAndRandomOneConcatenation(value1);
                    hash = CommitmentMessage.extractHashFromHashAndRandomOneConcatenation(value1);
                } catch (Exception e)
                {
                    e.printStackTrace();
                    continue;
                }
                /* check the randomOne number */
                if (!randNumber.equals(cmMessage.getRandomOne()))
                {
                    debug("check the randomOne number failed" + randNumber + " -> "
                            + cmMessage.getRandomOne());
                    continue;
                }
                /* check hash values */
                if (!hash.equals(cmMessage.getHashCode()))
                {
                    debug("check hash values failed" + hash + "-> " + cmMessage.getHashCode());
                    continue;
                }
                debug("AGREEMENT FOR: " + key + ": " + cmMessage.getProposal());
                results.put(key, cmMessage.getProposal());
            }
        }
        int cm = ((this.getInfoService().getNumberOfAllInfoservices() * 2) + 1) / 3;
        if (results.size() < cm)
        {
            debug("ATTENTION! AGREEMENT FINISHED WITHOUT A  RESULT. CRITICAL MASS: " + cm
                    + " REACHED RESULTS: " + results.size());
            return null;
        }
        /* Calculate the CommonRandom */
        long comRand = 0l;
        Enumeration emu = results.elements();
        while (emu.hasMoreElements())
        {
            comRand += Long.valueOf(((String) emu.nextElement())).longValue();
        }
        return Long.valueOf(comRand);
    }

    /*
     * (non-Javadoc)
     *
     * @see infoservice.agreement.interfaces.IAgreementHandler#isAgreementRuns()
     */
    public boolean isAgreementRuns()
    {
        return m_agreementIsStillRunning;
    }

    /**
     * Sets a value which determines whether the agreement protocol is active
     * runnig. Notize: The passiv part can just running.
     *
     * @param isStillRunning
     *            Value to set.
     */
    public void setAgreementIsStillRunning(boolean isStillRunning)
    {
        m_agreementIsStillRunning = isStillRunning;
    }

    /**
     * Gets he value which holds the last agreed random number or an init value
     * at start up.
     *
     * @return
     */
    public String getLastCommonRandom()
    {
        return m_lastCommonRandom;
    }

    /*
     * (non-Javadoc)
     *
     * @see infoservice.agreement.interfaces.IAgreementHandler#setLastCommonRandom(java.lang.String)
     */
    public void setLastCommonRandom(String commonRandom)
    {
        m_lastCommonRandom = commonRandom;
    }

    /**
     * Logs debug messages
     *
     * @param a_message
     *            The message to log.
     */
    private void debug(String a_message)
    {
        log(LogLevel.DEBUG, this.m_infoService.getIdentifier() + a_message);
    }

    /**
     * Logs info messages
     *
     * @param a_message
     */
    void info(String a_message)
    {
        log(LogLevel.INFO, a_message);
    }

    private void log(int a_lvl, String a_message)
    {
        LogHolder.log(a_lvl, LogType.NET, a_message);
        if (this.m_logger != null)
            this.m_logger.writeOut(a_message);
    }

    /*
     * (non-Javadoc)
     *
     * @see infoservice.agreement.interfaces.IAgreementHandler#getInfoService()
     */
    public IInfoService getInfoService()
    {
        return this.m_infoService;
    }

}
