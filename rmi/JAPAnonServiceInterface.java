package rmi;

import java.rmi.Remote;
import java.rmi.RemoteException;


public interface JAPAnonServiceInterface extends Remote {
    // Configuration for the local computer
    public int     getLocalListeningPort()   throws RemoteException;
    public boolean getLocallyListeningOnly() throws RemoteException;
    public boolean setLocalListeningPort(int port) throws RemoteException;
    public boolean setLocallyListeningOnly(boolean listenOnlyLocally) throws RemoteException;

    // Configuration for the Info Service
    public String  getInfoServiceServerName() throws RemoteException;
    public int     getInfoServiceServerPort() throws RemoteException;
    public boolean setInfoServiceServerName(String name) throws RemoteException;
    public boolean setInfoServiceServerPort(int port) throws RemoteException;

    // Configuration for the mixCascades
    public MixCascade[] loadMixCascadesFromTheNet() throws RemoteException;
    public MixCascade   getMixCascadeCurrentlyUsed() throws RemoteException;
    public boolean setMixCascadeCurrentlyUsed(MixCascade mixCascade) throws RemoteException;

    // Configuration for a proxy
    public String  getProxyServerName() throws RemoteException;
    public int     getProxyServerPort() throws RemoteException;
    public boolean setProxyServerName(String name) throws RemoteException;
    public boolean setProxyServerPort(int port) throws RemoteException;
}