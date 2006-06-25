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
package jap.forward;

import java.util.Observable;
import java.util.Observer;

import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.WindowConstants;
import javax.swing.border.TitledBorder;

import gui.JAPHelp;
import gui.JAPHtmlMultiLineLabel;
import gui.JAPMessages;
import gui.dialog.JAPDialog;
import jap.AbstractJAPConfModule;
import jap.JAPModel;
import jap.MessageSystem;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/**
 * This is the configuration GUI for the JAP forwarding client component.
 */
public class JAPConfForwardingClient extends AbstractJAPConfModule
{

	/**
	 * This is the internal message system of this module.
	 */
	private MessageSystem m_messageSystem;

	/**
	 * Constructor for JAPConfForwardingClient. We do some initialization here.
	 */
	public JAPConfForwardingClient()
	{
		super(new JAPConfForwardingClientSavePoint());
	}

	/**
	 * Creates the forwarding client root panel with all child components.
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
			/* recreate all parts of the forwarding client configuration dialog */
			JPanel clientPanel = createForwardingClientConfigPanel();

			GridBagLayout rootPanelLayout = new GridBagLayout();
			rootPanel.setLayout(rootPanelLayout);

			GridBagConstraints rootPanelConstraints = new GridBagConstraints();
			rootPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
			rootPanelConstraints.fill = GridBagConstraints.BOTH;

