
import java.io.*;
import java.net.*;
import java.util.*;
import java.text.*;
import java.util.StringTokenizer;

public class JAPDirectProxyConnection implements Runnable {
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
		threadNumber = getThreadNumber();
		JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"C("+threadNumber+") - New connection handler started.");
		try {
			// open stream from client
			inputStream = new DataInputStream(clientSocket.getInputStream());
			// read first line of request
			requestLine = this.readLine(inputStream);
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
				} else {
					unknownProtocol();
				}
			} else {
				badRequest();
			}
		} catch (IOException ioe) {
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
			prt.start();			
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
			Socket serverSocket = new Socket(host,port);		
			// Response from server is transfered to client in a sepatate thread
			JAPDirectProxyResponse pr = new JAPDirectProxyResponse(serverSocket, clientSocket);
			Thread prt = new Thread(pr);
			prt.start();
			// Send request --> server
			BufferedWriter outputStream = new BufferedWriter(new OutputStreamWriter(serverSocket.getOutputStream()));
			String protocolString = "";
//			protocolString += method+" "+file+ " "+version;
			protocolString += method+" "+file+ " "+"HTTP/1.0";
			JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"C("+threadNumber+") - ProtocolString: >" + protocolString + "<");
			outputStream.write(protocolString + "\r\n");
			// read next Header lines <-- client
			String nextLine = this.readLine(inputStream);
			JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"C("+threadNumber+") - Header: >" + nextLine + "<");
			while (nextLine.length() != 0) {
				if (! filter(nextLine) ) {
					outputStream.write(nextLine+"\r\n");
				} else {
					JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"C("+threadNumber+") - Header " + nextLine + " filtered");
				}
				nextLine = this.readLine(inputStream);
				JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"C("+threadNumber+") - Header: >" + nextLine + "<");
			}
			// send final CRLF --> server
			outputStream.write("\r\n");
			outputStream.flush();
			// transfer rest of request, i.e. POST data or similar --> server
			JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"C("+threadNumber+") - Headers sended, POST data may follow");
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
			prt.join(); 
			JAPDebug.out(JAPDebug.DEBUG,JAPDebug.THREAD,"C("+threadNumber+") -                           ...finished!");
		    outputStream.close();
	    	inputStream.close();
	    	serverSocket.close();
		} catch (Exception e) {
			throw e;
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
