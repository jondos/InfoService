import java.util.*;
import java.text.NumberFormat;
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
	private JProgressBar 		ownTrafficChannelsProgressBar;
	private JLabel 					ownTrafficBytesLabel;
	private ImageIcon[]			meterIcons;
	private JAPHelp helpWindow;
	private JAPConf configDialog;

	public JAPView (String s)
		{
			super(s);
			model = JAPModel.getModel();
			init();
			helpWindow =  new JAPHelp(this); 
			configDialog = new JAPConf(this);
		}
	
	public void init() {
	    try {
		UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName()); 
	    } 
	    catch(Exception e) {
		if (model.debug) e.printStackTrace();
	    }
		    
	    // Load Icon in upper left corner of the frame window
	    ImageIcon ii=model.loadImageIcon(model.IICON16FN,true);
	    if(ii!=null) setIconImage(ii.getImage());

/* 
   // mir unklar, was die 
   // folgenden auskommentierten 
   // Zeilen sollen (HF)
	
	    ii=loadImageIcon(model.getString("loading"),true);
	    JLabel waitLabel = null;
	    if(ii!=null)
		    waitLabel=new JLabel((ii), JLabel.CENTER);
	    else
		    waitLabel=new JLabel("", JLabel.CENTER);
*/

/*		
	    // Show wait message
	    JLabel waitLabel = new JLabel(model.getString("loading"), JLabel.CENTER);
	    //waitLabel.setFont(new Font("Sans", Font.BOLD,14));
	    waitLabel.setBackground(Color.black);
	    waitLabel.setForeground(Color.white);
	    Color bgColor = getContentPane().getBackground();
	    getContentPane().setBackground(Color.black);
	    getContentPane().add(waitLabel, BorderLayout.SOUTH);
	    getContentPane().add(new JLabel(loadImageIcon(model.SPLASHFN,true)), BorderLayout.CENTER);
	    //setSize(250, 50);
	    pack();
	    setResizable(false);
	    centerFrame();
	    setVisible(true);
*/
	    // listen for events from outside the frame
	    addWindowListener(new WindowAdapter() {
		    public void windowClosing(WindowEvent e) {exitProgram();}
	    });	
	    
	    // Check for new version on server
	    if(JAPVersion.checkForNewVersion(model)==1) {
		JAPVersion.getNewVersion(model);
		JOptionPane.showMessageDialog(this, model.getString("newVersion"));
		exitProgram();
	    }
	    
	    // Load Images for "Anonymity Meter"
	    loadMeterIcons();
	    
	    // "NORTH": Image
	    ImageIcon northImage = model.loadImageIcon(model.getString("northPath"),true);
	    JLabel northLabel = new JLabel(northImage);

	    // "West": Image
	    ImageIcon westImage = model.loadImageIcon(model.getString("westPath"),true);;
	    JLabel westLabel = new JLabel(westImage);
//		westLabel.setOpaque(false);
	    
	    // "Center:" tabs
	    JTabbedPane tabs = new JTabbedPane();
	    JPanel config = buildConfigPanel();
	    JPanel level = buildLevelPanel();
	    tabs.addTab(model.getString("mainConfTab"), model.loadImageIcon(model.CONFIGICONFN,true), config );
	    tabs.addTab(model.getString("mainMeterTab"), model.loadImageIcon(model.METERICONFN,true), level );
	    
	    // "South": Buttons
	    JPanel buttonPanel = new JPanel();
//		buttonPanel.setOpaque(false);
	    
	    infoB = new JButton(model.getString("infoButton"));
	    helpB = new JButton(model.getString("helpButton"));
//		startB = new JButton(model.msg.getString("startButton"));
	    quitB = new JButton(model.getString("quitButton"));
	    // Add real buttons
	    buttonPanel.add(infoB);
	    buttonPanel.add(helpB);
//		buttonPanel.add(startB);
	    buttonPanel.add(quitB);
	    infoB.addActionListener(this);
	    helpB.addActionListener(this);
//		startB.addActionListener(this);
	    quitB.addActionListener(this);
	    infoB.setMnemonic(model.getString("infoButtonMn").charAt(0));
	    helpB.setMnemonic(model.getString("helpButtonMn").charAt(0));
