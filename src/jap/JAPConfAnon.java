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

import java.net.URL;
import java.util.Enumeration;
import java.util.Observable;
import java.util.Observer;
import java.util.Vector;

import java.awt.Color;
import java.awt.Component;
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
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTextField;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import anon.infoservice.InfoServiceHolder;
import anon.infoservice.ListenerInterface;
import anon.infoservice.MixCascade;
import anon.infoservice.MixInfo;
import gui.GUIUtils;
import gui.JAPHelp;
import gui.JAPJIntField;
import gui.JAPMessages;
import gui.JAPMultilineLabel;
import gui.ServerListPanel;
import gui.dialog.JAPDialog;
import jap.forward.JAPRoutingMessage;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import platform.AbstractOS;
import javax.swing.SwingConstants;
import javax.swing.ScrollPaneConstants;

class JAPConfAnon extends AbstractJAPConfModule implements MouseListener, ActionListener,
	ListSelectionListener, ItemListener, KeyListener, Observer
{

	/** Messages */
	private static final String MSG_BUTTONEDITSHOW = JAPConfAnon.class.
		getName() + "_buttoneditshow";
	private static final String MSG_PAYCASCADE = JAPConfAnon.class.
		getName() + "_paycascade";

	private static final String URL_BEGIN = "<html><font color=blue><u>";
	private static final String URL_END = "</u></font></html>";

	private boolean bErr;

	private InfoServiceTempLayer m_infoService;

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
	private JLabel m_payLabel;

	private JButton m_manualCascadeButton;
	private JButton m_reloadCascadesButton;
	private JButton m_selectCascadeButton;
	private JButton m_enterCascadeButton;
	private JButton m_editCascadeButton;
	private JButton m_deleteCascadeButton;
	private JButton m_cancelCascadeButton;
	private JButton m_showEditPanelButton;

	private JTextField m_manHostField;
	private JTextField m_manPortField;

	private boolean mb_backSpacePressed;
	private boolean mb_manualCascadeNew;

	private String m_oldCascadeHost;
	private String m_oldCascadePort;

	protected JAPConfAnon(IJAPConfSavePoint savePoint)
	{
		super(null);
		m_Controller = JAPController.getInstance();
		m_infoService = new InfoServiceTempLayer(false);
		/* observe JAPRoutingSettings to get a notification, if connect-via-forwarder is enabled */
		JAPModel.getInstance().getRoutingSettings().addObserver(this);
	}

	public void recreateRootPanel()
	{
		Font font = getFontSetting();
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
		drawCompleteDialog();
	}

	private void drawServerPanel(int a_numberOfMixes, String a_strCascadeName, boolean a_enabled)
	{
		if (m_manualPanel != null)
		{
			pRoot.remove(m_manualPanel);
			pRoot.validate();
			m_manualPanel = null;
		}
		GridBagLayout layout = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		if (m_serverPanel == null)
		{
			m_serverPanel = new JPanel();
			m_rootPanelConstraints.gridx = 0;
			m_rootPanelConstraints.gridy = 2;
			m_rootPanelConstraints.weightx = 1;
			m_rootPanelConstraints.weighty = 0;
			m_rootPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
			m_rootPanelConstraints.fill = GridBagConstraints.BOTH;
			pRoot.add(m_serverPanel, m_rootPanelConstraints);
		}
		else
		{
			m_serverPanel.removeAll();
		}

		m_serverPanel.setLayout(layout);
		c.gridx = 0;
		c.gridy = 0;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		c.weighty = 0;
		c.insets = new Insets(5, 10, 5, 5);
		if (a_strCascadeName == null || a_strCascadeName.length() < 1)
		{
			a_strCascadeName = " ";
		}
		else if (a_strCascadeName.length() > 65)
		{
			a_strCascadeName = a_strCascadeName.substring(0, 65);
			a_strCascadeName = a_strCascadeName + "...";
		}
		JAPMultilineLabel label = new JAPMultilineLabel(JAPMessages.getString("infoAboutCascade")
			+ "\n" + a_strCascadeName);
		m_serverPanel.add(label, c);

		m_serverList = new ServerListPanel(a_numberOfMixes, a_enabled);
		m_serverList.addItemListener(this);
		c.insets = new Insets(5, 10, 5, 5);
		c.gridy = 1;
		c.insets = new Insets(2, 20, 2, 2);
		m_serverPanel.add(m_serverList, c);

	}

	private void drawServerInfoPanel(String a_operator, String a_url, String a_location)
	{
		GridBagLayout layout = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();

		if (m_serverInfoPanel == null)
		{
			m_serverInfoPanel = new JPanel();
			m_rootPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
			m_rootPanelConstraints.gridx = 0;
			m_rootPanelConstraints.gridy = 3;
			m_rootPanelConstraints.weightx = 1.0;
			m_rootPanelConstraints.weighty = 0;
			m_rootPanelConstraints.fill = GridBagConstraints.BOTH;
			pRoot.add(m_serverInfoPanel, m_rootPanelConstraints);
		}
		else
		{
			m_serverInfoPanel.removeAll();
		}
		m_serverInfoPanel.setLayout(layout);

		JLabel l = new JLabel(JAPMessages.getString("infoAboutMix"));
		c.insets = new Insets(5, 10, 5, 5);
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 0;
		c.gridwidth = 2;
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.insets = new Insets(5, 20, 5, 5);
		m_serverInfoPanel.add(l, c);

		l = new JLabel(JAPMessages.getString("mixOperator"));
		c.gridy = 1;
		c.gridwidth = 1;
		c.insets = new Insets(5, 30, 5, 5);
		m_serverInfoPanel.add(l, c);

		m_operatorLabel = new JLabel(a_operator);
		c.weightx = 1;
		c.gridx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		m_serverInfoPanel.add(m_operatorLabel, c);

		l = new JLabel(JAPMessages.getString("mixUrl"));
		c.gridx = 0;
		c.gridy = 2;
		c.weightx = 0;
		m_serverInfoPanel.add(l, c);

		m_urlLabel = new JLabel(a_url);
		m_urlLabel.addMouseListener(this);
		c.gridx = 1;
		m_serverInfoPanel.add(m_urlLabel, c);

		l = new JLabel(JAPMessages.getString("mixLocation"));
		c.gridx = 0;
		c.gridy = 3;
		m_serverInfoPanel.add(l, c);

		m_locationLabel = new JLabel(a_location);
		c.gridx = 1;
		m_serverInfoPanel.add(m_locationLabel, c);

	}

	private void drawManualPanel(String a_hostName, String a_port, boolean a_newCascade)
	{
		if (m_manualPanel != null)
		{
			pRoot.remove(m_manualPanel);
		}

		if (a_newCascade)
		{
			try
			{
				MixCascade dummyCascade = new MixCascade(JAPMessages.getString("dummyCascade"), 0);
				m_Controller.getMixCascadeDatabase().addElement(dummyCascade);
				this.updateMixCascadeCombo();
				m_listMixCascade.setSelectedIndex(m_listMixCascade.getModel().getSize() - 1);
				//m_manHostField.selectAll();
			}
			catch (Exception a_e)
			{
				JAPDialog.showErrorDialog(this.getRootPanel(), JAPMessages.getString("errorCreateCascadeDesc"),
										  LogType.MISC);
			}
		}

		m_manualPanel = new JPanel();
		GridBagLayout layout = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(5, 5, 5, 5);
		c.anchor = GridBagConstraints.NORTHWEST;
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
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		c.gridx = 1;
		c.gridy = 0;
		c.gridwidth = 3;
		m_manualPanel.add(m_manHostField, c);
		m_manPortField = new JAPJIntField(ListenerInterface.PORT_MAX_VALUE);
		m_manPortField.setText(a_port);
		c.gridy = 1;
		c.fill = c.NONE;
		m_manualPanel.add(m_manPortField, c);
		c.weightx = 0;
		c.gridy = 2;
		c.fill = c.HORIZONTAL;
		c.gridx = 2;
		c.gridwidth = 1;
		c.fill = GridBagConstraints.NONE;
		c.anchor = GridBagConstraints.NORTHEAST;

		m_editCascadeButton = new JButton(JAPMessages.getString("okButton"));
		m_editCascadeButton.addActionListener(this);
		c.gridx = 1;
		c.weightx = 1;
		m_manualPanel.add(m_editCascadeButton, c);
		m_deleteCascadeButton = new JButton(JAPMessages.getString("manualServiceDelete"));
		m_deleteCascadeButton.addActionListener(this);
		c.gridx = 3;
		c.weightx = 0;
		m_manualPanel.add(m_deleteCascadeButton, c);
		m_cancelCascadeButton = new JButton(JAPMessages.getString("cancelButton"));
		m_cancelCascadeButton.addActionListener(this);
		c.gridx = 2;
		m_manualPanel.add(m_cancelCascadeButton, c);
		m_manHostField.addKeyListener(this);
		m_manPortField.addKeyListener(this);

		if (m_serverPanel != null)
		{
			pRoot.remove(m_serverPanel);
			pRoot.remove(m_serverInfoPanel);
			m_serverPanel = null;
			m_serverInfoPanel = null;
		}

		pRoot.validate();
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
		m_listMixCascade.setFixedCellWidth(30);
		c.gridx = 0;
		c.gridy = 1;
		c.gridheight = 4;
		c.gridwidth = 1;
		c.weightx = 1.0;
		c.weighty = 1.0;
		c.fill = GridBagConstraints.BOTH;
		c.insets = new Insets(5, 5, 0, 5);
		JScrollPane scroll = new JScrollPane(m_listMixCascade);
		scroll.setHorizontalScrollBarPolicy(ScrollPaneConstants.HORIZONTAL_SCROLLBAR_NEVER);
		scroll.setMinimumSize(new Dimension(100, 100));
		m_cascadesPanel.add(scroll, c);

		JPanel panelBttns = new JPanel(new GridBagLayout());
		GridBagConstraints c1 = new GridBagConstraints();
		c1.fill = GridBagConstraints.VERTICAL;
		c1.anchor = GridBagConstraints.WEST;
		c1.gridheight = 1;
		c1.gridwidth = 1;
		c1.gridx = 0;
		c1.gridy = 0;
		c1.insets = new Insets(0, 0, 0, 10);
		m_reloadCascadesButton = new JButton(JAPMessages.getString("reloadCascades"));
		m_reloadCascadesButton.setIcon(GUIUtils.loadImageIcon(JAPConstants.IMAGE_RELOAD, true));
		m_reloadCascadesButton.setDisabledIcon(GUIUtils.loadImageIcon(JAPConstants.IMAGE_RELOAD_DISABLED, true));
		m_reloadCascadesButton.setPressedIcon(GUIUtils.loadImageIcon(JAPConstants.IMAGE_RELOAD_ROLLOVER, true));

		m_reloadCascadesButton.addActionListener(this);
		panelBttns.add(m_reloadCascadesButton, c1);

		m_selectCascadeButton = new JButton(JAPMessages.getString("selectCascade"));
		/* maybe the button must be disabled (if connect-via-forwarder is selected) */
		m_selectCascadeButton.setEnabled(!JAPModel.getInstance().getRoutingSettings().isConnectViaForwarder());
		m_selectCascadeButton.addActionListener(this);
		c1.gridx = 1;
		panelBttns.add(m_selectCascadeButton, c1);

		m_manualCascadeButton = new JButton(JAPMessages.getString("manualCascade"));
		m_manualCascadeButton.addActionListener(this);
		c1.gridx = 2;
		panelBttns.add(m_manualCascadeButton, c1);

		m_showEditPanelButton = new JButton(JAPMessages.getString(MSG_BUTTONEDITSHOW));
		m_showEditPanelButton.addActionListener(this);
		c1.gridx = 3;
		c1.weightx = 1.0;
		panelBttns.add(m_showEditPanelButton, c1);

		c.gridx = 0;
		c.gridy = 5;
		c.gridheight = 1;
		c.gridwidth = 4;
		c.weightx = 1.0;
		c.weighty = 0;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.insets = new Insets(0, 5, 0, 0);

		m_cascadesPanel.add(panelBttns, c);

		c.insets = new Insets(5, 20, 0, 5);

		l = new JLabel(JAPMessages.getString("numOfUsersOnCascade"));
		c.gridx = 2;
		c.gridy = 1;
		c.weightx = 0;
		c.gridwidth = 1;
		c.fill = GridBagConstraints.NONE;
		m_cascadesPanel.add(l, c);

		c.insets = new Insets(5, 5, 0, 5);
		m_numOfUsersLabel = new JLabel("");
		c.gridx = 3;
		c.gridy = 1;
		c.weightx = 0;
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
		c.weightx = 0;
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
		c.weightx = 0;
		c.fill = GridBagConstraints.HORIZONTAL;
		m_cascadesPanel.add(m_portsLabel, c);

		c.insets = new Insets(5, 20, 0, 5);
		c.gridy = 4;
		c.gridx = 2;
		c.gridwidth = 2;
		m_payLabel = new JLabel("");
		m_cascadesPanel.add(m_payLabel, c);

		c.insets = new Insets(5, 5, 0, 5);
		c.gridwidth = 1;
		c.gridx = 3;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridy = 5;
		m_cascadesPanel.add(new JLabel("                                               "), c);

		m_rootPanelConstraints.gridx = 0;
		m_rootPanelConstraints.gridy = 0;
		m_rootPanelConstraints.insets = new Insets(10, 10, 0, 10);
		m_rootPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
		m_rootPanelConstraints.fill = GridBagConstraints.BOTH;
		m_rootPanelConstraints.weightx = 1.0;
		m_rootPanelConstraints.weighty = 1.0;

		pRoot.add(m_cascadesPanel, m_rootPanelConstraints);

		m_rootPanelConstraints.weightx = 1;
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
		m_cascadesPanel = null;
		m_serverPanel = null;
		m_serverInfoPanel = null;
		m_manualPanel = null;
		pRoot = getRootPanel();
		pRoot.removeAll();
		pRoot.setLayout(m_rootPanelLayout);
		m_rootPanelConstraints.anchor = GridBagConstraints.NORTHWEST;

		drawCascadesPanel();
		drawServerPanel(3, "", false);
		drawServerInfoPanel("", "", "");
	}

	public void itemStateChanged(ItemEvent e)
	{
			int server = m_serverList.getSelectedIndex();
		 MixCascade cascade = (MixCascade) m_listMixCascade.getSelectedValue();
		 if (cascade != null && !cascade.isUserDefined())
		 {
		  String selectedMixId = (String) cascade.getMixIds().elementAt(server);
		  try
		  {
		   m_operatorLabel.setText(m_infoService.getOperator(selectedMixId));
		   m_operatorLabel.setToolTipText(m_infoService.getOperator(selectedMixId));
		   m_locationLabel.setText(m_infoService.getLocation(selectedMixId));
		   m_urlLabel.setText(URL_BEGIN + m_infoService.getUrl(selectedMixId)
				  + URL_END);
		  }
		  catch (Exception ex)
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
		/** @todo Do this in the event thread only! */
		LogHolder.log(LogLevel.DEBUG, LogType.GUI, "-start");
		Enumeration it = m_Controller.getMixCascadeDatabase().elements();
		DefaultListModel listModel = new DefaultListModel();
		CustomRenderer cr = new CustomRenderer();
		m_listMixCascade.setCellRenderer(cr);
		MixCascade currentCascade = JAPController.getInstance().getCurrentMixCascade();
		boolean bCurrentAlreadyAdded = false;
		while (it.hasMoreElements())
		{
			MixCascade cascade = (MixCascade) it.nextElement();
			//if (cascade.isUserDefined())
			{
				listModel.addElement(cascade);
			}
			//else
			//{
			//	listModel.addElement(cascade);
			//}
			if (cascade.equals(currentCascade))
			{
				bCurrentAlreadyAdded = true;
			}

		}

		LogHolder.log(LogLevel.DEBUG, LogType.GUI, "-added All other Items");
		if (!bCurrentAlreadyAdded)
		{
			listModel.addElement(currentCascade);
		}
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
		//m_tfMixHost.setText(JAPConstants.DEFAULT_ANON_HOST);
		//m_tfMixPortNumber.setText(Integer.toString(JAPConstants.DEFAULT_ANON_PORT_NUMBER));

	}

	public boolean onOkPressed()
	{
		return true;

	}

	public void onUpdateValues()
	{
		updateMixCascadeCombo();
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
				//Update the temporary infoservice database
				updateFromInfoservice();
				updateMixCascadeCombo();

				LogHolder.log(LogLevel.DEBUG, LogType.GUI, "JAPConf: finished updateMixCascadeCombo()");

				getRootPanel().setCursor(c);

				if (m_Controller.getMixCascadeDatabase().size() == 0)
				{
					if (!JAPModel.isSmallDisplay() && bErr)
					{
						JAPDialog.showErrorDialog(getRootPanel(),
												  JAPMessages.getString("settingsNoServersAvailable"),
												  LogType.MISC);
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
		m_payLabel.setText("");
		drawServerPanel(3, "", false);

		drawServerInfoPanel("", "", "");
		m_serverInfoPanel.setEnabled(false);

	}

	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == m_cancelCascadeButton)
		{
			if (mb_manualCascadeNew)
			{
				this.deleteManualCascade();
			}
			else
			{
				m_manHostField.setText(m_oldCascadeHost);
				m_manPortField.setText(m_oldCascadePort);
				m_cancelCascadeButton.setEnabled(false);
			}
		}
		else if (e.getSource() == m_reloadCascadesButton)
		{
			fetchCascades(false);
		}
		else if (e.getSource() == m_selectCascadeButton)
		{
			MixCascade newCascade = null;
			try
			{
				newCascade = (MixCascade) m_listMixCascade.getSelectedValue();
			}
			catch (Exception ex)
			{
				newCascade = null;
			}
			if (newCascade != null)
			{
				m_Controller.setCurrentMixCascade(newCascade);
				m_selectCascadeButton.setEnabled(false);
				m_listMixCascade.repaint();
			}
		}
		else if (e.getSource() == m_manualCascadeButton)
		{
			drawManualPanel(null, null, true);
			mb_manualCascadeNew = true;
			m_deleteCascadeButton.setEnabled(false);
			m_cancelCascadeButton.setEnabled(true);
		}
		else if (e.getSource() == m_enterCascadeButton)
		{
			this.enterManualCascade();
		}
		else if (e.getSource() == m_editCascadeButton)
		{
			this.editManualCascade();
		}
		else if (e.getSource() == m_deleteCascadeButton)
		{
			this.deleteManualCascade();
		}
		else if (e.getSource() == m_showEditPanelButton)
		{
			MixCascade cascade = (MixCascade) m_listMixCascade.getSelectedValue();
			this.drawManualPanel(cascade.getListenerInterface(0).getHost(),
								 String.valueOf(cascade.getListenerInterface(0).getPort()),
								 false);
			mb_manualCascadeNew = false;
			m_deleteCascadeButton.setEnabled(true);
			m_cancelCascadeButton.setEnabled(false);
			m_oldCascadeHost = m_manHostField.getText();
			m_oldCascadePort = m_manPortField.getText();

		}

	}

	/**
	 * Edits a manually configured cascade
	 */
	private void editManualCascade()
	{
		boolean valid = true;
		try
		{
			MixCascade oldCascade = (MixCascade) m_listMixCascade.getSelectedValue();
			MixCascade c = new MixCascade(m_manHostField.getText(),
										  Integer.parseInt(m_manPortField.getText()));
			//Check if this cascade already exists
			Vector db = m_Controller.getMixCascadeDatabase();
			for (int i = 0; i < db.size(); i++)
			{
				MixCascade mc = (MixCascade) db.elementAt(i);
				if (mc.getListenerInterface(0).getHost().equalsIgnoreCase(
					c.getListenerInterface(0).getHost()))
				{
					if (mc.getListenerInterface(0).getPort() ==
						c.getListenerInterface(0).getPort())
					{
						valid = false;
					}
				}
			}

			if (valid)
			{
				m_Controller.getMixCascadeDatabase().addElement(c);
				if (m_Controller.getCurrentMixCascade().equals(oldCascade))
				{
					m_Controller.setCurrentMixCascade(c);
					JAPDialog.showMessageDialog(this.getRootPanel(),
												JAPMessages.getString("activeCascadeEdited"));
				}
				m_Controller.getMixCascadeDatabase().removeElement(oldCascade);

				this.updateMixCascadeCombo();
				m_listMixCascade.setSelectedIndex(m_listMixCascade.getModel().getSize() - 1);
			}
			else
			{
				JAPDialog.showErrorDialog(this.getRootPanel(), JAPMessages.getString("cascadeExistsDesc"),
										  LogType.MISC);
			}
		}
		catch (Exception a_e)
		{
			LogHolder.log(LogLevel.ERR, LogType.MISC, "Cannot edit cascade");
			JAPDialog.showErrorDialog(this.getRootPanel(), JAPMessages.getString("errorCreateCascadeDesc"),
									  LogType.MISC);

		}
	}

	/**
	 * Deletes a manually configured cascade
	 */
	private void deleteManualCascade()
	{
		try
		{
			MixCascade cascade = (MixCascade) m_listMixCascade.getSelectedValue();
			if (m_Controller.getCurrentMixCascade().equals(cascade))
			{
				JAPDialog.showErrorDialog(this.getRootPanel(),
										  JAPMessages.getString("activeCascadeDelete"),
										  LogType.MISC);
			}
			else
			{
				m_Controller.getMixCascadeDatabase().removeElement(cascade);
				this.updateMixCascadeCombo();
			}
		}
		catch (Exception a_e)
		{
			LogHolder.log(LogLevel.ERR, LogType.MISC, "Cannot delete cascade");
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
			m_listMixCascade.setSelectedIndex(m_listMixCascade.getModel().getSize() - 1);
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
				os.openURL(new URL(url));
			}
			catch (Exception a_e)
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
		//Register help context
		JAPHelp.getInstance().getContextObj().setContext("services");
		if (!JAPModel.isInfoServiceDisabled())
		{
			if (!m_infoService.isFilled())
			{
				m_reloadCascadesButton.setEnabled(false);
				Runnable doIt = new Runnable()
				{
					public void run()
					{
						Cursor c = getRootPanel().getCursor();
						getRootPanel().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

						m_Controller.fetchMixCascades(bErr);
						//Update the temporary infoservice database
						updateFromInfoservice();
						updateMixCascadeCombo();

						getRootPanel().setCursor(c);

						if (m_Controller.getMixCascadeDatabase().size() == 0)
						{
							if (!JAPModel.isSmallDisplay() && bErr)
							{
								JAPDialog.showMessageDialog(getRootPanel(),
									JAPMessages.getString("settingsNoServersAvailable"),
									JAPMessages.getString("settingsNoServersAvailableTitle"));
							}
							//No mixcascades returned by Infoservice
							deactivate();
						}
						else
						{
							// show a window containing all available cascades
							m_listMixCascade.setEnabled(true);
						}
						m_reloadCascadesButton.setEnabled(true);
					}
				};
				Thread t = new Thread(doIt);
				t.start();
				try
				{
					m_listMixCascade.setSelectedValue(m_Controller.getCurrentMixCascade(), true);
					valueChanged(new ListSelectionEvent(m_listMixCascade, 0,
						m_listMixCascade.getModel().getSize(), false));
				}
				catch (Exception e)
				{
				}
			}
		}
	}

	/**
	 * Handles the selection of a cascade
	 * @param e ListSelectionEvent
	 */
	public void valueChanged(ListSelectionEvent e)
	{
			m_showEditPanelButton.setEnabled(false);
		 //Object source = e.getSource();
		 if (!e.getValueIsAdjusting())
		 {
		  if (m_listMixCascade.getSelectedIndex() > -1)
		  {
		   MixCascade cascade;
		   String cascadeId;

		   cascade = (MixCascade) m_listMixCascade.getSelectedValue();
		   if (cascade == null)
		   {
			   // no cascade is available and selected
			   m_showEditPanelButton.setEnabled(false);
			   m_selectCascadeButton.setEnabled(false);
			   m_showEditPanelButton.setEnabled(false);
			   return;
		   }
		   cascadeId = cascade.getId();


		   if (m_infoService != null)
		   {
			if (m_infoService.getNumOfMixes(cascadeId) == -1)
			{
			 drawServerPanel(3, "", false);
			}
			else
			{
			 drawServerPanel(m_infoService.getNumOfMixes(cascadeId), cascade.getName(), true);
			}
			m_numOfUsersLabel.setText(m_infoService.getNumOfUsers(cascadeId));
			m_reachableLabel.setText(m_infoService.getHosts(cascadeId));
			m_portsLabel.setText(m_infoService.getPorts(cascadeId));
					if (m_infoService.isPay(cascadeId))
					{
						m_payLabel.setText(JAPMessages.getString(MSG_PAYCASCADE));
					}
					else
					{
						m_payLabel.setText("");
					}
		   }
		   drawServerInfoPanel(null, null, null);

		   if (cascade.isUserDefined())
		   {
			m_showEditPanelButton.setEnabled(true);
		   }

		   if (m_Controller.getCurrentMixCascade().getName().equalsIgnoreCase(cascade.getName()))
		   {
			m_selectCascadeButton.setEnabled(false);
		   }
		   else
		   {
			m_selectCascadeButton.setEnabled(true);
		   }
		   itemStateChanged(null);
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
		if (e.getSource() == m_manPortField)
		{
			char theKey = e.getKeyChar();
			if ( ( (int) theKey < 48 || (int) theKey > 57) && !mb_backSpacePressed)
			{
				e.consume();
			}
		}

	}

	/**
	 * keyPressed
	 *
	 * @param e KeyEvent
	 */
	public void keyPressed(KeyEvent e)
	{
		if (e.getSource() == m_manHostField || e.getSource() == m_manPortField)
		{
			m_editCascadeButton.setVisible(true);
			m_cancelCascadeButton.setEnabled(true);
		}
		if (e.getSource() == m_manPortField)
		{
			if (e.getKeyCode() == e.VK_BACK_SPACE)
			{
				mb_backSpacePressed = true;
			}
			else
			{
				mb_backSpacePressed = false;
			}
		}
	}

	/**
	 * keyReleased
	 *
	 * @param e KeyEvent
	 */
	public void keyReleased(KeyEvent e)
	{
	}

	/**
	 * This is the observer implementation. We observe the forwarding system to enabled / disable
	 * the mixselection button. The button has to be disabled, if connect-via-forwarder is enabled
	 * because, selecting a mixcascade is not possible via the "normal" way.
	 *
	 * @param a_notifier The observed Object (JAPRoutingSettings at the moment).
	 * @param a_message The reason of the notification, should be a JAPRoutingMessage.
	 *
	 */
	public void update(Observable a_notifier, Object a_message)
	{
		try
		{
			if (a_notifier == JAPModel.getInstance().getRoutingSettings())
			{
				if ( ( (JAPRoutingMessage) (a_message)).getMessageCode() ==
					JAPRoutingMessage.CLIENT_SETTINGS_CHANGED)
				{
					/* the forwarding-client settings were changed -> enable or disable the mixcascade
					 * selection button
					 */
					JButton mixcascadeSelectionButton = m_selectCascadeButton;
					if (mixcascadeSelectionButton != null)
					{
						mixcascadeSelectionButton.setEnabled(!JAPModel.getInstance().getRoutingSettings().
							isConnectViaForwarder());
					}
				}
			}
		}
		catch (Exception e)
		{
			/* should not happen, but better than throwing a runtime exception */
		}
	}

	/**
	 * Refresh the temporary infoservice database
	 */
	private void updateFromInfoservice()
	{
		m_infoService = new InfoServiceTempLayer(true);
	}
}

/*
 * Allows usage of icons in elements of the MixCascade list
 */
final class CustomRenderer extends DefaultListCellRenderer
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
			{
				l = new JLabel( ( (MixCascade) value).getName(),
							   GUIUtils.loadImageIcon(JAPConstants.IMAGE_CASCADE_MANUELL, true), LEFT);
			}
			else if ( ( (MixCascade) value).isPayment())
			{
				l = new JLabel( ( (MixCascade) value).getName(),
							   GUIUtils.loadImageIcon(JAPConstants.IMAGE_CASCADE_PAYMENT, true), LEFT);
			}
			else
			{
				l = new JLabel( ( (MixCascade) value).getName(),
							   GUIUtils.loadImageIcon(JAPConstants.IMAGE_CASCADE_INTERNET, true), LEFT);
			}

			l.setToolTipText( ( (MixCascade) value).getName());
			if (isSelected)
			{
				l.setOpaque(true);
				l.setBackground(Color.lightGray);
			}
			JAPController c = JAPController.getInstance();
			Font f = l.getFont();
			if ( ( (MixCascade) value).equals(c.getCurrentMixCascade()))
			{
				l.setFont(new Font(f.getName(), Font.BOLD, f.getSize()));
			}
			else
			{
				l.setFont(new Font(f.getName(), Font.PLAIN, f.getSize()));
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

/**
 * Temporary image of relevant infoservice entries. For better response time
 * of the GUI.
 */
final class InfoServiceTempLayer
{
	private Vector m_Cascades;
	private Vector m_Mixes;
	private boolean m_isFilled = false;

	public InfoServiceTempLayer(boolean a_autoFill)
	{
		m_Cascades = new Vector();
		m_Mixes = new Vector();
		if (a_autoFill)
		{
			this.fill();
		}
	}

	public boolean isFilled()
	{
		return m_isFilled;
	}

	/**
	 * Fills the temporary database by requesting info from the infoservice.
	 */
	public void fill()
	{
		m_Mixes = new Vector();
		m_Cascades = new Vector();

		try
		{
			Vector c = InfoServiceHolder.getInstance().getMixCascades();
			for (int j = 0; j < c.size(); j++)
			{
				MixCascade cascade = (MixCascade) c.elementAt(j);
				/* fetch the current cascade state */
				cascade.fetchCurrentStatus();
				//Get cascade id
				String id = cascade.getId();
				//Get number of mixes in cascade
				int numOfMixes = cascade.getNumberOfMixes();
				// Get the number of users on the cascade
				String numOfUsers = Integer.toString(cascade.getCurrentStatus().getNrOfActiveUsers());
				// Get hostnames and ports
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
					for (int k = i + 1; k < portsArray.length; k++)
					{
						if (portsArray[i] > portsArray[k])
						{
							int tmp = portsArray[k];
							portsArray[k] = portsArray[i];
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
				m_Cascades.addElement(new TempCascade(id, numOfUsers, interfaces, ports, numOfMixes,
					cascade.isPayment()));
				//Get mixes in cascade
				Vector mixIds = cascade.getMixIds();
				for (int k = 0; k < mixIds.size(); k++)
				{
					String mixId = (String) mixIds.elementAt(k);
					MixInfo mixInfo =
						InfoServiceHolder.getInstance().getMixInfo(mixId);
					m_Mixes.addElement(new TempMix(mixId, mixInfo.getServiceOperator().getOrganisation(),
						mixInfo.getServiceOperator().getUrl(),
						mixInfo.getServiceLocation().getCity() + ", " +
						mixInfo.getServiceLocation().getCountry()));

				}
			}
		}
		catch (Exception a_e)
		{
		}

		m_isFilled = true;
	}

	/**
	 * Get the number of mixes in a cascade.
	 * @param a_cascadeId String
	 * @return int
	 */
	public int getNumOfMixes(String a_cascadeId)
	{
		for (int i = 0; i < m_Cascades.size(); i++)
		{
			if ( ( (TempCascade) m_Cascades.elementAt(i)).getId().equalsIgnoreCase(a_cascadeId))
			{
				return ( (TempCascade) m_Cascades.elementAt(i)).getNumOfMixes();
			}
		}
		return -1;

	}

	/**
	 * Get the number of users in a cascade as a String.
	 * @param a_cascadeId String
	 * @return String
	 */
	public String getNumOfUsers(String a_cascadeId)
	{
		for (int i = 0; i < m_Cascades.size(); i++)
		{
			if ( ( (TempCascade) m_Cascades.elementAt(i)).getId().equalsIgnoreCase(a_cascadeId))
			{
				return ( (TempCascade) m_Cascades.elementAt(i)).getNumOfUsers();
			}
		}
		return "N/A";
	}

	/**
	 * Get the hostnames of a cascade.
	 * @param a_cascadeId String
	 * @return String
	 */
	public String getHosts(String a_cascadeId)
	{
		for (int i = 0; i < m_Cascades.size(); i++)
		{
			if ( ( (TempCascade) m_Cascades.elementAt(i)).getId().equalsIgnoreCase(a_cascadeId))
			{
				return ( (TempCascade) m_Cascades.elementAt(i)).getHosts();
			}
		}
		return "N/A";
	}

	/**
	 * Get the ports of a cascade.
	 * @param a_cascadeId String
	 * @return String
	 */
	public String getPorts(String a_cascadeId)
	{
		for (int i = 0; i < m_Cascades.size(); i++)
		{
			if ( ( (TempCascade) m_Cascades.elementAt(i)).getId().equalsIgnoreCase(a_cascadeId))
			{
				return ( (TempCascade) m_Cascades.elementAt(i)).getPorts();
			}
		}
		return "N/A";
	}

	/**
	 * Get the operator name of a cascade.
	 * @param a_mixId String
	 * @return String
	 */
	public String getOperator(String a_mixId)
	{
		for (int i = 0; i < m_Mixes.size(); i++)
		{
			if ( ( (TempMix) m_Mixes.elementAt(i)).getId().equalsIgnoreCase(a_mixId))
			{
				return ( (TempMix) m_Mixes.elementAt(i)).getOperator();
			}
		}
		return "N/A";
	}

	/**
	 * Get the web URL of a cascade.
	 * @param a_mixId String
	 * @return String
	 */
	public String getUrl(String a_mixId)
	{
		for (int i = 0; i < m_Mixes.size(); i++)
		{
			if ( ( (TempMix) m_Mixes.elementAt(i)).getId().equalsIgnoreCase(a_mixId))
			{
				return ( (TempMix) m_Mixes.elementAt(i)).getUrl();
			}
		}
		return "N/A";
	}

	/**
	 * Get the location of a cascade.
	 * @param a_mixId String
	 * @return String
	 */
	public String getLocation(String a_mixId)
	{
		for (int i = 0; i < m_Mixes.size(); i++)
		{
			if ( ( (TempMix) m_Mixes.elementAt(i)).getId().equalsIgnoreCase(a_mixId))
			{
				return ( (TempMix) m_Mixes.elementAt(i)).getLocation();
			}
		}
		return "N/A";
	}

	/**
	 * Get payment property of a cascade.
	 * @param a_cascadeId String
	 * @return boolean
	 */
	public boolean isPay(String a_cascadeId)
	{
		for (int i = 0; i < m_Cascades.size(); i++)
		{
			if ( ( (TempCascade) m_Cascades.elementAt(i)).getId().equalsIgnoreCase(a_cascadeId))
			{
				return ( (TempCascade) m_Cascades.elementAt(i)).isPay();
			}
		}
		return false;
	}

}

/**
 *
 * Cascade database entry for the temporary infoservice.
 */
final class TempCascade
{
	private String m_id;
	private String m_users;
	private String m_ports;
	private String m_hosts;
	private boolean m_bIsPay = false;
	private int m_numOfMixes;

	public TempCascade(String a_id, String a_numOfUsers, String a_hosts, String a_ports, int a_numOfMixes,
					   boolean a_isPay)
	{
		m_id = a_id;
		m_users = a_numOfUsers;
		m_hosts = a_hosts;
		m_ports = a_ports;
		m_numOfMixes = a_numOfMixes;
		m_bIsPay = a_isPay;
	}

	public String getId()
	{
		return m_id;
	}

	public boolean isPay()
	{
		return m_bIsPay;
	}

	public int getNumOfMixes()
	{
		return m_numOfMixes;
	}

	public String getNumOfUsers()
	{
		return m_users;
	}

	public String getPorts()
	{
		return m_ports;
	}

	public String getHosts()
	{
		return m_hosts;
	}

}

/**
 * Mix database entry for the temporary cascade
 */
final class TempMix
{
	private String m_id;
	private String m_operator;
	private String m_url;
	private String m_location;

	public TempMix(String a_id, String a_operator, String a_url, String a_location)
	{
		m_id = a_id;
		m_operator = a_operator;
		m_url = a_url;
		m_location = a_location;
	}

	public String getId()
	{
		return m_id;
	}

	public String getOperator()
	{
		return m_operator;
	}

	public String getUrl()
	{
		return m_url;
	}

	public String getLocation()
	{
		return m_location;
	}

}
