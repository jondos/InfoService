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

import java.text.NumberFormat;
import java.util.Enumeration;
import java.util.Observable;
import java.util.Observer;
import java.util.Vector;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import javax.swing.DefaultComboBoxModel;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
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
import javax.swing.JTable;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import anon.infoservice.InfoServiceDBEntry;
import anon.infoservice.InfoServiceHolder;
import anon.infoservice.ListenerInterface;
import anon.infoservice.MixCascade;
import gui.EmbeddedButton;
import gui.JAPConfRoutingSlider;
import gui.JAPHtmlMultiLineLabel;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/**
 * This is the configuration GUI for the JAP routing component.
 */
public class JAPConfRouting extends AbstractJAPConfModule
{
  
  /**
   * This is the internal message system of this module.
   */
  private MessageSystem m_messageSystem;
  

  /**
   * Constructor for JAPConfRouting. We do some initializing here.
   */
  public JAPConfRouting() {
    super(null);
  }

  /**
   * Creates the forwarding root panel with all child components.
   */
  public void recreateRootPanel()
  {
    synchronized (this) {
      if (m_messageSystem == null) {
        /* create a new object for sending internal messages */
        m_messageSystem = new MessageSystem();
      }
    }
    
    JPanel rootPanel = getRootPanel();

    /* clear the whole root panel */
    rootPanel.removeAll();

    /* insert all components in the root panel */
    JTabbedPane forwardingTabPane = new JTabbedPane();
    forwardingTabPane.setFont(getFontSetting());
    
    synchronized (this) {
      /* notify the observers of the message system that we recreate the root panel */
      m_messageSystem.sendMessage();
      /* recreate all parts of the forwarding configuration dialog */
      forwardingTabPane.insertTab(JAPMessages.getString("settingsRoutingStatusTabTitle"), null, createRoutingStatusPanel(), null, 0);
      forwardingTabPane.insertTab(JAPMessages.getString("settingsRoutingServerConfigTabTitle"), null, createRoutingServerConfigPanel(), null, 1);
      forwardingTabPane.insertTab(JAPMessages.getString("settingsRoutingClientConfigTabTitle"), null, createRoutingClientConfigPanel(), null, 2);
    }

    GridBagLayout rootPanelLayout = new GridBagLayout();
    rootPanel.setLayout(rootPanelLayout);

    GridBagConstraints rootPanelConstraints = new GridBagConstraints();
    rootPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
    rootPanelConstraints.fill = GridBagConstraints.BOTH;
    rootPanelConstraints.weightx = 1.0;
    rootPanelConstraints.weighty = 1.0;

    rootPanelConstraints.gridx = 0;
    rootPanelConstraints.gridy = 0;
    rootPanelLayout.setConstraints(forwardingTabPane, rootPanelConstraints);
    rootPanel.add(forwardingTabPane);
  }

  /**
   * Returns the title for routing configuration tab within the configuration window.
   *
   * @return The title for the routing configuration tab.
   */
  public String getTabTitle()
  {
    return JAPMessages.getString("confRoutingTab");
  }


