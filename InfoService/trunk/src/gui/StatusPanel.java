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

import javax.swing.*;
import java.awt.*;
import jap.*;

public class StatusPanel extends JPanel implements Runnable
{
	Dimension m_dimensionPreferredSize;
	int m_Height;
	Object oMsgSync;
	private final static int ms_IconHeight = 16;
	private final static int ms_IconWidth = 17;
	private final static Image ms_imageWarning=JAPUtil.loadImageIcon(JAPConstants.IMAGE_WARNING, true).getImage();
	private final static Image ms_imageInformation=JAPUtil.loadImageIcon(JAPConstants.IMAGE_INFORMATION, true).getImage();
	private final static Image ms_imageError=JAPUtil.loadImageIcon(JAPConstants.IMAGE_ERROR, true).getImage();

	final class MsgQueueEntry
	{
		String m_Msg;
		Image m_Icon;
		MsgQueueEntry m_Next;
		int m_DisplayCount = 10;
	}

	private MsgQueueEntry m_Msgs;
	private MsgQueueEntry m_lastMsg;
	private volatile boolean m_bRun;
	private volatile int m_aktY;
	private Thread m_Thread;
	private int m_idyFont;
	public StatusPanel()
	{
		oMsgSync = new Object();
		Font font = new JLabel("Status").getFont();
		m_Height = (int) (font.getSize() * 0.8);
		m_Height = Math.min(m_Height, ms_IconHeight);
		font = new Font(font.getName(), font.getStyle(), m_Height);
		m_dimensionPreferredSize = new Dimension(100,ms_IconHeight);
		m_idyFont = (ms_IconHeight - font.getSize()) / 2;
		setLayout(null);
		setFont(font);
		//setBackground(Color.red);
		setSize(m_dimensionPreferredSize);
		m_Msgs = null;
		m_lastMsg = null;
		m_Thread = new Thread(this);
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

	public void addMsg(String msg, int type)
	{
		synchronized (oMsgSync)
		{
			MsgQueueEntry entry = new MsgQueueEntry();
			entry.m_Msg = msg;
			if (type == JOptionPane.WARNING_MESSAGE)
			{
				entry.m_Icon = ms_imageWarning;
			}
			else if (type == JOptionPane.INFORMATION_MESSAGE)
			{
				entry.m_Icon = ms_imageInformation;
			}
			else if (type == JOptionPane.ERROR_MESSAGE)
			{
				entry.m_Icon = ms_imageError;
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
			//displayLastMessage....
			m_Thread.interrupt();
		}
	}

	public void paint(Graphics g)
	{
		if (g == null)
		{
			return;
		}
		super.paint(g);
		boolean b;
		synchronized (oMsgSync)
		{
			if (m_Msgs != null)
			{
				g.drawString(m_Msgs.m_Msg, ms_IconWidth + 2, m_aktY - m_idyFont);
				if (m_Msgs.m_Icon != null)
				{
					b = g.drawImage(m_Msgs.m_Icon, 0, m_aktY - ms_IconHeight, this);
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
				synchronized (oMsgSync)
				{
					if (m_Msgs == null)
					{
						continue;
					}
					m_Msgs = m_Msgs.m_Next;
					m_aktY = 0;
				}
				for (int i = 0; i < ms_IconHeight && m_bRun; i++)
				{
					paint(getGraphics());
					try
					{
						Thread.sleep(100);
					}
					catch (InterruptedException e)
					{
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
