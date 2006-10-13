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

package jap;


import logging.Log;

import java.io.File;
import java.io.FileWriter;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Properties;
import java.util.Enumeration;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.Frame;
import java.awt.Font;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JScrollPane;
import javax.swing.JScrollBar;

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
final public class JAPDebug extends WindowAdapter implements ActionListener,Log {

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

	private static int debugtype=GUI+NET+THREAD+MISC;
	private static int debuglevel=DEBUG;
	private static JTextArea textareaConsole;
	private static JDialog frameConsole;
	private static boolean m_bConsole=false;
//	private PrintWriter[] outStreams;
	private static JAPDebug debug;
	private static SimpleDateFormat dateFormatter=new SimpleDateFormat ("yyyy/MM/dd-hh:mm:ss, ");

	private final static String strLevels[]=
		{
			"Emergency",
			"Alert    ",
			"Exception",
			"Error    ",
			"Warning  ",
			"Notice   ",
			"Info     ",
			"Debug    "
		};


	private JAPDebug () {
		debugtype=GUI+NET+THREAD+MISC;
		debuglevel=DEBUG;
		m_bConsole=false;
//		outStreams=new PrintWriter[8];
//		for(int i=0;i<8;i++)
//			outStreams[i]=new PrintWriter(System.out);
//		dateFormatter= new SimpleDateFormat ("yyyy/MM/dd-hh:mm:ss, ");
	}
	public  static JAPDebug create() {
		if(debug==null)
			debug = new JAPDebug();
		return debug;
	}

	/** Output a debug message.
	 *  @param level The level of the debugging message (EMERG,ALERT,CRIT,ERR,WARNING,NOTICE,INFO,DEBUG)
	 *  @param type The type of the debugging message (GUI, NET, THREAD, MISC)
	 *  @param txt   The message itself
	 */
	public static void out(int level, int type, String txt) {
		if(debug==null)
			JAPDebug.create();
		if ( (level <= debug.debuglevel) && (debug.debugtype & type) !=0 ) {
//			debug.outStreams[level].println("JAPDebug: "+txt);
			synchronized(debug) {
				String str="["+dateFormatter.format(new Date())+strLevels[level]+"] "+txt+"\n";
				if(!debug.m_bConsole)
					System.err.print(str);
				else {
					debug.textareaConsole.append(str);
					debug.textareaConsole.setCaretPosition(debug.textareaConsole.getText().length());
				}
			}
		}
	}

  public void log(int level,int type,String msg)
    {
      out(level,type,msg);
    }

	/** Set the debugging type you like to output. To activate more than one type you simly add
	 *  the types like this <code>setDebugType(JAPDebug.GUI+JAPDebug.NET)</code>.
	 *  @param type The debug type (NUL, GUI, NET, THREAD, MISC)
	 */
	public static void setDebugType(int type) {
		if(debug==null)
			JAPDebug.create();
		debug.debugtype = type;
	}

	/** Get the current debug type.
	 */
	public static int getDebugType() {
		if(debug==null)
			JAPDebug.create();
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
		if(debug==null)
			JAPDebug.create();
		debug.debuglevel = level;
	}

	/** Get the current debug level.
	 */
	public static int getDebugLevel() {
		if(debug==null)
			JAPDebug.create();
		return debug.debuglevel;
	}

	/** Returns short words discribing each debug-level.
	 */
	public static final String[] getDebugLevels() {
		return strLevels;
	}

	/** Shows or hiddes a Debug-Console-Window
	 */
	public static void showConsole(boolean b,Frame parent) {
		debug.internal_showConsole(b,parent);
	}

