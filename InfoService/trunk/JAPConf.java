import java.util.Enumeration;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.border.*;

public final class JAPConf extends JDialog 
	{
	private JAPModel model;
		
	final static public int PORT_TAB = 0;
	final static public int HTTP_TAB = 1;
	final static public int INFO_TAB = 2;	
	final static public int ANON_TAB = 3;	
	final static public int MISC_TAB = 4;
		
	private JCheckBox guiChB;
	private JCheckBox netChB;
	private JCheckBox threadChB;
	private JCheckBox miscChB;
	private JAPJIntField debugLevelTextField;

	private JAPJIntField portnumberTextField;
	private JCheckBox	proxyCheckBox;
	private JAPJIntField proxyportnumberTextField;
	private JTextField   proxyhostTextField;
	private JCheckBox	autoConnectCheckBox;
	private JAPJIntField anonportnumberTextField;
	private JTextField   anonhostTextField;
	private JAPJIntField infoportnumberTextField;
	private JTextField   infohostTextField;
	private JRadioButton b1,b2,b3;
	private JButton fetchB;
	private JComboBox select;
	private JTabbedPane tabs;
	private JPanel portP, httpP, infoP, anonP, miscP;
	
	private JFrame  parent;
	
	public JAPConf (JFrame f)
			{
				super(f, JAPModel.getString("settingsDialog"), true);
				parent=f;
				model = JAPModel.getModel();
	
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
				pack();
//				setResizable(false);
				model.centerFrame(this);
			}

		protected JPanel buildportPanel()
			{
				portnumberTextField = new JAPJIntField();
				portnumberTextField.addActionListener(new ActionListener() {
						   public void actionPerformed(ActionEvent e) {
				   OKPressed();
				   }});
				JPanel p = new JPanel();
				p.setLayout( new BorderLayout() );
				p.setBorder( new TitledBorder(model.getString("settingsListenerBorder")) );
				JPanel p1 = new JPanel();
				p1.setLayout( new GridLayout(3,1) );
				p1.setBorder( new EmptyBorder(5,10,10,10) );
				p1.add(new JLabel(model.getString("settingsPort1")) );
				p1.add(new JLabel(model.getString("settingsPort2")) );
				p1.add(portnumberTextField);
				p.add(p1, BorderLayout.NORTH);
				return p;
			}

	protected JPanel buildhttpPanel()
			{
				proxyCheckBox = new JCheckBox(model.getString("settingsProxyCheckBox"));
				proxyhostTextField = new JTextField();
				proxyportnumberTextField = new JAPJIntField();
				proxyhostTextField.setEnabled(model.isProxyMode());
				proxyportnumberTextField.setEnabled(model.isProxyMode());
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
				// not yet implemented --> disable it
				proxyCheckBox.setEnabled(false);
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
				autoConnectCheckBox = new JCheckBox(model.getString("settingsautoConnectCheckBox"));
				anonhostTextField = new JTextField();
				anonportnumberTextField = new JAPJIntField();
				anonhostTextField.setEditable(false);
				anonportnumberTextField.setEditable(false);
				anonhostTextField.addActionListener(new ActionListener() {
									   public void actionPerformed(ActionEvent e) {
							   OKPressed();
							   }});
				anonportnumberTextField.addActionListener(new ActionListener() {
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
						Cursor c = getCursor();
						setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
						model.fetchAnonServers();
						model.notifyJAPObservers();
						b2.doClick();
						setCursor(c);
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
							anonhostTextField.setText( 
							   ((AnonServerDBEntry)model.anonServerDatabase.elementAt(select.getSelectedIndex()-1)).getHost()   );
							anonportnumberTextField.setText( String.valueOf(
							   ((AnonServerDBEntry)model.anonServerDatabase.elementAt(select.getSelectedIndex()-1)).getPort() ) );
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
				}});
				b2.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						JAPDebug.out(JAPDebug.DEBUG,JAPDebug.GUI,"JAPConf:b2 selected");
						fetchB.setEnabled(false);
						select.setEnabled(true);
						select.setPopupVisible(true);
						anonhostTextField.setEditable(false);
						anonportnumberTextField.setEditable(false);
									 
				}});
				b3.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						JAPDebug.out(JAPDebug.DEBUG,JAPDebug.GUI,"JAPConf:b3 selected");
						fetchB.setEnabled(false);
						select.setEnabled(false);
						anonhostTextField.setEditable(true);
						anonportnumberTextField.setEditable(true);
				}});

				// layout stuff

				// AnonServer settings
				JPanel p = new JPanel();
				p.setLayout( new BorderLayout() );
				p.setBorder( new TitledBorder(model.getString("settingsAnonBorder")) );
				JPanel p1 = new JPanel();
				p1.setLayout( new GridLayout(8,1) );
				p1.setBorder( new EmptyBorder(5,10,10,10) );
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
				// 3-8
				p1.add(b3);
				p1.add(new JLabel(model.getString("settingsAnonHost")));
				p1.add(anonhostTextField);
				p1.add(new JLabel(model.getString("settingsAnonPort")));
				p1.add(anonportnumberTextField);
				p1.add(autoConnectCheckBox);
				//
				p.add(p1, BorderLayout.NORTH);
				return p;
			}
		
		protected JPanel buildmiscPanel()
			{
				JPanel p=new JPanel();
				p.setLayout(new BorderLayout() );
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
				c=new JComboBox();
				c.addItem("Deutsch");
				c.addItem("English");
				// not yet implemented --> disable it
				c.setEnabled(false);
				p1.add(c);
				JPanel p2=new JPanel();
				p2.setLayout(new BorderLayout());
				p2.setBorder( new TitledBorder(model.getString("miscconfigBorder")) );
				p2.add(new JLabel(model.getString("noOptions")), BorderLayout.NORTH);

				// Panel for Debugging Option
				JPanel p3=new JPanel();
				p3.setLayout( new GridLayout(6,1));
				p3.setBorder( new TitledBorder("Debugging") );
				guiChB = new JCheckBox("GUI");
				netChB = new JCheckBox("NET");
				threadChB = new JCheckBox("THREAD");
				miscChB = new JCheckBox("MISC");
				debugLevelTextField = new JAPJIntField();
				debugLevelTextField.addActionListener(new ActionListener() {
									   public void actionPerformed(ActionEvent e) {
							   OKPressed();
							   }});
				p3.add(guiChB);
				p3.add(netChB);
				p3.add(threadChB);
				p3.add(miscChB);
				p3.add(new JLabel("Level ("+JAPDebug.DEBUG+".."+JAPDebug.EMERG+")"));
				p3.add(debugLevelTextField);
				
				p.add(p1, BorderLayout.NORTH);
				p.add(p2, BorderLayout.CENTER);
				p.add(p3, BorderLayout.WEST);
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
			int i;
			s=infohostTextField.getText().trim();
			if(s==null||s.equals(""))
				{
					showError(model.getString("inputInfoServiceHostNotNull"));
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
			if(!model.isPort(i))
				{
					showError(model.getString("inputInfoServicePortWrong"));
					return false;
				}
			return true;
		}
															
	protected void OKPressed() 
			{
				if(!checkValues())
					return;
				setVisible(false);
				model.setProxyMode(proxyCheckBox.isSelected());
				model.setPortNumber(Integer.parseInt(portnumberTextField.getText().trim()));
				model.proxyHostName = proxyhostTextField.getText().trim();
				model.proxyPortNumber = Integer.parseInt(proxyportnumberTextField.getText().trim());
				
				model.setInfoService(infohostTextField.getText().trim(),
														 Integer.parseInt(infoportnumberTextField.getText().trim()));
				model.anonHostName = anonhostTextField.getText().trim();
				model.anonPortNumber  = Integer.parseInt(anonportnumberTextField.getText().trim());
				model.autoConnect = autoConnectCheckBox.isSelected();
				JAPDebug.setDebugType(
					 (guiChB.isSelected()?JAPDebug.GUI:JAPDebug.NUL)+
					 (netChB.isSelected()?JAPDebug.NET:JAPDebug.NUL)+
					 (threadChB.isSelected()?JAPDebug.THREAD:JAPDebug.NUL)+
					 (miscChB.isSelected()?JAPDebug.MISC:JAPDebug.NUL)
					);
				JAPDebug.setDebugLevel(Integer.parseInt(debugLevelTextField.getText().trim()));
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
		guiChB.setSelected((((JAPDebug.getDebugType()&JAPDebug.GUI)!=0)?true:false));
		netChB.setSelected((((JAPDebug.getDebugType()&JAPDebug.NET)!=0)?true:false));
		threadChB.setSelected((((JAPDebug.getDebugType()&JAPDebug.THREAD)!=0)?true:false));
		miscChB.setSelected((((JAPDebug.getDebugType()&JAPDebug.MISC)!=0)?true:false));
		debugLevelTextField.setText(String.valueOf(JAPDebug.getDebugLevel()));
		// listener tab
		portnumberTextField.setText(String.valueOf(model.getPortNumber()));
		// http proxy tab
		proxyCheckBox.setSelected(model.isProxyMode());
		proxyhostTextField.setEnabled(proxyCheckBox.isSelected());
		proxyportnumberTextField.setEnabled(proxyCheckBox.isSelected());
		proxyhostTextField.setText(model.proxyHostName);
		proxyportnumberTextField.setText(String.valueOf(model.proxyPortNumber));
		// info tab
		infohostTextField.setText(model.getInfoServiceHost());
		infoportnumberTextField.setText(String.valueOf(model.getInfoServicePort()));
		// anon tab
		anonhostTextField.setText(model.anonHostName);
		anonportnumberTextField.setText(String.valueOf(model.anonPortNumber));
		select.setSelectedIndex(0);
		autoConnectCheckBox.setSelected(model.autoConnect);
		
	}
	

}

