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

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Vector;
import java.util.Locale;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.TimeZone;
import java.util.Vector;

import anon.crypto.JAPCertificate;
import anon.crypto.PKCS12;
import anon.crypto.SignatureCreator;
import anon.crypto.SignatureVerifier;
import anon.infoservice.Constants;
import anon.infoservice.ListenerInterface;
import anon.util.ResourceLoader;
import infoservice.tor.TorDirectoryAgent;
import infoservice.tor.TorDirectoryServer;
import infoservice.tor.TorDirectoryServerUrl;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import infoservice.tor.MixminionDirectoryAgent;

public class Configuration
{
	/**
	 * Stores the instance of Configuration (Singleton).
	 */
	private static Configuration configurationInstance = null;

	/**
	 * Returns the instance of Configuration (Singleton).
	 * @return The Configuration instance.
	 */
	public static Configuration getInstance()
	{
		return configurationInstance;
	}

	/**
	 * Stores the ListenerInterfaces of all neighbour infoservices declared in the config file.
	 */
	private Vector m_initialNeighbourInfoServices;

	/**
	 * Stores the ListenerInterfaces of all interfaces our infoservice is bound to.
	 */
	private Vector m_hardwareListenerList;

	/**
	 * Stores the ListenerInterfaces of all interfaces our infoservice propagates to others.
	 */
	private Vector m_virtualListenerList;

	/**
	 * Stores the name of our infoservice. The name is just for comfort reasons and has no
	 * importance.
	 */
	private String m_strOwnName;

	/**
	 * Maximum size in bytes of HTTP POST data. We will not accept the post of longer messages.
	 * Longer messages will be thrown away.
	 */
	private int m_iMaxPostContentLength;

	/**
	 * This stores, whether status statistics are enabled (true) or not (false).
	 */
	private boolean m_bStatusStatisticsEnabled;

	/**
	 * Stores the interval (in ms, see System.currentTimeMillis()) between two status statistics.
	 */
	private long m_lStatusStatisticsInterval;

	/**
	 * Stores the directory, where to write the status statistics files. The name must end with a
	 * slash or backslash (WINDOWS).
	 */
	private String m_strStatusStatisticsLogDir;

	/**
	 * Stores the addresses of the proxy servers at the end of the cascades. This is only for
	 * compatibility and will be removed soon.
	 */
	private String m_strProxyAddresses;

	/**
	 * Stores, whether the Signatures of InfoService Messages are verified.
	 */
	private boolean m_bCheckInfoServiceSignatures = true;

	/**
	 * Stores the date format information for HTTP headers.
	 */
	private SimpleDateFormat m_httpDateFormat;

	/**
	 * Stores, whether we are the "root" of the JAP update information (JNLP-Files + JAPMinVersion).
	 */
	private boolean m_bRootOfUpdateInformation;

	/**
	 * Stores where the japRelease.jnlp is located in the local file system (path + filename).
	 */
	private String m_strJapReleaseJnlpFile;

	/**
	 * Stores where the japDevelopment.jnlp is located in the local file system (path + filename).
	 */
	private String m_strJapDevelopmentJnlpFile;

	/**
	 * Stores where the japMinVersion.xml is located in the local file system (path + filename).
	 */
	private String m_strJapMinVersionFile;

	/**
	 * Stores the startup time of the infoservice (time when the configuration instance was
	 * created).
	 */
	private Date m_startupTime;

	/**
	 * Stores whether this infoservice should hold a list with JAP forwarders (true) or not (false).
	 */
	private boolean m_holdForwarderList;

