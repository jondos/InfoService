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

import java.util.Vector;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.Font;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.ImageIcon;
import javax.swing.JMenuItem;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.MenuEvent;
import javax.swing.event.MenuListener;

import anon.infoservice.Database;
import anon.infoservice.StatusInfo;
import anon.infoservice.MixCascade;
import gui.JAPDll;
import gui.JAPHelp;
import gui.JAPMessages;
import gui.PopupMenu;
import gui.GUIUtils;
import java.awt.GridBagConstraints;
import javax.swing.JPanel;
import java.awt.GridBagLayout;

/**
 *
 *
 * @author Rolf Wendolsky
 */
public class SystrayPopupMenu extends PopupMenu
{
	private static final String MSG_EXIT = SystrayPopupMenu.class.getName() + "_exit";
	private static final String MSG_SHOW_MAIN_WINDOW = SystrayPopupMenu.class.getName() + "_showMainWindow";
	private static final String MSG_ANONYMITY_MODE = SystrayPopupMenu.class.getName() + "_anonymityMode";
	private static final String MSG_CURRENT_SERVICE = SystrayPopupMenu.class.getName() + "_currentService";
	private static final String MSG_CONNECTED = SystrayPopupMenu.class.getName() + "_connected";
	private static final String MSG_NOT_CONNECTED = SystrayPopupMenu.class.getName() + "_notConnected";
	private static final String MSG_USER_NUMBER = SystrayPopupMenu.class.getName() + "_userNumber";

	private MainWindowListener m_mainWindowListener;

	public SystrayPopupMenu(MainWindowListener a_mainWindowListener)
	{
		if (a_mainWindowListener == null)
		{
			throw new IllegalArgumentException(MainWindowListener.class.getName() + " is null!");
		}
		m_mainWindowListener = a_mainWindowListener;

		JMenuItem menuItem;
		JLabel label;
		ImageIcon icon;
		MixCascade cascade = JAPController.getInstance().getCurrentMixCascade();
		TrustModel currentModel = TrustModel.getCurrentTrustModel();
		String connected;
		String users = "";
		if (JAPController.getInstance().isAnonConnected())
		{
			StatusInfo info =
				(StatusInfo)Database.getInstance(StatusInfo.class).getEntryById(cascade.getId());
			if (info != null)
			{
				users = ", " + JAPMessages.getString(MSG_USER_NUMBER, new Integer(info.getNrOfActiveUsers()));
			}

			connected = JAPMessages.getString(MSG_CONNECTED);
		}
		else
		{
			connected = JAPMessages.getString(MSG_NOT_CONNECTED);
		}

		label = new JLabel(JAPMessages.getString(MSG_CURRENT_SERVICE, new Object[]{connected, users}));
		GUIUtils.setFontStyle(label, Font.PLAIN);
		JPanel panel = new JPanel(new GridBagLayout());
		GridBagConstraints constraints = new GridBagConstraints();
		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.anchor = GridBagConstraints.CENTER;
		panel.add(label, constraints);
		getPopupMenu().add(panel);


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
		label = new JLabel(GUIUtils.trim(cascade.getName()));
		GUIUtils.setFontStyle(label, Font.BOLD);
		label.setIcon(icon);
		//label.setOpaque(true);
		//label.setBackground(getPopupMenu().getBackground());
		panel = new JPanel(new GridBagLayout());
		//panel.setOpaque(true);
		//panel.setBackground(getPopupMenu().getBackground());
		constraints = new GridBagConstraints();
		constraints.gridx = 0;
		constraints.gridy = 0;
		constraints.weightx = 1.0;
		constraints.fill = GridBagConstraints.HORIZONTAL;
		constraints.anchor = GridBagConstraints.WEST;
		panel.add(label, constraints);
		getPopupMenu().add(panel);


		final JCheckBoxMenuItem cbxMenuItem = new JCheckBoxMenuItem(JAPMessages.getString(MSG_ANONYMITY_MODE));
		GUIUtils.setFontStyle(cbxMenuItem, Font.PLAIN);
		cbxMenuItem.setSelected(JAPController.getInstance().getAnonMode());
		cbxMenuItem.addChangeListener(new ChangeListener()
		{
			public void stateChanged(ChangeEvent a_event)
			{
				JAPController.getInstance().setAnonMode(cbxMenuItem.isSelected());
			}
		});
		getPopupMenu().add(cbxMenuItem);

		getPopupMenu().addSeparator();
		menuItem = new JMenuItem(JAPMessages.getString(MSG_SHOW_MAIN_WINDOW));
		GUIUtils.setFontStyle(menuItem, Font.PLAIN);
		menuItem.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent a_event)
			{
				m_mainWindowListener.onShowMainWindow();
			}
		});
		getPopupMenu().add(menuItem);

		menuItem = new JMenuItem(JAPMessages.getString(JAPHelp.MSG_HELP_MENU_ITEM));
		GUIUtils.setFontStyle(menuItem, Font.PLAIN);
		getPopupMenu().add(menuItem);
		menuItem.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent a_event)
			{
				JAPHelp.getInstance().getContextObj().setContext("index");
				JAPHelp.getInstance().setAlwaysOnTop(true);
				JAPHelp.getInstance().setVisible(true);
				JAPHelp.getInstance().setAlwaysOnTop(false);
			}
		});

		getPopupMenu().addSeparator();
		JMenu filterMenu;
		Vector models = TrustModel.getTrustModels();

		for (int i = 0; i < models.size(); i++)
		{
			final TrustModel tmpModel = (TrustModel) models.elementAt(i);
			if (tmpModel.isAdded())
			{
				filterMenu = new JMenu(tmpModel.getName());
				if (currentModel.equals(tmpModel))
				{
					GUIUtils.setFontStyle(filterMenu, Font.BOLD);
				}
				else
				{
					GUIUtils.setFontStyle(filterMenu, Font.PLAIN);
				}

				final CascadePopupMenu tmpPopupCascade = new CascadePopupMenu(filterMenu.getPopupMenu());
				getPopupMenu().add(filterMenu);

				filterMenu.addMenuListener(new MenuListener()
				{
					public void menuSelected(MenuEvent e)
					{
						tmpPopupCascade.update(tmpModel);
					}

					public void menuDeselected(MenuEvent e)
					{
					}

					public void menuCanceled(MenuEvent e)
					{
					}
				});
			}
		}

		getPopupMenu().addSeparator();
		menuItem = new JMenuItem(JAPMessages.getString(MSG_EXIT));
		GUIUtils.setFontStyle(menuItem, Font.PLAIN);
		menuItem.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent a_event)
			{
				JAPController.getInstance().goodBye(true);
			}
		});
		getPopupMenu().addSeparator();
		getPopupMenu().add(menuItem);
		getPopupMenu().pack();
	}

	public static interface MainWindowListener
	{
		public void onShowMainWindow();
	}
}
