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

import java.io.UnsupportedEncodingException;

public class URLDecoder
{
	public static String decode(String s)
	{
		byte[] enc = new byte[s.length()];
		int bytes;
		char c;

	    if( s == null)
		{
			return null;
		}
		try
		{
			StringBuffer output=new StringBuffer();
			int i = 0;
			bytes = 0;

			while(i < s.length())
			{
				c = s.charAt(i);
				if(c=='+')
				{
					output.append(' ');
				}
				else if (c=='%')
				{
					enc[bytes] = (byte)Integer.parseInt(s.substring(i + 1, i + 3), 16);
					bytes++;
					i += 2;
				}
				else
				{
					output.append(c);
				}
				i++;

	            if ((i < s.length() && s.charAt(i) != '%') || i >= s.length())
				{
					output.append(new String(enc, 0, bytes, "UTF-8"));
					bytes = 0;
				}
			}
			return output.toString();
		}
		catch (NumberFormatException a_e)
		{
			return null;
		}
		catch (UnsupportedEncodingException a_e)
		{
			return null;
		}
	}
}
