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
import java.util.Date;
import java.util.Enumeration;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import java.awt.Color;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentAdapter;
import java.awt.event.ComponentEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import javax.swing.DefaultListCellRenderer;
import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSeparator;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import anon.crypto.JAPCertificate;
import anon.crypto.XMLEncryption;
import anon.pay.BI;
import anon.pay.BIConnection;
import anon.pay.PayAccount;
import anon.pay.PayAccountsFile;
import anon.pay.xml.XMLAccountInfo;
import anon.pay.xml.XMLBalance;
import anon.pay.xml.XMLPaymentOption;
import anon.pay.xml.XMLPaymentOptions;
import anon.pay.xml.XMLTransCert;
import anon.util.ResourceLoader;
import anon.util.XMLUtil;
import gui.GUIUtils;
import gui.JAPHelp;
import gui.JAPMessages;
import gui.dialog.CaptchaContentPane;
import gui.dialog.DialogContentPane;
import gui.dialog.DialogContentPane.Options;
import gui.dialog.IDialogOptions;
import gui.dialog.JAPDialog;
import gui.dialog.PasswordContentPane;
import gui.dialog.SimpleWizardContentPane;
import gui.dialog.WorkerContentPane;
import jap.AbstractJAPConfModule;
import jap.JAPConstants;
import jap.JAPController;
import jap.JAPUtil;
import jap.pay.wizardnew.MethodSelectionPane;
import jap.pay.wizardnew.PassivePaymentPane;
import jap.pay.wizardnew.PaymentInfoPane;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import jap.JAPModel;
import anon.infoservice.ListenerInterface;
import javax.swing.JTabbedPane;
import javax.swing.JCheckBox;

/**
 * The Jap Conf Module (Settings Tab Page) for the Accounts and payment Management
 *
 * @author Bastian Voigt, Tobias Bayer
 * @version 1.0
 */
