/*
 * Created on May 9, 2004
 */
package anon.tor;

import java.io.IOException;
import java.net.ConnectException;
import java.net.InetAddress;
import java.util.Vector;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

import anon.tor.cells.RelayCell;
import anon.tor.util.helper;

import anon.server.impl.AbstractChannel;

/**
 * @author stefan
 */
public class TorChannel extends AbstractChannel {

	private final static int MAX_CELL_DATA = 498;

	protected Circuit m_circuit;
	protected int m_streamID;
	protected boolean m_opened;
	protected boolean m_error;
	private int m_recvcellcounter;
	private int m_sendcellcounter;
	private Vector m_cellQueue;

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
				cell = new RelayCell(this.m_circuit.getCircID(),RelayCell.RELAY_DATA,this.m_streamID,helper.copybytes(b,0,MAX_CELL_DATA));
				b = helper.copybytes(b,MAX_CELL_DATA,len-MAX_CELL_DATA);
				len-=MAX_CELL_DATA;
			} else
			{
				cell = new RelayCell(this.m_circuit.getCircID(),RelayCell.RELAY_DATA,this.m_streamID,helper.copybytes(b,0,len));
				len=0;
			}
			this.m_cellQueue.addElement(cell);
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
		this.m_circuit = circuit;
		this.m_streamID = streamID;
		this.m_opened = false;
		this.m_error = false;
		this.m_recvcellcounter = 500;
		this.m_sendcellcounter = 500;
		this.m_cellQueue = new Vector();
	}

	private synchronized void deliverCells() throws IOException
	{
		if(this.m_cellQueue.size()>0)
		{
			try
			{
				this.m_circuit.send((RelayCell)this.m_cellQueue.elementAt(0));
				this.m_cellQueue.removeElementAt(0);
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
		RelayCell cell = new RelayCell(this.m_circuit.getCircID(),RelayCell.RELAY_BEGIN,this.m_streamID,data);
		try
		{
			this.m_circuit.send(cell);
		} catch (Exception ex)
		{
			throw new ConnectException(ex.getLocalizedMessage());
		}
		while(!this.m_opened)
		{
		}
		if(this.m_error)
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
					this.m_opened = true;
					break;
				}
				case RelayCell.RELAY_SENDME  :
				{
					this.m_sendcellcounter+=50;
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
					this.m_recvcellcounter--;
					if(this.m_recvcellcounter<250)
					{
						RelayCell rc=new RelayCell(this.m_circuit.getCircID(),RelayCell.RELAY_SENDME,0,null);
						try
						{
							this.m_circuit.send(rc);
						} catch (Exception ex)
						{
						}
						this.m_recvcellcounter+=50;
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
					this.m_error = true;
					this.m_opened = true;
					this.closedByPeer();
				}
			}
		} else
		{
			LogHolder.log(LogLevel.DEBUG,LogType.MISC,"tor keine relaycell");
			this.m_opened = true;
			this.m_error = true;
		}
	}


}
