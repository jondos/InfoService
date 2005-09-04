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

public class ORList
{

	private Vector m_onionrouters;
	private Vector m_exitnodes;
	private Vector m_middlenodes;
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
		m_exitnodes = new Vector();
		m_middlenodes = new Vector();
		m_onionroutersWithNames = new Hashtable();
		m_orlistFetcher = fetcher;
		m_rand = new MyRandom();
	}

	/**
	 * size of the ORList
	 * @return
	 * number of routers in the list
	 */
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
			if (doc == null)
			{
				return false;
			}
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
		return (Vector) m_onionrouters.clone();
	}

	/**
	 * gets the date when the List was pubished
	 * @return
	 * date
	 */
	public Date getPublished()
	{
		return m_datePublished;
	}

	/**
	 * gets an onion router by it's name
	 * @param name
	 * name of the OR
	 * @return
	 * ORDescription of the onion router
	 */
	public synchronized ORDescription getByName(String name)
	{
		return (ORDescription) m_onionroutersWithNames.get(name);
	}

	/**
	 * removes an onion router
	 * @param name
	 * name of the OR
	 */
	public synchronized void remove(String name)
	{
		ORDescription ord = getByName(name);
		m_onionrouters.removeElement(ord);
		if(ord.isExitNode())
		{
			m_exitnodes.removeElement(ord);
		} else
		{
			m_middlenodes.removeElement(ord);
		}
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
	 * selects a OR randomly
	 * tries to blanace the probability of exit and non-exit nodes
	 * @param length
	 * length of the circuit
	 * @return
	 */
	public synchronized ORDescription getByRandom(int length)
	{
		//we know that the last node is an exit node, so we have to calculate a new probaility
		//p(x') = (p(x)-1/length)*(length/(length-1))
		//p(x) ... probability for exit nodes    p(x') ... new probability for exit nodes
		//p(x) = exit_nodes/number_of_routers
		int number_of_routers = m_onionrouters.size();
		int numerator = length*m_exitnodes.size() - number_of_routers;
		int denominator = (length-1)*number_of_routers;

		//TODO: line can be removed if tor balance exit nodes and middlerouters in the right way
		//we double the probability of middlerouters, because original tor doesn't use them so often
		denominator *=2;

		if(m_rand.nextInt(denominator)>numerator)
		{
			return (ORDescription)this.m_middlenodes.elementAt(m_rand.nextInt(m_middlenodes.size()));
		} else
		{
			return (ORDescription)this.m_exitnodes.elementAt(m_rand.nextInt(m_exitnodes.size()));
		}
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
		Vector exitnodes = new Vector();
		Vector middlenodes = new Vector();
		Hashtable orswn = new Hashtable();
		LineNumberReader reader = new LineNumberReader(new StringReader(strDocument));
		String strRunningOrs = " ";
		Date published = null;
		for (; ; )
		{
			reader.mark(200);
			String aktLine = reader.readLine();
			if (aktLine == null)
			{
				break;
			}
			//remove "opt"(optional) in front of line
			if(aktLine.startsWith("opt "))
			{
				aktLine=aktLine.substring(4,aktLine.length());
			}
			//TODO: can be removed when no routers version <0.0.9pre5 are used in tor
			if (aktLine.startsWith("running-routers"))
			{
				strRunningOrs = aktLine + " ";
			}
			//new in version 0.0.9pre5 - added instead of running-routers line
			else if (aktLine.startsWith("router-status"))
			{
				strRunningOrs = " ";
				StringTokenizer st = new StringTokenizer(aktLine, " ");
				String token = st.nextToken();
				while (st.hasMoreTokens())
				{
					token = st.nextToken();
					//check if router is running
					if (!token.startsWith("!"))
					{
						//check if the router is verified
						if (!token.startsWith("$"))
						{
							strRunningOrs += (new StringTokenizer(token, "=")).nextToken() + " ";
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
					if ( (strRunningOrs.indexOf(" " + ord.getName() + " ") >=
						  0) /*&&(ord.getSoftware().startsWith("Tor 0.0.8"))*/)
					{
						if(ord.isExitNode())
						{
							exitnodes.addElement(ord);
						} else
						{
							middlenodes.addElement(ord);
						}
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
		m_exitnodes = exitnodes;
		m_middlenodes = middlenodes;
		LogHolder.log(LogLevel.DEBUG, LogType.MISC, "Exit Nodes : "+exitnodes.size()+" Non-Exit Nodes : "+middlenodes.size());
		m_onionrouters = ors;
		m_onionroutersWithNames = orswn;
		m_datePublished = published;
	}
}
