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
import anon.crypto.JAPCertificateStore;
import anon.crypto.JAPCertificate;
import gui.JAPDll;

/* This is the Model of All. It's a Singelton!*/
public final class JAPModel
{

	private String m_biHost = JAPConstants.PIHOST;
	private int m_biPort = JAPConstants.PIPORT;
	private int m_HttpListenerPortNumber = JAPConstants.defaultPortNumber; // port number of HTTP  listener
	private boolean m_bHttpListenerIsLocal = true; // indicates whether listeners serve for localhost only or not
	private boolean m_bUseFirewall = false; // indicates whether JAP connects via a proxy or directly
	private int m_FirewallType = JAPConstants.defaultFirewallType;
	private String m_FirewallHostName = ""; // hostname of proxy
	private int m_FirewallPortNumber = -1; // portnumber of proxy
	private boolean m_bUseFirewallAuthentication = false; //indicates whether JAP should use a UserID/Password to authenticat to the proxy
	private String m_FirewallAuthenticationUserID = null; //userid for authentication
	private String m_FirewallAuthenticationPasswd = null; // password --> will never be saved...
	private boolean m_bAutoConnect = false; // autoconnect after program start
	private boolean m_bAutoReConnect = false; // autoReconnects after loosing connection to mix
	private boolean m_bMinimizeOnStartup = false; // true if programm will start minimized

	//private boolean  mbActCntMessageNotRemind    = false; // indicates if Warning message in setAnonMode has been deactivated for the session
	//private boolean  mbActCntMessageNeverRemind  = false; // indicates if Warning message in setAnonMode has been deactivated forever
	//private boolean  mbDoNotAbuseReminder        = false; // indicates if new warning message in setAnonMode (containing Do no abuse) has been shown
	//private boolean  mbGoodByMessageNeverRemind  = false; // indicates if Warning message before exit has been deactivated forever
	private int m_iDummyTrafficIntervall = -1; // indicates what Dummy Traffic should be generated or not

	private boolean m_bSmallDisplay = false;
	private boolean m_bInfoServiceDisabled = false;
	private boolean m_bSaveMainWindowPosition = false;
	public Point m_OldMainWindowLocation = null;
	public Dimension m_OldMainWindowSize = null;
	private static JAPModel model = null;

	private boolean m_bCertCheckDisabled = true;

	private JAPCertificate m_certJAPCodeSigning = null;
	private JAPCertificateStore m_certStore = null;

	private JAPModel()
	{
		m_certStore = JAPCertificateStore.getInstance();
		try
		{
			m_certJAPCodeSigning = JAPCertificate.getInstance(
				 JAPUtil.loadRessource(JAPConstants.CERTSPATH+JAPConstants.CERT_JAPCODESIGNING));
		}
		catch(Throwable t)
		{
			m_certJAPCodeSigning = null;
		}
	}

	// m_Locale=Locale.getDefault();

	/** Creates the Model - as Singleton.
	 *  @return The one and only JAPModel
	 */
	public static JAPModel create()
	{
		if (model == null)
		{
			model = new JAPModel();
		}
		return model;
	}

	public static JAPModel getModel()
	{
		return model;
	}

	protected void setUseFirewall(boolean b)
	{
		m_bUseFirewall = b;
	}

	public static boolean getUseFirewall()
	{
		return model.m_bUseFirewall;
	}

	protected void setFirewallType(int type)
	{
		m_FirewallType = type;
	}

	public static int getFirewallType()
	{
		return model.m_FirewallType;
	}

	protected void setFirewallHost(String host)
	{
		m_FirewallHostName = host;
	}

	public static String getFirewallHost()
	{
		return model.m_FirewallHostName;
	}

	protected void setFirewallPort(int port)
	{
		m_FirewallPortNumber = port;
	}

	public static int getFirewallPort()
	{
		return model.m_FirewallPortNumber;
	}

	protected void setUseFirewallAuthorization(boolean b)
	{
		m_bUseFirewallAuthentication = b;
	}

	public static boolean getUseFirewallAuthorization()
	{
		return model.m_bUseFirewallAuthentication;
	}

