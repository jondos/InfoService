/*
 * Created on Jun 13, 2004
 */
package tor;

import java.util.Vector;

import tor.ordescription.ORDescription;

/**
 *
 */
public class FirstOnionRouterFactory {

	private Vector m_firstOnionRouters;
	private Vector m_firstORDescription;

	/**
	 * constructor
	 *
	 */
	public FirstOnionRouterFactory()
	{
		this.m_firstOnionRouters = new Vector();
		this.m_firstORDescription = new Vector();
	}

	/**
	 * creates a FOR with the given description if it doesn't exist. else it returns a existing FOR
	 * @param d
	 * description of a FOR
	 * @return
	 * FirstOnionRouter
	 */
	public FirstOnionRouter createFirstOnionRouter(ORDescription d)
	{
		ORDescription ord;
		for(int i=0;i<this.m_firstORDescription.size();i++)
		{
			ord = (ORDescription)this.m_firstORDescription.elementAt(i);
			if(ord.getAddress().equals(d.getAddress())&&(ord.getPort()==d.getPort()))
			{
				return (FirstOnionRouter)this.m_firstOnionRouters.elementAt(i);
			}
		}
		FirstOnionRouter firstOR = new FirstOnionRouter(d);
		try
		{
			firstOR.connect();
		} catch(Exception ex)
		{
			return null;
		}
		firstOR.start();
		this.m_firstOnionRouters.add(firstOR);
		this.m_firstORDescription.add(d);
		return firstOR;
	}

}