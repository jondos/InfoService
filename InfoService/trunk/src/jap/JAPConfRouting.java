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

import java.text.NumberFormat;
import java.util.Observable;
import java.util.Observer;
import java.util.Vector;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import anon.crypto.JAPCertificateStore;
import anon.infoservice.InfoServiceHolder;
import anon.infoservice.ListenerInterface;
import anon.infoservice.MixCascade;
import forward.JAPThread;
import forward.client.ClientForwardException;
import forward.client.ForwardConnectionDescriptor;
import forward.client.ForwarderInformationGrabber;
import forward.client.captcha.IImageEncodedCaptcha;
import forward.server.ForwardSchedulerStatistics;
import forward.server.ForwardServerManager;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/**
 * This is the configuration GUI for the JAP routing component.
 */
public class JAPConfRouting extends AbstractJAPConfModule implements Observer
{

  /**
   * This is the constant for getting the information about a forwarder directly from the
   * infoservice.
   */
  private static final int CLIENT_CONNECTION_DIRECT_TO_INFOSERVICE = 1;

  /**
   * This is the constant for getting the information about a forwarder via mail from the
   * infoservice.
   */
  private static final int CLIENT_CONNECTION_VIA_MAIL = 2;

  /**
   * This is the update interval for the server status panel in milliseconds.
   */
  private static final long SERVER_STATUS_UPDATE_INTERVAL = (long) 1000;

  /**
   * This stores the instance of the server port label.
   */
  private JLabel m_settingsRoutingServerPortLabel;

  /**
   * This stores the label wich shows the current bandwidth limitation.
   */
  private JLabel m_settingsRoutingBandwidthLabel;

  /**
   * This stores the instance of the bandwidth slider.
   */
  private JSlider m_settingsRoutingBandwidthSlider;

  /**
   * This stores the instance of the allowed connections label.
   */
  private JLabel m_settingsRoutingUserLabel;

  /**
   * This stores the instance of the allowed connections slider.
   */
  private JSlider m_settingsRoutingUserSlider;

  /**
   * This stores the instance of the list of allowed mixcascades for forwarding.
   */
  private JList m_settingsRoutingAllowedCascadesList;

  /**
   * This stores the selected connection method for getting the forwarder information from
   * infoservice. See the constants in this class for viewing the possible values.
   */
  private int m_clientConnectionMethod;

  /**
   * Stores the thread, which updates the server status panel.
   */
  private Thread m_serverStatusThread;

  /**
   * Constructor for JAPConfRouting. We do some initializing here.
   */
  public JAPConfRouting()
  {
    super(null);
    JAPModel.getModel().getRoutingSettings().addObserver(this);
  }

  /**
   * Creates the infoservice root panel with all child-panels.
   */
  public void recreateRootPanel()
  {
    updateRootPanel();
  }

  /**
   * Returns the title for the infoservice configuration tab.
   *
   * @return The title for the infoservice configuration tab.
   */
  public String getTabTitle()
  {
    return JAPMessages.getString("confRoutingTab");
  }

  /**
   * This is the observer implementation. If the routing mode is changed in JAPRoutingSettings,
   * we update the root panel to show only the available panels for the new routing mode.
   *
   * @param a_notifier The observed Object. This should always be JAPRoutingSettings at the moment.
   * @param a_message The reason of the notification. This should always be a JAPRoutingMessage
   *                  at the moment.
   */
  public void update(Observable a_notifier, Object a_message)
  {
    try
    {
      if (a_notifier.getClass().equals(Class.forName("jap.JAPRoutingSettings")))
      {
        /* message is from JAPRoutingSettings */
        if ( ( (JAPRoutingMessage) (a_message)).getMessageCode() ==
          JAPRoutingMessage.ROUTING_MODE_CHANGED)
        {
          updateRootPanel();
        }
      }
    }
    catch (Exception e)
    {
    }
  }

