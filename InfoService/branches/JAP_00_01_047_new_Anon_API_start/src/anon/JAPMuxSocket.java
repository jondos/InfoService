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
import java.io.BufferedReader;
import java.io.DataInputStream;
import java.io.BufferedWriter;
import java.io.Writer;
import java.io.OutputStreamWriter;
import java.io.InterruptedIOException;
import java.lang.Integer;
import java.util.Enumeration;
import java.math.BigInteger;
import JAPDebug;


import HTTPClient.Codecs;

final class JAPMuxSocket implements Runnable
	{
		private int lastChannelId;
		private Dictionary oSocketList;
		private DataOutputStream m_outDataStream;
		private DataInputStream m_inDataStream;

		private Socket m_ioSocket;

		private final static int FIREWALL_METHOD_HTTP_1_1=11;
		private final static int FIREWALL_METHOD_HTTP_1_0=10;

		private final static int[]FIREWALL_METHODS={FIREWALL_METHOD_HTTP_1_1,FIREWALL_METHOD_HTTP_1_0}; //which HTTP-Proxy methods to try
		private	byte[] outBuff;
		private JAPASymCipher[] m_arASymCipher;
		private JAPKeyPool keypool;
		private int chainlen;
		private volatile boolean m_bRunFlag;
		private boolean m_bIsConnected=false;

		private static final String CRLF="\r\n";

		public final static int KEY_SIZE=16;
		public final static int DATA_SIZE=992;
		public final static int PAYLOAD_SIZE=989;
		private final static int RSA_SIZE=128;
		private final static short CHANNEL_DATA=0;
		private final static short CHANNEL_CLOSE=1;
		private final static short CHANNEL_OPEN=8;

		public final static int E_ALREADY_CONNECTED=-8;
		public final static int E_NOT_CONNECTED=-9;

		private static JAPMuxSocket ms_MuxSocket=null;
		private int m_RunCount=0;

		private Thread threadRunLoop;

		private long m_TimeLastPacketSend=0;
		private static boolean m_bDummyTraffic=false;
		private JAPDummyTraffic m_DummyTraffic=null;

		private final class SocketListEntry
			{
				SocketListEntry(JAPSocket s) throws Exception
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
								JAPDebug.out(JAPDebug.ERR,JAPDebug.NET,"JAPMuxSocket:SocketListEntry() oops");
							  throw e;
							}
					}
				public final JAPSocket inSocket;
				public final OutputStream outStream;
				public JAPSymCipher[] arCipher;
				public boolean bIsSuspended;
			};

		private JAPMuxSocket()
			{
				lastChannelId=0;
				oSocketList=new Hashtable();
				m_arASymCipher=null;
				outBuff=new byte[DATA_SIZE];
				threadRunLoop=null;
				keypool=JAPKeyPool.start(/*20,16*/);
				m_RunCount=0;
				m_bDummyTraffic=false;
				m_TimeLastPacketSend=0;
				//threadgroupChannels=null;
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

		public static void setEnableDummyTraffic(boolean b)
			{
				if(b==m_bDummyTraffic)
					return;
				if(isConnected())
					{
						if(b)
							{
								ms_MuxSocket.m_DummyTraffic=new JAPDummyTraffic(ms_MuxSocket);
								ms_MuxSocket.m_DummyTraffic.start();
							}
						else
							{
								ms_MuxSocket.m_DummyTraffic.stop();
								ms_MuxSocket.m_DummyTraffic=null;
							}
					}
				ms_MuxSocket.m_bDummyTraffic=b;
			}

		public static boolean getEnableDummyTraffic()
			{
				return ms_MuxSocket.m_bDummyTraffic;
			}
		//2001-02-20(HF)
		public int connect(String host, int port) {
			return connectViaFirewall(host,port,null,-1,null,null);
		}

		//Write stuff for connecting over proxy/firewall
		// should look like this example
		//   CONNECT www.inf.tu-dresden.de:443 HTTP/1.0
		//   Connection: Keep-Alive
		//   Proxy-Connection: Keep-Alive
		//differs a little bit for HTTP/1.0 and HTTP/1.1
		private void sendHTTPProxyCommands(int firewallMethod,Writer out,String host,int port,String user,String passwd)
			throws Exception
			{
				try
					{
						if(firewallMethod==FIREWALL_METHOD_HTTP_1_1)
						  out.write("CONNECT "+host+":"+Integer.toString(port)+" HTTP/1.1"+CRLF);
						else
						  out.write("CONNECT "+host+":"+Integer.toString(port)+" HTTP/1.0"+CRLF);
						if(user!=null&&passwd!=null) // proxy authentication required...
							{
								String str=Codecs.base64Encode(user+":"+passwd);
								out.write("Proxy-Authorization: Basic "+str+CRLF);
							}
						out.write("Connection: Keep-Alive"+CRLF);
					  out.write("Keep-Alive: max=20, timeout=100"+CRLF);
						out.write("Proxy-Connection: Keep-Alive"+CRLF);
						out.write(CRLF);
						out.flush();
					}
				catch(Exception e)
					{
						throw e;
					}
			}

		//2001-02-20(HF)
		public int connectViaFirewall(String host, int port, String fwHost, int fwPort,String fwUserID,String fwPasswd)
			{
				synchronized(this)
					{
						if(m_bIsConnected)
							return E_ALREADY_CONNECTED;
						//2001-02-20(HF)
						if (fwHost==null)
							{
								try
									{
										//Connect directly to anon service
										JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"JAPMuxSocket:Try to connect directly to mix ("+host+":"+port+")");
										m_ioSocket=new Socket(host,port);
										m_ioSocket.setSoTimeout(10000); //Timout 10 second
										m_inDataStream=new DataInputStream(m_ioSocket.getInputStream());
									  m_bIsConnected=true;
									}
								catch(Exception e)
									{
										m_bIsConnected=false;
									}
							}
						else
							{
								//Connect via a firewall betwenn JAP and anon service
								for(int k=0;k<FIREWALL_METHODS.length;k++)
									{
										try
											{
											  JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"JAPMuxSocket:Try to connect via proxy ("+fwHost+":"+fwPort+") to mix ("+host+":"+port+")");
											  m_ioSocket=new Socket(fwHost,fwPort);
										    m_ioSocket.setSoTimeout(10000); //Timout 10 second
											  BufferedWriter o=new BufferedWriter(new OutputStreamWriter(m_ioSocket.getOutputStream()));
											  m_inDataStream=new DataInputStream(m_ioSocket.getInputStream());
											  sendHTTPProxyCommands(FIREWALL_METHODS[k],o,host,port,fwUserID,fwPasswd);
											  //Read response from proxy/firewall
											  // a typical response is
											  //   HTTP/1.0 200 Connection established
											  String firstLine = null;
											  try
												  {
													  firstLine = readLine(m_inDataStream);
												  }
											  catch(InterruptedIOException ei)
												  { //time out
													  m_bIsConnected=false;
														break;
												  }
												catch (Exception e)
													{
														JAPDebug.out(JAPDebug.EXCEPTION,JAPDebug.NET,"JAPMuxSocket:Exception while reading response from proxy server: "+e);
															m_bIsConnected=false;
															break;
													}
											  JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"JAPMuxSocket:Response from firewall is <"+firstLine+">");
											  JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"JAPMuxSocket:Reading remainig headers...");
											  String l=null;
										  	do
												  {
													  l = readLine(m_inDataStream);
													  JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"JAPMuxSocket: <"+l+">");
												  }while (l!=null&&l.length() != 0);
											  if(firstLine.indexOf("200")!=-1) //we get an ok
												  {
														m_bIsConnected=true;
													  break;
											    }
											}
										catch(Exception e)
											{
											}
									}
							}
						if(!m_bIsConnected)
							{
								JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"JAPMuxSocket:Something goes wrong by trying to connect to Mix!");
								try{m_inDataStream.close();}catch(Exception e){};
								try{m_ioSocket.close();}catch(Exception e){};
								m_inDataStream=null;
								m_ioSocket=null;
								return -1;
							}
						JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"JAPMuxSocket:Connected to Mix! Now starting key exchange...");
						try
							{
								m_outDataStream=new DataOutputStream(new BufferedOutputStream(m_ioSocket.getOutputStream(),DATA_SIZE+6));
								JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"JAPMuxSocket:Reading len...");
								m_inDataStream.readUnsignedShort(); //len.. unitressteing at the moment
								JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"JAPMuxSocket:Reading chainlen...");
								chainlen=m_inDataStream.readByte();
								JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"JAPMuxSocket:chainlen="+chainlen);
								m_arASymCipher=new JAPASymCipher[chainlen];
								for(int i=chainlen-1;i>=0;i--)
									{
										m_arASymCipher[i]=new JAPASymCipher();
										int tmp=m_inDataStream.readUnsignedShort();
										byte[] buff=new byte[tmp];
										m_inDataStream.readFully(buff);
										BigInteger n=new BigInteger(1,buff);
										tmp=m_inDataStream.readUnsignedShort();
										buff=new byte[tmp];
										m_inDataStream.readFully(buff);
										BigInteger e=new BigInteger(1,buff);
										m_arASymCipher[i].setPublicKey(n,e);
									}
								m_ioSocket.setSoTimeout(0); //Now we have a unlimited timeout...
							}
						catch(Exception e)
							{
								JAPDebug.out(JAPDebug.EXCEPTION,JAPDebug.NET,"JAPMuxSocket:Exception during connection: "+e);
								m_arASymCipher=null;
							  try{m_inDataStream.close();}catch(Exception e1){}
							  try{m_outDataStream.close();}catch(Exception e1){}
							  try{m_ioSocket.close();}catch(Exception e1){}
								m_inDataStream=null;
								m_outDataStream=null;
								m_ioSocket=null;
								m_bIsConnected=false;
								return -1;
							}
						m_bIsConnected=true;
						setEnableDummyTraffic(m_bDummyTraffic);
						return 0;
					}
			}

	  private String readLine(DataInputStream inputStream) throws Exception
		  {
				StringBuffer strBuff=new StringBuffer(256);
				try
					{
						int byteRead = inputStream.read();
						while (byteRead != 10 && byteRead != -1)
							{
								if (byteRead != 13)
									strBuff.append((char)byteRead);
								byteRead = inputStream.read();
							}
					}
				catch (Exception e)
					{
						throw e;
					}
				return strBuff.toString();
			}

	  public int newConnection(JAPSocket s,int type) throws ConnectException
			{
				synchronized(this)
					{
						if(m_bIsConnected)
							{
								try
									{
                    JAPAnonChannel p=new JAPAnonChannel(s,lastChannelId,type,this);
										oSocketList.put(new Integer(lastChannelId),new SocketListEntry(s));

										JAPAnonService.setNrOfChannels(oSocketList.size());
										Thread t2=new Thread(p);
										t2.start();
										lastChannelId++;
										return 0;
									}
								catch(Exception e)
									{
									  throw new ConnectException("Error trying open a new channel!");
									}
							}
						else
							throw new ConnectException("Lost connection to mix or not connected to a mix!");
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
						if(m_bDummyTraffic)
							{
								m_DummyTraffic.stop();
								m_DummyTraffic=null;
							}
						JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"JAPMuxSocket:close() Closing MuxSocket...");
						m_bRunFlag=false;
						try
							{
								threadRunLoop.join(1000);
							}
						catch(Exception e)
							{
								//e.printStackTrace();
							}
						if(threadRunLoop.isAlive())
							{
								JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"JAPMuxSocket:close() Closing MuxSocket harder...");
								try{m_inDataStream.close();}catch(Exception e1){}
								//try{threadgroupChannels.stop();}catch(Exception e2){}
								//try{threadgroupChannels.destroy();}catch(Exception e3){}
								try{threadRunLoop.join(2000);}catch(Exception e){}
								if(threadRunLoop.isAlive())
									{
										JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"JAPMuxSocket:close() Hm...MuxSocket is still alive...");
										threadRunLoop.stop();
										runStoped();
									}
							}
						threadRunLoop=null;
						JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"JAPMuxSocket:close() MuxSocket closed!");
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
						JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"JAPMuxSocket:stopService()");
						m_RunCount--;
						if(m_RunCount==0)
							close();
						return m_RunCount;
					}
			}


		private void runStoped()
			{
				JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"JAPMuxSocket:runStoped()");
				Enumeration e=oSocketList.keys();
				while(e.hasMoreElements())
					{
						Integer key=(Integer)e.nextElement();
						SocketListEntry entry=(SocketListEntry)oSocketList.get(key);
						close(key.intValue());
						try{entry.inSocket.close();}catch(Exception ie){}
						oSocketList.remove(key);
					}
				JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"JAPMuxSocket:MuxSocket all channels closed...");
				m_bRunFlag=false;
				m_bIsConnected=false;
				try{m_inDataStream.close();}catch(Exception e1){}
				try{m_outDataStream.close();}catch(Exception e2){}
				try{m_ioSocket.close();}catch(Exception e3){}
				JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"JAPMuxSocket:MuxSocket socket closed...");
				m_inDataStream=null;
				m_outDataStream=null;
				m_ioSocket=null;
				JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"JAPMuxSocket:Updating View...");
				JAPAnonService.setNrOfChannels(oSocketList.size());
				JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"JAPMuxSocket:All done..");
			}

		public void run()
			{
 				JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"JAPMuxSocket:run()");
				byte[] buff=new byte[DATA_SIZE];
				int flags=0;
				int channel=0;
				int len=0;
				m_bRunFlag=true;
				while(m_bRunFlag)
					{

						try
							{
								channel=m_inDataStream.readInt();
								flags=m_inDataStream.readShort();
								m_inDataStream.readFully(buff);
							}
						catch(Exception e)
							{
								JAPDebug.out(JAPDebug.ERR,JAPDebug.NET,"JAPMuxSocket:run() Exception while receiving!");
								break;
							}
						SocketListEntry tmpEntry=(SocketListEntry)oSocketList.get(new Integer(channel));
						if(tmpEntry!=null)
							{
								if(flags==CHANNEL_CLOSE)
									{
										oSocketList.remove(new Integer(channel));
										JAPAnonService.setNrOfChannels(oSocketList.size());
										try{tmpEntry.outStream.close();}catch(Exception e){}
										try{tmpEntry.inSocket.close();}catch(Exception e){}
									}
								else if(flags==CHANNEL_DATA)
									{
										for(int i=0;i<chainlen;i++)
												tmpEntry.arCipher[i].encryptAES2(buff);
										len=(buff[0]<<8)|(buff[1]&0xFF);
										len&=0x0000FFFF;
										if(len<0||len>DATA_SIZE)
											JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"JAPMuxSocket:Receveived MuxPacket with invalid data size: "+Integer.toString(len));
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
															JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"JAPMuxSocket:Fehler bei write to browser...retrying..."+e.toString());
														}
												}
											try{tmpEntry.outStream.flush();}catch(Exception e){}
											JAPAnonService.increaseNrOfBytes(len);
										}
									}
						/*		else if(flags==CHANNEL_SUSPEND)
									{
										tmpEntry.bIsSuspended=true;
										JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"JAPMuxSocket:Suspending Channel: "+Integer.toString(channel));
										}
								else if(flags==CHANNEL_RESUME)
									{
										tmpEntry.bIsSuspended=false;
										JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"JAPMuxSocket:Resuming Channel: "+Integer.toString(channel));
									}*/
							}
					}
				runStoped();
				JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"JAPMuxSocket:MuxSocket thread run exited...");
			}

		public synchronized int send(int channel,int type,byte[] buff,short len)
			{
				try
					{
						if(!m_bIsConnected)
							return E_NOT_CONNECTED;

					short channelMode=CHANNEL_DATA;
						m_TimeLastPacketSend=System.currentTimeMillis();
						if(buff==null&&len==0)
							{
								m_outDataStream.writeInt(channel);
								m_outDataStream.writeShort(CHANNEL_CLOSE);
								m_outDataStream.write(outBuff);
								m_outDataStream.flush();
								return 0;
							}
						if(buff==null)
							return -1;
						if(len==0)
							return 0;
						SocketListEntry entry=(SocketListEntry)oSocketList.get(new Integer(channel));
						if(entry!=null&&entry.arCipher==null)
							{
								int size=PAYLOAD_SIZE-KEY_SIZE;
								entry.arCipher=new JAPSymCipher[chainlen];

								//Last Mix
								entry.arCipher[chainlen-1]=new JAPSymCipher();
								keypool.getKey(outBuff);
								outBuff[0]&=0x7F; //RSA HACK!! (to ensure what m<n in RSA-Encrypt: c=m^e mod n)

								outBuff[KEY_SIZE]=(byte)(len>>8);
								outBuff[KEY_SIZE+1]=(byte)(len%256);
								if(type==JAPAnonService.PROTO_SOCKS)
									outBuff[KEY_SIZE+2]=1;
								else
									outBuff[KEY_SIZE+2]=0;

								System.arraycopy(buff,0,outBuff,KEY_SIZE+3,size);

								entry.arCipher[chainlen-1].setEncryptionKeyAES(outBuff);
								m_arASymCipher[chainlen-1].encrypt(outBuff,0,buff,0);
								entry.arCipher[chainlen-1].encryptAES(outBuff,RSA_SIZE,buff,RSA_SIZE,DATA_SIZE-RSA_SIZE);
								size-=KEY_SIZE;
								for(int i=chainlen-2;i>=0;i--)
									{
										entry.arCipher[i]=new JAPSymCipher();
										keypool.getKey(outBuff);
										outBuff[0]&=0x7F; //RSA HACK!! (to ensure what m<n in RSA-Encrypt: c=m^e mod n)
										entry.arCipher[i].setEncryptionKeyAES(outBuff);
										System.arraycopy(buff,0,outBuff,KEY_SIZE,size);
										m_arASymCipher[i].encrypt(outBuff,0,buff,0);
										entry.arCipher[i].encryptAES(outBuff,RSA_SIZE,buff,RSA_SIZE,DATA_SIZE-RSA_SIZE);
										size-=KEY_SIZE;
									}
							  channelMode=CHANNEL_OPEN;
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
						m_outDataStream.writeInt(channel);
						m_outDataStream.writeShort(channelMode);
						m_outDataStream.write(buff,0,DATA_SIZE);
						m_outDataStream.flush();
						JAPAnonService.increaseNrOfBytes(len);
						//if(entry!=null&&entry.bIsSuspended)
						//	return E_CHANNEL_SUSPENDED;
					}
				catch(Exception e)
					{
						//e.printStackTrace();
						JAPDebug.out(JAPDebug.ERR,JAPDebug.NET,"JAPMuxSocket:send() Exception!");
						return -1;
					}
				return 0;
			}

		public final int getChainLen()
			{
				return chainlen;
			}

		public long getTimeLastPacketSend()
			{
				return m_TimeLastPacketSend;
			}
	}
