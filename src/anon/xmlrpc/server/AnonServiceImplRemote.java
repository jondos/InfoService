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
  private AnonChannel m_AnonChannel;
  public AnonServiceImplRemote(AnonService anonService)
    {
      m_AnonService=anonService;
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
      if(method.equals("createChannel"))
        return doCreateChannel(params);
      else if(method.equals("channelInputStreamRead"))
        return doChannelInputStreamRead(params);
      else if(method.equals("channelOutputStreamWrite"))
        return doChannelOutputStreamWrite(params);
      throw new Exception("Unknown Method");
    }

  private Object doCreateChannel(Vector params) throws Exception
    {
      m_AnonChannel=m_AnonService.createChannel(AnonChannel.HTTP);
      return new Integer(((AbstractChannel)m_AnonChannel).hashCode());
    }

  private Object doChannelInputStreamRead(Vector params) throws Exception
    {
      InputStream in=m_AnonChannel.getInputStream();
      int id=((Integer)params.elementAt(0)).intValue();
      int len=((Integer)params.elementAt(1)).intValue();
      byte[] buff=new byte[len];
      int retlen=in.read(buff);
      if(retlen<0)
        return new byte[0];
      byte[] outbuff=new byte[retlen];
      System.arraycopy(buff,0,outbuff,0,retlen);
      return outbuff;
    }

  private Object doChannelOutputStreamWrite(Vector params) throws Exception
    {
      OutputStream out=m_AnonChannel.getOutputStream();
      out.write("GET HTTP://www.bild.de/ HTTP/1.0\n\n".getBytes());
      out.flush();
      return new Integer(0);
    }

}