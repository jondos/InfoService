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
import anon.JAPAnonService;
/**
 * User Interface for an Mix-Cascade Monitor.
 * 
 * @author  Hannes Federrath
 */
class JAPCascadeMonitorView extends JDialog /*implements Runnable*/ {
	private JAPModel model;
	
	private JButton startButton;
	private JButton stopButton;
	private JLabel  statusTextField;
	private JCheckBox contCheckBox;
	private JScrollPane tableAggregate,scrollpane;
	JTable tableView;
	TableModel dataModel;
	
	private Vector db;
	private Thread t;
	private boolean runFlag = true;
		
	/**
	 * The Main Method
	 */
    public static void main(String[] args) {
 		AnonServerDBEntry[] initialServerList = new AnonServerDBEntry[2];
		AnonServerDBEntry myEntry0 = new AnonServerDBEntry("192.168.0.11:4453", "192.168.0.11", 4453);
		AnonServerDBEntry myEntry1 = new AnonServerDBEntry("192.168.0.2:4453", "192.168.0.2", 4453);
		initialServerList[0] = myEntry0;
		initialServerList[1] = myEntry1;
		new JAPRoundTripTimeView (null,initialServerList);
    }
	
//	JAPCascadeMonitorView (Frame parent,AnonServerDBEntry[] initialServerList) {
	JAPCascadeMonitorView (Frame parent, Vector initialServerList) {
 		super(parent,"Cascade Monitor");
		this.db = initialServerList;
		model=JAPModel.getModel();
		Component contents = this.createComponents();
		getContentPane().add(contents, BorderLayout.CENTER);
		pack();
		JAPUtil.upRightFrame(this);
		setVisible(true);
	}
	
	private Component createComponents() {
	    JPanel p = new JPanel(new BorderLayout());
		// status
		JPanel statusPanel = new JPanel();
		statusTextField = new JLabel(model.getString("chkPressStartToCheck"));
		contCheckBox = new JCheckBox(model.getString("chkChontinouslyCheck"));
		statusPanel.add(new Label("Status: "));
		statusPanel.add(statusTextField);
//		statusPanel.add(contCheckBox);
		// Table
		tableAggregate = createTable();
		// Buttons
		JPanel buttonPanel   = new JPanel();		
		startButton     = new JButton(model.getString("startButton"));
		stopButton      = new JButton(model.getString("stopButton"));
		stopButton.setEnabled(false);
		final JButton closeButton = new JButton(model.getString("closeButton"));
		startButton.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
				stopButton.setEnabled(true);
				startButton.setEnabled(false);
				startRequest();
		    }
		});
		stopButton.setEnabled(false);
		stopButton.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
				startButton.setEnabled(true);
				stopButton.setEnabled(false);
				stopRequest();
		    }
		});
		closeButton.addActionListener(new ActionListener() {
		    public void actionPerformed(ActionEvent e) {
				stopRequest();
		        dispose();
		    }
		});
		buttonPanel.add(startButton);
		buttonPanel.add(stopButton);
		buttonPanel.add(closeButton);
		// add components to main panel 
		p.add(statusPanel,BorderLayout.NORTH);
//	    p.add(new JScrollPane(ta),BorderLayout.CENTER);
	    p.add(tableAggregate,BorderLayout.CENTER);
		p.add(buttonPanel,BorderLayout.SOUTH);
	    return p;
	}
	
    public JScrollPane createTable() {

        // Create a model of the data.
        dataModel = new AbstractTableModel() {
            public int getColumnCount() { return 4; }
            public String getColumnName(int column) { 
				if (column==0) return model.getString("chkCascade");
				if (column==1) return model.getString("chkUsers");
				if (column==2) return model.getString("chkDelay");
				if (column==3) return model.getString("chkStatus");
				return " ";
			}
            public int getRowCount() { return db.size();}
            public Object getValueAt(int row, int col) {
				AnonServerDBEntry e = (AnonServerDBEntry)db.elementAt(row);
				if (col==0) return e.getName();
				if (col==1) return (e.getNrOfActiveUsers()==-1?"n/a":""+e.getNrOfActiveUsers());
				if (col==2) return (e.getDelay()==-1?"n/a":""+e.getDelay());
				if (col==3) return e.getStatus();
				return " ";
			}
         };


        // Create the table
        tableView = new JTable(dataModel);
		tableView.setRowHeight(15);
		//JAPUtil.setPerfectTableSize(tableView, new Dimension(550,300));
		tableView.setPreferredScrollableViewportSize(new Dimension(350,47));
        scrollpane = new JScrollPane(tableView);
		scrollpane.createVerticalScrollBar();
        return scrollpane;
    }
	
	
private final class CascadeMonitor implements Runnable {

	public void CascadeMonitor() {
	}
	
	public void run() {
		int nr = db.size();
		// connect to all Mix cascades (not yet working)
		for(int i=0;i<nr;i++) {
			AnonServerDBEntry e = (AnonServerDBEntry)db.elementAt(i);
			statusTextField.setText("Connecting to Cascade "+e.getName());
			e.setStatus("Connecting...");
			tableView.repaint();
			e.setAnonService(new JAPAnonService());
			JAPAnonService s = e.getAnonService();
			s.setAnonService(model.anonHostName,model.anonPortNumber);
			s.setProtocol(JAPAnonService.PROTO_HTTP);
			if (model.getUseProxy()) {
				s.setFirewall(model.getProxyHost(),model.getProxyPort());
				s.connectViaFirewall(true);
			}
			int ret=s.start();
			if(ret==JAPAnonService.E_SUCCESS)
				e.setStatus("Connected");
			if (ret==JAPAnonService.E_BIND) 
				e.setStatus("Bind error");
			else
				e.setStatus("No connection");
			tableView.repaint();
		}
		// get the feedback forever
		while(runFlag) {			
			statusTextField.setText("Getting feedback data ...");
			Enumeration enum = db.elements();
			while (enum.hasMoreElements()) {
				model.getInfoService().getFeedback((AnonServerDBEntry)enum.nextElement());
			}
			tableView.repaint();
			statusTextField.setText("Finished");
			try {
				t.sleep(30000);
			} catch (Exception e) {
			}
		}
	}
}

	
	private void startRequest() {
		t = new Thread(new CascadeMonitor());
		t.setPriority(Thread.MIN_PRIORITY);
		t.start();
	}
	
	private void stopRequest() {
		runFlag=false;
		try {
			t.join(3000);
		} catch (Exception e) {			
		}
		try {
			t.stop();
		} catch (Exception e) {			
		}
		int nr = db.size();
		for(int i=0;i<nr;i++) {
			AnonServerDBEntry e = (AnonServerDBEntry)db.elementAt(i);
			JAPAnonService s = e.getAnonService();
			s.stop();
		}
		statusTextField.setText("Cancelled");
	}
	

}
