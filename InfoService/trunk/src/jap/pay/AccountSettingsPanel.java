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

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.security.SecureRandom;
import java.util.Date;
import java.util.Enumeration;

import java.awt.Color;
import java.awt.Component;
import java.awt.Cursor;
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
import javax.swing.JCheckBox;
import javax.swing.JComponent;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.JSeparator;
import javax.swing.JTabbedPane;
import javax.swing.ListSelectionModel;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.filechooser.FileFilter;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import HTTPClient.ForbiddenIOException;
import anon.crypto.DSAKeyPair;
import anon.crypto.JAPCertificate;
import anon.crypto.XMLEncryption;
import anon.infoservice.ListenerInterface;
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
import anon.util.SingleStringPasswordReader;
import anon.util.XMLUtil;
import anon.util.captcha.ICaptchaSender;
import anon.util.captcha.IImageEncodedCaptcha;
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
import jap.JAPModel;
import jap.JAPUtil;
import jap.pay.wizardnew.MethodSelectionPane;
import jap.pay.wizardnew.PassivePaymentPane;
import jap.pay.wizardnew.PaymentInfoPane;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import java.util.Observer;
import java.util.Observable;

/**
 * The Jap Conf Module (Settings Tab Page) for the Accounts and payment Management
 *
 * @author Bastian Voigt, Tobias Bayer
 * @version 1.0
 */
