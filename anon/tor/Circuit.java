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
public class Circuit {

	//max number of streams over a circuit
	private final static int MAX_STREAMS_OVER_CIRCUIT = 1000;

	private OnionRouter m_or;
	private FirstOnionRouter m_onionProxy;
	private Vector m_onionRouters;
	private TinyTLS m_tlssocket;
	private int m_circID;
	private Hashtable m_streams;
	private boolean m_closed;
	private int m_streamIDCounter;
	private int m_size;
	private int m_recvCellCounter;
	private int m_sendCellCounter;
	private Vector m_cellQueue;
	private byte[] m_resolvedData;
	private MyRandom m_rand;
	
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
	public Circuit(int circID,Vector orList,FirstOnionRouter onionProxy) throws IOException
	{
		this.m_onionProxy = onionProxy;
		this.m_circID = circID;
		this.m_streams = new Hashtable();
		this.m_closed = false;
		this.m_streamIDCounter = 10;
		this.m_onionRouters= (Vector)orList.clone();
		this.m_size = orList.size();
		this.m_extended_correct = true;
		if(this.m_onionRouters.size()<1)
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
		} catch (InterruptedException ex)
		{
		}
	}

	/**
	 * creates a circuit and connects to all onionrouters
	 * @throws IOException
	 */
	public void connect() throws IOException
	{
		this.m_destroyed = false;
		LogHolder.log(LogLevel.DEBUG,LogType.MISC,"[TOR] Creating Circuit '"+this.m_circID+"'");
		this.m_onionProxy.addCircuit(this);
		this.m_or = new OnionRouter(this.m_circID,(ORDescription)(this.m_onionRouters.elementAt(0)));
		try
		{
			this.m_created = false;
			this.m_onionProxy.send(this.m_or.createConnection());
			//wait until a created cell arrives
			while(!this.m_created)
			{
				this.waitForNotify();
			}
			System.out.println("created");
			if(this.m_destroyed)
			{
				throw new IOException("DestroyCell recieved");
			}
			for(int i=1;i<this.m_onionRouters.size();i++)
			{
				ORDescription nextOR = (ORDescription)(this.m_onionRouters.elementAt(i));
				this.m_extended = false;
				System.out.println("trying to extend");
				this.m_onionProxy.send(this.m_or.extendConnection(nextOR));
				while(!this.m_extended)
				{
					this.waitForNotify();
				}
				if(this.m_destroyed)
				{
					throw new IOException("DestroyCell recieved");
				}
				if(!this.m_extended_correct)
				{
					throw new IOException("Cannot Connect to router :"+nextOR.getAddress()+":"+nextOR.getPort());
				}
				System.out.println("extended");
			}
			LogHolder.log(LogLevel.DEBUG,LogType.MISC,"[TOR] Circuit '"+this.m_circID+"' ready!!! - Length of this Circuit : "+this.m_size+" Onionrouters");
		} catch(Exception ex)
		{
			throw new IOException(ex.getLocalizedMessage());
		}
	}

	/**
	 * leaves the circuit opened for all currently used channels, but no new connection is allowed
	 * 
	 * to close the circuit immediately use forceclose()
	 *  
	 * @throws Exception
	 */
	public void close() throws IOException
	{
		if(this.m_streams.isEmpty())
		{
			InetAddress addr = InetAddress.getByName(this.m_or.getDescription().getAddress());
			int port = this.m_or.getDescription().getPort();
			this.m_onionProxy.send(new DestroyCell(this.m_circID));
		}
		this.m_closed = true;
	}
	
	/**
	 * closes the circuit immediately.
	 * 
	 * @throws Exception
	 */
	public void forceclose() throws Exception
	{
		InetAddress addr = InetAddress.getByName(this.m_or.getDescription().getAddress());
		int port = this.m_or.getDescription().getPort();
		this.m_onionProxy.send(new DestroyCell(this.m_circID));
		this.m_streams.clear();
		this.m_closed = true;
	}
	
	/**
	 * check if the circuit is closed and no channel is opened
	 * 
	 * @return if the channel is closed
	 */
	public boolean closed()
	{
		if(this.m_closed&&this.m_streams.isEmpty())
		{
			return true;
		}
		return false;
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
			if(cell instanceof RelayCell)
			{
				if(!this.m_extended)
				{
					try
					{
						this.m_or.checkExtendedCell((RelayCell)cell);
					} catch (Exception ex)
					{
						this.m_extended_correct = false;
					}
					this.m_extended = true;
				} else
				{
					this.m_recvCellCounter--;
					if(this.m_recvCellCounter<900)
					{
						RelayCell rc=new RelayCell(this.m_circID,RelayCell.RELAY_SENDME,0,null);
						this.send(rc);
						this.m_recvCellCounter+=100;
					}

					RelayCell c = this.m_or.decryptCell((RelayCell)cell);
					Integer streamID = new Integer(c.getStreamID());
					if(c.getStreamID() == 0)	// Relay cells that belong to the circuit
					{
						switch(c.getRelayCommand())
						{
							case RelayCell.RELAY_SENDME :
							{
								this.m_sendCellCounter+=100;
								this.deliverCells();
							}
							default :
							{
							}
						}
					} else if(this.m_streams.containsKey(streamID)) //dispatch cell to the circuit where it belongs to
					{

						TorChannel channel = (TorChannel)this.m_streams.get(streamID);
						if(channel!=null)
						{
							channel.dispatchCell(c);
						}
					} else 
					{
						switch(c.getRelayCommand())
						{
							case RelayCell.RELAY_RESOLVED :
							{
								byte[] tmp = c.getPayload();
								this.m_resolvedData = helper.copybytes(tmp,11,((tmp[9]&0xFF)<<8)+(tmp[10]&0xFF));
								this.m_resolved = true;
							}
						}
					}
				}
			}	else if(cell instanceof CreatedCell)
			{
				this.m_or.checkCreatedCell(cell);
				LogHolder.log(LogLevel.DEBUG,LogType.MISC,"[TOR] Connected to the first OR");
				this.m_created = true;
			}	else if(cell instanceof PaddingCell)
			{
			
			} else if(cell instanceof DestroyCell)
			{
				LogHolder.log(LogLevel.DEBUG,LogType.MISC,"[TOR] recieved destroycell - circuit destroyed");
				this.m_destroyed = true;
				this.m_created = true;
				this.m_extended = true;
				System.out.println("Destroycell recieved");
				
			} else
			{
				LogHolder.log(LogLevel.DEBUG,LogType.MISC,"tor kein bekannter cell type");
			}
		} catch(Exception ex)
		{
			throw new IOException("Unable to dispatch the cell \n"+ex.getLocalizedMessage());
		}
		notifyAll();
	}

	/**
	 * sends a cell through the circuit
	 * @param cell
	 * cell to send
	 * @throws Exception
	 */
	public synchronized void send(Cell cell) throws Exception
	{
		if(cell instanceof RelayCell)
		{
			this.m_cellQueue.addElement(this.m_or.encryptCell((RelayCell)cell));
			this.deliverCells();
		}
	}

	/**
	 * delivers the cells if the FOR accept more cells
	 * @throws Exception
	 */
	private void deliverCells() throws Exception
	{
		if(this.m_sendCellCounter!=0)
		{
			RelayCell c = (RelayCell)this.m_cellQueue.elementAt(0);
			this.m_cellQueue.removeElementAt(0);
			this.m_onionProxy.send(c);
			this.m_sendCellCounter--;
			if(this.m_cellQueue.size()>0)
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
		while(!this.m_extended)
		{
			this.waitForNotify();
		}
		this.m_resolved = false;
		int temp = this.m_rand.nextInt(65535);
		while(this.m_streams.containsKey(new Integer(temp)))
		{
			temp = this.m_rand.nextInt(65535);
		}
		RelayCell cell = new RelayCell(this.getCircID(),RelayCell.RELAY_RESOLVE,temp,name.getBytes());
		try
		{
			this.send(cell);
		} catch(Exception ex)
		{
		}
		while(!this.m_resolved)
		{
			this.waitForNotify();
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
		if(this.m_streams.containsKey(key))
		{
			this.m_streams.remove(key);
			if(this.m_closed)
			{
				this.close();
			}
		}
	}

	/**
	 * returns the ID of this circuit
	 * @return ID
	 */
	public int getCircID()
	{
		return this.m_circID;
	}

	/**
	 * creates a channel through the tor-network
	 * @param type
	 * type of the channel
	 * @return
	 * a channel
	 * @throws IOException
	 */
	public TorChannel createChannel(int type) throws IOException
	{
		if(this.m_closed)
		{
			throw new ConnectException("Circuit Closed - cannot connect");
		} else
		{
			this.m_streamIDCounter++;
			TorSocksChannel tsc =  new TorSocksChannel(this.m_streamIDCounter,this);
			this.m_streams.put(new Integer(this.m_streamIDCounter),tsc);

			if(this.m_streamIDCounter == MAX_STREAMS_OVER_CIRCUIT)
			{
				this.m_closed = true;
			}
			return tsc;
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
	public TorChannel createChannel(InetAddress addr, int port) throws IOException
	{
		if(this.m_closed)
		{
			throw new ConnectException("Circuit Closed - cannot connect");
		} else
		{
			this.m_streamIDCounter++;
			TorChannel channel = new TorChannel(this.m_streamIDCounter,this);
			this.m_streams.put(new Integer(this.m_streamIDCounter),channel);

			if(this.m_streamIDCounter == MAX_STREAMS_OVER_CIRCUIT)
			{
				this.m_closed = true;
			}
			channel.connect(addr,port);
			return channel;
		}
	}

}
