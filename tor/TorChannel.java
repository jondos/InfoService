/*
 * Created on May 9, 2004
 */
package tor;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.util.Vector;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

import tor.cells.RelayCell;
import tor.util.helper;

import anon.server.impl.AbstractChannel;

/**
 * @author stefan
 */
public class TorChannel extends AbstractChannel {

	private final static int MAX_CELL_DATA = 498;

	protected Circuit circuit;
	protected int streamID;
	protected boolean opened;
	protected boolean error;
	private int recvcellcounter;
	private int sendcellcounter;
	private Vector cellQueue;

	public int getOutputBlockSize()
	{
		return MAX_CELL_DATA;
	}

	protected void send(byte[] arg0,int len) throws IOException
	{
		byte[] b = arg0;
		RelayCell cell;
		while(len!=0)
		{
			if(len>MAX_CELL_DATA)
			{
				cell = new RelayCell(circuit.getCircID(),RelayCell.RELAY_DATA,streamID,helper.copybytes(b,0,MAX_CELL_DATA));
				b = helper.copybytes(b,MAX_CELL_DATA,len-MAX_CELL_DATA);
				len-=MAX_CELL_DATA;
			} else
			{
				cell = new RelayCell(circuit.getCircID(),RelayCell.RELAY_DATA,streamID,helper.copybytes(b,0,len));
				len=0;
			}
			this.cellQueue.addElement(cell);
			this.deliverCells();
		}
	}

	protected void close_impl()
	{
	}

	/**
	 * constructor
	 * @param streamID
	 * streamID of the new channel
	 * @param circuit
	 * the circuit where this channel belongs to
	 * @throws IOException
	 */
	public TorChannel(int streamID,Circuit circuit) throws IOException
	{
		super(streamID);
		this.circuit = circuit;
		this.streamID = streamID;
		this.opened = false;
		this.error = false;
		this.recvcellcounter = 500;
		this.sendcellcounter = 500;
		this.cellQueue = new Vector();
	}

	private synchronized void deliverCells() throws IOException
	{
		if(this.cellQueue.size()>0)
		{
			try
			{
				this.circuit.send((RelayCell)this.cellQueue.elementAt(0));
				this.cellQueue.removeElementAt(0);
			} catch(Exception ex)
			{
				throw new IOException(ex.getMessage());
			}
		}
	}

	/**
	 * connects to a host over the tor-network
	 * @param addr
	 * address
	 * @param port
	 * port
	 * @throws ConnectException
	 */
	public synchronized void connect(InetAddress addr, int port) throws ConnectException
	{
		byte[] data = (""+addr.getHostAddress()+":"+port).getBytes();
		data = helper.conc(data,new byte[1]);
		RelayCell cell = new RelayCell(this.circuit.getCircID(),RelayCell.RELAY_BEGIN,this.streamID,data);
		try
		{
			this.circuit.send(cell);
		} catch (Exception ex)
		{
			throw new ConnectException(ex.getLocalizedMessage());
		}
		while(!this.opened)
		{
		}
		if(this.error)
		{
			throw new ConnectException("Cannot connect to "+addr.getHostAddress()+":"+port);
		}
	}
	
	/**
	 * dispatches the cells to the outputstream
	 * @param cell
	 * cell
	 */
	public void dispatchCell(RelayCell cell)
	{
		if(cell instanceof RelayCell)
		{
			RelayCell c = (RelayCell)cell;
			switch(c.getRelayCommand())
			{
				case RelayCell.RELAY_CONNECTED :
				{
					this.opened = true;
					break;
				}
				case RelayCell.RELAY_SENDME  :
				{
					this.sendcellcounter+=50;
					try
					{
						this.deliverCells();
					} catch (IOException ex)
					{
					}
					break;
				}
				case RelayCell.RELAY_DATA :
				{
					this.recvcellcounter--;
					if(this.recvcellcounter<250)
					{
						RelayCell rc=new RelayCell(this.circuit.getCircID(),RelayCell.RELAY_SENDME,0,null);
						try
						{
							this.circuit.send(rc);
						} catch (Exception ex)
						{
						}
						this.recvcellcounter+=50;
					}
					try
					{
						byte[] payload = cell.getPayload();
						int len=payload[9]&0x00FF;
						len<<=8;
						len|=(payload[10]&0x00FF);
						byte[] buffer = helper.copybytes(payload,11,len);
						recv(buffer,0,len);
					} catch(Exception ex)
					{
						ex.printStackTrace();
						close();
					}
					break;
				}
				default :
				{
					this.error = true;
					this.opened = true;
					this.closedByPeer();
				}
			}
		} else
		{
			LogHolder.log(LogLevel.DEBUG,LogType.MISC,"tor keine relaycell");
			this.opened = true;
			this.error = true;
		}
	}


}
