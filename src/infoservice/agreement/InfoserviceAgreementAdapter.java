package infoservice.agreement;

import infoservice.Configuration;
import infoservice.HttpResponseStructure;
import infoservice.InfoServiceDistributor;
import infoservice.agreement.interfaces.IAgreementHandler;
import infoservice.agreement.interfaces.IAgreementMessage;
import infoservice.agreement.interfaces.IInfoService;
import infoservice.agreement.multicast.EchoMulticastAgreementHandlerImpl;
import infoservice.agreement.multicast.messages.RawMessage;
import infoservice.dynamic.DynamicCascadeConfigurator;
import infoservice.dynamic.TemporaryCascade;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import HTTPClient.HTTPConnection;
import HTTPClient.HTTPResponse;
import anon.infoservice.Database;
import anon.infoservice.HTTPConnectionFactory;
import anon.infoservice.IDistributable;
import anon.infoservice.InfoServiceDBEntry;
import anon.infoservice.ListenerInterface;

/**
 * @author LERNGRUPPE An adapter for the infoservice to give them the ability to
 *         handle the agreement protocol.
 *
 */
public class InfoserviceAgreementAdapter implements IInfoService
{
    /**
     * A cascade configurator which holds logic for wireing mixes to cascades.
     * He uses the agreement result, the common random number, to do that.
     */
    private DynamicCascadeConfigurator m_dynamicMixConfigurator = new DynamicCascadeConfigurator();

    /**
     * Holds the number of all infoservices and freeze it as long as the
     * agreement runs.
     */
    private int m_numberOfAllActiveInfoservices;

    /**
     * The agreement handler. He encapsuletes all the logic for getting an
     * common random number.
     */
    private IAgreementHandler m_agreementHandler = new EchoMulticastAgreementHandlerImpl(this);

    /**
     * Holds an freeze the infoservice object which are current existing as long
     * as the agreement runs.
     */
    Hashtable m_infoServiceSnapshot = new Hashtable();

    /**
     * A data stack which implements fifo logic used for holding messages.
     */
    private FifoQueue m_queue = new FifoQueue();

    /**
     * The database entry representation for this infoservice.
     */
    InfoServiceDBEntry m_self = null;

    /**
     * A thread which executes the agreement logic.
     */
    private Thread m_agreementStarter = null;

    /**
     * A thread which handles messages for performance reasons.
     */
    private MessageHandlerThread m_messageHandler;

    /**
     * Indicator if an agreement has yet manually been started
     */
    private boolean m_initialized = false;

    /**
     * Indicates if the currently running agreement has also been started (i.e.
     * an <code>InitMessage</code> has been sent) by this InfoService
     */
    private boolean m_started = false;

    /**
     * Generates an <code>InfoServiceDBEntry</code> for this InfoService
     *
     * @return The <code>InfoServiceDBEntry</code>
     */
    InfoServiceDBEntry generateInfoServiceSelf()
    {
        Vector virtualListeners = Configuration.getInstance().getVirtualListeners();
        return new InfoServiceDBEntry(Configuration.getInstance().getOwnName(), Configuration
                .getInstance().getID(), virtualListeners, Configuration.getInstance()
                .holdForwarderList(), false, System.currentTimeMillis(), System.currentTimeMillis());
    }

    /**
     * Creates a new <code>InfoserviceAgreementAdapter</code>. This adapter
     * is used to connect the real InfoService to the agreement extension.
     *
     * @param a_cmds
     *            The connection object to serve network and communication.
     */
    public InfoserviceAgreementAdapter()
    {
        m_self = generateInfoServiceSelf();
        new Thread()
        {
            public void run()
            {
                /*
                 * Make sure we have "real" information about our neighbours
                 * (and they about us) once. Might not be needed in real world,
                 * but overcomes some problems at concurrent startup of multiple
                 * InfoServices (InfoServicePropagandist is too fast then and we
                 * don't want to wait 10 minutes)
                 */
                try
                {
                    Thread.sleep(20000);
                } catch (InterruptedException e)
                {
                    e.printStackTrace();
                }
                InfoServiceDBEntry generatedOwnEntry = generateInfoServiceSelf();
                Database.getInstance(InfoServiceDBEntry.class).update(generatedOwnEntry);
                InfoServiceDistributor.getInstance().addJobToInititalNeighboursQueue(
                        generatedOwnEntry);
                prepareAgreementProtocol();
                LogHolder
                        .log(LogLevel.INFO, LogType.NET, "Now we are ready to start the agreement");
            }
        }.start();
    }

