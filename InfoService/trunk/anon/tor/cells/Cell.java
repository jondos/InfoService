package anon.tor.cells;

import anon.tor.util.helper;

/**
 * @author stefan
 *
 */

public abstract class Cell
{

	private int m_circID;
	private int m_command;
	protected byte[] m_payload;

	protected Cell(int command)
	{
		this.m_circID = 0;
		this.m_command = command;
		this.m_payload = new byte[509];
	}

	protected Cell(int command, int circID)
	{
		this(command);
		this.m_circID = circID;
	}

	protected Cell(int command, int circID, byte[] payload)
	{
		this(command, circID);
		this.setPayload(payload);
	}

	/**
	 * creates a fixed-sized cell<br>
	 * <br>
	 * 2   bytes - circID<br>
	 * 1   byte  - command<br>
	 * 509 bytes - payload
	 *
	 * @return
	 * composed cell
	 */
	public byte[] getCellData()
	{
		return helper.conc(helper.inttobyte(this.m_circID, 2),
						   helper.conc(helper.inttobyte(this.m_command, 1), this.m_payload));
	}

	/**
	 * returns the command of the cell
	 */
	public int getCommand()
	{
		return this.m_command;
	}

	public byte[] getPayload()
	{
		return this.m_payload;
	}

	public void setPayload(byte[] payload)
	{
		if (payload.length < 509)
		{
			this.m_payload = helper.conc(payload, (new byte[509 - payload.length]));
		}
		else
		{
			this.m_payload = helper.copybytes(payload, 0, 509);
		}
	}

}
