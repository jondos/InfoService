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

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URL;
import java.text.NumberFormat;

import HTTPClient.HTTPConnection;
import HTTPClient.HTTPResponse;

import anon.JAPAnonService;

/**
 * User Interface for an Mix-Cascade Monitor.
 * 
 * @author  Hannes Federrath
 */
class JAPCascadeMonitorView extends JDialog implements ListSelectionListener {
	private JAPModel model;
	private JAPCascadeMonitor cm = null;
	private ServerSocket listener=null;
	private JAPCascadeMonitorView view = null;

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
		view=this;
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
//				startButton.setEnabled(true);
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
//				model.notityJAPObservers();
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
				if (col==1) return (e.getNrOfActiveUsers()==-1?" ":Integer.toString(e.getNrOfActiveUsers()));
				if (col==2) return (e.getDelay()==null?" ":e.getDelay());
				if (col==3) return (e.getStatus()==null?"                              ":e.getStatus());
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
	
private final class JAPCascadeMonitor implements Runnable {
	private int IDLETIME = 90000;

	public JAPCascadeMonitor() {
		JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"JAPCascadeMonitor:initializing...");
	}
	
	public synchronized void run() {
		JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"JAPCascadeMonitor:run()");
		while(runFlag) {
			// get the feedback from InfoSercive
			statusTextField.setText(model.getString("chkGettingFeedback"));
			Enumeration enum = db.elements();
			while (enum.hasMoreElements()) {
				model.getInfoService().getFeedback((AnonServerDBEntry)enum.nextElement());
				tableView.repaint();
			}
			statusTextField.setText(model.getString("chkFeedbackReceived"));
			// connect to all Mix cascades
			int nr = db.size();
			if (listener==null) {
				nr = 0;
				statusTextField.setText(model.getString("chkListenerError"));
			}
			try {
				t.sleep(2000);
			} catch (Exception e) {
			}			
			for(int i=0;i<nr;i++) {
				AnonServerDBEntry e = (AnonServerDBEntry)db.elementAt(i);
				statusTextField.setText(model.getString("chkCnctToCasc")+" "+e.getName());
				e.setStatus(model.getString("chkConnecting"));
				tableView.repaint();
				// create the AnonService
				JAPAnonService proxyAnon=new JAPAnonService(listener,JAPAnonService.PROTO_HTTP);
				if (model.getUseProxy()) {
					// connect vi proxy to first mix (via ssl portnumber)
					if (e.getSSLPort() == -1) {
						proxyAnon.setAnonService(e.getHost(),e.getPort());
						proxyAnon.setFirewall(model.getProxyHost(),model.getProxyPort());
						proxyAnon.connectViaFirewall(true);
					} else {
						proxyAnon.setAnonService(e.getHost(),e.getSSLPort());
						proxyAnon.setFirewall(model.getProxyHost(),model.getProxyPort());
						proxyAnon.connectViaFirewall(true);
					}
				} else {
					// connect directly to first mix
					proxyAnon.setAnonService(e.getHost(),e.getPort());
				}
				// start the AnonService
				long dtConnect  = 0;
				long dtResponse = 0;
				long t1 = 0;
				long t2 = 0;
				t1 = System.currentTimeMillis();
				int ret=proxyAnon.start();
				t2 = System.currentTimeMillis();
				dtConnect = t2-t1;
				JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"CascadeMonitor:"+t1);
				JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"CascadeMonitor:"+t2);
				JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"CascadeMonitor:"+dtConnect);
				if(ret==JAPAnonService.E_SUCCESS) {
					e.setStatus(model.getString("chkConnected"));
				} else {
					dtConnect=-1;
					if (ret==JAPAnonService.E_BIND)  e.setStatus(model.getString("chkBindError"));		
					else                             e.setStatus(model.getString("chkConnectionError"));
				}				
				tableView.repaint();
				// if sucessfull perform check
				if (ret==JAPAnonService.E_SUCCESS) {					
					// send request via AnonService
					//
					try {						
						URL url = new URL("http://"+model.getInfoServiceHost()+":"+model.getInfoServicePort()+model.aktJAPVersionFN); 
//						URL url = new URL("http://www.inf.tu-dresden.de/cgi-bin/cgiwrap/hf2/img.cgi/monitor"); 
						HTTPConnection c = new HTTPConnection(url.getHost(),url.getPort());
						c.setProxyServer(InetAddress.getLocalHost().getHostAddress(),model.getPortNumber()+1);
						c.setAllowUserInteraction(false);
						//c.setTimeout(16000);
						t1 = System.currentTimeMillis();
						HTTPResponse resp = c.Get(url.getFile());
						t2 = System.currentTimeMillis();
						dtResponse = t2-t1;
				JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"CascadeMonitor:"+t1);
				JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"CascadeMonitor:"+t2);
				JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"CascadeMonitor:"+dtResponse);
						if (resp==null || resp.getStatusCode()!=200) {
							e.setStatus(model.getString("chkBadResponse"));
						} else {
							e.setStatus(model.getString("chkCascResponding"));
						}
						tableView.repaint();
						byte[] buff=resp.getData();
						String s=new String(buff).trim();
						if ( (s.charAt(2) == '.') && (s.charAt(5) == '.') )
							e.setStatus(model.getString("chkOK"));
						tableView.repaint();
					}
					catch (Exception ex) {
						dtResponse = -1;
						e.setStatus(model.getString("chkConButError"));
						tableView.repaint();
					}					
				}
				NumberFormat nf = NumberFormat.getInstance();
				nf.setMaximumFractionDigits(3);
				e.setDelay("" + ((dtConnect==-1)?"-":nf.format((float)dtConnect/1000.F)) + "/" + 
					((dtResponse==-1)?"-":nf.format((float)dtResponse/1000.F)) + " s");
				tableView.repaint();
				ret = proxyAnon.stop();
			}
			statusTextField.setText(model.getString("chkIdle")+" "+IDLETIME/1000+" s");
//			view.setCursor(Cursor.getDefaultCursor());
			// sleep for a while
			view.setCursor(Cursor.getDefaultCursor());
			try {
				t.sleep(IDLETIME);
			} catch (Exception e) {
			}
			view.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		}
	}
}
	private void startTest() {
		this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		try {
			if (listener==null) listener = new ServerSocket(model.getAnonServer().getPort()+1);
		}
		catch (Exception ex) {
			JAPDebug.out(JAPDebug.ERR,JAPDebug.NET,"JAPCascadeMonitor:Cannot establish listener on port "+
						 Integer.toString(model.getAnonServer().getPort()+1));
		}
		if (cm == null)
			cm = new JAPCascadeMonitor();
		t = new Thread(cm);
		t.setPriority(Thread.MIN_PRIORITY);
		t.start();
	}
	
	private void stopTest() {
		statusTextField.setText(model.getString("chkCancelled"));
		runFlag=false;
		try {
			listener.close();
		}
		catch (Exception e) {
			JAPDebug.out(JAPDebug.ERR,JAPDebug.NET,"JAPCascadeMonitor:Error closing listener on port "+
						 Integer.toString(model.getAnonServer().getPort()+1));
		}
		try {
			t.join(3000);
		} catch (Exception e) {			
		}
		try {
			t.stop();
		} catch (Exception e) {			
		}
		t=null;
		this.setCursor(Cursor.getDefaultCursor());
		statusTextField.setText(model.getString("chkPressStartToCheck"));
	}
	


}
