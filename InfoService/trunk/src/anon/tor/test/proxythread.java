/*
 * Created on Jun 22, 2004
 */
package anon.tor.test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import anon.tor.TorChannel;

/**
 * @author stefan
 */
public class proxythread implements Runnable{

	private OutputStream torout;
	private InputStream torin;
	private OutputStream out;
	private InputStream in;
	private Socket client;
	private Thread t;
	private TorChannel channel;

	public proxythread(Socket client,TorChannel channel) throws IOException
	{
		this.torin = channel.getInputStream();
		this.torout = channel.getOutputStream();
		this.in = client.getInputStream();
		this.out = client.getOutputStream();
		this.client = client;
		this.channel = channel;

	}

	public void start()
	{
		t = new Thread(this);
		t.start();
	}

	public void stop()
	{
		channel.close();
		try
		{
			client.close();
		} catch (IOException ex)
		{
			System.out.println("Fehler beim schliessen des kanals");
		}
		System.out.println("kanal wird geschlossen");
		t.stop();
	}

	public void run() {
		while(true)
		{
			try
			{
				while(in.available()>0)
				{
					byte[] b = new byte[in.available()];
					in.read(b);
					torout.write(b);
					torout.flush();
				}
				while(torin.available()>0)
				{
					byte[] b = new byte[torin.available()];
					torin.read(b);
					out.write(b);
					out.flush();
				}
				if(channel.isClosedByPeer())
				{
					this.stop();
				}
			} catch (Exception ex)
			{
				System.out.println("Exception catched : "+ex.getLocalizedMessage());
				this.stop();
			}
		}

	}



}
