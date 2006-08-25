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

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.JCheckBox;
import javax.swing.event.ChangeListener;
import javax.swing.event.ChangeEvent;
import javax.swing.JRadioButton;
import javax.swing.ButtonGroup;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JSlider;
import javax.swing.SwingConstants;

import gui.JAPHelp;
import gui.JAPMessages;
import gui.GUIUtils;
import javax.swing.JList;
import javax.swing.DefaultListModel;
import javax.swing.JTextArea;
import javax.swing.ListSelectionModel;
import javax.swing.JLabel;
import javax.swing.JScrollPane;
import java.util.Dictionary;

final class JAPConfAnonGeneral extends AbstractJAPConfModule
{
	public static final String MSG_DENY_NON_ANONYMOUS_SURFING = JAPConfAnonGeneral.class.getName() +
		"_denyNonAnonymousSurfing";
	private static final String MSG_AUTO_CHOOSE_CASCADES = JAPConfAnonGeneral.class.getName() +
		"_autoChooseCascades";
	private static final String MSG_RESTRICT_AUTO_CHOOSE = JAPConfAnonGeneral.class.getName() +
		"_RestrictAutoChoosing";
	private static final String MSG_DO_NOT_RESTRICT_AUTO_CHOOSE = JAPConfAnonGeneral.class.getName() +
		"_doNotRestrictAutoChoosing";
	private static final String MSG_RESTRICT_AUTO_CHOOSE_PAY = JAPConfAnonGeneral.class.getName() +
		"_restrictAutoChoosingPay";
	private static final String MSG_KNOWN_CASCADES = JAPConfAnonGeneral.class.getName() +
		"_knownCascades";
	private static final String MSG_ALLOWED_CASCADES = JAPConfAnonGeneral.class.getName() +
		"_allowedCascades";
	private static final String MSG_AUTO_CHOOSE_ON_START = JAPConfAnonGeneral.class.getName() +
		"_autoChooseOnStart";

	private static final String IMG_ARROW_RIGHT = JAPConfAnonGeneral.class.getName() + "_arrowRight.gif";
	private static final String IMG_ARROW_LEFT = JAPConfAnonGeneral.class.getName() + "_arrowLeft.gif";

	private static final int DT_INTERVAL_STEPLENGTH = 10;
	private static final int DT_INTERVAL_STEPS = 6;
	private static final int DT_INTERVAL_DEFAULT = 3;

	private JCheckBox m_cbDenyNonAnonymousSurfing;
	private JCheckBox m_cbDummyTraffic;
	private JCheckBox m_cbAutoConnect;
	private JCheckBox m_cbAutoReConnect;
	private JCheckBox m_cbAutoChooseCascades;
	private JRadioButton m_cbRestrictAutoChoose;
	private JRadioButton m_cbRestrictAutoChoosePay;
	private JRadioButton m_cbDoNotRestrictAutoChoose;
	private JSlider m_sliderDummyTrafficIntervall;
	private JAPController m_Controller;

	private JPanel m_panelRestrictedCascades;

	private JList m_knownCascadesList;

	protected JAPConfAnonGeneral(IJAPConfSavePoint savePoint)
	{
		super(null);
		m_Controller = JAPController.getInstance();
	}

