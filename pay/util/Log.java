// Copyright 2002 Adam Megacz, see the COPYING file for licensing [LGPL]
//package org.xwt.util;
//
package pay.util;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.util.Date;

/** easy to use logger */
public class Log {

	public static final int TEST = 2; 			// unsinnige Testausgaben
	public static final int LONG_DEBUG = 1; 	// ausführliche Debug Infos
	public static final int SHORT_DEBUG = 3;  	// knappe Debug Infos
	public static final int INFO = 4; 			// Information für den Benutzer
	public static final int VERY_IMPORTANT = 5; // Sehr wichtige Information für den Benutzer (Fehlermeldungen)

	static final String messageTyp[] = {"NO","DEBUG","TEST","DEBUG","INFO","INFO"};

		public static boolean on = true;
		public static boolean verbose = false;
		public static boolean logDates = false;
		public static Date lastDate = null;

		/** true iff nothing has yet been logged */
		public static boolean firstMessage = true;

	private static int state = SHORT_DEBUG;

	private Log(){
	}
	/**
	 * Hier wird eigestellt Nachrichten welcher Priorität angezeigt werden.
	 *
	 * @param status Die niedrigste priorität die noch angezeigt wird.
	**/
	public static void setDebugModus(int status){
		log("Log","Debug Modus Chanced new Modus = "+status,Log.SHORT_DEBUG);
		state = status;

	}

	/**
	 * Methode zum anzeigen einen Nachricht
	 *
	 * @param message Nachricht die Angezeigt werden soll
	 * @param priority gibt die wichtigkeit der Nachricht an.
	 *
	 **/
		public static synchronized void log(Object o, Object message,int priority) {
		if (state<=priority){
			doLog(o,message,messageTyp[priority]);
		}
	}

	public static synchronized void log(Object o, Object message) {
		log(o,message,SHORT_DEBUG);
	}
	private static synchronized void doLog(Object o, Object message, String typ) {
		if (o==null) o = new Object();
				if (firstMessage && !logDates) {
						firstMessage = false;
						System.err.println("===========================================================================");
						log(Log.class, "Logging enabled at " + new java.util.Date());
				}

				String classname;
				if (o instanceof Class) classname = ((Class)o).getName();
				else if (o instanceof String) classname = (String)o;
				else classname = o.getClass().getName();

				if (classname.indexOf('.') != -1) classname = classname.substring(classname.lastIndexOf('.') + 1);
				if (classname.length() > (logDates ? 10 : 12)) classname = classname.substring(0, (logDates ? 10 : 12));
				while (classname.length() < (logDates ? 10 : 12)) classname = " " + classname;
				classname = classname + ": ";

				if (logDates) {
						Date d = new Date();
						if (lastDate == null || d.getYear() != lastDate.getYear() || d.getMonth() != lastDate.getMonth() || d.getDay() != lastDate.getDay()) {
								String now = new java.text.SimpleDateFormat("EEE dd MMM yyyy").format(d);
								System.err.println();
								System.err.println("=== " + now + " ==========================================================");
						}
						java.text.DateFormat df = new java.text.SimpleDateFormat("[EEE HH:mm:ss] ");
						classname = df.format(d) + classname;
						lastDate = d;
				}

				if (!(message instanceof Throwable)) System.err.println(classname + typ+": "+message);
				else {
						ByteArrayOutputStream baos = new ByteArrayOutputStream();
						((Throwable)message).printStackTrace(new PrintStream(baos));
						byte[] b = baos.toByteArray();
						BufferedReader br = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(b)));
						String s = null;
						try {
								while((s = br.readLine()) != null) {
										System.err.print(classname+ typ+": ");
										for(int i=0; i<s.length(); i++)
												System.err.print(s.charAt(i) == '\t' ? "    " : ("" + s.charAt(i)));
										System.err.println();
								}
						} catch (Exception e) { }

				}
		}

}
