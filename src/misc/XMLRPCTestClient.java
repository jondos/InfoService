package misc;

import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import anon.*;
class XMLRPCTestClient
{
	public static void main(String[] args)
		{
			try{
			AnonService anonService=AnonServiceFactory.create(InetAddress.getLocalHost(),8889);
			AnonChannel c=anonService.createChannel(AnonChannel.HTTP);
			InputStream in=c.getInputStream();
			OutputStream out=c.getOutputStream();
			out.write(1);
			int b;
			byte[] buff=new byte[100];
			while(in.read(buff)>0)
				{
					System.out.print(new String(buff));
				}
				}
				catch(Exception e)
					{
						e.printStackTrace();
					}
		}
}