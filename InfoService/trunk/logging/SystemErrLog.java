package logging;

public class SystemErrLog extends DummyLog
{
	public void log(int level, int type, String msg)
	{
		System.err.println(msg);
	}

}
