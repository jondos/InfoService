/*
 Copyright (c) 2004, The JAP-Team
 All rights reserved.
 Redistribution and use in source and binary forms, with or without modification,
 are permitted provided that the following conditions are met:

 - Redistributions of source code must retain the above copyright notice,
   this list of conditions and the following disclaimer.

 - Redistributions in binary form must reproduce the above copyright notice,
   this list of conditions and the following disclaimer in the documentation and/or
  other materials provided with the distribution.

 - Neither the name of the University of Technology Dresden, Germany nor the names of its contributors
   may be used to endorse or promote products derived from this software without specific
  prior written permission.


 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS ``AS IS'' AND ANY EXPRESS
 OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS
 BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE
 */
/*
 * Created on Jun 13, 2004
 *
 */
package anon.tor;

import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.security.SecureRandom;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import anon.tor.cells.Cell;
import anon.tor.ordescription.ORDescription;
import anon.tor.tinytls.TinyTLS;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import anon.crypto.*;

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
	private MyRandom m_rand;
	private Object m_oSendSync;
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
		m_rand = new MyRandom(new SecureRandom());
		m_oSendSync = new Object();
	}

	public ORDescription getORDescription()
	{
		return m_description;
	}

	public boolean isClosed()
	{
		return m_bIsClosed;
	}

	/**
	 * sends a cell
	 * @param cell
	 * cell with data
	 * @throws IOException
	 */
	public void send(Cell cell) throws IOException
	{
		synchronized (m_oSendSync)
		{
			for (; ; )
			{
				try
				{
					m_ostream.write(cell.getCellData());
					m_ostream.flush();
					LogHolder.log(LogLevel.DEBUG, LogType.MISC,
								  "OnionConnection " + m_description.getName() + "Send a cell");
					break;
				}
				catch (InterruptedIOException ex)
				{
				}
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
			if (circuit != null)
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

	public synchronized Circuit createCircuit(Vector onionRouters)
	{
		int circid = 0;
		try
		{
			do
			{
				circid = m_rand.nextInt(65535);
			}
			while (m_Circuits.containsKey(new Integer(circid)) && (circid != 0));
			Circuit circ = new Circuit(circid, this, onionRouters);
			m_Circuits.put(new Integer(circid), circ);
			circ.create();
			return circ;
		}
		catch (Exception e)
		{
			m_Circuits.remove(new Integer(circid));
			return null;
		}
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
			LogHolder.log(LogLevel.DEBUG, LogType.TOR,
						  "OnionConnection " + m_description.getName() + " received a Cell!");
			cell = Cell.createCell(buff);
			if (cell == null)
			{
				LogHolder.log(LogLevel.EMERG, LogType.TOR,
							  "OnionConnection " + m_description.getName() + " dont know about this Cell!");
			}
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
				Enumeration enumer = m_Circuits.elements();
				while (enumer.hasMoreElements())
				{
					( (Circuit) enumer.nextElement()).close();
				}
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
	private void closedByPeer()
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

	protected void notifyCircuitClosed(Circuit circ)
	{
		m_Circuits.remove(new Integer(circ.getCircID()));
	}

}
