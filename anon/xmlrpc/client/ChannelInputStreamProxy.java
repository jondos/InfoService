package anon.xmlrpc.client;

import java.io.InputStream;
import java.io.IOException;
class ChannelInputStreamProxy extends InputStream
{
  ChannelProxy m_channel;
  public ChannelInputStreamProxy(ChannelProxy proxychannel)
    {
      m_channel=proxychannel;
    }

   public int read() throws IOException
    {
      byte[] buff=new byte[1];
      m_channel.recv(buff,1);
      return (int)buff[0];
    }

}