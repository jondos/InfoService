package anon.transport.connection.util;

import java.io.IOException;
import java.util.Collection;
import java.util.Enumeration;
import java.util.LinkedList;
import java.util.Vector;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import anon.transport.connection.ConnectionException;
import anon.transport.connection.IChunkReader;


/**
 * Implementierung eines {@link IChunkReader}, welcher die auszugebenen Chunks
 * nacheinander aus einer Eingangs uebergebenen {@link BlockingQueue} bezieht.
 */
public class QueuedChunkReader implements IChunkReader {

	/** Die BlockingQueue uas welcher chunks gelesen werden. */
	private final BlockingQueue/*<byte[]>*/ m_readingQueue;

	/** Gibt an ob der Reader geschlossen ist. */
	private final AtomicBoolean m_isClosed;

	/**
	 * Sammelt alle Threats, welche sich innerhalb der read() Methode befinden.
	 * <p>
	 * Dient dazu um beim Schließen des Reader evtl. blockierte Threats mittelst
	 * {@link Thread#interrupt()} aufzuwecken.
	 */
	private final Vector /*Collection<Thread>*/ m_waitingThreads;

	/** Das Timeout fuer Leseoperationen in millisekunden */
	private int m_timeout;

	/**
	 * Gibt an ob der Reader heruntergefahren werden soll.
	 * <p>
	 * Dies erlaubt das Schließen des Reader solange zu verzoegern, bis keine
	 * ausstehenden Chunks mehr vorhanden sind.
	 */
	private boolean m_isTearDown = false; // per default we are running

	/**
	 * Erstellt einen neuen {@link QueuedChunkReader} auf Grundlage der
	 * uebergebene Queue und dem entsprechenden Timeout.
	 * 
	 * @param a_readingQueue
	 *            Die Queue aus welcher die Chunks gelesen werden.
	 * @param a_timeout
	 *            Der initiale Wert fuer das Timeout der Leseoperationen. Ein
	 *            Wert von 0 bestimmt ein unendliches Timeout.
	 */
	public QueuedChunkReader(BlockingQueue/*<byte[]>*/ a_readingQueue, int a_timeout) {
		m_readingQueue = a_readingQueue;
		m_isClosed = new AtomicBoolean(false);
		m_waitingThreads = new Vector();//LinkedList<Thread>();
		m_timeout = a_timeout;
	}

	/**
	 * Erstellt einen neuen {@link QueuedChunkReader} auf Grundlage der
	 * uebergebene Queue mit einen unendlichen Timeout.
	 * 
	 * @param a_readingQueue
	 *            Die Queue aus welcher die Chunks gelesen werden.
	 */
	public QueuedChunkReader(BlockingQueue/*<byte[]>*/ a_readingQueue) {
		m_readingQueue = a_readingQueue;
		m_isClosed = new AtomicBoolean(false);
		m_waitingThreads = new Vector();//new LinkedList<Thread>();
		m_timeout = 0;
	}

	/** Liefert den aktuelle Wert des Timeout */
	public int getTimeout() {
		return m_timeout;
	}

	/** Setzt den Wert fuer das Timeout der Leseoperationen */
	public void setTimeout(int a_value) {
		m_timeout = a_value;
	}

	public byte[] readChunk() throws ConnectionException {
		Thread caller = Thread.currentThread();
		try {
			// save caller for interrupting when closed
			m_waitingThreads.add(caller);
			if (m_isClosed.get())
				throw new ConnectionException("Reader allready closed");
			// after the previous step, we assume that the reader is open.
			// it could close till we reach the return, but we will
			// get an interruptedException when this happens
			if (m_timeout > 0) {
				byte[] result = (byte[])m_readingQueue.poll(m_timeout,
						TimeUnit.MILLISECONDS);
				if (result != null)
					return result;
				throw new ConnectionException("Timeout elapsed");
			}
			return (byte[])m_readingQueue.take();
		} catch (InterruptedException e) {
			throw new ConnectionException(
					"Innterupted while reading. Probaly closed Reader.");
		} finally {
			// the finally is our save block, as its guarantees the removing of
			// the
			// thread from the list.
			// every thread can only ones enter this method and should
			// always be there.
			boolean removed = m_waitingThreads.remove(caller);
			//assert removed : "Unable to remove caller Thread";
			// should we also close the reader?
			if (m_isTearDown && m_readingQueue.isEmpty())
				try {
					close();
				} catch (IOException e) {
					// will never happen as close throws no exceptions
				}
		}
	}

	/**
	 * Schließt den Reader ungeachtet noch ausstehender Chunks innerhalb der
	 * Queue.
	 * <p>
	 * Wenn gewuenscht wird mit dem Schließen bis zur Auslieferung dieser Chunks
	 * zu warten sollte {@link #tearDown()} gewählt werden.
	 */
	public void close() throws IOException {
		if (m_isClosed.getAndSet(true))
			return; // nothing more to do
		// wake up all waiting threads
		Enumeration allthreads=m_waitingThreads.elements();
		//for (Thread thread : m_waitingThreads)
		while(allthreads.hasMoreElements())
			((Thread)(allthreads.nextElement())).interrupt();
	}

	/**
	 * Fährt den Reader runter.
	 * <p>
	 * Das heißt bis zum Erreichen einer leeren Queue bleibt der Reader offen.
	 * Danach wird er automatisch geschlossen.
	 */
	public void tearDown() throws IOException {
		m_isTearDown = true;
	}

	/**
	 * Liefert die Anzahl der Chunks innerhalb der Queue.
	 */
	public int availableChunks() throws ConnectionException {
//		int result = 0;
//		for (byte[] chunk : m_readingQueue)
//			++result;
//		return result;
		return m_readingQueue.size();
	}

}
