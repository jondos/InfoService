/*
Copyright (c) 2000, The JAP-Team 
All rights reserved.
Redistribution and use in source and binary forms, with or without modification, 
are permitted provided that the following conditions are met:

	- Redistributions of source code must retain the above copyright notice, 
	  this list of conditions and the following disclaimer.

	- Redistributions in binary form must reproduce the above copyright notice, 
	  this list of conditions and the following disclaimer in the documentation and/or 
		other materials provided with the distribution.

	- Neither the name of the University of Technology Dresden, Germany nor the names of its contributors 
	  may be used to endorse or promote products derived from this software without specific 
		prior written permission. 

	
THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS ``AS IS'' AND ANY EXPRESS 
OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY 
AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS
BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, 
OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER 
IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY 
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE
*/
import java.util.Dictionary;
import java.util.Hashtable;
import java.net.Socket;
import java.net.ConnectException;
import java.io.OutputStream;
import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.BufferedInputStream;
import java.io.DataInputStream;
import java.lang.Integer;
import java.util.Enumeration;
import java.math.BigInteger;
public final class JAPMuxSocket extends Thread /*implements Runnable*/
	{
/*PROTO1
		private int lastChannelId;
		private Dictionary oSocketList;
		private DataOutputStream outDataStream;
		private DataInputStream inDataStream;
		
		private Socket ioSocket;
		private	byte[] outBuff;
		private JAPASymCipher[] arASymCipher;
		private JAPKeyPool keypool;
		private int chainlen;
		private volatile boolean runflag;
		
		private final static int KEY_SIZE=16;
		private final static int DATA_SIZE=1000;
		private final static int RSA_SIZE=128;
		
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
				arASymCipher=null;
				outBuff=new byte[DATA_SIZE];
				keypool=JAPModel.getModel().keypool;
			}

		public int connect(String host, int port)
			{
				try
					{
						ioSocket=new Socket(host,port);
						outDataStream=new DataOutputStream(new BufferedOutputStream(ioSocket.getOutputStream(),1008));
						inDataStream=new DataInputStream(ioSocket.getInputStream());
						inDataStream.readUnsignedShort(); //len.. unitressteing at the moment
						chainlen=inDataStream.readByte();
						arASymCipher=new JAPASymCipher[chainlen];
						for(int i=chainlen-1;i>=0;i--)
							{
								arASymCipher[i]=new JAPASymCipher();
								int tmp=inDataStream.readUnsignedShort();
								byte[] buff=new byte[tmp];
								inDataStream.readFully(buff);
								BigInteger n=new BigInteger(1,buff);
								tmp=inDataStream.readUnsignedShort();
								buff=new byte[tmp];
								inDataStream.readFully(buff);
								BigInteger e=new BigInteger(1,buff);
								arASymCipher[i].setPublicKey(n,e);
							}
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
										tmpEntry.arCipher[i].encryptAES(buff);
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
						SocketListEntry entry=(SocketListEntry)oSocketList.get(new Integer(channel));
						if(entry!=null&&entry.arCipher==null)
							{
								int size=DATA_SIZE-16;
								byte[] key=new byte[KEY_SIZE];		
								entry.arCipher=new JAPSymCipher[chainlen];
								for(int i=chainlen-1;i>=0;i--)
									{
										entry.arCipher[i]=new JAPSymCipher();
										keypool.getKey(key);
										key[0]=(byte)(key[0]&0x7F); //RSA HACK!! (to ensure what m<n in RSA-Encrypt: c=m^e mod n)
										entry.arCipher[i].setEncryptionKeyAES(key);
	//									entry.arCipher[i].setEncryptionKey(key);
										
										System.arraycopy(key,0,outBuff,0,KEY_SIZE);
										System.arraycopy(buff,0,outBuff,KEY_SIZE,size);
										arASymCipher[i].encrypt(outBuff,0,outBuff,0);
										entry.arCipher[i].encryptAES(outBuff,RSA_SIZE,outBuff,RSA_SIZE,DATA_SIZE-RSA_SIZE);
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
											entry.arCipher[i].encryptAES(outBuff); //something throws a null pointer....
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
END PROTO1*/
///*PROTO2
		private int lastChannelId;
		private Dictionary oSocketList;
		private DataOutputStream outDataStream;
		private DataInputStream inDataStream;
		
		private Socket ioSocket;
		private	byte[] outBuff;
		private JAPASymCipher[] arASymCipher;
		private JAPKeyPool keypool;
		private int chainlen;
		private volatile boolean runflag;
		private boolean bIsConnected=false;
		
		public final static int KEY_SIZE=16;
		public final static int DATA_SIZE=992;
		private final static int RSA_SIZE=128;
		
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
				arASymCipher=null;
				outBuff=new byte[DATA_SIZE];
				keypool=JAPModel.getModel().keypool;
			}

		public int connect(String host, int port)
			{
				try
					{
						ioSocket=new Socket(host,port);
						outDataStream=new DataOutputStream(new BufferedOutputStream(ioSocket.getOutputStream(),DATA_SIZE+6));
						inDataStream=new DataInputStream(ioSocket.getInputStream());
						inDataStream.readUnsignedShort(); //len.. unitressteing at the moment
						chainlen=inDataStream.readByte();
						arASymCipher=new JAPASymCipher[chainlen];
						for(int i=chainlen-1;i>=0;i--)
							{
								arASymCipher[i]=new JAPASymCipher();
								int tmp=inDataStream.readUnsignedShort();
								byte[] buff=new byte[tmp];
								inDataStream.readFully(buff);
								BigInteger n=new BigInteger(1,buff);
								tmp=inDataStream.readUnsignedShort();
								buff=new byte[tmp];
								inDataStream.readFully(buff);
								BigInteger e=new BigInteger(1,buff);
								arASymCipher[i].setPublicKey(n,e);
							}
					}
				catch(Exception e)
					{
						bIsConnected=false;
						return -1;
					}
				bIsConnected=true;
				return 0;
			}

		public synchronized int newConnection(JAPSocket s) throws ConnectException
			{
				if(bIsConnected)
					{
						JAPProxyConnection p=new JAPProxyConnection(s,lastChannelId,this);
						oSocketList.put(new Integer(lastChannelId),new SocketListEntry(s));
						JAPModel.getModel().setNrOfChannels(oSocketList.size());
						p.start();
						lastChannelId++;
						return 0;
					}
				else
					throw new ConnectException("Not connected to a MIX!");
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
				bIsConnected=false;
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
 				byte[] buff=new byte[DATA_SIZE];
				int flags=0;
				int channel=0;
				int len=0;
				runflag=true;
				while(runflag)
					{
						try
							{
								channel=inDataStream.readInt();
								flags=inDataStream.readShort();
								inDataStream.readFully(buff);
							}
						catch(Exception e)
							{
								runflag=false;
								bIsConnected=false;
								JAPDebug.out(JAPDebug.ERR,JAPDebug.NET,"CAMuxSocket-run-Exception: receive");
								break;
							}
						SocketListEntry tmpEntry=(SocketListEntry)oSocketList.get(new Integer(channel));
						if(tmpEntry!=null)
							{
								if(flags!=0)
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
												e.printStackTrace();
											}
									}
								else
									{
										for(int i=0;i<chainlen;i++)
												tmpEntry.arCipher[i].encryptAES(buff);									
										len=(buff[0]<<8)|(buff[1]&0xFF);
										for(int i=0;i<3;i++)
											{
												try
													{
														tmpEntry.outStream.write(buff,3,len);
														break;
													}
												catch(Exception e)
													{
														JAPDebug.out(JAPDebug.WARNING,JAPDebug.NET,"Fehler bei write to browser...retrying..."+e.toString());														
														len=0;
													}
											}
										try{tmpEntry.outStream.flush();}catch(Exception e){e.printStackTrace();};
										JAPModel.getModel().increasNrOfBytes(len);
									}
							}
					}
			}

		public synchronized int send(int channel,byte[] buff,short len)
			{
				try
					{
						if(buff==null&&len==0)
							{
								outDataStream.writeInt(channel);
								outDataStream.writeShort(1);
								outDataStream.write(outBuff);
								outDataStream.flush();
								return 0;
							}
						if(buff==null)
							return -1;
						if(len==0)
							return 0;
						SocketListEntry entry=(SocketListEntry)oSocketList.get(new Integer(channel));
						if(entry!=null&&entry.arCipher==null)
							{
								int size=DATA_SIZE-KEY_SIZE;
								entry.arCipher=new JAPSymCipher[chainlen];
								
								//Last Mix
								entry.arCipher[chainlen-1]=new JAPSymCipher();
								keypool.getKey(outBuff);
								outBuff[0]&=0x7F; //RSA HACK!! (to ensure what m<n in RSA-Encrypt: c=m^e mod n)
								
								System.arraycopy(buff,0,outBuff,KEY_SIZE+3,size-3);
								outBuff[KEY_SIZE]=(byte)(len>>8);
								outBuff[KEY_SIZE+1]=(byte)(len%256);
								outBuff[KEY_SIZE+2]=0;
																
								entry.arCipher[chainlen-1].setEncryptionKeyAES(outBuff);
								arASymCipher[chainlen-1].encrypt(outBuff,0,buff,0);
								entry.arCipher[chainlen-1].encryptAES(outBuff,RSA_SIZE,buff,RSA_SIZE,DATA_SIZE-RSA_SIZE);
								size-=KEY_SIZE;
								for(int i=chainlen-2;i>=0;i--)
									{
										entry.arCipher[i]=new JAPSymCipher();
										keypool.getKey(outBuff);
										outBuff[0]&=0x7F; //RSA HACK!! (to ensure what m<n in RSA-Encrypt: c=m^e mod n)
										entry.arCipher[i].setEncryptionKeyAES(outBuff);
										System.arraycopy(buff,0,outBuff,KEY_SIZE,size);
										arASymCipher[i].encrypt(outBuff,0,buff,0);
										entry.arCipher[i].encryptAES(outBuff,RSA_SIZE,buff,RSA_SIZE,DATA_SIZE-RSA_SIZE);
										size-=KEY_SIZE;
									}						
							}
						else
							{
								System.arraycopy(buff,0,outBuff,3,len);
								outBuff[2]=0;
								outBuff[0]=(byte)(len>>8);
								outBuff[1]=(byte)(len%256);
								for(int i=chainlen-1;i>0;i--)
									entry.arCipher[i].encryptAES(outBuff); //something throws a null pointer....
								entry.arCipher[0].encryptAES(outBuff,0,buff,0,DATA_SIZE); //something throws a null pointer....			
							}
						outDataStream.writeInt(channel);
						outDataStream.writeShort(0);
						outDataStream.write(buff,0,DATA_SIZE);
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
		
		public final int getChainLen()
			{
				return chainlen;
			}

	}
