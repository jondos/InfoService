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

import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.text.DateFormat;
import java.util.Hashtable;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;

import anon.tor.ordescription.InfoServiceORListFetcher;
import anon.tor.ordescription.ORDescription;
import anon.tor.ordescription.ORList;

final class JAPConfTor extends AbstractJAPConfModule implements ActionListener
{
	JTable m_tableRouters;
	JSlider m_sliderMaxPathLen, m_sliderMinPathLen, m_sliderConnectionsPerPath;
	JButton m_bttnFetchRouters;
	JLabel m_labelAvailableRouters;
	long m_lastUpdate;
	public JAPConfTor()
	{
		super(null);
	}

	public void recreateRootPanel()
	{
		JPanel panelRoot = getRootPanel();

		/* clear the whole root panel */
		panelRoot.removeAll();
		GridBagLayout l = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		c.insets = new Insets(5, 5, 5, 5);
		c.anchor = GridBagConstraints.NORTHWEST;
		panelRoot.setLayout(l);
		c.gridwidth = 5;
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 1;
		c.weighty = 1;
		c.gridx = 0;
		c.gridy = 2;
		GridBagLayout g2 = new GridBagLayout();
		GridBagConstraints c2 = new GridBagConstraints();
		JPanel p = new JPanel(g2);
		m_labelAvailableRouters = new JLabel(JAPMessages.getString("torBorderAvailableRouters") + ":");
		c2.fill = GridBagConstraints.HORIZONTAL;
		c2.weightx = 1;
		c2.weighty = 0;
		p.add(m_labelAvailableRouters, c2);

		DefaultTableModel model = new DefaultTableModel();
		model.addColumn(JAPMessages.getString("torRouterName"));
		model.addColumn(JAPMessages.getString("torRouterAdr"));
		model.addColumn(JAPMessages.getString("torRouterPort"));
		model.addColumn(JAPMessages.getString("torRouterSoftware"));
		model.setNumRows(3);
		m_tableRouters = new JTable(model)
		{
			public boolean isCellEditable(int i, int j)
			{
				return false;
			}
		};
	m_tableRouters.setPreferredScrollableViewportSize(new Dimension(70, m_tableRouters.getRowHeight()*5));
		m_tableRouters.setCellSelectionEnabled(false);
		m_tableRouters.setColumnSelectionAllowed(false);
		m_tableRouters.setRowSelectionAllowed(true);
		m_tableRouters.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		JScrollPane s = new JScrollPane(m_tableRouters);
		s.setAutoscrolls(true);
		c2.fill = GridBagConstraints.BOTH;
		c2.gridy = 1;
		c2.weightx = 1;
		c2.weighty = 1;
		c2.gridwidth = 2;
		p.add(s, c2);
		m_bttnFetchRouters = new JButton(JAPMessages.getString("torBttnFetchRouters"));
		m_bttnFetchRouters.setIcon(JAPUtil.loadImageIcon(JAPConstants.IMAGE_RELOAD, true));
		m_bttnFetchRouters.setDisabledIcon(JAPUtil.loadImageIcon(JAPConstants.IMAGE_RELOAD_DISABLED, true));
		m_bttnFetchRouters.setPressedIcon(JAPUtil.loadImageIcon(JAPConstants.IMAGE_RELOAD_ROLLOVER, true));

		m_bttnFetchRouters.setActionCommand("fetchRouters");
		m_bttnFetchRouters.addActionListener(this);
		c2.fill = GridBagConstraints.NONE;
		c2.weighty = 0;
		c2.gridy = 0;
		c2.gridwidth = 1;
		c2.gridx = 1;
		c2.anchor = GridBagConstraints.EAST;
		c2.insets = new Insets(5, 5, 5, 0);
		p.add(m_bttnFetchRouters, c2);
		panelRoot.add(p, c);

		p = new JPanel(new GridBagLayout());
		GridBagConstraints c3=new GridBagConstraints();
		c3.anchor=GridBagConstraints.NORTHWEST;
		c3.insets=new Insets(2,5,2,5);
		c3.fill=GridBagConstraints.NONE;
		p.add(new JLabel(JAPMessages.getString("torPrefMinPathLen")),c3);
		m_sliderMinPathLen = new JSlider();
		m_sliderMinPathLen.setPaintLabels(true);
		m_sliderMinPathLen.setPaintTicks(true);
		m_sliderMinPathLen.setMajorTickSpacing(1);
		m_sliderMinPathLen.setSnapToTicks(true);
		m_sliderMinPathLen.setMinimum(JAPConstants.TOR_MIN_ROUTE_LEN);
		m_sliderMinPathLen.setMaximum(JAPConstants.TOR_MAX_ROUTE_LEN);
		c3.gridx=1;
		c3.fill=GridBagConstraints.HORIZONTAL;
		p.add(m_sliderMinPathLen,c3);
		c3.gridx=0;
		c3.gridy=1;
		c3.fill=GridBagConstraints.NONE;
		p.add(new JLabel(JAPMessages.getString("torPrefMaxPathLen")),c3);
		m_sliderMaxPathLen = new JSlider();
		m_sliderMaxPathLen.setMinimum(JAPConstants.TOR_MIN_ROUTE_LEN);
		m_sliderMaxPathLen.setMaximum(JAPConstants.TOR_MAX_ROUTE_LEN);
		m_sliderMaxPathLen.setPaintLabels(true);
		m_sliderMaxPathLen.setPaintTicks(true);
		m_sliderMaxPathLen.setMajorTickSpacing(1);
		m_sliderMaxPathLen.setMinorTickSpacing(1);
		m_sliderMaxPathLen.setSnapToTicks(true);
		c3.gridx=1;
		c3.fill=GridBagConstraints.HORIZONTAL;
		p.add(m_sliderMaxPathLen,c3);
		c3.gridx=0;
		c3.gridy=2;
		c3.fill=GridBagConstraints.NONE;
		p.add(new JLabel(JAPMessages.getString("torPrefPathSwitchTime")),c3);
		m_sliderConnectionsPerPath = new JSlider();
		Hashtable sliderLabels = new Hashtable();
		sliderLabels.put(new Integer(1), new JLabel("10"));
		sliderLabels.put(new Integer(2), new JLabel("50"));
		sliderLabels.put(new Integer(3), new JLabel("100"));
		sliderLabels.put(new Integer(4), new JLabel("500"));
		sliderLabels.put(new Integer(5), new JLabel("1000"));
		m_sliderConnectionsPerPath.setLabelTable(sliderLabels);
		m_sliderConnectionsPerPath.setMinimum(1);
		m_sliderConnectionsPerPath.setMaximum(5);
		m_sliderConnectionsPerPath.setMajorTickSpacing(1);
		m_sliderConnectionsPerPath.setMinorTickSpacing(1);
		m_sliderConnectionsPerPath.setSnapToTicks(true);
		m_sliderConnectionsPerPath.setPaintLabels(true);
		m_sliderConnectionsPerPath.setPaintTicks(true);
		c3.gridx=1;
		c3.weightx=1;
		c3.fill=GridBagConstraints.HORIZONTAL;
		p.add(m_sliderConnectionsPerPath,c3);
		p.setBorder(new TitledBorder(JAPMessages.getString("torBorderPreferences")));
		c.gridy = 3;
		c.weighty = 0;
		c.fill = GridBagConstraints.HORIZONTAL;
		panelRoot.add(p, c);
		m_lastUpdate = 0;
	}

