import java.io.InputStream;

public final class JAPProxyConnection extends Thread
	{
		private JAPSocket inSocket;
		private JAPMuxSocket outSocket;
    
		private InputStream fromClient;
    private int channel;
	
		public JAPProxyConnection (JAPSocket s, int channelID,JAPMuxSocket muxSocket) 
			{
				inSocket = s;
				channel=channelID;
				outSocket=muxSocket;
			}
    
    
		public void run() 
			{
				try
					{
						fromClient = inSocket.getInputStream();
						byte[] buff=new byte[1000];
						int len;
						while((len=fromClient.read(buff,0,1000))!=-1)
							{
								if(len>0&&outSocket.send(channel,buff,(short)len)==-1)
									{
										break;
									}
							}
					} // if (protocol....)
				catch (Exception e)
					{
					}
		  	try 
					{
						fromClient.close();
					}
				catch(Exception e)
					{
					}
				try
					{
						if(!inSocket.isClosed())
							{
								outSocket.close(channel);
								inSocket.close();
							}
					}
				catch (Exception e)
					{
						JAPDebug.out(JAPDebug.DEBUG,JAPDebug.THREAD,
												 "ProxyConnection  - Exception while closing: " + e);
					}
		  }    
}
