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

/**
 * This is a generic definition for a database entry. Every database entry
 * must implement this functions.
 */
public abstract class DatabaseEntry
{
	/**
	 * Stores the time when this entry will be deleted from the database.
	 */
	private long m_expireTime;

	/**
	 * Creates a new DatabaseEntry with the specified expireTime.
	 * @param a_expireTime the time when this entry will be deleted from the database
	 */
	public DatabaseEntry(long a_expireTime)
	{
		m_expireTime = a_expireTime;
	}

	/**
	 * Returns a unique ID for a database entry.
	 *
	 * @return The ID of this database entry.
	 */
	public abstract String getId();

	/**
	 * Returns the time (see System.currentTimeMillis()) when this DatabaseEntry will be removed
	 * from the Database, if it is not updated meanwhile.
	 *
	 * @return The expire time for this DatabaseEntry.
	 */
	public long getExpireTime()
	{
		return m_expireTime;
	}

	/**
	 * Returns a version number that indicates if a DatabaseEntry is newer than an other
	 * DatabaseEntry. By default, the expire time is returned.
	 *
	 * @return The expire time for this DatabaseEntry.
	 */
	public long getVersionNumber()
	{
		return getExpireTime();
	}
}
