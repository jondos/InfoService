/*
 * Created on Jun 13, 2004
 *
 */
package anon.tor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Hashtable;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

import anon.tor.cells.Cell;
import anon.tor.cells.CreatedCell;
import anon.tor.cells.DestroyCell;
import anon.tor.cells.RelayCell;
import anon.tor.ordescription.ORDescription;
import anon.tor.tinytls.TinyTLS;

/**
 *
 */
public class FirstOnionRouter implements Runnable {

	private TinyTLS m_tinyTLS;
	private ORDescription m_description;
	private Thread m_readDataLoop;
	private InputStream m_istream;
	private OutputStream m_ostream;
	private Hashtable m_circuits;
	private volatile boolean m_bRun;

	/**
	 * constructor
	 *
	 * creates a FOR from the description
	 * @param d
	 * description of the onion router
	 */
	public FirstOnionRouter(ORDescription d)
	{
		this.m_readDataLoop = null;
		this.m_description = d;
	}

	/**
	 * sends a cell
	 * @param cell
	 * cell with data
	 * @throws IOException
	 */
	public void send(Cell cell) throws IOException
	{
		this.m_ostream.write(cell.getCellData());
	}

	/**
	 * dispatches a cell to the circuits if one is recieved
	 *
	 */
	synchronized private void dispatchCells()
	{
		Cell cell = null;;
		byte[] b = new byte[512];
		int readPos=0;
		try
		{
			while(readPos<512)
			{
				//TODO:maybee we can let the thread sleep here for a while
				readPos+=this.m_istream.read(b,readPos,512-readPos);
			}
			int cid = ((b[0] & 0xFF)<<8) | (b[1] & 0xFF);
			int type = b[2] & 0xFF;
			LogHolder.log(LogLevel.DEBUG,LogType.MISC,"OnionProxy read() Tor Cell - Circuit: "+cid+" Type: "+type);
			byte[] a = new byte[509];
			System.arraycopy(b,3,a,0,509);
			switch(type)
			{
				case 2 :
				{
					cell = new CreatedCell(cid,a);
					break;
				}
				case 3 :
				{
					cell = new RelayCell(cid,a);
					break;
				}
				case 4 :
				{
					cell = new DestroyCell(cid,a);
					break;
				}
				default :
				{
					LogHolder.log(LogLevel.DEBUG,LogType.MISC,"Tor cell read - unbekannter zelltyp");
				}
			}
			Object o = this.m_circuits.get(new Integer(cid));
			if(o instanceof Circuit)
			{
				Circuit circ = (Circuit)o;
				circ.dispatchCell(cell);
			} else
			{
				//TODO : Fehler ausgeben
			}
		} catch(IOException ex)
		{
			//TODO : Fehler ausgeben
		}
	}

	/**
	 * connects to the FOR
	 * @throws Exception
	 */
	public void connect() throws Exception
	{
		this.m_tinyTLS = new TinyTLS(this.m_description.getAddress(),this.m_description.getPort());
		this.m_tinyTLS.startHandshake();
		this.m_istream = this.m_tinyTLS.getInputStream();
		this.m_ostream = this.m_tinyTLS.getOutputStream();
		this.m_circuits = new Hashtable();
	}

	/**
	 * starts the thread that reads from the inputstream and dispatches the cells
	 *
	 */
	public synchronized void start()
	{
		if(this.m_readDataLoop== null)
		{
			m_bRun=true;
			this.m_readDataLoop = new Thread(this);
			this.m_readDataLoop.start();
		}
	}

	/**
	 * dispatches cells while the thread, started with start is running
	 */
	public synchronized void run()
	{
		while(m_bRun)
		{
			this.dispatchCells();
		}
	}

	/**
	 * stops the thread that dispatches cells
	 * @throws IOException
	 */
	public void stop() throws IOException,InterruptedException
	{
		m_bRun=false;
		if(m_readDataLoop!=null)
		{
		m_readDataLoop.interrupt();
		m_readDataLoop.join();
		this.m_readDataLoop=null;
		this.close();
		}
	}

	/**
	 * closes the connection to the onionrouter
	 * @throws IOException
	 */
	public void close() throws IOException
	{
		this.m_tinyTLS.close();
	}

	/**
	 * adds a circuit to the FOR. its only possible to dispatch cells to here registered circuits
	 * @param circ
	 * a circuit
	 */
	public void addCircuit(Circuit circ)
	{
		this.m_circuits.put(new Integer(circ.getCircID()),circ);
	}

}
