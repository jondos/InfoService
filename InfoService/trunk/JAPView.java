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


public final class JAPView extends JFrame implements ActionListener, JAPObserver {
	private JAPModel 			model;
	private JLabel				meterLabel;
	private JLabel				statusTextField1;
	private JLabel				statusTextField2;
	private JLabel				portnumberTextField;
	private JLabel				proxyTextField;
	private JLabel				infoServiceTextField;
	private JLabel	 			anonTextField;
	private JButton				portB, httpB, isB, anonB, infoB, helpB, startB, quitB, iconifyB;
	private JCheckBox			proxyCheckBox;
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
	
	private boolean m_bIsIconified;
	private String m_Title;

	public JAPView (String s)
		{
			super(s);
			m_Title=s;
			JAPDebug.out(JAPDebug.INFO,JAPDebug.MISC,"JAPView:initializing...");
			model = JAPModel.getModel();
			model.setView(this);
			init();
			helpWindow =  null;//new JAPHelp(this); 
			configDialog = null;//new JAPConf(this);
			JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"JAPView:initialization finished!");
			m_bIsIconified=false;
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
	    tabs.addTab(model.getString("mainConfTab"), JAPUtil.loadImageIcon(model.CONFIGICONFN,true), config );
	    tabs.addTab(model.getString("mainMeterTab"),JAPUtil.loadImageIcon(model.METERICONFN,true), level );
	    // "South": Buttons
	   
			JPanel buttonPanel = new JPanel();
	    infoB = new JButton(model.getString("infoButton"));
	    helpB = new JButton(model.getString("helpButton"));
	    quitB = new JButton(model.getString("quitButton"));
			iconifyB = new JButton(JAPUtil.loadImageIcon(model.ICONIFYICONFN,true));
			iconifyB.setToolTipText(model.getString("iconifyWindow"));
			
