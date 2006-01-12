/*
 Copyright (c) 2000 - 2005, The JAP-Team
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
package infoservice;

import java.io.BufferedOutputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.URL;
import java.util.Enumeration;
import java.util.StringTokenizer;
import java.util.Vector;

import anon.infoservice.Constants;
import anon.util.URLDecoder;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/**
 * This is a simple implementation of an HTTP server. This implementation doesn't support the most
 * HTTP/1.1 stuff (like persistent connections or special encodings). But parsing an HTTP/1.1
 * request is working.
 */
public class InfoServiceConnection implements Runnable
{

	/**
	 * Stores the socket which is connected to the client we got the request from.
	 */
	private Socket m_socket;

	/**
	 * Stores the ID of the connection which is used in the log-output for identifying the current
	 * connection.
	 */
	private int m_connectionId;

	/**
	 * Stores the implementation which is used for processing the received HTTP request and creating
	 * the HTTP response which is sent back to the client.
	 */
	private JWSInternalCommands m_serverImplementation;

	/**
	 * Creates a new instance of InfoServiceConnection for handling the received data as an HTTP
	 * request.
	 *
	 * @param a_socket The socket which is connected to the client. We read the data from this
	 *                 socket, parse it as an HTTP request, process the request and send back the
	 *                 HTTP response to this socket.
	 * @param a_connectionId The connection ID which is used for identifying the log outputs of this
	 *                       new instance of InfoServiceConnection.
	 * @param a_serverImplementation The implementation which is used for processing the HTTP
	 *                               request and creating the HTTP response which is sent back to
	 *                               the client.
	 */
	public InfoServiceConnection(Socket a_socket, int a_connectionId,
								 JWSInternalCommands a_serverImplementation)
	{
		m_socket = a_socket;
		m_connectionId = a_connectionId;
		m_serverImplementation = a_serverImplementation;
	}

