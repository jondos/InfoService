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
package anon.server.impl;

import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.SocketException;
import HTTPClient.Codecs;
import anon.infoservice.ImmutableProxyInterface;
import anon.infoservice.ListenerInterface;
import anon.server.AnonServiceImpl;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import anon.infoservice.*;

final public class ProxyConnection
{
	private final static int FIREWALL_METHOD_HTTP_1_1 = 11;
	private final static int FIREWALL_METHOD_HTTP_1_0 = 10;
	private static final String CRLF = "\r\n";

	private Socket m_ioSocket;
	private InputStream m_In;
	private OutputStream m_Out;

	private ImmutableProxyInterface m_proxyInterface;

	/** @todo use HTTPConnectionFactory */
	public ProxyConnection(ImmutableProxyInterface a_proxyInterface,
						   String host, int port) throws Exception
	{
		if (a_proxyInterface == null)
		{
			m_ioSocket = new Socket(host, port);
		}
		else
		{
			m_proxyInterface = a_proxyInterface;
			LogHolder.log(LogLevel.DEBUG, LogType.NET,
						  "ProxyConnection: Try to connect via Firewall (" +
						  a_proxyInterface.getHost() + ":" + a_proxyInterface.getPort() +
						  ") to Server (" + host + ":" + port + ")");
			m_ioSocket = new Socket(a_proxyInterface.getHost(), a_proxyInterface.getPort());
		}
		m_In = m_ioSocket.getInputStream();
		m_Out = m_ioSocket.getOutputStream();

		if (a_proxyInterface != null)
		{
			m_ioSocket.setSoTimeout(60000);
			if (a_proxyInterface.getProtocol().equals(ListenerInterface.PROTOCOL_TYPE_SOCKS))
		{
			doSOCKS(host, port);
		}
			else if (a_proxyInterface.getProtocol().equals(ListenerInterface.PROTOCOL_TYPE_HTTP))
		{
			OutputStreamWriter writer = new OutputStreamWriter(m_Out);
			try
			{
					sendHTTPProxyCommands(FIREWALL_METHOD_HTTP_1_1, writer, host, port, a_proxyInterface);
			}
			catch (Exception e1)
			{
					sendHTTPProxyCommands(FIREWALL_METHOD_HTTP_1_0, writer, host, port, a_proxyInterface);
			}
			String tmp = readLine(m_In);
			LogHolder.log(LogLevel.DEBUG, LogType.NET, "ProxyConnection: Firewall response is: " + tmp);
			if (tmp.indexOf("200") != -1)
			{
				while (! (tmp = readLine(m_In)).equals(""))
				{
					LogHolder.log(LogLevel.DEBUG, LogType.NET,
								  "ProxyConnection: Firewall response is: " + tmp);
				}
			}
			else
			{
				throw new Exception("HTTP-Proxy response: " + tmp);
			}
		}
		}
		m_ioSocket.setSoTimeout(0);
	}

	/**
	 * Returns an immutable proxy interface.
	 * @return IConstProxyInterface
	 */
	public ImmutableProxyInterface getProxyInterface()
	{
		return m_proxyInterface;
	}

	private void doSOCKS(String host, int port) throws Exception
	{
		byte[] buff = new byte[10 + host.length()];
		buff[0] = 5; //SOCKS Version 5
		buff[1] = 1; //NO Auth
		buff[2] = 0;

		buff[3] = 5;
		buff[4] = 1; //CMD=Connect
		buff[5] = 0; //RSV
		buff[6] = 3; //Addr=Host-String
		buff[7] = (byte) host.length();

		System.arraycopy(host.getBytes(), 0, buff, 8, host.length());
		buff[8 + host.length()] = (byte) (port >> 8);
		buff[9 + host.length()] = (byte) (port & 0xFF);
		m_Out.write(buff, 0, 10 + host.length());
		m_Out.flush();
		//read OK for Methods...
		int ret=m_In.read(); //Version=5
		ret=m_In.read(); //00=No Auth
		//read ok for connect
		ret=m_In.read();//Version=5
		ret=m_In.read();//SUCCED=0
		ret=m_In.read();//reserved==0;
		int adrType=m_In.read();
		int len=0;
		switch(adrType)
		{
			case 1:
				len=4; //IPv4
			break;
			case 3:
				len=m_In.read(); //domainname len
			break;
			default: //unknown
				throw new Exception("Socks: unknow adr type in reply!");
		}
		len+=2; //port
		while (len > 0)
		{
			len -= m_In.read(buff, 0, len);
		}

	}

	public Socket getSocket()
	{
		return m_ioSocket;
	}

	public InputStream getInputStream()
	{
		return m_In;
	}

	public OutputStream getOutputStream()
	{
		return m_Out;
	}

	public void setSoTimeout(int ms) throws SocketException
	{
		m_ioSocket.setSoTimeout(ms);
	}

	public void close()
	{
		try
		{
			m_In.close();
		}
		catch (Exception e)
		{}
		try
		{
			m_Out.close();
		}
		catch (Exception e)
		{}
		try
		{
			m_ioSocket.close();
		}
		catch (Exception e)
		{}
	}

	/**
	 *
	  Write stuff for connecting over proxy/firewall
	// should look like this example
	//   CONNECT www.inf.tu-dresden.de:443 HTTP/1.0
	//   Connection: Keep-Alive
	//   Proxy-Connection: Keep-Alive
	//differs a little bit for HTTP/1.0 and HTTP/1.1
	 @todo use HTTPConnectionFactory to create a connection!!
		*/
	private void sendHTTPProxyCommands(int httpMethod, OutputStreamWriter out, String host, int port,
									   ImmutableProxyInterface a_proxyInterface) throws Exception
	{
		if (httpMethod == FIREWALL_METHOD_HTTP_1_1)
		{
			out.write("CONNECT " + host + ":" + Integer.toString(port) + " HTTP/1.1" + CRLF);
		}
		else
		{
			out.write("CONNECT " + host + ":" + Integer.toString(port) + " HTTP/1.0" + CRLF);
		}
		if (a_proxyInterface.isAuthenticationUsed()) // proxy authentication required...
		{
			out.write(a_proxyInterface.getProxyAuthorizationHeaderAsString());
		}
		out.write("Connection: Keep-Alive" + CRLF);
		out.write("Keep-Alive: max=20, timeout=100" + CRLF);
		out.write("Proxy-Connection: Keep-Alive" + CRLF);
		out.write(CRLF);
		out.flush();
	}

	private String readLine(InputStream inputStream) throws Exception
	{
		StringBuffer strBuff = new StringBuffer(256);
		try
		{
			int byteRead = inputStream.read();
			while (byteRead != 10 && byteRead != -1)
			{
				if (byteRead != 13)
				{
					strBuff.append( (char) byteRead);
				}
				byteRead = inputStream.read();
			}
		}
		catch (Exception e)
		{
			throw e;
		}
		return strBuff.toString();
	}
}
