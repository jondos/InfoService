/*
 Copyright (c) 2000 - 2004, The JAP-Team
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
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.Hashtable;
import java.util.Vector;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.MouseEvent;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JTextPane;
import javax.swing.LookAndFeel;
import javax.swing.UIDefaults;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.plaf.FontUIResource;

import anon.util.ClassUtil;
import anon.util.ResourceLoader;
import gui.dialog.JAPDialog;
import gui.dialog.WorkerContentPane;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/**
 * This class contains helper methods for the GUI.
 */
public final class GUIUtils
{
	/**
	 * The default path to store images.
	 */
	public static final String MSG_DEFAULT_IMGAGE_PATH = GUIUtils.class.getName() + "_imagePath";

	/**
	 * Images with a smaller pixes size than 16 bit should be stored in this path. Their names must
	 * be equal to the corresponding images in the default path.
	 */
	public static final String MSG_DEFAULT_IMGAGE_PATH_LOWCOLOR =
		GUIUtils.class.getName() + "_imagePathLowColor";

	private static final String MSG_PASTE_FILE = GUIUtils.class.getName() + "_pasteFile";
	private static final String MSG_COPY_FROM_CLIP = GUIUtils.class.getName() + "_copyFromClip";
	private static final String MSG_SAVED_TO_CLIP = GUIUtils.class.getName() + "_savedToClip";

	private static final int MAXIMUM_TEXT_LENGTH = 60;

	private static boolean ms_loadImages = true;

	private static final IIconResizer DEFAULT_RESIZER = new IIconResizer()
	{
		public double getResizeFactor()
		{
			return 1.0;
		}
	};
	private static IIconResizer ms_resizer = DEFAULT_RESIZER;

	private static final NativeGUILibrary DUMMY_GUI_LIBRARY = new NativeGUILibrary()
	{
		public boolean setAlwaysOnTop(Window a_window, boolean a_bOnTop)
		{
			return false;
		}

		public boolean isAlwaysOnTop(Window a_window)
		{
			return false;
		}
	};
	private static NativeGUILibrary ms_nativeGUILibrary = DUMMY_GUI_LIBRARY;

	private static final IIconResizer RESIZER = new IIconResizer()
	{
		public double getResizeFactor()
		{
			return ms_resizer.getResizeFactor();
		}
	};



	// all loaded icons are stored in the cache and do not need to be reloaded from file
	private static Hashtable ms_iconCache = new Hashtable();

	public static interface NativeGUILibrary
	{
		public boolean setAlwaysOnTop(Window a_window, boolean a_bOnTop);

		public boolean isAlwaysOnTop(Window a_window);
	}

	/**
	 * Defines a resize factor for icons that is especially useful if the font size is altered.
	 */
	public static interface IIconResizer
	{
		/**
		 * 1.0 means no resizing is done
		 * @return 1.0 means 100%
		 */
		public double getResizeFactor();
	}

	public static final IIconResizer getIconResizer()
	{
		return RESIZER;
	}

	/**
	 * Stops loading of images, e.g. because of an update of the parent JAR file.
	 */
	public static void setLoadImages(boolean a_bLoadImages)
	{
		if (ms_loadImages && !a_bLoadImages)
		{
			LogHolder.log(LogLevel.NOTICE, LogType.GUI, "Loading of images has been stopped!");
		}
		ms_loadImages = a_bLoadImages;
	}

	public static boolean isLoadingImagesStopped()
	{
		return !ms_loadImages;
	}

	public static final void setIconResizer(IIconResizer a_resizer)
	{
		if (a_resizer != null)
		{
			ms_resizer = a_resizer;
		}
		else
		{
			ms_resizer = DEFAULT_RESIZER;
		}
	}