	public String getTabTitle()
	{
		return JAPMessages.getString("ngAnonGeneralPanelTitle");
	}

//updateGui
	protected void onUpdateValues()
	{
		int iTmp = JAPModel.getDummyTraffic();
		m_cbDummyTraffic.setSelected(iTmp > -1);
		if (iTmp > -1)
		{
			m_sliderDummyTrafficIntervall.setValue(iTmp / 1000);
		}
		m_sliderDummyTrafficIntervall.setEnabled(iTmp > -1);
		Dictionary d = m_sliderDummyTrafficIntervall.getLabelTable();
		for (int i = 1; i <= DT_INTERVAL_STEPS; i++)
		{
			( (JLabel) d.get(new Integer(i * DT_INTERVAL_STEPLENGTH))).setEnabled(
						 m_sliderDummyTrafficIntervall.isEnabled());
		}
		m_cbDenyNonAnonymousSurfing.setSelected(JAPModel.getInstance().isNonAnonymousSurfingDenied());
		m_cbAutoConnect.setSelected(JAPModel.getAutoConnect());
		m_cbAutoReConnect.setSelected(JAPModel.isAutomaticallyReconnected());
		m_cbAutoChooseCascades.setSelected(JAPModel.getInstance().isCascadeConnectionChosenAutomatically());
		if (JAPModel.getInstance().getAutomaticCascadeChangeRestriction().equals(
			  JAPModel.AUTO_CHANGE_RESTRICT))
		{
		   m_cbRestrictAutoChoose.setSelected(true);
		   m_panelRestrictedCascades.setEnabled(true);
		}
		else if (JAPModel.getInstance().getAutomaticCascadeChangeRestriction().equals(
			  JAPModel.AUTO_CHANGE_RESTRICT_TO_PAY))
		{
			m_cbRestrictAutoChoosePay.setSelected(true);
			m_panelRestrictedCascades.setEnabled(false);
		}
		else
		{
			m_cbDoNotRestrictAutoChoose.setSelected(true);
			m_panelRestrictedCascades.setEnabled(false);
		}
		//m_cbAutoChooseCascades.setEnabled(m_cbAutoReConnect.isSelected());

		m_cbRestrictAutoChoose.setEnabled(m_cbAutoChooseCascades.isSelected());
		m_cbDoNotRestrictAutoChoose.setEnabled(m_cbAutoChooseCascades.isSelected());
		m_cbRestrictAutoChoosePay.setEnabled(m_cbAutoChooseCascades.isSelected());
		m_panelRestrictedCascades.setEnabled(m_panelRestrictedCascades.isEnabled() &&
											 m_cbAutoChooseCascades.isSelected());
	}

//ok pressed
	protected boolean onOkPressed()
	{
		int dummyTraffic;
		if (m_cbDummyTraffic.isSelected())
		{
			dummyTraffic = m_sliderDummyTrafficIntervall.getValue() * 1000;
		}
		else
		{
			dummyTraffic = - 1;
			// Listener settings
		}
		/*
		 * Set DT asynchronous; otherwise, the Event Thread is locked while the AnonClient connects
		 */
		final int dtAsync = dummyTraffic;
		new Thread()
		{
			public void run()
			{
				m_Controller.setDummyTraffic(dtAsync);
			}
		}.start();

		// Anonservice settings
		JAPModel.getInstance().denyNonAnonymousSurfing(m_cbDenyNonAnonymousSurfing.isSelected());
		JAPModel.getInstance().setAutoConnect(m_cbAutoConnect.isSelected());
		JAPModel.getInstance().setAutoReConnect(m_cbAutoReConnect.isSelected());
		JAPModel.getInstance().setChooseCascadeConnectionAutomatically(m_cbAutoChooseCascades.isSelected());
		if (m_cbRestrictAutoChoose.isSelected())
		{
			JAPModel.getInstance().setAutomaticCascadeChangeRestriction(JAPModel.AUTO_CHANGE_RESTRICT);
		}
		else if (m_cbRestrictAutoChoosePay.isSelected())
		{
			JAPModel.getInstance().setAutomaticCascadeChangeRestriction(JAPModel.AUTO_CHANGE_RESTRICT_TO_PAY);
		}
		else
		{
			JAPModel.getInstance().setAutomaticCascadeChangeRestriction(JAPModel.AUTO_CHANGE_NO_RESTRICTION);
		}
		return true;
	}