//		startB.setMnemonic(model.msg.getString("startButtonMn").charAt(0));
	    quitB.setMnemonic(model.getString("quitButtonMn").charAt(0));
	    

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
	    setResizable(false);
		try	
			{
				pack();  // optimize size
			}
		catch(Exception e)
			{
				if (model.debug) System.out.println("Hm.. Error by Pack - Has To be fixed!!");
			}
		model.centerFrame(this);
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
		ano1CheckBox = new JCheckBox(model.getString("confActivateCheckBox"));
		ano1CheckBox.setForeground(Color.red);
		ano1CheckBox.setMnemonic(model.getString("confActivateCheckBoxMn").charAt(0));
		ano1CheckBox.addActionListener(this);

		JPanel meterPanel = new JPanel();
		meterPanel.setLayout( new BorderLayout() );
		meterPanel.setBorder( new TitledBorder(model.getString("meterBorder")) );
		meterLabel = new JLabel(setMeterImage(model.NOMEASURE));
		meterPanel.add(ano1CheckBox,BorderLayout.NORTH);
		meterPanel.add(meterLabel, BorderLayout.CENTER);
		
		JPanel detailsPanel = new JPanel();
		detailsPanel.setLayout( new GridLayout(3,2,5,5) );
		detailsPanel.setBorder( new TitledBorder(model.getString("meterDetailsBorder")) );
		detailsPanel.add(new JLabel(model.getString("meterDetailsUsers")) );
		detailsPanel.add(userProgressBar);
		detailsPanel.add(new JLabel(model.getString("meterDetailsTraffic")) );
		detailsPanel.add(trafficProgressBar);
		detailsPanel.add(new JLabel(model.getString("meterDetailsRisk")) );
		detailsPanel.add(protectionProgressBar);

		// Own traffic situation: current # of channels
		ownTrafficChannelsProgressBar = new 
			JProgressBar(JProgressBar.HORIZONTAL,0, 1);
		ownTrafficChannelsProgressBar.setStringPainted(true);
		ownTrafficChannelsProgressBar.setBorderPainted(true);

		// Own traffic situation: # of bytes transmitted
		ownTrafficBytesLabel = new JLabel("",SwingConstants.RIGHT);
//			JProgressBar(JProgressBar.HORIZONTAL,0, model.MAXBYTESVALUE);
//		ownTrafficBytesProgressBar.setStringPainted(true);
//		ownTrafficBytesProgressBar.setBorderPainted(false);

		JPanel ownTrafficPanel = new JPanel();
		ownTrafficPanel.setLayout( new GridLayout(2,2,5,5) );
		ownTrafficPanel.setBorder( new TitledBorder(model.getString("ownTrafficBorder")) );
		ownTrafficPanel.add(new JLabel(model.getString("ownTrafficChannels")) );
		ownTrafficPanel.add(ownTrafficChannelsProgressBar);
		ownTrafficPanel.add(new JLabel(model.getString("ownTrafficBytes")) );
		ownTrafficPanel.add(ownTrafficBytesLabel);

		levelPanel.add(ownTrafficPanel, BorderLayout.NORTH);
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
		portPanel.setBorder( new TitledBorder(model.getString("confListenerBorder")) );
		// Line 1
		JPanel p1 = new JPanel();
		p1.setLayout(new BoxLayout(p1, BoxLayout.X_AXIS) );
		p1.add(Box.createRigidArea(new Dimension(10,0)) );
		p1.add(new JLabel(model.getString("confPort")) );
		p1.add(Box.createRigidArea(new Dimension(5,0)) );
		portnumberTextField = new JLabel(String.valueOf(model.portNumber));
