/*
 Copyright (c) 2000 - 2006, The JAP-Team
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
package jap;

import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Vector;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Point;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.SwingUtilities;

import anon.infoservice.Database;
import anon.infoservice.MixCascade;
import anon.infoservice.NewCascadeIDEntry;
import gui.GUIUtils;
import gui.PopupMenu;

/**
 *
 * @author Rolf Wendolsky
 */
public class CascadePopupMenu extends PopupMenu
{
	private static final int MAX_CASCADE_NAME_LENGTH = 30;
	private final Color m_newCascadeColor = new Color(255, 255, 170);

	private Hashtable m_menuItems;
	private ActionListener m_cascadeItemListener;
	private TrustModel m_trustModel;
	private int m_headerHeight = 0;

	public CascadePopupMenu()
	{
		this (new JPopupMenu());
	}

	public CascadePopupMenu(JPopupMenu a_popup)
	{
		super(a_popup);

		m_menuItems = new Hashtable();
		m_cascadeItemListener = new CascadeItemListener();
	}


	public TrustModel getTrustModel()
	{
		return m_trustModel;
	}

	public int getHeaderHeight()
	{
		return m_headerHeight;
	}

	public boolean update(TrustModel a_trustModel)
	{
		boolean updated = false;

		if (a_trustModel == null)
		{
			throw new IllegalArgumentException("Given argument is null!");
		}
		m_trustModel = a_trustModel;

		Hashtable hashCascades = Database.getInstance(MixCascade.class).getEntryHash();
		MixCascade currentCascade = JAPController.getInstance().getCurrentMixCascade();
		if (currentCascade != null)
		{
			hashCascades.put(currentCascade.getId(), currentCascade);
		}
		Enumeration cascades = hashCascades.elements();

		if (cascades.hasMoreElements())
		{
			MixCascade cascade;
			JMenuItem menuItem;

			ImageIcon icon;
			Vector userDefined = new Vector();

			getPopupMenu().removeAll();
			m_menuItems.clear();


			JPanel panel = new JPanel(new GridBagLayout());
			GridBagConstraints constraints = new GridBagConstraints();
			constraints.gridx = 0;
			constraints.gridy = 0;
			constraints.anchor = GridBagConstraints.CENTER;
			panel.add(new JLabel(m_trustModel.getName()), constraints);
			getPopupMenu().add(panel);
			JPopupMenu.Separator separator = new JPopupMenu.Separator();
			getPopupMenu().add(separator);
			m_headerHeight = panel.getPreferredSize().height + separator.getPreferredSize().height;
			//m_headerHeight = m_popup.getPreferredSize().height;

			while (cascades.hasMoreElements())
			{
				cascade = (MixCascade) cascades.nextElement();
				if (!m_trustModel.isTrusted(cascade))
				{
					continue;
				}

				if (cascade.isPayment())
				{
					icon = GUIUtils.loadImageIcon(JAPConstants.IMAGE_CASCADE_PAYMENT);
				}
				else if (cascade.isUserDefined())
				{
					icon = GUIUtils.loadImageIcon(JAPConstants.IMAGE_CASCADE_MANUELL);
				}
				else
				{
					icon = GUIUtils.loadImageIcon(JAPConstants.IMAGE_CASCADE_INTERNET);
				}
				menuItem = new JMenuItem(
								GUIUtils.trim(cascade.toString(), MAX_CASCADE_NAME_LENGTH), icon);
				if (isNewCascade(cascade))
				{
					menuItem.setBackground(m_newCascadeColor);
				}
				if (currentCascade != null && currentCascade.equals(cascade))
				{
					menuItem.setFont(new Font(menuItem.getFont().getName(), Font.BOLD,
											  menuItem.getFont().getSize()));
					getPopupMenu().insert(menuItem, 2);
				}
				else
				{
					menuItem.setFont(new Font(menuItem.getFont().getName(), Font.PLAIN,
											  menuItem.getFont().getSize()));
					if (cascade.isUserDefined())
					{
						userDefined.addElement(menuItem);
					}
					else
					{
						getPopupMenu().add(menuItem);
					}
				}
				menuItem.addActionListener(m_cascadeItemListener);
				m_menuItems.put(menuItem, cascade);
				updated = true;
			}
			for (int i = 0; i < userDefined.size(); i++)
			{
				getPopupMenu().add( (JMenuItem) userDefined.elementAt(i));
			}
		}
		return updated;
	}

	private boolean isNewCascade(MixCascade a_cascade)
	{
		if ( (Database.getInstance(NewCascadeIDEntry.class).getNumberOfEntries() * 2 <
			  Database.getInstance(MixCascade.class).getNumberOfEntries()) &&
			 Database.getInstance(NewCascadeIDEntry.class).getEntryById(
					  a_cascade.getMixIDsAsString()) != null)
		{
			   return true;
		}
		return false;
	}

	private class CascadeItemListener implements ActionListener
	{
		public void actionPerformed(ActionEvent a_event)
		{
			MixCascade cascade = (MixCascade)m_menuItems.get(a_event.getSource());
			if (cascade != null)
			{
				TrustModel.setCurrentTrustModel(m_trustModel);
				JAPController.getInstance().setCurrentMixCascade(cascade);
				setVisible(false);
			}
		}
	}
}
