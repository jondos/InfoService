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
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.io.InterruptedIOException;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.text.SimpleDateFormat;


final class JAPDirectProxy implements Runnable
	{
/** 
 *  This class is used to inform the user that he tries to
 *  send requests although anonymity mode is off.
 */
private final class JAPDirectConnection implements Runnable 
	{
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
						toClient.write("<HTML><TITLE> </TITLE>");
						toClient.write("<PRE>"+date+"</PRE>");
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


		private volatile boolean runFlag; 
		private boolean isRunningProxy = false;
    private int portN;
    private ServerSocket socketListener;
    private Thread threadRunLoop;
		private ThreadGroup threadgroupAll;
		private JAPModel model;
    

    public JAPDirectProxy (ServerSocket s) 
			{
				socketListener=s;
				model=JAPModel.getModel();
			}

		public boolean startService()
			{
	//			socketListener = null;
/*				try 
					{
						if(model.getListenerIsLocal())
							{
								InetAddress[] a=InetAddress.getAllByName("localhost");
								JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"Try binding Listener on localhost: "+a[0]);
								socketListener = new ServerSocket (portN,50,a[0]);
							}
						else
							socketListener = new ServerSocket (portN);
						JAPDebug.out(JAPDebug.INFO,JAPDebug.NET,"JAPProxyServer:Listener on port " + portN + " started.");
						Thread t1=new Thread(this);
						t1.start();
						isRunningProxy = true;
						return true;
					}
				catch(Exception e)
					{
						socketListener=null;
						isRunningProxy = false;
						return false;
					}
	*/
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
								JAPDirectConnection doIt = new JAPDirectConnection(socket);
								Thread thread = new Thread (threadgroupAll,doIt);
								thread.start();
							}
					}
				catch (Exception e)
					{
			//			try {
			//			socketListener.close();
			//			} 
			//			catch (Exception e2) {
			//			}
						JAPDebug.out(JAPDebug.ERR,JAPDebug.NET,"JAPProxyServer:ProxyServer.run() Exception: " +e);
					}
				isRunningProxy = false;
				JAPDebug.out(JAPDebug.INFO,JAPDebug.NET,"JAPProxyServer:ProxyServer on port " + portN + " stopped.");
		  }
	

    public void stopService()
			{
				runFlag = false;
				try{threadRunLoop.join(5000);}catch(Exception e){}
				threadgroupAll.stop();
				threadgroupAll.destroy();
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
}
