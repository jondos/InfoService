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
// import java.io.PrintWriter;
import java.util.Dictionary;
import java.util.Hashtable;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.Frame;
import java.awt.Dimension;

import javax.swing.JDialog;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;
import javax.swing.JScrollBar;
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
final public class JAPDebug extends WindowAdapter{

	/** No debugging */
	public final static int NUL = 0; 
	/** Indicates a GUI related message (binary: <code>00000001</code>) */
	public final static int GUI = 1;
	/** Indicates a network related message (binary: <code>00000010</code>) */
	public final static int NET = 2; 
	/** Indicates a thread related message (binary: <code>00000100</code>) */
	public final static int THREAD = 4; 
	/** Indicates a misc message (binary: <code>00001000</code>) */
	public final static int MISC = 8; 

	/** Indicates level type of message: Emergency message*/
	public final static int EMERG     = 0; 
	/** Indicates level type of message: Alert message */
	public final static int ALERT     = 1; 
	/** Indicates level type of message: For instance to  use when catching Exeption to output a debug message.*/
	public final static int EXCEPTION = 2; //2000-07-31(HF): CRIT zu EXCEPTION geaendert, wegen besserem Verstaendnis
	/** Indicates level type of message: Error message */
	public final static int ERR       = 3; 
	/** Indicates level type of message: Warning */
	public final static int WARNING   = 4; 
	/** Indicates level type of message: Notice */
	public final static int NOTICE    = 5; 
	/** Indicates level type of message: Information */
	public final static int INFO      = 6; 
	/** Indicates level type of message, e.g. a simple debugging message to output something */
	public final static int DEBUG     = 7; 

	private int debugtype;
	private int debuglevel;
	private JTextArea textareaConsole;
	private JDialog frameConsole;
	private boolean m_bConsole;
	
	private final static String strLevels[]=
		{
			"Emergency",
			"Alert",
			"Exception",
			"Error",
			"Warning",
			"Notice",
			"Info ",
			"Debug"
		};
//	private PrintWriter[] outStreams;
		
	private static JAPDebug debug; 

	private JAPDebug () {
		debugtype=GUI+NET+THREAD+MISC;
		debuglevel=DEBUG;
		m_bConsole=false;
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
		if ( (level <= debug.debuglevel) && (debug.debugtype & type) !=0 ) {
//			debug.outStreams[level].println("JAPDebug: "+txt);
			if(!debug.m_bConsole)
				System.err.println("["+strLevels[level]+"] "+txt);
			else
				{
					debug.textareaConsole.append("["+strLevels[level]+"] "+txt+"\n");
				}
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
		if(level<0||level>DEBUG)
			return;
		debug.debuglevel = level;
	}
	
	/** Get the current debug level.
	 */
	public static int getDebugLevel() {
		return debug.debuglevel;
	}
	
	/** Returns short words discribing each debug-level.
	 */
	public static final String[] getDebugLevels()
		{
			return strLevels;
		}
	
	/** Shows or hiddes a Debug-Console-Window
	 */
	public static void showConsole(boolean b,Frame parent)
		{
			debug.internal_showConsole(b,parent);
		}
	
	public static void setConsoleParent(Frame parent)
		{
			if(debug!=null&&debug.m_bConsole&&debug.frameConsole!=null)
				{
					JDialog tmpDlg=new JDialog(parent,"Debug-Console");
					tmpDlg.getContentPane().add(new JScrollPane(debug.textareaConsole));
					tmpDlg.addWindowListener(debug);
					tmpDlg.setSize(debug.frameConsole.getSize());
					tmpDlg.setLocation(debug.frameConsole.getLocation());
					tmpDlg.setVisible(true);
					debug.frameConsole.dispose();
				}
		}
	public static boolean isShowConsole()
		{
			return debug.m_bConsole;
		}
	
	public void internal_showConsole(boolean b,Frame parent)
		{
			if(!b&&m_bConsole)
				{
					frameConsole.dispose();
					textareaConsole=null;
					frameConsole=null;
					m_bConsole=false;
				}
			else if(b&&!m_bConsole)
				{
					frameConsole=new JDialog(parent,"Debug-Console");
					textareaConsole=new JTextArea(null,20,30);
					textareaConsole.setEditable(false);
					frameConsole.getContentPane().add(new JScrollPane(textareaConsole));
					frameConsole.addWindowListener(this);
					frameConsole.pack();
					Dimension screenSize=frameConsole.getToolkit().getScreenSize();
					Dimension ownSize=frameConsole.getSize();
					frameConsole.setLocation((screenSize.width-ownSize.width),0);
					frameConsole.setVisible(true);
					m_bConsole=true;
				}
		}
	
		public void windowClosing(WindowEvent e)
			{
				m_bConsole=false;
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
	public static void main(String argc[]) {
	JAPDebug.create();
	
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