	public String getTabTitle()
	{
		return "Tor";
	}

	/**
	 * actionPerformed
	 *
	 * @param actionEvent ActionEvent
	 */
	public void actionPerformed(ActionEvent actionEvent)
	{
		if (actionEvent.getActionCommand().equals("enableTor"))
		{
			updateGuiOutput();
		}
		else if (actionEvent.getActionCommand().equals("fetchRouters"))
		{
			fetchRoutersAsync(true);

		}
	}

	protected boolean onOkPressed()
	{
		int i = m_sliderConnectionsPerPath.getValue();
		int[] ar =
			{
			10, 50, 100, 500, 1000};
		JAPController.setTorMaxConnectionsPerRoute(ar[i - 1]);
		JAPController.setTorRouteLen(m_sliderMinPathLen.getValue(), m_sliderMaxPathLen.getValue());
		return true;
	}

	protected void onUpdateValues()
	{
		updateGuiOutput();
	}

	protected void onRootPanelShown()
	{
//		if (System.currentTimeMillis() - m_lastUpdate > 600000)
//		{
//			fetchRouters(false);
//		}
	}

	private void updateGuiOutput()
	{
		int i = JAPModel.getTorMaxConnectionsPerRoute();
		if (i < 25)
		{
			i = 1;
		}
		else if (i < 75)
		{
			i = 2;
		}
		else if (i < 250)
		{
			i = 3;
		}
		else if (i < 750)
		{
			i = 4;
		}
		else
		{
			i = 5;
		}
		m_sliderConnectionsPerPath.setValue(i);
		m_sliderMaxPathLen.setValue(JAPModel.getTorMaxRouteLen());
		m_sliderMinPathLen.setValue(JAPModel.getTorMinRouteLen());
	}

	private void fetchRoutersAsync(final boolean bShowError)
	{
		m_bttnFetchRouters.setEnabled(false);
		Runnable doIt = new Runnable()
		{
			public void run()
			{
				ORList ol = new ORList(new InfoServiceORListFetcher());
				if (!ol.updateList())
				{
					if (bShowError)
					{
						JAPConf.showError(JAPMessages.getString("torErrorFetchRouters"));
					}
					m_bttnFetchRouters.setEnabled(true);
					return;
				}
				m_lastUpdate = System.currentTimeMillis();
				DefaultTableModel m = (DefaultTableModel) m_tableRouters.getModel();
				Vector ors = ol.getList();
				m.setNumRows(ors.size());
				for (int i = 0; i < ors.size(); i++)
				{
					ORDescription ord = (ORDescription) ors.elementAt(i);
					m_tableRouters.setValueAt(ord.getName(), i, 0);
					m_tableRouters.setValueAt(ord.getAddress(), i, 1);
					m_tableRouters.setValueAt(new Integer(ord.getPort()), i, 2);
					m_tableRouters.setValueAt(ord.getSoftware(), i, 3);
				}
				m_labelAvailableRouters.setText(JAPMessages.getString("torBorderAvailableRouters") + " (" +
												DateFormat.getDateTimeInstance(DateFormat.MEDIUM,
					DateFormat.SHORT).
												format(ol.getPublished()) + "):");
				getRootPanel().updateUI();
				m_bttnFetchRouters.setEnabled(true);
			}
		};
		Thread t = new Thread(doIt);
		t.start();
	}
}
