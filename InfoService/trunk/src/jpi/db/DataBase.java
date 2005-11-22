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
package jpi.db;

import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Random;
import java.util.Vector;

import anon.pay.xml.XMLEasyCC;
import anon.util.XMLUtil;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/**
 * Implements {@link DBInterface} for Postgresql
 *
 * @author Andreas Mueller, Bastian Voigt
 * @version 1.1
 */
public class DataBase extends DBInterface
{
	private Connection m_dbConn;
	public DataBase(Connection c)
	{
		m_dbConn = c;
	}

	// random numbers for generating account / transfer numbers
	private Random rnd = new Random(System.currentTimeMillis());

	// Names of objects in the DB
	private String[] db_tables =
		{
		"TABLE ACCOUNTS", "TABLE COSTCONFIRMATIONS",
		"TABLE TRANSFERS",
		"TABLE RATES", "SEQUENCE RATES_ID_SEQ",
		"SEQUENCE ACCOUNTS_ACCOUNTNUMBER_SEQ",
		"SEQUENCE TRANSFERS_TRANSFERNUMBER_SEQ",
		"SEQUENCE TRANSFERS_ACCOUNTNUMBER_SEQ",
//		"SEQUENCE CASCADES_ACCOUNTNUMBER_SEQ",
//		"SEQUENCE CASCADES_CASCADENUMBER_SEQ",
//		"SEQUENCE CASCADENAMES_CASCADENUMBER_SEQ"
	};

	// sql statements for creating the DB tables
	private String[] create_statements =
		{
		"CREATE TABLE ACCOUNTS (" +
		"ACCOUNTNUMBER BIGSERIAL PRIMARY KEY," +
		"XMLPUBLICKEY VARCHAR(1000)," +
		"DEPOSIT BIGINT," +
		"DEPOSITVALIDTIME TIMESTAMP (0)," +
		"SPENT BIGINT," +
		"CREATION_TIME TIMESTAMP (0)," +
		"ACCOUNTCERT VARCHAR(2000));",
		/*		"CREATE TABLE CASCADENAMES (" +
		"CASCADENUMBER SERIAL PRIMARY KEY," +
		"NAME VARCHAR(20));",

		"CREATE TABLE CASCADES (" +
		"CASCADENUMBER SERIAL REFERENCES CASCADENAMES ON DELETE CASCADE, " +
		"ACCOUNTNUMBER BIGINT REFERENCES ACCOUNTS ON DELETE CASCADE," +
		"COSTS BIGINT," +
		"PAYCOSTS INTEGER," +
		"USERCONFIRM VARCHAR(2000))",*/

		"CREATE TABLE COSTCONFIRMATIONS (" +
		"AiID VARCHAR(128)," +
		"ACCOUNTNUMBER BIGINT," +
		"TRANSFERREDBYTES BIGINT," +
		"XMLCC VARCHAR(1024))",

		"CREATE TABLE TRANSFERS (" +
		"TRANSFERNUMBER BIGSERIAL PRIMARY KEY," +
		"ACCOUNTNUMBER BIGINT REFERENCES ACCOUNTS ON DELETE CASCADE," +
		"DEPOSIT BIGINT," +
		"VALIDTIME TIMESTAMP (0)," +
		"USED BOOLEAN)",

		"CREATE TABLE RATES (" +
		"ID SERIAL PRIMARY KEY," +
		"NAME VARCHAR(32)," +
		"AMOUNT NUMERIC, FIXED_AMOUNT BOOLEAN," +
		"MBYTES INTEGER," +
		"VALID_DAYS INTEGER, VALID_MONTHS INTEGER);"
	};

