package jap;

import java.text.DateFormat;
import java.util.Vector;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import anon.tor.ordescription.InfoServiceORListFetcher;
import anon.tor.ordescription.ORDescription;
import anon.tor.ordescription.ORList;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;

class JAPConfTor extends AbstractJAPConfModule implements ActionListener
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
		m_tableRouters.setPreferredScrollableViewportSize(new Dimension(70, 70));
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
		p = new JPanel(new GridLayout(3, 2));
		p.add(new JLabel(JAPMessages.getString("torPrefMinPathLen")));
		m_sliderMinPathLen = new JSlider();
		m_sliderMinPathLen.setPaintLabels(true);
		m_sliderMinPathLen.setPaintTicks(true);
		m_sliderMinPathLen.setMajorTickSpacing(1);
		m_sliderMinPathLen.setSnapToTicks(true);
		m_sliderMinPathLen.setMinimum(JAPConstants.TOR_MIN_ROUTE_LEN);
		m_sliderMinPathLen.setMaximum(JAPConstants.TOR_MAX_ROUTE_LEN);
		p.add(m_sliderMinPathLen);
		p.add(new JLabel(JAPMessages.getString("torPrefMaxPathLen")));
		m_sliderMaxPathLen = new JSlider();
		m_sliderMaxPathLen.setMinimum(JAPConstants.TOR_MIN_ROUTE_LEN);
		m_sliderMaxPathLen.setMaximum(JAPConstants.TOR_MAX_ROUTE_LEN);
		m_sliderMaxPathLen.setPaintLabels(true);
		m_sliderMaxPathLen.setPaintTicks(true);
		m_sliderMaxPathLen.setMajorTickSpacing(1);
		m_sliderMaxPathLen.setMinorTickSpacing(1);
		m_sliderMaxPathLen.setSnapToTicks(true);
		p.add(m_sliderMaxPathLen);
		p.add(new JLabel(JAPMessages.getString("torPrefPathSwitchTime")));
		m_sliderConnectionsPerPath = new JSlider();
		m_sliderConnectionsPerPath.setMinimum(1);
		m_sliderConnectionsPerPath.setMaximum(JAPConstants.TOR_MAX_CONNECTIONS_PER_ROUTE);
		m_sliderConnectionsPerPath.setMajorTickSpacing(150);
		m_sliderConnectionsPerPath.setMinorTickSpacing(50);
		m_sliderConnectionsPerPath.setPaintLabels(true);
		m_sliderConnectionsPerPath.setPaintTicks(true);

		p.add(m_sliderConnectionsPerPath);
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
		JAPController.setTorMaxConnectionsPerRoute(m_sliderConnectionsPerPath.getValue());
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
		m_sliderConnectionsPerPath.setValue(JAPModel.getTorMaxConnectionsPerRoute());
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
