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

import java.io.File;
import java.io.IOException;
import java.net.URL;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JEditorPane;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import javax.swing.event.HyperlinkEvent;
import javax.swing.event.HyperlinkListener;
import javax.swing.text.Document;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.Vector;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/* classes modified from Swing Example "Metalworks" */

final class JAPHelp extends JDialog implements ActionListener, PropertyChangeListener, WindowListener
{
	private String helpPath = " ";
	private String helpLang = " ";
	private String langShort = " ";
	private JComboBox language;
	HtmlPane html;

	private JButton m_closeButton;
	private JButton m_backButton;
	private JButton m_forwardButton;

	private JDialog m_virtualParent;

	private boolean m_initializing;

	public JAPHelp(JFrame parent)
	{
		super(parent, JAPMessages.getString("helpWindow"), false);
		init();
	}

	/**
	 * If the parent is a JDialog, we have to use it as virtualParent. It then gets
	 * invisble when the help window arises and visible again when the help window
	 * is closed. This is a workaround for the missing ability to make a JDialog parent
	 * of another JDialog in JDK 1.1.8 (Swing).
	 * @param parent JFrame
	 * @param virtualParent JDialog
	 */
	public JAPHelp(JFrame parent, JDialog virtualParent)
	{
		super(parent, JAPMessages.getString("helpWindow"), false);
		m_virtualParent = virtualParent;
		m_virtualParent.setVisible(false);
		init();
	}

	final private void init()
	{
		m_initializing = true;
		html = new HtmlPane();
		html.addPropertyChangeListener(this);
		this.addWindowListener(this);

		JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));

		language = new JComboBox();
		buttonPanel.add(language);
		buttonPanel.add(new JLabel("   "));

		m_backButton = new JButton(JAPMessages.getString("backButton"));
		buttonPanel.add(m_backButton);
		m_forwardButton = new JButton(JAPMessages.getString("forwardButton"));
		buttonPanel.add(m_forwardButton);
		buttonPanel.add(new JLabel("   "));
		m_closeButton = new JButton(JAPMessages.getString("closeButton"));
		buttonPanel.add(m_closeButton);
		buttonPanel.add(new JLabel("   "));
		m_forwardButton.setEnabled(false);
		m_backButton.setEnabled(false);

		getContentPane().add(html, BorderLayout.CENTER);
		getContentPane().add(buttonPanel, BorderLayout.SOUTH);
		//getContentPane().add(container);
		getRootPane().setDefaultButton(m_closeButton);
		m_closeButton.addActionListener(this);
		m_backButton.addActionListener(this);
		m_forwardButton.addActionListener(this);
		language.addActionListener(this);
		for (int i = 1; i < JAPConstants.MAXHELPLANGUAGES; i++)
		{
			try
			{
				String path = JAPMessages.getString("helpPath" + String.valueOf(i));

				helpLang = JAPMessages.getString("lang" + String.valueOf(i));
				String lshort = JAPMessages.getString("langshort" + String.valueOf(i));

				// This checks if the entry exists in the properties file
				// if yes, the item will be added
				if ( (helpLang.equals("lang" + String.valueOf(i))) != true)
				{
					language.addItem(helpLang);
				}

				// Make sure to use the language with number 1 listed in the properties file
				if (helpPath.equals(" ") && langShort.equals(" "))
				{
					helpPath = path;
					langShort = lshort;
				}
			}
			catch (Exception e)
			{
			}
		}
		pack();
		JAPUtil.centerFrame(this);
		m_initializing = false;
	}

	public Dimension getPreferredSize()
	{
		Dimension d = Toolkit.getDefaultToolkit().getScreenSize();
		d.width = Math.min(d.width - 50, 600 /*400*/);
		d.height = Math.min(d.height - 80, 350 /*300*/);
		return (d);
	}

	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == language && !m_initializing)
		{
			helpPath = JAPMessages.getString("helpPath" + String.valueOf(language.getSelectedIndex() + 1));
			langShort = JAPMessages.getString("langshort" + String.valueOf(language.getSelectedIndex() + 1));
			html.load(helpPath + JAPController.getInstance().getHelpContext().getContext() + "_" + langShort +
					  ".html");
		}
		else if (e.getSource() == m_closeButton)
		{
			closePressed();
		}
		else if (e.getSource() == m_backButton)
		{
			backPressed();
		}
		else if (e.getSource() == m_forwardButton)
		{
			forwardPressed();
		}
	}

	private void closePressed()
	{
		setVisible(false);
		if (m_virtualParent != null)
		{
			m_virtualParent.setVisible(true);
		}
	}

	private void backPressed()
	{
		html.goBack();
		checkNavigationButtons();
	}

	private void forwardPressed()
	{
		html.goForward();
		checkNavigationButtons();
	}

	/**
	 * Checks whether to enable or disable the forward and back buttons
	 */
	private void checkNavigationButtons()
	{
		if (html.backAllowed())
		{
			m_backButton.setEnabled(true);
		}
		else
		{
			m_backButton.setEnabled(false);
		}

		if (html.forwardAllowed())
		{
			m_forwardButton.setEnabled(true);
		}
		else
		{
			m_forwardButton.setEnabled(false);
		}
	}

	public void loadCurrentContext()
	{
		String currentContext = JAPController.getInstance().getHelpContext().getContext();
		html.load(helpPath + currentContext + "_" + langShort + ".html");
		if (!this.isVisible())
		{
			this.show();
		}
	}


	/**
	 * Listens to events fired by the HtmlPane in order to update the history buttons
	 * @param a_e PropertyChangeEvent
	 */
	public void propertyChange(PropertyChangeEvent a_e)
	{
		if (a_e.getSource() == html)
		{
			checkNavigationButtons();
		}
	}

	public void windowOpened(WindowEvent e)
	{
	}

	public void windowClosing(WindowEvent e)
	{
		closePressed();
	}

	public void windowClosed(WindowEvent e)
	{
	}

	public void windowIconified(WindowEvent e)
	{
	}

	public void windowDeiconified(WindowEvent e)
	{
	}

	public void windowActivated(WindowEvent e)
	{
	}

	public void windowDeactivated(WindowEvent e)
	{
	}
}

