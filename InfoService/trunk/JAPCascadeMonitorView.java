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

import java.net.*;
import java.io.*;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.URL;
import java.text.NumberFormat;

import HTTPClient.HTTPConnection;
import HTTPClient.HTTPResponse;

//import anon.JAPAnonService;

/**
 * User Interface for an Mix-Cascade Monitor.
 *
 * @author  Hannes Federrath
 */
class JAPCascadeMonitorView extends JDialog implements ListSelectionListener {
	private int IDLETIME = 90000;
	private JAPController controller;
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
	private JAPAnonServerDB db = null;
	private Thread idleThread, monitorThread;
	private boolean runFlag = true;

	JAPCascadeMonitorView (Frame parent) {
 		super(parent);
		controller=JAPController.getController();
		view=this;
		db = controller.anonServerDatabase;
		this.setModal(true);
		this.setTitle(JAPMessages.getString("chkAvailableCascades"));
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
		statusTextField = new JLabel(JAPMessages.getString("chkPressStartToCheck"));
		contCheckBox = new JCheckBox(JAPMessages.getString("chkChontinouslyCheck"));
//		statusPanel.add(new JLabel("Status: "));
		statusPanel.add(statusTextField);
//		statusPanel.add(contCheckBox);
		// Table
		tableAggregate = createTable();
		// Buttons
		JPanel buttonPanel   = new JPanel();
		startButton     = new JButton(JAPMessages.getString("chkBttnTest"));
		stopButton      = new JButton(JAPMessages.getString("stopButton"));
		okButton        = new JButton(JAPMessages.getString("chkBttnSelect"));
		stopButton.setEnabled(false);
		final JButton closeButton = new JButton(JAPMessages.getString("cancelButton"));
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
				controller.setAnonServer(db.getEntry(tableView.getSelectedRow()));
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

        // Create a controller of the data.
        dataModel = new AbstractTableModel() {
            public int getColumnCount() { return 5; }
            public String getColumnName(int column) {
				if (column==0) return JAPMessages.getString("chkCascade");
				if (column==1) return JAPMessages.getString("chkUsers");
				if (column==2) return JAPMessages.getString("chkDelay");
				if (column==3) return JAPMessages.getString("chkStatus");
				if (column==4) return JAPMessages.getString("chkSelect");
				return " ";
			}
            public int getRowCount() { return db.size();}
            public Object getValueAt(int row, int col) {
				AnonServerDBEntry e = db.getEntry(row);
				if (col==0) return e.getName();
				if (col==1) return (e.getNrOfActiveUsers()==-1?" ":Integer.toString(e.getNrOfActiveUsers()));
				if (col==2) return (e.getDelay()==null?" ":e.getDelay());
				if (col==3) return (e.getStatus()==null?"                              ":e.getStatus());
				if (col==4) return (e.equals(controller.getAnonServer())?JAPMessages.getString("chkSelected"):" ");
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
		JAPUtil.setPerfectTableSize(tableView, new Dimension(600,450));
		//tableView.setPreferredScrollableViewportSize(new Dimension(tableView.getSize().width,Math.min(db.size(),6)*(tableView.getRowHeight()+1)));
		//tableView.setPreferredScrollableViewportSize(new Dimension(550,Math.min(db.size(),6)*(tableView.getRowHeight()+1)));
        return scrollpane;
    }

    public void valueChanged(ListSelectionEvent e) {
		JAPDebug.out(JAPDebug.DEBUG,JAPDebug.GUI,"JAPCascadeMonitorView:valuesChanged() selected row="+tableView.getSelectedRow());
		okButton.setEnabled(true);
    }

	private void startTest() {
		this.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
		try {
			if (listener==null) listener = new ServerSocket(controller.getAnonServer().getPort()+1);
		}
		catch (Exception ex) {
			JAPDebug.out(JAPDebug.ERR,JAPDebug.NET,"JAPCascadeMonitor:Cannot establish listener on port "+
						 Integer.toString(controller.getAnonServer().getPort()+1));
		}
		if (cm == null)
			cm = new JAPCascadeMonitor();
		monitorThread = new Thread(cm);
		monitorThread.setPriority(Thread.MAX_PRIORITY);
		monitorThread.start();
	}

	private void stopTest() {
		statusTextField.setText(JAPMessages.getString("chkCancelled"));
		runFlag=false;
		try {
			idleThread.stop();
		} catch (Exception e) {
		}
		try {
			monitorThread.stop();
		} catch (Exception e) {
		}
//		monitorThread=null;
		try {
			listener.close();
		}
		catch (Exception e) {
			JAPDebug.out(JAPDebug.ERR,JAPDebug.NET,"JAPCascadeMonitor:Error closing listener on port "+
						 Integer.toString(controller.getAnonServer().getPort()+1));
		}
		this.setCursor(Cursor.getDefaultCursor());
		statusTextField.setText(JAPMessages.getString("chkPressStartToCheck"));
	}


	public static void main(String[] args) {
		JAPMessages.init();
		JAPController controller = JAPController.create();
		JAPDebug.create();
		controller.loadConfigFile();
		controller.fetchAnonServers();
		JAPDebug.setDebugType(JAPDebug.NET+JAPDebug.GUI+JAPDebug.THREAD+JAPDebug.MISC);
		JAPDebug.setDebugLevel(JAPDebug.DEBUG);
		new JAPCascadeMonitorView(null);
	}

	private final class JAPCascadeMonitorIdle implements Runnable {
		public void run() {
			boolean idle = true;
			long t1 = System.currentTimeMillis();
			while(idle) {
				long t2 = System.currentTimeMillis();
				long milisec = t2 - t1;
				if (milisec < IDLETIME) {
					statusTextField.setText(JAPMessages.getString("chkIdle")+" "+((IDLETIME-milisec)/1000)+" s");
					try {
						idleThread.sleep(1000);
					} catch (Exception e) {
						;
					}
				} else {
					idle = false;
				}
			}
		}
	}

	private final class JAPCascadeMonitor implements Runnable {

		public JAPCascadeMonitor() {
			JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"JAPCascadeMonitor:initializing...");
		}

		public synchronized void run() {
			JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"JAPCascadeMonitor:run()");
			while(runFlag) {
				// get the feedback from InfoSercive
				statusTextField.setText(JAPMessages.getString("chkGettingFeedback"));
				Enumeration enum = db.elements();
				while (enum.hasMoreElements()) {
					controller.getInfoService().getFeedback((AnonServerDBEntry)enum.nextElement());
					tableView.repaint();
				}
				statusTextField.setText(JAPMessages.getString("chkFeedbackReceived"));
				// connect to all Mix cascades
				int nr = db.size();
				if (listener==null) {
					nr = 0;
					statusTextField.setText(JAPMessages.getString("chkListenerError"));
				}
				for(int i=0;i<nr;i++) {
					AnonServerDBEntry e = db.getEntry(i);
					statusTextField.setText(JAPMessages.getString("chkCnctToCasc")+" "+e.getName());
					e.setStatus(JAPMessages.getString("chkConnecting"));
					tableView.repaint();
					// create the AnonService
					JAPAnonProxy proxyAnon=new JAPAnonProxy(listener);
					if (controller.getUseFirewall()) {
						// connect vi proxy to first mix (via ssl portnumber)
						if (e.getSSLPort() == -1) {
							proxyAnon.setAnonService(e.getHost(),e.getPort());
							proxyAnon.setFirewall(JAPModel.getFirewallHost(),JAPModel.getFirewallPort());
						} else {
							proxyAnon.setAnonService(e.getHost(),e.getSSLPort());
							proxyAnon.setFirewall(JAPModel.getFirewallHost(),JAPModel.getFirewallPort());
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
					NumberFormat nf = NumberFormat.getInstance();
					nf.setMaximumFractionDigits(3);
					e.setDelay("" + ((dtConnect==-1)?"-":nf.format((float)dtConnect/1000.F)) + " s");
					tableView.repaint();
					if(ret==JAPAnonProxy.E_SUCCESS) {
						e.setStatus(JAPMessages.getString("chkConnected"));
					} else {
						dtConnect=-1;
						if (ret==JAPAnonProxy.E_BIND)  e.setStatus(JAPMessages.getString("chkBindError"));
						else                             e.setStatus(JAPMessages.getString("chkConnectionError"));
					}
					tableView.repaint();
					// if sucessfull perform check
					if (ret==JAPAnonProxy.E_SUCCESS) {
						// send request via AnonService
						//
						try {
							String target="http://"+JAPModel.getInfoServiceHost()+":"+JAPModel.getInfoServicePort()+"/aktVersion";
							// simply get the current version number via the anon service
							URL url = new URL(target);

//							URL url = new URL("http://www.inf.tu-dresden.de/cgi-bin/cgiwrap/hf2/img.cgi/monitor");
//							HTTPConnection c = new HTTPConnection(url.getHost(),url.getPort());
//							c.setProxyServer(InetAddress.getLocalHost().getHostAddress(),controller.getAnonServer().getPort()+1);
//							c.setAllowUserInteraction(false);
//							c.setTimeout(8000);

							Socket socket = new Socket(InetAddress.getLocalHost().getHostAddress(),controller.getAnonServer().getPort()+1);
							socket.setSoTimeout(60000);
							BufferedWriter outputStream = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
							outputStream.write("GET "+target+" HTTP/1.0\r\n\r\n");
							outputStream.flush();
							JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"Sending request: "+target);
							DataInputStream inputStream = new DataInputStream(socket.getInputStream());

							t1 = System.currentTimeMillis();
//							HTTPResponse resp = c.Get(url.getFile());
							String response = JAPUtil.readLine(inputStream);
							JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"Response:>" + response + "<");
//							if (resp==null || resp.getStatusCode()!=200) {
							if ((response == null) || (response.length() == 0) || (response.indexOf("200")==-1)) {
								e.setStatus(JAPMessages.getString("chkBadResponse"));
								tableView.repaint();
								dtResponse=-1;
							} else {
								String nextLine = JAPUtil.readLine(inputStream);
								while (nextLine.length() != 0) {
									JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,">" + nextLine + "<");
									nextLine = JAPUtil.readLine(inputStream);
								}
								String data = JAPUtil.readLine(inputStream);
								JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"Data:>" + data + "<");
								t2 = System.currentTimeMillis();
								dtResponse = t2-t1;
								e.setStatus(JAPMessages.getString("chkCascResponding"));
								tableView.repaint();
//								byte[] buff=resp.getData();
//								String s=new String(buff).trim();
								String s=data.trim();
								if ( (s.charAt(2) == '.') && (s.charAt(5) == '.') ) {
									e.setStatus(JAPMessages.getString("chkOK"));
									tableView.repaint();
								}
							}
							socket.close();       socket = null;
							outputStream.close(); outputStream = null;
							inputStream.close();  inputStream  = null;
						}
						catch (Exception ex) {
							dtResponse = -1;
							e.setStatus(JAPMessages.getString("chkConButError"));
							tableView.repaint();
							JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"Exception: "+ex);
						}

					}
					e.setDelay("" + ((dtConnect==-1)?"-":nf.format((float)dtConnect/1000.F)) + "/" +
						((dtResponse==-1)?"-":nf.format((float)dtResponse/1000.F)) + " s");
					tableView.repaint();
					proxyAnon.stop();
				}
				// sleep for a while
				view.setCursor(Cursor.getDefaultCursor());
				JAPCascadeMonitorIdle idl = new JAPCascadeMonitorIdle();
				idleThread = new Thread(idl);
				idleThread.setPriority(Thread.MIN_PRIORITY);
				idleThread.start();
				try { idleThread.join(); } catch (Exception e) { ; }
				view.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
			}
		}


	}
}
