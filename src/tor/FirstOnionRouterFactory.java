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

	private Vector firstOnionRouters;
	private Vector firstORDescription;

	/**
	 * constructor
	 *
	 */
	public FirstOnionRouterFactory()
	{
		this.firstOnionRouters = new Vector();
		this.firstORDescription = new Vector();
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
		for(int i=0;i<this.firstORDescription.size();i++)
		{
			ord = (ORDescription)this.firstORDescription.elementAt(i);
			if(ord.getAddress().equals(d.getAddress())&&(ord.getPort()==d.getPort()))
			{
				return (FirstOnionRouter)this.firstOnionRouters.elementAt(i);
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
		this.firstOnionRouters.add(firstOR);
		this.firstORDescription.add(d);
		return firstOR;
	}

}