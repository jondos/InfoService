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
/** Project Web Mixes: JAP
 *
 *  The class JAP can be inherited by another class
 *  in order to implement system specific stuff, e.g.
 *  on a Macintosh to register the MRJ Handler.
 *
 */
import java.security.SecureRandom;
import java.awt.Frame;
import java.util.Hashtable;
import java.util.Locale;

import anon.client.crypto.KeyPool;
import gui.JAPAWTMsgBox;
import gui.JAPDll;
import gui.JAPMessages;
import gui.dialog.JAPDialog;
import gui.GUIUtils;
import jap.JAPConstants;
import jap.JAPController;
import jap.JAPDebug;
import jap.JAPModel;
import jap.JAPNewView;
import jap.JAPSplash;
import jap.ConsoleSplash;
import jap.IJAPMainView;
import jap.ConsoleJAPMainView;
import jap.JAPViewIconified;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import jap.AbstractJAPMainView;
import jap.ISplashResponse;

/** This is the main class of the JAP project. It starts everything. It can be inherited by another
 *  class that wants to initialize platform dependend features, e.g. see
 *  <A HREF="JAPMacintosh.html">JAPMacintosh.html</A>
 *  as an example.
 */
public class JAP
{
	private static final String MSG_ERROR_NEED_NEWER_JAVA = "errorNeedNewerJava";
	private static final String MSG_GNU_NOT_COMPATIBLE = JAP.class.getName() +  "_gnuNotCompatible";
	private static final String MSG_LOADING_INTERNATIONALISATION = JAP.class.getName() +
		"_loadingInternationalisation";
	private static final String MSG_LOADING_SETTINGS = JAP.class.getName() +
		"_loadingSettings";
	private static final String MSG_STARTING_CONTROLLER = JAP.class.getName() +
		"_startingController";
	private static final String MSG_INIT_DLL = JAP.class.getName() +  "_initLibrary";
	private static final String MSG_INIT_VIEW = JAP.class.getName() +  "_initView";
	private static final String MSG_INIT_ICON_VIEW = JAP.class.getName() +  "_initIconView";
	private static final String MSG_INIT_RANDOM = JAP.class.getName() +  "_initRandom";
	private static final String MSG_FINISH_RANDOM = JAP.class.getName() +  "_finishRandom";
	private static final String MSG_START_LISTENER = JAP.class.getName() +  "_startListener";


	private boolean bConsoleOnly = false;
	private boolean loadPay = true;
	private JAPController m_controller;

	Hashtable m_arstrCmdnLnArgs = null;
	String[] m_temp = null;

	public JAP()
	{
	}

	/** Constructor for the JAP object.
	 * @param argv The commandline arguments.
	 */
	JAP(String[] argv)
	{
		m_temp = argv;
		if (argv != null)
		{
			if (argv.length > 0)
			{
				m_arstrCmdnLnArgs = new Hashtable(argv.length);
			}
			else
			{
				m_arstrCmdnLnArgs = new Hashtable();
			}
			for (int i = 0; i < argv.length; i++)
			{
				m_arstrCmdnLnArgs.put(argv[i], argv[i]);
			}
		}
		else
		{
			m_arstrCmdnLnArgs = new Hashtable();
		}
	}

