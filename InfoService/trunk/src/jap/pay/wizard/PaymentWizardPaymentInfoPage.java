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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Vector;

import java.awt.Component;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;

import anon.pay.PayAccount;
import anon.pay.xml.XMLPassivePayment;
import anon.pay.xml.XMLPaymentOption;
import anon.pay.xml.XMLPaymentOptions;
import anon.pay.xml.XMLTransCert;
import anon.util.Util;
import gui.GUIUtils;
import gui.JAPMessages;
import gui.wizard.BasicWizardHost;
import gui.wizard.BasicWizardPage;
import jap.JAPConstants;
import jap.JAPController;
import jap.JAPUtil;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import platform.AbstractOS;

public class PaymentWizardPaymentInfoPage extends BasicWizardPage implements MouseListener, ActionListener
{
	/** Messages */
	private static final String MSG_BUTTON_COPY = PaymentWizardPaymentInfoPage.class.
		getName() + "_button_copy";
	private static final String MSG_BUTTON_OPEN = PaymentWizardPaymentInfoPage.class.
		getName() + "_button_open";

	private XMLPaymentOptions m_paymentOptions;
	private XMLPaymentOption m_selectedOption;
	private XMLTransCert m_transCert;
	private PayAccount m_payAccount;
	private JLabel m_fetchingLabel, m_detailedInfoLabel, m_extraInfoLabel, m_sendingLabel;
	private JButton m_bttnCopy, m_bttnOpen;
	private JPanel m_infoPanel, m_inputPanel;
	private String m_language, m_amount, m_currency;
	private BasicWizardHost m_host;
	private Vector m_inputFields;
	GridBagConstraints m_c = new GridBagConstraints();

