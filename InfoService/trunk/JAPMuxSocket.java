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
import java.security.SecureRandom;

public final class JAPMuxSocket extends Thread
	{
		private int lastChannelId;
		private Dictionary oSocketList;
		private DataOutputStream outDataStream;
		private DataInputStream inDataStream;
		
		private Socket ioSocket;
		private	byte[] outBuff;
		private JAPASymCipher oRSA;
		private int chainlen;
		private volatile boolean runflag;
		
		private final static int KEY_SIZE=16;
		private final static int DATA_SIZE=1000;
		private final static int RSA_SIZE=128;
		
		private SecureRandom rand;
		private final class SocketListEntry
			{
				SocketListEntry(JAPSocket s)
					{
						inSocket=s;
						try
							{
								outStream=s.getOutputStream();
								arCipher=null;
							}
						catch(Exception e)
							{
								JAPDebug.out(JAPDebug.ERR,JAPDebug.NET,"Oops - SocketListEntry!");
							}
					}
				public JAPSocket inSocket;
				public OutputStream outStream;
				public JAPSymCipher[] arCipher;
			};

		JAPMuxSocket()
			{
				lastChannelId=0;
				oSocketList=new Hashtable();
				oRSA=new JAPASymCipher();
				outBuff=new byte[DATA_SIZE];
				JAPModel model=JAPModel.getModel();
				model.status2 = model.getString("initSecureRandom");
				model.notifyJAPObservers();
				rand=new SecureRandom(SecureRandom.getSeed(20));
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
						SocketListEntry tmpEntry=(SocketListEntry)oSocketList.get(new Integer(channel));
						if(tmpEntry!=null)
							{
								for(int i=0;i<chainlen;i++)
										tmpEntry.arCipher[i].encrypt(buff);
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
	/*					for(int l=0;l<1000;l++)
							buff[l]=65;
						len=512;
	*/				SocketListEntry entry=(SocketListEntry)oSocketList.get(new Integer(channel));
						if(entry!=null&&entry.arCipher==null)
							{
								int size=DATA_SIZE-16;
								byte[] key=new byte[KEY_SIZE];		
								entry.arCipher=new JAPSymCipher[chainlen];
								for(int i=chainlen-1;i>=0;i--)
									{
										entry.arCipher[i]=new JAPSymCipher();
										rand.nextBytes(key);
										key[0]=(byte)(key[0]&0x7F); //RSA HACK!! (to ensure what m<n in RSA-Encrypt: c=m^e mod n)
										entry.arCipher[i].setEncryptionKey(key);
										entry.arCipher[i].setDecryptionKey(key);
										System.arraycopy(key,0,outBuff,0,KEY_SIZE);
										System.arraycopy(buff,0,outBuff,KEY_SIZE,size);
										oRSA.encrypt(outBuff,0,outBuff,0);
										entry.arCipher[i].encrypt(outBuff,RSA_SIZE,outBuff,RSA_SIZE,DATA_SIZE-RSA_SIZE);
										System.arraycopy(outBuff,0,buff,0,DATA_SIZE);
										size-=KEY_SIZE;
										len+=KEY_SIZE;
									}						
							}
						else
							{
								if(buff!=null)
									{
										System.arraycopy(buff,0,outBuff,0,len);
										for(int i=chainlen-1;i>=0;i--)
											entry.arCipher[i].encrypt(outBuff);
									}	
							}
						outDataStream.writeInt(channel);
						outDataStream.writeShort(len);
						outDataStream.writeShort(0);
						outDataStream.write(outBuff);
						outDataStream.flush();
						JAPModel.getModel().increasNrOfBytes(len);
					}
				catch(Exception e)
					{
						e.printStackTrace();
						JAPDebug.out(JAPDebug.ERR,JAPDebug.NET,"Sending exception!");
						return -1;
					}
				return 0;
			}

	}
