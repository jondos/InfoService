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
package gui;

import java.awt.CardLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.JPanel;
import jap.JAPUtil;

final public class FlippingPanel extends JPanel
{
	private JPanel m_panelContainer;
	private JPanel m_panelSmall;
	private JPanel m_panelFull;
	private JLabel m_labelBttn;
	private CardLayout m_Layout;
	private Window m_Parent;
	private boolean m_bIsFlipped;
	private final static Icon ms_iconUp = JAPUtil.loadImageIcon("arrow.gif", true);
	private final static Icon ms_iconDown = JAPUtil.loadImageIcon("arrow90.gif", true);
	private final static int ms_iBttnWidth = ms_iconUp.getIconWidth();
	private final static int ms_iBttnHeight = ms_iconDown.getIconHeight();

	public FlippingPanel(Window parent)
	{
		m_bIsFlipped = false;
		m_Parent = parent;
		GridBagLayout gbl = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		setLayout(gbl);
		m_labelBttn = new JLabel(ms_iconUp);
		c.insets = new Insets(0, 0, 0, 0);
		c.anchor = GridBagConstraints.NORTHWEST;
		m_labelBttn.addMouseListener(new MouseListener()
		{
			public void mouseClicked(MouseEvent e)
			{
				m_bIsFlipped = !m_bIsFlipped;
				if (m_bIsFlipped)
				{
					m_Layout.last(m_panelContainer);
					m_labelBttn.setIcon(ms_iconDown);
				}
				else
				{
					m_Layout.first(m_panelContainer);
					m_labelBttn.setIcon(ms_iconUp);
				}
				m_Parent.pack();
			}

			public void mouseEntered(MouseEvent e)
			{
			}

			public void mouseExited(MouseEvent e)
			{
			}

			public void mousePressed(MouseEvent e)
			{
			}

			public void mouseReleased(MouseEvent e)
			{
			}
		}
		);
		add(m_labelBttn, c);
		c.gridx = 1;
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1;
		c.weighty = 1;
		c.insets = new Insets(0, 0, 0, 0);
		m_panelContainer = new JPanel();
		m_Layout = new CardLayout();
		m_panelContainer.setLayout(m_Layout);
		add(m_panelContainer, c);

		m_panelSmall = new JPanel(new GridLayout(1, 1));
		m_panelContainer.add(m_panelSmall, "SMALL", 0);
		m_panelFull = new JPanel(new GridLayout(1, 1));
		m_Layout.addLayoutComponent(m_panelFull, "FULL");
		m_panelContainer.add(m_panelFull, "FULL", 1);
	}

	public void setFullPanel(JPanel p)
	{
		m_panelFull.removeAll();
		m_panelFull.add(p);
	}

	public void setSmallPanel(JPanel p)
	{
		m_panelSmall.removeAll();
		m_panelSmall.add(p);
	}

	public Dimension getPreferredSize()
	{
		Dimension d;
		if (m_bIsFlipped)
		{
			d = m_panelFull.getPreferredSize();
		}
		else
		{
			d = m_panelSmall.getPreferredSize();
		}
		d.width += ms_iBttnWidth;
		return d;
	}

	public Dimension getMinimumSize()
	{
		Dimension d;
		if (m_bIsFlipped)
		{
			d = m_panelFull.getMinimumSize();
		}
		else
		{
			d = m_panelSmall.getMinimumSize();
		}
		d.width += ms_iBttnWidth;
		d.height = Math.max(d.height, ms_iBttnHeight);
		return d;
	}

	public Dimension getMaximumSize()
	{
		Dimension d;
		if (m_bIsFlipped)
		{
			d = m_panelFull.getMaximumSize();
		}
		else
		{
			d = m_panelSmall.getMaximumSize();
		}
		d.width += ms_iBttnWidth;
		return d;
	}

	public void setFlipped(boolean bFlipped)
	{
		if (bFlipped == m_bIsFlipped)
		{
			return;
		}
		else
		{
			m_labelBttn.dispatchEvent(
				new MouseEvent(
				m_labelBttn, MouseEvent.MOUSE_CLICKED,
				0, 0, 0, 0, 1, false, MouseEvent.BUTTON1));
		}
	}
}
