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

import java.io.File;
import java.io.FileOutputStream;
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
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileFilter;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import anon.crypto.XMLEncryption;
import anon.util.XMLUtil;
import jap.AbstractJAPConfModule;
import jap.JAPConstants;
import jap.JAPController;
import jap.JAPMessages;
import jap.JAPWaitSplash;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import anon.pay.Pay;
import anon.pay.PayAccount;
import anon.pay.PayAccountsFile;
import anon.pay.xml.XMLAccountInfo;
import anon.pay.xml.XMLTransCert;
import anon.pay.xml.XMLBalance;

/**
 * The Jap Conf Module (Settings Tab Page) for the Accounts and payment Management
 *
 * @author Bastian Voigt
 * @version 1.0
 */
public class AccountSettingsPanel extends AbstractJAPConfModule
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
	 * @todo internationalize
	 */
	public String getTabTitle()
	{
		return "Payment";
	}

	/**
	 * recreateRootPanel - recreates all GUI elements
	 */
	public void recreateRootPanel()
	{
		JPanel rootPanel = getRootPanel();

		/* clear the whole root panel */
		rootPanel.removeAll();
		rootPanel.setLayout(new BorderLayout());

		JPanel centerPanel = new JPanel(new BorderLayout());

		m_Table = new JTable();
		m_MyTableModel = new MyTableModel();
		m_Table.setModel(m_MyTableModel);
		JTableHeader header = m_Table.getTableHeader();
		centerPanel.add(new JScrollPane(m_Table), BorderLayout.CENTER);

		JPanel eastPanel = new JPanel(new GridLayout(16, 1));
		ActionListener myActionListener = new MyActionListener();
		eastPanel.setBorder(new EmptyBorder(5, 5, 5, 5));

		m_btnCreateAccount = new JButton("Create Account");
		m_btnCreateAccount.addActionListener(myActionListener);
		eastPanel.add(m_btnCreateAccount);

		m_btnChargeAccount = new JButton("Charge");
		m_btnChargeAccount.addActionListener(myActionListener);
		eastPanel.add(m_btnChargeAccount);

		m_btnStatement = new JButton("Get Accountstatement");
		m_btnStatement.addActionListener(myActionListener);
		eastPanel.add(m_btnStatement);

		m_btnShowDetails = new JButton("Show Details");
		m_btnShowDetails.addActionListener(myActionListener);
		eastPanel.add(m_btnShowDetails);

		m_btnDeleteAccount = new JButton("Delete account");
		m_btnDeleteAccount.addActionListener(myActionListener);
		eastPanel.add(m_btnDeleteAccount);

		m_btnExportAccount = new JButton("Export account");
		m_btnExportAccount.addActionListener(myActionListener);
		eastPanel.add(m_btnExportAccount);

		m_btnImportAccount = new JButton("Import account");
		m_btnImportAccount.addActionListener(myActionListener);
		eastPanel.add(m_btnImportAccount);

		m_btnActivate = new JButton("Activate account");
		m_btnActivate.addActionListener(myActionListener);
		eastPanel.add(m_btnActivate);

		centerPanel.add(eastPanel, BorderLayout.EAST);
		rootPanel.add(centerPanel, BorderLayout.CENTER);
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
				view,
				"<html>Sie haben f&uuml;r dieses Konto noch keinen Auszug geholt.<br>" +
				"M&ouml;chten Sie jetzt einen Kontoauszug bei der<br>" +
				"Bezahlinstanz anfordern?</html>",
				"Kein Kontoauszug vorhanden",
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
					view,
					"<html>Der aktuelle Kontoauszug ist vom " + t + "<br>" +
					"M&ouml;chten Sie jetzt einen neuen Auszug anfordern?",
					"Kontoauszug anfordern",
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
		JOptionPane.showMessageDialog(
			view,
			"<html><h3>Kontoauszug vom " + balance.getTimestamp() + "</h3>" +
			"<table>" +
			"<tr><td>Kontonummer</td><td>" + selectedAccount.getAccountNumber() + "</td></tr>" +
			"<tr><td>Konto erzeugt am</td><td>" + selectedAccount.getCreationDate() + "</td></tr>" +
			"<tr><td> </td></tr>" +
			"<tr><td>Eingezahlt</td><td>" + balance.getDeposit() + "</td></tr>" +
			"<tr><td>Verbraucht</td><td>" + balance.getSpent() + "</td></tr>" +
			"<tr><td>aktueller Kontostand</td><td>" + (balance.getDeposit()-balance.getSpent()) + "</td></tr>" +
			"<tr><td>Guthaben g&uuml;ltig bis</td><td>" + balance.getValidTime() + "</td></tr>" +
			"</table></html>",
			"Detaillierte Kontoinformationen",
			JOptionPane.INFORMATION_MESSAGE
			);
	}

	/**
	 * Tries to find the right account
	 * @return PayAccount
	 */
	private PayAccount getSelectedAccount()
	{
		PayAccountsFile accounts = PayAccountsFile.getInstance();
		JFrame view = JAPController.getView();

		// try to find the account the user wishes to charge
		int selectedRow = m_Table.getSelectedRow();
		if (selectedRow < 0)
		{
			int numAccounts = accounts.getNumAccounts();
			if (numAccounts == 0)
			{
				// todo: internationalize message
				int choice = JOptionPane.showOptionDialog(
					view,
					"<html>Sie haben noch kein Konto.<br>" +
					"Bevor Sie diese Aktion ausf&uuml;hren k&ouml;nnen, m&uuml;ssen Sie<br>" +
					"erst ein Konto anlegen.<br>M&ouml;chten Sie das jetzt tun?</html>",
					"Noch kein Konto vorhanden",
					JOptionPane.YES_NO_OPTION,
					JOptionPane.QUESTION_MESSAGE,
					null, null, null
					);
				if (choice == JOptionPane.YES_OPTION)
				{
					if (doCreateAccount())
					{
						selectedRow = 0;
					}
					else
					{
						return null;
					}
				}
				else
				{
					return null;
				}
			}
			else if (numAccounts == 1)
			{
				selectedRow = 0;
			}
			else
			{
				// todo: internationalize message
				JOptionPane.showMessageDialog(
					view,
					"<html>Sie haben mehrere Konten. Bitte w&auml;hlen Sie eine Zeile<br>" +
					"in der Tabelle aus.<br>",
					"Kein Konto ausgewaehlt",
					JOptionPane.ERROR_MESSAGE
					);
				return null;
			}
		}
		return accounts.getAccountAt(selectedRow);
	}

	/**
	 * Charges the selected account
	 */
	private void doChargeAccount(PayAccount selectedAccount)
	{
		if (selectedAccount == null)
		{
			return;
		}
		JFrame view = JAPController.getView();
		XMLTransCert transferCertificate = null;

		// TODO: internationalize: JAPMessage.get....()
		int choice = JOptionPane.showOptionDialog(
			view,
			"<html>Um Ihr Konto aufzuladen, muss JAP zun&auml;chst eine<br>" +
			"Transaktionsnummer bei der Bezahlinstanz anfordern.<br>" +
			"JAP wird dann versuchen, Ihren Web-Browser zu starten<br>" +
			"damit Sie den Rest der Transaktion komfortabel zB &uuml;ber PayPal<br>" +
			"machen k&ouml;nnen.</html>",
			"Konto aufladen",
			JOptionPane.YES_NO_OPTION,
			JOptionPane.QUESTION_MESSAGE,
			null, null, null
			);
		if (choice == JOptionPane.YES_OPTION)
		{
			JAPWaitSplash splash = null;
			try
			{
				splash = JAPWaitSplash.start("Fetching transfer number...", "Please wait");
				Thread.sleep(5);
				transferCertificate = Pay.getInstance().chargeAccount(selectedAccount.getAccountNumber());
				splash.abort();
			}
			catch (Exception ex)
			{
				splash.abort();
				ex.printStackTrace();
				LogHolder.log(LogLevel.DEBUG, LogType.PAY, "Exception ChargeAccount: " + ex.toString());
				JOptionPane.showMessageDialog(
					view,
					"Error requesting transfer number: " + ex.getMessage(),
					"Error", JOptionPane.ERROR_MESSAGE
					);
				return;
			}

			// try to launch webbrowser (hack!)
			splash = JAPWaitSplash.start("Launching browser...", "Please wait");
			Process runcode = null;
			String[] browser = JAPConstants.BROWSERLIST;
			String url = transferCertificate.getBaseUrl();
			url += "?transfernum=" + transferCertificate.getTransferNumber();
//				url += "&lang="+JAPModel.getLa... todo: add language code
			for (int i = 0; i < browser.length; i++)
			{
				try
				{
					LogHolder.log(LogLevel.DEBUG, LogType.PAY, "Trying to start browser " + browser[i]);
					runcode = Runtime.getRuntime().exec(new String[]
						{browser[i], url});
					break;
				}
				catch (Exception ex)
				{
				}
			}
			if (runcode == null)
			{
				splash.abort();
				JOptionPane.showMessageDialog(
					view,
					"<html>JAP konnte Ihren Browser leider nicht finden<br>" +
					"Bitte starten Sie Ihren Browser manuell und geben Sie die folgende<br>" +
					"Adresse ein:<br><br><h3>" +
					url + "</h3></html>",
					"Browserstart fehlgeschlagen",
					JOptionPane.INFORMATION_MESSAGE
					);
			}
			else
			{
				LogHolder.log(LogLevel.DEBUG, LogType.PAY, "Browser running. Waiting ... ");
				try
				{
					runcode.waitFor();
				}
				catch (InterruptedException ex1)
				{
				}
				LogHolder.log(LogLevel.DEBUG, LogType.PAY, "Browser terminated with exitcode " +
							  runcode.exitValue());
				splash.abort();
			}
			m_MyTableModel.fireTableDataChanged();
		}
	}

	private boolean doCreateAccount()
	{
		JFrame view = JAPController.getView();

		// TODO: JAPMessage.getString()
		int choice = JOptionPane.showOptionDialog(
			view,
			"<html>Um ein Konto anzulegen, muss JAP zun&auml;chst ein<br>" +
			"Schl&uuml;sselpaar erzeugen, das dauert einige Sekunden.<br>" +
			"Danach wird der &ouml;ffentliche Schl&uuml;ssel an die<br>" +
			"Bezahlinstanz geschickt, diese er&ouml;ffnet dann das Konto.<br>" +
			"M&ouml;chten Sie fortfahren?</html>",
			"Konto anlegen",
			JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
			null, null, null
			);
		if (choice == JOptionPane.YES_OPTION)
		{
			JAPWaitSplash splash = null;
			try
			{
				splash = JAPWaitSplash.start(
					JAPMessages.getString("Creating new account.."),
					JAPMessages.getString("Please wait")
					);
				Pay.getInstance().createAccount(true); // always use DSA keys for now
				splash.abort();
			}
			catch (Exception ex)
			{
				splash.abort();
				LogHolder.log(LogLevel.DEBUG, LogType.PAY, "Exception CreateAccount: " + ex.toString());
				JOptionPane.showMessageDialog(
					view,
					JAPMessages.getString("Error creating account: ") + ex.getMessage(),
					JAPMessages.getString("Error"), JOptionPane.ERROR_MESSAGE
					);
				return false;
			}
			m_MyTableModel.fireTableDataChanged();
			return true;
		}
		else
		{
			return false;
		}
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
				JAPMessages.getString("Error"),
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
			Pay.getInstance().fetchAccountInfo(selectedAccount.getAccountNumber());
			splash.abort();
		}
		catch (Exception ex)
		{
			splash.abort();
			LogHolder.log(LogLevel.DEBUG, LogType.PAY, "Exception FetchStatement: " + ex.toString());
			JOptionPane.showMessageDialog(
				view,
				JAPMessages.getString("Error fetching account statement: ") + ex.getMessage(),
				JAPMessages.getString("Error"),
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
	 * @todo: show only asterisks and input password two times!
	 */
	private void doExportAccount(PayAccount selectedAccount)
	{
		if (selectedAccount == null)
		{
			return;
		}
		PayAccountsFile accounts = PayAccountsFile.getInstance();
		JFrame view = JAPController.getView();

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
				//Document doc2 = selectedAccount.toXmlElement(doc);
				Element elemAccount = selectedAccount.toXmlElement(doc);
				//elemAccount = (Element) XMLUtil.importNode(doc, elemAccount, true);
				elemRoot.appendChild(elemAccount);

				int choice = JOptionPane.showOptionDialog(
					view,
					"<html>Die Kontodaten enthalten einen privaten Schl&uuml;ssel,<br>" +
					"der vor fremden Blicken gesch&uuml;tzt sein sollte. Deshalb haben Sir<br>" +
					"jetzt die M&ouml;glichkeit Ihre Kontodaten verschl&uuml;sselt zu exportieren.<br>" +
					"Zugriff auf die Daten ist dann nur noch mit einem Passwort m&ouml;glich.<br><br>" +
					"M&ouml;chten Sie die Kontodaten verschl&uuml;sseln?",
					"Konto exportieren",
					JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
					null, null, null);
				if (choice == JOptionPane.YES_OPTION)
				{
					// todo: show only asterisks and input password two times!!
					String strPassword = JOptionPane.showInputDialog(view, "Bitte Passwort eingeben:");
					XMLEncryption.encryptElement(elemAccount, strPassword);
				}
				String strOutput = XMLUtil.XMLDocumentToString(doc);
				FileOutputStream outStream = new FileOutputStream(f);
				outStream.write(strOutput.getBytes());
				outStream.close();
			}
			catch (Exception e)
			{
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
	 * @todo handle cancel button properly
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
						String strMessage = "Please type a password for decryption";
						String strPassword = "";
						while (true) // todo: handle "cancel" button properly...
						{
							// ask for password
							strPassword = JOptionPane.showInputDialog(
								view, strMessage,
								"Password",
								JOptionPane.QUESTION_MESSAGE | JOptionPane.OK_CANCEL_OPTION
								);
							try
							{
								elemAccount = XMLEncryption.decryptElement(elemCrypt, strPassword);
							}
							catch (Exception ex)
							{
								strMessage = "Bad password. Please type a password for decryption";
								continue;
							}
							break;
						}
					}
				}
			}
			catch (Exception e)
			{
				JOptionPane.showMessageDialog(
					view,
					"Could not import accountfile",
					"Sorry",
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
					"<html>Error importing account:<br>" +
					ex.getMessage() + "</html>",
					"Error",
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
				"<html>Sie haben f&uuml;r dieses Konto noch keinen Kontoauszug.<br>" +
				"M&ouml;chten Sie einen neuen Kontoauszug anfordern, um wirklich sicher zu sein<br>" +
				"dass sich kein Guthaben mehr auf dem Konto befindet?</html>",
				"Konto entfernen",
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
			if (accInfo.getBalance().getTimestamp().getTime() < (System.currentTimeMillis() - 1000 * 60 * 60 * 24))
			{
				int choice = JOptionPane.showOptionDialog(
					view,
					"<html>Der Kontoauszug f&uuml;r dieses Konto ist &auml;lter als einen Tag.<br>" +
					"M&ouml;chten Sie einen neuen Kontoauszug anfordern, um wirklich sicher zu sein<br>" +
					"dass sich kein Guthaben mehr auf dem Konto befindet?</html>",
					"Konto entfernen",
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
				// todo: internationalize message
				int choice = JOptionPane.showOptionDialog(
					view,
					"<html>Auf diesem Konto befindet sich noch ein Guthaben von<br>" +
					accInfo.getBalance().getCredit() + " Bytes<br>" +
					"Das Guthaben geht beim Entfernen unwiederbringlich verloren, <br>" +
					"au&szlig;er Sie haben das Konto zuvor exportiert.<br><br>" +
					"M&ouml;chten Sie dieses Konto wirklich trotzdem entfernen?</html>",
					"Konto entfernen",
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
					"<html>M&ouml;chten Sie dieses Konto wirklich entfernen?</html>",
					"Konto entfernen",
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
				"<html>M&ouml;chten Sie dieses Konto wirklich entfernen?</html>",
				"Konto entfernen",
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
				// konnte nicht l\uFFFDschen
				JOptionPane.showMessageDialog(
					view,
					"<html>Error while deleting: " + ex.getMessage(),
					"Sorry", JOptionPane.ERROR_MESSAGE);
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
		// TODO: JAPController.saveJapConf() or sth similar;
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
			// TODO: Update information in table
		}
	}

	/**
	 * change listener
	 */
	/*	public void accountDataChanged()
	 {
	  if (m_MyTableModel != null)
	  {
	   m_MyTableModel.fireTableDataChanged();
	  }
	 }*/

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

		public Object getValueAt(int rowIndex, int columnIndex)
		{
			PayAccount account = m_accounts.getAccountAt(rowIndex);
			switch (columnIndex)
			{
				case 0:
					return new Long(account.getAccountNumber());
				case 1:
					return account.getCreationDate();
				case 2:
					if (account.hasAccountInfo())
					{
						return new Long(account.getAccountInfo().getBalance().getCredit());
					}
					else
					{
						return JAPMessages.getString("unknown");
					}
				case 3:
					if (account.hasAccountInfo())
					{
						return account.getAccountInfo().getBalance().getValidTime();
					}
					else
					{
						return JAPMessages.getString("unknown");
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
					return "Kontonr.";
				case 1:
					return "Erstellt am";
				case 2:
					return "Guthaben";
				case 3:
					return "Gueltig bis";
				case 4:
					return "aktiv?";
				default:
					return "---";
			}
		}

	}
}