	public Configuration(Properties a_properties) throws Exception
	{
		/* for running in non-graphic environments, we need the awt headless support, it is only
		 * available since Java 1.4, so running the infoservice in a non-graphic environment is
		 * only possible with Java versions since 1.4
		 */
		/* set the property in Java 1.0 compatible style */
		Properties systemProperties = System.getProperties();
		systemProperties.put("java.awt.headless", "true");
		System.setProperties(systemProperties);

		configurationInstance = this;
		m_startupTime = new Date();
		try
		{
			LogHolder.setLogInstance(new InfoServiceLog(a_properties));
			LogHolder.setDetailLevel(Integer.parseInt(a_properties.getProperty("messageDetailLevel", "0").
				trim()));
			m_strOwnName = a_properties.getProperty("ownname").trim();
			m_iMaxPostContentLength = Integer.parseInt(a_properties.getProperty("maxPOSTContentLength").trim());
			String strHardwareListeners = a_properties.getProperty("HardwareListeners").trim();
			String strVirtualListeners = a_properties.getProperty("VirtualListeners").trim();
			StringTokenizer stHardware = new StringTokenizer(strHardwareListeners, ",");
			StringTokenizer stVirtual = new StringTokenizer(strVirtualListeners, ",");

			/* create a list of all interfaces we are listening on */
			m_hardwareListenerList = new Vector();
			while (stHardware.hasMoreTokens())
			{
				StringTokenizer stCurrentInterface = new StringTokenizer(stHardware.nextToken(), ":");
				String inetHost = stCurrentInterface.nextToken().trim();
				int inetPort = Integer.parseInt(stCurrentInterface.nextToken().trim());
				m_hardwareListenerList.addElement(new ListenerInterface(inetHost, inetPort));
			}
			m_virtualListenerList = new Vector();
			while (stVirtual.hasMoreTokens())
			{
				StringTokenizer stCurrentInterface = new StringTokenizer(stVirtual.nextToken(), ":");
				String inetHost = stCurrentInterface.nextToken().trim();
				int inetPort = Integer.parseInt(stCurrentInterface.nextToken().trim());
				m_virtualListenerList.addElement(new ListenerInterface(
					inetHost, inetPort, ListenerInterface.PROTOCOL_TYPE_HTTP));
			}

			/* only for compatibility */
			m_strProxyAddresses = a_properties.getProperty("proxyAddresses").trim();

			/* load the private key for signing our own infoservice messages */
			String privatePkcs12KeyFile = a_properties.getProperty("privateKeyFile");
			if ( (privatePkcs12KeyFile != null) && (!privatePkcs12KeyFile.trim().equals("")))
			{
				privatePkcs12KeyFile = privatePkcs12KeyFile.trim();
				PKCS12 infoServiceMessagesPrivateKey = null;
				try
				{
					String lastPassword = "";
					do
					{
						infoServiceMessagesPrivateKey = loadPkcs12PrivateKey(privatePkcs12KeyFile,
							lastPassword);
						if (infoServiceMessagesPrivateKey == null)
						{
							/* file was found, but the private key could not be loaded -> maybe wrong password */
							System.out.println(
								"Cannot load private key! Enter password for private key from file: " +
								privatePkcs12KeyFile);
							System.out.print("Password: ");
							BufferedReader passwordReader = new BufferedReader(new InputStreamReader(System.
								in));
							lastPassword = passwordReader.readLine();
						}
					}
					while (infoServiceMessagesPrivateKey == null);
					/* we have loaded the private key for signing our own infoservice messages -> put it in
					 * the SignatureCreator
					 */
					SignatureCreator.getInstance().setSigningKey(SignatureVerifier.DOCUMENT_CLASS_INFOSERVICE,
						infoServiceMessagesPrivateKey);
				}
				catch (FileNotFoundException e)
				{
					System.out.println("Cannot find the private key file: " + privatePkcs12KeyFile);
					System.out.println("Exiting...");
					throw (e);
				}
			}
			else
			{
				LogHolder.log(LogLevel.WARNING, LogType.MISC,
							  "No private key for signing the own infoservice entry specified. Unsigned messages will be sent.");
			}

			/* whether to check signatures or not (default is enabled signature verification) */
			SignatureVerifier.getInstance().setCheckSignatures(true);
			String checkSignatures = a_properties.getProperty("checkSignatures");
			if (checkSignatures != null)
			{
				if (checkSignatures.equalsIgnoreCase("false"))
				{
					SignatureVerifier.getInstance().setCheckSignatures(false);
					LogHolder.log(LogLevel.WARNING, LogType.MISC,
								  "Disabling signature verification for all documents.");
				}
			}
			if (SignatureVerifier.getInstance().isCheckSignatures())
			{
				LogHolder.log(LogLevel.DEBUG, LogType.MISC,
							  "Signature verification is enabled, loading certificates...");
				/* load the root certificates */
				String trustedRootCertificateFiles = a_properties.getProperty("trustedRootCertificateFiles");
				if ( (trustedRootCertificateFiles != null) && (!trustedRootCertificateFiles.trim().equals("")))
				{
					StringTokenizer stTrustedRootCertificates = new StringTokenizer(
						trustedRootCertificateFiles.trim(), ",");
					while (stTrustedRootCertificates.hasMoreTokens())
					{
						String currentCertificateFile = stTrustedRootCertificates.nextToken().trim();
						JAPCertificate currentCertificate = loadX509Certificate(currentCertificateFile);
						if (currentCertificate != null)
						{
							SignatureVerifier.getInstance().getVerificationCertificateStore().
								addCertificateWithoutVerification(currentCertificate,
								JAPCertificate.CERTIFICATE_TYPE_ROOT_MIX, true, false);
							LogHolder.log(LogLevel.WARNING, LogType.MISC,
										  "Added the following file to the store of trusted root certificates: " +
										  currentCertificateFile);
						}
						else
						{
							LogHolder.log(LogLevel.ERR, LogType.MISC,
										  "Error loading trusted root certificate: " + currentCertificateFile);
						}
					}
				}
				else
				{
					LogHolder.log(LogLevel.WARNING, LogType.MISC, "No trusted root certificates specified.");
				}
				/* load the infoservice certificates */
				LogHolder.log(LogLevel.WARNING, LogType.MISC,
							  "Try to load trusted InfoService certificates specified.");
				String trustedInfoServiceCertificateFiles = a_properties.getProperty(
					"trustedInfoServiceCertificateFiles");
				if ( (trustedInfoServiceCertificateFiles != null) &&
					(!trustedInfoServiceCertificateFiles.trim().equals("")))
				{
					StringTokenizer stTrustedInfoServiceCertificates = new StringTokenizer(
						trustedInfoServiceCertificateFiles.trim(), ",");
					LogHolder.log(LogLevel.WARNING, LogType.MISC,
								  "trustedInfoServiceCertificateFiles: " + trustedInfoServiceCertificateFiles);
					while (stTrustedInfoServiceCertificates.hasMoreTokens())
					{
						String currentCertificateFile = stTrustedInfoServiceCertificates.nextToken().trim();
						JAPCertificate currentCertificate = loadX509Certificate(currentCertificateFile);
						if (currentCertificate != null)
						{
							SignatureVerifier.getInstance().getVerificationCertificateStore().
								addCertificateWithoutVerification(currentCertificate,
								JAPCertificate.CERTIFICATE_TYPE_INFOSERVICE, true, false);
							LogHolder.log(LogLevel.DEBUG, LogType.MISC,
										  "Added the following file to the store of trusted infoservice certificates: " +
										  currentCertificateFile);
						}
						else
						{
							LogHolder.log(LogLevel.ERR, LogType.MISC,
										  "Error loading trusted infoservice certificate: " +
										  currentCertificateFile);
						}
					}
				}
				else
				{
					LogHolder.log(LogLevel.DEBUG, LogType.MISC,
								  "No trusted infoservice certificates specified.");
				}
				/* load the mix certificates */
				String trustedMixCertificateFiles = a_properties.getProperty("trustedMixCertificateFiles");
				if ( (trustedMixCertificateFiles != null) && (!trustedMixCertificateFiles.trim().equals("")))
				{
					StringTokenizer stTrustedMixCertificates = new StringTokenizer(trustedMixCertificateFiles.
						trim(), ",");
					while (stTrustedMixCertificates.hasMoreTokens())
					{
						String currentCertificateFile = stTrustedMixCertificates.nextToken().trim();
						JAPCertificate currentCertificate = loadX509Certificate(currentCertificateFile);
						if (currentCertificate != null)
						{
							SignatureVerifier.getInstance().getVerificationCertificateStore().
								addCertificateWithoutVerification(currentCertificate,
								JAPCertificate.CERTIFICATE_TYPE_MIX, true, false);
							LogHolder.log(LogLevel.DEBUG, LogType.MISC,
										  "Added the following file to the store of trusted mix certificates: " +
										  currentCertificateFile);
						}
						else
						{
							LogHolder.log(LogLevel.ERR, LogType.MISC,
										  "Error loading trusted mix certificate: " + currentCertificateFile);
						}
					}
				}
				else
				{
					LogHolder.log(LogLevel.DEBUG, LogType.MISC, "No trusted mix certificates specified.");
				}
				/* load the update certificates */
				String trustedUpdateCertificateFiles = a_properties.getProperty(
					"trustedUpdateCertificateFiles");
				if ( (trustedUpdateCertificateFiles != null) &&
					(!trustedUpdateCertificateFiles.trim().equals("")))
				{
					StringTokenizer stTrustedUpdateCertificates = new StringTokenizer(
						trustedUpdateCertificateFiles.trim(), ",");
					while (stTrustedUpdateCertificates.hasMoreTokens())
					{
						String currentCertificateFile = stTrustedUpdateCertificates.nextToken().trim();
						JAPCertificate currentCertificate = loadX509Certificate(currentCertificateFile);
						if (currentCertificate != null)
						{
							SignatureVerifier.getInstance().getVerificationCertificateStore().
								addCertificateWithoutVerification(currentCertificate,
								JAPCertificate.CERTIFICATE_TYPE_UPDATE, true, false);
							LogHolder.log(LogLevel.DEBUG, LogType.MISC,
										  "Added the following file to the store of trusted debug certificates: " +
										  currentCertificateFile);
						}
						else
						{
							LogHolder.log(LogLevel.ERR, LogType.MISC,
										  "Error loading trusted update certificate: " +
										  currentCertificateFile);
						}
					}
				}
				else
				{
					LogHolder.log(LogLevel.WARNING, LogType.MISC, "No trusted update certificates specified.");
				}

				m_bCheckInfoServiceSignatures = true;
				try
				{
					String b = a_properties.getProperty("checkInfoServiceSignatures").trim();
					if (b.equalsIgnoreCase("false"))
					{
						m_bCheckInfoServiceSignatures = false;

					}
				}
				catch (Exception e)
				{
					LogHolder.log(LogLevel.WARNING, LogType.MISC,
								  "Could not read 'checkInfoServiceSignatures' setting - default to: " +
								  m_bCheckInfoServiceSignatures);
				}
				/* start the certificate manager, which manages the appended certificates of the
				 * MixCascade entries for verification of the StatusInfo entries
				 */
				new CertificateManager();
			}

			/* get the JAP update information persistence settings */
			m_bRootOfUpdateInformation = a_properties.getProperty("rootOfUpdateInformation").trim().
				equalsIgnoreCase("true");
			if (m_bRootOfUpdateInformation)
			{
				m_strJapReleaseJnlpFile = a_properties.getProperty("japReleaseFileName").trim();
				m_strJapDevelopmentJnlpFile = a_properties.getProperty("japDevelopmentFileName").trim();
				m_strJapMinVersionFile = a_properties.getProperty("japMinVersionFileName").trim();
				/* load the private key for signing our own infoservice messages */
				String updatePkcs12KeyFile = a_properties.getProperty("updateInformationPrivateKey");
				if ( (updatePkcs12KeyFile != null) && (!updatePkcs12KeyFile.trim().equals("")))
				{
					updatePkcs12KeyFile = updatePkcs12KeyFile.trim();
					PKCS12 updateMessagesPrivateKey = null;
					try
					{
						String lastPassword = "";
						do
						{
							updateMessagesPrivateKey = loadPkcs12PrivateKey(updatePkcs12KeyFile, lastPassword);
							if (updateMessagesPrivateKey == null)
							{
								/* file was found, but the private key could not be loaded -> maybe wrong password */
								System.out.println(
									"Cannot load private key! Enter password for private key from file: " +
									updatePkcs12KeyFile);
								System.out.print("Password: ");
								BufferedReader passwordReader = new BufferedReader(new InputStreamReader(
									System.in));
								lastPassword = passwordReader.readLine();
							}
						}
						while (updateMessagesPrivateKey == null);
						/* we have loaded the private key for signing the update messages -> put it in the
						 * SignatureCreator
						 */
						SignatureCreator.getInstance().setSigningKey(SignatureVerifier.DOCUMENT_CLASS_UPDATE,
							updateMessagesPrivateKey);
					}
					catch (FileNotFoundException e)
					{
						System.out.println("Cannot find the private key file: " + updatePkcs12KeyFile);
						System.out.println("Exiting...");
						throw (e);
					}
				}
				else
				{
					LogHolder.log(LogLevel.WARNING, LogType.MISC,
								  "No private key for signing the update messages specified. Unsigned messages will be sent.");
				}
			}
			else
			{
				m_strJapReleaseJnlpFile = null;
				m_strJapDevelopmentJnlpFile = null;
				m_strJapMinVersionFile = null;
			}
			/* start the UpdateInformationHandler announce thread */
			UpdateInformationHandler.getInstance();

			/* Create the list of all neighbour infoservices. So we know, where to announce ourself at
			 * startup.
			 */
			StringTokenizer stNeighbours = new StringTokenizer(a_properties.getProperty("neighbours").trim(),
				",");
			m_initialNeighbourInfoServices = new Vector();
			while (stNeighbours.hasMoreTokens())
			{
				try
				{
					StringTokenizer stCurrentInterface = new StringTokenizer(stNeighbours.nextToken(), ":");
					String inetHost = stCurrentInterface.nextToken();
					int inetPort = Integer.parseInt(stCurrentInterface.nextToken());
					m_initialNeighbourInfoServices.addElement(new ListenerInterface(inetHost, inetPort));
				}
				catch (Exception e)
				{
					/* simply don't use this neighbour */
				}
			}

			/* get the settings for status statistics */
			m_bStatusStatisticsEnabled = a_properties.getProperty("statusStatistics").trim().equalsIgnoreCase(
				"enabled");
			/* set some default values */
			m_lStatusStatisticsInterval = 3600 * (long) (1000); // 1 hour
			m_strStatusStatisticsLogDir = ""; // log to the current directory
			if (m_bStatusStatisticsEnabled == true)
			{
				/* overwrite the default values */
				long tempInterval = Long.parseLong(a_properties.getProperty("statusStatisticsInterval").trim()) *
					60 *
					1000;
				if (tempInterval > 0)
				{
					/* set only to valid values */
					m_lStatusStatisticsInterval = tempInterval;
				}
				m_strStatusStatisticsLogDir = a_properties.getProperty("statusStatisticsLogDir").trim();
			}

			/* get the settings for fetching the tor nodes list */
			boolean fetchTorNodesList = a_properties.getProperty("fetchTorNodesList").trim().equalsIgnoreCase(
				"enabled");
			if (fetchTorNodesList == true)
			{
				/* set some default values */
				long fetchTorNodesListInterval = 600 * (long) 1000;
				/* overwrite the default values */
				long tempInterval = Long.parseLong(a_properties.getProperty("fetchTorNodesListInterval").trim()) *
					1000;
				if (tempInterval > 0)
				{
					/* set only to valid values */
					fetchTorNodesListInterval = tempInterval;
				}
				/* load the list of known tor directory servers */
				String torDirectoryServers = a_properties.getProperty("torDirectoryServers").trim();
				StringTokenizer stTorDirectoryServers = new StringTokenizer(torDirectoryServers, ",");
				while (stTorDirectoryServers.hasMoreTokens())
				{
					try
					{
						URL torDirectoryServer = new URL(stTorDirectoryServers.nextToken().trim());
						int torServerPort = torDirectoryServer.getPort();
						if (torServerPort == -1)
						{
							torServerPort = torDirectoryServer.getPort();
						}
						/* add the directory server with nearly infinite timeout (1000 years) */
						TorDirectoryAgent.getInstance().addTorDirectoryServer(new TorDirectoryServer(new
							TorDirectoryServerUrl(torDirectoryServer.getHost(), torServerPort,
												  torDirectoryServer.getFile()),
							(long) 1000 * 365 * 24 * 3600 * 1000, true));
					}
					catch (Exception e)
					{
						/* don't add the directory server to the database, because there was an error */
					}
				}
				/* start the update tor nodes list thread -> if the fetchTorNodesList value was false,
				 * the thread is never started -> we will never fetch the list
				 */
				TorDirectoryAgent.getInstance().startUpdateThread(fetchTorNodesListInterval);
			}

			/* get the settings for fetching the tor nodes list */
			String str = a_properties.getProperty("fetchMixminionNodesList");
			boolean fetchMixminionNodesList = false;
			if (str != null && str.trim().equalsIgnoreCase("enabled"))
			{
				fetchMixminionNodesList = true;
			}
			if (fetchMixminionNodesList)
			{
				/* set some default values */
				long fetchMixminionNodesListInterval = 600 * (long) 1000;
				/* overwrite the default values */
				long tempInterval = Long.parseLong(a_properties.getProperty("fetchMixminionNodesListInterval").
					trim()) *
					1000;
				if (tempInterval > 0)
				{
					/* set only to valid values */
					fetchMixminionNodesListInterval = tempInterval;
				}
				/* load the list of known tor directory servers */
				String mixminionDirectoryServers = a_properties.getProperty("mixminionDirectoryServers").trim();
				StringTokenizer stMixminionDirectoryServers = new StringTokenizer(mixminionDirectoryServers,
					",");
				while (stMixminionDirectoryServers.hasMoreTokens())
				{
					try
					{
						URL mixminionDirectoryServer = new URL(stMixminionDirectoryServers.nextToken().trim());
						MixminionDirectoryAgent.getInstance().addDirectoryServer(mixminionDirectoryServer);
					}
					catch (Exception e)
					{
						/* don't add the directory server to the database, because there was an error */
					}
				}
				/* start the update tor nodes list thread -> if the fetchTorNodesList value was false,
				 * the thread is never started -> we will never fetch the list
				 */
				MixminionDirectoryAgent.getInstance().startUpdateThread(fetchMixminionNodesListInterval);
			}
			/* get the JAP forwarder list settings */
			m_holdForwarderList = false;
			try
			{
				m_holdForwarderList = a_properties.getProperty("primaryForwarderList").trim().
					equalsIgnoreCase(
						"enabled");
			}
			catch (Exception e)
			{
				LogHolder.log(LogLevel.WARNING, LogType.MISC,
							  "Could not read 'primaryForwarderList' setting - default to: " +
							  m_holdForwarderList);
			}
			/* do some more initialization stuff */
			m_httpDateFormat = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss zzz", Locale.US);
			m_httpDateFormat.setTimeZone(TimeZone.getTimeZone("GMT"));

		}
		catch (Exception e)
		{
			System.err.println("Error reading configuration!");
			System.err.println("Exception: " + e.toString());
			throw e;
		}
	}

