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
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
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
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;

import anon.infoservice.InfoServiceDBEntry;
import anon.infoservice.InfoServiceHolder;
import anon.infoservice.MixCascade;
import anon.infoservice.MixInfo;
import gui.JAPMultilineLabel;
import gui.ServerListPanel;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

import jap.platform.*;

class JAPConfAnon extends AbstractJAPConfModule implements MouseListener, ActionListener,
	ListSelectionListener, ItemListener, KeyListener
{
	private static final String URL_BEGIN = "<HTML><font color=blue><u>";
	private static final String URL_END = "</u></font></HTML>";

	private boolean bErr;

	private JAPJIntField m_tfMixPortNumber;
	private JTextField m_tfMixHost;

	private JCheckBox m_cbMixManual;
	private JButton m_bttnFetchCascades;
	private JList m_listMixCascade;

	private JAPController m_Controller;

	private ServerListPanel m_serverList;
	private JPanel pRoot;

	private JPanel m_cascadesPanel;
	private JPanel m_serverPanel;
	private JPanel m_serverInfoPanel;
	private JPanel m_manualPanel;

	private JLabel m_numOfUsersLabel;
	private JAPMultilineLabel m_reachableLabel;
	private JAPMultilineLabel m_portsLabel;

	private GridBagLayout m_rootPanelLayout;
	private GridBagConstraints m_rootPanelConstraints;

	private JLabel m_operatorLabel;
	private JLabel m_urlLabel;
	private JLabel m_locationLabel;

	private JButton m_manualCascadeButton;
	private JButton m_reloadCascadesButton;
	private JButton m_selectCascadeButton;
	private JButton m_enterCascadeButton;
	private JButton m_editCascadeButton;

	private JTextField m_manHostField;
	private JTextField m_manPortField;

	protected JAPConfAnon(IJAPConfSavePoint savePoint)
	{
		super(null);
		m_Controller = JAPController.getInstance();
	}

	public void recreateRootPanel()
	{
		Font font = getFontSetting();

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
			}
		});
		m_listMixCascade = new JList();
		m_listMixCascade.addListSelectionListener(this);
		m_listMixCascade.addMouseListener(this);
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

	private void drawServerPanel(int a_numberOfMixes, boolean a_enabled)
	{
		if (m_manualPanel != null)
		{
			pRoot.remove(m_manualPanel);
			pRoot.updateUI();
			m_manualPanel = null;
		}
		GridBagLayout layout = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.insets = new Insets(5, 5, 5, 5);

		JLabel l = new JLabel(JAPMessages.getString("infoAboutCascade"));

		if (m_serverPanel != null)
		{
			m_serverPanel.removeAll();
		}

		m_serverPanel = new JPanel();
		m_serverPanel.setLayout(layout);
		m_serverList = new ServerListPanel(a_numberOfMixes, a_enabled);
		m_serverList.addItemListener(this);

		c.insets = new Insets(5, 10, 5, 5);
		m_serverPanel.add(l, c);
		c.gridy = 1;
		c.insets = new Insets(2, 20, 2, 2);
		m_serverPanel.add(m_serverList, c);
		m_serverList.addItemListener(this);

		m_rootPanelConstraints.gridx = 0;
		m_rootPanelConstraints.gridy = 2;
		m_rootPanelConstraints.weightx = 0;
		m_rootPanelConstraints.weighty = 0;
		m_rootPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
		m_rootPanelConstraints.fill = GridBagConstraints.NONE;
		pRoot.add(m_serverPanel, m_rootPanelConstraints);
	}

	private void drawServerInfoPanel(String a_operator, String a_url, String a_location)
	{
		GridBagLayout layout = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(5, 10, 5, 5);

		if (m_serverInfoPanel != null)
		{
			m_serverInfoPanel.removeAll();
		}
		m_serverInfoPanel = new JPanel();
		m_serverInfoPanel.setLayout(layout);

		JLabel l = new JLabel(JAPMessages.getString("mixOperator"));
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 0;
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.NORTHWEST;
		m_serverInfoPanel.add(l, c);

		m_operatorLabel = new JLabel(a_operator);
		c.weightx = 1;
		c.gridx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		m_serverInfoPanel.add(m_operatorLabel, c);

		l = new JLabel(JAPMessages.getString("mixUrl"));
		c.gridx = 0;
		c.gridy = 1;
		c.weightx = 0;
		m_serverInfoPanel.add(l, c);

		m_urlLabel = new JLabel(a_url);
		m_urlLabel.addMouseListener(this);
		c.gridx = 1;
		m_serverInfoPanel.add(m_urlLabel, c);

		l = new JLabel(JAPMessages.getString("mixLocation"));
		c.gridx = 0;
		c.gridy = 2;
		m_serverInfoPanel.add(l, c);

		m_locationLabel = new JLabel(a_location);
		c.gridx = 1;
		m_serverInfoPanel.add(m_locationLabel, c);

		m_serverInfoPanel.setPreferredSize(new Dimension(300,200));
		m_rootPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
		m_rootPanelConstraints.gridx = 0;
		m_rootPanelConstraints.gridy = 3;
		m_rootPanelConstraints.weightx = 0;
		m_rootPanelConstraints.weighty = 0;
		pRoot.add(m_serverInfoPanel, m_rootPanelConstraints);
	}

	private void drawManualPanel(String a_hostName, String a_port, boolean a_newCascade)
	{
		m_manualPanel = new JPanel();
		GridBagLayout layout = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(5,5,5,5);
		c.anchor = c.NORTHWEST;
		m_manualPanel.setLayout(layout);
		JLabel l = new JLabel(JAPMessages.getString("manualServiceAddHost"));
		c.gridx = 0;
		c.gridy = 0;
		m_manualPanel.add(l, c);
		l = new JLabel(JAPMessages.getString("manualServiceAddPort"));
		c.gridy = 1;
		m_manualPanel.add(l, c);
		m_manHostField = new JTextField();
		m_manHostField.setText(a_hostName);
		c.fill=c.HORIZONTAL;
		c.weightx = 1;
		c.gridx = 1;
		c.gridy = 0;
		m_manualPanel.add(m_manHostField, c);
		m_manPortField = new JTextField();
		m_manPortField.setText(a_port);
		c.gridy = 1;
		c.gridwidth = 1;
		m_manualPanel.add(m_manPortField, c);
		c.weightx = 0;
		c.gridy = 2;
		c.gridx = 1;
		c.fill=c.NONE;
		c.anchor = c.NORTHEAST;
		if (a_newCascade)
		{
			m_enterCascadeButton = new JButton(JAPMessages.getString("manualServiceEnter"));
			m_enterCascadeButton.addActionListener(this);
			m_manualPanel.add(m_enterCascadeButton, c);
		}
		else
		{
			m_editCascadeButton = new JButton(JAPMessages.getString("manualServiceEdit"));
			m_editCascadeButton.addActionListener(this);
			m_editCascadeButton.setVisible(false);
			m_manualPanel.add(m_editCascadeButton, c);
			m_manHostField.addKeyListener(this);
			m_manPortField.addKeyListener(this);
		}
		if (m_serverPanel != null)
		{
			pRoot.remove(m_serverPanel);
			pRoot.remove(m_serverInfoPanel);
			m_serverPanel = null;
		}
		pRoot.updateUI();
		m_rootPanelConstraints.gridx = 0;
		m_rootPanelConstraints.gridy = 2;
		m_rootPanelConstraints.weightx = 0;
		m_rootPanelConstraints.weighty = 0;
		m_rootPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
		m_rootPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
		pRoot.add(m_manualPanel, m_rootPanelConstraints);
	}

	private void drawCascadesPanel()
	{
		GridBagLayout layout = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();

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
		c.gridwidth = 2;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.insets = new Insets(5, 5, 5, 5);
		m_cascadesPanel.add(l, c);

		m_listMixCascade = new JList();
		m_listMixCascade.addListSelectionListener(this);
		m_listMixCascade.addMouseListener(this);
		m_listMixCascade.setFixedCellWidth(l.getPreferredSize().width);

		m_listMixCascade.setBorder(LineBorder.createBlackLineBorder());
		m_listMixCascade.addListSelectionListener(this);
		c.gridx = 0;
		c.gridy = 1;
		c.ipady = 70;
		c.gridheight = 3;
		c.gridwidth = 2;
		c.fill = GridBagConstraints.BOTH;
		m_cascadesPanel.add(m_listMixCascade, c);

		c.ipady = 0;
		c.gridheight = 1;
		c.gridwidth = 1;

		c.insets = new Insets(2, 5, 0, 5);
		m_reloadCascadesButton = new JButton(JAPMessages.getString("reloadCascades"));
		m_reloadCascadesButton.addActionListener(this);
		c.gridy = 4;
		m_cascadesPanel.add(m_reloadCascadesButton, c);

		m_manualCascadeButton = new JButton(JAPMessages.getString("manualCascade"));
		m_manualCascadeButton.addActionListener(this);
		c.gridx = 1;
		m_cascadesPanel.add(m_manualCascadeButton, c);

		m_selectCascadeButton = new JButton(JAPMessages.getString("selectCascade"));
		m_selectCascadeButton.addActionListener(this);
		c.gridx = 2;
		m_cascadesPanel.add(m_selectCascadeButton, c);

		c.insets = new Insets(5, 20, 0, 5);

		l = new JLabel(JAPMessages.getString("numOfUsersOnCascade"));
		c.gridx = 2;
		c.gridy = 1;
		c.weightx = 0;
		c.fill = GridBagConstraints.NONE;
		m_cascadesPanel.add(l, c);

		c.insets = new Insets(5, 5, 0, 5);
		m_numOfUsersLabel = new JLabel("");
		c.gridx = 3;
		c.gridy = 1;
		c.weightx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		m_cascadesPanel.add(m_numOfUsersLabel, c);

		c.insets = new Insets(5, 20, 0, 5);
		l = new JLabel(JAPMessages.getString("cascadeReachableBy"));
		c.gridx = 2;
		c.gridy = 2;
		c.weightx = 0;
		c.fill = GridBagConstraints.NONE;
		m_cascadesPanel.add(l, c);

		c.insets = new Insets(5, 5, 0, 5);
		m_reachableLabel = new JAPMultilineLabel("");
		c.gridx = 3;
		c.gridy = 2;
		c.weightx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		m_cascadesPanel.add(m_reachableLabel, c);

		c.insets = new Insets(5, 20, 0, 5);
		l = new JLabel(JAPMessages.getString("cascadePorts"));
		c.gridx = 2;
		c.gridy = 3;
		c.weightx = 0;
		c.fill = GridBagConstraints.NONE;
		m_cascadesPanel.add(l, c);

		c.insets = new Insets(5, 5, 0, 5);
		m_portsLabel = new JAPMultilineLabel("");
		c.gridx = 3;
		c.gridy = 3;
		c.weightx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		m_cascadesPanel.add(m_portsLabel, c);

		m_rootPanelConstraints.gridx = 0;
		m_rootPanelConstraints.gridy = 0;
		m_rootPanelConstraints.insets = new Insets(10, 10, 10, 10);
		m_rootPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
		m_rootPanelConstraints.weightx = 1;

		pRoot.add(m_cascadesPanel, m_rootPanelConstraints);

		m_rootPanelConstraints.weightx = 0;
		m_rootPanelConstraints.weighty = 0;
		JSeparator sep = new JSeparator();
		m_rootPanelConstraints.gridy = 1;
		m_rootPanelConstraints.fill = GridBagConstraints.HORIZONTAL;
		pRoot.add(sep, m_rootPanelConstraints);
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
		drawServerInfoPanel("", "", "");

		m_rootPanelConstraints.gridx = 0;
		m_rootPanelConstraints.gridy = 3;
		m_rootPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
		m_rootPanelConstraints.fill = GridBagConstraints.VERTICAL;
		m_rootPanelConstraints.weighty = 1;
		JPanel fillPanel = new JPanel();
		pRoot.add(fillPanel, m_rootPanelConstraints);
	}

	public void itemStateChanged(ItemEvent e)
	{
		int server = m_serverList.getSelectedIndex();
		MixCascade cascade = (MixCascade) m_listMixCascade.getSelectedValue();
		if (!cascade.isUserDefined())
		{
		String selectedMixId = (String) cascade.getMixIds().elementAt(server);
			try
			{
		MixInfo selectedMixInfo =
			InfoServiceHolder.getInstance().getMixInfo(selectedMixId);

		m_operatorLabel.setText(selectedMixInfo.getServiceOperator().getOrganisation());
		m_operatorLabel.setToolTipText(selectedMixInfo.getServiceOperator().getOrganisation());
		m_locationLabel.setText(selectedMixInfo.getServiceLocation().getCity() + ", " +
								selectedMixInfo.getServiceLocation().getCountry());
		m_urlLabel.setText(URL_BEGIN + selectedMixInfo.getServiceOperator().getUrl()
						   + URL_END);
			}
			catch(Exception ex)
			{
				LogHolder.log(LogLevel.ERR, LogType.GUI, "Could not retrieve information from Infoservice");
				m_operatorLabel.setText("");
				m_operatorLabel.setToolTipText("");
				m_locationLabel.setText("");
				m_urlLabel.setText("");
			}
		}

	}

	private void updateMixCascadeCombo()
	{
		LogHolder.log(LogLevel.DEBUG, LogType.GUI, "-start");
		Enumeration it = m_Controller.getMixCascadeDatabase().elements();
		DefaultListModel listModel = new DefaultListModel();
		CustomRenderer cr = new CustomRenderer();
		m_listMixCascade.setCellRenderer(cr);
		while (it.hasMoreElements())
		{
			MixCascade cascade = (MixCascade)it.nextElement();
			if (cascade.isUserDefined())
			{
				listModel.addElement(cascade);
			}
			else
			{
				listModel.addElement(cascade);
			}

		}

		LogHolder.log(LogLevel.DEBUG, LogType.GUI, "-added All other Items");
		m_listMixCascade.setModel(listModel);
		m_listMixCascade.setSelectedIndex(0);
		LogHolder.log(LogLevel.DEBUG, LogType.GUI,
					  "- select First Item -- finished!");
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

	}

	public boolean onOkPressed()
	{ //Checking First Mix (Host + Port)
/*		MixCascade newCascade = null;
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
				// this is only a test for the values
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

		//Try to Set AnonService
		if (newCascade != null)
		{
			m_Controller.setCurrentMixCascade(newCascade);
		}
*/
		return true;

	}

	public void onUpdateValues()
	{
		MixCascade mixCascade = m_Controller.getCurrentMixCascade();
		m_tfMixHost.setText(mixCascade.getListenerInterface(0).getHost());
		m_tfMixPortNumber.setText(Integer.toString(mixCascade.getListenerInterface(0).getPort()));
		updateMixCascadeCombo();
		if (!m_cbMixManual.isSelected())
		{
			try
			{
				m_listMixCascade.setSelectedValue(mixCascade, true);
			}
			catch (Exception e)
			{
			}
		}
	}

	private void fetchCascades(boolean bShowError)
	{
		bErr = bShowError;
		m_reloadCascadesButton.setEnabled(false);
		Runnable doIt = new Runnable()
		{
			public void run()
			{
		LogHolder.log(LogLevel.DEBUG, LogType.GUI, "JAPConf:m_bttnFetchCascades");

		// fetch available mix cascades from the Internet
		Cursor c = getRootPanel().getCursor();
		getRootPanel().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

				m_Controller.fetchMixCascades(bErr);

		updateMixCascadeCombo();

		LogHolder.log(LogLevel.DEBUG, LogType.GUI, "JAPConf: finished updateMixCascadeCombo()");

		getRootPanel().setCursor(c);

		if (m_Controller.getMixCascadeDatabase().size() == 0)
		{
					if (!JAPModel.isSmallDisplay() && bErr)
			{
				JOptionPane.showMessageDialog(JAPController.getView(),
											  JAPMessages.getString("settingsNoServersAvailable"),
											  JAPMessages.getString("settingsNoServersAvailableTitle"),
											  JOptionPane.INFORMATION_MESSAGE);

			}
			//No mixcascades returned by Infoservice
			deactivate();
		}
		else
		{
			// show a window containing all available cascades
			LogHolder.log(LogLevel.DEBUG, LogType.GUI, "JAPConf: setting old cursor()");
			m_listMixCascade.setEnabled(true);
		}
				LogHolder.log(LogLevel.DEBUG, LogType.GUI, "Enabling reload button");
				m_reloadCascadesButton.setEnabled(true);
			}
		};
		Thread t = new Thread(doIt);
		t.start();
	}

	/** Deactivates GUI when no cascades are returned by the Infoservice
	 *
	 */
	private void deactivate()
	{

		m_listMixCascade.removeAll();
		DefaultListModel model = new DefaultListModel();
		model.addElement(JAPMessages.getString("noCascadesAvail"));
		m_listMixCascade.setModel(model);
		m_listMixCascade.setEnabled(false);

		m_numOfUsersLabel.setText("");
		m_portsLabel.setText("");
		m_reachableLabel.setText("");
		drawServerPanel(3, false);

		drawServerInfoPanel("", "", "");
		m_serverInfoPanel.setEnabled(false);

	}

	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == m_reloadCascadesButton)
		{
			fetchCascades(false);
		}
		else if (e.getSource() == m_selectCascadeButton)
		{
			MixCascade newCascade = null;
			try
			{
				newCascade = (MixCascade) m_listMixCascade.getSelectedValue();
				m_listMixCascade.repaint();
			}
			catch (Exception ex)
			{
				newCascade = null;
			}
			if (newCascade != null)
			{
				m_Controller.setCurrentMixCascade(newCascade);
			}
		}
		else if (e.getSource() == m_manualCascadeButton)
		{
			MixCascade cascade = (MixCascade) m_listMixCascade.getSelectedValue();
			if (cascade.isUserDefined())
			{
				this.drawManualPanel(cascade.getListenerInterface(0).getHost(),
									 String.valueOf(cascade.getListenerInterface(0).getPort()),
									 false);
			}
			else
			{
				this.drawManualPanel(null, null, true);
			}
		}
		else if (e.getSource() == m_enterCascadeButton)
		{
			this.enterManualCascade();
		}
		else if (e.getSource() == m_editCascadeButton)
		{
			this.editManualCascade();
		}
	}


	/**
	 * Edits a manually configured cascade
	 */
	private void editManualCascade()
	{
		try
		{
			MixCascade oldCascade = (MixCascade) m_listMixCascade.getSelectedValue();
			MixCascade c = new MixCascade(m_manHostField.getText(),
										  Integer.parseInt(m_manPortField.getText()));
			m_Controller.getMixCascadeDatabase().removeElement(oldCascade);
			m_Controller.getMixCascadeDatabase().addElement(c);
			this.updateMixCascadeCombo();
			m_enterCascadeButton.setVisible(false);
		}
		catch (Exception a_e)
		{
			LogHolder.log(LogLevel.ERR, LogType.MISC, "Cannot edit cascade");
		}
	}

	/**
	 * Adds a manually entered cascade to the cascade database
	 */
	private void enterManualCascade()
	{
		try
		{
			MixCascade c = new MixCascade(m_manHostField.getText(),
										  Integer.parseInt(m_manPortField.getText()));
			m_Controller.getMixCascadeDatabase().addElement(c);
			this.updateMixCascadeCombo();
			m_enterCascadeButton.setVisible(false);
		}
		catch (Exception a_e)
		{
			LogHolder.log(LogLevel.ERR, LogType.MISC, "Cannot create cascade");
		}
	}

	public void mouseClicked(MouseEvent e)
	{
		if (e.getSource() == m_urlLabel)
		{
			String url = m_urlLabel.getText();
			int start = url.indexOf(URL_BEGIN) + URL_BEGIN.length();
			int end = url.indexOf(URL_END);
			url = url.substring(start, end);
			AbstractOS os = AbstractOS.getInstance();
			try
			{
				os.openURLInBrowser(url);
			}
			catch(Exception a_e)
			{
				LogHolder.log(LogLevel.ERR, LogType.MISC, "Error opening URL in browser");
			}
		}
		else if (e.getSource() == m_listMixCascade)
		{
			if (e.getClickCount() == 2)
			{
				int index = m_listMixCascade.locationToIndex(e.getPoint());
				MixCascade c;
				try
				{
					c = (MixCascade) m_listMixCascade.getModel().getElementAt(index);
				}
				catch (ClassCastException a_e)
				{
					return;
				}
				m_Controller.setCurrentMixCascade(c);
				m_listMixCascade.repaint();
			}
		}
	}

	public void mousePressed(MouseEvent e)
	{
	}

	public void mouseReleased(MouseEvent e)
	{
	}

	public void mouseEntered(MouseEvent e)
	{
		if (e.getSource() == m_urlLabel)
		{
			this.getRootPanel().setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		}
	}

	public void mouseExited(MouseEvent e)
	{
		if (e.getSource() == m_urlLabel)
		{
			this.getRootPanel().setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
		}
	}

	protected void onRootPanelShown()
	{
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
			if (m_listMixCascade.getLastVisibleIndex() > -1 &&
				m_listMixCascade.getSelectedIndex() > -1)
			{
				MixCascade cascade = (MixCascade) m_listMixCascade.getSelectedValue();

				this.drawServerPanel(cascade.getMixCount(), true);
				this.drawServerInfoPanel(null, null, null);
				try
				{
					InfoServiceDBEntry entry = InfoServiceHolder.getInstance().getPreferedInfoService();
					int numUsers = entry.getStatusInfo(cascade.getId(), cascade.getMixCount(),
						InfoServiceHolder.getInstance()
						.getCertificateStore()).getNrOfActiveUsers();
					m_numOfUsersLabel.setText(Integer.toString(numUsers));
				}
				catch (Exception ex)
				{

				}

				if (cascade.isUserDefined())
				{
					this.drawManualPanel(cascade.getListenerInterface(0).getHost(),
										 String.valueOf(cascade.getListenerInterface(0).getPort()),
										 false);
				}

					String interfaces = "";
					String ports = "";
					int[] portsArray = new int[cascade.getNumberOfListenerInterfaces()];

					for (int i = 0; i < cascade.getNumberOfListenerInterfaces(); i++)
					{
						if (interfaces.indexOf(cascade.getListenerInterface(i).getHost()) == -1)
						{
							interfaces += cascade.getListenerInterface(i).getHost();
						}
						portsArray[i] = cascade.getListenerInterface(i).getPort();

						if (i != cascade.getNumberOfListenerInterfaces() - 1)
						{
							interfaces += "\n";
						}

					}

					// Sort the array containing the port numbers and put the numbers into a string
					for (int i = 0; i < portsArray.length; i++)
					{
						for (int j = i + 1; j < portsArray.length; j++)
						{
							if (portsArray[i] > portsArray[j])
							{
								int tmp = portsArray[j];
								portsArray[j] = portsArray[i];
								portsArray[i] = tmp;
							}
						}
					}

					for (int i = 0; i < portsArray.length; i++)
					{
						ports += String.valueOf(portsArray[i]);
						if (i != portsArray.length - 1)
						{
							ports += ", ";
						}

					}

					m_reachableLabel.setText(interfaces);
					m_portsLabel.setText(ports);
					itemStateChanged(null);
				m_listMixCascade.repaint();
				}
		}
	}

	/**
	 * keyTyped
	 *
	 * @param e KeyEvent
	 */
	public void keyTyped(KeyEvent e)
	{
		if (e.getSource() == m_manHostField || e.getSource() == m_manPortField)
				{
			m_editCascadeButton.setVisible(true);
		}
	}

	/**
	 * keyPressed
	 *
	 * @param e KeyEvent
	 */
	public void keyPressed(KeyEvent e)
	{
				}

	/**
	 * keyReleased
	 *
	 * @param e KeyEvent
	 */
	public void keyReleased(KeyEvent e)
	{
			}

}

