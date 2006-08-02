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
import java.util.Vector;
import java.io.File;

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
import javax.swing.JFileChooser;
import javax.swing.filechooser.FileFilter;
import javax.swing.JButton;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.border.TitledBorder;

import gui.GUIUtils;
import gui.JAPHelp;
import gui.JAPMessages;
import gui.LanguageMapper;
import gui.TitledGridBagPanel;
import gui.dialog.JAPDialog;
import gui.dialog.*;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import anon.util.ClassUtil;
import java.io.IOException;
import javax.swing.LookAndFeel;

final class JAPConfUI extends AbstractJAPConfModule
{
	private static final String MSG_ON_CLOSING_JAP = JAPConfUI.class.getName() + "_onClosingJAP";
	private static final String MSG_WARNING_ON_CLOSING_JAP = JAPConfUI.class.getName() + "_warningOnClosingJAP";
	private static final String MSG_FONT_SIZE = JAPConfUI.class.getName() + "_fontSize";
	private static final String MSG_WARNING_IMPORT_LNF = JAPConfUI.class.getName() + "_warningImportLNF";
	private static final String MSG_INCOMPATIBLE_JAVA = JAPConfUI.class.getName() + "_incompatibleJava";
	private static final String MSG_REMOVE = JAPConfUI.class.getName() + "_remove";
	private static final String MSG_IMPORT = JAPConfUI.class.getName() + "_import";
	private static final String MSG_COULD_NOT_REMOVE = JAPConfUI.class.getName() + "_couldNotRemove";
	private static final String MSG_TITLE_IMPORT = JAPConfUI.class.getName() + "_titleImport";
	private static final String MSG_PROGRESS_IMPORTING = JAPConfUI.class.getName() + "_progressImport";
	private static final String MSG_NEED_RESTART = JAPConfUI.class.getName() + "_needRestart";
	private static final String MSG_IMPORT_SUCCESSFUL = JAPConfUI.class.getName() + "_importSuccessful";
	private static final String MSG_NO_LNF_FOUND = JAPConfUI.class.getName() + "_noLNFFound";






	private TitledBorder m_borderLookAndFeel, m_borderView;
	private JComboBox m_comboLanguage, m_comboUI;
	private JCheckBox m_cbSaveWindowPositions, m_cbAfterStart;
	private JRadioButton m_rbViewSimplified, m_rbViewNormal, m_rbViewMini, m_rbViewSystray;
	private JCheckBox m_cbWarnOnClose;
	private JSlider m_slidFontSize;
	private JButton m_btnAddUI, m_btnDeleteUI;
	private File m_currentDirectory;

	public JAPConfUI()
	{
		super(null);
	}

