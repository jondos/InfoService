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
package jap.pay;

/**
 * This class is the main payment view on JAP's main gui window
 *
 * @author Bastian Voigt, Tobias Bayer, Elmar Schraml
 * @version 1.0
 */
import java.sql.Timestamp;
import java.net.URL;

import java.awt.Dimension;
import java.awt.Component;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import anon.pay.AIControlChannel;
import anon.pay.PayAccount;
import anon.pay.PayAccountsFile;
import anon.pay.xml.XMLBalance;
import anon.pay.xml.XMLErrorMessage;
import anon.util.captcha.ICaptchaSender;
import anon.util.captcha.IImageEncodedCaptcha;
import anon.infoservice.MixCascade;
import logging.LogHolder;
import logging.LogLevel;
import gui.FlippingPanel;
import gui.GUIUtils;
import gui.JAPMessages;
import gui.JAPProgressBar;
import gui.dialog.JAPDialog;
import jap.JAPConf;
import jap.JAPConstants;
import jap.JAPController;
import jap.JAPNewView;
import jap.JAPUtil;
import jap.JAPModel;
import logging.LogType;
import java.util.Date;
import java.awt.Cursor;

public class PaymentMainPanel extends FlippingPanel
{
	private final long WARNING_AMOUNT = 25 * 1000; // 25 MB (db stores in Kbyte!)

	/** Messages */
	private static final String MSG_TITLE =
		AccountSettingsPanel.MSG_ACCOUNT_FLAT_VOLUME;
		//PaymentMainPanel.class.getName() + "_title";
	private static final String MSG_LASTUPDATE = PaymentMainPanel.class.getName() +
		"_lastupdate";
	private static final String MSG_PAYMENTNOTACTIVE = PaymentMainPanel.class.getName() +
		"_paymentnotactive";
	private static final String MSG_NEARLYEMPTY = PaymentMainPanel.class.getName() +
		"_nearlyempty";
	private static final String MSG_NEARLYEMPTY_CREATE_ACCOUNT = PaymentMainPanel.class.getName() +
		"_nearlyEmptyCreateAccount";
	private static final String MSG_SESSIONSPENT = PaymentMainPanel.class.getName() +
		"_sessionspent";
	private static final String MSG_TOTALSPENT = PaymentMainPanel.class.getName() +
		"_totalspent";
	private static final String MSG_NO_ACTIVE_ACCOUNT = PaymentMainPanel.class.getName() +
		"_noActiveAccount";
	private static final String MSG_ENABLE_AUTO_SWITCH = PaymentMainPanel.class.getName() +
		"_enableAutoSwitch";
	private static final String MSG_EXPERIMENTAL = PaymentMainPanel.class.getName() +
		"_experimental";
	private static final String MSG_TITLE_FLAT = PaymentMainPanel.class.getName() + "_title_flat";
	private static final String MSG_VALID_UNTIL =
		//PaymentMainPanel.class.getName() + "_valid_until";
		AccountSettingsPanel.MSG_ACCOUNT_VALID;
	private static final String MSG_EURO_BALANCE = PaymentMainPanel.class.getName() + "_euro_balance";
	private static final String MSG_NO_FLATRATE = PaymentMainPanel.class.getName() + "_no_flatrate";
	private static final String MSG_WANNA_CHARGE = PaymentMainPanel.class.getName() + "_wannaCharge";
	private static final String MSG_TT_CREATE_ACCOUNT = PaymentMainPanel.class.getName() + "_ttCreateAccount";
	private static final String MSG_FREE_OF_CHARGE = PaymentMainPanel.class.getName() + "_freeOfCharge";
	private static final String MSG_OPEN_TRANSACTION = PaymentMainPanel.class.getName() + "_openTransaction";