public class AccountSettingsPanel extends AbstractJAPConfModule implements
	ListSelectionListener,  ChangeListener
{
	protected static final String IMG_COINS_DISABLED = AccountSettingsPanel.class.getName() +
		"_coins-disabled.gif";

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
	private static final String MSG_CREATEERROR = AccountSettingsPanel.class.getName() +
		"_createerror";
	private static final String MSG_DIRECT_CONNECTION_FORBIDDEN = AccountSettingsPanel.class.getName() +
		"_directConnectionForbidden";
	private static final String MSG_NO_ANONYMITY_POSSIBLY_BLOCKED = AccountSettingsPanel.class.getName() +
		"_noAnonymityPossiblyBlocked";
	private static final String MSG_ERROR_FORBIDDEN = AccountSettingsPanel.class.getName() +
		"_errorForbidden";
	private static final String MSG_GETACCOUNTSTATEMENT = AccountSettingsPanel.class.
		getName() + "_getaccountstatement";
	private static final String MSG_GETACCOUNTSTATEMENTTITLE = AccountSettingsPanel.class.
		getName() + "_getaccountstatementtitle";
	private static final String MSG_ACCOUNTCREATEDESC = AccountSettingsPanel.class.
		getName() + "_accountcreatedesc";
	private static final String MSG_ACCPASSWORDTITLE = AccountSettingsPanel.class.
		getName() + "_accpasswordtitle";
	private static final String MSG_EXPORTENCRYPT = AccountSettingsPanel.class.
		getName() + "_exportencrypt";
	private static final String MSG_ACCPASSWORD = AccountSettingsPanel.class.
		getName() + "_accpassword";
	private static final String MSG_OLDSTATEMENT = AccountSettingsPanel.class.
		getName() + "_oldstatement";
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
	private static final String MSG_TEST_PI_CONNECTION = AccountSettingsPanel.class.
		getName() + "_testingPIConnection";
	private static final String MSG_CREATE_KEY_PAIR = AccountSettingsPanel.class.getName() +
		"_creatingKeyPair";
	private static final String MSG_KEY_PAIR_CREATE_ERROR = AccountSettingsPanel.class.getName() +
		"_keyPairCreateError";
	private static final String MSG_FETCHING_BIS = AccountSettingsPanel.class.getName() +
		"_fetchingBIs";
	private static final String MSG_SAVE_CONFIG = AccountSettingsPanel.class.getName() +
		"_savingConfig";
	private static final String MSG_CREATED_ACCOUNT_NOT_SAVED = AccountSettingsPanel.class.getName() +
		"_createdAccountNotSaved";
	private static final String MSG_ACCOUNT_IMPORT_FAILED = AccountSettingsPanel.class.getName() +
		"_accountImportFailed";
	private static final String MSG_ACCOUNT_ALREADY_EXISTING = AccountSettingsPanel.class.getName() +
		"_accountAlreadyExisting";
	private static final String MSG_ALLOW_DIRECT_CONNECTION = AccountSettingsPanel.class.getName() +
		"_allowDirectConnection";
	private static final String MSG_BI_CONNECTION_LOST = AccountSettingsPanel.class.getName() +
		"_biConnectionLost";
	private static final String MSG_BUTTON_ACTIVATE = AccountSettingsPanel.class.getName() +
		"_activateAccount";
	private static final String MSG_BUTTON_DEACTIVATE = AccountSettingsPanel.class.getName() +
		"_deactivateAccount";
	private static final String MSG_ERROR_DELETING = AccountSettingsPanel.class.getName() +
		"_errorDeletingAccount";
	private static final String MSG_ACCOUNT_DISABLED = AccountSettingsPanel.class.getName() +
		"_accountDisabled";
	private static final String MSG_GIVE_ACCOUNT_PASSWORD = AccountSettingsPanel.class.getName() +
		"_giveAccountPassword";
	private static final String MSG_ACTIVATION_SUCCESSFUL = AccountSettingsPanel.class.getName() +
		"_activationSuccessful";
	private static final String MSG_ACTIVATION_FAILED = AccountSettingsPanel.class.getName() +
		"_activationFailed";
	private static final String MSG_SHOW_AI_ERRORS = AccountSettingsPanel.class.getName() +
		"_showAIErrors";
	private static final String MSG_BALANCE_AUTO_UPDATE_ENABLED = AccountSettingsPanel.class.getName() +
		"_balanceAutoUpdateEnabled";
	private static final String MSG_NO_BACKUP = AccountSettingsPanel.class.getName() + "_noBackup";
	private static final String MSG_TOOL_TIP_NO_BACKUP =
		AccountSettingsPanel.class.getName() + "_toolTipNoBackup";
	private static final String MSG_TOOL_TIP_ACTIVATE =
		AccountSettingsPanel.class.getName() + "_toolTipActivate";
	private static final String MSG_TOOL_TIP_EXPIRED =
		AccountSettingsPanel.class.getName() + "_toolTipExpired";

	private static final String MSG_FILE_EXISTS = AccountSettingsPanel.class.getName() + "_fileExists";

	private JButton m_btnCreateAccount;
	private JButton m_btnChargeAccount;
	private JButton m_btnDeleteAccount;
	private JButton m_btnExportAccount;
	private JButton m_btnImportAccount;
	private JButton m_btnTransactions;
	private JButton m_btnSelect;
	private JButton m_btnPassword;
	private JButton m_btnReload;
	private JButton m_btnActivate;
	private JCheckBox m_cbxShowPaymentConfirmation;
	private JCheckBox m_cbxAllowNonAnonymousConnection;
	private JCheckBox m_cbxShowAIErrors;
	private JCheckBox m_cbxBalanceAutoUpdateEnabled;

	private JLabel m_labelCreationDate;
	private JLabel m_labelStatementDate;
	private JLabel m_labelDeposit;
	private JLabel m_labelSpent;
	private JLabel m_labelBalance;
	private JLabel m_labelValid;
	private JLabel m_lblInactiveMessage, m_lblNoBackupMessage;
	private JProgressBar m_coinstack;
	private JList m_listAccounts;
	private boolean m_bReady = true;
	private boolean m_bDoNotCloseDialog = false;

	/**The TabbedPane Component*/
	private JTabbedPane m_tabPane;

	public AccountSettingsPanel()
	{
		super(null);
	}

	public void fontSizeChanged(JAPModel.FontResize a_fontSize, JLabel a_dummyLabel)
	{
		m_coinstack.setUI(new CoinstackProgressBarUI(GUIUtils.loadImageIcon(JAPConstants.
			IMAGE_COIN_COINSTACK, true), 0, 8));
	}

	/**
	 * This method will be called when another tab is chosen*/
	public void stateChanged(ChangeEvent ce)
	{
		setHelpContext();
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
		m_tabPane = new JTabbedPane();
		//tabPane.setFont(getFontSetting());
		m_tabPane.insertTab(JAPMessages.getString("ngPseudonymAccounts"),
						  null, createBasicSettingsTab(), null, 0);
		m_tabPane.insertTab(JAPMessages.getString(
			"settingsInfoServiceConfigAdvancedSettingsTabTitle"), null, createAdvancedSettingsTab(), null, 1);
		m_tabPane.addChangeListener(this);
		GridBagLayout rootPanelLayout = new GridBagLayout();
		rootPanel.setLayout(rootPanelLayout);
		rootPanelLayout.setConstraints(m_tabPane, createTabbedRootPanelContraints());
		rootPanel.add(m_tabPane);
	}

	private JPanel createBasicSettingsTab()
	{
		JPanel rootPanel = new JPanel();

		rootPanel.setLayout(new GridBagLayout());

		m_listAccounts = new JList();
		m_listAccounts.setCellRenderer(new CustomRenderer());
		m_listAccounts.addListSelectionListener(this);
		m_listAccounts.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		//m_listAccounts.setPreferredSize(new Dimension(200, 200));
		m_listAccounts.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent e)
			{
				//Activate account on double click
				if (e.getClickCount() == 2)
				{
					doSelectAccount(getSelectedAccount());
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
		m_btnSelect = new JButton(JAPMessages.getString(MSG_BUTTON_SELECT));
		m_btnSelect.addActionListener(myActionListener);
		buttonsPanel.add(m_btnSelect, c);

		c.gridx++;
		c.weightx = 1;
		c.weighty = 1;
		m_btnActivate = new JButton(JAPMessages.getString(MSG_BUTTON_ACTIVATE));
		m_btnActivate.addActionListener(myActionListener);
		buttonsPanel.add(m_btnActivate, c);

		c = new GridBagConstraints();
		c.fill = c.BOTH;
		c.anchor = c.NORTHWEST;
		c.weightx = 2.0;
		c.weighty = 1.0;
		c.gridx = 0;
		c.gridy = 0;
		c.gridheight = 5;
		c.insets = new Insets(5, 5, 5, 5);
		JScrollPane scroller = new JScrollPane(m_listAccounts);
		scroller.setHorizontalScrollBarPolicy(JScrollPane.HORIZONTAL_SCROLLBAR_NEVER);
		//scroller.setVerticalScrollBarPolicy(JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED);
		rootPanel.add(scroller, c);

		c.gridx++;
		c.fill = c.NONE;
		c.gridheight = 1;
		c.weighty = 0.0;
		c.weightx = 0.0;
		rootPanel.add(new JLabel(JAPMessages.getString(MSG_ACCOUNT_CREATION_DATE)), c);
		c.gridx++;
		c.weightx = 1.0;
		m_labelCreationDate = new JLabel();
		rootPanel.add(m_labelCreationDate, c);

		c.gridx--;
		c.gridy++;
		c.weightx = 0.0;
		rootPanel.add(new JLabel(JAPMessages.getString(MSG_ACCOUNT_STATEMENT_DATE)), c);
		c.gridx++;
		m_labelStatementDate = new JLabel();
		c.weightx = 1.0;
		rootPanel.add(m_labelStatementDate, c);

		c.gridx--;
		c.gridy++;
		c.weightx = 0.0;
		rootPanel.add(new JLabel(JAPMessages.getString(MSG_ACCOUNT_VALID)), c);
		c.gridx++;
		c.weightx = 1.0;
		m_labelValid = new JLabel();
		rootPanel.add(m_labelValid, c);

		c.gridy++;
		c.gridx--;
		c.gridwidth = 2;
		c.weightx = 1.0;
		m_lblInactiveMessage = new JLabel();
		m_lblInactiveMessage.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		m_lblInactiveMessage.setForeground(Color.red);
		m_lblInactiveMessage.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent a_event)
			{
				m_btnActivate.doClick();
			}
		});

		rootPanel.add(m_lblInactiveMessage, c);

		c.gridy++;
		m_lblNoBackupMessage = new JLabel();
		m_lblNoBackupMessage.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent a_event)
			{
				m_btnExportAccount.doClick();
			}
		});
		m_lblNoBackupMessage.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		m_lblNoBackupMessage.setForeground(Color.red);
		rootPanel.add(m_lblNoBackupMessage, c);


		c.gridy++;
		c.weightx = 0;
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
		rootPanel.add(this.createDetailsPanel(myActionListener), c);

		//updateAccountList(); //would possibly lead to deadlock with AWT-Thread when showing JAPConf
		enableDisableButtons();

		return rootPanel;
	}

	private JPanel createAdvancedSettingsTab()
	{
		JPanel panelAdvanced = new JPanel();

		m_cbxShowPaymentConfirmation = new JCheckBox(JAPMessages.getString(MSG_SHOW_PAYMENT_CONFIRM_DIALOG));
		//m_cbxShowPaymentConfirmation.setFont(getFontSetting());
		GridBagLayout advancedPanelLayout = new GridBagLayout();
		panelAdvanced.setLayout(advancedPanelLayout);

		GridBagConstraints advancedPanelConstraints = new GridBagConstraints();
		advancedPanelConstraints.anchor = GridBagConstraints.NORTHWEST;
		advancedPanelConstraints.fill = GridBagConstraints.NONE;
		advancedPanelConstraints.weightx = 1.0;

		advancedPanelConstraints.gridx = 0;
		advancedPanelConstraints.gridy = 0;
		advancedPanelConstraints.insets = new Insets(5, 5, 10, 5);

		panelAdvanced.add(m_cbxShowPaymentConfirmation, advancedPanelConstraints);

		m_cbxAllowNonAnonymousConnection = new JCheckBox(JAPMessages.getString(MSG_ALLOW_DIRECT_CONNECTION));

		advancedPanelConstraints.gridy = 1;
		panelAdvanced.add(m_cbxAllowNonAnonymousConnection, advancedPanelConstraints);

		advancedPanelConstraints.gridy = 2;
		m_cbxShowAIErrors = new JCheckBox(JAPMessages.getString(MSG_SHOW_AI_ERRORS));

		if (JAPConstants.m_bReleasedVersion)
		{
			// this does not work in release version, as it is only meant for debugging purposes
			m_cbxShowAIErrors.setVisible(false);
		}
		panelAdvanced.add(m_cbxShowAIErrors, advancedPanelConstraints);

		advancedPanelConstraints.gridy = 3;
		advancedPanelConstraints.weighty = 1.0;
		m_cbxBalanceAutoUpdateEnabled = new JCheckBox(JAPMessages.getString(MSG_BALANCE_AUTO_UPDATE_ENABLED));

		panelAdvanced.add(m_cbxBalanceAutoUpdateEnabled, advancedPanelConstraints);

		onUpdateValues();

		return panelAdvanced;
	}

	/**
	 * Creates a new lower view of the dialog for displaying account details.
	 * @return JPanel
	 */
	private JPanel createDetailsPanel(ActionListener a_actionListener)
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
		m_btnChargeAccount.addActionListener(a_actionListener);
		buttonsPanel.add(m_btnChargeAccount, d);

		d.gridx++;
		m_btnReload = new JButton(JAPMessages.getString(MSG_BUTTONRELOAD));
		m_btnReload.addActionListener(a_actionListener);
		buttonsPanel.add(m_btnReload, d);

		d.gridx++;
		m_btnTransactions = new JButton(JAPMessages.getString(MSG_BUTTON_TRANSACTIONS));
		m_btnTransactions.addActionListener(a_actionListener);
		buttonsPanel.add(m_btnTransactions, d);

		d.gridx++;
		m_btnExportAccount = new JButton(JAPMessages.getString(MSG_BUTTON_EXPORT));
		m_btnExportAccount.addActionListener(a_actionListener);
		buttonsPanel.add(m_btnExportAccount, d);

		d.gridx++;
		d.weightx = 1;
		d.weighty = 1;
		m_btnDeleteAccount = new JButton(JAPMessages.getString(MSG_BUTTON_DELETE));
		m_btnDeleteAccount.addActionListener(a_actionListener);
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
		Runnable updateAccountThread = new Runnable()
		{
			public void run()
			{
				PayAccount account;
				DefaultListModel listModel = new DefaultListModel();
				Enumeration accounts = PayAccountsFile.getInstance().getAccounts();
				int selectedItem = m_listAccounts.getSelectedIndex();

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
		};
		if (SwingUtilities.isEventDispatchThread())
		{
			updateAccountThread.run();
		}
		else
		{
			try
			{
				SwingUtilities.invokeAndWait(updateAccountThread);
			}
			catch (Exception a_e)
			{
				LogHolder.log(LogLevel.EXCEPTION, LogType.GUI, a_e);
			}
		}
	}

	private void enableDisableButtons()
	{
		//if (m_listAccounts.getModel().getSize() > 0)
		{
			boolean enable = (getSelectedAccount() != null && getSelectedAccount().getPrivateKey() != null);
			m_btnActivate.setEnabled(
						 getSelectedAccount() != null && getSelectedAccount().getPrivateKey() == null);
			m_btnChargeAccount.setEnabled(enable);
			m_btnTransactions.setEnabled(enable);
			m_btnExportAccount.setEnabled(enable);
			m_btnReload.setEnabled(enable);
			m_btnSelect.setEnabled(enable);
			m_btnDeleteAccount.setEnabled(getSelectedAccount() != null);
		}/*
		else
		{
			m_btnActivate.setEnabled(false);
			m_btnChargeAccount.setEnabled(false);
			m_btnTransactions.setEnabled(false);
			m_btnExportAccount.setEnabled(false);
			m_btnReload.setEnabled(false);
			m_btnSelect.setEnabled(false);
			m_btnDeleteAccount.setEnabled(false);
		}*/
	}

