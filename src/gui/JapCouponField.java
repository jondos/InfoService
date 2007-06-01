/*
 Copyright (c) 2000-2007, The JAP-Team
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

import javax.swing.JTextField;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;

/**
 * specialized version of a JTextField, similar to JAPJIntField
 * differences to regulat text field:
 * - accepts only hexadecimal values (0-9, A-F)
 * - accepts only 4 characters, transfers focus once full
 * - displays characters as uppercase regardless of whether they were entered upper- or lowercase
 *
 * @author Elmar Schraml
 *
 */
public class JapCouponField extends JTextField
{
	private static final int NR_OF_CHARACTERS = 4;
	private static final char[] ACCEPTED_CHARS = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'}; //assumes text has already been made uppercase


	public JapCouponField()
	{
		super(NR_OF_CHARACTERS);
	}

	protected final Document createDefaultModel()
	{
		return new CouponDocument();
	}

	private final class CouponDocument extends PlainDocument
	{
		public void insertString(int offset, String string, AttributeSet attributeSet) throws BadLocationException
		{
			//make everything uppercase
			string = string.toUpperCase();

			//remove chars that are not in ACCEPTED_CHARs
			char[] originalString = string.toCharArray();
			char[] modifiedString = new char[originalString.length];
			int nrOfOkayChars = 0;
			for (int i = 0; i < originalString.length ; i++)
			{
				if ( isCharacterAccepted(originalString[i]) )
				{
					modifiedString[nrOfOkayChars] = originalString[i];
					nrOfOkayChars++;
				}
				else {
					continue; //exclude the char from modifiedString, do not increase nrOfOkayChars to give its space to next accepted char
				}
			}
			string = new String(modifiedString,0,nrOfOkayChars);

			//prevent more than NR_OF_CHARACTERS to be entered
		/* throws exception, needs debugging

			if (getLength()+string.length() > NR_OF_CHARACTERS)
			{
			    String shortenedString = string.substring(0,NR_OF_CHARACTERS);
				string = new String(shortenedString);
            }
        */
			//modifications done, set the string
			super.insertString(offset, string, attributeSet);

			//move on after 4 characters have been entered
			if (getLength() >= NR_OF_CHARACTERS)
			{
				transferFocus();
			}
		}

		private boolean isCharacterAccepted(char charToCheck)
		{
			for (int i = 0; i < ACCEPTED_CHARS.length ; i++)
			{
				if (charToCheck == ACCEPTED_CHARS[i] )
				{
					return true;
				}
			}
			return false;
		}

	}

}
