package anon.tor.ordescription;

import HTTPClient.HTTPConnection;
import HTTPClient.HTTPResponse;
import logging.*;
public class PlainORListFetcher implements ORListFetcher
{
	private String m_ORListServer;
	private int m_ORListPort;

	public PlainORListFetcher(String addr, int port)

	{
		m_ORListServer = addr;
		m_ORListPort = port;
	}

	public String getORList()
	{
		try
		{
			LogHolder.log(LogLevel.DEBUG, LogType.MISC,
						  "[UPDATE OR-LIST] Starting update on " + m_ORListServer + ":" + m_ORListPort);
			HTTPConnection http = new HTTPConnection(m_ORListServer, m_ORListPort);
			HTTPResponse resp = http.Get("/");
			if (resp.getStatusCode() != 200)
			{
				return null;
			}
			String doc = resp.getText();
			LogHolder.log(LogLevel.DEBUG, LogType.MISC, "[UPDATE OR-LIST] Update finished");
			return doc;
		}
		catch (Throwable t)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.MISC,
						  "There was a problem with fetching the available ORRouters: " + t.getMessage());
		}
		return null;
	}
}
