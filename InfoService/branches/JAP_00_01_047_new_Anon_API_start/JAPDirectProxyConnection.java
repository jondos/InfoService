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

import java.io.*;
import java.net.*;
import java.util.*;
import java.text.*;
import java.util.StringTokenizer;
import kasper.net.ftp.*;
import kasper.net.*;

final class JAPDirectProxyConnection implements Runnable
	{
    private Socket clientSocket;

    private int threadNumber;
    private static int threadCount;

    private DataInputStream inputStream = null;

	  private String requestLine = null;

	  private String method   = "";
	  private String uri      = "";
	  private String protocol = "";
	  private String version  = "";
	  private String host     = "";
	  private String file     = "";
	  private int    port     = -1;

    public JAPDirectProxyConnection (Socket s)
			{
				clientSocket = s;
      }

    public void run()
			{
				threadNumber = getThreadNumber();
				JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"C("+threadNumber+") - New connection handler started.");
				try
					{
					  // open stream from client
					  inputStream = new DataInputStream(clientSocket.getInputStream());
					  // read first line of request
					  requestLine = JAPUtil.readLine(inputStream);
						//JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"C("+threadNumber+") - RequestLine: >" + requestLine +"<");
						// Examples:
						//  CONNECT 192.168.1.2:443 HTTP/1.0
						//  GET http://192.168.1.2/incl/button.css HTTP/1.0
						StringTokenizer st = new StringTokenizer(requestLine);
						method = st.nextToken(); //Must be alwasy there
						uri    = st.nextToken();// Must be always there
						if(st.hasMoreTokens())
							version = st.nextToken();
			    }
			  catch (Exception e)
				  {
				    badRequest();
			      return;
					}
				//JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"C("+threadNumber+") - RequestMethod: >" + method +"<");
				//JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"C("+threadNumber+") - URI: >" + uri +"<");
				//JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"C("+threadNumber+") - Version: >" + version +"<");
			  try{
				if (method.equalsIgnoreCase("CONNECT"))
					{
					  // Handle CONNECT
						int idx = uri.indexOf(':');
						if (idx > 0)
							{
								host = uri.substring(0,idx);
								//JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"C("+threadNumber+") - Host: >" + host +"<");
							  port = Integer.parseInt(uri.substring(idx+1));
							  //JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"C("+threadNumber+") - Port: >" + port +"<");
							  handleCONNECT();
						  }
						else
							{
							  badRequest();
						  }
					}
				else if ( method.equalsIgnoreCase("GET")     ||
								  method.equalsIgnoreCase("POST")    ||
								  method.equalsIgnoreCase("PUT")     ||
								  method.equalsIgnoreCase("DELETE")  ||
								  method.equalsIgnoreCase("TRACE")   ||
								  method.equalsIgnoreCase("OPTIONS") ||
								  method.equalsIgnoreCase("HEAD"))
					{
					  // Handle HTTP Connections
						URL url = new URL(uri);
						protocol = url.getProtocol();
						//JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"C("+threadNumber+") - Protocol: >" + protocol +"<");
						host = url.getHost();
						//JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"C("+threadNumber+") - Host: >" + host +"<");
						port = url.getPort();
						if (port == -1)
							port = 80;
						//JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"C("+threadNumber+") - Port: >" + port +"<");
						file = url.getFile();
					  //JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"C("+threadNumber+") - File: >" + file +"<");

						if (protocol.equalsIgnoreCase("http"))
							{
							  handleHTTP();
						  }
						else if (protocol.equalsIgnoreCase("ftp"))
							{
								handleFTP();
              }
						else
							{
								unknownProtocol();
							}
					}
				else
					{
					  badRequest();
					}
		  }//try
		catch(UnknownHostException uho)
			{
				cannotConnect();
			}
    catch (Exception ioe)
			{
					JAPDebug.out(JAPDebug.NOTICE,JAPDebug.NET,"C("+threadNumber+") - Exception: " + ioe);
					badRequest();
			}
		try
			{
			  clientSocket.close();
		  }
		catch (Exception e)
			{
			  JAPDebug.out(JAPDebug.EXCEPTION,JAPDebug.NET,"C("+threadNumber+") - Exception while closing socket: " + e);
		  }

  }

	private void responseTemplate(String error, String message)
		{
		  try
				{
					BufferedWriter toClient = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
					toClient.write("HTTP/1.0 "+error+"\r\n");
					toClient.write("Content-type: text/html\r\n");
					toClient.write("Pragma: no-cache\r\n");
					toClient.write("Cache-Control: no-cache\r\n\r\n");
					toClient.write("<HTML><TITLE>"+message+"</TITLE>");
					toClient.write("<H1>"+error+"</H1>");
					toClient.write("<P>"+message+"</P>");
					toClient.write("</HTML>\n");
					toClient.flush();
					toClient.close();
				}
			catch (Exception e)
				{
					JAPDebug.out(JAPDebug.EXCEPTION,JAPDebug.NET,"C("+threadNumber+") - Exception: " + e);
				}
	  }

	private void cannotConnect()
		{
		  responseTemplate("404 Connection error","Cannot connect to "+host+":"+port+".");
	  }

	private void unknownProtocol()
		{
		  responseTemplate("501 Not implemented","Protocol <B>"+protocol+"</B> not implemented, supported or unknown.");
	  }

	private void badRequest()
		{
		  responseTemplate("400 Bad Request","Bad request: "+requestLine);
	  }

	private void handleCONNECT() throws Exception {
		try {
			// create Socket to Server
			Socket serverSocket = new Socket(host,port);
				// next Header lines
			String nextLine = JAPUtil.readLine(inputStream);
			JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"C("+threadNumber+") - Header: >" + nextLine + "<");
			while (nextLine.length() != 0) {
				nextLine = JAPUtil.readLine(inputStream);
				JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"C("+threadNumber+") - Header: >" + nextLine + "<");
			}
			// create stream --> server
			OutputStream outputStream = serverSocket.getOutputStream();
			// send "HTTP/1.0 200 Connection established" --> client
			BufferedWriter toClient = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
			toClient.write("HTTP/1.0 200 Connection established\r\n\r\n");
			toClient.flush();

			// Response from server is transfered to client in a sepatate thread
			JAPDirectProxyResponse pr = new JAPDirectProxyResponse(serverSocket.getInputStream(),
																														 clientSocket.getOutputStream());
			Thread prt = new Thread(pr);

			prt.start();
			// Transfer data client --> server
			byte[] buff=new byte[1000];
			int len;
			while((len=inputStream.read(buff))!=-1) {
				if(len>0)
					outputStream.write(buff,0,len);
			}
		   outputStream.flush();
			// wait unitl response thread has finished
			JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"\n");
			JAPDebug.out(JAPDebug.DEBUG,JAPDebug.THREAD,"C("+threadNumber+") - Waiting for resonse thread...");
			prt.join();
			JAPDebug.out(JAPDebug.DEBUG,JAPDebug.THREAD,"C("+threadNumber+") -                           ...finished!");
			toClient.close();
		  outputStream.close();
	    inputStream.close();
	    serverSocket.close();
		} catch (Exception e) {
			throw e;
		}
	}


	private void handleHTTP() throws Exception {
		try {
			// create Socket to Server
			Socket serverSocket = new Socket(host,port);

			// Send request --> server
			OutputStream outputStream = serverSocket.getOutputStream();
      // Send response --> client
      String protocolString = "";
      // protocolString += method+" "+file+ " "+version;
			protocolString += method+" "+file+ " "+"HTTP/1.0";
			JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"C("+threadNumber+") - ProtocolString: >" + protocolString + "<");
			outputStream.write((protocolString + "\r\n").getBytes());
      String nextLine = JAPUtil.readLine(inputStream);
			////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

      JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"C("+threadNumber+") - Header: >" + nextLine + "<");
			while (nextLine.length() != 0) {
				if (! filter(nextLine) ) {
                        // write single lines to server
					outputStream.write((nextLine+"\r\n").getBytes());
   			} else {
					JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"C("+threadNumber+") - Header " + nextLine + " filtered");
				}
				nextLine =JAPUtil.readLine(inputStream);
				JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"C("+threadNumber+") - Header: >" + nextLine + "<");
			}

			// send final CRLF --> server
			outputStream.write("\r\n".getBytes());
			outputStream.flush();

			// Response from server is transfered to client in a sepatate thread
 			JAPDirectProxyResponse pr = new JAPDirectProxyResponse(serverSocket.getInputStream(),
																														 clientSocket.getOutputStream());
			Thread prt = new Thread(pr);
			prt.start();

			JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"C("+threadNumber+") - Headers sended, POST data may follow");
			byte[] buff=new byte[1000];
			int len;
			while((len=inputStream.read(buff))!=-1)
				{
				  if(len>0)
					  outputStream.write(buff,0,len);
			  }
			outputStream.flush();

			JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"\n");
			JAPDebug.out(JAPDebug.DEBUG,JAPDebug.THREAD,"C("+threadNumber+") - Waiting for resonse thread...");
			prt.join();
			JAPDebug.out(JAPDebug.DEBUG,JAPDebug.THREAD,"C("+threadNumber+") -                  ...finished!");

			outputStream.close();
			inputStream.close();
			serverSocket.close();
		} catch (Exception e) {
			throw e;
		}
    }

		private class MyFTPClient extends FTPClient
			{
				public MyFTPClient()
					{
						super();
					}
				public FTPServerResponse[] retr(String File,OutputStream os) throws IOException
					{
						try{
						if(openConnection)
							{
								dataConnection=new Socket(InetAddress.getByName(remoteDataIP),remoteDataPort);
								dataConnection.setSoTimeout(30000);
							}
						FTPServerResponse resp=execute("retr ",File);
						InputStream in=dataConnection.getInputStream();
						int len;
						byte[] buff=new byte[2048];
						while((len=in.read(buff))>0)
							os.write(buff,0,len);
						dataConnection.close();
						return null;
						}
						catch(IOException ioe)
							{
								throw ioe;
							}
					}
				public FTPServerResponse connect(String host,
                                  int port) throws UnknownHostException, IOException
			  {
					try{
					  FTPServerResponse r=super.connect(host,port);
					  controlConnection.setSoTimeout(30000);
					  return r;
					}
					catch(UnknownHostException uh)
						{
							throw uh;
						}
					catch(IOException e)
						{
							throw e;
						}

				}

			};
    private void handleFTP()
    {
    //a request as GET ftp://213.244.188.60/pub/jaxp/89h324hruh/jaxp-1_1.zip was started

    try{
     String end="</pre></body></html>";
     String endInfo ="</pre></h4><hr><pre>";

     OutputStream os = clientSocket.getOutputStream();

     String nextLine = JAPUtil.readLine(inputStream);
     FTPServerResponse ftpsp = null;
     MyFTPClient ftpClient = new MyFTPClient();
			//Login...
	    ftpsp = ftpClient.connect(host);

			//Login +passive Mode
			ftpsp=ftpClient.user("anonymous");
      ftpsp=ftpClient.pass("JAP@xxx.com");
      //todo -> message bie list ausgeben....
		  String[] arstrMotd=ftpsp.getResponses();

      ftpsp=ftpClient.pasv();


     //////////////////////////////////////////////////////////////////////////
     // LIST or GET
     String GETString = file;
		 ftpsp=ftpClient.cwd(file); //directory?

		 if(ftpsp.getResponseCode()==250) //was Directory!
		  {// a directory
				String URL = uri;
				if(!URL.endsWith("/"))
					URL+="/";
				os.write("HTTP/1.0 200 Ok\n\rContent-Type: text/html\r\n\r\n<html><head><title>FTP root at ".getBytes());
				os.write(URL.getBytes());
				os.write("</title></head><body><h2>FTP root at ".getBytes());
				os.write(URL.getBytes());
				os.write("</h2><hr><h4><pre>".getBytes());
				for(int k=0;k<arstrMotd.length;k++)
					{
						os.write(arstrMotd[k].getBytes());
						os.write('\n');
					}
				os.write("</pre></h4><hr><pre>".getBytes());
				FTPServerResponse currentResponses[] = ftpClient.list();
				// Now let's print out the list.  Let's print the length as well.

				RemoteFile remoteFiles[] = currentResponses[0].getRemoteFiles();
				StringBuffer help=new StringBuffer(256);
				help.append(' ');
				for (int i = 0; i < remoteFiles.length; ++i)
					{
						String strLen="          "+Long.toString(remoteFiles[i].length());
						strLen=strLen.substring(strLen.length()-10);
						help.append(remoteFiles[i].getProtections());
						help.append(strLen);
					  if (remoteFiles[i].isDirectory())
							{
								help.append(" Directory ");
							  help.append("<a href=\"");
								help.append(URL);
								help.append(remoteFiles[i].getName());
								help.append("/\"><b>");
								help.append(remoteFiles[i].getName());
								help.append("</b></a><br>");
							}
						else
							{
								help.append("    File   ");
							  help.append("<a href=\"");
								help.append(URL);
								help.append(remoteFiles[i].getName());
								help.append("\">");
								help.append(remoteFiles[i].getName());
								help.append("</a><br>");
							}
					 os.write(help.toString().getBytes());
					 help.setLength(1);
					}//for
        os.write(end.getBytes());
      }
		else//a file
      {
        FTPServerResponse currentResponses[] = null;
			  ftpsp=ftpClient.type(FTPClient.IMAGE);
			  currentResponses=ftpClient.list(file);
			  long len=currentResponses[0].getRemoteFiles()[0].length();
			  os.write(("HTTP/1.0 200 Ok\r\nContent-Type: application/octet-stream\r\nContent-Length: "+Long.toString(len)+"\r\n\r\n").getBytes());
			  ftpsp=ftpClient.pasv();
			  currentResponses=ftpClient.retr(file,os);
      }//else

		//Logout
		ftpClient.quit();

    os.flush();
    os.close();

    }
		catch (Exception e)
            {
            }
	  }


    private boolean filter(String l) {
		String cmp = "Proxy-Connection";
		if (l.regionMatches(true,0,cmp,0,cmp.length())) return true;
		return false;
    }



    private synchronized int getThreadNumber() {
		return threadCount++;
    }
}
