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
import javax.swing.*;
import javax.swing.border.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.plaf.basic.*;
import java.util.*;
import logging.*;
import javax.swing.UIManager.*;

final class JAPConfUI extends AbstractJAPConfModule
{
	private TitledBorder m_borderLookAndFeel, m_borderView;
	private JComboBox m_comboLanguage;
	private boolean m_bIgnoreComboLanguageEvents = false;
	private JCheckBox m_cbSaveWindowPositions, m_cbAfterStart;
	private JRadioButton m_rbViewSimplified, m_rbViewNormal, m_rbViewMini, m_rbViewSystray;

	public JAPConfUI()
	{
		super(null);
	}

	public void recreateRootPanel()
	{
		JPanel panelRoot = getRootPanel();

		/* clear the whole root panel */
		panelRoot.removeAll();
		GridBagLayout gbl1 = new GridBagLayout();
		GridBagConstraints c1 = new GridBagConstraints();
		panelRoot.setLayout(gbl1);
		GridBagLayout gbl = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		m_borderLookAndFeel = new TitledBorder(JAPMessages.getString("settingsLookAndFeelBorder"));
		JPanel p = new JPanel(gbl);
		p.setBorder(m_borderLookAndFeel);
		JLabel l = new JLabel(JAPMessages.getString("settingsLookAndFeel"));
		c.insets = new Insets(10, 10, 10, 10);
		c.anchor = GridBagConstraints.WEST;
		p.add(l, c);
		c.gridx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		JComboBox combo = new JComboBox();
		LookAndFeelInfo[] lf = UIManager.getInstalledLookAndFeels();
		String currentLf = UIManager.getLookAndFeel().getClass().getName();
		// add menu items
		for (int lfidx = 0; lfidx < lf.length; lfidx++)
		{
			combo.addItem(lf[lfidx].getName());
		}
		// select the current
		int lfidx;
		for (lfidx = 0; lfidx < lf.length; lfidx++)
		{
			if (lf[lfidx].getClassName().equals(currentLf))
			{
				combo.setSelectedIndex(lfidx);
				break;
			}
		}
		if (! (lfidx < lf.length))
		{
			combo.addItem("(unknown)");
			combo.setSelectedIndex(lfidx);
		}
		combo.addItemListener(new ItemListener()
		{
			public void itemStateChanged(ItemEvent e)
			{
				if (e.getStateChange() == e.SELECTED)
				{
					try
					{
						UIManager.setLookAndFeel(UIManager.getInstalledLookAndFeels()[ ( (JComboBox) e.
							getItemSelectable()).getSelectedIndex()].getClassName());
//									SwingUtilities.updateComponentTreeUI(m_frmParent);
//									SwingUtilities.updateComponentTreeUI(SwingUtilities.getRoot(((JComboBox)e.getItemSelectable())));

						JAPConf.showInfo(
							JAPMessages.getString("confLookAndFeelChanged"));
					}
					catch (Exception ie)
					{
					}
				}
			}
		});
		p.add(combo, c);
		l = new JLabel(JAPMessages.getString("settingsLanguage"));
		c.gridx = 0;
		c.gridy = 1;
		c.fill = GridBagConstraints.NONE;
		c.weightx = 0;
		p.add(l, c);
		m_comboLanguage = new JComboBox();
		m_comboLanguage.addItem("Deutsch");
		m_comboLanguage.addItem("English");
		m_comboLanguage.addItem("Fran\u00E7ais");
		m_comboLanguage.addItemListener(new ItemListener()
		{
			public void itemStateChanged(ItemEvent e)
			{
				if (!m_bIgnoreComboLanguageEvents && e.getStateChange() == e.SELECTED)
				{
					try
					{
						JAPConf.showInfo(JAPMessages.getString("confLanguageChanged"));
					}
					catch (Exception ie)
					{
					}
				}
			}
		});

		c.gridx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		p.add(m_comboLanguage, c);
		m_cbSaveWindowPositions = new JCheckBox(JAPMessages.getString("settingsSaveWindowPosition"));
		c.gridwidth = 2;
		c.gridx = 0;
		c.gridy = 2;
		c.fill = GridBagConstraints.NONE;
		c.weightx = 0;
		p.add(m_cbSaveWindowPositions, c);
		c1.anchor = GridBagConstraints.WEST;
		c1.fill = GridBagConstraints.HORIZONTAL;
		c1.weightx = 1;
		panelRoot.add(p, c1);

		gbl = new GridBagLayout();
		c = new GridBagConstraints();
		m_borderView = new TitledBorder(JAPMessages.getString("ngSettingsViewBorder"));
		p = new JPanel(gbl);
		p.setBorder(m_borderView);
		m_rbViewNormal = new JRadioButton(JAPMessages.getString("ngSettingsViewNormal"));
		m_rbViewSimplified = new JRadioButton(JAPMessages.getString("ngSettingsViewSimplified"));
		ButtonGroup bg = new ButtonGroup();
		bg.add(m_rbViewNormal);
		bg.add(m_rbViewSimplified);
		c.insets = new Insets(10, 10, 10, 10);
		c.weightx = 1;
		c.anchor = GridBagConstraints.WEST;
		p.add(m_rbViewNormal, c);
		c.gridy = 1;
		p.add(m_rbViewSimplified, c);
		c1.gridy = 1;
		c1.insets = new Insets(10, 0, 10, 0);
		panelRoot.add(p, c1);

		gbl = new GridBagLayout();
		c = new GridBagConstraints();
		p = new JPanel(gbl);
		p.setBorder(new TitledBorder(""));
		m_cbAfterStart = new JCheckBox(JAPMessages.getString("ngViewAfterStart"));
		m_cbAfterStart.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				boolean b = m_cbAfterStart.isSelected();
				updateThirdPanel(b);
			}
		});
		c.insets = new Insets(10, 10, 0, 10);
		c.anchor = GridBagConstraints.NORTHWEST;
		c.weightx = 1;
		p.add(m_cbAfterStart, c);
		m_rbViewMini = new JRadioButton(JAPMessages.getString("ngViewMini"));
		m_rbViewSystray = new JRadioButton(JAPMessages.getString("ngViewSystray"));
		bg = new ButtonGroup();
		bg.add(m_rbViewMini);
		bg.add(m_rbViewSystray);
		c.gridy = 1;
		c.insets = new Insets(5, 30, 0, 10);
		p.add(m_rbViewMini, c);
		c.gridy = 2;
		p.add(m_rbViewSystray, c);

		c1.gridy = 2;
		c1.insets = new Insets(0, 0, 0, 0);
		panelRoot.add(p, c1);

	}

	public String getTabTitle()
	{
		return JAPMessages.getString("ngUIPanelTitle");
	}

	protected boolean onOkPressed()
	{
		LogHolder.log(LogLevel.DEBUG, LogType.GUI,
					  "m_comboLanguage: " + Integer.toString(m_comboLanguage.getSelectedIndex()));
		if (m_comboLanguage.getSelectedIndex() == 0)
		{
			JAPController.setLocale(Locale.GERMAN);
		}
		else if (m_comboLanguage.getSelectedIndex() == 1)
		{
			JAPController.setLocale(Locale.ENGLISH);
		}
		else
		{
			JAPController.setLocale(Locale.FRENCH);
		}
		JAPController.setSaveMainWindowPosition(m_cbSaveWindowPositions.isSelected());
		int defaultView=JAPConstants.VIEW_NORMAL;
		if(m_rbViewSimplified.isSelected())
			defaultView=JAPConstants.VIEW_SIMPLIFIED;
		JAPController.getInstance().setDefaultView(defaultView);
		JAPController.getInstance().setMinimizeOnStartup(m_rbViewMini.isSelected()&&m_cbAfterStart.isSelected());
		JAPController.getInstance().setMoveToSystrayOnStartup(m_rbViewSystray.isSelected()&&m_cbAfterStart.isSelected());
		return true;
	}

	protected void onUpdateValues()
	{
		updateGuiOutput();
	}

	private void updateGuiOutput()
	{
		m_bIgnoreComboLanguageEvents = true;
		if (JAPController.getLocale().equals(Locale.ENGLISH))
		{
			m_comboLanguage.setSelectedIndex(1);
		}
		else if (JAPController.getLocale().equals(Locale.FRENCH))
		{
			m_comboLanguage.setSelectedIndex(2);
		}
		else
		{
			m_comboLanguage.setSelectedIndex(0);
		}
		m_bIgnoreComboLanguageEvents = false;
		m_cbSaveWindowPositions.setSelected(JAPModel.getSaveMainWindowPosition());
		m_rbViewNormal.setSelected(JAPModel.getDefaultView() == JAPConstants.VIEW_NORMAL);
		m_rbViewSimplified.setSelected(JAPModel.getDefaultView() == JAPConstants.VIEW_SIMPLIFIED);
		m_rbViewSystray.setSelected(JAPModel.getMoveToSystrayOnStartup());
		m_rbViewMini.setSelected(JAPModel.getMinimizeOnStartup());
		boolean b = m_rbViewSystray.isSelected() || m_rbViewMini.isSelected();
		updateThirdPanel(b);
	}

	private void updateThirdPanel(boolean bAfterStart)
	{
		m_cbAfterStart.setSelected(bAfterStart);
		m_rbViewMini.setEnabled(bAfterStart);
		m_rbViewSystray.setEnabled(bAfterStart);
		if(bAfterStart&&!(m_rbViewSystray.isSelected()||m_rbViewMini.isSelected()))
			m_rbViewMini.setSelected(true);
	}
}
