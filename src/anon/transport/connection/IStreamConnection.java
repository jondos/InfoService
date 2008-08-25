package anon.transport.connection;

import java.io.InputStream;
import java.io.OutputStream;

/**
 * Eine Konkretisierung des {@link IConnection} Interface, bei welchem die
 * Übertragung über ein bidirektionales Datenstrommodell erfolgt.
 * 
 * Ein- und Ausgabestrom treten dabei ausschließlich als Paar auf und sind
 * fester Bestandteil der Verbindung.
 * 
 * Die einzelnen Ströme kapseln die eigentlichen Übertragung von Daten und
 * stellen eine Schnittstelle zum garantierten und konsitenten Transport bereit.
 */
public interface IStreamConnection extends IConnection {

	/**
	 * Liefert den Eingabestrom der Verbinundung, über welchen kontinuierlich
	 * Daten empfangen werden können.
	 * 
	 * Sofern sich der Zustand der Verbindung nicht ändert, sollte diese Methode
	 * immer das selbe Objekt zurückgeben.
	 */
	InputStream getInputStream();

	/**
	 * Liefert den Ausgabestrom der verbindung, über welchem Daten gesendet
	 * werden können.
	 * 
	 * Sofern sich der Zustand der Verbindung nicht ändert, sollte diese Methode
	 * immer das selbe Objekt zurückgeben.
	 */
	OutputStream getOutputStream();
}
