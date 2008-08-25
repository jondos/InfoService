package anon.transport.connection.util;

import java.io.IOException;
import java.io.InputStream;

import anon.transport.connection.IStreamConnection;


/**
 * Eine Implentierung von InputStream, welche immer geschlossen ist. Sämtliche
 * read-Methoden, werfen entsprechende Ausnahmen.
 * <p>
 * Diese Klasse ist zu Verwenden, wenn der Rückgabewert einer Methode einen
 * InputStream verlangt, aber aufgrund des inneren Zustandes des Objektes keine
 * geeignete Implentierung ausgewählt werden kann. Um in solchen Fällen dir
 * Rückgabe von null oder den Wurf einer Ausnahme zuvermeiden, sollte diese
 * Klasse verwendet werden.
 * <p>
 * So wird sie beispielsweise oft bei konkreten Implementierungen von
 * {@link IStreamConnection} verwendet, um bei Verbindungen, welche bereits
 * während der Initialisierung geschlossen sind, einen geeigneten Rückgabewert
 * für {@link IStreamConnection#getInputStream()} anzugeben.
 * 
 */
public class ClosedInputStream extends InputStream {

	/**
	 * Gibt an ob {@link #close()} mehrmals ohne den Wurf einer Ausnahme
	 * ausgerufen werden kann.
	 */
	private final boolean m_multibleClose;

	/**
	 * Singletonholder.
	 * 
	 * @see <a
	 *      href="http://en.wikipedia.org/wiki/Initialization_on_demand_holder_idiom">
	 *      On demand holder idiom</a>
	 */
	private static class Holder {
		private static InputStream neverCloseable = new ClosedInputStream(false);
		private static InputStream multibleCloseable = new ClosedInputStream(
				true);
	}

	/**
	 * Gibt den geschlossen {@link InputStream} zurück, welcher bei erneuten
	 * Schließen mittels {@link #close()} eine Ausnahme wirft.
	 */
	public static InputStream getNotCloseable() {
		return Holder.neverCloseable;
	}

	/**
	 * Gibt den geschlossenen {@link InputStream} zurück, welcher erneutes
	 * Schließen mittels {@link #close()} gestattet.
	 */
	public static InputStream getMultibleCloseable() {
		return Holder.multibleCloseable;
	}

	/**
	 * Erstellt einen geschlossen Eingabestrom, welcher bei erneuten schließen
	 * mittels {@link #close()} eine Ausnahme wirft.
	 */
	private ClosedInputStream() {
		m_multibleClose = false;
	}

	/**
	 * Erstellt einen bereits geschlossenen Eingabestrom.
	 * 
	 * @param a_multibleClose
	 *            Bestimmt ob der Versuch den Strom erneut zu schließen eine
	 *            Ausnahme verursacht.
	 */
	private ClosedInputStream(boolean a_multibleClose) {
		m_multibleClose = a_multibleClose;
	}

	//@Override
	public int read() throws IOException {
		throw new IOException("InputStream is closed");
	}

//	@Override
	public void close() throws IOException {
		if (m_multibleClose)
			return;
		throw new IOException("InputStream allready closed");
	}

}