	/** Initializes and starts the JAP.
	 */
	public void startJAP()
	{
		final String msg =
			"JAP must run with a 1.1.3 or higher version Java!\nYou will find more information at the JAP webpage!\nYour Java Version: ";
		String javaVersion = System.getProperty("java.version");
		String vendor = System.getProperty("java.vendor");
		String os = System.getProperty("os.name");
		String mrjVersion = System.getProperty("mrj.version");

		if (isArgumentSet("--version") || isArgumentSet("-v"))
		{
			System.out.println("JAP version: " + JAPConstants.aktVersion + "\n" +
							   "Java Vendor: " + vendor + "\n" +
							   "Java Version: " + javaVersion +"\n");
			System.exit(0);
		}

		if (!JAPConstants.m_bReleasedVersion)
		{
			System.out.println("Starting up JAP version " + JAPConstants.aktVersion +". (" + javaVersion + "/" + vendor + "/" + os +
							   (mrjVersion != null ? "/" + mrjVersion : "")  + ")");
		}
		//Macintosh Runtime for Java (MRJ) on Mac OS
		// Test (part 1) for right JVM
		if (javaVersion.compareTo("1.0.2") <= 0)
		{
			System.out.println(msg + javaVersion);
			System.exit(0);
		}

		if (isArgumentSet("--help") || isArgumentSet("-h"))
		{
			System.out.println("Usage:");
			System.out.println("--help, -h:              Show this text.");
			System.out.println("--console, -c:           Start JAP in console-only mode.");
			System.out.println("--minimized, -m:         Minimize JAP on startup.");
			System.out.println("--version, -v:           Print version information.");
			System.out.println("--showDialogFormat       Show and set dialog format options.");
			System.out.println("--config, -c {Filename}: Force JAP to use a specific configuration file.");
			System.exit(0);
		}

		if (isArgumentSet("-console") || isArgumentSet("--console"))
		{
			bConsoleOnly = true;
		}

		// Test (part 2) for right JVM....
		if (vendor.startsWith("Transvirtual"))
		{ // Kaffe

			if (javaVersion.compareTo("1.0.5") <= 0)
			{
				JAPMessages.init(JAPConstants.MESSAGESFN);
				if (bConsoleOnly)
				{
					System.out.println(JAPMessages.getString(MSG_ERROR_NEED_NEWER_JAVA));
				}
				else
				{
					JAPAWTMsgBox.MsgBox(
						new Frame(),
						JAPMessages.getString(MSG_ERROR_NEED_NEWER_JAVA),
						JAPMessages.getString("error"));
				}
				System.exit(0);
			}
		}
		else if (vendor.toUpperCase().indexOf("FREE SOFTWARE FOUNDATION") >= 0)
		{
			JAPMessages.init(JAPConstants.MESSAGESFN);
			// latest version reported not to run: 1.4.2, Free Software Foundation Inc.
			System.out.println("\n" + JAPMessages.getString(MSG_GNU_NOT_COMPATIBLE) + "\n");
			//System.exit(0);
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
				JAPMessages.init(JAPConstants.MESSAGESFN);
				if (bConsoleOnly)
				{
					System.out.println(JAPMessages.getString(MSG_ERROR_NEED_NEWER_JAVA));
				}
				else
				{
					JAPAWTMsgBox.MsgBox(
						new Frame(),
						JAPMessages.getString(MSG_ERROR_NEED_NEWER_JAVA),
						JAPMessages.getString("error"));
				}
				System.exit(0);
			}
		}
		// Show splash screen
		ISplashResponse splash;
		String splashText;
		Locale defaultLocale = Locale.getDefault();
		// splashText = JAPMessages.getString(MSG_LOADING_INTERNATIONALISATION);
		if (defaultLocale.getLanguage().equals("de"))
		{
			splashText = "Lade Internationalisierung";
		}
		else
		{
			splashText = "Loading internationalisation";
		}

		if (bConsoleOnly)
		{
			JAPDialog.setConsoleOnly(true);
			splash = new ConsoleSplash();
			splash.setText(splashText);
		}
		else
		{
			Frame hidden = new Frame();
			splash = new JAPSplash(hidden, splashText);
			((JAPSplash)splash).centerOnScreen();
			((JAPSplash)splash).setVisible(true);
			GUIUtils.setAlwaysOnTop(((JAPSplash)splash), true);
		}

		// Init Messages....
		if (!JAPMessages.isInitialised())
		{
			JAPMessages.init(JAPConstants.MESSAGESFN);
		}

		splash.setText(JAPMessages.getString(MSG_INIT_RANDOM));
		// initialise secure random generators
		Thread secureRandomThread = new Thread(new Runnable()
		{
			public void run()
			{
				KeyPool.start();
				new SecureRandom().nextInt();
			}
		});
		secureRandomThread.setPriority(Thread.MIN_PRIORITY);
		secureRandomThread.start();


		if (!bConsoleOnly)
		{
			JAPModel.getInstance().setDialogFormatShown(isArgumentSet("--showDialogFormat"));

			GUIUtils.setIconResizer(JAPModel.getInstance().getIconResizer());
			// Test for Swing
			try
			{
				Object o = new javax.swing.JLabel();
				o = null;
			}
			catch (NoClassDefFoundError e)
			{
				JAPAWTMsgBox.MsgBox(
					new Frame(),
					JAPMessages.getString("errorSwingNotInstalled"),
					JAPMessages.getString("error"));
				System.exit(0);
			}
		}
		// Create debugger object and set the LogHolder to JAPDebug
		LogHolder.setLogInstance(JAPDebug.getInstance());
		JAPDebug.getInstance().setLogType(LogType.ALL);
		JAPDebug.getInstance().setLogLevel(LogLevel.WARNING);