	public void recreateRootPanel()
	{
		JPanel panelRoot = getRootPanel();
		panelRoot.removeAll();
		//Font font = getFontSetting();
		m_cbDenyNonAnonymousSurfing = new JCheckBox(JAPMessages.getString(MSG_DENY_NON_ANONYMOUS_SURFING));
		//m_cbDenyNonAnonymousSurfing.setFont(font);
		m_cbAutoConnect = new JCheckBox(JAPMessages.getString("settingsautoConnectCheckBox"));
		//m_cbAutoConnect.setFont(font);
		m_cbAutoReConnect = new JCheckBox(JAPMessages.getString("settingsautoReConnectCheckBox"));
		//m_cbAutoReConnect.setFont(font);
		m_cbAutoChooseCascades = new JCheckBox(JAPMessages.getString(MSG_AUTO_CHOOSE_CASCADES));
		//m_cbAutoChooseCascades.setFont(font);
		m_cbDoNotRestrictAutoChoose = new JRadioButton(JAPMessages.getString(MSG_DO_NOT_RESTRICT_AUTO_CHOOSE));
		//m_cbDoNotRestrictAutoChoose.setFont(font);
		m_cbRestrictAutoChoosePay = new JRadioButton(JAPMessages.getString(MSG_RESTRICT_AUTO_CHOOSE_PAY));
		//m_cbRestrictAutoChoosePay.setFont(font);
		m_cbRestrictAutoChoose = new JRadioButton(JAPMessages.getString(MSG_RESTRICT_AUTO_CHOOSE) + ":");
		//m_cbRestrictAutoChoose.setFont(font);
		/** @todo Implement and show the whitelist button */
		m_cbRestrictAutoChoose.setVisible(false);
		ButtonGroup groupAutoChoose = new ButtonGroup();
		groupAutoChoose.add(m_cbDoNotRestrictAutoChoose);
		groupAutoChoose.add(m_cbRestrictAutoChoosePay);
		groupAutoChoose.add(m_cbRestrictAutoChoose);


		m_cbAutoReConnect.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent a_event)
			{
				//m_cbAutoChooseCascades.setEnabled(m_cbAutoReConnect.isSelected());
				m_cbRestrictAutoChoose.setEnabled(m_cbAutoChooseCascades.isSelected());
				m_cbRestrictAutoChoosePay.setEnabled(m_cbAutoChooseCascades.isSelected());
				m_cbDoNotRestrictAutoChoose.setEnabled(m_cbAutoChooseCascades.isSelected());
				m_panelRestrictedCascades.setEnabled(
					m_cbAutoChooseCascades.isSelected() && m_cbRestrictAutoChoose.isSelected());
			}
		});
		m_cbAutoChooseCascades.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent a_event)
			{
				m_cbRestrictAutoChoose.setEnabled(m_cbAutoChooseCascades.isSelected());
				m_cbRestrictAutoChoosePay.setEnabled(m_cbAutoChooseCascades.isSelected());
				m_cbDoNotRestrictAutoChoose.setEnabled(m_cbAutoChooseCascades.isSelected());
				m_panelRestrictedCascades.setEnabled(m_cbAutoChooseCascades.isSelected() &&
					m_cbRestrictAutoChoose.isSelected());
			}
		});
		m_cbRestrictAutoChoose.addChangeListener(new ChangeListener()
		{
			public void stateChanged(ChangeEvent a_event)
			{
				m_panelRestrictedCascades.setEnabled(m_cbRestrictAutoChoose.isSelected());
			}
		});



		m_cbDummyTraffic = new JCheckBox(JAPMessages.getString("ngConfAnonGeneralSendDummy"));

		GridBagLayout gb = new GridBagLayout();
		panelRoot.setLayout(gb);
		GridBagConstraints c = new GridBagConstraints();

		c.gridx = 0;
		c.gridy = 0;
		c.gridheight = 1;
		c.gridwidth = 1;
		c.insets = new Insets(10, 10, 0, 10);
		c.anchor = GridBagConstraints.NORTHWEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		c.weighty = 0;
		panelRoot.add(m_cbDenyNonAnonymousSurfing, c);
		c.gridy++;
		panelRoot.add(m_cbAutoConnect, c);
		c.gridy++;
		panelRoot.add(m_cbAutoReConnect, c);
		c.gridy++;
		Insets insets = c.insets;
		//c.insets = new Insets(insets.top, insets.left + 20, insets.bottom, insets.right);
		panelRoot.add(m_cbAutoChooseCascades, c);
		c.gridy++;
		c.insets = new Insets(c.insets.top, c.insets.left + 20, c.insets.bottom, c.insets.right);
		panelRoot.add(m_cbDoNotRestrictAutoChoose, c);
		c.gridy++;
		panelRoot.add(m_cbRestrictAutoChoosePay, c);
		c.gridy++;
		panelRoot.add(m_cbRestrictAutoChoose, c);
		c.insets = insets;
		c.gridy++;



		m_panelRestrictedCascades = createRestrictedCacadesPanel();
		/** @todo Implement and show the whitelist */
		m_panelRestrictedCascades.setVisible(false);

		panelRoot.add(m_panelRestrictedCascades, c);
		c.gridy++;

		panelRoot.add(m_cbDummyTraffic, c);
		c.gridy++;
		c.weighty = 1.0;
		m_sliderDummyTrafficIntervall = new JSlider(SwingConstants.HORIZONTAL,
													DT_INTERVAL_STEPLENGTH,
													DT_INTERVAL_STEPS * DT_INTERVAL_STEPLENGTH,
													DT_INTERVAL_DEFAULT * DT_INTERVAL_STEPLENGTH);
		m_sliderDummyTrafficIntervall.setMajorTickSpacing(DT_INTERVAL_STEPLENGTH);
		m_sliderDummyTrafficIntervall.setMinorTickSpacing(DT_INTERVAL_STEPLENGTH / 2);
		m_sliderDummyTrafficIntervall.setPaintLabels(true);
		m_sliderDummyTrafficIntervall.setPaintTicks(true);
		m_sliderDummyTrafficIntervall.setSnapToTicks(true);
		panelRoot.add(m_sliderDummyTrafficIntervall, c);

		m_cbDummyTraffic.addItemListener(new ItemListener()
		{
			public void itemStateChanged(ItemEvent e)
			{
				m_sliderDummyTrafficIntervall.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
				Dictionary d = m_sliderDummyTrafficIntervall.getLabelTable();
				for (int i = 1; i <= DT_INTERVAL_STEPS; i++)
				{
					( (JLabel) d.get(new Integer(i*DT_INTERVAL_STEPLENGTH))).setEnabled(e.getStateChange() ==
						ItemEvent.SELECTED);
				}
			}
	});
	}

	//defaults
	public void onResetToDefaultsPressed()
	{
		m_cbDenyNonAnonymousSurfing.setSelected(false);
		m_cbDummyTraffic.setSelected(false);
		m_sliderDummyTrafficIntervall.setEnabled(false);
		m_cbAutoConnect.setSelected(true);
		m_cbAutoReConnect.setSelected(true);
		m_cbAutoChooseCascades.setSelected(true);
		m_cbDoNotRestrictAutoChoose.setSelected(true);
		m_panelRestrictedCascades.setEnabled(false);
	}

	protected void onRootPanelShown()
	{
		//Register help context
		JAPHelp.getInstance().getContextObj().setContext("services");
	}

	private JPanel createRestrictedCacadesPanel()
	{
		GridBagConstraints c = new GridBagConstraints();

		c.gridx = 0;
		c.gridy = 0;
		c.anchor = GridBagConstraints.NORTHWEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		c.weighty = 0;

		final JLabel lblKnownCascades =new JLabel(JAPMessages.getString(MSG_KNOWN_CASCADES));
		//lblKnownCascades.setFont(getFontSetting());
		final JLabel lblAllowedCascades = new JLabel(JAPMessages.getString(MSG_ALLOWED_CASCADES));
		//lblAllowedCascades.setFont(getFontSetting());

		DefaultListModel m_knownCascadesListModel = new DefaultListModel();
		m_knownCascadesList = new JList(m_knownCascadesListModel);
		m_knownCascadesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		final JScrollPane knownCascadesScrollPane = new JScrollPane(m_knownCascadesList);
		//knownCascadesScrollPane.setFont(getFontSetting());
		/* set the preferred size of the scrollpane to a 4x20 textarea */
		knownCascadesScrollPane.setPreferredSize( (new JTextArea(4, 20)).getPreferredSize());

		DefaultListModel m_allowedCascadesListModel = new DefaultListModel();
		final JList allowedCascadesList = new JList(m_allowedCascadesListModel);
		allowedCascadesList.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		final JScrollPane allowedCascadesScrollPane = new JScrollPane(allowedCascadesList);
		//allowedCascadesScrollPane.setFont(getFontSetting());
		/* set the preferred size of the scrollpane to a 4x20 textarea */
			allowedCascadesScrollPane.setPreferredSize( (new JTextArea(4, 20)).getPreferredSize());


		final JButton m_btnAddCascades = new JButton(GUIUtils.loadImageIcon(IMG_ARROW_RIGHT, true));
		final JButton m_btnRemoveCascades = new JButton(GUIUtils.loadImageIcon(IMG_ARROW_LEFT, true));


		JPanel panel = new JPanel(new GridBagLayout())
		{
			public void setEnabled(boolean a_bEnable)
			{
				super.setEnabled(a_bEnable);
				lblKnownCascades.setEnabled(a_bEnable);
				lblAllowedCascades.setEnabled(a_bEnable);
				m_knownCascadesList.setEnabled(a_bEnable);
				allowedCascadesList.setEnabled(a_bEnable);
				m_btnAddCascades.setEnabled(a_bEnable);
				m_btnRemoveCascades.setEnabled(a_bEnable);
				knownCascadesScrollPane.setEnabled(a_bEnable);
				allowedCascadesScrollPane.setEnabled(a_bEnable);
			}
		};


		panel.add(lblKnownCascades, c);
		c.gridx = 2;
		panel.add(lblAllowedCascades, c);
		c.gridx = 0;
		c.gridy++;
		c.gridheight = 2;
		panel.add(knownCascadesScrollPane, c);
		c.gridx++;
		c.weightx = 0;
		c.weighty = 1;
		c.gridheight = 1;
		c.fill = GridBagConstraints.BOTH;
		panel.add(m_btnAddCascades, c);
		c.gridy++;
		panel.add(m_btnRemoveCascades, c);
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weighty = 0;
		c.weightx = 1;
		c.gridheight = 2;
		c.gridx++;
		c.gridy--;
		panel.add(allowedCascadesScrollPane, c);

		return panel;
	}
}
