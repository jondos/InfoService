/*
 Copyright (c) 2004, The JAP-Team
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
/*
 * Created on Apr 21, 2004
 */
package anon.tor;

import java.io.IOException;
import java.net.ConnectException;
import java.security.SecureRandom;
import java.util.Date;
import java.util.Enumeration;
import java.util.Vector;
import anon.AnonChannel;
import anon.AnonServerDescription;
import anon.AnonService;
import anon.AnonServiceEventListener;
import anon.ErrorCodes;
import anon.crypto.MyRandom;
import anon.infoservice.Database;
import anon.tor.ordescription.InfoServiceORListFetcher;
import anon.tor.ordescription.ORDescription;
import anon.tor.ordescription.ORList;
import anon.tor.ordescription.PlainORListFetcher;
import anon.tor.util.DNSCacheEntry;
import anon.tor.util.helper;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import anon.infoservice.*;
/**
 * @author stefan
 *
 */
public class Tor implements Runnable, AnonService
{

	//maximal possible length of a circuit
	public final static int MAX_ROUTE_LEN = 5;

	//minimal onion routers, that are used
	public final static int MIN_ROUTE_LEN = 2;

	//the time when an entry in the DNS-Cache is no more actuell
	public final static int DNS_TIME_OUT = 600000;

	private static Tor ms_theTorInstance = null;

	//list of all onion routers
	private ORList m_orList;

	//list of allowed OR's
	private Vector m_allowedORNames;

	//list of allowed FirstOnionRouters
	private Vector m_allowedFirstORNames;

	//list of allowed exitnodes
	private Vector m_allowedExitNodeNames;

	//list of circuits
	//private Hashtable m_Circuits;
	//active circuit
	private Circuit[] m_activeCircuits;
	private int m_MaxNrOfActiveCircuits;
	private int m_CircuitsCreated;

	//used for synchronisation on active Circuit operations...
	private Object m_oActiveCircuitSync;
	private Object m_oStartStopSync;

	private FirstOnionRouterConnectionFactory m_firstORFactory;

	private Database m_DNSCache;

	//private long m_createNewCircuitIntervall;
	//private Thread m_createNewCircuitLoop;
	private volatile boolean m_bIsStarted;
	private boolean m_bIsCreatingCircuit;
	private boolean m_useDNSCache;

	private int m_circuitLengthMin;
	private int m_circuitLengthMax;
	private int m_ConnectionsPerCircuit;

	private MyRandom m_rand;

	public final static String DEFAULT_DIR_SERVER_ADDR = "moria.seul.org";
	public final static int DEFAULT_DIR_SERVER_PORT = 9031;

	//used to create circuits at startup
	private Thread m_circuitCreator;
	private boolean m_closeCreator;

	/**
	 * Constructor
	 *
	 * initialize variables
	 */
	private Tor()
	{
		m_orList = new ORList(new PlainORListFetcher(DEFAULT_DIR_SERVER_ADDR, DEFAULT_DIR_SERVER_PORT));
		m_oActiveCircuitSync = new Object();
		m_oStartStopSync = new Object();
		//create a new circuit every 5 minutes
		//m_createNewCircuitIntervall = 60000 * 5;

		m_firstORFactory = new FirstOnionRouterConnectionFactory();
		m_allowedORNames = null;

		m_allowedFirstORNames = null;

		m_allowedExitNodeNames = null;

		m_circuitLengthMin = MIN_ROUTE_LEN;
		m_circuitLengthMax = MAX_ROUTE_LEN;
		m_ConnectionsPerCircuit = Circuit.MAX_STREAMS_OVER_CIRCUIT;
		m_rand = new MyRandom(new SecureRandom());
		m_bIsStarted = false;
		m_bIsCreatingCircuit = false;
		m_MaxNrOfActiveCircuits = 10;
		m_activeCircuits = new Circuit[m_MaxNrOfActiveCircuits];
		m_useDNSCache = true;
		m_DNSCache = Database.getInstance(DNSCacheEntry.class);
		//counts the number of circuits that have been created (-1 : nouse of this variable / 0-m_maxnrofactivecircuits : number of created circuits)
		m_CircuitsCreated = -1;
		m_closeCreator = false;
	}

