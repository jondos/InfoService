import java.util.*;

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
	public String 		status1 = "<init value>";
	public String 		status2 = "<init value>";

	static final String url_info_version="http://anon.inf.tu-dresden.de/~sk13/anon/jap/aktVersion.txt";
	static final String url_jap_newversion="http://anon.inf.tu-dresden.de/~sk13/anon/jap/JAP.jar";

	static ResourceBundle msg;

	static final String TITLE = "JAVA ANON PROXY -- JAP";
    static final String AUTHOR = "Hannes Federrath\n<federrath@inf.tu-dresden.de>\n \n(c) 2000 \n";

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
		msg = ResourceBundle.getBundle(MESSAGESFN, Locale.getDefault() );
		//
		observerVector = new Vector();
		//
		anonServerDatabase = new Vector();
		anonServerDatabase.addElement(new AnonServerDBEntry(anonHostName, anonPortNumber));
		anonServerDatabase.addElement(new AnonServerDBEntry(proxyHostName, proxyPortNumber));
		anonServerDatabase.addElement(new AnonServerDBEntry("ikt.inf.tu-dresden.de", 4007));
		anonServerDatabase.addElement(new AnonServerDBEntry("sole.icsi.berkeley.edu", 4007));
		anonServerDatabase.addElement(new AnonServerDBEntry("amadeus.icsi.berkeley.edu", 4007));
		anonServerDatabase.addElement(new AnonServerDBEntry("192.168.1.1", 4007));
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
		
	public void goodBye() {
		stopProxy();
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


