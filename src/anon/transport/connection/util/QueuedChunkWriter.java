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
import anon.transport.connection.IChunkWriter;


/**
 * Implementierung eines {@link IChunkWriter}, welcher die zu senden Chunks
 * nacheinander in eine Eingangs übergebenen {@link BlockingQueue} einfügt.
 */
public class QueuedChunkWriter implements IChunkWriter {

	/** Die BlockingQueue in welche Chunks eingefügt werden. */
	private final BlockingQueue/*<byte[]>*/ m_writingQueue;

	/** Gibt an ob der Writer geschlossen ist. */
	private final AtomicBoolean m_isClosed;

	/**
	 * Sammelt alle Threats, welche sich innerhalb der write() Methode befinden.
	 * <p>
	 * Dient dazu, beim Schließen des Writers evtl. blockierte Threats mittelst
	 * {@link Thread#interrupt()} aufzuwecken.
	 */
	private final /*Collection<Thread>*/ Vector m_waitingThreads;

	/** Das Timeout für Schreiboperationen in Millisekunden */
	private int m_timeout;

	/**
	 * Erstellt einen neuen {@link QueuedChunkWriter} auf Grundlage der
	 * übergebene Queue und dem entsprechenden Timeout.
	 * 
	 * @param a_writingQueue
	 *            Die Queue in welche die Chunks eingefügt werden.
	 * @param a_timeout
	 *            Der initiale Wert für das Timeout der Schreiboperationen. Ein
	 *            Wert von 0 bestimmt ein unendliches Timeout.
	 */
	public QueuedChunkWriter(BlockingQueue/*<byte[]>*/ a_writingQueue, int a_timeout) {
		m_writingQueue = a_writingQueue;
		m_isClosed = new AtomicBoolean(false);
		m_waitingThreads = new Vector();//LinkedList<Thread>();
		m_timeout = a_timeout;
	}

	/**
	 * Erstellt einen neuen {@link QueuedChunkWriter} auf Grundlage der
	 * übergebene Queue mit unendlichen Timeout.
	 * 
	 * @param a_writingQueue
	 *            Die Queue in welche die Chunks eingefügt werden.
	 */
	public QueuedChunkWriter(BlockingQueue/*<byte[]>*/ a_readingQueue) {
		m_writingQueue = a_readingQueue;
		m_isClosed = new AtomicBoolean(false);
		m_waitingThreads = new Vector();//LinkedList<Thread>();
		m_timeout = 0;
	}

	public int getTimeout() {
		return m_timeout;
	}

	public void setTimeout(int a_value) {
		m_timeout = a_value;
	}

	public void writeChunk(byte[] a_chunk) throws ConnectionException {
		Thread caller = Thread.currentThread();
		try {
			// save caller for interrupting when closed
			m_waitingThreads.add(caller);
			if (m_isClosed.get())
				throw new ConnectionException("Reader allready closed");
			// after the previous step, we assume that the writer is open.
			// it could close until now, but we will
			// get an interruptedException when this happens
			if (m_timeout > 0)
				if (!m_writingQueue.offer(a_chunk, m_timeout,
						TimeUnit.MILLISECONDS))
					throw new ConnectionException("Timeout elapsed");
			m_writingQueue.put(a_chunk);
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
//			assert removed : "Unable to remove caller Thread";
		}

	}

	public void close() throws IOException {
		if (m_isClosed.getAndSet(true))
			return; // nothing more to do
		// wake up all waiting threads
		Enumeration allthreads=m_waitingThreads.elements();
		while (allthreads.hasMoreElements())
			((Thread)(allthreads.nextElement())).interrupt();
	}

}