			rootPanelConstraints.gridx = 0;
			rootPanelConstraints.gridy = 0;
			rootPanelConstraints.weightx = 1.0;
			rootPanelConstraints.weighty = 1.0;
			rootPanelLayout.setConstraints(clientPanel, rootPanelConstraints);
			rootPanel.add(clientPanel);
		}
	}

	/**
	 * Returns the title for the forwarding client configuration within the configuration tree.
	 *
	 * @return The title for the forwarding client configuration leaf within the tree.
	 */
	public String getTabTitle()
	{
		return JAPMessages.getString("confTreeForwardingClientLeaf");
	}

	/**
	 * Creates the forwarding client configuration panel.
	 *
	 * @return The forwarding client config panel.
	 */
	private JPanel createForwardingClientConfigPanel()
	{
		final JPanel clientPanel = new JPanel();

		final JCheckBox settingsForwardingClientConfigNeedForwarderBox =
			new JCheckBox(JAPMessages.getString("settingsForwardingClientConfigNeedForwarderBox"));
		settingsForwardingClientConfigNeedForwarderBox.setFont(getFontSetting());
		settingsForwardingClientConfigNeedForwarderBox.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent event)
			{
				if (settingsForwardingClientConfigNeedForwarderBox.isSelected())
				{
					/* we shall enabled the connect-via-forwarder feature */
					if (JAPModel.getInstance().getRoutingSettings().getRoutingMode() ==
						JAPRoutingSettings.ROUTING_MODE_SERVER)
					{
						/* we have to shutdown the server first -> user has to confirm this */
						showForwardingClientConfirmServerShutdownDialog(clientPanel);
						/* maybe the user has canceled the shutdown -> update the selection state of the
						 * checkbox
						 */
						settingsForwardingClientConfigNeedForwarderBox.setSelected(JAPModel.getInstance().
							getRoutingSettings().isConnectViaForwarder());
					}
					else
					{
						/* we can directly enable the connect-via-forwarder setting, because the forwarding
						 * server isn't running
						 */
						JAPModel.getInstance().getRoutingSettings().setConnectViaForwarder(true);
					}
				}
				else
				{
					/* disable the connect-via-forwarder setting in JAPRoutingSettings */
					JAPModel.getInstance().getRoutingSettings().setConnectViaForwarder(false);
				}
			}
		});

		final JCheckBox settingsForwardingClientConfigForwardInfoServiceBox = new JCheckBox(
			  JAPMessages.getString("settingsForwardingClientConfigForwardInfoServiceBox"));
		settingsForwardingClientConfigForwardInfoServiceBox.setFont(getFontSetting());
		settingsForwardingClientConfigForwardInfoServiceBox.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent event)
			{
				/* enable or disable the forward InfoService setting in JAPRoutingSettings */
				JAPModel.getInstance().getRoutingSettings().setForwardInfoService(
					settingsForwardingClientConfigForwardInfoServiceBox.isSelected());
			}
		});

		TitledBorder settingsForwardingClientConfigBorder = new TitledBorder(
			  JAPMessages.getString("settingsForwardingClientConfigBorder"));
		settingsForwardingClientConfigBorder.setTitleFont(getFontSetting());
		clientPanel.setBorder(settingsForwardingClientConfigBorder);

		GridBagLayout clientPanelLayout = new GridBagLayout();
		clientPanel.setLayout(clientPanelLayout);

		GridBagConstraints clientPanelConstraints = new GridBagConstraints();
		clientPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
		clientPanelConstraints.fill = GridBagConstraints.NONE;
		clientPanelConstraints.weightx = 1.0;
		/* we need at least a minimal value to prevent horinzontal centering if no other component
		 * is visible
		 */
		clientPanelConstraints.weighty = 0.0001;
		clientPanelConstraints.gridx = 0;
		clientPanelConstraints.gridy = 0;
		clientPanelConstraints.insets = new Insets(5, 5, 5, 5);
		clientPanelLayout.setConstraints(settingsForwardingClientConfigNeedForwarderBox,
										 clientPanelConstraints);
		clientPanel.add(settingsForwardingClientConfigNeedForwarderBox);

		clientPanelConstraints.gridx = 0;
		clientPanelConstraints.gridy = 1;
		clientPanelConstraints.insets = new Insets(0, 20, 5, 5);
		clientPanelLayout.setConstraints(settingsForwardingClientConfigForwardInfoServiceBox,
										 clientPanelConstraints);
		clientPanel.add(settingsForwardingClientConfigForwardInfoServiceBox);

		Observer clientSettingsObserver = new Observer()
		{
			/**
			 * This is the observer implementation. If the client settings were changed, we update the
			 * checkboxes.
			 *
			 * @param a_notifier The observed Object. This should always be JAPRoutingSettings or the
			 *                   module internal message system at the moment.
			 * @param a_message The reason of the notification. This should always be a JAPRoutingMessage
			 *                  or null at the moment.
			 */
			public void update(Observable a_notifier, Object a_message)
			{
				try
				{
					if (a_notifier == JAPModel.getInstance().getRoutingSettings())
					{
						if ( ( (JAPRoutingMessage) (a_message)).getMessageCode() ==
							JAPRoutingMessage.CLIENT_SETTINGS_CHANGED)
						{
							/* the client settings were changed -> update the state of the checkboxes, maybe
							 * also make them invisible, if they are not needed
							 */
							if (JAPModel.getInstance().getRoutingSettings().isConnectViaForwarder())
							{
								settingsForwardingClientConfigNeedForwarderBox.setSelected(true);
								settingsForwardingClientConfigForwardInfoServiceBox.setVisible(true);
							}
							else
							{
								settingsForwardingClientConfigNeedForwarderBox.setSelected(false);
								settingsForwardingClientConfigForwardInfoServiceBox.setVisible(false);
							}
							settingsForwardingClientConfigForwardInfoServiceBox.setSelected(JAPModel.
								getInstance().getRoutingSettings().getForwardInfoService());
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
		m_messageSystem.addObserver(clientSettingsObserver);
		JAPModel.getInstance().getRoutingSettings().addObserver(clientSettingsObserver);
		/* tricky: initialize the checkboxes by calling the observer */
		clientSettingsObserver.update(JAPModel.getInstance().getRoutingSettings(),
									  new JAPRoutingMessage(JAPRoutingMessage.CLIENT_SETTINGS_CHANGED));

		clientPanelConstraints.gridy++;
		clientPanelConstraints.weightx = 1.0;
		clientPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
		clientPanel.add(new JSeparator(), clientPanelConstraints);
		clientPanelConstraints.gridy++;
		clientPanelConstraints.weighty = 1.0;
		JAPHtmlMultiLineLabel descLabel =
			new JAPHtmlMultiLineLabel(JAPMessages.getString("forwardingClientDesc"),
									  getFontSetting());
		descLabel.setFont(new Font(descLabel.getFont().getName(),
								   descLabel.getFont().getStyle(), 10));
		clientPanel.add(descLabel, clientPanelConstraints);
		return clientPanel;
	}

	/**
	 * Shows the forwarding server shutdown confirmation dialog. This dialog is necessary if the
	 * forwarding server is running when the connect-via-forwarder feature is enabled, because the
	 * components for starting/stopping the forwarding server will be disabled after that.
	 *
	 * @param a_parentComponent The component where the dialog will be centered over.
	 */
	private void showForwardingClientConfirmServerShutdownDialog(Component a_parentComponent)
	{
		final JAPDialog confirmDialog = new JAPDialog(a_parentComponent,
			JAPMessages.getString("settingsForwardingClientConfigConfirmServerShutdownDialogTitle"));
		confirmDialog.setDefaultCloseOperation(WindowConstants.DO_NOTHING_ON_CLOSE);
		JPanel confirmPanel = new JPanel();
		confirmDialog.getContentPane().add(confirmPanel);

		JAPHtmlMultiLineLabel settingsForwardingClientConfigConfirmServerShutdownLabel = new
			JAPHtmlMultiLineLabel(
				JAPMessages.getString("settingsForwardingClientConfigConfirmServerShutdownLabel"),
				getFontSetting());

		JButton settingsForwardingClientConfigConfirmServerShutdownShutdownButton = new JButton(
			  JAPMessages.getString("settingsForwardingClientConfigConfirmServerShutdownShutdownButton"));
		settingsForwardingClientConfigConfirmServerShutdownShutdownButton.setFont(getFontSetting());
		settingsForwardingClientConfigConfirmServerShutdownShutdownButton.addActionListener(new
			ActionListener()
		{
			public void actionPerformed(ActionEvent event)
			{
				/* if the shutdown button is pressed, shutdown the server and enable the
				 * connect-via-forwarder setting
				 */
				JAPModel.getInstance().getRoutingSettings().setRoutingMode(JAPRoutingSettings.
					ROUTING_MODE_DISABLED);
				JAPModel.getInstance().getRoutingSettings().setConnectViaForwarder(true);
				confirmDialog.dispose();
			}
		});

		JButton settingsForwardingClientConfigConfirmServerShutdownCancelButton = new JButton(
			  JAPMessages.getString("cancelButton"));
		settingsForwardingClientConfigConfirmServerShutdownCancelButton.setFont(getFontSetting());
		settingsForwardingClientConfigConfirmServerShutdownCancelButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent event)
			{
				/* if the Cancel button is pressed, leave everything untouched */
				confirmDialog.dispose();
			}
		});

		GridBagLayout confirmPanelLayout = new GridBagLayout();
		confirmPanel.setLayout(confirmPanelLayout);

		GridBagConstraints confirmPanelConstraints = new GridBagConstraints();
		confirmPanelConstraints.anchor = GridBagConstraints.NORTH;
		confirmPanelConstraints.fill = GridBagConstraints.NONE;
		confirmPanelConstraints.weighty = 0.0;
		confirmPanelConstraints.weightx = 1.0;

		confirmPanelConstraints.gridx = 0;
		confirmPanelConstraints.gridy = 0;
		confirmPanelConstraints.gridwidth = 2;
		confirmPanelConstraints.insets = new Insets(10, 5, 20, 5);
		confirmPanelLayout.setConstraints(settingsForwardingClientConfigConfirmServerShutdownLabel,
										  confirmPanelConstraints);
		confirmPanel.add(settingsForwardingClientConfigConfirmServerShutdownLabel);

		confirmPanelConstraints.gridx = 0;
		confirmPanelConstraints.gridy = 1;
		confirmPanelConstraints.weighty = 1.0;
		confirmPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
		confirmPanelConstraints.gridwidth = 1;
		confirmPanelConstraints.insets = new Insets(0, 5, 15, 5);
		confirmPanelLayout.setConstraints(settingsForwardingClientConfigConfirmServerShutdownShutdownButton,
										  confirmPanelConstraints);
		confirmPanel.add(settingsForwardingClientConfigConfirmServerShutdownShutdownButton);

		confirmPanelConstraints.gridx = 1;
		confirmPanelConstraints.gridy = 1;
		confirmPanelConstraints.insets = new Insets(0, 5, 15, 5);
		confirmPanelLayout.setConstraints(settingsForwardingClientConfigConfirmServerShutdownCancelButton,
										  confirmPanelConstraints);
		confirmPanel.add(settingsForwardingClientConfigConfirmServerShutdownCancelButton);

		confirmDialog.pack();
		confirmDialog.setVisible(true);
	}

	protected void onRootPanelShown()
	{
		//Register help context
		JAPHelp.getInstance().getContextObj().setContext("forwarding_client");
	}

}
