/*
 * Created on Jun 17, 2004
 */
package anon.tor.test;

import java.net.ServerSocket;
import java.net.Socket;

import logging.LogHolder;
import logging.SystemErrLog;

import anon.tor.Tor;
import anon.tor.TorAnonServerDescription;
import anon.tor.TorChannel;


/**
 * @author stefan
  */
public class torproxy {

	public static void main(String[] args) throws Exception{
//		LogHolder.setLogInstance(new SystemErrLog());
		Tor tor = Tor.getInstance();
		tor.initialize(new TorAnonServerDescription());

		ServerSocket server = new ServerSocket(1234);
		System.out.println("Server läuft");
		while(true)
		{
			Socket client = server.accept();
			proxythread t = new proxythread(client,(TorChannel)tor.createChannel(TorChannel.SOCKS));
			System.out.println("verbunden");
			t.start();

		}
	}
}
