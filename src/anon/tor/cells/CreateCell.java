package anon.tor.cells;

/**
 * @author stefan
 *
 */
public class CreateCell extends Cell
{

	/**
	 * Constructor for a create cell
	 */
	public CreateCell()
	{
		super(1);
	}

	/**
	 * Constructor for a create cell
	 *
	 * @param circID
	 * circID
	 */
	public CreateCell(int circID)
	{
		super(1, circID);
	}

	/**
	 * Constructor for a create cell
	 *
	 * @param circID
	 * circID
	 * @param payload
	 * payload
	 */
	public CreateCell(int circID, byte[] payload)
	{
		super(1, circID, payload);
	}

}