	/**
	 * Returns the list with ListenerInterfaces of all neighbour infoservices from
	 * the config file.
	 *
	 * @return Vector with ListenerInterfaces of initial neighbours.
	 */
	public Vector getInitialNeighbourInfoServices()
	{
		return m_initialNeighbourInfoServices;
	}

	/**
	 * Returns the ListenerInterfaces of all Interfaces our infoservice is
	 * locally bound to.
	 *
	 * @return the ListenerInterfaces of bound interfaces.
	 */
	public Vector getHardwareListeners()
	{
		return m_hardwareListenerList;
	}

	/**
	 * Returns the ListenerInterfaces of all Interfaces our infoservice
	 * propagates to others.
	 *
	 * @return the ListenerInterfaces of propagated interfaces.
	 */
	public Vector getVirtualListeners()
	{
		return m_virtualListenerList;
	}

	/**
	 * Returns the name of our infoservice. The name is just for comfort reasons and has no
	 * importance.
	 *
	 * @return The name of our infoservice from config file.
	 */
	public String getOwnName()
	{
		return m_strOwnName;
	}

	/**
	 * Returns the maximum HTTP POST data size which will be accepted. We throw away longer POST
	 * messages.
	 *
	 * @return The maximum HTTP POST data size we accept.
	 */
	public int getMaxPostContentLength()
	{
		return m_iMaxPostContentLength;
	}

