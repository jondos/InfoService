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
import anon.JAPAnonService;
import anon.JAPAnonServiceListener;
/* jh5 */ import rmi.JAPAnonServiceRMIServer;

/* This is the Model of All. It's a Singelton!*/
public final class JAPModel implements JAPAnonServiceListener{

	public static final String aktVersion = "00.01.022"; // Version of JAP

	public  Vector            anonServerDatabase = null; // vector of all available mix cascades
	public final static String   defaultanonHost   = "mix.inf.tu-dresden.de";
	public final static int defaultanonPortNumber=6544;
	private AnonServerDBEntry currentAnonService = null; // current anon service data object

	private ServerSocket   m_socketHTTPListener;     // listener object

	private JAPDirectProxy proxyDirect    = null;    // service object for direct access (bypass anon service)
	private JAPAnonService proxyAnon      = null;    // service object for HTTP  listener
	private JAPAnonService proxyAnonSocks = null;    // service object for SOCKS listener

	public final static int defaultPortNumber=4001;
	private int      portNumber            = defaultPortNumber;   // port number of HTTP  listener
	private int      portSocksListener     = 1080;   // port number of SOCKS listener
	private boolean  mbSocksListener       = false;  // indicates whether JAP should support SOCKS or not
	private boolean  mblistenerIsLocal     = true;   // indicates whether listeners serve for localhost only or not
	private boolean  isRunningListener     = false;  // true if a listener is running

	public final static String   defaultinfoServiceHostName   = "infoservice.inf.tu-dresden.de";
	private String   infoServiceHostName   = defaultinfoServiceHostName;
	public final static int defaultinfoServicePortNumber=6543;
	private int      infoServicePortNumber = defaultinfoServicePortNumber;

	private boolean  mbUseProxy            = false;  // indicates whether JAP connects via a proxy or directly
	private  String  proxyHostName         = "";     // hostname of proxy
	private  int     proxyPortNumber       = -1;     // portnumber of proxy
	private boolean  mb_UseProxyAuthentication   = false; //indicates whether JAp should use a UserID/Password to authenticat to the proxy
	private String   m_ProxyAuthenticationUserID = null;  //userid for authentication
	private String   m_ProxyAuthenticationPasswd = null;  // password --> will never be saved...
	public  boolean  autoConnect                 = false; // autoconnect after program start
	private boolean  mbMinimizeOnStartup         = false; // true if programm will start minimized
	public  boolean  canStartService             = false; // indicates if anon service can be started
	public  boolean  alreadyCheckedForNewVersion = false; // indicates if check for new version has already been done
	private boolean  mbActCntMessageNotRemind    = false; // indicates if Warning message in setAnonMode has been deactivated for the session
	private boolean  mbActCntMessageNeverRemind  = false; // indicates if Warning message in setAnonMode has been deactivated forever
	private boolean  mbDoNotAbuseReminder        = false; // indicates if new warning message in setAnonMide (containing Do no abuse) has been shown

	private	static final Object oSetAnonModeSyncObject=new Object();

	public  String   status1           = " ";
	public  String   status2           = " ";

	private int      nrOfChannels      = 0;
	private int      nrOfBytes         = 0;

	static private   JAPView           view          = null;
	static private   JAPViewIconified  iconifiedView = null;
	static private   JAPInfoService    mInfoService  = null;

// 2000-08-01(HF): the following url is now defined in JAPMessages.properties:
// usage: model.getString("infoURL")
//  static final String url_download_version       = "http://www.inf.tu-dresden.de/~hf2/anon/JAP/";
//	static final String aktJAPVersionFN            = "/~sk13/anon/jap/aktVersion.txt"; // retrieved from Info Service
	static final String aktJAPVersionFN            = "/aktVersion"; // retrieved from Info Service
	static final String urlJAPNewVersionDownload   = "/~sk13/anon/jap/JAP.jar"; // also retrieved from Info Service
	static final String JAPLocalFilename           = "JAP.jar";


