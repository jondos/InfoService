import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.border.*;

public class JAPConf extends JDialog 
	{
    private JAPModel model;
    
    final static public int PORT_TAB = 0;
    final static public int HTTP_TAB = 1;
    final static public int ANON_TAB = 2;    
    final static public int MISC_TAB = 3;
		
    private JAPJIntField portnumberTextField;
    private JCheckBox proxyCheckBox;
    private JAPJIntField proxyportnumberTextField;
    private JTextField proxyhostTextField;
    private JAPJIntField anonportnumberTextField;
    private JTextField anonhostTextField;
		private JRadioButton b1,b2,b3;
		private JButton fetchB;
		private JComboBox select;

		private JTabbedPane tabs;
    private JPanel portP, httpP, anonP, miscP;
	
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
				anonP = buildanonPanel();
				miscP =buildmiscPanel();
				tabs.addTab( model.getString("confListenerTab"), null, portP );
				tabs.addTab( model.getString("confProxyTab"), null, httpP );
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
				container.add(new JLabel(new ImageIcon(model.JAPICONFN)), BorderLayout.WEST);
				getContentPane().add(container);
				pack();
				centerDialog();
			}

		protected JPanel buildportPanel()
			{
				portnumberTextField = new JAPJIntField(String.valueOf(model.portNumber));
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
				proxyCheckBox.setSelected(model.proxyMode);
				proxyhostTextField = new JTextField(model.proxyHostName);
				proxyportnumberTextField = new JAPJIntField(String.valueOf(model.proxyPortNumber));
				proxyhostTextField.setEnabled(model.proxyMode);
				proxyportnumberTextField.setEnabled(model.proxyMode);
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
    
    protected JPanel buildanonPanel() 
			{
				anonhostTextField = new JTextField(model.anonHostName);
				anonportnumberTextField = new JAPJIntField(String.valueOf(model.anonPortNumber));
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
						if (model.debug) System.out.println("fetchB");
				        JOptionPane.showMessageDialog(null,model.getString("notYetImlmplemented"));
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
						if (model.debug) System.out.println("Item " + select.getSelectedIndex() + " selected");
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
						if (model.debug) System.out.println("b1 selected");
						fetchB.setEnabled(true);
						select.setEnabled(false);
						anonhostTextField.setEditable(false);
						anonportnumberTextField.setEditable(false);
				}});
				b2.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						if (model.debug) System.out.println("b2 selected");
						fetchB.setEnabled(false);
						select.setEnabled(true);
						anonhostTextField.setEditable(false);
						anonportnumberTextField.setEditable(false);
									 
				}});
				b3.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent e) {
						if (model.debug) System.out.println("b3 selected");
						fetchB.setEnabled(false);
						select.setEnabled(false);
						anonhostTextField.setEditable(true);
						anonportnumberTextField.setEditable(true);
				}});

				// layout stuff
				JPanel p = new JPanel();
				p.setLayout( new BorderLayout() );
				p.setBorder( new TitledBorder(model.getString("settingsAnonBorder")) );
				JPanel p1 = new JPanel();
				p1.setLayout( new GridLayout(7,1) );
				p1.setBorder( new EmptyBorder(5,10,10,10) );
	
				// start lines
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
				// 3-7
				p1.add(b3);
				p1.add(new JLabel(model.getString("settingsAnonHost")));
				p1.add(anonhostTextField);
				p1.add(new JLabel(model.getString("settingsAnonPort")));
				p1.add(anonportnumberTextField);
				// end lines
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
				p1.add(c);
				JPanel p2=new JPanel();
				p2.setLayout(new BorderLayout());
				p2.setBorder( new TitledBorder(model.getString("miscconfigBorder")) );
				p2.add(new JLabel("Much space for additional settings..."), BorderLayout.NORTH);
				p.add(p1, BorderLayout.NORTH);
				p.add(p2, BorderLayout.CENTER);
				return p;
			}

		protected void centerDialog() 
			{
				Dimension screenSize = getToolkit().getScreenSize();
				Dimension ownSize = getSize();
				setLocation((screenSize.width  - ownSize.width )/2,
										(screenSize.height - ownSize.height)/2);
			}
	
    protected void CancelPressed()
			{
				setVisible(false);
			}

    protected void OKPressed() 
			{
        setVisible(false);
				model.proxyMode = proxyCheckBox.isSelected();
				model.portNumber = Integer.parseInt(portnumberTextField.getText().trim());
				model.proxyHostName = proxyhostTextField.getText().trim();
				model.proxyPortNumber = Integer.parseInt(proxyportnumberTextField.getText().trim());
				model.anonHostName = anonhostTextField.getText().trim();
				model.anonPortNumber  = Integer.parseInt(anonportnumberTextField.getText().trim());
				model.notifyJAPObservers();
			}
    
    public void selectCard(int selectedCard)
			{	
				// set selected card to foreground
				if (selectedCard == HTTP_TAB)
					tabs.setSelectedComponent(httpP);
				else if (selectedCard == ANON_TAB)
					tabs.setSelectedComponent(anonP);
				else if (selectedCard == MISC_TAB)
					tabs.setSelectedComponent(miscP);
				else
					tabs.setSelectedComponent(portP);
			}
  
	}

