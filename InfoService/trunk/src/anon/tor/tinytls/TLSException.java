/*
 * Created on Mar 29, 2004
 *
 */
package anon.tor.tinytls;

import java.io.IOException;

/**
 * 
 * @author stefan
 *
 *TLSException
 */
public class TLSException extends IOException
{
	/**
	 * Constructor
	 * @param s message
	 */
	public TLSException(String s)
	{
		super(s);
	}
}
	