//		portnumberTextField.setForeground(Color.black);
		p1.add(portnumberTextField );
		p1.add(Box.createRigidArea(new Dimension(5,0)) );
		p1.add(Box.createHorizontalGlue() );
		portB = new JButton(model.getString("confPortButton"));
		portB.addActionListener(this);
		p1.add(portB);
		portPanel.add(p1);
		// Line 2
		JPanel p11 = new JPanel();
		p11.setLayout(new BoxLayout(p11, BoxLayout.X_AXIS) );
		p11.add(Box.createRigidArea(new Dimension(10,0)) );
		p11.add(new JLabel(model.getString("confStatus1")) );
		p11.add(Box.createRigidArea(new Dimension(5,0)) );
		statusTextField1 = new JLabel("unknown");
		p11.add(statusTextField1);
		portPanel.add(p11);
		// add to mainPanel
		mainPanel.add(portPanel);	
		
		// HTTP Proxy
		JPanel proxyPanel = new JPanel();
		proxyPanel.setLayout(new GridLayout(3,1) );
		proxyPanel.setBorder( new TitledBorder(model.getString("confProxyBorder")) );
		// Line 1
		proxyCheckBox = new JCheckBox(model.getString("confProxyCheckBox"));
		proxyCheckBox.addActionListener(this);
		proxyPanel.add(proxyCheckBox );
		// Line 2
		JPanel p2 = new JPanel();
		p2.setLayout(new BoxLayout(p2, BoxLayout.X_AXIS) );
		p2.add(Box.createRigidArea(new Dimension(10,0)) );
		p2.add(new JLabel(model.getString("confProxyHost")) );
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
		p3.add(new JLabel(model.getString("confProxyPort")) );
		p3.add(Box.createRigidArea(new Dimension(5,0)) );
		proxyportnumberTextField = new JLabel();
		p3.add(proxyportnumberTextField );
		p3.add(Box.createRigidArea(new Dimension(5,0)) );
		p3.add(Box.createHorizontalGlue() );
		httpB = new JButton(model.getString("confProxyButton"));
		httpB.addActionListener(this);
		p3.add(httpB);
		proxyPanel.add(p3);
		// add to mainPanel
		mainPanel.add(proxyPanel);	
		
		// Activate Anonymity
		JPanel activatePanel = new JPanel();
		activatePanel.setLayout(new GridLayout(4,1) );
		activatePanel.setBorder( new TitledBorder(model.getString("confActivateBorder")) );
		// Line 1
		JPanel p4 = new JPanel();
		p4.setLayout(new BoxLayout(p4, BoxLayout.X_AXIS) );
		//p4.add(Box.createRigidArea(new Dimension(10,0)) );
		anonCheckBox = new JCheckBox(model.getString("confActivateCheckBox"));
		anonCheckBox.setForeground(Color.red);
		anonCheckBox.setMnemonic(model.getString("confActivateCheckBoxMn").charAt(0));
		anonCheckBox.addActionListener(this);
		p4.add(anonCheckBox );
		p4.add(Box.createRigidArea(new Dimension(5,0)) );
		p4.add(Box.createHorizontalGlue() );
		anonB = new JButton(model.getString("confActivateButton"));
		anonB.addActionListener(this);
		p4.add(anonB);
		activatePanel.add(p4);
		// Line 2
		JPanel p21 = new JPanel();
		p21.setLayout(new BoxLayout(p21, BoxLayout.X_AXIS) );
		p21.add(Box.createRigidArea(new Dimension(10,0)) );
		p21.add(new JLabel(model.getString("confAnonHost")) );
		p21.add(Box.createRigidArea(new Dimension(5,0)) );
		anonhostTextField = new JLabel(model.anonHostName);
		p21.add(anonhostTextField);
		activatePanel.add(p21);
		// Line 3
		JPanel p31 = new JPanel();
		p31.setLayout(new BoxLayout(p31, BoxLayout.X_AXIS) );
		p31.add(Box.createRigidArea(new Dimension(10,0)) );
		p31.add(new JLabel(model.getString("confAnonPort")) );
		p31.add(Box.createRigidArea(new Dimension(5,0)) );
		anonportnumberTextField = new JLabel();
		p31.add(anonportnumberTextField);
		activatePanel.add(p31);
		// Line 4
		JPanel p41 = new JPanel();
		p41.setLayout(new BoxLayout(p41, BoxLayout.X_AXIS) );
		p41.add(Box.createRigidArea(new Dimension(10,0)) );
		p41.add(new JLabel(model.getString("confStatus2")) );
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
					meterIcons[i] = model.loadImageIcon(model.METERFNARRAY[i],false);
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

    public ImageIcon setMeterImage(boolean b) 
			{
				if (model.NOMEASURE)
					return meterIcons[0]; // No measure available
				else
					return setMeterImage();
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
		} else if (event.getSource() == anonCheckBox) { 
			if (model.debug) 
				System.out.println("anonCheckBox now "+ (anonCheckBox.isSelected()?"selected":"unselected"));
			model.setAnonMode(anonCheckBox.isSelected());
		} else if (event.getSource() == ano1CheckBox) { 
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
			model.TITLE + "\n" + model.getString("infoText") + "\n \n" + model.AUTHOR, 
			model.getString("aboutBox"),
			JOptionPane.INFORMATION_MESSAGE //,
//			new ImageIcon(model.JAPICONFN)
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
		if (model.isAnonMode())
			{
				userProgressBar.setValue(model.nrOfActiveUsers);
				userProgressBar.setString(String.valueOf(model.nrOfActiveUsers));
				protectionProgressBar.setValue(model.currentRisk);
				trafficProgressBar.setValue(model.trafficSituation);
				if      (model.trafficSituation < 30) 
					trafficProgressBar.setString(model.getString("meterTrafficLow"));
				else if (model.trafficSituation < 60) 
					trafficProgressBar.setString(model.getString("meterTrafficMedium")); 
				else if (model.trafficSituation < 90) 
					trafficProgressBar.setString(model.getString("meterTrafficHigh"));
				else                                  
					trafficProgressBar.setString(model.getString("meterTrafficCongestion")); 
			} else {
			userProgressBar.setValue(model.MAXPROGRESSBARVALUE);
			userProgressBar.setString(model.getString("meterNA"));
			protectionProgressBar.setValue(model.MAXPROGRESSBARVALUE);
			protectionProgressBar.setString(model.getString("meterRiskVeryHigh"));
			trafficProgressBar.setValue(model.MAXPROGRESSBARVALUE);
			trafficProgressBar.setString(model.getString("meterNA"));
		}
			int c=model.getNrOfChannels();
			if(c>ownTrafficChannelsProgressBar.getMaximum())
				ownTrafficChannelsProgressBar.setMaximum(c);
			ownTrafficChannelsProgressBar.setValue(c);
			ownTrafficChannelsProgressBar.setString(String.valueOf(model.getNrOfChannels()));
			ownTrafficBytesLabel.setText(NumberFormat.getInstance().format(model.getNrOfBytes())+" Bytes");
    }
	
	public void valuesChanged (Object o)
		{
			if (model.debug) System.out.println("view.valuesChanged()");
			updateValues();
		}

}


