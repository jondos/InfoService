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

import java.io.File;
import java.io.FileOutputStream;
import java.sql.Timestamp;
import java.util.Enumeration;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import java.awt.BorderLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.GridLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.border.EmptyBorder;
import javax.swing.border.TitledBorder;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import anon.crypto.JAPCertificate;
import anon.crypto.XMLEncryption;
import anon.pay.BI;
import anon.pay.PayAccount;
import anon.pay.PayAccountsFile;
import anon.pay.xml.XMLAccountInfo;
import anon.pay.xml.XMLBalance;
import anon.pay.xml.XMLTransCert;
import anon.util.XMLUtil;
import gui.ByteNumberCellRenderer;
import gui.JAPMessages;
import gui.TimestampCellRenderer;
import jap.AbstractJAPConfModule;
import jap.JAPConstants;
import jap.JAPController;
import jap.JAPPasswordReader;
import jap.JAPUtil;
import jap.JAPWaitSplash;
import jap.pay.wizard.PaymentWizard;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import javax.swing.JSeparator;
import java.awt.Dimension;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;

/**
 * The Jap Conf Module (Settings Tab Page) for the Accounts and payment Management
 *
 * @author Bastian Voigt, Tobias Bayer
 * @version 1.0
 */
public class AccountSettingsPanel extends AbstractJAPConfModule implements ChangeListener
{

	/** Messages */
	private static final String MSG_BUTTON_TRANSACTIONS = AccountSettingsPanel.class.
		getName() + "_button_transactions";
	private static final String MSG_BUTTON_DELETE = AccountSettingsPanel.class.
		getName() + "_button_delete";
	private static final String MSG_BUTTON_EXPORT = AccountSettingsPanel.class.
		getName() + "_button_export";
	private static final String MSG_TRANSACTION_OVERVIEW_DIALOG = AccountSettingsPanel.class.
		getName() + "_transaction_overview_dialog";
	private static final String MSG_ACCOUNT_SPENT = AccountSettingsPanel.class.
		getName() + "_account_spent";
	private static final String MSG_ACCOUNT_DEPOSIT = AccountSettingsPanel.class.
		getName() + "_account_deposit";
	private static final String MSG_ACCOUNT_BALANCE = AccountSettingsPanel.class.
		getName() + "_account_balance";
	private static final String MSG_ACCOUNT_VALID = AccountSettingsPanel.class.
		getName() + "_account_valid";
	private static final String MSG_ACCOUNT_DETAILS = AccountSettingsPanel.class.
		getName() + "_account_details";
	private static final String MSG_ACCOUNT_CREATION_DATE = AccountSettingsPanel.class.
		getName() + "_account_creation_date";
	private static final String MSG_ACCOUNT_STATEMENT_DATE = AccountSettingsPanel.class.
		getName() + "_account_statement_date";
	private static final String MSG_BUTTON_CHARGE = AccountSettingsPanel.class.
		getName() + "_button_charge";

	private JTable m_Table;
	private JButton m_btnCreateAccount;
	private JButton m_btnChargeAccount;
	private JButton m_btnStatement;
	private JButton m_btnActivate;
	private JButton m_btnDeleteAccount;
	private JButton m_btnExportAccount;
	private JButton m_btnImportAccount;
	private JButton m_btnTransactions;

	private JLabel m_labelCreationDate;
	private JLabel m_labelStatementDate;
	private JLabel m_labelDeposit;
	private JLabel m_labelSpent;
	private JLabel m_labelBalance;
	private JLabel m_labelValid;

	private MyTableModel m_MyTableModel;

	public AccountSettingsPanel()
	{
		super(null);
	}

	/**
	 * getTabTitle
	 *
	 * @return String
	 */
	public String getTabTitle()
	{
		return JAPMessages.getString("ngPaymentTabTitle");
	}

