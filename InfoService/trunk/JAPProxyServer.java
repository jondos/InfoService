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
    private Thread thread;
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
				JAPDebug.out(JAPDebug.INFO,JAPDebug.NET,"ProxyServer on port " + portN + " started.");
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
					JAPDebug.out(JAPDebug.ERR,JAPDebug.NET,"ProxyServer.run() Exception: " +e);
				}
				stopMux();
				isRunningProxy = false;
				JAPDebug.out(JAPDebug.INFO,JAPDebug.NET,"ProxyServer on port " + portN + " stopped.");
				model.status1 = model.getString("statusNotRunning");
				model.notifyJAPObservers();
    }
	
	public void startMux() throws Exception {
		if (isRunningProxy && (isRunningMux == false)) {
			try {
				JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"Mux starting...");
				oMuxSocket=new JAPMuxSocket();
				if(oMuxSocket.connect(model.anonHostName,model.anonPortNumber)==-1) {
					model.setAnonMode(false);
					model.status2 = model.getString("statusCannotConnect");
					model.notifyJAPObservers();
					throw new Exception("ProxyServer.startMux() Exception: Cannot connect to first Mix.");
				}
				oMuxSocket.start();
				model.status2 = model.getString("statusRunning");
				model.notifyJAPObservers();
				isRunningMux = true;
			}
			catch (Exception e) {
				JAPDebug.out(JAPDebug.EXCEPTION,JAPDebug.NET,"ProxyServer.startMux() Exception: " +e);
				model.setAnonMode(false);
				model.status2 = model.getString("statusCannotConnect");
				model.notifyJAPObservers();
				throw new Exception("ProxyServer.startMux() Exception: " +e);
			}
		}
	}
	
	public void stopMux() {
		JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"Mux stopping...");
		try {
			oMuxSocket.close();
		}
		catch(Exception e){ 
				JAPDebug.out(JAPDebug.EXCEPTION,JAPDebug.NET,"ProxyServer.stopMux() Exception: " +e);
		}
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
			JAPDebug.out(JAPDebug.EXCEPTION,JAPDebug.NET,"ProxyServer.stopService() Exception: " +e);
		}
		stopMux();
		model.status1 = model.getString("statusNotRunning");
		model.notifyJAPObservers();
	}
}