	public static void setConsoleParent(Frame parent) {
		if((debug!=null)&&(debug.m_bConsole)&&(debug.frameConsole!=null)) {
			JDialog tmpDlg=new JDialog(parent,"Debug-Console");
			//tmpDlg.getContentPane().add(new JScrollPane(debug.textareaConsole));
			tmpDlg.setContentPane(debug.frameConsole.getContentPane());
			tmpDlg.addWindowListener(debug);
			tmpDlg.setSize(debug.frameConsole.getSize());
			tmpDlg.setLocation(debug.frameConsole.getLocation());
			tmpDlg.setVisible(true);
			debug.frameConsole.dispose();
			debug.frameConsole=tmpDlg;
		}
	}
	public static boolean isShowConsole() {
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
					Font f=Font.decode("Courier");
          if(f!=null)
            textareaConsole.setFont(f);
          JPanel panel=new JPanel();
					JButton bttnSave=new JButton(JAPMessages.getString("bttnSaveAs")+"...",
																			 JAPUtil.loadImageIcon("saveicon.gif",true));
					bttnSave.setActionCommand("saveas");
					bttnSave.addActionListener(debug);
					JButton bttnCopy=new JButton(JAPMessages.getString("bttnCopy"),
																			 JAPUtil.loadImageIcon("copyicon.gif",true));
					bttnCopy.setActionCommand("copy");
					bttnCopy.addActionListener(debug);
					JButton bttnInsertConfig=new JButton(JAPMessages.getString("bttnInsertConfig"));
					bttnInsertConfig.setActionCommand("insertConfig");
					bttnInsertConfig.addActionListener(debug);
					JButton bttnDelete=new JButton(JAPMessages.getString("bttnDelete"),
																			 JAPUtil.loadImageIcon("deleteicon.gif",true));
					bttnDelete.setActionCommand("delete");
					bttnDelete.addActionListener(debug);
					JButton bttnClose=new JButton(JAPMessages.getString("bttnClose"),
																				JAPUtil.loadImageIcon("exiticon.gif",true));
					bttnClose.setActionCommand("close");
					bttnClose.addActionListener(debug);
					GridBagLayout g=new GridBagLayout();
					panel.setLayout(g);
					GridBagConstraints c=new GridBagConstraints();
					c.insets=new Insets(5,5,5,5);
					c.gridy=0;
					c.gridx=1;
					c.weightx=0;
					g.setConstraints(bttnSave,c);
					panel.add(bttnSave);
					c.gridx=2;
					g.setConstraints(bttnCopy,c);
					panel.add(bttnCopy);
					c.gridx=3;
					g.setConstraints(bttnInsertConfig,c);
					panel.add(bttnInsertConfig);
					c.gridx=4;
					g.setConstraints(bttnDelete,c);
					panel.add(bttnDelete);
					c.weightx=1;
					c.anchor=c.EAST;
					c.fill=c.NONE;
					c.gridx=5;
					g.setConstraints(bttnClose,c);
					panel.add(bttnClose);
					//panel.add("Center",new Canvas());
					frameConsole.getContentPane().add("North",panel);
					frameConsole.getContentPane().add("Center",new JScrollPane(textareaConsole));
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

		public void actionPerformed(ActionEvent e)
			{
				if(e.getActionCommand().equals("saveas"))
					{
						saveLog();
					}
				else if(e.getActionCommand().equals("copy"))
					{
						textareaConsole.selectAll();
						textareaConsole.copy();
						textareaConsole.moveCaretPosition(textareaConsole.getCaretPosition());
//						PrintJob p=Toolkit.getDefaultToolkit().getPrintJob(JAPModel.getModel().getView(),"Print Log",null);
//						debug.textareaConsole.print(p.getGraphics());
//						p.end();
					}
				else if(e.getActionCommand().equals("delete"))
					{
						textareaConsole.setText("");
					}
				else if(e.getActionCommand().equals("insertConfig"))
					{
            try
              {
                Properties p=System.getProperties();
                //StringWriter s=new StringWriter();
                //p.list(new PrintWriter(s));
                Enumeration enum=p.propertyNames();
                while(enum.hasMoreElements())
                  {
                    String st=(String)enum.nextElement();
                    String value=p.getProperty(st);
                    textareaConsole.append(st+": "+value+"\n");
                  }
//                textareaConsole.append(s.toString());
              }
            catch(Exception e1)
              {}
            textareaConsole.append(JAPModel.getModel().toString());
					}
				else
					{
						frameConsole.dispose();
						m_bConsole=false;
					}
			}

		private void saveLog()
			{
				JFileChooser fc=new JFileChooser();
				fc.setDialogType(fc.SAVE_DIALOG);
				int ret=fc.showDialog(debug.frameConsole,null);
				if(ret==fc.APPROVE_OPTION)
					{
						File file=fc.getSelectedFile();
						try
							{
								FileWriter fw=new FileWriter(file);
								debug.textareaConsole.write(fw);
								fw.flush();
								fw.close();
							}
						catch(Exception e)
							{
								JOptionPane.showMessageDialog(debug.frameConsole,
																							JAPMessages.getString("errWritingLog"),
																							JAPMessages.getString("error"),
																							JOptionPane.ERROR_MESSAGE);
							}
					}
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

	}
