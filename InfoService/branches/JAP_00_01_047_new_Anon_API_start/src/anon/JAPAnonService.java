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

import java.io.InterruptedIOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import anon.JAPMuxSocket;
import JAPDebug;

public final class JAPAnonService implements Runnable
	{
		public final static int E_SUCCESS=0;
		public final static int E_RUNNING=-6;
		public final static int E_INVALID_PROTOCOL=-5;
		public final static int E_INVALID_PORT=-7;
		public final static int E_BIND=-8;
		public final static int E_CONNECT=-10;

		public final static int PROTO_HTTP=1;
		public final static int PROTO_SOCKS=2;

		private int m_Port=-1;
		private int m_Protocol=-1;
		private boolean m_bBindToLocalHostOnly=true;
		private volatile boolean m_bIsRunning=false;
		private static String m_AnonHostName=null;
		private static int m_AnonHostPort=-1;

		private String m_FirewallHost = null;
		private int    m_FirewallPort = -1;
		private boolean m_connectViaFirewall = false;
		private boolean m_bUseFirewallAuthentication=false;
		private String m_FirewallAuthenticationUserID=null;
		private String m_FirewallAuthenticationPasswd=null;
		private Thread m_threadRunLoop=null;
		private ServerSocket m_socketListener=null;
		private boolean m_bDontChangeListener=false;
		private JAPMuxSocket m_MuxSocket=null;
		private static JAPAnonServiceListener m_AnonServiceListener=null;

		public JAPAnonService(){}
		public JAPAnonService(int port){this();setPort(port);}
		public JAPAnonService(int port,int protocol)
			{
				this();
				setService(port,protocol);
			}

		public JAPAnonService(int port,int protocol,boolean bLocalHostOnly)
			{
				this();
				setService(port,protocol,bLocalHostOnly);
			}

		public JAPAnonService(ServerSocket listener,int protocol)
			{
				this();
				setService(listener,protocol);
			}

		public int setService(int port,int protocol)
			{
				setPort(port);
				return setProtocol(protocol);
			}

		public int setService(ServerSocket listener,int protocol)
			{
				if(listener==null)
					return -1;
				setPort(listener.getLocalPort());
				m_socketListener=listener;
				m_bDontChangeListener=true;
				return setProtocol(protocol);
			}

		public int setService(int port,int protocol,boolean bLocalHostOnly)
			{
				setPort(port,bLocalHostOnly);
				return setProtocol(protocol);
			}

		public int setPort(int port)
			{
				m_Port=port;
				return E_SUCCESS;
			}

		public int setPort(int port,boolean bLocalHostOnly)
			{
				m_Port=port;
				m_bBindToLocalHostOnly=bLocalHostOnly;
				return E_SUCCESS;
			}

		public int setProtocol(int protocol)
			{
				m_Protocol=protocol;
				return E_SUCCESS;
			}

		public static int setAnonService(String name,int port)
			{
				if(JAPMuxSocket.isConnected())
					return E_CONNECT;
				m_AnonHostName=name;
				m_AnonHostPort=port;
				return E_SUCCESS;
			}

		public int setAnonServiceListener(JAPAnonServiceListener listener)
			{
				m_AnonServiceListener=listener;
				return E_SUCCESS;
			}

		//2001-02-20(HF)
		public int setFirewall(String name,int port) {
		    m_FirewallHost = name;
		    m_FirewallPort = port;
		    return E_SUCCESS;
		}

		public int setFirewallAuthorization(String userid,String passwd)
			{
				m_FirewallAuthenticationUserID=userid;
				m_FirewallAuthenticationPasswd=passwd;
				if(userid!=null)
					m_bUseFirewallAuthentication=true;
				else
					m_bUseFirewallAuthentication=false;
				return E_SUCCESS;
			}

		//2001-02-20(HF)
		public int connectViaFirewall(boolean b) {
		    m_connectViaFirewall = b;
		    return E_SUCCESS;
		}


		public static void setEnableDummyTraffic(boolean b)
			{
				JAPMuxSocket.setEnableDummyTraffic(b);
			}

		public static boolean getEnableDummyTraffic()
			{
				return JAPMuxSocket.getEnableDummyTraffic();
			}

		//Starts long time initalisation procedures in the background. Returns imedaly.
		//You don't need to call these procedure in order to use AnonService
		public static void init()
			{
				JAPKeyPool.start();
			}

		public int start()
			{
				if(m_bIsRunning)
					return E_RUNNING;
				if(m_Port<0||m_Port>0x00FFFF)
					return E_INVALID_PORT;
				if(m_Protocol!=PROTO_HTTP&&m_Protocol!=PROTO_SOCKS)
					return E_INVALID_PROTOCOL;

				if(!m_bDontChangeListener)
					{
						boolean bindOk=false;
						for(int i=0;i<10;i++) //HAck for Mac!!
							try
								{
									if(m_bBindToLocalHostOnly)
										{
											InetAddress[] a=InetAddress.getAllByName("127.0.0.1");
											JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"Try binding Listener on localhost: "+a[0]);
											m_socketListener = new ServerSocket (m_Port,50,a[0]);
										}
									else
										m_socketListener = new ServerSocket (m_Port);
									JAPDebug.out(JAPDebug.INFO,JAPDebug.NET,"JAPProxyServer:Listener on port " + m_Port + " started.");
									bindOk=true;
									break;
								}
							catch(Exception e)
								{
									JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"Exception: "+e.getMessage());
									m_socketListener=null;
								}
						if(!bindOk)
							return E_BIND;
					}
				JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"JAPProxyServer:Mux starting...");
				m_MuxSocket = JAPMuxSocket.create();
				//2001-02-20(HF)
				int connectResult = -1;
				if (m_connectViaFirewall) {
					connectResult = m_MuxSocket.connectViaFirewall(m_AnonHostName,m_AnonHostPort,m_FirewallHost,m_FirewallPort,m_FirewallAuthenticationUserID,m_FirewallAuthenticationPasswd);
				} else {
					connectResult = m_MuxSocket.connect(m_AnonHostName,m_AnonHostPort);
				}
				if(connectResult==-1)
					{
						if(!m_bDontChangeListener)
							{
								try{ m_socketListener.close(); }catch(Exception e){}
								m_socketListener=null;
							}
						m_MuxSocket=null;
						return E_CONNECT;
					}
				JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"JAPProxyServer:Mux connected!");
				m_MuxSocket.startService();
				m_threadRunLoop=new Thread(this);
				m_threadRunLoop.start();
				return E_SUCCESS;
			}

		public int stop()
			{
				try
					{
						JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"JAPAnonService: stopping...");
						m_bIsRunning=false;
						if(!m_bDontChangeListener)
						{try{m_socketListener.close();}catch(Exception e1){};m_socketListener=null;}
						JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"JAPAnonService: wait for joining...");
						m_threadRunLoop.join(10000);
						if(m_threadRunLoop.isAlive())
							{
								JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"JAPAnonService: Still alvie - so stopping harder...");
								try{m_threadRunLoop.interrupt();}catch(Exception e1){};
								m_threadRunLoop.join(3000);
							}
						m_threadRunLoop=null;
					}
				catch(Exception e)
					{
						JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"Exception: "+e.getMessage());
					}
				return E_SUCCESS;
			}

		public void run()
			{
				m_bIsRunning=true;
//				int oldTimeOut=0;
//				try{oldTimeOut=m_socketListener.getSoTimeout();}catch(Exception e){}
//				try{m_socketListener.setSoTimeout(2000);}
//				catch(Exception e1){JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"Could not set accept time out: Exception: "+e1.getMessage());}
//				try{System.out.println(m_socketListener.getSoTimeout());}catch(Exception e5){e5.printStackTrace();}
				try
					{
						while(m_bIsRunning)
							{
								Socket socket=null;
								try
									{
										socket = m_socketListener.accept();
									}
								catch(InterruptedIOException e)
									{
										continue;
									}
								try
									{
										socket.setSoTimeout(0); //ensure that socket is blocking!
									}
								catch(SocketException soex)
									{
										socket=null;
										JAPDebug.out(JAPDebug.ERR,JAPDebug.NET,"JAPAnonService.run() Could not set non-Blocking mode for Channel-Socket! Exception: " +soex);
										continue;
									}
								//2001-04-04(HF)
								try
									{
										m_MuxSocket.newConnection(new JAPSocket(socket),m_Protocol);
									}
								catch(Exception e)
									{
										JAPDebug.out(JAPDebug.ERR,JAPDebug.NET,"JAPAnonService.run() Exception: " +e);
									}
							}
					}
				catch (Exception e)
					{
						JAPDebug.out(JAPDebug.ERR,JAPDebug.NET,"JAPProxyServer:ProxyServer.run1() Exception: " +e);
					}
	//			try{m_socketListener.setSoTimeout(oldTimeOut);}catch(Exception e4){}
				JAPDebug.out(JAPDebug.INFO,JAPDebug.NET,"JAPProxyServer:ProxyServer on port " + m_Port + " stopped.");
				m_bIsRunning=false;
				m_MuxSocket.stopService();
				m_MuxSocket=null;
				if(!m_bDontChangeListener)
					{
						try
							{
								m_socketListener.close();
							}
						catch (Exception e2) {}
						m_socketListener=null;
					}
			}

		protected static void setNrOfChannels(int channels)
			{
				JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"Notify channel listeners");
				if(m_AnonServiceListener!=null)
					m_AnonServiceListener.channelsChanged(channels);
				JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"End Notify channel listeners");
			}

		protected static void increaseNrOfBytes(int bytes)
			{
				//JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"Notify bytes listeners");
				if(m_AnonServiceListener!=null)
					m_AnonServiceListener.transferedBytes(bytes);
				//JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"End Notify bytes listeners");
			}


	}
