/*
 * Created on Apr 21, 2004
 */
package anon.tor;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
//import java.util.Vector;
//import java.util.HashMap;
import java.security.SecureRandom;
import java.util.*;

import anon.tor.tinytls.TinyTLS;
import anon.tor.util.helper;
import anon.tor.cells.Cell;
import anon.tor.cells.CreatedCell;
import anon.tor.cells.DestroyCell;
import anon.tor.cells.PaddingCell;
import anon.tor.cells.RelayCell;
import anon.tor.ordescription.ORDescription;
import logging.*;

/**
 * @author stefan
 *
 */
public class Circuit
{

	//max number of streams over a circuit
	private final static int MAX_STREAMS_OVER_CIRCUIT = 1000;

	private OnionRouter m_FirstOR;
	private ORDescription m_lastORDescription;
	private FirstOnionRouterConnection m_FirstORConnection;
	private Vector m_onionRouters;
	//private TinyTLS m_tlssocket;
	private int m_circID;
	private Hashtable m_streams;
	/// Can we creat new channels?
	private boolean m_bShutdown;
	///Is this circuit destroyed?
	private boolean m_bClosed;
	private int m_streamIDCounter;
	private int m_circuitLength;
	private int m_recvCellCounter;
	private int m_sendCellCounter;
	private Vector m_cellQueue;
	private byte[] m_resolvedData;
	private MyRandom m_rand;
	//the Tor instance this circuit belongs to...
	private Tor m_Tor;

	//if the created or extended cells have arrived an are correct
	private boolean m_created;
	private boolean m_extended;
	private boolean m_extended_correct;
	private boolean m_destroyed;
	private boolean m_resolved;

	/**
	 * constructor
	 * @param circID
	 * ID of this circuit
	 * @param orList
	 * list of onionrouters to use for this circuit
	 * @param onionProxy
	 * FirstOnionRouter, where all the data will be send. the onionProxy has to be the firstOR in the orList
	 * @throws IOException
	 */
	public Circuit(int circID, Vector orList, FirstOnionRouterConnection onionProxy, Tor tor) throws IOException
	{
		m_Tor = tor;
		this.m_FirstORConnection = onionProxy;
		this.m_circID = circID;
		this.m_streams = new Hashtable();
		m_bShutdown = false;
		m_bClosed=false;
		m_streamIDCounter = 10;
		m_onionRouters = (Vector) orList.clone();
		m_circuitLength = orList.size();
		m_lastORDescription=(ORDescription)m_onionRouters.elementAt(m_circuitLength-1);
		this.m_extended_correct = true;
		if (this.m_onionRouters.size() < 1)
		{
			throw new IOException("No Onionrouters defined for this circuit");
		}
		this.m_recvCellCounter = 1000;
		this.m_sendCellCounter = 1000;
		this.m_cellQueue = new Vector();
		this.m_rand = new MyRandom(new SecureRandom());
	}

	private synchronized void waitForNotify()
	{
		try
		{
			wait();
		}
		catch (InterruptedException ex)
		{
		}
	}

	/**
	 * creates a circuit and connects to all onionrouters
	 * @throws IOException
	 */
	public synchronized void connect() throws IOException
	{
		this.m_destroyed = false;
		LogHolder.log(LogLevel.DEBUG, LogType.TOR, "[TOR] Creating Circuit '" + m_circID + "'");
		m_FirstORConnection.addCircuit(this);
		m_FirstOR = new OnionRouter(m_circID, (ORDescription) (m_onionRouters.elementAt(0)));
		try
		{
			m_created = false;
			m_FirstORConnection.send(m_FirstOR.createConnection());
			//wait until a created cell arrives
			while (!m_created)
			{
				waitForNotify();
			}
			LogHolder.log(LogLevel.DEBUG, LogType.TOR, "[TOR] created!");
			if (m_destroyed)
			{
				throw new IOException("DestroyCell recieved");
			}
			for (int i = 1; i < this.m_onionRouters.size(); i++)
			{
				ORDescription nextOR = (ORDescription) (this.m_onionRouters.elementAt(i));
				m_extended = false;
				LogHolder.log(LogLevel.DEBUG, LogType.TOR, "[TOR] trying to extend!");
				m_FirstORConnection.send(m_FirstOR.extendConnection(nextOR));
				while (!m_extended)
				{
					waitForNotify();
				}
				if (m_destroyed)
				{
					throw new IOException("DestroyCell recieved");
				}
				if (!m_extended_correct)
				{
					throw new IOException("Cannot Connect to router :" + nextOR.getAddress() + ":" +
										  nextOR.getPort());
				}
				LogHolder.log(LogLevel.DEBUG, LogType.TOR, "[TOR] extended!");
			}
			m_extended=true;
			LogHolder.log(LogLevel.DEBUG, LogType.MISC,
						  "[TOR] Circuit '" + this.m_circID + "' ready!!! - Length of this Circuit : " +
						  this.m_circuitLength + " Onionrouters");
		}
		catch (Exception ex)
		{
			throw new IOException(ex.getLocalizedMessage());
		}
	}

