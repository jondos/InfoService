import java.util.*;
import com.sun.xml.tree.XmlDocument;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.FileInputStream;
import java.io.FileOutputStream;
public class JAPModel implements JAPObserver {

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
	public String 		status1 = "OK";
	public String 		status2 = "OK";

	static final String url_info_version="http://anon.inf.tu-dresden.de/~sk13/anon/jap/aktVersion.txt";
	static final String url_jap_newversion="http://anon.inf.tu-dresden.de/~sk13/anon/jap/JAP.jar";

	private ResourceBundle msg;

	static final String TITLE = "JAVA ANON PROXY -- JAP";
    static final String AUTHOR = "The JAP-Team\n<jap@inf.tu-dresden.de>\n \n(c) 2000 \n";

	static final String MESSAGESFN = "JAPMessages";
	static final int	MAXHELPLANGUAGES = 6;
	static final String SPLASHFN = "images/splash.gif";
	static final String JAPICONFN = "images/japi.gif";
	static final String CONFIGICONFN = "images/icoc.gif";
	static final String METERICONFN = "images/icom.gif";
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
	static final int 	MAXPROGRESSBARVALUE = 100;
	static final boolean NOMEASURE = false;
	public int 			nrOfActiveUsers = 1;
	public int 			trafficSituation = 0;
	public int 			currentRisk = 100;
	
	private Vector observerVector;
	public Vector anonServerDatabase;
	
	private ProxyServer p;

	
	public JAPModel () {
		// Load Texts for Messages and Windows
		try
			{
				msg = ResourceBundle.getBundle(MESSAGESFN, Locale.getDefault() );
			}
		catch(Exception e)
			{
				try
					{
						msg = ResourceBundle.getBundle(MESSAGESFN, Locale.ENGLISH );
					}
				catch(Exception e1)
					{
						try
							{
								msg = ResourceBundle.getBundle(MESSAGESFN);
							}
						catch(Exception e2)
							{
								System.out.println("Can't find any Message String - Critical Error - Terminating...");
								System.exit(-1);
							}
					}
			}
		//
		observerVector = new Vector();
		//
		anonServerDatabase = new Vector();
		anonServerDatabase.addElement(new AnonServerDBEntry(anonHostName, anonPortNumber));
		anonServerDatabase.addElement(new AnonServerDBEntry(proxyHostName, proxyPortNumber));
		anonServerDatabase.addElement(new AnonServerDBEntry("anon.inf.tu-dresden.de", 6543));
		anonServerDatabase.addElement(new AnonServerDBEntry("sole.icsi.berkeley.edu", 4007));
		anonServerDatabase.addElement(new AnonServerDBEntry("localhost", 6543));
		anonServerDatabase.addElement(new AnonServerDBEntry("192.168.1.1", 4007));
	}
	
		public boolean load()
			{
				try
					{
						FileInputStream f=new FileInputStream("jap.conf");
						Document doc=DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(f);
						NamedNodeMap n=doc.getFirstChild().getAttributes();
						anonHostName=n.getNamedItem("host").getNodeValue();
						anonPortNumber=Integer.valueOf(n.getNamedItem("port").getNodeValue()).intValue();
						System.out.println(anonHostName);
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
						FileOutputStream f=new FileOutputStream("jap.conf");
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
	
		
	public String getString(String key)
		{
			try
				{
					return msg.getString(key);
				}
			catch(Exception e)
				{
					return key;
				}
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
					isRunningProxy = true;
					runningPortNumber = portNumber;
					p = new ProxyServer(portNumber, debug,this);
					Thread proxyThread = new Thread (p);
					proxyThread.start();
					status1 = "Listening...";
				}
		}
	
	private void stopProxy()
		{
			if (isRunningProxy)
				{
					p.stopService();
					isRunningProxy = false;
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
	
	public void notifyJAPObservers() {
		if (debug) System.out.println("notifyJAPObservers()");
		Enumeration enum = observerVector.elements();
	    while (enum.hasMoreElements()){
			JAPObserver listener = (JAPObserver)enum.nextElement();
			listener.valuesChanged(this);
		}
	}
	
	public void valuesChanged (Object o) {
		if (debug) System.out.println("model.valuesChanged()");
		if (runningPortNumber != portNumber) {
			stopProxy();
			startProxy();
		}
	}

}


