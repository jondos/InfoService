/*
 * Created on Apr 21, 2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package tor.test;

import java.net.InetAddress;

import anon.AnonChannel;
import tor.Circuit;
import tor.OnionRouter;
import tor.ordescription.ORDescription;
import tor.ordescription.ORList;
import logging.*;
/**
 * @author stefan
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class tor2jap {

	public static void main(String[] args) throws Exception {
		LogHolder.log(LogLevel.DEBUG,LogType.MISC,"[TOR] Establishing connection");
		ORList orl = new ORList();
		orl.updateList("moria.seul.org",9031);
		((ORDescription)orl.getList().elementAt(1)).getAcl().isAllowed("141.76.46.90",67);

		LogHolder.log(LogLevel.DEBUG,LogType.MISC,"[TOR] Creating new circuit");
		Circuit c = new Circuit(10,orl.getList());
		LogHolder.log(LogLevel.DEBUG,LogType.MISC,"[TOR] Connecting");
		c.connect();
		LogHolder.log(LogLevel.DEBUG,LogType.MISC,"[TOR] Creating new Stream ...");
		AnonChannel channel = c.createChannel(InetAddress.getByName("www.google.de"),80);
		channel.getOutputStream().write(("GET /index.html HTTP/1.0\n\r\n\r").getBytes());
		byte[] b = new byte[16];
		channel.getInputStream().read(b);
		for(int i=0;i<16;i++)
		{
			System.out.print((char)b[i]);
		}
		LogHolder.log(LogLevel.DEBUG,LogType.MISC,"[TOR] Closing Connection");
		c.close();
	}
}
