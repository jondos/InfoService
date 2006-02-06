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
package anon.mixminion.mmrdescription;

import java.io.LineNumberReader;
import java.io.StringReader;
import java.util.Date;
import java.util.Hashtable;
import java.util.Vector;

import anon.crypto.MyRandom;
import anon.mixminion.mmrdescription.MMRDescription;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

public class MMRList
{

	private Vector m_mixminionrouters; // all Routers
	private Vector m_middlenodes; //all but the exitnodes
	private Vector m_exitnodes; //nodes with smtp-availibility
	private Hashtable m_mixminionroutersWithNames;
	private MyRandom m_rand;
	private MMRListFetcher m_mmrlistFetcher;

	/**
	 * constructor
	 *
	 */
	public MMRList(MMRListFetcher fetcher)
	{
		m_mixminionrouters = new Vector();
		m_middlenodes = new Vector();
		m_exitnodes = new Vector();
		m_mixminionroutersWithNames = new Hashtable();
		m_mmrlistFetcher = fetcher;
		m_rand = new MyRandom();

	}

	/**
	 * size of the MMRList
	 * @return
	 * number of routers in the list
	 */
	public synchronized int size()
	{
		return m_mixminionrouters.size();
	}

	public synchronized void setFetcher(MMRListFetcher fetcher)
	{
		m_mmrlistFetcher = fetcher;
	}

	/** Updates the list of available MMRouters.
	 * @return true if it was ok, false otherwise
	 */
	public synchronized boolean updateList()
	{
		try
		{
			String doc = m_mmrlistFetcher.getMMRList();
			if (doc == null)
			{
				return false;
			}
			return parseDocument(doc);
		}
		catch (Throwable t)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.MISC,
						  "There was a problem with fetching the available MMRouters: " + t.getMessage());
		}
		return false;
	}

	/**
	 * returns a List of all Mixminionrouters
	 * @return
	 * List of MMRDescriptions
	 */
	public Vector getList()
	{
		return (Vector) m_mixminionrouters.clone();
	}

	/**
	 * gets an Mixminion router by it's name
	 * @param name
	 * name of the MMR
	 * @return
	 * MMRDescription of the onion router
	 */
	public synchronized MMRDescription getByName(String name)
	{
		return (MMRDescription) m_mixminionroutersWithNames.get(name);
	}

	/**
	 * removes an Mixminion router
	 * @param name
	 * name of the MMR
	 */
	public synchronized void remove(String name)
	{
		MMRDescription mmrd = getByName(name);
		m_mixminionrouters.removeElement(mmrd);
		if(mmrd.isExitNode())
		{
			m_exitnodes.removeElement(mmrd);
		} else
		{
			m_middlenodes.removeElement(mmrd);
		}
		m_mixminionroutersWithNames.remove(name);

	}

	/**
	 * selects a MMR randomly from a given list of allowed OR names
	 * @param mmrlist list of mixminionrouter names
	 * @return
	 */
	public synchronized MMRDescription getByRandom(Vector allowedNames)
	{
		return (MMRDescription) allowedNames.elementAt( (m_rand.nextInt(allowedNames.size())));
	}

	/**
	 * selects a MMR randomly
	 * @return
	 */
	public synchronized MMRDescription getByRandom()
	{
		return (MMRDescription) this.m_mixminionrouters.elementAt( (m_rand.nextInt(m_mixminionrouters.size())));
	}

	/**
	 * selects a Routing List randomly, last element is surely an exit-node
	 * tries to blanace the probability of exit and non-exit nodes
	 * @param hops int
	 * length of the circuit
	 * @return routers vector
	 */

	public synchronized Vector getByRandom(int hops, boolean fragmented)
	{
		Vector routers = new Vector();
		MMRDescription x = null;
		boolean contains = true;
		boolean frags = false;

		for (int i=0; i<hops-1; i++) {
			contains=true;
			while (contains) {
			x = getByRandom();
				contains = routers.contains(x);
			}
			routers.addElement(x);
		}

		contains = true;
		if (fragmented) {
			while (contains || !frags) {
				x = getByRandom(m_exitnodes);
				contains = routers.contains(x);
				frags = x.allowsFragmented();
		}
		}
		else {
			while (contains ) {
				x = getByRandom(m_exitnodes);
				contains = routers.contains(x);
			}
		}
		routers.addElement(x);
		System.out.println("Last Router frags: " +x.allowsFragmented() +"exit: " + x.isExitNode());

		return routers;
	}

	/**
	 * returns a MMRDescription to the given MMRName
	 * @param name
	 * MMRName
	 * @return
	 * MMRDescription if the MMR exist, null else
	 */
	public synchronized MMRDescription getMMRDescription(String name)
	{
		if (this.m_mixminionroutersWithNames.containsKey(name))
		{
			return (MMRDescription)this.m_mixminionroutersWithNames.get(name);
		}
		return null;
	}


	/**
	 * parses the document and creates a list with all MMRDescriptions
	 * @param strDocument
	 * @throws Exception
	 * @return false if document is not a valid directory, true otherwise
	 */

	private boolean parseDocument(String strDocument) throws Exception
	{
		Vector mmrs = new Vector();
		Vector mnodes = new Vector();
		Vector enodes = new Vector();

		Hashtable mmrswn = new Hashtable();
		LineNumberReader reader = new LineNumberReader(new StringReader(strDocument));
		Date published = null;
		String aktLine = reader.readLine();

		if(aktLine==null)
			return false;
		for (; ; )
		{
			aktLine = reader.readLine();
			if (aktLine == null)
			{
				break;
			}


			if (aktLine.startsWith("[Server]"))
			{
				MMRDescription mmrd = MMRDescription.parse(reader);
				if ((mmrd != null) && !mmrswn.containsKey(mmrd.getName()) /*&& mmrd.hasValidDates()*/)
				{
					if (mmrd.isExitNode()) enodes.addElement(mmrd);
					else mnodes.addElement(mmrd);

					mmrs.addElement(mmrd);
					mmrswn.put(mmrd.getName(), mmrd);
				}

				LogHolder.log(LogLevel.DEBUG, LogType.MISC, "Added: " + mmrd);
					}
		}

		m_middlenodes = mnodes;
		m_exitnodes = enodes;
		LogHolder.log(LogLevel.DEBUG, LogType.MISC, "ExitNodes : "+enodes.size()+" MiddleNodes : " +mnodes.size());
		m_mixminionrouters = mmrs;
		m_mixminionroutersWithNames = mmrswn;
		return true;
	}
}
