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

import java.util.Vector;
import java.awt.Dimension;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.table.AbstractTableModel;
import pay.Pay;
import pay.PayAccount;
import pay.event.ModelEvent;
import pay.event.ModelListener;
import pay.util.PayText;
import logging.*;

/**
 *	KontenTabel ist ein TableModell welches die Daten aus dem PayAcocuntFile
 *	in Tabellenform organisiert. Die Daten bleiben dabei im PayAccountFile
 * 	KonteTabel ist also kein eigentliches Model sondern ein mittler zwischen
 *	JTable und PayAccountFile
 **/


public class KontenTable extends AbstractTableModel implements ModelListener{

		int rowSize = 16;
		int columnSize = 100;

		private String aktuell = PayText.get("usedAccount");

		private Vector daten;
		private String[] names ={aktuell,PayText.get("accountNr"),PayText.get("validTo"), PayText.get("credit")};


		public KontenTable(){
			Pay pay = Pay.create();
			daten = pay.getAccountVec();
			pay.addModelListener(this);
			LogHolder.log(LogLevel.DEBUG,LogType.PAY,"[konstruktor] Rows: "+getRowCount());
		}


		public int getColumnCount() { return names.length; }
		public int getRowCount() { return daten.size();}
		public Object getValueAt(int row, int col) {
			PayAccount ac = (PayAccount) daten.elementAt(row);
			switch(col){
				case 0: return new Boolean(ac.getAccountNumber()==Pay.create().getUsedAccount());
				case 1: return new Long(ac.getAccountNumber());
				//case 2: return ac.getValidFrom();
				case 2: return ac.getValidTo();
				case 3: return new Long(ac.getCredit());
			}
			return "fehler";
		}
		public PayAccount getRow(int row){
			return (PayAccount) daten.elementAt(row);
		}
		public PayAccount getAccount(int row){
			try{
				return (PayAccount) daten.elementAt(row);
			}catch(Exception ex){
				return Pay.create().getAccount(Pay.create().getUsedAccount());
			}
		}
		public long getAccountNumber(int row){
			PayAccount ac = (PayAccount) daten.elementAt(row);
			return ac.getAccountNumber();
		}

		public Class getColumnClass(int c) {return getValueAt(0, c).getClass();}

			public boolean isCellEditable(int row, int col) {
			if(getColumnName(col).equals(aktuell)) return true;
			else return false;
		}

		public String getColumnName(int c) {
			if(c<0||c>getColumnCount()) return "";
			else return names[c];
		}
		public JScrollPane getView(){
			JTable table = new JTable(this);
			table.setPreferredScrollableViewportSize(new Dimension(this.getColumnCount()*columnSize, getRowCount()*rowSize));
			return new JScrollPane(table);
		}
		/**
		* Wert setzen - nur die erste Spalte ist editierbar
		* in allen anderen Spalten passiert nichts
		**/
		public void setValueAt(Object ob ,int row, int col){
			LogHolder.log(LogLevel.DEBUG,LogType.PAY,"setValueAt: col: "+col+" getcolNam: --"+getColumnName(col)+"--  ");
			if(getColumnName(col).equals(aktuell)){
				Pay.create().setUsedAccount(getRow(row).getAccountNumber());
			}
		}
		public void modelUpdated(ModelEvent me){
			LogHolder.log(LogLevel.DEBUG,LogType.PAY,"modelUpdated");
			fireTableDataChanged();
		}
	}
