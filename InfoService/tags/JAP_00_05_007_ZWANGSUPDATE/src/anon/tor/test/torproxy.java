/*
 * Created on Jun 17, 2004
 */
package anon.tor.test;

import java.net.ServerSocket;
import java.net.Socket;

import anon.tor.Tor;
import anon.tor.TorAnonServerDescription;
import anon.tor.TorChannel;
import jap.JAPController;
import logging.LogHolder;
import logging.SystemErrLog;

/**
 * @author stefan
 */
public class torproxy
{

	public static void main(String[] args) throws Exception
	{
		LogHolder.setLogInstance(new SystemErrLog());

		JAPController m_controller = JAPController.getInstance();
		m_controller.loadConfigFile(null, false);
//		m_controller.initialRun();

		Tor tor = Tor.getInstance();
		tor.setConnectionsPerRoute(10);
		tor.initialize(new TorAnonServerDescription(true, false));
//		tor.initialize(new TorAnonServerDescription());
		ServerSocket server = new ServerSocket(1234);
		System.out.println("Server läuft");
		int i = 0;
		while (i < 500)
		{
			Socket client = server.accept();
			proxythread t = new proxythread(client, (TorChannel) tor.createChannel(TorChannel.SOCKS));
			t.start();
			i++;
		}
		System.out.println(
			"Waiting 5 seconds, hopefully all channels finish in this time, else you get a null pointer exception ;-)");
		Thread.sleep(5000);
		tor.shutdown();
		JAPController.goodBye(false);
		System.out.println("Done");
	}
}
