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
import com.sun.xml.tree.XmlDocument;
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
import java.net.ServerSocket;
import java.net.InetAddress;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JDialog;
import javax.swing.JLabel;
import anon.JAPAnonService;
import anon.JAPAnonServiceListener;
/* This is the Model of All. It's a Singelton!*/
public final class JAPModel implements JAPAnonServiceListener{

	public static final String aktVersion = "00.01.003"; // Version of JAP

	private int      portNumber            = 4001;
	private boolean  mblistenerIsLocal     = true;  // indicates whether the Listener serves for localhost only or not
	private boolean  mbSocksListener       = false;  // indicates whether JAP should support Socks
	private int			 portSocksListener		 = 1080;
	
	//private int      runningPortNumber = 0;      // the port where proxy listens
	private boolean  isRunningListener     = false;  // true if a listener is running
	private  String  proxyHostName         = "";
	private  int     proxyPortNumber       = -1;
	private boolean  mbUseProxy            = false;  // indicates whether JAP connects via a proxy or directly
	private String   infoServiceHostName   = "infoservice.inf.tu-dresden.de";
	private int      infoServicePortNumber = 6543;
	public  String   anonserviceName       = "Default Mix Cascade";
	public  String   anonHostName          = "mix.inf.tu-dresden.de";
	public  int      anonPortNumber        = 6544;
	public  int      anonSSLPortNumber     = 443;
	public  boolean  autoConnect                 = false; // autoconnect after program start
	private boolean  mbMinimizeOnStartup         = false; // true if programm should be started minimized...
	public  boolean  canStartService             = false; // indicates if Anon service can be started
	public  boolean  alreadyCheckedForNewVersion = false; // indicates if check for new version has already been done
	private boolean  mbActCntMessageNotRemind    = false; // indicates if Warning message in setAnonMode has been deactivated for the session
	private boolean  mbActCntMessageNeverRemind  = false; // indicates if Warning message in setAnonMode has been deactivated forever
	public  String   status1           = "?";
	public  String   status2           = " ";

	private int      nrOfChannels      = 0;
	private int      nrOfBytes         = 0;

	public int       nrOfActiveUsers   = -1;
	public int       trafficSituation  = -1;
	public int       currentRisk       = -1;
	public int       mixedPackets      = -1;

	static final int MAXPROGRESSBARVALUE = 100; // for trafficSituation and currentRisk
	static private   JAPView           view          = null;
	static private   JAPViewIconified  iconifiedView = null;
	static private   JAPInfoService    mInfoService  = null;
// 2000-08-01(HF): the following url is now defined in JAPMessages.properties:
// usage: model.getString("infoURL")
//static final String url_download_version       = "http://www.inf.tu-dresden.de/~hf2/anon/JAP/";

//	static final String aktJAPVersionFN            = "/~sk13/anon/jap/aktVersion.txt"; // retrieved from Info Service
	static final String aktJAPVersionFN            = "/aktVersion"; // retrieved from Info Service
	static final String urlJAPNewVersionDownload   = "/~sk13/anon/jap/JAP.jar"; // also retrieved from Info Service
	static final String JAPLocalFilename           = "JAP.jar";

	private ResourceBundle msg;

	static final String TITLE = "JAVA ANON PROXY -- JAP";
	static final String AUTHOR = "(c) 2000 The JAP-Team";

	static final int    MAXHELPLANGUAGES = 6;
	static final String XMLCONFFN    = "jap.conf";
	public static final String BUSYFN       = "images/busy.gif";
	static final String DOWNLOADFN   = "images/install.gif";
	static final String IICON16FN    = "images/icon16.gif";
	static final String ICONFN       = "images/icon.gif";
	static final String JAPTXTFN     = "images/japtxt.gif";
	static final String JAPEYEFN     = "images/japeye.gif";
	static final String JAPICONFN    = "images/japi.gif";
	static final String CONFIGICONFN = "images/icoc.gif";
	static final String ICONIFYICONFN= "images/iconify.gif";
	static final String ENLARGEYICONFN= "images/enlarge.gif";
	static final String METERICONFN  = "images/icom.gif";
	static final String[] METERFNARRAY = {
						"images/meterD.gif", // anonymity deactivated
						"images/meterNnew.gif", // no measure available
						"images/meter1.gif",
						"images/meter2.gif",
						"images/meter3.gif",
						"images/meter4.gif",
						"images/meter5.gif",
						"images/meter6.gif"
						};

