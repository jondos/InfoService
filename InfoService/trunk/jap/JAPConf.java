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
import java.awt.BorderLayout;
import java.awt.Color;
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
import javax.swing.ButtonGroup;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JScrollPane;
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
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import org.bouncycastle.asn1.x509.X509NameTokenizer;
import anon.crypto.JAPCertificate;
import anon.crypto.JAPCertificateStore;
import anon.infoservice.InfoService;
import anon.infoservice.InfoServiceDatabase;
import anon.infoservice.InfoServiceHolder;
import anon.infoservice.MixCascade;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import update.JAPUpdate;

final class JAPConf extends JDialog
{

	final static public int PORT_TAB = 0;
	final static public int PROXY_TAB = 1;
	final static public int INFO_TAB = 2;
	final static public int ANON_TAB = 3;
	final static public int CERT_TAB = 4;
	final static public int KONTO_TAB = 5;
	final static public int MISC_TAB = 6;

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

	private JCheckBox m_cbAutoConnect;
	private JCheckBox m_cbAutoReConnect;
	private JCheckBox m_cbStartupMinimized;

	private JAPJIntField m_tfMixPortNumber;
	private JTextField m_tfMixHost;
	private String m_strMixName, m_strOldMixName;
	private JComboBox m_comboMixCascade;
	private JRadioButton m_rbMixStep1, m_rbMixStep2, m_rbMixStep3;
	private JButton m_bttnFetchCascades;
	private TitledBorder m_borderAnonSettings, m_borderAnonSettings2;
	private JLabel m_labelAnonHost, m_labelAnonPort;

	private DefaultListModel m_dlmCertList;
	private TitledBorder m_borderCertInfo;
	private JLabel m_labelTrust1, m_labelTrust2, m_labelDate, m_labelCN, m_labelE, m_labelCSTL, m_labelO,
		m_labelOU;
	private JLabel m_labelDateData, m_labelCNData, m_labelEData, m_labelCSTLData, m_labelOData, m_labelOUData;
	private JButton m_bttnCertInsert, m_bttnCertRemove, m_bttnCertEnable, m_bttnCertDisable;
	private DefaultListModel m_listmodelCertList;
	private JList m_listCert;
	private JScrollPane m_scrpaneList;
	private Enumeration m_enumCerts;
	private ListSelectionListener m_listsel;

	private JCheckBox m_cbDebugGui;
	private JCheckBox m_cbDebugNet;
	private JCheckBox m_cbDebugThread;
	private JCheckBox m_cbDebugMisc;
	private JCheckBox m_cbShowDebugConsole;
	private JCheckBox m_cbInfoServiceDisabled;
	private JCheckBox m_cbSaveWindowPositions;
	private JCheckBox m_cbCertCheckDisabled;

	private JSlider m_sliderDebugLevel;

	private JComboBox m_comboLanguage;
	private boolean m_bIgnoreComboLanguageEvents = false;

	private JCheckBox m_cbDummyTraffic;
	private JSlider m_sliderDummyTrafficIntervall;

	private JTabbedPane m_Tabs;
	private JPanel m_pPort, m_pFirewall, m_pInfo, m_pMix, m_pCert, m_pMisc, m_pKonto;
	private JButton m_bttnDefaultConfig, m_bttnCancel;

	private JFrame m_frmParent;

	private JAPConf m_JapConf;

	private Font m_fontControls;

	private static File m_fileCurrentDir;

	//Einfug
	private JAPUpdate update;
	private boolean loadPay = false;

	public static JAPConf getInstance()
	{
		return japConfInstance;
	}

