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

package anon.server.impl;

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
import anon.ErrorCodes;
public final class MuxSocket implements Runnable
	{
		private int lastChannelId;
		private Dictionary oChannelList;
		private DataOutputStream m_outDataStream;
		private DataInputStream m_inDataStream;

		private Socket m_ioSocket;

		private final static int FIREWALL_METHOD_HTTP_1_1=11;
		private final static int FIREWALL_METHOD_HTTP_1_0=10;

		private final static int[]FIREWALL_METHODS={FIREWALL_METHOD_HTTP_1_1,FIREWALL_METHOD_HTTP_1_0}; //which HTTP-Proxy methods to try
		private	byte[] outBuff;
		private ASymCipher[] m_arASymCipher;
		private KeyPool keypool;
		private int chainlen;
		private volatile boolean m_bRunFlag;
		private boolean m_bIsConnected=false;

		private static final String CRLF="\r\n";

		public final static int KEY_SIZE=16;
		public final static int DATA_SIZE=992;
		public final static int PAYLOAD_SIZE=989;
		private final static int PACKET_SIZE=998;  //DATA_SIZE+6
		private final static int RSA_SIZE=128;
		private final static short CHANNEL_DATA=0;
		private final static short CHANNEL_CLOSE=1;
		private final static short CHANNEL_OPEN=8;

		private static MuxSocket ms_MuxSocket=null;
		//private int m_RunCount=0;

		private Thread threadRunLoop;

		private long m_TimeLastPacketSend=0;
		private static boolean m_bDummyTraffic=false;
		private DummyTraffic m_DummyTraffic=null;

		private final class ChannelListEntry
			{
				ChannelListEntry(AbstractChannel c)
					{
						channel=c;
						arCipher=null;
						bIsSuspended=false;
					}
				public final AbstractChannel channel;
				public SymCipher[] arCipher;
				public boolean bIsSuspended;
			};

		private MuxSocket()
			{
				lastChannelId=0;
				m_arASymCipher=null;
				outBuff=new byte[DATA_SIZE];
				threadRunLoop=null;
				keypool=KeyPool.start(/*20,16*/);
				//m_RunCount=0;
				m_bDummyTraffic=false;
				m_TimeLastPacketSend=0;
				//threadgroupChannels=null;
			}

		public static MuxSocket create()
			{
				if(ms_MuxSocket==null)
					ms_MuxSocket=new MuxSocket();
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
								ms_MuxSocket.m_DummyTraffic=new DummyTraffic(ms_MuxSocket);
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
	/*	//2001-02-20(HF)
		public int connect(String host, int port) {
			return connectViaFirewall(host,port,null,-1,null,null);
		}
*/
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
							return ErrorCodes.E_ALREADY_CONNECTED;
            try
              {
                //Connect directly to anon service
  							JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"MuxSocket:Try to connect directly to mix ("+host+":"+port+")");
                m_ioSocket=new Socket(host,port);
                m_ioSocket.setSoTimeout(10000); //Timout 10 second
                m_inDataStream=new DataInputStream(m_ioSocket.getInputStream());
                m_bIsConnected=true;
              }
            catch(Exception e)
              {
  						  JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"MuxSocket : Error(1) connection to mix: "+e.toString());
                m_bIsConnected=false;
              }
						if(!m_bIsConnected)
							{
								//JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"JAPMuxSocket:Something goes wrong by trying to connect to Mix!");
								try{m_inDataStream.close();}catch(Exception e){};
								try{m_ioSocket.close();}catch(Exception e){};
								m_inDataStream=null;
								m_ioSocket=null;
								return ErrorCodes.E_CONNECT;
							}
						//JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"JAPMuxSocket:Connected to Mix! Now starting key exchange...");
						try
							{
								m_outDataStream=new DataOutputStream(new BufferedOutputStream(m_ioSocket.getOutputStream(),PACKET_SIZE));
							//	JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"JAPMuxSocket:Reading len...");
								m_inDataStream.readUnsignedShort(); //len.. unitressteing at the moment
							//	JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"JAPMuxSocket:Reading chainlen...");
								chainlen=m_inDataStream.readByte();
								//JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"JAPMuxSocket:chainlen="+chainlen);
								m_arASymCipher=new ASymCipher[chainlen];
								for(int i=chainlen-1;i>=0;i--)
									{
										m_arASymCipher[i]=new ASymCipher();
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
	//							m_ioSocket.setSoTimeout(1000); //Now we have asecond timeout...
							}
						catch(Exception e)
							{
								JAPDebug.out(JAPDebug.EXCEPTION,JAPDebug.NET,"MuxSocket:Exception(2) during connection: "+e);
								m_arASymCipher=null;
							  try{m_inDataStream.close();}catch(Exception e1){}
							  try{m_outDataStream.close();}catch(Exception e1){}
							  try{m_ioSocket.close();}catch(Exception e1){}
								m_inDataStream=null;
								m_outDataStream=null;
								m_ioSocket=null;
								m_bIsConnected=false;
								return ErrorCodes.E_CONNECT;
							}
						m_bIsConnected=true;
     				oChannelList=new Hashtable();
            return ErrorCodes.E_SUCCESS;
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


   public Channel newChannel(int type) throws ConnectException
			{
				synchronized(this)
					{
						if(m_bIsConnected)
							{
								try
									{
                    Channel c=new Channel(this,lastChannelId,type);
										oChannelList.put(new Integer(lastChannelId),new ChannelListEntry(c));

										//JAPAnonService.setNrOfChannels(oSocketList.size());
										//Thread t2=new Thread(c);
										//t2.start();
										lastChannelId++;
										return c;
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

		public int close(int channel_id)
			{
				synchronized(this)
					{
						oChannelList.remove(new Integer(channel_id));
						send(channel_id,0,null,(short)0);
	//					JAPAnonService.setNrOfChannels(oSocketList.size());
						return 0;
					}
			}


		public int startService()
			{
				synchronized(this)
					{
						if(!m_bIsConnected)
							return ErrorCodes.E_NOT_CONNECTED;
              threadRunLoop=new Thread(this);
							threadRunLoop.setPriority(Thread.MAX_PRIORITY);
              threadRunLoop.start();
					}
        return ErrorCodes.E_SUCCESS;
			}

		public void stopService()
			{
				synchronized(this)
					{
						//JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"JAPMuxSocket:stopService()");
						//m_RunCount--;
						//if(m_RunCount==0)
            close();
						//return m_RunCount;
					}
			}

		private int close()
			{
				synchronized(this)
					{
						if(!m_bIsConnected)
							return ErrorCodes.E_NOT_CONNECTED;
						if(m_bDummyTraffic)
							{
								m_DummyTraffic.stop();
								m_DummyTraffic=null;
							}
						JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"JAPMuxSocket:close() Closing MuxSocket...");
						m_bRunFlag=false;
					  try{m_inDataStream.close();}catch(Exception e1){}
						try{threadRunLoop.interrupt();}catch(Exception e7){}
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
                  JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"JAPMuxSocket:close() Hm...MuxSocket is still alive...");
                  threadRunLoop.stop();
                  runStoped();
              }
						threadRunLoop=null;
						JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"JAPMuxSocket:close() MuxSocket closed!");
						return 0;
					}
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
						ChannelListEntry tmpEntry=(ChannelListEntry)oChannelList.get(new Integer(channel));
						if(tmpEntry!=null)
							{
								if(flags==CHANNEL_CLOSE)
									{
										oChannelList.remove(new Integer(channel));
										//JAPAnonService.setNrOfChannels(oSocketList.size());
										tmpEntry.channel.closedByPeer();
                    //try{tmpEntry.outStream.close();}catch(Exception e){}
										//try{tmpEntry.inSocket.close();}catch(Exception e){}
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
                      try
                        {
												  tmpEntry.channel.recv(buff,3,len);
                        }
                      catch(Exception e)
                        {
                          JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"JAPMuxSocket:Fehler bei write to channel..."+e.toString());
                        }
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

		private void runStoped()
			{
				JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"JAPMuxSocket:runStoped()");
				Enumeration e=oChannelList.elements();
				while(e.hasMoreElements())
					{
						ChannelListEntry entry=(ChannelListEntry)e.nextElement();
            entry.channel.closedByPeer();
					}
        oChannelList=null;
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
				JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"JAPMuxSocket:All done..");
			}

		public synchronized int send(int channel,int type,byte[] buff,short len)
			{
				try
					{
 						if(!m_bIsConnected)
							return ErrorCodes.E_NOT_CONNECTED;

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
						ChannelListEntry entry=(ChannelListEntry)oChannelList.get(new Integer(channel));
						if(entry!=null&&entry.arCipher==null)
							{
								int size=PAYLOAD_SIZE-KEY_SIZE;
								entry.arCipher=new SymCipher[chainlen];

								//Last Mix
								entry.arCipher[chainlen-1]=new SymCipher();
								keypool.getKey(outBuff);
								outBuff[0]&=0x7F; //RSA HACK!! (to ensure what m<n in RSA-Encrypt: c=m^e mod n)

								outBuff[KEY_SIZE]=(byte)(len>>8);
								outBuff[KEY_SIZE+1]=(byte)(len%256);
								//if(type==JAPAnonService.PROTO_SOCKS)
									//outBuff[KEY_SIZE+2]=1;
								//else
									outBuff[KEY_SIZE+2]=0;

								System.arraycopy(buff,0,outBuff,KEY_SIZE+3,size);

								entry.arCipher[chainlen-1].setEncryptionKeyAES(outBuff);
								m_arASymCipher[chainlen-1].encrypt(outBuff,0,buff,0);
								entry.arCipher[chainlen-1].encryptAES(outBuff,RSA_SIZE,buff,RSA_SIZE,DATA_SIZE-RSA_SIZE);
								size-=KEY_SIZE;
								for(int i=chainlen-2;i>=0;i--)
									{
										entry.arCipher[i]=new SymCipher();
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
						//JAPAnonService.increaseNrOfBytes(len);
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

	/*	public final int getChainLen()
			{
				return chainlen;
			}*/

		public long getTimeLastPacketSend()
			{
				return m_TimeLastPacketSend;
			}
	}