	/**
	 * This is the Thread implementation for reading, parsing, processing the request and sending
	 * the response. In every case, the socket is closed after finishing this method.
	 */
	public void run()
	{
		try
		{
			LogHolder.log(LogLevel.DEBUG, LogType.NET,
						  "InfoServiceConnection (" + Integer.toString(m_connectionId) +
						  "): Handle connection from: " + m_socket.getInetAddress().getHostAddress() + ":" +
						  m_socket.getPort());
			try
			{
				m_socket.setSoTimeout(Constants.COMMUNICATION_TIMEOUT);
			}
			catch (Exception e)
			{
				LogHolder.log(LogLevel.WARNING, LogType.NET,
							  "InfoServiceConnection (" + Integer.toString(m_connectionId) +
							  "): Cannot set socket timeout: " + e.toString());
			}
			InputStream streamFromClient = null;
			OutputStream streamToClient = null;
			try
			{
				streamFromClient = m_socket.getInputStream();
		//		streamToClient = new TimedOutputStream(m_socket.getOutputStream(),
		//			Constants.COMMUNICATION_TIMEOUT);
				streamToClient = m_socket.getOutputStream();
			}
			catch (Exception e)
			{
				LogHolder.log(LogLevel.ERR, LogType.NET,
							  "InfoServiceConnection (" + Integer.toString(m_connectionId) +
							  "): Error while accessing the socket streams: " + e.toString());
			}

			if ( (streamFromClient != null) && (streamToClient != null))
			{
				HttpResponseStructure response = null;
				int internalRequestMethodCode = 0;
				String requestMethod = "";
				String requestUrl = "";
				byte[] postData = null;

				try
				{
					/* first line is the Request-Line with the format:
					 * METHOD <space> REQUEST-URI <space> HTTP-VERSION <CRLF>
					 * Attention: <CRLF> is removed from readRequestLine()
					 */
					InfoServiceConnectionReader connectionReader = new InfoServiceConnectionReader(
						streamFromClient, Constants.MAX_REQUEST_HEADER_SIZE);
					String requestLine = readRequestLine(connectionReader);
					LogHolder.log(LogLevel.DEBUG, LogType.NET,
								  "InfoServiceConnection (" + Integer.toString(m_connectionId) +
								  "): Request line: " + requestLine);
					StringTokenizer requestLineTokenizer = new StringTokenizer(requestLine, " ");
					if (requestLineTokenizer.countTokens() != 3)
					{
						throw (new Exception("Invalid HTTP request line: " + requestLine));
					}
					requestMethod = requestLineTokenizer.nextToken();
					requestUrl = requestLineTokenizer.nextToken();
					if (requestMethod.equals("POST"))
					{
						internalRequestMethodCode = Constants.REQUEST_METHOD_POST;
					}
					else if (requestMethod.equals("GET"))
					{
						internalRequestMethodCode = Constants.REQUEST_METHOD_GET;
					}
					else if (requestMethod.equals("HEAD"))
					{
						internalRequestMethodCode = Constants.REQUEST_METHOD_GET;
					}
					else
					{
						throw (new Exception("Unknown HTTP request method: " + requestMethod));
					}

					int contentLength = 0;
					/* now process the HTTP request header */
					Enumeration headerLines = readHeader(connectionReader).elements();
					while (headerLines.hasMoreElements())
					{
						String currentHeaderLine = (String) (headerLines.nextElement());
						LogHolder.log(LogLevel.DEBUG, LogType.NET,
									  "InfoServiceConnection (" + Integer.toString(m_connectionId) +
									  "): Processing header line: " + currentHeaderLine);
						/* everything until the first ':' is the field-name, everything after the first ':'
						 * belongs to the field-value
						 */
						int fieldDelimiterPos = currentHeaderLine.indexOf(":");
						if (fieldDelimiterPos < 0)
						{
							throw (new Exception("Invalid header line: " + currentHeaderLine));
						}
						String currentHeaderFieldName = currentHeaderLine.substring(0, fieldDelimiterPos);
						/* leading or trailing whitspaces can be removed from a field value */
						String currentHeaderFieldValue = currentHeaderLine.substring(fieldDelimiterPos + 1).
							trim();
						if (currentHeaderFieldName.equalsIgnoreCase("Content-Length"))
						{
							try
							{
								contentLength = Integer.parseInt(currentHeaderFieldValue);
								LogHolder.log(LogLevel.DEBUG, LogType.NET,
											  "InfoServiceConnection (" + Integer.toString(m_connectionId) +
											  "): Read 'Content-Length: " + Integer.toString(contentLength) +
											  "' from header.");
							}
							catch (Exception e)
							{
								throw (new Exception("Invalid Content-Length: " + currentHeaderLine + " " +
									e.toString()));
							}
						}
					}

					/* read the POST data, if it is a POST request */
					if ( (internalRequestMethodCode == Constants.REQUEST_METHOD_POST) && (contentLength >= 0))
					{
						/* the volume of post data should be limited -> check the limit */
						if (contentLength > Configuration.getInstance().getMaxPostContentLength())
						{
							throw (new Exception(
								"POST: Content is longer than allowed maximum POST content length."));
						}
						ByteArrayOutputStream postDataRead = new ByteArrayOutputStream(contentLength);
						int currentPos = 0;
						while (currentPos < contentLength)
						{
							int byteRead = streamFromClient.read();
							if (byteRead == -1)
							{
								throw (new Exception(
									"POST: Content was shorter than specified in the header. Content length from header: " +
									Integer.toString(contentLength) + " Real content length: " +
									Integer.toString(currentPos)));
							}
							currentPos++;
							postDataRead.write(byteRead);
						}
						postData = postDataRead.toByteArray();
						LogHolder.log(LogLevel.DEBUG, LogType.NET,
									  "InfoServiceConnection (" + Integer.toString(m_connectionId) +
									  "): Post-Data received for request: " + requestUrl + ": " +
									  postDataRead.toString());
					}
				}
				catch (Exception e)
				{
					LogHolder.log(LogLevel.ERR, LogType.NET,
								  "InfoServiceConnection (" + Integer.toString(m_connectionId) +
								  "): Invalid request - not processed: " + e.toString());
					response = new HttpResponseStructure(HttpResponseStructure.HTTP_RETURN_BAD_REQUEST);
				}

				/* request parsing done -> process the request, if there was no error */
				if (response == null)
				{
					/* no error until yet -> process the command via the server implementation */
					if (m_serverImplementation != null)
					{
						response = m_serverImplementation.processCommand(internalRequestMethodCode,
							requestUrl, postData, m_socket.getInetAddress());
						if (response == null)
						{
							LogHolder.log(LogLevel.ERR, LogType.NET,
										  "InfoServiceConnection (" + Integer.toString(m_connectionId) +
										  "): Response could not be generated: Request: " + requestMethod +
										  " " + requestUrl);
							response = new HttpResponseStructure(HttpResponseStructure.HTTP_RETURN_NOT_FOUND);
						}
					}
					else
					{
						LogHolder.log(LogLevel.ERR, LogType.NET,
									  "InfoServiceConnection (" + Integer.toString(m_connectionId) +
									  "): Server implementation not available.");
						response = new HttpResponseStructure(HttpResponseStructure.
							HTTP_RETURN_INTERNAL_SERVER_ERROR);
					}
				}

				/* send our response back to the client */
				if (response != null)
				{
					try
					{
						LogHolder.log(LogLevel.DEBUG, LogType.NET,
									  "InfoServiceConnection (" + Integer.toString(m_connectionId) +
									  "): Response for request: " + requestUrl + ": " +
									  (new String(response.getResponseData())));
						byte[] theResponse = response.getResponseData();
						int index = 0;
						int len = theResponse.length;
						//we send the data bakch to the client in chunks of 10000 bytes in order
						//to avoid unwanted timeouts for large messages and slow connections
						while (len > 0)
						{
							int aktLen = Math.min(len, 10000);
							streamToClient.write(theResponse, index, aktLen);
							index += aktLen;
							len -= aktLen;
						}
					}
					catch (Exception e)
					{
						LogHolder.log(LogLevel.ERR, LogType.NET,
									  "InfoServiceConnection (" + Integer.toString(m_connectionId) +
									  "): Error while sending the response to the client: " + e.toString());
					}
				}
				try
				{
					streamToClient.flush();
				}
				catch (Exception e)
				{
					/* if we get an error here, normally there was already one -> log only for debug reasons */
					LogHolder.log(LogLevel.DEBUG, LogType.NET,
								  "InfoServiceConnection (" + Integer.toString(m_connectionId) +
								  "): Error while flushing output stream to client: " + e.toString());
				}
			}

			/* try to close everything */
			try
			{
				streamFromClient.close();
			}
			catch (Exception e)
			{
				/* if we get an error here, normally there was already one -> log only for debug reasons */
				LogHolder.log(LogLevel.DEBUG, LogType.NET,
							  "InfoServiceConnection (" + Integer.toString(m_connectionId) +
							  "): Error while closing input stream from client: " + e.toString());
			}
			try
			{
				streamToClient.close();
			}
			catch (Exception e)
			{
				/* if we get an error here, normally there was already one -> log only for debug reasons */
				LogHolder.log(LogLevel.DEBUG, LogType.NET,
							  "InfoServiceConnection (" + Integer.toString(m_connectionId) +
							  "): Error while closing output stream to client: " + e.toString());
			}
			try
			{
				m_socket.close();
			}
			catch (Exception e)
			{
				/* if we get an error here, normally there was already one -> log only for debug reasons */
				LogHolder.log(LogLevel.DEBUG, LogType.NET,
							  "InfoServiceConnection (" + Integer.toString(m_connectionId) +
							  "): Error while closing connection: " + e.toString());
			}
			LogHolder.log(LogLevel.DEBUG, LogType.NET,
						  "InfoServiceConnection (" + Integer.toString(m_connectionId) +
						  "): Connection thread finished.");
		}
		catch (Throwable t)
		{
		}
	}

