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
import java.io.File;
import java.util.Vector;
import java.util.StringTokenizer;

/**
 * This class loads resources from the file system.
 */
final public class ResourceLoader
{
	private static final String SYSTEM_RESOURCE_TYPE_ZIP = "ZIP";
	private static final String SYSTEM_RESOURCE_TYPE_JAR = "JAR";
	private static final String SYSTEM_RESOURCE_TYPE_FILE = "FILE";
	private static final String SYSTEM_RESOURCE = "systemresource:/";
	private static final String SYSTEM_RESOURCE_ENDSIGN = "/+/";

	private static Vector ms_classpathFiles;
	private static String ms_classpath;


	private ResourceLoader()
	{
	}

	/**
	 * Loads a resource from the classpath or the current directory.
	 * The resource may be contained in an archive (ZIP,JAR) or a directory structure.
	 * If the resource could not be found in the classpath, it is loaded from the current
	 * directory.
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

	/**
	 * Returns a given a requested system resource as a file. A system resource is either
	 * a zip file or a directory that is specified in the classpath
	 * (Property <Code> java.class.path </Code>). The resource must be specified with by
	 * the following protocol syntax:
	 * <DL>
	 * <DT> ZipFile </DT>
	 * <DD> :systemresource:/ZIP[id]/+/ </DD>
	 * <DT> File </DT>
	 * <DD> :systemresource:/FILE[id]/+/ </DD>
	 * </DL>
	 * [id] may be an integer specifying the resource's position int the classpath
	 * (beginning with 0) or an absolute path containing the requested resource. The end sign
	 * '/+/' is optional and marks the end of the [id].
	 * The system resource protocol is only used in old JDKs < 1.2.
	 *
	 * @param a_systemResource a system resource a String
	 * @return The requested system resource as a file or null if the resource could not be found
	 */
	public static File getSystemResource(String a_systemResource)
	{
		int endIndex;

	    if (a_systemResource.indexOf(SYSTEM_RESOURCE) != 0)
		{
			return null;
		}

	    // find the beginning of the [id] string
		a_systemResource =
			a_systemResource.substring(SYSTEM_RESOURCE.length(), a_systemResource.length());
		if (a_systemResource.toUpperCase().startsWith(SYSTEM_RESOURCE_TYPE_ZIP))
		{
			a_systemResource = a_systemResource.substring(
				 SYSTEM_RESOURCE_TYPE_ZIP.length(), a_systemResource.length());
		}
		else if (a_systemResource.toUpperCase().startsWith(SYSTEM_RESOURCE_TYPE_JAR))
		{
			a_systemResource = a_systemResource.substring(
				 SYSTEM_RESOURCE_TYPE_JAR.length(), a_systemResource.length());
		}
		else if (a_systemResource.toUpperCase().startsWith(SYSTEM_RESOURCE_TYPE_FILE))
		{
			a_systemResource = a_systemResource.substring(
				 SYSTEM_RESOURCE_TYPE_FILE.length(), a_systemResource.length());
		}

	    // now find the end of the [id] string and extract the [id]
		endIndex = a_systemResource.indexOf(SYSTEM_RESOURCE_ENDSIGN);
		if (endIndex >= 0)
		{
			a_systemResource = a_systemResource.substring(0, endIndex);
		}

       // try to interpret the [id] as an integer number
	    try
		{
			readResourcesFromClasspath(); // initialize the vector if necessary
			return (File)ms_classpathFiles.elementAt(Integer.parseInt(a_systemResource));
		}
		catch (Exception a_e)
		{
			// the [id] seems to be a file path
			return new File(a_systemResource);
		}
	}

	/**
	 * Reads all resources from the classpath and stores them as files.
	 * The method does nothing if the classpath has not changed since the last call.
	 */
	private static void readResourcesFromClasspath()
	{
		String classpath = System.getProperty("java.class.path");

		if (ms_classpath == null || !ms_classpath.equals(classpath))
		{
			StringTokenizer tokenizer;

			ms_classpath = classpath;
			ms_classpathFiles = new Vector();

			tokenizer = new StringTokenizer(ms_classpath, System.getProperty("path.separator"));
			while (tokenizer.hasMoreTokens())
			{
				ms_classpathFiles.addElement(new File(tokenizer.nextToken()));
			}
		}
	}
}
