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

public final class JAPViewIconified extends JFrame implements ActionListener, JAPObserver {
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
		setBackground(Color.black);
		setForeground(Color.blue);
		setFont(new Font("Sans",Font.PLAIN,9));
		z1 = new JLabel("0000000",JLabel.LEFT);
		z2 = new JLabel("0000000",JLabel.LEFT);
		z3 = new JLabel("0000000",JLabel.LEFT);
		z1.setForeground(Color.red);
		z2.setForeground(Color.red);
		z3.setForeground(Color.red);
		
		JPanel p1 = new JPanel(new GridLayout(3,2) );
		p1.add(new JLabel("Chnls:",JLabel.RIGHT));
		p1.add(z1);
		p1.add(new JLabel("Bytes:",JLabel.RIGHT));
		p1.add(z2);
		p1.add(new JLabel("Users:",JLabel.RIGHT));
		p1.add(z3);
		
		JPanel p2 = new JPanel(new FlowLayout() );
		b = new JButton(model.loadImageIcon(model.ENLARGEYICONFN,true));
		b.addActionListener(this);
		b.setToolTipText(model.getString("enlargeWindow"));
		p2.add(b);
		
		getContentPane().add(new JLabel(model.loadImageIcon(model.ICONFN,true)), BorderLayout.NORTH);
		getContentPane().add(p1, BorderLayout.CENTER);
//		getContentPane().add(new JLabel(" "), BorderLayout.EAST);
//		getContentPane().add(new JLabel(" "), BorderLayout.WEST);
		getContentPane().add(p2, BorderLayout.SOUTH);
		addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) { model.setJAPViewDeIconified(); }
		});	
		setResizable(false);
		pack();
		model.upRightFrame(this);
		
	}
	
	public void actionPerformed(ActionEvent event) {
		Object object = event.getSource();
		if (object == b) 
			model.setJAPViewDeIconified();
	}	
	
	public synchronized void valuesChanged (JAPModel m) {
		z1.setText(""+model.getNrOfChannels());
		z2.setText(""+model.getNrOfBytes());
		if (model.nrOfActiveUsers != -1)
			z3.setText(""+model.nrOfActiveUsers);
		else
			z3.setText("N/A");
	}
			
}
