package anon.transport.connection;

import java.io.Closeable;
import java.io.InputStream;

/**
 * Durch einen {@link IChunkReader} wird das lesende Ende eines Datenkanals
 * beschrieben, ueber welchen Daten in Form von byte[] beliebiger Länge
 * verschickt werden koennen.
 * <p>
 * Konzeptionel handelt es sich um die chunk-basierte Entsprechung eines
 * {@link InputStream} und erlauben den gesicherte Empfang von mehreren Bytes am
 * Stueck (Chunk), wobei davon ausgegangen werden kann, dass der empfangen Chunk
 * inhaltlich genauso gesendet wurde. Die Zuordnung und die Reihenfolge der
 * einzelnen Bytes innerhalb eines Chunks wird durch die uebertragung nicht
 * verändert.
 */
public interface IChunkReader extends Closeable {

	/**
	 * Gibt den ältesten (im Sinne des Einfuegens in den Kanal) der im Kanal
	 * befindlichen Chunks zurueck. Sofern der Kanal leer ist blockiert der
	 * Aufruf bis ein Chunk ausgeliefert werden kann.
	 * <p>
	 * Evtl. Fehler oder unzulässige Zustände des Kanals koennen durch
	 * entsprechende Ausnahmen angezeigt werden.
	 * 
	 * @return Den ältesten Chunk innerhalb des Kanals. Ein Rueckgabewert von
	 *         <code>null</code> ist nicht zulässig und ein Rueckgabewert von
	 *         <code>byte[0]</code> sollte vermieden werden.
	 */
	byte[] readChunk() throws ConnectionException;

	/**
	 * Gibt aufschluss darueber, wieviele Chunks gelesen werden koennen, ohne das
	 * der Aufruf von {@link #readChunk()} blockiert.
	 * <p>
	 * Der Wert dieser Methode sollte nicht schrumpfen, sofern kein lesender
	 * Zugriff auf den Kanal erfolgt.
	 */
	int availableChunks() throws ConnectionException;
}
