import anon.JAPAnonService;
import anon.JAPAnonServiceListener;

class JAPLean implements JAPAnonServiceListener {

	static JAPAnonService    japAnonService = null;
	
	static int      portNumberHTTPListener;
	static int      portNumberAnonService;
	static String   hostNameAnonService;

	static int      nrOfChannels      = 0;
	static int      nrOfBytes         = 0;

	JAPLean ( ) {		
		JAPDebug.create();
		JAPDebug.setDebugType(JAPDebug.NET);
		JAPDebug.setDebugLevel(JAPDebug.ERR);
		
		// JAPAnonService.init();
		japAnonService = new JAPAnonService(portNumberHTTPListener,JAPAnonService.PROTO_HTTP,false);
		japAnonService.setAnonService(hostNameAnonService,portNumberAnonService);
		int returnCode = japAnonService.start();
		japAnonService.setAnonServiceListener(this);
		if (returnCode == JAPAnonService.E_SUCCESS) {
			System.out.print("Amount of anonymized bytes: ");
			Thread t = new Thread(new JAPLeanActivityLoop());
			t.start();
		}
		else if (returnCode ==JAPAnonService.E_BIND) {
			System.err.println("Error binding listener!");
			System.exit(1);
		} else {
			System.err.println("Error connecting to anon service!");
			System.exit(1);
		}
	}
	
	public static void main(String[] argv) {
		// check for command line
		if (argv == null || argv.length < 3) {			
			System.err.println("Usage: JAPLean <listener_port> <first_mix_address> <first_mix_port>");
			System.exit(1);
		}
		portNumberHTTPListener = Integer.parseInt(argv[0]);
		hostNameAnonService    = argv[1];
		portNumberAnonService  = Integer.parseInt(argv[2]);
		System.out.println("["+portNumberHTTPListener+"]-->["+hostNameAnonService+":"+portNumberAnonService+"]");
		new JAPLean();
	}	
	  
	/* Implementation of Interface JAPAnonServiceListener */
	public void channelsChanged(int channels) {
		nrOfChannels=channels;
	}

	/* Implementation of Interface JAPAnonServiceListener */
	public void transferedBytes(int bytes) {
		nrOfBytes+=bytes;
	}
		
	private final class JAPLeanActivityLoop implements Runnable {
		int nrOfBytesBefore = -1;
		public void run() {
			while(true) {
				if(nrOfBytesBefore<nrOfBytes) {
					System.out.print("["+nrOfBytes+"] ");
					nrOfBytesBefore = nrOfBytes;
				}
				try { Thread.sleep(60000); } catch (Exception e) { ; }
			}
		}
	}
}
