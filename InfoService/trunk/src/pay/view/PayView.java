/*
 Copyright (c) 2000, The JAP-Team
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
package pay.view;

import java.net.URL;
import java.util.Locale;
import java.awt.Dimension;
import java.awt.GridBagLayout;
import java.awt.event.ActionEvent;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import pay.Pay;
import pay.PayAccount;
import pay.control.PayControl;
import pay.data.Literals;
//import pay.event.ActionThread;
import pay.util.BrowserStart;
import pay.util.PayText;
import pay.util.URLSpec;

/**
 * Dies Klasse bietet eine GUI zum addiern, l\uFFFDschen, aufladen von PayAccounts
 * Es ist die Haupt GUI Klasse der Pay Anwendung
 **/

public class PayView extends JPanel
{

	String manuell = PayText.get("startBrowserText");

	KontenTable kontenTable;
	JTable table;
	MyGridBag gridBag;
	//UserDaten user;
	public UserDatenView userPanel;
	Pay pay;
	Dimension tableDimension;

	JButton neuesKonto;
	JButton aufladen;
	JButton loeschen;
	JButton kontostand;
	JLabel markedAccount;

	public PayView()
	{
		//super(BoxLayout.Y_AXIS);

		PayControl.initGui();
		setLayout(new GridBagLayout());
		gridBag = new MyGridBag();

		//user = UserDaten.create();
		pay = Pay.getInstance();

		table = createKontenTable(new KontenTable());
		JScrollPane scroll = new JScrollPane(table);

		loeschen = new JButton(PayText.get("delete"));
		aufladen = new JButton(PayText.get("charge"));
		kontostand = new JButton(PayText.get("refreshAccount"));
		markedAccount = new JLabel(PayText.get("markedAccount") + ": ");
		neuesKonto = new JButton(PayText.get("addAccount"));
		userPanel = new UserDatenView(this);

		markedAccount.setFont(Literals.NORM_FONT_BOLD);

		//add(createVerticalGlue());
		//add(new KontenControlBox());
		//add(scroll);
		//add(userPanel);
		//add(createVerticalGlue());

		add(neuesKonto, gridBag.feld(0, 0).inset(2, 0, 2, 0).remain().anchor(gridBag.WEST));
		add(Box.createVerticalStrut(20), gridBag.feld(0, 1).inset(2, 0, 2, 0).remain());

		add(markedAccount, gridBag.feld(0, 2).size(2, 1).anchor(gridBag.WEST));

		add(loeschen, gridBag.feld(2, 2).anchor(gridBag.EAST).size(1, 1));
		add(aufladen, gridBag.feld(3, 2).anchor(gridBag.EAST));
		add(kontostand, gridBag.feld(4, 2).anchor(gridBag.WEST));

		add(scroll, gridBag.feld(0, 3).remain().anchor(gridBag.WEST));
		add(userPanel, gridBag.feld(0, 4).anchor(gridBag.EAST).remain());
		addAnonymActions();
	}

	public JTable createKontenTable(KontenTable model)
	{
/*		kontenTable = model;*/
		JTable table = new JTable(/* model */);
/*		table.getColumnModel().getColumn(0).setPreferredWidth(15);
		table.getColumnModel().getColumn(1).setPreferredWidth(40);
		table.getColumnModel().getColumn(2).setPreferredWidth(30);
		table.getColumnModel().getColumn(3).setPreferredWidth(25);*/
		tableDimension = new Dimension(400, 80);
		table.setPreferredScrollableViewportSize(tableDimension);
		return null;
	}

	public class KontenControlBox extends Box
	{
		public KontenControlBox()
		{
			super(BoxLayout.X_AXIS);

			Box box3 = new Box(BoxLayout.X_AXIS);
			box3.add(loeschen);
			box3.add(aufladen);
			box3.add(kontostand);

			Box box1 = new Box(BoxLayout.Y_AXIS);
			box1.add(Box.createVerticalGlue());
			box1.add(neuesKonto);

			Box box2 = new Box(BoxLayout.Y_AXIS);
			box2.add(markedAccount);
			box2.add(box3);

			add(box1);
			add(Box.createHorizontalGlue());
			add(box2);
		}
	}

