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
package anon.infoservice.test;

import java.util.Vector;
import junitx.framework.PrivateTestCase;

import anon.infoservice.Database;
import anon.infoservice.DatabaseEntry;
import anon.infoservice.InfoService;

/**
 * These are the tests for the main part of the Database class.
 * @author Wendolsky
 */
public class DatabaseTest extends PrivateTestCase
{
	/**
	 * A generic Database object is needed in order to access the private static methods.
	 */
	Database m_DatabaseObject;

	MockDistributor m_distributor;

	public DatabaseTest(String a_strName)
	{
		super(a_strName);
	}

	protected void setUp() throws Exception
	{
		m_distributor = new MockDistributor();
		m_DatabaseObject = Database.getInstance(DatabaseEntry.class);
		m_DatabaseObject.registerDistributor(m_distributor);

		invoke(m_DatabaseObject, "unregisterInstances", NOARGS);
	}

	protected void tearDown() throws Exception
	{
		invoke(m_DatabaseObject, "unregisterInstances", NOARGS);
	}

	/**
	 * Tests the exception that is thrown if an invalid DatabaseEntry class is used to get
	 * or update a database.
	 */
	public void testDatabaseEntryClassChecking()
	{
		DatabaseEntry dbEntry;

		try
		{
			Database.getInstance(Vector.class);
			fail();
		} catch (IllegalArgumentException a_e)
		{
		}

		Database.getInstance(InfoService.class);
		Database.getInstance(DummyDatabaseEntry.class);
		dbEntry = new DummyDatabaseEntry();

		//this works
		Database.getInstance(DummyDatabaseEntry.class).update(dbEntry);
		try
		{
			// this does not work
			Database.getInstance(InfoService.class).update(dbEntry);
			fail();
		} catch (IllegalArgumentException a_e)
		{
		}
	}

	/**
	 * Tests the update method.
	 */
	public void testUpdate()
	{
		DummyDatabaseEntry dbEntry, dbEntry_sameID, dbEntry_otherID;

		dbEntry = new DummyDatabaseEntry();
		dbEntry.setId("123456");

		dbEntry_sameID = new DummyDatabaseEntry();
		dbEntry_sameID.setId("123456");

		dbEntry_otherID = new DummyDatabaseEntry();
		dbEntry_otherID.setId("66666");

		//Database.
	}
}
