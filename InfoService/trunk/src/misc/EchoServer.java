package misc;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;

public class EchoServer implements Runnable
	{

		private Socket m_Socket;

		public EchoServer(Socket s)
		{
			// TODO Auto-generated constructor stub
			m_Socket=s;
			new Thread(this).start();
		}

		/**
		 * @param args
		 */
		public static void main(String[] args) throws Exception
			{
				// TODO Auto-generated method stub
				ServerSocket server=new ServerSocket(7777);
				for(;;)
					{
						Socket s=server.accept();
						new EchoServer(s);
					}
			}
		
		public void run()
			{
				try{
					
				byte buff[]=new byte[10000];
				InputStream in=m_Socket.getInputStream();
				OutputStream out=m_Socket.getOutputStream();
				in.read(buff,0,32); //HTTP Request;
				out.write(buff,0,40); //response 
				int len=0;
				int currentPos=0;
				while((len=in.read(buff))>0)
					{
						for(int z=0;z<len;z++)
							{
								int b=buff[z]&0x00FF;
								if(b!=currentPos%256)
									{
										System.out.println("EchoServer ("+m_Socket+")read failure at position: "+currentPos+" -- Read: "+b+" -- Expected: "+currentPos%256);
									}
								currentPos++;
							}
						out.write(buff,0,len);
					}
				System.out.println("EchoServer ("+m_Socket+")read/write "+currentPos+" Bytes");
				}
				catch(Throwable t)
					{
							t.printStackTrace();
					}
				try
					{
						m_Socket.close();
					}
				catch (IOException e)
					{
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
			}

	}
