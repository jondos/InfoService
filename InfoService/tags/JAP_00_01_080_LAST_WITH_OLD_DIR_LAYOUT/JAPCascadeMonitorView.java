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
import javax.swing.event.*;
import java.awt.event.*;
import java.util.Vector;
import java.util.Enumeration;
import javax.swing.table.*;
import javax.swing.event.ListSelectionListener;
import javax.swing.event.ListSelectionEvent;

import java.net.*;
import java.io.*;
import java.net.InetAddress;
//import java.net.ServerSocket;
//import java.net.URL;
import java.text.NumberFormat;

//import HTTPClient.HTTPConnection;
//import HTTPClient.HTTPResponse;

import anon.AnonServer;
import anon.AnonChannel;
import anon.ErrorCodes;
import anon.server.AnonServiceImpl;
//import proxy.AnonProxy;
/**
 * User Interface for an Mix-Cascade Monitor.
 *
 * @author  Hannes Federrath
 */
class JAPCascadeMonitorView extends JDialog implements ListSelectionListener,Runnable {
	private final static int IDLETIME = 90000;
	private final static int STATUS_AVAILABLE = 12;
	private final static int STATUS_UNAVAILABLE = 13;
	private JAPController controller;
//	private JAPCascadeMonitorView view = null;

	private JButton startButton;
	private JButton stopButton;
	private JButton okButton;
	private JLabel  statusTextField;
	private JCheckBox contCheckBox;
	private JScrollPane tableAggregate,scrollpane;
	private JTable tableView;
	protected CascadeMonitorTableModel dataModel1;
	private int selectedRow = -1;
	//private JAPAnonServerDB db = null;
	private Thread monitorThread;
	private volatile boolean m_bTestIsRunning = false;

