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


/**
 * This class stores the Log instance.
 */
public final class LogHolder {

	/**
	 * Stores the instance of LogHolder (Singleton).
	 */
	private static LogHolder ms_logHolderInstance;


	/**
	 * Stores the Log instance.
	 */
	private Log m_logInstance;


	/**
	 * This creates a new instance of LogHolder. This is only used for setting some
	 * values. Use LogHolder.getInstance() for getting an instance of this class.
	 */
	private LogHolder() {
		m_logInstance = new DummyLog();
	}


	/**
	 * Returns the instance of LogHolder (Singleton). If there is no instance,
	 * there is a new one created.
	 *
	 * @return The LogHolder instance.
	 */
	public static LogHolder getInstance() {
		if (ms_logHolderInstance == null) {
			ms_logHolderInstance = new LogHolder();
		}
		return ms_logHolderInstance;
	}

	/**
	 * Write the log data to the Log instance. This is only a comfort function, which is a
	 * shortcut to LogHolder.getInstance().getLogInstance().log()
	 *
	 * @param logLevel The log level (see constants in class LogLevel).
	 * @param logType The log type (see constants in class LogType).
	 * @param message The message to log.
	 */
	public static void log(int logLevel, int logType, String message) {
		getInstance().getLogInstance().log(logLevel, logType, message);
	}


	/**
	 * Sets the logInstance.
	 *
	 * @param logInstance The instance of a Log implementation.
	 */
	public static void setLogInstance(Log logInstance) {
		getInstance().m_logInstance = logInstance;
		if(getInstance().m_logInstance==null)
			getInstance().m_logInstance=new DummyLog();
	}

	/**
	 * Returns the logInstance. If the logInstance is not set, there is a new DummyLog instance
	 * returned.
	 *
	 * @return The current logInstance.
	 */
	public Log getLogInstance() {
		return m_logInstance;
	}
}
