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
package jpi;

import jpi.db.DBSupplier;
import jpi.helper.PayPalHelper;
import logging.ChainedLog;
import logging.FileLog;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import logging.SystemErrLog;
import jpi.helper.ICreditCardHelper;
import java.util.Properties;

public class JPIMain
{
	public static void main(String argv[])
	{
		try
		{
			/* for running in non-graphic environments, we need the awt headless support, it is only
			 * available since Java 1.4, so running the service in a non-graphic environment is
			 * only possible with Java versions since 1.4
			 */
			/* set the property in Java 1.0 compatible style */
			Properties systemProperties = System.getProperties();
			systemProperties.put("java.awt.headless", "true");
			System.setProperties(systemProperties);
		}
		catch (Throwable t)
		{
		}
		// wrong commandline arguments
		if ( (argv.length < 1) || (argv.length > 3))
		{
			usage();
		}

		// initialize logging
		SystemErrLog log1 = new SystemErrLog();
		LogHolder.setLogInstance(log1);
		log1.setLogType(LogType.ALL);
		LogHolder.setDetailLevel(LogHolder.DETAIL_LEVEL_HIGHEST);

		// read config file
		if (!Configuration.init(argv[0]))
		{
			LogHolder.log(LogLevel.ERR, LogType.PAY,
						  "JPIMain: Error loading configuration, I'm going to die now");
			System.exit(0);
		}
		if (Configuration.getLogFileName() != null)
		{
			FileLog log2 = new FileLog(Configuration.getLogFileName(), 1000000, 10);
			log2.setLogType(LogType.ALL);
			log2.setLogLevel(Configuration.getLogFileThreshold());
			ChainedLog l = new ChainedLog(log1, log2);
			LogHolder.setLogInstance(l);
		}
		log1.setLogLevel(Configuration.getLogStderrThreshold());
		log1 = new SystemErrLog();
		LogHolder.setLogInstance(log1);
		log1.setLogType(LogType.ALL);
		LogHolder.setDetailLevel(LogHolder.DETAIL_LEVEL_HIGHEST);

		// process command line args
		boolean newdb = false;
		if (argv.length == 2)
		{
			if (argv[1].equals("new"))
			{
				newdb = true;
			}
			else
			{
				usage();
			}
		}
		else if (argv.length == 3)
		{
			if (argv[1].equals("new"))
			{
				newdb = true;
			}
			else
			{
				usage();
			}
		}

		// initialize database connection
		try
		{
			LogHolder.log(LogLevel.INFO, LogType.PAY, "Connecting to database");
			DBSupplier.initDataBase(
				Configuration.getDatabaseHost(),
				Configuration.getDatabasePort(),
				Configuration.getDatabaseName(),
				Configuration.getDatabaseUserName(),
				Configuration.getDatabasePassword()
				);

			if (newdb)
			{ // drop and recreate all tables
				LogHolder.log(LogLevel.INFO, LogType.PAY, "JPIMain: Recreating database tables...");
				DBSupplier.getDataBase().dropTables();
				DBSupplier.getDataBase().createTables();
			}

			// launch database maintenance thread
			DBSupplier.getDataBase().startCleanupThread();
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.ERR, LogType.PAY, "Could not connect to PostgreSQL database server");
			LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, e);
			System.exit(0);
		}

		// start PIServer for JAP connections
		LogHolder.log(LogLevel.INFO, LogType.PAY, "JPIMain: Launching PIServer for JAP connections");
		PIServer userServer = new PIServer(false);
		Thread userThread = new Thread(userServer);
		userThread.start();

		// start PIServer for AI connections
		LogHolder.log(LogLevel.INFO, LogType.PAY, "JPIMain: Launching PIServer for AI connections on port ");
		PIServer aiServer = new PIServer(true);
		Thread aiThread = new Thread(aiServer);
		aiThread.start();

		//start the credit card helper
		String strHelperClass = "jpi.helper." + Configuration.getCreditCardHelper() + "CreditCardHelper";
		LogHolder.log(LogLevel.INFO, LogType.PAY, "JPIMain: Launching CreditCardHelper: " + strHelperClass);
		try
		{
			Class helper = (Class) Class.forName(strHelperClass);
			ICreditCardHelper helperObj = (ICreditCardHelper) helper.newInstance();
			Thread helperThread = new Thread(helperObj);
			helperThread.start();
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.EXCEPTION, LogType.PAY,
						  "JPIMain: Could not load helper class: " + strHelperClass);
		}

		// start InfoService thread for InfoService connections
		LogHolder.log(LogLevel.INFO, LogType.PAY,
					  "JPIMain: Launching InfoService thread for InfoService connections");
		InfoServiceThread infoServer = new InfoServiceThread();
		Thread infoServiceThread = new Thread(infoServer);
		infoServiceThread.start();
		LogHolder.log(LogLevel.INFO, LogType.PAY, "Initialization complete, JPIMain Thread terminating");

		// start PayPalHelper thread for IPN acceptance
		LogHolder.log(LogLevel.INFO, LogType.PAY, "JPIMain: Launching PayPalHelper thread");
		PayPalHelper payPal = new PayPalHelper(Integer.parseInt(Configuration.getPayPalPort()));
		Thread payPalThread = new Thread(payPal);
		payPalThread.start();

		LogHolder.log(LogLevel.INFO, LogType.PAY, "Initialization complete, JPIMain Thread terminating");
	}

	private static void usage()
	{
		System.out.println("Usage: java JPIMain <configfile> [new]");
		System.exit(0);
	}

}