	/**
	 * updates the ORList
	 *
	 */
	private synchronized void updateORList()
	{
		synchronized (m_orList)
		{
			m_orList.updateList();
		}
	}

	protected synchronized Circuit getCircuitForDestination(String addr, int port)
	{
		if (!m_bIsStarted)
		{
			return null;
		}
		synchronized (m_oActiveCircuitSync)
		{
			if (!helper.isIPAddress(addr))
			{
				addr = resolveDNS(addr);
				if (!helper.isIPAddress(addr))
				{
					return null;
				}
			}
			int maxCircuit;
			// todo : remove busy waiting if this line works
			while(m_CircuitsCreated==0);
			if(m_CircuitsCreated>0)
			{
				maxCircuit = m_CircuitsCreated;
			} else
			{
				maxCircuit = m_MaxNrOfActiveCircuits;
			}
			for (int i = 0; i < 3; i++)
			{
				int circstart = m_rand.nextInt(maxCircuit);
				int j = 0;
				int circ = 0;
				while (j < maxCircuit)
				{
					circ = circstart % maxCircuit;
					System.out.println(circ);
					if (m_activeCircuits[circ] == null || m_activeCircuits[circ].isShutdown())
					{
						m_activeCircuits[circ] = createNewCircuit(addr, port);
						if (m_activeCircuits[circ] != null && !m_activeCircuits[circ].isShutdown())
						{
							return m_activeCircuits[circ];
						}
						else
						{
							break;
						}
					}
					else
					{
						if (m_activeCircuits[circ].isAllowed(addr, port))
						{
							System.out.println("done");
							return m_activeCircuits[circ];
						}
					}
					circstart++;
					j++;
				}
				if (m_activeCircuits[circ] != null && !m_activeCircuits[circ].isShutdown())
				{
					//all circuits are active but no one fits...
					//shutdown one and use them...
					circ = circstart % maxCircuit;
					m_activeCircuits[circ].shutdown();
					m_activeCircuits[circ] = createNewCircuit(addr, port);
					if (m_activeCircuits[circ] != null && !m_activeCircuits[circ].isShutdown())
					{
						System.out.println("done - new circuit created");
						return m_activeCircuits[circ];
					}
				}

			}
			return null;
		}
	}

