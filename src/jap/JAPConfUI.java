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

import java.util.Hashtable;
import java.util.Locale;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import javax.swing.ButtonGroup;
import javax.swing.JCheckBox;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JRadioButton;
import javax.swing.JSlider;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.border.TitledBorder;

import gui.GUIUtils;
import gui.JAPHelp;
import gui.JAPMessages;
import gui.LanguageMapper;
import gui.TitledGridBagPanel;
import gui.dialog.JAPDialog;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

final class JAPConfUI extends AbstractJAPConfModule
{
	private static final String MSG_ON_CLOSING_JAP = JAPConfUI.class.getName() + "_onClosingJAP";
	private static final String MSG_WARNING_ON_CLOSING_JAP = JAPConfUI.class.getName() + "_warningOnClosingJAP";
	private static final String MSG_FONT_SIZE = JAPConfUI.class.getName() + "_fontSize";


	private TitledBorder m_borderLookAndFeel, m_borderView;
	private JComboBox m_comboLanguage, m_comboUI;
	private boolean m_bIgnoreComboLanguageEvents = false, m_bIgnoreComboUIEvents = false;
	private JCheckBox m_cbSaveWindowPositions, m_cbAfterStart;
	private JRadioButton m_rbViewSimplified, m_rbViewNormal, m_rbViewMini, m_rbViewSystray;
	private JCheckBox m_cbWarnOnClose;
	private JSlider m_slidFontSize;

	public JAPConfUI()
	{
		super(null);
	}

	public void recreateRootPanel()
	{
		JPanel panelRoot = getRootPanel();

		/* clear the whole root panel */
		panelRoot.removeAll();
		boolean bSimpleView = (JAPModel.getDefaultView() == JAPConstants.VIEW_SIMPLIFIED);
		GridBagLayout gbl1 = new GridBagLayout();
		GridBagConstraints c1 = new GridBagConstraints();
		panelRoot.setLayout(gbl1);
		c1.insets = new Insets(0, 0, 0, 0);
		c1.gridx = 0;
		c1.gridy = 0;
		c1.anchor = GridBagConstraints.NORTHWEST;
		c1.fill = GridBagConstraints.HORIZONTAL;
		c1.weightx = 1;
		JPanel pLookAndFeel = createLookAndFeelPanel();
		if (!bSimpleView)
		{
			panelRoot.add(pLookAndFeel, c1);
			c1.insets = new Insets(10, 0, 10, 0);
		}

		c1.gridy++;
		panelRoot.add(createViewPanel(), c1);

		c1.insets = new Insets(0, 0, 0, 0);
		c1.gridy++;
		JPanel pStartup = createAfterStartupPanel();
		if (!bSimpleView)
		{
			panelRoot.add(pStartup, c1);
			c1.gridy++;
		}

		panelRoot.add(createAfterShutdownPanel(), c1);

		c1.gridy++;
		c1.anchor = GridBagConstraints.NORTHWEST;
		c1.fill = GridBagConstraints.VERTICAL;
		c1.weighty = 1;
		panelRoot.add(new JPanel(), c1);
	}

