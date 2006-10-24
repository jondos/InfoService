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
import java.util.Hashtable;
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;

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
	private static final String JAP_DLL_REQUIRED_VERSION = "00.02.003";
	private static final String UPDATE_PATH =
		ClassUtil.getClassDirectory(JAPDll.class).getParent() + File.separator;

	private static final String DLL_LIBRARY_NAME = "japdll";
	private static final String JAP_DLL     = DLL_LIBRARY_NAME + ".dll";
	private static final String JAP_DLL_NEW  = JAP_DLL + "." + JAP_DLL_REQUIRED_VERSION;
	private static final String JAP_DLL_OLD = DLL_LIBRARY_NAME + ".old";

	/** Messages */
	private static final String MSG_DLL_UPDATE = JAPDll.class.getName() + "_updateRestartMessage";
	private static final String MSG_DLL_UPDATE_FAILED = JAPDll.class.getName() + "_updateFailed";
	private static final String MSG_CONFIRM_OVERWRITE = JAPDll.class.getName() + "_confirmOverwrite";
	private static final String MSG_PERMISSION_PROBLEM = JAPDll.class.getName() + "_permissionProblem";
	private static final String MSG_COULD_NOT_SAVE = JAPDll.class.getName() + "_couldNotSave";


	private static Hashtable ms_hashOnTop = new Hashtable();

	private static boolean m_sbHasOnTraffic = true;
	public static void init()
	{
		try
		{
			if (System.getProperty("os.name", "").toLowerCase().indexOf("win") > -1)
			{
				GUIUtils.setNativeGUILibrary(new GUIUtils.NativeGUILibrary()
				{
					public boolean setAlwaysOnTop(Window a_window, boolean a_bOnTop)
					{
						return setWindowOnTop(a_window, a_bOnTop);
					}

					public boolean isAlwaysOnTop(Window a_window)
					{
						return isWindowOnTop(a_window);
					}

				});

				boolean bUpdateDone = false;
				if ( JAPModel.getInstance().getDLLupdate())
				{
					update();
					bUpdateDone = true;
				}
				System.loadLibrary(DLL_LIBRARY_NAME);
				if (bUpdateDone && (JAPDll.getDllVersion() == null || // == null means there were problems...
									JAPDll.getDllVersion().compareTo(JAP_DLL_REQUIRED_VERSION) < 0))
				{
					// update was not successful
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
	 * proper with this JAP version. If no japdll.dll exists at all, nothing will happen.
	 * If the japdll.dll has the wrong version, a backup of the old file is created and
	 * the suitable japdll.dll is extracted from the JAP.jar.
	 * In this case the user must restart the JAP.
	 * @param a_bShowDialogAndCloseOnUpdate if, in case of a neccessary update, a dialog is shown and JAP
	 * is closed
	 */
	public static void checkDllVersion(boolean a_bShowDialogAndCloseOnUpdate)
	{
		if (System.getProperty("os.name", "").toLowerCase().indexOf("win") < 0)
		{
			return;
		}

		LogHolder.log(LogLevel.INFO, LogType.GUI, "Existing " + JAP_DLL + " version: " + JAPDll.getDllVersion());
		LogHolder.log(LogLevel.INFO, LogType.GUI, "Required " + JAP_DLL + " version: " + JAP_DLL_REQUIRED_VERSION);


		// checks, if the japdll.dll must (and can) be extracted from jar-file.
		if (JAPDll.getDllVersion() != null && // != null means that there is a loaded dll
			JAPDll.getDllVersion().compareTo(JAP_DLL_REQUIRED_VERSION) < 0 &&
			ResourceLoader.getResourceURL(JAP_DLL_NEW) != null) // null means there is no new dll available
		{

			// check, if NO japdll.dll exists in jar-path
			File file = new File(UPDATE_PATH + JAP_DLL);
			if (!file.exists())
			{
				askUserWhatToDo();
				return;
			}

			// tried to updated AND there is still a problem
			if (JAPModel.getInstance().getDLLupdate())
			{
				if (a_bShowDialogAndCloseOnUpdate)
				{
					askUserWhatToDo();
				}
				return;
			}

			// try to update, perhaps it even works right now when the dll is loaded
			if (update() && JAPDll.getDllVersion() != null && // == null means that there were problems...
				JAPDll.getDllVersion().compareTo(JAP_DLL_REQUIRED_VERSION) < 0)
			{
				// update was successful
				LogHolder.log(LogLevel.INFO, LogType.GUI,
							  "Update successful, existing " + JAP_DLL + " version: " + JAPDll.getDllVersion());
				System.loadLibrary(DLL_LIBRARY_NAME);
				if ( JAPDll.getDllVersion().compareTo(JAP_DLL_REQUIRED_VERSION) < 0 )
				{
					JAPModel.getInstance().setDLLupdate(true);
					JAPController.getInstance().saveConfigFile();
					informUserAboutJapRestart();

				}
				else
				{
					return;
				}
			}

			//write a flag to the jap.conf, that at the next startup the dll must be extracted from jar-file
			JAPModel.getInstance().setDLLupdate(true);
			JAPController.getInstance().saveConfigFile();
			if (a_bShowDialogAndCloseOnUpdate)
			{
				informUserAboutJapRestart();
			}
		}
		else
		{
			// version status OK
			// OR no dll loaded
			// OR no new-dll in jar-file
			if (JAPModel.getInstance().getDLLupdate())
			{
				JAPModel.getInstance().setDLLupdate(false);
				JAPController.getInstance().saveConfigFile();
			}
		}
	}

	private static boolean update()
	{
		if (renameDLL(JAP_DLL, JAP_DLL_OLD) &&
			extractDLL(new File(UPDATE_PATH + JAP_DLL )))
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
				return true;
			}
			else {
				//if the file dose not exist, but a dll was loaded
				return false;
			}

		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.NOTICE, LogType.GUI, "Unable to rename " + UPDATE_PATH + a_oldName + ".");
		}
		return false;
   }

   /**
	* Extracts the japdll.dll from the jar-file to the given file
	* @param a_file File
	* @return boolean
	*/
   private static boolean extractDLL(File a_file)
   {
		LogHolder.log(LogLevel.DEBUG, LogType.GUI, "Extracting " + JAP_DLL_NEW + " from jar-file: ");
		FileOutputStream fos;

		try
		{
			InputStream is = ResourceLoader.loadResourceAsStream(JAP_DLL_NEW);
			if (is == null)
			{
				return false;
			}
			fos = new FileOutputStream(a_file);

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
			//unable to write to file, perhaps a permissions problem
			//JAPDialog.showMessageDialog( JAPController.getView(),
			//JAPMessages.getString(MSG_PERMISSION_PROBLEM, "'" + a_file + "'") );
		}
		return false;
	}

	/**
	 * This method should be invoked if there was no possibility to so a successfull dll update:
	 * Reasons can be:
	 * - japdll.dll cannot be written (e.g. not enough user privileges)
	 * - a japdll.dll is loaded, but there is no japdll.dll in the jar-path (japdll.dll is e.g. in Windows-System-Directory)
	 * - japdll.dll update was successfull, but the version loaded is still the wrong
	 *   (this can be happen, if there is a old japdll.dll in the Windows-System-Directory AND in the jar-path
	 */
	private static void askUserWhatToDo()
	{
		String[] args = new String[2];
		args[0] = "'" + JAP_DLL + "'";
		args[1] = "'" + UPDATE_PATH + "'";
		int answer =
			JAPDialog.showConfirmDialog(JAPController.getInstance().getViewWindow(),
										JAPMessages.getString(MSG_DLL_UPDATE_FAILED, args),
										JAPMessages.getString(JAPDialog.MSG_TITLE_ERROR),
										JAPDialog.OPTION_TYPE_YES_NO, JAPDialog.MESSAGE_TYPE_ERROR,
										new JAPDialog.LinkedHelpContext(JAPDll.class.getName()));

		if ( answer == JAPDialog.RETURN_VALUE_YES )
		{
			chooseAndSave();
		}
	}

	/**
	 * Choose where to save the file and save it
	 */
	private static void chooseAndSave()
	{
		boolean b_extractOK = false;
		final JFileChooser chooser = new JFileChooser();
		chooser.setSelectedFile(new File(UPDATE_PATH + JAP_DLL));
		MyFileFilter filter = new MyFileFilter();
		chooser.setFileFilter(filter);
		int returnVal = chooser.showSaveDialog(JAPController.getInstance().getViewWindow());

		// "OK" is pressed at the file chooser
		if (returnVal == JFileChooser.APPROVE_OPTION)
		{
			try
			{
				File f = chooser.getSelectedFile();
				if (!f.getName().toLowerCase().endsWith(MyFileFilter.DLL_EXTENSION))
				{
					f = new File(f.getParent(), f.getName() + MyFileFilter.DLL_EXTENSION);
				}

				//confirm overwrite if file exists
				if (f.exists())
				{
					int answer = JAPDialog.showConfirmDialog(JAPController.getInstance().getViewWindow(),
						JAPMessages.getString(MSG_CONFIRM_OVERWRITE, "'" + f + "'"),
						JAPDialog.MSG_TITLE_CONFIRMATION,
						JAPDialog.OPTION_TYPE_YES_NO, JAPDialog.MESSAGE_TYPE_WARNING);
					if (answer == JAPDialog.RETURN_VALUE_OK)
					{
						b_extractOK = extractDLL(f);
					}
					else
					{
						b_extractOK = false;
					}
				}
				//if file dose not exist -> extract
				else
				{
					b_extractOK = extractDLL(f);
				}

			}
			catch (Exception e)
			{
				LogHolder.log(LogLevel.EXCEPTION, LogType.MISC, e);
			}
		}

		// "Cancel" is pressed at the file chooser
		else if (returnVal == JFileChooser.CANCEL_OPTION)
		{
			askUserWhatToDo();
			return;
		}

		if (!b_extractOK)
		{
			JAPDialog.showErrorDialog(JAPController.getInstance().getViewWindow(),
									  JAPMessages.getString(MSG_COULD_NOT_SAVE),
									  LogType.MISC);
			chooseAndSave();
		}
   }



	/**
	 * informs the user, that the JAP must be restarted, in order to finish the update
	 */
	private static void informUserAboutJapRestart()
	{
		//Inform the User about the necessary JAP restart
		JAPDialog.showMessageDialog(JAPController.getInstance().getViewWindow(),
									JAPMessages.getString(MSG_DLL_UPDATE, "'" + JAP_DLL + "'"));
		//close JAP
		JAPController.getInstance().goodBye(false);
	}

	/**
	 * Checks if the onTop method of the dll has been used for this window.
	 * @param a_window Window
	 * @return if the onTop method of the dll has been used for this window
	 */
	private static boolean isWindowOnTop(Window a_window)
	{
		if (a_window == null)
		{
			return false;
		}
		return ms_hashOnTop.contains(a_window.getName());
	}

	private static boolean setWindowOnTop(Window theWindow, boolean onTop)
	{
		if (theWindow == null)
		{
			return false;
		}

		//theWindow.setName(Double.toString(Math.random()));
		String caption = theWindow.getName();
		if (caption == null)
		{
			return false;
		}

		try
		{
			synchronized (ms_hashOnTop)
			{
				setWindowOnTop_dll(caption, onTop);
				if (onTop)
				{
					ms_hashOnTop.put(caption, caption);
				}
				else
				{
					ms_hashOnTop.remove(caption);
				}
			}
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

	public static String getDllVersion()
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
		JAPController.getInstance().getViewWindow().setVisible(true);
		JAPController.getInstance().getViewWindow().toFront();
		JAPController.getInstance().getViewWindow().repaint();
		return 0;
	}


	native static private void setWindowOnTop_dll(String caption, boolean onTop);

	native static private boolean hideWindowInTaskbar_dll(String caption);

	native static private boolean setWindowIcon_dll(String caption);

	native static private void onTraffic_dll();

	native static private String getDllVersion_dll();

	native static private String getDllFileName_dll();


	private static class MyFileFilter extends FileFilter
	{
		public static final String DLL_EXTENSION = ".dll";
		private final String ACCOUNT_DESCRIPTION = "JAP dll file (*" + DLL_EXTENSION + ")";

		private int filterType;

		public int getFilterType()
		{
			return filterType;
		}

		public boolean accept(File f)
		{
			return f.isDirectory() || f.getName().endsWith(DLL_EXTENSION);
		}

		public String getDescription()
		{
			return ACCOUNT_DESCRIPTION;
		}
	}

}
