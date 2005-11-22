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
 * @author Bastian Voigt
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
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import anon.pay.PayAccount;
import anon.pay.PayAccountsFile;
import anon.pay.xml.XMLErrorMessage;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import jap.*;
import gui.*;

public class PaymentMainPanel extends JPanel
{
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

	/** shows the account number and the timestamp of last update */
	private JLabel m_AccountText;

	/** this button opens the configuration tab for payment */
	private JButton m_ConfigButton;

	/** the main jap window */
	private JAPNewView m_view;

	private MyPaymentListener m_MyPaymentListener = new MyPaymentListener();

	public PaymentMainPanel(final JAPNewView view)
	{
		super(null);
		m_view = view;

		loadIcons();
		GridBagLayout l = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		this.setLayout(l);

		// the date of last update
		m_AccountText = new JLabel("LastUpdate");
		//m_AccountText.setBorder(new EtchedBorder());
		c.anchor = GridBagConstraints.NORTHWEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 1;
		c.gridy = 0;
		c.gridwidth = 2;
		c.weighty = 0;
		c.weightx = 1;
		c.insets = new Insets(0, 5, 0, 0);
		this.add(m_AccountText, c);

		// the current balance (progressbar + label)
		m_BalanceProgressBar = new JProgressBar(0, 100);
		m_BalanceProgressBar.setValue(77);
		c.anchor = GridBagConstraints.SOUTHWEST;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.weightx = 1;
		c.gridwidth = 1;
		c.gridx = 1;
		c.gridy = 1;
		c.insets = new Insets(0, 5, 0, 0);
		this.add(m_BalanceProgressBar, c);
		m_BalanceText = new JLabel("Balance");
		c.anchor = GridBagConstraints.SOUTHWEST;
		c.gridx = 2;
		this.add(m_BalanceText, c);

		// the icon label in the middle
		m_AccountIconLabel = new JLabel(m_accountIcons[1]);
		c.anchor = GridBagConstraints.CENTER;
		c.fill = GridBagConstraints.VERTICAL;
		c.weighty = 1;
		c.gridheight = 2;
		c.gridx = 3;
		c.gridy = 0;
		this.add(m_AccountIconLabel, c);

		// the JButton on the right
		m_ConfigButton = new JButton(JAPMessages.getString("ngPaymentCharge"));
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
		});

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
			m_AccountText.setText(JAPMessages.getString("ngPaymentDisabled"));
			m_AccountIconLabel.setIcon(m_accountIcons[0]);
			m_BalanceText.setText("");
			m_BalanceText.setEnabled(false);
			m_BalanceProgressBar.setValue(0);
			m_BalanceProgressBar.setEnabled(false);
		}

		// account is nearly empty :-(
		else if ( (activeAccount.getCertifiedCredit() <= (activeAccount.getDeposit() / 10)) ||
				 (activeAccount.getCertifiedCredit() <= (1024 * 1024)))
		{
			m_AccountText.setText(JAPMessages.getString("ngPaymentRecharge"));
			m_AccountText.setForeground(Color.red);
			m_AccountIconLabel.setIcon(m_accountIcons[2]);
			m_BalanceText.setEnabled(true);
			m_BalanceText.setText(JAPUtil.formatBytesValue(activeAccount.getCertifiedCredit()));
			m_BalanceProgressBar.setValue(0);
			m_BalanceProgressBar.setEnabled(true);
		}

		// we got everything under control, situation normal
		else
		{
			Timestamp t = activeAccount.getBalance().getTimestamp();
			SimpleDateFormat sdf = new SimpleDateFormat("dd.MM.yyyy HH:mm");
			//String dateText = t.getDay() + "." + (t.getMonth() + 1) + "." + (t.getYear() + 1900) + " " +
			//	t.getHours() + ":" + t.getMinutes();
			String dateText = sdf.format(t);
			m_AccountText.setText(JAPMessages.getString("ngPaymentBalanceDate") + ": " + dateText);
			m_AccountText.setForeground(Color.black);
			m_AccountIconLabel.setIcon(m_accountIcons[1]);
			m_BalanceText.setEnabled(true);
			m_BalanceText.setText(JAPUtil.formatBytesValue(activeAccount.getCertifiedCredit()));
			m_BalanceProgressBar.setMaximum( (int) activeAccount.getDeposit());
			m_BalanceProgressBar.setValue( (int) activeAccount.getCertifiedCredit());
			m_BalanceProgressBar.setEnabled(true);
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
					m_view.showConfigDialog(JAPConf.PAYMENT_TAB);
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
			/** @todo internationalize */
			JOptionPane.showOptionDialog(
				PaymentMainPanel.this,
				JAPMessages.getString("aiErrorMessage") + msg.getErrorDescription(),
				JAPMessages.getString("error"),
				JOptionPane.YES_NO_OPTION, JOptionPane.ERROR_MESSAGE,
				null, null, null);
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
				m_accountIcons[i] = JAPUtil.loadImageIcon(JAPConstants.ACCOUNTICONFNARRAY[i], false);
			}
		}
		else // scale down for small displays
		{
			MediaTracker m = new MediaTracker(this);
			for (int i = 0; i < JAPConstants.ACCOUNTICONFNARRAY.length; i++)
			{
				Image tmp = JAPUtil.loadImageIcon(JAPConstants.ACCOUNTICONFNARRAY[i], true).getImage();
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
