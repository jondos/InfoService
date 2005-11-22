package jap;

import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPasswordField;

import anon.util.IMiscPasswordReader;
import gui.*;

/**
 * PasswordReader that displays a dialog window.
 * Can be used to simply enter a password, or to choose a new password (must be entered two times).
 */
public class JAPPasswordReader implements ActionListener, IMiscPasswordReader
{
	private String passwd = null;
	private JDialog dialog = null;
	private JPasswordField pwdField1, pwdField2;
	private boolean m_bRepeat;

	/**
	 * Creates a JAPPasswordReader object.
	 * @param repeat boolean if true, the password must be repeated, i.e. entered two times.
	 */
	public JAPPasswordReader(boolean repeat)
	{
		m_bRepeat = repeat;
	}

	public void actionPerformed(ActionEvent e)
	{
		if (e.getActionCommand() == "ok")
		{
			String p1 = new String(pwdField1.getPassword());
			String p2 = null;
			if (m_bRepeat)
			{
				p2 = new String(pwdField2.getPassword());
			}
			if (p1.length() < 6)
			{
				JOptionPane.showMessageDialog(JAPController.getView(),
											  JAPMessages.getString("passwordTooShort"));
				return;
			}
			if (m_bRepeat && ! (p1.equals(p2)))
			{
				JOptionPane.showMessageDialog(JAPController.getView(),
											  JAPMessages.getString("passwordsDontMatch"));
				return;
			}
			passwd = p1;
		}
		dialog.dispose();
	}

	public String readPassword(Object message)
	{
		GridBagLayout l = new GridBagLayout();
		Insets i = new Insets(0, 5, 0, 5);
		JPanel panel = new JPanel(l);
		GridBagConstraints c = new GridBagConstraints();
		JLabel lbl = new JLabel(JAPMessages.getString("password") + ":");
		c.anchor = GridBagConstraints.WEST;
		c.gridx = 0;
		c.gridy = 0;
		c.insets = i;
		panel.add(lbl, c);
		pwdField1 = new JPasswordField(20);
		c.gridx = 1;
		panel.add(pwdField1, c);

		if (m_bRepeat)
		{
			lbl = new JLabel(JAPMessages.getString("passwordAgain") + ":");
			c.gridx = 0;
			c.gridy = 1;
			panel.add(lbl, c);

			pwdField2 = new JPasswordField(20);
			c.gridx = 1;
			panel.add(pwdField2, c);
		}

		JButton bttnOk = new JButton(JAPMessages.getString("bttnOk"));
		bttnOk.setActionCommand("ok");
		bttnOk.addActionListener(this);
		JPanel btnPanel = new JPanel();
		btnPanel.add(bttnOk);
		JButton bttnCancel = new JButton(JAPMessages.getString("bttnCancel"));
		bttnCancel.addActionListener(this);
		btnPanel.add(bttnCancel);
		c.gridx = 0;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridwidth = 2;
		c.gridy = 2;
		panel.add(btnPanel, c);
		Object[] options = new Object[1];
		options[0] = panel;
		JOptionPane o = new JOptionPane(message,
										JOptionPane.QUESTION_MESSAGE, 0
										, null, options);
		dialog = o.createDialog(JAPController.getView(), JAPMessages.getString("passwdDlgTitle"));
		dialog.toFront();
		dialog.setVisible(true);
		return passwd;
	}
}
