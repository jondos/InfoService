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

import java.awt.Point;
import java.awt.Dimension;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.Component;
import javax.swing.ImageIcon;
import javax.swing.JTextPane;
import javax.swing.JLabel;
import java.awt.Font;

/**
 * This class contains helper methods for the GUI.
 */
public final class GUIUtils
{
	public static final String IMGPATHHICOLOR = "images/";
	public static final String IMGPATHLOWCOLOR = "images/lowcolor/";

	public static final String HTML_OPEN_TAG = "<html>";
	public static final String HTML_CLOSE_TAG = "</html>";
	public static final String BODY_OPEN_TAG = "<body>";
	public static final String BODY_CLOSE_TAG = "</body>";

	/**
	 * Loads an Image from a File or a Resource.
	 * @param strImage the Resource or filename of the Image
	 * @param sync true if the loading is synchron, false if it should be asynchron
	 * @return the loaded image or an empty icon if the image could not be loaded
	 */
	public static ImageIcon loadImageIcon(String strImage, boolean sync)
	{
		ImageIcon img = null;

		// try loading the lowcolor images
		if (Toolkit.getDefaultToolkit().getColorModel().getPixelSize() <= 16)
		{
			img = ImageIconLoader.loadImageIcon(IMGPATHLOWCOLOR + strImage, sync);
		}
		// if loading of lowcolor images was not successful or
		//    we have to load the hicolor images
		if (img == null || img.getImageLoadStatus() == MediaTracker.ERRORED)
		{
			img = ImageIconLoader.loadImageIcon(IMGPATHHICOLOR + strImage, sync);
		}

		return img;
	}

	/**
	 * Centers a window relative to the screen.
	 * @param a_window a Window
	 */
	public static void centerFrame(Window a_window)
	{
		Dimension screenSize = a_window.getToolkit().getScreenSize();
		Dimension ownSize = a_window.getSize();
		a_window.setLocation( (screenSize.width - ownSize.width) / 2,
							 (screenSize.height - ownSize.height) / 2);
	}

	/**
	 * Positions a window on the screen relative to a parent window so that its position is optimised.
	 * @param a_window a Window
	 * @param a_parent the Window's parent window
	 */
	public static void positionWindow(Window a_window, Window a_parent)
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
	 * This method adds HTML and BODY tags to a String and overwrites existing tags of this type.
	 * @param a_HTMLtext a String
	 * @return the String with HTML and BODY tags
	 */
	public static String formatTextAsHTML(String a_HTMLtext, Font a_defaultFont)
	{
		if (a_HTMLtext == null || a_HTMLtext.trim().length() == 0)
		{
			return a_HTMLtext;
		}
		if (a_defaultFont == null)
		{
			a_defaultFont = new JLabel().getFont();
		}

		/* set the new font with the HTML default size */
		// style=\"font-family:" + a_defaultFont.getFamily() + ";font-size:small\"
		String header = HTML_OPEN_TAG  + BODY_OPEN_TAG.substring(0, BODY_OPEN_TAG.length() - 1) +
			" style=\"font-family:" + a_defaultFont.getFamily() + "\">";
		String trailer = BODY_CLOSE_TAG + HTML_CLOSE_TAG;
		if (a_defaultFont.isBold())
		{
			header = header + "<b>";
			trailer = "</b>" + trailer;
		}

		return header + removeHTMLAndBODYTags(a_HTMLtext) + trailer;
	}

	/**
	 * Removes heading and trailing HTML and BODY tags from a String if present.
	 * @param a_HTMLtext a String
	 * @return the String without heading and trailing HTML and BODY tags
	 */
	public static String removeHTMLAndBODYTags(String a_HTMLtext)
	{
		return removeTAG(removeTAG(a_HTMLtext, HTML_OPEN_TAG, HTML_CLOSE_TAG), BODY_OPEN_TAG, BODY_CLOSE_TAG);
	}

	/**
	 * Removes an embracing TAG from a String.
	 * @param a_HTMLtext a String
	 * @param a_openTAG an HTML open tag
	 * @param a_closeTAG the corresponding HTML close TAG
	 * @return the String without the embracing tag
	 */
	private static String removeTAG(String a_HTMLtext, String a_openTAG, String a_closeTAG)
	{
		String strOpenTagWithoutBrace;
		String strTemp;
		int start, stop;

		if (a_HTMLtext == null || (a_HTMLtext = a_HTMLtext.trim()).length() == 0)
		{
			return a_HTMLtext;
		}

		strOpenTagWithoutBrace = a_openTAG.substring(0, a_openTAG.length() - 1);
		strTemp = a_HTMLtext.toLowerCase();

		start = 0;
		stop = a_HTMLtext.length();
		if (strTemp.startsWith(strOpenTagWithoutBrace))
		{
			start = strTemp.indexOf(">");
		}
		if (strTemp.endsWith(a_closeTAG))
		{
			stop -= a_closeTAG.length();
		}
		if ( (start > 0 || stop < a_HTMLtext.length()))
		{
			if (start >= stop)
			{
				a_HTMLtext = "";
			}
			else
			{
				a_HTMLtext = a_HTMLtext.substring(start, stop).trim();
			}
		}

		return a_HTMLtext;
	}

}
