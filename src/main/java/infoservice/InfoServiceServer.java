package infoservice;

import java.net.Socket;
import logging.LogHolder;
import logging.LogLevel;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.InetAddress;
import anon.infoservice.ListenerInterface;
import anon.util.SocketGuard;
import logging.LogType;

final class InfoServiceServer implements Runnable
{
	private static final int GUARD_TIMEOUT_IN_MS = 120000;
	
	private ListenerInterface m_Listener;
	private InfoService m_IS;

	public InfoServiceServer(ListenerInterface listener,InfoService is)
	{
		m_Listener = listener;
		m_IS=is;
	}

	public void run()
	{
		String strInterface = m_Listener.getHost();
		LogHolder.log(LogLevel.ALERT, LogType.ALL,
					  "Server on interface: " + strInterface + " on port: " + m_Listener.getPort() +
					  " starting...");
		ServerSocket server = null;
		SocketGuard socket = null;
		try
		{
			server = new ServerSocket(m_Listener.getPort(), 200, InetAddress.getByName(m_Listener.getHost()));
			LogHolder.log(LogLevel.INFO, LogType.NET, "ServerSocket is listening!");
			while (true)
			{
				try
				{
					socket = null;
					socket = new SocketGuard(server.accept(), GUARD_TIMEOUT_IN_MS);
					//socket = server.accept();
				}
				catch (IOException ioe)
				{
					LogHolder.log(LogLevel.EXCEPTION, LogType.NET, "Accept-Exception: " + ioe);
					if (socket != null)
					{
						try
						{
							socket.close();
						}
						catch (Exception ie)
						{
						}
					}
					//Hack!!! [i do not know how to get otherwise notice of "Too many open Files"]
					if (ioe.getMessage().equalsIgnoreCase("Too many open files"))
					{
						try
						{
							Thread.sleep(1000);
						}
						catch (Exception e)
						{
						}
					}
					continue;
				}
				ISRuntimeStatistics.ms_lTCPIPConnections++;
				try
				{
					InfoServiceConnection doIt = new InfoServiceConnection(socket,
						InfoService.getConnectionCounter(),
						m_IS.oicHandler);
					m_IS.m_ThreadPool.addRequest(doIt);
					doIt = null;
				}
				catch (Exception e2)
				{
					if (socket != null)
					{
						try
						{
							socket.close();
						}
						catch (Exception ie)
						{
						}
						socket = null;
					}
					LogHolder.log(LogLevel.EXCEPTION, LogType.MISC, "Run-Loop-Exception: " + e2);
				}
			}
		}
		catch (Throwable t)
		{
			LogHolder.log(LogLevel.ALERT, LogType.MISC,
						  "Unexcpected Exception in Run-Loop (exiting): " + t);
			try
			{
				if (socket != null)
				{
					socket.close();
				}
				server.close();
			}
			catch (Exception e2)
			{
			}
			LogHolder.log(LogLevel.ERR, LogType.NET, "JWS Exception: " + t);
		}
		LogHolder.log(LogLevel.ALERT, LogType.NET,
					  "Server on interface: " + strInterface + " on port: " + m_Listener.getPort());
		LogHolder.log(LogLevel.EMERG, LogType.NET, "Exiting because of fatal Error!");
	}

	public String toString()
	{
		String s;
		s = "InfoService Server on Interface: ";
		if (m_Listener == null)
		{
			s += "unknown";
		}
		else
		{
			s += m_Listener.getHost() + ":" + m_Listener.getPort();
		}
		return s;
	}

	}
