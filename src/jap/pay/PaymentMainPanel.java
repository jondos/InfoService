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
 * @author Bastian Voigt, Tobias Bayer
 * @version 1.0
 */
import java.sql.Timestamp;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.MediaTracker;
import javax.swing.ImageIcon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;

import anon.pay.AIControlChannel;
import anon.pay.PayAccount;
import anon.pay.PayAccountsFile;
import anon.pay.xml.XMLBalance;
import anon.pay.xml.XMLErrorMessage;
import anon.util.captcha.ICaptchaSender;
import anon.util.captcha.IImageEncodedCaptcha;
import gui.FlippingPanel;
import gui.GUIUtils;
import gui.JAPMessages;
import gui.MyProgressBarUI;
import gui.dialog.JAPDialog;
import jap.JAPConf;
import jap.JAPConstants;
import jap.JAPController;
import jap.JAPModel;
import jap.JAPNewView;
import jap.JAPUtil;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

public class PaymentMainPanel extends FlippingPanel
{

	/** Messages */
	private static final String MSG_TITLE = PaymentMainPanel.class.getName() +
		"_title";
	private static final String MSG_LASTUPDATE = PaymentMainPanel.class.getName() +
		"_lastupdate";
	private static final String MSG_PAYMENTNOTACTIVE = PaymentMainPanel.class.getName() +
		"_paymentnotactive";
	private static final String MSG_NEARLYEMPTY = PaymentMainPanel.class.getName() +
		"_nearlyempty";
	private static final String MSG_SESSIONSPENT = PaymentMainPanel.class.getName() +
		"_sessionspent";
	private static final String MSG_TOTALSPENT = PaymentMainPanel.class.getName() +
		"_totalspent";
	private static final String MSG_NO_ACTIVE_ACCOUNT = PaymentMainPanel.class.getName() +
		"_noActiveAccount";
	private static final String[] MSG_PAYMENT_ERRORS = {"_xmlSuccess", "_xmlErrorInternal",
		"_xmlErrorWrongFormat", "_xmlErrorWrongData", "_xmlErrorKeyNotFound", "_xmlErrorBadSignature",
	"_xmlErrorBadRequest", "_xmlErrorNoAccountCert", "_xmlErrorNoBalance", "_xmlErrorNoConfirmation",
	"_accountempty"};

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
	private JProgressBar m_BalanceProgressBar;
	private JProgressBar m_BalanceSmallProgressBar;

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
		m_BalanceProgressBar = new JProgressBar();
		MyProgressBarUI ui = new MyProgressBarUI(true);
		ui.setFilledBarColor(Color.blue);
		m_BalanceProgressBar.setUI(ui);
		m_BalanceProgressBar.setMinimum(0);
		m_BalanceProgressBar.setMaximum(5);
		m_BalanceProgressBar.setBorderPainted(false);
		c1.gridx = 4;
		c1.weightx = 0;
		c1.fill = GridBagConstraints.NONE;
		c1.insets = new Insets(0, 5, 0, 0);
		fullPanel.add(m_BalanceProgressBar, c1);

		m_labelSessionSpentHeader = new JLabel(JAPMessages.getString(MSG_SESSIONSPENT));
		c1.insets = new Insets(10, 20, 0, 0);
		c1.gridx = 0;
		c1.gridy = 1;
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
		m_labelSessionSpent.setHorizontalAlignment(JLabel.RIGHT);
		c1.insets = new Insets(10, 5, 0, 0);
		c1.gridx = 2;
		c1.fill = GridBagConstraints.HORIZONTAL;
		c1.weightx = 0;
		fullPanel.add(m_labelSessionSpent, c1);

		m_labelTotalSpentHeader = new JLabel(JAPMessages.getString(MSG_TOTALSPENT));
		c1.insets = new Insets(10, 20, 0, 0);
		c1.gridx = 0;
		c1.gridy = 2;
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

