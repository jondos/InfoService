/** Project Web Mixes: Java Anon Proxy -- JAP
 * 
 *  The class JAP can be inherited by another class
 *  in order to implement system specific stuff, e.g.
 *  on a Macintosh to register the MRJ Handler.
 *  
 */

import java.lang.NoClassDefFoundError;
import java.awt.Frame;
import java.awt.event.WindowEvent;
/** This is the MAIN of all this. It starts everything.
 */
public class JAP extends Frame{

	//JAPDebug jdebug;
	JAPModel model;
	JAPView  view;
	
	/** At the moment - just do nothing...
	 * @param argv The commandline arguments - maybe for future use.
	 */
	private JAP(String[] argv) {
	}
	
	/** Initialize and starts the JAP.
	 */
	private void startJAP() {
		String   os;
		String   vers;
		vers = System.getProperty("java.version");
		// Test for right VM....
		if (vers.compareTo("1.0.2") <= 0) 
			{
				System.out.println("Your JAVA Version: "+vers);
				System.out.println("JAP must be run with a 1.1.3 or higher version VM!");
				System.exit(0);
			}
		if (vers.compareTo("1.1.2") <= 0) 
			{
				JAPAWTMsgBox.MsgBox(this,"JAP must be run with a 1.1.3 or higher version VM!","Error");
				System.exit(0);
			}	
		
		//Test for Swing....
		try
			{
				Object o=new javax.swing.JLabel();
				o=null;
			}
		catch(NoClassDefFoundError e)
			{
				JAPAWTMsgBox.MsgBox(this,"SWING must be installed!","Error");
				System.exit(0);
			}
		
		
		os = System.getProperty("os.name");

		// Create debugger object
		JAPDebug.create();
		JAPDebug.setDebugType(JAPDebug.NET+JAPDebug.GUI+JAPDebug.THREAD+JAPDebug.MISC);
		JAPDebug.setDebugLevel(JAPDebug.WARNING);
		JAPDebug.out(JAPDebug.INFO,JAPDebug.MISC,"JAP:Welcome! Java "+vers+" running on "+os+" ...");

		// Create the model object
		model = JAPModel.createModel();
		JAPSplash splash = new JAPSplash(model);
	//	splash.show(); // show splash screen as soon as possible
		
		// load settings from config file
		model.load();
		
		// Create the main frame
		view = new JAPView (model.TITLE);
		model.addJAPObserver(view);
		
		// Dispose the spash screen and show main frame
		splash.dispose();
		//if(model.getMinimizeOnStartup())
			view.getToolkit().getSystemEventQueue().postEvent(new WindowEvent(view,WindowEvent.WINDOW_ICONIFIED));
		view.show();
		view.toFront();		
		
		// initially start services
		model.initialRun();
	}
	
	public static void main(String[] argv) {
		// do NOT change anything in main!
		JAP jap = new JAP(argv);
		jap.startJAP();
	}
}
