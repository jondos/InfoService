package anon.tor.cells;

/**
 * @author stefan
 *
 */
public class DestroyCell extends Cell
{

	/**
	 * Constructor for a destroy cell
	 */
	public DestroyCell()
	{
		super(4);
	}

	/**
	 * Constructor for a destroy cell
	 *
	 * @param circID
	 * circID
	 */
	public DestroyCell(int circID)
	{
		super(4, circID);
	}

	/**
	 * Constructor for a destroy cell
	 *
	 * @param circID
	 * circID
	 * @param payload
	 * payload
	 */
	public DestroyCell(int circID, byte[] payload, int offset)
	{
		super(4, circID, payload, offset);
	}

}
