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

class JAPConfAnonGeneral extends AbstractJAPConfModule
{
	private JCheckBox m_cbDummyTraffic, m_cbCertCheckDisabled, m_cbPreCreateRoutes;
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
		//cert tab
		m_cbCertCheckDisabled.setSelected(JAPModel.isCertCheckDisabled());
		m_cbPreCreateRoutes.setSelected(JAPModel.isPreCreateAnonRoutesEnabled());
		m_cbAutoConnect.setSelected(JAPModel.getAutoConnect());
		m_cbAutoReConnect.setSelected(JAPModel.getAutoReConnect());
	}

//ok pressed
	protected boolean onOkPressed()
	{

		//Cert seetings
		m_Controller.setCertCheckDisabled(m_cbCertCheckDisabled.isSelected());
		m_Controller.setPreCreateAnonRoutes(m_cbPreCreateRoutes.isSelected());
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

		m_cbCertCheckDisabled = new JCheckBox(JAPMessages.getString("ngConfAnonGeneralDisableCertCheck"));
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
		m_sliderDummyTrafficIntervall = new JSlider(SwingConstants.HORIZONTAL, 10, 60, 30);
		m_sliderDummyTrafficIntervall.setMajorTickSpacing(10);
		m_sliderDummyTrafficIntervall.setMinorTickSpacing(5);
		m_sliderDummyTrafficIntervall.setPaintLabels(true);
		m_sliderDummyTrafficIntervall.setPaintTicks(true);
		m_sliderDummyTrafficIntervall.setSnapToTicks(true);
		panelRoot.add(m_sliderDummyTrafficIntervall, c);
		c.gridy++;
		c.weighty = 0;
		c.fill = GridBagConstraints.HORIZONTAL;
		panelRoot.add(m_cbCertCheckDisabled, c);
		m_cbPreCreateRoutes = new JCheckBox(JAPMessages.getString("ngConfAnonGeneralPreCreateRoutes"));
		c.gridy++;
		c.weighty = 1;
		panelRoot.add(m_cbPreCreateRoutes, c);
	}

	//defaults
	public void onResetToDefaultsPressed()
	{
		m_cbDummyTraffic.setSelected(false);
		m_cbCertCheckDisabled.setSelected(false);
		m_sliderDummyTrafficIntervall.setEnabled(false);
		m_cbPreCreateRoutes.setSelected(true);
		m_cbAutoConnect.setSelected(false);
		m_cbAutoReConnect.setSelected(false);
	}
}
