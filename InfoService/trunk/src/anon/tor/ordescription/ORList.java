/*
 Copyright (c) 2004, The JAP-Team
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
package anon.tor.ordescription;

import java.io.LineNumberReader;
import java.io.StringReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Hashtable;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.Vector;
import anon.crypto.MyRandom;
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
	private MyRandom m_rand;
	private ORListFetcher m_orlistFetcher;
	private Date m_datePublished;
	private final static DateFormat ms_DateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
	static
	{
		ms_DateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));
	}

	/**
	 * constructor
	 *
	 */
	public ORList(ORListFetcher fetcher)
	{
		m_onionrouters = new Vector();
		m_onionroutersWithNames = new Hashtable();
		m_orlistFetcher = fetcher;
		m_rand = new MyRandom();
	}

	public synchronized int size()
	{
		return m_onionrouters.size();
	}

	public synchronized void setFetcher(ORListFetcher fetcher)
	{
		m_orlistFetcher = fetcher;
	}

	/** Updates the list of available ORRouters.
	 * @return true if it was ok, false otherwise
	 */
	public synchronized boolean updateList()
	{
		try
		{
			String doc = m_orlistFetcher.getORList();
			if(doc==null)
				return false;
			parseDocument(doc);
			return true;
		}
		catch (Throwable t)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.MISC,
						  "There was a problem with fetching the available ORRouters: " + t.getMessage());
			t.printStackTrace();
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
		return (Vector) m_onionrouters.clone();
	}

	public Date getPublished()
	{
		return m_datePublished;
	}

	public synchronized ORDescription getByName(String name)
	{
		return (ORDescription) m_onionroutersWithNames.get(name);
	}

	public synchronized void remove(String name)
	{
		m_onionrouters.removeElement(getByName(name));
		m_onionroutersWithNames.remove(name);
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
		return (ORDescription)this.m_onionrouters.elementAt(m_rand.nextInt(m_onionrouters.size()));
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
		Date published = null;
		for (; ; )
		{
			reader.mark(200);
			String aktLine = reader.readLine();
			if (aktLine == null)
			{
				break;
			}
			//can be removed when no routers version <0.0.9pre5 are used in tor
			if (aktLine.startsWith("running-routers"))
			{
				strRunningOrs = aktLine + " ";
			}
			//new in version 0.0.9pre5 - added instead of running-routers line
			else if(aktLine.startsWith("opt router-status")||aktLine.startsWith("router-status"))
			{
				strRunningOrs = "";
				StringTokenizer st = new StringTokenizer(aktLine," ");
				String token = st.nextToken();
				if(!token.toLowerCase().equals("router-status"))
				{
					token = st.nextToken();
				}
				while(st.hasMoreTokens())
				{
					token = st.nextToken();
					//check if router is running
					if(!token.startsWith("!"))
					{
						//check if the router is verified
						if(!token.startsWith("$"))
						{
							strRunningOrs +=	(new StringTokenizer(token,"=")).nextToken() +" ";
						}
					}
				}
			}
			else if (aktLine.startsWith("router"))
			{
				reader.reset();
				ORDescription ord = ORDescription.parse(reader);
				if (ord != null)
				{
					if ( (strRunningOrs.indexOf(" " + ord.getName() + " ") >
						  0) /*&&(ord.getSoftware().startsWith("Tor 0.0.8"))*/)
					{
						ors.addElement(ord);
						orswn.put(ord.getName(), ord);
						LogHolder.log(LogLevel.DEBUG, LogType.MISC, "Added: " + ord);
					}
				}
			}
			else if (aktLine.startsWith("published"))
			{
				StringTokenizer st = new StringTokenizer(aktLine, " ");
				st.nextToken(); //skip "published"
				String strPublished = st.nextToken(); //day
				strPublished += " " + st.nextToken(); //time
				published = ms_DateFormat.parse(strPublished);
			}
		}
		m_onionrouters = ors;
		m_onionroutersWithNames = orswn;
		m_datePublished = published;
	}
}
