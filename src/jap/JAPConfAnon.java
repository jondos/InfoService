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
import java.awt.event.MouseAdapter;
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
import javax.swing.border.TitledBorder;
import javax.swing.ListSelectionModel;
import javax.swing.ScrollPaneConstants;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import anon.crypto.JAPCertificate;
import anon.crypto.XMLSignature;
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
import anon.util.Util;
import gui.CertDetailsDialog;
import gui.CountryMapper;
import gui.GUIUtils;
import gui.JAPHelp;
import gui.JAPJIntField;
import gui.JAPMessages;
import gui.MapBox;

import gui.dialog.JAPDialog;
import jap.forward.JAPRoutingMessage;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import platform.AbstractOS;
import anon.crypto.SignatureVerifier;
import gui.JAPMultilineLabel;
import anon.infoservice.ServiceSoftware;
import anon.crypto.AbstractX509AlternativeName;
import javax.swing.ImageIcon;

class JAPConfAnon extends AbstractJAPConfModule implements MouseListener, ActionListener,
	ListSelectionListener, ItemListener, KeyListener, Observer
{
	private static final String MSG_LABEL_CERTIFICATE = JAPConfAnon.class.getName() + "_certificate";
	private static final String MSG_LABEL_EMAIL = JAPConfAnon.class.getName() + "_labelEMail";
	private static final String MSG_REALLY_DELETE = JAPConfAnon.class.getName() + "_reallyDelete";
	private static final String MSG_MIX_VERSION = JAPConfAnon.class.getName() + "_mixVersion";
	private static final String MSG_MIX_ID = JAPConfAnon.class.getName() + "_mixID";
	private static final String MSG_BUTTONEDITSHOW = JAPConfAnon.class.
		getName() + "_buttoneditshow";
	private static final String MSG_PAYCASCADE = JAPConfAnon.class.getName() + "_paycascade";
	private static final String MSG_MIX_X_OF_Y = JAPConfAnon.class.getName() + "_mixXOfY";
	private static final String MSG_MIX_POSITION = JAPConfAnon.class.getName() + "_mixPosition";
	private static final String MSG_MIX_FIRST = JAPConfAnon.class.getName() + "_mixFirst";
	private static final String MSG_MIX_MIDDLE = JAPConfAnon.class.getName() + "_mixMiddle";
	private static final String MSG_MIX_LAST = JAPConfAnon.class.getName() + "_mixLast";
	private static final String MSG_EXPLAIN_MIX_TT = JAPConfAnon.class.getName() + "_explainMixTT";
	private static final String MSG_FIRST_MIX_TEXT = JAPConfAnon.class.getName() + "_firstMixText";
	private static final String MSG_MIDDLE_MIX_TEXT = JAPConfAnon.class.getName() + "_middleMixText";
	private static final String MSG_LAST_MIX_TEXT = JAPConfAnon.class.getName() + "_lastMixText";


	private static final String DEFAULT_MIX_NAME = "AN.ON Mix";

	private static final int MAX_HOST_LENGTH = 30;

	private final Object MIX_COMBO_UPDATE_LOCK = new Object();
	private boolean m_bUpdateServerPanel = true;

	private InfoServiceTempLayer m_infoService;

	private JList m_listMixCascade;

	private JAPController m_Controller;

	private ServerListPanel m_serverList;
	private JPanel pRoot;

	private JPanel m_cascadesPanel;
	private ServerPanel m_serverPanel;
	private JPanel m_serverInfoPanel;
	private ManualPanel m_manualPanel;

	private JLabel m_numOfUsersLabel;
	private GridBagConstraints m_constrHosts, m_constrPorts;
	private JLabel m_lblHosts;
	private JLabel m_lblPorts;
	private JAPMultilineLabel m_reachableLabel;
	private JLabel m_portsLabel;


	private GridBagLayout m_rootPanelLayout;
	private GridBagConstraints m_rootPanelConstraints;

	private JLabel m_lblMix;

	private JPanel m_nrPanel;
	private JLabel m_nrLabel;
	private JLabel m_nrLblExplainBegin;
	private JLabel m_nrLblExplain;
	private JLabel m_nrLblExplainEnd;
	private JLabel m_operatorLabel;
	private JLabel m_emailLabel;
	private JLabel m_locationLabel;
	private JLabel m_payLabel;
	private JLabel m_viewCertLabel;
	private JLabel m_viewCertLabelValidity;

	private JButton m_manualCascadeButton;
	private JButton m_reloadCascadesButton;
	private JButton m_selectCascadeButton;
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

	private boolean m_bMixInfoShown = false;
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
		m_listMixCascade = new JList();
		m_listMixCascade.addListSelectionListener(this);
		m_listMixCascade.addMouseListener(this);
		//m_listMixCascade.setFont(font);

		m_listMixCascade.setEnabled(true);
		//m_lblCascadeInfo = new JLabel(JAPMessages.getString("infoAboutCascade"));

		m_lblMix = new JLabel();


		drawCompleteDialog();
	}

	private void drawServerPanel(int a_numberOfMixes, String a_strCascadeName, boolean a_enabled,
								 int a_selectedIndex)
	{
		if (m_manualPanel != null)
		{
			m_manualPanel.setVisible(false);
		}

		if (m_serverPanel == null)
		{
			m_serverPanel = new ServerPanel(this);
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
			m_serverPanel.setVisible(true);
		}
		m_serverPanel.setCascadeName(a_strCascadeName);
		m_serverPanel.updateServerList(a_numberOfMixes, a_enabled, a_selectedIndex);
		pRoot.validate();
	}

	private void drawServerInfoPanel()
	{
		if (m_serverInfoPanel == null)
		{
			m_serverInfoPanel = new ServerInfoPanel(this);
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
			//m_serverInfoPanel.removeAll();
			m_serverInfoPanel.setVisible(true);
		}
	}

	private void drawManualPanel(String a_hostName, String a_port)
	{
		if (m_serverPanel != null)
		{
			m_serverPanel.setVisible(false);
			m_serverInfoPanel.setVisible(false);
		}

		if (m_manualPanel == null)
		{
			m_manualPanel = new ManualPanel(this);
			m_rootPanelConstraints.gridx = 0;
			m_rootPanelConstraints.gridy = 2;
			m_rootPanelConstraints.weightx = 0;
			m_rootPanelConstraints.weighty = 1;
			m_rootPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
			m_rootPanelConstraints.fill = GridBagConstraints.BOTH;
			pRoot.add(m_manualPanel, m_rootPanelConstraints);
		}

		m_manualPanel.setHostName(a_hostName);
		m_manualPanel.setPort(a_port);
		m_manualPanel.setVisible(true);

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

		JLabel l;
		if (JAPModel.getDefaultView() != JAPConstants.VIEW_SIMPLIFIED)
		{
			l = new JLabel(JAPMessages.getString("availableCascades"));
			c.gridx = 0;
			c.gridy = 0;
			c.gridwidth = 2;
			c.anchor = GridBagConstraints.NORTHWEST;
			c.insets = new Insets(0, 5, 5, 5);
			m_cascadesPanel.add(l, c);
		}

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
		c.insets = new Insets(5, 5, 5, 5);
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
		m_reloadCascadesButton.setIcon(GUIUtils.loadImageIcon(JAPConstants.IMAGE_RELOAD, true, false));
		m_reloadCascadesButton.setDisabledIcon(GUIUtils.loadImageIcon(JAPConstants.IMAGE_RELOAD_DISABLED, true, false));
		m_reloadCascadesButton.setPressedIcon(GUIUtils.loadImageIcon(JAPConstants.IMAGE_RELOAD_ROLLOVER, true, false));

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

		l = new JLabel(JAPMessages.getString("numOfUsersOnCascade") + ":");
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
		m_lblHosts = new JLabel(JAPMessages.getString("cascadeReachableBy") + ":");
		c.gridx = 2;
		c.gridy = 2;
		c.weightx = 0;
		c.fill = GridBagConstraints.HORIZONTAL;
		m_constrHosts = (GridBagConstraints)c.clone();
		m_cascadesPanel.add(m_lblHosts, c);

		c.insets = new Insets(5, 5, 0, 5);
		m_reachableLabel = new JAPMultilineLabel("", null, null);
		c.gridx = 3;
		c.gridy = 2;
		c.weightx = 0;
		c.fill = GridBagConstraints.HORIZONTAL;
		m_cascadesPanel.add(m_reachableLabel, c);

		c.insets = new Insets(5, 20, 0, 5);
		m_lblPorts = new JLabel(JAPMessages.getString("cascadePorts") + ":");
		c.gridx = 2;
		c.gridy = 3;
		c.weightx = 0;
		c.fill = GridBagConstraints.HORIZONTAL;
		m_constrPorts = (GridBagConstraints)c.clone();
		m_cascadesPanel.add(m_lblPorts, c);

		c.insets = new Insets(5, 5, 0, 5);
		m_portsLabel = new JLabel("");
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
		if (JAPModel.getDefaultView() == JAPConstants.VIEW_SIMPLIFIED)
		{
			pRoot.setBorder(new TitledBorder(JAPMessages.getString("availableCascades")));
		}
		m_rootPanelConstraints.anchor = GridBagConstraints.NORTHWEST;

		drawManualPanel("","");
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
		if (selectedMixId != null)
		{
			String version = GUIUtils.trim(m_infoService.getMixVersion(selectedMixId));
			if (version != null)
			{
				version = ", " + JAPMessages.getString(MSG_MIX_VERSION) + "=" + version;
			}
			else
			{
				version = "";
			}

			m_lblMix.setToolTipText(JAPMessages.getString(MSG_MIX_ID) + "=" + selectedMixId + version);
			//m_lblMix.setText(JAPMessages.getString("infoAboutMix") +
			String name = GUIUtils.trim(m_infoService.getName(selectedMixId), 40);
			if (name == null)
			{
				m_lblMix.setText(DEFAULT_MIX_NAME);
				m_lblMix.setForeground(m_lblMix.getBackground());
			}
			else
			{
				m_lblMix.setText(name);
				m_lblMix.setForeground(m_nrLabel.getForeground());
			}

		}
		else
		{
			m_lblMix.setToolTipText("");
		}
		if (m_serverList.areMixButtonsEnabled())
		{
			String mixType;
			if (server == 0)
			{
				mixType = JAPMessages.getString(MSG_MIX_FIRST);
			}
			else if ((server + 1) == m_serverList.getNumberOfMixes())
			{
				mixType = JAPMessages.getString(MSG_MIX_LAST);
			}
			else
			{
				mixType = JAPMessages.getString(MSG_MIX_MIDDLE);
			}

			m_nrLabel.setText(JAPMessages.getString(MSG_MIX_X_OF_Y, new Object[]{new Integer(server + 1),
													new Integer(m_serverList.getNumberOfMixes())}));
			m_nrLblExplain.setText(mixType);
		}
		else
		{
			m_nrLabel.setText("N/A");
			m_nrLblExplain.setText("");
		}
		m_nrLabel.setToolTipText(m_nrLabel.getText());

		m_nrLblExplainBegin.setVisible(m_serverList.areMixButtonsEnabled());
		m_nrLblExplainEnd.setVisible(m_serverList.areMixButtonsEnabled());


		//m_nrLabel.setToolTipText(m_infoService.getOperator(selectedMixId));

		m_operatorLabel.setText(GUIUtils.trim(m_infoService.getOperator(selectedMixId)));
		//m_operatorLabel.setToolTipText(m_infoService.getOperator(selectedMixId));

		m_operatorLabel.setToolTipText(m_infoService.getUrl(selectedMixId));

		if (getUrlFromLabel(m_operatorLabel) != null)
		{
			m_operatorLabel.setForeground(Color.blue);
		}
		else
		{
			m_operatorLabel.setForeground(m_nrLabel.getForeground());
		}

		m_emailLabel.setText(GUIUtils.trim(m_infoService.getEMail(selectedMixId)));
		m_emailLabel.setToolTipText(m_infoService.getEMail(selectedMixId));
		if (getEMailFromLabel(m_emailLabel) != null)
		{
			m_emailLabel.setForeground(Color.blue);
		}
		else
		{
			m_emailLabel.setForeground(m_nrLabel.getForeground());
		}
		m_emailLabel.setToolTipText(m_infoService.getEMail(selectedMixId));



		m_locationCoordinates = m_infoService.getCoordinates(selectedMixId);
		m_locationLabel.setText(GUIUtils.trim(m_infoService.getLocation(selectedMixId)));
		if (m_locationCoordinates != null)
		{
			m_locationLabel.setForeground(Color.blue);
		}
		else
		{
			m_locationLabel.setForeground(m_nrLabel.getForeground());
		}
		m_locationLabel.setToolTipText(m_infoService.getLocation(selectedMixId));

		m_serverInfo = m_infoService.getMixInfo(selectedMixId);
		if(m_serverInfo != null)
		{
			m_serverCert = m_serverInfo.getMixCertificate();
		}
		else
		{
			m_serverCert = null;
		}
		/*if (m_serverCert == null && cascade != null && server == 0)
		{
			// get the certificate for the first mix
			m_serverCert = cascade.getMixCascadeCertificate();
		}*/
		if (m_serverCert != null && m_serverInfo != null)
		{
			m_viewCertLabel.setText((isServerCertVerified() ? JAPMessages.getString(CertDetailsDialog.MSG_CERT_VERIFIED) + "," :
				JAPMessages.getString(CertDetailsDialog.MSG_CERT_NOT_VERIFIED) + ","));
			m_viewCertLabel.setForeground(isServerCertVerified() ? Color.blue : Color.red);
			m_viewCertLabelValidity.setText((m_serverCert.getValidity().isValid(new Date()) ? " " +
				 JAPMessages.getString(CertDetailsDialog.MSG_CERTVALID) : " " +
				 JAPMessages.getString(JAPMessages.getString(CertDetailsDialog.MSG_CERTNOTVALID))));
			m_viewCertLabelValidity.setForeground(
						 m_serverCert.getValidity().isValid(new Date()) ? Color.blue : Color.red);
			m_viewCertLabel.setToolTipText(
						 m_viewCertLabel.getText() + m_viewCertLabelValidity.getText());
			m_viewCertLabelValidity.setToolTipText(
						 m_viewCertLabel.getText() + m_viewCertLabelValidity.getText());
		}
		else
		{
			m_viewCertLabelValidity.setText(" ");
			m_viewCertLabel.setText("N/A");
			m_viewCertLabel.setToolTipText("N/A");
			m_viewCertLabel.setForeground(m_nrLabel.getForeground());
		}

		pRoot.validate();
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

	protected void onUpdateValues()
	{
		Vector entries = Database.getInstance(MixCascade.class).getEntryList();
		Vector entriesUserDefined = new Vector();
		DefaultListModel listModel = new DefaultListModel();
		listModel.ensureCapacity(entries.size());

		CustomRenderer cr = new CustomRenderer();
		m_listMixCascade.setCellRenderer(cr);
		MixCascade currentCascade = JAPController.getInstance().getCurrentMixCascade();
		boolean bCurrentAlreadyAdded = false;
		MixCascade cascade;
		Enumeration it = entries.elements();
		while (it.hasMoreElements())
		{
			cascade = (MixCascade) it.nextElement();
			if (cascade.equals(currentCascade))
			{
				bCurrentAlreadyAdded = true;
			}

			if (cascade.isUserDefined())
			{
				entriesUserDefined.addElement(cascade);
				continue;
			}
			listModel.addElement(cascade);
		}

		if (!bCurrentAlreadyAdded)
		{
			listModel.addElement(currentCascade);
		}

		if (entriesUserDefined.size() > 0)
		{
			for (int i = 0; i < entriesUserDefined.size(); i++)
			{
				listModel.addElement(entriesUserDefined.elementAt(i));
			}
		}


		Object value = m_listMixCascade.getSelectedValue();

		try
		{
			synchronized (MIX_COMBO_UPDATE_LOCK)
			{
				m_bUpdateServerPanel = ((m_manualPanel == null) || (!m_manualPanel.isVisible()));
				m_listMixCascade.setModel(listModel);
				m_listMixCascade.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);

				if (value == null)
				{
					int index = m_listMixCascade.getFirstVisibleIndex();
					if (index >= 0)
					{
						m_listMixCascade.setSelectedIndex(index);
					}
				}
				else
				{
					m_listMixCascade.setSelectedValue(value, true);
				}
				m_bUpdateServerPanel = true;
			}
		}
		catch (Exception a_e)
		{
			LogHolder.log(LogLevel.EXCEPTION, LogType.GUI, a_e);
		}

		LogHolder.log(LogLevel.DEBUG, LogType.GUI, "- select First Item -- finished!");
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
				updateValues(false);

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
				m_manualPanel.setVisible(false);
				m_serverPanel.setVisible(true);
				m_serverInfoPanel.setVisible(true);
				updateValues(false);
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
			drawManualPanel(null, null);
			mb_manualCascadeNew = true;
			m_deleteCascadeButton.setEnabled(false);
			m_cancelCascadeButton.setEnabled(true);
		}

		else if (e.getSource() == m_editCascadeButton)
		{
			if (mb_manualCascadeNew)
			{
				enterManualCascade();
			}
			else
			{
				editManualCascade();
			}
		}
		else if (e.getSource() == m_deleteCascadeButton)
		{
			this.deleteManualCascade();
		}
		else if (e.getSource() == m_showEditPanelButton)
		{
			MixCascade cascade = (MixCascade) m_listMixCascade.getSelectedValue();
			drawManualPanel(cascade.getListenerInterface(0).getHost(),
							String.valueOf(cascade.getListenerInterface(0).getPort()));
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
			final MixCascade c = new MixCascade(m_manHostField.getText(),
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
				new Thread(new Runnable()
				{
					// get out of event thread
					public void run()
					{
						updateValues(true);
						m_listMixCascade.setSelectedValue(c, true);
					}
				}).start();

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
					int index = m_listMixCascade.getFirstVisibleIndex();
					if (index >= 0)
					{
						m_listMixCascade.setSelectedIndex(index);
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
			final MixCascade c = new MixCascade(m_manHostField.getText(),
										  Integer.parseInt(m_manPortField.getText()));

			Database.getInstance(MixCascade.class).update(c);
			((DefaultListModel)m_listMixCascade.getModel()).addElement(c);
			m_listMixCascade.setSelectedValue(c, true);
			updateValues(false);
		}
		catch (Exception a_e)
		{
			LogHolder.log(LogLevel.ERR, LogType.MISC, "Cannot create cascade");
		}
	}

	public void mouseClicked(MouseEvent e)
	{
		if (e.getSource() == m_operatorLabel)
		{
			String url = getUrlFromLabel(m_operatorLabel);
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
		else if (e.getSource() == m_emailLabel)
		{
			AbstractOS.getInstance().openEMail(getEMailFromLabel(m_emailLabel));
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
		else if (e.getSource() == m_viewCertLabel || e.getSource() == m_viewCertLabelValidity)
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
				new Thread(new Runnable()
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
				}).start();
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
		if ( (e.getSource() == m_operatorLabel && getUrlFromLabel(m_operatorLabel) != null) ||
			 (e.getSource() == m_emailLabel && getEMailFromLabel(m_emailLabel) != null) ||
			(e.getSource() == m_viewCertLabel && m_serverCert != null) ||
			(e.getSource() == m_viewCertLabelValidity && m_serverCert != null) ||
			(e.getSource() == m_locationLabel && m_locationCoordinates != null))
		{
			((JLabel)e.getSource()).setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		}
		else if (e.getSource() instanceof JLabel)
		{
			((JLabel)e.getSource()).setCursor(Cursor.getDefaultCursor());
		}
	}

	public void mouseExited(MouseEvent e)
	{
	}

	public String getHelpContext()
	{
		return "services_anon";
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

		if (!m_infoService.isFilled())
		{
			fetchCascades(false, false, true);
		}

		int index = m_listMixCascade.getFirstVisibleIndex();
		if (m_listMixCascade.getSelectedIndex() < 0 && index >= 0)
		{
			m_listMixCascade.setSelectedIndex(index);
		}
	}

	public void fontSizeChanged(final JAPModel.FontResize a_resize, final JLabel a_dummyLabel)
	{
		if (m_serverList != null)
		{
			m_serverList.fontSizeChanged(a_resize, a_dummyLabel);
		}

		/*
		m_lblCascadeInfo.setFont(new Font(a_dummyLabel.getFont().getName(), Font.BOLD,
										  (int) (a_dummyLabel.getFont().getSize() * 1.2)));
		m_lblMix.setFont(new Font(a_dummyLabel.getFont().getName(), Font.BOLD,
								  (int) (a_dummyLabel.getFont().getSize() * 1.2)));
						   */
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
			if (m_listMixCascade.getSelectedIndex() >= 0)
			{
				MixCascade cascade;
				String cascadeId;

				cascade = (MixCascade) m_listMixCascade.getSelectedValue();
				//System.out.println(cascade);
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
					//System.out.println(m_numOfUsersLabel.getText());
					//m_reachableLabel.setFont(m_numOfUsersLabel.getFont());
					//m_lblHosts.setFont(m_numOfUsersLabel.getFont());
					m_reachableLabel.setText(m_infoService.getHosts(cascadeId));
					m_cascadesPanel.remove(m_lblHosts);
					m_cascadesPanel.add(m_lblHosts, m_constrHosts);
					//m_portsLabel.setFont(m_numOfUsersLabel.getFont());
					//m_lblPorts.setFont(m_numOfUsersLabel.getFont());
					m_portsLabel.setText(m_infoService.getPorts(cascadeId));
					m_cascadesPanel.remove(m_lblPorts);
					m_cascadesPanel.add(m_lblPorts, m_constrPorts);
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
							// remove the corresponding mixes if the cascade is not the current cascade
							if (!JAPController.getInstance().getCurrentMixCascade().equals(cascade))
							{
								Vector mixIDs = cascade.getMixIds();
								for (int i = 0; i < mixIDs.size(); i++)
								{
									Database.getInstance(MixInfo.class).remove(
										(String) mixIDs.elementAt(i));
								}
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

			if (bDatabaseChanged)
			{
				updateValues(false);
			}
		}
		catch (Exception e)
		{
			/* should not happen, but better than throwing a runtime exception */
			LogHolder.log(LogLevel.EMERG, LogType.GUI, e);
		}
	}

	private static String getEMailFromLabel(JLabel a_emailLabel)
	{
		String email = a_emailLabel.getText();
		if (AbstractX509AlternativeName.isValidEMail(email))
		{
			return email;
		}
		else
		{
			return null;
		}
	}

	private static String getUrlFromLabel(JLabel a_urlLabel)
	{
		try
		{
			return new URL(a_urlLabel.getToolTipText()).toString();
		}
		catch (Exception a_e)
		{
			return null;
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
				MixCascade cascade = (MixCascade)value;
				ImageIcon icon;

				if (cascade.isUserDefined())
				{
					icon = GUIUtils.loadImageIcon(JAPConstants.IMAGE_CASCADE_MANUELL, true);
				}
				else if (cascade.isPayment())
				{
					icon = GUIUtils.loadImageIcon(JAPConstants.IMAGE_CASCADE_PAYMENT, true);
				}
				else
				{
					icon = GUIUtils.loadImageIcon(JAPConstants.IMAGE_CASCADE_INTERNET, true);
				}
				l = new JLabel(GUIUtils.trim(cascade.getName()), icon, LEFT);

				l.setToolTipText(cascade.getName());
				if (isSelected)
				{
					l.setOpaque(true);
					l.setBackground(Color.lightGray);
				}
				JAPController c = JAPController.getInstance();
				Font f = l.getFont();
				if (cascade.equals(c.getCurrentMixCascade()))
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
					l = new JLabel(GUIUtils.trim(value.toString()));

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
			Vector countPorts;
			Integer port;

			// Get hostnames and ports
			String interfaces = "";
			String ports = "";
			int[] portsArray = new int[a_cascade.getNumberOfListenerInterfaces()];

			for (int i = 0; i < a_cascade.getNumberOfListenerInterfaces(); i++)
			{
				if (interfaces.indexOf(a_cascade.getListenerInterface(i).getHost()) == -1)
				{
					if (interfaces.length() > 0)
					{
						interfaces += "\n";
					}
					interfaces += GUIUtils.trim(a_cascade.getListenerInterface(i).getHost(), MAX_HOST_LENGTH);
				}
				portsArray[i] = a_cascade.getListenerInterface(i).getPort();
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

			countPorts = new Vector(portsArray.length);
			for (int i = 0; i < portsArray.length; i++)
			{
				// do not double-count any ports
				port = new Integer(portsArray[i]);
				if (countPorts.contains(port))
				{
					continue;
				}
				countPorts.addElement(new Integer(portsArray[i]));
			}
			for (int i = 0; i < countPorts.size(); i++)
			{
				ports += countPorts.elementAt(i).toString();
				if (i != countPorts.size() - 1)
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

		public String getMixVersion(String a_mixID)
		{
			MixInfo mixinfo = getMixInfo(a_mixID);
			if (mixinfo != null)
			{
				ServiceSoftware software = mixinfo.getServiceSoftware();
				if (software != null)
				{
					return software.getVersion();
				}
			}
			return null;
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
			//return URL_BEGIN + strUrl + URL_END;
			return strUrl;
		}

		public String getName(String a_mixId)
		{
			String name;
			MixInfo info = getMixInfo(a_mixId);
			if (info == null)
			{
				return null;
			}
			name = info.getName();
			if (name == null || name.trim().length() == 0)
			{
				name = null;
			}
			return name;
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

				if (location.getState() != null && location.getState().trim().length() > 0 &&
					!strLocation.equals(location.getState().trim()))
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
			else
			{
				MixCascade cascade = (MixCascade)Database.getInstance(MixCascade.class).getEntryById(a_mixId);
				JAPCertificate mixCertificate;
				if (cascade != null)
				{
					// this is a first mix
					XMLSignature signature = cascade.getMixCascadeSignature();
					if (signature != null && signature.getCertPath() != null)
					{
						mixCertificate = signature.getCertPath().getSecondCertificate();
						if (mixCertificate != null)
						{
							return new ServiceLocation(null, mixCertificate);
						}
					}
				}
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
			else
			{
				MixCascade cascade = (MixCascade)Database.getInstance(MixCascade.class).getEntryById(a_mixId);
				JAPCertificate mixCertificate;
				if (cascade != null)
				{
					// this is a first mix
					XMLSignature signature = cascade.getMixCascadeSignature();
					if (signature != null && signature.getCertPath() != null)
					{
						mixCertificate = signature.getCertPath().getSecondCertificate();
						if (mixCertificate != null)
						{
							return new ServiceOperator(null, mixCertificate);
						}
					}
				}
			}
			return null;
		}

		private MixInfo getMixInfo(String a_mixId)
		{

			MixInfo info = (MixInfo) Database.getInstance(MixInfo.class).getEntryById(a_mixId);
			if (info == null)
			{
				MixCascade cascade = (MixCascade) Database.getInstance(MixCascade.class).getEntryById(a_mixId);
				if (cascade != null && cascade.getMixCascadeSignature() != null)
				{
					//System.out.println(cascade.getMixCascadeCertificate().getId());
					info = new MixInfo(cascade.getId(), cascade.getMixCascadeSignature().getCertPath());
				}

				//System.out.println(info + ":" + a_mixId);
			}
			return info;
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

	private class ManualPanel extends JPanel
	{
		public ManualPanel(JAPConfAnon a_listener)
		{
			GridBagLayout layout = new GridBagLayout();
			GridBagConstraints c = new GridBagConstraints();
			c.insets = new Insets(5, 5, 5, 5);
			c.anchor = GridBagConstraints.NORTHWEST;
			setLayout(layout);
			JLabel l = new JLabel(JAPMessages.getString("manualServiceAddHost"));
			c.gridx = 0;
			c.gridy = 0;
			add(l, c);
			l = new JLabel(JAPMessages.getString("manualServiceAddPort"));
			c.gridy = 1;
			add(l, c);
			m_manHostField = new JTextField();
			c.fill = GridBagConstraints.HORIZONTAL;
			c.weightx = 1;
			c.gridx = 1;
			c.gridy = 0;
			c.gridwidth = 3;
			add(m_manHostField, c);
			m_manPortField = new JAPJIntField(ListenerInterface.PORT_MAX_VALUE);
			c.gridy = 1;
			c.fill = c.NONE;
			add(m_manPortField, c);
			c.weightx = 0;
			c.gridy = 2;
			c.fill = c.HORIZONTAL;
			c.gridx = 2;
			c.gridwidth = 1;
			c.fill = GridBagConstraints.NONE;
			c.anchor = GridBagConstraints.NORTHEAST;

			m_editCascadeButton = new JButton(JAPMessages.getString("okButton"));
			m_editCascadeButton.addActionListener(a_listener);
			c.gridx = 1;
			//c.weightx = 1;
			add(m_editCascadeButton, c);
			m_cancelCascadeButton = new JButton(JAPMessages.getString("cancelButton"));
			m_cancelCascadeButton.addActionListener(a_listener);
			c.gridx = 2;
			add(m_cancelCascadeButton, c);
			m_manHostField.addKeyListener(a_listener);
			m_manPortField.addKeyListener(a_listener);
		}

		public void setHostName(String a_hostName)
		{
			m_manHostField.setText(a_hostName);
		}

		public void setPort(String a_port)
		{
			m_manPortField.setText(a_port);
		}
	}

	private class ServerPanel extends JPanel
	{
		private JLabel m_lblCascadeName;
		private JAPConfAnon m_listener;
		GridBagConstraints m_constraints;

		public ServerPanel(JAPConfAnon a_listener)
		{
			m_listener = a_listener;

			GridBagLayout layout = new GridBagLayout();
			m_constraints = new GridBagConstraints();
			setLayout(layout);
			m_constraints.gridx = 0;
			m_constraints.gridy = 0;
			m_constraints.anchor = GridBagConstraints.NORTHWEST;
			m_constraints.fill = GridBagConstraints.HORIZONTAL;
			m_constraints.weightx = 1;
			m_constraints.weighty = 0;
			m_constraints.insets = new Insets(5, 10, 5, 5);
			//add(m_lblCascadeInfo, m_constraints);

			m_constraints.gridy = 1;
			m_lblCascadeName = new JLabel();
			add(new JLabel(), m_constraints);

			// contraints for server list
			m_constraints.gridy = 2;
			m_constraints.insets = new Insets(2, 20, 2, 2);
		}

		public void setCascadeName(String a_strCascadeName)
		{
			GUIUtils.trim(a_strCascadeName);
			if (a_strCascadeName == null || a_strCascadeName.length() < 1)
			{
				a_strCascadeName = " ";
			}
			m_lblCascadeName.setText(a_strCascadeName);
		}

		public void updateServerList(int a_numberOfMixes, boolean a_bEnabled, int a_selectedIndex)
		{
			if (m_serverList != null && m_serverList.areMixButtonsEnabled() == a_bEnabled &&
				m_serverList.getNumberOfMixes() == a_numberOfMixes)
			{
				m_serverList.setSelectedIndex(a_selectedIndex);
			}
			else
			{
				if (m_serverList != null)
				{
					remove(m_serverList);
					m_serverList.removeItemListener(m_listener);
					m_serverList.setVisible(false);
				}
				m_serverList = new ServerListPanel(a_numberOfMixes, a_bEnabled, a_selectedIndex);
				m_serverList.addItemListener(m_listener);
			}

			add(m_serverList, m_constraints);
		}
	}

	private class ServerInfoPanel extends JPanel
	{
		public ServerInfoPanel(JAPConfAnon a_listener)
		{
			GridBagLayout layout = new GridBagLayout();
			GridBagConstraints c = new GridBagConstraints();
			JLabel l;
			setLayout(layout);

			c.insets = new Insets(5, 10, 5, 5);
			c.gridx = 0;
			c.gridy = 0;
			c.weightx = 0;
			c.gridwidth = 3;
			c.fill = GridBagConstraints.NONE;
			c.anchor = GridBagConstraints.NORTHWEST;
			c.insets = new Insets(5, 20, 5, 5);
			add(m_lblMix, c);


			l = new JLabel(JAPMessages.getString(MSG_MIX_POSITION) +":");
			c.gridy = 1;
			c.gridwidth = 1;
			c.insets = new Insets(5, 30, 5, 5);
			add(l, c);


			m_nrPanel = new JPanel(new GridBagLayout());
			c.gridx = 1;
			c.gridwidth = 3;
			c.insets = new Insets(5, 30, 5, 0);
			add(m_nrPanel, c);

			c.gridx = 3;
			c.weightx = 1;
			c.fill = GridBagConstraints.HORIZONTAL;
			c.anchor = GridBagConstraints.WEST;
			add(new JLabel(), c);


			GridBagConstraints nrPanelConstraints = new GridBagConstraints();

			m_nrLabel = new JLabel();
			nrPanelConstraints.gridx = 0;
			nrPanelConstraints.gridy = 0;
			nrPanelConstraints.weightx = 0;
			nrPanelConstraints.insets = new Insets(0, 0, 0, 5);
			m_nrPanel.add(m_nrLabel, nrPanelConstraints);

			m_nrLblExplainBegin = new JLabel("(");
			m_nrLblExplainBegin.setVisible(false);
			nrPanelConstraints.gridx++;
			nrPanelConstraints.insets = new Insets(0, 0, 0, 0);
			m_nrPanel.add(m_nrLblExplainBegin, nrPanelConstraints);

			m_nrLblExplain = new JLabel();

			m_nrLblExplain.addMouseListener(new MouseAdapter()
			{
				public void mouseClicked(MouseEvent a_event)
				{
					if (m_bMixInfoShown)
					{
						return;
					}
					m_bMixInfoShown = true;

					String mixType;

					if (m_nrLblExplain.getText().equals(mixType = JAPMessages.getString(MSG_MIX_FIRST)))
					{
						JAPDialog.showMessageDialog(
											  getRootPanel(), JAPMessages.getString(MSG_FIRST_MIX_TEXT),
											  mixType);
					}
					else if (m_nrLblExplain.getText().equals(mixType = JAPMessages.getString(MSG_MIX_MIDDLE)))
					{
						JAPDialog.showMessageDialog(
											  getRootPanel(), JAPMessages.getString(MSG_MIDDLE_MIX_TEXT),
											  mixType);
					}
					else if (m_nrLblExplain.getText().equals(mixType = JAPMessages.getString(MSG_MIX_LAST)))
					{
						JAPDialog.showMessageDialog(
											  getRootPanel(), JAPMessages.getString(MSG_LAST_MIX_TEXT),
											  mixType);
					}

					m_bMixInfoShown = false;
				}
			});
			m_nrLblExplain.setToolTipText(JAPMessages.getString(MSG_EXPLAIN_MIX_TT));
			m_nrLblExplain.setForeground(Color.blue);
			m_nrLblExplain.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			nrPanelConstraints.gridx++;
			m_nrPanel.add(m_nrLblExplain, nrPanelConstraints);

			m_nrLblExplainEnd = new JLabel(")");
			m_nrLblExplainEnd.setVisible(false);
			nrPanelConstraints.gridx++;
			m_nrPanel.add(m_nrLblExplainEnd, nrPanelConstraints);


			l = new JLabel(JAPMessages.getString("mixOperator"));
			c.gridy++;
			c.weightx = 0;
			c.gridx = 0;
			c.gridwidth = 1;
			c.insets = new Insets(5, 30, 5, 5);
			add(l, c);

			m_operatorLabel = new JLabel();
			m_operatorLabel.addMouseListener(a_listener);
			c.weightx = 1;
			c.gridx = 1;
			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridwidth = 2;
			add(m_operatorLabel, c);

			l = new JLabel(JAPMessages.getString(MSG_LABEL_EMAIL) + ":");
			c.gridx = 0;
			c.gridy++;
			c.weightx = 0;
			c.insets = new Insets(5, 30, 5, 5);
			c.gridwidth = 1;
			add(l, c);

			m_emailLabel = new JLabel();
			m_emailLabel.addMouseListener(a_listener);
			c.weightx = 1;
			c.gridx = 1;
			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridwidth = 2;
			add(m_emailLabel, c);

			l = new JLabel(JAPMessages.getString("mixLocation") + ":");
			c.weightx = 0;
			c.gridx = 0;
			c.gridy++;
			c.gridwidth = 1;
			add(l, c);

			m_locationLabel = new JLabel();
			m_locationLabel.addMouseListener(a_listener);
			c.gridx = 1;
			c.gridwidth = 2;
			add(m_locationLabel, c);

			l = new JLabel(JAPMessages.getString(MSG_LABEL_CERTIFICATE) + ":");
			c.gridx = 0;
			c.gridy++;
			c.gridwidth = 1;
			add(l, c);

			m_viewCertLabel = new JLabel();
			m_viewCertLabel.addMouseListener(a_listener);
			c.gridx = 1;
			c.gridwidth = 1;
			c.insets = new Insets(5, 30, 5, 0);
			add(m_viewCertLabel, c);
			m_viewCertLabelValidity = new JLabel();
			m_viewCertLabelValidity.addMouseListener(a_listener);
			c.gridx = 2;
			c.gridwidth = 1;
			c.insets = new Insets(5, 0, 5, 5);
			add(m_viewCertLabelValidity, c);
			/*
			c.weightx = 1.0;
			c.weighty = 1.0;
			c.gridy++;
			c.gridx = 0;
			c.gridwidth = 3;
			add(new JLabel(), c);
			*/
		}
	}
}