/*
 Copyright (c) 2000 - 2004, The JAP-Team
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
/* Hint: This file may be only a copy of the original file which is always in the JAP source tree!
 * If you change something - do not forget to add the changes also to the JAP source tree!
 */
package anon.infoservice;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;
import anon.crypto.MyRandom;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/**
 * This class is the generic implementation of a database. It is used by the database
 * implementations for the different services.
 * It is also a registry for all databases used in the context of this application.
 */
public final class Database implements Runnable
{
	/**
	 * The registered databases.
	 */
	private static Hashtable m_databases = new Hashtable();

	/**
	 * The distributor that forwards new database entries.
	 */
	private static AbstractDistributor m_distributor;

	/**
	 * The DatabaseEntry class for that this Database is registered.
	 * The Database can only hold instances of this class.
	 */
	private Class m_DatabaseEntryClass;

	/**
	 * Stores services we know.
	 */
	private Hashtable m_serviceDatabase;

	/**
	 * Chronological order (in relation to timeouts) of all objects in the database.
	 */
	private Vector m_timeoutList;

	/**
	 * Registers a distributor that forwards new database entries.
	 * @param a_distributor a distributor that forwards new database entries
	 */
	public static void registerDistributor(AbstractDistributor a_distributor) {
		m_distributor = a_distributor;
	}

	/**
	 * Registers a Database object. If a Database was previously registered for the same
	 * DatabaseEntry class, the method does nothing and returns the previously registered Database.
	 * Otherwise, the given Database is returned.
	 * This method is used for testing purposes and should not be removed.
	 * @param a_Database the registered Database
	 * @return the actually registered Database instance for the specified DatabaseEntry class
	 */
	private static Database registerInstance(Database a_Database)
	{
		Database database = (Database)m_databases.get(a_Database.getEntryClass());

		if (database == null && a_Database != null) {
			m_databases.put(a_Database.getEntryClass(), a_Database);
			database = a_Database;
		}

		return database;
	}

	/**
	 * Unregisters the Database object that contains instances of the specified DatabaseEntry class.
	 * This method is used for testing purposes and should not be removed.
	 * @param a_DatabaseEntryClass the DatabaseEntry class for that the corresponding Database
	 *                             is unregistered
	 * @return the Database instance for the specified DatabaseEntry class that was unregistered
	 *         or null if  no corresponding Database could be found
	 */
	private static Database unregisterInstance(Class a_DatabaseEntryClass)
	{
		return (Database)m_databases.remove(a_DatabaseEntryClass);
	}

	/**
	 * Unregisters all Database instances
	 * This method is used for testing purposes and should not be removed.
	 */
	private static void unregisterInstances()
	{
		m_databases.clear();
	}

	/**
	 * Gets the Database for the specified database entries. Creates the Database
	 * if it does not exist already.
	 * @param a_DatabaseEntryClass the DatabaseEntry class for that the method returns
	 * the corresponding Database object
	 * @return the Database object that contains DatabaseEntries of the specified type
	 * @exception IllegalArgumentException if the argument is no valid DatabaseEntry class
	 */
	public static Database getInstance(Class a_DatabaseEntryClass)
		throws IllegalArgumentException
	{
		Database database = (Database)m_databases.get(a_DatabaseEntryClass);

		if (database == null) {
			database = new Database(a_DatabaseEntryClass);
			m_databases.put(a_DatabaseEntryClass, database);
		}

		return database;
	}

	/**
	 * Get an Enumeration of all registered Databases.
	 * @return an Enumeration of all registered Databases
	 */
	public static Enumeration getInstances() {
		return m_databases.elements();
	}

	/**
	 * Creates a new instance of a Database.
	 * @param a_DatabaseEntryClass the DatabaseEntry class for that this Database is registered
	 * @exception IllegalArgumentException if the argument is no valid DatabaseEntry class
	 */
	private Database(Class a_DatabaseEntryClass)
		throws IllegalArgumentException
	{
		if (!DatabaseEntry.class.isAssignableFrom(a_DatabaseEntryClass)) {
			throw new IllegalArgumentException(
				"There is no Database that can store entries of type " +
				a_DatabaseEntryClass.getName() + "!");
		}

		m_DatabaseEntryClass = a_DatabaseEntryClass;
		m_serviceDatabase = new Hashtable();
		m_timeoutList = new Vector();

		Thread dbThread = new Thread(this);
		dbThread.setDaemon(true);
		dbThread.start();
	}