	private static final String[] MSG_PAYMENT_ERRORS = {"_xmlSuccess", "_xmlErrorInternal",
		"_xmlErrorWrongFormat", "_xmlErrorWrongData", "_xmlErrorKeyNotFound", "_xmlErrorBadSignature",
	"_xmlErrorBadRequest", "_xmlErrorNoAccountCert", "_xmlErrorNoBalance", "_xmlErrorNoConfirmation",
	"_accountempty", "_xmlErrorCascadeLength", "_xmlErrorDatabase", "_xmlErrorInsufficientBalance",
	"_xmlErrorNoFlatrateOffered", "_xmlErrorInvalidCode", "_xmlErrorInvalidCC", "_xmlErrorInvalidPriceCerts",
	"_xmlErrorMultipleLogin", "_xmlErrorNoRecordFound", "_xmlErrorPartialSuccess", "_xmlErrorBlocked"};

	static
	{
		for (int i = 0; i < MSG_PAYMENT_ERRORS.length; i++)
		{
			MSG_PAYMENT_ERRORS[i] = PaymentMainPanel.class.getName() + MSG_PAYMENT_ERRORS[i];
		}
	}

	/**
	 * Icons for the account icon display
	 */
	private ImageIcon[] m_accountIcons;

	/** shows the current balance state */
	private JAPProgressBar m_BalanceProgressBar;
	private JAPProgressBar m_BalanceSmallProgressBar;

	/** shows the current balance as text */
	private JLabel m_BalanceText, m_BalanceTextSmall;
	;

	/** show the date of the last balance update */
	private JLabel m_lastUpdateLabel;

	/** show the date of the last balance update */
	private JLabel m_dateLabel;

	/** the main jap window */
	private JAPNewView m_view;

	/** Listens to payment events */
	private MyPaymentListener m_MyPaymentListener = new MyPaymentListener();

	/** has user been notified about nearly empty accout? */
	private boolean m_notifiedEmpty = false;

	private boolean m_bShowingError = false;

	private JLabel m_labelTotalSpent;
	private JLabel m_labelSessionSpent;
	private JLabel m_labelTitle;
	private JLabel m_labelTitleSmall;
	private JLabel m_labelTotalSpentHeader;
	private JLabel m_labelSessionSpentHeader;

	private JLabel m_labelValidUntilHeader;
	private JLabel m_labelValidUntil;
	//private JLabel m_labelBalanceInEurosHeader;
	//private JLabel m_labelBalanceInEuros;

	private long m_spentThisSession;

