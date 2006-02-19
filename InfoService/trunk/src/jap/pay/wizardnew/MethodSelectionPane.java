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
package jap.pay.wizardnew;

import java.util.Enumeration;
import java.util.Vector;

import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.ButtonGroup;
import javax.swing.JComboBox;
import javax.swing.JLabel;
import javax.swing.JRadioButton;

import anon.pay.xml.XMLPaymentOption;
import anon.pay.xml.XMLPaymentOptions;
import gui.JAPMessages;
import gui.dialog.DialogContentPane;
import gui.dialog.DialogContentPane.IWizardSuitable;
import gui.dialog.JAPDialog;
import gui.dialog.WorkerContentPane;
import jap.JAPController;
import logging.LogType;

public class MethodSelectionPane extends DialogContentPane implements IWizardSuitable, ActionListener
{
	/** Messages */
	private static final String MSG_PRICE = MethodSelectionPane.class.
		getName() + "_price";
	private static final String MSG_SELECTOPTION = MethodSelectionPane.class.
		getName() + "_selectoption";
	private static final String MSG_ERRSELECT = MethodSelectionPane.class.
		getName() + "_errselect";

	private ButtonGroup m_rbGroup;
	private XMLPaymentOptions m_paymentOptions;
	private GridBagConstraints m_c = new GridBagConstraints();
	private XMLPaymentOption m_selectedPaymentOption;
	private JComboBox m_cbAmount;
	private JComboBox m_cbCurrency;
	private Container m_rootPanel;

	public MethodSelectionPane(JAPDialog a_parentDialog, DialogContentPane a_previousContentPane)
	{
		super(a_parentDialog, "",
			  new Layout(JAPMessages.getString(MSG_SELECTOPTION), MESSAGE_TYPE_PLAIN),
			  new Options(OPTION_TYPE_OK_CANCEL, a_previousContentPane));
		setDefaultButtonOperation(ON_CLICK_DISPOSE_DIALOG | ON_YESOK_SHOW_NEXT_CONTENT |
								  ON_NO_SHOW_PREVIOUS_CONTENT);

		m_rootPanel = this.getContentPane();
		m_c = new GridBagConstraints();
		m_rootPanel.setLayout(new GridBagLayout());
		m_rbGroup = new ButtonGroup();
		m_c = new GridBagConstraints();
		m_c.gridx = 0;
		m_c.gridy = 0;
		m_c.weightx = 0;
		m_c.weightx = 0;
		m_c.insets = new Insets(5, 5, 5, 5);
		m_c.anchor = m_c.NORTHWEST;
		m_c.fill = m_c.NONE;

		//Add some dummy options for dialog sizing
		for (int i = 0; i < 6; i++)
		{
			addOption("Dummy");
		}
		Vector dummyVector = new Vector();
		dummyVector.addElement("EUR");
		addCurrencies(dummyVector);
	}

	private void addOption(String a_name)
	{
		m_c.insets = new Insets(0, 5, 0, 5);
		m_c.gridy++;
		m_c.gridwidth = 3;
		JRadioButton rb = new JRadioButton("<html>" + a_name + "</html>");
		rb.setName(a_name);
		rb.addActionListener(this);
		m_rbGroup.add(rb);
		m_rootPanel.add(rb, m_c);
	}

	private void addCurrencies(Vector a_currencies)
	{
		m_c.insets = new Insets(5, 5, 5, 5);
		m_c.gridy++;
		m_c.gridwidth = 3;
		JLabel label = new JLabel(JAPMessages.getString(MSG_PRICE));
		m_rootPanel.add(label, m_c);
		m_c.gridy++;
		label = new JLabel(JAPMessages.getString("payAmount"));
		m_rootPanel.add(label, m_c);
		m_c.gridwidth = 1;
		m_cbAmount = new JComboBox(new String[]
								   {"1", "2", "3", "4", "5", "10", "15"});
		/*m_tfAmount = new JAPJIntField(new IntFieldBounds()
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
		   );*/
		m_c.gridy++;
		m_rootPanel.add(m_cbAmount, m_c);
		m_c.gridx++;
		m_c.weightx = 1;
		m_c.weighty = 1;
		m_cbCurrency = new JComboBox(a_currencies);
		m_rootPanel.add(m_cbCurrency, m_c);
	}

	public String getSelectedCurrency()
	{
		return (String) m_cbCurrency.getSelectedItem();
	}

	public String getAmount()
	{
		return (String) m_cbAmount.getSelectedItem();
		//return m_tfAmount.getText();
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

	public void showPaymentOptions()
	{
		m_rootPanel.removeAll();
		m_c = new GridBagConstraints();
		m_c.gridx = 0;
		m_c.gridy = 0;
		m_c.weightx = 0;
		m_c.weightx = 0;
		m_c.insets = new Insets(5, 5, 5, 5);
		m_c.anchor = m_c.NORTHWEST;
		m_c.fill = m_c.NONE;
		//Get fetched payment options
		WorkerContentPane p = (WorkerContentPane) getPreviousContentPane();
		Object value = p.getValue();
		XMLPaymentOptions options = (XMLPaymentOptions) value;
		m_paymentOptions = options;
		Enumeration headings = options.getOptionHeadings(JAPController.getLocale().
			getLanguage());
		while (headings.hasMoreElements())
		{
			addOption( (String) headings.nextElement());
		}
		addCurrencies(options.getCurrencies());
	}

	public CheckError[] checkYesOK()
	{
		CheckError[] error = new CheckError[1];

		if (m_selectedPaymentOption == null)
		{
			error[0] = new CheckError(JAPMessages.getString(MSG_ERRSELECT), LogType.PAY);
			return error;
		}

		return null;
	}

	public void resetSelection()
	{
		m_selectedPaymentOption = null;
	}

	public void setInitValue(Object a_object)
	{
		showPaymentOptions();
		resetSelection();
	}

}
