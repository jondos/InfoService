/** Project Web Mixes
 *  Java Anon Proxy
 */

//import java.security.Security;
//import cryptix.jce.provider.Cryptix;
public final class JAP {

	public static void main(String[] argv) {
		
		String vers = System.getProperty("java.version");
		if (vers.compareTo("1.1.2") < 0) {
			System.out.println("!!!WARNING: JAP must be run with a " +
			 "1.1.2 or higher version VM!!!");
		}
		String os = System.getProperty("os.name");
		
		// Create debugger object
		JAPDebug jdebug=new JAPDebug();
		JAPDebug.setDebugType(JAPDebug.NET+JAPDebug.GUI+JAPDebug.THREAD+JAPDebug.MISC);
		JAPDebug.setDebugLevel(JAPDebug.INFO);
		JAPDebug.out(JAPDebug.INFO,JAPDebug.MISC,"JAP:Welcome! Java "+vers+" running on "+os+" ...");

		// Create the model object
		JAPModel model = new JAPModel();
		JAPSplash splash = new JAPSplash(model);
		splash.show(); // show splash screen as soon as possible

//		Security.addProvider(new Cryptix());
		
		// load settings from config file
		model.load();
		
		// register Handlers if running JAP under Mac OS
		if (os.equals("Mac OS")) {
			JAPDebug.out(JAPDebug.INFO,JAPDebug.MISC,"JAP:Registering MRJHandlers");
			model.registerMRJHandlers();
		}

		// Create the main frame
		JAPView view = new JAPView (model.TITLE);
		model.addJAPObserver(view);
		
		// Dispose the spash screen and show main frame
		splash.dispose();
		view.show();
		
		// initially start services
		model.initialRun();
		
		model.keypool=new JAPKeyPool(20,16);
		model.keypool.run();
	}
}
