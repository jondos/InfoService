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

package update;

import jap.JAPController;
import jap.JAPConstants;
import jap.JAPUtil;
import jap.JAPMessages;

import anon.infoservice.InfoServiceHolder;
import anon.infoservice.JAPVersionInfo;


import javax.swing.JPanel;
import javax.swing.JLabel;
import javax.swing.JDialog;
import javax.swing.JTextArea;
import javax.swing.JComboBox;
import javax.swing.JButton;
import javax.swing.JScrollPane;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.GridBagConstraints;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.awt.event.ItemListener;
import java.awt.event.ItemEvent;
import java.awt.Insets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.border.TitledBorder;
import java.io.File;

public class JAPUpdate implements ActionListener,ItemListener,Runnable
	{
		private JDialog m_Dialog;
		private JTextArea m_taInfo;
		private JLabel m_labelVersion, m_labelDate;

	 // private JAPController japController;
		private JComboBox m_comboType;
		private JButton m_bttnUpgrade;

		private Thread m_threadGetVersionInfo;
		private JAPVersionInfo m_devVersion;
		private JAPVersionInfo m_releaseVersion;
		private DateFormat m_DateFormat;

		private final String COMMAND_ABORT="ABORT";
		private final String COMMAND_UPGRADE="UPGRADE";
		private final String COMMAND_HELP="HELP";

		public JAPUpdate()
			{
				m_Dialog = new JDialog(JAPController.getView(),"JAP Update",true);

				GridBagLayout gridBagFrame = new GridBagLayout();
				m_Dialog.getContentPane().setLayout(gridBagFrame);

				//The Buttons
				JPanel buttonPanel = new JPanel();
				GridBagLayout gridBagPanel = new GridBagLayout();
				buttonPanel.setLayout(gridBagPanel);
				GridBagConstraints cButtons= new GridBagConstraints();
				cButtons.gridx=GridBagConstraints.RELATIVE;
				cButtons.weightx=1.0;
				cButtons.weighty=1.0;
				cButtons.fill=GridBagConstraints.NONE;
				cButtons.anchor=GridBagConstraints.WEST;

				JButton bttnHelp = new JButton(JAPMessages.getString("updateM_bttnHelp"));
				bttnHelp.addActionListener(this);
				bttnHelp.setActionCommand(COMMAND_HELP);
				gridBagPanel.setConstraints(bttnHelp,cButtons);
				buttonPanel.add(bttnHelp);

				m_bttnUpgrade = new JButton("Upgrade");
				m_bttnUpgrade.addActionListener(this);
				m_bttnUpgrade.setActionCommand(COMMAND_UPGRADE);
				cButtons.anchor=GridBagConstraints.CENTER;
				gridBagPanel.setConstraints(m_bttnUpgrade,cButtons);
				m_bttnUpgrade.setEnabled(false);
				buttonPanel.add(m_bttnUpgrade);

				JButton bttnAbort = new JButton(JAPMessages.getString("updateM_bttnCancel"));
				bttnAbort.addActionListener(this);
				bttnAbort.setActionCommand(COMMAND_ABORT);
				cButtons.anchor=GridBagConstraints.EAST;
				gridBagPanel.setConstraints(bttnAbort,cButtons);
				buttonPanel.add(bttnAbort);

				//The Installed-Panel
				gridBagPanel=new GridBagLayout();
				GridBagConstraints c=new GridBagConstraints();
				TitledBorder titledBorder = new TitledBorder(" "+JAPMessages.getString("updateTitleBorderInstalled")+" ");
				JPanel installedPanel = new JPanel(gridBagPanel);
				installedPanel.setBorder(titledBorder);
				JLabel l=new JLabel("Version: ");
				c.gridx=0;
				c.gridy=0;
				c.anchor=c.NORTHWEST;
				c.weighty=0.33;
				c.weightx=0;
				c.fill=c.NONE;
				c.insets=new Insets(5,5,5,5);
				gridBagPanel.setConstraints(l,c);
				installedPanel.add(l);
				l=new JLabel(JAPConstants.aktVersion);
				c.gridx=1;
				c.fill=c.BOTH;
				c.weightx=1;
				gridBagPanel.setConstraints(l,c);
				installedPanel.add(l);
				l=new JLabel(JAPMessages.getString("updateLabelDate")+" ");
				c.gridx=0;
				c.gridy=1;
				c.weightx=0;
				c.fill=c.NONE;
				gridBagPanel.setConstraints(l,c);
				installedPanel.add(l);
				String strDate=JAPConstants.strReleaseDate;
				try
					{
						DateFormat sdf=new SimpleDateFormat("yyyy/MM/dd HH:mm:ss z");
						Date d=sdf.parse(strDate+" GMT");
						m_DateFormat=DateFormat.getDateTimeInstance(DateFormat.MEDIUM,DateFormat.MEDIUM);
						strDate=m_DateFormat.format(d);
					}
				catch(Exception e){e.printStackTrace();}
				l=new JLabel(strDate);
				c.gridx=1;
				c.weightx=1;
				c.fill=c.BOTH;
				gridBagPanel.setConstraints(l,c);
				installedPanel.add(l);
				l=new JLabel("Type: ");
				c.gridy=2;
				c.gridx=0;
				c.weightx=0;
				c.fill=c.NONE;
				gridBagPanel.setConstraints(l,c);
				installedPanel.add(l);
				if(JAPConstants.m_bReleasedVersion)
					l=new JLabel("Release");
				else
					l=new JLabel("Development");
				c.gridx=1;
				c.weightx=1;
				c.fill=c.BOTH;
				gridBagPanel.setConstraints(l,c);
				installedPanel.add(l);

			 //The latestVersion-Panel
				gridBagPanel=new GridBagLayout();
				titledBorder = new TitledBorder(" "+JAPMessages.getString("updateTitleBorderLatest")+" ");
				JPanel latestPanel = new JPanel(gridBagPanel);
				latestPanel.setBorder(titledBorder);
				l=new JLabel("Version: ");
				c.gridx=0;
				c.gridy=0;
				c.weightx=0;
				c.fill=c.NONE;
				gridBagPanel.setConstraints(l,c);
				latestPanel.add(l);
				m_labelVersion=new JLabel("Unknown");
				c.gridx=1;
				c.weightx=1;
				c.fill=c.BOTH;
				gridBagPanel.setConstraints(m_labelVersion,c);
				latestPanel.add(m_labelVersion);
				l=new JLabel(JAPMessages.getString("updateLabelDate")+" ");
				c.gridy=1;
				c.gridx=0;
				c.weightx=0;
				c.fill=c.NONE;
				gridBagPanel.setConstraints(l,c);
				latestPanel.add(l);
				m_labelDate=new JLabel("Unknown");
				c.gridx=1;
				c.weightx=1;
				c.fill=c.BOTH;
				gridBagPanel.setConstraints(m_labelDate,c);
				latestPanel.add(m_labelDate);
				l=new JLabel("Type: ");
				c.gridy=2;
				c.gridx=0;
				c.weightx=0;
				c.fill=c.NONE;
				gridBagPanel.setConstraints(l,c);
				latestPanel.add(l);
				m_comboType=new JComboBox();
				m_comboType.addItem("Release");
				m_comboType.addItem("Development");
				m_comboType.setEnabled(false);
				m_comboType.addItemListener(this);
				c.gridx=1;
				c.weightx=1;
				c.fill=c.BOTH;
				gridBagPanel.setConstraints(m_comboType,c);
				latestPanel.add(m_comboType);

				//The Info-Panel
				titledBorder = new TitledBorder(" Info ");
				JPanel infoPanel = new JPanel(new GridLayout(1,1));
				infoPanel.setBorder(titledBorder);
				m_taInfo=new JTextArea(10,40);
				m_taInfo.setEditable(false);
				m_taInfo.setHighlighter(null);
				JScrollPane scrollpane=new JScrollPane(m_taInfo);
				infoPanel.add(scrollpane);

				//Putting it all together
				GridBagConstraints cFrame = new GridBagConstraints();
				cFrame.insets=new Insets(10,10,10,10);
				cFrame.gridx = 0;
				cFrame.gridy = 0;
				cFrame.weightx = 1;
				cFrame.weighty = 0;
				cFrame.anchor = GridBagConstraints.NORTHWEST;
				cFrame.fill=GridBagConstraints.BOTH;
				gridBagFrame.setConstraints(installedPanel, cFrame);
				m_Dialog.getContentPane().add(installedPanel);

				cFrame.gridx = 1;
				cFrame.gridy = 0;
				cFrame.anchor = GridBagConstraints.NORTHEAST;
				gridBagFrame.setConstraints(latestPanel, cFrame);
				m_Dialog.getContentPane().add(latestPanel);

				cFrame.gridx = 0;
				cFrame.gridy = 1;
				cFrame.gridwidth = 2;
				cFrame.anchor = GridBagConstraints.CENTER;
				cFrame.fill=GridBagConstraints.BOTH;
				cFrame.weightx=1.0;
				cFrame.weighty=1.0;
				gridBagFrame.setConstraints(infoPanel, cFrame);
				m_Dialog.getContentPane().add(infoPanel);

				cFrame.gridx = 0;
				cFrame.gridy = 2;
				cFrame.weighty = 0;
				cFrame.fill = GridBagConstraints.HORIZONTAL;
				cFrame.anchor = GridBagConstraints.SOUTH;
				gridBagFrame.setConstraints(buttonPanel, cFrame);
				m_Dialog.getContentPane().add(buttonPanel);
				m_Dialog.pack();
				JAPUtil.centerFrame(m_Dialog);
				m_Dialog.setResizable(true);
				m_threadGetVersionInfo=new Thread(this);
				m_threadGetVersionInfo.start();
				m_Dialog.show();
			}

    public void run() {
      //Thread Run Loop for getting the Version Infos...
				m_taInfo.setText(JAPMessages.getString("updateFetchVersionInfo"));
      m_releaseVersion = InfoServiceHolder.getInstance().getJAPVersionInfo(JAPVersionInfo.JAP_RELEASE_VERSION);
      m_devVersion = InfoServiceHolder.getInstance().getJAPVersionInfo(JAPVersionInfo.JAP_DEVELOPMENT_VERSION);
      if ((m_releaseVersion == null) || (m_devVersion == null)) {
						m_taInfo.setText(JAPMessages.getString("updateFetchVersionInfoFailed"));
					}
      else {
						m_comboType.setEnabled(true);
						m_taInfo.setText("");
						m_labelVersion.setText(m_releaseVersion.getVersion());
        if (m_releaseVersion.getDate() != null) {
							m_labelDate.setText(m_DateFormat.format(m_releaseVersion.getDate()));
        }
        else {
          m_labelDate.setText("Unknown");
        }
						m_bttnUpgrade.setEnabled(true);
					}
			}

		public void actionPerformed(ActionEvent e)
			{
				if(e.getActionCommand().equals(COMMAND_ABORT))
					{
						try{m_threadGetVersionInfo.join();}catch(Exception ex){}
						m_Dialog.dispose();
					}
				else if(e.getActionCommand().equals(COMMAND_UPGRADE))
					{
						try{m_threadGetVersionInfo.join();}catch(Exception ex){}
						m_Dialog.dispose();
						// User' wants to Update --> give the version Info and the jnlp-file
						if(m_comboType.getSelectedIndex()==0)
							new JAPUpdateWizard(m_releaseVersion);
						else
							new JAPUpdateWizard(m_devVersion);
					}
			}

		public void itemStateChanged(ItemEvent e)
			{
				if(e.getStateChange()==ItemEvent.SELECTED)
					{
						if(m_comboType.getSelectedIndex()==0)//Release
							{
								m_labelVersion.setText(m_releaseVersion.getVersion());
								if(m_releaseVersion.getDate()!=null)
									m_labelDate.setText(m_DateFormat.format(m_releaseVersion.getDate()));
								else
									m_labelDate.setText("Unknown");
							}
						else
							{
								m_labelVersion.setText(m_devVersion.getVersion());
								if(m_devVersion.getDate()!=null)
									m_labelDate.setText(m_DateFormat.format(m_devVersion.getDate()));
								else
									m_labelDate.setText("Unknown");
							}
					}
			}
	}