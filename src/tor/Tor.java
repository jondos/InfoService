/*
 * Created on Apr 21, 2004
 */
package tor;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Hashtable;
import java.util.Random;
import java.util.Vector;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

import anon.AnonChannel;

import tor.ordescription.ORDescription;
import tor.ordescription.ORList;

/**
 * @author stefan
 *
 */
public class Tor implements Runnable{

	//maximal possible length of a circuit
	private final static int MAX_CIRCUIT_LENGTH = 10;
	//minimal onion routers, that are used
	private final static int MIN_ONION_ROUTERS = 5;

	private static Tor instance = null;

	private ORList orList;
	private Vector allowedORs;
	private Vector FORList;
	private Vector exitNodes;
	private Vector usedFORs;
	private Hashtable circuits;
	private int activeCircuit;

	private FirstOnionRouterFactory firstORFactory;

	private long createNewCircuitIntervall;
	private Thread createNewCircuitLoop;

	private int circuitLengthMin;
	private int circuitLengthMax;

	private String ORListServer;
	private int ORListPort;

	private Random rand;

	/**
	 * Constructor
	 * 
	 * initialize variables
	 */
	private Tor()
	{
		this.orList = new ORList();
		this.ORListServer = "moria.seul.org";
		this.ORListPort = 9031;
		
		//create a new circuit every 5 minutes
		this.createNewCircuitIntervall = 60000*5;
		
		this.firstORFactory = new FirstOnionRouterFactory();
		
		this.usedFORs = new Vector();
		this.allowedORs = null;
		
		this.FORList = new Vector();
		this.FORList.addElement("anize");
		this.FORList.addElement("moria1");
		this.FORList.addElement("moria2");
		
		this.exitNodes = new Vector();
		this.exitNodes.addElement("wannabe");
		this.exitNodes.addElement("cassandra");
		
		this.circuitLengthMin = 3;
		this.circuitLengthMax = 5;
		
		this.rand = new Random();
	}
	
	/**
	 * updates the ORList
	 *
	 */
	private void updateORList()
	{
		this.orList.updateList(this.ORListServer,this.ORListPort);
	}
	
	/**
	 * creates a new Circuit
	 * @throws IOException
	 */
	private synchronized void createNewCircuit() throws IOException
	{
		this.updateORList();
		int circuitLength = this.rand.nextInt(this.circuitLengthMax-this.circuitLengthMin+1)+this.circuitLengthMin;
		Vector orsForNewCircuit = new Vector();
		Object o = this.FORList.elementAt(this.rand.nextInt(this.FORList.size()));
		if(o instanceof String)
		{
			String firstOR = (String)o;
			orsForNewCircuit.addElement(this.orList.getORDescription(firstOR));
		} else
		{
			throw new IOException("Cannot create Circuit");
		}
		o = this.exitNodes.elementAt(this.rand.nextInt(this.exitNodes.size()));
		if(o instanceof String)
		{
			String lastOR = (String)o;
			orsForNewCircuit.addElement(this.orList.getORDescription(lastOR));
		} else
		{
			throw new IOException("Cannot create Circuit");
		}
		ORDescription ord = null;
		for(int i=2;i<circuitLength;i++)
		{
			do
			{
				do
				{
					o = this.orList.getList().elementAt(this.rand.nextInt(this.orList.getList().size()));
					if(o instanceof ORDescription)
					{
						ord = (ORDescription)o; 
						if(this.allowedORs!=null)
						{
							if(!this.allowedORs.contains(ord.getName()))
							{
								ord = null;
							}
						} 
					}
				} while(ord==null);
			}	while(orsForNewCircuit.contains(ord));
			orsForNewCircuit.insertElementAt(ord,1);
		}
		int circid;
		do
		{
			circid = this.rand.nextInt(65535);
		} while(this.circuits.containsKey(new Integer(circid))&&(circid!=0));
		FirstOnionRouter f = this.firstORFactory.createFirstOnionRouter((ORDescription)orsForNewCircuit.elementAt(0));
		if(!this.usedFORs.contains(f))
		{
			this.usedFORs.addElement(f);
		}
		Circuit c = new Circuit(circid,orsForNewCircuit,f);
		c.connect();
		this.circuits.put(new Integer(circid),c);
		if(this.activeCircuit!=0)
		{
			int last = this.activeCircuit;
			this.activeCircuit = circid;
			o = this.circuits.get(new Integer(last));
			if(o instanceof Circuit)
			{
				((Circuit)o).close();
			}
		} else
		{
			this.activeCircuit = circid;
		}
	}

