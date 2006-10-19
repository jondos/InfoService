/*
 Copyright (c) 2000, The JAP-Team
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

package update;

import gui.JAPMultilineLabel;
import gui.wizard.BasicWizardPage;
import jap.JAPConstants;
import gui.JAPMessages;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JLabel;
import javax.swing.JTextField;
import gui.GUIUtils;
import anon.util.ClassUtil;
import anon.infoservice.JAPVersionInfo;

public class JAPWelcomeWizardPage extends BasicWizardPage implements ActionListener
{
	private JTextField m_tfJapPath = null;
	private JLabel m_labelClickNext;

	//search the folder for saving the new jap.jar
	private JButton m_bttnChooseJapFile = null;
	private File m_fileAktJapJar;
	private JCheckBox m_cbIncrementalUpdate;
	private JarFileFilter jarFileFilter = new JarFileFilter();

	private final String COMMAND_SEARCH = "SEARCH";
	private boolean m_bIncrementalUpdate = false;
	final JFileChooser m_fileChooser =
		new JFileChooser(ClassUtil.getClassDirectory(ClassUtil.class).getParent());

	public JAPWelcomeWizardPage(JAPVersionInfo a_versionInfo)
	{
		//this.updateWizard = updateWizard;
		setIcon(GUIUtils.loadImageIcon(JAPConstants.DOWNLOADFN, false));
		setPageTitle(JAPMessages.getString("updateWelcomeWizardPageTitle",
										   new Object[]{a_versionInfo.getJapVersion()}));

		GridBagLayout m_panelComponentsLayout = new GridBagLayout();
		GridBagConstraints m_panelConstraints = new GridBagConstraints();

		m_panelComponents.setLayout(m_panelComponentsLayout);

		JAPMultilineLabel label = new JAPMultilineLabel(JAPMessages.getString("updateIntroductionMessage"));

		m_panelConstraints.anchor = GridBagConstraints.NORTHWEST;
		m_panelConstraints.fill = GridBagConstraints.HORIZONTAL;
		m_panelConstraints.weightx = 1.0;
		m_panelConstraints.weighty = 0;
		m_panelConstraints.gridx = 0;
		m_panelConstraints.gridy = 0;
		m_panelConstraints.gridwidth = 2;
		m_panelComponentsLayout.setConstraints(label, m_panelConstraints);
		m_panelComponents.add(label);

		m_tfJapPath = new JTextField(20);
		m_panelConstraints.anchor = GridBagConstraints.WEST;
		m_panelConstraints.gridx = 0;
		m_panelConstraints.gridy = 1;
		m_panelConstraints.gridwidth = 1;
		m_panelConstraints.weighty = 0;
		m_panelConstraints.weightx = 1.0;
		m_panelConstraints.insets = new Insets(10, 0, 0, 10);
		m_panelConstraints.fill = GridBagConstraints.HORIZONTAL;
		m_panelComponentsLayout.setConstraints(m_tfJapPath, m_panelConstraints);
		m_panelComponents.add(m_tfJapPath, m_panelConstraints);
		m_tfJapPath.setText(ClassUtil.getClassDirectory(ClassUtil.class).getParent() +
							System.getProperty("file.separator", "/") + "JAP.jar");

		m_bttnChooseJapFile = new JButton(JAPMessages.getString("updateM_chooseFolder_bttn"));
		m_panelConstraints.anchor = GridBagConstraints.EAST;
		m_panelConstraints.gridx = 1;
		m_panelConstraints.gridy = 1;
		m_panelConstraints.weightx = 0;
		m_panelConstraints.insets = new Insets(10, 0, 0, 0);
		m_panelConstraints.fill = GridBagConstraints.NONE;
		m_panelComponentsLayout.setConstraints(m_bttnChooseJapFile, m_panelConstraints);
		m_panelComponents.add(m_bttnChooseJapFile);
		m_bttnChooseJapFile.addActionListener(this);
		m_bttnChooseJapFile.setActionCommand(COMMAND_SEARCH);

		m_cbIncrementalUpdate = new JCheckBox(JAPMessages.getString("updateM_doIncrementalUpdate"));
		m_cbIncrementalUpdate.setToolTipText(JAPMessages.getString("updateM_doIncrementalUpdate"));
		m_cbIncrementalUpdate.setSelected(m_bIncrementalUpdate);
		m_panelConstraints.insets = new Insets(0, 10, 10, 0);
		m_panelConstraints.anchor = GridBagConstraints.WEST;
		m_panelConstraints.gridx = 0;
		m_panelConstraints.gridy = 2;
		m_panelConstraints.weightx = 1.0;
		m_panelConstraints.weighty = 0;
		m_panelConstraints.fill = GridBagConstraints.HORIZONTAL;
		m_panelConstraints.gridwidth = 2;
		m_panelComponentsLayout.setConstraints(m_cbIncrementalUpdate, m_panelConstraints);
		m_panelComponents.add(m_cbIncrementalUpdate);

		m_labelClickNext = new JLabel(JAPMessages.getString("updateM_labelClickNext"));
		m_panelConstraints.insets = new Insets(0, 0, 0, 0);
		m_panelConstraints.anchor = GridBagConstraints.WEST;
		m_panelConstraints.gridx = 0;
		m_panelConstraints.gridy = 3;
		m_panelConstraints.weightx = 1.0;
		m_panelConstraints.weighty = 0;
		m_panelConstraints.fill = GridBagConstraints.HORIZONTAL;
		m_panelConstraints.gridwidth = 2;
		m_panelComponentsLayout.setConstraints(m_labelClickNext, m_panelConstraints);
		m_panelComponents.add(m_labelClickNext);

		JLabel l = new JLabel("");
		m_panelConstraints.gridx = 0;
		m_panelConstraints.gridy = 4;
		m_panelConstraints.weighty = 1.0;
		m_panelConstraints.fill = GridBagConstraints.VERTICAL;
		m_panelComponentsLayout.setConstraints(l, m_panelConstraints);
		m_panelComponents.add(l);

	}

	// is a file chosen? called by host when pressed next
	public boolean checkPage()
	{
		// needed for testing whether the user typed in a correct File
		File testFile;
		boolean checkPage = false;
		if (!m_tfJapPath.getText().equals(""))
		{ //test whether it's a file
			testFile = new File(m_tfJapPath.getText());
			if (testFile.isFile() && testFile.exists())
			{
				m_fileAktJapJar = testFile;
				checkPage = true;
			}
			else
			{
				showInformationDialog(JAPMessages.getString("updateM_SelectedJapJarDoesNotExist"));
				checkPage = false;
			}
		}
		return checkPage;
	}

	//called by JAPUpdateWizard.next()
	public File getJapJarFile()
	{
		return m_fileAktJapJar;
	}

	public boolean isIncrementalUpdate()
	{
		return m_cbIncrementalUpdate.isSelected();
	}

	public void actionPerformed(ActionEvent e)
	{
		if (e.getActionCommand().equals(COMMAND_SEARCH))
		{
			//final JFileChooser m_fileChooser = new JFileChooser(System.getProperty("user.dir", ""));
			m_fileChooser.setDialogTitle(JAPMessages.getString("updateM_fileChooserTitle"));
			m_fileChooser.setApproveButtonText(JAPMessages.getString("updateM_fileChooserApprove_bttn"));
			m_fileChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
			//m_fileChooser.setFileFilter(jarFileFilter);
			m_fileChooser.addChoosableFileFilter(jarFileFilter);
			int returnVal = m_fileChooser.showOpenDialog(this);

			if (returnVal == JFileChooser.APPROVE_OPTION)
			{
				m_fileAktJapJar = m_fileChooser.getSelectedFile();
				if ( (!m_fileAktJapJar.isFile()))
				{
					m_fileChooser.cancelSelection();

					showInformationDialog(JAPMessages.getString("updateM_fileChooserDialogNotAFile"));
					m_tfJapPath.setText("");
					//checkPage = false;

				}
				else if (!m_fileAktJapJar.exists())
				{
					if (m_tfJapPath.getText().equals(""))
					{
						m_fileChooser.cancelSelection();
						showInformationDialog(JAPMessages.getString("updateM_fileChooserDialogFileNotExists"));
						m_tfJapPath.setText("");
						//checkPage = false;
					}
					else
					{ //user wrote sthing in the textfield --> test wheter it exists
						m_tfJapPath.getText();

						//  checkPage = true;
					}
				}
				else
				{
					//System.out.println(selectedFile.getName());
					//checkPage = true;
					m_tfJapPath.setText(m_fileAktJapJar.getAbsolutePath());

					//updateWizard.setSelectedFile(selectedFile.getAbsolutePath());

				} //else

			}

		}
	}

}
