import javax.swing.JOptionPane;
import javax.swing.JDialog;
import javax.swing.JLabel;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
final class JAPSetAnonModeSplash implements Runnable
	{
		private Thread t;
		static JAPSetAnonModeSplash oSetAnonMode=null;
		private JDialog dlgAbort;
		
		private JAPSetAnonModeSplash()
			{
				t=null;
			}
		
		static public void start(boolean bSetAnonMode)
			{
				if(oSetAnonMode==null)
					oSetAnonMode=new JAPSetAnonModeSplash();
				oSetAnonMode.start_internal(bSetAnonMode);
			}
		
		private void start_internal(boolean bSetAnonMode)
			{
				t=new Thread(this);
				Object[] optionsAbort={new JLabel(JAPUtil.loadImageIcon(JAPModel.BUSYFN,false))};
				String message;
				if(bSetAnonMode)
					message=JAPMessages.getString("setAnonModeSplashConnect");
				else
					message=JAPMessages.getString("setAnonModeSplashDisconnect");
				JOptionPane optionPaneAbort=new JOptionPane(message,JOptionPane.INFORMATION_MESSAGE,
																						 0,null,optionsAbort);
				dlgAbort=optionPaneAbort.createDialog(JAPModel.getModel().getView(),JAPMessages.getString("setAnonModeSplashTitle"));
				dlgAbort.setModal(true);
				dlgAbort.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);			
				dlgAbort.setEnabled(false);
	   		t.start();
			}
		
		static public void abort()
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
				dlgAbort.show();
			}

}
