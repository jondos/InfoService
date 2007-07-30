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
import java.awt.Cursor;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Toolkit;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JLabel;

import anon.pay.xml.XMLPaymentOption;
import anon.pay.xml.XMLTransCert;
import anon.util.Util;
import gui.JAPMessages;
import gui.dialog.DialogContentPane;
import gui.dialog.DialogContentPane.IWizardSuitable;
import gui.dialog.JAPDialog;
import gui.dialog.WorkerContentPane;
import jap.JAPController;
import jap.JAPUtil;
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
	public static final String MSG_PAYPAL_ITEM_NAME = PaymentInfoPane.class.getName() + "_paypalitemname";


	private Container m_rootPanel;
	private GridBagConstraints m_c;
	private JButton m_bttnCopy, m_bttnOpen;
	private String m_language;
	private XMLPaymentOption m_selectedOption;
	private String m_strExtraInfo;
	private XMLTransCert transCert;

	private String m_url;

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

	/**
	 * same as regular constructor, except it additionally takes a url
	 * which will be added as the last extraInfo of the selected paymentOption,
	 * and displayed prominently
	 *
	 * Use this for mixed payment options like paysafecard or call2pay,
	 * whenever you need to construct a URL to display that is dependent on the current transaction
	 * and therefor cannot be stored in the paymentoption itself
	 *
	 * @param a_parentDialog JAPDialog
	 * @param a_previousContentPane DialogContentPane
	 * @param a_url String
	 */
	public PaymentInfoPane(JAPDialog a_parentDialog, DialogContentPane a_previousContentPane, String a_url)
	{
		this(a_parentDialog,a_previousContentPane);
		m_url = a_url;
	}

	public static String createPaypalLink(String baseLink, long amount, String planName, String transferNumber)
	{
		String paypalCurrency = "EUR";


        String paypalAmount = amountAsString(amount);
		String localeLang = JAPMessages.getLocale().getLanguage();
		String paypalLang = localeLang.toUpperCase();
		String paypalItem = JAPMessages.getString(MSG_PAYPAL_ITEM_NAME) + "%20-%20" + planName; //URL-encode Spaces etc!!

		baseLink = Util.replaceAll(baseLink, "%t", transferNumber);
		baseLink = Util.replaceAll(baseLink, "%item%", paypalItem);
		baseLink = Util.replaceAll(baseLink, "%amount%", paypalAmount);
		baseLink = Util.replaceAll(baseLink, "%currency%", paypalCurrency);
		baseLink = Util.replaceAll(baseLink, "%lang%", paypalLang);

		return baseLink;
	}

	public static String createEgoldLink(String baseLink, long amount, String planName, String transferNumber)
	{
		//currently the same treatment as for paypal works, but uses a different method name
		//to make the dependency clear
		return createPaypalLink(baseLink,amount,planName,transferNumber);
	}

	private static String amountAsString(long amount)
	{

		//amountString: eurocent, e.g. "500", transform it into a format suitable for paypal
		String amountString = new Long(amount).toString();
		String amountWhole;
		String amountFractions;
		amountString.trim();
		if (amountString.length() == 1)
		{
			amountWhole = "0";
			amountFractions = "0" + amountString;
		}
		else if (amountString.length() < 3)
		{
			amountWhole = "0";
			amountFractions = amountString;
		}
		else
		{
			amountWhole = amountString.substring(0, amountString.length() - 2);
			amountFractions = amountString.substring(amountString.length() - 2, amountString.length());
		}
		String result = amountWhole + "%2e" + amountFractions;
		return result;
	}

	public void showInfo()
	{
		XMLPaymentOption selectedOption = ( (MethodSelectionPane) getPreviousContentPane().
										   getPreviousContentPane()).getSelectedPaymentOption();
	    transCert = (XMLTransCert) ( (WorkerContentPane) getPreviousContentPane()).getValue();
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


		m_strExtraInfo = selectedOption.getExtraInfo(m_language);
		boolean isURL = false;
		if (m_strExtraInfo != null)
		{

			DialogContentPane somePreviousPane = getPreviousContentPane();
			while (! (somePreviousPane instanceof VolumePlanSelectionPane))
			{
				somePreviousPane = somePreviousPane.getPreviousContentPane();
				//warning: will loop endlessly if no VolumePlanSelectionPane to be found
			}
			VolumePlanSelectionPane planPane = (VolumePlanSelectionPane) somePreviousPane;
			String amountString = planPane.getAmount();
			String planName = planPane.getSelectedVolumePlan().getName();
			int intAmount = Integer.parseInt(amountString);
			String tan = String.valueOf(transCert.getTransferNumber());

			//take special care of paypal links
			if (m_strExtraInfo.indexOf("paypal") != -1 )
			{
	            m_strExtraInfo = createPaypalLink(m_strExtraInfo,intAmount,planName,tan);
            }
			else if (m_strExtraInfo.indexOf("e-gold") != -1)
			{
				m_strExtraInfo = createEgoldLink(m_strExtraInfo,intAmount,planName,tan);
			}
			else
			{
				//regualar extra infos, e.g. instructions for wire transfer
				m_strExtraInfo = Util.replaceAll(m_strExtraInfo, "%t",tan);
				String amount = JAPUtil.formatEuroCentValue(intAmount);
				m_strExtraInfo = Util.replaceAll(m_strExtraInfo, "%a",amount );
				m_strExtraInfo = Util.replaceAll(m_strExtraInfo, "%c","");
			}


			m_c.gridy++;
			m_bttnCopy = new JButton(JAPMessages.getString(MSG_BUTTONCOPY));
			m_bttnCopy.addActionListener(this);
			m_rootPanel.add(m_bttnCopy, m_c);

			m_c.gridx++;
			m_bttnOpen = new JButton(JAPMessages.getString(MSG_BUTTONOPEN));
			m_bttnOpen.addActionListener(this);
			m_rootPanel.add(m_bttnOpen, m_c);
			m_bttnOpen.setVisible(false);

			isURL = selectedOption.getExtraInfoType(m_language).equalsIgnoreCase(XMLPaymentOption.EXTRA_LINK);
			if (isURL)
			{
				m_bttnOpen.setVisible(true);
				htmlExtraInfo = "<br> <font color=blue><u><b>" + m_strExtraInfo + "</b></u></font>";
			}
			else
			{
				m_bttnOpen.setVisible(false);
				htmlExtraInfo = "<p> <b>" + m_strExtraInfo + "</b> </p>";
			}
		}
        if (isURL)
		{
			setText(selectedOption.getDetailedInfo(m_language));// + htmlExtraInfo); //links should never be shown, only confuse the user
		}
		else
		{
			setText(selectedOption.getDetailedInfo(m_language) + htmlExtraInfo);
		}
		if (isURL) setMouseListener(new LinkMouseListener());

	}

	public XMLTransCert getTransCert()
	{
		return transCert;
	}

	/**
	 * Copies the extra payment info to the system clipboard
	 */
	private void copyExtraInfoToClipboard()
	{
		Clipboard sysClip = Toolkit.getDefaultToolkit().getSystemClipboard();
		String link = m_strExtraInfo;
		if (m_selectedOption.getExtraInfoType(m_language).equalsIgnoreCase(XMLPaymentOption.EXTRA_TEXT))
		{
			link = Util.replaceAll(link, "<br>", "\n");
			link = Util.replaceAll(link, "<p>", "\n\n");
			link = Util.replaceAll(link, "&nbsp;", " ");
		}
		else
		{
			link = Util.replaceAll(link, "<br>", "");
			link = Util.replaceAll(link, "<p>", "");
			link = Util.replaceAll(link, "&nbsp;", "%20");
			link = Util.replaceAll(link, " ", "%20");
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
		String link = m_strExtraInfo;

		if (!JAPController.getInstance().isAnonConnected() && JAPController.getInstance().getAnonMode())
		{
			/*
			 * JAP still tries to connect but fails... Switch of the anonymous connection so that
			 * the site may be opened in the browser window.
			 */
			JAPController.getInstance().stopAnonModeWait();
		}


		AbstractOS os = AbstractOS.getInstance();
		link = Util.replaceAll(link, "<br>", "");
		link = Util.replaceAll(link, "<p>", "");
		link = Util.replaceAll(link, "<html>", " ");
		link = Util.replaceAll(link, "</html>", " ");
		link = Util.replaceAll(link, "&nbsp;", "%20");
		link = Util.replaceAll(link, " ", "%20");
		link = Util.replaceAll(link, "<font color=blue><u>", "");
		link = Util.replaceAll(link, "</u></font>", "");
		link = link.trim();

		LogHolder.log(LogLevel.DEBUG, LogType.PAY, "Opening " + link + " in browser.");
		try
		{
			URL urlToOpen = new URL(link);
			os.openURL(urlToOpen);
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

class LinkMouseListener extends MouseAdapter
{
	public void mouseClicked(MouseEvent e)
	{
		try
		{
			//Warning: will fail if LinkMouseListener is added to a JComponent other than a JLabel
			JLabel source = (JLabel) e.getSource();
			String linkText = source.getText();
			URL linkUrl = new URL(linkText);
			AbstractOS.getInstance().openURL(linkUrl);
		} catch (ClassCastException cce)
		{
			LogHolder.log(LogLevel.ERR, LogType.PAY, "opening a link failed, reason: called on non-JLabel component");
		} catch (MalformedURLException mue)
		{
			LogHolder.log(LogLevel.ERR, LogType.PAY, "opening a link failed, reason: malformed URL");
		}
	}

	public void mouseEntered(MouseEvent e)
	{
		JComponent source = (JComponent) e.getSource();
		source.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
	}

	public void mouseExited(MouseEvent e)
	{
		JComponent source = (JComponent) e.getSource();
		source.setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
	}

}
