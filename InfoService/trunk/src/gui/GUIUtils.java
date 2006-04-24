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

import java.net.URL;
import java.util.Hashtable;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.Rectangle;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JTextPane;
import javax.swing.JFrame;

import anon.util.ResourceLoader;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import javax.swing.JOptionPane;
import java.awt.event.MouseEvent;
import java.awt.datatransfer.Clipboard;

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

	// all loaded icons are stored in the cache and do not need to be reloaded from file
	private static Hashtable ms_iconCache = new Hashtable();

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
		return loadImageIcon(a_strRelativeImagePath, true);
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
		ImageIcon img;
		int statusBits;

		// try to load the image from the cache
		if (ms_iconCache.containsKey(a_strRelativeImagePath))
		{
			return new ImageIcon((Image)ms_iconCache.get(a_strRelativeImagePath));
		}

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

}
