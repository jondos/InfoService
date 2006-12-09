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

import java.awt.Color;
import java.awt.Component;
import javax.swing.DefaultComboBoxModel;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.ListCellRenderer;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;

import anon.infoservice.MixCascade;
import jap.JAPConstants;
import jap.JAPMessages;
import jap.JAPUtil;
import java.awt.Dimension;

public class JAPMixCascadeComboBox extends JComboBox
{
	final static String ITEM_AVAILABLE_CASCADES = "ITEM_AVAILABLE_CASCADES";
	final static String ITEM_USER_CASCADES = "ITEM_USER_CASCADES";
	final static String ITEM_NO_SERVERS_AVAILABLE = "ITEM_NO_SERVERS_AVAILABLE";

	public JAPMixCascadeComboBox()
	{
		super();
		setModel(new JAPMixCascadeComboBoxModel());
		setRenderer(new JAPMixCascadeComboBoxListCellRender());
		setEditable(false);
		super.addItem(ITEM_AVAILABLE_CASCADES);
	}

	public void addItem(Object o)
	{
	}

	public void addMixCascade(MixCascade cascade)
	{
		DefaultComboBoxModel model = (DefaultComboBoxModel) getModel();
		if (cascade.isUserDefined())
		{
			if (model.getIndexOf(ITEM_USER_CASCADES) < 0)
			{
				super.addItem(ITEM_USER_CASCADES);
			}
			super.addItem(cascade);
		}
		else
		{
			int index = model.getIndexOf(ITEM_USER_CASCADES);
			if (index < 0)
			{
				super.addItem(cascade);
			}
			else
			{
				super.insertItemAt(cascade, index);
			}
		}
	}

	public void removeAllItems()
	{
		//Note: We do not use super.removeAllItems() here because it does no correctly
		//resets the selected item at least on SUN JDK 1.4.1 !
		setModel(new JAPMixCascadeComboBoxModel());
		super.addItem(ITEM_AVAILABLE_CASCADES);
	}

	public void setNoDataAvailable()
	{
		super.insertItemAt(ITEM_NO_SERVERS_AVAILABLE, 1);
	}

	public Dimension getPreferredSize()
	{
		Dimension d = super.getPreferredSize();
		if (d.width > 50)
		{
			d.width = 50;
		}
		return d;
	}
}

final class JAPMixCascadeComboBoxModel extends DefaultComboBoxModel
{
	public void setSelectedItem(Object anObject)
	{
		if (anObject.equals(JAPMixCascadeComboBox.ITEM_AVAILABLE_CASCADES) ||
			anObject.equals(JAPMixCascadeComboBox.ITEM_USER_CASCADES) ||
			anObject.equals(JAPMixCascadeComboBox.ITEM_NO_SERVERS_AVAILABLE))
		{
			return;
		}
		super.setSelectedItem(anObject);
	}
}

final class JAPMixCascadeComboBoxListCellRender implements ListCellRenderer
{
	private JLabel m_componentNoServer;
	private JLabel m_componentAvailableServer;
	private JLabel m_componentUserServer;
	private JLabel m_componentUserDefinedCascade;
	private JLabel m_componentAvailableCascade;
	public JAPMixCascadeComboBoxListCellRender()
	{
		m_componentNoServer = new JLabel(JAPMessages.getString("ngMixComboNoServers"));
		m_componentNoServer.setIcon(JAPUtil.loadImageIcon(JAPConstants.IMAGE_ERROR, true));
		m_componentNoServer.setBorder(new EmptyBorder(0, 3, 0, 3));
		m_componentNoServer.setForeground(Color.red);

		m_componentAvailableServer = new JLabel(JAPMessages.getString("ngMixComboAvailableServers"));
		m_componentAvailableServer.setOpaque(true);
		m_componentAvailableServer.setHorizontalAlignment(SwingConstants.LEFT);
		m_componentAvailableServer.setBorder(new EmptyBorder(1, 3, 1, 3));

		m_componentUserServer = new JLabel(JAPMessages.getString("ngMixComboUserServers"));
		m_componentUserServer.setBorder(new EmptyBorder(1, 3, 1, 3));
		m_componentUserServer.setHorizontalAlignment(SwingConstants.LEFT);
		m_componentUserServer.setOpaque(true);
		m_componentUserDefinedCascade = new JLabel(JAPUtil.loadImageIcon(JAPConstants.
			IMAGE_CASCADE_MANUELL, true));
		m_componentUserDefinedCascade.setOpaque(true);
		m_componentUserDefinedCascade.setHorizontalAlignment(SwingConstants.LEFT);
		m_componentUserDefinedCascade.setBorder(new EmptyBorder(1, 3, 1, 3));
		m_componentAvailableCascade = new JLabel(JAPUtil.loadImageIcon(JAPConstants.
			IMAGE_CASCADE_INTERNET, true));
		m_componentAvailableCascade.setHorizontalAlignment(SwingConstants.LEFT);
		m_componentAvailableCascade.setOpaque(true);
		m_componentAvailableCascade.setBorder(new EmptyBorder(1, 3, 1, 3));
	}

	public Component getListCellRendererComponent(JList list, Object value, int index,
												  boolean isSelected, boolean cellHasFocus)
	{
		if (value == null)
		{
			return new JLabel();
		}
		if (value.equals(JAPMixCascadeComboBox.ITEM_AVAILABLE_CASCADES))
		{
			return m_componentAvailableServer;
		}
		else if (value.equals(JAPMixCascadeComboBox.ITEM_USER_CASCADES))
		{
			return m_componentUserServer;
		}
		else if (value.equals(JAPMixCascadeComboBox.ITEM_NO_SERVERS_AVAILABLE))
		{
			return m_componentNoServer;
		}
		MixCascade cascade = (MixCascade) value;
		JLabel l;
		if (cascade.isUserDefined())
		{
			l = m_componentUserDefinedCascade;
		}
		else
		{
			l = m_componentAvailableCascade;
		}
		l.setText(cascade.getName());
		if (isSelected)
		{
			l.setBackground(list.getSelectionBackground());
			l.setForeground(list.getSelectionForeground());
		}
		else
		{
			l.setBackground(list.getBackground());
			l.setForeground(list.getForeground());
		}
		return l;
	}
}