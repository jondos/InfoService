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

package jap;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.border.TitledBorder;

import anon.infoservice.InfoServiceHolder;
import anon.infoservice.JAPVersionInfo;
import gui.GUIUtils;
import gui.JAPHelp;
import gui.JAPMessages;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import update.JAPUpdateWizard;

final class JAPConfUpdate extends AbstractJAPConfModule implements ActionListener, ItemListener, Runnable
{
	private static final String COMMAND_UPGRADE = "UPGRADE";
	private static final String COMMAND_CHECKFORUPGRADE = "CHECKFORUPGRADE";

	private static final String MSG_ALLOW_DIRECT_CONN = JAPConfUpdate.class.getName() +
		"_allowDirectConnection";
	private static final String MSG_REMIND_OPTIONAL_UPDATE = JAPConfUpdate.class.getName() +
		"_remindOptionalUpdate";

	//private JDialog m_Dialog;
	private JTextArea m_taInfo;
	private JLabel m_labelVersion, m_labelDate;

	// private JAPController japController;
	private JComboBox m_comboType;
	private JButton m_bttnUpgrade, m_bttnCheckForUpgrade;
	private JCheckBox m_cbxAllowDirectUpdate;
	private JCheckBox m_cbxRemindOptionalUpdate;

	private Thread m_threadGetVersionInfo;
	private JAPVersionInfo m_devVersion;
	private JAPVersionInfo m_releaseVersion;
	private DateFormat m_DateFormat;

	public JAPConfUpdate()
	{
		super(null);
	}

