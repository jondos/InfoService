/*
 Copyright (c) 2000 - 2004, The JAP-Team
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

import java.util.Random;

import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import gui.GUIUtils;
import gui.IStatusLine;
import gui.JAPMessages;

/** A panel which display some status messages, one after each other*/
public class StatusPanel extends JPanel implements Runnable, IStatusLine
{
	private static final String MSG_CLICK_HERE = StatusPanel.class.getName() + "_clickHere";

	private Dimension m_dimensionPreferredSize;
	private int m_Height;
	private final Object SYNC_MSG = new Object();
	private Random m_Random;
	private final Object SYNC_ADD_MSG = new Object();

	private final static int ICON_HEIGHT = 15;
	private final static int ICON_WIDTH = 16;

	private Image m_imageError;
	private Image m_imageInformation;
	private Image m_imageWarning;

	private final class MsgQueueEntry
	{
		ActionListener listener;
		String m_Msg;
		Image m_Icon;
		int m_Id;
		MsgQueueEntry m_Next;
		int m_DisplayCount = -1;
	}

	private MsgQueueEntry m_Msgs;
	private volatile boolean m_bRun;
	private volatile int m_aktY;
	private Thread m_Thread;
	private int m_idyFont;

	public StatusPanel()
	{
		m_imageInformation = GUIUtils.loadImageIcon(JAPConstants.IMAGE_INFORMATION, true, false).getImage();
		m_imageError = GUIUtils.loadImageIcon(JAPConstants.IMAGE_ERROR, true, false).getImage();
		m_imageWarning = GUIUtils.loadImageIcon(JAPConstants.IMAGE_WARNING, true, false).getImage();

		addMouseListener(new MouseAdapter()
		{
			boolean m_bClicked = false;

			public void mouseClicked(MouseEvent a_event)
			{
				ActionListener listener = null;
				if (m_bClicked)
				{
					return;
				}
				m_bClicked = true;

				synchronized (SYNC_MSG)
				{
					if (m_Msgs != null)
					{
						listener = m_Msgs.listener;
					}
				}
				if (listener != null)
				{
					listener.actionPerformed(new ActionEvent(StatusPanel.this, a_event.getID(),
						"mouseClicked"));
					StatusPanel.this.repaint();
				}

				m_bClicked = false;
			}
		});

		m_Random = new Random();
		Font font = new JLabel("Status").getFont();
		m_Height = (int) (font.getSize() * 0.7);
		m_Height = Math.min(m_Height, ICON_HEIGHT);
		font = new Font(font.getName(), Font.PLAIN, m_Height);
		m_dimensionPreferredSize = new Dimension(100, (int) (ICON_HEIGHT * 1.2));
		m_idyFont = (ICON_HEIGHT - font.getSize()) / 2;
		setLayout(null);
		//setFont(font);
		//setBackground(Color.red);
		//setSize(m_dimensionPreferredSize);
		m_Msgs = null;
		m_Thread = new Thread(this, "StatusPanel");
		m_Thread.setDaemon(true);
		m_bRun = true;
		m_Thread.start();
	}

	public void finalize()
	{
		m_bRun = false;
		try
		{
			m_Thread.interrupt();
			m_Thread.join();
		}
		catch (Exception e)
		{
		}
	}

	/** Adds a message to be displayed in the status panel.
	 * @param type chose one of JOptionPane.*
	 * @param msg the message to be displayed
	 * @return an id useful for removing this message from the status panel
	 */
	public int addStatusMsg(String msg, int type, boolean bAutoRemove)
	{
		return addStatusMsg(msg, type, bAutoRemove, null);
	}

	/** Adds a message to be displayed in the status panel.
	 * @param type chose one of JOptionPane.*
	 * @param msg the message to be displayed
	 * @return an id >= 0 useful for removing this message from the status panel
	 */
	public int addStatusMsg(String msg, int type, boolean bAutoRemove, ActionListener a_listener)
	{
		MsgQueueEntry entry = null;
		synchronized (SYNC_MSG)
		{
			entry = new MsgQueueEntry();
			entry.listener = a_listener;
			entry.m_Msg = msg;
			entry.m_Id = Math.abs(m_Random.nextInt());
			if (bAutoRemove)
			{
				entry.m_DisplayCount = 2;
			}
			if (type == JOptionPane.WARNING_MESSAGE)
			{
				entry.m_Icon = m_imageWarning;
			}
			else if (type == JOptionPane.INFORMATION_MESSAGE)
			{
				entry.m_Icon = m_imageInformation;
			}
			else if (type == JOptionPane.ERROR_MESSAGE)
			{
				entry.m_Icon = m_imageError;
			}

			if (m_Msgs == null)
			{
				m_Msgs = entry;
				entry.m_Next = entry;
				m_aktY = 0;
			}
			else
			{
				entry.m_Next = m_Msgs.m_Next;
				m_Msgs.m_Next = entry;
			}
			m_Thread.interrupt(); //display next message
		}

		return entry.m_Id;

	}

