import java.util.Enumeration;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Vector;
import com.sun.xml.tree.XmlDocument;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;
import org.w3c.dom.NodeList;
import org.w3c.dom.NamedNodeMap;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.Frame;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import javax.swing.ImageIcon;

/* This is the Model of All. It's a Singelton!*/
public final class JAPModel {

// 2000-08-01(HF): 
// JAPDebug now initialized in JAP in order to use
// the functions also in JAP.main()
    
	
	
	static final String aktVersion = "00.00.020"; // Version of JAP
	
	private int      portNumber        = 4001;
	private int      runningPortNumber = 0;      // the port where proxy listens
	private boolean  isRunningProxy    = false;  // true if a proxy is running
	public  String   proxyHostName     = "ikt.inf.tu-dresden.de";
	public  int      proxyPortNumber   = 80;
	private boolean  proxyMode         = false;  // indicates whether JAP connects via a proxy or directly
	private String   infoServiceHostName      = "anon.inf.tu-dresden.de";
	private int      infoServicePortNumber    = 6543;
	public  String   anonHostName      = "anon.inf.tu-dresden.de";
	public  int      anonPortNumber    = 6544;
	private boolean  anonMode          = false;  // indicates whether user wants to send data via MIXes or not
	public  boolean  autoConnect       = false;  // autoconnect after program start
	public  boolean  alreadyCheckedForNewVersion = false; // indicates if check for new version has already been done 
	public  boolean  canStartService   = false;  // indicates if Anon service can be started
	public  String   status1           = "?";
	public  String   status2           = " ";
	private int      nrOfChannels      = 0;
	private int      nrOfBytes         = 0;
	public int       nrOfActiveUsers   = -1;
	static final int MAXPROGRESSBARVALUE = 100; // for trafficSituation and currentRisk
	public int       trafficSituation  = -1;
	public int       currentRisk       = -1;
	static private JAPView view			   = null;
// 2000-08-01(HF): the following url is now defined in JAPMessages.properties:
// usage: model.getString("infoURL")
//static final String url_download_version       = "http://www.inf.tu-dresden.de/~hf2/anon/JAP/";
	
	static final String aktJAPVersionFN            = "/~sk13/anon/jap/aktVersion.txt"; // retrieved from Info Service
	static final String urlJAPNewVersionDownload   = "/~sk13/anon/jap/JAP.jar"; // also retrieved from Info Service
	static final String JAPLocalFilename           = "JAP.jar";

	private ResourceBundle msg;

	static final String TITLE = "JAVA ANON PROXY -- JAP";
	static final String AUTHOR = "(c) 2000 The JAP-Team";

	static final int    MAXHELPLANGUAGES = 6;
	static final String MESSAGESFN   = "JAPMessages";
	static final String XMLCONFFN    = "jap.conf";
	static final String SPLASHFN     = "images/splash.gif";
	static final String BUSYFN       = "images/busy.gif";
	static final String DOWNLOADFN   = "images/install.gif";
	static final String IICON16FN    = "images/icon16.gif";
	static final String JAPICONFN    = "images/japi.gif";
	static final String CONFIGICONFN = "images/icoc.gif";
	static final String METERICONFN  = "images/icom.gif";
	static final String[] METERFNARRAY = {
						"images/meterD.gif", // anonymity deactivated
						"images/meterN.gif", // no measure available
						"images/meter1.gif",
						"images/meter2.gif",
						"images/meter3.gif",
						"images/meter4.gif",
						"images/meter5.gif",
						"images/meter6.gif"
						};
	
	private Vector observerVector=null;
	public Vector anonServerDatabase=null;
	
	private JAPProxyServer proxy=null;;
	
	private static JAPModel model=null;
//	public JAPLoading japLoading;
	private static JAPFeedback feedback=null;
	
	public static JAPKeyPool keypool=null;
	
