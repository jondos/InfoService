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
import anon.AnonServer;

final public class JAPView extends JFrame implements ActionListener, JAPObserver {

	public class MyProgressBarUI extends BasicProgressBarUI {
		public void paint(Graphics g, JComponent c) {
			JProgressBar pb = (JProgressBar) c;
			int dx     = 13;
			int max    = pb.getMaximum();
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

	private JAPController 			controller;
	private JLabel				meterLabel;
	private JLabel	 			m_labelCascadeName;
  private JPanel        m_panelMain;
	//private JLabel				m_labelProxyPort;
	//private JLabel				m_labelProxyHost;
	//private JLabel				infoServiceTextField;
	//private JLabel	 			anonTextField;
	//private JLabel              anonNameTextField;
	private JButton				/*portB, httpB,isB, anonB,*/ ano1B, infoB, helpB, quitB, iconifyB, confB;
	//private JLabel			proxyMustUseLabel;
	//private JCheckBox			anonCheckBox;
	private JCheckBox			ano1CheckBox;
	private JProgressBar 		userProgressBar;
	private JProgressBar 		trafficProgressBar;
	private JProgressBar 		protectionProgressBar;
	private JProgressBar 		ownTrafficChannelsProgressBar;
	private JLabel 				m_labelOwnTrafficBytes;
	private ImageIcon[]			meterIcons;
	private JAPHelp 			helpWindow;
	private JAPConf 			configDialog;
	private Frame				viewIconified;
	private Object oValueUpdateSemaphore;
	private boolean m_bIsIconified;
	private String m_Title;
	private final static boolean PROGRESSBARBORDER = true;

	public JAPView (String s)
		{
			super(s);
			m_Title=s;
			controller = JAPController.getController();
			helpWindow =  null;//new JAPHelp(this);
			configDialog = null;//new JAPConf(this);
			m_bIsIconified=false;
			oValueUpdateSemaphore=new Object();
		}

	public void create()
		{
			JAPDebug.out(JAPDebug.INFO,JAPDebug.GUI,"JAPView:initializing...");
			init();
//			JAPDebug.out(JAPDebug.DEBUG,JAPDebug.GUI,"JAPView:initialization finished!");
		}

	private void init()
		{

	    // Load Icon in upper left corner of the frame window
	    ImageIcon ii=JAPUtil.loadImageIcon(JAPConstants.IICON16FN,true);
	    if(ii!=null)
				setIconImage(ii.getImage());

			// listen for events from outside the frame
	    addWindowListener(new WindowAdapter() {
		    public void windowClosing(WindowEvent e) {exitProgram();}
	    });

			// Load Images for "Anonymity Meter"
	    loadMeterIcons();
	    // "NORTH": Image
	    ImageIcon northImage = JAPUtil.loadImageIcon(JAPMessages.getString("northPath"),true);
	    JLabel northLabel = new JLabel(northImage);
		  JPanel northPanel = new JPanel();
		  northPanel.setLayout(new BoxLayout(northPanel, BoxLayout.X_AXIS) );
		  northPanel.add(northLabel);
		  northPanel.add(Box.createHorizontalGlue());

	    // "West": Image
	    ImageIcon westImage = JAPUtil.loadImageIcon(JAPMessages.getString("westPath"),true);;
	    JLabel westLabel = new JLabel(westImage);

			// "Center:" tabs
	    //JTabbedPane tabs = new JTabbedPane();
	    //JPanel config = buildConfigPanel();
	    JPanel level = buildLevelPanel();
	    //tabs.addTab(JAPMessages.getString("mainMeterTab"),JAPUtil.loadImageIcon(JAPConstants.METERICONFN, true), level );
	    // "South": Buttons

			JPanel buttonPanel = new JPanel();
	    infoB = new JButton(JAPMessages.getString("infoButton"));
	    helpB = new JButton(JAPMessages.getString("helpButton"));
	    quitB = new JButton(JAPMessages.getString("quitButton"));
	    confB = new JButton(JAPMessages.getString("confButton"));
			iconifyB = new JButton(JAPUtil.loadImageIcon(JAPConstants.ICONIFYICONFN,true));
			iconifyB.setToolTipText(JAPMessages.getString("iconifyWindow"));

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
	    JAPUtil.setMnemonic(iconifyB,JAPMessages.getString("iconifyButtonMn"));
	    JAPUtil.setMnemonic(confB,JAPMessages.getString("confButtonMn"));
		  JAPUtil.setMnemonic(infoB,JAPMessages.getString("infoButtonMn"));
	    JAPUtil.setMnemonic(helpB,JAPMessages.getString("helpButtonMn"));
	    JAPUtil.setMnemonic(quitB,JAPMessages.getString("quitButtonMn"));

			// add Components to Frame
			getContentPane().setBackground(buttonPanel.getBackground());
			getContentPane().add(/*tabs*/level, BorderLayout.CENTER);
      m_panelMain=level;
			getContentPane().add(buttonPanel, BorderLayout.SOUTH);
//			getContentPane().add(northLabel, BorderLayout.NORTH);
			if(!JAPModel.isSmallDisplay())
        {
          getContentPane().add(northPanel, BorderLayout.NORTH);
			    getContentPane().add(westLabel, BorderLayout.WEST);
			    getContentPane().add(new JLabel("  "), BorderLayout.EAST); //Spacer
        }
			//tabs.setSelectedComponent(level);

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
					setResizable(/*true*/true/*false*/); //2001-11-12(HF):Changed due to a Mac OS X problem during redraw of the progress bars
				}
			catch(Exception e) {
				JAPDebug.out(JAPDebug.EXCEPTION,JAPDebug.GUI,"JAPView:Hm.. Error by Pack - Has To be fixed!!");
				}
			updateValues();
			JAPUtil.centerFrame(this);
	}

