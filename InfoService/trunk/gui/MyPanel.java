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
import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JToggleButton;
import jap.JAPUtil;

public class MyPanel extends JPanel
{
	JPanel mainPanel;
	JPanel smallPanel;
	JPanel fullPanel;
	JToggleButton bttn;
	CardLayout l;
	public MyPanel()
	{
		GridBagLayout gbl = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		setLayout(gbl);
		//  	setBackground(Color.DARK_GRAY);
		bttn = new JToggleButton(JAPUtil.loadImageIcon("arrow.gif", true));
		bttn.setSelectedIcon(JAPUtil.loadImageIcon("arrow90.gif", true));
		bttn.setBorderPainted(false);
		bttn.setContentAreaFilled(false);
		bttn.setFocusPainted(false);
		c.insets = new Insets(0, 0, 0, 0);
		c.anchor = GridBagConstraints.NORTHWEST;
		bttn.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				if (bttn.isSelected())
				{
					l.last(mainPanel);
				}
				else
				{
					l.first(mainPanel);
				}
			}
		});
		add(bttn, c);
		c.gridx = 1;
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1;
		c.weighty = 1;
		c.insets = new Insets(0, 0, 0, 0);
		mainPanel = new JPanel();
		//addComponentListener(this);
		l = new CardLayout();
		mainPanel.setLayout(l);
		//mainPanel.setSize(200,200);
		//mainPanel.setLocation(20,5);
		mainPanel.setBackground(Color.blue);
		add(mainPanel, c);
		smallPanel = new JPanel();
		smallPanel.setBackground(Color.green);
		smallPanel.add(new JButton("Help"));
		mainPanel.add(smallPanel, "FIRST");
		fullPanel = new JPanel();
		fullPanel.add(new JTextArea(10, 10));
		fullPanel.setBackground(Color.red);
		mainPanel.add(fullPanel, "FULL");
	}

	public void setFullPanel(JPanel p)
	{
		mainPanel.remove(1);
		fullPanel = p;
		mainPanel.add(fullPanel, "FULL");
	}

	public Dimension getPreferredSize()
	{
		if (bttn.isSelected())
		{
			return fullPanel.getPreferredSize();
		}
		else
		{
			return smallPanel.getPreferredSize();
		}
	}

}
