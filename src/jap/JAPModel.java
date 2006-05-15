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
package jap;

import java.awt.Dimension;
import java.awt.Point;

import anon.crypto.JAPCertificate;
import anon.infoservice.ProxyInterface;
import anon.util.ResourceLoader;
import anon.infoservice.InfoServiceDBEntry;
import anon.infoservice.ImmutableProxyInterface;
import gui.JAPDll;
import jap.forward.JAPRoutingSettings;

/* This is the Model of All. It's a Singelton!*/
public final class JAPModel
{
	public final static String DLL_VERSION_UPDATE = "dllVersionUpdate";

	private int m_HttpListenerPortNumber = JAPConstants.DEFAULT_PORT_NUMBER; // port number of HTTP  listener
	private boolean m_bHttpListenerIsLocal = JAPConstants.DEFAULT_LISTENER_IS_LOCAL; // indicates whether listeners serve for localhost only or not
	private ProxyInterface m_proxyInterface = null;
	private boolean m_bAutoConnect = false; // autoconnect after program start
	private boolean m_bAutoReConnect = true; // autoReconnects after loosing connection to mix

	private int m_iDummyTrafficIntervall = -1; // indicates what Dummy Traffic should be generated or not

	private boolean m_bSmallDisplay = false;
	private boolean m_bInfoServiceDisabled = JAPConstants.DEFAULT_INFOSERVICE_DISABLED;
	private boolean m_bMinimizeOnStartup = JAPConstants.DEFAULT_MINIMIZE_ON_STARTUP; // true if programm will start minimized
	private boolean m_bMoveToSystrayOnStartup = JAPConstants.DEFAULT_MOVE_TO_SYSTRAY_ON_STARTUP; // true if programm will start in the systray
	private int m_iDefaultView = JAPConstants.DEFAULT_VIEW; //which view we should start?

	private boolean m_bSaveMainWindowPosition = JAPConstants.DEFAULT_SAVE_MAIN_WINDOW_POSITION;
	private Dimension m_OldMainWindowSize = null;
	private Point m_OldMainWindowLocation = null;
	private boolean m_bAllowPaymentViaDirectConnection;
	private boolean m_bAllowInfoServiceViaDirectConnection;

	private static JAPModel ms_TheModel = null;

	//private boolean m_bCertCheckDisabled = true;

	private JAPCertificate m_certJAPCodeSigning = null;

	private int m_TorMaxConnectionsPerRoute = JAPConstants.DEFAULT_TOR_MAX_CONNECTIONS_PER_ROUTE;
	private int m_TorMaxRouteLen = JAPConstants.DEFAULT_TOR_MAX_ROUTE_LEN;
	private int m_TorMinRouteLen = JAPConstants.DEFAULT_TOR_MIN_ROUTE_LEN;
	private int m_MixminionRouteLen = JAPConstants.DEFAULT_MIXMINION_ROUTE_LEN;
	private boolean m_bPreCreateAnonRoutes = JAPConstants.DEFAULT_TOR_PRECREATE_ROUTES;
	private boolean m_bUseProxyAuthentication = false;
	private JAPController.AnonConnectionChecker m_connectionChecker;

	/**
	 * Stores the instance with the routing settings.
	 */
	private JAPRoutingSettings m_routingSettings;

	/**
	 * Stores the path and the name of the config file.
	 */
	private String m_configFileName;

	/**
	 * Stores whether the forwarding state module shall be visible within the configuration
	 * dialog.
	 */
	private boolean m_forwardingStateModuleVisible;

	/**
	 * Stores the password for account data encryption
	 */
	private String m_paymentPassword;

	/** Stores the number of total mixed bytes in this session*/
	private long m_mixedBytes;

	/** Boolen value which describes if a dll update is necessary */
	private boolean m_bUpdateDll = false;

	private JAPModel()
	{
		try
		{
			m_certJAPCodeSigning = JAPCertificate.getInstance(
				ResourceLoader.loadResource(JAPConstants.CERTSPATH + JAPConstants.CERT_JAPCODESIGNING));
		}
		catch (Throwable t)
		{
			m_certJAPCodeSigning = null;
		}
		m_routingSettings = new JAPRoutingSettings();
		m_configFileName = null;
		m_forwardingStateModuleVisible = false;
	}

