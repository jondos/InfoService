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
//import java.awt.BorderLayout;
//import java.awt.Color;
import java.awt.Window;
import java.awt.Frame;
import java.awt.Font;
import java.awt.Image;
import java.awt.Toolkit;
import java.awt.Graphics;
import java.awt.FontMetrics;
import java.awt.MediaTracker;
import java.io.InputStream;
//import javax.swing.JLabel;
//import javax.swing.JPanel;
//import javax.swing.ImageIcon;

final class JAPSplash extends Window  
	{
		Image imageSplash;
		Image imageBusy;
		Image imageOffScreen=null;
		Font font;
		String strLoading;
		String strVersion;
		int xVersion;		
		int yVersion;

		JAPSplash(Frame parent)
			{
				super(parent);
				setLayout(null);
		    xVersion=yVersion=100;
				Toolkit t=Toolkit.getDefaultToolkit();
				MediaTracker ma=new MediaTracker(this);
				InputStream in=JAPSplash.class.getResourceAsStream("images/splash.gif");
				int len;
				int aktIndex;
				if(in!=null)
					{
						byte[] buff=new byte[26000];
						len=0;
						aktIndex=0;
						try
							{
								while((len=in.read(buff,aktIndex,26000-aktIndex))>0)
									aktIndex+=len;
								imageSplash=t.createImage(buff,0,aktIndex);
								ma.addImage(imageSplash,1);
								ma.checkID(1,true);
							}
						catch(Exception e)
							{
							}
					}
				in=JAPSplash.class.getResourceAsStream("images/busy.gif");
				if(in!=null)
					{
						byte[] buff1=new byte[7000];
						len=0;
						aktIndex=0;
						try
							{
								while((len=in.read(buff1,aktIndex,7000-aktIndex))>0)
									aktIndex+=len;
								imageBusy=t.createImage(buff1,0,aktIndex);
								ma.addImage(imageBusy,2);
								ma.checkID(2,true);
							}
						catch(Exception e)
							{
							}
					}
				strLoading=JAPMessages.getString("loading");
				strVersion="Version: "+JAPModel.aktVersion;
				font=new Font("Sans",Font.PLAIN,9);
				FontMetrics fontmetrics=t.getFontMetrics(font);
				xVersion=350-10-fontmetrics.stringWidth(strVersion);
				yVersion=158;
				setLocation(-350,-173);
				setSize(350,173);
				try{ma.waitForAll();}catch(Exception e){};
				setVisible(true);
				toFront();
//				paint(getGraphics());
				//setSize(350,173);
				JAPUtil.centerFrame(this);
//				System.out.println("----------------------- SPLASH FINISHED -----------------------");
			}
		
		public void update(Graphics g)
			{
				paint(g);
			}
		
		public void paint(Graphics g)
			{
				if(imageOffScreen==null)	
					imageOffScreen=createImage(350,173);
				Graphics goff=imageOffScreen.getGraphics();
				if(imageSplash!=null)
					goff.drawImage(imageSplash,0,0,this);
				if(imageBusy!=null)
					goff.drawImage(imageBusy,15,150,this);
				goff.setFont(font);
				goff.drawString(strLoading,17,140);
				goff.drawString(strVersion,xVersion,yVersion);
				g.drawImage(imageOffScreen,0,0,this);
			}

		/*public  JAPSplash (int i) {
//		super(new Frame());
//		setBackground(Color.black);
//		Toolkit t=Toolkit.getDefaultToolkit();
//		InputStream in=this.class.getRessourceAsStream("images/splash.gif");
//		byte buff=new byte[65000];
//		int len=0;
//		while((len=in.read(buff,aktIndex,65000-aktIndex))!=0)
//			aktIndex+=len;
//		Image image=t.createImage(buff,0,aktIndex);
		setLayout(null);
		add(new SplashCanvas());
		//JLabel splashLabel = new JLabel(JAPUtil.loadImageIcon(m.SPLASHFN, false));
		
		//JPanel p1 = new JPanel();
		//p1.setBackground(Color.black);
		//p1.setForeground(Color.white);

		//JLabel waitLabel   = new JLabel(m.getString("loading"), JLabel.CENTER);
	//	waitLabel.setOpaque(false);
		//waitLabel.setBackground(Color.black);
		//waitLabel.setForeground(Color.white);
		//waitLabel.setFont(new Font("Sans",Font.PLAIN,9));

		//ImageIcon busy = JAPUtil.loadImageIcon(m.BUSYFN, false);
		//JLabel busyLabel = new JLabel(busy);
		//busyLabel.setBackground(Color.black);
		
		//p1.add(busyLabel);
		//p1.add(waitLabel);

		//add(splashLabel, BorderLayout.NORTH);
		//add(p1, BorderLayout.CENTER);

		//pack();
		setSize(350,173);
		JAPUtil.centerFrame(this);
		setVisible(true);
		toFront();
		System.out.println("----------------------- SPLASH FINISHED -----------------------");
	}*/
}


