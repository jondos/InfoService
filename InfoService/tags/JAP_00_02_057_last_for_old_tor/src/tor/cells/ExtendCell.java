package tor.cells;

/**
 * @author stefan
 *
 */
public class ExtendCell extends RelayCell
{

	/**
	 * Constructor for extend cell
	 */
	public ExtendCell()
	{
		super();
	}

	/**
	 * Constructor for extend cell
	 *
	 * @param circID
	 * circID
	 */
	public ExtendCell(int circID)
	{
		super(circID);
	}

	/**
	 * Constructor for extend cell
	 *
	 * @param circID
	 * circID
	 * @param payload
	 * payload
	 */
	public ExtendCell(int circID, byte[] payload)
	{
		super(circID, payload);
	}

}