    private JPanel buildLevelPanel() {
		JPanel levelPanel = new JPanel(new BorderLayout());
//		JPanel levelPanel = new JPanel();
//		levelPanel.setLayout(new BoxLayout(levelPanel, BoxLayout.Y_AXIS) );

		// Own traffic situation: current # of channels
		ownTrafficChannelsProgressBar = new JProgressBar(JProgressBar.HORIZONTAL,0, 1);
		Font fontControls=JAPController.getDialogFont();
    ownTrafficChannelsProgressBar.setFont(fontControls);
    ownTrafficChannelsProgressBar.setUI(new MyProgressBarUI());
		ownTrafficChannelsProgressBar.setStringPainted(true);
		ownTrafficChannelsProgressBar.setBorderPainted(false /*PROGRESSBARBORDER*/);
		ownTrafficChannelsProgressBar.setString(" ");

		// Own traffic situation: # of bytes transmitted
		m_labelOwnTrafficBytes = new JLabel("0 Bytes",SwingConstants.RIGHT);
    m_labelOwnTrafficBytes.setFont(fontControls);
		//
		userProgressBar = new
			JProgressBar(JProgressBar.HORIZONTAL,0, 1);
		userProgressBar.setStringPainted(true);
		userProgressBar.setBorderPainted(PROGRESSBARBORDER);
    userProgressBar.setFont(fontControls);
		//
		trafficProgressBar = new
			JProgressBar(JProgressBar.HORIZONTAL);
		trafficProgressBar.setStringPainted(true);
		trafficProgressBar.setBorderPainted(PROGRESSBARBORDER);
    trafficProgressBar.setFont(fontControls);
		//
		protectionProgressBar = new
			JProgressBar(JProgressBar.HORIZONTAL);
		protectionProgressBar.setStringPainted(true);
		protectionProgressBar.setBorderPainted(PROGRESSBARBORDER);
    protectionProgressBar.setFont(fontControls);

		JPanel ownTrafficPanel = new JPanel();
		ownTrafficPanel.setLayout( new GridLayout(2,2,5,5) );
    TitledBorder border=new TitledBorder(JAPMessages.getString("ownTrafficBorder"));
    border.setTitleFont(fontControls);
		ownTrafficPanel.setBorder( border );
    JLabel l=new JLabel(JAPMessages.getString("ownTrafficChannels"));
    l.setFont(fontControls);
		ownTrafficPanel.add(l);
		ownTrafficPanel.add(ownTrafficChannelsProgressBar);
    l=new JLabel(JAPMessages.getString("ownTrafficBytes"));
	  l.setFont(fontControls);
		ownTrafficPanel.add(l );
		ownTrafficPanel.add(m_labelOwnTrafficBytes);

		ano1CheckBox = new JCheckBox(JAPMessages.getString("confActivateCheckBox"));
//		ano1CheckBox.setForeground(Color.red);
		JAPUtil.setMnemonic(ano1CheckBox,JAPMessages.getString("confActivateCheckBoxMn"));
    ano1CheckBox.setFont(fontControls);
    ano1CheckBox.addActionListener(this);

		// Line 1
		JPanel p41 = new JPanel();
		p41.setLayout(new BoxLayout(p41, BoxLayout.X_AXIS) );
		//p41.add(Box.createRigidArea(new Dimension(10,0)) );
		p41.add(ano1CheckBox );
		if(!JAPModel.isSmallDisplay())
      p41.add(Box.createRigidArea(new Dimension(5,0)) );
		p41.add(Box.createHorizontalGlue() );
		ano1B = new JButton(JAPMessages.getString("confActivateButton"));
		ano1B.setFont(fontControls);
    if(JAPModel.isSmallDisplay())
      ano1B.setMargin(JAPConstants.SMALL_BUTTON_MARGIN);
    ano1B.addActionListener(this);
		p41.add(ano1B);

		JPanel meterPanel = new JPanel();
		meterPanel.setLayout( new BorderLayout() );
    border=new TitledBorder(JAPMessages.getString("meterBorder"));
		border.setTitleFont(fontControls);
    meterPanel.setBorder( border );
		meterLabel = new JLabel(getMeterImage(-1));
		meterPanel.add(p41/*ano1CheckBox*/,BorderLayout.NORTH);
		meterPanel.add(meterLabel, BorderLayout.CENTER);

		// details panel
		JPanel detailsPanel = new JPanel();
		m_labelCascadeName = new JLabel();
    m_labelCascadeName.setFont(fontControls);
		JLabel labelMeterDetailsName    = new JLabel(JAPMessages.getString("meterDetailsName")+" ");
		labelMeterDetailsName.setFont(fontControls);
    JLabel labelMeterDetailsUser    = new JLabel(JAPMessages.getString("meterDetailsUsers")+" ");
	  labelMeterDetailsUser.setFont(fontControls);
 		JLabel labelMeterDetailsTraffic = new JLabel(JAPMessages.getString("meterDetailsTraffic")+" ");
	  labelMeterDetailsTraffic.setFont(fontControls);
    JLabel labelMeterDetailsRisk    = new JLabel(JAPMessages.getString("meterDetailsRisk")+" ");
	  labelMeterDetailsRisk.setFont(fontControls);
 		GridBagLayout g = new GridBagLayout();
		detailsPanel.setLayout( g );
    border=new TitledBorder(JAPMessages.getString("meterDetailsBorder")) ;
    border.setTitleFont(fontControls);
		detailsPanel.setBorder( border );
		GridBagConstraints c = new GridBagConstraints();
		c.anchor=c.WEST;
		c.fill=GridBagConstraints.HORIZONTAL;
		Insets normInsets = new Insets(0,0,8,0);
		c.insets=normInsets;
		c.gridwidth=1;
		c.weightx=0;
		c.weighty=1;
		c.gridx=0;
		c.gridy=0;
		g.setConstraints(labelMeterDetailsName,c);
		detailsPanel.add(labelMeterDetailsName);
		c.gridx=1;
		c.weightx=1;
		g.setConstraints(m_labelCascadeName,c);
		detailsPanel.add(m_labelCascadeName);
		c.weightx=0;
		c.gridx=0;
		c.gridy=1;
		g.setConstraints(labelMeterDetailsUser,c);
		detailsPanel.add(labelMeterDetailsUser);
		c.gridx=1;
		c.weightx=1;
		g.setConstraints(userProgressBar,c);
		detailsPanel.add(userProgressBar);
		c.gridx=0;
		c.gridy=2;
		c.weightx=0;
		g.setConstraints(labelMeterDetailsTraffic,c);
		detailsPanel.add(labelMeterDetailsTraffic);
		c.gridx=1;
		c.weightx=1;
		g.setConstraints(trafficProgressBar,c);
		detailsPanel.add(trafficProgressBar);
		normInsets = new Insets(0,0,0,0);
		c.insets=normInsets;
		c.gridx=0;
		c.gridy=3;
		g.setConstraints(labelMeterDetailsRisk,c);
//		detailsPanel.add(labelMeterDetailsRisk);
		c.gridx=1;
		g.setConstraints(protectionProgressBar,c);
//		detailsPanel.add(protectionProgressBar);

		// Add all panels to level panel
		levelPanel.add(ownTrafficPanel, BorderLayout.NORTH);
		levelPanel.add(meterPanel, BorderLayout.CENTER);
		levelPanel.add(detailsPanel, BorderLayout.SOUTH);

		return levelPanel;
    }