/**
 * Allows usage of icons in elements of the MixCascade list
 */
class CustomRenderer extends DefaultListCellRenderer
{
	public Component getListCellRendererComponent(JList list, Object value,
												  int index, boolean isSelected, boolean cellHasFocus)
	{
		JLabel l;
		Component comp = super.getListCellRendererComponent(list, value,
			index, isSelected, cellHasFocus);
		if (comp instanceof JComponent && value != null && value instanceof MixCascade)
		{
			if ( ( (MixCascade) value).isUserDefined())
				l = new JLabel( ( (MixCascade) value).getName(),
								JAPUtil.loadImageIcon("servermanuell.gif", true), LEFT);
			else
				l = new JLabel( ( (MixCascade) value).getName(),
							   JAPUtil.loadImageIcon("serverfrominternet.gif", true), LEFT);

			l.setToolTipText( ( (MixCascade) value).getName());
			if (isSelected)
			{
				l.setOpaque(true);
				l.setBackground(Color.lightGray);
			}
			JAPController c = JAPController.getInstance();
			if (((MixCascade) value) == c.getCurrentMixCascade())
			{
				l.setForeground(Color.blue);
			}
		}
		else
		{
			if (value != null)
			{
				l = new JLabel(value.toString());

			}
			else
			{
				l = new JLabel();
		}

	}

		return l;
	}
}
