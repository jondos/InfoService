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
package jap;

import java.util.Enumeration;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Locale;

import java.awt.BorderLayout;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Font;
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
import javax.swing.JFrame;

import anon.AnonServer;
import update.*;
final class JAPConf extends JDialog
	{

		final static public int PORT_TAB = 0;
		final static public int PROXY_TAB = 1;
		final static public int INFO_TAB = 2;
		final static public int ANON_TAB = 3;
		final static public int MISC_TAB = 4;

		private JAPController      m_Controller;

		private JAPJIntField	m_tfListenerPortNumber;
		private JCheckBox			m_cbListenerIsLocal;
		private JLabel        m_labelPortnumber1,m_labelPortnumber2;
		private TitledBorder	m_borderSettingsListener;

		private JCheckBox			m_cbProxy;
		private JAPJIntField	m_tfProxyPortNumber;
		private JTextField		m_tfProxyHost;
    private JComboBox     m_comboProxyType;
    private JCheckBox			m_cbProxyAuthentication;
		private JTextField		m_tfProxyAuthenticationUserID;
    private JLabel				m_labelProxyHost,m_labelProxyPort,m_labelProxyType,m_labelProxyAuthUserID;

		private JCheckBox			m_cbAutoConnect;
		private JCheckBox			m_cbAutoReConnect;
		private JCheckBox			m_cbStartupMinimized;

		private JAPJIntField	m_tfMixPortNumber;
		private JAPJIntField	m_tfMixPortNumberSSL;
		private JTextField		m_tfMixHost;
		private String				m_strMixName,m_strOldMixName;
		private JComboBox			m_comboMixCascade;
		private JRadioButton	m_rbMixStep1,m_rbMixStep2,m_rbMixStep3;
		private JButton				m_bttnFetchCascades;
    private TitledBorder  m_borderAnonSettings,m_borderAnonSettings2;
    private JLabel        m_labelAnonHost,m_labelAnonPort,m_labelAnonProxyPort;

    private JAPJIntField	m_tfInfoPortNumber;
		private JTextField		m_tfInfoHost;
    private JLabel				m_labelSettingsInfoText,m_labelSettingsInfoHost,m_labelSettingsInfoPort;
    private TitledBorder  m_borderInfoService;

		private JCheckBox     m_cbDebugGui;
		private JCheckBox     m_cbDebugNet;
		private JCheckBox     m_cbDebugThread;
		private JCheckBox     m_cbDebugMisc;
		private JCheckBox     m_cbShowDebugConsole;
		private JCheckBox			m_cbInfoServiceDisabled;
		private JSlider				m_sliderDebugLevel;

		private JComboBox			m_comboLanguage;
		private boolean				m_bIgnoreComboLanguageEvents=false;

		private JCheckBox     m_cbDummyTraffic;
		private JSlider				m_sliderDummyTrafficIntervall;

		private JTabbedPane		m_Tabs;
		private JPanel				m_pPort, m_pFirewall, m_pInfo, m_pMix, m_pMisc;
    private JButton       m_bttnDefaultConfig,m_bttnCancel;

		private JFrame        m_frmParent;

		private JAPConf       m_JapConf;

    private Font          m_fontControls;
    //Einfug
    private JAPUpdate update;

	  public JAPConf (JFrame frmParent)
			{
				super(frmParent);
				m_frmParent=frmParent;
				m_Controller = JAPController.getController();
				setModal(true);
				setTitle(JAPMessages.getString("settingsDialog"));
				m_JapConf=this;
				JPanel pContainer = new JPanel();
  			m_Tabs = new JTabbedPane();
	      m_fontControls=JAPController.getDialogFont();
				pContainer.setLayout( new BorderLayout() );
				m_Tabs.setFont(m_fontControls);
        m_pPort = buildPortPanel();
				m_pFirewall = buildProxyPanel();
				m_pInfo = buildInfoServicePanel();
				m_pMix = buildAnonPanel();
				m_pMisc = buildMiscPanel();
				m_Tabs.addTab( JAPMessages.getString("confListenerTab"), null, m_pPort );
				m_Tabs.addTab( JAPMessages.getString("confProxyTab"), null, m_pFirewall );
				m_Tabs.addTab( JAPMessages.getString("confInfoTab"), null, m_pInfo );
				m_Tabs.addTab( JAPMessages.getString("confAnonTab"), null, m_pMix );
				if(!JAPModel.isSmallDisplay())
          m_Tabs.addTab( JAPMessages.getString("confMiscTab"), null, m_pMisc );

				JPanel buttonPanel = new JPanel();
				buttonPanel.setLayout ( new FlowLayout(FlowLayout.RIGHT) );
				m_bttnDefaultConfig=new JButton(JAPMessages.getString("bttnDefaultConfig"));
				m_bttnDefaultConfig.setFont(m_fontControls);
        m_bttnDefaultConfig.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent e) {
						resetToDefault();
				}});
				if(!JAPModel.isSmallDisplay())
          buttonPanel.add(m_bttnDefaultConfig);
				m_bttnCancel = new JButton(JAPMessages.getString("cancelButton"));
				m_bttnCancel.setFont(m_fontControls);
        m_bttnCancel.addActionListener(new ActionListener()
					{
						public void actionPerformed(ActionEvent e)
							{
							  cancelPressed();
							}
					});
				buttonPanel.add(m_bttnCancel );
        JButton ok = new JButton(JAPMessages.getString("okButton"));
				ok.addActionListener(new ActionListener() {
						   public void actionPerformed(ActionEvent e) {
				   okPressed();
				   }});
        ok.setFont(m_fontControls);
				buttonPanel.add( ok );
				buttonPanel.add(new JLabel("   "));
				getRootPane().setDefaultButton(ok);

				pContainer.add(m_Tabs, BorderLayout.CENTER);
				pContainer.add(buttonPanel, BorderLayout.SOUTH);
