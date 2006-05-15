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
package gui;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.reflect.Method;

import java.awt.Window;

import anon.util.ResourceLoader;
import anon.util.ClassUtil;
import gui.dialog.JAPDialog;
import jap.JAPController;
import jap.JAPModel;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;


final public class JAPDll {

	//required japdll.dll version for this JAP-version
	private static final String JAP_DLL_REQUIRED_VERSION = "00.02.001";
	private static final String UPDATE_PATH =
		ClassUtil.getClassDirectory(JAPDll.class).getParent() + File.separator;

	private static final String JAP_DLL     = "japdll.dll";
	private static final String JAP_DLL_NEW  = JAP_DLL + "." + JAP_DLL_REQUIRED_VERSION;
	private static final String JAP_DLL_OLD = "japdll.old";

	/** Message */
	private static final String MSG_DLL_UPDATE = JAPDll.class.getName() + "_updateRestartMessage";
	private static final String MSG_DLL_UPDATE_FAILED = JAPDll.class.getName() + "_updateFailed";

	private static boolean m_sbHasOnTraffic = true;
	static
	{
		try
		{
			if (System.getProperty("os.name", "").toLowerCase().indexOf("win") > -1)
			{
				boolean bUpdateDone = false;
				if ( JAPModel.getInstance().getDLLupdate())
				{
					update();
					bUpdateDone = true;
				}
				System.loadLibrary("japdll");
				if (bUpdateDone && (JAPDll.getDllVersion() == null || // == null means there are problems...
									JAPDll.getDllVersion().compareTo(JAP_DLL_REQUIRED_VERSION) < 0))
				{
					 JAPModel.getInstance().setDLLupdate(true);
					 JAPController.getInstance().saveConfigFile();
				}
			}
		}
		catch (Throwable t)
		{
		}
	}


	/**
	 * This method should be invoked on every JAP-start:
	 * It will check if the existing japdll.dll has the right version to work
	 * propper with this JAP version. If no japdll.dll exists at all, nothing will haben.
	 * If the japdll.dll has the wrong version, a backup of the old file is created and
	 * the suitable japdll.dll is extracted from the JAP.jar.
	 * In this case the user must restart the JAP.
	 * @param a_bShowDIalogAndCloseOnUpdate if, in case of a neccessary update, a dialog is shown and JAP
	 * is closed
	 */
	public static void checkDllVersion(boolean a_bShowDialogAndCloseOnUpdate)
	{
		if (System.getProperty("os.name", "").toLowerCase().indexOf("win") < 0)
		{
			return;
		}

		LogHolder.log(LogLevel.DEBUG, LogType.GUI, "Existing " + JAP_DLL + " version: " + JAPDll.getDllVersion());
		LogHolder.log(LogLevel.DEBUG, LogType.GUI, "Required " + JAP_DLL + " version: " + JAP_DLL_REQUIRED_VERSION);

		// checks, if the japdll.dll must (and can) be extracted from jar-file.
		/** @todo remove this 'false' to make it run */
		if (false&& JAPDll.getDllVersion() != null && // != null means that there is a loaded dll
			JAPDll.getDllVersion().compareTo(JAP_DLL_REQUIRED_VERSION) < 0 &&
			ResourceLoader.getResourceURL(JAP_DLL_NEW) != null)
		{
			// test if we already tried to update
			if (JAPModel.getInstance().getDLLupdate())
			{
				if (a_bShowDialogAndCloseOnUpdate)
				{
					String[] args = new String[2];
					args[0] = "'" + JAP_DLL + "'";
					args[1] = "'" + UPDATE_PATH + "'";
					if (a_bShowDialogAndCloseOnUpdate)
					{
						int answer =
							JAPDialog.showConfirmDialog(JAPController.getView(),
							JAPMessages.getString(MSG_DLL_UPDATE_FAILED, args),
							JAPMessages.getString(JAPDialog.MSG_TITLE_ERROR),
							JAPDialog.OPTION_TYPE_YES_NO, JAPDialog.MESSAGE_TYPE_ERROR,
							new JAPDialog.LinkedHelpContext(JAPDll.class.getName()));
						/** @todo if yes, show a file dialog to extract and save the dll */
					}
				}
				return;
			}

			if (update() && JAPDll.getDllVersion() != null && // == null means that there are problems...
				JAPDll.getDllVersion().compareTo(JAP_DLL_REQUIRED_VERSION) < 0)
			{
				// update was successful
				return;
			}

			//write a flag to the jap.conf, that at the next startup the japdll.dll must e extracted from jar-file
			JAPModel.getInstance().setDLLupdate(true);
			JAPController.getInstance().saveConfigFile();
			if (a_bShowDialogAndCloseOnUpdate)
			{
				//Inform the User about the necessary JAP restart
				JAPDialog.showMessageDialog(JAPController.getView(),
											JAPMessages.getString(MSG_DLL_UPDATE, "'" + JAP_DLL + "'"));
				//close JAP
				JAPController.getInstance().goodBye(false);
			}
		}
		else
		{
			// version status OK
			if (JAPModel.getInstance().getDLLupdate())
			{
				JAPModel.getInstance().setDLLupdate(false);
				JAPController.getInstance().saveConfigFile();
			}
		}
	}

