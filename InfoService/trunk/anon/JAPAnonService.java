package anon;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import anon.JAPMuxSocket;

public class JAPAnonService implements Runnable
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
		private boolean m_bIsRunning=false;
		private String m_AnonHostName=null;
		private int m_AnonHostPort=-1;
		private Thread m_threadRunLoop=null;
		private ServerSocket socketListener=null;
		private JAPMuxSocket m_MuxSocket=null;
		
		private JAPAnonServiceListener m_AnonServiceListener=null;

		private static JAPKeyPool m_KeyPool=null;
		public JAPAnonService(){if(m_KeyPool==null)
														{
															m_KeyPool=new JAPKeyPool(20,16);
															Thread t1 = new Thread (m_KeyPool);
															t1.setPriority(Thread.MIN_PRIORITY);
															t1.start();
														}}
		public JAPAnonService(int port){this();setPort(port);}
		public JAPAnonService(int port,int protocol)
			{
				this();
				setService(port,protocol);
			}
		public int setService(int port,int protocol)
			{
				setPort(port);
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
		
		public int setAnonService(String name,int port)
			{
				m_AnonHostName=name;
				m_AnonHostPort=port;
				return E_SUCCESS;
			}
		
		public int setAnonServiceListener(JAPAnonServiceListener listener)
			{
				m_AnonServiceListener=listener;
				return E_SUCCESS;
			}
		
		public int start()
			{
				if(m_bIsRunning)
					return E_RUNNING;
				if(m_Port<0||m_Port>0x00FFFF)
					return E_INVALID_PORT;
				if(m_Protocol!=PROTO_HTTP&&m_Protocol!=PROTO_SOCKS)
					return E_INVALID_PROTOCOL;
				
				socketListener = null;
				try 
					{
						if(m_bBindToLocalHostOnly)
							{
								InetAddress[] a=InetAddress.getAllByName("localhost");
								JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"Try binding Listener on localhost: "+a[0]);
								socketListener = new ServerSocket (m_Port,50,a[0]);
							}
						else
							socketListener = new ServerSocket (m_Port,50);
						JAPDebug.out(JAPDebug.INFO,JAPDebug.NET,"JAPProxyServer:Listener on port " + m_Port + " started.");
					}
				catch(Exception e)
					{
						socketListener=null;
						return E_BIND;
					}
				
				JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"JAPProxyServer:Mux starting...");
				m_MuxSocket = new JAPMuxSocket(this);
				if(m_MuxSocket.connect(m_AnonHostName,m_AnonHostPort)==-1)
					{
						try{ socketListener.close(); }catch(Exception e){}
						socketListener=null;
						m_MuxSocket=null;
						return E_CONNECT;
					}
				JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"JAPProxyServer:Mux connected!");
				m_MuxSocket.start();
				m_threadRunLoop=new Thread(this);
				m_threadRunLoop.start();
				return E_SUCCESS;
			}
		
		public int stop()
			{
				try
					{
						m_bIsRunning=false;
						m_threadRunLoop.interrupt();
						m_threadRunLoop.join();
						m_threadRunLoop=null;
						m_MuxSocket.close();
						m_MuxSocket=null;
						socketListener.close();
						socketListener=null;
					}
				catch(Exception e){}
				return E_SUCCESS;
			}
		
		public void run()
			{
				m_bIsRunning=true;
				
				try 
					{
						while(m_bIsRunning)
							{
								Socket socket = socketListener.accept();
								m_MuxSocket.newConnection(new JAPSocket(socket));
							}
					}
				catch (Exception e)
					{
						try
							{
								socketListener.close();
							} 
						catch (Exception e2) {}
						JAPDebug.out(JAPDebug.ERR,JAPDebug.NET,"JAPProxyServer:ProxyServer.run() Exception: " +e);
					}
				JAPDebug.out(JAPDebug.INFO,JAPDebug.NET,"JAPProxyServer:ProxyServer on port " + m_Port + " stopped.");
				m_bIsRunning=false;
			}
		
		protected void setNrOfChannels(int channels)
			{
				if(m_AnonServiceListener!=null)
					m_AnonServiceListener.channelsChanged(channels);
			}
		
		protected void increaseNrOfBytes(int bytes)
			{
				if(m_AnonServiceListener!=null)
					m_AnonServiceListener.transferedBytes(bytes);
			}
		
		protected JAPKeyPool getKeyPool()
			{
				return m_KeyPool;
			}
	}
