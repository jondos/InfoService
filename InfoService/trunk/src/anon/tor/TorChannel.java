/*
 * Created on May 9, 2004
 */
package anon.tor;

import java.io.IOException;
import java.net.ConnectException;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

import anon.tor.cells.RelayCell;
import anon.tor.util.helper;

import anon.server.impl.AbstractChannel;

/**
 * @author stefan
 */
public class TorChannel extends AbstractChannel
{

	private final static int MAX_CELL_DATA = 498;

	protected Circuit m_circuit;
	protected boolean m_opened;
	protected boolean m_error;
	private int m_recvcellcounter;
	private int m_sendcellcounter;
	private Object m_oWaitForOpen;

	/**
	 * constructor
	 * @param streamID
	 * streamID of the new channel
	 * @param circuit
	 * the circuit where this channel belongs to
	 * @throws IOException
	 */
	public TorChannel(int streamID, Circuit circuit) throws IOException
	{
		this();
		this.m_circuit = circuit;
		setStreamID(streamID);
	}

	public TorChannel() throws IOException
	{
		super();
		m_opened = false;
		m_error = false;
		m_recvcellcounter = 500;
		m_sendcellcounter = 500;
		m_oWaitForOpen = new Object();
	}

	protected void setStreamID(int id)
	{
		super.m_id = id;
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

	protected void close_impl()
	{
	}

	/**
	 * connects to a host over the tor-network
	 * @param addr
	 * address
	 * @param port
	 * port
	 * @throws ConnectException
	 */
	public synchronized void connect(String addr, int port) throws ConnectException
	{
		byte[] data = (addr + ":" + Integer.toString(port)).getBytes();
		data = helper.conc(data, new byte[1]);
		RelayCell cell = new RelayCell(m_circuit.getCircID(), RelayCell.RELAY_BEGIN, m_id, data);
		try
		{
			m_circuit.send(cell);
		}
		catch (Exception ex)
		{
			throw new ConnectException(ex.getLocalizedMessage());
		}
		while (!m_opened)
		{
			try
			{
				synchronized (m_oWaitForOpen)
				{
					m_oWaitForOpen.wait();
				}
			}
			catch (Exception e)
			{}
		}
		if (this.m_error)
		{
			throw new ConnectException("Cannot connect to " + addr + ":" + port);
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
				m_opened = true;
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
					close();
				}
				break;
			}
			case RelayCell.RELAY_END:
			{
				LogHolder.log(LogLevel.DEBUG, LogType.TOR,
							  "Relay stream closed with reason: " + cell.getRelayPayload()[0]);
			}
			default:
			{
				m_error = true;
				m_opened = false;
				closedByPeer();
			}
		}
	}
}
