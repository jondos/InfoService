package logging;

public class SystemErrLog extends DummyLog
{
	public void log(int level, int type, String msg)
	{
		System.err.println(msg);
	}

	/** Get the current debug type.
	 */
	public int getLogType()
	{
		return LogType.ALL;
	}

	/** Get the current debug level.
	 */
	public int getLogLevel()
	{
		return LogLevel.DEBUG;
	}


}