	    // Add real buttons
			buttonPanel.add(iconifyB);
			buttonPanel.add(infoB);
	    buttonPanel.add(helpB);
	    buttonPanel.add(quitB);
			iconifyB.addActionListener(this);
	    infoB.addActionListener(this);
	    helpB.addActionListener(this);
	    quitB.addActionListener(this);
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
				e.printStackTrace();
				JAPDebug.out(JAPDebug.EXCEPTION,JAPDebug.GUI,"JAPView:Hm.. Error by Pack - Has To be fixed!!");
				}
			JAPUtil.centerFrame(this);
	}

    public JPanel buildLevelPanel() {
		JPanel levelPanel = new JPanel(new BorderLayout());
				
		// Own traffic situation: current # of channels
		ownTrafficChannelsProgressBar = new 
			JProgressBar(JProgressBar.HORIZONTAL,0, 1);
		ownTrafficChannelsProgressBar.setStringPainted(true);
		ownTrafficChannelsProgressBar.setBorderPainted(true);

		// Own traffic situation: # of bytes transmitted
		ownTrafficBytesLabel = new JLabel("",SwingConstants.RIGHT);

		//
		userProgressBar = new 
			JProgressBar(JProgressBar.HORIZONTAL,0, 1);
		userProgressBar.setStringPainted(true);
		userProgressBar.setBorderPainted(true);
		//
		trafficProgressBar = new 
			JProgressBar(JProgressBar.HORIZONTAL, 0, model.MAXPROGRESSBARVALUE);
		trafficProgressBar.setStringPainted(true);
		trafficProgressBar.setBorderPainted(true);
		
		//
		protectionProgressBar = new 
			JProgressBar(JProgressBar.HORIZONTAL, 0, model.MAXPROGRESSBARVALUE);
		protectionProgressBar.setStringPainted(true);
		protectionProgressBar.setBorderPainted(true);
		
		
		JPanel ownTrafficPanel = new JPanel();
		ownTrafficPanel.setLayout( new GridLayout(2,2,5,5) );
		ownTrafficPanel.setBorder( new TitledBorder(model.getString("ownTrafficBorder")) );
		ownTrafficPanel.add(new JLabel(model.getString("ownTrafficChannels")) );
		ownTrafficPanel.add(ownTrafficChannelsProgressBar);
		ownTrafficPanel.add(new JLabel(model.getString("ownTrafficBytes")) );
		ownTrafficPanel.add(ownTrafficBytesLabel);
		
		ano1CheckBox = new JCheckBox(model.getString("confActivateCheckBox"));
		ano1CheckBox.setForeground(Color.red);
		ano1CheckBox.setMnemonic(model.getString("confActivateCheckBoxMn").charAt(0));
		ano1CheckBox.addActionListener(this);

		JPanel meterPanel = new JPanel();
		meterPanel.setLayout( new BorderLayout() );
		meterPanel.setBorder( new TitledBorder(model.getString("meterBorder")) );
		meterLabel = new JLabel(setMeterImage());
		meterPanel.add(ano1CheckBox,BorderLayout.NORTH);
		meterPanel.add(meterLabel, BorderLayout.CENTER);
		
		JPanel detailsPanel = new JPanel();
		detailsPanel.setLayout( new GridLayout(3,2,5,5) );
		detailsPanel.setBorder( new TitledBorder(model.getString("meterDetailsBorder")) );
		detailsPanel.add(new JLabel(model.getString("meterDetailsUsers")) );
		detailsPanel.add(userProgressBar);
//		detailsPanel.add(new JLabel(model.getString("meterDetailsTraffic")) );
//		detailsPanel.add(trafficProgressBar);
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
		proxyCheckBox = new JCheckBox(model.getString("confProxyCheckBox"));
		proxyCheckBox.addActionListener(this);
		p21.add(proxyCheckBox);
		p21.add(Box.createRigidArea(new Dimension(5,0)) );
		p21.add(Box.createHorizontalGlue() );
		httpB = new JButton(model.getString("confProxyButton"));
		httpB.addActionListener(this);
		p21.add(httpB);
		// Line 2
		JPanel p22 = new JPanel();
		p22.setLayout(new BoxLayout(p22, BoxLayout.X_AXIS) );
		p22.add(Box.createRigidArea(new Dimension(10,0)) );
		p22.add(new JLabel(model.getString("confProxyHost")) );
		p22.add(Box.createRigidArea(new Dimension(5,0)) );
		proxyTextField = new JLabel();
		p22.add(proxyTextField);
		// set Font in proxyCheckBox in same color as in proxyTextField
		proxyCheckBox.setForeground(proxyTextField.getForeground());
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
		anonCheckBox.setForeground(Color.red);
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
		// add to activatePanel
		activatePanel.add(p41);
		activatePanel.add(p42);
		activatePanel.add(p43);
		// add to mainPanel
		mainPanel.add(activatePanel);	
		
		return mainPanel;
	}
	
	public void disableSetAnonMode()
		{	
			anonCheckBox.setEnabled(false);
			ano1CheckBox.setEnabled(false);
		}
	
	protected void loadMeterIcons() {
		// Load Images for "Anonymity Meter"
		meterIcons = new ImageIcon [model.METERFNARRAY.length];
		JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"JAPView:METERFNARRAY.length="+model.METERFNARRAY.length);
		for (int i=0; i<model.METERFNARRAY.length; i++) {
			meterIcons[i] = JAPUtil.loadImageIcon(model.METERFNARRAY[i],false);
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
						return meterIcons[0]; // Anon deactivated
					}
			}

	public void actionPerformed(ActionEvent event)
			{
		//		JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"GetEvent: "+event.getSource());
				if (event.getSource() == quitB)
					exitProgram(); 
				else if (event.getSource() == iconifyB)
					model.setJAPViewIconified();
				else if (event.getSource() == portB)
					showConfigDialog(JAPConf.PORT_TAB);
				else if (event.getSource() == httpB)
					showConfigDialog(JAPConf.HTTP_TAB);
				else if (event.getSource() == isB)
					showConfigDialog(JAPConf.INFO_TAB);
				else if (event.getSource() == anonB)
					showConfigDialog(JAPConf.ANON_TAB);
				else if (event.getSource() == infoB)
					model.aboutJAP();
				else if (event.getSource() == helpB)
					showHelpWindow();
				else if (event.getSource() == proxyCheckBox) 
					model.setUseProxy(proxyCheckBox.isSelected());
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

	private void showConfigDialog(int card) {
		if(configDialog==null)
			{
				Cursor c=getCursor();
				setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				configDialog=new JAPConf(this);
				setCursor(c);
			}
		configDialog.selectCard(card);
		configDialog.updateValues();
		configDialog.show();
	}
	
	private void exitProgram() {
		model.goodBye(); // call the final exit procedure of JAP
	}
	
    private void updateValues() {
		// Config panel
		portnumberTextField.setText(String.valueOf(model.getPortNumber()));
		proxyCheckBox.setSelected(model.getUseProxy());
		proxyTextField.setText(model.getProxyHost()+":"+String.valueOf(model.getProxyPort()));
		infoServiceTextField.setText(model.getInfoServiceHost()+":"+String.valueOf(model.getInfoServicePort()));
		anonCheckBox.setSelected(model.isAnonMode());
		anonTextField.setText(model.anonHostName+":"+String.valueOf(model.anonPortNumber));
		
		statusTextField1.setText(model.status1);
		statusTextField2.setText(model.status2);
		
		// Meter panel
		ano1CheckBox.setSelected(model.isAnonMode());
		meterLabel.setIcon(setMeterImage());
		if (model.isAnonMode()) {
				if (model.nrOfActiveUsers != -1)
					{
						// Nr of active users
						if (model.nrOfActiveUsers > userProgressBar.getMaximum())
							userProgressBar.setMaximum(model.nrOfActiveUsers);
						userProgressBar.setValue(model.nrOfActiveUsers);
						userProgressBar.setString(String.valueOf(model.nrOfActiveUsers));
						if(m_bIsIconified)
							setTitle("JAP ("+Integer.toString(model.nrOfActiveUsers)+" "+model.getString("iconifiedviewUsers")+")");
					}
				else
					{
							userProgressBar.setValue(userProgressBar.getMaximum());
							userProgressBar.setString(model.getString("meterNA"));
					}
				if (model.currentRisk != -1) {
					// Current Risk
					protectionProgressBar.setValue(model.currentRisk);
					if (model.currentRisk < 80)
						protectionProgressBar.setString(String.valueOf(model.currentRisk)+" %");
					else
						protectionProgressBar.setString(model.getString("meterRiskVeryHigh"));
				} else {
					protectionProgressBar.setValue(protectionProgressBar.getMaximum());
					protectionProgressBar.setString(model.getString("meterNA"));
				}
				if (model.trafficSituation != -1) {
					// Traffic Situation
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
					trafficProgressBar.setValue(trafficProgressBar.getMaximum());
					trafficProgressBar.setString(model.getString("meterNA"));
				}
		} else {
			userProgressBar.setValue(userProgressBar.getMaximum());
			userProgressBar.setString(model.getString("meterNA"));
			protectionProgressBar.setValue(protectionProgressBar.getMaximum());
			if (model.isAnonMode())
				protectionProgressBar.setString(model.getString("meterNA"));
			else
				protectionProgressBar.setString(model.getString("meterRiskVeryHigh"));
			trafficProgressBar.setValue(trafficProgressBar.getMaximum());
			trafficProgressBar.setString(model.getString("meterNA"));
		}
		// Nr of Channels
		int c=model.getNrOfChannels();
		if (c > ownTrafficChannelsProgressBar.getMaximum())
			ownTrafficChannelsProgressBar.setMaximum(c);
		ownTrafficChannelsProgressBar.setValue(c);
		ownTrafficChannelsProgressBar.setString(String.valueOf(model.getNrOfChannels()));
		// Nr of Bytes transmitted anonymously
		ownTrafficBytesLabel.setText(NumberFormat.getInstance().format(model.getNrOfBytes())+" Bytes");
 	
		}
	
	public synchronized void valuesChanged (JAPModel m)
		{
			updateValues();
		}

}


