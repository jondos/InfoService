/*
 * Created on Jun 13, 2004
 *
 */
package anon.tor;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.util.Enumeration;
import java.util.Hashtable;
import anon.tor.cells.Cell;
import anon.tor.ordescription.ORDescription;
import anon.tor.tinytls.TinyTLS;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

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
	private boolean m_bIsClosed = true;

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
		m_bRun = false;
		m_bIsClosed = true;
		m_description = d;
	}

	public ORDescription getORDescription()
	{
		return m_description;
	}

	public synchronized boolean isClosed()
	{
		return m_bIsClosed;
	}

	/**
	 * sends a cell
	 * @param cell
	 * cell with data
	 * @throws IOException
	 */
	public synchronized void send(Cell cell) throws IOException
	{
		for (; ; )
		{
			try
			{
				m_ostream.write(cell.getCellData());
				m_ostream.flush();
				break;
			}
			catch (InterruptedIOException ex)
			{
			}
		}

	}

	/**
	 * dispatches a cell to the circuits if one is recieved
	 *
	 */
	private boolean dispatchCell(Cell cell)
	{
		try
		{
			int cid = cell.getCircuitID();
			LogHolder.log(LogLevel.DEBUG, LogType.MISC,
						  "OnionProxy read() Tor Cell - Circuit: " + cid + " Type: " + cell.getCommand());
			Circuit circuit = (Circuit) m_Circuits.get(new Integer(cid));
			if (circuit != null && !circuit.isDestroyed())
			{
				circuit.dispatchCell(cell);
			}
			else
			{
				m_Circuits.remove(new Integer(cid));
			}
			return true;
		}
		catch (Exception ex)
		{
			ex.printStackTrace();
			return false;
			//TODO : Fehler ausgeben
		}
	}

	/**
	 * connects to the FOR
	 * @throws Exception
	 */
	public synchronized void connect() throws Exception
	{
		m_tinyTLS = new TinyTLS(m_description.getAddress(), m_description.getPort());
		m_tinyTLS.setSoTimeout(0);
		m_tinyTLS.startHandshake();
		m_istream = m_tinyTLS.getInputStream();
		m_ostream = m_tinyTLS.getOutputStream();
		m_Circuits = new Hashtable();
		m_tinyTLS.setSoTimeout(1000);
		start();
		m_bIsClosed = false;
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
			m_readDataLoop = new Thread(this, "FirstOnionRouterConnection - " + m_description.getName());
			m_readDataLoop.start();
		}
	}

	/**
	 * dispatches cells while the thread, started with start is running
	 */
	public void run()
	{
		Cell cell = null;
		byte[] buff = new byte[512];
		int readPos = 0;

		while (m_bRun)
		{
			readPos = 0;
			while (readPos < 512 && m_bRun)
			{
				//TODO:maybe we can let the thread sleep here for a while
				int ret = 0;
				try
				{
					ret = m_istream.read(buff, readPos, 512 - readPos);
				}
				catch (InterruptedIOException ioe)
				{
					continue;
				}
				catch (IOException io)
				{
					break;
				}
				if (ret <= 0) //closed
				{
					break;
				}
				readPos += ret;
			}
			if (readPos != 512)
			{
				closedByPeer();
				return;
			}
			cell = Cell.createCell(buff);
			if (cell == null || !dispatchCell(cell))
			{
				closedByPeer();
				return;
			}
		}
	}

	/**
	 * stops the thread that dispatches cells
	 * @throws IOException
	 */
	private void stop() throws IOException
	{
		if (m_readDataLoop != null && m_bRun)
		{
			try
			{
				m_bRun = false;
				m_readDataLoop.interrupt();
				m_readDataLoop.join();
			}
			catch (Throwable t)
			{
			}
		}
		m_readDataLoop = null;
	}

	/**
	 * closes the connection to the onionrouter
	 *
	 */
	public synchronized void close()
	{
		try
		{
			if (!m_bIsClosed)
			{
				m_bIsClosed = true;
				stop();
				m_tinyTLS.close();
				m_Circuits.clear();
			}
		}
		catch (Throwable t)
		{
		}
	}

	/**
	 * connection was closed by peer
	 *
	 */
	public void closedByPeer()
	{
		if (m_bIsClosed)
		{
			return;
		}
		synchronized (this)
		{
			try
			{
				stop();
				m_tinyTLS.close();
				Enumeration enumer = m_Circuits.elements();
				while (enumer.hasMoreElements())
				{
					( (Circuit) enumer.nextElement()).destroyedByPeer();
				}
				m_Circuits.clear();
			}
			catch (Throwable t)
			{
			}
			m_bIsClosed = true;
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
