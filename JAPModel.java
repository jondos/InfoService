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
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JPasswordField;
import javax.swing.JLabel;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.UIManager;
//import anon.JAPAnonService;
import anon.JAPAnonServiceListener;
/* jh5 */ import anon.xmlrpc.Server;

/* This is the Model of All. It's a Singelton!*/
public final class JAPModel implements JAPAnonServiceListener {

	public  JAPAnonServerDB      anonServerDatabase   = null; // vector of all available mix cascades
	private AnonServerDBEntry    currentAnonService   = null; // current anon service data object
	private ServerSocket         m_socketHTTPListener = null; // listener object
	private JAPDirectProxy       proxyDirect    = null;    // service object for direct access (bypass anon service)
	private JAPAnonProxy       proxyAnon      = null;    // service object for HTTP  listener
	//private JAPAnonService       proxyAnonSocks = null;    // service object for SOCKS listener

	private String   infoServiceHostName   = JAPConstants.defaultinfoServiceHostName;
	private int      infoServicePortNumber = JAPConstants.defaultinfoServicePortNumber;
	private int      portNumber            = JAPConstants.defaultPortNumber;  // port number of HTTP  listener
//	private int      portSocksListener     = 1080;   // port number of SOCKS listener
//	private boolean  mbSocksListener       = false;  // indicates whether JAP should support SOCKS or not
	private  String  proxyHostName         = "";     // hostname of proxy
	private  int     proxyPortNumber       = -1;     // portnumber of proxy
	private String   m_ProxyAuthenticationUserID = null;  //userid for authentication
	private String   m_ProxyAuthenticationPasswd = null;  // password --> will never be saved...
	private boolean  mb_UseProxyAuthentication   = false; //indicates whether JAp should use a UserID/Password to authenticat to the proxy
	private boolean  mblistenerIsLocal           = true;  // indicates whether listeners serve for localhost only or not
	private boolean  isRunningListener           = false; // true if a listener is running
	private boolean  mbUseProxy                  = false; // indicates whether JAP connects via a proxy or directly
	public  boolean  autoConnect                 = false; // autoconnect after program start
	private boolean  mbMinimizeOnStartup         = false; // true if programm will start minimized
	public  boolean  canStartService             = false; // indicates if anon service can be started
	public  boolean  alreadyCheckedForNewVersion = false; // indicates if check for new version has already been done
	private boolean  mbActCntMessageNotRemind    = false; // indicates if Warning message in setAnonMode has been deactivated for the session
	private boolean  mbActCntMessageNeverRemind  = false; // indicates if Warning message in setAnonMode has been deactivated forever
	private boolean  mbDoNotAbuseReminder        = false; // indicates if new warning message in setAnonMode (containing Do no abuse) has been shown
	private boolean  mbGoodByMessageNeverRemind  = false; // indicates if Warning message before exit has been deactivated forever
  private boolean m_bUseDummyTraffic           = false; // indicates what Dummy Traffic should be generated or not


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

	private static Server anonServiceRMIServer= null;