	static final int    MAXHELPLANGUAGES = 6;
	static final String TITLE = "JAP";
	static final String AUTHOR = "(c) 2000 The JAP-Team";

// The following two definitions now in JAPUtil - due to a Henne - Ei problem
	static final String XMLCONFFN       = "jap.conf";
	public static final String BUSYFN   = "busy.gif";
//	public static final String SPLASHFN = "splash.gif";
	public static final String ABOUTFN  = "info.gif";
	static final String DOWNLOADFN      = "install.gif";
	static final String IICON16FN       = "icon16.gif";
	static final String ICONFN          = "icon.gif";
	static final String JAPTXTFN        = "japtxt.gif";
	static final String JAPEYEFN        = "japeye.gif";
	static final String JAPICONFN       = "japi.gif";
	static final String CONFIGICONFN    = "icoc.gif";
	static final String ICONIFYICONFN   = "iconify.gif";
	static final String ENLARGEYICONFN  = "enlarge.gif";
	static final String METERICONFN     = "icom.gif";
	static final String[] METERFNARRAY  = {
						"meterD.gif",    // anonymity deactivated
						"meterNnew.gif", // no measure available
						"meter1.gif",
						"meter2.gif",
						"meter3.gif",
						"meter4.gif",
						"meter5.gif",
						"meter6.gif"
						};

	private ResourceBundle msg;

	private Locale m_Locale=null;
	private Vector observerVector=null;
	private static JAPModel model=null;
//	public JAPLoading japLoading;
	private static JAPFeedback feedback=null;

	/* jh5 */ private static JAPAnonServiceRMIServer anonServiceRMIServer= null;

	private JAPModel ()
		{
			//JAPDebug.out(JAPDebug.INFO,JAPDebug.MISC,"JAPModel:initializing...");
			// Create observer object
			observerVector = new Vector();
			currentAnonService = new AnonServerDBEntry(defaultanonHost,defaultanonPortNumber);
			proxyDirect=null;
			proxyAnon=null;
			m_Locale=Locale.getDefault();
			//JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"JAPModel:initialization finished!");
		}

	/** Creates the Model - as Singleton.
	 * @return The one and only JAPModel
	 */
	public static JAPModel createModel()
		{
			if(model==null)
				model=new JAPModel();
			return model;
		}

	public static JAPModel getModel() {
			return model;
	}

	public int setRMISupport(boolean b)
		{
			if(b)
				{
					if(anonServiceRMIServer==null)
			/* jh5 */ anonServiceRMIServer = new JAPAnonServiceRMIServer(this);
				}
			else
				{
					if(anonServiceRMIServer!=null)
						anonServiceRMIServer.quitServer();
					anonServiceRMIServer=null;
				}
			return 0;
		}

	//---------------
	public void setIconifiedView(JAPViewIconified v)
		{
			iconifiedView=v;
		}

	public JAPViewIconified getIconifiedView()
		{
			return iconifiedView;
		}

	//---------------
	public void setView(JAPView v)
		{
			view=v;
		}

