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
package anon.tor;

import anon.AnonServerDescription;

public class TorAnonServerDescription implements AnonServerDescription
{
	private final int m_iTorDirServerPort;
	private final String m_strTorDirServerAddr;
	private final boolean m_bUseInfoService;
	private final boolean m_bStartCircuitsAtStartup;

	public TorAnonServerDescription()
	{
		m_strTorDirServerAddr = Tor.DEFAULT_DIR_SERVER_ADDR;
		m_iTorDirServerPort = Tor.DEFAULT_DIR_SERVER_PORT;
		m_bUseInfoService = false;
		m_bStartCircuitsAtStartup = false;
	}

	public TorAnonServerDescription(boolean bUseInfoService)
	{
		this(bUseInfoService,false);
	}

	public TorAnonServerDescription(boolean bUseInfoService, boolean bStartCircuitsAtStartup)
	{
		if (bUseInfoService)
		{
			m_strTorDirServerAddr = null;
			m_iTorDirServerPort = -1;
			m_bUseInfoService = true;
		}
		else
		{
			m_strTorDirServerAddr = Tor.DEFAULT_DIR_SERVER_ADDR;
			m_iTorDirServerPort = Tor.DEFAULT_DIR_SERVER_PORT;
			m_bUseInfoService = false;
		}
		m_bStartCircuitsAtStartup = bStartCircuitsAtStartup;
	}

	public TorAnonServerDescription(String torDirServerAddr, int torDirServerPort, boolean bStartCircuitsAtStartup)
	{
		m_strTorDirServerAddr = torDirServerAddr;
		m_iTorDirServerPort = torDirServerPort;
		m_bUseInfoService = false;
		m_bStartCircuitsAtStartup = bStartCircuitsAtStartup;
	}

	public String getTorDirServerAddr()
	{
		return m_strTorDirServerAddr;
	}

	public int getTorDirServerPort()
	{
		return m_iTorDirServerPort;
	}

	public boolean useInfoService()
	{
		return m_bUseInfoService;
	}

	public boolean startCircuitsAtStartup()
	{
		return m_bStartCircuitsAtStartup;
	}
}