	/**
	 * Loads an ImageIcon from the classpath or the current directory.
	 * The icon may be contained in an archive (JAR) or a directory structure. If the icon could
	 * not be found in the classpath, it is loaded from the current directory.
	 * If even the current directory does not contain the icon, it is loaded from the default image path.
	 * Once an icon is loaded, it is stored in a memory cache, so that further calls of this method
	 * do not load the icon from the file system, but from the cache.
	 * @param a_strRelativeImagePath the relative resource path or filename of the Image
	 * @return the loaded ImageIcon or null if the icon could not be loaded
	 *         (getImageLoadStatus() == java.awt.MediaTracker.ERRORED)
	 */
	public static ImageIcon loadImageIcon(String a_strRelativeImagePath)
	{
		return loadImageIcon(a_strRelativeImagePath, true, true);
	}

	/**
	 * Loads an ImageIcon from the classpath or the current directory.
	 * The icon may be contained in an archive (JAR) or a directory structure. If the icon could
	 * not be found in the classpath, it is loaded from the current directory.
	 * If even the current directory does not contain the icon, it is loaded from the default image path.
	 * Once an icon is loaded, it is stored in a memory cache, so that further calls of this method
	 * do not load the icon from the file system, but from the cache.
	 * The image may be loaded synchronously so that the method only returns when the image has been
	 * loaded completely (or an error occured), or asynchronously so that the method returns even if
	 * the image has not been loaded yet.
	 * @param a_strRelativeImagePath the relative resource path or filename of the Image
	 * @param a_bSync true if the image should be loaded synchronously; false otherwise
	 * @return the loaded ImageIcon or null if the icon could not be loaded
	 *         (getImageLoadStatus() == java.awt.MediaTracker.ERRORED)
	 */
	public static ImageIcon loadImageIcon(String a_strRelativeImagePath, boolean a_bSync)
	{
		return loadImageIcon(a_strRelativeImagePath, a_bSync, true);
	}


	/**
	 * Loads an ImageIcon from the classpath or the current directory.
	 * The icon may be contained in an archive (JAR) or a directory structure. If the icon could
	 * not be found in the classpath, it is loaded from the current directory.
	 * If even the current directory does not contain the icon, it is loaded from the default image path.
	 * Once an icon is loaded, it is stored in a memory cache, so that further calls of this method
	 * do not load the icon from the file system, but from the cache.
	 * The image may be loaded synchronously so that the method only returns when the image has been
	 * loaded completely (or an error occured), or asynchronously so that the method returns even if
	 * the image has not been loaded yet.
	 * @param a_strRelativeImagePath the relative resource path or filename of the Image
	 * @param a_bSync true if the image should be loaded synchronously; false otherwise
	 * @param a_bScale if the icon should be auto-scaled
	 * @return the loaded ImageIcon or null if the icon could not be loaded
	 *         (getImageLoadStatus() == java.awt.MediaTracker.ERRORED)
	 */
	public static ImageIcon loadImageIcon(String a_strRelativeImagePath, boolean a_bSync, boolean a_bScale)
	{
		ImageIcon img;
		int statusBits;

		// try to load the image from the cache
		if (ms_iconCache.containsKey(a_strRelativeImagePath))
		{
			img = new ImageIcon((Image)ms_iconCache.get(a_strRelativeImagePath));
		}
		else if (ms_loadImages)
		{
			// load image from the local classpath or the local directory
			img = loadImageIconInternal(ResourceLoader.getResourceURL(a_strRelativeImagePath));

			if (img == null && (Toolkit.getDefaultToolkit().getColorModel().getPixelSize() <= 16))
			{
				// load the image from the low color image path
				img = loadImageIconInternal(
					ResourceLoader.getResourceURL(
						JAPMessages.getString(MSG_DEFAULT_IMGAGE_PATH_LOWCOLOR) + a_strRelativeImagePath));
			}

			if (img == null || img.getImageLoadStatus() == MediaTracker.ERRORED)
			{
				// load the image from the default image path
				img = loadImageIconInternal(
					ResourceLoader.getResourceURL(
						JAPMessages.getString(MSG_DEFAULT_IMGAGE_PATH) + a_strRelativeImagePath));
			}

			if (img != null)
			{
				if (a_bSync)
				{
					statusBits = MediaTracker.ABORTED | MediaTracker.ERRORED | MediaTracker.COMPLETE;
					while ( (img.getImageLoadStatus() & statusBits) == 0)
					{
						Thread.yield();
					}
				}

				// write the image to the cache
				ms_iconCache.put(a_strRelativeImagePath, img.getImage());
			}

			statusBits = MediaTracker.ABORTED | MediaTracker.ERRORED;
			if (img == null || (img.getImageLoadStatus() & statusBits) != 0)
			{
				LogHolder.log(LogLevel.INFO, LogType.GUI,
							  "Could not load requested image '" + a_strRelativeImagePath + "'!");
			}
		}
		else
		{
			img = null;
		}
		if (a_bScale && ms_loadImages)
		{
			final ImageIcon image = img;
			WorkerContentPane.IReturnRunnable run = new WorkerContentPane.IReturnRunnable()
			{
				private ImageIcon m_icon;
				public void run()
				{
					m_icon = GUIUtils.createScaledImageIcon(image, ms_resizer);
				}

				public Object getValue()
				{
					return m_icon;
				}
			};
			Thread thread = new Thread(run);
			thread.setDaemon(true);
			thread.start();
			try
			{
				thread.join(1000);
			}
			catch (InterruptedException ex)
			{
				// ignore
			}
			while (thread.isAlive())
			{
				thread.interrupt();
				try
				{
					thread.join();
				}
				catch (InterruptedException a_e)
				{
					// ignore
				}
			}
			if (run.getValue() != null)
			{
				return (ImageIcon)run.getValue();
			}
			if (img != null && run.getValue() == null)
			{
				LogHolder.log(LogLevel.ERR, LogType.GUI, "Interrupted while scaling image icon!");
			}
		}
		return img;
	}

