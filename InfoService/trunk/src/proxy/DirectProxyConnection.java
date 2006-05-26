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
package proxy;

import jap.JAPModel;
import jap.JAPUtil;

import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.URL;
import java.net.UnknownHostException;
import java.text.DateFormat;
import java.text.NumberFormat;
import java.text.SimpleDateFormat;
import java.util.StringTokenizer;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPFile;

import anon.infoservice.HTTPConnectionFactory;
import anon.infoservice.ListenerInterface;
import anon.infoservice.ProxyInterface;
import anon.shared.ProxyConnection;

final class DirectProxyConnection implements Runnable
{
	private Socket m_clientSocket;

	private int m_threadNumber;
	private static int m_threadCount;

	private DataInputStream m_inputStream = null;

	private String m_requestLine = null;

	private String m_strMethod = "";
	private String m_strURI = "";
	private String m_strProtocol = "";
	private String m_strVersion = "";
	private String m_strHost = "";
	private String m_strFile = "";
	private int m_iPort = -1;
	private static DateFormat m_DateFormat = SimpleDateFormat.getDateTimeInstance();
	private static NumberFormat m_NumberFormat = NumberFormat.getInstance();

	public DirectProxyConnection(Socket s)
	{
		m_clientSocket = s;
	}

	public void run()
	{
		m_threadNumber = getThreadNumber();
		LogHolder.log(LogLevel.DEBUG, LogType.NET,
					  "C(" + m_threadNumber + ") - New connection handler started.");
		try
		{
			// open stream from client
			m_inputStream = new DataInputStream(m_clientSocket.getInputStream());
			// read first line of request
			m_requestLine = JAPUtil.readLine(m_inputStream);
			LogHolder.log(LogLevel.DEBUG, LogType.NET,
						  "C(" + m_threadNumber + ") - RequestLine: >" + m_requestLine + "<");
			// Examples:
			//  CONNECT 192.168.1.2:443 HTTP/1.0
			//  GET http://192.168.1.2/incl/button.css HTTP/1.0
			StringTokenizer st = new StringTokenizer(m_requestLine);
			m_strMethod = st.nextToken(); //Must be alwasy there
			m_strURI = st.nextToken(); // Must be always there
			if (st.hasMoreTokens())
			{
				m_strVersion = st.nextToken();
			}
		}
		catch (Exception e)
		{
			badRequest();
			return;
		}
		//LogHolder.log(LogLevel.DEBUG,LogType.NET,"C("+threadNumber+") - RequestMethod: >" + method +"<");
		//LogHolder.log(LogLevel.DEBUG,LogType.NET,"C("+threadNumber+") - URI: >" + uri +"<");
		//LogHolder.log(LogLevel.DEBUG,LogType.NET,"C("+threadNumber+") - Version: >" + version +"<");
		try
		{
			if (m_strMethod.equalsIgnoreCase("CONNECT"))
			{
				// Handle CONNECT
				int idx = m_strURI.indexOf(':');
				if (idx > 0)
				{
					m_strHost = m_strURI.substring(0, idx);
					//LogHolder.log(LogLevel.DEBUG,LogType.NET,"C("+threadNumber+") - Host: >" + host +"<");
					m_iPort = Integer.parseInt(m_strURI.substring(idx + 1));
					//LogHolder.log(LogLevel.DEBUG,LogType.NET,"C("+threadNumber+") - Port: >" + port +"<");
					handleCONNECT();
				}
				else
				{
					badRequest();
				}
			}
			else if (m_strMethod.equalsIgnoreCase("GET") ||
					 m_strMethod.equalsIgnoreCase("POST") ||
					 m_strMethod.equalsIgnoreCase("PUT") ||
					 m_strMethod.equalsIgnoreCase("DELETE") ||
					 m_strMethod.equalsIgnoreCase("TRACE") ||
					 m_strMethod.equalsIgnoreCase("OPTIONS") ||
					 m_strMethod.equalsIgnoreCase("HEAD"))
			{
				// Handle HTTP Connections
				URL url = new URL(m_strURI);
				m_strProtocol = url.getProtocol();
				//LogHolder.log(LogLevel.DEBUG,LogType.NET,"C("+threadNumber+") - Protocol: >" + protocol +"<");
				m_strHost = url.getHost();
				//LogHolder.log(LogLevel.DEBUG,LogType.NET,"C("+threadNumber+") - Host: >" + host +"<");
				m_iPort = url.getPort();
				if (m_iPort == -1)
				{
					m_iPort = 80;
					//LogHolder.log(LogLevel.DEBUG,LogType.NET,"C("+threadNumber+") - Port: >" + port +"<");
				}
				m_strFile = url.getFile();
				//LogHolder.log(LogLevel.DEBUG,LogType.NET,"C("+threadNumber+") - File: >" + file +"<");

				if (m_strProtocol.equalsIgnoreCase("http"))
				{
					handleHTTP();
				}
				else if (m_strProtocol.equalsIgnoreCase("ftp"))
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
		} //try
		catch (UnknownHostException uho)
		{
			cannotConnect();
		}
		catch (Exception ioe)
		{
			LogHolder.log(LogLevel.NOTICE, LogType.NET, "C(" + m_threadNumber + ") - Exception: " + ioe);
			badRequest();
		}
		try
		{
			m_clientSocket.close();
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.EXCEPTION, LogType.NET,
						  "C(" + m_threadNumber + ") - Exception while closing socket: " + e);
		}

	}

