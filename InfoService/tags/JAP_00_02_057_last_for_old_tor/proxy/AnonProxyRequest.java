package proxy;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import anon.AnonChannel;
import anon.AnonService;
import anon.AnonServiceFactory;
import anon.ErrorCodes;
import anon.NotConnectedToMixException;
import anon.ToManyOpenChannelsException;
import anon.crypto.JAPCertificateStore;
import anon.infoservice.MixCascade;
import anon.pay.AICommunication;
import anon.server.AnonServiceImpl;
import jap.JAPConstants;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import pay.Pay;

final class AnonProxyRequest implements Runnable
{
	InputStream m_InChannel;
	OutputStream m_OutChannel;
	InputStream m_InSocket;
	OutputStream m_OutSocket;
	Socket m_clientSocket;
	Thread m_threadResponse;
	Thread m_threadRequest;
	AnonChannel m_Channel;
	IAnonProxy m_Proxy;
	volatile boolean m_bRequestIsAlive;

	AnonProxyRequest(IAnonProxy proxy,Socket clientSocket, AnonChannel c)
	{
		try
		{
			m_Proxy=proxy;
			m_clientSocket = clientSocket;
			m_clientSocket.setSoTimeout(1000); //just to ensure that threads will stop
			m_InSocket = clientSocket.getInputStream();
			m_OutSocket = clientSocket.getOutputStream();
			m_InChannel = c.getInputStream();
			m_OutChannel = c.getOutputStream();
			m_Channel = c;
			m_threadRequest = new Thread(this, "JAP - AnonProxy Request");
			m_threadResponse = new Thread(new Response(), "JAP - AnonProxy Response");
			m_threadResponse.start();
			m_threadRequest.start();
		}
		catch (Exception e)
		{
		}
	}

	public void run()
	{
		m_bRequestIsAlive = true;
		m_Proxy.incNumChannels();
		int len = 0;
		byte[] buff = new byte[1900];
		try
		{
			for (; ; )
			{
				try
				{
					len=Math.min(m_Channel.getOutputBlockSize(),1900);
					len = m_InSocket.read(buff, 0,len);
				}
				catch (InterruptedIOException ioe)
				{
					continue;
				}
				if (len <= 0)
				{
					break;
				}
				m_OutChannel.write(buff, 0, len);
				m_Proxy.transferredBytes(len);
			}
		}
		catch (Exception e)
		{
		}
		m_bRequestIsAlive = false;
		try
		{
			m_Channel.close();
		}
		catch (Exception e)
		{}
		m_Proxy.decNumChannels();
	}

	final class Response implements Runnable
	{
		Response()
		{
		}

		public void run()
		{
			int len = 0;
			byte[] buff = new byte[2900];
			try
			{
				while ( (len = m_InChannel.read(buff, 0, 1000)) > 0)
				{
					int count = 0;
					for (; ; )
					{
						try
						{
							m_OutSocket.write(buff, 0, len);
							m_OutSocket.flush();
							break;
						}
						catch (InterruptedIOException ioe)
						{
							LogHolder.log(LogLevel.EMERG, LogType.NET,
										  "Should never be here: Timeout in sending to Browser!");
						}
						count++;
						if (count > 3)
						{
							throw new Exception("Could not send to Browser...");
						}
					}
					m_Proxy.transferredBytes(len);
				}
			}
			catch (Exception e)
			{}
			try
			{
				m_clientSocket.close();
			}
			catch (Exception e)
			{}
			try
			{
				Thread.sleep(500);
			}
			catch (Exception e)
			{}
			if (m_bRequestIsAlive)
			{
				try
				{
					m_threadRequest.interrupt();
				}
				catch (Exception e)
				{}
			}
		}

	}
}