	/**
	 * recreateRootPanel - recreates all GUI elements
	 */
	public void recreateRootPanel()
	{
		JPanel rootPanel = getRootPanel();
		/* clear the whole root panel */
		rootPanel.removeAll();
		rootPanel.setLayout(new GridBagLayout());
		rootPanel.setBorder(new TitledBorder(JAPMessages.getString("ngPseudonymAccounts")));

		m_Table = new JTable();
		m_Table.setPreferredScrollableViewportSize(new Dimension(550,200));
		m_MyTableModel = new MyTableModel();
		m_Table.setModel(m_MyTableModel);
		m_Table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		m_Table.setDefaultRenderer(java.sql.Timestamp.class, new TimestampCellRenderer(false));
		m_Table.setDefaultRenderer(Long.class, new ByteNumberCellRenderer());
		m_Table.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent e)
			{
				//Activate account on double click
				if (e.getClickCount() == 2)
				{
					int selectedRow = m_Table.getSelectedRow();
					doActivateAccount(getSelectedAccount());
					m_Table.setRowSelectionInterval(selectedRow, selectedRow);
				}
			}
		}
		);

		ActionListener myActionListener = new MyActionListener();
		//Ask to be notified of selection changes.
		ListSelectionModel rowSM = m_Table.getSelectionModel();
		rowSM.addListSelectionListener(new ListSelectionListener()
		{
			public void valueChanged(ListSelectionEvent e)
			{
				enableDisableButtons();
				doShowDetails(getSelectedAccount());
			}
		});

		JPanel buttonsPanel = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = c.HORIZONTAL;
		c.anchor = c.NORTHWEST;
		c.weightx = 0;
		c.weighty = 0;
		c.gridx = 0;
		c.gridy = 0;
		c.insets = new Insets(5, 5, 5, 5);

		m_btnCreateAccount = new JButton(JAPMessages.getString("ngCreateAccount"));
		m_btnCreateAccount.addActionListener(myActionListener);
		buttonsPanel.add(m_btnCreateAccount, c);

		c.gridx++;
		c.weightx = 1;
		c.weighty = 1;
		m_btnImportAccount = new JButton(JAPMessages.getString("ngImportAccount"));
		m_btnImportAccount.addActionListener(myActionListener);
		buttonsPanel.add(m_btnImportAccount, c);

		c = new GridBagConstraints();
		c.fill = c.NONE;
		c.anchor = c.NORTHWEST;
		c.weightx = 0;
		c.weighty = 0;
		c.gridx = 0;
		c.gridy = 0;
		c.insets = new Insets(5, 5, 5, 5);
		rootPanel.add(new JScrollPane(m_Table), c);
		c.gridy++;
		rootPanel.add(buttonsPanel, c);
		c.gridy++;
		JSeparator sep = new JSeparator(JSeparator.HORIZONTAL);
		sep.setPreferredSize(new Dimension(560, 10));
		rootPanel.add(sep, c);
		c.weightx = 1;
		c.weighty = 1;
		c.gridy++;
		c.fill = c.HORIZONTAL;
		rootPanel.add(this.createDetailsPanel(), c);
		enableDisableButtons();
	}

	/**
	 * Creates a new lower view of the dialog for displaying account details.
	 * @return JPanel
	 */
	private JPanel createDetailsPanel()
	{
		JPanel p = new JPanel(new GridBagLayout());
		GridBagConstraints c = new GridBagConstraints();
		c.fill = c.NONE;
		c.anchor = c.NORTHWEST;
		c.weightx = 0;
		c.weighty = 0;
		c.gridx = 0;
		c.gridy = 0;
		c.insets = new Insets(5, 5, 5, 5);
		p.add(new JLabel(JAPMessages.getString(MSG_ACCOUNT_DETAILS)), c);

		c.insets = new Insets(5, 10, 5, 5);
		c.gridy++;
		p.add(new JLabel(JAPMessages.getString(MSG_ACCOUNT_CREATION_DATE)), c);
		c.gridx++;
		m_labelCreationDate = new JLabel();
		p.add(m_labelCreationDate, c);

		c.gridx--;
		c.gridy++;
		p.add(new JLabel(JAPMessages.getString(MSG_ACCOUNT_STATEMENT_DATE)), c);
		c.gridx++;
		m_labelStatementDate = new JLabel();
		p.add(m_labelStatementDate, c);

		c.gridx--;
		c.gridy++;
		p.add(new JLabel(JAPMessages.getString(MSG_ACCOUNT_DEPOSIT)), c);
		c.gridx++;
		m_labelDeposit = new JLabel();
		p.add(m_labelDeposit, c);

		c.gridx--;
		c.gridy++;
		p.add(new JLabel(JAPMessages.getString(MSG_ACCOUNT_SPENT)), c);
		c.gridx++;
		m_labelSpent = new JLabel();
		p.add(m_labelSpent, c);

		c.gridx--;
		c.gridy++;
		p.add(new JLabel(JAPMessages.getString(MSG_ACCOUNT_BALANCE)), c);
		c.gridx++;
		m_labelBalance = new JLabel();
		p.add(m_labelBalance, c);

		c.gridx--;
		c.gridy++;
		p.add(new JLabel(JAPMessages.getString(MSG_ACCOUNT_VALID)), c);
		c.gridx++;
		m_labelValid = new JLabel();
		p.add(m_labelValid, c);

		ActionListener myActionListener = new MyActionListener();

		JPanel buttonsPanel = new JPanel(new GridBagLayout());
		GridBagConstraints d = new GridBagConstraints();
		d.fill = c.HORIZONTAL;
		d.anchor = c.NORTHWEST;
		d.weightx = 0;
		d.weighty = 0;
		d.gridx = 0;
		d.gridy = 0;
		d.insets = new Insets(5, 5, 5, 5);

		m_btnChargeAccount = new JButton(JAPMessages.getString(MSG_BUTTON_CHARGE));
		m_btnChargeAccount.setEnabled(false);
		m_btnChargeAccount.addActionListener(myActionListener);
		buttonsPanel.add(m_btnChargeAccount, d);

		d.gridx++;
		m_btnTransactions = new JButton(JAPMessages.getString(MSG_BUTTON_TRANSACTIONS));
		m_btnTransactions.addActionListener(myActionListener);
		buttonsPanel.add(m_btnTransactions, d);

		d.gridx++;
		m_btnExportAccount = new JButton(JAPMessages.getString(MSG_BUTTON_EXPORT));
		m_btnExportAccount.addActionListener(myActionListener);
		buttonsPanel.add(m_btnExportAccount, d);

		d.gridx++;
		d.weightx = 1;
		d.weighty = 1;
		m_btnDeleteAccount = new JButton(JAPMessages.getString(MSG_BUTTON_DELETE));
		m_btnDeleteAccount.addActionListener(myActionListener);
		buttonsPanel.add(m_btnDeleteAccount, d);

		c.anchor = c.NORTHWEST;
		c.weightx = 1;
		c.weighty = 1;
		c.gridx = 0;
		c.gridy++;
		c.gridwidth = 2;
		p.add(buttonsPanel, c);

		return p;
	}

	private void enableDisableButtons()
	{
		boolean enable = (m_Table.getSelectedRow() >= 0);
		m_btnChargeAccount.setEnabled(enable);
		m_btnTransactions.setEnabled(enable);
		m_btnDeleteAccount.setEnabled(enable);
		m_btnExportAccount.setEnabled(enable);
	}

	/**
	 * Handler for the Button Clicks
	 * @author Bastian Voigt
	 * @version 1.0
	 */
	private class MyActionListener implements ActionListener
	{
		public void actionPerformed(ActionEvent e)
		{
			JButton source = (JButton) e.getSource();
			if (source == m_btnCreateAccount)
			{
				doCreateAccount();
			}
			else if (source == m_btnChargeAccount)
			{
				doChargeAccount(getSelectedAccount());
			}
			else if (source == m_btnStatement)
			{
				doGetStatement(getSelectedAccount());
			}
			else if (source == m_btnActivate)
			{
				doActivateAccount(getSelectedAccount());
			}
			else if (source == m_btnDeleteAccount)
			{
				doDeleteAccount(getSelectedAccount());
			}
			else if (source == m_btnImportAccount)
			{
				doImportAccount();
			}
			else if (source == m_btnExportAccount)
			{
				doExportAccount(getSelectedAccount());
			}
			else if (source == m_btnTransactions)
			{
				doShowTransactions(getSelectedAccount());
			}

		}
	}

	/**
	 * Shows transaction numbers and if they have been used
	 * @param a_account PayAccount
	 */
	private void doShowTransactions(PayAccount a_account)
	{
		TransactionOverviewDialog d = new TransactionOverviewDialog(JAPController.getView(),
			JAPMessages.getString(MSG_TRANSACTION_OVERVIEW_DIALOG), true, a_account);
	}

	/**
	 * doShowDetails - shows account details in the details panel
	 */
	private void doShowDetails(PayAccount selectedAccount)
	{
		if (selectedAccount == null)
		{
			return;
		}

		/** If there is no account info or the account info is older than 24 hours,
		 * fetch a new statement from the Payment Instance.
		 */
		if (!selectedAccount.hasAccountInfo() ||
			(selectedAccount.getAccountInfo().getBalance().getTimestamp().getTime() <
			 (System.currentTimeMillis() - 1000 * 60 * 60 * 24)))
		{
			doGetStatement(selectedAccount);
		}

		XMLAccountInfo accountInfo = selectedAccount.getAccountInfo();
		XMLBalance balance = accountInfo.getBalance();

		m_labelCreationDate.setText(JAPUtil.formatTimestamp(selectedAccount.getCreationTime(), false));
		m_labelStatementDate.setText(JAPUtil.formatTimestamp(balance.getTimestamp(), true));
		m_labelDeposit.setText(JAPUtil.formatBytesValue(balance.getDeposit()));
		m_labelSpent.setText(JAPUtil.formatBytesValue(balance.getSpent()));
		m_labelBalance.setText(JAPUtil.formatBytesValue(balance.getDeposit() - balance.getSpent()));
		m_labelValid.setText(JAPUtil.formatTimestamp(balance.getValidTime(), true));

		/*JFrame view = JAPController.getView();
		if (!selectedAccount.hasAccountInfo())
		{
			int choice = JOptionPane.showOptionDialog(
				view, JAPMessages.getString("ngNoStatement"), JAPMessages.getString("ngNoStatementTitle"),
				JOptionPane.YES_NO_OPTION,
				JOptionPane.QUESTION_MESSAGE,
				null, null, null
				);
			if (choice == JOptionPane.YES_OPTION)
			{
				if (!doGetStatement(selectedAccount))
				{
					return;
				}
			}
			else
			{
				return;
			}
		}
		else
		{
			// if timestamp is older than 24 hours... maybe user wants to fetch
			// a new statement
			java.sql.Timestamp t = selectedAccount.getAccountInfo().getBalance().getTimestamp();
			if (t.getTime() < (System.currentTimeMillis() - 1000 * 60 * 60 * 24))
			{
				int choice = JOptionPane.showOptionDialog(
					view, JAPMessages.getString("ngOldStatement"),
					JAPMessages.getString("ngOldStatementTitle"),
					JOptionPane.YES_NO_OPTION,
					JOptionPane.QUESTION_MESSAGE,
					null, null, null
					);
				if (choice == JOptionPane.YES_OPTION)
				{
					doGetStatement(selectedAccount);
				}
			}
		}
		XMLAccountInfo accountInfo = selectedAccount.getAccountInfo();
		XMLBalance balance = accountInfo.getBalance();
		String msg =
			"<html><h2>" + JAPMessages.getString("ngAccountDetailsTxt") + " " +
			selectedAccount.getAccountNumber() + "</h2>" +
			"<table>" +
			"<tr><td>" + JAPMessages.getString("creationDate") + "</td><td>" +
			JAPUtil.formatTimestamp(selectedAccount.getCreationTime(), false) + "</td></tr>" +
			"<tr><td>" + JAPMessages.getString("ngStatementDate") + "</td><td>" +
			JAPUtil.formatTimestamp(balance.getTimestamp(), true) + "</td></tr>" +
			"<tr><td> </td></tr>" +
			"<tr><td>" + JAPMessages.getString("deposit") + "</td><td>" +
			JAPUtil.formatBytesValue(balance.getDeposit()) + "</td></tr>" +
			"<tr><td>" + JAPMessages.getString("spent") + "</td><td>" +
			JAPUtil.formatBytesValue(balance.getSpent()) + "</td></tr>" +
			"<tr><td>" + JAPMessages.getString("balance") + "</td><td>" +
			JAPUtil.formatBytesValue(balance.getDeposit() - balance.getSpent()) +
			"</td></tr>" +
			"<tr><td>" + JAPMessages.getString("validTo") + "</td><td>" +
			JAPUtil.formatTimestamp(balance.getValidTime(), true) + "</td></tr>" +
			"</table>";
		Enumeration ccs = accountInfo.getCCs();
		XMLEasyCC cc = null;
		if (ccs.hasMoreElements())
		{
			msg += "<hr><h3>" + JAPMessages.getString("costconfirmations") + "</h3>";
			do
			{
				cc = ( (XMLEasyCC) (ccs.nextElement()));
				msg += JAPMessages.getString("rttMixCascade") + " " + cc.getAIName() + ": " +
					JAPUtil.formatBytesValue(cc.getTransferredBytes()) + "<br>";
			}
			while (ccs.hasMoreElements());
		}
		msg += "</html>";

		JOptionPane.showMessageDialog(
			view, msg,
			JAPMessages.getString("ngAccountDetailsTxt") + " " + selectedAccount.getAccountNumber(),
			JOptionPane.INFORMATION_MESSAGE
		 );*/
	}

	/**
	 * returns the selected (active) account
	 * @return PayAccount
	 */
	private PayAccount getSelectedAccount()
	{
		PayAccountsFile accounts = PayAccountsFile.getInstance();

		// try to find the account the user wishes to charge
		int selectedRow = m_Table.getSelectedRow();
		if (selectedRow < 0)
		{
			return null;
		}
		return accounts.getAccountAt(selectedRow);
	}

	/**
	 * Charges the selected account
	 * @todo replace splashscreen by rotating icon
	 */
	private void doChargeAccount(PayAccount selectedAccount)
	{
		if (selectedAccount == null)
		{
			return;
		}
		JFrame view = JAPController.getView();
		XMLTransCert transferCertificate = null;

		PaymentWizard paymentWiz = new PaymentWizard(selectedAccount);

		/*int choice = JOptionPane.showOptionDialog(
		 view,
		 JAPMessages.getString("ngFetchTransferNumber"),
		 JAPMessages.getString("ngPaymentCharge"),
		 JOptionPane.YES_NO_OPTION,
		 JOptionPane.QUESTION_MESSAGE,
		 null, null, null
		 );
		   if (choice == JOptionPane.YES_OPTION)
		   {
		 /** @todo find out why the wait splash screen looks so ugly
			JAPWaitSplash splash = null;
			try
			{
		  splash = JAPWaitSplash.start("Fetching transfer number...", "Please wait");
		  Thread.sleep(5);
		  transferCertificate = selectedAccount.charge();
		  splash.abort();
			}
			catch (Exception ex)
			{
		  splash.abort();
		  LogHolder.log(LogLevel.DEBUG, LogType.PAY, ex);
		  JOptionPane.showMessageDialog(
		   view,
		   "<html>" + JAPMessages.getString("ngTransferNumberError") + "<br>" + ex.getMessage() +
		   "</html>",
		   JAPMessages.getString("error"), JOptionPane.ERROR_MESSAGE
		   );
		  return;
			}

			// try to launch webbrowser
			AbstractOS os = AbstractOS.getInstance();
			String url = transferCertificate.getBaseUrl();
			url += "?transfernum=" + transferCertificate.getTransferNumber();
			try
			{
		  os.openURLInBrowser(url);
			}
			catch (Exception e)
			{
		  JOptionPane.showMessageDialog(
		   view,
		   "<html>" + JAPMessages.getString("ngCouldNotFindBrowser") + "<br>" +
		   "<h3>" + url + "</h3></html>",
		   JAPMessages.getString("ngCouldNotFindBrowserTitle"),
		   JOptionPane.INFORMATION_MESSAGE
		   );
			}

			m_MyTableModel.fireTableDataChanged();
		   }*/
	}

	/**
	 *
	 * @return boolean
	 */
	private void doCreateAccount()
	{
		JFrame view = JAPController.getView();

		//Show a window that contains all known Payment Instances and let the user select one. (tb)
		/*BI theBI = getBIforAccountCreation();*/
		//BI for semi-open test cascade
		BI theBI = null;
		try
		{
			theBI = new BI(JAPConstants.PI_ID, JAPConstants.PI_NAME, JAPConstants.PI_HOST,
						   JAPConstants.PI_PORT,
						   JAPCertificate.getInstance(JAPConstants.CERTSPATH + JAPConstants.PI_CERT));
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, "Could not create Test-PI: " + e.getMessage());
		}

		if (theBI != null)
		{
			int choice = JOptionPane.showOptionDialog(
				view, JAPMessages.getString("ngCreateKeyPair"),
				JAPMessages.getString("ngCreateAccount"),
				JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
				null, null, null
				);
			if (choice == JOptionPane.YES_OPTION)
			{
				try
				{
					AccountCreator worker = new AccountCreator(theBI, JAPController.getView());
					worker.addChangeListener(this);
					worker.start();
				}
				catch (Exception ex)
				{
					LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, ex);
					JOptionPane.showMessageDialog(
						view,
						JAPMessages.getString("Error creating account: ") + ex,
						JAPMessages.getString("error"), JOptionPane.ERROR_MESSAGE
						);
				}
			}
		}
	}

	/**
	 * Shows a window with all known Payment Instances and lets the user select one.
	 * @return BI
	 */
	private BI getBIforAccountCreation()
	{
		BISelectionDialog d = new BISelectionDialog();
		BI theBI = d.getSelectedBI();

		if (theBI != null)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.PAY, "Selected Payment Instance is: " +
						  theBI.getHostName() + ":" + theBI.getPortNumber());
		}
		else
		{
			LogHolder.log(LogLevel.DEBUG, LogType.PAY, "Dialog returned no payment instance");
		}

		return theBI;
	}

	/**
	 * doActivateAccount
	 *
	 * @param payAccount PayAccount
	 */
	private void doActivateAccount(PayAccount selectedAccount)
	{
		if (selectedAccount == null)
		{
			return;
		}
		PayAccountsFile accounts = PayAccountsFile.getInstance();
		try
		{
			accounts.setActiveAccount(selectedAccount.getAccountNumber());
		}
		catch (Exception ex)
		{
			JOptionPane.showMessageDialog(
				JAPController.getView(),
				JAPMessages.getString("Could not activate account. Error Code: ") +
				ex.getMessage(),
				JAPMessages.getString("error"),
				JOptionPane.ERROR_MESSAGE
				);
		}
		m_MyTableModel.fireTableDataChanged();
	}

	/**
	 * doGetStatement - fetches an account statement (=german kontoauszug)
	 */
	private boolean doGetStatement(PayAccount selectedAccount)
	{
		if (selectedAccount == null)
		{
			return false;
		}
		JFrame view = JAPController.getView();
		JAPWaitSplash splash = null;
		try
		{
			splash = JAPWaitSplash.start(
				JAPMessages.getString("Fetching account statement"),
				JAPMessages.getString("Please wait")
				);
			selectedAccount.fetchAccountInfo();
			splash.abort();
		}
		catch (Exception ex)
		{
			splash.abort();
			LogHolder.log(LogLevel.DEBUG, LogType.PAY, "Exception FetchStatement: " + ex.toString());
			JOptionPane.showMessageDialog(
				view,
				JAPMessages.getString("Error fetching account statement: ") + ex.getMessage(),
				JAPMessages.getString("error"),
				JOptionPane.ERROR_MESSAGE
				);
			return false;
		}
		m_MyTableModel.fireTableDataChanged();
		return true;
	}

	/**
	 * doExportAccount
	 *
	 * @param payAccount PayAccount
	 */
	private void doExportAccount(PayAccount selectedAccount)
	{
		boolean encrypt = false;
		String strPassword = null;

		if (selectedAccount == null)
		{
			return;
		}
		JFrame view = JAPController.getView();

		int choice = JOptionPane.showOptionDialog(
			view,
			JAPMessages.getString("ngExportAccountEncrypt"),
			JAPMessages.getString("ngExportAccount"),
			JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
			null, null, null);
		if (choice == JOptionPane.YES_OPTION)
		{
			strPassword = new JAPPasswordReader(true).readPassword(JAPMessages.getString("choosePassword"));
			encrypt = true;
		}

		JFileChooser chooser = new JFileChooser();
		MyFileFilter filter = new MyFileFilter();
		chooser.setFileFilter(filter);
		int returnVal = chooser.showSaveDialog(view);
		if (returnVal == JFileChooser.APPROVE_OPTION)
		{
			try
			{
				File f = chooser.getSelectedFile();
				Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
				Element elemRoot = doc.createElement("root");
				elemRoot.setAttribute("filetype", "JapAccountFile");
				elemRoot.setAttribute("version", "1.0");

				doc.appendChild(elemRoot);
				Element elemAccount = selectedAccount.toXmlElement(doc);
				elemRoot.appendChild(elemAccount);
				if (encrypt)
				{
					XMLEncryption.encryptElement(elemAccount, strPassword);
				}
				String strOutput = XMLUtil.toString(doc);
				FileOutputStream outStream = new FileOutputStream(f);
				outStream.write(strOutput.getBytes());
				outStream.close();
			}
			catch (Exception e)
			{
				LogHolder.log(LogLevel.DEBUG, LogType.PAY, e);
				JOptionPane.showMessageDialog(
					view,
					"Could not export account, an error occured: " + e.getMessage(),
					"Sorry", JOptionPane.ERROR_MESSAGE
					);
			}
		}
	}

	/**
	 * Filefilter for the import function
	 *
	 * @author Bastian Voigt
	 * @version 1.0
	 */
	private class MyFileFilter extends javax.swing.filechooser.FileFilter
	{
		private String m_strDesc = "JAP Accountfile (*.account)";
		private String m_strExtension = ".account";
		private int filterType;

		public int getFilterType()
		{
			return filterType;
		}

		public boolean accept(File f)
		{
			return f.isDirectory() || f.getName().endsWith(m_strExtension);
		}

		public String getDescription()
		{
			return m_strDesc;
		}
	}

	/**
	 * doImportAccount - imports an account from a file
	 */
	private void doImportAccount()
	{
		JFrame view = JAPController.getView();
		PayAccount importedAccount = null;
		Element elemAccount = null;
		JFileChooser chooser = new JFileChooser();
		MyFileFilter filter = new MyFileFilter();
		chooser.setFileFilter(filter);
		int returnVal = chooser.showOpenDialog(view);
		if (returnVal == JFileChooser.APPROVE_OPTION)
		{
			File f = chooser.getSelectedFile();
			try
			{
				DocumentBuilder b = DocumentBuilderFactory.newInstance().newDocumentBuilder();
				Document doc = b.parse(f);
				Element elemRoot = doc.getDocumentElement();
				elemAccount = (Element) XMLUtil.getFirstChildByName(elemRoot, "Account");

				// maybe it was encrypted
				if (elemAccount == null)
				{
					Element elemCrypt = (Element) XMLUtil.getFirstChildByName(elemRoot, "EncryptedData");
					if (elemCrypt != null)
					{
						String strMessage = JAPMessages.getString("ngPasswordDecrypt");
						String strPassword = null;
						JAPPasswordReader pr = new JAPPasswordReader(false);
						while (true)
						{
							// ask for password
							strPassword = pr.readPassword(strMessage);

							if (strPassword == null) // user pressed "cancel"
							{
								break;
							}
							try
							{
								elemAccount = XMLEncryption.decryptElement(elemCrypt, strPassword);
							}
							catch (Exception ex)
							{
								strMessage = JAPMessages.getString("ngPasswordTryAgain") +
									JAPMessages.getString("ngPasswordDecrypt");
								strPassword = null;
								continue;
							}
							break ;
						}
					}
				}
			}
			catch (Exception e)
			{
				JOptionPane.showMessageDialog(
					view,
					JAPMessages.getString("ngImportFailed"),
					JAPMessages.getString("error"),
					JOptionPane.ERROR_MESSAGE
					);
			}
			try
			{
				if (elemAccount != null)
				{
					importedAccount = new PayAccount(elemAccount);
					PayAccountsFile accounts = PayAccountsFile.getInstance();
					accounts.addAccount(importedAccount);
					m_MyTableModel.fireTableDataChanged();
				}
			}
			catch (Exception ex)
			{
				JOptionPane.showMessageDialog(
					view,
					"<html>" + JAPMessages.getString("ngImportFailed") + "<br>" +
					ex.getMessage() + "</html>",
					JAPMessages.getString("error"),
					JOptionPane.ERROR_MESSAGE
					);
			}
		}
	}

	/**
	 * doDeleteAccount
	 *
	 * @param payAccount PayAccount
	 */
	private void doDeleteAccount(PayAccount selectedAccount)
	{
		if (selectedAccount == null)
		{
			return;
		}
		PayAccountsFile accounts = PayAccountsFile.getInstance();
		JFrame view = JAPController.getView();
		boolean reallyDelete = false;

		if (!selectedAccount.hasAccountInfo())
		{
			int choice = JOptionPane.showOptionDialog(
				view,
				JAPMessages.getString("ngDeleteAccountStatement"),
				JAPMessages.getString("ngDeleteAccount"),
				JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
				null, null, null);
			if (choice == JOptionPane.YES_OPTION)
			{
				if (!doGetStatement(selectedAccount))
				{
					return;
				}
			}
		}
		if (selectedAccount.hasAccountInfo())
		{
			XMLAccountInfo accInfo = selectedAccount.getAccountInfo();
			if (accInfo.getBalance().getTimestamp().getTime() <
				(System.currentTimeMillis() - 1000 * 60 * 60 * 24))
			{
				int choice = JOptionPane.showOptionDialog(
					view,
					JAPMessages.getString("ngDeleteAccountOldStmt"),
					JAPMessages.getString("ngDeleteAccount"),
					JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
					null, null, null);
				if (choice == JOptionPane.YES_OPTION)
				{
					if (!doGetStatement(selectedAccount))
					{
						return;
					}
				}
			}

			if (accInfo.getBalance().getCredit() > 0)
			{
				int choice = JOptionPane.showOptionDialog(
					view,
					JAPMessages.getString("ngDeleteAccountCreditLeft"),
					JAPMessages.getString("ngDeleteAccount"),
					JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
					null, null, null);
				if (choice == JOptionPane.YES_OPTION)
				{
					reallyDelete = true;
				}
			}
			else
			{
				int choice = JOptionPane.showOptionDialog(
					view,
					JAPMessages.getString("ngReallyDeleteAccount"),
					JAPMessages.getString("ngDeleteAccount"),
					JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
					null, null, null);
				if (choice == JOptionPane.YES_OPTION)
				{
					reallyDelete = true;
				}
			}
		}
		else
		{
			int choice = JOptionPane.showOptionDialog(
				view,
				JAPMessages.getString("ngReallyDeleteAccount"),
				JAPMessages.getString("ngDeleteAccount"),
				JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
				null, null, null);
			if (choice == JOptionPane.YES_OPTION)
			{
				reallyDelete = true;
			}
		}
		if (reallyDelete)
		{
			try
			{
				accounts.deleteAccount(selectedAccount.getAccountNumber());
				m_MyTableModel.fireTableDataChanged();
			}
			catch (Exception ex)
			{
				JOptionPane.showMessageDialog(
					view,
					"<html>Error while deleting: " + ex.getMessage(),
					JAPMessages.getString("error"), JOptionPane.ERROR_MESSAGE);
			}
		}
	}

	/**
	 * This method can be overwritten by the children of AbstractJAPConfModule. It is called
	 * every time the root panel comes to the foreground (is set to visible).
	 */
	protected void onRootPanelShown()
	{
	}

	/**
	 * This method can be overwritten by the children of AbstractJAPConfModule. It is called
	 * every time the user presses "OK" in the configuration dialog.
	 */
	protected boolean onOkPressed()
	{
		return true;
	}

	/**
	 * This method can be overwritten by the children of AbstractJAPConfModule. It is called
	 * every time the user presses "Cancel" in the configuration dialog after the restoring
	 * of the savepoint data (if there is a savepoint for this module).
	 */
	protected void onCancelPressed()
	{
		// it does not make sense to do anything here IMO...
	}

	/**
	 * This method can be overwritten by the children of AbstractJAPConfModule. It is called
	 * every time the user presses "Reset to defaults" in the configuration dialog after the
	 * restoring of the default configuration from the savepoint (if there is a savepoint for
	 * this module).
	 */
	protected void onResetToDefaultsPressed()
	{
	}

	/**
	 * Fetches new (changed) account data from the PayAccountsFile
	 */
	protected void onUpdateValues()
	{
		PayAccountsFile accounts = PayAccountsFile.getInstance();
		Enumeration enumAccounts = accounts.getAccounts();
		while (enumAccounts.hasMoreElements())
		{
			PayAccount a = (PayAccount) enumAccounts.nextElement();
		}
	}

	public void stateChanged(ChangeEvent e)
	{
		if (e.getSource() instanceof AccountCreator)
		{
			m_MyTableModel.fireTableDataChanged();
		}
	}

	/**
	 * Table model implementation
	 */
	private class MyTableModel extends AbstractTableModel
	{
		PayAccountsFile m_accounts = PayAccountsFile.getInstance();

		public int getColumnCount()
		{
			return 5;
		}

		public int getRowCount()
		{
			return m_accounts.getNumAccounts();
		}

		public Class getColumnClass(int c)
		{
			switch (c)
			{
				case 0:
					return Object.class;
				case 1:
					return java.sql.Timestamp.class;
				case 2:
					return Long.class;
				case 3:
					return java.sql.Timestamp.class;
				case 4:
				default:
					return Object.class;
			}
		}

		public Object getValueAt(int rowIndex, int columnIndex)
		{
			PayAccount account = m_accounts.getAccountAt(rowIndex);
			switch (columnIndex)
			{
				case 0:
					return new Long(account.getAccountNumber());
				case 1:
					return account.getCreationTime();
				case 2:
					if (account.hasAccountInfo())
					{
						return new Long(account.getAccountInfo().getBalance().getCredit());
					}
					else
					{
						return new Long(0);
					}
				case 3:
					if (account.hasAccountInfo())
					{
						return account.getAccountInfo().getBalance().getValidTime();
					}
					else
					{
						return new Timestamp(0);
					}
				case 4:
					if (account.equals(m_accounts.getActiveAccount()))
					{
						return JAPMessages.getString("active");
					}
					else
					{
						return "-----";
					}
				default:
					return JAPMessages.getString("unknown");
			}
		}

		public String getColumnName(int col)
		{
			switch (col)
			{
				case 0:
					return JAPMessages.getString("accountNr");
				case 1:
					return JAPMessages.getString("creationDate");
				case 2:
					return JAPMessages.getString("credit");
				case 3:
					return JAPMessages.getString("validTo");
				case 4:
					return JAPMessages.getString("active");
				default:
					return "---";
			}
		}

		public boolean isCellEditable(int col, int row)
		{
			return false;
		}
	}
}
