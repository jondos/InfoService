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

import java.awt.GridBagConstraints;
import java.awt.Insets;
import java.awt.GridBagLayout;
import java.awt.Font;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.JList;
import javax.swing.JScrollPane;
import javax.swing.ListSelectionModel;
import javax.swing.border.TitledBorder;
import javax.swing.event.AncestorListener;
import javax.swing.event.AncestorEvent;

import java.util.Vector;
import java.util.Enumeration;

import anon.infoservice.InfoService;
import anon.infoservice.InfoServiceDatabase;
import anon.infoservice.InfoServiceHolder;

/**
 * This is the configuration GUI for the infoservice (singleton).
 */
public class JAPConfInfoService {

	/**
	 * Stores the instance of JAPConfInfoService (Singleton).
	 */
	private static JAPConfInfoService japConfInfoServiceInstance = null;

	/**
	 * Stores the instance of the infoservice JList.
	 */
	private JList settingsInfoAllList;

	/**
	 * Stores the instance of the prefered infoservice JTextField.
	 */
	private JTextField settingsInfoPreferedField;

	/**
	 * Stores the current font setting.
	 */
	private Font japFontSetting;

	/**
	 * This creates a new instance of JAPConfInfoService. This is only used for setting some
	 * values. Use JAPConfInfoService.getInstance() for getting an instance of this class.
	 */
	private JAPConfInfoService() {
	}


	/**
	 * Returns the instance of JAPConfInfoService (Singleton). If there is no instance,
	 * there is a new one created.
	 *
	 * @return The JAPConfInfoService instance.
	 */
	public static JAPConfInfoService getInstance() {
		if (japConfInfoServiceInstance == null) {
			japConfInfoServiceInstance = new JAPConfInfoService();
		}
		return japConfInfoServiceInstance;
	}


	/**
	 * Updates the GUI (the list of all known infoservices and the prefered infoservice).
	 */
	public void updateGuiOutput() {
		if (settingsInfoAllList != null) {
			settingsInfoAllList.setListData(InfoServiceDatabase.getInstance().getInfoServiceList());
		}
		InfoService preferedInfoService = InfoServiceHolder.getInstance().getPreferedInfoService();
		if (preferedInfoService != null) {
			settingsInfoPreferedField.setText(preferedInfoService.getName());
		}
		else {
			/* should not happen */
			settingsInfoPreferedField.setText(JAPMessages.getString("settingsInfoNotAvailableText"));
		}
	}


	/**
	 * Helper class for updating the GUI when the infoservice tab is set to visible. So all values
	 * (infoservice list and prefered infoservice) are up to date.
	 */
	private class SettingsInfoPreferedAncestorListener implements AncestorListener {
		/**
		 * This method is called when the settingsInfoPreferedField is set to visible. This only
		 * happens if the whole infoservice tab is set to visible.
		 *
		 * @param event The fired AncestorEvent.
		 */
		public void ancestorAdded(AncestorEvent event) {
			/* update all values (including the infoservice list) if SettingsInfoPreferedField is shown */
			updateGuiOutput();
		}

		/**
		 * This method is called when the settingsInfoPreferedField is moved. This only
		 * happens if the whole infoservice tab is moved.
		 *
		 * @param event The fired AncestorEvent.
		 */
		public void ancestorMoved(AncestorEvent event) {
		}

		/**
		 * This method is called when the settingsInfoPreferedField is set to invisible. This only
		 * happens if the whole infoservice tab is set to invisible.
		 *
		 * @param event The fired AncestorEvent.
		 */
		public void ancestorRemoved(AncestorEvent event) {
		}
	}

