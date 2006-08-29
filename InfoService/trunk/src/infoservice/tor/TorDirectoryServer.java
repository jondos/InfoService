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

import anon.infoservice.AbstractDatabaseEntry;
import anon.infoservice.HTTPConnectionFactory;
import anon.infoservice.ListenerInterface;
import anon.util.ZLibTools;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/**
 * This is the implementation for an entry in the tor directory server database.
 */
public class TorDirectoryServer extends AbstractDatabaseEntry
{

	/**
	 * Stores the information how we can reach the directory server.
	 */
	private TorDirectoryServerUrl m_url;

	/**
	 * Stores the ID of this TOR directory server.
	 */
	private String m_id;

	/**
	 * Stores the time when this TorDirectoryServer was created. This value is used to determine the
	 * more recent entry, if two entries are compared (higher version number -> more recent entry).
	 */
	private long m_creationTimeStamp;

	/** Stores if this Entry was user defined and not automatic retrieved*/
	private boolean m_bUserDefined;

	/**
	 * Creates a new TorDirectoryServer entry.
	 *
	 * @param a_url The URL where we can reach the directory server (host, port and filename).
	 * @param a_timeValide The period this TorDirectoryServer entry is valid and not outdated. The
	 *                     interval is given in milliseconds.
	 * @param a_userDefined Whether this TorDirectoryServer was defined in the config file (true)
	 *                      or was obtained via the list of running TOR directory servers (false).
	 *                      This is important, because user-defined TOR directory servers shall not
	 *                      be overwritten by one, we have obtained via the TOR directory server
	 *                      list.
	 */
	public TorDirectoryServer(TorDirectoryServerUrl a_url, long a_timeValid, boolean a_userDefined)
	{
		/* add the current system time to the given period -> we get the time until this
		 * TorDirectoryServer is valid
		 */
		super(a_timeValid + System.currentTimeMillis());
		m_creationTimeStamp = System.currentTimeMillis();

		m_url = a_url;

		m_id = "http://" + a_url.getHost() + ":" + Integer.toString(a_url.getPort()) + a_url.getFileName();
		if (a_userDefined)
		{
			m_id = "(userdefined) " + m_id;
		}
		m_bUserDefined=a_userDefined;
	}

	/**
	 * This method connects to the tor directory server and downloads the tor nodes list. If this
	 * method is successful, the raw TOR nodes structure from the TOR directory server is returned.
	 * If there was an error while fetching the nodes list, null is returned.
	 *
	 * @return A String with the plain information (already decompressed) about the running TOR
	 *         nodes or null, if there was an error while fetching the list.
	 */
	public String downloadTorNodesInformation()
	{
		LogHolder.log(LogLevel.INFO, LogType.NET,
			"TorDirectoryServer: downloadTorNodesInformation: Try to get tor nodes list from http://" +
					  m_url.getHost() + ":" + Integer.toString(m_url.getPort()) + m_url.getFileName());
		String torNodesList = null;
		try
		{
			byte[] torNodesListCompressedData = HTTPConnectionFactory.getInstance().createHTTPConnection(new
				ListenerInterface(m_url.getHost(), m_url.getPort())).Get(m_url.getFileName()).getData();
			if (torNodesListCompressedData != null)
			{
				/* decompress it */
				byte[] decompressedData = ZLibTools.decompress(torNodesListCompressedData);
				if (decompressedData == null)
				{
					/* uups, maybe the data were not compressed or they are decompressed already by the
					 * HTTPClient library (some versions seem to do so, some not), also no problem, if the
					 * data are really invalid, it's detected by the TorDirectoryAgent
					 */
					decompressedData = torNodesListCompressedData;
				}
				torNodesList = new String(decompressedData);
			}
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.ERR, LogType.NET,
				"TorDirectoryServer: downloadTorNodesInformation: Error while getting tor nodes list (" +
						  e.toString() + ").");
			torNodesList = null;
		}
		return torNodesList;
	}

	/**
	 * Returns an ID for this TorDirectoryServer. It's the complete URL of the tor nodes file
	 * (including protocol, host, port and filename, e.g.
	 * http://infoservice.inf.tu-dresden.de:80/torNodes). If this TorDirectoryServer is
	 * user-defined (in the configuration file), a (userdefined) is put before the ID.
	 *
	 * @return A unique ID for this TorDirectoryServer.
	 */
	public String getId()
	{
		return m_id;
	}

	public long getLastUpdate()
	{
		return m_creationTimeStamp;
	}

	/**
	 * Returns the time when this TorDirectoryServer was created.
	 *
	 * @return A version number which is used to determine the more recent entry, if two entries are
	 *         compared (higher version number -> more recent entry).
	 */
	public long getVersionNumber()
	{
		return m_creationTimeStamp;
	}

	/** Returns if this torDirectory Server was user defined
	 *
	 */
	public boolean isUserDefined()
	{
		return m_bUserDefined;
	}
}
