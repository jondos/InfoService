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

import java.awt.CardLayout;
import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.util.Enumeration;
import java.util.Hashtable;

import javax.swing.JPanel;
import javax.swing.JTree;
import javax.swing.border.CompoundBorder;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;
import javax.swing.event.TreeExpansionEvent;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.event.TreeWillExpandListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeCellRenderer;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.DefaultTreeSelectionModel;
import javax.swing.tree.ExpandVetoException;
import javax.swing.tree.TreePath;
import javax.swing.tree.TreeSelectionModel;

public class JAPConfModuleSystem {
  
  private Font m_fontSetting;
  
  private JPanel m_rootPanel;
  
  private JPanel m_configurationCardsPanel;
  
  private CardLayout m_configurationCards;
    
  private JTree m_configurationTree;

  private DefaultMutableTreeNode m_configurationTreeRootNode;
  
  private Hashtable m_registratedModules;
  
  private Hashtable m_registratedPanelTitleIdentifiers;
  
  private Hashtable m_treeNodesToSymbolicNames;
  
  private Hashtable m_symbolicNamesToTreeNodes;
  
  public JAPConfModuleSystem() {   
    m_fontSetting = JAPController.getDialogFont();
    m_registratedModules = new Hashtable();
    m_registratedPanelTitleIdentifiers = new Hashtable();
    m_treeNodesToSymbolicNames = new Hashtable();
    m_symbolicNamesToTreeNodes = new Hashtable();
    m_configurationCards = new CardLayout();
    m_configurationCardsPanel = new JPanel(m_configurationCards);
    
    m_configurationTreeRootNode = new DefaultMutableTreeNode("root");    
    DefaultTreeModel configurationTreeModel = new DefaultTreeModel(m_configurationTreeRootNode);

    DefaultTreeCellRenderer configurationTreeRenderer = new DefaultTreeCellRenderer();
    configurationTreeRenderer.setClosedIcon(JAPUtil.loadImageIcon("arrow.gif", true));
    configurationTreeRenderer.setOpenIcon(JAPUtil.loadImageIcon("arrow90.gif", true));
    configurationTreeRenderer.setLeafIcon(null);
    
    TreeSelectionModel configurationTreeSelectionModel = new DefaultTreeSelectionModel() {
      public void setSelectionPath(TreePath a_treePath) {
        String symbolicName = (String)(m_treeNodesToSymbolicNames.get(a_treePath.getLastPathComponent()));
        if (symbolicName != null) {
          /* there is a panel associated to this node -> selecting this node is possible */
          super.setSelectionPath(a_treePath);
        }
      }
    };
    configurationTreeSelectionModel.setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);

    m_configurationTree = new JTree(configurationTreeModel);
    m_configurationTree.setSelectionModel(configurationTreeSelectionModel);
    m_configurationTree.setRootVisible(false);
    m_configurationTree.setEditable(false);
    m_configurationTree.setCellRenderer(configurationTreeRenderer);
    m_configurationTree.setBorder(new CompoundBorder(LineBorder.createBlackLineBorder(), new EmptyBorder(5, 5, 5, 5)));
    m_configurationTree.addTreeWillExpandListener(new TreeWillExpandListener() {
      public void treeWillCollapse(TreeExpansionEvent a_event) throws ExpandVetoException {
        throw new ExpandVetoException(a_event);
      }

      public void treeWillExpand(TreeExpansionEvent event) {
      }
    });
    m_configurationTree.addTreeSelectionListener(new TreeSelectionListener() {
      public void valueChanged(TreeSelectionEvent a_event) {
        if (a_event.isAddedPath()) {
          m_configurationCards.show(m_configurationCardsPanel, (String)(m_treeNodesToSymbolicNames.get(a_event.getPath().getLastPathComponent())));
        }
      }
    });

    m_rootPanel = new JPanel();
    
    GridBagLayout rootPanelLayout = new GridBagLayout();
    m_rootPanel.setLayout(rootPanelLayout);
    
    GridBagConstraints rootPanelConstraints = new GridBagConstraints();
    rootPanelConstraints.weightx = 0.0;
    rootPanelConstraints.weighty = 1.0;
    rootPanelConstraints.gridx = 0;
    rootPanelConstraints.gridy = 0;
    rootPanelConstraints.insets = new Insets(10, 10, 10, 10);    
    rootPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
    rootPanelConstraints.fill = GridBagConstraints.BOTH;
    rootPanelLayout.setConstraints(m_configurationTree, rootPanelConstraints);
    m_rootPanel.add(m_configurationTree);    