	/**
	 * This returns, whether status statistics are enabled (true) or not (false).
	 *
	 * @return True, if status statistics are enabled or false, if they are disabled.
	 */
	public boolean isStatusStatisticsEnabled()
	{
		return m_bStatusStatisticsEnabled;
	}

	/**
	 * Returns the interval (in ms, see System.currentTimeMillis()) between two status statistics.
	 * The value is only meaningful, if isStatusStatisticsEnabled() returns true.
	 *
	 * @return The status statistics interval.
	 */
	public long getStatusStatisticsInterval()
	{
		return m_lStatusStatisticsInterval;
	}

	/**
	 * Returns the directory where to log the status statistics. The directory name must end with
	 * a slash or backslash (WINDOWS). The value is only meaningful, if isStatusStatisticsEnabled()
	 * returns true.
	 *
	 * @return The directory, where to write the status statistics files.
	 */
	public String getStatusStatisticsLogDir()
	{
		return m_strStatusStatisticsLogDir;
	}

	/**
	 * Returns the addresses of the proxy servers at the end of the cascades. The info is from
	 * the proxyAddresses line of the properties file. This method is only for compatibility and
	 * will be removed soon.
	 *
	 * @return The addresses of the proxy servers.
	 */
	public String getProxyAddresses()
	{
		return m_strProxyAddresses;
	}

