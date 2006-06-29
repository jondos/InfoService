/*
 Copyright (c) 2006, The JAP-Team
 All rights reserved.
 Redistribution and use in source and binary forms, with or without modification,
 are permitted provided that the following conditions are met:

  - Redistributions of source code must retain the above copyright notice,
 this list of conditions and the following disclaimer.

  - Redistributions in bisnary form must reproduce the above copyright notice,
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
package jap;

import java.util.Enumeration;
import java.util.Observable;
import java.util.Observer;
import java.util.Hashtable;

import anon.infoservice.AbstractDatabaseEntry;
import anon.infoservice.Database;
import anon.infoservice.MixCascade;
import anon.util.ClassUtil;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/**
 * Updates the local database. This may be done automatically (by a background thread) and manually
 * by method call. The automatic update is only done if this is allowed by the model.
 * @author Rolf Wendolsky
 */
public abstract class AbstractDatabaseUpdater implements Observer
{
	private IUpdateInterval m_updateInterval;
	private Thread m_updateThread;
	private boolean m_successfulUpdate = false;
	private boolean m_bAutoUpdateChanged = false;
	private boolean m_bInitialRun = true;

	/**
	 * Initialises and starts the database update thread.
	 */
	public AbstractDatabaseUpdater(IUpdateInterval a_updateInterval)
	{
		if (a_updateInterval == null)
		{
			throw new IllegalArgumentException("No update interval specified!");
		}
		/*if (a_updateInterval.getUpdateInterval() <= 1000)
		{
			throw new IllegalArgumentException(
						 "Database update interval of " + a_updateInterval + " is too short!");
		}*/

		m_updateInterval = a_updateInterval;

		JAPModel.getInstance().addObserver(this);
		m_updateThread = new Thread(new Runnable()
		{
			public void run()
			{
				LogHolder.log(LogLevel.INFO, LogType.THREAD,
							  getUpdatedClassName() + "update thread started.");
				while (!Thread.currentThread().isInterrupted())
				{
					synchronized (Thread.currentThread())
					{
						m_bAutoUpdateChanged = true; // this is important to switch waiting times
						while (m_bAutoUpdateChanged)
						{
							m_bAutoUpdateChanged = false; // normally, this should be false after first call
							try
							{
								Thread.currentThread().notify();
								if (JAPModel.getInstance().isInfoServiceDisabled() || m_bInitialRun)
								{
									Thread.currentThread().wait();
								}
								else
								{
									Thread.currentThread().wait(m_updateInterval.getUpdateInterval());
								}
							}
							catch (InterruptedException a_e)
							{
								Thread.currentThread().notifyAll();
								break;
							}
							if (Thread.currentThread().isInterrupted())
							{
								Thread.currentThread().notifyAll();
								break;
							}
						}
					}

					if (!Thread.currentThread().isInterrupted() && !isUpdatePaused())
					{
						LogHolder.log(LogLevel.INFO, LogType.THREAD,
									  "Updating " + getUpdatedClassName() + "list.");

						updateInternal();
					}
				}
				LogHolder.log(LogLevel.INFO, LogType.THREAD,
							  getUpdatedClassName() + "update thread stopped.");
			}
		}, getUpdatedClassName() + "Update Thread");
		m_updateThread.setPriority(Thread.MIN_PRIORITY);
		m_updateThread.setDaemon(true);
		m_updateThread.start();
	}

	public void update(Observable a_observable, Object a_argument)
	{
		if (!(a_argument instanceof Integer) ||
			!((Integer)a_argument).equals(JAPModel.CHANGED_INFOSERVICE_AUTO_UPDATE))
		{
			return;
		}
		final AbstractDatabaseUpdater updater = this;
		new Thread()
		{
			public void run()
			{
				updater.start(false);
			}
		}.start();
	}

	/**
	 * Starts the thread if it has not already started or has been stopped before.
	 */
	public final void start(boolean a_bSynchronized)
	{
		synchronized (this)
		{
			synchronized (m_updateThread)
			{
				m_bAutoUpdateChanged = true;
				m_bInitialRun = false;
				m_updateThread.notifyAll();
				if (a_bSynchronized)
				{
					try
					{
						m_updateThread.wait();
					}
					catch (InterruptedException ex)
					{
					}
				}
			}
		}
	}

	/**
	 * Force a synchronized update of the known database entries.
	 * @return true if the update was successful, false otherwise
	 */
	public final boolean update()
	{
		return update(true);
	}

	/**
	 * Force an update of the known database entries. The current thread does not wait until it is done.
	 * @return true if the update was successful, false otherwise
	 */
	public final void updateAsync()
	{
		new Thread()
		{
			public void run()
			{
				update(false);
			}
		}.start();
	}

