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

import java.util.Enumeration;
import java.util.Vector;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;

import anon.infoservice.HTTPConnectionFactory;
import anon.infoservice.InfoServiceDBEntry;
import anon.infoservice.Database;
import anon.infoservice.InfoServiceHolder;
import anon.infoservice.ListenerInterface;

/**
 * This is the configuration GUI for the infoservice.
 */
public class JAPConfInfoService extends AbstractJAPConfModule
{

	/**
	 * Stores the instance of the infoservice JList.
	 */
	private JList settingsInfoAllList;

	/**
	 * Stores the instance of interface info host label.
	 */
	private JLabel m_interfaceInfoHostLabel;

	/**
	 * Stores the instance of interface info port label.
	 */
	private JLabel m_interfaceInfoPortLabel;

	/**
	 * Stores the instance of interface info up button.
	 */
	private JButton m_interfaceInfoUpButton;

	/**
	 * Stores the instance of interface info down button.
	 */
	private JButton m_interfaceInfoDownButton;

	/**
	 * Stores the instance of the interface selection field.
	 */
	private JTextField m_interfaceInfoInterfaceField;

	/**
	 * Stores the listener interfaces of the selected infoservice.
	 */
	private Vector m_currentListenerInterfaces;

	/**
	 * Stores the number of the currently selected listener interface.
	 */
	private int m_selectedListenerInterface;

	/**
	 * We need this for some synchronizing stuff because anonymous internal classes can't get this
	 * instance via "this".
	 */
	private JAPConfInfoService infoServiceConfigModuleInstance;

	/**
   * Stores the current prefered InfoService.
   */
  private InfoServiceDBEntry m_preferedInfoService;
  

  /**
	 * Constructor for JAPConfInfoService. We do some initializing here.
	 */
	public JAPConfInfoService()
	{
		super(new JAPConfInfoServiceSavePoint());
		infoServiceConfigModuleInstance = this;
	}

	/**
	 * Creates the infoservice root panel with all child-panels.
	 */
	public void recreateRootPanel()
	{
		JPanel rootPanel = getRootPanel();

		/* clear the whole root panel */
		rootPanel.removeAll();

		/* insert all components in the root panel */
    JTabbedPane infoServiceTabPane = new JTabbedPane();
    infoServiceTabPane.setFont(getFontSetting());
    infoServiceTabPane.insertTab(JAPMessages.getString("settingsInfoSettingsTabTitle"), null, createInfoServiceConfigPanel(), null, 0);
    infoServiceTabPane.insertTab(JAPMessages.getString("settingsInfoAdvancedTabTitle"), null, createInfoServiceAdvancedPanel(), null, 1);

		GridBagLayout rootPanelLayout = new GridBagLayout();
		rootPanel.setLayout(rootPanelLayout);

		GridBagConstraints rootPanelConstraints = new GridBagConstraints();
		rootPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
		rootPanelConstraints.fill = GridBagConstraints.BOTH;
		rootPanelConstraints.weightx = 1.0;
		rootPanelConstraints.weighty = 1.0;

		rootPanelConstraints.gridx = 0;
    rootPanelConstraints.gridy = 0;
    rootPanelLayout.setConstraints(infoServiceTabPane, rootPanelConstraints);
    rootPanel.add(infoServiceTabPane);

		/* set current values */
		updateGuiOutput();
	}

	/**
	 * Returns the title for the infoservice configuration tab.
	 *
	 * @return The title for the infoservice configuration tab.
	 */
	public String getTabTitle()
	{
		return JAPMessages.getString("confInfoTab");
	}

	/**
	 * This method is called automatically by AbstractJAPConfModule if the infoservice tab comes to
	 * foreground. This method calls updateGuiOutput().
	 */
	protected void onRootPanelShown()
	{
		updateGuiOutput();
	}

	protected void onResetToDefaultsPressed()
	{
		/* update the GUI */
		updateGuiOutput();
	}

