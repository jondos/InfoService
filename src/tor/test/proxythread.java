/*
 * Created on Jun 22, 2004
 *
 * To change the template for this generated file go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
package tor.test;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;

import anon.AnonChannel;

/**
 * @author stefan
 *
 * To change the template for this generated type comment go to
 * Window&gt;Preferences&gt;Java&gt;Code Generation&gt;Code and Comments
 */
public class proxythread implements Runnable{
	
	private OutputStream torout;
	private InputStream torin;
	private OutputStream out;
	private InputStream in;
	private Socket client;
	private Thread t;
	
	public proxythread(Socket client,AnonChannel channel) throws IOException
	{
		this.torin = channel.getInputStream();
		this.torout = channel.getOutputStream();
		this.in = client.getInputStream();
		this.out = client.getOutputStream();
		this.client = client;
		
	}

	public void start()
	{
		t = new Thread(this);
		t.start();
	}
	
	public void stop()
	{
		t.stop();
	}

	public void run() {
		while(client.isConnected())
		{
			try
			{
				while(in.available()>0)
				{
					byte[] b = new byte[in.available()];
					in.read(b);
					torout.write(b);
				}
				while(torin.available()>0)
				{
					byte[] b = new byte[torin.available()];
					torin.read(b);
					out.write(b);
				}
			} catch (Exception ex)
			{
				System.out.println("Exception catched");
				this.stop();
			}
		}
		
	}

	

}
