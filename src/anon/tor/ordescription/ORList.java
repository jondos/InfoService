package anon.tor.ordescription;

import java.io.LineNumberReader;
import java.io.StringReader;
import java.util.Hashtable;
import java.util.Vector;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import anon.tor.MyRandom;
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
	private MyRandom m_rand;
	private ORListFetcher m_orlistFetcher;
	/**
	 * constructor
	 *
	 */
	public ORList(ORListFetcher fetcher)
	{
		m_onionrouters=new Vector();
		m_onionroutersWithNames=new Hashtable();
		m_orlistFetcher=fetcher;
		m_rand=new MyRandom();
	}

	public synchronized int size()
	{
		return m_onionrouters.size();
	}

	public synchronized void setFetcher(ORListFetcher fetcher)
	{
		m_orlistFetcher=fetcher;
	}
	/** Updates the list of available ORRouters.
	 * @return true if it was ok, false otherwise
	 */
	public synchronized boolean updateList()
	{
		try
		{
			String doc=m_orlistFetcher.getORList();
			parseDocument(doc);
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
		return  (Vector)m_onionrouters.clone();
	}

   	public synchronized ORDescription getByName(String name)
	   {
		   return (ORDescription)m_onionroutersWithNames.get(name);
	   }

	/**
	 * selects a OR randomly from a given list of allowed OR names
	 * @param orlist list of onionrouter names
	 * @return
	 */

	public synchronized ORDescription getByRandom(Vector allowedNames)
	{
		String orName = (String) allowedNames.elementAt( (m_rand.nextInt(allowedNames.size())));
		return getByName(orName);
	}

	/**
	 * selects a OR randomly
	 * @return
	 */
	public synchronized ORDescription getByRandom()
	{
		return (ORDescription) this.m_onionrouters.elementAt(m_rand.nextInt(m_onionrouters.size()));
	}
	/**
	 * returns a ORDescription to the given ORName
	 * @param name
	 * ORName
	 * @return
	 * ORDescription if the OR exist, null else
	 */
	public synchronized ORDescription getORDescription(String name)
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
					if ((strRunningOrs.indexOf(" " + ord.getName() + " ") > 0)/*&&(ord.getSoftware().startsWith("Tor 0.0.8"))*/)
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
