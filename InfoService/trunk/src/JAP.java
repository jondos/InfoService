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
		
		// 2000-07-31(HF): JAPDebug now in JAP in order to use
		// the functions also in main()
		//private static JAPDebug jdebug = null;
		JAPDebug jdebug=new JAPDebug();
		JAPDebug.setDebugType(JAPDebug.NET+JAPDebug.GUI+JAPDebug.THREAD+JAPDebug.MISC);
		JAPDebug.setDebugLevel(JAPDebug.DEBUG);
		JAPDebug.out(JAPDebug.INFO,JAPDebug.MISC,"JAP:Welcome! Java "+vers+" running on "+os+" ...");

		JAPModel model = new JAPModel();
		JAPSplash splash = new JAPSplash(model);
		splash.show(); // show splash screen as soon as possible

//		Security.addProvider(new Cryptix());
		
		model.load();
		model.addJAPObserver(model);
		
		if (os.equals("Mac OS")) {
			JAPDebug.out(JAPDebug.INFO,JAPDebug.MISC,"JAP:Registering MRJHandlers");
			model.registerMRJHandlers();
		}

		JAPView view = new JAPView (model.TITLE);
		model.addJAPObserver(view);
		
		splash.dispose();
		view.show();
		
		model.startProxy();
		
		model.keypool=new JAPKeyPool(20,16);
		model.keypool.run();
	}
}
