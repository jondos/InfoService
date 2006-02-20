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
package jap.pay.wizardnew;

import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Vector;

import java.awt.Component;
import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JTextField;

import anon.pay.xml.XMLPassivePayment;
import anon.pay.xml.XMLPaymentOption;
import anon.pay.xml.XMLPaymentOptions;
import anon.pay.xml.XMLTransCert;
import anon.util.Util;
import gui.JAPMessages;
import gui.dialog.DialogContentPane;
import gui.dialog.DialogContentPane.IWizardSuitable;
import gui.dialog.JAPDialog;
import gui.dialog.WorkerContentPane;
import jap.JAPController;
import logging.LogType;

public class PassivePaymentPane extends DialogContentPane implements IWizardSuitable
{
	/** Messages */
	private static final String MSG_ENTER = PassivePaymentPane.class.
		getName() + "_enter";
	private static final String MSG_ERRALLFIELDS = PassivePaymentPane.class.
		getName() + "_errallfields";

	private Container m_rootPanel;
	private GridBagConstraints m_c;
	private String m_language;
	private Vector m_inputFields;
	private XMLPaymentOption m_selectedOption;

	public PassivePaymentPane(JAPDialog a_parentDialog, DialogContentPane a_previousContentPane)
	{
		super(a_parentDialog, "",
			  new Layout(JAPMessages.getString(MSG_ENTER), MESSAGE_TYPE_PLAIN),
			  new Options(OPTION_TYPE_OK_CANCEL, a_previousContentPane));
		setDefaultButtonOperation(ON_CLICK_DISPOSE_DIALOG | ON_YESOK_SHOW_NEXT_CONTENT |
								  ON_NO_SHOW_PREVIOUS_CONTENT);
		m_language = JAPController.getLocale().getLanguage();
		m_rootPanel = this.getContentPane();
		m_c = new GridBagConstraints();
		m_rootPanel.setLayout(new GridBagLayout());
		m_c = new GridBagConstraints();
		m_c.gridx = 0;
		m_c.gridy = 0;
		m_c.weightx = 0;
		m_c.weightx = 0;
		m_c.insets = new Insets(5, 5, 5, 5);
		m_c.anchor = m_c.NORTHWEST;
		m_c.fill = m_c.NONE;

		//Add some dummy labels for dialog sizing
		for (int i = 0; i < 12; i++)
		{
			m_rootPanel.add(new JLabel("..................................................."),
							m_c);
			m_c.gridy++;
		}
	}

	public void showForm()
	{
		XMLPaymentOption selectedOption = ( (MethodSelectionPane) getPreviousContentPane().
										   getPreviousContentPane().
										   getPreviousContentPane()).getSelectedPaymentOption();
		m_selectedOption = selectedOption;
		XMLPaymentOptions paymentOptions = (XMLPaymentOptions) ( (WorkerContentPane) getPreviousContentPane().
			getPreviousContentPane().getPreviousContentPane().getPreviousContentPane()).getValue();

		m_rootPanel.removeAll();
		m_rootPanel = this.getContentPane();
		m_c = new GridBagConstraints();
		m_rootPanel.setLayout(new GridBagLayout());
		m_c = new GridBagConstraints();
		m_c.gridx = 0;
		m_c.gridy = 0;
		m_c.weightx = 0;
		m_c.weightx = 0;
		m_c.insets = new Insets(5, 5, 5, 5);
		m_c.anchor = m_c.NORTHWEST;
		m_c.fill = m_c.NONE;
		m_c.gridwidth = 2;
		JLabel label = new JLabel("<html>" + selectedOption.getDetailedInfo(m_language) + "</html>");
		m_rootPanel.add(label, m_c);
		m_c.gridwidth = 1;
		m_c.gridy++;

		JTextField textField = null;
		label = null;
		JComboBox comboBox = null;

		m_inputFields = new Vector();
		Vector inputFields = selectedOption.getInputFields();

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
					String acceptedCards = paymentOptions.getAcceptedCreditCards();
					StringTokenizer st = new StringTokenizer(acceptedCards, ",");
					comboBox = new JComboBox();
					comboBox.setName(field[0]);
					while (st.hasMoreTokens())
					{
						comboBox.addItem(st.nextToken());
					}
					m_inputFields.addElement(comboBox);
					m_rootPanel.add(label, m_c);
					m_c.gridx++;
					m_rootPanel.add(comboBox, m_c);
				}
				else
				{
					textField = new JTextField(15);
					textField.setName(field[0]);
					m_inputFields.addElement(textField);
					m_rootPanel.add(label, m_c);
					m_c.gridx++;
					m_rootPanel.add(textField, m_c);
				}

				m_c.gridy++;
				m_c.gridx = 0;
			}
		}
	}

	public XMLPassivePayment getEnteredInfo()
	{
		/** Construct PassivePayment object */
		XMLTransCert transCert = (XMLTransCert) ( (WorkerContentPane) getPreviousContentPane().
												 getPreviousContentPane()).
			getValue();
		String amount = ( (MethodSelectionPane) getPreviousContentPane().getPreviousContentPane().
						 getPreviousContentPane()).getAmount();
		String currency = ( (MethodSelectionPane) getPreviousContentPane().getPreviousContentPane().
						   getPreviousContentPane()).getSelectedCurrency();
		XMLPassivePayment pp = new XMLPassivePayment();
		pp.setTransferNumber(transCert.getTransferNumber());
		pp.setAmount(Util.parseFloat(amount));
		pp.setCurrency(currency);
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

	public CheckError[] checkYesOK()
	{
		CheckError error[] = new CheckError[1];
		if (m_selectedOption.getType().equals(XMLPaymentOption.OPTION_PASSIVE))
		{
			Enumeration e = m_inputFields.elements();
			while (e.hasMoreElements())
			{
				Component c = (Component) e.nextElement();
				if (c instanceof JTextField)
				{
					JTextField tf = (JTextField) c;
					if (tf.getText() == null || tf.getText().trim().equals(""))
					{
						error[0] = new CheckError(JAPMessages.getString(MSG_ERRALLFIELDS), LogType.PAY);
						return error;
					}
				}
			}
		}
		return null;
	}

	public CheckError[] checkUpdate()
	{
		showForm();
		return null;
	}

}