	// m_Locale=Locale.getDefault();

	/** Creates the Model - as Singleton.
	 *  @return The one and only JAPModel
	 */
	public static JAPModel getInstance()
	{
		if (ms_TheModel == null)
		{
			ms_TheModel = new JAPModel();
		}

		return ms_TheModel;
	}

	public ProxyInterface getProxyInterface()
	{
		return m_proxyInterface;
	}

	void setProxyListener(ProxyInterface a_proxyInterface)
	{
		m_proxyInterface = a_proxyInterface;
	}

	void setAutoConnect(boolean b)
	{
		m_bAutoConnect = b;
	}

	public static boolean getAutoConnect()
	{
		return ms_TheModel.m_bAutoConnect;
	}

	protected void setAutoReConnect(boolean b)
	{
		m_bAutoReConnect = b;
	}

	public static boolean getAutoReConnect()
	{
		return ms_TheModel.m_bAutoReConnect;
	}

	protected void setMinimizeOnStartup(boolean b)
	{
		m_bMinimizeOnStartup = b;
	}

	public static boolean getMinimizeOnStartup()
	{
		return ms_TheModel.m_bMinimizeOnStartup;
	}

	protected void setMoveToSystrayOnStartup(boolean b)
	{
		m_bMoveToSystrayOnStartup = b;
	}

	public static boolean getMoveToSystrayOnStartup()
	{
		return ms_TheModel.m_bMoveToSystrayOnStartup;
	}

	protected void setDefaultView(int view)
	{
		m_iDefaultView = view;
	}

	public static int getDefaultView()
	{
		return ms_TheModel.m_iDefaultView;
	}

	protected void setSaveMainWindowPosition(boolean b)
	{
		m_bSaveMainWindowPosition = b;
	}

	public static boolean getSaveMainWindowPosition()
	{
		return ms_TheModel.m_bSaveMainWindowPosition;
	}

	protected void setOldMainWindowSize(Dimension size)
	{
		m_OldMainWindowSize = size;
	}

	protected void setOldMainWindowLocation(Point location)
	{
		m_OldMainWindowLocation = location;
	}

	public static Point getOldMainWindowLocation()
	{
		return ms_TheModel.m_OldMainWindowLocation;
	}

	/*
//---------------------------------------------------------------------
	 public Locale getLocale() {
	  return m_Locale;
	 }
//---------------------------------------------------------------------
//---------------------------------------------------------------------
	 */

	protected void setDummyTraffic(int msIntervall)
	{
		m_iDummyTrafficIntervall = msIntervall;
	}

	public static int getDummyTraffic()
	{
		return ms_TheModel.m_iDummyTrafficIntervall;
	}

	protected void setHttpListenerPortNumber(int p)
	{
		m_HttpListenerPortNumber = p;
	}

	public void setAnonConnectionChecker(JAPController.AnonConnectionChecker a_connectionChecker)
	{
		m_connectionChecker = a_connectionChecker;
	}



	public boolean isAnonConnected()
	{
		return m_connectionChecker.checkAnonConnected();
	}

	public boolean isPaymentViaDirectConnectionAllowed()
	{
		return m_bAllowPaymentViaDirectConnection;
	}

	public boolean isInfoServiceViaDirectConnectionAllowed()
	{
		return m_bAllowInfoServiceViaDirectConnection;
	}

	public void allowInfoServiceViaDirectConnection(boolean a_bAllowInfoServiceViaDirectConnection)
	{
		m_bAllowInfoServiceViaDirectConnection = a_bAllowInfoServiceViaDirectConnection;
	}

	public void allowPaymentViaDirectConnection(boolean a_bAllowPaymentViaDirectConnection)
	{
		m_bAllowPaymentViaDirectConnection = a_bAllowPaymentViaDirectConnection;
	}

	public InfoServiceDBEntry.MutableProxyInterface getInfoServiceProxyInterface()
	{
		return new InfoServiceDBEntry.MutableProxyInterface()
		{
			public ImmutableProxyInterface[] getProxyInterfaces()
			{
				return getProxyInterface(false);
			}
		};
	}

