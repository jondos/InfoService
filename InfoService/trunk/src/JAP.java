/*
Copyright (c) 2000, The JAP-Team 
All rights reserved.
Redistribution and use in source and binary forms, with or without modification, 
are permitted provided that the following conditions are met:

	- Redistributions of source code must retain the above copyright notice, 
	  this list of conditions and the following disclaimer.

	- Redistributions in binary form must reproduce the above copyright notice, 
	  this list of conditions and the following disclaimer in the documentation and/or 
		other materials provided with the distribution.

	- Neither the name of the University of Technology Dresden, Germany nor the names of its contributors 
	  may be used to endorse or promote products derived from this software without specific 
		prior written permission. 

	
THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS ``AS IS'' AND ANY EXPRESS 
OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY 
AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS
BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, 
OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER 
IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY 
OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE
*/
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
class JAP extends Frame{

	//JAPDebug jdebug;
	
	/** At the moment - just do nothing...
	 * @param argv The commandline arguments - maybe for future use.
	 */
	JAP(String[] argv) {
	}
	
	/** Initialize and starts the JAP.
	 */
	void startJAP() {
		String javaVersion = System.getProperty("java.version");
		String vendor      = System.getProperty("java.vendor");
		String os          = System.getProperty("os.name");
		String mrjVersion  = System.getProperty("mrj.version"); //Macintosh Runtime for Java (MRJ) on Mac OS

		// Test for right VM....
		if(vendor.startsWith("Transvirtual")) //Kaffe
			{
				if (javaVersion.compareTo("1.0.5") <= 0) 
					{
						JAPAWTMsgBox.MsgBox(this,"JAP must run with a 1.1.3 or higher version of Java! (Kaffe >=1.0.6) \nYou will find more information at the JAP webpage!","Error");
						System.exit(0);
					}
			}
		else
			{
				if (javaVersion.compareTo("1.0.2") <= 0) 
					{
						System.out.println("Your JAVA Version: "+javaVersion);
						System.out.println("JAP must run with a 1.1.3 or higher version Java!");
						System.out.println("You will find more information at the JAP webpage!");
						System.exit(0);
					}
				if (javaVersion.compareTo("1.1.2") <= 0) 
					{
						JAPAWTMsgBox.MsgBox(this,"JAP must run with a 1.1.3 or higher version of Java! \nYou will find more information at the JAP webpage!","Error");
						System.exit(0);
					}	
			}
		
		
		//Init Messages....
		JAPMessages.init();
		
		// Show splash screen
		JAPSplash splash = new JAPSplash();
		
		//Test for Swing....
		try
			{
				Object o=new javax.swing.JLabel();
				o=null;
			}
		catch(NoClassDefFoundError e)
			{
				JAPAWTMsgBox.MsgBox(this,"JAP needs the Swing library to run properly! \nYou will find more information at the JAP webpage!","Error");
				System.exit(0);
			}
		// Create the model object
		JAPModel model = JAPModel.createModel();

		// Create debugger object
		JAPDebug.create();
		JAPDebug.setDebugType(JAPDebug.NET+JAPDebug.GUI+JAPDebug.THREAD+JAPDebug.MISC);
		JAPDebug.setDebugLevel(JAPDebug.WARNING);
		// load settings from config file
		model.load();
		// Output some information about the system
		JAPDebug.out(JAPDebug.INFO,JAPDebug.MISC,"JAP:Welcome! This is version "+model.aktVersion+" of JAP.");
		JAPDebug.out(JAPDebug.INFO,JAPDebug.MISC,"JAP:Java "+javaVersion+" running on "+os+".");
		if (mrjVersion != null)
			JAPDebug.out(JAPDebug.INFO,JAPDebug.MISC,"JAP:MRJ Version is "+mrjVersion+".");
		// Create the view object
		JAPView view = new JAPView (model.TITLE);
		
		// Create the main frame
		view.create();
		
		//Switching Debug Console Parent to MainView
		JAPDebug.setConsoleParent(view);
		
		model.addJAPObserver(view);
		// Create the iconified view
		JAPViewIconified iconifiedView = new JAPViewIconified("JAP");
		model.addJAPObserver(iconifiedView);
		
		//Init Crypto...
		//java.security.Security.addProvider(new cryptix.jce.provider.CryptixCrypto());

		// Dispose the spash screen and show main frame
		view.show();
		view.toFront();		
		splash.dispose();
//		view.show();
//		view.toFront();		
		// initially start services
		model.initialRun();
	}
	
	public static void main(String[] argv) {
		// do NOT change anything in main!
		JAP jap = new JAP(argv);
		jap.startJAP();
	}
}
