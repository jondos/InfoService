/*
 Copyright (c) 2000-2007, The JAP-Team
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

import gui.dialog.DialogContentPane;
import java.awt.GridBagConstraints;
import java.awt.Font;
import java.awt.Container;
import anon.pay.xml.XMLVolumePlan;
import anon.pay.xml.XMLVolumePlans;
import gui.dialog.WorkerContentPane;
import gui.dialog.JAPDialog;
import gui.JAPMessages;
import java.awt.GridBagLayout;
import javax.swing.JRadioButton;
import java.awt.event.ActionEvent;
import java.awt.Insets;
import javax.swing.JLabel;
import java.awt.event.ActionListener;
import javax.swing.ButtonGroup;
import jap.JAPUtil;
import gui.dialog.DialogContentPane.IWizardSuitable;
import java.sql.Timestamp;
import jap.JAPController;
import logging.LogType;
import gui.GUIUtils;
import javax.swing.JTextField;


/**
 * Shows all available volume plans (as sent by the jpi in the form of a XMLVolumePlans object),
 * and allows the user to pick one.
 * Part of the wizard for charging an account.
 *
 * @author Elmar Schraml
 */
public class VolumePlanSelectionPane extends DialogContentPane implements  IWizardSuitable,ActionListener
{
	/* Messages */
	private static final String MSG_PRICE = VolumePlanSelectionPane.class.
		getName() + "_price";
	private static final String MSG_HEADING = VolumePlanSelectionPane.class.
		getName() + "_heading";
	private static final String MSG_VOLUME = VolumePlanSelectionPane.class.
		getName() + "_volume";
	private static final String MSG_UNLIMITED = VolumePlanSelectionPane.class.
		getName() + "_unlimited";
	private static final String MSG_ERROR_NO_PLAN_CHOSEN = VolumePlanSelectionPane.class.
		getName() + "_errorNoPlanChosen";
	private static final String MSG_VALIDUNTIL = VolumePlanSelectionPane.class.
		getName() + "_validuntil";
	private static final String MSG_VOLUMEPLAN = VolumePlanSelectionPane.class.
		getName() + "_volumeplan";
	private static final String MSG_CHOOSEAPLAN = VolumePlanSelectionPane.class.
		getName() + "_chooseaplan";
	private static final String MSG_ENTER_COUPON = VolumePlanSelectionPane.class.getName() + "_entercouponcode";

	private XMLVolumePlans m_allPlans;
	private XMLVolumePlan m_selectedPlan;
	private JTextField m_couponCode;
	private GridBagConstraints m_c = new GridBagConstraints();
	private Container m_rootPanel;
	private ButtonGroup m_rbGroup;
	private WorkerContentPane m_fetchPlansPane;

	public VolumePlanSelectionPane(JAPDialog a_parentDialog, WorkerContentPane a_previousContentPane)
	{
		super(a_parentDialog, JAPMessages.getString(MSG_CHOOSEAPLAN),
			  new Layout(JAPMessages.getString(MSG_HEADING), MESSAGE_TYPE_PLAIN),
			  new Options(OPTION_TYPE_OK_CANCEL, a_previousContentPane));
		setDefaultButtonOperation(ON_CLICK_DISPOSE_DIALOG | ON_YESOK_SHOW_NEXT_CONTENT |
								  ON_NO_SHOW_PREVIOUS_CONTENT);
		m_fetchPlansPane = a_previousContentPane;
		m_rbGroup = new ButtonGroup();
		m_rootPanel = this.getContentPane();
		m_c = new GridBagConstraints();
		m_rootPanel.setLayout(new GridBagLayout());
		m_c.gridx = 0;
		m_c.gridy = 0;
		m_c.weightx = 0;
		m_c.weightx = 0;
		m_c.insets = new Insets(5, 5, 5, 5);
		m_c.anchor = m_c.NORTHWEST;
		m_c.fill = m_c.NONE;

		//show some dummy plans
		//(real plans are shown by showVolumePlans(), called by checkUpdate()
		// (heaven knows why immediately showing them here in the constructor doesnt work)

		for (int i = 0; i < 10; i++)
		{
			XMLVolumePlan dummyPlan = new XMLVolumePlan("Dummy        for sizing",100,2,"months",2000000);
			addPlan(dummyPlan);
		}
	}

	public XMLVolumePlan getSelectedVolumePlan()
	{
		return m_selectedPlan;
	}

	public String getEnteredCouponCode()
	{
		return m_couponCode.getText();
	}

	public boolean isCouponUsed()
	{
		if (m_couponCode.getText().equals("") ) //no coupon entered
		{
			return false;
		} else
		{
			return true;
		}
	}

	/**
	 * returns the amount (= price) of the currently selected plan
	 * for reasons of backwards compatibility (amount used to be a field of MethodSelectionPane)
	 * as a String
	 *
	 * @return String: e.g. "500" for 500 Eurocent
	 */
	public String getAmount()
	{
		int amount = m_selectedPlan.getPrice();
		Integer foo = new Integer(amount);
		String bar = foo.toString();
		return bar;
	}

