/*
 * Created on Apr 21, 2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package tor.test;

import anon.AnonChannel;
import tor.Tor;

//import org.bouncycastle.crypto.params.
/**
 * @author stefan
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class tor2jap {

	public static void main(String[] args) throws Exception {
		Tor tor = Tor.getInstance();
		tor.start();
		AnonChannel channel = tor.createChannel("www.google.de",80);
		channel.getOutputStream().write(("GET /index.html HTTP/1.0\n\r\n\r").getBytes());
		for(;;)
		{
			int b=channel.getInputStream().read();
			if(b<0)
				break;
			System.out.print((char)b);
		}
		tor.stop();
	}
}
