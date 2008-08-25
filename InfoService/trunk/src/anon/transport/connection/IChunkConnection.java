package anon.transport.connection;

/**
 * Konkretisierung von {@link IConnection}, bei welchem die Übertragung von
 * Daten über bidirektional und stoßweise in Form von Datenblocken beliebiger
 * Länge (chunks) erfolgt.
 * <p>
 * Aufgabe der {@link IChunkConnection}, sowie der mit ihr verbunden Reader und
 * Writer, ist es dabei die Reihenfolge und Unversehrtheit der Datenblöcke zu
 * garantieren.
 * <p>
 * Die jeweiligen Reader und Writer treten dabei immer als Paar auf und sind
 * fest mit der Verbindung verknüpft. Entsprechend sollten
 * {@link #getChunkReader()} und {@link #getChunkWriter()} solange die selben
 * Objekte zurückliefern, bis sich der Zustand der Verbindung ändert.
 */
public interface IChunkConnection extends IConnection {

	/**
	 * Liefert den {@link IChunkReader}, über welchen gesendete Datenblöcke des
	 * Kommunikationspartners gelesen werden können.
	 */
	IChunkReader getChunkReader();

	/**
	 * Liefert den {@link IChunkWriter}, über welchen Datenblöcke zum
	 * Kommunikationspartner gesendet werden können.
	 */
	IChunkWriter getChunkWriter();

}
