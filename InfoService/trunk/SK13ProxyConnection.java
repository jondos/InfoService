import java.io.*;
import java.net.*;
import java.util.*;
import java.text.*;

public class SK13ProxyConnection extends Thread {
	private CASocket inSocket;
    
  private boolean debug = false; 
  private int threadNumber;
  private static int threadCount;

  private BufferedInputStream fromClient;
    
	private int channel;
	private CAMuxSocket outSocket;
	
	public SK13ProxyConnection (CASocket s, int channelID,CAMuxSocket muxSocket) 
		{
			inSocket = s;
			channel=channelID;
			outSocket=muxSocket;
    }
    
    
  public void run() 
		{
			try
				{
					fromClient = new BufferedInputStream(inSocket.getInputStream());
					//
					// Response from server is transfered to client in a sepatate thread
					// Request
					byte[] buff=new byte[1000];
					int len;
					while((len=fromClient.read(buff,0,1000))!=-1)
						{
							if(len>0)
								if(outSocket.send(channel,buff,(short)len)==-1)
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
					if (debug)
						System.out.println("ProxyConnection ("+threadNumber+") - Exception while closing: " + e);
				}
    }    
    
 
    
    private synchronized int getThreadNumber() {
	return threadCount++;
    }
}
