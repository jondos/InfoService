import java.net.* ;
import java.io.*;

/** 
 *  This class is used to inform the user that he tries to
 *  send requests although anonymity mode is off.
 *  This version should be replaced later by a parsing and direct sending of the
 *  request to the server.
 */
public class JAPDirectConnection extends Thread {
	
	private JAPModel model;
	private Socket s;
	
	public JAPDirectConnection(Socket s) {
		this.s = s;
		this.model = JAPModel.getModel();
	}
	
	public void run() {
		try {
			BufferedWriter toClient = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
			toClient.write(model.getString("htmlAnonModeOff"));
			toClient.close();
			s.close();
		}
		catch (Exception e) {
				JAPDebug.out(JAPDebug.EXCEPTION,JAPDebug.NET,"JAPFeedbackConnection: Exception: "+e);
		}
	}
	
}
