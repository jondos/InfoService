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
//import java.net.InetAddress;
import java.security.SecureRandom;
import java.util.Date;
import java.util.Vector;
import anon.AnonChannel;
import anon.AnonServerDescription;
import anon.AnonService;
import anon.AnonServiceEventListener;
import anon.ErrorCodes;
import anon.tor.ordescription.InfoServiceORListFetcher;
import anon.tor.ordescription.ORDescription;
import anon.tor.ordescription.ORList;
import anon.tor.ordescription.PlainORListFetcher;
import anon.tor.util.helper;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import java.util.*;

/**
 * @author stefan
 *
 */
public class Tor implements /*Runnable,*/ AnonService
{

	//maximal possible length of a circuit
	public final static int MAX_ROUTE_LEN = 5;

	//minimal onion routers, that are used
	public final static int MIN_ROUTE_LEN = 2;

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

	//used for synchronisation on active Circuit operations...
	private Object m_oActiveCircuitSync;
	private Object m_oStartStopSync;

	private FirstOnionRouterConnectionFactory m_firstORFactory;

	//private long m_createNewCircuitIntervall;
	//private Thread m_createNewCircuitLoop;
	private volatile boolean m_bIsStarted;
	private boolean m_bIsCreatingCircuit;

	private int m_circuitLengthMin;
	private int m_circuitLengthMax;
	private int m_ConnectionsPerCircuit;

	private MyRandom m_rand;

	public final static String DEFAULT_DIR_SERVER_ADDR = "moria.seul.org";
	public final static int DEFAULT_DIR_SERVER_PORT = 9031;

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
				for (int i = 0; i < 3; i++)
				{
					int circ = m_rand.nextInt(m_MaxNrOfActiveCircuits);
					if (m_activeCircuits[circ] == null || m_activeCircuits[circ].isShutdown())
					{
						m_activeCircuits[circ] = createNewCircuit(null, -1);
					}
					if (m_activeCircuits[circ] != null && !m_activeCircuits[circ].isShutdown())
					{
						String s = m_activeCircuits[circ].resolveDNS(addr);
						if (s != null)
						{
							addr = s;
							break;
						}
					}
				}

				if (!helper.isIPAddress(addr))
				{
					return null;
				}
			}
			for (int i = 0; i < 3; i++)
			{
				int circstart = m_rand.nextInt(m_MaxNrOfActiveCircuits);
				int j = 0;
				while (j < m_MaxNrOfActiveCircuits)
				{
					int circ = circstart % m_MaxNrOfActiveCircuits;
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
							return m_activeCircuits[circ];
						}
					}
					circstart++;
					j++;
				}
				//all circuits are active but no one fits...
				//shutdown one and use them...
				int circ = circstart % m_MaxNrOfActiveCircuits;
				m_activeCircuits[circ].shutdown();
				m_activeCircuits[circ] = createNewCircuit(addr, port);
				if (m_activeCircuits[circ] != null && !m_activeCircuits[circ].isShutdown())
				{
					return m_activeCircuits[circ];
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
					(listPublished != null && listPublished.getTime() < System.currentTimeMillis() - 600000
					 /*list is older than 10 min*/
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
				Enumeration enumer = possibleOrs.elements();
				//remove alle ORs which can not connect to our destination
				boolean bHas0_8 = false;
				while (enumer.hasMoreElements())
				{
					ord = (ORDescription) enumer.nextElement();
					if(m_allowedExitNodeNames!=null&&!m_allowedExitNodeNames.contains(ord.getName()))
					{
						possibleOrs.remove(ord);
					}
					else if (addr != null && !ord.getAcl().isAllowed(addr, port))
					{
						possibleOrs.remove(ord);
					}
					else if (!ord.getSoftware().startsWith("Tor 0.0.7"))
					{
						bHas0_8 = true;
					}
				}
				//if we found one which is >0.0.7 prefer them
				if (bHas0_8)
				{
					enumer = possibleOrs.elements();
					while (enumer.hasMoreElements())
					{
						ord = (ORDescription) enumer.nextElement();
						if (ord.getSoftware().startsWith("Tor 0.0.7"))
						{
							possibleOrs.remove(ord);
						}
					}
				}
				if(possibleOrs.size()<=0)
					return null;
				//select one randomly...
				ord=(ORDescription)possibleOrs.elementAt(m_rand.nextInt(possibleOrs.size()));
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
				firstOR = m_firstORFactory.createFirstOnionRouterConnection( (ORDescription)
					orsForNewCircuit.
					elementAt(0));
				if (firstOR == null)
				{
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
	private void start() throws IOException
	{
		synchronized (m_oStartStopSync)
		{
			m_bIsStarted = true;
			m_activeCircuits = new Circuit[m_MaxNrOfActiveCircuits];
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
			start();
		}
		catch (Exception e)
		{
			return ErrorCodes.E_NOT_CONNECTED;
		}
		return ErrorCodes.E_SUCCESS;
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

}