    /**
     * Handles the messages coming in through the /agreement-command. The
     * a_postData is parsed to an IAgreementMessage and then put into the
     * message queue. Messages are only accepted if an agreement is currently
     * running or we are in the timeframe to start an new one
     *
     * @param a_postData
     *            The post data of the request containing an XML encoded
     *            IAgreementMessage
     * @return HTTP_RETURN_INTERNAL_SERVER_ERROR if the message could not be
     *         parsed or the time was not right, HTTP_RETURN_OK otherwise
     */
    public HttpResponseStructure handleMessage(byte[] a_postData)
    {
        HttpResponseStructure httpResponse = new HttpResponseStructure(
                HttpResponseStructure.HTTP_RETURN_INTERNAL_SERVER_ERROR);
        // We only accept messages if an agreement is running (or it is ok to
        // start a new one)
        if (!this.m_agreementHandler.isAgreementRuns())
            return httpResponse;
        RawMessage msg = new RawMessage(a_postData);
        httpResponse = new HttpResponseStructure(HttpResponseStructure.HTTP_RETURN_OK);

        synchronized (m_queue)
        {
            m_queue.push(msg);
            m_queue.notify();
        }
        // If we are prepared, but not yet initialized, then this is the first
        // message ever!
        if (!m_initialized)
        {
            m_initialized = true;
            startAgreementProtocol();
        }
        return httpResponse;
    }

    /**
     * LERNGRUPPE This gets called when the AgreementHandler has reached
     * angreement with the others. When this happens it is time to wire new
     * cascades :-)
     *
     * @param a_newCommonRandomSeed
     *            the agreed-upon random seed
     */
    public void notifyAgreement(Long a_newCommonRandomSeed)
    {
        if (a_newCommonRandomSeed != null)
        {
            info("AGREEMENT REACHED: " + a_newCommonRandomSeed);
            m_dynamicMixConfigurator.buildCascades(a_newCommonRandomSeed.longValue());
        } else
        {
            LogHolder.log(LogLevel.ERR, LogType.ALL, "NO AGREEMENT WAS REACHED, ABORTING");
        }
        m_messageHandler.setRunning(false);
        m_queue.clear();
        m_started = false;
        this.m_agreementStarter = new Thread()
        {
            public void run()
            {
                try
                {
                    Thread.sleep(AgreementConstants.MIN_AGREEMENT_INTERVAL);
                } catch (InterruptedException e)
                {
                    // we cannot do anything, but its evil
                }
                prepareAgreementProtocol();
            }
        };
        this.m_agreementStarter.start();
    }

    /**
     * Prepare the agreement protocol (e.g. freeze status)
     *
     */
    public void prepareAgreementProtocol()
    {
        m_agreementHandler.prepareAgreementsStart();
        // In subsequent agreements we wait for others to reach the starting
        // phase, then start
        // the agreement
        if (m_initialized)
        {
            new Thread()
            {
                public void run()
                {
                    try
                    {
                        Thread.sleep(AgreementConstants.AGREEMENT_PHASE_GAP);
                    } catch (InterruptedException e)
                    {
                        // s
                    }
                    startAgreementProtocol();
                }
            }.start();
        }
    }

    /**
     * Start agreement protocol.
     *
     */
    void startAgreementProtocol()
    {
        if (!m_started)
        {
            m_started = true;

            /* Remove all InfoServiceDBEntries which have no valid IDs */
            Enumeration en = Database.getInstance(InfoServiceDBEntry.class)
                    .getEntrySnapshotAsEnumeration();
            info("MY OWN ID is " + m_self.getId());
            while (en.hasMoreElements())
            {
                InfoServiceDBEntry entry = (InfoServiceDBEntry) en.nextElement();
                if (!entry.checkId() && !m_self.equals(entry))
                {
                    info("Discarting " + entry.getId() + " because its a dummy");
                    Database.getInstance(InfoServiceDBEntry.class).remove(entry);
                } else
                {
                    info("Using InfoService " + entry.getId());
                }
            }

            /*
             * Build a snapshot of the currently known InfoServices to prevent
             * InfoServices coming online during the execution to get messages
             * of the current round
             */
            this.m_numberOfAllActiveInfoservices = Database.getInstance(InfoServiceDBEntry.class)
                    .getNumberOfEntries();
            Enumeration enInfoServices = Database.getInstance(InfoServiceDBEntry.class)
                    .getEntrySnapshotAsEnumeration();

            while (enInfoServices.hasMoreElements())
            {
                InfoServiceDBEntry current = (InfoServiceDBEntry) enInfoServices.nextElement();
                m_infoServiceSnapshot.put(current.getId(), current);
            }
            m_messageHandler = new MessageHandlerThread(m_agreementHandler, m_queue);
            m_messageHandler.start();
            /* Forget all TemporaryCascades if there are some */
            Database.getInstance(TemporaryCascade.class).removeAll();
            this.m_agreementHandler.startAgreementProtocol();
        }
    }