	/**
	 * creates a new random Circuit
	 * @throws IOException
	 */
	private Circuit createNewCircuit(String addr, int port)
	{
		synchronized (m_oStartStopSync)
		{
			if (!m_bIsStarted)
			{
				return null;
			}
			m_bIsCreatingCircuit = true;
		}
		try
		{
			synchronized (m_orList)
			{
				ORDescription ord;
				Vector orsForNewCircuit = new Vector();
				int circuitLength = m_rand.nextInt(m_circuitLengthMax - m_circuitLengthMin + 1) +
					m_circuitLengthMin;
				//check if know about some Onion Routers...
				Date listPublished = m_orList.getPublished();
				if (listPublished != null)
				{
					long t1 = listPublished.getTime();
					long t2 = System.currentTimeMillis();
					long t3 = t2 - t1;
				}
				if (m_orList.size() == 0 ||
					(listPublished != null && listPublished.getTime() < System.currentTimeMillis() - 3600000
					 /*list is older than 1 hour*/
					 ))
				{
					updateORList();
					if (m_orList.size() == 0)
					{
						return null;
					}
				}
				//get first OR
				if (m_allowedFirstORNames != null)
				{
					ord = m_orList.getByRandom(m_allowedFirstORNames);
				}
				else
				{
					ord = m_orList.getByRandom();
				}
				LogHolder.log(LogLevel.DEBUG, LogType.TOR,
							  "added as first: " + ord.getName() + " " + ord.getSoftware());
				orsForNewCircuit.addElement(ord);
				//get last OR
				Vector possibleOrs = m_orList.getList();
				Enumeration enumer = ( (Vector) possibleOrs.clone()).elements();
				//remove alle ORs which can not connect to our destination
				boolean bHas0_8 = false;
				while (enumer.hasMoreElements())
				{
					ord = (ORDescription) enumer.nextElement();
					if (m_allowedExitNodeNames != null && !m_allowedExitNodeNames.contains(ord.getName()))
					{
						possibleOrs.removeElement(ord);
					}
					else if (addr != null && !ord.getAcl().isAllowed(addr, port))
					{
						possibleOrs.removeElement(ord);
					}
					else if (!ord.getSoftware().startsWith("Tor 0.0.7"))
					{
						bHas0_8 = true;
					}
				}
				//if we found one which is >0.0.7 prefer them
				if (bHas0_8)
				{
					enumer = ( (Vector) possibleOrs.clone()).elements();
					while (enumer.hasMoreElements())
					{
						ord = (ORDescription) enumer.nextElement();
						if (ord.getSoftware().startsWith("Tor 0.0.7"))
						{
							possibleOrs.removeElement(ord);
						}
					}
				}
				if (possibleOrs.size() <= 0)
				{
					return null;
				}
				//select one randomly...
				ord = (ORDescription) possibleOrs.elementAt(m_rand.nextInt(possibleOrs.size()));
				orsForNewCircuit.addElement(ord);
				LogHolder.log(LogLevel.DEBUG, LogType.TOR,
							  "added as last: " + ord.getName() + " " + ord.getSoftware());
				//get middle ORs
				for (int i = 2; i < circuitLength; i++)
				{
					do
					{
						if (m_allowedORNames != null)
						{
							ord = m_orList.getByRandom(m_allowedORNames);
						}
						else
						{
							ord = m_orList.getByRandom();
						}
					}
					while (orsForNewCircuit.contains(ord));
					LogHolder.log(LogLevel.DEBUG, LogType.TOR,
								  "added " + ord.getName() + " " + ord.getSoftware());
					orsForNewCircuit.insertElementAt(ord, 1);
				}
				//establishes or gets an already established SSL-connection to the first OR
				FirstOnionRouterConnection firstOR;
				ORDescription firstORDescription=(ORDescription)orsForNewCircuit.elementAt(0);
				firstOR = m_firstORFactory.createFirstOnionRouterConnection(firstORDescription);
				if (firstOR == null)
				{
					LogHolder.log(LogLevel.DEBUG, LogType.TOR,
								  "removed " + firstORDescription.getName());
					m_orList.remove(firstORDescription.getName());
					throw new IOException("Problem with router " + orsForNewCircuit +
										  ". Cannot connect.");
				}

				Circuit circuit = firstOR.createCircuit(orsForNewCircuit);
				circuit.setMaxNrOfStreams(m_ConnectionsPerCircuit);
				return circuit;
			}
		}
		catch (Exception e)
		{
			return null;
		}
		finally
		{
			m_bIsCreatingCircuit = false;
		}

	}

	/**
	 * Returns a Instance of Tor
	 * @return a Instance of Tor
	 */
	public static Tor getInstance()
	{
		if (ms_theTorInstance == null)
		{
			ms_theTorInstance = new Tor();
		}
		return ms_theTorInstance;
	}

	/**
	 * creates new circuits
	 */
	public void run()
	{
		for(int i=0;(i<m_MaxNrOfActiveCircuits)&&(!m_closeCreator);i++)
		{
			m_activeCircuits[i] = createNewCircuit("141.76.46.90",80);
			m_CircuitsCreated++;
		}
		m_CircuitsCreated = -1; 
	}
	/*	public void run()
	 {
	  while (m_bRun)
	  {
	   boolean error = true;
	   while (error)
	   {
	 error = false;
	 try
	 {
	  createNewActiveCircuit(null, -1);
	 }
	 catch (IOException ex)
	 {
	  LogHolder.log(LogLevel.DEBUG, LogType.MISC, "Error during circuit creation");
	  error = true;
	  updateORList();
	 }
	   }

	   try
	   {
	 Thread.sleep(m_createNewCircuitIntervall);
	   }
	   catch (InterruptedException ex)
	   {
	   }
	  }
	 }*/

	/**
	 * starts the Tor-Service
	 * @throws IOException
	 */
	private void start(boolean startCuircuits) throws IOException
	{
		synchronized (m_oStartStopSync)
		{
			m_bIsStarted = true;
			m_activeCircuits = new Circuit[m_MaxNrOfActiveCircuits];
			m_circuitCreator = new Thread(this);
			m_CircuitsCreated = 0;
			m_circuitCreator.start();
		}
	}

