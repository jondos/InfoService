package rmi;

import java.rmi.server.UnicastRemoteObject;
import java.rmi.RemoteException;

/**
 * Title:
 * Description:
 * Copyright:    Copyright (c) 2001
 * Company:
 * @author
 * @version 1.0
 */

public class JAPAnonServiceImpl extends UnicastRemoteObject
                                implements JAPAnonServiceInterface {

    public JAPAnonServiceImpl() throws RemoteException {
        super();
    }

// ----------- local configuration ----------------------------------------

    /** @returns the port where JAP is listening at the moment */
    public int getLocalListeningPort() throws RemoteException {
        return 4001;
    }

    /** @returns whether JAP ist actually listening locally only */
    public boolean getLocallyListeningOnly() throws RemoteException {
        return true;
    }

    /** Set the port where the AnonService is locally listening */
    public boolean setLocalListeningPort(int port) throws RemoteException {
        return true;
    }

    /** Set whether the AnonService is only available for the local computer */
    public boolean setLocallyListeningOnly(boolean listenOnlyLocally) throws RemoteException {
        return true;
    }

// ----------- infoServiceConfiguration -----------------------------------

    /** @returns the actual server name of the infoService */
    public String getInfoServiceServerName() throws RemoteException {
        return "infoservice.inf.tu-dresden.de";
    }

    /** @returns the actual portnumber of the infoService */
    public int getInfoServiceServerPort() throws RemoteException {
        return 6543;
    }

    /** Set the InfoService server name */
    public boolean setInfoServiceServerName(String name) throws RemoteException {
        return true;
    }

    /** Set the InfoService server port */
    public boolean setInfoServiceServerPort(int port) throws RemoteException {
        return true;
    }

// ----------- mixCascadeConfiguration ------------------------------------

    /** @returns all mixCascades that are available in the net. They are loaded via the InfoService. */
    public MixCascade[] loadMixCascadesFromTheNet() throws RemoteException {
        return null;
    }

    /** @returns the currently used mixCascade */
    public MixCascade getMixCascadeCurrentlyUsed() throws RemoteException {
        return new MixCascade("Dresden-Dresden", "mix.inf.tu-dresden.de", 1024);
    }


    /** Change the currently used MixCascade */
    public boolean setMixCascadeCurrentlyUsed(MixCascade mixCascade) throws RemoteException {
        return true;
    }

// ----------- proxyServer configuration ----------------------------------

    /** @returns the actual server name of the infoService */
    public String getProxyServerName() throws RemoteException {
        return "www-proxy";
    }

    /** @returns the actual portnumber of the infoService */
    public int getProxyServerPort() throws RemoteException {
        return 1080;
    }

    /** Set the Proxy server name */
    public boolean setProxyServerName(String name) throws RemoteException {
        return true;
    }

    /** Set the Proxy server port */
    public boolean setProxyServerPort(int port) throws RemoteException {
        return true;
    }
}