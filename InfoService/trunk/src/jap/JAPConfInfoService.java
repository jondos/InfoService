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


import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.util.Enumeration;
import java.util.Observable;
import java.util.Observer;
import java.util.Vector;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JScrollPane;
import javax.swing.JTabbedPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.ListSelectionModel;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import anon.infoservice.Database;
import anon.infoservice.DatabaseMessage;
import anon.infoservice.InfoServiceDBEntry;
import anon.infoservice.InfoServiceHolder;
import anon.infoservice.InfoServiceHolderMessage;
import anon.infoservice.ListenerInterface;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/**
 * This is the configuration GUI for the infoservice.
 */
public class JAPConfInfoService extends AbstractJAPConfModule {

  /**
   * This is the internal message system of this module.
   */
  private MessageSystem m_messageSystem;


  /**
   * Constructor for JAPConfInfoService. We do some initializing here.
   */
  public JAPConfInfoService() {
    super(new JAPConfInfoServiceSavePoint());
  }


  /**
   * Creates the infoservice root panel with all child-panels.
   */
  public void recreateRootPanel() {
    synchronized (this) {
      if (m_messageSystem == null) {
        /* create a new object for sending internal messages */
        m_messageSystem = new MessageSystem();
      }
    }
    JPanel rootPanel = getRootPanel();
      
    synchronized (this) {
      /* clear the whole root panel */
      rootPanel.removeAll();     
      /* notify the observers of the message system that we recreate the root panel */
      m_messageSystem.sendMessage();
      
      /* insert all components in the root panel */
      JTabbedPane infoServiceTabPane = new JTabbedPane();
      infoServiceTabPane.setFont(getFontSetting());
      infoServiceTabPane.insertTab(JAPMessages.getString("settingsInfoServiceConfigBasicSettingsTabTitle"), null, createInfoServiceConfigPanel(), null, 0);
      infoServiceTabPane.insertTab(JAPMessages.getString("settingsInfoServiceConfigAdvancedSettingsTabTitle"), null, createInfoServiceAdvancedPanel(), null, 1);

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
    }
  }

  /**
   * Returns the title for the infoservice configuration tab.
   *
   * @return The title for the infoservice configuration tab.
   */
  public String getTabTitle() {
    return JAPMessages.getString("confTreeInfoServiceLeaf");
  }


