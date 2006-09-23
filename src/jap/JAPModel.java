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
import java.util.Vector;
import java.io.File;

import anon.crypto.JAPCertificate;
import anon.infoservice.ProxyInterface;
import anon.util.ResourceLoader;
import anon.infoservice.ImmutableProxyInterface;
import gui.JAPDll;
import java.util.Observable;
import jap.forward.JAPRoutingSettings;
import anon.infoservice.IMutableProxyInterface;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import gui.GUIUtils;

/* This is the Model of All. It's a Singelton!*/
public final class JAPModel extends Observable
{
	public static final String DLL_VERSION_UPDATE = "dllVersionUpdate";
	public static final String XML_REMIND_OPTIONAL_UPDATE = "remindOptionalUpdate";
	public static final String XML_REMIND_JAVA_UPDATE = "remindJavaUpdate";
	public static final String XML_RESTRICT_CASCADE_AUTO_CHANGE = "restrictCascadeAutoChange";
	public static final String XML_DENY_NON_ANONYMOUS_SURFING = "denyNonAnonymousSurfing";
	public static final String XML_ATTR_ACTIVATED = "activated";
	public static final String XML_FONT_SIZE = "fontSize";
	public static final String XML_CONFIG_WINDOW = "ConfigWindow";
	public static final String XML_SIZE = "Size";
	public static final String XML_ICONIFIED_WINDOW = "IconifiedWindow";
	public static final String XML_HELP_WINDOW = "HelpWindow";
	public static final String XML_ATTR_WIDTH = "width";
	public static final String XML_ATTR_HEIGHT = "height";
	public static final String XML_ATTR_SAVE = "save";

	public static final String AUTO_CHANGE_NO_RESTRICTION = "none";
	public static final String AUTO_CHANGE_RESTRICT_TO_PAY = "pay";
	public static final String AUTO_CHANGE_RESTRICT = "restrict";

	public static final int MAX_FONT_SIZE = 4;

	// observer messages
	public static final Integer CHANGED_INFOSERVICE_AUTO_UPDATE = new Integer(0);
	public static final Integer CHANGED_ALLOW_INFOSERVICE_DIRECT_CONNECTION = new Integer(1);
	public static final Integer CHANGED_ALLOW_UPDATE_DIRECT_CONNECTION = new Integer(2);
	public static final Integer CHANGED_NOTIFY_JAP_UPDATES = new Integer(3);
	public static final Integer CHANGED_NOTIFY_JAVA_UPDATES = new Integer(4);
	public static final Integer CHANGED_AUTO_CONNECT = new Integer(5);
	public static final Integer CHANGED_AUTO_RECONNECT = new Integer(6);
	public static final Integer CHANGED_CASCADE_AUTO_CHANGE = new Integer(7);

	private static final int DIRECT_CONNECTION_INFOSERVICE = 0;
	private static final int DIRECT_CONNECTION_PAYMENT = 1;
	private static final int DIRECT_CONNECTION_UPDATE = 2;

	private int m_HttpListenerPortNumber = JAPConstants.DEFAULT_PORT_NUMBER; // port number of HTTP  listener
	private boolean m_bHttpListenerIsLocal = JAPConstants.DEFAULT_LISTENER_IS_LOCAL; // indicates whether listeners serve for localhost only or not
	private ProxyInterface m_proxyInterface = null;
	private boolean m_bAutoConnect; // autoconnect after program start
	private boolean m_bAutoReConnect; // autoReconnects after loosing connection to mix

	private int m_iDummyTrafficIntervall = -1; // indicates what Dummy Traffic should be generated or not

	private boolean m_bSmallDisplay = false;
	private boolean m_bInfoServiceDisabled = JAPConstants.DEFAULT_INFOSERVICE_DISABLED;
	private boolean m_bMinimizeOnStartup = JAPConstants.DEFAULT_MINIMIZE_ON_STARTUP; // true if programm will start minimized
	private boolean m_bMoveToSystrayOnStartup = JAPConstants.DEFAULT_MOVE_TO_SYSTRAY_ON_STARTUP; // true if programm will start in the systray
	private int m_iDefaultView = JAPConstants.DEFAULT_VIEW; //which view we should start?