	/*private JPanel buildConfigPanel() {
		// "Center" Panel
		JPanel mainPanel = new JPanel();
		mainPanel.setLayout(new BoxLayout(mainPanel, BoxLayout.Y_AXIS) );
		//mainPanel.setBackground(Color.white);

		// Listen on Port
		JPanel portPanel = new JPanel();
		portPanel.setLayout(new GridLayout(2,1) );
		portPanel.setBorder( new TitledBorder(JAPMessages.getString("confListenerBorder")) );
		// Line 1
		JPanel p11 = new JPanel();
		p11.setLayout(new BoxLayout(p11, BoxLayout.X_AXIS) );
		p11.add(Box.createRigidArea(new Dimension(10,0)) );
		p11.add(new JLabel(JAPMessages.getString("confPort")) );
		p11.add(Box.createRigidArea(new Dimension(5,0)) );
		m_labelProxyPort = new JLabel(String.valueOf(JAPModel.getHttpListenerPortNumber()));
//		m_labelProxyPort.setForeground(Color.black);
		p11.add(m_labelProxyPort );
		p11.add(Box.createRigidArea(new Dimension(5,0)) );
		p11.add(Box.createHorizontalGlue() );
		portB = new JButton(JAPMessages.getString("confPortButton"));
		portB.addActionListener(this);
		p11.add(portB);
		// Line 2
		JPanel p12 = new JPanel();
		p12.setLayout(new BoxLayout(p12, BoxLayout.X_AXIS) );
		p12.add(Box.createRigidArea(new Dimension(10,0)) );
		p12.add(new JLabel(JAPMessages.getString("confStatus1")) );
		p12.add(Box.createRigidArea(new Dimension(5,0)) );
		// add to portPanel
		portPanel.add(p11);
		portPanel.add(p12);
		// add to mainPanel
		mainPanel.add(portPanel);

		// HTTP Proxy
		JPanel proxyPanel = new JPanel();
		proxyPanel.setLayout(new GridLayout(2,1) );
		proxyPanel.setBorder( new TitledBorder(JAPMessages.getString("confProxyBorder")) );
		// Line 1
		JPanel p21 = new JPanel();
		p21.setLayout(new BoxLayout(p21, BoxLayout.X_AXIS) );
		p21.add(Box.createRigidArea(new Dimension(10,0)) );
		proxyMustUseLabel = new JLabel();
		p21.add(proxyMustUseLabel);
		p21.add(Box.createRigidArea(new Dimension(5,0)) );
		p21.add(Box.createHorizontalGlue() );
		httpB = new JButton(JAPMessages.getString("confProxyButton"));
		httpB.addActionListener(this);
		p21.add(httpB);
		// Line 2
		JPanel p22 = new JPanel();
		p22.setLayout(new BoxLayout(p22, BoxLayout.X_AXIS) );
		p22.add(Box.createRigidArea(new Dimension(10,0)) );
//		p22.add(new JLabel(JAPMessages.getString("confProxyHost")) );
//		p22.add(Box.createRigidArea(new Dimension(5,0)) );
		m_labelProxyHost = new JLabel();
		p22.add(m_labelProxyHost);
		// add to proxypanel
		proxyPanel.add(p21);
		proxyPanel.add(p22);
		// add to mainPanel
		mainPanel.add(proxyPanel);

		// Information Service
		JPanel infoServicePanel = new JPanel();
		infoServicePanel.setLayout(new GridLayout(1,1) );
		infoServicePanel.setBorder( new TitledBorder(JAPMessages.getString("confInfoServiceBorder")) );
		// Line 1
		JPanel p31 = new JPanel();
		p31.setLayout(new BoxLayout(p31, BoxLayout.X_AXIS) );
		p31.add(Box.createRigidArea(new Dimension(10,0)) );
		p31.add(new JLabel(JAPMessages.getString("confInfoServiceHost")) );
		p31.add(Box.createRigidArea(new Dimension(5,0)) );
		infoServiceTextField = new JLabel();
		p31.add(infoServiceTextField);
		p31.add(Box.createRigidArea(new Dimension(5,0)) );
		p31.add(Box.createHorizontalGlue() );
		isB = new JButton(JAPMessages.getString("confInfoServiceButton"));
		isB.addActionListener(this);
		p31.add(isB);
		// add to infoServicePanel
		infoServicePanel.add(p31);
		// add to mainPanel
		mainPanel.add(infoServicePanel);

		// Activate Anonymity
		JPanel activatePanel = new JPanel();
		activatePanel.setLayout(new GridLayout(4,1) );
		activatePanel.setBorder( new TitledBorder(JAPMessages.getString("confActivateBorder")) );
		// Line 1
		JPanel p41 = new JPanel();
		p41.setLayout(new BoxLayout(p41, BoxLayout.X_AXIS) );
		//p41.add(Box.createRigidArea(new Dimension(10,0)) );
		anonCheckBox = new JCheckBox(JAPMessages.getString("confActivateCheckBox"));
//		anonCheckBox.setForeground(Color.red);
		JAPUtil.setMnemonic(anonCheckBox,JAPMessages.getString("confActivateCheckBoxMn"));
		anonCheckBox.addActionListener(this);
		p41.add(anonCheckBox );
		p41.add(Box.createRigidArea(new Dimension(5,0)) );
		p41.add(Box.createHorizontalGlue() );
		anonB = new JButton(JAPMessages.getString("confActivateButton"));
		anonB.addActionListener(this);
		p41.add(anonB);
		// Line 2
		JPanel p42 = new JPanel();
		p42.setLayout(new BoxLayout(p42, BoxLayout.X_AXIS) );
		p42.add(Box.createRigidArea(new Dimension(10,0)) );
		p42.add(new JLabel(JAPMessages.getString("confAnonHost")) );
		p42.add(Box.createRigidArea(new Dimension(5,0)) );
		anonTextField = new JLabel();
		p42.add(anonTextField);
		// Line 3
		JPanel p43 = new JPanel();
		p43.setLayout(new BoxLayout(p43, BoxLayout.X_AXIS) );
		p43.add(Box.createRigidArea(new Dimension(10,0)) );
		p43.add(new JLabel(JAPMessages.getString("confStatus2")) );
		p43.add(Box.createRigidArea(new Dimension(5,0)) );
		// Line 4
		JPanel p44 = new JPanel();
		p44.setLayout(new BoxLayout(p44, BoxLayout.X_AXIS) );
		p44.add(Box.createRigidArea(new Dimension(10,0)) );
		p44.add(new JLabel(JAPMessages.getString("confAnonName")) );
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
*/
	/**
	 * Used to disable activation on JAP
	 * Example: Activation of listener failed
	 *          --> disable activation checkboxes
	 */
	public void disableSetAnonMode()
		{
			//anonCheckBox.setEnabled(false);
			ano1CheckBox.setEnabled(false);
		}

