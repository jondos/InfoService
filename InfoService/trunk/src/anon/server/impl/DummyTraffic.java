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
package anon.server.impl;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/**
 * This is the implementation for the dummy traffic interval timeout.
 */
final class DummyTraffic implements Runnable
{
	/**
	 * Stores the parent MuxSocket, this DummyTraffic instance belongs to.
	 */
	private MuxSocket m_muxSocket;

	/**
	 * Stores whether the internal thread shall work (true) or come to the end (false).
	 */
	private volatile boolean m_bRun;

	/**
	 * Stores the instance of the internal dummy traffic thread.
	 */
	private Thread m_threadRunLoop;

	/**
	 * Stores the dummy traffic interval in milliseconds. After that interval of inactivity (no
	 * traffic) on the connection, a dummy packet is sent.
	 */
	private long m_interval;

	/**
	 * Creates a new DummyTraffic instance. The dummy traffic interval is set to -1 (dummy traffic
	 * disabled and the internal thread isn't started.
	 *
	 * a_muxSocket The MuxSocket the new DummyTraffic instance belongs to.
	 */
	public DummyTraffic(MuxSocket a_muxSocket)
	{
		m_muxSocket = a_muxSocket;
		m_bRun = false;
		m_threadRunLoop = null;
		m_interval = -1;
	}

	/**
	 * This is the implementation for the dummy traffic thread.
	 */
	public void run()
	{
		while (m_bRun)
		{
			try
			{
				Thread.sleep(m_interval);
				/* if we reach the timeout without interruption, we have to send a dummy */
				LogHolder.log(LogLevel.DEBUG, LogType.NET, "Sending Dummy!");
				m_muxSocket.sendDummy();
			}
			catch (Exception e)
			{
				/* if we got an interruption within the timeout, everything is ok */
			}
		}
	}

	/**
	 * Holds the internal dummy traffic thread. This method blocks until the internal thread has
	 * come to the end.
	 */
	public void stop()
	{
		synchronized (this)
		{
			m_bRun = false;
			if (m_threadRunLoop != null)
			{
				m_threadRunLoop.interrupt();
				try
				{
					m_threadRunLoop.join();
				}
				catch (Exception e)
				{
				}
				m_threadRunLoop = null;
			}
		}
	}

	/**
	 * This resets the dummy traffic interval. If a packet is sent or received on the connection,
	 * the dummy traffic timer can be reset, so the next dummy traffic packet is generated after
	 * the next timeout of the dummy traffic timer, if it reaches the timeout and no reset
	 * interrupts the timer.
	 */
	public void resetDummyTrafficInterval()
	{
		synchronized (this)
		{
			if (m_threadRunLoop != null)
			{
				m_threadRunLoop.interrupt();
			}
		}
	}

	/**
	 * Changes the dummy traffic interval.
	 *
	 * @param a_interval The new dummy traffic interval in milliseconds or -1, if dummy traffic
	 *                   shall be disabled.
	 */
	public void setDummyTrafficInterval(int a_interval)
	{
		boolean sendDummy = false;
		synchronized (this)
		{
			stop();
			m_interval = (long) a_interval;
			if (a_interval > -1)
			{
				start();
				/* send a dummy, else the interval until a dummy is sent could be the sum of the old and
				 * the new interval value -> too long interval
				 */
				sendDummy = true;
			}
		}
		if (sendDummy == true)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.NET, "Sending Dummy!");
			m_muxSocket.sendDummy();
		}
	}

	/**
	 * Starts the internal dummy traffic thread, if it is not already running.
	 */
	private void start()
	{
		synchronized (this)
		{
			if (m_bRun == false)
			{
				m_bRun = true;
				m_threadRunLoop = new Thread(this, "JAP - Dummy Traffic");
				m_threadRunLoop.setDaemon(true);
				m_threadRunLoop.start();
			}
		}
	}

}
