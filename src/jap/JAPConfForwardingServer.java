/*
 Copyright (c) 2000 - 2005, The JAP-Team
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
import java.util.Observable;
import java.util.Observer;
import java.util.Vector;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.FocusAdapter;
import java.awt.event.FocusEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.TitledBorder;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.PlainDocument;

import anon.infoservice.InfoServiceDBEntry;
import anon.infoservice.InfoServiceHolder;
import anon.infoservice.MixCascade;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/**
 * This is the configuration GUI for the JAP forwarding server component.
 */
public class JAPConfForwardingServer extends AbstractJAPConfModule
{

	/**
	 * This is the internal message system of this module.
	 */
	private MessageSystem m_messageSystem;

	/**
	 * Constructor for JAPConfForwardingServer. We do some initialization here.
	 */
	public JAPConfForwardingServer()
	{
		super(new JAPConfForwardingServerSavePoint());
	}

	/**
	 * Creates the forwarding root panel with all child components.
	 */
	public void recreateRootPanel()
	{
		synchronized (this)
		{
			if (m_messageSystem == null)
			{
				/* create a new object for sending internal messages */
				m_messageSystem = new MessageSystem();
			}
		}

		JPanel rootPanel = getRootPanel();

		synchronized (this)
		{
			/* clear the whole root panel */
			rootPanel.removeAll();

			/* notify the observers of the message system that we recreate the root panel */
			m_messageSystem.sendMessage();
			/* recreate all parts of the forwarding server configuration dialog */
			JPanel serverPanel = createForwardingServerConfigPanel();

			GridBagLayout rootPanelLayout = new GridBagLayout();
			rootPanel.setLayout(rootPanelLayout);

			GridBagConstraints rootPanelConstraints = new GridBagConstraints();
			rootPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
			rootPanelConstraints.fill = GridBagConstraints.BOTH;
			rootPanelConstraints.weightx = 1.0;
			rootPanelConstraints.weighty = 1.0;

			rootPanelConstraints.gridx = 0;
			rootPanelConstraints.gridy = 0;
			rootPanelLayout.setConstraints(serverPanel, rootPanelConstraints);
			rootPanel.add(serverPanel);
		}
	}

	/**
	 * Returns the title for the forwarding server configuration within the configuration tree.
	 *
	 * @return The title for the forwarding server configuration leaf within the tree.
	 */
	public String getTabTitle()
	{
		return JAPMessages.getString("confTreeForwardingServerLeaf");
	}