	private boolean m_bSaveMainWindowPosition;
	private boolean m_bSaveConfigWindowPosition;
	private boolean m_bSaveIconifiedWindowPosition;
	private boolean m_bSaveHelpWindowPosition;
	private Point m_OldMainWindowLocation = null;
	private Point m_iconifiedWindowLocation = null;
	private Point m_configWindowLocation = null;
	private Point m_helpWindowLocation = null;

	private boolean m_bGoodByMessageNeverRemind = false; // indicates if Warning message before exit has been deactivated forever

	private boolean m_bAllowPaymentViaDirectConnection;
	private boolean m_bAllowInfoServiceViaDirectConnection;
	private boolean m_bAllowUpdateViaDirectConnection;

	private boolean m_bDenyNonAnonymousSurfing;

	private boolean m_bRemindOptionalUpdate;
	private boolean m_bRemindJavaUpdate;

	private boolean m_bTorActivated;
	private boolean m_bMixMinionActivated;

	private boolean m_bChooseCascasdeConnectionAutomatically;
	private String m_automaticCascadeChangeRestriction;

	private String m_strLookAndFeel;
	private Vector m_vecLookAndFeels = new Vector();
	private LookAndFeelInfo[] m_systemLookAndFeels;
	private Object LOOK_AND_FEEL_SYNC = new Object();

	private boolean m_bShowDialogFormat = false;

	private int m_fontSize = 0;

	private GUIUtils.IIconResizer m_resizer = new GUIUtils.IIconResizer()
		{
			public double getResizeFactor()
			{
				return 1.0 + getFontSize() * 0.1;
			}
	};

	private static JAPModel ms_TheModel = null;

	//private boolean m_bCertCheckDisabled = true;

	private JAPCertificate m_certJAPCodeSigning = null;

	private int m_TorMaxConnectionsPerRoute = JAPConstants.DEFAULT_TOR_MAX_CONNECTIONS_PER_ROUTE;
	private int m_TorMaxRouteLen = JAPConstants.DEFAULT_TOR_MAX_ROUTE_LEN;
	private int m_TorMinRouteLen = JAPConstants.DEFAULT_TOR_MIN_ROUTE_LEN;
	private int m_MixminionRouteLen = JAPConstants.DEFAULT_MIXMINION_ROUTE_LEN;
	private String m_MixminionMyEMail = JAPConstants.DEFAULT_MIXMINION_EMAIL;
	private boolean m_bPreCreateAnonRoutes = JAPConstants.DEFAULT_TOR_PRECREATE_ROUTES;
	private boolean m_bUseProxyAuthentication = false;
	private JAPController.AnonConnectionChecker m_connectionChecker;

