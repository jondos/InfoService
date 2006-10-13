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
import java.io.IOException;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Observable;
import java.util.Observer;
import java.util.Vector;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
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
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JSlider;
import javax.swing.JTextField;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

import anon.infoservice.ImmutableListenerInterface;
import anon.infoservice.ListenerInterface;
import anon.infoservice.ProxyInterface;
import gui.JAPHelp;
import gui.JAPJIntField;
import gui.JAPMessages;
import gui.JAPMultilineLabel;
import gui.dialog.JAPDialog;
import jap.forward.JAPConfForwardingClient;
import jap.forward.JAPConfForwardingServer;
import jap.forward.JAPConfForwardingState;
import jap.pay.AccountSettingsPanel;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import gui.GUIUtils;
import java.awt.Dimension;

final public class JAPConf extends JAPDialog implements ActionListener, Observer
{

	/** Messages */
	public static final String MSG_READ_PANEL_HELP = JAPConf.class.getName() + "_readPanelHelp";
	private static final String MSG_DETAILLEVEL = JAPConf.class.getName() + "_detaillevel";
	private static final String MSG_BTN_SAVE = JAPConf.class.getName() + "_btnSave";
	private static final String MSG_ASK_RESET_DEFAULTS = JAPConf.class.getName() + "_askResetDefaults";
	private static final String MSG_NEED_RESTART = JAPConf.class.getName() + "_needRestart";
	private static final String MSG_LISTENER_CHANGED = JAPConf.class.getName() + "_listenerChanged";
	private static final String MSG_ACCESS_TO_JAP = JAPConf.class.getName() + "_accessToJAP";

	final static public String PORT_TAB = "PORT_TAB";
	final static public String UI_TAB = "UI_TAB";
	final static public String UPDATE_TAB = "UPDATE_TAB";
	final static public String PROXY_TAB = "PROXY_TAB";
	final static public String INFOSERVICE_TAB = "INFOSERVICE_TAB";
	final static public String ANON_TAB = "ANON_TAB";
	final static public String ANON_SERVICES_TAB = "SERVICES_TAB";
	final static public String CERT_TAB = "CERT_TAB";
	final static public String TOR_TAB = "TOR_TAB";
	final static public String DEBUG_TAB = "DEBUG_TAB";
	final static public String PAYMENT_TAB = "PAYMENT_TAB";

	/**
	 * This constant is a symbolic name for accessing the forwarding client configuration tab.
	 */
	final static public String FORWARDING_CLIENT_TAB = "FORWARDING_CLIENT_TAB";

	/**
	 * This constant is a symbolic name for accessing the forwarding server configuration tab.
	 */
	final static public String FORWARDING_SERVER_TAB = "FORWARDING_SERVER_TAB";

	/**
	 * This constant is a symbolic name for accessing the forwarding state tab.
	 */
	final static public String FORWARDING_STATE_TAB = "FORWARDING_STATE_TAB";

	private static JAPConf ms_JapConfInstance = null;

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

	private JCheckBox[] m_cbLogTypes;
	private JCheckBox m_cbShowDebugConsole, m_cbDebugToFile;
	private JTextField m_tfDebugFileName;
	private JButton m_bttnDebugFileNameSearch;
	private JAPMultilineLabel m_labelConfDebugLevel, m_labelConfDebugTypes;

	private JSlider m_sliderDebugLevel;
	private JSlider m_sliderDebugDetailLevel;

	private JPanel m_pPort, m_pFirewall, m_pMisc;
	private JButton m_bttnDefaultConfig, m_bttnCancel, m_bttnHelp;

	//private Font m_fontControls;

	private boolean m_bWithPayment = false;
	private boolean m_bIsSimpleView;
	private Vector m_vecConfigChangesNeedRestart = new Vector();

	private JAPConfModuleSystem m_moduleSystem;
	private JAPConfServices m_confServices;
	private AbstractJAPMainView m_parentView;

	public static JAPConf getInstance()
	{
		return ms_JapConfInstance;
	}

