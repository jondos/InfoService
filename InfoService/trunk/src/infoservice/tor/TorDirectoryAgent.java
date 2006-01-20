/*
 Copyright (c) 2000 - 2004 The JAP-Team
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
package infoservice.tor;

import java.util.Enumeration;
import java.util.Vector;

import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import anon.tor.ordescription.ORDescription;
import anon.tor.ordescription.ORList;
import anon.infoservice.Database;
import anon.util.Base64;
import anon.util.BZip2Tools;
import anon.util.XMLUtil;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/**
 * This class is responsible for fetching the information about the active tor nodes. This class
 * is a singleton.
 */
public class TorDirectoryAgent implements Runnable
{

	/**
	 * The filename where we can find the TOR nodes file on a TOR directory server.
	 */
	private static final String DEFAULT_DIRECTORY_FILE = "/dir.z";

	/**
	 * Stores the instance of TorDirectoryAgent (singleton).
	 */
	private static TorDirectoryAgent ms_tdaInstance;

	/**
	 * Stores the XML container with the current tor nodes list. If we don't have a topical list,
	 * this value is null.
	 */
	private Node m_currentTorNodesList;

	/**
	 * Stores the XML container with the current BZip2 compressed tor nodes list. If we don't have
	 * a topical list, this value is null.
	 */
	private Node m_currentCompressedTorNodesList;

	/**
	 * Stores the cycle time (in milliseconds) for updating the tor nodes list. Until the update
	 * thread is started, this value is -1.
	 */
	private long m_updateInterval;

	/**
	 * Returns the instance of TorDirectoryAgent (singleton). If there is no instance, a new one is
	 * created.
	 *
	 * @return The TorDirectoryAgent instance.
	 */
	public static TorDirectoryAgent getInstance()
	{
		if (ms_tdaInstance == null)
		{
			ms_tdaInstance = new TorDirectoryAgent();
		}
		return ms_tdaInstance;
	}

	/**
	 * Creates a new instance of TorDirectoryAgent. We do some initialization here.
	 */
	private TorDirectoryAgent()
	{
		m_currentTorNodesList = null;
		m_updateInterval = -1;
	}

	/**
	 * Adds a TorDirectoryServer to the database of known tor directory servers. This method can be
	 * used to add default directory servers (the expire time should be set to a nearly infinite
	 * value).
	 *
	 * @param a_torDirectoryServer The tor directory server to add.
	 */
	public void addTorDirectoryServer(TorDirectoryServer a_torDirectoryServer)
	{
		Database.getInstance(TorDirectoryServer.class).update(a_torDirectoryServer);
		LogHolder.log(LogLevel.DEBUG, LogType.NET,
					  "TorDirectoryAgent: addTorDirectoryServer: " + a_torDirectoryServer.getId() +
					  " was updated or added to the list of known TOR directory servers.");
	}

	/**
	 * This starts the internal tor nodes list update thread. You have to call this method exactly
	 * once after the creation of this TorDirectoryAgent. After the update thread is started once,
	 * later calls of this method are ignored.
	 *
	 * @param a_updateInterval The cycle time in milliseconds for fetching the current list of the
	 *                         tor nodes.
	 */
	public void startUpdateThread(long a_updateInterval)
	{
		synchronized (this)
		{
			/* we need exclusive access */
			if ( (m_updateInterval == -1) && (a_updateInterval > 0))
			{
				m_updateInterval = a_updateInterval;
				/* start the internal thread */
				Thread fetchThread = new Thread(this);
				fetchThread.setDaemon(true);
				fetchThread.start();
			}
		}
	}

	/**
	 * Returns the XML container with the current tor nodes list. If we don't have a topical list,
	 * the returned value is null.
	 *
	 * @return The XML structur with the current tor nodes list or null, if we don't have a topical
	 *         list.
	 */
	public Node getTorNodesList()
	{
		Node torNodes = null;
		synchronized (this)
		{
			/* fetch always the current list */
			torNodes = m_currentTorNodesList;
		}
		return torNodes;
	}

	/**
	 * Returns the XML container with the current BZip2 compressed tor nodes list (same uncompressed
	 * data as obtained by the getTorNodesList() call). If we don't have a topical list, the
	 * returned value is null.
	 *
	 * @return The XML structur with the current BZip2 compressed tor nodes list or null, if we
	 *         don't have a topical list.
	 */
	public Node getCompressedTorNodesList()
	{
		Node torNodes = null;
		synchronized (this)
		{
			/* fetch always the current list */
			torNodes = m_currentCompressedTorNodesList;
		}
		return torNodes;
	}