	/**
	 * shutdown this circuit so that it cannot be used for new connections
	 * but leaves the circuit opened for all currently used channels
	 *
	 * to close the circuit immediately use close()
	 *
	 * @throws Exception
	 */
	public synchronized void shutdown() throws IOException
	{
		if (m_streams.isEmpty())
		{
			InetAddress addr = InetAddress.getByName(m_FirstOR.getDescription().getAddress());
			int port = m_FirstOR.getDescription().getPort();
			m_FirstORConnection.send(new DestroyCell(m_circID));
		}
		m_bShutdown = true;
	}

	/**
	 * closes the circuit immediately.
	 *
	 * @throws Exception
	 */
	public synchronized void destroy() throws Exception
	{
		InetAddress addr = InetAddress.getByName(m_FirstOR.getDescription().getAddress());
		int port =m_FirstOR.getDescription().getPort();
		m_FirstORConnection.send(new DestroyCell(m_circID));
		m_streams.clear();
		m_bClosed = true;
		m_bShutdown = true;
		m_Tor.notifyCircuitClosed(m_circID);
	}

	/**
	 * circuit was destroyed by peer.
	 *
	 * @throws Exception
	 */
	public synchronized void destroyedByPeer() throws Exception
	{
		m_streams.clear();
		m_bClosed = true;
		m_bShutdown = true;
		m_Tor.notifyCircuitClosed(m_circID);
	}

	/**
	 * check if the circuit is already destroyed
	 *
	 * @return if the channel is closed
	 */
	public synchronized boolean isDestroyed()
	{
		return m_bClosed;
	}

	/* check if the circuit is already shutdown
		*
		* @return if the circuit is shutdown
		*/
	   public synchronized boolean isShutdown()
	   {
		   return m_bShutdown;
	   }

	/**
	 * dispatches cells to the opended channels
	 * @param cell
	 * cell
	 * @throws IOException
	 */
	public synchronized void dispatchCell(Cell cell) throws IOException
	{
		try
		{
			if (cell instanceof RelayCell)
			{
				if (!m_extended)
				{
					try
					{
						m_FirstOR.checkExtendedCell( (RelayCell) cell);
					}
					catch (Exception ex)
					{
						m_extended_correct = false;
					}
					m_extended = true;
					notifyAll();
				}
				else
				{
					m_recvCellCounter--;
					if (m_recvCellCounter < 900)
					{
						RelayCell rc = new RelayCell(m_circID, RelayCell.RELAY_SENDME, 0, null);
						send(rc);
						m_recvCellCounter += 100;
					}

					RelayCell c = m_FirstOR.decryptCell( (RelayCell) cell);
					Integer streamID = new Integer(c.getStreamID());
					if (c.getStreamID() == 0) // Relay cells that belong to the circuit
					{
						switch (c.getRelayCommand())
						{
							case RelayCell.RELAY_SENDME:
							{
								m_sendCellCounter += 100;
								deliverCells();
							}
							default:
							{
								LogHolder.log(LogLevel.DEBUG,LogType.TOR,"Upps...");
							}
						}
					}
					else if (this.m_streams.containsKey(streamID)) //dispatch cell to the circuit where it belongs to
					{

						TorChannel channel = (TorChannel)m_streams.get(streamID);
						if (channel != null)
						{
							channel.dispatchCell(c);
						}
						else
							LogHolder.log(LogLevel.DEBUG,LogType.TOR,"Upps...");

					}
					else
					{
						switch (c.getRelayCommand())
						{
							case RelayCell.RELAY_RESOLVED:
							{
								byte[] tmp = c.getPayload();
								m_resolvedData = helper.copybytes(tmp, 11,
									( (tmp[9] & 0xFF) << 8) + (tmp[10] & 0xFF));
								m_resolved = true;
								notifyAll();
							}
						}
					}
				}
			}
			else if (cell instanceof CreatedCell)
			{
				if(!m_FirstOR.checkCreatedCell(cell))
				{
					LogHolder.log(LogLevel.DEBUG, LogType.MISC, "[TOR] Should never be here - 'created' cell was wrong");
					m_created=false;
				}
				else
				{
				LogHolder.log(LogLevel.DEBUG, LogType.MISC, "[TOR] Connected to the first OR");
				m_created = true;
				}
				notifyAll();
			}
			else if (cell instanceof PaddingCell)
			{

			}
			else if (cell instanceof DestroyCell)
			{
				LogHolder.log(LogLevel.DEBUG, LogType.MISC, "[TOR] recieved destroycell - circuit destroyed");
				this.m_destroyed = true;
				this.m_created = false;
				this.m_extended = true;
				notifyAll();
			}
			else
			{
				LogHolder.log(LogLevel.DEBUG, LogType.MISC, "tor kein bekannter cell type");
			}
		}
		catch (Exception ex)
		{
			throw new IOException("Unable to dispatch the cell \n" + ex.getLocalizedMessage());
		}
	}

