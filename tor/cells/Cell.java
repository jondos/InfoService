package tor.cells;

import tor.util.helper;

/**
 * @author stefan
 *
 */

public abstract class Cell {
	
	private int circID;
	private int command;
	protected byte[] payload;
	
	protected Cell(int command) {
		this.circID = 0;
		this.command = command;
		this.payload = new byte[509];
	}

	protected Cell(int command,int circID) {
		this(command);
		this.circID = circID;
	}

	protected Cell(int command,int circID,byte[] payload) {
		this(command,circID);
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
	public byte[] getCellData() {
		return helper.conc(helper.inttobyte(this.circID,2),helper.conc(helper.inttobyte(this.command,1),this.payload));
	}
	
	/**
	 * returns the command of the cell
	 */
	public int getCommand() {
		return this.command;
	}

	public byte[] getPayload()
	{
		return this.payload;
	}
	
	public void setPayload(byte[] payload)
	{
		if(payload.length<509)
		{
			this.payload = helper.conc(payload,(new byte[509-payload.length]));
		} else
		{
			this.payload = helper.copybytes(payload,0,509);
		}
	}


}
