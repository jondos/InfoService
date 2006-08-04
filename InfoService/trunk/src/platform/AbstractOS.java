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
package platform;

import java.net.URL;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import gui.JAPHelp.IExternalURLCaller;
import gui.dialog.JAPDialog;

/**
 * This abstract class provides access to OS-specific implementations of certain
 * functions. It tries to instantiate an OS-specific class by determining on which
 * operating system JAP is currently running.
 */
public abstract class AbstractOS implements IExternalURLCaller
{
	/**
	 * Make sure that the default OS is the last OS in the array.
	 */
	private static Class[] REGISTERED_PLATFORM_CLASSES =
		{
		LinuxOS.class, WindowsOS.class, MacOS.class, DefaultOS.class};

	/**
	 * The instanciated operation system class.
	 * (no, ms_operating system does not mean only Microsoft OS are supported... ;-))
	 */
	private static AbstractOS ms_operatingSystem;

	/**
	 * Instantiates an OS-specific class. If no specific class is found, the default OS
	 * (which is a dummy implementation) is instanciated.
	 * @return the instanciated operating system class
	 */
	public static final AbstractOS getInstance()
	{
		for (int i = 0; ms_operatingSystem == null && i < REGISTERED_PLATFORM_CLASSES.length; i++)
		{
			try
			{
				ms_operatingSystem =
					(AbstractOS) REGISTERED_PLATFORM_CLASSES[i].newInstance();
			}
			catch (Exception a_e)
			{
				LogHolder.log(LogLevel.DEBUG, LogType.MISC,
							  "Cannot instantiate class " + REGISTERED_PLATFORM_CLASSES[i] +
							  ". Trying to instanciate an other platform class.");
			}
		}

		return ms_operatingSystem;
	}

	public JAPDialog.ILinkedInformation createURLLink(final URL a_url, final String a_optionalText)
	{
		if (a_url == null)
		{
			return null;
		}

		JAPDialog.ILinkedInformation link = new JAPDialog.ILinkedInformation()
		{
			public boolean  isApplicationModalityForced()
			{
				return false;
			}
			public int getType()
			{
				return JAPDialog.ILinkedInformation.TYPE_LINK;
			}
			public void clicked(boolean a_bState)
			{
				openURL(a_url);
			}
			public String getMessage()
			{
				if (a_optionalText == null || a_optionalText.trim().length() == 0)
				{
					return a_url.toString();
				}
				else
				{
					return a_optionalText;
				}
			}
	};


		return link;
	}

	/**
	 * Implementations must return a valid path to the config file.
	 */
	public abstract String getConfigPath();

}
