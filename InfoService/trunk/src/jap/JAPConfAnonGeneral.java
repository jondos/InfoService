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

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.JCheckBox;
import javax.swing.JPanel;
import javax.swing.JSlider;
import javax.swing.SwingConstants;

import anon.crypto.SignatureVerifier;

final class JAPConfAnonGeneral extends AbstractJAPConfModule
{
	private JCheckBox m_cbDummyTraffic;
	private JCheckBox m_cbAutoConnect;
	private JCheckBox m_cbAutoReConnect;
	private JSlider m_sliderDummyTrafficIntervall;
	private JAPController m_Controller;

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
		m_cbAutoConnect.setSelected(JAPModel.getAutoConnect());
		m_cbAutoReConnect.setSelected(JAPModel.getAutoReConnect());
	}

//ok pressed
	protected boolean onOkPressed()
	{

		if (m_cbDummyTraffic.isSelected())
		{
			m_Controller.setDummyTraffic(m_sliderDummyTrafficIntervall.getValue() * 1000);
		}
		else
		{
			m_Controller.setDummyTraffic( -1);
			// Listener settings
		}
		// Anonservice settings
		m_Controller.setAutoConnect(m_cbAutoConnect.isSelected());
		m_Controller.setAutoReConnect(m_cbAutoReConnect.isSelected());
		return true;
	}

	public void recreateRootPanel()
	{
		JPanel panelRoot = getRootPanel();
		panelRoot.removeAll();
		Font font = getFontSetting();
		m_cbAutoConnect = new JCheckBox(JAPMessages.getString("settingsautoConnectCheckBox"));
		m_cbAutoConnect.setFont(font);
		m_cbAutoReConnect = new JCheckBox(JAPMessages.getString("settingsautoReConnectCheckBox"));
		m_cbAutoReConnect.setFont(font);
		m_cbDummyTraffic = new JCheckBox(JAPMessages.getString("ngConfAnonGeneralSendDummy"));
		m_cbDummyTraffic.addItemListener(new ItemListener()
		{
			public void itemStateChanged(ItemEvent e)
			{
				m_sliderDummyTrafficIntervall.setEnabled(e.getStateChange() == ItemEvent.SELECTED);
			}
		});

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
		panelRoot.add(m_cbAutoConnect, c);
		c.gridy++;
		panelRoot.add(m_cbAutoReConnect, c);
		c.gridy++;
		panelRoot.add(m_cbDummyTraffic, c);
		c.gridy++;
		c.weighty = 1.0;
		m_sliderDummyTrafficIntervall = new JSlider(SwingConstants.HORIZONTAL, 10, 60, 30);
		m_sliderDummyTrafficIntervall.setMajorTickSpacing(10);
		m_sliderDummyTrafficIntervall.setMinorTickSpacing(5);
		m_sliderDummyTrafficIntervall.setPaintLabels(true);
		m_sliderDummyTrafficIntervall.setPaintTicks(true);
		m_sliderDummyTrafficIntervall.setSnapToTicks(true);
		panelRoot.add(m_sliderDummyTrafficIntervall, c);
	}

	//defaults
	public void onResetToDefaultsPressed()
	{
		m_cbDummyTraffic.setSelected(false);
		m_sliderDummyTrafficIntervall.setEnabled(false);
		m_cbAutoConnect.setSelected(false);
		m_cbAutoReConnect.setSelected(false);
	}

	protected void onRootPanelShown()
	{
		//Register help context
		JAPHelp.getInstance().getContextObj().setContext("services");
	}
}
