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
package jap;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.Locale;
import java.util.Observable;
import java.util.Observer;
import java.util.Vector;
import javax.xml.parsers.DocumentBuilderFactory;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import anon.ErrorCodes;
import anon.crypto.JAPCertificate;
import anon.crypto.SignatureVerifier;
import anon.crypto.XMLEncryption;
import anon.infoservice.Database;
import anon.infoservice.HTTPConnectionFactory;
import anon.infoservice.InfoServiceDBEntry;
import anon.infoservice.InfoServiceHolder;
import anon.infoservice.JAPVersionInfo;
import anon.infoservice.ListenerInterface;
import anon.infoservice.MixCascade;
import anon.infoservice.ProxyInterface;
import anon.pay.BI;
import anon.pay.PayAccountsFile;
import anon.util.IPasswordReader;
import anon.util.ResourceLoader;
import anon.util.XMLUtil;
import forward.server.ForwardServerManager;
import gui.JAPHtmlMultiLineLabel;
import jap.platform.AbstractOS;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import proxy.AnonProxy;
import proxy.DirectProxy;
import proxy.ProxyListener;
import update.JAPUpdateWizard;

/* This is the Model of All. It's a Singelton!*/
public final class JAPController implements ProxyListener, Observer
{
	/**
	 * Stores all MixCascades we know (information comes from infoservice or was entered by a user).
	 * This list may or may not include the current active MixCascade.
	 */
	private Vector m_vectorMixCascadeDatabase = null;

	/**
	 * Stores the active MixCascade.
	 */
	private MixCascade m_currentMixCascade = null;

	private ServerSocket m_socketHTTPListener = null; // listener object for HTTP

	private DirectProxy m_proxyDirect = null; // service object for direct access (bypass anon service)
	private AnonProxy m_proxyAnon = null; // service object for anon access

	private boolean isRunningHTTPListener = false; // true if a HTTP listener is running

	//private boolean  canStartService             = false; // indicates if anon service can be started
	private boolean m_bAlreadyCheckedForNewVersion = false; // indicates if check for new version has already been done
	private boolean mbActCntMessageNotRemind = false; // indicates if Warning message in setAnonMode has been deactivated for the session
	private boolean mbActCntMessageNeverRemind = false; // indicates if Warning message in setAnonMode has been deactivated forever
	private boolean mbDoNotAbuseReminder = false; // indicates if new warning message in setAnonMode (containing Do no abuse) has been shown
	private boolean mbGoodByMessageNeverRemind = false; // indicates if Warning message before exit has been deactivated forever
	private boolean m_bForwarderNotExplain = false; //indicates if the warning message about forwarding should be shown

	private boolean m_bPaymentFirstTime = false; // indicates if encryption dialog should be showed before saving payment configuration data

	/** @todo check is it ok to have the password in memory while Jap is running? if not, user must enter it everytime */
	private String m_strPayAccountsPassword = null; // password for encrypting the payment data

	public String status1 = " ";
	public String status2 = " ";

	private int m_nrOfBytesWWW = 0;
	private int m_nrOfBytesOther = 0;

	private static AbstractJAPMainView m_View = null;
	private static JAPController m_Controller = null;
	private static JAPModel m_Model = null;
	private static JAPFeedback feedback = null;
	private Locale m_Locale = null;
	private Vector observerVector = null;
	private IPasswordReader m_passwordReader;

	private static Font m_fontControls;
	/** Holds the MsgID of the status message after the forwaring server was started.*/
	private int m_iStatusPanelMsgIdForwarderServerStatus;

	private static Object oAnonSyncObject = new Object(); //for synchronisation of setMode(true/false)
	private static Object oAnonSetThreadIDSyncObject = new Object(); //for synchronisation of setMode(true/false)
	private static int ms_AnonModeAsyncLastStarted = -1;
	private static int ms_AnonModeAsyncLastFinished = -1;

	private JAPHelpContext m_helpContext;

	private JAPController()
	{
		m_Model = JAPModel.getInstance();
		// Create observer object
		observerVector = new Vector();

		/* set a default mixcascade */
		try
		{
			m_currentMixCascade = new MixCascade(JAPConstants.defaultAnonName,
												 JAPConstants.defaultAnonID,
												 JAPConstants.defaultAnonHost,
												 JAPConstants.defaultAnonPortNumber);
			m_currentMixCascade.setIsUserDefined(false);
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.EMERG, LogType.NET, "JAPController: Constructor - default mix cascade: " + e.getMessage());
		}
		/* set a default infoservice */
		try
		{
			InfoServiceDBEntry defaultInfoService = new InfoServiceDBEntry(JAPConstants.
				defaultInfoServiceName,
				new ListenerInterface(JAPConstants.defaultInfoServiceHostName,
									  JAPConstants.defaultInfoServicePortNumber).toVector(), true);
			InfoServiceHolder.getInstance().setPreferedInfoService(defaultInfoService);
		}
		catch (Exception e)
		{
			e.printStackTrace();
			LogHolder.log(LogLevel.EMERG, LogType.NET, "JAPController: Constructor - default info service: " + e.getMessage());
		}
		/* set some default values for infoservice communication */
		setInfoServiceDisabled(JAPConstants.DEFAULT_INFOSERVICE_DISABLED);
		InfoServiceHolder.getInstance().setChangeInfoServices(JAPConstants.DEFAULT_INFOSERVICE_CHANGES);

		/* load the default certificates */
		JAPCertificate defaultRootCert = JAPCertificate.getInstance(ResourceLoader.loadResource(JAPConstants.
			CERTSPATH + JAPConstants.TRUSTEDROOTCERT));
		if (defaultRootCert != null)
		{
			SignatureVerifier.getInstance().getVerificationCertificateStore().
				addCertificateWithoutVerification(defaultRootCert, JAPCertificate.CERTIFICATE_TYPE_ROOT, true);
		}
		else
		{
			LogHolder.log(LogLevel.ERR, LogType.MISC,
						  "JAPController: Constructor: Error loading default root certificate.");
		}
		JAPCertificate updateMessagesCert = JAPCertificate.getInstance(ResourceLoader.loadResource(
			JAPConstants.CERTSPATH + JAPConstants.CERT_JAPINFOSERVICEMESSAGES));
		if (updateMessagesCert != null)
		{
			SignatureVerifier.getInstance().getVerificationCertificateStore().
				addCertificateWithoutVerification(updateMessagesCert, JAPCertificate.CERTIFICATE_TYPE_UPDATE, true);
		}
		else
		{
			LogHolder.log(LogLevel.ERR, LogType.MISC,
						  "JAPController: Constructor: Error loading default update messages certificate.");
		}

		HTTPConnectionFactory.getInstance().setTimeout(JAPConstants.DEFAULT_INFOSERVICE_TIMEOUT);

		m_vectorMixCascadeDatabase = new Vector();
		m_proxyDirect = null;
		m_proxyAnon = null;
		//m_proxySocks = null;
		m_Locale = Locale.getDefault();

		m_passwordReader = new JAPFirewallPasswdDlg();

		/* we want to observe some objects */
		JAPModel.getInstance().getRoutingSettings().addObserver(this);
		JAPModel.getInstance().getRoutingSettings().getServerStatisticsListener().addObserver(this);
		JAPModel.getInstance().getRoutingSettings().getRegistrationStatusObserver().addObserver(this);
		m_iStatusPanelMsgIdForwarderServerStatus = -1;

