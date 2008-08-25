package anon.transport.connection;

import java.io.Closeable;
import java.io.OutputStream;

/**
 * Durch einen {@link IChunkWriter} wird das Schreibende eines Datenkanals
 * beschrieben, über welchen Daten in Form von byte[] beliebiger Länge
 * verschickt werden können.
 * <p>
 * Konzeptionel handelt es sich um die chunk-basierte Entsprechung eines
 * {@link OutputStream} und erlauben die gesicherte Übertragung von mehreren
 * Bytes am Stück (Chunk), wobei davon ausgegangen werden kann, dass der
 * gesendete Chunk inhaltlich genauso empfangen wird. Die Zuordnung und die
 * Reihenfolge der einzelnen Bytes innerhalb eines Chunks wird durch die
 * übertragung nicht verändert.
 * <p>
 * Die Einspeisung in den Kanal sollte durch die Schreibmethode immer sofort
 * erfolgen, weshalb keine notwendigkeit für eine {@link OutputStream#flush()}
 * ähnliche Methode besteht.
 */
public interface IChunkWriter extends Closeable {

	/**
	 * Versucht den übergeben Chunk in den Kanal einzuspeisen und somit zum
	 * Empfänger zu übertragen.
	 * <p>
	 * Sofern der Kanal voll ist blockiert der Aufruf, bis es möglich war den
	 * Chunk zu übertragen oder eine entsprechende Ausnahme wird geworfen.
	 * 
	 * @param Der
	 *            zu übertragene Chunk
	 */
	void writeChunk(byte[] a_chunk) throws ConnectionException;

}
