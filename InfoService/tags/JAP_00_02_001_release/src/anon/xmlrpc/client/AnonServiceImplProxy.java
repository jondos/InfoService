package anon.xmlrpc.client;

import org.apache.xmlrpc.XmlRpcClientLite;

import java.io.IOException;
import java.util.Vector;
import java.net.InetAddress;
import java.net.ConnectException;
import anon.AnonService;
import anon.AnonChannel;
import anon.AnonServer;
import anon.AnonServiceEventListener;
import logging.Log;

public class AnonServiceImplProxy implements AnonService
	{
		InetAddress m_RpcServerHost;
		int m_RpcServerPort;
		int m_ClientID;

		public AnonServiceImplProxy(InetAddress addr,int port) throws Exception
			{
				m_RpcServerHost=addr;
				m_RpcServerPort=port;
				Object o=doRemote("registerClient",new Vector());
				m_ClientID=((Integer)o).intValue();
			}

		public int connect(AnonServer anonService)
			{
				return 0;
			}

		public void disconnect()
			{
			}

		public AnonChannel createChannel(int type) throws ConnectException
			{
				try
					{
						Vector v=new Vector(1);
						v.addElement(new Integer(m_ClientID));
						Object o=doRemote("createChannel",v);
						return new ChannelProxy(((Integer)o).intValue(),this);
					}
				catch(Exception e)
					{
						throw new ConnectException("Could not connect");
					}
			}

		public AnonChannel createChannel(InetAddress host,int port) throws ConnectException
			{
				return null;
			}

		public void addEventListener(AnonServiceEventListener l)
			{
			}

		public void removeEventListener(AnonServiceEventListener l)
			{
			}

		public void setLogging(Log log)
			{
			}

//Implementation
		private Object doRemote(String method,Vector params)throws IOException
			{
				try
					{
						XmlRpcClientLite xmlrpc = new XmlRpcClientLite("http://localhost:8889/RPC2");//m_RpcServerHost.getHostAddress(),m_RpcServerPort);
						return xmlrpc.execute ("ANONXMLRPC."+method, params);
					}
				catch(Exception e)
					{
						e.printStackTrace();
						throw new IOException ("Error processing XML-RCP: "+method);
					}
			}

		protected void send(int channelid,byte[]buff,int off,int len) throws IOException
			{
				Vector v=new Vector();
				v.addElement(new Integer(m_ClientID));
				v.addElement(new Integer(channelid));
				byte[] tmpBuff=new byte[len];
				System.arraycopy(buff,off,tmpBuff,0,len);
				v.addElement(tmpBuff);
				Object o=doRemote("channelOutputStreamWrite",v);
			}

		protected int recv(int channelid,byte[]buff,int off,int len) throws IOException
			{
				Vector v=new Vector();
				v.addElement(new Integer(m_ClientID));
				v.addElement(new Integer(channelid));
				v.addElement(new Integer(len));
				Object o=doRemote("channelInputStreamRead",v);
				if(o instanceof byte[])
					{
						len=((byte[])o).length;
						System.arraycopy(o,0,buff,off,len);
						return len;
					}
				return -1;
			}
	}