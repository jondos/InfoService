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

import java.io.File;
import java.util.Dictionary;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Vector;
import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import pay.gui.AccountSettingsPanel;
//import update.JAPUpdate;
import javax.swing.*;
import javax.swing.tree.*;
import javax.swing.event.*;
import java.awt.*;
import javax.swing.border.*;
final class JAPConf extends JDialog
{

	final class TreeElement
	{
		final String m_Name;
		final String m_Value;
		TreeElement(String name,String value)
		{
			m_Name=name;
			m_Value=value;
		}

		public String toString()
		{
			return m_Name;
		}

		public String getValue()
		{
			return m_Value;
		}
	}
	final static public String PORT_TAB = "PORT_TAB";
	final static public String UI_TAB = "UI_TAB";
	final static public String UPDATE_TAB = "UPDATE_TAB";
	final static public String PROXY_TAB = "PROXY_TAB";
	final static public String INFOSERVICE_TAB = "INFOSERVICE_TAB";
	final static public String ANON_TAB = "ANON_TAB";
	final static public String CERT_TAB = "CERT_TAB";
	final static public String TOR_TAB = "TOR_TAB";
	final static public String DEBUG_TAB = "DEBUG_TAB";
	final static public String PAYMENT_TAB = "PAYMENT_TAB";

	/**
	 * This constant is a symbolic name for accessing the forwarding tab.
	 */
	final static public String FORWARD_TAB = "FORWARD_TAB";

	private static JAPConf japConfInstance = null;

	private JAPController m_Controller;

	private JAPJIntField m_tfListenerPortNumber;
	private JCheckBox m_cbListenerIsLocal;
	private JLabel m_labelPortnumber1, m_labelPortnumber2;

	private TitledBorder m_borderSettingsListener;

	private JCheckBox m_cbProxy;
	private JAPJIntField m_tfProxyPortNumber;
	private JTextField m_tfProxyHost;
	private JComboBox m_comboProxyType;
	private JCheckBox m_cbProxyAuthentication;
	private JTextField m_tfProxyAuthenticationUserID;
	private JLabel m_labelProxyHost, m_labelProxyPort, m_labelProxyType, m_labelProxyAuthUserID;

	private JCheckBox m_cbDebugGui;
	private JCheckBox m_cbDebugNet;
	private JCheckBox m_cbDebugThread;
	private JCheckBox m_cbDebugMisc;
	private JCheckBox m_cbShowDebugConsole;
	//private JCheckBox m_cbSaveWindowPositions;
	private JCheckBox m_cbCertCheckDisabled;

	private JSlider m_sliderDebugLevel;

	//private JComboBox m_comboLanguage;
	//private boolean m_bIgnoreComboLanguageEvents = false;

	private JCheckBox m_cbDummyTraffic;
	private JSlider m_sliderDummyTrafficIntervall;

	private JPanel m_Tabs;
	private CardLayout m_TabsLayout;
	private JTree m_Tree;
	/**
	 * Stores the index of the various tabs in the tabbed pane.
	 */
	//private Hashtable m_tabOrder;

	private JPanel m_pPort, m_pFirewall, m_pMisc;
	private JButton m_bttnDefaultConfig, m_bttnCancel;

	//private JFrame m_frmParent;

	private JAPConf m_JapConf;

	private Font m_fontControls;

	/**
	 * Stores all loaded configuration modules (AbstractJAPConfModule). This is needed for handling
	 * events.
	 */
	private Vector m_confModules;
	private static File m_fileCurrentDir;

	//Einfug
	//private JAPUpdate update;
	private boolean loadPay = false;

	public static JAPConf getInstance()
	{
		return japConfInstance;
	}