	/**
	 * This is the garbage collector for the database. If an entry becomes
	 * outdated, it will be automatically removed from the database.
	 */
	public void run()
	{
		while (true)
		{
			boolean moreOldEntrys = true;
			synchronized (m_serviceDatabase)
			{
				/* we need exclusive access to the database */
				while ( (m_timeoutList.size() > 0) && (moreOldEntrys))
				{
					if (System.currentTimeMillis() >=
						( (DatabaseEntry) (
						m_serviceDatabase.get(m_timeoutList.firstElement()))).getExpireTime())
					{
						/* we remove the old entry now, because it has reached the expire time */
						LogHolder.log(LogLevel.INFO, LogType.MISC,
									  "DatabaseEntry " +
									  ( (DatabaseEntry) (m_serviceDatabase.get(m_timeoutList.firstElement()))).
									  getId() + " has reached the expire time and is removed.");
						m_serviceDatabase.remove(m_timeoutList.firstElement());
						m_timeoutList.removeElementAt(0);
					}
					else
					{
						/* the oldest entry in the database
						 * has not reached expire time now, so there are not more old entrys
						 */
						moreOldEntrys = false;
					}
				}
			}
			synchronized (m_serviceDatabase)
			{
				/* we need the database in a consistent state */
				long sleepTime = 0;
				if (m_timeoutList.size() > 0)
				{
					/* get time until next timeout */
					sleepTime = ( (DatabaseEntry) (m_serviceDatabase.get(m_timeoutList.firstElement()))).
						getExpireTime() - System.currentTimeMillis();
				}
				if (sleepTime > 0)
				{
					/* there is nothing to do now -> wait until next expire time */
					try
					{
						m_serviceDatabase.wait(sleepTime);
						LogHolder.log(LogLevel.DEBUG, LogType.MISC,
									  "One entry could be expired. Wake up...");
					}
					catch (Exception e)
					{
					}
				}
				if (m_timeoutList.size() == 0)
				{
					/* there are no entries in the database, wait until there are some */
					try
					{
						m_serviceDatabase.wait();
						LogHolder.log(LogLevel.DEBUG, LogType.MISC,
									  "First entry in the database. " +
									  "Look when it expires. Wake up...");
					}
					catch (Exception e)
					{
					}
				}
			}
		}
	}

	/**
	 * Updates an entry in the database. If the entry is an unknown or if it is newer then the
	 * one stored in the database for this service, the new entry is stored in the database and
	 * forwarded to all neighbour infoservices.
	 *
	 * @param newEntry The DatabaseEntry to update.
	 * @exception IllegalArgumentException if the DatabaseEntry is not of the type the Database
	 * can store
	 */
	public void update(DatabaseEntry newEntry)
		throws IllegalArgumentException
	{
		if (!m_DatabaseEntryClass.isAssignableFrom(newEntry.getClass()))
		{
			throw new IllegalArgumentException(
						 "Database cannot store entries of type " +
						 newEntry.getClass().getName() + "!");
		}
		synchronized (m_serviceDatabase)
		{
			/* we need exclusive access to the database */
			DatabaseEntry oldEntry = (DatabaseEntry) (m_serviceDatabase.get(newEntry.getId()));
			boolean addEntry = false;
			if (oldEntry == null)
			{
				/* this is a new unknown service */
				addEntry = true;
			} else if (newEntry.getVersionNumber() > oldEntry.getVersionNumber())
			{
				// we know this service, and the entry is newer than the one we have stored
				addEntry = true;
				m_timeoutList.removeElement(oldEntry.getId());
			}
			if (addEntry)
			{
				// add the entry to the database
				m_serviceDatabase.put(newEntry.getId(), newEntry);

				/* update the timeoutList */
				boolean timeoutEntryInserted = false;
				int i = 0;
				while (timeoutEntryInserted == false)
				{
					if (i < m_timeoutList.size())
					{
						if ( ( (DatabaseEntry) (m_serviceDatabase.get(
											  m_timeoutList.elementAt(i)))).getExpireTime() >=
							 newEntry.getExpireTime())
						{
							m_timeoutList.insertElementAt(newEntry.getId(), i);
							timeoutEntryInserted = true;
						}
					}
					else
					{
						/* we are at the last position in the list -> add entry at the end */
						m_timeoutList.addElement(newEntry.getId());
						timeoutEntryInserted = true;
					}
					i++;
				}
				if (i == 1)
				{
					/* entry at the first expire position added -> notify the cleanup thread */
					m_serviceDatabase.notify();
				}
				if (newEntry instanceof IDistributable)
				{
					// forward new entries
					if (m_distributor != null)
					{
						m_distributor.addJob( (IDistributable) newEntry);
					} else
					{
						LogHolder.log(LogLevel.WARNING, LogType.MISC,
									  "Database: update: No distributor specified." +
									  "Cannot distribute database entries!");
					}
				}
			}
		}
	}