	/**
	 * stops the Tor-Service and all opended connections
	 * @throws IOException
	 */
	private void stop() throws IOException, InterruptedException
	{
		synchronized (m_oStartStopSync)
		{
			m_bIsStarted = false;
			m_closeCreator = true;
			//remove busy waiting if this works
			while(m_CircuitsCreated!=-1);
			if (m_bIsCreatingCircuit)
			{
				m_firstORFactory.closeAll();
				while (m_bIsCreatingCircuit)
				{
					Thread.sleep(100);
				}
			}
			m_firstORFactory.closeAll();
		}
	}

	/**
	 * sets a List of allowed middle Onion Routers
	 *
	 * @param ORList
	 * List of the names of allowed Onion Routers
	 * if ORList is null, then all OR's are used
	 */
	public void setOnionRouterList(Vector listOfORNames)
	{
		m_allowedORNames = listOfORNames;
	}

	/**
	 * sets a List of allowed Onion Routers that are used as entry point to the Tor Network
	 * @param FORList
	 * List of Onion Routers, if null all are allowed
	 */
	public void setFirstOnionRouterList(Vector listOfORNames)
	{
		m_allowedFirstORNames = listOfORNames;
	}

	/**
	 * sets a List of allowed exit nodes. these nodes are exit points of the Tor Network
	 * @param exitNodes
	 * List of exit nodes
	 */
	public void setExitNodes(Vector listOfORNames)
	{
		m_allowedExitNodeNames = listOfORNames;
	}

	/**
	 * sets a circuit length
	 *
	 * @param min
	 * minimum circuit length
	 * @param max
	 * maximum circuit length
	 */
	public void setCircuitLength(int min, int max)
	{
		if ( (max >= min) && (min >= MIN_ROUTE_LEN) && (max <= MAX_ROUTE_LEN))
		{
			m_circuitLengthMax = max;
			m_circuitLengthMin = min;
		}
	}

	/*** Set the total number of allowed different connections per route*/
	public void setConnectionsPerRoute(int i)
	{
		m_ConnectionsPerCircuit = i;
	}

	/**
	 * sets the time after that a new circuit is created
	 * @param milsek
	 * time in 1/1000 s
	 */
	/*	public void setCreateCircuitTime(long milsek)
	 {
	  this.m_createNewCircuitIntervall = milsek;
	 }
	 */
	/**
	 * sets the server where the onionrouterlist is fetched
	 * @param name
	 * address
	 * @param port
	 * port
	 */
	private void setORListServer(boolean bUseInfoService, String name, int port)
	{
		if (bUseInfoService)
		{
			m_orList.setFetcher(new InfoServiceORListFetcher());

		}
		else
		{
			m_orList.setFetcher(new PlainORListFetcher(name, port));

		}
	}

	public void setUseDNSCache(boolean usecache)
	{
		m_useDNSCache = usecache;
	}

	/**
	 * returns a list of all onionrouters
	 * @return
	 * returns a list with the Description of all onion routers
	 */
	public Vector getOnionRouterList()
	{
		updateORList();
		return m_orList.getList();
	}

	/**
	 * returns a list of all onion routers that are allowed at the moment as first onion routers
	 * @return
	 * first onion router list
	 */
	public Vector getFirstOnionRouterList()
	{
		return this.m_allowedFirstORNames;
	}

	/**
	 * creates a channel through the tor-network
	 * @param type
	 * channeltype
	 * @return
	 * a channel
	 * @throws IOException
	 */
	public AnonChannel createChannel(int type) throws ConnectException
	{
		try
		{
			return new TorSocksChannel(this);
		}
		catch (Exception e)
		{
			throw new ConnectException("Could not create Tor-Channel: " + e.getMessage());
		}
	}

	/**
	 * creates a channel through the tor-network
	 * @param addr
	 * address
	 * @param port
	 * port
	 * @return
	 * a channel
	 * @throws IOException
	 */
	public AnonChannel createChannel(String addr, int port) throws ConnectException
	{
		try
		{
			Circuit c = getCircuitForDestination(addr, port);
			return c.createChannel(addr, port);
		}
		catch (Exception e)
		{
			throw new ConnectException("Error creating Tor channel: " + e.getMessage());
		}
	}

