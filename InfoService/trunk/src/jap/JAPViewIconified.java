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
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.border.LineBorder;
import javax.swing.BorderFactory;

import anon.infoservice.MixCascade;
import anon.infoservice.StatusInfo;
import anon.proxy.IProxyListener;
import gui.JAPDll;
import gui.JAPMessages;
import gui.GUIUtils;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import anon.infoservice.Database;
import anon.infoservice.DatabaseMessage;
import java.util.Observable;
import java.util.Observer;
import java.awt.Cursor;

final public class JAPViewIconified extends JWindow implements ActionListener,
	JAPObserver, Observer
{
	private static final String MSG_TT_SWITCH_ANONYMITY =
		JAPViewIconified.class.getName() + "_ttSwitchAnonymity";

	private static final String STR_HIDDEN_WINDOW = Double.toString(Math.random());
	private static Frame m_frameParent;

	private JAPController m_Controller;
	private AbstractJAPMainView m_mainView;
	private JLabel m_labelBytes, m_labelUsers, m_labelTraffic;
	private JLabel m_lblJAPIcon;
	private Point m_startPoint;
	private Font m_fontDlg;
	private boolean m_bIsDragging = false;
	private NumberFormat m_NumberFormat;
	private boolean m_anonModeDisabled = false;
	private Object SYNC_CURSOR = new Object();

	private static Frame getParentFrame()
	{
		if (m_frameParent == null)
		{
			m_frameParent = new Frame(STR_HIDDEN_WINDOW);
		}
		return m_frameParent;
}

	private long m_lTrafficWWW, m_lTrafficOther;

	private Runnable m_runnableValueUpdate;

	public JAPViewIconified(AbstractJAPMainView a_mainView)
	{
		super(getParentFrame());
		 m_fontDlg = new Font("Sans", Font.BOLD, 11);
		setName(STR_HIDDEN_WINDOW);
		m_mainView = a_mainView;
		m_frameParent.setIconImage(m_mainView.getIconImage());
		LogHolder.log(LogLevel.INFO, LogType.MISC, "JAPViewIconified:initializing...");
		m_Controller = JAPController.getInstance();
		m_NumberFormat = NumberFormat.getInstance();
		m_runnableValueUpdate = new Runnable()
		{
			public void run()
			{
				updateValues1();
			}
		};
		init();
		Database.getInstance(StatusInfo.class).addObserver(this);
	}

	private void init()
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

		JButton bttn = new JButton(GUIUtils.loadImageIcon(JAPConstants.ENLARGEYICONFN, true, false));
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
		m_lblJAPIcon = new JLabel(GUIUtils.loadImageIcon(JAPViewIconified.class.getName() + "_icon16discon.gif", true, false));
		//m_lblJAPIcon.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
		m_lblJAPIcon.setToolTipText(JAPMessages.getString(MSG_TT_SWITCH_ANONYMITY));
		m_lblJAPIcon.addMouseListener(new MouseAdapter()
		{
			public void mouseClicked(MouseEvent a_event)
			{
				if (!m_anonModeDisabled && GUIUtils.isMouseButton(a_event, MouseEvent.BUTTON1_MASK))
				{
					m_lblJAPIcon.setBorder(BorderFactory.createLoweredBevelBorder());
					m_Controller.setAnonMode(!m_Controller.getAnonMode());
				}
			}
			public void mouseEntered(MouseEvent a_event)
			{
				synchronized (SYNC_CURSOR)
				{
					if (!m_anonModeDisabled)
					{
						// do it that 'complicated' as it would otherwise not work on JRE 1.5...
						setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
						getRootPane().setToolTipText(JAPMessages.getString(MSG_TT_SWITCH_ANONYMITY));
					}
				}
			}

			public void mouseExited(MouseEvent e)
			{
				synchronized (SYNC_CURSOR)
				{
					setCursor(Cursor.getPredefinedCursor(Cursor.DEFAULT_CURSOR));
					getRootPane().setToolTipText(null);
				}
			}
		});
		p2.add(m_lblJAPIcon);
		p2.add(bttn);
		p.add(p2, BorderLayout.SOUTH);
		MyMouseListener listener = new MyMouseListener();
		p.addMouseListener(listener);
		p.addMouseMotionListener(listener);
		setContentPane(p);

		pack();
		GUIUtils.moveToUpRightCorner(this);
		m_labelBytes.setText(JAPMessages.getString("iconifiedViewZero"));
		m_labelUsers.setText(JAPMessages.getString("iconifiedViewNA"));
		m_labelTraffic.setText(JAPMessages.getString("iconifiedViewNA"));
		//JAPDll.setWindowOnTop(this,STR_HIDDEN_WINDOW, true);
		GUIUtils.setAlwaysOnTop(this, true);
	}

	public void setVisible(boolean a_bVisible)
	{
		JAPController.getInstance().switchViewWindow(!a_bVisible);
		super.setVisible(a_bVisible);
	}

	public void switchBackToMainView()
	{
		if (m_mainView == null || (!isVisible() && m_mainView.isVisible()))
		{
			return;
		}
		m_mainView.setVisible(true);
		setVisible(false);
		m_mainView.toFront();
	}

	public JAPViewIconified getViewIconified()
	{
		// should be this...
		return m_mainView.getViewIconified();
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
				if (m_Controller.isAnonConnected())  //m_Controller.getAnonMode())
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
						m_lblJAPIcon.setIcon(GUIUtils.loadImageIcon(JAPConstants.IICON16FN, true, false));
					}
				}
				else
				{
					/* not in anonymity mode */
					m_labelUsers.setText(JAPMessages.getString("iconifiedViewNA"));
					m_labelTraffic.setText(JAPMessages.getString("iconifiedViewNA"));
					synchronized(m_lblJAPIcon)
					{
						m_lblJAPIcon.setIcon(GUIUtils.loadImageIcon(JAPViewIconified.class.getName() + "_icon16discon.gif", true, false));
					}
				}

			}
			catch (Throwable t)
			{
			}
		}
	}

	public void update(Observable a_observable, Object a_message)
	{
		if (a_observable == Database.getInstance(StatusInfo.class))
		{
			Object data =  ((DatabaseMessage)a_message).getMessageData();
			if (data instanceof StatusInfo && ((StatusInfo)data).getId().equals(
								JAPController.getInstance().getCurrentMixCascade().getId()))
			{

				updateValues(false);
			}
		}
	}



	public void disableSetAnonMode()
	{
		m_anonModeDisabled = true;
	}

	public void updateValues(boolean bSync)
	{
		synchronized (m_runnableValueUpdate)
		{
			if (SwingUtilities.isEventDispatchThread())
			{
				m_runnableValueUpdate.run();
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
			setButtonBorder();
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

	private class MyMouseListener extends MouseAdapter implements MouseMotionListener
	{
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
	}

	private void setButtonBorder()
	{
		if (!m_anonModeDisabled)
		{
			m_lblJAPIcon.setBorder(BorderFactory.createRaisedBevelBorder());
		}
		else
		{
			m_lblJAPIcon.setBorder(BorderFactory.createEmptyBorder());
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
					m_lblJAPIcon.setIcon(GUIUtils.loadImageIcon(JAPViewIconified.class.getName() + "_icon16red.gif", true, false));
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
						m_lblJAPIcon.setIcon(GUIUtils.loadImageIcon(JAPConstants.IICON16FN, true, false));
					}
					else
					{
						m_lblJAPIcon.setIcon(GUIUtils.loadImageIcon(JAPViewIconified.class.getName() + "_icon16discon.gif", true, false));
					}
				}
			}
		};
		blinkThread.setDaemon(true);
		blinkThread.start();
	}
} //class ViewIconified