	public JAPConf(JFrame frmParent, boolean loadPay)
	{
		super(frmParent);
		this.loadPay = loadPay;
		/* set the instance pointer */
		japConfInstance = this;
		m_frmParent = frmParent;
		m_Controller = JAPController.getController();
		setModal(true);
		setTitle(JAPMessages.getString("settingsDialog"));
		m_JapConf = this;
		JPanel pContainer = new JPanel();
		m_Tabs = new JTabbedPane();
		m_fontControls = JAPController.getDialogFont();
		pContainer.setLayout(new BorderLayout());
		m_Tabs.setFont(m_fontControls);
		m_pPort = buildPortPanel();
		m_pFirewall = buildProxyPanel();
		m_pInfo = buildInfoServicePanel();
		m_pMix = buildAnonPanel();
		m_pCert = buildCertPanel();
		if (loadPay)
		{
			m_pKonto = new pay.view.PayView();
			m_Tabs.addTab(pay.util.PayText.get("confAccountTab"), null, m_pKonto);
		}
		m_pMisc = buildMiscPanel();
		m_Tabs.addTab(JAPMessages.getString("confListenerTab"), null, m_pPort);
		m_Tabs.addTab(JAPMessages.getString("confProxyTab"), null, m_pFirewall);
		m_Tabs.addTab(JAPMessages.getString("confInfoTab"), null, m_pInfo);
		m_Tabs.addTab(JAPMessages.getString("confAnonTab"), null, m_pMix);
		m_Tabs.addTab(JAPMessages.getString("confCertTab"), null, m_pCert);
		if (!JAPModel.isSmallDisplay())
		{
			m_Tabs.addTab(JAPMessages.getString("confMiscTab"), null, m_pMisc);

		}
		JPanel buttonPanel = new JPanel();
		buttonPanel.setLayout(new FlowLayout(FlowLayout.RIGHT));
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
		if (loadPay)
		{
			ok.addActionListener( ( (pay.view.PayView) m_pKonto).userPanel);
		}
		ok.setFont(m_fontControls);
		buttonPanel.add(ok);
		buttonPanel.add(new JLabel("   "));
		getRootPane().setDefaultButton(ok);

		pContainer.add(m_Tabs, BorderLayout.CENTER);
		pContainer.add(buttonPanel, BorderLayout.SOUTH);
//				container.add(new JLabel(new ImageIcon(m_Controller.JAPICONFN)), BorderLayout.WEST);
		getContentPane().add(pContainer);
		//updateValues();
		// largest tab to front
		m_Tabs.setSelectedComponent(m_pMix);
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

		/*m_cbListenerSocks=new JCheckBox(JAPMessages.getString("settingsListenerCheckBoxSOCKS"));
			 m_cbListenerSocks.setForeground(portnumberLabel1.getForeground());
			 m_cbListenerSocks.addChangeListener(new ChangeListener(){

		 public void stateChanged(ChangeEvent e)
		  {
		   if(m_cbListenerSocks.isSelected())
			{
			 m_tfListenerPortNumberSocks.setEnabled(true);
			}
		   else
			//{
			 m_tfListenerPortNumberSocks.setEnabled(false);
			//}
		 }});
			 m_tfListenerPortNumberSocks = new JAPJIntField();
			 m_tfListenerPortNumberSocks.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
		  okPressed();
		  }});*/
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
		/*c.gridy=4;
			 c.insets=new Insets(10,0,0,0);
			 g.setConstraints(m_cbListenerSocks,c);
			 p1.add(m_cbListenerSocks);
			 c.gridy=5;
			 g.setConstraints(m_tfListenerPortNumberSocks,c);
			 p1.add(m_tfListenerPortNumberSocks);
			 c.gridy=6;
			 JSeparator seperator2=new JSeparator();
			 c.insets=new Insets(10,0,0,0);
			 g.setConstraints(seperator2,c);
			 p1.add(seperator2);
		 */
		c.gridy = 4;
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

	protected JPanel buildInfoServicePanel()
	{
		JPanel dummyPanel = new JPanel();
		dummyPanel.setLayout(new GridLayout(1, 1));
		dummyPanel.add(JAPConfInfoService.getInstance().createInfoServicePanel(m_fontControls));
		return dummyPanel;
	}

	protected JPanel buildAnonPanel()
	{
		m_cbStartupMinimized = new JCheckBox(JAPMessages.getString("settingsstartupMinimizeCheckBox"));
		m_cbStartupMinimized.setFont(m_fontControls);
		m_cbAutoConnect = new JCheckBox(JAPMessages.getString("settingsautoConnectCheckBox"));
		m_cbAutoConnect.setFont(m_fontControls);
		m_cbAutoReConnect = new JCheckBox(JAPMessages.getString("settingsautoReConnectCheckBox"));
		m_cbAutoReConnect.setFont(m_fontControls);
		m_tfMixHost = new JTextField();
		m_tfMixHost.setFont(m_fontControls);
		m_tfMixPortNumber = new JAPJIntField();
		m_tfMixPortNumber.setFont(m_fontControls);
		m_tfMixHost.setEditable(false);
		m_tfMixPortNumber.setEditable(false);
		m_tfMixHost.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				okPressed();
			}
		});
		m_tfMixPortNumber.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				okPressed();
			}
		});
		ButtonGroup bg = new ButtonGroup();
		m_rbMixStep1 = new JRadioButton(JAPMessages.getString("settingsAnonRadio1"), true);
		m_rbMixStep1.setFont(m_fontControls);
		m_rbMixStep2 = new JRadioButton(JAPMessages.getString("settingsAnonRadio2"));
		m_rbMixStep2.setFont(m_fontControls);
		m_rbMixStep3 = new JRadioButton(JAPMessages.getString("settingsAnonRadio3"));
		m_rbMixStep3.setFont(m_fontControls);
		m_bttnFetchCascades = new JButton(JAPMessages.getString("settingsAnonFetch"));
		m_bttnFetchCascades.setFont(m_fontControls);
		if (JAPModel.isSmallDisplay())
		{
			m_bttnFetchCascades.setMargin(JAPConstants.SMALL_BUTTON_MARGIN);
		}
		m_bttnFetchCascades.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				LogHolder.log(LogLevel.DEBUG, LogType.GUI, "JAPConf:m_bttnFetchCascades");
				// fetch available mix cascades from the Internet
				Cursor c = getCursor();
				setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				m_Controller.fetchMixCascades();
				updateMixCascadeCombo();
				LogHolder.log(LogLevel.DEBUG, LogType.GUI, "JAPConf: finished updateMixCascadeCombo()");
				if (m_Controller.getMixCascadeDatabase().size() == 0)
				{
					setCursor(c);
					if (!JAPModel.isSmallDisplay())
					{
						JOptionPane.showMessageDialog(m_Controller.getView(),
							JAPMessages.getString("settingsNoServersAvailable"),
							JAPMessages.getString("settingsNoServersAvailableTitle"),
							JOptionPane.INFORMATION_MESSAGE);
					}
				}
				else
				{
					// show a window containing all available cascades
					//JAPCascadeMonitorView v=new JAPCascadeMonitorView(m_Controller.getView());
					// ------ !!!!! die folgenden zwei zeilen auskommentieren, wenn JAPCascadeMonitorView
					// ------ !!!!! ordentlich geht!!!!
					LogHolder.log(LogLevel.DEBUG, LogType.GUI, "JAPConf: setting old cursor()");
					setCursor(c);
					m_rbMixStep2.doClick();
				}
				// ------ !!!!! diese wieder aktivieren!
				//okPressed();
			}
		});
		m_comboMixCascade = new JComboBox();
		m_comboMixCascade.setFont(m_fontControls);

		m_comboMixCascade.setEnabled(false);
		m_comboMixCascade.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				LogHolder.log(LogLevel.DEBUG, LogType.GUI,
							  "JAPConf:Item " + m_comboMixCascade.getSelectedIndex() + " selected");
				if (m_comboMixCascade.getSelectedIndex() > 0)
				{
					MixCascade mixCascadeEntry = (MixCascade) m_comboMixCascade.getSelectedItem();
					m_strMixName = mixCascadeEntry.getName();
					m_strOldMixName = m_strMixName;
					m_tfMixHost.setText(mixCascadeEntry.getListenerInterface(0).getHostName());
					m_tfMixPortNumber.setText(Integer.toString(mixCascadeEntry.getListenerInterface(0).
						getPort()));
				}
			}
		});
		bg.add(m_rbMixStep1);
		bg.add(m_rbMixStep2);
		bg.add(m_rbMixStep3);
		m_rbMixStep1.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				LogHolder.log(LogLevel.DEBUG, LogType.GUI, "JAPConf:m_rbMixStep1 selected");
				m_bttnFetchCascades.setEnabled(true);
				m_comboMixCascade.setEnabled(false);
				m_tfMixHost.setEditable(false);
				m_tfMixPortNumber.setEditable(false);
			}
		});
		m_rbMixStep2.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				LogHolder.log(LogLevel.DEBUG, LogType.GUI, "JAPConf:m_rbMixStep2 selected");
				m_bttnFetchCascades.setEnabled(false);
				m_comboMixCascade.setEnabled(true);
				m_comboMixCascade.setPopupVisible(true);
				m_tfMixHost.setEditable(false);
				m_tfMixPortNumber.setEditable(false);
			}
		});
		m_rbMixStep3.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				LogHolder.log(LogLevel.DEBUG, LogType.GUI, "JAPConf:m_rbMixStep3 selected");
				m_bttnFetchCascades.setEnabled(false);
				m_comboMixCascade.setEnabled(false);
				m_tfMixHost.setEditable(true);
				m_tfMixPortNumber.setEditable(true);
				m_strMixName = JAPMessages.getString("manual");
			}
		});

		// layout stuff
		JPanel p = new JPanel();
		p.setLayout(new BorderLayout());
		// Upper panel
		//First line
		JPanel pp1 = new JPanel();
		GridBagLayout layout = new GridBagLayout();
		pp1.setLayout(layout);
		m_borderAnonSettings = new TitledBorder(JAPMessages.getString("settingsAnonBorder"));
		m_borderAnonSettings.setTitleFont(m_fontControls);
		pp1.setBorder(m_borderAnonSettings);
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		c.weighty = 1;
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 2;
		c.anchor = GridBagConstraints.NORTHWEST;
		//First line
		JPanel pl1 = new JPanel(new BorderLayout());
		pl1.add(m_rbMixStep1, BorderLayout.CENTER);
		pl1.add(m_bttnFetchCascades, BorderLayout.EAST);
		layout.setConstraints(pl1, c);
		pp1.add(pl1);
		// Second Line
		c.gridx = 0;
		c.gridy = 1;
		c.weightx = 0;
		c.gridwidth = 1;
		c.fill = GridBagConstraints.NONE;
		layout.setConstraints(m_rbMixStep2, c);
		pp1.add(m_rbMixStep2);
		c.weightx = 1;
		c.gridx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.anchor = GridBagConstraints.NORTHEAST;
		layout.setConstraints(m_comboMixCascade, c);
		pp1.add(m_comboMixCascade);

		// Lower panel
		JPanel pp2 = new JPanel();
		pp2.setLayout(new BorderLayout());
		m_borderAnonSettings2 = new TitledBorder(JAPMessages.getString("settingsAnonBorder2"));
		m_borderAnonSettings2.setTitleFont(m_fontControls);
		pp2.setBorder(m_borderAnonSettings2);
		// Upper panel content
		// Lower Panel content
		JPanel p2 = new JPanel();
		p2.setLayout(new GridLayout(9, 1));
		//p1.setBorder( new EmptyBorder(5,10,10,10) );
		//
		p2.add(m_rbMixStep3);
		m_labelAnonHost = new JLabel(JAPMessages.getString("settingsAnonHost"));
		m_labelAnonHost.setFont(m_fontControls);
		p2.add(m_labelAnonHost);
		p2.add(m_tfMixHost);
		m_labelAnonPort = new JLabel(JAPMessages.getString("settingsAnonPort"));
		m_labelAnonPort.setFont(m_fontControls);
		p2.add(m_labelAnonPort);
		p2.add(m_tfMixPortNumber);
		//
		p2.add(m_cbAutoConnect);
		p2.add(m_cbAutoReConnect);
		// Add contents to upper and lower panel
		//pp1.add(p1);
		pp2.add(p2);
		// Add to main panel
		p.add(pp1, BorderLayout.NORTH);
		p.add(pp2, BorderLayout.CENTER);
		return p;
	}

	/*
	 * TODO: updates info panel by list element, ugly, needs fix
	 */
	private void updateInfoPanel(String currIssuerCN)
	{
		m_enumCerts = JAPModel.getCertificateStore().elements();
		JAPCertificate j;
		while (m_enumCerts.hasMoreElements())
		{
			j = (JAPCertificate) m_enumCerts.nextElement();

			if (j.getIssuer().getValues().elementAt(0).equals(currIssuerCN))
			{
				m_labelDateData.setText(j.getStartDate().toGMTString() + " - " + j.getEndDate().toGMTString());
				m_labelCNData.setText("");
				m_labelEData.setText("");
				m_labelCSTLData.setText("");
				m_labelOData.setText("");
				m_labelOUData.setText("");

				X509NameTokenizer m_issuerData = new X509NameTokenizer(j.getIssuer().toString());
				while (m_issuerData.hasMoreTokens())
				{
					String m_element = (String) m_issuerData.nextToken();
					if (m_element.startsWith("CN="))
					{
						m_labelCNData.setText(m_element.substring(3));
					}
					else if (m_element.startsWith("E="))
					{
						m_labelEData.setText(m_element.substring(2));
					}
					else if (m_element.startsWith("C="))
					{
						m_labelCSTLData.setText(m_element.substring(2) + m_labelCSTLData.getText());
					}
					else if (m_element.startsWith("ST="))
					{
						m_labelCSTLData.setText(m_element.substring(3) + " / " + m_labelCSTLData.getText());
					}
					else if (m_element.startsWith("L="))
					{
						m_labelCSTLData.setText(m_element.substring(2) + " / " + m_labelCSTLData.getText());
					}
					else if (m_element.startsWith("O="))
					{
						m_labelOData.setText(m_element.substring(2));
					}
					else if (m_element.startsWith("OU="))
					{
						m_labelOUData.setText(m_element.substring(3));
					}
				}

				if (m_labelCSTLData.getText().trim().endsWith("/"))
				{
					String t_label = m_labelCSTLData.getText().trim();
					int length = t_label.length();
					t_label = t_label.substring(0, length - 1);
					m_labelCSTLData.setText(t_label);
				}
				if (j.getEnabled())
				{
					m_listCert.setSelectionForeground(Color.black);
				}
				else
				{
					m_listCert.setSelectionForeground(Color.red);

					// m_tfIssuer.setText(j.getIssuer().toString());
				}
			}
		} // while
	}

	protected JPanel buildCertPanel()
	{

		JPanel p = new JPanel();
		p.setLayout(new BorderLayout());

		JPanel caPanel = new JPanel();
		GridBagLayout caPanelLayout = new GridBagLayout();
		caPanel.setLayout(caPanelLayout);

		m_labelTrust1 = new JLabel(JAPMessages.getString("certTrust1"));
		m_labelTrust1.setFont(m_fontControls);
		m_labelTrust2 = new JLabel(JAPMessages.getString("certTrust2"));
		m_labelTrust2.setFont(m_fontControls);

		GridBagConstraints caPanelConstraints = new GridBagConstraints();
		caPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
		caPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
		caPanelConstraints.weightx = 1.0;
		caPanelConstraints.insets = new Insets(10, 10, 0, 0);

		caPanelConstraints.gridx = 0;
		caPanelConstraints.gridy = 0;
		caPanelLayout.setConstraints(m_labelTrust1, caPanelConstraints);
		caPanel.add(m_labelTrust1);

		caPanelConstraints.gridx = 0;
		caPanelConstraints.gridy = 1;
		caPanelConstraints.insets = new Insets(0, 10, 10, 0);
		caPanelLayout.setConstraints(m_labelTrust2, caPanelConstraints);
		caPanel.add(m_labelTrust2);

		GridBagConstraints c = new GridBagConstraints();

//		GridBagLayout layout1 = new GridBagLayout();
//		pp2.setLayout(layout1);
//		m_borderCertInfo = new TitledBorder(JAPMessages.getString("certBorder"));
//		m_borderCertInfo.setTitleFont(m_fontControls);
//		pp2.setBorder(m_borderCertInfo);

		m_listmodelCertList = new DefaultListModel();

		// list init, add certificates by issuer name
		m_enumCerts = JAPModel.getCertificateStore().elements();
		while (m_enumCerts.hasMoreElements())
		{
			JAPCertificate j = (JAPCertificate) m_enumCerts.nextElement();
			String issuerCN = (String) j.getIssuer().getValues().elementAt(0);
			m_listmodelCertList.addElement(issuerCN);
		}

		m_listCert = new JList(m_listmodelCertList);
		m_listCert.setSelectedIndex(0);

		m_listCert.addListSelectionListener(new ListSelectionListener()
		{

			public void valueChanged(ListSelectionEvent e)
			{
				System.out.println(m_listmodelCertList.getSize());

				if (m_listmodelCertList.getSize() == 0)
				{
					m_labelDateData.setText("");
					m_labelCNData.setText("");
					m_labelEData.setText("");
					m_labelCSTLData.setText("");
					m_labelOData.setText("");
					m_labelOUData.setText("");
				}
				else
				{
					String currIssuerCN = (String) m_listCert.getSelectedValue();
					updateInfoPanel(currIssuerCN);

					/*					m_enumCerts = JAPModel.getCertificateStore().elements();
						 JAPCertificate j;
						 while (m_enumCerts.hasMoreElements())
						 {
						  j = (JAPCertificate) m_enumCerts.nextElement();

						  if (j.getIssuer().getValues().elementAt(0).equals(currIssuerCN))
						  {
					 m_labelDateData.setText(j.getStartDate().toGMTString() + " - " + j.getEndDate().toGMTString());
						   m_labelCNData.setText("");
						   m_labelEData.setText("");
						   m_labelCSTLData.setText("");
						   m_labelOData.setText("");
						   m_labelOUData.setText("");

						   X509NameTokenizer m_issuerData = new X509NameTokenizer(j.getIssuer().toString());
						   while (m_issuerData.hasMoreTokens())
						   {
							String m_element = (String) m_issuerData.nextToken();
							if (m_element.startsWith("CN="))
							 m_labelCNData.setText(m_element.substring(3));
							else if (m_element.startsWith("E="))
							 m_labelEData.setText(m_element.substring(2));
							else if (m_element.startsWith("C="))
							 m_labelCSTLData.setText(m_element.substring(2) + m_labelCSTLData.getText());
							else if (m_element.startsWith("ST="))
					 m_labelCSTLData.setText(m_element.substring(3) + " / " + m_labelCSTLData.getText());
							else if (m_element.startsWith("L="))
					 m_labelCSTLData.setText(m_element.substring(2) + " / " + m_labelCSTLData.getText());
							else if (m_element.startsWith("O="))
							 m_labelOData.setText(m_element.substring(2));
							else if (m_element.startsWith("OU="))
							 m_labelOUData.setText(m_element.substring(3));
						   }

						   if (m_labelCSTLData.getText().trim().endsWith("/"))
						   {
							String t_label = m_labelCSTLData.getText().trim();
							int length = t_label.length();
							t_label = t_label.substring(0,length-1);
							m_labelCSTLData.setText(t_label);
						   }
						   if (j.getEnabled())
							m_listCert.setSelectionForeground(Color.black);
						   else
							m_listCert.setSelectionForeground(Color.red);

						   // m_tfIssuer.setText(j.getIssuer().toString());
						  }
						 } // while */
				} // else

			} // valuechanged
		});

		m_scrpaneList = new JScrollPane();
		m_scrpaneList.getViewport().add(m_listCert, null);
		caPanelConstraints.gridx = 0;
		caPanelConstraints.gridy = 2;
		caPanelConstraints.gridheight = 5;
		caPanelConstraints.weighty = 1.0;
		caPanelConstraints.insets = new Insets(0, 10, 20, 0);
		caPanelConstraints.fill = GridBagConstraints.BOTH;
		caPanelLayout.setConstraints(m_scrpaneList, caPanelConstraints);
		caPanel.add(m_scrpaneList);

		m_bttnCertInsert = new JButton(JAPMessages.getString("certBttnInsert"));
		m_bttnCertInsert.setFont(m_fontControls);
		m_bttnCertInsert.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				JAPCertificate t_cert = JAPUtil.openFile(new JFrame());
				if (t_cert != null)
				{
					String issuerCN = (String) t_cert.getIssuer().getValues().elementAt(0);

					JAPCertificateStore jcs = JAPModel.getCertificateStore();

					if (!jcs.checkCertificateExists(t_cert))
					{
						System.out.println("zertifikat existiert nicht ...");
						System.out.println("groesse:  " + jcs.size());
						m_listmodelCertList.addElement(issuerCN);
						// m_listCert.removeAll();
						// m_listCert.setModel(m_listmodelCertList);
						// m_scrpaneList.getViewport().removeAll();
						// m_scrpaneList.getViewport().add(m_listCert, null);
						jcs.addCertificate(t_cert);
						JAPController.setCertificateStore(jcs);
						jcs.dumpKeys();
					}
					else
					{
						System.out.println("zertifikat existiert");
					}
				}
				m_bttnCertRemove.setEnabled(true);
				m_bttnCertDisable.setEnabled(true);
				m_bttnCertEnable.setEnabled(true);
				if (m_listmodelCertList.getSize() == 1)
				{
					m_labelDateData.setText("");
					m_labelCNData.setText("");
					m_labelEData.setText("");
					m_labelCSTLData.setText("");
					m_labelOData.setText("");
					m_labelOUData.setText("");

					m_listCert.setSelectedIndex(0);
					String currIssuerCN = (String) m_listCert.getSelectedValue();
					updateInfoPanel(currIssuerCN);

					/*								m_enumCerts = JAPModel.getCertificateStore().elements();
							JAPCertificate j;
							while (m_enumCerts.hasMoreElements())
							{
							 j = (JAPCertificate) m_enumCerts.nextElement();
							 if (j.getIssuer().getValues().elementAt(0).equals(currIssuerCN))
							 {
					 m_labelDateData.setText(j.getStartDate().toGMTString() + " - " + j.getEndDate().toGMTString());
							  m_labelCNData.setText("");
							  m_labelEData.setText("");
							  m_labelCSTLData.setText("");
							  m_labelOData.setText("");
							  m_labelOUData.setText("");

					 X509NameTokenizer m_issuerData = new X509NameTokenizer(j.getIssuer().toString());
							  while (m_issuerData.hasMoreTokens())
							  {
							   String m_element = (String) m_issuerData.nextToken();
							   if (m_element.startsWith("CN="))
								m_labelCNData.setText(m_element.substring(3));
							   else if (m_element.startsWith("E="))
								m_labelEData.setText(m_element.substring(2));
							   else if (m_element.startsWith("C="))
								m_labelCSTLData.setText(m_element.substring(2) + m_labelCSTLData.getText());
							   else if (m_element.startsWith("ST="))
					 m_labelCSTLData.setText(m_element.substring(3) + " / " + m_labelCSTLData.getText());
							   else if (m_element.startsWith("L="))
					 m_labelCSTLData.setText(m_element.substring(2) + " / " + m_labelCSTLData.getText());
							   else if (m_element.startsWith("O="))
								m_labelOData.setText(m_element.substring(2));
							   else if (m_element.startsWith("OU="))
								m_labelOUData.setText(m_element.substring(3));
							  } // while
							 } // if
							} // while */
				}

			}
		});

		m_bttnCertRemove = new JButton(JAPMessages.getString("certBttnRemove"));
		m_bttnCertRemove.setFont(m_fontControls);
		m_bttnCertRemove.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				int t_index = m_listCert.getSelectedIndex();
				String t_issuerCN = (String) m_listCert.getSelectedValue();
				m_enumCerts = JAPModel.getCertificateStore().elements();
				while (m_enumCerts.hasMoreElements())
				{
					JAPCertificate j = (JAPCertificate) m_enumCerts.nextElement();
					String issuerCN = (String) j.getIssuer().getValues().elementAt(0);
					if (issuerCN.equals(t_issuerCN))
					{
						JAPCertificateStore jcs = JAPModel.getCertificateStore();
						jcs.removeCertificate(j);
						m_listmodelCertList.remove(t_index);
						JAPController.setCertificateStore(jcs);
					}
				}
				if (m_listmodelCertList.getSize() == 0)
				{
					m_bttnCertRemove.setEnabled(false);
					m_bttnCertDisable.setEnabled(false);
					m_bttnCertEnable.setEnabled(false);
					m_labelDateData.setText("");
					m_labelCNData.setText("");
					m_labelEData.setText("");
					m_labelCSTLData.setText("");
					m_labelOData.setText("");
					m_labelOUData.setText("");
				}
				else
				{
					m_labelDateData.setText("");
					m_labelCNData.setText("");
					m_labelEData.setText("");
					m_labelCSTLData.setText("");
					m_labelOData.setText("");
					m_labelOUData.setText("");

					m_listCert.setSelectedIndex(0);
					String currIssuerCN = (String) m_listCert.getSelectedValue();
					updateInfoPanel(currIssuerCN);

					/*
							   m_enumCerts = JAPModel.getCertificateStore().elements();
							   JAPCertificate j;
							   while (m_enumCerts.hasMoreElements())
							   {
								j = (JAPCertificate) m_enumCerts.nextElement();
								if (j.getIssuer().getValues().elementAt(0).equals(currIssuerCN))
								{
					 m_labelDateData.setText(j.getStartDate().toGMTString() + " - " + j.getEndDate().toGMTString());
								 m_labelCNData.setText("");
								 m_labelEData.setText("");
								 m_labelCSTLData.setText("");
								 m_labelOData.setText("");
								 m_labelOUData.setText("");

					 X509NameTokenizer m_issuerData = new X509NameTokenizer(j.getIssuer().toString());
								 while (m_issuerData.hasMoreTokens())
								 {
								  String m_element = (String) m_issuerData.nextToken();
								  if (m_element.startsWith("CN="))
								   m_labelCNData.setText(m_element.substring(3));
								  else if (m_element.startsWith("E="))
								   m_labelEData.setText(m_element.substring(2));
								  else if (m_element.startsWith("C="))
					 m_labelCSTLData.setText(m_element.substring(2) + m_labelCSTLData.getText());
								  else if (m_element.startsWith("ST="))
					 m_labelCSTLData.setText(m_element.substring(3) + " / " + m_labelCSTLData.getText());
								  else if (m_element.startsWith("L="))
					 m_labelCSTLData.setText(m_element.substring(2) + " / " + m_labelCSTLData.getText());
								  else if (m_element.startsWith("O="))
								   m_labelOData.setText(m_element.substring(2));
								  else if (m_element.startsWith("OU="))
								   m_labelOUData.setText(m_element.substring(3));
								 } // while
								} // if
							   } // while */

				}

			}
		});

		m_bttnCertEnable = new JButton(JAPMessages.getString("certBttnEnable"));
		m_bttnCertEnable.setFont(m_fontControls);
		m_bttnCertEnable.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				int t_index = m_listCert.getSelectedIndex();
				String t_issuerCN = (String) m_listCert.getSelectedValue();
				m_enumCerts = JAPModel.getCertificateStore().elements();
				while (m_enumCerts.hasMoreElements())
				{
					JAPCertificate j = (JAPCertificate) m_enumCerts.nextElement();
					String issuerCN = (String) j.getIssuer().getValues().elementAt(0);
					if (issuerCN.equals(t_issuerCN))
					{
						JAPCertificateStore jcs = JAPModel.getCertificateStore();
						jcs.enableCertificate(j);
						m_listCert.setSelectionForeground(Color.black);
						JAPController.setCertificateStore(jcs);
					}
				}
			}
		});

		m_bttnCertDisable = new JButton(JAPMessages.getString("certBttnDisable"));
		m_bttnCertDisable.setFont(m_fontControls);
		m_bttnCertDisable.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				int t_index = m_listCert.getSelectedIndex();
				String t_issuerCN = (String) m_listCert.getSelectedValue();
				m_enumCerts = JAPModel.getCertificateStore().elements();
				while (m_enumCerts.hasMoreElements())
				{
					JAPCertificate j = (JAPCertificate) m_enumCerts.nextElement();
					String issuerCN = (String) j.getIssuer().getValues().elementAt(0);
					if (issuerCN.equals(t_issuerCN))
					{
						JAPCertificateStore jcs = JAPModel.getCertificateStore();
						jcs.disableCertificate(j);
						m_listCert.setSelectionForeground(Color.red);
						JAPController.setCertificateStore(jcs);
						break;
					}
				}
			}
		});

		/*
			 c.gridx = 0;
			 c.gridy = 0;
			 c.gridwidth = 1;
			 c.gridheight = 2;
			 c.weightx = 1;
//	 c.weighty = 1;
			 c.fill = GridBagConstraints.HORIZONTAL;
			 c.anchor = GridBagConstraints.CENTER;
		 */

		caPanelConstraints.gridx = 2;
		caPanelConstraints.gridy = 2;
		caPanelConstraints.weightx = 0.0;
		caPanelConstraints.gridheight = 1;
		caPanelConstraints.weighty = 0.0;
		caPanelConstraints.fill = GridBagConstraints.BOTH;
		caPanelConstraints.insets = new Insets(0, 10, 0, 0);
		caPanelLayout.setConstraints(m_bttnCertInsert, caPanelConstraints);
		caPanel.add(m_bttnCertInsert);

		caPanelConstraints.gridx = 2;
		caPanelConstraints.gridy = 3;
		caPanelConstraints.fill = GridBagConstraints.BOTH;
		caPanelLayout.setConstraints(m_bttnCertRemove, caPanelConstraints);
		caPanel.add(m_bttnCertRemove);

		caPanelConstraints.gridx = 2;
		caPanelConstraints.gridy = 4;
		caPanelConstraints.fill = GridBagConstraints.BOTH;
		caPanelLayout.setConstraints(m_bttnCertEnable, caPanelConstraints);
		caPanel.add(m_bttnCertEnable);

		caPanelConstraints.gridx = 2;
		caPanelConstraints.gridy = 5;
		caPanelConstraints.fill = GridBagConstraints.BOTH;
		caPanelLayout.setConstraints(m_bttnCertDisable, caPanelConstraints);
		caPanel.add(m_bttnCertDisable);

		JPanel infoPanel = new JPanel();
		GridBagLayout infoPanelLayout = new GridBagLayout();
		infoPanel.setLayout(infoPanelLayout);

		GridBagConstraints infoPanelConstraints = new GridBagConstraints();
		infoPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
		infoPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
		infoPanelConstraints.weightx = 1.0;
		infoPanelConstraints.insets = new Insets(10, 10, 0, 0);

		infoPanelConstraints.gridx = 0;
		infoPanelConstraints.gridy = 0;

		m_borderCertInfo = new TitledBorder(JAPMessages.getString("certInfoBorder"));
		m_borderCertInfo.setTitleFont(m_fontControls);
		infoPanel.setBorder(m_borderCertInfo);

		m_labelDate = new JLabel(JAPMessages.getString("certDate"));
		m_labelDate.setFont(m_fontControls);
