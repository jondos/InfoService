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
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.JLabel;
import javax.swing.JPanel;

import anon.pay.PayAccount;
import anon.pay.xml.XMLPaymentOption;
import anon.pay.xml.XMLTransCert;
import gui.JAPMessages;
import gui.wizard.BasicWizardHost;
import gui.wizard.BasicWizardPage;
import jap.JAPConstants;
import jap.JAPController;
import jap.JAPUtil;
import jap.platform.AbstractOS;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

public class PaymentWizardPaymentInfoPage extends BasicWizardPage implements MouseListener
{
	private XMLPaymentOption m_selectedOption;
	private XMLTransCert m_transCert;
	private PayAccount m_payAccount;
	private JLabel m_fetchingLabel, m_detailedInfoLabel, m_extraInfoLabel;
	private JPanel m_infoPanel;
	private String m_language, m_amount, m_currency;
	private BasicWizardHost m_host;

	public PaymentWizardPaymentInfoPage(PayAccount a_payAccount, BasicWizardHost a_host)
	{
		m_payAccount = a_payAccount;
		m_host = a_host;
		m_language = JAPController.getLocale().getLanguage();
		setPageTitle(JAPMessages.getString("payWizPaymentInfoTitle"));
		GridBagConstraints c = new GridBagConstraints();
		m_fetchingLabel = new JLabel(JAPMessages.getString("fetchingTransferNumber"),
									 JAPUtil.loadImageIcon(JAPConstants.BUSYFN, true), JLabel.CENTER);
		Font f = m_fetchingLabel.getFont();
		m_fetchingLabel.setFont(new Font(f.getName(), f.getStyle(), f.getSize() - 1));
		m_fetchingLabel.setVerticalTextPosition(JLabel.TOP);
		m_fetchingLabel.setHorizontalTextPosition(JLabel.CENTER);
		c.insets = new Insets(5, 5, 5, 5);
		c.anchor = c.NORTHWEST;
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 0;
		c.weighty = 0;
		m_panelComponents.add(m_fetchingLabel);
		m_fetchingLabel.setVisible(false);
		createInfoPanel();
		m_panelComponents.add(m_infoPanel, c);
	}

	private void createInfoPanel()
	{
		JPanel p = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 0;
		c.fill = c.NONE;
		c.insets = new Insets(5, 5, 5, 5);
		c.anchor = c.NORTHWEST;
		m_detailedInfoLabel = new JLabel();
		p.add(m_detailedInfoLabel, c);
		c.gridy++;
		c.weightx = 1;
		c.weighty = 1;
		m_extraInfoLabel = new JLabel(" ");
		p.add(m_extraInfoLabel, c);

		m_infoPanel = p;
	}

	public void fetchTransferNumber()
	{
		if (m_transCert == null)
		{
			m_host.setFinishEnabled(false);
			m_infoPanel.setVisible(false);
			m_fetchingLabel.setVisible(true);

			Runnable doIt = new Runnable()
			{
				public void run()
				{
					try
					{
						LogHolder.log(LogLevel.DEBUG, LogType.PAY,
									  "Fetching Transaction Certificate from Payment Instance");

						m_transCert = m_payAccount.charge();

						m_fetchingLabel.setVisible(false);
						m_infoPanel.setVisible(true);
						updateExtraInfo();
						m_host.setFinishEnabled(true);
					}
					catch (Exception e)
					{
						LogHolder.log(LogLevel.EXCEPTION, LogType.NET,
									  "Error fetching TransCert: " + e.getMessage());
						m_fetchingLabel.setVisible(false);
					}
				}
			};

			Thread t = new Thread(doIt);
			t.start();
		}
	}

	private void updateExtraInfo()
	{
		if (m_transCert != null)
		{
			String extraInfo = m_selectedOption.getExtraInfo(m_language);
//			extraInfo = extraInfo.replaceAll("%t", String.valueOf(m_transCert.getTransferNumber()));
//			extraInfo = extraInfo.replaceAll("%a", m_amount);
//			extraInfo = extraInfo.replaceAll("%c", m_currency);
//			extraInfo = "<html>" + extraInfo + "</html>";
			m_extraInfoLabel.setText(extraInfo);

			if (m_selectedOption.getExtraInfoType(m_language).equalsIgnoreCase(XMLPaymentOption.EXTRA_LINK))
			{
				/** Make label clickable */
				makeLabelClickable(m_extraInfoLabel);
			}
		}
	}

	private void makeLabelClickable(JLabel a_label)
	{
		a_label.addMouseListener(this);
		String link = a_label.getText();
		if (link.indexOf("<html>") != -1)
		{
//			link = link.replaceAll("<html>", "<html><font color=blue><u>");
//			link = link.replaceAll("</html>", "</u></font></html>");
		}
		else
		{
			link = "<html><font color=blue><u>" + link + "</u></font></html>";
		}
		a_label.setText(link);
	}

	public void setSelectedPaymentOption(XMLPaymentOption a_option)
	{
		m_selectedOption = a_option;
		setPageTitle("<html>" + a_option.getHeading(m_language) + "</html>");
		m_detailedInfoLabel.setText("<html>" + a_option.getDetailedInfo(m_language) + "</html>");
		updateExtraInfo();
	}

	public void setSelectedCurrency(String a_currency)
	{
		m_currency = a_currency;
	}

	public void setAmount(String a_amount)
	{
		m_amount = a_amount;
	}

	public void mouseClicked(MouseEvent e)
	{
		if (e.getSource() == m_extraInfoLabel)
		{
			AbstractOS os = AbstractOS.getInstance();
			String link = m_extraInfoLabel.getText();
//			link = link.replaceAll("<br>", "");
//			link = link.replaceAll("<p>", "");
///			link = link.replaceAll("<html>", "");
//			link = link.replaceAll("</html>", "");
//			link = link.replaceAll("<font color=blue><u>", "");
//			link = link.replaceAll("</u></font>", "");
//			link = link.replaceAll(" ", "");

			LogHolder.log(LogLevel.DEBUG, LogType.PAY, "Opening " + link + " in browser.");
			os.openURLInBrowser(link);
		}
	}

	public void mousePressed(MouseEvent e)
	{
	}

	public void mouseReleased(MouseEvent e)
	{
	}

	public void mouseEntered(MouseEvent e)
	{
	}

	public void mouseExited(MouseEvent e)
	{
	}
}
