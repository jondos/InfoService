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
		Tor tor = Tor.getInstance();
		for(int i2=0;i2<3;i2++)
		{
			tor.initialize(new TorAnonServerDescription());
		/*	byte[] temp = tor.DNSResolve("www.google.de");
			for(int i=0;i<temp.length;i++)
			{
				System.out.print(" "+(temp[i]&0xFF));
			}
			System.out.println();*/
			AnonChannel channel = tor.createChannel(InetAddress.getByName("www.google.de"), 80);
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
}