		m_dateLabel = new JLabel(JAPMessages.getString(MSG_LASTUPDATE));
		c1.insets = new Insets(10, 20, 0, 0);
		c1.gridx = 0;
		c1.gridy = 3;
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
		m_lastUpdateLabel.setHorizontalAlignment(JLabel.RIGHT);
		c1.insets = new Insets(10, 5, 0, 0);
		c1.gridx = 2;
		c1.fill = GridBagConstraints.HORIZONTAL;
		c1.weightx = 0;
		fullPanel.add(m_lastUpdateLabel, c1);
		this.setFullPanel(fullPanel);

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
		m_BalanceSmallProgressBar = new JProgressBar();
		MyProgressBarUI uiSmall = new MyProgressBarUI(true);
		uiSmall.setFilledBarColor(Color.blue);
		m_BalanceSmallProgressBar.setUI(uiSmall);
		m_BalanceSmallProgressBar.setMinimum(0);
		m_BalanceSmallProgressBar.setMaximum(5);
		m_BalanceSmallProgressBar.setBorderPainted(false);

		smallPanel.add(m_BalanceSmallProgressBar, c1);

		this.setSmallPanel(smallPanel);

		PayAccountsFile.getInstance().addPaymentListener(m_MyPaymentListener);
		updateDisplay(PayAccountsFile.getInstance().getActiveAccount());
	}

	/**
	 * This should be called by the changelistener whenever the state of the
	 * active account changes.
	 *
	 * @param activeAccount PayAccount
	 */
	private void updateDisplay(PayAccount activeAccount)
	{
		// payment disabled
		if (activeAccount == null)
		{
			m_BalanceText.setText(JAPMessages.getString(MSG_PAYMENTNOTACTIVE));
			m_BalanceTextSmall.setText(JAPMessages.getString(MSG_PAYMENTNOTACTIVE));
			m_BalanceText.setEnabled(false);
			m_BalanceTextSmall.setEnabled(false);

			m_BalanceProgressBar.setValue(0);
			m_BalanceProgressBar.setEnabled(false);
			m_BalanceSmallProgressBar.setValue(0);
			m_BalanceSmallProgressBar.setEnabled(false);
		}

		// we got everything under control, situation normal
		else
		{
			XMLBalance balance = activeAccount.getBalance();
			if (balance != null)
			{
				m_spentThisSession = AIControlChannel.getBytes();
				Timestamp t = balance.getTimestamp();
				m_lastUpdateLabel.setText(JAPUtil.formatTimestamp(t, true, JAPController.getInstance().getLocale().getLanguage()));
				m_BalanceText.setEnabled(true);
				if (activeAccount.getCertifiedCredit() < 0 )
				{
					m_BalanceText.setText(JAPUtil.formatBytesValue(0));
					m_BalanceTextSmall.setText(JAPUtil.formatBytesValue(0));
				}
				else
				{
				m_BalanceText.setText(JAPUtil.formatBytesValue(activeAccount.getCertifiedCredit()));
				m_BalanceTextSmall.setText(JAPUtil.formatBytesValue(activeAccount.getCertifiedCredit()));
				}
				m_BalanceTextSmall.setEnabled(true);
				m_labelSessionSpent.setText(JAPUtil.formatBytesValue(m_spentThisSession));
				double deposit = (double) activeAccount.getDeposit();
				double credit = (double) activeAccount.getCertifiedCredit();
				double percent = credit / deposit;
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

				m_labelTotalSpent.setText(JAPUtil.formatBytesValue(activeAccount.getSpent()));
				// account is nearly empty
				if (activeAccount.getCertifiedCredit() <= (1024 * 1024) && !m_notifiedEmpty &&
					activeAccount.getCertifiedCredit() != 0)
				{
					JAPDialog.showMessageDialog(JAPController.getView(),
												JAPMessages.getString(MSG_NEARLYEMPTY));
					m_notifiedEmpty = true;
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
			updateDisplay(acc);
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
			updateDisplay(acc);
		}

		/**
		 * accountCertRequested
		 *
		 * @param usingCurrentAccount boolean
		 */
		public void accountCertRequested(boolean blubb)
		{
			PayAccountsFile accounts = PayAccountsFile.getInstance();
			if (accounts.getNumAccounts() == 0)
			{
				JAPController.getInstance().setAnonMode(false);

				SwingUtilities.invokeLater(new Thread()
				{
					public void run()
					{
						boolean answer = JAPDialog.showYesNoDialog(JAPController.getView(),
							JAPMessages.getString("payCreateAccountQuestion"));
						if (answer)
						{
							m_view.showConfigDialog(JAPConf.PAYMENT_TAB);
						}
					}
				});
			}
			else
			{
				if (accounts.getActiveAccount() == null)
				{
					JAPController.getInstance().setAnonMode(false);
					SwingUtilities.invokeLater(new Thread()
					{
						public void run()
						{
							JAPDialog.showErrorDialog(JAPController.getView(),
								JAPMessages.getString(MSG_NO_ACTIVE_ACCOUNT), LogType.PAY);
						}
					});
				}
				else if (!JAPController.getInstance().getDontAskPayment())
				{
					JAPDialog.LinkedCheckBox checkBox = new JAPDialog.LinkedCheckBox(false);

					int ret = JAPDialog.showConfirmDialog(JAPController.getView(),
						JAPMessages.getString("payUseAccountQuestion"),
						JAPDialog.OPTION_TYPE_OK_CANCEL, JAPDialog.MESSAGE_TYPE_INFORMATION,
						checkBox);
					JAPController.getInstance().setDontAskPayment(checkBox.getState());

					if (ret != JAPDialog.RETURN_VALUE_OK)
					{
						JAPController.getInstance().setAnonMode(false);
					}
				}
			}
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
		public void accountError(XMLErrorMessage msg)
		{
			String error;
			if (msg.getErrorCode() <= XMLErrorMessage.ERR_OK || msg.getErrorCode() < 0)
			{
				// no error
				return;
			}
			if (msg.getErrorCode() < MSG_PAYMENT_ERRORS.length)
			{
				 error = JAPMessages.getString(MSG_PAYMENT_ERRORS[msg.getErrorCode()]);
			}
			else
			{
				 error = msg.getErrorDescription();
			}
			if (!m_bShowingError)
			{
				m_bShowingError = true;
				JAPDialog.showErrorDialog(PaymentMainPanel.this,
										  JAPMessages.getString("aiErrorMessage") + " " + error,
										  LogType.PAY);
				m_bShowingError = false;
			}
		}

		public void gotCaptcha(ICaptchaSender a_source, final IImageEncodedCaptcha a_captcha)
		{
		}
	}

	/**
	 * Loads some icons for the account display
	 */
	protected void loadIcons()
	{
		// Load Images for Account Icon Display
		m_accountIcons = new ImageIcon[JAPConstants.ACCOUNTICONFNARRAY.length];
		if (!JAPModel.isSmallDisplay())
		{
			for (int i = 0; i < JAPConstants.ACCOUNTICONFNARRAY.length; i++)
			{
				m_accountIcons[i] = GUIUtils.loadImageIcon(JAPConstants.ACCOUNTICONFNARRAY[i], false);
			}
		}
		else // scale down for small displays
		{
			MediaTracker m = new MediaTracker(this);
			for (int i = 0; i < JAPConstants.ACCOUNTICONFNARRAY.length; i++)
			{
				Image tmp = GUIUtils.loadImageIcon(JAPConstants.ACCOUNTICONFNARRAY[i], true).getImage();
				int w = tmp.getWidth(null);
				tmp = tmp.getScaledInstance( (int) (w * 0.75), -1, Image.SCALE_SMOOTH);
				m.addImage(tmp, i);
				m_accountIcons[i] = new ImageIcon(tmp);
			}
			try
			{
				m.waitForAll();
			}
			catch (Exception e)
			{
				LogHolder.log(LogLevel.DEBUG, LogType.PAY, e);
			}
		}
	}

	public void localeChanged()
	{
		m_labelTitle.setText(JAPMessages.getString(MSG_TITLE));
		m_labelTitleSmall.setText(JAPMessages.getString(MSG_TITLE));
		m_labelTotalSpentHeader.setText(JAPMessages.getString(MSG_TOTALSPENT));
		m_labelSessionSpentHeader.setText(JAPMessages.getString(MSG_SESSIONSPENT));
		m_dateLabel.setText(JAPMessages.getString(MSG_LASTUPDATE));
		Timestamp t = new Timestamp(System.currentTimeMillis());
		if (PayAccountsFile.getInstance().getNumAccounts() > 0)
		{
			t = PayAccountsFile.getInstance().getActiveAccount().getBalance().getTimestamp();
		}

		m_lastUpdateLabel.setText(JAPUtil.formatTimestamp(t, true, JAPController.getInstance().getLocale().getLanguage()));
	}
}