	protected void setFirewallAuthUserID(String userid)
	{
		m_FirewallAuthenticationUserID = userid;
	}

	public static String getFirewallAuthUserID()
	{
		return model.m_FirewallAuthenticationUserID;
	}

	protected void setFirewallAuthPasswd(String passwd)
	{
		m_FirewallAuthenticationPasswd = passwd;
	}

	public static String getFirewallAuthPasswd()
	{
		return model.m_FirewallAuthenticationPasswd;
	}

	protected void setAutoConnect(boolean b)
	{
		m_bAutoConnect = b;
	}

	public static boolean getAutoConnect()
	{
		return model.m_bAutoConnect;
	}

	protected void setAutoReConnect(boolean b)
	{
		m_bAutoReConnect = b;
	}

	public static boolean getAutoReConnect()
	{
		return model.m_bAutoReConnect;
	}

	protected void setMinimizeOnStartup(boolean b)
	{
		m_bMinimizeOnStartup = b;
	}

	public static boolean getMinimizeOnStartup()
	{
		return model.m_bMinimizeOnStartup;
	}

	protected void setSaveMainWindowPosition(boolean b)
	{
		m_bSaveMainWindowPosition = b;
	}

	public static boolean getSaveMainWindowPosition()
	{
		return model.m_bSaveMainWindowPosition;
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
		return model.m_iDummyTrafficIntervall;
	}

	protected void setHttpListenerPortNumber(int p)
	{
		m_HttpListenerPortNumber = p;
	}

	public static int getHttpListenerPortNumber()
	{
		return model.m_HttpListenerPortNumber;
	}

	protected void setHttpListenerIsLocal(boolean b)
	{
		m_bHttpListenerIsLocal = b;
	}

	public static boolean getHttpListenerIsLocal()
	{
		return model.m_bHttpListenerIsLocal;
	}

	public void setSmallDisplay(boolean b)
	{
		m_bSmallDisplay = b;
	}

	public static boolean isSmallDisplay()
	{
		return model.m_bSmallDisplay;
	}

	protected void setInfoServiceDisabled(boolean b)
	{
		m_bInfoServiceDisabled = b;
	}

	public static boolean isInfoServiceDisabled()
	{
		return model.m_bInfoServiceDisabled;
	}

	public static String getBIHost()
	{
		return model.m_biHost;
	}

	public static int getBIPort()
	{
		return model.m_biPort;
	}

	protected static void setBIHost(String host)
	{
		model.m_biHost = host;
	}

	protected static void setBIPort(int port)
	{
		model.m_biPort = port;
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
			buff.append("Using JAPDll: ");
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
		buff.append(m_bUseFirewall);
		buff.append("\n");
		buff.append("FirewallType: ");
		buff.append(m_FirewallType);
		buff.append("\n");
		buff.append("FirewallHost: ");
		buff.append(m_FirewallHostName);
		buff.append("\n");
		buff.append("FirewallPort: ");
		buff.append(m_FirewallPortNumber);
		buff.append("\n");
		buff.append("AutoConnect: ");
		buff.append(m_bAutoConnect);
		buff.append("\n");
		buff.append("AutoReConnect: ");
		buff.append(m_bAutoReConnect);
		buff.append("\n");

		/*	private boolean m_bUseFirewallAuthentication   = false; //indicates whether JAP should use a UserID/Password to authenticat to the proxy
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

	protected void setCertCheckDisabled(boolean b)
	{
		m_bCertCheckDisabled = b;
	}

	public static boolean isCertCheckDisabled()
	{
		return model.m_bCertCheckDisabled;
	}

	public static JAPCertificateStore getCertificateStore()
	{
		return model.m_certStore;
	}

	protected void setCertificateStore(JAPCertificateStore jcs)
	{
		if(jcs!=null)
			m_certStore = jcs;
		else
			m_certStore=JAPCertificateStore.getInstance();
	}

	public static JAPCertificate getJAPCodeSigningCert()
	{
		return model.m_certJAPCodeSigning;
	}

}
