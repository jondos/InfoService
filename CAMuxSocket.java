import java.util.Dictionary;
import java.util.Hashtable;
import java.net.Socket;
import java.io.BufferedOutputStream;
import java.io.BufferedInputStream;
import java.lang.Integer;

public class CAMuxSocket extends Thread
	{
		private int lastChannelId;
		private Dictionary oSocketList;
		private BufferedOutputStream toServer;
		private BufferedInputStream fromServer;
		private Socket outSocket;
		private	byte[] tmpBuff;
		
		class SocketListEntry
			{
				SocketListEntry(Socket s)
					{
						inSocket=s;
						try{
							out=new BufferedOutputStream(s.getOutputStream());
							}
						catch(Exception e)
							{
								System.out.println("Oops - SocketListEntry!");
							}
					}
				public Socket inSocket;
				public BufferedOutputStream out;
			};
		
		CAMuxSocket()
			{
				lastChannelId=0;
				oSocketList=new Hashtable();
				tmpBuff=new byte[1000];
			}
		
		public int connect(String host, int port)
			{
				try
					{		
						outSocket=new Socket(host,port);
						toServer=new BufferedOutputStream(outSocket.getOutputStream(),1008);
						fromServer=new BufferedInputStream(outSocket.getInputStream(),1008);
						System.out.println("CAMuxSocket - Connected!");
					}
				catch(Exception e)
					{
						return -1;
					}
				return 0;
			}
		
		public synchronized int newConnection(Socket s)
			{
				oSocketList.put(new Integer(lastChannelId),new SocketListEntry(s));
				(new SK13ProxyConnection(s,lastChannelId,this)).start();
				lastChannelId++;
				System.out.println("CAMuxSocket - new Connection");
				return 0;
			}
		
		public synchronized int close(int channel)
			{
				System.out.println("Closing channel: "+Integer.toString(channel));
				send(channel,null,0);
				oSocketList.remove(new Integer(channel));
				return 0;
			}
		
		public void run()
			{
 				byte[] buff=new byte[1008]; 
				int len=0;
				int count=0;
				try{
				while(true)
					{
						System.out.print("CAMuxSocket - receving ");
						count=1008;
						len=0;
						do
							{
								len=fromServer.read(buff,len,count);
								count-=len;
							}
						while (len!=-1&&count>0);
						if(len==-1)
							break;
						int channel=buff[0]<<24;
						channel|=(buff[1]<<16)&0x00FF0000;
						channel|=(buff[2]<<8)&0x0000FF00;
						channel|=(buff[3])&0x000000FF;
						System.out.println("channel- "+Integer.toString(channel));
						len=buff[4];
						len<<=8;
						len&=0x00FF00;
						len|=(buff[5]&0x00FF);
						SocketListEntry tmpEntry=(SocketListEntry)oSocketList.get(new Integer(channel));
						if(tmpEntry!=null)
							{
								if(len==0)
									{
										System.out.println("Closing channel: "+Integer.toString(channel));
										oSocketList.remove(new Integer(channel));
										tmpEntry.out.close();
										tmpEntry.inSocket.close();
									}
								else
									{
										System.out.println("CAMuxSocket - writing to Browser ");
										tmpEntry.out.write(buff,8,len);
										tmpEntry.out.flush();
										System.out.print(Integer.toString(len)+" Bytes ");
									}
							}
						else
							System.out.println("Channel not valid!");
					}
				}
				catch(Exception e)
					{
						System.out.println("CAMuxSocket -run -Exception!");
						e.printStackTrace();
					}
				System.out.println("CAMuxSocket -exiting! "+Integer.toString(len));
			}
		
		public synchronized int send(int channel,byte[] buff,int len)
			{
				try
					{
						tmpBuff[0]=(byte)((channel>>24)&0x00FF);
						tmpBuff[1]=(byte)((channel>>16)&0x00FF);
						tmpBuff[2]=(byte)((channel>>16)&0x00FF);
						tmpBuff[3]=(byte)((channel)&0x00FF);
						tmpBuff[4]=(byte)((len>>8)&0x00FF);
						tmpBuff[5]=(byte)(len&0x00FF);
						tmpBuff[6]=0;
						tmpBuff[7]=0;
						toServer.write(tmpBuff,0,8);
						toServer.write(buff,0,len);
						toServer.write(tmpBuff,0,1000-len);
						toServer.flush();
					}
				catch(Exception e)
					{
						return -1;
					}
				return 0;
			}
		
	}
