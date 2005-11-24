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
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
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
import anon.pay.xml.XMLEasyCC;
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

/**
 * The Jap Conf Module (Settings Tab Page) for the Accounts and payment Management
 *
 * @author Bastian Voigt, Tobias Bayer
 * @version 1.0
 */
public class AccountSettingsPanel extends AbstractJAPConfModule implements ChangeListener
{
	private JTable m_Table;
	private JButton m_btnCreateAccount;
	private JButton m_btnChargeAccount;
	private JButton m_btnStatement;
	private JButton m_btnShowDetails;
	private JButton m_btnActivate;
	private JButton m_btnDeleteAccount;
	private JButton m_btnExportAccount;
	private JButton m_btnImportAccount;
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
		PayAccountsFile accounts = PayAccountsFile.getInstance();
		PayAccount activeAccount = accounts.getActiveAccount();

		/* clear the whole root panel */
		rootPanel.removeAll();
		rootPanel.setLayout(new BorderLayout());
		rootPanel.setBorder(new TitledBorder(JAPMessages.getString("ngPseudonymAccounts")));

		JPanel centerPanel = new JPanel(new BorderLayout());

		m_Table = new JTable();
		m_MyTableModel = new MyTableModel();
		m_Table.setModel(m_MyTableModel);
		m_Table.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		m_Table.setDefaultRenderer(java.sql.Timestamp.class, new TimestampCellRenderer(false));
		m_Table.setDefaultRenderer(Long.class, new ByteNumberCellRenderer());
		centerPanel.add(new JScrollPane(m_Table), BorderLayout.CENTER);

		GridLayout gl = new GridLayout(16, 1);
		gl.setVgap(10);
		JPanel eastPanel = new JPanel(gl);

		ActionListener myActionListener = new MyActionListener();

		//Ask to be notified of selection changes.
		ListSelectionModel rowSM = m_Table.getSelectionModel();
		rowSM.addListSelectionListener(new ListSelectionListener()
		{
			public void valueChanged(ListSelectionEvent e)
			{
				enableDisableButtons();
			}
		});

		eastPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

		m_btnCreateAccount = new JButton(JAPMessages.getString("ngCreateAccount"));
		m_btnCreateAccount.addActionListener(myActionListener);
		eastPanel.add(m_btnCreateAccount);

		m_btnChargeAccount = new JButton(JAPMessages.getString("ngChargeAccount"));
		m_btnChargeAccount.addActionListener(myActionListener);
		eastPanel.add(m_btnChargeAccount);
		/** @todo Reactivate*/
		m_btnChargeAccount.setEnabled(true);

		m_btnStatement = new JButton(JAPMessages.getString("ngStatement"));
		m_btnStatement.addActionListener(myActionListener);
		eastPanel.add(m_btnStatement);
		/** @todo Reactivate*/
		m_btnChargeAccount.setEnabled(activeAccount != null);

		m_btnShowDetails = new JButton(JAPMessages.getString("ngAccountDetails"));
		m_btnShowDetails.addActionListener(myActionListener);
		eastPanel.add(m_btnShowDetails);

		m_btnDeleteAccount = new JButton(JAPMessages.getString("ngDeleteAccount"));
		m_btnDeleteAccount.addActionListener(myActionListener);
		eastPanel.add(m_btnDeleteAccount);

		m_btnExportAccount = new JButton(JAPMessages.getString("ngExportAccount"));
		m_btnExportAccount.addActionListener(myActionListener);
		eastPanel.add(m_btnExportAccount);

		m_btnImportAccount = new JButton(JAPMessages.getString("ngImportAccount"));
		m_btnImportAccount.addActionListener(myActionListener);
		eastPanel.add(m_btnImportAccount);

		m_btnActivate = new JButton(JAPMessages.getString("ngActivateAccount"));
		m_btnActivate.addActionListener(myActionListener);
		eastPanel.add(m_btnActivate);

		centerPanel.add(eastPanel, BorderLayout.EAST);
		rootPanel.add(centerPanel, BorderLayout.CENTER);
		enableDisableButtons();
	}

	private void enableDisableButtons()
	{
		boolean enable = (m_Table.getSelectedRow() >= 0);
		/** @todo Reactivate*/
		m_btnChargeAccount.setEnabled(enable);
		m_btnStatement.setEnabled(enable);
		m_btnShowDetails.setEnabled(enable); ;
		if ( (enable) &&
			(PayAccountsFile.getInstance().getActiveAccount() !=
			 PayAccountsFile.getInstance().getAccountAt(m_Table.getSelectedRow())))
		{
			m_btnActivate.setEnabled(true);
		}
		else
		{
			m_btnActivate.setEnabled(false);
		}

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
			else if (source == m_btnShowDetails)
			{
				doShowDetails(getSelectedAccount());
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

		}
	}

	/**
	 * doShowDetails - shows account details in a messagebox
	 */
	private void doShowDetails(PayAccount selectedAccount)
	{
		if (selectedAccount == null)
		{
			return;
		}
		JFrame view = JAPController.getView();
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
			);
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
