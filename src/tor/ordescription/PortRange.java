package tor.ordescription;
/*
 * Created on Mar 25, 2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */

/**
 * @author stefan
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class PortRange {
	
	private int from;
	private int to;
	
	public PortRange(int from, int to)
	{
		this.from = Math.min(from,to);
		this.to = Math.max(from,to);
	}
	
	public PortRange(int port)
	{
		this.from = port;
		this.to = port;
	}
	
	public PortRange(String portrange)
	{
		int i = portrange.indexOf("-");
		if(i!=-1)
		{
			String s1=portrange.substring(0,i);
			String s2=portrange.substring(i+1,portrange.length());
			this.from = Integer.parseInt(s1);
			this.to = Integer.parseInt(s2);
		} else
		{
			if(portrange.trim().equals("*"))
			{
				this.from = 0;
				this.to = 65535;
			} else
			{
				this.from = Integer.parseInt(portrange);
				this.to = this.from;
			}
		}
	}
	
	public boolean isInRange(int port)
	{
		if( (port>=from) && (port<=to) )
		{
		 	return true;
		}
		return false;
	}

}