	JAPCascadeMonitorView (Frame parent) {
 		super(parent);
		controller=JAPController.getController();
	//	view=this;
		//db = controller.getAnonServerDB();
		setModal(true);
		setTitle(JAPMessages.getString("chkAvailableCascades"));
		Component contents = createComponents();
		getContentPane().add(contents, BorderLayout.CENTER);
		pack();
		JAPUtil.centerFrame(this);
		setVisible(true);
    m_bTestIsRunning=false;
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
				controller.setAnonServer(dataModel1.getSelectedAnonServer());
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

  private final class CascadeMonitorTableModel extends AbstractTableModel
    {
      private AnonServer[] m_arAnonServers;
      private String[] m_arStatus;
      public CascadeMonitorTableModel()
        {
          m_arAnonServers=null;
          m_arStatus=null;
        }

      public int getColumnCount() { return 5; }

      public String getColumnName(int column) {
				if (column==0) return JAPMessages.getString("chkCascade");
				if (column==1) return JAPMessages.getString("chkUsers");
				if (column==2) return JAPMessages.getString("chkDelay");
				if (column==3) return JAPMessages.getString("chkStatus");
				if (column==4) return JAPMessages.getString("chkSelect");
				return " ";
			}

      public int getRowCount()
        {
          if(m_arAnonServers!=null)
            return m_arAnonServers.length;
          return 5;
        }

      public Object getValueAt(int row, int col)
        {
          if(m_arAnonServers==null)
            {
              switch(col)
                {
                  case 0:
                    return "                                                           ";
                  case 1:
                  case 2:
                  case 4:
                   return " ";
                  case 3: return "                                                         ";
                }
              return " ";
            }
				  AnonServer e = m_arAnonServers[row];
				  if (col==0) return e.getName();
				  if (col==1) return (e.getNrOfActiveUsers()==-1?"        ":Integer.toString(e.getNrOfActiveUsers()));
				  if (col==2) return (e.getDelay()==null?"               ":e.getDelay());
				  if (col==3)
          {
            String status=m_arStatus[row];
            if(status==null)
              return "                              ";
            if(status.equals("GREEN"))
              return JAPUtil.loadImageIcon("green.gif",true);
            else if(status.equals("RED"))
              return JAPUtil.loadImageIcon("red.gif",false);
             return status;
     		  }
        if (col==4) return (e.equals(controller.getAnonServer())?JAPMessages.getString("chkSelected"):" ");
				return " ";
			}
           public Class getColumnClass(int c) {return String.class;}
            public boolean isCellEditable(int row, int col) {return false;}
            public void setValueAt(Object aValue, int row, int column) {}

        protected void setStatus(int row,String status)
          {
            m_arStatus[row]=status;
          }

        protected void setAnonServers(AnonServer[] anonservers)
          {
            m_arAnonServers=anonservers;
            m_arStatus=new String[m_arAnonServers.length];
          }

        protected AnonServer getSelectedAnonServer()
          {
            return m_arAnonServers[tableView.getSelectedRow()];
          }
  }

    private JScrollPane createTable() {

        // Create a controller of the data.
        dataModel1 = new CascadeMonitorTableModel();
        // Create the table
        tableView = new JTable(dataModel1)
          {
            public TableCellRenderer getCellRenderer(int row, int column) {
	            if (column == 3) {
	              return super.getDefaultRenderer(dataModel1.getValueAt(row,column).getClass());
                }
	          return super.getCellRenderer(row, column);
          }

          };
        JAPUtil.setPerfectTableSize(tableView, new Dimension(600,450));
	      scrollpane = new JScrollPane(tableView);
		    scrollpane.createVerticalScrollBar();
		    tableView.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
		    tableView.getSelectionModel().addListSelectionListener(this);
	//	    JAPUtil.setPerfectTableSize(tableView, new Dimension(600,450));
		//tableView.setPreferredScrollableViewportSize(new Dimension(tableView.getSize().width,Math.min(db.size(),6)*(tableView.getRowHeight()+1)));
		//tableView.setPreferredScrollableViewportSize(new Dimension(550,Math.min(db.size(),6)*(tableView.getRowHeight()+1)));
        return scrollpane;
    }

    public void valueChanged(ListSelectionEvent e) {
		JAPDebug.out(JAPDebug.DEBUG,JAPDebug.GUI,"JAPCascadeMonitorView:valuesChanged() selected row="+tableView.getSelectedRow());
    okButton.setEnabled(true);
    }

	private synchronized void startTest()
    {
      if(m_bTestIsRunning)
        return;
		  m_bTestIsRunning=true;
      monitorThread = new Thread(this);
		  monitorThread.setPriority(Thread.MAX_PRIORITY);
		  monitorThread.start();
	  }

	private synchronized void stopTest()
    {
       if(!m_bTestIsRunning)
          return;
        m_bTestIsRunning=false;
        statusTextField.setText(JAPMessages.getString("chkCancelled"));
        try
          {
            monitorThread.join();
          } catch (Exception e) {}
      setCursor(Cursor.getDefaultCursor());
		  statusTextField.setText(JAPMessages.getString("chkPressStartToCheck"));
	  }


  private void setStatus(int row,String status)
    {
      dataModel1.setStatus(row,status);
    }

	public static void main(String[] args) {
		JAPMessages.init();
		JAPController controller = JAPController.create();
		JAPDebug.create();
		controller.loadConfigFile(null);
		controller.fetchAnonServers();
		JAPDebug.setDebugType(JAPDebug.NET+JAPDebug.GUI+JAPDebug.THREAD+JAPDebug.MISC);
		JAPDebug.setDebugLevel(JAPDebug.DEBUG);
		new JAPCascadeMonitorView(null);
	}

  public void run()
    {
		  JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"JAPCascadeMonitor:run()");
			while(m_bTestIsRunning)
        {
				  setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
          // get the feedback from InfoSercive
				  statusTextField.setText(JAPMessages.getString("chkAvailableAnonServers"));
				  AnonServer[] anonservers=null;
          try
            {
              anonservers=controller.getInfoService().getAvailableAnonServers();
            }
          catch(Exception e){}
		      dataModel1.setAnonServers(anonservers);
          tableView.repaint();
				  statusTextField.setText(JAPMessages.getString("chkGettingFeedback"));
          for(int i=0;i<anonservers.length;i++)
            {
					    controller.getInfoService().getFeedback(anonservers[i]);
					    tableView.repaint();
				    }
				  statusTextField.setText(JAPMessages.getString("chkFeedbackReceived"));
				  // create local AnonService directly!
	        AnonServiceImpl anonService=(AnonServiceImpl)AnonServiceImpl.create();
          if (JAPModel.getUseFirewall())
            {
              anonService.setFirewall(JAPModel.getFirewallType(),JAPModel.getFirewallHost(),JAPModel.getFirewallPort());
					  }

				  for(int i=0;i<anonservers.length&&m_bTestIsRunning;i++)
            {
					    AnonServer e = anonservers[i];
					    statusTextField.setText(JAPMessages.getString("chkCnctToCasc")+" "+e.getName());
					    setStatus(i,JAPMessages.getString("chkConnecting"));
					    tableView.repaint();

              // start the AnonService
					    long dtConnect  = 0;
					    long dtResponse = 0;
					    long t1 = 0;
					    long t2 = 0;
					    t1 = System.currentTimeMillis();
					    int ret=anonService.connect(e);
					    t2 = System.currentTimeMillis();
					    dtConnect = t2-t1;
					    NumberFormat nf = NumberFormat.getInstance();
					    nf.setMaximumFractionDigits(3);
              e.setDelay("" + ((dtConnect==-1)?"-":nf.format((float)dtConnect/1000.F)) + " s");
					    if(ret==ErrorCodes.E_SUCCESS)
                setStatus(i,JAPMessages.getString("chkConnected"));
					    else
                {
						      dtConnect=-1;
							    setStatus(i,JAPMessages.getString("chkConnectionError"));
					      }
					    tableView.repaint();

              // if sucessfull perform check
					    if (ret==ErrorCodes.E_SUCCESS&&m_bTestIsRunning)
                {
						      // send request via AnonService
						      try
                    {
							        // simply get the current version number via the anon service from the InfoService
							        String target="http://"+JAPModel.getInfoServiceHost()+":"+JAPModel.getInfoServicePort()+"/aktVersion";
							        AnonChannel c=anonService.createChannel(AnonChannel.HTTP);
							        BufferedWriter outputStream = new BufferedWriter(new OutputStreamWriter(c.getOutputStream()));
							        DataInputStream inputStream = new DataInputStream(c.getInputStream());
						          JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"Sending request: "+target);
							        t1 = System.currentTimeMillis();
							        outputStream.write("GET "+target+" HTTP/1.0\r\n\r\n");
							        outputStream.flush();
							        String response = JAPUtil.readLine(inputStream);
							        JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"Response:>" + response + "<");
                      if ((response == null) || (response.length() == 0) || (response.indexOf("200")==-1))
                        {
								          setStatus(i,JAPMessages.getString("chkBadResponse"));
								          tableView.repaint();
								          dtResponse=-1;
							          }
                      else
                        {
								          String nextLine = JAPUtil.readLine(inputStream);
								          while (nextLine.length() != 0) {
									          JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,">" + nextLine + "<");
									          nextLine = JAPUtil.readLine(inputStream);
								          }
                          String data = JAPUtil.readLine(inputStream);
                          JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"Data:>" + data + "<");
                          t2 = System.currentTimeMillis();
                          dtResponse = t2-t1;
                          setStatus(i,JAPMessages.getString("chkCascResponding"));
                          tableView.repaint();
                          String s=data.trim();
                          if ( (s.charAt(2) == '.') && (s.charAt(5) == '.') )
                            {
                              setStatus(i,"GREEN");
                              tableView.tableChanged(new TableModelEvent(dataModel1));
                              tableView.repaint();
								            }
							          }
							        outputStream.close(); outputStream = null;
							        inputStream.close();  inputStream  = null;
                      c.close();
						        }
						      catch (Exception ex)
                    {
							        dtResponse = -1;
							        setStatus(i,"RED");
							        tableView.repaint();
							        JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"Exception: "+ex);
						        }
                }//if connect was succesful
              e.setDelay("" + ((dtConnect==-1)?"-":nf.format((float)dtConnect/1000.F)) + "/" +
						            ((dtResponse==-1)?"-":nf.format((float)dtResponse/1000.F)) + " s");
					    tableView.repaint();
					    anonService.disconnect();
            }//for all Cascades
          // sleep for a while
				  setCursor(Cursor.getDefaultCursor());
			    long t1 = System.currentTimeMillis();
			    while(m_bTestIsRunning)
            {
				      long t2 = System.currentTimeMillis();
				      long milisec = t2 - t1;
				      if (milisec < IDLETIME)
                {
					        statusTextField.setText(JAPMessages.getString("chkIdle")+" "+((IDLETIME-milisec)/1000)+" s");
					        try{Thread.sleep(1000);} catch (Exception e) {}
                }
              else
					      break;
            }
        }//while test loop
    }//run()
}
