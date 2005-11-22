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

import jap.JAPUtil;

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.util.Enumeration;
import java.util.Vector;

import javax.swing.AbstractButton;
import javax.swing.ButtonGroup;
import javax.swing.ImageIcon;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
/**
 * Class for painting a mix cascade in the configuration dialog
 */
final public class ServerListPanel extends JPanel implements ActionListener
{
	private final static ImageIcon ms_iconServerBlau=JAPUtil.loadImageIcon(JAPConstants.IMAGE_SERVER_BLAU, true);
	private final static ImageIcon ms_iconServerRed=JAPUtil.loadImageIcon(JAPConstants.IMAGE_SERVER_ROT, true);
	private final static ImageIcon ms_iconServer=JAPUtil.loadImageIcon(JAPConstants.IMAGE_SERVER, true);
	private ButtonGroup m_bgMixe;
	private int m_selectedIndex;
	private Vector m_itemListeners;

	/**
	 * Creates a panel with numberOfMixes Mix-icons
	 * @param numberOfMixes int
	 */
	public ServerListPanel(int a_numberOfMixes, boolean a_enabled)
	{
		m_itemListeners = new Vector();
		GridBagLayout layout = new GridBagLayout();
		GridBagConstraints constraints = new GridBagConstraints();
		m_bgMixe = new ButtonGroup();
		m_selectedIndex = 0;

		this.setLayout(layout);
		constraints.anchor = GridBagConstraints.WEST;

		for (int i = 0; i < a_numberOfMixes; i++)
		{
			//Insert a line from the previous mix
			if (i != 0)
			{
				JSeparator line = new JSeparator();
				line.setPreferredSize(new Dimension(50, 3));
				line.setSize(50, 3);
				layout.setConstraints(line, constraints);
				this.add(line);
			}

			//Create the mix icon and place it in the panel
			AbstractButton mix = new JRadioButton(ms_iconServer);
			mix.setToolTipText(JAPMessages.getString("serverPanelAdditional"));
			mix.addActionListener(this);
			mix.setBorder(null);
			mix.setFocusPainted(false);
			mix.setRolloverEnabled(true);
			mix.setRolloverIcon(ms_iconServerBlau);
			mix.setSelectedIcon(ms_iconServerRed);
			if (i == 0)
			{
				mix.setSelected(true);
			}
			layout.setConstraints(mix, constraints);
			this.add(mix);
			m_bgMixe.add(mix);
			if (!a_enabled)
				mix.setEnabled(false);
		}
	}

	/**
	 * Determine which mix was clicked and set m_selectedMix accordingly
	 * @param e ActionEvent
	 */
	public void actionPerformed(ActionEvent e)
	{
		Object source = e.getSource();
		Enumeration mixes = m_bgMixe.getElements();
		int index=0;
		while (mixes.hasMoreElements())
		{
			if (source == mixes.nextElement())
			{
				m_selectedIndex=index;
				ItemEvent itemEvent = new ItemEvent( (AbstractButton) source, ItemEvent.ITEM_STATE_CHANGED,
					source, ItemEvent.SELECTED);
				Enumeration enumer = m_itemListeners.elements();
				while (enumer.hasMoreElements())
				{
					( (ItemListener) enumer.nextElement()).itemStateChanged(itemEvent);
				}
				return;
			}
			index++;
		}
	}

	public void addItemListener(ItemListener l)
	{
		m_itemListeners.addElement(l);
	}

	/**
	 * Getter method for m_selectedMix
	 */
	public int getSelectedIndex()
	{
		return m_selectedIndex;
	}

}