	public PaymentMainPanel(final JAPNewView view)
	{
		super(view);
		m_view = view;

		loadIcons();
		JPanel fullPanel = new JPanel();
		fullPanel.setLayout(new GridBagLayout());
		GridBagConstraints c1 = new GridBagConstraints();
		m_labelTitle = new JLabel(JAPMessages.getString(MSG_TITLE));
		c1.insets = new Insets(0, 5, 0, 0);
		c1.anchor = GridBagConstraints.WEST;
		c1.weightx = 0;
		c1.fill = GridBagConstraints.HORIZONTAL;
		fullPanel.add(m_labelTitle, c1);
		JComponent spacer = new JPanel();
		Dimension spacerDimension = new Dimension(m_labelTitle.getFontMetrics(m_labelTitle.getFont()).
												  charWidth('9') * 6, 1);
		spacer.setPreferredSize(spacerDimension);
		c1.insets = new Insets(0, 0, 0, 0);
		c1.gridx = 1;
		c1.fill = GridBagConstraints.NONE;
		c1.weightx = 1;
		fullPanel.add(spacer, c1);
		m_BalanceText = new JLabel(" ");
		m_BalanceText.setHorizontalAlignment(JLabel.RIGHT);
		c1.insets = new Insets(0, 5, 0, 0);
		c1.weightx = 0;
		c1.fill = GridBagConstraints.HORIZONTAL;
		c1.gridx = 2;
		fullPanel.add(m_BalanceText, c1);
		JLabel label = new JLabel(" ", SwingConstants.RIGHT);
		c1.weightx = 0;
		c1.fill = GridBagConstraints.HORIZONTAL;
		c1.gridx = 3;
		c1.insets = new Insets(0, 10, 0, 0);
		fullPanel.add(label, c1);
		m_BalanceProgressBar = new JAPProgressBar();
		m_BalanceProgressBar.setMinimum(0);
		m_BalanceProgressBar.setMaximum(5);
		m_BalanceProgressBar.setBorderPainted(false);
		c1.gridx = 4;
		c1.weightx = 0;
		c1.fill = GridBagConstraints.NONE;
		c1.insets = new Insets(0, 5, 0, 0);
		fullPanel.add(m_BalanceProgressBar, c1);

		//Elmar: suggestion for improvement:
		//since all rows follow the same format, extract to a method like addRow(labelName)

		//row for validtime of flatrate
		m_labelValidUntilHeader = new JLabel(JAPMessages.getString(MSG_VALID_UNTIL));
		c1.insets = new Insets(10, 20, 0, 0);
		c1.gridx = 0;
		c1.gridy = 1;
		c1.anchor = GridBagConstraints.WEST;
		c1.weightx = 0;
		fullPanel.add(m_labelValidUntilHeader, c1);
		spacer = new JPanel();
		spacer.setPreferredSize(spacerDimension);
		c1.gridx = 1;
		c1.insets = new Insets(0, 0, 0, 0);
		c1.weightx = 1;
		c1.fill = GridBagConstraints.NONE;
		fullPanel.add(spacer, c1);
		m_labelValidUntil = new JLabel(" ");
		m_labelValidUntil.setHorizontalAlignment(JLabel.RIGHT);
		c1.insets = new Insets(10, 5, 0, 0);
		c1.gridx = 2;
		c1.fill = GridBagConstraints.HORIZONTAL;
		c1.weightx = 0;
		fullPanel.add(m_labelValidUntil, c1);

		//row for bytes_left of flatrate
		/*
		m_labelBalanceInEurosHeader = new JLabel(JAPMessages.getString(MSG_EURO_BALANCE));
		c1.insets = new Insets(10, 20, 0, 0);
		c1.gridx = 0;
		c1.gridy = 2;
		c1.anchor = GridBagConstraints.WEST;
		c1.weightx = 0;
		fullPanel.add(m_labelBalanceInEurosHeader, c1);
		spacer = new JPanel();
		spacer.setPreferredSize(spacerDimension);
		c1.gridx = 1;
		c1.insets = new Insets(0, 0, 0, 0);
		c1.weightx = 1;
		c1.fill = GridBagConstraints.NONE;
		fullPanel.add(spacer, c1);
		m_labelBalanceInEuros = new JLabel(" ");
		m_labelBalanceInEuros.setHorizontalAlignment(JLabel.RIGHT);
		c1.insets = new Insets(10, 5, 0, 0);
		c1.gridx = 2;
		c1.fill = GridBagConstraints.HORIZONTAL;
		c1.weightx = 0;
		fullPanel.add(m_labelBalanceInEuros, c1);
	  */



		//row for bytes spent in this session
		m_labelSessionSpentHeader = new JLabel(JAPMessages.getString(MSG_SESSIONSPENT));
		m_labelSessionSpentHeader.setVisible(false);
		c1.insets = new Insets(10, 20, 0, 0);
		c1.gridx = 0;
		c1.gridy = 3;
		c1.anchor = GridBagConstraints.WEST;
		c1.weightx = 0;
		fullPanel.add(m_labelSessionSpentHeader, c1);
		spacer = new JPanel();
		spacer.setPreferredSize(spacerDimension);
		c1.gridx = 1;
		c1.insets = new Insets(0, 0, 0, 0);
		c1.weightx = 1;
		c1.fill = GridBagConstraints.NONE;
		fullPanel.add(spacer, c1);
		m_labelSessionSpent = new JLabel(" ");
		m_labelSessionSpent.setVisible(false);
		m_labelSessionSpent.setHorizontalAlignment(JLabel.RIGHT);
		c1.insets = new Insets(10, 5, 0, 0);
		c1.gridx = 2;
		c1.fill = GridBagConstraints.HORIZONTAL;
		c1.weightx = 0;
		fullPanel.add(m_labelSessionSpent, c1);

		//row for total spent
		m_labelTotalSpentHeader = new JLabel(JAPMessages.getString(MSG_TOTALSPENT));
		c1.insets = new Insets(10, 20, 0, 0);
		c1.gridx = 0;
		c1.gridy = 4;
		c1.anchor = GridBagConstraints.WEST;
		c1.weightx = 0;
		fullPanel.add(m_labelTotalSpentHeader, c1);
		spacer = new JPanel();
		spacer.setPreferredSize(spacerDimension);
		c1.gridx = 1;
		c1.insets = new Insets(0, 0, 0, 0);
		c1.weightx = 1;
		c1.fill = GridBagConstraints.NONE;
		fullPanel.add(spacer, c1);
		m_labelTotalSpent = new JLabel(" ");
		m_labelTotalSpent.setHorizontalAlignment(JLabel.RIGHT);
		c1.insets = new Insets(10, 5, 0, 0);
		c1.gridx = 2;
		c1.fill = GridBagConstraints.HORIZONTAL;
		c1.weightx = 0;
		fullPanel.add(m_labelTotalSpent, c1);

		//row for date of last update
		m_dateLabel = new JLabel(JAPMessages.getString(MSG_LASTUPDATE));
		m_dateLabel.setVisible(false);
		c1.insets = new Insets(10, 20, 0, 0);
		c1.gridx = 0;
		c1.gridy = 5;
		c1.anchor = GridBagConstraints.WEST;
		c1.weightx = 0;
		fullPanel.add(m_dateLabel, c1);
		spacer = new JPanel();
		spacer.setPreferredSize(spacerDimension);
		c1.gridx = 1;
		c1.insets = new Insets(0, 0, 0, 0);
		c1.weightx = 1;
		c1.fill = GridBagConstraints.NONE;
		fullPanel.add(spacer, c1);
		m_lastUpdateLabel = new JLabel(" ");
		m_lastUpdateLabel.setVisible(false);
		m_lastUpdateLabel.setHorizontalAlignment(JLabel.RIGHT);
		c1.insets = new Insets(10, 5, 0, 0);
		c1.gridx = 2;
		c1.fill = GridBagConstraints.HORIZONTAL;
		c1.weightx = 0;
		fullPanel.add(m_lastUpdateLabel, c1);
		this.setFullPanel(fullPanel);

		//build small panel
		JPanel smallPanel = new JPanel();
		smallPanel.setLayout(new GridBagLayout());
		c1 = new GridBagConstraints();
		m_labelTitleSmall = new JLabel(JAPMessages.getString(MSG_TITLE));
		c1.insets = new Insets(0, 5, 0, 0);
		c1.anchor = GridBagConstraints.WEST;
		c1.weightx = 0;
		c1.fill = GridBagConstraints.HORIZONTAL;
		smallPanel.add(m_labelTitleSmall, c1);
		spacer = new JPanel();
		spacerDimension = new Dimension(label.getFontMetrics(label.getFont()).charWidth('9') * 6, 1);
		spacer.setPreferredSize(spacerDimension);
		c1.insets = new Insets(0, 0, 0, 0);
		c1.gridx = 1;
		c1.fill = GridBagConstraints.NONE;
		c1.weightx = 1;
		smallPanel.add(spacer, c1);
		c1.insets = new Insets(0, 5, 0, 0);
		c1.weightx = 0;
		c1.fill = GridBagConstraints.HORIZONTAL;
		c1.gridx = 2;
		m_BalanceTextSmall = new JLabel(" ");
		m_BalanceTextSmall.setHorizontalAlignment(JLabel.RIGHT);
		smallPanel.add(m_BalanceTextSmall, c1);
		label = new JLabel(" ", SwingConstants.RIGHT);
		c1.weightx = 0;
		c1.fill = GridBagConstraints.HORIZONTAL;
		c1.gridx = 3;
		c1.insets = new Insets(0, 10, 0, 0);
		smallPanel.add(label, c1);

		c1.gridx = 4;
		c1.weightx = 0;
		c1.fill = GridBagConstraints.NONE;
		c1.insets = new Insets(0, 5, 0, 0);
		m_BalanceSmallProgressBar = new JAPProgressBar();
		m_BalanceSmallProgressBar.setMinimum(0);
		m_BalanceSmallProgressBar.setMaximum(5);
		m_BalanceSmallProgressBar.setBorderPainted(false);

		smallPanel.add(m_BalanceSmallProgressBar, c1);
		this.setSmallPanel(smallPanel);

		MouseAdapter adapter = new MouseAdapter()
		{
			public void mouseClicked(MouseEvent a_event)
			{
				if (((JLabel)a_event.getSource()).getCursor() != Cursor.getDefaultCursor())
				{
					m_view.showConfigDialog(JAPConf.PAYMENT_TAB, new Boolean(true));
				}
			}
		};

		m_BalanceTextSmall.addMouseListener(adapter);
		m_BalanceText.addMouseListener(adapter);


		PayAccountsFile.getInstance().addPaymentListener(m_MyPaymentListener);
		// do not show nearly empty dialog, as this would freeze the start sequence
		updateDisplay(PayAccountsFile.getInstance().getActiveAccount(), false);
	}