	private static ImageIcon loadImageIconInternal(URL a_imageURL)
	{
		try
		{
			return new ImageIcon(a_imageURL);
		}
		catch (NullPointerException a_e)
		{
			return null;
		}
	}




	/**
	 * Finds the first parent that is a window.
	 * @param a_component a Component
	 * @return the first parent that is a window (may be the component itself) or the
	 * default frame if no parent window was found
	 */
	public static Window getParentWindow(Component a_component)
	{
		Component component = a_component;

		if (component == null)
		{
			// no component given; get the default frame instead
			component = new JOptionPane().createDialog(component, "").getParent();
		}

		while (component != null && ! (component instanceof Window))
		{

			component = component.getParent();
		}

		return (Window)component;
	}


	/**
	 * Positions a window on the screen relative to a parent window so that its position is optimised.
	 * @param a_window a Window
	 * @param a_parent the Window's parent window
	 */
	public static void positionRightUnderWindow(Window a_window, Window a_parent)
	{
		if (a_window == null || a_parent == null)
		{
			return;
		}
		Dimension parentSize = a_parent.getSize();
		Dimension ownSize = a_window.getSize();
		Point parentLocation = a_parent.getLocationOnScreen();
		a_window.setLocation(parentLocation.x + (parentSize.width / 2) - (ownSize.width / 2),
							 parentLocation.y + 40);
    }

	/**
	 * Moves the given Window to the upright corner of the default screen.
	 * @param a_window a Window
	 */
	public static void moveToUpRightCorner(Window a_window)
	{
		Rectangle screenBounds = getScreenBounds(a_window);
		Dimension ownSize = a_window.getSize();
		a_window.setLocation( (screenBounds.width - ownSize.width), 0);
	}

	public static void setNativeGUILibrary(NativeGUILibrary a_library)
	{
		if (a_library != null)
		{
			ms_nativeGUILibrary = a_library;
		}
	}

