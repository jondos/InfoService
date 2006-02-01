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

import anon.util.Util;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;

import anon.pay.BIConnection;
import anon.pay.PayAccount;
import anon.pay.xml.XMLTransCert;
import anon.pay.xml.XMLTransactionOverview;
import gui.JAPMessages;
import jap.JAPConstants;
import jap.JAPUtil;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/** This dialog shows an overview of transaction numbers for an account
 *
 *  @author Tobias Bayer
 */
public class TransactionOverviewDialog extends JDialog implements ActionListener
{
	/** Messages */
	private static final String MSG_OK_BUTTON = TransactionOverviewDialog.class.
		getName() + "_ok_button";
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
	private JButton m_okButton;
	private PayAccount m_account;
	private JLabel m_fetchingLabel;

	public TransactionOverviewDialog(Frame owner, String title, boolean modal, PayAccount a_account)
	{
		super(owner, title, modal);
		try
		{
			m_account = a_account;
			setDefaultCloseOperation(DISPOSE_ON_CLOSE);
			jbInit();
			pack();
			setModal(true);
			setSize(450, 300);
			JAPUtil.centerFrame(this);
			show();
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
		c.fill = c.BOTH;
		panel1.add(new JScrollPane(m_tList), c);
		c.weightx = 0;
		c.weighty = 0;
		c.fill = c.NONE;

		//The fetching label
		m_fetchingLabel = new JLabel(JAPMessages.getString(MSG_FETCHING),
									 JAPUtil.loadImageIcon(JAPConstants.BUSYFN, true), JLabel.LEADING);
		//m_fetchingLabel.setVerticalTextPosition(JLabel.);
		m_fetchingLabel.setHorizontalTextPosition(JLabel.LEADING);
		bttnPanel.add(m_fetchingLabel);

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
					biConn.connect();
					biConn.authenticate(m_account.getAccountCertificate(), m_account.getSigningInstance());
					overview = biConn.fetchTransactionOverview(overview);
				}
				catch (Exception e)
				{
					LogHolder.log(LogLevel.EXCEPTION, LogType.PAY,
								  "Cannot connect to Payment Instance: " + e.getMessage());
				}
				MyTableModel tableModel = new MyTableModel(overview);
				m_tList.setEnabled(true);
				m_tList.setModel(tableModel);
				m_okButton.setText(MSG_OK_BUTTON);
				m_fetchingLabel.setVisible(false);
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
				case 2:
					if (new Boolean(line[1]).booleanValue())
					{
						float amount = Util.parseFloat(line[3]);
						amount /= (1024 * 1024);
						int amountInt = Math.round(amount);
						return String.valueOf(amountInt) + " MB";
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