		// Set the default Look-And-Feel
		if (!bConsoleOnly && !os.regionMatches(true, 0, "mac", 0, 3))
		{
			LogHolder.log(LogLevel.DEBUG, LogType.GUI, "Setting Cross Platform Look-And-Feel!");
			try
			{
				javax.swing.UIManager.setLookAndFeel(
					javax.swing.UIManager.getCrossPlatformLookAndFeelClassName());
			}
			catch (Exception e)
			{
				LogHolder.log(LogLevel.EXCEPTION, LogType.GUI,
							  "Exception while setting Cross Platform Look-And-Feel!");
			}
		}
		//deactivate socks proxy settings if given by the os
		/*		try
		  {
		   Properties p = System.getProperties();
		   boolean changed = false;
		   if (p.containsKey("socksProxyHost"))
		   {
		 System.out.println("Found sosckProxyHost");
		 p.remove("socksProxyHost");
		 changed = true;
		   }
		   if (p.containsKey("socksProxyPort"))
		   {
		 p.remove("socksProxyPort");
		 changed = true;
		   }
		   if (changed)
		   {

		 p.list(System.out);
		 System.setProperties(p);
		 //System.setProperty("socksProxyHost","hallo");
		 System.out.println("removed socks settings");
		 Socket.setSocketImplFactory(null);
		   }
		  }
		  catch (Throwable t)
		  {
		   t.printStackTrace();
		   LogHolder.log(
		 LogLevel.EXCEPTION,
		 LogType.NET,
		 "JAP:Exception while trying to deactivate SOCKS proxy settings: " + t.getMessage());
		  }
		 */


		// Create the controller object
		splash.setText(JAPMessages.getString(MSG_STARTING_CONTROLLER));
		m_controller = JAPController.getInstance();
		String cmdArgs = "";
		if (m_temp != null)
		{
			for (int i = 0; i < m_temp.length; i++)
			{
				cmdArgs += " " + m_temp[i];
			}
			m_controller.setCommandLineArgs(cmdArgs);
		}
		String configFileName = null;
		/* check, whether there is the -config parameter, which means the we use userdefined config
		 * file
		 */
		if (m_temp != null)
		{
			for (int i = 0; i < m_temp.length; i++)
			{
				if (m_temp[i].equalsIgnoreCase("-config") || m_temp[i].equalsIgnoreCase("--config") ||
					m_temp[i].equalsIgnoreCase("-c"))
				{
					if (i + 1 < m_temp.length)
					{
						configFileName = m_temp[i + 1];
					}
					break;
				}
			}
		}

		/* check, whether there is the -forwarding_state parameter, which extends
		 * the configuration dialog
		 */
		boolean forwardingStateVisible = false;
		if (isArgumentSet("-forwarding_state"))
		{
			forwardingStateVisible = true;
		}

		JAPModel.getInstance().setForwardingStateModuleVisible(forwardingStateVisible);
		// load settings from config file
		splash.setText(JAPMessages.getString(MSG_LOADING_SETTINGS));
		m_controller.loadConfigFile(configFileName, loadPay, splash);

		splash.setText(JAPMessages.getString(MSG_INIT_DLL));
		JAPDll.init();
		/*
		if (splash instanceof JAPSplash)
		{
			hidden.setName(Double.toString(Math.random()));
			hidden.setTitle(hidden.getName());
			( (JAPSplash) splash).setName(hidden.getName());

			GUIUtils.setAlwaysOnTop( ( (JAPSplash) splash), true);
		}*/
		// Output some information about the system
		LogHolder.log(LogLevel.INFO, LogType.MISC,
			"Welcome! This is version " + JAPConstants.aktVersion + " of JAP.");
		LogHolder.log(LogLevel.INFO, LogType.MISC, "Java " + javaVersion + " running on " + os + ".");
		if (mrjVersion != null)
		{
			LogHolder.log(LogLevel.INFO, LogType.MISC, "MRJ Version is " + mrjVersion + ".");
		}

