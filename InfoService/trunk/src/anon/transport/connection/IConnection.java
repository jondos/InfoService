package anon.transport.connection;

import java.io.Closeable;
import java.net.Socket;

import anon.transport.address.IAddress;


/**
 * Allgemeine Beschreibung einer Verbindung zwischen zwei Endpunkten und der
 * zugehörenden Primitive.
 * 
 * Dieses Interface ist als "abstract" anzusehen, in dem Sinne, das die
 * fundamentalen Primitive zum Empfang und der Übertragung von Daten erst in
 * aufbauenden Interfaces konkretisiert werden.
 */
public interface IConnection extends Closeable {

	/**
	 * Aufzählung der möglichen Zustände einer Verbindung
	 */
	//enum ConnectionState {
		/**
		 * Beschreibt den Zustand, in welchem eine Verbindung als offen
		 * angesehen werden kann und zur Übertragung, sowie dem Empfang von
		 * Daten zur Verfügung steht.
		 */
		public final static int ConnectionState_OPEN=1;

		/**
		 * Beschreibt den Zustand, in welchem eine Verbinung als geschlossen
		 * anzusehen ist und somit keine Übertragung von Daten möglich ist.
		 * 
		 * Der geschlossen Zustand ist der finale Zustand einer Verbindung und
		 * kann nach erreichen nicht mehr gewächselt werden.
		 */
		public final static int ConnectionState_CLOSE=2;
	//}

	/**
	 * Setzt den Timeout in Millisekunden für die Lese- und Schreiboperationen
	 * der Verbindung. Der Definition von {@link Socket} folgend, bestimmt 0 ein
	 * unendliches Timeout.
	 * 
	 * @throws ConnectionException
	 *             Wird geworfen, wenn das setzen des TimeOut nicht mögliche
	 *             war. Insbesondere kann durch eine
	 *             {@link UnsuportedCommandException} Ausnahme angegben werde,
	 *             das das setzen generell nicht möglich ist.
	 */
	void setTimeout(int value) throws ConnectionException;

	/**
	 * Ermittelt den momentanen Wert für das Timeout in Millisekunden für Lese-
	 * und Schreiboperationen der Verbindung. Der Definition von {@link Socket}
	 * folgend, bestimmt 0 ein unendliches Timeout.
	 * 
	 * @throws ConnectionException
	 *             Wird geworfen, wenn der Wert nicht ermittelt werden kann.
	 */
	int getTimeout() throws ConnectionException;

	/**
	 * Gibt die Adresse des lokalen Endpunkt der Verbindung an.
	 * <p>
	 * Für Implentierungen, welche eine Bereits eingerichte Verbindung kapsel um
	 * weitere Funktionalität bereitzustellen, sollte diese Methode die Adresse
	 * der drunterlegenen Verbindung zurückgeben.
	 */
	IAddress getLocalAddress();

	/**
	 * Gibt die Adresse des entfernten Endpunkt an.
	 * <p>
	 * Für Implentierungen, welche eine Bereits eingerichte Verbindung kapsel um
	 * weitere Funktionalität bereitzustellen, sollte diese Methode die Adresse
	 * der drunterlegenen Verbindung zurückgeben.
	 */
	IAddress getRemoteAddress();

	/**
	 * Gibt den momentanen Zustand der Verbindung an.
	 * <p>
	 * Die Belegung sollte dabei den in {@link ConnectionState} definierten
	 * Konstanten folgen.
	 */
	int getCurrentState();
}