	public JAPConf(JFrame frmParent, boolean loadPay)
	{
		super(frmParent);
		m_confModules = new Vector();
		this.loadPay = loadPay;
		/* set the instance pointer */
		japConfInstance = this;
		//m_frmParent = frmParent;
		m_Controller = JAPController.getController();
		setModal(true);
		setTitle(JAPMessages.getString("settingsDialog"));
		m_JapConf = this;
		JPanel pContainer = new JPanel();
		m_TabsLayout=new CardLayout();
		m_Tabs = new JPanel(m_TabsLayout);
		m_fontControls = JAPController.getDialogFont();
		GridBagLayout gbl=new GridBagLayout();
		pContainer.setLayout(gbl);
		m_Tabs.setFont(m_fontControls);
		m_pPort = buildPortPanel();
		m_pFirewall = buildProxyPanel();
		m_pMisc = buildMiscPanel();

		AbstractJAPConfModule uiModule = new JAPConfUI();
		AbstractJAPConfModule updateModule = new JAPConfUpdate();
		AbstractJAPConfModule infoServiceModule = new JAPConfInfoService();
		AbstractJAPConfModule certModule = new JAPConfCert();
		AbstractJAPConfModule torModule = new JAPConfTor();
		AbstractJAPConfModule anonModule = new JAPConfAnon(null);
		AbstractJAPConfModule accountsModule = null;
		if (loadPay)
		{
			accountsModule = new AccountSettingsPanel();
		}
		AbstractJAPConfModule routingModule = new JAPConfRouting();

		/* there is no need to set the font because it is already set in the constructor but so it is
		 * save for the future
		 */
		//remove due to performance reasons...
		//infoServiceModule.setFontSetting(m_fontControls);
		//certModule.setFontSetting(m_fontControls);
		//torModule.setFontSetting(m_fontControls);
		//anonModul

		m_confModules.addElement(uiModule);
		m_confModules.addElement(updateModule);
		m_confModules.addElement(infoServiceModule);
		m_confModules.addElement(certModule);
		m_confModules.addElement(torModule);
		m_confModules.addElement(anonModule);
		if (JAPConstants.WITH_BLOCKINGRESISTANCE)
		{
			m_confModules.addElement(routingModule);
		}
		if (loadPay)
		{
			m_confModules.addElement(accountsModule);
		}

		/* create the hashtable, which stores the index of the tabs in the tabbed pane */
		//m_tabOrder = new Hashtable();

		m_Tabs.add(m_pPort,PORT_TAB);
		//m_Tabs.addTab(JAPMessages.getString("confListenerTab"), null, m_pPort);
		//m_tabOrder.put(new Integer(PORT_TAB), new Integer(m_Tabs.getTabCount() - 1));
		m_Tabs.add(m_pFirewall,PROXY_TAB);
//		m_tabOrder.put(new Integer(PROXY_TAB), new Integer(m_Tabs.getTabCount() - 1));
		m_Tabs.add(uiModule.getRootPanel(),UI_TAB);
		m_Tabs.add(updateModule.getRootPanel(),UPDATE_TAB);
		m_Tabs.add(infoServiceModule.getRootPanel(),INFOSERVICE_TAB);
		//m_tabOrder.put(new Integer(INFO_TAB), new Integer(m_Tabs.getTabCount() - 1));
		JTabbedPane tabs=new JTabbedPane();
		tabs.addTab(anonModule.getTabTitle(),anonModule.getRootPanel());
		tabs.addTab(torModule.getTabTitle(),torModule.getRootPanel());
		tabs.addTab("Allgemein",new JPanel());
		m_Tabs.add(tabs,ANON_TAB);
		//m_tabOrder.put(new Integer(ANON_TAB), new Integer(m_Tabs.getTabCount() - 1));
		m_Tabs.add(certModule.getRootPanel(),CERT_TAB);
		//m_tabOrder.put(new Integer(CERT_TAB), new Integer(m_Tabs.getTabCount() - 1));
		//m_Tabs.addTab(torModule.getTabTitle(), null, torModule.getRootPanel());
		//m_tabOrder.put(new Integer(TOR_TAB), new Integer(m_Tabs.getTabCount() - 1));
		if (JAPConstants.WITH_BLOCKINGRESISTANCE)
		{
			m_Tabs.add(routingModule.getRootPanel(),FORWARD_TAB);
			//m_tabOrder.put(new Integer(FORWARD_TAB), new Integer(m_Tabs.getTabCount() - 1));
		}
		if (loadPay)
		{
			m_Tabs.add(accountsModule.getRootPanel(),PAYMENT_TAB);
			//m_tabOrder.put(new Integer(KONTO_TAB), new Integer(m_Tabs.getTabCount() - 1));
		}

		if (!JAPModel.isSmallDisplay())
		{
			m_Tabs.add(m_pMisc,DEBUG_TAB);
	//		m_tabOrder.put(new Integer(MISC_TAB), new Integer(m_Tabs.getTabCount() - 1));
		}
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
		JButton bttnHelp = new JButton(JAPMessages.getString("updateM_bttnHelp"));
		bttnHelp.setFont(m_fontControls);
		buttonPanel.add(bttnHelp);

		m_bttnDefaultConfig = new JButton(JAPMessages.getString("bttnDefaultConfig"));
		m_bttnDefaultConfig.setFont(m_fontControls);
		m_bttnDefaultConfig.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				resetToDefault();
			}
		});
		if (!JAPModel.isSmallDisplay())
		{
			buttonPanel.add(m_bttnDefaultConfig);
		}
		m_bttnCancel = new JButton(JAPMessages.getString("cancelButton"));
		m_bttnCancel.setFont(m_fontControls);
		m_bttnCancel.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				cancelPressed();
			}
		});
		buttonPanel.add(m_bttnCancel);
		JButton ok = new JButton(JAPMessages.getString("okButton"));
		ok.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				okPressed();
			}
		});
		/*		if (loadPay)
		  {
		   ok.addActionListener( ( (pay.view.PayView) m_pKonto).userPanel);
		  }*/
		ok.setFont(m_fontControls);
		buttonPanel.add(ok);
		buttonPanel.add(new JLabel("   "));
		getRootPane().setDefaultButton(ok);


