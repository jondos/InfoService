package gui;

import javax.swing.*;
import java.awt.*;

public class StatusPanel extends JPanel implements Runnable
{
	Dimension m_dimensionPreferredSize;
	int m_Height;
	Object oMsgSync;
	final class MsgQueueEntry
	{
		String m_Msg;
		//Image m_Icon;
		Icon m_Icon;
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
		Font font=new JLabel("Status").getFont();
		m_Height=(int)(font.getSize()*0.8);
		m_Height=Math.min(m_Height,16);
		font=new Font(font.getName(),font.getStyle(),m_Height);
		m_dimensionPreferredSize = new Dimension(100, 16);
		m_idyFont=(16-font.getSize())/2;
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

	public void addMsg(String msg,int type)
	{
		synchronized (oMsgSync)
		{
			MsgQueueEntry entry = new MsgQueueEntry();
			entry.m_Msg = msg;
			String s="OptionPane.informationIcon";
			if(type==JOptionPane.WARNING_MESSAGE)
				s="OptionPane.warningIcon";
			else if(type==JOptionPane.ERROR_MESSAGE)
				s="OptionPane.errorIcon";
			else if(type==JOptionPane.QUESTION_MESSAGE)
				s="OptionPane.questionIcon";
			Icon icon=	UIManager.getDefaults().getIcon(s);
			entry.m_Icon=icon;
			//entry.m_Icon=((ImageIcon)icon).getImage();
			//entry.m_Icon=entry.m_Icon.getScaledInstance(16,16,Image.SCALE_SMOOTH);
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
				g.drawString(m_Msgs.m_Msg, 18, m_aktY-m_idyFont);
	//if(m_Msgs.m_Icon!=null)
				//	b=g.drawImage(m_Msgs.m_Icon,0,m_aktY,this);
		//b=g.drawImage(m_Msgs.m_Icon,0,m_aktY-16,16,m_aktY,0,0,m_Msgs.m_Icon.getWidth(this),
		//			 m_Msgs.m_Icon.getHeight(this) ,this);
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
				for (int i = 0; i < 16 && m_bRun; i++)
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
