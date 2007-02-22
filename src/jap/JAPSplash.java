/*
 Copyright (c) 2000-2006, The JAP-Team
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

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics;
import java.awt.Image;
import java.awt.MediaTracker;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.Frame;
import java.awt.image.ColorModel;

final public class JAPSplash extends Window implements ISplashResponse
{
	private static final String IMGPATHHICOLOR = "images/";
	private static final String IMGPATHLOWCOLOR = "images/lowcolor/";
	private static final String SPLASHFN = "splash.jpg";

	private Image m_imgSplash;
	private Image m_imgBusy;
	private Image m_imgOffScreen = null;
	private Font m_fntFont;
	private String m_strLoading;
	private String m_strVersion;
	private int m_iXVersion;
	private int m_iYVersion;

	public JAPSplash(Frame a_frmParent)
	{
		this (a_frmParent, null);
	}

	public JAPSplash(Frame frmParent, String a_message)
	{
		super(frmParent);
		setLayout(null);
		m_iXVersion = m_iYVersion = 100;
		Toolkit t = Toolkit.getDefaultToolkit();
		MediaTracker ma = new MediaTracker(this);
		InputStream in = null;
		Class c = null;
		boolean bLowColorDisplay=false;
		try
		{
			c = Class.forName("JAP");
		}
		catch (Exception e)
		{
		}
		ColorModel colorModel=null;
		try{//Do not remove the try-catch block as some faulty JRE throw a null pointer excpetion in getColorModel()
			colorModel = t.getColorModel();
		}catch(Throwable t1)
		{

		}

		if (colorModel==null||colorModel.getPixelSize() <= 16)
		{
			bLowColorDisplay=true;
			in = c.getResourceAsStream(IMGPATHLOWCOLOR + SPLASHFN);
			if (in == null)
			{
				try
				{
					in = new FileInputStream(IMGPATHLOWCOLOR + SPLASHFN);
				}
				catch (FileNotFoundException ex)
				{
				}
			}
		}
		if (in == null)
		{
			in = c.getResourceAsStream(IMGPATHHICOLOR + SPLASHFN);
		}
		if (in == null)
		{
			try
			{
				in = new FileInputStream(IMGPATHHICOLOR + SPLASHFN);
			}
			catch (FileNotFoundException ex)
			{
			}
		}
		int len;
		int aktIndex;
		if (in != null)
		{
			byte[] buff = new byte[27000];
			len = 0;
			aktIndex = 0;
			try
			{
				while ( (len = in.read(buff, aktIndex, 27000 - aktIndex)) > 0)
				{
					aktIndex += len;
				}
				m_imgSplash = t.createImage(buff, 0, aktIndex);
				ma.addImage(m_imgSplash, 1);
				ma.checkID(1, true);
			}
			catch (Exception e)
			{
			}
		}
		in = null;
		if (bLowColorDisplay)
		{
			in = c.getResourceAsStream(IMGPATHLOWCOLOR + JAPConstants.BUSYFN);
			if (in == null)
			{
				try
				{
					in = new FileInputStream(IMGPATHLOWCOLOR + JAPConstants.BUSYFN);
				}
				catch (FileNotFoundException ex)
				{
				}
			}
		}
		if (in == null)
		{
			in = c.getResourceAsStream(IMGPATHHICOLOR + JAPConstants.BUSYFN);
		}
		if (in == null)
		{
			try
			{
				in = new FileInputStream(IMGPATHHICOLOR + JAPConstants.BUSYFN);
			}
			catch (FileNotFoundException ex)
			{
			}
		}
		if (in != null)
		{
			byte[] buff1 = new byte[7000];
			len = 0;
			aktIndex = 0;
			try
			{
				while ( (len = in.read(buff1, aktIndex, 7000 - aktIndex)) > 0)
				{
					aktIndex += len;
				}
				m_imgBusy = t.createImage(buff1, 0, aktIndex);
				ma.addImage(m_imgBusy, 2);
				ma.checkID(2, true);
			}
			catch (Exception e)
			{
			}
		}

		if (a_message == null || a_message.trim().length() == 0)
		{
			setText("Busy");
		}
		else
		{
			setText(a_message);
		}

		m_strVersion = "Version: " + JAPConstants.aktVersion;
		m_fntFont = new Font("Sans", Font.PLAIN, 9);
		FontMetrics fontmetrics = t.getFontMetrics(m_fntFont);
		m_iXVersion = 457 - 10 - fontmetrics.stringWidth(m_strVersion);
		m_iYVersion = 178 - 15;
		//setLocation( -350, -173);
		//setSize(350, 173);
		setSize(457, 178);
	 try
		{
			ma.waitForAll();
		}
		catch (Exception e)
		{}
		;
		toFront();

}

	public void setText(String a_text)
	{
		if (a_text != null && a_text.trim().length() > 0)
		{
			m_strLoading = a_text + "...";
		}
	}

	public void update(Graphics g)
	{
		paint(g);
	}

	public void paint(Graphics g)
	{
		if (m_imgOffScreen == null)
		{
			m_imgOffScreen = createImage(457, 178);
		}
		Graphics goff = m_imgOffScreen.getGraphics();
		if (m_imgSplash != null)
		{
			goff.drawImage(m_imgSplash, 0, 0, this);
		}
		if (m_imgBusy != null)
		{
			goff.drawImage(m_imgBusy, 15, 150, this);
		}
		goff.setColor(Color.gray);
		goff.drawRect(0, 0, 456, 177);
		goff.setFont(m_fntFont);
		goff.setColor(Color.black);
		goff.drawString(m_strLoading, 17, 140);
		goff.drawString(m_strVersion, m_iXVersion, m_iYVersion);
		g.drawImage(m_imgOffScreen, 0, 0, this);
	}

	public void centerOnScreen()
	{
		centerOnScreen(this);
	}

	/**
	 * Centers a window relative to the screen.
	 * @param a_window a Window
	 * @note copied form GUIUtils - because we want to have the smallest possible dependencies for JAPSplash-Screen to make it load faster
	 */
	private static void centerOnScreen(Window a_window)
	{
		Rectangle screenBounds;
		Dimension ownSize = a_window.getSize();

		try
		{
			// try to center the window on the default screen; useful if there is more than one screen
			Object graphicsEnvironment =
				Class.forName("java.awt.GraphicsEnvironment").getMethod(
						"getLocalGraphicsEnvironment", null).invoke(null, null);
			Object graphicsDevice = graphicsEnvironment.getClass().getMethod(
				 "getDefaultScreenDevice", null).invoke(graphicsEnvironment, null);
			Object graphicsConfiguration = graphicsDevice.getClass().getMethod(
				"getDefaultConfiguration", null).invoke(graphicsDevice, null);
			screenBounds = (Rectangle)graphicsConfiguration.getClass().getMethod(
				 "getBounds", null).invoke(graphicsConfiguration, null);
		}
		catch(Exception a_e)
		{
			// not all methods to get the default screen are available in JDKs < 1.3
			screenBounds = new Rectangle(new Point(0,0), a_window.getToolkit().getScreenSize());
		}

		a_window.setLocation(screenBounds.x + ((screenBounds.width - ownSize.width) / 2),
							 screenBounds.y + ((screenBounds.height - ownSize.height) / 2));
	}

}
