import java.io.PrintWriter;
/**
 * This class serves as a debugging interface.
 * It provides different debug levels, one for GUI (Graphical User Interface) related
 * messages, and one for NET (Networking) related messages.
 * 
 * @author  Hannes Federrath (2000-Jun-26)
 */
public final class JAPDebug
	{
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

		/** Indicates the type of message*/
    public static int EMERG = 7; 
    public static int ALERT = 6; 
    public static int CRIT = 5; 
    public static int ERR = 4; 
    public static int WARNING = 3; 
    public static int NOTICE = 2; 
    public static int INFO = 1; 
    public static int DEBUG = 0; 

		private int debugtype;
    private PrintWriter[] outStreams;
		
    private static JAPDebug debug; 
		/** Creates the object. The debug level must be set with 
     *  <code>setDebuglevel(int level)</code>.
     */
    public JAPDebug ()
			{
				debug=this;
				debugtype=0;
				outStreams=new PrintWriter[8];
				for(int i=0;i<8;i++)
					outStreams[i]=new PrintWriter(System.out);
			}
    
		
    /** Output a debug message.
     *  @param level The level of the debugging message (EMERG,ALERT,CRIT,ERR,WARNING,NOTICE,INFO,DEBUG)
     *  @param type The type of the debugging message (GUI, NET)
     *  @param txt   The message itself
     */
    public static void out(int level,int type, String txt)
			{
				if(level<0||level>JAPDebug.EMERG||debug.outStreams[level]==null)
					return;
				if ( (debug.debugtype & type) !=0 ) 
					debug.outStreams[level].println("JAPDebug: "+txt);
			}
    
    /** Set the debugging type. To activate more than one type you simly add 
     *  the levels like this <code>setDebugType(GUI+NET)</code>.
     *  @param type The debug type (NUL, GUI, NET)
     */
    public static void setDebugType(int type)
			{
        debug.debugtype = type;
			}
		
    /** Set the debugging output stream. Each debug level has his on outputstream. This defualts to System.out
     * @param level The debug level
     * @param out The assoziated otuput stream (maybe null)
    * @return true if succesful, false otherwise 
		*/
    public static boolean setLevelOutputStream(int level,PrintWriter out)
			{
				if(level<0 || level>JAPDebug.EMERG)
					return false;
				debug.outStreams[level]=out;
				return true;
			}

		/** Prints the current debug type. This is for testing purposes only. */
    public static void printDebugType()
			{
        System.out.println("JAPDebug: debugtype="+Integer.toString(debug.debugtype));
			}
    
    /** Provides a simle self test of the debugging functions. */
    public static void main() {
	
	System.out.println("JAPDebug: Selftest 1: GUI+NET");
//	d.setDebuglevel(d.GUI+d.NET);
	JAPDebug.printDebugType();
	JAPDebug.out(JAPDebug.DEBUG,JAPDebug.GUI,"This is a GUI related debug message");
	JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"This is a NET related debug message");
	
	System.out.println("JAPDebug: Selftest 2: GUI only");
	JAPDebug.setDebugType(JAPDebug.GUI);
	JAPDebug.printDebugType();
	JAPDebug.out(JAPDebug.DEBUG,JAPDebug.GUI,"This is a GUI related debug message");
	JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"This is a NET related debug message");
	
	System.out.println("JAPDebug: Selftest 3: NET");
	JAPDebug.setDebugType(JAPDebug.NET);
	JAPDebug.printDebugType();
	JAPDebug.out(JAPDebug.DEBUG,JAPDebug.GUI,"This is a GUI related debug message");
	JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"This is a NET related debug message");
	
	System.out.println("JAPDebug: Selftest 4: no debugging");
	JAPDebug.setDebugType(JAPDebug.NUL);
	JAPDebug.printDebugType();
	JAPDebug.out(JAPDebug.DEBUG,JAPDebug.GUI,"This is a GUI related debug message");
	JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"This is a NET related debug message");
	System.out.println("JAPDebug: End");
    }
}