		splash.setText(JAPMessages.getString(MSG_INIT_VIEW));
		IJAPMainView view;
		if (!bConsoleOnly)
		{
			view = new JAPNewView(JAPConstants.TITLE, m_controller);

			// Create the main frame
			view.create(loadPay);
			//view.setWindowIcon();
			// Switch Debug Console Parent to MainView
			JAPDebug.setConsoleParent((JAPNewView)view);
		}
		else
		{
			view = new ConsoleJAPMainView();
		}
		// Add observer
		m_controller.addJAPObserver(view);
		m_controller.addEventListener(view);
		// Register the Main view where they are needed
		m_controller.setView(view);

		// Create the iconified view
		if (!bConsoleOnly)
		{
			splash.setText(JAPMessages.getString(MSG_INIT_ICON_VIEW));
			JAPViewIconified viewIconified;
			viewIconified = new JAPViewIconified((AbstractJAPMainView)view);
			// Register the views where they are needed
			view.registerViewIconified(viewIconified);
		}

		//Init Crypto...
		//		java.security.Security.addProvider(new cryptix.jce.provider.CryptixCrypto());
		// Show main frame and dispose splash screen
		//view.show();
		//view.setVisible(true);
		//view.toFront();
		boolean bSystray = JAPModel.getMoveToSystrayOnStartup();
		if (isArgumentSet("-minimized") || isArgumentSet("--minimized") || isArgumentSet("-m"))
		{
			bSystray = true;
		}

		splash.setText(JAPMessages.getString(MSG_FINISH_RANDOM));
		try
		{
			secureRandomThread.join();
		}
		catch (InterruptedException a_e)
		{
			LogHolder.log(LogLevel.NOTICE, LogType.CRYPTO, a_e);
		}

		splash.setText(JAPMessages.getString(MSG_START_LISTENER));
		if (!m_controller.startHTTPListener())
		{
			view.disableSetAnonMode();
		}

		if (!bConsoleOnly)
		{
			JAPNewView frameView = (JAPNewView)view;
			if (bSystray)
			{
				/* The old JAPDll does return false even if hideWindowInTaskbar() succeeded - so we have to do
				 * this to circumvent the bug...
				 * @todo Remove if new DLL is deployed
				 */
				String s = JAPDll.getDllVersion();
				boolean bOldDll = false;
				if (s == null || s.compareTo("00.02.00") < 0)
				{
					frameView.setVisible(true);
					frameView.toFront();
					bOldDll = true;
				}
				if (!frameView.hideWindowInTaskbar() && !bOldDll)
				{
					frameView.setVisible(true);
					frameView.toFront();
				}
			}
			else if (JAPModel.getMinimizeOnStartup())
			{
				frameView.setVisible(true);
				frameView.showIconifiedView();
			}
			else
			{
				GUIUtils.setAlwaysOnTop(frameView, true);
				frameView.setVisible(true);
				frameView.toFront();
				GUIUtils.setAlwaysOnTop(frameView, false);
			}
			((JAPSplash)splash).dispose();
		}

		//WP: check japdll.dll version
		JAPDll.checkDllVersion(true);

		// initially start services
		m_controller.initialRun();

		if (bConsoleOnly)
		{
			try
			{
				String entered = null;
				while (true)
				{
					System.out.println("Type 'exit' to quit or 'save' to save the configuration.");
					entered = new java.io.BufferedReader(new java.io.InputStreamReader(System.in)).readLine();
					if (entered == null)
					{
						continue;
					}
					if (entered.equals("exit"))
					{
						break;
					}
					if (entered.equals("save"))
					{
						System.out.println("Saving configuration...");
						if (!m_controller.saveConfigFile())
						{
							System.out.println("Configuration saved!");
						}
						else
						{
							System.out.println("Error while saving configuration!");
						}
					}

				}
				m_controller.goodBye(true);
			}
			catch (java.io.IOException a_e)
			{
			}
		}
	}

	public boolean isArgumentSet(String a_argument)
	{
		return m_arstrCmdnLnArgs.containsKey(a_argument);
	}

	public static void main(String[] argv)
	{
		// do NOT change anything in main!
		JAP jap = new JAP(argv);
		jap.startJAP();
	}
}