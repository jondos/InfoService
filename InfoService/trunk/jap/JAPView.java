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
package jap;

import java.text.NumberFormat;
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;
import javax.swing.border.*;
import javax.swing.plaf.basic.BasicProgressBarUI;

import anon.AnonServer;
import gui.JAPDll;

final public class JAPView extends JFrame implements ActionListener, JAPObserver {

	final private class MyProgressBarUI extends BasicProgressBarUI {
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
	}

  final private class MyViewUpdate implements Runnable
    {
      public void run()
        {
          updateValues1();
        }
    }

	private JAPController 	controller;
	private JLabel				  meterLabel;
	private JLabel	 		  	m_labelCascadeName;
	private JPanel          m_panelMain;
	private JButton				  m_bttnInfo, m_bttnHelp,m_bttnQuit, m_bttnIconify,m_bttnConf;
	private JButton         m_bttnAnonConf;
	private JCheckBox			  m_cbAnon;
	private JProgressBar 		userProgressBar;
	private JProgressBar 		trafficProgressBar;
	private JProgressBar 		protectionProgressBar;
	private JProgressBar 		ownTrafficChannelsProgressBar;
	private JLabel 				  m_labelOwnTrafficBytes,m_labelMeterDetailsName;
	private JLabel          m_labelMeterDetailsUser,m_labelMeterDetailsTraffic;
	private JLabel          m_labelMeterDetailsRisk,m_labelOwnBytes,m_labelOwnChannels;
	private TitledBorder    m_borderOwnTraffic,m_borderAnonMeter,m_borderDetails;
	private ImageIcon[]			meterIcons;
	private JAPHelp 			  helpWindow;
	private JAPConf 			  m_dlgConfig;
	private Window				  m_ViewIconified;
	private NumberFormat    m_NumberFormat;
	private MyViewUpdate    m_runnableValueUpdate;
	private boolean         m_bIsIconified;
	private String          m_Title;
	private final static boolean PROGRESSBARBORDER = true;