	public JAPConf(AbstractJAPMainView frmParent, boolean loadPay)
	{
		super(frmParent, JAPMessages.getString("settingsDialog"), true);
		m_parentView = frmParent;
		setDefaultCloseOperation(HIDE_ON_CLOSE);
		m_bWithPayment = loadPay;
		m_bIsSimpleView = (JAPModel.getDefaultView() == JAPConstants.VIEW_SIMPLIFIED);
		/* set the instance pointer */
		ms_JapConfInstance = this;
		m_Controller = JAPController.getInstance();
		JPanel pContainer = new JPanel();
		GridBagLayout gbl = new GridBagLayout();
		pContainer.setLayout(gbl);
		m_pPort = buildPortPanel();
		m_pFirewall = buildProxyPanel();
		m_pMisc = buildMiscPanel();

		m_moduleSystem = new JAPConfModuleSystem();
		DefaultMutableTreeNode rootNode = m_moduleSystem.getConfigurationTreeRootNode();
		JAPConfUI confUI = new JAPConfUI();
		m_moduleSystem.addConfigurationModule(rootNode, confUI, UI_TAB);
		if (m_bWithPayment)
		{
			m_moduleSystem.addConfigurationModule(rootNode, new AccountSettingsPanel(), PAYMENT_TAB);
		}
		if (!m_bIsSimpleView)
		{
			m_moduleSystem.addConfigurationModule(rootNode, new JAPConfUpdate(), UPDATE_TAB);
		}

		DefaultMutableTreeNode nodeNet = m_moduleSystem.addComponent(rootNode, null, "ngTreeNetwork", null,
			null);
		m_moduleSystem.addComponent(nodeNet, m_pPort, "confListenerTab", PORT_TAB, "portlistener");
		m_moduleSystem.addComponent(nodeNet, m_pFirewall, "confProxyTab", PROXY_TAB, "proxy");
		m_moduleSystem.addConfigurationModule(nodeNet, new JAPConfForwardingClient() ,
											  FORWARDING_CLIENT_TAB);


		m_confServices = new JAPConfServices();
		if (!m_bIsSimpleView)
		{
			DefaultMutableTreeNode nodeAnon =
				m_moduleSystem.addComponent(rootNode, null, "ngAnonymitaet", null, null);
			m_moduleSystem.addConfigurationModule(nodeAnon, new JAPConfInfoService(), INFOSERVICE_TAB);
			m_moduleSystem.addConfigurationModule(nodeAnon, m_confServices, ANON_SERVICES_TAB);
			m_moduleSystem.addConfigurationModule(nodeAnon, new JAPConfForwardingServer(),
												  FORWARDING_SERVER_TAB);
			m_moduleSystem.addConfigurationModule(nodeAnon, new JAPConfCert(), CERT_TAB);
			DefaultMutableTreeNode debugNode =
				m_moduleSystem.addComponent(rootNode, m_pMisc, "ngTreeDebugging", DEBUG_TAB, "debugging");
			if (JAPModel.getInstance().isForwardingStateModuleVisible())
			{
				m_moduleSystem.addConfigurationModule(debugNode, new JAPConfForwardingState(),
					FORWARDING_STATE_TAB);
			}
			m_moduleSystem.getConfigurationTree().expandPath(new TreePath(nodeAnon.getPath()));
		}
		else
		{
			m_moduleSystem.addConfigurationModule(rootNode, m_confServices, ANON_SERVICES_TAB);
		}
		m_moduleSystem.getConfigurationTree().expandPath(new TreePath(nodeNet.getPath()));

		m_moduleSystem.getConfigurationTree().setSelectionRow(0);
		/* after finishing building the tree, it is important to update the tree size */
		m_moduleSystem.getConfigurationTree().setMinimumSize(m_moduleSystem.getConfigurationTree().
			getPreferredSize());

		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
		m_bttnHelp = new JButton(JAPMessages.getString("updateM_bttnHelp"));
		buttonPanel.add(m_bttnHelp);
		m_bttnHelp.addActionListener(this);

		m_bttnDefaultConfig = new JButton(JAPMessages.getString("bttnDefaultConfig"));
		final JAPDialog view = this;
		m_bttnDefaultConfig.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				if (JAPDialog.showConfirmDialog(view, JAPMessages.getString(MSG_ASK_RESET_DEFAULTS),
												JAPDialog.OPTION_TYPE_OK_CANCEL,
												JAPDialog.MESSAGE_TYPE_WARNING)
					== JAPDialog.RETURN_VALUE_OK)
				{
					resetToDefault();
				}
			}
		});
		if (!JAPModel.isSmallDisplay())
		{
			buttonPanel.add(m_bttnDefaultConfig);
		}
		m_bttnCancel = new JButton(JAPMessages.getString("cancelButton"));
		m_bttnCancel.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				cancelPressed();
			}
		});
		buttonPanel.add(m_bttnCancel);
		JButton bttnSave = new JButton(JAPMessages.getString(MSG_BTN_SAVE));
		bttnSave.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				okPressed(false);
			}
		});

		buttonPanel.add(bttnSave);

		JButton ok = new JButton(JAPMessages.getString("okButton"));
		ok.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				okPressed(true);
			}
		});
		//ok.setFont(m_fontControls);
		buttonPanel.add(ok);
		buttonPanel.add(new JLabel("   "));
		getRootPane().setDefaultButton(ok);

		JPanel moduleSystemPanel = m_moduleSystem.getRootPanel();

		GridBagLayout configPanelLayout = new GridBagLayout();
		pContainer.setLayout(configPanelLayout);

		GridBagConstraints configPanelConstraints = new GridBagConstraints();
		configPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
		configPanelConstraints.fill = GridBagConstraints.BOTH;
		configPanelConstraints.weightx = 1.0;
		configPanelConstraints.weighty = 1.0;
		configPanelConstraints.gridx = 0;
		configPanelConstraints.gridy = 0;
		configPanelLayout.setConstraints(moduleSystemPanel, configPanelConstraints);
		pContainer.add(moduleSystemPanel);

		configPanelConstraints.weighty = 0.0;
		configPanelConstraints.gridx = 0;
		configPanelConstraints.gridy = 1;
		configPanelConstraints.insets = new Insets(10, 10, 10, 10);
		configPanelLayout.setConstraints(buttonPanel, configPanelConstraints);
		pContainer.add(buttonPanel);

		setContentPane(pContainer);
		updateValues();
		// largest tab to front
		if (JAPModel.isSmallDisplay())
		{
			setSize(240, 300);
			setLocation(0, 0);
		}
		else
		{
			for (int i = 0; i < 2; i++)
			{
				pack();
				if (i != 1 && getSize().width < getSize().height)
				{
					LogHolder.log(LogLevel.ERR, LogType.GUI,
								  "Could not pack config properly. Width is smaller than height! " +
								  "Width:" + getSize().width + " Height:" + getSize().height);
					Thread.yield();
					continue;
				}
				else if (getSize().width > getScreenBounds().width ||
						 getSize().height > getScreenBounds().height)
				{
					LogHolder.log(LogLevel.ERR, LogType.GUI, "Packed config view with illegal size! " +
								  "Width:" + getSize().width + " Height:" + getSize().height +
								  "\nSetting defaults...");

					if (JAPModel.getInstance().getConfigSize() != null &&
						JAPModel.getInstance().getConfigSize().width > 0 &&
						JAPModel.getInstance().getConfigSize().height > 0)
					{
						setSize(JAPModel.getInstance().getConfigSize());
					}
					else
					{
						// default size for MacOS
						setSize(new Dimension(786, 545));
					}
				}
				else
				{
					JAPModel.getInstance().setConfigSize(getSize());
				}
				break;
			}
		}
		confUI.afterPack();
		restoreLocation(JAPModel.getInstance().getConfigWindowLocation());
		//setDockable(true);
		JAPModel.getInstance().addObserver(this);
	}

	public static abstract class AbstractRestartNeedingConfigChange
	{
		public abstract String getName();
		public abstract void doChange();
		public String getMessage()
		{
			return "";
		}
	}

	/**
	 * This method to show the Dialog We need it for
	 * creating the module savepoints. After this, we call the parent setVisible(true) method.
	 */
	public void setVisible(boolean a_bVisible)
	{
		/* every time the configuration is set to visible, we need to create the savepoints for the
		 * modules for the case that 'Cancel' is pressed later
		 */
		if (a_bVisible)
		{
			m_parentView.getViewIconified().switchBackToMainView();
			m_moduleSystem.createSavePoints();
		}
		/* call the original method */
		super.setVisible(a_bVisible);
	}

	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == m_bttnHelp)
		{
			JAPHelp.getInstance().getContextObj().setContext(m_moduleSystem);
			JAPHelp.getInstance().loadCurrentContext();
		}
		else if (e.getSource() == m_cbProxyAuthentication)
		{
			if (m_cbProxyAuthentication.isSelected())
			{
				JAPModel.getInstance().setUseProxyAuthentication(true);
			}
			else
			{
				JAPModel.getInstance().setUseProxyAuthentication(false);
			}
		}
	}

	JPanel buildPortPanel()
	{
		m_labelPortnumber1 = new JLabel(JAPMessages.getString("settingsPort1"));
		//m_labelPortnumber1.setFont(m_fontControls);
		m_labelPortnumber2 = new JLabel(JAPMessages.getString("settingsPort2"));
		//m_labelPortnumber2.setFont(m_fontControls);
		m_tfListenerPortNumber = new JAPJIntField(ListenerInterface.PORT_MAX_VALUE);
		//m_tfListenerPortNumber.setFont(m_fontControls);
		m_tfListenerPortNumber.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				okPressed(false);
			}
		});
		m_cbListenerIsLocal = new JCheckBox(JAPMessages.getString("settingsListenerCheckBox"));
		//m_cbListenerIsLocal.setFont(m_fontControls);
		// set Font in listenerCheckBox in same color as in portnumberLabel1
		m_cbListenerIsLocal.setForeground(m_labelPortnumber1.getForeground());

		//m_tfListenerPortNumberSocks.setEnabled(false);
		//}
		//m_tfListenerPortNumberSocks = new JAPJIntField();
		//m_tfListenerPortNumberSocks.setFont(m_fontControls);

		JPanel p = new JPanel();
		p.setLayout(new BorderLayout());
		m_borderSettingsListener = new TitledBorder(JAPMessages.getString("settingsListenerBorder"));
		//m_borderSettingsListener.setTitleFont(m_fontControls);
		p.setBorder(m_borderSettingsListener);
		JPanel p1 = new JPanel();
		GridBagLayout g = new GridBagLayout();
		p1.setLayout(g);
		p1.setBorder(new EmptyBorder(5, 10, 10, 10));
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.NORTHWEST;
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
		c.fill = c.NONE;
		g.setConstraints(m_tfListenerPortNumber, c);
		p1.add(m_tfListenerPortNumber);
		c.fill = c.HORIZONTAL;
		JSeparator seperator = new JSeparator();
		c.gridy = 3;
		c.insets = new Insets(10, 0, 0, 0);
		g.setConstraints(seperator, c);
		p1.add(seperator);
		c.insets = normInsets;
		c.gridy = 4;
		c.insets = new Insets(10, 0, 0, 0);

		c.gridy = 7;
		c.insets = new Insets(10, 0, 0, 0);
		g.setConstraints(m_cbListenerIsLocal, c);
		p1.add(m_cbListenerIsLocal);
		p.add(p1, BorderLayout.NORTH);

		return p;
	}

	JPanel buildProxyPanel()
	{
		m_cbProxy = new JCheckBox(JAPMessages.getString("settingsProxyCheckBox"));
		//m_cbProxy.setFont(m_fontControls);
		m_comboProxyType = new JComboBox();
		//m_comboProxyType.setFont(m_fontControls);
		m_comboProxyType.addItem(JAPMessages.getString("settingsProxyTypeHTTP"));
		m_comboProxyType.addItem(JAPMessages.getString("settingsProxyTypeSOCKS"));
		m_tfProxyHost = new JTextField(20);
		//m_tfProxyHost.setFont(m_fontControls);
		m_tfProxyPortNumber = new JAPJIntField(ListenerInterface.PORT_MAX_VALUE);
		//m_tfProxyPortNumber.setFont(m_fontControls);
		ProxyInterface proxyInterface = JAPModel.getInstance().getProxyInterface();
		boolean bUseProxy = (proxyInterface != null && proxyInterface.isValid());
		m_tfProxyHost.setEnabled(bUseProxy);
		m_tfProxyPortNumber.setEnabled(bUseProxy);
		m_cbProxy.addItemListener(new ItemListener()
		{
			public void itemStateChanged(ItemEvent e)
			{
				boolean b = m_cbProxy.isSelected();
				m_comboProxyType.setEnabled(b);
				m_tfProxyHost.setEnabled(b);
				m_tfProxyPortNumber.setEnabled(b);
				m_cbProxyAuthentication.setEnabled(b);
				m_labelProxyHost.setEnabled(b);
				m_labelProxyPort.setEnabled(b);
				m_labelProxyType.setEnabled(b);
				m_labelProxyAuthUserID.setEnabled(m_cbProxyAuthentication.isSelected() & b);
				m_tfProxyAuthenticationUserID.setEnabled(m_cbProxyAuthentication.isSelected() & b);
			}

		});
		m_tfProxyHost.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				okPressed(false);
			}
		});
		m_tfProxyPortNumber.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				okPressed(false);
			}
		});
		m_cbProxyAuthentication = new JCheckBox(JAPMessages.getString("settingsProxyAuthenticationCheckBox"));
		//m_cbProxyAuthentication.setFont(m_fontControls);
		m_tfProxyAuthenticationUserID = new JTextField(10);
		//m_tfProxyAuthenticationUserID.setFont(m_fontControls);
		m_cbProxyAuthentication.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				m_tfProxyAuthenticationUserID.setEnabled(m_cbProxyAuthentication.isSelected());
				m_labelProxyAuthUserID.setEnabled(m_cbProxyAuthentication.isSelected());
			}
		});
		m_labelProxyHost = new JLabel(JAPMessages.getString("settingsProxyHost"));
		//m_labelProxyHost.setFont(m_fontControls);
		m_labelProxyPort = new JLabel(JAPMessages.getString("settingsProxyPort"));
		//m_labelProxyPort.setFont(m_fontControls);
		m_labelProxyType = new JLabel(JAPMessages.getString("settingsProxyType"));
		//m_labelProxyType.setFont(m_fontControls);
		m_labelProxyAuthUserID = new JLabel(JAPMessages.getString("settingsProxyAuthUserID"));
		//m_labelProxyAuthUserID.setFont(m_fontControls);
		// set Font in m_cbProxy in same color as in proxyPortLabel
		m_cbProxy.setForeground(m_labelProxyPort.getForeground());
		m_cbProxyAuthentication.setForeground(m_labelProxyPort.getForeground());

		JPanel p = new JPanel();
		p.setLayout(new BorderLayout());
		TitledBorder border = new TitledBorder(JAPMessages.getString("settingsProxyBorder"));
		//border.setTitleFont(m_fontControls);
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
		c.anchor = GridBagConstraints.NORTHWEST;
		c.fill = GridBagConstraints.NONE;
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

	JPanel buildMiscPanel()
	{
		JPanel p = new JPanel(new GridBagLayout());
		p.setBorder(new TitledBorder("Debugging"));
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.NORTHWEST;
		c.insets = new Insets(5, 5, 5, 5);
		JPanel panelLogTypes = new JPanel(new GridLayout(0, 1));
		m_cbLogTypes = new JCheckBox[LogType.getNumberOfLogTypes()];
		int[] availableLogTypes = LogType.getAvailableLogTypes();
		for (int i = 0; i < m_cbLogTypes.length; i++)
		{
			m_cbLogTypes[i] = new JCheckBox(LogType.getLogTypeName(availableLogTypes[i]));
			if (i > 0)
			{
				panelLogTypes.add(m_cbLogTypes[i]);
			}
		}

		m_labelConfDebugTypes = new JAPMultilineLabel(JAPMessages.getString("ConfDebugTypes"));
		p.add(m_labelConfDebugTypes, c);
		c.gridy = 1;
		p.add(panelLogTypes, c);
		c.gridy = 2;
		c.gridwidth = 5;
		c.weightx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(0, 5, 0, 5);
		p.add(new JSeparator(), c);
		m_cbShowDebugConsole = new JCheckBox(JAPMessages.getString("ConfDebugShowConsole"));
		m_cbShowDebugConsole.setSelected(JAPDebug.isShowConsole());
		m_cbShowDebugConsole.addItemListener(new ItemListener()
		{
			public void itemStateChanged(ItemEvent e)
			{
				JAPDebug.showConsole(e.getStateChange() == ItemEvent.SELECTED,
									 JAPController.getInstance().getViewWindow());
			}
		});
		c.gridy = 3;
		c.weighty = 0;
		c.insets = new Insets(5, 5, 5, 5);
		p.add(m_cbShowDebugConsole, c);

		m_cbDebugToFile = new JCheckBox(JAPMessages.getString("ConfDebugFile"));
		m_cbDebugToFile.addItemListener(new ItemListener()
		{
			public void itemStateChanged(ItemEvent e)
			{
				boolean b = m_cbDebugToFile.isSelected();
				m_bttnDebugFileNameSearch.setEnabled(b);
				m_tfDebugFileName.setEnabled(b);
			}
		});

		c.gridy = 4;
		c.weighty = 0;
		p.add(m_cbDebugToFile, c);
		JPanel panelDebugFileName = new JPanel(new GridBagLayout());
		GridBagConstraints c1 = new GridBagConstraints();
		m_tfDebugFileName = new JTextField(20);
		c1.weightx = 1;
		c1.insets = new Insets(0, 5, 0, 5);
		c1.fill = GridBagConstraints.HORIZONTAL;
		panelDebugFileName.add(m_tfDebugFileName, c1);
		m_bttnDebugFileNameSearch = new JButton(JAPMessages.getString("ConfDebugFileNameSearch"));
		m_bttnDebugFileNameSearch.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				JFileChooser fileChooser = new JFileChooser();
				String strCurrentFile = ms_JapConfInstance.m_tfDebugFileName.getText().trim();
				if (!strCurrentFile.equals(""))
				{
					try
					{
						File f = new File(strCurrentFile);
						fileChooser.setCurrentDirectory(new File(f.getParent()));
					}
					catch (Exception e1)
					{
					}
				}
				int ret = fileChooser.showOpenDialog(ms_JapConfInstance.getContentPane());
				if (ret == JFileChooser.APPROVE_OPTION)
				{
					try
					{
						ms_JapConfInstance.m_tfDebugFileName.setText(fileChooser.getSelectedFile().
							getCanonicalPath());
					}
					catch (IOException ex)
					{
					}
				}
			}
		});
		c1.gridx = 1;
		c1.weightx = 0;
		panelDebugFileName.add(m_bttnDebugFileNameSearch, c1);
		c.gridy = 5;
		c.weighty = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		p.add(panelDebugFileName, c);

		final String debugLevels[] = LogLevel.STR_Levels;
		JPanel panelDebugLevels = new JPanel();
		m_sliderDebugLevel = new JSlider(SwingConstants.VERTICAL, 0, 7, 0);
		m_sliderDebugLevel.addChangeListener(new ChangeListener()
		{
			public void stateChanged(ChangeEvent e)
			{
				Dictionary d = m_sliderDebugLevel.getLabelTable();
				for (int i = 0; i < debugLevels.length; i++)
				{
					( (JLabel) d.get(new Integer(i))).setEnabled(i <= m_sliderDebugLevel.getValue());
				}
			}
		});

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
		panelDebugLevels.add(m_sliderDebugLevel);

		c.gridheight = 2;
		c.gridwidth = 1;
		c.insets = new Insets(0, 10, 0, 10);
		c.gridx = 1;
		c.gridy = 0;
		c.weightx = 0;
		c.weighty = 0;
		c.fill = GridBagConstraints.VERTICAL;
		p.add(new JSeparator(SwingConstants.VERTICAL), c);
		c.gridheight = 1;
		c.gridwidth = 1;
		c.gridx = 2;
		c.gridy = 0;
		c.fill = GridBagConstraints.NONE;
		c.insets = new Insets(5, 5, 5, 5);
		m_labelConfDebugLevel = new JAPMultilineLabel(JAPMessages.getString("ConfDebugLevels"));
		p.add(m_labelConfDebugLevel, c);
		c.gridy = 1;
		c.weightx = 1;
		p.add(panelDebugLevels, c);

		JPanel panelDebugDetailLevel = new JPanel();
		m_sliderDebugDetailLevel = new JSlider(SwingConstants.VERTICAL, LogHolder.DETAIL_LEVEL_LOWEST,
											   LogHolder.DETAIL_LEVEL_HIGHEST, LogHolder.getDetailLevel());
		m_sliderDebugDetailLevel.addChangeListener(new ChangeListener()
		{
			public void stateChanged(ChangeEvent e)
			{
				LogHolder.setDetailLevel(m_sliderDebugDetailLevel.getValue());
			}
		});
		m_sliderDebugDetailLevel.setPaintTicks(false);
		m_sliderDebugDetailLevel.setPaintLabels(true);
		m_sliderDebugDetailLevel.setMajorTickSpacing(1);
		m_sliderDebugDetailLevel.setMinorTickSpacing(1);
		m_sliderDebugDetailLevel.setSnapToTicks(true);
		m_sliderDebugDetailLevel.setPaintTrack(true);

		panelDebugDetailLevel.add(m_sliderDebugDetailLevel);

		c.gridheight = 2;
		c.gridwidth = 1;
		c.insets = new Insets(0, 10, 0, 10);
		c.gridx = 3;
		c.gridy = 0;
		c.weightx = 0;
		c.weighty = 0;
		c.fill = GridBagConstraints.VERTICAL;
		p.add(new JSeparator(SwingConstants.VERTICAL), c);

		c.gridheight = 1;
		c.gridwidth = 1;
		c.gridx = 4;
		c.gridy = 0;
		c.fill = GridBagConstraints.NONE;
		c.insets = new Insets(5, 5, 5, 5);
		m_labelConfDebugLevel = new JAPMultilineLabel(JAPMessages.getString(MSG_DETAILLEVEL));
		p.add(m_labelConfDebugLevel, c);

		c.gridy = 1;
		c.weightx = 1;
		p.add(panelDebugDetailLevel, c);

		return p;
	}

	void cancelPressed()
	{
		m_vecConfigChangesNeedRestart.removeAllElements();
		m_moduleSystem.processCancelPressedEvent();
		setVisible(false);
	}

	/** Checks if all Input in all Fiels make sense. Displays InfoBoxes about what is wrong.
	 * @return true if all is ok
	 *					false otherwise
	 */
	private boolean checkValues()
	{
		String s = null;
		int i;

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
		if (!ProxyInterface.isValidPort(i))
		{
			JAPDialog.showErrorDialog(ms_JapConfInstance, JAPMessages.getString("errorListenerPortWrong"),
									  LogType.MISC);
			return false;
		}

		//Checking Firewall Settings (Host + Port)
		if (m_cbProxy.isSelected())
		{
			s = m_tfProxyHost.getText().trim();
			if (s == null || s.equals(""))
			{
				JAPDialog.showErrorDialog(ms_JapConfInstance,
										  JAPMessages.getString("errorFirewallHostNotNull"), LogType.MISC);
				this.selectCard(PROXY_TAB);
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
			if (!ProxyInterface.isValidPort(i))
			{
				JAPDialog.showErrorDialog(ms_JapConfInstance,
										  JAPMessages.getString("errorFirewallServicePortWrong"),
										  LogType.MISC);
				this.selectCard(PROXY_TAB);
				return false;
			}
			if (m_cbProxyAuthentication.isSelected())
			{
				s = m_tfProxyAuthenticationUserID.getText().trim();
				if (s == null || s.equals(""))
				{
					JAPDialog.showErrorDialog(ms_JapConfInstance,
											  JAPMessages.getString("errorFirewallAuthUserIDNotNull"),
											  LogType.MISC);
					this.selectCard(PROXY_TAB);
					return false;
				}
			}
		}

		return true;
	}

	/** Resets the Configuration to the Default values*/
	private void resetToDefault()
	{
		m_vecConfigChangesNeedRestart.removeAllElements();
		m_moduleSystem.processResetToDefaultsPressedEvent();
		m_tfListenerPortNumber.setInt(JAPConstants.DEFAULT_PORT_NUMBER);
		m_cbListenerIsLocal.setSelected(JAPConstants.DEFAULT_LISTENER_IS_LOCAL);

		m_cbProxy.setSelected(false);
		m_cbShowDebugConsole.setSelected(false);
		m_sliderDebugLevel.setValue(LogLevel.WARNING);
		for (int i = 0; i < m_cbLogTypes.length; i++)
		{
			m_cbLogTypes[i].setSelected(true);
		}
		m_sliderDebugDetailLevel.setValue(0);
		m_cbDebugToFile.setSelected(false);
	}

	private void onOkPressed()
	{
		// Misc settings
		int[] availableLogTypes = LogType.getAvailableLogTypes();
		int logType = LogType.NUL;
		for (int i = 0; i < m_cbLogTypes.length; i++)
		{
			logType |= (m_cbLogTypes[i].isSelected() ? availableLogTypes[i] : LogType.NUL);
		}

		JAPDebug.getInstance().setLogType(logType);
		JAPDebug.getInstance().setLogLevel(m_sliderDebugLevel.getValue());
		String strFilename = m_tfDebugFileName.getText().trim();
		if (!m_cbDebugToFile.isSelected())
		{
			strFilename = null;
		}
		JAPDebug.setLogToFile(strFilename);

		if (JAPModel.getHttpListenerPortNumber() != m_tfListenerPortNumber.getInt())
		{
			addNeedRestart(new AbstractRestartNeedingConfigChange()
			{
				public String getName()
				{
					return JAPMessages.getString("Portlistener");
				}

				public String getMessage()
				{
					return JAPMessages.getString(MSG_LISTENER_CHANGED);
				}

				public void doChange()
				{
					JAPModel.getInstance().setHttpListenerPortNumber(m_tfListenerPortNumber.getInt());
				}
			});
		}
		if (JAPModel.isHttpListenerLocal() != m_cbListenerIsLocal.isSelected())
		{
			addNeedRestart(new AbstractRestartNeedingConfigChange()
			{
				public String getName()
				{
					return JAPMessages.getString(JAPMessages.getString(MSG_ACCESS_TO_JAP));
				}

				public void doChange()
				{
					JAPModel.getInstance().setHttpListenerIsLocal(m_cbListenerIsLocal.isSelected());
				}
			});
		}

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
		int firewallType = ImmutableListenerInterface.PROTOCOL_TYPE_HTTP;
		if (m_comboProxyType.getSelectedIndex() == 1)
		{
			firewallType = ImmutableListenerInterface.PROTOCOL_TYPE_SOCKS;
		}
		m_Controller.changeProxyInterface(
			new ProxyInterface(m_tfProxyHost.getText().trim(),
							   port,
							   firewallType,
							   m_tfProxyAuthenticationUserID.getText().trim(),
							   m_Controller.getPasswordReader(),
							   m_cbProxyAuthentication.isSelected(),
							   m_cbProxy.isSelected()),
			m_cbProxyAuthentication.isSelected(), getContentPane());
	}

	private void okPressed(final boolean a_bCloseConfiguration)
	{
		if (!checkValues())
		{
			return;
		}
		m_vecConfigChangesNeedRestart.removeAllElements();
		if (m_moduleSystem.processOkPressedEvent() == false)
		{
			m_vecConfigChangesNeedRestart.removeAllElements();
			return;
		}
		onOkPressed();
		resetAutomaticLocation(JAPModel.getInstance().isConfigWindowLocationSaved());

		if (m_vecConfigChangesNeedRestart.size() > 0)
		{
			String strChanges = "<ul>";
			AbstractRestartNeedingConfigChange change;
			for (int i = 0; i < m_vecConfigChangesNeedRestart.size(); i++)
			{
				change = (AbstractRestartNeedingConfigChange)m_vecConfigChangesNeedRestart.elementAt(i);
				strChanges += "<li>" + change.getName();
				if (change.getMessage() != null && change.getMessage().trim().length() > 0)
				{
					strChanges += "<br>" + change.getMessage();
				}
				strChanges += "</li>";

			}
			strChanges += "</ul>";

			if (JAPDialog.showYesNoDialog(this, JAPMessages.getString(MSG_NEED_RESTART, strChanges)))
			{
				for (int i = 0; i < m_vecConfigChangesNeedRestart.size(); i++)
				{
					((AbstractRestartNeedingConfigChange)m_vecConfigChangesNeedRestart.elementAt(i)).doChange();
				}
			}
			else
			{
				m_vecConfigChangesNeedRestart.removeAllElements();
				return;
			}
		}


		// We are in event dispatch thread!!
		Thread run = new Thread(new Runnable()
		{
			public void run()
			{
				// save configuration
				m_Controller.saveConfigFile();

				if (a_bCloseConfiguration && !isRestartNeeded())
				{
					setVisible(false);
				}

				// force notifying the observers set the right server name
				//m_Controller.notifyJAPObservers();

				if (isRestartNeeded())
				{
					JAPController.goodBye(false);
				}
			}
		});
		run.setDaemon(true);
		run.start();

		// ... manual settings stuff finished
	}

	public void update(Observable a_observable, final Object a_message)
	{
		if (a_message instanceof JAPModel.FontResize)
		{
			// font changed
			SwingUtilities.updateComponentTreeUI(getContentPane());
		}
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
		if (a_strSelectedCard.equals(JAPConf.ANON_TAB))
		{
			m_moduleSystem.selectNode(JAPConf.ANON_SERVICES_TAB);
			m_confServices.selectAnonTab();
		}
		else
		{
			m_moduleSystem.selectNode(a_strSelectedCard);
		}
	}

	/** Updates the shown Values from the Model.*/
	public void updateValues()
	{
		m_moduleSystem.processUpdateValuesEvent();
		/*		if (loadPay)
		  {
		   ( (pay.view.PayView) m_pKonto).userPanel.valuesChanged();
		  }*/
		// misc tab
		m_cbShowDebugConsole.setSelected(JAPDebug.isShowConsole());
		int[] availableLogTypes = LogType.getAvailableLogTypes();
		for (int i = 0; i < m_cbLogTypes.length; i++)
		{
			m_cbLogTypes[i].setSelected(
				( ( (JAPDebug.getInstance().getLogType() & availableLogTypes[i]) != 0) ?
				 true : false));
		}
		m_sliderDebugLevel.setValue(JAPDebug.getInstance().getLogLevel());
		m_sliderDebugDetailLevel.setValue(LogHolder.getDetailLevel());
		boolean b = JAPDebug.isLogToFile();
		m_tfDebugFileName.setEnabled(b);
		m_bttnDebugFileNameSearch.setEnabled(b);
		m_cbDebugToFile.setSelected(b);
		if (b)
		{
			m_tfDebugFileName.setText(JAPDebug.getLogFilename());
		}
		// listener tab
		m_tfListenerPortNumber.setInt(JAPModel.getHttpListenerPortNumber());
		m_cbListenerIsLocal.setSelected(JAPModel.isHttpListenerLocal());
		//m_tfListenerPortNumberSocks.setInt(JAPModel.getSocksListenerPortNumber());
		//boolean bSocksVisible = JAPModel.isTorEnabled();
		//m_tfListenerPortNumberSocks.setVisible(bSocksVisible);
		//m_labelSocksPortNumber.setVisible(bSocksVisible);
		//m_cbListenerSocks.setSelected(m_Controller.getUseSocksPort());
		// firewall tab
		ProxyInterface proxyInterface = JAPModel.getInstance().getProxyInterface();
		boolean bEnableProxy = proxyInterface != null &&
			proxyInterface.isValid();
		m_cbProxy.setSelected(bEnableProxy);
		m_tfProxyHost.setEnabled(bEnableProxy);
		m_tfProxyPortNumber.setEnabled(bEnableProxy);
		m_comboProxyType.setEnabled(bEnableProxy);
		m_tfProxyAuthenticationUserID.setEnabled(bEnableProxy);
		m_labelProxyHost.setEnabled(bEnableProxy);
		m_labelProxyPort.setEnabled(bEnableProxy);
		m_labelProxyType.setEnabled(bEnableProxy);
		if (proxyInterface == null ||
			proxyInterface.getProtocol() ==
			ImmutableListenerInterface.PROTOCOL_TYPE_HTTP)
		{
			m_comboProxyType.setSelectedIndex(0);
		}
		else
		{
			m_comboProxyType.setSelectedIndex(1);
		}
		m_cbProxyAuthentication.setEnabled(bEnableProxy);
		if (proxyInterface != null)
		{
			m_tfProxyHost.setText(proxyInterface.getHost());
			m_tfProxyPortNumber.setText(String.valueOf(
				proxyInterface.getPort()));
			m_tfProxyAuthenticationUserID.setText(
				proxyInterface.getAuthenticationUserID());
			m_cbProxyAuthentication.setSelected(
				proxyInterface.isAuthenticationUsed());
		}
		m_labelProxyAuthUserID.setEnabled(m_cbProxyAuthentication.isSelected() & bEnableProxy);
		m_tfProxyAuthenticationUserID.setEnabled(m_cbProxyAuthentication.isSelected() & bEnableProxy);
		if (m_tfProxyPortNumber.getText().trim().equalsIgnoreCase("-1"))
		{
			m_tfProxyPortNumber.setText("");
		}
		validate();
	}

	protected void addNeedRestart(AbstractRestartNeedingConfigChange a_change)
	{
		if (a_change != null)
		{
			m_vecConfigChangesNeedRestart.addElement(a_change);
		}
	}

	private boolean isRestartNeeded()
	{
		return m_vecConfigChangesNeedRestart.size() > 0;
	}
}