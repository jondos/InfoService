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
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;

public final class JAPWaitSplash implements Runnable
{
	private Thread t;
	private JDialog dlgAbort;

	private JAPWaitSplash()
	{
		t = null;
	}

	public static synchronized JAPWaitSplash start(String message, String titel /*,Object oSync*/)
	{
//		synchronized(oSync)
		{
			JAPWaitSplash splash = new JAPWaitSplash();
			splash.start_internal(message, titel);
			return splash;
		}
	}

	private void start_internal(String message, String titel)
	{
		t = new Thread(this, "JAP - WaitSplash - " + titel);
		dlgAbort = new JDialog(JAPController.getView(), titel);
		dlgAbort.getContentPane().setLayout(new BorderLayout());
		JLabel l = new JLabel(message, UIManager.getIcon("OptionPane.informationIcon"), SwingConstants.CENTER);
		if (JAPModel.isSmallDisplay())
		{
			l.setBorder(new EmptyBorder(3, 3, 3, 3));
		}
		else
		{
			l.setBorder(new EmptyBorder(10, 10, 10, 10));
		}
		l.setFont(JAPController.getDialogFont());
		l.setIconTextGap(10);
		dlgAbort.getContentPane().add("Center", l);
		l = new JLabel(JAPUtil.loadImageIcon(JAPConstants.BUSYFN, true));
		l.setBorder(new EmptyBorder(10, 10, 10, 10));
		dlgAbort.getContentPane().add("South", l);
		dlgAbort.pack();
		dlgAbort.setResizable(false);
		dlgAbort.setLocationRelativeTo(JAPController.getView());
		//On Mac setModel(true) does not seam to work (Dialog is not painted...)
		String sl = System.getProperty("os.name").substring(0, 3);
		dlgAbort.setModal(!System.getProperty("os.name").substring(0, 3).equalsIgnoreCase("mac"));
		dlgAbort.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
	t.setDaemon(true);
	t.start();
	}

	public void abort( /*Object oSync*/)
	{
//		synchronized(oSync)
		{
			abort_internal();
		}
	}

	private void abort_internal()
	{
		if (dlgAbort != null)
		{
			dlgAbort.dispose();
		}
		dlgAbort = null;
	}

	public void run()
	{
		if (dlgAbort != null)
		{
			dlgAbort.setVisible(true);
		}
	}
}
