import java.net.* ;
import java.io.*;
import java.util.Date;

/** 
 *  This class is used to inform the user that he tries to
 *  send requests although anonymity mode is off.
 *  This version should be replaced later by a parsing and direct sending of the
 *  request to the server.
 */
public final class JAPDirectConnection extends Thread {
	
	private JAPModel model;
	private Socket s;
	
	public JAPDirectConnection(Socket s) {
		this.s = s;
		this.model = JAPModel.getModel();
	}
	
	public void run() {
		try {
			BufferedWriter toClient = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
			toClient.write("HTTP/1.0 200 OK\r\nContent-type: text/html\r\n\r\n");
			toClient.write("<HTML><TITLE> </TITLE>");
			toClient.write("<PRE>"+new Date()+"</PRE>");
			toClient.write(model.getString("htmlAnonModeOff"));
			toClient.write("</HTML>\n");
			toClient.close();
			s.close();
		}
		catch (Exception e) {
				JAPDebug.out(JAPDebug.EXCEPTION,JAPDebug.NET,"JAPFeedbackConnection: Exception: "+e);
		}
	}
	
}
