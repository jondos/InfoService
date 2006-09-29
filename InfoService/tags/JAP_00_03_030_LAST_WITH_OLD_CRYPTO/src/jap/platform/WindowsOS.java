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
package jap.platform;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/**
 * This class is instantiated by AbstractOS if the current OS is Windows
 */
public class WindowsOS extends AbstractOS
{
	public static final String[] BROWSERLIST =
	{
		"firefox", "iexplore", "explorer", "mozilla", "konqueror", "mozilla-firefox",
		"firebird", "opera"
	};

	public WindowsOS() throws Exception
	{
		String osName = System.getProperty("os.name", "").toLowerCase();
		if (osName.indexOf("win") == -1)
		{
			throw new Exception("Operating system is not Windows");
		}
	}

	public void openURLInBrowser(String a_url)
	{
		boolean success = false;

		String[] browser = BROWSERLIST;
		for (int i = 0; i < browser.length; i++)
		{
			try
			{
				Runtime.getRuntime().exec(new String[]
										  {browser[i], a_url});
				success = true;
				break;
			}
			catch (Exception ex)
			{
			}
		}
		if (!success)
		{
			LogHolder.log(LogLevel.ERR, LogType.MISC, "Cannot open URL in browser");
		}
	}
}