	public void recreateRootPanel()
	{
		JPanel panelRoot = getRootPanel();

		/* clear the whole root panel */
		panelRoot.removeAll();
		//boolean bSimpleView = (JAPModel.getDefaultView() == JAPConstants.VIEW_SIMPLIFIED);
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
		//if (!bSimpleView)
		{
			panelRoot.add(pLookAndFeel, c1);
			c1.insets = new Insets(10, 0, 10, 0);
		}

		c1.gridy++;
		panelRoot.add(createViewPanel(), c1);

		c1.insets = new Insets(0, 0, 0, 0);
		c1.gridy++;
		JPanel pStartup = createAfterStartupPanel();
		//if (!bSimpleView)
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
		p.add(m_comboUI, c);

		m_btnDeleteUI = new JButton(JAPMessages.getString(MSG_REMOVE));
		c.gridx++;
		c.weightx = 0;
		p.add(m_btnDeleteUI, c);
		m_btnDeleteUI.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent a_event)
			{
				try
				{
					synchronized (m_comboUI)
					{
						LookAndFeelInfo[] oldLnFs = UIManager.getInstalledLookAndFeels();
						LookAndFeelInfo[] alteredLnFs = new LookAndFeelInfo[oldLnFs.length - 1];
						Class lnf = Class.forName( (oldLnFs[m_comboUI.getSelectedIndex()].getClassName()));
						for (int i = 0, j = 0; i < oldLnFs.length; i++)
						{
							if (i == m_comboUI.getSelectedIndex())
							{
								continue;
							}
							alteredLnFs[j] = oldLnFs[i];
							j++;
						}
						UIManager.setInstalledLookAndFeels(alteredLnFs);
						JAPModel.getInstance().removeLookAndFeelFile(ClassUtil.getClassDirectory(lnf));
						updateUICombo();
					}
				}
				catch (Exception a_e)
				{
					JAPDialog.showErrorDialog(
									   getRootPanel(),
						JAPMessages.getString(MSG_COULD_NOT_REMOVE), LogType.MISC, a_e);
				}
			}
		});


		m_btnAddUI = new JButton(JAPMessages.getString(MSG_IMPORT));

		c.gridx++;
		//c.gridwidth = 1;
		p.add(m_btnAddUI, c);
		m_btnAddUI.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent a_event)
			{
				final JFileChooser fileChooser = new JFileChooser(m_currentDirectory);
				final JAPDialog dialog = new JAPDialog(getRootPanel(), JAPMessages.getString(MSG_TITLE_IMPORT));
				final DialogContentPane pane =
					new SimpleWizardContentPane(dialog,
												"<font color='red'>" +
												JAPMessages.getString(MSG_WARNING_IMPORT_LNF) + "</font>",
												new DialogContentPane.Layout(
					JAPMessages.getString(JAPDialog.MSG_TITLE_WARNING),
					DialogContentPane.MESSAGE_TYPE_WARNING),
												null)
				{
					boolean m_bCanceled = false;

					public CheckError[] checkYesOK()
					{
						m_bCanceled = false;
						CheckError[] errors = super.checkYesOK();
						fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
						FileFilter filter = new FileFilter()
						{
							public boolean accept(File a_file)
							{
								return a_file.isDirectory() || a_file.getName().endsWith(".jar");
							}

							public String getDescription()
							{
								return "*.jar";
							}
						};
						fileChooser.setFileFilter(filter);
						if (fileChooser.showOpenDialog(dialog.getContentPane()) != JFileChooser.APPROVE_OPTION)
						{
							m_bCanceled = true;
						}
						return errors;
					}
					public Object getValue()
					{
						return new Boolean(m_bCanceled);
					}
				};
				final WorkerContentPane.IReturnRunnable doIt = new WorkerContentPane.IReturnRunnable()
				{
					Object m_value;

					public Object getValue()
					{
						return m_value;
					}

					public void run()
					{
						if (fileChooser.getSelectedFile() != null)
						{
							m_currentDirectory = fileChooser.getCurrentDirectory();
							try
							{
								if (GUIUtils.registerLookAndFeelClasses(fileChooser.getSelectedFile()))
								{
									LogHolder.log(LogLevel.NOTICE, LogType.GUI,
												  "Added new L&F class file: " + fileChooser.getSelectedFile());
									JAPModel.getInstance().addLookAndFeelFile(fileChooser.getSelectedFile());
									updateUICombo();
									m_value = JAPMessages.getString(MSG_IMPORT_SUCCESSFUL);
								}
								else
								{
									m_value = new Exception(JAPMessages.getString(MSG_NO_LNF_FOUND));
								}
							}
							catch (IllegalAccessException a_e)
							{
								m_value = new Exception(JAPMessages.getString(MSG_INCOMPATIBLE_JAVA));
							}
							fileChooser.setSelectedFile(null);
						}
					}
				};

				DialogContentPane importPane = new WorkerContentPane(dialog,
					JAPMessages.getString(MSG_PROGRESS_IMPORTING) + "...", pane, doIt)
				{
					public boolean isSkippedAsNextContentPane()
					{
						return ((Boolean)pane.getValue()).booleanValue();
					}
				};

				DialogContentPane goodResultPane = new SimpleWizardContentPane(dialog, "OK",
					new DialogContentPane.Layout(
									   JAPMessages.getString(JAPDialog.MSG_TITLE_INFO),
									   DialogContentPane.MESSAGE_TYPE_INFORMATION),
					new DialogContentPane.Options(importPane))
				{
					public CheckError[] checkUpdate()
					{
						setText((String)doIt.getValue());
						return null;
					}



					public boolean isSkippedAsNextContentPane()
					{
						return ((Boolean)pane.getValue()).booleanValue() ||
							doIt.getValue() instanceof Exception;
					}
					public boolean isSkippedAsPreviousContentPane()
					{
						return true;
					}

				};
				goodResultPane.getButtonCancel().setVisible(false);

				DialogContentPane errorPane = new SimpleWizardContentPane(dialog, "ERROR",
					new DialogContentPane.Layout(
									   JAPMessages.getString(JAPDialog.MSG_TITLE_ERROR),
									   DialogContentPane.MESSAGE_TYPE_ERROR),
					new DialogContentPane.Options(goodResultPane))
				{
					public boolean isSkippedAsPreviousContentPane()
					{
						return true;
					}
					public CheckError[] checkUpdate()
					{
						setText(((Exception)doIt.getValue()).getMessage());
						return null;
					}

					public boolean isSkippedAsNextContentPane()
					{
						return ((Boolean)pane.getValue()).booleanValue() ||
							!(doIt.getValue() instanceof Exception);
					}
				};
				errorPane.getButtonCancel().setVisible(false);



				DialogContentPane.updateDialogOptimalSized(pane);
				dialog.setVisible(true);

			}
		});


		m_comboUI.addItemListener(new ItemListener()
		{
			public void itemStateChanged(ItemEvent a_event)
			{
				synchronized (m_comboUI)
				{
					if (m_comboUI.getSelectedIndex() >= 0)
					{
						String lnfClass = UIManager.getInstalledLookAndFeels()[
							m_comboUI.getSelectedIndex()].getClassName();
						if (JAPModel.getInstance().getLookAndFeel().equals(lnfClass) ||
							JAPModel.getInstance().isSystemLookAndFeel(lnfClass))
						{
							m_btnDeleteUI.setEnabled(false);
						}
						else
						{
							m_btnDeleteUI.setEnabled(true);
						}
					}
				}
			}
		});



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

		c.gridx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		c.gridwidth = 3;
		p.add(m_comboLanguage, c);


		c.gridx = 0;
		c.gridy = 2;
		c.weightx = 0;
		c.gridwidth = 1;
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
		c.gridwidth = 3;
		c.gridx++;
		p.add(m_slidFontSize, c);

		m_cbSaveWindowPositions = new JCheckBox(JAPMessages.getString("settingsSaveWindowPosition"));
		c.gridwidth = 4;
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
		onUpdateValues();
	}

	protected boolean onOkPressed()
	{
		boolean bNeedRestart = false;
	/*
	 JAPDialog.showMessageDialog(getRootPanel(),
												JAPMessages.getString("confViewChanged"));

	 JAPDialog.showMessageDialog(getRootPanel(),
												JAPMessages.getString("confLanguageChanged"));

	 JAPDialog.showMessageDialog(getRootPanel(),
												JAPMessages.getString("confLookAndFeelChanged"));
		*/

		JAPModel.getInstance().setFontSize(m_slidFontSize.getValue());

		JAPController.setSaveMainWindowPosition(m_cbSaveWindowPositions.isSelected());

		JAPController.getInstance().setMinimizeOnStartup(m_rbViewMini.isSelected() &&
			m_cbAfterStart.isSelected());
		JAPController.getInstance().setMoveToSystrayOnStartup(m_rbViewSystray.isSelected() &&
			m_cbAfterStart.isSelected());
		JAPModel.getInstance().setNeverRemindGoodbye(!m_cbWarnOnClose.isSelected());

		Locale newLocale;
		if (m_comboLanguage.getSelectedIndex() >= 0)
		{
			newLocale = ( (LanguageMapper) m_comboLanguage.getSelectedItem()).getLocale();
		}
		else
		{
			newLocale = JAPController.getLocale();
		}
		if (!JAPController.getLocale().equals(newLocale))
		{
			bNeedRestart = true;
		}
		int newDefaultView = JAPConstants.VIEW_NORMAL;
		if (m_rbViewSimplified.isSelected())
		{
			newDefaultView = JAPConstants.VIEW_SIMPLIFIED;
		}

		if (!bNeedRestart && JAPModel.getInstance().getDefaultView() != newDefaultView)
		{
			bNeedRestart = true;
		}

		String newLaF;
		if (m_comboUI.getSelectedIndex() >= 0)
		{
			newLaF = UIManager.getInstalledLookAndFeels()[m_comboUI.getSelectedIndex()].getClassName();
		}
		else
		{
			newLaF = UIManager.getLookAndFeel().getClass().getName();
		}
		if (!bNeedRestart && !UIManager.getLookAndFeel().getClass().getName().equals(newLaF))
		{
			bNeedRestart = true;
		}

		if (bNeedRestart)
		{
			if (JAPDialog.showYesNoDialog(getRootPanel(), JAPMessages.getString(MSG_NEED_RESTART)))
			{
				JAPConf.getInstance().setNeedRestart();
			}
			else
			{
				newLocale = JAPController.getLocale();
				newDefaultView = JAPModel.getInstance().getDefaultView();
				newLaF = UIManager.getLookAndFeel().getClass().getName();
			}
		}

		JAPController.setLocale(newLocale);
		JAPController.getInstance().setDefaultView(newDefaultView);
		try
		{
			JAPModel.getInstance().setLookAndFeel(newLaF);
		}
		catch (Exception ex)
		{
		}

		return true;
	}


	private void setLanguageComboIndex(Locale a_locale)
	{
		LanguageMapper langMapper = new LanguageMapper(a_locale.getLanguage());
		int i = 0;

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
	}

	protected void onUpdateValues()
	{
		updateUICombo();

		m_slidFontSize.setValue(JAPModel.getInstance().getFontSize());
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
				m_comboUI.setSelectedIndex(i);
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

	private void updateUICombo()
	{
		synchronized (m_comboUI)
		{
			LookAndFeelInfo[] lf = UIManager.getInstalledLookAndFeels();
			Vector vecLFs = new Vector(lf.length);
			Vector vecLFNames = new Vector(lf.length);
			String currentLf = UIManager.getLookAndFeel().getClass().getName();

			// eliminate duplicate L&Fs
			for (int i = 0; i < lf.length; i++)
			{
				if (!vecLFNames.contains(lf[i].getClassName()))
				{
					vecLFNames.addElement(lf[i].getClassName());
					vecLFs.addElement(lf[i]);
				}
			}
			lf = new LookAndFeelInfo[vecLFs.size()];
			for (int i = 0; i < lf.length; i++)
			{
				lf[i] = (LookAndFeelInfo)vecLFs.elementAt(i);
			}
			UIManager.setInstalledLookAndFeels(lf);

			m_comboUI.removeAllItems();
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
		}
	}
}