//				container.add(new JLabel(new ImageIcon(m_Controller.JAPICONFN)), BorderLayout.WEST);
//new Config select...
		DefaultMutableTreeNode rootNode=new DefaultMutableTreeNode("root");
		DefaultTreeModel treeModel=new DefaultTreeModel(rootNode);
		DefaultMutableTreeNode node=new DefaultMutableTreeNode(new TreeElement("Erscheinungsbild",UI_TAB));
		rootNode.add(node);
		if (loadPay)
		{
		node=new DefaultMutableTreeNode("Bezahlung");
		rootNode.add(node);
		}
		node=new DefaultMutableTreeNode(new TreeElement("JAP-Update",UPDATE_TAB));
		rootNode.add(node);
		DefaultMutableTreeNode nodeNet=new DefaultMutableTreeNode("Netz");
		rootNode.add(nodeNet);
		DefaultMutableTreeNode n2=new DefaultMutableTreeNode(new TreeElement("Listener",PORT_TAB));
		nodeNet.add(n2);
		n2=new DefaultMutableTreeNode(new TreeElement("Firewall",PROXY_TAB));
		nodeNet.add(n2);
		DefaultMutableTreeNode nodeAnon=new DefaultMutableTreeNode("Anonymität");
		rootNode.add(nodeAnon);
		n2=new DefaultMutableTreeNode(new TreeElement("InfoService",INFOSERVICE_TAB));
		nodeAnon.add(n2);
		n2=new DefaultMutableTreeNode(new TreeElement("Dienste",ANON_TAB));
		nodeAnon.add(n2);
		n2=new DefaultMutableTreeNode(new TreeElement("Blockungsresistenz",FORWARD_TAB));
		nodeAnon.add(n2);
		node=new DefaultMutableTreeNode(new TreeElement("Zertifikate",CERT_TAB));
		rootNode.add(node);
		node=new DefaultMutableTreeNode(new TreeElement("Debugging",DEBUG_TAB));
		rootNode.add(node);
		DefaultTreeCellRenderer renderer=new DefaultTreeCellRenderer();
		renderer.setClosedIcon(JAPUtil.loadImageIcon("arrow.gif", true));
		renderer.setOpenIcon(JAPUtil.loadImageIcon("arrow90.gif", true));
		renderer.setLeafIcon(null);
		m_Tree=new JTree(treeModel);
		TreeSelectionModel sm=new DefaultTreeSelectionModel(){
			public void  setSelectionPath(TreePath t)
				{
					if(((TreeNode)t.getLastPathComponent()).isLeaf())
						super.setSelectionPath(t);
					//return false;
				}
		};

		sm.setSelectionMode(TreeSelectionModel.SINGLE_TREE_SELECTION);
		m_Tree.setSelectionModel(sm);
		m_Tree.setRootVisible(false);
		m_Tree.setEditable(false);
		m_Tree.setCellRenderer(renderer);
		m_Tree.setBorder(new CompoundBorder(LineBorder.createBlackLineBorder(),new EmptyBorder(5,5,5,5)));
		m_Tree.addTreeSelectionListener(new TreeSelectionListener()
									  {
			public void valueChanged(TreeSelectionEvent e)
			{
				if(e.isAddedPath())
				{
					DefaultMutableTreeNode n=(DefaultMutableTreeNode)e.getPath().getLastPathComponent();
					m_TabsLayout.show(m_Tabs,((TreeElement)n.getUserObject()).getValue());
				}
			}
		});
		m_Tree.expandPath(new TreePath(nodeNet.getPath()));
		m_Tree.expandPath(new TreePath(nodeAnon.getPath()));
		m_Tree.setSelectionRow(0);
		m_Tree.addTreeWillExpandListener(new TreeWillExpandListener(){
			public void treeWillCollapse(TreeExpansionEvent event) throws ExpandVetoException
			{
				throw new ExpandVetoException(event);
			}

			public void treeWillExpand(TreeExpansionEvent event)
			{
			}
		});

		GridBagConstraints c=new GridBagConstraints();
		c.weighty=1;
		c.weightx=0;
		c.insets=new Insets(20,10,10,10);
		c.fill=GridBagConstraints.VERTICAL;
		pContainer.add(m_Tree,c);
		c.gridx=1;
		c.weightx=1;
		c.fill=GridBagConstraints.BOTH;
		pContainer.add(m_Tabs,c);
		c.gridx=0;
		c.gridwidth=2;
		c.weightx=1;
		c.weighty=0;
		c.fill=GridBagConstraints.HORIZONTAL;
		c.insets=new Insets(0,10,10,10);
		pContainer.add(buttonPanel,c);

		getContentPane().add(pContainer);
		updateValues();
		// largest tab to front
		selectCard(ANON_TAB);
		if (JAPModel.isSmallDisplay())
		{
			setSize(240, 300);
			setLocation(0, 0);
		}
		else
		{
			pack();
			JAPUtil.centerFrame(this);
		}
	}

	/**
	 * This method overwrites the method show() from java.awt.Dialog. We need it for
	 * creating the module savepoints. After this, we call the parent show() method.
	 */
	public void show()
	{
		/* Call the create savepoint handler of all configuration modules. Because this is
		 * a modal dialog, every call of show() from JAPView is equal to the start of the
		 * configuration by the user.
		 */
		Enumeration confModules = m_confModules.elements();
		while (confModules.hasMoreElements())
		{
			( (AbstractJAPConfModule) (confModules.nextElement())).createSavePoint();
		}
		/* call the original method */
		super.show();
	}

	protected JPanel buildPortPanel()
	{
		m_labelPortnumber1 = new JLabel(JAPMessages.getString("settingsPort1"));
		m_labelPortnumber1.setFont(m_fontControls);
		m_labelPortnumber2 = new JLabel(JAPMessages.getString("settingsPort2"));
		m_labelPortnumber2.setFont(m_fontControls);
		m_tfListenerPortNumber = new JAPJIntField();
		m_tfListenerPortNumber.setFont(m_fontControls);
		m_tfListenerPortNumber.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				okPressed();
			}
		});
		m_cbListenerIsLocal = new JCheckBox(JAPMessages.getString("settingsListenerCheckBox"));
		m_cbListenerIsLocal.setFont(m_fontControls);
		// set Font in listenerCheckBox in same color as in portnumberLabel1
		m_cbListenerIsLocal.setForeground(m_labelPortnumber1.getForeground());

		//m_tfListenerPortNumberSocks.setEnabled(false);
		//}
		//m_tfListenerPortNumberSocks = new JAPJIntField();
		//m_tfListenerPortNumberSocks.setFont(m_fontControls);

		JPanel p = new JPanel();
		p.setLayout(new BorderLayout());
		m_borderSettingsListener = new TitledBorder(JAPMessages.getString("settingsListenerBorder"));
		m_borderSettingsListener.setTitleFont(m_fontControls);
		p.setBorder(m_borderSettingsListener);
		JPanel p1 = new JPanel();
		GridBagLayout g = new GridBagLayout();
		p1.setLayout(g);
		p1.setBorder(new EmptyBorder(5, 10, 10, 10));
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = c.NORTHWEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridwidth = 1;
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1;
		c.weighty = 0;
		Insets normInsets = new Insets(0, 0, 3, 0);
		c.insets = normInsets;
		g.setConstraints(m_labelPortnumber1, c);
		p1.add(m_labelPortnumber1);
		c.gridy = 1;
		g.setConstraints(m_labelPortnumber2, c);
		p1.add(m_labelPortnumber2);
		c.gridy = 2;
		g.setConstraints(m_tfListenerPortNumber, c);
		p1.add(m_tfListenerPortNumber);
		JSeparator seperator = new JSeparator();
		c.gridy = 3;
		c.insets = new Insets(10, 0, 0, 0);
		g.setConstraints(seperator, c);
		p1.add(seperator);
		c.insets = normInsets;
		c.gridy = 4;
		c.insets = new Insets(10, 0, 0, 0);
		//m_labelSocksPortNumber = new JLabel(JAPMessages.getString("settingsListenerSOCKS"));
		//m_labelSocksPortNumber.setFont(m_fontControls);
		//p1.add(m_labelSocksPortNumber, c);
		//c.gridy = 5;
		//g.setConstraints(m_tfListenerPortNumberSocks, c);
		//p1.add(m_tfListenerPortNumberSocks);
		//c.gridy = 6;
		//JSeparator seperator2 = new JSeparator();
		//c.insets = new Insets(10, 0, 0, 0);
		//g.setConstraints(seperator2, c);
		//p1.add(seperator2);

		c.gridy = 7;
		c.insets = new Insets(10, 0, 0, 0);
		g.setConstraints(m_cbListenerIsLocal, c);
		p1.add(m_cbListenerIsLocal);
		p.add(p1, BorderLayout.NORTH);
		return p;
	}

	protected JPanel buildProxyPanel()
	{
		m_cbProxy = new JCheckBox(JAPMessages.getString("settingsProxyCheckBox"));
		m_cbProxy.setFont(m_fontControls);
		m_comboProxyType = new JComboBox();
		m_comboProxyType.setFont(m_fontControls);
		m_comboProxyType.addItem(JAPMessages.getString("settingsProxyTypeHTTP"));
		m_comboProxyType.addItem(JAPMessages.getString("settingsProxyTypeSOCKS"));
		m_tfProxyHost = new JTextField();
		m_tfProxyHost.setFont(m_fontControls);
		m_tfProxyPortNumber = new JAPJIntField();
		m_tfProxyPortNumber.setFont(m_fontControls);
		m_tfProxyHost.setEnabled(JAPModel.getUseFirewall());
		m_tfProxyPortNumber.setEnabled(JAPModel.getUseFirewall());
		m_cbProxy.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				boolean b = m_cbProxy.isSelected();
				m_comboProxyType.setEnabled(b);
				m_tfProxyHost.setEnabled(b);
				m_tfProxyPortNumber.setEnabled(b);
				m_cbProxyAuthentication.setEnabled(b);
				m_tfProxyAuthenticationUserID.setEnabled(b);
			}
		});
		m_tfProxyHost.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				okPressed();
			}
		});
		m_tfProxyPortNumber.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				okPressed();
			}
		});
		m_cbProxyAuthentication = new JCheckBox(JAPMessages.getString("settingsProxyAuthenticationCheckBox"));
		m_cbProxyAuthentication.setFont(m_fontControls);
		m_tfProxyAuthenticationUserID = new JTextField();
		m_tfProxyAuthenticationUserID.setFont(m_fontControls);
		m_cbProxyAuthentication.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				m_tfProxyAuthenticationUserID.setEnabled(m_cbProxyAuthentication.isSelected());
			}
		});
		m_labelProxyHost = new JLabel(JAPMessages.getString("settingsProxyHost"));
		m_labelProxyHost.setFont(m_fontControls);
		m_labelProxyPort = new JLabel(JAPMessages.getString("settingsProxyPort"));
		m_labelProxyPort.setFont(m_fontControls);
		m_labelProxyType = new JLabel(JAPMessages.getString("settingsProxyType"));
		m_labelProxyType.setFont(m_fontControls);
		m_labelProxyAuthUserID = new JLabel(JAPMessages.getString("settingsProxyAuthUserID"));
		m_labelProxyAuthUserID.setFont(m_fontControls);
		// set Font in m_cbProxy in same color as in proxyPortLabel
		m_cbProxy.setForeground(m_labelProxyPort.getForeground());
		m_cbProxyAuthentication.setForeground(m_labelProxyPort.getForeground());

		JPanel p = new JPanel();
		p.setLayout(new BorderLayout());
		TitledBorder border = new TitledBorder(JAPMessages.getString("settingsProxyBorder"));
		border.setTitleFont(m_fontControls);
		p.setBorder(border);
		JPanel p1 = new JPanel();
		GridBagLayout g = new GridBagLayout();
		p1.setLayout(g);
		if (JAPModel.isSmallDisplay())
		{
			p1.setBorder(new EmptyBorder(1, 10, 1, 10));
		}
		else
		{
			p1.setBorder(new EmptyBorder(5, 10, 10, 10));
		}
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = c.NORTHWEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridwidth = 1;
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 1;
		c.weighty = 0;
		Insets normInsets;
		if (JAPModel.isSmallDisplay())
		{
			normInsets = new Insets(0, 0, 1, 0);
		}
		else
		{
			normInsets = new Insets(0, 0, 3, 0);
		}
		c.insets = normInsets;
		g.setConstraints(m_cbProxy, c);
		p1.add(m_cbProxy);
		c.gridy = 1;
		g.setConstraints(m_labelProxyType, c);
		c.gridy = 2;
		p1.add(m_labelProxyType);
		g.setConstraints(m_comboProxyType, c);
		c.gridy = 3;
		p1.add(m_comboProxyType);
		g.setConstraints(m_labelProxyHost, c);
		p1.add(m_labelProxyHost);
		c.gridy = 4;
		g.setConstraints(m_tfProxyHost, c);
		p1.add(m_tfProxyHost);
		c.gridy = 5;
		g.setConstraints(m_labelProxyPort, c);
		p1.add(m_labelProxyPort);
		c.gridy = 6;
		g.setConstraints(m_tfProxyPortNumber, c);
		p1.add(m_tfProxyPortNumber);
		JSeparator seperator = new JSeparator();
		c.gridy = 7;
		if (JAPModel.isSmallDisplay())
		{
			c.insets = new Insets(5, 0, 1, 0);
		}
		else
		{
			c.insets = new Insets(10, 0, 3, 0);
		}
		g.setConstraints(seperator, c);
		p1.add(seperator);
		c.insets = normInsets;
		c.gridy = 8;
		//c.insets=new Insets(10,0,0,0);
		g.setConstraints(m_cbProxyAuthentication, c);
		p1.add(m_cbProxyAuthentication);
		c.gridy = 9;
		g.setConstraints(m_labelProxyAuthUserID, c);
		p1.add(m_labelProxyAuthUserID);
		c.gridy = 10;
		g.setConstraints(m_tfProxyAuthenticationUserID, c);
		p1.add(m_tfProxyAuthenticationUserID);
		c.gridy = 11;
		p.add(p1, BorderLayout.NORTH);
		return p;
	}

	protected JPanel buildMiscPanel()
	{
		JPanel p = new JPanel();
		p.setLayout(new BorderLayout());

		// Panel for Look and Feel Options
		//JPanel p1 = new JPanel();
		//p1.setLayout(new GridLayout(2, 2));
		//p1.setBorder(new TitledBorder(JAPMessages.getString("settingsLookAndFeelBorder")));
		//p1.add(new JLabel(JAPMessages.getString("settingsLookAndFeel")));
		/*JComboBox c = new JComboBox();
		LookAndFeelInfo[] lf = UIManager.getInstalledLookAndFeels();
		String currentLf = UIManager.getLookAndFeel().getClass().getName();
		// add menu items
		for (int lfidx = 0; lfidx < lf.length; lfidx++)
		{
			c.addItem(lf[lfidx].getName());
		}
		// select the current
		int lfidx;
		for (lfidx = 0; lfidx < lf.length; lfidx++)
		{
			if (lf[lfidx].getClassName().equals(currentLf))
			{
				c.setSelectedIndex(lfidx);
				break;
			}
		}
		if (! (lfidx < lf.length))
		{
			c.addItem("(unknown)");
			c.setSelectedIndex(lfidx);
		}
		c.addItemListener(new ItemListener()
		{
			public void itemStateChanged(ItemEvent e)
			{
				if (e.getStateChange() == e.SELECTED)
				{
					try
					{
						UIManager.setLookAndFeel(UIManager.getInstalledLookAndFeels()[ ( (JComboBox) e.
							getItemSelectable()).getSelectedIndex()].getClassName());
//									SwingUtilities.updateComponentTreeUI(m_frmParent);
//									SwingUtilities.updateComponentTreeUI(SwingUtilities.getRoot(((JComboBox)e.getItemSelectable())));
						JOptionPane.showMessageDialog(m_JapConf,
							JAPMessages.getString("confLookAndFeelChanged"),
							JAPMessages.getString("information"), JOptionPane.INFORMATION_MESSAGE);
					}
					catch (Exception ie)
					{
					}
				}
			}
		});
		p1.add(c);*/
		//p1.add(new JLabel(JAPMessages.getString("settingsLanguage")));
		/*m_comboLanguage = new JComboBox();
		m_comboLanguage.addItem("Deutsch");
		m_comboLanguage.addItem("English");
		m_comboLanguage.addItem("Fran\u00E7ais");
		m_comboLanguage.addItemListener(new ItemListener()
		{
			public void itemStateChanged(ItemEvent e)
			{
				if (!m_bIgnoreComboLanguageEvents && e.getStateChange() == e.SELECTED)
				{
					try
					{
						JOptionPane.showMessageDialog(m_JapConf, JAPMessages.getString("confLanguageChanged"),
							JAPMessages.getString("information"), JOptionPane.INFORMATION_MESSAGE);
					}
					catch (Exception ie)
					{
					}
				}
			}
		});

		p1.add(m_comboLanguage);
*/
		// Panel for Misc Options
		JPanel p2 = new JPanel();
		p2.setLayout(new BorderLayout());
		p2.setBorder(new TitledBorder(JAPMessages.getString("miscconfigBorder")));
		JButton bttnPing = new JButton(JAPMessages.getString("bttnPing"));
		/*				bttnPing.addActionListener(new ActionListener()
		  {
		   public void actionPerformed(ActionEvent e)
		 {
		  AnonServerDBEntry[] a=new AnonServerDBEntry[1];
//								a[0]=new AnonServerDBEntry(m_Controller.anonHostName,m_Controller.anonHostName,m_Controller.anonPortNumber+1);
		  a[0]=new AnonServerDBEntry(m_Controller.getAnonServer().getHost(),m_Controller.getAnonServer().getHost(),m_Controller.getAnonServer().getPort()+1);
		  JAPRoundTripTimeView v=new JAPRoundTripTimeView(m_Controller.getView(),a);
//								v.show();
		 }
		  });
		 */
		JButton bttnMonitor = new JButton(JAPMessages.getString("bttnMonitor"));
//				bttnMonitor.setEnabled(false);
		bttnMonitor.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				Cursor c1 = getCursor();
				setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				//m_Controller.fetchAnonServers();
				JAPCascadeMonitorView v = new JAPCascadeMonitorView(m_Controller.getView());
				//updateValues(); //THIS IS WRONG!!!!
				//okPressed();
				setCursor(c1);

			}
		});
		m_cbDummyTraffic = new JCheckBox("Send dummy packet every x seconds:");
		m_cbDummyTraffic.addItemListener(new ItemListener()
		{
			public void itemStateChanged(ItemEvent e)
			{
				m_sliderDummyTrafficIntervall.setEnabled(e.getStateChange() == e.SELECTED);
			}
		});

		m_cbCertCheckDisabled = new JCheckBox("Disable check of certificates");
		//m_cbSaveWindowPositions = new JCheckBox("Remember Location of JAP");
		JPanel p22 = new JPanel();
		GridBagLayout gb = new GridBagLayout();
		p22.setLayout(gb);
		GridBagConstraints lc = new GridBagConstraints();

		//p22.add(bttnPing);
		//////////////////////////////////////////////////////////////////
		//Einfug
		//JButton testButton = new JButton("Update");
		//testButton.addActionListener(new ActionListener()
		//{
	//		public void actionPerformed(ActionEvent e)
	//		{
	//			Cursor c1 = getCursor();
	//			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
	//			cancelPressed();
	//			update = new JAPUpdate();