	/**
	 * Returns the HTTP-header date format information.
	 *
	 * @return The HTTP-header date format.
	 */
	public SimpleDateFormat getHttpDateFormat()
	{
		return m_httpDateFormat;
	}

	/**
	 * Returns, whether we are the root of the JAP update information (JNLP-Files + JAPMinVersion).
	 *
	 */
	public boolean isRootOfUpdateInformation()
	{
		return m_bRootOfUpdateInformation;
	}

	/**
	 * Returns, whether the Signatures of InfoService Messaegs should be checked.
	 *
	 */
	public boolean isInfoServiceMessageSignatureCheckEnabled()
	{
		return m_bCheckInfoServiceSignatures;
	}

	/**
	 * Returns where the japRelease.jnlp is located in the local file system (path + filename).
	 *
	 * @return The filename (maybe with path) of japRelease.jnlp.
	 */
	public String getJapReleaseJnlpFile()
	{
		return m_strJapReleaseJnlpFile;
	}

	/**
	 * Returns where the japDevelopment.jnlp is located in the local file system (path + filename).
	 *
	 * @return The filename (maybe with path) of japDevelopment.jnlp.
	 */
	public String getJapDevelopmentJnlpFile()
	{
		return m_strJapDevelopmentJnlpFile;
	}

	/**
	 * Returns where the file with JAP minimal version number is located in the local file system
	 * (path + filename).
	 *
	 * @return The filename (maybe with path) of japMinVersion.xml.
	 */
	public String getJapMinVersionFile()
	{
		return m_strJapMinVersionFile;
	}

