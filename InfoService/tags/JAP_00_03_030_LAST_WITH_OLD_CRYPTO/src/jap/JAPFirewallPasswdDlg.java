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

import java.awt.BorderLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import anon.util.IPasswordReader;
import anon.infoservice.ImmutableProxyInterface;

/**
 * This class shows a dialog window and reads a password from user input.
 */
final class JAPFirewallPasswdDlg implements ActionListener, IPasswordReader
{
	private String passwd;
	private JDialog dialog = null;
	private JPasswordField pwdField;

	public void actionPerformed(ActionEvent e)
	{
		if (e.getActionCommand() == "ok")
		{
			passwd = new String(pwdField.getPassword());
		}
		dialog.dispose();
	}

	public String readPassword(ImmutableProxyInterface a_proxyInterface)
	{
		pwdField = new JPasswordField(20);
		JPanel panel = new JPanel();
		JButton bttnOk = new JButton(JAPMessages.getString("bttnOk"));
		bttnOk.setActionCommand("ok");
		bttnOk.addActionListener(this);
		JButton bttnCancel = new JButton(JAPMessages.getString("bttnCancel"));
		bttnCancel.addActionListener(this);
		panel.add(bttnOk);
		panel.add(bttnCancel);
		JPanel p = new JPanel(new BorderLayout());
		p.add("Center", pwdField);
		p.add("South", panel);
		Object[] options = new Object[1];
		options[0] = p;
		JOptionPane o = new JOptionPane(JAPMessages.getString("passwdDlgInput") + "\n" +
										JAPMessages.getString("passwdDlgHost") + ": " +
										a_proxyInterface.getHost() + "\n" +
										JAPMessages.getString("passwdDlgPort") + ": " +
										a_proxyInterface.getPort() + "\n" +
										JAPMessages.getString("passwdDlgUserID") + ": " +
										a_proxyInterface.getAuthenticationUserID() + "\n",
										JOptionPane.QUESTION_MESSAGE, 0
										, null, options);
		dialog = o.createDialog(JAPController.getView(), JAPMessages.getString("passwdDlgTitle"));
		dialog.toFront();
		dialog.show();
		return passwd;
	}
}
