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
import java.text.NumberFormat;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.plaf.basic.BasicProgressBarUI;
import anon.JAPAnonServiceListener;

final class JAPView extends JFrame implements ActionListener, JAPObserver {
	
	public class MyProgressBarUI extends BasicProgressBarUI {
		public void paint(Graphics g, JComponent c) {
			JProgressBar pb = (JProgressBar) c;
			int dx=13;
			int max=pb.getMaximum(); 
			int anz    = pb.getWidth()/dx;
			int value  = pb.getValue()*anz/max;
			int x      = 0;
			int y      = 0;
			int height = c.getHeight();
			int width  = 9;
			for(int i=0;i<value;i++) {
				g.fill3DRect(x,y,width,height,false);
				x+=dx;
			}
			for(int i=value;i<anz;i++) {
				g.draw3DRect(x,y,width,height,false);
				x+=dx;
			}			
		}
/*
		public void update(Graphics g, JComponent c) {
		}
*/		
	}
	
	private JAPModel 			model;
	private JLabel				meterLabel;
	private JLabel	 			nameLabel;
	private JLabel				statusTextField1;
	private JLabel				statusTextField2;
	private JLabel				portnumberTextField;
	private JLabel				proxyTextField;
	private JLabel				infoServiceTextField;
	private JLabel	 			anonTextField;
	private JLabel              anonNameTextField;
	private JButton				portB, httpB, isB, anonB, ano1B, infoB, helpB, startB, quitB, iconifyB, confB;
	private JLabel			proxyMustUseLabel;
	private JCheckBox			anonCheckBox;
	private JCheckBox			ano1CheckBox;
	private JProgressBar 		userProgressBar;
	private JProgressBar 		trafficProgressBar;
	private JProgressBar 		protectionProgressBar;
	private JProgressBar 		ownTrafficChannelsProgressBar;
	private JLabel 				ownTrafficBytesLabel;
	private ImageIcon[]			meterIcons;
	private JAPHelp 			helpWindow;
	private JAPConf 			configDialog;
	private Object oValueUpdateSemaphore;
	private boolean m_bIsIconified;
	private String m_Title;
	private final static boolean PROGRESSBARBORDER = true;
	
	//private int diff, avg, last=-1, sum=0, cnt=0; // helper variables for calculating traffic situation

	public JAPView (String s)
		{
			super(s);
			m_Title=s;
			model = JAPModel.getModel();
			model.setView(this);
			helpWindow =  null;//new JAPHelp(this);
			configDialog = null;//new JAPConf(this);
			m_bIsIconified=false;
			oValueUpdateSemaphore=new Object();
		}

	public void create()
		{
			JAPDebug.out(JAPDebug.INFO,JAPDebug.GUI,"JAPView:initializing...");
			init();
			JAPDebug.out(JAPDebug.DEBUG,JAPDebug.GUI,"JAPView:initialization finished!");
		}

