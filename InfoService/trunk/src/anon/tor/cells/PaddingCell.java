package anon.tor.cells;

/**
 * @author stefan
 *
 */
public class PaddingCell extends Cell
{

	/**
	 * Constructor for a padding cell
	 */
	public PaddingCell()
	{
		super(0);
	}

	/**
	 * Constructor for a padding cell
	 *
	 * @param circID
	 * circID
	 */
	public PaddingCell(int circID)
	{
		super(0, circID);
	}

	/**
	 * Constructor for a padding cell
	 *
	 * @param circID
	 * circID
	 * @param payload
	 * payload
	 */
	public PaddingCell(int circID, byte[] payload)
	{
		super(0, circID, payload);
	}

}