	/**
	 * Returns if the alwaysOnTop method of JRE 1.5 is set on a given Window.
	 * @param a_Window a Window
	 * @return if the alwaysOnTop method of JRE 1.5 is set on a given Window
	 */
	public static boolean isAlwaysOnTop(Window a_Window)
	{
		try
		{
			Method m = Window.class.getMethod("isAlwaysOnTop", new Class[0]);
			return ( (Boolean) m.invoke(a_Window, new Object[0])).booleanValue();
		}
		catch (Throwable t)
		{
		}
		return ms_nativeGUILibrary.isAlwaysOnTop(a_Window);
	}

	/**
	 * Tries to use the method setAlwaysOnTop of JRE 1.5.
	 * @param a_Window Window
	 * @param a_bOnTop boolean
	 * @return if the method setAlwaysOnTop could be called with the given arguments
	 */
	public static boolean setAlwaysOnTop(Window a_Window, boolean a_bOnTop)
	{
		try
		{
			Class[] c = new Class[1];
			c[0] = boolean.class;
			Method m = Window.class.getMethod("setAlwaysOnTop", c);
			Object[] args = new Object[1];
			args[0] = new Boolean(a_bOnTop);
			m.invoke(a_Window, args);
			return true;
		}
		catch (Throwable t)
		{
		}
		return ms_nativeGUILibrary.setAlwaysOnTop(a_Window, a_bOnTop);
	}

	/**
	 * Returns the bounds of the screen where a specified window is currently shown.
	 * @param a_window a Window
	 * @return the bounds of the screen where the specified window is currently shown
	 */
	public static Rectangle getScreenBounds(Window a_window)
	{
		Rectangle screenBounds;

		try
		{
			// try to center the window on the default screen; useful if there is more than one screen
			Object graphicsEnvironment =
				Class.forName("java.awt.GraphicsEnvironment").getMethod(
						"getLocalGraphicsEnvironment", null).invoke(null, null);
			Object graphicsDevice = graphicsEnvironment.getClass().getMethod(
				 "getDefaultScreenDevice", null).invoke(graphicsEnvironment, null);
			Object graphicsConfiguration = graphicsDevice.getClass().getMethod(
				"getDefaultConfiguration", null).invoke(graphicsDevice, null);
			screenBounds = (Rectangle)graphicsConfiguration.getClass().getMethod(
				 "getBounds", null).invoke(graphicsConfiguration, null);
		}
		catch(Exception a_e)
		{
			// not all methods to get the default screen are available in JDKs < 1.3
			screenBounds = new Rectangle(new Point(0,0), a_window.getToolkit().getScreenSize());
		}
		return screenBounds;
	}


	/**
	 * Centers a window relative to the screen.
	 * @param a_window a Window
	 */
	public static void centerOnScreen(Window a_window)
	{
		Rectangle screenBounds = getScreenBounds(a_window);
		Dimension ownSize = a_window.getSize();

		a_window.setLocation(screenBounds.x + ((screenBounds.width - ownSize.width) / 2),
							 screenBounds.y + ((screenBounds.height - ownSize.height) / 2));
	}

	/**
	 * Positions a window on the screen centered to a parent window.
	 * @param a_window a Window
	 * @param a_parent the Window's parent window
	 */
	public static void centerOnWindow(Window a_window, Window a_parent)
	{
		if (a_window == null || a_parent == null)
		{
			return;
		}
		Dimension parentSize = a_parent.getSize();
		Dimension ownSize = a_window.getSize();
		Point parentLocation = a_parent.getLocationOnScreen();
		a_window.setLocation(parentLocation.x + (parentSize.width / 2) - (ownSize.width / 2),
							 parentLocation.y + (parentSize.height / 2) - (ownSize.height / 2));
	}

	/**
	 * Creates a JTextPane that may be used to simulate a selectable and resizeable JLabel.
	 * If you do not want the label to be selectable, you may set <i>enabled<i> to <i>false<i>.
	 * @param a_parent Component
	 * @return JTextPane
	 */
	public static JTextPane createSelectableAndResizeableLabel(Component a_parent)
	{
		Font jlFont;
		JTextPane selectableLabel = new JTextPane();
		selectableLabel.setBackground(a_parent.getBackground());
		selectableLabel.setEditable(false);
		selectableLabel.setDisabledTextColor(selectableLabel.getCaretColor());
		jlFont = new JLabel().getFont();
		selectableLabel.setFont(new Font(jlFont.getName(),Font.BOLD, jlFont.getSize()));
		return selectableLabel;
	}

