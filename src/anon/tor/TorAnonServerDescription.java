package anon.tor;

import anon.AnonServerDescription;

public class TorAnonServerDescription implements AnonServerDescription
{
	private final int m_iTorDirServerPort;
	private final String m_strTorDirServerAddr;
	private final boolean m_bUseInfoService;

	public TorAnonServerDescription()
	{
		m_strTorDirServerAddr = Tor.DEFAULT_DIR_SERVER_ADDR;
		m_iTorDirServerPort = Tor.DEFAULT_DIR_SERVER_PORT;
		m_bUseInfoService = false;
	}

	public TorAnonServerDescription(boolean bUseInfoService)
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
	}

	public TorAnonServerDescription(String torDirServerAddr, int torDirServerPort) throws Exception
	{
		m_strTorDirServerAddr = torDirServerAddr;
		m_iTorDirServerPort = torDirServerPort;
		m_bUseInfoService = false;
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
}
