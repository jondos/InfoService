package gui;

import javax.swing.*;
import javax.swing.plaf.*;
import javax.swing.plaf.basic.*;
import java.awt.*;

final public class MyProgressBarUI extends BasicProgressBarUI
{
	final static int ms_dx=13;
	final static int ms_width = 9;
	private boolean m_bOneBarPerValue=false;

	public MyProgressBarUI(boolean bOneBarPerValue)
	{
		super();
		m_bOneBarPerValue=bOneBarPerValue;
	}

	public void paint(Graphics g, JComponent c)
	{
		JProgressBar pb = (JProgressBar) c;
		int max = pb.getMaximum();
		int anz = pb.getWidth() / ms_dx;
		int value = pb.getValue() * anz / max;
		int x = 0;
		int y = 0;
		int height = c.getHeight();
		for (int i = 0; i < value; i++)
		{
			g.fill3DRect(x, y, ms_width, height, false);
			x += ms_dx;
		}
		for (int i = value; i < anz; i++)
		{
			g.draw3DRect(x, y, ms_width, height, false);
			x += ms_dx;
		}
	}

	public Dimension getPreferredSize(JComponent c)
	{
		if(!m_bOneBarPerValue)
			return super.getPreferredSize(c);
		JProgressBar pb = (JProgressBar) c;
		return new Dimension(ms_dx*pb.getMaximum(),12);
	}

	public Dimension getMinimumSize(JComponent c)
	{
		if(!m_bOneBarPerValue)
			return super.getMinimumSize(c);
		return getPreferredSize(c);
	}

	public Dimension getMaximumSize(JComponent c)
	{
		if(!m_bOneBarPerValue)
			return super.getMaximumSize(c);
		return getPreferredSize(c);
	}

}
