import java.net.* ;

public class ProxyServer implements Runnable {
    private boolean runFlag; 
    private boolean debug = false; 
    private int portN;
    private ServerSocket server;
    private Socket socket;
    private CAMuxSocket oMuxSocket;
    private Thread thread;
    
    public ProxyServer (int port, boolean debugFlag) {
			this.portN = port;
			this.debug = debugFlag;
    }

    public ProxyServer (int port) {
	this.portN = port;
	this.debug = false;
    }

    public void run()
			{
				server = null;
				socket = null;
				runFlag = true;
				
				while (runFlag) {
				if (debug)
					System.out.println("ProxyServer on port " + portN + " started.");
				try
					{
						server = new ServerSocket (portN);
						oMuxSocket=new CAMuxSocket();
						if(oMuxSocket.connect("anon.inf.tu-dresden.de",6543)==-1)
							{
								System.out.println("Cannot connect to Mix...!");
								return;
							}
						oMuxSocket.start();
						while(runFlag)
							{
								socket = server.accept();
								oMuxSocket.newConnection(new CASocket(socket));
							}
					}
				catch (Exception e) {
		try { server.close(); } catch (Exception e2) {
		    ;
		}
		if (debug) System.out.println("ProxyServer Exception: " +e);
	    }
	}
	if (debug) System.out.println("ProxyServer on port " + portN + " stopped.");
    }

    public void stopService() {
	runFlag = false;
	try {
	    server.close();
	} catch(Exception e) { 
	    ;
	}
    }
}