	public JAPView (String s)
		{
			super(s);
			m_Title=s;
			m_NumberFormat=NumberFormat.getInstance();
			controller = JAPController.getController();
			helpWindow =  null;//new JAPHelp(this);
			m_dlgConfig = null;//new JAPConf(this);
			m_bIsIconified=false;
			m_runnableValueUpdate=new MyViewUpdate();
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
			m_bttnInfo = new JButton(JAPMessages.getString("infoButton"));
			m_bttnHelp = new JButton(JAPMessages.getString("helpButton"));
			m_bttnQuit = new JButton(JAPMessages.getString("quitButton"));
			m_bttnConf = new JButton(JAPMessages.getString("confButton"));
			m_bttnIconify = new JButton(JAPUtil.loadImageIcon(JAPConstants.ICONIFYICONFN,true));
			m_bttnIconify.setToolTipText(JAPMessages.getString("iconifyWindow"));

			// Add real buttons
			buttonPanel.add(m_bttnIconify);
			buttonPanel.add(m_bttnInfo);
			buttonPanel.add(m_bttnHelp);
			buttonPanel.add(m_bttnConf);
			buttonPanel.add(new JLabel("  "));
			buttonPanel.add(m_bttnQuit);
			m_bttnIconify.addActionListener(this);
			m_bttnConf.addActionListener(this);
			m_bttnInfo.addActionListener(this);
			m_bttnHelp.addActionListener(this);
			m_bttnQuit.addActionListener(this);
			JAPUtil.setMnemonic(m_bttnIconify,JAPMessages.getString("iconifyButtonMn"));
			JAPUtil.setMnemonic(m_bttnConf,JAPMessages.getString("confButtonMn"));
			JAPUtil.setMnemonic(m_bttnInfo,JAPMessages.getString("infoButtonMn"));
			JAPUtil.setMnemonic(m_bttnHelp,JAPMessages.getString("helpButtonMn"));
			JAPUtil.setMnemonic(m_bttnQuit,JAPMessages.getString("quitButtonMn"));

			// add Components to Frame
			getContentPane().setBackground(buttonPanel.getBackground());
			getContentPane().add(level, BorderLayout.CENTER);
			m_panelMain=level;
			if(!JAPModel.isSmallDisplay())
				{
					getContentPane().add(northPanel, BorderLayout.NORTH);
					getContentPane().add(westLabel, BorderLayout.WEST);
					getContentPane().add(new JLabel("  "), BorderLayout.EAST); //Spacer
					getContentPane().add(buttonPanel, BorderLayout.SOUTH);
				}
			//tabs.setSelectedComponent(level);

			addWindowListener(new WindowAdapter()
				{
						public void windowClosing(WindowEvent e) {exitProgram();}

						public void windowDeiconified(WindowEvent e)
						{
							m_bIsIconified=false;
							setTitle(m_Title);
						}

						public void windowIconified(WindowEvent e)
						{
							setTitle(Double.toString(Math.random())); //ensure that we have an uinque title
							if(!JAPDll.hideWindowInTaskbar(getTitle()))
								setTitle(m_Title);
							m_bIsIconified=true;
							valuesChanged();
						}
				});

			setOptimalSize();
			valuesChanged();
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
		m_borderOwnTraffic=new TitledBorder(JAPMessages.getString("ownTrafficBorder"));
		m_borderOwnTraffic.setTitleFont(fontControls);
		ownTrafficPanel.setBorder(m_borderOwnTraffic);
		m_labelOwnChannels=new JLabel(JAPMessages.getString("ownTrafficChannels"));
		m_labelOwnChannels.setFont(fontControls);
		ownTrafficPanel.add(m_labelOwnChannels);
		ownTrafficPanel.add(ownTrafficChannelsProgressBar);
		m_labelOwnBytes=new JLabel(JAPMessages.getString("ownTrafficBytes"));
		m_labelOwnBytes.setFont(fontControls);
		ownTrafficPanel.add(m_labelOwnBytes);
		ownTrafficPanel.add(m_labelOwnTrafficBytes);

		m_cbAnon = new JCheckBox(JAPMessages.getString("confActivateCheckBox"));
		JAPUtil.setMnemonic(m_cbAnon,JAPMessages.getString("confActivateCheckBoxMn"));
		m_cbAnon.setFont(fontControls);
		m_cbAnon.addActionListener(this);

		// Line 1
		JPanel p41 = new JPanel();
		p41.setLayout(new BoxLayout(p41, BoxLayout.X_AXIS) );
		//p41.add(Box.createRigidArea(new Dimension(10,0)) );
		p41.add(m_cbAnon );
		if(!JAPModel.isSmallDisplay())
			p41.add(Box.createRigidArea(new Dimension(5,0)) );
		p41.add(Box.createHorizontalGlue() );
		m_bttnAnonConf = new JButton(JAPMessages.getString("confActivateButton"));
		m_bttnAnonConf.setFont(fontControls);
		if(JAPModel.isSmallDisplay())
			m_bttnAnonConf.setMargin(JAPConstants.SMALL_BUTTON_MARGIN);
		m_bttnAnonConf.addActionListener(this);
		p41.add(m_bttnAnonConf);

		JPanel meterPanel = new JPanel();
		meterPanel.setLayout( new BorderLayout() );
		m_borderAnonMeter=new TitledBorder(JAPMessages.getString("meterBorder"));
		m_borderAnonMeter.setTitleFont(fontControls);
		meterPanel.setBorder(m_borderAnonMeter);
		meterLabel = new JLabel(getMeterImage(-1));
		meterPanel.add(p41/*ano1CheckBox*/,BorderLayout.NORTH);
		meterPanel.add(meterLabel, BorderLayout.CENTER);

		// details panel
		JPanel detailsPanel = new JPanel();
		m_labelCascadeName = new JLabel();
		m_labelCascadeName.setFont(fontControls);
		m_labelMeterDetailsName    = new JLabel(JAPMessages.getString("meterDetailsName")+" ");
		m_labelMeterDetailsName.setFont(fontControls);
		m_labelMeterDetailsUser    = new JLabel(JAPMessages.getString("meterDetailsUsers")+" ");
		m_labelMeterDetailsUser.setFont(fontControls);
		m_labelMeterDetailsTraffic = new JLabel(JAPMessages.getString("meterDetailsTraffic")+" ");
		m_labelMeterDetailsTraffic.setFont(fontControls);
		m_labelMeterDetailsRisk    = new JLabel(JAPMessages.getString("meterDetailsRisk")+" ");
		m_labelMeterDetailsRisk.setFont(fontControls);
		GridBagLayout g = new GridBagLayout();
		detailsPanel.setLayout( g );
		m_borderDetails=new TitledBorder(JAPMessages.getString("meterDetailsBorder")) ;
		m_borderDetails.setTitleFont(fontControls);
		detailsPanel.setBorder(m_borderDetails);
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
		g.setConstraints(m_labelMeterDetailsName,c);
		detailsPanel.add(m_labelMeterDetailsName);
		c.gridx=1;
		c.weightx=1;
		g.setConstraints(m_labelCascadeName,c);
		detailsPanel.add(m_labelCascadeName);
		c.weightx=0;
		c.gridx=0;
		c.gridy=1;
		g.setConstraints(m_labelMeterDetailsUser,c);
		detailsPanel.add(m_labelMeterDetailsUser);
		c.gridx=1;
		c.weightx=1;
		g.setConstraints(userProgressBar,c);
		detailsPanel.add(userProgressBar);
		c.gridx=0;
		c.gridy=2;
		c.weightx=0;
		g.setConstraints(m_labelMeterDetailsTraffic,c);
		detailsPanel.add(m_labelMeterDetailsTraffic);
		c.gridx=1;
		c.weightx=1;
		g.setConstraints(trafficProgressBar,c);
		detailsPanel.add(trafficProgressBar);
		normInsets = new Insets(0,0,0,0);
		c.insets=normInsets;
		c.gridx=0;
		c.gridy=3;
		g.setConstraints(m_labelMeterDetailsRisk,c);
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
			m_cbAnon.setEnabled(false);
		}

