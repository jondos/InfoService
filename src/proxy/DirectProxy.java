/*
 Copyright (c) 2000, The JAP-Team
 All rights reserved.
 Redistribution and use in source and binary forms, with or without modification,
 are permitted provided that the following conditions are met:

 - Redistributions of source code must retain the above copyright notice,
  this list of conditions and the following disclaimer.

 - Redistributions in binary form must reproduce the above copyright notice,
  this list of conditions and the following disclaimer in the documentation and/or
  other materials provided with the distribution.

 - Neither the name of the University of Technology Dresden, Germany nor the names of its contributors
  may be used to endorse or promote products derived from this software without specific
  prior written permission.


 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS ``AS IS'' AND ANY EXPRESS
 OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS
 BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE
 */
package proxy;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import jap.JAPConstants;
import jap.JAPController;
import jap.JAPMessages;
import jap.JAPModel;
import jap.JAPUtil;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

final public class DirectProxy implements Runnable
{
	private volatile boolean runFlag;
	private boolean isRunningProxy = false;
	private int portN;
	private ServerSocket m_socketListener;
	private volatile Thread threadRunLoop;
	private ThreadGroup threadgroupAll;
	private boolean warnUser = true;

	public DirectProxy(ServerSocket s)
	{
		m_socketListener = s;
		warnUser = true;
		isRunningProxy = false;
	}

	public synchronized boolean startService()
	{
		if (m_socketListener == null)
		{
			return false;
		}
		threadgroupAll = new ThreadGroup("directproxy");
		threadRunLoop = new Thread(this, "JAP - Direct Proxy");
		threadRunLoop.setDaemon(true);
		runFlag = true;
		threadRunLoop.start();
		isRunningProxy = true;
		return true;
	}

