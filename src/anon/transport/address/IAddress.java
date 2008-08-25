package anon.transport.address;

/**
 * Über eine {@link IAddress} werden die Endpunkte einer
 * Kommunikationsbeziehung, ausgehend von der Bezeichnung des jeweiligen
 * Transportmediums ({@link #getTransportIdentifier()}) und der Liste der
 * notwendigen Parameter ({@link #getAllParameters()}), eindeutigt bestimmt.
 */
public interface IAddress {

	/**
	 * Liefert den Identifier des Transportmediums zurück.
	 * 
	 * @return Der Identifer des Transportmediums. Es muss dafür Sorge getragen
	 *         werden, das der Rückgabewert nie den Wert null annimmt. Im
	 *         Notfall sollte auf den leeren String zurückgegriffen werden.
	 */
	String getTransportIdentifier();

	/**
	 * Gibt eine Liste sämtlicher Parameter der Adresse zurück.
	 * 
	 * @return Die Liste aller Parameter der Adresse. Es muss dafür Sorge
	 *         getragen werden, das der Rückgabewert nie den Wert null annimmt.
	 *         Im Notfall sollte eine Array der Länge 0 zurückgegeben werden..
	 */
	AddressParameter[] getAllParameters();
}
