/*
 Copyright (c) 2004, The JAP-Team
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
/*
 * Created on Mar 25, 2004
 *
 */
package anon.tor.util;
import java.util.StringTokenizer;
/**
 * @author stefan
 *
 *some usefull utilities
 */
public class helper {


	public static byte[]  conc(byte[] b1,byte[] b2)
	{
		return conc(b1,b2,b2.length);
	}

	/**
	 * Concatenates two byte arrays
	 * @param b1
	 * @param b2
	 * @return
	 */
	public static byte[]  conc(byte[] b1,byte[] b2,int b2_len)
	{
		if(b1==null||b1.length==0)
		{
			return copybytes(b2,0,b2_len);
		}
		byte[] b= new byte[b1.length+b2_len];
		for(int i=0;i<b1.length;i++)
		{
			b[i]=b1[i];
		}
		if(b2.length>0)
		{
			for(int i=0;i<b2_len;i++)
			{
				b[i+b1.length]=b2[i];
			}
		}
		return b;
	}

	/**
	 * Converts a long to a array of bytes
	 * @param l
	 * @param length
	 * @return
	 */
	public static byte[] inttobyte(long l,int length)
	{
		byte[] b=new byte[length];
		for(int i=0;i<length;i++)
		{
			b[length-i-1]=(byte)( ( l & (0xff << (i*8)) ) >> (i*8));
		}
		return b;
	}

	/**
	 * copy some bytes from a array of bytes
	 * @param bytes array of bytes
	 * @param index startposition
	 * @param length length
	 * @return copied bytes
	 */
	public static byte[] copybytes(byte[] bytes,int index,int length)
	{
		byte[] b = new byte[length];
		for(int i=0;i<length;i++)
		{
			b[i] = bytes[index+i];
		}
		return b;
	}
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
