import java.net.* ;

public class JAPProxyServer implements Runnable
	{
    private boolean runFlag; 
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
				
				JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,
										 "ProxyServer on port " + portN + " started.");
				try
					{
						server = new ServerSocket (portN);
						oMuxSocket=new JAPMuxSocket();
						if(oMuxSocket.connect(model.anonHostName,model.anonPortNumber)==-1)
							{
								model.status2 = model.getString("statusCannotConnect");
								model.notifyJAPObservers();
								return;
							}
						oMuxSocket.start();
						while(runFlag)
							{
								socket = server.accept();
								oMuxSocket.newConnection(new JAPSocket(socket));
							}
					}
				catch (Exception e)
					{
						try 
							{
								server.close();
							} 
						catch (Exception e2)
							{
							}
						JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,
												 "ProxyServer Exception: " +e);
					}
				JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"ProxyServer on port " + portN + " stopped.");
    }

    public void stopService()
			{
				runFlag = false;
				try
					{
						server.close();
					}
				catch(Exception e)
					{ 
					}
				try
					{
						oMuxSocket.close();
					}
				catch(Exception e)
					{ 
					}
			}
}
