/** Project Web Mixes: Java Anon Proxy -- JAP
 * 
 *  The class JAP can be inherited by another class
 *  in order to implement system specific stuff, e.g.
 *  on a Macintosh to register the MRJ Handler.
 *  
 */

//import java.security.Security;
//import cryptix.jce.provider.Cryptix;

public /*final*/ class JAP {

	String   os;
	String   vers;
	JAPDebug jdebug;
	JAPModel model;
	JAPView  view;
	
	JAP(String[] argv) {
		this.vers = System.getProperty("java.version");
		if (vers.compareTo("1.1.2") < 0) {
			System.out.println("!!!WARNING: JAP must be run with a " +
			 "1.1.2 or higher version VM!!!");
		}
		this.os = System.getProperty("os.name");
	}
	
	public void startJAP() {
		// Create debugger object
		jdebug = new JAPDebug();
		JAPDebug.setDebugType(JAPDebug.NET+JAPDebug.GUI+JAPDebug.THREAD+JAPDebug.MISC);
		JAPDebug.setDebugLevel(JAPDebug.DEBUG);
		JAPDebug.out(JAPDebug.INFO,JAPDebug.MISC,"JAP:Welcome! Java "+vers+" running on "+os+" ...");

		// Create the model object
		model = new JAPModel();
		JAPSplash splash = new JAPSplash(model);
		splash.show(); // show splash screen as soon as possible

//		Security.addProvider(new Cryptix());
		
		// load settings from config file
		model.load();
		
		// Create the main frame
		view = new JAPView (model.TITLE);
		model.addJAPObserver(view);
		
		// Dispose the spash screen and show main frame
		splash.dispose();
		view.show();
		
		// Keypool stuff now in model.initialRun();
		
		// initially start services
		model.initialRun();
	}
	
	public static void main(String[] argv) {
		// do NOT change anything in main!
		JAP jap = new JAP(argv);
		jap.startJAP();
	}
}
