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

final class JAPViewIconified extends JFrame implements ActionListener,JAPObserver {
	private JAPModel model;
	private JLabel    z1, z2, z3,z4;
	private JButton   b;
	private JPanel p1,p2;
	private static final Font fnt = new Font("Sans",Font.PLAIN,9);	
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
		this.setFont(fnt);
		
		p1 = new JPanel(new GridLayout(3/*4*/,2) );
		
		z1 = new JLabel("00000000  ",JLabel.LEFT);
		z1.setForeground(Color.red); z1.setFont(fnt);
		JLabel x1 = new JLabel(model.getString("iconifiedviewChannels")+": ",JLabel.RIGHT);
		x1.setFont(fnt); 
//		p1.add(x1);p1.add(z1);

		z2 = new JLabel("",JLabel.LEFT);
		z2.setForeground(Color.red); z2.setFont(fnt);
		JLabel x2 = new JLabel(model.getString("iconifiedviewBytes")+": ",JLabel.RIGHT);
		x2.setFont(fnt); 
		p1.add(x2);p1.add(z2);
		
		z3 = new JLabel("",JLabel.LEFT);
		z3.setForeground(Color.red); z3.setFont(fnt);
		JLabel x3 = new JLabel(model.getString("iconifiedviewUsers")+": ",JLabel.RIGHT);
		x3.setFont(fnt); 
		p1.add(x3);p1.add(z3);

		z4 = new JLabel("",JLabel.LEFT);
		z4.setForeground(Color.red); z4.setFont(fnt);
		JLabel x4 = new JLabel(model.getString("iconifiedviewTraffic")+": ",JLabel.RIGHT);
		x4.setFont(fnt); 
		p1.add(x4);p1.add(z4);
				
		p2 = new JPanel(new FlowLayout() );
		b = new JButton(JAPUtil.loadImageIcon(model.ENLARGEYICONFN,true));
		b.addActionListener(this);
		b.setToolTipText(model.getString("enlargeWindow"));
	    JAPUtil.setMnemonic(b,model.getString("iconifyButtonMn"));
		p2.add(b);
		
//		getContentPane().add(new JLabel(JAPUtil.loadImageIcon(model.JAPEYEFN,true)), BorderLayout.NORTH);
		getContentPane().add(p1, BorderLayout.CENTER);
//		getContentPane().add(new JLabel(" "), BorderLayout.EAST);
//		getContentPane().add(new JLabel(" "), BorderLayout.WEST);
		getContentPane().add(p2, BorderLayout.SOUTH);
		addWindowListener(new WindowAdapter() { public void windowClosing(WindowEvent e) {
			switchBackToMainView();
		}});	
		
		setResizable(false);
		pack();
		JAPUtil.upRightFrame(this);
		z1.setText(model.getString("iconifiedViewZero"));
		z2.setText(model.getString("iconifiedViewZero"));
		z3.setText(model.getString("iconifiedViewNA"));
		z4.setText(model.getString("iconifiedViewNA"));
	}
	
	void switchBackToMainView() {
//			model.setJAPViewDeIconified();
			model.getView().setVisible(true);
			setVisible(false);
	}
	
	public void actionPerformed(ActionEvent event) {
		Object object = event.getSource();
		if (object == b) {
			switchBackToMainView();
		}
	}	

	public void valuesChanged (JAPModel m) {
		if (model.getAnonMode()) {
			AnonServerDBEntry e = model.getAnonServer();
			if (e.getNrOfActiveUsers() != -1)
				z3.setText(Integer.toString(model.getAnonServer().getNrOfActiveUsers()));
			else
				z3.setText(model.getString("iconifiedViewNA"));
        	int t=e.getTrafficSituation();
			if(t>-1) {
    	    	if(t < 30)
        			z4.setText(model.getString("iconifiedViewMeterTrafficLow"));
				else if (t < 60)
					z4.setText(model.getString("iconifiedViewMeterTrafficMedium"));
				else
					z4.setText(model.getString("iconifiedViewMeterTrafficHigh"));
			} else
				z4.setText(model.getString("iconifiedViewNA"));
		} else {
			z3.setText(model.getString("iconifiedViewNA"));
			z4.setText(model.getString("iconifiedViewNA"));
		}
	}

	public void channelsChanged(int c){
		z1.setText(Integer.toString(c));
	}
	public void transferedBytes(int b) {
		z2.setText(Integer.toString(b));
	}
}
