/*
 * Created on Apr 21, 2004
 */
package anon.tor.test;

import anon.AnonChannel;
import anon.tor.Tor;
import anon.tor.TorAnonServerDescription;
import logging.LogHolder;
import logging.SystemErrLog;
import anon.tor.ordescription.*;
import java.util.Vector;
import java.net.*;
/**
 * @author stefan
 *
 */
public class tor2jap
{

	public static void main(String[] args) throws Exception
	{
		LogHolder.setLogInstance(new SystemErrLog());
		ORList ol=new ORList(new PlainORListFetcher(Tor.DEFAULT_DIR_SERVER_ADDR,Tor.DEFAULT_DIR_SERVER_PORT));
		ol.updateList();
		Vector v=ol.getList();
		for(int i=0;i<v.size();i++)
		{
			ORDescription od=(ORDescription)v.elementAt(i);
			Socket s=new Socket();
		//	s.connect(new InetSocketAddress(od.getAddress(),od.getPort()),1000);
		}
		/*Tor tor = Tor.getInstance();
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
		tor.shutdown();*/
	}

}
