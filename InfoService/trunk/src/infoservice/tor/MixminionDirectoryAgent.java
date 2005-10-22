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
import java.net.URL;
import anon.infoservice.ListenerInterface;
import anon.infoservice.HTTPConnectionFactory;

/**
 * This class is responsible for fetching the information about the active tor nodes. This class
 * is a singleton.
 */
public class MixminionDirectoryAgent implements Runnable
{

	/**
	 * The filename where we can find the TOR nodes file on a TOR directory server.
	 */
	private static final String DEFAULT_DIRECTORY_FILE = "/Directory.gz";

	/**
	 * Stores the instance of MixminionDirectoryAgent (singleton).
	 */
	private static MixminionDirectoryAgent ms_mdaInstance;

	private URL m_urlDirectoryServer = null;
	/**
	 * Stores the XML container with the current tor nodes list. If we don't have a topical list,
	 * this value is null.
	 */
	private Node m_currentMixminionNodesList;

	/**
	 * Stores the cycle time (in milliseconds) for updating the tor nodes list. Until the update
	 * thread is started, this value is -1.
	 */
	private long m_updateInterval;

	/**
	 * Returns the instance of MixminionDirectoryAgent (singleton). If there is no instance, a new one is
	 * created.
	 *
	 * @return The MixminionDirectoryAgent instance.
	 */
	public static MixminionDirectoryAgent getInstance()
	{
		if (ms_mdaInstance == null)
		{
			ms_mdaInstance = new MixminionDirectoryAgent();
		}
		return ms_mdaInstance;
	}

	/**
	 * Creates a new instance of MixminionDirectoryAgent. We do some initialization here.
	 */
	private MixminionDirectoryAgent()
	{
		m_currentMixminionNodesList = null;
		m_updateInterval = -1;
	}

	public void addDirectoryServer(URL directoryServer)
	{
		m_urlDirectoryServer=directoryServer;
	}
	/**
	 * This starts the internal mixminion nodes list update thread. You have to call this method exactly
	 * once after the creation of this MixminionDirectoryAgent. After the update thread is started once,
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
	public Node getMixminionNodesList()
	{
		return m_currentMixminionNodesList;
	}

	/**
	 * This is the implementation of the mixminion nodes list update thread.
	 */
	public void run()
	{
		while (true)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.NET,
						  "MixminionDirectoryAgent: run: Try to fetch the mixminion nodes list from the known mixminion directory servers.");

			Element mixminionNodesListNode = null;
			try
			{
				byte[] mixminionNodesListCompressedData = HTTPConnectionFactory.getInstance().createHTTPConnection(
								new ListenerInterface(m_urlDirectoryServer.getHost(),
					m_urlDirectoryServer.getPort())).Get(m_urlDirectoryServer.getFile()).getData();

				String mixminionNodesListInformation=Base64.encode(mixminionNodesListCompressedData,false);
				/* create the MixminionNodesList XML structure for the clients */
				mixminionNodesListNode = XMLUtil.createDocument().createElement("MixminionNodesList");
				mixminionNodesListNode.setAttribute("xml:space", "preserve");
				XMLUtil.setValue(mixminionNodesListNode, mixminionNodesListInformation);
			}
			catch (Exception e)
			{
				LogHolder.log(LogLevel.ERR, LogType.MISC,
							  "MixminionDirectoryAgent: run: Error while creating the XML structure with the Mixminion nodes list: " +
							  e.toString());
				mixminionNodesListNode = null;
			}

			if (mixminionNodesListNode == null)
			{
				LogHolder.log(LogLevel.ERR, LogType.NET,
							  "MixminionDirectoryAgent: run: Could not fetch the mixminion nodes list from the known tor directory servers.");
			}
			else
			{
				LogHolder.log(LogLevel.DEBUG, LogType.NET,
							  "MixminionDirectoryAgent: run: Fetched the list of mixminion nodes successfully.");
				m_currentMixminionNodesList = mixminionNodesListNode;
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
