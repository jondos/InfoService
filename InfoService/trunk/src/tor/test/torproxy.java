/*
 * Created on Jun 17, 2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package tor.test;

import java.net.ServerSocket;
import java.net.Socket;

import tor.Tor;
import tor.TorChannel;

/**
 * @author stefan
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class torproxy {

	public static void main(String[] args) throws Exception{
		Tor tor = Tor.getInstance();
		tor.start();

		ServerSocket server = new ServerSocket(1234);
		System.out.println("Server läuft");
		while(true)
		{
			Socket client = server.accept();
			proxythread t = new proxythread(client,tor.createChannel(TorChannel.SOCKS));
			System.out.println("verbunden");
			t.start();

		}
	}
}
