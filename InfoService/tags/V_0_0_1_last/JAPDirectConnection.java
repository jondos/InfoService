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