	/**
	 * Tests which mouse button was the cause for the specified MouseEvent.
	 * Use the button masks from MouseEvent.
	 * @param a_event a MouseEvent
	 * @param a_buttonMask a button mask from MouseEvent
	 * @return if the event was triggered by the given mouse button
	 * @see java.awt.event.MouseEvent
	 */
	public static boolean isMouseButton(MouseEvent a_event, int a_buttonMask)
	{
		return ((a_event.getModifiers() & a_buttonMask) == a_buttonMask);
	}

	/**
	 * Returns the system-wide clipboard.
	 * @return the system-wide clipboard Clipboard
	 */
	public static Clipboard getSystemClipboard()
	{
		Clipboard r_cb = null;

		try
		{
			Method getSystemSelection = Toolkit.class.getMethod("getSystemSelection", new Class[0]);
			r_cb = (Clipboard) getSystemSelection.invoke(Toolkit.getDefaultToolkit(), new Object[0]);
		}
		catch (NoSuchMethodException nsme)
		{
			// JDK < 1.4 does not support getSystemSelection
		}
		catch (IllegalAccessException iae)
		{
			// this should not happen
		}
		catch (InvocationTargetException ite)
		{
			// this should not happen
		}

		// alternate way of retrieving the clipboard
		if (r_cb == null)
		{
			r_cb = Toolkit.getDefaultToolkit().getSystemClipboard();
		}
		return r_cb;
	}



	/**
	 * Registers all instanciable subclasses of javax.swing.LookAndFeel from a file in the UIManager.
	 * @return the files that contain the newly loaded look&feel classes
	 */
	public static Vector registerLookAndFeelClasses(File a_file) throws IllegalAccessException
	{
		if (a_file == null)
		{
			return new Vector();
		}

		LookAndFeelInfo lnfOldInfo[] = UIManager.getInstalledLookAndFeels();
		LookAndFeelInfo lnfNewInfo[];
		LookAndFeel lnf;
		Vector oldFiles = new Vector(lnfOldInfo.length);
		Vector newFiles;
		File file;
		for (int i = 0; i < lnfOldInfo.length; i++)
		{
			file = ClassUtil.getClassDirectory(lnfOldInfo[i].getClassName());
			if (file != null)
			{
				oldFiles.addElement(file);
			}
		}

		ClassUtil.addFileToClasspath(a_file);
		ClassUtil.loadClasses(a_file);

		Vector tempLnfClasses = ClassUtil.findSubclasses(LookAndFeel.class);
		for (int i = 0; i < tempLnfClasses.size(); i++)
		{
			try
			{
				lnf = (LookAndFeel)( (Class) tempLnfClasses.elementAt(i)).newInstance();
			}
			catch (IllegalAccessException ex)
			{
				continue;
			}
			catch (InstantiationException ex)
			{
				continue;
			}
			catch (ClassCastException a_e)
			{
				continue;
			}
			try
			{

				if (lnf.isSupportedLookAndFeel())
				{
					LookAndFeelInfo installed[] = UIManager.getInstalledLookAndFeels();
					boolean bInstalled = false;
					for (int j = 0; j < installed.length; j++)
					{
						if (installed[j].getClassName().equals(lnf.getClass().getName()))
						{
							// this theme has been previously installed
							bInstalled = true;
						}
					}
					if (!bInstalled)
					{
						UIManager.installLookAndFeel(lnf.getName(), lnf.getClass().getName());
					}
				}
			}
			catch (Throwable a_e)
			{
				continue;
			}
		}
		lnfNewInfo = UIManager.getInstalledLookAndFeels();
		if (lnfNewInfo.length > lnfOldInfo.length)
		{
			newFiles = new Vector(lnfNewInfo.length - lnfOldInfo.length);
			for (int i = 0; i < lnfNewInfo.length; i++)
			{
				file = ClassUtil.getClassDirectory(lnfNewInfo[i].getClassName());
				if (!oldFiles.contains(file))
				{
					newFiles.addElement(file);
				}
			}
		}
		else
		{
			newFiles = new Vector();
		}
		return newFiles;
	}