	public static String translateBIError(XMLErrorMessage a_msg)
	{
		String error = JAPMessages.getString("aiErrorMessage"); // + "<br>";
		if (a_msg.getErrorCode() >= 0 && a_msg.getErrorCode() < MSG_PAYMENT_ERRORS.length)
		{
			error += JAPMessages.getString(MSG_PAYMENT_ERRORS[a_msg.getErrorCode()]);
		}
		else
		{
			error += a_msg.getErrorDescription();
		}
		return error;
	}

	/**
	 * This should be called by the changelistener whenever the state of the
	 * active account changes.
	 *
	 * @param activeAccount PayAccount
	 */
	private void updateDisplay(PayAccount activeAccount, boolean a_bWarnIfNearlyEmpty)
	{
		// payment disabled
		if (activeAccount == null)
		{
			m_BalanceTextSmall.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			m_BalanceText.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
			m_BalanceTextSmall.setToolTipText(JAPMessages.getString(MSG_TT_CREATE_ACCOUNT));
			m_BalanceText.setToolTipText(JAPMessages.getString(MSG_TT_CREATE_ACCOUNT));


			m_BalanceText.setText(JAPMessages.getString(MSG_PAYMENTNOTACTIVE));
			m_BalanceTextSmall.setText(JAPMessages.getString(MSG_PAYMENTNOTACTIVE));


			m_BalanceProgressBar.setValue(0);
			m_BalanceProgressBar.setEnabled(false);
			m_BalanceSmallProgressBar.setValue(0);
			m_BalanceSmallProgressBar.setEnabled(false);
		}

		// we got everything under control, situation normal
		else
		{
			m_BalanceTextSmall.setCursor(Cursor.getDefaultCursor());
			m_BalanceText.setCursor(Cursor.getDefaultCursor());
			m_BalanceTextSmall.setToolTipText(null);
			m_BalanceText.setToolTipText(null);

			XMLBalance balance = activeAccount.getBalance();
			if (balance != null)
			{
				//check for active flatrate
				boolean flatIsActive = false;
				Timestamp flatEnddate = balance.getFlatEnddate();
				Timestamp now = new Timestamp(new Date().getTime() );
				if (flatEnddate.after(now) )
				{
					flatIsActive = true;
				}
				//set flatrate-specific fields
				if (flatIsActive)
				{
					m_BalanceProgressBar.setEnabled(false);
					m_BalanceSmallProgressBar.setEnabled(false);
					m_BalanceText.setText(JAPUtil.formatBytesValue(balance.getCredit()*1000) );
					m_BalanceTextSmall.setText(JAPUtil.formatBytesValue(balance.getCredit()*1000) );
					String language = JAPMessages.getLocale().getLanguage();
					String endDateString = JAPUtil.formatTimestamp(balance.getFlatEnddate(),false,language);
					m_labelValidUntil.setText(endDateString);
					//m_labelBalanceInEuros.setText(JAPUtil.formatEuroCentValue(balance.getBalance()));

					long deposit = balance.getVolumeBytesLeft() * 1000 + balance.getSpent();
					long credit = activeAccount.getCertifiedCredit() * 1000;
					double percent = (double) credit / (double) deposit;
					if (percent > 0.83)
					{
						m_BalanceProgressBar.setValue(5);
						m_BalanceSmallProgressBar.setValue(5);
					}
					else if (percent > 0.66)
					{
						m_BalanceProgressBar.setValue(4);
						m_BalanceSmallProgressBar.setValue(4);
					}
					else if (percent > 0.49)
					{
						m_BalanceProgressBar.setValue(3);
						m_BalanceSmallProgressBar.setValue(3);
					}
					else if (percent > 0.32)
					{
						m_BalanceProgressBar.setValue(2);
						m_BalanceSmallProgressBar.setValue(2);
					}
					else if (credit > 0.15)
					{
						m_BalanceProgressBar.setValue(1);
						m_BalanceSmallProgressBar.setValue(1);
					}
					else
					{
						m_BalanceProgressBar.setValue(0);
						m_BalanceSmallProgressBar.setValue(0);
					}
					m_BalanceProgressBar.setEnabled(true);
					m_BalanceSmallProgressBar.setEnabled(true);
				}
				else
				{
					m_labelValidUntil.setText(JAPMessages.getString(AccountSettingsPanel.MSG_EXPIRED));
					//m_labelBalanceInEuros.setText(JAPUtil.formatEuroCentValue(balance.getBalance()));
					m_BalanceProgressBar.setValue(0);
					m_BalanceSmallProgressBar.setValue(0);

					m_BalanceText.setText(JAPUtil.formatBytesValue(0));
					m_BalanceTextSmall.setText(JAPUtil.formatBytesValue(0));
				}
				//set rest of the panel
				m_spentThisSession = AIControlChannel.getBytes();
				Timestamp t = balance.getTimestamp();
				m_lastUpdateLabel.setText(JAPUtil.formatTimestamp(t, true, JAPController.getInstance().getLocale().getLanguage()));

				m_labelSessionSpent.setText(JAPUtil.formatBytesValueWithUnit(m_spentThisSession));

				m_labelTotalSpent.setText(JAPUtil.formatBytesValueWithUnit(activeAccount.getSpent()));
				// account is nearly empty

				if (a_bWarnIfNearlyEmpty && //a_bWarnIfNearlyEmpty means warnings are not to be suppressed
					activeAccount.getCertifiedCredit() <= WARNING_AMOUNT && !m_notifiedEmpty &&
					activeAccount.getCertifiedCredit() != 0 &&
					PayAccountsFile.getInstance().getAlternativeNonEmptyAccount(
									   JAPController.getInstance().getCurrentMixCascade().getPIID()) == null)
				{
					m_notifiedEmpty = true;
					// start a new thread to prevent freezing of GUI update
					new Thread(new Runnable()
					{
						public void run()
						{
							if (JAPDialog.showYesNoDialog(JAPController.getInstance().getViewWindow(),
								JAPMessages.getString(MSG_NEARLYEMPTY_CREATE_ACCOUNT)))
							{
								m_view.showConfigDialog(JAPConf.PAYMENT_TAB,
									JAPController.getInstance().getCurrentMixCascade().getPIID());
							}
						}
					}).start();
				}

			}
		}
	}

