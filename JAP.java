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

		JAPModel model = new JAPModel();
		JAPSplash splash = new JAPSplash(model);
		splash.show(); // show splash screen as soon as possible
//		Security.addProvider(new Cryptix());
		model.load();
		model.addJAPObserver(model);

		JAPView view = new JAPView (model.TITLE);
		model.addJAPObserver(view);
		splash.dispose();
		view.show();
		model.keypool=new JAPKeyPool(20,16);
		model.keypool.run();

//		model.setAnonMode(true);
	}

}
