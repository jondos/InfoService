/*
 Copyright (c) 2000-2009, The JAP-Team
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

public class SystemErrLog implements Log
{
	private int m_logLevel;
	private int m_logType;

	public SystemErrLog()
	{
		this(LogLevel.DEBUG, LogType.ALL);
	}

	public SystemErrLog(int a_logLevel, int a_logType)
	{
		m_logLevel = a_logLevel;
		m_logType = a_logType;
	}

	public void log(int a_logLevel, int a_logType, String msg)
	{
		if ( (a_logLevel <= m_logLevel) && ( (a_logType & m_logType) == a_logType))
		{
			System.err.println("[" + LogLevel.STR_Levels[a_logLevel] + "] " + msg);
		}
	}

	public void setLogLevel(int a_logLevel)
	{
		if (a_logLevel >= 0 && a_logLevel < LogLevel.STR_Levels.length)
		{
			m_logLevel = a_logLevel;
		}
	}

	public void setLogType(int a_logType)
	{
		m_logType = a_logType;
	}

	/** Get the current debug type.
	 */
	public int getLogType()
	{
		return m_logType;
	}

	/** Get the current debug level.
	 */
	public int getLogLevel()
	{
		return m_logLevel;
	}

}
