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
import java.util.Enumeration;
import java.util.Dictionary;
import java.util.Hashtable;
import java.util.Locale;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.border.*;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;

final class JAPConf extends JDialog 
	{
	private JAPModel model;
		
	final static public int PORT_TAB = 0;
	final static public int HTTP_TAB = 1;
	final static public int INFO_TAB = 2;	
	final static public int ANON_TAB = 3;	
	final static public int MISC_TAB = 4;
		
	private JCheckBox    guiChB;
	private JCheckBox    netChB;
	private JCheckBox    threadChB;
	private JCheckBox    miscChB;
	private JCheckBox    checkboxShowDebugConsole;
	//private JAPJIntField debugLevelTextField;
	private JSlider      sliderDebugLevel;
	private JAPJIntField portnumberTextField;
	private JAPJIntField portnumberTextFieldSocks;
	private JCheckBox	 proxyCheckBox;
	private JCheckBox	 listenerCheckBoxSocks;
	private JCheckBox	 listenerCheckBoxIsLocal;
	private JAPJIntField proxyportnumberTextField;
	private JTextField   proxyhostTextField;
	private JCheckBox	 autoConnectCheckBox;
	private JCheckBox	 startupMinimizeCheckBox;
	private JAPJIntField anonportnumberTextField;
	private JAPJIntField anonsslportnumberTextField;
	private String       anonServerName,oldAnonServerName;
	private JTextField   anonhostTextField;
	private JAPJIntField infoportnumberTextField;
	private JTextField   infohostTextField;
	private JRadioButton b1,b2,b3;
	private JButton      fetchB;
	private JComboBox    select;
	private JComboBox    comboLanguage;
	private boolean			 bIgnoreComboLanguageEvents=false;
	private JTabbedPane  tabs;
	private JPanel       portP, httpP, infoP, anonP, miscP;
	
	private JFrame  parent;
	private JAPConf japconf;
	
	public JAPConf (JFrame f)
			{
				super(f);
				parent=f;
				model = JAPModel.getModel();
				this.setModal(true);
				this.setTitle(model.getString("settingsDialog"));
				japconf=this;
				JPanel container = new JPanel();
				container.setLayout( new BorderLayout() );
				tabs = new JTabbedPane();
				portP = buildportPanel();
				httpP = buildhttpPanel();
				infoP = buildinfoPanel();
				anonP = buildanonPanel();
				miscP = buildmiscPanel();
				tabs.addTab( model.getString("confListenerTab"), null, portP );
				tabs.addTab( model.getString("confProxyTab"), null, httpP );
				tabs.addTab( model.getString("confInfoTab"), null, infoP );
				tabs.addTab( model.getString("confAnonTab"), null, anonP );
				tabs.addTab( model.getString("confMiscTab"), null, miscP );

				JPanel buttonPanel = new JPanel();
				buttonPanel.setLayout ( new FlowLayout(FlowLayout.RIGHT) );
				JButton cancel = new JButton(model.getString("cancelButton"));
				cancel.addActionListener(new ActionListener() {
						   public void actionPerformed(ActionEvent e) {
				   CancelPressed();
				   }});
				buttonPanel.add( cancel );
				JButton ok = new JButton(model.getString("okButton"));
				ok.addActionListener(new ActionListener() {
						   public void actionPerformed(ActionEvent e) {
				   OKPressed();
				   }});
				buttonPanel.add( ok );
				buttonPanel.add(new JLabel("   "));
				getRootPane().setDefaultButton(ok);

				container.add(tabs, BorderLayout.CENTER);
				container.add(buttonPanel, BorderLayout.SOUTH);
//				container.add(new JLabel(new ImageIcon(model.JAPICONFN)), BorderLayout.WEST);
				getContentPane().add(container);
				updateValues();
				// largest tab to front
				tabs.setSelectedComponent(anonP);
				pack();
//				setResizable(false);
				JAPUtil.centerFrame(this);
			}

		protected JPanel buildportPanel()
			{
				JLabel portnumberLabel1 = new JLabel(model.getString("settingsPort1"));
				JLabel portnumberLabel2 = new JLabel(model.getString("settingsPort2"));
				portnumberTextField = new JAPJIntField();
				portnumberTextField.addActionListener(new ActionListener() {
						   public void actionPerformed(ActionEvent e) {
				   OKPressed();
				   }});
				listenerCheckBoxIsLocal = new JCheckBox(model.getString("settingsListenerCheckBox"));
				// set Font in listenerCheckBox in same color as in portnumberLabel1
				listenerCheckBoxIsLocal.setForeground(portnumberLabel1.getForeground());

				listenerCheckBoxSocks=new JCheckBox(model.getString("settingsListenerCheckBoxSOCKS"));
				listenerCheckBoxSocks.setForeground(portnumberLabel1.getForeground());
				listenerCheckBoxSocks.addChangeListener(new ChangeListener(){
					public void stateChanged(ChangeEvent e)
						{
							if(listenerCheckBoxSocks.isSelected())
								{
									portnumberTextFieldSocks.setEnabled(true);
								}
							else
								{
									portnumberTextFieldSocks.setEnabled(false);
								}
					}});
				portnumberTextFieldSocks = new JAPJIntField();
				portnumberTextFieldSocks.addActionListener(new ActionListener() {
						   public void actionPerformed(ActionEvent e) {
				   OKPressed();
				   }});
				JPanel p = new JPanel();
				p.setLayout( new BorderLayout() );
				p.setBorder( new TitledBorder(model.getString("settingsListenerBorder")) );
				JPanel p1 = new JPanel();
				GridBagLayout g=new GridBagLayout();
				p1.setLayout( g );
				p1.setBorder( new EmptyBorder(5,10,10,10) );
				GridBagConstraints c=new GridBagConstraints();
				c.anchor=c.NORTHWEST;					
				c.fill=GridBagConstraints.HORIZONTAL;
				c.gridwidth=1;
				c.gridx=0;
				c.gridy=0;
				c.weightx=1;
				c.weighty=0;
				Insets normInsets=new Insets(0,0,3,0);
				c.insets=normInsets;
				g.setConstraints(portnumberLabel1,c);
				p1.add(portnumberLabel1);
				c.gridy=1;
				g.setConstraints(portnumberLabel2,c);
				p1.add(portnumberLabel2);
				c.gridy=2;
				g.setConstraints(portnumberTextField,c);
				p1.add(portnumberTextField);
				JSeparator seperator=new JSeparator();
				c.gridy=3;
				c.insets=new Insets(10,0,0,0);
				g.setConstraints(seperator,c);
				p1.add(seperator);
				c.insets=normInsets;
				c.gridy=4;
				c.insets=new Insets(10,0,0,0);
				g.setConstraints(listenerCheckBoxSocks,c);
				p1.add(listenerCheckBoxSocks);
				c.gridy=5;
				g.setConstraints(portnumberTextFieldSocks,c);
				p1.add(portnumberTextFieldSocks);
				c.gridy=6;
				JSeparator seperator2=new JSeparator();
				c.insets=new Insets(10,0,0,0);
				g.setConstraints(seperator2,c);
				p1.add(seperator2);
				c.gridy=7;
				c.insets=new Insets(10,0,0,0);
				g.setConstraints(listenerCheckBoxIsLocal,c);
				p1.add(listenerCheckBoxIsLocal);
				p.add(p1, BorderLayout.NORTH);
				return p;
			}

	protected JPanel buildhttpPanel()
			{
				proxyCheckBox = new JCheckBox(model.getString("settingsProxyCheckBox"));
				proxyhostTextField = new JTextField();
				proxyportnumberTextField = new JAPJIntField();
				proxyhostTextField.setEnabled(model.getUseProxy());
				proxyportnumberTextField.setEnabled(model.getUseProxy());
				proxyCheckBox.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						proxyhostTextField.setEnabled(proxyCheckBox.isSelected());
						proxyportnumberTextField.setEnabled(proxyCheckBox.isSelected());
				}});
				proxyhostTextField.addActionListener(new ActionListener() {
						   public void actionPerformed(ActionEvent e) {
				   OKPressed();
				   }});
				proxyportnumberTextField.addActionListener(new ActionListener() {
						   public void actionPerformed(ActionEvent e) {
				   OKPressed();
				   }});
				JPanel p = new JPanel();
				p.setLayout( new BorderLayout() );
				p.setBorder( new TitledBorder(model.getString("settingsProxyBorder")) );
				JPanel p1 = new JPanel();
				p1.setLayout( new GridLayout(4,1) );
				p1.setBorder( new EmptyBorder(5,10,10,10) );
				p1.add(proxyCheckBox);
				p1.add(proxyhostTextField);
				JLabel proxyPortLabel = new JLabel(model.getString("settingsProxyPort"));
				// set Font in proxyCheckBox in same color as in proxyPortLabel
				proxyCheckBox.setForeground(proxyPortLabel.getForeground());
				p1.add(proxyPortLabel);
				p1.add(proxyportnumberTextField);
				p.add(p1, BorderLayout.NORTH);
				return p;
			}
	
	protected JPanel buildinfoPanel() {
				infohostTextField = new JTextField();
				infoportnumberTextField = new JAPJIntField();
				infohostTextField.addActionListener(new ActionListener() {
									   public void actionPerformed(ActionEvent e) {
							   OKPressed();
							   }});
				infoportnumberTextField.addActionListener(new ActionListener() {
									   public void actionPerformed(ActionEvent e) {
							   OKPressed();
							   }});
				// InfoServer settings
				JPanel p = new JPanel();
				p.setLayout( new BorderLayout() );
				p.setBorder( new TitledBorder(model.getString("settingsInfoBorder")) );
				JPanel p1 = new JPanel();
				p1.setLayout( new GridLayout(5,1) );
				p1.setBorder( new EmptyBorder(5,10,10,10) );
				// lines
				p1.add(new JLabel(model.getString("settingsInfoText")));
				//p1.add(new JLabel(" ")); //vertical spacer
				p1.add(new JLabel(model.getString("settingsInfoHost")));
				p1.add(infohostTextField);
				p1.add(new JLabel(model.getString("settingsInfoPort")));
				p1.add(infoportnumberTextField);
				//
				p.add(p1, BorderLayout.NORTH);
				return p;
	}

	protected JPanel buildanonPanel() 
			{
				startupMinimizeCheckBox=new JCheckBox(model.getString("settingsstartupMinimizeCheckBox"));
				autoConnectCheckBox = new JCheckBox(model.getString("settingsautoConnectCheckBox"));
				anonhostTextField = new JTextField();
				anonportnumberTextField = new JAPJIntField();
				anonsslportnumberTextField = new JAPJIntField();
				anonhostTextField.setEditable(false);
				anonportnumberTextField.setEditable(false);
				anonsslportnumberTextField.setEditable(false);
				anonhostTextField.addActionListener(new ActionListener() {
									   public void actionPerformed(ActionEvent e) {
							   OKPressed();
							   }});
				anonportnumberTextField.addActionListener(new ActionListener() {
									   public void actionPerformed(ActionEvent e) {
							   OKPressed();
							   }});
				anonsslportnumberTextField.addActionListener(new ActionListener() {
									   public void actionPerformed(ActionEvent e) {
							   OKPressed();
							   }});
				ButtonGroup bg = new ButtonGroup();
				b1 = new JRadioButton(model.getString("settingsAnonRadio1"), true);
				b2 = new JRadioButton(model.getString("settingsAnonRadio2"));
				b3 = new JRadioButton(model.getString("settingsAnonRadio3"));
				fetchB = new JButton(model.getString("settingsAnonFetch"));
				fetchB.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
					JAPDebug.out(JAPDebug.DEBUG,JAPDebug.GUI,"JAPConf:fetchB");
//						JOptionPane.showMessageDialog(null,model.getString("notYetImlmplemented"));
						// fetch available mix cascades from the Internet
						Cursor c=getCursor();
						setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
						model.fetchAnonServers();
						if (model.anonServerDatabase.size() == 0) {
							setCursor(c);
							JOptionPane.showMessageDialog(model.getView(),
											model.getString("settingsNoServersAvailable"),
											model.getString("settingsNoServersAvailableTitle"),
											JOptionPane.INFORMATION_MESSAGE);
						} else {
							// show a window containing all available cascades
							//JAPCascadeMonitorView v=new JAPCascadeMonitorView(model.getView());
							// ------ !!!!! die folgenden zwei zeilen auskommentieren, wenn JAPCascadeMonitorView
							// ------ !!!!! ordentlich geht!!!!
							updateValues();
							b2.doClick();
							
							setCursor(c);
						}
						// ------ !!!!! diese wieder aktivieren!
						//OKPressed();
				}});
				select = new JComboBox();
				// add elements to combobox
				select.addItem(model.getString("settingsAnonSelect"));
				Enumeration enum = model.anonServerDatabase.elements();
				while (enum.hasMoreElements())
					{
						select.addItem( ((AnonServerDBEntry)enum.nextElement()).getName() );
					}
					   
				select.setEnabled(false);
				select.addActionListener(new ActionListener() {
							public void actionPerformed(ActionEvent e) {
						JAPDebug.out(JAPDebug.DEBUG,JAPDebug.GUI,"JAPConf:Item " + select.getSelectedIndex() + " selected");
						if (select.getSelectedIndex() > 0) {
							anonServerName = ((AnonServerDBEntry)model.anonServerDatabase.elementAt(select.getSelectedIndex()-1)).getName();
							oldAnonServerName = anonServerName;
							anonhostTextField.setText( 
							   ((AnonServerDBEntry)model.anonServerDatabase.elementAt(select.getSelectedIndex()-1)).getHost()   );
							anonportnumberTextField.setText( String.valueOf(
							   ((AnonServerDBEntry)model.anonServerDatabase.elementAt(select.getSelectedIndex()-1)).getPort() ) );
							int i = ((AnonServerDBEntry)model.anonServerDatabase.elementAt(select.getSelectedIndex()-1)).getSSLPort();
							if (i == -1)
								anonsslportnumberTextField.setText("");
							else
								anonsslportnumberTextField.setText( String.valueOf(i) );
						}
				}});
				bg.add(b1);
				bg.add(b2);
				bg.add(b3);
				b1.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						JAPDebug.out(JAPDebug.DEBUG,JAPDebug.GUI,"JAPConf:b1 selected");
						fetchB.setEnabled(true);
						select.setEnabled(false);
						anonhostTextField.setEditable(false);
						anonportnumberTextField.setEditable(false);
						anonsslportnumberTextField.setEditable(false);
				}});
				b2.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						JAPDebug.out(JAPDebug.DEBUG,JAPDebug.GUI,"JAPConf:b2 selected");
						fetchB.setEnabled(false);
						select.setEnabled(true);
						select.setPopupVisible(true);
						anonhostTextField.setEditable(false);
						anonportnumberTextField.setEditable(false);
						anonsslportnumberTextField.setEditable(false);
									 
				}});
				b3.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						JAPDebug.out(JAPDebug.DEBUG,JAPDebug.GUI,"JAPConf:b3 selected");
						fetchB.setEnabled(false);
						select.setEnabled(false);
						anonhostTextField.setEditable(true);
						anonportnumberTextField.setEditable(true);
						anonsslportnumberTextField.setEditable(true);
						anonServerName = model.getString("manual");
				}});

				// layout stuff
				JPanel p=new JPanel();
				p.setLayout(new BorderLayout() );
				// Upper panel
				JPanel pp1 = new JPanel();
				pp1.setLayout( new BorderLayout() );
				pp1.setBorder( new TitledBorder(model.getString("settingsAnonBorder")) );
				// Lower panel
				JPanel pp2 = new JPanel();
				pp2.setLayout( new BorderLayout() );
				pp2.setBorder( new TitledBorder(model.getString("settingsAnonBorder2")) );
				// Upper panel content
				JPanel p1 = new JPanel();
				p1.setLayout( new GridLayout(2,1) );
				//p1.setBorder( new EmptyBorder(5,10,10,10) );
				// 1
				JPanel p11 = new JPanel();
				p11.setLayout(new BoxLayout(p11, BoxLayout.X_AXIS));
				p11.add(b1);
				p11.add(Box.createRigidArea(new Dimension(5,0)) );
				p11.add(Box.createHorizontalGlue() );
				p11.add(fetchB);
				p1.add(p11);
				// 2
				JPanel p12 = new JPanel();
				p12.setLayout(new BoxLayout(p12, BoxLayout.X_AXIS));
				p12.add(b2);
				p12.add(Box.createRigidArea(new Dimension(5,0)) );
				p12.add(Box.createHorizontalGlue() );
				p12.add(select);
				p1.add(p12);
				// Lower Panel content
				JPanel p2 = new JPanel();
				p2.setLayout( new GridLayout(8,1) );
				//p1.setBorder( new EmptyBorder(5,10,10,10) );
				//
				p2.add(b3);
				p2.add(new JLabel(model.getString("settingsAnonHost")));
				p2.add(anonhostTextField);
				p2.add(new JLabel(model.getString("settingsAnonPort")));
				p2.add(anonportnumberTextField);
				p2.add(new JLabel(model.getString("settingsAnonSSLPort")));
				p2.add(anonsslportnumberTextField);
				//
				p2.add(autoConnectCheckBox);
				//p2.add(startupMinimizeCheckBox);
				// Add contents to upper and lower panel
				pp1.add(p1);
				pp2.add(p2);
				// Add to main panel
				p.add(pp1, BorderLayout.NORTH);
				p.add(pp2, BorderLayout.CENTER);
				return p;
			}
		
		protected JPanel buildmiscPanel()
			{
				JPanel p=new JPanel();
				p.setLayout(new BorderLayout() );
				
				// Panel for Look and Feel Options
				JPanel p1=new JPanel();
				p1.setLayout(new GridLayout(2,2));
				p1.setBorder( new TitledBorder(model.getString("settingsLookAndFeelBorder")) );
				p1.add(new JLabel(model.getString("settingsLookAndFeel")));
				JComboBox c=new JComboBox();
				// not yet implemented, or doesn't work very well on my Mac --> disable it
				c.setEnabled(false);
				LookAndFeelInfo[] lf=UIManager.getInstalledLookAndFeels();	
				for(int i=0;i<lf.length;i++)
					{
						c.addItem(lf[i].getName());
					}
				c.addItemListener(new ItemListener(){
					public void itemStateChanged(ItemEvent e){
						if(e.getStateChange()==e.SELECTED)
							{
								try
									{
										UIManager.setLookAndFeel(UIManager.getInstalledLookAndFeels()[((JComboBox)e.getItemSelectable()).getSelectedIndex()].getClassName());
										SwingUtilities.updateComponentTreeUI(parent);
										SwingUtilities.updateComponentTreeUI(SwingUtilities.getRoot(((JComboBox)e.getItemSelectable())));
									}
								catch(Exception ie)
								{
								}
							}
					}});
				p1.add(c);
				p1.add(new JLabel(model.getString("settingsLanguage")));
				comboLanguage=new JComboBox();
				comboLanguage.addItem("Deutsch");
				comboLanguage.addItem("English");
				// yet implemented !! 
				//c.setEnabled(false);
				comboLanguage.addItemListener(new ItemListener(){
					public void itemStateChanged(ItemEvent e){
						if(!bIgnoreComboLanguageEvents&&e.getStateChange()==e.SELECTED)
							{
								try
									{
									
										JOptionPane.showMessageDialog(japconf,JAPMessages.getString("confLanguageChanged"),JAPMessages.getString("information"),JOptionPane.INFORMATION_MESSAGE);
									}
								catch(Exception ie)
								{
								}
							}
					}});

				p1.add(comboLanguage);
				
				// Panel for Misc Options
				JPanel p2=new JPanel();
				p2.setLayout(new BorderLayout());
				p2.setBorder( new TitledBorder(model.getString("miscconfigBorder")) );
				JButton bttnPing=new JButton(model.getString("bttnPing"));
				bttnPing.addActionListener(new ActionListener()
					{
						public void actionPerformed(ActionEvent e)
							{
								AnonServerDBEntry[] a=new AnonServerDBEntry[1];
//								a[0]=new AnonServerDBEntry(model.anonHostName,model.anonHostName,model.anonPortNumber+1);
								a[0]=new AnonServerDBEntry(model.getAnonServer().getHost(),model.getAnonServer().getHost(),model.getAnonServer().getPort()+1);
								JAPRoundTripTimeView v=new JAPRoundTripTimeView(model.getView(),a);
//								v.show();
							}
					});
				JButton bttnMonitor=new JButton(model.getString("bttnMonitor"));
//				bttnMonitor.setEnabled(false);
				bttnMonitor.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						Cursor c1=getCursor();
						setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
						model.fetchAnonServers();
						JAPCascadeMonitorView v=new JAPCascadeMonitorView(model.getView());
						updateValues();
						OKPressed();
						setCursor(c1);
						
				}});
				JPanel p22 = new JPanel();
				p22.setLayout(new GridLayout(2,1));
				p22.add(bttnPing);
				p22.add(bttnMonitor);
				p2.add(p22, BorderLayout.NORTH);

				// Panel for Debugging Options
				JPanel p3=new JPanel();
				p3.setLayout( new GridLayout(1,2));
				p3.setBorder( new TitledBorder("Debugging") );
				JPanel p31=new JPanel(new GridLayout(0,1));
				guiChB = new JCheckBox("GUI");
				netChB = new JCheckBox("NET");
				threadChB = new JCheckBox("THREAD");
				miscChB = new JCheckBox("MISC");
				p31.add(guiChB);
				p31.add(netChB);
				p31.add(threadChB);
				p31.add(miscChB);
				
				checkboxShowDebugConsole=new JCheckBox("Show Console");
				checkboxShowDebugConsole.setSelected(JAPDebug.isShowConsole());
				checkboxShowDebugConsole.addItemListener(new ItemListener()
					{public void itemStateChanged(ItemEvent e)
					 {
						 JAPDebug.showConsole(e.getStateChange()==e.SELECTED,model.getView());
					}});
				p31.add(checkboxShowDebugConsole);
				
				p3.add(p31);
				JPanel p32=new JPanel();
				sliderDebugLevel=new JSlider(JSlider.VERTICAL,0,7,0);
				sliderDebugLevel.addChangeListener(new ChangeListener()
					{public void stateChanged(ChangeEvent e)
					 {Dictionary d=sliderDebugLevel.getLabelTable();
						for(int i=0;i<8;i++)
							((JLabel)d.get(new Integer(i))).setEnabled(i<=sliderDebugLevel.getValue());
					}});
				String debugLevels[]=JAPDebug.getDebugLevels();
				Hashtable ht=new Hashtable(debugLevels.length,1.0f);
				for(int i=0;i<debugLevels.length;i++)
					{
						ht.put(new Integer(i),new JLabel(" "+debugLevels[i]));
					}
				sliderDebugLevel.setLabelTable(ht);
				sliderDebugLevel.setPaintLabels(true);
				sliderDebugLevel.setMajorTickSpacing(1);
				sliderDebugLevel.setMinorTickSpacing(1);
				sliderDebugLevel.setSnapToTicks(true);
				sliderDebugLevel.setPaintTrack(true);
				sliderDebugLevel.setPaintTicks(false);
				
				p32.add(sliderDebugLevel);
				p3.add(p32);

				JPanel pp = new JPanel( new BorderLayout() );
				pp.add(p1, BorderLayout.NORTH);
				pp.add(p2, BorderLayout.CENTER);

				p.add(p3, BorderLayout.WEST);
				p.add(pp, BorderLayout.CENTER);
				
				return p;
			}

	protected void CancelPressed()
			{
				setVisible(false);
		}
	
	/**Shows a Dialog about whats going wrong
	 */
	private void showError(String msg)
		{
			JOptionPane.showMessageDialog(this,msg,model.getString("ERROR"),JOptionPane.ERROR_MESSAGE);
		}

	/** Checks if all Input in all Fiels make sense. Dispaly InfoBoxes about what is wrong.
	 * @return true if all is ok
	 *					false otherwise
	 */
	private boolean checkValues()
		{
			String s=null;
			int iListenerPort,i;
			//Checking InfoService (Host + Port)
			s=infohostTextField.getText().trim();
			if(s==null||s.equals(""))
				{
					showError(model.getString("errorInfoServiceHostNotNull"));
					return false;
				}
			try
				{
					i=Integer.parseInt(infoportnumberTextField.getText().trim());
				}
			catch(Exception e)
				{
					i=-1;
				}
			if(!JAPUtil.isPort(i))
				{
					showError(model.getString("errorInfoServicePortWrong"));
					return false;
				}
			
			//Checking First Mix (Host + Port)
			s=anonhostTextField.getText().trim();
			if(s==null||s.equals(""))
				{
					showError(model.getString("errorAnonHostNotNull"));
					return false;
				}
			try
				{
					i=Integer.parseInt(anonportnumberTextField.getText().trim());
				}
			catch(Exception e)
				{
					i=-1;
				}
			if(!JAPUtil.isPort(i))
				{
					showError(model.getString("errorAnonServicePortWrong"));
					return false;
				}
			//--------------
			if (anonsslportnumberTextField.getText().trim().equals("")) {
				;
			} else {
				try {
					i=Integer.parseInt(anonsslportnumberTextField.getText().trim());
				} 
				catch(Exception e) {
					i=-1;
				}
				if(!JAPUtil.isPort(i)) {
					showError(model.getString("errorAnonServicePortWrong"));
					return false;
				}
			}
			//checking Listener Port Number
			try
				{
					i=Integer.parseInt(portnumberTextField.getText().trim());
				}
			catch(Exception e)
				{
					i=-1;
				}
			if(!JAPUtil.isPort(i))
				{
					showError(model.getString("errorListenerPortWrong"));
					return false;
				}
			iListenerPort=i;
			//checking Socks Port Number
			if(listenerCheckBoxSocks.isSelected())
				{
					try
						{
							i=Integer.parseInt(portnumberTextFieldSocks.getText().trim());
						}
					catch(Exception e)
						{
							i=-1;
						}
					if(!JAPUtil.isPort(i))
						{
							showError(model.getString("errorSocksListenerPortWrong"));
							return false;
						}
					if(i==iListenerPort)
						{
							showError(model.getString("errorListenerPortsAreEqual"));
							return false;
						}
				}
			//checking Debug-Level
	/*		try
				{
					i=Integer.parseInt(debugLevelTextField.getText().trim());
				}
			catch(Exception e)
				{
					i=-1;
				}
			if(i<0||i>JAPDebug.DEBUG)
				{
					showError(model.getString("errorDebugLevelWrong"));
					return false;
				}
		*/	
			
			return true;
		}
															
	protected void OKPressed() 
			{
				if(!checkValues())
					return;
				setVisible(false);
				model.setUseSocksPort(listenerCheckBoxSocks.isSelected());
				model.setSocksPortNumber(Integer.parseInt(portnumberTextFieldSocks.getText().trim()));
				model.setListenerIsLocal(listenerCheckBoxIsLocal.isSelected());
				model.setUseProxy(proxyCheckBox.isSelected());
				int newPort=Integer.parseInt(portnumberTextField.getText().trim());
				if(newPort!=model.getPortNumber())
					{
						JOptionPane.showMessageDialog(this,model.getString("confmessageListernPortChanged"));	
					}
				model.setPortNumber(Integer.parseInt(portnumberTextField.getText().trim()));
				model.setProxy(proxyhostTextField.getText().trim(),
					Integer.parseInt(proxyportnumberTextField.getText().trim()));
				// infoservice settings
				model.setInfoService(infohostTextField.getText().trim(),
					Integer.parseInt(infoportnumberTextField.getText().trim()));
				// anon settings
				int anonSSLPortNumber = -1; 
				if (!anonsslportnumberTextField.getText().equals(""))
					anonSSLPortNumber = Integer.parseInt(anonsslportnumberTextField.getText().trim());
				
				if (b3.isSelected())
						anonServerName = model.getString("manual");
				AnonServerDBEntry e = new AnonServerDBEntry(
															anonServerName,
															anonhostTextField.getText().trim(),
															Integer.parseInt(anonportnumberTextField.getText().trim()),
															anonSSLPortNumber);
				// if (the same server) (re)set the name from "manual" to the correct name
				if (model.getAnonServer().equals(e))
					e.setName(oldAnonServerName);
				model.autoConnect = autoConnectCheckBox.isSelected();
				model.setMinimizeOnStartup(startupMinimizeCheckBox.isSelected());
				JAPDebug.setDebugType(
					 (guiChB.isSelected()?JAPDebug.GUI:JAPDebug.NUL)+
					 (netChB.isSelected()?JAPDebug.NET:JAPDebug.NUL)+
					 (threadChB.isSelected()?JAPDebug.THREAD:JAPDebug.NUL)+
					 (miscChB.isSelected()?JAPDebug.MISC:JAPDebug.NUL)
					);
				JAPDebug.setDebugLevel(sliderDebugLevel.getValue());
				model.setAnonServer(e);
				// force setting the correct name of the selected server
				model.getAnonServer().setName(e.getName());
				// force notifying the observers set the right server name
				JAPDebug.out(JAPDebug.DEBUG,JAPDebug.GUI,"comboLanguage: "+Integer.toString(comboLanguage.getSelectedIndex()));
				if(comboLanguage.getSelectedIndex()==0)
					model.setLocale(Locale.GERMAN);
				else
					model.setLocale(Locale.ENGLISH);				
				model.notifyJAPObservers();
			}
	
	public void selectCard(int selectedCard)
			{	
				// set selected card to foreground
				if (selectedCard == HTTP_TAB)
					tabs.setSelectedComponent(httpP);
				else if (selectedCard == INFO_TAB)
					tabs.setSelectedComponent(infoP);
				else if (selectedCard == ANON_TAB)
					tabs.setSelectedComponent(anonP);
				else if (selectedCard == MISC_TAB)
					tabs.setSelectedComponent(miscP);
				else
					tabs.setSelectedComponent(portP);
			}

	public void updateValues() {
		// misc tab
		checkboxShowDebugConsole.setSelected(JAPDebug.isShowConsole());
		guiChB.setSelected((((JAPDebug.getDebugType()&JAPDebug.GUI)!=0)?true:false));
		netChB.setSelected((((JAPDebug.getDebugType()&JAPDebug.NET)!=0)?true:false));
		threadChB.setSelected((((JAPDebug.getDebugType()&JAPDebug.THREAD)!=0)?true:false));
		miscChB.setSelected((((JAPDebug.getDebugType()&JAPDebug.MISC)!=0)?true:false));
		sliderDebugLevel.setValue(JAPDebug.getDebugLevel());
		bIgnoreComboLanguageEvents=true;
		if(model.getLocale().equals(Locale.ENGLISH))
			comboLanguage.setSelectedIndex(1);
		else
			comboLanguage.setSelectedIndex(0);
		bIgnoreComboLanguageEvents=false;
		// listener tab
		portnumberTextField.setText(String.valueOf(model.getPortNumber()));
		listenerCheckBoxIsLocal.setSelected(model.getListenerIsLocal());
		portnumberTextFieldSocks.setText(String.valueOf(model.getSocksPortNumber()));
		listenerCheckBoxSocks.setSelected(model.getUseSocksPort());
		// http proxy tab
		proxyCheckBox.setSelected(model.getUseProxy());
		proxyhostTextField.setEnabled(proxyCheckBox.isSelected());
		proxyportnumberTextField.setEnabled(proxyCheckBox.isSelected());
		proxyhostTextField.setText(model.getProxyHost());
		proxyportnumberTextField.setText(String.valueOf(model.getProxyPort()));
		// info tab
		infohostTextField.setText(model.getInfoServiceHost());
		infoportnumberTextField.setText(String.valueOf(model.getInfoServicePort()));
		// anon tab
		AnonServerDBEntry e = model.getAnonServer();
		anonServerName = e.getName();
		oldAnonServerName = anonServerName;
		anonhostTextField.setText(e.getHost());
		anonportnumberTextField.setText(String.valueOf(e.getPort()));
		if (e.getSSLPort()==-1)
			anonsslportnumberTextField.setText("");
		else
			anonsslportnumberTextField.setText(String.valueOf(e.getSSLPort()));
		select.setSelectedIndex(0);
		autoConnectCheckBox.setSelected(model.autoConnect);
		startupMinimizeCheckBox.setSelected(model.getMinimizeOnStartup());
						select.removeAllItems();
						select.addItem(model.getString("settingsAnonSelect"));
						Enumeration enum = model.anonServerDatabase.elements();
						while (enum.hasMoreElements())
							{
								select.addItem( ((AnonServerDBEntry)enum.nextElement()).getName() );
							}
	}
	

}