	public Dimension getPreferredSize()
	{
		Dimension d = new Dimension(tableDimension);
		d.height += 140;
		d.width += 40;
		return d;
	}

	private void addAnonymActions()
	{
/*		loeschen.addActionListener(new ActionThread(getParent(), PayText.get("delete"))
		{
			public void action(ActionEvent e)
			{
				if (table.getSelectedRow() != -1)
				{
					if (JOptionPane.OK_OPTION == JOptionPane.showConfirmDialog(
						getParent(), PayText.get("reallyDelete"), PayText.get("deleteDialog"),
						JOptionPane.OK_CANCEL_OPTION))
					{
						PayAccount account = kontenTable.getRow(table.getSelectedRow());
						boolean del = Pay.getInstance().deleteAccount(account.getAccountNumber());
						if (!del)
						{
							JOptionPane.showMessageDialog(getParent(), PayText.get("notDeleted"));
						}
					}
				}
			}
		});
		aufladen.addActionListener(new ActionThread(getParent(), PayText.get("charge"))
		{
			public void action(ActionEvent e)
			{
				long transNr = -1;
				LogHolder.log(LogLevel.DEBUG, LogType.PAY,
							  "kontenTbasel= " + kontenTable + "  table= " + table);
				PayAccount account = kontenTable.getAccount(table.getSelectedRow());
				URL url = null;
				try
				{
					transNr = Pay.getInstance().chargeAccount(account.getAccountNumber());
					url = new URL(Literals.PROTOKOLL, Literals.CHARGEHOST, Literals.CHARGEPATH);

				}
				catch (Exception ex)
				{
					LogHolder.log(LogLevel.DEBUG, LogType.PAY, "aufladen nicht geklappt");
				}
				LogHolder.log(LogLevel.DEBUG, LogType.PAY,
							  new URLSpec(Literals.CHARGEFILE, Literals.CHARGEPARAMS).toString());
				new BrowserStart(getParent(), Literals.BROWSERLIST, url,
								 new URLSpec(Literals.CHARGEFILE, Literals.CHARGEPARAMS),
								 PayText.get("startBrowserText")).start(new String[]
					{transNr + "", Locale.getDefault().getLanguage()});
			}
		});
		neuesKonto.addActionListener(new ActionThread(getParent(), PayText.get("addAccount"))
		{
			public void action(ActionEvent event)
			{
				LogHolder.log(LogLevel.DEBUG, LogType.PAY, "addAccount");
				if (pay.initDone())
				{
					try
					{
						pay.registerNewAccount();
					}
					catch (Exception e1)
					{
						e1.printStackTrace();
						LogHolder.log(LogLevel.DEBUG, LogType.PAY,
									  "neue Konto erschaffen geht nicht: " + e1.getMessage());
					}
				}
				else
				{
					LogHolder.log(LogLevel.DEBUG, LogType.PAY,
								  "neue Konto erschaffen geht nicht falsche IP-Adresse oder AccountDatei nicht geladen");
				}
			}
		});
		kontostand.addActionListener(new ActionThread(getParent(), PayText.get("refreshAccount"))
		{
			public void action(ActionEvent event)
			{
				LogHolder.log(LogLevel.DEBUG, LogType.PAY, "refreshAccount");
				if (pay.initDone())
				{
					if (table.getSelectedRow() != -1)
					{
<<<<<<< PayView.java
						pay.getAccountInfo(kontenTable.getAccountNumber(table.getSelectedRow()));
=======
						pay.getAccountInfo(kontenTable.getAccountNumber(table.getSelectedRow())).getBalance();
>>>>>>> 1.6
					}
					else
					{
						LogHolder.log(LogLevel.DEBUG, LogType.PAY, "keine Zeile markiert");
					}
				}
				else
				{
					LogHolder.log(LogLevel.DEBUG, LogType.PAY,
								  "aktuelles Guthaben anzeigen geht nicht Pay Instance Server ist nicht initialisiert");
				}
			}
		});*/
	}

	public void neuMalen()
	{
		repaint();
	}

}
