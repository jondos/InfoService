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
//import sun.net.ftp.FtpClient;
//import sun.net.ftp.*;

//import com.enterprisedt.net.ftp.FTPClient;

final class JAPDirectProxyConnection implements Runnable {
    private Socket clientSocket;

    private boolean debug = true;
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

    public JAPDirectProxyConnection (Socket s) {
		this.clientSocket = s;
    }

    public void run() {
                System.out.println("run().jappr");
		threadNumber = getThreadNumber();
		JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"C("+threadNumber+") - New connection handler started.");
		try {
			// open stream from client
			inputStream = new DataInputStream(clientSocket.getInputStream());
			// read first line of request
			requestLine = this.readLine(inputStream);
              System.out.println(requestLine+ "request DirectProx.run()");
			JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"C("+threadNumber+") - RequestLine: >" + requestLine +"<");
			// Examples:
			//  CONNECT 192.168.1.2:443 HTTP/1.0
			//  GET http://192.168.1.2/incl/button.css HTTP/1.0
			StringTokenizer st = new StringTokenizer(requestLine);
			try {
				method = st.nextToken();
				uri    = st.nextToken();
				try {
					version = st.nextToken();
				} catch (NoSuchElementException e) {
					;
				}
			} catch (NoSuchElementException e) {
				badRequest();
			}
			JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"C("+threadNumber+") - RequestMethod: >" + method +"<");
			JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"C("+threadNumber+") - URI: >" + uri +"<");
			JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"C("+threadNumber+") - Version: >" + version +"<");
			if (method.equalsIgnoreCase("CONNECT")) {
			// Handle CONNECT
				int idx = uri.indexOf(':');
				if ((idx > 0) && (idx < uri.length())) {
					host = uri.substring(0,idx);
					JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"C("+threadNumber+") - Host: >" + host +"<");
					port = Integer.parseInt(uri.substring(idx+1));
					JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"C("+threadNumber+") - Port: >" + port +"<");
					handleCONNECT();
				} else {
					badRequest();
				}
			} else if (method.equalsIgnoreCase("GET")     ||
					   method.equalsIgnoreCase("POST")    ||
					   method.equalsIgnoreCase("PUT")     ||
					   method.equalsIgnoreCase("DELETE")  ||
					   method.equalsIgnoreCase("TRACE")   ||
					   method.equalsIgnoreCase("OPTIONS") ||
					   method.equalsIgnoreCase("HEAD")) {
			// Handle HTTP Connections
				URL url = new URL(uri);
				protocol = url.getProtocol();
				JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"C("+threadNumber+") - Protocol: >" + protocol +"<");
				host = url.getHost();
				JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"C("+threadNumber+") - Host: >" + host +"<");
				port = url.getPort();
				if (port == -1) { port = 80; }
				JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"C("+threadNumber+") - Port: >" + port +"<");
				file = url.getFile();
				JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"C("+threadNumber+") - File: >" + file +"<");

        if (protocol.equalsIgnoreCase("http")) {
					handleHTTP();
				} else if (protocol.equalsIgnoreCase("ftp")){
                                     // unknownProtocol();
                                     System.out.println("handleftp()");
                                     handleFTP();
                        }
			} else {
				badRequest();
			}
		}//else if
                        catch (IOException ioe) {
//			JAPDebug.out(JAPDebug.NOTICE,JAPDebug.NET,"C("+threadNumber+") - Exception: " + ioe);
			if (ioe.toString().startsWith("java.net.UnknownHostException"))
				cannotConnect();
		} catch (Exception e) {
			JAPDebug.out(JAPDebug.NOTICE,JAPDebug.NET,"C("+threadNumber+") - Exception: " + e);
			badRequest();
		}
		try {
			clientSocket.close();
		} catch (Exception e) {
			JAPDebug.out(JAPDebug.EXCEPTION,JAPDebug.NET,"C("+threadNumber+") - Exception while closing socket: " + e);
		}

    }

	private void responseTemplate(String error, String message) {
		try {
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
		} catch (Exception e) {
			JAPDebug.out(JAPDebug.EXCEPTION,JAPDebug.NET,"C("+threadNumber+") - Exception: " + e);
		}
	}

	private void cannotConnect() {
		responseTemplate("404 Connection error","Cannot connect to "+host+":"+port+".");
	}

	private void unknownProtocol() {
		responseTemplate("501 Not implemented","Protocol <B>"+protocol+"</B> not implemented, supported or unknown.");
	}

	private void badRequest() {
		responseTemplate("400 Bad Request","Bad request: "+requestLine);
	}

	private void handleCONNECT() throws Exception {
		try {
			// create Socket to Server
			Socket serverSocket = new Socket(host,port);
			// Response from server is transfered to client in a sepatate thread
			JAPDirectProxyResponse pr = new JAPDirectProxyResponse(serverSocket, clientSocket);
			Thread prt = new Thread(pr);
			// next Header lines
			String nextLine = this.readLine(inputStream);
			JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"C("+threadNumber+") - Header: >" + nextLine + "<");
			while (nextLine.length() != 0) {
				nextLine = this.readLine(inputStream);
				JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"C("+threadNumber+") - Header: >" + nextLine + "<");
			}
			// create stream --> server
		    DataOutputStream outputStream = new DataOutputStream(serverSocket.getOutputStream());
			// send "HTTP/1.0 200 Connection established" --> client
			BufferedWriter toClient = new BufferedWriter(new OutputStreamWriter(clientSocket.getOutputStream()));
			toClient.write("HTTP/1.0 200 Connection established\r\n\r\n");
			toClient.flush();
			prt.start();
			// Transfer data client --> server
			byte[] buff=new byte[1000];
			int len;
			while((len=inputStream.read(buff))!=-1) {
				if(len>0) {
					outputStream.write(buff,0,len);
				}
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
                      System.out.println("handlehttp()");
			Socket serverSocket = new Socket(host,port);

			// Response from server is transfered to client in a sepatate thread
                        System.out.println(serverSocket.toString()+" serverSocket");
			//JAPDirectProxyResponse pr = new JAPDirectProxyResponse(serverSocket, clientSocket);
			//Thread prt = new Thread(pr);
			//prt.start();
                        //write response to client
                        OutputStream os = clientSocket.getOutputStream();
                        //read response of the server
                        InputStream is = serverSocket.getInputStream();
			// Send request --> server
			BufferedWriter outputStream = new BufferedWriter(new OutputStreamWriter(serverSocket.getOutputStream()));
                        // Send response --> client
                        String protocolString = "";
                        // protocolString += method+" "+file+ " "+version;
			protocolString += method+" "+file+ " "+"HTTP/1.0";
			JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"C("+threadNumber+") - ProtocolString: >" + protocolString + "<");
			outputStream.write(protocolString + "\r\n");
                        String nextLine = this.readLine(inputStream);
   ////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
                         System.out.println(nextLine+" nextline handleHTTP()");

                         JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"C("+threadNumber+") - Header: >" + nextLine + "<");
			while (nextLine.length() != 0) {
				if (! filter(nextLine) ) {
                        // write single lines to server
					outputStream.write(nextLine+"\r\n");
                                        //System.out.println(nextLine);
				} else {
					JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"C("+threadNumber+") - Header " + nextLine + " filtered");
				}
				nextLine = this.readLine(inputStream);
				JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"C("+threadNumber+") - Header: >" + nextLine + "<");
			}

			// send final CRLF --> server
			outputStream.write("\r\n");
			outputStream.flush();

                        byte[] buff=new byte[1000];
                        int len=0;
                                              if (is.toString()!= null)
                                              {

                                                                while((len = is.read(buff))>0)
									{
										os.write(buff,0,len);

									}
                                                                //}//fi
                                                                os.flush();
                                                                JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"R - EOF from Server.");

                                                }//fi
                                                else
                                                {
                                                 System.out.println(" is is null");
                                                }
                        // wait unitl response thread has finished
			JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"\n");
			JAPDebug.out(JAPDebug.DEBUG,JAPDebug.THREAD,"C("+threadNumber+") - Waiting for resonse thread...");
			//prt.join();
			JAPDebug.out(JAPDebug.DEBUG,JAPDebug.THREAD,"C("+threadNumber+") -                  ...finished!");

			// transfer rest of request, i.e. POST data or similar --> server
		/*	JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"C("+threadNumber+") - Headers sended, POST data may follow");
			int byteRead = inputStream.read();
			while (byteRead != -1) {
//				System.out.print((char)byteRead);
				outputStream.write(byteRead);
				outputStream.flush();
				byteRead = inputStream.read();
			}
			JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"\n("+threadNumber+") - End of request");
			// send final CRLF --> server
			outputStream.write("\r\n");
			outputStream.flush();
			// wait unitl response thread has finished
			JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"\n");
			JAPDebug.out(JAPDebug.DEBUG,JAPDebug.THREAD,"C("+threadNumber+") - Waiting for resonse thread...");
			JAPDebug.out(JAPDebug.DEBUG,JAPDebug.THREAD,"C("+threadNumber+") -                         ...finished!");*/
		    outputStream.close();
	    	inputStream.close();
                is.close();
                os.close();
	    	serverSocket.close();
		} catch (Exception e) {
			throw e;
		}
    }

    private void handleFTP()
    {
    //a request as GET ftp://213.244.188.60/pub/jaxp/89h324hruh/jaxp-1_1.zip was started

    try{
  // Array of String[] for the list command
    // String[] list;
     System.out.println("FTP-MODE");
    // Socket serverSocket = new Socket(host,port);
     //write response to client
     OutputStream os = clientSocket.getOutputStream();
     //read response of the server
    // InputStream is = serverSocket.getInputStream();
     InputStream is ;
     // Send request --> server
    // BufferedWriter outputStream = new BufferedWriter(new OutputStreamWriter(serverSocket.getOutputStream()));
     // to do parse Input for username and e-mail-adress
     String nextLine = this.readLine(inputStream);
     System.out.println(nextLine);
     // create an FTP-Client and try to communicate: Browser --> FTP-Client --> FTP-Server
   //  FTPClient ftpClient = new FTPClient (host);

     // read next Header lines <-- client
   //   FtpClient ftpClient = new FtpClient(host);
	 //ftpClient.debugResponses(true);
    //  ftpClient.login("anonymous","heinz@operamail.com");
   /*   ftpClient.binary();
    */// ftpClient.ascii();-

     //File text = new File("test.txt");
     //text.createNewFile();
     //OutputStream fos = new FileOutputStream(text);
     String GETString = new String(file);
     // file or directory
     byte[] byt = new byte[1000];
     int len=0;
     if (GETString.endsWith("/"))
     {
      // String list = ftpClient.list(GETString,true);

  /*             for(int i=0; i<list.length;i++)
               {
               byte[] byt = list[i].getBytes();
               os.write(byt);
                }*///for*/
//           is = ftpClient.list();
 //          while((len = is.read())!= -1)
           {
            //System.out.println(len+" len");
            //os.write(byt,0,len);
          //  os.write(list.getBytes());
           // if(len < 1000)
           // break;
           }
        //ftpClient.quit();
        os.flush();
        os.close();
    }else{
    /*ftpClient.get(file,os);
    ftpClient.close();*/
    //is = ftpClient.get(file);
//    while((len= is.read())!= -1)
           {
           //System.out.println(len+" len");
            os.write((byte) len);
            //if(len < 1000)
           // break;
           }
      //     ftpClient.closeServer();
           os.flush();
           os.close();
          }//else
 // to do parse the following lines
  //   while (nextLine.length()!=0)
    //     {
         // nextLine = this.readLine(inputStream);
      //    System.out.println(nextLine);
       //  }//while
// Response from server is transfered to client in a seperate thread through a
// FileInputStream
/*    InputStream fis = null;
   if (text.exists()&&text.canRead()){
     fis = new FileInputStream(text);
     System.out.println("ja");
        }else{
                   //text.createNewFile();
                   fis = new FileInputStream(text);
                   System.out.println("nein");
             }
      fis.close(); */
            if( clientSocket.toString()!=null)
            {
     //JAPDirectProxyResponse pr = new JAPDirectProxyResponse(is, clientSocket);
     //Thread prt = new Thread(pr);
    // prt.start();
     System.out.println("!=null");
     os.close();
     ///prt.join();
              }
   System.out.println("FTP-MODE 1");

     }/* catch (com.aecys.net.FtpException aex)
            {
            aex.printStackTrace();
            }*/catch(IOException ioe)
            {
             ioe.printStackTrace();
            }catch (Exception e)
            {
            e.printStackTrace();
            }



    }

    private boolean filter(String l) {
		String cmp = "Proxy-Connection";
		if (l.regionMatches(true,0,cmp,0,cmp.length())) return true;
		return false;
    }

    private String readLine(DataInputStream inputStream) throws Exception {
		String returnString = "";
		try{
			int byteRead = inputStream.read();
			while (byteRead != 10 && byteRead != -1) {
				if (byteRead != 13)
					returnString += (char)byteRead;
				byteRead = inputStream.read();
			}
		} catch (Exception e) {
	    	throw e;
		}
		return returnString;
    }

    private synchronized int getThreadNumber() {
		return threadCount++;
    }
}