	private void init()
		{
			try
				{
					UIManager.setLookAndFeel(UIManager.getCrossPlatformLookAndFeelClassName());
				}
			catch (Exception e)
				{
					JAPDebug.out(JAPDebug.EXCEPTION,JAPDebug.GUI,"JAPView: "+e);
				}
	    // Load Icon in upper left corner of the frame window
	    ImageIcon ii=JAPUtil.loadImageIcon(model.IICON16FN,true);
	    if(ii!=null)
				setIconImage(ii.getImage());

			// listen for events from outside the frame
	    addWindowListener(new WindowAdapter() {
		    public void windowClosing(WindowEvent e) {exitProgram();}
	    });

			// Load Images for "Anonymity Meter"
	    loadMeterIcons();
	    // "NORTH": Image
	    ImageIcon northImage = JAPUtil.loadImageIcon(model.getString("northPath"),true);
	    JLabel northLabel = new JLabel(northImage);
	    // "West": Image
	    ImageIcon westImage = JAPUtil.loadImageIcon(model.getString("westPath"),true);;
	    JLabel westLabel = new JLabel(westImage);
	    // "Center:" tabs
	    JTabbedPane tabs = new JTabbedPane();
	    JPanel config = buildConfigPanel();
	    JPanel level = buildLevelPanel();
	    tabs.addTab(model.getString("mainMeterTab"),JAPUtil.loadImageIcon(model.METERICONFN, true), level );
	    tabs.addTab(model.getString("mainConfTab"), JAPUtil.loadImageIcon(model.CONFIGICONFN,true), config );
	    // "South": Buttons

			JPanel buttonPanel = new JPanel();
	    infoB = new JButton(model.getString("infoButton"));
	    helpB = new JButton(model.getString("helpButton"));
	    quitB = new JButton(model.getString("quitButton"));
	    confB = new JButton(model.getString("confButton"));
			iconifyB = new JButton(JAPUtil.loadImageIcon(model.ICONIFYICONFN,true));
			iconifyB.setToolTipText(model.getString("iconifyWindow"));

	    // Add real buttons
			buttonPanel.add(iconifyB);
			buttonPanel.add(infoB);
	    buttonPanel.add(helpB);
			buttonPanel.add(confB);
			buttonPanel.add(new JLabel("  "));
	    buttonPanel.add(quitB);
			iconifyB.addActionListener(this);
			confB.addActionListener(this);
	    infoB.addActionListener(this);
	    helpB.addActionListener(this);
	    quitB.addActionListener(this);
	    iconifyB.setMnemonic(model.getString("iconifyButtonMn").charAt(0));
	    confB.setMnemonic(model.getString("confButtonMn").charAt(0));
	    infoB.setMnemonic(model.getString("infoButtonMn").charAt(0));
	    helpB.setMnemonic(model.getString("helpButtonMn").charAt(0));
	    quitB.setMnemonic(model.getString("quitButtonMn").charAt(0));

			// add Components to Frame
			getContentPane().setBackground(buttonPanel.getBackground());
			getContentPane().add(buttonPanel, BorderLayout.SOUTH);
			getContentPane().add(northLabel, BorderLayout.NORTH);
			getContentPane().add(westLabel, BorderLayout.WEST);
			getContentPane().add(new JLabel("  "), BorderLayout.EAST); //Spacer
			getContentPane().add(tabs, BorderLayout.CENTER);

			updateValues();
			tabs.setSelectedComponent(level);

			this.addWindowListener(new WindowAdapter()
				{
					public void windowDeiconified(WindowEvent e)
						{
							m_bIsIconified=false;
							setTitle(m_Title);
						}
					public void windowIconified(WindowEvent e)
						{
							m_bIsIconified=true;
							updateValues();
						}
				});

			try
				{
					pack();  // optimize size
					setResizable(false);
				}
			catch(Exception e) {
				JAPDebug.out(JAPDebug.EXCEPTION,JAPDebug.GUI,"JAPView:Hm.. Error by Pack - Has To be fixed!!");
				}
			JAPUtil.centerFrame(this);
	}