	private void responseTemplate(String error, String message)
	{
		try
		{
			BufferedWriter toClient = new BufferedWriter(new OutputStreamWriter(m_clientSocket.
				getOutputStream()));
			toClient.write("HTTP/1.0 " + error + "\r\n");
			toClient.write("Content-type: text/html\r\n");
			toClient.write("Pragma: no-cache\r\n");
			toClient.write("Cache-Control: no-cache\r\n\r\n");
			toClient.write("<HTML><TITLE>" + message + "</TITLE>");
			toClient.write("<H1>" + error + "</H1>");
			toClient.write("<P>" + message + "</P>");
			toClient.write("</HTML>\n");
			toClient.flush();
			toClient.close();
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.EXCEPTION, LogType.NET, "C(" + m_threadNumber + ") - Exception: " + e);
		}
	}

	private void cannotConnect()
	{
		responseTemplate("404 Connection error", "Cannot connect to " + m_strHost + ":" + m_iPort + ".");
	}

	private void unknownProtocol()
	{
		responseTemplate("501 Not implemented",
						 "Protocol <B>" + m_strProtocol + "</B> not implemented, supported or unknown.");
	}

	private void badRequest()
	{
		responseTemplate("400 Bad Request", "Bad request: " + m_requestLine);
	}

	private void handleCONNECT() throws Exception
	{
		try
		{
			// create Socket to Server
			Socket serverSocket = new Socket(m_strHost, m_iPort);
			// next Header lines
			String nextLine = JAPUtil.readLine(m_inputStream);
			LogHolder.log(LogLevel.DEBUG, LogType.NET,
						  "C(" + m_threadNumber + ") - Header: >" + nextLine + "<");
			while (nextLine.length() != 0)
			{
				nextLine = JAPUtil.readLine(m_inputStream);
				LogHolder.log(LogLevel.DEBUG, LogType.NET,
							  "C(" + m_threadNumber + ") - Header: >" + nextLine + "<");
			}
			// create stream --> server
			OutputStream outputStream = serverSocket.getOutputStream();
			// send "HTTP/1.0 200 Connection established" --> client
			BufferedWriter toClient = new BufferedWriter(new OutputStreamWriter(m_clientSocket.
				getOutputStream()));
			toClient.write("HTTP/1.0 200 Connection established\r\n\r\n");
			toClient.flush();

			// Response from server is transfered to client in a sepatate thread
			DirectProxyResponse pr = new DirectProxyResponse(serverSocket.getInputStream(),
				m_clientSocket.getOutputStream());
			Thread prt = new Thread(pr, "JAP - DirectProxyResponse");
			prt.setDaemon(true);
			prt.start();
			// Transfer data client --> server
			byte[] buff = new byte[1000];
			int len;
			while ( (len = m_inputStream.read(buff)) != -1)
			{
				if (len > 0)
				{
					outputStream.write(buff, 0, len);
					outputStream.flush();
				}
			}
			// wait unitl response thread has finished
			LogHolder.log(LogLevel.DEBUG, LogType.NET, "\n");
			LogHolder.log(LogLevel.DEBUG, LogType.THREAD,
						  "C(" + m_threadNumber + ") - Waiting for resonse thread...");
			prt.join();
			LogHolder.log(LogLevel.DEBUG, LogType.THREAD,
						  "C(" + m_threadNumber + ") -                           ...finished!");
			toClient.close();
			outputStream.close();
			m_inputStream.close();
			serverSocket.close();
		}
		catch (Exception e)
		{
			throw e;
		}
	}

	private void handleHTTP() throws Exception
	{
		try
		{
			// create Socket to Server
			Socket serverSocket = null;
			ProxyConnection p = null;
			if (JAPModel.getInstance().getProxyInterface()!=null&&
				JAPModel.getInstance().getProxyInterface().isValid() &&
				JAPModel.getInstance().getProxyInterface().getProtocol()==
							ProxyInterface.PROTOCOL_TYPE_SOCKS)
			{
				p = new ProxyConnection(HTTPConnectionFactory.getInstance().createHTTPConnection(new ListenerInterface(m_strHost, m_iPort), JAPModel.getInstance().getProxyInterface()).Connect());
			}
			else
			{
				p = new ProxyConnection(HTTPConnectionFactory.getInstance().createHTTPConnection(new ListenerInterface(m_strHost, m_iPort), null).Connect());

			}
			// Send request --> server
			serverSocket = p.getSocket();
			OutputStream outputStream = serverSocket.getOutputStream();
			// Send response --> client
			String protocolString = "";
			// protocolString += method+" "+file+ " "+version;
			protocolString += m_strMethod + " " + m_strFile + " " + "HTTP/1.0";
			LogHolder.log(LogLevel.DEBUG, LogType.NET,
						  "C(" + m_threadNumber + ") - ProtocolString: >" + protocolString + "<");
			outputStream.write( (protocolString + "\r\n").getBytes());
			String nextLine = JAPUtil.readLine(m_inputStream);
			////////////////////////////////////////////////////////////////////////////////////////////////////////////////////

			LogHolder.log(LogLevel.DEBUG, LogType.NET,
						  "C(" + m_threadNumber + ") - Header: >" + nextLine + "<");
			while (nextLine.length() != 0)
			{
				if (!filter(nextLine))
				{
					// write single lines to server
					outputStream.write( (nextLine + "\r\n").getBytes());
				}
				else
				{
					LogHolder.log(LogLevel.DEBUG, LogType.NET,
								  "C(" + m_threadNumber + ") - Header " + nextLine + " filtered");
				}
				nextLine = JAPUtil.readLine(m_inputStream);
				LogHolder.log(LogLevel.DEBUG, LogType.NET,
							  "C(" + m_threadNumber + ") - Header: >" + nextLine + "<");
			}

			// send final CRLF --> server
			outputStream.write("\r\n".getBytes());
			outputStream.flush();

			// Response from server is transfered to client in a sepatate thread
			DirectProxyResponse pr = new DirectProxyResponse(serverSocket.getInputStream(),
				m_clientSocket.getOutputStream());
			Thread prt = new Thread(pr, "JAP - DirectProxyResponse");
			prt.start();

			LogHolder.log(LogLevel.DEBUG, LogType.NET,
						  "C(" + m_threadNumber + ") - Headers sended, POST data may follow");
			byte[] buff = new byte[1000];
			int len;
			while ( (len = m_inputStream.read(buff)) != -1)
			{
				if (len > 0)
				{
					outputStream.write(buff, 0, len);
					outputStream.flush();
				}
			}

			LogHolder.log(LogLevel.DEBUG, LogType.NET, "\n");
			LogHolder.log(LogLevel.DEBUG, LogType.THREAD,
						  "C(" + m_threadNumber + ") - Waiting for resonse thread...");
			prt.join();
			LogHolder.log(LogLevel.DEBUG, LogType.THREAD,
						  "C(" + m_threadNumber + ") -                  ...finished!");

			outputStream.close();
			m_inputStream.close();
			serverSocket.close();
		}
		catch (Exception e)
		{
			throw e;
		}
	}

	private void handleFTP()
	{
		FTPClient ftpClient = null;
		OutputStream os = null;
		//a request as GET ftp://213.244.188.60/pub/jaxp/89h324hruh/jaxp-1_1.zip was started
		try
		{
			String end = "</pre></body></html>";
			String endInfo = "</pre></h4><hr><pre>";

			os = m_clientSocket.getOutputStream();
			ftpClient = new FTPClient();
			ftpClient.setDefaultTimeout(30000);
			ftpClient.connect(m_strHost);
			ftpClient.setSoTimeout(30000);
			//Login +passive Mode [Timeout: 30 sec]
			ftpClient.setDataTimeout(30000);
			ftpClient.login("anonymous", "JAP@xxx.com");
			ftpClient.enterLocalPassiveMode();

			if (ftpClient.changeWorkingDirectory(m_strFile)) //directory?
			{ // a directory
				ftpClient.changeToParentDirectory();
				String parentDir = ftpClient.printWorkingDirectory();
				String URL = m_strURI;
				if (!URL.endsWith("/"))
				{
					URL += "/";
				}
				os.write(
					"HTTP/1.0 200 Ok\n\rContent-Type: text/html\r\n\r\n<html><head><title>FTP directory at ".
					getBytes());
				os.write(URL.getBytes());
				os.write("</title></head><body><h2>FTP directory at ".getBytes());
				os.write(URL.getBytes());
				os.write( ("</h2><hr><pre> DIR  | <A HREF=\"" + parentDir + "\">..</A>\n").getBytes());
	FTPFile remoteFiles[] = ftpClient.listFiles(m_strFile);
				if (remoteFiles == null)
				{
					os.write( ("No files in Directory!\nServer replied:\n" + ftpClient.getReplyString()).
							 getBytes());
				}
				else
				{
					int iMaxFileNameLen = 0;
					//Sort directory...
					for (int i = 0; i < remoteFiles.length; i++)
					{
						if (remoteFiles[i].getName().length() > iMaxFileNameLen)
						{
							iMaxFileNameLen = remoteFiles[i].getName().length();
						}
						for (int j = i + 1; j < remoteFiles.length; j++)
						{
							if (remoteFiles[i].isFile() && !remoteFiles[j].isFile())
							{
								FTPFile tmp = remoteFiles[i];
								remoteFiles[i] = remoteFiles[j];
								remoteFiles[j] = tmp;
							}
						}
					}
					StringBuffer help = new StringBuffer(256);
					for (int i = 0; i < remoteFiles.length; ++i)
					{
						String strName = remoteFiles[i].getName();
						if (strName.equals(".") || strName.equals(".."))
						{
							continue;
						}
						String strLen = m_NumberFormat.format(remoteFiles[i].getSize());
						strLen = "            " + strLen;
						strLen = strLen.substring(strLen.length() - 12);
						strName = remoteFiles[i].getName() + "</A>                                        ";
						strName = strName.substring(0, Math.min(iMaxFileNameLen + 5, strName.length() - 1));
						if (remoteFiles[i].isDirectory() || remoteFiles[i].isSymbolicLink())
						{
							help.append(" DIR  | ");
							help.append("<a href=\"");
							help.append(URL);
							if (remoteFiles[i].isSymbolicLink())
							{
								help.append(remoteFiles[i].getLink());
							}
							else
							{
								help.append(remoteFiles[i].getName());
							}
							help.append("/\"><b>");
							help.append(strName);
							help.append("</b></a>\n");
						}
						else
						{
							help.append(" FILE | ");
							help.append("<a href=\"");
							help.append(URL);
							help.append(remoteFiles[i].getName());
							help.append("\">");
							help.append(strName);
							help.append(" | ");
							help.append(strLen + " | " +
										m_DateFormat.format(remoteFiles[i].getTimestamp().getTime()) + "\n");
						}
						os.write(help.toString().getBytes());
						help.setLength(0);
					} //for
				} //end if remotefiles!=null
				os.write(end.getBytes());
			} // end if Directory
			else //a file
			{
				ftpClient.setFileType(FTPClient.IMAGE_FILE_TYPE);
				FTPFile[] currentResponses = ftpClient.listFiles(m_strFile);
				long len = currentResponses[0].getSize();
				os.write( ("HTTP/1.0 200 Ok\r\nContent-Type: application/octet-stream\r\nContent-Length: " +
						   Long.toString(len) + "\r\n\r\n").getBytes());
				ftpClient.retrieveFile(m_strFile, os);
			} //else

			os.flush();
			//Logout
			ftpClient.disconnect();

			os.close();
			os = null;
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.NOTICE, LogType.NET,
						  "C(" + m_threadNumber + ") - Exception in handleFTP(): " + e);
			try
			{ //TODO generate Error message in Browser.....
				ftpClient.disconnect();
				os.flush();
				os.close();
			}
			catch (Throwable t)
			{
			}
		}
	}

	private boolean filter(String l)
	{
		String[] cmp =
			{
			"Proxy-Connection", "Pragma"};
		for (int i = 0; i < cmp.length; i++)
		{
			if (l.regionMatches(true, 0, cmp[i], 0, cmp[i].length()))
			{
				return true;
			}
		}
		return false;
	}

	private synchronized int getThreadNumber()
	{
		return m_threadCount++;
	}
}
