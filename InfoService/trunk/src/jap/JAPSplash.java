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

import java.awt.Window;
import java.awt.Frame;
import java.awt.Font;
import java.awt.Image;
import java.awt.Color;
import java.awt.Toolkit;
import java.awt.Graphics;
import java.awt.FontMetrics;
import java.awt.MediaTracker;
import java.io.InputStream;

final public class JAPSplash extends Window
	{
		private Image m_imgSplash;
		private Image m_imgBusy;
		private Image m_imgOffScreen=null;
		private Font m_fntFont;
		private String m_strLoading;
		private String m_strVersion;
		private int m_iXVersion;
		private int m_iYVersion;

		public JAPSplash(Frame frmParent)
			{
				super(frmParent);
				setLayout(null);
				m_iXVersion=m_iYVersion=100;
				Toolkit t=Toolkit.getDefaultToolkit();
				MediaTracker ma=new MediaTracker(this);
				InputStream in=null;
				Class c=null;
				try
					{
						c=Class.forName("JAP");
					}
				catch(Exception e)
					{
					}
				if(t.getColorModel().getPixelSize()<=16)
					in=c.getResourceAsStream(JAPConstants.IMGPATHLOWCOLOR+JAPConstants.SPLASHFN);
				if(in==null)
					in=c.getResourceAsStream(JAPConstants.IMGPATHHICOLOR+JAPConstants.SPLASHFN);
				int len;
				int aktIndex;
				if(in!=null)
					{
						byte[] buff=new byte[27000];
						len=0;
						aktIndex=0;
						try
							{
								while((len=in.read(buff,aktIndex,27000-aktIndex))>0)
									aktIndex+=len;
								m_imgSplash=t.createImage(buff,0,aktIndex);
								ma.addImage(m_imgSplash,1);
								ma.checkID(1,true);
							}
						catch(Exception e)
							{
							}
					}
				in=null;
				if(t.getColorModel().getPixelSize()<=16)
					in=c.getResourceAsStream(JAPConstants.IMGPATHLOWCOLOR+JAPConstants.BUSYFN);
				if(in==null)
					in=c.getResourceAsStream(JAPConstants.IMGPATHHICOLOR+JAPConstants.BUSYFN);
				if(in!=null)
					{
						byte[] buff1=new byte[7000];
						len=0;
						aktIndex=0;
						try
							{
								while((len=in.read(buff1,aktIndex,7000-aktIndex))>0)
									aktIndex+=len;
								m_imgBusy=t.createImage(buff1,0,aktIndex);
								ma.addImage(m_imgBusy,2);
								ma.checkID(2,true);
							}
						catch(Exception e)
							{
							}
					}
				m_strLoading=JAPMessages.getString("loading");
				m_strVersion="Version: "+JAPConstants.aktVersion;
				m_fntFont=new Font("Sans",Font.PLAIN,9);
				FontMetrics fontmetrics=t.getFontMetrics(m_fntFont);
				m_iXVersion=350-10-fontmetrics.stringWidth(m_strVersion);
				m_iYVersion=158;
				setLocation(-350,-173);
				setSize(350,173);
				try{ma.waitForAll();}catch(Exception e){};
				setVisible(true);
				toFront();
				JAPUtil.centerFrame(this);
			}

		public void update(Graphics g)
			{
				paint(g);
			}

		public void paint(Graphics g)
			{
				if(m_imgOffScreen==null)
					m_imgOffScreen=createImage(350,173);
				Graphics goff=m_imgOffScreen.getGraphics();
				if(m_imgSplash!=null)
					goff.drawImage(m_imgSplash,0,0,this);
				if(m_imgBusy!=null)
					goff.drawImage(m_imgBusy,15,150,this);
				goff.setFont(m_fntFont);
				goff.setColor(Color.black);
				goff.drawString(m_strLoading,17,140);
				goff.drawString(m_strVersion,m_iXVersion,m_iYVersion);
				g.drawImage(m_imgOffScreen,0,0,this);
			}

}