    rootPanelConstraints.weightx = 1.0;
    rootPanelConstraints.weighty = 1.0;
    rootPanelConstraints.gridx = 1;
    rootPanelConstraints.gridy = 0;
    rootPanelConstraints.insets = new Insets(10, 10, 10, 10);    
    rootPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
    rootPanelConstraints.fill = GridBagConstraints.BOTH;
    rootPanelLayout.setConstraints(m_configurationCardsPanel, rootPanelConstraints);
    m_rootPanel.add(m_configurationCardsPanel);
  }

  
  public DefaultMutableTreeNode addConfigurationModule(DefaultMutableTreeNode a_parentNode, AbstractJAPConfModule a_module, String a_symbolicName) {
    DefaultMutableTreeNode moduleNode = new DefaultMutableTreeNode(a_module.getTabTitle());
    synchronized (this) {
      a_parentNode.add(moduleNode);
      m_configurationCardsPanel.add(a_module.getRootPanel(), a_symbolicName);
      m_registratedModules.put(moduleNode, a_module);
      m_treeNodesToSymbolicNames.put(moduleNode, a_symbolicName);
      m_symbolicNamesToTreeNodes.put(a_symbolicName, moduleNode);
    }
    return moduleNode;
  }
  
  public DefaultMutableTreeNode addComponent(DefaultMutableTreeNode a_parentNode, Component a_component, String a_nodeNameIdentifier, String a_symbolicName) {
    DefaultMutableTreeNode componentNode = new DefaultMutableTreeNode(JAPMessages.getString(a_nodeNameIdentifier));
    synchronized (this) {
      a_parentNode.add(componentNode);
      m_registratedPanelTitleIdentifiers.put(componentNode, a_nodeNameIdentifier);      
      if (a_component != null) {
        /* this node has an associated component -> it will be selectable */
        m_configurationCardsPanel.add(a_component, a_symbolicName);
        m_treeNodesToSymbolicNames.put(componentNode, a_symbolicName);
        m_symbolicNamesToTreeNodes.put(a_symbolicName, componentNode);     
      }
    }
    return componentNode;
  }
  
  public DefaultMutableTreeNode getConfigurationTreeRootNode() {
    return m_configurationTreeRootNode;
  } 

  public JTree getConfigurationTree() {
    return m_configurationTree;
  }

  public JPanel getRootPanel() {
    return m_rootPanel;
  }

  public void selectNode(String a_symbolicName) {
    synchronized (this) {
      DefaultMutableTreeNode treeNodeToSelect = (DefaultMutableTreeNode)(m_symbolicNamesToTreeNodes.get(a_symbolicName));
      if (treeNodeToSelect != null) {
        /* the symbolic name matches a node in the tree */
        m_configurationTree.setSelectionPath(new TreePath(treeNodeToSelect.getPath()));
      }
    }
  }
  
  public boolean processOkPressedEvent() { 
    boolean returnValue = true;
    synchronized (this) {
      /* Call the event handler of all configuration modules. */
      Enumeration confModules = m_registratedModules.elements();
      while (confModules.hasMoreElements()) {
        AbstractJAPConfModule confModule = (AbstractJAPConfModule) (confModules.nextElement());
        if (!confModule.okPressed()) {
          returnValue = false;
        }
      }
    }
    return returnValue;
  }
  
  public void processCancelPressedEvent() {
    synchronized (this) {
      /* Call the event handler of all configuration modules. */
      Enumeration confModules = m_registratedModules.elements();
      while (confModules.hasMoreElements()) {
        ((AbstractJAPConfModule)(confModules.nextElement())).cancelPressed();
      }
    }
  }
  
  public void processResetToDefaultsPressedEvent() {
    synchronized (this) {
      /* Call the event handler of all configuration modules. */
      Enumeration confModules = m_registratedModules.elements();
      while (confModules.hasMoreElements()) {
        ((AbstractJAPConfModule)(confModules.nextElement())).resetToDefaultsPressed();
      }
    }
  }
  
  public void processUpdateValuesEvent() {
    synchronized (this) {
      /* Call the event handler of all configuration modules. */
      Enumeration confModules = m_registratedModules.elements();
      while (confModules.hasMoreElements()) {
        ((AbstractJAPConfModule)(confModules.nextElement())).updateValues();
      }
    }
  }
  
  public void repaintEverything() {
    synchronized (this) {
      /* update the nodes in the tree */
      Enumeration moduleNodes = m_registratedModules.keys();
      while (moduleNodes.hasMoreElements()) {
        DefaultMutableTreeNode currentNode = (DefaultMutableTreeNode)(moduleNodes.nextElement());
        currentNode.setUserObject(((AbstractJAPConfModule)(m_registratedModules.get(currentNode))).getTabTitle());
      }
      Enumeration panelNodes = m_registratedPanelTitleIdentifiers.keys();
      while (panelNodes.hasMoreElements()) {
        DefaultMutableTreeNode currentNode = (DefaultMutableTreeNode)(panelNodes.nextElement());
        currentNode.setUserObject(JAPMessages.getString((String)(m_registratedPanelTitleIdentifiers.get(currentNode))));
      }
      /* Call the event handler of all configuration modules. */
      Enumeration confModules = m_registratedModules.elements();
      while (confModules.hasMoreElements()) {
        ((AbstractJAPConfModule)(confModules.nextElement())).recreateRootPanel();
      }
    }       
  }
  
  public void createSavePoints() {
    synchronized (this) {
      /* Call the create savepoint handler of all configuration modules. */
      Enumeration confModules = m_registratedModules.elements();
      while (confModules.hasMoreElements()) {
        ((AbstractJAPConfModule)(confModules.nextElement())).createSavePoint();
      }
    }
  }
      
}