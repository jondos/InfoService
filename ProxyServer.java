import java.net.* ;

public class ProxyServer implements Runnable
	{
    private boolean runFlag; 
    private boolean debug = false; 
    private int portN;
    private ServerSocket server;
    private Socket socket;
    private CAMuxSocket oMuxSocket;
    private Thread thread;
    private JAPModel model;
    
		public ProxyServer (int port, boolean debugFlag, JAPModel m)
			{
				portN = port;
				debug = debugFlag;
				model=m;
			}

    public ProxyServer (int port,JAPModel m)
			{
				portN = port;
				debug = false;
				model=m;
			}

    public void run()
			{
				server = null;
				socket = null;
				runFlag = true;
				
				while (runFlag)
					{
						if (debug)
							System.out.println("ProxyServer on port " + portN + " started.");
						try
							{
								server = new ServerSocket (portN);
								oMuxSocket=new CAMuxSocket();
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
										oMuxSocket.newConnection(new CASocket(socket));
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
								if (debug) System.out.println("ProxyServer Exception: " +e);
							}
					}
				if (debug) System.out.println("ProxyServer on port " + portN + " stopped.");
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
			}
}
