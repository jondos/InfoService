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

/** This is the main class of the JAP project. It starts everything. It can be inherited by another
 *  class that wants to initialize platform dependend features, e.g. see
 *  <A HREF="JAPMacintosh.html">JAPMacintosh.html</A>
 *  as an example.
 */
class JAP extends Frame {

	boolean bSupportRMI=false;

	/** Constructor for the JAP object.
	 * @param argv The commandline arguments.
	 *        <code>-rmi</code>   Enable Support for RMI, so that other apllications can control the JAP
	 */
	JAP(String[] argv) {
		if(argv!=null&&argv.length>0) {
			for(int i=0;i<argv.length;i++) {
				if(argv[i].equalsIgnoreCase("-rmi"))
					 bSupportRMI=true;
			}
		}
	}

	/** Initializes and starts the JAP.
	 */
	void startJAP() {
		final String  msg  = "JAP must run with a 1.1.3 or higher version Java!\nYou will find more information at the JAP webpage!\nYour Java Version: ";
		String javaVersion = System.getProperty("java.version");
		String vendor      = System.getProperty("java.vendor");
		String os          = System.getProperty("os.name");
		String mrjVersion  = System.getProperty("mrj.version"); //Macintosh Runtime for Java (MRJ) on Mac OS
		// Test (part 1) for right JVM
		if (javaVersion.compareTo("1.0.2") <= 0) {
			System.out.println(msg+javaVersion);
			System.exit(0);
		}
		// Init Messages....
		JAPMessages.init();
		// Test (part 2) for right JVM....
		if(vendor.startsWith("Transvirtual"))  {  // Kaffe
			if (javaVersion.compareTo("1.0.5") <= 0) {
				JAPAWTMsgBox.MsgBox(this,JAPMessages.getString("errorNeedNewerJava"),JAPMessages.getString("error"));
				System.exit(0);
			}
		} else {
			if (javaVersion.compareTo("1.0.2") <= 0) {
				System.out.println(msg+javaVersion);
				System.exit(0);
			}
			if (javaVersion.compareTo("1.1.2") <= 0) {
				JAPAWTMsgBox.MsgBox(this,JAPMessages.getString("errorNeedNewerJava"),JAPMessages.getString("error"));
				System.exit(0);
			}
		}
		// Show splash screen
		JAPSplash splash = new JAPSplash(this);
		// Test for Swing
		try {
			Object o=new javax.swing.JLabel();
			o=null;
		} catch(NoClassDefFoundError e) {
			JAPAWTMsgBox.MsgBox(this,JAPMessages.getString("errorSwingNotInstalled"),JAPMessages.getString("error"));
			System.exit(0);
		}
		// Create the model object
		JAPModel model = JAPModel.create();
		// Create debugger object
		JAPDebug.create();
		JAPDebug.setDebugType(JAPDebug.NET+JAPDebug.GUI+JAPDebug.THREAD+JAPDebug.MISC);
		JAPDebug.setDebugLevel(JAPDebug.WARNING);
		// load settings from config file
		model.loadConfigFile();
		// Output some information about the system
		JAPDebug.out(JAPDebug.INFO,JAPDebug.MISC,"JAP:Welcome! This is version "+JAPConstants.aktVersion+" of JAP.");
		JAPDebug.out(JAPDebug.INFO,JAPDebug.MISC,"JAP:Java "+javaVersion+" running on "+os+".");
		if (mrjVersion != null)
			JAPDebug.out(JAPDebug.INFO,JAPDebug.MISC,"JAP:MRJ Version is "+mrjVersion+".");
		// Set the Look-And-Feel
		if (!os.regionMatches(true,0,"mac",0,3)) {
			JAPDebug.out(JAPDebug.DEBUG,JAPDebug.GUI,"JAP:Setting Cross Platform Look-And-Feel!");
			try {
				javax.swing.UIManager.setLookAndFeel(javax.swing.UIManager.getCrossPlatformLookAndFeelClassName());
			} catch (Exception e) {
				JAPDebug.out(JAPDebug.EXCEPTION,JAPDebug.GUI,"JAP:Exception while setting Cross Platform Look-And-Feel!");
			}
		}
		// Create the view object
		JAPView view = new JAPView(JAPConstants.TITLE);
		// Create the main frame
		view.create();
		// Switch Debug Console Parent to MainView
		JAPDebug.setConsoleParent(view);
		// Add observer
		model.addJAPObserver(view);
		// Create the iconified view
		JAPViewIconified viewIconified = new JAPViewIconified(JAPConstants.TITLEOFICONIFIEDVIEW);
		model.addJAPObserver(viewIconified);
		// Register the views where they are needed
		model.registerView(view);
		viewIconified.registerMainView(view);
		view.registerViewIconified(viewIconified);
		//Init Crypto...
//		java.security.Security.addProvider(new cryptix.jce.provider.CryptixCrypto());
		// Enable RMI if requested
		model.setRMISupport(bSupportRMI);

		// Show main frame and dispose splash screen
		view.show();
		view.toFront();
		splash.dispose();
		// pre-initalize anon service
		anonnew.server.AnonServiceImpl.init();

    // initially start services
		model.initialRun();
	}

	public static void main(String[] argv) {
		// do NOT change anything in main!
		JAP jap = new JAP(argv);
		jap.startJAP();
	}
}
