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
package anon.util;

import java.io.DataInputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;

/**
 * This class loads resources from the file system.
 */
final public class ResourceLoader
{

	private ResourceLoader()
	{
	}

	/**
	 * Loads a resource from the classpath or the current directory.
	 * The resource may be contained in an archive (JAR) or a directory structure. If the resource
	 * could not be found in the classpath, it is loaded from the current directory.
	 * @param a_strRelativeResourcePath a relative filename for the resource
	 * @return the contents of the resource or null if resource could not be loaded
	 */
	public static byte[] loadResource(String a_strRelativeResourcePath)
	{
		InputStream in;
		byte[] resource = null;

		if (a_strRelativeResourcePath == null)
		{
			return null;
		}

		// load images from the local classpath or jar-file
		in = Object.class.getResourceAsStream("/" + a_strRelativeResourcePath);
		try
		{
			if (in == null)
			{
				// load resource from the current directory
				in = new FileInputStream(a_strRelativeResourcePath);
			}
			resource = new byte[in.available()];
			new DataInputStream(in).readFully(resource);
			in.close();
		}
		catch (IOException a_e)
		{
			resource =  null;
		}

		return resource;
	}


}
