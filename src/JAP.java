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
import gui.JAPAWTMsgBox;
import jap.JAPConstants;
import jap.JAPController;
import jap.JAPDebug;
import gui.JAPMessages;
import jap.JAPModel;
import jap.JAPNewView;
import jap.JAPSplash;
import jap.JAPViewIconified;

import java.awt.Frame;
import javax.swing.SwingUtilities;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import gui.JAPDll;

/** This is the main class of the JAP project. It starts everything. It can be inherited by another
 *  class that wants to initialize platform dependend features, e.g. see
 *  <A HREF="JAPMacintosh.html">JAPMacintosh.html</A>
 *  as an example.
 */
public class JAP extends Frame
{
	// um pay funktionalitaet ein oder auszuschalten
	private boolean loadPay = true;
	private JAPController m_controller;

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
	public void startJAP()
	{
		final String msg =
			"JAP must run with a 1.1.3 or higher version Java!\nYou will find more information at the JAP webpage!\nYour Java Version: ";
		String javaVersion = System.getProperty("java.version");
		String vendor = System.getProperty("java.vendor");
		String os = System.getProperty("os.name");
		String mrjVersion = System.getProperty("mrj.version");
		if (!JAPConstants.m_bReleasedVersion)
		{
			System.out.println("Starting up JAP. (" + javaVersion + "/" + vendor + "/" + os + "/" +
							   mrjVersion +
							   ")");
		}
		//Macintosh Runtime for Java (MRJ) on Mac OS
		// Test (part 1) for right JVM
		if (javaVersion.compareTo("1.0.2") <= 0)
		{
			System.out.println(msg + javaVersion);
			System.exit(0);
		}
		// Init Messages....
		JAPMessages.init(JAPConstants.MESSAGESFN);
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
		JAPDebug.getInstance().setLogType(LogType.ALL);
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
		// um pay funktionalitaet ein oder auszuschalten
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
		m_controller = JAPController.getInstance();
		String configFileName = null;
		/* check, whether there is the -config parameter, which means the we use userdefined config
		 * file
		 */
		if (m_arstrCmdnLnArgs != null)
		{
			for (int i = 0; i < m_arstrCmdnLnArgs.length; i++)
			{
				if (m_arstrCmdnLnArgs[i].equalsIgnoreCase("-config"))
				{
					if (i + 1 < m_arstrCmdnLnArgs.length)
					{
						configFileName = m_arstrCmdnLnArgs[i + 1];
					}
					break;
				}
			}
		}

		/* check, whether there is the -forwarding_state parameter, which extends
		 * the configuration dialog
		 */
		boolean forwardingStateVisible = false;
		if (m_arstrCmdnLnArgs != null)
		{
			for (int i = 0; i < m_arstrCmdnLnArgs.length; i++)
			{
				if (m_arstrCmdnLnArgs[i].equalsIgnoreCase("-forwarding_state"))
				{
					forwardingStateVisible = true;
					break;
				}
			}
		}

		JAPModel.getInstance().setForwardingStateModuleVisible(forwardingStateVisible);
		// load settings from config file
		m_controller.loadConfigFile(configFileName, loadPay, splash);
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
		JAPNewView view = new JAPNewView(JAPConstants.TITLE, m_controller);
		// Create the main frame
		view.create(loadPay);
		//view.setWindowIcon();
		// Switch Debug Console Parent to MainView
		JAPDebug.setConsoleParent(view);
		// Add observer
		m_controller.addJAPObserver(view);
		m_controller.addEventListener(view);
		// Register the Main view where they are needed
		m_controller.registerMainView(view);
		// Create the iconified view
		JAPViewIconified viewIconified = new JAPViewIconified();
		m_controller.addJAPObserver(viewIconified);
		// Register the views where they are needed
		view.registerViewIconified(viewIconified);
		//Init Crypto...
		//		java.security.Security.addProvider(new cryptix.jce.provider.CryptixCrypto());
		// Show main frame and dispose splash screen
		//view.show();
		//view.setVisible(true);
		//view.toFront();
		boolean bSystray = JAPModel.getMoveToSystrayOnStartup();
		if (m_arstrCmdnLnArgs != null && !bSystray)
		{
			for (int i = 0; i < m_arstrCmdnLnArgs.length; i++)
			{
				if (m_arstrCmdnLnArgs[i].equalsIgnoreCase("-minimized"))
				{
					bSystray = true;
					break;
				}
			}
		}
		if (bSystray)
		{
			/* The old JAPDll does return false even if hideWindowInTaskbar() succeeded - so we have to do this
			 * to circumvent the bug...
			 * @todo Remove if new DLL is deployed
			 */
			String s=JAPDll.getDllVersion();
			boolean bOldDll=false;
			if(s==null||s.compareTo("00.02.00")<0)
			{
				view.setVisible(true);
				view.toFront();
				bOldDll=true;
			}
			if(!view.hideWindowInTaskbar()&&!bOldDll)
			{
				view.setVisible(true);
				view.toFront();
			}
		}
		else if (JAPModel.getMinimizeOnStartup())
		{
			view.showIconifiedView();
		}
		else
		{
			view.setVisible(true);
			view.toFront();
		}
		splash.dispose();
		// pre-initalize anon service
		anon.server.AnonServiceImpl.init();
		//Update account balance
		m_controller.updateAccountStatements();
		// initially start services
		SwingUtilities.invokeLater(new Thread()
		{
			public void run()
			{
				m_controller.initialRun();
			}
		});
	}

	public static void main(String[] argv)
	{
		// do NOT change anything in main!
		JAP jap = new JAP(argv);
		jap.startJAP();
	}
}