/**
	 * Handler for the Button Clicks
	 * @author Bastian Voigt
	 * @version 1.0
	 */
	private class MyActionListener implements ActionListener
	{
		private boolean m_bButtonClicked = false;

		public void actionPerformed(final ActionEvent e)
		{
			Thread clickThread = new Thread()
			{
				public void run()
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
						doSelectAccount(getSelectedAccount());
					}
					else if (source == m_btnPassword)
					{
						doChangePassword();
					}
					else if (source == m_btnReload)
					{
						doGetStatement( (PayAccount) m_listAccounts.getSelectedValue());
					}
					else if (source == m_btnActivate)
					{
						doActivateAccount((PayAccount) m_listAccounts.getSelectedValue());
					}
					m_bButtonClicked = false;
				}
			};
			if (!m_bButtonClicked)
			{
				m_bButtonClicked = true;
				clickThread.start();
			}
		}
	}

/**
	 * Asks the user for a new payment password
	 */
	private void doChangePassword()
	{
		JAPDialog d = new JAPDialog(GUIUtils.getParentWindow(this.getRootPanel()),
									JAPMessages.getString(MSG_ACCPASSWORDTITLE), true);
		PasswordContentPane p;

		if (JAPController.getInstance().getPaymentPassword() != null)
		{
			p = new PasswordContentPane(d, PasswordContentPane.PASSWORD_CHANGE, "")
			{
				public char[] getComparedPassword()
				{
					return JAPController.getInstance().getPaymentPassword().toCharArray();
				}
			};
		}
		else
		{
			p = new PasswordContentPane(d, PasswordContentPane.PASSWORD_NEW, "");
		}
		p.updateDialog();
		d.pack();
		d.setVisible(true);
		if (p.getButtonValue() != PasswordContentPane.RETURN_VALUE_CANCEL &&
			p.getButtonValue() != PasswordContentPane.RETURN_VALUE_CLOSED)
		{
			JAPController.getInstance().setPaymentPassword(new String(p.getPassword()));
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
			m_lblInactiveMessage.setText("");
			m_lblNoBackupMessage.setText("");
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
		if (selectedAccount.getPrivateKey() == null)
		{
			m_lblInactiveMessage.setText(JAPMessages.getString(MSG_ACCOUNT_DISABLED));
			m_lblInactiveMessage.setToolTipText(JAPMessages.getString(MSG_TOOL_TIP_ACTIVATE));
		}
		else
		{
			m_lblInactiveMessage.setText("");
			m_lblInactiveMessage.setToolTipText("");
		}
		if (!selectedAccount.isBackupDone())
		{
			m_lblNoBackupMessage.setText(JAPMessages.getString(MSG_NO_BACKUP));
			m_lblNoBackupMessage.setToolTipText(JAPMessages.getString(MSG_TOOL_TIP_NO_BACKUP));
		}
		else
		{
			m_lblNoBackupMessage.setText("");
			m_lblNoBackupMessage.setToolTipText("");
		}


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
			if (balance.getValidTime().before(new Date()))
			{
				m_labelValid.setForeground(Color.red);
				m_labelValid.setToolTipText(JAPMessages.getString(MSG_TOOL_TIP_EXPIRED));
			}
			else
			{
				m_labelValid.setForeground(new JLabel().getForeground());
				m_labelValid.setToolTipText("");
			}

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
		try
		{
			return (PayAccount) m_listAccounts.getSelectedValue();
		}
		catch (Exception a_e)
		{
			return null;
		}
	}

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
		d.setDefaultCloseOperation(JAPDialog.DISPOSE_ON_CLOSE);
		d.setResizable(true);
		doChargeAccount(new FixedReturnAccountRunnable(selectedAccount), d, null, null);
		d.setLocationCenteredOnOwner();
		d.setVisible(true);
	}

	/**
	 * Charges the selected account
	 */
	private void doChargeAccount(final IReturnAccountRunnable a_accountCreationThread,
								 final JAPDialog a_parentDialog,
								 final DialogContentPane a_previousContentPane,
								 final IReturnBooleanRunnable a_booleanThread)
	{
		WorkerContentPane.IReturnRunnable fetchOptions = new WorkerContentPane.IReturnRunnable()
		{
			private XMLPaymentOptions m_paymentOptions;
			public void run()
			{
				try
				{
					BI pi = a_accountCreationThread.getAccount().getBI();
					BIConnection piConn = new BIConnection(pi);

					piConn.connect(JAPModel.getInstance().getPaymentProxyInterface());
					piConn.authenticate(PayAccountsFile.getInstance().getActiveAccount().
										getAccountCertificate(),
										PayAccountsFile.getInstance().getActiveAccount().getPrivateKey());
					LogHolder.log(LogLevel.DEBUG, LogType.PAY, "Fetching payment options");
					m_paymentOptions = piConn.getPaymentOptions();
					piConn.disconnect();
				}
				catch (Exception e)
				{
					if (!Thread.currentThread().isInterrupted())
					{
						LogHolder.log(LogLevel.EXCEPTION, LogType.NET,
									  "Error fetching payment options: " + e.getMessage());
						showPIerror(a_parentDialog.getContentPane(), e);
						Thread.currentThread().interrupt();
					}
				}
			}

			public Object getValue()
			{
				return m_paymentOptions;
			}
		};

		final WorkerContentPane fetchOptionsPane = new WorkerContentPane(a_parentDialog,
			JAPMessages.getString(MSG_FETCHINGOPTIONS), a_previousContentPane, fetchOptions)
		{
			public boolean isMoveForwardAllowed()
			{
				return a_parentDialog.isVisible() && a_booleanThread != null && !a_booleanThread.isTrue();
			}
		};
		fetchOptionsPane.setInterruptThreadSafe(false);


		final MethodSelectionPane methodSelectionPane =
			new MethodSelectionPane(a_parentDialog, fetchOptionsPane);

		WorkerContentPane.IReturnRunnable fetchTan = new WorkerContentPane.IReturnRunnable()
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

						m_transCert = a_accountCreationThread.getAccount().charge(
							JAPModel.getInstance().getPaymentProxyInterface());
					}
					catch (Exception e)
					{
						if (!Thread.currentThread().isInterrupted())
						{
							LogHolder.log(LogLevel.EXCEPTION, LogType.NET,
										  "Error fetching TransCert: " + e.getMessage());
							showPIerror(GUIUtils.getParentWindow(getRootPanel()), e);
							Thread.currentThread().interrupt();
						}
					}
				}
			}

			public Object getValue()
			{
				return m_transCert;
			}
		};

		WorkerContentPane fetchTanPane =
			new WorkerContentPane(a_parentDialog, JAPMessages.getString(MSG_FETCHINGTAN),
			methodSelectionPane, fetchTan);
		fetchTanPane.setInterruptThreadSafe(false);

		final PaymentInfoPane paymentInfoPane = new PaymentInfoPane(a_parentDialog, fetchTanPane)
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
		paymentInfoPane.getButtonNo().setVisible(false);

		final PassivePaymentPane passivePaymentPane = new PassivePaymentPane(a_parentDialog, paymentInfoPane)
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

		WorkerContentPane.IReturnRunnable sendPassive = new WorkerContentPane.IReturnRunnable()
		{
			private Boolean m_successful = new Boolean(true);

			public void run()
			{
				/** Post data to payment instance */
				BIConnection biConn = new BIConnection(a_accountCreationThread.getAccount().getBI());
				try
				{
					biConn.connect(JAPModel.getInstance().getPaymentProxyInterface());
					biConn.authenticate(a_accountCreationThread.getAccount().getAccountCertificate(),
										a_accountCreationThread.getAccount().getPrivateKey());
					if (!biConn.sendPassivePayment(passivePaymentPane.getEnteredInfo()))
					{
						m_successful = new Boolean(false);
					}
					biConn.disconnect();
				}
				catch (Exception e)
				{
					m_successful = new Boolean(false);
					if (!Thread.currentThread().isInterrupted())
					{
						LogHolder.log(LogLevel.EXCEPTION, LogType.PAY,
									  "Could not send PassivePayment to payment instance: " +
									  e.getMessage());
						showPIerror(GUIUtils.getParentWindow(getRootPanel()), e);
						Thread.currentThread().interrupt();
					}
				}
			}

			public Object getValue()
			{
				return m_successful;
			}
		};

		final WorkerContentPane sendPassivePane = new WorkerContentPane(a_parentDialog,
			JAPMessages.getString(MSG_SENDINGPASSIVE), passivePaymentPane, sendPassive)
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

		final SimpleWizardContentPane sentPane = new SimpleWizardContentPane(a_parentDialog,
			JAPMessages.getString(MSG_SENTPASSIVE), null,
			new Options(createUpdateAccountPane(
					 a_accountCreationThread, methodSelectionPane, a_parentDialog, sendPassivePane)))
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
			public CheckError[] checkUpdate()
			{
				if ( ( (Boolean) sendPassivePane.getValue()).booleanValue() == false)
				{
					setText("<Font color='red'><b>" +
							JAPMessages.getString(MSG_NOTSENTPASSIVE) + "</b></Font>");
				}
				else
				{
					setText(JAPMessages.getString(MSG_SENTPASSIVE));
				}

				return null;
			}
		};

		sentPane.getButtonCancel().setVisible(false);
		sentPane.getButtonNo().setVisible(false);

		if (a_previousContentPane == null)
		{
			DialogContentPane.updateDialogOptimalSized(fetchOptionsPane);
		}
	}

	/**
	 *
	 * @return boolean
	 */
	private void doCreateAccount()
	{
		final JAPDialog d = new JAPDialog(getRootPanel(), JAPMessages.getString(MSG_ACCOUNTCREATE), true);
		d.setDefaultCloseOperation(JAPDialog.DO_NOTHING_ON_CLOSE);
		d.setResizable(true);

		WorkerContentPane.IReturnRunnable fetchBIThread = new WorkerContentPane.IReturnRunnable()
		{
			private BI theBI;

			public void run()
			{
				Exception biException = null;

				//First try and get the standard PI the preferred way
				try
				{
					theBI = PayAccountsFile.getInstance().getBI(JAPConstants.PI_ID);
				}
				catch (Exception e)
				{
					biException = e;
					LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, e);
				}

				//Try and construct a new PI
				if (theBI == null)
				{
					ListenerInterface li = new ListenerInterface(JAPConstants.PI_HOST,
						JAPConstants.PI_PORT);
					try
					{
						theBI = new BI(JAPConstants.PI_ID, JAPConstants.PI_NAME, li.toVector(),
									   JAPCertificate.getInstance(ResourceLoader.loadResource(
										   JAPConstants.
										   CERTSPATH +
										   JAPConstants.PI_CERT)));
					}
					catch (Exception e)
					{
						if (biException == null || e instanceof ForbiddenIOException)
						{
							biException = e;
						}
						LogHolder.log(LogLevel.EXCEPTION, LogType.PAY,
									  "Could not create Test-PI: " + e.getMessage());
						theBI = getBIforAccountCreation();
					}
				}

				if (Thread.currentThread().isInterrupted())
				{
					theBI = null;
				}

				if (theBI == null)
				{
					// no valid BI could be found
					if (!Thread.currentThread().isInterrupted())
					{
						showPIerror(d.getContentPane(), biException);
						Thread.currentThread().interrupt();
					}
				}
			}

			public Object getValue()
			{
				return theBI;
			}

		};
		final WorkerContentPane fetchBiWorker =
			new WorkerContentPane(d, JAPMessages.getString(MSG_FETCHING_BIS) + "...", fetchBIThread);

		Runnable piTestThread = new Runnable()
		{
			public void run()
			{
				if (fetchBiWorker.getValue() == null)
				{
					Thread.currentThread().interrupt();
					return;
				}
				try
				{
					//Check if payment instance is reachable
					BIConnection biconn = new BIConnection( (BI) fetchBiWorker.getValue());
					biconn.connect(JAPModel.getInstance().getPaymentProxyInterface());
					biconn.disconnect();
				}
				catch (Exception e)
				{
					if (!Thread.currentThread().isInterrupted())
					{
						showPIerror(d.getContentPane(), e);
						Thread.currentThread().interrupt();
					}
				}
			}
		};

		WorkerContentPane PITestWorkerPane = new WorkerContentPane(d,
			JAPMessages.getString(MSG_TEST_PI_CONNECTION) + "...", fetchBiWorker, piTestThread);
		PITestWorkerPane.setInterruptThreadSafe(false);

		SimpleWizardContentPane panel1 = new SimpleWizardContentPane(d,
			JAPMessages.getString("ngCreateKeyPair"), null,
			new SimpleWizardContentPane.Options(PITestWorkerPane));

		final WorkerContentPane.IReturnRunnable keyCreationThread = new WorkerContentPane.IReturnRunnable()
		{
			private DSAKeyPair m_keyPair;

			public void run()
			{
				m_bDoNotCloseDialog = true;
				m_keyPair =
					DSAKeyPair.getInstance(new SecureRandom(), DSAKeyPair.KEY_LENGTH_1024, 20);
				if (m_keyPair == null)
				{
					JAPDialog.showErrorDialog(
						d, JAPMessages.getString(MSG_KEY_PAIR_CREATE_ERROR), LogType.PAY);
				}
				m_bDoNotCloseDialog = false;
			}

			public Object getValue()
			{
				return m_keyPair;
			}
		};
		final WorkerContentPane keyWorkerPane = new WorkerContentPane(
			d, JAPMessages.getString(MSG_CREATE_KEY_PAIR) + "...", panel1, keyCreationThread);
		keyWorkerPane.getButtonCancel().setEnabled(false);

		m_bReady = true;

		final IReturnAccountRunnable doIt = new IReturnAccountRunnable()
		{
			private PayAccount m_payAccount;
			private IOException m_connectionError;

			public void run()
			{
				m_bReady = false;

				while (!Thread.currentThread().isInterrupted())
				{
					try
					{
						m_payAccount = PayAccountsFile.getInstance().createAccount(
							(BI) fetchBiWorker.getValue(),
							JAPModel.getInstance().getPaymentProxyInterface(),
							(DSAKeyPair) keyWorkerPane.getValue());

						m_payAccount.fetchAccountInfo(
											  JAPModel.getInstance().getPaymentProxyInterface(), true);
						break;
					}
					catch (IOException a_e)
					{
						m_connectionError = a_e;
					}
					catch (Exception ex)
					{

						if (!Thread.currentThread().isInterrupted() && ex.getMessage() != null &&
							!ex.getMessage().equals("CAPTCHA"))
						{
							//User has not pressed cancel and no io exception occured
							showPIerror(d.getContentPane(), ex);
							Thread.currentThread().interrupt();
							break;
						}
					}
				}
				m_connectionError = null;
			}
			public PayAccount getAccount()
			{
				Object account = getValue();
				if (account instanceof PayAccount)
				{
					return (PayAccount)account;
				}
				return null;
			}

			public Object getValue()
			{
				if (m_connectionError != null)
				{
					return m_connectionError;
				}
				return m_payAccount;
			}
		};
		WorkerContentPane panel2 = new WorkerContentPane(
			d, JAPMessages.getString(MSG_ACCOUNTCREATEDESC), keyWorkerPane, doIt)
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
		panel2.setInterruptThreadSafe(false);

		final CaptchaContentPane captcha = new CaptchaContentPane(d, panel2)
		{
			public void gotCaptcha(ICaptchaSender a_source, IImageEncodedCaptcha a_captcha)
			{
				if (keyCreationThread.getValue() != null)
				{
					// we might receive a capta from a previous request; ignore it!
					super.gotCaptcha(a_source, a_captcha);
				}
			}
		};
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
				try
				{
					if (doIt.getValue() instanceof IOException)
					{
						captcha.printErrorStatusMessage(
							JAPMessages.getString(MSG_BI_CONNECTION_LOST), LogType.NET);
					}
				}
				catch (Exception a_e)
				{

				}
				m_bDoNotCloseDialog = false;
			}
		});

		//First account, ask for password
		final boolean bFirstPayAccount = PayAccountsFile.getInstance().getNumAccounts() == 0;
		PasswordContentPane pc = new PasswordContentPane(d, captcha,
			PasswordContentPane.PASSWORD_NEW,
			JAPMessages.getString(MSG_ACCPASSWORD))
		{
			public CheckError[] checkYesOK()
			{
				CheckError[] errors = super.checkYesOK();

				if (errors == null || errors.length == 0)
				{
					setButtonValue(RETURN_VALUE_OK);
					if (getPassword() != null)
					{
						JAPController.getInstance().setPaymentPassword(new String(getPassword()));
					}
					else
					{
						JAPController.getInstance().setPaymentPassword("");
					}
				}
				return errors;
			}

			public boolean isSkippedAsNextContentPane()
			{
				return d.isVisible() && !bFirstPayAccount;
			}

			public boolean isSkippedAsPreviousContentPane()
			{
				return d.isVisible() && !bFirstPayAccount;
			}
		};


	final IReturnBooleanRunnable exportThread = new IReturnBooleanRunnable()
	{
			private Boolean m_bAccountSaved = new Boolean(false);

			public void run()
			{
				// save all accounts to the config file
				m_bDoNotCloseDialog = true;
				if (JAPController.getInstance().saveConfigFile())
				{
					// an error occured while saving the configuration
					JAPDialog.showErrorDialog(d, JAPMessages.getString(
						JAPController.MSG_ERROR_SAVING_CONFIG,
						JAPModel.getInstance().getConfigFile()), LogType.MISC);
					try
					{
						if (exportAccount(doIt.getAccount(), d.getContentPane(),
										  JAPController.getInstance().getPaymentPassword()))
						{
							m_bAccountSaved = new Boolean(true);
						}
						else
						{
							m_bAccountSaved = new Boolean(false);
						}
					}
					catch (Exception a_e)
					{
						LogHolder.log(LogLevel.EXCEPTION, LogType.MISC, a_e);
					}
				}
				else
				{
					m_bAccountSaved = new Boolean(true);
				}
				m_bDoNotCloseDialog = false;
			}

			public Object getValue()
			{
				if (!d.isVisible())
				{
					return new Boolean(true);
				}
				return m_bAccountSaved;
			}
			public boolean isTrue()
			{
				return ((Boolean)getValue()).booleanValue();
			}
		};

		WorkerContentPane saveConfig =
			new WorkerContentPane(d, JAPMessages.getString(MSG_SAVE_CONFIG) + WorkerContentPane.DOTS,
								  pc, exportThread)
		{
			public boolean isMoveBackAllowed()
			{
				return false;
			}
		};
		saveConfig.getButtonCancel().setEnabled(false);
		DialogContentPane saveErrorPane = new SimpleWizardContentPane(
			d, "<Font color=\"red\">" + JAPMessages.getString(MSG_CREATED_ACCOUNT_NOT_SAVED) + "</Font>",
			new DialogContentPane.Layout("", DialogContentPane.MESSAGE_TYPE_ERROR),
			new DialogContentPane.Options(saveConfig))
		{
			public boolean isSkippedAsNextContentPane()
			{
				return ( (Boolean) exportThread.getValue()).booleanValue();
			}

			public boolean isSkippedAsPreviousContentPane()
			{
				return true;
			}
		};
		saveErrorPane.getButtonCancel().setVisible(false);



		d.addWindowListener(new WindowAdapter()
		{
			public void windowClosing(WindowEvent e)
			{
				if (!m_bDoNotCloseDialog)
				{
					if (captcha.isVisible())
					{
						captcha.setButtonValue(IDialogOptions.RETURN_VALUE_CLOSED);
						captcha.checkCancel();
					}
					d.dispose();
				}
			}

			public void windowClosed(WindowEvent a_event)
			{
				PayAccountsFile.getInstance().removePaymentListener(captcha);
				updateAccountList();
				if (doIt.getValue() != null)
				{
					/** Select new account */
					m_listAccounts.setSelectedValue(doIt.getValue(), true);
					/*
					if ( ( (Boolean) exportThread.getValue()).booleanValue())
					{
						doChargeAccount(doIt.getAccount());
					}*/
				}
			}
		});


		m_bDoNotCloseDialog = false;
		doChargeAccount(doIt, d, saveErrorPane, exportThread);
		PITestWorkerPane.updateDialogOptimalSized(fetchBiWorker);
		d.setLocationCenteredOnOwner();
		d.setVisible(true);

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
	private void doSelectAccount(PayAccount selectedAccount)
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

	private DialogContentPane createUpdateAccountPane(final IReturnAccountRunnable a_accountCreationThread,
		final MethodSelectionPane a_methodSelectionPane,
		final JAPDialog a_parentDialog, DialogContentPane a_previousContentPane)
	{
		Thread t = new Thread()
		{
			public void run()
			{
				try
				{
					a_accountCreationThread.getAccount().fetchAccountInfo(
									   JAPModel.getInstance().getPaymentProxyInterface(), true);
					updateAccountList();
				}
				catch (Exception e)
				{
					if (!Thread.currentThread().isInterrupted())
					{
						showPIerror(a_parentDialog.getContentPane(), e);
						LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, "Could not get account statement");
						Thread.currentThread().interrupt();
					}
				}
			}
		};
		WorkerContentPane worker = new WorkerContentPane(a_parentDialog,
			JAPMessages.getString(MSG_GETACCOUNTSTATEMENT), a_previousContentPane, t)
		{
			public boolean isSkippedAsNextContentPane()
			{
				if (a_methodSelectionPane == null ||
					a_methodSelectionPane.getSelectedPaymentOption() == null ||
					a_methodSelectionPane.getSelectedPaymentOption().getType().equalsIgnoreCase(
									   XMLPaymentOption.OPTION_PASSIVE))
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
				return isSkippedAsNextContentPane();
			}
		};
		worker.setInterruptThreadSafe(false);
		return worker;
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
		JAPDialog busy = new JAPDialog(GUIUtils.getParentWindow(getRootPanel()),
									   JAPMessages.getString(MSG_GETACCOUNTSTATEMENTTITLE), true);
		DialogContentPane worker =
			createUpdateAccountPane(new FixedReturnAccountRunnable(a_selectedAccount), null, busy, null);
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
		if (selectedAccount == null)
		{
			return;
		}
		if (selectedAccount.getPrivateKey() != null)
		{
			JAPDialog d = new JAPDialog(GUIUtils.getParentWindow(this.getRootPanel()),
										JAPMessages.getString(MSG_ACCPASSWORDTITLE), true);

			PasswordContentPane p = new PasswordContentPane(d, PasswordContentPane.PASSWORD_NEW,
				JAPMessages.getString(MSG_EXPORTENCRYPT));
			p.updateDialog();
			d.pack();
			d.setVisible(true);
			if (p.getButtonValue() == PasswordContentPane.RETURN_VALUE_OK)
			{
				if (exportAccount(selectedAccount, this.getRootPanel(), new String(p.getPassword())))
				{
					selectedAccount.setBackupDone(true);
					doShowDetails(selectedAccount);
				}
			}
		}
		else
		{
			// account is already encrypted, save it only
			if (exportAccount(selectedAccount, this.getRootPanel(), null))
			{
				selectedAccount.setBackupDone(true);
				doShowDetails(selectedAccount);
			}
		}
	}

	private boolean exportAccount(PayAccount selectedAccount, Component a_parent, String strPassword)
	{
		JFileChooser chooser = new JFileChooser();
		chooser.setSelectedFile(new File(selectedAccount.getAccountNumber() + MyFileFilter.ACCOUNT_EXTENSION));
		MyFileFilter filter = new MyFileFilter();
		chooser.setFileFilter(filter);
		while (true)
		{
			int returnVal = chooser.showSaveDialog(a_parent);
			if (returnVal == JFileChooser.APPROVE_OPTION)
			{
				try
				{
					File f = chooser.getSelectedFile();
					if (!f.getName().toLowerCase().endsWith(MyFileFilter.ACCOUNT_EXTENSION))
					{
						f = new File(f.getParent(), f.getName() + MyFileFilter.ACCOUNT_EXTENSION);
					}
					if (f.exists())
					{
						if (!JAPDialog.showYesNoDialog(GUIUtils.getParentWindow(this.getRootPanel()),
							JAPMessages.getString(MSG_FILE_EXISTS)))
						{
							continue;
						}
					}

					Document doc = XMLUtil.createDocument();
					Element elemRoot = doc.createElement("root");
					elemRoot.setAttribute("filetype", "JapAccountFile");
					elemRoot.setAttribute("version", "1.1");

					doc.appendChild(elemRoot);
					Element elemAccount = selectedAccount.toXmlElement(doc, strPassword);
					elemRoot.appendChild(elemAccount);
					/*
						 if (strPassword != null && strPassword.length() > 0)
						 {
					 XMLEncryption.encryptElement(elemAccount, strPassword);
						 }*/

					String strOutput = XMLUtil.toString(XMLUtil.formatHumanReadable(doc));
					FileOutputStream outStream = new FileOutputStream(f);
					outStream.write(strOutput.getBytes());
					outStream.close();
					JAPDialog.showMessageDialog(GUIUtils.getParentWindow(this.getRootPanel()),
												JAPMessages.getString(MSG_EXPORTED));
					return true;
				}
				catch (Exception e)
				{
					JAPDialog.showErrorDialog(GUIUtils.getParentWindow(a_parent),
											  JAPMessages.getString(MSG_NOTEXPORTED) + ": " + e, LogType.PAY);
				}
			}
			break;
		}
		return false;
	}

