import java.util.*;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;


public class JAPView extends JFrame implements ActionListener, JAPObserver {
	private JAPModel model;
	private JLabel				meterLabel;
	private JLabel				statusTextField1;
	private JLabel				statusTextField2;
	private JLabel				portnumberTextField;
	private JLabel				proxyportnumberTextField;
	private JLabel	 			proxyhostTextField;
	private JLabel				anonportnumberTextField;
	private JLabel	 			anonhostTextField;
	private JButton				portB, httpB, anonB, infoB, helpB, startB, quitB;
	private JCheckBox			proxyCheckBox;
	private JCheckBox			anonCheckBox;
	private JCheckBox			ano1CheckBox;
	private JProgressBar 		userProgressBar;
	private JProgressBar 		trafficProgressBar;
	private JProgressBar 		protectionProgressBar;
	private ImageIcon[]			meterIcons;
	private JAPHelp helpWindow;
	private JAPConf configDialog;
	

	public JAPView (JAPModel m, String s)
		{
			super(s);
			this.model = m;
			init();
			helpWindow =  new JAPHelp(this, model); 
			configDialog = new JAPConf(this, model);
		}
	
	public void init()
		{
			try
				{
					UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
				}
			catch(Exception e)
				{
					e.printStackTrace();
				}
			setIconImage(new ImageIcon(this.getClass().getResource("images/icon.gif")).getImage());
			// Show wait message
			JLabel waitLabel = new JLabel(model.msg.getString("loading"), JLabel.CENTER);
			//waitLabel.setFont(new Font("Sans", Font.BOLD,14));
			waitLabel.setBackground(Color.black);
			waitLabel.setForeground(Color.white);
			//Color bgColor = getContentPane().getBackground();
			getContentPane().setBackground(Color.black);
			getContentPane().add(waitLabel, BorderLayout.SOUTH);
			getContentPane().add(new JLabel(new ImageIcon(this.getClass().getResource(model.SPLASHFN))), BorderLayout.CENTER);
			//setSize(250, 50);
			setResizable(false);
			pack();
			centerFrame();
			setVisible(true);

		// listen for events from outside the frame
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {exitProgram();}
		});	
		
		if(CAVersion.checkForNewVersion(model)==1)
			{
				CAVersion.getNewVersion(model);
				System.out.println("Bitte neu starten...");
				System.exit(0);
			}
		// Load Images for "Anonymity Meter"
		loadMeterIcons();
		
		// "NORTH": Image
		ImageIcon northImage = new ImageIcon(this.getClass().getResource(model.msg.getString("northPath")));
		JLabel northLabel = new JLabel(northImage);

		// "West": Image
		ImageIcon westImage = new ImageIcon(this.getClass().getResource(model.msg.getString("westPath")));
		JLabel westLabel = new JLabel(westImage);
		westLabel.setOpaque(false);
		
		// "Center:" tabs
		JTabbedPane tabs = new JTabbedPane();
		JPanel config = buildConfigPanel();
		JPanel level = buildLevelPanel();
		tabs.addTab(model.msg.getString("mainConfTab"), new ImageIcon(this.getClass().getResource(model.CONFIGICONFN)), config );
		tabs.addTab(model.msg.getString("mainMeterTab"), new ImageIcon(this.getClass().getResource(model.METERICONFN)), level );
		
		// "South": Buttons
		JPanel buttonPanel = new JPanel();
//		buttonPanel.setOpaque(false);
		
		infoB = new JButton(model.msg.getString("infoButton"));
		helpB = new JButton(model.msg.getString("helpButton"));
//		startB = new JButton(model.msg.getString("startButton"));
		quitB = new JButton(model.msg.getString("quitButton"));
		// Add real buttons
		buttonPanel.add(infoB);
		buttonPanel.add(helpB);
//		buttonPanel.add(startB);
		buttonPanel.add(quitB);
		infoB.addActionListener(this);
		helpB.addActionListener(this);