	 /** Used to notice the View, that the locale has Changed.
		*
		*/
	public void localeChanged()
		{
			m_bttnInfo.setText(JAPMessages.getString("infoButton"));
			m_bttnHelp.setText(JAPMessages.getString("helpButton"));
			m_bttnQuit.setText(JAPMessages.getString("quitButton"));
			m_bttnConf.setText(JAPMessages.getString("confButton"));
			JAPUtil.setMnemonic(m_bttnConf,JAPMessages.getString("confButtonMn"));
			JAPUtil.setMnemonic(m_bttnInfo,JAPMessages.getString("infoButtonMn"));
			JAPUtil.setMnemonic(m_bttnHelp,JAPMessages.getString("helpButtonMn"));
			JAPUtil.setMnemonic(m_bttnQuit,JAPMessages.getString("quitButtonMn"));
			m_labelMeterDetailsName.setText(JAPMessages.getString("meterDetailsName")+" ");
			m_labelMeterDetailsUser.setText(JAPMessages.getString("meterDetailsUsers")+" ");
			m_labelMeterDetailsTraffic.setText(JAPMessages.getString("meterDetailsTraffic")+" ");
			m_labelMeterDetailsRisk.setText(JAPMessages.getString("meterDetailsRisk")+" ");
			m_borderOwnTraffic.setTitle(JAPMessages.getString("ownTrafficBorder"));
			m_labelOwnChannels.setText(JAPMessages.getString("ownTrafficChannels"));
			m_labelOwnBytes.setText(JAPMessages.getString("ownTrafficBytes"));
			m_cbAnon.setText(JAPMessages.getString("confActivateCheckBox"));
			JAPUtil.setMnemonic(m_cbAnon,JAPMessages.getString("confActivateCheckBoxMn"));
			m_borderAnonMeter.setTitle(JAPMessages.getString("meterBorder"));
			m_bttnAnonConf.setText(JAPMessages.getString("confActivateButton"));
			m_borderDetails.setTitle(JAPMessages.getString("meterDetailsBorder")) ;
			if(m_dlgConfig!=null)
				m_dlgConfig.localeChanged();
			m_NumberFormat=NumberFormat.getInstance();
			valuesChanged();
			setOptimalSize();
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

	 /**Anon Level is >=0 amd <=5. If -1 no measure is available*/
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
				if (event.getSource() == m_bttnQuit)
					exitProgram();
				else if (event.getSource() == m_bttnIconify)
					{
						if(m_ViewIconified!=null)
							{
								setVisible(false);
								m_ViewIconified.setVisible(true);
								m_ViewIconified.toFront();
							}
					}
				else if (event.getSource() == m_bttnConf)
					showConfigDialog();
				/*else if (event.getSource() == portB)
					showConfigDialog(JAPConf.PORT_TAB);
				else if (event.getSource() == httpB)
					showConfigDialog(JAPConf.HTTP_TAB);
				else if (event.getSource() == isB)
					showConfigDialog(JAPConf.INFO_TAB);
				else if (event.getSource() == anonB)
					showConfigDialog(JAPConf.ANON_TAB);
				*/else if (event.getSource() == m_bttnAnonConf)
					showConfigDialog(JAPConf.ANON_TAB);
				else if (event.getSource() == m_bttnInfo)
					controller.aboutJAP();
				else if (event.getSource() == m_bttnHelp)
					showHelpWindow();
				//else if (event.getSource() == anonCheckBox)
				//	controller.setAnonMode(anonCheckBox.isSelected());
				else if (event.getSource() == m_cbAnon)
					controller.setAnonMode(m_cbAnon.isSelected());
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
		if(m_dlgConfig==null)
			{
				Cursor c=getCursor();
				setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				m_dlgConfig=new JAPConf(this);
				setCursor(c);
			}
		if (card!=-1)
			m_dlgConfig.selectCard(card);
		m_dlgConfig.updateValues();
		m_dlgConfig.show();
	}

