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
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.util.Vector;
import java.util.Enumeration;
import javax.swing.table.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;

/**
 * User Interface for an Mix-Cascade Monitor.
 * 
 * @author  Hannes Federrath
 */
class JAPCascadeMonitorView extends JDialog implements ListSelectionListener {
	private JAPModel model;
//	private JAPCascadeMonitor cm = null;

	private JButton startButton;
	private JButton stopButton;
	private JButton okButton;
	private JLabel  statusTextField;
	private JCheckBox contCheckBox;
	private JScrollPane tableAggregate,scrollpane;
	private JTable tableView;
	private TableModel dataModel;
	private int selectedRow = -1;
	private Vector db = null;
	private Thread t;
	private boolean runFlag = true;
		
	JAPCascadeMonitorView (Frame parent) {
 		super(parent);
		model=JAPModel.getModel();
		this.db = model.anonServerDatabase;
		this.setTitle(model.getString("chkAvailableCascades"));
		Component contents = this.createComponents();
		getContentPane().add(contents, BorderLayout.CENTER);
		pack();
		JAPUtil.centerFrame(this);
		setVisible(true);
	}
	
	private Component createComponents() {
	    JPanel p = new JPanel(new BorderLayout());
		// status
		JPanel statusPanel = new JPanel();
		statusTextField = new JLabel(model.getString("chkPressStartToCheck"));
		contCheckBox = new JCheckBox(model.getString("chkChontinouslyCheck"));
//		statusPanel.add(new JLabel("Status: "));
		statusPanel.add(statusTextField);
//		statusPanel.add(contCheckBox);
		// Table
		tableAggregate = createTable();
		// Buttons
		JPanel buttonPanel   = new JPanel();		
		startButton     = new JButton(model.getString("chkBttnTest"));
		stopButton      = new JButton(model.getString("stopButton"));
		okButton        = new JButton(model.getString("chkBttnSelect"));
		stopButton.setEnabled(false);
		final JButton closeButton = new JButton(model.getString("cancelButton"));
		startButton.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
				stopButton.setEnabled(true);
				startButton.setEnabled(false);
				startTest();
		    }
		});
		stopButton.setEnabled(false);
		stopButton.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
				startButton.setEnabled(true);
				stopButton.setEnabled(false);
				stopTest();
		    }
		});
		closeButton.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
				stopTest();
		        dispose();
		    }
		});
		okButton.setEnabled(false);
		okButton.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
				model.setAnonServer((AnonServerDBEntry)db.elementAt(tableView.getSelectedRow())); 
				stopTest();
		        dispose();
		    }
		});
		buttonPanel.add(startButton);
		buttonPanel.add(stopButton);
		buttonPanel.add(closeButton);
		buttonPanel.add(okButton);
		// add components to main panel 
		p.add(statusPanel,BorderLayout.NORTH);
//	    p.add(new JScrollPane(ta),BorderLayout.CENTER);
	    p.add(tableAggregate,BorderLayout.CENTER);
		p.add(buttonPanel,BorderLayout.SOUTH);
	    return p;
	}
	
    private JScrollPane createTable() {
				
        // Create a model of the data.
        dataModel = new AbstractTableModel() {
            public int getColumnCount() { return 5; }
            public String getColumnName(int column) { 
				if (column==0) return model.getString("chkCascade");
				if (column==1) return model.getString("chkUsers");
				if (column==2) return model.getString("chkDelay");
				if (column==3) return model.getString("chkStatus");
				if (column==4) return model.getString("chkSelect");
				return " ";
			}
            public int getRowCount() { return db.size();}
            public Object getValueAt(int row, int col) {
				AnonServerDBEntry e = (AnonServerDBEntry)db.elementAt(row);
				if (col==0) return e.getName();
				if (col==1) return (e.getNrOfActiveUsers()==-1?"n/a":""+e.getNrOfActiveUsers());
				if (col==2) return (e.getDelay()==null?"n/a":""+e.getDelay());
				if (col==3) return (e.getStatus()==null?"                              ":""+e.getStatus());
//				if (col==4) return ((e.getHost().equals(model.anonHostName)&&(e.getPort()==model.anonPortNumber))?model.getString("chkSelected"):" ");
				if (col==4) return (e.equals(model.getAnonServer())?model.getString("chkSelected"):" ");
				return " ";
			}
            public Class getColumnClass(int c) {return getValueAt(0, c).getClass();}
            public boolean isCellEditable(int row, int col) {return false/*getColumnClass(col) == String.class*/;}
            public void setValueAt(Object aValue, int row, int column) { ; }
         };


        // Create the table
        tableView = new JTable(dataModel);
        scrollpane = new JScrollPane(tableView);
		scrollpane.createVerticalScrollBar();
		tableView.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		tableView.getSelectionModel().addListSelectionListener(this);
		JAPUtil.setPerfectTableSize(tableView, new Dimension(550,450));
		//tableView.setPreferredScrollableViewportSize(new Dimension(tableView.getSize().width,Math.min(db.size(),6)*(tableView.getRowHeight()+1)));
		tableView.setPreferredScrollableViewportSize(new Dimension(550,Math.min(db.size(),6)*(tableView.getRowHeight()+1)));
        return scrollpane;
    }
	
    public void valueChanged(ListSelectionEvent e) {
		JAPDebug.out(JAPDebug.DEBUG,JAPDebug.GUI,"JAPCascadeMonitorView:valuesChanged() selected row="+tableView.getSelectedRow());
		okButton.setEnabled(true);
    }
	
	

	
	private void startTest() {
		this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));/*
		cm = new JAPCascadeMonitor();
		t = new Thread(cm);
		t.setPriority(Thread.MIN_PRIORITY);
		t.start();*/
	}
	
	private void stopTest() {
		statusTextField.setText(model.getString("chkCancelled"));/*
		runFlag=false;
		try {
			t.join(3000);
		} catch (Exception e) {			
		}
		try {
			t.stop();
		} catch (Exception e) {			
		}*/
		this.setCursor(Cursor.getDefaultCursor());
		statusTextField.setText(model.getString("chkPressStartToCheck"));
	}
	

}
