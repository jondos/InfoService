/*
 * Created on Apr 21, 2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package tor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetAddress;
//import java.util.Vector;
//import java.util.HashMap;
import java.util.*;

import tor.tinytls.TinyTLS;
import tor.cells.Cell;
import tor.cells.CreateCell;
import tor.cells.CreatedCell;
import tor.cells.DestroyCell;
import tor.cells.RelayCell;
import tor.ordescription.ORDescription;
import tor.util.helper;
import logging.*;
/**
 * @author stefan
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class Circuit {

	//max number of streams over a circuit
	private final static int MAX_STREAMS_OVER_CIRCUIT = 1000;

	private OnionRouter or;
	private OnionProxy op;
	private Vector onionRouters;
	private TinyTLS tlssocket;
	private int circID;
	private Hashtable streams;
	private boolean closed;
	private int streamIDCounter;
	private int size;
	private int recvCellCounter;

	public Circuit(int circID,Vector orList) throws IOException
	{

		this.circID = circID;
		this.streams = new Hashtable();
		this.closed = false;
		this.streamIDCounter = 10;
		this.onionRouters= (Vector)orList.clone();
		this.size = orList.size();
		if(onionRouters.size()<1)
		{
			throw new IOException("No Onionrouters defined for this circuit");
		}
		recvCellCounter=50;
	}

	public void connect() throws Exception
	{
		LogHolder.log(LogLevel.DEBUG,LogType.MISC,"[TOR] Creating Circuit '"+this.circID+"'");
		this.op = OnionProxy.getInstance();
		this.or = new OnionRouter(circID,(ORDescription)(this.onionRouters.elementAt(0)));
		InetAddress addr = InetAddress.getByName(this.or.getDescription().getAddress());
		int port = this.or.getDescription().getPort();
		this.op.send(addr,port,this.or.createConnection());
		this.or.checkCreatedCell(this.op.read(addr,port,this.circID));
		LogHolder.log(LogLevel.DEBUG,LogType.MISC,"[TOR] Connected to the first OR");
		for(int i=1;i<onionRouters.size();i++)
		{
			ORDescription nextOR = (ORDescription)(this.onionRouters.elementAt(i));
			this.op.send(addr,port,this.or.extendConnection(nextOR));
			Cell c = this.op.read(addr,port,this.circID);
			if(c instanceof RelayCell)
			{
				this.or.checkExtendedCell((RelayCell)c);
			} else
			{
				throw new IOException("Cannot Connect to router :"+nextOR.getAddress()+":"+nextOR.getPort());
			}
		}
		LogHolder.log(LogLevel.DEBUG,LogType.MISC,"[TOR] Circuit '"+this.circID+"' ready!!! - Length of this Circuit : "+this.size+" Onionrouters");
	}

	public void close() throws Exception
	{
		if(this.streams.isEmpty())
		{
			InetAddress addr = InetAddress.getByName(this.or.getDescription().getAddress());
			int port = this.or.getDescription().getPort();
			this.op.send(addr,port,new DestroyCell(this.circID));
		}
		this.closed = true;
	}

	public synchronized void send(Cell cell) throws Exception
	{
		if(cell instanceof RelayCell)
		{
			InetAddress addr = InetAddress.getByName(this.or.getDescription().getAddress());
			int port = this.or.getDescription().getPort();
			RelayCell c = this.or.encryptCell((RelayCell)cell);
			this.op.send(addr,port,c);
		}
	}

	public Cell read(int streamID) throws Exception
	{
		InetAddress addr = InetAddress.getByName(this.or.getDescription().getAddress());
		int port = this.or.getDescription().getPort();
		RelayCell cell = this.or.decryptCell((RelayCell)this.op.read(addr,port,this.circID));
		byte[] b=cell.getCellData();
		String s="";
		for(int i=0;i<b.length;i++)
		{
			s+=Integer.toString(b[i])+",";
		}
		recvCellCounter--;
		if(recvCellCounter==0)
		{
			RelayCell c=new RelayCell(this.circID,cell.RELAY_SENDME,streamID,null);
			c=or.encryptCell(c);
			op.send(addr,port,c);
			c=new RelayCell(this.circID,cell.RELAY_SENDME,0,null);
			c=or.encryptCell(c);
			op.send(addr,port,c);
			recvCellCounter=50;
		}
		LogHolder.log(LogLevel.DEBUG,LogType.MISC,"Circuit read() Tor Cell data gelesen: "+s);
		return cell;
	}

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

	public int getCircID()
	{
		return this.circID;
	}

	public TorChannel createChannel(int type) throws IOException
	{
		return new TorSocksChannel(streamIDCounter++,this);
	}

	public TorChannel createChannel(InetAddress addr, int port) throws Exception
	{
		if(this.closed)
		{
			throw new ConnectException("Circuit Closed - cannot connect");
		} else
		{
			int streamID = this.streamIDCounter;

			byte[] data = (""+addr.getHostAddress()+":"+port).getBytes();
			data = helper.conc(data,new byte[1]);
			RelayCell cell = new RelayCell(this.circID,RelayCell.RELAY_BEGIN,streamID,data);
			this.send(cell);

			Cell c=this.read(streamID);

			if(c instanceof RelayCell)
			{
				cell = (RelayCell)c;
				if(cell.getRelayCommand()==RelayCell.RELAY_CONNECTED)
				{
					this.streamIDCounter++;

					TorChannel channel = new TorChannel(streamID,this);
					this.streams.put(new Integer(streamID),channel);

					//don't allow a new connection over this circuit if max number of streams is reached
					if(this.streamIDCounter == MAX_STREAMS_OVER_CIRCUIT)
					{
						this.closed = true;
					}
					channel.start();
					return channel;
				}
			} else
			{
				LogHolder.log(LogLevel.DEBUG,LogType.MISC,"tor keine relaycell");
			}
		}
		return null;
	}

}
