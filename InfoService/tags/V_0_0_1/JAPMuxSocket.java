import java.util.Dictionary;
import java.util.Hashtable;
import java.net.Socket;
import java.io.OutputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.lang.Integer;
import java.util.Enumeration;

public final class JAPMuxSocket extends Thread
	{
		private int lastChannelId;
		private Dictionary oSocketList;
		private DataOutputStream outDataStream;
		private DataInputStream inDataStream;
		
		private Socket ioSocket;
		private	byte[] outBuff;
		private JAPSymCipher oSymCipher;
		private int chainlen;
		private volatile boolean runflag;
		private final class SocketListEntry
			{
				SocketListEntry(JAPSocket s)
					{
						inSocket=s;
						try
							{
								outStream=s.getOutputStream();
							}
						catch(Exception e)
							{
								JAPDebug.out(JAPDebug.ERR,JAPDebug.NET,"Oops - SocketListEntry!");
							}
					}
				public JAPSocket inSocket;
				public OutputStream outStream;
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
						ioSocket=new Socket(host,port);
						outDataStream=new DataOutputStream(new BufferedOutputStream(ioSocket.getOutputStream(),1008));
						inDataStream=new DataInputStream(ioSocket.getInputStream());
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
				JAPProxyConnection p=new JAPProxyConnection(s,lastChannelId,this);
				oSocketList.put(new Integer(lastChannelId),new SocketListEntry(s));
				JAPModel.getModel().setNrOfChannels(oSocketList.size());
				p.start();
				lastChannelId++;
				return 0;
			}

		public synchronized int close(int channel)
			{
				oSocketList.remove(new Integer(channel));
				send(channel,null,(short)0);
				JAPModel.getModel().setNrOfChannels(oSocketList.size());
				return 0;
			}

		public synchronized int close()
			{
				runflag=false;
				Enumeration e=oSocketList.keys();
				while(e.hasMoreElements())
					{
						Integer key=(Integer)e.nextElement();
						SocketListEntry entry=(SocketListEntry)oSocketList.get(key);
						close(key.intValue());
						try
							{
								entry.inSocket.close();
							}
						catch(Exception ie){}
						oSocketList.remove(key);
					}
				JAPModel.getModel().setNrOfChannels(oSocketList.size());
				try
					{
						inDataStream.close();
						outDataStream.close();
						ioSocket.close();
						join();
					}
				catch(Exception ie)
					{
					};
				return 0;
			}

		public void run()
			{
 				byte[] buff=new byte[1000];
				int len=0;
				int tmp=0;
				int channel=0;
				runflag=true;
				while(runflag)
					{
						try
							{
								channel=inDataStream.readInt();
								len=inDataStream.readShort();
								tmp=inDataStream.readShort();								
								inDataStream.readFully(buff);
							}
						catch(Exception e)
							{
								runflag=false;
								JAPDebug.out(JAPDebug.ERR,JAPDebug.NET,"CAMuxSocket-run-Exception: receive");
								break;
							}
						for(int i=0;i<chainlen;i++)
								oSymCipher.encrypt(buff);
						SocketListEntry tmpEntry=(SocketListEntry)oSocketList.get(new Integer(channel));
						if(tmpEntry!=null)
							{
								if(len==0)
									{
										oSocketList.remove(new Integer(channel));
										JAPModel.getModel().setNrOfChannels(oSocketList.size());
										try
											{
												tmpEntry.outStream.close();
												tmpEntry.inSocket.close();
											}
										catch(Exception e)
											{
											}
									}
								else
									{
										for(int i=0;i<3;i++)
											{
												try
													{
														tmpEntry.outStream.write(buff,0,len);
														break;
													}
												catch(Exception e)
													{
														e.printStackTrace();
														JAPDebug.out(JAPDebug.WARNING,JAPDebug.NET,"Fehler bei write to browser...retrying...");														
													}
											}
										try{tmpEntry.outStream.flush();}catch(Exception e){};
										JAPModel.getModel().increasNrOfBytes(len);
									}
							}
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
