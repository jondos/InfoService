/*
 * Created on Jun 13, 2004
 *
 */
package tor;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Hashtable;
import java.util.Vector;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

import tor.cells.Cell;
import tor.cells.CreatedCell;
import tor.cells.DestroyCell;
import tor.cells.RelayCell;
import tor.ordescription.ORDescription;
import tor.tinytls.TinyTLS;

/**
 *
 */
public class FirstOnionRouter implements Runnable {

	private TinyTLS tinyTLS;
	private ORDescription description;
	private Thread readDataLoop; 
	private InputStream istream;
	private OutputStream ostream;
	private Hashtable circuits;

	/**
	 * constructor
	 * 
	 * creates a FOR from the description
	 * @param d
	 * description of the onion router
	 */
	public FirstOnionRouter(ORDescription d)
	{
		this.readDataLoop = null;
		this.description = d;
	}

	/**
	 * sends a cell
	 * @param cell
	 * cell with data
	 * @throws IOException
	 */
	public void send(Cell cell) throws IOException
	{
		this.ostream.write(cell.getCellData());
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
				readPos+=this.istream.read(b,readPos,512-readPos);
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
			Object o = this.circuits.get(new Integer(cid));
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
		this.tinyTLS = new TinyTLS(this.description.getAddress(),this.description.getPort());
		this.tinyTLS.startHandshake();
		this.istream = this.tinyTLS.getInputStream();
		this.ostream = this.tinyTLS.getOutputStream();
		this.circuits = new Hashtable();
	}
	
	/**
	 * starts the thread that reads from the inputstream and dispatches the cells
	 *
	 */
	public synchronized void start()
	{
		if(this.readDataLoop== null)
		{		
			this.readDataLoop = new Thread(this);
			this.readDataLoop.start();
		}
	}
	
	/**
	 * dispatches cells while the thread, started with start is running
	 */
	public synchronized void run()
	{
		while(this.readDataLoop!=null)
		{
			this.dispatchCells();
		}
	}
	
	/**
	 * stops the thread that dispatches cells
	 * @throws IOException
	 */
	public void stop() throws IOException
	{
		this.readDataLoop.stop();
		this.close();
		this.readDataLoop=null;
	}
	
	/**
	 * closes the connection to the onionrouter
	 * @throws IOException
	 */
	public void close() throws IOException
	{
		this.tinyTLS.close();
	}

	/**
	 * adds a circuit to the FOR. its only possible to dispatch cells to here registered circuits
	 * @param circ
	 * a circuit
	 */
	public void addCircuit(Circuit circ)
	{
		this.circuits.put(new Integer(circ.getCircID()),circ);
	}

}