	public PaymentWizardPaymentInfoPage(PayAccount a_payAccount, BasicWizardHost a_host)
	{
		m_payAccount = a_payAccount;
		m_host = a_host;
		m_language = JAPController.getLocale().getLanguage();
		setPageTitle(JAPMessages.getString("payWizPaymentInfoTitle"));
		m_fetchingLabel = new JLabel(JAPMessages.getString("fetchingTransferNumber"),
									 GUIUtils.loadImageIcon(JAPConstants.BUSYFN, true), JLabel.CENTER);
		Font f = m_fetchingLabel.getFont();
		m_fetchingLabel.setFont(new Font(f.getName(), f.getStyle(), f.getSize() - 1));
		m_fetchingLabel.setVerticalTextPosition(JLabel.TOP);
		m_fetchingLabel.setHorizontalTextPosition(JLabel.CENTER);
		m_c.insets = new Insets(5, 5, 5, 5);
		m_c.anchor = m_c.NORTHWEST;
		m_c.fill = m_c.NONE;
		m_c.gridx = 0;
		m_c.gridy = 0;
		m_c.weightx = 0;
		m_c.weighty = 0;
		m_panelComponents.setLayout(new GridBagLayout());
		m_panelComponents.add(m_fetchingLabel, m_c);
		m_fetchingLabel.setVisible(false);
		createInfoPanel();
		m_panelComponents.add(m_infoPanel, m_c);
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
		c.gridwidth = 2;
		m_detailedInfoLabel = new JLabel();
		p.add(m_detailedInfoLabel, c);
		c.gridy++;
		m_extraInfoLabel = new JLabel(" ");
		p.add(m_extraInfoLabel, c);

		c.gridy++;
		c.gridwidth = 1;
		m_bttnCopy = new JButton(JAPMessages.getString(MSG_BUTTON_COPY));
		m_bttnCopy.addActionListener(this);
		p.add(m_bttnCopy, c);

		c.gridx++;
		c.weightx = 1;
		c.weighty = 1;
		m_bttnOpen = new JButton(JAPMessages.getString(MSG_BUTTON_OPEN));
		m_bttnOpen.addActionListener(this);
		p.add(m_bttnOpen, c);
		m_bttnOpen.setVisible(false);

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

						if (m_selectedOption.getType().equalsIgnoreCase(XMLPaymentOption.OPTION_PASSIVE))
						{
							createInputPanel();
							m_panelComponents.remove(m_infoPanel);
							m_panelComponents.add(m_inputPanel, m_c);
							m_panelComponents.repaint();
							m_panelComponents.revalidate();
						}
						else
						{
							m_infoPanel.setVisible(true);
							updateExtraInfo();
							if (!m_selectedOption.getExtraInfoType(m_language).equalsIgnoreCase(
								XMLPaymentOption.EXTRA_LINK) && !m_selectedOption.getType().equals("CreditCard"))
							{
								m_host.setFinishEnabled(true);
								m_host.setNextEnabled(false);
							}
						}
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

	public void updateExtraInfo()
	{
		if (m_transCert != null && m_selectedOption.getType().equalsIgnoreCase(XMLPaymentOption.OPTION_ACTIVE))
		{
			String extraInfo = m_selectedOption.getExtraInfo(m_language);
			if (extraInfo != null)
			{
			extraInfo = JAPUtil.replaceAll(extraInfo, "%t", String.valueOf(m_transCert.getTransferNumber()));
			extraInfo = JAPUtil.replaceAll(extraInfo, "%a", m_amount);
			extraInfo = JAPUtil.replaceAll(extraInfo, "%c", m_currency);
			extraInfo = "<html>" + extraInfo + "</html>";
			m_extraInfoLabel.setText(extraInfo);

				if (m_selectedOption.getExtraInfoType(m_language).equalsIgnoreCase(XMLPaymentOption.
					EXTRA_LINK))
			{
				/** Make label clickable */
				makeLabelClickable(m_extraInfoLabel);
				m_bttnOpen.setVisible(true);
			}
			else
			{
				m_bttnOpen.setVisible(false);
			}
		}
	}
	}

	public XMLPaymentOption getSelectedPaymentOption()
	{
		return m_selectedOption;
	}

	private void makeLabelClickable(JLabel a_label)
	{
		a_label.addMouseListener(this);
		String link = a_label.getText();
		if (link.indexOf("<html>") != -1)
		{
			link = JAPUtil.replaceAll(link, "<html>", "<html><font color=blue><u>");
			link = JAPUtil.replaceAll(link, "</html>", "</u></font></html>");
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
	}

	public void setSelectedCurrency(String a_currency)
	{
		m_currency = a_currency;
	}

	public void setAmount(String a_amount)
	{
		m_amount = a_amount;
	}

	public void setPaymentOptions(XMLPaymentOptions a_options)
	{
		m_paymentOptions = a_options;
	}

	public void createInputPanel()
	{
		JTextField textField = null;
		JLabel label = null;
		JComboBox comboBox = null;

		m_inputFields = new Vector();
		Vector inputFields = m_selectedOption.getInputFields();
		m_inputPanel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = c.NORTHWEST;
		c.fill = c.NONE;
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 0;
		c.weighty = 0;
		c.insets = new Insets(5, 5, 5, 5);
		c.gridwidth = 2;
		m_inputPanel.add(new JLabel("<html>" + m_selectedOption.getDetailedInfo(m_language) + "</html>"), c);
		c.gridwidth = 1;
		c.gridy++;

		for (int i = 0; i < inputFields.size(); i++)
		{
			String[] field = (String[]) inputFields.elementAt(i);

			if (field[2].equalsIgnoreCase(m_language))
			{
				label = new JLabel("<html>" + field[1] + "</html>");
				//If the input field asks for credit card type we use a combobox
				//that displays all accepted cards instead of a simple text field
				if (field[0].equalsIgnoreCase("creditcardtype"))
				{
					String acceptedCards = m_paymentOptions.getAcceptedCreditCards();
					StringTokenizer st = new StringTokenizer(acceptedCards, ",");
					comboBox = new JComboBox();
					comboBox.setName(field[0]);
					while (st.hasMoreTokens())
					{
						comboBox.addItem(st.nextToken());
					}
					m_inputFields.addElement(comboBox);
					m_inputPanel.add(label, c);
					c.gridx++;
					m_inputPanel.add(comboBox, c);
				}
				else
				{
					textField = new JTextField(15);
					textField.setName(field[0]);
					m_inputFields.addElement(textField);
					m_inputPanel.add(label, c);
				c.gridx++;
					m_inputPanel.add(textField, c);
				}

				c.gridy++;
				c.gridx--;
			}
		}

		c.gridy++;
		c.weightx = 1;
		c.weightx = 1;
		m_sendingLabel = new JLabel(" ", GUIUtils.loadImageIcon(JAPConstants.BUSYFN, true), JLabel.CENTER);
		m_inputPanel.add(m_sendingLabel, c);
		m_sendingLabel.setVisible(false);
	}

	public void mouseClicked(MouseEvent e)
	{
		if (e.getSource() == m_extraInfoLabel)
		{
			AbstractOS os = AbstractOS.getInstance();
			String link = m_extraInfoLabel.getText();
			link = JAPUtil.replaceAll(link, "<br>", "");
			link = JAPUtil.replaceAll(link, "<p>", "");
			link = JAPUtil.replaceAll(link, "<html>", " ");
			link = JAPUtil.replaceAll(link, "</html>", " ");
			link = JAPUtil.replaceAll(link, "<font color=blue><u>", "");
			link = JAPUtil.replaceAll(link, "</u></font>", "");
			link = link.trim();

			LogHolder.log(LogLevel.DEBUG, LogType.PAY, "Opening " + link + " in browser.");
			try
			{
				os.openURL(new URL(link));
			}
			catch(MalformedURLException me)
			{
				LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, "Malformed URL");
			}
			m_host.setFinishEnabled(true);
		}
	}

	/**
	 * Submits information the user has entered for a passive
	 * payment to the payment instance
	 */
	public XMLPassivePayment getPassiveInfo()
			{
		/** Construct PassivePayment object */
		XMLPassivePayment pp = new XMLPassivePayment();
		pp.setTransferNumber(m_transCert.getTransferNumber());
		pp.setAmount(Util.parseFloat(m_amount));
		pp.setCurrency(m_currency);
		pp.setPaymentName(m_selectedOption.getName());
		Enumeration fields = m_inputFields.elements();
		while (fields.hasMoreElements())
		{
					Component comp = (Component) fields.nextElement();
					if (comp instanceof JTextField)
					{
						pp.addData( ( (JTextField) comp).getName(), ( (JTextField) comp).getText());
					}
					else if (comp instanceof JComboBox)
					{
						pp.addData( ( (JComboBox) comp).getName(),
								   (String) ( (JComboBox) comp).getSelectedItem());
					}
		}

		return pp;
	}

	/**
	 * Copies the extra payment info to the system clipboard
	 */
	private void copyExtraInfoToClipboard()
	{
		Clipboard sysClip = Toolkit.getDefaultToolkit().getSystemClipboard();
		String link = m_extraInfoLabel.getText();
		link = JAPUtil.replaceAll(link, "<br>", "");
		link = JAPUtil.replaceAll(link, "<p>", "");
		link = JAPUtil.replaceAll(link, "<html>", " ");
		link = JAPUtil.replaceAll(link, "</html>", " ");
		link = JAPUtil.replaceAll(link, "<font color=blue><u>", "");
		link = JAPUtil.replaceAll(link, "</u></font>", "");
		link = link.trim();

		Transferable transfer = new StringSelection(link);
		sysClip.setContents(transfer, null);
		m_host.setFinishEnabled(true);
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

	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == m_bttnCopy)
		{
			copyExtraInfoToClipboard();
		}
	}
}
