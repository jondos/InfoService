package anon.mixminion;

import anon.AnonChannel;
import java.io.InputStream;
import java.io.OutputStream;

/** This class implements a channel,which speaks SMTP*/
public class MixminionSMTPChannel implements AnonChannel
{
	public InputStream getInputStream()
	{
		return null;
	}

	public OutputStream getOutputStream()
	{
		return null;
	}

	public int getOutputBlockSize()
	{
		return 0;
	}

	public void close()
	{
	}
}