	public static JAPView getView()
		{
			return model.view;
		}

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
	 *		anonHostName=".."							// the hostname of the anon-service
	 *		anonPortNumber=".."						// the portnumber of the anon-service
	 *		autoConnect="true"/"false"		// should we start the anon service immedialy after programm launch ?
	 *		minimizedStartup="true"/"false" // should we start minimized ???
	 *		neverRemindActiveContent="true"/"false" // should we remind the user about active content ?
	 *    Locale="LOCALE_IDENTIFIER (two letter iso 639 code)" //the Language for the UI to use
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
	public synchronized void load() {
		// Load default anon services
		anonServerDatabase = new Vector();
//		anonServerDatabase.addElement(new AnonServerDBEntry(anonHostName, anonPortNumber));
//		anonServerDatabase.addElement(new AnonServerDBEntry(proxyHostName, proxyPortNumber));
//		anonServerDatabase.addElement(new AnonServerDBEntry("anon.inf.tu-dresden.de", 6543));
//		anonServerDatabase.addElement(new AnonServerDBEntry("passat.mesh.de", 6543));
		// Load config from xml file
		JAPDebug.out(JAPDebug.INFO,JAPDebug.MISC,"JAPModel:try loading configuration from "+XMLCONFFN);
		try {
			String dir=System.getProperty("user.home","");
			FileInputStream f=null;
			try  //first tries in user.home
				{
					f=new FileInputStream(dir+"/"+XMLCONFFN);
				}
			catch(Exception e)
				{
					f=new FileInputStream(XMLCONFFN); //and then in the current directory
				};
			Document doc=DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(f);
			Element root=doc.getDocumentElement();
			NamedNodeMap n=root.getAttributes();
			//
			portNumber=JAPUtil.parseElementAttrInt(root,"portNumber",portNumber);
			portSocksListener=JAPUtil.parseElementAttrInt(root,"portNumberSocks",portSocksListener);
			setUseSocksPort(JAPUtil.parseNodeBoolean(n.getNamedItem("supportSocks"),false));
			setListenerIsLocal(JAPUtil.parseNodeBoolean(n.getNamedItem("listenerIsLocal"),true));
			setUseProxy(JAPUtil.parseNodeBoolean(n.getNamedItem("proxyMode"),false));
			setUseFirewallAuthorization(JAPUtil.parseNodeBoolean(n.getNamedItem("proxyAuthorization"),false));
			// load settings for the reminder message in setAnonMode
			mbActCntMessageNeverRemind=JAPUtil.parseNodeBoolean(n.getNamedItem("neverRemindActiveContent"),false);
			mbDoNotAbuseReminder      =JAPUtil.parseNodeBoolean(n.getNamedItem("doNotAbuseReminder"),false);
			if(mbActCntMessageNeverRemind && mbDoNotAbuseReminder)
				mbActCntMessageNotRemind=true;
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
			int anonPortNumber     = model.getAnonServer().getPort();
			int anonSSLPortNumber  = model.getAnonServer().getSSLPort();
			anonserviceName   = JAPUtil.parseNodeString(n.getNamedItem("anonserviceName"),anonserviceName);
			anonHostName      = JAPUtil.parseNodeString(n.getNamedItem("anonHostName"),anonHostName);
			anonPortNumber    = JAPUtil.parseElementAttrInt(root,"anonPortNumber",anonPortNumber);
			anonSSLPortNumber = JAPUtil.parseElementAttrInt(root,"anonSSLPortNumber",anonSSLPortNumber);
			model.setAnonServer(new AnonServerDBEntry(anonserviceName,anonHostName,anonPortNumber,anonSSLPortNumber));
			// force setting the correct name of the selected service
			model.getAnonServer().setName(anonserviceName);

			autoConnect=JAPUtil.parseNodeBoolean(n.getNamedItem("autoConnect"),false);
			mbMinimizeOnStartup=JAPUtil.parseNodeBoolean(n.getNamedItem("minimizedStartup"),false);

			//Locale-Settings
			String locale=JAPUtil.parseNodeString(n.getNamedItem("Locale"),m_Locale.getLanguage());
			setLocale(new Locale(locale,""));

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

	public void save() {
		boolean error=false;
		JAPDebug.out(JAPDebug.INFO,JAPDebug.MISC,"JAPModel:try saving configuration to "+XMLCONFFN);
		try {
			String dir=System.getProperty("user.home","");
			FileOutputStream f=null;
			try  //first tries in user.home
				{
					f=new FileOutputStream(dir+"/"+XMLCONFFN);
				}
			catch(Exception e)
				{
					f=new FileOutputStream(XMLCONFFN); //and then in the current directory
				};
		   String sb=getConfigurationAsXML();
			 if(sb!=null)
				{
					f.write(sb.getBytes());
					f.flush();
					f.close();
				}
			else
				error=true;
			}
		catch(Exception e)
			{
				error=true;
			}
		if(error)
			{
				JAPDebug.out(JAPDebug.ERR,JAPDebug.MISC,"JAPModel:error saving configuration to "+XMLCONFFN);
				JOptionPane.showMessageDialog(model.getView(),
											model.getString("errorSavingConfig"),
											model.getString("errorSavingConfigTitle"),
											JOptionPane.ERROR_MESSAGE);
			}
	  }
	protected String getConfigurationAsXML()

	{
		// Save config to xml file
		// Achtung!! Fehler im Sun-XML --> NULL-Attributte können hinzugefügt werden,
		// beim Abspeichern gibt es dann aber einen Fehler!
		try {
			Document doc=DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
			Element e=doc.createElement("JAP");
			doc.appendChild(e);
			//
			e.setAttribute("portNumber",Integer.toString(portNumber));
			e.setAttribute("portNumberSocks",Integer.toString(portSocksListener));
			e.setAttribute("supportSocks",(getUseSocksPort()?"true":"false"));
			e.setAttribute("listenerIsLocal",(getListenerIsLocal()?"true":"false"));
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
			e.setAttribute("anonPortNumber",   Integer.toString(e1.getPort()));
			e.setAttribute("anonSSLPortNumber",Integer.toString(e1.getSSLPort()));
			e.setAttribute("autoConnect",(autoConnect?"true":"false"));
			e.setAttribute("minimizedStartup",(mbMinimizeOnStartup?"true":"false"));
			e.setAttribute("neverRemindActiveContent",(mbActCntMessageNeverRemind?"true":"false"));
			e.setAttribute("doNotAbuseReminder",(mbDoNotAbuseReminder?"true":"false"));
			e.setAttribute("Locale",m_Locale.getLanguage());
			//
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

	public void initialRun()
		{
			JAPDebug.out(JAPDebug.INFO,JAPDebug.MISC,"JAPModel:initial run of JAP...");
			//pre initalize some long time initalizations...
			JAPAnonService.init();
			// start Service if autoConnect
			if(!startHTTPListener())
				{
					Object[] args={new Integer(portNumber)};
					String msg=MessageFormat.format(model.getString("errorListenerPort"),args);
					JOptionPane.showMessageDialog(model.getView(),
											msg,
											model.getString("errorListenerPortTitle"),
											JOptionPane.ERROR_MESSAGE);
					JAPDebug.out(JAPDebug.EMERG,JAPDebug.NET,"Cannot start listener!");
					model.status1 = model.getString("statusCannotStartListener");
					model.getView().disableSetAnonMode();
					notifyJAPObservers();
				}
			else
				{
					model.status1 = model.getString("statusRunning");
					setAnonMode(autoConnect);
				}
		}

	public Locale getLocale()
		{
			return m_Locale;
		}

	public void setLocale(Locale l)
		{
			JAPMessages.init(l);
			m_Locale=l;
		}







	//---------------------------------------------------------------------

	public synchronized void setAnonServer(AnonServerDBEntry s)
		{
			if(s==null)
				return;
	    AnonServerDBEntry current=getAnonServer();
		  if (current!=null&&current.equals(s))
				{
					JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"JAPModel:currentAnonService NOT changed");
				}
		  else
				{
					// if service has changed --> stop service (if running) and restart
			    this.currentAnonService = s;
			    if(getAnonMode())
					  {
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

	public void setPortNumber (int p) {
		portNumber = p;
	}

	public int getPortNumber() {
		return portNumber;
	}

	public void setSocksPortNumber (int p)
		{
			portSocksListener = p;
		}

	public int getSocksPortNumber()
		{
			return portSocksListener;
		}

	public static String getString(String key)
		{
			return JAPMessages.getString(key);
		}

	public void setUseSocksPort(boolean b)
		{
			mbSocksListener=b;
		}

	public boolean getUseSocksPort()
		{
			return mbSocksListener;
		}

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

/*	public int getNrOfChannels()
		{
			return nrOfChannels;
		}
	*/
	public void transferedBytes(int bytes)
		{
			nrOfBytes+=bytes;
					Enumeration enum = observerVector.elements();
					while (enum.hasMoreElements())
						{
							JAPObserver listener = (JAPObserver)enum.nextElement();
							listener.transferedBytes(nrOfBytes);
						}
//			notifyJAPObservers();
		}
	/*
	public int getNrOfBytes() {
		return nrOfBytes;
	}
*/
	public boolean setInfoService(String host,int port)
		{
			if(!JAPUtil.isPort(port))
				return false;
			if(host==null)
				return false;
			synchronized(this)
				{
					infoServiceHostName=host;
					infoServicePortNumber=port;
					if(mInfoService!=null)
						mInfoService.setInfoService(host,port);
					notifyJAPObservers();
					return true;
				}
		}


	public String getInfoServiceHost()
		{
			synchronized(this)
				{
					return infoServiceHostName;
				}
		}

	public int getInfoServicePort()
		{
			synchronized(this)
				{
					return infoServicePortNumber;
				}
		}


	public JAPInfoService getInfoService()
		{
			if(mInfoService==null)
				mInfoService=new JAPInfoService();
			return mInfoService;
		}

	public void setListenerIsLocal(boolean b)
		{
			synchronized(this)
				{
					mblistenerIsLocal=b;
				}
		}

	public boolean getListenerIsLocal()
		{
			synchronized(this)
				{
					return mblistenerIsLocal;
				}
		}

	public void setUseProxy(boolean b)
		{
			synchronized(this)
				{
					mbUseProxy=b;
					if(mInfoService!=null)
						{
							if(mbUseProxy)
								mInfoService.setProxy(proxyHostName,proxyPortNumber,null,null);
							else
								mInfoService.setProxy(null,0,null,null);
						}
				}
			notifyJAPObservers();
		}


	public void setMinimizeOnStartup(boolean b)
		{
			synchronized(this)
				{
					mbMinimizeOnStartup=b;
				}
		}

	public boolean getMinimizeOnStartup()
		{
			synchronized(this)
				{
					return mbMinimizeOnStartup;
				}
		}

	public boolean setProxy(String host,int port)
		{
			if(!JAPUtil.isPort(port))
				return false;
			if(host==null)
				return false;
			synchronized(this)
				{
					proxyHostName=host;
					proxyPortNumber=port;
					if(mInfoService!=null&&mbUseProxy)
						mInfoService.setProxy(host,port,null,null);
					notifyJAPObservers();
					return true;
				}
		}

	public boolean getUseFirewall()
		{
			synchronized(this)
				{
					return mbUseProxy;
				}
		}

	public String getFirewallHost()
		{
			synchronized(this)
				{
					return proxyHostName;
				}
		}

	public int getFirewallPort()
		{
			synchronized(this)
				{
					return proxyPortNumber;
				}
		}

	public void setUseFirewallAuthorization(boolean b)
		{
			mb_UseProxyAuthentication=b;
		}

	public boolean getUseFirewallAuthorization()
		{
			return mb_UseProxyAuthentication;
		}

	public void  setFirewallAuthUserID(String userid)
		{
			m_ProxyAuthenticationUserID=userid;
		}

	public String getFirewallAuthUserID()
		{
			return m_ProxyAuthenticationUserID;
		}

	public String getFirewallAuthPasswd()
		{
			if(mb_UseProxyAuthentication)
				{
					if(m_ProxyAuthenticationPasswd==null)
						m_ProxyAuthenticationPasswd=JAPFirewallPasswdDlg.getPasswd();
					return m_ProxyAuthenticationPasswd;
				}
			else
				return null;
		}

	/*
	public synchronized void setJAPViewIconified() {
		JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"JAPModel:setJAPViewIconified()");
		view.setVisible(false);
		iconifiedView.setVisible(true);
	}

	public synchronized void setJAPViewDeIconified() {
		JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"JAPModel:setJAPViewDeIconified()");
		iconifiedView.setVisible(false);
		view.setVisible(true);
	}
*/

private final class SetAnonModeAsync implements Runnable
{
	boolean anonModeSelected=false;
	public SetAnonModeAsync(boolean b)
		{
		  anonModeSelected=b;
	  }

	public void run() {
		synchronized(oSetAnonModeSyncObject)
		{
	//setAnonMode--> async!!
		JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"JAPModel:setAnonMode("+anonModeSelected+")");
		if ((proxyAnon == null) && (anonModeSelected == true))
			{
				view.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				JAPSetAnonModeSplash.start(true);
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
									model.getString("errorFirewallModeNotSupported"),
									model.getString("errorFirewallModeNotSupportedTitle"),
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
								Object[] options = { model.getString("disableActCntMessageDontRemind"), model.getString("okButton") };
								JCheckBox checkboxRemindNever=new JCheckBox(model.getString("disableActCntMessageNeverRemind"));
								Object[] message={model.getString("disableActCntMessage"),checkboxRemindNever};
								if (!mbActCntMessageNotRemind)
									{
										ret=0;
										ret= JOptionPane.showOptionDialog(model.getView(),
																		(Object)message,
																		model.getString("disableActCntMessageTitle"),
																		JOptionPane.DEFAULT_OPTION,
																		JOptionPane.WARNING_MESSAGE,
																		null, options, options[1]);
										mbActCntMessageNeverRemind = checkboxRemindNever.isSelected();
										mbDoNotAbuseReminder       = checkboxRemindNever.isSelected();
										if(ret==0||mbActCntMessageNeverRemind)
											mbActCntMessageNotRemind=true;
									}
								if(mbSocksListener)
									{
										proxyAnonSocks=new JAPAnonService(1080,JAPAnonService.PROTO_SOCKS,model.mblistenerIsLocal);
										proxyAnonSocks.start();
									}
								model.status2 = model.getString("statusRunning");
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
								String msg=MessageFormat.format(model.getString("errorListenerPort"),args);
								JOptionPane.showMessageDialog(model.getView(),
																							msg,
																							model.getString("errorListenerPortTitle"),
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
						model.status2 = model.getString("statusNotRunning");
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
				if(proxyAnonSocks!=null)
					proxyAnonSocks.stop();
				proxyAnonSocks=null;
				if(feedback!=null)
					{
						feedback.stopRequests();
						feedback=null;
					}
				model.status2 = model.getString("statusNotRunning");
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
	public synchronized void setAnonMode(boolean anonModeSelected)
		{
			Thread t=new Thread(new SetAnonModeAsync(anonModeSelected));
			t.start();
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
									model.getString("errorFirewallModeNotSupported"),
									model.getString("errorFirewallModeNotSupportedTitle"),
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
								Object[] options = { model.getString("disableActCntMessageDontRemind"), model.getString("okButton") };
								JCheckBox checkboxRemindNever=new JCheckBox(model.getString("disableActCntMessageNeverRemind"));
								Object[] message={model.getString("disableActCntMessage"),checkboxRemindNever};
								if (!mbActCntMessageNotRemind)
									{
										ret=0;
										ret= JOptionPane.showOptionDialog(model.getView(),
																		(Object)message,
																		model.getString("disableActCntMessageTitle"),
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
								model.status2 = model.getString("statusRunning");
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
								String msg=MessageFormat.format(model.getString("errorListenerPort"),args);
								JOptionPane.showMessageDialog(model.getView(),
																							msg,
																							model.getString("errorListenerPortTitle"),
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
						model.status2 = model.getString("statusNotRunning");
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
				model.status2 = model.getString("statusNotRunning");
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
	public boolean getAnonMode() {
		return proxyAnon!=null;
	}


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
								JAPDebug.out(JAPDebug.INFO,JAPDebug.NET,"JAPProxyServer:Listener on port " + portNumber + " started.");
								try
									{
										m_socketHTTPListener.setSoTimeout(2000);
									}
								catch(Exception e1)
									{
										JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"Could not set accept time out: Exception: "+e1.getMessage());
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


	/** This (and only this!) is the final exit procedure of JAP!
	 *
	 */
	public static void goodBye() {
		//stopListener();
		model.save();
		System.exit(0);
	}

	public static void aboutJAP()
		{
			try
				{
					new JAPAbout(view);
				}
			catch(Throwable t)
				{
				}
		}

	/** Try to load all available MIX-Cascades form the InfoService...
	 */
	public void fetchAnonServers()
		{
			JAPDebug.out(JAPDebug.INFO,JAPDebug.MISC,"JAPModel:Trying to fetch Anon Servers from ...");
			try
				{
					getInfoService().fetchAnonServers();
				}
			catch (Exception e)
				{
					JAPDebug.out(JAPDebug.ERR,JAPDebug.NET,"JAPModel:fetchAnonServers: "+e);
					JOptionPane.showMessageDialog(view,
																				model.getString("errorConnectingInfoService"),
																				model.getString("errorConnectingInfoServiceTitle"),
																				JOptionPane.ERROR_MESSAGE);
				}
			notifyJAPObservers();
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
					JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"JAPModel:Version:"+aktVersion);
					if ( s.compareTo(aktVersion) > 0 )
						{
							// OK, new version available
							// ->Ask user if he/she wants to download new version
							//	Object[] options = { model.getString("newVersionNo"), model.getString("newVersionYes") };
							//	ImageIcon   icon = loadImageIcon(DOWNLOADFN,true);
							String answer;
							JAPLoading japLoading = new JAPLoading(this,view);
								answer = japLoading.message(model.getString("newVersionAvailableTitle"),
						   model.getString("newVersionAvailable"),
						   model.getString("newVersionNo"),
						   model.getString("newVersionYes"),
						   true,false);
					if (answer.equals(model.getString("newVersionYes"))) {
						// User has elected to download new version of JAP
						// ->Download, Alert, exit program
						// To do: show busy message
						try {
							vc.registerProgress(japLoading);
//						vc.getVersionFromNet("http://"+infoServiceHostName+":"+infoServicePortNumber+urlJAPNewVersionDownload, JAPLocalFilename);
							vc.getVersionFromNet("http://anon.inf.tu-dresden.de:80"+urlJAPNewVersionDownload, JAPLocalFilename);
							Thread t = new Thread(vc);
							t.start();
							answer = japLoading.message(model.getString("downloadingProgressTitle"),
						  model.getString("downloadingProgress"),
						   null,
						   null,
						   true,true);
							t.join();
							result = vc.getResult();
							if (result == 0) {
							//
								answer = japLoading.message(model.getString("newVersionAvailableTitle"),
							  model.getString("newVersionLoaded"),
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
																					model.getString("downloadFailed")+model.getString("infoURL"),
																					model.getString("downloadFailedTitle"),
																					JOptionPane.ERROR_MESSAGE);
						notifyJAPObservers();
						return -1;
					}
				} else {
					// User has elected not to download
					// ->Alert, we should'nt start the system due to possible compatibility problems
					answer = japLoading.message(model.getString("youShouldUpdateTitle"),
						   model.getString("youShouldUpdate")+model.getString("infoURL"),
						   null,
						   "OK",
						   true,false);
					notifyJAPObservers();
					return -1;
				}
			}
			//endif ( s.compareTo(aktVersion) > 0 )
			// --> no new version available, i.e. you are running the newest version of JAP
			return 0; // meaning: version check says that anonymity service can be started
		}
		catch (Exception e) {
			// Verson check failed
			// ->Alert, and reset anon mode to false
			JAPDebug.out(JAPDebug.ERR,JAPDebug.MISC,"JAPModel: "+e);
			JOptionPane.showMessageDialog(view,
																		model.getString("errorConnectingInfoService"),
																		model.getString("errorConnectingInfoServiceTitle"),
																		JOptionPane.ERROR_MESSAGE);
			notifyJAPObservers();
			return -1;
		}
		// this line should never be reached
	}

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
							listener.valuesChanged(this);
						}
				}
		//	JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"JAPModel:notifyJAPObservers()-ended");
		}

}

