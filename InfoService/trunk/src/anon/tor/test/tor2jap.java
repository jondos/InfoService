/*
 * Created on Apr 21, 2004
 */
package anon.tor.test;

import anon.AnonChannel;
import anon.tor.Tor;
import anon.tor.TorAnonServerDescription;
import logging.*;

/**
 * @author stefan
 *
  */
public class tor2jap
{

	public static void main(String[] args) throws Exception
	{
		LogHolder.setLogInstance(new SystemErrLog());
		Tor tor = Tor.getInstance();
		tor.initialize(new TorAnonServerDescription());
		tor.testDNS();
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
