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

import java.text.NumberFormat;
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.border.*;

import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JTabbedPane;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import javax.swing.border.TitledBorder;
import javax.swing.plaf.basic.BasicProgressBarUI;
import anon.infoservice.MixCascade;
import anon.infoservice.StatusInfo;
import gui.JAPDll;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import pay.gui.PaymentMainPanel;
import javax.swing.*;
import gui.*;
import java.awt.event.*;
import java.util.*;
import java.awt.Component;

final public class JAPNewView extends AbstractJAPMainView implements IJAPMainView, ActionListener,
	JAPObserver
{

	//private JLabel meterLabel;
	private JLabel m_labelCascadeName;
	private JPanel m_panelMain;
	private JButton m_bttnInfo, m_bttnHelp, m_bttnQuit, m_bttnIconify, m_bttnConf;
	//private JButton m_bttnAnonConf;
	//private JCheckBox m_cbAnon;
	//private JProgressBar userProgressBar;
	//private JProgressBar trafficProgressBar;
	//private JProgressBar protectionProgressBar;
	//private JProgressBar ownTrafficChannelsProgressBar;

	private JLabel m_labelMeterDetailsRisk, m_labelOwnBytes, m_labelOwnChannels;
	//private TitledBorder m_borderOwnTraffic, m_borderAnonMeter, m_borderDetails;
	private ImageIcon[] meterIcons;
	private JAPHelp helpWindow;
	private JAPConf m_dlgConfig;
	private Window m_ViewIconified;
	private NumberFormat m_NumberFormat;
	private boolean m_bIsIconified;
	//private final static boolean PROGRESSBARBORDER = true;
	//private GuthabenAnzeige guthaben;
	private boolean loadPay = false;

	private JAPMixCascadeComboBox m_comboAnonServices;
	private JLabel m_labelAnonService, m_labelAnonymity, m_labelAnonymitySmall, m_labelAnonymityOnOff;
	private JLabel m_labelAnonMeter, m_labelAnonymityLow, m_labelAnonymityHigh;
	private JProgressBar m_progressAnonTraffic;
	private JLabel m_labelAnonymityUser, m_labelAnonymityUserLabel, m_labelAnonymityTrafficLabel;

	private JLabel m_labelOwnTraffic, m_labelOwnTrafficSmall;
	private JLabel m_labelOwnActivity, m_labelForwarderActivity;
	private JLabel m_labelOwnActivitySmall, m_labelForwarderActivitySmall;
	private JLabel m_labelOwnTrafficBytes, m_labelOwnTrafficUnit;
	private JLabel m_labelOwnTrafficBytesSmall, m_labelOwnTrafficUnitSmall;
	private JLabel m_labelOwnTrafficWWW, m_labelOwnTrafficOther;
	private JLabel m_labelOwnTrafficBytesWWW, m_labelOwnTrafficUnitWWW;
	private JLabel m_labelForwarding, m_labelForwardingSmall;
	private JLabel m_labelForwardedTrafficBytes, m_labelForwardedTrafficBytesUnit;
	private JLabel m_labelForwarderCurrentConnections, m_labelForwarderAcceptedConnections;
	private JLabel m_labelForwarderRejectedConnections;
	private JLabel m_labelForwardedTraffic, m_labelForwarderUsedBandwidth;
	private JLabel m_labelForwarderCurrentConnectionsLabel, m_labelForwarderAcceptedConnectionsLabel;
	private JLabel m_labelForwarderRejectedConnectionsLabel, m_labelForwarderUsedBandwidthLabel;
	private JLabel m_labelForwarderConnections;
	private JProgressBar m_progressOwnTrafficActivity, m_progressOwnTrafficActivitySmall, m_progressAnonLevel;
	private JButton m_bttnAnonDetails;
	private JCheckBox m_cbAnonymityOn;
	private JRadioButton m_rbAnonOff, m_rbAnonOn;
	private JCheckBox m_cbForwarding, m_cbForwardingSmall;
	private FlippingPanel m_flippingpanelAnon, m_flippingpanelOwnTraffic, m_flippingpanelForward;
	private StatusPanel m_StatusPanel;
	private JPanel m_panelAnonService;
	private int m_iPreferredWidth;
	private boolean m_bIgnoreAnonComboEvents = false;
	private FlippingPanel m_flippingPanelPayment;
	private JLabel m_labelPayment;

	public JAPNewView(String s, JAPController a_controller)
	{
		super(s, a_controller);
		m_NumberFormat = NumberFormat.getInstance();
		m_Controller = JAPController.getInstance();
		helpWindow = null; //new JAPHelp(this);
		m_dlgConfig = null; //new JAPConf(this);
		m_bIsIconified = false;
	}

	public void create(boolean loadPay)
	{
		this.loadPay = loadPay;
		LogHolder.log(LogLevel.INFO, LogType.GUI, "JAPView:initializing...");
		init();
//			LogHolder.log(LogLevel.DEBUG,LogType.GUI,"JAPView:initialization finished!");
	}

	private void init()
	{
		m_flippingpanelOwnTraffic = new FlippingPanel(this);
		m_flippingpanelForward = new FlippingPanel(this);

		// Load Icon in upper left corner of the frame window
		ImageIcon ii = JAPUtil.loadImageIcon(JAPConstants.IICON16FN, true);
		if (ii != null)
		{
			setIconImage(ii.getImage());

			// Load Images for "Anonymity Meter"
		}
		loadMeterIcons();
		// "NORTH": Image
		ImageIcon northImage = JAPUtil.loadImageIcon(JAPMessages.getString("northPath"), true);
		JLabel northLabel = new JLabel(northImage);
		JPanel northPanel = new JPanel();
		GridBagLayout gbl = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		northPanel.setLayout(gbl);
		c.anchor = GridBagConstraints.NORTHWEST;
		c.fill = GridBagConstraints.NONE;
		c.weighty = 1;
		northPanel.add(northLabel, c);
		c.gridx = 1;
		c.anchor = GridBagConstraints.SOUTHEAST;
		JLabel l = new JLabel("<HTML><BODY><A HREF=\"\">" + JAPConstants.aktVersion + "</A></BODY></HTML>");
		l.setFont(new Font(l.getFont().getName(),
						   l.getFont().getStyle(),
						   (int) (l.getFont().getSize() * 0.8)));
		l.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		l.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent e)
			{
				m_Controller.aboutJAP();
			}
		});
		c.insets = new Insets(0, 0, 0, 10);
		northPanel.add(l, c);
		c.gridwidth = 2;
		c.gridx = 0;
		c.gridy = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		c.insets = new Insets(5, 10, 5, 10);
		northPanel.add(new JSeparator(), c);

		GridBagLayout gbl1 = new GridBagLayout();
		GridBagConstraints c1 = new GridBagConstraints();

		m_panelAnonService = new JPanel(gbl1);
		m_labelAnonService = new JLabel(JAPMessages.getString("ngAnonymisierungsdienst"));
		c1.insets = new Insets(0, 0, 0, 0);
		c1.anchor = GridBagConstraints.WEST;
		c1.weightx = 0;
		c1.fill = GridBagConstraints.NONE;
		m_panelAnonService.add(m_labelAnonService, c1);
		m_comboAnonServices = new JAPMixCascadeComboBox();
		m_comboAnonServices.addItemListener(new ItemListener()
		{
			public void itemStateChanged(ItemEvent e)
			{
				if (m_bIgnoreAnonComboEvents)
				{
					return;
				}
				if (e.getStateChange() == ItemEvent.SELECTED)
				{
					MixCascade cascade = (MixCascade) m_comboAnonServices.getSelectedItem();
					m_Controller.setCurrentMixCascade(cascade);
				}
			}
		});

		c1.insets = new Insets(0, 5, 0, 0);
		c1.fill = GridBagConstraints.HORIZONTAL;
		c1.weightx = 1;
		m_panelAnonService.add(m_comboAnonServices, c1);
		JButton bttnReload = new JButton(JAPUtil.loadImageIcon(JAPConstants.IMAGE_RELOAD, true));
		bttnReload.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				m_Controller.fetchMixCascades(false);
			}
		});
		bttnReload.setRolloverEnabled(true);
		ImageIcon icon = JAPUtil.loadImageIcon(JAPConstants.IMAGE_RELOAD_ROLLOVER, true);
		bttnReload.setRolloverIcon(icon);
		bttnReload.setSelectedIcon(icon);
		bttnReload.setRolloverSelectedIcon(icon);
		bttnReload.setPressedIcon(icon);
		bttnReload.setOpaque(false);
		c1.gridx = 2;
		c1.weightx = 0;
		c1.fill = GridBagConstraints.NONE;
		bttnReload.setBorder(new EmptyBorder(0, 0, 0, 0));
		bttnReload.setFocusPainted(false);
		m_panelAnonService.add(bttnReload, c1);
		m_bttnAnonDetails = new JButton(JAPMessages.getString("ngBttnAnonDetails"));
		m_bttnAnonDetails.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				showConfigDialog(JAPConf.ANON_TAB);
			}
		});
		c1.gridx = 3;
		c1.weightx = 0;
		c1.fill = GridBagConstraints.NONE;
		m_panelAnonService.add(m_bttnAnonDetails, c1);

		c.weighty = 1;
		c.gridwidth = 2;
		c.gridy = 2;
		c.gridx = 0;
		c.anchor = GridBagConstraints.WEST;
		northPanel.add(m_panelAnonService, c);

