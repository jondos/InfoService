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

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.Enumeration;
import java.util.Vector;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

/**
 * This class performs some basic operations related to Class objects.
 * @author Rolf Wendolsky
 */
public final class ClassUtil
{
	private static final String JAR_FILE = "jar:file:";
	private static final String FILE = "file:";

	/**
	 * Stores all loaded classes.
	 */
	private static Vector ms_loadedClasses = new Vector();

	/**
	 * Stores all loaded directories.
	 */
	private static Vector ms_loadedDirectories = new Vector();

	/**
	 * This class works without being initialised and is completely static.
	 * Therefore, the constructor is not needed and private.
	 */
	private ClassUtil()
	{
	}

	/**
	 * Gets the name of a class without package (everything before the last "." is removed).
	 * @param a_class a Class
	 * @return the name of the class without package
	 */
	public static String getShortClassName(Class a_class)
	{
		String classname = a_class.getName();
		int pointIndex;

		pointIndex = classname.lastIndexOf('.');

		if (pointIndex >= 0) {
			classname = classname.substring(pointIndex + 1, classname.length());
		}

		return classname;
	}

	/**
	 * Returns the current class from a static context. This method is a replacement to
	 * this.getClass() in a static environment, as <Code>this</Code> is not available there.
	 * @return the current class
	 */
	public static Class getClassStatic()
	{
		return new ClassGetter().getCurrentClassStatic();
	}

	/**
	 * Returns the class that called the current method. This method is an alternative to
	 * Object.getClass(), as the caller is not needed to be an argument or an Object either.
	 * @return the class that called the current method
	 */
	public static Class getCallingClassStatic()
	{
		return new ClassGetter().getCallingClassStatic();
	}

	/**
	 * Gets all classes that extend the given class or implement the given
	 * interface, including the class itself. It is recommended to store this
	 * information somewhere and reuse it instead of calling this method again,
	 * as all known classes have to be searched at each call.
	 * If classes from special packages should be found, it is recommended to
	 * load those packages at program start. Otherwise, some classes in those
	 * packages, that inherit from classes in this or other packages, can not be
	 * found.
	 * WARNING: this may be slow at the first call, especially for large packages
	 * @param a_class a Class
	 * @return all known subclasses of the given class
	 */
	public static Vector findSubclasses(Class a_class)
	{
		Enumeration classes;
		Vector subclasses;
		Class possibleSubclass;

		loadClasses(a_class);
		classes = loadClasses(getCallingClassStatic());
		subclasses = new Vector();

		while (classes.hasMoreElements())
		{
			possibleSubclass = (Class) classes.nextElement();
			if (a_class.isAssignableFrom(possibleSubclass))
			{
				subclasses.addElement(possibleSubclass);
			}
		}

		return subclasses;
	}

	/**
	 * Loads all classes into cache that are in the same file structure as this class
	 * and as the calling class. Recommended to be called at program start to
	 * initially fill the cache.
	 * WARNING: this may be slow at the first call, especially for large packages (like the JRE)
	 * @return all loaded classes
	 */
	public static Enumeration loadClasses()
	{
		Class callingClass;

		callingClass = getCallingClassStatic();

		// load all classes for this class and the calling class
		loadClasses(callingClass);

		return ms_loadedClasses.elements();
	}

	/**
	 * Loads all classes into cache that are in the same file structure as
	 * the given class and as the calling class.
	 * WARNING: this may be slow at the first call, especially for large packages (like the JRE)
	 * @param a_rootClass the class from that loading is started
	 * @return all loaded classes
	 */
	public static Enumeration loadClasses(Class a_rootClass)
	{
		PrintStream syserror;
		PrintStream dummyStream = new PrintStream(new ByteArrayOutputStream());
		Class thisClass, callingClass;

		thisClass = getClassStatic();
		callingClass = getCallingClassStatic();

		// temporarily deactivate standard error to suppress printStackStrace() messages
		syserror = System.err;
		System.setErr(dummyStream);
		try
		{
			// load all classes for the specified class
			loadClassesInternal(a_rootClass);

			// load all classes for this class
			loadClassesInternal(thisClass);

			// load all classes for the calling class
			if (callingClass != a_rootClass && callingClass != thisClass)
			{
				loadClassesInternal(callingClass);
			}
		}
		catch (Throwable a_e)
		{
			System.setErr(syserror);
		}
		System.setErr(syserror);

		return ms_loadedClasses.elements();
	}