  /**
   * Creates the infoservice configuration panel with all components.
   *
   * @return The infoservice configuration panel.
   */
  private JPanel createInfoServiceConfigPanel() {
    final JPanel basicPanel = new JPanel();

    final JPanel configPanel = new JPanel();
    final JPanel switchPanel = new JPanel();
    final JPanel informationPanel = new JPanel();
    final JPanel addInfoServicePanel = new JPanel();

    final DefaultListModel knownInfoServicesListModel = new DefaultListModel();
    Observer knownInfoServicesListObserver = new Observer() {
      
      private boolean m_preferredInfoServiceIsAlsoInDatabase = false;
      
      private InfoServiceDBEntry m_currentPreferredInfoService = null;
      
      /**
       * This is the observer implementation. If there are changes at the database of known
       * InfoServices or the preferred InfoService is changed, this observer will update the list
       * of known infoservices. If the panel is recreated (message via the module internal message
       * system), the observer removes itself from all observed objects.
       *
       * @param a_notifier The observed Object. This should always be the Database instance for
       *                   the InfoServiceDBEntries, the InfoServiceHolder instance or the module
       *                   internal message system at the moment.
       * @param a_message The reason of the notification. This should always be a DatabaseMessage,
       *                  a InfoServiceHolderMessage or null at the moment (depending on the
       *                  notifier).
       */
      public void update(Observable a_notifier, Object a_message) {
        try {
          if (a_notifier == Database.getInstance(InfoServiceDBEntry.class)) {
            /* message is from the infoservice database */
            int messageCode = ((DatabaseMessage) a_message).getMessageCode();
            if ((messageCode == DatabaseMessage.ENTRY_ADDED) || (messageCode == DatabaseMessage.ENTRY_RENEWED)) {
              InfoServiceDBEntry updatedEntry = (InfoServiceDBEntry)(((DatabaseMessage)a_message).getMessageData());
              synchronized (knownInfoServicesListModel) {
                int entryPositionInList = knownInfoServicesListModel.indexOf(updatedEntry);
                if (entryPositionInList != -1) {
                  /* we already know an entry with the same ID, but maybe something has changed
                   * -> replace the entry by the new one
                   */
                  knownInfoServicesListModel.setElementAt(updatedEntry, entryPositionInList);
                  if (updatedEntry.equals(m_currentPreferredInfoService)) {
                    /* the preferred InfoService is also in the database of all known
                     * InfoServices (because we have received this add or renew message)
                     */
                    m_preferredInfoServiceIsAlsoInDatabase = true;
                  }
                }
                else {
                  /* the entry is really new */
                  if (updatedEntry.isUserDefined()) {
                    /* it's an user-defined entry -> add it at the end of the list */
                    knownInfoServicesListModel.addElement(updatedEntry);
                  }
                  else {
                    /* it's an entry downloaded from the Internet -> add it at the end of the
                     * Internet entries but before the user-defined entries
                     */                 
                    boolean positionFound = false;
                    int i = 0;
                    while ((i < knownInfoServicesListModel.size()) && (positionFound == false)) {
                      if (((InfoServiceDBEntry)(knownInfoServicesListModel.getElementAt(i))).isUserDefined()) {
                        /* we have found the first user-defined entry */
                        positionFound = true;
                      }
                      else {
                        i++;
                      }
                    }
                    knownInfoServicesListModel.insertElementAt(updatedEntry, i);
                  }
                }
              }
            }
            if (messageCode == DatabaseMessage.ENTRY_REMOVED) {            
              InfoServiceDBEntry removedInfoService = (InfoServiceDBEntry)(((DatabaseMessage)a_message).getMessageData());
              synchronized (knownInfoServicesListModel) {
                if (removedInfoService.equals(m_currentPreferredInfoService)) {
                  /* the preferred InfoService war removed from the database of all known
                   * InfoServices but don't remove it from this list as long as it is the
                   * preferred InfoService
                   */
                  m_preferredInfoServiceIsAlsoInDatabase = false;
                }
                else {
                  /* it's not the preferred InfoService -> we can remove it from the list */
                  knownInfoServicesListModel.removeElement(removedInfoService);
                }
              }
            }
            if (messageCode == DatabaseMessage.ALL_ENTRIES_REMOVED) {            
              synchronized (knownInfoServicesListModel) {
                /* also the preferred InfoService war removed from the database of all known
                 * InfoServices (but we have to keep it in the list)
                 */
                Enumeration infoServicesInList = knownInfoServicesListModel.elements();
                while (infoServicesInList.hasMoreElements()) {
                  InfoServiceDBEntry currentInfoService = (InfoServiceDBEntry)(infoServicesInList.nextElement());
                  if (currentInfoService.equals(m_currentPreferredInfoService) == false) {
                    /* it's not the preferred InfoService -> we can remove it */
                    knownInfoServicesListModel.removeElement(currentInfoService);
                  }
                }
                m_preferredInfoServiceIsAlsoInDatabase = false;               
              }
            }
            if (messageCode == DatabaseMessage.INITIAL_OBSERVER_MESSAGE) {            
              Enumeration databaseEntries = ((Vector)(((DatabaseMessage)a_message).getMessageData())).elements();
              synchronized (knownInfoServicesListModel) {
                while (databaseEntries.hasMoreElements()) {
                  /* trick: call this observer with an ADD message for every single entry */
                  update(a_notifier, new DatabaseMessage(DatabaseMessage.ENTRY_ADDED, databaseEntries.nextElement()));
                }           
              }
            }
          }
          if (a_notifier == InfoServiceHolder.getInstance()) {
            /* message is from InfoServiceHolder */
            int messageCode = ((InfoServiceHolderMessage)a_message).getMessageCode();
            if (messageCode == InfoServiceHolderMessage.PREFERRED_INFOSERVICE_CHANGED) {
              /* we have a new preferred InfoService */
              InfoServiceDBEntry newPreferredInfoService = (InfoServiceDBEntry)(((InfoServiceHolderMessage)a_message).getMessageData());
              synchronized (knownInfoServicesListModel) {               
                if ((m_currentPreferredInfoService != null) && (m_currentPreferredInfoService.equals(newPreferredInfoService))) {
                  /* the new preferred InfoService has the same ID as the old one, but maybe
                   * something other was changed -> call the set method of the vector
                   */
                  int positionOfPreferredInfoService = knownInfoServicesListModel.indexOf(m_currentPreferredInfoService);
                  if (positionOfPreferredInfoService != -1) {
                    knownInfoServicesListModel.setElementAt(newPreferredInfoService, positionOfPreferredInfoService);
                  }
                  m_currentPreferredInfoService = newPreferredInfoService;
                }
                else {  
                  if (m_currentPreferredInfoService != null) {
                    if (m_preferredInfoServiceIsAlsoInDatabase) {
                      /* don't remove the old preferred InfoService from the list, but we have to
                       * repaint the entry -> call the set method of the vector, but don't change
                       * anything
                       */
                      int positionOfOldPreferredInfoService = knownInfoServicesListModel.indexOf(m_currentPreferredInfoService);
                      if (positionOfOldPreferredInfoService != -1) {
                        knownInfoServicesListModel.setElementAt(knownInfoServicesListModel.elementAt(positionOfOldPreferredInfoService), positionOfOldPreferredInfoService);
                      }
                    }
                    else {
                      /* we can remove the old preferred InfoService from the list, because it is
                       * also not in the database any more
                       */
                      knownInfoServicesListModel.removeElement(m_currentPreferredInfoService);
                    }
                  }
                  m_currentPreferredInfoService = newPreferredInfoService;
                  if (newPreferredInfoService != null) {
                    int positionOfNewPreferredInfoService = knownInfoServicesListModel.indexOf(newPreferredInfoService);
                    if (positionOfNewPreferredInfoService != -1) {
                      /* the new preferred InfoService was already in the list -> it was already in
                       * the Database -> only update the value by calling the set method
                       */
                      m_preferredInfoServiceIsAlsoInDatabase = true;
                      knownInfoServicesListModel.setElementAt(newPreferredInfoService, positionOfNewPreferredInfoService);
                    }
                    else {
                      /* the new preferred InfoService is not already in the list -> it is not in
                       * the database of known infoservices
                       */
                      m_preferredInfoServiceIsAlsoInDatabase = false;
                      /* tricky: to add the new preferred InfoService to the list, we simply call
                       * this observer with an ADD message
                       */
                      update(Database.getInstance(InfoServiceDBEntry.class), new DatabaseMessage(DatabaseMessage.ENTRY_ADDED, newPreferredInfoService));
                    }
                  }
                }      
              }
            }
          }  
          if (a_notifier == m_messageSystem) {
            /* the root panel was recreated -> stop observing and remove ourself from the observed
             * objects
             */
            Database.getInstance(InfoServiceDBEntry.class).deleteObserver(this);
            InfoServiceHolder.getInstance().deleteObserver(this);
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
    m_messageSystem.addObserver(knownInfoServicesListObserver);
    Database.getInstance(InfoServiceDBEntry.class).addObserver(knownInfoServicesListObserver);
    synchronized (InfoServiceHolder.getInstance()) {
      InfoServiceHolder.getInstance().addObserver(knownInfoServicesListObserver);
      /* tricky: initialize the preferred InfoService by calling the observer */
      knownInfoServicesListObserver.update(InfoServiceHolder.getInstance(), new InfoServiceHolderMessage(InfoServiceHolderMessage.PREFERRED_INFOSERVICE_CHANGED, InfoServiceHolder.getInstance().getPreferredInfoService()));
    }
   
    final JList knownInfoServicesList = new JList(knownInfoServicesListModel);
    knownInfoServicesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
    knownInfoServicesList.setCellRenderer(new ListCellRenderer() {
      public Component getListCellRendererComponent(JList a_list, Object a_value, int a_index, boolean a_isSelected, boolean a_cellHasFocus) {
        JLabel returnLabel = null;
        if (((InfoServiceDBEntry)a_value).isUserDefined()) {
          returnLabel = new JLabel(((InfoServiceDBEntry) a_value).getName(), JAPUtil.loadImageIcon(JAPConstants.IMAGE_INFOSERVICE_MANUELL, true), JLabel.LEFT);
        }
        else {
          returnLabel = new JLabel(((InfoServiceDBEntry)a_value).getName(), JAPUtil.loadImageIcon(JAPConstants.IMAGE_INFOSERVICE_INTERNET, true), JLabel.LEFT);
        }
        returnLabel.setOpaque(true);
        /* the default is a non-bold font */
        returnLabel.setFont(new Font(getFontSetting().getName(), getFontSetting().getStyle() & (~Font.BOLD), getFontSetting().getSize()));
        InfoServiceDBEntry preferredInfoService = InfoServiceHolder.getInstance().getPreferredInfoService();
        if (preferredInfoService != null) {
          if (preferredInfoService.equals((InfoServiceDBEntry) a_value)) {
            /* print the preferred InfoService in a bold font */
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
    JScrollPane knownInfoServicesListScrollPane = new JScrollPane(knownInfoServicesList);
    knownInfoServicesListScrollPane.setFont(getFontSetting());

    JButton settingsInfoServiceConfigBasicSettingsFetchInfoServicesButton = new JButton(JAPMessages.getString("settingsInfoServiceConfigBasicSettingsFetchInfoServicesButton"));
    settingsInfoServiceConfigBasicSettingsFetchInfoServicesButton.setFont(getFontSetting());
    settingsInfoServiceConfigBasicSettingsFetchInfoServicesButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent a_event) {
        /* show the fetch InfoServices dialog */
        Vector fetchedInfoservices = showFetchInfoServicesDialog(configPanel);
        if (fetchedInfoservices != null) {
          /* update the database of known infoservices */
          Enumeration infoservices = fetchedInfoservices.elements();
          while (infoservices.hasMoreElements()) {
            InfoServiceDBEntry currentInfoService = (InfoServiceDBEntry) (infoservices.nextElement());
            Database.getInstance(InfoServiceDBEntry.class).update(currentInfoService);
            InfoServiceDBEntry preferredInfoService = InfoServiceHolder.getInstance().getPreferredInfoService();
            if (preferredInfoService != null) {
              /* if the current infoservice is equal to the preferred infoservice, update the
               * preferred infoservice also
               */
              if (preferredInfoService.equals(currentInfoService)) {
                InfoServiceHolder.getInstance().setPreferredInfoService(currentInfoService);
              }
            }
          }
          /* now remove all non user-defined infoservices, which were not updated, from the
           * database of known infoservices
           */
          Enumeration knownInfoServices = Database.getInstance(InfoServiceDBEntry.class).getEntryList().elements();
          while (knownInfoServices.hasMoreElements()) {
            InfoServiceDBEntry currentInfoService = (InfoServiceDBEntry)(knownInfoServices.nextElement());
            if ((currentInfoService.isUserDefined() == false) && (fetchedInfoservices.contains(currentInfoService) == false)) {
              /* the InfoService was fetched from the Internet earlier, but it is not in the list
               * fetched from the Internet this time -> remove that InfoService from the database
               * of known InfoServices
               */
              Database.getInstance(InfoServiceDBEntry.class).remove(currentInfoService);
            }
          }
        }
      }
    });
    
    JButton settingsInfoServiceConfigBasicSettingsSetPreferredButton = new JButton(JAPMessages.getString("settingsInfoServiceConfigBasicSettingsSetPreferredButton"));
    settingsInfoServiceConfigBasicSettingsSetPreferredButton.setFont(getFontSetting());
    settingsInfoServiceConfigBasicSettingsSetPreferredButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent a_event) {
        /* set the selected infoservice as prefered infoservice */
        InfoServiceDBEntry selectedInfoService = (InfoServiceDBEntry) (knownInfoServicesList.getSelectedValue());
        if (selectedInfoService != null) {
          /* change the prefered infoservice only, if something is selected */
          InfoServiceHolder.getInstance().setPreferredInfoService(selectedInfoService);
        }
      }
    });
    
    final JButton settingsInfoServiceConfigBasicSettingsAddButton = new JButton(JAPMessages.getString("settingsInfoServiceConfigBasicSettingsAddButton"));
    settingsInfoServiceConfigBasicSettingsAddButton.setFont(getFontSetting());
    settingsInfoServiceConfigBasicSettingsAddButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent a_event) {
        /* if the Add button is pressed, show the add InfoService panel instead of the information
         * panel
         */
        settingsInfoServiceConfigBasicSettingsAddButton.setEnabled(false);
        informationPanel.setVisible(false);
        addInfoServicePanel.setVisible(true);
      }
    });
    
    JButton settingsInfoServiceConfigBasicSettingsRemoveButton = new JButton(JAPMessages.getString("settingsInfoServiceConfigBasicSettingsRemoveButton"));
    settingsInfoServiceConfigBasicSettingsRemoveButton.setFont(getFontSetting());
    settingsInfoServiceConfigBasicSettingsRemoveButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent a_event) {
        /* if the Remove button is pressed, remove the selected infoservice from the list of all
         * infoservices (if it is not the preferred infoservice)
         */
        InfoServiceDBEntry selectedInfoService = (InfoServiceDBEntry)(knownInfoServicesList.getSelectedValue());
        InfoServiceDBEntry preferredInfoService = InfoServiceHolder.getInstance().getPreferredInfoService();
        if (selectedInfoService != null) {
          if (selectedInfoService.equals(preferredInfoService)) {           
            JOptionPane.showMessageDialog(configPanel, JAPMessages.getString("settingsInfoServiceConfigBasicSettingsRemovePreferredError"), JAPMessages.getString("ERROR"), JOptionPane.ERROR_MESSAGE);
          }
          else {
            Database.getInstance(InfoServiceDBEntry.class).remove(selectedInfoService);
          }
        }
      }
    });

    JLabel settingsInfoServiceConfigBasicSettingsListLabel = new JLabel(JAPMessages.getString("settingsInfoServiceConfigBasicSettingsListLabel"));
    settingsInfoServiceConfigBasicSettingsListLabel.setFont(getFontSetting());

    TitledBorder settingsInfoServiceConfigBasicSettingsBorder = new TitledBorder(JAPMessages.getString("settingsInfoServiceConfigBasicSettingsBorder"));
    settingsInfoServiceConfigBasicSettingsBorder.setTitleFont(getFontSetting());
    configPanel.setBorder(settingsInfoServiceConfigBasicSettingsBorder);

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
    configPanelLayout.setConstraints(settingsInfoServiceConfigBasicSettingsListLabel, configPanelConstraints);
    configPanel.add(settingsInfoServiceConfigBasicSettingsListLabel);

    configPanelConstraints.gridx = 0;
    configPanelConstraints.gridy = 1;
    configPanelConstraints.weighty = 1.0;
    configPanelConstraints.insets = new Insets(0, 5, 10, 5);
    configPanelConstraints.fill = GridBagConstraints.BOTH;
    configPanelLayout.setConstraints(knownInfoServicesListScrollPane, configPanelConstraints);
    configPanel.add(knownInfoServicesListScrollPane);

    configPanelConstraints.gridx = 0;
    configPanelConstraints.gridy = 2;
    configPanelConstraints.weightx = 0.0;
    configPanelConstraints.weighty = 0.0;
    configPanelConstraints.gridwidth = 1;
    configPanelConstraints.fill = GridBagConstraints.NONE;
    configPanelConstraints.insets = new Insets(0, 5, 10, 5);
    configPanelLayout.setConstraints(settingsInfoServiceConfigBasicSettingsFetchInfoServicesButton, configPanelConstraints);
    configPanel.add(settingsInfoServiceConfigBasicSettingsFetchInfoServicesButton);

    configPanelConstraints.gridx = 1;
    configPanelConstraints.gridy = 2;
    configPanelConstraints.insets = new Insets(0, 5, 10, 5);
    configPanelLayout.setConstraints(settingsInfoServiceConfigBasicSettingsSetPreferredButton, configPanelConstraints);
    configPanel.add(settingsInfoServiceConfigBasicSettingsSetPreferredButton);

    configPanelConstraints.gridx = 2;
    configPanelConstraints.gridy = 2;
    configPanelConstraints.insets = new Insets(0, 5, 10, 5);
    configPanelLayout.setConstraints(settingsInfoServiceConfigBasicSettingsAddButton, configPanelConstraints);
    configPanel.add(settingsInfoServiceConfigBasicSettingsAddButton);

    configPanelConstraints.gridx = 3;
    configPanelConstraints.gridy = 2;
    configPanelConstraints.weightx = 1.0;
    configPanelConstraints.insets = new Insets(0, 5, 10, 5);
    configPanelLayout.setConstraints(settingsInfoServiceConfigBasicSettingsRemoveButton, configPanelConstraints);
    configPanel.add(settingsInfoServiceConfigBasicSettingsRemoveButton);    


    JLabel settingsInfoServiceConfigBasicSettingsInformationInterfacesLabel = new JLabel(JAPMessages.getString("settingsInfoServiceConfigBasicSettingsInformationInterfacesLabel"));
    settingsInfoServiceConfigBasicSettingsInformationInterfacesLabel.setFont(getFontSetting());

    final JTextArea infoServiceInterfaceInfoArea = new JTextArea(5, 0);
    infoServiceInterfaceInfoArea.setFont(getFontSetting());
    infoServiceInterfaceInfoArea.setOpaque(false);
    infoServiceInterfaceInfoArea.setEditable(false);
    infoServiceInterfaceInfoArea.setLineWrap(false);
    /* add an implementation for a popup menu with a copy item */
    infoServiceInterfaceInfoArea.addMouseListener(new MouseAdapter() {
      public void mousePressed(MouseEvent a_event) {
        handlePopupEvent(a_event);
      }

      public void mouseReleased(MouseEvent a_event) {
        handlePopupEvent(a_event);
      }

      private void handlePopupEvent(MouseEvent a_event) {
        if (a_event.isPopupTrigger()) {
          JPopupMenu rightButtonMenu = new JPopupMenu();
          JMenuItem copyItem = new JMenuItem(JAPMessages.getString("settingsInfoServiceConfigBasicSettingsInformationInterfacesAreaCopyItem"));
          String selectedText = infoServiceInterfaceInfoArea.getSelectedText();
          if ((selectedText == null) || (new String("")).equals(selectedText)) {
            copyItem.setEnabled(false);
          }
          copyItem.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent a_event) {
              infoServiceInterfaceInfoArea.copy();
            }
          });
          rightButtonMenu.add(copyItem);
          rightButtonMenu.show(a_event.getComponent(), a_event.getX(), a_event.getY());
        }
      }
    });

    JScrollPane infoServiceInterfaceInfoAreaScrollPane = new JScrollPane(infoServiceInterfaceInfoArea);
    infoServiceInterfaceInfoAreaScrollPane.setFont(getFontSetting());

    knownInfoServicesList.addListSelectionListener(new ListSelectionListener() {
      public void valueChanged(ListSelectionEvent event) {
        synchronized (infoServiceInterfaceInfoArea) {
          /* we need exclusive access to the interface information area */
          infoServiceInterfaceInfoArea.setText("");
          InfoServiceDBEntry selectedInfoService = (InfoServiceDBEntry) (knownInfoServicesList.getSelectedValue());
          if (selectedInfoService != null) {
            Vector infoserviceListenerInterfaces = selectedInfoService.getListenerInterfaces();
            Enumeration listenerInterfaces = infoserviceListenerInterfaces.elements();
            while (listenerInterfaces.hasMoreElements()) {
              ListenerInterface currentListenerInterface = (ListenerInterface)(listenerInterfaces.nextElement());
              if (infoServiceInterfaceInfoArea.getText().equals("") == false) {
                /* add some breaklines because it is not the first line */
                infoServiceInterfaceInfoArea.setText(infoServiceInterfaceInfoArea.getText() + "\n\n");
              }
              infoServiceInterfaceInfoArea.setText(infoServiceInterfaceInfoArea.getText() + " " + JAPMessages.getString("settingsInfoServiceConfigBasicSettingsInformationInterfacesHostInfo") + " " + currentListenerInterface.getHost() + "    " + JAPMessages.getString("settingsInfoServiceConfigBasicSettingsInformationInterfacesPortInfo") + " " + Integer.toString(currentListenerInterface.getPort()) + " ");
            }
          }
        }
      }
    });

    TitledBorder settingsInfoServiceConfigBasicSettingsInformationBorder = new TitledBorder(JAPMessages.getString("settingsInfoServiceConfigBasicSettingsInformationBorder"));
    settingsInfoServiceConfigBasicSettingsInformationBorder.setTitleFont(getFontSetting());
    informationPanel.setBorder(settingsInfoServiceConfigBasicSettingsInformationBorder);

    GridBagLayout informationPanelLayout = new GridBagLayout();
    informationPanel.setLayout(informationPanelLayout);

    GridBagConstraints informationPanelConstraints = new GridBagConstraints();
    informationPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
    informationPanelConstraints.weightx = 1.0;

    informationPanelConstraints.gridx = 0;
    informationPanelConstraints.gridy = 0;
    informationPanelConstraints.fill = GridBagConstraints.NONE;
    informationPanelConstraints.insets = new Insets(5, 5, 0, 5);
    informationPanelLayout.setConstraints(settingsInfoServiceConfigBasicSettingsInformationInterfacesLabel, informationPanelConstraints);
    informationPanel.add(settingsInfoServiceConfigBasicSettingsInformationInterfacesLabel);

    informationPanelConstraints.gridx = 0;
    informationPanelConstraints.gridy = 1;
    informationPanelConstraints.fill = GridBagConstraints.BOTH;
    informationPanelConstraints.weighty = 1.0;
    informationPanelConstraints.insets = new Insets(0, 5, 10, 5);
    informationPanelLayout.setConstraints(infoServiceInterfaceInfoAreaScrollPane, informationPanelConstraints);
    informationPanel.add(infoServiceInterfaceInfoAreaScrollPane);


    final JTextField addInfoServiceHostField = new JTextField();
    addInfoServiceHostField.setFont(getFontSetting());
    final JAPJIntField addInfoServicePortField = new JAPJIntField();
    addInfoServicePortField.setFont(getFontSetting());
    final JTextField addInfoServiceNameField = new JTextField();
    addInfoServiceNameField.setFont(getFontSetting());

    JButton settingsInfoServiceConfigBasicSettingsAddInfoServiceAddButton = new JButton(JAPMessages.getString("settingsInfoServiceConfigBasicSettingsAddInfoServiceAddButton"));
    settingsInfoServiceConfigBasicSettingsAddInfoServiceAddButton.setFont(getFontSetting());
    settingsInfoServiceConfigBasicSettingsAddInfoServiceAddButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent event) {
        /* if the Add button is pressed, we create a new InfoService from host, port (and name),
         * add it to the infoservice database and hide the add panel and show the information
         * panel
         */
        try {
          String infoServiceName = addInfoServiceNameField.getText().trim();
          if (infoServiceName.equals("")) {
            /* use gernerated default name */
            infoServiceName = null;
          }
          InfoServiceDBEntry newInfoService = new InfoServiceDBEntry(infoServiceName, new ListenerInterface(addInfoServiceHostField.getText().trim(), Integer.parseInt(addInfoServicePortField.getText().trim())).toVector(), false, true);
          Database.getInstance(InfoServiceDBEntry.class).update(newInfoService);
          addInfoServicePanel.setVisible(false);
          addInfoServiceHostField.setText("");
          addInfoServicePortField.setText("");
          addInfoServiceNameField.setText("");        
          informationPanel.setVisible(true);
          settingsInfoServiceConfigBasicSettingsAddButton.setEnabled(true);
        }
        catch (Exception e) {
          JOptionPane.showMessageDialog(addInfoServicePanel, JAPMessages.getString("settingsInfoServiceConfigBasicSettingsAddInfoServiceAddError"), JAPMessages.getString("ERROR"), JOptionPane.ERROR_MESSAGE);
        }
      }
    });
    
    JButton settingsInfoServiceConfigBasicSettingsAddInfoServiceCancelButton = new JButton(JAPMessages.getString("cancelButton"));
    settingsInfoServiceConfigBasicSettingsAddInfoServiceCancelButton.setFont(getFontSetting());
    settingsInfoServiceConfigBasicSettingsAddInfoServiceCancelButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent event) {
        /* if the Cancel button is pressed, hide the add panel and show the information panel */
        addInfoServicePanel.setVisible(false);
        addInfoServiceHostField.setText("");
        addInfoServicePortField.setText("");
        addInfoServiceNameField.setText("");        
        informationPanel.setVisible(true);
        settingsInfoServiceConfigBasicSettingsAddButton.setEnabled(true);
      }
    });
    
    JLabel settingsInfoServiceConfigBasicSettingsAddInfoServiceHostLabel = new JLabel(JAPMessages.getString("settingsInfoServiceConfigBasicSettingsAddInfoServiceHostLabel"));
    settingsInfoServiceConfigBasicSettingsAddInfoServiceHostLabel.setFont(getFontSetting());
    JLabel settingsInfoServiceConfigBasicSettingsAddInfoServicePortLabel = new JLabel(JAPMessages.getString("settingsInfoServiceConfigBasicSettingsAddInfoServicePortLabel"));
    settingsInfoServiceConfigBasicSettingsAddInfoServicePortLabel.setFont(getFontSetting());
    JLabel settingsInfoServiceConfigBasicSettingsAddInfoServiceNameLabel = new JLabel(JAPMessages.getString("settingsInfoServiceConfigBasicSettingsAddInfoServiceNameLabel"));
    settingsInfoServiceConfigBasicSettingsAddInfoServiceNameLabel.setFont(getFontSetting());

    TitledBorder settingsInfoServiceConfigBasicSettingsAddInfoServiceBorder = new TitledBorder(JAPMessages.getString("settingsInfoServiceConfigBasicSettingsAddInfoServiceBorder"));
    settingsInfoServiceConfigBasicSettingsAddInfoServiceBorder.setTitleFont(getFontSetting());
    addInfoServicePanel.setBorder(settingsInfoServiceConfigBasicSettingsAddInfoServiceBorder);

    GridBagLayout addInfoServicePanelLayout = new GridBagLayout();
    addInfoServicePanel.setLayout(addInfoServicePanelLayout);

    GridBagConstraints addInfoServicePanelConstraints = new GridBagConstraints();
    addInfoServicePanelConstraints.weighty = 0.0;

    addInfoServicePanelConstraints.gridx = 0;
    addInfoServicePanelConstraints.gridy = 0;
    addInfoServicePanelConstraints.weightx = 1.0;
    addInfoServicePanelConstraints.anchor = GridBagConstraints.NORTHWEST;
    addInfoServicePanelConstraints.fill = GridBagConstraints.NONE;
    addInfoServicePanelConstraints.insets = new Insets(5, 5, 0, 10);
    addInfoServicePanelLayout.setConstraints(settingsInfoServiceConfigBasicSettingsAddInfoServiceHostLabel, addInfoServicePanelConstraints);
    addInfoServicePanel.add(settingsInfoServiceConfigBasicSettingsAddInfoServiceHostLabel);

    addInfoServicePanelConstraints.gridx = 1;
    addInfoServicePanelConstraints.gridy = 0;
    addInfoServicePanelConstraints.weightx = 0.0;
    addInfoServicePanelConstraints.anchor = GridBagConstraints.SOUTHWEST;
    addInfoServicePanelConstraints.gridheight = 2;
    addInfoServicePanelConstraints.insets = new Insets(5, 0, 5, 5);
    addInfoServicePanelLayout.setConstraints(settingsInfoServiceConfigBasicSettingsAddInfoServiceAddButton, addInfoServicePanelConstraints);
    addInfoServicePanel.add(settingsInfoServiceConfigBasicSettingsAddInfoServiceAddButton);

    addInfoServicePanelConstraints.gridx = 0;
    addInfoServicePanelConstraints.gridy = 1;
    addInfoServicePanelConstraints.anchor = GridBagConstraints.NORTHWEST;
    addInfoServicePanelConstraints.fill = GridBagConstraints.HORIZONTAL;
    addInfoServicePanelConstraints.gridheight = 1;
    addInfoServicePanelConstraints.weightx = 1.0;
    addInfoServicePanelConstraints.insets = new Insets(0, 5, 5, 10);
    addInfoServicePanelLayout.setConstraints(addInfoServiceHostField, addInfoServicePanelConstraints);
    addInfoServicePanel.add(addInfoServiceHostField);

    addInfoServicePanelConstraints.gridx = 0;
    addInfoServicePanelConstraints.gridy = 2;
    addInfoServicePanelConstraints.fill = GridBagConstraints.NONE;
    addInfoServicePanelConstraints.insets = new Insets(0, 5, 0, 10);
    addInfoServicePanelLayout.setConstraints(settingsInfoServiceConfigBasicSettingsAddInfoServicePortLabel, addInfoServicePanelConstraints);
    addInfoServicePanel.add(settingsInfoServiceConfigBasicSettingsAddInfoServicePortLabel);

    addInfoServicePanelConstraints.gridx = 1;
    addInfoServicePanelConstraints.gridy = 2;
    addInfoServicePanelConstraints.weightx = 0.0;
    addInfoServicePanelConstraints.anchor = GridBagConstraints.SOUTHWEST;
    addInfoServicePanelConstraints.gridheight = 2;
    addInfoServicePanelConstraints.insets = new Insets(0, 0, 5, 5);
    addInfoServicePanelLayout.setConstraints(settingsInfoServiceConfigBasicSettingsAddInfoServiceCancelButton, addInfoServicePanelConstraints);
    addInfoServicePanel.add(settingsInfoServiceConfigBasicSettingsAddInfoServiceCancelButton);

    addInfoServicePanelConstraints.gridx = 0;
    addInfoServicePanelConstraints.gridy = 3;
    addInfoServicePanelConstraints.fill = GridBagConstraints.HORIZONTAL;
    addInfoServicePanelConstraints.gridheight = 1;
    addInfoServicePanelConstraints.weightx = 1.0;
    addInfoServicePanelConstraints.insets = new Insets(0, 5, 5, 10);
    addInfoServicePanelLayout.setConstraints(addInfoServicePortField, addInfoServicePanelConstraints);
    addInfoServicePanel.add(addInfoServicePortField);

    addInfoServicePanelConstraints.gridx = 0;
    addInfoServicePanelConstraints.gridy = 4;
    addInfoServicePanelConstraints.fill = GridBagConstraints.NONE;
    addInfoServicePanelConstraints.insets = new Insets(0, 5, 0, 10);
    addInfoServicePanelLayout.setConstraints(settingsInfoServiceConfigBasicSettingsAddInfoServiceNameLabel, addInfoServicePanelConstraints);
    addInfoServicePanel.add(settingsInfoServiceConfigBasicSettingsAddInfoServiceNameLabel);

    addInfoServicePanelConstraints.gridx = 0;
    addInfoServicePanelConstraints.gridy = 5;
    addInfoServicePanelConstraints.fill = GridBagConstraints.HORIZONTAL;    
    addInfoServicePanelConstraints.weighty = 1.0;
    addInfoServicePanelConstraints.insets = new Insets(0, 5, 10, 10);
    addInfoServicePanelLayout.setConstraints(addInfoServiceNameField, addInfoServicePanelConstraints);
    addInfoServicePanel.add(addInfoServiceNameField);

    GridBagLayout switchPanelLayout = new GridBagLayout();
    switchPanel.setLayout(switchPanelLayout);

    GridBagConstraints switchPanelConstraints = new GridBagConstraints();
    switchPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
    switchPanelConstraints.fill = GridBagConstraints.BOTH;
    switchPanelConstraints.weightx = 1.0;
    switchPanelConstraints.weighty = 1.0;

    /* add the information area and the add infoservice panel at the same place -> only one of
     * them is always visible
     */
    switchPanelConstraints.gridx = 0;
    switchPanelConstraints.gridy = 0;
    switchPanelLayout.setConstraints(informationPanel, switchPanelConstraints);
    switchPanel.add(informationPanel);
    switchPanelLayout.setConstraints(addInfoServicePanel, switchPanelConstraints);
    switchPanel.add(addInfoServicePanel);
    
    switchPanel.setPreferredSize(new Dimension(Math.max(informationPanel.getPreferredSize().width, addInfoServicePanel.getPreferredSize().width), Math.max(informationPanel.getPreferredSize().height, addInfoServicePanel.getPreferredSize().height)));
 
    /* make the add infoservice panel invisible */
    addInfoServicePanel.setVisible(false);

    GridBagLayout basicPanelLayout = new GridBagLayout();
    basicPanel.setLayout(basicPanelLayout);

    GridBagConstraints basicPanelConstraints = new GridBagConstraints();
    basicPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
    basicPanelConstraints.fill = GridBagConstraints.BOTH;
    basicPanelConstraints.weightx = 1.0;

    basicPanelConstraints.gridx = 0;
    basicPanelConstraints.gridy = 0;
    basicPanelConstraints.weighty = 1.0;
    basicPanelConstraints.insets = new Insets(0, 0, 5, 0);
    basicPanelLayout.setConstraints(configPanel, basicPanelConstraints);
    basicPanel.add(configPanel);
   
    basicPanelConstraints.gridx = 0;
    basicPanelConstraints.gridy = 1;
    basicPanelConstraints.weighty = 0.0;
    basicPanelConstraints.insets = new Insets(0, 0, 0, 0);
    basicPanelLayout.setConstraints(switchPanel, basicPanelConstraints);
    basicPanel.add(switchPanel);

    return basicPanel;
  }

  /**
   * Creates the infoservice advanced configuration panel with all components.
   *
   * @return The infoservice advanced configuration panel.
   */
  private JPanel createInfoServiceAdvancedPanel() {
    JPanel advancedPanel = new JPanel();
    
    final JCheckBox settingsInfoServiceConfigAdvancedSettingsDisableAutomaticRequestsBox = new JCheckBox(JAPMessages.getString("settingsInfoServiceConfigAdvancedSettingsDisableAutomaticRequestsBox"));
    settingsInfoServiceConfigAdvancedSettingsDisableAutomaticRequestsBox.setFont(getFontSetting());
    settingsInfoServiceConfigAdvancedSettingsDisableAutomaticRequestsBox.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent event) {
        /* enable/disable the automatic infoservice requests */
        JAPController.getInstance().setInfoServiceDisabled(settingsInfoServiceConfigAdvancedSettingsDisableAutomaticRequestsBox.isSelected());
      }
    });

    final JCheckBox settingsInfoServiceConfigAdvancedSettingsUseOnlyDefaultInfoServiceBox = new JCheckBox(JAPMessages.getString("settingsInfoServiceConfigAdvancedSettingsUseOnlyDefaultInfoServiceBox"));
    settingsInfoServiceConfigAdvancedSettingsUseOnlyDefaultInfoServiceBox.setFont(getFontSetting());
    settingsInfoServiceConfigAdvancedSettingsUseOnlyDefaultInfoServiceBox.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent event) {
        /* enable/disable the automatic changes of the infoservices */
        InfoServiceHolder.getInstance().setChangeInfoServices(!settingsInfoServiceConfigAdvancedSettingsUseOnlyDefaultInfoServiceBox.isSelected());
      }
    });
    
    Observer infoServicePolicyObserver = new Observer() {         
      /**
       * This is the observer implementation. If the InfoService management policy is changed,
       * this observer will update the policy checkboxes. If the panel is recreated (message via
       * the module internal message system), the observer removes itself from all observed
       * objects.
       *
       * @param a_notifier The observed Object. This should always be the InfoServiceHolder
       *                   instance, the JAPController instance or the module internal message
       *                   system at the moment.
       * @param a_message The reason of the notification. This should always be a
       *                  InfoServiceHolderMessage, a JAPControllerMessage or null at the moment
       *                  (depending on the notifier).
       */
      public void update(Observable a_notifier, Object a_message) {
        try {
          if (a_notifier == InfoServiceHolder.getInstance()) {
            /* message is from InfoServiceHolder */
            int messageCode = ((InfoServiceHolderMessage)a_message).getMessageCode();
            if (messageCode == InfoServiceHolderMessage.INFOSERVICE_MANAGEMENT_CHANGED) {
              /* the InfoService management policy was changed */
              boolean newPolicy = ((Boolean)(((InfoServiceHolderMessage)a_message).getMessageData())).booleanValue();
              settingsInfoServiceConfigAdvancedSettingsUseOnlyDefaultInfoServiceBox.setSelected(!newPolicy);
            }
          }  
          if (a_notifier == JAPController.getInstance()) {
            /* message is from JAPController */
            int messageCode = ((JAPControllerMessage)a_message).getMessageCode();
            if (messageCode == JAPControllerMessage.INFOSERVICE_POLICY_CHANGED) {
              /* the InfoService requests policy was changed */
              settingsInfoServiceConfigAdvancedSettingsDisableAutomaticRequestsBox.setSelected(JAPModel.getInstance().isInfoServiceDisabled());
            }
          }  
          if (a_notifier == m_messageSystem) {
            /* the root panel was recreated -> stop observing and remove ourself from the observed
             * objects
             */
            InfoServiceHolder.getInstance().deleteObserver(this);
            JAPController.getInstance().deleteObserver(this);
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
    m_messageSystem.addObserver(infoServicePolicyObserver);
    synchronized (InfoServiceHolder.getInstance()) {
      InfoServiceHolder.getInstance().addObserver(infoServicePolicyObserver);
      /* tricky: initialize the policy management box by calling the observer */
      infoServicePolicyObserver.update(InfoServiceHolder.getInstance(), new InfoServiceHolderMessage(InfoServiceHolderMessage.INFOSERVICE_MANAGEMENT_CHANGED, new Boolean(InfoServiceHolder.getInstance().isChangeInfoServices())));
    }
    JAPController.getInstance().addObserver(infoServicePolicyObserver);
    /* tricky: initialize the automatic requests policy checkbox by calling the observer */
    infoServicePolicyObserver.update(JAPController.getInstance(), new JAPControllerMessage(JAPControllerMessage.INFOSERVICE_POLICY_CHANGED));    

    TitledBorder settingsInfoServiceConfigAdvancedSettingsBorder = new TitledBorder(JAPMessages.getString("settingsInfoServiceConfigAdvancedSettingsBorder"));
    settingsInfoServiceConfigAdvancedSettingsBorder.setTitleFont(getFontSetting());
    advancedPanel.setBorder(settingsInfoServiceConfigAdvancedSettingsBorder);

    GridBagLayout advancedPanelLayout = new GridBagLayout();
    advancedPanel.setLayout(advancedPanelLayout);

    GridBagConstraints advancedPanelConstraints = new GridBagConstraints();
    advancedPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
    advancedPanelConstraints.fill = GridBagConstraints.NONE;
    advancedPanelConstraints.weightx = 1.0;

    advancedPanelConstraints.gridx = 0;
    advancedPanelConstraints.gridy = 0;
    advancedPanelConstraints.insets = new Insets(5, 5, 10, 5);
    advancedPanelLayout.setConstraints(settingsInfoServiceConfigAdvancedSettingsDisableAutomaticRequestsBox, advancedPanelConstraints);
    advancedPanel.add(settingsInfoServiceConfigAdvancedSettingsDisableAutomaticRequestsBox);

    advancedPanelConstraints.gridx = 0;
    advancedPanelConstraints.gridy = 1;
    advancedPanelConstraints.weighty = 1.0;
    advancedPanelConstraints.insets = new Insets(0, 5, 20, 5);
    advancedPanelLayout.setConstraints(settingsInfoServiceConfigAdvancedSettingsUseOnlyDefaultInfoServiceBox, advancedPanelConstraints);
    advancedPanel.add(settingsInfoServiceConfigAdvancedSettingsUseOnlyDefaultInfoServiceBox);

    return advancedPanel;
  }

  /**
   * Shows the fetch infoservices dialog box when the "Get list" button in the basic configuration
   * was pressed. A Vector with the fetched infoservices is returned. If there was an error while
   * fetching the infoservices, an error message is displayed and null is returned.
   *
   * @param a_parentComponent The parent component where this dialog is centered over.
   *
   * @return A Vector with the fetched infoservices or null, if there occured an error while
   *         fetching the infoservices.
   */
  private Vector showFetchInfoServicesDialog(Component a_parentComponent) {
    final JAPDialog fetchInfoServicesDialog = new JAPDialog(a_parentComponent, JAPMessages.getString("settingsInfoServiceConfigBasicSettingsFetchInfoServicesDialogTitle"));
    fetchInfoServicesDialog.disableManualClosing();
    JPanel fetchInfoServicesPanel = fetchInfoServicesDialog.getRootPanel();

    JLabel settingsInfoServiceConfigBasicSettingsFetchInfoServicesDialogFetchLabel = new JLabel(JAPMessages.getString("settingsInfoServiceConfigBasicSettingsFetchInfoServicesDialogFetchLabel"));
    settingsInfoServiceConfigBasicSettingsFetchInfoServicesDialogFetchLabel.setFont(getFontSetting());
    JLabel busyLabel = new JLabel(JAPUtil.loadImageIcon(JAPConstants.BUSYFN, true));

    final Vector fetchedInfoServices = new Vector();
    final Vector errorOccured = new Vector();

    final Thread fetchInfoServicesThread = new Thread(new Runnable() {
      public void run() {
        Vector downloadedInfoServices = InfoServiceHolder.getInstance().getInfoServices();
        fetchInfoServicesDialog.dispose();
        /* clear the interrupted flag, if it is set */
        Thread.interrupted();
        if (downloadedInfoServices == null) {
          errorOccured.addElement(new NullPointerException());
        }
        else {
          /* copy the fetched infoservices in the result Vector */
          Enumeration infoservices = downloadedInfoServices.elements();
          while (infoservices.hasMoreElements()) {
            fetchedInfoServices.addElement(infoservices.nextElement());
          }
        }
      }
    });

    JButton settingsInfoServiceConfigBasicSettingsFetchInfoServicesDialogCancelButton = new JButton(JAPMessages.getString("cancelButton"));
    settingsInfoServiceConfigBasicSettingsFetchInfoServicesDialogCancelButton.setFont(getFontSetting());
    settingsInfoServiceConfigBasicSettingsFetchInfoServicesDialogCancelButton.addActionListener(new ActionListener() {
      public void actionPerformed(ActionEvent event) {
        /* if the Cancel button is pressed, interrupt fetching of the infoservices -> it is stopped
         * immediately
         */
        fetchInfoServicesThread.interrupt();
      }
    });

    GridBagLayout fetchInfoServicesPanelLayout = new GridBagLayout();
    fetchInfoServicesPanel.setLayout(fetchInfoServicesPanelLayout);

    GridBagConstraints fetchInfoServicesPanelConstraints = new GridBagConstraints();
    fetchInfoServicesPanelConstraints.anchor = GridBagConstraints.NORTH;
    fetchInfoServicesPanelConstraints.fill = GridBagConstraints.NONE;
    fetchInfoServicesPanelConstraints.weighty = 0.0;

    fetchInfoServicesPanelConstraints.gridx = 0;
    fetchInfoServicesPanelConstraints.gridy = 0;
    fetchInfoServicesPanelConstraints.insets = new Insets(5, 5, 0, 5);
    fetchInfoServicesPanelLayout.setConstraints(settingsInfoServiceConfigBasicSettingsFetchInfoServicesDialogFetchLabel, fetchInfoServicesPanelConstraints);
    fetchInfoServicesPanel.add(settingsInfoServiceConfigBasicSettingsFetchInfoServicesDialogFetchLabel);

    fetchInfoServicesPanelConstraints.gridx = 0;
    fetchInfoServicesPanelConstraints.gridy = 1;
    fetchInfoServicesPanelConstraints.insets = new Insets(10, 5, 10, 5);
    fetchInfoServicesPanelLayout.setConstraints(busyLabel, fetchInfoServicesPanelConstraints);
    fetchInfoServicesPanel.add(busyLabel);

    fetchInfoServicesPanelConstraints.gridx = 0;
    fetchInfoServicesPanelConstraints.gridy = 2;
    fetchInfoServicesPanelConstraints.insets = new Insets(0, 5, 5, 5);
    fetchInfoServicesPanelConstraints.weighty = 1.0;
    fetchInfoServicesPanelLayout.setConstraints(settingsInfoServiceConfigBasicSettingsFetchInfoServicesDialogCancelButton, fetchInfoServicesPanelConstraints);
    fetchInfoServicesPanel.add(settingsInfoServiceConfigBasicSettingsFetchInfoServicesDialogCancelButton);

    fetchInfoServicesDialog.align();

    /* for synchronization purposes, it is necessary to show the dialog first and start the thread
     * after that event
     */
    fetchInfoServicesDialog.getInternalDialog().addWindowListener(new WindowAdapter() {
      public void windowOpened(WindowEvent a_event) {
        fetchInfoServicesDialog.getInternalDialog().removeWindowListener(this);
        fetchInfoServicesThread.start();
      }
    });

    fetchInfoServicesDialog.show();

    /* wait until the fetch-thread is ready */
    try {
      fetchInfoServicesThread.join();
    }
    catch (InterruptedException e) {
    }

    Vector returnValue = fetchedInfoServices;
    if (errorOccured.size() > 0) {
      /* there occured an error while fetching the infoservices */
      returnValue = null;
      JOptionPane.showMessageDialog(a_parentComponent, JAPMessages.getString("settingsInfoServiceConfigBasicSettingsFetchInfoServicesDialogFetchInfoServicesError"), JAPMessages.getString("ERROR"), JOptionPane.ERROR_MESSAGE);
    }
    return returnValue;
  }
  
  protected void onRootPanelShown()
  {
	  //Register help context
		JAPHelp.getInstance().getContextObj().setContext("infoservice");
  }
}
