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
public final class JAPModel implements JAPObserver {

	public boolean		debug =false;
	
	public int			portNumber = 4001;
	private int			runningPortNumber = 0; // the port where proxy listens
	private boolean		isRunningProxy = false; // true if a proxy is running
	public String 		proxyHostName = "ikt.inf.tu-dresden.de";
	public int 			proxyPortNumber = 80;
	public boolean		proxyMode = false;
	public String		anonHostName ="sole.icsi.berkeley.edu";
	public int			anonPortNumber = 6543;
	private boolean		anonMode = false;
	public String 		status1 = "???";
	public String 		status2 = "???";

	private int nrOfChannels = 0;
	private int nrOfBytes = 0;
	//static final int MAXCHANNELVALUE = 5; // maximal number of anonymous channels
	//static final int MAXBYTESVALUE = 100000; // maximal bytes of all anonymous channels

	static final String url_download_version = "http://anon.inf.tu-dresden.de/~sk13/anon/jap/download.html";
	static final String url_info_version     = "http://anon.inf.tu-dresden.de/~sk13/anon/jap/aktVersion.txt";
	static final String url_jap_newversion   = "http://anon.inf.tu-dresden.de/~sk13/anon/jap/JAP.jar";

	private ResourceBundle msg;

	static final String TITLE = "JAVA ANON PROXY -- JAP";
    static final String AUTHOR = "The JAP-Team\n<jap@inf.tu-dresden.de>\n \n(c) 2000 \n";

	static final int    MAXHELPLANGUAGES = 6;
	static final String MESSAGESFN   = "JAPMessages";
	static final String XMLCONFFN    = "jap.conf";
	static final String SPLASHFN     = "images/splash.gif";
	static final String BUSYFN       = "images/busy.gif";
	static final String IICON16FN    = "images/icon16.gif";
	static final String JAPICONFN    = "images/japi.gif";
	static final String CONFIGICONFN = "images/icoc.gif";
	static final String METERICONFN  = "images/icom.gif";
	static final String[] METERFNARRAY = {
						"images/meterN.gif",
						"images/meterD.gif",
						"images/meter1.gif",
						"images/meter2.gif",
						"images/meter3.gif",
						"images/meter4.gif",
						"images/meter5.gif",
						"images/meter6.gif"
						};
	static final int MAXPROGRESSBARVALUE = 100;
	static final boolean NOMEASURE = false;
	public int 			nrOfActiveUsers = 1;
	public int 			trafficSituation = 0;
	public int 			currentRisk = 100;
	
	private Vector observerVector;
	public Vector anonServerDatabase;
	
	private JAPProxyServer p;
	
	private static JAPModel model=null;
	public JAPModel () {
		// Load Texts for Messages and Windows
		try 
			{ 
				msg = ResourceBundle.getBundle(MESSAGESFN, Locale.getDefault() ); 
			}
		catch(Exception e1)
			{
				try 
					{
						msg=ResourceBundle.getBundle(MESSAGESFN,Locale.ENGLISH);
					}
				catch(Exception e) 
					{
						System.out.println("File not found: "+MESSAGESFN+".properties");
						System.out.println("Your package of JAP may be corrupted.");
						System.out.println("Try download of the package again. URL:");
						System.out.println(url_download_version);
						System.exit(-1);
					}
			}

		// Create observer object 
		observerVector = new Vector();

		// Load default anon services
		anonServerDatabase = new Vector();
		anonServerDatabase.addElement(new AnonServerDBEntry(anonHostName, anonPortNumber));
		anonServerDatabase.addElement(new AnonServerDBEntry(proxyHostName, proxyPortNumber));
		anonServerDatabase.addElement(new AnonServerDBEntry("anon.inf.tu-dresden.de", 6543));
		model=this;
	}
	
	public static JAPModel getModel()
		{
			return model;
		}
	
