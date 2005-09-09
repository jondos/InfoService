package jpi;

import java.net.*;
import anon.tor.tinytls.*;
import java.io.*;
import logging.*;
import anon.crypto.*;

/**
 * Serversocket der Bezahlinstanz.
 *
 * @author Bastian Voigt
 */
public class PIServer implements Runnable
{
	private ServerSocket m_serverSocket;
	private boolean m_typeAI;
	private int m_listenPort;

	/**
	 * Konstruktor f\uFFFDr Serversocket. Es gibt zwei Arten von Serversockets:
	 * ein Socket f\uFFFDr die Verbindungen vom JAP, der andere Socket f\uFFFDr die
	 * Verbindungen von den Abrechnungsinstanzen. Wenn <b>typeai</b> gleich
	 * <b>true</b> wird ein Abrechnungsinstanz-Socket erzeugt.
	 *
	 * @param typeai Socket f\uFFFDr AI- oder JAP-Verbindungen? (true = AI)
	 * @param sslOn SSL ein ?
	 */
	public PIServer(boolean typeai, boolean sslOn)
	{
		if (typeai)
		{
			m_listenPort = Configuration.getAIPort();
		}
		else
		{
			m_listenPort = Configuration.getJAPPort();
		}
		m_typeAI = typeai;
	}

	/**
	 * Erzeugt Socket, wartet auf eingehende Verbindungen und startet bei
	 * neuen Verbindungen eine {@link PIConnection} in einem neuen Thread.
	 */
	public void run()
	{
		try
		{
			if (Configuration.isTLSEnabled())
			{
				LogHolder.log(LogLevel.DEBUG, LogType.PAY, "PIServer starting up on port " + m_listenPort+". TLS is on");
				TinyTLSServer tlssock = new TinyTLSServer(m_listenPort);
				tlssock.setDSSParameters(Configuration.getOwnCertificate(), (MyDSAPrivateKey)Configuration.getPrivateKey());
				m_serverSocket = tlssock;
			}
			else
			{
				LogHolder.log(LogLevel.DEBUG, LogType.PAY, "PIServer starting up on port " + m_listenPort+". TLS is OFF");
				m_serverSocket = new ServerSocket(m_listenPort);
			}
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, e);
			return;
		}

		Socket acceptedSocket;
		JPIConnection con;
		while (true)
		{
			try
			{
				LogHolder.log(LogLevel.DEBUG, LogType.PAY, "JPIServer: waiting for conn");
				acceptedSocket = m_serverSocket.accept();
				LogHolder.log(LogLevel.DEBUG, LogType.PAY,
							  "JPIServer: connection from " + acceptedSocket.getInetAddress().toString());

				con = new JPIConnection(acceptedSocket, m_typeAI);
				new Thread(con).start();
			}
			catch (IOException e)
			{
				LogHolder.log(LogLevel.ALERT, LogType.PAY, "Class Server died: " + e.getMessage());
				LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, e);
				return;
			}
		}
	}
}
