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

import HTTPClient.HTTPConnection;
import HTTPClient.HTTPResponse;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

public class PlainORListFetcher implements ORListFetcher
{
	private String m_ORListServer;
	private int m_ORListPort;

	/**
	 * Constructor
	 * @param addr
	 * address of the directory server
	 * @param port
	 * port of the directory server
	 */
	public PlainORListFetcher(String addr, int port)

	{
		m_ORListServer = addr;
		m_ORListPort = port;
	}

	public byte[] getORList()
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
			byte[] doc = resp.getData();
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