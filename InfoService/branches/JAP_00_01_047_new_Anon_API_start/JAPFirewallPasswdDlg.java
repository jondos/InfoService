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
import javax.swing.JOptionPane;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JPasswordField;
import javax.swing.JDialog;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.BorderLayout;

final class JAPFirewallPasswdDlg implements ActionListener
	{
		private String passwd;
		private static JAPFirewallPasswdDlg dlg=null;
		private JDialog dialog=null;
		private JPasswordField pwdField;

		private JAPFirewallPasswdDlg()
			{
				passwd=null;
			}

		public void	actionPerformed(ActionEvent e)
			{
				if(e.getActionCommand()=="ok")
					{
						passwd=new String(pwdField.getPassword());
					}
				dialog.dispose();
			}

		public static String getPasswd()
			{
				dlg=new JAPFirewallPasswdDlg();
				dlg.pwdField=new JPasswordField(20);
				JPanel panel=new JPanel();
				JButton bttnOk=new JButton(JAPMessages.getString("bttnOk"));
				bttnOk.setActionCommand("ok");
				bttnOk.addActionListener(dlg);
				JButton bttnCancel=new JButton(JAPMessages.getString("bttnCancel"));
				bttnCancel.addActionListener(dlg);
				panel.add(bttnOk);
				panel.add(bttnCancel);
				JPanel p=new JPanel(new BorderLayout());
				p.add("Center",dlg.pwdField);
				p.add("South",panel);
				JAPModel m=JAPModel.getModel();
				Object[] options=new Object[1];
				options[0]=p;
				JOptionPane o=new JOptionPane(JAPMessages.getString("passwdDlgInput")+"\n"+
																			JAPMessages.getString("passwdDlgHost")+": "+
																			m.getFirewallHost()+"\n"+
																			JAPMessages.getString("passwdDlgPort")+": "+
																			m.getFirewallPort()+"\n"+
																			JAPMessages.getString("passwdDlgUserID")+": "+
																			m.getFirewallAuthUserID()+"\n",
																			JOptionPane.QUESTION_MESSAGE,0
																			,null,options);
				dlg.dialog=o.createDialog(JAPModel.getView(),JAPMessages.getString("passwdDlgTitle"));
				dlg.dialog.toFront();
				dlg.dialog.show();
				return dlg.passwd;
			}
	}