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
import java.awt.*;
import java.awt.event.*;
import javax.swing.*;

import anon.JAPAnonServiceListener;

final class JAPViewIconified extends JFrame implements ActionListener, JAPObserver {
	private JAPModel model;
	private JLabel    z1, z2, z3;
	private JButton   b;
	
	public JAPViewIconified(String s) {
		super(s);
		JAPDebug.out(JAPDebug.INFO,JAPDebug.MISC,"JAPViewIconified:initializing...");
		model = JAPModel.getModel();
		model.setIconifiedView(this);
		init();
	}
	
	public void init() {
		setIconImage(model.getView().getIconImage());
		//setBackground(Color.black);
		//setForeground(Color.blue);
		
		Font fnt = new Font("Sans",Font.PLAIN,9);
		setFont(fnt);
		
		z1 = new JLabel("00000000  ",JLabel.LEFT);
		z2 = new JLabel("00000000  ",JLabel.LEFT);
		z3 = new JLabel("00000000  ",JLabel.LEFT);
		z1.setForeground(Color.red);
		z2.setForeground(Color.red);
		z3.setForeground(Color.red);
		z1.setFont(fnt);
		z2.setFont(fnt);
		z3.setFont(fnt);
		
		JLabel x1 = new JLabel(model.getString("iconifiedviewChannels")+": ",JLabel.RIGHT);
		JLabel x2 = new JLabel(model.getString("iconifiedviewBytes")+": ",JLabel.RIGHT);
		JLabel x3 = new JLabel(model.getString("iconifiedviewUsers")+": ",JLabel.RIGHT);
		x1.setFont(fnt);
		x2.setFont(fnt);
		x3.setFont(fnt);
		
		JPanel p1 = new JPanel(new GridLayout(2/*3*/,2) );
//		p1.add(x1); p1.add(z1);
		p1.add(x2); p1.add(z2);
		p1.add(x3); p1.add(z3);
		
		JPanel p2 = new JPanel(new FlowLayout() );
		b = new JButton(JAPUtil.loadImageIcon(model.ENLARGEYICONFN,true));
		b.addActionListener(this);
		b.setToolTipText(model.getString("enlargeWindow"));
	    b.setMnemonic(model.getString("iconifyButtonMn").charAt(0));
		p2.add(b);
		
		getContentPane().add(new JLabel(JAPUtil.loadImageIcon(model.JAPEYEFN,true)), BorderLayout.NORTH);
		getContentPane().add(p1, BorderLayout.CENTER);
//		getContentPane().add(new JLabel(" "), BorderLayout.EAST);
//		getContentPane().add(new JLabel(" "), BorderLayout.WEST);
		getContentPane().add(p2, BorderLayout.SOUTH);
		addWindowListener(new WindowAdapter() {public void windowClosing(WindowEvent e) {
//			model.setJAPViewDeIconified();
			model.getView().setVisible(true);
			setVisible(false);
			}
		});	
		setResizable(false);
		pack();
		JAPUtil.upRightFrame(this);
		z1.setText("0");
		z2.setText("0");
		z3.setText("N/A");
		
	}
	
	public void actionPerformed(ActionEvent event) {
		Object object = event.getSource();
		if (object == b) {
//			model.setJAPViewDeIconified();
			model.getView().setVisible(true);
			setVisible(false);
		}
		
	}	
	
	public void valuesChanged (JAPModel m) {
		if (model.getAnonServer().getNrOfActiveUsers() != -1)
			z3.setText(Integer.toString(model.getAnonServer().getNrOfActiveUsers()));
		else
			z3.setText("N/A");
	}

	public void channelsChanged(int c)
	{
			z1.setText(Integer.toString(c));
	}
	public void transferedBytes(int b)
	{
	z2.setText(Integer.toString(b));
	}
}
