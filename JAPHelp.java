import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import javax.swing.*;
import java.net.URL;
import java.net.MalformedURLException;
import java.io.IOException;
import javax.swing.text.Document;
import javax.swing.event.HyperlinkListener;
import javax.swing.event.HyperlinkEvent;

/* classes modified from Swing Example "Metalworks" */

public final class JAPHelp extends JDialog implements ActionListener 
	{
    private JAPModel model;
    private String helpPath;
    private String helpLang;
    private JComboBox language;
    private HtmlPane html;

    public JAPHelp(JFrame f)
			{
				super(f, JAPModel.getString("helpWindow"), false);
				model=JAPModel.getModel();
				JPanel container = new JPanel();
				container.setLayout( new BorderLayout() );
	
				helpPath = model.getString("helpPath1");
	
				html = new HtmlPane(helpPath);

				JPanel buttonPanel = new JPanel();
				buttonPanel.setLayout ( new FlowLayout(FlowLayout.RIGHT) );
	
				language = new JComboBox();
				for (int i = 1; i < model.MAXHELPLANGUAGES; i++)
					{
				    try { 
									helpPath = model.getString("helpPath"+String.valueOf(i)); 
									helpLang = model.getString("lang"+String.valueOf(i));
									language.addItem(helpLang);
								}
						catch (Exception e) 
							{
							}
					}
				language.addActionListener(this);
				buttonPanel.add( language );
				buttonPanel.add(new JLabel("   "));
				JButton close = new JButton(model.getString("closeButton"));
				close.addActionListener(new ActionListener()
					{
						public void actionPerformed(ActionEvent e)
							{
								ClosePressed();
							}
					});
				buttonPanel.add( close );
				getRootPane().setDefaultButton(close);
				buttonPanel.add(new JLabel("   "));

				container.add(html, BorderLayout.CENTER);
				container.add(buttonPanel, BorderLayout.SOUTH);
				getContentPane().add(container);
				//setBackground(getContentPane().getBackground()); //a stupid but schönermach bugfix...
				pack();
				model.centerFrame(this);
    }

    
    public Dimension getPreferredSize()
			{
				Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
				d.width = Math.min(d.width - 50, 400);
				d.height = Math.min(d.height - 80, 300);
				return (d);
			}
    
    public void actionPerformed(ActionEvent e) {
	// for Language Combobox only
	helpPath = model.getString("helpPath"
		    +String.valueOf(language.getSelectedIndex()+1));
	html.load(helpPath);
    }

    public void ClosePressed() {
        this.setVisible(false);
    }
}


final class HtmlPane extends JScrollPane implements HyperlinkListener 
	{
    private JEditorPane html;
		private URL url;
		private Cursor cursor;

    public HtmlPane(String fn)
			{
				try
					{
						html = new JEditorPane(getClass().getResource(fn));
						html.setEditable(false);
						html.addHyperlinkListener(this);

						JViewport vp = getViewport();
						vp.add(html);
						cursor=html.getCursor();
					}
				catch (Exception e)
					{
						JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"Exception: " + e);
					}	
			}
    
    public void load(String fn)
			{
				try 
					{
						URL url = getClass().getResource(fn);
						linkActivated(url);
					}
				catch (Exception e)
					{
						JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"Exception: " + e);
					}
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
