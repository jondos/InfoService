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
	private Vector m_allowedORs;
	//list of allowed FirstOnionRouters
	private Vector m_FORList;
	//list of allowed exitnodes
	private Vector m_exitNodes;
	//list of used FOR's
	private Vector m_usedFORs;
	//list of circuits
	private Hashtable m_circuits;
	//active circuit
	private int m_activeCircuit;

	private FirstOnionRouterFactory m_firstORFactory;

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
		this.m_orList = new ORList();
		this.m_ORListServer = DEFAULT_DIR_SERVER_ADDR;
		this.m_ORListPort = DEFAULT_DIR_SERVER_PORT;

		//create a new circuit every 5 minutes
		this.m_createNewCircuitIntervall = 60000 * 5;

		this.m_firstORFactory = new FirstOnionRouterFactory();

		this.m_usedFORs = new Vector();
		this.m_allowedORs = null;

		this.m_FORList = new Vector();

		this.m_exitNodes = new Vector();

		this.m_circuitLengthMin = 3;
		this.m_circuitLengthMax = 5;

		this.m_rand = new MyRandom(new SecureRandom());
	}

	/**
	 * updates the ORList
	 *
	 */
	private void updateORList()
	{
		this.m_orList.updateList(this.m_ORListServer, this.m_ORListPort);
	}

	/**
	 * selects a or randomly from a given list and returns a ORdescription
	 * @param orlist
	 * list of onionrouters
	 * @return
	 */
	private synchronized ORDescription selectORRandomly(Vector orlist)
	{
		Vector temp = this.m_orList.getList();
		Object temp2;
		ORDescription temp3;
		Vector orl = orlist;
		if(orlist ==null)
		{
			orl=new Vector();
		}
		if(orl.size()>0)
		{
			temp2 = orl.elementAt((this.m_rand.nextInt(orl.size())));
			if(temp2 instanceof String)
			{
				temp3 = this.m_orList.getORDescription((String)temp2);
				if(temp3!=null)
				{
					return temp3;
				}
			}
		} else
		{
			if(this.m_allowedORs!=null)
			{
				if(this.m_allowedORs.size()>0)
				{
					temp2 = this.m_allowedORs.elementAt(this.m_rand.nextInt(this.m_allowedORs.size()));
					if(temp2 instanceof String)
					{
						temp3 = this.m_orList.getORDescription((String)temp2);
						if(temp3!=null)
						{
							return temp3;
						}
					}
				}
			}
		}
		return (ORDescription)temp.elementAt(this.m_rand.nextInt(temp.size()));
	}

	/**
	 * creates a new Circuit
	 * @throws IOException
	 */
	private synchronized void createNewCircuit() throws IOException
	{
		ORDescription ord;
		Object o;
		Vector orsForNewCircuit = new Vector();
		int circuitLength = m_rand.nextInt(this.m_circuitLengthMax - this.m_circuitLengthMin + 1) +
			this.m_circuitLengthMin;
		ord = this.selectORRandomly(this.m_FORList);
		orsForNewCircuit.addElement(ord);
		do
		{
			ord = this.selectORRandomly(this.m_exitNodes);
		} while((orsForNewCircuit.contains(ord))/*||(!ord.getAcl().isAllowed(80))*/);
		orsForNewCircuit.addElement(ord);
		for (int i = 2; i < circuitLength; i++)
		{
			do
			{
				ord=this.selectORRandomly(null);
			}
			while (orsForNewCircuit.contains(ord));
			orsForNewCircuit.insertElementAt(ord, 1);
		}
		int circid;
		do
		{
			circid = this.m_rand.nextInt(65535);
		}
		while (this.m_circuits.containsKey(new Integer(circid)) && (circid != 0));
		FirstOnionRouter f = this.m_firstORFactory.createFirstOnionRouter( (ORDescription) orsForNewCircuit.
			elementAt(0));
		if (!this.m_usedFORs.contains(f))
		{
			this.m_usedFORs.addElement(f);
		}
		Circuit c = new Circuit(circid, orsForNewCircuit, f);
		c.connect();
		this.m_circuits.put(new Integer(circid), c);
		if (this.m_activeCircuit != 0)
		{
			int last = this.m_activeCircuit;
			this.m_activeCircuit = circid;
			o = this.m_circuits.get(new Integer(last));
			if (o instanceof Circuit)
			{
				( (Circuit) o).close();
			}
		}
		else
		{
			this.m_activeCircuit = circid;
		}
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
		this.updateORList();
		while (m_bRun)
		{
			boolean error = true;
			while (error)
			{
				error = false;
				try
				{
					this.createNewCircuit();
				}
				catch (IOException ex)
				{
					LogHolder.log(LogLevel.DEBUG, LogType.MISC, "Error during circuit creation");
					error = true;
					this.updateORList();
				}
			}

			try
			{
				Thread.sleep(this.m_createNewCircuitIntervall);
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
	private void start() throws IOException
	{
		this.m_circuits = new Hashtable();
		this.m_activeCircuit = 0;
		this.m_createNewCircuitLoop = new Thread(this);
		m_bRun = true;
		this.m_createNewCircuitLoop.start();
	}

	/**
	 * stops the Tor-Service and all opended connections
	 * @throws IOException
	 */
	private void stop() throws IOException, InterruptedException
	{
		m_bRun = false;
		if (this.m_createNewCircuitLoop != null)
		{
			m_createNewCircuitLoop.interrupt();
			this.m_createNewCircuitLoop.join();
			for (int i = 0; i < this.m_usedFORs.size(); i++)
			{
				if (this.m_usedFORs.elementAt(i) instanceof FirstOnionRouter)
				{
					FirstOnionRouter f = (FirstOnionRouter)this.m_usedFORs.elementAt(i);
					f.stop();
					f.close();
				}
			}
		}
	}

	public byte[] DNSResolve(String name)
	{
		while(this.m_activeCircuit==0);
		Circuit c = (Circuit)this.m_circuits.get(new Integer(this.m_activeCircuit));
		return c.DNSResolve(name);
	}

	/**
	 * sets a List of allowed Onion Routers
	 *
	 * @param ORList
	 * List of the names of allowed Onion Routers
	 * if ORList is null, then all OR's are used
	 */
	public void setOnionRouterList(Vector ORList)
	{
		if (ORList == null)
		{
			this.m_allowedORs = null;
			return;
		}
		for (int i = 0; i < ORList.size(); i++)
		{
			if (! (ORList.elementAt(i) instanceof String))
			{
				return;
			}
			else if (this.m_orList.getORDescription( (String) ORList.elementAt(i)) == null)
			{
				return;
			}

		}
		if (ORList.size() >= MIN_ONION_ROUTERS)
		{
			this.m_allowedORs = ORList;
		}

	}

	/**
	 * sets a List of allowed Onion Routers that are used as entry point to the Tor Network
	 * @param FORList
	 * List of Onion Routers
	 */
	public void setFirstOnionRouterList(Vector FORList)
	{
		for (int i = 0; i < FORList.size(); i++)
		{
			if (! (this.m_FORList.elementAt(i) instanceof String))
			{
				return;
			}
			if (this.m_orList.getORDescription( (String) FORList.elementAt(i)) == null)
			{
				return;
			}
		}
		this.m_FORList = FORList;
	}

	/**
	 * sets a List of allowed exit nodes. these nodes are exit points of the Tor Network
	 * @param exitNodes
	 * List of exit nodes
	 */
	public void setExitNodes(Vector exitNodes)
	{
		for (int i = 0; i < exitNodes.size(); i++)
		{
			if (! (this.m_exitNodes.elementAt(i) instanceof String))
			{
				return;
			}
			if (this.m_orList.getORDescription( (String) exitNodes.elementAt(i)) == null)
			{
				return;
			}
		}
		this.m_exitNodes = exitNodes;
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
		return this.m_FORList;
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
			while (this.m_activeCircuit == 0)
			{
				;
			}
			Object o = this.m_circuits.get(new Integer(this.m_activeCircuit));
			if (o instanceof Circuit)
			{
				Circuit c = (Circuit) o;
				return c.createChannel(type);
			}
		}
		catch (Exception e)
		{
			throw new ConnectException("Could not create Tor-Channel: " + e.getMessage());
		}
		return null;
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
	public AnonChannel createChannel(InetAddress addr, int port) throws ConnectException
	{
		try
		{
			while (this.m_activeCircuit == 0)
			{
				;
			}
			Object o = this.m_circuits.get(new Integer(this.m_activeCircuit));
			if (o instanceof Circuit)
			{
				Circuit c = (Circuit) o;
				return c.createChannel(addr, port);
			}
		}
		catch (Exception e)
		{
			throw new ConnectException("Error creating Tor channel: " + e.getMessage());
		}
		return null;
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