	public ImmutableProxyInterface[] getPaymentProxyInterface()
	{
		return getProxyInterface(true);
	}

	/**
	 *
	 * @param a_bPayment if the proxy interface for Payment should be returned; otherwise, return
	 * the one for the InfoService
	 * @return ImmutableProxyInterface[] one or more proxy interfaces or null if there is no
	 * proxy available that is allowed
	 */
	private ImmutableProxyInterface[] getProxyInterface(boolean a_bPayment)
	{
		ProxyInterface[] interfaces = new ProxyInterface[4];
		interfaces[0] = getProxyInterface();
		interfaces[1] = null;
		interfaces[2] = new ProxyInterface("localhost", getHttpListenerPortNumber(), null); // AN.ON
		interfaces[3] = new ProxyInterface("localhost", getHttpListenerPortNumber(),
										   ProxyInterface.PROTOCOL_TYPE_SOCKS, null); // TOR
		if ((a_bPayment && !isPaymentViaDirectConnectionAllowed()) ||
			(!a_bPayment && !isInfoServiceViaDirectConnectionAllowed()))
		{
			// force anonymous connections to BI and InfoService
			if (!m_connectionChecker.checkAnonConnected())
			{
				// no anonymous connection available... it is not possible to connect!
				return null;
			}
			// ok, there seems to be an anonymous channel
			return new ProxyInterface[]{interfaces[2], interfaces[3]};
		}
		else if (!m_connectionChecker.checkAnonConnected())
		{
			// return only direct connections
			if (interfaces[0] == null)
			{
				// no proxy/firewall is set
				return new ProxyInterface[]{interfaces[1]};
			}
			return new ProxyInterface[]{interfaces[0], interfaces[1]};
		}

		if (interfaces[0] == null)
		{
			// no proxy/firewall is set
			return new ProxyInterface[]{interfaces[1], interfaces[2], interfaces[3]};
		}

		return interfaces;
	}

	public static int getHttpListenerPortNumber()
	{
		return ms_TheModel.m_HttpListenerPortNumber;
	}

	/* protected void setSocksListenerPortNumber(int p)
	 {
	   m_SOCKSListenerPortnumber = p;
	 }

	 public static int getSocksListenerPortNumber()
	 {
	   return ms_TheModel.m_SOCKSListenerPortnumber;
	 }
	 */
	protected void setHttpListenerIsLocal(boolean b)
	{
		m_bHttpListenerIsLocal = b;
	}

	public static boolean getHttpListenerIsLocal()
	{
		return ms_TheModel.m_bHttpListenerIsLocal;
	}

	public void setSmallDisplay(boolean b)
	{
		m_bSmallDisplay = b;
	}

	public static boolean isSmallDisplay()
	{
		return ms_TheModel.m_bSmallDisplay;
	}

	protected void setInfoServiceDisabled(boolean b)
	{
		m_bInfoServiceDisabled = b;
	}

	public boolean isPaymentDisabled()
	{
		return false;
	}

	public static boolean isInfoServiceDisabled()
	{
		return ms_TheModel.m_bInfoServiceDisabled;
	}

