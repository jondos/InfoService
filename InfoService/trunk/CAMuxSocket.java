import java.util.Dictionary;
import java.util.Hashtable;
import java.net.Socket;
import java.io.OutputStream;
import java.io.InputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.lang.Integer;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;

public class CAMuxSocket extends Thread
	{
		private int lastChannelId;
		private Dictionary oSocketList;
		private OutputStream toServer;
		private ByteArrayOutputStream tmpByteStream;
		private DataOutputStream tmpDataStream;
		private	byte[] inBuff;
		private ByteArrayInputStream inByteStream;
		private DataInputStream inDataStream;
		private InputStream fromServer;
		
		private Socket outSocket;
		private	byte[] tmpBuff;
		private JASymCipher oSymCipher;
		
		class SocketListEntry
			{
				SocketListEntry(CASocket s)
					{
						inSocket=s;
						try
							{
								out=s.getOutputStream();
							}
						catch(Exception e)
							{
								System.out.println("Oops - SocketListEntry!");
							}
					}
				public CASocket inSocket;
				public OutputStream out;
			};

		CAMuxSocket()
			{
				lastChannelId=0;
				oSocketList=new Hashtable();
				tmpBuff=new byte[1000];
				oSymCipher=new JASymCipher();
				byte[] key=new byte[16];
				for(int i=0;i<16;i++)
					key[i]=0;
				oSymCipher.setEncryptionKey(key);
				oSymCipher.setDecryptionKey(key);
				tmpByteStream=new ByteArrayOutputStream(1008);
				tmpDataStream=new DataOutputStream(tmpByteStream);
				inBuff=new byte[1008];
				inByteStream=new ByteArrayInputStream(inBuff);
				inDataStream=new DataInputStream(inByteStream);
		}

		public int connect(String host, int port)
			{
				try
					{
						outSocket=new Socket(host,port);
						toServer=outSocket.getOutputStream();
						fromServer=outSocket.getInputStream();
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
				int tmp=0;
				int channel=0;
				try
					{
						while(true)
							{
								len=inBuff.length;
								while(len>0)
									{
										len-=fromServer.read(inBuff);
									}
								oSymCipher.decrypt(inBuff);
								inByteStream.reset();
								channel=inDataStream.readInt();
		//						System.out.println("Receiving channel: "+Integer.toString(channel));
								len=inDataStream.readShort();
								tmp=inDataStream.readShort();
								inDataStream.readFully(buff);
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
												for(int i=0;i<3;i++)
													{
														try
															{
																tmpEntry.out.write(buff,0,len);
																break;
															}
														catch(Exception e)
															{
																e.printStackTrace();
																System.out.println("Fehler bei write to browser...retrying...");														
															}
													}
												tmpEntry.out.flush();
											}
									}
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
			//			System.out.println("Sending: channel "+Integer.toString(channel)+"len: "+Integer.toString(len));
						tmpByteStream.reset();
						tmpDataStream.writeInt(channel);
						tmpDataStream.writeShort(len);
						tmpDataStream.writeShort(0);
						if(buff!=null)
							tmpDataStream.write(buff,0,len);
						tmpDataStream.write(tmpBuff,0,1000-len);
						tmpDataStream.flush();
						byte[] b=tmpByteStream.toByteArray();
						oSymCipher.encrypt(b);
						toServer.write(b);
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
