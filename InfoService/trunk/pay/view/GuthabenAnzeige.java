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
package pay.view;

import java.awt.Color;
import java.awt.event.ActionListener;
import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.border.TitledBorder;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import pay.Pay;
import pay.util.PayText;
import pay.PayAccount;
import pay.PayAccountsFile;

/**
 * Klasse zum darstellen der wichtigsten Informationen eines Kontos. keine BenutzerInteraktion
 * @author Grischan Gl\uFFFDnzel
 */
public class GuthabenAnzeige extends Box
{

	Pay m_Pay = Pay.getInstance();
	PayAccountsFile m_Accounts = PayAccountsFile.getInstance();

	private JLabel info;
	private JLabel guthaben;
	private JButton options;

	public GuthabenAnzeige()
	{
		super(BoxLayout.X_AXIS);
		info = new JLabel(PayText.get("no Account"));
		guthaben = new JLabel("");
		options = new JButton(PayText.get("change"));
		if (m_Accounts.hasActiveAccount())
		{
/*			modelUpdated(null);*/
		}
/*		pay.addModelListener(this);*/
		makeGuthabenBox(this);
	}

/*	public void modelUpdated(ModelEvent event)
	{
		LogHolder.log(LogLevel.DEBUG, LogType.PAY, "modelUpdated");
		if (pay.accountFileHasUsedAccount())
		{
			info.setText("Nr: " + pay.getAccount(pay.getUsedAccount()).getAccountNumber());
			guthaben.setText("" + pay.getAccount(pay.getUsedAccount()).getCredit() + " Bytes");
		}
	}*/

	private void makeGuthabenBox(Box box)
	{
		TitledBorder border = new TitledBorder(PayText.get("Balance"));
		/**SK13 setBorder not Swing1.1
		 //box.setBorder(border);*/
		box.setBackground(Color.red);
		box.add(info);
		box.add(Box.createHorizontalGlue());
		box.add(guthaben);
		box.add(Box.createHorizontalStrut(10));
		box.add(options);
	}

	public void addButtonListener(ActionListener listener)
	{
		options.addActionListener(listener);
	}

}
