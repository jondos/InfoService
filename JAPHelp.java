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

public class JAPHelp extends JDialog implements ActionListener {
    private JAPModel model;
    private String helpPath = " ";
    private String helpLang = " ";
    private JComboBox language;
    HtmlPane html;

    public JAPHelp(JFrame f, JAPModel m) {
	super(f, m.msg.getString("helpWindow"), false);

	JPanel container = new JPanel();
	container.setLayout( new BorderLayout() );
	
	/* works but makes no sens to catch here
	try { helpPath = model.msg.getString("helpPath"); }
	catch (Exception e) { helpPath = model.HELPPATH; }
	*/
	helpPath = model.msg.getString("helpPath1");
	
	html = new HtmlPane(helpPath);

	JPanel buttonPanel = new JPanel();
	buttonPanel.setLayout ( new FlowLayout(FlowLayout.RIGHT) );
	
	language = new JComboBox();
	for (int i = 1; i < model.MAXHELPLANGUAGES; i++) {
	    try { 
		helpPath = model.msg.getString("helpPath"+String.valueOf(i)); 
		helpLang = model.msg.getString("lang"+String.valueOf(i));
		language.addItem(helpLang);
	    } catch (Exception e) { ; }
	}
	language.addActionListener(this);
	buttonPanel.add( language );
	buttonPanel.add(new JLabel("   "));
	
	JButton close = new JButton(model.msg.getString("closeButton"));
	close.addActionListener(new ActionListener() {
	                       public void actionPerformed(ActionEvent e) {
				   ClosePressed();
			       }});
	buttonPanel.add( close );
	getRootPane().setDefaultButton(close);
	buttonPanel.add(new JLabel("   "));

	container.add(html, BorderLayout.CENTER);
	container.add(buttonPanel, BorderLayout.SOUTH);
	getContentPane().add(container);
	pack();
	centerDialog();
    }

    protected void centerDialog() {
        Dimension screenSize = this.getToolkit().getScreenSize();
	Dimension ownSize = this.getSize();
	this.setLocation(
		(screenSize.width  - ownSize.width )/2,
		(screenSize.height - ownSize.height)/2
	);
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
	helpPath = model.msg.getString("helpPath"
		    +String.valueOf(language.getSelectedIndex()+1));
	html.load(helpPath);
    }

    public void ClosePressed() {
        this.setVisible(false);
    }
}


class HtmlPane extends JScrollPane implements HyperlinkListener {
    JEditorPane html;

    public HtmlPane(String fn) {
	try {
	    File f = new File (fn);
	    String s = f.getAbsolutePath();
	    s = "file:"+s;
//	    URL url = new URL(s);
	    html = new JEditorPane(s);
	    html.setEditable(false);
	    html.addHyperlinkListener(this);

	    JViewport vp = getViewport();
	    vp.add(html);
	} catch (MalformedURLException e) {
	    System.out.println("Malformed URL: " + e);
	} catch (IOException e) {
	    System.out.println("IOException: " + e);
	}	
    }
    
    public void load(String fn) {
	try {
	    File f = new File (fn);
	    String s = f.getAbsolutePath();
	    s = "file:"+s;
	    URL url = new URL(s);
//	    html = new JEditorPane(s);
//	    html.setEditable(false);
//	    html.addHyperlinkListener(this);

//	    JViewport vp = getViewport();
//	    vp.add(html);
	
//Entweder:	    
//	    html.setPage(s);
//Oder:
	    linkActivated(url);
	    
	} catch (MalformedURLException e) {
	    System.out.println("Malformed URL: " + e);
	} /*catch (IOException e) {
	    System.out.println("IOException: " + e);
	}*/	
    }

    public void hyperlinkUpdate(HyperlinkEvent e) {
	if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
	    linkActivated(e.getURL());
	}
    }

    protected void linkActivated(URL u) {
	Cursor c = html.getCursor();
	Cursor waitCursor = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR);
	html.setCursor(waitCursor);
	SwingUtilities.invokeLater(new PageLoader(u, c));
    }

    class PageLoader implements Runnable {
	
	PageLoader(URL u, Cursor c) {
	    url = u;
	    cursor = c;
	}

        public void run() {
	    if (url == null) {
		// restore the original cursor
		html.setCursor(cursor);

		// PENDING(prinz) remove this hack when 
		// automatic validation is activated.
		Container parent = html.getParent();
		parent.repaint();
	    } else {
		Document doc = html.getDocument();
		try {
		    html.setPage(url);
		} catch (IOException ioe) {
		    html.setDocument(doc);
		    getToolkit().beep();
		} finally {
		    // schedule the cursor to revert after
		    // the paint has happended.
		    url = null;
		    SwingUtilities.invokeLater(this);
		}
	    }
	}

	URL url;
	Cursor cursor;
    }
    
}
