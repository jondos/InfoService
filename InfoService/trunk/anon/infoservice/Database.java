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
package anon.infoservice;

import java.util.Hashtable;
import java.util.Vector;
import java.util.Enumeration;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/**
 * This class is the generic implementation of a database. It is used by the database
 * implementations for the different services.
 */
public abstract class Database implements Runnable {

  /**
   * Stores services we know.
   */
  private Hashtable serviceDatabase;

  /**
   * Chronological order (in relation to timeouts) of all objects in the database.
   */
  private Vector timeoutList;


  /**
   * Creates a new instance of a Database.
   */
  protected Database() {
    serviceDatabase = new Hashtable();
    timeoutList = new Vector();
  }

  /**
   * This is the garbage collector for the database. If an entry becomes
   * out dated, it will automaticly removed from the database.
   */
  public void run() {
    while (true) {
      boolean moreOldEntrys = true;
      synchronized (serviceDatabase) {
        /* we need exclusive access to the database */
        while ((timeoutList.size() > 0) && (moreOldEntrys)) {
          if (System.currentTimeMillis() >= ((DatabaseEntry)(serviceDatabase.get(timeoutList.firstElement()))).getExpireTime()) {
            /* we remove the old entry now, because it has reached the expire time */
            LogHolder.log(LogLevel.INFO, LogType.MISC, "Database: run: DatabaseEntry " + ((DatabaseEntry)(serviceDatabase.get(timeoutList.firstElement()))).getId() + " has reached the expire time and is removed.");
            serviceDatabase.remove(timeoutList.firstElement());
            timeoutList.removeElementAt(0);
          }
          else {
            /* the oldest entry in the database, has not reached expire time now, so there are not more old entrys */
            moreOldEntrys = false;
          }
        }
      }
      synchronized (serviceDatabase) {
        /* we need the database in a consistent state */
        long sleepTime = 0;
        if (timeoutList.size() > 0) {
          /* get time until next timeout */
          sleepTime = ((DatabaseEntry)(serviceDatabase.get(timeoutList.firstElement()))).getExpireTime() - System.currentTimeMillis();
        }
        if (sleepTime > 0) {
          /* there is nothing to do now -> wait until next expire time */
          try {
            serviceDatabase.wait(sleepTime);
            LogHolder.log(LogLevel.DEBUG, LogType.MISC, "Database: run: One InfoService entry could be expired. Wake up...");
          }
          catch (Exception e) {
          }
        }
        if (timeoutList.size() == 0) {
          /* there are no entries in the database, wait until there are some */
          try {
            serviceDatabase.wait();
            LogHolder.log(LogLevel.DEBUG, LogType.MISC, "Database: run: First InfoService entry in the database. Look when it expires. Wake up...");
          }
          catch (Exception e) {
          }
        }
      }
    }
  }

  /**
   * Updates an entry in the database. If the entry is an unknown or if it is newer then the
   * one stored in the database for this service, the new entry is stored in the database and
   * forwarded to all neighbour infoservices. This method is called by the childs of Database.
   *
   * @param newEntry The DatabaseEntry to update.
   */
  protected void update(DatabaseEntry newEntry) {
    synchronized (serviceDatabase) {
      /* we need exclusive access to the database */
      DatabaseEntry oldEntry = (DatabaseEntry)(serviceDatabase.get(newEntry.getId()));
      boolean entryAdded = false;
      if (oldEntry == null) {
        /* this is a new unknown service */
        serviceDatabase.put(newEntry.getId(), newEntry);
        entryAdded = true;
      }
      else {
        /* we know this service, look whether the entry is newer, then the one we have stored */
        if (newEntry.getExpireTime() > oldEntry.getExpireTime()) {
          /* it is newer */
          timeoutList.removeElement(oldEntry.getId());
          serviceDatabase.put(newEntry.getId(), newEntry);
          entryAdded = true;
        }
      }
      if (entryAdded == true) {
        /* update the timeoutList */
        boolean timeoutEntryInserted = false;
        int i = 0;
        while (timeoutEntryInserted == false) {
          if (i < timeoutList.size()) {
            if (((DatabaseEntry)(serviceDatabase.get(timeoutList.elementAt(i)))).getExpireTime() >= newEntry.getExpireTime()) {
              timeoutList.insertElementAt(newEntry.getId(), i);
              timeoutEntryInserted = true;
            }
          }
          else {
            /* we are at the last position in the list -> add entry at the end */
            timeoutList.addElement(newEntry.getId());
            timeoutEntryInserted = true;
          }
          i++;
        }
        if (i == 1) {
          /* entry at the first expire position added -> notify the cleanup thread */
          serviceDatabase.notify();
        }
      }
    }
  }

  /**
   * Removes an entry from the database.
   *
   * @deleteEntry The entry to remove. If it is not in the database, nothing is done.
   */
  protected void remove(DatabaseEntry deleteEntry) {
    if (deleteEntry != null) {
      synchronized (serviceDatabase) {
        /* we need exclusive access to the database */
        if (serviceDatabase.remove(deleteEntry.getId()) != null) {
          timeoutList.removeElement(deleteEntry.getId());
        }
      }
    }
  }

  /**
   * Removes all entries from the database.
   */
  protected void removeAll() {
    synchronized (serviceDatabase) {
      /* we need exclusive access to the database */
      serviceDatabase.clear();
      timeoutList.removeAllElements();
    }
  }

  /**
   * Returns a snapshot of all values in the serviceDatabase.
   *
   * @return A Vector with all values which are stored in the serviceDatabase.
   */
  protected Vector getEntryList() {
    Vector entryList = new Vector();
    synchronized (serviceDatabase) {
      /* get the actual values */
      Enumeration serviceDatabaseElements = serviceDatabase.elements();
      while (serviceDatabaseElements.hasMoreElements()) {
        entryList.addElement(serviceDatabaseElements.nextElement());
      }
    }
    return entryList;
  }

  /**
   * Returns the DatabaseEntry with the given ID. If there is no DatabaseEntry with this ID is
   * in the database, null is returned.
   *
   * @param entryId The ID of the database entry.
   */
  protected DatabaseEntry getEntryById(String entryId) {
    DatabaseEntry resultEntry = null;
    synchronized (serviceDatabase) {
      /* get the actual value */
      resultEntry = (DatabaseEntry)(serviceDatabase.get(entryId));
    }
    return resultEntry;
  }

}