    public JPanel buildLevelPanel() {
		JPanel levelPanel = new JPanel(new BorderLayout());
//		JPanel levelPanel = new JPanel();
//		levelPanel.setLayout(new BoxLayout(levelPanel, BoxLayout.Y_AXIS) );

		// Own traffic situation: current # of channels
		ownTrafficChannelsProgressBar = new JProgressBar(JProgressBar.HORIZONTAL,0, 1);
		ownTrafficChannelsProgressBar.setUI(new MyProgressBarUI());
		ownTrafficChannelsProgressBar.setStringPainted(true);
		ownTrafficChannelsProgressBar.setBorderPainted(false /*PROGRESSBARBORDER*/);
		ownTrafficChannelsProgressBar.setString(" ");

		// Own traffic situation: # of bytes transmitted
		ownTrafficBytesLabel = new JLabel("0 Bytes",SwingConstants.RIGHT);

		//
		userProgressBar = new
			JProgressBar(JProgressBar.HORIZONTAL,0, 1);
		userProgressBar.setStringPainted(true);
		userProgressBar.setBorderPainted(PROGRESSBARBORDER);
		//
		trafficProgressBar = new
			JProgressBar(JProgressBar.HORIZONTAL);
		trafficProgressBar.setStringPainted(true);
		trafficProgressBar.setBorderPainted(PROGRESSBARBORDER);

		//
		protectionProgressBar = new
			JProgressBar(JProgressBar.HORIZONTAL);
		protectionProgressBar.setStringPainted(true);
		protectionProgressBar.setBorderPainted(PROGRESSBARBORDER);


		JPanel ownTrafficPanel = new JPanel();
		ownTrafficPanel.setLayout( new GridLayout(2,2,5,5) );
		ownTrafficPanel.setBorder( new TitledBorder(model.getString("ownTrafficBorder")) );
		ownTrafficPanel.add(new JLabel(model.getString("ownTrafficChannels")) );
		ownTrafficPanel.add(ownTrafficChannelsProgressBar);
		ownTrafficPanel.add(new JLabel(model.getString("ownTrafficBytes")) );
		ownTrafficPanel.add(ownTrafficBytesLabel);

		ano1CheckBox = new JCheckBox(model.getString("confActivateCheckBox"));
//		ano1CheckBox.setForeground(Color.red);		
		ano1CheckBox.setMnemonic(model.getString("confActivateCheckBoxMn").charAt(0));
		ano1CheckBox.addActionListener(this);

		// Line 1
		JPanel p41 = new JPanel();
		p41.setLayout(new BoxLayout(p41, BoxLayout.X_AXIS) );
		//p41.add(Box.createRigidArea(new Dimension(10,0)) );
		p41.add(ano1CheckBox );
		p41.add(Box.createRigidArea(new Dimension(5,0)) );
		p41.add(Box.createHorizontalGlue() );
		ano1B = new JButton(model.getString("confActivateButton"));
		ano1B.addActionListener(this);
		p41.add(ano1B);

		JPanel meterPanel = new JPanel();
		meterPanel.setLayout( new BorderLayout() );
		meterPanel.setBorder( new TitledBorder(model.getString("meterBorder")) );
		meterLabel = new JLabel(getMeterImage(-1));
		meterPanel.add(p41/*ano1CheckBox*/,BorderLayout.NORTH);
		meterPanel.add(meterLabel, BorderLayout.CENTER);

		JPanel detailsPanel = new JPanel();
		detailsPanel.setLayout( new GridLayout(3,2,5,5) );
		detailsPanel.setBorder( new TitledBorder(model.getString("meterDetailsBorder")) );
		nameLabel = new JLabel();
		detailsPanel.add(new JLabel(model.getString("meterDetailsName")) );
		detailsPanel.add(nameLabel);
		detailsPanel.add(new JLabel(model.getString("meterDetailsUsers")) );
		detailsPanel.add(userProgressBar);
		detailsPanel.add(new JLabel(model.getString("meterDetailsTraffic")) );
		detailsPanel.add(trafficProgressBar);
//		detailsPanel.add(new JLabel(model.getString("meterDetailsRisk")) );
//		detailsPanel.add(protectionProgressBar);

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
		JPanel p11 = new JPanel();
		p11.setLayout(new BoxLayout(p11, BoxLayout.X_AXIS) );
		p11.add(Box.createRigidArea(new Dimension(10,0)) );
		p11.add(new JLabel(model.getString("confPort")) );
		p11.add(Box.createRigidArea(new Dimension(5,0)) );
		portnumberTextField = new JLabel(String.valueOf(model.getPortNumber()));
//		portnumberTextField.setForeground(Color.black);
		p11.add(portnumberTextField );
		p11.add(Box.createRigidArea(new Dimension(5,0)) );
		p11.add(Box.createHorizontalGlue() );
		portB = new JButton(model.getString("confPortButton"));
		portB.addActionListener(this);
		p11.add(portB);
		// Line 2
		JPanel p12 = new JPanel();
		p12.setLayout(new BoxLayout(p12, BoxLayout.X_AXIS) );
		p12.add(Box.createRigidArea(new Dimension(10,0)) );
		p12.add(new JLabel(model.getString("confStatus1")) );
		p12.add(Box.createRigidArea(new Dimension(5,0)) );
		statusTextField1 = new JLabel("unknown");
		p12.add(statusTextField1);
		// add to portPanel
		portPanel.add(p11);
		portPanel.add(p12);
		// add to mainPanel
		mainPanel.add(portPanel);

		// HTTP Proxy
		JPanel proxyPanel = new JPanel();
		proxyPanel.setLayout(new GridLayout(2,1) );
		proxyPanel.setBorder( new TitledBorder(model.getString("confProxyBorder")) );
		// Line 1
		JPanel p21 = new JPanel();
		p21.setLayout(new BoxLayout(p21, BoxLayout.X_AXIS) );
		p21.add(Box.createRigidArea(new Dimension(10,0)) );
		proxyMustUseLabel = new JLabel();
		p21.add(proxyMustUseLabel);
		p21.add(Box.createRigidArea(new Dimension(5,0)) );
		p21.add(Box.createHorizontalGlue() );
		httpB = new JButton(model.getString("confProxyButton"));
		httpB.addActionListener(this);
		p21.add(httpB);
		// Line 2
		JPanel p22 = new JPanel();
		p22.setLayout(new BoxLayout(p22, BoxLayout.X_AXIS) );
		p22.add(Box.createRigidArea(new Dimension(10,0)) );
//		p22.add(new JLabel(model.getString("confProxyHost")) );
//		p22.add(Box.createRigidArea(new Dimension(5,0)) );
		proxyTextField = new JLabel();
		p22.add(proxyTextField);
		// add to proxypanel
		proxyPanel.add(p21);
		proxyPanel.add(p22);
		// add to mainPanel
		mainPanel.add(proxyPanel);

		// Information Service
		JPanel infoServicePanel = new JPanel();
		infoServicePanel.setLayout(new GridLayout(1,1) );
		infoServicePanel.setBorder( new TitledBorder(model.getString("confInfoServiceBorder")) );
		// Line 1
		JPanel p31 = new JPanel();
		p31.setLayout(new BoxLayout(p31, BoxLayout.X_AXIS) );
		p31.add(Box.createRigidArea(new Dimension(10,0)) );
		p31.add(new JLabel(model.getString("confInfoServiceHost")) );
		p31.add(Box.createRigidArea(new Dimension(5,0)) );
		infoServiceTextField = new JLabel();
		p31.add(infoServiceTextField);
		p31.add(Box.createRigidArea(new Dimension(5,0)) );
		p31.add(Box.createHorizontalGlue() );
		isB = new JButton(model.getString("confInfoServiceButton"));
		isB.addActionListener(this);
		p31.add(isB);
		// add to infoServicePanel
		infoServicePanel.add(p31);
		// add to mainPanel
		mainPanel.add(infoServicePanel);

		// Activate Anonymity
		JPanel activatePanel = new JPanel();
		activatePanel.setLayout(new GridLayout(4,1) );
		activatePanel.setBorder( new TitledBorder(model.getString("confActivateBorder")) );
		// Line 1
		JPanel p41 = new JPanel();
		p41.setLayout(new BoxLayout(p41, BoxLayout.X_AXIS) );
		//p41.add(Box.createRigidArea(new Dimension(10,0)) );
		anonCheckBox = new JCheckBox(model.getString("confActivateCheckBox"));
//		anonCheckBox.setForeground(Color.red);
		anonCheckBox.setMnemonic(model.getString("confActivateCheckBoxMn").charAt(0));
		anonCheckBox.addActionListener(this);
		p41.add(anonCheckBox );
		p41.add(Box.createRigidArea(new Dimension(5,0)) );
		p41.add(Box.createHorizontalGlue() );
		anonB = new JButton(model.getString("confActivateButton"));
		anonB.addActionListener(this);
		p41.add(anonB);
		// Line 2
		JPanel p42 = new JPanel();
		p42.setLayout(new BoxLayout(p42, BoxLayout.X_AXIS) );
		p42.add(Box.createRigidArea(new Dimension(10,0)) );
		p42.add(new JLabel(model.getString("confAnonHost")) );
		p42.add(Box.createRigidArea(new Dimension(5,0)) );
		anonTextField = new JLabel();
		p42.add(anonTextField);
		// Line 3
		JPanel p43 = new JPanel();
		p43.setLayout(new BoxLayout(p43, BoxLayout.X_AXIS) );
		p43.add(Box.createRigidArea(new Dimension(10,0)) );
		p43.add(new JLabel(model.getString("confStatus2")) );
		p43.add(Box.createRigidArea(new Dimension(5,0)) );
		statusTextField2 = new JLabel("unknown");
		p43.add(statusTextField2);
		// Line 4
		JPanel p44 = new JPanel();
		p44.setLayout(new BoxLayout(p44, BoxLayout.X_AXIS) );
		p44.add(Box.createRigidArea(new Dimension(10,0)) );
		p44.add(new JLabel(model.getString("confAnonName")) );
		p44.add(Box.createRigidArea(new Dimension(5,0)) );
		anonNameTextField = new JLabel();
		p44.add(anonNameTextField);
		// add to activatePanel
		activatePanel.add(p41);
		activatePanel.add(p44);
		activatePanel.add(p42);
		activatePanel.add(p43);
		// add to mainPanel
		mainPanel.add(activatePanel);

		return mainPanel;
	}

