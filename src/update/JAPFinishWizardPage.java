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

import jap.JAPConstants;
import jap.JAPUtil;
import jap.JAPMessages;

import gui.wizard.BasicWizardPage;
import javax.swing.JLabel;
import javax.swing.JTextField;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.border.EmptyBorder;

public class JAPFinishWizardPage extends BasicWizardPage
{

public JLabel m_labelBackupOfJapJar;

  public JAPFinishWizardPage(String name)
  {
    setIcon(JAPUtil.loadImageIcon(JAPConstants.DOWNLOADFN,false));
    setPageTitle("Update-Wizard beenden");
    GridBagLayout gridBagFinish=new GridBagLayout();
    GridBagConstraints constraintsFinish=new GridBagConstraints();

    m_panelComponents.setLayout(gridBagFinish);
    JLabel labelFinish = new JLabel(JAPMessages.getString("updateFinishMessage"));
    constraintsFinish.gridx = 0;
    constraintsFinish.gridy = 0;
    constraintsFinish.weightx=1;
    constraintsFinish.fill=GridBagConstraints.HORIZONTAL;
    constraintsFinish.anchor = GridBagConstraints.NORTHWEST;
    gridBagFinish.setConstraints(labelFinish,constraintsFinish);
    m_panelComponents.add(labelFinish, constraintsFinish);

    m_labelBackupOfJapJar = new JLabel();
    constraintsFinish.gridx = 0;
    constraintsFinish.gridy = 1;
    constraintsFinish.gridwidth = 2;
    constraintsFinish.anchor = GridBagConstraints.WEST;
    constraintsFinish.fill = GridBagConstraints.HORIZONTAL;
    gridBagFinish.setConstraints(m_labelBackupOfJapJar,constraintsFinish);
    m_panelComponents.add(m_labelBackupOfJapJar);
//    setVisible(true);
  }



}