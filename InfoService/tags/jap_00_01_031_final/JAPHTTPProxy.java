import java.net.* ;

public class JAPHTTPProxy {
	boolean runFlag; 
	int port = 4008;
	ServerSocket server;

	public JAPHTTPProxy (int port) {
		this.port = port;
		// Create debugger object
		JAPDebug.create();
		JAPDebug.setDebugType(JAPDebug.NET+JAPDebug.GUI+JAPDebug.THREAD+JAPDebug.MISC);
		JAPDebug.setDebugLevel(JAPDebug.DEBUG);
	}

	public void startService() {
		server = null;
		runFlag = true;
		while (runFlag) {
			System.out.println("Service on port " + port + " started.");
			try {
				server = new ServerSocket (port);
				while(runFlag) {
					Socket socket = server.accept();
					JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"Incoming connection from "+socket.getInetAddress());
					JAPDirectProxyConnection c = new JAPDirectProxyConnection (socket);
					Thread ct = new Thread(c);
					ct.start();
				}
			} catch (Exception e) {
				try { 
					server.close(); 
				} catch (Exception e2) { 
					; 
				}
				JAPDebug.out(JAPDebug.EXCEPTION,JAPDebug.NET,"Exception: " +e);
			}
		}
		JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"Service on port " + port + " stopped.");
	}

	public void stopService() {
		runFlag = false;
		try {
			server.close();
		} catch(Exception e) { 
			;
		}
	}

	public static void help() {
		System.out.println("HTTPProxy");
		System.out.println(" Options: -debug -port <port>");
	}
		
	public static void main(String[] args) {
		int cmdPort = 4001;
		boolean showHelp=false;
		if (args.length==0)
					showHelp=true;
		try {
		    for (int i = 0; i < args.length; i++) {
				if (args[i].equalsIgnoreCase("-port")) {
		    		cmdPort = Integer.parseInt(args[i+1]);
		    		i++;
				} else {
					showHelp=true;
	    		}
		    }
		} catch (Exception e) {
					showHelp=true;
		}
		if (showHelp)
			help();
		JAPHTTPProxy p = new JAPHTTPProxy(cmdPort);
		p.startService();
	}
	
}
