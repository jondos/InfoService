import java.net.* ;
import java.io.*;

public class JAPProxyServer implements Runnable
	{
    private boolean runFlag; 
	private boolean isRunningMux = false;
	private boolean isRunningProxy = false;
    private int portN;
    private ServerSocket server;
    private Socket socket;
    private JAPMuxSocket oMuxSocket;
//    private Thread oMuxSocketThread;
    private JAPModel model;
    

    public JAPProxyServer (int port)
			{
				portN = port;
				model=JAPModel.getModel();
			}

    public void run()
			{
				server = null;
				socket = null;
				runFlag = true;
				
				model.status1 = model.getString("statusRunning");
				model.notifyJAPObservers();
				JAPDebug.out(JAPDebug.INFO,JAPDebug.NET,"JAPProxyServer:Listener on port " + portN + " started.");
				try {
					server = new ServerSocket (portN);
					isRunningProxy = true;
					while(runFlag) {
						socket = server.accept();
						if (isRunningMux)
							oMuxSocket.newConnection(new JAPSocket(socket));
						else {
							// if not anon mode selected then call a class
							// that processes the request without anonymity
							JAPDirectConnection doIt = new JAPDirectConnection(socket);
							Thread thread = new Thread (doIt);
							thread.start();
						}
					}
				}
				catch (Exception e) {
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
																	 null, 
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
								 null, 
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
