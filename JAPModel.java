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
import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Vector;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.NamedNodeMap;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.Frame;
import java.awt.Cursor;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.net.ServerSocket;
import java.net.InetAddress;
//import anon.JAPAnonService;
import anon.JAPAnonServiceListener;
import javax.swing.UIManager;
/* This is the Model of All. It's a Singelton!*/
public final class JAPModel {

	//public  JAPAnonServerDB      anonServerDatabase   = null; // vector of all available mix cascades
	//private AnonServerDBEntry    currentAnonService   = null; // current anon service data object
	//private ServerSocket         m_socketHTTPListener = null; // listener object
	//private JAPDirectProxy       proxyDirect    = null;    // service object for direct access (bypass anon service)
	//private JAPAnonProxy       proxyAnon      = null;    // service object for HTTP  listener
	//private JAPAnonService       proxyAnonSocks = null;    // service object for SOCKS listener

	private String   m_InfoServiceHostName   = JAPConstants.defaultinfoServiceHostName;
	private int      m_InfoServicePortNumber = JAPConstants.defaultinfoServicePortNumber;
	//private int      portNumber            = JAPConstants.defaultPortNumber;  // port number of HTTP  listener
//	private int      portSocksListener     = 1080;   // port number of SOCKS listener
//	private boolean  mbSocksListener       = false;  // indicates whether JAP should support SOCKS or not
	private  String  m_FirewallHostName         = "";     // hostname of proxy
	private  int     m_FirewallPortNumber       = -1;     // portnumber of proxy
	private String   m_FirewallAuthenticationUserID = null;  //userid for authentication
	private String   m_FirewallAuthenticationPasswd = null;  // password --> will never be saved...
	//private boolean  mb_UseProxyAuthentication   = false; //indicates whether JAp should use a UserID/Password to authenticat to the proxy
	//private boolean  mblistenerIsLocal           = true;  // indicates whether listeners serve for localhost only or not
	//private boolean  isRunningListener           = false; // true if a listener is running
	//private boolean  mbUseProxy                  = false; // indicates whether JAP connects via a proxy or directly
	//public  boolean  autoConnect                 = false; // autoconnect after program start
	//private boolean  mbMinimizeOnStartup         = false; // true if programm will start minimized
	//public  boolean  canStartService             = false; // indicates if anon service can be started
	//public  boolean  alreadyCheckedForNewVersion = false; // indicates if check for new version has already been done
	//private boolean  mbActCntMessageNotRemind    = false; // indicates if Warning message in setAnonMode has been deactivated for the session
	//private boolean  mbActCntMessageNeverRemind  = false; // indicates if Warning message in setAnonMode has been deactivated forever
	//private boolean  mbDoNotAbuseReminder        = false; // indicates if new warning message in setAnonMode (containing Do no abuse) has been shown
	//private boolean  mbGoodByMessageNeverRemind  = false; // indicates if Warning message before exit has been deactivated forever
  //private boolean m_bUseDummyTraffic           = false; // indicates what Dummy Traffic should be generated or not
/*

	private	static final Object oSetAnonModeSyncObject=new Object();

	public  String   status1           = " ";
	public  String   status2           = " ";

	private int      nrOfChannels      = 0;
	private int      nrOfBytes         = 0;

	private static  JAPView           view          = null;
//	private static  JAPViewIconified  iconifiedView = null;
	private static  JAPInfoService    mInfoService  = null;
	private static  JAPModel          model         = null;
	private static  JAPFeedback       feedback      = null;
//	public JAPLoading japLoading;
//	private ResourceBundle msg;
	private Locale m_Locale=null;
	private Vector observerVector=null;
*/

	private static  JAPModel          model         = null;

	private JAPModel ()
    {
		 // m_Locale=Locale.getDefault();
	  }

	/** Creates the Model - as Singleton.
	 *  @return The one and only JAPModel
	 */
	public static JAPModel create()
    {
		  if(model==null)
			  model=new JAPModel();
		  return model;
	  }

