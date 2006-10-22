/*
 Copyright (c) 2000 - 2004, The JAP-Team
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
package jpi;

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.util.Properties;
import anon.crypto.IMyPrivateKey;
import anon.crypto.JAPCertificate;
import anon.crypto.PKCS12;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import anon.pay.xml.XMLPaymentOptions;
import anon.pay.xml.XMLPaymentOption;
import java.util.Enumeration;
import java.util.Vector;
import java.util.StringTokenizer;
import anon.infoservice.ListenerInterface;
import anon.crypto.X509SubjectKeyIdentifier;

/**
 * Loads and stores all configuration data, keys
 * and certificates.
 */
public class Configuration
{
	/** Versionsnummer --> Please update if you change anything*/
	public static final String BEZAHLINSTANZ_VERSION = "BI.02.026";

	public static IMyPrivateKey getPrivateKey()
	{
		return m_privateKey;
	}

	/** private Constructor! */
	private Configuration()
	{}

	private static Configuration ms_Instance;

	/** return the one and only Configuration */
	public static Configuration getInstance()
	{
		if (ms_Instance == null)
		{
			ms_Instance = new Configuration();
		}
		return ms_Instance;
	}

	/** holds the public x509 certificate */
	private static JAPCertificate m_ownX509Certificate;

	/** returns the public x509 certificate */
	public static JAPCertificate getOwnCertificate()
	{
		return m_ownX509Certificate;
	}

	/** holds the JPI ID */
	private static String m_ID;

	/** returns the JPI ID */
	public static String getBiID()
	{
		return m_ID;
	}

	/** holds the JPI Name */
	private static String m_Name;

	/** returns the JPI Name */
	public static String getBiName()
	{
		if (m_Name == null)
		{
			return "Payment Instance " + m_ID;
		}
		return m_Name;
	}

	/** holds the database hostname */
	private static String m_dbHostname;

	/** returns the database hostname */
	public static String getDatabaseHost()
	{
		return m_dbHostname;
	}

	/** holds the database name */
	private static String m_dbDatabaseName;

	/** returns the database name */
	public static String getDatabaseName()
	{
		return m_dbDatabaseName;
	}

	/** holds the database username */
	private static String m_dbUsername;

	/** returns the database username */
	public static String getDatabaseUserName()
	{
		return m_dbUsername;
	}

	/** holds the database password */
	private static String m_dbPassword;

	/** returns the database password */
	public static String getDatabasePassword()
	{
		return m_dbPassword;
	}

	/** holds the database portnumber */
	private static int m_dbPort;

	/** returns the database portnumber */
	public static int getDatabasePort()
	{
		return m_dbPort;
	}

	/** Holds listener interfaces of infoservices */
	private static Vector ms_isListenerInterfaces = new Vector();

	/** Returns listener interfaces for infoservices */
	public static Enumeration getInfoservices()
	{
		return ms_isListenerInterfaces.elements();
	}

	/** holds the port where the JPI should listen for AI connections */
	private static int m_AIPort;

	/** returns the port where the JPI should listen for AI connections */
	public static int getAIPort()
	{
		return m_AIPort;
	}

	/** holds maximum concurrent connections for accounting instances*/
	private static int ms_aiConnections;

	/** returns maximum concurrent connections for accounting instances*/
	public static int getMaxAiConnections()
	{
		return ms_aiConnections;
	}

	/** holds maximum concurrent connections for japs (per interface!)*/
	private static int ms_japConnections;

	/** holds maximum concurrent connections for japs (per interface!)*/
	public static int getMaxJapConnections()
	{
		return ms_japConnections;
	}

	/** Holds listener interfaces for jap connections*/
	private static Vector ms_japListenerInterfaces = new Vector();

	/** Holds listener interface (only one!) for accounting instance connections*/
	private static ListenerInterface ms_aiListenerInterface;

	/** Holds the private key */
	private static IMyPrivateKey m_privateKey;

	/** Returns listener interfaces for jap connections*/
	public static Enumeration getJapListenerInterfaces()
	{
		return ms_japListenerInterfaces.elements();
	}