/**
	 * Filefilter for the import function
	 *
	 * @author Bastian Voigt
	 * @version 1.0
	 */
	private static class MyFileFilter extends FileFilter
	{
		public static final String ACCOUNT_EXTENSION = ".acc";
		private final String ACCOUNT_DESCRIPTION = "JAP Accountfile (*" + ACCOUNT_EXTENSION + ")";

		private int filterType;

		public int getFilterType()
		{
			return filterType;
		}

		public boolean accept(File f)
		{
			return f.isDirectory() || f.getName().endsWith(ACCOUNT_EXTENSION);
		}

		public String getDescription()
		{
			return ACCOUNT_DESCRIPTION;
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
				Document doc = XMLUtil.readXMLDocument(f);
				XMLUtil.removeComments(doc);
				Element elemRoot = doc.getDocumentElement();
				elemAccount = (Element) XMLUtil.getFirstChildByName(elemRoot, PayAccount.XML_ELEMENT_NAME);

				// maybe it was encrypted; only for compatibility with old export format 1.0, remove!
				if (elemAccount == null)
				{
					Element elemCrypt =
						(Element) XMLUtil.getFirstChildByName(elemRoot, XMLEncryption.XML_ELEMENT_NAME);
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
				JAPDialog.showErrorDialog(getRootPanel(),
										  JAPMessages.getString(MSG_ACCOUNT_IMPORT_FAILED), LogType.MISC, e);
			}
			try
			{
				if (elemAccount != null)
				{
					XMLUtil.removeComments(elemAccount);

					importedAccount = new PayAccount(elemAccount, null);
					importedAccount.setBackupDone(true); // we know there is a backup file...
					PayAccountsFile accounts = PayAccountsFile.getInstance();
					accounts.addAccount(importedAccount);
					doActivateAccount(importedAccount);
					updateAccountList();
				}
			}
			catch (Exception ex)
			{
				String message = "";

				if (ex instanceof PayAccountsFile.AccountAlreadyExisting)
				{
					message = JAPMessages.getString(MSG_ACCOUNT_ALREADY_EXISTING);
				}
				JAPDialog.showErrorDialog(getRootPanel(),
										  JAPMessages.getString(MSG_ACCOUNT_IMPORT_FAILED) + message,
										  LogType.MISC, ex);
			}
		}
	}

	private void doActivateAccount(PayAccount a_selectedAccount)
	{
		if (a_selectedAccount != null)
		{
			Enumeration accounts;
			PayAccount currentAccount;
			JAPDialog dialog = new JAPDialog(getRootPanel(), JAPMessages.getString(MSG_ACCPASSWORDTITLE));
			dialog.setDefaultCloseOperation(JAPDialog.HIDE_ON_CLOSE);
			PasswordContentPane contentPane =
				new PasswordContentPane(dialog, PasswordContentPane.PASSWORD_ENTER,
										JAPMessages.getString(MSG_GIVE_ACCOUNT_PASSWORD));
			contentPane.setDefaultButtonOperation(DialogContentPane.ON_CLICK_HIDE_DIALOG);
			contentPane.updateDialog();
			dialog.pack();
			try
			{
				a_selectedAccount.decryptPrivateKey(contentPane);

				// try to decrypt all inactive accounts with this password
				try
				{
					accounts = PayAccountsFile.getInstance().getAccounts();
					while (accounts.hasMoreElements())
					{
						currentAccount = (PayAccount) accounts.nextElement();
						currentAccount.decryptPrivateKey(
											  new SingleStringPasswordReader(contentPane.getPassword()));
					}
				}
				catch (Exception a_e)
				{
					LogHolder.log(LogLevel.EXCEPTION, LogType.GUI, a_e);
				}

				// set the active account if none exists
				if (PayAccountsFile.getInstance().getActiveAccount() == null)
				{
					if (a_selectedAccount.getPrivateKey() != null)
					{
						PayAccountsFile.getInstance().setActiveAccount(a_selectedAccount);
					}
					else
					{
						accounts = PayAccountsFile.getInstance().getAccounts();
						while (accounts.hasMoreElements())
						{
							currentAccount = (PayAccount)accounts.nextElement();
							if (currentAccount.getPrivateKey() != null)
							{
								PayAccountsFile.getInstance().setActiveAccount(currentAccount);
							}
						}
					}
				}

				doShowDetails(a_selectedAccount);
				enableDisableButtons();
				m_listAccounts.repaint();

				if (a_selectedAccount.getPrivateKey() != null)
				{
					JAPDialog.showMessageDialog(getRootPanel(),
												JAPMessages.getString(MSG_ACTIVATION_SUCCESSFUL));
				}
			}
			catch(Exception a_e)
			{
				JAPDialog.showErrorDialog(getRootPanel(),
										  JAPMessages.getString(MSG_ACTIVATION_FAILED), LogType.PAY, a_e);
			}
			dialog.dispose();
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
		boolean reallyDelete = false;

		if (accounts.getActiveAccount() == selectedAccount && JAPController.getInstance().getAnonMode())
		{
			JAPDialog.showMessageDialog(GUIUtils.getParentWindow(this.getRootPanel()),
										JAPMessages.getString(MSG_CONNECTIONACTIVE));
			return;
		}

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
				boolean yes = JAPDialog.showYesNoDialog(GUIUtils.getParentWindow(this.getRootPanel()),
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
				doShowDetails(getSelectedAccount());
			}
			catch (Exception a_ex)
			{
				JAPDialog.showErrorDialog(GUIUtils.getParentWindow(getRootPanel()),
					JAPMessages.getString(MSG_ERROR_DELETING), LogType.MISC, a_ex);
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
		this.setHelpContext();
		updateAccountList();
	}

	/**
	 * This panel has one or more tabs. Depending on which tab is active, another help context has to be set.
	 * @return
	 */
	private void setHelpContext(){
		int index = 0;
		index = this.m_tabPane.getSelectedIndex();
		String context = null;
		switch (index){
			case 0: context = "payment";
					break;
			case 1: context = "payment_extend";
					break;
		}
		JAPHelp.getInstance().getContextObj().setContext(context);
	}

	/**
	 * This method can be overwritten by the children of AbstractJAPConfModule. It is called
	 * every time the user presses "OK" in the configuration dialog.
	 */
	protected boolean onOkPressed()
	{
		JAPController.getInstance().setDontAskPayment(!m_cbxShowPaymentConfirmation.isSelected());
		JAPModel.getInstance().allowPaymentViaDirectConnection(m_cbxAllowNonAnonymousConnection.isSelected());
		PayAccountsFile.getInstance().setIgnoreAIAccountError(!m_cbxShowAIErrors.isSelected());
		PayAccountsFile.getInstance().setBalanceAutoUpdateEnabled(m_cbxBalanceAutoUpdateEnabled.isSelected());

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
		m_cbxShowPaymentConfirmation.setSelected(true);
		m_cbxAllowNonAnonymousConnection.setSelected(true);
		m_cbxShowAIErrors.setSelected(true);
		m_cbxBalanceAutoUpdateEnabled.setSelected(true);
	}

	/**
	 * Fetches new (changed) account data from the PayAccountsFile
	 */
	protected void onUpdateValues()
	{
		m_cbxShowPaymentConfirmation.setSelected(!JAPController.getInstance().getDontAskPayment());
		m_cbxAllowNonAnonymousConnection.setSelected(
			  JAPModel.getInstance().isPaymentViaDirectConnectionAllowed());
		m_cbxShowAIErrors.setSelected(!PayAccountsFile.getInstance().isAIAccountErrorIgnored());
		m_cbxBalanceAutoUpdateEnabled.setSelected(PayAccountsFile.getInstance().isBalanceAutoUpdateEnabled());

		/*
		PayAccountsFile accounts = PayAccountsFile.getInstance();
		Enumeration enumAccounts = accounts.getAccounts();
		while (enumAccounts.hasMoreElements())
		{
			PayAccount a = (PayAccount) enumAccounts.nextElement();
		}*/
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

	public void showPIerror(Component a_parent, Exception a_e)
	{
		if (!JAPModel.getInstance().isAnonConnected() &&
			!JAPModel.getInstance().isPaymentViaDirectConnectionAllowed())
		{
			JAPDialog.showErrorDialog(a_parent,
									  JAPMessages.getString(MSG_DIRECT_CONNECTION_FORBIDDEN), LogType.PAY);
		}
		else if (!JAPModel.getInstance().isAnonConnected())
		{
			JAPDialog.showErrorDialog(a_parent,
									  JAPMessages.getString(MSG_NO_ANONYMITY_POSSIBLY_BLOCKED), LogType.PAY);
		}
		else if (a_e instanceof ForbiddenIOException)
		{
			JAPDialog.showErrorDialog(a_parent, JAPMessages.getString(MSG_ERROR_FORBIDDEN), LogType.PAY);
		}
		else
		{
			JAPDialog.showErrorDialog(a_parent, JAPMessages.getString(MSG_CREATEERROR), LogType.PAY);
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
				if ( ( (PayAccount) value).getPrivateKey() == null)
				{
					// encrypted account
					l = new JLabel(String.valueOf( ( (PayAccount) value).getAccountNumber()),
								   GUIUtils.loadImageIcon(AccountSettingsPanel.IMG_COINS_DISABLED, true),
								   LEFT);
				}
				else
				{
					l = new JLabel(String.valueOf( ( (PayAccount) value).getAccountNumber()),
								   GUIUtils.loadImageIcon(JAPConstants.IMAGE_COINS_FULL, true), LEFT);
				}
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

	private static interface IReturnAccountRunnable extends WorkerContentPane.IReturnRunnable
	{
		public PayAccount getAccount();
	}

	private static interface IReturnBooleanRunnable extends WorkerContentPane.IReturnRunnable
	{
		public boolean isTrue();
	}


	private static final class FixedReturnAccountRunnable implements IReturnAccountRunnable
	{
		private PayAccount m_account;

		public FixedReturnAccountRunnable(PayAccount a_account)
		{
			m_account = a_account;
		}

		public Object getValue()
		{
			return m_account;
		}

		public PayAccount getAccount()
		{
			return m_account;
		}

		public void run()
		{}
	}
}