	private Vector observerVector=null;
	public Vector anonServerDatabase=null;

	private ServerSocket m_socketHTTPListener;
	private JAPDirectProxy proxyDirect=null;
	private JAPAnonService proxyAnon=null;

	private JAPAnonService proxyAnonSocks=null;


	private static JAPModel model=null;
//	public JAPLoading japLoading;
	private static JAPFeedback feedback=null;


	private JAPModel ()
		{
			//JAPDebug.out(JAPDebug.INFO,JAPDebug.MISC,"JAPModel:initializing...");
			//JAPDebug.out(JAPDebug.INFO,JAPDebug.MISC,"JAPModel:properties loaded");

			// Create observer object
			observerVector = new Vector();
			proxyDirect=null;
			proxyAnon=null;
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

	public JAPView getView()
		{
			return view;
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
	 *		infoServiceHostName="..."			// hostname of the infoservice
	 *		infoServicePortnumber=".."		// the portnumber of the info service
	 *		anonHostName=".."							// the hostname of the anon-service
	 *		anonPortNumber=".."						// the portnumber of the anon-service
	 *		autoConnect="true"/"false"		// should we start the anon service immedialy after programm launch ?
	 *		minimizedStartup="true"/"false" // should we start minimized ???
	 *		neverRemindActiveContent="true"/"false" // should we remind the user about active content ?
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
			portNumber=JAPUtil.parseNodeInt(n.getNamedItem("portNumber"),portNumber);
			portSocksListener=JAPUtil.parseNodeInt(n.getNamedItem("portNumberSocks"),portSocksListener);
			setUseSocksPort(JAPUtil.parseNodeBoolean(n.getNamedItem("supportSocks"),false));
			setListenerIsLocal(JAPUtil.parseNodeBoolean(n.getNamedItem("listenerIsLocal"),true));
			setUseProxy(JAPUtil.parseNodeBoolean(n.getNamedItem("proxyMode"),false));
			mbActCntMessageNeverRemind=JAPUtil.parseNodeBoolean(n.getNamedItem("neverRemindActiveContent"),false);
			if(mbActCntMessageNeverRemind)
				mbActCntMessageNotRemind=true;
			String host;
			int port;
			host=JAPUtil.parseNodeString(n.getNamedItem("infoServiceHostName"),infoServiceHostName);
			port=JAPUtil.parseNodeInt(n.getNamedItem("infoServicePortNumber"),infoServicePortNumber);
			setInfoService(host,port);

			host=JAPUtil.parseNodeString(n.getNamedItem("proxyHostName"),proxyHostName);
			port=JAPUtil.parseNodeInt(n.getNamedItem("proxyPortNumber"),proxyPortNumber);
			if(host.equalsIgnoreCase("ikt.inf.tu-dresden.de"))
				host="";
			setProxy(host,port);

			anonserviceName=JAPUtil.parseNodeString(n.getNamedItem("anonserviceName"),anonserviceName);
			anonHostName=JAPUtil.parseNodeString(n.getNamedItem("anonHostName"),anonHostName);
			anonPortNumber=JAPUtil.parseNodeInt(n.getNamedItem("anonPortNumber"),anonPortNumber);
			anonSSLPortNumber=JAPUtil.parseNodeInt(n.getNamedItem("anonSSLPortNumber"),anonSSLPortNumber);
			autoConnect=JAPUtil.parseNodeBoolean(n.getNamedItem("autoConnect"),false);
			mbMinimizeOnStartup=JAPUtil.parseNodeBoolean(n.getNamedItem("minimizedStartup"),false);

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
		// Create the Window for Update
	}

	public void save() {
		// Save config to xml file
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
			Document doc=DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
			Element e=doc.createElement("JAP");
			doc.appendChild(e);
			//
			e.setAttribute("portNumber",Integer.toString(portNumber));
			e.setAttribute("portNumberSocks",Integer.toString(portSocksListener));
			e.setAttribute("supportSocks",(getUseSocksPort()?"true":"false"));
			e.setAttribute("listenerIsLocal",(getListenerIsLocal()?"true":"false"));
			e.setAttribute("proxyMode",(mbUseProxy?"true":"false"));
			e.setAttribute("proxyHostName",proxyHostName);
			e.setAttribute("proxyPortNumber",Integer.toString(proxyPortNumber));
			e.setAttribute("infoServiceHostName",infoServiceHostName);
			e.setAttribute("infoServicePortNumber",Integer.toString(infoServicePortNumber));
			e.setAttribute("anonserviceName",anonserviceName);
			e.setAttribute("anonHostName",anonHostName);
			e.setAttribute("anonPortNumber",Integer.toString(anonPortNumber));
			e.setAttribute("anonSSLPortNumber",Integer.toString(anonSSLPortNumber));
			e.setAttribute("autoConnect",(autoConnect?"true":"false"));
			e.setAttribute("minimizedStartup",(mbMinimizeOnStartup?"true":"false"));
			e.setAttribute("neverRemindActiveContent",(mbActCntMessageNeverRemind?"true":"false"));
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
			if(JAPDebug.isShowConsole())
				{
					tmp=doc.createElement("Output");
					txt=doc.createTextNode("Console");
					tmp.appendChild(txt);
					elemDebug.appendChild(tmp);
				}
			((XmlDocument)doc).write(f);
		}
		catch(Exception ex) {
			JAPDebug.out(JAPDebug.EXCEPTION,JAPDebug.MISC,"JAPModel:save() Exception: "+ex);
			JAPDebug.out(JAPDebug.ERR,JAPDebug.MISC,"JAPModel:error saving configuration to "+XMLCONFFN);
		}
	}