  public static JAPModel getModel()
    {
			return model;
	  }

	protected void setInfoServiceHost(String host)
    {
			m_InfoServiceHostName=host;
		}

  public static String getInfoServiceHost()
    {
      return model.m_InfoServiceHostName;
	  }

  protected void setInfoServicePort(int port)
    {
			model.m_FirewallPortNumber=port;
		}

  public static int getInfoServicePort()
    {
			return model.m_InfoServicePortNumber;
		}

  protected void setFirewallHost(String host)
    {
      m_FirewallHostName=host;
    }

	public static String getFirewallHost()
    {
	    return model.m_FirewallHostName;
	  }

  protected void setFirewallPort(int port)
    {
      m_FirewallPortNumber=port;
    }

  public static int getFirewallPort()
    {
	    return model.m_FirewallPortNumber;
		}

  protected void setFirewallAuthUserID(String userid)
    {
			m_FirewallAuthenticationUserID=userid;
	  }

	public static String getFirewallAuthUserID()
    {
			return model.m_FirewallAuthenticationUserID;
	  }

	protected void setFirewallAuthPasswd(String passwd)
    {
			m_FirewallAuthenticationPasswd=passwd;
	  }

	public static String getFirewallAuthPasswd()
    {
			return model.m_FirewallAuthenticationPasswd;
	  }

	//---------------------------------------------------------------------
	/*protected String getConfigurationAsXML() {
		// Save config to xml file
		// Achtung!! Fehler im Sun-XML --> NULL-Attributte koennen hinzugefuegt werden,
		// beim Abspeichern gibt es dann aber einen Fehler!
		try {
			Document doc=DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
			Element e=doc.createElement("JAP");
			doc.appendChild(e);
			//
			e.setAttribute("portNumber",Integer.toString(portNumber));
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
		return null;
	}
	//---------------------------------------------------------------------
	public Locale getLocale() {
		return m_Locale;
	}
	//---------------------------------------------------------------------
	public boolean getMinimizeOnStartup() {
		synchronized(this) {
			return mbMinimizeOnStartup;
		}
	}
	//---------------------------------------------------------------------
	public AnonServerDBEntry getAnonServer() {
	    return this.currentAnonService;
	}
	//---------------------------------------------------------------------
	//---------------------------------------------------------------------
	public boolean getUseFirewall() {
		synchronized(this) {
			return mbUseProxy;
		}
	}

	public boolean getUseFirewallAuthorization() {
			return mb_UseProxyAuthentication;
	}
	public String getFirewallAuthUserID() {
			return m_ProxyAuthenticationUserID;
	}
	public String getFirewallAuthPasswd() {
		if(mb_UseProxyAuthentication) {
			if(m_ProxyAuthenticationPasswd==null)
				m_ProxyAuthenticationPasswd=JAPFirewallPasswdDlg.getPasswd();
				return m_ProxyAuthenticationPasswd;
			} else
				return null;
	}
	}*/
/*	public JAPInfoService getInfoService() {
		if(mInfoService==null) {
			mInfoService=new JAPInfoService(infoServiceHostName,infoServicePortNumber);
			this.applyProxySettingsToInfoService();
		}
		return mInfoService;
	}*/
	//---------------------------------------------------------------------


	//---------------------------------------------------------------------
	//---------------------------------------------------------------------
	//---------------------------------------------------------------------
	/*public boolean getAnonMode() {
		return proxyAnon!=null;
	}


  public boolean getEnableDummyTraffic()
    {
      return m_bUseDummyTraffic;
    }


	public int getHTTPListenerPortNumber() {
		return portNumber;
	}
	public boolean getHTTPListenerIsLocal() {
		synchronized(this) {
			return mblistenerIsLocal;
		}
	}


*/


	/*public static JAPView getView() {
			return model.view;
	}*/
	//---------------------------------------------------------------------
	//---------------------------------------------------------------------


}