//		startB.addActionListener(this);
		quitB.addActionListener(this);
		infoB.setMnemonic(model.msg.getString("infoButtonMn").charAt(0));
		helpB.setMnemonic(model.msg.getString("helpButtonMn").charAt(0));
//		startB.setMnemonic(model.msg.getString("startButtonMn").charAt(0));
		quitB.setMnemonic(model.msg.getString("quitButtonMn").charAt(0));
		

		// add Components to Frame
		setVisible(false);
		//setResizable(true);
		setBackground(buttonPanel.getBackground());
		getContentPane().setBackground(buttonPanel.getBackground());
		getContentPane().removeAll();
		getContentPane().add(buttonPanel, BorderLayout.SOUTH);
		getContentPane().add(northLabel, BorderLayout.NORTH);
		getContentPane().add(westLabel, BorderLayout.WEST);
		getContentPane().add(new JLabel("  "), BorderLayout.EAST); //Spacer
		getContentPane().add(tabs, BorderLayout.CENTER);

		updateValues();
		getContentPane().invalidate();
		pack();  // optimize size
		centerFrame();
		toFront();
		getContentPane().validate();
//		setVisible(true);
	}

    public JPanel buildLevelPanel() {
		JPanel levelPanel = new JPanel();
		levelPanel.setLayout( new BorderLayout() );
				
		//
		userProgressBar = new 
			JProgressBar(JProgressBar.HORIZONTAL,0, model.MAXPROGRESSBARVALUE);
		userProgressBar.setStringPainted(true);
		userProgressBar.setBorderPainted(false);
		//
		trafficProgressBar = new 
			JProgressBar(JProgressBar.HORIZONTAL, 0, model.MAXPROGRESSBARVALUE);
		trafficProgressBar.setStringPainted(true);
		trafficProgressBar.setBorderPainted(false);
		//
		protectionProgressBar = new 
			JProgressBar(JProgressBar.HORIZONTAL, 0, model.MAXPROGRESSBARVALUE);
		protectionProgressBar.setStringPainted(true);
		protectionProgressBar.setBorderPainted(false);
		//
		ano1CheckBox = new JCheckBox(model.msg.getString("confActivateCheckBox"));
		ano1CheckBox.setForeground(Color.red);
		ano1CheckBox.setMnemonic(model.msg.getString("confActivateCheckBoxMn").charAt(0));
		ano1CheckBox.addActionListener(this);

		JPanel meterPanel = new JPanel();
		meterPanel.setLayout( new BorderLayout() );
		meterPanel.setBorder( new TitledBorder(model.msg.getString("meterBorder")) );
		meterLabel = new JLabel(setMeterImage(model.NOMEASURE));
		meterPanel.add(ano1CheckBox,BorderLayout.NORTH);
		meterPanel.add(meterLabel, BorderLayout.CENTER);
		

		JPanel detailsPanel = new JPanel();
		detailsPanel.setLayout( new GridLayout(3,2,5,5) );
		detailsPanel.setBorder( new TitledBorder(model.msg.getString("meterDetailsBorder")) );
		detailsPanel.add(new JLabel(model.msg.getString("meterDetailsUsers")) );
		detailsPanel.add(userProgressBar);
		detailsPanel.add(new JLabel(model.msg.getString("meterDetailsTraffic")) );
		detailsPanel.add(trafficProgressBar);
		detailsPanel.add(new JLabel(model.msg.getString("meterDetailsRisk")) );
		detailsPanel.add(protectionProgressBar);

		levelPanel.add(meterPanel, BorderLayout.CENTER);
		levelPanel.add(detailsPanel, BorderLayout.SOUTH);

		return levelPanel;
    }
	
	public JPanel buildConfigPanel() {
		// "Center" Panel
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS) );
		//mainPanel.setBackground(Color.white);
		
		// Listen on Port
		JPanel portPanel = new JPanel();
		portPanel.setLayout(new GridLayout(2,1) );
		portPanel.setBorder( new TitledBorder(model.msg.getString("confListenerBorder")) );
		// Line 1
		JPanel p1 = new JPanel();
		p1.setLayout(new BoxLayout(p1, BoxLayout.X_AXIS) );
		p1.add(Box.createRigidArea(new Dimension(10,0)) );
		p1.add(new JLabel(model.msg.getString("confPort")) );
		p1.add(Box.createRigidArea(new Dimension(5,0)) );
		portnumberTextField = new JLabel(String.valueOf(model.portNumber));
