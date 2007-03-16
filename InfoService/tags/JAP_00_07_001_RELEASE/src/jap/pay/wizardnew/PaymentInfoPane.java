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

import java.net.MalformedURLException;
import java.net.URL;

import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;

import anon.pay.xml.XMLPaymentOption;
import anon.pay.xml.XMLTransCert;
import anon.util.Util;
import gui.JAPMessages;
import gui.dialog.DialogContentPane;
import gui.dialog.DialogContentPane.IWizardSuitable;
import gui.dialog.JAPDialog;
import gui.dialog.WorkerContentPane;
import jap.JAPController;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import platform.AbstractOS;

public class PaymentInfoPane extends DialogContentPane implements IWizardSuitable,
	ActionListener
{
	/** Messages */
	private static final String MSG_INFOS = PaymentInfoPane.class.
		getName() + "_infos";
	private static final String MSG_BUTTONCOPY = PaymentInfoPane.class.
		getName() + "_buttoncopy";
	private static final String MSG_BUTTONOPEN = PaymentInfoPane.class.
		getName() + "_buttonopen";

	private Container m_rootPanel;
	private GridBagConstraints m_c;
	private JButton m_bttnCopy, m_bttnOpen;
	private String m_language;
	private XMLPaymentOption m_selectedOption;
	private String m_strExtraInfo;

	public PaymentInfoPane(JAPDialog a_parentDialog, DialogContentPane a_previousContentPane)
	{
		super(a_parentDialog, "Dummy",
			  new Layout(JAPMessages.getString(MSG_INFOS), MESSAGE_TYPE_PLAIN),
			  new Options(OPTION_TYPE_OK_CANCEL, a_previousContentPane));
		setDefaultButtonOperation(ON_CLICK_DISPOSE_DIALOG | ON_YESOK_SHOW_NEXT_CONTENT |
								  ON_NO_SHOW_PREVIOUS_CONTENT);
		m_language = JAPController.getLocale().getLanguage();
		m_rootPanel = this.getContentPane();
		m_rootPanel.setLayout(new GridBagLayout());
		m_c = new GridBagConstraints();
		m_c.gridx = 0;
		m_c.gridy = 0;
		m_c.weightx = 0;
		m_c.weightx = 0;
		m_c.insets = new Insets(5, 5, 5, 5);
		m_c.anchor = GridBagConstraints.NORTHWEST;
		m_c.fill = GridBagConstraints.NONE;

		//Add some dummy labels for dialog sizing
		/*
		for (int i = 0; i < 12; i++)
		{
			m_rootPanel.add(new JLabel("..................................................."),
							m_c);
			m_c.gridy++;
		}*/

		getButtonCancel().setVisible(false);
	}

	public void showInfo()
	{
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
		m_c.anchor = GridBagConstraints.NORTHWEST;
		m_c.fill = GridBagConstraints.NONE;

		//setText(selectedOption.getDetailedInfo(m_language));


		m_strExtraInfo = selectedOption.getExtraInfo(m_language);
		if (m_strExtraInfo != null)
		{
			m_strExtraInfo = Util.replaceAll(m_strExtraInfo, "%t",
											 String.valueOf(transCert.getTransferNumber()));
			m_strExtraInfo = Util.replaceAll(m_strExtraInfo, "%a",
											 ( (MethodSelectionPane) getPreviousContentPane().
											   getPreviousContentPane()).getAmount());
			m_strExtraInfo = Util.replaceAll(m_strExtraInfo, "%c",
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

	/**
	 * Copies the extra payment info to the system clipboard
	 */
	private void copyExtraInfoToClipboard()
	{
		Clipboard sysClip = Toolkit.getDefaultToolkit().getSystemClipboard();
		String link = m_strExtraInfo;
		if (m_selectedOption.getExtraInfoType(m_language).equals(XMLPaymentOption.EXTRA_TEXT))
		{
			link = Util.replaceAll(link, "<br>", "\n");
			link = Util.replaceAll(link, "<p>", "\n\n");
		}
		else
		{
			link = Util.replaceAll(link, "<br>", "");
			link = Util.replaceAll(link, "<p>", "");
		}
		link = Util.replaceAll(link, "<html>", " ");
		link = Util.replaceAll(link, "</html>", " ");
		link = Util.replaceAll(link, "<font color=blue><u>", "");
		link = Util.replaceAll(link, "</u></font>", "");
		link = link.trim();

		Transferable transfer = new StringSelection(link);
		sysClip.setContents(transfer, null);
	}

	public void openURL()
	{
		AbstractOS os = AbstractOS.getInstance();
		String link = m_strExtraInfo;
		link = Util.replaceAll(link, "<br>", "");
		link = Util.replaceAll(link, "<p>", "");
		link = Util.replaceAll(link, "<html>", " ");
		link = Util.replaceAll(link, "</html>", " ");
		link = Util.replaceAll(link, "<font color=blue><u>", "");
		link = Util.replaceAll(link, "</u></font>", "");
		link = link.trim();

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

	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == m_bttnCopy)
		{
			copyExtraInfoToClipboard();
		}
		else if (e.getSource() == m_bttnOpen)
		{
			openURL();
		}
	}

	public CheckError[] checkUpdate()
	{
		showInfo();
		return null;
	}


}