	/**
	 * Returns the class directory of the specified class. The class directory is either the
	 * directory in that the highest package in the package structure of the class is contained, 
	 * or the jar-File in that the class is contained. For extracting the contents of a jar-File, 
	 * see {@link java.util.zip.ZipFile}.
	 * @param a_class a class
	 * @return the class directory of the specified class, either a real directory or a Jar-file
	 *         or null if the directory/jar-file does not exist
	 */
	public static File getClassDirectory(Class a_class)
	{
		String classResource;
		String classDirectory;
		File file;

		// generate a url with this class as resource
		classResource = a_class.getName();
		classResource = "/" + classResource;
		classResource = classResource.replace('.','/');
		classResource += ".class";
		classDirectory = URLDecoder.decode(a_class.getResource(classResource).toString());

		// check whether it is a jar file or a directory
		if (classDirectory.startsWith(JAR_FILE))
		{
			classDirectory = classDirectory.substring(
				JAR_FILE.length(), classDirectory.indexOf(classResource) - 1);
			if (classDirectory.charAt(2) == ':')
			{
				// this is a windows file of the format /C:/...
				classDirectory = classDirectory.substring(1, classDirectory.length());
			}
			classDirectory = toSystemSpecificFileName(classDirectory);
			file = new File(classDirectory);
		}
		else if (classDirectory.startsWith(FILE))
		{
			classDirectory =
				classDirectory.substring(FILE.length(), classDirectory.indexOf(classResource));
			file = new File(classDirectory);
		}
		else
		{
			// we cannot read from this source; it is neither a jar-file nor a directory
			file = null;
		}

		if (file == null || !file.exists())
		{
			return null;
		}

		return file;
	}

	/**
	 * This small inner class is needed to get information about static classes.
	 */
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
	 * Loads all classes into cache that are in the same file structure as the given class.
	 * WARNING: this may be slow at the first call, especially for large packages (like the JRE)
	 * @param a_rootClass the class from that loading is started
	 */
	private static void loadClassesInternal(Class a_rootClass)
	{
		File file;
		Enumeration entries;

		if ((file = getClassDirectory(a_rootClass)) == null)
		{
			return;
		}

		// look in the cache if the class directory has already been read
		if (ms_loadedDirectories.contains(file.getAbsolutePath()))
		{
			// do not load the classes again
			return;
		}
		ms_loadedDirectories.addElement(file.getAbsolutePath());

		if (file.isDirectory())
		{
			// fetch the classes
			entries = getClasses(file, file).elements();
			while (entries.hasMoreElements())
			{
				ms_loadedClasses.addElement(entries.nextElement());
			}
		}
		else
		{
			try
			{
				// fetch the classes
				Class classObject;
				entries = new ZipFile(file).entries();
				while (entries.hasMoreElements())
				{
					classObject = toClass(new File((((ZipEntry) entries.nextElement())).toString()),
										  (File)null);
					if (classObject != null)
					{
						ms_loadedClasses.addElement(classObject);
					}
				}
			}
			catch (IOException a_e)
			{
				// this zip file DOES exist, but we cannot read it; should not happen
			}
		}
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
					enumClasses = getClasses(new File(a_file.getAbsolutePath() +
						File.separatorChar + filesArray[i]),
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
			if (classDirectory.endsWith(System.getProperty("file.separator")))
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
			className = className.replace(File.separatorChar, '.');
			classObject = Class.forName(className);
		}
		catch (Throwable a_e)
		{
			classObject = null;
		}

		return classObject;
	}

	/**
	 * Interprets a String as a filename and converts it to a system specific
	 * file name.
	 * @param a_filename a generic file name
	 * @return a system specific file name
	 */
	private static String toSystemSpecificFileName(String a_filename)
	{
		if (a_filename == null)
		{
			return null;
		}
		a_filename = a_filename.replace('/', File.separatorChar);
		a_filename = a_filename.replace('\\', File.separatorChar);

		return a_filename;
	}
}