	/**
	 * Force an update of the known database entries.
	 * @param a_bSynchronized true if the current thread should wait until the update is done; false otherwise
	 * @return true if the update was successful, false otherwise
	 */
	private final boolean update(boolean a_bSynchronized)
	{
		if (m_bInitialRun)
		{
			start(true);
		}

		synchronized (this)
		{
			synchronized (m_updateThread)
			{
				m_bAutoUpdateChanged = false;
				m_updateThread.notifyAll();
				if (a_bSynchronized)
				{
					try
					{
						m_updateThread.wait();
					}
					catch (InterruptedException a_e)
					{
						return false;
					}
					return m_successfulUpdate;
				}
				return true;
			}
		}
	}

	/**
	 * Stops the update thread. No further updates are possible.
	 */
	public final void stop()
	{
		JAPModel.getInstance().deleteObserver(this);
		while (m_updateThread.isAlive())
		{
			m_updateThread.interrupt();
			synchronized (m_updateThread)
			{
				m_bAutoUpdateChanged = false;
				m_bInitialRun = false;
				m_updateThread.notifyAll();
				m_updateThread.interrupt();
			}
			try
			{
				m_updateThread.join();
			}
			catch (InterruptedException a_e)
			{
				// ignore
			}
		}
	}

	public abstract Class getUpdatedClass();

	protected static class ConstantUpdateInterval implements IUpdateInterval
	{
		private int m_updateInterval;
		public ConstantUpdateInterval(int a_updateInterval)
		{
			m_updateInterval = a_updateInterval;
		}

		public int getUpdateInterval()
		{
			return m_updateInterval;
		}
	}

	protected static interface IUpdateInterval
	{
		int getUpdateInterval();
	}

	/**
	 * Does the update. Subclasses may overwrite to, for example, synchronize the update with another object.
	 */
	protected void updateInternal()
	{
		Hashtable newEntries = getUpdatedEntries();
		if (Thread.currentThread().isInterrupted())
		{
			// this thread is being stopped; ignore this error
			m_successfulUpdate = true;
		}
		else if (newEntries == null)
		{
			LogHolder.log(LogLevel.ERR, LogType.THREAD,
						  getUpdatedClassName() + "update failed!");
			m_successfulUpdate = false;

		}
		else
		{
			LogHolder.log(LogLevel.DEBUG, LogType.THREAD,
						  getUpdatedClassName() + "update was successful.");
			boolean updated = false;
			m_successfulUpdate = true;
			/* we have successfully downloaded the list of running infoservices -> update the
			 * internal database of known infoservices
			 */
			Enumeration entries = newEntries.elements();
			while (entries.hasMoreElements())
			{

				AbstractDatabaseEntry currentEntry = (AbstractDatabaseEntry) (entries.nextElement());
				if (Database.getInstance(getUpdatedClass()).update(currentEntry))
				{
					updated = true;
				}
				AbstractDatabaseEntry preferredEntry = getPreferredEntry();
				if (preferredEntry != null)
				{
					/* if the current entry is equal to the preferred entry,
					 * update the preferred entry, too
					 */
					if (preferredEntry.equals(currentEntry))
					{
						setPreferredEntry(currentEntry);
					}
				}
			}


			updated = doCleanup(newEntries) || updated;


			if ((getUpdatedClass() == MixCascade.class) && updated)
			{
				JAPController.getInstance().notifyJAPObservers();
			}
		}
	}

	/**
	 * Does some cleaup operations of the database. All old entries that were not updated by
	 * the new entries are removed. Subclasses may overwrite this method to suppres or alter this
	 * behaviour. This method is called by updateInternal().
	 * @param a_newEntries the list of new entries
	 * @return boolean
	 */
	protected boolean doCleanup(Hashtable a_newEntries)
	{
		boolean bUpdated = false;

		/* now remove all non user-defined infoservices, which were not updated, from the
		 * database of known infoservices
		 */
		Enumeration knownInfoServices =
			Database.getInstance(getUpdatedClass()).getEntryList().elements();
		while (knownInfoServices.hasMoreElements())
		{
			AbstractDatabaseEntry currentEntry = (AbstractDatabaseEntry) (
				knownInfoServices.nextElement());
			if (!currentEntry.isUserDefined() && !a_newEntries.contains(currentEntry))
			{
				/* the InfoService was fetched from the Internet earlier, but it is not
				 * in the list fetched from the Internet this time
				 * -> remove that InfoService from the database of known InfoServices
				 */
				if (Database.getInstance(getUpdatedClass()).remove(currentEntry))
				{
					bUpdated = true;
				}
			}
		}
		return bUpdated;
	}

	protected boolean isUpdatePaused()
	{
		return false;
	}

	protected  AbstractDatabaseEntry getPreferredEntry()
	{
		return null;
	}
	protected void setPreferredEntry(AbstractDatabaseEntry a_preferredEntry)
	{
	}

	protected abstract Hashtable getUpdatedEntries();

	private String getUpdatedClassName()
	{
		return ClassUtil.getShortClassName(getUpdatedClass()) + " ";
	}
}
