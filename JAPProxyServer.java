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
import java.net.* ;
import java.io.*;

public class JAPProxyServer implements Runnable
	{
    private boolean runFlag; 
		private boolean isRunningMux = false;
		private boolean isRunningProxy = false;
    private int portN;
    private ServerSocket server;
    private JAPMuxSocket oMuxSocket;
    private JAPModel model;
    

    public JAPProxyServer (int port) 
			{
				portN = port;
				model=JAPModel.getModel();
			}

		public boolean create()
			{
				server = null;
				try 
					{
						server = new ServerSocket (portN);
						JAPDebug.out(JAPDebug.INFO,JAPDebug.NET,"JAPProxyServer:Listener on port " + portN + " started.");
						isRunningProxy = true;
						return true;
					}
				catch(Exception e)
					{
						server=null;
						isRunningProxy = false;
						return false;
					}
			}
		
    public void run()
			{
				runFlag = true;
				model.status1 = model.getString("statusRunning");
				model.notifyJAPObservers();
				try 
					{
						while(runFlag)
							{
								Socket socket = server.accept();
								if (isRunningMux)
									oMuxSocket.newConnection(new JAPSocket(socket));
								else
									{
										// if not anon mode selected then call a class
										// that processes the request without anonymity
										JAPDirectConnection doIt = new JAPDirectConnection(socket);
										Thread thread = new Thread (doIt);
										thread.start();
									}
							}
					}
				catch (Exception e)
					{
						try {
						server.close();
						} 
						catch (Exception e2) {
						}
						JAPDebug.out(JAPDebug.ERR,JAPDebug.NET,"JAPProxyServer:ProxyServer.run() Exception: " +e);
					}
				stopMux();
				isRunningProxy = false;
				JAPDebug.out(JAPDebug.INFO,JAPDebug.NET,"JAPProxyServer:ProxyServer on port " + portN + " stopped.");
				model.status1 = model.getString("statusNotRunning");
				model.notifyJAPObservers();
    }
	
	public boolean startMux()
		{
			if (isRunningProxy && (isRunningMux == false)) 
				{
					try
						{
							JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"JAPProxyServer:Mux starting...");
							oMuxSocket = new JAPMuxSocket();
							if(oMuxSocket.connect(model.anonHostName,model.anonPortNumber)==-1)
								{
									model.setAnonMode(false);
									model.status2 = model.getString("statusCannotConnect");
									model.notifyJAPObservers();
									javax.swing.JOptionPane.showMessageDialog
																	(
																	 model.getView(), 
																	 model.getString("errorConnectingFirstMix"),
																	 model.getString("errorConnectingFirstMixTitle"),
																	 javax.swing.JOptionPane.ERROR_MESSAGE
																	);
									return false;
								}
							oMuxSocket.start();
							model.status2 = model.getString("statusRunning");
							isRunningMux = true;
							model.notifyJAPObservers();							
						}
					catch (Exception e)
						{
							JAPDebug.out(JAPDebug.EXCEPTION,JAPDebug.NET,"JAPProxyServer:startMux() Exception: " +e);
							model.setAnonMode(false);
							model.status2 = model.getString("statusCannotConnect");
							model.notifyJAPObservers();
							javax.swing.JOptionPane.showMessageDialog
								(
								 model.getView(), 
								 model.getString("errorConnectingFirstMix"),
								 model.getString("errorConnectingFirstMixTitle"),
								 javax.swing.JOptionPane.ERROR_MESSAGE
								);
							return false;
						}
				}
			return true;
		}
	
	public void stopMux()
		{
			JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"JAPProxyServer:Mux stopping...");
			try
				{
					if(oMuxSocket!=null)
						oMuxSocket.close();
				}
			catch(Exception e)
				{ 
					JAPDebug.out(JAPDebug.EXCEPTION,JAPDebug.NET,"JAPProxyServer:stopMux() Exception: " +e);
				}
			oMuxSocket=null;
			isRunningMux = false;
			model.status2 = model.getString("statusNotRunning");
			model.notifyJAPObservers();
		}

    public void stopService() {
		runFlag = false;
		try {
			server.close();
		}
		catch(Exception e) { 
			JAPDebug.out(JAPDebug.EXCEPTION,JAPDebug.NET,"JAPProxyServer:stopService() Exception: " +e);
		}
		stopMux();
		model.status1 = model.getString("statusNotRunning");
		model.notifyJAPObservers();
	}
}