	/** Removes a message from the ones which are displayed in the status panel
	 * @param id the message to be removed
	 */
	public void removeStatusMsg(int id)
	{
		synchronized (SYNC_MSG)
		{
			if (m_Msgs == null) //zero elements
			{
				m_aktY = 0;
				return;
			}
			if (m_Msgs.m_Id == id && m_Msgs.m_Next == m_Msgs) //one element
			{
				m_Msgs = null;
				m_aktY = 0;
			}
			else
			{
				//more than one
				MsgQueueEntry entry = m_Msgs;
				MsgQueueEntry prev = null;
				while (entry != null)
				{
					if (entry.m_Next.m_Id == id)
					{
						prev = entry;
						entry = entry.m_Next;
						break;
					}
					entry = entry.m_Next;
					if (entry == m_Msgs)
					{
						return; //not found
					}
				}
				if (entry == m_Msgs) //remove curent entry
				{
					m_Msgs = entry.m_Next;
					m_aktY = 0;
					m_Thread.interrupt(); //display changes
				}
				prev.m_Next = entry.m_Next; //remove entry from list
			}
		}


	}

	public void paint(Graphics g)
	{
		if (g == null)
		{
			return;
		}
		super.paint(g);
		synchronized (SYNC_MSG)
		{
			if (m_Msgs != null)
			{
				String msg = m_Msgs.m_Msg;
				if (m_Msgs.listener != null)
				{
					setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
					msg += " (" + JAPMessages.getString(MSG_CLICK_HERE) + ")";
					setToolTipText(JAPMessages.getString(MSG_CLICK_HERE));
					//g.setColor(Color.blue);
				}
				else
				{
					//g.setColor(Color.black);
					setToolTipText(null);
					setCursor(Cursor.getDefaultCursor());
				}
				g.drawString(msg, ICON_WIDTH + 2, m_aktY - m_idyFont);
				if (m_Msgs.m_Icon != null)
				{
					g.drawImage(m_Msgs.m_Icon, 0, m_aktY - ICON_HEIGHT, this);
				}
			}
		}
	}

	public Dimension getPreferredSize()
	{
		return m_dimensionPreferredSize;
	}

	public Dimension getMinimumSize()
	{
		return m_dimensionPreferredSize;
	}

	public void run()
	{
		try
		{
			//boolean bInterrupted = false;
			while (m_bRun)
			{
				try
				{
					Thread.sleep(10000);
				}
				catch (InterruptedException e)
				{
					if (!m_bRun)
					{
						return;
					}
				}

				synchronized (SYNC_MSG)
				{
					if (m_Msgs != null && m_Msgs.m_DisplayCount == 0)
					{
						removeStatusMsg(m_Msgs.m_Id);
					}

					if (m_Msgs == null)
					{
						repaint();
						continue;
					}
					if (m_Msgs.m_DisplayCount > 0)
					{
						m_Msgs.m_DisplayCount--;
					}

					if (m_Msgs == null)
					{
						m_aktY = 0;
						repaint();
						continue;
					}
					else if (m_Msgs.m_Next == m_Msgs && m_Msgs.listener != null && m_aktY == (ICON_HEIGHT + 1))
					{
						// there are no other status messages; leave this one on top
						repaint();
						continue;
					}
					else
					{
						m_Msgs = m_Msgs.m_Next;
						m_aktY = 0;
					}
				}


				for (int i = 0; i < ICON_HEIGHT + 1 && m_bRun; i++)
				{
					try
					{
						repaint();
						Thread.sleep(100);
						m_aktY++;
					}
					catch (InterruptedException e)
					{
						synchronized (SYNC_MSG)
						{
							if (m_Msgs != null)
							{
								m_aktY = 0;
								i = -1;
								m_Msgs = m_Msgs.m_Next;
							}
						}
					}
				}

			}
		}
		catch (Exception e)
		{
		}
	}
}