	private JAPModel ()
		{
			JAPDebug.out(JAPDebug.INFO,JAPDebug.MISC,"JAPModel:initializing...");
			// Load Texts for Messages and Windows
			try
				{ 
					msg = ResourceBundle.getBundle(MESSAGESFN, Locale.getDefault() ); 
				}	 
			catch(Exception e1)
				{
					try
						{
							msg=ResourceBundle.getBundle(MESSAGESFN);
						}
					catch(Exception e) 
						{
							JAPAWTMsgBox.MsgBox(new Frame(),
																	"File not found: "+MESSAGESFN+".properties\nYour package of JAP may be corrupted.\nTry again to download or install the package.",
																	"Error");
							System.exit(-1);
						}
				}
			JAPDebug.out(JAPDebug.INFO,JAPDebug.MISC,"JAPModel:properties loaded");
				
			// Create observer object 
			observerVector = new Vector();
			anonMode=false;
			JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"JAPModel:initialization finished!");
		}
	
	/** Creates the Model - as Singleton.
	 * @return The one and only JAPModel
	 */
	public static JAPModel createModel()
		{
			if(model!=null)
				JAPDebug.out(JAPDebug.ALERT,JAPDebug.MISC,"JAPModel is initialized twice - Bug in Programm!!");
			else
				model=new JAPModel();
			return model;
		}
	
	public static JAPModel getModel() {
			return model;
	}
	
	public void setView(JAPView v)
		{
			view=v;	
		}
	
	public JAPView getView()
		{
			return view;
		}
	
	/** Loads the Configuration. This is an XML-File with the following structure:
	 *	<JAP 
	 *		portNumber=""									// Listener-Portnumber
	 *		proxyMode="true"/"false"			// Using a HTTP-Proxy??
	 *		proxyHostName="..."						// the Name of the HTTP-Proxy
	 *		proxyPortNumber="..."					// port number of the HTTP-proxy
	 *		infoServiceHostName="..."			// hostname of the infoservice
	 *		infoServicePortnumber=".."		// the portnumber of the info service
	 *		anonHostName=".."							// the hostname of the anon-service
	 *		anonPortNumber=".."						// the portnumber of the anon-service
	 *		autoConnect="true"/"false"		// should we start the anon service immedialy after programm launch ?
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
	 *	</Debug>		
	 *	</JAP>
	 */
	public void load() {
		// Load default anon services
		anonServerDatabase = new Vector();
//		anonServerDatabase.addElement(new AnonServerDBEntry(anonHostName, anonPortNumber));
//		anonServerDatabase.addElement(new AnonServerDBEntry(proxyHostName, proxyPortNumber));
//		anonServerDatabase.addElement(new AnonServerDBEntry("anon.inf.tu-dresden.de", 6543));
//		anonServerDatabase.addElement(new AnonServerDBEntry("passat.mesh.de", 6543));
		// Load config from xml file
		JAPDebug.out(JAPDebug.INFO,JAPDebug.MISC,"JAPModel:try loading configuration from "+XMLCONFFN);
		try {
			FileInputStream f=new FileInputStream(XMLCONFFN);
			Document doc=DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(f);
			Element root=doc.getDocumentElement();
			NamedNodeMap n=root.getAttributes();
			// 
			portNumber=Integer.valueOf(n.getNamedItem("portNumber").getNodeValue()).intValue();
			proxyMode=((n.getNamedItem("proxyMode").getNodeValue()).equals("true")?true:false);
			proxyHostName=n.getNamedItem("proxyHostName").getNodeValue();
			proxyPortNumber=Integer.valueOf(n.getNamedItem("proxyPortNumber").getNodeValue()).intValue();
			infoServiceHostName=n.getNamedItem("infoServiceHostName").getNodeValue();
			infoServicePortNumber=Integer.valueOf(n.getNamedItem("infoServicePortNumber").getNodeValue()).intValue();
			anonHostName=n.getNamedItem("anonHostName").getNodeValue();
			anonPortNumber=Integer.valueOf(n.getNamedItem("anonPortNumber").getNodeValue()).intValue();
			autoConnect=((n.getNamedItem("autoConnect").getNodeValue()).equals("true")?true:false);
		
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
			FileOutputStream f=new FileOutputStream(XMLCONFFN);
			Document doc=DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
			Element e=doc.createElement("JAP");
			doc.appendChild(e);
			//
			e.setAttribute("portNumber",Integer.toString(portNumber));
			e.setAttribute("proxyMode",(proxyMode?"true":"false"));
			e.setAttribute("proxyHostName",proxyHostName);
			e.setAttribute("proxyPortNumber",Integer.toString(proxyPortNumber));
			e.setAttribute("infoServiceHostName",infoServiceHostName);
			e.setAttribute("infoServicePortNumber",Integer.toString(infoServicePortNumber));
			e.setAttribute("anonHostName",anonHostName);
			e.setAttribute("anonPortNumber",Integer.toString(anonPortNumber));
			e.setAttribute("autoConnect",(autoConnect?"true":"false"));
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
						
			((XmlDocument)doc).write(f);
		}
		catch(Exception e) {
			JAPDebug.out(JAPDebug.ERR,JAPDebug.MISC,"JAPModel:error saving configuration to "+XMLCONFFN);
		}
	}
	
	
	public void initialRun()
		{
			JAPDebug.out(JAPDebug.INFO,JAPDebug.MISC,"JAPModel:initial run of JAP...");
			// start keypool thread
			keypool=new JAPKeyPool(20,16);
			Thread t1 = new Thread (keypool);
			t1.setPriority(Thread.MIN_PRIORITY);
			t1.start();
		
			// start Proxy
			startProxy();
			
			// start anon service immediately if autoConnect is true
			setAnonMode(autoConnect);
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
		
	public synchronized static String getString(String key) {
		try {
			return model.msg.getString(key);
		}
		catch(Exception e) {
			return key;
		}
	}
		
	public synchronized void setNrOfChannels(int cannels) {
		nrOfChannels=cannels;
		notifyJAPObservers();
	}
	
	public int getNrOfChannels() {
		return nrOfChannels;
	}
	
	public synchronized void increasNrOfBytes(int bytes) {
		nrOfBytes+=bytes;
		notifyJAPObservers();
	}
	
	public int getNrOfBytes() {
		return nrOfBytes;
	}

	public boolean setInfoService(String host,int port)
		{
			if(!isPort(port))
				return false;
			if(host==null)
				return false;
			synchronized(this)
				{
					infoServiceHostName=host;
					infoServicePortNumber=port;
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
	
	public synchronized void setAnonMode(boolean anonModeSelected)
	{
		if ((anonMode == false) && (anonModeSelected == true)) {
			JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"JAPModel:setAnonMode("+anonModeSelected+")");
			if (alreadyCheckedForNewVersion == false) {
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
			if (canStartService) {
				// -> we can start anonymity
				anonMode = true;
				// starting MUX --> Success ???
				if(proxy.startMux())
					{
				// start feedback thread
						feedback=new JAPFeedback();
						Thread t2 = new Thread (feedback);
						t2.setPriority(Thread.MIN_PRIORITY);
						t2.start();
					}
				notifyJAPObservers();
			}
		} else if ((anonMode == true) && (anonModeSelected == false)) {
			JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"JAPModel:setAnonMode("+anonModeSelected+")");
			anonMode = false;
			proxy.stopMux();
			if(feedback==null)
				{
					feedback.stopRequests();
					feedback=null;
				}
			notifyJAPObservers();
		}
	}
	
	public boolean isAnonMode() {
		return anonMode;
	}
	
	public synchronized void setProxyMode(boolean b) {
		proxyMode=b;
		notifyJAPObservers();
	}
	
	public boolean isProxyMode() {
		return proxyMode;
	}

	private void startProxy() 
		{
			JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"JAPModel:startProxy");
			if (isRunningProxy == false)
				{
					runningPortNumber = portNumber;
					proxy = new JAPProxyServer(portNumber);
					if(proxy.create())
						{
							Thread proxyThread = new Thread (proxy);
							proxyThread.start();
							isRunningProxy = true;
						}
					else
						proxy=null;
				}
		}
	
	private void stopProxy()
		{
			JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"JAPModel:stopProxy");
			if (isRunningProxy)
				{
					proxy.stopService();
					proxy=null;
					isRunningProxy = false;
				}
		}
	
	
	/** This (and only this!) is the final exit procedure of JAP!
	 * 
	 */
	public void goodBye() {
		stopProxy();
		save();
		System.exit(0);
	}
	
	public void aboutJAP() {
		javax.swing.JOptionPane.showMessageDialog
			(view, 
			 model.TITLE + "\n" + 
			  model.getString("infoText") + "\n\n" + 
			  model.AUTHOR + "\n\n" +
			  model.getString("infoEMail") + "\n" + 
			  model.getString("infoURL") + "\n\n" + 
			  model.getString("version")+": "+model.aktVersion+"\n\n", 
			 model.getString("aboutBox"),
			 javax.swing.JOptionPane.INFORMATION_MESSAGE
			);
	}
	
	/** Try to load all available MIX-Cascades form the InfoService...
	 */
	public void fetchAnonServers() 
		{
			JAPDebug.out(JAPDebug.INFO,JAPDebug.MISC,"JAPModel:Trying to fetch Anon Servers from InfoService...");
			JAPFetchAnonServers f = new JAPFetchAnonServers();
			try
				{
					f.fetch();
				}
			catch (Exception e)
				{
					JAPDebug.out(JAPDebug.ERR,JAPDebug.NET,"JAPModel:fetchAnonServers: "+e);
					javax.swing.JOptionPane.showMessageDialog(view, model.getString("errorConnectingInfoService"), model.getString("errorConnectingInfoServiceTitle"), javax.swing.JOptionPane.ERROR_MESSAGE); 
				}
		}
	
	/** Performs the Versioncheck.
	 *  @return -1, if version check says that anonymity mode should not be enabled.
	 *          Reasons can be: new version found, version check failed 
	 */
	public int versionCheck() {
		JAPDebug.out(JAPDebug.INFO,JAPDebug.MISC,"JAPModel:Checking for new version of JAP...");
		try {
			int result = 0;
			Versionchecker vc = new Versionchecker();
//			String s = vc.getNewVersionnumberFromNet("http://"+infoServiceHostName+":"+infoServicePortNumber+aktJAPVersionFN);
// temporary changed due to stability.... (sk13)
			String s = vc.getNewVersionnumberFromNet("http://anon.inf.tu-dresden.de:80"+aktJAPVersionFN);
			JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"JAPModel:Version:"+s);
			if ( s.compareTo(aktVersion) > 0 ) {
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
						javax.swing.JOptionPane.showMessageDialog(view, model.getString("downloadFailed")+model.getString("infoURL"), model.getString("downloadFailedTitle"), javax.swing.JOptionPane.ERROR_MESSAGE); 
						anonMode = false;
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
					anonMode = false;
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
			javax.swing.JOptionPane.showMessageDialog(view, model.getString("errorConnectingInfoService"), model.getString("errorConnectingInfoServiceTitle"), javax.swing.JOptionPane.ERROR_MESSAGE); 
			anonMode = false;
			notifyJAPObservers();
			return -1;
		}
		// this line should never be reached
	}
		
	public void addJAPObserver(JAPObserver o)
		{
			observerVector.addElement(o);
		}
	
	public synchronized void notifyJAPObservers()
		{
			//JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"JAPModel:notifyJAPObservers()");
			Enumeration enum = observerVector.elements();
			while (enum.hasMoreElements())
				{
					JAPObserver listener = (JAPObserver)enum.nextElement();
					listener.valuesChanged(this);
				}
		}
	
	/** Loads an Image from a File or a Resource.
	 *	@param strImage the Resource or filename of the Image
	 *	@param sync true if the loading is synchron, false if it should be asynchron
	 */
	ImageIcon loadImageIcon(String strImage, boolean sync) 
		{
			JAPDebug.out(JAPDebug.DEBUG,JAPDebug.GUI,"JAPModel:Image "+strImage+" loading...");
			boolean finished = false;
			ImageIcon img = null;
			// this is necessary to make shure that the images are loaded when contained in a JAP.jar
			try 
				{
					img = new ImageIcon(getClass().getResource(strImage));
				}
			catch (Exception e)
				{
					img = null;
				}
			// ...otherwise
			if (img == null)
				{
					img = new ImageIcon(strImage);
				}
			if ((sync == false) || (img == null)) {
				finished = true;
			}
			while(finished!=true)
				{
					int status = img.getImageLoadStatus();
					if ( (status & MediaTracker.ABORTED) != 0 ) {
						JAPDebug.out(JAPDebug.ERR,JAPDebug.GUI,"JAPModel:Loading of image "+strImage+" aborted!");
						finished = true;
						}
					if ( (status & MediaTracker.ERRORED) != 0 ) {
						JAPDebug.out(JAPDebug.ERR,JAPDebug.GUI,"JAPModel:Error loading image "+strImage+"!");
						finished = true;
					}
					if ( (status & MediaTracker.COMPLETE) != 0) {
						finished = true;
					}
				}
		return img;
	}
	
	public static void centerFrame(Window f) {
		Dimension screenSize = f.getToolkit().getScreenSize();
		try //JAVA 1.1
			{
				Dimension ownSize = f.getSize();
				f.setLocation((screenSize.width-ownSize.width )/2,(screenSize.height-ownSize.height)/2);
			}
		catch(Error e) //JAVA 1.0.2
			{
				Dimension ownSize = f.size();
				f.locate((screenSize.width-ownSize.width )/2,(screenSize.height-ownSize.height)/2);
			}
	}
	
	public static boolean isPort(int port)
		{
			if((port<1)||(port>65536))
				return false;
			return true;
		}

}

