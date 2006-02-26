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

import java.awt.Window;
import java.lang.reflect.Method;
import jap.JAPController;

final public class JAPDll
{
	private static boolean m_sbHasOnTraffic = true;
	static
	{
		try
		{
			String osName = System.getProperty("os.name", "").toLowerCase();
			if (osName.indexOf("win") > -1)
			{
				System.loadLibrary("japdll");
			}
		}
		catch (Throwable t)
		{
		}
	}

	static public boolean setWindowOnTop(Window theWindow, String caption, boolean onTop)
	{
		try
		{ //first we try the new setAlwaysOnTop method of Java 1.5
			Class[] c = new Class[1];
			c[0] = boolean.class;
			Method m = Window.class.getMethod("setAlwaysOnTop", c);
			Object[] args = new Object[1];
			args[0] = new Boolean(onTop);
			m.invoke(theWindow, args);
			return true;
		}
		catch (Throwable t)
		{
		}
		try
		{
			setWindowOnTop_dll(caption, onTop);
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

	static public String getDllVersion()
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

	static public long showMainWindow()
	{
		JAPController.getView().setVisible(true);
		JAPController.getView().toFront();
		return 0;
	}

	native static private void setWindowOnTop_dll(String caption, boolean onTop);

	native static private boolean hideWindowInTaskbar_dll(String caption);

	native static private boolean setWindowIcon_dll(String caption);

	native static private void onTraffic_dll();

	native static private String getDllVersion_dll();
}