	/** 
	 * Used to disable activation on JAP
	 * Example: Activation of listener failed
	 *          --> disable activation checkboxes
	 */
	public void disableSetAnonMode()
		{
			anonCheckBox.setEnabled(false);
			ano1CheckBox.setEnabled(false);
		}

	protected void loadMeterIcons() {
		// Load Images for "Anonymity Meter"
		meterIcons = new ImageIcon [model.METERFNARRAY.length];
//		JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"JAPView:METERFNARRAY.length="+model.METERFNARRAY.length);
		for (int i=0; i<model.METERFNARRAY.length; i++) {
			meterIcons[i] = JAPUtil.loadImageIcon(model.METERFNARRAY[i],false);
		}
	}

	 /**Anon Level is >=0 amd <=5. if -1 no measure is available*/
    private ImageIcon getMeterImage(int iAnonLevel)
			{
				if (model.getAnonMode())
					{
						if(iAnonLevel>=0&&iAnonLevel<6)
						  return meterIcons[iAnonLevel+2];
					  else
							return meterIcons[1];//No measure available
					}
				else
					{
						return meterIcons[0]; // Anon deactivated
					}
			}

	public void actionPerformed(ActionEvent event)
			{
		//		JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"GetEvent: "+event.getSource());
				if (event.getSource() == quitB)
					exitProgram();
				else if (event.getSource() == iconifyB) {
					model.getIconifiedView().setVisible(true);
					this.setVisible(false);
//					model.setJAPViewIconified();
				}
				else if (event.getSource() == confB)
					showConfigDialog();					
				else if (event.getSource() == portB)
					showConfigDialog(JAPConf.PORT_TAB);
				else if (event.getSource() == httpB)
					showConfigDialog(JAPConf.HTTP_TAB);
				else if (event.getSource() == isB)
					showConfigDialog(JAPConf.INFO_TAB);
				else if (event.getSource() == anonB)
					showConfigDialog(JAPConf.ANON_TAB);
				else if (event.getSource() == ano1B)
					showConfigDialog(JAPConf.ANON_TAB);
				else if (event.getSource() == infoB)
					model.aboutJAP();
				else if (event.getSource() == helpB)
					showHelpWindow();
				else if (event.getSource() == anonCheckBox)
					model.setAnonMode(anonCheckBox.isSelected());
				else if (event.getSource() == ano1CheckBox)
					model.setAnonMode(ano1CheckBox.isSelected());
				else
					JAPDebug.out(JAPDebug.DEBUG,JAPDebug.GUI,"Event ?????: "+event.getSource());
			}

