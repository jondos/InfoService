package anon.xmlrpc.client;

import java.io.OutputStream;
import java.io.IOException;
class ChannelOutputStreamProxy extends OutputStream
{
	private ChannelProxy m_channel;
	private boolean m_bIsClosed=false;
	public ChannelOutputStreamProxy(ChannelProxy proxychannel)
		{
			m_channel=proxychannel;
			m_bIsClosed=false;
		}

	public void write(byte[] buff) throws IOException
		{
			write(buff,0,buff.length);
		}

	public void write(byte[] buff,int off,int len) throws IOException
		{
			if(/*m_bIsClosedByPeer||*/m_bIsClosed)
				throw new IOException("Channel closed by peer");
			m_channel.send(buff,off,len);
		}

	public void write(int i) throws IOException
		{
			if(/*m_bIsClosedByPeer||*/m_bIsClosed)
				throw new IOException("Channel closed by peer");
			byte[] buff=new byte[1];
			buff[0]=(byte)i;
			m_channel.send(buff,0,1);
		}

}