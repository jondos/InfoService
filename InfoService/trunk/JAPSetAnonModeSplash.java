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
import javax.swing.UIManager;
import javax.swing.border.EmptyBorder;
import javax.swing.SwingConstants;
import javax.swing.JDialog;
import javax.swing.JLabel;
import java.awt.BorderLayout;

final class JAPSetAnonModeSplash implements Runnable
	{
		private Thread t;
		static JAPSetAnonModeSplash oSetAnonMode=null;
		private JDialog dlgAbort;

		private JAPSetAnonModeSplash()
			{
				t=null;
			}

		static synchronized public void start(boolean bSetAnonMode)
			{
				if(oSetAnonMode==null)
					oSetAnonMode=new JAPSetAnonModeSplash();
				oSetAnonMode.start_internal(bSetAnonMode);
			}

		private void start_internal(boolean bSetAnonMode)
			{
				t=new Thread(this);
				String message;
				if(bSetAnonMode)
					message=JAPMessages.getString("setAnonModeSplashConnect");
				else
					message=JAPMessages.getString("setAnonModeSplashDisconnect");
				dlgAbort=new JDialog(JAPController.getController().getView(),JAPMessages.getString("setAnonModeSplashTitle"));//optionPaneAbort.createDialog(JAPModel.getModel().getView(),JAPMessages.getString("setAnonModeSplashTitle"));
				dlgAbort.getContentPane().setLayout(new BorderLayout());
				JLabel l=new JLabel(message,UIManager.getIcon("OptionPane.informationIcon"),SwingConstants.CENTER);
				l.setBorder(new EmptyBorder(10,10,10,10));
				l.setIconTextGap(10);
				dlgAbort.getContentPane().add("Center",l);
				l=new JLabel(JAPUtil.loadImageIcon(JAPConstants.BUSYFN,true));
				l.setBorder(new EmptyBorder(10,10,10,10));
				dlgAbort.getContentPane().add("South",l);
				dlgAbort.pack();
				dlgAbort.setResizable(false);
				dlgAbort.setLocationRelativeTo(JAPController.getController().getView());
				//On Mac setModel(true) does not seam to work (Dialog is not painted...)
        String sl=System.getProperty("os.name").substring(0,3);
        dlgAbort.setModal(!System.getProperty("os.name").substring(0,3).equalsIgnoreCase("mac"));
				dlgAbort.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
        t.start();
			}

		static synchronized public void abort()
			{
				if(oSetAnonMode!=null)
					oSetAnonMode.abort_internal();
			}

		private void abort_internal()
			{
				if(dlgAbort!=null)
					dlgAbort.dispose();
				dlgAbort=null;
			}

		public void run()
			{
				if(dlgAbort!=null)
          dlgAbort.show();
			}
}