	/**
	 * Resizes all fonts of the UIManager by a fixed factor.
	 * @param a_resize  the factor to resize the fonts
	 */
	public static void resizeAllFonts(float a_resize)
	{
		java.util.Enumeration keys = UIManager.getDefaults().keys();
		while (keys.hasMoreElements())
		{
			Object key = keys.nextElement();
			Object value = UIManager.get (key);
			if (value instanceof FontUIResource)
			{
				adjustFontSize(key.toString(), a_resize);
			}
		}
	}

	public static String getTextFromClipboard(Component a_requestingComponent)
	{
		return getTextFromClipboard(a_requestingComponent, true);
	}

	public static void saveTextToClipboard(String strText, Component a_requestingComponent)
	{
		try
		{
			Clipboard cb = GUIUtils.getSystemClipboard();
			cb.setContents(new StringSelection(strText),
						   new ClipboardOwner()
			{
				public void lostOwnership(Clipboard cb, Transferable co)
				{
					// Don't care.
				}
			});

			if (strText.equals(getTextFromClipboard(a_requestingComponent, false)))
			{
				JAPDialog.showMessageDialog(a_requestingComponent, JAPMessages.getString(MSG_SAVED_TO_CLIP));
				return;
			}
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.NOTICE, LogType.GUI, e);
		}

