package pay.gui;

import java.awt.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.event.*;

import jap.*;
import pay.*;
import logging.LogLevel;
import logging.LogType;
import logging.LogHolder;

/**
 * This class is the main payment view on JAP's main gui window
 *
 * @author Bastian Voigt
 * @version 1.0
 */

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
			m_AccountText.setText("Auszug vom "+activeAccount.getBalanceValidTime());
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
			if(accounts==null) accounts = PayAccountsFile.getInstance();
			PayAccount source = (PayAccount) e.getSource();
			if (source == accounts.getActiveAccount())
			{
				updateDisplay(source);
			}
		}
	}

	private MyChangeListener m_MyChangeListener = new MyChangeListener();
}
