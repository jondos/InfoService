package anon.tor.cells;

/**
 * @author stefan
 *
 */
public class CreatedCell extends Cell
{

	/**
	 * Constructor for a created cell
	 */
	public CreatedCell()
	{
		super(2);
	}

	/**
	 * Constructor for created cell
	 *
	 * @param circID
	 * circID
	 */
	public CreatedCell(int circID)
	{
		super(2, circID);
	}

	/**
	 * Constructor for a created cell
	 *
	 * @param circID
	 * circID
	 * @param payload
	 * payload
	 */
	public CreatedCell(int circID, byte[] payload)
	{
		super(2, circID, payload);
	}

	public CreatedCell(int circID, byte[] payload, int offset)
	{
		super(2, circID, payload, offset);
	}

}
