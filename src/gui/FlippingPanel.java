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

/**
 * This class is used for the various panels on the Jap's
 * main view (JapNewView).
 *
 * It is a panel with two states (normal vs. flipped).
 * In normal state the contents of the full panel is shown,
 * in flipped state the contents of the small panel is shown,
 * which should be much smaller in height.
 *
 * The user can toggle the flipped state by clicking an arrow icon on the
 * left side.
 *
 * @author ??
 */
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
		c.fill = GridBagConstraints.NONE;
		c.weightx = 0;
		c.weighty = 0;
		c.insets = new Insets(0, 0, 0, 0);
		c.anchor = GridBagConstraints.NORTHWEST;
		m_labelBttn.addMouseListener(new MouseListener()
		{
			public void mouseClicked(MouseEvent e)
			{
				m_bIsFlipped = !m_bIsFlipped;
				m_Layout.next(m_panelContainer);
				if (m_bIsFlipped)
				{
					m_labelBttn.setIcon(ms_iconDown);
				}
				else
				{
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
		Dimension d1, d2;
		d1 = m_panelFull.getPreferredSize();
		d2 = m_panelSmall.getPreferredSize();
		d1.width = Math.max(d1.width, d2.width);
		d1.width += ms_iBttnWidth;
		if (!m_bIsFlipped)
		{
			d1.height = d2.height;
		}
		return d1;
	}

	public Dimension getMinimumSize()
	{
		Dimension d1, d2;
		d1 = m_panelFull.getMinimumSize();
		d2 = m_panelSmall.getMinimumSize();
		d1.width = Math.max(d1.width, d2.width);
		d1.width += ms_iBttnWidth;
		if (!m_bIsFlipped)
		{
			d1.height = d2.height;
		}
		d1.height = Math.max(d1.height, ms_iBttnHeight);
		return d1;
	}

	public Dimension getMaximumSize()
	{
		Dimension d1, d2;
		d1 = m_panelFull.getMaximumSize();
		d2 = m_panelSmall.getMaximumSize();
		d1.width = Math.max(d1.width, d2.width);
		d1.width += ms_iBttnWidth;
		if (!m_bIsFlipped)
		{
			d1.height = d2.height;
		}
		return d1;
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
				0, 0, 0, 0, 1, false));
		}
	}
}
