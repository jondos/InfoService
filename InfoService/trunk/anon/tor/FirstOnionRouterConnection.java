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
public class FirstOnionRouterConnection implements Runnable
{

	private TinyTLS m_tinyTLS;
	private ORDescription m_description;
	private Thread m_readDataLoop;
	private InputStream m_istream;
	private OutputStream m_ostream;
	private Hashtable m_Circuits;
	private volatile boolean m_bRun;

	/**
	 * constructor
	 *
	 * creates a FOR from the description
	 * @param d
	 * description of the onion router
	 */
	public FirstOnionRouterConnection(ORDescription d)
	{
		m_readDataLoop = null;
		m_description = d;
	}

	public ORDescription getORDescription()
	{
		return m_description;
	}

	/**
	 * sends a cell
	 * @param cell
	 * cell with data
	 * @throws IOException
	 */
	public synchronized void send(Cell cell) throws IOException
	{
		m_ostream.write(cell.getCellData());
	}

	/**
	 * dispatches a cell to the circuits if one is recieved
	 *
	 */
	private void dispatchCells()
	{
		Cell cell = null;
		byte[] buff = new byte[512];
		int readPos = 0;
		try
		{
			while (readPos < 512)
			{
				//TODO:maybe we can let the thread sleep here for a while
				readPos += m_istream.read(buff, readPos, 512 - readPos);
			}
			cell=Cell.createCell(buff);
			int cid=cell.getCircuitID();
			LogHolder.log(LogLevel.DEBUG, LogType.MISC,
						  "OnionProxy read() Tor Cell - Circuit: " + cid + " Type: " + cell.getCommand());
			Circuit circuit = (Circuit)m_Circuits.get(new Integer(cid));
			if(circuit!=null&&!circuit.isDestroyed())
				circuit.dispatchCell(cell);
			else
				m_Circuits.remove(new Integer(cid));
		}
		catch (IOException ex)
		{
			//TODO : Fehler ausgeben
		}
	}

	/**
	 * connects to the FOR
	 * @throws Exception
	 */
	public synchronized void connect() throws Exception
	{
		m_tinyTLS = new TinyTLS(this.m_description.getAddress(), this.m_description.getPort());
		m_tinyTLS.startHandshake();
		m_istream = this.m_tinyTLS.getInputStream();
		m_ostream = this.m_tinyTLS.getOutputStream();
		m_Circuits = new Hashtable();
		start();
	}

	/**
	 * starts the thread that reads from the inputstream and dispatches the cells
	 *
	 */
	private void start()
	{
		if (m_readDataLoop == null)
		{
			m_bRun = true;
			m_readDataLoop = new Thread(this,"FirstOnionRouterConnection - "+m_description.getName());
			m_readDataLoop.start();
		}
	}

	/**
	 * dispatches cells while the thread, started with start is running
	 */
	public void run()
	{
		while (m_bRun)
		{
			dispatchCells();
		}
	}

	/**
	 * stops the thread that dispatches cells
	 * @throws IOException
	 */
	private void stop() throws IOException
	{
		m_bRun = false;
		if (m_readDataLoop != null)
		{
			try
			{
				m_readDataLoop.interrupt();
				m_readDataLoop.join();
			}
			catch (Throwable t)
			{
			}
			m_readDataLoop = null;
		}
	}

	/**
	 * closes the connection to the onionrouter
	 *
	 */
	public synchronized void close()
	{
		try
		{
			stop();
			m_tinyTLS.close();
			m_Circuits.clear();
		}
		catch (Throwable t)
		{
		}
	}

	/**
	 * adds a circuit to the FOR. its only possible to dispatch cells to here registered circuits
	 * @param circ
	 * a circuit
	 */
	public synchronized void addCircuit(Circuit circ)
	{
		m_Circuits.put(new Integer(circ.getCircID()), circ);
	}

}