//				m_labelIssuer = new JLabel(JAPMessages.getString("certIssuer"));

		m_labelCN = new JLabel(JAPMessages.getString("certName"));
		m_labelCN.setFont(m_fontControls);
		m_labelE = new JLabel(JAPMessages.getString("certMail"));
		m_labelE.setFont(m_fontControls);
		m_labelCSTL = new JLabel(JAPMessages.getString("certLocation"));
		m_labelCSTL.setFont(m_fontControls);
		m_labelO = new JLabel(JAPMessages.getString("certOrg"));
		m_labelO.setFont(m_fontControls);
		m_labelOU = new JLabel(JAPMessages.getString("certOrgUnit"));
		m_labelOU.setFont(m_fontControls);

		/*
		 m_tfDate = new JTextField();
		 m_tfDate.setFont(m_fontControls);
		 m_tfDate.setEditable(false);
		 */

		m_labelDateData = new JLabel();
		m_labelDateData.setFont(m_fontControls);

		/*
		 m_tfCN = new JTextField();
		 m_tfCN.setFont(m_fontControls);
		 m_tfCN.setEditable(false);
		 */

		m_labelCNData = new JLabel();
		m_labelCNData.setFont(m_fontControls);

		/*
		 m_tfE = new JTextField();
		 m_tfE.setFont(m_fontControls);
		 m_tfE.setEditable(false);
		 */

		m_labelEData = new JLabel();
		m_labelEData.setFont(m_fontControls);

		/*
		 m_tfCSTL = new JTextField();
		 m_tfCSTL.setFont(m_fontControls);
		 m_tfCSTL.setEditable(false);
		 */

		m_labelCSTLData = new JLabel();
		m_labelCSTLData.setFont(m_fontControls);

		/*
		 m_tfO = new JTextField();
		 m_tfO.setFont(m_fontControls);
		 m_tfO.setEditable(false);
		 */

		m_labelOData = new JLabel();
		m_labelOData.setFont(m_fontControls);

		/*
		 m_tfOU = new JTextField();
		 m_tfOU.setFont(m_fontControls);
		 m_tfOU.setEditable(false);
		 */

		m_labelOUData = new JLabel();
		m_labelOUData.setFont(m_fontControls);

		// init with first certificate of store
		JAPCertificateStore csw = JAPModel.getCertificateStore();
		if (csw != null && csw.elements().hasMoreElements())
		{
			JAPCertificate j = (JAPCertificate) csw.elements().nextElement();

			m_labelDateData.setText(j.getStartDate().toGMTString() + " - " + j.getEndDate().toGMTString());

			X509NameTokenizer m_issuerData = new X509NameTokenizer(j.getIssuer().toString());
			while (m_issuerData.hasMoreTokens())
			{
				String m_element = (String) m_issuerData.nextToken();
				if (m_element.startsWith("CN="))
				{
					m_labelCNData.setText(m_element.substring(3));
				}
				else if (m_element.startsWith("E="))
				{
					m_labelEData.setText(m_element.substring(2));
				}
				else if (m_element.startsWith("C="))
				{
					m_labelCSTLData.setText(m_element.substring(2) + " / " + m_labelCSTLData.getText());
				}
				else if (m_element.startsWith("ST="))
				{
					m_labelCSTLData.setText(m_element.substring(3) + " / " + m_labelCSTLData.getText());
				}
				else if (m_element.startsWith("L="))
				{
					m_labelCSTLData.setText(m_element.substring(2) + " / " + m_labelCSTLData.getText());
				}
				else if (m_element.startsWith("O="))
				{
					m_labelOData.setText(m_element.substring(2));
				}
				else if (m_element.startsWith("OU="))
				{
					m_labelOUData.setText(m_element.substring(3));

				}
			}
			if (m_labelCSTLData.getText().trim().endsWith("/"))
			{
				String t_label = m_labelCSTLData.getText().trim();
				int length = t_label.length();
				t_label = t_label.substring(0, length - 1);
				m_labelCSTLData.setText(t_label);
			}
		}

		/*		    	gridx
			0:				1:
		 gridy	0:
		   1:  labelDate		labelDateData
		   2:  labelCN			labelCNData
		   3:	labelE			labelEData
		   4:	labelCSTL		labelCSTLData
		   5:	labelO			labelOData
		   6:	labelOU			labelOUData
		 */

		// infoPanelConstraints.weightx = 1.0;
		infoPanelConstraints.ipadx = 5;
		infoPanelConstraints.ipady = 5;

		infoPanelConstraints.gridx = 0;
		infoPanelConstraints.gridy = 1;
		infoPanelConstraints.ipadx = 10;
		infoPanelConstraints.fill = GridBagConstraints.BOTH;
		infoPanelConstraints.insets = new Insets(0, 10, 0, 10);
		infoPanelLayout.setConstraints(m_labelDate, infoPanelConstraints);
		infoPanel.add(m_labelDate);

		infoPanelConstraints.gridx = 1;
		infoPanelConstraints.gridy = 1;
		infoPanelLayout.setConstraints(m_labelDateData, infoPanelConstraints);
		infoPanel.add(m_labelDateData);

		infoPanelConstraints.gridx = 0;
		infoPanelConstraints.gridy = 2;
		infoPanelConstraints.fill = GridBagConstraints.BOTH;
		infoPanelConstraints.insets = new Insets(0, 10, 0, 10);
		infoPanelLayout.setConstraints(m_labelCN, infoPanelConstraints);
		infoPanel.add(m_labelCN);

		infoPanelConstraints.gridx = 1;
		infoPanelConstraints.gridy = 2;
		infoPanelConstraints.fill = GridBagConstraints.BOTH;
		infoPanelLayout.setConstraints(m_labelCNData, infoPanelConstraints);
		infoPanel.add(m_labelCNData);

		infoPanelConstraints.gridx = 0;
		infoPanelConstraints.gridy = 3;
		infoPanelConstraints.fill = GridBagConstraints.BOTH;
		infoPanelConstraints.insets = new Insets(0, 10, 0, 10);
		infoPanelLayout.setConstraints(m_labelE, infoPanelConstraints);
		infoPanel.add(m_labelE);

		infoPanelConstraints.gridx = 1;
		infoPanelConstraints.gridy = 3;
		infoPanelConstraints.fill = GridBagConstraints.BOTH;
		infoPanelLayout.setConstraints(m_labelEData, infoPanelConstraints);
		infoPanel.add(m_labelEData);

		infoPanelConstraints.gridx = 0;
		infoPanelConstraints.gridy = 4;
		infoPanelConstraints.fill = GridBagConstraints.BOTH;
		infoPanelConstraints.insets = new Insets(0, 10, 0, 10);
		infoPanelLayout.setConstraints(m_labelCSTL, infoPanelConstraints);
		infoPanel.add(m_labelCSTL);

		infoPanelConstraints.gridx = 1;
		infoPanelConstraints.gridy = 4;
		infoPanelConstraints.fill = GridBagConstraints.BOTH;
		infoPanelLayout.setConstraints(m_labelCSTLData, infoPanelConstraints);
		infoPanel.add(m_labelCSTLData);

		infoPanelConstraints.gridx = 0;
		infoPanelConstraints.gridy = 5;
		infoPanelConstraints.fill = GridBagConstraints.BOTH;
		infoPanelLayout.setConstraints(m_labelO, infoPanelConstraints);
		infoPanel.add(m_labelO);

		infoPanelConstraints.gridx = 1;
		infoPanelConstraints.gridy = 5;
		infoPanelLayout.setConstraints(m_labelOData, infoPanelConstraints);
		infoPanel.add(m_labelOData);

		infoPanelConstraints.gridx = 0;
		infoPanelConstraints.gridy = 6;
		infoPanelConstraints.fill = GridBagConstraints.BOTH;
		infoPanelLayout.setConstraints(m_labelOU, infoPanelConstraints);
		infoPanel.add(m_labelOU);

		infoPanelConstraints.gridx = 1;
		infoPanelConstraints.gridy = 6;
		infoPanelLayout.setConstraints(m_labelOUData, infoPanelConstraints);
		infoPanel.add(m_labelOUData);

		p.add(caPanel, BorderLayout.CENTER); // , BorderLayout.NORTH);
		p.add(infoPanel, BorderLayout.SOUTH);

		return p;
	}

	protected JPanel buildMiscPanel()
	{
		JPanel p = new JPanel();
		p.setLayout(new BorderLayout());

		// Panel for Look and Feel Options
		JPanel p1 = new JPanel();
		p1.setLayout(new GridLayout(2, 2));
		p1.setBorder(new TitledBorder(JAPMessages.getString("settingsLookAndFeelBorder")));
		p1.add(new JLabel(JAPMessages.getString("settingsLookAndFeel")));
		JComboBox c = new JComboBox();
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
		p1.add(c);
		p1.add(new JLabel(JAPMessages.getString("settingsLanguage")));
		m_comboLanguage = new JComboBox();
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

		m_cbInfoServiceDisabled = new JCheckBox("Disable InfoService");
		m_cbCertCheckDisabled = new JCheckBox("Disable check of certificates");
		m_cbSaveWindowPositions = new JCheckBox("Remember Location of JAP");
		JPanel p22 = new JPanel();
		GridBagLayout gb = new GridBagLayout();
		p22.setLayout(gb);
		GridBagConstraints lc = new GridBagConstraints();

		//p22.add(bttnPing);
		//////////////////////////////////////////////////////////////////
		//Einfug
		JButton testButton = new JButton("Update");
		testButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				Cursor c1 = getCursor();
				setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				cancelPressed();
				update = new JAPUpdate();
//  if (update == null){

				//         }//fi
				//updateValues();
				setCursor(c1);
			}
		});
		testButton.setVisible(true);
		testButton.setEnabled(true);
		lc.gridx = 0;
		lc.gridy = 0;
		lc.gridheight = 1;
		lc.gridwidth = 1;
		lc.anchor = lc.NORTHWEST;
		lc.fill = lc.HORIZONTAL;
		lc.weightx = 1;
		lc.weighty = 1;
		gb.setConstraints(testButton, lc);
		p22.add(testButton);
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
		gb.setConstraints(m_cbInfoServiceDisabled, lc);
		p22.add(m_cbInfoServiceDisabled);
		lc.gridy++;
		gb.setConstraints(m_cbSaveWindowPositions, lc);
		p22.add(m_cbSaveWindowPositions);
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
		pp.add(p1, BorderLayout.NORTH);
		pp.add(p2, BorderLayout.CENTER);

		p.add(p3, BorderLayout.WEST);
		p.add(pp, BorderLayout.CENTER);

		return p;
	}

	protected void cancelPressed()
	{
		setVisible(false);
	}

	/**Shows a Dialog about whats going wrong
	 */
	public static void showError(String msg)
	{
		JOptionPane.showMessageDialog(japConfInstance, msg, JAPMessages.getString("ERROR"),
									  JOptionPane.ERROR_MESSAGE);
	}

	/** Checks if all Input in all Fiels make sense. Displays InfoBoxes about what is wrong.
	 * @return true if all is ok
	 *					false otherwise
	 */
	private boolean checkValues()
	{
		String s = null;
		int iListenerPort, i;

		//Checking First Mix (Host + Port)
		if (m_rbMixStep3.isSelected())
		{ // -- do stuff for manual setting of anon service
			String host = m_tfMixHost.getText().trim();
			if (host == null || host.equals(""))
			{
				showError(JAPMessages.getString("errorAnonHostNotNull"));
				return false;
			}
			int port;
			try
			{
				port = Integer.parseInt(m_tfMixPortNumber.getText().trim());
			}
			catch (Exception e)
			{
				port = -1;
			}
			if (!JAPUtil.isPort(port))
			{
				showError(JAPMessages.getString("errorAnonServicePortWrong"));
				return false;
			}
			//now combine it together...
			try
			{
				/* this is only a test for the values */
				MixCascade newMixCascade = new MixCascade(host, port);
			}
			catch (Exception ex)
			{
				showError(JAPMessages.getString("errorAnonServiceWrong"));
				return false;
			}
			int anonSSLPortNumber = -1;
		}
		else
		if (m_rbMixStep2.isSelected())
		{
			//AnonService NOT manual selected
			try
			{
				/* check, if something is selected */
				MixCascade selectedMixCascade = (MixCascade) m_comboMixCascade.getSelectedItem();
			}
			catch (Exception e)
			{
				showError(JAPMessages.getString("errorPleaseSelectAnonService"));
				return false;
			}
		}

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
		/*if(m_cbListenerSocks.isSelected())
		 {
		  try
		   {
			i=Integer.parseInt(m_tfListenerPortNumberSocks.getText().trim());
		   }
		  catch(Exception e)
		   {
			i=-1;
		   }
		  if(!JAPUtil.isPort(i))
		   {
			showError(JAPMessages.getString("errorSocksListenerPortWrong"));
			return false;
		   }
		  if(i==iListenerPort)
		   {
			showError(JAPMessages.getString("errorListenerPortsAreEqual"));
			return false;
		   }
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
		if (loadPay)
		{
			( (pay.view.PayView) m_pKonto).userPanel.setUserData(JAPConstants.PIHOST, JAPConstants.PIPORT);
		}
		m_tfListenerPortNumber.setText(Integer.toString(JAPConstants.defaultPortNumber));

		/* remove all infoservices from database and set prefered infoservice to the default
		 * infoservice
		 */
		InfoServiceDatabase.getInstance().removeAll();
		try
		{
			InfoService defaultInfoService = new InfoService(JAPConstants.defaultInfoServiceHostName,
				JAPConstants.defaultInfoServicePortNumber);
			InfoServiceHolder.getInstance().setPreferedInfoService(defaultInfoService);
		}
		catch (Exception e)
		{
			/* should not happen, if it happens, we can't do anything */
			LogHolder.log(LogLevel.EXCEPTION, LogType.MISC,
						  "JAPConf: resetToDefault: Cannot create the default infoservice.");
		}
		/* update the GUI */
		JAPConfInfoService.getInstance().updateGuiOutput();

		m_tfMixHost.setText(JAPConstants.defaultAnonHost);
		m_tfMixPortNumber.setText(Integer.toString(JAPConstants.defaultAnonPortNumber));
		m_cbProxy.setSelected(false);
		m_cbStartupMinimized.setSelected(false);
		m_cbAutoConnect.setSelected(false);
		m_cbAutoReConnect.setSelected(false);
		m_cbListenerIsLocal.setSelected(true);
		//m_cbListenerSocks.setSelected(false);
		m_cbShowDebugConsole.setSelected(false);
		m_sliderDebugLevel.setValue(LogLevel.EMERG);
		m_cbDebugNet.setSelected(false);
		m_cbDebugGui.setSelected(false);
		m_cbDebugMisc.setSelected(false);
		m_cbDebugThread.setSelected(false);
		m_cbDummyTraffic.setSelected(false);
		m_cbInfoServiceDisabled.setSelected(false);
		m_cbCertCheckDisabled.setSelected(false);
	}

	protected void okPressed()
	{
		if (!checkValues())
		{
			return;
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
		LogHolder.log(LogLevel.DEBUG, LogType.GUI,
					  "m_comboLanguage: " + Integer.toString(m_comboLanguage.getSelectedIndex()));
		if (m_comboLanguage.getSelectedIndex() == 0)
		{
			m_Controller.setLocale(Locale.GERMAN);
		}
		else if (m_comboLanguage.getSelectedIndex() == 1)
		{
			m_Controller.setLocale(Locale.ENGLISH);
		}
		else
		{
			m_Controller.setLocale(Locale.FRENCH);
		}
		if (m_cbDummyTraffic.isSelected())
		{
			m_Controller.setDummyTraffic(m_sliderDummyTrafficIntervall.getValue() * 1000);
		}
		else
		{
			m_Controller.setDummyTraffic( -1);
			// Listener settings
		}
		m_Controller.setHTTPListener(Integer.parseInt(m_tfListenerPortNumber.getText().trim()),
									 m_cbListenerIsLocal.isSelected(), true);
//				m_Controller.setUseSocksPort(m_cbListenerSocks.isSelected());
//				m_Controller.setSocksPortNumber(Integer.parseInt(m_tfListenerPortNumberSocks.getText().trim()));
		// Firewall settings
		int port = -1;
		try
		{
			port = Integer.parseInt(m_tfProxyPortNumber.getText().trim());
		}
		catch (Exception e)
		{};
		int firewallType = JAPConstants.FIREWALL_TYPE_HTTP;
		if (m_comboProxyType.getSelectedIndex() == 1)
		{
			firewallType = JAPConstants.FIREWALL_TYPE_SOCKS;
		}
		m_Controller.setProxy(firewallType, m_tfProxyHost.getText().trim(), port, m_cbProxy.isSelected());
		m_Controller.setFirewallAuthUserID(m_tfProxyAuthenticationUserID.getText().trim());
		m_Controller.setUseFirewallAuthorization(m_cbProxyAuthentication.isSelected());
		m_Controller.setInfoServiceDisabled(m_cbInfoServiceDisabled.isSelected());
		//Cert seetings
		m_Controller.setCertCheckDisabled(m_cbCertCheckDisabled.isSelected());

		// Anonservice settings
		m_Controller.setAutoConnect(m_cbAutoConnect.isSelected());
		m_Controller.setAutoReConnect(m_cbAutoReConnect.isSelected());
		m_Controller.setMinimizeOnStartup(m_cbStartupMinimized.isSelected());
		m_Controller.setSaveMainWindowPosition(m_cbSaveWindowPositions.isSelected());
		//Try to Set AnonService

		MixCascade newMixCascade = null;
		if (m_rbMixStep3.isSelected())
		{
			// -- do stuff for manual setting of anon service
			try
			{
				newMixCascade = new MixCascade(m_tfMixHost.getText().trim(),
											   Integer.parseInt(m_tfMixPortNumber.getText().trim()));
			}
			catch (Exception ex)
			{
				//Should NEVER happen (because of checked before)
				newMixCascade = m_Controller.getCurrentMixCascade();
			}
		}
		else
		{
			if (m_rbMixStep2.isSelected())
			{
				newMixCascade = (MixCascade) m_comboMixCascade.getSelectedItem();
			}
		}
		if (newMixCascade != null)
		{
			m_Controller.setCurrentMixCascade(newMixCascade);
		}
		// force notifying the observers set the right server name
		m_Controller.notifyJAPObservers(); // this should be the last line of okPressed() !!!
		// ... manual settings stuff finished
	}

	public void selectCard(int selectedCard)
	{
		// set selected card to foreground
		if (selectedCard == PROXY_TAB)
		{
			m_Tabs.setSelectedComponent(m_pFirewall);
		}
		else if (selectedCard == INFO_TAB)
		{
			m_Tabs.setSelectedComponent(m_pInfo);
		}
		else if (selectedCard == ANON_TAB)
		{
			m_Tabs.setSelectedComponent(m_pMix);
		}
		else if (selectedCard == CERT_TAB)
		{
			m_Tabs.setSelectedComponent(m_pCert);
		}
		else if (selectedCard == MISC_TAB)
		{
			m_Tabs.setSelectedComponent(m_pMisc);
		}
		else if (selectedCard == KONTO_TAB && loadPay)
		{
			m_Tabs.setSelectedComponent(m_pKonto);
		}
		else
		{
			m_Tabs.setSelectedComponent(m_pPort);
		}
	}

	public void localeChanged()
	{
		setTitle(JAPMessages.getString("settingsDialog"));
		m_Tabs.setTitleAt(PORT_TAB, JAPMessages.getString("confListenerTab"));
		m_Tabs.setTitleAt(PROXY_TAB, JAPMessages.getString("confProxyTab"));
		m_Tabs.setTitleAt(INFO_TAB, JAPMessages.getString("confInfoTab"));
		m_Tabs.setTitleAt(ANON_TAB, JAPMessages.getString("confAnonTab"));
		if (!JAPModel.isSmallDisplay())
		{
			m_Tabs.setTitleAt(MISC_TAB, JAPMessages.getString("confMiscTab"));
		}
		m_bttnDefaultConfig.setText(JAPMessages.getString("bttnDefaultConfig"));
		m_bttnCancel.setText(JAPMessages.getString("cancelButton"));
		//Anon Panel
		m_cbStartupMinimized.setText(JAPMessages.getString("settingsstartupMinimizeCheckBox"));
		m_cbAutoConnect.setText(JAPMessages.getString("settingsautoConnectCheckBox"));
		m_cbAutoReConnect.setText(JAPMessages.getString("settingsautoReConnectCheckBox"));
		m_rbMixStep1.setText(JAPMessages.getString("settingsAnonRadio1"));
		m_rbMixStep2.setText(JAPMessages.getString("settingsAnonRadio2"));
		m_rbMixStep3.setText(JAPMessages.getString("settingsAnonRadio3"));
		m_bttnFetchCascades.setText(JAPMessages.getString("settingsAnonFetch"));
		m_borderAnonSettings.setTitle(JAPMessages.getString("settingsAnonBorder"));
		m_borderAnonSettings2.setTitle(JAPMessages.getString("settingsAnonBorder2"));
		m_labelAnonHost.setText(JAPMessages.getString("settingsAnonHost"));
		m_labelAnonPort.setText(JAPMessages.getString("settingsAnonPort"));
		//InfoService Panel
		m_pInfo.removeAll();
		m_pInfo.add(JAPConfInfoService.getInstance().createInfoServicePanel(m_fontControls));
		//Port Panel
		m_labelPortnumber1.setText(JAPMessages.getString("settingsPort1"));
		m_labelPortnumber2.setText(JAPMessages.getString("settingsPort2"));
		m_cbListenerIsLocal.setText(JAPMessages.getString("settingsListenerCheckBox"));
		m_borderSettingsListener.setTitle(JAPMessages.getString("settingsListenerBorder"));
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
	}

	public void updateValues()
	{
		if (loadPay)
		{
			( (pay.view.PayView) m_pKonto).userPanel.valuesChanged();
		}
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
		m_cbInfoServiceDisabled.setSelected(JAPModel.isInfoServiceDisabled());
		m_bIgnoreComboLanguageEvents = true;
		if (m_Controller.getLocale().equals(Locale.ENGLISH))
		{
			m_comboLanguage.setSelectedIndex(1);
		}
		else if (m_Controller.getLocale().equals(Locale.FRENCH))
		{
			m_comboLanguage.setSelectedIndex(2);
		}
		else
		{
			m_comboLanguage.setSelectedIndex(0);
		}
		m_bIgnoreComboLanguageEvents = false;
		// listener tab
		m_tfListenerPortNumber.setText(String.valueOf(JAPModel.getHttpListenerPortNumber()));
		m_cbListenerIsLocal.setSelected(JAPModel.getHttpListenerIsLocal());
		//m_tfListenerPortNumberSocks.setText(String.valueOf(m_Controller.getSocksPortNumber()));
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
		// anon tab
		MixCascade mixCascade = m_Controller.getCurrentMixCascade();
		m_strMixName = mixCascade.getName();
		m_strOldMixName = m_strMixName;
		m_tfMixHost.setText(mixCascade.getListenerInterface(0).getHostName());
		m_tfMixPortNumber.setText(Integer.toString(mixCascade.getListenerInterface(0).getPort()));
		m_cbAutoConnect.setSelected(JAPModel.getAutoConnect());
		m_cbAutoReConnect.setSelected(JAPModel.getAutoReConnect());
		m_cbStartupMinimized.setSelected(JAPModel.getMinimizeOnStartup());
		m_cbSaveWindowPositions.setSelected(JAPModel.getSaveMainWindowPosition());
		updateMixCascadeCombo();
		if (m_rbMixStep2.isSelected()) //Auswahl is selected
		{ //try to select the current MixCascade
			m_comboMixCascade.setSelectedItem(mixCascade);
		}
	}

	private void updateMixCascadeCombo()
	{
		LogHolder.log(LogLevel.DEBUG, LogType.GUI, "JAPConf: updateMixCascadeCombo() -start");
		m_comboMixCascade.removeAllItems();
		LogHolder.log(LogLevel.DEBUG, LogType.GUI, "JAPConf: updateMixCascadeCombo() -all ItemsRemoved");
		m_comboMixCascade.addItem(JAPMessages.getString("settingsAnonSelect"));
		LogHolder.log(LogLevel.DEBUG, LogType.GUI, "JAPConf: updateMixCascadeCombo() -added Default Item");
		Enumeration it = m_Controller.getMixCascadeDatabase().elements();
		while (it.hasMoreElements())
		{
			m_comboMixCascade.addItem(it.nextElement());
		}
		LogHolder.log(LogLevel.DEBUG, LogType.GUI, "JAPConf: updateMixCascadeCombo() -added All other Items");
		m_comboMixCascade.setSelectedIndex(0);
		LogHolder.log(LogLevel.DEBUG, LogType.GUI,
					  "JAPConf: updateMixCascadeCombo() - select First Item -- finished!");
	}
}