		// Global help context object
		m_helpContext = new JAPHelpContext();
	}

	/** Creates the Controller - as Singleton.
	 *  @return The one and only JAPController
	 */
	public static JAPController getInstance()
	{
		if (m_Controller == null)
		{
			m_Controller = new JAPController();
		}
		return m_Controller;
	}

	/**
	 * Returns the password reader.
	 * @return the password reader
	 */
	public IPasswordReader getPasswordReader()
	{
		return m_passwordReader;
	}

	//---------------------------------------------------------------------
	public void initialRun()
	{
		LogHolder.log(LogLevel.INFO, LogType.MISC, "JAPModel:initial run of JAP...");
		// start http listener object
		/* if (JAPModel.isTorEnabled())
		 {
		   startSOCKSListener();
		 }*/
		if (!startHTTPListener())
		{ // start was not sucessful
			Object[] args =
				{
				new Integer(JAPModel.getHttpListenerPortNumber())};
			String msg = MessageFormat.format(JAPMessages.getString("errorListenerPort"), args);
			// output error message
			JOptionPane.showMessageDialog(getView(),
										  msg,
										  JAPMessages.getString("errorListenerPortTitle"),
										  JOptionPane.ERROR_MESSAGE);
			LogHolder.log(LogLevel.EMERG, LogType.NET, "Cannot start listener!");
			m_Controller.status1 = JAPMessages.getString("statusCannotStartListener");
			getView().disableSetAnonMode();
			notifyJAPObservers();
		}
		else
		{ // listender has started correctly
			m_Controller.status1 = JAPMessages.getString("statusRunning");
			// initial setting of anonMode
			setAnonMode(JAPModel.getAutoConnect());
		}
	}

	//---------------------------------------------------------------------
	/** Loads the Configuration.
	 * First tries to read a config file provided on the command line.
	 * If none is provided, it will look in the operating system specific locations
	 * for configuration files (e.g. Library/Preferences on Mac OS X or hidden
	 * in the user's home on Linux).
	 * If there are no config files in these locations, the method will look
	 * in the user's home directory and in the installation path of JAP
	 * (the last two locations are checked for compatibility reasons and are deprecated).
	 *
	 * The configuration is a XML-File with the following structure:
	 *  <JAP
	 *    version="0.18"                     // version of the xml struct (DTD) used for saving the configuration
	 *    portNumber=""                     // Listener-Portnumber
	 *    portNumberSocks=""                // Listener-Portnumber for SOCKS
	 *    supportSocks=""                   // Will we support SOCKS ?
	 *    listenerIsLocal="true"/"false"    // Listener lauscht nur an localhost ?
	 *    proxyMode="true"/"false"          // Using a HTTP-Proxy??
	 *    proxyType="SOCKS"/"HTTP"          // which kind of proxy
	 *    proxyHostName="..."               // the Hostname of the Proxy
	 *    proxyPortNumber="..."             // port number of the Proxy
	 *    proxyAuthorization="true"/"false" // Need authorization to acces the proxy ?
	 *    porxyAuthUserID="..."             // UserId for the Proxy if Auth is neccessary
	 *    infoServiceHostName="..."         // hostname of the infoservice (only config version < 0.3)
	 *    infoServicePortnumber=".."        // the portnumber of the info service (only config version < 0.3)
	 *    infoServiceDisabled="true/false"  // disable use of InfoService
	 *    infoServiceChange="true/false"    // automatic change of infoservice after failure (since config version 0.5)
	 *    infoServiceTimeout="..."          // timeout (sec) for infoservice and update communication (since config version 0.5)
	 *    preCreateAnonRoutes="true/false"  // Should we setup Anon Routes in the "Background" ? (sinc 0.13)
	 *    autoConnect="true"/"false"    // should we start the anon service immedialy after programm launch ?
	 *    autoReConnect="true"/"false"    // should we automatically reconnect to mix if connection was lost ?
	 *    DummyTrafficIntervall=".."    //Time of inactivity in milli seconds after which a dummy is send
	 *    minimizedStartup="true"/"false" // should we start minimized ???
	 *    neverRemindActiveContent="true"/"false" // should we remind the user about active content ?
	 *    Locale="LOCALE_IDENTIFIER" (two letter iso 639 code) //the Language for the UI to use
	 *    LookAndFeel="..."             //the LookAndFeel class name
	 *  >
	 * <MixCascades>								//info about known MixCascades (since version 0.16)
	 *	<MixCascade>...</MixCascade>
	 * </MixCascades>							   //at the moment only user defined cascades are stored
	 * <MixCascade id=.." userDefined="true/false">  //info about the used AnonServer (since version 0.1) [equal to the general MixCascade struct]
	 *												//Attr "userDefined" since Version 0.12
	 * 												//if true this cascade information was handcrafted by the user
	 * 												//otherwise it comes from the InfoService
	 *   <Name>..</Name>
	 *   <Network>
	 *     <ListenerInterfaces>
	 *       <ListenerInterface> ... </ListenerInterface>
	 *     </ListenerInterfaces>
	 *   </Network>
	 * </MixCascade>
	 * <GUI> //since version 0.2 --> store the position and size of JAP on the Desktop
	 *    <MainWindow> //for the Main Window
	 *       <SetOnStartup>"true/false"</SetOnStartup> //remember Position ?
	 *       <Location x=".." y=".."> //Location of the upper left corner
	 *       <Size dx=".." dy=.."> //Size of the Main window
	 *       <DefaultView>Normal|Simplified</DefaultView> //Which view of JAP to show? (since version 0.11); default: Normal
	 * 		 <MoveToSystray>"true"/"false"</MoveToSystray> //After start move JAP into the systray? (since version 0.11); default: false
	 *     </MainWindow>
	 * </GUI>
	 * <Debug>                          //info about debug output
	 *    <Level>..</Level>              // the amount of output (0 means less.. 7 means max)
	 *    <Type                          // which type of messages should be logged
	 *      GUI="true"/"false"          // messages related to the user interface
	 *      NET="true"/"false"          // messages related to the network
	 *      THREAD="true"/"false"        // messages related to threads
	 *      MISC="true"/"false"          // all the others
	 *    >
	 *    </Type>
	 *    <Output>...                      //the kind of Output, at the moment only: if NodeValue==Console --> Console
	 *       <File>...                    //if given, log to the given File (since version 0.14)
	 *       </File>
	 *    </Output>
	 *
	 * </Debug>
	 * <SignatureVerification>                                   // since version 0.18
	 *   <CheckSignatures>true</CheckSignatures>                 // whether signature verification of received XML data is enabled or disabled
	 *   <TrustedCertificates>                                   // list of all certificates to uses for signature verification
	 *     <CertificateContainer>
	 *       <CertificateType>1</CertificateType>                              // the type of the stored certificate (until it's stored within the certificate itself), see JAPCertificate.java
	 *       <CertificateNeedsVerification>true<CertificateNeedsVerification>  // whether the certificate has to be verified against an active root certificate from the certificat store in order to get activated itself
	 *       <CertificateEnabled>true<CertificateEnabled>                      // whether the certificate is enabled (available for signature verification) or not
	 *       <CertificateData>
	 *         <X509Certificate>...</X509Certificate>                          // the certificate data, see JAPCertificate.java
	 *       </CertificateData>
	 *     </CertificateContainer>
	 *     ...
	 *   </TrustedCertificates>
	 * </SignatureVerification>
	 * <InfoServices>                                           // info about all known infoservices (since config version 0.3)
	 *   <InfoService id="...">...</InfoService>                // the same format as from infoservice, without signature, if expired, it is removed from infoservice list
	 *   <InfoService id="...">...</InfoService>
	 * </InfoServices>
	 * <PreferedInfoService>                                    // info about the prefered infoservice, only one infoservice is supported here (since config version 0.3)
	 *   <InfoService id="...">...</InfoService>                // the same format as from infoservice, without signature, expire time does not matter
	 * </PreferedInfoService>
	 * <Tor>    //  Tor related seetings (since Version 0.6)
	 * 	 <MaxConnectionsPerRoute>...</MaxConnectionsPerRoute>(since Vresion 0.8) //How many connections are allowed before a new circuit is created
	 * 	 <RouteLen min=" " max=" "/>(since Vresion 0.9) //How long should a route be
	 * </Tor>
	 * <Payment //Since version 0.7
	 *    biHost="..."                      // BI's Hostname
	 *    biPort="..."                      // BI's portnumber
	 * >
	 *   <EncryptedData>  // Account data encrypted with password
	 *      <Accounts>
	 *        <Account>.....</Account>
	 *        <Account>.....</Account>
	 *      </Accounts>
	 *   </EncryptedData>
	 * </Payment>
	 * <JapForwardingSettings>                                   // since version 0.10, if WITH_BLOCKINGRESISTANCE is enabled
	 *   <ForwardingServer>
	 *     <ServerPort>12345</ServerPort>                        // the port number, where the forwarding server is listening
	 *     <ServerRunning>false</ServerRunning>                  // whether the forwarding server shall be started, when JAP is starting
	 *     <ConnectionClassSettings>
	 *       <ConnectionClasses>                                 // list of all connection classes including settings
	 *         <ConnectionClass>                                 // a single connection class entry
	 *           <ClassIdentifier>0</ClassIdentifier>            // the identifier of the connection class
	 *           <MaximumBandwidth>10000</MaximumBandwidth>      // the maximum bandwidth (bytes/sec) the class provides
	 *           <RelativeBandwidth>50</RelativeBandwidth>       // since version 0.17, the percentage of the bandwidth useable for forwarding
	 *         </ConnectionClass>
	 *         ...
	 *       </ConnectionClasses>
	 *       <CurrentConnectionClass>0</CurrentConnectionClass>  // the currently selected connection class (identifier)
	 *     </ConnectionClassSettings>
	 *     <InfoServiceRegistrationSettings>
	 *       <UseAllPrimaryInfoServices>false</UseAllPrimaryInfoServices>  // whether to registrate the local forwarding server at all infoservices with a forwarder list
	 *       <RegistrationInfoServices>                                    // a list of InfoServices, where the local forwarding server shall be registrated on startup
	 *         <InfoService>...</InfoService>
	 *         ...
	 *       </RegistrationInfoServices>
	 *     </InfoServiceRegistrationSettings>
	 *     <AllowedMixCascadesSettings>
	 *       <AllowAllAvailableMixCascades>true</AllowAllAvailableMixCascades>  // whether the clients of the local forwarding server are allowed to use all running mixcascades
	 *       <AllowedMixCascades>                                               // a list of MixCascades, where the the clients are allowed to connect to
	 *         <MixCascade>...<MixCascade>
	 *         ...
	 *       </AllowedMixCascades>
	 *     </AllowedMixCascadesSettings>
	 *   </ForwardingServer>
	 *   <ForwardingClient>                                      // since version 0.15
	 *     <ConnectViaForwarder>false</ConnectViaForwarder>      // whether a forwarder is needed to contact the mixcascades when enabling the anonymous mode
	 *     <ForwardInfoService>false</ForwardInfoService>        // whether an InfoService can be reached or also the InfoService needs forwarding
	 *   </ForwardingClient>
	 * </JapForwardingSettings>
	 *  </JAP>
	 *  @param a_strJapConfFile - file containing the Configuration. If null $(user.home)/jap.conf or ./jap.conf is used.
	 *  @param loadPay does this JAP support Payment ?
	 */
	public synchronized void loadConfigFile(String a_strJapConfFile, boolean loadPay)
	{
		String japConfFile = a_strJapConfFile;
		boolean success = false;
		if (japConfFile != null)
		{
			/* try the config file from the command line */
			success = this.loadConfigFileCommandLine(japConfFile);
		}
		if (!success)
		{
			/* no config file found -> try to use the config file in the OS-specific location */
			success = this.loadConfigFileOSdependent();
		}
		if (!success)
		{
			/* no config file found -> try to use the config file in the home directory of the user */
			success = this.loadConfigFileHome();
		}
		if (!success)
		{
			/* no config file found -> try to use the config file in the current directory */
			success = this.loadConfigFileCurrentDir();
		}
		if (!success)
		{
			/* no config file at any position->use OS-specific path for storing a new one*/
			JAPModel.getInstance().setConfigFile(AbstractOS.getInstance().getConfigPath());
		}
		if (a_strJapConfFile != null)
		{
			/* always try to use the config file specified on the command-line for storing the
			 * configuration
			 */
			JAPModel.getInstance().setConfigFile(a_strJapConfFile);
		}
		else
		{
			if (!success)
			{
				/* no config file was specified on the command line and the default config files don't
				 * exist -> store the configuration in the OS-specific directory
				 */
				JAPModel.getInstance().setConfigFile(AbstractOS.getInstance().getConfigPath());
			}
		}
		if (success)
		{
			try
			{
				FileInputStream f = new FileInputStream(JAPModel.getInstance().getConfigFile());
				Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(f);
				Element root = doc.getDocumentElement();
				NamedNodeMap n = root.getAttributes();

				//
				setDefaultView(JAPConstants.VIEW_NORMAL);

				String strVersion = XMLUtil.parseValue(n.getNamedItem(JAPConstants.CONFIG_VERSION), null);
				int port = XMLUtil.parseAttribute(root, JAPConstants.CONFIG_PORT_NUMBER,
												  JAPModel.getHttpListenerPortNumber());
				boolean bListenerIsLocal = XMLUtil.parseValue(n.getNamedItem(JAPConstants.
					CONFIG_LISTENER_IS_LOCAL), true);
				setHTTPListener(port, bListenerIsLocal, false);
				//port = XMLUtil.parseAttribute(root, "portNumberSocks",
				//  JAPModel.getSocksListenerPortNumber());
				//setSocksPortNumber(port);
				//setUseSocksPort(JAPUtil.parseNodeBoolean(n.getNamedItem("supportSocks"),false));
				//setUseProxy(JAPUtil.parseNodeBoolean(n.getNamedItem("proxyMode"),false));
				// load settings for the reminder message in setAnonMode
				try
				{
					mbActCntMessageNeverRemind = XMLUtil.parseValue(n.getNamedItem(
						JAPConstants.CONFIG_NEVER_REMIND_ACTIVE_CONTENT), false);
					mbDoNotAbuseReminder = XMLUtil.parseValue(n.getNamedItem(JAPConstants.
						CONFIG_DO_NOT_ABUSE_REMINDER), false);
					if (mbActCntMessageNeverRemind && mbDoNotAbuseReminder)
					{
						mbActCntMessageNotRemind = true;
						// load settings for the reminder message before goodBye
					}
					mbGoodByMessageNeverRemind = XMLUtil.parseValue(n.getNamedItem(JAPConstants.
						CONFIG_NEVER_REMIND_GOODBYE), false);
					m_bForwarderNotExplain = XMLUtil.parseValue(n.getNamedItem(JAPConstants.
						CONFIG_NEVER_EXPLAIN_FORWARD), false);

				}
				catch (Exception ex)
				{
					LogHolder.log(LogLevel.INFO, LogType.MISC,
								  "JAPController: loadConfigFile: Error loading reminder message ins setAnonMode.");
				}
				/* infoservice configuration options */
				boolean b = XMLUtil.parseValue(n.getNamedItem(JAPConstants.CONFIG_INFOSERVICE_DISABLED),
											   JAPModel.isInfoServiceDisabled());
				setInfoServiceDisabled(b);
				b = XMLUtil.parseValue(n.getNamedItem(JAPConstants.CONFIG_INFOSERVICE_CHANGE),
									   InfoServiceHolder.getInstance().isChangeInfoServices());
				InfoServiceHolder.getInstance().setChangeInfoServices(b);
				int i = XMLUtil.parseValue(n.getNamedItem(JAPConstants.CONFIG_INFOSERVICE_TIMEOUT), -1);
				try
				{
					if ( (i >= 1) && (i <= 60))
					{
						HTTPConnectionFactory.getInstance().setTimeout(i);
					}
				}
				catch (Exception e)
				{
					LogHolder.log(LogLevel.INFO, LogType.MISC,
								  "JAPController: loadConfigFile: Error loading InfoService timeout.");
				}

				// load settings for proxy
				ProxyInterface proxyInterface = null;

				try
				{
					proxyInterface = new ProxyInterface(
						XMLUtil.parseValue(n.getNamedItem(JAPConstants.CONFIG_PROXY_HOST_NAME), null),
						XMLUtil.parseAttribute(root, JAPConstants.CONFIG_PROXY_PORT_NUMBER, -1),
						XMLUtil.parseValue(n.getNamedItem(JAPConstants.CONFIG_PROXY_TYPE),
										   ProxyInterface.PROTOCOL_STR_TYPE_HTTP),
						XMLUtil.parseValue(n.getNamedItem(JAPConstants.CONFIG_PROXY_AUTH_USER_ID), null),
						getPasswordReader(),
						XMLUtil.parseValue(n.getNamedItem(JAPConstants.CONFIG_PROXY_AUTHORIZATION), false),
						XMLUtil.parseValue(n.getNamedItem(JAPConstants.CONFIG_PROXY_MODE), false));
				}
				catch (Exception a_e)
				{
					a_e.printStackTrace();
					LogHolder.log(LogLevel.NOTICE, LogType.NET,
								  "JAPController: could not load proxy settings: " + a_e);
				}

				// check if something has changed
				changeProxyInterface(proxyInterface);

				/* try to get the info from the MixCascade node */
				MixCascade defaultMixCascade = null;
				Element mixCascadeNode = (Element) XMLUtil.getFirstChildByName(root,
					JAPConstants.CONFIG_MIX_CASCADE);
				try
				{
					defaultMixCascade = new MixCascade( (Element) mixCascadeNode);
					defaultMixCascade.setIsUserDefined(
						XMLUtil.parseAttribute(mixCascadeNode, JAPConstants.CONFIG_USER_DEFINED, false));
				}
				catch (Exception e)
				{
					/* take the current mixcascade as the default */
					defaultMixCascade = getCurrentMixCascade();
				}
				setCurrentMixCascade(defaultMixCascade);

				/* try to load information about user defined cascades */
				Node nodeCascades = XMLUtil.getFirstChildByName(root, JAPConstants.CONFIG_MIX_CASCADES);
				if (nodeCascades != null)
				{
					Node nodeCascade = nodeCascades.getFirstChild();
					while (nodeCascade != null)
					{
						if (nodeCascade.getNodeName().equals(JAPConstants.CONFIG_MIX_CASCADE))
						{
							try
							{
								MixCascade cascade = new MixCascade( (Element) nodeCascade);
								cascade.setIsUserDefined(
									XMLUtil.parseAttribute(nodeCascade, JAPConstants.CONFIG_USER_DEFINED, false));
								m_vectorMixCascadeDatabase.addElement(cascade);
							}
							catch (Exception e)
							{}
						}
						nodeCascade = nodeCascade.getNextSibling();
					}
				}

				setDummyTraffic(XMLUtil.parseAttribute(root, JAPConstants.CONFIG_DUMMY_TRAFFIC_INTERVALL,
					-1));
				setAutoConnect(XMLUtil.parseValue(n.getNamedItem(JAPConstants.CONFIG_AUTO_CONNECT), false));
				setAutoReConnect(XMLUtil.parseValue(n.getNamedItem(JAPConstants.CONFIG_AUTO_RECONNECT), false));
				setPreCreateAnonRoutes(XMLUtil.parseValue(n.getNamedItem(JAPConstants.
					CONFIG_PRECREATE_ANON_ROUTES), true));
				m_Model.setMinimizeOnStartup(XMLUtil.parseValue(n.getNamedItem(JAPConstants.
					CONFIG_MINIMIZED_STARTUP), false));
				//Load Locale-Settings
				String strLocale = XMLUtil.parseValue(n.getNamedItem(JAPConstants.CONFIG_LOCALE),
					m_Locale.getLanguage());
				Locale locale = new Locale(strLocale, "");
				setLocale(locale);
				//Load look-and-feel settings (not changed if SmmallDisplay!
				if (!JAPModel.isSmallDisplay())
				{
					String lf = XMLUtil.parseValue(n.getNamedItem(JAPConstants.CONFIG_LOOK_AND_FEEL),
						JAPConstants.CONFIG_UNKNOWN);
					LookAndFeelInfo[] lfi = UIManager.getInstalledLookAndFeels();
					for (i = 0; i < lfi.length; i++)
					{
						if (lfi[i].getName().equals(lf) || lfi[i].getClassName().equals(lf))
						{
							try
							{
								UIManager.setLookAndFeel(lfi[i].getClassName());
								//        SwingUtilities.updateComponentTreeUI(m_frmParent);
								//        SwingUtilities.updateComponentTreeUI(SwingUtilities.getRoot(((JComboBox)e.getItemSelectable())));
							}
							catch (Exception lfe)
							{
								LogHolder.log(LogLevel.WARNING, LogType.GUI,
											  "JAPModel:Exception while setting look-and-feel");
							}
							break ;
						}
					}
				}
				//Loading GUI Setting
				Element elemGUI = (Element) XMLUtil.getFirstChildByName(root, JAPConstants.CONFIG_GUI);
				if (elemGUI != null)
				{
					Element elemMainWindow = (Element) XMLUtil.getFirstChildByName(elemGUI,
						JAPConstants.CONFIG_MAIN_WINDOW);
					if (elemMainWindow != null)
					{
						try
						{
							Element tmp = (Element) XMLUtil.getFirstChildByName(elemMainWindow,
								JAPConstants.CONFIG_SET_ON_STARTUP);
							b = XMLUtil.parseValue(tmp, false);
							JAPController.setSaveMainWindowPosition(b);
							if (b)
							{
								tmp = (Element) XMLUtil.getFirstChildByName(elemMainWindow,
									JAPConstants.CONFIG_LOCATION);
								Point p = new Point();
								p.x = XMLUtil.parseAttribute(tmp, JAPConstants.CONFIG_X, -1);
								p.y = XMLUtil.parseAttribute(tmp, JAPConstants.CONFIG_Y, -1);
								Dimension d = new Dimension();
								tmp = (Element) XMLUtil.getFirstChildByName(elemMainWindow,
									JAPConstants.CONFIG_SIZE);
								d.width = XMLUtil.parseAttribute(tmp, JAPConstants.CONFIG_DX, -1);
								d.height = XMLUtil.parseAttribute(tmp, JAPConstants.CONFIG_DY, -1);
								m_Model.m_OldMainWindowLocation = p;
								m_Model.m_OldMainWindowSize = d;
							}
							tmp = (Element) XMLUtil.getFirstChildByName(elemMainWindow,
								JAPConstants.CONFIG_MOVE_TO_SYSTRAY);
							b = XMLUtil.parseValue(tmp, false);
							setMoveToSystrayOnStartup(b);
							if (b)
							{ ///todo: move to systray
								if (m_View != null)
								{
									m_View.hideWindowInTaskbar();
								}
							}
							tmp = (Element) XMLUtil.getFirstChildByName(elemMainWindow,
								JAPConstants.CONFIG_DEFAULT_VIEW);
							String strDefaultView = XMLUtil.parseValue(tmp, JAPConstants.CONFIG_NORMAL);
							if (strDefaultView.equals(JAPConstants.CONFIG_SIMPLIFIED))
							{
								setDefaultView(JAPConstants.VIEW_SIMPLIFIED);
							}
						}
						catch (Exception ex)
						{
							LogHolder.log(LogLevel.INFO, LogType.MISC,
										  "JAPController: loadConfigFile: Error loading GUI configuration.");
						}
					}
				}
				//Loading debug settings
				Element elemDebug = (Element) XMLUtil.getFirstChildByName(root, JAPConstants.CONFIG_DEBUG);
				if (elemDebug != null)
				{
					try
					{
						Element elemLevel = (Element) XMLUtil.getFirstChildByName(elemDebug,
							JAPConstants.CONFIG_LEVEL);
						if (elemLevel != null)
						{
							int l = XMLUtil.parseValue(elemLevel, JAPDebug.getInstance().getLogLevel());
							JAPDebug.getInstance().setLogLevel(l);
						}
						Element elemType = (Element) XMLUtil.getFirstChildByName(elemDebug,
							JAPConstants.CONFIG_TYPE);
						if (elemType != null)
						{
							int debugtype = LogType.NUL;
							if (XMLUtil.parseAttribute(elemType, JAPConstants.CONFIG_GUI, false))
							{
								debugtype += LogType.GUI;
							}
							if (XMLUtil.parseAttribute(elemType, JAPConstants.CONFIG_NET, false))
							{
								debugtype += LogType.NET;
							}
							if (XMLUtil.parseAttribute(elemType, JAPConstants.CONFIG_THREAD, false))
							{
								debugtype += LogType.THREAD;
							}
							if (XMLUtil.parseAttribute(elemType, JAPConstants.CONFIG_MISC, false))
							{
								debugtype += LogType.MISC;
								debugtype += LogType.PAY;
								debugtype += LogType.TOR;
							}
							JAPDebug.getInstance().setLogType(debugtype);
						}
						Node elemOutput = XMLUtil.getFirstChildByName(elemDebug, JAPConstants.CONFIG_OUTPUT);
						if (elemOutput != null)
						{
							String strConsole = XMLUtil.parseValue(elemOutput, "");
							if (strConsole != null)
							{
								strConsole.trim();
								JAPDebug.showConsole(strConsole.equalsIgnoreCase(JAPConstants.CONFIG_CONSOLE),
									m_View);
							}
							Node elemFile = XMLUtil.getLastChildByName(elemOutput, JAPConstants.CONFIG_FILE);
							JAPDebug.setLogToFile(XMLUtil.parseValue(elemFile, null));
						}
					}
					catch (Exception ex)
					{
						LogHolder.log(LogLevel.INFO, LogType.MISC,
									  "JAPController: loadConfigFile: Error loading Debug Settings.");
					}
				}

				/* load the signature verification settings */
				try
				{
					Element signatureVerificationNode = (Element) (XMLUtil.getFirstChildByName(root,
						SignatureVerifier.getXmlSettingsRootNodeName()));
					if (signatureVerificationNode != null)
					{
						SignatureVerifier.getInstance().loadSettingsFromXml(signatureVerificationNode);
					}
					else
					{
						throw (new Exception("JAPController: loadConfigFile: No SignatureVerification node found. Using default settings for signature verification."));
					}
				}
				catch (Exception e)
				{
					LogHolder.log(LogLevel.ERR, LogType.MISC, e);
				}

				/* loading infoservice settings */
				/* infoservice list */
				NodeList infoServicesNodes = root.getElementsByTagName(JAPConstants.CONFIG_INFOSERVICES);
				if (infoServicesNodes.getLength() > 0)
				{
					Element infoServicesNode = (Element) (infoServicesNodes.item(0));
					InfoServiceDBEntry.loadFromXml(
						infoServicesNode, Database.getInstance(InfoServiceDBEntry.class));
				}
				/* prefered infoservice */
				NodeList preferedInfoServiceNodes = root.getElementsByTagName(JAPConstants.
					CONFIG_PREFERED_INFOSERVICE);
				if (preferedInfoServiceNodes.getLength() > 0)
				{
					Element preferedInfoServiceNode = (Element) (preferedInfoServiceNodes.item(0));
					NodeList infoServiceNodes = preferedInfoServiceNode.getElementsByTagName(JAPConstants.
						CONFIG_INFOSERVICE);
					if (infoServiceNodes.getLength() > 0)
					{
						Element infoServiceNode = (Element) (infoServiceNodes.item(0));
						try
						{
							InfoServiceDBEntry preferedInfoService = new InfoServiceDBEntry(infoServiceNode);
							InfoServiceHolder.getInstance().setPreferedInfoService(preferedInfoService);
						}
						catch (Exception e)
						{
							LogHolder.log(LogLevel.INFO, LogType.MISC,
										  "JAPController: loadConfigFile: Error loading prefered InfoService.");
						}
					}
				}

				/*loading Tor settings*/
				try
				{
					Element elemTor = (Element) XMLUtil.getFirstChildByName(root, JAPConstants.CONFIG_TOR);
					Element elem = (Element) XMLUtil.getFirstChildByName(elemTor,
						JAPConstants.CONFIG_MAX_CONNECTIONS_PER_ROUTE);
					setTorMaxConnectionsPerRoute(XMLUtil.parseValue(elem,
						JAPModel.getTorMaxConnectionsPerRoute()));
					elem = (Element) XMLUtil.getFirstChildByName(elemTor, JAPConstants.CONFIG_ROUTE_LEN);
					int min, max;
					min = XMLUtil.parseAttribute(elem, JAPConstants.CONFIG_MIN,
												 JAPModel.getTorMinRouteLen());
					max = XMLUtil.parseAttribute(elem, JAPConstants.CONFIG_MAX,
												 JAPModel.getTorMaxRouteLen());
					setTorRouteLen(min, max);
				}
				catch (Exception ex)
				{
					LogHolder.log(LogLevel.INFO, LogType.MISC,
								  "JAPController: loadConfigFile: Error loading Tor configuration.");
				}

				/* load Payment settings */
				if (loadPay)
				{
					Element elemPay = (Element) XMLUtil.getFirstChildByName(root, JAPConstants.CONFIG_PAYMENT);
					/*					String biName = elemPay.getAttribute("biName");
					   if (biName == null || biName.equals(""))
					   {
					 biName = JAPConstants.PIHOST;
					   }
					   String biHost = elemPay.getAttribute("biHost");
					   if (biHost == null || biHost.equals(""))
					   {
					 biHost = JAPConstants.PIHOST;
					   }
					   int biPort = Integer.parseInt(elemPay.getAttribute("biPort"));
					   if (biPort == 0)
					   {
					 biPort = JAPConstants.PIPORT;
					   }
					   BI theBI = new BI(
					 m_Model.getResourceLoader().loadResource(JAPConstants.CERTSPATH +
					 JAPConstants.CERT_BI),
					   biName, biHost, biPort);*/
					Element elemBI = (Element) XMLUtil.getFirstChildByName(elemPay, BI.XML_ELEMENT_NAME);
					BI theBI = null;
					if (elemBI == null)
					{
						theBI = new BI(
							ResourceLoader.loadResource(JAPConstants.CERTSPATH +
							JAPConstants.CERT_BI),
							JAPConstants.PIHOST, JAPConstants.PIHOST, JAPConstants.PIPORT);
					}
					else
					{
						theBI = new BI(elemBI);
					}
					Element elemAccounts = (Element) XMLUtil.getFirstChildByName(elemPay,
						JAPConstants.CONFIG_ENCRYPTED_DATA);

					// test: is account data encrypted?
					if (elemAccounts != null)
					{
						// it is encrypted -> ask user for password
						Element elemPlainTxt;
						String strMessage =
							"<html>Please enter password for decrypting your accounts data</html>";

						while (true)
						{
							/** @todo handle cancel button properly */
							/** @todo internationalize msgs */
							/** @todo use an IPasswordReader dialog here */
							m_strPayAccountsPassword = JOptionPane.showInputDialog(
								m_View, strMessage,
								"Jap Account Data",
								JOptionPane.QUESTION_MESSAGE | JOptionPane.OK_CANCEL_OPTION
								);

							try
							{
								elemPlainTxt = XMLEncryption.decryptElement(elemAccounts,
									m_strPayAccountsPassword);
							}
							catch (Exception ex)
							{
								strMessage = "<html>Wrong password. Please try again</html>";
								continue;
							}
							break ;
						}

						PayAccountsFile.init(theBI, elemPlainTxt);
						m_bPaymentFirstTime = false;
					}
					else
					{
						// accounts data is not encrypted
						elemAccounts = (Element) XMLUtil.getFirstChildByName(elemPay,
							JAPConstants.CONFIG_PAY_ACCOUNTS_FILE);
						if (elemAccounts != null)
						{
							PayAccountsFile.init(theBI, elemAccounts);
							if (PayAccountsFile.getInstance().getNumAccounts() == 0)
							{
								m_bPaymentFirstTime = true;
							}
							else
							{
								m_bPaymentFirstTime = false;
							}
						}
						else
						{
							PayAccountsFile.init(theBI, null);
							m_bPaymentFirstTime = true;
						}
					}
				}

				/* read the settings of the JAP forwarding system, if it is enabled */
//				if (JAPConstants.WITH_BLOCKINGRESISTANCE)
//				{
					Element japForwardingSettingsNode = (Element) (XMLUtil.getFirstChildByName(root,
						JAPConstants.CONFIG_JAP_FORWARDING_SETTINGS));
					if (japForwardingSettingsNode != null)
					{
						JAPModel.getInstance().getRoutingSettings().loadSettingsFromXml(
							japForwardingSettingsNode);
					}
					else
					{
						LogHolder.log(LogLevel.ERR, LogType.MISC, "JAPController: loadConfigFile: Error in XML structure (JapForwardingSettings node): Using default settings for forwarding.");
					}
//				}

			}
			catch (Exception e)
			{
				LogHolder.log(LogLevel.INFO, LogType.MISC,
							  "JAPModel:Error loading configuration! " + e.toString());
			}
		} //end if f!=null
		// fire event
		notifyJAPObservers();
	}

	/**
	 * Tries to load the config file provided in the command line
	 * @return FileInputStream
	 */
	private boolean loadConfigFileCommandLine(String a_configFile)
	{
		LogHolder.log(LogLevel.INFO, LogType.MISC,
					  "JAPController: loadConfigFile: Trying to load configuration from: " + a_configFile);
		try
		{
			FileInputStream f = new FileInputStream(a_configFile);
			/* if we are successful, use this config file also for storing the configuration */
			JAPModel.getInstance().setConfigFile(a_configFile);
			return true;
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.ERR, LogType.MISC,
						  "JAPController: loadConfigFile: Configuration file \"" + a_configFile +
						  "\" not found.");
			return false;
		}
	}

	/**
	 * Tries to load a config file in OS-depended locations
	 * @return boolean
	 */
	private boolean loadConfigFileOSdependent()
	{
		String japConfFile = AbstractOS.getInstance().getConfigPath();
		LogHolder.log(LogLevel.INFO, LogType.MISC,
					  "JAPController: loadConfigFile: Trying to load configuration from: " + japConfFile);
		try
		{
			FileInputStream f = new FileInputStream(japConfFile);
			/* if we are successful, use this config file also for storing the configuration */
			JAPModel.getInstance().setConfigFile(japConfFile);
			return true;
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.ERR, LogType.MISC,
						  "JAPController: loadConfigFile: Configuration file \"" + japConfFile +
						  "\" not found.");
			return false;
		}
	}

	/**
	 * Tries to load a config file from the user's home directory
	 * @return boolean
	 */
	private boolean loadConfigFileHome()
	{
		String japConfFile = System.getProperty("user.home", "") + "/" + JAPConstants.XMLCONFFN;
		LogHolder.log(LogLevel.INFO, LogType.MISC,
					  "JAPController: loadConfigFile: Trying to load configuration from: " + japConfFile);
		try
		{
			FileInputStream f = new FileInputStream(japConfFile);
			/* if we are successful, use this config file also for storing the configuration */
			JAPModel.getInstance().setConfigFile(japConfFile);
			return true;
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.ERR, LogType.MISC,
						  "JAPController: loadConfigFile: Configuration file \"" + japConfFile +
						  "\" not found.");
			return false;
		}

	}

	/**
	 * Tries to load a config file in the current directory
	 * @return boolean
	 */
	private boolean loadConfigFileCurrentDir()
	{
		String japConfFile = JAPConstants.XMLCONFFN;
		LogHolder.log(LogLevel.INFO, LogType.MISC,
					  "JAPController: loadConfigFile: Trying to load configuration from: " + japConfFile);
		try
		{
			FileInputStream f = new FileInputStream(japConfFile);
			/* if we are successful, use this config file also for storing the configuration */
			JAPModel.getInstance().setConfigFile(japConfFile);
			return true;
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.ERR, LogType.MISC,
						  "JAPController: loadConfigFile: Configuration file \"" + japConfFile +
						  "\" not found.");
			return false;
		}

	}

	/**
	 * Changes the common proxy.
	 * @param a_proxyInterface a proxy interface
	 */
	public synchronized void changeProxyInterface(ProxyInterface a_proxyInterface)
	{
		if (a_proxyInterface != null &&
			(m_Model.getProxyInterface() == null ||
			 !m_Model.getProxyInterface().equals(a_proxyInterface)))
		{
			// change settings
			m_Model.setProxyListener(a_proxyInterface);

			applyProxySettingsToInfoService();
			applyProxySettingsToAnonService();

			notifyJAPObservers();
		}
	}

	public boolean saveConfigFile()
	{
		boolean error = false;
		LogHolder.log(LogLevel.INFO, LogType.MISC,
					  "JAPModel:try saving configuration.");
		try
		{
			String sb = getConfigurationAsXmlString();
			if (sb == null)
			{
				error = true;
			}
			else
			{
				/* JAPModel.getModel().getConfigFile() should always point to a valid configuration file */
				FileOutputStream f = new FileOutputStream(JAPModel.getInstance().getConfigFile());
				f.write(sb.getBytes());
				f.flush();
				f.close();
			}
		}
		catch (Throwable e)
		{
			error = true;
		}
		return error;
	}

	String getConfigurationAsXmlString()
	{
		// Save config to xml file
		// Achtung!! Fehler im Sun-XML --> NULL-Attributte koennen hinzugefuegt werden,
		// beim Abspeichern gibt es dann aber einen Fehler!
		try
		{
			Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
			Element e = doc.createElement("JAP");
			doc.appendChild(e);
			XMLUtil.setAttribute(e, JAPConstants.CONFIG_VERSION, "0.18");
			//
			XMLUtil.setAttribute(e, JAPConstants.CONFIG_PORT_NUMBER,
								 Integer.toString(JAPModel.getHttpListenerPortNumber()));
			//XMLUtil.setAttribute(e,"portNumberSocks", Integer.toString(JAPModel.getSocksListenerPortNumber()));
			//XMLUtil.setAttribute(e,"supportSocks",(getUseSocksPort()?"true":"false"));
			XMLUtil.setAttribute(e, JAPConstants.CONFIG_LISTENER_IS_LOCAL, JAPModel.getHttpListenerIsLocal());
			ProxyInterface proxyInterface = m_Model.getProxyInterface();
			boolean bUseProxy = proxyInterface != null && proxyInterface.isValid();
			XMLUtil.setAttribute(e, JAPConstants.CONFIG_PROXY_MODE, bUseProxy);
			if (proxyInterface != null)
			{
				XMLUtil.setAttribute(e, JAPConstants.CONFIG_PROXY_TYPE,
									 m_Model.getProxyInterface().getProtocolAsString().toUpperCase());
				XMLUtil.setAttribute(e, JAPConstants.CONFIG_PROXY_HOST_NAME,
									 m_Model.getProxyInterface().getHost());
				XMLUtil.setAttribute(e, JAPConstants.CONFIG_PROXY_PORT_NUMBER,
									 Integer.toString(m_Model.getProxyInterface().getPort()));
				XMLUtil.setAttribute(e, JAPConstants.CONFIG_PROXY_AUTHORIZATION,
									 m_Model.getProxyInterface().isAuthenticationUsed());
				XMLUtil.setAttribute(e, JAPConstants.CONFIG_PROXY_AUTH_USER_ID,
									 m_Model.getProxyInterface().getAuthenticationUserID());
			}
			/* infoservice configuration options */
			XMLUtil.setAttribute(e, JAPConstants.CONFIG_INFOSERVICE_DISABLED, JAPModel.isInfoServiceDisabled());
			XMLUtil.setAttribute(e, JAPConstants.CONFIG_INFOSERVICE_CHANGE,
								 InfoServiceHolder.getInstance().isChangeInfoServices());
			XMLUtil.setAttribute(e, JAPConstants.CONFIG_INFOSERVICE_TIMEOUT,
								 Integer.toString(HTTPConnectionFactory.getInstance().getTimeout()));

			XMLUtil.setAttribute(e, JAPConstants.CONFIG_DUMMY_TRAFFIC_INTERVALL,
								 Integer.toString(JAPModel.getDummyTraffic()));
			XMLUtil.setAttribute(e, JAPConstants.CONFIG_AUTO_CONNECT, JAPModel.getAutoConnect());
			XMLUtil.setAttribute(e, JAPConstants.CONFIG_AUTO_RECONNECT, JAPModel.getAutoReConnect());
			XMLUtil.setAttribute(e, JAPConstants.CONFIG_PRECREATE_ANON_ROUTES,
								 JAPModel.isPreCreateAnonRoutesEnabled());
			XMLUtil.setAttribute(e, JAPConstants.CONFIG_MINIMIZED_STARTUP, JAPModel.getMinimizeOnStartup());
			XMLUtil.setAttribute(e, JAPConstants.CONFIG_NEVER_REMIND_ACTIVE_CONTENT,
								 mbActCntMessageNeverRemind);
			XMLUtil.setAttribute(e, JAPConstants.CONFIG_NEVER_EXPLAIN_FORWARD, m_bForwarderNotExplain);
			XMLUtil.setAttribute(e, JAPConstants.CONFIG_DO_NOT_ABUSE_REMINDER, mbDoNotAbuseReminder);
			XMLUtil.setAttribute(e, JAPConstants.CONFIG_NEVER_REMIND_GOODBYE, mbGoodByMessageNeverRemind);
			XMLUtil.setAttribute(e, JAPConstants.CONFIG_LOCALE, m_Locale.getLanguage());
			XMLUtil.setAttribute(e, JAPConstants.CONFIG_LOOK_AND_FEEL,
								 UIManager.getLookAndFeel().getClass().getName());

			/*stores user defined MixCascades*/
			Element elemCascades = doc.createElement(JAPConstants.CONFIG_MIX_CASCADES);
			e.appendChild(elemCascades);
			Enumeration enumer = m_vectorMixCascadeDatabase.elements();
			while (enumer.hasMoreElements())
			{
				MixCascade entry = (MixCascade) enumer.nextElement();
				if (entry.isUserDefined())
				{
					Element elem = entry.toXmlNode(doc);
					XMLUtil.setAttribute(elem, JAPConstants.CONFIG_USER_DEFINED, true);
					elemCascades.appendChild(elem);
				}
			}
			/* store the current MixCascade */
			MixCascade defaultMixCascade = getCurrentMixCascade();
			if (defaultMixCascade != null)
			{
				Element elem = defaultMixCascade.toXmlNode(doc);
				if (defaultMixCascade.isUserDefined())
				{
					XMLUtil.setAttribute(elem, JAPConstants.CONFIG_USER_DEFINED, true);
				}
				e.appendChild(elem);
			}

			// adding GUI-Element
			Element elemGUI = doc.createElement(JAPConstants.CONFIG_GUI);
			e.appendChild(elemGUI);
			Element elemMainWindow = doc.createElement(JAPConstants.CONFIG_MAIN_WINDOW);
			elemGUI.appendChild(elemMainWindow);
			if (JAPModel.getSaveMainWindowPosition())
			{
				Element tmp = doc.createElement(JAPConstants.CONFIG_SET_ON_STARTUP);
				elemMainWindow.appendChild(tmp);
				XMLUtil.setValue(tmp, true);
				tmp = doc.createElement(JAPConstants.CONFIG_LOCATION);
				elemMainWindow.appendChild(tmp);
				Point p = m_View.getLocation();
				tmp.setAttribute(JAPConstants.CONFIG_X, Integer.toString(p.x));
				tmp.setAttribute(JAPConstants.CONFIG_Y, Integer.toString(p.y));
				tmp = doc.createElement(JAPConstants.CONFIG_SIZE);
				elemMainWindow.appendChild(tmp);
				Dimension d = m_View.getSize();
				tmp.setAttribute(JAPConstants.CONFIG_DX, Integer.toString(d.width));
				tmp.setAttribute(JAPConstants.CONFIG_DY, Integer.toString(d.height));
			}
			if (JAPModel.getMoveToSystrayOnStartup())
			{
				Element tmp = doc.createElement(JAPConstants.CONFIG_MOVE_TO_SYSTRAY);
				XMLUtil.setValue(tmp, true);
				elemMainWindow.appendChild(tmp);
			}
			if (JAPModel.getDefaultView() == JAPConstants.VIEW_SIMPLIFIED)
			{
				Element tmp = doc.createElement(JAPConstants.CONFIG_DEFAULT_VIEW);
				XMLUtil.setValue(tmp, JAPConstants.CONFIG_SIMPLIFIED);
				elemMainWindow.appendChild(tmp);
			}
			// adding Debug-Element
			Element elemDebug = doc.createElement(JAPConstants.CONFIG_DEBUG);
			e.appendChild(elemDebug);
			Element tmp = doc.createElement(JAPConstants.CONFIG_LEVEL);
			Text txt = doc.createTextNode(Integer.toString(JAPDebug.getInstance().getLogLevel()));
			tmp.appendChild(txt);
			elemDebug.appendChild(tmp);
			tmp = doc.createElement(JAPConstants.CONFIG_TYPE);
			int debugtype = JAPDebug.getInstance().getLogType();
			XMLUtil.setAttribute(tmp, JAPConstants.CONFIG_GUI, ( (debugtype & LogType.GUI) != 0));
			XMLUtil.setAttribute(tmp, JAPConstants.CONFIG_NET, ( (debugtype & LogType.NET) != 0));
			XMLUtil.setAttribute(tmp, JAPConstants.CONFIG_THREAD, ( (debugtype & LogType.THREAD) != 0));
			XMLUtil.setAttribute(tmp, JAPConstants.CONFIG_MISC, ( (debugtype & LogType.MISC) != 0));
			elemDebug.appendChild(tmp);
			if (JAPDebug.isShowConsole() || JAPDebug.isLogToFile())
			{
				tmp = doc.createElement(JAPConstants.CONFIG_OUTPUT);
				elemDebug.appendChild(tmp);
				if (JAPDebug.isShowConsole())
				{
					XMLUtil.setValue(tmp, JAPConstants.CONFIG_CONSOLE);
				}
				if (JAPDebug.isLogToFile())
				{
					Element elemFile = doc.createElement(JAPConstants.CONFIG_FILE);
					tmp.appendChild(elemFile);
					XMLUtil.setValue(elemFile, JAPDebug.getLogFilename());
				}
			}

			/* adding signature verification settings */
			e.appendChild(SignatureVerifier.getInstance().getSettingsAsXml(doc));

			/* adding infoservice settings */
			/* infoservice list */
			e.appendChild(InfoServiceDBEntry.toXmlElement(doc, Database.getInstance(InfoServiceDBEntry.class)));
			/* prefered infoservice */
			InfoServiceDBEntry preferedInfoService = InfoServiceHolder.getInstance().getPreferedInfoService();
			Element preferedInfoServiceNode = doc.createElement(JAPConstants.CONFIG_PREFERED_INFOSERVICE);
			if (preferedInfoService != null)
			{
				preferedInfoServiceNode.appendChild(preferedInfoService.toXmlElement(doc));
			}
			e.appendChild(preferedInfoServiceNode);

			/** add tor*/
			Element elemTor = doc.createElement(JAPConstants.CONFIG_TOR);
			Element elem = doc.createElement(JAPConstants.CONFIG_MAX_CONNECTIONS_PER_ROUTE);
			XMLUtil.setValue(elem, Integer.toString(JAPModel.getTorMaxConnectionsPerRoute()));
			elemTor.appendChild(elem);
			elem = doc.createElement(JAPConstants.CONFIG_ROUTE_LEN);
			elem.setAttribute(JAPConstants.CONFIG_MIN, Integer.toString(JAPModel.getTorMinRouteLen()));
			elem.setAttribute(JAPConstants.CONFIG_MAX, Integer.toString(JAPModel.getTorMaxRouteLen()));
			elemTor.appendChild(elem);
			e.appendChild(elemTor);

			/* save payment configuration */
			PayAccountsFile accounts = PayAccountsFile.getInstance();
			if (accounts != null)
			{
				Element elemPayment = doc.createElement(JAPConstants.CONFIG_PAYMENT);
				e.appendChild(elemPayment);
//				elemPayment.setAttribute("biHost", JAPModel.getBIHost());
//				elemPayment.setAttribute("biPort", Integer.toString(JAPModel.getBIPort()));
				if (accounts.getBI() != null)
				{
					elemPayment.appendChild(accounts.getBI().toXmlElement(doc));

					// get configuration from accountsfile
				}
				Element elemAccounts = accounts.toXmlElement(doc);
				elemPayment.appendChild(elemAccounts);
				if (m_bPaymentFirstTime && PayAccountsFile.getInstance().getNumAccounts() > 0)
				{
					// payment functionality was used for the first time, ask user for password...
					/** @todo internationalize, maybe check password length/strength?? */
					int choice = JOptionPane.showOptionDialog(
						m_View,
						"<html>Sie haben w&auml;hrend dieser JAP-Sitzung zum ersten Mal<br> " +
						"Konten angelegt. Zu jedem Konto geh&ouml;rt auch ein<br> " +
						"privater Schl&uuml;ssel, der sicher verwahrt werden muss.<br> " +
						"Sie haben deshalb jetzt die M&ouml;glichkeit, Ihre<br> " +
						"Kontendaten verschl&uuml;sselt zu speichern.<br> " +
						"Falls Ihre Kontendaten verschl&uuml;sselt sind,<br> " +
						"m&uuml;ssen Sie von nun an bei jedem JAP-Start das Passwort<br> " +
						"zum Entschl&uuml;sseln eingeben.<br><br>" +
						"M&ouml;chten Sie Ihre Kontendaten jetzt verschl&uuml;sseln?</html>",
						"Verschluesselung der Kontendaten",
						JOptionPane.YES_NO_OPTION,
						JOptionPane.QUESTION_MESSAGE,
						null, null, null
						);
					if (choice == JOptionPane.YES_OPTION)
					{
						/** @todo use an IPasswordReader dialog here */
						m_strPayAccountsPassword = JOptionPane.showInputDialog("Geben Sie ein Passwort ein:");
					}
				}
				if (m_strPayAccountsPassword != null && !m_strPayAccountsPassword.equals(""))
				{
					// encrypt XML
					XMLEncryption.encryptElement(elemAccounts, m_strPayAccountsPassword);
				}
			}

			e.appendChild(JAPModel.getInstance().getRoutingSettings().getSettingsAsXml(doc));

			return XMLUtil.toString(doc);
			//((XmlDocument)doc).write(f);
		}
		catch (Throwable ex)
		{
			LogHolder.log(LogLevel.EXCEPTION, LogType.MISC, "JAPModel:save() Exception: " + ex.getMessage());
		}
		return null;
	}

	//---------------------------------------------------------------------
	public static Locale getLocale()
	{
		return m_Controller.m_Locale;
	}

	public static void setLocale(Locale l)
	{
		if (l == null)
		{
			return;
		}
		if (m_Controller.m_Locale != null && m_Controller.m_Locale.equals(l))
		{
			return;
		}
		JAPMessages.init(l);
		m_Controller.m_Locale = l;
		Locale.setDefault(l);
		if (m_View != null)
		{
			m_View.localeChanged();
		}
	}

	//---------------------------------------------------------------------
	public void setMinimizeOnStartup(boolean b)
	{
		synchronized (this)
		{
			m_Model.setMinimizeOnStartup(b);
		}
	}

	public void setMoveToSystrayOnStartup(boolean b)
	{
		synchronized (this)
		{
			m_Model.setMoveToSystrayOnStartup(b);
		}
	}

	public void setDefaultView(int defaultView)
	{
		synchronized (this)
		{
			m_Model.setDefaultView(defaultView);
		}
	}

	/**
	 * Changes the active MixCascade.
	 *
	 * @param newMixCascade The MixCascade which is activated.
	 */
	public void setCurrentMixCascade(MixCascade newMixCascade)
	{
		if (newMixCascade != null && !m_currentMixCascade.getId().equals(newMixCascade.getId()))
		{
			synchronized (this)
			{
				/* we need consistent states */
				if ( (getAnonMode()) && (m_currentMixCascade != null))
				{
					/* we are running in anonymity mode */
					setAnonMode(false);
					m_currentMixCascade = newMixCascade;
					LogHolder.log(LogLevel.DEBUG, LogType.MISC,
								  "JAPController: setCurrentMixCascade: MixCascade changed while in anonymity mode.");
					setAnonMode(true);
				}
				else
				{
					m_currentMixCascade = newMixCascade;
				}
			}
			notifyJAPObservers();
		}
	}

	/**
	 * Returns the active MixCascade.
	 *
	 * @return The active MixCascade.
	 */
	public MixCascade getCurrentMixCascade()
	{
		synchronized (this)
		{
			/* return only consistent values */
			return m_currentMixCascade;
		}
	}

	public Vector getMixCascadeDatabase()
	{
		return m_vectorMixCascadeDatabase;
	}

	void applyProxySettingsToInfoService()
	{
		if (m_Model.getProxyInterface().isValid())
		{
			HTTPConnectionFactory.getInstance().setNewProxySettings(m_Model.getProxyInterface());
		}
		else
		{
			//no Proxy should be used....
			HTTPConnectionFactory.getInstance().setNewProxySettings(null);
		}
	}

	private void applyProxySettingsToAnonService()
	{
		if (JAPModel.getInstance().getProxyInterface().isValid() && getAnonMode())
		{
			// anon service is running
			Object[] options =
				{
				JAPMessages.getString("later"), JAPMessages.getString("reconnect")};
			int ret = JOptionPane.showOptionDialog(JAPController.getView(),
				JAPMessages.getString("reconnectAfterProxyChangeMsg"),
				JAPMessages.getString("reconnectAfterProxyChangeTitle"),
				JOptionPane.DEFAULT_OPTION,
				JOptionPane.WARNING_MESSAGE,
				null, options, options[0]);
			if (ret == 1)
			{
				// reconnect
				setAnonMode(false);
				setAnonMode(true);
			}
		}
	}

	public static Font getDialogFont()
	{
		if (m_fontControls != null)
		{
			return m_fontControls;
		}
		m_fontControls = new JButton().getFont();
		if (JAPModel.isSmallDisplay())
		{
			m_fontControls = new Font(m_fontControls.getName(), JAPConstants.SMALL_FONT_STYLE,
									  JAPConstants.SMALL_FONT_SIZE);
		}
		return m_fontControls;
	}

	public static String getFirewallAuthPasswd_()
	{
		/*
		   if (JAPModel.getUseFirewallAuthorization())
		   {
		 if (JAPModel.getFirewallAuthPasswd() == null)
		 {
		  m_Model.setFirewallAuthPasswd(JAPFirewallPasswdDlg.getPasswd());
		 }
		 return JAPModel.getFirewallAuthPasswd();
		   }
		   else
		   {
		 return null;
		   }*/
		return null;
	}

	public static void setInfoServiceDisabled(boolean b)
	{
		m_Model.setInfoServiceDisabled(b);
	}

	public void setPreCreateAnonRoutes(boolean b)
	{
		m_Model.setPreCreateAnonRoutes(b);
	}

	public static void setSaveMainWindowPosition(boolean b)
	{
		m_Model.setSaveMainWindowPosition(b);
	}

	//---------------------------------------------------------------------

	/* public void setSocksPortNumber(int p)
	 {
	   m_Model.setSocksListenerPortNumber(p);
	 }*/

	//---------------------------------------------------------------------
	//---------------------------------------------------------------------
	//---------------------------------------------------------------------
	private final class SetAnonModeAsync implements Runnable
	{
		private boolean ServerModeSelected = false;
		private int id = 0;

		public SetAnonModeAsync(boolean b)
		{
			synchronized (oAnonSetThreadIDSyncObject)
			{
				ServerModeSelected = b;
				ms_AnonModeAsyncLastStarted++;
				id = ms_AnonModeAsyncLastStarted;

				new Thread(this, this.getClass().getName()).start();
			}

		}

		/** @todo Still very bugy, because mode change is async done but not
		 * all properties (like currentMixCascade etc.)are synchronized!!
		 *
		 */

		public void run()
		{
			synchronized (oAnonSyncObject)
			{
				while (id != ms_AnonModeAsyncLastFinished + 1)
				{
					try
					{
						oAnonSyncObject.wait();
					}
					catch (InterruptedException ieo)
					{
						LogHolder.log(LogLevel.EXCEPTION, LogType.THREAD,
									  "Waiting for becoming current SetServerModeAsnyc Thread intterrupted!");
					}
				}
				setServerMode(ServerModeSelected);
				ms_AnonModeAsyncLastFinished++;
				oAnonSyncObject.notifyAll();
			}
		}

		/** @todo Still very bugy, because mode change is async done but not
		 * all properties (like currentMixCascade etc.)are synchronized!!
		 *
		 * @param anonModeSelected true, if anonymity should be started; false otherwise
		 */
		private void setServerMode(boolean anonModeSelected)
		{
			//JAPWaitSplash splash = null;
			int msgIdConnect = 0;
			boolean canStartService = true;
			//setAnonMode--> async!!
			LogHolder.log(LogLevel.DEBUG, LogType.MISC, "JAPModel:setAnonMode(" + anonModeSelected + ")");
			if ( (m_proxyAnon == null) && (anonModeSelected))
			{ //start Anon Mode
				m_View.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				msgIdConnect = m_View.addStatusMsg(JAPMessages.getString("setAnonModeSplashConnect"),
					JOptionPane.INFORMATION_MESSAGE, false);
				//splash = JAPWaitSplash.start(JAPMessages.getString("setAnonModeSplashConnect"),
				//							 JAPMessages.getString("setAnonModeSplashTitle"));
				if ( (!m_bAlreadyCheckedForNewVersion) && (!JAPModel.isInfoServiceDisabled()) &&
					( (JAPModel.getInstance().getRoutingSettings().getRoutingMode() !=
					   JAPRoutingSettings.ROUTING_MODE_CLIENT) ||
					 (!JAPModel.getInstance().getRoutingSettings().getForwardInfoService())))
				{
					/* check for a new version of JAP if not already done, automatic infoservice requests
					 * are allowed and we don't use forwarding with also forwarded infoservice (because
					 * if the infoservice is also forwarded, we would need a initialized connection to a
					 * mixcascade, which we don't have at the moment, maybe later versions of the
					 * forwarding protocol will support direct connections to the infoservice too and not
					 * via the mixcascade -> we will be able to do the version check here)
					 */
					int ok = versionCheck();
					if (ok != -1)
					{
						// -> we can start anonymity
						m_bAlreadyCheckedForNewVersion = true;
					}
					else
					{
						canStartService = false;
					}
				}
				if (canStartService)
				{
					// starting MUX --> Success ???
					if (JAPModel.getInstance().getRoutingSettings().getRoutingMode() ==
						JAPRoutingSettings.ROUTING_MODE_CLIENT)
					{
						/* we use a forwarded connection */
						m_proxyAnon = JAPModel.getInstance().getRoutingSettings().getAnonProxyInstance(
							m_socketHTTPListener);
					}
					else
					{
						/* we use a direct connection */
						if (JAPModel.getInstance().getProxyInterface().isValid())
						{
							m_proxyAnon = new AnonProxy(
								m_socketHTTPListener, JAPModel.getInstance().getProxyInterface());
						}
						else
						{
							m_proxyAnon = new AnonProxy(m_socketHTTPListener, null);
						}

					}
					MixCascade currentMixCascade = m_Controller.getCurrentMixCascade();
					m_proxyAnon.setMixCascade(currentMixCascade);
					m_proxyAnon.setAutoReConnect(JAPModel.getAutoReConnect());
					m_proxyAnon.setPreCreateAnonRoutes(JAPModel.isPreCreateAnonRoutesEnabled());
					m_proxyAnon.setProxyListener(m_Controller);
					m_proxyAnon.setDummyTraffic(JAPModel.getDummyTraffic());
					// -> we can try to start anonymity
					if (m_proxyDirect != null)
					{
						m_proxyDirect.stopService();
					}
					m_proxyDirect = null;

					int ret = m_proxyAnon.start();
					if (ret != ErrorCodes.E_SUCCESS)
					{
						canStartService = false;
						m_proxyAnon = null;
					}
					if (ret == ErrorCodes.E_SUCCESS)
					{
						if (!mbActCntMessageNotRemind && !JAPModel.isSmallDisplay())
						{
							// show a Reminder message that active contents should be disabled
							Object[] options =
								{
								JAPMessages.getString("okButton")};
							JCheckBox checkboxRemindNever = new JCheckBox(JAPMessages.getString(
								"disableActCntMessageNeverRemind"));
							Object[] message =
								{
								JAPMessages.getString("disableActCntMessage"), checkboxRemindNever};
							ret = 0;
							ret = JOptionPane.showOptionDialog(getView(),
								(Object) message,
								JAPMessages.getString("disableActCntMessageTitle"),
								JOptionPane.DEFAULT_OPTION,
								JOptionPane.WARNING_MESSAGE,
								null, options, options[0]);
							mbActCntMessageNeverRemind = checkboxRemindNever.isSelected();
							mbDoNotAbuseReminder = checkboxRemindNever.isSelected();
							if (mbActCntMessageNeverRemind)
							{
								mbActCntMessageNotRemind = true;
							}
						}
						// start feedback thread
						feedback = new JAPFeedback();
						feedback.startRequests();
					}
					else if (ret == AnonProxy.E_BIND)
					{
						Object[] args =
							{
							new Integer(JAPModel.getHttpListenerPortNumber())};
						String msg = MessageFormat.format(JAPMessages.getString("errorListenerPort"),
							args);
						JOptionPane.showMessageDialog(JAPController.getView(),
							msg,
							JAPMessages.getString("errorListenerPortTitle"),
							JOptionPane.ERROR_MESSAGE);
						LogHolder.log(LogLevel.EMERG, LogType.NET, "Listener could not be started!");
						JAPController.getView().disableSetAnonMode();
					}
					else if (ret == AnonProxy.E_MIX_PROTOCOL_NOT_SUPPORTED)
					{
						JOptionPane.showMessageDialog
							(
								getView(),
								JAPMessages.getString("errorMixProtocolNotSupported"),
								JAPMessages.getString("errorMixProtocolNotSupportedTitle"),
								JOptionPane.ERROR_MESSAGE
							);
					}
//otte
					else if (ret == AnonProxy.E_SIGNATURE_CHECK_FIRSTMIX_FAILED)
					{
						JOptionPane.showMessageDialog
							(
								getView(),
								JAPMessages.getString("errorMixFirstMixSigCheckFailed"),
								JAPMessages.getString("errorMixFirstMixSigCheckFailedTitle"),
								JOptionPane.ERROR_MESSAGE
							);
					}

					else if (ret == AnonProxy.E_SIGNATURE_CHECK_OTHERMIX_FAILED)
					{
						JOptionPane.showMessageDialog
							(
								getView(),
								JAPMessages.getString("errorMixOtherMixSigCheckFailed"),
								JAPMessages.getString("errorMixOtherMixSigCheckFailedTitle"),
								JOptionPane.ERROR_MESSAGE
							);
					}
					// ootte
					else
					{
						if (!JAPModel.isSmallDisplay())
						{
							JOptionPane.showMessageDialog
								(
									getView(),
									JAPMessages.getString("errorConnectingFirstMix") + "\n" +
									JAPMessages.getString("errorCode") + ": " + Integer.toString(ret),
									JAPMessages.getString("errorConnectingFirstMixTitle"),
									JOptionPane.ERROR_MESSAGE
								);
						}
					}
				}
				m_View.setCursor(Cursor.getDefaultCursor());
				notifyJAPObservers();
				//splash.abort();
				m_View.removeStatusMsg(msgIdConnect);
				if (!canStartService)
				{
					setAnonMode(false);
				}
			}
			else if ( (m_proxyDirect == null) && (!anonModeSelected))
			{
				if (m_proxyAnon != null)
				{
					msgIdConnect = m_View.addStatusMsg(JAPMessages.getString(
						"setAnonModeSplashDisconnect"),
						JOptionPane.INFORMATION_MESSAGE, false);
					//splash = JAPWaitSplash.start(JAPMessages.getString("setAnonModeSplashDisconnect"),
					//	JAPMessages.getString("setAnonModeSplashTitle"));
					m_proxyAnon.stop();
				}
				m_proxyAnon = null;
				/* if (m_proxySocks != null)
				 {
				   m_proxySocks.stop();
				 }
				 m_proxySocks = null;*/
				if (feedback != null)
				{
					feedback.stopRequests();
					feedback = null;
				}
				m_proxyDirect = new DirectProxy(m_socketHTTPListener);
				m_proxyDirect.startService();

				getCurrentMixCascade().resetCurrentStatus();

				/* notify the forwarding system after! m_proxyAnon is set to null */
				JAPModel.getInstance().getRoutingSettings().anonConnectionClosed();

				notifyJAPObservers();
				if (msgIdConnect != 0)
				{
					m_View.removeStatusMsg(msgIdConnect);
				}
			}
		}

	} //end of class SetAnonModeAsync

	//---------------------------------------------------------------------
	//---------------------------------------------------------------------
	//---------------------------------------------------------------------
	public boolean getAnonMode()
	{
		return m_proxyAnon != null;
	}

	public synchronized void setAnonMode(boolean anonModeSelected)
	{
		new SetAnonModeAsync(anonModeSelected);
	}

	/**
	 * This will do all necessary things in order to enable the anonymous mode. The method decides
	 * whether to establish the connection via a forwarder or direct to the selected anonymity
	 * service.
	 * Attention: Maybe it is necessary to show a dialog in order to get the information about a
	 *            forwarder. Thus only the Java-AWT event dispatch thread should call this method.
	 *            Any other caller will produce a freeze, if the connect-to-forwarder dialog
	 *            appears.
	 *
	 * @param a_parentComponent The parent component over which the connect to forwarder dialog (if
	 *                          necessary) is centered.
	 */
	public void startAnonymousMode(Component a_parentComponent)
	{
		/* decide whether to establish a forwarded connection or not */
		if (JAPModel.getInstance().getRoutingSettings().isConnectViaForwarder())
		{
			/* show the connect via forwarder dialog -> the dialog will do the remaining things */
			new JAPRoutingEstablishForwardedConnectionDialog(a_parentComponent, getDialogFont());
			/* maybe connection to forwarder failed -> notify the observers, because the view maybe
			 * still shows the anonymity mode enabled
			 */
			notifyJAPObservers();
		}
		else
		{
			/* simply enable the anonymous mode */
			setAnonMode(true);
		}
	}

	public void setDummyTraffic(int msIntervall)
	{
		m_Model.setDummyTraffic(msIntervall);
		ForwardServerManager.getInstance().setDummyTrafficInterval(msIntervall);
		if (m_proxyAnon != null)
		{
			m_proxyAnon.setDummyTraffic(msIntervall);
		}
	}

	public void setAutoConnect(boolean b)
	{
		m_Model.setAutoConnect(b);
	}

	public void setAutoReConnect(boolean b)
	{
		m_Model.setAutoReConnect(b);
		if (m_proxyAnon != null)
		{
			m_proxyAnon.setAutoReConnect(b);
		}
	}

	//---------------------------------------------------------------------
	public void setHTTPListener(int port, boolean isLocal, boolean bShowWarning)
	{
		if (JAPModel.getHttpListenerPortNumber() == port)
		{
			bShowWarning = false;
		}
		if ( (JAPModel.getHttpListenerPortNumber() != port) ||
			(JAPModel.getHttpListenerIsLocal() != isLocal))
		{
			m_Model.setHttpListenerPortNumber(port);
			synchronized (this)
			{
				m_Model.setHttpListenerIsLocal(isLocal);
			}
			LogHolder.log(LogLevel.DEBUG, LogType.MISC, "JAPModel:HTTP listener settings changed");
			if (bShowWarning)
			{
				JOptionPane.showMessageDialog(getView(),
											  JAPMessages.getString("confmessageListernPortChanged"));
			}
			m_Controller.notifyJAPObservers();
		}
	}

	/** @deprecated to be removed
	 *  @param host BI hostname
	 * */
	public static void setBIHost(String host)
	{
		JAPModel.setBIHost(host);
		m_Controller.notifyJAPObservers();
	}

	/** @deprecated to be removed
	 *  @param port BI port number
	 * */
	public static void setBIPort(int port)
	{
		JAPModel.setBIPort(port);
		m_Controller.notifyJAPObservers();
	}

	public static void setTorMaxConnectionsPerRoute(int i)
	{
		m_Model.setTorMaxConnectionsPerRoute(i);
	}

	public static void setTorRouteLen(int min, int max)
	{
		m_Model.setTorMaxRouteLen(max);
		m_Model.setTorMinRouteLen(min);
	}

	//---------------------------------------------------------------------
	private ServerSocket intern_startListener(int port, boolean bLocal)
	{
		LogHolder.log(LogLevel.DEBUG, LogType.MISC, "JAPModel:startListener on port: " + port);
		ServerSocket s = null;
		for (int i = 0; i < 10; i++) //HAck for Mac!!
		{
			try
			{
				if (bLocal)
				{
					//InetAddress[] a=InetAddress.getAllByName("localhost");
					InetAddress[] a = InetAddress.getAllByName("127.0.0.1");
					LogHolder.log(LogLevel.DEBUG, LogType.NET,
								  "Try binding Listener on localhost: " + a[0]);
					s = new ServerSocket(port, 50,
										 a[0]);
				}
				else
				{
					s = new ServerSocket(port);
				}
				LogHolder.log(LogLevel.INFO, LogType.NET,
							  "Listener on port " + port +
							  " started.");
				try
				{
					s.setSoTimeout(2000);
				}
				catch (Exception e1)
				{
					LogHolder.log(LogLevel.DEBUG, LogType.NET,
								  "Could not set listener accept timeout: Exception: " +
								  e1.getMessage());
				}
				break ;
			}
			catch (Exception e)
			{
				LogHolder.log(LogLevel.DEBUG, LogType.NET, "Exception: " + e.getMessage());
				s = null;
			}
		}
		return s;
	}

	private boolean startHTTPListener()
	{
		LogHolder.log(LogLevel.DEBUG, LogType.MISC, "JAPModel:start HTTP Listener");
		if (!isRunningHTTPListener)
		{
			m_socketHTTPListener = intern_startListener(JAPModel.getHttpListenerPortNumber(),
				JAPModel.getHttpListenerIsLocal());
			if (m_socketHTTPListener != null)
			{
				isRunningHTTPListener = true;
			}
		}
		return isRunningHTTPListener;
	}

	private void stopHTTPListener()
	{
		LogHolder.log(LogLevel.DEBUG, LogType.MISC, "JAPModel:stopListener");
		if (isRunningHTTPListener)
		{
			setAnonMode(false);
			try
			{
				m_socketHTTPListener.close();
			}
			catch (Exception e)
			{}
			;
			m_socketHTTPListener = null;
			isRunningHTTPListener = false;
		}
	}

	/* private boolean startSOCKSListener()
	 {
	   LogHolder.log(LogLevel.DEBUG, LogType.MISC, "JAPModel:start SOCKS Listener");
	   if (isRunningSOCKSListener == false)
	   {
	  m_socketSOCKSListener = intern_startListener(JAPModel.getSocksListenerPortNumber(),
	 JAPModel.getHttpListenerIsLocal());
	  if (m_socketSOCKSListener != null)
	  {
	 isRunningSOCKSListener = true;
	  }
	   }
	   return isRunningSOCKSListener;
	 }
	 */
	//---------------------------------------------------------------------

	/** This (and only this) is the final exit procedure of JAP!
	 * It shows a reminder to reset the proxy configurations and saves the current configuration.
	 *	@param bShowConfigSaveErrorMsg if true shows an error message iff saving of
	 * 			the current configuration goes wrong
	 */
	public static void goodBye(boolean bShowConfigSaveErrorMsg)
	{
		try
		{
			// show a Reminder message that active contents should be disabled
			Object[] options =
				{
				JAPMessages.getString("okButton")};
			JCheckBox checkboxRemindNever = new JCheckBox(JAPMessages.getString(
				"disableGoodByMessageNeverRemind"));
			Object[] message =
				{
				JAPMessages.getString("disableGoodByMessage"), checkboxRemindNever};
			if (!m_Controller.mbGoodByMessageNeverRemind)
			{
				JOptionPane.showOptionDialog(getView(),
											 (Object) message,
											 JAPMessages.getString("disableGoodByMessageTitle"),
											 JOptionPane.DEFAULT_OPTION,
											 JOptionPane.WARNING_MESSAGE,
											 null, options, options[0]);
				m_Controller.mbGoodByMessageNeverRemind = checkboxRemindNever.isSelected();
			}
			boolean error = m_Controller.saveConfigFile();
			if (error && bShowConfigSaveErrorMsg)
			{
				LogHolder.log(LogLevel.ERR, LogType.MISC,
							  "JAPController: saveConfigFile: Error saving configuration to: " +
							  JAPModel.getInstance().getConfigFile());
				JOptionPane.showMessageDialog(getView(),
											  JAPMessages.getString("errorSavingConfig"),
											  JAPMessages.getString("errorSavingConfigTitle"),
											  JOptionPane.ERROR_MESSAGE);
			}
		}
		catch (Throwable t)
		{
		}
		System.exit(0);
	}

	/** Shows the About dialog
	 */
	public static void aboutJAP()
	{
		try
		{
			new JAPAbout(m_View);
		}
		catch (Throwable t)
		{
			t.printStackTrace();
		}
	}

	/**
	 * Returns the global help context object
	 * @return JAPHelpContext
	 */
	public JAPHelpContext getHelpContext()
	{
		return m_helpContext;
	}

	/**
	 * Get all available mixcascades from the infoservice and store it in the database.
	 * @param bShowError should an Error Message be displayed if something goes wrong ?
	 */
	public void fetchMixCascades(boolean bShowError)
	{
		LogHolder.log(LogLevel.INFO, LogType.MISC,
					  "JAPController: fetchMixCascades: Trying to fetch mixcascades from infoservice.");
		Vector newMixCascades = InfoServiceHolder.getInstance().getMixCascades();
		if (newMixCascades == null)
		{
			LogHolder.log(LogLevel.ERR, LogType.NET,
						  "JAPController: fetchMixCascades: No connection to infoservices.");
			if (!JAPModel.isSmallDisplay() && bShowError)
			{
				JOptionPane.showMessageDialog(m_View, JAPMessages.getString("errorConnectingInfoService"),
											  JAPMessages.getString("errorConnectingInfoServiceTitle"),
											  JOptionPane.ERROR_MESSAGE);
			}
		}
		else
		{
			LogHolder.log(LogLevel.DEBUG, LogType.NET, "JAPController: fetchMixCascades: success!");
			//add all user added cascades
			Enumeration enumer = m_vectorMixCascadeDatabase.elements();
			while (enumer.hasMoreElements())
			{
				MixCascade entry = (MixCascade) enumer.nextElement();
				if (entry.isUserDefined())
				{
					newMixCascades.addElement(entry);
				}
			}
			m_vectorMixCascadeDatabase = newMixCascades;
			notifyJAPObservers();
		}
	}

	/**
	 * Performs the Versioncheck.
	 *
	 * @return  0, if the local JAP version is up to date.
	 *         -1, if version check says that anonymity mode should not be enabled. Reasons can be:
	 *             new version found, version check failed.
	 */
	public int versionCheck()
	{
		LogHolder.log(LogLevel.INFO, LogType.MISC,
					  "JAPController: versionCheck: Checking for new version of JAP...");
		int result = 0;
		String currentVersionNumber = InfoServiceHolder.getInstance().getNewVersionNumber();
		if (currentVersionNumber != null)
		{
			currentVersionNumber = currentVersionNumber.trim();
			LogHolder.log(LogLevel.DEBUG, LogType.MISC,
						  "JAPController: versionCheck: Local version: " + JAPConstants.aktVersion);
			if (currentVersionNumber.compareTo(JAPConstants.aktVersion) <= 0)
			{
				/* the local JAP version is up to date -> exit */
				return 0;
			}
			/* local version is not up to date, new version is available -> ask the user whether to
			 * download the new version or not
			 */
			int answer = JOptionPane.showConfirmDialog(m_View,
				JAPMessages.getString("newVersionAvailable"),
				JAPMessages.getString("newVersionAvailableTitle"),
				JOptionPane.YES_NO_OPTION);
			if (answer == JOptionPane.YES_OPTION)
			{
				/* User has elected to download new version of JAP -> Download, Alert, exit program */
				JAPVersionInfo vi = InfoServiceHolder.getInstance().getJAPVersionInfo(JAPVersionInfo.
					JAP_RELEASE_VERSION);
				if (vi != null)
				{
					//store current configuration first
					saveConfigFile();
					JAPUpdateWizard wz = new JAPUpdateWizard(vi);
					/* we got the JAPVersionInfo from the infoservice */
					/* Assumption: If we are here, the download failed for some resaons, otherwise the
					 * program would quit
					 */
					//TODO: Do this in a better way!!
					if (wz.getStatus() != JAPUpdateWizard.UPDATESTATUS_SUCCESS)
					{
						/* Download failed -> alert, and reset anon mode to false */
						LogHolder.log(LogLevel.ERR, LogType.MISC,
									  "JAPController: versionCheck: Some update problem.");
						JOptionPane.showMessageDialog(m_View,
							JAPMessages.getString("downloadFailed") + JAPMessages.getString("infoURL"),
							JAPMessages.getString("downloadFailedTitle"), JOptionPane.ERROR_MESSAGE);
						notifyJAPObservers();
						/* update failed -> exit */
						return -1;
					}
					/* should never be reached, because if update was successful, the JAPUpdateWizard closes
					 * JAP
					 */
					goodBye(false);
					return 0;
				}
				/* update was not successful, because we could not get the JAPVersionInfo -> alert, and
				 * reset anon mode to false
				 */
				LogHolder.log(LogLevel.ERR, LogType.MISC,
							  "JAPController: versionCheck: Could not get JAPVersionInfo.");
				JOptionPane.showMessageDialog(m_View,
											  JAPMessages.getString("downloadFailed") +
											  JAPMessages.getString("infoURL"),
											  JAPMessages.getString("downloadFailedTitle"),
											  JOptionPane.ERROR_MESSAGE);
				notifyJAPObservers();
				/* update failed -> exit */
				return -1;
			}
			else
			{
				/* User has elected not to download -> Alert, we should'nt start the system due to
				 * possible compatibility problems
				 */
				JOptionPane.showMessageDialog(m_View,
											  JAPMessages.getString("youShouldUpdate") +
											  JAPMessages.getString("infoURL"),
											  JAPMessages.getString("youShouldUpdateTitle"),
											  JOptionPane.WARNING_MESSAGE);
				notifyJAPObservers();
				return -1;
			}
		}
		else
		{
			/* can't get the current version number from the infoservices -> Alert, and reset anon
			 * mode to false
			 */
			LogHolder.log(LogLevel.ERR, LogType.MISC,
						  "JAPController: versionCheck: Could not get the current JAP version number from infoservice.");
			JAPUtil.showMessageBox(m_View, "errorConnectingInfoService",
								   "errorConnectingInfoServiceTitle",
								   JOptionPane.ERROR_MESSAGE);
			notifyJAPObservers();
			return -1;
		}
		/* this line should never be reached */
	}

	//---------------------------------------------------------------------
	public void registerMainView(AbstractJAPMainView v)
	{
		m_View = v;
	}

	public static AbstractJAPMainView getView()
	{
		return JAPController.m_View;
	}

	//---------------------------------------------------------------------
	public void addJAPObserver(JAPObserver o)
	{
		observerVector.addElement(o);
	}

	public void notifyJAPObservers()
	{
		LogHolder.log(LogLevel.DEBUG, LogType.MISC, "JAPModel:notifyJAPObservers()");
		synchronized (observerVector)
		{
			try
			{
				Enumeration enumer = observerVector.elements();
				int i = 0;
				while (enumer.hasMoreElements())
				{
					JAPObserver listener = (JAPObserver) enumer.nextElement();
					LogHolder.log(LogLevel.DEBUG, LogType.MISC, "JAPModel:notifyJAPObservers: " + i);
					listener.valuesChanged(false);
					i++;
				}
			}
			catch (Throwable t)
			{
				LogHolder.log(LogLevel.EMERG, LogType.MISC,
							  "JAPModel:notifyJAPObservers - critical exception: " + t.getMessage());
			}
		}
		LogHolder.log(LogLevel.DEBUG, LogType.MISC, "JAPModel:notifyJAPObservers()-ended");
	}

	//---------------------------------------------------------------------
	public synchronized void channelsChanged(int channels)
	{
		//nrOfChannels = channels;
		Enumeration enumer = observerVector.elements();
		while (enumer.hasMoreElements())
		{
			JAPObserver listener = (JAPObserver) enumer.nextElement();
			listener.channelsChanged(channels);
		}
	}

	public synchronized void transferedBytes(int bytes, int protocolType)
	{
		int b;
		if (protocolType == ProxyListener.PROTOCOL_WWW)
		{
			m_nrOfBytesWWW += bytes;
			b = m_nrOfBytesWWW;
		}
		else if (protocolType == ProxyListener.PROTOCOL_OTHER)
		{
			m_nrOfBytesOther += bytes;
			b = m_nrOfBytesOther;
		}
		else
		{
			return;
		}
		Enumeration enumer = observerVector.elements();
		while (enumer.hasMoreElements())
		{
			JAPObserver listener = (JAPObserver) enumer.nextElement();
			listener.transferedBytes(b, protocolType);
		}
	}

	/**
	 * This is the observer implementation. At the moment only the routing system is observed.
	 * It's just for comfort reasons, so there is no need to registrate the JAPView at all
	 * observable objects. We collect all messages here and send them to the view. But it's also
	 * possible to registrate directly at the observed objects. So every developer can decide,
	 * whether to use the common JAP notification system or the specific ones. Also keep in mind,
	 * that maybe not all messages are forwarded to the common notification system (like statistic messages).
	 *
	 * @param a_notifier The observed Object (various forwarding related objects).
	 * @param a_message The reason of the notification, e.g. a JAPRoutingMessage.
	 *
	 */
	public void update(Observable a_notifier, Object a_message)
	{
		try
		{
			if (a_notifier == JAPModel.getInstance().getRoutingSettings())
			{
				/* message is from JAPRoutingSettings */
				if ( ( (JAPRoutingMessage) (a_message)).getMessageCode() ==
					JAPRoutingMessage.ROUTING_MODE_CHANGED)
				{
					/* routing mode was changed -> notify the observers of JAPController */
					notifyJAPObservers();
				}
				if ( ( (JAPRoutingMessage) (a_message)).getMessageCode() ==
					JAPRoutingMessage.CLIENT_SETTINGS_CHANGED)
				{
					/* the forwarding-client settings were changed -> notify the observers of JAPController */
					notifyJAPObservers();
				}

			}
			else if (a_notifier == JAPModel.getInstance().getRoutingSettings().getRegistrationStatusObserver())
			{
				/* message is from JAPRoutingRegistrationStatusObserver */
				if ( ( (JAPRoutingMessage) (a_message)).getMessageCode() ==
					JAPRoutingMessage.REGISTRATION_STATUS_CHANGED)
				{
					/* the registration status of the local forwarding server has changed */
					notifyJAPObservers();
				}
			}
		}
		catch (Exception e)
		{
			/* should not happen, but better than throwing a runtime exception */
		}
	}

