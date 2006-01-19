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
import java.text.SimpleDateFormat;
import java.util.Enumeration;

import java.awt.Color;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Image;
import java.awt.Insets;
import java.awt.MediaTracker;
import javax.swing.ImageIcon;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import anon.pay.BIConnection;
import anon.pay.PayAccount;
import anon.pay.PayAccountsFile;
import anon.pay.xml.XMLBalance;
import anon.pay.xml.XMLErrorMessage;
import anon.util.captcha.IImageEncodedCaptcha;
import gui.CaptchaDialog;
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

public class PaymentMainPanel extends JPanel
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

	/**
	 * Icons for the account icon display
	 */
	private ImageIcon[] m_accountIcons;

	/** shows different coin icons */
	private JLabel m_AccountIconLabel;

	/** shows the current balance state */
	private JProgressBar m_BalanceProgressBar;

	/** shows the current balance as text */
	private JLabel m_BalanceText;

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

	public PaymentMainPanel(final JAPNewView view)
	{
		super(null);
		m_view = view;

		loadIcons();
		GridBagLayout l = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		this.setLayout(l);

		// the date of last update
		JLabel label = new JLabel(JAPMessages.getString(MSG_TITLE));
		//m_AccountText.setBorder(new EtchedBorder());
		c.anchor = GridBagConstraints.NORTHWEST;
		c.fill = c.NONE; //GridBagConstraints.HORIZONTAL;
		c.gridx = 1;
		c.gridy = 0;
		c.gridwidth = 3;
		c.weighty = 0;
		c.weightx = 1;
		c.insets = new Insets(0, 5, 0, 0);
		this.add(label, c);

		m_dateLabel = new JLabel(JAPMessages.getString(MSG_LASTUPDATE));
		c.insets = new Insets(10, 10, 10, 10);
		c.weightx = 0;
		c.gridy++;
		c.gridwidth = 1;
		this.add(m_dateLabel, c);

		c.gridx++;
		m_lastUpdateLabel = new JLabel();
		this.add(m_lastUpdateLabel, c);

		// the current balance (progressbar + label)
		MyProgressBarUI progressUi = new MyProgressBarUI(false);
		progressUi.setFilledBarColor(Color.blue);
		m_BalanceProgressBar = new JProgressBar(0, 100000000);
		m_BalanceProgressBar.setUI(progressUi);
		m_BalanceProgressBar.setValue(0);
		m_BalanceProgressBar.setBorderPainted(false);
		c.anchor = GridBagConstraints.NORTHWEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridwidth = 1;
		c.gridx = 2;
		c.gridy++;
		c.insets = new Insets(10, 10, 10, 0);
		this.add(m_BalanceProgressBar, c);
		m_BalanceText = new JLabel(" ");
		c.gridx = 1;
		c.weightx = 1;
		this.add(m_BalanceText, c);

		// the icon label in the middle
		m_AccountIconLabel = new JLabel(m_accountIcons[1]);
		c.anchor = GridBagConstraints.NORTHEAST;
		c.fill = GridBagConstraints.VERTICAL;
		c.weighty = 1;
		c.weightx = 0;
		c.gridheight = 2;
		c.gridx = 3;
		c.gridy = 0;
		//this.add(m_AccountIconLabel, c);

		// the JButton on the right
		/*	m_ConfigButton = new JButton(JAPMessages.getString("ngPaymentCharge"));
		c.insets.left = 0;
		c.insets.right = 5;
		c.gridheight = 2;
		c.gridx = 4;
		c.gridy = 0;
		c.anchor = GridBagConstraints.EAST;
		c.fill = GridBagConstraints.NONE;
		this.add(m_ConfigButton, c);
		m_ConfigButton.addActionListener(new ActionListener()
		{
			public void actionPerformed(ActionEvent e)
			{
				view.showConfigDialog(JAPConf.PAYMENT_TAB);
			}
		 });*/

		PayAccountsFile.getInstance().addPaymentListener(m_MyPaymentListener);
		updateDisplay(null);
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
			m_AccountIconLabel.setIcon(m_accountIcons[0]);
			m_dateLabel.setText(JAPMessages.getString(MSG_PAYMENTNOTACTIVE));
			m_dateLabel.setEnabled(false);
			m_BalanceProgressBar.setValue(0);
			m_BalanceProgressBar.setEnabled(false);
		}

		// we got everything under control, situation normal
		else
		{
			XMLBalance balance = activeAccount.getBalance();
			if (balance != null)
			{
				Timestamp t = balance.getTimestamp();
			SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm");
			String dateText = sdf.format(t);
			m_lastUpdateLabel.setText(dateText);
			m_dateLabel.setText(JAPMessages.getString(MSG_LASTUPDATE));
			m_dateLabel.setEnabled(true);
			m_AccountIconLabel.setIcon(m_accountIcons[1]);
			m_BalanceText.setEnabled(true);
			m_BalanceText.setText(JAPUtil.formatBytesValue(activeAccount.getCertifiedCredit()));
				m_BalanceProgressBar.setMaximum( (int) (activeAccount.getDeposit() / 100));
				m_BalanceProgressBar.setValue( (int) (activeAccount.getCertifiedCredit() / 100));
			m_BalanceProgressBar.setEnabled(true);

				// account is nearly empty
				if (activeAccount.getCertifiedCredit() <= (1024 * 1024) && !m_notifiedEmpty)
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
			int option = 0;
			PayAccountsFile accounts = PayAccountsFile.getInstance();
			if (accounts.getNumAccounts() == 0)
			{
				if (JOptionPane.showOptionDialog(
					PaymentMainPanel.this,
					"<html>" + JAPMessages.getString("payCreateAccountQuestion") + "</html>",
					JAPMessages.getString("ngPaymentTabTitle"), JOptionPane.YES_NO_OPTION,
					JOptionPane.QUESTION_MESSAGE, null, null, null) == JOptionPane.YES_OPTION)
				{
					JAPController.getInstance().setAnonMode(false);
					m_view.showConfigDialog(JAPConf.PAYMENT_TAB);
				}
				else
				{
					JAPController.getInstance().setAnonMode(false);
				}
			}
			else
			{
				if (accounts.getActiveAccount() != null)
				{
					option = JOptionPane.showConfirmDialog(
						PaymentMainPanel.this,
						"<html>" + JAPMessages.getString("payUseAccountQuestion") + "</html>",
						JAPMessages.getString("ngPaymentTabTitle"), JOptionPane.YES_NO_OPTION,
						JOptionPane.QUESTION_MESSAGE);
				}
				else
				{
					if (accounts.getNumAccounts() == 1)
					{
						/** @todo i18n*/
						option = JOptionPane.showConfirmDialog(
							PaymentMainPanel.this,
							"The mixcascade you are currently using wants to be payed. " +
							"You have created an account, however it is not marked as " +
							"active. Jap will now automatically activate this account " +
							"for you. ",
							JAPMessages.getString("ngPaymentTabTitle"), JOptionPane.YES_NO_OPTION,
							JOptionPane.QUESTION_MESSAGE
							);
						Enumeration enumE = accounts.getAccounts();
						accounts.setActiveAccount( (PayAccount) enumE.nextElement());
					}
					else
					{
						if (JOptionPane.showOptionDialog(
							PaymentMainPanel.this,
							"The mixcascade you are currently using wants to be payed. " +
							"You must activate an account to allow Jap using it for payment. " +
							"Would you like to choose an active account now?",
							JAPMessages.getString("ngPaymentTabTitle"),
							JOptionPane.YES_NO_OPTION, JOptionPane.INFORMATION_MESSAGE,
							null, null, null) == JOptionPane.YES_OPTION)
						{
							m_view.showConfigDialog(JAPConf.PAYMENT_TAB);
						}
						else
						{
							option = JOptionPane.NO_OPTION;
					}
				}
				}
			}

			if (option == JOptionPane.NO_OPTION)
			{
				JAPController.getInstance().setAnonMode(false);
			}
			if (accounts.getActiveAccount() != null)
			{
				accounts.getActiveAccount().updated();
			}
		}

		/**
		 * accountError
		 *
		 * @param msg XMLErrorMessage
		 */
		public void accountError(XMLErrorMessage msg)
		{
			JAPDialog.showErrorDialog(PaymentMainPanel.this,
				JAPMessages.getString("aiErrorMessage") + msg.getErrorDescription(),
									  LogType.PAY);
		}

		/**
		 * The BIConnection got a captcha. Let the user solve it and set the
		 * solution to the BIConnection.
		 * @param a_source Object
		 * @param a_captcha IImageEncodedCaptcha
		 */
		public void gotCaptcha(Object a_source, final IImageEncodedCaptcha a_captcha)
		{
			CaptchaDialog c = new CaptchaDialog(a_captcha, "<Cha", JAPConf.getInstance().getView());
			( (BIConnection) a_source).setCaptchaSolution(c.getSolution());
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
}
