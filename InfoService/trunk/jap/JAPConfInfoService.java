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
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextField;
import javax.swing.ListSelectionModel;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import anon.infoservice.HTTPConnectionFactory;
import anon.infoservice.InfoService;
import anon.infoservice.InfoServiceDatabase;
import anon.infoservice.InfoServiceHolder;
import anon.infoservice.ListenerInterface;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

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
   * Stores the instance of the prefered infoservice JTextField.
   */
  private JTextField settingsInfoPreferedField;

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
    JPanel configPanel = createInfoServiceConfigPanel();
    JPanel interfacePanel = createInfoServiceInterfacePanel();
    JPanel advancedPanel = createInfoServiceAdvancedPanel();

    GridBagLayout rootPanelLayout = new GridBagLayout();
    rootPanel.setLayout(rootPanelLayout);

    GridBagConstraints rootPanelConstraints = new GridBagConstraints();
    rootPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
    rootPanelConstraints.fill = GridBagConstraints.BOTH;
    rootPanelConstraints.weightx = 1.0;

    rootPanelConstraints.gridx = 0;
    rootPanelConstraints.gridy = 0;
    rootPanelConstraints.weighty = 1.0;
    rootPanelLayout.setConstraints(configPanel, rootPanelConstraints);
    rootPanel.add(configPanel);
    rootPanelConstraints.weighty = 0.0;

    rootPanelConstraints.gridx = 0;
    rootPanelConstraints.gridy = 1;
    rootPanelLayout.setConstraints(interfacePanel, rootPanelConstraints);
    rootPanel.add(interfacePanel);

    rootPanelConstraints.gridx = 0;
    rootPanelConstraints.gridy = 2;
    rootPanelLayout.setConstraints(advancedPanel, rootPanelConstraints);
    rootPanel.add(advancedPanel);

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

    settingsInfoPreferedField = new JTextField();
    settingsInfoPreferedField.setFont(getFontSetting());
    settingsInfoPreferedField.setEditable(false);
    JLabel settingsInfoPreferedLabel = new JLabel(JAPMessages.getString("settingsInfoPreferedLabel"));
    settingsInfoPreferedLabel.setFont(getFontSetting());

    settingsInfoAllList = new JList(InfoServiceDatabase.getInstance().getInfoServiceList());
    settingsInfoAllList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    settingsInfoAllList.addListSelectionListener(new ListSelectionListener()
    {
      public void valueChanged(ListSelectionEvent event)
      {
        updateSelectedInfoService();
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
          InfoServiceDatabase.getInstance().removeAll();
          /* now put the new infoservices in the list */
          Enumeration it = infoservices.elements();
          while (it.hasMoreElements())
          {
            InfoServiceDatabase.getInstance().update( (InfoService) (it.nextElement()));
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
        InfoService selectedInfoService = (InfoService) (settingsInfoAllList.getSelectedValue());
        if (selectedInfoService != null)
        {
          /* change the prefered infoservice only, if something is selected */
          InfoServiceHolder.getInstance().setPreferedInfoService(selectedInfoService);
          settingsInfoPreferedField.setText(selectedInfoService.getName());
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
        InfoService selectedInfoService = (InfoService) (settingsInfoAllList.getSelectedValue());
        InfoService preferedInfoService = InfoServiceHolder.getInstance().getPreferedInfoService();
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
              InfoServiceDatabase.getInstance().remove(selectedInfoService);
              /* update the infoservice list */
              updateGuiOutput();
            }
          }
          else
          {
            InfoServiceDatabase.getInstance().remove(selectedInfoService);
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
    configPanelLayout.setConstraints(settingsInfoListLabel, configPanelConstraints);
    configPanel.add(settingsInfoListLabel);

    configPanelConstraints.gridx = 0;
    configPanelConstraints.gridy = 1;
    configPanelConstraints.gridheight = 4;
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
    configPanelLayout.setConstraints(settingsInfoAddButton, configPanelConstraints);
    configPanel.add(settingsInfoAddButton);

    configPanelConstraints.gridx = 1;
    configPanelConstraints.gridy = 4;
    configPanelConstraints.insets = new Insets(0, 10, 0, 0);
    configPanelLayout.setConstraints(settingsInfoRemoveButton, configPanelConstraints);
    configPanel.add(settingsInfoRemoveButton);
    configPanelConstraints.weightx = 1.0;
    configPanelConstraints.insets = new Insets(0, 0, 0, 0);

    configPanelConstraints.gridx = 0;
    configPanelConstraints.gridy = 5;
    configPanelLayout.setConstraints(settingsInfoPreferedLabel, configPanelConstraints);
    configPanel.add(settingsInfoPreferedLabel);

    configPanelConstraints.gridx = 0;
    configPanelConstraints.gridy = 6;
    configPanelLayout.setConstraints(settingsInfoPreferedField, configPanelConstraints);
    configPanel.add(settingsInfoPreferedField);

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
   * Creates the infoservice advanced configuration panel with all components.
   *
   * @return The interface advanced configuration panel.
   */
  private JPanel createInfoServiceAdvancedPanel()
  {
    JPanel advancedPanel = new JPanel();
    TitledBorder settingsInfoAdvancedBorder = new TitledBorder(JAPMessages.getString(
      "settingsInfoAdvancedBorder"));
    settingsInfoAdvancedBorder.setTitleFont(getFontSetting());
    advancedPanel.setBorder(settingsInfoAdvancedBorder);

    JButton settingsInfoExpertSettingsButton = new JButton(JAPMessages.getString(
      "settingsInfoExpertSettingsButton"));
    settingsInfoExpertSettingsButton.setFont(getFontSetting());
    settingsInfoExpertSettingsButton.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent event)
      {
        /* if the expert settins button is pressed, show the expert settings dialog */
        showInfoServiceExpertDialog();
      }
    });

    GridBagLayout advancedPanelLayout = new GridBagLayout();
    advancedPanel.setLayout(advancedPanelLayout);

    GridBagConstraints advancedPanelConstraints = new GridBagConstraints();
    advancedPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
    advancedPanelConstraints.fill = GridBagConstraints.NONE;
    advancedPanelConstraints.weightx = 1.0;
    advancedPanelConstraints.weighty = 1.0;
    advancedPanelConstraints.gridx = 0;
    advancedPanelConstraints.gridy = 0;
    advancedPanelLayout.setConstraints(settingsInfoExpertSettingsButton, advancedPanelConstraints);
    advancedPanel.add(settingsInfoExpertSettingsButton);

    return advancedPanel;
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
          InfoService newInfoService = new InfoService(settingsInfoHostField.getText().trim(),
            Integer.parseInt(settingsInfoPortField.getText().trim()), null);
          InfoServiceDatabase.getInstance().update(newInfoService);
          addDialog.hide();
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
        addDialog.hide();
      }
    });
    JLabel settingsInfoHostLabel = new JLabel(JAPMessages.getString("settingsInfoHostLabel"));
    settingsInfoHostLabel.setFont(getFontSetting());
    JLabel settingsInfoPortLabel = new JLabel(JAPMessages.getString("settingsInfoPortLabel"));
    settingsInfoPortLabel.setFont(getFontSetting());

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
    manualPanelConstraints.insets = new Insets(0, 0, 10, 0);
    manualPanelLayout.setConstraints(settingsInfoPortField, manualPanelConstraints);
    manualPanel.add(settingsInfoPortField);

    manualPanelConstraints.gridwidth = 1;
    manualPanelConstraints.gridx = 0;
    manualPanelConstraints.gridy = 4;
    manualPanelConstraints.fill = GridBagConstraints.NONE;
    manualPanelConstraints.weighty = 1.0;
    manualPanelConstraints.weightx = 0.0;
    manualPanelLayout.setConstraints(settingsInfoAddDialogAddButton, manualPanelConstraints);
    manualPanel.add(settingsInfoAddDialogAddButton);

    manualPanelConstraints.gridx = 1;
    manualPanelConstraints.gridy = 4;
    manualPanelConstraints.weightx = 1.0;
    manualPanelConstraints.insets = new Insets(0, 10, 10, 0);
    manualPanelLayout.setConstraints(settingsInfoAddDialogCancelButton, manualPanelConstraints);
    manualPanel.add(settingsInfoAddDialogCancelButton);

    addDialog.align();
    addDialog.show();
  }

  /**
   * Shows the infoservice expert settings dialog.
   */
  private void showInfoServiceExpertDialog()
  {
    final JAPDialog expertDialog = new JAPDialog(getRootPanel(),
      JAPMessages.getString("settingsInfoExpertDialogTitle"));
    final JPanel expertPanel = expertDialog.getRootPanel();

    final JCheckBox disableInfoServiceBox = new JCheckBox(JAPMessages.getString(
      "settingsInfoDisableInfoService"), JAPModel.isInfoServiceDisabled());
    disableInfoServiceBox.setFont(getFontSetting());
    final JCheckBox disableInfoServiceChangeBox = new JCheckBox(JAPMessages.getString(
      "settingsInfoDisableInfoServiceChange"), !InfoServiceHolder.getInstance().isChangeInfoServices());
    disableInfoServiceChangeBox.setFont(getFontSetting());
    final JAPJIntField infoServiceTimeoutField = new JAPJIntField();
    infoServiceTimeoutField.setText(Integer.toString(HTTPConnectionFactory.getInstance().getTimeout()));
    infoServiceTimeoutField.setColumns(3);
    infoServiceTimeoutField.setFont(getFontSetting());
    JButton okButton = new JButton(JAPMessages.getString("okButton"));
    okButton.setFont(getFontSetting());
    okButton.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent event)
      {
        /* change the values and close the dialog */
        try
        {
          int infoServiceTimeout = Integer.parseInt(infoServiceTimeoutField.getText());
          if ( (infoServiceTimeout < 1) || (infoServiceTimeout > 60))
          {
            throw (new Exception("Wrong number."));
          }
          JAPController.setInfoServiceDisabled(disableInfoServiceBox.isSelected());
          InfoServiceHolder.getInstance().setChangeInfoServices(!disableInfoServiceChangeBox.
            isSelected());
          HTTPConnectionFactory.getInstance().setTimeout(infoServiceTimeout);
          expertDialog.hide();
        }
        catch (Exception e)
        {
          JOptionPane.showMessageDialog(expertPanel,
                          JAPMessages.getString("settingsInfoExpertTimeoutError"),
                          JAPMessages.getString("ERROR"), JOptionPane.ERROR_MESSAGE);
        }
      }
    });
    JButton cancelButton = new JButton(JAPMessages.getString("cancelButton"));
    cancelButton.setFont(getFontSetting());
    cancelButton.addActionListener(new ActionListener()
    {
      public void actionPerformed(ActionEvent event)
      {
        /* if the Cancel button is pressed, close the dialog */
        expertDialog.hide();
      }
    });
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
    expertPanelConstraints.gridx = 0;
    expertPanelConstraints.gridy = 0;
    expertPanelConstraints.gridwidth = 2;
    expertPanelConstraints.insets = new Insets(5, 5, 10, 0);
    expertPanelLayout.setConstraints(settingsInfoTimeoutLabel, expertPanelConstraints);
    expertPanel.add(settingsInfoTimeoutLabel);

    expertPanelConstraints.gridx = 2;
    expertPanelConstraints.gridy = 0;
    expertPanelConstraints.weightx = 1.0;
    expertPanelConstraints.gridwidth = 1;
    expertPanelConstraints.insets = new Insets(5, 5, 10, 0);
    expertPanelLayout.setConstraints(infoServiceTimeoutField, expertPanelConstraints);
    expertPanel.add(infoServiceTimeoutField);

    expertPanelConstraints.gridx = 0;
    expertPanelConstraints.gridy = 1;
    expertPanelConstraints.gridwidth = 3;
    expertPanelConstraints.insets = new Insets(0, 5, 10, 0);
    expertPanelLayout.setConstraints(disableInfoServiceBox, expertPanelConstraints);
    expertPanel.add(disableInfoServiceBox);

    expertPanelConstraints.gridx = 0;
    expertPanelConstraints.gridy = 2;
    expertPanelConstraints.insets = new Insets(0, 5, 10, 0);
    expertPanelLayout.setConstraints(disableInfoServiceChangeBox, expertPanelConstraints);
    expertPanel.add(disableInfoServiceChangeBox);

    expertPanelConstraints.gridwidth = 1;
    expertPanelConstraints.gridx = 0;
    expertPanelConstraints.gridy = 3;
    expertPanelConstraints.weighty = 1.0;
    expertPanelConstraints.weightx = 0.0;
    expertPanelLayout.setConstraints(okButton, expertPanelConstraints);
    expertPanel.add(okButton);

    expertPanelConstraints.gridx = 1;
    expertPanelConstraints.gridy = 3;
    expertPanelConstraints.weightx = 1.0;
    expertPanelConstraints.gridwidth = 2;
    expertPanelConstraints.insets = new Insets(0, 10, 10, 0);
    expertPanelLayout.setConstraints(cancelButton, expertPanelConstraints);
    expertPanel.add(cancelButton);

    expertDialog.align();
    expertDialog.show();
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
      Vector knownInfoServices = InfoServiceDatabase.getInstance().getInfoServiceList();
      InfoService preferedInfoService = InfoServiceHolder.getInstance().getPreferedInfoService();
      if (preferedInfoService != null)
      {
        settingsInfoPreferedField.setText(preferedInfoService.getName());
        /* if the prefered InfoService is not in the list (because it is expired), add it */
        boolean preferedIsInList = false;
        Enumeration knownInfoServiceEnumeration = knownInfoServices.elements();
        while (knownInfoServiceEnumeration.hasMoreElements())
        {
          InfoService tempInfoService = (InfoService) (knownInfoServiceEnumeration.nextElement());
          if (tempInfoService.getId().equals(preferedInfoService.getId()))
          {
            preferedIsInList = true;
          }
        }
        if (preferedIsInList == false)
        {
          knownInfoServices.addElement(preferedInfoService);
        }
      }
      else
      {
        /* should not happen */
        settingsInfoPreferedField.setText(JAPMessages.getString("settingsInfoNotAvailableText"));
      }
      settingsInfoAllList.setListData(knownInfoServices);
    }
    updateSelectedInfoService();
  }

  /**
   * Updates the listener interfaces list for the selected infoservice.
   */
  private void updateSelectedInfoService()
  {
    /* get the listener interfaces of the selected infoservice */
    InfoService selectedInfoService = (InfoService) (settingsInfoAllList.getSelectedValue());
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
