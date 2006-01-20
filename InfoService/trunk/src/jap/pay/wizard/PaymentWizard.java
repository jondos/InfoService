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

import javax.swing.JOptionPane;

import anon.pay.PayAccount;
import gui.JAPMessages;
import gui.wizard.BasicWizard;
import gui.wizard.BasicWizardHost;
import gui.wizard.WizardPage;
import jap.JAPController;
import java.awt.Dimension;

/**
 * This wizard guides the user through the account charging process
 *
 * @author Tobias Bayer
 */
public class PaymentWizard extends BasicWizard
{
	BasicWizardHost m_host;
	PaymentWizardWelcomePage m_welcomePage;
	PaymentWizardMethodSelectionPage m_methodSelectionPage;
	PaymentWizardPaymentInfoPage m_infoPage;
	SubmitPage m_submitPage;
	PayAccount m_payAccount;

	public PaymentWizard(PayAccount a_payAccount)
	{
		m_payAccount = a_payAccount;
		setWizardTitle(JAPMessages.getString("paymentWizardTitle"));
		m_host = new BasicWizardHost(JAPController.getView(), this);
		setHost(m_host);
		m_welcomePage = new PaymentWizardWelcomePage();
		m_methodSelectionPage = new PaymentWizardMethodSelectionPage(a_payAccount, m_host);
		m_infoPage = new PaymentWizardPaymentInfoPage(a_payAccount, m_host);
		m_submitPage = new SubmitPage(a_payAccount, m_host);

		addWizardPage(0, m_welcomePage);
		addWizardPage(1, m_methodSelectionPage);
		addWizardPage(2, m_infoPage);
		addWizardPage(3, m_submitPage);

		m_host.setHelpEnabled(false);
		m_host.getDialogParent().setSize(new Dimension(640, 480));

		invokeWizard();
	}

	public WizardPage next()
	{
		if (m_PageIndex == 1 && m_methodSelectionPage.getSelectedPaymentOption() == null)
		{
			JOptionPane.showMessageDialog(
				m_host.getDialogParent(), JAPMessages.getString("payWizNoMethod"),
				JAPMessages.getString("error"),
				JOptionPane.ERROR_MESSAGE
				);
			return null;
		}
		if (m_PageIndex == 1 && m_methodSelectionPage.getAmount().trim().equals(""))
		{
			JOptionPane.showMessageDialog(
				m_host.getDialogParent(), JAPMessages.getString("payWizNoAmount"),
				JAPMessages.getString("error"),
				JOptionPane.ERROR_MESSAGE
				);
			return null;
		}



		super.next();
		//Fetch methods from BI if MethodSelection page is shown
		if (m_PageIndex == 1)
		{
			m_methodSelectionPage.fetchPaymentOptions();
		}
		//Fetch transfer number, get detailed info
		else if (m_PageIndex == 2)
		{
			m_infoPage.setPaymentOptions(m_methodSelectionPage.getPaymentOptions());
			m_infoPage.setSelectedPaymentOption(m_methodSelectionPage.getSelectedPaymentOption());
			m_infoPage.setSelectedCurrency(m_methodSelectionPage.getSelectedCurrency());
			m_infoPage.setAmount(m_methodSelectionPage.getAmount());
			m_infoPage.fetchTransferNumber();
			m_infoPage.updateExtraInfo();

		}
		else if (m_PageIndex == 3)
		{
			m_submitPage.submitPassivePayment(m_infoPage.getPassiveInfo());
		}
		return null;
	}

	public WizardPage finish()
	{
		super.finish();
		m_host.getDialogParent().dispose();
		return null;
	}
}