    /**
     * Returns the freezed number of all infoservices which take part on the
     * agreement protocol.
     */
    public int getNumberOfAllInfoservices()
    {
        return this.m_numberOfAllActiveInfoservices;
    }

    /**
     * Returns a unique identifer for this infoservice.
     */
    public String getIdentifier()
    {
        return m_self.getId();
    }

    /**
     * Sends a message to a specified infoservice.
     *
     * @param a_id
     *            The id of the receiver.
     * @param a_message
     *            The message to send.
     */
    public void sendMessageTo(final String a_id, final IAgreementMessage a_message)
    {
        if (a_id.equals(m_self.getId()))
            return;
        /* Send the message asynchronously */
        new Thread()
        {
            public void run()
            {
                sendToInfoService((InfoServiceDBEntry) m_infoServiceSnapshot.get(a_id), a_message);
            }
        }.start();
    }

    /**
     * Sends a message to all known infoservices.
     *
     * @param a_message
     *            The message to send.
     */
    public void multicastMessage(final IAgreementMessage a_message)
    {
        Enumeration infoServices = this.m_infoServiceSnapshot.elements();
        while (infoServices.hasMoreElements())
        {
            final InfoServiceDBEntry entry = (InfoServiceDBEntry) infoServices.nextElement();
            if (entry.getId().equals(m_self.getId()))
                continue;

            /* Send the message asynchronously */
            new Thread()
            {
                public void run()
                {
                    sendToInfoService(entry, a_message);
                }
            }.start();
        }
    }

    /**
     * Encapsulates logic for sending a message to a infoservice.
     *
     * @param a_infoservice
     *            The target infoservice.
     * @param postFile
     * @param postData
     * @return Success if <code>true</code>.
     */
    protected boolean sendToInfoService(InfoServiceDBEntry a_infoservice, IDistributable a_message)
    {
        /*
         * TODO Copied from InfoServiceDistributor, maybe make it public
         * there...
         */
        boolean connected = false;
        if (a_infoservice == null)
        {
            return false;
        }
        Enumeration enumer = a_infoservice.getListenerInterfaces().elements();
        while ((enumer.hasMoreElements()) && (connected == false))
        {
            ListenerInterface currentInterface = (ListenerInterface) (enumer.nextElement());
            if (currentInterface.isValid())
            {
                if (sendToInterface(currentInterface, a_message))
                {
                    connected = true;
                } else
                {
                    //currentInterface.setUseInterface(false);
                }
            }
        }
        return connected;
    }

    /**
     * Encapsulates logic for sending a message to a specified listenere
     * interface.
     *
     * @param a_listener
     *            The network interface.
     * @param postFile
     * @param postData
     * @return Success if <code>true</code>.
     */
    private boolean sendToInterface(ListenerInterface a_listener, IDistributable a_message)
    {
        /*
         * TODO Copied from InfoServiceDistributor, maybe make it public
         * there...
         */
        boolean connected = true;
        HTTPConnection connection = null;
        try
        {
            connection = HTTPConnectionFactory.getInstance().createHTTPConnection(a_listener,
                    a_message.getPostEncoding(), false);
            HTTPResponse response = connection.Post(a_message.getPostFile(), a_message
                    .getPostData());
            int statusCode = response.getStatusCode();
            connected = (statusCode >= 200 && statusCode <= 299);
        } catch (Exception e)
        {
            LogHolder.log(LogLevel.EMERG, LogType.NET, "ERROR WHILE SENDING TO "
                    + connection.getHost() + ":" + connection.getPort() + ": " + e.toString());
            connected = false;
        }
        if (connection != null)
        {
            connection.stop();
        }
        return connected;
    }

    /**
     * An operator may initialize the first agreement calling /startagreement on
     * one of the InfoServices. Subsequent calls to this method are ignored
     */
    public void initializeOnce()
    {
        if (!m_initialized)
        {
            m_initialized = true;
            startAgreementProtocol();
        }
    }

    /**
     * Logs info messages, calls <code>log</code>
     *
     * @param a_message
     */
    void info(String a_message)
    {
        log(LogLevel.INFO, a_message);
    }

    /**
     * Actually logs a message
     *
     * @param a_lvl
     *            The <code>LogLevel</code> to use
     * @param a_message
     *            The message to log
     */
    private void log(int a_lvl, String a_message)
    {
        LogHolder.log(a_lvl, LogType.NET, a_message);
    }

    /* ---------------------- Only needed for Simulator ---------------------- */

    public void handleMessage(IAgreementMessage message)
    {
        // TODO Auto-generated method stub
    }

}
