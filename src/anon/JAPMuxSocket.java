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
package anon;

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
import JAPDebug;

final class JAPMuxSocket implements Runnable
	{
		private int lastChannelId;
		private Dictionary oSocketList;
		private DataOutputStream outDataStream;
		private DataInputStream inDataStream;
		
		private Socket ioSocket;
		private	byte[] outBuff;
		private JAPASymCipher[] arASymCipher;
		private JAPKeyPool keypool;
		private int chainlen;
		private volatile boolean m_bRunFlag;
		private boolean m_bIsConnected=false;
		
		public final static int KEY_SIZE=16;
		public final static int DATA_SIZE=992;
		private final static int RSA_SIZE=128;
		private final static short CHANNEL_DATA=0;
		private final static short CHANNEL_CLOSE=1;
	/*	#define CHANNEL_DATA		0x00
	#define CHANNEL_OPEN		0x00
	#define CHANNEL_CLOSE		0x01
	#define CHANNEL_SUSPEND 0x02
	#define	CHANNEL_RESUME	0x04
	*/	//private final static short CHANNEL_RESUME=1;
		//private final static short CHANNEL_SUSPEND=1;
		//public final static int E_CHANNEL_SUSPENDED=-7;	
		public final static int E_ALREADY_CONNECTED=-8;	
		public final static int E_NOT_CONNECTED=-8;	
		
		private static JAPMuxSocket ms_MuxSocket=null;
		private int m_RunCount=0;
		
		private Thread threadRunLoop;

		private final class SocketListEntry
			{
				SocketListEntry(JAPSocket s)
					{
						inSocket=s;
						try
							{
								outStream=s.getOutputStream();
								arCipher=null;
								bIsSuspended=false;
							}
						catch(Exception e)
							{
								JAPDebug.out(JAPDebug.ERR,JAPDebug.NET,"Oops - SocketListEntry!");
							}
					}
				public JAPSocket inSocket;
				public OutputStream outStream;
				public JAPSymCipher[] arCipher;
				public boolean bIsSuspended;
			};

		private JAPMuxSocket()
			{
				lastChannelId=0;
				oSocketList=new Hashtable();
				arASymCipher=null;
				outBuff=new byte[DATA_SIZE];
				threadRunLoop=null;
				keypool=new JAPKeyPool(20,16);
				Thread t1 = new Thread (keypool);
				t1.setPriority(Thread.MIN_PRIORITY);
				t1.start();
				m_RunCount=0;
			}

		public static JAPMuxSocket create()
			{
				if(ms_MuxSocket==null)
					ms_MuxSocket=new JAPMuxSocket();
				return ms_MuxSocket;
			}
		
		public static boolean isConnected()
			{
				return (ms_MuxSocket!=null&&ms_MuxSocket.m_bIsConnected);
			}
		
		public int connect(String host, int port)
			{
				synchronized(this)
					{
						if(m_bIsConnected)
							return E_ALREADY_CONNECTED;
						try
							{
								JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"Try to connect to Mix");														
								ioSocket=new Socket(host,port);
								JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"Connected to Mix - starting Key-Exchange...");														
								outDataStream=new DataOutputStream(new BufferedOutputStream(ioSocket.getOutputStream(),DATA_SIZE+6));
								inDataStream=new DataInputStream(ioSocket.getInputStream());
								JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"Reading len...");														
								ioSocket.setSoTimeout(1000); //Timout 1 second
								inDataStream.readUnsignedShort(); //len.. unitressteing at the moment
								JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"Reading Mix-Count...");														
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
								ioSocket.setSoTimeout(0); //Now we have a unlimit time out...
							}
						catch(Exception e)
							{
								arASymCipher=null;
								m_bIsConnected=false;
								return -1;
							}
						m_bIsConnected=true;
						return 0;
					}
			}

		public int newConnection(JAPSocket s,int type) throws ConnectException
			{
				synchronized(this)
					{
						if(m_bIsConnected)
							{
								JAPAnonChannel p=new JAPAnonChannel(s,lastChannelId,type,this);
								oSocketList.put(new Integer(lastChannelId),new SocketListEntry(s));
								
								JAPAnonService.setNrOfChannels(oSocketList.size());
								p.start();
								lastChannelId++;
								return 0;
							}
						else
							throw new ConnectException("Not connected to a MIX!");
					}
			}

		public int close(int channel)
			{
				synchronized(this)
					{
						oSocketList.remove(new Integer(channel));
						send(channel,0,null,(short)0);
						JAPAnonService.setNrOfChannels(oSocketList.size());
						return 0;
					}
			}

		private int close()
			{
				synchronized(this)
					{
						if(!m_bIsConnected)
							return E_NOT_CONNECTED;
						JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"Closing MuxSocket...");														
						m_bRunFlag=false;
						try	
							{
								threadRunLoop.join(100);
							}
						catch(Exception e)
							{
								e.printStackTrace();
							}
						if(threadRunLoop.isAlive())
							{
								try{inDataStream.close();}catch(Exception e1){}
								try{threadRunLoop.join(2000);}catch(Exception e){e.printStackTrace();}
								if(threadRunLoop.isAlive())
									{
										JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"Hm.. still alive");														
										threadRunLoop.stop();
										runStoped();
									}
							}
						threadRunLoop=null;
						JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"MuxSocket closed!");														
						return 0;
					}
			}

		public int startService()
			{
				synchronized(this)
					{
						if(!m_bIsConnected)
							return -1;
						if(m_RunCount==0)
							{
								threadRunLoop=new Thread(this);
								threadRunLoop.setPriority(Thread.MAX_PRIORITY);
								threadRunLoop.start();
							}
						m_RunCount++;
						return m_RunCount;
					}
			}
		
		public int stopService()
			{	
				synchronized(this)
					{
						JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"Stopping Service...");														
						m_RunCount--;
						if(m_RunCount==0)
							close();
						return m_RunCount;
					}
			}
		
		
		private void runStoped()
			{
				JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"MuxSocket run stoped...");														
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
				JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"MuxSocket all channels closed...");														
				m_bRunFlag=false;
				m_bIsConnected=false;
				try{inDataStream.close();}catch(Exception e1){}
				try{outDataStream.close();}catch(Exception e2){}
				try{ioSocket.close();}catch(Exception e3){}
				JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"MuxSocket socket closed...");														
				inDataStream=null;
				outDataStream=null;
				ioSocket=null;
				JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"Updating View...");														
				JAPAnonService.setNrOfChannels(oSocketList.size());
				JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"All done..");														
			}
		
		public void run()
			{
 				JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"MuxSocket is running...");														
				byte[] buff=new byte[DATA_SIZE];
				int flags=0;
				int channel=0;
				int len=0;
				m_bRunFlag=true;
				while(m_bRunFlag)
					{
						try
							{
								channel=inDataStream.readInt();
								flags=inDataStream.readShort();
								inDataStream.readFully(buff);
							}
						catch(Exception e)
							{
								JAPDebug.out(JAPDebug.ERR,JAPDebug.NET,"CAMuxSocket-run-Exception: receive");
								break;
							}
						SocketListEntry tmpEntry=(SocketListEntry)oSocketList.get(new Integer(channel));
						if(tmpEntry!=null)
							{
								if(flags==CHANNEL_CLOSE)
									{
										oSocketList.remove(new Integer(channel));
										JAPAnonService.setNrOfChannels(oSocketList.size());
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
								else if(flags==CHANNEL_DATA)
									{
										for(int i=0;i<chainlen;i++)
												tmpEntry.arCipher[i].encryptAES(buff);									
										len=(buff[0]<<8)|(buff[1]&0xFF);
										len&=0x0000FFFF;
										if(len<0||len>DATA_SIZE)
											JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"Receveived MuxPacket with invalid data size: "+Integer.toString(len));											
										else
										{
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
														}
												}
											try{tmpEntry.outStream.flush();}catch(Exception e){e.printStackTrace();};
											JAPAnonService.increaseNrOfBytes(len);
										}
									}
						/*		else if(flags==CHANNEL_SUSPEND)
									{
										tmpEntry.bIsSuspended=true;
										JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"Suspending Channel: "+Integer.toString(channel));
										}
								else if(flags==CHANNEL_RESUME)
									{	
										tmpEntry.bIsSuspended=false;
										JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"Resumeing Channel: "+Integer.toString(channel));
									}*/
							}
					}
				runStoped();
				JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"MuxSocket thread run exited...");														
			}

		public synchronized int send(int channel,int type,byte[] buff,short len)
			{
				try
					{
						if(!m_bIsConnected)
							return E_NOT_CONNECTED;
						if(buff==null&&len==0)
							{
								outDataStream.writeInt(channel);
								outDataStream.writeShort(CHANNEL_CLOSE);
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
								if(type==JAPAnonService.PROTO_SOCKS)
									outBuff[KEY_SIZE+2]=1;
								else
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
						outDataStream.writeShort(CHANNEL_DATA);
						outDataStream.write(buff,0,DATA_SIZE);
						outDataStream.flush();
						JAPAnonService.increaseNrOfBytes(len);
						//if(entry!=null&&entry.bIsSuspended)
						//	return E_CHANNEL_SUSPENDED;
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