	private JPanel createLookAndFeelPanel()
	{
		GridBagLayout gbl = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();

		m_borderLookAndFeel = new TitledBorder(JAPMessages.getString("settingsLookAndFeelBorder"));
		final JPanel p = new JPanel(gbl);
		p.setBorder(m_borderLookAndFeel);
		JLabel l = new JLabel(JAPMessages.getString("settingsLookAndFeel"));
		c.insets = new Insets(10, 10, 10, 10);
		c.anchor = GridBagConstraints.NORTHWEST;
		p.add(l, c);
		c.gridx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		m_comboUI = new JComboBox();
		LookAndFeelInfo[] lf = UIManager.getInstalledLookAndFeels();
		String currentLf = UIManager.getLookAndFeel().getClass().getName();
// add menu items
		for (int lfidx = 0; lfidx < lf.length; lfidx++)
		{
			m_comboUI.addItem(lf[lfidx].getName());
		}
// select the current
		int lfidx;
		for (lfidx = 0; lfidx < lf.length; lfidx++)
		{
			if (lf[lfidx].getClassName().equals(currentLf))
			{
				m_comboUI.setSelectedIndex(lfidx);
				break;
			}
		}
		if (! (lfidx < lf.length))
		{
			m_comboUI.addItem("(unknown)");
			m_comboUI.setSelectedIndex(lfidx);
		}
		m_comboUI.addItemListener(new ItemListener()
		{
			public void itemStateChanged(ItemEvent e)
			{
				if (!m_bIgnoreComboUIEvents && e.getStateChange() == ItemEvent.SELECTED)
				{
					JAPDialog.showMessageDialog(getRootPanel(),
												JAPMessages.getString("confLookAndFeelChanged"));
				}
			}
		});
		p.add(m_comboUI, c);
		l = new JLabel(JAPMessages.getString("settingsLanguage"));
		c.gridx = 0;
		c.gridy = 1;
		c.fill = GridBagConstraints.NONE;
		c.weightx = 0;
		p.add(l, c);
		m_comboLanguage = new JComboBox();
		m_comboLanguage.addItem(new LanguageMapper("en"));
		m_comboLanguage.addItem(new LanguageMapper("de"));
		m_comboLanguage.addItem(new LanguageMapper("fr"));
		m_comboLanguage.addItem(new LanguageMapper("pt"));
		m_comboLanguage.addItemListener(new ItemListener()
		{
			public void itemStateChanged(ItemEvent e)
			{
				if (!m_bIgnoreComboLanguageEvents && e.getStateChange() == ItemEvent.SELECTED)
				{
					try
					{
						JAPDialog.showMessageDialog(getRootPanel(),
												JAPMessages.getString("confLanguageChanged"));
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




		c.gridx = 0;
		c.gridy = 2;
		c.weightx = 0;
		p.add(new JLabel(JAPMessages.getString(MSG_FONT_SIZE)), c);

		m_slidFontSize = new JSlider(
			  JSlider.HORIZONTAL, 0, JAPModel.MAX_FONT_SIZE, JAPModel.getInstance().getFontSize());
		m_slidFontSize.setPaintTicks(false);
		m_slidFontSize.setPaintLabels(true);
		m_slidFontSize.setMajorTickSpacing(1);
		m_slidFontSize.setMinorTickSpacing(1);
		m_slidFontSize.setSnapToTicks(true);
		m_slidFontSize.setPaintTrack(true);
		Hashtable map = new Hashtable(JAPModel.MAX_FONT_SIZE + 1);
		for (int i = 0; i <= JAPModel.MAX_FONT_SIZE; i++)
		{
			map.put(new Integer(i), new JLabel("1" + i + "0%"));
		}
		m_slidFontSize.setLabelTable(map);




		c.gridx++;
		p.add(m_slidFontSize, c);

		m_cbSaveWindowPositions = new JCheckBox(JAPMessages.getString("settingsSaveWindowPosition"));
		c.gridwidth = 2;
		c.weightx = 1;
		c.gridx = 0;
		c.gridy++;
		c.fill = GridBagConstraints.NONE;
		c.weightx = 0;
		p.add(m_cbSaveWindowPositions, c);
		return p;
	}

	private JPanel createViewPanel()
	{
		GridBagLayout gbl = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		m_borderView = new TitledBorder(JAPMessages.getString("ngSettingsViewBorder"));
		JPanel p = new JPanel(gbl);
		p.setBorder(m_borderView);
		m_rbViewNormal = new JRadioButton(JAPMessages.getString("ngSettingsViewNormal"));
		m_rbViewSimplified = new JRadioButton(JAPMessages.getString("ngSettingsViewSimplified"));
		ButtonGroup bg = new ButtonGroup();
		bg.add(m_rbViewNormal);
		bg.add(m_rbViewSimplified);
		ActionListener listener = new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				JAPDialog.showMessageDialog(getRootPanel(),
												JAPMessages.getString("confViewChanged"));
			}
		};
		m_rbViewNormal.addActionListener(listener);
		m_rbViewSimplified.addActionListener(listener);
		c.insets = new Insets(10, 10, 10, 10);
		c.weightx = 1;
		c.anchor = GridBagConstraints.NORTHWEST;
		p.add(m_rbViewNormal, c);
		c.gridy = 1;
		p.add(m_rbViewSimplified, c);
		return p;
	}

	private JPanel createAfterShutdownPanel()
	{
		TitledGridBagPanel panel = new TitledGridBagPanel(JAPMessages.getString(MSG_ON_CLOSING_JAP));
		m_cbWarnOnClose = new JCheckBox(JAPMessages.getString(MSG_WARNING_ON_CLOSING_JAP));
		panel.addRow(m_cbWarnOnClose, null);
		return panel;
	}

	private JPanel createAfterStartupPanel()
	{
		GridBagLayout gbl = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		JPanel p = new JPanel(gbl);
		p.setBorder(new TitledBorder(JAPMessages.getString("ngSettingsStartBorder")));
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
		ButtonGroup bg = new ButtonGroup();
		bg.add(m_rbViewMini);
		bg.add(m_rbViewSystray);
		c.gridy = 1;
		c.insets = new Insets(5, 30, 0, 10);
		p.add(m_rbViewMini, c);
		c.gridy = 2;
		p.add(m_rbViewSystray, c);
		return p;
	}

	public String getTabTitle()
	{
		return JAPMessages.getString("ngUIPanelTitle");
	}

	protected void onCancelPressed()
	{
		m_slidFontSize.setValue(JAPModel.getInstance().getFontSize());
	}

	protected boolean onOkPressed()
	{
		JAPModel.getInstance().setFontSize(m_slidFontSize.getValue());

		LogHolder.log(LogLevel.DEBUG, LogType.GUI,
					  "m_comboLanguage: " + Integer.toString(m_comboLanguage.getSelectedIndex()));
		if (m_comboLanguage.getSelectedIndex() >= 0)
		{
			JAPController.setLocale(((LanguageMapper)m_comboLanguage.getSelectedItem()).getLocale());
		}
		JAPController.setSaveMainWindowPosition(m_cbSaveWindowPositions.isSelected());
		int defaultView = JAPConstants.VIEW_NORMAL;
		if (m_rbViewSimplified.isSelected())
		{
			defaultView = JAPConstants.VIEW_SIMPLIFIED;
		}
		JAPController.getInstance().setDefaultView(defaultView);
		JAPController.getInstance().setMinimizeOnStartup(m_rbViewMini.isSelected() &&
			m_cbAfterStart.isSelected());
		JAPController.getInstance().setMoveToSystrayOnStartup(m_rbViewSystray.isSelected() &&
			m_cbAfterStart.isSelected());
		JAPModel.getInstance().setNeverRemindGoodbye(!m_cbWarnOnClose.isSelected());

		try
		{
			/*
			UIManager.setLookAndFeel(UIManager.getInstalledLookAndFeels()[m_comboUI
									 .getSelectedIndex()].getClassName());*/
			JAPModel.getInstance().setLookAndFeel(
				 UIManager.getInstalledLookAndFeels()[m_comboUI.getSelectedIndex()].getClassName());
		}
		catch (Exception ex)
		{
		}
		return true;
	}

	protected void onUpdateValues()
	{
		updateGuiOutput();
	}

	private void setLanguageComboIndex(Locale a_locale)
	{
		LanguageMapper langMapper = new LanguageMapper(a_locale.getLanguage());
		int i = 0;

		m_bIgnoreComboLanguageEvents = true;
		for (; i < m_comboLanguage.getItemCount(); i++)
		{
			if (m_comboLanguage.getItemAt(i).equals(langMapper))
			{
				m_comboLanguage.setSelectedIndex(i);
				break;
			}
		}
		if (i == m_comboLanguage.getItemCount())
		{
			// the requested language was not found
			m_comboLanguage.setSelectedIndex(0);
		}
		m_bIgnoreComboLanguageEvents = false;
	}

	private void updateGuiOutput()
	{
		setLanguageComboIndex(JAPController.getLocale());
		m_cbSaveWindowPositions.setSelected(JAPModel.getSaveMainWindowPosition());
		m_rbViewNormal.setSelected(JAPModel.getDefaultView() == JAPConstants.VIEW_NORMAL);
		m_rbViewSimplified.setSelected(JAPModel.getDefaultView() == JAPConstants.VIEW_SIMPLIFIED);
		m_rbViewSystray.setSelected(JAPModel.getMoveToSystrayOnStartup());
		m_rbViewMini.setSelected(JAPModel.getMinimizeOnStartup());
		m_cbWarnOnClose.setSelected(!JAPModel.getInstance().isNeverRemindGoodbye());
		boolean b = JAPModel.getMoveToSystrayOnStartup() || JAPModel.getMinimizeOnStartup();
		updateThirdPanel(b);
	}

	public void onResetToDefaultsPressed()
	{
		setLanguageComboIndex(Locale.getDefault());
		LookAndFeelInfo lookandfeels[] = UIManager.getInstalledLookAndFeels();
		for (int i = 0; i < lookandfeels.length; i++)
		{
			if (lookandfeels[i].getClassName().equals(UIManager.getCrossPlatformLookAndFeelClassName()))
			{
				m_bIgnoreComboUIEvents = true;
				m_comboUI.setSelectedIndex(i);
				m_bIgnoreComboUIEvents = false;
				break;
			}
		}
		m_cbSaveWindowPositions.setSelected(JAPConstants.DEFAULT_SAVE_MAIN_WINDOW_POSITION);
		m_rbViewNormal.setSelected(JAPConstants.DEFAULT_VIEW == JAPConstants.VIEW_NORMAL);
		m_rbViewSimplified.setSelected(JAPConstants.DEFAULT_VIEW == JAPConstants.VIEW_SIMPLIFIED);
		m_rbViewSystray.setSelected(JAPConstants.DEFAULT_MOVE_TO_SYSTRAY_ON_STARTUP);
		m_rbViewMini.setSelected(JAPConstants.DEFAULT_MINIMIZE_ON_STARTUP);
		m_cbWarnOnClose.setSelected(JAPConstants.DEFAULT_WARN_ON_CLOSE);
		updateThirdPanel(JAPConstants.DEFAULT_MOVE_TO_SYSTRAY_ON_STARTUP ||
						 JAPConstants.DEFAULT_MINIMIZE_ON_STARTUP);
	}

	private void updateThirdPanel(boolean bAfterStart)
	{
		m_cbAfterStart.setSelected(bAfterStart);
		m_rbViewMini.setEnabled(bAfterStart);
		m_rbViewSystray.setEnabled(bAfterStart);
		if (bAfterStart && ! (m_rbViewSystray.isSelected() || m_rbViewMini.isSelected()))
		{
			m_rbViewMini.setSelected(true);
		}
	}

	protected void onRootPanelShown()
	{
		//Register help context
		JAPHelp.getInstance().getContextObj().setContext("appearance");
	}
}