//				container.add(new JLabel(new ImageIcon(m_Controller.JAPICONFN)), BorderLayout.WEST);
				getContentPane().add(pContainer);
				//updateValues();
				// largest tab to front
				m_Tabs.setSelectedComponent(m_pMix);
				if(JAPModel.isSmallDisplay())
          setSize(240,320);
        else
          pack();
//				setResizable(false);
				JAPUtil.centerFrame(this);
			}

		protected JPanel buildPortPanel()
			{
				m_labelPortnumber1 = new JLabel(JAPMessages.getString("settingsPort1"));
				m_labelPortnumber1.setFont(m_fontControls);
        m_labelPortnumber2 = new JLabel(JAPMessages.getString("settingsPort2"));
				m_labelPortnumber2.setFont(m_fontControls);
 				m_tfListenerPortNumber = new JAPJIntField();
				m_tfListenerPortNumber.setFont(m_fontControls);
        m_tfListenerPortNumber.addActionListener(new ActionListener() {
						   public void actionPerformed(ActionEvent e) {
				   okPressed();
				   }});
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
				   }});
				*/JPanel p = new JPanel();
				p.setLayout( new BorderLayout() );
				m_borderSettingsListener=new TitledBorder(JAPMessages.getString("settingsListenerBorder"));
        m_borderSettingsListener.setTitleFont(m_fontControls);
        p.setBorder( m_borderSettingsListener );
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
				g.setConstraints(m_labelPortnumber1,c);
				p1.add(m_labelPortnumber1);
				c.gridy=1;
				g.setConstraints(m_labelPortnumber2,c);
				p1.add(m_labelPortnumber2);
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

	  protected JPanel buildProxyPanel()
			{
				m_cbProxy = new JCheckBox(JAPMessages.getString("settingsProxyCheckBox"));
				m_cbProxy.setFont(m_fontControls);
        m_comboProxyType=new JComboBox();
        m_comboProxyType.setFont(m_fontControls);
        m_comboProxyType.addItem(JAPMessages.getString("settingsProxyTypeHTTP"));
        m_comboProxyType.addItem(JAPMessages.getString("settingsProxyTypeSOCKS"));
        m_tfProxyHost = new JTextField();
				m_tfProxyHost.setFont(m_fontControls);
        m_tfProxyPortNumber = new JAPJIntField();
				m_tfProxyPortNumber.setFont(m_fontControls);
        m_tfProxyHost.setEnabled(JAPModel.getUseFirewall());
				m_tfProxyPortNumber.setEnabled(JAPModel.getUseFirewall());
				m_cbProxy.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						boolean b=m_cbProxy.isSelected();
						m_comboProxyType.setEnabled(b);
            m_tfProxyHost.setEnabled(b);
						m_tfProxyPortNumber.setEnabled(b);
						m_cbProxyAuthentication.setEnabled(b);
						m_tfProxyAuthenticationUserID.setEnabled(b);
				}});
				m_tfProxyHost.addActionListener(new ActionListener() {
						   public void actionPerformed(ActionEvent e) {
				   okPressed();
				   }});
				m_tfProxyPortNumber.addActionListener(new ActionListener() {
						   public void actionPerformed(ActionEvent e) {
				   okPressed();
				   }});
				m_cbProxyAuthentication=new JCheckBox(JAPMessages.getString("settingsProxyAuthenticationCheckBox"));
				m_cbProxyAuthentication.setFont(m_fontControls);
        m_tfProxyAuthenticationUserID=new JTextField();
        m_tfProxyAuthenticationUserID.setFont(m_fontControls);
				m_cbProxyAuthentication.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						m_tfProxyAuthenticationUserID.setEnabled(m_cbProxyAuthentication.isSelected());
				}});
				m_labelProxyHost = new JLabel(JAPMessages.getString("settingsProxyHost"));
				m_labelProxyHost.setFont(m_fontControls);
        m_labelProxyPort = new JLabel(JAPMessages.getString("settingsProxyPort"));
				m_labelProxyPort.setFont(m_fontControls);
        m_labelProxyType =new JLabel(JAPMessages.getString("settingsProxyType"));
        m_labelProxyType.setFont(m_fontControls);
        m_labelProxyAuthUserID = new JLabel(JAPMessages.getString("settingsProxyAuthUserID"));
				m_labelProxyAuthUserID.setFont(m_fontControls);
        // set Font in m_cbProxy in same color as in proxyPortLabel
				m_cbProxy.setForeground(m_labelProxyPort.getForeground());
				m_cbProxyAuthentication.setForeground(m_labelProxyPort.getForeground());


				JPanel p = new JPanel();
				p.setLayout( new BorderLayout() );
        TitledBorder border=new TitledBorder(JAPMessages.getString("settingsProxyBorder"));
				border.setTitleFont(m_fontControls);
        p.setBorder( border );
				JPanel p1 = new JPanel();
				GridBagLayout g=new GridBagLayout();
				p1.setLayout( g );
				if(JAPModel.isSmallDisplay())
          p1.setBorder( new EmptyBorder(1,10,1,10) );
				else
           p1.setBorder( new EmptyBorder(5,10,10,10) );
        GridBagConstraints c=new GridBagConstraints();
				c.anchor=c.NORTHWEST;
				c.fill=GridBagConstraints.HORIZONTAL;
				c.gridwidth=1;
				c.gridx=0;
				c.gridy=0;
				c.weightx=1;
				c.weighty=0;
				Insets normInsets;
        if(JAPModel.isSmallDisplay())
          normInsets=new Insets(0,0,1,0);
        else
          normInsets=new Insets(0,0,3,0);
				c.insets=normInsets;
				g.setConstraints(m_cbProxy,c);
				p1.add(m_cbProxy);
				c.gridy=1;
				g.setConstraints(m_labelProxyType,c);
				c.gridy=2;
				p1.add(m_labelProxyType);
				g.setConstraints(m_comboProxyType,c);
				c.gridy=3;
    		p1.add(m_comboProxyType);
				g.setConstraints(m_labelProxyHost,c);
				p1.add(m_labelProxyHost);
				c.gridy=4;
				g.setConstraints(m_tfProxyHost,c);
				p1.add(m_tfProxyHost);
				c.gridy=5;
				g.setConstraints(m_labelProxyPort,c);
				p1.add(m_labelProxyPort);
				c.gridy=6;
				g.setConstraints(m_tfProxyPortNumber,c);
				p1.add(m_tfProxyPortNumber);
				JSeparator seperator=new JSeparator();
				c.gridy=7;
				if(JAPModel.isSmallDisplay())
          c.insets=new Insets(5,0,1,0);
				else
          c.insets=new Insets(10,0,3,0);
        g.setConstraints(seperator,c);
				p1.add(seperator);
				c.insets=normInsets;
				c.gridy=8;
				//c.insets=new Insets(10,0,0,0);
				g.setConstraints(m_cbProxyAuthentication,c);
				p1.add(m_cbProxyAuthentication);
				c.gridy=9;
				g.setConstraints(m_labelProxyAuthUserID,c);
				p1.add(m_labelProxyAuthUserID);
				c.gridy=10;
				g.setConstraints(m_tfProxyAuthenticationUserID,c);
				p1.add(m_tfProxyAuthenticationUserID);
				c.gridy=11;
				p.add(p1, BorderLayout.NORTH);
				return p;
			}

	  protected JPanel buildInfoServicePanel()
			{
				m_tfInfoHost = new JTextField();
        m_tfInfoHost.setFont(m_fontControls);
				m_tfInfoPortNumber = new JAPJIntField();
				m_tfInfoPortNumber.setFont(m_fontControls);
        m_tfInfoHost.addActionListener(new ActionListener() {
									   public void actionPerformed(ActionEvent e) {
							   okPressed();
							   }});
				m_tfInfoPortNumber.addActionListener(new ActionListener() {
									   public void actionPerformed(ActionEvent e) {
							   okPressed();
							   }});
				m_labelSettingsInfoText = new JLabel(JAPMessages.getString("settingsInfoText"));
				m_labelSettingsInfoText.setFont(m_fontControls);
        m_labelSettingsInfoHost = new JLabel(JAPMessages.getString("settingsInfoHost"));
				m_labelSettingsInfoHost.setFont(m_fontControls);
        m_labelSettingsInfoPort = new JLabel(JAPMessages.getString("settingsInfoPort"));
        m_labelSettingsInfoPort.setFont(m_fontControls);
				JPanel p = new JPanel();
				p.setLayout( new BorderLayout() );
        m_borderInfoService=new TitledBorder(JAPMessages.getString("settingsInfoBorder"));
				m_borderInfoService.setTitleFont(m_fontControls);
        p.setBorder( m_borderInfoService );
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
				g.setConstraints(m_labelSettingsInfoText,c);
				p1.add(m_labelSettingsInfoText);
				c.gridy=1;
				g.setConstraints(m_labelSettingsInfoHost,c);
				p1.add(m_labelSettingsInfoHost);
				c.gridy=2;
				g.setConstraints(m_tfInfoHost,c);
				p1.add(m_tfInfoHost);
				c.gridy=3;
				g.setConstraints(m_labelSettingsInfoPort,c);
				p1.add(m_labelSettingsInfoPort);
				c.gridy=4;
				g.setConstraints(m_tfInfoPortNumber,c);
				p1.add(m_tfInfoPortNumber);
				c.gridy=5;

				p.add(p1, BorderLayout.NORTH);
				return p;
			}

	  protected JPanel buildAnonPanel()
			{
				m_cbStartupMinimized=new JCheckBox(JAPMessages.getString("settingsstartupMinimizeCheckBox"));
				m_cbStartupMinimized.setFont(m_fontControls);
        m_cbAutoConnect = new JCheckBox(JAPMessages.getString("settingsautoConnectCheckBox"));
				m_cbAutoConnect.setFont(m_fontControls);
        m_cbAutoReConnect = new JCheckBox(JAPMessages.getString("settingsautoReConnectCheckBox"));
				m_cbAutoReConnect.setFont(m_fontControls);
        m_tfMixHost = new JTextField();
				m_tfMixHost.setFont(m_fontControls);
        m_tfMixPortNumber = new JAPJIntField();
        m_tfMixPortNumber.setFont(m_fontControls);
				m_tfMixPortNumberSSL = new JAPJIntField();
        m_tfMixPortNumberSSL.setFont(m_fontControls);
				m_tfMixHost.setEditable(false);
				m_tfMixPortNumber.setEditable(false);
				m_tfMixPortNumberSSL.setEditable(false);
				m_tfMixHost.addActionListener(new ActionListener() {
									   public void actionPerformed(ActionEvent e) {
							   okPressed();
							   }});
				m_tfMixPortNumber.addActionListener(new ActionListener() {
									   public void actionPerformed(ActionEvent e) {
							   okPressed();
							   }});
				m_tfMixPortNumberSSL.addActionListener(new ActionListener() {
									   public void actionPerformed(ActionEvent e) {
							   okPressed();
							   }});
				ButtonGroup bg = new ButtonGroup();
				m_rbMixStep1 = new JRadioButton(JAPMessages.getString("settingsAnonRadio1"), true);
				m_rbMixStep1.setFont(m_fontControls);
        m_rbMixStep2 = new JRadioButton(JAPMessages.getString("settingsAnonRadio2"));
				m_rbMixStep2.setFont(m_fontControls);
        m_rbMixStep3 = new JRadioButton(JAPMessages.getString("settingsAnonRadio3"));
				m_rbMixStep3.setFont(m_fontControls);
        m_bttnFetchCascades = new JButton(JAPMessages.getString("settingsAnonFetch"));
        m_bttnFetchCascades.setFont(m_fontControls);
        if(JAPModel.isSmallDisplay())
          m_bttnFetchCascades.setMargin(JAPConstants.SMALL_BUTTON_MARGIN);
        m_bttnFetchCascades.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
					JAPDebug.out(JAPDebug.DEBUG,JAPDebug.GUI,"JAPConf:m_bttnFetchCascades");
						// fetch available mix cascades from the Internet
						Cursor c=getCursor();
						setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
						m_Controller.fetchAnonServers();
            updateMixCascadeCombo();
						if (m_Controller.getAnonServerDB().size() == 0) {
							setCursor(c);
							if(!JAPModel.isSmallDisplay())
                {
                  JOptionPane.showMessageDialog(m_Controller.getView(),
											JAPMessages.getString("settingsNoServersAvailable"),
											JAPMessages.getString("settingsNoServersAvailableTitle"),
											JOptionPane.INFORMATION_MESSAGE);
                }
						} else {
							// show a window containing all available cascades
							//JAPCascadeMonitorView v=new JAPCascadeMonitorView(m_Controller.getView());
							// ------ !!!!! die folgenden zwei zeilen auskommentieren, wenn JAPCascadeMonitorView
							// ------ !!!!! ordentlich geht!!!!
							setCursor(c);
							m_rbMixStep2.doClick();
						}
						// ------ !!!!! diese wieder aktivieren!
						//okPressed();
				}});
				m_comboMixCascade = new JComboBox();
        m_comboMixCascade.setFont(m_fontControls);
				// add elements to combobox
				//m_comboMixCascade.addItem(JAPMessages.getString("settingsAnonSelect"));
				//Enumeration enum = m_Controller.anonServerDatabase.elements();
				//while (enum.hasMoreElements())
					//{
					//	m_comboMixCascade.addItem( ((AnonServerDBEntry)enum.nextElement()).getName() );
					//}

				m_comboMixCascade.setEnabled(false);
				m_comboMixCascade.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent e) {
						JAPDebug.out(JAPDebug.DEBUG,JAPDebug.GUI,"JAPConf:Item " + m_comboMixCascade.getSelectedIndex() + " selected");
						if (m_comboMixCascade.getSelectedIndex() > 0)
						{
							AnonServer entry=(AnonServer)m_comboMixCascade.getSelectedItem();
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
        //First line
				JPanel pp1 = new JPanel();
        GridBagLayout layout=new GridBagLayout();
				pp1.setLayout( layout);
        m_borderAnonSettings=new TitledBorder(JAPMessages.getString("settingsAnonBorder"));
				m_borderAnonSettings.setTitleFont(m_fontControls);
        pp1.setBorder(m_borderAnonSettings);
        GridBagConstraints c=new GridBagConstraints();
        c.fill=GridBagConstraints.HORIZONTAL;
        c.weightx=1;
        c.weighty=1;
        c.gridx=0;
        c.gridy=0;
        c.gridwidth=2;
        c.anchor=GridBagConstraints.NORTHWEST;
        //First line
        JPanel pl1=new JPanel(new BorderLayout());
				pl1.add(m_rbMixStep1,BorderLayout.CENTER);
				pl1.add(m_bttnFetchCascades,BorderLayout.EAST);
        layout.setConstraints(pl1,c);
        pp1.add(pl1);
				// Second Line
				c.gridx=0;
        c.gridy=1;
        c.weightx=0;
        c.gridwidth=1;
        c.fill=GridBagConstraints.NONE;
        layout.setConstraints(m_rbMixStep2,c);
        pp1.add(m_rbMixStep2);
				c.weightx=1;
        c.gridx=1;
        c.fill=GridBagConstraints.HORIZONTAL;
        c.anchor=GridBagConstraints.NORTHEAST;
        layout.setConstraints(m_comboMixCascade,c);
        pp1.add(m_comboMixCascade);

				// Lower panel
				JPanel pp2 = new JPanel();
				pp2.setLayout( new BorderLayout() );
				m_borderAnonSettings2=new TitledBorder(JAPMessages.getString("settingsAnonBorder2"));
        m_borderAnonSettings2.setTitleFont(m_fontControls);
				pp2.setBorder(m_borderAnonSettings2);
				// Upper panel content
				// Lower Panel content
				JPanel p2 = new JPanel();
				p2.setLayout( new GridLayout(9,1) );
				//p1.setBorder( new EmptyBorder(5,10,10,10) );
				//
				p2.add(m_rbMixStep3);
        m_labelAnonHost=new JLabel(JAPMessages.getString("settingsAnonHost"));
        m_labelAnonHost.setFont(m_fontControls);
				p2.add(m_labelAnonHost);
				p2.add(m_tfMixHost);
        m_labelAnonPort=new JLabel(JAPMessages.getString("settingsAnonPort"));
        m_labelAnonPort.setFont(m_fontControls);
				p2.add(m_labelAnonPort);
				p2.add(m_tfMixPortNumber);
				m_labelAnonProxyPort=new JLabel(JAPMessages.getString("settingsAnonSSLPort"));
        m_labelAnonProxyPort.setFont(m_fontControls);
        p2.add(m_labelAnonProxyPort);
				p2.add(m_tfMixPortNumberSSL);
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

		protected JPanel buildMiscPanel()
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
//								a[0]=new AnonServerDBEntry(m_Controller.anonHostName,m_Controller.anonHostName,m_Controller.anonPortNumber+1);
								a[0]=new AnonServerDBEntry(m_Controller.getAnonServer().getHost(),m_Controller.getAnonServer().getHost(),m_Controller.getAnonServer().getPort()+1);
								JAPRoundTripTimeView v=new JAPRoundTripTimeView(m_Controller.getView(),a);
//								v.show();
							}
					});
