package anon.tor.cells;

/**
 * @author stefan
 *
 */
public class ExtendedCell extends RelayCell
{

	/**
	 * Constructor for extended cell
	 */
	public ExtendedCell()
	{
		super();
	}

	/**
	 * Constructor for extended cell
	 *
	 * @param circID
	 * circID
	 */
	public ExtendedCell(int circID)
	{
		super(circID);
	}

	/**
	 * Constructor for extended cell
	 *
	 * @param circID
	 * circID
	 * @param payload
	 * payload
	 */
	public ExtendedCell(int circID, byte[] payload)
	{
		super(circID, payload);
	}

}
