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

import anon.infoservice.ProxyInterface;
import gui.JAPMessages;
import jap.JAPModel;
import jap.JAPUtil;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;



final public class DirectProxy implements Runnable
{
	private static final int REMEMBER_NOTHING = 0;
	private static final int REMEMBER_WARNING = 1;
	private static final int REMEMBER_NO_WARNING = 2;
	private static final int TEMPORARY_REMEMBER_TIME = 5000;

	private static AllowUnprotectedConnectionCallback ms_callback;

	private ServerSocket m_socketListener;
	private final Object THREAD_SYNC = new Object();
	private volatile Thread threadRunLoop;

	public DirectProxy(ServerSocket s)
	{
		m_socketListener = s;
	}

	public static void setAllowUnprotectedConnectionCallback(AllowUnprotectedConnectionCallback a_callback)
	{
		ms_callback = a_callback;
	}

	public static abstract class AllowUnprotectedConnectionCallback
	{
		public static class Answer
		{
			private boolean m_bRemembered;
			private boolean m_bAllow;

			public Answer(boolean a_bAllow, boolean a_bRemembered)
			{
				m_bAllow = a_bAllow;
				m_bRemembered = a_bRemembered;
			}
			public boolean isRemembered()
			{
				return m_bRemembered;
			}
			public boolean isAllowed()
			{
				return m_bAllow;
			}
		}

		public abstract Answer callback();
}


	public synchronized boolean startService()
	{
		if (m_socketListener == null)
		{
			return false;
		}
		synchronized (THREAD_SYNC)
		{
			//stopService();
			//threadRunLoop.join();
			threadRunLoop = new Thread(this, "JAP - Direct Proxy");
			threadRunLoop.setDaemon(true);
			threadRunLoop.start();
			return true;
		}
	}

	public void run()
	{
		int remember = REMEMBER_NOTHING;
		boolean bShowHtmlWarning = true;
		Runnable doIt;
		long rememberTime = 0;

		try
		{
			m_socketListener.setSoTimeout(2000);
		}
		catch (Exception e1)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.NET, "Could not set accept time out!" , e1);
		}

		while (!Thread.currentThread().isInterrupted())
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
				LogHolder.log(LogLevel.ERR, LogType.NET, "Accept socket excpetion: " + e2);
				break;
			}
			catch (IOException a_e)
			{
				LogHolder.log(LogLevel.EXCEPTION, LogType.NET, "Socket could not accept!" + a_e);
				break;
			}

			try
			{
				socket.setSoTimeout(0); //Ensure socket is in Blocking Mode
			}
			catch (SocketException soex)
			{
				LogHolder.log(LogLevel.ERR, LogType.NET,
							  "Could not set socket to blocking mode! Excpetion: " + soex);
				socket = null;
				continue;
			}

			if (remember == REMEMBER_NOTHING && !JAPModel.isSmallDisplay() &&
				rememberTime < System.currentTimeMillis())
			{
				AllowUnprotectedConnectionCallback.Answer answer;
				AllowUnprotectedConnectionCallback callback = ms_callback;
				if (callback != null)
				{
					answer = callback.callback();
				}
				else
				{
					answer = new AllowUnprotectedConnectionCallback.Answer(false, false);
				}
				bShowHtmlWarning = !answer.isAllowed();

				if (answer.isRemembered())
				{
					if (bShowHtmlWarning)
					{
						remember = REMEMBER_WARNING;
					}
					else
					{
						remember = REMEMBER_NO_WARNING;
					}
				}
				else
				{
					rememberTime = System.currentTimeMillis() + TEMPORARY_REMEMBER_TIME;
				}
			}

			if (!bShowHtmlWarning && !JAPModel.isSmallDisplay())
			{
				if (JAPModel.getInstance().getProxyInterface() != null &&
					JAPModel.getInstance().getProxyInterface().isValid() &&
					JAPModel.getInstance().getProxyInterface().getProtocol() ==
					ProxyInterface.PROTOCOL_TYPE_HTTP)
				{
					doIt = new DirectConViaHTTPProxy(socket);
				}
				else
				{
					doIt = new DirectProxyConnection(socket);
				}
				Thread thread = new Thread(doIt);
				thread.start();
			}
			else
			{
				Thread thread = new Thread(new SendAnonWarning(socket));
				thread.start();
			}

		}
		LogHolder.log(LogLevel.INFO, LogType.NET, "Direct Proxy Server stopped.");

	}

	public synchronized void stopService()
	{
		synchronized (THREAD_SYNC)
		{
			if (threadRunLoop == null)
			{
				return;
			}

			while (threadRunLoop.isAlive())
			{
				threadRunLoop.interrupt();
				try
				{
					threadRunLoop.join(1000);
				}
				catch (InterruptedException e)
				{
					//LogHolder.log(LogLevel.ERR, LogType.NET, "Direct Proxy Server could not be stopped!!!");
				}
			}
			threadRunLoop = null;
		}
	}

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
				// read something so that the browser realises everything is OK
				s.getInputStream().read();
			}
			catch (IOException a_e)
			{
				// ignored
			}


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
				Socket serverSocket = new Socket(JAPModel.getInstance().getProxyInterface().getHost(),
												 JAPModel.getInstance().getProxyInterface().getPort());
				// Response from server is transfered to client in a sepatate thread
				DirectProxyResponse pr = new DirectProxyResponse(serverSocket.getInputStream(),
					m_clientSocket.getOutputStream());
				Thread prt = new Thread(pr, "JAP - DirectProxyResponse");
				prt.start();
				// create stream --> server
				OutputStream outputStream = serverSocket.getOutputStream();

				// Transfer data client --> server
				//first check if we use authorization for the proxy
				if (JAPModel.getInstance().getProxyInterface().isAuthenticationUsed())
				{ //we need to insert an authorization line...
					//read first line and after this insert the authorization
					String str = JAPUtil.readLine(inputStream);
					str += "\r\n";
					outputStream.write(str.getBytes());
					str = JAPModel.getInstance().getProxyInterface().getProxyAuthorizationHeaderAsString();
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
