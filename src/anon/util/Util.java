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
package anon.util;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.StringTokenizer;

public final class Util
{
	/**
	 * This class works without being initialised and is completely static.
	 * Therefore, the constructor is not needed and private.
	 */
	private Util()
	{
	}

	/**
	 * Normalises a String to the given length by filling it up with spaces, if it
	 * does not already have this length or is even longer.
	 * @param a_string a String
	 * @param a_normLength a length to normalise the String
	 * @return the normalised String
	 */
	public static String normaliseString(String a_string, int a_normLength)
	{
		if (a_string.length() < a_normLength)
		{
			char[] space = new char[a_normLength - a_string.length()];
			for (int i = 0; i < space.length; i++)
			{
				space[i] = ' ';
			}
			a_string = a_string + new String(space);
		}

		return a_string;
	}

	/**
	 * Gets the stack trace of a Throwable as String.
	 * @param a_t a Throwable
	 * @return the stack trace of a throwable as String
	 */
	public static String getStackTrace(Throwable a_t)
	{
		PrintWriter writer;
		StringWriter strWriter;

		strWriter = new StringWriter();
		writer = new PrintWriter(strWriter);

		a_t.printStackTrace(writer);

		return strWriter.toString();
	}


	/**
	 * checks if the given address is a valid IP address
	 * @param addr
	 * address
	 * @return
	 */
	public static boolean isIPAddress(String addr)
	{
		StringTokenizer st = new StringTokenizer(addr, ".");
		int i = 0;
		int c;
		while (st.hasMoreTokens())
		{
			String s = st.nextToken();
			try
			{
				c = Integer.parseInt(s);
			}
			catch (Exception e)
			{
				return false;
			}
			if (c < 0 || c > 255)
			{
				return false;
			}
			i++;
			if (i > 4)
			{
				return false;
			}
		}
		return true;
	}

}
