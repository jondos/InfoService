/*
 * Created on Apr 21, 2004
 */
package anon.tor;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.security.SecureRandom;
import java.util.Hashtable;
import java.util.Vector;
import anon.AnonChannel;
import anon.AnonServerDescription;
import anon.AnonService;
import anon.AnonServiceEventListener;
import anon.ErrorCodes;
import anon.tor.ordescription.ORDescription;
import anon.tor.ordescription.ORList;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/**
 * @author stefan
 *
 */
public class Tor implements Runnable, AnonService
{

	//maximal possible length of a circuit
	private final static int MAX_CIRCUIT_LENGTH = 10;
	//minimal onion routers, that are used
	private final static int MIN_ONION_ROUTERS = 5;

	private static Tor instance = null;

	//list of all onion routers
	private ORList m_orList;
	//list of allowed OR's
	private Vector m_allowedORNames;
	//list of allowed FirstOnionRouters
	private Vector m_allowedFirstORNames;
	//list of allowed exitnodes
	private Vector m_allowedExitNodeNames;
	//list of circuits
	private Hashtable m_Circuits;
	//active circuit
	private Circuit m_activeCircuit;
	//used for synchronisation on active Circuit operations...
	private Object m_oActiveCircuitSync;

	private FirstOnionRouterConnectionFactory m_firstORFactory;

	private long m_createNewCircuitIntervall;
	private Thread m_createNewCircuitLoop;
	private volatile boolean m_bRun;

	private int m_circuitLengthMin;
	private int m_circuitLengthMax;

	private String m_ORListServer;
	private int m_ORListPort;

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
		m_orList = new ORList();
		m_ORListServer = DEFAULT_DIR_SERVER_ADDR;
		m_ORListPort = DEFAULT_DIR_SERVER_PORT;
		m_oActiveCircuitSync = new Object();
		//create a new circuit every 5 minutes
		m_createNewCircuitIntervall = 60000 * 5;

		m_firstORFactory = new FirstOnionRouterConnectionFactory();
		m_allowedORNames = null;

		m_allowedFirstORNames = null;

		m_allowedExitNodeNames = null;

		m_circuitLengthMin = 3;
		m_circuitLengthMax = 5;

