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
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;

/**
 * This class loads resources.
 */
final public class ResourceLoader
{
	private String m_unpackagedClass;

	/**
	 * Creates a new ResourceLoader.
	 * @param a_unpackagedClass a class in the default package path
	 */
	public ResourceLoader(String a_unpackagedClass)
	{
		m_unpackagedClass = a_unpackagedClass;
	}

	/** Loads an resource from a file or an archive resource.
	 *	@param strResource the resource name or filename
	 *  @return null if Resource could not be loaded
	 *  @return contents of the resource otherwise
	 */
	public byte[] loadResource(String strResource)
	{
		try
		{
			InputStream in = null;
			int len = 0;
			try
			{
				// this is necessary to make sure that the resources are loaded when contained in a jar-file
				in = Class.forName(m_unpackagedClass).getResourceAsStream(strResource);
				len = in.available();
			}
			catch (Exception e)
			{
				in = null;
			}
			if (in == null || len == 0)
			{
				try
				{
					//we have to check if the file does exist because a new file will always succeed!!!
					File f = new File(strResource);
					if (f.canRead())
					{
						in = new FileInputStream(f);
						len = (int) f.length();
					}
					else
					{
						return null;
					}
				}
				catch (Exception e1)
				{
					return null;
				}
			}
			byte[] tmp = new byte[len];
			new DataInputStream(in).readFully(tmp);
			return tmp;
		}
		catch (Throwable t)
		{
			return null;
		}
	}

}
