// import java.io.PrintWriter;

/* 2000-08-01(HF):
Stefan, ich habe das Zeug mit PrintWriter und so auskommentiert, weil es irgendwie nicht ging.
Koenntest Du es im Self test (main()) evtl. im Beispielcode angeben?

/**
 * This class serves as a debugging interface.
 * It provides different debug types and levels for the output of debug
 * messages.
 * <P>
 * The debug level can be set with 
 * <code>JAPDebug.setDebuglevel(int level)</code>.
 * <P>
 * The debug type can be set with 
 * <code>JAPDebug.setDebugType(int type)</code>.
 * <P>
 * To output a debug message use
 * <code>JAPDebug.out(int level, int type, String txt)</code>
 * This is a Singleton!
 */
public final class JAPDebug {

	/** No debugging */
	public static int NUL = 0; 
	/** Indicates a GUI related message (binary: <code>00000001</code>) */
	public static int GUI = 1;
	/** Indicates a network related message (binary: <code>00000010</code>) */
	public static int NET = 2; 
	/** Indicates a thread related message (binary: <code>00000100</code>) */
	public static int THREAD = 4; 
	/** Indicates a misc message (binary: <code>00001000</code>) */
	public static int MISC = 8; 

	/** Indicates level type of message: Emergency message*/
	public static int EMERG     = 7; 
	/** Indicates level type of message: Alert message */
	public static int ALERT     = 6; 
	/** Indicates level type of message: For instance to  use when catching Exeption to output a debug message.*/
	public static int EXCEPTION = 5; //2000-07-31(HF): CRIT zu EXCEPTION geaendert, wegen besserem Verstaendnis
	/** Indicates level type of message: Error message */
	public static int ERR       = 4; 
	/** Indicates level type of message: Warning */
	public static int WARNING   = 3; 
	/** Indicates level type of message: Notice */
	public static int NOTICE    = 2; 
	/** Indicates level type of message: Information */
	public static int INFO      = 1; 
	/** Indicates level type of message, e.g. a simple debugging message to output something */
	public static int DEBUG     = 0; 

	private int debugtype;
	private int debuglevel;
	
//	private PrintWriter[] outStreams;
		
	private static JAPDebug debug; 

	private JAPDebug () {
		debugtype=GUI+NET+THREAD+MISC;
		debuglevel=DEBUG;
//		outStreams=new PrintWriter[8];
//		for(int i=0;i<8;i++)
//			outStreams[i]=new PrintWriter(System.out);
		}
	
	public static JAPDebug create()
		{
			if(debug!=null)
				out(debug.ALERT,debug.INFO,"Debug inialized twice - Big Bug!");
			else
				debug=new JAPDebug();
			return debug;
		}
	
	/** Output a debug message.
	 *  @param level The level of the debugging message (EMERG,ALERT,CRIT,ERR,WARNING,NOTICE,INFO,DEBUG)
	 *  @param type The type of the debugging message (GUI, NET, THREAD, MISC)
	 *  @param txt   The message itself
	 */
	public static void out(int level, int type, String txt) {
//		if(level<0||level>JAPDebug.EMERG||txt==null||debug==null||debug.outStreams[level]==null)
//			return;
		if ( (level >= debug.debuglevel) && (debug.debugtype & type) !=0 ) {
//			debug.outStreams[level].println("JAPDebug: "+txt);
			System.err.println("JAPDebug: "+txt);
		}
	}
	
	/** Set the debugging type you like to output. To activate more than one type you simly add 
	 *  the types like this <code>setDebugType(JAPDebug.GUI+JAPDebug.NET)</code>.
	 *  @param type The debug type (NUL, GUI, NET, THREAD, MISC)
	 */
	public static void setDebugType(int type) {
		debug.debugtype = type;
	}

	/** Get the current debug type.
	 */
	public static int getDebugType() {
		return debug.debugtype;
	}

	/** Set the debugging level you would like to output.  
	 *  The possible parameters are (EMERG, ALERT, EXCEPTION, ERR, WARNING, NOTICE, INFO, DEBUG).
	 *  DEBUG means output all messages, EMERG means only emergency messages.
	 *  @param type The debug level (EMERG, ALERT, EXCEPTION, ERR, WARNING, NOTICE, INFO, DEBUG)
	 */
	public static void setDebugLevel(int level) {
		debug.debuglevel = level;
	}
	
	/** Get the current debug level.
	 */
	public static int getDebugLevel() {
		return debug.debuglevel;
	}
		
//	/** Set the debugging output stream. Each debug level has his on outputstream. This defaults to System.out
//	 * @param level The debug level
//	 * @param out The assoziated otuput stream (maybe null)
//	 * @return true if succesful, false otherwise 
//	*/
//	public static boolean setLevelOutputStream(int level, PrintWriter out) {
//	if(level<0 || level>JAPDebug.EMERG)
//		return false;
//	debug.outStreams[level]=out;
//	return true;
//	}
 	
	private static void printDebugSettings() {
		System.out.println("JAPDebug: debugtype ="+Integer.toString(debug.debugtype));
		System.out.println("JAPDebug: debuglevel="+Integer.toString(debug.debuglevel));
	}
	
	/** Provides a simle self test of the debugging functions. */
	public static void main() {
	JAPDebug japdebug = new JAPDebug();
	
	System.out.println("JAPDebug: Self test");
	
	System.out.println("JAPDebug: Default settings");
	JAPDebug.printDebugSettings();
	
	System.out.println("JAPDebug: Selftest 1: GUI+NET");
	JAPDebug.setDebugType(JAPDebug.GUI+JAPDebug.NET);
	JAPDebug.printDebugSettings();
	JAPDebug.out(JAPDebug.DEBUG,JAPDebug.GUI,"This is a GUI related debug message");
	JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"This is a NET related debug message");
	
	System.out.println("JAPDebug: Selftest 2: GUI only");
	JAPDebug.setDebugType(JAPDebug.GUI);
	JAPDebug.printDebugSettings();
	JAPDebug.out(JAPDebug.DEBUG,JAPDebug.GUI,"This is a GUI related debug message");
	JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"This is a NET related debug message");
	
	System.out.println("JAPDebug: Selftest 3: NET");
	JAPDebug.setDebugType(JAPDebug.NET);
	JAPDebug.printDebugSettings();
	JAPDebug.out(JAPDebug.DEBUG,JAPDebug.GUI,"This is a GUI related debug message");
	JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"This is a NET related debug message");
	
	System.out.println("JAPDebug: Selftest 4: no debugging");
	JAPDebug.setDebugType(JAPDebug.NUL);
	JAPDebug.printDebugSettings();
	JAPDebug.out(JAPDebug.DEBUG,JAPDebug.GUI,"This is a GUI related debug message");
	JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"This is a NET related debug message");
	System.out.println("JAPDebug: End");
	}
}