package anon.tor;

import anon.AnonServerDescription;

public class TorAnonServerDescription implements AnonServerDescription
{
	private final int m_iTorDirServerPort;
	private final String m_strTorDirServerAddr;
	public TorAnonServerDescription(String torDirServerAddr, int torDirServerPort) throws Exception
	{
		m_strTorDirServerAddr=torDirServerAddr;
		m_iTorDirServerPort=torDirServerPort;
	}

	public String getTorDirServerAddr()
	{
		return m_strTorDirServerAddr;
	}

	public int getTorDirServerPort()
	{
		return m_iTorDirServerPort;
	}
}
