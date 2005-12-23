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
package gui;

import java.awt.Frame;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JTextArea;

import anon.util.IMiscPasswordReader;
import logging.LogType;

public class PasswordBox extends JAPDialog implements ActionListener, IMiscPasswordReader
{
	private static final String OK_COMMAND = "OK";

	private JPasswordField m_textOldPasswd, m_textNewPasswd, m_textConfirmPasswd;
	private char[] m_passwd = null;
	private char[] m_oldPasswd = null;
	private int m_Type;
	private boolean m_bCanceled = false;
	public final static int NEW_PASSWORD = 1;
	public final static int ENTER_PASSWORD = 2;
	public final static int CHANGE_PASSWORD = 3;

	public PasswordBox(Frame parent, String title, int type, String msg)
	{
		super(parent, title);

		m_Type = type;
		GridBagLayout layout = new GridBagLayout();
		getContentPane().setLayout(layout);
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.WEST;
		c.insets = new Insets(10, 10, 10, 10);
		c.gridx = 0;
		c.gridy = 0;

		if (msg != null)
		{
			c.gridwidth = 2;
			c.fill = GridBagConstraints.HORIZONTAL;
			c.weighty = 1;
			c.weightx = 1;
			JTextArea msgl = new JTextArea(msg);
			msgl.setEditable(false);
			msgl.setEnabled(false);
			msgl.setBackground(getContentPane().getBackground());
			msgl.setDisabledTextColor(new JLabel("").getForeground());
			msgl.setWrapStyleWord(false);
			msgl.setLineWrap(false);
			layout.setConstraints(msgl, c);
			getContentPane().add(msgl);
			c.gridy++;
			c.gridwidth = 1;
			c.fill = GridBagConstraints.NONE;
			c.weighty = 0;
			c.weightx = 0;
		}
		if (type == CHANGE_PASSWORD)
		{
			JLabel old = new JLabel("Enter Old Password");
			layout.setConstraints(old, c);
			getContentPane().add(old);
			m_textOldPasswd = new JPasswordField(20);
			m_textOldPasswd.setEchoChar('*');
			c.gridx = 1;
			c.weightx = 1;
			c.fill = GridBagConstraints.HORIZONTAL;
			layout.setConstraints(m_textOldPasswd, c);
			getContentPane().add(m_textOldPasswd);
		}
		if (type == CHANGE_PASSWORD || type == NEW_PASSWORD)
		{
			JLabel new1 = new JLabel("Enter New Password");
			c.gridx = 0;
			c.gridy++;
			c.weightx = 0;
			c.fill = GridBagConstraints.NONE;
			layout.setConstraints(new1, c);
			getContentPane().add(new1);
			m_textNewPasswd = new JPasswordField(20);
			m_textNewPasswd.setEchoChar('*');
			c.fill = GridBagConstraints.HORIZONTAL;
			c.gridx = 1;
			c.weightx = 1;
			layout.setConstraints(m_textNewPasswd, c);
			getContentPane().add(m_textNewPasswd);
		}
		JLabel new2 = new JLabel("Confirm Password");
		c.fill = GridBagConstraints.NONE;
		c.gridx = 0;
		c.gridy++;
		c.weightx = 0;
		layout.setConstraints(new2, c);
		getContentPane().add(new2);
		m_textConfirmPasswd = new JPasswordField(20);
		m_textConfirmPasswd.setEchoChar('*');
		m_textConfirmPasswd.addKeyListener(new PasswordKeyListener());
		c.gridx = 1;
		c.weightx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		layout.setConstraints(m_textConfirmPasswd, c);
		getContentPane().add(m_textConfirmPasswd);

		JPanel p = new JPanel();
		JButton b = new JButton(OK_COMMAND);
		b.setActionCommand(OK_COMMAND);
		/*      c.gridx = 0;
		   c.gridwidth=1;
		   c.anchor=GridBagConstraints.CENTER;
		   layout.setConstraints(b,c);
		   getContentPane().add(b);
		 */
		p.add(b);
		b.addActionListener(this);
		b = new JButton("Cancel");
		b.setActionCommand("Cancel");
		/*      c.gridx = 1;
		   c.gridwidth=1;
		   c.gridy ++;
		   layout.setConstraints(b,c);
		   getContentPane().add(b);
		 */
		p.add(b);
		b.addActionListener(this);
		c.gridx = 0;
		c.gridwidth = 2;
		c.gridy++;
		c.anchor = GridBagConstraints.CENTER;
		c.weightx = 1;
		c.fill = GridBagConstraints.HORIZONTAL;
		layout.setConstraints(p, c);
		getContentPane().add(p);
		pack();
		setResizable(false);
	}

	public void actionPerformed(ActionEvent ae)
	{
		if (ae.getActionCommand().equals(OK_COMMAND))
		{
			m_bCanceled = false;

			if (m_Type == NEW_PASSWORD || m_Type == CHANGE_PASSWORD)
			{
				boolean eqv;
				char[] oldpw = m_textConfirmPasswd.getPassword();
				char[] newpw = m_textNewPasswd.getPassword();
				eqv = (oldpw.length == newpw.length);
				if (eqv)
				{
					for (int i = 0; i < oldpw.length; i++)
					{
						if (oldpw[i] != newpw[i])
						{
							eqv = false;
							break;
						}
					}
				}
				if (!eqv)
				{
					showErrorDialog(this, null, "Passwords do not match.", "Password Error", LogType.GUI);
					return;
				}
				m_passwd = m_textNewPasswd.getPassword();
			}
			else if (m_Type == ENTER_PASSWORD)
			{
				m_passwd = m_textConfirmPasswd.getPassword();
			}
			if (m_Type == CHANGE_PASSWORD)
			{
				m_oldPasswd = m_textOldPasswd.getPassword();
			}
		}
		else
		{
			m_passwd = null;
			m_bCanceled = true;
		}
		dispose();
	}

	public boolean isCanceled()
	{
		return m_bCanceled;
	}

	public String readPassword(Object a_message)
	{
		setVisible(true);
		if (isCanceled())
		{
			return null;
		}
		if (getPassword() == null)
		{
			return new String();
		}
		return new String(getPassword());
	}

	public char[] getPassword()
	{
		return m_passwd;
	}

	public char[] getOldPassword()
	{
		return m_oldPasswd;
	}


	private class PasswordKeyListener extends KeyAdapter
	{
		public void keyReleased(KeyEvent a_event)
		{
			if (a_event.getKeyCode() == KeyEvent.VK_ENTER)
			{
				actionPerformed(
					new ActionEvent(
					a_event.getSource(), ActionEvent.ACTION_PERFORMED, OK_COMMAND));
			}
		}
	}
}