//------------------------------------------------------
		c.gridwidth = 2;
		c.gridx = 0;
		c.gridy = 3;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		northPanel.add(new JSeparator(), c);

//------------------ Anon Panel
		m_flippingpanelAnon = new FlippingPanel(this);
		//the panel with all the deatils....
		JPanel p = new JPanel();
		gbl1 = new GridBagLayout();
		c1 = new GridBagConstraints();
		c1.anchor = GridBagConstraints.NORTHWEST;
		p.setLayout(gbl1);
		m_labelAnonymity = new JLabel(JAPMessages.getString("ngAnonymitaet"));
		c1.insets = new Insets(0, 5, 0, 0);
		p.add(m_labelAnonymity, c1);
		m_labelAnonymityUserLabel = new JLabel(JAPMessages.getString("ngNrOfUsers"));
		c1.gridy = 1;
		c1.anchor = GridBagConstraints.WEST;
		c1.insets = new Insets(10, 15, 0, 10);
		p.add(m_labelAnonymityUserLabel, c1);
		m_labelAnonymityTrafficLabel = new JLabel(JAPMessages.getString("ngAnonymityTraffic"));
		c1.gridy = 2;
		p.add(m_labelAnonymityTrafficLabel, c1);
		m_labelAnonymityUser = new JLabel("", JLabel.CENTER);
		c1.insets = new Insets(10, 0, 0, 0);
		c1.anchor = GridBagConstraints.CENTER;
		c1.weightx = 1;
		c1.fill = GridBagConstraints.HORIZONTAL;
		c1.gridy = 1;
		c1.gridx = 1;
		p.add(m_labelAnonymityUser, c1);
		m_progressAnonTraffic = new JProgressBar();
		MyProgressBarUI ui = new MyProgressBarUI(true);
		ui.setFilledBarColor(Color.blue.brighter());
		m_progressAnonTraffic.setUI(ui);
		m_progressAnonTraffic.setMinimum(0);
		m_progressAnonTraffic.setMaximum(5);
		m_progressAnonTraffic.setBorderPainted(false);
		c1.gridy = 2;
		p.add(m_progressAnonTraffic, c1);

		m_labelAnonMeter = new JLabel(getMeterImage(3));
		c1.gridx = 2;
		c1.gridy = 0;
		c1.gridheight = 3;
		c1.anchor = GridBagConstraints.WEST;
		c1.weightx = 0;
		c1.fill = GridBagConstraints.NONE;
		c1.insets = new Insets(0, 10, 0, 10);
		p.add(m_labelAnonMeter, c1);

		GridBagLayout gbl2 = new GridBagLayout();
		GridBagConstraints c2 = new GridBagConstraints();
		JPanel p2 = new JPanel(gbl2);
		p2.setBorder(LineBorder.createBlackLineBorder());
		//new BoxLayout(p2,BoxLayout.Y_AXIS);
		m_labelAnonymityOnOff = new JLabel(JAPMessages.getString("ngAnonymitaet"));
		c2.anchor = GridBagConstraints.NORTHWEST;
		c2.insets = new Insets(2, 2, 2, 2);
		p2.add(m_labelAnonymityOnOff, c2);
		m_rbAnonOn = new JRadioButton(JAPMessages.getString("ngAnonOn"));
		m_rbAnonOn.addActionListener(this);
		m_rbAnonOff = new JRadioButton(JAPMessages.getString("ngAnonOff"));
		m_rbAnonOff.addActionListener(this);
		ButtonGroup bg = new ButtonGroup();
		bg.add(m_rbAnonOn);
		bg.add(m_rbAnonOff);
		m_rbAnonOff.setSelected(true);
		c2.gridy = 1;
		c2.insets = new Insets(0, 7, 0, 0);
		p2.add(m_rbAnonOn, c2);
		c2.gridy = 2;
		p2.add(m_rbAnonOff, c2);

		c1.gridx = 3;
		c1.anchor = GridBagConstraints.WEST;
		p.add(p2, c1);
		m_flippingpanelAnon.setFullPanel(p);

		//the small panel
		gbl1 = new GridBagLayout();
		c1 = new GridBagConstraints();
		p = new JPanel(gbl1);
		m_labelAnonymitySmall = new JLabel(JAPMessages.getString("ngAnonymitaet") + ":");
		c1.gridx = 0;
		c1.anchor = GridBagConstraints.WEST;
		c1.weightx = 0;
		c1.insets = new Insets(0, 5, 0, 0);
		p.add(m_labelAnonymitySmall, c1);
		m_cbAnonymityOn = new JCheckBox(JAPMessages.getString("ngAnonOn"));
		m_cbAnonymityOn.addActionListener(this);
		c1.gridx = 1;
		p.add(m_cbAnonymityOn);
		m_labelAnonymityLow = new JLabel(JAPMessages.getString("ngAnonymityLow"), JLabel.RIGHT);
		c1.insets = new Insets(0, 20, 0, 5);
		c1.gridx = 2;
		c1.weightx = 0.5;
		c1.fill = GridBagConstraints.HORIZONTAL;
		c1.anchor = GridBagConstraints.EAST;
		p.add(m_labelAnonymityLow, c1);
		m_progressAnonLevel = new JProgressBar();
		m_progressAnonLevel.setMinimum(0);
		m_progressAnonLevel.setMaximum(5);
		m_progressAnonLevel.setBorderPainted(false);
		m_progressAnonLevel.setUI(new MyProgressBarUI(true));
		c1.weightx = 0.75;
		c1.fill = GridBagConstraints.HORIZONTAL;
		c1.anchor = GridBagConstraints.CENTER;
		c1.insets = new Insets(0, 0, 0, 0);
		c1.gridx = 3;
		p.add(m_progressAnonLevel, c1);
		m_labelAnonymityHigh = new JLabel(JAPMessages.getString("ngAnonymityHigh"));
		c1.gridx = 4;
		c1.weightx = 0.5;
		c1.insets = new Insets(0, 5, 0, 0);
		c1.fill = GridBagConstraints.HORIZONTAL;
		c1.anchor = GridBagConstraints.WEST;
		p.add(m_labelAnonymityHigh, c1);
		m_flippingpanelAnon.setSmallPanel(p);

		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.gridy = 4;
		m_flippingpanelAnon.setFlipped(true);
		northPanel.add(m_flippingpanelAnon, c);

//-----------------------------------------------------------
		c.gridwidth = 2;
		c.gridx = 0;
		c.gridy = 5;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		northPanel.add(new JSeparator(), c);

//------------------ Payment Panel
		if(loadPay)
		{
		m_flippingPanelPayment = new FlippingPanel(this);

		m_flippingPanelPayment.setFullPanel(new pay.gui.PaymentMainPanel());


		gbl1 = new GridBagLayout();
		c1 = new GridBagConstraints();
		p = new JPanel(gbl1);
		/** @todo internationalize */
		m_labelPayment = new JLabel("Payment");
		c1.insets = new Insets(0, 5, 0, 0);
		c1.weightx = 0;
		c1.anchor = GridBagConstraints.WEST;
		c1.fill = GridBagConstraints.HORIZONTAL;
		p.add(m_labelPayment, c1);
		m_flippingPanelPayment.setSmallPanel(p);


		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.gridy = 6;
		m_flippingPanelPayment.setFlipped(false);
		northPanel.add(m_flippingPanelPayment, c);
		}
//-----------------------------------------------------------
		c.gridwidth = 2;
		c.gridx = 0;
		c.gridy = 7;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		northPanel.add(new JSeparator(), c);

