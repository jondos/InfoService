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

import java.awt.Font;
import java.awt.Color;
import java.awt.Insets;
import java.awt.BorderLayout;
import java.awt.Point;
import java.awt.Panel;
import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.event.*;
import java.text.NumberFormat;
import javax.swing.*;
import javax.swing.border.LineBorder;


import anon.AnonServer;

final public class JAPViewIconified extends JWindow implements ActionListener,MouseMotionListener,MouseListener,JAPObserver {
	private JAPController m_Controller;
	private JAPView       m_mainView;
	private JLabel        m_labelBytes,m_labelUsers,m_labelTraffic;
	private Point         m_startPoint;
	private boolean       m_bIsDragging=false;
	private NumberFormat  m_NumberFormat;
	private static final Font fnt = new Font("Sans",Font.PLAIN,9);
//	static final private Frame m_frameParent=new Frame("HIDDEN JAP2wer2");
	public JAPViewIconified()
		{
//			super(m_frameParent);
//			super();
//			super(JAPController.getView());
			m_mainView=JAPController.getView();
			JAPDebug.out(JAPDebug.INFO,JAPDebug.MISC,"JAPViewIconified:initializing...");
			m_Controller = JAPController.getController();
			m_NumberFormat=NumberFormat.getInstance();
			init();
		}

	public void init()
		{
			GridBagLayout la=new GridBagLayout();
			GridBagConstraints c=new GridBagConstraints();
			JPanel pTop = new JPanel(la);
			pTop.setOpaque(false);
			JLabel x2 = new JLabel(JAPMessages.getString("iconifiedviewBytes")+": ",JLabel.RIGHT);
			x2.setFont(fnt);
			c.gridx=0;
			c.gridy=0;
			c.fill=c.BOTH;
			c.weightx=0;
			c.insets=new Insets(3,3,0,0);
			c.anchor=c.NORTHWEST;
			la.setConstraints(x2,c);
			pTop.add(x2);
			c.weightx=1;
			m_labelBytes = new JLabel("00000000  ",JLabel.LEFT);
			m_labelBytes.setForeground(Color.red);
			m_labelBytes.setFont(fnt);
			c.gridx=1;
			c.weightx=0;
			la.setConstraints(m_labelBytes,c);
			pTop.add(m_labelBytes);

			JLabel x3 = new JLabel(JAPMessages.getString("iconifiedviewUsers")+": ",JLabel.RIGHT);
			x3.setFont(fnt);
			c.gridx=0;
			c.gridy=1;
			la.setConstraints(x3,c);
			pTop.add(x3);

			m_labelUsers = new JLabel("",JLabel.LEFT);
			m_labelUsers.setForeground(Color.red);
			m_labelUsers.setFont(fnt);
			c.gridx=1;
			la.setConstraints(m_labelUsers,c);
			pTop.add(m_labelUsers);

			JLabel x4 = new JLabel(JAPMessages.getString("iconifiedviewTraffic")+": ",JLabel.RIGHT);
			x4.setFont(fnt);
			c.gridy=2;
			c.gridx=0;
			la.setConstraints(x4,c);
			pTop.add(x4);
			m_labelTraffic = new JLabel("",JLabel.LEFT);
			m_labelTraffic.setForeground(Color.red);
			m_labelTraffic.setFont(fnt);
			c.gridx=1;
			la.setConstraints(m_labelTraffic,c);
			pTop.add(m_labelTraffic);

			JButton bttn = new JButton(JAPUtil.loadImageIcon(JAPConstants.ENLARGEYICONFN,true));
			bttn.setOpaque(false);
			bttn.addActionListener(this);
			bttn.setToolTipText(JAPMessages.getString("enlargeWindow"));
			JAPUtil.setMnemonic(bttn,JAPMessages.getString("iconifyButtonMn"));

			JLabel l1=new JLabel(JAPUtil.loadImageIcon(JAPConstants.JAPEYEFN,true));
			JPanel co=new JPanel();
			co.add(pTop);
			JPanel pBottom=new JPanel(new BorderLayout());
			pBottom.setBackground(new Color(204,204,204));
			pBottom.add(l1,BorderLayout.CENTER);
			co.add(pBottom);
			OverlayLayout lo=new OverlayLayout(co);
			co.setLayout(lo);
			JPanel p=new JPanel(new BorderLayout());
			p.setBorder(new LineBorder(Color.black,1));
			p.add(co,BorderLayout.CENTER);
			JPanel p2=new JPanel();
			p2.setBackground(new Color(204,204,204));
			p2.add(bttn);
			p.add(p2,BorderLayout.SOUTH);
			p.addMouseListener(this);
			p.addMouseMotionListener(this);
			setContentPane(p);

			pack();
			JAPUtil.upRightFrame(this);
			m_labelBytes.setText(JAPMessages.getString("iconifiedViewZero"));
			m_labelUsers.setText(JAPMessages.getString("iconifiedViewNA"));
			m_labelTraffic.setText(JAPMessages.getString("iconifiedViewNA"));
		}

	void switchBackToMainView() {
			if(m_mainView==null)
				return;
			setVisible(false);
			m_mainView.setVisible(true);
	}

	public void actionPerformed(ActionEvent event) {
			switchBackToMainView();
	}

	public void valuesChanged () {
		if (m_Controller.getAnonMode()) {
			AnonServer e = m_Controller.getAnonServer();
			if (e.getNrOfActiveUsers() != -1)
				m_labelUsers.setText(m_NumberFormat.format(m_Controller.getAnonServer().getNrOfActiveUsers()));
			else
				m_labelUsers.setText(JAPMessages.getString("iconifiedViewNA"));
					int t=e.getTrafficSituation();
			if(t>-1) {
						if(t < 30)
							m_labelTraffic.setText(JAPMessages.getString("iconifiedViewMeterTrafficLow"));
				else if (t < 60)
					m_labelTraffic.setText(JAPMessages.getString("iconifiedViewMeterTrafficMedium"));
				else
					m_labelTraffic.setText(JAPMessages.getString("iconifiedViewMeterTrafficHigh"));
			} else
				m_labelTraffic.setText(JAPMessages.getString("iconifiedViewNA"));
		} else {
			m_labelUsers.setText(JAPMessages.getString("iconifiedViewNA"));
			m_labelTraffic.setText(JAPMessages.getString("iconifiedViewNA"));
		}
	}

	public void channelsChanged(int c)
		{
		}

	public void transferedBytes(int b)
		{
			m_labelBytes.setText(m_NumberFormat.format(b));
		}

	public void mouseExited(MouseEvent e)
		{
		}
	public void mouseEntered(MouseEvent e)
		{
		}
	public void mouseReleased(MouseEvent e)
		{
			m_bIsDragging=false;
		}

	public void mousePressed(MouseEvent e)
		{
		}

	public void mouseClicked(MouseEvent e)
		{
			if(e.getClickCount()>1)
				switchBackToMainView();
		}

	public void mouseMoved(MouseEvent e)
		{
		}

	public void mouseDragged(MouseEvent e)
		{
			if(!m_bIsDragging)
				{
					m_bIsDragging=true;
					m_startPoint=e.getPoint();
				}
			else
				{
					Point endPoint=e.getPoint();
					Point aktLocation=getLocation();
					setLocation(aktLocation.x+endPoint.x-m_startPoint.x,aktLocation.y+endPoint.y-m_startPoint.y);
				}
		}
}
