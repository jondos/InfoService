package anon.tor.cells;

/**
 * @author stefan
 *
 */

public abstract class Cell
{
	public final static int CELL_SIZE = 512;
	public final static int CELL_PAYLOAD_SIZE = 509;
	private int m_circID;
	private int m_command;
	protected byte[] m_payload;

	protected Cell(int command)
	{
		m_circID = 0;
		m_command = command;
		m_payload = new byte[CELL_PAYLOAD_SIZE];
	}

	protected Cell(int command, int circID)
	{
		this(command);
		this.m_circID = circID;
	}

	protected Cell(int command, int circID, byte[] payload)
	{
		this(command, circID);
		setPayload(payload, 0);
	}

	protected Cell(int command, int circID, byte[] payload, int offset)
	{
		this(command, circID);
		setPayload(payload, offset);
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
		byte[] buff = new byte[CELL_SIZE];
		buff[0] = (byte) ( (m_circID >> 8) & 0x00FF);
		buff[1] = (byte) ( (m_circID) & 0x00FF);
		buff[2] = (byte) ( (m_command) & 0x00FF);
		System.arraycopy(m_payload, 0, buff, 3, CELL_PAYLOAD_SIZE);
		return buff;
	}

	/**
	 * returns the command of the cell
	 */
	public int getCommand()
	{
		return m_command;
	}

	public int getCircuitID()
	{
		return m_circID;
	}

	public byte[] getPayload()
	{
		return m_payload;
	}

	public void setPayload(byte[] payload, int offset)
	{
		int len = Math.min(CELL_PAYLOAD_SIZE, payload.length);
		System.arraycopy(payload, offset, m_payload, 0, len);
	}

	public static Cell createCell(byte[] cellData)
	{
		if (cellData.length != CELL_SIZE)
		{
			return null;
		}
		Cell cell = null;
		int cid = ( (cellData[0] & 0xFF) << 8) | (cellData[1] & 0xFF);
		int type = cellData[2] & 0xFF;
		switch (type)
		{
			case 2:
			{
				cell = new CreatedCell(cid, cellData, 3);
				break;
			}
			case 3:
			{
				cell = new RelayCell(cid, cellData, 3);
				break;
			}
			case 4:
			{
				cell = new DestroyCell(cid, cellData, 3);
				break;
			}
			case 0:
			{
				cell = new PaddingCell(cid, cellData, 3);
				break;
			}

			default:
			{
				cell = null;
			}
		}
		return cell;
	}

}
