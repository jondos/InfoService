package jap;

import java.util.Vector;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSlider;
import javax.swing.JTable;
import javax.swing.JTextField;
import javax.swing.border.TitledBorder;
import javax.swing.table.DefaultTableModel;
import anon.tor.ordescription.ORDescription;
import anon.tor.ordescription.ORList;

class JAPConfTor extends AbstractJAPConfModule implements ActionListener
{
	JCheckBox m_cbEnableTor;
	JTextField m_tfDirServerAdr;
	JAPJIntField m_tfDirServerPort;
	JTable m_tableRouters;
	JSlider m_sliderMaxPathLen, m_sliderMinPathLen, m_sliderPathSwitchTime;
	JButton m_bttnFetchRouters;

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
		c.insets = new Insets(0, 0, 5, 5);
		c.anchor = c.NORTHWEST;
		m_cbEnableTor = new JCheckBox(JAPMessages.getString("torEnableTor"));
		m_cbEnableTor.addActionListener(this);
		m_cbEnableTor.setActionCommand("enableTor");
		panelRoot.setLayout(l);
		c.gridwidth = 5;
		panelRoot.add(m_cbEnableTor, c);
		JLabel label = new JLabel(JAPMessages.getString("torDirServer"));
		c.gridy = 1;
		c.gridwidth = 1;
		panelRoot.add(label, c);
		m_tfDirServerAdr = new JTextField(15);
		c.gridx = 1;
		c.fill = c.HORIZONTAL;
		c.weightx = 1;
		panelRoot.add(m_tfDirServerAdr, c);
		label = new JLabel(JAPMessages.getString("torDirServerPort"));
		c.gridx = 2;
		c.fill = c.NONE;
		c.weightx = 0;
		panelRoot.add(label, c);
		m_tfDirServerPort = new JAPJIntField(5);
		c.gridx = 3;
		panelRoot.add(m_tfDirServerPort, c);
		c.gridwidth = 5;
		c.fill = c.BOTH;
		c.weightx = 1;
		c.weighty = 1;
		c.gridx = 0;
		c.gridy = 2;
		GridBagLayout g2 = new GridBagLayout();
		GridBagConstraints c2 = new GridBagConstraints();
		JPanel p = new JPanel(g2);
		p.setBorder(new TitledBorder(JAPMessages.getString("torBorderAvailableRouters")));
		//m_tableRouters.setAutoscrolls(true);
		DefaultTableModel model = new DefaultTableModel();
		model.addColumn(JAPMessages.getString("torRouterName"));
		model.addColumn(JAPMessages.getString("torRouterAdr"));
		model.addColumn(JAPMessages.getString("torRouterPort"));
		model.addColumn(JAPMessages.getString("torRouterSoftwaret"));
		model.setNumRows(3);
		m_tableRouters = new JTable(model);
		m_tableRouters.setPreferredScrollableViewportSize(new Dimension(70, 70));
		m_tableRouters.setColumnSelectionAllowed(false);
		JScrollPane s = new JScrollPane(m_tableRouters);
		s.setAutoscrolls(true);
		c2.fill = c2.BOTH;
		c2.weightx = 1;
		c2.weighty = 1;
		p.add(s, c2);
		m_bttnFetchRouters = new JButton(JAPMessages.getString("torBttnFetchRouters"));
		m_bttnFetchRouters.setActionCommand("fetchRouters");
		m_bttnFetchRouters.addActionListener(this);
		c2.fill = c2.NONE;
		c2.weighty = 0;
		c2.gridy = 1;
		c2.anchor = c.SOUTHEAST;
		p.add(m_bttnFetchRouters, c2);
		panelRoot.add(p, c);
		p = new JPanel(new GridLayout(3, 2));
		p.add(new JLabel(JAPMessages.getString("torPrefMinPathLen")));
		m_sliderMinPathLen = new JSlider();
		m_sliderMinPathLen.setPaintLabels(true);
		m_sliderMinPathLen.setPaintTicks(true);
		m_sliderMinPathLen.setMajorTickSpacing(1);
		m_sliderMinPathLen.setSnapToTicks(true);
		m_sliderMinPathLen.setMinimum(1);
		m_sliderMinPathLen.setMaximum(5);
		p.add(m_sliderMinPathLen);
		p.add(new JLabel(JAPMessages.getString("torPrefMaxPathLen")));
		m_sliderMaxPathLen = new JSlider();
		m_sliderMaxPathLen.setMinimum(1);
		m_sliderMaxPathLen.setMaximum(9);
		m_sliderMaxPathLen.setPaintLabels(true);
		m_sliderMaxPathLen.setPaintTicks(true);
		m_sliderMaxPathLen.setMajorTickSpacing(2);
		m_sliderMaxPathLen.setMinorTickSpacing(1);
		m_sliderMaxPathLen.setSnapToTicks(true);
		p.add(m_sliderMaxPathLen);
		p.add(new JLabel(JAPMessages.getString("torPrefPathSwitchTime")));
		m_sliderPathSwitchTime = new JSlider();
		m_sliderPathSwitchTime.setMinimum(10);
		m_sliderPathSwitchTime.setMaximum(110);
		m_sliderPathSwitchTime.setMajorTickSpacing(20);
		m_sliderPathSwitchTime.setMinorTickSpacing(5);
		m_sliderPathSwitchTime.setPaintLabels(true);
		m_sliderPathSwitchTime.setPaintTicks(true);