	protected void loadMeterIcons() {
		// Load Images for "Anonymity Meter"
		meterIcons = new ImageIcon [JAPConstants.METERFNARRAY.length];
//		JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"JAPView:METERFNARRAY.length="+JAPConstants.METERFNARRAY.length);
    if(!JAPModel.isSmallDisplay())
      {
        for (int i=0; i<JAPConstants.METERFNARRAY.length; i++)
          {
            meterIcons[i] = JAPUtil.loadImageIcon(JAPConstants.METERFNARRAY[i],false);
          }
      }
    else
      {
        MediaTracker m=new MediaTracker(this);
        for (int i=0; i<JAPConstants.METERFNARRAY.length; i++)
          {
            Image tmp=JAPUtil.loadImageIcon(JAPConstants.METERFNARRAY[i],true).getImage();
            int w=tmp.getWidth(null);
            tmp=tmp.getScaledInstance((int)(w*0.75),-1,Image.SCALE_SMOOTH);
            m.addImage(tmp,i);
            meterIcons[i] = new ImageIcon(tmp);
          }
        try{m.waitForAll();}catch(Exception e){}
      }
	}

	 /**Anon Level is >=0 amd <=5. if -1 no measure is available*/
    private ImageIcon getMeterImage(int iAnonLevel)
			{
				if (controller.getAnonMode())
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
					if(viewIconified!=null) {
						this.setVisible(false);
						viewIconified.setVisible(true);
					}
				}
				else if (event.getSource() == confB)
					showConfigDialog();
				/*else if (event.getSource() == portB)
					showConfigDialog(JAPConf.PORT_TAB);
				else if (event.getSource() == httpB)
					showConfigDialog(JAPConf.HTTP_TAB);
				else if (event.getSource() == isB)
					showConfigDialog(JAPConf.INFO_TAB);
				else if (event.getSource() == anonB)
					showConfigDialog(JAPConf.ANON_TAB);
				*/else if (event.getSource() == ano1B)
					showConfigDialog(JAPConf.ANON_TAB);
				else if (event.getSource() == infoB)
					controller.aboutJAP();
				else if (event.getSource() == helpB)
					showHelpWindow();
				//else if (event.getSource() == anonCheckBox)
				//	controller.setAnonMode(anonCheckBox.isSelected());
				else if (event.getSource() == ano1CheckBox)
					controller.setAnonMode(ano1CheckBox.isSelected());
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
		controller.goodBye(); // call the final exit procedure of JAP
	}

