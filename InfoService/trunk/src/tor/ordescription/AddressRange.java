package tor.ordescription;

import java.math.BigInteger;
import java.net.InetAddress;
import java.net.UnknownHostException;

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
public class AddressRange {
	
	BigInteger mask;
	BigInteger ip;
	
	public AddressRange(String range)
	{
		if(range.trim().equals("*"))
		{
			this.ip=new BigInteger(new byte[]{0});
			this.mask = new BigInteger(new byte[]{0});
		}
	}
	
	public boolean isInRange(String address) throws UnknownHostException
	{
		InetAddress addr = InetAddress.getByName(address);
		BigInteger b = new BigInteger(addr.getAddress());
		if(b.and(this.mask).equals(this.ip))
		{
			return true;
		}
		return false;
	}

}
