package anon.tor.ordescription;

import java.io.LineNumberReader;
import java.io.StringReader;
import java.util.Hashtable;
import java.util.Vector;
import HTTPClient.HTTPConnection;
import HTTPClient.HTTPResponse;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/*
 * Created on Mar 25, 2004
 *
 */

/**
 * @author stefan
 *

 */
public class ORList
{

	private Vector m_onionrouters;
	private Hashtable m_onionroutersWithNames;

	/**
	 * constructor
	 *
	 */
	public ORList()
	{
	}

	/** Updates the list of available ORRouters.
	 * @return true if it was ok, false otherwise
	 */
	public boolean updateList(String server, int port)
	{
		try
		{
			LogHolder.log(LogLevel.DEBUG, LogType.MISC,
						  "[UPDATE OR-LIST] Starting update on " + server + ":" + port);
			HTTPConnection http = new HTTPConnection(server, port);
			HTTPResponse resp = http.Get("/");
			if (resp.getStatusCode() != 200)
			{
				return false;
			}
			String doc = resp.getText();
			LogHolder.log(LogLevel.DEBUG, LogType.MISC, "ORList: " + doc);
			parseDocument(doc);
			LogHolder.log(LogLevel.DEBUG, LogType.MISC, "[UPDATE OR-LIST] Update finished");
			return true;
		}
		catch (Throwable t)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.MISC,
						  "There was a problem with fetching the available ORRouters: " + t.getMessage());
		}
		return false;
	}

	/**
	 * returns a List of all onionrouters
	 * @return
	 * List of ORDescriptions
	 */
	public Vector getList()
	{
		return this.m_onionrouters;
	}

	/**
	 * returns a ORDescription to the given ORName
	 * @param name
	 * ORName
	 * @return
	 * ORDescription if the OR exist, null else
	 */
	public ORDescription getORDescription(String name)
	{
		if (this.m_onionroutersWithNames.containsKey(name))
		{
			return (ORDescription)this.m_onionroutersWithNames.get(name);
		}
		return null;
	}

	/**
	 * parses the document and creates a list with all ORDescriptions
	 * @param strDocument
	 * @throws Exception
	 */
	private void parseDocument(String strDocument) throws Exception
	{
		Vector ors = new Vector();
		Hashtable orswn = new Hashtable();
		LineNumberReader reader = new LineNumberReader(new StringReader(strDocument));
		String strRunningOrs = "";
		for (; ; )
		{
			reader.mark(200);
			String aktLine = reader.readLine();
			if (aktLine == null)
			{
				break;
			}
			if (aktLine.startsWith("running-routers"))
			{
				strRunningOrs = aktLine+" ";
			}
			else if (aktLine.startsWith("router"))
			{
				reader.reset();
				ORDescription ord = ORDescription.parse(reader);
				if (ord != null)
				{
					if (strRunningOrs.indexOf(" " + ord.getName() + " ") > 0)
					{
						ors.addElement(ord);
						orswn.put(ord.getName(), ord);
						LogHolder.log(LogLevel.DEBUG, LogType.MISC, "Added: " + ord);
					}
				}
			}
		}
		this.m_onionrouters = ors;
		this.m_onionroutersWithNames = orswn;
	}
}
