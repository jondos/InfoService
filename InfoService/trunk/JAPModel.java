import java.util.*;
import com.sun.xml.tree.XmlDocument;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import javax.swing.ImageIcon;

public final class JAPModel {

// 2000-08-01(HF): 
// JAPDebug now initialized in JAP in order to use
// the functions also in JAP.main()
    
	static final String aktVersion = "00.00.012"; // Version of JAP
	
	public  int      portNumber        = 4001;
	private int      runningPortNumber = 0;      // the port where proxy listens
	private boolean  isRunningProxy    = false;  // true if a proxy is running
	public  String   proxyHostName     = "ikt.inf.tu-dresden.de";
	public  int      proxyPortNumber   = 80;
	private boolean  proxyMode         = false;  // indicates whether JAP connects via a proxy or directly
	public  String   infoServiceHostName      = "anon.inf.tu-dresden.de";
	public  int      infoServicePortNumber    = 6543;
	public  String   anonHostName      = "anon.inf.tu-dresden.de";
	public  int      anonPortNumber    = 6544;
	private boolean  anonMode          = false;  // indicates whether user wants to send data via MIXes or not
	public  boolean  autoConnect       = false;  // autoconnect after program start
	public  boolean  alreadyCheckedForNewVersion = false; // indicates if check for new version has already been done 
	public  boolean  canStartService   = false;  // indicates if Anon service can be started
	public  String   status1           = "?";
	public  String   status2           = " ";