*/				JButton bttnMonitor=new JButton(JAPMessages.getString("bttnMonitor"));
//				bttnMonitor.setEnabled(false);
				  bttnMonitor.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						Cursor c1=getCursor();
						setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
						//m_Controller.fetchAnonServers();
						JAPCascadeMonitorView v=new JAPCascadeMonitorView(m_Controller.getView());
						//updateValues(); //THIS IS WRONG!!!!
						//okPressed();
						setCursor(c1);

				}});
				m_cbDummyTraffic=new JCheckBox("Send dummy packet every x seconds:");
        m_cbDummyTraffic.addItemListener(new ItemListener(){
					public void itemStateChanged(ItemEvent e){
            m_sliderDummyTrafficIntervall.setEnabled(e.getStateChange()==e.SELECTED);
						}
					});

				m_cbInfoServiceDisabled=new JCheckBox("Disable InfoService");
        JPanel p22 = new JPanel();
				p22.setLayout(new GridLayout(5,1));
				//p22.add(bttnPing);
              //////////////////////////////////////////////////////////////////
              //Einfug
             JButton testButton = new JButton("Update");
            testButton.addActionListener(new ActionListener() {
  public void actionPerformed (ActionEvent e) {
  Cursor c1=getCursor();
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
   p22.add(testButton);
              //////////////////////////////////////////////////////////////////

				p22.add(bttnMonitor);
        p22.add(m_cbDummyTraffic);
				m_sliderDummyTrafficIntervall=new JSlider(JSlider.HORIZONTAL,10,60,30);
        m_sliderDummyTrafficIntervall.setMajorTickSpacing(10);
        m_sliderDummyTrafficIntervall.setMinorTickSpacing(5);
        m_sliderDummyTrafficIntervall.setPaintLabels(true);
        m_sliderDummyTrafficIntervall.setPaintTicks(true);
        m_sliderDummyTrafficIntervall.setSnapToTicks(true);
        p22.add(m_sliderDummyTrafficIntervall);
        p22.add(m_cbInfoServiceDisabled);
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
						 JAPDebug.showConsole(e.getStateChange()==e.SELECTED,m_Controller.getView());
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

	  protected void cancelPressed()
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
				if (m_rbMixStep3.isSelected())
          {// -- do stuff for manual setting of anon service
            String host=m_tfMixHost.getText().trim();
            if(host==null||host.equals(""))
              {
                showError(JAPMessages.getString("errorAnonHostNotNull"));
                return false;
              }
            int port;
            try
              {
                port=Integer.parseInt(m_tfMixPortNumber.getText().trim());
              }
            catch(Exception e)
              {
                port=-1;
              }
            if(!JAPUtil.isPort(port))
              {
                showError(JAPMessages.getString("errorAnonServicePortWrong"));
                return false;
              }
            s=  m_tfMixPortNumberSSL.getText().trim();
				    int proxyport=-1;
            if (!s.equals(""))
					    {
                try
                  {
                    proxyport=Integer.parseInt(s);
                  }
                catch(Exception e)
                  {
                    proxyport=-1;
                  }
                if(!JAPUtil.isPort(proxyport))
                  {
						        showError(JAPMessages.getString("errorAnonServicePortWrong"));
						        return false;
					        }
				      }
           //now combine it together...
           try
              {
                AnonServer server = new AnonServer( null,null,
                                                    host,null,port,proxyport);
                server=null;
              }
            catch(Exception ex)
              {
                showError(JAPMessages.getString("errorAnonServiceWrong"));
				        return false;
              }
        				int anonSSLPortNumber = -1;
          }
        else if(m_rbMixStep2.isSelected())
          {//AnonService NOT manual selected
            try
              {
                AnonServer server=(AnonServer)m_comboMixCascade.getSelectedItem();
                server=null;
              }
            catch(Exception e)
              {
                showError(JAPMessages.getString("errorPleaseSelectAnonService"));
                return false;
             }
          }


 			//--------------
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
				m_tfInfoHost.setText(JAPConstants.defaultInfoServiceHostName);
				m_tfInfoPortNumber.setText(Integer.toString(JAPConstants.defaultInfoServicePortNumber));
				m_tfMixHost.setText(JAPConstants.defaultAnonHost);
				m_tfMixPortNumber.setText(Integer.toString(JAPConstants.defaultAnonPortNumber));
				m_cbProxy.setSelected(false);
				m_cbStartupMinimized.setSelected(false);
				m_cbAutoConnect.setSelected(false);
				m_cbAutoReConnect.setSelected(false);
				m_cbListenerIsLocal.setSelected(true);
				//m_cbListenerSocks.setSelected(false);
				m_cbShowDebugConsole.setSelected(false);
				m_sliderDebugLevel.setValue(JAPDebug.EMERG);
				m_cbDebugNet.setSelected(false);
				m_cbDebugGui.setSelected(false);
				m_cbDebugMisc.setSelected(false);
				m_cbDebugThread.setSelected(false);
        m_cbDummyTraffic.setSelected(false);
        m_cbInfoServiceDisabled.setSelected(false);
			}

	  protected void okPressed()
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
					m_Controller.setLocale(Locale.GERMAN);
				else
					m_Controller.setLocale(Locale.ENGLISH);
				if(m_cbDummyTraffic.isSelected())
          m_Controller.setDummyTraffic(m_sliderDummyTrafficIntervall.getValue()*1000);
				else
          m_Controller.setDummyTraffic(-1);
        // Listener settings
				m_Controller.setHTTPListener(Integer.parseInt(m_tfListenerPortNumber.getText().trim()),m_cbListenerIsLocal.isSelected(),true);
//				m_Controller.setUseSocksPort(m_cbListenerSocks.isSelected());
//				m_Controller.setSocksPortNumber(Integer.parseInt(m_tfListenerPortNumberSocks.getText().trim()));
				// Firewall settings
				int port=-1;
				try{port=Integer.parseInt(m_tfProxyPortNumber.getText().trim());}catch(Exception e){};
				int firewallType=JAPConstants.FIREWALL_TYPE_HTTP;
        if(m_comboProxyType.getSelectedIndex()==1)
          firewallType=JAPConstants.FIREWALL_TYPE_SOCKS;
        m_Controller.setProxy(firewallType,m_tfProxyHost.getText().trim(),port,m_cbProxy.isSelected());
				m_Controller.setFirewallAuthUserID(m_tfProxyAuthenticationUserID.getText().trim());
				m_Controller.setUseFirewallAuthorization(m_cbProxyAuthentication.isSelected());
				// Infoservice settings
				m_Controller.setInfoService(m_tfInfoHost.getText().trim(),Integer.parseInt(m_tfInfoPortNumber.getText().trim()));
				m_Controller.setInfoServiceDisabled(m_cbInfoServiceDisabled.isSelected());
        // Anonservice settings
				m_Controller.setAutoConnect(m_cbAutoConnect.isSelected());
				m_Controller.setAutoReConnect(m_cbAutoReConnect.isSelected());
				m_Controller.setMinimizeOnStartup(m_cbStartupMinimized.isSelected());
        //Try to Set AnonService

        AnonServer server=null;
				if (m_rbMixStep3.isSelected())
          {// -- do stuff for manual setting of anon service
						m_strMixName = JAPMessages.getString("manual");
				    int anonSSLPortNumber = -1;
            if (!m_tfMixPortNumberSSL.getText().trim().equals(""))
					    anonSSLPortNumber = Integer.parseInt(m_tfMixPortNumberSSL.getText().trim());
	          try{server = new AnonServer(null,
															m_strMixName,
															m_tfMixHost.getText().trim(),null,
															Integer.parseInt(m_tfMixPortNumber.getText().trim()),
															anonSSLPortNumber);
              }
            catch(Exception ex) //Should NEVER happen (because of checked before)
              {
                server=m_Controller.getAnonServer();
              }
          }
        else if(m_rbMixStep2.isSelected())
          {
            server=(AnonServer)m_comboMixCascade.getSelectedItem();
          }
				// -- if (the same server) (re)set the name from "manual" to the correct name
				//if (m_Controller.getAnonServer().equals(e))
				//	e.setName(m_strOldMixName);
				if(server!=null)
          m_Controller.setAnonServer(server);
				// force setting the correct name of the selected server
				//m_Controller.getAnonServer().setName(e.getName());
				// force notifying the observers set the right server name
				m_Controller.notifyJAPObservers(); // this should be the last line of okPressed() !!!
				// ... manual settings stuff finished
			}

	  public void selectCard(int selectedCard)
			{
				// set selected card to foreground
				if (selectedCard == PROXY_TAB)
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

		public void localeChanged()
      {
        setTitle(JAPMessages.getString("settingsDialog"));
   			m_Tabs.setTitleAt(PORT_TAB,JAPMessages.getString("confListenerTab"));
 				m_Tabs.setTitleAt(PROXY_TAB,JAPMessages.getString("confProxyTab"));
				m_Tabs.setTitleAt(INFO_TAB,JAPMessages.getString("confInfoTab"));
				m_Tabs.setTitleAt(ANON_TAB,JAPMessages.getString("confAnonTab"));
				if(!JAPModel.isSmallDisplay())
          m_Tabs.setTitleAt(MISC_TAB,JAPMessages.getString("confMiscTab"));
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
				m_labelAnonProxyPort.setText(JAPMessages.getString("settingsAnonSSLPort"));
        //InfoService Panel
				m_labelSettingsInfoText.setText(JAPMessages.getString("settingsInfoText"));
        m_labelSettingsInfoHost.setText(JAPMessages.getString("settingsInfoHost"));
        m_labelSettingsInfoPort.setText(JAPMessages.getString("settingsInfoPort"));
        m_borderInfoService.setTitle(JAPMessages.getString("settingsInfoBorder"));
	      //Port Panel
     		m_labelPortnumber1.setText(JAPMessages.getString("settingsPort1"));
				m_labelPortnumber2.setText(JAPMessages.getString("settingsPort2"));
        m_cbListenerIsLocal.setText(JAPMessages.getString("settingsListenerCheckBox"));
				m_borderSettingsListener.setTitle(JAPMessages.getString("settingsListenerBorder"));
        //ProxyPanel
        m_cbProxy.setText(JAPMessages.getString("settingsProxyCheckBox"));
				int i=m_comboProxyType.getSelectedIndex();
        m_comboProxyType.removeAllItems();
        m_comboProxyType.addItem(JAPMessages.getString("settingsProxyTypeHTTP"));
        m_comboProxyType.addItem(JAPMessages.getString("settingsProxyTypeSOCKS"));
        if(i!=-1)
          m_comboProxyType.setSelectedIndex(i);
      	m_cbProxyAuthentication.setText(JAPMessages.getString("settingsProxyAuthenticationCheckBox"));
				m_labelProxyHost.setText(JAPMessages.getString("settingsProxyHost"));
	      m_labelProxyPort.setText(JAPMessages.getString("settingsProxyPort"));
	      m_labelProxyType.setText(JAPMessages.getString("settingsProxyType"));
        m_labelProxyAuthUserID.setText(JAPMessages.getString("settingsProxyAuthUserID"));
			}

    public void updateValues()
			{
				// misc tab
        int iTmp=JAPModel.getDummyTraffic();
				m_cbDummyTraffic.setSelected(iTmp>-1);
        if(iTmp>-1)
          m_sliderDummyTrafficIntervall.setValue(iTmp/1000);
				m_cbShowDebugConsole.setSelected(JAPDebug.isShowConsole());
				m_cbDebugGui.setSelected((((JAPDebug.getDebugType()&JAPDebug.GUI)!=0)?true:false));
				m_cbDebugNet.setSelected((((JAPDebug.getDebugType()&JAPDebug.NET)!=0)?true:false));
				m_cbDebugThread.setSelected((((JAPDebug.getDebugType()&JAPDebug.THREAD)!=0)?true:false));
				m_cbDebugMisc.setSelected((((JAPDebug.getDebugType()&JAPDebug.MISC)!=0)?true:false));
				m_sliderDebugLevel.setValue(JAPDebug.getDebugLevel());
        m_cbInfoServiceDisabled.setSelected(JAPModel.isInfoServiceDisabled());
				m_bIgnoreComboLanguageEvents=true;
				if(m_Controller.getLocale().equals(Locale.ENGLISH))
					m_comboLanguage.setSelectedIndex(1);
				else
					m_comboLanguage.setSelectedIndex(0);
				m_bIgnoreComboLanguageEvents=false;
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
				if(JAPModel.getFirewallType()==JAPConstants.FIREWALL_TYPE_HTTP)
          m_comboProxyType.setSelectedIndex(0);
        else
          m_comboProxyType.setSelectedIndex(1);
        m_cbProxyAuthentication.setEnabled(m_cbProxy.isSelected());
        m_tfProxyHost.setText(JAPModel.getFirewallHost());
				m_tfProxyPortNumber.setText(String.valueOf(JAPModel.getFirewallPort()));
				m_tfProxyAuthenticationUserID.setText(JAPModel.getFirewallAuthUserID());
				m_cbProxyAuthentication.setSelected(JAPModel.getUseFirewallAuthorization());
				// infoservice tab
				m_tfInfoHost.setText(JAPModel.getInfoServiceHost());
				m_tfInfoPortNumber.setText(String.valueOf(JAPModel.getInfoServicePort()));
        // anon tab
				AnonServer server = m_Controller.getAnonServer();
				m_strMixName = server.getName();
				m_strOldMixName = m_strMixName;
				m_tfMixHost.setText(server.getHost());
				m_tfMixPortNumber.setText(String.valueOf(server.getPort()));
				if (server.getSSLPort()==-1)
					m_tfMixPortNumberSSL.setText("");
				else
					m_tfMixPortNumberSSL.setText(String.valueOf(server.getSSLPort()));
				m_cbAutoConnect.setSelected(JAPModel.getAutoConnect());
				m_cbAutoReConnect.setSelected(JAPModel.getAutoReConnect());
				m_cbStartupMinimized.setSelected(JAPModel.getMinimizeOnStartup());
				updateMixCascadeCombo();
        if(m_rbMixStep2.isSelected()) //Auswahl is selected
          {//try to select the current Anon Service
            m_comboMixCascade.setSelectedItem(server);
          }
		  }


    private void updateMixCascadeCombo()
      {
        m_comboMixCascade.removeAllItems();
        m_comboMixCascade.addItem(JAPMessages.getString("settingsAnonSelect"));
        Enumeration enum = m_Controller.getAnonServerDB().elements();
        while (enum.hasMoreElements())
          {
            m_comboMixCascade.addItem(enum.nextElement());
          }
        m_comboMixCascade.setSelectedIndex(0);
      }
  }