  /**
   * Creates the routing server config panel with all components.
   *
   * @return The routing server config panel.
   */
  private JPanel createRoutingServerConfigPanel()
  {
    final JPanel configPanel = new JPanel();

    boolean serverRunning = false;
    if (JAPModel.getModel().getRoutingSettings().getRoutingMode() ==
      JAPRoutingSettings.ROUTING_MODE_SERVER)
    {
      serverRunning = true;
    }
    final JCheckBox settingsRoutingStartServerBox = new JCheckBox(JAPMessages.getString(
      "settingsRoutingStartServerBox"), serverRunning);
    settingsRoutingStartServerBox.setFont(getFontSetting());
    settingsRoutingStartServerBox.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent event)
      {
        /* start or shutdown the forwarding server */
        if (settingsRoutingStartServerBox.isSelected())
        {
          /* only for the moment -> later we will have a configuration dialog */
          JAPModel.getModel().getRoutingSettings().setRegistrationInfoServices(InfoServiceHolder.getInstance().getInfoservicesWithForwarderList());
          /* start the server by changing the routing mode */
          if (JAPModel.getModel().getRoutingSettings().setRoutingMode(JAPRoutingSettings.
            ROUTING_MODE_SERVER) == false)
          {
            /* there was an error while starting the server */
            settingsRoutingStartServerBox.setSelected(false);
            JOptionPane.showMessageDialog(configPanel,
              JAPMessages.getString("settingsRoutingStartServerError"),
              JAPMessages.getString("ERROR"), JOptionPane.ERROR_MESSAGE);
          }
          else {
            /* starting the server was successful, start the infoservice registration */
            showRegisterAtInfoServices();
          }
        }
        else
        {
          /* shutdown the server */
          JAPModel.getModel().getRoutingSettings().setRoutingMode(JAPRoutingSettings.
            ROUTING_MODE_DISABLED);
        }
      }
    });

    m_settingsRoutingServerPortLabel = new JLabel();
    m_settingsRoutingServerPortLabel.setFont(getFontSetting());
    updateServerPortLabel();
    JButton settingsRoutingPortEditButton = new JButton(JAPMessages.getString(
      "settingsRoutingPortEditButton"));
    settingsRoutingPortEditButton.setFont(getFontSetting());
    settingsRoutingPortEditButton.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent event)
      {
        /* if the change port button is pressed, show the change server port dialog */
        showChangeServerPortDialog();
        updateServerPortLabel();
      }
    });

    JButton settingsRoutingServerConfigExpertButton = new JButton(JAPMessages.getString("settingsRoutingServerConfigExpertButton"));
    settingsRoutingServerConfigExpertButton.setFont(getFontSetting());
    settingsRoutingServerConfigExpertButton.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent event)
      {
        /* if the expert settings button is pressed, show the server expert settings dialog */
        showRoutingServerExpertDialog();
      }
    });

    TitledBorder settingsRoutingConfigBorder = new TitledBorder(JAPMessages.getString(
      "settingsRoutingConfigBorder"));
    settingsRoutingConfigBorder.setTitleFont(getFontSetting());
    configPanel.setBorder(settingsRoutingConfigBorder);

    GridBagLayout configPanelLayout = new GridBagLayout();
    configPanel.setLayout(configPanelLayout);

    GridBagConstraints configPanelConstraints = new GridBagConstraints();
    configPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
    configPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
    configPanelConstraints.weightx = 1.0;
    configPanelConstraints.weighty = 0.0;

    configPanelConstraints.gridx = 0;
    configPanelConstraints.gridy = 0;
    configPanelConstraints.gridwidth = 2;
    configPanelConstraints.insets = new Insets(5, 5, 10, 5);
    configPanelLayout.setConstraints(settingsRoutingStartServerBox, configPanelConstraints);
    configPanel.add(settingsRoutingStartServerBox);

    configPanelConstraints.gridx = 0;
    configPanelConstraints.gridy = 1;
    configPanelConstraints.gridwidth = 1;
    configPanelConstraints.weightx = 1.0;
    configPanelConstraints.insets = new Insets(0, 5, 10, 10);
    configPanelLayout.setConstraints(m_settingsRoutingServerPortLabel, configPanelConstraints);
    configPanel.add(m_settingsRoutingServerPortLabel);

    configPanelConstraints.gridx = 1;
    configPanelConstraints.gridy = 1;
    configPanelConstraints.weightx = 0.0;
    configPanelConstraints.insets = new Insets(0, 0, 10, 5);
    configPanelLayout.setConstraints(settingsRoutingPortEditButton, configPanelConstraints);
    configPanel.add(settingsRoutingPortEditButton);

    configPanelConstraints.gridx = 0;
    configPanelConstraints.gridy = 2;
    configPanelConstraints.weightx = 1.0;
    configPanelConstraints.weighty = 1.0;
    configPanelConstraints.insets = new Insets(0, 5, 10, 5);
    configPanelConstraints.fill = GridBagConstraints.NONE;
    configPanelLayout.setConstraints(settingsRoutingServerConfigExpertButton, configPanelConstraints);
    configPanel.add(settingsRoutingServerConfigExpertButton);

    return configPanel;
  }

  /**
   * Shows the register at infoservices box after starting the forwarding server.
   */
  private void showRegisterAtInfoServices()
  {
    final JAPDialog registerDialog = new JAPDialog(getRootPanel(), JAPMessages.getString("settingsRoutingServerRegisterDialogTitle"));
    registerDialog.disableManualClosing();
    final JPanel registerPanel = registerDialog.getRootPanel();

    JLabel settingsRoutingServerRegisterDialogRegisterLabel = new JLabel(JAPMessages.getString("settingsRoutingServerRegisterDialogRegisterLabel"));
    settingsRoutingServerRegisterDialogRegisterLabel.setFont(getFontSetting());
    JLabel busyLabel = new JLabel(JAPUtil.loadImageIcon(JAPConstants.BUSYFN, true));

    JButton settingsRoutingServerRegisterDialogCancelButton = new JButton(JAPMessages.getString("cancelButton"));
    settingsRoutingServerRegisterDialogCancelButton.setFont(getFontSetting());
    settingsRoutingServerRegisterDialogCancelButton.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent event)
      {
        /* if the Cancel button is pressed, stop the server -> the infoservice registration
         * process is also canceled
         */
        JAPModel.getModel().getRoutingSettings().setRoutingMode(JAPRoutingSettings.ROUTING_MODE_DISABLED);
      }
    });

    GridBagLayout registerPanelLayout = new GridBagLayout();
    registerPanel.setLayout(registerPanelLayout);

    GridBagConstraints registerPanelConstraints = new GridBagConstraints();
    registerPanelConstraints.anchor = GridBagConstraints.NORTH;
    registerPanelConstraints.fill = GridBagConstraints.NONE;
    registerPanelConstraints.weighty = 0.0;

    registerPanelConstraints.gridx = 0;
    registerPanelConstraints.gridy = 0;
    registerPanelConstraints.insets = new Insets(5, 5, 0, 5);
    registerPanelLayout.setConstraints(settingsRoutingServerRegisterDialogRegisterLabel, registerPanelConstraints);
    registerPanel.add(settingsRoutingServerRegisterDialogRegisterLabel);

    registerPanelConstraints.gridx = 0;
    registerPanelConstraints.gridy = 1;
    registerPanelConstraints.insets = new Insets(10, 5, 10, 5);
    registerPanelLayout.setConstraints(busyLabel, registerPanelConstraints);
    registerPanel.add(busyLabel);

    registerPanelConstraints.gridx = 0;
    registerPanelConstraints.gridy = 2;
    registerPanelConstraints.insets = new Insets(0, 5, 5, 5);
    registerPanelConstraints.weighty = 1.0;
    registerPanelLayout.setConstraints(settingsRoutingServerRegisterDialogCancelButton, registerPanelConstraints);
    registerPanel.add(settingsRoutingServerRegisterDialogCancelButton);

    registerDialog.align();

    Thread registerThread = new Thread(new JAPThread()
    {
      public void run()
      {
        JAPModel.getModel().getRoutingSettings().startPropaganda(true);
        registerDialog.hide();
      }
    });
    registerThread.start();

    registerDialog.show();
  }
  
  /**
   * Creates the routing server status panel. Also the internal thread, which updates the
   * panel periodically is started.
   */
  private JPanel createRoutingServerStatusPanel()
  {
    /* get the statistics instance */
    final ForwardSchedulerStatistics schedulerStatistics = JAPModel.getModel().getRoutingSettings().
      getSchedulerStatistics();

    /* get the NumberFormat instance for formating the bandwidth (double) */
    final NumberFormat bandwidthFormat = NumberFormat.getInstance();
    bandwidthFormat.setMinimumFractionDigits(1);
    bandwidthFormat.setMaximumFractionDigits(1);
    bandwidthFormat.setMinimumIntegerDigits(1);

    /* get the NumberFormat instance for formating the other numbers (integer) */
    final NumberFormat integerFormat = NumberFormat.getInstance();
    bandwidthFormat.setMinimumIntegerDigits(1);

    JPanel serverStatusPanel = new JPanel();

    final JLabel settingsRoutingServerStatusBandwidthLabel = new JLabel();
    settingsRoutingServerStatusBandwidthLabel.setFont(getFontSetting());
    final JLabel settingsRoutingServerStatusForwardedBytesLabel = new JLabel();
    settingsRoutingServerStatusForwardedBytesLabel.setFont(getFontSetting());
    final JLabel settingsRoutingServerStatusConnectionsLabel = new JLabel(JAPMessages.getString(
      "settingsRoutingServerStatusConnectionsLabel"));
    settingsRoutingServerStatusConnectionsLabel.setFont(getFontSetting());
    final JLabel settingsRoutingServerStatusCurrentConnectionsLabel = new JLabel();
    settingsRoutingServerStatusCurrentConnectionsLabel.setFont(getFontSetting());
    final JLabel settingsRoutingServerStatusAcceptedConnectionsLabel = new JLabel();
    settingsRoutingServerStatusAcceptedConnectionsLabel.setFont(getFontSetting());
    final JLabel settingsRoutingServerStatusRejectedConnectionsLabel = new JLabel();
    settingsRoutingServerStatusRejectedConnectionsLabel.setFont(getFontSetting());

    TitledBorder settingsRoutingServerStatusBorder = new TitledBorder(JAPMessages.getString(
      "settingsRoutingServerStatusBorder"));
    settingsRoutingServerStatusBorder.setTitleFont(getFontSetting());
    serverStatusPanel.setBorder(settingsRoutingServerStatusBorder);

    GridBagLayout serverStatusPanelLayout = new GridBagLayout();
    serverStatusPanel.setLayout(serverStatusPanelLayout);

    GridBagConstraints serverStatusPanelConstraints = new GridBagConstraints();
    serverStatusPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
    serverStatusPanelConstraints.weightx = 1.0;
    serverStatusPanelConstraints.weighty = 0.0;
    serverStatusPanelConstraints.gridx = 0;
    serverStatusPanelConstraints.gridy = 0;
    serverStatusPanelConstraints.gridwidth = 4;
    serverStatusPanelConstraints.insets = new Insets(5, 5, 10, 5);
    serverStatusPanelLayout.setConstraints(settingsRoutingServerStatusBandwidthLabel,
                         serverStatusPanelConstraints);
    serverStatusPanel.add(settingsRoutingServerStatusBandwidthLabel);

    serverStatusPanelConstraints.gridx = 0;
    serverStatusPanelConstraints.gridy = 1;
    serverStatusPanelConstraints.insets = new Insets(0, 5, 10, 5);
    serverStatusPanelLayout.setConstraints(settingsRoutingServerStatusForwardedBytesLabel,
                         serverStatusPanelConstraints);
    serverStatusPanel.add(settingsRoutingServerStatusForwardedBytesLabel);

    serverStatusPanelConstraints.gridx = 0;
    serverStatusPanelConstraints.gridy = 2;
    serverStatusPanelConstraints.weightx = 0.0;
    serverStatusPanelConstraints.weighty = 1.0;
    serverStatusPanelConstraints.gridwidth = 1;
    serverStatusPanelConstraints.insets = new Insets(0, 5, 5, 15);
    serverStatusPanelLayout.setConstraints(settingsRoutingServerStatusConnectionsLabel,
                         serverStatusPanelConstraints);
    serverStatusPanel.add(settingsRoutingServerStatusConnectionsLabel);

    serverStatusPanelConstraints.gridx = 1;
    serverStatusPanelConstraints.gridy = 2;
    serverStatusPanelConstraints.insets = new Insets(0, 0, 5, 15);
    serverStatusPanelLayout.setConstraints(settingsRoutingServerStatusCurrentConnectionsLabel,
                         serverStatusPanelConstraints);
    serverStatusPanel.add(settingsRoutingServerStatusCurrentConnectionsLabel);

    serverStatusPanelConstraints.gridx = 2;
    serverStatusPanelConstraints.gridy = 2;
    serverStatusPanelConstraints.insets = new Insets(0, 0, 5, 15);
    serverStatusPanelLayout.setConstraints(settingsRoutingServerStatusAcceptedConnectionsLabel,
                         serverStatusPanelConstraints);
    serverStatusPanel.add(settingsRoutingServerStatusAcceptedConnectionsLabel);

    serverStatusPanelConstraints.gridx = 3;
    serverStatusPanelConstraints.gridy = 2;
    serverStatusPanelConstraints.weightx = 1.0;
    serverStatusPanelConstraints.insets = new Insets(0, 0, 5, 5);
    serverStatusPanelLayout.setConstraints(settingsRoutingServerStatusRejectedConnectionsLabel,
                         serverStatusPanelConstraints);
    serverStatusPanel.add(settingsRoutingServerStatusRejectedConnectionsLabel);

    /* create the status update thread */
    m_serverStatusThread = new Thread(new JAPThread()
    {
      /* this is the implementation for the server status update thread */
      public void run()
      {
        boolean interrupted = false;
        while (interrupted == false)
        {
          settingsRoutingServerStatusBandwidthLabel.setText(JAPMessages.getString(
            "settingsRoutingServerStatusBandwidthLabel") + " " +
            bandwidthFormat.format( ( (double) schedulerStatistics.getCurrentBandwidthUsage()) /
                         (double) 1024) + " " +
            JAPMessages.getString("settingsRoutingServerStatusBandwidthLabelPart2"));
          settingsRoutingServerStatusForwardedBytesLabel.setText(JAPMessages.getString(
            "settingsRoutingServerStatusForwardedBytesLabel") + " " +
            integerFormat.format(schedulerStatistics.getTransferedBytes()));
          settingsRoutingServerStatusCurrentConnectionsLabel.setText(JAPMessages.getString(
            "settingsRoutingServerStatusCurrentConnectionsLabel") + " " +
            integerFormat.format(JAPModel.getModel().getRoutingSettings().
                       getCurrentlyForwardedConnections()));
          settingsRoutingServerStatusAcceptedConnectionsLabel.setText(JAPMessages.getString(
            "settingsRoutingServerStatusAcceptedConnectionsLabel") + " " +
            integerFormat.format(schedulerStatistics.getAcceptedConnections()));
          settingsRoutingServerStatusRejectedConnectionsLabel.setText(JAPMessages.getString(
            "settingsRoutingServerStatusRejectedConnectionsLabel") + " " +
            integerFormat.format(schedulerStatistics.getRejectedConnections()));
          interrupted = Thread.interrupted();
          if (interrupted == false)
          {
            try
            {
              Thread.sleep(SERVER_STATUS_UPDATE_INTERVAL);
            }
            catch (Exception e)
            {
              interrupted = true;
            }
          }
        }
      }
    });
    m_serverStatusThread.setDaemon(true);

    m_serverStatusThread.start();

    return serverStatusPanel;
  }

  /**
   * Creates the routing client panel (only the config-dialog start button).
   *
   * @return The routing client config panel.
   */
  private JPanel createRoutingClientConfigPanel()
  {
    JPanel clientPanel = new JPanel();

    JButton settingsRoutingClientButton = null;
    if (JAPModel.getModel().getRoutingSettings().getRoutingMode() !=
      JAPRoutingSettings.ROUTING_MODE_CLIENT)
    {
      settingsRoutingClientButton = new JButton(JAPMessages.getString(
        "settingsRoutingClientStartButton"));
      settingsRoutingClientButton.addActionListener(new ActionListener()
      {
        public void actionPerformed(ActionEvent event)
        {
          /* if the Start Client button is pressed, show the config client dialog */
          showConfigClientDialogStep0();
        }
      });
    }
    else
    {
      settingsRoutingClientButton = new JButton(JAPMessages.getString("settingsRoutingClientStopButton"));
      settingsRoutingClientButton.addActionListener(new ActionListener()
      {
        public void actionPerformed(ActionEvent event)
        {
          /* if the Stop Client button is pressed, disable client forwarding */
          JAPModel.getModel().getRoutingSettings().setRoutingMode(JAPRoutingSettings.
            ROUTING_MODE_DISABLED);
        }
      });
    }
    settingsRoutingClientButton.setFont(getFontSetting());

    TitledBorder settingsRoutingClientBorder = new TitledBorder(JAPMessages.getString(
      "settingsRoutingClientBorder"));
    settingsRoutingClientBorder.setTitleFont(getFontSetting());
    clientPanel.setBorder(settingsRoutingClientBorder);

    GridBagLayout clientPanelLayout = new GridBagLayout();
    clientPanel.setLayout(clientPanelLayout);

    GridBagConstraints clientPanelConstraints = new GridBagConstraints();
    clientPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
    clientPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
    clientPanelConstraints.weightx = 1.0;
    clientPanelConstraints.weighty = 0.0;
    clientPanelConstraints.gridx = 0;
    clientPanelConstraints.gridy = 0;
    clientPanelLayout.setConstraints(settingsRoutingClientButton, clientPanelConstraints);
    clientPanel.add(settingsRoutingClientButton);

    return clientPanel;
  }

  /**
   * Shows the change forwarding server port dialog.
   */
  private void showChangeServerPortDialog()
  {
    final JAPDialog changeDialog = new JAPDialog(getRootPanel(),
      JAPMessages.getString("settingsRoutingChangeDialogTitle"));
    final JPanel changePanel = changeDialog.getRootPanel();

    final JAPJIntField settingsRoutingPortField = new JAPJIntField(Integer.toString(JAPModel.getModel().
      getRoutingSettings().getServerPort()));
    settingsRoutingPortField.setFont(getFontSetting());
    JButton settingsRoutingChangeDialogChangeButton = new JButton(JAPMessages.getString(
      "settingsRoutingChangeDialogChangeButton"));
    settingsRoutingChangeDialogChangeButton.setFont(getFontSetting());
    settingsRoutingChangeDialogChangeButton.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent event)
      {
        /* if the Change button is pressed, we try to change the routing server port */
        try
        {
          int port = Integer.parseInt(settingsRoutingPortField.getText().trim());
          if (JAPModel.getModel().getRoutingSettings().setServerPort(port) == false)
          {
            throw (new Exception(
              "JAPConfRouting: showChangeServerPortDialog: Error while changing server port."));
          }
          changeDialog.hide();
        }
        catch (Exception e)
        {
          JOptionPane.showMessageDialog(changePanel,
                          JAPMessages.getString("settingsRoutingChangeServerPortError"),
                          JAPMessages.getString("ERROR"), JOptionPane.ERROR_MESSAGE);
        }
      }
    });
    JButton settingsRoutingChangeDialogCancelButton = new JButton(JAPMessages.getString("cancelButton"));
    settingsRoutingChangeDialogCancelButton.setFont(getFontSetting());
    settingsRoutingChangeDialogCancelButton.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent event)
      {
        /* if the Cancel button is pressed, close the dialog */
        changeDialog.hide();
      }
    });
    JLabel settingsRoutingChangeDialogPortLabel = new JLabel(JAPMessages.getString(
      "settingsRoutingChangeDialogPortLabel"));
    settingsRoutingChangeDialogPortLabel.setFont(getFontSetting());

    TitledBorder settingsRoutingChangeDialogBorder = new TitledBorder(JAPMessages.getString(
      "settingsRoutingChangeDialogBorder"));
    settingsRoutingChangeDialogBorder.setTitleFont(getFontSetting());
    changePanel.setBorder(settingsRoutingChangeDialogBorder);

    GridBagLayout changePanelLayout = new GridBagLayout();
    changePanel.setLayout(changePanelLayout);

    GridBagConstraints changePanelConstraints = new GridBagConstraints();
    changePanelConstraints.anchor = GridBagConstraints.NORTHWEST;
    changePanelConstraints.fill = GridBagConstraints.NONE;
    changePanelConstraints.weightx = 0.0;

    changePanelConstraints.gridx = 0;
    changePanelConstraints.gridy = 0;
    changePanelLayout.setConstraints(settingsRoutingChangeDialogPortLabel, changePanelConstraints);
    changePanel.add(settingsRoutingChangeDialogPortLabel);

    changePanelConstraints.gridx = 1;
    changePanelConstraints.gridy = 0;
    changePanelConstraints.weightx = 1.0;
    changePanelConstraints.insets = new Insets(0, 5, 10, 0);
    changePanelLayout.setConstraints(settingsRoutingPortField, changePanelConstraints);
    changePanel.add(settingsRoutingPortField);

    changePanelConstraints.gridx = 0;
    changePanelConstraints.gridy = 1;
    changePanelConstraints.weightx = 0.0;
    changePanelConstraints.weighty = 1.0;
    changePanelLayout.setConstraints(settingsRoutingChangeDialogChangeButton, changePanelConstraints);
    changePanel.add(settingsRoutingChangeDialogChangeButton);

    changePanelConstraints.gridx = 1;
    changePanelConstraints.gridy = 1;
    changePanelConstraints.weightx = 1.0;
    changePanelConstraints.insets = new Insets(0, 10, 10, 0);
    changePanelLayout.setConstraints(settingsRoutingChangeDialogCancelButton, changePanelConstraints);
    changePanel.add(settingsRoutingChangeDialogCancelButton);

    changeDialog.align();
    changeDialog.show();
  }

  /**
   * Shows the forwarding server expert settings dialog.
   */
  private void showRoutingServerExpertDialog() {
    final JAPDialog expertDialog = new JAPDialog(getRootPanel(), JAPMessages.getString("settingsRoutingServerExpertDialogTitle"));
    JPanel expertPanel = expertDialog.getRootPanel();
    
    m_settingsRoutingBandwidthLabel = new JLabel();
    m_settingsRoutingBandwidthLabel.setFont(getFontSetting());
    m_settingsRoutingBandwidthSlider = new JSlider(0, JAPModel.getModel().getRoutingSettings().getMaxBandwidth() / 1024, 0);
    m_settingsRoutingBandwidthSlider.setFont(getFontSetting());
    updateBandwidthSlider();
    m_settingsRoutingBandwidthSlider.setSnapToTicks(true);
    m_settingsRoutingBandwidthSlider.setPaintTicks(true);
    m_settingsRoutingBandwidthSlider.setPaintLabels(true);
    m_settingsRoutingBandwidthSlider.addChangeListener(new ChangeListener()
    {
      public void stateChanged(ChangeEvent event)
      {
        /* change the bandwidth limitation */
        JAPModel.getModel().getRoutingSettings().setBandwidth(m_settingsRoutingBandwidthSlider.
          getValue() * 1024);
        updateBandwidthLabel();
        /* update the user slider, maybe the number of allowed connections was influenced */
        updateUserSlider();
      }
    });
    JButton settingsRoutingMaxBandwidthButton = new JButton(JAPMessages.getString(
      "settingsRoutingMaxBandwidthButton"));
    settingsRoutingMaxBandwidthButton.setFont(getFontSetting());
    settingsRoutingMaxBandwidthButton.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent event)
      {
        /* if the max bandwidth button is pressed, show the maximum bandwidth dialog */
        showMaximumBandwidthDialog(expertDialog.getRootPanel());
        updateBandwidthSlider();
        /* maybe the number of simultaneous possible connections is also affected */
        updateUserSlider();
      }
    });

    m_settingsRoutingUserLabel = new JLabel();
    m_settingsRoutingUserLabel.setFont(getFontSetting());
    m_settingsRoutingUserSlider = new JSlider(0,
                          JAPModel.getModel().getRoutingSettings().getBandwidthMaxConnections(),
                          0);
    m_settingsRoutingUserSlider.setFont(getFontSetting());
    updateUserSlider();
    m_settingsRoutingUserSlider.setSnapToTicks(true);
    m_settingsRoutingUserSlider.setPaintTicks(true);
    m_settingsRoutingUserSlider.setPaintLabels(true);
    m_settingsRoutingUserSlider.addChangeListener(new ChangeListener()
    {
      public void stateChanged(ChangeEvent event)
      {
        /* change the number of simultaneously forwarded connections */
        JAPModel.getModel().getRoutingSettings().setAllowedConnections(m_settingsRoutingUserSlider.
          getValue());
        updateUserLabel();
      }
    });

    JButton settingsRoutingCascadesEditButton = new JButton(JAPMessages.getString(
      "settingsRoutingCascadesEditButton"));
    settingsRoutingCascadesEditButton.setFont(getFontSetting());
    settingsRoutingCascadesEditButton.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent event)
      {
        /* if the edit cascades button is pressed, show the edit allowed cascades dialog */
        showAllowedMixCascadesDialog(expertDialog.getRootPanel());
      }
    });

    JButton settingsRoutingServerExpertDialogReadyButton = new JButton(JAPMessages.getString("settingsRoutingServerExpertDialogReadyButton"));
    settingsRoutingServerExpertDialogReadyButton.setFont(getFontSetting());
    settingsRoutingServerExpertDialogReadyButton.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent event)
      {
        /* if the ready button is pressed, we can close the dialog */
        expertDialog.hide();
      }
    });
    
    TitledBorder settingsRoutingServerExpertDialogBorder = new TitledBorder(JAPMessages.getString("settingsRoutingServerExpertDialogBorder"));
    settingsRoutingServerExpertDialogBorder.setTitleFont(getFontSetting());
    expertPanel.setBorder(settingsRoutingServerExpertDialogBorder);

    GridBagLayout expertPanelLayout = new GridBagLayout();
    expertPanel.setLayout(expertPanelLayout);
    GridBagConstraints expertPanelConstraints = new GridBagConstraints();
    
    expertPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
    expertPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
    expertPanelConstraints.gridx = 0;
    expertPanelConstraints.gridy = 0;
    expertPanelConstraints.weightx = 1.0;
    expertPanelConstraints.gridwidth = 1;
    expertPanelConstraints.insets = new Insets(5, 5, 0, 0);
    expertPanelLayout.setConstraints(m_settingsRoutingBandwidthLabel, expertPanelConstraints);
    expertPanel.add(m_settingsRoutingBandwidthLabel);

    expertPanelConstraints.gridx = 0;
    expertPanelConstraints.gridy = 1;
    expertPanelConstraints.insets = new Insets(0, 5, 10, 0);
    expertPanelLayout.setConstraints(m_settingsRoutingBandwidthSlider, expertPanelConstraints);
    expertPanel.add(m_settingsRoutingBandwidthSlider);

    expertPanelConstraints.gridx = 1;
    expertPanelConstraints.gridy = 1;
    expertPanelConstraints.weightx = 0.0;
    expertPanelConstraints.insets = new Insets(0, 10, 10, 5);
    expertPanelLayout.setConstraints(settingsRoutingMaxBandwidthButton, expertPanelConstraints);
    expertPanel.add(settingsRoutingMaxBandwidthButton);

    expertPanelConstraints.gridx = 0;
    expertPanelConstraints.gridy = 2;
    expertPanelConstraints.weightx = 1.0;
    expertPanelConstraints.insets = new Insets(0, 5, 0, 0);
    expertPanelLayout.setConstraints(m_settingsRoutingUserLabel, expertPanelConstraints);
    expertPanel.add(m_settingsRoutingUserLabel);

    expertPanelConstraints.gridx = 0;
    expertPanelConstraints.gridy = 3;
    expertPanelConstraints.insets = new Insets(0, 5, 10, 0);
    expertPanelLayout.setConstraints(m_settingsRoutingUserSlider, expertPanelConstraints);
    expertPanel.add(m_settingsRoutingUserSlider);

    expertPanelConstraints.gridx = 0;
    expertPanelConstraints.gridy = 4;
    expertPanelConstraints.fill = GridBagConstraints.NONE;
    expertPanelConstraints.insets = new Insets(0, 5, 20, 0);
    expertPanelLayout.setConstraints(settingsRoutingCascadesEditButton, expertPanelConstraints);
    expertPanel.add(settingsRoutingCascadesEditButton);

    expertPanelConstraints.gridx = 0;
    expertPanelConstraints.gridy = 5;
    expertPanelConstraints.gridwidth = 2;
    expertPanelConstraints.weighty = 1.0;
    expertPanelConstraints.anchor = GridBagConstraints.NORTH;
    expertPanelConstraints.insets = new Insets(0, 5, 5, 5);
    expertPanelLayout.setConstraints(settingsRoutingServerExpertDialogReadyButton, expertPanelConstraints);
    expertPanel.add(settingsRoutingServerExpertDialogReadyButton);   

    expertDialog.align();
    expertDialog.show();
  }

  /**
   * Shows the maximum bandwidth dialog.
   *
   * @param a_parentComponent The parent component over which the dialog is centered.
   */
  private void showMaximumBandwidthDialog(JComponent a_parentComponent)
  {
    final JAPDialog bandwidthDialog = new JAPDialog(a_parentComponent, JAPMessages.getString("settingsRoutingBandwidthDialogTitle"));
    final JPanel bandwidthPanel = bandwidthDialog.getRootPanel();

    final JAPJIntField settingsRoutingBandwidthField = new JAPJIntField(Integer.toString(JAPModel.
      getModel().getRoutingSettings().getMaxBandwidth() / 1024));
    settingsRoutingBandwidthField.setFont(getFontSetting());
    settingsRoutingBandwidthField.setColumns(5);
    JButton settingsRoutingBandwidthDialogChangeButton = new JButton(JAPMessages.getString(
      "settingsRoutingBandwidthDialogChangeButton"));
    settingsRoutingBandwidthDialogChangeButton.setFont(getFontSetting());
    settingsRoutingBandwidthDialogChangeButton.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent event)
      {
        /* if the Change button is pressed, we change the maximum possible bandwidth */
        try
        {
          int maxBandwidth = Integer.parseInt(settingsRoutingBandwidthField.getText().trim()) *
            1024;
          JAPModel.getModel().getRoutingSettings().setMaxBandwidth(maxBandwidth);
          bandwidthDialog.hide();
        }
        catch (Exception e)
        {
          JOptionPane.showMessageDialog(bandwidthPanel,
                          JAPMessages.getString("settingsRoutingMaxBandwidthError"),
                          JAPMessages.getString("ERROR"), JOptionPane.ERROR_MESSAGE);
        }
      }
    });
    JButton settingsRoutingBandwidthDialogCancelButton = new JButton(JAPMessages.getString("cancelButton"));
    settingsRoutingBandwidthDialogCancelButton.setFont(getFontSetting());
    settingsRoutingBandwidthDialogCancelButton.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent event)
      {
        /* if the Cancel button is pressed, close the dialog */
        bandwidthDialog.hide();
      }
    });
    JLabel settingsRoutingBandwidthDialogBandwidthLabel = new JLabel(JAPMessages.getString(
      "settingsRoutingBandwidthDialogBandwidthLabel"));
    settingsRoutingBandwidthDialogBandwidthLabel.setFont(getFontSetting());

    TitledBorder settingsRoutingBandwidthDialogBorder = new TitledBorder(JAPMessages.getString(
      "settingsRoutingBandwidthDialogBorder"));
    settingsRoutingBandwidthDialogBorder.setTitleFont(getFontSetting());
    bandwidthPanel.setBorder(settingsRoutingBandwidthDialogBorder);

    GridBagLayout bandwidthPanelLayout = new GridBagLayout();
    bandwidthPanel.setLayout(bandwidthPanelLayout);

    GridBagConstraints bandwidthPanelConstraints = new GridBagConstraints();
    bandwidthPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
    bandwidthPanelConstraints.fill = GridBagConstraints.NONE;
    bandwidthPanelConstraints.weightx = 0.0;

    bandwidthPanelConstraints.gridx = 0;
    bandwidthPanelConstraints.gridy = 0;
    bandwidthPanelLayout.setConstraints(settingsRoutingBandwidthDialogBandwidthLabel,
                      bandwidthPanelConstraints);
    bandwidthPanel.add(settingsRoutingBandwidthDialogBandwidthLabel);

    bandwidthPanelConstraints.gridx = 1;
    bandwidthPanelConstraints.gridy = 0;
    bandwidthPanelConstraints.weightx = 1.0;
    bandwidthPanelConstraints.insets = new Insets(0, 5, 10, 0);
    bandwidthPanelLayout.setConstraints(settingsRoutingBandwidthField, bandwidthPanelConstraints);
    bandwidthPanel.add(settingsRoutingBandwidthField);

    bandwidthPanelConstraints.gridx = 0;
    bandwidthPanelConstraints.gridy = 1;
    bandwidthPanelConstraints.weightx = 0.0;
    bandwidthPanelConstraints.weighty = 1.0;
    bandwidthPanelLayout.setConstraints(settingsRoutingBandwidthDialogChangeButton,
                      bandwidthPanelConstraints);
    bandwidthPanel.add(settingsRoutingBandwidthDialogChangeButton);

    bandwidthPanelConstraints.gridx = 1;
    bandwidthPanelConstraints.gridy = 1;
    bandwidthPanelConstraints.weightx = 1.0;
    bandwidthPanelConstraints.insets = new Insets(0, 10, 10, 0);
    bandwidthPanelLayout.setConstraints(settingsRoutingBandwidthDialogCancelButton,
                      bandwidthPanelConstraints);
    bandwidthPanel.add(settingsRoutingBandwidthDialogCancelButton);

    bandwidthDialog.align();
    bandwidthDialog.show();
  }

  /**
   * Shows the allowed mixcascades dialog.
   *
   * @param a_parentComponent The parent component over which the dialog is centered.
   */
  private void showAllowedMixCascadesDialog(JComponent a_parentComponent)
  {
    final JAPDialog cascadesDialog = new JAPDialog(a_parentComponent, JAPMessages.getString("settingsRoutingCascadesDialogTitle"));
    JPanel cascadesPanel = cascadesDialog.getRootPanel();

    Vector knownCascades = InfoServiceHolder.getInstance().getMixCascades();
    if (knownCascades == null)
    {
      JOptionPane.showMessageDialog(cascadesPanel,
                      JAPMessages.getString("settingsRoutingFetchCascadesError"),
                      JAPMessages.getString("ERROR"), JOptionPane.ERROR_MESSAGE);
      knownCascades = new Vector();
    }

    final JList knownCascadesList = new JList(knownCascades);
    knownCascadesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    JScrollPane knownCascadesScrollPane = new JScrollPane(knownCascadesList);
    knownCascadesScrollPane.setFont(getFontSetting());

    m_settingsRoutingAllowedCascadesList = new JList();
    updateAllowedCascadesList();
    m_settingsRoutingAllowedCascadesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    JScrollPane allowedCascadesScrollPane = new JScrollPane(m_settingsRoutingAllowedCascadesList);
    allowedCascadesScrollPane.setFont(getFontSetting());

    JButton settingsRoutingCascadesDialogAddButton = new JButton(JAPMessages.getString(
      "settingsRoutingCascadesDialogAddButton"));
    settingsRoutingCascadesDialogAddButton.setFont(getFontSetting());
    settingsRoutingCascadesDialogAddButton.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent event)
      {
        /* if the Add button is pressed, add the selected known mixcascade to the list of allowed
         * mixcascades
         */
        MixCascade selectedMixCascade = (MixCascade) (knownCascadesList.getSelectedValue());
        if (selectedMixCascade != null)
        {
          ForwardServerManager.getInstance().getAllowedCascadesDatabase().addCascade(
            selectedMixCascade);
          updateAllowedCascadesList();
        }
      }
    });

    JButton settingsRoutingCascadesDialogRemoveButton = new JButton(JAPMessages.getString(
      "settingsRoutingCascadesDialogRemoveButton"));
    settingsRoutingCascadesDialogRemoveButton.setFont(getFontSetting());
    settingsRoutingCascadesDialogRemoveButton.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent event)
      {
        /* if the Remove button is pressed, remove the selected mixcascade from the list of
         * allowed mixcascades
         */
        MixCascade selectedMixCascade = (MixCascade) (m_settingsRoutingAllowedCascadesList.
          getSelectedValue());
        if (selectedMixCascade != null)
        {
          ForwardServerManager.getInstance().getAllowedCascadesDatabase().removeCascade(
            selectedMixCascade.getId());
          updateAllowedCascadesList();
        }
      }
    });

    JButton settingsRoutingCascadesDialogReadyButton = new JButton(JAPMessages.getString(
      "settingsRoutingCascadesDialogReadyButton"));
    settingsRoutingCascadesDialogReadyButton.setFont(getFontSetting());
    settingsRoutingCascadesDialogReadyButton.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent event)
      {
        /* if the Ready button is pressed, we leave the dialog */
        cascadesDialog.hide();
      }
    });

    JLabel settingsRoutingCascadesDialogKnownCascadesLabel = new JLabel(JAPMessages.getString(
      "settingsRoutingCascadesDialogKnownCascadesLabel"));
    settingsRoutingCascadesDialogKnownCascadesLabel.setFont(getFontSetting());

    JLabel settingsRoutingCascadesDialogAllowedCascadesLabel = new JLabel(JAPMessages.getString(
      "settingsRoutingCascadesDialogAllowedCascadesLabel"));
    settingsRoutingCascadesDialogAllowedCascadesLabel.setFont(getFontSetting());

    TitledBorder settingsRoutingCascadesDialogBorder = new TitledBorder(JAPMessages.getString(
      "settingsRoutingCascadesDialogBorder"));
    settingsRoutingCascadesDialogBorder.setTitleFont(getFontSetting());
    cascadesPanel.setBorder(settingsRoutingCascadesDialogBorder);

    GridBagLayout cascadesPanelLayout = new GridBagLayout();
    cascadesPanel.setLayout(cascadesPanelLayout);

    GridBagConstraints cascadesPanelConstraints = new GridBagConstraints();
    cascadesPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
    cascadesPanelConstraints.fill = GridBagConstraints.NONE;
    cascadesPanelConstraints.weightx = 1.0;
    cascadesPanelConstraints.weighty = 0.0;

    cascadesPanelConstraints.gridx = 0;
    cascadesPanelConstraints.gridy = 0;
    cascadesPanelConstraints.insets = new Insets(0, 0, 0, 5);
    cascadesPanelLayout.setConstraints(settingsRoutingCascadesDialogKnownCascadesLabel,
                       cascadesPanelConstraints);
    cascadesPanel.add(settingsRoutingCascadesDialogKnownCascadesLabel);

    cascadesPanelConstraints.gridx = 1;
    cascadesPanelConstraints.gridy = 0;
    cascadesPanelConstraints.insets = new Insets(0, 5, 0, 0);
    cascadesPanelLayout.setConstraints(settingsRoutingCascadesDialogAllowedCascadesLabel,
                       cascadesPanelConstraints);
    cascadesPanel.add(settingsRoutingCascadesDialogAllowedCascadesLabel);

    cascadesPanelConstraints.gridx = 0;
    cascadesPanelConstraints.gridy = 1;
    cascadesPanelConstraints.weighty = 1.0;
    cascadesPanelConstraints.fill = GridBagConstraints.BOTH;
    cascadesPanelConstraints.insets = new Insets(0, 0, 0, 5);
    cascadesPanelLayout.setConstraints(knownCascadesScrollPane, cascadesPanelConstraints);
    cascadesPanel.add(knownCascadesScrollPane);

    cascadesPanelConstraints.gridx = 1;
    cascadesPanelConstraints.gridy = 1;
    cascadesPanelConstraints.insets = new Insets(0, 5, 0, 0);
    cascadesPanelLayout.setConstraints(allowedCascadesScrollPane, cascadesPanelConstraints);
    cascadesPanel.add(allowedCascadesScrollPane);

    cascadesPanelConstraints.gridx = 0;
    cascadesPanelConstraints.gridy = 2;
    cascadesPanelConstraints.weighty = 0.0;
    cascadesPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
    cascadesPanelConstraints.insets = new Insets(0, 0, 0, 5);
    cascadesPanelLayout.setConstraints(settingsRoutingCascadesDialogAddButton, cascadesPanelConstraints);
    cascadesPanel.add(settingsRoutingCascadesDialogAddButton);

    cascadesPanelConstraints.gridx = 1;
    cascadesPanelConstraints.gridy = 2;
    cascadesPanelConstraints.insets = new Insets(0, 5, 0, 0);
    cascadesPanelLayout.setConstraints(settingsRoutingCascadesDialogRemoveButton,
                       cascadesPanelConstraints);
    cascadesPanel.add(settingsRoutingCascadesDialogRemoveButton);

    cascadesPanelConstraints.gridx = 0;
    cascadesPanelConstraints.gridy = 3;
    cascadesPanelConstraints.gridwidth = 2;
    cascadesPanelConstraints.insets = new Insets(20, 0, 0, 0);
    cascadesPanelLayout.setConstraints(settingsRoutingCascadesDialogReadyButton, cascadesPanelConstraints);
    cascadesPanel.add(settingsRoutingCascadesDialogReadyButton);

    cascadesDialog.align();
    cascadesDialog.show();
  }

  /**
   * Shows the client configuration step 0 (selecting the connection method to the infoservice).
   */
  private void showConfigClientDialogStep0()
  {
    final JAPDialog client0Dialog = new JAPDialog(getRootPanel(),
      JAPMessages.getString("settingsRoutingClientConfigDialog0Title"));
    final JPanel client0Panel = client0Dialog.getRootPanel();

    JButton settingsRoutingClientConfigDialog0InfoServiceButton = new JButton(JAPMessages.getString(
      "settingsRoutingClientConfigDialog0InfoServiceButton"));
    settingsRoutingClientConfigDialog0InfoServiceButton.setFont(getFontSetting());
    settingsRoutingClientConfigDialog0InfoServiceButton.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent event)
      {
        /* if the con infoservice reach button is pressed, set the connection method */
        m_clientConnectionMethod = CLIENT_CONNECTION_DIRECT_TO_INFOSERVICE;
        client0Dialog.hide();
        showConfigClientDialogStep1();
      }
    });

    JButton settingsRoutingClientConfigDialog0MailButton = new JButton(JAPMessages.getString(
      "settingsRoutingClientConfigDialog0MailButton"));
    settingsRoutingClientConfigDialog0MailButton.setFont(getFontSetting());
    settingsRoutingClientConfigDialog0MailButton.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent event)
      {
        /* if the con infoservice reach button is pressed, set the connection method */
        m_clientConnectionMethod = CLIENT_CONNECTION_VIA_MAIL;
        client0Dialog.hide();
        showConfigClientDialogStep1();
      }
    });

    JButton settingsRoutingClientConfigDialog0CancelButton = new JButton(JAPMessages.getString(
      "cancelButton"));
    settingsRoutingClientConfigDialog0CancelButton.setFont(getFontSetting());
    settingsRoutingClientConfigDialog0CancelButton.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent event)
      {
        /* if the Cancel button is pressed, close the dialog */
        client0Dialog.hide();
      }
    });

    TitledBorder settingsRoutingClientConfigDialog0Border = new TitledBorder(JAPMessages.getString(
      "settingsRoutingClientConfigDialog0Border"));
    settingsRoutingClientConfigDialog0Border.setTitleFont(getFontSetting());
    client0Panel.setBorder(settingsRoutingClientConfigDialog0Border);

    GridBagLayout client0PanelLayout = new GridBagLayout();
    client0Panel.setLayout(client0PanelLayout);

    GridBagConstraints client0PanelConstraints = new GridBagConstraints();
    client0PanelConstraints.anchor = GridBagConstraints.NORTH;
    client0PanelConstraints.fill = GridBagConstraints.HORIZONTAL;
    client0PanelConstraints.weightx = 1.0;
    client0PanelConstraints.weighty = 0.0;

    client0PanelConstraints.gridx = 0;
    client0PanelConstraints.gridy = 0;
    client0PanelConstraints.insets = new Insets(0, 5, 5, 5);
    client0PanelLayout.setConstraints(settingsRoutingClientConfigDialog0InfoServiceButton,
                      client0PanelConstraints);
    client0Panel.add(settingsRoutingClientConfigDialog0InfoServiceButton);

    client0PanelConstraints.gridx = 0;
    client0PanelConstraints.gridy = 1;
    client0PanelConstraints.insets = new Insets(0, 5, 20, 5);
    client0PanelLayout.setConstraints(settingsRoutingClientConfigDialog0MailButton,
                      client0PanelConstraints);
    client0Panel.add(settingsRoutingClientConfigDialog0MailButton);

    client0PanelConstraints.gridx = 0;
    client0PanelConstraints.gridy = 2;
    client0PanelConstraints.fill = GridBagConstraints.NONE;
    client0PanelConstraints.weighty = 1.0;
    client0PanelLayout.setConstraints(settingsRoutingClientConfigDialog0CancelButton,
                      client0PanelConstraints);
    client0Panel.add(settingsRoutingClientConfigDialog0CancelButton);

    client0Dialog.align();
    client0Dialog.show();
  }

  /**
   * This method selects the correct connection method to get the forwarder information from
   * the infoservice. The decision is made in dependence on what the user has selected in the
   * step 0.
   */
  private void showConfigClientDialogStep1()
  {
    if (m_clientConnectionMethod == CLIENT_CONNECTION_DIRECT_TO_INFOSERVICE)
    {
      showConfigClientDialogGetForwarderInfo();
    }
    if (m_clientConnectionMethod == CLIENT_CONNECTION_VIA_MAIL)
    {
      showConfigClientDialogStep1Mail();
    }
  }

  /**
   * Shows the get forwarder information from infoservice box in the client configuration dialog.
   */
  private void showConfigClientDialogGetForwarderInfo()
  {
    /* abuse the JAPRoutingSettingsPropagandaThreadLock for synchronization between our two
     * threads
     */
    final JAPRoutingSettingsPropagandaThreadLock lock = new JAPRoutingSettingsPropagandaThreadLock();
    
    /* we can directly connect to the infoservice -> no infoservice forwarding needed */
    JAPModel.getModel().getRoutingSettings().setForwardInfoService(false);

    final JAPDialog infoserviceDialog = new JAPDialog(getRootPanel(), JAPMessages.getString("settingsRoutingClientConfigDialogInfoServiceTitle"));
    infoserviceDialog.disableManualClosing();
    final JPanel infoservicePanel = infoserviceDialog.getRootPanel();

    JLabel settingsRoutingClientConfigDialogInfoServiceLabel = new JLabel(JAPMessages.getString("settingsRoutingClientConfigDialogInfoServiceLabel"));
    settingsRoutingClientConfigDialogInfoServiceLabel.setFont(getFontSetting());
    JLabel busyLabel = new JLabel(JAPUtil.loadImageIcon(JAPConstants.BUSYFN, true));
    JButton settingsRoutingClientConfigDialogInfoServiceCancelButton = new JButton(JAPMessages.getString("cancelButton"));
    settingsRoutingClientConfigDialogInfoServiceCancelButton.setFont(getFontSetting());
    settingsRoutingClientConfigDialogInfoServiceCancelButton.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent event)
      {
        /* if the cancel button is pressed, close the dialog -> the infoservice information
         * is dropped
         */
        boolean weAreFirst = false;
        synchronized (lock) {
          if (lock.isPropagandaThreadReady() == false) {
            /* we are the first -> close the dialog */
            lock.propagandaThreadIsReady();
            weAreFirst = true;
          }
        }
        if (weAreFirst == true) {
          infoserviceDialog.hide();
        }       
      }
    });

    GridBagLayout infoservicePanelLayout = new GridBagLayout();
    infoservicePanel.setLayout(infoservicePanelLayout);

    GridBagConstraints infoservicePanelConstraints = new GridBagConstraints();
    infoservicePanelConstraints.anchor = GridBagConstraints.NORTH;
    infoservicePanelConstraints.fill = GridBagConstraints.NONE;
    infoservicePanelConstraints.weighty = 0.0;

    infoservicePanelConstraints.gridx = 0;
    infoservicePanelConstraints.gridy = 0;
    infoservicePanelConstraints.insets = new Insets(5, 5, 0, 5);
    infoservicePanelLayout.setConstraints(settingsRoutingClientConfigDialogInfoServiceLabel, infoservicePanelConstraints);
    infoservicePanel.add(settingsRoutingClientConfigDialogInfoServiceLabel);

    infoservicePanelConstraints.gridx = 0;
    infoservicePanelConstraints.gridy = 1;
    infoservicePanelConstraints.insets = new Insets(10, 5, 20, 5);
    infoservicePanelLayout.setConstraints(busyLabel, infoservicePanelConstraints);
    infoservicePanel.add(busyLabel);

    infoservicePanelConstraints.gridx = 0;
    infoservicePanelConstraints.gridy = 2;
    infoservicePanelConstraints.insets = new Insets(0, 5, 5, 5);
    infoservicePanelConstraints.weighty = 1.0;
    infoservicePanelLayout.setConstraints(settingsRoutingClientConfigDialogInfoServiceCancelButton, infoservicePanelConstraints);
    infoservicePanel.add(settingsRoutingClientConfigDialogInfoServiceCancelButton);

    infoserviceDialog.align();

    Thread infoserviceThread = new Thread(new JAPThread()
    {
      public void run()
      {
        /* this is the infoservice get forwarder thread */
        /* create a new ForwarderInformationGrabber, which fetches a captcha from the
         * infoservices
         */
        ForwarderInformationGrabber grabber = new ForwarderInformationGrabber();
        boolean weAreFirst = false;
        synchronized (lock) {
          if (lock.isPropagandaThreadReady() == false) {
            /* the users has not pressed cancel until yet */
            lock.propagandaThreadIsReady();
            weAreFirst = true;
          }
        }
        if (weAreFirst == true) {
          if (grabber.getErrorCode() == ForwarderInformationGrabber.RETURN_INFOSERVICE_ERROR) {
            JOptionPane.showMessageDialog(infoservicePanel, JAPMessages.getString("settingsRoutingClientGrabCapchtaInfoServiceError"), JAPMessages.getString("ERROR"), JOptionPane.ERROR_MESSAGE);
            infoserviceDialog.hide();
          }
          if (grabber.getErrorCode() == ForwarderInformationGrabber.RETURN_UNKNOWN_ERROR) {
            JOptionPane.showMessageDialog(infoservicePanel, JAPMessages.getString("settingsRoutingClientGrabCaptchaUnknownError"), JAPMessages.getString("ERROR"), JOptionPane.ERROR_MESSAGE);
            infoserviceDialog.hide();
          }
          if (grabber.getErrorCode() == ForwarderInformationGrabber.RETURN_NO_CAPTCHA_IMPLEMENTATION) {
            JOptionPane.showMessageDialog(infoservicePanel, JAPMessages.getString("settingsRoutingClientGrabCapchtaImplementationError"), JAPMessages.getString("ERROR"), JOptionPane.ERROR_MESSAGE);
            infoserviceDialog.hide();
          }
          if (grabber.getErrorCode() == ForwarderInformationGrabber.RETURN_SUCCESS) {
            /* we are successful -> go to the solve captcha dialog */
            infoserviceDialog.hide();
            showConfigClientDialogCaptcha(grabber.getCaptcha());
          }
        }        
      }
    });
    infoserviceThread.start();

    infoserviceDialog.show();
  }

  /**
   * Shows the "solve captcha" box in the client configuration dialog.
   *
   * @param a_captcha The captcha to solve.
   */
  private void showConfigClientDialogCaptcha(final IImageEncodedCaptcha a_captcha) {
    final JAPDialog captchaDialog = new JAPDialog(getRootPanel(), JAPMessages.getString("settingsRoutingClientConfigDialogCaptchaTitle"));
    final JPanel captchaPanel = captchaDialog.getRootPanel();

    JLabel captchaImageLabel = new JLabel(new ImageIcon(a_captcha.getImage()));

    JLabel settingsRoutingClientConfigDialogCaptchaCharacterSetLabel = new JLabel(JAPMessages.getString("settingsRoutingClientConfigDialogCaptchaCharacterSetLabel") + " " + a_captcha.getCharacterSet());
    settingsRoutingClientConfigDialogCaptchaCharacterSetLabel.setFont(getFontSetting());
    JLabel settingsRoutingClientConfigDialogCaptchaCharacterNumberLabel = new JLabel(JAPMessages.getString("settingsRoutingClientConfigDialogCaptchaCharacterNumberLabel") + " " + Integer.toString(a_captcha.getCharacterNumber()));
    settingsRoutingClientConfigDialogCaptchaCharacterNumberLabel.setFont(getFontSetting());

    JLabel settingsRoutingClientConfigDialogCaptchaInsertCaptchaLabel = new JLabel(JAPMessages.getString("settingsRoutingClientConfigDialogCaptchaInsertCaptchaLabel"));
    settingsRoutingClientConfigDialogCaptchaInsertCaptchaLabel.setFont(getFontSetting());

    final JTextField captchaField = new JTextField();
    captchaField.setFont(getFontSetting());

    JButton settingsRoutingClientConfigDialogCaptchaNextButton = new JButton(JAPMessages.getString("settingsRoutingClientConfigDialogCaptchaNextButton"));
    settingsRoutingClientConfigDialogCaptchaNextButton.setFont(getFontSetting());
    settingsRoutingClientConfigDialogCaptchaNextButton.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent event)
      {
        /* if the Next button is pressed, we try to solve the captcha */
        try
        {
          ListenerInterface forwarder = a_captcha.solveCaptcha(captchaField.getText().trim());
          JAPModel.getModel().getRoutingSettings().setForwarder(forwarder.getHost(), forwarder.getPort());
          /* forwarder was set, try to connect to the forwarder */
          captchaDialog.hide();
          showConfigClientDialogConnectToForwarder();
        }
        catch (Exception e) {
          /* the inserted key is not valid */
          JOptionPane.showMessageDialog(captchaPanel, JAPMessages.getString("settingsRoutingClientConfigDialogCaptchaError"), JAPMessages.getString("ERROR"), JOptionPane.ERROR_MESSAGE);
          captchaField.setText("");
        }
      }
    });

    JButton settingsRoutingClientConfigDialogCaptchaCancelButton = new JButton(JAPMessages.getString("cancelButton"));
    settingsRoutingClientConfigDialogCaptchaCancelButton.setFont(getFontSetting());
    settingsRoutingClientConfigDialogCaptchaCancelButton.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent event)
      {
        /* if the Cancel button is pressed, close the dialog */
        captchaDialog.hide();
      }
    });

    TitledBorder settingsRoutingClientConfigDialogCaptchaBorder = new TitledBorder(JAPMessages.getString("settingsRoutingClientConfigDialogCaptchaBorder"));
    settingsRoutingClientConfigDialogCaptchaBorder.setTitleFont(getFontSetting());
    captchaPanel.setBorder(settingsRoutingClientConfigDialogCaptchaBorder);

    GridBagLayout captchaPanelLayout = new GridBagLayout();
    captchaPanel.setLayout(captchaPanelLayout);

    GridBagConstraints captchaPanelConstraints = new GridBagConstraints();
    captchaPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
    captchaPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
    captchaPanelConstraints.weightx = 1.0;
    captchaPanelConstraints.gridwidth = 2;

    captchaPanelConstraints.gridx = 0;
    captchaPanelConstraints.gridy = 0;
    captchaPanelConstraints.insets = new Insets(5, 5, 0, 5);
    captchaPanelLayout.setConstraints(captchaImageLabel, captchaPanelConstraints);
    captchaPanel.add(captchaImageLabel);

    captchaPanelConstraints.gridx = 0;
    captchaPanelConstraints.gridy = 1;
    captchaPanelConstraints.insets = new Insets(10, 5, 0, 5);
    captchaPanelLayout.setConstraints(settingsRoutingClientConfigDialogCaptchaCharacterSetLabel, captchaPanelConstraints);
    captchaPanel.add(settingsRoutingClientConfigDialogCaptchaCharacterSetLabel);

    captchaPanelConstraints.gridx = 0;
    captchaPanelConstraints.gridy = 2;
    captchaPanelConstraints.insets = new Insets(5, 5, 0, 5);
    captchaPanelLayout.setConstraints(settingsRoutingClientConfigDialogCaptchaCharacterNumberLabel, captchaPanelConstraints);
    captchaPanel.add(settingsRoutingClientConfigDialogCaptchaCharacterNumberLabel);

    captchaPanelConstraints.gridx = 0;
    captchaPanelConstraints.gridy = 3;
    captchaPanelConstraints.insets = new Insets(10, 5, 0, 5);
    captchaPanelLayout.setConstraints(settingsRoutingClientConfigDialogCaptchaInsertCaptchaLabel, captchaPanelConstraints);
    captchaPanel.add(settingsRoutingClientConfigDialogCaptchaInsertCaptchaLabel);

    captchaPanelConstraints.gridx = 0;
    captchaPanelConstraints.gridy = 4;
    captchaPanelConstraints.insets = new Insets(0, 5, 0, 5);
    captchaPanelLayout.setConstraints(captchaField, captchaPanelConstraints);
    captchaPanel.add(captchaField);

    captchaPanelConstraints.gridx = 0;
    captchaPanelConstraints.gridy = 5;
    captchaPanelConstraints.gridwidth = 1;
    captchaPanelConstraints.weighty = 1.0;
    captchaPanelConstraints.insets = new Insets(20, 5, 5, 5);
    captchaPanelLayout.setConstraints(settingsRoutingClientConfigDialogCaptchaCancelButton, captchaPanelConstraints);
    captchaPanel.add(settingsRoutingClientConfigDialogCaptchaCancelButton);

    captchaPanelConstraints.gridx = 1;
    captchaPanelConstraints.gridy = 5;
    captchaPanelConstraints.insets = new Insets(20, 5, 5, 5);
    captchaPanelLayout.setConstraints(settingsRoutingClientConfigDialogCaptchaNextButton, captchaPanelConstraints);
    captchaPanel.add(settingsRoutingClientConfigDialogCaptchaNextButton);

    captchaDialog.align();
    captchaDialog.show();
  }
    

  /**
   * Shows the first step of the client configuration dialog. This is the way to connect via
   * the information directly got from the infoservice.
   */
  private void showConfigClientDialogStep1InfoService()
  {
    final JAPDialog client1IsDialog = new JAPDialog(getRootPanel(),
      JAPMessages.getString("settingsRoutingClientConfigDialog1IsTitle"));
    final JPanel client1IsPanel = client1IsDialog.getRootPanel();

    JLabel settingsRoutingClientConfigDialog1IsHostLabel = new JLabel(JAPMessages.getString(
      "settingsRoutingClientConfigDialog1IsHostLabel"));
    settingsRoutingClientConfigDialog1IsHostLabel.setFont(getFontSetting());
    final JTextField settingsRoutingHostField = new JTextField();
    settingsRoutingHostField.setFont(getFontSetting());

    JLabel settingsRoutingClientConfigDialog1IsPortLabel = new JLabel(JAPMessages.getString(
      "settingsRoutingClientConfigDialog1IsPortLabel"));
    settingsRoutingClientConfigDialog1IsPortLabel.setFont(getFontSetting());
    final JAPJIntField settingsRoutingPortField = new JAPJIntField();
    settingsRoutingPortField.setFont(getFontSetting());

    JButton settingsRoutingClientConfigDialog1IsNextButton = new JButton(JAPMessages.getString(
      "settingsRoutingClientConfigDialog1IsNextButton"));
    settingsRoutingClientConfigDialog1IsNextButton.setFont(getFontSetting());
    settingsRoutingClientConfigDialog1IsNextButton.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent event)
      {
        /* if the Next button is pressed, we try to connect to the forwarder and get the
         * connection offer
         */
        int port = -1;
        try
        {
          port = Integer.parseInt(settingsRoutingPortField.getText().trim());
          JAPModel.getModel().getRoutingSettings().setForwarder(settingsRoutingHostField.getText(),
            port);
          /* we can directly connect to the infoservice -> no infoservice forwarding needed */
          JAPModel.getModel().getRoutingSettings().setForwardInfoService(false);
          /* forwarder was set, try to connect to the forwarder */
          client1IsDialog.hide();
          showConfigClientDialogConnectToForwarder();
        }
        catch (NumberFormatException e)
        {
          JOptionPane.showMessageDialog(client1IsPanel,
                          JAPMessages.getString("settingsRoutingClientConfigDialog1IsPortError"),
                          JAPMessages.getString("ERROR"), JOptionPane.ERROR_MESSAGE);
        }
      }
    });

    JButton settingsRoutingClientConfigDialog1IsCancelButton = new JButton(JAPMessages.getString(
      "cancelButton"));
    settingsRoutingClientConfigDialog1IsCancelButton.setFont(getFontSetting());
    settingsRoutingClientConfigDialog1IsCancelButton.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent event)
      {
        /* if the Cancel button is pressed, close the dialog */
        client1IsDialog.hide();
      }
    });

    TitledBorder settingsRoutingClientConfigDialog1IsBorder = new TitledBorder(JAPMessages.getString(
      "settingsRoutingClientConfigDialog1IsBorder"));
    settingsRoutingClientConfigDialog1IsBorder.setTitleFont(getFontSetting());
    client1IsPanel.setBorder(settingsRoutingClientConfigDialog1IsBorder);

    GridBagLayout client1IsPanelLayout = new GridBagLayout();
    client1IsPanel.setLayout(client1IsPanelLayout);

    GridBagConstraints client1IsPanelConstraints = new GridBagConstraints();
    client1IsPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
    client1IsPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
    client1IsPanelConstraints.weightx = 1.0;
    client1IsPanelConstraints.gridwidth = 2;

    client1IsPanelConstraints.gridx = 0;
    client1IsPanelConstraints.gridy = 0;
    client1IsPanelLayout.setConstraints(settingsRoutingClientConfigDialog1IsHostLabel,
                      client1IsPanelConstraints);
    client1IsPanel.add(settingsRoutingClientConfigDialog1IsHostLabel);

    client1IsPanelConstraints.gridx = 0;
    client1IsPanelConstraints.gridy = 1;
    client1IsPanelConstraints.insets = new Insets(0, 0, 10, 0);
    client1IsPanelLayout.setConstraints(settingsRoutingHostField, client1IsPanelConstraints);
    client1IsPanel.add(settingsRoutingHostField);

    client1IsPanelConstraints.gridx = 0;
    client1IsPanelConstraints.gridy = 2;
    client1IsPanelConstraints.insets = new Insets(0, 0, 0, 0);
    client1IsPanelLayout.setConstraints(settingsRoutingClientConfigDialog1IsPortLabel,
                      client1IsPanelConstraints);
    client1IsPanel.add(settingsRoutingClientConfigDialog1IsPortLabel);

    client1IsPanelConstraints.gridx = 0;
    client1IsPanelConstraints.gridy = 3;
    client1IsPanelConstraints.insets = new Insets(0, 0, 20, 0);
    client1IsPanelLayout.setConstraints(settingsRoutingPortField, client1IsPanelConstraints);
    client1IsPanel.add(settingsRoutingPortField);

    client1IsPanelConstraints.gridx = 0;
    client1IsPanelConstraints.gridy = 4;
    client1IsPanelConstraints.gridwidth = 1;
    client1IsPanelConstraints.weighty = 1.0;
    client1IsPanelConstraints.insets = new Insets(0, 0, 0, 5);
    client1IsPanelLayout.setConstraints(settingsRoutingClientConfigDialog1IsCancelButton,
                      client1IsPanelConstraints);
    client1IsPanel.add(settingsRoutingClientConfigDialog1IsCancelButton);

    client1IsPanelConstraints.gridx = 1;
    client1IsPanelConstraints.gridy = 4;
    client1IsPanelConstraints.insets = new Insets(0, 5, 0, 0);
    client1IsPanelLayout.setConstraints(settingsRoutingClientConfigDialog1IsNextButton,
                      client1IsPanelConstraints);
    client1IsPanel.add(settingsRoutingClientConfigDialog1IsNextButton);

    client1IsDialog.align();
    client1IsDialog.show();
  }

  /**
   * Shows the first step of the client configuration dialog. This is the way to connect via
   * the information from the mail system.
   */
  private void showConfigClientDialogStep1Mail()
  {
    final JAPDialog client1MailDialog = new JAPDialog(getRootPanel(),
      JAPMessages.getString("settingsRoutingClientConfigDialog1MailTitle"));
    final JPanel client1MailPanel = client1MailDialog.getRootPanel();

    JLabel settingsRoutingClientConfigDialog1MailHostLabel = new JLabel(JAPMessages.getString(
      "settingsRoutingClientConfigDialog1MailHostLabel"));
    settingsRoutingClientConfigDialog1MailHostLabel.setFont(getFontSetting());
    final JTextField settingsRoutingHostField = new JTextField();
    settingsRoutingHostField.setFont(getFontSetting());

    JLabel settingsRoutingClientConfigDialog1MailPortLabel = new JLabel(JAPMessages.getString(
      "settingsRoutingClientConfigDialog1MailPortLabel"));
    settingsRoutingClientConfigDialog1MailPortLabel.setFont(getFontSetting());
    final JAPJIntField settingsRoutingPortField = new JAPJIntField();
    settingsRoutingPortField.setFont(getFontSetting());

    JButton settingsRoutingClientConfigDialog1MailNextButton = new JButton(JAPMessages.getString(
      "settingsRoutingClientConfigDialog1MailNextButton"));
    settingsRoutingClientConfigDialog1MailNextButton.setFont(getFontSetting());
    settingsRoutingClientConfigDialog1MailNextButton.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent event)
      {
        /* if the Next button is pressed, we try to connect to the forwarder and get the
         * connection offer
         */
        int port = -1;
        try
        {
          port = Integer.parseInt(settingsRoutingPortField.getText().trim());
          JAPModel.getModel().getRoutingSettings().setForwarder(settingsRoutingHostField.getText(),
            port);
          /* we can't reach the infoservice directly -> we need infoservice forwarding */
          JAPModel.getModel().getRoutingSettings().setForwardInfoService(true);
          /* forwarder was set, try to connect to the forwarder */
          client1MailDialog.hide();
          showConfigClientDialogConnectToForwarder();
        }
        catch (NumberFormatException e)
        {
          JOptionPane.showMessageDialog(client1MailPanel,
                          JAPMessages.getString("settingsRoutingClientConfigDialog1MailPortError"),
                          JAPMessages.getString("ERROR"), JOptionPane.ERROR_MESSAGE);
        }
      }
    });

    JButton settingsRoutingClientConfigDialog1MailCancelButton = new JButton(JAPMessages.getString(
      "cancelButton"));
    settingsRoutingClientConfigDialog1MailCancelButton.setFont(getFontSetting());
    settingsRoutingClientConfigDialog1MailCancelButton.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent event)
      {
        /* if the Cancel button is pressed, close the dialog */
        client1MailDialog.hide();
      }
    });

    TitledBorder settingsRoutingClientConfigDialog1MailBorder = new TitledBorder(JAPMessages.getString(
      "settingsRoutingClientConfigDialog1MailBorder"));
    settingsRoutingClientConfigDialog1MailBorder.setTitleFont(getFontSetting());
    client1MailPanel.setBorder(settingsRoutingClientConfigDialog1MailBorder);

    GridBagLayout client1MailPanelLayout = new GridBagLayout();
    client1MailPanel.setLayout(client1MailPanelLayout);

    GridBagConstraints client1MailPanelConstraints = new GridBagConstraints();
    client1MailPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
    client1MailPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
    client1MailPanelConstraints.weightx = 1.0;
    client1MailPanelConstraints.gridwidth = 2;

    client1MailPanelConstraints.gridx = 0;
    client1MailPanelConstraints.gridy = 0;
    client1MailPanelLayout.setConstraints(settingsRoutingClientConfigDialog1MailHostLabel,
                        client1MailPanelConstraints);
    client1MailPanel.add(settingsRoutingClientConfigDialog1MailHostLabel);

    client1MailPanelConstraints.gridx = 0;
    client1MailPanelConstraints.gridy = 1;
    client1MailPanelConstraints.insets = new Insets(0, 0, 10, 0);
    client1MailPanelLayout.setConstraints(settingsRoutingHostField, client1MailPanelConstraints);
    client1MailPanel.add(settingsRoutingHostField);

    client1MailPanelConstraints.gridx = 0;
    client1MailPanelConstraints.gridy = 2;
    client1MailPanelConstraints.insets = new Insets(0, 0, 0, 0);
    client1MailPanelLayout.setConstraints(settingsRoutingClientConfigDialog1MailPortLabel,
                        client1MailPanelConstraints);
    client1MailPanel.add(settingsRoutingClientConfigDialog1MailPortLabel);

    client1MailPanelConstraints.gridx = 0;
    client1MailPanelConstraints.gridy = 3;
    client1MailPanelConstraints.insets = new Insets(0, 0, 20, 0);
    client1MailPanelLayout.setConstraints(settingsRoutingPortField, client1MailPanelConstraints);
    client1MailPanel.add(settingsRoutingPortField);

    client1MailPanelConstraints.gridx = 0;
    client1MailPanelConstraints.gridy = 4;
    client1MailPanelConstraints.gridwidth = 1;
    client1MailPanelConstraints.weighty = 1.0;
    client1MailPanelConstraints.insets = new Insets(0, 0, 0, 5);
    client1MailPanelLayout.setConstraints(settingsRoutingClientConfigDialog1MailCancelButton,
                        client1MailPanelConstraints);
    client1MailPanel.add(settingsRoutingClientConfigDialog1MailCancelButton);

    client1MailPanelConstraints.gridx = 1;
    client1MailPanelConstraints.gridy = 4;
    client1MailPanelConstraints.insets = new Insets(0, 5, 0, 0);
    client1MailPanelLayout.setConstraints(settingsRoutingClientConfigDialog1MailNextButton,
                        client1MailPanelConstraints);
    client1MailPanel.add(settingsRoutingClientConfigDialog1MailNextButton);

    client1MailDialog.align();
    client1MailDialog.show();
  }

  /**
   * Shows the connect to forwarder box in the client configuration dialog.
   */
  private void showConfigClientDialogConnectToForwarder()
  {
    final JAPDialog connectDialog = new JAPDialog(getRootPanel(), JAPMessages.getString("settingsRoutingClientConfigConnectToForwarderTitle"));
    connectDialog.disableManualClosing();
    final JPanel connectPanel = connectDialog.getRootPanel();

    JLabel settingsRoutingClientConfigDialogConnectToForwarderInfoLabel = new JLabel(JAPMessages.getString("settingsRoutingClientConfigDialogConnectToForwarderInfoLabel") + " " + JAPModel.getModel().getRoutingSettings().getForwarderString());
    settingsRoutingClientConfigDialogConnectToForwarderInfoLabel.setFont(getFontSetting());
    JLabel settingsRoutingClientConfigDialogConnectToForwarderLabel = new JLabel(JAPMessages.getString("settingsRoutingClientConfigDialogConnectToForwarderLabel"));
    settingsRoutingClientConfigDialogConnectToForwarderLabel.setFont(getFontSetting());
    JLabel busyLabel = new JLabel(JAPUtil.loadImageIcon(JAPConstants.BUSYFN, true));

    GridBagLayout connectPanelLayout = new GridBagLayout();
    connectPanel.setLayout(connectPanelLayout);

    GridBagConstraints connectPanelConstraints = new GridBagConstraints();
    connectPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
    connectPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
    connectPanelConstraints.weighty = 0.0;

    connectPanelConstraints.gridx = 0;
    connectPanelConstraints.gridy = 0;
    connectPanelConstraints.insets = new Insets(5, 5, 0, 5);
    connectPanelLayout.setConstraints(settingsRoutingClientConfigDialogConnectToForwarderLabel, connectPanelConstraints);
    connectPanel.add(settingsRoutingClientConfigDialogConnectToForwarderLabel);

    connectPanelConstraints.gridx = 0;
    connectPanelConstraints.gridy = 1;
    connectPanelConstraints.insets = new Insets(10, 5, 0, 5);
    connectPanelLayout.setConstraints(settingsRoutingClientConfigDialogConnectToForwarderInfoLabel, connectPanelConstraints);
    connectPanel.add(settingsRoutingClientConfigDialogConnectToForwarderInfoLabel);

    connectPanelConstraints.gridx = 0;
    connectPanelConstraints.gridy = 2;
    connectPanelConstraints.anchor = GridBagConstraints.NORTH;
    connectPanelConstraints.weighty = 1.0;
    connectPanelConstraints.insets = new Insets(20, 5, 10, 5);
    connectPanelLayout.setConstraints(busyLabel, connectPanelConstraints);
    connectPanel.add(busyLabel);

    connectDialog.align();

    Thread connectThread = new Thread(new JAPThread()
    {
      public void run()
      {
        /* this is the connect to forwarder thread */
        /* the forwarder is already set, we only need to connect to */
        if (JAPModel.getModel().getRoutingSettings().setRoutingMode(JAPRoutingSettings.
          ROUTING_MODE_CLIENT) == false)
        {
          /* error while connecting, show a message and go back to step 1 */
          JOptionPane.showMessageDialog(connectPanel,
                          JAPMessages.getString("settingsRoutingClientConfigConnectToForwarderError"),
                          JAPMessages.getString("ERROR"), JOptionPane.ERROR_MESSAGE);
          connectDialog.hide();
          showConfigClientDialogStep1();
        }
        else
        {
          /* connection successful */
          connectDialog.hide();
          showConfigClientDialogGetOffer();
        }
      }
    });
    connectThread.start();

    connectDialog.show();
  }

  /**
   * Shows the get connection offer box in the client configuration dialog.
   */
  private void showConfigClientDialogGetOffer()
  {
    final JAPDialog offerDialog = new JAPDialog(getRootPanel(),
      JAPMessages.getString("settingsRoutingClientConfigGetOfferTitle"));
    offerDialog.disableManualClosing();
    final JPanel offerPanel = offerDialog.getRootPanel();

    JLabel settingsRoutingClientConfigDialogGetOfferLabel = new JLabel(JAPMessages.getString(
      "settingsRoutingClientConfigDialogGetOfferLabel"));
    settingsRoutingClientConfigDialogGetOfferLabel.setFont(getFontSetting());
    JLabel busyLabel = new JLabel(JAPUtil.loadImageIcon(JAPConstants.BUSYFN, true));
    JButton settingsRoutingClientConfigDialogGetOfferCancelButton = new JButton(JAPMessages.getString(
      "cancelButton"));
    settingsRoutingClientConfigDialogGetOfferCancelButton.setFont(getFontSetting());
    settingsRoutingClientConfigDialogGetOfferCancelButton.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent event)
      {
        /* if the Cancel button is pressed, stop the connection -> the getConnectionDescriptor()
         * method ends with an exception
         */
        JAPModel.getModel().getRoutingSettings().setRoutingMode(JAPRoutingSettings.ROUTING_MODE_DISABLED);
      }
    });

    GridBagLayout offerPanelLayout = new GridBagLayout();
    offerPanel.setLayout(offerPanelLayout);

    GridBagConstraints offerPanelConstraints = new GridBagConstraints();
    offerPanelConstraints.anchor = GridBagConstraints.NORTH;
    offerPanelConstraints.fill = GridBagConstraints.NONE;
    offerPanelConstraints.weighty = 0.0;

    offerPanelConstraints.gridx = 0;
    offerPanelConstraints.gridy = 0;
    offerPanelConstraints.insets = new Insets(5, 5, 0, 5);
    offerPanelLayout.setConstraints(settingsRoutingClientConfigDialogGetOfferLabel, offerPanelConstraints);
    offerPanel.add(settingsRoutingClientConfigDialogGetOfferLabel);

    offerPanelConstraints.gridx = 0;
    offerPanelConstraints.gridy = 1;
    offerPanelConstraints.insets = new Insets(10, 5, 20, 5);
    offerPanelLayout.setConstraints(busyLabel, offerPanelConstraints);
    offerPanel.add(busyLabel);

    offerPanelConstraints.gridx = 0;
    offerPanelConstraints.gridy = 2;
    offerPanelConstraints.insets = new Insets(0, 5, 5, 5);
    offerPanelConstraints.weighty = 1.0;
    offerPanelLayout.setConstraints(settingsRoutingClientConfigDialogGetOfferCancelButton,
                    offerPanelConstraints);
    offerPanel.add(settingsRoutingClientConfigDialogGetOfferCancelButton);

    offerDialog.align();

    Thread offerThread = new Thread(new JAPThread()
    {
      public void run()
      {
        /* this is the get connection offer thread */
        ForwardConnectionDescriptor connectionDescriptor = null;
        try
        {
          JAPCertificateStore certificateStore = null;
          if (JAPModel.isCertCheckDisabled() == false)
          {
            certificateStore = JAPModel.getCertificateStore();
          }
          connectionDescriptor = JAPModel.getModel().getRoutingSettings().getConnectionDescriptor(
            certificateStore);
        }
        catch (ClientForwardException e)
        {
          /* there was an error while receiving the connection offer */
          LogHolder.log(LogLevel.ERR, LogType.NET,
                  "JAPConfRouting: showConfigClientDialogGetOffer: " + e.toString());
          if (e.getErrorCode() == ClientForwardException.ERROR_CONNECTION_ERROR)
          {
            JOptionPane.showMessageDialog(offerPanel,
              JAPMessages.getString("settingsRoutingClientGetOfferConnectError"),
              JAPMessages.getString("ERROR"), JOptionPane.ERROR_MESSAGE);
          }
          else
          {
            if (e.getErrorCode() == ClientForwardException.ERROR_VERSION_ERROR)
            {
              JOptionPane.showMessageDialog(offerPanel,
                JAPMessages.getString("settingsRoutingClientGetOfferVersionError"),
                JAPMessages.getString("ERROR"), JOptionPane.ERROR_MESSAGE);
            }
            else
            {
              JOptionPane.showMessageDialog(offerPanel,
                JAPMessages.getString("settingsRoutingClientGetOfferUnknownError"),
                JAPMessages.getString("ERROR"), JOptionPane.ERROR_MESSAGE);
            }
          }
          offerDialog.hide();
          showConfigClientDialogStep1();
        }
        if (connectionDescriptor != null)
        {
          /* receiving connection offer was successful */
          offerDialog.hide();
          showConfigClientDialogStep2(connectionDescriptor);
        }
      }
    });
    offerThread.start();

    offerDialog.show();
  }

  /**
   * Shows the second step of the client configuration dialog.
   *
   * @param a_connectionDescriptor The connection offer from the forwarder, which is visualized.
   */
  private void showConfigClientDialogStep2(ForwardConnectionDescriptor a_connectionDescriptor)
  {
    final JAPDialog client2Dialog = new JAPDialog(getRootPanel(),
      JAPMessages.getString("settingsRoutingClientConfigDialog2Title"));
    client2Dialog.disableManualClosing();
    JPanel client2Panel = client2Dialog.getRootPanel();

    JLabel settingsRoutingClientConfigDialog2GuaranteedBandwidthLabel = new JLabel(JAPMessages.getString(
      "settingsRoutingClientConfigDialog2GuaranteedBandwidthLabel") + " " +
      Integer.toString(a_connectionDescriptor.getGuaranteedBandwidth()));
    settingsRoutingClientConfigDialog2GuaranteedBandwidthLabel.setFont(getFontSetting());

    JLabel settingsRoutingClientConfigDialog2MaxBandwidthLabel = new JLabel(JAPMessages.getString(
      "settingsRoutingClientConfigDialog2MaxBandwidthLabel") + " " +
      Integer.toString(a_connectionDescriptor.getMaximumBandwidth()));
    settingsRoutingClientConfigDialog2MaxBandwidthLabel.setFont(getFontSetting());

    JLabel settingsRoutingClientConfigDialog2DummyTrafficLabel = new JLabel();
    settingsRoutingClientConfigDialog2DummyTrafficLabel.setFont(getFontSetting());
    if (a_connectionDescriptor.getMinDummyTrafficInterval() != -1)
    {
      settingsRoutingClientConfigDialog2DummyTrafficLabel.setText(JAPMessages.getString(
        "settingsRoutingClientConfigDialog2DummyTrafficLabel") + " " +
        Integer.toString(a_connectionDescriptor.getMinDummyTrafficInterval() / 1000));
    }
    else
    {
      settingsRoutingClientConfigDialog2DummyTrafficLabel.setText(JAPMessages.getString(
        "settingsRoutingClientConfigDialog2DummyTrafficLabel") + " " +
        JAPMessages.getString("settingsRoutingClientConfigDialog2DummyTrafficLabelNoNeed"));
    }

    final JButton settingsRoutingClientConfigDialog2FinishButton = new JButton(JAPMessages.getString(
      "settingsRoutingClientConfigDialog2FinishButton"));

    JLabel settingsRoutingClientConfigDialog2MixCascadesLabel = new JLabel(JAPMessages.getString(
      "settingsRoutingClientConfigDialog2MixCascadesLabel"));
    settingsRoutingClientConfigDialog2MixCascadesLabel.setFont(getFontSetting());
    final JList supportedCascadesList = new JList(a_connectionDescriptor.getMixCascadeList());
    supportedCascadesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    supportedCascadesList.addListSelectionListener(new ListSelectionListener()
    {
      public void valueChanged(ListSelectionEvent event)
      {
        /* if there is something selected, we can enable the Finish button */
        if (supportedCascadesList.getSelectedIndex() != -1)
        {
          settingsRoutingClientConfigDialog2FinishButton.setEnabled(true);
        }
        else
        {
          settingsRoutingClientConfigDialog2FinishButton.setEnabled(false);
        }
      }
    });
    JScrollPane supportedCascadesScrollPane = new JScrollPane(supportedCascadesList);
    supportedCascadesScrollPane.setFont(getFontSetting());

    settingsRoutingClientConfigDialog2FinishButton.setFont(getFontSetting());
    if (supportedCascadesList.getSelectedIndex() != -1)
    {
      settingsRoutingClientConfigDialog2FinishButton.setEnabled(true);
    }
    else
    {
      settingsRoutingClientConfigDialog2FinishButton.setEnabled(false);
    }
    settingsRoutingClientConfigDialog2FinishButton.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent event)
      {
        /* if the Finish button is pressed, we start the submit mixcascade step */
        client2Dialog.hide();
        showConfigClientDialogAnnounceCascade( (MixCascade) (supportedCascadesList.getSelectedValue()));
      }
    });

    JButton settingsRoutingClientConfigDialog2CancelButton = new JButton(JAPMessages.getString(
      "cancelButton"));
    settingsRoutingClientConfigDialog2CancelButton.setFont(getFontSetting());
    settingsRoutingClientConfigDialog2CancelButton.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent event)
      {
        /* if the Cancel button is pressed, stop routing and close the dialog */
        JAPModel.getModel().getRoutingSettings().setRoutingMode(JAPRoutingSettings.
          ROUTING_MODE_DISABLED);
        client2Dialog.hide();
        /* let the user enter another forwarder, maybe he gets a better one */
        showConfigClientDialogStep1();
      }
    });

    TitledBorder settingsRoutingClientConfigDialog2Border = new TitledBorder(JAPMessages.getString(
      "settingsRoutingClientConfigDialog2Border"));
    settingsRoutingClientConfigDialog2Border.setTitleFont(getFontSetting());
    client2Panel.setBorder(settingsRoutingClientConfigDialog2Border);

    GridBagLayout client2PanelLayout = new GridBagLayout();
    client2Panel.setLayout(client2PanelLayout);

    GridBagConstraints client2PanelConstraints = new GridBagConstraints();
    client2PanelConstraints.anchor = GridBagConstraints.NORTHWEST;
    client2PanelConstraints.fill = GridBagConstraints.BOTH;
    client2PanelConstraints.weightx = 1.0;
    client2PanelConstraints.weighty = 0.0;
    client2PanelConstraints.gridwidth = 2;

    client2PanelConstraints.gridx = 0;
    client2PanelConstraints.gridy = 0;
    client2PanelConstraints.insets = new Insets(0, 5, 10, 5);
    client2PanelLayout.setConstraints(settingsRoutingClientConfigDialog2GuaranteedBandwidthLabel,
                      client2PanelConstraints);
    client2Panel.add(settingsRoutingClientConfigDialog2GuaranteedBandwidthLabel);

    client2PanelConstraints.gridx = 0;
    client2PanelConstraints.gridy = 1;
    client2PanelLayout.setConstraints(settingsRoutingClientConfigDialog2MaxBandwidthLabel,
                      client2PanelConstraints);
    client2Panel.add(settingsRoutingClientConfigDialog2MaxBandwidthLabel);

    client2PanelConstraints.gridx = 0;
    client2PanelConstraints.gridy = 2;
    client2PanelLayout.setConstraints(settingsRoutingClientConfigDialog2DummyTrafficLabel,
                      client2PanelConstraints);
    client2Panel.add(settingsRoutingClientConfigDialog2DummyTrafficLabel);

    client2PanelConstraints.gridx = 0;
    client2PanelConstraints.gridy = 3;
    client2PanelConstraints.insets = new Insets(0, 5, 0, 5);
    client2PanelLayout.setConstraints(settingsRoutingClientConfigDialog2MixCascadesLabel,
                      client2PanelConstraints);
    client2Panel.add(settingsRoutingClientConfigDialog2MixCascadesLabel);

    client2PanelConstraints.gridx = 0;
    client2PanelConstraints.gridy = 4;
    client2PanelConstraints.weighty = 1.0;
    client2PanelConstraints.insets = new Insets(0, 5, 20, 5);
    client2PanelLayout.setConstraints(supportedCascadesScrollPane, client2PanelConstraints);
    client2Panel.add(supportedCascadesScrollPane);

    client2PanelConstraints.gridx = 0;
    client2PanelConstraints.gridy = 5;
    client2PanelConstraints.gridwidth = 1;
    client2PanelConstraints.weighty = 0.0;
    client2PanelConstraints.insets = new Insets(0, 5, 5, 5);
    client2PanelLayout.setConstraints(settingsRoutingClientConfigDialog2CancelButton,
                      client2PanelConstraints);
    client2Panel.add(settingsRoutingClientConfigDialog2CancelButton);

    client2PanelConstraints.gridx = 1;
    client2PanelConstraints.gridy = 5;
    client2PanelConstraints.insets = new Insets(0, 5, 5, 5);
    client2PanelLayout.setConstraints(settingsRoutingClientConfigDialog2FinishButton,
                      client2PanelConstraints);
    client2Panel.add(settingsRoutingClientConfigDialog2FinishButton);

    client2Dialog.align();
    client2Dialog.show();
  }

  /**
   * Shows the announce selected mixcascade box in the client configuration dialog.
   *
   * @param a_selectedMixCascade The mixcascade which was selected in step 2.
   */
  private void showConfigClientDialogAnnounceCascade(final MixCascade a_selectedMixCascade)
  {
    final JAPDialog announceDialog = new JAPDialog(getRootPanel(),
      JAPMessages.getString("settingsRoutingClientConfigAnnounceCascadeTitle"));
    announceDialog.disableManualClosing();
    final JPanel announcePanel = announceDialog.getRootPanel();

    JLabel settingsRoutingClientConfigDialogAnnounceCascadeLabel = new JLabel(JAPMessages.getString(
      "settingsRoutingClientConfigDialogAnnounceCascadeLabel"));
    settingsRoutingClientConfigDialogAnnounceCascadeLabel.setFont(getFontSetting());
    JLabel busyLabel = new JLabel(JAPUtil.loadImageIcon(JAPConstants.BUSYFN, true));
    JButton settingsRoutingClientConfigDialogAnnounceCascadeCancelButton = new JButton(JAPMessages.
      getString("cancelButton"));
    settingsRoutingClientConfigDialogAnnounceCascadeCancelButton.setFont(getFontSetting());
    settingsRoutingClientConfigDialogAnnounceCascadeCancelButton.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent event)
      {
        /* if the Cancel button is pressed, stop the connection -> the selectMixCascade()
         * method ends with an exception
         */
        JAPModel.getModel().getRoutingSettings().setRoutingMode(JAPRoutingSettings.
          ROUTING_MODE_DISABLED);
      }
    });

    GridBagLayout announcePanelLayout = new GridBagLayout();
    announcePanel.setLayout(announcePanelLayout);

    GridBagConstraints announcePanelConstraints = new GridBagConstraints();
    announcePanelConstraints.anchor = GridBagConstraints.NORTH;
    announcePanelConstraints.fill = GridBagConstraints.NONE;
    announcePanelConstraints.weighty = 0.0;

    announcePanelConstraints.gridx = 0;
    announcePanelConstraints.gridy = 0;
    announcePanelConstraints.insets = new Insets(5, 5, 0, 5);
    announcePanelLayout.setConstraints(settingsRoutingClientConfigDialogAnnounceCascadeLabel,
                       announcePanelConstraints);
    announcePanel.add(settingsRoutingClientConfigDialogAnnounceCascadeLabel);

    announcePanelConstraints.gridx = 0;
    announcePanelConstraints.gridy = 1;
    announcePanelConstraints.insets = new Insets(10, 5, 20, 5);
    announcePanelLayout.setConstraints(busyLabel, announcePanelConstraints);
    announcePanel.add(busyLabel);

    announcePanelConstraints.gridx = 0;
    announcePanelConstraints.gridy = 2;
    announcePanelConstraints.insets = new Insets(0, 5, 5, 5);
    announcePanelConstraints.weighty = 1.0;
    announcePanelLayout.setConstraints(settingsRoutingClientConfigDialogAnnounceCascadeCancelButton,
                       announcePanelConstraints);
    announcePanel.add(settingsRoutingClientConfigDialogAnnounceCascadeCancelButton);

    announceDialog.align();

    Thread announceThread = new Thread(new JAPThread()
    {
      public void run()
      {
        /* this is the announce mixcascade thread */
        try
        {
          JAPModel.getModel().getRoutingSettings().selectMixCascade(a_selectedMixCascade);
          /* if sending the mixcascade was successful, start the anonymous mode */
          JAPController.getController().setCurrentMixCascade(a_selectedMixCascade);
          JAPController.getController().setAnonMode(true);
          /* finish the client configuration dialog */
          announceDialog.hide();
        }
        catch (ClientForwardException e)
        {
          /* there was an error while receiving the connection offer */
          LogHolder.log(LogLevel.ERR, LogType.NET,
                  "JAPConfRouting: showConfigClientDialogAnnounceCascade: " + e.toString());
          if (e.getErrorCode() == ClientForwardException.ERROR_CONNECTION_ERROR)
          {
            JOptionPane.showMessageDialog(announcePanel,
              JAPMessages.getString("settingsRoutingClientAnnounceCascadeConnectError"),
              JAPMessages.getString("ERROR"), JOptionPane.ERROR_MESSAGE);
          }
          else
          {
            JOptionPane.showMessageDialog(announcePanel,
              JAPMessages.getString("settingsRoutingClientAnnounceCascadeUnknownError"),
              JAPMessages.getString("ERROR"), JOptionPane.ERROR_MESSAGE);
          }
          announceDialog.hide();
          showConfigClientDialogStep1();
        }
      }
    });
    announceThread.start();

    announceDialog.show();
  }

  /**
   * Updates the label with the server port after changes of the port.
   */
  private void updateServerPortLabel()
  {
    m_settingsRoutingServerPortLabel.setText(JAPMessages.getString("settingsRoutingServerPortLabel") +
                         " " +
                         Integer.toString(JAPModel.getModel().getRoutingSettings().
      getServerPort()));
  }

  /**
   * Updates the label, which shows the current bandwidth limitation.
   */
  private void updateBandwidthLabel()
  {
    m_settingsRoutingBandwidthLabel.setText(JAPMessages.getString("settingsRoutingBandwidthLabel") + " " +
                        Integer.toString(JAPModel.getModel().getRoutingSettings().
      getBandwidth() / 1024) + " " + JAPMessages.getString("settingsRoutingBandwidthLabelPart2"));
  }

  /**
   * Updates the slider with the bandwidth for forwarding. The label with that bandwidth is also
   * updated.
   */
  private void updateBandwidthSlider()
  {
    m_settingsRoutingBandwidthSlider.setMaximum(JAPModel.getModel().getRoutingSettings().getMaxBandwidth() /
      1024);
    m_settingsRoutingBandwidthSlider.setValue(JAPModel.getModel().getRoutingSettings().getBandwidth() /
                          1024);
    m_settingsRoutingBandwidthSlider.setMajorTickSpacing(Math.max(1,
      m_settingsRoutingBandwidthSlider.getMaximum() / 5));
    m_settingsRoutingBandwidthSlider.setMinorTickSpacing(Math.max(1,
      m_settingsRoutingBandwidthSlider.getMaximum() / 20));
    m_settingsRoutingBandwidthSlider.setLabelTable(m_settingsRoutingBandwidthSlider.createStandardLabels(
      m_settingsRoutingBandwidthSlider.getMajorTickSpacing(), 0));
    /* update the label */
    updateBandwidthLabel();
  }

  /**
   * Updates the label with the number of simultaneously forwarded connections.
   */
  private void updateUserLabel()
  {
    m_settingsRoutingUserLabel.setText(JAPMessages.getString("settingsRoutingUserLabel") + " " +
                       JAPModel.getModel().getRoutingSettings().getAllowedConnections());
  }

  /**
   * Updates the slider with the number of simultaneously forwarded connections. The label with
   * that number is also updated.
   */
  private void updateUserSlider()
  {
    m_settingsRoutingUserSlider.setMaximum(JAPModel.getModel().getRoutingSettings().
                         getBandwidthMaxConnections());
    m_settingsRoutingUserSlider.setValue(JAPModel.getModel().getRoutingSettings().getAllowedConnections());
    m_settingsRoutingUserSlider.setMajorTickSpacing(Math.max(1,
      m_settingsRoutingUserSlider.getMaximum() / 5));
    m_settingsRoutingUserSlider.setMinorTickSpacing(Math.max(1,
      m_settingsRoutingUserSlider.getMaximum() / 20));
    m_settingsRoutingUserSlider.setLabelTable(m_settingsRoutingUserSlider.createStandardLabels(
      m_settingsRoutingUserSlider.getMajorTickSpacing(), 0));
    /* update the label */
    updateUserLabel();
  }

  /**
   * Updates the list with mixcascades allowed for forwarding.
   */
  private void updateAllowedCascadesList()
  {
    m_settingsRoutingAllowedCascadesList.setListData(ForwardServerManager.getInstance().
      getAllowedCascadesDatabase().getEntryList());
  }

  /**
   * Creates the routing root panel with all child-panels dependent on the current routing mode.
   */
  private void updateRootPanel()
  {
    JPanel rootPanel = getRootPanel();

    /* clear the whole root panel */
    rootPanel.removeAll();

    /* insert all components in the root panel dependent on the current routing mode */
    int routingMode = JAPModel.getModel().getRoutingSettings().getRoutingMode();

    if (routingMode != JAPRoutingSettings.ROUTING_MODE_SERVER)
    {
      /* maybe the last routing mode was the server routing mode -> try to close the update
       * thread
       */
      try
      {
        m_serverStatusThread.interrupt();
        m_serverStatusThread.join();
      }
      catch (Exception e)
      {
        /* the thread wasn't running (NullPointerException) -> no problem */
      }
      m_serverStatusThread = null;
    }

    if (routingMode == JAPRoutingSettings.ROUTING_MODE_DISABLED)
    {
      JPanel configServerPanel = createRoutingServerConfigPanel();
      JPanel configClientPanel = createRoutingClientConfigPanel();

      GridBagLayout rootPanelLayout = new GridBagLayout();
      rootPanel.setLayout(rootPanelLayout);

      GridBagConstraints rootPanelConstraints = new GridBagConstraints();
      rootPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
      rootPanelConstraints.fill = GridBagConstraints.BOTH;
      rootPanelConstraints.weightx = 1.0;
      rootPanelConstraints.weighty = 1.0;

      rootPanelConstraints.gridx = 0;
      rootPanelConstraints.gridy = 0;
      rootPanelLayout.setConstraints(configServerPanel, rootPanelConstraints);
      rootPanel.add(configServerPanel);

      rootPanelConstraints.gridx = 0;
      rootPanelConstraints.gridy = 1;
      rootPanelConstraints.weighty = 0.0;
      rootPanelLayout.setConstraints(configClientPanel, rootPanelConstraints);
      rootPanel.add(configClientPanel);
    }

    if (routingMode == JAPRoutingSettings.ROUTING_MODE_SERVER)
    {
      JPanel configServerPanel = createRoutingServerConfigPanel();
      JPanel statusServerPanel = createRoutingServerStatusPanel();

      GridBagLayout rootPanelLayout = new GridBagLayout();
      rootPanel.setLayout(rootPanelLayout);

      GridBagConstraints rootPanelConstraints = new GridBagConstraints();
      rootPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
      rootPanelConstraints.fill = GridBagConstraints.BOTH;
      rootPanelConstraints.weightx = 1.0;

      rootPanelConstraints.weighty = 0.0;
      rootPanelConstraints.gridx = 0;
      rootPanelConstraints.gridy = 0;
      rootPanelLayout.setConstraints(configServerPanel, rootPanelConstraints);
      rootPanel.add(configServerPanel);

      rootPanelConstraints.weighty = 1.0;
      rootPanelConstraints.gridx = 0;
      rootPanelConstraints.gridy = 1;
      rootPanelLayout.setConstraints(statusServerPanel, rootPanelConstraints);
      rootPanel.add(statusServerPanel);
    }

    if (routingMode == JAPRoutingSettings.ROUTING_MODE_CLIENT)
    {
      JPanel configClientPanel = createRoutingClientConfigPanel();

      GridBagLayout rootPanelLayout = new GridBagLayout();
      rootPanel.setLayout(rootPanelLayout);

      GridBagConstraints rootPanelConstraints = new GridBagConstraints();
      rootPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
      rootPanelConstraints.fill = GridBagConstraints.BOTH;
      rootPanelConstraints.weightx = 1.0;
      rootPanelConstraints.weighty = 1.0;

      rootPanelConstraints.gridx = 0;
      rootPanelConstraints.gridy = 0;
      rootPanelLayout.setConstraints(configClientPanel, rootPanelConstraints);
      rootPanel.add(configClientPanel);
    }

    /* that repaints the panel, a simple repaint() doesn't work */
    rootPanel.setVisible(false);
    rootPanel.setVisible(true);
  }

}
