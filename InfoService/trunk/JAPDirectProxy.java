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
import java.net.InetAddress ;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.Socket ;
import java.net.SocketException;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.InterruptedIOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.text.SimpleDateFormat;


final class JAPDirectProxy implements Runnable
	{
		private volatile boolean runFlag;
		private boolean isRunningProxy = false;
    private int portN;
    private ServerSocket socketListener;
    private Thread threadRunLoop;
		private ThreadGroup threadgroupAll;
		private JAPModel model;
		private boolean warnUser = true;

    public JAPDirectProxy (ServerSocket s)
			{
				socketListener=s;
			  warnUser = true;
				isRunningProxy = false;
				model=JAPModel.getModel();
			}

		public boolean startService()
			{
				if(socketListener==null)
					return false;
				threadgroupAll=new ThreadGroup("directproxy");
				threadRunLoop=new Thread(this);
				threadRunLoop.start();
				isRunningProxy = true;
				return true;
			}

    public void run()
			{
				runFlag = true;
				try
					{
						while(runFlag)
							{
								Socket socket=null;
								try
									{
										socket = socketListener.accept();
									}
								catch(InterruptedIOException e1)
									{
										continue;
									}
								catch(SocketException e2)
									{
										JAPDebug.out(JAPDebug.ERR,JAPDebug.NET,"JAPDirectProxy:DirectProxy.run() accept socket excpetion: " +e2);
										break;
									}
								try
									{
										socket.setSoTimeout(0); //Ensur socket is in Blocking Mode
									}
								catch(SocketException soex)
									{
										JAPDebug.out(JAPDebug.ERR,JAPDebug.NET,"JAPDirectProxy:DirectProxy.run() Colud not set sockt to blocking mode! Excpetion: " +soex);
										socket=null;
										continue;
									}

								if (warnUser)
									{
										JAPDirectConnection      doIt = new JAPDirectConnection(socket);
										Thread thread = new Thread (threadgroupAll,doIt);
										thread.start();
										warnUser=false;
									}
								else
									{
										if (model.getUseFirewall())
											{
												JAPDirectConViaProxy doIt = new JAPDirectConViaProxy (socket);
												Thread thread = new Thread (threadgroupAll,doIt);
												thread.start();
											}
										else
											{
												JAPDirectProxyConnection doIt = new JAPDirectProxyConnection (socket);
												Thread thread = new Thread (threadgroupAll,doIt);
												thread.start();
											}
									}
							}
					}
				catch (Exception e)
					{
						JAPDebug.out(JAPDebug.ERR,JAPDebug.NET,"JAPDirectProxy:DirectProxy.run() Exception: " +e);
					}
				isRunningProxy = false;
				JAPDebug.out(JAPDebug.INFO,JAPDebug.NET,"JAPDirect:DircetProxyServer stopped.");
		}


    public void stopService()
			{
				runFlag = false;
				try{threadRunLoop.join(5000);}catch(Exception e){}
				if(threadgroupAll!=null)
					{
						try //Hack for kaffe!
							{
								threadgroupAll.stop();
								threadgroupAll.destroy();
							}
						catch(Exception e)
							{
							}
					}
				threadgroupAll=null;
				threadRunLoop=null;
			}
	//	try {
	//		socketListener.close();
	//	}
	//	catch(Exception e) {
	//		JAPDebug.out(JAPDebug.EXCEPTION,JAPDebug.NET,"JAPProxyServer:stopService() Exception: " +e);
	//	}
	//}


/**
 *  This class is used to inform the user that he tries to
 *  send requests although anonymity mode is off.
 */
	private final class JAPDirectConnection implements Runnable {
		private JAPModel model;
		private Socket s;
		private SimpleDateFormat dateFormatHTTP;

		public JAPDirectConnection(Socket s)
			{
				this.s = s;
				this.model = JAPModel.getModel();
				dateFormatHTTP=new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz",Locale.US);
				dateFormatHTTP.setTimeZone(TimeZone.getTimeZone("GMT"));
			}

		public void run()
			{
				try
					{
						String date=dateFormatHTTP.format(new Date());
						BufferedWriter toClient = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
						toClient.write("HTTP/1.1 200 OK\r\n");
						toClient.write("Content-type: text/html\r\n");
						toClient.write("Expires: "+date+"\r\n");
						toClient.write("Date: "+date+"\r\n");
						toClient.write("Pragma: no-cache\r\n");
						toClient.write("Cache-Control: no-cache\r\n\r\n");
						toClient.write("<HTML><TITLE>JAP</TITLE>\n");
						toClient.write("<PRE>"+date+"</PRE>\n");
						toClient.write(model.getString("htmlAnonModeOff"));
						toClient.write("</HTML>\n");
						toClient.flush();
						toClient.close();
						s.close();
					}
				catch (Exception e)
					{
						JAPDebug.out(JAPDebug.EXCEPTION,JAPDebug.NET,"JAPFeedbackConnection: Exception: "+e);
					}
			}
	}

/**
 *  This class is used to transfer requests via the selected proxy
 */
	private final class JAPDirectConViaProxy implements Runnable
		{
			private JAPModel model;
			private Socket clientSocket;

			public JAPDirectConViaProxy(Socket s)
				{
					this.clientSocket = s;
					this.model = JAPModel.getModel();
				}

			public void run()
				{
					try
						{
							// open stream from client
							InputStream inputStream = clientSocket.getInputStream();
							// create Socket to Server
							Socket serverSocket = new Socket(model.getFirewallHost(),model.getFirewallPort());
							// Response from server is transfered to client in a sepatate thread
							JAPDirectProxyResponse pr = new JAPDirectProxyResponse(serverSocket.getInputStream(),
																																		 clientSocket.getOutputStream());
							Thread prt = new Thread(pr);
							prt.start();
							// create stream --> server
							OutputStream outputStream = serverSocket.getOutputStream();


							// Transfer data client --> server
							//first check if the us authorization for the proxy
							if(model.getUseFirewallAuthorization())
								{//we need to insert an authorization line...
									//read first line and after this insert the authorization
									String str=JAPUtil.readLine(inputStream);
									str+="\r\n";
									outputStream.write(str.getBytes());
									str=JAPUtil.getProxyAuthorization(model.getFirewallAuthUserID(),model.getFirewallAuthPasswd());
									outputStream.write(str.getBytes());
								}
							byte[] buff=new byte[1000];
							int len;
							while((len=inputStream.read(buff))!=-1)
								{
									if(len>0)
										{
											outputStream.write(buff,0,len);
										}
								}
							outputStream.flush();
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
							JAPDebug.out(JAPDebug.EXCEPTION,JAPDebug.NET,"JAPDirectConViaProxy: Exception: "+e);
						}
				}
		}
}
