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
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.border.LineBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import anon.infoservice.InfoServiceDBEntry;
import anon.infoservice.InfoServiceHolder;
import anon.infoservice.MixCascade;
import anon.infoservice.MixInfo;
import gui.JAPMultilineLabel;
import gui.ServerListPanel;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

class JAPConfAnon extends AbstractJAPConfModule implements ListSelectionListener, ItemListener
{
	private JCheckBox m_cbAutoConnect;
	private JCheckBox m_cbAutoReConnect;

	private JAPJIntField m_tfMixPortNumber;
	private JTextField m_tfMixHost;
	private JPanel m_panelManual;
	private JLabel m_labelAnonHost, m_labelAnonPort;
	private JCheckBox m_cbMixManual;
	private JButton m_bttnFetchCascades;
	private JList m_listMixCascade;

	private TitledBorder m_borderAnonSettings, m_borderAnonSettings2;

	private JAPController m_Controller;
	private long m_lastUpdate = 0;

	private ServerListPanel m_serverList;
	private JPanel pRoot;

	private JPanel m_cascadesPanel;
	private JPanel m_serverPanel;
	private JPanel m_serverInfoPanel;

	private JLabel m_numOfUsersLabel;
	private JAPMultilineLabel m_reachableLabel;
	private JAPMultilineLabel m_portsLabel;

	private GridBagLayout m_rootPanelLayout;
	private GridBagConstraints m_rootPanelConstraints;

	private JLabel m_operatorLabel;
	private JLabel m_urlLabel;
	private JLabel m_locationLabel;

	private JLabel m_cascadeNameLabel;

	protected JAPConfAnon(IJAPConfSavePoint savePoint)
	{
		super(null);
		m_Controller = JAPController.getInstance();
	}