	private Dimension m_iconifiedSize = new Dimension(0, 0);
	private Dimension m_configSize = new Dimension(0, 0);
	private Dimension m_helpSize;
	private boolean m_bSaveHelpSize;

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
		synchronized (this)
		{
			if (m_bAutoConnect != b)
			{
				m_bAutoConnect = b;
				setChanged();
			}
			notifyObservers(CHANGED_AUTO_CONNECT);
		}
	}

	public static boolean isAutoConnect()
	{
		return ms_TheModel.m_bAutoConnect;
	}

	void setAutoReConnect(boolean b)
	{
		synchronized (this)
		{
			if (m_bAutoReConnect != b)
			{
				m_bAutoReConnect = b;
				setChanged();
			}
			notifyObservers(CHANGED_AUTO_RECONNECT);
		}
	}

	public static boolean isAutomaticallyReconnected()
	{
		return ms_TheModel.m_bAutoReConnect;
	}

	public void setLookAndFeel(String a_strLookAndFeel)
	{
		m_strLookAndFeel = a_strLookAndFeel;
	}

	/**
	 * Returns a Vector with all files that are registerd to contain LookAndFeel classes.
	 * @return a Vector with all files that are registerd to contain LookAndFeel classes
	 */
	public Vector getLookAndFeelFiles()
	{
		return (Vector)m_vecLookAndFeels.clone();
	}

	public boolean addLookAndFeelFile(File a_file)
	{
		if (a_file != null)
		{
			synchronized (m_vecLookAndFeels)
			{
				if (!m_vecLookAndFeels.contains(a_file))
				{
					m_vecLookAndFeels.addElement(a_file);
					return true;
				}
			}
		}
		return false;
	}

	public boolean removeLookAndFeelFile(File a_file)
	{
		return m_vecLookAndFeels.removeElement(a_file);
	}


	public String getLookAndFeel()
	{
		return m_strLookAndFeel;
	}

	public boolean isTorActivated()
	{
		return m_bTorActivated;
	}

	public void setTorActivated(boolean a_bActivate)
	{
		m_bTorActivated = a_bActivate;
	}

	public void setMixMinionActivated(boolean a_bActivate)
	{
		m_bMixMinionActivated = a_bActivate;
	}

	public boolean isMixMinionActivated()
	{
		return m_bMixMinionActivated;
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

	public void setSaveConfigWindowPosition(boolean a_bSave)
	{
		m_bSaveConfigWindowPosition = a_bSave;
	}

	public void setSaveIconifiedWindowPosition(boolean a_bSave)
	{
		m_bSaveIconifiedWindowPosition = a_bSave;
	}

	public void setSaveHelpWindowPosition(boolean a_bSave)
	{
		m_bSaveHelpWindowPosition = a_bSave;
	}


	public void updateSystemLookAndFeels()
	{
		synchronized (LOOK_AND_FEEL_SYNC)
		{
			m_systemLookAndFeels = UIManager.getInstalledLookAndFeels();
		}
	}

	public boolean isSystemLookAndFeel(String a_LAFclassName)
	{
		synchronized (LOOK_AND_FEEL_SYNC)
		{
			if (m_systemLookAndFeels == null || a_LAFclassName == null)
			{
				return false;
			}
			for (int i = 0; i < m_systemLookAndFeels.length; i++)
			{
				if (m_systemLookAndFeels[i] == null)
				{
					continue;
				}
				if (m_systemLookAndFeels[i].getClassName().equals(a_LAFclassName))
				{
					return true;
				}
			}
		}
		return false;
	}

	public boolean isIconifiedWindowLocationSaved()
	{
		return m_bSaveIconifiedWindowPosition;
	}

	public void setIconifiedWindowLocation(Point a_location)
	{
		m_iconifiedWindowLocation = a_location;
	}

	public Point getIconifiedWindowLocation()
	{
		if (isIconifiedWindowLocationSaved())
		{
			return m_iconifiedWindowLocation;
		}
		return null;
	}

	public boolean isHelpWindowLocationSaved()
	{
		return m_bSaveHelpWindowPosition;
	}

	public void setHelpWindowLocation(Point a_location)
	{
		m_helpWindowLocation = a_location;
	}

	public Point getHelpWindowLocation()
	{
		if (isHelpWindowLocationSaved())
		{
			return m_helpWindowLocation;
		}
		return null;
	}


	public boolean isConfigWindowLocationSaved()
	{
		return m_bSaveConfigWindowPosition;
	}

	public void setConfigWindowLocation(Point a_location)
	{
		m_configWindowLocation = a_location;
	}

	public Point getConfigWindowLocation()
	{
		if (isConfigWindowLocationSaved())
		{
			return m_configWindowLocation;
		}
		return null;
	}


	public static boolean isMainWindowLocationSaved()
	{
		return ms_TheModel.m_bSaveMainWindowPosition;
	}

	protected void setMainWindowLocation(Point location)
	{
		m_OldMainWindowLocation = location;
	}

	public static Point getMainWindowLocation()
	{
		if (isMainWindowLocationSaved())
		{
			return ms_TheModel.m_OldMainWindowLocation;
		}
		return null;
	}

	/*
//---------------------------------------------------------------------
	 public Locale getLocale() {
	  return m_Locale;
	 }
//---------------------------------------------------------------------
//---------------------------------------------------------------------
	 */
	/**
	 * Show the options to alter the dialog format
	 * @return boolean
	 */
	public boolean isDialogFormatShown()
	{
		return m_bShowDialogFormat;
	}

	public void setDialogFormatShown(boolean a_bShow)
	{
		m_bShowDialogFormat = a_bShow;
	}


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

	public boolean isReminderForOptionalUpdateActivated()
	{
		return m_bRemindOptionalUpdate;
	}

	public void setReminderForOptionalUpdate(boolean a_bRemind)
	{
		synchronized (this)
		{
			if (m_bRemindOptionalUpdate != a_bRemind)
			{
				m_bRemindOptionalUpdate = a_bRemind;
				setChanged();
			}
			notifyObservers(CHANGED_NOTIFY_JAP_UPDATES);
		}

	}

	public boolean isReminderForJavaUpdateActivated()
	{
		return m_bRemindJavaUpdate;
	}

	public void setReminderForJavaUpdate(boolean a_bRemind)
	{
		synchronized (this)
		{
			if (m_bRemindJavaUpdate != a_bRemind)
			{
				m_bRemindJavaUpdate = a_bRemind;
				setChanged();
			}
			notifyObservers(CHANGED_NOTIFY_JAVA_UPDATES);
		}
	}


	public String getAutomaticCascadeChangeRestriction()
	{
		return m_automaticCascadeChangeRestriction;
	}

	public void setAutomaticCascadeChangeRestriction(String a_restriction)
	{
		if (a_restriction != null &&
			(a_restriction.equals(AUTO_CHANGE_NO_RESTRICTION) ||
			a_restriction.equals(AUTO_CHANGE_RESTRICT_TO_PAY) ||
			a_restriction.equals(AUTO_CHANGE_RESTRICT)))
		{
			m_automaticCascadeChangeRestriction = a_restriction;
		}
		else
		{
			m_automaticCascadeChangeRestriction = AUTO_CHANGE_NO_RESTRICTION;
		}
	}

	public void setCascadeAutoSwitch(boolean a_bChooseCascasdeConnectionAutomatically)
	{
		synchronized (this)
		{
			if (m_bChooseCascasdeConnectionAutomatically != a_bChooseCascasdeConnectionAutomatically)
			{
				m_bChooseCascasdeConnectionAutomatically = a_bChooseCascasdeConnectionAutomatically;
				setChanged();
			}
			notifyObservers(CHANGED_CASCADE_AUTO_CHANGE);
		}
	}

	public boolean isCascadeAutoSwitched()
	{
		return m_bChooseCascasdeConnectionAutomatically;
	}

	public boolean isAnonConnected()
	{
		return m_connectionChecker.checkAnonConnected();
	}

	public boolean isNonAnonymousSurfingDenied()
	{
		return m_bDenyNonAnonymousSurfing;
	}

	public void denyNonAnonymousSurfing(boolean a_bDenyNonAnonymousSurfing)
	{
		m_bDenyNonAnonymousSurfing = a_bDenyNonAnonymousSurfing;
	}

	public boolean isPaymentViaDirectConnectionAllowed()
	{
		return m_bAllowPaymentViaDirectConnection;
	}

	public boolean isUpdateViaDirectConnectionAllowed()
	{
		return m_bAllowUpdateViaDirectConnection;
	}

	public void allowUpdateViaDirectConnection(boolean a_bAllow)
	{
		synchronized (this)
		{
			if (m_bAllowUpdateViaDirectConnection != a_bAllow)
			{
				m_bAllowUpdateViaDirectConnection = a_bAllow;
				setChanged();
			}
			notifyObservers(CHANGED_ALLOW_UPDATE_DIRECT_CONNECTION);
		}
	}


	public boolean isInfoServiceViaDirectConnectionAllowed()
	{
		return m_bAllowInfoServiceViaDirectConnection;
	}

	public void allowInfoServiceViaDirectConnection(boolean a_bAllowInfoServiceViaDirectConnection)
	{
		synchronized (this)
		{
			if (m_bAllowInfoServiceViaDirectConnection != a_bAllowInfoServiceViaDirectConnection)
			{
				m_bAllowInfoServiceViaDirectConnection = a_bAllowInfoServiceViaDirectConnection;
				setChanged();
			}
			notifyObservers(CHANGED_ALLOW_INFOSERVICE_DIRECT_CONNECTION);
		}
	}

	public void allowPaymentViaDirectConnection(boolean a_bAllowPaymentViaDirectConnection)
	{
		m_bAllowPaymentViaDirectConnection = a_bAllowPaymentViaDirectConnection;
	}

	public IMutableProxyInterface getInfoServiceProxyInterface()
	{
		return new IMutableProxyInterface()
		{
			public IProxyInterfaceGetter getProxyInterface(boolean a_bAnonInterface)
			{
				return JAPModel.getInstance().getProxyInterface(
								DIRECT_CONNECTION_INFOSERVICE, a_bAnonInterface);
			}
		};
	}

	public IMutableProxyInterface getPaymentProxyInterface()
	{
		return new IMutableProxyInterface()
		{
			public IProxyInterfaceGetter getProxyInterface(boolean a_bAnonInterface)
			{
				return JAPModel.getInstance().getProxyInterface(DIRECT_CONNECTION_PAYMENT, a_bAnonInterface);
			}
		};
	}

	public IMutableProxyInterface getUpdateProxyInterface()
	{
		return new IMutableProxyInterface()
		{
			public IProxyInterfaceGetter getProxyInterface(boolean a_bAnonInterface)
			{
				return JAPModel.getInstance().getProxyInterface(DIRECT_CONNECTION_UPDATE, a_bAnonInterface);
			}
		};
	}

	public ImmutableProxyInterface getTorProxyInterface()
	{
		return new ProxyInterface("localhost", getHttpListenerPortNumber(),
								  ProxyInterface.PROTOCOL_TYPE_SOCKS, null);
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

	public static boolean isHttpListenerLocal()
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

	public boolean isNeverRemindGoodbye()
	{
		return m_bGoodByMessageNeverRemind;
	}

	public void setNeverRemindGoodbye(boolean a_bGoodByMessageNeverRemind)
	{
		m_bGoodByMessageNeverRemind = a_bGoodByMessageNeverRemind;
	}

	protected void setInfoServiceDisabled(boolean b)
	{
		synchronized (this)
		{
			if (m_bInfoServiceDisabled != b)
			{
				m_bInfoServiceDisabled = b;
				setChanged();
			}
			notifyObservers(CHANGED_INFOSERVICE_AUTO_UPDATE);
		}

	}

	public static boolean isInfoServiceDisabled()
	{
		return ms_TheModel.m_bInfoServiceDisabled;
	}

	public GUIUtils.IIconResizer getIconResizer()
	{
		return m_resizer;
	}

	/**
	 * Returns the relative font size as integer from 0 to MAX_FONT_SIZE. The real font size
	 * is calculated as 100% + getFontSize() * 10%.
	 * @return the relative font size as integer from 0 to MAX_FONT_SIZE
	 */
	public int getFontSize()
	{
		return m_fontSize;
	}

	public void setFontSize(int a_fontSize)
	{
		if (a_fontSize < 0)
		{
			a_fontSize = 0;
		}
		else if (a_fontSize > MAX_FONT_SIZE)
		{
			a_fontSize = MAX_FONT_SIZE;
		}
		if (m_fontSize != a_fontSize)
		{
			synchronized (this)
			{
				FontResize resize = new FontResize(m_fontSize, a_fontSize);
				GUIUtils.resizeAllFonts(1.0f / (1.0f + 0.1f * resize.getOldSize()));
				GUIUtils.resizeAllFonts(1.0f + 0.1f * resize.getNewSize());
				m_fontSize = a_fontSize;
				setChanged();
				notifyObservers(resize);
			}
		}
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

	public void setIconifiedSize(Dimension a_size)
	{
		m_iconifiedSize = a_size;
	}

	public Dimension getIconifiedSize()
	{
		return m_iconifiedSize;
	}

	public void setHelpWindowSize(Dimension a_size)
	{
		m_helpSize = a_size;
	}

	public Dimension getHelpWindowSize()
	{
		return m_helpSize;
	}

	public boolean isHelpWindowSizeSaved()
	{
		return m_bSaveHelpSize;
	}

	public void setSaveHelpWindowSize(boolean a_bSave)
	{
		m_bSaveHelpSize = a_bSave;
	}

	public void setConfigSize(Dimension a_size)
	{
		m_configSize = a_size;
	}

	public Dimension getConfigSize()
	{
		return m_configSize;
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

	//von Stefan Roenisch
	protected void setMixminionMyEMail(String address) {
		m_MixminionMyEMail = address;
	}

	public static String getMixminionMyEMail() {
		return ms_TheModel.m_MixminionMyEMail;
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

	public static class FontResize
	{
		private int m_oldSize;
		private int m_newSize;

		public FontResize(int a_oldSize, int a_newSize)
		{
			m_oldSize = a_oldSize;
			m_newSize = a_newSize;
		}
		public int getOldSize()
		{
			return m_oldSize;
		}
		public int getNewSize()
		{
			return m_newSize;
		}
	}

	public boolean isShuttingDown()
	{
		return JAPController.getInstance().isShuttingDown();
	}

	private IMutableProxyInterface.IProxyInterfaceGetter getProxyInterface(
		   int a_component, boolean a_bAnonInterface)
	{
		if (isShuttingDown())
		{
			return null;
		}

		IMutableProxyInterface.IProxyInterfaceGetter proxyDirect, proxyAnon;
		proxyDirect = new IMutableProxyInterface.IProxyInterfaceGetter()
		{
			public ImmutableProxyInterface getProxyInterface()
			{
				// try direct connection via proxy, if present
				return JAPModel.getInstance().getProxyInterface();
			}
		};
		proxyAnon = new IMutableProxyInterface.IProxyInterfaceGetter()
		{
			public ImmutableProxyInterface getProxyInterface()
			{
				return new ProxyInterface("localhost", getHttpListenerPortNumber(), null); // AN.ON
			}
		};

		//interfaces[3] = new ProxyInterface("localhost", getHttpListenerPortNumber(),
			//							   ProxyInterface.PROTOCOL_TYPE_SOCKS, null); // TOR
		if ((DIRECT_CONNECTION_PAYMENT == a_component && !isPaymentViaDirectConnectionAllowed()) ||
			(DIRECT_CONNECTION_INFOSERVICE == a_component && !isInfoServiceViaDirectConnectionAllowed()) ||
			(DIRECT_CONNECTION_UPDATE == a_component && !isUpdateViaDirectConnectionAllowed()))
		{
			// force anonymous connections to BI and InfoService
			if (!m_connectionChecker.checkAnonConnected())
			{
				// no anonymous connection available... it is not possible to connect!
				return null;
			}
			// ok, there seems to be an anonymous channel
			if (a_bAnonInterface)
			{
				return proxyAnon;
			}
			// A direct proxy was requested; not allowed!
			return null;

		}
		else if (!m_connectionChecker.checkAnonConnected())
		{
			if (a_bAnonInterface)
			{
				// no anonymous connection is available
				return null;
			}
			return proxyDirect;
		}

		// both proxies are available
		if (a_bAnonInterface)
		{
			return proxyAnon;
		}
		return proxyDirect;
	}
}