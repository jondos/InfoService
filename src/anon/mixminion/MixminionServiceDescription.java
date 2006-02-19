package anon.mixminion;

import anon.AnonServerDescription;

/** Holder for the neseccary information to setup the Mixmion service*/
public class MixminionServiceDescription implements AnonServerDescription
{
	private int m_iRouteLen;

	public MixminionServiceDescription(int routeLen)
	{
		m_iRouteLen=routeLen;
	}

	public int getRouteLen()
	{
		return m_iRouteLen;
	}
}