//------------------ Own Traffic Panel
		//m_flippingpanelOwnTraffic = new FlippingPanel(this);
		//full
		gbl1 = new GridBagLayout();
		c1 = new GridBagConstraints();
		p = new JPanel(gbl1);
		m_labelOwnTraffic = new JLabel(JAPMessages.getString("ngOwnTraffic"));
		c1.insets = new Insets(0, 5, 0, 0);
		c1.anchor = GridBagConstraints.WEST;
		c1.weightx = 0;
		c1.fill = GridBagConstraints.HORIZONTAL;
		p.add(m_labelOwnTraffic, c1);
		JComponent spacer = new JPanel();
		Dimension spacerDimension = new Dimension(l.getFontMetrics(l.getFont()).charWidth('9') * 6, 1);
		spacer.setPreferredSize(spacerDimension);
		c1.insets = new Insets(0, 0, 0, 0);
		c1.gridx = 1;
		c1.fill = GridBagConstraints.NONE;
		c1.weightx = 1;
		p.add(spacer, c1);
		m_labelOwnTrafficBytes = new JLabel("0");
		c1.insets = new Insets(0, 5, 0, 0);
		c1.weightx = 0;
		c1.fill = GridBagConstraints.HORIZONTAL;
		c1.gridx = 2;
		p.add(m_labelOwnTrafficBytes, c1);
		m_labelOwnTrafficUnit = new JLabel(JAPMessages.getString("Byte"));
		c1.gridx = 3;
		p.add(m_labelOwnTrafficUnit, c1);
		m_labelOwnActivity = new JLabel(JAPMessages.getString("ngActivity"), JLabel.RIGHT);
		c1.weightx = 0;
		c1.fill = GridBagConstraints.HORIZONTAL;
		c1.gridx = 4;
		p.add(m_labelOwnActivity, c1);
		m_progressOwnTrafficActivity = new JProgressBar();
		ui = new MyProgressBarUI(true);
		ui.setFilledBarColor(Color.blue);
		m_progressOwnTrafficActivity.setUI(ui);
		m_progressOwnTrafficActivity.setMinimum(0);
		m_progressOwnTrafficActivity.setMaximum(5);
		m_progressOwnTrafficActivity.setBorderPainted(false);
		c1.gridx = 5;
		c1.weightx = 0;
		c1.fill = GridBagConstraints.NONE;
		p.add(m_progressOwnTrafficActivity, c1);
		m_labelOwnTrafficWWW = new JLabel(JAPMessages.getString("ngOwnTrafficWWW"));
		c1.insets = new Insets(10, 20, 0, 0);
		c1.gridx = 0;
		c1.gridy = 1;
		c1.anchor = GridBagConstraints.WEST;
		c1.weightx = 0;
		p.add(m_labelOwnTrafficWWW, c1);
		spacer = new JPanel();
		spacer.setPreferredSize(spacerDimension);
		c1.gridx = 1;
		c1.insets = new Insets(0, 0, 0, 0);
		c1.weightx = 1;
		c1.fill = GridBagConstraints.NONE;
		p.add(spacer, c1);
		m_labelOwnTrafficBytesWWW = new JLabel("0");
		c1.insets = new Insets(10, 5, 0, 0);
		c1.gridx = 2;
		c1.fill = GridBagConstraints.HORIZONTAL;
		c1.weightx = 0;
		p.add(m_labelOwnTrafficBytesWWW, c1);
		m_labelOwnTrafficUnitWWW = new JLabel(JAPMessages.getString("Byte"));
		c1.gridx = 3;
		p.add(m_labelOwnTrafficUnitWWW, c1);
		m_labelOwnTrafficOther = new JLabel(JAPMessages.getString("ngOwnTrafficOther"));
		c1.insets = new Insets(7, 20, 0, 0);
		c1.gridx = 0;
		c1.gridy = 2;
		p.add(m_labelOwnTrafficOther, c1);
		spacer = new JPanel();
		spacer.setPreferredSize(spacerDimension);
		c1.insets = new Insets(0, 0, 0, 0);
		c1.weightx = 1;
		c1.gridx = 1;
		c1.fill = GridBagConstraints.NONE;
		p.add(spacer, c1);
		l = new JLabel("0");
		c1.fill = GridBagConstraints.HORIZONTAL;
		c1.weightx = 0;
		c1.insets = new Insets(7, 5, 0, 0);
		c1.gridx = 2;
		p.add(l, c1);
		l = new JLabel(JAPMessages.getString("Byte"));
		c1.gridx = 3;
		p.add(l, c1);
		m_flippingpanelOwnTraffic.setFullPanel(p);

		//small
		gbl1 = new GridBagLayout();
		c1 = new GridBagConstraints();
		p = new JPanel(gbl1);
		m_labelOwnTrafficSmall = new JLabel(JAPMessages.getString("ngOwnTraffic"));
		c1.insets = new Insets(0, 5, 0, 0);
		c1.weightx = 0;
		c1.fill = GridBagConstraints.NONE;
		c1.anchor = GridBagConstraints.WEST;
		p.add(m_labelOwnTrafficSmall, c1);
		m_labelOwnTrafficBytesSmall = new JLabel("0");
		c1.gridx = 1;
		p.add(m_labelOwnTrafficBytesSmall, c1);
		m_labelOwnTrafficUnitSmall = new JLabel(JAPMessages.getString("Byte"));
		c1.gridx = 2;
		p.add(m_labelOwnTrafficUnitSmall, c1);
		m_labelOwnActivitySmall = new JLabel(JAPMessages.getString("ngActivity"), JLabel.RIGHT);
		c1.weightx = 1;
		c1.fill = GridBagConstraints.HORIZONTAL;
		c1.gridx = 3;
		p.add(m_labelOwnActivitySmall, c1);
		m_progressOwnTrafficActivitySmall = new JProgressBar();
		ui = new MyProgressBarUI(true);
		ui.setFilledBarColor(Color.blue);
		m_progressOwnTrafficActivitySmall.setUI(ui);
		m_progressOwnTrafficActivitySmall.setMinimum(0);
		m_progressOwnTrafficActivitySmall.setMaximum(5);
		m_progressOwnTrafficActivitySmall.setBorderPainted(false);
		c1.weightx = 0;
		c1.fill = GridBagConstraints.NONE;
		c1.gridx = 4;
		p.add(m_progressOwnTrafficActivitySmall, c1);
		m_flippingpanelOwnTraffic.setSmallPanel(p);

		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.gridy = 8;
		northPanel.add(m_flippingpanelOwnTraffic, c);

//-----------------------------------------------------------
		c.gridwidth = 2;
		c.gridx = 0;
		c.gridy = 9;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		northPanel.add(new JSeparator(), c);