	/**
	 * Creates the infoservice configuration panel with all components.
	 *
	 * @return The infoservice configuration panel.
	 */
	private JPanel createInfoServiceConfigPanel() {
		settingsInfoPreferedField = new JTextField();
		settingsInfoPreferedField.setFont(japFontSetting);
		settingsInfoPreferedField.setEditable(false);
		settingsInfoPreferedField.addAncestorListener(new SettingsInfoPreferedAncestorListener());
		JLabel settingsInfoPreferedLabel = new JLabel(JAPMessages.getString("settingsInfoPreferedLabel"));
		settingsInfoPreferedLabel.setFont(japFontSetting);

		settingsInfoAllList = new JList(InfoServiceDatabase.getInstance().getInfoServiceList());
		settingsInfoAllList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

		JScrollPane settingsInfoAllScrollPane = new JScrollPane(settingsInfoAllList);
		settingsInfoAllScrollPane.setFont(japFontSetting);

		JButton settingsInfoGetListButton = new JButton(JAPMessages.getString("settingsInfoGetListButton"));
		settingsInfoGetListButton.setFont(japFontSetting);
		settingsInfoGetListButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				/* if the GetList button is pressed, get the list of all infoservices from the internet
				 * and update the local database
				 */
				Vector infoservices = InfoServiceHolder.getInstance().getInfoServices();
				if (infoservices != null) {
					/* clear the list of all infoservices */
					InfoServiceDatabase.getInstance().removeAll();
					/* now put the new infoservices in the list */
					Enumeration it = infoservices.elements();
					while (it.hasMoreElements()) {
						InfoServiceDatabase.getInstance().update((InfoService)(it.nextElement()));
					}
					/* update the infoservice list */
					updateGuiOutput();
				}
				else {
					JAPConf.showError(JAPMessages.getString("settingsInfoGetListError"));
				}
			}
		});
		JButton settingsInfoSetPreferedButton = new JButton(JAPMessages.getString("settingsInfoSetPreferedButton"));
		settingsInfoSetPreferedButton.setFont(japFontSetting);
		settingsInfoSetPreferedButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				/* set the selected infoservice as prefered infoservice */
				InfoService selectedInfoService = (InfoService)(settingsInfoAllList.getSelectedValue());
				if (selectedInfoService != null) {
					/* change the prefered infoservice only, if something is selected */
					InfoServiceHolder.getInstance().setPreferedInfoService(selectedInfoService);
					settingsInfoPreferedField.setText(selectedInfoService.getName());
				}
			}
		});
		JButton settingsInfoRemoveButton = new JButton(JAPMessages.getString("settingsInfoRemoveButton"));
		settingsInfoRemoveButton.setFont(japFontSetting);
		settingsInfoRemoveButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				/* if the Remove button is pressed, remove the selected infoservice from the list of all
				 * infoservices
				 */
				InfoService selectedInfoService = (InfoService)(settingsInfoAllList.getSelectedValue());
				InfoServiceDatabase.getInstance().remove(selectedInfoService);
				/* update the infoservice list */
				updateGuiOutput();
			}
		});

		JLabel settingsInfoListLabel = new JLabel(JAPMessages.getString("settingsInfoListLabel"));
		settingsInfoListLabel.setFont(japFontSetting);

		JPanel configPanel = new JPanel();
		TitledBorder settingsInfoConfigBorder = new TitledBorder(JAPMessages.getString("settingsInfoConfigBorder"));
		settingsInfoConfigBorder.setTitleFont(japFontSetting);
		configPanel.setBorder(settingsInfoConfigBorder);

		GridBagLayout configPanelLayout = new GridBagLayout();
		configPanel.setLayout(configPanelLayout);

		GridBagConstraints configPanelConstraints = new GridBagConstraints();
		configPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
		configPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
		configPanelConstraints.weightx = 1.0;

		configPanelConstraints.gridx = 0;
		configPanelConstraints.gridy = 0;
		configPanelLayout.setConstraints(settingsInfoListLabel, configPanelConstraints);
		configPanel.add(settingsInfoListLabel);

		configPanelConstraints.gridx = 0;
		configPanelConstraints.gridy = 1;
		configPanelConstraints.gridheight = 3;
		configPanelConstraints.weighty = 1.0;
		configPanelConstraints.insets = new Insets(0, 0, 10, 0);
		configPanelConstraints.fill = GridBagConstraints.BOTH;
		configPanelLayout.setConstraints(settingsInfoAllScrollPane, configPanelConstraints);
		configPanel.add(settingsInfoAllScrollPane);
		configPanelConstraints.gridheight = 1;
		configPanelConstraints.weighty = 0.0;
		configPanelConstraints.insets = new Insets(0, 0, 0, 0);
		configPanelConstraints.fill = GridBagConstraints.HORIZONTAL;

		configPanelConstraints.gridx = 1;
		configPanelConstraints.gridy = 1;
		configPanelConstraints.weightx = 0.0;
		configPanelConstraints.insets = new Insets(0, 10, 5, 0);
		configPanelLayout.setConstraints(settingsInfoGetListButton, configPanelConstraints);
		configPanel.add(settingsInfoGetListButton);

		configPanelConstraints.gridx = 1;
		configPanelConstraints.gridy = 2;
		configPanelLayout.setConstraints(settingsInfoSetPreferedButton, configPanelConstraints);
		configPanel.add(settingsInfoSetPreferedButton);

		configPanelConstraints.gridx = 1;
		configPanelConstraints.gridy = 3;
		configPanelConstraints.insets = new Insets(0, 10, 0, 0);
		configPanelLayout.setConstraints(settingsInfoRemoveButton, configPanelConstraints);
		configPanel.add(settingsInfoRemoveButton);
		configPanelConstraints.weightx = 1.0;
		configPanelConstraints.insets = new Insets(0, 0, 0, 0);

		configPanelConstraints.gridx = 0;
		configPanelConstraints.gridy = 4;
		configPanelLayout.setConstraints(settingsInfoPreferedLabel, configPanelConstraints);
		configPanel.add(settingsInfoPreferedLabel);

		configPanelConstraints.gridx = 0;
		configPanelConstraints.gridy = 5;
		configPanelLayout.setConstraints(settingsInfoPreferedField, configPanelConstraints);
		configPanel.add(settingsInfoPreferedField);

		return configPanel;
	}

	/**
	 * Creates the manual infoservice configuration panel with all components.
	 *
	 * @return The manual infoservice configuration panel.
	 */
	private JPanel createInfoServiceManualPanel() {
		final JTextField settingsInfoHostField = new JTextField();
		settingsInfoHostField.setFont(japFontSetting);
		final JAPJIntField settingsInfoPortField = new JAPJIntField();
		settingsInfoPortField.setFont(japFontSetting);
		JButton settingsInfoAddButton = new JButton(JAPMessages.getString("settingsInfoAddButton"));
		settingsInfoAddButton.setFont(japFontSetting);
		settingsInfoAddButton.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent event) {
				/* if the Add button is pressed, we create a new InfoService from HostField and PortField
				 * and add it to the infoservice database
				 */
				try {
					InfoService newInfoService = new InfoService(settingsInfoHostField.getText().trim(), Integer.parseInt(settingsInfoPortField.getText().trim()));
					InfoServiceDatabase.getInstance().update(newInfoService);
					/* update the infoservice listbox and clear the fields */
					updateGuiOutput();
					settingsInfoHostField.setText("");
					settingsInfoPortField.setText("");
				}
				catch (Exception e) {
					JAPConf.showError(JAPMessages.getString("settingsInfoAddError"));
				}
			}
		});
		JLabel settingsInfoHostLabel = new JLabel(JAPMessages.getString("settingsInfoHostLabel"));
		settingsInfoHostLabel.setFont(japFontSetting);
		JLabel settingsInfoPortLabel = new JLabel(JAPMessages.getString("settingsInfoPortLabel"));
		settingsInfoPortLabel.setFont(japFontSetting);

		JPanel manualPanel = new JPanel();
		TitledBorder settingsInfoManualBorder = new TitledBorder(JAPMessages.getString("settingsInfoManualBorder"));
		settingsInfoManualBorder.setTitleFont(japFontSetting);
		manualPanel.setBorder(settingsInfoManualBorder);

		GridBagLayout manualPanelLayout = new GridBagLayout();
		manualPanel.setLayout(manualPanelLayout);

		GridBagConstraints manualPanelConstraints = new GridBagConstraints();
		manualPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
		manualPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
		manualPanelConstraints.weightx = 1.0;

		manualPanelConstraints.gridx = 0;
		manualPanelConstraints.gridy = 0;
		manualPanelLayout.setConstraints(settingsInfoHostLabel, manualPanelConstraints);
		manualPanel.add(settingsInfoHostLabel);

		manualPanelConstraints.gridx = 0;
		manualPanelConstraints.gridy = 1;
		manualPanelConstraints.insets = new Insets(0, 0, 3, 0);
		manualPanelLayout.setConstraints(settingsInfoHostField, manualPanelConstraints);
		manualPanel.add(settingsInfoHostField);
		manualPanelConstraints.insets = new Insets(0, 0, 0, 0);

		manualPanelConstraints.gridx = 0;
		manualPanelConstraints.gridy = 2;
		manualPanelLayout.setConstraints(settingsInfoPortLabel, manualPanelConstraints);
		manualPanel.add(settingsInfoPortLabel);

		manualPanelConstraints.gridx = 0;
		manualPanelConstraints.gridy = 3;
		manualPanelConstraints.weighty = 1.0;
		manualPanelLayout.setConstraints(settingsInfoPortField, manualPanelConstraints);
		manualPanel.add(settingsInfoPortField);

		manualPanelConstraints.gridx = 1;
		manualPanelConstraints.gridy = 3;
		manualPanelConstraints.weightx = 0.0;
		manualPanelConstraints.insets = new Insets(0, 10, 0, 0);
		manualPanelLayout.setConstraints(settingsInfoAddButton, manualPanelConstraints);
		manualPanel.add(settingsInfoAddButton);

		return manualPanel;
	}

	/**
	 * Creates the infoservice panel with all child-panels.
	 *
	 * @param japFontSetting The Font to use.
	 *
	 * @return The infoservice panel.
	 */
	public JPanel createInfoServicePanel(Font japFontSetting) {
		this.japFontSetting = japFontSetting;

		JPanel configPanel = createInfoServiceConfigPanel();
		JPanel manualPanel = createInfoServiceManualPanel();

		JPanel infoServicePanel = new JPanel();

		GridBagLayout infoServicePanelLayout = new GridBagLayout();
		infoServicePanel.setLayout(infoServicePanelLayout);

		GridBagConstraints infoServicePanelConstraints = new GridBagConstraints();
		infoServicePanelConstraints.anchor = GridBagConstraints.NORTHWEST;
		infoServicePanelConstraints.fill = GridBagConstraints.BOTH;
		infoServicePanelConstraints.weightx = 1.0;

		infoServicePanelConstraints.gridx = 0;
		infoServicePanelConstraints.gridy = 0;
		infoServicePanelConstraints.weighty = 1.0;
		infoServicePanelLayout.setConstraints(configPanel, infoServicePanelConstraints);
		infoServicePanel.add(configPanel);
		infoServicePanelConstraints.weighty = 0.0;

		infoServicePanelConstraints.gridx = 0;
		infoServicePanelConstraints.gridy = 1;
		infoServicePanelLayout.setConstraints(manualPanel, infoServicePanelConstraints);
		infoServicePanel.add(manualPanel);

		return infoServicePanel;
	}
}
