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
import java.awt.Cursor;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import anon.infoservice.MixCascade;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

class JAPConfAnon extends AbstractJAPConfModule
{
	private JCheckBox m_cbAutoConnect;
	private JCheckBox m_cbAutoReConnect;
	private JCheckBox m_cbStartupMinimized;

	private JAPJIntField m_tfMixPortNumber;
	private JTextField m_tfMixHost;
	private JPanel m_panelManual;
	private JLabel m_labelAnonHost, m_labelAnonPort;
	private JCheckBox m_cbMixManual;
	private JButton m_bttnFetchCascades;
	private JComboBox m_comboMixCascade;

	private TitledBorder m_borderAnonSettings, m_borderAnonSettings2;

	private JAPController m_Controller;
	private long m_lastUpdate = 0;

	protected JAPConfAnon(IJAPConfSavePoint savePoint)
	{
		super(null);
		m_Controller = JAPController.getController();
	}

	public void recreateRootPanel()
	{
		m_lastUpdate = 0;
		Font font = getFontSetting();

		m_cbStartupMinimized = new JCheckBox(JAPMessages.getString("settingsstartupMinimizeCheckBox"));
		m_cbStartupMinimized.setFont(font);
		m_cbAutoConnect = new JCheckBox(JAPMessages.getString("settingsautoConnectCheckBox"));
		m_cbAutoConnect.setFont(font);
		m_cbAutoReConnect = new JCheckBox(JAPMessages.getString("settingsautoReConnectCheckBox"));
		m_cbAutoReConnect.setFont(font);
		m_tfMixHost = new JTextField();
		m_tfMixHost.setFont(font);
		m_tfMixPortNumber = new JAPJIntField();
		m_tfMixPortNumber.setFont(font);
		m_tfMixHost.setEditable(false);
		m_tfMixPortNumber.setEditable(false);
		m_cbMixManual = new JCheckBox(JAPMessages.getString("settingsAnonRadio3"));
		m_cbMixManual.setFont(font);
		m_bttnFetchCascades = new JButton(JAPMessages.getString("settingsAnonFetch"));
		m_bttnFetchCascades.setFont(font);
		if (JAPModel.isSmallDisplay())
		{
			m_bttnFetchCascades.setMargin(JAPConstants.SMALL_BUTTON_MARGIN);
		}
		m_bttnFetchCascades.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				fetchCascades(true);
				// ------ !!!!! diese wieder aktivieren!
				//okPressed();
			}
		});
		m_comboMixCascade = new JComboBox();
		m_comboMixCascade.setFont(font);

		m_comboMixCascade.setEnabled(true);
		m_comboMixCascade.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				LogHolder.log(LogLevel.DEBUG, LogType.GUI,
							  "JAPConf:Item " + m_comboMixCascade.getSelectedIndex() + " selected");
				if (m_comboMixCascade.getSelectedIndex() > 0)
				{
					MixCascade mixCascadeEntry = (MixCascade) m_comboMixCascade.getSelectedItem();
					//m_strMixName = mixCascadeEntry.getName();
					//m_strOldMixName = m_strMixName;
					m_tfMixHost.setText(mixCascadeEntry.getListenerInterface(0).getHost());
					m_tfMixPortNumber.setText(Integer.toString(mixCascadeEntry.getListenerInterface(0).
						getPort()));
				}
			}
		});

		m_cbMixManual.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				if (m_cbMixManual.isSelected())
				{
					LogHolder.log(LogLevel.DEBUG, LogType.GUI, "JAPConf:m_rbMixStep3 selected");
					m_bttnFetchCascades.setEnabled(false);
					m_comboMixCascade.setEnabled(false);
					m_tfMixHost.setEditable(true);
					m_tfMixPortNumber.setEditable(true);
				}
				else
				{
					m_bttnFetchCascades.setEnabled(true);
					m_comboMixCascade.setEnabled(true);
					m_tfMixHost.setEditable(false);
					m_tfMixPortNumber.setEditable(false);
				}
			}
		});

// layout stuff
// Upper panel
//First line
		JPanel pp1 = new JPanel();
		GridBagLayout layout = new GridBagLayout();
		pp1.setLayout(layout);
		m_borderAnonSettings = new TitledBorder(JAPMessages.getString("settingsAnonBorder"));
		m_borderAnonSettings.setTitleFont(font);
		pp1.setBorder(m_borderAnonSettings);
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		if (!JAPModel.isSmallDisplay())
		{
			c.insets = new Insets(5, 5, 5, 5);
		}
		c.weightx = 1;
		c.weighty = 1;
		c.gridx = 0;
		c.gridy = 0;
		c.gridwidth = 1;
		c.anchor = GridBagConstraints.NORTHWEST;
//First line
		layout.setConstraints(m_comboMixCascade, c);
		pp1.add(m_comboMixCascade);
// Second Line
		c.gridx = 1;
		c.weightx = 0;
		c.fill = GridBagConstraints.NONE;
		layout.setConstraints(m_bttnFetchCascades, c);
		pp1.add(m_bttnFetchCascades);