/////////////////////////////
////////////////////////////
	private final class SetForwardingServerModeAsync extends AbstractJAPSetServerModeAsync
	{
		public SetForwardingServerModeAsync(boolean b)
		{
			super(b);
		}

		void setServerMode(boolean b)
		{
			if (!m_bForwarderNotExplain && b)
			{
				Object[] options =
					{
					JAPMessages.getString("okButton")};
				JCheckBox checkboxRemindNever = new JCheckBox(JAPMessages.getString(
					"disableActCntMessageNeverRemind"));
				Object[] message =
					{
					JAPMessages.getString("forwardingExplainMessage"), checkboxRemindNever};
				int ret = 0;
				ret = JOptionPane.showOptionDialog(getView(),
					(Object) message,
					JAPMessages.getString("forwardingExplainMessageTitle"),
					JOptionPane.DEFAULT_OPTION,
					JOptionPane.WARNING_MESSAGE,
					null, options, options[0]);
				m_bForwarderNotExplain = checkboxRemindNever.isSelected();
			}
			if (m_iStatusPanelMsgIdForwarderServerStatus != -1)
			{
				m_View.removeStatusMsg(m_iStatusPanelMsgIdForwarderServerStatus);
				m_iStatusPanelMsgIdForwarderServerStatus = -1;
			}

			boolean returnValue = false;
			// don't allow to interrupt the client routing mode //
			if (JAPModel.getInstance().getRoutingSettings().getRoutingMode() !=
				JAPRoutingSettings.ROUTING_MODE_CLIENT)
			{
				if (b)
				{
					int msgId = m_View.addStatusMsg(JAPMessages.getString(
						"controllerStatusMsgRoutingStartServer"), JOptionPane.INFORMATION_MESSAGE, false);
					// start the server //
					returnValue = JAPModel.getInstance().getRoutingSettings().setRoutingMode(
						JAPRoutingSettings.
						ROUTING_MODE_SERVER);
					if (returnValue)
					{
						// starting the server was successful -> start propaganda with blocking //
						int registrationStatus = JAPModel.getInstance().getRoutingSettings().startPropaganda(true);
						m_View.removeStatusMsg(msgId);
						if (registrationStatus == JAPRoutingSettings.REGISTRATION_NO_INFOSERVICES)
						{
							JOptionPane.showMessageDialog(m_View,
								new JAPHtmlMultiLineLabel(JAPMessages.
								getString("settingsRoutingServerRegistrationEmptyListError"), getDialogFont()),
								JAPMessages.getString("ERROR"), JOptionPane.ERROR_MESSAGE);
						}
						else if (registrationStatus == JAPRoutingSettings.REGISTRATION_UNKNOWN_ERRORS)
						{
							JOptionPane.showMessageDialog(m_View,
								new JAPHtmlMultiLineLabel(JAPMessages.
								getString("settingsRoutingServerRegistrationUnknownError"), getDialogFont()),
								JAPMessages.getString("ERROR"), JOptionPane.ERROR_MESSAGE);
						}
						else if (registrationStatus == JAPRoutingSettings.REGISTRATION_INFOSERVICE_ERRORS)
						{
							JOptionPane.showMessageDialog(m_View,
								new JAPHtmlMultiLineLabel(JAPMessages.
								getString("settingsRoutingServerRegistrationInfoservicesError"),
								getDialogFont()), JAPMessages.getString("ERROR"), JOptionPane.ERROR_MESSAGE);
						}
						else if (registrationStatus == JAPRoutingSettings.REGISTRATION_VERIFY_ERRORS)
						{
							JOptionPane.showMessageDialog(m_View,
								new JAPHtmlMultiLineLabel(JAPMessages.
								getString("settingsRoutingServerRegistrationVerificationError"),
								getDialogFont()), JAPMessages.getString("ERROR"), JOptionPane.ERROR_MESSAGE);
						}
						else if (registrationStatus == JAPRoutingSettings.REGISTRATION_SUCCESS)
						{
							m_iStatusPanelMsgIdForwarderServerStatus = m_View.addStatusMsg(JAPMessages.
								getString("controllerStatusMsgRoutingStartServerSuccess"),
								JOptionPane.INFORMATION_MESSAGE, true);
						}

					}
					else
					{ //starting 1 stage was not succesfull
						m_View.removeStatusMsg(msgId);
						m_iStatusPanelMsgIdForwarderServerStatus = m_View.addStatusMsg(JAPMessages.
							getString("controllerStatusMsgRoutingStartServerError"),
							JOptionPane.ERROR_MESSAGE, true);
						JOptionPane.showMessageDialog
							(
								getView(),
								JAPMessages.getString("settingsRoutingStartServerError"),
								JAPMessages.getString("settingsRoutingStartServerErrorTitle"),
								JOptionPane.ERROR_MESSAGE
							);
					}
				}
				else
				{
					// stop the server //
					int msgId = m_View.addStatusMsg(JAPMessages.getString(
						"controllerStatusMsgRoutingStopServer"), JOptionPane.INFORMATION_MESSAGE, false);
					returnValue = JAPModel.getInstance().getRoutingSettings().setRoutingMode(
						JAPRoutingSettings.
						ROUTING_MODE_DISABLED);
					m_View.removeStatusMsg(msgId);
					m_iStatusPanelMsgIdForwarderServerStatus = m_View.addStatusMsg(JAPMessages.getString(
						"controllerStatusMsgRoutingServerStopped"), JOptionPane.INFORMATION_MESSAGE, true);
				}

			}

		} //end of class SetAnonModeAsync
	}

	/**
	 * Enables or disables the forwarding server.
	 * Attention: If there is an active forwarding client running, nothing is done and this method
	 * returns always false. Run a forwarding server and a client at the same time is not supported.
	 * This method returns always immedailly and the real job is done in a background thread.
	 * @param a_activate True, if ther server shall be activated or false, if it shall be disabled.
	 *
	 */
	public synchronized void enableForwardingServer(boolean a_activate)
	{
		new SetForwardingServerModeAsync(a_activate);
	}

}