	/**
	 * sends a cell through the circuit
	 * @param cell
	 * cell to send
	 * @throws IOException
	 */
	public synchronized void send(Cell cell) throws IOException
	{
		if (cell instanceof RelayCell)
		{
			this.m_cellQueue.addElement(this.m_FirstOR.encryptCell( (RelayCell) cell));
			this.deliverCells();
		}
	}

	/**
	 * delivers the cells if the FOR accept more cells
	 * @throws Exception
	 */
	private synchronized void deliverCells() throws IOException
	{
		if (this.m_sendCellCounter != 0)
		{
			RelayCell c = (RelayCell)this.m_cellQueue.elementAt(0);
			this.m_cellQueue.removeElementAt(0);
			this.m_FirstORConnection.send(c);
			this.m_sendCellCounter--;
			if (this.m_cellQueue.size() > 0)
			{
				this.deliverCells();
			}
		}
	}

	/**
	 * Returns a address to a given name
	 * @param name
	 * @return
	 *  Type   (1 octet)
	 *  Length (1 octet)
	 *  Value  (variable-width)
	 *"Length" is the length of the Value field.
	 * "Type" is one of:
	 * 0x04 -- IPv4 address
	 * 0x06 -- IPv6 address
	 * 0xF0 -- Error, transient
	 * 0xF1 -- Error, nontransient
	 */
	public byte[] DNSResolve(String name)
	{
		while (!m_extended)
		{
			waitForNotify();
		}
		this.m_resolved = false;
		int temp = this.m_rand.nextInt(65535);
		while (this.m_streams.containsKey(new Integer(temp)))
		{
			temp = this.m_rand.nextInt(65535);
		}
		RelayCell cell = new RelayCell(this.getCircID(), RelayCell.RELAY_RESOLVE, temp, name.getBytes());
		try
		{
			this.send(cell);
		}
		catch (Exception ex)
		{
		}
		while (!m_resolved)
		{
			waitForNotify();
		}
		return this.m_resolvedData;
	}

	/**
	 * closes a stream
	 * @param streamID
	 * streamID
	 * @throws Exception
	 */
	public void close(int streamID) throws Exception
	{
		Integer key = new Integer(streamID);
		if (m_streams.containsKey(key))
		{
			m_streams.remove(key);
			if (m_bShutdown)
			{
				//check if we can destroy this circuit
				shutdown();
			}
		}
	}

	/**
	 * returns the ID of this circuit
	 * @return ID
	 */
	public int getCircID()
	{
		return m_circID;
	}

	/**
	 * creates a channel through the tor-network
	 * @param type
	 * type of the channel
	 * @return
	 * a channel
	 * @throws IOException
	 */
/*	public TorChannel createSOCKSChannel(int type) throws IOException
	{
		if (m_bShutdown)
		{
			throw new ConnectException("Circuit Closed - cannot connect");
		}
		else
		{
			m_streamIDCounter++;
			TorSocksChannel tsc = new TorSocksChannel(m_streamIDCounter, this);
			m_streams.put(new Integer(m_streamIDCounter), tsc);

			if (m_streamIDCounter == MAX_STREAMS_OVER_CIRCUIT)
			{
				shutdown();
			}
			return tsc;
		}
	}
*/
	/**
	 * creates a channel through the tor-network
	 * @param addr
	 * address of the server you want do connect
	 * @param port
	 * port
	 * @return
	 * a channel
	 * @throws IOException
	 */
	public TorChannel createChannel(String addr, int port) throws IOException
	{
		///todo ACL ueberpruefen
		if (m_bShutdown)
		{
			throw new ConnectException("Circuit Closed - cannot connect");
		}
		else
		{
			m_streamIDCounter++;
			TorChannel channel = new TorChannel(this.m_streamIDCounter, this);
			this.m_streams.put(new Integer(this.m_streamIDCounter), channel);
			channel.connect(addr, port);
			if (m_streamIDCounter == MAX_STREAMS_OVER_CIRCUIT)
			{
				shutdown();
			}
			return channel;
		}
	}

	/**
	 * creates a channel through the tor-network
	 * @param addr
	 * address of the server you want do connect
	 * @param port
	 * port
	 * @return
	 * a channel
	 * @throws IOException
	 */
	protected void connectChannel(TorChannel channel,String addr, int port) throws IOException
	{
		///todo ACL ueberpruefen
		if (m_bShutdown)
		{
			throw new ConnectException("Circuit Closed - cannot connect");
		}
		else
		{
			m_streamIDCounter++;
			channel.setStreamID(m_streamIDCounter);
			channel.setCircuit(this);
			m_streams.put(new Integer(this.m_streamIDCounter), channel);

			if (m_streamIDCounter == MAX_STREAMS_OVER_CIRCUIT)
			{
				shutdown();
			}
			channel.connect(addr, port);
		}
	}

	public boolean isAllowed(String adr, int port)
	{
		return m_lastORDescription.getAcl().isAllowed(adr,port);
	}

}