//  if (update == null){

				//         }//fi
				//updateValues();
	//			setCursor(c1);
	//		}
	//	});
	//	testButton.setVisible(true);
	//	testButton.setEnabled(true);
		lc.gridx = 0;
		lc.gridy = 0;
		lc.gridheight = 1;
		lc.gridwidth = 1;
		lc.anchor = lc.NORTHWEST;
		lc.fill = lc.HORIZONTAL;
		lc.weightx = 1;
		lc.weighty = 1;
		//gb.setConstraints(testButton, lc);
		//p22.add(testButton);
		lc.gridy++;
		//////////////////////////////////////////////////////////////////
		gb.setConstraints(bttnMonitor, lc);
		p22.add(bttnMonitor);
		lc.gridy++;
		gb.setConstraints(m_cbDummyTraffic, lc);
		p22.add(m_cbDummyTraffic);
		lc.gridy++;
		m_sliderDummyTrafficIntervall = new JSlider(JSlider.HORIZONTAL, 10, 60, 30);
		m_sliderDummyTrafficIntervall.setMajorTickSpacing(10);
		m_sliderDummyTrafficIntervall.setMinorTickSpacing(5);
		m_sliderDummyTrafficIntervall.setPaintLabels(true);
		m_sliderDummyTrafficIntervall.setPaintTicks(true);
		m_sliderDummyTrafficIntervall.setSnapToTicks(true);
		gb.setConstraints(m_sliderDummyTrafficIntervall, lc);
		p22.add(m_sliderDummyTrafficIntervall);
		lc.gridy++;
		//gb.setConstraints(m_cbSaveWindowPositions, lc);
		//p22.add(m_cbSaveWindowPositions);
		lc.gridy++;
		gb.setConstraints(m_cbCertCheckDisabled, lc);
		p22.add(m_cbCertCheckDisabled);
		p2.add(p22, BorderLayout.NORTH);

		// Panel for Debugging Options
		JPanel p3 = new JPanel();
		p3.setLayout(new GridLayout(1, 2));
		p3.setBorder(new TitledBorder("Debugging"));
		JPanel p31 = new JPanel(new GridLayout(0, 1));
		m_cbDebugGui = new JCheckBox("GUI");
		m_cbDebugNet = new JCheckBox("NET");
		m_cbDebugThread = new JCheckBox("THREAD");
		m_cbDebugMisc = new JCheckBox("MISC");
		p31.add(m_cbDebugGui);
		p31.add(m_cbDebugNet);
		p31.add(m_cbDebugThread);
		p31.add(m_cbDebugMisc);

		m_cbShowDebugConsole = new JCheckBox("Show Console");
		m_cbShowDebugConsole.setSelected(JAPDebug.isShowConsole());
		m_cbShowDebugConsole.addItemListener(new ItemListener()
		{
			public void itemStateChanged(ItemEvent e)
			{
				JAPDebug.showConsole(e.getStateChange() == e.SELECTED, m_Controller.getView());
			}
		});
		p31.add(m_cbShowDebugConsole);

		p3.add(p31);
		JPanel p32 = new JPanel();
		m_sliderDebugLevel = new JSlider(JSlider.VERTICAL, 0, 7, 0);
		m_sliderDebugLevel.addChangeListener(new ChangeListener()
		{
			public void stateChanged(ChangeEvent e)
			{
				Dictionary d = m_sliderDebugLevel.getLabelTable();
				for (int i = 0; i < 8; i++)
				{
					( (JLabel) d.get(new Integer(i))).setEnabled(i <= m_sliderDebugLevel.getValue());
				}
			}
		});
		String debugLevels[] = LogLevel.STR_Levels;
		Hashtable ht = new Hashtable(debugLevels.length, 1.0f);
		for (int i = 0; i < debugLevels.length; i++)
		{
			ht.put(new Integer(i), new JLabel(" " + debugLevels[i]));
		}
		m_sliderDebugLevel.setLabelTable(ht);
		m_sliderDebugLevel.setPaintLabels(true);
		m_sliderDebugLevel.setMajorTickSpacing(1);
		m_sliderDebugLevel.setMinorTickSpacing(1);
		m_sliderDebugLevel.setSnapToTicks(true);
		m_sliderDebugLevel.setPaintTrack(true);
		m_sliderDebugLevel.setPaintTicks(false);

		p32.add(m_sliderDebugLevel);
		p3.add(p32);

		JPanel pp = new JPanel(new BorderLayout());
		//pp.add(p1, BorderLayout.NORTH);
		pp.add(p2, BorderLayout.CENTER);

		p.add(p3, BorderLayout.WEST);
		p.add(pp, BorderLayout.CENTER);

		return p;
	}

	protected void cancelPressed()
	{
		/* Call the event handler of all configuration modules. */
		Enumeration confModules = m_confModules.elements();
		while (confModules.hasMoreElements())
		{
			( (AbstractJAPConfModule) (confModules.nextElement())).cancelPressed();
		}
		setVisible(false);
	}

	/**Shows a Dialog about whats going wrong
	 */
	public static void showError(String msg)
	{
		JOptionPane.showMessageDialog(japConfInstance, msg, JAPMessages.getString("ERROR"),
									  JOptionPane.ERROR_MESSAGE);
	}

	/**Shows a Dialog with some info
	 */
	public static void showInfo(String msg)
	{
		JOptionPane.showMessageDialog(japConfInstance, msg, JAPMessages.getString("information"),
									  JOptionPane.INFORMATION_MESSAGE);
	}

	/** Checks if all Input in all Fiels make sense. Displays InfoBoxes about what is wrong.
	 * @return true if all is ok
	 *					false otherwise
	 */
	private boolean checkValues()
	{
		String s = null;
		int iListenerPort, i;

		//--------------
		//checking Listener Port Number
		try
		{
			i = Integer.parseInt(m_tfListenerPortNumber.getText().trim());
		}
		catch (Exception e)
		{
			i = -1;
		}
		if (!JAPUtil.isPort(i))
		{
			showError(JAPMessages.getString("errorListenerPortWrong"));
			return false;
		}
		iListenerPort = i;
		//checking Socks Port Number
		/*try
		   {
		 i = Integer.parseInt(m_tfListenerPortNumberSocks.getText().trim());
		   }
		   catch (Exception e)
		   {
		 i = -1;
		   }*/
		/*if (!JAPUtil.isPort(i))
		   {
		 showError(JAPMessages.getString("errorSocksListenerPortWrong"));
		 return false;
		   }*/
		/*		if (i == iListenerPort)
		  {
		   showError(JAPMessages.getString("errorListenerPortsAreEqual"));
		   return false;
		  }*/
		//Checking Firewall Settings (Host + Port)
		if (m_cbProxy.isSelected())
		{
			s = m_tfProxyHost.getText().trim();
			if (s == null || s.equals(""))
			{
				showError(JAPMessages.getString("errorFirewallHostNotNull"));
				return false;
			}
			try
			{
				i = Integer.parseInt(m_tfProxyPortNumber.getText().trim());
			}
			catch (Exception e)
			{
				i = -1;
			}
			if (!JAPUtil.isPort(i))
			{
				showError(JAPMessages.getString("errorFirewallServicePortWrong"));
				return false;
			}
			if (m_cbProxyAuthentication.isSelected())
			{
				s = m_tfProxyAuthenticationUserID.getText().trim();
				if (s == null || s.equals(""))
				{
					showError(JAPMessages.getString("errorFirewallAuthUserIDNotNull"));
					return false;
				}
			}
		}
		//checking Debug-Level
		/*		try
		   {
		 i=Integer.parseInt(debugLevelTextField.getText().trim());
		   }
		  catch(Exception e)
		   {
		 i=-1;
		   }
		  if(i<0||i>LogLevel.DEBUG)
		   {
		 showError(JAPMessages.getString("errorDebugLevelWrong"));
		 return false;
		   }
		 */

		return true;
	}

	/** Resets the Configuration to the Default values*/
	private void resetToDefault()
	{
		/* Call the event handler of all configuration modules. */
		Enumeration confModules = m_confModules.elements();
		while (confModules.hasMoreElements())
		{
			( (AbstractJAPConfModule) (confModules.nextElement())).resetToDefaultsPressed();
		}

		m_tfListenerPortNumber.setInt(JAPConstants.defaultPortNumber);
		//m_tfListenerPortNumberSocks.setInt(JAPConstants.defaultSOCKSPortNumber);

		m_cbProxy.setSelected(false);
		//m_cbListenerSocks.setSelected(false);
		m_cbShowDebugConsole.setSelected(false);
		m_sliderDebugLevel.setValue(LogLevel.EMERG);
		m_cbDebugNet.setSelected(false);
		m_cbDebugGui.setSelected(false);
		m_cbDebugMisc.setSelected(false);
		m_cbDebugThread.setSelected(false);
		m_cbDummyTraffic.setSelected(false);
		m_cbCertCheckDisabled.setSelected(false);

	}

	protected void okPressed()
	{
		if (!checkValues())
		{
			return;
		}
		/* Call the event handler of all configuration modules. */
		Enumeration confModules = m_confModules.elements();
		while (confModules.hasMoreElements())
		{
			AbstractJAPConfModule confModule=(AbstractJAPConfModule) (confModules.nextElement());
			if (! confModule.okPressed())
			{
				return;
			}
		}

		setVisible(false);
		// Misc settings
		JAPDebug.getInstance().setLogType(
			(m_cbDebugGui.isSelected() ? LogType.GUI : LogType.NUL) +
			(m_cbDebugNet.isSelected() ? LogType.NET : LogType.NUL) +
			(m_cbDebugThread.isSelected() ? LogType.THREAD : LogType.NUL) +
			(m_cbDebugMisc.isSelected() ? LogType.MISC : LogType.NUL)
			);
		JAPDebug.getInstance().setLogLevel(m_sliderDebugLevel.getValue());
		if (m_cbDummyTraffic.isSelected())
		{
			m_Controller.setDummyTraffic(m_sliderDummyTrafficIntervall.getValue() * 1000);
		}
		else
		{
			m_Controller.setDummyTraffic( -1);
			// Listener settings
		}
		m_Controller.setHTTPListener(m_tfListenerPortNumber.getInt(),
									 m_cbListenerIsLocal.isSelected(), true);
		//m_Controller.setSocksPortNumber(m_tfListenerPortNumberSocks.getInt());
		// Firewall settings
		int port = -1;
		try
		{
			port = Integer.parseInt(m_tfProxyPortNumber.getText().trim());
		}
		catch (Exception e)
		{}
		;
		int firewallType = JAPConstants.FIREWALL_TYPE_HTTP;
		if (m_comboProxyType.getSelectedIndex() == 1)
		{
			firewallType = JAPConstants.FIREWALL_TYPE_SOCKS;
		}
		m_Controller.setProxy(firewallType, m_tfProxyHost.getText().trim(), port, m_cbProxy.isSelected());
		m_Controller.setFirewallAuthUserID(m_tfProxyAuthenticationUserID.getText().trim());
		m_Controller.setUseFirewallAuthorization(m_cbProxyAuthentication.isSelected());
		//Cert seetings
		m_Controller.setCertCheckDisabled(m_cbCertCheckDisabled.isSelected());


		// force notifying the observers set the right server name
		m_Controller.notifyJAPObservers(); // this should be the last line of okPressed() !!!
		// ... manual settings stuff finished
	}

	/**
	 * Brings the specified card of the tabbed pane of the configuration window to the foreground.
	 * If there is no card with the specified symbolic name, nothing is done (current foreground
	 * card is not changed).
	 *
	 * @param a_selectedCard The card to bring to the foreground. See the TAB constants in this
	 *                       class.
	 */
	public void selectCard(String a_strSelectedCard)
	{
		/* try to get the specified card from the tab order table */
		//Integer cardIndex = (Integer) (m_tabOrder.get(new Integer(a_selectedCard)));
		//if (cardIndex != null)
		//{
			/* a card with the specified symoblic name is available in the hashtable */
		//	m_Tabs.setSelectedIndex(cardIndex.intValue());
		//}
	}

	public void localeChanged()
	{
		/* Call the repaintRootPanel() method of all configuration modules and update the tab titles. */
		Enumeration confModules = m_confModules.elements();
		while (confModules.hasMoreElements())
		{
			AbstractJAPConfModule currentModule = (AbstractJAPConfModule) (confModules.nextElement());
			try
			{
//				m_Tabs.setTitleAt(m_Tabs.indexOfComponent(currentModule.getRootPanel()),
//								  currentModule.getTabTitle());
			}
			catch (Exception e)
			{
			}
			currentModule.recreateRootPanel();
		}
		setTitle(JAPMessages.getString("settingsDialog"));
/*		m_Tabs.setTitleAt( ( (Integer) (m_tabOrder.get(new Integer(PORT_TAB)))).intValue(),
						  JAPMessages.getString("confListenerTab"));
		m_Tabs.setTitleAt( ( (Integer) (m_tabOrder.get(new Integer(PROXY_TAB)))).intValue(),
						  JAPMessages.getString("confProxyTab"));
		m_Tabs.setTitleAt( ( (Integer) (m_tabOrder.get(new Integer(ANON_TAB)))).intValue(),
						  JAPMessages.getString("confAnonTab"));
		if (!JAPModel.isSmallDisplay())
		{
			m_Tabs.setTitleAt( ( (Integer) (m_tabOrder.get(new Integer(MISC_TAB)))).intValue(),
							  JAPMessages.getString("confMiscTab"));
		}
	*/	m_bttnDefaultConfig.setText(JAPMessages.getString("bttnDefaultConfig"));
		m_bttnCancel.setText(JAPMessages.getString("cancelButton"));
		//Port Panel
		m_labelPortnumber1.setText(JAPMessages.getString("settingsPort1"));
		m_labelPortnumber2.setText(JAPMessages.getString("settingsPort2"));
		m_cbListenerIsLocal.setText(JAPMessages.getString("settingsListenerCheckBox"));
		m_borderSettingsListener.setTitle(JAPMessages.getString("settingsListenerBorder"));
		//m_labelSocksPortNumber.setText(JAPMessages.getString("settingsListenerSOCKS"));
		//ProxyPanel
		m_cbProxy.setText(JAPMessages.getString("settingsProxyCheckBox"));
		int i = m_comboProxyType.getSelectedIndex();
		m_comboProxyType.removeAllItems();
		m_comboProxyType.addItem(JAPMessages.getString("settingsProxyTypeHTTP"));
		m_comboProxyType.addItem(JAPMessages.getString("settingsProxyTypeSOCKS"));
		if (i != -1)
		{
			m_comboProxyType.setSelectedIndex(i);
		}
		m_cbProxyAuthentication.setText(JAPMessages.getString("settingsProxyAuthenticationCheckBox"));
		m_labelProxyHost.setText(JAPMessages.getString("settingsProxyHost"));
		m_labelProxyPort.setText(JAPMessages.getString("settingsProxyPort"));
		m_labelProxyType.setText(JAPMessages.getString("settingsProxyType"));
		m_labelProxyAuthUserID.setText(JAPMessages.getString("settingsProxyAuthUserID"));
		pack();
	/*	if(!m_Tree.isVisible())
		{
			Dimension d=getSize();
			setSize(d.width+10,d.height);
		}*/
	}

	/** Updates the shown Values from the Model.*/
	public void updateValues()
	{
		/* Call the event handler of all configuration modules. */
		Enumeration confModules = m_confModules.elements();
		while (confModules.hasMoreElements())
		{
			( (AbstractJAPConfModule) (confModules.nextElement())).updateValues();
		}

		/*		if (loadPay)
		  {
		   ( (pay.view.PayView) m_pKonto).userPanel.valuesChanged();
		  }*/
		// misc tab
		int iTmp = JAPModel.getDummyTraffic();
		m_cbDummyTraffic.setSelected(iTmp > -1);
		if (iTmp > -1)
		{
			m_sliderDummyTrafficIntervall.setValue(iTmp / 1000);
		}
		m_cbShowDebugConsole.setSelected(JAPDebug.isShowConsole());
		m_cbDebugGui.setSelected( ( ( (JAPDebug.getInstance().getLogType() & LogType.GUI) != 0) ? true : false));
		m_cbDebugNet.setSelected( ( ( (JAPDebug.getInstance().getLogType() & LogType.NET) != 0) ? true : false));
		m_cbDebugThread.setSelected( ( ( (JAPDebug.getInstance().getLogType() & LogType.THREAD) != 0) ? true : false));
		m_cbDebugMisc.setSelected( ( ( (JAPDebug.getInstance().getLogType() & LogType.MISC) != 0) ? true : false));
		m_sliderDebugLevel.setValue(JAPDebug.getInstance().getLogLevel());
		// listener tab
		m_tfListenerPortNumber.setInt(JAPModel.getHttpListenerPortNumber());
		m_cbListenerIsLocal.setSelected(JAPModel.getHttpListenerIsLocal());
		//m_tfListenerPortNumberSocks.setInt(JAPModel.getSocksListenerPortNumber());
		//boolean bSocksVisible = JAPModel.isTorEnabled();
		//m_tfListenerPortNumberSocks.setVisible(bSocksVisible);
		//m_labelSocksPortNumber.setVisible(bSocksVisible);
		//m_cbListenerSocks.setSelected(m_Controller.getUseSocksPort());
		// firewall tab
		m_cbProxy.setSelected(JAPModel.getUseFirewall());
		m_tfProxyHost.setEnabled(m_cbProxy.isSelected());
		m_tfProxyPortNumber.setEnabled(m_cbProxy.isSelected());
		m_comboProxyType.setEnabled(m_cbProxy.isSelected());
		if (JAPModel.getFirewallType() == JAPConstants.FIREWALL_TYPE_HTTP)
		{
			m_comboProxyType.setSelectedIndex(0);
		}
		else
		{
			m_comboProxyType.setSelectedIndex(1);
		}
		m_cbProxyAuthentication.setEnabled(m_cbProxy.isSelected());
		m_tfProxyHost.setText(JAPModel.getFirewallHost());
		m_tfProxyPortNumber.setText(String.valueOf(JAPModel.getFirewallPort()));
		m_tfProxyAuthenticationUserID.setText(JAPModel.getFirewallAuthUserID());
		m_cbProxyAuthentication.setSelected(JAPModel.getUseFirewallAuthorization());
		//cert tab
		m_cbCertCheckDisabled.setSelected(JAPModel.isCertCheckDisabled());
//		m_cbSaveWindowPositions.setSelected(JAPModel.getSaveMainWindowPosition());
	}

}