	public String toString()
	{
		StringBuffer buff = new StringBuffer(2048);
		buff.append("Configuration for JAP Version ");
		buff.append(JAPConstants.aktVersion);
		buff.append("\n");
		String s = JAPDll.getDllVersion();
		if (s != null)
		{
			buff.append("Using JAPDll Version: ");
			buff.append(s);
			buff.append("\n");
		}
		s=JAPDll.getDllFileName();
		if(s!=null)
		{
			buff.append("Using JAPDll File: ");
			buff.append(s);
			buff.append("\n");
		}
		buff.append("HttpListenerPortNumber: ");
		buff.append(m_HttpListenerPortNumber);
		buff.append("\n");
		buff.append("HttpListenerIsLocal: ");
		buff.append(m_bHttpListenerIsLocal);
		buff.append("\n");
		buff.append("UseFirewall: ");
		boolean bFirewall = m_proxyInterface != null && m_proxyInterface.isValid();
		buff.append(bFirewall);
		buff.append("\n");
		if (bFirewall)
		{
			buff.append("FirewallType: ");
			buff.append(m_proxyInterface.getProtocol());
			buff.append("\n");
			buff.append("FirewallHost: ");
			buff.append(m_proxyInterface.getHost());
			buff.append("\n");
			buff.append("FirewallPort: ");
			buff.append(m_proxyInterface.getPort());
			buff.append("\n");
		}
		buff.append("AutoConnect: ");
		buff.append(m_bAutoConnect);
		buff.append("\n");
		buff.append("AutoReConnect: ");
		buff.append(m_bAutoReConnect);
		buff.append("\n");

		/*  private boolean m_bUseFirewallAuthentication   = false; //indicates whether JAP should use a UserID/Password to authenticat to the proxy
		 private String  m_FirewallAuthenticationUserID = null;  //userid for authentication
		 private String  m_FirewallAuthenticationPasswd = null;  // password --> will never be saved...
		 private boolean m_bAutoConnect                 = false; // autoconnect after program start
		 private boolean m_bMinimizeOnStartup
		 */
		/*     e.setAttribute("portNumber",Integer.toString(portNumber));
		  //e.setAttribute("portNumberSocks",Integer.toString(portSocksListener));
		  //e.setAttribute("supportSocks",(getUseSocksPort()?"true":"false"));
		  e.setAttribute("listenerIsLocal",(mblistenerIsLocal?"true":"false"));
		  e.setAttribute("proxyMode",(mbUseProxy?"true":"false"));
		  e.setAttribute("proxyHostName",((proxyHostName==null)?"":proxyHostName));
		  e.setAttribute("proxyPortNumber",Integer.toString(proxyPortNumber));
		  e.setAttribute("proxyAuthorization",(mb_UseProxyAuthentication?"true":"false"));
		 e.setAttribute("proxyAuthUserID",((m_ProxyAuthenticationUserID==null)?"":m_ProxyAuthenticationUserID));
		  e.setAttribute("infoServiceHostName",((infoServiceHostName==null)?"":infoServiceHostName));
		  e.setAttribute("infoServicePortNumber",Integer.toString(infoServicePortNumber));
		  AnonServerDBEntry e1 = model.getAnonServer();
		  e.setAttribute("anonserviceName",((e1.getName()==null)?"":e1.getName()));
		  e.setAttribute("anonHostName",   ((e1.getHost()==null)?"":e1.getHost()));
		  e.setAttribute("anonHostIP",   ((e1.getIP()==null)?"":e1.getIP()));
		  e.setAttribute("anonPortNumber",   Integer.toString(e1.getPort()));
		  e.setAttribute("anonSSLPortNumber",Integer.toString(e1.getSSLPort()));
		  e.setAttribute("autoConnect",(autoConnect?"true":"false"));
		  e.setAttribute("minimizedStartup",(mbMinimizeOnStartup?"true":"false"));
		  e.setAttribute("neverRemindActiveContent",(mbActCntMessageNeverRemind?"true":"false"));
		  e.setAttribute("doNotAbuseReminder",(mbDoNotAbuseReminder?"true":"false"));
		  e.setAttribute("neverRemindGoodBye",(mbGoodByMessageNeverRemind?"true":"false"));
		  e.setAttribute("Locale",m_Locale.getLanguage());
		  e.setAttribute("LookAndFeel",UIManager.getLookAndFeel().getName());
		  // adding Debug-Element
		  Element elemDebug=doc.createElement("Debug");
		  e.appendChild(elemDebug);
		  Element tmp=doc.createElement("Level");
		  Text txt=doc.createTextNode(Integer.toString(JAPDebug.getDebugLevel()));
		  tmp.appendChild(txt);
		  elemDebug.appendChild(tmp);
		  tmp=doc.createElement("Type");
		  int debugtype=JAPDebug.getDebugType();
		  tmp.setAttribute("GUI",(debugtype&JAPDebug.GUI)!=0?"true":"false");
		  tmp.setAttribute("NET",(debugtype&JAPDebug.NET)!=0?"true":"false");
		  tmp.setAttribute("THREAD",(debugtype&JAPDebug.THREAD)!=0?"true":"false");
		  tmp.setAttribute("MISC",(debugtype&JAPDebug.MISC)!=0?"true":"false");
		  elemDebug.appendChild(tmp);
		  if(JAPDebug.isShowConsole()){
		 tmp=doc.createElement("Output");
		 txt=doc.createTextNode("Console");
		 tmp.appendChild(txt);
		 elemDebug.appendChild(tmp);
		  }
		  return JAPUtil.XMLDocumentToString(doc);
		  //((XmlDocument)doc).write(f);
		 }
		 catch(Exception ex) {
		  JAPDebug.out(JAPDebug.EXCEPTION,JAPDebug.MISC,"JAPModel:save() Exception: "+ex.getMessage());
		  //ex.printStackTrace();
		 }
		 */
		return buff.toString();
	}