	/**
	 * Returns a Instance of Tor
	 * @return a Instance of Tor
	 */
	public static Tor getInstance()
	{
		if(instance==null)
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
		while(createNewCircuitLoop.isAlive())
		{
			try
			{
				this.createNewCircuit();
			} catch (IOException ex)
			{
				LogHolder.log(LogLevel.DEBUG,LogType.MISC,"Error during circuit creation");
				
			}
			
			try
			{
				Thread.sleep(this.createNewCircuitIntervall);
			} catch(InterruptedException ex)
			{
			}
		}
	}

	/**
	 * starts the Tor-Service
	 * @throws IOException
	 */
	public void start() throws IOException
	{
		this.circuits = new Hashtable();
		this.activeCircuit = 0;
		this.createNewCircuitLoop = new Thread(this);
		this.createNewCircuitLoop.start();
	}
	
	/**
	 * stops the Tor-Service and all opended connections
	 * @throws IOException
	 */
	public void stop() throws IOException
	{
		if(createNewCircuitLoop!=null)
		{
			this.createNewCircuitLoop.stop();
			for(int i=0;i<this.usedFORs.size();i++)
			{
				if(this.usedFORs.elementAt(i) instanceof FirstOnionRouter)
				{
					FirstOnionRouter f = (FirstOnionRouter)this.usedFORs.elementAt(i);
					f.stop();
					f.close();
				}
			}
		}
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
		if(ORList==null)
		{
			this.allowedORs = null;
			return;
		}
		for(int i=0;i<ORList.size();i++)
		{
			if(!(ORList.elementAt(i) instanceof String))
			{
				return;
			} else if(this.orList.getORDescription((String)ORList.elementAt(i)) ==null)
			{
				return;
			}
			
		}
		if(ORList.size()>=MIN_ONION_ROUTERS)
		{
			this.allowedORs = ORList;
		}
		
	}
	
	/**
	 * sets a List of allowed Onion Routers that are used as entry point to the Tor Network
	 * @param FORList
	 * List of Onion Routers
	 */
	public void setFirstOnionRouterList(Vector FORList)
	{
		for(int i=0;i<FORList.size();i++)
		{
			if(!(this.FORList.elementAt(i) instanceof String))
			{
				return;
			}
			if(this.orList.getORDescription((String)FORList.elementAt(i))==null)
			{
				return;
			}
		}
		this.FORList = FORList;
	}
	
	/**
	 * sets a List of allowed exit nodes. these nodes are exit points of the Tor Network
	 * @param exitNodes
	 * List of exit nodes
	 */
	public void setExitNodes(Vector exitNodes)
	{
		for(int i=0;i<exitNodes.size();i++)
		{
			if(!(this.exitNodes.elementAt(i) instanceof String))
			{
				return;
			}
			if(this.orList.getORDescription((String)exitNodes.elementAt(i))==null)
			{
				return;
			}
		}
		this.exitNodes = exitNodes;
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
		if((max>=min)&&(min>1)&&(max<MAX_CIRCUIT_LENGTH))
		{
			this.circuitLengthMax = max;
			this.circuitLengthMin = min;
		}
	}

	/**
	 * sets the time after that a new circuit is created
	 * @param milsek
	 * time in 1/1000 s
	 */
	public void setCreateCircuitTime(long milsek)
	{
		this.createNewCircuitIntervall = milsek;
	}
	
	/**
	 * sets the server where the onionrouterlist is fetched
	 * @param name
	 * address
	 * @param port
	 * port
	 */
	public void setORListServer(String name, int port)
	{
		this.ORListServer = name;
		this.ORListPort = port;
	}
	
	/**
	 * returns a list of all onionrouters
	 * @return
	 * returns a list with the Description of all onion routers
	 */
	public Vector getOnionRouterList()
	{
		this.updateORList();
		return this.orList.getList();
	}

	/**
	 * returns a list of all onion routers that are allowed at the moment as first onion routers
	 * @return
	 * first onion router list
	 */
	public Vector getFirstOnionRouterList()
	{
		return this.FORList;
	}
	
	/**
	 * creates a channel through the tor-network
	 * @param type
	 * channeltype
	 * @return
	 * a channel
	 * @throws IOException
	 */
	public AnonChannel createChannel(int type) throws IOException
	{
		while(this.activeCircuit==0);
		Object o = this.circuits.get(new Integer(this.activeCircuit));
		if(o instanceof Circuit)
		{
			Circuit c = (Circuit)o;
			return c.createChannel(type);
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
	public AnonChannel createChannel(String addr,int port) throws IOException
	{
		while(this.activeCircuit==0);
		Object o = this.circuits.get(new Integer(this.activeCircuit));
		if(o instanceof Circuit)
		{
			Circuit c = (Circuit)o;
			return c.createChannel(InetAddress.getByName(addr),port);
		}
		return null;
	}
	
	

}
