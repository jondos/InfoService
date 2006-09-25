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
import gui.wizard.BasicWizardPage;
import java.awt.GridLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import javax.swing.JLabel;
import javax.swing.JTextField;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JFrame;
import javax.swing.JFileChooser;
import javax.swing.JOptionPane;
import javax.swing.filechooser.*;
import java.awt.Component;
import java.awt.Image;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.io.File;

import jap.JAPUtil;
import jap.JAPConstants;
import jap.JAPMessages;

public class JAPWelcomeWizardPage extends BasicWizardPage implements ActionListener
	{
		private JTextField m_tfJapPath=null;
		private JLabel m_labelClickNext;
		//search the folder for saving the new jap.jar
		private JButton m_bttnChooseJapFile = null;
		private File m_fileAktJapJar;
		private JAPUpdateWizard m_theUpdateWizard;

		private JarFileFilter jarFileFilter = new JarFileFilter();

		private final String COMMAND_SEARCH = "SEARCH";
		final JFileChooser m_fileChooser = new JFileChooser(System.getProperty("user.dir", ""));

		public JAPWelcomeWizardPage()
			{
				//this.updateWizard = updateWizard;
				setIcon(JAPUtil.loadImageIcon(JAPConstants.DOWNLOADFN,false));
				setPageTitle(JAPMessages.getString("updateWelcomeWizardPageTitle"));

				GridBagLayout m_panelComponentsLayout = new GridBagLayout();
				GridBagConstraints m_panelConstraints = new GridBagConstraints();

				m_panelComponents.setLayout(m_panelComponentsLayout);


				JLabel label = new JLabel(JAPMessages.getString("updateIntroductionMessage"));
				m_panelConstraints.weightx = 1.0;
				m_panelConstraints.weighty = 1.0;
				m_panelConstraints.gridx = 0;
				m_panelConstraints.gridy = 0;
				m_panelConstraints.gridwidth = 2;
				m_panelConstraints.anchor = GridBagConstraints.NORTH;
				m_panelComponentsLayout.setConstraints(label, m_panelConstraints);
				m_panelComponents.add(label);


				m_tfJapPath=new JTextField(20);
				m_panelConstraints.anchor = GridBagConstraints.WEST;
				m_panelConstraints.gridx = 0;
				m_panelConstraints.gridy = 1;
				m_panelConstraints.gridwidth = 1 ;
				m_panelComponentsLayout.setConstraints(m_tfJapPath, m_panelConstraints);
				m_panelComponents.add(m_tfJapPath, m_panelConstraints);
				m_tfJapPath.setText(System.getProperty("user.dir",".")+System.getProperty("file.separator","/")+"JAP.jar");


				m_bttnChooseJapFile = new JButton(JAPMessages.getString("updateM_chooseFolder_bttn"));
				m_panelConstraints.anchor = GridBagConstraints.EAST;
				m_panelConstraints.gridx = 1;
				m_panelConstraints.gridy = 1;
				m_panelComponentsLayout.setConstraints(m_bttnChooseJapFile, m_panelConstraints);
				m_panelComponents.add(m_bttnChooseJapFile, m_panelConstraints);
				m_bttnChooseJapFile.addActionListener(this);
				m_bttnChooseJapFile.setActionCommand(COMMAND_SEARCH);

				m_labelClickNext = new JLabel(JAPMessages.getString("updateM_labelClickNext"));
				m_panelConstraints.anchor = GridBagConstraints.WEST;
				m_panelConstraints.gridx = 0;
				m_panelConstraints.gridy = 2;
				m_panelConstraints.gridwidth = 2;
				m_panelComponentsLayout.setConstraints(m_labelClickNext, m_panelConstraints);
				m_panelComponents.add(m_labelClickNext,m_panelConstraints);

			}

			// is a file chosen? called by host when pressed next
			public boolean checkPage()
				{
					// needed for testing whether the user typed in a correct File
					File testFile;
					boolean checkPage = false;
					if(!m_tfJapPath.getText().equals(""))
						{//test whether it's a file
							testFile = new File(m_tfJapPath.getText());
							if(testFile.isFile() && testFile.exists())
								{
									m_fileAktJapJar = testFile;
									checkPage = true;
								}
							else
								{
									checkPage = false;
								}
						}
					return checkPage;
				}

			// there is sthing wrong with the selection
			public void showInformationDialog(String message)
			{
					JOptionPane.showMessageDialog((Component)this, message);
			}

	/*    private void createFileChooser()
			{
					this.setEnabled(false);
					final JFrame m_fileChooserDialog = new JFrame("Directory");
					final JFileChooser m_fileChooser = new JFileChooser(m_tfJapPath.getText());
				 // int returnval = m_fileChooser.showOpenDialog(m_fileChooserDialog);
					m_fileChooser.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
								int returnVal = m_fileChooser.showOpenDialog(m_fileChooserDialog);

								if (returnVal == JFileChooser.APPROVE_OPTION) {
										 m_fileAktJapJar = m_fileChooser.getSelectedFile();
										//System.out.println(selectedFile.getName());
										m_fileChooserDialog.dispose();

								} else {

								}
						}
				});

					//m_fileChooserDialog.getContentPane().add(m_fileChooser);
					m_fileChooserDialog.setVisible(true);
					m_fileChooserDialog.pack();
			}
*/
			//called by JAPUpdateWizard.next()
			public File getJapJarFile()
				{
					return m_fileAktJapJar;
				}

			public void actionPerformed(ActionEvent e)
			{
				if(e.getActionCommand().equals(COMMAND_SEARCH))
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
												if((!m_fileAktJapJar.isFile()))
													{
														m_fileChooser.cancelSelection();

														showInformationDialog(JAPMessages.getString("updateM_fileChooserDialogNotAFile"));
														m_tfJapPath.setText("");
														//checkPage = false;

													}else if(!m_fileAktJapJar.exists())
													{
													 if(m_tfJapPath.getText().equals(""))
															{
																	m_fileChooser.cancelSelection();
																	showInformationDialog(JAPMessages.getString("updateM_fileChooserDialogFileNotExists"));
																	m_tfJapPath.setText("");
																	//checkPage = false;
															}else
															{//user wrote sthing in the textfield --> test wheter it exists
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


													}//else

								}

					 }
		 }

}