		m_rand = new MyRandom(new SecureRandom());
	}

	/**
	 * updates the ORList
	 *
	 */
	private synchronized void updateORList()
	{
		synchronized(m_orList)
		{
			m_orList.updateList(m_ORListServer, m_ORListPort);
		}
	}

	/**
	 * selects a OR randomly from a given list or from all allowed ORs and returns the ORdescription
	 * @param orlist list of onionrouter names
	 * if null, all possible ORs are used
	 * @return
	 */
	private synchronized ORDescription selectORRandomly(Vector orlist)
	{
		if (orlist == null)
		{
			Vector allORDescriptions = m_orList.getList();
			if (allORDescriptions == null)
			{
				return null;
			}
			return (ORDescription) allORDescriptions.elementAt(m_rand.nextInt(allORDescriptions.size()));
		}
		String orName = (String) orlist.elementAt( (m_rand.nextInt(orlist.size())));
		return m_orList.getORDescription(orName);
	}

	/**
	 * creates a new random Circuit
	 * @throws IOException
	 */
	protected synchronized Circuit createNewActiveCircuit(String addr, int port) throws IOException
	{
		synchronized (m_orList)
		{
			ORDescription ord;
			Vector orsForNewCircuit = new Vector();
			int circuitLength = m_rand.nextInt(m_circuitLengthMax - m_circuitLengthMin + 1) +
				m_circuitLengthMin;
			//check if know about some Onion Routers...
			if (m_orList.getList() == null)
			{
				updateORList();
				if (m_orList.getList() == null)
				{
					return null;
				}
			}
			//get first OR
			ord = selectORRandomly(m_allowedFirstORNames);
			LogHolder.log(LogLevel.DEBUG, LogType.TOR, "added " + ord.getName() + " " + ord.getSoftware());
			orsForNewCircuit.addElement(ord);
			//get last OR
			//remove this address resolve here...
			if (addr != null)
			{
				addr = InetAddress.getByName(addr).getHostAddress();
			}
			do
			{
				ord = selectORRandomly(m_allowedExitNodeNames);
			}
			while (orsForNewCircuit.contains(ord) || (addr != null && !ord.getAcl().isAllowed(addr, port)));
			orsForNewCircuit.addElement(ord);
			LogHolder.log(LogLevel.DEBUG, LogType.TOR, "added " + ord.getName() + " " + ord.getSoftware());
			//get middle ORs
			for (int i = 2; i < circuitLength; i++)
			{
				do
				{
					ord = selectORRandomly(m_allowedORNames);
				}
				while (orsForNewCircuit.contains(ord));
				LogHolder.log(LogLevel.DEBUG, LogType.TOR, "added " + ord.getName() + " " + ord.getSoftware());
				orsForNewCircuit.insertElementAt(ord, 1);
			}
			//get Circuit ID
			int circid;
			do
			{
				circid = m_rand.nextInt(65535);
			}
			while (m_Circuits.containsKey(new Integer(circid)) && (circid != 0));
			//establishes or gets an already established SSL-connection to the first OR
			FirstOnionRouterConnection firstOR;
			firstOR = m_firstORFactory.createFirstOnionRouterConnection( (ORDescription) orsForNewCircuit.
				elementAt(0));
			if (firstOR == null)
			{
				throw new IOException("Problem with router " + orsForNewCircuit + ". Cannot connect.");
			}
			Circuit circuit = new Circuit(circid, orsForNewCircuit, firstOR, this);
			//creates the circuit
			circuit.connect();
			m_Circuits.put(new Integer(circid), circuit);
			synchronized (m_oActiveCircuitSync)
			{
				if (m_activeCircuit != null)
				{
					//if there exists an "old" circuit --> close them --> move this to run()
					Circuit last = m_activeCircuit;
					m_activeCircuit = circuit;
					last.shutdown();
				}
				else
				{
					m_activeCircuit = circuit;
				}
			}
			return circuit;
		}
	}

	/** Used by a circuit to notify the Tor instance that a circuit was closed*/
	protected void notifyCircuitClosed(int circid)
	{
		m_Circuits.remove(new Integer(circid));
	}

	/**
	 * Returns a Instance of Tor
	 * @return a Instance of Tor
	 */
	public static Tor getInstance()
	{
		if (instance == null)
		{
			instance = new Tor();
		}
		return instance;
	}

	/**
	 * creates new circuits
	 */
	public void run()
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
	}

	/**
	 * starts the Tor-Service
	 * @throws IOException
	 */
	private synchronized void start() throws IOException
	{
		m_Circuits = new Hashtable();
		m_activeCircuit = null;
		m_createNewCircuitLoop = new Thread(this,"Tor");
		m_bRun = true;
		//m_createNewCircuitLoop.start();
	}

	/**
	 * stops the Tor-Service and all opended connections
	 * @throws IOException
	 */
	private synchronized void stop() throws IOException, InterruptedException
	{
		m_bRun = false;
		if (m_createNewCircuitLoop != null)
		{
			m_createNewCircuitLoop.interrupt();
			m_createNewCircuitLoop.join();
			m_firstORFactory.closeAll();
		}
	}

	public byte[] DNSResolve(String name)
	{
		//to be changed
		while (m_activeCircuit == null)
		{
			;
		}
		return m_activeCircuit.DNSResolve(name);
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
		if ( (max >= min) && (min > 1) && (max < MAX_CIRCUIT_LENGTH))
		{
			this.m_circuitLengthMax = max;
			this.m_circuitLengthMin = min;
		}
	}

	/**
	 * sets the time after that a new circuit is created
	 * @param milsek
	 * time in 1/1000 s
	 */
	public void setCreateCircuitTime(long milsek)
	{
		this.m_createNewCircuitIntervall = milsek;
	}

	/**
	 * sets the server where the onionrouterlist is fetched
	 * @param name
	 * address
	 * @param port
	 * port
	 */
	private void setORListServer(String name, int port)
	{
		this.m_ORListServer = name;
		this.m_ORListPort = port;
	}

	/**
	 * returns a list of all onionrouters
	 * @return
	 * returns a list with the Description of all onion routers
	 */
	public Vector getOnionRouterList()
	{
		this.updateORList();
		return this.m_orList.getList();
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
			synchronized (m_oActiveCircuitSync)
			{
				if (m_activeCircuit == null || !m_activeCircuit.isAllowed(addr, port))
				{
					while (m_activeCircuit == null)
					{
						createNewActiveCircuit(addr, port);
					}
				}
				return m_activeCircuit.createChannel(addr, port);
			}
		}
		catch (Exception e)
		{
			throw new ConnectException("Error creating Tor channel: " + e.getMessage());
		}
	}

	public int initialize(AnonServerDescription torDirServer)
	{
		if (! (torDirServer instanceof TorAnonServerDescription))
		{
			return ErrorCodes.E_INVALID_SERVICE;
		}
		setORListServer( ( (TorAnonServerDescription) torDirServer).getTorDirServerAddr(),
						( (TorAnonServerDescription) torDirServer).getTorDirServerPort());
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