	/** Returns listener interface (only one!) for accounting instance connections*/
	public static ListenerInterface getAiListenerInterface()
	{
		return ms_aiListenerInterface;
	}

	/** Holds threshold for logging to stderr */
	private static int m_LogStderrThreshold;

	/** Returns threshold for logging to stderr */
	public static int getLogStderrThreshold()
	{
		return m_LogStderrThreshold;
	}

	/** Holds threshold for logging to log file */
	private static int m_LogFileThreshold;

	/** Returns threshold for logging to log file */
	public static int getLogFileThreshold()
	{
		return m_LogFileThreshold;
	}

	/** Holds log file name*/
	private static String m_LogFileName = null;

	/** Returns log file name*/
	public static String getLogFileName()
	{
		return m_LogFileName;
	}

	/** Holds the payment options*/
	private static XMLPaymentOptions ms_paymentOptions = new XMLPaymentOptions();

	/** Returns the payment options*/
	public static XMLPaymentOptions getPaymentOptions()
	{
		return ms_paymentOptions;
	}

	/** Holds the credit card helper class name. The class is named *CreditCardHelper.
	 * The configuration file must provide "*" (e.g. Dummy)
	 */
	private static String ms_creditCardHelper;

	/** Returns the credit card helper class name*/
	public static String getCreditCardHelper()
	{
		return ms_creditCardHelper;
	}

	/**Holds the port where the JPI receives PayPal IPNs */
	private static String ms_payPalport;

	/**Returns the port where the JPI receives PayPal IPNs */
	public static String getPayPalPort()
	{
		return ms_payPalport;
	}

	/** Holds the rate per megabyte (in cents) */
	private static double ms_ratePerMB;

	/** Returns the rate per megabyte (in cents) */
	public static double getRatePerMB()
	{
		return ms_ratePerMB;
	}

	/** Holds password for external charging */
	private static String ms_externalChargePassword;

	/** Returns password for external charging */
	public static String getExternalChargePassword()
	{
		return ms_externalChargePassword;
	}

	/**Holds the port for external charging */
	private static String ms_externalChargePort;

	/**Returns the port for external charging */
	public static String getExternalChargePort()
	{
		return ms_externalChargePort;
	}

	/** Path to keyfile */
	public static String ms_keyFile;
	/** Returns the path to the keyfile */
	public static String getKeyFile()
	{
		return ms_keyFile;
	}

	/** Keyfile password */
	public static String ms_keyFilePassword;
	/** Returns the keyfile password */
	public static String getKeyFilePassword()
	{
		return ms_keyFilePassword;
	}

	/**
	 * Load configuration from properties file,
	 * initialize keys and certificates,
	 * and ask the user for all missing passwords
	 */
	public static boolean init(String configFileName)
	{

		// Load Properties file
		FileInputStream in;
		Properties props = new Properties();
		try
		{
			in = new FileInputStream(configFileName);
			props.load(in);
			in.close();
		}
		catch (java.io.IOException e)
		{
			LogHolder.log(LogLevel.ALERT, LogType.PAY,
						  "Could not read config file " + configFileName);
			LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, e);
			return false; // Panic!
		}


		// parse Name (the name of the BI)
		m_Name = props.getProperty("name");