final class HtmlPane extends JScrollPane implements HyperlinkListener
{
	private JEditorPane html;
	private URL url;
	private Cursor cursor;
	private Vector m_history;
	private int m_historyPosition;

	public HtmlPane()
	{
		html = new JEditorPane();
		html.setEditable(false);
		html.addHyperlinkListener(this);
		m_history = new Vector();
		m_historyPosition = -1;

		getViewport().add(html);
		cursor = html.getCursor(); // ??? (hf)
	}

	public JEditorPane getPane()
	{
		return html;
	}

	/**
	 * Goes back in the history and loads the appropriate file
	 */
	public void goBack()
	{
		m_historyPosition--;
		this.loadURL( (URL) m_history.elementAt(m_historyPosition));
	}

	/**
	 * Goes forward in the history and loads the appropriate file
	 */
	public void goForward()
	{
		m_historyPosition++;
		this.loadURL( (URL) m_history.elementAt(m_historyPosition));
	}

	/**
	 * Adds the given URL to the browser history
	 * @param a_url URL
	 */
	private void addToHistory(URL a_url)
	{
		m_history.insertElementAt(a_url, ++m_historyPosition);
	}

	private URL getUrlFor(String fn)
	{
		// used to find help files within a .jar file
		try
		{
			URL url = Class.forName("JAP").getResource(fn);
			if (url != null)
			{
				return url;
			}
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.MISC, "JAPHelp:load:Exception: " + e);
			LogHolder.log(LogLevel.DEBUG, LogType.MISC, "URL was: " + fn);
		}
		// ...else
		try
		{
			File f = new File(fn);
			String s = f.getAbsolutePath();
			s = "file:" + s;
			return new URL(s);
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.MISC, "JAPHelp:HtmlPane(constructor):Exception: " + e);
			LogHolder.log(LogLevel.DEBUG, LogType.MISC, "URL was: " + fn);
		}
		return null;
	}

	public void load(String fn)
	{
		URL url = getUrlFor(fn);
		if (url != null)
		{
			linkActivated(url);
		}
	}

	public void hyperlinkUpdate(HyperlinkEvent e)
	{
		if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED)
		{
			linkActivated(e.getURL());
		}
		else if (e.getEventType() == HyperlinkEvent.EventType.ENTERED)
		{
			html.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		}
		else if (e.getEventType() == HyperlinkEvent.EventType.EXITED)
		{
			html.setCursor(cursor);
		}
	}

	protected void linkActivated(URL u)
	{
		Cursor waitCursor = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR);
		html.setCursor(waitCursor);
		SwingUtilities.invokeLater(new PageLoader(u));
		//Update history
		this.addToHistory(u);
		this.cleanForwardHistory();
		//Make sure the window updates its history buttons
		this.firePropertyChange("CheckButtons", false, true);
	}

	/**
	 * Removes all entries from the forward history
	 */
	private void cleanForwardHistory()
	{
		for (int i = m_history.size() - 1; i > m_historyPosition; i--)
		{
			m_history.removeElementAt(i);
		}
	}

	/**
	 * Returns true if there are entries in the back history
	 * @return boolean
	 */
	public boolean backAllowed()
	{
		if (m_historyPosition <= 0)
		{
			return false;
		}
		else
		{
			return true;
		}
	}

	/**
	 * Returns true if there are entries in the forward history
	 * @return boolean
	 */
	public boolean forwardAllowed()
	{
		if (m_history.size() - 1 > m_historyPosition)
		{
			return true;
		}
		else
		{
			return false;
		}
	}

	/**
	 * Loads URL without adding it to the history
	 * @param a_url URL
	 */
	protected void loadURL(URL a_url)
	{
		Cursor waitCursor = Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR);
		html.setCursor(waitCursor);
		SwingUtilities.invokeLater(new PageLoader(a_url));
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
