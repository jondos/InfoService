import java.util.Dictionary;
import java.util.Hashtable;
import java.net.Socket;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.lang.Integer;

public class CAMuxSocket extends Thread
	{
		private int lastChannelId;
		private Dictionary oSocketList;
		private DataOutputStream toServer;
		private DataInputStream fromServer;
		private Socket outSocket;
		private	byte[] tmpBuff;
		
		class SocketListEntry
			{
				SocketListEntry(CASocket s)
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
				public CASocket inSocket;
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
						toServer=new DataOutputStream(new BufferedOutputStream(outSocket.getOutputStream(),1008));
						fromServer=new DataInputStream(new BufferedInputStream(outSocket.getInputStream(),1008));
						System.out.println("CAMuxSocket - Connected!");
					}
				catch(Exception e)
					{
						return -1;
					}
				return 0;
			}
		
		public synchronized int newConnection(CASocket s)
			{
				oSocketList.put(new Integer(lastChannelId),new SocketListEntry(s));
				(new SK13ProxyConnection(s,lastChannelId,this)).start();
				lastChannelId++;
//				System.out.println("CAMuxSocket - new Connection");
				return 0;
			}
		
		public synchronized int close(int channel)
			{
	//			System.out.println("Closing channel: "+Integer.toString(channel));
				send(channel,null,(short)0);
				oSocketList.remove(new Integer(channel));
				return 0;
			}
		
		public void run()
			{
 				byte[] buff=new byte[1000]; 
				int len=0;
				int count=0;
				int tmp=0;
				int channel=0;
				try{
				while(true)
					{
	//					System.out.print("CAMuxSocket - receving ");
						channel=fromServer.readInt();
						len=fromServer.readShort();
						tmp=fromServer.readShort();
						count=1000;
						tmp=0;
						do
							{
								tmp=fromServer.read(buff,tmp,count);
								count-=tmp;
							}
						while (tmp!=-1&&count>0);
						if(tmp==-1)
							break;
//						System.out.println("channel- "+Integer.toString(channel));
						SocketListEntry tmpEntry=(SocketListEntry)oSocketList.get(new Integer(channel));
						if(tmpEntry!=null)
							{
								if(len==0)
									{
										oSocketList.remove(new Integer(channel));
										tmpEntry.out.close();
										tmpEntry.inSocket.close();
									}
								else
									{
	//									System.out.println("CAMuxSocket - writing to Browser ");
										tmpEntry.out.write(buff,0,len);
										tmpEntry.out.flush();
		//								System.out.print(Integer.toString(len)+" Bytes ");
									}
							}
				//		else
				//			System.out.println("Channel not valid!");
					}
				}
				catch(Exception e)
					{
						System.out.println("CAMuxSocket -run -Exception!");
						e.printStackTrace();
					}
				System.out.println("CAMuxSocket -exiting! "+Integer.toString(len));
			}
		
		public synchronized int send(int channel,byte[] buff,short len)
			{
				try
					{
						//System.out.println("Sending: channel "+Integer.toString(channel)+"len: "+Integer.toString(len));
						toServer.writeInt(channel);
						toServer.writeShort(len);
						toServer.writeShort(0);
						if(buff!=null)
							toServer.write(buff,0,len);
						toServer.write(tmpBuff,0,1000-len);
						toServer.flush();
					}
				catch(Exception e)
					{
						System.out.println("Sending exception!");
//						e.printStackTrace();
						return -1;
					}
				return 0;
			}
		
	}
