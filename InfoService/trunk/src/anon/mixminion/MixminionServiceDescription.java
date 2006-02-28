package anon.mixminion;

import anon.AnonServerDescription;

/** Holder for the neseccary information to setup the Mixmion service.*/
public class MixminionServiceDescription implements AnonServerDescription
{
	private int m_iRouteLen;

	/** Constucts a new MixminionServiceDescription object.
	 * @see #setRouteLen(int)
	 * @param routeLen number of hops for the anonymous mail
	 */

	public MixminionServiceDescription(int routeLen)
	{
		setRouteLen(routeLen);
	}

	public int getRouteLen()
	{
		return m_iRouteLen;
	}

	/** Sets the number of hops for the anonymous mail.
	 * @param routeLen number of hops for the anonymous mail. This number must be {@code <= Mixminion.MAX_ROUTE_LEN} and
	 * {@code >=Mixminion.MIN_ROUTE_LEN}
	 *
	 */
	public void setRouteLen(int routeLen)
	{
		if (routeLen >= Mixminion.MIN_ROUTE_LEN && routeLen <= Mixminion.MAX_ROUTE_LEN)
		{
			m_iRouteLen = routeLen;
		}
	}
}