  private void showHelpWindow()
			{
				if(helpWindow==null)
					helpWindow=new JAPHelp(this);
				helpWindow.show();
			}

	private void showConfigDialog() { 
		showConfigDialog(-1);
	}
	
	private void showConfigDialog(int card) {
		if(configDialog==null)
			{
				Cursor c=getCursor();
				setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				configDialog=new JAPConf(this);
				setCursor(c);
			}
		if (card!=-1) 
			configDialog.selectCard(card);
		configDialog.updateValues();
		configDialog.show();
	}

	private void exitProgram() {
		model.goodBye(); // call the final exit procedure of JAP
	}

  private void updateValues() {
		AnonServerDBEntry e = model.getAnonServer();
		// Config panel
		JAPDebug.out(JAPDebug.DEBUG,JAPDebug.GUI,"Start updateValues");
		portnumberTextField.setText(String.valueOf(model.getPortNumber()));
		if(model.getUseFirewall())
			{
			  proxyMustUseLabel.setText(JAPMessages.getString("firewallMustUse"));
			  proxyTextField.setVisible(true);
				int firewallPort=model.getFirewallPort();
				if(firewallPort==-1)
					proxyTextField.setText(JAPMessages.getString("firewallNotConfigured"));
				else
					proxyTextField.setText(model.getString("confProxyHost")+" "+model.getFirewallHost()+":"+String.valueOf(firewallPort));
			}
		else
			{
			  proxyMustUseLabel.setText(JAPMessages.getString("firewallMustNotUse"));
			  proxyTextField.setVisible(false);
			}
		infoServiceTextField.setText(model.getInfoServiceHost()+":"+String.valueOf(model.getInfoServicePort()));
		anonTextField.setText(e.getHost()+":"+String.valueOf(e.getPort())+((e.getSSLPort()==-1)?"":":"+e.getSSLPort()));
		anonNameTextField.setText(e.getName());
		statusTextField1.setText(model.status1);
		statusTextField2.setText(model.status2);
		anonCheckBox.setSelected(model.getAnonMode());
		if(model.getAnonMode()) {
			anonCheckBox.setForeground(Color.black);
		} else {
			anonCheckBox.setForeground(Color.red);
		}

		// Meter panel
		ano1CheckBox.setSelected(model.getAnonMode());
		if(model.getAnonMode()) {
			ano1CheckBox.setForeground(Color.black);
		} else {
			ano1CheckBox.setForeground(Color.red);
		}
		nameLabel.setText(e.getName());
		meterLabel.setIcon(getMeterImage(e.getAnonLevel()));
		if (model.getAnonMode()) {
				if (e.getNrOfActiveUsers() > -1)
					{
						// Nr of active users
						if (e.getNrOfActiveUsers() > userProgressBar.getMaximum())
							userProgressBar.setMaximum(e.getNrOfActiveUsers());
						userProgressBar.setValue(e.getNrOfActiveUsers());
						userProgressBar.setString(String.valueOf(e.getNrOfActiveUsers()));
						if(m_bIsIconified)
							setTitle("JAP ("+Integer.toString(e.getNrOfActiveUsers())+" "+model.getString("iconifiedviewUsers")+")");
					}
				else
					{
							userProgressBar.setValue(userProgressBar.getMaximum());
							userProgressBar.setString(model.getString("meterNA"));
					}
				if (e.getCurrentRisk() > -1) {
					// Current Risk
					if (e.getCurrentRisk() > protectionProgressBar.getMaximum())
							protectionProgressBar.setMaximum(e.getCurrentRisk());
					protectionProgressBar.setValue(e.getCurrentRisk());
					if (e.getCurrentRisk() < 80)
						protectionProgressBar.setString(String.valueOf(e.getCurrentRisk())+" %");
					else
						protectionProgressBar.setString(model.getString("meterRiskVeryHigh"));
				} else {
					protectionProgressBar.setValue(protectionProgressBar.getMaximum());
					protectionProgressBar.setString(model.getString("meterNA"));
				}
        int t=e.getTrafficSituation();
				if(t>-1)
          { //Trafic Situation directly form InfoService
            trafficProgressBar.setMaximum(100);
            trafficProgressBar.setValue(t);
            if(t < 30)
              trafficProgressBar.setString(model.getString("meterTrafficLow"));
            else if (t< 60)
              trafficProgressBar.setString(model.getString("meterTrafficMedium"));
            else
              trafficProgressBar.setString(model.getString("meterTrafficHigh"));
          }
        else { // no value from InfoService
					trafficProgressBar.setValue(trafficProgressBar.getMaximum());
					trafficProgressBar.setString(model.getString("meterNA"));
				}
		} else {
			userProgressBar.setValue(userProgressBar.getMaximum());
			userProgressBar.setString(model.getString("meterNA"));
			protectionProgressBar.setValue(protectionProgressBar.getMaximum());
			if (model.getAnonMode())
				protectionProgressBar.setString(model.getString("meterNA"));
			else
				protectionProgressBar.setString(model.getString("meterRiskVeryHigh"));
			trafficProgressBar.setValue(trafficProgressBar.getMaximum());
			trafficProgressBar.setString(model.getString("meterNA"));
		}


		}

		public void channelsChanged(int c)
		{
		// Nr of Channels
		//int c=model.getNrOfChannels();
		if (c > ownTrafficChannelsProgressBar.getMaximum())
			ownTrafficChannelsProgressBar.setMaximum(c);
		ownTrafficChannelsProgressBar.setValue(c);
//		ownTrafficChannelsProgressBar.setString(String.valueOf(c));
		}
		public void transferedBytes(int c)
		{
		// Nr of Bytes transmitted anonymously
		ownTrafficBytesLabel.setText(NumberFormat.getInstance().format(c)+" Bytes");
		}

		public void valuesChanged (JAPModel m)
		{
			synchronized(oValueUpdateSemaphore)
				{
//					JAPDebug.out(JAPDebug.DEBUG,JAPDebug.GUI,"Start valuesChanged");
					updateValues();
//					JAPDebug.out(JAPDebug.DEBUG,JAPDebug.GUI,"End valuesChanged");
				}
		}

}


