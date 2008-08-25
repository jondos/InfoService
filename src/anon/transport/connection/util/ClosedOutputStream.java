package anon.transport.connection.util;

import java.io.IOException;
import java.io.OutputStream;

import anon.transport.connection.IStreamConnection;


/**
 * Eine Implentierung von OutputStream, welche immer geschlossen ist. Sämtliche
 * write-Methoden, werfen entsprechende Ausnahmen.
 * 
 * Diese Klasse ist zu Verwenden, wenn der Rückgabewert einer Methode einen
 * OutputStream verlangt, aber aufgrund des inneren Zustandes des Objektes keine
 * geeignete Implentierung ausgewählt werden kann. Um in solchen Fällen dir
 * Rückgabe von null oder den Wurf einer Ausnahme zuvermeiden, sollte diese
 * Klasse verwendet werden.
 * 
 * So wird sie beispielsweise oft bei konkreten Implementierungen von
 * {@link IStreamConnection} verwendet, um bei Verbindungen Verbindung, welche
 * bereits während der Initialisierung geschlossen sind, einen geeigneten
 * Rückgabewert für {@link IStreamConnection#getOutputStream()} anzugeben.
 */
public class ClosedOutputStream extends OutputStream {

	/**
	 * Gibt an ob {@link #close()} mehrmals ohne den Wurf einer Ausnahme
	 * ausgerufen werden kann.
	 */
	private final boolean m_multibleClose;

	/**
	 * Singletonholder. See
	 * http://en.wikipedia.org/wiki/Initialization_on_demand_holder_idiom.
	 */
	private static class Holder {
		private static OutputStream neverCloseable = new ClosedOutputStream(
				false);
		private static OutputStream multibleCloseable = new ClosedOutputStream(
				true);
	}

	/**
	 * Gibt den geschlossen {@link OutputStream} zurück, welcher bei erneuten
	 * Schließen mittels {@link #close()} eine Ausnahme wirft.
	 */
	public static OutputStream getNotCloseable() {
		return Holder.neverCloseable;
	}

	/**
	 * Gibt den geschlossenen {@link OutputStream} zurück, welcher erneutes
	 * Schließen mittels {@link #close()} gestattet.
	 */
	public static OutputStream getMultibleCloseable() {
		return Holder.multibleCloseable;
	}

	/**
	 * Erstellt einen geschlossen Eingabestrom, welcher bei erneuten schließen
	 * mittels {@link #close()} eine Ausnahme wirft.
	 */
	private ClosedOutputStream() {
		m_multibleClose = false;
	}

	/**
	 * Erstellt einen bereits geschlossenen Ausgabestrom.
	 * 
	 * @param a_multibleClose
	 *            Bestimmt ob der Versuch den Strom erneut zu schließen eine
	 *            Ausnahme verursacht.
	 */
	private ClosedOutputStream(boolean a_multibleClose) {
		m_multibleClose = a_multibleClose;
	}

//	@Override
	public void write(int b) throws IOException {
		throw new IOException("OutputStream is closed");
	}

//	@Override
	public void close() throws IOException {
		if (m_multibleClose)
			return;
		throw new IOException("InputStream allready closed");
	}

}
