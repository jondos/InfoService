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
package pay.util;

public class EasyXMLParser
{

	String source;

	public EasyXMLParser(String source)
	{
		this.source = source;
	}

	int tagBegin;
	int tagEnd;

	/**
	 * gibt den inhalt des übergebenen tags heraus. fängt ab offset an zu suchen
	 * durch aufruf dieser methode wird tagBegin und tagEnd verändert.
	 */
	public String get(String tag, int offset)
	{
		int first, second;
		tagBegin = source.indexOf("<" + tag + ">");
		first = tagBegin + tag.length() + 2;
		if (first != -1)
		{
			second = source.indexOf("</" + tag + ">");
			tagEnd = second + tag.length() + 3;
			return source.substring(first, second);
		}
		else
		{
			return null;
		}
	}

	/**
	 * wie get(String tag,int offset) - fängt immer nach dem letzten gefundenen tag an
	 */
	public String getNext(String tag)
	{
		return get(tag, tagEnd);
	}

	/**
	 * wie get(String tag,int offset) - fängt immer vor dem letzten gefundenen tag  an
	 */
	public String get(String tag)
	{
		return get(tag, tagBegin);
	}

	/**
	 * gibt immer den Offset an vor dem letzten gefundenen öffnenden Tag
	 */
	public int getTagBegin()
	{
		return tagBegin;
	}

	/**
	 * gibt immer den Offset an nach dem letzten gefundenen schließenden Tag
	 */

	public int getTagEnd()
	{
		return tagEnd;
	}

	public static String getFirst(String source, String tag)
	{
		return new EasyXMLParser(source).get(tag);
	}

}
