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

import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;
import java.text.MessageFormat;
import java.awt.Frame;

import anon.util.Util;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;


public final class JAPMessages
{
	private static ResourceBundle msg = null;

	private JAPMessages()
	{
	}

	/* Initalize with the System default Locale...*/
	public static void init(String a_resourceBundleFilename)
	{
		// Load Texts for Messages and Windows
		init(Locale.getDefault(), a_resourceBundleFilename);
	}

	/* Init with the specified Locale**/
	public static void init(Locale locale, String a_resourceBundleFilename)
	{
		// Load Texts for Messages and Windows
		try
		{
			msg = PropertyResourceBundle.getBundle(a_resourceBundleFilename, locale);
		}
		catch (Exception e1)
		{
			try
			{
				msg = PropertyResourceBundle.getBundle(a_resourceBundleFilename);
			}
			catch (Exception e)
			{
				JAPAWTMsgBox.MsgBox(new Frame(),
									"File not found: " + a_resourceBundleFilename +
									".properties\nYour package of JAP may be corrupted.\n"+
									"Try again to download or install the package.",
									"Error");
				System.exit( -1);
			}
		}
	}

	/**
	 * Gets the localised String for a given key.
	 * @param a_key a key for the localised String
	 * @return the localised String
	 */
	public static String getString(String a_key)
	{
		try
		{
			String s=msg.getString(a_key);
			if(s==null||s.length()==0)
				return a_key;
			return s;
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.INFO, LogType.GUI, "Could not load messsage string: " + a_key, true);
			return a_key;
		}
	}

	/**
	 * Gets the localised String for a given key. If the String contains formatting patterns,
	 * these patterns are replaced by the corresponding arguments given in an object array.
	 * For a detailed description of the formatting options please see class
	 * <code> java.text.MessageFormat </code>.
	 * @param a_key a key for the localised String
	 * @param a_arguments an object array that contains the objects that replace
	 * @return the localised String with inserted arguments
	 * @see java.text.MessageFormat
	 */
	public static String getString(String a_key, Object[] a_arguments)
	{
		return MessageFormat.format(getString(a_key), a_arguments);
	}

	/**
	 * Gets the localised String for a given key. If the String contains a formatting pattern,
	 * this pattern is replaced by the given argument object. Note that this method allows only
	 * one argument.
	 * For a detailed description of the formatting options please see class
	 * <code> java.text.MessageFormat </code>.
	 * @param a_key a key for the localised String
	 * @param a_argument a object that is inserted into the message String
	 * @return the localised String with inserted arguments
	 * @see java.text.MessageFormat
	 */
	public static String getString(String a_key, Object a_argument)
	{
		return getString(a_key, Util.toArray(a_argument));
	}

}
