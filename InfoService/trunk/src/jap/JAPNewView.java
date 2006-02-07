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
import java.util.Enumeration;
import java.util.Observable;
import java.util.Observer;
import java.util.Vector;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.MediaTracker;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.ButtonGroup;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JRadioButton;
import javax.swing.JSeparator;
import javax.swing.SwingConstants;
import javax.swing.border.EmptyBorder;
import javax.swing.border.LineBorder;

import anon.infoservice.MixCascade;
import anon.infoservice.StatusInfo;
import gui.FlippingPanel;
import gui.JAPDll;
import gui.JAPMixCascadeComboBox;
import gui.MyProgressBarUI;
import gui.StatusPanel;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import proxy.IProxyListener;
import javax.swing.UIManager;
import javax.swing.LookAndFeel;
import javax.swing.SwingUtilities;
import jap.pay.*;
import jap.forward.*;
import gui.*;

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
	private JAPConf m_dlgConfig;
	private Window m_ViewIconified;
	private NumberFormat m_NumberFormat;
	private boolean m_bIsIconified;
	//private final static boolean PROGRESSBARBORDER = true;
	//private GuthabenAnzeige guthaben;
	private boolean m_bWithPayment = false;

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
	private JLabel m_labelOwnTrafficBytesOther, m_labelOwnTrafficUnitOther;
	private JLabel m_labelForwarding, m_labelForwardingSmall;
	private JLabel m_labelForwardedTrafficBytes, m_labelForwardedTrafficBytesUnit;
	private JLabel m_labelForwarderCurrentConnections, m_labelForwarderAcceptedConnections;
	private JLabel m_labelForwarderRejectedConnections;
	private JLabel m_labelForwardedTraffic, m_labelForwarderUsedBandwidth;
	private JLabel m_labelForwarderCurrentConnectionsLabel, m_labelForwarderAcceptedConnectionsLabel;
	private JLabel m_labelForwarderRejectedConnectionsLabel, m_labelForwarderUsedBandwidthLabel;
	private JLabel m_labelForwarderConnections;
	private JLabel m_labelForwardingErrorSmall, m_labelForwardingError;
	private JProgressBar m_progressOwnTrafficActivity, m_progressOwnTrafficActivitySmall, m_progressAnonLevel;
	private JButton m_bttnAnonDetails, m_bttnReload;
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

	private long m_lTrafficWWW, m_lTrafficOther;

	private boolean m_bIsSimpleView;

	public JAPNewView(String s, JAPController a_controller)
	{
		super(s, a_controller);
		m_bIsSimpleView = (JAPModel.getDefaultView() == JAPConstants.VIEW_SIMPLIFIED);
		m_NumberFormat = NumberFormat.getInstance();
		m_Controller = JAPController.getInstance();
		m_dlgConfig = null; //new JAPConf(this);
		m_bIsIconified = false;
		m_lTrafficWWW = 0;
		m_lTrafficOther = 0;
	}

	public void create(boolean loadPay)
	{
		this.m_bWithPayment = loadPay;
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
		c.weighty = 0;
		JLabel l = new JLabel(JAPConstants.aktVersion);
		l.setForeground(Color.blue);
		l.setFont(new Font(l.getFont().getName(),
						   l.getFont().getStyle(),
						   (int) (l.getFont().getSize() * 0.8)));
		l.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		l.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent e)
			{
				JAPController.aboutJAP();
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
		m_bttnReload = new JButton(JAPUtil.loadImageIcon(JAPConstants.IMAGE_RELOAD, true));
		m_bttnReload.setOpaque(false);
		LookAndFeel laf = UIManager.getLookAndFeel();
		if (laf != null && UIManager.getCrossPlatformLookAndFeelClassName().equals(laf.getClass().getName())) //stupid but is necessary for JDK 1.5 and Metal L&F on Windows XP (and maybe others)
		{
			m_bttnReload.setBackground(Color.gray);
		}
		m_bttnReload.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				m_bttnReload.setEnabled(false);
				fetchMixCascadesAsync(true);
			}
		});
		m_bttnReload.setRolloverEnabled(true);
		m_bttnReload.setToolTipText(JAPMessages.getString("ngCascadeReloadTooltip"));
		ImageIcon tmpIcon = JAPUtil.loadImageIcon(JAPConstants.IMAGE_RELOAD_ROLLOVER, true);
		m_bttnReload.setRolloverIcon(tmpIcon);
		m_bttnReload.setSelectedIcon(tmpIcon);
		m_bttnReload.setRolloverSelectedIcon(tmpIcon);
		m_bttnReload.setPressedIcon(tmpIcon);
		m_bttnReload.setDisabledIcon(JAPUtil.loadImageIcon(JAPConstants.IMAGE_RELOAD_DISABLED, true));
		m_bttnReload.setBorder(new EmptyBorder(0, 0, 0, 0));
		m_bttnReload.setFocusPainted(false);

		c1.gridx = 2;
		c1.weightx = 0;
		c1.fill = GridBagConstraints.NONE;
		m_panelAnonService.add(m_bttnReload, c1);
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
		m_labelAnonymityUser = new JLabel("", SwingConstants.CENTER);
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
		m_cbAnonymityOn.setBorder(null);
		m_cbAnonymityOn.addActionListener(this);
		c1.gridx = 1;
		c1.insets = new Insets(0, 10, 0, 0);
		p.add(m_cbAnonymityOn, c1);
		m_labelAnonymityLow = new JLabel(JAPMessages.getString("ngAnonymityLow"), SwingConstants.RIGHT);
		c1.insets = new Insets(0, 20, 0, 5);
		c1.gridx = 2;
		c1.weightx = 0.5;
		c1.fill = GridBagConstraints.HORIZONTAL;
		c1.anchor = GridBagConstraints.EAST;
		p.add(m_labelAnonymityLow, c1);
		m_progressAnonLevel = new JProgressBar();
		m_progressAnonLevel.setMinimum(0);
		m_progressAnonLevel.setMaximum(6);
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
		c1.insets = new Insets(0, 0, 0, 0);
		c1.fill = GridBagConstraints.HORIZONTAL;
		c1.anchor = GridBagConstraints.WEST;
		p.add(m_labelAnonymityHigh, c1);
		m_flippingpanelAnon.setSmallPanel(p);

		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.gridy = 4;
		m_flippingpanelAnon.setFlipped(true);
		if (m_bIsSimpleView)
		{
			northPanel.add(m_flippingpanelAnon.getFullPanel(), c);
		}
		else
		{
			northPanel.add(m_flippingpanelAnon, c);
		}