// Lower panel
		m_panelManual = new JPanel();
		m_panelManual.setLayout(new GridLayout(5, 1));
		m_borderAnonSettings2 = new TitledBorder(JAPMessages.getString("settingsAnonBorder2"));
		m_borderAnonSettings2.setTitleFont(font);
		m_panelManual.setBorder(m_borderAnonSettings2);
		m_panelManual.add(m_cbMixManual);
		m_labelAnonHost = new JLabel(JAPMessages.getString("settingsAnonHost"));
		m_labelAnonHost.setFont(font);
		m_panelManual.add(m_labelAnonHost);
		m_panelManual.add(m_tfMixHost);
		m_labelAnonPort = new JLabel(JAPMessages.getString("settingsAnonPort"));
		m_labelAnonPort.setFont(font);
		m_panelManual.add(m_labelAnonPort);
		m_panelManual.add(m_tfMixPortNumber);
//
// Add to main panel
		JPanel p = getRootPanel();
		layout = new GridBagLayout();
		p.setLayout(layout);
		c.weightx = 1;
		c.weighty = 0;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 0;
		if (!JAPModel.isSmallDisplay())
		{
			c.insets = new Insets(5, 5, 5, 5);
		}
		layout.setConstraints(pp1, c);
		p.add(pp1);
		c.gridy = 1;
		layout.setConstraints(m_panelManual, c);
		p.add(m_panelManual);

		c.gridy = 3;
		layout.setConstraints(m_cbAutoConnect, c);
		p.add(m_cbAutoConnect);
		c.gridy = 4;
		layout.setConstraints(m_cbAutoReConnect, c);
		p.add(m_cbAutoReConnect);

		JLabel label = new JLabel();
		c.gridy = 5;
		c.weighty = 1;
		c.fill = GridBagConstraints.VERTICAL;
		layout.setConstraints(label, c);
		p.add(label);
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

	/**
	 * getTabTitle
	 *
	 * @return String
	 */
	public String getTabTitle()
	{
		return JAPMessages.getString("confAnonTab");
	}

	public void onResetToDefaultsPressed()
	{
		m_tfMixHost.setText(JAPConstants.defaultAnonHost);
		m_tfMixPortNumber.setText(Integer.toString(JAPConstants.defaultAnonPortNumber));
		m_cbStartupMinimized.setSelected(false);
		m_cbAutoConnect.setSelected(false);
		m_cbAutoReConnect.setSelected(false);

	}

	public boolean onOkPressed()
	{ //Checking First Mix (Host + Port)
		MixCascade newCascade = null;
		if (m_cbMixManual.isSelected())
		{ // -- do stuff for manual setting of anon service
			String host = m_tfMixHost.getText().trim();
			if (host == null || host.equals(""))
			{
				JAPConf.showError(JAPMessages.getString("errorAnonHostNotNull"));
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
				JAPConf.showError(JAPMessages.getString("errorAnonServicePortWrong"));
				return false;
			}
			//now combine it together...
			try
			{
				/* this is only a test for the values */
				newCascade = new MixCascade(null, null, host, port);
			}
			catch (Exception ex)
			{
				JAPConf.showError(JAPMessages.getString("errorAnonServiceWrong"));
				return false;
			}
		}
		else
		{
			//AnonService NOT manual selected
			try
			{
				// check, if something is selected //
				newCascade = (MixCascade) m_comboMixCascade.getSelectedItem();
			}
			catch (Exception e)
			{
				newCascade = null;
			}
		}

		// Anonservice settings
		m_Controller.setAutoConnect(m_cbAutoConnect.isSelected());
		m_Controller.setAutoReConnect(m_cbAutoReConnect.isSelected());
		m_Controller.setMinimizeOnStartup(m_cbStartupMinimized.isSelected());
		//Try to Set AnonService
		if (newCascade != null)
		{
			m_Controller.setCurrentMixCascade(newCascade);
		}

		return true;

	}

	public void onUpdateValues()
	{
		// anon tab
		MixCascade mixCascade = m_Controller.getCurrentMixCascade();
		m_tfMixHost.setText(mixCascade.getListenerInterface(0).getHost());
		m_tfMixPortNumber.setText(Integer.toString(mixCascade.getListenerInterface(0).getPort()));
		m_cbAutoConnect.setSelected(JAPModel.getAutoConnect());
		m_cbAutoReConnect.setSelected(JAPModel.getAutoReConnect());
		m_cbStartupMinimized.setSelected(JAPModel.getMinimizeOnStartup());
		updateMixCascadeCombo();
		if (!m_cbMixManual.isSelected()) //Auswahl is selected
		{ //try to select the current MixCascade
			m_comboMixCascade.setSelectedItem(mixCascade);
		}
	}

	private void fetchCascades(boolean bShowError)
	{
		LogHolder.log(LogLevel.DEBUG, LogType.GUI, "JAPConf:m_bttnFetchCascades");
		// fetch available mix cascades from the Internet
		Cursor c = getRootPanel().getCursor();
		getRootPanel().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		m_Controller.fetchMixCascades(bShowError);
		updateMixCascadeCombo();
		LogHolder.log(LogLevel.DEBUG, LogType.GUI, "JAPConf: finished updateMixCascadeCombo()");
		getRootPanel().setCursor(c);
		if (m_Controller.getMixCascadeDatabase().size() == 0)
		{
			if (!JAPModel.isSmallDisplay() && bShowError)
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
			m_lastUpdate = System.currentTimeMillis();
			LogHolder.log(LogLevel.DEBUG, LogType.GUI, "JAPConf: setting old cursor()");
			m_comboMixCascade.showPopup();
			//m_rbMixStep2.doClick();
		}

	}

	protected void onRootPanelShown()
	{
		if (System.currentTimeMillis() - m_lastUpdate > 600000)
		{
			fetchCascades(false);
		}
	}

}