	public boolean load()
		{
			try
				{
					FileInputStream f=new FileInputStream(XMLCONFFN);
					Document doc=DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(f);
					NamedNodeMap n=doc.getFirstChild().getAttributes();
					anonHostName=n.getNamedItem("host").getNodeValue();
					anonPortNumber=Integer.valueOf(n.getNamedItem("port").getNodeValue()).intValue();
					return true;	
				}
			catch(Exception e)
				{
					return false;
				}
		}

	public boolean save()
		{
			try
				{
					FileOutputStream f=new FileOutputStream(XMLCONFFN);
					Document doc=DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
					Element e=doc.createElement("anonhost");
					e.setAttribute("host",anonHostName);
					e.setAttribute("port",Integer.toString(anonPortNumber));
					doc.appendChild(e);
					((XmlDocument)doc).write(f);
					return true;
				}
			catch(Exception e)
				{
					return false;
				}
		}
	
    public int getCurrentProtectionLevel() {
		// Hier eine moeglichst komplizierte Formel einfuegen,
		// nach der die Anzeige fuer das "Anon Meter" berechnet wird.
//		float f = (trafficSituation/MAXPROGRESSBARVALUE)*(METERFNARRAY.length-1);
		float f;
		f = trafficSituation / (float)MAXPROGRESSBARVALUE;
		f = f * (METERFNARRAY.length-3) + 2;
		if (debug) System.out.println("getCurrentProtectionLevel(): f="+f);
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

	public void setAnonMode(boolean bAnonMode)
		{
			anonMode=bAnonMode;
			if(bAnonMode)
				startProxy();
			else
				stopProxy();
			notifyJAPObservers();
		}
	public boolean isAnonMode()
		{
			return anonMode;
		}
	private void startProxy()
		{
			if (isRunningProxy == false)
				{
				    // Status messages should be set default value _before_
				    // starting service to make sure that errors will be
				    // displayed right
				    status1 = getString("statusRunning");
				    status2 = getString("statusRunning");
				    isRunningProxy = true;
				    runningPortNumber = portNumber;
				    p = new JAPProxyServer(portNumber);
				    Thread proxyThread = new Thread (p);
				    proxyThread.start();
				    this.notifyJAPObservers();
				}
		}
	
	private void stopProxy()
		{
			if (isRunningProxy)
				{
					p.stopService();
					isRunningProxy = false;
					status1 = getString("statusNotRunning");
					status2 = getString("statusNotRunning");
					this.notifyJAPObservers();
				}
		}
		
	public void goodBye()
		{
			stopProxy();
			save();
		}
	
	public void addJAPObserver(Object o) {
		observerVector.addElement(o);
	}
	
	public synchronized void notifyJAPObservers()
		{
			if (debug) System.out.println("notifyJAPObservers()");
				Enumeration enum = observerVector.elements();
	    while (enum.hasMoreElements())
				{
					JAPObserver listener = (JAPObserver)enum.nextElement();
					listener.valuesChanged(this);
				}
		}
	
		public void valuesChanged (Object o)
			{
				if (debug)
					System.out.println("model.valuesChanged()");
				if (runningPortNumber != portNumber)
					{
						stopProxy();
						startProxy();
					}
			}

	ImageIcon loadImageIcon(String strImage,boolean sync)
		{
			ImageIcon i=null;
			try
				{
					i=new ImageIcon(getClass().getResource(strImage));
				}
			catch(Exception e)
				{
					return null;
				}
			if(sync)
				{
					while(true)
						{
							int status=i.getImageLoadStatus();
							if((status&MediaTracker.COMPLETE)!=0)
								return i;
							else if(((status&MediaTracker.ABORTED)!=0)||((status&MediaTracker.ERRORED)!=0))
								return null;
						}
				}
			return i;
		}
	
	public void centerFrame(Window f)
		{
			Dimension screenSize = f.getToolkit().getScreenSize();
			Dimension ownSize = f.getSize();
			f.setLocation((screenSize.width  - ownSize.width )/2,
										(screenSize.height - ownSize.height)/2);
		}
}