//-----------------------------------------------------------
		c.gridwidth = 2;
		c.gridx = 0;
		c.gridy = 5;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		northPanel.add(new JSeparator(), c);

//------------------ Payment Panel
		if (m_bWithPayment)
		{
			m_flippingPanelPayment = new PaymentMainPanel(this);
			c.fill = GridBagConstraints.HORIZONTAL;
			c.weightx = 1;
			c.anchor = GridBagConstraints.NORTHWEST;
			c.gridy = 6;
			m_flippingPanelPayment.setFlipped(false);
			northPanel.add(m_flippingPanelPayment, c);
//-----------------------------------------------------------
			// Separator
			c.gridwidth = 2;
			c.gridx = 0;
			c.gridy = 7;
			c.fill = GridBagConstraints.HORIZONTAL;
			c.weightx = 1;
			northPanel.add(new JSeparator(), c);
		}
//-----------------------------------------------------------

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
		m_labelOwnTrafficBytes.setHorizontalAlignment(JLabel.RIGHT);
		c1.insets = new Insets(0, 5, 0, 0);
		c1.weightx = 0;
		c1.fill = GridBagConstraints.HORIZONTAL;
		c1.gridx = 2;
		p.add(m_labelOwnTrafficBytes, c1);
		m_labelOwnTrafficUnit = new JLabel(JAPMessages.getString("Byte"));
		c1.gridx = 3;
		p.add(m_labelOwnTrafficUnit, c1);
		m_labelOwnActivity = new JLabel(JAPMessages.getString("ngActivity"), SwingConstants.RIGHT);
		c1.weightx = 0;
		c1.fill = GridBagConstraints.HORIZONTAL;
		c1.gridx = 4;
		c1.insets = new Insets(0, 10, 0, 0);
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
		c1.insets = new Insets(0, 5, 0, 0);
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
		m_labelOwnTrafficBytesWWW.setHorizontalAlignment(JLabel.RIGHT);
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
		m_labelOwnTrafficBytesOther = new JLabel("0");
		m_labelOwnTrafficBytesOther.setHorizontalAlignment(JLabel.RIGHT);
		c1.fill = GridBagConstraints.HORIZONTAL;
		c1.weightx = 0;
		c1.insets = new Insets(7, 5, 0, 0);
		c1.gridx = 2;
		p.add(m_labelOwnTrafficBytesOther, c1);
		m_labelOwnTrafficUnitOther = new JLabel(JAPMessages.getString("Byte"));
		c1.gridx = 3;
		p.add(m_labelOwnTrafficUnitOther, c1);
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
		m_labelOwnTrafficBytesSmall.setHorizontalAlignment(JLabel.RIGHT);
		c1.weightx = 1;
		c1.fill = GridBagConstraints.HORIZONTAL;
		c1.gridx = 1;
		p.add(m_labelOwnTrafficBytesSmall, c1);
		m_labelOwnTrafficUnitSmall = new JLabel(JAPMessages.getString("Byte"));
		c1.gridx = 2;
		c1.weightx = 0;
		c1.fill = GridBagConstraints.NONE;
		p.add(m_labelOwnTrafficUnitSmall, c1);
		m_labelOwnActivitySmall = new JLabel(JAPMessages.getString("ngActivity"), SwingConstants.RIGHT);
		c1.insets = new Insets(0, 10, 0, 0);
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
		c1.insets = new Insets(0, 5, 0, 0);
		c1.fill = GridBagConstraints.NONE;
		c1.gridx = 4;
		p.add(m_progressOwnTrafficActivitySmall, c1);
		m_flippingpanelOwnTraffic.setSmallPanel(p);

		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.gridy = 8;
		if (m_bIsSimpleView)
		{
			northPanel.add(m_flippingpanelOwnTraffic.getSmallPanel(), c);
		}
		else
		{
			northPanel.add(m_flippingpanelOwnTraffic, c);
		}

