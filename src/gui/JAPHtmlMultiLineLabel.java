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

import java.util.StringTokenizer;
import java.awt.Font;
import javax.swing.JLabel;
import anon.util.ClassUtil;

/**
 * This class provides support for labels with more than one line which can also display HTML
 * styled text.
 */
public class JAPHtmlMultiLineLabel extends JLabel
{
	public static final String TAG_BREAK = "<br>";
	public static final String TAG_A_OPEN = "<a href=\"\">";
	public static final String TAG_A_CLOSE = "</a>";
	private static final String TAG_HTML_OPEN = "<html>";
	private static final String TAG_HTML_CLOSE = "</html>";
	private static final String TAG_BODY_OPEN = "<body>";
	private static final String TAG_BODY_CLOSE = "</body>";

	/**
	 * Stores the HTML text displayed by this JAPHtmlMultiLineLabel without the header and the
	 * trailer.
	 */
	private String m_rawText;
	private Font m_font;

	/**
   * Creates a new JAPHtmlMultiLineLabel.
	 *
	 * @param a_text Any HTML 3.2 conform text, which is allowed in the body of an HTML 3.2 structure
	 *               (without the leading and trailing <html> and <body> tags).
   * @param a_defaultFont The font to use as the default font for the text (set in the HTML body
   *                      tag). So any part of the text, which is not influenced by special
   *                      modifiers is displayed with this default font. If the specified Font is
   *                      BOLD, the text is also included within a <b> tag.
	 */
	public JAPHtmlMultiLineLabel(String a_text, Font a_defaultFont)
	{
		m_rawText = a_text;
		setFont(a_defaultFont);
	}

	/**
	 * Creates a new JAPHtmlMultiLineLabel.
	 *
	 * @param a_text Any HTML 3.2 conform text, which is allowed in the body of an HTML 3.2 structure
	 *               (without the leading and trailing <html> and <body> tags).
	 */
	public JAPHtmlMultiLineLabel(String a_text)
	{
		this(a_text, null);
	}

	/**
	 * Changes the text displayed by the JAPHtmlMultiLineLabel.
	 *
	 * @param a_newText Any HTML 3.2 conform text, which is allowed in the body of an HTML 3.2 structure
	 *               (without the leading and trailing <html> and <body> tags).
	 */
	public void setText(String a_newText)
	{
		if (JLabel.class.isAssignableFrom(ClassUtil.getCallingClassStatic()) &&
			!JAPHtmlMultiLineLabel.class.isAssignableFrom(ClassUtil.getCallingClassStatic()))
		{
			// other JLabels use this method as expected (important for superclass calls)
			super.setText(a_newText);
		}
		else
		{
			m_rawText = a_newText;
			/* call changeFont() to create the header and trailer of the HTML structure and display the
			 * new text
			 */
			changeFont(getFont());
		}
	}

	public Font getFont()
	{
		if (JLabel.class.isAssignableFrom(ClassUtil.getCallingClassStatic()) &&
			!JAPHtmlMultiLineLabel.class.isAssignableFrom(ClassUtil.getCallingClassStatic()))
		{
			// other JLabels use this method as expected (important for superclass calls)
			return super.getFont();
		}

		return m_font;
	}

	/**
	 * Changes the default font of the displayed text.
	 *
   * @param a_defaultFont The font to use as the default font for the text (set in the HTML body
   *                      tag). So any part of the text, which is not influenced by special
   *                      modifiers is displayed with this default font. If the specified Font is
   *                      BOLD, the text is also included within a <b> tag.
	 */
	public void setFont(Font a_defaultFont)
	{
		if (a_defaultFont == null)
		{
			a_defaultFont = new JLabel().getFont();
		}

		if (JLabel.class.isAssignableFrom(ClassUtil.getCallingClassStatic()) &&
			!JAPHtmlMultiLineLabel.class.isAssignableFrom(ClassUtil.getCallingClassStatic()))
	{
			// other JLabels use this method as expected (important for superclass calls)
			super.setFont(a_defaultFont);
		}
		else
		{
			m_font = a_defaultFont;
			super.setText(formatTextAsHTML(m_rawText, a_defaultFont));
		}
	}

	/**
	 * Changes the default font of the displayed text.
	 *
	 * @param a_defaultFont The font to use as the default font for the text (set in the HTML body
	 *                      tag). So any part of the text, which is not influenced by special
	 *                      modifiers is displayed with this default font. If the specified Font is
	 *                      BOLD, the text is also included within a <b> tag.
	 * @deprecated use <i>setFont(Font)</i> instead
	 */
	public void changeFont(Font a_defaultFont)
	{
		setFont(a_defaultFont);
	}

	/**
	 * Changes the text displayed by the JAPHtmlMultiLineLabel.
	 *
	 * @param a_newText Any HTML 3.2 conform text, which is allowed in the body of an HTML 3.2 structure
	 *               (without the leading and trailing <html> and <body> tags).
	 *  @deprecated use <I>setText(String)</I> instead
	 */
	public void changeText(String a_newText)
	{
		setText(a_newText);
	}


	/**
	 * This method adds HTML and BODY tags to a String and overwrites existing tags of this type.
	 * @param a_HTMLtext a String
	 * @param a_defaultFont a default font for the HTML text
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
		String header = TAG_HTML_OPEN  + TAG_BODY_OPEN.substring(0, TAG_BODY_OPEN.length() - 1) +
			" style=\"font-family:" + a_defaultFont.getFamily() + "\">";
		String trailer = TAG_BODY_CLOSE + TAG_HTML_CLOSE;
		if (a_defaultFont.isBold())
		{
			header = header + "<b>";
			trailer = "</b>" + trailer;
		}

		return header + removeHTMLAndBODYTags(a_HTMLtext) + trailer;
	}

	/**
	 * Automatically removes all tags "<" and ">" and the space between and new lines that are
	 * found in a text.
	 * @param a_text a String
	 * @return the String without tags and new lines
	 */
	public static String removeTagsAndNewLines(String a_text)
	{
		String text;
		String token;
		StringTokenizer tokenizer;
		int indexTagBegin, indexTagEnd;

		text = a_text;
		while (true)
		{
			indexTagBegin = text.indexOf("<");
			indexTagEnd = text.indexOf(">");
			if (indexTagBegin < 0 && indexTagEnd < 0)
			{
				break;
			}

			if (indexTagEnd >= 0 && (indexTagBegin < 0 || indexTagEnd < indexTagBegin))
			{
				// there is a ">" without a corresponding "<"
				indexTagBegin = indexTagEnd;
			}
			else if (indexTagEnd < 0)
			{
				// there is a "<" without a corresponding ">"
					indexTagEnd = indexTagBegin;
			}
			indexTagEnd++;
			if (indexTagEnd >= text.length())
			{
				text = text.substring(0, indexTagBegin);
			}
			else
			{
				text = text.substring(0, indexTagBegin) + text.substring(indexTagEnd, text.length());
			}
		}

	    tokenizer = new StringTokenizer(text, "\t\n\r\f");
		text = "";
		while (tokenizer.hasMoreTokens())
		{
			token = tokenizer.nextToken();
			text += token;
		}

		return text.trim();
	}

	/**
	 * Removes heading and trailing HTML and BODY tags from a String if present.
	 * @param a_HTMLtext a String
	 * @return the String without heading and trailing HTML and BODY tags
	 */
	public static String removeHTMLAndBODYTags(String a_HTMLtext)
	{
		return removeTAG(removeTAG(a_HTMLtext, TAG_HTML_OPEN, TAG_HTML_CLOSE), TAG_BODY_OPEN, TAG_BODY_CLOSE);
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
