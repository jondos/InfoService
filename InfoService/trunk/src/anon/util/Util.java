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

import java.io.File;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Vector;
import java.util.zip.ZipFile;
import java.util.zip.ZipEntry;

public final class Util
{
	private static final String JAR_FILE = "jar:file:";
	private static final String FILE = "file:";

	private static Vector ms_classNames;


	/**
	 * Gets the name of a class without package. It can be used to create an
	 * xml tag with the class name.
	 * @param a_class a Class
	 * @return the name of the class without package
	 */
	public static String getShortClassName(Class a_class)
	{
		StringTokenizer tokenizer;
		String classname = null;

		tokenizer = new StringTokenizer(a_class.getName(), ".");

		while (tokenizer.hasMoreTokens())
		{
			classname = tokenizer.nextToken();
		}

		return classname;
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
	 * Returns the current class from a static context. This method
	 * replaces the Object.getClass() method in a static environment, as
	 * <Code>this</Code> is not available.
	 * @return the current class
	 */
	public static Class getClassStatic()
	{
		return new ClassGetter().getCurrentClassStatic();
	}

	/**
	 * Returns the class that called the current method.
	 * @return the class that called the current method
	 */
	public static Class getCallingClassStatic()
	{
		return new ClassGetter().getCallingClassStatic();
	}

	/**
	 * Gets all classes that extend the given class or implement the given
	 * interface, including the class itself.
	 * @param a_class a Class
	 * @return all subclasses of the given class
	 */
	public static Enumeration getSubclasses(Class a_class)
	{
		Enumeration classes;
		Vector subclasses;
		Class currentClass;

		classes = loadClassNames();
		subclasses = new Vector();

		while (classes.hasMoreElements())
		{
			currentClass = (Class) classes.nextElement();
			if (a_class.isAssignableFrom(currentClass))
			{
				subclasses.addElement(currentClass);
			}
		}

		return subclasses.elements();
	}

	private static class ClassGetter extends SecurityManager
	{
		public Class getCurrentClassStatic()
		{
			return getClassContext()[2];
		}

		public Class getCallingClassStatic()
		{
			return getClassContext()[3];
		}
	}

	/**
	 * Loads all classes in the local packages into an enumeration.
	 * @return all classes in the local packages
	 */
	private static Enumeration loadClassNames()
	{
		String classResource;
		String classDirectory;

		// look in the cache if the class names have already been read
		if (ms_classNames != null)
		{
			return ms_classNames.elements();
		}

		// create the class name cache
		ms_classNames = new Vector();

		// generate a url with this class as resource
		classResource = Util.getClassStatic().getName();
		classResource = "/" + classResource;
		classResource = classResource.replace('.','/');
		classResource += ".class";
		try
		{
			classDirectory =
				Class.forName(Util.getClassStatic().getName()).getResource(classResource).toString();
		}
		catch (ClassNotFoundException a_e)
		{
			// not possible, this class DOES exist
			classDirectory = null;
		}

		// check whether it is a jar file or a directory
		if (classDirectory.startsWith(JAR_FILE))
		{
			ZipFile file;
			Enumeration entries;

			classDirectory = classDirectory.substring(
						 JAR_FILE.length(), classDirectory.indexOf(classResource) - 1);

			try
			{
				file = new ZipFile(classDirectory);
			}
			catch (Exception a_e)
			{
				// not possible, this class DOES exist
				file = null;
			}

			entries = file.entries();
			while (entries.hasMoreElements())
			{
				Class classObject;
				classObject = toClass(new File((((ZipEntry) entries.nextElement())).toString()),
									  (File)null);

				if (classObject != null)
				{
					ms_classNames.addElement(classObject);
				}
			}
		}
		else if (classDirectory.startsWith(FILE))
		{
			classDirectory = classDirectory.substring(
						 FILE.length(), classDirectory.indexOf(classResource));
			ms_classNames = getClasses(new File(classDirectory), new File(classDirectory));
		}
		else
		{
			// we cannot read from this source; it is neither a jar-file nor a directory
			ms_classNames = null;
		}

		return ms_classNames.elements();
	}

	/**
	 * Returns all classes in a directory as Class objects or the given file itself as a Class,
	 * if it is a class file.
	 * @param a_file a class file or directory
	 * @param a_classDirectory the directory where all class files and class directories reside
	 * @return Class objects
	 */
	private static Vector getClasses(File a_file, File a_classDirectory)
	{
		Vector classes = new Vector();
		Enumeration enumClasses;
		String[] filesArray;

		if (a_file != null)
		{
			if (!a_file.isDirectory())
			{
				Class classObject = toClass(a_file, a_classDirectory);

				if (classObject != null)
				{
					classes.addElement(classObject);
				}
			}
			else
			{
				// this file is a directory
				filesArray = a_file.list();
				for (int i = 0; i < filesArray.length; i++)
				{
					enumClasses = getClasses(new File(a_file.getAbsolutePath() + "/" + filesArray[i]),
											 a_classDirectory).elements();
					while (enumClasses.hasMoreElements())
					{
						classes.addElement(enumClasses.nextElement());
					}
				}
			}
		}

		return classes;
	}

	/**
	 * Turns class files into Class objects.
	 * @param a_classFile a class file with full directory path
	 * @param a_classDirectory the directory where all class files and class directories reside
	 * @return the class file as Class object
	 */
	private static Class toClass(File a_classFile, File a_classDirectory)
	{
		Class classObject;
		String className;
		String classDirectory;
		int startIndex;

		if (a_classDirectory == null || !a_classDirectory.isDirectory())
		{
			startIndex = 0;
		}
		else
		{
			classDirectory = a_classDirectory.toString();
			if (classDirectory.endsWith("/"))
			{
				startIndex = classDirectory.length();
			}
			else
			{
				startIndex = classDirectory.length() + 1;
			}
		}

		try
		{
			className = a_classFile.toString();
			className = className.substring(startIndex, className.indexOf(".class"));
			className = className.replace('/', '.');
			classObject = Class.forName(className);
		}
		catch (Throwable a_e)
		{
			classObject = null;
		}

		return classObject;
	}
}
