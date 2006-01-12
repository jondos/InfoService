/*
 Copyright (c) 2000, The JAP-Team
 All rights reserved.
 Redistribution and use in source and binary forms, with or without modification,
 are permitted provided that the following conditions are met:

 - Redistributions of source code must retain the above copyright notice,
   this list of conditions and the following disclaimer.

 - Redistributions in binary form must reproduce the above copyright notice,
   this list of conditions and the following disclaimer in the documentation and/or
  other materials provided with the distribution.

 - Neither the name of the University of Technology Dresden, Germany nor the names of its contributors
   may be used to endorse or promote products derived from this software without specific
  prior written permission.


 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS ``AS IS'' AND ANY EXPRESS
 OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS
 BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE
 */
package infoservice;

import java.io.IOException;
import java.io.OutputStream;
import java.io.InterruptedIOException;

/** This class implements an OutputStream, where a timeout for the write() operations can be set.
 *
 */

final public class TimedOutputStream extends OutputStream
{
	private OutputStream m_Out;
	private long m_msTimeout;
	private volatile boolean m_bWriteStarted;
	private static ThreadPool ms_ThreadPool;
	final private class TimedOutputStreamInterrupt implements Runnable
	{
		private Thread m_ThisThread=null;
		private volatile boolean m_bRun = true;
		public void run()
		{
			m_ThisThread = Thread.currentThread();
			try
			{
				if (m_bRun)
				{
					Thread.sleep(m_msTimeout);
					m_ThisThread =null;
				}
				else
				{
					return;
				}
			}
			catch (InterruptedException ex)
			{
			}
			try
			{
				if (m_bWriteStarted)
				{
					m_bWriteStarted = false;
					m_Out.close();
				}
			}
			catch (Exception ex1)
			{
			}
		}

		public void interrupt()
		{
			m_bRun = false;
			if (m_ThisThread != null)
			{
				m_ThisThread.interrupt();
			}
		}
	}

	private TimedOutputStream()
	{
	}

	/** Calls this with an resonable value for nrOfthreads. This number tells the class how many concurrent write() operations are possible
	 *
	 */
	public static void init(int nrOfThreads)
	{
		ms_ThreadPool = new ThreadPool("TimedOutputStream",nrOfThreads);
	}

	/**
	 *
	 * @param parent OutputStream the outputstrem which will be used for I/O operations
	 * @param msTimeout long the timeout in milli seconds for the write operations (zero means blocking I/O)
	 */
	public TimedOutputStream(OutputStream parent, long msTimeout)
	{
		m_Out = parent;
		m_msTimeout = msTimeout;
	}

	/**
	 * Writes the specified byte to this output stream.
	 *
	 * @param b the <code>byte</code>.
	 * @throws IOException if an I/O error occurs. In particular, an
	 *   <code>IOException</code> may be thrown if the output stream has
	 *   been closed.
	 * @todo Diese java.io.OutputStream-Methode implementieren
	 */
	public void write(int b) throws IOException
	{
		TimedOutputStreamInterrupt t = new TimedOutputStreamInterrupt();
		ms_ThreadPool.addRequest(t);
		m_bWriteStarted = true;
		try
		{
			m_Out.write(b);
		}
		catch (IOException e)
		{
			if (!m_bWriteStarted) //I/O was interrupted
			{
				throw new InterruptedIOException("TimedOutputStream: write() timed out!");
			}
			else
			{
				m_bWriteStarted = false;
				t.interrupt();
				throw e;
			}
		}
		m_bWriteStarted = false;
		t.interrupt();
	}

	public void write(byte[] b) throws IOException
	{
		write(b, 0, b.length);
	}

	public void write(byte[] b, int i1, int i2) throws IOException
	{
		TimedOutputStreamInterrupt t = new TimedOutputStreamInterrupt();
		//ms_ThreadPool.addRequest(t);
		m_bWriteStarted = true;
		try
		{
			m_Out.write(b, i1, i2);
		}
		catch (IOException e)
		{
			if (!m_bWriteStarted) //I/O was interrupted
			{
				throw new InterruptedIOException("TimedOutputStream: write() timed out!");
			}
			else
			{
				m_bWriteStarted = false;
				t.interrupt();
				throw e;
			}
		}
		m_bWriteStarted = false;
		t.interrupt();
	}

	public void close() throws IOException
	{
		m_Out.close();
	}

	public void flush() throws IOException
	{
		m_Out.flush();
	}

}