	/**
	 * get Currency of the selected volume Plan
	 *
	 * @return String: currently always "EUR"
	 * @todo: handle this more gracefully, e.g. convert into currency of the user's country
	 */
	public String getCurrency()
	{
		return new String("EUR");
	}

	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() instanceof JRadioButton)
		{
			JRadioButton clickedButton = (JRadioButton) e.getSource();
			String name = clickedButton.getName();
			m_selectedPlan = m_allPlans.getVolumePlan(name);
		}
	}

	private void addPlan(XMLVolumePlan aPlan)
	{
		m_c.insets = new Insets(0, 5, 0, 5);
		m_c.gridy++;
		//m_c.gridwidth = 3;

		String a_name = aPlan.getName();
		// @todo show user's currency

		m_c.gridx = 0;
		JRadioButton rb = new JRadioButton(a_name);
		rb.setName(a_name);
		rb.addActionListener(this);
		m_rbGroup.add(rb);
		m_rootPanel.add(rb, m_c);
		m_c.gridx++;
		m_rootPanel.add(new JLabel(JAPUtil.formatEuroCentValue(aPlan.getPrice())), m_c);
		m_c.gridx++;
		if (aPlan.isDurationLimited() )
		{
			Timestamp endDate = JAPUtil.getEnddate(aPlan.getDuration(),aPlan.getDurationUnit() );
			String lang = JAPController.getLocale().getLanguage();
			m_rootPanel.add(new JLabel(JAPUtil.formatTimestamp(endDate,false,lang)), m_c);
		}
		else
		{
			m_rootPanel.add(new JLabel(JAPMessages.getString(MSG_UNLIMITED)), m_c);
		}

		m_c.gridx++;
		if (aPlan.isVolumeLimited() )
		{
			m_rootPanel.add(new JLabel(JAPUtil.formatBytesValueWithUnit(aPlan.getVolumeKbytes()*1000)), m_c);
		}
		else
		{
			m_rootPanel.add(new JLabel(JAPMessages.getString(MSG_UNLIMITED)), m_c);
		}
	}

	private void addCouponField()
	{
		m_c.gridy++;
		m_c.insets = new Insets(10, 5, 0, 5);
		m_c.gridx = 1;
		m_c.gridwidth = 3;
		m_rootPanel.add(new JLabel(JAPMessages.getString(MSG_ENTER_COUPON)),m_c);
		m_c.gridy++;
		m_c.gridx = 1;
		m_c.gridwidth = 3;
		m_couponCode = new JTextField(15);
		m_rootPanel.add(m_couponCode,m_c);

	}

	public CheckError[] checkYesOK()
	{
		CheckError[] errors = super.checkYesOK();
		if ((errors == null || errors.length == 0) && m_rbGroup.getSelection() == null && !isCouponUsed() )
		{
			errors = new CheckError[]{
				new CheckError(JAPMessages.getString(MSG_ERROR_NO_PLAN_CHOSEN), LogType.GUI)};
		}

		return errors;
	}


	public CheckError[] checkUpdate()
	{
		showVolumePlans();
		resetSelection();
		return null;
	}

	public void showVolumePlans()
	{
		//Get fetched volume plans
		WorkerContentPane p = m_fetchPlansPane;
		Object value = p.getValue();
		XMLVolumePlans allPlans = (XMLVolumePlans) value;
		JLabel label;
		m_allPlans = allPlans;

		m_rootPanel.removeAll();
		m_c = new GridBagConstraints();
		m_c.gridx = 0;
		m_c.gridy = 0;
		m_c.weightx = 0;
		m_c.weightx = 0;
		m_c.insets = new Insets(5, 5, 5, 5);
		m_c.anchor = m_c.NORTHWEST;
		m_c.fill = m_c.NONE;


		m_c.gridx++;
		label = new JLabel(JAPMessages.getString(MSG_PRICE));
		GUIUtils.setFontStyle(label, Font.BOLD);
		m_rootPanel.add(label, m_c);
		m_c.gridx++;
		label = new JLabel(JAPMessages.getString(MSG_VALIDUNTIL));
		GUIUtils.setFontStyle(label, Font.BOLD);
		m_rootPanel.add(label, m_c);
		m_c.gridx++;
		label = new JLabel(JAPMessages.getString(MSG_VOLUME));
		GUIUtils.setFontStyle(label, Font.BOLD);
		m_rootPanel.add(label, m_c);

		//show plans
		m_c.gridy++;
		for (int i = 0; i < m_allPlans.getNrOfPlans(); i++)
		{
			addPlan(m_allPlans.getVolumePlan(i));
		}

	    //show coupon field
		addCouponField();
	}

	public void resetSelection()
	{
		m_selectedPlan = null;
		m_couponCode.setText("");
	}
}