	/**
	 * Creates the forwarding server config panel with all components.
	 *
	 * @return The forwarding server config panel.
	 */
	private JPanel createForwardingServerConfigPanel()
	{
		final JPanel configPanel = new JPanel();

		JLabel settingsForwardingServerConfigPortLabel = new JLabel(JAPMessages.getString(
			"settingsForwardingServerConfigPortLabel"));
		settingsForwardingServerConfigPortLabel.setFont(getFontSetting());

		final JTextField serverPortField = new JTextField()
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
							int port = Integer.parseInt(getText(0, getLength()) + a_stringToInsert);
							if ( (port >= 1) && (port <= 65535))
							{
								/* port is within the range (1 .. 65535) -> insert the String */
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
		serverPortField.addFocusListener(new FocusAdapter()
		{
			public void focusLost(FocusEvent a_focusEvent)
			{
				/* we lost the focus -> try to update the forwarding server port setting */
				try
				{
					int port = Integer.parseInt(serverPortField.getText());
					if (JAPModel.getInstance().getRoutingSettings().setServerPort(port) == false)
					{
						throw (new Exception("JAPConfForwardingServer: Error while changing server port."));
					}
				}
				catch (Exception e)
				{
					/* empty field or port is already in use */
					JOptionPane.showMessageDialog(configPanel,
												  JAPMessages.getString("settingsForwardingServerConfigChangeServerPortError"),
												  JAPMessages.getString("ERROR"), JOptionPane.ERROR_MESSAGE);
					/* show the current forwarding server port in the server port field */
					serverPortField.setText(Integer.toString(JAPModel.getInstance().getRoutingSettings().
						getServerPort()));
				}
			}
		});
		serverPortField.setColumns(6);
		serverPortField.setFont(getFontSetting());

		Observer serverPortObserver = new Observer()
		{
			/**
			 * This is the observer implementation. If the routing mode is changed in JAPRoutingSettings,
			 * we update the start routing server checkbox (enabled/disabled, selected/unselected). If the
			 * panel is recreated (message via the module internal message system), the observer removes
			 * itself from all observed objects.
			 *
			 * @param a_notifier The observed Object. This should always be JAPRoutingSettings or the module
			 *                   internal message system at the moment.
			 * @param a_message The reason of the notification. This should always be a JAPRoutingMessage
			 *                  at the moment or null.
			 */
			public void update(Observable a_notifier, Object a_message)
			{
				try
				{
					if (a_notifier == JAPModel.getInstance().getRoutingSettings())
					{
						/* message is from JAPRoutingSettings */
						if ( ( (JAPRoutingMessage) (a_message)).getMessageCode() ==
							JAPRoutingMessage.SERVER_PORT_CHANGED)
						{
							serverPortField.setText(Integer.toString(JAPModel.getInstance().
								getRoutingSettings().getServerPort()));
						}
					}
					if (a_notifier == m_messageSystem)
					{
						/* the root panel was recreated -> stop observing and remove ourself from the observed
						 * objects
						 */
						JAPModel.getInstance().getRoutingSettings().deleteObserver(this);
						m_messageSystem.deleteObserver(this);
					}
				}
				catch (Exception e)
				{
					/* should not happen */
					LogHolder.log(LogLevel.EXCEPTION, LogType.GUI, e);
				}
			}
		};
		/* registrate the observer also at the internal message system */
		m_messageSystem.addObserver(serverPortObserver);
		JAPModel.getInstance().getRoutingSettings().addObserver(serverPortObserver);
		/* tricky: initialize the components by calling the observer */
		serverPortObserver.update(JAPModel.getInstance().getRoutingSettings(),
								  new JAPRoutingMessage(JAPRoutingMessage.SERVER_PORT_CHANGED));

		JLabel settingsForwardingServerConfigMyConnectionLabel = new JLabel(JAPMessages.getString(
			"settingsForwardingServerConfigMyConnectionLabel"));
		settingsForwardingServerConfigMyConnectionLabel.setFont(getFontSetting());

		final JComboBox connectionClassesComboBox = new JComboBox(JAPModel.getInstance().getRoutingSettings().
			getConnectionClassSelector().getConnectionClasses());
		connectionClassesComboBox.setFont(getFontSetting());
		connectionClassesComboBox.setEditable(false);
		connectionClassesComboBox.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent event)
			{
				/* if the connection class is changed, we update the current connection class in the
				 * selector and the routing settings
				 */
				JAPModel.getInstance().getRoutingSettings().getConnectionClassSelector().
					setCurrentConnectionClass( ( (JAPRoutingConnectionClass) (connectionClassesComboBox.
					getSelectedItem())).getIdentifier());
			}
		});

		JLabel settingsForwardingServerConfigMaxUploadBandwidthLabel = new JLabel(JAPMessages.getString(
			"settingsForwardingServerConfigMaxUploadBandwidthLabel"));
		settingsForwardingServerConfigMaxUploadBandwidthLabel.setFont(getFontSetting());

		final JTextField uploadBandwidthField = new JTextField()
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
							int bandwidth = Integer.parseInt(getText(0, getLength()) + a_stringToInsert);
							if (bandwidth >= 1)
							{
								/* valid bandwidth (>=1) -> insert the String */
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
		uploadBandwidthField.addFocusListener(new FocusAdapter()
		{
			public void focusLost(FocusEvent a_focusEvent)
			{
				/* we lost the focus -> try to update the forwarding server port setting */
				try
				{
					int bandwidth = (Integer.parseInt(uploadBandwidthField.getText()) * 1000) / 8;
					if (bandwidth >= JAPConstants.ROUTING_BANDWIDTH_PER_USER)
					{
						JAPRoutingConnectionClass currentConnectionClass = JAPModel.getInstance().
							getRoutingSettings().getConnectionClassSelector().getCurrentConnectionClass();
						currentConnectionClass.setMaximumBandwidth(bandwidth);
						/* for updating the values within the forwarding system it is necessary to make an
						 * separate call of setCurrentConnectionClass()
						 */
						JAPModel.getInstance().getRoutingSettings().getConnectionClassSelector().
							setCurrentConnectionClass(currentConnectionClass.getIdentifier());
					}
					else
					{
						throw (new Exception(
							"JAPConfForwardingServer: Error while changing maximum upload bandwidth."));
					}
				}
				catch (Exception e)
				{
					/* empty field or bandwidth value to low */
					/* the +999 in the following line is only for rounding up the value */
					JOptionPane.showMessageDialog(configPanel,
						JAPMessages.getString("settingsForwardingServerConfigChangeMaximumUploadBandwidthErrorPart1") +
												  " " +
												  Integer.toString( ( (JAPConstants.ROUTING_BANDWIDTH_PER_USER *
						8) + 999) / 1000) + " " +
						JAPMessages.getString("settingsForwardingServerConfigChangeMaximumUploadBandwidthErrorPart2"),
						JAPMessages.getString("ERROR"), JOptionPane.ERROR_MESSAGE);
					/* show the current maximum upload bandwidth in the upload bandwidth field */
					uploadBandwidthField.setText(Integer.toString( (JAPModel.getInstance().getRoutingSettings().
						getConnectionClassSelector().getCurrentConnectionClass().getMaximumBandwidth() * 8) /
						1000));
				}
			}
		});
		uploadBandwidthField.setColumns(7);
		uploadBandwidthField.setFont(getFontSetting());

		Observer connectionClassSelectionObserver = new Observer()
		{
			/**
			 * This is the observer implementation. If the current connection class is changed, the
			 * connection classes combobox and the maximum bandwidth label are updated. If the panel is
			 * recreated (message via the module internal message system), the observer removes itself
			 * from all observed objects.
			 *
			 * @param a_notifier The observed Object. This should always be
			 *                   JAPRoutingConnectionClassSelector or the module internal message system
			 *                   at the moment.
			 * @param a_message The reason of the notification. This should always be a JAPRoutingMessage
			 *                  or null at the moment.
			 */
			public void update(Observable a_notifier, Object a_message)
			{
				try
				{
					if (a_notifier == JAPModel.getInstance().getRoutingSettings().getConnectionClassSelector())
					{
						/* message is from JAPRoutingConnectionClassSelector */
						if ( ( (JAPRoutingMessage) (a_message)).getMessageCode() ==
							JAPRoutingMessage.CONNECTION_CLASS_CHANGED)
						{
							/* change the selected connection class */
							JAPRoutingConnectionClass currentConnectionClass = JAPModel.getInstance().
								getRoutingSettings().getConnectionClassSelector().getCurrentConnectionClass();
							connectionClassesComboBox.setSelectedItem(currentConnectionClass);
							if (currentConnectionClass.getIdentifier() ==
								JAPRoutingConnectionClassSelector.CONNECTION_CLASS_USER)
							{
								/* user-defined class -> enable possibility for changing the upload bandwidth */
								uploadBandwidthField.setEnabled(true);
							}
							else
							{
								/* predefined class -> disable possibility for changing the upload bandwidth */
								uploadBandwidthField.setEnabled(false);
							}
							uploadBandwidthField.setText(Integer.toString( (currentConnectionClass.
								getMaximumBandwidth() * 8) / 1000));
						}
					}
					if (a_notifier == m_messageSystem)
					{
						/* the root panel was recreated -> stop observing and remove ourself from the observed
						 * objects
						 */
						JAPModel.getInstance().getRoutingSettings().getConnectionClassSelector().
							deleteObserver(this);
						m_messageSystem.deleteObserver(this);
					}
				}
				catch (Exception e)
				{
					/* should not happen */
					LogHolder.log(LogLevel.EXCEPTION, LogType.GUI, e);
				}
			}
		};
		/* registrate the observer also at the internal message system */
		m_messageSystem.addObserver(connectionClassSelectionObserver);
		JAPModel.getInstance().getRoutingSettings().getConnectionClassSelector().addObserver(
			connectionClassSelectionObserver);
		/* tricky: initialize the components by calling the observer */
		connectionClassSelectionObserver.update(JAPModel.getInstance().getRoutingSettings().
												getConnectionClassSelector(),
												new JAPRoutingMessage(JAPRoutingMessage.
			CONNECTION_CLASS_CHANGED));

		JLabel settingsForwardingServerConfigForwardingPercentageLabel = new JLabel(JAPMessages.getString(
			"settingsForwardingServerConfigForwardingPercentageLabel"));
		settingsForwardingServerConfigForwardingPercentageLabel.setFont(getFontSetting());

		final JTextField relativeBandwidthField = new JTextField();
		relativeBandwidthField.setColumns(4);
		relativeBandwidthField.setHorizontalAlignment(JTextField.RIGHT);
		relativeBandwidthField.setFont(getFontSetting());
		relativeBandwidthField.setDisabledTextColor(relativeBandwidthField.getForeground());
		relativeBandwidthField.setEnabled(false);

		final JButton increaseRelativeBandwidthButton = new JButton(JAPUtil.loadImageIcon("arrowUp.gif", true));
		increaseRelativeBandwidthButton.setMargin(new Insets(1, 1, 1, 1));
		increaseRelativeBandwidthButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent event)
			{
				/* increase the relative bandwidth of the current connection class by 10 */
				JAPRoutingConnectionClass currentConnectionClass = JAPModel.getInstance().getRoutingSettings().
					getConnectionClassSelector().getCurrentConnectionClass();
				/* we round always to the next multiple of 10 */
				int newRelativeBandwidth = Math.min(100,
					( ( (currentConnectionClass.getRelativeBandwidth() + 9) / 10) + 1) * 10);
				currentConnectionClass.setRelativeBandwidth(newRelativeBandwidth);
				/* for updating the values within the forwarding system it is necessary to make an
				 * separate call of setCurrentConnectionClass()
				 */
				JAPModel.getInstance().getRoutingSettings().getConnectionClassSelector().
					setCurrentConnectionClass(currentConnectionClass.getIdentifier());
			}
		});

		final JButton decreaseRelativeBandwidthButton = new JButton(JAPUtil.loadImageIcon("arrowDown.gif", true));
		decreaseRelativeBandwidthButton.setMargin(new Insets(1, 1, 1, 1));
		decreaseRelativeBandwidthButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent event)
			{
				/* increase the relative bandwidth of the current connection class by 10 */
				JAPRoutingConnectionClass currentConnectionClass = JAPModel.getInstance().getRoutingSettings().
					getConnectionClassSelector().getCurrentConnectionClass();
				/* we round always to the next multiple of 10 */
				int newRelativeBandwidth = ( (Math.max(currentConnectionClass.getMinimumRelativeBandwidth(),
					currentConnectionClass.getRelativeBandwidth() - 10) + 9) / 10) * 10;
				currentConnectionClass.setRelativeBandwidth(newRelativeBandwidth);
				/* for updating the values within the forwarding system it is necessary to make an
				 * separate call of setCurrentConnectionClass()
				 */
				JAPModel.getInstance().getRoutingSettings().getConnectionClassSelector().
					setCurrentConnectionClass(currentConnectionClass.getIdentifier());
			}
		});

		final JLabel settingsForwardingServerConfigCurrentBandwidthLabel = new JLabel();
		settingsForwardingServerConfigCurrentBandwidthLabel.setFont(getFontSetting());

		Observer connectionClassSettingsObserver = new Observer()
		{
			/**
			 * This is the observer implementation. If the connection class or some sttings of the
			 * current connection class are changed, the components for selecting the relative bandwidth
			 * are updated. If the panel is recreated (message via the module internal message system),
			 * the observer removes itself from all observed objects.
			 *
			 * @param a_notifier The observed Object. This should always be
			 *                   JAPRoutingConnectionClassSelector or the module internal message system
			 *                   at the moment.
			 * @param a_message The reason of the notification. This should always be a JAPRoutingMessage
			 *                  or null at the moment.
			 */
			public void update(Observable a_notifier, Object a_message)
			{
				try
				{
					if (a_notifier == JAPModel.getInstance().getRoutingSettings().getConnectionClassSelector())
					{
						/* message is from JAPRoutingConnectionClassSelector */
						if ( ( ( (JAPRoutingMessage) (a_message)).getMessageCode() ==
							  JAPRoutingMessage.CONNECTION_CLASS_CHANGED) ||
							( ( (JAPRoutingMessage) (a_message)).getMessageCode() ==
							 JAPRoutingMessage.CONNECTION_PARAMETERS_CHANGED))
						{
							/* change the relative bandwidth settings */
							JAPRoutingConnectionClass currentConnectionClass = JAPModel.getInstance().
								getRoutingSettings().getConnectionClassSelector().getCurrentConnectionClass();
							relativeBandwidthField.setText(Integer.toString(currentConnectionClass.
								getRelativeBandwidth()) + "%");
							if (currentConnectionClass.getRelativeBandwidth() < 100)
							{
								/* increasing the bandwidth is possible */
								increaseRelativeBandwidthButton.setEnabled(true);
							}
							else
							{
								/* increasing the bandwidth is not possible */
								increaseRelativeBandwidthButton.setEnabled(false);
							}
							if (currentConnectionClass.getRelativeBandwidth() >
								( (currentConnectionClass.getMinimumRelativeBandwidth() + 9) / 10) * 10)
							{
								/* decreasing the bandwidth is possible (at least by one full step of 10) */
								decreaseRelativeBandwidthButton.setEnabled(true);
							}
							else
							{
								/* increasing the bandwidth is not possible (or we cannot decrease it by at least
								 * one full step of 10)
								 */
								decreaseRelativeBandwidthButton.setEnabled(false);
							}
						}
						if ( ( (JAPRoutingMessage) (a_message)).getMessageCode() ==
							JAPRoutingMessage.CONNECTION_PARAMETERS_CHANGED)
						{
							/* update the label with the bandwidth and connection number information */
							settingsForwardingServerConfigCurrentBandwidthLabel.setText(JAPMessages.getString(
								"settingsForwardingServerConfigCurrentBandwidthLabelPart1") + " " +
								Integer.
								toString( (JAPModel.getInstance().getRoutingSettings().getBandwidth() * 8) /
										 1000) + " " +
								JAPMessages.getString("settingsForwardingServerConfigCurrentBandwidthLabelPart2") +
								" " +
								Integer.toString(JAPModel.getInstance().getRoutingSettings().getAllowedConnections()) +
								" " +
								JAPMessages.getString("settingsForwardingServerConfigCurrentBandwidthLabelPart3"));
						}
					}
					if (a_notifier == m_messageSystem)
					{
						/* the root panel was recreated -> stop observing and remove ourself from the observed
						 * objects
						 */
						JAPModel.getInstance().getRoutingSettings().getConnectionClassSelector().
							deleteObserver(this);
						m_messageSystem.deleteObserver(this);
					}
				}
				catch (Exception e)
				{
					/* should not happen */
					LogHolder.log(LogLevel.EXCEPTION, LogType.GUI, e);
				}
			}
		};
		/* registrate the observer also at the internal message system */
		m_messageSystem.addObserver(connectionClassSettingsObserver);
		JAPModel.getInstance().getRoutingSettings().getConnectionClassSelector().addObserver(
			connectionClassSettingsObserver);
		/* tricky: initialize the components by calling the observer */
		connectionClassSettingsObserver.update(JAPModel.getInstance().getRoutingSettings().
											   getConnectionClassSelector(),
											   new JAPRoutingMessage(JAPRoutingMessage.
			CONNECTION_PARAMETERS_CHANGED));

		JTabbedPane advancedConfigurationTabPane = new JTabbedPane();
		advancedConfigurationTabPane.setFont(getFontSetting());
		advancedConfigurationTabPane.insertTab(JAPMessages.getString(
			"settingsForwardingServerConfigAllowedCascadesTabTitle"), null,
											   createForwardingServerConfigAllowedCascadesPanel(), null, 0);
		advancedConfigurationTabPane.insertTab(JAPMessages.getString(
			"settingsForwardingServerConfigRegistrationInfoServicesTabTitle"), null,
											   createForwardingServerConfigRegistrationInfoServicesPanel(), null,
											   1);

		JPanel serverPortPanel = new JPanel();
		GridBagLayout serverPortPanelLayout = new GridBagLayout();
		serverPortPanel.setLayout(serverPortPanelLayout);

		GridBagConstraints serverPortPanelConstraints = new GridBagConstraints();
		serverPortPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
		serverPortPanelConstraints.fill = GridBagConstraints.NONE;
		serverPortPanelConstraints.weightx = 0.0;
		serverPortPanelConstraints.weighty = 1.0;
		serverPortPanelConstraints.gridx = 0;
		serverPortPanelConstraints.gridy = 0;
		serverPortPanelConstraints.insets = new Insets(5, 5, 20, 5);
		serverPortPanelLayout.setConstraints(settingsForwardingServerConfigPortLabel,
											 serverPortPanelConstraints);
		serverPortPanel.add(settingsForwardingServerConfigPortLabel);

		serverPortPanelConstraints.gridx = 1;
		serverPortPanelConstraints.gridy = 0;
		serverPortPanelConstraints.weightx = 1.0;
		serverPortPanelConstraints.insets = new Insets(5, 0, 20, 5);
		serverPortPanelLayout.setConstraints(serverPortField, serverPortPanelConstraints);
		serverPortPanel.add(serverPortField);

		TitledBorder settingsForwardingServerConfigBorder = new TitledBorder(JAPMessages.getString(
			"settingsForwardingServerConfigBorder"));
		settingsForwardingServerConfigBorder.setTitleFont(getFontSetting());
		configPanel.setBorder(settingsForwardingServerConfigBorder);

		GridBagLayout configPanelLayout = new GridBagLayout();
		configPanel.setLayout(configPanelLayout);

		GridBagConstraints configPanelConstraints = new GridBagConstraints();
		configPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
		configPanelConstraints.fill = GridBagConstraints.BOTH;
		configPanelConstraints.weightx = 1.0;
		configPanelConstraints.weighty = 0.0;
		configPanelConstraints.gridx = 0;
		configPanelConstraints.gridy = 0;
		configPanelConstraints.gridwidth = 4;
		configPanelLayout.setConstraints(serverPortPanel, configPanelConstraints);
		configPanel.add(serverPortPanel);

		configPanelConstraints.fill = GridBagConstraints.NONE;
		configPanelConstraints.gridx = 0;
		configPanelConstraints.gridy = 1;
		configPanelConstraints.gridwidth = 1;
		configPanelConstraints.weightx = 0.0;
		configPanelConstraints.insets = new Insets(0, 5, 0, 10);
		configPanelLayout.setConstraints(settingsForwardingServerConfigMyConnectionLabel,
										 configPanelConstraints);
		configPanel.add(settingsForwardingServerConfigMyConnectionLabel);

		configPanelConstraints.gridx = 1;
		configPanelConstraints.gridy = 1;
		configPanelConstraints.insets = new Insets(0, 10, 0, 10);
		configPanelLayout.setConstraints(settingsForwardingServerConfigMaxUploadBandwidthLabel,
										 configPanelConstraints);
		configPanel.add(settingsForwardingServerConfigMaxUploadBandwidthLabel);

		configPanelConstraints.gridx = 2;
		configPanelConstraints.gridy = 1;
		configPanelConstraints.gridwidth = 2;
		configPanelConstraints.weightx = 1.0;
		configPanelConstraints.insets = new Insets(0, 10, 0, 5);
		configPanelLayout.setConstraints(settingsForwardingServerConfigForwardingPercentageLabel,
										 configPanelConstraints);
		configPanel.add(settingsForwardingServerConfigForwardingPercentageLabel);

		configPanelConstraints.gridx = 0;
		configPanelConstraints.gridy = 2;
		configPanelConstraints.weightx = 0.0;
		configPanelConstraints.gridwidth = 1;
		configPanelConstraints.gridheight = 2;
		configPanelConstraints.fill = GridBagConstraints.VERTICAL;
		configPanelConstraints.insets = new Insets(0, 5, 10, 10);
		configPanelLayout.setConstraints(connectionClassesComboBox, configPanelConstraints);
		configPanel.add(connectionClassesComboBox);

		configPanelConstraints.gridx = 1;
		configPanelConstraints.gridy = 2;
		configPanelConstraints.insets = new Insets(0, 10, 10, 10);
		configPanelLayout.setConstraints(uploadBandwidthField, configPanelConstraints);
		configPanel.add(uploadBandwidthField);

		configPanelConstraints.gridx = 2;
		configPanelConstraints.gridy = 2;
		configPanelConstraints.fill = GridBagConstraints.BOTH;
		configPanelConstraints.anchor = GridBagConstraints.WEST;
		configPanelConstraints.insets = new Insets(0, 10, 10, 0);
		configPanelLayout.setConstraints(relativeBandwidthField, configPanelConstraints);
		configPanel.add(relativeBandwidthField);

		configPanelConstraints.gridx = 3;
		configPanelConstraints.gridy = 2;
		configPanelConstraints.gridheight = 1;
		configPanelConstraints.weightx = 1.0;
		configPanelConstraints.fill = GridBagConstraints.NONE;
		configPanelConstraints.anchor = GridBagConstraints.SOUTHWEST;
		configPanelConstraints.insets = new Insets(0, 0, 0, 5);
		configPanelLayout.setConstraints(increaseRelativeBandwidthButton, configPanelConstraints);
		configPanel.add(increaseRelativeBandwidthButton);

		configPanelConstraints.gridx = 3;
		configPanelConstraints.gridy = 3;
		configPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
		configPanelConstraints.insets = new Insets(0, 0, 10, 5);
		configPanelLayout.setConstraints(decreaseRelativeBandwidthButton, configPanelConstraints);
		configPanel.add(decreaseRelativeBandwidthButton);

		configPanelConstraints.gridx = 0;
		configPanelConstraints.gridy = 4;
		configPanelConstraints.weightx = 1.0;
		configPanelConstraints.gridwidth = 4;
		configPanelConstraints.insets = new Insets(0, 5, 20, 5);
		configPanelLayout.setConstraints(settingsForwardingServerConfigCurrentBandwidthLabel,
										 configPanelConstraints);
		configPanel.add(settingsForwardingServerConfigCurrentBandwidthLabel);

		configPanelConstraints.gridx = 0;
		configPanelConstraints.gridy = 5;
		configPanelConstraints.weighty = 1.0;
		configPanelConstraints.fill = GridBagConstraints.BOTH;
		configPanelConstraints.insets = new Insets(0, 5, 5, 5);
		configPanelLayout.setConstraints(advancedConfigurationTabPane, configPanelConstraints);
		configPanel.add(advancedConfigurationTabPane);

		return configPanel;
	}

	/**
	 * Creates the panel where the supported mixcascades for forwarding can be configured.
	 *
	 * @return The allowed mixcascades configuration panel.
	 */
	private JPanel createForwardingServerConfigAllowedCascadesPanel()
	{
		final JPanel allowedCascadesPanel = new JPanel();

		final JLabel settingsForwardingServerConfigAllowedCascadesKnownCascadesLabel = new JLabel(JAPMessages.
			getString("settingsForwardingServerConfigAllowedCascadesKnownCascadesLabel"));
		settingsForwardingServerConfigAllowedCascadesKnownCascadesLabel.setFont(getFontSetting());
		final JLabel settingsForwardingServerConfigAllowedCascadesAllowedCascadesLabel = new JLabel(
			JAPMessages.getString("settingsForwardingServerConfigAllowedCascadesAllowedCascadesLabel"));
		settingsForwardingServerConfigAllowedCascadesAllowedCascadesLabel.setFont(getFontSetting());

		final DefaultListModel knownCascadesListModel = new DefaultListModel();
		final JList knownCascadesList = new JList(knownCascadesListModel);
		knownCascadesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		final JScrollPane knownCascadesScrollPane = new JScrollPane(knownCascadesList);
		knownCascadesScrollPane.setFont(getFontSetting());
		/* set the preferred size of the scrollpane to a 4x20 textarea */
		knownCascadesScrollPane.setPreferredSize( (new JTextArea(4, 20)).getPreferredSize());

		final DefaultListModel allowedCascadesListModel = new DefaultListModel();
		final JList allowedCascadesList = new JList(allowedCascadesListModel);
		allowedCascadesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		final JScrollPane allowedCascadesScrollPane = new JScrollPane(allowedCascadesList);
		allowedCascadesScrollPane.setFont(getFontSetting());
		/* set the preferred size of the scrollpane to a 4x20 textarea */
		allowedCascadesScrollPane.setPreferredSize( (new JTextArea(4, 20)).getPreferredSize());

		final JButton settingsForwardingServerConfigAllowedCascadesReloadButton = new JButton(JAPMessages.
			getString("settingsForwardingServerConfigAllowedCascadesReloadButton"));
		settingsForwardingServerConfigAllowedCascadesReloadButton.setFont(getFontSetting());
		settingsForwardingServerConfigAllowedCascadesReloadButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent event)
			{
				synchronized (knownCascadesListModel)
				{
					knownCascadesListModel.clear();
					Enumeration fetchedCascades = showFetchMixCascadesDialog(allowedCascadesPanel).elements();
					while (fetchedCascades.hasMoreElements())
					{
						knownCascadesListModel.addElement(fetchedCascades.nextElement());
					}
				}
			}
		});

		final JButton settingsForwardingServerConfigAllowedCascadesAddButton = new JButton(JAPMessages.
			getString("settingsForwardingServerConfigAllowedCascadesAddButton"));
		settingsForwardingServerConfigAllowedCascadesAddButton.setFont(getFontSetting());
		settingsForwardingServerConfigAllowedCascadesAddButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent event)
			{
				/* if the Add button is pressed, add the selected mixcascade to the list of allowed
				 * mixcascades for the clients of the local forwarding server, if it is not already there
				 */
				MixCascade selectedCascade = (MixCascade) (knownCascadesList.getSelectedValue());
				if (selectedCascade != null)
				{
					JAPModel.getInstance().getRoutingSettings().getUseableMixCascadesStore().
						addToAllowedMixCascades(selectedCascade);
				}
			}
		});

		final JButton settingsForwardingServerConfigAllowedCascadesRemoveButton = new JButton(JAPMessages.
			getString("settingsForwardingServerConfigAllowedCascadesRemoveButton"));
		settingsForwardingServerConfigAllowedCascadesRemoveButton.setFont(getFontSetting());
		settingsForwardingServerConfigAllowedCascadesRemoveButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent event)
			{
				/* if the Remove button is pressed, remove the selected mixcascade from the list of
				 * allowed mixcascades
				 */
				MixCascade selectedCascade = (MixCascade) (allowedCascadesList.getSelectedValue());
				if (selectedCascade != null)
				{
					JAPModel.getInstance().getRoutingSettings().getUseableMixCascadesStore().
						removeFromAllowedMixCascades(selectedCascade.getId());
				}
			}
		});

		final JCheckBox settingsForwardingServerConfigAllowedCascadesAllowAllBox = new JCheckBox(JAPMessages.
			getString("settingsForwardingServerConfigAllowedCascadesAllowAllBox"),
			JAPModel.getInstance().getRoutingSettings().getUseableMixCascadesStore().
						  getAllowAllAvailableMixCascades());
		settingsForwardingServerConfigAllowedCascadesAllowAllBox.setFont(getFontSetting());
		settingsForwardingServerConfigAllowedCascadesAllowAllBox.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent event)
			{
				JAPModel.getInstance().getRoutingSettings().getUseableMixCascadesStore().
					setAllowAllAvailableMixCascades(settingsForwardingServerConfigAllowedCascadesAllowAllBox.
					isSelected());
			}
		});

		Observer allowedMixCascadesObserver = new Observer()
		{
			/**
			 * This is the observer implementation. If the allowed mixcascades policy or the list of
			 * allowed mixcascades for the restricted mode is changed, we update the checkbox, the lists
			 * and the enable-status of the most components on the mixcascades panel, if necessary. If the
			 * panel is recreated (message via the module internal message system), the observer removes
			 * itself from all observed objects.
			 *
			 * @param a_notifier The observed Object. This should always be
			 *                   JAPRoutingUseableMixCascades or the module internal message system at
			 *                   the moment.
			 * @param a_message The reason of the notification. This should always be a JAPRoutingMessage
			 *                  or null at the moment.
			 */
			public void update(Observable a_notifier, Object a_message)
			{
				try
				{
					if (a_notifier == JAPModel.getInstance().getRoutingSettings().getUseableMixCascadesStore())
					{
						/* message is from JAPRoutingUseableMixCascades */
						int messageCode = ( (JAPRoutingMessage) (a_message)).getMessageCode();
						if (messageCode == JAPRoutingMessage.ALLOWED_MIXCASCADES_POLICY_CHANGED)
						{
							/* enable or disable the components on the panel as needed for currently selected
							 * mode
							 */
							if (JAPModel.getInstance().getRoutingSettings().getUseableMixCascadesStore().
								getAllowAllAvailableMixCascades())
							{
								/* access-to-all available mixcascades -> components for managing the list of
								 * allowed mixcascades not needed
								 */
								settingsForwardingServerConfigAllowedCascadesKnownCascadesLabel.setEnabled(false);
								settingsForwardingServerConfigAllowedCascadesAllowedCascadesLabel.setEnabled(false);
								knownCascadesList.setEnabled(false);
								allowedCascadesList.setEnabled(false);
								/* remove all entries from the both listboxes -> no irritation */
								knownCascadesList.setModel(new DefaultListModel());
								allowedCascadesList.setModel(new DefaultListModel());
								settingsForwardingServerConfigAllowedCascadesReloadButton.setEnabled(false);
								settingsForwardingServerConfigAllowedCascadesAddButton.setEnabled(false);
								settingsForwardingServerConfigAllowedCascadesRemoveButton.setEnabled(false);
								/* select the access-to-all checkbox */
								settingsForwardingServerConfigAllowedCascadesAllowAllBox.setSelected(true);
							}
							else
							{
								/* available mixcascades for forwarding are restricted to a list -> components for
								 * managing that list needed
								 */
								settingsForwardingServerConfigAllowedCascadesKnownCascadesLabel.setEnabled(true);
								settingsForwardingServerConfigAllowedCascadesAllowedCascadesLabel.setEnabled(true);
								/* restore the original listmodels */
								knownCascadesList.setModel(knownCascadesListModel);
								allowedCascadesList.setModel(allowedCascadesListModel);
								knownCascadesList.setEnabled(true);
								allowedCascadesList.setEnabled(true);
								settingsForwardingServerConfigAllowedCascadesReloadButton.setEnabled(true);
								settingsForwardingServerConfigAllowedCascadesAddButton.setEnabled(true);
								settingsForwardingServerConfigAllowedCascadesRemoveButton.setEnabled(true);
								/* deselect the access-to-all checkbox */
								settingsForwardingServerConfigAllowedCascadesAllowAllBox.setSelected(false);
							}
						}
						if (messageCode == JAPRoutingMessage.ALLOWED_MIXCASCADES_LIST_CHANGED)
						{
							synchronized (allowedCascadesListModel)
							{
								allowedCascadesListModel.clear();
								Enumeration allowedCascades = JAPModel.getInstance().getRoutingSettings().
									getUseableMixCascadesStore().getAllowedMixCascades().elements();
								while (allowedCascades.hasMoreElements())
								{
									allowedCascadesListModel.addElement(allowedCascades.nextElement());
								}
							}
						}
					}
					if (a_notifier == m_messageSystem)
					{
						/* the root panel was recreated -> stop observing and remove ourself from the observed
						 * objects
						 */
						JAPModel.getInstance().getRoutingSettings().getUseableMixCascadesStore().
							deleteObserver(this);
						m_messageSystem.deleteObserver(this);
					}
				}
				catch (Exception e)
				{
					/* should not happen */
					LogHolder.log(LogLevel.EXCEPTION, LogType.GUI, e);
				}
			}
		};
		/* registrate the observer also at the internal message system */
		m_messageSystem.addObserver(allowedMixCascadesObserver);
		JAPModel.getInstance().getRoutingSettings().getUseableMixCascadesStore().addObserver(
			allowedMixCascadesObserver);
		/* tricky: initialize the components by calling the observer (with all possible messages) */
		allowedMixCascadesObserver.update(JAPModel.getInstance().getRoutingSettings().
										  getUseableMixCascadesStore(),
										  new JAPRoutingMessage(JAPRoutingMessage.
			ALLOWED_MIXCASCADES_LIST_CHANGED));
		allowedMixCascadesObserver.update(JAPModel.getInstance().getRoutingSettings().
										  getUseableMixCascadesStore(),
										  new JAPRoutingMessage(JAPRoutingMessage.
			ALLOWED_MIXCASCADES_POLICY_CHANGED));

		TitledBorder settingsForwardingServerConfigAllowedCascadesBorder = new TitledBorder(JAPMessages.
			getString("settingsForwardingServerConfigAllowedCascadesBorder"));
		settingsForwardingServerConfigAllowedCascadesBorder.setTitleFont(getFontSetting());
		allowedCascadesPanel.setBorder(settingsForwardingServerConfigAllowedCascadesBorder);

		GridBagLayout allowedCascadesPanelLayout = new GridBagLayout();
		allowedCascadesPanel.setLayout(allowedCascadesPanelLayout);

		GridBagConstraints allowedCascadesPanelConstraints = new GridBagConstraints();
		allowedCascadesPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
		allowedCascadesPanelConstraints.fill = GridBagConstraints.NONE;
		allowedCascadesPanelConstraints.weightx = 1.0;
		allowedCascadesPanelConstraints.weighty = 0.0;

		allowedCascadesPanelConstraints.gridx = 0;
		allowedCascadesPanelConstraints.gridy = 0;
		allowedCascadesPanelConstraints.gridwidth = 2;
		allowedCascadesPanelConstraints.insets = new Insets(0, 5, 10, 5);
		allowedCascadesPanelLayout.setConstraints(settingsForwardingServerConfigAllowedCascadesAllowAllBox,
												  allowedCascadesPanelConstraints);
		allowedCascadesPanel.add(settingsForwardingServerConfigAllowedCascadesAllowAllBox);

		allowedCascadesPanelConstraints.gridx = 0;
		allowedCascadesPanelConstraints.gridy = 1;
		allowedCascadesPanelConstraints.gridwidth = 1;
		allowedCascadesPanelConstraints.insets = new Insets(0, 5, 0, 5);
		allowedCascadesPanelLayout.setConstraints(
			settingsForwardingServerConfigAllowedCascadesKnownCascadesLabel, allowedCascadesPanelConstraints);
		allowedCascadesPanel.add(settingsForwardingServerConfigAllowedCascadesKnownCascadesLabel);

		allowedCascadesPanelConstraints.gridx = 1;
		allowedCascadesPanelConstraints.gridy = 1;
		allowedCascadesPanelConstraints.insets = new Insets(0, 5, 0, 5);
		allowedCascadesPanelLayout.setConstraints(
			settingsForwardingServerConfigAllowedCascadesAllowedCascadesLabel,
			allowedCascadesPanelConstraints);
		allowedCascadesPanel.add(settingsForwardingServerConfigAllowedCascadesAllowedCascadesLabel);

		allowedCascadesPanelConstraints.gridx = 0;
		allowedCascadesPanelConstraints.gridy = 2;
		allowedCascadesPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
		allowedCascadesPanelConstraints.insets = new Insets(0, 5, 0, 5);
		allowedCascadesPanelLayout.setConstraints(settingsForwardingServerConfigAllowedCascadesReloadButton,
												  allowedCascadesPanelConstraints);
		allowedCascadesPanel.add(settingsForwardingServerConfigAllowedCascadesReloadButton);

		allowedCascadesPanelConstraints.gridx = 0;
		allowedCascadesPanelConstraints.gridy = 3;
		allowedCascadesPanelConstraints.weighty = 1.0;
		allowedCascadesPanelConstraints.fill = GridBagConstraints.BOTH;
		allowedCascadesPanelConstraints.insets = new Insets(0, 5, 0, 5);
		allowedCascadesPanelLayout.setConstraints(knownCascadesScrollPane, allowedCascadesPanelConstraints);
		allowedCascadesPanel.add(knownCascadesScrollPane);

		allowedCascadesPanelConstraints.gridx = 1;
		allowedCascadesPanelConstraints.gridy = 2;
		allowedCascadesPanelConstraints.gridheight = 2;
		allowedCascadesPanelConstraints.insets = new Insets(0, 5, 0, 5);
		allowedCascadesPanelLayout.setConstraints(allowedCascadesScrollPane, allowedCascadesPanelConstraints);
		allowedCascadesPanel.add(allowedCascadesScrollPane);

		allowedCascadesPanelConstraints.gridx = 0;
		allowedCascadesPanelConstraints.gridy = 4;
		allowedCascadesPanelConstraints.weighty = 0.0;
		allowedCascadesPanelConstraints.gridheight = 1;
		allowedCascadesPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
		allowedCascadesPanelConstraints.insets = new Insets(5, 5, 5, 5);
		allowedCascadesPanelLayout.setConstraints(settingsForwardingServerConfigAllowedCascadesAddButton,
												  allowedCascadesPanelConstraints);
		allowedCascadesPanel.add(settingsForwardingServerConfigAllowedCascadesAddButton);

		allowedCascadesPanelConstraints.gridx = 1;
		allowedCascadesPanelConstraints.gridy = 4;
		allowedCascadesPanelConstraints.insets = new Insets(5, 5, 5, 5);
		allowedCascadesPanelLayout.setConstraints(settingsForwardingServerConfigAllowedCascadesRemoveButton,
												  allowedCascadesPanelConstraints);
		allowedCascadesPanel.add(settingsForwardingServerConfigAllowedCascadesRemoveButton);

		return allowedCascadesPanel;
	}

	/**
	 * Creates the infoservice registration configuration panel with all child components.
	 *
	 * @return The infoservice registration configuration panel.
	 */
	private JPanel createForwardingServerConfigRegistrationInfoServicesPanel()
	{
		JPanel registrationInfoServicesPanel = new JPanel();

		final JLabel settingsForwardingServerConfigRegistrationInfoServicesKnownInfoServicesLabel = new
			JLabel(JAPMessages.getString(
			"settingsForwardingServerConfigRegistrationInfoServicesKnownInfoServicesLabel"));
		settingsForwardingServerConfigRegistrationInfoServicesKnownInfoServicesLabel.setFont(getFontSetting());
		final JLabel settingsForwardingServerConfigRegistrationInfoServicesSelectedInfoServicesLabel = new
			JLabel(JAPMessages.getString(
			"settingsForwardingServerConfigRegistrationInfoServicesSelectedInfoServicesLabel"));
		settingsForwardingServerConfigRegistrationInfoServicesSelectedInfoServicesLabel.setFont(
			getFontSetting());

		final DefaultListModel knownInfoServicesListModel = new DefaultListModel();
		final JList knownInfoServicesList = new JList(knownInfoServicesListModel);
		knownInfoServicesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		final JScrollPane knownInfoServicesScrollPane = new JScrollPane(knownInfoServicesList);
		knownInfoServicesScrollPane.setFont(getFontSetting());
		/* set the preferred size of the scrollpane to a 4x20 textarea */
		knownInfoServicesScrollPane.setPreferredSize( (new JTextArea(4, 20)).getPreferredSize());

		final DefaultListModel registrationInfoServicesListModel = new DefaultListModel();
		final JList registrationInfoServicesList = new JList(registrationInfoServicesListModel);
		registrationInfoServicesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		final JScrollPane registrationInfoServicesScrollPane = new JScrollPane(registrationInfoServicesList);
		registrationInfoServicesScrollPane.setFont(getFontSetting());
		/* set the preferred size of the scrollpane to a 4x20 textarea */
		registrationInfoServicesScrollPane.setPreferredSize( (new JTextArea(4, 20)).getPreferredSize());

		final JButton settingsForwardingServerConfigRegistrationInfoServicesReloadButton = new JButton(
			JAPMessages.getString("settingsForwardingServerConfigRegistrationInfoServicesReloadButton"));
		settingsForwardingServerConfigRegistrationInfoServicesReloadButton.setFont(getFontSetting());
		settingsForwardingServerConfigRegistrationInfoServicesReloadButton.addActionListener(new
			ActionListener()
		{
			public void actionPerformed(ActionEvent event)
			{
				synchronized (knownInfoServicesListModel)
				{
					knownInfoServicesListModel.clear();
					Enumeration knownInfoServices = InfoServiceHolder.getInstance().
						getInfoservicesWithForwarderList().elements();
					while (knownInfoServices.hasMoreElements())
					{
						knownInfoServicesListModel.addElement(knownInfoServices.nextElement());
					}
				}
			}
		});

		final JButton settingsForwardingServerConfigRegistrationInfoServicesAddButton = new JButton(
			JAPMessages.getString("settingsForwardingServerConfigRegistrationInfoServicesAddButton"));
		settingsForwardingServerConfigRegistrationInfoServicesAddButton.setFont(getFontSetting());
		settingsForwardingServerConfigRegistrationInfoServicesAddButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent event)
			{
				/* if the Add button is pressed, add the selected known infoservice to the list of
				 * registration infoservices
				 */
				InfoServiceDBEntry selectedInfoService = (InfoServiceDBEntry) (knownInfoServicesList.
					getSelectedValue());
				if (selectedInfoService != null)
				{
					JAPModel.getInstance().getRoutingSettings().getRegistrationInfoServicesStore().
						addToRegistrationInfoServices(selectedInfoService);
				}
			}
		});

		final JButton settingsForwardingServerConfigRegistrationInfoServicesRemoveButton = new JButton(
			JAPMessages.getString("settingsForwardingServerConfigRegistrationInfoServicesRemoveButton"));
		settingsForwardingServerConfigRegistrationInfoServicesRemoveButton.setFont(getFontSetting());
		settingsForwardingServerConfigRegistrationInfoServicesRemoveButton.addActionListener(new
			ActionListener()
		{
			public void actionPerformed(ActionEvent event)
			{
				/* if the Remove button is pressed, remove the selected infoservice from the list of
				 * registration infoservices
				 */
				InfoServiceDBEntry selectedInfoService = (InfoServiceDBEntry) (registrationInfoServicesList.
					getSelectedValue());
				if (selectedInfoService != null)
				{
					JAPModel.getInstance().getRoutingSettings().getRegistrationInfoServicesStore().
						removeFromRegistrationInfoServices(selectedInfoService.getId());
				}
			}
		});

		final JCheckBox settingsForwardingServerConfigRegistrationInfoServicesRegisterAtAllBox = new
			JCheckBox(JAPMessages.getString(
			"settingsForwardingServerConfigRegistrationInfoServicesRegisterAtAllBox"),
					  JAPModel.getInstance().getRoutingSettings().getRegistrationInfoServicesStore().
					  getRegisterAtAllAvailableInfoServices());
		settingsForwardingServerConfigRegistrationInfoServicesRegisterAtAllBox.setFont(getFontSetting());
		settingsForwardingServerConfigRegistrationInfoServicesRegisterAtAllBox.addActionListener(new
			ActionListener()
		{
			public void actionPerformed(ActionEvent event)
			{
				JAPModel.getInstance().getRoutingSettings().getRegistrationInfoServicesStore().
					setRegisterAtAllAvailableInfoServices(
					settingsForwardingServerConfigRegistrationInfoServicesRegisterAtAllBox.isSelected());
			}
		});

		Observer registrationInfoServicesObserver = new Observer()
		{
			/**
			 * This is the observer implementation. If the registration infoservices policy or the list
			 * of registration infoservices for the manual registration mode is changed, we update the
			 * checkbox, the lists and the enable-status of the most components on the infoservices
			 * panel, if necessary. If the panel is recreated (message via the module internal message
			 * system), the observer removes itself from all observed objects.
			 *
			 * @param a_notifier The observed Object. This should always be
			 *                   JAPRoutingRegistrationInfoServices or the module internal message
			 *                   system at the moment.
			 * @param a_message The reason of the notification. This should always be a JAPRoutingMessage
			 *                  or null at the moment.
			 */
			public void update(Observable a_notifier, Object a_message)
			{
				try
				{
					if (a_notifier ==
						JAPModel.getInstance().getRoutingSettings().getRegistrationInfoServicesStore())
					{
						/* message is from JAPRoutingUseableMixCascades */
						int messageCode = ( (JAPRoutingMessage) (a_message)).getMessageCode();
						if (messageCode == JAPRoutingMessage.REGISTRATION_INFOSERVICES_POLICY_CHANGED)
						{
							/* enable or disable the components on the panel as needed for currently selected
							 * mode
							 */
							if (JAPModel.getInstance().getRoutingSettings().getRegistrationInfoServicesStore().
								getRegisterAtAllAvailableInfoServices())
							{
								/* register-at-all available primary infoservices -> components for managing the
								 * list of registration infoservices not needed
								 */
								settingsForwardingServerConfigRegistrationInfoServicesKnownInfoServicesLabel.
									setEnabled(false);
								settingsForwardingServerConfigRegistrationInfoServicesSelectedInfoServicesLabel.
									setEnabled(false);
								knownInfoServicesList.setEnabled(false);
								registrationInfoServicesList.setEnabled(false);
								/* remove all entries from the both listboxes -> no irritation */
								knownInfoServicesList.setModel(new DefaultListModel());
								registrationInfoServicesList.setModel(new DefaultListModel());
								settingsForwardingServerConfigRegistrationInfoServicesReloadButton.setEnabled(false);
								settingsForwardingServerConfigRegistrationInfoServicesAddButton.setEnabled(false);
								settingsForwardingServerConfigRegistrationInfoServicesRemoveButton.setEnabled(false);
								/* select the register-at-all checkbox */
								settingsForwardingServerConfigRegistrationInfoServicesRegisterAtAllBox.
									setSelected(true);
							}
							else
							{
								/* register only at the infoservices from the registration list -> components for
								 * managing that list needed
								 */
								settingsForwardingServerConfigRegistrationInfoServicesKnownInfoServicesLabel.
									setEnabled(true);
								settingsForwardingServerConfigRegistrationInfoServicesSelectedInfoServicesLabel.
									setEnabled(true);
								/* restore the original listmodels */
								knownInfoServicesList.setModel(knownInfoServicesListModel);
								registrationInfoServicesList.setModel(registrationInfoServicesListModel);
								knownInfoServicesList.setEnabled(true);
								registrationInfoServicesList.setEnabled(true);
								settingsForwardingServerConfigRegistrationInfoServicesReloadButton.setEnabled(true);
								settingsForwardingServerConfigRegistrationInfoServicesAddButton.setEnabled(true);
								settingsForwardingServerConfigRegistrationInfoServicesRemoveButton.setEnabled(true);
								/* deselect the register-at-all checkbox */
								settingsForwardingServerConfigRegistrationInfoServicesRegisterAtAllBox.
									setSelected(false);
							}
						}
						if (messageCode == JAPRoutingMessage.REGISTRATION_INFOSERVICES_LIST_CHANGED)
						{
							synchronized (registrationInfoServicesListModel)
							{
								registrationInfoServicesListModel.clear();
								Enumeration registrationInfoServices = JAPModel.getInstance().
									getRoutingSettings().getRegistrationInfoServicesStore().
									getRegistrationInfoServices().elements();
								while (registrationInfoServices.hasMoreElements())
								{
									registrationInfoServicesListModel.addElement(registrationInfoServices.
										nextElement());
								}
							}
						}
					}
					if (a_notifier == m_messageSystem)
					{
						/* the root panel was recreated -> stop observing and remove ourself from the observed
						 * objects
						 */
						JAPModel.getInstance().getRoutingSettings().getRegistrationInfoServicesStore().
							deleteObserver(this);
						m_messageSystem.deleteObserver(this);
					}
				}
				catch (Exception e)
				{
					/* should not happen */
					LogHolder.log(LogLevel.EXCEPTION, LogType.GUI, e);
				}
			}
		};
		/* registrate the observer also at the internal message system */
		m_messageSystem.addObserver(registrationInfoServicesObserver);
		JAPModel.getInstance().getRoutingSettings().getRegistrationInfoServicesStore().addObserver(
			registrationInfoServicesObserver);
		/* tricky: initialize the components by calling the observer (with all possible messages) */
		registrationInfoServicesObserver.update(JAPModel.getInstance().getRoutingSettings().
												getRegistrationInfoServicesStore(),
												new JAPRoutingMessage(JAPRoutingMessage.
			REGISTRATION_INFOSERVICES_LIST_CHANGED));
		registrationInfoServicesObserver.update(JAPModel.getInstance().getRoutingSettings().
												getRegistrationInfoServicesStore(),
												new JAPRoutingMessage(JAPRoutingMessage.
			REGISTRATION_INFOSERVICES_POLICY_CHANGED));

		TitledBorder settingsForwardingServerConfigRegistrationInfoServicesBorder = new TitledBorder(
			JAPMessages.getString("settingsForwardingServerConfigRegistrationInfoServicesBorder"));
		settingsForwardingServerConfigRegistrationInfoServicesBorder.setTitleFont(getFontSetting());
		registrationInfoServicesPanel.setBorder(settingsForwardingServerConfigRegistrationInfoServicesBorder);

		GridBagLayout registrationInfoServicesPanelLayout = new GridBagLayout();
		registrationInfoServicesPanel.setLayout(registrationInfoServicesPanelLayout);

		GridBagConstraints registrationInfoServicesPanelConstraints = new GridBagConstraints();
		registrationInfoServicesPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
		registrationInfoServicesPanelConstraints.fill = GridBagConstraints.NONE;
		registrationInfoServicesPanelConstraints.weightx = 1.0;
		registrationInfoServicesPanelConstraints.weighty = 0.0;

		registrationInfoServicesPanelConstraints.gridx = 0;
		registrationInfoServicesPanelConstraints.gridy = 0;
		registrationInfoServicesPanelConstraints.gridwidth = 2;
		registrationInfoServicesPanelConstraints.insets = new Insets(0, 5, 10, 5);
		registrationInfoServicesPanelLayout.setConstraints(
			settingsForwardingServerConfigRegistrationInfoServicesRegisterAtAllBox,
			registrationInfoServicesPanelConstraints);
		registrationInfoServicesPanel.add(
			settingsForwardingServerConfigRegistrationInfoServicesRegisterAtAllBox);

		registrationInfoServicesPanelConstraints.gridx = 0;
		registrationInfoServicesPanelConstraints.gridy = 1;
		registrationInfoServicesPanelConstraints.gridwidth = 1;
		registrationInfoServicesPanelConstraints.insets = new Insets(0, 5, 0, 5);
		registrationInfoServicesPanelLayout.setConstraints(
			settingsForwardingServerConfigRegistrationInfoServicesKnownInfoServicesLabel,
			registrationInfoServicesPanelConstraints);
		registrationInfoServicesPanel.add(
			settingsForwardingServerConfigRegistrationInfoServicesKnownInfoServicesLabel);

		registrationInfoServicesPanelConstraints.gridx = 1;
		registrationInfoServicesPanelConstraints.gridy = 1;
		registrationInfoServicesPanelConstraints.insets = new Insets(0, 5, 0, 5);
		registrationInfoServicesPanelLayout.setConstraints(
			settingsForwardingServerConfigRegistrationInfoServicesSelectedInfoServicesLabel,
			registrationInfoServicesPanelConstraints);
		registrationInfoServicesPanel.add(
			settingsForwardingServerConfigRegistrationInfoServicesSelectedInfoServicesLabel);

		registrationInfoServicesPanelConstraints.gridx = 0;
		registrationInfoServicesPanelConstraints.gridy = 2;
		registrationInfoServicesPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
		registrationInfoServicesPanelConstraints.insets = new Insets(0, 5, 0, 5);
		registrationInfoServicesPanelLayout.setConstraints(
			settingsForwardingServerConfigRegistrationInfoServicesReloadButton,
			registrationInfoServicesPanelConstraints);
		registrationInfoServicesPanel.add(settingsForwardingServerConfigRegistrationInfoServicesReloadButton);

		registrationInfoServicesPanelConstraints.gridx = 0;
		registrationInfoServicesPanelConstraints.gridy = 3;
		registrationInfoServicesPanelConstraints.weighty = 1.0;
		registrationInfoServicesPanelConstraints.fill = GridBagConstraints.BOTH;
		registrationInfoServicesPanelConstraints.insets = new Insets(0, 5, 0, 5);
		registrationInfoServicesPanelLayout.setConstraints(knownInfoServicesScrollPane,
			registrationInfoServicesPanelConstraints);
		registrationInfoServicesPanel.add(knownInfoServicesScrollPane);

		registrationInfoServicesPanelConstraints.gridx = 1;
		registrationInfoServicesPanelConstraints.gridy = 2;
		registrationInfoServicesPanelConstraints.gridheight = 2;
		registrationInfoServicesPanelConstraints.insets = new Insets(0, 5, 0, 5);
		registrationInfoServicesPanelLayout.setConstraints(registrationInfoServicesScrollPane,
			registrationInfoServicesPanelConstraints);
		registrationInfoServicesPanel.add(registrationInfoServicesScrollPane);

		registrationInfoServicesPanelConstraints.gridx = 0;
		registrationInfoServicesPanelConstraints.gridy = 4;
		registrationInfoServicesPanelConstraints.weighty = 0.0;
		registrationInfoServicesPanelConstraints.gridheight = 1;
		registrationInfoServicesPanelConstraints.insets = new Insets(5, 5, 5, 5);
		registrationInfoServicesPanelLayout.setConstraints(
			settingsForwardingServerConfigRegistrationInfoServicesAddButton,
			registrationInfoServicesPanelConstraints);
		registrationInfoServicesPanel.add(settingsForwardingServerConfigRegistrationInfoServicesAddButton);

		registrationInfoServicesPanelConstraints.gridx = 1;
		registrationInfoServicesPanelConstraints.gridy = 4;
		registrationInfoServicesPanelConstraints.insets = new Insets(5, 5, 5, 5);
		registrationInfoServicesPanelLayout.setConstraints(
			settingsForwardingServerConfigRegistrationInfoServicesRemoveButton,
			registrationInfoServicesPanelConstraints);
		registrationInfoServicesPanel.add(settingsForwardingServerConfigRegistrationInfoServicesRemoveButton);

		return registrationInfoServicesPanel;
	}

	/**
	 * Shows the fetch mixcascades dialog box when configuring the allowed mixcascades for the
	 * forwarding server. A Vector with the fetched mixcascades is returned. If there was an error
	 * while fetching the cascades, an error message is displayed and an empty Vector is returned.
	 *
	 * @param a_parentComponent The parent component where this dialog is centered over.
	 *
	 * @return A Vector with the fetched mixcacades (maybe empty).
	 */
	private Vector showFetchMixCascadesDialog(JComponent a_parentComponent)
	{
		final JAPDialog fetchMixCascadesDialog = new JAPDialog(a_parentComponent,
			JAPMessages.getString("settingsForwardingServerConfigAllowedCascadesFetchMixCascadesDialogTitle"));
		fetchMixCascadesDialog.disableManualClosing();
		JPanel fetchMixCascadesPanel = fetchMixCascadesDialog.getRootPanel();

		JLabel settingsRoutingServerFetchMixCascadesDialogFetchLabel = new JLabel(JAPMessages.getString(
			"settingsForwardingServerConfigAllowedCascadesFetchMixCascadesDialogFetchLabel"));
		settingsRoutingServerFetchMixCascadesDialogFetchLabel.setFont(getFontSetting());
		JLabel busyLabel = new JLabel(JAPUtil.loadImageIcon(JAPConstants.BUSYFN, true));

		final Vector fetchedCascades = new Vector();
		final Vector errorOccured = new Vector();

		final Thread fetchMixCascadesThread = new Thread(new Runnable()
		{
			public void run()
			{
				Vector knownMixCascades = InfoServiceHolder.getInstance().getMixCascades();
				fetchMixCascadesDialog.dispose();
				/* clear the interrupted flag, if it is set */
				Thread.interrupted();
				if (knownMixCascades == null)
				{
					errorOccured.addElement(new NullPointerException());
					knownMixCascades = new Vector();
				}
				/* copy the fetched cascades in the result Vector */
				Enumeration cascades = knownMixCascades.elements();
				while (cascades.hasMoreElements())
				{
					fetchedCascades.addElement(cascades.nextElement());
				}
			}
		});

		JButton settingsRoutingFetchMixCascadesDialogCancelButton = new JButton(JAPMessages.getString(
			"cancelButton"));
		settingsRoutingFetchMixCascadesDialogCancelButton.setFont(getFontSetting());
		settingsRoutingFetchMixCascadesDialogCancelButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent event)
			{
				/* if the Cancel button is pressed, interrupt fetching of the mixcascades -> it is stopped
				 * immediately
				 */
				fetchMixCascadesThread.interrupt();
			}
		});

		GridBagLayout fetchMixCascadesPanelLayout = new GridBagLayout();
		fetchMixCascadesPanel.setLayout(fetchMixCascadesPanelLayout);

		GridBagConstraints fetchMixCascadesPanelConstraints = new GridBagConstraints();
		fetchMixCascadesPanelConstraints.anchor = GridBagConstraints.NORTH;
		fetchMixCascadesPanelConstraints.fill = GridBagConstraints.NONE;
		fetchMixCascadesPanelConstraints.weighty = 0.0;

		fetchMixCascadesPanelConstraints.gridx = 0;
		fetchMixCascadesPanelConstraints.gridy = 0;
		fetchMixCascadesPanelConstraints.insets = new Insets(5, 5, 0, 5);
		fetchMixCascadesPanelLayout.setConstraints(settingsRoutingServerFetchMixCascadesDialogFetchLabel,
			fetchMixCascadesPanelConstraints);
		fetchMixCascadesPanel.add(settingsRoutingServerFetchMixCascadesDialogFetchLabel);

		fetchMixCascadesPanelConstraints.gridx = 0;
		fetchMixCascadesPanelConstraints.gridy = 1;
		fetchMixCascadesPanelConstraints.insets = new Insets(10, 5, 10, 5);
		fetchMixCascadesPanelLayout.setConstraints(busyLabel, fetchMixCascadesPanelConstraints);
		fetchMixCascadesPanel.add(busyLabel);

		fetchMixCascadesPanelConstraints.gridx = 0;
		fetchMixCascadesPanelConstraints.gridy = 2;
		fetchMixCascadesPanelConstraints.insets = new Insets(0, 5, 5, 5);
		fetchMixCascadesPanelConstraints.weighty = 1.0;
		fetchMixCascadesPanelLayout.setConstraints(settingsRoutingFetchMixCascadesDialogCancelButton,
			fetchMixCascadesPanelConstraints);
		fetchMixCascadesPanel.add(settingsRoutingFetchMixCascadesDialogCancelButton);

		fetchMixCascadesDialog.align();

		/* for synchronization purposes, it is necessary to show the dialog first and start the thread
		 * after that event
		 */
		fetchMixCascadesDialog.getInternalDialog().addWindowListener(new WindowAdapter()
		{
			public void windowOpened(WindowEvent a_event)
			{
				fetchMixCascadesDialog.getInternalDialog().removeWindowListener(this);
				fetchMixCascadesThread.start();
			}
		});

		fetchMixCascadesDialog.show();

		/* wait until the fetch-thread is ready */
		try
		{
			fetchMixCascadesThread.join();
		}
		catch (InterruptedException e)
		{
		}

		if (errorOccured.size() > 0)
		{
			JOptionPane.showMessageDialog(a_parentComponent,
				JAPMessages.getString("settingsForwardingServerConfigAllowedCascadesFetchMixCascadesDialogFetchCascadesError"),
										  JAPMessages.getString("ERROR"), JOptionPane.ERROR_MESSAGE);
		}

		return fetchedCascades;
	}

}
