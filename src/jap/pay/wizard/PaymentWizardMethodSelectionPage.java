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

import java.util.Enumeration;
import java.util.Vector;

import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JRadioButton;

import anon.pay.BI;
import anon.pay.BIConnection;
import anon.pay.PayAccount;
import anon.pay.PayAccountsFile;
import anon.pay.xml.XMLPaymentOption;
import anon.pay.xml.XMLPaymentOptions;
import gui.JAPMessages;
import gui.wizard.BasicWizardHost;
import gui.wizard.BasicWizardPage;
import jap.JAPConstants;
import jap.JAPController;
import gui.GUIUtils;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import gui.JAPJIntField;
import gui.JAPJIntField.IntFieldBounds;
import jap.pay.AccountSettingsPanel;

public class PaymentWizardMethodSelectionPage extends BasicWizardPage implements ActionListener
{
	/** Messages */
	private static final String MSG_PRICE = PaymentWizardMethodSelectionPage.class.
		getName() + "_price";

	private JLabel m_fetchingLabel;
	private ButtonGroup m_rbGroup;
	private XMLPaymentOptions m_paymentOptions;
	private GridBagConstraints m_c = new GridBagConstraints();
	private boolean m_fetched = false;
	private XMLPaymentOption m_selectedPaymentOption;
	private PayAccount m_payAccount;
	private JAPJIntField m_tfAmount;
	private JComboBox m_cbCurrency;
	private BasicWizardHost m_host;

	public PaymentWizardMethodSelectionPage(PayAccount a_payAccount, BasicWizardHost a_host)
	{
		m_payAccount = a_payAccount;
		m_host = a_host;
		setPageTitle(JAPMessages.getString("payWizMethodSelectionTitle"));
		m_rbGroup = new ButtonGroup();
		m_c = new GridBagConstraints();
		m_fetchingLabel = new JLabel(JAPMessages.getString("fetchingMethods"),
									 GUIUtils.loadImageIcon(JAPConstants.BUSYFN, true), JLabel.CENTER);
		Font f = m_fetchingLabel.getFont();
		m_fetchingLabel.setFont(new Font(f.getName(), f.getStyle(), f.getSize() - 1));
		m_fetchingLabel.setVerticalTextPosition(JLabel.TOP);
		m_fetchingLabel.setHorizontalTextPosition(JLabel.CENTER);
		m_c.gridx = 0;
		m_c.gridy = 0;
		m_c.weightx = 0;
		m_c.weightx = 0;
		m_c.insets = new Insets(5, 5, 5, 5);
		m_c.anchor = m_c.NORTHWEST;
		m_c.fill = m_c.NONE;
		m_panelComponents.setLayout(new GridBagLayout());
		m_panelComponents.add(m_fetchingLabel, m_c);
		m_fetchingLabel.setVisible(false);
		m_c.gridy++;
		m_panelComponents.add(m_fetchingLabel, m_c);
	}

	public void fetchPaymentOptions()
	{
		if (!m_fetched)
		{
			m_host.setNextEnabled(false);
			m_fetchingLabel.setVisible(true);

			Runnable doIt = new Runnable()
			{
				public void run()
				{
					try
					{
						BI pi = m_payAccount.getBI();
						BIConnection piConn = new BIConnection(pi);
						LogHolder.log(LogLevel.DEBUG, LogType.PAY,
									  "Connecting to PI: " + pi.getHostName() + ":" + pi.getPortNumber());
						piConn.connect();
						piConn.authenticate(PayAccountsFile.getInstance().getActiveAccount().
											getAccountCertificate(),
											PayAccountsFile.getInstance().getActiveAccount().
											getSigningInstance());
						LogHolder.log(LogLevel.DEBUG, LogType.PAY,
									  "Fetching payment options");
						XMLPaymentOptions xmlPayOpt = piConn.getPaymentOptions();
						m_paymentOptions = xmlPayOpt;
						piConn.disconnect();
						Enumeration headings = xmlPayOpt.getOptionHeadings(JAPController.getLocale().
							getLanguage());
						while (headings.hasMoreElements())
						{
							addOption( (String) headings.nextElement());
						}
						addCurrencies(m_paymentOptions.getCurrencies());
						m_fetchingLabel.setVisible(false);
						m_fetched = true;
						m_host.setNextEnabled(true);
					}
					catch (Exception e)
					{
						e.printStackTrace();
						LogHolder.log(LogLevel.EXCEPTION, LogType.NET,
									  "Error fetching payment options: " + e.getMessage());
						m_fetchingLabel.setVisible(false);
					}
				}
			};

			Thread t = new Thread(doIt);
			t.start();
		}
	}

	private void addOption(String a_name)
	{
		m_c.gridy++;
		m_c.gridwidth = 3;
		JRadioButton rb = new JRadioButton("<html>" + a_name + "</html>");
		rb.setName(a_name);
		rb.addActionListener(this);
		m_rbGroup.add(rb);
		m_panelComponents.add(rb, m_c);
	}

	private void addCurrencies(Vector a_currencies)
	{
		m_c.gridy++;
		m_c.gridwidth = 3;
		JLabel label = new JLabel(JAPMessages.getString(MSG_PRICE));
		m_panelComponents.add(label, m_c);
		m_c.gridy++;
		label = new JLabel(JAPMessages.getString("payAmount"));
		m_panelComponents.add(label, m_c);
		m_c.gridwidth = 1;
		m_tfAmount = new JAPJIntField(new IntFieldBounds()
		{
			public boolean isZeroAllowed()
			{
				return false;
			}

			public int getMaximum()
			{
				return 15;
			}

		}
		);
		m_c.gridy++;
		m_panelComponents.add(m_tfAmount, m_c);
		m_c.gridx++;
		m_c.weightx = 1;
		m_c.weighty = 1;
		m_cbCurrency = new JComboBox(a_currencies);
		m_panelComponents.add(m_cbCurrency, m_c);
	}

	public String getSelectedCurrency()
	{
		return (String) m_cbCurrency.getSelectedItem();
	}

	public String getAmount()
	{
		return m_tfAmount.getText();
	}

	public XMLPaymentOptions getPaymentOptions()
	{
		if (m_paymentOptions == null)
		{
			fetchPaymentOptions();
		}

		return m_paymentOptions;
	}

	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() instanceof JRadioButton)
		{
			String selectedHeading = ( (JRadioButton) e.getSource()).getName();
			m_selectedPaymentOption = m_paymentOptions.getOption(selectedHeading,
				JAPController.getLocale().getLanguage());
		}
	}

	public XMLPaymentOption getSelectedPaymentOption()
	{
		return m_selectedPaymentOption;
	}

}
