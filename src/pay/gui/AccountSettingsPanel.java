package pay.gui;

import java.util.Enumeration;

import java.awt.BorderLayout;
import java.awt.GridLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.border.EmptyBorder;
import javax.swing.table.AbstractTableModel;
import javax.swing.table.JTableHeader;

import jap.AbstractJAPConfModule;
import jap.JAPController;
import jap.JAPView;
import jap.JAPWaitSplash;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import pay.Pay;
import pay.PayAccount;
import pay.PayAccountsFile;

/**
 * The Jap Conf Module (Settings Tab Page) for the Accounts and payment Management
 *
 * @author Bastian Voigt
 * @version 1.0
 */
public class AccountSettingsPanel extends jap.AbstractJAPConfModule
{
	private JTable m_Table;
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
		LogHolder.log(LogLevel.DEBUG, LogType.PAY, "AccountSettingsPanel.recreateRootPanel()!!!!!!");
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

		JPanel eastPanel = new JPanel(new GridLayout(12, 1));
		eastPanel.setBorder(new EmptyBorder(5, 5, 5, 5));
		eastPanel.add(new JButton("Details"));
		eastPanel.add(new JButton("Aufladen"));
		JButton btnCreateAccount = new JButton("Neues Konto");
		btnCreateAccount.addActionListener(new CreateAccountListener());
		eastPanel.add(btnCreateAccount);
		centerPanel.add(eastPanel, BorderLayout.EAST);
		rootPanel.add(centerPanel, BorderLayout.CENTER);
	}

	/**
	 * Handler for the CreateAccount Button
	 * @author Bastian Voigt
	 * @version 1.0
	 */
	private class CreateAccountListener implements ActionListener
	{
		public void actionPerformed(ActionEvent e)
		{
			JAPView view = JAPController.getView();
			LogHolder.log(LogLevel.DEBUG, LogType.PAY, "CreateAccount button clicked");

			// TODO: internationalize: JAPMessage.get....()
			int choice = JOptionPane.showOptionDialog(
				view,
				"<html>Um ein Konto anzulegen, muss JAP zun&auml;chst ein<br>" +
				"Schl&uuml;sselpaar erzeugen, das dauert einige Sekunden.<br>" +
				"Danach wird der &ouml;ffentliche Schl&uuml;ssel an die<br>" +
				"Bezahlinstanz geschickt, diese er&ouml;ffnet dann das Konto.<br>" +
				"Mouml;chten Sie fortfahren?</html>",
				"Konto anlegen",
				JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE,
				null, null, null
				);
			if (choice == JOptionPane.YES_OPTION)
			{
				JAPWaitSplash splash = null;
				try
				{
					splash = JAPWaitSplash.start("Creating new account..", "Please wait");
					Pay.getInstance().createAccount();
					splash.abort();
				}
				catch (Exception ex)
				{
					splash.abort();
					LogHolder.log(LogLevel.DEBUG, LogType.PAY, "Exception CreateAccount: " + ex.toString());
					JOptionPane.showMessageDialog(
						view,
						"Error creating account: " + ex.getMessage(),
						"Error", JOptionPane.ERROR_MESSAGE
						);
				}
				m_MyTableModel.fireTableDataChanged();
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
	protected void onOkPressed()
	{
		// TODO: JAPController.speichereDenScheiss();
	}

	/**
	 * This method can be overwritten by the children of AbstractJAPConfModule. It is called
	 * every time the user presses "Cancel" in the configuration dialog after the restoring
	 * of the savepoint data (if there is a savepoint for this module).
	 */
	protected void onCancelPressed()
	{
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
	public void accountDataChanged()
	{
		if (m_MyTableModel != null)
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
						return account.getAccountInfo().getBalance();
					}
					else
					{
						return "unbekannt";
					}
				case 3:
					if (account.hasAccountInfo())
					{
						return account.getAccountInfo().getValidTime();
					}
					else
					{
						return "unbekannt";
					}
				case 4:
					if (account.equals(m_accounts.getActiveAccount()))
					{
						return "aktiv";
					}
					else
					{
						return "/";
					}
				default:
					return "unbekannt";
			}
		}
	}
}