	private void exitProgram() {
		controller.goodBye(); // call the final exit procedure of JAP
	}

	public JPanel getMainPanel()
		{
			return m_panelMain;
		}

		private void setOptimalSize()
			{
				try
				{
					pack();  // optimize size
					setResizable(/*true*/true/*false*/); //2001-11-12(HF):Changed due to a Mac OS X problem during redraw of the progress bars
				}
				catch(Exception e) {
					JAPDebug.out(JAPDebug.EXCEPTION,JAPDebug.GUI,"JAPView:Hm.. Error by Pack - Has To be fixed!!");
				}
			}

	private void updateValues1() {
  			synchronized(m_runnableValueUpdate)
{
		AnonServer e = controller.getAnonServer();
		// Config panel
		JAPDebug.out(JAPDebug.DEBUG,JAPDebug.GUI,"JAPView:Start updateValues");

		// Meter panel
    try{
		m_cbAnon.setSelected(controller.getAnonMode());
		if(controller.getAnonMode()) {
			m_cbAnon.setForeground(Color.black);
		} else {
			m_cbAnon.setForeground(Color.red);
		}
		JAPDebug.out(JAPDebug.DEBUG,JAPDebug.GUI,"JAPView: update CascadeName");

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
			JAPDebug.out(JAPDebug.DEBUG,JAPDebug.GUI,"JAPView:Finished updateValues");
		}
}
catch(Throwable t)
{
			JAPDebug.out(JAPDebug.EMERG,JAPDebug.GUI,"JAPVIew: Ooops... Crash in updateValues(): "+t.getMessage());
}
}
		}
		public void registerViewIconified(Window v) {
			m_ViewIconified = v;
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
			m_labelOwnTrafficBytes.setText(m_NumberFormat.format(c)+" Bytes");
			JAPDll.onTraffic();
		}

  public void valuesChanged ()
    {
			synchronized(m_runnableValueUpdate)
				{
					if(SwingUtilities.isEventDispatchThread())
            updateValues1();
          else
            SwingUtilities.invokeLater(m_runnableValueUpdate);
				}
		}
}