	/**
	 * Returns the startup time of this infoservice.
	 *
	 * @return The time when this infoservices was started.
	 */
	public Date getStartupTime()
	{
		return m_startupTime;
	}

	/**
	 * Returns whether this infoservice holds a JAP forwarder list or not.
	 *
	 * @return True, if this infoservice has a primary forwarder list or false, if this infoservice
	 *         doesn't have a primary forwarder list and redirects all requests from blockees to
	 *         an infoservice with such a list (if we know one in the InfoserviceDatabase).
	 */
	public boolean holdForwarderList()
	{
		return m_holdForwarderList;
	}

	/**
	 * Loads a PKCS12 certificate from a file.
	 *
	 * @param a_pkcs12FileName The filename (with path) of the PKCS12 file.
	 * @param a_password The password for the PKCS12 file, if necessary. If no password is necessary,
	 *                   you can supply null or an empty string.
	 *
	 * @return The PKCS12 certificate structure including the private key or null, if the
	 *         certificate could not be loaded (invalid password or invalid data within the file).
	 *
	 * @throws FileNotFoundException If the file cannot be found in the filesystem.
	 */
	private PKCS12 loadPkcs12PrivateKey(String a_pkcs12FileName, String a_password) throws
		FileNotFoundException
	{
		PKCS12 loadedCertificate = null;
		if (a_password == null)
		{
			a_password = "";
		}
		try
		{
			loadedCertificate = PKCS12.getInstance(new FileInputStream(a_pkcs12FileName),
				a_password.toCharArray());
		}
		catch (FileNotFoundException fnfe)
		{
			/* we throw an exception, if the file was not found */
			throw (fnfe);
		}
		catch (Exception e)
		{
			/* do nothing and return null */
		}
		return loadedCertificate;
	}

	/**
	 * Loads a X509 certificate from a file.
	 *
	 * @param a_x509FileName The filename (with path) of the X509 file.
	 *
	 * @return The X509 certificate or null, if there was an error while loading the certificate
	 *         from the specified file.
	 */
	private JAPCertificate loadX509Certificate(String a_x509FileName)
	{
		return JAPCertificate.getInstance(new File(a_x509FileName));
	}

}
