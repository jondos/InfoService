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
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import java.net.URL;
import java.net.MalformedURLException;
import java.io.*;
import javax.swing.text.*;
import javax.swing.event.*;

/* classes modified from Swing Example "Metalworks" */

final class JAPHelp extends JDialog implements ActionListener {
    private JAPModel model;
    private String helpPath = " ";
    private String helpLang = " ";
    private JComboBox language;
    HtmlPane html;

    public JAPHelp(JFrame f) 
			{
				super(f, JAPMessages.getString("helpWindow"), false);
				model = JAPModel.getModel();
				init();
			}
		
		final private void init()
			{
				//JPanel container = new JPanel();
				//container.setLayout( new BorderLayout() );
				//getContentPane().setLayout(new BorderLayout());
	
				html = new HtmlPane(JAPMessages.getString("helpPath1"));

				JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
	
				language = new JComboBox();
				buttonPanel.add( language );
				buttonPanel.add(new JLabel("   "));
	
				JButton close = new JButton(JAPMessages.getString("closeButton"));
				buttonPanel.add( close );
				buttonPanel.add(new JLabel("   "));

				
				getContentPane().add(html, BorderLayout.CENTER);
				getContentPane().add(buttonPanel, BorderLayout.SOUTH);
				//getContentPane().add(container);
				getRootPane().setDefaultButton(close);
				close.addActionListener(this);
				language.addActionListener(this);
				for (int i = 1; i < JAPConstants.MAXHELPLANGUAGES; i++) {
					try 
						{ 
							helpPath = JAPMessages.getString("helpPath"+String.valueOf(i)); 
							helpLang = JAPMessages.getString("lang"+String.valueOf(i));
							// This checks if the entry exists in the properties file
							// if yes, the item will be added
							if (( helpLang.equals("lang"+String.valueOf(i)) )!= true)
								language.addItem(helpLang);
						}
							catch (Exception e) {}
					}
				pack();
				JAPUtil.centerFrame(this);
			}

    
    public Dimension getPreferredSize()
			{
				Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
				d.width = Math.min(d.width - 50, 600/*400*/);
				d.height = Math.min(d.height - 80, 350/*300*/);
				return (d);
			}
    
    public void actionPerformed(ActionEvent e) 
			{
				// for Language Combobox AND Close Burtton only
				if(e.getSource()==language)
					{
						helpPath = JAPMessages.getString("helpPath"+String.valueOf(language.getSelectedIndex()+1));
						html.load(helpPath);
					}
				else
					closePressed();
			}

    private void closePressed() {
        setVisible(false);
    }
}


final class HtmlPane extends JScrollPane implements HyperlinkListener 
	{
    private JEditorPane html;
		private URL url;
		private Cursor cursor;

    public HtmlPane(String fn) 
			{
				html=new JEditorPane();
				html.setEditable(false);
				html.addHyperlinkListener(this);
				try
					{
						html.setPage(getUrlFor(fn));
					}
				catch(Exception e){}
				getViewport().add(html);
				cursor=html.getCursor(); // ??? (hf)
			}
    
		private URL getUrlFor(String fn)
			{
				// used to find help files within a .jar file
				try  
					{
						URL url = getClass().getResource(fn);
						if(url!=null)
							return url;
					}
				catch (Exception e) {
						JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"JAPHelp:load:Exception: " + e);
						JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"URL was: " + fn);
					}
				// ...else
				try
					{
						File f = new File (fn);
						String s = f.getAbsolutePath();
						s = "file:"+s;
						return new URL(s);
					}
				catch (Exception e)
					{
						JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"JAPHelp:HtmlPane(constructor):Exception: " + e);
						JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"URL was: " + fn);
					}
				return null;
			}
		
    public void load(String fn)
			{
				URL url=getUrlFor(fn);
				if(url != null)
					linkActivated(url);
			}

    public void hyperlinkUpdate(HyperlinkEvent e)
			{
				if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED)
					{
						linkActivated(e.getURL());
					}
				else if(e.getEventType()==HyperlinkEvent.EventType.ENTERED)
					{
						html.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
					}
				else if(e.getEventType()==HyperlinkEvent.EventType.EXITED)
					html.setCursor(cursor);
			}

    protected void linkActivated(URL u) 
			{
				Cursor waitCursor = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR);
				html.setCursor(waitCursor);
				SwingUtilities.invokeLater(new PageLoader(u));
			}

    final class PageLoader implements Runnable
			{
				PageLoader(URL u)
					{
						url = u;
					}

        public void run()
					{
						if (url == null)
							{
								// restore the original cursor
								html.setCursor(cursor);
								// PENDING(prinz) remove this hack when 
								// automatic validation is activated.
								html.getParent().repaint();
							}
						else
							{
								Document doc = html.getDocument();
								try
									{
										html.setPage(url);
									}
								catch (IOException ioe)
									{
										html.setDocument(doc);
										getToolkit().beep();
									} 
								finally
									{
										// schedule the cursor to revert after
										// the paint has happended.
										url = null;
										SwingUtilities.invokeLater(this);
									}
							}
					}
			}
 }