	public void run()
	{
		try
		{
			while (runFlag)
			{
				Socket socket = null;
				try
				{
					socket = m_socketListener.accept();
				}
				catch (InterruptedIOException e1)
				{
					continue;
				}
				catch (SocketException e2)
				{
					LogHolder.log(LogLevel.ERR, LogType.NET,
								  "JAPDirectProxy:DirectProxy.run() accept socket excpetion: " + e2);
					break;
				}
				try
				{
					socket.setSoTimeout(0); //Ensure socket is in Blocking Mode
				}
				catch (SocketException soex)
				{
					LogHolder.log(LogLevel.ERR, LogType.NET,
						"JAPDirectProxy:DirectProxy.run() Colud not set sockt to blocking mode! Excpetion: " +
								  soex);
					socket = null;
					continue;
				}

				if (warnUser && !JAPModel.isSmallDisplay())
				{
					SendAnonWarning doIt = new SendAnonWarning(socket);
					Thread thread = new Thread(threadgroupAll, doIt);
					thread.start();
					warnUser = false;
				}
				else
				{
					if (JAPModel.getUseFirewall() &&
						JAPModel.getFirewallType() == JAPConstants.FIREWALL_TYPE_HTTP)
					{
						DirectConViaHTTPProxy doIt = new DirectConViaHTTPProxy(socket);
						Thread thread = new Thread(threadgroupAll, doIt);
						thread.start();
					}
					else
					{
						DirectProxyConnection doIt = new DirectProxyConnection(socket);
						Thread thread = new Thread(threadgroupAll, doIt);
						thread.start();
					}
				}
			}
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.ERR, LogType.NET, "JAPDirectProxy:DirectProxy.run() Exception: " + e);
		}
		isRunningProxy = false;
		LogHolder.log(LogLevel.INFO, LogType.NET, "JAPDirect:DircetProxyServer stopped.");
	}

	public synchronized void stopService()
	{
		runFlag = false;
		try
		{
			int timeToWait = 0;
			try
			{
				timeToWait = m_socketListener.getSoTimeout();
			}
			catch (Exception ex)
			{}
			if (timeToWait <= 0)
			{
				timeToWait = 3000;
			}
			timeToWait += 1000;
			threadRunLoop.join(timeToWait);
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.ERR, LogType.NET, "JAPDirect:DirectProxyServer could not be stopped!!!");
		}
		if (threadgroupAll != null)
		{
			try //Hack for kaffe!
			{
				threadgroupAll.stop();
				threadgroupAll.destroy();
			}
			catch (Exception e)
			{
			}
		}
		threadgroupAll = null;
		threadRunLoop = null;
	}

	//	try {
	//		socketListener.close();
	//	}
	//	catch(Exception e) {
	//		LogHolder.log(LogLevel.EXCEPTION,LogLevel.NET,"JAPProxyServer:stopService() Exception: " +e);
	//	}
	//}


	/**
	 *  This class is used to inform the user that he tries to
	 *  send requests although anonymity mode is off.
	 */
	private final class SendAnonWarning implements Runnable
	{
		private Socket s;
		private SimpleDateFormat dateFormatHTTP;

		public SendAnonWarning(Socket s)
		{
			this.s = s;
			dateFormatHTTP = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
			dateFormatHTTP.setTimeZone(TimeZone.getTimeZone("GMT"));
		}

		public void run()
		{
			try
			{
				String date = dateFormatHTTP.format(new Date());
				BufferedWriter toClient = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
				toClient.write("HTTP/1.1 200 OK\r\n");
				toClient.write("Content-type: text/html\r\n");
				toClient.write("Expires: " + date + "\r\n");
				toClient.write("Date: " + date + "\r\n");
				toClient.write("Pragma: no-cache\r\n");
				toClient.write("Cache-Control: no-cache\r\n\r\n");
				toClient.write("<HTML><TITLE>JAP</TITLE>\n");
				toClient.write("<PRE>" + date + "</PRE>\n");
				toClient.write(JAPMessages.getString("htmlAnonModeOff"));
				toClient.write("</HTML>\n");
				toClient.flush();
				toClient.close();
				s.close();
			}
			catch (Exception e)
			{
				LogHolder.log(LogLevel.EXCEPTION, LogType.NET, "JAPFeedbackConnection: Exception: " + e);
			}
		}
	}

	/**
	 *  This class is used to transfer requests via the selected proxy
	 */
	private final class DirectConViaHTTPProxy implements Runnable
	{
		private Socket m_clientSocket;

		public DirectConViaHTTPProxy(Socket s)
		{
			m_clientSocket = s;
		}

		public void run()
		{
			try
			{
				// open stream from client
				InputStream inputStream = m_clientSocket.getInputStream();
				// create Socket to Server
				Socket serverSocket = new Socket(JAPModel.getFirewallHost(), JAPModel.getFirewallPort());
				// Response from server is transfered to client in a sepatate thread
				DirectProxyResponse pr = new DirectProxyResponse(serverSocket.getInputStream(),
					m_clientSocket.getOutputStream());
				Thread prt = new Thread(pr, "JAP - DirectProxyResponse");
				prt.start();
				// create stream --> server
				OutputStream outputStream = serverSocket.getOutputStream();

				// Transfer data client --> server
				//first check if the us authorization for the proxy
				if (JAPModel.getUseFirewallAuthorization())
				{ //we need to insert an authorization line...
					//read first line and after this insert the authorization
					String str = JAPUtil.readLine(inputStream);
					str += "\r\n";
					outputStream.write(str.getBytes());
					str = JAPUtil.getProxyAuthorization(JAPModel.getFirewallAuthUserID(),
						JAPController.getFirewallAuthPasswd());
					outputStream.write(str.getBytes());
					outputStream.flush();
				}
				byte[] buff = new byte[1000];
				int len;
				while ( (len = inputStream.read(buff)) != -1)
				{
					if (len > 0)
					{
						outputStream.write(buff, 0, len);
						outputStream.flush();
					}
				}
				prt.join();
				outputStream.close();
				inputStream.close();
				serverSocket.close();
			}
			catch (IOException ioe)
			{
			}
			catch (Exception e)
			{
				LogHolder.log(LogLevel.EXCEPTION, LogType.NET, "JAPDirectConViaProxy: Exception: " + e);
			}
		}
	}
}
