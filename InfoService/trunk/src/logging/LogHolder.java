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
package logging;

import anon.util.Util;
import java.util.StringTokenizer;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.io.StringWriter;

/**
 * This class stores the Log instance.
 */
public final class LogHolder
{

	/// the lowest detail level that is possible
	public static final int DETAIL_LEVEL_LOWEST = 0;
	public static final int DETAIL_LEVEL_HIGH = 1;
	/// the highest detail level that is possible
	public static final int DETAIL_LEVEL_HIGHEST = 2;

	/**
	 * Stores the instance of LogHolder (Singleton).
	 */
	private static LogHolder ms_logHolderInstance;

	/**
	 * The current detail level of all log messages.
	 */
	private static int m_messageDetailLevel = DETAIL_LEVEL_LOWEST;

	/**
	 * Stores the Log instance.
	 */
	private Log m_logInstance;

	/**
	 * This creates a new instance of LogHolder. This is only used for setting some
	 * values. Use LogHolder.getInstance() for getting an instance of this class.
	 */
	private LogHolder()
	{
		m_logInstance = new DummyLog();
	}

	public void finalize()
	{
		if (equals(ms_logHolderInstance))
		{
			ms_logHolderInstance = null;
		}
		try
		{
			super.finalize();
		}
		catch (Throwable ex)
		{
		}
	}

	/**
	 * Sets the detail level of all log messages. Use one of the class constants to set it.
	 * @param a_messageDetailLevel the detail level of all log messages
	 */
	public static void setDetailLevel(int a_messageDetailLevel)
	{
		if (a_messageDetailLevel < DETAIL_LEVEL_LOWEST)
		{
			m_messageDetailLevel = DETAIL_LEVEL_LOWEST;
		}
		else if (a_messageDetailLevel > DETAIL_LEVEL_HIGHEST)
		{
			m_messageDetailLevel = DETAIL_LEVEL_HIGHEST;
		}
		else
		{
			m_messageDetailLevel = a_messageDetailLevel;
		}
	}

	/**
	 * Gets the detail level of all log messages.
	 * @return a_messageDetailLevel the detail level of all log messages
	 */
	public static int getDetailLevel()
	{
		return m_messageDetailLevel;
	}

	/**
	 * Write the log data for a Throwable to the Log instance.
	 *
	 * @param logLevel The log level (see constants in class LogLevel).
	 * @param logType The log type (see constants in class LogType).
	 * @param a_throwable a Throwable to log
	 */
	public static void log(int logLevel, int logType, Throwable a_throwable)
	{
		if (isLogged(logLevel, logType))
		{
			if (m_messageDetailLevel == DETAIL_LEVEL_LOWEST)
			{
				getInstance().getLogInstance().log(logLevel, logType, a_throwable.getMessage());
			}
			else if (m_messageDetailLevel == DETAIL_LEVEL_HIGH)
			{
				getInstance().getLogInstance().log(logLevel, logType, a_throwable.toString());
			}
			else
			{
				getInstance().getLogInstance().log(logLevel, logType, Util.getStackTrace(a_throwable));
			}
		}
	}

	/**
	 * Write the log data to the Log instance.
	 *
	 * @param logLevel The log level (see constants in class LogLevel).
	 * @param logType The log type (see constants in class LogType).
	 * @param message The message to log.
	 */
	public static void log(int logLevel, int logType, String message)
	{
		if (isLogged(logLevel, logType))
		{
			if (m_messageDetailLevel == DETAIL_LEVEL_LOWEST)
			{
				getInstance().getLogInstance().log(logLevel, logType, message);
			}
			else if (m_messageDetailLevel == DETAIL_LEVEL_HIGH)
			{
				getInstance().getLogInstance().log(logLevel, logType,
					Util.normaliseString(getCallingClassFile() + ": ", 40) + message);
			}
			else
			{
				getInstance().getLogInstance().log(logLevel, logType,
					Util.normaliseString(getCallingMethod() + ": ", 80) + message);
			}
		}
	}

	/**
	 * Sets the logInstance.
	 *
	 * @param logInstance The instance of a Log implementation.
	 */
	public static void setLogInstance(Log logInstance)
	{
		getInstance().m_logInstance = logInstance;
		if (getInstance().m_logInstance == null)
		{
			getInstance().m_logInstance = new DummyLog();
		}
	}

	/**
	 * Returns the instance of LogHolder (Singleton). If there is no instance,
	 * there is a new one created.
	 *
	 * @return The LogHolder instance.
	 */
	private static synchronized LogHolder getInstance()
	{
		if (ms_logHolderInstance == null)
		{
			ms_logHolderInstance = new LogHolder();
		}
		return ms_logHolderInstance;
	}

	/**
	 * Returns the logInstance. If the logInstance is not set, there is a new DummyLog instance
	 * returned.
	 *
	 * @return The current logInstance.
	 */
	private Log getLogInstance()
	{
		return m_logInstance;
	}

	private static boolean isLogged(int a_logLevel, int a_logType)
	{
		return (a_logLevel <= getInstance().getLogInstance().getLogLevel()) &&
			( (a_logType & getInstance().getLogInstance().getLogType()) == a_logType);
	}

	/**
	 * Returns the filename and line number of the calling method (from outside
	 * this class) in the form <Code> (class.java:<LineNumber>) </Code>.
	 * @return the filename and line number of the calling method
	 */
	private static String getCallingClassFile()
	{
		String strClassFile = getCallingMethod();
		strClassFile = strClassFile.substring(strClassFile.indexOf('('), strClassFile.indexOf(')') + 1);
		return strClassFile;
	}

	/**
	 * Returns the name, class, file and line number of the calling method (from outside
	 * this class) in the form <Code> package.class.method(class.java:<LineNumber>) </Code>.
	 * This method does need some processing time, as an exception with the stack trace is generated.
	 * @return the name, class and line number of the calling method
	 */
	private static String getCallingMethod()
	{
		StringTokenizer tokenizer;
		String strCurrentMethod = "";
		StringWriter swriter = new java.io.StringWriter();
		PrintWriter pwriter = new java.io.PrintWriter(swriter);

		new Exception().printStackTrace(pwriter);

		tokenizer = new StringTokenizer(swriter.toString());
		tokenizer.nextToken(); // jump over the exception message
		while (tokenizer.hasMoreTokens())
		{
			tokenizer.nextToken(); // jump over the "at"
			/* jump over all local class calls */
			if ( (strCurrentMethod = tokenizer.nextToken()).indexOf(LogHolder.class.getName()) < 0)
			{
				// this is the method that called us
				break;
			}
		}
		return strCurrentMethod;
	}
}
