import java.net.Socket;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.UnknownHostException;

public final class JAPSocket 
	{
		private volatile boolean bisClosed;
		private Socket s;
		
		public JAPSocket()
			{
				bisClosed=false;
			}
		
		public JAPSocket(Socket so)	
			{
				s=so;
				bisClosed=false;
			}
		
		public void close() throws IOException
			{
				synchronized(this)
					{
						if(!bisClosed)
							{
								bisClosed=true;
								try
									{
										s.close();
									}
								catch (IOException ioe)
									{
										throw ioe;
									}
							}
					}
			}
		
		public boolean isClosed()
			{
				synchronized(this)
					{
						return bisClosed;
					}
			}
		
		public OutputStream getOutputStream()
			{
				try
					{
						return s.getOutputStream();
					}
				catch(Exception e)
					{
						return null;
					}
			}
	
		public InputStream getInputStream()
			{
				try
					{
						return s.getInputStream();
					}
				catch(Exception e)
					{
						return null;
					}
			}
	}
