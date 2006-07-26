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

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Date;
import java.util.Enumeration;
import java.util.Hashtable;
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
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.SwingUtilities;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import anon.crypto.JAPCertificate;
import anon.crypto.X509SubjectAlternativeName;
import anon.infoservice.Database;
import anon.infoservice.DatabaseMessage;
import anon.infoservice.InfoServiceHolder;
import anon.infoservice.ListenerInterface;
import anon.infoservice.MixCascade;
import anon.infoservice.MixInfo;
import anon.infoservice.ServiceLocation;
import anon.infoservice.ServiceOperator;
import anon.infoservice.StatusInfo;
/* for opening a CertDetailsDialog with the certificate data of the selected server*/
import anon.util.Util;
import gui.CertDetailsDialog;
import gui.CountryMapper;
import gui.GUIUtils;
import gui.JAPHelp;
import gui.JAPJIntField;
import gui.JAPMessages;
import gui.JAPMultilineLabel;
import gui.MapBox;
import gui.ServerListPanel;
import gui.dialog.JAPDialog;
import jap.forward.JAPRoutingMessage;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import platform.AbstractOS;
import anon.crypto.SignatureVerifier;

class JAPConfAnon extends AbstractJAPConfModule implements MouseListener, ActionListener,
	ListSelectionListener, ItemListener, KeyListener, Observer
{
	private static final String MSG_LABEL_CERTIFICATE = JAPConfAnon.class.getName() + "_certificate";
	private static final String MSG_LABEL_EMAIL = JAPConfAnon.class.getName() + "_labelEMail";
	private static final String MSG_REALLY_DELETE = JAPConfAnon.class.getName() + "_reallyDelete";


	private static final Insets SMALL_BUTTON_MARGIN = new Insets(1, 1, 1, 1);

	/** Messages */
	private static final String MSG_BUTTONEDITSHOW = JAPConfAnon.class.
		getName() + "_buttoneditshow";
	private static final String MSG_PAYCASCADE = JAPConfAnon.class.
		getName() + "_paycascade";

	private static final String URL_BEGIN = "<html><font color=blue><u>";
	private static final String URL_END = "</u></font></html>";
	private static final String RED_BEGIN = "<font color=red>";
	private static final String RED_END = "</font>";

	private final Object MIX_COMBO_UPDATE_LOCK = new Object();
	private boolean m_bUpdateServerPanel = true;

	private InfoServiceTempLayer m_infoService;

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
	private JLabel m_emailLabel;
	private JLabel m_urlLabel;
	private JLabel m_locationLabel;
	private JLabel m_payLabel;
	private JLabel m_viewCertLabel;

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

	private boolean m_mapShown = false;
	private boolean m_observablesRegistered = false;
	private final Object LOCK_OBSERVABLE = new Object();

	/** the Certificate of the selected Mix-Server */
	private JAPCertificate m_serverCert;
	private MixInfo m_serverInfo;

	private Vector m_locationCoordinates;

	protected JAPConfAnon(IJAPConfSavePoint savePoint)
	{
		super(null);
		m_Controller = JAPController.getInstance();
		m_infoService = new InfoServiceTempLayer(false);
	}

	public void recreateRootPanel()
	{
		Font font = getFontSetting();
		m_listMixCascade = new JList();
		m_listMixCascade.addListSelectionListener(this);
		m_listMixCascade.addMouseListener(this);
		m_listMixCascade.setFont(font);

		m_listMixCascade.setEnabled(true);
		drawCompleteDialog();
	}

	private void drawServerPanel(int a_numberOfMixes, String a_strCascadeName, boolean a_enabled,
								 int a_selectedIndex)
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
		JLabel label = new JLabel(JAPMessages.getString("infoAboutCascade"));
		label.setFont(new Font(label.getFont().getName(), Font.BOLD, label.getFont().getSize() + 2));
		m_serverPanel.add(label, c);

		c.gridy = 1;
		m_serverPanel.add(new JLabel(a_strCascadeName), c);

		m_serverList = new ServerListPanel(a_numberOfMixes, a_enabled, a_selectedIndex);
		m_serverList.addItemListener(this);
		c.gridy = 2;
		c.insets = new Insets(2, 20, 2, 2);
		m_serverPanel.add(m_serverList, c);

	}

	private void drawServerInfoPanel()
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
		l.setFont(new Font(l.getFont().getName(), Font.BOLD, l.getFont().getSize() + 2));
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

		m_operatorLabel = new JLabel();
		c.weightx = 1;
		c.gridx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		m_serverInfoPanel.add(m_operatorLabel, c);

		l = new JLabel(JAPMessages.getString(MSG_LABEL_EMAIL));
		c.gridx = 0;
		c.gridy++;
		c.weightx = 0;
		c.insets = new Insets(5, 30, 5, 5);
		m_serverInfoPanel.add(l, c);

		m_emailLabel = new JLabel();
		c.weightx = 1;
		c.gridx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		m_serverInfoPanel.add(m_emailLabel, c);

		l = new JLabel(JAPMessages.getString("mixUrl"));
		c.gridx = 0;
		c.gridy++;
		c.weightx = 0;
		m_serverInfoPanel.add(l, c);

		m_urlLabel = new JLabel();
		m_urlLabel.addMouseListener(this);
		c.gridx = 1;
		m_serverInfoPanel.add(m_urlLabel, c);

		l = new JLabel(JAPMessages.getString("mixLocation"));
		c.gridx = 0;
		c.gridy++;
		m_serverInfoPanel.add(l, c);

		m_locationLabel = new JLabel();
		m_locationLabel.addMouseListener(this);
		c.gridx = 1;
		m_serverInfoPanel.add(m_locationLabel, c);

		l = new JLabel(JAPMessages.getString(MSG_LABEL_CERTIFICATE) + ":");
		c.gridx = 0;
		c.gridy++;
		m_serverInfoPanel.add(l, c);

		m_viewCertLabel = new JLabel();
		m_viewCertLabel.addMouseListener(this);
		c.gridx = 1;
		m_serverInfoPanel.add(m_viewCertLabel, c);

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
				Database.getInstance(MixCascade.class).update(dummyCascade);
				this.updateMixCascadeCombo();
				m_listMixCascade.setSelectedValue(dummyCascade, true);
				//m_manHostField.selectAll();
			}
			catch (Exception a_e)
			{
				JAPDialog.showErrorDialog(this.getRootPanel(), JAPMessages.getString("errorCreateCascadeDesc"),
										  LogType.MISC);
				return;
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
		//c.weightx = 1;
		m_manualPanel.add(m_editCascadeButton, c);
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
		m_rootPanelConstraints.weighty = 1;
		m_rootPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
		m_rootPanelConstraints.fill = GridBagConstraints.BOTH;
		pRoot.add(m_manualPanel, m_rootPanelConstraints);
		pRoot.validate();
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
		panelBttns.add(m_showEditPanelButton, c1);

		m_deleteCascadeButton = new JButton(JAPMessages.getString("manualServiceDelete"));
		m_deleteCascadeButton.addActionListener(this);
		c1.gridx = 4;
		c1.weightx = 1.0;
		panelBttns.add(m_deleteCascadeButton, c1);



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
		drawServerPanel(3, "", false, 0);
		drawServerInfoPanel();
	}

	public synchronized void itemStateChanged(ItemEvent e)
	{
		int server = m_serverList.getSelectedIndex();

		MixCascade cascade = (MixCascade) m_listMixCascade.getSelectedValue();
		String selectedMixId = null;

		if (cascade != null)
		{
			selectedMixId = (String) cascade.getMixIds().elementAt(server);
		}

		String operator = m_infoService.getOperator(selectedMixId);
		if (operator != null && operator.length() > 82)
		{
			operator = operator.substring(0, 82) + "...";
		}
		m_operatorLabel.setText(operator);
		m_operatorLabel.setToolTipText(m_infoService.getOperator(selectedMixId));

		m_emailLabel.setText(m_infoService.getEMail(selectedMixId));



		m_locationCoordinates = m_infoService.getCoordinates(selectedMixId);
		if (m_locationCoordinates != null)
		{
			m_locationLabel.setText(URL_BEGIN + m_infoService.getLocation(selectedMixId) + URL_END);
		}
		else
		{
			m_locationLabel.setText(m_infoService.getLocation(selectedMixId));
		}

		m_urlLabel.setText(m_infoService.getUrl(selectedMixId));

		m_serverInfo = m_infoService.getMixInfo(selectedMixId);
		if(m_serverInfo != null)
		{
			m_serverCert = m_serverInfo.getMixCertificate();
		}
		/*if (m_serverCert == null && cascade != null && server == 0)
		{
			// get the certificate for the first mix
			m_serverCert = cascade.getMixCascadeCertificate();
		}*/
		if (m_serverCert != null && m_serverInfo != null)
		{
			m_viewCertLabel.setText(
				URL_BEGIN + (isServerCertVerified() ? JAPMessages.getString(CertDetailsDialog.MSG_CERT_VERIFIED) + "," :
				RED_BEGIN + JAPMessages.getString(CertDetailsDialog.MSG_CERT_NOT_VERIFIED) + "," + RED_END) +
				(m_serverCert.getValidity().isValid(new Date()) ? " " +
				 JAPMessages.getString(CertDetailsDialog.MSG_CERTVALID) : RED_BEGIN + " " +
				 JAPMessages.getString(JAPMessages.getString(CertDetailsDialog.MSG_CERTNOTVALID)) + RED_END) +
				URL_END);
		}
		else
		{
			m_viewCertLabel.setText("N/A");
		}

		pRoot.validate();
	}

	private void updateMixCascadeCombo()
	{
		/** @todo Do this in the event thread only! */
		LogHolder.log(LogLevel.DEBUG, LogType.GUI, "-start");
		Enumeration it = Database.getInstance(MixCascade.class).getEntrySnapshotAsEnumeration();
		DefaultListModel listModel = new DefaultListModel();
		CustomRenderer cr = new CustomRenderer();
		m_listMixCascade.setCellRenderer(cr);
		MixCascade currentCascade = JAPController.getInstance().getCurrentMixCascade();
		boolean bCurrentAlreadyAdded = false;
		while (it.hasMoreElements())
		{
			MixCascade cascade = (MixCascade) it.nextElement();
			listModel.addElement(cascade);
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

		Object value = m_listMixCascade.getSelectedValue();

		synchronized (MIX_COMBO_UPDATE_LOCK)
		{
			m_bUpdateServerPanel = m_manualPanel == null;
			m_listMixCascade.setModel(listModel);
			m_listMixCascade.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

			if (value == null)
			{
				m_listMixCascade.setSelectedIndex(0);
			}
			else
			{
				m_listMixCascade.setSelectedValue(value, true);
			}
			m_bUpdateServerPanel = true;
		}
		//m_listMixCascade.setEnabled(m_listMixCascade.getModel().getSize() > 0);

		LogHolder.log(LogLevel.DEBUG, LogType.GUI, "- select First Item -- finished!");
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

	private void fetchCascades(final boolean bErr, final boolean a_bForceCascadeUpdate,
							   final boolean a_bCheckInfoServiceUpdateStatus)
	{
		m_reloadCascadesButton.setEnabled(false);
		final Component component = getRootPanel();
		Runnable doIt = new Runnable()
		{
			public void run()
			{
				// fetch available mix cascades from the Internet
				//Cursor c = getRootPanel().getCursor();
				//getRootPanel().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));

				if (a_bForceCascadeUpdate)
				{
					m_Controller.fetchMixCascades(bErr, component);
				}
				//Update the temporary infoservice database
				m_infoService.fill(a_bCheckInfoServiceUpdateStatus);
				updateMixCascadeCombo();

				//getRootPanel().setCursor(c);

				if (Database.getInstance(MixCascade.class).getNumberofEntries() == 0)
				{
					if (!JAPModel.isSmallDisplay() && false)
					{
						JAPDialog.showMessageDialog(getRootPanel(),
							JAPMessages.getString("settingsNoServersAvailable"),
							JAPMessages.getString("settingsNoServersAvailableTitle"));
					}
					//No mixcascades returned by Infoservice
					//deactivate();
				}
				else
				{
					// show a window containing all available cascades
					//m_listMixCascade.setEnabled(true);
				}
				try
				{
					//m_listMixCascade.setSelectedValue(m_Controller.getCurrentMixCascade(), true);
					/** @todo check if needed...
					if (m_manualPanel == null) // do not interrupt cascade editing...
					{
						valueChanged(new ListSelectionEvent(m_listMixCascade, 0,
							m_listMixCascade.getModel().getSize(), false));
					}*/
				}
				catch (Exception e)
				{
				}

				LogHolder.log(LogLevel.DEBUG, LogType.GUI, "Enabling reload button");
				m_reloadCascadesButton.setEnabled(true);
			}
		};
		Thread t = new Thread(doIt);
		t.start();
	}

	/**
	 * Deactivates GUI when no cascades are returned by the Infoservice
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
		drawServerPanel(3, "", false, 0);

		drawServerInfoPanel();
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
			fetchCascades(true, true, false);
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

			m_deleteCascadeButton.setEnabled(!JAPController.getInstance().getCurrentMixCascade().equals(
						 cascade));
			m_cancelCascadeButton.setEnabled(false);
			m_oldCascadeHost = m_manHostField.getText();
			m_oldCascadePort = m_manPortField.getText();
		}
	}

	private boolean isServerCertVerified()
	{
		if(m_serverInfo != null)
				{
			return m_serverInfo.getMixCertPath().verify();
			}
		return false;
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
			Vector db = Database.getInstance(MixCascade.class).getEntryList();
			for (int i = 0; i < db.size(); i++)
			{
				MixCascade mc = (MixCascade) db.elementAt(i);
				if (mc.getListenerInterface(0).getHost().equalsIgnoreCase(
					c.getListenerInterface(0).getHost()))
				{
					if (mc.getListenerInterface(0).getPort() == c.getListenerInterface(0).getPort() &&
						mc.isUserDefined())
					{
						valid = false;
					}
				}
			}

			if (valid)
			{
				Database.getInstance(MixCascade.class).update(c);
				Database.getInstance(MixCascade.class).remove(oldCascade);
				if (m_Controller.getCurrentMixCascade().equals(oldCascade))
				{
					m_Controller.setCurrentMixCascade(c);
					/**					if (m_Controller.isAnonConnected())
						 {
						  JAPDialog.showMessageDialog(this.getRootPanel(),
						   JAPMessages.getString("activeCascadeEdited"));
						 }**/
				}

				updateMixCascadeCombo();
				m_listMixCascade.setSelectedValue(c, true);
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
									  LogType.MISC, a_e);

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
				if (JAPDialog.showYesNoDialog(getRootPanel(), JAPMessages.getString(MSG_REALLY_DELETE)))
				{
					Database.getInstance(MixCascade.class).remove(cascade);
					this.updateMixCascadeCombo();
					if (m_listMixCascade.getModel().getSize() > 0)
					{
						m_listMixCascade.setSelectedIndex(0);
					}
				}
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

			Database.getInstance(MixCascade.class).update(c);
			this.updateMixCascadeCombo();
			m_listMixCascade.setSelectedValue(c, true);
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
			String url = getUrlFromLabel( (JLabel) m_urlLabel);
			if (url == null)
			{
				return;
			}
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
				if (index < 0)
				{
					return;
				}
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
				m_deleteCascadeButton.setEnabled(false);
				m_showEditPanelButton.setEnabled(false);
				m_listMixCascade.repaint();
			}
		}
		else if (e.getSource() == m_viewCertLabel)
		{
			if (m_serverCert != null && m_serverInfo != null)
			{
				CertDetailsDialog dialog = new CertDetailsDialog(getRootPanel().getParent(),
					m_serverCert.getX509Certificate(), isServerCertVerified(),
					JAPController.getInstance().getLocale(), m_serverInfo.getMixCertPath());
				dialog.pack();
				dialog.setVisible(true);
			}
		}
		else if (e.getSource() == m_locationLabel)
		{
			if (m_locationCoordinates != null && !m_mapShown)
			{
				new Thread()
				{
					public void run()
					{
						m_mapShown = true;
						getRootPanel().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
						try
						{
							new MapBox(getRootPanel(), (String) m_locationCoordinates.elementAt(0),
									   (String) m_locationCoordinates.elementAt(1), 6).setVisible(true);
						}
						catch (IOException a_e)
						{
							JAPDialog.showErrorDialog(GUIUtils.getParentWindow(getRootPanel()),
													  JAPMessages.getString(MapBox.MSG_ERROR_WHILE_LOADING),
													  LogType.NET, a_e);
						}
						getRootPanel().setCursor(Cursor.getDefaultCursor());
						m_mapShown = false;
					}
				}.start();
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
		if ( (e.getSource() == m_urlLabel && getUrlFromLabel( (JLabel) m_urlLabel) != null) ||
			(e.getSource() == m_viewCertLabel && m_serverCert != null) ||
			(e.getSource() == m_locationLabel && m_locationCoordinates != null))
		{
			if (getRootPanel().getCursor().equals(Cursor.getDefaultCursor()))
			{
				this.getRootPanel().setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			}
		}
	}

	public void mouseExited(MouseEvent e)
	{
		if (e.getSource() == m_urlLabel || e.getSource() == m_viewCertLabel ||
			e.getSource() == m_locationLabel)
		{
			if (getRootPanel().getCursor().equals(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR)))
			{
				getRootPanel().setCursor(Cursor.getDefaultCursor());
			}
		}
	}

	protected void onRootPanelShown()
	{
		synchronized (LOCK_OBSERVABLE)
		{
			if (!m_observablesRegistered)
			{
				/* register observables */
				m_Controller.addObserver(this);
				JAPModel.getInstance().getRoutingSettings().addObserver(this);
				SignatureVerifier.getInstance().getVerificationCertificateStore().addObserver(this);
				Database.getInstance(MixCascade.class).addObserver(this);
				Database.getInstance(StatusInfo.class).addObserver(this);
				Database.getInstance(MixInfo.class).addObserver(this);
				m_observablesRegistered = true;
			}
		}

		//Register help context
		JAPHelp.getInstance().getContextObj().setContext("services");
		if (!m_infoService.isFilled())
		{
			fetchCascades(false, false, true);
		}
	}
	/**
	 * Handles the selection of a cascade
	 * @param e ListSelectionEvent
	 */
	public void valueChanged(ListSelectionEvent e)
	{
		boolean bUpdateServerPanel;
		synchronized (MIX_COMBO_UPDATE_LOCK)
		{
			bUpdateServerPanel = m_bUpdateServerPanel;
		}

		if (!e.getValueIsAdjusting() && bUpdateServerPanel)
		{
			if (m_listMixCascade.getSelectedIndex() > -1)
			{
				MixCascade cascade;
				String cascadeId;

				cascade = (MixCascade) m_listMixCascade.getSelectedValue();
				int selectedMix = m_serverList.getSelectedIndex();
				if (cascade == null)
				{
					// no cascade is available and selected
					m_deleteCascadeButton.setEnabled(false);
					m_showEditPanelButton.setEnabled(false);
					m_selectCascadeButton.setEnabled(false);
					return;
				}
				cascadeId = cascade.getId();

				if (m_infoService != null)
				{
					if (cascade.getNumberOfMixes() <= 1)
					{
						drawServerPanel(3, "", false, 0);
					}
					else
					{
						drawServerPanel(cascade.getNumberOfMixes(), cascade.getName(), true, selectedMix);
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
				drawServerInfoPanel();

				if (cascade.isUserDefined())
				{
				   m_deleteCascadeButton.setEnabled(
					!JAPController.getInstance().getCurrentMixCascade().equals(cascade));
					m_showEditPanelButton.setEnabled(
					   !JAPController.getInstance().getCurrentMixCascade().equals(cascade));
				}
				else
				{
					m_deleteCascadeButton.setEnabled(false);
					m_showEditPanelButton.setEnabled(false);
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
	public void update(final Observable a_notifier, final Object a_message)
	{
		try
		{
			boolean bDatabaseChanged = false;
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
			else if (a_message != null && a_message instanceof DatabaseMessage)
			{
				DatabaseMessage message = (DatabaseMessage) a_message;
				if (message.getMessageData() instanceof MixCascade)
				{
					if (message.getMessageCode() == DatabaseMessage.ENTRY_ADDED ||
						message.getMessageCode() == DatabaseMessage.ENTRY_REMOVED ||
						message.getMessageCode() == DatabaseMessage.ALL_ENTRIES_REMOVED)
					{
						bDatabaseChanged = true;
					}
					else if (message.getMessageCode() == DatabaseMessage.ENTRY_RENEWED)
					{
						MixCascade currentCascade = (MixCascade) m_listMixCascade.getSelectedValue();
						if (currentCascade != null &&
							currentCascade.equals((MixCascade)message.getMessageData()))
						{
							bDatabaseChanged = true;
						}
					}

					if (message.getMessageCode() == DatabaseMessage.ALL_ENTRIES_REMOVED)
					{
						Database.getInstance(MixInfo.class).removeAll();
					}
					else if (message.getMessageCode() == DatabaseMessage.ENTRY_REMOVED)
					{
						try
						{
							MixCascade cascade =
								(MixCascade) ( (DatabaseMessage) a_message).getMessageData();
							m_infoService.removeCascade(cascade);
							Vector mixIDs = (cascade).getMixIds();
							for (int i = 0; i < mixIDs.size(); i++)
							{
								Database.getInstance(MixInfo.class).remove(
									(String) mixIDs.elementAt(i));
							}
						}
						catch (Exception a_e)
						{
							LogHolder.log(LogLevel.EXCEPTION, LogType.MISC, a_e);
						}
					}
					else if (message.getMessageCode() == DatabaseMessage.ENTRY_ADDED ||
							 message.getMessageCode() == DatabaseMessage.ENTRY_RENEWED)
					{
						try
						{
							MixCascade cascade = (MixCascade) ( (DatabaseMessage) a_message).getMessageData();
							MixInfo mixinfo;
							String mixId;
							m_infoService.updateCascade(cascade);

							Vector mixIDs = (cascade).getMixIds();
							for (int i = 0; i < mixIDs.size(); i++)
							{
								mixId = (String) mixIDs.elementAt(i);
								mixinfo = (MixInfo) Database.getInstance(MixInfo.class).getEntryById(mixId);
								if (!JAPModel.isInfoServiceDisabled() && !cascade.isUserDefined() &&
									(mixinfo == null || mixinfo.isFromCascade()))
								{
									MixInfo mixInfo = InfoServiceHolder.getInstance().getMixInfo(mixId);
									if (mixInfo == null)
									{
										LogHolder.log(LogLevel.NOTICE, LogType.GUI,
											"Did not get Mix info from InfoService for Mix " + mixId + "!");
										continue;
									}
									Database.getInstance(MixInfo.class).update(mixInfo);
								}
							}
						}
						catch (Exception a_e)
						{
							LogHolder.log(LogLevel.EXCEPTION, LogType.MISC, a_e);
						}
					}
				}
				else if (message.getMessageData() instanceof StatusInfo)
				{
					MixCascade currentCascade = (MixCascade) m_listMixCascade.getSelectedValue();
					if (currentCascade != null)
					{
						if (m_listMixCascade.getSelectedValue() != null &&
							currentCascade.getId().equals(
								( (StatusInfo) message.getMessageData()).getId()))
						{
							bDatabaseChanged = true;
						}
					}
				}
				else if (message.getMessageData() instanceof MixInfo)
				{
					MixCascade currentCascade = (MixCascade) m_listMixCascade.getSelectedValue();
					if (currentCascade != null)
					{
						if (m_listMixCascade.getSelectedValue() != null &&
							currentCascade.getMixIds().contains(
							((MixInfo) message.getMessageData()).getId()))
						{
							bDatabaseChanged = true;
						}
					}
				}
			}
			else if (a_notifier == JAPController.getInstance() && a_message != null)
			{
				if ( ( (JAPControllerMessage) a_message).getMessageCode() ==
					JAPControllerMessage.CURRENT_MIXCASCADE_CHANGED)
				{
					bDatabaseChanged = true;
				}
			}
			else if (a_notifier == SignatureVerifier.getInstance().getVerificationCertificateStore())
			{
				bDatabaseChanged = true;
			}

			final boolean bFinalDatabaseChanged = bDatabaseChanged;
			SwingUtilities.invokeLater(
				new Runnable()
			{
				public void run()
				{
					if (bFinalDatabaseChanged)
					{
						updateMixCascadeCombo();
					}
					/** @todo seems to be superfluous...
					if (m_manualPanel == null) // do not interrupt cascade editing...
					{
						valueChanged(new ListSelectionEvent(m_listMixCascade, 0,
							m_listMixCascade.getModel().getSize(), false));
					}*/
				}
				});
		}
		catch (Exception e)
		{
			/* should not happen, but better than throwing a runtime exception */
			LogHolder.log(LogLevel.EMERG, LogType.GUI, e);
		}
	}

	private static String getUrlFromLabel(JLabel a_urlLabel)
	{
		String url = a_urlLabel.getText();
		int start = url.indexOf(URL_BEGIN) + URL_BEGIN.length();
		int end = url.indexOf(URL_END);
		if (start < 0 || end < 0 || end <= start)
		{
			return null;
		}
		return url.substring(start, end);
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
		private Hashtable m_Cascades;
		private boolean m_isFilled = false;
		private Object LOCK_FILL = new Object();

		public InfoServiceTempLayer(boolean a_autoFill)
		{
			m_Cascades = new Hashtable();
			if (a_autoFill)
			{
				this.fill(true);
			}
		}

		public boolean isFilled()
		{
			return m_isFilled;
		}

		public synchronized void removeCascade(MixCascade a_cascade)
		{
			if (a_cascade == null)
			{
				return;
			}
			m_Cascades.remove(a_cascade.getId());
		}

		/**
		 * Adds or updates cached cascade information concerning ports and hosts.
		 * @param a_cascade the cascade that should be updated
		 */
		public synchronized void updateCascade(MixCascade a_cascade)
		{
			if (a_cascade == null)
			{
				return;
			}

			//Get cascade id
			String id = a_cascade.getId();

			// Get hostnames and ports
			String interfaces = "";
			String ports = "";
			int[] portsArray = new int[a_cascade.getNumberOfListenerInterfaces()];

			for (int i = 0; i < a_cascade.getNumberOfListenerInterfaces(); i++)
			{
				if (interfaces.indexOf(a_cascade.getListenerInterface(i).getHost()) == -1)
				{
					interfaces += a_cascade.getListenerInterface(i).getHost();
				}
				portsArray[i] = a_cascade.getListenerInterface(i).getPort();

				if (i != a_cascade.getNumberOfListenerInterfaces() - 1)
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
			m_Cascades.put(id, new TempCascade(id, interfaces, ports));
		}

		private void fill(boolean a_bCheckInfoServiceUpdateStatus)
		{
			synchronized (LOCK_FILL)
			{
				if (!fill(Database.getInstance(MixCascade.class).getEntryList(),
						  a_bCheckInfoServiceUpdateStatus))
				{
					fill(Util.toVector(JAPController.getInstance().getCurrentMixCascade()),
						 a_bCheckInfoServiceUpdateStatus);
				}
			}
		}

		/**
		 * Fills the temporary database by requesting info from the infoservice.
		 * @todo check if synchronized with update is needed!!!
		 */
		private boolean fill(Vector c, boolean a_bCheckInfoServiceUpdateStatus)
		{
			if (c == null || c.size() == 0)
			{
				return false;
			}
			synchronized (LOCK_FILL)
			{
				m_Cascades = new Hashtable();

				for (int j = 0; j < c.size(); j++)
				{
					MixCascade cascade = (MixCascade) c.elementAt(j);
					// update hosts and ports
					updateCascade(cascade);
				}

				for (int j = 0; j < c.size(); j++)
				{
					MixCascade cascade = (MixCascade) c.elementAt(j);
					/* fetch the current cascade state */
					if (!cascade.isUserDefined() &&
						(!a_bCheckInfoServiceUpdateStatus || !JAPModel.isInfoServiceDisabled()))
					{
						Database.getInstance(StatusInfo.class).update(cascade.fetchCurrentStatus());
					}
					// update hosts and ports
					updateCascade(cascade);
				}
				for (int j = 0; j < c.size(); j++)
				{
					MixCascade cascade = (MixCascade) c.elementAt(j);
					//Get mixes in cascade
					/*if (cascade.isUserDefined())
						  {
					 continue;
						  }*/
					// update MixInfo for each mix in cascade
					update(Database.getInstance(MixCascade.class),
						   new DatabaseMessage(DatabaseMessage.ENTRY_ADDED, cascade));
				}

				m_isFilled = true;
			}
			return true;
		}

		/**
		 * Get the number of users in a cascade as a String.
		 * @param a_cascadeId String
		 * @return String
		 */
		public String getNumOfUsers(String a_cascadeId)
		{
			StatusInfo statusInfo = getStatusInfo(a_cascadeId);
			if (statusInfo != null)
			{
				return "" + statusInfo.getNrOfActiveUsers();
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
			TempCascade cascade = (TempCascade) m_Cascades.get(a_cascadeId);
			if (cascade == null)
			{
				return "N/A";
			}

			return cascade.getHosts();
		}

		/**
		 * Get the ports of a cascade.
		 * @param a_cascadeId String
		 * @return String
		 */
		public String getPorts(String a_cascadeId)
		{
			TempCascade cascade = (TempCascade) m_Cascades.get(a_cascadeId);
			if (cascade == null)
			{
				return "N/A";
			}
			return cascade.getPorts();
		}


		public JAPCertificate getMixCertificate(String a_mixID)
		{
			MixInfo mixinfo = getMixInfo(a_mixID);
			JAPCertificate certificate = null;
			if (mixinfo != null)
			{
				certificate = mixinfo.getMixCertificate();
			}
			return certificate;
		}

		/**
		 * Get the operator name of a cascade.
		 * @param a_mixId String
		 * @return String
		 */
		public String getEMail(String a_mixId)
		{
			ServiceOperator operator;
			MixInfo info;
			String strEmail = null;

			info = getMixInfo(a_mixId);
			if (info != null)
			{
				operator = info.getServiceOperator();
				if (operator != null)
				{
					strEmail = operator.getEMail();
				}
			}
			if (strEmail == null || !X509SubjectAlternativeName.isValidEMail(strEmail))
			{
				return "N/A";
			}
			return strEmail;
		}


		/**
		 * Get the operator name of a cascade.
		 * @param a_mixId String
		 * @return String
		 */
		public String getOperator(String a_mixId)
		{
			ServiceOperator operator = getServiceOperator(a_mixId);
			String strOperator = null;
			if (operator != null)
			{
				strOperator = operator.getOrganisation();
			}
			if (strOperator == null)
			{
				return "N/A";
			}
			return strOperator;
		}

		/**
		 * Get the web URL of a cascade.
		 * @param a_mixId String
		 * @return String
		 */
		public String getUrl(String a_mixId)
		{
			ServiceOperator operator = getServiceOperator(a_mixId);
			String strUrl = null;
			if (operator != null)
			{
				strUrl = operator.getUrl();
			}
			try
			{
				if (strUrl != null && strUrl.toLowerCase().startsWith("https"))
				{
					// old java < 1.4 does not know https...
					new URL("http" + strUrl.substring(5, strUrl.length()));
				}
				else
				{
					new URL(strUrl);
				}
			}
			catch (MalformedURLException a_e)
			{
				strUrl = null;
			}
			if (strUrl == null)
			{
				return "N/A";
			}
			return URL_BEGIN + strUrl + URL_END;
		}

		/**
		 * Get the location of a cascade.
		 * @param a_mixId String
		 * @return String
		 */
		public String getLocation(String a_mixId)
		{
			ServiceLocation location = getServiceLocation(a_mixId);
			String strLocation = "";

			if (location != null)
			{
				if (location.getCity() != null && location.getCity().trim().length() > 0)
				{
					strLocation = location.getCity().trim();
				}

				if (location.getState() != null && location.getState().trim().length() > 0)
				{
					if (strLocation.length() > 0)
					{
						strLocation += ", ";
					}
					strLocation += location.getState().trim();
				}

				if (location.getCountry() != null && location.getCountry().trim().length() > 0)
				{
					if (strLocation.length() > 0)
					{
						strLocation += ", ";
					}

					try
					{
						strLocation += new CountryMapper(
							location.getCountry(), JAPController.getInstance().getLocale()).toString();
					}
					catch (IllegalArgumentException a_e)
					{
						strLocation += location.getCountry().trim();
					}
				}
			}

			if (strLocation.trim().length() == 0)
			{
				return "N/A";
			}
			return strLocation;
		}

		/**
		 * Get payment property of a cascade.
		 * @param a_cascadeId String
		 * @return boolean
		 */
		public boolean isPay(String a_cascadeId)
		{
			MixCascade cascade = getMixCascade(a_cascadeId);
			if (cascade != null)
			{
				return cascade.isPayment();
			}
			return false;
		}

		public Vector getCoordinates(String a_mixId)
		{
			ServiceLocation location = getServiceLocation(a_mixId);
			Vector coordinates;

			if (location == null || location.getLatitude() == null || location.getLongitude() == null)
			{
				return null;
			}
			try
			{
				Double.valueOf(location.getLatitude());
				Double.valueOf(location.getLongitude());
			}
			catch (NumberFormatException a_e)
			{
				return null;
			}

			coordinates = new Vector();
			coordinates.addElement(location.getLatitude());
			coordinates.addElement(location.getLongitude());
			return coordinates;
		}

		private StatusInfo getStatusInfo(String a_cascadeId)
		{
			return (StatusInfo) Database.getInstance(StatusInfo.class).getEntryById(a_cascadeId);
		}

		private MixCascade getMixCascade(String a_cascadeId)
		{
			return (MixCascade) Database.getInstance(MixCascade.class).getEntryById(a_cascadeId);
		}

		private ServiceLocation getServiceLocation(String a_mixId)
		{
			MixInfo info = getMixInfo(a_mixId);

			if (info != null)
			{
				return info.getServiceLocation();
			}
			return null;
		}

		private ServiceOperator getServiceOperator(String a_mixId)
		{
			MixInfo info = getMixInfo(a_mixId);

			if (info != null)
			{
				return info.getServiceOperator();
			}
			return null;
		}

		private MixInfo getMixInfo(String a_mixId)
		{
			return (MixInfo) Database.getInstance(MixInfo.class).getEntryById(a_mixId);
		}
	}

	/**
	 *
	 * Cascade database entry for the temporary infoservice.
	 */
	final class TempCascade
	{
		private String m_id;
		private String m_ports;
		private String m_hosts;

		public TempCascade(String a_id, String a_hosts, String a_ports)
		{
			m_id = a_id;
			m_hosts = a_hosts;
			m_ports = a_ports;
		}

		public String getId()
		{
			return m_id;
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
}
