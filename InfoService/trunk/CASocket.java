import java.net.Socket;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.UnknownHostException;

public class CASocket 
	{
		private boolean bisClosed;
		private Socket s;
		
		public CASocket()
			{
				bisClosed=false;
			}
		
		public CASocket(Socket so)	
			{
				s=so;
				bisClosed=false;
			}
		
		public synchronized void close() throws IOException
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
		
		public synchronized boolean isClosed()
			{
				return bisClosed;
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
