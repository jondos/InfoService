package anon.xmlrpc.client;
import anon.AnonChannel;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;

class ChannelProxy implements AnonChannel
{
		private boolean m_bIsClosedByPeer=false;
		private boolean m_bIsClosed=false;
		protected int m_id;
		protected int m_type;
		protected AnonServiceImplProxy m_RemoteAnonService;


		private ChannelInputStreamProxy m_inputStream;
		private ChannelOutputStreamProxy m_outputStream;

		public ChannelProxy(int id,AnonServiceImplProxy remote) throws IOException
			{
				m_bIsClosedByPeer=false;
				m_bIsClosed=false;
				m_id=id;
				m_RemoteAnonService=remote;
				m_inputStream=new ChannelInputStreamProxy(this);
				m_outputStream=new ChannelOutputStreamProxy(this);
			}

		public void finalize()
			{
				close();
			}

		public int hashCode()
			{
				return m_id;
			}

		public InputStream getInputStream()
			{
				return m_inputStream;
			}

		 public OutputStream getOutputStream()
			{
				return m_outputStream;
			}

		//called from ChannelOutputStream to send data to the AnonService which belongs to this channel
		protected /*synchronized*/ void send(byte[] buff,int len) throws IOException
			{
				m_RemoteAnonService.send(m_id,buff,len);
			}

		protected int recv(byte[] buff,int off,int len) throws IOException
			{
				return m_RemoteAnonService.recv(m_id,buff,off,len);
			}

		public synchronized void close()
			{
/*        try
					{
						if(!m_bIsClosed)
							{
								m_outputStream.close();
								m_inputStream.close();
								if(!m_bIsClosedByPeer)
									close_impl();
							}
					}
				catch(Exception e)
					{
					}
				m_bIsClosed=true;*/
			}


/*

		protected  void closedByPeer()
			{
				try
					{
						m_inputStream.closedByPeer();
						m_outputStream.closedByPeer();
					}
				catch(Exception e)
					{
					}
				m_bIsClosedByPeer=true;
			}
*/


	}
