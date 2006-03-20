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

import java.util.Date;
import java.util.Vector;

import java.awt.FlowLayout;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Window;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import anon.pay.BIConnection;
import anon.pay.PayAccount;
import anon.pay.xml.XMLTransCert;
import anon.pay.xml.XMLTransactionOverview;
import gui.GUIUtils;
import gui.JAPMessages;
import gui.dialog.JAPDialog;
import jap.JAPConstants;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import jap.JAPUtil;
import jap.JAPModel;

/** This dialog shows an overview of transaction numbers for an account
 *
 *  @author Tobias Bayer
 */
public class TransactionOverviewDialog extends JAPDialog implements ActionListener
{
	/** Messages */
	private static final String MSG_OK_BUTTON = TransactionOverviewDialog.class.
		getName() + "_ok_button";
	private static final String MSG_RELOADBUTTON = TransactionOverviewDialog.class.
		getName() + "_reloadbutton";
	private static final String MSG_CANCELBUTTON = TransactionOverviewDialog.class.
		getName() + "_cancelbutton";
	private static final String MSG_FETCHING = TransactionOverviewDialog.class.
		getName() + "_fetching";
	private static final String MSG_TAN = TransactionOverviewDialog.class.
		getName() + "_tan";
	private static final String MSG_AMOUNT = TransactionOverviewDialog.class.
		getName() + "_amount";
	private static final String MSG_USED_DATE = TransactionOverviewDialog.class.
		getName() + "_used_date";
	private static final String MSG_RECEIVED_DATE = TransactionOverviewDialog.class.
		getName() + "_received_date";

	private JTable m_tList;
	private JButton m_okButton, m_reloadButton;
	private PayAccount m_account;
	private JLabel m_fetchingLabel;
	private AccountSettingsPanel m_parent;

	public TransactionOverviewDialog(AccountSettingsPanel a_parent, String title, boolean modal,
									 PayAccount a_account)
	{
		super(GUIUtils.getParentWindow(a_parent.getRootPanel()), title, modal);
		m_parent = a_parent;

		try
		{
			m_account = a_account;
			setDefaultCloseOperation(DISPOSE_ON_CLOSE);
			jbInit();
			setModal(true);
			setSize(450, 400);
			setVisible(true);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			LogHolder.log(LogLevel.EXCEPTION, LogType.PAY,
						  "Could not create TransactionOverviewDialog: " + e.getMessage());
		}
	}

	private void jbInit() throws Exception
	{
		JPanel panel1 = new JPanel(new GridBagLayout());
		JPanel bttnPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = c.NORTHWEST;
		c.insets = new Insets(5, 5, 5, 5);

		m_tList = new JTable();

		c.gridx = 0;
		c.gridy = 0;
		c.weighty = 1;
		c.fill = GridBagConstraints.BOTH;
		panel1.add(new JScrollPane(m_tList), c);
		c.weightx = 0;
		c.weighty = 0;
		c.fill = c.NONE;

		//The fetching label
		c.gridy++;
		m_fetchingLabel = new JLabel(JAPMessages.getString(MSG_FETCHING),
									 GUIUtils.loadImageIcon(JAPConstants.BUSYFN, true), JLabel.LEADING);
		m_fetchingLabel.setHorizontalTextPosition(JLabel.LEADING);
		panel1.add(m_fetchingLabel, c);

		//The Reload button
		m_reloadButton = new JButton(JAPMessages.getString(MSG_RELOADBUTTON));
		m_reloadButton.addActionListener(this);
		bttnPanel.add(m_reloadButton);

		//The Ok button
		m_okButton = new JButton(JAPMessages.getString(MSG_CANCELBUTTON));
		m_okButton.addActionListener(this);
		bttnPanel.add(m_okButton);

		//Add the button panel
		c.gridy = 5;
		c.gridx = 0;
		c.weightx = 1;
		c.anchor = c.SOUTHEAST;
		panel1.add(bttnPanel, c);

		getContentPane().add(panel1);
		doFill();

	}

	private void doFill()
	{
		m_reloadButton.setEnabled(false);
		m_fetchingLabel.setVisible(true);

		Runnable fillList = new Runnable()
		{
			public void run()
			{

				Vector transCerts = m_account.getTransCerts();
				XMLTransactionOverview overview = new XMLTransactionOverview();
				for (int i = 0; i < transCerts.size(); i++)
				{
					XMLTransCert cert = (XMLTransCert) transCerts.elementAt(i);
					overview.addTan(cert.getTransferNumber());
				}

				BIConnection biConn = new BIConnection(m_account.getBI());
				try
				{
					biConn.connect(JAPModel.getInstance().getProxyInterface());
					biConn.authenticate(m_account.getAccountCertificate(), m_account.getSigningInstance());
					overview = biConn.fetchTransactionOverview(overview);
					MyTableModel tableModel = new MyTableModel(overview);
					m_tList.setEnabled(true);
					m_tList.setModel(tableModel);
				}
				catch (Exception e)
				{
					LogHolder.log(LogLevel.EXCEPTION, LogType.PAY,
								  "Cannot connect to Payment Instance: " + e.getMessage());
					m_parent.showPIerror(getRootPane());
				}

				m_okButton.setText(JAPMessages.getString(MSG_OK_BUTTON));
				m_fetchingLabel.setVisible(false);
				m_reloadButton.setEnabled(true);
			}
		};

		Thread t = new Thread(fillList);
		t.start();

	}

	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == m_okButton)
		{
			dispose();
		}
		else if (e.getSource() == m_reloadButton)
		{
			doFill();
		}
	}

	/**
	 * Tabel model
	 */
	private class MyTableModel extends AbstractTableModel
	{
		private XMLTransactionOverview m_overview;

		public MyTableModel(XMLTransactionOverview a_overview)
		{
			super();
			m_overview = a_overview;
		}

		public int getColumnCount()
		{
			return 4;
		}

		public int getRowCount()
		{
			return m_overview.size();
		}

		public Class getColumnClass(int c)
		{
			switch (c)
			{
				case 0:
					return String.class;
				case 1:
					return Date.class;
				case 2:
					return String.class;
				case 3:
					return Date.class;
				default:
					return Object.class;
			}
		}

		public Object getValueAt(int rowIndex, int columnIndex)
		{
			String[] line = (String[]) m_overview.getTans().elementAt(rowIndex);
			switch (columnIndex)
			{
				case 0:
					return line[0];
				case 1:
					Vector transCerts = m_account.getTransCerts();
					for (int i = 0; i < transCerts.size(); i++)
					{
						XMLTransCert tc = (XMLTransCert) transCerts.elementAt(i);
						if (tc.getTransferNumber() == Long.parseLong(line[0]))
						{
							return tc.getReceivedDate();
						}
					}
					return null;
				case 2:
					if (new Boolean(line[1]).booleanValue())
					{
						return JAPUtil.formatBytesValue(Long.parseLong(line[3]));
					}
					else
					{
						return "";
					}
				case 3:
					if (!line[2].equals("0"))
					{
						return new Date(Long.parseLong(line[2]));
					}
					else
					{
						return null;
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
					return JAPMessages.getString(MSG_TAN);
				case 1:
					return JAPMessages.getString(MSG_RECEIVED_DATE);
				case 2:
					return JAPMessages.getString(MSG_AMOUNT);
				case 3:
					return JAPMessages.getString(MSG_USED_DATE);

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