	private JAPModel () {
		//JAPDebug.out(JAPDebug.INFO,JAPDebug.MISC,"JAPModel:initializing...");
		// Create observer object
		observerVector = new Vector();
		currentAnonService = new AnonServerDBEntry(JAPConstants.defaultanonHost,JAPConstants.defaultanonPortNumber);
		proxyDirect=null;
		proxyAnon=null;
		m_Locale=Locale.getDefault();
		//JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"JAPModel:initialization finished!");
	}
	/** Creates the Model - as Singleton.
	 *  @return The one and only JAPModel
	 */
	public static JAPModel create() {
		if(model==null)
			model=new JAPModel();
		return model;
	}
	public static JAPModel getModel() {
			return model;
	}
	//---------------------------------------------------------------------
	public void initialRun() {
		JAPDebug.out(JAPDebug.INFO,JAPDebug.MISC,"JAPModel:initial run of JAP...");
		// start http listener object
		if(!startHTTPListener())
			{	// start was not sucessful
				Object[] args={new Integer(portNumber)};
				String msg=MessageFormat.format(JAPMessages.getString("errorListenerPort"),args);
				// output error message
				JOptionPane.showMessageDialog(model.getView(),
										msg,
										JAPMessages.getString("errorListenerPortTitle"),
										JOptionPane.ERROR_MESSAGE);
				JAPDebug.out(JAPDebug.EMERG,JAPDebug.NET,"Cannot start listener!");
				model.status1 = JAPMessages.getString("statusCannotStartListener");
				model.getView().disableSetAnonMode();
				notifyJAPObservers();
			}
			else
				{	// listender has started correctly
					model.status1 = JAPMessages.getString("statusRunning");
					// initial setting of anonMode
					setAnonMode(autoConnect);
				}
	}
	//---------------------------------------------------------------------
	/** Loads the Configuration.
	 * First tries to read the configuration file in the users home directory
	 * and then in the JAP install directory.
	 * The configuration is a XML-File with the following structure:
	 *	<JAP
	 *		portNumber=""									// Listener-Portnumber
	 *		portNumberSocks=""						// Listener-Portnumber for SOCKS
	 *		supportSocks=""								// Will we support SOCKS ?
	 *    listenerIsLocal="true"/"false"// Listener lasucht nur an localhost ?
	 *		proxyMode="true"/"false"			// Using a HTTP-Proxy??
	 *		proxyHostName="..."						// the Name of the HTTP-Proxy
	 *		proxyPortNumber="..."					// port number of the HTTP-proxy
	 *    proxyAuthorization="true"/"false" //Need authorization to acces the proxy ?
	 *    porxyAuthUserID="..."         //UserId for the Proxy if Auth is neccesary
	 *		infoServiceHostName="..."			// hostname of the infoservice
	 *		infoServicePortnumber=".."		// the portnumber of the info service
	 *    anonserviceName=".."          //the name of the anon-service
	 *		anonHostName=".."							// the hostname of the anon-service
	 *		anonHostIP=".."							  // the ip of the anon-service
	 *		anonPortNumber=".."						// the portnumber of the anon-service
	 *    anonSSLPortNumber=".."        /the "proxy" port number of anon-service
	 *		autoConnect="true"/"false"		// should we start the anon service immedialy after programm launch ?
	 *		minimizedStartup="true"/"false" // should we start minimized ???
	 *		neverRemindActiveContent="true"/"false" // should we remind the user about active content ?
	 *    Locale="LOCALE_IDENTIFIER (two letter iso 639 code)" //the Language for the UI to use
	 *    LookAndFeel="..."             //the LookAndFeel
	 *	>
	 *	<Debug>
	 *		<Level>..</Level>							// the amount of output (0 means less.. 7 means max)
	 *		<Type													// which type of messages should be logged
	 *			GUI="true"/"false"					// messages related to the user interface
	 *			NET="true"/"false"					// messages related to the network
	 *			THREAD="true"/"false"				// messages related to threads
	 *			MISC="true"/"false"					// all the others
	 *		>
	 *		</Type>
	 *		<Output>..</Output>						//the kind of Output, at the moment only: Console
	 * 	</Debug>
	 *	</JAP>
	 */
	public synchronized void loadConfigFile() {
		// Load default anon services
		anonServerDatabase = new JAPAnonServerDB();
//		anonServerDatabase.addElement(new AnonServerDBEntry(anonHostName, anonPortNumber));
//		anonServerDatabase.addElement(new AnonServerDBEntry(proxyHostName, proxyPortNumber));
//		anonServerDatabase.addElement(new AnonServerDBEntry("anon.inf.tu-dresden.de", 6543));
//		anonServerDatabase.addElement(new AnonServerDBEntry("passat.mesh.de", 6543));
		// Load config from xml file
		JAPDebug.out(JAPDebug.INFO,JAPDebug.MISC,"JAPModel:try loading configuration from "+JAPConstants.XMLCONFFN);
		try {
			String dir=System.getProperty("user.home","");
			FileInputStream f=null;
			//first tries in user.home
			try {
				f=new FileInputStream(dir+"/"+JAPConstants.XMLCONFFN);
			} catch(Exception e) {
				f=new FileInputStream(JAPConstants.XMLCONFFN); //and then in the current directory
			}
			Document doc=DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(f);
			Element root=doc.getDocumentElement();
			NamedNodeMap n=root.getAttributes();
			//
			portNumber=JAPUtil.parseElementAttrInt(root,"portNumber",portNumber);
			mblistenerIsLocal=JAPUtil.parseNodeBoolean(n.getNamedItem("listenerIsLocal"),true);
			//portSocksListener=JAPUtil.parseElementAttrInt(root,"portNumberSocks",portSocksListener);
			//setUseSocksPort(JAPUtil.parseNodeBoolean(n.getNamedItem("supportSocks"),false));
			setUseProxy(JAPUtil.parseNodeBoolean(n.getNamedItem("proxyMode"),false));
			setUseFirewallAuthorization(JAPUtil.parseNodeBoolean(n.getNamedItem("proxyAuthorization"),false));
			// load settings for the reminder message in setAnonMode
			mbActCntMessageNeverRemind=JAPUtil.parseNodeBoolean(n.getNamedItem("neverRemindActiveContent"),false);
			mbDoNotAbuseReminder      =JAPUtil.parseNodeBoolean(n.getNamedItem("doNotAbuseReminder"),false);
			if(mbActCntMessageNeverRemind && mbDoNotAbuseReminder)
				mbActCntMessageNotRemind=true;
			// load settings for the reminder message before goodBye
			mbGoodByMessageNeverRemind=JAPUtil.parseNodeBoolean(n.getNamedItem("neverRemindGoodBye"),false);
			// load settings for Info Service
			String host;
			int port;
			host=JAPUtil.parseNodeString(n.getNamedItem("infoServiceHostName"),infoServiceHostName);
			port=JAPUtil.parseElementAttrInt(root,"infoServicePortNumber",infoServicePortNumber);
			setInfoService(host,port);
			// load settings for proxy
			host=JAPUtil.parseNodeString(n.getNamedItem("proxyHostName"),proxyHostName);
			port=JAPUtil.parseElementAttrInt(root,"proxyPortNumber",proxyPortNumber);
			if(host.equalsIgnoreCase("ikt.inf.tu-dresden.de"))
				host="";
			setProxy(host,port);
			String userid=JAPUtil.parseNodeString(n.getNamedItem("proxyAuthUserID"),m_ProxyAuthenticationUserID);
			setFirewallAuthUserID(userid);

			String anonserviceName = model.getAnonServer().getName();
			String anonHostName    = model.getAnonServer().getHost();
			String anonHostIP       =model.getAnonServer().getIP();
			int anonPortNumber     = model.getAnonServer().getPort();
			int anonSSLPortNumber  = model.getAnonServer().getSSLPort();
			anonserviceName   = JAPUtil.parseNodeString(n.getNamedItem("anonserviceName"),anonserviceName);
			anonHostName      = JAPUtil.parseNodeString(n.getNamedItem("anonHostName"),anonHostName);
			anonHostIP      = JAPUtil.parseNodeString(n.getNamedItem("anonHostIP"),anonHostIP);
			anonPortNumber    = JAPUtil.parseElementAttrInt(root,"anonPortNumber",anonPortNumber);
			anonSSLPortNumber = JAPUtil.parseElementAttrInt(root,"anonSSLPortNumber",anonSSLPortNumber);
			model.setAnonServer(new AnonServerDBEntry(anonserviceName,anonHostName,anonHostIP,anonPortNumber,anonSSLPortNumber));
			// force setting the correct name of the selected service
			model.getAnonServer().setName(anonserviceName);

			autoConnect=JAPUtil.parseNodeBoolean(n.getNamedItem("autoConnect"),false);
			mbMinimizeOnStartup=JAPUtil.parseNodeBoolean(n.getNamedItem("minimizedStartup"),false);
			//Load Locale-Settings
			String locale=JAPUtil.parseNodeString(n.getNamedItem("Locale"),m_Locale.getLanguage());
			setLocale(new Locale(locale,""));
			//Load look-and-feel settings
			String lf=JAPUtil.parseNodeString(n.getNamedItem("LookAndFeel"),"unknown");
			LookAndFeelInfo[] lfi=UIManager.getInstalledLookAndFeels();
			for(int i=0;i<lfi.length;i++) {
				if(lfi[i].getName().equals(lf)) {
					try {
						UIManager.setLookAndFeel(lfi[i].getClassName());
//				SwingUtilities.updateComponentTreeUI(m_frmParent);
//				SwingUtilities.updateComponentTreeUI(SwingUtilities.getRoot(((JComboBox)e.getItemSelectable())));
					}
					catch(Exception lfe) {
						JAPDebug.out(JAPDebug.WARNING,JAPDebug.GUI,"JAPModel:Exception while setting look-and-feel");
					}
					break;
				}
			}
			//Loading debug settings
			NodeList nl=root.getElementsByTagName("Debug");
			if(nl!=null&&nl.getLength()>0)
				{
					Element elemDebug=(Element)nl.item(0);
					nl=elemDebug.getElementsByTagName("Level");
					if(nl!=null&&nl.getLength()>0)
						{
							Element elemLevel=(Element)nl.item(0);
							JAPDebug.setDebugLevel(Integer.parseInt(elemLevel.getFirstChild().getNodeValue().trim()));
						}
					nl=elemDebug.getElementsByTagName("Type");
					if(nl!=null&&nl.getLength()>0)
						{
							Element elemType=(Element)nl.item(0);
							int debugtype=JAPDebug.NUL;
							if(elemType.getAttribute("GUI").equals("true"))
								debugtype+=JAPDebug.GUI;
							if(elemType.getAttribute("NET").equals("true"))
								debugtype+=JAPDebug.NET;
							if(elemType.getAttribute("THREAD").equals("true"))
								debugtype+=JAPDebug.THREAD;
							if(elemType.getAttribute("MISC").equals("true"))
								debugtype+=JAPDebug.MISC;
							JAPDebug.setDebugType(debugtype);
						}
					nl=elemDebug.getElementsByTagName("Output");
					if(nl!=null&&nl.getLength()>0)
						{
							Element elemOutput=(Element)nl.item(0);
							JAPDebug.showConsole(elemOutput.getFirstChild().getNodeValue().trim().equalsIgnoreCase("Console"),view);
						}
				}
		}
		catch(Exception e) {
			JAPDebug.out(JAPDebug.INFO,JAPDebug.MISC,"JAPModel:Error loading configuration! "+e.toString());
		}
		// fire event
		notifyJAPObservers();
	}
	public void saveConfigFile() {
		boolean error=false;
		JAPDebug.out(JAPDebug.INFO,JAPDebug.MISC,"JAPModel:try saving configuration to "+JAPConstants.XMLCONFFN);
		try {
			String dir=System.getProperty("user.home","");
			FileOutputStream f=null;
			//first tries in user.home
			try  {
				f=new FileOutputStream(dir+"/"+JAPConstants.XMLCONFFN);
			} catch(Exception e) {
				f=new FileOutputStream(JAPConstants.XMLCONFFN); //and then in the current directory
			}
			String sb=getConfigurationAsXML();
			if(sb!=null) {
				f.write(sb.getBytes());
				f.flush();
				f.close();
			} else
				error=true;
		} catch(Exception e) {
			error=true;
		}
		if(error) {
			JAPDebug.out(JAPDebug.ERR,JAPDebug.MISC,"JAPModel:error saving configuration to "+JAPConstants.XMLCONFFN);
			JOptionPane.showMessageDialog(model.getView(),
											JAPMessages.getString("errorSavingConfig"),
											JAPMessages.getString("errorSavingConfigTitle"),
											JOptionPane.ERROR_MESSAGE);
		}
	}
	protected String getConfigurationAsXML() {
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
	public void setLocale(Locale l) {
		JAPMessages.init(l);
		m_Locale=l;
	}
	//---------------------------------------------------------------------
	public void setMinimizeOnStartup(boolean b) {
		synchronized(this) {
			mbMinimizeOnStartup=b;
		}
	}
	public boolean getMinimizeOnStartup() {
		synchronized(this) {
			return mbMinimizeOnStartup;
		}
	}
	//---------------------------------------------------------------------
	public synchronized void setAnonServer(AnonServerDBEntry s) {
		if(s==null)
			return;
	    AnonServerDBEntry current=getAnonServer();
		if (current!=null&&current.equals(s)) {
			JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"JAPModel:currentAnonService NOT changed");
		} else {
			// if service has changed --> stop service (if running) and restart
			this.currentAnonService = s;
			if(getAnonMode()) {
				setAnonMode(false);
				JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"JAPModel:currentAnonService changed");
				setAnonMode(true);
			}
		}
		model.notifyJAPObservers();
	}
	public AnonServerDBEntry getAnonServer() {
	    return this.currentAnonService;
	}
	//---------------------------------------------------------------------
	public boolean setProxy(String host,int port) {
		if(!JAPUtil.isPort(port))
			return false;
		if(host==null)
			return false;
		// check if values have changed
		if((!host.equals(proxyHostName))||(port!=proxyPortNumber)) {
			// reset firewall authentication password
			m_ProxyAuthenticationPasswd=null;
			// change settings
			synchronized(this) {
				proxyHostName=host;
				proxyPortNumber=port;
			}
		}
		// apply changes to infoservice
		applyProxySettingsToInfoService();
		// apply changes to anonservice
		applyProxySettingsToAnonService();
		notifyJAPObservers();
		return true;
	}
	public void setUseProxy(boolean b) {
		synchronized(this) {
			// if service already runs in proxy/firewall mode, we do not have to
			// change settings of InfoService and AnonService, since setProxy()
			// has already done this.
			if(!mbUseProxy) {
				mbUseProxy=b;
				// apply changes to infoservice
				applyProxySettingsToInfoService();
				// apply changes to anonservice
				applyProxySettingsToAnonService();
			}
		}
		notifyJAPObservers();
	}
	private void applyProxySettingsToInfoService() {
		if(mInfoService!=null)
			if(mbUseProxy)
				if(mb_UseProxyAuthentication)
					mInfoService.setProxy(proxyHostName,proxyPortNumber,m_ProxyAuthenticationUserID,this.getFirewallAuthPasswd());
				else
					mInfoService.setProxy(proxyHostName,proxyPortNumber,null,null);
			else
				mInfoService.setProxyEnabled(false);
	}
	private void applyProxySettingsToAnonService() {
		if(mbUseProxy && getAnonMode()) {
			// anon service is running
			Object[] options = { JAPMessages.getString("later"),JAPMessages.getString("reconnect") };
			int ret = JOptionPane.showOptionDialog(model.getView(),
														JAPMessages.getString("reconnectAfterProxyChangeMsg"),
														JAPMessages.getString("reconnectAfterProxyChangeTitle"),
														JOptionPane.DEFAULT_OPTION,
														JOptionPane.WARNING_MESSAGE,
														null, options, options[0]);
			if(ret==1) {
				// reconnect
				setAnonMode(false);
				setAnonMode(true);
			}
		}
	}
	public void setFirewallAuthUserID(String userid) {
		// check if values have changed
		if(!userid.equals(m_ProxyAuthenticationUserID)) {
			m_ProxyAuthenticationPasswd=null;   // reset firewall authentication password
			m_ProxyAuthenticationUserID=userid; // change settings
		}
	}
	public void setUseFirewallAuthorization(boolean b) {
			mb_UseProxyAuthentication=b;
	}
	//---------------------------------------------------------------------
	public boolean getUseFirewall() {
		synchronized(this) {
			return mbUseProxy;
		}
	}
	public String getFirewallHost() {
		synchronized(this) {
			return proxyHostName;
		}
	}
	public int getFirewallPort() {
		synchronized(this) {
			return proxyPortNumber;
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
	//---------------------------------------------------------------------
	public boolean setInfoService(String host,int port) {
		if(!JAPUtil.isPort(port))
			return false;
		if(host==null)
			return false;
		synchronized(this) {
			infoServiceHostName=host;
			infoServicePortNumber=port;
			if(mInfoService!=null)
				this.getInfoService().setInfoService(host,port);
			notifyJAPObservers();
			return true;
		}
	}
	public String getInfoServiceHost() {
		synchronized(this) {
			return infoServiceHostName;
		}
	}
	public int getInfoServicePort() {
		synchronized(this) {
			return infoServicePortNumber;
		}
	}
	public JAPInfoService getInfoService() {
		if(mInfoService==null) {
			mInfoService=new JAPInfoService(infoServiceHostName,infoServicePortNumber);
			this.applyProxySettingsToInfoService();
		}
		return mInfoService;
	}
	//---------------------------------------------------------------------
/*
	public void setSocksPortNumber (int p)
		{
			portSocksListener = p;
		}
	public int getSocksPortNumber()
		{
			return portSocksListener;
		}
	public void setUseSocksPort(boolean b)
		{
			mbSocksListener=b;
		}
	public boolean getUseSocksPort()
		{
			return mbSocksListener;
		}
*/

	//---------------------------------------------------------------------
	//---------------------------------------------------------------------
	//---------------------------------------------------------------------
private final class SetAnonModeAsync implements Runnable
{
	boolean anonModeSelected=false;
	public SetAnonModeAsync(boolean b)
		{
		  anonModeSelected=b;
	  }
/** oldRun!*/
/*
	public void run() {
		synchronized(oSetAnonModeSyncObject)
		{
	//setAnonMode--> async!!
		JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"JAPModel:setAnonMode("+anonModeSelected+")");
		if ((proxyAnon == null) && (anonModeSelected == true))
			{
				view.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				//JAPSetAnonModeSplash.start(true);
	       if (alreadyCheckedForNewVersion == false)
					{
						// Check for a new Version of JAP if not already done
						int ok = versionCheck();
						if (ok == -1) {
						// -> at the moment nothing to do
						//canStartService = false; // no necessary to set this variable
						} else {
						// -> we can start anonymity
						canStartService = true;
						alreadyCheckedForNewVersion = true;
						}
					}
				if (canStartService)
					{
						// -> we can start anonymity
						if(proxyDirect!=null)
							proxyDirect.stopService();
						proxyDirect=null;
						// starting MUX --> Success ???
						proxyAnon=new JAPAnonService(m_socketHTTPListener,JAPAnonService.PROTO_HTTP);
						AnonServerDBEntry e = model.getAnonServer();
						//2001-02-20(HF)
						if (model.getUseFirewall()) {
							// connect vi proxy to first mix (via ssl portnumber)
							if (e.getSSLPort() == -1) {
								JOptionPane.showMessageDialog(model.getView(),
									JAPMessages.getString("errorFirewallModeNotSupported"),
									JAPMessages.getString("errorFirewallModeNotSupportedTitle"),
									JOptionPane.ERROR_MESSAGE);
								return; //TODO: Maybe need to check what to do...--> anonmode=false =?
							} else {
								proxyAnon.setAnonService(e.getHost(),e.getSSLPort());
								proxyAnon.setFirewall(model.getFirewallHost(),model.getFirewallPort());
								if(model.getUseFirewallAuthorization())
									{
										proxyAnon.setFirewallAuthorization(model.getFirewallAuthUserID(),
																												model.getFirewallAuthPasswd());
									}
								proxyAnon.connectViaFirewall(true);
							}
						} else {
							// connect directly to first mix
							proxyAnon.setAnonService(e.getHost(),e.getPort());
						}
						int ret=proxyAnon.start();
						if(ret==JAPAnonService.E_SUCCESS)
							{
								// show a Reminder message that active contents should be disabled
								Object[] options = {  JAPMessages.getString("okButton") };
								JCheckBox checkboxRemindNever=new JCheckBox(JAPMessages.getString("disableActCntMessageNeverRemind"));
								Object[] message={JAPMessages.getString("disableActCntMessage"),checkboxRemindNever};
								if (!mbActCntMessageNotRemind)
									{
										ret=0;
										ret= JOptionPane.showOptionDialog(model.getView(),
																		(Object)message,
																		JAPMessages.getString("disableActCntMessageTitle"),
																		JOptionPane.DEFAULT_OPTION,
																		JOptionPane.WARNING_MESSAGE,
																		null, options, options[0]);
										mbActCntMessageNeverRemind = checkboxRemindNever.isSelected();
										mbDoNotAbuseReminder       = checkboxRemindNever.isSelected();
										if(mbActCntMessageNeverRemind)
											mbActCntMessageNotRemind=true;
									}
								model.status2 = JAPMessages.getString("statusRunning");
								proxyAnon.setAnonServiceListener(model);
								// start feedback thread
								feedback=new JAPFeedback();
								feedback.startRequests();
								view.setCursor(Cursor.getDefaultCursor());
								notifyJAPObservers();
								JAPSetAnonModeSplash.abort();
								return;
							}
						if (ret==JAPAnonService.E_BIND)
							{
								Object[] args={new Integer(portNumber)};
								String msg=MessageFormat.format(JAPMessages.getString("errorListenerPort"),args);
								JOptionPane.showMessageDialog(model.getView(),
																							msg,
																							JAPMessages.getString("errorListenerPortTitle"),
																							JOptionPane.ERROR_MESSAGE);
								JAPDebug.out(JAPDebug.EMERG,JAPDebug.NET,"Listener could not be started!");
								model.getView().disableSetAnonMode();
							}
						else
							{
								JOptionPane.showMessageDialog
									(
									 getView(),
									 JAPMessages.getString("errorConnectingFirstMix")+"\n"+JAPMessages.getString("errorCode")+": "+Integer.toString(ret),
									 JAPMessages.getString("errorConnectingFirstMixTitle"),
									 JOptionPane.ERROR_MESSAGE
									);
							}
						proxyAnon=null;
						//proxyAnonSocks=null;
						view.setCursor(Cursor.getDefaultCursor());
						model.status2 = JAPMessages.getString("statusNotRunning");
						notifyJAPObservers();
						JAPSetAnonModeSplash.abort();
						setAnonMode(false);
					}
				else
					{
						view.setCursor(Cursor.getDefaultCursor());
						JAPSetAnonModeSplash.abort();
				}
		}
		else if ((proxyDirect==null) && (anonModeSelected == false))
			{
				if(proxyAnon!=null)
					{
						JAPSetAnonModeSplash.start(false);
						proxyAnon.stop();
					}
				proxyAnon=null;
				//if(proxyAnonSocks!=null)
				//	proxyAnonSocks.stop();
				//proxyAnonSocks=null;
				if(feedback!=null)
					{
						feedback.stopRequests();
						feedback=null;
					}
				model.status2 = JAPMessages.getString("statusNotRunning");
				proxyDirect=new JAPDirectProxy(m_socketHTTPListener);
				proxyDirect.startService();

				model.getAnonServer().setMixedPackets(-1);
				model.getAnonServer().setNrOfActiveUsers(-1);
				model.getAnonServer().setTrafficSituation(-1);
				model.getAnonServer().setCurrentRisk(-1);
				notifyJAPObservers();
				JAPSetAnonModeSplash.abort();
			}
	}
}*/

/*new Run!!*/
	public void run() {
		synchronized(oSetAnonModeSyncObject)
		{
	//setAnonMode--> async!!
		JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"JAPModel:setAnonMode("+anonModeSelected+")");
		if ((proxyAnon == null) && (anonModeSelected == true))
			{
				view.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				//JAPSetAnonModeSplash.start(true);
	       if (alreadyCheckedForNewVersion == false)
					{
						// Check for a new Version of JAP if not already done
						int ok = versionCheck();
						if (ok == -1) {
						// -> at the moment nothing to do
						//canStartService = false; // no necessary to set this variable
						} else {
						// -> we can start anonymity
						canStartService = true;
						alreadyCheckedForNewVersion = true;
						}
					}
				if (canStartService)
					{
						// -> we can start anonymity
						if(proxyDirect!=null)
							proxyDirect.stopService();
						proxyDirect=null;
						// starting MUX --> Success ???
						proxyAnon=new JAPAnonProxy(m_socketHTTPListener);
						AnonServerDBEntry e = model.getAnonServer();
						//2001-02-20(HF)
						if (model.getUseFirewall()) {
							// connect vi proxy to first mix (via ssl portnumber)
							if (e.getSSLPort() == -1) {
								JOptionPane.showMessageDialog(model.getView(),
									JAPMessages.getString("errorFirewallModeNotSupported"),
									JAPMessages.getString("errorFirewallModeNotSupportedTitle"),
									JOptionPane.ERROR_MESSAGE);
								return; //TODO: Maybe need to check what to do...--> anonmode=false =?
							} else {
								proxyAnon.setAnonService(e.getHost(),e.getSSLPort());
								proxyAnon.setFirewall(model.getFirewallHost(),model.getFirewallPort());
								if(model.getUseFirewallAuthorization())
									{
										proxyAnon.setFirewallAuthorization(model.getFirewallAuthUserID(),
																												model.getFirewallAuthPasswd());
									}
								//proxyAnon.connectViaFirewall(true);
							}
						} else {
							// connect directly to first mix
							proxyAnon.setAnonService(e.getHost(),e.getPort());
						}
						int ret=proxyAnon.start();
						if(ret==JAPAnonProxy.E_SUCCESS)
							{
								// show a Reminder message that active contents should be disabled
								Object[] options = {  JAPMessages.getString("okButton") };
								JCheckBox checkboxRemindNever=new JCheckBox(JAPMessages.getString("disableActCntMessageNeverRemind"));
								Object[] message={JAPMessages.getString("disableActCntMessage"),checkboxRemindNever};
								if (!mbActCntMessageNotRemind)
									{
										ret=0;
										ret= JOptionPane.showOptionDialog(model.getView(),
																		(Object)message,
																		JAPMessages.getString("disableActCntMessageTitle"),
																		JOptionPane.DEFAULT_OPTION,
																		JOptionPane.WARNING_MESSAGE,
																		null, options, options[0]);
										mbActCntMessageNeverRemind = checkboxRemindNever.isSelected();
										mbDoNotAbuseReminder       = checkboxRemindNever.isSelected();
										if(mbActCntMessageNeverRemind)
											mbActCntMessageNotRemind=true;
									}
								model.status2 = JAPMessages.getString("statusRunning");
								proxyAnon.setAnonServiceListener(model);
								// start feedback thread
								feedback=new JAPFeedback();
								feedback.startRequests();
								view.setCursor(Cursor.getDefaultCursor());
								notifyJAPObservers();
								JAPSetAnonModeSplash.abort();
								return;
							}
						if (ret==JAPAnonProxy.E_BIND)
							{
								Object[] args={new Integer(portNumber)};
								String msg=MessageFormat.format(JAPMessages.getString("errorListenerPort"),args);
								JOptionPane.showMessageDialog(model.getView(),
																							msg,
																							JAPMessages.getString("errorListenerPortTitle"),
																							JOptionPane.ERROR_MESSAGE);
								JAPDebug.out(JAPDebug.EMERG,JAPDebug.NET,"Listener could not be started!");
								model.getView().disableSetAnonMode();
							}
						else
							{
								JOptionPane.showMessageDialog
									(
									 getView(),
									 JAPMessages.getString("errorConnectingFirstMix")+"\n"+JAPMessages.getString("errorCode")+": "+Integer.toString(ret),
									 JAPMessages.getString("errorConnectingFirstMixTitle"),
									 JOptionPane.ERROR_MESSAGE
									);
							}
						proxyAnon=null;
						//proxyAnonSocks=null;
						view.setCursor(Cursor.getDefaultCursor());
						model.status2 = JAPMessages.getString("statusNotRunning");
						notifyJAPObservers();
						JAPSetAnonModeSplash.abort();
						setAnonMode(false);
					}
				else
					{
						view.setCursor(Cursor.getDefaultCursor());
						JAPSetAnonModeSplash.abort();
				}
		}
		else if ((proxyDirect==null) && (anonModeSelected == false))
			{
				if(proxyAnon!=null)
					{
						JAPSetAnonModeSplash.start(false);
						proxyAnon.stop();
					}
				proxyAnon=null;
				//if(proxyAnonSocks!=null)
				//	proxyAnonSocks.stop();
				//proxyAnonSocks=null;
				if(feedback!=null)
					{
						feedback.stopRequests();
						feedback=null;
					}
				model.status2 = JAPMessages.getString("statusNotRunning");
				proxyDirect=new JAPDirectProxy(m_socketHTTPListener);
				proxyDirect.startService();

				model.getAnonServer().setMixedPackets(-1);
				model.getAnonServer().setNrOfActiveUsers(-1);
				model.getAnonServer().setTrafficSituation(-1);
				model.getAnonServer().setCurrentRisk(-1);
				notifyJAPObservers();
				JAPSetAnonModeSplash.abort();
			}
	}
}
	}
	//---------------------------------------------------------------------
	//---------------------------------------------------------------------
	//---------------------------------------------------------------------
	public boolean getAnonMode() {
		return proxyAnon!=null;
	}
	public synchronized void setAnonMode(boolean anonModeSelected) {
		Thread t=new Thread(new SetAnonModeAsync(anonModeSelected));
		t.start();
	}

  public boolean getEnableDummyTraffic()
    {
      return m_bUseDummyTraffic;
    }

  public void setEnableDummyTraffic(boolean b)
    {
      m_bUseDummyTraffic=b;
      if(proxyAnon!=null)
        proxyAnon.setEnableDummyTraffic(b);
    }

	/*public synchronized void setAnonMode(boolean anonModeSelected)
	{
		JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"JAPModel:setAnonMode("+anonModeSelected+")");
		if ((proxyAnon == null) && (anonModeSelected == true))
			{
				view.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
	//			JAPSetAnonMode.start();
//				java.awt.Toolkit.getDefaultToolkit().sync();
//				try{javax.swing.SwingUtilities.invokeAndWait(new JAPSetAnonMode());}catch(Exception e){};
				if (alreadyCheckedForNewVersion == false)
					{
						// Check for a new Version of JAP if not already done
						int ok = this.versionCheck();
						if (ok == -1) {
						// -> at the moment nothing to do
						//canStartService = false; // no necessary to set this variable
						} else {
						// -> we can start anonymity
						canStartService = true;
						alreadyCheckedForNewVersion = true;
						}
					}
				if (canStartService)
					{
						// -> we can start anonymity
						if(proxyDirect!=null)
							proxyDirect.stopService();
						proxyDirect=null;
						// starting MUX --> Success ???
						proxyAnon=new JAPAnonService(m_socketHTTPListener,JAPAnonService.PROTO_HTTP);
						//2001-02-20(HF)
						if (model.getUseProxy()) {
							// connect vi proxy to first mix (via ssl portnumber)
							if (model.anonSSLPortNumber == -1) {
								JOptionPane.showMessageDialog(model.getView(),
									JAPMessages.getString("errorFirewallModeNotSupported"),
									JAPMessages.getString("errorFirewallModeNotSupportedTitle"),
									JOptionPane.ERROR_MESSAGE);
								proxyAnon.setAnonService(model.anonHostName,model.anonPortNumber);
								proxyAnon.setFirewall(model.getProxyHost(),model.getProxyPort());
								proxyAnon.connectViaFirewall(true);
							} else {
								proxyAnon.setAnonService(model.anonHostName,model.anonSSLPortNumber);
								proxyAnon.setFirewall(model.getProxyHost(),model.getProxyPort());
								proxyAnon.connectViaFirewall(true);
							}
						} else {
							// connect directly to first mix
							proxyAnon.setAnonService(model.anonHostName,model.anonPortNumber);
						}
						int ret=proxyAnon.start();
						if(ret==JAPAnonService.E_SUCCESS)
							{
								// show a Reminder message that active contents should be disabled
								Object[] options = { JAPMessages.getString("disableActCntMessageDontRemind"), JAPMessages.getString("okButton") };
								JCheckBox checkboxRemindNever=new JCheckBox(JAPMessages.getString("disableActCntMessageNeverRemind"));
								Object[] message={JAPMessages.getString("disableActCntMessage"),checkboxRemindNever};
								if (!mbActCntMessageNotRemind)
									{
										ret=0;
										ret= JOptionPane.showOptionDialog(model.getView(),
																		(Object)message,
																		JAPMessages.getString("disableActCntMessageTitle"),
																		JOptionPane.DEFAULT_OPTION,
																		JOptionPane.WARNING_MESSAGE,
																		null, options, options[1]);
										mbActCntMessageNeverRemind=checkboxRemindNever.isSelected();
										if(ret==0||mbActCntMessageNeverRemind)
											mbActCntMessageNotRemind=true;
									}
								if(mbSocksListener)
									{
										proxyAnonSocks=new JAPAnonService(1080,JAPAnonService.PROTO_SOCKS,model.mblistenerIsLocal);
										proxyAnonSocks.start();
									}
								model.status2 = JAPMessages.getString("statusRunning");
								proxyAnon.setAnonServiceListener(this);
								// start feedback thread
								feedback=new JAPFeedback();
								feedback.startRequests();
								view.setCursor(Cursor.getDefaultCursor());
								notifyJAPObservers();
								return;
							}
						if (ret==JAPAnonService.E_BIND)
							{
								Object[] args={new Integer(portNumber)};
								String msg=MessageFormat.format(JAPMessages.getString("errorListenerPort"),args);
								JOptionPane.showMessageDialog(model.getView(),
																							msg,
																							JAPMessages.getString("errorListenerPortTitle"),
																							JOptionPane.ERROR_MESSAGE);
								JAPDebug.out(JAPDebug.EMERG,JAPDebug.NET,"Listener could not be started!");
								model.getView().disableSetAnonMode();
							}
						else
							{
								JOptionPane.showMessageDialog
									(
									 getView(),
									 getString("errorConnectingFirstMix")+"\n"+getString("errorCode")+": "+Integer.toString(ret),
									 getString("errorConnectingFirstMixTitle"),
									 JOptionPane.ERROR_MESSAGE
									);
							}
						proxyAnon=null;
						proxyAnonSocks=null;
						view.setCursor(Cursor.getDefaultCursor());
						model.status2 = JAPMessages.getString("statusNotRunning");
						notifyJAPObservers();
						setAnonMode(false);
					}
				else
					view.setCursor(Cursor.getDefaultCursor());

		}
		else if ((proxyDirect==null) && (anonModeSelected == false))
			{
				if(proxyAnon!=null)
					proxyAnon.stop();
				proxyAnon=null;
				if(proxyAnonSocks!=null)
					proxyAnonSocks.stop();
				proxyAnonSocks=null;
				if(feedback!=null)
					{
						feedback.stopRequests();
						feedback=null;
					}
				model.status2 = JAPMessages.getString("statusNotRunning");
				proxyDirect=new JAPDirectProxy(m_socketHTTPListener);
				proxyDirect.startService();
				model.mixedPackets = -1;
				model.nrOfActiveUsers = -1;
				model.trafficSituation = -1;
				model.currentRisk = -1;
				notifyJAPObservers();
			}
	}
*/
	//---------------------------------------------------------------------
	public void setHTTPListenerConfig(int port, boolean isLocal) {
		if((portNumber!=port)||(mblistenerIsLocal!=isLocal)) {
			portNumber = port;
			synchronized(this) {
				mblistenerIsLocal=isLocal;
			}
			JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"JAPModel:HTTP listener settings changed");
			JOptionPane.showMessageDialog(model.getView(),JAPMessages.getString("confmessageListernPortChanged"));
			model.notifyJAPObservers();
		}
	}
	public int getHTTPListenerPortNumber() {
		return portNumber;
	}
	public boolean getHTTPListenerIsLocal() {
		synchronized(this) {
			return mblistenerIsLocal;
		}
	}
	//---------------------------------------------------------------------
	private boolean startHTTPListener()
		{
			JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"JAPModel:startListener");
			if (isRunningListener == false)
				{
					boolean bindOk=false;
					for(int i=0;i<10;i++) //HAck for Mac!!
						try
							{
								if(mblistenerIsLocal)
									{
										//InetAddress[] a=InetAddress.getAllByName("localhost");
										InetAddress[] a=InetAddress.getAllByName("127.0.0.1");
										JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"Try binding Listener on localhost: "+a[0]);
										m_socketHTTPListener = new ServerSocket (portNumber,50,a[0]);
									}
								else
									m_socketHTTPListener = new ServerSocket (portNumber);
								JAPDebug.out(JAPDebug.INFO,JAPDebug.NET,"Listener on port " + portNumber + " started.");
								try
									{
										m_socketHTTPListener.setSoTimeout(2000);
									}
								catch(Exception e1)
									{
										JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"Could not set listener accept timeout: Exception: "+e1.getMessage());
									}
								bindOk=true;
								break;
							}
						catch(Exception e)
							{
								JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"Exception: "+e.getMessage());
								m_socketHTTPListener=null;
							}
						isRunningListener=bindOk;
				}
			return isRunningListener;
		}
	private void stopHTTPListener()
		{
			JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"JAPModel:stopListener");
			if (isRunningListener)
				{
					setAnonMode(false);
					try{m_socketHTTPListener.close();}catch(Exception e){};
					m_socketHTTPListener=null;
					isRunningListener = false;
				}
		}
	//---------------------------------------------------------------------

	/** This (and only this!) is the final exit procedure of JAP!
	 *
	 */
	public void goodBye() {
		//stopListener();
		//view.setVisible(false);
		//iconifiedView.setVisible(false);
		// show a Reminder message that active contents should be disabled
		Object[] options = { JAPMessages.getString("okButton") };
		JCheckBox checkboxRemindNever=new JCheckBox(JAPMessages.getString("disableGoodByMessageNeverRemind"));
		Object[] message = { JAPMessages.getString("disableGoodByMessage"),checkboxRemindNever };
		if (!mbGoodByMessageNeverRemind) {
				JOptionPane.showOptionDialog(model.getView(),
												(Object)message,
												JAPMessages.getString("disableGoodByMessageTitle"),
												JOptionPane.DEFAULT_OPTION,
												JOptionPane.WARNING_MESSAGE,
												null, options, options[0]);
				mbGoodByMessageNeverRemind = checkboxRemindNever.isSelected();
		}
		model.saveConfigFile();
		System.exit(0);
	}

	/** Shows the About dialog
	 */
	public static void aboutJAP() {
		try {
			new JAPAbout(view);
		} catch(Throwable t) {
		}
	}

	/** Try to load all available MIX-Cascades form the InfoService...
	 */
	public void fetchAnonServers() {
		JAPDebug.out(JAPDebug.INFO,JAPDebug.MISC,"JAPModel:Trying to fetch anon servers from InfoService");
		AnonServerDBEntry[] db=null;
		try {
			db=getInfoService().getAvailableAnonServers();
		} catch (Exception e) {
			JAPDebug.out(JAPDebug.ERR,JAPDebug.NET,"JAPModel:fetchAnonServers: "+e);
			JOptionPane.showMessageDialog(view,
											JAPMessages.getString("errorConnectingInfoService"),
											JAPMessages.getString("errorConnectingInfoServiceTitle"),
											JOptionPane.ERROR_MESSAGE);
		}
		if((db!=null)&&(db.length>=1)) {
			anonServerDatabase.clean();
			for(int i=0;i<db.length;i++)
				anonServerDatabase.addEntry(db[i]);
			notifyJAPObservers();
		}
	}

	/** Performs the Versioncheck.
	 *  @return -1, if version check says that anonymity mode should not be enabled.
	 *          Reasons can be: new version found, version check failed
	 */
	public int versionCheck()
		{
			JAPDebug.out(JAPDebug.INFO,JAPDebug.MISC,"JAPModel:Checking for new version of JAP...");
			try
				{
					int result = 0;
					Versionchecker vc = new Versionchecker();
					String s = getInfoService().getNewVersionNumber();
					if(s==null)
						return -1;
					s=s.trim();
					// temporary changed due to stability.... (sk13)
					//String s = vc.getNewVersionnumberFromNet("http://anon.inf.tu-dresden.de:80"+aktJAPVersionFN);
					JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"JAPModel:Version:"+JAPConstants.aktVersion);
					if ( s.compareTo(JAPConstants.aktVersion) > 0 )
						{
							// OK, new version available
							// ->Ask user if he/she wants to download new version
							//	Object[] options = { JAPMessages.getString("newVersionNo"), JAPMessages.getString("newVersionYes") };
							//	ImageIcon   icon = loadImageIcon(DOWNLOADFN,true);
							String answer;
							JAPLoading japLoading = new JAPLoading(this,view);
								answer = japLoading.message(JAPMessages.getString("newVersionAvailableTitle"),
						   JAPMessages.getString("newVersionAvailable"),
						   JAPMessages.getString("newVersionNo"),
						   JAPMessages.getString("newVersionYes"),
						   true,false);
					if (answer.equals(JAPMessages.getString("newVersionYes"))) {
						// User has elected to download new version of JAP
						// ->Download, Alert, exit program
						// To do: show busy message
						try {
							vc.registerProgress(japLoading);
							vc.getVersionFromNet(JAPConstants.urlJAPNewVersionDownload,JAPConstants.JAPLocalFilename);
							Thread t = new Thread(vc);
							t.start();
							answer = japLoading.message(JAPMessages.getString("downloadingProgressTitle"),
						  JAPMessages.getString("downloadingProgress"),
						   null,
						   null,
						   true,true);
							t.join();
							result = vc.getResult();
							if (result == 0) {
							//
								answer = japLoading.message(JAPMessages.getString("newVersionAvailableTitle"),
							  JAPMessages.getString("newVersionLoaded"),
							  null,
							  "OK",
							  true,false);
								goodBye();
						} else {
							throw new Exception("Error loading new version");
						}
					}
					catch (Exception e) {
						// Download failed
						// Alert, and reset anon mode to false
						JAPDebug.out(JAPDebug.ERR,JAPDebug.MISC,"JAPModel:versionCheck(): Exception" + e);
						JOptionPane.showMessageDialog(view,
																					JAPMessages.getString("downloadFailed")+JAPMessages.getString("infoURL"),
																					JAPMessages.getString("downloadFailedTitle"),
																					JOptionPane.ERROR_MESSAGE);
						notifyJAPObservers();
						return -1;
					}
				} else {
					// User has elected not to download
					// ->Alert, we should'nt start the system due to possible compatibility problems
					answer = japLoading.message(JAPMessages.getString("youShouldUpdateTitle"),
						   JAPMessages.getString("youShouldUpdate")+JAPMessages.getString("infoURL"),
						   null,
						   "OK",
						   true,false);
					notifyJAPObservers();
					return -1;
				}
			}
			//endif ( s.compareTo(JAPConstants.aktVersion) > 0 )
			// --> no new version available, i.e. you are running the newest version of JAP
			return 0; // meaning: version check says that anonymity service can be started
		}
		catch (Exception e) {
			// Verson check failed
			// ->Alert, and reset anon mode to false
			JAPDebug.out(JAPDebug.ERR,JAPDebug.MISC,"JAPModel: "+e);
			JOptionPane.showMessageDialog(view,
																		JAPMessages.getString("errorConnectingInfoService"),
																		JAPMessages.getString("errorConnectingInfoServiceTitle"),
																		JOptionPane.ERROR_MESSAGE);
			notifyJAPObservers();
			return -1;
		}
		// this line should never be reached
	}
	//---------------------------------------------------------------------
	public void registerView(JAPView v) {
			view=v;
	}
	public static JAPView getView() {
			return model.view;
	}
	//---------------------------------------------------------------------
	public void addJAPObserver(JAPObserver o)
		{
			observerVector.addElement(o);
		}
	public void notifyJAPObservers()
		{
	//		JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"JAPModel:notifyJAPObservers()");
			synchronized(observerVector)
				{
					Enumeration enum = observerVector.elements();
					while (enum.hasMoreElements())
						{
							JAPObserver listener = (JAPObserver)enum.nextElement();
							listener.valuesChanged();
						}
				}
		//	JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"JAPModel:notifyJAPObservers()-ended");
		}
	//---------------------------------------------------------------------
	public void channelsChanged(int channels)
		{
			nrOfChannels=channels;
					Enumeration enum = observerVector.elements();
					while (enum.hasMoreElements())
						{
							JAPObserver listener = (JAPObserver)enum.nextElement();
							listener.channelsChanged(channels);
						}
		}
	public void transferedBytes(int bytes)
		{
			nrOfBytes+=bytes;
					Enumeration enum = observerVector.elements();
					while (enum.hasMoreElements())
						{
							JAPObserver listener = (JAPObserver)enum.nextElement();
							listener.transferedBytes(nrOfBytes);
						}
		}
	//---------------------------------------------------------------------
	public int setRMISupport(boolean b) {
		if(b) {
			if(anonServiceRMIServer==null)
				anonServiceRMIServer = Server.generateServer();
				anonServiceRMIServer.start();
		} else {
			if(anonServiceRMIServer!=null)
				anonServiceRMIServer.stop();
			anonServiceRMIServer=null;
		}
		return 0;
	}
}