//		portnumberTextField.setForeground(Color.black);
		p1.add(portnumberTextField );
		p1.add(Box.createRigidArea(new Dimension(5,0)) );
		p1.add(Box.createHorizontalGlue() );
		portB = new JButton(model.msg.getString("confPortButton"));
		portB.addActionListener(this);
		p1.add(portB);
		portPanel.add(p1);
		// Line 2
		JPanel p11 = new JPanel();
		p11.setLayout(new BoxLayout(p11, BoxLayout.X_AXIS) );
		p11.add(Box.createRigidArea(new Dimension(10,0)) );
		p11.add(new JLabel(model.msg.getString("confStatus1")) );
		p11.add(Box.createRigidArea(new Dimension(5,0)) );
		statusTextField1 = new JLabel("unknown");
		p11.add(statusTextField1);
		portPanel.add(p11);
		// add to mainPanel
		mainPanel.add(portPanel);	
		
		// HTTP Proxy
		JPanel proxyPanel = new JPanel();
		proxyPanel.setLayout(new GridLayout(3,1) );
		proxyPanel.setBorder( new TitledBorder(model.msg.getString("confProxyBorder")) );
		// Line 1
		proxyCheckBox = new JCheckBox(model.msg.getString("confProxyCheckBox"));
		proxyCheckBox.addActionListener(this);
		proxyPanel.add(proxyCheckBox );
		// Line 2
		JPanel p2 = new JPanel();
		p2.setLayout(new BoxLayout(p2, BoxLayout.X_AXIS) );
		p2.add(Box.createRigidArea(new Dimension(10,0)) );
		p2.add(new JLabel(model.msg.getString("confProxyHost")) );
		p2.add(Box.createRigidArea(new Dimension(5,0)) );
		proxyhostTextField = new JLabel(model.proxyHostName);
		// set Font in proxyCheckBox in same color as in proxyhostTextField
		proxyCheckBox.setForeground(proxyhostTextField.getForeground());
		p2.add(proxyhostTextField);
		proxyPanel.add(p2);
		// Line 3
		JPanel p3 = new JPanel();
		p3.setLayout(new BoxLayout(p3, BoxLayout.X_AXIS) );
		p3.add(Box.createRigidArea(new Dimension(10,0)) );
		p3.add(new JLabel(model.msg.getString("confProxyPort")) );
		p3.add(Box.createRigidArea(new Dimension(5,0)) );
		proxyportnumberTextField = new JLabel();
		p3.add(proxyportnumberTextField );
		p3.add(Box.createRigidArea(new Dimension(5,0)) );
		p3.add(Box.createHorizontalGlue() );
		httpB = new JButton(model.msg.getString("confProxyButton"));
		httpB.addActionListener(this);
		p3.add(httpB);
		proxyPanel.add(p3);
		// add to mainPanel
		mainPanel.add(proxyPanel);	
		
		// Activate Anonymity
		JPanel activatePanel = new JPanel();
		activatePanel.setLayout(new GridLayout(4,1) );
		activatePanel.setBorder( new TitledBorder(model.msg.getString("confActivateBorder")) );
		// Line 1
		JPanel p4 = new JPanel();
		p4.setLayout(new BoxLayout(p4, BoxLayout.X_AXIS) );
		//p4.add(Box.createRigidArea(new Dimension(10,0)) );
		anonCheckBox = new JCheckBox(model.msg.getString("confActivateCheckBox"));
		anonCheckBox.setForeground(Color.red);
		anonCheckBox.setMnemonic(model.msg.getString("confActivateCheckBoxMn").charAt(0));
		anonCheckBox.addActionListener(this);
		p4.add(anonCheckBox );
		p4.add(Box.createRigidArea(new Dimension(5,0)) );
		p4.add(Box.createHorizontalGlue() );
		anonB = new JButton(model.msg.getString("confActivateButton"));
		anonB.addActionListener(this);
		p4.add(anonB);
		activatePanel.add(p4);
		// Line 2
		JPanel p21 = new JPanel();
		p21.setLayout(new BoxLayout(p21, BoxLayout.X_AXIS) );
		p21.add(Box.createRigidArea(new Dimension(10,0)) );
		p21.add(new JLabel(model.msg.getString("confAnonHost")) );
		p21.add(Box.createRigidArea(new Dimension(5,0)) );
		anonhostTextField = new JLabel(model.anonHostName);
		p21.add(anonhostTextField);
		activatePanel.add(p21);
		// Line 3
		JPanel p31 = new JPanel();
		p31.setLayout(new BoxLayout(p31, BoxLayout.X_AXIS) );
		p31.add(Box.createRigidArea(new Dimension(10,0)) );
		p31.add(new JLabel(model.msg.getString("confAnonPort")) );
		p31.add(Box.createRigidArea(new Dimension(5,0)) );
		anonportnumberTextField = new JLabel();
		p31.add(anonportnumberTextField);
		activatePanel.add(p31);
		// Line 4
		JPanel p41 = new JPanel();
		p41.setLayout(new BoxLayout(p41, BoxLayout.X_AXIS) );
		p41.add(Box.createRigidArea(new Dimension(10,0)) );
		p41.add(new JLabel(model.msg.getString("confStatus2")) );
		p41.add(Box.createRigidArea(new Dimension(5,0)) );
		statusTextField2 = new JLabel("unknown");
		p41.add(statusTextField2);
		activatePanel.add(p41);
		// add to mainPanel
		mainPanel.add(activatePanel);	
		
		return mainPanel;
	}
	
	protected void loadMeterIcons()
		{
		// Load Images for "Anonymity Meter"
			meterIcons = new ImageIcon [model.METERFNARRAY.length];
			if (model.debug) 
				System.out.println("METERFNARRAY.length="+model.METERFNARRAY.length);
			for (int i=0; i<model.METERFNARRAY.length; i++)
				{
					meterIcons[i] = new ImageIcon(this.getClass().getResource(model.METERFNARRAY[i]));
					if (model.debug) 
						System.out.println("Image "+model.METERFNARRAY[i]+" loaded");
				}
		}
	
    public ImageIcon setMeterImage()
			{
				if (model.isAnonMode())
					{
						return meterIcons[model.getCurrentProtectionLevel()];
					}
				else
					{
						return meterIcons[1]; // Anon deactivated
					}
			}

    public ImageIcon setMeterImage(boolean b) {
		if (model.NOMEASURE) return meterIcons[0]; // No measure available
		else return setMeterImage();
	}

    protected void centerFrame() {
        Dimension screenSize = this.getToolkit().getScreenSize();
		Dimension ownSize = this.getSize();
		this.setLocation(
			(screenSize.width  - ownSize.width )/2,
			(screenSize.height - ownSize.height)/3
		);
    }

	public void actionPerformed(ActionEvent event){
		if (event.getSource() == quitB) { exitProgram(); } 
		if (event.getSource() == portB) { 
			if (model.debug) System.out.println("portB pressed");
			showConfigDialog(JAPConf.PORT_TAB);
		} else if (event.getSource() == httpB) { 
			if (model.debug) System.out.println("httpB pressed");
			showConfigDialog(JAPConf.HTTP_TAB);
		} else if (event.getSource() == anonB) { 
			if (model.debug) System.out.println("anonB pressed");
			showConfigDialog(JAPConf.ANON_TAB);
		} else if (event.getSource() == infoB) { 
			if (model.debug) System.out.println("infoB pressed");
			showInfoBox();
		} else if (event.getSource() == helpB) { 
			if (model.debug) System.out.println("helpB pressed");
			showHelpWindow();
		} else if (event.getSource() == proxyCheckBox) { 
			if (model.debug) 
				System.out.println("proxyCheckBox now "
					+ (proxyCheckBox.isSelected()?"selected":"unselected")
				);
				model.proxyMode = proxyCheckBox.isSelected();
				model.notifyJAPObservers();
		} else if (event.getSource() == anonCheckBox)
				{ 
					if (model.debug) 
						System.out.println("anonCheckBox now "+ (anonCheckBox.isSelected()?"selected":"unselected"));
					model.setAnonMode(anonCheckBox.isSelected());
				}
			else if (event.getSource() == ano1CheckBox) { 
				if (model.debug) 
				System.out.println("ano1CheckBox now "
					+ (ano1CheckBox.isSelected()?"selected":"unselected")
				);
				model.setAnonMode(ano1CheckBox.isSelected());
		} else {
			if (model.debug) System.out.println("Event ?????: "+event.getSource());
		}
	}
 
    public void showHelpWindow() {
//		JAPHelp d = new JAPHelp(this, model);
//		d.show();
		helpWindow.show();
    }

	private void showConfigDialog(int card) {
//		JAPConf d = new JAPConf(this, model);
		configDialog.selectCard(card);
		configDialog.show();
	}
	
    public void showInfoBox() {
        JOptionPane.showMessageDialog(
			this, 
			model.TITLE + "\n" + model.msg.getString("infoText") + "\n \n" + model.AUTHOR, 
			model.msg.getString("aboutBox"),
			JOptionPane.INFORMATION_MESSAGE,
			new ImageIcon(model.JAPICONFN)
		);
    }

	private void exitProgram() {
		// * requestListener.stop();
		model.goodBye();
		System.exit(0);
	}
	
    public void updateValues() {
		// Config panel
		portnumberTextField.setText(String.valueOf(model.portNumber));
		
		proxyportnumberTextField.setText(String.valueOf(model.proxyPortNumber));
		proxyhostTextField.setText(model.proxyHostName);
		
		anonportnumberTextField.setText(String.valueOf(model.anonPortNumber));
		anonhostTextField.setText(model.anonHostName);

		proxyCheckBox.setSelected(model.proxyMode);
		anonCheckBox.setSelected(model.isAnonMode());
		
		statusTextField1.setText(model.status1);
		statusTextField2.setText(model.status2);
		
		// Meter panel
		ano1CheckBox.setSelected(model.isAnonMode());
		meterLabel.setIcon(setMeterImage());
		if (model.isAnonMode()) {
			userProgressBar.setValue(model.nrOfActiveUsers);
			userProgressBar.setString(String.valueOf(model.nrOfActiveUsers));
			protectionProgressBar.setValue(model.currentRisk);
			trafficProgressBar.setValue(model.trafficSituation);
			if      (model.trafficSituation < 30) 
				trafficProgressBar.setString(model.msg.getString("meterTrafficLow"));
			else if (model.trafficSituation < 60) 
				trafficProgressBar.setString(model.msg.getString("meterTrafficMedium")); 
			else if (model.trafficSituation < 90) 
				trafficProgressBar.setString(model.msg.getString("meterTrafficHigh"));
			else                                  
				trafficProgressBar.setString(model.msg.getString("meterTrafficCongestion")); 
		} else {
			userProgressBar.setValue(model.MAXPROGRESSBARVALUE);
			userProgressBar.setString(model.msg.getString("meterNA"));
			protectionProgressBar.setValue(model.MAXPROGRESSBARVALUE);
			protectionProgressBar.setString(model.msg.getString("meterRiskVeryHigh"));
			trafficProgressBar.setValue(model.MAXPROGRESSBARVALUE);
			trafficProgressBar.setString(model.msg.getString("meterNA"));
		}
    }
	
	public void valuesChanged (Object o)
		{
			if (model.debug) System.out.println("view.valuesChanged()");
			updateValues();
		}
}


