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
package pay.gui;

/**
 * This class is the main payment view on JAP's main gui window
 *
 * @author Bastian Voigt
 * @version 1.0
 */
import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Image;
import java.awt.MediaTracker;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.border.EtchedBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import jap.JAPConstants;
import jap.JAPModel;
import jap.JAPUtil;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import pay.PayAccount;
import pay.PayAccountsFile;

public class PaymentMainPanel extends JPanel
{
	/**
	 * Icons for the account icon display
	 */
	private ImageIcon[] m_accountIcons;

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
			{}
		}
	}

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

	public PaymentMainPanel()
	{
		super(new BorderLayout());
		loadIcons();

		setBorder(new TitledBorder("Account information"));

		// show the icon label
		m_AccountIconLabel = new JLabel(m_accountIcons[1]);
		this.add(m_AccountIconLabel, BorderLayout.WEST);

		JPanel centerPanel = new JPanel(new BorderLayout());

		// show the date of last update
		m_AccountText = new JLabel("");
		m_AccountText.setBorder(new EtchedBorder());
		centerPanel.add(m_AccountText, BorderLayout.NORTH);

		JPanel kontostandPanel = new JPanel(new BorderLayout());
		// show the current account balance
		m_BalanceProgressBar = new JProgressBar(0, 100);
		m_BalanceProgressBar.setValue(77);
		kontostandPanel.add(m_BalanceProgressBar, BorderLayout.CENTER);

		m_BalanceText = new JLabel("");
		kontostandPanel.add(m_BalanceText, BorderLayout.EAST);

		centerPanel.add(kontostandPanel, BorderLayout.SOUTH);
		this.add(centerPanel, BorderLayout.CENTER);

		this.add(new JButton("Aufladen"), BorderLayout.EAST);
		LogHolder.log(LogLevel.DEBUG, LogType.PAY, "PaymentMainPanel: Calling accountsFile()");
		PayAccountsFile.getInstance().addChangeListener(m_MyChangeListener);
		LogHolder.log(LogLevel.DEBUG, LogType.PAY, "PaymentMainPanel: Calling updateDisplay()");
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
			m_AccountText.setText("Payment disabled");
			m_AccountIconLabel.setIcon(m_accountIcons[0]);
			m_BalanceText.setText("");
			m_BalanceText.setEnabled(false);
			m_BalanceProgressBar.setValue(0);
			m_BalanceProgressBar.setEnabled(false);
		}

		// account is empty :-(
		else if (activeAccount.getCredit() == 0)
		{
			m_AccountText.setText("You should recharge your account");
			m_AccountText.setForeground(Color.red);
			m_AccountIconLabel.setIcon(m_accountIcons[2]);
			m_BalanceText.setEnabled(true);
			m_BalanceText.setText("0 Bytes");
			m_BalanceProgressBar.setValue(0);
			m_BalanceProgressBar.setEnabled(true);
		}

		// we got everything under control, situation normal
		else
		{
			m_AccountText.setText("Auszug vom " + activeAccount.getBalanceValidTime());
			m_AccountText.setForeground(Color.black);
			m_AccountIconLabel.setIcon(m_accountIcons[1]);
			m_BalanceText.setEnabled(true);
			m_BalanceText.setText(activeAccount.getCredit() + " Bytes");
			m_BalanceProgressBar.setMaximum( (int) activeAccount.getDeposit());
			m_BalanceProgressBar.setValue( (int) activeAccount.getCredit());
			m_BalanceProgressBar.setEnabled(true);
		}
	}

	/**
	 * Notifies us when the state of the active account changes, so we can update
	 * the display
	 *
	 * @version 1.0
	 */
	private class MyChangeListener implements ChangeListener
	{
		private PayAccountsFile accounts = null;
		public void stateChanged(ChangeEvent e)
		{
			if (accounts == null)
			{
				accounts = PayAccountsFile.getInstance();
			}
			PayAccount source = (PayAccount) e.getSource();
			if (source == accounts.getActiveAccount())
			{
				updateDisplay(source);
			}
		}
	}

	private MyChangeListener m_MyChangeListener = new MyChangeListener();
}