	/**
	 * This is the implementation of the tor nodes list update thread.
	 */
	public void run()
	{
		TorDirectoryServer preferedDirectoryServer = null;
		while (true)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.NET,
						  "TorDirectoryAgent: run: Try to fetch the tor nodes list from the known tor directory servers.");
			Vector torServers = Database.getInstance(TorDirectoryServer.class).getEntryList();
			if (preferedDirectoryServer != null)
			{
				/* remove the occurence of the prefered directoy server from the list, if there is one */
				boolean preferedServerFound = false;
				int i = 0;
				while ( (i < torServers.size()) && (preferedServerFound == false))
				{
					if ( ( (TorDirectoryServer) (torServers.elementAt(i))).getId().equals(
						preferedDirectoryServer.getId()))
					{
						torServers.removeElementAt(i);
						preferedServerFound = true;
					}
					i++;
				}
				/* add the prefered directory server at the top of the list, so it is used first */
				torServers.insertElementAt(preferedDirectoryServer, 0);
			}
			Element torNodesListNode = null;
			Element torNodesListCompressedNode = null;
			//all servers timed out --> restart with configured ones...
			//if(torServers.size()==0)
			//{
			//}
			while ( (torNodesListNode == null) && (torServers.size() > 0))
			{
				TorDirectoryServer currentDirectoryServer = (TorDirectoryServer) (torServers.firstElement());
				String torNodesListInformation = currentDirectoryServer.downloadTorNodesInformation();
				torServers.removeElementAt(0);
				if (torNodesListInformation != null)
				{
					/* we have gotten a structure of TOR nodes, try to parse it */
					ORList torNodes = new ORList(new DummyORListFetcher(torNodesListInformation));
					if (torNodes.updateList())
					{
						/* we have a parsed list of TOR nodes, use it to update the database of known TOR
						 * directory servers
						 */
						Enumeration runningTorNodes = torNodes.getList().elements();
						while (runningTorNodes.hasMoreElements())
						{
							ORDescription currentTorNode = (ORDescription) (runningTorNodes.nextElement());
							if (currentTorNode.getDirPort() > 0)
							{
								/* that is a TOR directory server, add it to the database with a timeout of
								 * 1,5 * update interval
								 */
								addTorDirectoryServer(new TorDirectoryServer(new TorDirectoryServerUrl(
									currentTorNode.getAddress(), currentTorNode.getDirPort(),
									DEFAULT_DIRECTORY_FILE), (3 * m_updateInterval) / 2, false));
							}
						}

						try
						{
							/* create the TorNodesList XML structure for the clients */
							/* create the TorNodesList element */
							torNodesListNode = XMLUtil.createDocument().createElement("TorNodesList");
							torNodesListNode.setAttribute("xml:space", "preserve");
							XMLUtil.setValue(torNodesListNode, torNodesListInformation);
							/* create also a compressed version, which needs less transfer capacity */
							/* create the TorNodesListCompressed element */
							torNodesListCompressedNode = XMLUtil.createDocument().createElement(
								"TorNodesListCompressed");
							torNodesListCompressedNode.setAttribute("xml:space", "preserve");
							String bzip2CompressedTorNodesList = Base64.encode(BZip2Tools.compress(
								torNodesListInformation.getBytes()), false);
							XMLUtil.setValue(torNodesListCompressedNode, bzip2CompressedTorNodesList);
							/* downloading the information was successful from that server, so update the prefered
							 * TOR directory server entry, if it was another one
							 */
							if (preferedDirectoryServer != null)
							{
								if (preferedDirectoryServer.getId().equals(currentDirectoryServer.getId()) == false)
								{
									preferedDirectoryServer = currentDirectoryServer;
									LogHolder.log(LogLevel.INFO, LogType.NET,
												  "TorDirectoryAgent: run: Prefered TOR directory server is now: " +
												  preferedDirectoryServer.getId());
								}
							}
							else
							{
								/* there was no prefered directory server until yet */
								preferedDirectoryServer = currentDirectoryServer;
								LogHolder.log(LogLevel.INFO, LogType.NET,
											  "TorDirectoryAgent: run: Prefered TOR directory server is now: " +
											  preferedDirectoryServer.getId());
							}
						}
						catch (Exception e)
						{
							LogHolder.log(LogLevel.ERR, LogType.MISC,
										  "TorDirectoryAgent: run: Error while creating the XML structure with the TOR nodes list: " +
										  e.toString());
							torNodesListNode = null;
						}
					}
				}
			}
			if (torNodesListNode == null)
			{
				LogHolder.log(LogLevel.ERR, LogType.NET,
							  "TorDirectoryAgent: run: Could not fetch the tor nodes list from the known tor directory servers.");
				if (preferedDirectoryServer != null)
				{
					/* remove the prefered directory server, because it is not working any more */
					preferedDirectoryServer = null;
					LogHolder.log(LogLevel.INFO, LogType.NET,
								  "TorDirectoryAgent: run: Prefered TOR directory server reset.");
				}
				/** @todo maybe it would be a good idea to ask all neighbour infoservices */
			}
			if (torNodesListNode != null)
			{
				LogHolder.log(LogLevel.DEBUG, LogType.NET,
							  "TorDirectoryAgent: run: Fetched the list of tor nodes successfully.");
			}
			synchronized (this)
			{
				/* we need exclusive access, if we don't have a new tor nodes list, we set the value to
				 * null -> asking clients get a http error and ask another infoservice
				 */
				m_currentTorNodesList = torNodesListNode;
				m_currentCompressedTorNodesList = torNodesListCompressedNode;
			}
			try
			{
				Thread.sleep(m_updateInterval);
			}
			catch (Exception e)
			{
			}
		}
	}

}