	private static boolean update()
	{
		if (renameDLL(JAP_DLL, JAP_DLL_OLD) && extractDLL())
		{
			JAPModel.getInstance().setDLLupdate(false);
			JAPController.getInstance().saveConfigFile();
			return true;
		}
		else
		{
			renameDLL(JAP_DLL_OLD, JAP_DLL);
			return false;
		}
	}

	/**
	 * Renames the existing japdll.dll to japdll.old before the new DLL is extracted from jar-file
	 * @param a_oldName old name of the file
	 * @param a_newName new name of the file
	 */
	private static boolean renameDLL(String a_oldName, String a_newName)
	{
		try
		{
			File file = new File(UPDATE_PATH + a_oldName);
			if(file.exists())
			{
				file.renameTo(new File(UPDATE_PATH + a_newName));
			}
			return true;
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.NOTICE, LogType.GUI, "Unable to rename " + UPDATE_PATH + a_oldName + ".");
		}
		return false;
   }

   /**
	* Extracts the japdll.dll from the jar-file
	*/
   private static boolean extractDLL()
   {
		LogHolder.log(LogLevel.DEBUG, LogType.GUI, "Extracting " + JAP_DLL_NEW + " from jar-file: ");
		File file = new File(UPDATE_PATH + JAP_DLL);
		FileOutputStream fos;

		try
		{
			InputStream is = ResourceLoader.loadResourceAsStream(JAP_DLL_NEW);
			if (is == null)
			{
				return false;
			}
			fos = new FileOutputStream(file);

			int b;
			while (true)
			{
				b = is.read();
				if (b == -1)
				{
					break;
				}
				fos.write(b);
			}
			fos.flush();
			fos.close();
			is.close();
			return true;

		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.EXCEPTION, LogType.MISC, e);
		}
		return false;
	}

	static public boolean setWindowOnTop(Window theWindow, String caption, boolean onTop)
	{
		try
		{ //first we try the new setAlwaysOnTop method of Java 1.5
			Class[] c = new Class[1];
			c[0] = boolean.class;
			Method m = Window.class.getMethod("setAlwaysOnTop", c);
			Object[] args = new Object[1];
			args[0] = new Boolean(onTop);
			m.invoke(theWindow, args);
			return true;
		}
		catch (Throwable t)
		{
		}
		try
		{
			setWindowOnTop_dll(caption, onTop);
			return true;
		}
		catch (Throwable t)
		{
		}
		return false;
	}

	static public boolean hideWindowInTaskbar(String caption)
	{
		try
		{
			return hideWindowInTaskbar_dll(caption);
		}
		catch (Throwable t)
		{
			return false;
		}
	}

	static public boolean setWindowIcon(String caption)
	{
		try
		{
			return setWindowIcon_dll(caption);
		}
		catch (Throwable t)
		{
			return false;
		}
	}

	static public boolean onTraffic()
	{
		if (m_sbHasOnTraffic)
		{
			try
			{
				onTraffic_dll();
				return true;
			}
			catch (Throwable t)
			{
				m_sbHasOnTraffic = false;
				return false;
			}
		}
		return false;
	}

	static public String getDllVersion()
	{
		try
		{
			return getDllVersion_dll();
		}
		catch (Throwable t)
		{
		}
		return null;
	}

	/** Returns the Filename of the JAPDll.
	 * @ret filename pf the JAP dll
	 * @ret null if getting the file name fails
	 */

	static public String getDllFileName()
	{
		try
		{
			String s=getDllFileName_dll();
			if(s==null||s.length()==0)
				return null;
			return s;
		}
		catch (Throwable t)
		{
		}
		return null;
	}

	static public long showMainWindow()
	{
		JAPController.getView().setVisible(true);
		JAPController.getView().toFront();
		return 0;
	}


	native static private void setWindowOnTop_dll(String caption, boolean onTop);

	native static private boolean hideWindowInTaskbar_dll(String caption);

	native static private boolean setWindowIcon_dll(String caption);

	native static private void onTraffic_dll();

	native static private String getDllVersion_dll();

	native static private String getDllFileName_dll();
}
