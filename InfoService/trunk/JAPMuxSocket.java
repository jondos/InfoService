import java.util.Dictionary;
import java.util.Hashtable;
import java.net.Socket;
import java.io.OutputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.lang.Integer;

public class JAPMuxSocket extends Thread
	{
		private int lastChannelId;
		private Dictionary oSocketList;
		private DataOutputStream outDataStream;
		private DataInputStream inDataStream;
		
		private Socket outSocket;
		private	byte[] outBuff;
		private JAPSymCipher oSymCipher;
		private int chainlen;
		class SocketListEntry
			{
				SocketListEntry(JAPSocket s)
					{
						inSocket=s;
						try
							{
								out=s.getOutputStream();
							}
						catch(Exception e)
							{
								JAPDebug.out(JAPDebug.ERR,JAPDebug.NET,"Oops - SocketListEntry!");
							}
					}
				public JAPSocket inSocket;
				public OutputStream out;
			};

		JAPMuxSocket()
			{
				lastChannelId=0;
				oSocketList=new Hashtable();
				oSymCipher=new JAPSymCipher();
				byte[] key=new byte[16];
				for(int i=0;i<16;i++)
					key[i]=0;
				oSymCipher.setEncryptionKey(key);
				oSymCipher.setDecryptionKey(key);
				outBuff=new byte[1000];
		}

		public int connect(String host, int port)
			{
				try
					{
						outSocket=new Socket(host,port);
						outDataStream=new DataOutputStream(new BufferedOutputStream(outSocket.getOutputStream(),1008));
						inDataStream=new DataInputStream(outSocket.getInputStream());
						chainlen=inDataStream.readByte();
					}
				catch(Exception e)
					{
						return -1;
					}
				return 0;
			}

		public synchronized int newConnection(JAPSocket s)
			{
				oSocketList.put(new Integer(lastChannelId),new SocketListEntry(s));
				JAPModel.getModel().setNrOfChannels(oSocketList.size());
				(new JAPProxyConnection(s,lastChannelId,this)).start();
				lastChannelId++;
				return 0;
			}

		public synchronized int close(int channel)
			{
				send(channel,null,(short)0);
				oSocketList.remove(new Integer(channel));
				JAPModel.getModel().setNrOfChannels(oSocketList.size());
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
								channel=inDataStream.readInt();
								len=inDataStream.readShort();
								tmp=inDataStream.readShort();								
								inDataStream.readFully(buff);
								for(int i=0;i<chainlen;i++)
									oSymCipher.encrypt(buff);
								SocketListEntry tmpEntry=(SocketListEntry)oSocketList.get(new Integer(channel));
								if(tmpEntry!=null)
									{
										if(len==0)
											{
												oSocketList.remove(new Integer(channel));
												JAPModel.getModel().setNrOfChannels(oSocketList.size());
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
																JAPDebug.out(JAPDebug.WARNING,JAPDebug.NET,"Fehler bei write to browser...retrying...");														
															}
													}
												tmpEntry.out.flush();
												JAPModel.getModel().increasNrOfBytes(len);
											}
									}
							}
					}
				catch(Exception e)
					{
						JAPDebug.out(JAPDebug.ERR,JAPDebug.NET,"CAMuxSocket -run -Exception!");
						e.printStackTrace();
					}
			}

		public synchronized int send(int channel,byte[] buff,short len)
			{
				try
					{
						outDataStream.writeInt(channel);
						outDataStream.writeShort(len);
						outDataStream.writeShort(0);
						if(buff!=null)
							{
								System.arraycopy(buff,0,outBuff,0,len);
								for(int i=0;i<chainlen;i++)
									oSymCipher.encrypt(outBuff);
							}	
						outDataStream.write(outBuff);
						outDataStream.flush();
						JAPModel.getModel().increasNrOfBytes(len);
					}
				catch(Exception e)
					{
						JAPDebug.out(JAPDebug.ERR,JAPDebug.NET,"Sending exception!");
						return -1;
					}
				return 0;
			}

	}
