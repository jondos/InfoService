/*
Copyright (c) 2000, The JAP-Team
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
import java.util.Enumeration;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Locale;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;

import java.awt.event.*;

import javax.swing.*;

import javax.swing.UIManager.LookAndFeelInfo;

import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

import anon.JAPAnonService;
final class JAPConf extends JDialog
	{

		final static public int PORT_TAB = 0;
		final static public int HTTP_TAB = 1;
		final static public int INFO_TAB = 2;
		final static public int ANON_TAB = 3;
		final static public int MISC_TAB = 4;

		private JAPModel      m_Model;

		private JAPJIntField	m_tfListenerPortNumber;
		//private JAPJIntField	m_tfListenerPortNumberSocks;
		//private JCheckBox			m_cbListenerSocks;
		private JCheckBox			m_cbListenerIsLocal;

		private JCheckBox			m_cbProxy;
		private JAPJIntField	m_tfProxyPortNumber;
		private JTextField		m_tfProxyHost;
		private JCheckBox			m_cbProxyAuthentication;
		private JTextField		m_tfProxyAuthenticationUserID;

		private JCheckBox			m_cbAutoConnect;
		private JCheckBox			m_cbStartupMinimized;

		private JAPJIntField	m_tfMixPortNumber;
		private JAPJIntField	m_tfMixPortNumberSSL;
		private JTextField		m_tfMixHost;
		private String				m_strMixName,m_strOldMixName;
		private JComboBox			m_comboMixCascade;
		private JRadioButton	m_rbMixStep1,m_rbMixStep2,m_rbMixStep3;
		private JButton				m_bttnFetchCascades;

		private JAPJIntField	m_tfInfoPortNumber;
		private JTextField		m_tfInfoHost;

		private JCheckBox     m_cbDebugGui;
		private JCheckBox     m_cbDebugNet;
		private JCheckBox     m_cbDebugThread;
		private JCheckBox     m_cbDebugMisc;
		private JCheckBox     m_cbShowDebugConsole;
		private JSlider				m_sliderDebugLevel;

		private JComboBox			m_comboLanguage;
		private boolean				m_bIgnoreComboLanguageEvents=false;

		private JCheckBox     m_cbDummyTraffic;

		private JTabbedPane		m_Tabs;
		private JPanel				m_pPort, m_pFirewall, m_pInfo, m_pMix, m_pMisc;

		private JFrame        m_frmParent;

		private JAPConf       m_JapConf;

	  public JAPConf (JFrame frmParent)
			{
				super(frmParent);
				m_frmParent=frmParent;
				m_Model = JAPModel.getModel();
				this.setModal(true);
				this.setTitle(JAPMessages.getString("settingsDialog"));
				m_JapConf=this;
				JPanel pContainer = new JPanel();
				pContainer.setLayout( new BorderLayout() );
				m_Tabs = new JTabbedPane();
				m_pPort = buildportPanel();
				m_pFirewall = buildhttpPanel();
				m_pInfo = buildinfoPanel();
				m_pMix = buildanonPanel();
				m_pMisc = buildmiscPanel();
				m_Tabs.addTab( JAPMessages.getString("confListenerTab"), null, m_pPort );
				m_Tabs.addTab( JAPMessages.getString("confProxyTab"), null, m_pFirewall );
				m_Tabs.addTab( JAPMessages.getString("confInfoTab"), null, m_pInfo );
				m_Tabs.addTab( JAPMessages.getString("confAnonTab"), null, m_pMix );
				m_Tabs.addTab( JAPMessages.getString("confMiscTab"), null, m_pMisc );

				JPanel buttonPanel = new JPanel();
				buttonPanel.setLayout ( new FlowLayout(FlowLayout.RIGHT) );
				JButton bttnDefaultConfig=new JButton(JAPMessages.getString("bttnDefaultConfig"));
				bttnDefaultConfig.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent e) {
						resetToDefault();
				}});
				buttonPanel.add(bttnDefaultConfig);
				JButton bttnCancel = new JButton(JAPMessages.getString("cancelButton"));
				bttnCancel.addActionListener(new ActionListener()
					{
						public void actionPerformed(ActionEvent e)
							{
							  CancelPressed();
							}
					});
				buttonPanel.add(bttnCancel );
				JButton ok = new JButton(JAPMessages.getString("okButton"));
				ok.addActionListener(new ActionListener() {
						   public void actionPerformed(ActionEvent e) {
				   OKPressed();
				   }});
				buttonPanel.add( ok );
				buttonPanel.add(new JLabel("   "));
				getRootPane().setDefaultButton(ok);

				pContainer.add(m_Tabs, BorderLayout.CENTER);
				pContainer.add(buttonPanel, BorderLayout.SOUTH);
//				container.add(new JLabel(new ImageIcon(m_Model.JAPICONFN)), BorderLayout.WEST);
				getContentPane().add(pContainer);
				updateValues();
				// largest tab to front
				m_Tabs.setSelectedComponent(m_pMix);
				pack();
//				setResizable(false);
				JAPUtil.centerFrame(this);
			}

		protected JPanel buildportPanel()
			{
				JLabel portnumberLabel1 = new JLabel(JAPMessages.getString("settingsPort1"));
				JLabel portnumberLabel2 = new JLabel(JAPMessages.getString("settingsPort2"));
				m_tfListenerPortNumber = new JAPJIntField();
				m_tfListenerPortNumber.addActionListener(new ActionListener() {
						   public void actionPerformed(ActionEvent e) {
				   OKPressed();
				   }});
				m_cbListenerIsLocal = new JCheckBox(JAPMessages.getString("settingsListenerCheckBox"));
				// set Font in listenerCheckBox in same color as in portnumberLabel1
				m_cbListenerIsLocal.setForeground(portnumberLabel1.getForeground());

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
				   OKPressed();
				   }});
				*/JPanel p = new JPanel();
				p.setLayout( new BorderLayout() );
				p.setBorder( new TitledBorder(JAPMessages.getString("settingsListenerBorder")) );
				JPanel p1 = new JPanel();
				GridBagLayout g=new GridBagLayout();
				p1.setLayout( g );
				p1.setBorder( new EmptyBorder(5,10,10,10) );
				GridBagConstraints c=new GridBagConstraints();
				c.anchor=c.NORTHWEST;
				c.fill=GridBagConstraints.HORIZONTAL;
				c.gridwidth=1;
				c.gridx=0;
				c.gridy=0;
				c.weightx=1;
				c.weighty=0;
				Insets normInsets=new Insets(0,0,3,0);
				c.insets=normInsets;
				g.setConstraints(portnumberLabel1,c);
				p1.add(portnumberLabel1);
				c.gridy=1;
				g.setConstraints(portnumberLabel2,c);
				p1.add(portnumberLabel2);
				c.gridy=2;
				g.setConstraints(m_tfListenerPortNumber,c);
				p1.add(m_tfListenerPortNumber);
				JSeparator seperator=new JSeparator();
				c.gridy=3;
				c.insets=new Insets(10,0,0,0);
				g.setConstraints(seperator,c);
				p1.add(seperator);
				c.insets=normInsets;
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
				c.gridy=4;
				c.insets=new Insets(10,0,0,0);
				g.setConstraints(m_cbListenerIsLocal,c);
				p1.add(m_cbListenerIsLocal);
				p.add(p1, BorderLayout.NORTH);
				return p;
			}

	  protected JPanel buildhttpPanel()
			{
				m_cbProxy = new JCheckBox(JAPMessages.getString("settingsProxyCheckBox"));
				m_tfProxyHost = new JTextField();
				m_tfProxyPortNumber = new JAPJIntField();
				m_tfProxyHost.setEnabled(m_Model.getUseFirewall());
				m_tfProxyPortNumber.setEnabled(m_Model.getUseFirewall());
				m_cbProxy.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						boolean b=m_cbProxy.isSelected();
						m_tfProxyHost.setEnabled(b);
						m_tfProxyPortNumber.setEnabled(b);
						m_cbProxyAuthentication.setEnabled(b);
						m_tfProxyAuthenticationUserID.setEnabled(b);
				}});
				m_tfProxyHost.addActionListener(new ActionListener() {
						   public void actionPerformed(ActionEvent e) {
				   OKPressed();
				   }});
				m_tfProxyPortNumber.addActionListener(new ActionListener() {
						   public void actionPerformed(ActionEvent e) {
				   OKPressed();
				   }});
				m_cbProxyAuthentication=new JCheckBox(JAPMessages.getString("settingsProxyAuthenticationCheckBox"));
				m_tfProxyAuthenticationUserID=new JTextField();

				m_cbProxyAuthentication.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						m_tfProxyAuthenticationUserID.setEnabled(m_cbProxyAuthentication.isSelected());
				}});
				JLabel proxyHostLabel = new JLabel(JAPMessages.getString("settingsProxyHost"));
				JLabel proxyPortLabel = new JLabel(JAPMessages.getString("settingsProxyPort"));
				JLabel proxyAuthUserIDLabel = new JLabel(JAPMessages.getString("settingsProxyAuthUserID"));
				// set Font in m_cbProxy in same color as in proxyPortLabel
				m_cbProxy.setForeground(proxyPortLabel.getForeground());
				m_cbProxyAuthentication.setForeground(proxyPortLabel.getForeground());


				JPanel p = new JPanel();
				p.setLayout( new BorderLayout() );
				p.setBorder( new TitledBorder(JAPMessages.getString("settingsProxyBorder")) );
				JPanel p1 = new JPanel();
				GridBagLayout g=new GridBagLayout();
				p1.setLayout( g );
				p1.setBorder( new EmptyBorder(5,10,10,10) );
				GridBagConstraints c=new GridBagConstraints();
				c.anchor=c.NORTHWEST;
				c.fill=GridBagConstraints.HORIZONTAL;
				c.gridwidth=1;
				c.gridx=0;
				c.gridy=0;
				c.weightx=1;
				c.weighty=0;
				Insets normInsets=new Insets(0,0,3,0);
				c.insets=normInsets;
				g.setConstraints(m_cbProxy,c);
				p1.add(m_cbProxy);
				c.gridy=1;
				g.setConstraints(proxyHostLabel,c);
				p1.add(proxyHostLabel);
				c.gridy=2;
				g.setConstraints(m_tfProxyHost,c);
				p1.add(m_tfProxyHost);
				c.gridy=3;
				g.setConstraints(proxyPortLabel,c);
				p1.add(proxyPortLabel);
				c.gridy=4;
				g.setConstraints(m_tfProxyPortNumber,c);
				p1.add(m_tfProxyPortNumber);
				JSeparator seperator=new JSeparator();
				c.gridy=5;
				c.insets=new Insets(10,0,0,0);
				g.setConstraints(seperator,c);
				p1.add(seperator);
				c.insets=normInsets;
				c.gridy=6;
				c.insets=new Insets(10,0,0,0);
				g.setConstraints(m_cbProxyAuthentication,c);
				p1.add(m_cbProxyAuthentication);
				c.gridy=7;
				g.setConstraints(proxyAuthUserIDLabel,c);
				p1.add(proxyAuthUserIDLabel);
				c.gridy=8;
				g.setConstraints(m_tfProxyAuthenticationUserID,c);
				p1.add(m_tfProxyAuthenticationUserID);
				c.gridy=9;
				p.add(p1, BorderLayout.NORTH);
				return p;
			}

	  protected JPanel buildinfoPanel()
			{
				m_tfInfoHost = new JTextField();
				m_tfInfoPortNumber = new JAPJIntField();
				m_tfInfoHost.addActionListener(new ActionListener() {
									   public void actionPerformed(ActionEvent e) {
							   OKPressed();
							   }});
				m_tfInfoPortNumber.addActionListener(new ActionListener() {
									   public void actionPerformed(ActionEvent e) {
							   OKPressed();
							   }});
				JLabel settingsInfoTextLabel = new JLabel(JAPMessages.getString("settingsInfoText"));
				JLabel settingsInfoHostLabel = new JLabel(JAPMessages.getString("settingsInfoHost"));
				JLabel settingsInfoPortLabel = new JLabel(JAPMessages.getString("settingsInfoPort"));

				JPanel p = new JPanel();
				p.setLayout( new BorderLayout() );
				p.setBorder( new TitledBorder(JAPMessages.getString("settingsInfoBorder")) );
				JPanel p1 = new JPanel();
				GridBagLayout g=new GridBagLayout();
				p1.setLayout( g );
				p1.setBorder( new EmptyBorder(5,10,10,10) );
				GridBagConstraints c=new GridBagConstraints();
				c.anchor=c.NORTHWEST;
				c.fill=GridBagConstraints.HORIZONTAL;
				c.gridwidth=1;
				c.gridx=0;
				c.gridy=0;
				c.weightx=1;
				c.weighty=0;
				Insets normInsets=new Insets(0,0,3,0);
				c.insets=normInsets;
				g.setConstraints(settingsInfoTextLabel,c);
				p1.add(settingsInfoTextLabel);
				c.gridy=1;
				g.setConstraints(settingsInfoHostLabel,c);
				p1.add(settingsInfoHostLabel);
				c.gridy=2;
				g.setConstraints(m_tfInfoHost,c);
				p1.add(m_tfInfoHost);
				c.gridy=3;
				g.setConstraints(settingsInfoPortLabel,c);
				p1.add(settingsInfoPortLabel);
				c.gridy=4;
				g.setConstraints(m_tfInfoPortNumber,c);
				p1.add(m_tfInfoPortNumber);
				c.gridy=5;

				p.add(p1, BorderLayout.NORTH);
				return p;
			}

	  protected JPanel buildanonPanel()
			{
				m_cbStartupMinimized=new JCheckBox(JAPMessages.getString("settingsstartupMinimizeCheckBox"));
				m_cbAutoConnect = new JCheckBox(JAPMessages.getString("settingsautoConnectCheckBox"));
				m_tfMixHost = new JTextField();
				m_tfMixPortNumber = new JAPJIntField();
				m_tfMixPortNumberSSL = new JAPJIntField();
				m_tfMixHost.setEditable(false);
				m_tfMixPortNumber.setEditable(false);
				m_tfMixPortNumberSSL.setEditable(false);
				m_tfMixHost.addActionListener(new ActionListener() {
									   public void actionPerformed(ActionEvent e) {
							   OKPressed();
							   }});
				m_tfMixPortNumber.addActionListener(new ActionListener() {
									   public void actionPerformed(ActionEvent e) {
							   OKPressed();
							   }});
				m_tfMixPortNumberSSL.addActionListener(new ActionListener() {
									   public void actionPerformed(ActionEvent e) {
							   OKPressed();
							   }});
				ButtonGroup bg = new ButtonGroup();
				m_rbMixStep1 = new JRadioButton(JAPMessages.getString("settingsAnonRadio1"), true);
				m_rbMixStep2 = new JRadioButton(JAPMessages.getString("settingsAnonRadio2"));
				m_rbMixStep3 = new JRadioButton(JAPMessages.getString("settingsAnonRadio3"));
				m_bttnFetchCascades = new JButton(JAPMessages.getString("settingsAnonFetch"));
				m_bttnFetchCascades.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
					JAPDebug.out(JAPDebug.DEBUG,JAPDebug.GUI,"JAPConf:m_bttnFetchCascades");
//						JOptionPane.showMessageDialog(null,JAPMessages.getString("notYetImlmplemented"));
						// fetch available mix cascades from the Internet
						Cursor c=getCursor();
						setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
						m_Model.fetchAnonServers();
						if (m_Model.anonServerDatabase.size() == 0) {
							setCursor(c);
							JOptionPane.showMessageDialog(m_Model.getView(),
											JAPMessages.getString("settingsNoServersAvailable"),
											JAPMessages.getString("settingsNoServersAvailableTitle"),
											JOptionPane.INFORMATION_MESSAGE);
						} else {
							// show a window containing all available cascades
							//JAPCascadeMonitorView v=new JAPCascadeMonitorView(m_Model.getView());
							// ------ !!!!! die folgenden zwei zeilen auskommentieren, wenn JAPCascadeMonitorView
							// ------ !!!!! ordentlich geht!!!!
							updateValues();
							m_rbMixStep2.doClick();

							setCursor(c);
						}
						// ------ !!!!! diese wieder aktivieren!
						//OKPressed();
				}});
				m_comboMixCascade = new JComboBox();
				// add elements to combobox
				m_comboMixCascade.addItem(JAPMessages.getString("settingsAnonSelect"));
				Enumeration enum = m_Model.anonServerDatabase.elements();
				while (enum.hasMoreElements())
					{
						m_comboMixCascade.addItem( ((AnonServerDBEntry)enum.nextElement()).getName() );
					}

				m_comboMixCascade.setEnabled(false);
				m_comboMixCascade.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent e) {
						JAPDebug.out(JAPDebug.DEBUG,JAPDebug.GUI,"JAPConf:Item " + m_comboMixCascade.getSelectedIndex() + " selected");
						if (m_comboMixCascade.getSelectedIndex() > 0)
						{
							AnonServerDBEntry entry=m_Model.anonServerDatabase.getEntry(m_comboMixCascade.getSelectedIndex()-1);
							if(entry!=null)
								{
									m_strMixName = entry.getName();
									m_strOldMixName = m_strMixName;
									m_tfMixHost.setText(entry.getHost());
									m_tfMixPortNumber.setText(Integer.toString(entry.getPort()));
									int i = entry.getSSLPort();
									if (i == -1)
										m_tfMixPortNumberSSL.setText("");
									else
										m_tfMixPortNumberSSL.setText(Integer.toString(i));
								}
						}
						}});
				bg.add(m_rbMixStep1);
				bg.add(m_rbMixStep2);
				bg.add(m_rbMixStep3);
				m_rbMixStep1.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						JAPDebug.out(JAPDebug.DEBUG,JAPDebug.GUI,"JAPConf:m_rbMixStep1 selected");
						m_bttnFetchCascades.setEnabled(true);
						m_comboMixCascade.setEnabled(false);
						m_tfMixHost.setEditable(false);
						m_tfMixPortNumber.setEditable(false);
						m_tfMixPortNumberSSL.setEditable(false);
				}});
				m_rbMixStep2.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						JAPDebug.out(JAPDebug.DEBUG,JAPDebug.GUI,"JAPConf:m_rbMixStep2 selected");
						m_bttnFetchCascades.setEnabled(false);
						m_comboMixCascade.setEnabled(true);
						m_comboMixCascade.setPopupVisible(true);
						m_tfMixHost.setEditable(false);
						m_tfMixPortNumber.setEditable(false);
						m_tfMixPortNumberSSL.setEditable(false);

				}});
				m_rbMixStep3.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						JAPDebug.out(JAPDebug.DEBUG,JAPDebug.GUI,"JAPConf:m_rbMixStep3 selected");
						m_bttnFetchCascades.setEnabled(false);
						m_comboMixCascade.setEnabled(false);
						m_tfMixHost.setEditable(true);
						m_tfMixPortNumber.setEditable(true);
						m_tfMixPortNumberSSL.setEditable(true);
						m_strMixName = JAPMessages.getString("manual");
				}});

				// layout stuff
				JPanel p=new JPanel();
				p.setLayout(new BorderLayout() );
				// Upper panel
				JPanel pp1 = new JPanel();
				pp1.setLayout( new BorderLayout() );
				pp1.setBorder( new TitledBorder(JAPMessages.getString("settingsAnonBorder")) );
				// Lower panel
				JPanel pp2 = new JPanel();
				pp2.setLayout( new BorderLayout() );
				pp2.setBorder( new TitledBorder(JAPMessages.getString("settingsAnonBorder2")) );
				// Upper panel content
				JPanel p1 = new JPanel();
				p1.setLayout( new GridLayout(2,1) );
				//p1.setBorder( new EmptyBorder(5,10,10,10) );
				// 1
				JPanel p11 = new JPanel();
				p11.setLayout(new BoxLayout(p11, BoxLayout.X_AXIS));
				p11.add(m_rbMixStep1);
				p11.add(Box.createRigidArea(new Dimension(5,0)) );
				p11.add(Box.createHorizontalGlue() );
				p11.add(m_bttnFetchCascades);
				p1.add(p11);
				// 2
				JPanel p12 = new JPanel();
				p12.setLayout(new BoxLayout(p12, BoxLayout.X_AXIS));
				p12.add(m_rbMixStep2);
				p12.add(Box.createRigidArea(new Dimension(5,0)) );
				p12.add(Box.createHorizontalGlue() );
				p12.add(m_comboMixCascade);
				p1.add(p12);
				// Lower Panel content
				JPanel p2 = new JPanel();
				p2.setLayout( new GridLayout(8,1) );
				//p1.setBorder( new EmptyBorder(5,10,10,10) );
				//
				p2.add(m_rbMixStep3);
				p2.add(new JLabel(JAPMessages.getString("settingsAnonHost")));
				p2.add(m_tfMixHost);
				p2.add(new JLabel(JAPMessages.getString("settingsAnonPort")));
				p2.add(m_tfMixPortNumber);
				p2.add(new JLabel(JAPMessages.getString("settingsAnonSSLPort")));
				p2.add(m_tfMixPortNumberSSL);
				//
				p2.add(m_cbAutoConnect);
				//p2.add(m_cbStartupMinimized);
				// Add contents to upper and lower panel
				pp1.add(p1);
				pp2.add(p2);
				// Add to main panel
				p.add(pp1, BorderLayout.NORTH);
				p.add(pp2, BorderLayout.CENTER);
				return p;
			}

		protected JPanel buildmiscPanel()
			{
				JPanel p=new JPanel();
				p.setLayout(new BorderLayout() );

				// Panel for Look and Feel Options
				JPanel p1=new JPanel();
				p1.setLayout(new GridLayout(2,2));
				p1.setBorder( new TitledBorder(JAPMessages.getString("settingsLookAndFeelBorder")) );
				p1.add(new JLabel(JAPMessages.getString("settingsLookAndFeel")));
				JComboBox c=new JComboBox();
				LookAndFeelInfo[] lf=UIManager.getInstalledLookAndFeels();
				String currentLf=UIManager.getLookAndFeel().getName().toString();
				// add menu items
				for(int lfidx=0;lfidx<lf.length;lfidx++) {
					c.addItem(lf[lfidx].getName());
				}
				// select the current
				int lfidx;
				for(lfidx=0;lfidx<lf.length;lfidx++) {
					if(lf[lfidx].getName().equals(currentLf)) {
						c.setSelectedIndex(lfidx);
						break;
					}
				}
				if ( !(lfidx<lf.length) ) {
					c.addItem("(unknown)");
					c.setSelectedIndex(lfidx);
				}
				c.addItemListener(new ItemListener(){
					public void itemStateChanged(ItemEvent e){
						if(e.getStateChange()==e.SELECTED) {
								try {
									UIManager.setLookAndFeel(UIManager.getInstalledLookAndFeels()[((JComboBox)e.getItemSelectable()).getSelectedIndex()].getClassName());
//									SwingUtilities.updateComponentTreeUI(m_frmParent);
//									SwingUtilities.updateComponentTreeUI(SwingUtilities.getRoot(((JComboBox)e.getItemSelectable())));
									JOptionPane.showMessageDialog(m_JapConf,JAPMessages.getString("confLookAndFeelChanged"),JAPMessages.getString("information"),JOptionPane.INFORMATION_MESSAGE);
								} catch(Exception ie) {
								}
						}
					}});
				p1.add(c);
				p1.add(new JLabel(JAPMessages.getString("settingsLanguage")));
				m_comboLanguage=new JComboBox();
				m_comboLanguage.addItem("Deutsch");
				m_comboLanguage.addItem("English");
				m_comboLanguage.addItemListener(new ItemListener(){
					public void itemStateChanged(ItemEvent e){
						if(!m_bIgnoreComboLanguageEvents&&e.getStateChange()==e.SELECTED) {
							try {
								JOptionPane.showMessageDialog(m_JapConf,JAPMessages.getString("confLanguageChanged"),JAPMessages.getString("information"),JOptionPane.INFORMATION_MESSAGE);
							} catch(Exception ie) {
							}
						}
					}});

				p1.add(m_comboLanguage);

				// Panel for Misc Options
				JPanel p2=new JPanel();
				p2.setLayout(new BorderLayout());
				p2.setBorder( new TitledBorder(JAPMessages.getString("miscconfigBorder")) );
				JButton bttnPing=new JButton(JAPMessages.getString("bttnPing"));
/*				bttnPing.addActionListener(new ActionListener()
					{
						public void actionPerformed(ActionEvent e)
							{
								AnonServerDBEntry[] a=new AnonServerDBEntry[1];
//								a[0]=new AnonServerDBEntry(m_Model.anonHostName,m_Model.anonHostName,m_Model.anonPortNumber+1);
								a[0]=new AnonServerDBEntry(m_Model.getAnonServer().getHost(),m_Model.getAnonServer().getHost(),m_Model.getAnonServer().getPort()+1);
								JAPRoundTripTimeView v=new JAPRoundTripTimeView(m_Model.getView(),a);
//								v.show();
							}
					});
*/				JButton bttnMonitor=new JButton(JAPMessages.getString("bttnMonitor"));
//				bttnMonitor.setEnabled(false);
				  bttnMonitor.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						Cursor c1=getCursor();
						setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
						m_Model.fetchAnonServers();
						JAPCascadeMonitorView v=new JAPCascadeMonitorView(m_Model.getView());
						updateValues();
						OKPressed();
						setCursor(c1);

				}});
				m_cbDummyTraffic=new JCheckBox("Dummy Traffic");
				JPanel p22 = new JPanel();
				p22.setLayout(new GridLayout(3,1));
				//p22.add(bttnPing);
				p22.add(bttnMonitor);
				p22.add(m_cbDummyTraffic);
				p2.add(p22, BorderLayout.NORTH);

				// Panel for Debugging Options
				JPanel p3=new JPanel();
				p3.setLayout( new GridLayout(1,2));
				p3.setBorder( new TitledBorder("Debugging") );
				JPanel p31=new JPanel(new GridLayout(0,1));
				m_cbDebugGui = new JCheckBox("GUI");
				m_cbDebugNet = new JCheckBox("NET");
				m_cbDebugThread = new JCheckBox("THREAD");
				m_cbDebugMisc = new JCheckBox("MISC");
				p31.add(m_cbDebugGui);
				p31.add(m_cbDebugNet);
				p31.add(m_cbDebugThread);
				p31.add(m_cbDebugMisc);

				m_cbShowDebugConsole=new JCheckBox("Show Console");
				m_cbShowDebugConsole.setSelected(JAPDebug.isShowConsole());
				m_cbShowDebugConsole.addItemListener(new ItemListener()
					{public void itemStateChanged(ItemEvent e)
					 {
						 JAPDebug.showConsole(e.getStateChange()==e.SELECTED,m_Model.getView());
					}});
				p31.add(m_cbShowDebugConsole);

				p3.add(p31);
				JPanel p32=new JPanel();
				m_sliderDebugLevel=new JSlider(JSlider.VERTICAL,0,7,0);
				m_sliderDebugLevel.addChangeListener(new ChangeListener()
					{public void stateChanged(ChangeEvent e)
					 {Dictionary d=m_sliderDebugLevel.getLabelTable();
						for(int i=0;i<8;i++)
							((JLabel)d.get(new Integer(i))).setEnabled(i<=m_sliderDebugLevel.getValue());
					}});
				String debugLevels[]=JAPDebug.getDebugLevels();
				Hashtable ht=new Hashtable(debugLevels.length,1.0f);
				for(int i=0;i<debugLevels.length;i++)
					{
						ht.put(new Integer(i),new JLabel(" "+debugLevels[i]));
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

				JPanel pp = new JPanel( new BorderLayout() );
				pp.add(p1, BorderLayout.NORTH);
				pp.add(p2, BorderLayout.CENTER);

				p.add(p3, BorderLayout.WEST);
				p.add(pp, BorderLayout.CENTER);

				return p;
			}

	  protected void CancelPressed()
			{
				setVisible(false);
		  }

		/**Shows a Dialog about whats going wrong
		 */
		private void showError(String msg)
			{
				JOptionPane.showMessageDialog(this,msg,JAPMessages.getString("ERROR"),JOptionPane.ERROR_MESSAGE);
			}

		/** Checks if all Input in all Fiels make sense. Displays InfoBoxes about what is wrong.
		 * @return true if all is ok
		 *					false otherwise
		 */
		private boolean checkValues()
			{
				String s=null;
				int iListenerPort,i;
				//Checking InfoService (Host + Port)
				s=m_tfInfoHost.getText().trim();
				if(s==null||s.equals(""))
					{
						showError(JAPMessages.getString("errorInfoServiceHostNotNull"));
						return false;
					}
				try
					{
						i=Integer.parseInt(m_tfInfoPortNumber.getText().trim());
					}
				catch(Exception e)
					{
						i=-1;
					}
				if(!JAPUtil.isPort(i))
					{
						showError(JAPMessages.getString("errorInfoServicePortWrong"));
						return false;
					}

				//Checking First Mix (Host + Port)
				s=m_tfMixHost.getText().trim();
				if(s==null||s.equals(""))
					{
						showError(JAPMessages.getString("errorAnonHostNotNull"));
						return false;
					}
				try
					{
						i=Integer.parseInt(m_tfMixPortNumber.getText().trim());
					}
				catch(Exception e)
					{
						i=-1;
					}
				if(!JAPUtil.isPort(i))
					{
						showError(JAPMessages.getString("errorAnonServicePortWrong"));
						return false;
					}
				//--------------
				if (m_tfMixPortNumberSSL.getText().trim().equals("")) {
					;
				} else {
					try {
						i=Integer.parseInt(m_tfMixPortNumberSSL.getText().trim());
					}
					catch(Exception e) {
						i=-1;
					}
					if(!JAPUtil.isPort(i)) {
						showError(JAPMessages.getString("errorAnonServicePortWrong"));
						return false;
					}
				}
				//checking Listener Port Number
				try
					{
						i=Integer.parseInt(m_tfListenerPortNumber.getText().trim());
					}
				catch(Exception e)
					{
						i=-1;
					}
				if(!JAPUtil.isPort(i))
					{
						showError(JAPMessages.getString("errorListenerPortWrong"));
						return false;
					}
				iListenerPort=i;
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
				if(m_cbProxy.isSelected())
					{
						s=m_tfProxyHost.getText().trim();
						if(s==null||s.equals(""))
							{
								showError(JAPMessages.getString("errorFirewallHostNotNull"));
								return false;
							}
						try
							{
								i=Integer.parseInt(m_tfProxyPortNumber.getText().trim());
							}
						catch(Exception e)
							{
								i=-1;
							}
						if(!JAPUtil.isPort(i))
							{
								showError(JAPMessages.getString("errorFirewallServicePortWrong"));
								return false;
							}
						if(m_cbProxyAuthentication.isSelected())
							{
								s=m_tfProxyAuthenticationUserID.getText().trim();
								if(s==null||s.equals(""))
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
				if(i<0||i>JAPDebug.DEBUG)
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
				m_tfListenerPortNumber.setText(Integer.toString(JAPConstants.defaultPortNumber));
				m_tfInfoHost.setText(JAPConstants.defaultinfoServiceHostName);
				m_tfInfoPortNumber.setText(Integer.toString(JAPConstants.defaultinfoServicePortNumber));
				m_tfMixHost.setText(JAPConstants.defaultanonHost);
				m_tfMixPortNumber.setText(Integer.toString(JAPConstants.defaultanonPortNumber));
				m_cbProxy.setSelected(false);
				m_cbStartupMinimized.setSelected(false);
				m_cbAutoConnect.setSelected(false);
				m_cbListenerIsLocal.setSelected(true);
				//m_cbListenerSocks.setSelected(false);
				m_cbShowDebugConsole.setSelected(false);
				m_sliderDebugLevel.setValue(JAPDebug.EMERG);
				m_cbDebugNet.setSelected(false);
				m_cbDebugGui.setSelected(false);
				m_cbDebugMisc.setSelected(false);
				m_cbDebugThread.setSelected(false);
			}

	  protected void OKPressed()
			{
				if(!checkValues())
					return;
				setVisible(false);
				// Misc settings
				JAPDebug.setDebugType(
					 (m_cbDebugGui.isSelected()?JAPDebug.GUI:JAPDebug.NUL)+
					 (m_cbDebugNet.isSelected()?JAPDebug.NET:JAPDebug.NUL)+
					 (m_cbDebugThread.isSelected()?JAPDebug.THREAD:JAPDebug.NUL)+
					 (m_cbDebugMisc.isSelected()?JAPDebug.MISC:JAPDebug.NUL)
					);
				JAPDebug.setDebugLevel(m_sliderDebugLevel.getValue());
				JAPDebug.out(JAPDebug.DEBUG,JAPDebug.GUI,"m_comboLanguage: "+Integer.toString(m_comboLanguage.getSelectedIndex()));
				if(m_comboLanguage.getSelectedIndex()==0)
					m_Model.setLocale(Locale.GERMAN);
				else
					m_Model.setLocale(Locale.ENGLISH);
				JAPAnonService.setEnableDummyTraffic(m_cbDummyTraffic.isSelected());
				// Listener settings
				m_Model.setHTTPListenerConfig(Integer.parseInt(m_tfListenerPortNumber.getText().trim()),m_cbListenerIsLocal.isSelected());
//				m_Model.setUseSocksPort(m_cbListenerSocks.isSelected());
//				m_Model.setSocksPortNumber(Integer.parseInt(m_tfListenerPortNumberSocks.getText().trim()));
				// Firewall settings
				int port=-1;
				try{port=Integer.parseInt(m_tfProxyPortNumber.getText().trim());}catch(Exception e){};
				m_Model.setProxy(m_tfProxyHost.getText().trim(),port);
				m_Model.setUseProxy(m_cbProxy.isSelected());
				m_Model.setFirewallAuthUserID(m_tfProxyAuthenticationUserID.getText().trim());
				m_Model.setUseFirewallAuthorization(m_cbProxyAuthentication.isSelected());
				// Infoservice settings
				m_Model.setInfoService(m_tfInfoHost.getText().trim(),Integer.parseInt(m_tfInfoPortNumber.getText().trim()));
				// Anonservice settings
				m_Model.autoConnect = m_cbAutoConnect.isSelected();
				m_Model.setMinimizeOnStartup(m_cbStartupMinimized.isSelected());
				int anonSSLPortNumber = -1;
				if (!m_tfMixPortNumberSSL.getText().equals(""))
					anonSSLPortNumber = Integer.parseInt(m_tfMixPortNumberSSL.getText().trim());
				// -- do stuff for manual setting of anon service
				if (m_rbMixStep3.isSelected())
						m_strMixName = JAPMessages.getString("manual");
				AnonServerDBEntry e = new AnonServerDBEntry(
															m_strMixName,
															m_tfMixHost.getText().trim(),
															Integer.parseInt(m_tfMixPortNumber.getText().trim()),
															anonSSLPortNumber);
				// -- if (the same server) (re)set the name from "manual" to the correct name
				if (m_Model.getAnonServer().equals(e))
					e.setName(m_strOldMixName);
				m_Model.setAnonServer(e);
				// force setting the correct name of the selected server
				m_Model.getAnonServer().setName(e.getName());
				// force notifying the observers set the right server name
				m_Model.notifyJAPObservers(); // this should be the last line of OKPressed() !!!
				// ... manual settings stuff finished
			}

	  public void selectCard(int selectedCard)
			{
				// set selected card to foreground
				if (selectedCard == HTTP_TAB)
					m_Tabs.setSelectedComponent(m_pFirewall);
				else if (selectedCard == INFO_TAB)
					m_Tabs.setSelectedComponent(m_pInfo);
				else if (selectedCard == ANON_TAB)
					m_Tabs.setSelectedComponent(m_pMix);
				else if (selectedCard == MISC_TAB)
					m_Tabs.setSelectedComponent(m_pMisc);
				else
					m_Tabs.setSelectedComponent(m_pPort);
			}

		public void updateValues()
			{
				// misc tab
				m_cbDummyTraffic.setSelected(JAPAnonService.getEnableDummyTraffic());
				m_cbShowDebugConsole.setSelected(JAPDebug.isShowConsole());
				m_cbDebugGui.setSelected((((JAPDebug.getDebugType()&JAPDebug.GUI)!=0)?true:false));
				m_cbDebugNet.setSelected((((JAPDebug.getDebugType()&JAPDebug.NET)!=0)?true:false));
				m_cbDebugThread.setSelected((((JAPDebug.getDebugType()&JAPDebug.THREAD)!=0)?true:false));
				m_cbDebugMisc.setSelected((((JAPDebug.getDebugType()&JAPDebug.MISC)!=0)?true:false));
				m_sliderDebugLevel.setValue(JAPDebug.getDebugLevel());
				m_bIgnoreComboLanguageEvents=true;
				if(m_Model.getLocale().equals(Locale.ENGLISH))
					m_comboLanguage.setSelectedIndex(1);
				else
					m_comboLanguage.setSelectedIndex(0);
				m_bIgnoreComboLanguageEvents=false;
				// listener tab
				m_tfListenerPortNumber.setText(String.valueOf(m_Model.getHTTPListenerPortNumber()));
				m_cbListenerIsLocal.setSelected(m_Model.getHTTPListenerIsLocal());
				//m_tfListenerPortNumberSocks.setText(String.valueOf(m_Model.getSocksPortNumber()));
				//m_cbListenerSocks.setSelected(m_Model.getUseSocksPort());
				// firewall tab
				m_cbProxy.setSelected(m_Model.getUseFirewall());
				m_tfProxyHost.setEnabled(m_cbProxy.isSelected());
				m_tfProxyPortNumber.setEnabled(m_cbProxy.isSelected());
				m_tfProxyHost.setText(m_Model.getFirewallHost());
				m_tfProxyPortNumber.setText(String.valueOf(m_Model.getFirewallPort()));
				m_tfProxyAuthenticationUserID.setText(m_Model.getFirewallAuthUserID());
				m_cbProxyAuthentication.setSelected(m_Model.getUseFirewallAuthorization());
				// infoservice tab
				m_tfInfoHost.setText(m_Model.getInfoServiceHost());
				m_tfInfoPortNumber.setText(String.valueOf(m_Model.getInfoServicePort()));
				// anon tab
				AnonServerDBEntry e = m_Model.getAnonServer();
				m_strMixName = e.getName();
				m_strOldMixName = m_strMixName;
				m_tfMixHost.setText(e.getHost());
				m_tfMixPortNumber.setText(String.valueOf(e.getPort()));
				if (e.getSSLPort()==-1)
					m_tfMixPortNumberSSL.setText("");
				else
					m_tfMixPortNumberSSL.setText(String.valueOf(e.getSSLPort()));
				m_comboMixCascade.setSelectedIndex(0);
				m_cbAutoConnect.setSelected(m_Model.autoConnect);
				m_cbStartupMinimized.setSelected(m_Model.getMinimizeOnStartup());
				m_comboMixCascade.removeAllItems();
				m_comboMixCascade.addItem(JAPMessages.getString("settingsAnonSelect"));
				Enumeration enum = m_Model.anonServerDatabase.elements();
				while (enum.hasMoreElements())
					{
						m_comboMixCascade.addItem( ((AnonServerDBEntry)enum.nextElement()).getName() );
					}
		  }

	}