//-----------------------------------------------------------
		c.gridwidth = 2;
		c.gridx = 0;
		c.gridy = 9;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		northPanel.add(new JSeparator(), c);

// Forwarder Panel
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.gridy = 10;
		m_flippingpanelForward = buildForwarderPanel();
		if (!m_bIsSimpleView)
		{
			northPanel.add(m_flippingpanelForward, c);

//-----------------------------------------------------------
			c.gridwidth = 2;
			c.gridx = 0;
			c.gridy = 11;
			c.fill = GridBagConstraints.HORIZONTAL;
			c.weightx = 1;
			northPanel.add(new JSeparator(), c);
		}
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
		c1.fill = GridBagConstraints.VERTICAL;
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
		ImageIcon westImage = JAPUtil.loadImageIcon(JAPMessages.getString("westPath"), true);
		JLabel westLabel = new JLabel(westImage);

		// "Center:" tabs
		//JTabbedPane tabs = new JTabbedPane();
		//JPanel config = buildConfigPanel();
		JPanel level = buildLevelPanel();
		// "South": Buttons

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
		m_bttnReload.setEnabled(false);
		fetchMixCascadesAsync(false);
		valuesChanged(true);
		setOptimalSize();
		JAPUtil.centerFrame(this);
		//Change size and location if the user requested to restore the old position/size
		if (JAPModel.getSaveMainWindowPosition())
		{
			JAPModel m = JAPModel.getInstance();
			Dimension ds = Toolkit.getDefaultToolkit().getScreenSize();
			Point oldLocation = m.getOldMainWindowLocation();
			if (oldLocation != null && oldLocation.x >= 0 &&
				oldLocation.y >= 0 /*&&m.m_OldMainWindowLocation.x<ds.width&&
					   m.m_OldMainWindowLocation.y<ds.height*/
				)
			{
				setLocation(oldLocation);
			}
			/*		if (m.m_OldMainWindowSize != null && m.m_OldMainWindowSize.height > 0 &&
			   m.m_OldMainWindowSize.width > 0)
			  {
			   setSize(m.m_OldMainWindowSize);
			  }*/
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
		m_labelOwnChannels = new JLabel(JAPMessages.getString("ownTrafficChannels"));
		m_labelOwnChannels.setFont(fontControls);
		ownTrafficPanel.add(m_labelOwnChannels);
		m_labelOwnBytes = new JLabel(JAPMessages.getString("ownTrafficBytes"));
		m_labelOwnBytes.setFont(fontControls);
		ownTrafficPanel.add(m_labelOwnBytes);

// "Guthaben"

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

		// details panel
		JPanel detailsPanel = new JPanel();
		m_labelCascadeName = new JLabel();
		m_labelCascadeName.setFont(fontControls);
		m_labelMeterDetailsRisk = new JLabel(JAPMessages.getString("meterDetailsRisk") + " ");
		m_labelMeterDetailsRisk.setFont(fontControls);
		GridBagLayout g = new GridBagLayout();
		detailsPanel.setLayout(g);
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.WEST;
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

	private FlippingPanel buildForwarderPanel()
	{
		//------------------ Forwarder Panel
		FlippingPanel flippingPanel = new FlippingPanel(this);
		//big view
		GridBagConstraints c1 = new GridBagConstraints();
		c1.insets = new Insets(0, 5, 0, 0);
		c1.anchor = GridBagConstraints.WEST;
		JPanel p = new JPanel(new GridBagLayout());
		GridBagLayout gbl = new GridBagLayout();
		GridBagConstraints c2 = new GridBagConstraints();
		JPanel p2 = new JPanel(new GridBagLayout());
		m_labelForwarding = new JLabel(JAPMessages.getString("ngForwarding"));
		c2.insets = new Insets(0, 0, 0, 0);
		c2.anchor = GridBagConstraints.WEST;
		p2.add(m_labelForwarding, c2);

		m_cbForwarding = new JCheckBox(JAPMessages.getString("ngForwardingOn"));
		m_cbForwarding.setBorder(null);
		ActionListener actionListener = new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				/* start or shutdown the forwarding server */
				m_Controller.enableForwardingServer( ( (JCheckBox) e.getSource()).isSelected());
			}
		};
		m_cbForwarding.addActionListener(actionListener);
		c2.gridx = 1;
		c2.weightx = 0;
		c2.fill = GridBagConstraints.NONE;
		c2.insets = new Insets(0, 5, 0, 0);
		p2.add(m_cbForwarding, c2);

		m_labelForwardingError = new JLabel();
		c2.gridx = 2;
		c2.weightx = 1;
		c2.fill = GridBagConstraints.NONE;
		c2.insets = new Insets(0, 15, 0, 0);
		p2.add(m_labelForwardingError, c2);

		m_labelForwarderActivity = new JLabel(JAPMessages.getString("ngActivity"));
		c2.insets = new Insets(0, 5, 0, 0);
		c2.gridx = 3;
		c2.weightx = 0;
		c2.fill = GridBagConstraints.NONE;
		p2.add(m_labelForwarderActivity, c2);
		JProgressBar progress = new JProgressBar();
		progress.setUI(new MyProgressBarUI(true));
		progress.setMinimum(0);
		progress.setMaximum(5);
		progress.setBorderPainted(false);
		c2.gridx = 4;
		p2.add(progress, c2);
		c1.fill = GridBagConstraints.HORIZONTAL;
		c1.weightx = 1;
		c1.gridx = 0;
		c1.gridwidth = 4;
		p.add(p2, c1);

		m_labelForwarderConnections = new JLabel(JAPMessages.getString("ngForwardedConnections"));
		c1.gridx = 0;
		c1.gridy = 1;
		c1.fill = GridBagConstraints.NONE;
		c1.weightx = 0;
		c1.gridwidth = 1;
		c1.insets = new Insets(10, 5, 0, 0);
		p.add(m_labelForwarderConnections, c1);
		JPanel spacer = new JPanel();
		Dimension spacerDimension = new Dimension(
			m_labelForwarderConnections.getFontMetrics(
				m_labelForwarderConnections.getFont()).charWidth('9') * 6, 1);
		spacer.setPreferredSize(spacerDimension);
		c1.fill = GridBagConstraints.NONE;
		c1.weightx = 0;
		c1.gridx = 1;
		c1.insets = new Insets(0, 0, 0, 0);
		p.add(spacer, c1);
		m_labelForwarderCurrentConnections = new JLabel("0");
		m_labelForwarderCurrentConnections.setHorizontalAlignment(JLabel.RIGHT);
		c1.insets = new Insets(10, 5, 0, 0);
		c1.fill = GridBagConstraints.HORIZONTAL;
		c1.weightx = 0;
		c1.gridx = 2;
		p.add(m_labelForwarderCurrentConnections, c1);
		m_labelForwarderCurrentConnectionsLabel = new JLabel(
			JAPMessages.getString("ngForwardedCurrentConnections"));
		c1.gridx = 3;
		p.add(m_labelForwarderCurrentConnectionsLabel, c1);
		m_labelForwarderAcceptedConnections = new JLabel("0");
		m_labelForwarderAcceptedConnections.setHorizontalAlignment(JLabel.RIGHT);
		c1.insets = new Insets(7, 5, 0, 0);
		c1.gridx = 2;
		c1.gridy = 2;
		p.add(m_labelForwarderAcceptedConnections, c1);
		m_labelForwarderAcceptedConnectionsLabel = new JLabel(
			JAPMessages.getString("ngForwardedAcceptedConnections"));
		c1.gridx = 3;
		p.add(m_labelForwarderAcceptedConnectionsLabel, c1);
		m_labelForwarderRejectedConnections = new JLabel("0");
		m_labelForwarderRejectedConnections.setHorizontalAlignment(JLabel.RIGHT);
		c1.gridx = 2;
		c1.gridy = 3;
		p.add(m_labelForwarderRejectedConnections, c1);
		m_labelForwarderRejectedConnectionsLabel = new JLabel(
			JAPMessages.getString("ngForwardedRejectedConnections"));
		c1.gridx = 3;
		p.add(m_labelForwarderRejectedConnectionsLabel, c1);
		m_labelForwardedTraffic = new JLabel(JAPMessages.getString("ngForwardedTraffic"));
		c1.gridx = 0;
		c1.gridy = 4;
		p.add(m_labelForwardedTraffic, c1);
		m_labelForwardedTrafficBytes = new JLabel("0");
		m_labelForwardedTrafficBytes.setHorizontalAlignment(JLabel.RIGHT);
		c1.gridx = 2;
		p.add(m_labelForwardedTrafficBytes, c1);
		m_labelForwardedTrafficBytesUnit = new JLabel(JAPMessages.getString("Byte"));
		c1.gridx = 3;
		p.add(m_labelForwardedTrafficBytesUnit, c1);
		m_labelForwarderUsedBandwidthLabel = new JLabel(JAPMessages.getString("ngForwardedUsedBandwidth"));
		c1.gridx = 0;
		c1.gridy = 5;
		p.add(m_labelForwarderUsedBandwidthLabel, c1);
		m_labelForwarderUsedBandwidth = new JLabel("0");
		m_labelForwarderUsedBandwidth.setHorizontalAlignment(JLabel.RIGHT);
		c1.gridx = 2;
		p.add(m_labelForwarderUsedBandwidth, c1);
		JLabel l = new JLabel("Byte/s");
		c1.gridx = 3;
		p.add(l, c1);

		flippingPanel.setFullPanel(p);

		//smallview
		c1 = new GridBagConstraints();
		p = new JPanel(new GridBagLayout());
		m_labelForwardingSmall = new JLabel(JAPMessages.getString("ngForwarding"));
		c1.insets = new Insets(0, 5, 0, 0);
		c1.anchor = GridBagConstraints.WEST;
		p.add(m_labelForwardingSmall, c1);
		c1.gridx = 1;
		c1.weightx = 0;
		c1.fill = GridBagConstraints.HORIZONTAL;
		m_cbForwardingSmall = new JCheckBox(JAPMessages.getString("ngForwardingOn"));
		m_cbForwardingSmall.setBorder(null);
		m_cbForwardingSmall.addActionListener(actionListener);
		p.add(m_cbForwardingSmall, c1);
		m_labelForwardingErrorSmall = new JLabel();
		c1.gridx = 2;
		c1.weightx = 1;
		c1.fill = GridBagConstraints.NONE;
		c1.insets = new Insets(0, 15, 0, 0);
		p.add(m_labelForwardingErrorSmall, c1);

		m_labelForwarderActivitySmall = new JLabel(JAPMessages.getString("ngActivity"));
		c1.gridx = 3;
		c1.weightx = 0;
		c1.fill = GridBagConstraints.NONE;
		c1.insets = new Insets(0, 5, 0, 0);
		p.add(m_labelForwarderActivitySmall, c1);
		progress = new JProgressBar();
		progress.setUI(new MyProgressBarUI(true));
		progress.setMinimum(0);
		progress.setMaximum(5);
		progress.setBorderPainted(false);
		c1.gridx = 4;
		p.add(progress, c1);
		flippingPanel.setSmallPanel(p);

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
						m_labelForwardedTrafficBytes.setText(JAPUtil.formatBytesValueWithoutUnit(c));
						m_labelForwardedTrafficBytesUnit.setText(JAPUtil.formatBytesValueOnlyUnit(c));
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
		return flippingPanel;
	}

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

		m_bttnReload.setToolTipText(JAPMessages.getString("ngCascadeReloadTooltip"));
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
		m_labelForwarderAcceptedConnectionsLabel.setText(
			JAPMessages.getString("ngForwardedAcceptedConnections"));
		m_labelForwarderRejectedConnectionsLabel.setText(
			JAPMessages.getString("ngForwardedRejectedConnections"));
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
		if (m_bWithPayment)
		{
			m_labelPayment.setText(JAPMessages.getString("ngPayment"));

		}
		m_labelMeterDetailsRisk.setText(JAPMessages.getString("meterDetailsRisk") + " ");
		m_labelOwnChannels.setText(JAPMessages.getString("ownTrafficChannels"));
		m_labelOwnBytes.setText(JAPMessages.getString("ownTrafficBytes"));
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

	void loadMeterIcons()
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
			JAPController.aboutJAP();
		}
		else if (source == m_bttnHelp)
		{
			showHelpWindow();
			//else if (event.getSource() == anonCheckBox)
			//	controller.setAnonMode(anonCheckBox.isSelected());
		}
		else if (source == m_rbAnonOn || source == m_rbAnonOff)
		{
			if (m_rbAnonOn.isSelected())
			{
				m_Controller.startAnonymousMode(this);
			}
			else
			{
				m_Controller.setAnonMode(false);
			}
		}
		else if (source == m_cbAnonymityOn)
		{
			if (m_cbAnonymityOn.isSelected())
			{
				m_Controller.startAnonymousMode(this);
			}
			else
			{
				m_Controller.setAnonMode(false);
			}
		}

		else
		{
			LogHolder.log(LogLevel.DEBUG, LogType.GUI, "Event ?????: " + event.getSource());
		}
	}

	private void showHelpWindow()
	{
		JAPHelp help = JAPHelp.getInstance();
		help.getContextObj().setContext("index");
		help.loadCurrentContext();
	}

	private void showConfigDialog()
	{
		showConfigDialog(null);
	}

	public void showConfigDialog(String card)
	{
		if (m_dlgConfig == null)
		{
			Cursor c = getCursor();
			setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			m_dlgConfig = new JAPConf(this, m_bWithPayment);
			m_dlgConfig.addComponentListener(new ComponentListener()
			{
				public void componentResized(ComponentEvent e)
				{
				}

				public void componentMoved(ComponentEvent e)
				{
				}

				public void componentShown(ComponentEvent e)
				{
				}

				public void componentHidden(ComponentEvent e)
				{
					setEnabled(true);
				}
			});
			setCursor(c);
		}
		if (card != null)
		{
			m_dlgConfig.selectCard(card);
		}
		m_dlgConfig.updateValues();
		setEnabled(false);
		m_dlgConfig.showDialog();
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
			//String strCascadeName = currentMixCascade.getName();
			Vector v = m_Controller.getMixCascadeDatabase();
			m_bIgnoreAnonComboEvents = true;
			boolean bMixCascadeAlreadyIncluded = false;
			m_comboAnonServices.removeAllItems();
			if (v != null && v.size() > 0)
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
					color = Color.green;
				}
				else if (anonLevel > 1)
				{
					color = Color.yellow;
				}
				( (MyProgressBarUI) m_progressAnonLevel.getUI()).setFilledBarColor(color);
				m_progressAnonLevel.setValue(anonLevel + 1);
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

					}
					int t = currentStatus.getTrafficSituation();
					if (t > -1)
					{
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

					}
				}
				else
				{
					/* we are not in anonymity mode */
					m_progressAnonTraffic.setValue(0);
					m_labelAnonymityUser.setText("");
					m_progressAnonLevel.setValue(0);
					LogHolder.log(LogLevel.DEBUG, LogType.GUI, "JAPView:Finished updateValues");
				}
				boolean bForwaringServerOn = JAPModel.getInstance().getRoutingSettings().getRoutingMode() ==
					JAPRoutingSettings.ROUTING_MODE_SERVER;
				m_cbForwarding.setSelected(bForwaringServerOn);
				m_cbForwardingSmall.setSelected(bForwaringServerOn);
				Icon icon = null;
				String strError = null;
				String strText = null;
				if (bForwaringServerOn)
				{
					/* update the server state label and the reason of error, if necessary */
					int currentRegistrationState = JAPModel.getInstance().getRoutingSettings().
						getRegistrationStatusObserver().getCurrentState();
					int currentErrorCode = JAPModel.getInstance().getRoutingSettings().
						getRegistrationStatusObserver().getCurrentErrorCode();
					if (currentRegistrationState ==
						JAPRoutingRegistrationStatusObserver.STATE_NO_REGISTRATION)
					{
						icon = JAPUtil.loadImageIcon(JAPConstants.IMAGE_WARNING, true);
						if (currentErrorCode ==
							JAPRoutingRegistrationStatusObserver.ERROR_NO_KNOWN_PRIMARY_INFOSERVICES)
						{
							strError = "settingsRoutingServerStatusRegistrationErrorLabelNoKnownInfoServices";
						}
						else if (currentErrorCode ==
								 JAPRoutingRegistrationStatusObserver.ERROR_INFOSERVICE_CONNECT_ERROR)
						{
							strError = "settingsRoutingServerStatusRegistrationErrorLabelConnectionFailed";
						}
						else if (currentErrorCode ==
								 JAPRoutingRegistrationStatusObserver.ERROR_VERIFICATION_ERROR)
						{
							strError = "settingsRoutingServerStatusRegistrationErrorLabelVerificationFailed";
						}
						else if (currentErrorCode == JAPRoutingRegistrationStatusObserver.ERROR_UNKNOWN_ERROR)
						{
							strError = "settingsRoutingServerStatusRegistrationErrorLabelUnknownReason";

						}
						if (strError != null)
						{
							strError = JAPMessages.getString(strError);
						}
					}
				}
				m_labelForwardingError.setIcon(icon);
				m_labelForwardingErrorSmall.setIcon(icon);
				m_labelForwardingError.setToolTipText(strError);
				m_labelForwardingErrorSmall.setToolTipText(strError);

				/* if the forwarding client is running, it should not be possible to start the forwarding
				 * server, also it should not be possible to change the selected mixcascade
				 */
				m_cbForwarding.setEnabled(!JAPModel.getInstance().getRoutingSettings().isConnectViaForwarder());
				m_cbForwardingSmall.setEnabled(!JAPModel.getInstance().getRoutingSettings().
											   isConnectViaForwarder());
				m_comboAnonServices.setEnabled(!JAPModel.getInstance().getRoutingSettings().
											   isConnectViaForwarder());
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

	public void transferedBytes(long c, int protocolType)
	{
		// Nr of Bytes transmitted anonymously
		if (protocolType == IProxyListener.PROTOCOL_WWW)
		{
			m_lTrafficWWW = JAPController.getInstance().getMixedBytes();
		}
		else if (protocolType == IProxyListener.PROTOCOL_OTHER)
		{
			m_lTrafficOther = c;

		}
		String unit = JAPUtil.formatBytesValueOnlyUnit(m_lTrafficWWW);
		m_labelOwnTrafficUnitWWW.setText(unit);
		String s = JAPUtil.formatBytesValueWithoutUnit(m_lTrafficWWW);
		m_labelOwnTrafficBytesWWW.setText(s);
		unit = JAPUtil.formatBytesValueOnlyUnit(m_lTrafficOther);
		m_labelOwnTrafficUnitOther.setText(unit);
		s = JAPUtil.formatBytesValueWithoutUnit(m_lTrafficOther);
		m_labelOwnTrafficBytesOther.setText(s);
		long sum = m_lTrafficWWW + m_lTrafficOther;
		unit = JAPUtil.formatBytesValueOnlyUnit(sum);
		m_labelOwnTrafficUnit.setText(unit);
		m_labelOwnTrafficUnitSmall.setText(unit);
		s = JAPUtil.formatBytesValueWithoutUnit(sum);
		m_labelOwnTrafficBytes.setText(s);
		this.m_labelOwnTrafficBytesSmall.setText(s);
		JAPDll.onTraffic();
	}

	public Dimension getPreferredSize()
	{
		Dimension d = super.getPreferredSize();
		d.width = m_iPreferredWidth;
		return d;
	}

	public int addStatusMsg(String msg, int type, boolean bAutoRemove)
	{
		return m_StatusPanel.addStatusMsg(msg, type, bAutoRemove);
	}

	public void removeStatusMsg(int id)
	{
		m_StatusPanel.removeStatusMsg(id);
	}

	private synchronized void fetchMixCascadesAsync(final boolean bShowError)
	{
		setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		Runnable doFetchMixCascades = new Runnable()
		{
			public void run()
			{
				m_Controller.fetchMixCascades(bShowError);
				setCursor(Cursor.getDefaultCursor());
				SwingUtilities.invokeLater(new Runnable()
				{
					public void run()
					{
						m_bttnReload.setEnabled(true);
					}
				});
			}
		};
		Thread t = new Thread(doFetchMixCascades);
		t.start();
	}

}
