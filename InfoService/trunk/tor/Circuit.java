/*
 * Created on Apr 21, 2004
 */
package tor;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
//import java.util.Vector;
//import java.util.HashMap;
import java.util.*;

import tor.tinytls.TinyTLS;
import tor.cells.Cell;
import tor.cells.CreatedCell;
import tor.cells.DestroyCell;
import tor.cells.PaddingCell;
import tor.cells.RelayCell;
import tor.ordescription.ORDescription;
import logging.*;
/**
 * @author stefan
 *
 */
public class Circuit {

	//max number of streams over a circuit
	private final static int MAX_STREAMS_OVER_CIRCUIT = 1000;

	private OnionRouter or;
	private FirstOnionRouter onionProxy;
	private Vector onionRouters;
	private TinyTLS tlssocket;
	private int circID;
	private Hashtable streams;
	private boolean closed;
	private int streamIDCounter;
	private int size;
	private int recvCellCounter;
	private int sendCellCounter;
	private Vector cellQueue;
	
	//if the created or extended cells have arrived an are correct
	private boolean created;
	private boolean extended;
	private boolean extended_correct;

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
		this.onionProxy = onionProxy;
		this.circID = circID;
		this.streams = new Hashtable();
		this.closed = false;
		this.streamIDCounter = 10;
		this.onionRouters= (Vector)orList.clone();
		this.size = orList.size();
		this.extended_correct = true;
		if(onionRouters.size()<1)
		{
			throw new IOException("No Onionrouters defined for this circuit");
		}
		this.recvCellCounter = 1000;
		this.sendCellCounter = 1000;
		this.cellQueue = new Vector();
	}

	/**
	 * creates a circuit and connects to all onionrouters
	 * @throws IOException
	 */
	public void connect() throws IOException
	{
		LogHolder.log(LogLevel.DEBUG,LogType.MISC,"[TOR] Creating Circuit '"+this.circID+"'");
		this.onionProxy.addCircuit(this);
		this.or = new OnionRouter(circID,(ORDescription)(this.onionRouters.elementAt(0)));
		InetAddress addr = InetAddress.getByName(this.or.getDescription().getAddress());
		int port = this.or.getDescription().getPort();
		try
		{
			this.created = false;
			this.onionProxy.send(this.or.createConnection());
			//wait until a created cell arrives
			while(!this.created);
			for(int i=1;i<onionRouters.size();i++)
			{
				ORDescription nextOR = (ORDescription)(this.onionRouters.elementAt(i));
				this.extended = false;
				this.onionProxy.send(this.or.extendConnection(nextOR));
				while(!this.extended);
				if(!this.extended_correct)
				{
					throw new IOException("Cannot Connect to router :"+nextOR.getAddress()+":"+nextOR.getPort());
				}
			}
			LogHolder.log(LogLevel.DEBUG,LogType.MISC,"[TOR] Circuit '"+this.circID+"' ready!!! - Length of this Circuit : "+this.size+" Onionrouters");
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
		if(this.streams.isEmpty())
		{
			InetAddress addr = InetAddress.getByName(this.or.getDescription().getAddress());
			int port = this.or.getDescription().getPort();
			this.onionProxy.send(new DestroyCell(this.circID));
		}
		this.closed = true;
	}
	
	/**
	 * closes the circuit immediately.
	 * 
	 * @throws Exception
	 */
	public void forceclose() throws Exception
	{
		InetAddress addr = InetAddress.getByName(this.or.getDescription().getAddress());
		int port = this.or.getDescription().getPort();
		this.onionProxy.send(new DestroyCell(this.circID));
		this.streams.clear();
		this.closed = true;
	}
	
	/**
	 * check if the circuit is closed and no channel is opened
	 * 
	 * @return if the channel is closed
	 */
	public boolean closed()
	{
		if(this.closed&&this.streams.isEmpty())
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
				if(!this.extended)
				{
					try
					{
						this.or.checkExtendedCell((RelayCell)cell);
					} catch (Exception ex)
					{
						this.extended_correct = false;
					}
					this.extended = true;
				} else
				{
					this.recvCellCounter--;
					if(this.recvCellCounter<900)
					{
						RelayCell rc=new RelayCell(this.circID,RelayCell.RELAY_SENDME,0,null);
						this.send(rc);
						this.recvCellCounter+=100;
					}

					RelayCell c = this.or.decryptCell((RelayCell)cell);
					Integer streamID = new Integer(c.getStreamID());
					if(this.streams.containsKey(streamID))
					{

						TorChannel channel = (TorChannel)this.streams.get(streamID);
						if(channel!=null)
						{
							channel.dispatchCell(c);
						}
					} else if(streamID.intValue()==0)
					{
						if(c.getRelayCommand()==RelayCell.RELAY_SENDME)
						{
							this.sendCellCounter+=100;
							this.send(null);
						}
					}
				}
			}	else if(cell instanceof CreatedCell)
			{
				this.or.checkCreatedCell(cell);
				LogHolder.log(LogLevel.DEBUG,LogType.MISC,"[TOR] Connected to the first OR");
				this.created = true;
			}	else if(cell instanceof PaddingCell)
			{
			
			} else
			{
				LogHolder.log(LogLevel.DEBUG,LogType.MISC,"tor kein bekannter cell type");
			}
		} catch(Exception ex)
		{
			throw new IOException("Unable to dispatch the cell \n"+ex.getLocalizedMessage());
		}
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
			this.cellQueue.addElement(this.or.encryptCell((RelayCell)cell));
			this.deliverCells();
		} else if(cell==null)
		{
			this.deliverCells();
		}
	}

	/**
	 * delivers the cells if the FOR accept more cells
	 * @throws Exception
	 */
	public void deliverCells() throws Exception
	{
		if(this.sendCellCounter!=0)
		{
			RelayCell c = (RelayCell)this.cellQueue.elementAt(0);
			this.cellQueue.removeElementAt(0);
			this.onionProxy.send(c);
			this.sendCellCounter--;
			if(this.cellQueue.size()>0)
			{
				this.deliverCells();
			}
		}
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
		if(this.streams.containsKey(key))
		{
			this.streams.remove(key);
			if(this.closed)
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
		return this.circID;
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
		if(this.closed)
		{
			throw new ConnectException("Circuit Closed - cannot connect");
		} else
		{
			this.streamIDCounter++;
			TorSocksChannel tsc =  new TorSocksChannel(streamIDCounter,this);
			this.streams.put(new Integer(this.streamIDCounter),tsc);

			if(this.streamIDCounter == MAX_STREAMS_OVER_CIRCUIT)
			{
				this.closed = true;
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
		if(this.closed)
		{
			throw new ConnectException("Circuit Closed - cannot connect");
		} else
		{
			this.streamIDCounter++;
			TorChannel channel = new TorChannel(this.streamIDCounter,this);
			this.streams.put(new Integer(this.streamIDCounter),channel);

			if(this.streamIDCounter == MAX_STREAMS_OVER_CIRCUIT)
			{
				this.closed = true;
			}
			channel.connect(addr,port);
			return channel;
		}
	}

}
