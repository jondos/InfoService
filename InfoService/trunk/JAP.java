
public class JAP {

    public static void main(String[] argv) {

	String vers = System.getProperty("java.version");
	if (vers.compareTo("1.1.2") < 0) {
	    System.out.println("!!!WARNING: JAP must be run with a " +
			       "1.1.2 or higher version VM!!!");
	}

	JAPModel model = new JAPModel();
	JAPSplash splash = new JAPSplash(model, model.TITLE);
	splash.show(); // show splash screen as soon as possible
	model.load();
	model.addJAPObserver(model);

	//Command line testing
	/*
	if (argv != null && argv.length == 1) {
		try {
			model.portNumber = Integer.parseInt(argv[0].trim());
		} catch (Exception e) {
			System.err.println("Invalid portnumber in command line,"
				+ "using default port ("
				+ model.portNumber + ") ");
		}
	}
	*/

	JAPView view = new JAPView (model, model.TITLE);
	model.addJAPObserver(view);
	splash.dispose();
	view.show();

	model.setAnonMode(true);
	    
	//	JAPDummy d = new JAPDummy(model, view);
	//		Thread dt = new Thread (d);
	//		dt.start();
    }
}