  /**
   * Creates the routing server config panel with all components.
   *
   * @return The routing server config panel.
   */
  private JPanel createRoutingServerConfigPanel() {
    JPanel configPanel = new JPanel();

    final JPanel configBasicPanel = new JPanel();
    final JPanel configExpertPanel = new JPanel();
    
    final JPanel configBasicSettingsPanel = new JPanel();

    final JCheckBox settingsRoutingStartServerBox = new JCheckBox(JAPMessages.getString("settingsRoutingStartServerBox"));
    settingsRoutingStartServerBox.setFont(getFontSetting());
    settingsRoutingStartServerBox.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent event) {
        /* start or shutdown the forwarding server */
        if (settingsRoutingStartServerBox.isSelected()) {
          /* start the server by changing the routing mode */
          if (JAPModel.getInstance().getRoutingSettings().setRoutingMode(JAPRoutingSettings.ROUTING_MODE_SERVER) == false) {
            /* there was an error while starting the server */
            settingsRoutingStartServerBox.setSelected(false);
            JOptionPane.showMessageDialog(configBasicSettingsPanel, JAPMessages.getString("settingsRoutingStartServerError"), JAPMessages.getString("ERROR"), JOptionPane.ERROR_MESSAGE);
          }
          else {
            /* starting the server was successful, start the infoservice registration */
            showRegisterAtInfoServices();
          }
        }
        else {
          /* shutdown the server */
          JAPModel.getInstance().getRoutingSettings().setRoutingMode(JAPRoutingSettings.ROUTING_MODE_DISABLED);
        }
      }
    });
    
    final JLabel settingsRoutingServerPortLabel = new JLabel();
    settingsRoutingServerPortLabel.setFont(getFontSetting());

    Observer routingSettingsObserver = new Observer() {
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
      public void update(Observable a_notifier, Object a_message) {
        try {
          if (a_notifier == JAPModel.getInstance().getRoutingSettings()) {
            /* message is from JAPRoutingSettings */
            if (((JAPRoutingMessage)(a_message)).getMessageCode() == JAPRoutingMessage.ROUTING_MODE_CHANGED) {
              int newRoutingMode = JAPModel.getInstance().getRoutingSettings().getRoutingMode();
              if (newRoutingMode == JAPRoutingSettings.ROUTING_MODE_SERVER) {
                settingsRoutingStartServerBox.setEnabled(true);
                settingsRoutingStartServerBox.setSelected(true);
              }
              if (newRoutingMode == JAPRoutingSettings.ROUTING_MODE_CLIENT) {
                settingsRoutingStartServerBox.setEnabled(false);
                settingsRoutingStartServerBox.setSelected(false);
              }
              if (newRoutingMode == JAPRoutingSettings.ROUTING_MODE_DISABLED) {
                settingsRoutingStartServerBox.setEnabled(true);
                settingsRoutingStartServerBox.setSelected(false);
              }
            }
            if (((JAPRoutingMessage)(a_message)).getMessageCode() == JAPRoutingMessage.SERVER_PORT_CHANGED) {
              settingsRoutingServerPortLabel.setText(JAPMessages.getString("settingsRoutingServerPortLabel") + " " + Integer.toString(JAPModel.getInstance().getRoutingSettings().getServerPort()));
            }
          }
          if (a_notifier == m_messageSystem) {
            /* the root panel was recreated -> stop observing and remove ourself from the observed
             * objects
             */
            JAPModel.getInstance().getRoutingSettings().deleteObserver(this);
            m_messageSystem.deleteObserver(this);
          }
        }
        catch (Exception e) {
          /* should not happen */
          LogHolder.log(LogLevel.EXCEPTION, LogType.GUI, e);
        }
      }
    };
    /* registrate the observer also at the internal message system */
    m_messageSystem.addObserver(routingSettingsObserver);    
    JAPModel.getInstance().getRoutingSettings().addObserver(routingSettingsObserver);
    /* tricky: initialize the components by calling the observer (with all possible messages) */
    routingSettingsObserver.update(JAPModel.getInstance().getRoutingSettings(), new JAPRoutingMessage(JAPRoutingMessage.ROUTING_MODE_CHANGED));
    routingSettingsObserver.update(JAPModel.getInstance().getRoutingSettings(), new JAPRoutingMessage(JAPRoutingMessage.SERVER_PORT_CHANGED));

    JButton settingsRoutingPortEditButton = new JButton(JAPMessages.getString("settingsRoutingPortEditButton"));
    settingsRoutingPortEditButton.setFont(getFontSetting());
    settingsRoutingPortEditButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent event)
      {
        /* if the change port button is pressed, show the change server port dialog */
        showChangeServerPortDialog();
      }
    });

    JLabel settingsRoutingServerConfigMyConnectionLabel = new JLabel(JAPMessages.getString("settingsRoutingServerConfigMyConnectionLabel"));
    settingsRoutingServerConfigMyConnectionLabel.setFont(getFontSetting());
    final JComboBox connectionClassesComboBox = new JComboBox();
    connectionClassesComboBox.setFont(getFontSetting());
    connectionClassesComboBox.setEditable(false);
    final JLabel settingsRoutingServerConfigBandwidthLabel = new JLabel();
    settingsRoutingServerConfigBandwidthLabel.setFont(getFontSetting());

    connectionClassesComboBox.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent event)
      {
        /* if the connection class is changed, we update the current connection class in the
         * selector and the routing settings
         */
        JAPModel.getInstance().getRoutingSettings().getConnectionClassSelector().setCurrentConnectionClass(((JAPRoutingConnectionClass)(connectionClassesComboBox.getSelectedItem())).getIdentifier());
      }
    });

    Observer connectionClassSelectionObserver = new Observer() {
      /**
       * This is the observer implementation. If the current connection class or the list of all
       * connection classes is changed, the connection classes combo box is updated (the label
       * of the maximum used forwarding bandwidth is updated from the observer in the expert
       * configuration panel). If the panel is recreated (message via the module internal message
       * system), the observer removes itself from all observed objects.
       *
       * @param a_notifier The observed Object. This should always be
       *                   JAPRoutingConnectionClassSelector or the module internal message system
       *                   at the moment.
       * @param a_message The reason of the notification. This should always be a JAPRoutingMessage
       *                  or null at the moment.
       */
      public void update(Observable a_notifier, Object a_message) {
        try {
          if (a_notifier == JAPModel.getInstance().getRoutingSettings().getConnectionClassSelector()) {
            /* message is from JAPRoutingConnectionClassSelector */
            if (((JAPRoutingMessage)(a_message)).getMessageCode() == JAPRoutingMessage.CONNECTION_CLASSES_LIST_CHANGED) {
              /* re-read the list of all available connection classes */
              connectionClassesComboBox.setModel(new DefaultComboBoxModel(JAPModel.getInstance().getRoutingSettings().getConnectionClassSelector().getConnectionClasses()));
              connectionClassesComboBox.setSelectedItem(JAPModel.getInstance().getRoutingSettings().getConnectionClassSelector().getCurrentConnectionClass());
            }
            if (((JAPRoutingMessage)(a_message)).getMessageCode() == JAPRoutingMessage.CONNECTION_CLASS_CHANGED) {
              /* change the selected connection class */
              connectionClassesComboBox.setSelectedItem(JAPModel.getInstance().getRoutingSettings().getConnectionClassSelector().getCurrentConnectionClass());
            }
          }
          if (a_notifier == m_messageSystem) {
            /* the root panel was recreated -> stop observing and remove ourself from the observed
             * objects
             */
            JAPModel.getInstance().getRoutingSettings().getConnectionClassSelector().deleteObserver(this);
            m_messageSystem.deleteObserver(this);
          }
        }
        catch (Exception e) {
          /* should not happen */
          LogHolder.log(LogLevel.EXCEPTION, LogType.GUI, e);
        }
      }
    };
    /* registrate the observer also at the internal message system */
    m_messageSystem.addObserver(connectionClassSelectionObserver);    
    JAPModel.getInstance().getRoutingSettings().getConnectionClassSelector().addObserver(connectionClassSelectionObserver);
    /* tricky: initialize the combobox by calling the observer */
    connectionClassSelectionObserver.update(JAPModel.getInstance().getRoutingSettings().getConnectionClassSelector(), new JAPRoutingMessage(JAPRoutingMessage.CONNECTION_CLASSES_LIST_CHANGED));    
    
    TitledBorder settingsRoutingServerConfigBasicBorder = new TitledBorder(JAPMessages.getString("settingsRoutingServerConfigBasicBorderTitle"));
    settingsRoutingServerConfigBasicBorder.setTitleFont(getFontSetting());
    configBasicSettingsPanel.setBorder(settingsRoutingServerConfigBasicBorder);

    GridBagLayout configBasicSettingsPanelLayout = new GridBagLayout();
    configBasicSettingsPanel.setLayout(configBasicSettingsPanelLayout);

    GridBagConstraints configBasicSettingsPanelConstraints = new GridBagConstraints();
    configBasicSettingsPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
    configBasicSettingsPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
    configBasicSettingsPanelConstraints.weightx = 1.0;
    configBasicSettingsPanelConstraints.weighty = 0.0;

    configBasicSettingsPanelConstraints.gridx = 0;
    configBasicSettingsPanelConstraints.gridy = 0;
    configBasicSettingsPanelConstraints.gridwidth = 2;
    configBasicSettingsPanelConstraints.insets = new Insets(5, 5, 10, 5);
    configBasicSettingsPanelLayout.setConstraints(settingsRoutingStartServerBox, configBasicSettingsPanelConstraints);
    configBasicSettingsPanel.add(settingsRoutingStartServerBox);

    configBasicSettingsPanelConstraints.gridx = 0;
    configBasicSettingsPanelConstraints.gridy = 1;
    configBasicSettingsPanelConstraints.gridwidth = 1;
    configBasicSettingsPanelConstraints.weightx = 1.0;
    configBasicSettingsPanelConstraints.insets = new Insets(0, 5, 5, 10);
    configBasicSettingsPanelLayout.setConstraints(settingsRoutingServerPortLabel, configBasicSettingsPanelConstraints);
    configBasicSettingsPanel.add(settingsRoutingServerPortLabel);

    configBasicSettingsPanelConstraints.gridx = 1;
    configBasicSettingsPanelConstraints.gridy = 1;
    configBasicSettingsPanelConstraints.weightx = 0.0;
    configBasicSettingsPanelConstraints.insets = new Insets(0, 0, 5, 5);
    configBasicSettingsPanelLayout.setConstraints(settingsRoutingPortEditButton, configBasicSettingsPanelConstraints);
    configBasicSettingsPanel.add(settingsRoutingPortEditButton);

    configBasicSettingsPanelConstraints.gridx = 0;
    configBasicSettingsPanelConstraints.gridy = 2;
    configBasicSettingsPanelConstraints.weightx = 1.0;
    configBasicSettingsPanelConstraints.insets = new Insets(0, 5, 0, 0);
    configBasicSettingsPanelLayout.setConstraints(settingsRoutingServerConfigMyConnectionLabel, configBasicSettingsPanelConstraints);
    configBasicSettingsPanel.add(settingsRoutingServerConfigMyConnectionLabel);

    configBasicSettingsPanelConstraints.gridx = 0;
    configBasicSettingsPanelConstraints.gridy = 3;
    configBasicSettingsPanelConstraints.fill = GridBagConstraints.NONE;
    configBasicSettingsPanelConstraints.insets = new Insets(0, 5, 5, 0);
    configBasicSettingsPanelLayout.setConstraints(connectionClassesComboBox, configBasicSettingsPanelConstraints);
    configBasicSettingsPanel.add(connectionClassesComboBox);

    configBasicSettingsPanelConstraints.gridx = 0;
    configBasicSettingsPanelConstraints.gridy = 4;
    configBasicSettingsPanelConstraints.weighty = 1.0;
    configBasicSettingsPanelConstraints.insets = new Insets(0, 5, 20, 0);
    configBasicSettingsPanelLayout.setConstraints(settingsRoutingServerConfigBandwidthLabel, configBasicSettingsPanelConstraints);
    configBasicSettingsPanel.add(settingsRoutingServerConfigBandwidthLabel);

    EmbeddedButton settingsRoutingServerConfigExpertButton = new EmbeddedButton(JAPMessages.getString("settingsRoutingServerConfigExpertButton"));
    settingsRoutingServerConfigExpertButton.setFont(getFontSetting());
    settingsRoutingServerConfigExpertButton.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent event)
      {
        /* if the expert settings button is pressed, show the server expert settings panel and
         * hide the basic settings
         */
        configBasicPanel.setVisible(false);
        configExpertPanel.setVisible(true);
      }
    });
    
    GridBagLayout configBasicPanelLayout = new GridBagLayout();
    configBasicPanel.setLayout(configBasicPanelLayout);

    GridBagConstraints configBasicPanelConstraints = new GridBagConstraints();
    configBasicPanelConstraints.weightx = 1.0;

    configBasicPanelConstraints.gridx = 0;
    configBasicPanelConstraints.gridy = 0;
    configBasicPanelConstraints.anchor = GridBagConstraints.NORTHEAST;
    configBasicPanelConstraints.fill = GridBagConstraints.NONE;
    configBasicPanelConstraints.weighty = 0.0;
    configBasicSettingsPanelConstraints.insets = new Insets(2, 0, 2, 0);
    configBasicPanelLayout.setConstraints(settingsRoutingServerConfigExpertButton, configBasicPanelConstraints);
    configBasicPanel.add(settingsRoutingServerConfigExpertButton);

    configBasicPanelConstraints.gridx = 0;
    configBasicPanelConstraints.gridy = 1;
    configBasicPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
    configBasicPanelConstraints.fill = GridBagConstraints.BOTH;
    configBasicPanelConstraints.weighty = 1.0;
    configBasicSettingsPanelConstraints.insets = new Insets(0, 0, 0, 0);
    configBasicPanelLayout.setConstraints(configBasicSettingsPanel, configBasicPanelConstraints);
    configBasicPanel.add(configBasicSettingsPanel);
    
    final JPanel configExpertSettingsPanel = new JPanel();
    
    final JLabel settingsRoutingServerConfigExpertBandwidthLabel = new JLabel();
    settingsRoutingServerConfigExpertBandwidthLabel.setFont(getFontSetting());
    final JAPConfRoutingSlider settingsRoutingServerConfigExpertBandwidthSlider = new JAPConfRoutingSlider();
    settingsRoutingServerConfigExpertBandwidthSlider.setFont(getFontSetting());
    settingsRoutingServerConfigExpertBandwidthSlider.addChangeListener(new ChangeListener() {
      public void stateChanged(ChangeEvent event) {
        /* update the connection class' current bandwidth */
        JAPRoutingConnectionClass currentConnectionClass = JAPModel.getInstance().getRoutingSettings().getConnectionClassSelector().getCurrentConnectionClass();
        currentConnectionClass.setCurrentBandwidth((settingsRoutingServerConfigExpertBandwidthSlider.getValue() * 1000) / 8);
        /* also update the number of simultaneous connections -> set it always to the maximum */
        currentConnectionClass.setSimultaneousConnections(currentConnectionClass.getMaxSimultaneousConnections());
        /* write the values to the forwarding system */
        JAPModel.getInstance().getRoutingSettings().getConnectionClassSelector().setCurrentConnectionClass(currentConnectionClass.getIdentifier());
      }
    });
    JButton settingsRoutingServerConfigExpertMaxBandwidthButton = new JButton(JAPMessages.getString("settingsRoutingServerConfigExpertMaxBandwidthButton"));
    settingsRoutingServerConfigExpertMaxBandwidthButton.setFont(getFontSetting());
    settingsRoutingServerConfigExpertMaxBandwidthButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent event) {
        /* if the max bandwidth button is pressed, show the maximum bandwidth dialog */
        showMaximumBandwidthDialog(configExpertSettingsPanel);
      }
    });

    final JLabel settingsRoutingServerConfigExpertUserLabel = new JLabel();
    settingsRoutingServerConfigExpertUserLabel.setFont(getFontSetting());
    final JAPConfRoutingSlider settingsRoutingServerConfigExpertUserSlider = new JAPConfRoutingSlider();
    settingsRoutingServerConfigExpertUserSlider.setFont(getFontSetting());
    settingsRoutingServerConfigExpertUserSlider.addChangeListener(new ChangeListener() {
      public void stateChanged(ChangeEvent event) {
        /* change the number of simultaneously forwarded connections */
        JAPRoutingConnectionClass currentConnectionClass = JAPModel.getInstance().getRoutingSettings().getConnectionClassSelector().getCurrentConnectionClass();
        currentConnectionClass.setSimultaneousConnections(settingsRoutingServerConfigExpertUserSlider.getValue());          
        /* write the values to the forwarding system */
        JAPModel.getInstance().getRoutingSettings().getConnectionClassSelector().setCurrentConnectionClass(currentConnectionClass.getIdentifier());
      }
    });

    Observer connectionClassSettingsObserver = new Observer() {
      /**
       * This is the observer implementation. If the forwarding bandwidth or the number of
       * simultaneous connections is changed via the connection class selector, we update the
       * sliders and labels (also the bandwidth label in the basic server configuration).  If the
       * panel is recreated (message via the module internal message system), the observer removes
       * itself from all observed objects.
       *
       * @param a_notifier The observed Object. This should always be
       *                   JAPRoutingConnectionClassSelector or the module internal message system
       *                   at the moment.
       * @param a_message The reason of the notification. This should always be a JAPRoutingMessage
       *                  or null at the moment.
       */
      public void update(Observable a_notifier, Object a_message) {
        try {
          if (a_notifier == JAPModel.getInstance().getRoutingSettings().getConnectionClassSelector()) {
            /* message is from JAPRoutingConnectionClassSelector */
            if (((JAPRoutingMessage)(a_message)).getMessageCode() == JAPRoutingMessage.CONNECTION_PARAMETERS_CHANGED) {
              /* update the sliders and labels*/
              /* bandwidth label in the basic server configuration */
              settingsRoutingServerConfigBandwidthLabel.setText(JAPMessages.getString("settingsRoutingServerConfigBandwidthLabelPart1") + " " + Integer.toString((JAPModel.getInstance().getRoutingSettings().getBandwidth() * 8) / 1000) + " " + JAPMessages.getString("settingsRoutingServerConfigBandwidthLabelPart2"));
              /* bandwidth label */
              settingsRoutingServerConfigExpertBandwidthLabel.setText(JAPMessages.getString("settingsRoutingServerConfigExpertBandwidthLabelPart1") + " " + Integer.toString((JAPModel.getInstance().getRoutingSettings().getBandwidth() * 8) / 1000) + " " + JAPMessages.getString("settingsRoutingServerConfigExpertBandwidthLabelPart2"));
              /* bandwidth slider */
              synchronized (settingsRoutingServerConfigExpertBandwidthSlider) {
                /* disable change events -> the ChangeListener will ignore the change */
                settingsRoutingServerConfigExpertBandwidthSlider.setChangeEventsEnabled(false);
                settingsRoutingServerConfigExpertBandwidthSlider.setMaximum((JAPModel.getInstance().getRoutingSettings().getMaxBandwidth() * 8) / 1000);
                settingsRoutingServerConfigExpertBandwidthSlider.setValue((JAPModel.getInstance().getRoutingSettings().getBandwidth() * 8) / 1000); 
                settingsRoutingServerConfigExpertBandwidthSlider.setLabelTable(settingsRoutingServerConfigExpertBandwidthSlider.createStandardLabels(Math.max(1, settingsRoutingServerConfigExpertBandwidthSlider.getMaximum() / 5), 0));
                /* re-enable the change events */
                settingsRoutingServerConfigExpertBandwidthSlider.setChangeEventsEnabled(true);
              }                
              /* user label */
              settingsRoutingServerConfigExpertUserLabel.setText(JAPMessages.getString("settingsRoutingServerConfigExpertUserLabel") + " " + JAPModel.getInstance().getRoutingSettings().getAllowedConnections());
              /* user slider */
              synchronized (settingsRoutingServerConfigExpertUserSlider) {
                /* disable change events -> the ChangeListener will ignore the change */
                settingsRoutingServerConfigExpertUserSlider.setChangeEventsEnabled(false);
                settingsRoutingServerConfigExpertUserSlider.setMaximum(JAPModel.getInstance().getRoutingSettings().getBandwidthMaxConnections());
                settingsRoutingServerConfigExpertUserSlider.setValue(JAPModel.getInstance().getRoutingSettings().getAllowedConnections());
                settingsRoutingServerConfigExpertUserSlider.setLabelTable(settingsRoutingServerConfigExpertUserSlider.createStandardLabels(Math.max(1, settingsRoutingServerConfigExpertUserSlider.getMaximum() / 5), 0));
                /* re-enable the change events */
                settingsRoutingServerConfigExpertUserSlider.setChangeEventsEnabled(true);
              }
            }
          }
          if (a_notifier == m_messageSystem) {
            /* the root panel was recreated -> stop observing and remove ourself from the observed
             * objects
             */
            JAPModel.getInstance().getRoutingSettings().getConnectionClassSelector().deleteObserver(this);
            m_messageSystem.deleteObserver(this);
          }
        }
        catch (Exception e) {
          /* should not happen */
          LogHolder.log(LogLevel.EXCEPTION, LogType.GUI, e);
        }
      }
    };
    /* registrate the observer also at the internal message system */
    m_messageSystem.addObserver(connectionClassSettingsObserver);    
    JAPModel.getInstance().getRoutingSettings().getConnectionClassSelector().addObserver(connectionClassSettingsObserver);
    /* tricky: initialize the sliders and labels by calling the observer */
    connectionClassSettingsObserver.update(JAPModel.getInstance().getRoutingSettings().getConnectionClassSelector(), new JAPRoutingMessage(JAPRoutingMessage.CONNECTION_PARAMETERS_CHANGED));

    TitledBorder settingsRoutingServerConfigExpertBorder = new TitledBorder(JAPMessages.getString("settingsRoutingServerConfigExpertBorderTitle"));
    settingsRoutingServerConfigExpertBorder.setTitleFont(getFontSetting());
    configExpertSettingsPanel.setBorder(settingsRoutingServerConfigExpertBorder);

    GridBagLayout configExpertSettingsPanelLayout = new GridBagLayout();
    configExpertSettingsPanel.setLayout(configExpertSettingsPanelLayout);
    GridBagConstraints configExpertSettingsPanelConstraints = new GridBagConstraints();

    configExpertSettingsPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
    configExpertSettingsPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
    configExpertSettingsPanelConstraints.gridx = 0;
    configExpertSettingsPanelConstraints.gridy = 0;
    configExpertSettingsPanelConstraints.weightx = 1.0;
    configExpertSettingsPanelConstraints.weighty = 0.0;
    configExpertSettingsPanelConstraints.gridwidth = 1;
    configExpertSettingsPanelConstraints.insets = new Insets(5, 5, 0, 0);
    configExpertSettingsPanelLayout.setConstraints(settingsRoutingServerConfigExpertBandwidthLabel, configExpertSettingsPanelConstraints);
    configExpertSettingsPanel.add(settingsRoutingServerConfigExpertBandwidthLabel);

    configExpertSettingsPanelConstraints.gridx = 0;
    configExpertSettingsPanelConstraints.gridy = 1;
    configExpertSettingsPanelConstraints.insets = new Insets(0, 5, 10, 0);
    configExpertSettingsPanelLayout.setConstraints(settingsRoutingServerConfigExpertBandwidthSlider, configExpertSettingsPanelConstraints);
    configExpertSettingsPanel.add(settingsRoutingServerConfigExpertBandwidthSlider);

    configExpertSettingsPanelConstraints.gridx = 1;
    configExpertSettingsPanelConstraints.gridy = 1;
    configExpertSettingsPanelConstraints.weightx = 0.0;
    configExpertSettingsPanelConstraints.insets = new Insets(0, 10, 10, 5);
    configExpertSettingsPanelLayout.setConstraints(settingsRoutingServerConfigExpertMaxBandwidthButton, configExpertSettingsPanelConstraints);
    configExpertSettingsPanel.add(settingsRoutingServerConfigExpertMaxBandwidthButton);

    configExpertSettingsPanelConstraints.gridx = 0;
    configExpertSettingsPanelConstraints.gridy = 2;
    configExpertSettingsPanelConstraints.weightx = 1.0;
    configExpertSettingsPanelConstraints.insets = new Insets(0, 5, 0, 0);
    configExpertSettingsPanelLayout.setConstraints(settingsRoutingServerConfigExpertUserLabel, configExpertSettingsPanelConstraints);
    configExpertSettingsPanel.add(settingsRoutingServerConfigExpertUserLabel);

    configExpertSettingsPanelConstraints.gridx = 0;
    configExpertSettingsPanelConstraints.gridy = 3;
    configExpertSettingsPanelConstraints.weighty = 1.0;
    configExpertSettingsPanelConstraints.insets = new Insets(0, 5, 15, 0);
    configExpertSettingsPanelLayout.setConstraints(settingsRoutingServerConfigExpertUserSlider, configExpertSettingsPanelConstraints);
    configExpertSettingsPanel.add(settingsRoutingServerConfigExpertUserSlider);

    final JPanel configExpertCascadesPanel = new JPanel();

    final JLabel settingsRoutingServerConfigExpertAllowedCascadesKnownCascadesLabel = new JLabel(JAPMessages.getString("settingsRoutingServerConfigExpertAllowedCascadesKnownCascadesLabel"));
    settingsRoutingServerConfigExpertAllowedCascadesKnownCascadesLabel.setFont(getFontSetting());
    final JLabel settingsRoutingServerConfigExpertAllowedCascadesAllowedCascadesLabel = new JLabel(JAPMessages.getString("settingsRoutingServerConfigExpertAllowedCascadesAllowedCascadesLabel"));
    settingsRoutingServerConfigExpertAllowedCascadesAllowedCascadesLabel.setFont(getFontSetting());

    final DefaultListModel knownCascadesListModel = new DefaultListModel();
    final JList knownCascadesList = new JList(knownCascadesListModel);
    knownCascadesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    final JScrollPane knownCascadesScrollPane = new JScrollPane(knownCascadesList);
    knownCascadesScrollPane.setFont(getFontSetting());
    /* set the preferred size of the scrollpane to a 4x20 textarea */
    knownCascadesScrollPane.setPreferredSize((new JTextArea(4, 20)).getPreferredSize());

    final DefaultListModel allowedCascadesListModel = new DefaultListModel();
    final JList allowedCascadesList = new JList(allowedCascadesListModel);
    allowedCascadesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    final JScrollPane allowedCascadesScrollPane = new JScrollPane(allowedCascadesList);
    allowedCascadesScrollPane.setFont(getFontSetting());
    /* set the preferred size of the scrollpane to a 4x20 textarea */
    allowedCascadesScrollPane.setPreferredSize((new JTextArea(4, 20)).getPreferredSize());

    final JButton settingsRoutingServerConfigExpertAllowedCascadesAddButton = new JButton(JAPMessages.getString("settingsRoutingServerConfigExpertAllowedCascadesAddButton"));
    settingsRoutingServerConfigExpertAllowedCascadesAddButton.setFont(getFontSetting());
    settingsRoutingServerConfigExpertAllowedCascadesAddButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent event) {
        /* if the Add button is pressed, add the selected mixcascade to the list of allowed
         * mixcascades for the clients of the local forwarding server, if it is not already there
         */
        MixCascade selectedCascade = (MixCascade)(knownCascadesList.getSelectedValue());
        if (selectedCascade != null) {
          JAPModel.getInstance().getRoutingSettings().getUseableMixCascadesStore().addToAllowedMixCascades(selectedCascade);
        }  
      }
    });

    final JButton settingsRoutingServerConfigExpertAllowedCascadesRemoveButton = new JButton(JAPMessages.getString("settingsRoutingServerConfigExpertAllowedCascadesRemoveButton"));
    settingsRoutingServerConfigExpertAllowedCascadesRemoveButton.setFont(getFontSetting());
    settingsRoutingServerConfigExpertAllowedCascadesRemoveButton.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent event)
      {
        /* if the Remove button is pressed, remove the selected mixcascade from the list of
         * allowed mixcascades
         */
        MixCascade selectedCascade = (MixCascade)(allowedCascadesList.getSelectedValue());
        if (selectedCascade != null) {
          JAPModel.getInstance().getRoutingSettings().getUseableMixCascadesStore().removeFromAllowedMixCascades(selectedCascade.getId());
        }
      }
    });

    final JCheckBox settingsRoutingServerConfigExpertAllowedCascadesAllowAllBox = new JCheckBox(JAPMessages.getString("settingsRoutingServerConfigExpertAllowedCascadesAllowAllBox"), JAPModel.getInstance().getRoutingSettings().getUseableMixCascadesStore().getAllowAllAvailableMixCascades());
    settingsRoutingServerConfigExpertAllowedCascadesAllowAllBox.setFont(getFontSetting());
    settingsRoutingServerConfigExpertAllowedCascadesAllowAllBox.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent event)
      {
        JAPModel.getInstance().getRoutingSettings().getUseableMixCascadesStore().setAllowAllAvailableMixCascades(settingsRoutingServerConfigExpertAllowedCascadesAllowAllBox.isSelected());
      }
    });

    Observer allowedMixCascadesObserver = new Observer() {
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
      public void update(Observable a_notifier, Object a_message) {
        try {
          if (a_notifier == JAPModel.getInstance().getRoutingSettings().getUseableMixCascadesStore()) {
            /* message is from JAPRoutingUseableMixCascades */
            int messageCode = ((JAPRoutingMessage)(a_message)).getMessageCode();
            if (messageCode == JAPRoutingMessage.ALLOWED_MIXCASCADES_POLICY_CHANGED) {
              /* enable or disable the components on the panel as needed for currently selected
               * mode
               */
              if (JAPModel.getInstance().getRoutingSettings().getUseableMixCascadesStore().getAllowAllAvailableMixCascades()) {
                /* access-to-all available mixcascades -> components for managing the list of
                 * allowed mixcascades not needed
                 */
                settingsRoutingServerConfigExpertAllowedCascadesKnownCascadesLabel.setEnabled(false);
                settingsRoutingServerConfigExpertAllowedCascadesAllowedCascadesLabel.setEnabled(false);
                knownCascadesList.setEnabled(false);
                allowedCascadesList.setEnabled(false);
                /* remove all entries from the both listboxes -> no irritation */
                knownCascadesList.setModel(new DefaultListModel());
                allowedCascadesList.setModel(new DefaultListModel());                
                settingsRoutingServerConfigExpertAllowedCascadesAddButton.setEnabled(false);
                settingsRoutingServerConfigExpertAllowedCascadesRemoveButton.setEnabled(false);
              }
              else {
                /* available mixcascades for forwarding are restricted to a list -> components for
                 * managing that list needed
                 */
                settingsRoutingServerConfigExpertAllowedCascadesKnownCascadesLabel.setEnabled(true);
                settingsRoutingServerConfigExpertAllowedCascadesAllowedCascadesLabel.setEnabled(true);
                /* restore the original listmodels */
                knownCascadesList.setModel(knownCascadesListModel);
                allowedCascadesList.setModel(allowedCascadesListModel);                
                knownCascadesList.setEnabled(true);
                allowedCascadesList.setEnabled(true);
                settingsRoutingServerConfigExpertAllowedCascadesAddButton.setEnabled(true);
                settingsRoutingServerConfigExpertAllowedCascadesRemoveButton.setEnabled(true);
              }
            }
            if (messageCode == JAPRoutingMessage.ALLOWED_MIXCASCADES_LIST_CHANGED) {
              synchronized (allowedCascadesListModel) {
                allowedCascadesListModel.clear();
                Enumeration allowedCascades = JAPModel.getInstance().getRoutingSettings().getUseableMixCascadesStore().getAllowedMixCascades().elements();
                while (allowedCascades.hasMoreElements()) {
                  allowedCascadesListModel.addElement(allowedCascades.nextElement());
                }
              }
            }
          }
          if (a_notifier == m_messageSystem) {
            /* the root panel was recreated -> stop observing and remove ourself from the observed
             * objects
             */
            JAPModel.getInstance().getRoutingSettings().getUseableMixCascadesStore().deleteObserver(this);
            m_messageSystem.deleteObserver(this);
          }
        }
        catch (Exception e) {
          /* should not happen */
          LogHolder.log(LogLevel.EXCEPTION, LogType.GUI, e);
        }
      }
    };
    /* registrate the observer also at the internal message system */
    m_messageSystem.addObserver(allowedMixCascadesObserver);    
    JAPModel.getInstance().getRoutingSettings().getUseableMixCascadesStore().addObserver(allowedMixCascadesObserver);
    /* tricky: initialize the components by calling the observer (with all possible messages) */
    allowedMixCascadesObserver.update(JAPModel.getInstance().getRoutingSettings().getUseableMixCascadesStore(), new JAPRoutingMessage(JAPRoutingMessage.ALLOWED_MIXCASCADES_LIST_CHANGED));
    allowedMixCascadesObserver.update(JAPModel.getInstance().getRoutingSettings().getUseableMixCascadesStore(), new JAPRoutingMessage(JAPRoutingMessage.ALLOWED_MIXCASCADES_POLICY_CHANGED));

    TitledBorder settingsRoutingServerConfigExpertAllowedCascadesBorder = new TitledBorder(JAPMessages.getString("settingsRoutingServerConfigExpertAllowedCascadesBorderTitle"));
    settingsRoutingServerConfigExpertAllowedCascadesBorder.setTitleFont(getFontSetting());
    configExpertCascadesPanel.setBorder(settingsRoutingServerConfigExpertAllowedCascadesBorder);

    GridBagLayout configExpertCascadesPanelLayout = new GridBagLayout();
    configExpertCascadesPanel.setLayout(configExpertCascadesPanelLayout);

    GridBagConstraints configExpertCascadesPanelConstraints = new GridBagConstraints();
    configExpertCascadesPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
    configExpertCascadesPanelConstraints.fill = GridBagConstraints.NONE;
    configExpertCascadesPanelConstraints.weightx = 1.0;
    configExpertCascadesPanelConstraints.weighty = 0.0;

    configExpertCascadesPanelConstraints.gridx = 0;
    configExpertCascadesPanelConstraints.gridy = 0;
    configExpertCascadesPanelConstraints.gridwidth = 2;
    configExpertCascadesPanelConstraints.insets = new Insets(0, 5, 10, 5);
    configExpertCascadesPanelLayout.setConstraints(settingsRoutingServerConfigExpertAllowedCascadesAllowAllBox, configExpertCascadesPanelConstraints);
    configExpertCascadesPanel.add(settingsRoutingServerConfigExpertAllowedCascadesAllowAllBox);

    configExpertCascadesPanelConstraints.gridx = 0;
    configExpertCascadesPanelConstraints.gridy = 1;
    configExpertCascadesPanelConstraints.gridwidth = 1;
    configExpertCascadesPanelConstraints.insets = new Insets(0, 5, 0, 5);
    configExpertCascadesPanelLayout.setConstraints(settingsRoutingServerConfigExpertAllowedCascadesKnownCascadesLabel, configExpertCascadesPanelConstraints);
    configExpertCascadesPanel.add(settingsRoutingServerConfigExpertAllowedCascadesKnownCascadesLabel);

    configExpertCascadesPanelConstraints.gridx = 1;
    configExpertCascadesPanelConstraints.gridy = 1;
    configExpertCascadesPanelConstraints.insets = new Insets(0, 5, 0, 5);
    configExpertCascadesPanelLayout.setConstraints(settingsRoutingServerConfigExpertAllowedCascadesAllowedCascadesLabel, configExpertCascadesPanelConstraints);
    configExpertCascadesPanel.add(settingsRoutingServerConfigExpertAllowedCascadesAllowedCascadesLabel);

    configExpertCascadesPanelConstraints.gridx = 0;
    configExpertCascadesPanelConstraints.gridy = 2;
    configExpertCascadesPanelConstraints.weighty = 1.0;
    configExpertCascadesPanelConstraints.fill = GridBagConstraints.BOTH;
    configExpertCascadesPanelConstraints.insets = new Insets(0, 5, 0, 5);
    configExpertCascadesPanelLayout.setConstraints(knownCascadesScrollPane, configExpertCascadesPanelConstraints);
    configExpertCascadesPanel.add(knownCascadesScrollPane);

    configExpertCascadesPanelConstraints.gridx = 1;
    configExpertCascadesPanelConstraints.gridy = 2;
    configExpertCascadesPanelConstraints.insets = new Insets(0, 5, 0, 5);
    configExpertCascadesPanelLayout.setConstraints(allowedCascadesScrollPane, configExpertCascadesPanelConstraints);
    configExpertCascadesPanel.add(allowedCascadesScrollPane);

    configExpertCascadesPanelConstraints.gridx = 0;
    configExpertCascadesPanelConstraints.gridy = 3;
    configExpertCascadesPanelConstraints.weighty = 0.0;
    configExpertCascadesPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
    configExpertCascadesPanelConstraints.insets = new Insets(5, 5, 5, 5);
    configExpertCascadesPanelLayout.setConstraints(settingsRoutingServerConfigExpertAllowedCascadesAddButton, configExpertCascadesPanelConstraints);
    configExpertCascadesPanel.add(settingsRoutingServerConfigExpertAllowedCascadesAddButton);

    configExpertCascadesPanelConstraints.gridx = 1;
    configExpertCascadesPanelConstraints.gridy = 3;
    configExpertCascadesPanelConstraints.insets = new Insets(5, 5, 5, 5);
    configExpertCascadesPanelLayout.setConstraints(settingsRoutingServerConfigExpertAllowedCascadesRemoveButton, configExpertCascadesPanelConstraints);
    configExpertCascadesPanel.add(settingsRoutingServerConfigExpertAllowedCascadesRemoveButton);

    final JPanel configExpertInfoServicePanel = new JPanel();

    final JLabel settingsRoutingServerConfigExpertRegistrationInfoServicesKnownInfoServicesLabel = new JLabel(JAPMessages.getString("settingsRoutingServerConfigExpertRegistrationInfoServicesKnownInfoServicesLabel"));
    settingsRoutingServerConfigExpertRegistrationInfoServicesKnownInfoServicesLabel.setFont(getFontSetting());
    final JLabel settingsRoutingServerConfigExpertRegistrationInfoServicesSelectedInfoServicesLabel = new JLabel(JAPMessages.getString("settingsRoutingServerConfigExpertRegistrationInfoServicesSelectedInfoServicesLabel"));
    settingsRoutingServerConfigExpertRegistrationInfoServicesSelectedInfoServicesLabel.setFont(getFontSetting());

    final DefaultListModel knownInfoServicesListModel = new DefaultListModel();
    final JList knownInfoServicesList = new JList(knownInfoServicesListModel);
    knownInfoServicesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    final JScrollPane knownInfoServicesScrollPane = new JScrollPane(knownInfoServicesList);
    knownInfoServicesScrollPane.setFont(getFontSetting());
    /* set the preferred size of the scrollpane to a 4x20 textarea */
    knownInfoServicesScrollPane.setPreferredSize((new JTextArea(4, 20)).getPreferredSize());

    final DefaultListModel registrationInfoServicesListModel = new DefaultListModel();
    final JList registrationInfoServicesList = new JList(registrationInfoServicesListModel);
    registrationInfoServicesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    final JScrollPane registrationInfoServicesScrollPane = new JScrollPane(registrationInfoServicesList);
    registrationInfoServicesScrollPane.setFont(getFontSetting());
    /* set the preferred size of the scrollpane to a 4x20 textarea */
    registrationInfoServicesScrollPane.setPreferredSize((new JTextArea(4, 20)).getPreferredSize());

    final JButton settingsRoutingServerConfigExpertRegistrationInfoServicesAddButton = new JButton(JAPMessages.getString("settingsRoutingServerConfigExpertRegistrationInfoServicesAddButton"));
    settingsRoutingServerConfigExpertRegistrationInfoServicesAddButton.setFont(getFontSetting());
    settingsRoutingServerConfigExpertRegistrationInfoServicesAddButton.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent event)
      {
        /* if the Add button is pressed, add the selected known infoservice to the list of
         * registration infoservices
         */
        InfoServiceDBEntry selectedInfoService = (InfoServiceDBEntry)(knownInfoServicesList.getSelectedValue());
        if (selectedInfoService != null) {
          JAPModel.getInstance().getRoutingSettings().getRegistrationInfoServicesStore().addToRegistrationInfoServices(selectedInfoService);
        }
      }
    });

    final JButton settingsRoutingServerConfigExpertRegistrationInfoServicesRemoveButton = new JButton(JAPMessages.getString("settingsRoutingServerConfigExpertRegistrationInfoServicesRemoveButton"));
    settingsRoutingServerConfigExpertRegistrationInfoServicesRemoveButton.setFont(getFontSetting());
    settingsRoutingServerConfigExpertRegistrationInfoServicesRemoveButton.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent event)
      {
        /* if the Remove button is pressed, remove the selected infoservice from the list of
         * registration infoservices
         */
        InfoServiceDBEntry selectedInfoService = (InfoServiceDBEntry)(registrationInfoServicesList.getSelectedValue());
        if (selectedInfoService != null) {
          JAPModel.getInstance().getRoutingSettings().getRegistrationInfoServicesStore().removeFromRegistrationInfoServices(selectedInfoService.getId());
        }
      }
    });

    final JCheckBox settingsRoutingServerConfigExpertRegistrationInfoServicesRegisterAtAllBox = new JCheckBox(JAPMessages.getString("settingsRoutingServerConfigExpertRegistrationInfoServicesRegisterAtAllBox"), JAPModel.getInstance().getRoutingSettings().getRegistrationInfoServicesStore().getRegisterAtAllAvailableInfoServices());
    settingsRoutingServerConfigExpertRegistrationInfoServicesRegisterAtAllBox.setFont(getFontSetting());
    settingsRoutingServerConfigExpertRegistrationInfoServicesRegisterAtAllBox.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent event)
      {
        JAPModel.getInstance().getRoutingSettings().getRegistrationInfoServicesStore().setRegisterAtAllAvailableInfoServices(settingsRoutingServerConfigExpertRegistrationInfoServicesRegisterAtAllBox.isSelected());
      }
    });

    Observer registrationInfoServicesObserver = new Observer() {
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
      public void update(Observable a_notifier, Object a_message) {
        try {
          if (a_notifier == JAPModel.getInstance().getRoutingSettings().getRegistrationInfoServicesStore()) {
            /* message is from JAPRoutingUseableMixCascades */
            int messageCode = ((JAPRoutingMessage)(a_message)).getMessageCode();
            if (messageCode == JAPRoutingMessage.REGISTRATION_INFOSERVICES_POLICY_CHANGED) {
              /* enable or disable the components on the panel as needed for currently selected
               * mode
               */
              if (JAPModel.getInstance().getRoutingSettings().getRegistrationInfoServicesStore().getRegisterAtAllAvailableInfoServices()) {
                /* register-at-all available primary infoservices -> components for managing the
                 * list of registration infoservices not needed
                 */
                settingsRoutingServerConfigExpertRegistrationInfoServicesKnownInfoServicesLabel.setEnabled(false);
                settingsRoutingServerConfigExpertRegistrationInfoServicesSelectedInfoServicesLabel.setEnabled(false);
                knownInfoServicesList.setEnabled(false);
                registrationInfoServicesList.setEnabled(false);
                /* remove all entries from the both listboxes -> no irritation */
                knownInfoServicesList.setModel(new DefaultListModel());
                registrationInfoServicesList.setModel(new DefaultListModel());                
                settingsRoutingServerConfigExpertRegistrationInfoServicesAddButton.setEnabled(false);
                settingsRoutingServerConfigExpertRegistrationInfoServicesRemoveButton.setEnabled(false);
              }
              else {
                /* register only at the infoservices from the registration list -> components for
                 * managing that list needed
                 */
                settingsRoutingServerConfigExpertRegistrationInfoServicesKnownInfoServicesLabel.setEnabled(true);
                settingsRoutingServerConfigExpertRegistrationInfoServicesSelectedInfoServicesLabel.setEnabled(true);
                /* restore the original listmodels */
                knownInfoServicesList.setModel(knownInfoServicesListModel);
                registrationInfoServicesList.setModel(registrationInfoServicesListModel);                
                knownInfoServicesList.setEnabled(true);
                registrationInfoServicesList.setEnabled(true);
                settingsRoutingServerConfigExpertRegistrationInfoServicesAddButton.setEnabled(true);
                settingsRoutingServerConfigExpertRegistrationInfoServicesRemoveButton.setEnabled(true);
              }
            }
            if (messageCode == JAPRoutingMessage.REGISTRATION_INFOSERVICES_LIST_CHANGED) {
              synchronized (registrationInfoServicesListModel) {
                registrationInfoServicesListModel.clear();
                Enumeration registrationInfoServices = JAPModel.getInstance().getRoutingSettings().getRegistrationInfoServicesStore().getRegistrationInfoServices().elements();
                while (registrationInfoServices.hasMoreElements()) {
                  registrationInfoServicesListModel.addElement(registrationInfoServices.nextElement());
                }
              }
            }
          }
          if (a_notifier == m_messageSystem) {
            /* the root panel was recreated -> stop observing and remove ourself from the observed
             * objects
             */
            JAPModel.getInstance().getRoutingSettings().getRegistrationInfoServicesStore().deleteObserver(this);
            m_messageSystem.deleteObserver(this);
          }
        }
        catch (Exception e) {
          /* should not happen */
          LogHolder.log(LogLevel.EXCEPTION, LogType.GUI, e);
        }
      }
    };
    /* registrate the observer also at the internal message system */
    m_messageSystem.addObserver(registrationInfoServicesObserver);    
    JAPModel.getInstance().getRoutingSettings().getRegistrationInfoServicesStore().addObserver(registrationInfoServicesObserver);
    /* tricky: initialize the components by calling the observer (with all possible messages) */
    registrationInfoServicesObserver.update(JAPModel.getInstance().getRoutingSettings().getRegistrationInfoServicesStore(), new JAPRoutingMessage(JAPRoutingMessage.REGISTRATION_INFOSERVICES_LIST_CHANGED));
    registrationInfoServicesObserver.update(JAPModel.getInstance().getRoutingSettings().getRegistrationInfoServicesStore(), new JAPRoutingMessage(JAPRoutingMessage.REGISTRATION_INFOSERVICES_POLICY_CHANGED));

    TitledBorder settingsRoutingServerConfigExpertRegistrationInfoServicesBorder = new TitledBorder(JAPMessages.getString("settingsRoutingServerConfigExpertRegistrationInfoServicesBorderTitle"));
    settingsRoutingServerConfigExpertRegistrationInfoServicesBorder.setTitleFont(getFontSetting());
    configExpertInfoServicePanel.setBorder(settingsRoutingServerConfigExpertRegistrationInfoServicesBorder);

    GridBagLayout configExpertInfoServicePanelLayout = new GridBagLayout();
    configExpertInfoServicePanel.setLayout(configExpertInfoServicePanelLayout);

    GridBagConstraints configExpertInfoServicePanelConstraints = new GridBagConstraints();
    configExpertInfoServicePanelConstraints.anchor = GridBagConstraints.NORTHWEST;
    configExpertInfoServicePanelConstraints.fill = GridBagConstraints.NONE;
    configExpertInfoServicePanelConstraints.weightx = 1.0;
    configExpertInfoServicePanelConstraints.weighty = 0.0;

    configExpertInfoServicePanelConstraints.gridx = 0;
    configExpertInfoServicePanelConstraints.gridy = 0;
    configExpertInfoServicePanelConstraints.gridwidth = 2;
    configExpertInfoServicePanelConstraints.insets = new Insets(0, 5, 10, 5);
    configExpertInfoServicePanelLayout.setConstraints(settingsRoutingServerConfigExpertRegistrationInfoServicesRegisterAtAllBox, configExpertInfoServicePanelConstraints);
    configExpertInfoServicePanel.add(settingsRoutingServerConfigExpertRegistrationInfoServicesRegisterAtAllBox);

    configExpertInfoServicePanelConstraints.gridx = 0;
    configExpertInfoServicePanelConstraints.gridy = 1;
    configExpertInfoServicePanelConstraints.gridwidth = 1;
    configExpertInfoServicePanelConstraints.insets = new Insets(0, 5, 0, 5);
    configExpertInfoServicePanelLayout.setConstraints(settingsRoutingServerConfigExpertRegistrationInfoServicesKnownInfoServicesLabel, configExpertInfoServicePanelConstraints);
    configExpertInfoServicePanel.add(settingsRoutingServerConfigExpertRegistrationInfoServicesKnownInfoServicesLabel);

    configExpertInfoServicePanelConstraints.gridx = 1;
    configExpertInfoServicePanelConstraints.gridy = 1;
    configExpertInfoServicePanelConstraints.insets = new Insets(0, 5, 0, 5);
    configExpertInfoServicePanelLayout.setConstraints(settingsRoutingServerConfigExpertRegistrationInfoServicesSelectedInfoServicesLabel, configExpertInfoServicePanelConstraints);
    configExpertInfoServicePanel.add(settingsRoutingServerConfigExpertRegistrationInfoServicesSelectedInfoServicesLabel);

    configExpertInfoServicePanelConstraints.gridx = 0;
    configExpertInfoServicePanelConstraints.gridy = 2;
    configExpertInfoServicePanelConstraints.weighty = 1.0;
    configExpertInfoServicePanelConstraints.fill = GridBagConstraints.BOTH;
    configExpertInfoServicePanelConstraints.insets = new Insets(0, 5, 0, 5);
    configExpertInfoServicePanelLayout.setConstraints(knownInfoServicesScrollPane, configExpertInfoServicePanelConstraints);
    configExpertInfoServicePanel.add(knownInfoServicesScrollPane);

    configExpertInfoServicePanelConstraints.gridx = 1;
    configExpertInfoServicePanelConstraints.gridy = 2;
    configExpertInfoServicePanelConstraints.insets = new Insets(0, 5, 0, 5);
    configExpertInfoServicePanelLayout.setConstraints(registrationInfoServicesScrollPane, configExpertInfoServicePanelConstraints);
    configExpertInfoServicePanel.add(registrationInfoServicesScrollPane);

    configExpertInfoServicePanelConstraints.gridx = 0;
    configExpertInfoServicePanelConstraints.gridy = 3;
    configExpertInfoServicePanelConstraints.weighty = 0.0;
    configExpertInfoServicePanelConstraints.fill = GridBagConstraints.HORIZONTAL;
    configExpertInfoServicePanelConstraints.insets = new Insets(5, 5, 5, 5);
    configExpertInfoServicePanelLayout.setConstraints(settingsRoutingServerConfigExpertRegistrationInfoServicesAddButton, configExpertInfoServicePanelConstraints);
    configExpertInfoServicePanel.add(settingsRoutingServerConfigExpertRegistrationInfoServicesAddButton);

    configExpertInfoServicePanelConstraints.gridx = 1;
    configExpertInfoServicePanelConstraints.gridy = 3;
    configExpertInfoServicePanelConstraints.insets = new Insets(5, 5, 5, 5);
    configExpertInfoServicePanelLayout.setConstraints(settingsRoutingServerConfigExpertRegistrationInfoServicesRemoveButton, configExpertInfoServicePanelConstraints);
    configExpertInfoServicePanel.add(settingsRoutingServerConfigExpertRegistrationInfoServicesRemoveButton);

    final EmbeddedButton settingsRoutingServerConfigExpertCascadesEditButton = new EmbeddedButton(JAPMessages.getString("settingsRoutingServerConfigExpertCascadesEditButton") + " >>>");
    final EmbeddedButton settingsRoutingServerConfigExpertRegistrationInfoservicesButton = new EmbeddedButton(JAPMessages.getString("settingsRoutingServerConfigExpertRegistrationInfoservicesButton") + " >>>");

    settingsRoutingServerConfigExpertCascadesEditButton.setFont(getFontSetting());
    settingsRoutingServerConfigExpertCascadesEditButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent event)
      {
        if (configExpertCascadesPanel.isVisible() == false) {
          configExpertInfoServicePanel.setVisible(false);
          synchronized (knownCascadesListModel) {
            knownCascadesListModel.clear();
            Enumeration fetchedCascades = showFetchMixCascadesDialog(configExpertPanel).elements();
            while (fetchedCascades.hasMoreElements()) {
              knownCascadesListModel.addElement(fetchedCascades.nextElement());
            }
          }
          settingsRoutingServerConfigExpertCascadesEditButton.setText("<<< " + JAPMessages.getString("settingsRoutingServerConfigExpertCascadesEditButton"));
          settingsRoutingServerConfigExpertRegistrationInfoservicesButton.setText(JAPMessages.getString("settingsRoutingServerConfigExpertRegistrationInfoservicesButton") + " >>>");
          configExpertCascadesPanel.setVisible(true);
        }
        else {
          configExpertCascadesPanel.setVisible(false);
          settingsRoutingServerConfigExpertCascadesEditButton.setText(JAPMessages.getString("settingsRoutingServerConfigExpertCascadesEditButton") + " >>>");
        }          
      }
    });

    settingsRoutingServerConfigExpertRegistrationInfoservicesButton.setFont(getFontSetting());
    settingsRoutingServerConfigExpertRegistrationInfoservicesButton.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent event)
      {
        if (configExpertInfoServicePanel.isVisible() == false) {
          configExpertCascadesPanel.setVisible(false);
          synchronized (knownInfoServicesListModel) {
            knownInfoServicesListModel.clear();
            Enumeration knownInfoServices = InfoServiceHolder.getInstance().getInfoservicesWithForwarderList().elements();
            while (knownInfoServices.hasMoreElements()) {
              knownInfoServicesListModel.addElement(knownInfoServices.nextElement());
            }
          }
          settingsRoutingServerConfigExpertRegistrationInfoservicesButton.setText("<<< " + JAPMessages.getString("settingsRoutingServerConfigExpertRegistrationInfoservicesButton"));
          settingsRoutingServerConfigExpertCascadesEditButton.setText(JAPMessages.getString("settingsRoutingServerConfigExpertCascadesEditButton") + " >>>");
          configExpertInfoServicePanel.setVisible(true);
        }
        else {
          configExpertInfoServicePanel.setVisible(false);
          settingsRoutingServerConfigExpertRegistrationInfoservicesButton.setText(JAPMessages.getString("settingsRoutingServerConfigExpertRegistrationInfoservicesButton") + " >>>");                 
        }
      }
    });

    EmbeddedButton settingsRoutingServerConfigBasicButton = new EmbeddedButton(JAPMessages.getString("settingsRoutingServerConfigBasicButton"));
    settingsRoutingServerConfigBasicButton.setFont(getFontSetting());
    settingsRoutingServerConfigBasicButton.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent event)
      {
        /* if the basic settings button is pressed, show the server basic settings panel and
         * hide the expert settings
         */
        configExpertPanel.setVisible(false);
        configBasicPanel.setVisible(true);
      }
    });

    /* only a spaceholder for the allowed cascades and infoservice registration panel */
    JPanel configExpertSpaceHolderPanel = new JPanel();
        
    GridBagLayout configExpertSpaceHolderPanelLayout = new GridBagLayout();
    configExpertSpaceHolderPanel.setLayout(configExpertSpaceHolderPanelLayout);
    GridBagConstraints configExpertSpaceHolderPanelConstraints = new GridBagConstraints();

    configExpertSpaceHolderPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
    configExpertSpaceHolderPanelConstraints.fill = GridBagConstraints.BOTH;
    configExpertSpaceHolderPanelConstraints.gridx = 0;
    configExpertSpaceHolderPanelConstraints.gridy = 0;    
    configExpertSpaceHolderPanelConstraints.weightx = 1.0;    
    configExpertSpaceHolderPanelConstraints.weighty = 1.0;    
    /* the allowed cascades and registration infoservices panel shall be at the same position, but
     * never more than one of the both is visible
     */
    configExpertSpaceHolderPanelLayout.setConstraints(configExpertCascadesPanel, configExpertSpaceHolderPanelConstraints);
    configExpertSpaceHolderPanel.add(configExpertCascadesPanel);
    configExpertSpaceHolderPanelLayout.setConstraints(configExpertInfoServicePanel, configExpertSpaceHolderPanelConstraints);
    configExpertSpaceHolderPanel.add(configExpertInfoServicePanel);

    GridBagLayout configExpertPanelLayout = new GridBagLayout();
    configExpertPanel.setLayout(configExpertPanelLayout);
    GridBagConstraints configExpertPanelConstraints = new GridBagConstraints();

    configExpertPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
    configExpertPanelConstraints.fill = GridBagConstraints.NONE;
    configExpertPanelConstraints.gridx = 0;
    configExpertPanelConstraints.gridy = 0;
    configExpertPanelConstraints.gridwidth = 2;
    configExpertPanelConstraints.weightx = 1.0;
    configExpertPanelConstraints.weighty = 0.0;    
    configExpertPanelConstraints.insets = new Insets(2, 0, 2, 0);
    configExpertPanelLayout.setConstraints(settingsRoutingServerConfigBasicButton, configExpertPanelConstraints);
    configExpertPanel.add(settingsRoutingServerConfigBasicButton);

    configExpertPanelConstraints.fill = GridBagConstraints.BOTH;
    configExpertPanelConstraints.gridx = 0;
    configExpertPanelConstraints.gridy = 1;
    configExpertPanelLayout.setConstraints(configExpertSettingsPanel, configExpertPanelConstraints);
    configExpertPanel.add(configExpertSettingsPanel);

    configExpertPanelConstraints.fill = GridBagConstraints.NONE;
    configExpertPanelConstraints.gridx = 0;
    configExpertPanelConstraints.gridy = 2;
    configExpertPanelConstraints.gridwidth = 1;
    configExpertPanelConstraints.weightx = 0.0;
    configExpertPanelConstraints.insets = new Insets(2, 0, 2, 5);
    configExpertPanelLayout.setConstraints(settingsRoutingServerConfigExpertCascadesEditButton, configExpertPanelConstraints);
    configExpertPanel.add(settingsRoutingServerConfigExpertCascadesEditButton);

    configExpertPanelConstraints.gridx = 1;
    configExpertPanelConstraints.gridy = 2;
    configExpertPanelConstraints.insets = new Insets(2, 5, 2, 0);
    configExpertPanelConstraints.weightx = 1.0;
    configExpertPanelLayout.setConstraints(settingsRoutingServerConfigExpertRegistrationInfoservicesButton, configExpertPanelConstraints);
    configExpertPanel.add(settingsRoutingServerConfigExpertRegistrationInfoservicesButton);

    configExpertPanelConstraints.fill = GridBagConstraints.BOTH;
    configExpertPanelConstraints.gridx = 0;
    configExpertPanelConstraints.gridy = 3;
    configExpertPanelConstraints.gridwidth = 2;
    configExpertPanelConstraints.weighty = 1.0;
    configExpertPanelConstraints.insets = new Insets(0, 0, 0, 0);
    configExpertPanelLayout.setConstraints(configExpertSpaceHolderPanel, configExpertPanelConstraints);
    configExpertPanel.add(configExpertSpaceHolderPanel);

    /* set the preferred size of the expert panel, before the infoservice and cascade panels are
     * made invisible -> the both panels are taken into account when calculating the peferred size
     */
    configExpertPanel.setPreferredSize(configExpertPanel.getPreferredSize());
    configExpertCascadesPanel.setVisible(false);
    configExpertInfoServicePanel.setVisible(false);
    
    GridBagLayout configPanelLayout = new GridBagLayout();
    configPanel.setLayout(configPanelLayout);

    GridBagConstraints configPanelConstraints = new GridBagConstraints();
    configPanelConstraints.weightx = 1.0;
    configPanelConstraints.weighty = 1.0;
    configPanelConstraints.gridx = 0;
    configPanelConstraints.gridy = 0;
    configPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
    configPanelConstraints.fill = GridBagConstraints.BOTH;
    /* the expert and the basic panel shall be at the same position, but always only one of the
     * both is visible
     */
    configPanelLayout.setConstraints(configBasicPanel, configPanelConstraints);
    configPanel.add(configBasicPanel);    
    configPanelLayout.setConstraints(configExpertPanel, configPanelConstraints);
    configPanel.add(configExpertPanel);    

    /* set the preferred size of the config panel, before the expert panel is made invisible
     * -> the expert panel is taken into account when calculating the peferred size
     */
    configPanel.setPreferredSize(configPanel.getPreferredSize());
    configExpertPanel.setVisible(false);

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
        JAPModel.getInstance().getRoutingSettings().setRoutingMode(JAPRoutingSettings.ROUTING_MODE_DISABLED);
        registerDialog.dispose();
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

    final Thread registerThread = new Thread(new Runnable()
    {
      public void run()
      {
        int registrationStatus = JAPModel.getInstance().getRoutingSettings().startPropaganda(true);
        if (registrationStatus == JAPRoutingSettings.REGISTRATION_NO_INFOSERVICES) {
          JOptionPane.showMessageDialog(registerPanel, new JAPHtmlMultiLineLabel(JAPMessages.getString("settingsRoutingServerRegistrationEmptyListError"), getFontSetting()), JAPMessages.getString("ERROR"), JOptionPane.ERROR_MESSAGE);
        }
        if (registrationStatus == JAPRoutingSettings.REGISTRATION_UNKNOWN_ERRORS) {
          JOptionPane.showMessageDialog(registerPanel, new JAPHtmlMultiLineLabel(JAPMessages.getString("settingsRoutingServerRegistrationUnknownError"), getFontSetting()), JAPMessages.getString("ERROR"), JOptionPane.ERROR_MESSAGE);
        }
        if (registrationStatus == JAPRoutingSettings.REGISTRATION_INFOSERVICE_ERRORS) {
          JOptionPane.showMessageDialog(registerPanel, new JAPHtmlMultiLineLabel(JAPMessages.getString("settingsRoutingServerRegistrationInfoservicesError"), getFontSetting()), JAPMessages.getString("ERROR"), JOptionPane.ERROR_MESSAGE);
        }
        if (registrationStatus == JAPRoutingSettings.REGISTRATION_VERIFY_ERRORS) {
          JOptionPane.showMessageDialog(registerPanel, new JAPHtmlMultiLineLabel(JAPMessages.getString("settingsRoutingServerRegistrationVerificationError"), getFontSetting()), JAPMessages.getString("ERROR"), JOptionPane.ERROR_MESSAGE);
        }
        if (registrationStatus != JAPRoutingSettings.REGISTRATION_INTERRUPTED) {
          /* if the registration was interrupted, the dialog is already hidden */
          registerDialog.dispose();
        }
      }
    });

    /* for synchronization purposes, it is necessary to show the dialog first and start the thread
     * after that event
     */
    registerDialog.getInternalDialog().addWindowListener(new WindowAdapter() {
      public void windowOpened(WindowEvent a_event) {
        registerDialog.getInternalDialog().removeWindowListener(this);
        registerThread.start();
      }
    });

    registerDialog.show();
  }    

  /**
   * Creates the routing server status panel. This panel should only be visible, if we are in
   * server routing mode.
   *
   * @return The status panel which is shown, if the forwarding server is running.
   */
  private JPanel createRoutingServerStatusPanel()
  {
    /* get the NumberFormat instance for formating the bandwidth (double) */
    final NumberFormat bandwidthFormat = NumberFormat.getInstance();
    bandwidthFormat.setMinimumFractionDigits(1);
    bandwidthFormat.setMaximumFractionDigits(1);
    bandwidthFormat.setMinimumIntegerDigits(1);

    /* get the NumberFormat instance for formating the other numbers (integer) */
    final NumberFormat integerFormat = NumberFormat.getInstance();
    bandwidthFormat.setMinimumIntegerDigits(1);

    final JAPHtmlMultiLineLabel settingsRoutingServerStatusLabel = new JAPHtmlMultiLineLabel("", getFontSetting());
    final JLabel settingsRoutingServerStatusRegistrationErrorLabel = new JLabel();
    settingsRoutingServerStatusRegistrationErrorLabel.setFont(getFontSetting());

    final JLabel settingsRoutingServerStatusStatisticsBandwidthLabel = new JLabel();
    settingsRoutingServerStatusStatisticsBandwidthLabel.setFont(getFontSetting());
    final JLabel settingsRoutingServerStatusStatisticsForwardedBytesLabel = new JLabel();
    settingsRoutingServerStatusStatisticsForwardedBytesLabel.setFont(getFontSetting());
    JLabel settingsRoutingServerStatusStatisticsConnectionsLabel = new JLabel(JAPMessages.getString("settingsRoutingServerStatusStatisticsConnectionsLabel"));
    settingsRoutingServerStatusStatisticsConnectionsLabel.setFont(getFontSetting());
    final JLabel settingsRoutingServerStatusStatisticsCurrentConnectionsLabel = new JLabel();
    settingsRoutingServerStatusStatisticsCurrentConnectionsLabel.setFont(getFontSetting());
    final JLabel settingsRoutingServerStatusStatisticsAcceptedConnectionsLabel = new JLabel();
    settingsRoutingServerStatusStatisticsAcceptedConnectionsLabel.setFont(getFontSetting());
    final JLabel settingsRoutingServerStatusStatisticsRejectedConnectionsLabel = new JLabel();
    settingsRoutingServerStatusStatisticsRejectedConnectionsLabel.setFont(getFontSetting());

    JLabel settingsRoutingServerStatusInfoServiceRegistrationsLabel = new JLabel(JAPMessages.getString("settingsRoutingServerStatusInfoServiceRegistrationsLabel"));
    settingsRoutingServerStatusInfoServiceRegistrationsLabel.setFont(getFontSetting());

    final JAPRoutingInfoServiceRegistrationTableModel infoServiceRegistrationTableModel = new JAPRoutingInfoServiceRegistrationTableModel();
    JTable infoServiceRegistrationTable = new JTable(infoServiceRegistrationTableModel);
    infoServiceRegistrationTable.setFont(getFontSetting());
    infoServiceRegistrationTable.getColumnModel().getColumn(1).setMaxWidth(125);
    infoServiceRegistrationTable.getColumnModel().getColumn(1).setPreferredWidth(125);
    infoServiceRegistrationTable.setEnabled(false);
    infoServiceRegistrationTable.getTableHeader().setFont(getFontSetting());
    infoServiceRegistrationTable.getTableHeader().setResizingAllowed(false);
    infoServiceRegistrationTable.getTableHeader().setReorderingAllowed(false);
    JScrollPane infoServiceRegistrationTableScrollPane = new JScrollPane(infoServiceRegistrationTable);
    infoServiceRegistrationTableScrollPane.setPreferredSize(new Dimension(infoServiceRegistrationTableScrollPane.getPreferredSize().width, 50));

    Observer serverStatusObserver = new Observer() {
      /**
       * This is the observer implementation. If there are new server statistics available, new
       * propganda instances started or the server status has been changed, the components on the
       * server status panel are updated. If the panel is recreated (message via the module
       * internal message system), the observer removes itself from all observed objects.
       *
       * @param a_notifier The observed Object. This should always be
       *                   JAPRoutingServerStatisticsListener, JAPRoutingSettings or the module
       *                   internal message system at the moment.
       * @param a_message The reason of the notification. This should always be a JAPRoutingMessage
       *                  or null at the moment.
       */
      public void update(Observable a_notifier, Object a_message) {
        try {
          if (a_notifier == JAPModel.getInstance().getRoutingSettings().getServerStatisticsListener()) {
            if (((JAPRoutingMessage)(a_message)).getMessageCode() == JAPRoutingMessage.SERVER_STATISTICS_UPDATED) {
              /* statistics might have been changed */
              settingsRoutingServerStatusStatisticsBandwidthLabel.setText(JAPMessages.getString("settingsRoutingServerStatusStatisticsBandwidthLabelPart1") + " " + bandwidthFormat.format( ( (double) (JAPModel.getInstance().getRoutingSettings().getServerStatisticsListener().getCurrentBandwidthUsage())) / (double) 1024) + " " + JAPMessages.getString("settingsRoutingServerStatusStatisticsBandwidthLabelPart2"));
              settingsRoutingServerStatusStatisticsForwardedBytesLabel.setText(JAPMessages.getString("settingsRoutingServerStatusStatisticsForwardedBytesLabel") + " " + integerFormat.format(JAPModel.getInstance().getRoutingSettings().getServerStatisticsListener().getTransferedBytes()));
              settingsRoutingServerStatusStatisticsCurrentConnectionsLabel.setText(JAPMessages.getString("settingsRoutingServerStatusStatisticsCurrentConnectionsLabel") + " " + integerFormat.format(JAPModel.getInstance().getRoutingSettings().getServerStatisticsListener().getCurrentlyForwardedConnections()));
              settingsRoutingServerStatusStatisticsAcceptedConnectionsLabel.setText(JAPMessages.getString("settingsRoutingServerStatusStatisticsAcceptedConnectionsLabel") + " " + integerFormat.format(JAPModel.getInstance().getRoutingSettings().getServerStatisticsListener().getAcceptedConnections()));
              settingsRoutingServerStatusStatisticsRejectedConnectionsLabel.setText(JAPMessages.getString("settingsRoutingServerStatusStatisticsRejectedConnectionsLabel") + " " + integerFormat.format(JAPModel.getInstance().getRoutingSettings().getServerStatisticsListener().getRejectedConnections()));
            }
          }
          if (a_notifier == JAPModel.getInstance().getRoutingSettings().getRegistrationStatusObserver()) {
            if (((JAPRoutingMessage)(a_message)).getMessageCode() == JAPRoutingMessage.REGISTRATION_STATUS_CHANGED) {
              /* update the server state label and the reason of error, if necessary */
              int currentRegistrationState = JAPModel.getInstance().getRoutingSettings().getRegistrationStatusObserver().getCurrentState();
              int currentErrorCode = JAPModel.getInstance().getRoutingSettings().getRegistrationStatusObserver().getCurrentErrorCode();
              if (currentRegistrationState == JAPRoutingRegistrationStatusObserver.STATE_DISABLED) {
                settingsRoutingServerStatusLabel.changeText(JAPMessages.getString("settingsRoutingServerStatusLabelStateRegistrationDisabled"));
              }
              else if (currentRegistrationState == JAPRoutingRegistrationStatusObserver.STATE_INITIAL_REGISTRATION) {
                settingsRoutingServerStatusLabel.changeText(JAPMessages.getString("settingsRoutingServerStatusLabelStateRegistrationInitiated"));
              }  
              else if (currentRegistrationState == JAPRoutingRegistrationStatusObserver.STATE_NO_REGISTRATION) {
                settingsRoutingServerStatusLabel.changeText(JAPMessages.getString("settingsRoutingServerStatusLabelStateRegistrationFailed"));
              }
              else if (currentRegistrationState == JAPRoutingRegistrationStatusObserver.STATE_SUCCESSFUL_REGISTRATION) {
                settingsRoutingServerStatusLabel.changeText(JAPMessages.getString("settingsRoutingServerStatusLabelStateRegistrationSuccessful"));
              }
              if (currentErrorCode == JAPRoutingRegistrationStatusObserver.ERROR_NO_ERROR) {
                settingsRoutingServerStatusRegistrationErrorLabel.setText(" ");
              }
              else if (currentErrorCode == JAPRoutingRegistrationStatusObserver.ERROR_NO_KNOWN_PRIMARY_INFOSERVICES) {
                settingsRoutingServerStatusRegistrationErrorLabel.setText(JAPMessages.getString("settingsRoutingServerStatusRegistrationErrorLabelNoKnownInfoServices"));
              }  
              else if (currentErrorCode == JAPRoutingRegistrationStatusObserver.ERROR_INFOSERVICE_CONNECT_ERROR) {
                settingsRoutingServerStatusRegistrationErrorLabel.setText(JAPMessages.getString("settingsRoutingServerStatusRegistrationErrorLabelConnectionFailed"));
              }
              else if (currentErrorCode == JAPRoutingRegistrationStatusObserver.ERROR_VERIFICATION_ERROR) {
                settingsRoutingServerStatusRegistrationErrorLabel.setText(JAPMessages.getString("settingsRoutingServerStatusRegistrationErrorLabelVerificationFailed"));
              }              
              else if (currentErrorCode == JAPRoutingRegistrationStatusObserver.ERROR_UNKNOWN_ERROR) {
                settingsRoutingServerStatusRegistrationErrorLabel.setText(JAPMessages.getString("settingsRoutingServerStatusRegistrationErrorLabelUnknownReason"));
              }
            }  
          }  
          if (a_notifier == JAPModel.getInstance().getRoutingSettings()) {
            if (((JAPRoutingMessage)(a_message)).getMessageCode() == JAPRoutingMessage.PROPAGANDA_INSTANCES_ADDED) {
              /* update the propagandists in the infoservice registration table */
              infoServiceRegistrationTableModel.updatePropagandaInstancesList((Vector)(((JAPRoutingMessage)a_message).getMessageData()));
            }
          }
          if (a_notifier == m_messageSystem) {
            /* the root panel was recreated -> stop observing and remove ourself from the observed
             * objects
             */
            JAPModel.getInstance().getRoutingSettings().getServerStatisticsListener().deleteObserver(this);
            JAPModel.getInstance().getRoutingSettings().deleteObserver(this);
            m_messageSystem.deleteObserver(this);
            /* also stop observing of the propaganda instances from the
             * InfoServiceRegistrationTableModel
             */
            infoServiceRegistrationTableModel.clearPropagandaInstancesTable(); 
          }
        }
        catch (Exception e) {
          /* should not happen */
          LogHolder.log(LogLevel.EXCEPTION, LogType.GUI, e);
        }
      }
    };
    /* registrate the observer also at the internal message system */
    m_messageSystem.addObserver(serverStatusObserver);    
    JAPModel.getInstance().getRoutingSettings().getServerStatisticsListener().addObserver(serverStatusObserver);
    JAPModel.getInstance().getRoutingSettings().getRegistrationStatusObserver().addObserver(serverStatusObserver);
    JAPModel.getInstance().getRoutingSettings().addObserver(serverStatusObserver);
    /* tricky: initialize the labels by calling the observer (with all possible messages) */
    serverStatusObserver.update(JAPModel.getInstance().getRoutingSettings().getServerStatisticsListener(), new JAPRoutingMessage(JAPRoutingMessage.SERVER_STATISTICS_UPDATED));
    serverStatusObserver.update(JAPModel.getInstance().getRoutingSettings().getRegistrationStatusObserver(), new JAPRoutingMessage(JAPRoutingMessage.REGISTRATION_STATUS_CHANGED));
    /* initialize the propaganda instances table */
    infoServiceRegistrationTableModel.updatePropagandaInstancesList(JAPModel.getInstance().getRoutingSettings().getRunningPropagandaInstances());

    JPanel serverStatusPanel = new JPanel();

    TitledBorder settingsRoutingServerStatusBorder = new TitledBorder(JAPMessages.getString("settingsRoutingServerStatusBorder"));
    settingsRoutingServerStatusBorder.setTitleFont(getFontSetting());
    serverStatusPanel.setBorder(settingsRoutingServerStatusBorder);

    GridBagLayout serverStatusPanelLayout = new GridBagLayout();
    serverStatusPanel.setLayout(serverStatusPanelLayout);

    GridBagConstraints serverStatusPanelConstraints = new GridBagConstraints();
    serverStatusPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
    serverStatusPanelConstraints.fill = GridBagConstraints.NONE;
    serverStatusPanelConstraints.weightx = 1.0;
    serverStatusPanelConstraints.weighty = 0.0;
    serverStatusPanelConstraints.gridx = 0;
    serverStatusPanelConstraints.gridy = 0;
    serverStatusPanelConstraints.insets = new Insets(5, 5, 5, 5);
    serverStatusPanelLayout.setConstraints(settingsRoutingServerStatusLabel, serverStatusPanelConstraints);
    serverStatusPanel.add(settingsRoutingServerStatusLabel);

    serverStatusPanelConstraints.gridx = 0;
    serverStatusPanelConstraints.gridy = 1;
    serverStatusPanelConstraints.insets = new Insets(0, 5, 20, 5);
    serverStatusPanelLayout.setConstraints(settingsRoutingServerStatusRegistrationErrorLabel, serverStatusPanelConstraints);
    serverStatusPanel.add(settingsRoutingServerStatusRegistrationErrorLabel);

    serverStatusPanelConstraints.gridx = 0;
    serverStatusPanelConstraints.gridy = 2;
    serverStatusPanelConstraints.insets = new Insets(0, 5, 0, 5);
    serverStatusPanelLayout.setConstraints(settingsRoutingServerStatusInfoServiceRegistrationsLabel, serverStatusPanelConstraints);
    serverStatusPanel.add(settingsRoutingServerStatusInfoServiceRegistrationsLabel);

    serverStatusPanelConstraints.fill = GridBagConstraints.BOTH;
    serverStatusPanelConstraints.weighty = 1.0;
    serverStatusPanelConstraints.gridx = 0;
    serverStatusPanelConstraints.gridy = 3;
    serverStatusPanelConstraints.insets = new Insets(0, 5, 5, 5);
    serverStatusPanelLayout.setConstraints(infoServiceRegistrationTableScrollPane, serverStatusPanelConstraints);
    serverStatusPanel.add(infoServiceRegistrationTableScrollPane);

    JPanel serverStatusStatisticsPanel = new JPanel();

    TitledBorder settingsRoutingServerStatusStatisticsBorder = new TitledBorder(JAPMessages.getString("settingsRoutingServerStatusStatisticsBorder"));
    settingsRoutingServerStatusStatisticsBorder.setTitleFont(getFontSetting());
    serverStatusStatisticsPanel.setBorder(settingsRoutingServerStatusStatisticsBorder);

    GridBagLayout serverStatusStatisticsPanelLayout = new GridBagLayout();
    serverStatusStatisticsPanel.setLayout(serverStatusStatisticsPanelLayout);

    GridBagConstraints serverStatusStatisticsPanelConstraints = new GridBagConstraints();
    serverStatusStatisticsPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
    serverStatusStatisticsPanelConstraints.fill = GridBagConstraints.NONE;
    serverStatusStatisticsPanelConstraints.weightx = 1.0;
    serverStatusStatisticsPanelConstraints.weighty = 0.0;
    serverStatusStatisticsPanelConstraints.gridx = 0;
    serverStatusStatisticsPanelConstraints.gridy = 0;
    serverStatusStatisticsPanelConstraints.gridwidth = 4;
    serverStatusStatisticsPanelConstraints.insets = new Insets(5, 5, 10, 5);
    serverStatusStatisticsPanelLayout.setConstraints(settingsRoutingServerStatusStatisticsBandwidthLabel, serverStatusStatisticsPanelConstraints);
    serverStatusStatisticsPanel.add(settingsRoutingServerStatusStatisticsBandwidthLabel);

    serverStatusStatisticsPanelConstraints.gridx = 0;
    serverStatusStatisticsPanelConstraints.gridy = 1;
    serverStatusStatisticsPanelConstraints.insets = new Insets(0, 5, 10, 5);
    serverStatusStatisticsPanelLayout.setConstraints(settingsRoutingServerStatusStatisticsForwardedBytesLabel, serverStatusStatisticsPanelConstraints);
    serverStatusStatisticsPanel.add(settingsRoutingServerStatusStatisticsForwardedBytesLabel);

    serverStatusStatisticsPanelConstraints.gridx = 0;
    serverStatusStatisticsPanelConstraints.gridy = 2;
    serverStatusStatisticsPanelConstraints.weighty = 1.0;
    serverStatusStatisticsPanelConstraints.weightx = 0.0;
    serverStatusStatisticsPanelConstraints.gridwidth = 1;
    serverStatusStatisticsPanelConstraints.insets = new Insets(0, 5, 5, 15);
    serverStatusStatisticsPanelLayout.setConstraints(settingsRoutingServerStatusStatisticsConnectionsLabel, serverStatusStatisticsPanelConstraints);
    serverStatusStatisticsPanel.add(settingsRoutingServerStatusStatisticsConnectionsLabel);

    serverStatusStatisticsPanelConstraints.gridx = 1;
    serverStatusStatisticsPanelConstraints.gridy = 2;
    serverStatusStatisticsPanelConstraints.insets = new Insets(0, 0, 5, 15);
    serverStatusStatisticsPanelLayout.setConstraints(settingsRoutingServerStatusStatisticsCurrentConnectionsLabel, serverStatusStatisticsPanelConstraints);
    serverStatusStatisticsPanel.add(settingsRoutingServerStatusStatisticsCurrentConnectionsLabel);

    serverStatusStatisticsPanelConstraints.gridx = 2;
    serverStatusStatisticsPanelConstraints.gridy = 2;
    serverStatusStatisticsPanelConstraints.insets = new Insets(0, 0, 5, 15);
    serverStatusStatisticsPanelLayout.setConstraints(settingsRoutingServerStatusStatisticsAcceptedConnectionsLabel, serverStatusStatisticsPanelConstraints);
    serverStatusStatisticsPanel.add(settingsRoutingServerStatusStatisticsAcceptedConnectionsLabel);

    serverStatusStatisticsPanelConstraints.gridx = 3;
    serverStatusStatisticsPanelConstraints.gridy = 2;
    serverStatusStatisticsPanelConstraints.weightx = 1.0;
    serverStatusStatisticsPanelConstraints.insets = new Insets(0, 0, 5, 5);
    serverStatusStatisticsPanelLayout.setConstraints(settingsRoutingServerStatusStatisticsRejectedConnectionsLabel, serverStatusStatisticsPanelConstraints);
    serverStatusStatisticsPanel.add(settingsRoutingServerStatusStatisticsRejectedConnectionsLabel);

    JPanel serverStatusAllPanel = new JPanel();
    
    GridBagLayout serverStatusAllPanelLayout = new GridBagLayout();
    serverStatusAllPanel.setLayout(serverStatusAllPanelLayout);

    GridBagConstraints serverStatusAllPanelConstraints = new GridBagConstraints();
    serverStatusAllPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
    serverStatusAllPanelConstraints.fill = GridBagConstraints.BOTH;
    serverStatusAllPanelConstraints.weightx = 1.0;
    serverStatusAllPanelConstraints.weighty = 1.0;
    serverStatusAllPanelConstraints.gridx = 0;
    serverStatusAllPanelConstraints.gridy = 0;
    serverStatusAllPanelLayout.setConstraints(serverStatusPanel, serverStatusAllPanelConstraints);
    serverStatusAllPanel.add(serverStatusPanel);

    serverStatusAllPanelConstraints.weighty = 0.0;
    serverStatusAllPanelConstraints.gridx = 0;
    serverStatusAllPanelConstraints.gridy = 1;
    serverStatusAllPanelLayout.setConstraints(serverStatusStatisticsPanel, serverStatusAllPanelConstraints);
    serverStatusAllPanel.add(serverStatusStatisticsPanel);
    
    return serverStatusAllPanel;
  }

  /**
   * Creates the routing client status panel. This panel should only be visible, if we are in
   * client routing mode.
   *
   * @return The status panel which is shown, if the forwarding client is running.
   */
  private JPanel createRoutingClientStatusPanel()
  {
    JPanel clientStatusPanel = new JPanel();

    JLabel settingsRoutingClientStatusClientRunningLabel = new JLabel(JAPMessages.getString("settingsRoutingClientStatusClientRunningLabel"));
    settingsRoutingClientStatusClientRunningLabel.setFont(getFontSetting());
    JLabel settingsRoutingClientStatusConnectedViaLabel = new JLabel(JAPMessages.getString("settingsRoutingClientStatusConnectedViaLabel"));
    settingsRoutingClientStatusConnectedViaLabel.setFont(getFontSetting());
    final JLabel settingsRoutingClientStatusForwarderInformationLabel = new JLabel();
    settingsRoutingClientStatusForwarderInformationLabel.setFont(getFontSetting());
    
    Observer clientStatusObserver = new Observer() {
      /**
       * This is the observer implementation. If we get connected via a new forwarding server, the
       * client status with the information about the forwarder is updated. If the panel is
       * recreated (message via the module internal message system), the observer removes itself
       * from all observed objects.
       *
       * @param a_notifier The observed Object. This should always be JAPRoutingSettings or the
       *                   module internal message system at the moment.
       * @param a_message The reason of the notification. This should always be a JAPRoutingMessage
       *                  or null at the moment.
       */
      public void update(Observable a_notifier, Object a_message) {
        try {
          if (a_notifier == JAPModel.getInstance().getRoutingSettings()) {
            if (((JAPRoutingMessage)(a_message)).getMessageCode() == JAPRoutingMessage.ROUTING_MODE_CHANGED) {
              if (JAPModel.getInstance().getRoutingSettings().getRoutingMode() == JAPRoutingSettings.ROUTING_MODE_CLIENT) {
                /* we are connected to a new client -> update the forwarder information label */
                ListenerInterface currentForwarder = JAPModel.getInstance().getRoutingSettings().getForwarder();
                if (currentForwarder != null) {
                  settingsRoutingClientStatusForwarderInformationLabel.setText(JAPMessages.getString("settingsRoutingClientStatusForwarderInformationLabelPart1") + " " + currentForwarder.getHost() + "    " + JAPMessages.getString("settingsRoutingClientStatusForwarderInformationLabelPart2") + " " + Integer.toString(currentForwarder.getPort()));
                }
                else {
                  /* should never occur */
                  settingsRoutingClientStatusForwarderInformationLabel.setText(JAPMessages.getString("settingsRoutingClientStatusForwarderInformationLabelInvalid"));
                }
              }
            }
          }
          if (a_notifier == m_messageSystem) {
            /* the root panel was recreated -> stop observing and remove ourself from the observed
             * objects
             */
            JAPModel.getInstance().getRoutingSettings().deleteObserver(this);
            m_messageSystem.deleteObserver(this);
          }
        }
        catch (Exception e) {
          /* should not happen */
          LogHolder.log(LogLevel.EXCEPTION, LogType.GUI, e);
        }
      }
    };
    /* registrate the observer also at the internal message system */
    m_messageSystem.addObserver(clientStatusObserver);    
    JAPModel.getInstance().getRoutingSettings().addObserver(clientStatusObserver);
    /* tricky: initialize the label (if necessary) by calling the observer */
    clientStatusObserver.update(JAPModel.getInstance().getRoutingSettings(), new JAPRoutingMessage(JAPRoutingMessage.ROUTING_MODE_CHANGED));

    TitledBorder settingsRoutingClientStatusBorder = new TitledBorder(JAPMessages.getString("settingsRoutingClientStatusBorder"));
    settingsRoutingClientStatusBorder.setTitleFont(getFontSetting());
    clientStatusPanel.setBorder(settingsRoutingClientStatusBorder);

    GridBagLayout clientStatusPanelLayout = new GridBagLayout();
    clientStatusPanel.setLayout(clientStatusPanelLayout);

    GridBagConstraints clientStatusPanelConstraints = new GridBagConstraints();
    clientStatusPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
    clientStatusPanelConstraints.fill = GridBagConstraints.NONE;
    clientStatusPanelConstraints.weightx = 1.0;
    clientStatusPanelConstraints.weighty = 0.0;
    clientStatusPanelConstraints.gridx = 0;
    clientStatusPanelConstraints.gridy = 0;
    clientStatusPanelConstraints.insets = new Insets(5, 5, 10, 5);
    clientStatusPanelLayout.setConstraints(settingsRoutingClientStatusClientRunningLabel, clientStatusPanelConstraints);
    clientStatusPanel.add(settingsRoutingClientStatusClientRunningLabel);

    clientStatusPanelConstraints.gridx = 0;
    clientStatusPanelConstraints.gridy = 1;
    clientStatusPanelConstraints.insets = new Insets(0, 5, 2, 5);
    clientStatusPanelLayout.setConstraints(settingsRoutingClientStatusConnectedViaLabel, clientStatusPanelConstraints);
    clientStatusPanel.add(settingsRoutingClientStatusConnectedViaLabel);

    clientStatusPanelConstraints.gridx = 0;
    clientStatusPanelConstraints.gridy = 2;
    clientStatusPanelConstraints.weighty = 1.0;
    clientStatusPanelConstraints.insets = new Insets(0, 15, 5, 5);
    clientStatusPanelLayout.setConstraints(settingsRoutingClientStatusForwarderInformationLabel, clientStatusPanelConstraints);
    clientStatusPanel.add(settingsRoutingClientStatusForwarderInformationLabel);

    return clientStatusPanel;
  }

  /**
   * Creates the status panel, for the case of a disabled forwarding server and client. This panel
   * should only be visible, if we are in ROUTING_MODE_DISABLED.
   *
   * @return The status panel which is shown, if neither the forwarding client nor the forwarding
   *         server is running.
   */
  private JPanel createRoutingDisabledStatusPanel()
  {
    JPanel disabledStatusPanel = new JPanel();

    JLabel settingsRoutingDisabledStatusNothingRunningLabel = new JLabel(JAPMessages.getString("settingsRoutingDisabledStatusNothingRunningLabel"));
    settingsRoutingDisabledStatusNothingRunningLabel.setFont(getFontSetting());

    TitledBorder settingsRoutingDisabledStatusBorder = new TitledBorder(JAPMessages.getString("settingsRoutingDisabledStatusBorder"));
    settingsRoutingDisabledStatusBorder.setTitleFont(getFontSetting());
    disabledStatusPanel.setBorder(settingsRoutingDisabledStatusBorder);

    GridBagLayout disabledStatusPanelLayout = new GridBagLayout();
    disabledStatusPanel.setLayout(disabledStatusPanelLayout);

    GridBagConstraints disabledStatusPanelConstraints = new GridBagConstraints();
    disabledStatusPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
    disabledStatusPanelConstraints.fill = GridBagConstraints.NONE;
    disabledStatusPanelConstraints.weightx = 1.0;
    disabledStatusPanelConstraints.weighty = 1.0;
    disabledStatusPanelConstraints.gridx = 0;
    disabledStatusPanelConstraints.gridy = 0;
    disabledStatusPanelConstraints.insets = new Insets(5, 5, 5, 5);
    disabledStatusPanelLayout.setConstraints(settingsRoutingDisabledStatusNothingRunningLabel, disabledStatusPanelConstraints);
    disabledStatusPanel.add(settingsRoutingDisabledStatusNothingRunningLabel);

    return disabledStatusPanel;
  }

  /**
   * Creates the forwarding status panel, which switches between the server status panel, the
   * client status panel and the forwarding-disabled status panel, if the routing mode is changed.
   *
   * @return The panel for the status tab of the forwarding module.
   */
  private JPanel createRoutingStatusPanel() {
    JPanel statusPanel = new JPanel();
    
    /* create all needed panels */
    final JPanel serverStatusPanel = createRoutingServerStatusPanel();
    final JPanel clientStatusPanel = createRoutingClientStatusPanel();
    final JPanel disabledStatusPanel = createRoutingDisabledStatusPanel();
    
    GridBagLayout statusPanelLayout = new GridBagLayout();
    statusPanel.setLayout(statusPanelLayout);

    /* the panels shall be at the same position, but always only one of them is visible according
     * to the routing mode
     */
    GridBagConstraints statusPanelConstraints = new GridBagConstraints();
    statusPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
    statusPanelConstraints.fill = GridBagConstraints.BOTH;
    statusPanelConstraints.weightx = 1.0;
    statusPanelConstraints.weighty = 1.0;
    statusPanelConstraints.gridx = 0;
    statusPanelConstraints.gridy = 0;
    statusPanelLayout.setConstraints(serverStatusPanel, statusPanelConstraints);
    statusPanel.add(serverStatusPanel);
    statusPanelLayout.setConstraints(clientStatusPanel, statusPanelConstraints);
    statusPanel.add(clientStatusPanel);
    statusPanelLayout.setConstraints(disabledStatusPanel, statusPanelConstraints);
    statusPanel.add(disabledStatusPanel);
    
    /* set the preferred size of the status panel, before the currently not needed panels are
     * made invisible -> all panels are taken into account when calculating the peferred size
     */
    statusPanel.setPreferredSize(statusPanel.getPreferredSize());

    Observer forwardingModeObserver = new Observer() {
      /**
       * This is the observer implementation. If the routing mode is changed, we display the
       * correct status panel for the new routing mode. If the panel is recreated (message via
       * the module internal message system), the observer removes itself from all observed
       * objects.
       *
       * @param a_notifier The observed Object. This should always be JAPRoutingSettings or the
       *                   module internal message system at the moment.
       * @param a_message The reason of the notification. This should always be a JAPRoutingMessage
       *                  or null at the moment.
       */
      public void update(Observable a_notifier, Object a_message) {
        try {
          if (a_notifier == JAPModel.getInstance().getRoutingSettings()) {
            if (((JAPRoutingMessage)(a_message)).getMessageCode() == JAPRoutingMessage.ROUTING_MODE_CHANGED) {
              int newRoutingMode = JAPModel.getInstance().getRoutingSettings().getRoutingMode();
              if (newRoutingMode == JAPRoutingSettings.ROUTING_MODE_CLIENT) {
                serverStatusPanel.setVisible(false);
                disabledStatusPanel.setVisible(false);
                clientStatusPanel.setVisible(true);
              }
              if (newRoutingMode == JAPRoutingSettings.ROUTING_MODE_SERVER) {
                clientStatusPanel.setVisible(false);
                disabledStatusPanel.setVisible(false);
                serverStatusPanel.setVisible(true);
              }
              if (newRoutingMode == JAPRoutingSettings.ROUTING_MODE_DISABLED) {
                serverStatusPanel.setVisible(false);
                clientStatusPanel.setVisible(false);
                disabledStatusPanel.setVisible(true);
              }
            }
          }
          if (a_notifier == m_messageSystem) {
            /* the root panel was recreated -> stop observing and remove ourself from the observed
             * objects
             */
            JAPModel.getInstance().getRoutingSettings().deleteObserver(this);
            m_messageSystem.deleteObserver(this);
          }
        }
        catch (Exception e) {
          /* should not happen */
          LogHolder.log(LogLevel.EXCEPTION, LogType.GUI, e);
        }
      }
    };
    /* registrate the observer also at the internal message system */
    m_messageSystem.addObserver(forwardingModeObserver);    
    JAPModel.getInstance().getRoutingSettings().addObserver(forwardingModeObserver);
    /* tricky: initialize the panel by calling the observer */
    forwardingModeObserver.update(JAPModel.getInstance().getRoutingSettings(), new JAPRoutingMessage(JAPRoutingMessage.ROUTING_MODE_CHANGED));
    
    return statusPanel;
  }

  /**
   * Creates the routing client panel (only the config-dialog start button).
   *
   * @return The routing client config panel.
   */
  private JPanel createRoutingClientConfigPanel()
  {
    JPanel clientPanel = new JPanel();

    final JCheckBox settingsRoutingClientConfigNeedForwarderBox = new JCheckBox(JAPMessages.getString("settingsRoutingClientConfigNeedForwarderBox"));
    settingsRoutingClientConfigNeedForwarderBox.setFont(getFontSetting());
    settingsRoutingClientConfigNeedForwarderBox.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent event) {
        /* enable or disable the connect-via-forwarder setting in JAPRoutingSettings */
        JAPModel.getInstance().getRoutingSettings().setConnectViaForwarder(settingsRoutingClientConfigNeedForwarderBox.isSelected());
      }
    });
    
    final JCheckBox settingsRoutingClientConfigNoInfoServiceBox = new JCheckBox(JAPMessages.getString("settingsRoutingClientConfigNoInfoServiceBox"));
    settingsRoutingClientConfigNoInfoServiceBox.setFont(getFontSetting());
    settingsRoutingClientConfigNoInfoServiceBox.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent event) {
        /* enable or disable the forward InfoService setting in JAPRoutingSettings */
        JAPModel.getInstance().getRoutingSettings().setForwardInfoService(settingsRoutingClientConfigNoInfoServiceBox.isSelected());
      }
    });

    TitledBorder settingsRoutingClientConfigBorder = new TitledBorder(JAPMessages.getString("settingsRoutingClientConfigBorder"));
    settingsRoutingClientConfigBorder.setTitleFont(getFontSetting());
    clientPanel.setBorder(settingsRoutingClientConfigBorder);

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
    clientPanelLayout.setConstraints(settingsRoutingClientConfigNeedForwarderBox, clientPanelConstraints);
    clientPanel.add(settingsRoutingClientConfigNeedForwarderBox);
    
    clientPanelConstraints.weighty = 1.0;
    clientPanelConstraints.gridx = 0;
    clientPanelConstraints.gridy = 1;
    clientPanelConstraints.insets = new Insets(0, 20, 5, 5);
    clientPanelLayout.setConstraints(settingsRoutingClientConfigNoInfoServiceBox, clientPanelConstraints);
    clientPanel.add(settingsRoutingClientConfigNoInfoServiceBox);    

    Observer clientSettingsObserver = new Observer() {
      /**
       * This is the observer implementation. If the client settings were changed, we update the
       * checkboxes.
       *
       * @param a_notifier The observed Object. This should always be JAPRoutingSettings or the
       *                   module internal message system at the moment.
       * @param a_message The reason of the notification. This should always be a JAPRoutingMessage
       *                  or null at the moment.
       */
      public void update(Observable a_notifier, Object a_message) {
        try {
          if (a_notifier == JAPModel.getInstance().getRoutingSettings()) {
            if (((JAPRoutingMessage)(a_message)).getMessageCode() == JAPRoutingMessage.CLIENT_SETTINGS_CHANGED) {
              /* the client settings were changed -> update the state of the checkboxes, maybe
               * also make them invisible, if they are not needed
               */
              if (JAPModel.getInstance().getRoutingSettings().isConnectViaForwarder()) {
                settingsRoutingClientConfigNeedForwarderBox.setSelected(true);
                settingsRoutingClientConfigNoInfoServiceBox.setVisible(true);
              }
              else {
                settingsRoutingClientConfigNeedForwarderBox.setSelected(false);
                settingsRoutingClientConfigNoInfoServiceBox.setVisible(false);
              }
              settingsRoutingClientConfigNoInfoServiceBox.setSelected(JAPModel.getInstance().getRoutingSettings().getForwardInfoService());
            }
          }
          if (a_notifier == m_messageSystem) {
            /* the root panel was recreated -> stop observing and remove ourself from the observed
             * objects
             */
            JAPModel.getInstance().getRoutingSettings().deleteObserver(this);
            m_messageSystem.deleteObserver(this);
          }
        }
        catch (Exception e) {
          /* should not happen */
          LogHolder.log(LogLevel.EXCEPTION, LogType.GUI, e);
        }
      }
    };
    /* registrate the observer also at the internal message system */
    m_messageSystem.addObserver(clientSettingsObserver);    
    JAPModel.getInstance().getRoutingSettings().addObserver(clientSettingsObserver);
    /* tricky: initialize the checkboxes by calling the observer */
    clientSettingsObserver.update(JAPModel.getInstance().getRoutingSettings(), new JAPRoutingMessage(JAPRoutingMessage.CLIENT_SETTINGS_CHANGED));

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

    final JAPJIntField settingsRoutingPortField = new JAPJIntField(Integer.toString(JAPModel.getInstance().getRoutingSettings().getServerPort()));
    settingsRoutingPortField.setFont(getFontSetting());
    settingsRoutingPortField.setColumns(5);
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
          if (JAPModel.getInstance().getRoutingSettings().setServerPort(port) == false)
          {
            throw (new Exception(
              "JAPConfRouting: showChangeServerPortDialog: Error while changing server port."));
          }
          changeDialog.dispose();
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
        changeDialog.dispose();
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
    changePanelConstraints.insets = new Insets(5, 5, 10, 5);
    changePanelLayout.setConstraints(settingsRoutingChangeDialogPortLabel, changePanelConstraints);
    changePanel.add(settingsRoutingChangeDialogPortLabel);

    changePanelConstraints.gridx = 1;
    changePanelConstraints.gridy = 0;
    changePanelConstraints.weightx = 1.0;
    changePanelConstraints.insets = new Insets(5, 5, 10, 5);
    changePanelLayout.setConstraints(settingsRoutingPortField, changePanelConstraints);
    changePanel.add(settingsRoutingPortField);

    changePanelConstraints.gridx = 0;
    changePanelConstraints.gridy = 1;
    changePanelConstraints.weightx = 0.0;
    changePanelConstraints.weighty = 1.0;
    changePanelConstraints.insets = new Insets(0, 5, 5, 5);
    changePanelLayout.setConstraints(settingsRoutingChangeDialogChangeButton, changePanelConstraints);
    changePanel.add(settingsRoutingChangeDialogChangeButton);

    changePanelConstraints.gridx = 1;
    changePanelConstraints.gridy = 1;
    changePanelConstraints.weightx = 1.0;
    changePanelConstraints.insets = new Insets(0, 5, 5, 5);
    changePanelLayout.setConstraints(settingsRoutingChangeDialogCancelButton, changePanelConstraints);
    changePanel.add(settingsRoutingChangeDialogCancelButton);

    changeDialog.align();
    changeDialog.show();
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

    final JAPJIntField settingsRoutingBandwidthField = new JAPJIntField(Integer.toString((JAPModel.getInstance().getRoutingSettings().getMaxBandwidth() * 8) / 1000));
    settingsRoutingBandwidthField.setFont(getFontSetting());
    settingsRoutingBandwidthField.setColumns(6);
    JButton settingsRoutingBandwidthDialogChangeButton = new JButton(JAPMessages.getString(
      "settingsRoutingBandwidthDialogChangeButton"));
    settingsRoutingBandwidthDialogChangeButton.setFont(getFontSetting());
    settingsRoutingBandwidthDialogChangeButton.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent event)
      {
        /* if the Change button is pressed, we change the maximum possible bandwidth, also
         * the user defined bandwidth class is updated and set as the current connection class
         */
        try
        {
          int maxBandwidth = (Integer.parseInt(settingsRoutingBandwidthField.getText().trim()) * 1000) / 8;
          JAPModel.getInstance().getRoutingSettings().getConnectionClassSelector().changeUserDefinedClass(maxBandwidth, JAPModel.getInstance().getRoutingSettings().getBandwidth());
          JAPModel.getInstance().getRoutingSettings().getConnectionClassSelector().setCurrentConnectionClass(JAPRoutingConnectionClassSelector.CONNECTION_CLASS_USER);
          bandwidthDialog.dispose();
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
        bandwidthDialog.dispose();
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
    bandwidthPanelConstraints.insets = new Insets(5, 5, 10, 5);
    bandwidthPanelLayout.setConstraints(settingsRoutingBandwidthDialogBandwidthLabel, bandwidthPanelConstraints);
    bandwidthPanel.add(settingsRoutingBandwidthDialogBandwidthLabel);

    bandwidthPanelConstraints.gridx = 1;
    bandwidthPanelConstraints.gridy = 0;
    bandwidthPanelConstraints.weightx = 1.0;
    bandwidthPanelConstraints.insets = new Insets(5, 5, 10, 5);
    bandwidthPanelLayout.setConstraints(settingsRoutingBandwidthField, bandwidthPanelConstraints);
    bandwidthPanel.add(settingsRoutingBandwidthField);

    bandwidthPanelConstraints.gridx = 0;
    bandwidthPanelConstraints.gridy = 1;
    bandwidthPanelConstraints.weightx = 0.0;
    bandwidthPanelConstraints.weighty = 1.0;
    bandwidthPanelConstraints.insets = new Insets(0, 5, 5, 5);
    bandwidthPanelLayout.setConstraints(settingsRoutingBandwidthDialogChangeButton, bandwidthPanelConstraints);
    bandwidthPanel.add(settingsRoutingBandwidthDialogChangeButton);

    bandwidthPanelConstraints.gridx = 1;
    bandwidthPanelConstraints.gridy = 1;
    bandwidthPanelConstraints.weightx = 1.0;
    bandwidthPanelConstraints.insets = new Insets(0, 5, 5, 5);
    bandwidthPanelLayout.setConstraints(settingsRoutingBandwidthDialogCancelButton, bandwidthPanelConstraints);
    bandwidthPanel.add(settingsRoutingBandwidthDialogCancelButton);

    bandwidthDialog.align();
    bandwidthDialog.show();
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
  private Vector showFetchMixCascadesDialog(JComponent a_parentComponent) { 
    final JAPDialog fetchMixCascadesDialog = new JAPDialog(a_parentComponent, JAPMessages.getString("settingsRoutingServerFetchMixCascadesDialogTitle"));
    fetchMixCascadesDialog.disableManualClosing();
    JPanel fetchMixCascadesPanel = fetchMixCascadesDialog.getRootPanel();

    JLabel settingsRoutingServerFetchMixCascadesDialogFetchLabel = new JLabel(JAPMessages.getString("settingsRoutingServerFetchMixCascadesDialogFetchLabel"));
    settingsRoutingServerFetchMixCascadesDialogFetchLabel.setFont(getFontSetting());
    JLabel busyLabel = new JLabel(JAPUtil.loadImageIcon(JAPConstants.BUSYFN, true));

    final Vector fetchedCascades = new Vector();
    final Vector errorOccured = new Vector();
    
    final Thread fetchMixCascadesThread = new Thread(new Runnable() {
      public void run()
      {       
        Vector knownMixCascades = InfoServiceHolder.getInstance().getMixCascades();
        fetchMixCascadesDialog.dispose();
        /* clear the interrupted flag, if it is set */
        Thread.interrupted();
        if (knownMixCascades == null) {
          errorOccured.addElement(new NullPointerException());          
          knownMixCascades = new Vector();
        }
        /* copy the fetched cascades in the result Vector */
        Enumeration cascades = knownMixCascades.elements();
        while (cascades.hasMoreElements()) {
          fetchedCascades.addElement(cascades.nextElement());
        }
      }
    });

    JButton settingsRoutingFetchMixCascadesDialogCancelButton = new JButton(JAPMessages.getString("cancelButton"));
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
    fetchMixCascadesPanelLayout.setConstraints(settingsRoutingServerFetchMixCascadesDialogFetchLabel, fetchMixCascadesPanelConstraints);
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
    fetchMixCascadesPanelLayout.setConstraints(settingsRoutingFetchMixCascadesDialogCancelButton, fetchMixCascadesPanelConstraints);
    fetchMixCascadesPanel.add(settingsRoutingFetchMixCascadesDialogCancelButton);

    fetchMixCascadesDialog.align();

    /* for synchronization purposes, it is necessary to show the dialog first and start the thread
     * after that event
     */
    fetchMixCascadesDialog.getInternalDialog().addWindowListener(new WindowAdapter() {
      public void windowOpened(WindowEvent a_event) {
        fetchMixCascadesDialog.getInternalDialog().removeWindowListener(this);
        fetchMixCascadesThread.start();
      }
    });

    fetchMixCascadesDialog.show();

    /* wait until the fetch-thread is ready */
    try {
      fetchMixCascadesThread.join();
    }
    catch (InterruptedException e) {
    }
    
    if (errorOccured.size() > 0) {
      JOptionPane.showMessageDialog(a_parentComponent, JAPMessages.getString("settingsRoutingFetchCascadesError"), JAPMessages.getString("ERROR"), JOptionPane.ERROR_MESSAGE);
    }
    
    return fetchedCascades;
  }
  
}
