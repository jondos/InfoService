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

final public class JAPHelp extends JDialog implements ActionListener {
    private JAPModel model;
    private String helpPath = " ";
    private String helpLang = " ";
    private JComboBox language;
    HtmlPane html;

    public JAPHelp(JFrame f) 
			{
				super(f, JAPModel.getString("helpWindow"), false);
				model = JAPModel.getModel();
				init();
			}
		
		final private void init()
			{
				//JPanel container = new JPanel();
				//container.setLayout( new BorderLayout() );
				//getContentPane().setLayout(new BorderLayout());
	
				html = new HtmlPane(model.getString("helpPath1"));

				JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
	
				language = new JComboBox();
				buttonPanel.add( language );
				buttonPanel.add(new JLabel("   "));
	
				JButton close = new JButton(model.getString("closeButton"));
				buttonPanel.add( close );
				buttonPanel.add(new JLabel("   "));

				
				getContentPane().add(html, BorderLayout.CENTER);
				getContentPane().add(buttonPanel, BorderLayout.SOUTH);
				//getContentPane().add(container);
				getRootPane().setDefaultButton(close);
				close.addActionListener(this);
				language.addActionListener(this);
				for (int i = 1; i < model.MAXHELPLANGUAGES; i++) {
					try 
						{ 
							helpPath = model.getString("helpPath"+String.valueOf(i)); 
							helpLang = model.getString("lang"+String.valueOf(i));
							// This checks if the entry exists in the properties file
							// if yes, the item will be added
							if (( helpLang.equals("lang"+String.valueOf(i)) )!= true)
								language.addItem(helpLang);
						}
							catch (Exception e) {}
					}
				pack();
				JAPModel.centerFrame(this);
			}

    
    public Dimension getPreferredSize()
			{
				Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
				d.width = Math.min(d.width - 50, 400);
				d.height = Math.min(d.height - 80, 300);
				return (d);
			}
    
    public void actionPerformed(ActionEvent e) 
			{
				// for Language Combobox AND Close Burtton only
				if(e.getSource()==language)
					{
						helpPath = model.getString("helpPath"+String.valueOf(language.getSelectedIndex()+1));
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