		// There are some problems with the access of the
		// clipboard, so after the try to copy it, we
		// still offer the ClipFrame.
		ClipFrame cf =
			new ClipFrame(a_requestingComponent, JAPMessages.getString(MSG_COPY_FROM_CLIP), false);
		cf.setText(strText);
		cf.setVisible(true, false);
	}

	public static ImageIcon createScaledImageIcon(ImageIcon a_icon, IIconResizer a_resizer)
	{
		if (a_icon == null)
		{
			return null;
		}
		if (a_resizer == null)
		{
			return a_icon;
		}
		return  new ImageIcon(a_icon.getImage().getScaledInstance(
			  (int) (a_icon.getIconWidth() * a_resizer.getResizeFactor()), -1, Image.SCALE_REPLICATE));
	}

	public static Icon createScaledIcon(Icon a_icon, IIconResizer a_resizer)
	{
		if (a_icon == null)
		{
			return a_icon;
		}

		return new IconScaler(a_icon, a_resizer.getResizeFactor());
	}

	/**
	 * Shortens a text received from the IS or in a certificate so that it is not to long to display.
	 * @param a_strOriginal String
	 * @param a_maximumLength the maximum length that is displayed
	 * @return the stripped text
	 */
	public static String trim(String a_strOriginal, int a_maximumLength)
	{
		if (a_strOriginal == null || a_maximumLength < 4)
		{
			return null;
		}
		// remove all html TAGS
		a_strOriginal = JAPHtmlMultiLineLabel.removeTagsAndNewLines(a_strOriginal);
		if (a_strOriginal.length() > a_maximumLength)
		{
			a_strOriginal = a_strOriginal.substring(0, a_maximumLength - 2) + "...";
		}
		return a_strOriginal;
	}

	/**
	 * Shortens a text received from the IS or in a certificate so that it is not to long to display.
	 * @param a_strOriginal String
	 * @return the stripped text
	 */
	public static String trim(String a_strOriginal)
	{
		return trim(a_strOriginal, MAXIMUM_TEXT_LENGTH);
	}


	private static String getTextFromClipboard(Component a_requestingComponent, boolean a_bUseTextArea)
	{
		Clipboard cb = getSystemClipboard();
		String strText = null;

		Transferable data = cb.getContents(a_requestingComponent);
		if (data != null && data.isDataFlavorSupported(DataFlavor.stringFlavor))
		{
			try
			{
				strText = (String) data.getTransferData(DataFlavor.stringFlavor);
			}
			catch (Exception a_e)
			{
				LogHolder.log(LogLevel.NOTICE, LogType.GUI, a_e);
			}
		}

		if (a_bUseTextArea && strText == null)
		{
			ClipFrame cf =
				new ClipFrame(a_requestingComponent, JAPMessages.getString(MSG_PASTE_FILE), true);
			cf.setVisible(true, false);
			strText = cf.getText();
		}
		return strText;
	}

	/**
	 * Resizes a specific default font of the UIManager by a fixed factor.
	 * @param a_fontObject a UIManager font object
	 * @param a_resize the factor to resize the given font
	 */
	private static void adjustFontSize(Object a_fontObject, float a_resize)
	{
		try
		{
			UIDefaults defaults = UIManager.getDefaults();
			Font font = defaults.getFont(a_fontObject);
			//defaults.put(a_fontObject, new FontUIResource(font.deriveFont(font.getSize() * a_resize)));
			defaults.put(a_fontObject, new FontUIResource(
						 font.getName(), font.getStyle(), (int)(Math.round(font.getSize() * a_resize))));
		}
		catch (Exception a_e)
		{
			LogHolder.log(LogLevel.ERR, LogType.GUI, a_e);
		}
	}

	/** * Diese Klasse dient dazu aus einem vorhandenen Icon ein neues Icon
	 * herzustellen. Dazu werden neben dem vorhanden Icon die Skalierungsfaktoren angegeben.
	 */
	private static class IconScaler implements Icon
	{
		private static Class GRAPHICS_2D;

		static
		{
			try
			{
				GRAPHICS_2D = Class.forName("java.awt.Graphics2D");
			}
			catch (ClassNotFoundException a_e)
			{
				GRAPHICS_2D = null;
			}
		}

		private Icon m_icon;
		private double m_scaleWidth;
		private double m_scaleHeight;


		/**
		 * Creates a new Icon that scales a given Icon with the given settings.
		 */
		public IconScaler(Icon icon, double a_scale)
		{
			this(icon, a_scale, a_scale);
		}

		public IconScaler(Icon icon, double a_scaleWidth, double a_scaleHeight)
		{
			m_icon = icon;
			if (GRAPHICS_2D != null)
			{
				m_scaleWidth = a_scaleWidth;
				m_scaleHeight = a_scaleHeight;
			}
			else
			{
				m_scaleWidth = 1.0;
				m_scaleHeight = 1.0;
			}
		}

		public int getIconHeight()
		{
			return (int) (m_icon.getIconHeight() * m_scaleHeight);
		}

		public int getIconWidth()
		{
			return (int) (m_icon.getIconWidth() * m_scaleWidth);
		}

		public void paintIcon(Component c, Graphics g, int x, int y)
		{
			scale(g, m_scaleWidth, m_scaleHeight);
			m_icon.paintIcon(c, g, x, y);
			scale(g, 1.0 / m_scaleWidth, 1.0 / m_scaleHeight);
		}
		private static void scale(Graphics a_graphics, double a_scaleWidth, double a_scaleHeight)
		{
			if (GRAPHICS_2D != null)
			{
				try
				{
					GRAPHICS_2D.getMethod("scale", new Class[]
										  {double.class, double.class}).invoke(
											  a_graphics,
											  new Object[]
											  {new Double(a_scaleWidth), new Double(a_scaleHeight)});
				}
				catch (Exception a_e)
				{
					LogHolder.log(LogLevel.ERR, LogType.GUI, a_e);
				}
				//Graphics2D g2 = (Graphics2D) g;
				//g2.scale(m_scaleWidth, m_scaleHeight);
			}
		}
	}
}
