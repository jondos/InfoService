package infoservice.agreement.simulator;

import infoservice.agreement.interfaces.IAgreementHandler;
import infoservice.agreement.interfaces.IAgreementMessage;
import infoservice.agreement.interfaces.IInfoService;
import infoservice.agreement.multicast.EchoMulticastAgreementHandlerImpl;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import org.bouncycastle.crypto.digests.SHA1Digest;

import anon.util.Base64;

public abstract class AInfoService extends Thread implements LogHolder, IInfoService
{
    protected static Hashtable infoservices = new Hashtable();

    protected static int agreements = 0;

    protected String identifier;

    public int nodeID = -1;

    private String prefix = "";

    private IAgreementHandler agreementHandler;

    private static Object lock;

    protected NetworkSimulator network;

    protected Hashtable randoms;

    private Semaphore sem;

    static String lastCommonRandom = "123";

    public static void resetAll()
    {
        infoservices.clear();
    }

    public abstract boolean isEvil();

    public AInfoService(String id, String prefix1, int nodeID)
    {
        this.identifier = id;
        infoservices.put(getIdentifier(), this);
        this.prefix = prefix1;
        // new CastroLiskovAgreementHandlerImpl(this);
        this.nodeID = nodeID;

    }

    public String getIdentifier()
    {
        if (this.isEvil())
            return this.identifier + "[e]";
        else
            return this.identifier + "[g]";
    }

    // public void handleMessage(IMessage message) throws
    // IllegalArgumentException {
    // this.agreementHandler.handleMessage(message);
    // }

    public void startProtocol()
    {
        // int view = this.agreementHandler.getView();
        // if (getNodeID() == view % infoservices.size())
        this.agreementHandler.prepareAgreementsStart();
        this.agreementHandler.startAgreementProtocol();
    }

    public String getPrefix()
    {
        return this.prefix;
    }

    public int getNodeID()
    {
        return this.nodeID;
    }

    public void multicastMessage(IAgreementMessage message)
    {
        synchronized (infoservices)
        {
            Enumeration en = infoservices.keys();
            while (en.hasMoreElements())
            {
                IInfoService is = (IInfoService) infoservices.get(en.nextElement());
                if (is != this)
                {
                    this.network.sendMessage(is, message);
                }
            }
        }
    }

    public void sendMessageTo(String nodeId, IAgreementMessage echoMessage)
    {
        AInfoService is = (AInfoService) infoservices.get(nodeId);
        this.network.sendMessage(is, echoMessage);
    }

    public void notifyAgreement(Long a_agreementResult)
    {
        System.out.println(this.getIdentifier() + "> " + a_agreementResult);
    }

    public static synchronized void initialize()
    {
        Vector ids = new Vector();
        Enumeration en = infoservices.keys();
        while (en.hasMoreElements())
        {
            ids.add(en.nextElement());
        }

        en = infoservices.elements();
        while (en.hasMoreElements())
        {
            AInfoService elem = (AInfoService) en.nextElement();
            elem.agreementHandler = new EchoMulticastAgreementHandlerImpl(elem);
        }
    }

    public static String analyse()
    {

        AInfoService master = (AInfoService) infoservices.get(new Integer(0));
        Enumeration en = master.getRandoms().keys();
        while (en.hasMoreElements())
        {
            String consensus = (String) en.nextElement();
            StringBuffer line = new StringBuffer();
            line.append("Consens " + consensus + ":");
            for (int i = 0; i < infoservices.size(); i++)
            {
                AInfoService is = ((AInfoService) infoservices.get(new Integer(i)));
                Long value = (Long) is.getRandoms().get(consensus);
                if (value != null)
                {
                    line.append(is.getIdentifier() + "  : " + Math.abs(value.longValue() % 1000));
                } else
                {
                    line.append(is.getIdentifier() + "  : ...");
                }
                line.append("   ");
            }
            return line.toString();

        }
        return "AInfoservice.analyse(): There are not enough agreeements.";
    }

    public void setLock(Object lock1)
    {
        this.lock = lock1;
    }

    public void setNetwork(NetworkSimulator sim)
    {
        this.network = sim;
    }

    public static void startAgreement()
    {
        AInfoService infoservice = null;
        Enumeration en = infoservices.elements();
        while (en.hasMoreElements())
        {
            infoservice = (AInfoService) en.nextElement();
            if (infoservice.isEvil())
            {
                infoservice.getAgreementHandler().setLastCommonRandom("666");
                infoservice.startProtocol();
            } else
            {
                infoservice.startProtocol();
            }
        }
        // ((AInfoService)infoservices.elements().nextElement()).startProtocol();

    }

    public int getNumberOfAllInfoservices()
    {
        return infoservices.size();
    }

    public Hashtable getRandoms()
    {
        return randoms;
    }

    public void setSemaphore(Semaphore sem)
    {
        this.sem = sem;
    }

    public void handleMessage(IAgreementMessage message)
    {
        this.agreementHandler.handleMessage(message);
    }

    public IAgreementHandler getAgreementHandler()
    {
        return agreementHandler;
    }

    public String getHashFromString(String a_string)
    {
        SHA1Digest digest = new SHA1Digest();
        byte[] proposalBytes = a_string.getBytes();
        digest.update(proposalBytes, 0, proposalBytes.length);
        byte[] tmp = new byte[digest.getDigestSize()];
        digest.doFinal(tmp, 0);
        return Base64.encode(tmp, false);
    }

}