  public JPanel getMainPanel()
    {
      return m_panelMain;
    }

  private void updateValues() {
		AnonServer e = controller.getAnonServer();
		// Config panel
		JAPDebug.out(JAPDebug.DEBUG,JAPDebug.GUI,"JAPView:Start updateValues");
		//m_labelProxyPort.setText(String.valueOf(JAPModel.getHttpListenerPortNumber()));
		//if(JAPModel.getUseFirewall())
			//{
			  //proxyMustUseLabel.setText(JAPMessages.getString("firewallMustUse"));
			  //m_labelProxyHost.setVisible(true);
				//int firewallPort=JAPModel.getFirewallPort();
				//if(firewallPort==-1)
					//m_labelProxyHost.setText(JAPMessages.getString("firewallNotConfigured"));
				//else
					//m_labelProxyHost.setText(JAPMessages.getString("confProxyHost")+" "+JAPModel.getFirewallHost()+":"+String.valueOf(firewallPort));
			//}
		//else
			//{
			  //proxyMustUseLabel.setText(JAPMessages.getString("firewallMustNotUse"));
			  //m_labelProxyHost.setVisible(false);
			//}
		//infoServiceTextField.setText(JAPModel.getInfoServiceHost()+":"+String.valueOf(JAPModel.getInfoServicePort()));
		//anonTextField.setText(e.getHost()+":"+String.valueOf(e.getPort())+((e.getSSLPort()==-1)?"":":"+e.getSSLPort()));
		//anonNameTextField.setText(e.getName());
		//anonNameTextField.setToolTipText(e.getName());
    //anonCheckBox.setSelected(controller.getAnonMode());
		//if(controller.getAnonMode()) {
		//	anonCheckBox.setForeground(Color.black);
		//} else {
		//	anonCheckBox.setForeground(Color.red);
		//}

		// Meter panel
		ano1CheckBox.setSelected(controller.getAnonMode());
		if(controller.getAnonMode()) {
			ano1CheckBox.setForeground(Color.black);
		} else {
			ano1CheckBox.setForeground(Color.red);
		}
		m_labelCascadeName.setText(e.getName());
		m_labelCascadeName.setToolTipText(e.getName());
		meterLabel.setIcon(getMeterImage(e.getAnonLevel()));
		if (controller.getAnonMode()) {
				if (e.getNrOfActiveUsers() > -1)
					{
						// Nr of active users
						if (e.getNrOfActiveUsers() > userProgressBar.getMaximum())
							userProgressBar.setMaximum(e.getNrOfActiveUsers());
						userProgressBar.setValue(e.getNrOfActiveUsers());
						userProgressBar.setString(String.valueOf(e.getNrOfActiveUsers()));
						if(m_bIsIconified)
							setTitle("JAP ("+Integer.toString(e.getNrOfActiveUsers())+" "+JAPMessages.getString("iconifiedviewUsers")+")");
					}
				else
					{
							userProgressBar.setValue(userProgressBar.getMaximum());
							userProgressBar.setString(JAPMessages.getString("meterNA"));
					}
				if (e.getCurrentRisk() > -1) {
					// Current Risk
					if (e.getCurrentRisk() > protectionProgressBar.getMaximum())
							protectionProgressBar.setMaximum(e.getCurrentRisk());
					protectionProgressBar.setValue(e.getCurrentRisk());
					if (e.getCurrentRisk() < 80)
						protectionProgressBar.setString(String.valueOf(e.getCurrentRisk())+" %");
					else
						protectionProgressBar.setString(JAPMessages.getString("meterRiskVeryHigh"));
				} else {
					protectionProgressBar.setValue(protectionProgressBar.getMaximum());
					protectionProgressBar.setString(JAPMessages.getString("meterNA"));
				}
        int t=e.getTrafficSituation();
				if(t>-1)
          { //Trafic Situation directly form InfoService
            trafficProgressBar.setMaximum(100);
            trafficProgressBar.setValue(t);
            if(t < 30)
              trafficProgressBar.setString(JAPMessages.getString("meterTrafficLow"));
            else if (t< 60)
              trafficProgressBar.setString(JAPMessages.getString("meterTrafficMedium"));
            else
              trafficProgressBar.setString(JAPMessages.getString("meterTrafficHigh"));
          }
        else { // no value from InfoService
					trafficProgressBar.setValue(trafficProgressBar.getMaximum());
					trafficProgressBar.setString(JAPMessages.getString("meterNA"));
				}
		} else {
			userProgressBar.setValue(userProgressBar.getMaximum());
			userProgressBar.setString(JAPMessages.getString("meterNA"));
			protectionProgressBar.setValue(protectionProgressBar.getMaximum());
			if (controller.getAnonMode())
				protectionProgressBar.setString(JAPMessages.getString("meterNA"));
			else
				protectionProgressBar.setString(JAPMessages.getString("meterRiskVeryHigh"));
			trafficProgressBar.setValue(trafficProgressBar.getMaximum());
			trafficProgressBar.setString(JAPMessages.getString("meterNA"));
		}


		}
		public void registerViewIconified(Frame v) {
			viewIconified = v;
		}
		public void channelsChanged(int c) {
			// Nr of Channels
			//int c=controller.getNrOfChannels();
			if (c > ownTrafficChannelsProgressBar.getMaximum())
				ownTrafficChannelsProgressBar.setMaximum(c);
			ownTrafficChannelsProgressBar.setValue(c);
//			ownTrafficChannelsProgressBar.setString(String.valueOf(c));
		}
		public void transferedBytes(int c) {
			// Nr of Bytes transmitted anonymously
			m_labelOwnTrafficBytes.setText(NumberFormat.getInstance().format(c)+" Bytes");
		}
		public void valuesChanged ()
		{
			synchronized(oValueUpdateSemaphore)
				{
//					JAPDebug.out(JAPDebug.DEBUG,JAPDebug.GUI,"Start valuesChanged");
					updateValues();
//					JAPDebug.out(JAPDebug.DEBUG,JAPDebug.GUI,"End valuesChanged");
				}
		}

}


