/*
 Copyright (c) 2000 - 2006, The JAP-Team
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
package anon.util;

import logging.LogHolder;
import logging.LogLevel;
import java.util.Vector;
import logging.LogType;

/**
 *
 *
 * @author Rolf Wendolsky
 */
public class JobQueue
{
	/**
	 * Stores the jobs, if we receive new setAnonMode() requests.
	 */
	private Vector m_changeAnonModeJobs;
	private Vector m_changeAnonModeJobThreads;

	private Thread m_threadQueue;
	private boolean m_bInterrupted = false;

	private Job m_currentJob;
	private Thread m_currentJobThread;

	public JobQueue()
	{
		this("Job queue");
	}

	public JobQueue(String a_name)
	{
		m_changeAnonModeJobs = new Vector();
		m_changeAnonModeJobThreads = new Vector();
		m_threadQueue = new Thread(new Runnable()
		{
			public void run()
			{
				synchronized (Thread.currentThread())
				{
					while (!Thread.currentThread().isInterrupted() && !m_bInterrupted)
					{
						try
						{
							Thread.currentThread().wait();
						}
						catch (InterruptedException ex)
						{
						}
						if (Thread.currentThread().isInterrupted())
						{
							Thread.currentThread().notifyAll();
							break;
						}

						// There is a new job!
						if (m_changeAnonModeJobs.size() > 0 &&
							m_currentJob == m_changeAnonModeJobs.firstElement() &&
							m_currentJobThread.isAlive())
						{
							// a job is currently running; stop it;
							m_currentJobThread.interrupt();
						}
						else if (m_changeAnonModeJobs.size() > 0)
						{
							// no job is running; remove all jobs that are outdated
							while (m_changeAnonModeJobs.size() > 1)
							{
								m_changeAnonModeJobs.removeElementAt(0);
								m_changeAnonModeJobThreads.removeElementAt(0);
							}

							// start the newest job
							m_currentJob = (Job) m_changeAnonModeJobs.elementAt(0);
							m_currentJobThread = (Thread) m_changeAnonModeJobThreads.elementAt(0);
							m_currentJobThread.start();
						}
					}
					// stop all threads
					while (m_changeAnonModeJobs.size() > 0)
					{
						if (m_currentJob == m_changeAnonModeJobs.firstElement())
						{
							m_currentJobThread.interrupt();

							try
							{
								Thread.currentThread().wait(500);
							}
							catch (InterruptedException ex1)
							{
							}
						}
						else
						{
							m_changeAnonModeJobs.removeAllElements();
							m_changeAnonModeJobThreads.removeAllElements();
						}
					}
				}
			}
		}, a_name);
		m_threadQueue.start();
	}

	public static abstract class Job implements Runnable
	{
		boolean m_bMayBeSkippedIfDuplicate;

		public Job(boolean a_bMayBeSkippedIfDuplicate)
		{
			super();
			m_bMayBeSkippedIfDuplicate = a_bMayBeSkippedIfDuplicate;
		}

		public Job()
		{
			this(false);
		}

		public String getAddedJobLogMessage()
		{
			return null;
		}

		public final boolean mayBeSkippedIfDuplicate()
		{
			return m_bMayBeSkippedIfDuplicate;
		}
	}

	public void addJob(final Job a_anonJob)
	{
		Thread jobThread;

		if (a_anonJob == null)
		{
			return;
		}

		if (!a_anonJob.mayBeSkippedIfDuplicate() && m_bInterrupted)
		{
			// do not make new connections during shutdown
			return;
		}

		synchronized (m_threadQueue)
		{
			if (m_changeAnonModeJobs.contains(a_anonJob))
			{
				// do not accept duplicate jobs
				return;
			}

			if (m_changeAnonModeJobs.size() > 0)
			{
				/* check whether this is job is different to the last one */
				Job lastJob = (Job) (m_changeAnonModeJobs.lastElement());
				if (lastJob.mayBeSkippedIfDuplicate() && a_anonJob.mayBeSkippedIfDuplicate())
				{
					/* it's the same (disabling server) as the last job */
					return;
				}
			}
			jobThread = new Thread(a_anonJob);
			jobThread.setDaemon(true);
			m_changeAnonModeJobs.addElement(a_anonJob);
			m_changeAnonModeJobThreads.addElement(jobThread);
			m_threadQueue.notify();

			String logMessage = a_anonJob.getAddedJobLogMessage();
			if (logMessage != null)
			{
				LogHolder.log(LogLevel.DEBUG, LogType.MISC, logMessage);
			}
		}
	}

	public void stop()
	{
		while (m_threadQueue.isAlive())
		{
			m_threadQueue.interrupt();
			synchronized (m_threadQueue)
			{
				m_bInterrupted = true;
				m_threadQueue.notifyAll();
				m_threadQueue.interrupt();
			}
			try
			{
				m_threadQueue.join(500);
			}
			catch (InterruptedException a_e)
			{
				// ignore
			}
		}
	}

	public void removeJob(final Job a_anonJob)
	{
		if (a_anonJob == null)
		{
			return;
		}
		synchronized (m_threadQueue)
		{
			int index = m_changeAnonModeJobs.indexOf(a_anonJob);

			if (index >= 0)
			{
				((Thread)m_changeAnonModeJobThreads.elementAt(index)).interrupt();
				m_changeAnonModeJobs.removeElementAt(index);
				m_changeAnonModeJobThreads.removeElementAt(index);
				m_threadQueue.notify();
			}
		}
	}
}
