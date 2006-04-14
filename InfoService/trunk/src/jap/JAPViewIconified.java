/*
 Copyright (c) 2000 - 2004, The JAP-Team
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

import java.lang.reflect.InvocationTargetException;
import java.text.NumberFormat;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Font;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseMotionListener;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.Icon;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;

import anon.infoservice.MixCascade;
import anon.infoservice.StatusInfo;
import anon.proxy.IProxyListener;
import gui.JAPDll;
import gui.JAPMessages;
import gui.GUIUtils;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

final public class JAPViewIconified extends JWindow implements ActionListener,
	MouseMotionListener,
	MouseListener,
	JAPObserver
{
	private static final Font m_fontDlg = new Font("Sans", Font.BOLD, 11);
	private static final String STR_HIDDEN_WINDOW = Double.toString(Math.random());
	private static final Frame m_frameParent = new Frame(STR_HIDDEN_WINDOW);
	private static final Icon m_iconJAPpassive = GUIUtils.loadImageIcon(JAPConstants.IICON16FN, true);
	private static final Icon m_iconJAPactive =
		GUIUtils.loadImageIcon(JAPViewIconified.class.getName() + "_icon16red.gif", true);
	private static final Icon m_iconJAPdisconnected =
		GUIUtils.loadImageIcon(JAPViewIconified.class.getName() + "_icon16discon.gif", true);

	private JAPController m_Controller;
	private AbstractJAPMainView m_mainView;
	private JLabel m_labelBytes, m_labelUsers, m_labelTraffic;
	private JLabel m_lblJAPIcon;
	private Point m_startPoint;
	private boolean m_bIsDragging = false;
	private NumberFormat m_NumberFormat;

	private long m_lTrafficWWW, m_lTrafficOther;
	final private class MyViewIconifiedUpdate implements Runnable
	{
		public void run()
		{
			updateValues1();
		}
	}

	private MyViewIconifiedUpdate m_runnableValueUpdate;

	public JAPViewIconified()
	{
		super(m_frameParent);
		setName(JAPConstants.TITLE);
		m_mainView = JAPController.getView();
		m_frameParent.setIconImage(m_mainView.getIconImage());
		LogHolder.log(LogLevel.INFO, LogType.MISC, "JAPViewIconified:initializing...");
		m_Controller = JAPController.getInstance();
		m_NumberFormat = NumberFormat.getInstance();
		m_runnableValueUpdate = new MyViewIconifiedUpdate();
		init();
	}

	public void init()
	{
		GridBagLayout la = new GridBagLayout();
		GridBagConstraints c = new GridBagConstraints();
		JPanel pTop = new JPanel(la);
		pTop.setOpaque(false);
		JLabel x2 = new JLabel(JAPMessages.getString("iconifiedviewBytes") + ": ", JLabel.RIGHT);
		x2.setFont(m_fontDlg);
		c.gridx = 0;
		c.gridy = 0;
		c.fill = GridBagConstraints.BOTH;
		c.weightx = 0;
		c.insets = new Insets(3, 3, 0, 0);
		c.anchor = GridBagConstraints.NORTHWEST;
		la.setConstraints(x2, c);
		pTop.add(x2);
		c.weightx = 1;
		m_lTrafficOther = m_lTrafficWWW = 0;
		m_labelBytes = new JLabel("000000000000  ", JLabel.LEFT);
		//m_labelBytes.setForeground(Color.red);
		m_labelBytes.setFont(m_fontDlg);
		c.gridx = 1;
		c.weightx = 0;
		la.setConstraints(m_labelBytes, c);
		pTop.add(m_labelBytes);

		JLabel x3 = new JLabel(JAPMessages.getString("iconifiedviewUsers") + ": ", JLabel.RIGHT);
		x3.setFont(m_fontDlg);
		c.gridx = 0;
		c.gridy = 1;
		la.setConstraints(x3, c);
		pTop.add(x3);

		m_labelUsers = new JLabel("", JLabel.LEFT);
		//m_labelUsers.setForeground(Color.red);
		m_labelUsers.setFont(m_fontDlg);
		c.gridx = 1;
		la.setConstraints(m_labelUsers, c);
		pTop.add(m_labelUsers);

		JLabel x4 = new JLabel(JAPMessages.getString("iconifiedviewTraffic") + ": ", JLabel.RIGHT);
		x4.setFont(m_fontDlg);
		c.gridy = 2;
		c.gridx = 0;
		la.setConstraints(x4, c);
		pTop.add(x4);
		m_labelTraffic = new JLabel("", JLabel.LEFT);
		//m_labelTraffic.setForeground(Color.red);
		m_labelTraffic.setFont(m_fontDlg);
		c.gridx = 1;
		la.setConstraints(m_labelTraffic, c);
		pTop.add(m_labelTraffic);

		JButton bttn = new JButton(GUIUtils.loadImageIcon(JAPConstants.ENLARGEYICONFN, true));
		bttn.setOpaque(false);
		bttn.addActionListener(this);
		bttn.setToolTipText(JAPMessages.getString("enlargeWindow"));
		JAPUtil.setMnemonic(bttn, JAPMessages.getString("iconifyButtonMn"));

		/*JLabel l1 = new JLabel(JAPUtil.loadImageIcon(JAPConstants.JAPEYEFN, true));
		   JPanel co = new JPanel();
		   co.add(pTop);
		   JPanel pBottom = new JPanel(new BorderLayout());
		   pBottom.setBackground(new Color(204, 204, 204));
		   pBottom.add(l1, BorderLayout.CENTER);
		   co.add(pBottom);
		   OverlayLayout lo = new OverlayLayout(co);
		   co.setLayout(lo);
		 */
		JPanel p = new JPanel(new BorderLayout());
		p.setBorder(new LineBorder(Color.black, 1));
		p.add(pTop, BorderLayout.CENTER);
		JPanel p2 = new JPanel();
		//p2.setBackground(new Color(204, 204, 204));
		m_lblJAPIcon = new JLabel(m_iconJAPdisconnected);
		m_lblJAPIcon.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent a_event)
			{
				if (a_event.getButton() == MouseEvent.BUTTON1)
				{
					m_Controller.setAnonMode(!m_Controller.getAnonMode());
				}
			}
		});
		p2.add(m_lblJAPIcon);
		p2.add(bttn);
		p.add(p2, BorderLayout.SOUTH);
		p.addMouseListener(this);
		p.addMouseMotionListener(this);
		setContentPane(p);

		pack();
		JAPUtil.upRightFrame(this);
		m_labelBytes.setText(JAPMessages.getString("iconifiedViewZero"));
		m_labelUsers.setText(JAPMessages.getString("iconifiedViewNA"));
		m_labelTraffic.setText(JAPMessages.getString("iconifiedViewNA"));
		JAPDll.setWindowOnTop(this,STR_HIDDEN_WINDOW, true);
	}

	void switchBackToMainView()
	{
		if (m_mainView == null)
		{
			return;
		}
		setVisible(false);
		m_mainView.setVisible(true);
	}

	public void actionPerformed(ActionEvent event)
	{
		switchBackToMainView();
	}

	private void updateValues1()
	{
		synchronized (m_runnableValueUpdate)
		{
			try
			{
				if (m_Controller.getAnonMode())
				{
					MixCascade currentMixCascade = m_Controller.getCurrentMixCascade();
					StatusInfo currentStatus = currentMixCascade.getCurrentStatus();
					if (currentStatus.getNrOfActiveUsers() != -1)
					{
						m_labelUsers.setText(m_NumberFormat.format(currentStatus.getNrOfActiveUsers()));
					}
					else
					{
						m_labelUsers.setText(JAPMessages.getString("iconifiedViewNA"));
					}
					int t = currentStatus.getTrafficSituation();
					if (t > -1)
					{
						if (t < 30)
						{
							m_labelTraffic.setText(JAPMessages.getString("iconifiedViewMeterTrafficLow"));
						}
						else
						{
							if (t < 60)
							{
								m_labelTraffic.setText(
									JAPMessages.getString("iconifiedViewMeterTrafficMedium"));
							}
							else
							{
								m_labelTraffic.setText(JAPMessages.getString("iconifiedViewMeterTrafficHigh"));
							}
						}
					}
					else
					{
						m_labelTraffic.setText(JAPMessages.getString("iconifiedViewNA"));
					}
					synchronized(m_lblJAPIcon)
					{
						m_lblJAPIcon.setIcon(m_iconJAPpassive);
					}
				}
				else
				{
					/* not in anonymity mode */
					m_labelUsers.setText(JAPMessages.getString("iconifiedViewNA"));
					m_labelTraffic.setText(JAPMessages.getString("iconifiedViewNA"));
					synchronized(m_lblJAPIcon)
					{
						m_lblJAPIcon.setIcon(m_iconJAPdisconnected);
					}
				}
			}
			catch (Throwable t)
			{
			}
		}
	}

	public void valuesChanged(boolean bSync)
	{
		synchronized (m_runnableValueUpdate)
		{
			if (SwingUtilities.isEventDispatchThread())
			{
				updateValues1();
			}
			else
			{
				try
				{
					if (bSync)
					{
						SwingUtilities.invokeAndWait(m_runnableValueUpdate);
					}
				}
				catch (InvocationTargetException ex)
				{
				}
				catch (InterruptedException ex)
				{
				}
				SwingUtilities.invokeLater(m_runnableValueUpdate);
			}
		}
	}

	public void channelsChanged(int c)
	{
	}


	public void transferedBytes(final long b, final int protocolType)
	{
		if (protocolType == IProxyListener.PROTOCOL_WWW)
		{
			m_lTrafficWWW = JAPModel.getInstance().getMixedBytes();
		}
		else if (protocolType == IProxyListener.PROTOCOL_OTHER)
		{
			m_lTrafficOther = b;
		}

		Runnable updateThread = new Runnable()
		{
			public void run()
			{
				m_labelBytes.setText(JAPUtil.formatBytesValue(m_lTrafficWWW + m_lTrafficOther));
			}
		};

		if (SwingUtilities.isEventDispatchThread())
		{
			updateThread.run();
		}
		else
		{
			SwingUtilities.invokeLater(updateThread);
		}

		blink();
	}

	public void mouseExited(MouseEvent e)
	{
	}

	public void mouseEntered(MouseEvent e)
	{
	}

	public void mouseReleased(MouseEvent e)
	{
		m_bIsDragging = false;
	}

	public void mousePressed(MouseEvent e)
	{
	}

	public void mouseClicked(MouseEvent e)
	{
		if (e.getClickCount() > 1)
		{
			switchBackToMainView();
		}
	}

	public void mouseMoved(MouseEvent e)
	{
	}

	public void mouseDragged(MouseEvent e)
	{
		if (!m_bIsDragging)
		{
			m_bIsDragging = true;
			m_startPoint = e.getPoint();
		}
		else
		{
			Point endPoint = e.getPoint();
			Point aktLocation = getLocation();
			setLocation(aktLocation.x + endPoint.x - m_startPoint.x,
						aktLocation.y + endPoint.y - m_startPoint.y);
		}
	}

	/**
	 * Shows a blinking JAP icon.
	 */
	private void blink()
	{
		Thread blinkThread = new Thread()
		{
			public void run()
			{
				synchronized(m_lblJAPIcon)
				{
					m_lblJAPIcon.setIcon(m_iconJAPactive);
					try
					{
						m_lblJAPIcon.wait(1000);
					}
					catch (InterruptedException a_e)
					{
						// ignore
					}
					if (m_Controller.getAnonMode())
					{
						m_lblJAPIcon.setIcon(m_iconJAPpassive);
					}
					else
					{
						m_lblJAPIcon.setIcon(m_iconJAPdisconnected);
					}
				}
			}
		};
		blinkThread.start();
	}
} //class ViewIconified
