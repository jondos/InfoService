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
package jap.pay.wizard;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JLabel;

import anon.pay.BIConnection;
import anon.pay.PayAccount;
import anon.pay.xml.XMLPassivePayment;
import gui.GUIUtils;
import gui.JAPMessages;
import gui.wizard.BasicWizardHost;
import gui.wizard.BasicWizardPage;
import jap.JAPConstants;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

public class SubmitPage extends BasicWizardPage
{
	/** Messages*/
	private static final String MSG_SENT = SubmitPage.class.
		getName() + "_sent";
	private static final String MSG_NOTSENT = SubmitPage.class.
		getName() + "_notsent";
	private static final String MSG_SUBMITTITLE = SubmitPage.class.
		getName() + "_submittitle";
	private static final String MSG_SUBMITTING = SubmitPage.class.
		getName() + "_submitting";

	private JLabel m_submittingLabel, m_infoLabel;
	private GridBagConstraints m_c = new GridBagConstraints();
	private PayAccount m_payAccount;
	private BasicWizardHost m_host;

	public SubmitPage(PayAccount a_payAccount, BasicWizardHost a_host)
	{
		m_payAccount = a_payAccount;
		m_host = a_host;
		setPageTitle(JAPMessages.getString(MSG_SUBMITTITLE));
		m_c = new GridBagConstraints();
		m_submittingLabel = new JLabel(JAPMessages.getString(MSG_SUBMITTING),
									   GUIUtils.loadImageIcon(JAPConstants.BUSYFN, true), JLabel.CENTER);
		Font f = m_submittingLabel.getFont();
		m_submittingLabel.setFont(new Font(f.getName(), f.getStyle(), f.getSize() - 1));
		m_submittingLabel.setVerticalTextPosition(JLabel.TOP);
		m_submittingLabel.setHorizontalTextPosition(JLabel.CENTER);
		m_c.gridx = 0;
		m_c.gridy = 0;
		m_c.weightx = 0;
		m_c.weightx = 0;
		m_c.insets = new Insets(5, 5, 5, 5);
		m_c.anchor = m_c.NORTHWEST;
		m_c.fill = m_c.NONE;
		m_panelComponents.setLayout(new GridBagLayout());
		m_panelComponents.add(m_submittingLabel, m_c);
		m_submittingLabel.setVisible(false);
		m_c.gridy++;
		m_panelComponents.add(m_submittingLabel, m_c);
		m_infoLabel = new JLabel();
		m_panelComponents.add(m_infoLabel, m_c);
		m_infoLabel.setVisible(false);
	}

	public void submitPassivePayment(final XMLPassivePayment a_pp)
	{
		m_infoLabel.setVisible(false);
		m_submittingLabel.setVisible(true);
		m_host.setFinishEnabled(false);

		Runnable doIt = new Runnable()
		{
			public void run()
			{
				/** Post data to payment instance */
				BIConnection biConn = new BIConnection(m_payAccount.getBI());
				try
				{
					biConn.connect();
					biConn.authenticate(m_payAccount.getAccountCertificate(), m_payAccount.getSigningInstance());
					if (!biConn.sendPassivePayment(a_pp))
					{
						m_submittingLabel.setVisible(false);
						m_infoLabel.setText("<html>" + JAPMessages.getString(MSG_NOTSENT) + "</html>");
						m_infoLabel.setVisible(true);
						m_host.setFinishEnabled(false);

					}
					biConn.disconnect();
					m_host.setFinishEnabled(true);
					m_host.setBackEnabled(false);
					m_submittingLabel.setVisible(false);

					//Show success info
					m_infoLabel.setText("<html>" + JAPMessages.getString(MSG_SENT) + "</html>");
					m_infoLabel.setVisible(true);
					m_host.setCancelEnabled(false);
				}
				catch (Exception e)
				{
					LogHolder.log(LogLevel.EXCEPTION, LogType.PAY,
								  "Could not send PassivePayment to payment instance: " + e.getMessage());
					m_submittingLabel.setVisible(false);
					m_infoLabel.setText("<html>" + JAPMessages.getString(MSG_NOTSENT) + "</html>");
					m_infoLabel.setVisible(true);
				}
			}
		};

		Thread t = new Thread(doIt);
		t.start();
	}
}
