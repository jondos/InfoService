package gui;

import javax.swing.*;
import java.awt.*;

public class StatusPanel extends JPanel implements Runnable
{
	Dimension m_dimensionPreferredSize;
	int m_Height;
	final class MsgQueueEntry
	{
		String m_Msg;
		MsgQueueEntry m_Next;
		int m_DisplayCount = 10;
	}

	private MsgQueueEntry m_Msgs;
	private MsgQueueEntry m_lastMsg;
	private volatile boolean m_bRun;
	private volatile int m_aktY;
	private Thread m_Thread;

	public StatusPanel()
	{
		m_Height = new JLabel("Status").getFont().getSize();
		m_dimensionPreferredSize = new Dimension(100, m_Height);
		setLayout(null);
		//setBackground(Color.red);
		setSize(m_dimensionPreferredSize);
		m_Msgs = null;
		m_lastMsg = null;
		m_Thread=new Thread(this);
		m_bRun=true;
		m_Thread.start();
	}

	public void finalize()
	{
		m_bRun=false;
		try{
			m_Thread.interrupt();
		m_Thread.join();
		}
		catch(Exception e)
		{
		}
	}

	public synchronized void addMsg(String msg)
	{
		MsgQueueEntry entry = new MsgQueueEntry();
		entry.m_Msg = msg;
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
		}
		//m_Msg=
		//displayLastMessage....
	}

	public synchronized void paint(Graphics g)
	{
		super.paint(g);
		if (m_Msgs != null)
		{
			g.drawString(m_Msgs.m_Msg, 0, m_aktY);
		}
	}

	public Dimension getPreferredSize()
	{
		return m_dimensionPreferredSize;
	}

	public void run()
	{
		try
		{
			while (m_bRun)
			{
				Thread.sleep(10000);
				if (m_Msgs != null)
				{
					m_Msgs = m_Msgs.m_Next;
					m_aktY = 0;
				}
				for (int i = 0; i < m_Height&&m_bRun; i++)
				{
					paint(getGraphics());
					Thread.sleep(100);
					m_aktY++;
				}
			}
		}
		catch (Exception e)
		{
		}
	}
}