	/**
	 * Notifies us when the state of the active account changes, so we can update
	 * the display
	 *
	 * @version 1.0
	 */
	private class MyPaymentListener implements anon.pay.IPaymentListener
	{
		/**
		 * accountActivated
		 *
		 * @param acc PayAccount
		 */
		public void accountActivated(PayAccount acc)
		{
			updateDisplay(acc, true);
		}

		/**
		 * accountAdded
		 *
		 * @param acc PayAccount
		 */
		public void accountAdded(PayAccount acc)
		{
		}

		/**
		 * accountRemoved
		 *
		 * @param acc PayAccount
		 */
		public void accountRemoved(PayAccount acc)
		{
		}

		/**
		 * creditChanged
		 *
		 * @param acc PayAccount
		 */
		public void creditChanged(PayAccount acc)
		{
			updateDisplay(acc, true);
		}

		/**
		 * accountCertRequested
		 *
		 * @param usingCurrentAccount boolean
		 */
		public boolean accountCertRequested(final MixCascade a_connectedCascade)
		{
			final PayAccountsFile accounts = PayAccountsFile.getInstance();
			boolean bSuccess = true;

			final JAPDialog.LinkedInformationAdapter adapter =
				new JAPDialog.AbstractLinkedURLAdapter()
			{
				public boolean isOnTop()
				{
					return true;
				}
				public URL getUrl()
				{
					try
					{
						if (JAPMessages.getLocale().getLanguage().equals("de"))
						{
							return new URL("http://www.jondos.de/de/payment");
						}
						else
						{
							return new URL("http://www.jondos.de/en/payment");
						}
					}
					catch (Exception a_e)
					{
						// ignore, should not happen
						return null;
					}
				}
			};
			Runnable run = null;
			final String strMessage = ""; //JAPMessages.getString(MSG_FREE_OF_CHARGE) + "<br><br>";

			if (accounts.getNumAccounts() == 0  ||
				(accounts.getActiveAccount() != null &&
				 !accounts.getActiveAccount().getBI().getId().equals(a_connectedCascade.getPIID())))
			{
				JAPController.getInstance().setAnonMode(false);
				bSuccess = false;

				run = new Runnable()
				{
					public void run()
					{
						boolean answer = JAPDialog.showYesNoDialog(
											  JAPController.getInstance().getViewWindow(),
							strMessage + JAPMessages.getString("payCreateAccountQuestion"), adapter);
						if (answer)
						{
							m_view.showConfigDialog(JAPConf.PAYMENT_TAB, a_connectedCascade.getPIID());
						}
					}
				};
			}
			else
			{
				if (accounts.getActiveAccount() == null)
				{
					JAPController.getInstance().setAnonMode(false);
					bSuccess = false;
					run = new Runnable()
					{
						public void run()
						{
							JAPDialog.showErrorDialog(JAPController.getInstance().getViewWindow(),
								strMessage + JAPMessages.getString(MSG_NO_ACTIVE_ACCOUNT), LogType.PAY, adapter);
							m_view.showConfigDialog(JAPConf.PAYMENT_TAB, null);
						}
					};
				}

				else if (!accounts.getActiveAccount().isCharged(new Timestamp(System.currentTimeMillis())))
				{
					JAPController.getInstance().setAnonMode(false);
					bSuccess = false;

					run = new Runnable()
					{
						public void run()
						{
							String message = strMessage +
								JAPMessages.getString(MSG_PAYMENT_ERRORS[XMLErrorMessage.ERR_ACCOUNT_EMPTY]) +
								" ";

							if (accounts.getActiveAccount().getBalance().getSpent() <= 0)
							{
								message += JAPMessages.getString(MSG_OPEN_TRANSACTION);
							}
							else
							{
								message += //JAPMessages.getString(MSG_WANNA_CHARGE);
									JAPMessages.getString("payCreateAccountQuestion");
							}
							JAPController.getInstance().setAnonMode(false);
							if (JAPDialog.showYesNoDialog(JAPController.getInstance().getViewWindow(),
								message, adapter))
							{
								m_view.showConfigDialog(JAPConf.PAYMENT_TAB,
									//PayAccountsFile.getInstance().getActiveAccount());
									new Boolean(true));
							}
						}
					};
				}
				else if (!JAPController.getInstance().getDontAskPayment())
				{
					JAPDialog.LinkedCheckBox checkBox = new JAPDialog.LinkedCheckBox(false);

					int ret = JAPDialog.showConfirmDialog(JAPController.getInstance().getViewWindow(),
						strMessage + JAPMessages.getString("payUseAccountQuestion"), // + "<br><br>" +
						//"<Font color=\"red\">" + JAPMessages.getString(MSG_EXPERIMENTAL) + "</Font>",
						JAPDialog.OPTION_TYPE_OK_CANCEL, JAPDialog.MESSAGE_TYPE_INFORMATION,
						checkBox);
					JAPController.getInstance().setDontAskPayment(checkBox.getState());

					if (ret != JAPDialog.RETURN_VALUE_OK)
					{
						JAPController.getInstance().setAnonMode(false);
						bSuccess = false;
					}
				}
			}
			if (run != null)
			{
				if (JAPDialog.isConsoleOnly())
				{
					run.run();
				}
				else
				{
					SwingUtilities.invokeLater(run);
				}
			}
			return bSuccess;
			/*
			if (accounts.getActiveAccount() != null)
			{
				accounts.getActiveAccount().updated();
			}*/

		}

