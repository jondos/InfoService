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
			if(m_channel.recv(buff,0,1)<0)
				return -1;
			return (int)buff[0]&0xFF;
		}

	public int read(byte[] buff,int off,int len) throws IOException
	 {
		 return m_channel.recv(buff,off,len);
	 }


}