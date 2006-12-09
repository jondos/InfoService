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

import java.awt.Font;

import javax.swing.JLabel;

/**
 * This class provides support for labels with more than one line which can also display HTML
 * styled text.
 */
public class JAPHtmlMultiLineLabel extends JLabel
{

	/**
	 * Stores the HTML text displayed by this JAPHtmlMultiLineLabel without the header and the
	 * trailer.
	 */
	private String m_rawText;

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
		changeFont(a_defaultFont);
	}

	/**
	 * Changes the default font of the displayed text.
	 *
   * @param a_defaultFont The font to use as the default font for the text (set in the HTML body
   *                      tag). So any part of the text, which is not influenced by special
   *                      modifiers is displayed with this default font. If the specified Font is
   *                      BOLD, the text is also included within a <b> tag.
	 */
	public void changeFont(Font a_defaultFont)
	{
		setFont(a_defaultFont);
		/* set the new font with the HTML default size */
    String header = "<html><body style=\"font-family:" + a_defaultFont.getFamily() + ";font-size:small\">";
		String trailer = "</BODY></HTML>";
		if (a_defaultFont.isBold())
		{
			header = header + "<b>";
			trailer = "</b>" + trailer;
		}
		setText(header + m_rawText + trailer);
	}

	/**
	 * Changes the text displayed by the JAPHtmlMultiLineLabel.
	 *
	 * @param a_text Any HTML 3.2 conform text, which is allowed in the body of an HTML 3.2 structure
	 *               (without the leading and trailing <html> and <body> tags).
	 */
	public void changeText(String a_newText)
	{
		m_rawText = a_newText;
		/* call changeFont() to create the header and trailer of the HTML structure and display the
		 * new text
		 */
		changeFont(getFont());
	}

}