	// Documentation see DBInterface / wird hoffentlich nicht mehr gebraucht /
	/*	public String getCert(IMyPublicKey pubkey) throws RequestException
	 {
	  LogHolder.log(LogLevel.DEBUG, LogType.PAY,"DataBase.getCert() called.");
	  String cert = null;
	  String exponent = pubkey.getPublicExponent().toString();
	  String modulus = pubkey.getModulus().toString();
	  try
	  {
	   Statement stmt = con.createStatement();
	   ResultSet rs =
	 stmt.executeQuery("SELECT ACCOUNTCERT FROM ACCOUNTS WHERE EXPONENT='" + exponent +
	 "' AND MODULUS='" + modulus + "'");
	   if (rs.next())
	   {
	 cert = rs.getString(1);
	   }
	   rs.close();
	   stmt.close();
	  }
	  catch (SQLException e)
	  {
	   LogHolder.log(LogLevel.EXCEPTION, LogType.PAY,e);
	   throw new RequestException(500);
	  }
	  return cert;
	 }*/

	// Documentation see DBInterface
	/*	public long getAccountNumber(IMyPublicKey pubkey) throws Exception
	 {
	  LogHolder.log(LogLevel.DEBUG, LogType.PAY, "DataBase.getAccountNumber() called.");
	  long accountnumber = 0;
	  String exponent = pubkey.getPublicExponent().toString();
	  String modulus = pubkey.getModulus().toString(); ;
	  try
	  {
	   Statement stmt = con.createStatement();
	   ResultSet rs =
	 stmt.executeQuery("SELECT ACCOUNTNUMBER FROM ACCOUNTS WHERE EXPONENT='" +
	 exponent + "' AND MODULUS= '" + modulus + "'");
	   if (rs.next())
	   {
	 accountnumber = rs.getLong(1);
	   }
	   rs.close();
	   stmt.close();
	  }
	  catch (Exception e)
	  {
	   LogHolder.log(LogLevel.EXCEPTION, LogType.PAY,e);
	   throw e;
	  }
	  return accountnumber;
	 }*/

