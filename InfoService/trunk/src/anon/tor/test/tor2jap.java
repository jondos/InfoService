/*
 * Created on Apr 21, 2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package anon.tor.test;

import anon.AnonChannel;
import anon.tor.Tor;
import anon.tor.TorAnonServerDescription;
import logging.*;
import java.net.InetAddress;

/**
 * @author stefan
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class tor2jap
{

	public static void main(String[] args) throws Exception
	{
		LogHolder.setLogInstance(new SystemErrLog());
		Tor tor = Tor.getInstance();
		tor.initialize(new TorAnonServerDescription());
			AnonChannel channel = tor.createChannel("www.google.de", 80);
			channel.getOutputStream().write( ("GET /index.html HTTP/1.0\n\r\n\r").getBytes());
			for (; ; )
			{
				int b = channel.getInputStream().read();
				if (b < 0)
				{
					break;
				}
				System.out.print( (char) b);
			}
			tor.shutdown();
		}

}