	private int nrOfChannels = 0;
	private int nrOfBytes    = 0;
	//static final int MAXCHANNELVALUE = 5; // maximal number of anonymous channels
	//static final int MAXBYTESVALUE = 100000; // maximal bytes of all anonymous channels

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
						"images/meterN.gif", // no measure available
						"images/meterD.gif", // anonymity deactivated
						"images/meter1.gif",
						"images/meter2.gif",
						"images/meter3.gif",
						"images/meter4.gif",
						"images/meter5.gif",
						"images/meter6.gif"
						};
	static final int MAXPROGRESSBARVALUE = 100;
	static final boolean NOMEASURE = false;
	public int nrOfActiveUsers = 1;
	public int trafficSituation = 0;
	public int currentRisk = 100;
	
	private Vector observerVector;
	public Vector anonServerDatabase;
	
	private JAPProxyServer p;
	
	private static JAPModel model=null;
	
	public static JAPKeyPool keypool;
	
	public JAPModel () {
		JAPDebug.out(JAPDebug.INFO,JAPDebug.MISC,"JAPModel:initializing...");
		// Load Texts for Messages and Windows
		try { 
			msg = ResourceBundle.getBundle(MESSAGESFN, Locale.getDefault() ); 
		} 
		catch(Exception e1) {
			try {
				msg=ResourceBundle.getBundle(MESSAGESFN,Locale.ENGLISH);
			}
			catch(Exception e) {
				System.out.println("File not found: "+MESSAGESFN+".properties");
				System.out.println("Your package of JAP may be corrupted.");
				System.out.println("Try again to download or install the package.");
				System.exit(-1);
			}
		}
		JAPDebug.out(JAPDebug.INFO,JAPDebug.MISC,"JAPModel:properties loaded");
				
		// Create observer object 
		observerVector = new Vector();

		model=this;
		
		JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"JAPModel:initialization finished!");
	}
	
	public static JAPModel getModel() {
			return model;
	}
	
	public void load() {
		// Load default anon services
		anonServerDatabase = new Vector();
		anonServerDatabase.addElement(new AnonServerDBEntry(anonHostName, anonPortNumber));
		anonServerDatabase.addElement(new AnonServerDBEntry(proxyHostName, proxyPortNumber));
		anonServerDatabase.addElement(new AnonServerDBEntry("anon.inf.tu-dresden.de", 6543));
		// Load config from xml file
		JAPDebug.out(JAPDebug.INFO,JAPDebug.MISC,"JAPModel:try loading configuration from "+XMLCONFFN);
		try {
			FileInputStream f=new FileInputStream(XMLCONFFN);
			Document doc=DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(f);
			NamedNodeMap n=doc.getFirstChild().getAttributes();
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
		}
		catch(Exception e) {
		}
		// fire event
		notifyJAPObservers();
	}

	public void save() {
		// Save config to xml file
		JAPDebug.out(JAPDebug.INFO,JAPDebug.MISC,"JAPModel:try saving configuration to "+XMLCONFFN);
		try {
			FileOutputStream f=new FileOutputStream(XMLCONFFN);
			Document doc=DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
			Element e=doc.createElement("JAP");
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
			doc.appendChild(e);
			((XmlDocument)doc).write(f);
		}
		catch(Exception e) {
			JAPDebug.out(JAPDebug.ERR,JAPDebug.MISC,"JAPModel:error saving configuration to "+XMLCONFFN);
		}
	}
	
	
	public void initialRun() {
		// start Proxy
		JAPDebug.out(JAPDebug.INFO,JAPDebug.MISC,"JAPModel:starting listener");
		startProxy();
		// start anon service immediately if autoConnect is true
		setAnonMode(autoConnect);
	}
	
    public int getCurrentProtectionLevel() {
		// Hier eine moeglichst komplizierte Formel einfuegen,
		// nach der die Anzeige fuer das "Anon Meter" berechnet wird.
//		float f = (trafficSituation/MAXPROGRESSBARVALUE)*(METERFNARRAY.length-1);
		float f;
		f = trafficSituation / (float)MAXPROGRESSBARVALUE;
		f = f * (METERFNARRAY.length-3) + 2;
		JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"JAPModel:getCurrentProtectionLevel(): f="+f);
		return (int)f;
	}
		
	/*
	public void setPortNumber (int p) {
		this.portNumber = p;
	}
	public int getPortNumber() {
		return portNumber;
	}
	*/
	
		
	public synchronized static String getString(String key)
		{
			try
				{
					return model.msg.getString(key);
				}
			catch(Exception e)
				{
					return key;
				}
		}
		
	public synchronized void setNrOfChannels(int cannels)
		{
			nrOfChannels=cannels;
			notifyJAPObservers();
		}
	
	public int getNrOfChannels()
		{
			return nrOfChannels;
		}
	
	public synchronized void increasNrOfBytes(int bytes)
		{
			nrOfBytes+=bytes;
			notifyJAPObservers();
		}
	
	public int getNrOfBytes()
		{
			return nrOfBytes;
		}

	public void setAnonMode(boolean anonModeSelected) {
		if ((anonMode == false) && (anonModeSelected == true)) {
			JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"JAPModel:setAnonMode("+anonModeSelected+")");
			if (alreadyCheckedForNewVersion == false) {
			// Check for a new Version of JAP if not already done
				alreadyCheckedForNewVersion = true;
				int ok = this.versionCheck();
				if (ok == -1) {
					// -> at the moment nothing to do
					canStartService = false;
				} else {
					// -> we can start anonymity
					canStartService = true;
				}
			}
			if (canStartService) {
				// -> we can start anonymity
				anonMode = true;
				try {
					p.startMux();
				}
				catch (Exception e) {
						javax.swing.JOptionPane.showMessageDialog
							(
							 null, 
							 model.getString("errorConnectingFirstMix"),
							 model.getString("errorConnectingFirstMixTitle"),
							 javax.swing.JOptionPane.ERROR_MESSAGE
							);
				}
				notifyJAPObservers();
			}
		} else if ((anonMode == true) && (anonModeSelected == false)) {
			JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"JAPModel:setAnonMode("+anonModeSelected+")");
			anonMode = false;
			p.stopMux();
			notifyJAPObservers();
		}
	}
	
	public boolean isAnonMode() {
		return anonMode;
	}
	
	public void setProxyMode(boolean b) {
		proxyMode=b;
		notifyJAPObservers();
	}
	
	public boolean isProxyMode() {
		return proxyMode;
	}

	private void startProxy() {
		JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"JAPModel:startProxy");
		if (isRunningProxy == false) {
			isRunningProxy = true;
			runningPortNumber = portNumber;
			p = new JAPProxyServer(portNumber);
			Thread proxyThread = new Thread (p);
			proxyThread.start();
		}
	}
	
	private void stopProxy() {
		JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"JAPModel:stopProxy");
		if (isRunningProxy) {
			p.stopService();
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
			(null, 
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
	
	/** Performs the Versioncheck.
	 *  @return -1, if version check says that anonymity mode should not be enabled.
	 *          Reasons can be: new version found, version check failed 
	 */
	public int versionCheck() {
		JAPDebug.out(JAPDebug.INFO,JAPDebug.MISC,"JAPModel:Checking for new version of JAP...");
		try {
			String s = Versionchecker.getNewVersionnumberFromNet("http://"+infoServiceHostName+":"+infoServicePortNumber+aktJAPVersionFN);
			JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"JAPModel:Version:"+s);
			if ( s.compareTo(aktVersion) > 0 ) {
				// OK, new version available
				// ->Ask user if he/she wants to download new version
				Object[] options = { model.getString("newVersionNo"), model.getString("newVersionYes") };
				ImageIcon   icon = loadImageIcon(DOWNLOADFN,true);
				int opt=javax.swing.JOptionPane.showOptionDialog
					(null,
					 model.getString("newVersionAvailable"),
					 model.getString("newVersionAvailableTitle"), 
					 javax.swing.JOptionPane.DEFAULT_OPTION, 
					 javax.swing.JOptionPane.PLAIN_MESSAGE,
					 icon, 
					 options, 
					 options[1]
					);
				JAPDebug.out(JAPDebug.DEBUG,JAPDebug.GUI,"JAPModel:opt="+opt);
				if (opt == 1) {
					// User has elected to download new version of JAP
					// ->Download, Alert, exit program
					// To do: show busy message
					try {
						Versionchecker.getVersionFromNet("http://"+infoServiceHostName+":"+infoServicePortNumber+urlJAPNewVersionDownload, JAPLocalFilename);
						// 
						javax.swing.JOptionPane.showMessageDialog
							(
							 null, 
							 model.getString("newVersionLoaded"),
							 model.getString("newVersionAvailableTitle"),
							 javax.swing.JOptionPane.PLAIN_MESSAGE,
							 icon
							);
						goodBye();
						// next line should never be reached!
						JAPDebug.out(JAPDebug.EMERG,JAPDebug.MISC,"JAPModel:this line should never be reached!");
					}
					catch (Exception e) {
						// Download failed
						// Alert, and reset anon mode to false
						JAPDebug.out(JAPDebug.ERR,JAPDebug.MISC,"JAPModel:checkForNewJAPVersion(): "+e);
						javax.swing.JOptionPane.showMessageDialog(null, model.getString("downloadFailed")+model.getString("infoURL"), model.getString("downloadFailedTitle"), javax.swing.JOptionPane.ERROR_MESSAGE); 
						anonMode = false;
						notifyJAPObservers();
						return -1;
					}
					
				} else {
					// User has elected not to download
					// ->Alert, we should'nt start the system due to possible compatibility problems
					javax.swing.JOptionPane.showMessageDialog
						(
						 null, 
						 model.getString("youShouldUpdate")+model.getString("infoURL"),
						 model.getString("youShouldUpdateTitle"),
						 javax.swing.JOptionPane.PLAIN_MESSAGE,
						 icon
						);
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
			javax.swing.JOptionPane.showMessageDialog(null, model.getString("versionCheckFailed"), model.getString("versionCheckFailedTitle"), javax.swing.JOptionPane.ERROR_MESSAGE); 
			anonMode = false;
			notifyJAPObservers();
			return -1;
		}
		// this line should never be reached
	}
		
	public void addJAPObserver(Object o) {
		observerVector.addElement(o);
	}
	
	public synchronized void notifyJAPObservers() {
		JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"JAPModel:notifyJAPObservers()");
		Enumeration enum = observerVector.elements();
		while (enum.hasMoreElements()) {
			JAPObserver listener = (JAPObserver)enum.nextElement();
			listener.valuesChanged(this);
		}
	}
	
	ImageIcon loadImageIcon(String strImage, boolean sync) {
		JAPDebug.out(JAPDebug.DEBUG,JAPDebug.GUI,"JAPModel:Image "+strImage+" loading...");
		boolean finished = false;
		ImageIcon img = null;
		// this is necessary to make shure that the images are loaded when contained in a JAP.jar
		try {
			img = new ImageIcon(getClass().getResource(strImage));
		}
		catch (Exception e) {
			img = null;
		}
		// ...otherwise
		if (img == null) {
			img = new ImageIcon(strImage);
		}
		if ((sync == false) || (img == null)) {
			finished = true;
		}
		while(finished!=true) {
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
	
	public void centerFrame(Window f) {
		Dimension screenSize = f.getToolkit().getScreenSize();
		Dimension ownSize = f.getSize();
		f.setLocation((screenSize.width-ownSize.width )/2,(screenSize.height-ownSize.height)/2);
	}

	
	
/*	
	
	// Macintosh stuff
	protected void registerMRJHandlers() {
		//Register MRJ handlers for open, about and quit.
		MRJI IMRJI = new MRJI();
		com.apple.mrj.MRJApplicationUtils.registerQuitHandler(IMRJI);
		com.apple.mrj.MRJApplicationUtils.registerAboutHandler(IMRJI);
	}

	// Macintosh stuff
	//Inner class defining the MRJ Interface
	//Insert "SlideShow MRJI"
	class MRJI implements com.apple.mrj.MRJQuitHandler, com.apple.mrj.MRJAboutHandler
	{
		public void handleQuit() {
			JAPDebug.out(JAPDebug.DEBUG,JAPDebug.GUI,"Macintosh MRJ event: Quit");
			goodBye();
		}
		public void handleAbout() {
			JAPDebug.out(JAPDebug.DEBUG,JAPDebug.GUI,"Macintosh MRJ event: About");
			aboutJAP();
		}
	}
	*/
	
	
	
}