	/**
	 * Creates the infoservice configuration panel with all components.
	 *
	 * @return The infoservice configuration panel.
	 */
	private JPanel createInfoServiceConfigPanel()
	{
		final JPanel configPanel = new JPanel();

		settingsInfoAllList = new JList(Database.getInstance(InfoServiceDBEntry.class).getEntryList());
		settingsInfoAllList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    settingsInfoAllList.setCellRenderer(new ListCellRenderer() {
      public Component getListCellRendererComponent(JList a_list, Object a_value, int a_index, boolean a_isSelected, boolean a_cellHasFocus) {
        JLabel returnLabel = null;
        if (((InfoServiceDBEntry)a_value).isUserDefined()) {
          returnLabel = new JLabel(((InfoServiceDBEntry)a_value).getName(), JAPUtil.loadImageIcon("servermanuell.gif", true), JLabel.LEFT);
        }
        else {
          returnLabel = new JLabel(((InfoServiceDBEntry)a_value).getName(), JAPUtil.loadImageIcon("serverfrominternet.gif", true), JLabel.LEFT);
        }
        returnLabel.setOpaque(true);
        /* the defualt is a non-bold font */
        returnLabel.setFont(new Font(getFontSetting().getName(), getFontSetting().getStyle() & (~Font.BOLD), getFontSetting().getSize()));
        if (m_preferedInfoService != null) {
          if (m_preferedInfoService.getId().equals(((InfoServiceDBEntry)a_value).getId())) {
            /* print the prefered InfoService in a bold font */
            returnLabel.setFont(new Font(getFontSetting().getName(), getFontSetting().getStyle() | Font.BOLD, getFontSetting().getSize()));
          }
        }
        if (a_isSelected == true) {
          returnLabel.setForeground(a_list.getSelectionForeground());
          returnLabel.setBackground(a_list.getSelectionBackground());
        }
        else {
          returnLabel.setForeground(a_list.getForeground());
          returnLabel.setBackground(a_list.getBackground());
        }
        return returnLabel;
			}
		});

		JScrollPane settingsInfoAllScrollPane = new JScrollPane(settingsInfoAllList);
		settingsInfoAllScrollPane.setFont(getFontSetting());

		JButton settingsInfoGetListButton = new JButton(JAPMessages.getString("settingsInfoGetListButton"));
		settingsInfoGetListButton.setFont(getFontSetting());
		settingsInfoGetListButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent event)
			{
				/* if the GetList button is pressed, get the list of all infoservices from the internet
				 * and update the local database
				 */
				Vector infoservices = InfoServiceHolder.getInstance().getInfoServices();
				if (infoservices != null)
				{
					/* clear the list of all infoservices */
					Database.getInstance(InfoServiceDBEntry.class).removeAll();
					/* now put the new infoservices in the list */
					Enumeration it = infoservices.elements();
					while (it.hasMoreElements())
					{
            InfoServiceDBEntry currentInfoService = (InfoServiceDBEntry)(it.nextElement());
            Database.getInstance(InfoServiceDBEntry.class).update(currentInfoService);
            InfoServiceDBEntry preferedInfoService = InfoServiceHolder.getInstance().getPreferedInfoService();
            if (preferedInfoService != null) {
              /* if the current infoservice ID is equal to the ID of the prefered infoservice,
               * update the prefered infoservice also
               */
              if (preferedInfoService.getId().equals(currentInfoService.getId())) {
                InfoServiceHolder.getInstance().setPreferedInfoService(currentInfoService);
              }
            }
					}
					/* update the infoservice list */
					updateGuiOutput();
				}
				else
				{
					JOptionPane.showMessageDialog(configPanel,
												  JAPMessages.getString("settingsInfoGetListError"),
												  JAPMessages.getString("ERROR"), JOptionPane.ERROR_MESSAGE);
				}
			}
		});
		JButton settingsInfoSetPreferedButton = new JButton(JAPMessages.getString(
			"settingsInfoSetPreferedButton"));
		settingsInfoSetPreferedButton.setFont(getFontSetting());
		settingsInfoSetPreferedButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent event)
			{
				/* set the selected infoservice as prefered infoservice */
				InfoServiceDBEntry selectedInfoService = (InfoServiceDBEntry) (settingsInfoAllList.getSelectedValue());
				if (selectedInfoService != null)
				{
					/* change the prefered infoservice only, if something is selected */
					InfoServiceHolder.getInstance().setPreferedInfoService(selectedInfoService);
          updateGuiOutput();
				}
			}
		});
		JButton settingsInfoAddButton = new JButton(JAPMessages.getString("settingsInfoAddButton"));
		settingsInfoAddButton.setFont(getFontSetting());
		settingsInfoAddButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent event)
			{
				/* if the Add button is pressed, show the Add InfoService dialog */
				showInfoServiceManualAddDialog();
			}
		});
		JButton settingsInfoRemoveButton = new JButton(JAPMessages.getString("settingsInfoRemoveButton"));
		settingsInfoRemoveButton.setFont(getFontSetting());
		settingsInfoRemoveButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent event)
			{
				/* if the Remove button is pressed, remove the selected infoservice from the list of all
				 * infoservices (if it is not the prefered infoservice)
				 */
				InfoServiceDBEntry selectedInfoService = (InfoServiceDBEntry) (settingsInfoAllList.getSelectedValue());
				InfoServiceDBEntry preferedInfoService = InfoServiceHolder.getInstance().getPreferedInfoService();
				if (selectedInfoService != null)
				{
					if (preferedInfoService != null)
					{
						if (preferedInfoService.getId().equals(selectedInfoService.getId()))
						{
							JOptionPane.showMessageDialog(configPanel,
								JAPMessages.getString("settingsInfoRemovePreferedError"),
								JAPMessages.getString("ERROR"), JOptionPane.ERROR_MESSAGE);
						}
						else
						{
							Database.getInstance(InfoServiceDBEntry.class).remove(selectedInfoService);
							/* update the infoservice list */
							updateGuiOutput();
						}
					}
					else
					{
						Database.getInstance(InfoServiceDBEntry.class).remove(selectedInfoService);
						/* update the infoservice list */
						updateGuiOutput();
					}
				}
			}
		});

		JLabel settingsInfoListLabel = new JLabel(JAPMessages.getString("settingsInfoListLabel"));
		settingsInfoListLabel.setFont(getFontSetting());

		TitledBorder settingsInfoConfigBorder = new TitledBorder(JAPMessages.getString(
			"settingsInfoConfigBorder"));
		settingsInfoConfigBorder.setTitleFont(getFontSetting());
		configPanel.setBorder(settingsInfoConfigBorder);

		GridBagLayout configPanelLayout = new GridBagLayout();
		configPanel.setLayout(configPanelLayout);

		GridBagConstraints configPanelConstraints = new GridBagConstraints();
		configPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
		configPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
		configPanelConstraints.weightx = 1.0;

		configPanelConstraints.gridx = 0;
		configPanelConstraints.gridy = 0;
    configPanelConstraints.gridwidth = 4;
    configPanelConstraints.insets = new Insets(5, 5, 0, 5);
		configPanelLayout.setConstraints(settingsInfoListLabel, configPanelConstraints);
		configPanel.add(settingsInfoListLabel);

		configPanelConstraints.gridx = 0;
		configPanelConstraints.gridy = 1;
		configPanelConstraints.weighty = 1.0;
    configPanelConstraints.insets = new Insets(0, 5, 10, 5);
		configPanelConstraints.fill = GridBagConstraints.BOTH;
		configPanelLayout.setConstraints(settingsInfoAllScrollPane, configPanelConstraints);
		configPanel.add(settingsInfoAllScrollPane);

    configPanelConstraints.gridx = 0;
    configPanelConstraints.gridy = 2;
		configPanelConstraints.weightx = 0.0;
    configPanelConstraints.weighty = 0.0;
    configPanelConstraints.gridwidth = 1;
    configPanelConstraints.fill = GridBagConstraints.NONE;
    configPanelConstraints.insets = new Insets(0, 5, 20, 5);
		configPanelLayout.setConstraints(settingsInfoGetListButton, configPanelConstraints);
		configPanel.add(settingsInfoGetListButton);

		configPanelConstraints.gridx = 1;
		configPanelConstraints.gridy = 2;
    configPanelConstraints.insets = new Insets(0, 5, 20, 5);
		configPanelLayout.setConstraints(settingsInfoSetPreferedButton, configPanelConstraints);
		configPanel.add(settingsInfoSetPreferedButton);

    configPanelConstraints.gridx = 2;
    configPanelConstraints.gridy = 2;
    configPanelConstraints.insets = new Insets(0, 5, 20, 5);
		configPanelLayout.setConstraints(settingsInfoAddButton, configPanelConstraints);
		configPanel.add(settingsInfoAddButton);

    configPanelConstraints.gridx = 3;
    configPanelConstraints.gridy = 2;
    configPanelConstraints.weightx = 1.0;
    configPanelConstraints.insets = new Insets(0, 5, 20, 5);
		configPanelLayout.setConstraints(settingsInfoRemoveButton, configPanelConstraints);
		configPanel.add(settingsInfoRemoveButton);

		return configPanel;
	}

	/**
	 * Creates the infoservice interface info panel with all components.
	 *
	 * @return The interface info panel.
	 */
	private JPanel createInfoServiceInterfacePanel()
	{
		JPanel interfacePanel = new JPanel();
		TitledBorder settingsInfoInterfaceBorder = new TitledBorder(JAPMessages.getString(
			"settingsInfoInterfaceBorder"));
		settingsInfoInterfaceBorder.setTitleFont(getFontSetting());
		interfacePanel.setBorder(settingsInfoInterfaceBorder);

		m_interfaceInfoUpButton = new JButton(JAPUtil.loadImageIcon(JAPConstants.IMAGE_ARROW_UP, true));
		m_interfaceInfoUpButton.setMargin(new Insets(0, 0, 0, 0));
		m_interfaceInfoUpButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent event)
			{
				/* if the up button is pressed, show the previous listener interface */
				synchronized (infoServiceConfigModuleInstance)
				{
					if (m_selectedListenerInterface > 0)
					{
						m_selectedListenerInterface--;
					}
				}
				updateInterfaceInfo();
			}
		});
		m_interfaceInfoDownButton = new JButton(JAPUtil.loadImageIcon(JAPConstants.IMAGE_ARROW_DOWN, true));
		m_interfaceInfoDownButton.setMargin(new Insets(0, 0, 0, 0));
		m_interfaceInfoDownButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent event)
			{
				/* if the down button is pressed, show the next listener interface */
				synchronized (infoServiceConfigModuleInstance)
				{
					if (m_selectedListenerInterface > -1)
					{
						/* there are listener interfaces in the list */
						if (m_selectedListenerInterface < m_currentListenerInterfaces.size() - 1)
						{
							m_selectedListenerInterface++;
						}
					}
				}
				updateInterfaceInfo();
			}
		});

		m_interfaceInfoInterfaceField = new JTextField(JAPMessages.getString("settingsInfoInterfaceField"));
		m_interfaceInfoInterfaceField.setFont(getFontSetting());
		m_interfaceInfoInterfaceField.setEditable(false);
		m_interfaceInfoHostLabel = new JLabel(JAPMessages.getString("settingsInfoInterfaceHostLabel"));
		m_interfaceInfoHostLabel.setFont(getFontSetting());
		m_interfaceInfoPortLabel = new JLabel(JAPMessages.getString("settingsInfoInterfacePortLabel"));
		m_interfaceInfoPortLabel.setFont(getFontSetting());

		GridBagLayout interfacePanelLayout = new GridBagLayout();
		interfacePanel.setLayout(interfacePanelLayout);
		GridBagConstraints interfacePanelConstraints = new GridBagConstraints();

		interfacePanelConstraints.anchor = GridBagConstraints.WEST;
		interfacePanelConstraints.fill = GridBagConstraints.HORIZONTAL;
		interfacePanelConstraints.gridheight = 2;
		interfacePanelConstraints.weightx = 1.0;
		interfacePanelConstraints.weighty = 0.0;
		interfacePanelConstraints.gridx = 0;
		interfacePanelConstraints.gridy = 0;
		interfacePanelConstraints.insets = new Insets(0, 0, 10, 0);
		interfacePanelLayout.setConstraints(m_interfaceInfoInterfaceField, interfacePanelConstraints);
		interfacePanel.add(m_interfaceInfoInterfaceField);

		interfacePanelConstraints.anchor = GridBagConstraints.NORTHWEST;
		interfacePanelConstraints.fill = GridBagConstraints.NONE;
		interfacePanelConstraints.gridheight = 1;
		interfacePanelConstraints.weightx = 0.0;
		interfacePanelConstraints.gridx = 1;
		interfacePanelConstraints.gridy = 0;
		interfacePanelConstraints.insets = new Insets(0, 5, 0, 0);
		interfacePanelLayout.setConstraints(m_interfaceInfoUpButton, interfacePanelConstraints);
		interfacePanel.add(m_interfaceInfoUpButton);

		interfacePanelConstraints.gridx = 1;
		interfacePanelConstraints.gridy = 1;
		interfacePanelConstraints.insets = new Insets(0, 5, 10, 0);
		interfacePanelLayout.setConstraints(m_interfaceInfoDownButton, interfacePanelConstraints);
		interfacePanel.add(m_interfaceInfoDownButton);

		interfacePanelConstraints.gridwidth = 2;
		interfacePanelConstraints.gridx = 0;
		interfacePanelConstraints.gridy = 2;
		interfacePanelConstraints.insets = new Insets(0, 0, 5, 0);
		interfacePanelLayout.setConstraints(m_interfaceInfoHostLabel, interfacePanelConstraints);
		interfacePanel.add(m_interfaceInfoHostLabel);

		interfacePanelConstraints.weighty = 1.0;
		interfacePanelConstraints.gridx = 0;
		interfacePanelConstraints.gridy = 3;
		interfacePanelLayout.setConstraints(m_interfaceInfoPortLabel, interfacePanelConstraints);
		interfacePanel.add(m_interfaceInfoPortLabel);

		return interfacePanel;
	}

	/**
	 * Shows the infoservice add dialog.
	 */
	private void showInfoServiceManualAddDialog()
	{
		final JAPDialog addDialog = new JAPDialog(getRootPanel(),
												  JAPMessages.getString("settingsInfoAddDialogTitle"));
		final JPanel manualPanel = addDialog.getRootPanel();

		final JTextField settingsInfoHostField = new JTextField();
		settingsInfoHostField.setFont(getFontSetting());
		settingsInfoHostField.setColumns(30);
		final JAPJIntField settingsInfoPortField = new JAPJIntField();
		settingsInfoPortField.setFont(getFontSetting());
    final JTextField settingsInfoNameField = new JTextField();
    settingsInfoNameField.setFont(getFontSetting());
    settingsInfoNameField.setColumns(30);
    
		JButton settingsInfoAddDialogAddButton = new JButton(JAPMessages.getString(
			"settingsInfoAddDialogAddButton"));
		settingsInfoAddDialogAddButton.setFont(getFontSetting());
		settingsInfoAddDialogAddButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent event)
			{
				/* if the Add button is pressed, we create a new InfoService from HostField and PortField,
				 * add it to the infoservice database and close the dialog
				 */
				try
				{
          String infoServiceName = settingsInfoNameField.getText().trim();
          if (infoServiceName.equals("")) {
            /* use gernerated default name */
            infoServiceName = null;
          }
          InfoServiceDBEntry newInfoService = new InfoServiceDBEntry(infoServiceName, new ListenerInterface(settingsInfoHostField.getText().trim(), Integer.parseInt(settingsInfoPortField.getText().trim())).toVector(), true);
					Database.getInstance(InfoServiceDBEntry.class).update(newInfoService);
          addDialog.dispose();
					/* update the infoservice list */
					updateGuiOutput();
				}
				catch (Exception e)
				{
					JOptionPane.showMessageDialog(manualPanel, JAPMessages.getString("settingsInfoAddError"),
												  JAPMessages.getString("ERROR"), JOptionPane.ERROR_MESSAGE);
				}
			}
		});
		JButton settingsInfoAddDialogCancelButton = new JButton(JAPMessages.getString("cancelButton"));
		settingsInfoAddDialogCancelButton.setFont(getFontSetting());
		settingsInfoAddDialogCancelButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent event)
			{
				/* if the Cancel button is pressed, close the dialog */
        addDialog.dispose();
			}
		});
		JLabel settingsInfoHostLabel = new JLabel(JAPMessages.getString("settingsInfoHostLabel"));
		settingsInfoHostLabel.setFont(getFontSetting());
		JLabel settingsInfoPortLabel = new JLabel(JAPMessages.getString("settingsInfoPortLabel"));
		settingsInfoPortLabel.setFont(getFontSetting());
    JLabel settingsInfoNameLabel = new JLabel(JAPMessages.getString("settingsInfoNameLabel"));
    settingsInfoNameLabel.setFont(getFontSetting());

		TitledBorder settingsInfoManualBorder = new TitledBorder(JAPMessages.getString(
			"settingsInfoManualBorder"));
		settingsInfoManualBorder.setTitleFont(getFontSetting());
		manualPanel.setBorder(settingsInfoManualBorder);

		GridBagLayout manualPanelLayout = new GridBagLayout();
		manualPanel.setLayout(manualPanelLayout);

		GridBagConstraints manualPanelConstraints = new GridBagConstraints();
		manualPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
		manualPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
		manualPanelConstraints.gridwidth = 2;
		manualPanelConstraints.weightx = 1.0;

		manualPanelConstraints.gridx = 0;
		manualPanelConstraints.gridy = 0;
    manualPanelConstraints.insets = new Insets(5, 5, 0, 5);
		manualPanelLayout.setConstraints(settingsInfoHostLabel, manualPanelConstraints);
		manualPanel.add(settingsInfoHostLabel);

		manualPanelConstraints.gridx = 0;
		manualPanelConstraints.gridy = 1;
    manualPanelConstraints.insets = new Insets(0, 5, 5, 5);
		manualPanelLayout.setConstraints(settingsInfoHostField, manualPanelConstraints);
		manualPanel.add(settingsInfoHostField);

		manualPanelConstraints.gridx = 0;
		manualPanelConstraints.gridy = 2;
    manualPanelConstraints.insets = new Insets(0, 5, 0, 5);
		manualPanelLayout.setConstraints(settingsInfoPortLabel, manualPanelConstraints);
		manualPanel.add(settingsInfoPortLabel);

		manualPanelConstraints.gridx = 0;
		manualPanelConstraints.gridy = 3;
    manualPanelConstraints.insets = new Insets(0, 5, 5, 5);
		manualPanelLayout.setConstraints(settingsInfoPortField, manualPanelConstraints);
		manualPanel.add(settingsInfoPortField);

		manualPanelConstraints.gridx = 0;
		manualPanelConstraints.gridy = 4;
    manualPanelConstraints.insets = new Insets(0, 5, 0, 5);
    manualPanelLayout.setConstraints(settingsInfoNameLabel, manualPanelConstraints);
    manualPanel.add(settingsInfoNameLabel);

    manualPanelConstraints.gridx = 0;
    manualPanelConstraints.gridy = 5;
    manualPanelConstraints.insets = new Insets(0, 5, 20, 5);
    manualPanelLayout.setConstraints(settingsInfoNameField, manualPanelConstraints);
    manualPanel.add(settingsInfoNameField);

    manualPanelConstraints.gridwidth = 1;
    manualPanelConstraints.gridx = 0;
    manualPanelConstraints.gridy = 6;
		manualPanelConstraints.fill = GridBagConstraints.NONE;
		manualPanelConstraints.weighty = 1.0;
		manualPanelConstraints.weightx = 0.0;
    manualPanelConstraints.insets = new Insets(0, 5, 5, 0);
		manualPanelLayout.setConstraints(settingsInfoAddDialogAddButton, manualPanelConstraints);
		manualPanel.add(settingsInfoAddDialogAddButton);

		manualPanelConstraints.gridx = 1;
    manualPanelConstraints.gridy = 6;
		manualPanelConstraints.weightx = 1.0;
    manualPanelConstraints.insets = new Insets(0, 10, 5, 5);
		manualPanelLayout.setConstraints(settingsInfoAddDialogCancelButton, manualPanelConstraints);
		manualPanel.add(settingsInfoAddDialogCancelButton);

		addDialog.align();
		addDialog.show();
	}

	/**
   * Creates the infoservice advanced configuration panel with all components.
   *
   * @return The ifoservice advanced configuration panel.
	 */
  private JPanel createInfoServiceAdvancedPanel()
	{
    JPanel expertPanel = new JPanel();

    final JCheckBox disableInfoServiceBox = new JCheckBox(JAPMessages.getString("settingsInfoDisableInfoService"), JAPModel.isInfoServiceDisabled());
		disableInfoServiceBox.setFont(getFontSetting());
    disableInfoServiceBox.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent event) {
        /* enable/disable the automatic infoservice requests */
					JAPController.setInfoServiceDisabled(disableInfoServiceBox.isSelected());
				}
    });

    final JCheckBox disableChangeInfoServiceBox = new JCheckBox(JAPMessages.getString("settingsInfoDisableChangeInfoService"), !InfoServiceHolder.getInstance().isChangeInfoServices());
    disableChangeInfoServiceBox.setFont(getFontSetting());
    disableChangeInfoServiceBox.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent event) {
        /* enable/disable the automatic changes of the infoservices */
        InfoServiceHolder.getInstance().setChangeInfoServices(!disableChangeInfoServiceBox.isSelected());
      }
    });

    final JTextField infoServiceTimeoutField = new JTextField() {
      protected Document createDefaultModel() {
        return (new PlainDocument() {
          public void insertString(int a_position, String a_stringToInsert, AttributeSet a_attributes) throws BadLocationException {
            try {
              int timeout = Integer.parseInt(getText(0, getLength()) + a_stringToInsert);
              if ((timeout >= 1) && (timeout <= 60)) {
                /* timeout is within the range (1 .. 60 seconds) -> insert the String */
                super.insertString(a_position, a_stringToInsert, a_attributes);
              }
            }
            catch (NumberFormatException e) {
              /* do nothing (because of invalid chars) */
				}
			}
		});
      }
    };
    infoServiceTimeoutField.addFocusListener(new FocusAdapter() {
      public void focusLost(FocusEvent a_focusEvent) {
        /* we lost the focus -> try to update the infoservice timeout setting */
        try {
          int timeout = Integer.parseInt(infoServiceTimeoutField.getText());
          HTTPConnectionFactory.getInstance().setTimeout(timeout);
        }
        catch (NumberFormatException e) {
          /* do nothing (empty field) */
        }
        /* show the current timeout in the tiemout field */
        infoServiceTimeoutField.setText(Integer.toString(HTTPConnectionFactory.getInstance().getTimeout()));
			}
		});
    infoServiceTimeoutField.setText(Integer.toString(HTTPConnectionFactory.getInstance().getTimeout()));
    infoServiceTimeoutField.setColumns(3);
    infoServiceTimeoutField.setFont(getFontSetting());
    
		JLabel settingsInfoTimeoutLabel = new JLabel(JAPMessages.getString("settingsInfoTimeoutLabel"));
		settingsInfoTimeoutLabel.setFont(getFontSetting());

    TitledBorder settingsInfoExpertBorder = new TitledBorder(JAPMessages.getString("settingsInfoExpertBorder"));
		settingsInfoExpertBorder.setTitleFont(getFontSetting());
		expertPanel.setBorder(settingsInfoExpertBorder);

		GridBagLayout expertPanelLayout = new GridBagLayout();
		expertPanel.setLayout(expertPanelLayout);

		GridBagConstraints expertPanelConstraints = new GridBagConstraints();
		expertPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
		expertPanelConstraints.fill = GridBagConstraints.NONE;

		expertPanelConstraints.weightx = 0.0;
    expertPanelConstraints.weighty = 0.0;
		expertPanelConstraints.gridx = 0;
		expertPanelConstraints.gridy = 0;
    expertPanelConstraints.gridwidth = 1;
		expertPanelConstraints.insets = new Insets(5, 5, 10, 0);
		expertPanelLayout.setConstraints(settingsInfoTimeoutLabel, expertPanelConstraints);
		expertPanel.add(settingsInfoTimeoutLabel);

    expertPanelConstraints.gridx = 1;
		expertPanelConstraints.gridy = 0;
		expertPanelConstraints.weightx = 1.0;
    expertPanelConstraints.insets = new Insets(5, 5, 10, 5);
		expertPanelLayout.setConstraints(infoServiceTimeoutField, expertPanelConstraints);
		expertPanel.add(infoServiceTimeoutField);

		expertPanelConstraints.gridx = 0;
		expertPanelConstraints.gridy = 1;
    expertPanelConstraints.gridwidth = 2;
    expertPanelConstraints.insets = new Insets(0, 5, 10, 5);
		expertPanelLayout.setConstraints(disableInfoServiceBox, expertPanelConstraints);
		expertPanel.add(disableInfoServiceBox);

		expertPanelConstraints.gridx = 0;
		expertPanelConstraints.gridy = 2;
		expertPanelConstraints.weighty = 1.0;
    expertPanelConstraints.insets = new Insets(0, 5, 10, 5);
    expertPanelLayout.setConstraints(disableChangeInfoServiceBox, expertPanelConstraints);
    expertPanel.add(disableChangeInfoServiceBox);

    return expertPanel;
	}

	/**
	 * Updates the GUI (the list of all known infoservices, the prefered infoservice, ...). This
	 * method is called automatically, if the infoservice tab comes to foreground.
	 */
	private void updateGuiOutput()
	{
		synchronized (this)
		{
			/* only work on consistent data */
			Vector knownInfoServices = Database.getInstance(InfoServiceDBEntry.class).getEntryList();
      m_preferedInfoService = InfoServiceHolder.getInstance().getPreferedInfoService();
      if (m_preferedInfoService != null)
			{
				/* if the prefered InfoService is not in the list (because it is expired), add it */
				boolean preferedIsInList = false;
				Enumeration knownInfoServiceEnumeration = knownInfoServices.elements();
				while (knownInfoServiceEnumeration.hasMoreElements())
				{
					InfoServiceDBEntry tempInfoService = (InfoServiceDBEntry) (knownInfoServiceEnumeration.nextElement());
          if (tempInfoService.getId().equals(m_preferedInfoService.getId()))
					{
						preferedIsInList = true;
					}
				}
				if (preferedIsInList == false)
				{
          knownInfoServices.addElement(m_preferedInfoService);
				}
			}
			settingsInfoAllList.setListData(knownInfoServices);
		}
    //updateSelectedInfoService();
	}

	/**
	 * Updates the listener interfaces list for the selected infoservice.
	 */
	private void updateSelectedInfoService()
	{
		/* get the listener interfaces of the selected infoservice */
		InfoServiceDBEntry selectedInfoService = (InfoServiceDBEntry) (settingsInfoAllList.getSelectedValue());
		if (selectedInfoService != null)
		{
			synchronized (this)
			{
				/* we need exclusive access */
				m_currentListenerInterfaces = selectedInfoService.getListenerInterfaces();
				if (m_currentListenerInterfaces.size() > 0)
				{
					m_selectedListenerInterface = 0;
				}
				else
				{
					m_selectedListenerInterface = -1;
				}
			}
		}
		else
		{
			synchronized (this)
			{
				/* we need exclusive access */
				m_currentListenerInterfaces = null;
				m_selectedListenerInterface = -1;
			}
		}
		updateInterfaceInfo();
	}

	/**
	 * Updates the listener interface info panel.
	 */
	private void updateInterfaceInfo()
	{
		synchronized (this)
		{
			/* get only consistent data */
			if (m_selectedListenerInterface > -1)
			{
				ListenerInterface currentInterface = (ListenerInterface) (m_currentListenerInterfaces.
					elementAt(m_selectedListenerInterface));
				String hostAndIp = currentInterface.getHostAndIp();
				m_interfaceInfoHostLabel.setText(JAPMessages.getString("settingsInfoInterfaceHostLabel") +
												 " " + hostAndIp);
				m_interfaceInfoPortLabel.setText(JAPMessages.getString("settingsInfoInterfacePortLabel") +
												 " " + Integer.toString(currentInterface.getPort()));
				m_interfaceInfoInterfaceField.setText(JAPMessages.getString("settingsInfoInterfaceField") +
					" " + Integer.toString(m_selectedListenerInterface + 1));
				if (m_selectedListenerInterface > 0)
				{
					m_interfaceInfoUpButton.setEnabled(true);
				}
				else
				{
					m_interfaceInfoUpButton.setEnabled(false);
				}
				if (m_selectedListenerInterface < m_currentListenerInterfaces.size() - 1)
				{
					m_interfaceInfoDownButton.setEnabled(true);
				}
				else
				{
					m_interfaceInfoDownButton.setEnabled(false);
				}
			}
			else
			{
				/* no infoservice selected -> no data available */
				m_interfaceInfoHostLabel.setText(JAPMessages.getString("settingsInfoInterfaceHostLabel"));
				m_interfaceInfoPortLabel.setText(JAPMessages.getString("settingsInfoInterfacePortLabel"));
				m_interfaceInfoInterfaceField.setText(JAPMessages.getString("settingsInfoInterfaceField"));
				m_interfaceInfoUpButton.setEnabled(false);
				m_interfaceInfoDownButton.setEnabled(false);
			}
		}
	}

}