//------------------ Forwarder Panel
		m_flippingpanelForward = new FlippingPanel(this);
		//big view
		gbl1 = new GridBagLayout();
		c1 = new GridBagConstraints();
		p = new JPanel(gbl1);
		m_labelForwarding = new JLabel(JAPMessages.getString("ngForwarding"));
		c1.insets = new Insets(0, 5, 0, 0);
		c1.anchor = GridBagConstraints.WEST;
		p.add(m_labelForwarding, c1);
		gbl = new GridBagLayout();
		c2 = new GridBagConstraints();
		p2 = new JPanel(gbl2);
		m_cbForwarding = new JCheckBox(JAPMessages.getString("ngForwardingOn"));
		m_cbForwarding.setBorder(null);
		ActionListener actionListener = new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				/* start or shutdown the forwarding server */
				JCheckBox source = (JCheckBox) e.getSource();
				m_Controller.enableForwardingServer(source.isSelected());
				valuesChanged(false);
			}
		};
		m_cbForwarding.addActionListener(actionListener);
		c2.gridx = 0;
		c2.weightx = 1;
		c2.fill = GridBagConstraints.HORIZONTAL;
		p2.add(m_cbForwarding, c2);
		m_labelForwarderActivity = new JLabel(JAPMessages.getString("ngActivity"));
		c2.insets = new Insets(0, 5, 0, 0);
		c2.gridx = 1;
		c2.weightx = 0;
		c2.fill = GridBagConstraints.NONE;
		p2.add(m_labelForwarderActivity, c2);
		JProgressBar progress = new JProgressBar();
		progress.setUI(new MyProgressBarUI(true));
		progress.setMinimum(0);
		progress.setMaximum(5);
		progress.setBorderPainted(false);
		c2.gridx = 2;
		p2.add(progress, c2);
		c1.fill = GridBagConstraints.HORIZONTAL;
		c1.weightx = 1;
		c1.gridx = 1;
		c1.gridwidth = 2;
		p.add(p2, c1);

		m_labelForwarderConnections = new JLabel(JAPMessages.getString("ngForwardedConnections"));
		c1.gridx = 0;
		c1.gridy = 1;
		c1.fill = GridBagConstraints.NONE;
		c1.weightx = 0;
		c1.gridwidth = 1;
		c1.insets = new Insets(10, 5, 0, 0);
		p.add(m_labelForwarderConnections, c1);
		m_labelForwarderCurrentConnections = new JLabel("0");
		c1.gridx = 1;
		p.add(m_labelForwarderCurrentConnections, c1);
		m_labelForwarderCurrentConnectionsLabel = new JLabel(JAPMessages.getString(
			"ngForwardedCurrentConnections"));
		c1.gridx = 2;
		p.add(m_labelForwarderCurrentConnectionsLabel, c1);
		m_labelForwarderAcceptedConnections = new JLabel("0");
		c1.insets = new Insets(7, 5, 0, 0);
		c1.gridx = 1;
		c1.gridy = 2;
		p.add(m_labelForwarderAcceptedConnections, c1);
		m_labelForwarderAcceptedConnectionsLabel = new JLabel(JAPMessages.getString(
			"ngForwardedAcceptedConnections"));
		c1.gridx = 2;
		p.add(m_labelForwarderAcceptedConnectionsLabel, c1);
		m_labelForwarderRejectedConnections = new JLabel("0");
		c1.gridx = 1;
		c1.gridy = 3;
		p.add(m_labelForwarderRejectedConnections, c1);
		m_labelForwarderRejectedConnectionsLabel = new JLabel(JAPMessages.getString(
			"ngForwardedRejectedConnections"));
		c1.gridx = 2;
		p.add(m_labelForwarderRejectedConnectionsLabel, c1);
		m_labelForwardedTraffic = new JLabel(JAPMessages.getString("ngForwardedTraffic"));
		c1.gridx = 0;
		c1.gridy = 4;
		p.add(m_labelForwardedTraffic, c1);
		m_labelForwardedTrafficBytes = new JLabel("0");
		c1.gridx = 1;
		p.add(m_labelForwardedTrafficBytes, c1);
		m_labelForwardedTrafficBytesUnit = new JLabel(JAPMessages.getString("Byte"));
		c1.gridx = 2;
		p.add(m_labelForwardedTrafficBytesUnit, c1);
		m_labelForwarderUsedBandwidthLabel = new JLabel(JAPMessages.getString("ngForwardedUsedBandwidth"));
		c1.gridx = 0;
		c1.gridy = 5;
		p.add(m_labelForwarderUsedBandwidthLabel, c1);
		m_labelForwarderUsedBandwidth = new JLabel("0");
		c1.gridx = 1;
		p.add(m_labelForwarderUsedBandwidth, c1);
		l = new JLabel("Byte/s");
		c1.gridx = 2;
		p.add(l, c1);

		m_flippingpanelForward.setFullPanel(p);

		//smallview
		gbl1 = new GridBagLayout();
		c1 = new GridBagConstraints();
		p = new JPanel(gbl1);
		m_labelForwardingSmall = new JLabel(JAPMessages.getString("ngForwarding"));
		c1.insets = new Insets(0, 5, 0, 0);
		c1.anchor = GridBagConstraints.WEST;
		p.add(m_labelForwardingSmall, c1);
		c1.gridx = 1;
		c1.weightx = 1;
		c1.fill = GridBagConstraints.HORIZONTAL;
		m_cbForwardingSmall = new JCheckBox(JAPMessages.getString("ngForwardingOn"));
		m_cbForwardingSmall.setBorder(null);
		m_cbForwardingSmall.addActionListener(actionListener);
		p.add(m_cbForwardingSmall, c1);
		m_labelForwarderActivitySmall = new JLabel(JAPMessages.getString("ngActivity"));
		c1.gridx = 2;
		c1.weightx = 0;
		c1.fill = GridBagConstraints.NONE;
		p.add(m_labelForwarderActivitySmall, c1);
		progress = new JProgressBar();
		progress.setUI(new MyProgressBarUI(true));
		progress.setMinimum(0);
		progress.setMaximum(5);
		progress.setBorderPainted(false);
		c1.gridx = 3;
		p.add(progress, c1);
		m_flippingpanelForward.setSmallPanel(p);

		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.gridy = 10;
		Observer observer = new Observer()
		{
			public void update(Observable a_notifier, Object a_message)
			{
				try
				{
					if (a_notifier instanceof JAPRoutingServerStatisticsListener)
					{
						JAPRoutingServerStatisticsListener stats = (JAPRoutingServerStatisticsListener)
							a_notifier;
						long c = stats.getTransferedBytes();
						String strUnit = JAPMessages.getString("Byte");
						if (c > 9999)
						{
							strUnit = JAPMessages.getString("kByte");
							c /= 1000;
						}
						m_labelForwardedTrafficBytes.setText(m_NumberFormat.format(c));
						m_labelForwardedTrafficBytesUnit.setText(strUnit);
						m_labelForwarderAcceptedConnections.setText(Integer.toString(stats.
							getAcceptedConnections()));
						m_labelForwarderRejectedConnections.setText(Integer.toString(stats.
							getRejectedConnections()));
						m_labelForwarderCurrentConnections.setText(Integer.toString(stats.
							getCurrentlyForwardedConnections()));
						m_labelForwarderUsedBandwidth.setText(Integer.toString(stats.getCurrentBandwidthUsage()));
					}
				}
				catch (Throwable t)
				{
				}
			}
		};
		JAPModel.getInstance().getRoutingSettings().getServerStatisticsListener().addObserver(observer);
		northPanel.add(m_flippingpanelForward, c);

//-----------------------------------------------------------
		c.gridwidth = 2;
		c.gridx = 0;
		c.gridy = 11;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		northPanel.add(new JSeparator(), c);
//Status
		c.gridy = 12;
		m_StatusPanel = new StatusPanel();
		northPanel.add(m_StatusPanel, c);

//-----------------------------------------------------------
//		c.gridwidth = 2;
//		c.gridx = 0;
//		c.gridy = 11;
		//	c.fill = GridBagConstraints.HORIZONTAL;
		//	c.weightx = 1;
		//	northPanel.add(new JSeparator(), c);
//---------------------------------------------------------

//Buttons
		gbl1 = new GridBagLayout();
		c1 = new GridBagConstraints();
		JPanel buttonPanel = new JPanel(gbl1);
		m_bttnInfo = new JButton(JAPMessages.getString("infoButton"));
		m_bttnHelp = new JButton(JAPMessages.getString("helpButton"));
		m_bttnQuit = new JButton(JAPMessages.getString("quitButton"));
		m_bttnConf = new JButton(JAPMessages.getString("confButton"));
		m_bttnIconify = new JButton(JAPUtil.loadImageIcon(JAPConstants.ICONIFYICONFN, true));
		m_bttnIconify.setToolTipText(JAPMessages.getString("iconifyWindow"));

		// Add real buttons
		buttonPanel.add(m_bttnIconify, c1);
		//buttonPanel.add(m_bttnInfo);
		c1.gridx = 1;
		c1.insets = new Insets(0, 10, 0, 0);
		buttonPanel.add(m_bttnHelp, c1);
		c1.gridx = 2;
		buttonPanel.add(m_bttnConf, c1);
		c1.gridx = 3;
		c1.weightx = 1;
		c1.fill = GridBagConstraints.HORIZONTAL;
		buttonPanel.add(new JLabel(), c1);
		c1.gridx = 4;
		c1.weightx = 0;
		buttonPanel.add(m_bttnQuit, c1);
		m_bttnIconify.addActionListener(this);
		m_bttnConf.addActionListener(this);
		m_bttnInfo.addActionListener(this);
		m_bttnHelp.addActionListener(this);
		m_bttnQuit.addActionListener(this);
		JAPUtil.setMnemonic(m_bttnIconify, JAPMessages.getString("iconifyButtonMn"));
		JAPUtil.setMnemonic(m_bttnConf, JAPMessages.getString("confButtonMn"));
		JAPUtil.setMnemonic(m_bttnInfo, JAPMessages.getString("infoButtonMn"));
		JAPUtil.setMnemonic(m_bttnHelp, JAPMessages.getString("helpButtonMn"));
		JAPUtil.setMnemonic(m_bttnQuit, JAPMessages.getString("quitButtonMn"));

		c.gridy = 13;
		northPanel.add(buttonPanel, c);

		// "West": Image
		ImageIcon westImage = JAPUtil.loadImageIcon(JAPMessages.getString("westPath"), true); ;
		JLabel westLabel = new JLabel(westImage);

		// "Center:" tabs
		//JTabbedPane tabs = new JTabbedPane();
		//JPanel config = buildConfigPanel();
		JPanel level = buildLevelPanel();
		//tabs.addTab(JAPMessages.getString("mainMeterTab"),JAPUtil.loadImageIcon(JAPConstants.METERICONFN, true), level );
		// "South": Buttons


