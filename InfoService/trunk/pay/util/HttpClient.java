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
package pay.util;

final public class HttpClient
{
	private final static int MAX_LINE_LENGTH = 100;
	private DataInputStream m_dataIS;
	private DataOutputStream m_dataOS;
	private Socket m_socket;

	/**
	 * Klasse zum Senden von Http-Request und empfangen der Antwort.
	 *
	 * @param socket Socket, über dem die Http-Verbindung läuft.
	 * @throws IOException
	 */
	public HttpClient(Socket socket) throws IOException
	{
		m_socket = socket;
		m_dataIS = new DataInputStream(m_socket.getInputStream());
		m_dataOS = new DataOutputStream(m_socket.getOutputStream());
	}

	/**
	 * Schließt die Http-Verbindung.
	 *
	 * @throws IOException
	 */
	public void close() throws IOException
	{
		writeRequest("GET", "close", null);
		readAnswer();
		//m_dataOS.close();
		//m_dataIS.close();
		/*SK13 removed because not Java 1.1. */
		//	m_socket.shutdownInput();
		//  m_socket.shutdownOutput();
		m_socket.close();
	}

	/**
	 * Sendet Http-Request.
	 *
	 * @param method Http-Methode (GET / POST)
	 * @param url URL
	 * @param data Im Body zu übermittelnde Daten
	 */
	public void writeRequest(String method, String url, String data) throws IOException
	{
		LogHolder.log(LogLevel.DEBUG, LogType.PAY,
					  "HttpClient.writeRequest(): " + method + " /" + url + " HTTP/1.1"
					  );

		m_dataOS.writeBytes(method + " /" + url + " HTTP/1.1\r\n");
		if (method.equals("POST"))
		{
			LogHolder.log(LogLevel.DEBUG, LogType.PAY,
						  "HttpClient.writeRequest(): POSTing data: " + data
						  );

			m_dataOS.writeBytes("Content-Length: " + data.length() + "\r\n");
			m_dataOS.writeBytes("\r\n");
			m_dataOS.writeBytes(data);
		}
		else
		{
			m_dataOS.writeBytes("\r\n");
		}
		m_dataOS.flush();
	}

	/**
	 * Einlesen der Http-Antwort.
	 *
	 * @return Die im Body der Antwort enthaltenen Daten als String
	 * @throws IOException
	 */
	public String readAnswer() throws IOException
	{
		int contentLength = -1;
		byte[] data = null;
		int index;
		String line = readLine(m_dataIS);

		if ( (index = line.indexOf(" ")) == -1)
		{
			throw new IOException("Wrong Header");
		}
		line = line.substring(index + 1);
		if ( (index = line.indexOf(" ")) == -1)
		{
			throw new IOException("Wrong Header");
		}
		String Status = line.substring(0, index);
		String statusString = line.substring(index + 1);
		while ( (line = readLine(m_dataIS)).length() != 0)
		{
			if ( (index = line.indexOf(" ")) == -1)
			{
				throw new IOException("Wrong Header");
			}
			String headerField = line.substring(0, index);
			String headerValue = line.substring(index + 1).trim();
			if (headerField.equalsIgnoreCase("Content-length:"))
			{
				contentLength = Integer.parseInt(headerValue);
			}
		}

		if (contentLength > 0)
		{
			if (contentLength > 10000)
			{
				throw new IOException("Communication Error");
			}

			data = new byte[contentLength];

			int pos = 0;
			int ret = 0;
			do
			{
				ret = m_dataIS.read(data, pos, contentLength - pos);
				if (ret == -1)
				{
					break;
				}
				pos += ret;
			}
			while (pos < contentLength);
		}
		if (!Status.equals("200"))
		{
			if (Status.equals("409"))
			{
				String descstr;
				try
				{
					XMLDescription desc = new XMLDescription(data);
					descstr = desc.getDescription();
				}
				catch (Exception e)
				{
					descstr = "Unkown Error";
				}

				throw new IOException(descstr);
			}
			throw new IOException(statusString);
		}

		LogHolder.log(LogLevel.DEBUG, LogType.PAY,
					  "HttpClient.readAnswer(): received: " + new String(data)
					  );

		return new String(data);

	}

	/**
	 * Hilfsfunktion zum Einlesen einer Textzeile.
	 *
	 * @param inputStream Eingabedatenstrom
	 * @return Textzeile
	 * @throws IOException
	 */
	private String readLine(DataInputStream inputStream) throws IOException
	{
		StringBuffer buff = new StringBuffer(256);
		int count = 0;
		try
		{
			int byteRead = inputStream.readByte();
			while (byteRead != 10 && byteRead != -1)
			{
				if (byteRead != 13)
				{
					count++;
					if (count > MAX_LINE_LENGTH)
					{
						throw new IOException("line to long");
					}
					buff.append( (char) byteRead);
				}
				byteRead = inputStream.read();
			}
		}
		catch (IOException e)
		{
			throw e;
		}
		return buff.toString();
	}

}
