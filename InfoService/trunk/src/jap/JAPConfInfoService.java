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
import javax.swing.JTextArea;
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

import anon.infoservice.Database;
import anon.infoservice.HTTPConnectionFactory;
import anon.infoservice.InfoServiceDBEntry;
import anon.infoservice.InfoServiceHolder;
import anon.infoservice.ListenerInterface;

/**
 * This is the configuration GUI for the infoservice.
 */
final class JAPConfInfoService extends AbstractJAPConfModule
{

	/**
	 * Stores the current prefered InfoService.
	 */
	private InfoServiceDBEntry m_preferedInfoService;

	/**
	 * Stores the listmodel with the data of all known InfoServices.
	 */
	private JAPInfoServiceListModel m_infoServiceListModel;

	/**
	 * Constructor for JAPConfInfoService. We do some initializing here.
	 */
	public JAPConfInfoService()
	{
		super(new JAPConfInfoServiceSavePoint());
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
		infoServiceTabPane.insertTab(JAPMessages.getString("settingsInfoSettingsTabTitle"), null,
									 createInfoServiceConfigPanel(), null, 0);
		infoServiceTabPane.insertTab(JAPMessages.getString("settingsInfoAdvancedTabTitle"), null,
									 createInfoServiceAdvancedPanel(), null, 1);
		infoServiceTabPane.insertTab(JAPMessages.getString("settingsInfoInformationTabTitle"), null,
									 createInfoServiceInformationPanel(), null, 2);

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

		final JList settingsInfoAllList = new JList(getInfoServiceListModel());
		settingsInfoAllList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		settingsInfoAllList.setCellRenderer(new ListCellRenderer()
		{
			public Component getListCellRendererComponent(JList a_list, Object a_value, int a_index,
				boolean a_isSelected, boolean a_cellHasFocus)
			{
				JLabel returnLabel = null;
				if ( ( (InfoServiceDBEntry) a_value).isUserDefined())
				{
					returnLabel = new JLabel( ( (InfoServiceDBEntry) a_value).getName(),
											 JAPUtil.loadImageIcon("servermanuell.gif", true), JLabel.LEFT);
				}
				else
				{
					returnLabel = new JLabel( ( (InfoServiceDBEntry) a_value).getName(),
											 JAPUtil.loadImageIcon("serverfrominternet.gif", true),
											 JLabel.LEFT);
				}
				returnLabel.setOpaque(true);
				/* the defualt is a non-bold font */
				returnLabel.setFont(new Font(getFontSetting().getName(),
											 getFontSetting().getStyle() & (~Font.BOLD),
											 getFontSetting().getSize()));
				if (m_preferedInfoService != null)
				{
					if (m_preferedInfoService.getId().equals( ( (InfoServiceDBEntry) a_value).getId()))
					{
						/* print the prefered InfoService in a bold font */
						returnLabel.setFont(new Font(getFontSetting().getName(),
							getFontSetting().getStyle() | Font.BOLD, getFontSetting().getSize()));
					}
				}
				if (a_isSelected == true)
				{
					returnLabel.setForeground(a_list.getSelectionForeground());
					returnLabel.setBackground(a_list.getSelectionBackground());
				}
				else
				{
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
						InfoServiceDBEntry currentInfoService = (InfoServiceDBEntry) (it.nextElement());
						Database.getInstance(InfoServiceDBEntry.class).update(currentInfoService);
						InfoServiceDBEntry preferedInfoService = InfoServiceHolder.getInstance().
							getPreferedInfoService();
						if (preferedInfoService != null)
						{
							/* if the current infoservice ID is equal to the ID of the prefered infoservice,
							 * update the prefered infoservice also
							 */
							if (preferedInfoService.getId().equals(currentInfoService.getId()))
							{
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
				InfoServiceDBEntry selectedInfoService = (InfoServiceDBEntry) (settingsInfoAllList.
					getSelectedValue());
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
				InfoServiceDBEntry selectedInfoService = (InfoServiceDBEntry) (settingsInfoAllList.
					getSelectedValue());
				InfoServiceDBEntry preferedInfoService = InfoServiceHolder.getInstance().
					getPreferedInfoService();
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
					if (infoServiceName.equals(""))
					{
						/* use gernerated default name */
						infoServiceName = null;
					}
					InfoServiceDBEntry newInfoService = new InfoServiceDBEntry(infoServiceName,
						new
						ListenerInterface(settingsInfoHostField.getText().trim(),
										  Integer.parseInt(settingsInfoPortField.getText().trim())).toVector(), true);
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
	 * @return The infoservice advanced configuration panel.
	 */
	private JPanel createInfoServiceAdvancedPanel()
	{
		JPanel expertPanel = new JPanel();

		final JCheckBox disableInfoServiceBox = new JCheckBox(JAPMessages.getString(
			"settingsInfoDisableInfoService"), JAPModel.isInfoServiceDisabled());
		disableInfoServiceBox.setFont(getFontSetting());
		disableInfoServiceBox.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent event)
			{
				/* enable/disable the automatic infoservice requests */
				JAPController.setInfoServiceDisabled(disableInfoServiceBox.isSelected());
			}
		});

		final JCheckBox disableChangeInfoServiceBox = new JCheckBox(JAPMessages.getString(
			"settingsInfoDisableChangeInfoService"), !InfoServiceHolder.getInstance().isChangeInfoServices());
		disableChangeInfoServiceBox.setFont(getFontSetting());
		disableChangeInfoServiceBox.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent event)
			{
				/* enable/disable the automatic changes of the infoservices */
				InfoServiceHolder.getInstance().setChangeInfoServices(!disableChangeInfoServiceBox.isSelected());
			}
		});

		final JTextField infoServiceTimeoutField = new JTextField()
		{
			protected Document createDefaultModel()
			{
				return (new PlainDocument()
				{
					public void insertString(int a_position, String a_stringToInsert,
											 AttributeSet a_attributes) throws BadLocationException
					{
						try
						{
							int timeout = Integer.parseInt(getText(0, getLength()) + a_stringToInsert);
							if ( (timeout >= 1) && (timeout <= 60))
							{
								/* timeout is within the range (1 .. 60 seconds) -> insert the String */
								super.insertString(a_position, a_stringToInsert, a_attributes);
							}
						}
						catch (NumberFormatException e)
						{
							/* do nothing (because of invalid chars) */
						}
					}
				});
			}
		};
		infoServiceTimeoutField.addFocusListener(new FocusAdapter()
		{
			public void focusLost(FocusEvent a_focusEvent)
			{
				/* we lost the focus -> try to update the infoservice timeout setting */
				try
				{
					int timeout = Integer.parseInt(infoServiceTimeoutField.getText());
					HTTPConnectionFactory.getInstance().setTimeout(timeout);
				}
				catch (NumberFormatException e)
				{
					/* do nothing (empty field) */
				}
				/* show the current timeout in the tiemout field */
				infoServiceTimeoutField.setText(Integer.toString(HTTPConnectionFactory.getInstance().
					getTimeout()));
			}
		});
		infoServiceTimeoutField.setText(Integer.toString(HTTPConnectionFactory.getInstance().getTimeout()));
		infoServiceTimeoutField.setColumns(3);
		infoServiceTimeoutField.setFont(getFontSetting());

		JLabel settingsInfoTimeoutLabel = new JLabel(JAPMessages.getString("settingsInfoTimeoutLabel"));
		settingsInfoTimeoutLabel.setFont(getFontSetting());

		TitledBorder settingsInfoExpertBorder = new TitledBorder(JAPMessages.getString(
			"settingsInfoExpertBorder"));
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
	 * Creates the infoservice information panel with all components.
	 *
	 * @return The infoservice information panel.
	 */
	private JPanel createInfoServiceInformationPanel()
	{
		JPanel informationPanel = new JPanel();

		JLabel settingsInfoListLabel = new JLabel(JAPMessages.getString("settingsInfoListLabel"));
		settingsInfoListLabel.setFont(getFontSetting());

		final JTextArea interfaceInfoArea = new JTextArea(5, 0);
		interfaceInfoArea.setFont(getFontSetting());
		interfaceInfoArea.setDisabledTextColor(interfaceInfoArea.getForeground());
		interfaceInfoArea.setEnabled(false);
		interfaceInfoArea.setLineWrap(false);
		interfaceInfoArea.setOpaque(false);
		JScrollPane interfaceInfoScrollPane = new JScrollPane(interfaceInfoArea);
		interfaceInfoScrollPane.setFont(getFontSetting());

		final JList infoServiceList = new JList(getInfoServiceListModel());
		infoServiceList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		infoServiceList.setCellRenderer(new ListCellRenderer()
		{
			public Component getListCellRendererComponent(JList a_list, Object a_value, int a_index,
				boolean a_isSelected, boolean a_cellHasFocus)
			{
				JLabel returnLabel = null;
				if ( ( (InfoServiceDBEntry) a_value).isUserDefined())
				{
					returnLabel = new JLabel( ( (InfoServiceDBEntry) a_value).getName(),
											 JAPUtil.loadImageIcon("servermanuell.gif", true), JLabel.LEFT);
				}
				else
				{
					returnLabel = new JLabel( ( (InfoServiceDBEntry) a_value).getName(),
											 JAPUtil.loadImageIcon("serverfrominternet.gif", true),
											 JLabel.LEFT);
				}
				returnLabel.setOpaque(true);
				/* the default is a non-bold font */
				returnLabel.setFont(new Font(getFontSetting().getName(),
											 getFontSetting().getStyle() & (~Font.BOLD),
											 getFontSetting().getSize()));
				if (m_preferedInfoService != null)
				{
					if (m_preferedInfoService.getId().equals( ( (InfoServiceDBEntry) a_value).getId()))
					{
						/* print the prefered InfoService in a bold font */
						returnLabel.setFont(new Font(getFontSetting().getName(),
							getFontSetting().getStyle() | Font.BOLD, getFontSetting().getSize()));
					}
				}
				if (a_isSelected == true)
				{
					returnLabel.setForeground(a_list.getSelectionForeground());
					returnLabel.setBackground(a_list.getSelectionBackground());
				}
				else
				{
					returnLabel.setForeground(a_list.getForeground());
					returnLabel.setBackground(a_list.getBackground());
				}
				return returnLabel;
			}
		});
		infoServiceList.addListSelectionListener(new ListSelectionListener()
		{
			public void valueChanged(ListSelectionEvent event)
			{
				synchronized (interfaceInfoArea)
				{
					/* we need exclusive access to the interface information area */
					interfaceInfoArea.setText("");
					InfoServiceDBEntry selectedInfoService = (InfoServiceDBEntry) (infoServiceList.
						getSelectedValue());
					if (selectedInfoService != null)
					{
						Vector infoserviceListenerInterfaces = selectedInfoService.getListenerInterfaces();
						Enumeration listenerInterfaces = infoserviceListenerInterfaces.elements();
						while (listenerInterfaces.hasMoreElements())
						{
							ListenerInterface currentListenerInterface = (ListenerInterface) (
								listenerInterfaces.nextElement());
							if (interfaceInfoArea.getText().equals("") == false)
							{
								/* add some breaklines because it is not the first line */
								interfaceInfoArea.setText(interfaceInfoArea.getText() + "\n\n");
							}
							interfaceInfoArea.setText(interfaceInfoArea.getText() + " " +
								JAPMessages.getString("settingsInfoInterfaceListHost") + " " +
								currentListenerInterface.getHost() + "    " +
								JAPMessages.getString("settingsInfoInterfaceListPort") + " " +
								Integer.toString(currentListenerInterface.getPort()) + " ");
						}
					}
				}
			}
		});
		JScrollPane infoServiceScrollPane = new JScrollPane(infoServiceList);
		infoServiceScrollPane.setFont(getFontSetting());

		JLabel settingsInfoInterfaceListLabel = new JLabel(JAPMessages.getString(
			"settingsInfoInterfaceListLabel"));
		settingsInfoInterfaceListLabel.setFont(getFontSetting());

		TitledBorder settingsInfoInformationBorder = new TitledBorder(JAPMessages.getString(
			"settingsInfoInformationBorder"));
		settingsInfoInformationBorder.setTitleFont(getFontSetting());
		informationPanel.setBorder(settingsInfoInformationBorder);

		GridBagLayout informationPanelLayout = new GridBagLayout();
		informationPanel.setLayout(informationPanelLayout);

		GridBagConstraints informationPanelConstraints = new GridBagConstraints();
		informationPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
		informationPanelConstraints.fill = GridBagConstraints.NONE;
		informationPanelConstraints.weightx = 1.0;

		informationPanelConstraints.gridx = 0;
		informationPanelConstraints.gridy = 0;
		informationPanelConstraints.weighty = 0.0;
		informationPanelConstraints.insets = new Insets(5, 5, 0, 5);
		informationPanelLayout.setConstraints(settingsInfoListLabel, informationPanelConstraints);
		informationPanel.add(settingsInfoListLabel);

		informationPanelConstraints.gridx = 0;
		informationPanelConstraints.gridy = 1;
		informationPanelConstraints.fill = GridBagConstraints.BOTH;
		informationPanelConstraints.weighty = 1.0;
		informationPanelConstraints.insets = new Insets(0, 5, 5, 5);
		informationPanelLayout.setConstraints(infoServiceScrollPane, informationPanelConstraints);
		informationPanel.add(infoServiceScrollPane);

		informationPanelConstraints.gridx = 0;
		informationPanelConstraints.gridy = 2;
		informationPanelConstraints.fill = GridBagConstraints.NONE;
		informationPanelConstraints.weighty = 0.0;
		informationPanelConstraints.insets = new Insets(10, 5, 0, 5);
		informationPanelLayout.setConstraints(settingsInfoInterfaceListLabel, informationPanelConstraints);
		informationPanel.add(settingsInfoInterfaceListLabel);

		informationPanelConstraints.gridx = 0;
		informationPanelConstraints.gridy = 3;
		informationPanelConstraints.fill = GridBagConstraints.BOTH;
		informationPanelConstraints.insets = new Insets(0, 5, 5, 5);
		informationPanelLayout.setConstraints(interfaceInfoScrollPane, informationPanelConstraints);
		informationPanel.add(interfaceInfoScrollPane);

		return informationPanel;
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
					InfoServiceDBEntry tempInfoService = (InfoServiceDBEntry) (knownInfoServiceEnumeration.
						nextElement());
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
			getInfoServiceListModel().setData(knownInfoServices);
		}
	}

	/**
	 * Returns the list model with the list of all known InfoServices. It is used for all
	 * InfoServices lists in this InfoService configuration module. If there is currently no such
	 * list model, a new one is created.
	 *
	 * @return The list model with the data of all known InfoServices.
	 */
	private JAPInfoServiceListModel getInfoServiceListModel()
	{
		synchronized (this)
		{
			if (m_infoServiceListModel == null)
			{
				m_infoServiceListModel = new JAPInfoServiceListModel();
			}
		}
		return m_infoServiceListModel;
	}

}
