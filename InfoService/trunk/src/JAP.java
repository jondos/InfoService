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
import java.awt.Frame;
import jap.JAPAWTMsgBox;
import jap.JAPConstants;
import jap.JAPController;
import jap.JAPDebug;
import jap.JAPMessages;
import jap.JAPSplash;
import jap.JAPView;
import jap.JAPViewIconified;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import pay.control.PayControl;

/** This is the main class of the JAP project. It starts everything. It can be inherited by another
 *  class that wants to initialize platform dependend features, e.g. see
 *  <A HREF="JAPMacintosh.html">JAPMacintosh.html</A>
 *  as an example.
 */
class JAP extends Frame
{
	// um pay funktionalität ein oder auszuschalten
	private boolean loadPay = false;

	String[] m_arstrCmdnLnArgs = null;
	public JAP()
	{
	}

	/** Constructor for the JAP object.
	 * @param argv The commandline arguments.
	 */
	JAP(String[] argv)
	{
		m_arstrCmdnLnArgs = argv;
	}

	/** Initializes and starts the JAP.
	 */
	void startJAP()
	{
		final String msg =
			"JAP must run with a 1.1.3 or higher version Java!\nYou will find more information at the JAP webpage!\nYour Java Version: ";
		String javaVersion = System.getProperty("java.version");
		String vendor = System.getProperty("java.vendor");
		String os = System.getProperty("os.name");
		String mrjVersion = System.getProperty("mrj.version");
		//Macintosh Runtime for Java (MRJ) on Mac OS
		// Test (part 1) for right JVM
		if (javaVersion.compareTo("1.0.2") <= 0)
		{
			System.out.println(msg + javaVersion);
			System.exit(0);
		}
		// Init Messages....
		JAPMessages.init();
		// Test (part 2) for right JVM....
		if (vendor.startsWith("Transvirtual"))
		{ // Kaffe
			if (javaVersion.compareTo("1.0.5") <= 0)
			{
				JAPAWTMsgBox.MsgBox(
					this,
					JAPMessages.getString("errorNeedNewerJava"),
					JAPMessages.getString("error"));
				System.exit(0);
			}
		}
		else
		{
			if (javaVersion.compareTo("1.0.2") <= 0)
			{
				System.out.println(msg + javaVersion);
				System.exit(0);
			}
			if (javaVersion.compareTo("1.1.2") <= 0)
			{
				JAPAWTMsgBox.MsgBox(
					this,
					JAPMessages.getString("errorNeedNewerJava"),
					JAPMessages.getString("error"));
				System.exit(0);
			}
		}
		// Show splash screen
		JAPSplash splash = new JAPSplash(this);
		// Test for Swing
		try
		{
			Object o = new javax.swing.JLabel();
			o = null;
		}
		catch (NoClassDefFoundError e)
		{
			JAPAWTMsgBox.MsgBox(
				this,
				JAPMessages.getString("errorSwingNotInstalled"),
				JAPMessages.getString("error"));
			System.exit(0);
		}
		// Create debugger object and set the LogHolder to JAPDebug
		LogHolder.setLogInstance(JAPDebug.getInstance());
		JAPDebug.getInstance().setLogType(
			LogType.NET + LogType.GUI + LogType.THREAD + LogType.MISC);
		JAPDebug.getInstance().setLogLevel(LogLevel.WARNING);

		// Set the default Look-And-Feel
		if (!os.regionMatches(true, 0, "mac", 0, 3))
		{
			LogHolder.log(
				LogLevel.DEBUG,
				LogType.GUI,
				"JAP:Setting Cross Platform Look-And-Feel!");
			try
			{
				javax.swing.UIManager.setLookAndFeel(
					javax.swing.UIManager.getCrossPlatformLookAndFeelClassName());
			}
			catch (Exception e)
			{
				LogHolder.log(
					LogLevel.EXCEPTION,
					LogType.GUI,
					"JAP:Exception while setting Cross Platform Look-And-Feel!");
			}
		}
		// um pay funktionalität ein oder auszuschalten
		if (m_arstrCmdnLnArgs != null)
		{
			for (int i = 0; i < m_arstrCmdnLnArgs.length; i++)
			{
				if (m_arstrCmdnLnArgs[i].equalsIgnoreCase("-pay"))
				{
					loadPay = true;
					break;
				}
			}
		}

		// Create the controller object
		JAPController controller = JAPController.create();
		// load settings from config file
		controller.loadConfigFile(null);
		// Output some information about the system
		LogHolder.log(
			LogLevel.INFO,
			LogType.MISC,
			"JAP:Welcome! This is version " + JAPConstants.aktVersion + " of JAP.");
		LogHolder.log(
			LogLevel.INFO,
			LogType.MISC,
			"JAP:Java " + javaVersion + " running on " + os + ".");
		if (mrjVersion != null)
		{
			LogHolder.log(
				LogLevel.INFO,
				LogType.MISC,
				"JAP:MRJ Version is " + mrjVersion + ".");
			//initalisiere PayInstance
		}
		if (loadPay)
		{
			PayControl.initPay();
			// Create the view object
		}
		JAPView view = new JAPView(JAPConstants.TITLE);
		// Create the main frame
		view.create(loadPay);
		// Switch Debug Console Parent to MainView
		JAPDebug.setConsoleParent(view);
		// Add observer
		controller.addJAPObserver(view);
		// Register the Main view where they are needed
		controller.registerMainView(view);
		// Create the iconified view
		JAPViewIconified viewIconified = new JAPViewIconified();
		controller.addJAPObserver(viewIconified);
		// Register the views where they are needed
		view.registerViewIconified(viewIconified);
		//Init Crypto...
		//		java.security.Security.addProvider(new cryptix.jce.provider.CryptixCrypto());
		// Show main frame and dispose splash screen
		view.show();
		view.toFront();
		if (m_arstrCmdnLnArgs != null)
		{
			for (int i = 0; i < m_arstrCmdnLnArgs.length; i++)
			{
				if (m_arstrCmdnLnArgs[i].equalsIgnoreCase("-minimized"))
				{
					view.hideWindowInTaskbar();
					break;
				}
			}
		}
		splash.dispose();
		// pre-initalize anon service
		anon.server.AnonServiceImpl.init();
		// initially start services
		controller.initialRun();
	}

	public static void main(String[] argv)
	{
		// do NOT change anything in main!
		JAP jap = new JAP(argv);
		jap.startJAP();
	}
}
