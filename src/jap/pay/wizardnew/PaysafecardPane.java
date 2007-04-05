/*
 Copyright (c) 2000-2006, The JAP-Team
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
import java.awt.event.ActionListener;
import gui.dialog.DialogContentPane.IWizardSuitable;
import gui.dialog.WorkerContentPane;
import gui.dialog.JAPDialog;
import gui.JAPMessages;
import java.awt.Container;
import jap.JAPController;
import java.awt.event.ActionEvent;
import logging.LogLevel;
import logging.LogHolder;
import platform.AbstractOS;
import java.net.MalformedURLException;
import java.net.URL;
import anon.util.Util;
import logging.LogType;
import java.awt.Toolkit;
import anon.pay.xml.XMLPaymentOption;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.Transferable;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import javax.swing.JButton;
import anon.pay.xml.XMLTransCert;

/**
 * To be used in the account-charging wizard
 * Tells the user how to complete his payment using a Paysafecard,
 * by directing him to the relevant webpage
 * (amount and method were already selected on the MethodSelectionPane)
 *
 * customized version, and therefor subclass, of PaymentInfoPane
 *
 * ********************* UNFINISHED, WORK ABANDONED********************
 * **********************CURRENTLY NOT IN USE ******************
 *
 * @author Elmar Schraml
 */
public class PaysafecardPane extends PaymentInfoPane
{

	private String m_paysafecardLink;


	public PaysafecardPane(JAPDialog a_parentDialog, WorkerContentPane a_previousContentPane)
	{
		super(a_parentDialog,a_previousContentPane);

	}


	public void openURL()
	{
		AbstractOS os = AbstractOS.getInstance();
		String link = m_paysafecardLink;
		LogHolder.log(LogLevel.DEBUG, LogType.PAY, "Opening " + link + " in browser.");
		try
		{
			os.openURL(new URL(link));
		}
		catch (MalformedURLException me)
		{
			LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, "Malformed URL");
		}

	}

	private void copyLinkToClipboard()
	{
		Clipboard sysClip = Toolkit.getDefaultToolkit().getSystemClipboard();
		String link = m_paysafecardLink;
		Transferable transfer = new StringSelection(link);
		sysClip.setContents(transfer, null);
	}

	public void showInfo(){
		XMLPaymentOption selectedOption = ( (MethodSelectionPane) getPreviousContentPane().
										   getPreviousContentPane()).getSelectedPaymentOption();
		XMLTransCert transCert = (XMLTransCert) ( (WorkerContentPane) getPreviousContentPane()).
			getValue();
		String htmlExtraInfo = "";
		m_selectedOption = selectedOption;
		m_rootPanel.removeAll();
		m_rootPanel = this.getContentPane();
		m_rootPanel.setLayout(new GridBagLayout());
		m_c = new GridBagConstraints();
		m_c.gridx = 0;
		m_c.gridy = 0;
		m_c.weightx = 0;
		m_c.weightx = 0;
		m_c.insets = new Insets(5, 5, 5, 5);
		m_c.anchor = m_c.NORTHWEST;
		m_c.fill = m_c.NONE;


		String strExtraInfo = selectedOption.getExtraInfo(m_language);
		if (m_strExtraInfo != null)
		{
			strExtraInfo = Util.replaceAll(strExtraInfo, "%t",
											 String.valueOf(transCert.getTransferNumber()));
			strExtraInfo = Util.replaceAll(strExtraInfo, "%a",
											 ( (MethodSelectionPane) getPreviousContentPane().
											   getPreviousContentPane()).getAmount());
			strExtraInfo = Util.replaceAll(m_strExtraInfo, "%c",
										   ( (MethodSelectionPane) getPreviousContentPane().
											getPreviousContentPane()).
										   getSelectedCurrency());

			m_c.gridy++;
			m_bttnCopy = new JButton(JAPMessages.getString(MSG_BUTTONCOPY));
			m_bttnCopy.addActionListener(this);
			m_rootPanel.add(m_bttnCopy, m_c);

			m_c.gridx++;
			m_bttnOpen = new JButton(JAPMessages.getString(MSG_BUTTONOPEN));
			m_bttnOpen.addActionListener(this);
			m_rootPanel.add(m_bttnOpen, m_c);
			m_bttnOpen.setVisible(false);


			if (selectedOption.getExtraInfoType(m_language).equalsIgnoreCase(XMLPaymentOption.EXTRA_LINK))
			{
				/** @todo Make label clickable */
				m_bttnOpen.setVisible(true);
				htmlExtraInfo = "<br> <font color=blue><u><b>" + m_strExtraInfo + "</b></u></font>";
			}
			else
			{
				m_bttnOpen.setVisible(false);
				htmlExtraInfo = "<br> <b>" + m_strExtraInfo + "</b>";
			}
		}

		setText(selectedOption.getDetailedInfo(m_language) + htmlExtraInfo);




	}




}
