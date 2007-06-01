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
import java.util.StringTokenizer;
import java.util.Hashtable;

import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.ButtonGroup;
import javax.swing.JRadioButton;

import anon.pay.xml.XMLPaymentOption;
import anon.pay.xml.XMLPaymentOptions;
import gui.JAPMessages;
import gui.dialog.DialogContentPane;
import gui.dialog.DialogContentPane.IWizardSuitable;
import gui.dialog.JAPDialog;
import gui.dialog.WorkerContentPane;
import jap.JAPConstants;
import jap.JAPController;
import logging.LogType;
import anon.pay.xml.XMLVolumePlan;
import jap.JAPUtil;
import javax.swing.JLabel;
import gui.JAPHtmlMultiLineLabel;

public class MethodSelectionPane extends DialogContentPane implements IWizardSuitable, ActionListener
{
	//if the markup in percent for a certain payment method is greate than this value, it will be shown behind the option to warn the user
	//(to disable showing markups, set this to any value over 100)
	private static final int SHOW_MARKUP_IF_ABOVE = 5;

	/** Messages */
	private static final String MSG_PRICE = MethodSelectionPane.class.
		getName() + "_price";
	private static final String MSG_SELECTOPTION = MethodSelectionPane.class.
		getName() + "_selectoption";
	private static final String MSG_ERRSELECT = MethodSelectionPane.class.
		getName() + "_errselect";
	private static final String MSG_NOTSUPPORTED = MethodSelectionPane.class.
		getName() + "_notsupported";
	private static final String MSG_SELECTED_PLAN = MethodSelectionPane.class.getName() + "_selectedplan";
	private static final String MSG_MARKUP = MethodSelectionPane.class.getName() + "_markup";
	private static final String MSG_MARKUP_CAPTION = MethodSelectionPane.class.getName() + "_markupcaption";

	private ButtonGroup m_rbGroup;
	private XMLPaymentOptions m_paymentOptions;
	private GridBagConstraints m_c = new GridBagConstraints();
	private XMLPaymentOption m_selectedPaymentOption;
	private Container m_rootPanel;

	public MethodSelectionPane(JAPDialog a_parentDialog, WorkerContentPane a_previousContentPane)
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
			addOption("Dummy","0");
		}
	}


	private void addOption(String a_name, String markup)
	{
		m_c.insets = new Insets(0, 5, 0, 5);
		m_c.gridy++;
		JRadioButton rb = new JRadioButton(new gui.JAPHtmlMultiLineLabel(a_name).getHTMLDocumentText() + markup);
		rb.setName(a_name);
		rb.addActionListener(this);
		m_rbGroup.add(rb);
		m_rootPanel.add(rb, m_c);
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
		WorkerContentPane previousContentPane = (WorkerContentPane) getPreviousContentPane();
		Object value = previousContentPane.getValue();
		XMLPaymentOptions options = (XMLPaymentOptions) value;

		m_paymentOptions = options;
		String language = JAPController.getLocale().getLanguage();
		Enumeration headings = options.getOptionHeadings(language);
		Hashtable usedOptions = new Hashtable(); // holds type and option
		/*
		 * Add non-genric options that work with this JAP version and all generic options that have not
		 * been replaced by non-generic options.
		 */
		XMLPaymentOption currentOption;
		String optionName;
		while (headings.hasMoreElements())
		{
			String heading = (String) headings.nextElement();

			currentOption = m_paymentOptions.getOption(heading, JAPController.getLocale().getLanguage());
			if (currentOption.worksWithJapVersion(JAPConstants.aktVersion))
			{
				optionName = currentOption.getName();
				/** @todo Change the name of this option to CreditCard in BI!!!! */
				if (optionName.equals("CreditCardOld"))
				{
					optionName = "CreditCard";
				}
				if (usedOptions.containsKey(optionName) &&
					!currentOption.isNewer((XMLPaymentOption)usedOptions.get(optionName)))
				{
					continue;
				}
				usedOptions.put(optionName, currentOption);
			}
		}
		Enumeration enumUsedOptions = usedOptions.elements();
		XMLPaymentOption curOption;
		String curLang;
		String curMarkup;
		while (enumUsedOptions.hasMoreElements())
		{
			curOption = (XMLPaymentOption)enumUsedOptions.nextElement();
			curLang = JAPController.getLocale().getLanguage();
			int markup = curOption.getMarkup();
			if (markup > SHOW_MARKUP_IF_ABOVE)
			{
				/*
				String markupTerm = JAPMessages.getString(MSG_MARKUP);
				curMarkup = " ("+markup+"% " + markupTerm + "! )";
				*/
			   curMarkup = " *";
			}
			else
			{
				curMarkup = "";
			}
			addOption(curOption.getHeading(curLang),curMarkup );
		}

	    //show explanation about most expensive payment methods
		String markupText = JAPMessages.getString(MSG_MARKUP_CAPTION);
		JAPHtmlMultiLineLabel markupLabel = new JAPHtmlMultiLineLabel(markupText);
		//JLabel markupLabel = new JLabel(markupText);
		m_c.insets = new Insets(30,5,0,5);
		m_c.gridy++;
		m_c.weightx = 1.0;
		m_c.fill = GridBagConstraints.HORIZONTAL;
		m_rootPanel.add(markupLabel,m_c);

		//show details of the volume plan selected (so the user is aware of how much he'll have to pay)
		DialogContentPane curPane = previousContentPane;
		XMLVolumePlan selectedPlan;
		while (true) //go back through previous panes until finding the VolumePlanSelectionPane
		{
			if (curPane instanceof VolumePlanSelectionPane)
			{
				VolumePlanSelectionPane vpsp = (VolumePlanSelectionPane) curPane;
				selectedPlan = vpsp.getSelectedVolumePlan();
				break;
			}
			curPane = curPane.getPreviousContentPane();
        }
	    String planName = selectedPlan.getName();
		String planPrice = JAPUtil.formatEuroCentValue(selectedPlan.getPrice());
	    JLabel planHeading = new JLabel(JAPMessages.getString(MSG_SELECTED_PLAN));
		JLabel planDetails = new JLabel(planName + ", " + planPrice);
		m_c.insets = new Insets(30, 5, 0, 5);
    	m_c.gridy++;
		m_rootPanel.add(planHeading, m_c);
		m_c.insets = new Insets(5, 5, 0, 5);
	    m_c.gridy++;
		m_rootPanel.add(planDetails, m_c);
	}

	public CheckError[] checkYesOK()
	{
		CheckError[] error = new CheckError[1];
		boolean supported = false;

		if (m_selectedPaymentOption == null)
		{
			error[0] = new CheckError(JAPMessages.getString(MSG_ERRSELECT), LogType.PAY);
			return error;
		}
		else if (!m_selectedPaymentOption.isGeneric())
		{
			StringTokenizer st = new StringTokenizer(JAPConstants.PAYMENT_NONGENERIC, ",");
			while (st.hasMoreTokens())
			{
				String supportedOption = st.nextToken();
				if (m_selectedPaymentOption.getName().equalsIgnoreCase(supportedOption))
				{
					supported = true;
				}
			}
			if (!supported)
			{
				error[0] = new CheckError(JAPMessages.getString(MSG_NOTSUPPORTED), LogType.PAY);
				return error;
			}
		}

		return null;
	}

	public void resetSelection()
	{
		m_selectedPaymentOption = null;
	}

	public CheckError[] checkUpdate()
	{
		showPaymentOptions();
		resetSelection();
		return null;
	}

}
