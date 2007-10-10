package misc;

import java.net.Socket;
import java.net.UnknownHostException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.InputStream;

public class GenerateUpload implements Runnable
{
	private static InputStream in;
	public static void main(String[] args) throws UnknownHostException, IOException
	{
		Socket s=new Socket("127.0.0.1",4001);
		OutputStream out=s.getOutputStream();
		in=s.getInputStream();
		Thread t=new Thread(new GenerateUpload());
		t.start();
		byte[] buff=new byte[1000];
		out.write("CONNECT 127.0.0.1:7 HTTP/1.0\n\r\n\r".getBytes());
		long l=0;
		while(true)
		{
			out.write(buff);
			l+=buff.length;
			if((l%1000000)==0)
				System.out.println("Sent: "+l/1000000+" MBytes");
		}

	}

	public void run()
	{
		try{
		byte[] buff=new byte[10000];
		long l=0;
		while(true)
		{
			long r=in.read(buff);
			l+=r;
			if((l%1000000)==0)
				System.out.println("Read: "+l/1000000+" MBytes");
		}
		}catch(Exception e)
		{
			System.out.println("Read failed!");
		}
	}
}
