/*
 * Created on Jun 13, 2004
 */
package anon.tor;

import java.util.Vector;

import anon.tor.ordescription.ORDescription;

/**
 *
 */
public class FirstOnionRouterConnectionFactory
{

	private Vector m_firstOnionRouters;

	/**
	 * constructor
	 *
	 */
	public FirstOnionRouterConnectionFactory()
	{
		m_firstOnionRouters = new Vector();
	}

	/**
	 * creates a FOR with the given description if it doesn't exist. else it returns a existing FOR
	 * @param d
	 * description of a FOR
	 * @return
	 * FirstOnionRouter
	 */
	public synchronized FirstOnionRouterConnection createFirstOnionRouterConnection(ORDescription d)
	{
		ORDescription ord;
		FirstOnionRouterConnection fOR;
		for (int i = 0; i < m_firstOnionRouters.size(); i++)
		{
			fOR=(FirstOnionRouterConnection)m_firstOnionRouters.elementAt(i);
			ord = fOR.getORDescription();
			if (ord.equals(d))
			{
				return fOR;
			}
		}
		fOR = new FirstOnionRouterConnection(d);
		try
		{
			fOR.connect();
		}
		catch (Exception ex)
		{
			return null;
		}
		m_firstOnionRouters.addElement(fOR);
		return fOR;
	}

	/** Closes all connections to all FirstOnionRouters */
	public synchronized void closeAll()
	{
		for (int i = 0; i < m_firstOnionRouters.size(); i++)
		{
			FirstOnionRouterConnection fOR=(FirstOnionRouterConnection)m_firstOnionRouters.elementAt(i);
			fOR.close();
		}
		m_firstOnionRouters.removeAllElements();
	}

}