		// parse network configuration
		try
		{
			String japlisteners = props.getProperty("japlisteners");
			StringTokenizer st = new StringTokenizer(japlisteners, ",");
			while (st.hasMoreTokens())
			{
				String listener = st.nextToken();
				StringTokenizer st2 = new StringTokenizer(listener, ":");
				ListenerInterface l = new ListenerInterface(st2.nextToken(), Integer.parseInt(st2.nextToken()));
				ms_japListenerInterfaces.addElement(l);
			}

			String ailistener = props.getProperty("ailistener");
			st = new StringTokenizer(ailistener, ":");
			while (st.hasMoreTokens())
			{
				ms_aiListenerInterface = new ListenerInterface(st.nextToken(), Integer.parseInt(st.nextToken()));
			}

		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.ERR, LogType.PAY,
						  "ailistener and japlisteners in configfile '" +
						  configFileName +
						  "' must be specified and must be in this format: host:port(,host2:port2...). ailistener may only contain one interface"
				);
			LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, e);
			return false;
		}

		// parse Logger Configuration
		m_LogFileName = props.getProperty("logfilename");
		try
		{
			m_LogStderrThreshold = Integer.parseInt(props.getProperty(
				"logstderrthreshold"));
			m_LogFileThreshold = Integer.parseInt(props.getProperty(
				"logfilethreshold"));
		}
		catch (NumberFormatException e)
		{
			m_LogStderrThreshold = 1;
			m_LogFileThreshold = 2;
		}

		// parse database configuration
		m_dbHostname = props.getProperty("dbhost");
		m_dbPassword = props.getProperty("dbpassword");
		try
		{
			m_dbPort = Integer.parseInt(props.getProperty("dbport"));
		}
		catch (NumberFormatException e)
		{
			LogHolder.log(LogLevel.ERR, LogType.PAY,
						  "dbport in configfile '" + configFileName +
						  "' should be a NUMBER!");
			return false;
		}
		m_dbUsername = props.getProperty("dbusername");
		m_dbDatabaseName = props.getProperty("dbname");

		// If db password was not specified, ask the user
		if (m_dbPassword == null || m_dbPassword.equals(""))
		{
			System.out.println(
				"Please enter the password for connecting to the\n" +
				"PostgreSQL server at " + m_dbHostname + ":" + m_dbPort);
			System.out.print("Password: ");
			BufferedReader passwordReader = new BufferedReader(
				new InputStreamReader(System.in)
				);
			try
			{
				m_dbPassword = passwordReader.readLine();
			}
			catch (java.io.IOException e)
			{
				LogHolder.log(LogLevel.ERR, LogType.PAY,
							  "Error reading password from stdin.. strange!");
				LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, e);
			}
		}

		// initialize private signing key
		String password = props.getProperty("keyfilepassword");
		String keyFileName = props.getProperty("keyfile");

		// If the keyfile password was not specified, ask the user
		if (password == null || m_dbPassword.equals(""))
		{
			System.out.println("Please enter the password for decrypting the\n" +
							   "PKCS12 private key file " + keyFileName);
			System.out.print("Password: ");
			BufferedReader passwordReader = new BufferedReader(
				new InputStreamReader(System.in)
				);
			try
			{
				password = passwordReader.readLine();
			}
			catch (java.io.IOException e)
			{
				LogHolder.log(LogLevel.ERR, LogType.PAY,
							  "Error reading password from stdin.. strange!");
				LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, e);
			}
		}

		try
		{
			LogHolder.log(LogLevel.DEBUG, LogType.PAY,
						  "Trying to load PKCS12 file " + keyFileName +
						  " with password '" + password +
						  "'.");
			PKCS12 ownPkcs12 = PKCS12.getInstance(
				new FileInputStream(keyFileName),
				password.toCharArray()
				);
			ms_keyFile = keyFileName;
			ms_keyFilePassword = password;
			m_privateKey = ownPkcs12.getPrivateKey();
			/* get the public certificate */
			m_ownX509Certificate = JAPCertificate.getInstance(ownPkcs12.
				getX509Certificate());
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.ALERT, LogType.PAY,
						  "Error loading private key file " + keyFileName);
			LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, e);
			return false;
		}

		// parse ID (the unique BI-Id)
		//m_ID = props.getProperty("id");
		//changed to use the certificate's subjectkeyidentifier instead of an arbitrary String as id
		m_ID = new X509SubjectKeyIdentifier(m_ownX509Certificate.getPublicKey()).getValueWithoutColon();


		// parse infoservice configuration
		try
		{
			String islisteners = props.getProperty("infoservices");
			StringTokenizer st = new StringTokenizer(islisteners, ",");
			while (st.hasMoreTokens())
			{
				String listener = st.nextToken();
				StringTokenizer st2 = new StringTokenizer(listener, ":");
				ListenerInterface l = new ListenerInterface(st2.nextToken(), Integer.parseInt(st2.nextToken()),
					ListenerInterface.PROTOCOL_TYPE_HTTP);
				ms_isListenerInterfaces.addElement(l);
			}
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.ERR, LogType.PAY,
						  "infoservices in configfile '" +
						  configFileName +
						  "' must be specified and must be in this format: host:port(,host2:port2...)."
				);
			LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, e);
			return false;
		}

		//parse payment options
		//parse currencies
		int i = 1;
		while (true)
		{
			String currency = props.getProperty("currency" + i);
			if (currency == null)
			{
				break;
			}
			ms_paymentOptions.addCurrency(currency.toUpperCase());

			i++;
		}

		//parse accepted credit cards
		ms_paymentOptions.setAcceptedCreditCards(props.getProperty("acceptedcards", ""));

		//parse options
		i = 1;
		while (true)
		{
			String name = props.getProperty("option" + i + "name");
			if (name == null)
			{
				break;
			}
			String type = props.getProperty("option" + i + "type");
			if (type == null)
			{
				break;
			}
			String generic = props.getProperty("option" + i + "generic");
			if (generic == null)
			{
				generic = "true";
			}

			String japversion = props.getProperty("option" + i + "japversion");
			if (japversion == null)
			{
				japversion = "00.00.000";
			}

			XMLPaymentOption option = new XMLPaymentOption(name, type, Boolean.valueOf(generic).booleanValue(),
				japversion);
/*			String japversion = props.getProperty("option" + i + "japversion");
			if (japversion == null)
			{
				japversion = Util.VERSION_FORMAT;
			}

			XMLPaymentOption option = new XMLPaymentOption(name, type, Boolean.valueOf(generic).booleanValue(), japversion);
*/
			//Add headings
			String heading;
			String headingLang;
			int j = 1;
			while (true)
			{
				heading = props.getProperty("option" + i + "heading" + j);
				headingLang = props.getProperty("option" + i + "headinglang" + j);
				if (heading == null || headingLang == null)
				{
					break;
				}
				option.addHeading(heading, headingLang);
				j++;
			}

			//Add detailed infos
			String info;
			String infoLang;
			j = 1;
			while (true)
			{
				info = props.getProperty("option" + i + "detailedinfo" + j);
				infoLang = props.getProperty("option" + i + "detailedinfolang" + j);
				if (info == null || infoLang == null)
				{
					break;
				}
				option.addDetailedInfo(info, infoLang);
				j++;
			}

			//Add extra infos
			String extra;
			String extraLang;
			String extraType;
			j = 1;
			while (true)
			{
				extra = props.getProperty("option" + i + "extrainfo" + j);
				extraLang = props.getProperty("option" + i + "extrainfolang" + j);
				extraType = props.getProperty("option" + i + "extrainfotype" + j);
				if (extra == null || extraLang == null || extraType == null)
				{
					break;
				}
				option.addExtraInfo(extra, extraType, extraLang);
				j++;
			}

			//Add input fields
			String input;
			String inputLang;
			String inputRef;
			j = 1;
			while (true)
			{
				input = props.getProperty("option" + i + "input" + j);
				inputLang = props.getProperty("option" + i + "inputlang" + j);
				inputRef = props.getProperty("option" + i + "inputref" + j);
				if (input == null || inputLang == null || inputRef == null)
				{
					break;
				}
				option.addInputField(inputRef, input, inputLang);
				j++;
			}

			ms_paymentOptions.addOption(option);
			i++;
		}

		// Parse the CreditCardHelper classname
		ms_creditCardHelper = props.getProperty("creditcardhelper", "Dummy");

		//Parse the PayPal IPN port
		ms_payPalport = props.getProperty("paypalport", "9999");

		//Parse rate per MB
		ms_ratePerMB = Double.parseDouble(props.getProperty("ratepermb", "1.0"));

		//Parse external charge password
		ms_externalChargePassword = props.getProperty("chargepw", "");

		//Parse external charge port
		ms_externalChargePort = props.getProperty("chargeport", "9950");

		//Parse max connections
		ms_aiConnections = Integer.parseInt(props.getProperty("aiconnections", "5"));
		ms_japConnections = Integer.parseInt(props.getProperty("japconnections", "25"));

		return true;
	}
}