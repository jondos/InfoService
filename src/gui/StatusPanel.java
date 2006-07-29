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
package gui;

import jap.JAPConstants;

import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Image;
import java.util.Random;

import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

/** A panel which display some status messages, one after each other*/
public class StatusPanel extends JPanel implements Runnable, IStatusLine
{
	Dimension m_dimensionPreferredSize;
	int m_Height;
	Object oMsgSync;
	private Random m_Random;

	private final static int ms_IconHeight = 15;
	private final static int ms_IconWidth = 16;


	final class MsgQueueEntry
	{
		String m_Msg;
		Image m_Icon;
		int m_Id;
		MsgQueueEntry m_Next;
		int m_DisplayCount = -1;
	}

	private MsgQueueEntry m_Msgs;
	private MsgQueueEntry m_lastMsg;
	private volatile boolean m_bRun;
	private volatile int m_aktY;
	private Thread m_Thread;
	private int m_idyFont;

	public StatusPanel()
	{
		m_Random = new Random();
		oMsgSync = new Object();
		Font font = new JLabel("Status").getFont();
		m_Height = (int) (font.getSize() * 0.9);
		m_Height = Math.min(m_Height, ms_IconHeight);
		font = new Font(font.getName(), Font.PLAIN, m_Height);
		m_dimensionPreferredSize = new Dimension(100, ms_IconHeight);
		m_idyFont = (ms_IconHeight - font.getSize()) / 2;
		setLayout(null);
		setFont(font);
		//setBackground(Color.red);
		setSize(m_dimensionPreferredSize);
		m_Msgs = null;
		m_lastMsg = null;
		m_Thread = new Thread(this,"StatusPanel");
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
		MsgQueueEntry entry = null;
		synchronized (oMsgSync)
		{
			entry = new MsgQueueEntry();
			entry.m_Msg = msg;
			entry.m_Id = m_Random.nextInt();
			if (bAutoRemove)
			{
				entry.m_DisplayCount = 1;
			}
			if (type == JOptionPane.WARNING_MESSAGE)
			{
				entry.m_Icon = GUIUtils.loadImageIcon(JAPConstants.IMAGE_WARNING, true).getImage();
			}
			else if (type == JOptionPane.INFORMATION_MESSAGE)
			{
				entry.m_Icon = GUIUtils.loadImageIcon(JAPConstants.IMAGE_INFORMATION, true).getImage();
			}
			else if (type == JOptionPane.ERROR_MESSAGE)
			{
				entry.m_Icon = GUIUtils.loadImageIcon(JAPConstants.IMAGE_ERROR, true).getImage();
			}
			if (m_lastMsg == null)
			{
				m_Msgs = entry;
				m_lastMsg = entry;
				entry.m_Next = entry;
			}
			else
			{
				entry.m_Next = m_lastMsg.m_Next;
				m_lastMsg.m_Next = entry;
				m_Msgs = m_lastMsg;
				m_lastMsg = entry;
			}
		}
		//displayLastMessage....
		m_Thread.interrupt();
		return entry.m_Id;

	}

	/** Removes a message from the ones which are displayed in the status panel
	 * @param id the message to be removed
	 */
	public void removeStatusMsg(int id)
	{
		synchronized (oMsgSync)
		{
			if (m_Msgs == null) //zero elements
			{
				return;
			}
			if (m_Msgs.m_Id == id && m_Msgs.m_Next == m_Msgs) //one element
			{
				m_Msgs = null;
				m_lastMsg = null;
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
				}
				prev.m_Next = entry.m_Next; //remove entry from list
				if (m_lastMsg == entry) //adjust last
				{
					m_lastMsg = prev;
				}
			}
		}
		m_Thread.interrupt(); //display changes
	}

	public void paint(Graphics g)
	{
		if (g == null)
		{
			return;
		}
		super.paint(g);
		synchronized (oMsgSync)
		{
			if (m_Msgs != null)
			{
				g.drawString(m_Msgs.m_Msg, ms_IconWidth + 2, m_aktY - m_idyFont);
				if (m_Msgs.m_Icon != null)
				{
					g.drawImage(m_Msgs.m_Icon, 0, m_aktY - ms_IconHeight, this);
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
			boolean bInterrupted = false;
			while (m_bRun)
			{
				try
				{
					if (!bInterrupted)
					{
						Thread.sleep(10000);
					}
				}
				catch (InterruptedException e)
				{
					if (!m_bRun)
					{
						return;
					}
				}
				bInterrupted = false;
				synchronized (oMsgSync)
				{
					if (m_Msgs != null && m_Msgs.m_DisplayCount == 0)
					{
						removeStatusMsg(m_Msgs.m_Id);
					}

					if (m_Msgs == null)
					{
						//paint(getGraphics());
						repaint();
						continue;
					}
					if (m_Msgs.m_DisplayCount > 0)
					{
						m_Msgs.m_DisplayCount--;
					}
					m_Msgs = m_Msgs.m_Next;
					m_aktY = 0;
				}
				for (int i = 0; i < ms_IconHeight + 1 && m_bRun; i++)
				{
					//paint(getGraphics());
					repaint();
					try
					{
						if (!bInterrupted)
						{
							Thread.sleep(100);
						}
					}
					catch (InterruptedException e)
					{
						bInterrupted = true;
					}
					m_aktY++;
				}

			}
		}
		catch (Exception e)
		{
		}
	}
}
