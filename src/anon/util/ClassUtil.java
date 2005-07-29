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

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.Hashtable;
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
	//private static Hashtable ms_loadedClasses = new Hashtable();

	/**
	 * Stores all loaded directories.
	 */
	//private static Vector ms_loadedDirectories = new Vector();

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

		if (pointIndex >= 0)
		{
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
/*	public static Class getCallingClassStatic()
	{
		return new ClassGetter().getCallingClassStatic();
	}
*/
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
	//remove doe to persistent bugs with some JDKs
	/*public static Vector findSubclasses(Class a_class)
	  {
	 Enumeration classes=null;
	 Vector subclasses=null;
	 Class possibleSubclass=null;

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
	 */
	/**
	 * Loads all classes into cache that are in the same file structure as this class
	 * and as the calling class. Recommended to be called at program start to
	 * initially fill the cache.
	 * WARNING: this may be slow at the first call, especially for large packages (like the JRE)
	 * @return all loaded classes
	 */
	/*public static Enumeration loadClasses()
	  {
	 Class callingClass=null;

	 callingClass = getCallingClassStatic();

	 // load all classes for this class and the calling class
	 loadClasses(callingClass);

	 return ms_loadedClasses.elements();
	  }
	 */
	/**
	 * Loads all classes into cache that are in the same file structure as
	 * the given class and as the calling class.
	 * WARNING: this may be slow at the first call, especially for large packages (like the JRE)
	 * @param a_rootClass the class from that loading is started
	 * @return all loaded classes
	 */
	/*public static Enumeration loadClasses(Class a_rootClass)
	  {
	 PrintStream syserror=null;
	 PrintStream dummyStream = new PrintStream(new ByteArrayOutputStream());
	 Class thisClass=null, callingClass=null;

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
	  if (a_e instanceof Exception && !(a_e instanceof RuntimeException))
	  {
	   // @todo throw the exception //
	   a_e.printStackTrace();
	  }
	 }
	 // reactivate standard error
	 System.setErr(syserror);

	 return ms_loadedClasses.elements();
	  }
	 */
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
		URL classUrl;
		File file;

		// generate a URL that points to the location of this class
		classResource = "/" + toRelativeResourcePath(a_class);
		classUrl = a_class.getResource(classResource);

		// check if the system resource protocol is used
		file = ResourceLoader.getSystemResource(classUrl.toString());
		if (file == null)
		{
			// the system resource protocol is not used; try to get the directory by another way...
			classDirectory = URLDecoder.decode(classUrl.toString());

			// check whether it is a jar file or a directory
			if (classDirectory.startsWith(JAR_FILE))
			{
				classDirectory = classDirectory.substring(
					JAR_FILE.length(), classDirectory.lastIndexOf(classResource) - 1);
				if (classDirectory.charAt(2) == ':')
				{
					// this is a windows file of the format /C:/...
					classDirectory = classDirectory.substring(1, classDirectory.length());
				}
				classDirectory = ResourceLoader.replaceFileSeparatorsSystemSpecific(classDirectory);
				file = new File(classDirectory);
			}
			else if (classDirectory.startsWith(FILE))
			{
				classDirectory =
					classDirectory.substring(FILE.length(), classDirectory.lastIndexOf(classResource));
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
		}

		return file;
	}

	/**
	 * Generates a relative resource path to the given class.
	 * @param a_class Class
	 * @return URL
	 */
	public static String toRelativeResourcePath(Class a_class)
	{
		String classResource;

		classResource = a_class.getName();
		classResource = classResource.replace('.', '/');
		classResource += ".class";

		return classResource;
	}

	/**
	 * Traverse a file, directory or zip/jar file recursive until a class file is found and
	 * instantiated or all files are traversed. The instantiated class is returned.
	 * @param a_file a file, directory or zip/jar file
	 * @return the first class that was instnatiated or
	 *         null if no class could be found and instantiated
	 */
	protected static Class getFirstClassFound(File a_file)
	{
		Hashtable classInstance = new Hashtable();

		ResourceLoader.loadResources("/", a_file, new ClassInstantiator(3), true, true, false,
									 classInstance);
		// we choose "3" because after 3 tries it is highly possible this is not a valid classdir

		if (classInstance.size() == 1)
		{
			return (Class) classInstance.elements().nextElement();
		}

		return null;
	}


	/**
	 * Loads all classes into cache that are in the same file structure as the given class.
	 * WARNING: this may be slow at the first call, especially for large packages (like the JRE)
	 * @param a_rootClass the class from that loading is started
	 * @throws IOException if an I/O error occurs
	 */
	/*private static void loadClassesInternal(Class a_rootClass) throws IOException
	  {
	 File file = null;

	 if ( (file = getClassDirectory(a_rootClass)) == null)
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

	 // read all classes in the directory
	 ResourceLoader.loadResources("/", file, new ClassInstantiator(),
			 true, false, false, ms_loadedClasses);
	  }
	 */
	/**
	 * Turns class files into Class objects.
	 * @param a_classFile a class file with full directory path
	 * @param a_classDirectory the directory where all class files and class directories reside
	 * @return the class file as Class object
	 */
	private static Class toClass(File a_classFile, File a_classDirectory)
	{
		Class classObject = null;
		String className = null;
		String classDirectory = null;
		int startIndex;

		if (a_classFile == null || !a_classFile.getName().endsWith(".class"))
		{
			return null;
		}

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
			className = className.substring(startIndex, className.lastIndexOf(".class"));
			className = className.replace(File.separatorChar, '.');
			classObject = Class.forName(className);
		}
		catch (Throwable a_e)
		{
			classObject = null;
		}

		return classObject;
	}

	private static class ClassInstantiator implements ResourceInstantiator
	{
		private int m_invalidAfterFailure;
		private int m_currentFailure;

		public ClassInstantiator()
		{
			m_invalidAfterFailure = 0;
			m_currentFailure = 0;
		}

		public ClassInstantiator(int a_invalidAfterFailure)
		{
			m_invalidAfterFailure = a_invalidAfterFailure;
			m_currentFailure = 0;
		}

		public Object getInstance(File a_file, File a_topDirectory) throws ResourceInstantiator.
			ResourceInstantiationException

		{
			Class loadedClass = toClass(a_file, a_topDirectory);

			if (m_invalidAfterFailure > 0)
			{
				checkValidity(loadedClass, a_file.getName());
			}

			return loadedClass;
		}

		public Object getInstance(ZipEntry a_entry, ZipFile a_file) throws ResourceInstantiator.
			ResourceInstantiationException
		{
			Class loadedClass = toClass(new File( (a_entry).toString()), (File)null);

			if (m_invalidAfterFailure > 0)
			{
				checkValidity(loadedClass, a_entry.getName());
			}

			return loadedClass;
		}

		private void checkValidity(Class a_loadedClass, String a_filename) throws ResourceInstantiator.
			ResourceInstantiationException
		{
			if (a_loadedClass == null && a_filename.endsWith(".class"))
			{
				m_currentFailure++;
			}
			if (m_currentFailure >= m_invalidAfterFailure)
			{
				throw new ResourceInstantiator.ResourceInstantiationException();
			}
		}
	}
}