		/**
		 * accountError
		 *
		 * @param msg XMLErrorMessage
		 */
		public void accountError(XMLErrorMessage a_msg, boolean a_bIgnore)
		{
			if (a_msg.getErrorCode() <= XMLErrorMessage.ERR_OK || a_msg.getErrorCode() < 0)
			{
				// no error
				return;
			}

			MixCascade cascade = JAPController.getInstance().getCurrentMixCascade();
			if (cascade.equals(JAPController.getInstance().switchToNextMixCascade()))
			{
				// there are no other cascades to switch to
				LogHolder.log(LogLevel.WARNING, LogType.NET, "There are no other cascades to choose!");
				for (int i = 0; i < 5 &&
					 (JAPController.getInstance().getAnonMode() ||
					  JAPController.getInstance().isAnonConnected()); i++)
				{
					// does not work well as of auto-reconnection
					JAPController.getInstance().setAnonMode(false);
					try
					{
						Thread.sleep(200);
					}
					catch (InterruptedException ex)
					{
						break;
					}
				}
			}

			if (!m_bShowingError && !a_bIgnore)
			{
				m_bShowingError = true;
				final XMLErrorMessage msg = a_msg;
				new Thread(new Runnable()
				{
					public void run()
					{
						String message = //JAPMessages.getString(MSG_FREE_OF_CHARGE) + "<br><br>" +
							translateBIError(msg);
						Component parent = PaymentMainPanel.this;
						JAPDialog.LinkedInformationAdapter adapter = new JAPDialog.LinkedInformationAdapter()
						{
							public boolean isOnTop()
							{
								return true;
							}
						};

						if (!GUIUtils.getParentWindow(parent).isVisible())
						{
							parent = JAPController.getInstance().getViewWindow();
						}

						if (msg.getErrorCode() == XMLErrorMessage.ERR_ACCOUNT_EMPTY)
						{
							message += "<br><br>" +
								//JAPMessages.getString(MSG_WANNA_CHARGE);
								JAPMessages.getString("payCreateAccountQuestion");

							if (JAPDialog.showYesNoDialog(parent, message, adapter))
							{
								new Thread(new Runnable()
								{
									public void run()
									{
										m_view.showConfigDialog(JAPConf.PAYMENT_TAB,
											//PayAccountsFile.getInstance().getActiveAccount());
											new Boolean(true));
									}
								}).start();
							}
						}
						else if (!JAPModel.getInstance().isCascadeAutoSwitched())
						{
							message += "<br><br>" + JAPMessages.getString(MSG_ENABLE_AUTO_SWITCH);
							if (JAPDialog.showYesNoDialog(parent, message, adapter))
							{
								JAPModel.getInstance().setCascadeAutoSwitch(true);
							}
						}
						else
						{
							JAPDialog.showErrorDialog(parent, message, LogType.PAY, adapter);
						}
						m_bShowingError = false;
					}
				}).start();
			}
		}

		public void gotCaptcha(ICaptchaSender a_source, final IImageEncodedCaptcha a_captcha)
		{
		}
	}

	/**
	 * Loads some icons for the account display
	 */
	private void loadIcons()
	{
		// Load Images for Account Icon Display
		m_accountIcons = new ImageIcon[JAPConstants.ACCOUNTICONFNARRAY.length];
		for (int i = 0; i < JAPConstants.ACCOUNTICONFNARRAY.length; i++)
		{
			m_accountIcons[i] = GUIUtils.loadImageIcon(JAPConstants.ACCOUNTICONFNARRAY[i], false);
		}
	}
}
