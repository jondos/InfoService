/*
 * Created on May 9, 2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package tor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;

import tor.cells.Cell;
import tor.cells.RelayCell;
import tor.util.helper;

import anon.AnonChannel;
import anon.server.impl.AbstractChannel;

/**
 * @author stefan
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class TorChannel extends AbstractChannel implements Runnable {

	private final static int MAX_CELL_DATA = 498;

	// used for exchange data between is and os, when a socks connection is established

	protected Circuit circuit;
	protected int streamID;

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
				try
				{
					circuit.send(cell);
				} catch(Exception ex)
				{
					throw new IOException(ex.getMessage());
				}
			}
		}

	protected void start()
	{
		(new Thread(this)).start();
	}
		protected void close_impl()
		{
		}

 public void run()
 {
	 while(!m_bIsClosed)
	{
		try
		{
			Cell cell = circuit.read(streamID);
			byte []
				payload=cell.getPayload();
			if(payload[0]==5)//send_me
			{
			}
			else if(payload[0]!=2)
			{
				this.closedByPeer();
				return ;
			}
			else
			{
			int len=payload[9]&0x00FF;
			len<<=8;
			len|=(payload[10]&0x00FF);
			byte[] buffer = helper.copybytes(payload,11,len);
			recv(buffer,0,len);}
		} catch(Exception ex)
		{
			ex.printStackTrace();
			close();
			return;
		}
	}

 }

	public TorChannel(int streamID,Circuit circuit) throws IOException
	{
		super(streamID);
		this.circuit = circuit;
		this.streamID = streamID;
	}



}