	public void recreateRootPanel()
	{
		m_lastUpdate = 0;
		Font font = getFontSetting();

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
		m_listMixCascade = new JList();
		m_listMixCascade.addListSelectionListener(this);
		m_listMixCascade.setFont(font);

		m_listMixCascade.setEnabled(true);
		m_cbMixManual.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				if (m_cbMixManual.isSelected())
				{
					LogHolder.log(LogLevel.DEBUG, LogType.GUI, "JAPConf:m_rbMixStep3 selected");
					m_bttnFetchCascades.setEnabled(false);
					m_listMixCascade.setEnabled(false);
					m_tfMixHost.setEditable(true);
					m_tfMixPortNumber.setEditable(true);
				}
				else
				{
					m_bttnFetchCascades.setEnabled(true);
					m_listMixCascade.setEnabled(true);
					m_tfMixHost.setEditable(false);
					m_tfMixPortNumber.setEditable(false);
				}
			}
		});

		drawCompleteDialog();
	}

	private void drawServerPanel(int a_numberOfMixes, String a_chainName)
	{
		GridBagLayout layout = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.anchor = GridBagConstraints.CENTER;

		JLabel l = new JLabel(JAPMessages.getString("infoAboutCascade"));
		m_cascadeNameLabel = new JLabel(a_chainName);

		if (m_serverPanel != null)
		{
			m_serverPanel.removeAll();

		}
		m_serverPanel = new JPanel();
		m_serverPanel.setLayout(layout);
		m_serverList = new ServerListPanel(a_numberOfMixes);
		m_serverList.addItemListener(this);

		c.insets = new Insets(2, 2, 2, 2);
		m_serverPanel.add(l, c);
		c.gridy = 1;
		m_serverPanel.add(m_cascadeNameLabel, c);
		c.gridy = 2;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(2, 2, 20, 2);
		m_serverPanel.add(m_serverList, c);
		m_serverList.addItemListener(this);

		m_rootPanelConstraints.gridx = 0;
		m_rootPanelConstraints.gridy = 1;
		m_rootPanelConstraints.anchor = GridBagConstraints.CENTER;
		pRoot.add(m_serverPanel, m_rootPanelConstraints);
	}

	private void drawServerInfoPanel(String a_operator, String a_url, String a_location)
	{
		GridBagLayout layout = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(2, 2, 2, 2);

		if (m_serverInfoPanel != null)
		{
			m_serverInfoPanel.removeAll();
		}
		m_serverInfoPanel = new JPanel();
		m_serverInfoPanel.setLayout(layout);

		JLabel l = new JLabel(JAPMessages.getString("mixOperator"));
		c.gridx = 0;
		c.gridy = 0;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.fill = GridBagConstraints.EAST;
		m_serverInfoPanel.add(l, c);

		m_operatorLabel = new JLabel(a_operator);
		// Temporary solution for long operator names destroying the whole layout
		m_operatorLabel.setPreferredSize(new Dimension(400, 17));
		c.gridx = 1;
		m_serverInfoPanel.add(m_operatorLabel, c);

		l = new JLabel(JAPMessages.getString("mixUrl"));
		c.gridx = 0;
		c.gridy = 1;
		m_serverInfoPanel.add(l, c);

		m_urlLabel = new JLabel(a_url);
		c.gridx = 1;
		m_serverInfoPanel.add(m_urlLabel, c);

		l = new JLabel(JAPMessages.getString("mixLocation"));
		c.gridx = 0;
		c.gridy = 2;
		m_serverInfoPanel.add(l, c);

		m_locationLabel = new JLabel(a_location);
		c.gridx = 1;
		m_serverInfoPanel.add(m_locationLabel, c);

		m_rootPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
		m_rootPanelConstraints.gridx = 0;
		m_rootPanelConstraints.gridy = 2;
		pRoot.add(m_serverInfoPanel, m_rootPanelConstraints);
	}

	private void drawCascadesPanel()
	{
		GridBagLayout layout = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(2, 2, 2, 2);

		if (m_cascadesPanel != null)
		{
			m_cascadesPanel.removeAll();
		}
		else
		{
			m_cascadesPanel = new JPanel();

		}
		m_cascadesPanel.setLayout(layout);

		JLabel l = new JLabel(JAPMessages.getString("availableCascades"));
		c.gridx = 0;
		c.gridy = 0;
		c.anchor = GridBagConstraints.NORTHWEST;
		m_cascadesPanel.add(l, c);

		m_listMixCascade = new JList();
		m_listMixCascade.setFixedCellWidth(l.getPreferredSize().width);
		m_listMixCascade.setBorder(LineBorder.createBlackLineBorder());
		m_listMixCascade.addListSelectionListener(this);
		c.gridx = 0;
		c.gridy = 1;
		c.gridheight = 3;
		m_cascadesPanel.add(m_listMixCascade, c);

		l = new JLabel(JAPMessages.getString("numOfUsersOnCascade"));
		c.gridheight = 1;
		c.gridx = 1;
		c.gridy = 1;
		c.gridheight = 1;
		c.weightx = 1;
		c.weighty = 1;
		m_cascadesPanel.add(l, c);

		m_numOfUsersLabel = new JLabel("");
		c.gridx = 2;
		c.gridy = 1;
		m_cascadesPanel.add(m_numOfUsersLabel, c);

		l = new JLabel(JAPMessages.getString("cascadeReachableBy"));
		c.gridx = 1;
		c.gridy = 2;
		m_cascadesPanel.add(l, c);

		m_reachableLabel = new JAPMultilineLabel("");
		c.gridx = 2;
		c.gridy = 2;
		m_cascadesPanel.add(m_reachableLabel, c);

		l = new JLabel(JAPMessages.getString("cascadePorts"));
		c.gridx = 1;
		c.gridy = 3;
		m_cascadesPanel.add(l, c);

		m_portsLabel = new JAPMultilineLabel("");
		c.gridx = 2;
		c.gridy = 3;
		m_cascadesPanel.add(m_portsLabel, c);

		m_rootPanelConstraints.gridx = 0;
		m_rootPanelConstraints.gridy = 0;
		m_rootPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
		m_rootPanelConstraints.weightx = 1;
		m_rootPanelConstraints.weighty = 1;

		pRoot.add(m_cascadesPanel, m_rootPanelConstraints);
	}

	private void drawCompleteDialog()
	{
		m_rootPanelLayout = new GridBagLayout();
		m_rootPanelConstraints = new GridBagConstraints();

		pRoot = getRootPanel();
		pRoot.removeAll();
		pRoot.setLayout(m_rootPanelLayout);
		m_rootPanelConstraints.anchor = GridBagConstraints.NORTHWEST;

		drawCascadesPanel();
		drawServerPanel(3, "");
		drawServerInfoPanel(null, null, null);
	}

	public void itemStateChanged(ItemEvent e)
	{
		int server = m_serverList.getSelectedIndex();
		MixCascade cascade = (MixCascade) m_listMixCascade.getSelectedValue();
		String selectedMixId = (String) cascade.getMixIds().elementAt(server);
		MixInfo selectedMixInfo =
			(MixInfo) InfoServiceHolder.getInstance().getMixInfo(selectedMixId);

		m_operatorLabel.setText(selectedMixInfo.getServiceOperator().getOrganisation());
		m_locationLabel.setText(selectedMixInfo.getServiceLocation().getCity());
		m_urlLabel.setText(selectedMixInfo.getServiceOperator().getUrl());

	}

	private void updateMixCascadeCombo()
	{
		LogHolder.log(LogLevel.DEBUG, LogType.GUI, "JAPConf: updateMixCascadeCombo() -start");
		Enumeration it = m_Controller.getMixCascadeDatabase().elements();
		DefaultListModel listModel = new DefaultListModel();
		while (it.hasMoreElements())
		{
			listModel.addElement(it.nextElement());
		}

		LogHolder.log(LogLevel.DEBUG, LogType.GUI, "JAPConf: updateMixCascadeCombo() -added All other Items");
		m_listMixCascade.setModel(listModel);
		m_listMixCascade.setSelectedIndex(0);
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
			if (!JAPUtil.isValidPort(port))
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
				newCascade = (MixCascade) m_listMixCascade.getSelectedValue();
			}
			catch (Exception e)
			{
				newCascade = null;
			}
		}

		// Anonservice settings
		m_Controller.setAutoConnect(m_cbAutoConnect.isSelected());
		m_Controller.setAutoReConnect(m_cbAutoReConnect.isSelected());
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
		updateMixCascadeCombo();
		if (!m_cbMixManual.isSelected()) //Auswahl is selected
		{ //try to select the current MixCascade
			try
			{
				m_listMixCascade.setSelectedValue(mixCascade, true);
			}
			catch (Exception e)
			{ ///@todo really undone work yet...
			}
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

	/**
	 * Handles the selection of a cascade
	 * @param e ListSelectionEvent
	 */
	public void valueChanged(ListSelectionEvent e)
	{
		Object source = e.getSource();
		if (source == m_listMixCascade)
		{
			if (m_listMixCascade.getSelectedIndex() > -1)
			{
				try
				{
					MixCascade cascade = (MixCascade) m_listMixCascade.getSelectedValue();
					drawServerPanel(cascade.getMixCount(), cascade.getName());
					/** @todo Temporary solution for getting number of active users */
					InfoServiceDBEntry entry = InfoServiceHolder.getInstance().getPreferedInfoService();
					int numUsers = entry.getStatusInfo(cascade.getId(), cascade.getMixCount(),
						InfoServiceHolder.getInstance()
						.getCertificateStore()).getNrOfActiveUsers();
					m_numOfUsersLabel.setText(Integer.toString(numUsers));
					String interfaces = "";
					String ports = "";

					for (int i = 0; i < cascade.getNumberOfListenerInterfaces(); i++)
					{
						interfaces += cascade.getListenerInterface(i).getHost();
						ports += String.valueOf(cascade.getListenerInterface(i).getPort());
						if (i != cascade.getNumberOfListenerInterfaces() - 1)
						{
							interfaces += "\n";
							ports += "\n";
						}
					}
					m_reachableLabel.setText(interfaces);
					m_portsLabel.setText(ports);
					itemStateChanged(null);
				}
				catch (Exception ex)
				{

				}
			}
		}
	}

}
