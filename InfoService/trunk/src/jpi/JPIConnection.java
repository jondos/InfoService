package jpi;

import java.net.*;
import jpi.util.*;
import java.io.*;
import logging.*;
import anon.util.*;

/**
 * Bedient eine Verbindung innerhalb eines Threads. Die Http-Requests werden
 * mit Hilfe der Klasse {@link HttpServer} geparst und die Kommandos an
 * {@link PICommandUser} (bei einer JAP-Verbindung) bzw. an {@link PICommandAI}
 * (bei einer AI-Verbindung} weitergereicht und deren Antworten zur\uFFFDckgesendet.
 *
 * @author Andreas Mueller, Bastian Voigt
 */
public class JPIConnection implements Runnable
{

	private boolean m_bIsAI;
//	private String aiName;

	/** the socket */
	private Socket m_socket;


	private DataInputStream m_inStream;
	private DataOutputStream m_outStream;

	/**
	 * Creates a new JPIConnection over a plaintext-socket
	 * @param socket Socket
	 * @param type AI- oder JAP-Verbindung (true = AI)
	 */
	public JPIConnection(Socket socket, boolean bIsAI) throws IOException
	{
		m_socket = socket;
		m_inStream = new DataInputStream(socket.getInputStream());
		m_outStream = new DataOutputStream(socket.getOutputStream());
		m_bIsAI = bIsAI;
	}


	public void run()
	{
		PICommand command = null;
		try
		{
			HttpServer server = new HttpServer(m_inStream, m_outStream);
			PIRequest request;
			if (!m_bIsAI)
			{
				LogHolder.log(LogLevel.DEBUG, LogType.PAY,"Jap connected");
				command = (PICommand)new PICommandUser();
			}
			else
			{
				LogHolder.log(LogLevel.DEBUG, LogType.PAY,"AI connected");

				/* some ssl-stuff: verify AI-name */
				/*				try
					{
				 X509Certificate[] certs = ( (SSLSocket) m_sslSocket).getSession().getPeerCertificateChain();
					   aiName = certs[0].getSubjectDN().getName();
					}
					catch (Exception e)
					{
					 LogHolder.log(LogLevel.DEBUG, LogType.PAY,"Could not get the peer's name from the SSL certificate");
					 LogHolder.log(LogLevel.EXCEPTION, LogType.PAY,e);
					 //return;
					}*/
				command = (PICommand)new PICommandAI();
			}

			PIAnswer answer;
			while (true)
			{
				try
				{
					request = server.parseRequest();
					if (request == null)
					{
						break;
					}

					answer = command.next(request);
					LogHolder.log(LogLevel.DEBUG, LogType.MISC, "Now sending answer: "+XMLUtil.toString(XMLUtil.toXMLDocument(answer.getContent())));
					server.writeAnswer(200, answer.getContent());
					if (answer.getType() == PIAnswer.TYPE_CLOSE)
					{
						break;
					}
				}
				catch (Exception e)
				{
					LogHolder.log(LogLevel.EXCEPTION, LogType.PAY,e);
					break;
				}
			}

			try
			{
				LogHolder.log(LogLevel.DEBUG, LogType.MISC, (m_bIsAI?"AI":"Jap")+" disconnected");
				m_socket.shutdownOutput(); // make sure all pending data is sent before closing
				m_socket.close();
			}
			catch (SocketException se)
			{
				LogHolder.log(LogLevel.DEBUG, LogType.PAY,"While closing http connection: " + se.getMessage());
			}

		}
		catch (Exception ie)
		{
			LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, ie);
		}
	}

}