	/**
	 * Reads the first line of an HTTP request (including the CRLF at the end of the line, so the
	 * next byte read from the underlying stream, is the first byte of the request header). The
	 * line is also parsed for illegal characters. If there are illegal characters found or the
	 * read limit is exhausted or there is an unexpected end of the stream, an exception is thrown.
	 * Also if there occured an exception while reading from the stream, this exception is thrown.
	 * The syntax of the request line (whether it is a valid HTTP request line) is not checked here.
	 *
	 * @param a_inputData The InfoServiceConnectionReader for reading the line (with a limit of
	 *                    maximally read bytes).
	 *
	 * @return The line read from the stream without the trailing CRLF.
	 */
	private String readRequestLine(InfoServiceConnectionReader a_inputData) throws Exception
	{
		ByteArrayOutputStream readBuffer = new ByteArrayOutputStream(256);
		boolean requestLineReadingDone = false;
		while (requestLineReadingDone == false)
		{
			int byteRead = a_inputData.read();
			if (byteRead == -1)
			{
				throw (new Exception("Unexpected end of request line. Request line was: " +
									 readBuffer.toString()));
			}
			/* check for illegal characters */
			if ( ( (byteRead < 32) && (byteRead != 13)) || (byteRead == 127))
			{
				throw (new Exception("Illegal character in request line found. Character (dec): " +
									 Integer.toString(byteRead) + ". Request line was: " +
									 readBuffer.toString()));
			}
			if (byteRead == 13)
			{
				byteRead = a_inputData.read();
				if (byteRead != 10)
				{
					/* only complete <CRLF> is allowed */
					throw (new Exception("CR without LF found in request line: Request line was: " +
										 readBuffer.toString()));
				}
				/* <CRLF> found -> end of line */
				requestLineReadingDone = true;
			}
			else
			{
				readBuffer.write(byteRead);
			}
		}
		return readBuffer.toString();
	}