public class AccountSettingsPanel extends AbstractJAPConfModule implements
	ListSelectionListener
{

	/** Messages */
	private static final String MSG_BUTTON_TRANSACTIONS = AccountSettingsPanel.class.
		getName() + "_button_transactions";
	private static final String MSG_BUTTON_DELETE = AccountSettingsPanel.class.
		getName() + "_button_delete";
	private static final String MSG_BUTTON_EXPORT = AccountSettingsPanel.class.
		getName() + "_button_export";
	private static final String MSG_BUTTONRELOAD = AccountSettingsPanel.class.
		getName() + "_buttonreload";
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
	private static final String MSG_BUTTON_SELECT = AccountSettingsPanel.class.
		getName() + "_button_select";
	private static final String MSG_BUTTON_CHANGE_PASSWORD = AccountSettingsPanel.class.
		getName() + "_button_change_password";
	private static final String MSG_ACCOUNT_INVALID = AccountSettingsPanel.class.
		getName() + "_account_invalid";
	private static final String MSG_ACCOUNTCREATE = AccountSettingsPanel.class.
		getName() + "_accountcreate";
	private static final String MSG_CREATEERROR = AccountSettingsPanel.class.
		getName() + "_createerror";
	private static final String MSG_GETACCOUNTSTATEMENT = AccountSettingsPanel.class.
		getName() + "_getaccountstatement";
	private static final String MSG_GETACCOUNTSTATEMENTTITLE = AccountSettingsPanel.class.
		getName() + "_getaccountstatementtitle";
	private static final String MSG_ACCOUNTCREATEDESC = AccountSettingsPanel.class.
		getName() + "_accountcreatedesc";
	private static final String MSG_ACCPASSWORDTITLE = AccountSettingsPanel.class.
		getName() + "_accpasswordtitle";
	private static final String MSG_ACCPASSWORD = AccountSettingsPanel.class.
		getName() + "_accpassword";
	private static final String MSG_OLDSTATEMENT = AccountSettingsPanel.class.
		getName() + "_oldstatement";
	private static final String MSG_EXPORTENCRYPT = AccountSettingsPanel.class.
		getName() + "_exportencrypt";
	private static final String MSG_EXPORTED = AccountSettingsPanel.class.
		getName() + "_exported";
	private static final String MSG_NOTEXPORTED = AccountSettingsPanel.class.
		getName() + "_notexported";
	private static final String MSG_CONNECTIONACTIVE = AccountSettingsPanel.class.
		getName() + "_connectionactive";
	private static final String MSG_FETCHINGOPTIONS = AccountSettingsPanel.class.
		getName() + "_fetchingoptions";
	private static final String MSG_FETCHINGTAN = AccountSettingsPanel.class.
		getName() + "_fetchingtan";
	private static final String MSG_CHARGEWELCOME = AccountSettingsPanel.class.
		getName() + "_chargewelcome";
	private static final String MSG_CHARGETITLE = AccountSettingsPanel.class.
		getName() + "_chargetitle";
	private static final String MSG_SENDINGPASSIVE = AccountSettingsPanel.class.
		getName() + "_sendingpassive";
	private static final String MSG_SENTPASSIVE = AccountSettingsPanel.class.
		getName() + "_sentpassive";
	private static final String MSG_NOTSENTPASSIVE = AccountSettingsPanel.class.
		getName() + "_notsentpassive";
	private static final String MSG_NEWCAPTCHA = AccountSettingsPanel.class.
		getName() + "_newcaptcha";
	private static final String MSG_NEWCAPTCHAEASTEREGG = AccountSettingsPanel.class.
		getName() + "_newcaptchaEasterEgg";
	private static final String MSG_SHOW_PAYMENT_CONFIRM_DIALOG = AccountSettingsPanel.class.
		getName() + "_showPaymentConfirmDialog";

	private JButton m_btnCreateAccount;
	private JButton m_btnChargeAccount;
	private JButton m_btnStatement;
	private JButton m_btnActivate;
	private JButton m_btnDeleteAccount;
	private JButton m_btnExportAccount;
	private JButton m_btnImportAccount;
	private JButton m_btnTransactions;
	private JButton m_btnSelect;
	private JButton m_btnPassword;
	private JButton m_btnReload;
	private JCheckBox m_cbxShowPaymentConfirmation;

	private JLabel m_labelCreationDate;
	private JLabel m_labelStatementDate;
	private JLabel m_labelDeposit;
	private JLabel m_labelSpent;
	private JLabel m_labelBalance;
	private JLabel m_labelValid;
	private JProgressBar m_coinstack;
	private JList m_listAccounts;
	private boolean m_bReady = true;
	private boolean m_bCreatingAccount = false;

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

		/* insert all components in the root panel */
		JTabbedPane tabPane = new JTabbedPane();
		tabPane.setFont(getFontSetting());
		tabPane.insertTab(JAPMessages.getString("ngPseudonymAccounts"),
						  null, createBasicSettingsTab(), null, 0);
		tabPane.insertTab(JAPMessages.getString(
			"settingsInfoServiceConfigAdvancedSettingsTabTitle"), null, createAdvancedSettingsTab(), null, 1);

		GridBagLayout rootPanelLayout = new GridBagLayout();
		rootPanel.setLayout(rootPanelLayout);
		rootPanelLayout.setConstraints(tabPane, createTabbedRootPanelContraints());
		rootPanel.add(tabPane);
  }
	private JPanel createBasicSettingsTab()
	{
		JPanel rootPanel = new JPanel();

		rootPanel.setLayout(new GridBagLayout());

		m_listAccounts = new JList();
		m_listAccounts.addListSelectionListener(this);
		m_listAccounts.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		m_listAccounts.setPreferredSize(new Dimension(200, 200));
		m_listAccounts.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent e)
			{
				//Activate account on double click
				if (e.getClickCount() == 2)
				{
					doActivateAccount(getSelectedAccount());
				}
			}
		}
		);

		ActionListener myActionListener = new MyActionListener();

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
		m_btnPassword = new JButton(JAPMessages.getString(MSG_BUTTON_CHANGE_PASSWORD));
		m_btnPassword.addActionListener(myActionListener);
		buttonsPanel.add(m_btnPassword, c);

		c.gridx++;
		m_btnImportAccount = new JButton(JAPMessages.getString("ngImportAccount"));
		m_btnImportAccount.addActionListener(myActionListener);
		buttonsPanel.add(m_btnImportAccount, c);

		c.gridx++;
		c.weightx = 1;
		c.weighty = 1;
		m_btnSelect = new JButton(JAPMessages.getString(MSG_BUTTON_SELECT));
		m_btnSelect.addActionListener(myActionListener);
		buttonsPanel.add(m_btnSelect, c);

		c = new GridBagConstraints();
		c.fill = c.NONE;
		c.anchor = c.NORTHWEST;
		c.weightx = 0;
		c.weighty = 0;
		c.gridx = 0;
		c.gridy = 0;
		c.gridheight = 3;
		c.insets = new Insets(5, 5, 5, 5);
		rootPanel.add(m_listAccounts, c);

		c.gridx++;
		c.gridheight = 1;
		rootPanel.add(new JLabel(JAPMessages.getString(MSG_ACCOUNT_CREATION_DATE)), c);
		c.gridx++;
		m_labelCreationDate = new JLabel();
		rootPanel.add(m_labelCreationDate, c);

		c.gridx--;
		c.gridy++;
		rootPanel.add(new JLabel(JAPMessages.getString(MSG_ACCOUNT_STATEMENT_DATE)), c);
		c.gridx++;
		m_labelStatementDate = new JLabel();
		rootPanel.add(m_labelStatementDate, c);

		c.gridx--;
		c.gridy++;
		rootPanel.add(new JLabel(JAPMessages.getString(MSG_ACCOUNT_VALID)), c);
		c.gridx++;
		m_labelValid = new JLabel();
		rootPanel.add(m_labelValid, c);

		c.gridy++;
		c.gridx = 0;
		c.gridwidth = 3;
		rootPanel.add(buttonsPanel, c);

		c.gridy++;
		JSeparator sep = new JSeparator(JSeparator.HORIZONTAL);
		sep.setPreferredSize(new Dimension(520, 10));
		rootPanel.add(sep, c);

		c.weightx = 1;
		c.weighty = 1;
		c.gridy++;
		c.fill = c.HORIZONTAL;
		rootPanel.add(this.createDetailsPanel(), c);

		updateAccountList();
		enableDisableButtons();

		return rootPanel;
	}

	private JPanel createAdvancedSettingsTab()
	{
		JPanel panelAdvanced = new JPanel();

		m_cbxShowPaymentConfirmation = new JCheckBox(
			JAPMessages.getString(MSG_SHOW_PAYMENT_CONFIRM_DIALOG));
		m_cbxShowPaymentConfirmation.setFont(getFontSetting());
		GridBagLayout advancedPanelLayout = new GridBagLayout();
		panelAdvanced.setLayout(advancedPanelLayout);

		GridBagConstraints advancedPanelConstraints = new GridBagConstraints();
		advancedPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
		advancedPanelConstraints.fill = GridBagConstraints.NONE;
		advancedPanelConstraints.weightx = 1.0;

		advancedPanelConstraints.gridx = 0;
		advancedPanelConstraints.gridy = 0;
		advancedPanelConstraints.weighty = 1.0;
		advancedPanelConstraints.insets = new Insets(5, 5, 10, 5);

		panelAdvanced.add(m_cbxShowPaymentConfirmation, advancedPanelConstraints);

		return panelAdvanced;
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
		c.gridwidth = 2;
		p.add(new JLabel(JAPMessages.getString(MSG_ACCOUNT_DETAILS)), c);

		c.gridwidth = 1;
		c.insets = new Insets(5, 10, 5, 5);
		c.gridy++;
		c.gridheight = 3;
		m_coinstack = new JProgressBar(0, 8);
		m_coinstack.setUI(new CoinstackProgressBarUI(GUIUtils.loadImageIcon(JAPConstants.IMAGE_COIN_COINSTACK, true),
			0, 8));
		p.add(m_coinstack, c);

		c.gridheight = 1;
		c.gridx++;
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
		p.add(new JLabel(JAPMessages.getString(MSG_ACCOUNT_BALANCE) + ":"), c);
		c.gridx++;
		m_labelBalance = new JLabel();
		p.add(m_labelBalance, c);

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
		m_btnReload = new JButton(JAPMessages.getString(MSG_BUTTONRELOAD));
		m_btnReload.addActionListener(myActionListener);
		buttonsPanel.add(m_btnReload, d);

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
		c.gridwidth = 3;
		p.add(buttonsPanel, c);

		return p;
	}

	private void updateAccountList()
	{
		PayAccount account;
		DefaultListModel listModel = new DefaultListModel();
		Enumeration accounts = PayAccountsFile.getInstance().getAccounts();
		int selectedItem = m_listAccounts.getSelectedIndex();

		CustomRenderer cr = new CustomRenderer();
		m_listAccounts.setCellRenderer(cr);

		while (accounts.hasMoreElements())
		{
			account = (PayAccount) accounts.nextElement();
			listModel.addElement(account);
		}

		m_listAccounts.setModel(listModel);
		m_listAccounts.revalidate();

		if (selectedItem != -1)
		{
			m_listAccounts.setSelectedIndex(selectedItem);
		}
		else
		{
			m_listAccounts.setSelectedIndex(0);
		}
	}

	private void enableDisableButtons()
	{
		if (m_listAccounts.getModel().getSize() > 0)
		{
			boolean enable = (getSelectedAccount() != null);
			m_btnChargeAccount.setEnabled(enable);
			m_btnTransactions.setEnabled(enable);
			m_btnDeleteAccount.setEnabled(enable);
			m_btnExportAccount.setEnabled(enable);
			m_btnReload.setEnabled(enable);
		}
		else
		{
			m_btnChargeAccount.setEnabled(false);
			m_btnTransactions.setEnabled(false);
			m_btnDeleteAccount.setEnabled(false);
			m_btnExportAccount.setEnabled(false);
			m_btnReload.setEnabled(false);
		}
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
			else if (source == m_btnSelect)
			{
				doActivateAccount(getSelectedAccount());
			}
			else if (source == m_btnPassword)
			{
				doChangePassword();
			}
			else if (source == m_btnReload)
			{
				doGetStatement( (PayAccount) m_listAccounts.getSelectedValue());
			}

		}
	}

	/**
	 * Asks the user for a new payment password
	 */
	private void doChangePassword()
	{
		if (JAPController.getInstance().getPaymentPassword() != null)
		{
			JAPDialog d = new JAPDialog(GUIUtils.getParentWindow(this.getRootPanel()),
										JAPMessages.getString(MSG_ACCPASSWORDTITLE), true);
			PasswordContentPane p = new PasswordContentPane(d, PasswordContentPane.PASSWORD_CHANGE, "")
			{
				public char[] getComparedPassword()
				{
					return JAPController.getInstance().getPaymentPassword().toCharArray();
				}
			};

			p.updateDialog();
			d.pack();
			d.setVisible(true);
			if (p.getButtonValue() != PasswordContentPane.RETURN_VALUE_CANCEL &&
				p.getButtonValue() != PasswordContentPane.RETURN_VALUE_CLOSED)
			{
				JAPController.getInstance().setPaymentPassword(new String(p.getPassword()));
			}
		}
		else
		{
			JAPDialog d = new JAPDialog(GUIUtils.getParentWindow(this.getRootPanel()),
										JAPMessages.getString(MSG_ACCPASSWORDTITLE), true);
			PasswordContentPane p = new PasswordContentPane(d, PasswordContentPane.PASSWORD_NEW, "");
			p.updateDialog();
			d.pack();
			d.setVisible(true);
			if (p.getButtonValue() != PasswordContentPane.RETURN_VALUE_CANCEL &&
				p.getButtonValue() != PasswordContentPane.RETURN_VALUE_CLOSED)
			{
				JAPController.getInstance().setPaymentPassword(new String(p.getPassword()));
			}
		}
	}

	/**
	 * Shows transaction numbers and if they have been used
	 * @param a_account PayAccount
	 */
	private void doShowTransactions(PayAccount a_account)
	{
			TransactionOverviewDialog d = new TransactionOverviewDialog(this,
				JAPMessages.getString(MSG_TRANSACTION_OVERVIEW_DIALOG), true, a_account);

	}

	/**
	 * doShowDetails - shows account details in the details panel
	 */
	private void doShowDetails(PayAccount selectedAccount)
	{
		if (selectedAccount == null)
		{
			m_coinstack.setValue(0);
			m_labelCreationDate.setText("");
			m_labelStatementDate.setText("");
			m_labelDeposit.setText("");
			m_labelSpent.setText("");
			m_labelBalance.setText("");
			m_labelValid.setText("");
			return;
		}

		/** If there is no account info or the account info is older than 24 hours,
		 * fetch a new statement from the Payment Instance.
		 */
		/*if (!selectedAccount.hasAccountInfo() ||
		 (selectedAccount.getAccountInfo().getBalance().getTimestamp().getTime() <
		  (System.currentTimeMillis() - 1000 * 60 * 60 * 24)))
		   {
		 doGetStatement(selectedAccount);
		   }*/

		XMLAccountInfo accountInfo = selectedAccount.getAccountInfo();
		if (accountInfo != null)
		{
			XMLBalance balance = accountInfo.getBalance();

			m_labelCreationDate.setText(JAPUtil.formatTimestamp(selectedAccount.getCreationTime(), false,
				JAPController.getInstance().getLocale().getLanguage()));
			m_labelStatementDate.setText(JAPUtil.formatTimestamp(balance.getTimestamp(), true,
				JAPController.getInstance().getLocale().getLanguage()));
			m_labelDeposit.setText(JAPUtil.formatBytesValue(balance.getDeposit()));
			m_labelSpent.setText(JAPUtil.formatBytesValue(balance.getSpent()));
			m_labelBalance.setText(JAPUtil.formatBytesValue(balance.getDeposit() - balance.getSpent()));
			m_labelValid.setText(JAPUtil.formatTimestamp(balance.getValidTime(), true,
				JAPController.getInstance().getLocale().getLanguage()));

			long dep = selectedAccount.getDeposit();
			long spe = selectedAccount.getSpent();
			if (dep == 0 || dep - spe == 0)
			{
				m_coinstack.setValue(0);
			}
			else
			{
				double onePercent = 100.0 / (double) dep;
				long percent = (long) (onePercent * spe);
				if (percent < 12)
				{
					m_coinstack.setValue(8);
				}
				else if (percent >= 12 && percent < 25)
				{
					m_coinstack.setValue(7);
				}
				else if (percent >= 25 && percent < 37)
				{
					m_coinstack.setValue(6);
				}
				else if (percent >= 37 && percent < 50)
				{
					m_coinstack.setValue(5);
				}
				else if (percent >= 50 && percent < 62)
				{
					m_coinstack.setValue(4);
				}
				else if (percent >= 62 && percent < 75)
				{
					m_coinstack.setValue(3);
				}
				else if (percent >= 75 && percent < 87)
				{
					m_coinstack.setValue(2);
				}
				else if (percent >= 87 && percent < 99)
				{
					m_coinstack.setValue(1);
				}
				else
				{
					m_coinstack.setValue(0);
				}
			}
		}
		else
		{
			m_coinstack.setValue(0);
			m_labelCreationDate.setText("");
			m_labelStatementDate.setText("");
			m_labelDeposit.setText("");
			m_labelSpent.setText("");
			m_labelBalance.setText("");
			m_labelValid.setText("");
		}

	}

	/**
	 * returns the selected (active) account
	 * @return PayAccount
	 */
	private PayAccount getSelectedAccount()
	{
		return (PayAccount) m_listAccounts.getSelectedValue();
	}

	/**
	 * Charges the selected account
	 */
	private void doChargeAccount(final PayAccount selectedAccount)
	{
			if (selectedAccount == null)
			{
				return;
			}

			if (selectedAccount.getBalanceValidTime().before(new Date()))
			{
				JAPDialog.showMessageDialog(GUIUtils.getParentWindow(getRootPanel()),
											JAPMessages.getString(MSG_ACCOUNT_INVALID));
				return;
			}

			final JAPDialog d = new JAPDialog(GUIUtils.getParentWindow(this.getRootPanel()),
											  JAPMessages.getString(MSG_CHARGETITLE), true);
			d.setResizable(true);
			SimpleWizardContentPane welcomePane = new SimpleWizardContentPane(d,
				JAPMessages.getString(MSG_CHARGEWELCOME), null, null);

			WorkerContentPane.ReturnThread fetchOptions = new WorkerContentPane.ReturnThread()
			{
				private XMLPaymentOptions m_paymentOptions;
				private boolean m_interrupted = false;
				public void run()
				{
					try
					{
						BI pi = selectedAccount.getBI();
						BIConnection piConn = new BIConnection(pi);

						piConn.connect(JAPModel.getInstance().getProxyInterface());
						piConn.authenticate(PayAccountsFile.getInstance().getActiveAccount().
											getAccountCertificate(),
											PayAccountsFile.getInstance().getActiveAccount().
											getSigningInstance());
						LogHolder.log(LogLevel.DEBUG, LogType.PAY, "Fetching payment options");
						m_paymentOptions = piConn.getPaymentOptions();
						piConn.disconnect();
					}
					catch (Exception e)
					{
						LogHolder.log(LogLevel.EXCEPTION, LogType.NET,
									  "Error fetching payment options: " + e.getMessage());
						showPIerror(d);
						m_interrupted = true;
					}
				}

				public boolean isInterrupted()
				{
					if (super.isInterrupted())
					{
						return true;
					}
					return m_interrupted;
				}

				public Object getValue()
				{
					return m_paymentOptions;
				}
			};

			final WorkerContentPane fetchOptionsPane = new WorkerContentPane(d,
				JAPMessages.getString(MSG_FETCHINGOPTIONS), welcomePane,
				fetchOptions);
			fetchOptionsPane.setInterruptThreadSafe(false);

			final MethodSelectionPane methodSelectionPane = new MethodSelectionPane(d, fetchOptionsPane);

			WorkerContentPane.ReturnThread fetchTan = new WorkerContentPane.ReturnThread()
			{
				private XMLTransCert m_transCert;
				public void run()
				{
					if (m_transCert == null)
					{
						try
						{
							LogHolder.log(LogLevel.DEBUG, LogType.PAY,
										  "Fetching Transaction Certificate from Payment Instance");

							m_transCert = selectedAccount.charge(JAPModel.getInstance().getProxyInterface());
						}
						catch (Exception e)
						{
							LogHolder.log(LogLevel.EXCEPTION, LogType.NET,
										  "Error fetching TransCert: " + e.getMessage());
							showPIerror(GUIUtils.getParentWindow(getRootPanel()));
						}
					}
				}

				public Object getValue()
				{
					return m_transCert;
				}
			};

			WorkerContentPane fetchTanPane = new WorkerContentPane(d, JAPMessages.getString(MSG_FETCHINGTAN),
				methodSelectionPane,
				fetchTan);
			fetchTanPane.setInterruptThreadSafe(false);

			final PaymentInfoPane paymentInfoPane = new PaymentInfoPane(d, fetchTanPane)
			{
				public boolean isSkippedAsNextContentPane()
				{
					if (methodSelectionPane.getSelectedPaymentOption().getType().equalsIgnoreCase(
						XMLPaymentOption.OPTION_ACTIVE))
					{
						return false;
					}
					else
					{
						return true;
					}
				}

				public boolean isSkippedAsPreviousContentPane()
				{
					return true;
				}
			};

			final PassivePaymentPane passivePaymentPane = new PassivePaymentPane(d, paymentInfoPane)
			{

				public boolean isSkippedAsNextContentPane()
				{
					if (methodSelectionPane.getSelectedPaymentOption().getType().equalsIgnoreCase(
						XMLPaymentOption.OPTION_ACTIVE))
					{
						return true;
					}
					else
					{
						return false;
					}
				}
			};

			WorkerContentPane.ReturnThread sendPassive = new WorkerContentPane.ReturnThread()
			{
				private Boolean m_successful = new Boolean(true);

				public void run()
				{
					/** Post data to payment instance */
					BIConnection biConn = new BIConnection(selectedAccount.getBI());
					try
					{
						biConn.connect(JAPModel.getInstance().getProxyInterface());
						biConn.authenticate(selectedAccount.getAccountCertificate(),
											selectedAccount.getSigningInstance());
						if (!biConn.sendPassivePayment(passivePaymentPane.getEnteredInfo()))
						{
							m_successful = new Boolean(false);
						}
						biConn.disconnect();
					}
					catch (Exception e)
					{
						m_successful = new Boolean(false);
						LogHolder.log(LogLevel.EXCEPTION, LogType.PAY,
									  "Could not send PassivePayment to payment instance: " + e.getMessage());
						showPIerror(GUIUtils.getParentWindow(getRootPanel()));
						d.dispose();
					}
				}

				public Object getValue()
				{
					return m_successful;
				}
			};

			final WorkerContentPane sendPassivePane = new WorkerContentPane(d,
				JAPMessages.getString(MSG_SENDINGPASSIVE),
				passivePaymentPane,
				sendPassive)
			{
				public boolean isSkippedAsNextContentPane()
				{
					if (methodSelectionPane.getSelectedPaymentOption().getType().equalsIgnoreCase(
						XMLPaymentOption.OPTION_ACTIVE))
					{
						return true;
					}
					else
					{
						return false;
					}
				}
			};
			sendPassivePane.getButtonCancel().setVisible(false);

			final SimpleWizardContentPane sentPane = new SimpleWizardContentPane(d,
				JAPMessages.getString(MSG_SENTPASSIVE), null, new Options(sendPassivePane))
			{
				public boolean isSkippedAsNextContentPane()
				{
					if (methodSelectionPane.getSelectedPaymentOption().getType().equalsIgnoreCase(
						XMLPaymentOption.OPTION_ACTIVE))
					{
						return true;
					}
					else
					{
						return false;
					}
				}
			};
			sentPane.addComponentListener(new ComponentAdapter()
			{
				public void componentShown(ComponentEvent a_event)
				{
					if ( ( (Boolean) sendPassivePane.getValue()).booleanValue() == false)
					{
						sentPane.setText(JAPMessages.getString(MSG_NOTSENTPASSIVE));
					}
				}
			});
			sentPane.getButtonCancel().setVisible(false);
			sentPane.getButtonNo().setVisible(false);

			welcomePane.updateDialogOptimalSized(welcomePane);
			d.setLocationCenteredOnOwner();
			d.setVisible(true);

			if (methodSelectionPane.getSelectedPaymentOption() != null &&
				passivePaymentPane.getButtonValue() == JAPDialog.RETURN_VALUE_OK)
			{
				if (methodSelectionPane.getSelectedPaymentOption().getName().toLowerCase().indexOf(
					"creditcard") != -1)
				{
					doGetStatement(selectedAccount);
				}
			}

	}

	/**
	 *
	 * @return boolean
	 */
	private void doCreateAccount()
	{
			boolean reachable = true;
			int numAccounts = m_listAccounts.getModel().getSize();
			BI theBI = null;

			//First try and get the standard PI the preferred way
			try
			{
				theBI = PayAccountsFile.getInstance().getBI(JAPConstants.PI_ID);
			}
			catch (Exception e)
			{
				LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, e);
			}

			//Try and construct a new PI
			if (theBI == null)
			{
				ListenerInterface li = new ListenerInterface(JAPConstants.PI_HOST, JAPConstants.PI_PORT);
				try
				{
					theBI = new BI(JAPConstants.PI_ID, JAPConstants.PI_NAME, li.toVector(),
								   JAPCertificate.getInstance(ResourceLoader.loadResource(JAPConstants.
						CERTSPATH +
						JAPConstants.PI_CERT)));
				}
				catch (Exception e)
				{
					LogHolder.log(LogLevel.EXCEPTION, LogType.PAY,
								  "Could not create Test-PI: " + e.getMessage());
					theBI = getBIforAccountCreation();
				}
			}

			if (theBI != null)
			{
				try
				{
					//Check if payment instance is reachable
					BIConnection biconn = new BIConnection(theBI);
					biconn.connect(JAPModel.getInstance().getProxyInterface());
					biconn.disconnect();
				}
				catch (Exception e)
				{
					reachable = false;
				}

				if (reachable)
				{
					final JAPDialog d = new JAPDialog(getRootPanel(), JAPMessages.getString(MSG_ACCOUNTCREATE), true);
					d.setDefaultCloseOperation(JAPDialog.DO_NOTHING_ON_CLOSE);
					d.setResizable(true);

					SimpleWizardContentPane panel1 = new SimpleWizardContentPane(d,
						JAPMessages.getString("ngCreateKeyPair"), null, null);

					final BI bi = theBI;
					m_bReady = true;

					Thread doIt = new Thread()
					{
						public void run()
						{
							m_bCreatingAccount = true;
							m_bReady = false;
							try
							{
								PayAccount p = PayAccountsFile.getInstance().createAccount(bi, true,
									JAPModel.getInstance().getProxyInterface());
								p.fetchAccountInfo(JAPModel.getInstance().getProxyInterface());
							}
							catch (Exception ex)
							{
								//User has pressed cancel
								if (!ex.getMessage().equals("CAPTCHA"))
								{
									showPIerror(d);
									this.interrupt();
								}
							}
							m_bCreatingAccount = false;
						}
					};
					WorkerContentPane panel2 = new WorkerContentPane(d,
						JAPMessages.getString(MSG_ACCOUNTCREATEDESC), panel1,
						doIt)
					{
						public boolean isReady()
						{
							return m_bReady;
						}

						public boolean isSkippedAsPreviousContentPane()
						{
							return false;
						}
					};
					panel2.setDefaultButtonOperation(WorkerContentPane.ON_CLICK_DISPOSE_DIALOG);
					panel2.getButtonCancel().setEnabled(false);
					panel2.setInterruptThreadSafe(false);

					final CaptchaContentPane captcha = new CaptchaContentPane(d, panel2);
					Date today = new Date();
					if ( ( (today.getDate() == 27 && today.getMonth() == 8) ||
						  (today.getDate() == 4 && today.getMonth() == 10)))
					{
						captcha.getButtonNo().setText(JAPMessages.getString(MSG_NEWCAPTCHAEASTEREGG));
					}
					else
					{
						captcha.getButtonNo().setText(JAPMessages.getString(MSG_NEWCAPTCHA));
					}

					PayAccountsFile.getInstance().addPaymentListener(captcha);
					captcha.addComponentListener(new ComponentAdapter()
					{
						public void componentShown(ComponentEvent a_event)
						{
							m_bCreatingAccount = false;
						}
					});

					PasswordContentPane pc = null;
					//First account, ask for password
					if (PayAccountsFile.getInstance().getNumAccounts() == 0)
					{
						pc = new PasswordContentPane(d, captcha,
							PasswordContentPane.PASSWORD_NEW,
							JAPMessages.getString(MSG_ACCPASSWORD))
						{
							public boolean isSkippedAsNextContentPane()
							{
								if (PayAccountsFile.getInstance().getNumAccounts() > 0)
								{
									return false;
								}
								else
								{
									return true;
								}
							}
						};
						pc.getButtonNo().setEnabled(false);
						pc.getButtonCancel().setEnabled(false);
					}

					panel1.updateDialogOptimalSized(panel1);

					d.addWindowListener(new WindowAdapter()
					{
						public void windowClosing(WindowEvent e)
						{
							if (!m_bCreatingAccount)
							{
								if (captcha.isVisible())
								{
									captcha.setButtonValue(IDialogOptions.RETURN_VALUE_CLOSED);
									captcha.checkCancel();
								}
								d.dispose();
							}
						}
					});

					d.setLocationCenteredOnOwner();
					m_bCreatingAccount = false;
					d.setVisible(true);

					updateAccountList();
					if (pc != null && pc.getPassword() != null)
					{
						JAPController.getInstance().setPaymentPassword(new String(pc.getPassword()));
					}
					PayAccountsFile.getInstance().removePaymentListener(captcha);

					/** Did the user finish the account creation successfully? */
					if (numAccounts < m_listAccounts.getModel().getSize())
					{
						/** Select new account and start charging wizard */
						m_listAccounts.setSelectedIndex(m_listAccounts.getModel().getSize() - 1);
						doExportAccount( (PayAccount) m_listAccounts.getSelectedValue());
						doChargeAccount( (PayAccount) m_listAccounts.getSelectedValue());
					}
				}
				else
				{
					showPIerror(GUIUtils.getParentWindow(this.getRootPanel()));
				}
			}
		}

	/**
	 * Shows a window with all known Payment Instances and lets the user select one.
	 * @return BI
	 */
	private BI getBIforAccountCreation()
	{
		BISelectionDialog d = new BISelectionDialog(getRootPanel());
		BI theBI = d.getSelectedBI();

		if (theBI != null)
		{
			/*LogHolder.log(LogLevel.DEBUG, LogType.PAY, "Selected Payment Instance is: " +
			  theBI.getHostName() + ":" + theBI.getPortNumber());*/
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

		if (JAPController.getInstance().getAnonMode())
		{
			JAPDialog.showMessageDialog(GUIUtils.getParentWindow(this.getRootPanel()),
										JAPMessages.getString(MSG_CONNECTIONACTIVE));
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
		updateAccountList();
	}

	/**
	 * doGetStatement - fetches an account statement
	 */
	private void doGetStatement(final PayAccount a_selectedAccount)
	{
			if (a_selectedAccount == null)
			{
				return;
			}
			JAPDialog busy = new JAPDialog(GUIUtils.getParentWindow(this.getRootPanel()),
										   JAPMessages.getString(MSG_GETACCOUNTSTATEMENTTITLE), true);
			Thread t = new Thread()
			{
				public void run()
				{
					try
					{
						a_selectedAccount.fetchAccountInfo(JAPModel.getInstance().getProxyInterface());
						updateAccountList();
					}
					catch (Exception e)
					{
						showPIerror(GUIUtils.getParentWindow(getRootPanel()));
						LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, "Could not get account statement");
					}
				}
			};
			WorkerContentPane worker = new WorkerContentPane(busy,
				JAPMessages.getString(MSG_GETACCOUNTSTATEMENT),
				t);
			worker.setInterruptThreadSafe(false);
			worker.updateDialog();
			busy.pack();
			busy.setLocationCenteredOnOwner();
			busy.setVisible(true);
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

		boolean yes = JAPDialog.showYesNoDialog(GUIUtils.getParentWindow(this.getRootPanel()),
												JAPMessages.getString(MSG_EXPORTENCRYPT));

		if (yes)
		{
			JAPDialog d = new JAPDialog(GUIUtils.getParentWindow(this.getRootPanel()),
										JAPMessages.getString(MSG_ACCPASSWORDTITLE), true);

			PasswordContentPane p = new PasswordContentPane(d, PasswordContentPane.PASSWORD_NEW, "");
			p.updateDialog();
			d.pack();
			d.setVisible(true);
			if (p.getButtonValue() != PasswordContentPane.RETURN_VALUE_CANCEL &&
				p.getButtonValue() != PasswordContentPane.RETURN_VALUE_CLOSED)
			{

				strPassword = new String(p.getPassword());
				encrypt = true;
			}
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
				JAPDialog.showMessageDialog(GUIUtils.getParentWindow(this.getRootPanel()),
											JAPMessages.getString(MSG_EXPORTED));
			}
			catch (Exception e)
			{
				JAPDialog.showErrorDialog(GUIUtils.getParentWindow(this.getRootPanel()),
										  JAPMessages.getString(MSG_NOTEXPORTED) + ": " + e, LogType.PAY);
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
						String strPassword = null;

						while (true)
						{
							JAPDialog d = new JAPDialog(GUIUtils.getParentWindow(this.getRootPanel()),
								JAPMessages.getString(MSG_ACCPASSWORDTITLE), true);
							PasswordContentPane p = new PasswordContentPane(d,
								PasswordContentPane.PASSWORD_ENTER, "");
							p.updateDialog();
							d.pack();
							d.setVisible(true);
							if (p.getButtonValue() != PasswordContentPane.RETURN_VALUE_CANCEL &&
								p.getButtonValue() != PasswordContentPane.RETURN_VALUE_CLOSED)
							{
								strPassword = new String(p.getPassword());
							}
							if (strPassword == null)
							{
								break;
							}
							try
							{
								elemAccount = XMLEncryption.decryptElement(elemCrypt, strPassword);
							}
							catch (Exception ex)
							{
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
					updateAccountList();
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
			boolean yes = JAPDialog.showYesNoDialog(GUIUtils.getParentWindow(this.getRootPanel()),
				JAPMessages.getString("ngDeleteAccountStatement"));

			if (yes)
			{
				doGetStatement(selectedAccount);
			}
		}
		if (selectedAccount.hasAccountInfo())
		{
			XMLAccountInfo accInfo = selectedAccount.getAccountInfo();
			if (accInfo.getBalance().getTimestamp().getTime() <
				(System.currentTimeMillis() - 1000 * 60 * 60 * 24))
			{
				boolean yes = JAPDialog.showYesNoDialog(GUIUtils.getParentWindow(this.getRootPanel()),
					JAPMessages.getString(MSG_OLDSTATEMENT));

				if (yes)
				{
					doGetStatement(selectedAccount);
				}
			}

			if (accInfo.getBalance().getCredit() > 0)
			{
				boolean yes = JAPDialog.showYesNoDialog(GUIUtils.getParentWindow(this.getRootPanel()),
					JAPMessages.getString("ngDeleteAccountCreditLeft"));

				if (yes)
				{
					reallyDelete = true;
				}
			}
			else
			{
				boolean yes  = JAPDialog.showYesNoDialog(GUIUtils.getParentWindow(this.getRootPanel()),
					JAPMessages.getString("ngReallyDeleteAccount"));

				if (yes)
				{
					reallyDelete = true;
				}
			}
		}
		else
		{
			boolean yes = JAPDialog.showYesNoDialog(GUIUtils.getParentWindow(this.getRootPanel()),
					JAPMessages.getString("ngReallyDeleteAccount"));

			if (yes)
			{
				reallyDelete = true;
			}
		}
		if (reallyDelete)
		{
			try
			{
				accounts.deleteAccount(selectedAccount.getAccountNumber());
				m_listAccounts.clearSelection();
				updateAccountList();
				doShowDetails(null);
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
		//Register help context
		JAPHelp.getInstance().getContextObj().setContext("payment");
		updateAccountList();
	 	m_cbxShowPaymentConfirmation.setSelected(!JAPController.getInstance().getDontAskPayment());
	}

	/**
	 * This method can be overwritten by the children of AbstractJAPConfModule. It is called
	 * every time the user presses "OK" in the configuration dialog.
	 */
	protected boolean onOkPressed()
	{
		JAPController.getInstance().setDontAskPayment(!m_cbxShowPaymentConfirmation.isSelected());
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

	public void valueChanged(ListSelectionEvent e)
	{
		if (e.getSource() == m_listAccounts)
		{
			if (m_listAccounts.getModel().getSize() > 0)
			{
				doShowDetails(getSelectedAccount());
				enableDisableButtons();
			}
		}
	}

	private void showPIerror(JAPDialog a_parent)
	{
		JAPDialog.showErrorDialog(a_parent,
								  JAPMessages.getString(MSG_CREATEERROR),
								  LogType.PAY);
	}

	public void showPIerror(Component a_parent)
	{
		JAPDialog.showErrorDialog(a_parent,
								  JAPMessages.getString(MSG_CREATEERROR),
								  LogType.PAY);
	}
}

class CustomRenderer extends DefaultListCellRenderer
{
	public Component getListCellRendererComponent(JList list, Object value,
												  int index, boolean isSelected, boolean cellHasFocus)
	{
		JLabel l;
		Component comp = super.getListCellRendererComponent(list, value,
			index, isSelected, cellHasFocus);
		if (comp instanceof JComponent && value != null && value instanceof PayAccount)
		{
			l = new JLabel(String.valueOf( ( (PayAccount) value).getAccountNumber()),
						   GUIUtils.loadImageIcon(JAPConstants.IMAGE_COINS_FULL, true), LEFT);

			if (isSelected)
			{
				l.setOpaque(true);
				l.setBackground(Color.lightGray);
			}
			Font f = l.getFont();
			if ( ( (PayAccount) value).equals(PayAccountsFile.getInstance().getActiveAccount()))
			{
				l.setFont(new Font(f.getName(), Font.BOLD, f.getSize()));
			}
			else
			{
				l.setFont(new Font(f.getName(), Font.PLAIN, f.getSize()));
			}
		}
		else
		{
			if (value != null)
			{
				l = new JLabel(value.toString());

			}
			else
			{
				l = new JLabel();
			}

		}

		return l;
	}
}
