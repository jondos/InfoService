/*
Copyright (c) 2000, The JAP-Team
All rights reserved.
Redistribution and use in source and binary forms, with or without modification,
are permitted provided that the following conditions are met:

	- Redistributions of source code must retain the above copyright notice,
	  this list of conditions and the following disclaimer.

	- Redistributions in binary form must reproduce the above copyright notice,
	  this list of conditions and the following disclaimer in the documentation and/or
		other materials provided with the distribution.

	- Neither the name of the University of Technology Dresden, Germany nor the names of its contributors
	  may be used to endorse or promote products derived from this software without specific
		prior written permission.


THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS ``AS IS'' AND ANY EXPRESS
OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS
BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE
*/
package rmi;

import java.rmi.server.UnicastRemoteObject;
import java.rmi.RemoteException;

import rmi.MixCascade;

// Needed for the Server only
import java.util.Enumeration;
import JAPModel;
import AnonServerDBEntry;

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
    public JAPModel myJAPModel = null;


    public JAPAnonServiceImpl() throws RemoteException {
        super();
    }

    public JAPAnonServiceImpl(JAPModel myJAPModel) throws RemoteException {
        super();
        this.myJAPModel = myJAPModel;
    }

    // ----------- local configuration ----------------------------------------
    /** @returns the port where JAP is listening at the moment */
    public int getLocalListeningPort() throws RemoteException {
        return -1;//myJAPModel.getSocksPortNumber();
    }

    /** @returns whether JAP ist actually listening locally only */
    public boolean getLocallyListeningOnly() throws RemoteException {
        return myJAPModel.getHTTPListenerIsLocal();
    }

    /** Set the port where the AnonService is locally listening */
    public boolean setLocalListeningPort(int port) throws RemoteException {
        //myJAPModel.setSocksPortNumber(port);
        //myJAPModel.setUseSocksPort(true);
        return true;
    }

    /** Set whether the AnonService is only available for the local computer */
    public boolean setLocallyListeningOnly(boolean listenOnlyLocally) throws RemoteException {
//        myJAPModel.setHTTPListenerIsLocal(listenOnlyLocally);
//2001-11-12(HF):
		myJAPModel.setHTTPListenerConfig(myJAPModel.getHTTPListenerPortNumber(),listenOnlyLocally);
        return true;
    }
    

// ----------- infoServiceConfiguration -----------------------------------

    /** @returns the actual server name of the infoService */
    public String getInfoServiceServerName() throws RemoteException {
        return myJAPModel.getInfoServiceHost();
    }

    /** @returns the actual portnumber of the infoService */
    public int getInfoServiceServerPort() throws RemoteException {
        return myJAPModel.getInfoServicePort();
    }

    /** Set the InfoService server name */
    public boolean setInfoServiceServerName(String name, int port) throws RemoteException {
        myJAPModel.setInfoService(name, port);
        return true;
    }

// ----------- mixCascadeConfiguration ------------------------------------

    /** @returns all mixCascades that are available in the net. They are loaded via the InfoService. */
    public MixCascade[] loadMixCascadesFromTheNet() throws RemoteException {
        myJAPModel.fetchAnonServers();
        Enumeration enum = myJAPModel.anonServerDatabase.elements();
        int lenght = myJAPModel.anonServerDatabase.size();
        int i = 0;
        MixCascade myMixes[] = new MixCascade[lenght];
        while (enum.hasMoreElements()) {
            myMixes[i] = new MixCascade((AnonServerDBEntry)enum.nextElement());
            i++;
        }
        return myMixes;
    }

    /* @returns the currently used mixCascade
    public MixCascade getMixCascadeCurrentlyUsed() throws RemoteException {
        return new MixCascade("Dresden-Dresden", "mix.inf.tu-dresden.de", 1024);
    }*/


    /** Change the currently used MixCascade */
    public boolean setMixCascadeCurrentlyUsed(MixCascade mixCascade) throws RemoteException {
        /*myJAPModel.anonHostName = mixCascade.getHost();
        myJAPModel.anonPortNumber = mixCascade.getPort();
        myJAPModel.anonserviceName = mixCascade.getName();
        myJAPModel.setAnonMode(true);*/
        return true;
    }

// ----------- proxyServer configuration ----------------------------------

    /** @returns the actual server name of the infoService */
    public String getProxyServerName() throws RemoteException {
        return myJAPModel.getFirewallHost();
    }

    /** @returns the actual portnumber of the infoService */
    public int getProxyServerPort() throws RemoteException {
        return myJAPModel.getFirewallPort();
    }

    /** Set the Proxy server name */
    public boolean setProxyServerName(String name, int port) throws RemoteException {
        myJAPModel.setProxy(name, port);
        return true;
    }
}