	/**
	 * Returns the DatabaseEntry class for that this Database is registered.
	 * @return the DatabaseEntry class for that this Database is registered
	 */
	public Class getEntryClass()
	{
		return m_DatabaseEntryClass;
	}

	/**
	 * Removes an entry from the database.
	 *
	 * @param deleteEntry The entry to remove. If it is not in the database, nothing is done.
	 */
	public void remove(DatabaseEntry deleteEntry)
	{
		if (deleteEntry != null)
		{
			synchronized (m_serviceDatabase)
			{
				/* we need exclusive access to the database */
				if (m_serviceDatabase.remove(deleteEntry.getId()) != null)
				{
					m_timeoutList.removeElement(deleteEntry.getId());
				}
			}
		}
	}

	/**
	 * Removes all entries from the database.
	 */
	public void removeAll()
	{
		synchronized (m_serviceDatabase)
		{
			/* we need exclusive access to the database */
			m_serviceDatabase.clear();
			m_timeoutList.removeAllElements();
		}
	}

	/**
	 * Returns a snapshot of all values in the serviceDatabase.
	 *
	 * @return A Vector with all values which are stored in the serviceDatabase.
	 */
	public Vector getEntryList()
	{
		Vector entryList = new Vector();
			/* get the actual values */
			Enumeration serviceDatabaseElements = m_serviceDatabase.elements();
			while (serviceDatabaseElements.hasMoreElements())
			{
				entryList.addElement(serviceDatabaseElements.nextElement());
			}
		return entryList;
	}

	/**
	 * Returns a snapshot of all entries in the Database as an Enumeration.
	 *
	 * @return a snapshot of all entries in the Database as an Enumeration
	 */
	public Enumeration getEntrySnapshotAsEnumeration()
	{
		return m_serviceDatabase.elements();
	}

	/**
	 * Returns the number of DatabaseEntries in the Database.
	 * @return the number of DatabaseEntries in the Database
	 */
	public int getNumberofEntries()
	{
		return m_serviceDatabase.size();
	}

	/**
	 * Returns the DatabaseEntry with the given ID. If there is no DatabaseEntry with this ID is
	 * in the database, null is returned.
	 *
	 * @param entryId The ID of the database entry.
	 * @return The entry with the specified ID or null, if there is no such entry.
	 */
	public DatabaseEntry getEntryById(String entryId)
	{
		DatabaseEntry resultEntry = null;
		synchronized (m_serviceDatabase)
		{
			/* get the actual value */
			resultEntry = (DatabaseEntry) (m_serviceDatabase.get(entryId));
		}
		return resultEntry;
	}

	/**
	 * Returns a random entry from the database. If there are no entries in the database, null is
	 * returned.
	 *
	 * @return A random entry from the database or null, if the database is empty.
	 */
	public DatabaseEntry getRandomEntry()
	{
		DatabaseEntry resultEntry = null;
		synchronized (m_serviceDatabase)
		{
			/* all keys of the database are in the timeout list -> select a random key from there
			 * and get the associated entry from the database
			 */
			if (m_timeoutList.size() > 0)
			{
				try
				{
					String entryId =
						(String) m_timeoutList.elementAt(
										  new MyRandom().nextInt(m_timeoutList.size()));
					resultEntry = (DatabaseEntry) (m_serviceDatabase.get(entryId));
				}
				catch (Exception e)
				{
					/* should never occur */
				}
			}
		}
		return resultEntry;
	}
}
