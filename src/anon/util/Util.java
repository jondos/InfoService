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
	 * Tests if two byte arrays are equal.
	 * @param arrayOne a byte array
	 * @param arrayTwo another byte array
	 * @return true if the two byte arrays are equal or both arrays are null; false otherwise
	 */
	public static boolean arraysEqual(byte[] arrayOne, byte[] arrayTwo)
	{
		if (arrayOne == null && arrayTwo == null)
		{
			return true;
		}

		if (arrayOne == null || arrayTwo == null)
		{
			return false;
		}

		if (arrayOne.length != arrayTwo.length)
		{
			return false;
		}

		for (int i = 0; i < arrayOne.length; i++)
		{
			if (arrayOne[i] != arrayTwo[i])
			{
				return false;
			}
		}

		return true;
	}

	/**
	 * Tests if a_length positions of two arrays are equal.
	 * @param a_arrayA byte[]
	 * @param a_APos int
	 * @param a_arrayB byte[]
	 * @param a_BPos int
	 * @param a_length int
	 * @return boolean
	 */
	public static final boolean arraysEqual(byte[] a_arrayA, int a_Aoff,
											byte[] a_arrayB, int a_Boff,
											int a_length)
	{
		if (a_length <= 0)
		{
			return true;
		}
		if (a_arrayA == null || a_arrayB == null || a_Aoff < 0 || a_Boff < 0)
		{
			return false;
		}
		if (a_Aoff + a_length > a_arrayA.length ||
			a_Boff + a_length > a_arrayB.length)
		{
			return false;
		}

		for (int i = 0; i < a_length; i++)
		{
			if (a_arrayA[a_Aoff + i] != a_arrayB[a_Boff + i])
			{
				return false;
			}
		}

		return true;
	}

}