	public void recreateRootPanel()
	{
		JPanel panelRoot = getRootPanel();
		panelRoot.removeAll();
		GridBagLayout gridBagFrame = new GridBagLayout();
		panelRoot.setLayout(gridBagFrame);

		//The Buttons
		JPanel buttonPanel = new JPanel();
		GridBagLayout gridBagPanel = new GridBagLayout();
		buttonPanel.setLayout(gridBagPanel);
		GridBagConstraints cButtons = new GridBagConstraints();
		cButtons.gridx = GridBagConstraints.RELATIVE;
		cButtons.weightx = 1.0;
		cButtons.weighty = 1.0;
		cButtons.fill = GridBagConstraints.VERTICAL;
		cButtons.anchor = GridBagConstraints.WEST;

		m_bttnUpgrade = new JButton(JAPMessages.getString("confUpgrade"));
		m_bttnUpgrade.addActionListener(this);
		m_bttnUpgrade.setActionCommand(COMMAND_UPGRADE);
		cButtons.anchor = GridBagConstraints.CENTER;
		cButtons.gridx = 1;
		gridBagPanel.setConstraints(m_bttnUpgrade, cButtons);
		m_bttnUpgrade.setEnabled(false);
		buttonPanel.add(m_bttnUpgrade);

		m_bttnCheckForUpgrade = new JButton(JAPMessages.getString("confCheckForUpgrade"));
		m_bttnCheckForUpgrade.setIcon(GUIUtils.loadImageIcon(JAPConstants.IMAGE_RELOAD, true));
		m_bttnCheckForUpgrade.setDisabledIcon(GUIUtils.loadImageIcon(JAPConstants.IMAGE_RELOAD_DISABLED, true));
		m_bttnCheckForUpgrade.setPressedIcon(GUIUtils.loadImageIcon(JAPConstants.IMAGE_RELOAD_ROLLOVER, true));
		m_bttnCheckForUpgrade.addActionListener(this);
		m_bttnCheckForUpgrade.setActionCommand(COMMAND_CHECKFORUPGRADE);
		cButtons.anchor = GridBagConstraints.CENTER;
		cButtons.gridx = 0;
		gridBagPanel.setConstraints(m_bttnCheckForUpgrade, cButtons);
		m_bttnCheckForUpgrade.setEnabled(true);
		buttonPanel.add(m_bttnCheckForUpgrade);

		//The Installed-Panel
		gridBagPanel = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		TitledBorder titledBorder = new TitledBorder(" " + JAPMessages.getString("updateTitleBorderInstalled") +
			" ");
		JPanel installedPanel = new JPanel(gridBagPanel);
		installedPanel.setBorder(titledBorder);
		JLabel l = new JLabel("Version: ");
		c.gridx = 0;
		c.gridy = 0;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.weighty = 0.33;
		c.weightx = 0;
		c.fill = GridBagConstraints.NONE;
		c.insets = new Insets(5, 5, 5, 5);
		gridBagPanel.setConstraints(l, c);
		installedPanel.add(l);
		l = new JLabel(JAPConstants.aktVersion);
		c.gridx = 1;
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1;
		gridBagPanel.setConstraints(l, c);
		installedPanel.add(l);
		l = new JLabel(JAPMessages.getString("updateLabelDate") + " ");
		c.gridx = 0;
		c.gridy = 1;
		c.weightx = 0;
		c.fill = GridBagConstraints.NONE;
		gridBagPanel.setConstraints(l, c);
		installedPanel.add(l);
		String strDate = JAPConstants.strReleaseDate;
		try
		{
			DateFormat sdf = new SimpleDateFormat("yyyy/MM/dd HH:mm:ss z");
			Date d = sdf.parse(strDate + " GMT");
			m_DateFormat = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.MEDIUM);
			strDate = m_DateFormat.format(d);
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.EXCEPTION, LogType.MISC, e);
		}
		l = new JLabel(strDate);
		c.gridx = 1;
		c.weightx = 1;
		c.fill = GridBagConstraints.BOTH;
		gridBagPanel.setConstraints(l, c);
		installedPanel.add(l);
		l = new JLabel(JAPMessages.getString("updateType") + ": ");
		c.gridy = 2;
		c.gridx = 0;
		c.weightx = 0;
		c.fill = GridBagConstraints.NONE;
		gridBagPanel.setConstraints(l, c);
		installedPanel.add(l);
		if (JAPConstants.m_bReleasedVersion)
		{
			l = new JLabel(JAPMessages.getString("updateReleaseVersion"));
		}
		else
		{
			l = new JLabel(JAPMessages.getString("updateDevelopmentVersion"));
		}
		c.gridx = 1;
		c.weightx = 1;
		c.fill = GridBagConstraints.BOTH;
		gridBagPanel.setConstraints(l, c);
		installedPanel.add(l);

		//The latestVersion-Panel
		gridBagPanel = new GridBagLayout();
		titledBorder = new TitledBorder(" " + JAPMessages.getString("updateTitleBorderLatest") + " ");
		JPanel latestPanel = new JPanel(gridBagPanel);
		latestPanel.setBorder(titledBorder);
		l = new JLabel("Version: ");
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 0;
		c.fill = GridBagConstraints.NONE;
		gridBagPanel.setConstraints(l, c);
		latestPanel.add(l);
		m_labelVersion = new JLabel(JAPMessages.getString("updateUnknown"));
		c.gridx = 1;
		c.weightx = 1;
		c.fill = GridBagConstraints.BOTH;
		gridBagPanel.setConstraints(m_labelVersion, c);
		latestPanel.add(m_labelVersion);
		l = new JLabel(JAPMessages.getString("updateLabelDate") + " ");
		c.gridy = 1;
		c.gridx = 0;
		c.weightx = 0;
		c.fill = GridBagConstraints.NONE;
		gridBagPanel.setConstraints(l, c);
		latestPanel.add(l);
		m_labelDate = new JLabel(JAPMessages.getString("updateUnknown"));
		c.gridx = 1;
		c.weightx = 1;
		c.fill = GridBagConstraints.BOTH;
		gridBagPanel.setConstraints(m_labelDate, c);
		latestPanel.add(m_labelDate);
		l = new JLabel(JAPMessages.getString("updateType") + ": ");
		c.gridy = 2;
		c.gridx = 0;
		c.weightx = 0;
		c.fill = GridBagConstraints.NONE;
		gridBagPanel.setConstraints(l, c);
		latestPanel.add(l);
		m_comboType = new JComboBox();
		m_comboType.addItem(JAPMessages.getString("updateReleaseVersion"));
		m_comboType.addItem(JAPMessages.getString("updateDevelopmentVersion"));
		if (!JAPConstants.m_bReleasedVersion)
		{
			m_comboType.setSelectedIndex(1);
		}
		m_comboType.setEnabled(false);
		m_comboType.addItemListener(this);
		c.gridx = 1;
		c.weightx = 1;
		c.fill = GridBagConstraints.BOTH;
		gridBagPanel.setConstraints(m_comboType, c);
		latestPanel.add(m_comboType);

		//The Info-Panel
		titledBorder = new TitledBorder(" Info ");
		JPanel infoPanel = new JPanel(new GridLayout(1, 1));
		infoPanel.setBorder(titledBorder);
		m_taInfo = new JTextArea(10, 40);
		m_taInfo.setEditable(false);
		m_taInfo.setHighlighter(null);
		JScrollPane scrollpane = new JScrollPane(m_taInfo);
		infoPanel.add(scrollpane);

		//Putting it all together
		GridBagConstraints cFrame = new GridBagConstraints();
		cFrame.insets = new Insets(10, 10, 10, 10);
		cFrame.gridx = 0;
		cFrame.gridy = 0;
		cFrame.weightx = 1;
		cFrame.weighty = 0;
		cFrame.anchor = GridBagConstraints.NORTHWEST;
		cFrame.fill = GridBagConstraints.BOTH;
		gridBagFrame.setConstraints(installedPanel, cFrame);
		panelRoot.add(installedPanel);

		cFrame.gridx = 1;
		cFrame.gridy = 0;
		cFrame.anchor = GridBagConstraints.NORTHEAST;
		panelRoot.add(latestPanel, cFrame);

		cFrame.gridx = 0;
		cFrame.gridy = 2;
		m_cbxAllowDirectUpdate = new JCheckBox(JAPMessages.getString(MSG_ALLOW_DIRECT_CONN));
		panelRoot.add(m_cbxAllowDirectUpdate, cFrame);

		cFrame.gridx = 0;
		cFrame.gridy = 3;
		m_cbxRemindOptionalUpdate = new JCheckBox(JAPMessages.getString(MSG_REMIND_OPTIONAL_UPDATE));
		panelRoot.add(m_cbxRemindOptionalUpdate, cFrame);

		cFrame.gridy = 4;
		cFrame.gridwidth = 2;
		cFrame.anchor = GridBagConstraints.CENTER;
		cFrame.fill = GridBagConstraints.BOTH;
		cFrame.weightx = 1.0;
		cFrame.weighty = 1.0;
		gridBagFrame.setConstraints(infoPanel, cFrame);
		panelRoot.add(infoPanel);

		cFrame.gridx = 0;
		cFrame.gridy = 5;
		cFrame.weighty = 0;
		cFrame.fill = GridBagConstraints.HORIZONTAL;
		cFrame.anchor = GridBagConstraints.SOUTH;
		gridBagFrame.setConstraints(buttonPanel, cFrame);
		panelRoot.add(buttonPanel);
	}

	protected boolean onOkPressed()
	{
		JAPModel.getInstance().allowUpdateViaDirectConnection(m_cbxAllowDirectUpdate.isSelected());
		JAPModel.getInstance().setReminderForOptionalUpdate(m_cbxRemindOptionalUpdate.isSelected());
		return true;
	}

	public void onResetToDefaultsPressed()
	{
		m_cbxAllowDirectUpdate.setSelected(true);
		m_cbxRemindOptionalUpdate.setSelected(true);
	}

	protected void onUpdateValues()
	{
		m_cbxAllowDirectUpdate.setSelected(JAPModel.getInstance().isUpdateViaDirectConnectionAllowed());
		m_cbxRemindOptionalUpdate.setSelected(JAPModel.getInstance().isReminderForOptionalUpdateActivated());
	}

	public void run()
	{
		//Thread Run Loop for getting the Version Infos...
		m_taInfo.setText(JAPMessages.getString("updateFetchVersionInfo"));
		m_releaseVersion = InfoServiceHolder.getInstance().getJAPVersionInfo(JAPVersionInfo.
			JAP_RELEASE_VERSION);
		m_devVersion = InfoServiceHolder.getInstance().getJAPVersionInfo(JAPVersionInfo.
			JAP_DEVELOPMENT_VERSION);
		if ( (m_releaseVersion == null) || (m_devVersion == null))
		{
			m_taInfo.setText(JAPMessages.getString("updateFetchVersionInfoFailed"));
		}
		else
		{
			m_comboType.setEnabled(true);
			String text = "";
			if (JAPConstants.m_bReleasedVersion)
			{
				text = m_releaseVersion.getJapVersion();
			}
			else
			{
				text = m_devVersion.getJapVersion();
			}
			if (JAPConstants.aktVersion.compareTo(text) >= 0)
			{
				text = JAPMessages.getString("japUpdate_YouHaveAlreadyTheNewestVersion");
			}
			else
			{
				text = JAPMessages.getString("japUpdate_NewVersionAvailable");
			}
			m_taInfo.setText(text);
			m_labelVersion.setText(m_releaseVersion.getJapVersion());
			if (m_releaseVersion.getDate() != null)
			{
				m_labelDate.setText(m_DateFormat.format(m_releaseVersion.getDate()));
			}
			else
			{
				m_labelDate.setText(JAPMessages.getString("updateUnknown"));
			}
			m_bttnUpgrade.setEnabled(true);
			if (JAPConstants.m_bReleasedVersion)
			{
				m_comboType.setSelectedIndex(0);
			}
			else
			{
				m_comboType.setSelectedIndex(1);
			}
			itemStateChanged(new ItemEvent(m_comboType, 0, m_comboType, ItemEvent.SELECTED));

		}
		m_bttnCheckForUpgrade.setEnabled(true);
	}

	public void actionPerformed(ActionEvent e)
	{
		if (e.getActionCommand().equals(COMMAND_UPGRADE))
		{
			try
			{
				m_threadGetVersionInfo.join();
			}
			catch (Exception ex)
			{}
			// User' wants to Update --> give the version Info and the jnlp-file
			if (m_comboType.getSelectedIndex() == 0)
			{
				new JAPUpdateWizard(m_releaseVersion);
			}
			else
			{
				new JAPUpdateWizard(m_devVersion);
			}
		}
		else if (e.getActionCommand().equals(COMMAND_CHECKFORUPGRADE))
		{
			m_bttnCheckForUpgrade.setEnabled(false);
			m_threadGetVersionInfo = new Thread(this);
			m_threadGetVersionInfo.start();
		}
	}

	public void itemStateChanged(ItemEvent e)
	{
		if (e.getStateChange() == ItemEvent.SELECTED)
		{
			if (m_comboType.getSelectedIndex() == 0) //Release
			{
				m_labelVersion.setText(m_releaseVersion.getJapVersion());
				if (m_releaseVersion.getDate() != null)
				{
					m_labelDate.setText(m_DateFormat.format(m_releaseVersion.getDate()));
				}
				else
				{
					m_labelDate.setText(JAPMessages.getString("updateUnknown"));
				}
			}
			else
			{
				m_labelVersion.setText(m_devVersion.getJapVersion());
				if (m_devVersion.getDate() != null)
				{
					m_labelDate.setText(m_DateFormat.format(m_devVersion.getDate()));
				}
				else
				{
					m_labelDate.setText(JAPMessages.getString("updateUnknown"));
				}
			}
		}
	}

	public String getTabTitle()
	{
		return JAPMessages.getString("ngUpdatePanelTitle");
	}

	protected void onRootPanelShown()
	{
		//Register help context
		JAPHelp.getInstance().getContextObj().setContext("update");
	}
}