	public synchronized int initialize(AnonServerDescription torDirServer)
	{
		if (! (torDirServer instanceof TorAnonServerDescription))
		{
			return ErrorCodes.E_INVALID_SERVICE;
		}
		TorAnonServerDescription td = (TorAnonServerDescription) torDirServer;
		setORListServer(td.useInfoService(), td.getTorDirServerAddr(),
						td.getTorDirServerPort());

		try
		{
			start(td.startCircuitsAtStartup());
		}
		catch (Exception e)
		{
			return ErrorCodes.E_NOT_CONNECTED;
		}
		return ErrorCodes.E_SUCCESS;
	}

	/** Proxies are not supported at the moment!*/
	public int setProxy(ImmutableProxyInterface a_Proxy)
	{
		return ErrorCodes.E_UNKNOWN;
	}

	public void shutdown()
	{
		try
		{
			stop();
		}
		catch (Exception e)
		{
		}
	}

	public void addEventListener(AnonServiceEventListener l)
	{
	}

	public void removeEventListener(AnonServiceEventListener l)
	{
	}

	public synchronized String resolveDNS(String name)
	{
		DNSCacheEntry entry;
		String resolvedIP = null;
		//searching in Cache
		if (m_useDNSCache)
		{
			entry = (DNSCacheEntry) m_DNSCache.getEntryById(name);
			if (entry != null)
			{
				LogHolder.log(LogLevel.DEBUG, LogType.TOR,
							  "Resolve from Database : " + entry.getId() + " - " + entry.getIp());
				return entry.getIp();
			}
		}
		//no entry found in Cache
		synchronized (m_oActiveCircuitSync)
		{
			int maxCircuit;
			//todo : remove busy waiting if this line works
			while(m_CircuitsCreated==0);
			if(m_CircuitsCreated>0)
			{
				maxCircuit = m_CircuitsCreated;
			} else
			{
				maxCircuit = m_MaxNrOfActiveCircuits;
			}
			for (int i = 0; i < 3; i++)
			{
				int circ = m_rand.nextInt(maxCircuit);
				if (m_activeCircuits[circ] == null || m_activeCircuits[circ].isShutdown())
				{
					m_activeCircuits[circ] = createNewCircuit(null, -1);
				}
				if (m_activeCircuits[circ] != null && !m_activeCircuits[circ].isShutdown())
				{
					String s = m_activeCircuits[circ].resolveDNS(name);
					if (s != null)
					{
						resolvedIP = s;
						break;
					}
				}
			}
		}
		if (resolvedIP != null)
		{
			entry = new DNSCacheEntry(name, resolvedIP, System.currentTimeMillis() + DNS_TIME_OUT);
			m_DNSCache.update(entry);
			LogHolder.log(LogLevel.DEBUG, LogType.TOR,
						  "Adding to Database : " + entry.getId() + " - " + entry.getIp());
		}
		return resolvedIP;
	}

	public void testDNS() throws Exception
	{
		LogHolder.log(LogLevel.DEBUG, LogType.TOR,
					  "DNS Resolve for www.mircosoft.de returned: " + resolveDNS("www.microsoft.de"));
		LogHolder.log(LogLevel.DEBUG, LogType.TOR,
					  "DNS Resolve for www.xyzabcdefg.hi returned: " + resolveDNS("www.xyzabcdefg.hi"));
		//TorChannel c=circ.createChannel("anon.inf.tu-dresden.de",80);
		//OutputStream out=c.getOutputStream();
		//out.write("GET /index.html HTTP/1.0\n\n".getBytes());
		//out.flush();
		LogHolder.log(LogLevel.DEBUG, LogType.TOR,
					  "DNS Resolve for www.bild.de returned: " + resolveDNS("www.bild.de"));
		LogHolder.log(LogLevel.DEBUG, LogType.TOR,
					  "DNS Resolve for www.mircosoft.de returned: " + resolveDNS("www.microsoft.de"));
	}

}