//---------------------------------------------- add Components to Frame
		// temporary testing of new GUI interface now disabled... (Bastian Voigt)
		/*		if (loadPay)
		{
			// add old GUI as a tab
			JPanel oldInterfacePanel = new JPanel(new BorderLayout());
			oldInterfacePanel.setBackground(buttonPanel.getBackground());
			oldInterfacePanel.add(level, BorderLayout.CENTER);
			m_panelMain = level;
			if (!JAPModel.isSmallDisplay())
			{
				oldInterfacePanel.add(northPanel, BorderLayout.NORTH);
				oldInterfacePanel.add(westLabel, BorderLayout.WEST);
				oldInterfacePanel.add(new JLabel("  "), BorderLayout.EAST); //Spacer
				oldInterfacePanel.add(buttonPanel, BorderLayout.SOUTH);
			}

			// initialize new Payment GUI
			JPanel drumherum = new JPanel(new BorderLayout());
			JPanel dummyPanel1 = new JPanel();
			dummyPanel1.setBorder(new TitledBorder("Dummy Panel 1"));
			JPanel dummyPanel2 = new JPanel();
			dummyPanel2.setBorder(new TitledBorder("Dummy Panel 2"));
			JPanel newInterfacePanel = new PaymentMainPanel();
			JTabbedPane tab = new JTabbedPane();
			drumherum.add(dummyPanel2, BorderLayout.NORTH);
			drumherum.add(dummyPanel1, BorderLayout.CENTER);
			drumherum.add(newInterfacePanel, BorderLayout.SOUTH);
			tab.addTab("Old interface", oldInterfacePanel);
			tab.addTab("New Payment Interface", drumherum);
			getContentPane().add(tab);
		   PayAccountsFile.getInstance().addPaymentListener(new MyPaymentListener());
		}
		else
		  { // if loadPay is off, all stays as usual*/

			getContentPane().setBackground(buttonPanel.getBackground());
			getContentPane().add(northPanel, BorderLayout.CENTER);
			m_panelMain = level;

			/*if (!JAPModel.isSmallDisplay())
			 {
			 getContentPane().add(northPanel, BorderLayout.NORTH);
			 getContentPane().add(westLabel, BorderLayout.WEST);
			 getContentPane().add(new JLabel("  "), BorderLayout.EAST); //Spacer
			 getContentPane().add(buttonPanel, BorderLayout.SOUTH);
		}
		   }*/
		//tabs.setSelectedComponent(level);

		addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent e)
			{
				exitProgram();
			}

			public void windowDeiconified(WindowEvent e)
			{
				synchronized (m_runnableValueUpdate) //updateValues may change the Titel of the Window!!
				{
					m_bIsIconified = false;
					setTitle(m_Title);
				}
			}

			public void windowIconified(WindowEvent e)
			{
				hideWindowInTaskbar();
				m_bIsIconified = true;
				valuesChanged(false);
			}
		});

		Dimension d = super.getPreferredSize();
		m_iPreferredWidth = Math.max(d.width, Math.max(m_flippingpanelOwnTraffic.getPreferredSize().width,
			Math.max(
			Math.max(m_panelAnonService.getPreferredSize().width,
					 m_flippingpanelForward.getPreferredSize().width),
			m_flippingpanelAnon.getPreferredSize().width)));

		m_Controller.fetchMixCascades(false);
		valuesChanged(true);
		setOptimalSize();
		JAPUtil.centerFrame(this);
		//Change size and location if the user requested to restore the old position/size
		if (JAPModel.getSaveMainWindowPosition())
		{
			JAPModel m = JAPModel.getInstance();
			Dimension ds = Toolkit.getDefaultToolkit().getScreenSize();
			if (m.m_OldMainWindowLocation != null && m.m_OldMainWindowLocation.x >= 0 &&
				m.m_OldMainWindowLocation.y > 0 /*&&m.m_OldMainWindowLocation.x<ds.width&&
						m.m_OldMainWindowLocation.y<ds.height*/
				)
			{
				setLocation(m.m_OldMainWindowLocation);
			}
			if (m.m_OldMainWindowSize != null && m.m_OldMainWindowSize.height > 0 &&
				m.m_OldMainWindowSize.width > 0)
			{
				setSize(m.m_OldMainWindowSize);
			}
		}
	}

	private JPanel buildLevelPanel()
	{
		JPanel levelPanel = new JPanel(new BorderLayout());
//		JPanel levelPanel = new JPanel();
//		levelPanel.setLayout(new BoxLayout(levelPanel, BoxLayout.Y_AXIS) );

		// Own traffic situation: current # of channels
		//ownTrafficChannelsProgressBar = new JProgressBar(JProgressBar.HORIZONTAL, 0, 1);
		Font fontControls = JAPController.getDialogFont();
		//wnTrafficChannelsProgressBar.setFont(fontControls);
		//ownTrafficChannelsProgressBar.setUI(new MyProgressBarUI(false));
		//ownTrafficChannelsProgressBar.setStringPainted(true);
		//ownTrafficChannelsProgressBar.setBorderPainted(false /*PROGRESSBARBORDER*/);
		//ownTrafficChannelsProgressBar.setString(" ");

		// Own traffic situation: # of bytes transmitted
		//m_labelOwnTrafficBytes = new JLabel("0 Bytes", SwingConstants.RIGHT);
		//m_labelOwnTrafficBytes.setFont(fontControls);
		//
		//userProgressBar = new
		//	JProgressBar(JProgressBar.HORIZONTAL, 0, 1);
		//userProgressBar.setStringPainted(true);
		//userProgressBar.setBorderPainted(PROGRESSBARBORDER);
		//userProgressBar.setFont(fontControls);
		//
		//trafficProgressBar = new
		//	JProgressBar(JProgressBar.HORIZONTAL);
		//trafficProgressBar.setStringPainted(true);
		//trafficProgressBar.setBorderPainted(PROGRESSBARBORDER);
		//	trafficProgressBar.setFont(fontControls);
		//
		//	protectionProgressBar = new
		//		JProgressBar(JProgressBar.HORIZONTAL);
		//	protectionProgressBar.setStringPainted(true);
		//	protectionProgressBar.setBorderPainted(PROGRESSBARBORDER);
		//	protectionProgressBar.setFont(fontControls);

		JPanel ownTrafficPanel = new JPanel();
		ownTrafficPanel.setLayout(new GridLayout(2, 2, 5, 5));
		//m_borderOwnTraffic = new TitledBorder(JAPMessages.getString("ownTrafficBorder"));
		//m_borderOwnTraffic.setTitleFont(fontControls);
		//ownTrafficPanel.setBorder(m_borderOwnTraffic);
		m_labelOwnChannels = new JLabel(JAPMessages.getString("ownTrafficChannels"));
		m_labelOwnChannels.setFont(fontControls);
		ownTrafficPanel.add(m_labelOwnChannels);
		//ownTrafficPanel.add(ownTrafficChannelsProgressBar);
		m_labelOwnBytes = new JLabel(JAPMessages.getString("ownTrafficBytes"));
		m_labelOwnBytes.setFont(fontControls);
		ownTrafficPanel.add(m_labelOwnBytes);
		//ownTrafficPanel.add(m_labelOwnTrafficBytes);

// "Guthaben"
		//m_cbAnon = new JCheckBox(JAPMessages.getString("confActivateCheckBox"));
		//JAPUtil.setMnemonic(m_cbAnon, JAPMessages.getString("confActivateCheckBoxMn"));
		//m_cbAnon.setFont(fontControls);
		//m_cbAnon.addActionListener(this);

		// Line 1
		JPanel p41 = new JPanel();
		p41.setLayout(new BoxLayout(p41, BoxLayout.X_AXIS));
		//p41.add(Box.createRigidArea(new Dimension(10,0)) );
		//p41.add(m_cbAnon);
		if (!JAPModel.isSmallDisplay())
		{
			p41.add(Box.createRigidArea(new Dimension(5, 0)));
		}
		p41.add(Box.createHorizontalGlue());
		//m_bttnAnonConf = new JButton(JAPMessages.getString("confActivateButton"));
		//m_bttnAnonConf.setFont(fontControls);
		/*if (JAPModel.isSmallDisplay())
		   {
		 m_bttnAnonConf.setMargin(JAPConstants.SMALL_BUTTON_MARGIN);
		   }
		   m_bttnAnonConf.addActionListener(this);
		   p41.add(m_bttnAnonConf);
		 */
		// "anonym-o-meter"
		//JPanel meterPanel = new JPanel();
		//meterPanel.setLayout(new BorderLayout());
		//m_borderAnonMeter = new TitledBorder(JAPMessages.getString("meterBorder"));
		//m_borderAnonMeter.setTitleFont(fontControls);
		//meterPanel.setBorder(m_borderAnonMeter);
		//meterLabel = new JLabel(getMeterImage( -1));
		//meterPanel.add(p41 /*ano1CheckBox*/, BorderLayout.NORTH);
		//meterPanel.add(meterLabel, BorderLayout.CENTER);

		// details panel
		JPanel detailsPanel = new JPanel();
		m_labelCascadeName = new JLabel();
		m_labelCascadeName.setFont(fontControls);
		//m_labelMeterDetailsName = new JLabel(JAPMessages.getString("meterDetailsName") + " ");
		//m_labelMeterDetailsName.setFont(fontControls);
		//m_labelAnonymityUser = new JLabel(JAPMessages.getString("meterDetailsUsers") + " ");
		//m_labelAnonymityUser.setFont(fontControls);
		//m_labelMeterDetailsTraffic = new JLabel(JAPMessages.getString("meterDetailsTraffic") + " ");
		//m_labelMeterDetailsTraffic.setFont(fontControls);
		m_labelMeterDetailsRisk = new JLabel(JAPMessages.getString("meterDetailsRisk") + " ");
		m_labelMeterDetailsRisk.setFont(fontControls);
		GridBagLayout g = new GridBagLayout();
		detailsPanel.setLayout(g);
		//m_borderDetails = new TitledBorder(JAPMessages.getString("meterDetailsBorder"));
		//m_borderDetails.setTitleFont(fontControls);
		//detailsPanel.setBorder(m_borderDetails);
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = c.WEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		Insets normInsets = new Insets(0, 0, 8, 0);
		c.insets = normInsets;
		c.gridwidth = 1;
		c.weightx = 0;
		c.weighty = 1;
		c.gridx = 0;
		c.gridy = 0;
		//g.setConstraints(m_labelMeterDetailsName, c);
		//detailsPanel.add(m_labelMeterDetailsName);
		c.gridx = 1;
		c.weightx = 1;
		g.setConstraints(m_labelCascadeName, c);
		detailsPanel.add(m_labelCascadeName);
		c.weightx = 0;
		c.gridx = 0;
		c.gridy = 1;
		//g.setConstraints(m_labelAnonymityUser, c);
		//detailsPanel.add(m_labelAnonymityUser);
		c.gridx = 1;
		c.weightx = 1;
		//g.setConstraints(userProgressBar, c);
		//detailsPanel.add(userProgressBar);
		c.gridx = 0;
		c.gridy = 2;
		c.weightx = 0;
		//g.setConstraints(m_labelMeterDetailsTraffic, c);
		//detailsPanel.add(m_labelMeterDetailsTraffic);
		c.gridx = 1;
		c.weightx = 1;
		//g.setConstraints(trafficProgressBar, c);
		//detailsPanel.add(trafficProgressBar);
		normInsets = new Insets(0, 0, 0, 0);
		c.insets = normInsets;
		c.gridx = 0;
		c.gridy = 3;
		g.setConstraints(m_labelMeterDetailsRisk, c);
//		detailsPanel.add(labelMeterDetailsRisk);
		c.gridx = 1;
		//g.setConstraints(protectionProgressBar, c);
//		detailsPanel.add(protectionProgressBar);

		// Add all panels to level panel
		levelPanel.add(ownTrafficPanel, BorderLayout.NORTH);
		//levelPanel.add(meterPanel, BorderLayout.CENTER);
		levelPanel.add(detailsPanel, BorderLayout.SOUTH);

		return levelPanel;
	}

	/*private JPanel buildConfigPanel() {
	 // "Center" Panel
	 JPanel mainPanel = new JPanel();
	 mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS) );
	 //mainPanel.setBackground(Color.white);

	 // Listen on Port
	 JPanel portPanel = new JPanel();
	 portPanel.setLayout(new GridLayout(2,1) );
	 portPanel.setBorder( new TitledBorder(JAPMessages.getString("confListenerBorder")) );
	 // Line 1
	 JPanel p11 = new JPanel();
	 p11.setLayout(new BoxLayout(p11, BoxLayout.X_AXIS) );
	 p11.add(Box.createRigidArea(new Dimension(10,0)) );
	 p11.add(new JLabel(JAPMessages.getString("confPort")) );
	 p11.add(Box.createRigidArea(new Dimension(5,0)) );
	 m_labelProxyPort = new JLabel(String.valueOf(JAPModel.getHttpListenerPortNumber()));
//		m_labelProxyPort.setForeground(Color.black);
	 p11.add(m_labelProxyPort );
	 p11.add(Box.createRigidArea(new Dimension(5,0)) );
	 p11.add(Box.createHorizontalGlue() );
	 portB = new JButton(JAPMessages.getString("confPortButton"));
	 portB.addActionListener(this);
	 p11.add(portB);
	 // Line 2
	 JPanel p12 = new JPanel();
	 p12.setLayout(new BoxLayout(p12, BoxLayout.X_AXIS) );
	 p12.add(Box.createRigidArea(new Dimension(10,0)) );
	 p12.add(new JLabel(JAPMessages.getString("confStatus1")) );
	 p12.add(Box.createRigidArea(new Dimension(5,0)) );
	 // add to portPanel
	 portPanel.add(p11);
	 portPanel.add(p12);
	 // add to mainPanel
	 mainPanel.add(portPanel);

	 // HTTP Proxy
	 JPanel proxyPanel = new JPanel();
	 proxyPanel.setLayout(new GridLayout(2,1) );
	 proxyPanel.setBorder( new TitledBorder(JAPMessages.getString("confProxyBorder")) );
	 // Line 1
	 JPanel p21 = new JPanel();
	 p21.setLayout(new BoxLayout(p21, BoxLayout.X_AXIS) );
	 p21.add(Box.createRigidArea(new Dimension(10,0)) );
	 proxyMustUseLabel = new JLabel();
	 p21.add(proxyMustUseLabel);
	 p21.add(Box.createRigidArea(new Dimension(5,0)) );
	 p21.add(Box.createHorizontalGlue() );
	 httpB = new JButton(JAPMessages.getString("confProxyButton"));
	 httpB.addActionListener(this);
	 p21.add(httpB);
	 // Line 2
	 JPanel p22 = new JPanel();
	 p22.setLayout(new BoxLayout(p22, BoxLayout.X_AXIS) );
	 p22.add(Box.createRigidArea(new Dimension(10,0)) );
//		p22.add(new JLabel(JAPMessages.getString("confProxyHost")) );
//		p22.add(Box.createRigidArea(new Dimension(5,0)) );
	 m_labelProxyHost = new JLabel();
	 p22.add(m_labelProxyHost);
	 // add to proxypanel
	 proxyPanel.add(p21);
	 proxyPanel.add(p22);
	 // add to mainPanel
	 mainPanel.add(proxyPanel);

	 // Information Service
	 JPanel infoServicePanel = new JPanel();
	 infoServicePanel.setLayout(new GridLayout(1,1) );
	 infoServicePanel.setBorder( new TitledBorder(JAPMessages.getString("confInfoServiceBorder")) );
	 // Line 1
	 JPanel p31 = new JPanel();
	 p31.setLayout(new BoxLayout(p31, BoxLayout.X_AXIS) );
	 p31.add(Box.createRigidArea(new Dimension(10,0)) );
	 p31.add(new JLabel(JAPMessages.getString("confInfoServiceHost")) );
	 p31.add(Box.createRigidArea(new Dimension(5,0)) );
	 infoServiceTextField = new JLabel();
	 p31.add(infoServiceTextField);
	 p31.add(Box.createRigidArea(new Dimension(5,0)) );
	 p31.add(Box.createHorizontalGlue() );
	 isB = new JButton(JAPMessages.getString("confInfoServiceButton"));
	 isB.addActionListener(this);
	 p31.add(isB);
	 // add to infoServicePanel
	 infoServicePanel.add(p31);
	 // add to mainPanel
	 mainPanel.add(infoServicePanel);

	 // Activate Anonymity
	 JPanel activatePanel = new JPanel();
	 activatePanel.setLayout(new GridLayout(4,1) );
	 activatePanel.setBorder( new TitledBorder(JAPMessages.getString("confActivateBorder")) );
	 // Line 1
	 JPanel p41 = new JPanel();
	 p41.setLayout(new BoxLayout(p41, BoxLayout.X_AXIS) );
	 //p41.add(Box.createRigidArea(new Dimension(10,0)) );
	 anonCheckBox = new JCheckBox(JAPMessages.getString("confActivateCheckBox"));
//		anonCheckBox.setForeground(Color.red);
	 JAPUtil.setMnemonic(anonCheckBox,JAPMessages.getString("confActivateCheckBoxMn"));
	 anonCheckBox.addActionListener(this);
	 p41.add(anonCheckBox );
	 p41.add(Box.createRigidArea(new Dimension(5,0)) );
	 p41.add(Box.createHorizontalGlue() );
	 anonB = new JButton(JAPMessages.getString("confActivateButton"));
	 anonB.addActionListener(this);
	 p41.add(anonB);
	 // Line 2
	 JPanel p42 = new JPanel();
	 p42.setLayout(new BoxLayout(p42, BoxLayout.X_AXIS) );
	 p42.add(Box.createRigidArea(new Dimension(10,0)) );
	 p42.add(new JLabel(JAPMessages.getString("confAnonHost")) );
	 p42.add(Box.createRigidArea(new Dimension(5,0)) );
	 anonTextField = new JLabel();
	 p42.add(anonTextField);
	 // Line 3
	 JPanel p43 = new JPanel();
	 p43.setLayout(new BoxLayout(p43, BoxLayout.X_AXIS) );
	 p43.add(Box.createRigidArea(new Dimension(10,0)) );
	 p43.add(new JLabel(JAPMessages.getString("confStatus2")) );
	 p43.add(Box.createRigidArea(new Dimension(5,0)) );
	 // Line 4
	 JPanel p44 = new JPanel();
	 p44.setLayout(new BoxLayout(p44, BoxLayout.X_AXIS) );
	 p44.add(Box.createRigidArea(new Dimension(10,0)) );
	 p44.add(new JLabel(JAPMessages.getString("confAnonName")) );
	 p44.add(Box.createRigidArea(new Dimension(5,0)) );
	 anonNameTextField = new JLabel();
	 p44.add(anonNameTextField);
	 // add to activatePanel
	 activatePanel.add(p41);
	 activatePanel.add(p44);
	 activatePanel.add(p42);
	 activatePanel.add(p43);
	 // add to mainPanel
	 mainPanel.add(activatePanel);

	 return mainPanel;
	  }
	 */
	/**
	 * Used to disable activation on JAP
	 * Example: Activation of listener failed
	 *          --> disable activation checkboxes
	 */
	public void disableSetAnonMode()
	{
		//anonCheckBox.setEnabled(false);
		m_rbAnonOn.setEnabled(false);
		m_rbAnonOff.setEnabled(false);
	}

	/** Used to notice the View, that the locale has Changed.
	 *
	 */
	public void localeChanged()
	{
		m_bttnInfo.setText(JAPMessages.getString("infoButton"));
		m_bttnHelp.setText(JAPMessages.getString("helpButton"));
		m_bttnQuit.setText(JAPMessages.getString("quitButton"));
		m_bttnConf.setText(JAPMessages.getString("confButton"));
		JAPUtil.setMnemonic(m_bttnConf, JAPMessages.getString("confButtonMn"));
		JAPUtil.setMnemonic(m_bttnInfo, JAPMessages.getString("infoButtonMn"));
		JAPUtil.setMnemonic(m_bttnHelp, JAPMessages.getString("helpButtonMn"));
		JAPUtil.setMnemonic(m_bttnQuit, JAPMessages.getString("quitButtonMn"));

		m_labelAnonService.setText(JAPMessages.getString("ngAnonymisierungsdienst"));
		m_bttnAnonDetails.setText(JAPMessages.getString("ngBttnAnonDetails"));
		m_rbAnonOn.setText(JAPMessages.getString("ngAnonOn"));
		m_rbAnonOff.setText(JAPMessages.getString("ngAnonOff"));
		m_labelAnonymityOnOff.setText(JAPMessages.getString("ngAnonymitaet"));
		m_labelAnonymity.setText(JAPMessages.getString("ngAnonymitaet"));
		m_labelAnonymitySmall.setText(JAPMessages.getString("ngAnonymitaet"));
		m_labelAnonymityUserLabel.setText(JAPMessages.getString("ngNrOfUsers"));
		m_labelAnonymityTrafficLabel.setText(JAPMessages.getString("ngAnonymityTraffic"));
		m_labelAnonymityLow.setText(JAPMessages.getString("ngAnonymityLow"));
		m_labelAnonymityHigh.setText(JAPMessages.getString("ngAnonymityHigh"));
		m_cbAnonymityOn.setText(JAPMessages.getString("ngAnonOn"));
		m_labelOwnActivity.setText(JAPMessages.getString("ngActivity"));
		m_labelOwnActivitySmall.setText(JAPMessages.getString("ngActivity"));
		m_labelForwarderActivity.setText(JAPMessages.getString("ngActivity"));
		m_labelForwarderActivitySmall.setText(JAPMessages.getString("ngActivity"));
		m_labelForwarderCurrentConnectionsLabel.setText(JAPMessages.getString("ngForwardedCurrentConnections"));
		m_labelForwarderAcceptedConnectionsLabel.setText(JAPMessages.getString(
			"ngForwardedAcceptedConnections"));
		m_labelForwarderRejectedConnectionsLabel.setText(JAPMessages.getString(
			"ngForwardedRejectedConnections"));
		m_labelForwardingSmall.setText(JAPMessages.getString("ngForwarding"));
		m_labelForwarding.setText(JAPMessages.getString("ngForwarding"));
		m_labelForwardedTraffic.setText(JAPMessages.getString("ngForwardedTraffic"));
		m_labelForwarderConnections.setText(JAPMessages.getString("ngForwardedConnections"));
		m_labelForwarderUsedBandwidthLabel.setText(JAPMessages.getString("ngForwardedUsedBandwidth"));
		m_cbForwarding.setText(JAPMessages.getString("ngForwardingOn"));
		m_cbForwardingSmall.setText(JAPMessages.getString("ngForwardingOn"));

		m_labelOwnTraffic.setText(JAPMessages.getString("ngOwnTraffic"));
		m_labelOwnTrafficSmall.setText(JAPMessages.getString("ngOwnTraffic"));
		m_labelOwnTrafficWWW.setText(JAPMessages.getString("ngOwnTrafficWWW"));
		m_labelOwnTrafficOther.setText(JAPMessages.getString("ngOwnTrafficOther"));

		//m_labelMeterDetailsName.setText(JAPMessages.getString("meterDetailsName") + " ");
		//m_labelAnonymityUser.setText(JAPMessages.getString("meterDetailsUsers") + " ");
		//m_labelMeterDetailsTraffic.setText(JAPMessages.getString("meterDetailsTraffic") + " ");
		m_labelMeterDetailsRisk.setText(JAPMessages.getString("meterDetailsRisk") + " ");
		//m_borderOwnTraffic.setTitle(JAPMessages.getString("ownTrafficBorder"));
		m_labelOwnChannels.setText(JAPMessages.getString("ownTrafficChannels"));
		m_labelOwnBytes.setText(JAPMessages.getString("ownTrafficBytes"));
		//m_cbAnon.setText(JAPMessages.getString("confActivateCheckBox"));
		//JAPUtil.setMnemonic(m_cbAnon, JAPMessages.getString("confActivateCheckBoxMn"));
		//m_borderAnonMeter.setTitle(JAPMessages.getString("meterBorder"));
		//m_bttnAnonConf.setText(JAPMessages.getString("confActivateButton"));
		//m_borderDetails.setTitle(JAPMessages.getString("meterDetailsBorder"));
		if (m_dlgConfig != null)
		{
			m_dlgConfig.localeChanged();
		}
		m_NumberFormat = NumberFormat.getInstance();
		Dimension d = super.getPreferredSize();
		m_iPreferredWidth = Math.max(d.width, Math.max(m_flippingpanelOwnTraffic.getPreferredSize().width,
			Math.max(
			Math.max(m_panelAnonService.getPreferredSize().width,
					 m_flippingpanelForward.getPreferredSize().width),
			m_flippingpanelAnon.getPreferredSize().width)));

		valuesChanged(true);
		setOptimalSize();
	}

	protected void loadMeterIcons()
	{
		// Load Images for "Anonymity Meter"
		meterIcons = new ImageIcon[JAPConstants.METERFNARRAY.length];
//		LogHolder.log(LogLevel.DEBUG,LogType.MISC,"JAPView:METERFNARRAY.length="+JAPConstants.METERFNARRAY.length);
		if (!JAPModel.isSmallDisplay())
		{
			for (int i = 0; i < JAPConstants.METERFNARRAY.length; i++)
			{
				meterIcons[i] = JAPUtil.loadImageIcon(JAPConstants.METERFNARRAY[i], false);
			}
		}
		else
		{
			MediaTracker m = new MediaTracker(this);
			for (int i = 0; i < JAPConstants.METERFNARRAY.length; i++)
			{
				Image tmp = JAPUtil.loadImageIcon(JAPConstants.METERFNARRAY[i], true).getImage();
				int w = tmp.getWidth(null);
				tmp = tmp.getScaledInstance( (int) (w * 0.75), -1, Image.SCALE_SMOOTH);
				m.addImage(tmp, i);
				meterIcons[i] = new ImageIcon(tmp);
			}
			try
			{
				m.waitForAll();
			}
			catch (Exception e)
			{}
		}
	}

	/**Anon Level is >=0 amd <=5. If -1 no measure is available*/
	private ImageIcon getMeterImage(int iAnonLevel)
	{
		if (m_Controller.getAnonMode())
		{
			if (iAnonLevel >= 0 && iAnonLevel < 6)
			{
				return meterIcons[iAnonLevel + 2];
			}
			else
			{
				return meterIcons[1]; //No measure available
			}
		}
		else
		{
			return meterIcons[0]; // Anon deactivated
		}
	}

	public void showIconifiedView()
	{
		if (m_ViewIconified != null)
		{
			setVisible(false);
			m_ViewIconified.setVisible(true);
			m_ViewIconified.toFront();
		}
	}

	public void actionPerformed(ActionEvent event)
	{
		//		LogHolder.log(LogLevel.DEBUG,LogType.MISC,"GetEvent: "+event.getSource());
		Object source = event.getSource();
		if (source == m_bttnQuit)
		{
			exitProgram();
		}
		else if (source == m_bttnIconify)
		{
			showIconifiedView();
		}
		else if (source == m_bttnConf)
		{
			showConfigDialog();
			/*else if (event.getSource() == portB)
			 showConfigDialog(JAPConf.PORT_TAB);
			  else if (event.getSource() == httpB)
			 showConfigDialog(JAPConf.HTTP_TAB);
			  else if (event.getSource() == isB)
			 showConfigDialog(JAPConf.INFO_TAB);
			  else if (event.getSource() == anonB)
			 showConfigDialog(JAPConf.ANON_TAB);*/
		}
		//else if (source == m_bttnAnonConf)
		//{
		//showConfigDialog(JAPConf.ANON_TAB);
		//}
		else if (source == m_bttnInfo)
		{
			m_Controller.aboutJAP();
		}
		else if (source == m_bttnHelp)
		{
			showHelpWindow();
			//else if (event.getSource() == anonCheckBox)
			//	controller.setAnonMode(anonCheckBox.isSelected());
		}
		else if (source == m_rbAnonOn || source == m_rbAnonOff)
		{
			m_Controller.setAnonMode(m_rbAnonOn.isSelected());
		}
		else if (source == m_cbAnonymityOn)
		{
			m_Controller.setAnonMode(m_cbAnonymityOn.isSelected());
		}

		else
		{
			LogHolder.log(LogLevel.DEBUG, LogType.GUI, "Event ?????: " + event.getSource());
		}
	}

	private void showHelpWindow()
	{
		if (helpWindow == null)
		{
			helpWindow = new JAPHelp(this);
		}
		helpWindow.show();
	}

	private void showConfigDialog()
	{
		showConfigDialog(null);
	}

	private void showConfigDialog(String card)
	{
		if (m_dlgConfig == null)
		{
			Cursor c = getCursor();
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			m_dlgConfig = new JAPConf(this, loadPay);
			setCursor(c);
		}
		if (card != null)
		{
			m_dlgConfig.selectCard(card);
		}
		m_dlgConfig.updateValues();
		m_dlgConfig.show();
	}

	public JPanel getMainPanel()
	{
		return m_panelMain;
	}

	private void setOptimalSize()
	{
		try
		{
			if (!JAPModel.isSmallDisplay()) //only do this on "real" Displays
			{
				pack(); // optimize size
				setResizable( /*true*/true /*false*/); //2001-11-12(HF):Changed due to a Mac OS X problem during redraw of the progress bars
			}
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.EXCEPTION, LogType.GUI, "JAPView:Hm.. Error by Pack - Has To be fixed!!");
		}
	}

	public void doSynchronizedUpdateValues()
	{
		synchronized (m_runnableValueUpdate)
		{
			MixCascade currentMixCascade = m_Controller.getCurrentMixCascade();
			String strCascadeName = currentMixCascade.getName();
			Vector v = m_Controller.getMixCascadeDatabase();
			m_bIgnoreAnonComboEvents = true;
			boolean bMixCascadeAlreadyIncluded = false;
			m_comboAnonServices.removeAllItems();
			if (v != null&&v.size()>0)
			{
				Enumeration enumer = v.elements();
				while (enumer.hasMoreElements())
				{
					MixCascade c = (MixCascade) enumer.nextElement();
					m_comboAnonServices.addMixCascade(c);
					if (c.equals(currentMixCascade))
					{
						bMixCascadeAlreadyIncluded = true;
					}
				}
			}
			else
			{

				m_comboAnonServices.setNoDataAvailable();
			}
			if (!bMixCascadeAlreadyIncluded)
			{
				m_comboAnonServices.addMixCascade(currentMixCascade);
			}
			m_comboAnonServices.setSelectedItem(currentMixCascade);
			m_bIgnoreAnonComboEvents = false;
			m_comboAnonServices.setToolTipText(currentMixCascade.getName());

			// Config panel
			LogHolder.log(LogLevel.DEBUG, LogType.GUI, "JAPView:Start updateValues");
			// Meter panel
			try
			{
				m_rbAnonOn.setSelected(m_Controller.getAnonMode());
				m_rbAnonOff.setSelected(!m_Controller.getAnonMode());
				m_cbAnonymityOn.setSelected(m_Controller.getAnonMode());
				LogHolder.log(LogLevel.DEBUG, LogType.GUI, "JAPView: update CascadeName");
				m_labelCascadeName.setText(currentMixCascade.getName());
				m_labelCascadeName.setToolTipText(currentMixCascade.getName());
				StatusInfo currentStatus = currentMixCascade.getCurrentStatus();
				int anonLevel = currentStatus.getAnonLevel();
				m_labelAnonMeter.setIcon(getMeterImage(anonLevel));
				Color color = Color.red;
				if (anonLevel > 3)
				{
					color = Color.blue;
				}
				else if (anonLevel > 1)
				{
					color = Color.green;
				}
				( (MyProgressBarUI) m_progressAnonLevel.getUI()).setFilledBarColor(color);
				m_progressAnonLevel.setValue(anonLevel);
				if (m_Controller.getAnonMode())
				{
					if (currentStatus.getNrOfActiveUsers() > -1)
					{
						// Nr of active users
						//if (currentStatus.getNrOfActiveUsers() > userProgressBar.getMaximum())
						//{
						//	userProgressBar.setMaximum(currentStatus.getNrOfActiveUsers());
						//}
						m_labelAnonymityUser.setText(Integer.toString(currentStatus.getNrOfActiveUsers()));
						//userProgressBar.setString(String.valueOf(currentStatus.getNrOfActiveUsers()));
						if (m_bIsIconified)
						{
							setTitle("JAP (" + Integer.toString(currentStatus.getNrOfActiveUsers()) + " " +
									 JAPMessages.getString("iconifiedviewUsers") + ")");
						}
					}
					else
					{
						m_labelAnonymityUser.setText(JAPMessages.getString("meterNA"));

						//userProgressBar.setValue(userProgressBar.getMaximum());
						//userProgressBar.setString(JAPMessages.getString("meterNA"));
					}
					/*	if (currentStatus.getCurrentRisk() > -1)
					 {
					  // Current Risk
					  if (currentStatus.getCurrentRisk() > protectionProgressBar.getMaximum())
					  {
					   protectionProgressBar.setMaximum(currentStatus.getCurrentRisk());
					  }
					  protectionProgressBar.setValue(currentStatus.getCurrentRisk());
					  if (currentStatus.getCurrentRisk() < 80)
					  {
					   protectionProgressBar.setString(String.valueOf(currentStatus.getCurrentRisk()) +
					 " %");
					  }
					  else
					  {
					   protectionProgressBar.setString(JAPMessages.getString("meterRiskVeryHigh"));
					  }
					 }
					 else
					 {
					  protectionProgressBar.setValue(protectionProgressBar.getMaximum());
					  protectionProgressBar.setString(JAPMessages.getString("meterNA"));
					 }*/
					int t = currentStatus.getTrafficSituation();
					if (t > -1)
					{
						//Trafic Situation directly from InfoService
						/*trafficProgressBar.setMaximum(100);
						 trafficProgressBar.setValue(t);
						 if (t < 30)
						 {
						 trafficProgressBar.setString(JAPMessages.getString("meterTrafficLow"));
						 }
						 else
						 {
						 if (t < 60)
						 {
						  trafficProgressBar.setString(JAPMessages.getString("meterTrafficMedium"));
						 }
						 else
						 {
						  trafficProgressBar.setString(JAPMessages.getString("meterTrafficHigh"));
						 }
						 }*/
						//map 0..100 --> 0..5
						//0 --> 0
						//1..20 --> 1
						//21..40 --> 2
						//41..60 --> 3
						//61..80 --> 4
						//81..100 --> 5
						m_progressAnonTraffic.setValue( (t + 19) / 20);
					}
					else
					{
						// no value from InfoService
						m_progressAnonTraffic.setValue(0);

						//trafficProgressBar.setValue(trafficProgressBar.getMaximum());
						//trafficProgressBar.setString(JAPMessages.getString("meterNA"));
					}
				}
				else
				{
					/* we are not in anonymity mode */
					m_progressAnonTraffic.setValue(0);
					m_labelAnonymityUser.setText("");
					m_progressAnonLevel.setValue(0);
					//userProgressBar.setValue(userProgressBar.getMaximum());
					//userProgressBar.setString(JAPMessages.getString("meterNA"));
					//protectionProgressBar.setValue(protectionProgressBar.getMaximum());
					//protectionProgressBar.setString(JAPMessages.getString("meterNA"));
					//trafficProgressBar.setValue(trafficProgressBar.getMaximum());
					//trafficProgressBar.setString(JAPMessages.getString("meterNA"));
					LogHolder.log(LogLevel.DEBUG, LogType.GUI, "JAPView:Finished updateValues");
				}
				m_cbForwarding.setSelected(JAPModel.getInstance().getRoutingSettings().getRoutingMode() ==
										   JAPRoutingSettings.ROUTING_MODE_SERVER);
				m_cbForwardingSmall.setSelected(JAPModel.getInstance().getRoutingSettings().getRoutingMode() ==
												JAPRoutingSettings.ROUTING_MODE_SERVER);
			}
			catch (Throwable t)
			{
				LogHolder.log(LogLevel.EMERG, LogType.GUI,
							  "JAPVIew: Ooops... Crash in updateValues(): " + t.getMessage());
			}
		}
	}

	public void registerViewIconified(Window v)
	{
		m_ViewIconified = v;
	}

	public void channelsChanged(int c)
	{
		// Nr of Channels
		//int c=controller.getNrOfChannels();
		c = Math.min(c, m_progressOwnTrafficActivity.getMaximum());
		m_progressOwnTrafficActivity.setValue(c);
		m_progressOwnTrafficActivitySmall.setValue(c);
//			ownTrafficChannelsProgressBar.setString(String.valueOf(c));
	}

	public void transferedBytes(int c)
	{
		// Nr of Bytes transmitted anonymously
		String unit = JAPMessages.getString("Byte");
		if (c > 9999)
		{
			c = c / 1000;
			unit = JAPMessages.getString("kByte");
		}
		String s = m_NumberFormat.format(c);
		m_labelOwnTrafficBytes.setText(s);
		m_labelOwnTrafficUnit.setText(unit);
		m_labelOwnTrafficBytesSmall.setText(s);
		m_labelOwnTrafficUnitSmall.setText(unit);
		m_labelOwnTrafficBytesWWW.setText(s);
		m_labelOwnTrafficUnitWWW.setText(unit);
		JAPDll.onTraffic();
	}

	public Dimension getPreferredSize()
	{
		Dimension d = super.getPreferredSize();
		d.width = m_iPreferredWidth;
		return d;
	}

	public int addStatusMsg(String msg, int type)
	{
		return m_StatusPanel.addStatusMsg(msg, type);
	}

	public void removeStatusMsg(int id)
	{
		m_StatusPanel.removeStatusMsg(id);
	}
}
