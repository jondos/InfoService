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
 * Created on May 9, 2004
 */
package anon.tor;

import java.io.IOException;
import anon.server.impl.AbstractChannel;
import anon.tor.cells.RelayCell;
import anon.tor.util.helper;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/**
 * @author stefan
 */
public class TorChannel extends AbstractChannel
{

	private final static int MAX_CELL_DATA = 498;

	protected Circuit m_circuit;
	private int m_recvcellcounter;
	private int m_sendcellcounter;
	private Object m_oWaitForOpen;

	public TorChannel()
	{
		super();
		m_recvcellcounter = 500;
		m_sendcellcounter = 500;
		m_oWaitForOpen = new Object();
	}

	protected void setStreamID(int id)
	{
		m_id = id;
	}

	protected void setCircuit(Circuit c)
	{
		m_circuit = c;
	}

	public int getOutputBlockSize()
	{
		return MAX_CELL_DATA;
	}

	protected synchronized void send(byte[] arg0, int len) throws IOException
	{
		if (m_bIsClosed)
		{
			throw new IOException("Tor-Channel is closed");
		}
		byte[] b = arg0;
		RelayCell cell;
		while (len != 0)
		{
			if (len > MAX_CELL_DATA)
			{
				cell = new RelayCell(m_circuit.getCircID(), RelayCell.RELAY_DATA, m_id,
									 helper.copybytes(b, 0, MAX_CELL_DATA));
				b = helper.copybytes(b, MAX_CELL_DATA, len - MAX_CELL_DATA);
				len -= MAX_CELL_DATA;
			}
			else
			{
				cell = new RelayCell(m_circuit.getCircID(), RelayCell.RELAY_DATA, m_id,
									 helper.copybytes(b, 0, len));
				len = 0;
			}
			m_circuit.send(cell);
			m_sendcellcounter--;
			if (m_sendcellcounter < 10)
			{
				throw new IOException("Should never be here: Channel sendme <10");
			}

		}
	}

	public synchronized void close()
	{
		super.close();
		synchronized (m_oWaitForOpen)
		{
			m_oWaitForOpen.notify();
		}

	}

	public void closedByPeer()
	{
		super.closedByPeer();
		synchronized (m_oWaitForOpen)
		{
			m_oWaitForOpen.notify();
		}

	}

	protected void close_impl()
	{
		try
		{
			if (!m_bIsClosed)
			{
				m_circuit.close(m_id);
			}
		}
		catch (Exception e)
		{
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
	public boolean connect(String addr, int port)
	{
		try
		{
			if (m_bIsClosed || m_bIsClosedByPeer)
			{
				return false;
			}
			byte[] data = (addr + ":" + Integer.toString(port)).getBytes();
			data = helper.conc(data, new byte[1]);
			RelayCell cell = new RelayCell(m_circuit.getCircID(), RelayCell.RELAY_BEGIN, m_id, data);
			m_circuit.send(cell);
			synchronized (m_oWaitForOpen)
			{
				m_oWaitForOpen.wait();
			}
			if (m_bIsClosed || m_bIsClosedByPeer)
			{
				return false;
			}
			return true;
		}
		catch (Throwable t)
		{
			return false;
		}
	}

	/**
	 * dispatches the cells to the outputstream
	 * @param cell
	 * cell
	 */
	public void dispatchCell(RelayCell cell)
	{
		switch (cell.getRelayCommand())
		{
			case RelayCell.RELAY_CONNECTED:
			{
				synchronized (m_oWaitForOpen)
				{
					m_oWaitForOpen.notify();
				}
				break;
			}
			case RelayCell.RELAY_SENDME:
			{
				m_sendcellcounter += 50;
				break;
			}
			case RelayCell.RELAY_DATA:
			{
				m_recvcellcounter--;
				if (m_recvcellcounter < 250)
				{
					RelayCell rc = new RelayCell(m_circuit.getCircID(), RelayCell.RELAY_SENDME, m_id, null);
					try
					{
						m_circuit.send(rc);
					}
					catch (Throwable t)
					{
						closedByPeer();
						return;
					}
					m_recvcellcounter += 50;
				}
				try
				{
					byte[] buffer = cell.getRelayPayload();
					recv(buffer, 0, buffer.length);
				}
				catch (Exception ex)
				{
					closedByPeer();
					return;
				}
				break;
			}
			case RelayCell.RELAY_END:
			{
				LogHolder.log(LogLevel.DEBUG, LogType.TOR,
							  "RELAY_END: Relay stream closed with reason: " + cell.getRelayPayload()[0]);
				closedByPeer();
				break;
			}
			default:
			{
				closedByPeer();
			}
		}
	}
	
	public boolean isClosedByPeer()
	{
		return m_bIsClosedByPeer;
	}

}