	// Documentation see DBInterface
	public Balance getBalance(long accountnumber) throws Exception
	{
		LogHolder.log(LogLevel.DEBUG, LogType.PAY, "DataBase.getBalance() called for account " +
					  accountnumber + ".");
		Balance bal = null;
		long deposit;
		long spent;
		java.sql.Timestamp timestamp;
		java.sql.Timestamp validTime;

		Vector confirms = new Vector();
		try
		{
			// get balance and max balance
			Statement stmt = m_dbConn.createStatement();
			ResultSet rs =
				stmt.executeQuery("SELECT DEPOSIT,SPENT,DEPOSITVALIDTIME " +
								  "FROM ACCOUNTS WHERE ACCOUNTNUMBER=" + accountnumber);
			if (rs.next())
			{
				deposit = rs.getLong(1);
				spent = rs.getLong(2);
				validTime = (java.sql.Timestamp) rs.getObject(3);
			}
			else
			{
				throw new Exception("account no. " + accountnumber + " is not in database");
			}
			LogHolder.log(LogLevel.DEBUG, LogType.PAY, "Fetched deposit " + deposit + ", spent " + spent +
						  ", validTime " + validTime.toString() + " from DB.");

			// get user cost confirmations
			Statement stmt2 = m_dbConn.createStatement();
			ResultSet rs2 = stmt.executeQuery("SELECT XMLCC FROM COSTCONFIRMATIONS WHERE ACCOUNTNUMBER=" +
											  accountnumber);
			while (rs2.next())
			{
				confirms.add(rs2.getString(1));
			}

			bal = new Balance(deposit, spent,
							  new java.sql.Timestamp(System.currentTimeMillis()),
							  validTime, confirms);
			rs2.close();
			stmt2.close();
			rs.close();
			stmt.close();
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, e);
			throw e;
		}
		return bal;
	}

	/***************************************************************************
	 * DATABASE CLEANUP
	 ***************************************************************************/

	/** Run once a day/week/month (depending on server load) to keep
	 * the database small, clean and fast
	 *
	 * @author Bastian Voigt
	 */
	private class CleanupThread extends Thread
	{
		public void run()
		{
			LogHolder.log(LogLevel.DEBUG, LogType.PAY, "Thread successfully started");
			while (true)
			{
				try
				{
					Thread.sleep(1000 * 60 * 60 * 24 * 10); // do it every 10 days
				}
				catch (InterruptedException e)
				{
					break;
				}

				LogHolder.log(LogLevel.DEBUG, LogType.PAY, "Database.cleanup starting");
				try
				{
					Statement stmt = m_dbConn.createStatement();

					// delete old transfer numbers (invalid for more than 60 days)
					stmt.execute("DELETE FROM TRANSFERS WHERE VALIDTIME < '" +
								 new java.sql.Timestamp(System.currentTimeMillis() + 1000 * 60 * 60 * 24 * 60) +
								 "'");

					// delete old accounts that have not been used for
					// more than half a year.
					stmt.execute("DELETE FROM ACCOUNTS WHERE DEPOSITVALIDTIME < '" +
								 new java.sql.Timestamp(System.currentTimeMillis() -
						1000 * 60 * 60 * 24 * 150) + "'");

					// routine maintenance commands for postgresql
					stmt.execute("VACUUM"); // free unused diskspace
					stmt.execute("ANALYZE"); // make queries faster
				}
				catch (SQLException e)
				{
					LogHolder.log(LogLevel.ERR, LogType.PAY, "Error occured during database cleanup");
					LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, e);
				}
				LogHolder.log(LogLevel.DEBUG, LogType.PAY, "Database.cleanup done.");
			}
		}
	}

	public void startCleanupThread()
	{
		LogHolder.log(LogLevel.DEBUG, LogType.PAY, "Starting Cleanup Thread ...");
		Thread t = new CleanupThread();
		t.start();
	}

	/***************************************************************************
	 * ACCOUNT NUMBER HANDLING
	 ***************************************************************************/

	// Documentation see DBInterface
	public void addAccount(long accountNumber,
						   String xmlPublicKey,
						   java.sql.Timestamp creationTime,
						   String accountCert) throws Exception
	{
		LogHolder.log(LogLevel.DEBUG, LogType.PAY,
					  "DataBase.addAccount() called for accountnumber " + accountNumber);
		/**@todo Replace third argument with 0 when account charging is working*/
		String statement =
			"INSERT INTO ACCOUNTS VALUES (" +
			accountNumber + ",'" + xmlPublicKey +
			"',100000000,'" + creationTime + "',0,'" + creationTime + "','"
			+ accountCert + "')";
		try
		{
			Statement stmt = m_dbConn.createStatement();
			stmt.executeUpdate(statement);
			stmt.close();
		}
		catch (SQLException e)
		{
			LogHolder.log(LogLevel.ERR, LogType.PAY, "Could not create account no. " + accountNumber);
			throw new Exception();
		}
	}

	// Documentation see DBInterface class
	public long getNextAccountNumber() throws Exception
	{
		boolean weiter = true;
		long accountnum = 0;
		Statement stmt;
		ResultSet rs;
		try
		{
			while (weiter)
			{
				stmt = m_dbConn.createStatement();
				accountnum = rnd.nextLong();
				if (accountnum < 0)
				{
					accountnum *= -1;
				}
				while (accountnum > 999999999999l)
				{
					accountnum /= 10; // account accountnumbers should
				}
				if (accountnum < 100000000000l)
				{
					accountnum += 100000000000l; // always have twelve digits!
				}
				rs = stmt.executeQuery("select * from accounts where accountnumber=" + accountnum);
				weiter = rs.next();
			}
		}
		catch (SQLException e)
		{
			throw new Exception();
		}
		return accountnum;
	}

	//**************************************************************************
	 // TRANSFER NUMBER HANDLING
	 //**************************************************************************

	  // Documentation see DBInterface class
	  public long getNextTransferNumber() throws Exception
	  {
		  boolean weiter = true;
		  long transfernum = 0;
		  Statement stmt;
		  ResultSet rs;
		  try
		  {
			  while (weiter)
			  {
				  stmt = m_dbConn.createStatement();
				  transfernum = rnd.nextLong();
				  if (transfernum < 0)
				  {
					  transfernum *= -1;
				  }
				  while (transfernum > 99999999999l)
				  {
					  transfernum /= 10; // account transfernumbers should
				  }
				  if (transfernum < 10000000000l)
				  {
					  transfernum += 10000000000l; // always have twelve digits!

					  //add checksum digit
				  }
				  transfernum = calculateDiederChecksum(transfernum);

				  rs = stmt.executeQuery("select * from transfers where transfernumber=" + transfernum);
				  weiter = rs.next();
			  }
		  }
		  catch (SQLException e)
		  {
			  throw new Exception();
		  }
		  return transfernum;
	  }

	// Documentation see DBInterface class
	public void storeTransferNumber(long transfer_num,
									long account_num,
									long deposit,
									java.sql.Timestamp validTime) throws Exception
	{
		LogHolder.log(LogLevel.DEBUG, LogType.PAY, "DataBase.storeTransferNumber() called for transfer no. " +
					  transfer_num + ", account no. " + account_num);
		try
		{
			Statement stmt = m_dbConn.createStatement();
			stmt.executeUpdate("INSERT INTO TRANSFERS VALUES (" + transfer_num + "," +
							   account_num + "," + deposit + ",'" + validTime + "','f')");
			stmt.close();
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.ERR, LogType.PAY,
						  "DataBase.storeTransferNumber() Could not add transfer number " + transfer_num +
						  " to DB");
			LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, e);
			throw e;
		}
	}

	// Documentation see DBInterface
	public void setTransferNumberUsed(long transfer_num) throws Exception
	{
		try
		{
			Statement stmt = m_dbConn.createStatement();
			stmt.executeUpdate("UPDATE TRANSFERS SET USED='T' WHERE TRANSFERNUMBER=" +
							   transfer_num);
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, e);
			throw e;
		}
	}

	// Documentation see DBInterface class
	public String getXmlPublicKey(long accountnumber) throws Exception
	{
		LogHolder.log(LogLevel.DEBUG, LogType.PAY,
					  "DataBase.getPubKey() called for account no. " + accountnumber);
		String strXmlKey = null;
		Statement stmt = m_dbConn.createStatement();
		ResultSet rs = stmt.executeQuery("SELECT XMLPUBLICKEY FROM ACCOUNTS WHERE ACCOUNTNUMBER=" +
										 accountnumber);
		if (rs.next())
		{
			strXmlKey = rs.getString(1);
		}
		rs.close();
		stmt.close();

		return strXmlKey;
	}

	// Documentation see DBInterface class
	public void insertCC(XMLEasyCC cc) throws Exception
	{
		ResultSet rs;
		Statement stmt = m_dbConn.createStatement();

		String query =
			"INSERT INTO COSTCONFIRMATIONS VALUES ('" + cc.getAIName() + "', " +
			cc.getAccountNumber() + "," + cc.getTransferredBytes() + ",'" +
			XMLUtil.toString(XMLUtil.toXMLDocument(cc)) +
			"')";
		LogHolder.log(LogLevel.DEBUG, LogType.MISC, "Executing query: " + query);
		if (stmt.executeUpdate(query) != 1)
		{
			// error while updating DB
			throw new Exception("Wrong number of affected rows!");
		}

		// Update the SPENT value for the account
		query = "UPDATE ACCOUNTS SET SPENT = " +
			"(SELECT SUM(TRANSFERREDBYTES) FROM COSTCONFIRMATIONS WHERE ACCOUNTNUMBER=" +
			cc.getAccountNumber() + ")" +
			"WHERE ACCOUNTNUMBER=" + cc.getAccountNumber();
		if (stmt.executeUpdate(query) != 1)
		{
			// error
			throw new Exception("Wrong number of affected rows!");
		}
	}

	public void updateCC(XMLEasyCC cc) throws Exception
	{
		ResultSet rs;
		Statement stmt = m_dbConn.createStatement();

		String query =
			"UPDATE COSTCONFIRMATIONS SET TRANSFERREDBYTES=" + cc.getTransferredBytes() +
			",XMLCC='" + XMLUtil.toString(XMLUtil.toXMLDocument(cc)) +
			"' WHERE ACCOUNTNUMBER=" + cc.getAccountNumber() +
			" AND AiID='" + cc.getAIName() + "'";
		if (stmt.executeUpdate(query) != 1)
		{
			// error while updating DB
		}

		// Update the SPENT value for the account
		query = "UPDATE ACCOUNTS SET SPENT = " +
			"(SELECT SUM(TRANSFERREDBYTES) FROM COSTCONFIRMATIONS WHERE ACCOUNTNUMBER=" +
			cc.getAccountNumber() + ")" +
			"WHERE ACCOUNTNUMBER=" + cc.getAccountNumber();
		if (stmt.executeUpdate(query) != 1)
		{
			// error
		}
	}

	/*	public void storeCosts(XMLEasyCC cc) throws Exception
	 {

	  try
	  {
	   stmt = m_dbConn.createStatement();

	   // insert CC into the database. Determine if we make an update or insert...
	   query = "SELECT COUNT(*) FROM COSTCONFIRMATIONS WHERE ACCOUNTNUMBER=" + cc.getAccountNumber() +
		" AND AiID='" + cc.getAIName() + "'";
	   rs = stmt.executeQuery(query);
	   if (rs.next())
	   {
		if (rs.getInt(1) == 1)
		{
		}
		else
		{
		}
		if (stmt.executeUpdate(query) != 1)
		{
		 // error while updating DB
		}
	   }
	   else
	   {
		// error
	   }

	   // Update the SPENT value for the account
	   query = "UPDATE ACCOUNTS SET SPENT = " +
		"(SELECT SUM(TRANSFERREDBYTES) FROM COSTCONFIRMATIONS WHERE ACCOUNTNUMBER=" +
		cc.getAccountNumber() + ")" +
		"WHERE ACCOUNTNUMBER=" + cc.getAccountNumber();
	   if (stmt.executeUpdate(query) != 1)
	   {
		// error
	   }
	  }
	 }*/

	/*	public AccountSnapshot getAccountSnapshot(long accountNumber, String aiName) throws Exception
	 {
	  int costsAI;
	  int creditMax;
	  int credit;
	  try
	  {
	   // con.setAutoCommit(false);
	   Statement stmt = con.createStatement();
	   ResultSet rs =
		stmt.executeQuery("SELECT COSTS FROM CASCADES WHERE ACCOUNTNUMBER=" + accountNumber +
			  " AND CASCADENUMBER=(SELECT CASCADENUMBER FROM CASCADENAMES WHERE NAME='" +
			  aiName + "')");
	   if (rs.next())
	   {
		costsAI = rs.getInt(1);
	   }
	   else
	   {
		costsAI = 0;
	   }
	   stmt = con.createStatement(ResultSet.TYPE_FORWARD_ONLY,
				ResultSet.CONCUR_UPDATABLE);
	   rs = stmt.executeQuery("SELECT BALANCE, MAXBALANCE FROM ACCOUNTS WHERE ACCOUNTNUMBER=" +
			   accountNumber);
	   if (rs.next())
	   {
		credit = rs.getInt(1);
		creditMax = rs.getInt(2);
	   }
	   else
	   {
		credit = 0;
		creditMax = 0;
	   }

	   AccountSnapshot ksa = new AccountSnapshot(creditMax, credit, costsAI);
	   return ksa;
	  }
	  catch (SQLException e)
	  {
	   throw e;
	  }
	 }*/



	public XMLEasyCC getCC(long accountNumber, String aiName) throws Exception
	{
		Statement stmt = m_dbConn.createStatement();
		XMLEasyCC cc = null;
		ResultSet rs = stmt.executeQuery(
			"SELECT XMLCC FROM COSTCONFIRMATIONS " +
			  "WHERE ACCOUNTNUMBER=" + accountNumber +
			" AND AiID='" + aiName + "'");
		if (rs.next())
		{
			cc = new XMLEasyCC(rs.getString(1));
		}
		return cc;
	}

	/*	public long getCosts(long accountNumber, String aiName) throws Exception
	 {
	  long costs;
	  try
	  {
	   Statement stmt = m_dbConn.createStatement();
	   ResultSet rs =
		stmt.executeQuery("SELECT COSTS FROM CASCADES WHERE ACCOUNTNUMBER=" + accountNumber +
			  " AND CASCADENUMBER=(SELECT CASCADENUMBER FROM CASCADENAMES WHERE NAME='" +
			  aiName + "')");
	   if (rs.next())
	   {
		costs = rs.getInt(1);
	   }
	   else
	   {
		costs = 0;
	   }
	   return costs;
	  }
	  catch (SQLException e)
	  {
	   throw e;
	  }

	 }*/

	/*	public long getPayCosts(long accountNumber, String aiName) throws Exception
	 {
	  long costs;
	  try
	  {
	   Statement stmt = m_dbConn.createStatement();
	   ResultSet rs =
		stmt.executeQuery("SELECT PAYCOSTS FROM CASCADES WHERE ACCOUNTNUMBER=" + accountNumber +
			  " AND CASCADENUMBER=(SELECT CASCADENUMBER FROM CASCADENAMES WHERE NAME='" +
			  aiName + "')");
	   if (rs.next())
	   {
		costs = rs.getInt(1);
	   }
	   else
	   {
		costs = 0;
	   }
	   return costs;
	  }
	  catch (SQLException e)
	  {
	   throw e;
	  }

	 }*/

	// Documentation see DBInterface class
	public void createTables()
	{
		Statement stmt;
		int num_tables = create_statements.length;

		for (int i = 0; i < num_tables; i++)
		{
			try
			{
				LogHolder.log(LogLevel.DEBUG, LogType.PAY,
							  "Creating Database " + db_tables[i]);
				stmt = m_dbConn.createStatement();
				stmt.executeUpdate(create_statements[i]);
			}
			catch (Exception e)
			{
				LogHolder.log(LogLevel.ERR, LogType.PAY,
							  "DataBase.createTables: Could not create " + db_tables[i]);
				LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, e);
			}
		}
	}

	/** Drops all tables */
	public void dropTables()
	{
		Statement stmt;

		int num_objects = db_tables.length;

		for (int i = 0; i < num_objects; i++)
		{
			try
			{
				stmt = m_dbConn.createStatement();
				LogHolder.log(LogLevel.DEBUG, LogType.PAY,
							  "Dropping old Database " + db_tables[i]);
				stmt.executeUpdate("DROP " + db_tables[i]);
			}
			catch (Exception e)
			{
				LogHolder.log(LogLevel.ERR, LogType.PAY,
							  "DataBase.dropTables: Could not drop " + db_tables[i] + "!");
				LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, e);
			}
		}
	}

	/*   protected void finalize () throws Throwable
	  {
	 try {
	 con.close();
	 }
	 catch (Exception e)
	 {
	  LogHolder.log(LogLevel.EXCEPTION, LogType.PAY,e);
	 }
	 super.finalize();
	  }*/

	public void chargeAccount(long a_transferNumber, long a_amount)
	{
		Statement stmt;
		long account;
		boolean used;
		try
		{
			//Get account for transfernumber
			stmt = m_dbConn.createStatement();
			ResultSet r = stmt.executeQuery("SELECT ACCOUNTNUMBER, USED FROM TRANSFERS WHERE TRANSFERNUMBER=" +
											a_transferNumber);
			if (r.next())
			{
				account = r.getLong(1);
				used = r.getBoolean(2);
			}
			else
			{
				throw new Exception("Transfer no. " + a_transferNumber + " is not in database");
			}
			LogHolder.log(LogLevel.DEBUG, LogType.PAY, "Fetched account no. " + account);
			//Update deposit
			if (!used)
			{
				stmt = m_dbConn.createStatement();
				stmt.executeUpdate("UPDATE ACCOUNTS SET DEPOSIT=DEPOSIT+" + a_amount + "WHERE ACCOUNTNUMBER=" +
								   account);
				//Set transfer number to "used"
				stmt = m_dbConn.createStatement();
				stmt.executeUpdate("UPDATE TRANSFERS SET USED=TRUE WHERE TRANSFERNUMBER=" +
								   a_transferNumber);
			}
			else
			{
				LogHolder.log(LogLevel.ERR, LogType.PAY, "Transfer number already used.");
			}
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, "Could not charge account: " + e.getMessage());

		}
	}

}