	public static boolean isPreCreateAnonRoutesEnabled()
	{
		return ms_TheModel.m_bPreCreateAnonRoutes;
	}

	void setPreCreateAnonRoutes(boolean b)
	{
		m_bPreCreateAnonRoutes = b;
	}

	public static JAPCertificate getJAPCodeSigningCert()
	{
		return ms_TheModel.m_certJAPCodeSigning;
	}

	/**
	 * Changes the filename of the used config file.
	 *
	 * @param a_configFileName The filename (including path) of the used configuration file.
	 */
	public void setConfigFile(String a_configFileName)
	{
		m_configFileName = a_configFileName;
	}

	/**
	 * Returns the filename of the used config file.
	 *
	 * @return The filename (including path) of the used configuration file.
	 */
	public String getConfigFile()
	{
		return m_configFileName;
	}

	/**
	 * This method returns the instance of JAPRoutingSettings, where all routing settings are
	 * stored in. Changes of the routing settings are directly done on the returned instance.
	 * @see JAPRoutingSettings
	 *
	 * @return The routing settings.
	 */
	public JAPRoutingSettings getRoutingSettings()
	{
		return m_routingSettings;
	}

	/**
	 * Sets whether the forwarding state module shall be visible within the configuration
	 * dialog.
	 *
	 * @param a_moduleVisible True, if the forwarding state module shall be visible, false
	 *                        otherwise.
	 */
	public void setForwardingStateModuleVisible(boolean a_moduleVisible)
	{
		m_forwardingStateModuleVisible = a_moduleVisible;
	}

	/**
	 * Returns whether the forwarding state module shall be visible within the configuration
	 * dialog.
	 *
	 * @return True, if the forwarding state module shall be visible, false otherwise.
	 */
	public boolean isForwardingStateModuleVisible()
	{
		return m_forwardingStateModuleVisible;
	}

	public static int getTorMaxConnectionsPerRoute()
	{
		return ms_TheModel.m_TorMaxConnectionsPerRoute;
	}

	protected void setTorMaxConnectionsPerRoute(int i)
	{
		m_TorMaxConnectionsPerRoute = i;
	}

	public static int getTorMaxRouteLen()
	{
		return ms_TheModel.m_TorMaxRouteLen;
	}

	protected void setTorMaxRouteLen(int i)
	{
		m_TorMaxRouteLen = i;
	}

	public static int getTorMinRouteLen()
	{
		return ms_TheModel.m_TorMinRouteLen;
	}

	protected void setTorMinRouteLen(int i)
	{
		m_TorMinRouteLen = i;
	}

	protected void setMixminionRouteLen(int i)
	{
		m_MixminionRouteLen = i;
	}

	public static int getMixminionRouteLen()
	{
		return ms_TheModel.m_MixminionRouteLen;
	}

	protected void setUseProxyAuthentication(boolean a_bUseAuth)
	{
		m_bUseProxyAuthentication = a_bUseAuth;
	}

	public boolean isProxyAuthenticationUsed()
	{
		return m_bUseProxyAuthentication;
	}

	public void setPaymentPassword(String a_password)
	{
		m_paymentPassword = a_password;
	}

	public String getPaymentPassword()
	{
		return m_paymentPassword;
	}

	public void setMixedBytes(long a_mixedBytes)
	{
		m_mixedBytes = a_mixedBytes;
	}

	public long getMixedBytes()
	{
		return m_mixedBytes;
	}

	public void setDLLupdate(boolean a_update) {
		m_bUpdateDll = a_update;
    }

	public boolean getDLLupdate() {
		return m_bUpdateDll;
	}
}
