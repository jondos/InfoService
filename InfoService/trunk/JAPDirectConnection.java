import java.net.Socket ;
import java.io.BufferedWriter;
import java.io.OutputStreamWriter;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.text.SimpleDateFormat;

/** 
 *  This class is used to inform the user that he tries to
 *  send requests although anonymity mode is off.
 *  This version should be replaced later by a parsing and direct sending of the
 *  request to the server.
 */
public final class JAPDirectConnection extends Thread 
	{
		private JAPModel model;
		private Socket s;
		private SimpleDateFormat dateFormatHTTP;
	
		public JAPDirectConnection(Socket s)
			{
				this.s = s;
				this.model = JAPModel.getModel();
				dateFormatHTTP=new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz",Locale.US);
				dateFormatHTTP.setTimeZone(TimeZone.getTimeZone("GMT"));
			}
	
		public void run()
			{
				try 
					{
						String date=dateFormatHTTP.format(new Date());
						BufferedWriter toClient = new BufferedWriter(new OutputStreamWriter(s.getOutputStream()));
						toClient.write("HTTP/1.1 200 OK\r\n");
						toClient.write("Content-type: text/html\r\n");
						toClient.write("Expires: "+date+"\r\n");
						toClient.write("Date: "+date+"\r\n");
						toClient.write("Pragma: no-cache\r\n");
						toClient.write("Cache-Control: no-cache\r\n\r\n");
						toClient.write("<HTML><TITLE> </TITLE>");
						toClient.write("<PRE>"+date+"</PRE>");
						toClient.write(model.getString("htmlAnonModeOff"));
						toClient.write("</HTML>\n");
						toClient.flush();
						toClient.close();
						s.close();
					}
				catch (Exception e)
					{
						JAPDebug.out(JAPDebug.EXCEPTION,JAPDebug.NET,"JAPFeedbackConnection: Exception: "+e);
					}
			}
}