		p.add(m_sliderPathSwitchTime);
		p.setBorder(new TitledBorder(JAPMessages.getString("torBorderPreferences")));
		c.gridy = 3;
		c.weighty = 0;
		c.fill = c.HORIZONTAL;
		panelRoot.add(p, c);

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
			fetchRouters();

		}
	}

	protected void onOkPressed()
	{
		JAPController.getController().setTorDirServer(m_tfDirServerAdr.getText(),
			m_tfDirServerPort.getInt());
		JAPController.getController().setTorEnabled(m_cbEnableTor.isSelected());
	}

	protected void onUpdateValues()
	{
		m_cbEnableTor.setSelected(JAPModel.isTorEnabled());
		m_tfDirServerAdr.setText(JAPModel.getTorDirServerHostName());
		m_tfDirServerPort.setInt(JAPModel.getTorDirServerPortNumber());
		updateGuiOutput();
	}

	private void updateGuiOutput()
	{
		boolean bEnabled = m_cbEnableTor.isSelected();
		this.m_bttnFetchRouters.setEnabled(bEnabled);
		this.m_tableRouters.setEnabled(bEnabled);
		this.m_tfDirServerAdr.setEnabled(bEnabled);
		this.m_tfDirServerPort.setEnabled(bEnabled);
		this.m_sliderMaxPathLen.setEnabled(bEnabled);
		this.m_sliderMinPathLen.setEnabled(bEnabled);
		this.m_sliderPathSwitchTime.setEnabled(bEnabled);

	}

	private void fetchRouters()
	{
		ORList ol = new ORList();
		if (!ol.updateList(m_tfDirServerAdr.getText(),
						   m_tfDirServerPort.getInt()))
		{
			JAPConf.showError(JAPMessages.getString("torErrorFetchRouters"));
			return;
		}
		DefaultTableModel m = (DefaultTableModel) m_tableRouters.getModel();
		Vector ors = ol.getList();
		m.setNumRows(ors.size());
		for (int i = 0; i < ors.size(); i++)
		{
			ORDescription ord = (ORDescription) ors.elementAt(i);
			m_tableRouters.setValueAt(ord.getName(), i, 0);
			m_tableRouters.setValueAt(ord.getAddress(), i, 1);
			m_tableRouters.setValueAt(new Integer(ord.getPort()), i, 2);
			m_tableRouters.setValueAt(ord.getSoftware(),i,3);
		}
	}
}
