package jpi;

import jpi.db.DBSupplier;
import logging.*;

public class JPIMain
{
	public static void main(String argv[])
	{
		// wrong commandline arguments
		if ( (argv.length < 1) || (argv.length > 3))
		{
			usage();
		}

		// initialize logging
		SystemErrLog log1=new SystemErrLog();
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
                if(Configuration.getLogFileName()!=null)
                {
                    FileLog log2=new FileLog(Configuration.getLogFileName(),1000000,10);
                    log2.setLogType(LogType.ALL);
                    log2.setLogLevel(Configuration.getLogFileThreshold());
                    ChainedLog l=new ChainedLog(log1,log2);
                    LogHolder.setLogInstance(l);
                }
                log1.setLogLevel(Configuration.getLogStderrThreshold());
		// process command line args
		boolean newdb = false;
		boolean sslOn = false;
		if (argv.length == 2)
		{
			if (argv[1].equals("new"))
			{
				newdb = true;
			}
			else if (argv[1].equals("on"))
			{
				sslOn = true;
			}
			else
			{
				usage();
			}
		}
		else if (argv.length == 3)
		{
			if (argv[1].equals("new") && argv[2].equals("on"))
			{
				sslOn = true;
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
				LogHolder.log(LogLevel.INFO, LogType.PAY,"JPIMain: Recreating database tables...");
				DBSupplier.getDataBase().dropTables();
				DBSupplier.getDataBase().createTables();
			}

			// launch database maintenance thread
			DBSupplier.getDataBase().startCleanupThread();
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.ERR, LogType.PAY,"Could not connect to PostgreSQL database server");
			LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, e);
			System.exit(0);
		}

		// start PIServer for JAP connections
		LogHolder.log(LogLevel.INFO, LogType.PAY,"JPIMain: Launching PIServer for JAP connections");
		PIServer userServer = new PIServer(false, sslOn);
		Thread userThread = new Thread(userServer);
		userThread.start();

		// start PIServer for AI connections
		LogHolder.log(LogLevel.INFO, LogType.PAY,"JPIMain: Launching PIServer for AI connections on port ");
		PIServer aiServer = new PIServer(true, sslOn);
		Thread aiThread = new Thread(aiServer);
		aiThread.start();

		LogHolder.log(LogLevel.INFO, LogType.PAY,"Initialization complete, JPIMain Thread terminating");
	}

	private static void usage()
	{
		System.out.println("Usage: java JPIMain <configfile> [new] [on]");
		System.exit(0);
	}

}