	public void initialRun()
		{
			JAPDebug.out(JAPDebug.INFO,JAPDebug.MISC,"JAPModel:initial run of JAP...");
			
			//pre initalize some long time initalisations...
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
								JAPDebug.out(JAPDebug.EMERG,JAPDebug.NET,"Listener could not be started!");
								model.getView().disableSetAnonMode();
				}
			else
				{
					model.status1 = model.getString("statusRunning");
					setAnonMode(autoConnect);
				}
			
		}

    public int getCurrentProtectionLevel() {
		// Hier eine moeglichst komplizierte Formel einfuegen,
		// nach der die Anzeige fuer das "Anon Meter" berechnet wird.
		if ((nrOfActiveUsers  == -1) ||
			(trafficSituation == -1) ||
			(currentRisk      == -1)) {
				return 1;
		} else {
			try {
				float f;
				f = trafficSituation / (float)MAXPROGRESSBARVALUE;
				f = f * (METERFNARRAY.length-3) + 2;
				//JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"JAPModel:getCurrentProtectionLevel(): f="+f);
				return (int)f;
			}
			catch (Exception e) {
				JAPDebug.out(JAPDebug.EXCEPTION,JAPDebug.MISC,"JAPModel:getCurrentProtectionLevel(): "+e);
				return 1;
			}
		}
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
								mInfoService.setProxy(proxyHostName,proxyPortNumber);
							else
								mInfoService.setProxy(null,0);
						}
				}
			notifyJAPObservers();
		}

	public boolean getUseProxy()
		{
			synchronized(this)
				{
					return mbUseProxy;
				}
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
						mInfoService.setProxy(host,port);
					notifyJAPObservers();
					return true;
				}
		}

	public String getProxyHost()
		{
			synchronized(this)
				{
					return proxyHostName;
				}
		}

	public int getProxyPort()
		{
			synchronized(this)
				{
					return proxyPortNumber;
				}
		}

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

	
private final class SetAnonModeAsync implements Runnable
{
	boolean anonModeSelected=false;
	public SetAnonModeAsync(boolean b)
		{
		anonModeSelected=b;
		}
		
	public void run() //setAnonMode--> async!!
	{
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
				model.mixedPackets = -1;
				model.nrOfActiveUsers = -1;
				model.trafficSituation = -1;
				model.currentRisk = -1;
				notifyJAPObservers();
				JAPSetAnonModeSplash.abort();
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
	public boolean isAnonMode() {
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

	public static void aboutJAP() {
	/*	JOptionPane.showMessageDialog
			(view,
			 model.TITLE + "\n" +
			  model.getString("infoText") + "\n\n" +
			  model.AUTHOR + "\n\n" +
			  model.getString("infoEMail") + "\n" +
			  model.getString("infoURL") + "\n\n" +
			  model.getString("version")+": "+model.aktVersion+"\n\n",
				model.getString("aboutBox"),
				JOptionPane.INFORMATION_MESSAGE
			);*/
		new JAPAbout(view);
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
					String s = vc.getNewVersionnumberFromNet("http://"+infoServiceHostName+":"+infoServicePortNumber+aktJAPVersionFN);
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

