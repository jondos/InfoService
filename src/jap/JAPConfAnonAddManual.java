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

import javax.swing.JFrame;
import javax.swing.JTextField;
import javax.swing.JButton;
import java.awt.*;
import javax.swing.*;
import java.awt.event.*;

public class JAPConfAnonAddManual extends JDialog implements ActionListener
{
	JTextField m_nameField;
	JTextField m_addressField;

	JButton m_okButton;
	JButton m_cancelButton;

	/**
	 * Constructor
	 */
	public JAPConfAnonAddManual()
	{
		this.setModal(true);
		Container root = this.getContentPane();
		this.setTitle("Dienst manuell eintragen");
		GridLayout layout = new GridLayout(3, 2);
		root.setLayout(layout);
		JLabel l = new JLabel("Name: ");
		root.add(l);
		m_nameField = new JTextField();
		root.add(m_nameField);
		l = new JLabel("Adresse: ");
		root.add(l);
		m_addressField = new JTextField();
		root.add(m_addressField);
		m_okButton = new JButton("OK");
		m_okButton.addActionListener(this);
		root.add(m_okButton);
		m_cancelButton = new JButton("Cancel");
		m_cancelButton.addActionListener(this);
		root.add(m_cancelButton);
		this.pack();
	}

	/**
	 * Processes ActionEvents
	 * @param e ActionEvent
	 */
	public void actionPerformed(ActionEvent e)
	{
		if (e.getSource() == m_cancelButton)
			this.hide();
	}



}