	/**
	 * Reads the whole header of an HTTP request (including the last CRLF signalizing the end of the
	 * header, so the next byte read from the stream would be the first of the HTTP content). The
	 * request line of the HTTP request should already be read from the stream. Folded header lines
	 * are concatenated to one header line (by removing the CRLF used for folding). Also all lines
	 * are parsed for illegal characters. If illegal characters are found or the read limit is
	 * exhausted or there is an unexpected end of the stream, an exception is thrown. Also if there
	 * occured an exception while reading from the stream, this exception is thrown. The syntax of
	 * the header lines (whether they are valid HTTP header lines) returned by this method is not
	 * checked here.
	 *
	 * @param a_inputData The InfoServiceConnectionReader where the HTTP header shall be read from
	 *                    (with a limit of maximally read bytes). The initially request line of the
	 *                    HTTP request should already be read from the underlying stream.
	 *
	 * @return A Vector of strings with the header lines (maybe empty, if there were no header
	 *         fields). The trailing CRLF is removed at every line. If a header line was folded, the
	 *         folding CRLF is removed (but not the SPACEs or TABs at the begin of the next line)
	 *         and the whole line is within one String stored. The empty line which signals the end
	 *         of the HTTP header is not included within the Vector.
	 */
	private Vector readHeader(InfoServiceConnectionReader a_inputData) throws Exception
	{
		ByteArrayOutputStream readBuffer = new ByteArrayOutputStream(256);
		Vector allHeaderLines = new Vector();
		boolean startOfHeader = true;
		boolean headerReadingDone = false;
		while (headerReadingDone == false)
		{
			int byteRead = a_inputData.read();
			/* first check, whether it is the <CR> -> read the next bytes in this case */
			if (byteRead == 13)
			{
				byteRead = a_inputData.read();
				if (byteRead != 10)
				{
					/* only complete <CRLF> is allowed in the header */
					throw (new Exception("CR without LF found in header: Current header line: " +
										 readBuffer.toString()));
				}
				if (startOfHeader == true)
				{
					/* header started with <CRLF> -> no header -> stop reading */
					headerReadingDone = true;
				}
				else
				{
					/* we have a complete <CRLF>, but maybe it is only for folding a long header line ->
					 * if next line starts with <space> or <TAB>, it's only a folded header line -> according
					 * to the HTTP specification, it is no problem to remove <CRLF> in this case
					 */
					byteRead = a_inputData.read();
					if ( (byteRead != 9) && (byteRead != 32))
					{
						/* <CRLF> was end of the header line -> add the header line to the Vector of header
						 * lines (without trailing <CRLF>)
						 */
						allHeaderLines.addElement(readBuffer.toString());
						readBuffer.reset();
						/* maybe it was the last header line, then there is a second <CRLF> */
						if (byteRead == 13)
						{
							byteRead = a_inputData.read();
							if (byteRead != 10)
							{
								/* only complete <CRLF> is allowed in the header */
								throw (new Exception(
									"CR without LF found in header: Only <CR> found on the current line."));
							}
							/* found empty header line -> end of header -> stop reading */
							headerReadingDone = true;
						}
					}
				}
			}
			if (headerReadingDone == false)
			{
				if (startOfHeader == true)
				{
					/* header not started with <CRLF> -> header is not empty */
					startOfHeader = false;
				}
				if (byteRead == -1)
				{
					throw (new Exception("Unexpected end of header. Current header line: " +
										 readBuffer.toString()));
				}
				/* check for illegal characters */
				if ( ( (byteRead < 32) && (byteRead != 9)) || (byteRead == 127))
				{
					throw (new Exception("Illegal character in header found. Character (dec): " +
										 Integer.toString(byteRead) + ". Current header line: " +
										 readBuffer.toString()));
				}
				/* valid character -> add it to the buffer */
				readBuffer.write(byteRead);
			}
		}
		return allHeaderLines;
	}

}
