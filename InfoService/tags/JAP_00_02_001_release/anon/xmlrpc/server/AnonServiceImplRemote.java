package anon.xmlrpc.server;

import java.util.Vector;
import java.io.OutputStream;
import java.io.InputStream;
import org.apache.xmlrpc.WebServer;
import org.apache.xmlrpc.XmlRpcHandler;

import anon.AnonService;
import anon.AnonChannel;
import anon.server.impl.AbstractChannel;

public class AnonServiceImplRemote implements XmlRpcHandler
{
	private AnonService m_AnonService;
	private WebServer m_RpcServer;
	private ClientList m_ClientList;
	public AnonServiceImplRemote(AnonService anonService)
		{
			m_AnonService=anonService;
			m_ClientList=new ClientList();
		}

	public int startService()
		{
			try
				{
					m_RpcServer = new WebServer (8889);
					m_RpcServer.addHandler ("ANONXMLRPC", this);
					m_RpcServer.start();
					return 0;
				}
			catch(Exception e)
				{
					e.printStackTrace();
					return -1;
				}
		}

	public int stopService()
		{
			return 0;
		}

	public Object execute(String method,Vector params) throws Exception
		{
			if(method.equals("registerClient"))
				return doRegisterClient(params);
			else if(method.equals("createChannel"))
				return doCreateChannel(params);
			else if(method.equals("channelInputStreamRead"))
				return doChannelInputStreamRead(params);
			else if(method.equals("channelOutputStreamWrite"))
				return doChannelOutputStreamWrite(params);
			throw new Exception("Unknown Method");
		}

	private Object doRegisterClient(Vector params) throws Exception
		{
			int id=m_ClientList.addNewClient();
			return new Integer(id);
		}

	private Object doCreateChannel(Vector params) throws Exception
		{
			Integer i=(Integer)params.elementAt(0);
			ClientEntry c=m_ClientList.getClient(i);
			AnonChannel channel;
			channel=m_AnonService.createChannel(AnonChannel.HTTP);
			c.addChannel(channel);
			return new Integer(((AbstractChannel)channel).hashCode());
		}

	private Object doChannelInputStreamRead(Vector params) throws Exception
		{
			Integer i=(Integer)params.elementAt(0);
			ClientEntry c=m_ClientList.getClient(i);
			AnonChannel channel=c.getChannel((Integer)params.elementAt(1));
			InputStream in=channel.getInputStream();
			int len=((Integer)params.elementAt(2)).intValue();
			byte[] buff=new byte[len];
			int retlen=in.read(buff);
			if(retlen<0)
				return new Integer(-1);
			byte[] outbuff=new byte[retlen];
			System.arraycopy(buff,0,outbuff,0,retlen);
			return outbuff;
		}

	private Object doChannelOutputStreamWrite(Vector params) throws Exception
		{
			Integer i=(Integer)params.elementAt(0);
			ClientEntry c=m_ClientList.getClient(i);
			AnonChannel channel=c.getChannel((Integer)params.elementAt(1));
			OutputStream out=channel.getOutputStream();
			byte[] buff=(byte[])params.elementAt(2);
			out.write(buff);
			out.flush();
			return new Integer(0);
		}

}