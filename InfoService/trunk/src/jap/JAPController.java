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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.text.MessageFormat;
import java.util.Enumeration;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Observable;
import java.util.Observer;
import java.util.Vector;
import java.util.Random;

import java.awt.Component;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import javax.swing.UIManager;
import javax.swing.UIManager.LookAndFeelInfo;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import anon.AnonServerDescription;
import anon.AnonServiceEventAdapter;
import anon.AnonServiceEventListener;
import anon.ErrorCodes;
import anon.crypto.JAPCertificate;
import anon.crypto.SignatureVerifier;
import anon.infoservice.Database;
import anon.infoservice.HTTPConnectionFactory;
import anon.infoservice.IDistributable;
import anon.infoservice.IDistributor;
import anon.infoservice.InfoServiceDBEntry;
import anon.infoservice.InfoServiceHolder;
import anon.infoservice.JAPVersionInfo;
import anon.infoservice.ListenerInterface;
import anon.infoservice.MixCascade;
import anon.infoservice.MixInfo;
import anon.infoservice.ProxyInterface;
import anon.infoservice.SimpleMixCascadeContainer;
import anon.mixminion.MixminionServiceDescription;
import anon.pay.BI;
import anon.pay.IAIEventListener;
import anon.pay.PayAccount;
import anon.pay.PayAccountsFile;
import anon.proxy.AnonProxy;
import anon.proxy.IProxyListener;
import anon.tor.TorAnonServerDescription;
import anon.util.IMiscPasswordReader;
import anon.util.IPasswordReader;
import anon.util.ResourceLoader;
import anon.util.XMLUtil;
import forward.server.ForwardServerManager;
import gui.JAPDll;
import gui.JAPHelp;
import gui.JAPMessages;
import gui.dialog.JAPDialog;
import gui.dialog.JAPDialog.LinkedCheckBox;
import gui.dialog.PasswordContentPane;
import jap.forward.JAPRoutingEstablishForwardedConnectionDialog;
import jap.forward.JAPRoutingMessage;
import jap.forward.JAPRoutingSettings;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import platform.AbstractOS;
import proxy.DirectProxy;
import proxy.DirectProxy.AllowUnprotectedConnectionCallback;
import update.JAPUpdateWizard;
import anon.infoservice.AbstractMixCascadeContainer;

/* This is the Controller of All. It's a Singleton!*/
public final class JAPController extends Observable implements IProxyListener, Observer,
	AnonServiceEventListener, IAIEventListener
{
	/** Messages */
	public static final String MSG_ERROR_SAVING_CONFIG = JAPController.class.getName() +
		"_errorSavingConfig";
	private static final String MSG_DIALOG_ACCOUNT_PASSWORD = JAPController.class.
		getName() + "_dialog_account_password";
	private static final String MSG_ACCOUNT_PASSWORD = JAPController.class.
		getName() + "_account_password";
	private static final String MSG_ENCRYPTACCOUNT = JAPController.class.
		getName() + "_encryptaccount";
	private static final String MSG_ENCRYPTACCOUNTTITLE = JAPController.class.
		getName() + "_encryptaccounttitle";
	private static final String MSG_ACCPASSWORDTITLE = JAPController.class.
		getName() + "_accpasswordtitle";
	private static final String MSG_ACCPASSWORD = JAPController.class.
		getName() + "_accpassword";
	private static final String MSG_ACCPASSWORDENTERTITLE = JAPController.class.
		getName() + "_accpasswordentertitle";
	private static final String MSG_ACCPASSWORDENTER = JAPController.class.
		getName() + "_accpasswordenter";
	private static final String MSG_LOSEACCOUNTDATA = JAPController.class.
		getName() + "_loseaccountdata";
	private static final String MSG_REPEAT_ENTER_ACCOUNT_PASSWORD = JAPController.class.getName() +
		"_repeatEnterAccountPassword";
	private static final String MSG_DISABLE_GOODBYE = JAPController.class.getName() +
		"_disableGoodByMessage";
	private static final String MSG_NEW_OPTIONAL_VERSION = JAPController.class.getName() +
		"_newOptionalVersion";
	private static final String MSG_ALLOWUNPROTECTED = JAPController.class.getName() + "_allowunprotected";
	public static final String MSG_IS_NOT_ALLOWED = JAPController.class.getName() + "_isNotAllowed";


	private static final String XML_ALLOW_NON_ANONYMOUS_CONNECTION = "AllowNonAnonymousConnection";
	private static final String XML_ALLOW_NON_ANONYMOUS_UPDATE = "AllowNonAnonymousUpdate";
	private static final String XML_ATTR_AUTO_CHOOSE_CASCADES = "AutoChooseCascades";

	/**
	 * Stores all MixCascades we know (information comes from infoservice or was entered by a user).
	 * This list may or may not include the current active MixCascade.
	 */
	//private Vector m_vectorMixCascadeDatabase = null;

	private boolean m_bShutdown = false;

	/**
	 * Stores the active MixCascade.
	 */
	private MixCascade m_currentMixCascade = null;

	private ServerSocket m_socketHTTPListener = null; // listener object for HTTP

	private DirectProxy m_proxyDirect = null; // service object for direct access (bypass anon service)
	private AnonProxy m_proxyAnon = null; // service object for anon access

	private InfoServiceUpdater m_InfoServiceUpdater;
	private MixCascadeUpdater m_MixCascadeUpdater;

	private boolean isRunningHTTPListener = false; // true if a HTTP listener is running

	//private boolean  canStartService             = false; // indicates if anon service can be started
	private boolean m_bAlreadyCheckedForNewVersion = false; // indicates if check for new version has already been done
	private boolean m_bAlreadyCheckedForNewDevVersion = false; // have we checked for a new development version?
	private boolean mbActCntMessageNotRemind = false; // indicates if Warning message in setAnonMode has been deactivated for the session
	private boolean mbActCntMessageNeverRemind = false; // indicates if Warning message in setAnonMode has been deactivated forever
	private boolean mbDoNotAbuseReminder = false; // indicates if new warning message in setAnonMode (containing Do no abuse) has been shown
	private boolean m_bForwarderNotExplain = false; //indicates if the warning message about forwarding should be shown
	private boolean m_bPayCascadeNoAsk = false;

	public String status1 = " ";
	public String status2 = " ";

	private long m_nrOfBytesWWW = 0;
	private long m_nrOfBytesOther = 0;

	private static IJAPMainView m_View = null;
	private static JAPController m_Controller = null;
	private static JAPModel m_Model = null;
	private static JAPFeedback m_feedback = null;
	private Locale m_Locale = null;
	private Vector observerVector = null;
	private Vector m_anonServiceListener;
	private IPasswordReader m_passwordReader;
	private boolean m_bInitialRun = true;
	private Object m_inititalRunSync = new Object();
	private Object m_finishSync = new Object();

	private static Font m_fontControls;
	/** Holds the MsgID of the status message after the forwaring server was started.*/
	private int m_iStatusPanelMsgIdForwarderServerStatus;

	/**
	 * Stores the jobs, if we receive new setAnonMode() requests.
	 */
	private Vector m_changeAnonModeJobs;

	private JAPController()
	{
		// simulate database distributor
		Database.registerDistributor(new IDistributor()
		{
			public void addJob(IDistributable a_distributable)
			{
			}
		});

		// initialise IS update threads
		m_feedback = new JAPFeedback();
		m_InfoServiceUpdater = new InfoServiceUpdater();
		m_MixCascadeUpdater = new MixCascadeUpdater();

		m_changeAnonModeJobs = new Vector();
		m_Model = JAPModel.getInstance();
		m_Model.setAnonConnectionChecker(new AnonConnectionChecker());
		InfoServiceDBEntry.setMutableProxyInterface(m_Model.getInfoServiceProxyInterface());

		// Create observer object
		observerVector = new Vector();
		// create service listener object
		m_anonServiceListener = new Vector();

		// initialise HTTP proxy
		DirectProxy.setAllowUnprotectedConnectionCallback(
			new DirectProxy.AllowUnprotectedConnectionCallback()
		{
			public DirectProxy.AllowUnprotectedConnectionCallback.Answer callback()
			{
				if (JAPController.getView() == null)
				{
					return new Answer(false, false);
				}

				boolean bShowHtmlWarning;
				JAPDll.setWindowOnTop(JAPController.getView(), true);
				JAPDialog.LinkedCheckBox cb = new JAPDialog.LinkedCheckBox(
					JAPMessages.getString(JAPDialog.LinkedCheckBox.MSG_REMEMBER_ANSWER), false);
				bShowHtmlWarning = ! (JAPDialog.showYesNoDialog(JAPController.getView(),
					JAPMessages.getString(MSG_ALLOWUNPROTECTED), cb));
				JAPDll.setWindowOnTop(JAPController.getView(), false);
				return new Answer(!bShowHtmlWarning, cb.getState());
			}
		});

		/* set a default mixcascade */
		try
		{
			Vector listeners = new Vector();
			for (int j = 0; j < JAPConstants.DEFAULT_ANON_HOSTS.length; j++)
			{
				for (int i = 0; i < JAPConstants.DEFAULT_ANON_PORT_NUMBERS.length; i++)
				{
					listeners.addElement(new ListenerInterface(JAPConstants.DEFAULT_ANON_HOSTS[j],
						JAPConstants.DEFAULT_ANON_PORT_NUMBERS[i],
						ListenerInterface.PROTOCOL_TYPE_RAW_TCP));
				}
			}
			m_currentMixCascade = new MixCascade(JAPConstants.DEFAULT_ANON_NAME,
												 JAPConstants.DEFAULT_ANON_ID,
												 listeners);
			m_currentMixCascade.setUserDefined(false);
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.EMERG, LogType.NET,
						  "JAPController: Constructor - default mix cascade: " + e.getMessage());
			LogHolder.log(LogLevel.EMERG, LogType.NET, e);
		}
		/* set a default infoservice */
		try
		{
			InfoServiceDBEntry defaultInfoService = JAPController.createDefaultInfoService();
			InfoServiceHolder.getInstance().setPreferredInfoService(defaultInfoService);
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.EMERG, LogType.NET,
						  "JAPController: Constructor - default info service: " + e.getMessage());
		}
		/* set some default values for infoservice communication */
		setInfoServiceDisabled(JAPConstants.DEFAULT_INFOSERVICE_DISABLED);
		InfoServiceHolder.getInstance().setChangeInfoServices(JAPConstants.DEFAULT_INFOSERVICE_CHANGES);

		addDefaultCertificates();
		SignatureVerifier.getInstance().setCheckSignatures(JAPConstants.DEFAULT_CERT_CHECK_ENABLED);

		HTTPConnectionFactory.getInstance().setTimeout(JAPConstants.DEFAULT_INFOSERVICE_TIMEOUT);

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

	public class AnonConnectionChecker
	{
		public boolean checkAnonConnected()
		{
			return isAnonConnected();
		}
}

	/**
	 * Returns the password reader.
	 * @return the password reader
	 */
	public IPasswordReader getPasswordReader()
	{
		return m_passwordReader;
	}

	public void chooseRandomMixCascade()
	{
		if (JAPModel.getInstance().isCascadeConnectionChosenAutomatically())
		{
			AutoSwitchedMixCascadeContainer cascadeSwitcher = new AutoSwitchedMixCascadeContainer();
			cascadeSwitcher.getNextMixCascade(); // this is the default cascade
			setCurrentMixCascade(cascadeSwitcher.getNextMixCascade());
		}
	}

	//---------------------------------------------------------------------
	public void initialRun()
	{
		m_bInitialRun = true;
		LogHolder.log(LogLevel.INFO, LogType.MISC, "JAPModel:initial run of JAP...");

		// start update threads
		m_feedback.start();
		m_InfoServiceUpdater.start();
		m_MixCascadeUpdater.start();

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
			JAPDialog.showErrorDialog(getView(), msg, LogType.NET);
			m_Controller.status1 = JAPMessages.getString("statusCannotStartListener");
			m_View.disableSetAnonMode();
			notifyJAPObservers();
		}
		else
		{ // listender has started correctly
			m_Controller.status1 = JAPMessages.getString("statusRunning");
			// initial setting of anonMode
			if (JAPModel.getAutoConnect() &&
				JAPModel.getInstance().getRoutingSettings().isConnectViaForwarder())
			{
				/* show the connect via forwarder dialog -> the dialog will do the remaining things */
				new JAPRoutingEstablishForwardedConnectionDialog(getView(), getDialogFont());
				notifyObservers();
			}
			else
			{
				setAnonMode(JAPModel.getAutoConnect());
			}
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
	 *    version="0.22"                     // version of the xml struct (DTD) used for saving the configuration
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
	 *    infoServiceDisabled="true/false"  // disable use of InfoService
	 *    infoServiceTimeout="..."          // timeout (sec) for infoservice and update communication (since config version 0.5)
	 *    autoConnect="true"/"false"    // should we start the anon service immedialy after programm launch ?
	 *    autoReConnect="true"/"false"    // should we automatically reconnect to mix if connection was lost ?
	 *    DummyTrafficIntervall=".."    //Time of inactivity in milli seconds after which a dummy is send
	 *    minimizedStartup="true"/"false" // should we start minimized ???
	 *    neverRemindActiveContent="true"/"false" // should we remind the user about active content ?
	 *    neverAskPayment="true"/"false" // should we remind the user about payment for cascades ?
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
	 *    <Detail>..</Detail>          // the detail level of the log output, sinver version 0.21
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
	 * <InfoServiceManagement>                                    // since config version 0.19
	 *   <InfoServices>                                           // info about all known infoservices
	 *   <InfoService id="...">...</InfoService>                // the same format as from infoservice, without signature, if expired, it is removed from infoservice list
	 *   <InfoService id="...">...</InfoService>
	 * </InfoServices>
	 *   <PreferredInfoService>                                   // info about the preferred infoservice, only one infoservice is supported here
	 *   <InfoService id="...">...</InfoService>                // the same format as from infoservice, without signature, expire time does not matter
	 * </PreferedInfoService>
	 *   <ChangeInfoServices>true<ChangeInfoServices>             // whether it is tried to change the infoservice automatically after failure
	 * </InfoServiceManagement>
	 * <Tor>    //  Tor related seetings (since Version 0.6)
	 * 	 <MaxConnectionsPerRoute>...</MaxConnectionsPerRoute>(since Vresion 0.8) //How many connections are allowed before a new circuit is created
	 * 	 <RouteLen min=" " max=" "/>(since Vresion 0.9) //How long should a route be
	 *   <PreCreateAnonRoutes>True/False</PreCreateAnonRoutes> //Should the routes be created in advance?
	 * </Tor>
	 * <Mixminion>    //  Mixminion related seetings (since Version 0.22)
	 * 	 <RouteLen>...</RouteLen> //How long should a route be
	 * </Mixminion>
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
	public synchronized void loadConfigFile(String a_strJapConfFile, boolean loadPay, final JAPSplash a_splash)
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
			JAPModel.getInstance().setConfigFile(AbstractOS.getInstance().getConfigPath() +
												 JAPConstants.XMLCONFFN);
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
				JAPModel.getInstance().setConfigFile(AbstractOS.getInstance().getConfigPath() +
					JAPConstants.XMLCONFFN);
			}
		}
		Document doc = null;

		if (success)
		{
			try
			{
				doc = XMLUtil.readXMLDocument(new File(JAPModel.getInstance().getConfigFile()));
			}
			catch (Exception a_e)
			{
				LogHolder.log(LogLevel.NOTICE, LogType.MISC, "Error while loading the configuration file!");
			}
		}
		if (doc == null)
		{
			doc = XMLUtil.createDocument();
		}


		//if (success)
		{
			try
			{
				Element root = doc.getDocumentElement();
				XMLUtil.removeComments(root);

				//
				setDefaultView(JAPConstants.VIEW_NORMAL);

				String strVersion = XMLUtil.parseAttribute(root, JAPConstants.CONFIG_VERSION, null);
	            m_Model.setDLLupdate(XMLUtil.parseAttribute(root, m_Model.DLL_VERSION_UPDATE, false));

				JAPModel.getInstance().allowUpdateViaDirectConnection(
								XMLUtil.parseAttribute(root, XML_ALLOW_NON_ANONYMOUS_UPDATE,
								JAPConstants.DEFAULT_ALLOW_UPDATE_NON_ANONYMOUS_CONNECTION));
				JAPModel.getInstance().setReminderForOptionalUpdate(
								XMLUtil.parseAttribute(root, JAPModel.XML_REMIND_OPTIONAL_UPDATE,
								JAPConstants.REMIND_OPTIONAL_UPDATE));
				JAPModel.getInstance().setChooseCascadeConnectionAutomatically(
								XMLUtil.parseAttribute(root, XML_ATTR_AUTO_CHOOSE_CASCADES, true));

	            int port = XMLUtil.parseAttribute(root, JAPConstants.CONFIG_PORT_NUMBER,
												  JAPModel.getHttpListenerPortNumber());
				boolean bListenerIsLocal = XMLUtil.parseAttribute(root,
					JAPConstants.CONFIG_LISTENER_IS_LOCAL, true);
				setHTTPListener(port, bListenerIsLocal, false);

				//port = XMLUtil.parseAttribute(root, "portNumberSocks",
				//  JAPModel.getSocksListenerPortNumber());
				//setSocksPortNumber(port);
				//setUseSocksPort(JAPUtil.parseNodeBoolean(n.getNamedItem("supportSocks"),false));
				//setUseProxy(JAPUtil.parseNodeBoolean(n.getNamedItem("proxyMode"),false));
				// load settings for the reminder message in setAnonMode
				try
				{
					mbActCntMessageNeverRemind = XMLUtil.parseAttribute(root,
						JAPConstants.CONFIG_NEVER_REMIND_ACTIVE_CONTENT, false);
					mbDoNotAbuseReminder =
						XMLUtil.parseAttribute(root, JAPConstants.CONFIG_DO_NOT_ABUSE_REMINDER, false);
					if (mbActCntMessageNeverRemind && mbDoNotAbuseReminder)
					{
						mbActCntMessageNotRemind = true;

					}
					// load settings for the reminder message before goodBye
					m_Model.setNeverRemindGoodbye(
						XMLUtil.parseAttribute(root, JAPConstants.CONFIG_NEVER_REMIND_GOODBYE,
											   !JAPConstants.DEFAULT_WARN_ON_CLOSE));
					m_bForwarderNotExplain =
						XMLUtil.parseAttribute(root, JAPConstants.CONFIG_NEVER_EXPLAIN_FORWARD, false);
					m_bPayCascadeNoAsk =
						XMLUtil.parseAttribute(root, JAPConstants.CONFIG_NEVER_ASK_PAYMENT, false);

				}
				catch (Exception ex)
				{
					LogHolder.log(LogLevel.INFO, LogType.MISC,
								  "Error loading reminder message ins setAnonMode.");
				}
				/* infoservice configuration options */
				boolean b = XMLUtil.parseAttribute(root, JAPConstants.CONFIG_INFOSERVICE_DISABLED,
					JAPModel.isInfoServiceDisabled());
				setInfoServiceDisabled(b);
				int i = XMLUtil.parseAttribute(root, JAPConstants.CONFIG_INFOSERVICE_TIMEOUT, -1);
				try
				{ //i = 5; /** @todo temp */
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
					String proxyType = XMLUtil.parseAttribute(root, JAPConstants.CONFIG_PROXY_TYPE,
															  ListenerInterface.PROTOCOL_STR_TYPE_HTTP);
					if (proxyType.equalsIgnoreCase("HTTP"))
					{
						proxyType = ListenerInterface.PROTOCOL_STR_TYPE_HTTP;
					}
					else if (proxyType.equalsIgnoreCase("SOCKS"))
					{
						proxyType = ListenerInterface.PROTOCOL_STR_TYPE_SOCKS;
					}
					JAPModel.getInstance().setUseProxyAuthentication(
					   XMLUtil.parseAttribute(root, JAPConstants.CONFIG_PROXY_AUTHORIZATION, false));
					proxyInterface = new ProxyInterface(
						XMLUtil.parseAttribute(root, JAPConstants.CONFIG_PROXY_HOST_NAME, null),
						XMLUtil.parseAttribute(root, JAPConstants.CONFIG_PROXY_PORT_NUMBER, -1),
						proxyType,
						XMLUtil.parseAttribute(root, JAPConstants.CONFIG_PROXY_AUTH_USER_ID, null),
						getPasswordReader(),
						JAPModel.getInstance().isProxyAuthenticationUsed(),
						XMLUtil.parseAttribute(root, JAPConstants.CONFIG_PROXY_MODE, false));
				}
				catch (Exception a_e)
				{
					LogHolder.log(LogLevel.NOTICE, LogType.NET,
								  "JAPController: could not load proxy settings: " + a_e);
				}

				// check if something has changed
				changeProxyInterface(proxyInterface,
									 XMLUtil.parseAttribute(
					root,JAPConstants.CONFIG_PROXY_AUTHORIZATION, false));

				setDummyTraffic(XMLUtil.parseAttribute(root, JAPConstants.CONFIG_DUMMY_TRAFFIC_INTERVALL,
					-1));
				setAutoConnect(XMLUtil.parseAttribute(root, JAPConstants.CONFIG_AUTO_CONNECT, false));
				setAutoReConnect(XMLUtil.parseAttribute(root, JAPConstants.CONFIG_AUTO_RECONNECT, true));
				m_Model.setMinimizeOnStartup(
					XMLUtil.parseAttribute(root, JAPConstants.CONFIG_MINIMIZED_STARTUP, false));


				/* try to get the info from the MixCascade node */
				Element mixCascadeNode = (Element) XMLUtil.getFirstChildByName(root,
					MixCascade.XML_ELEMENT_NAME);
				try
				{
					m_currentMixCascade = new MixCascade( (Element) mixCascadeNode,  Long.MAX_VALUE);
				}
				catch (Exception e)
				{
					/* take the current mixcascade as the default */
					m_currentMixCascade = getCurrentMixCascade();
				}
				Database.getInstance(MixCascade.class).update(getCurrentMixCascade());

				/* try to load information about user defined cascades */
				Node nodeCascades = XMLUtil.getFirstChildByName(root, MixCascade.XML_ELEMENT_CONTAINER_NAME);
				if (nodeCascades != null)
				{
					Node nodeCascade = nodeCascades.getFirstChild();
					while (nodeCascade != null)
					{
						if (nodeCascade.getNodeName().equals(MixCascade.XML_ELEMENT_NAME))
						{
							try
							{
								Database.getInstance(MixCascade.class).update(
									new MixCascade((Element)nodeCascade, Long.MAX_VALUE));
							}
							catch (Exception e)
							{}
						}
						nodeCascade = nodeCascade.getNextSibling();
					}
				}

				/* try to load information about user defined mixes */
				Node nodeMixes = XMLUtil.getFirstChildByName(root, MixInfo.XML_ELEMENT_CONTAINER_NAME);
				if (nodeMixes != null)
				{
					Node nodeMix = nodeMixes.getFirstChild();
					while (nodeMix != null)
					{
						if (nodeMix.getNodeName().equals(MixInfo.XML_ELEMENT_NAME))
						{
							try
							{
								Database.getInstance(MixInfo.class).update(
									new MixInfo((Element)nodeMix, Long.MAX_VALUE));
							}
							catch (Exception e)
							{
								try
								{
									Database.getInstance(MixInfo.class).update(
										new MixInfo( (Element) nodeMix, Long.MAX_VALUE, true));
								}
								catch (Exception a_e)
								{
									LogHolder.log(LogLevel.ERR, LogType.MISC,
												  "Illegal MixInfo object in configuration.");
								}
							}
						}
						nodeMix = nodeMix.getNextSibling();
					}
				}

				//Load Locale-Settings
				String strLocale =
					XMLUtil.parseAttribute(root, JAPConstants.CONFIG_LOCALE,	m_Locale.getLanguage());
				Locale locale = new Locale(strLocale, "");
				setLocale(locale);
				//Load look-and-feel settings (not changed if SmmallDisplay!
				if (!JAPModel.isSmallDisplay() && !JAPDialog.isConsoleOnly())
				{
					String lf = XMLUtil.parseAttribute(
									   root, JAPConstants.CONFIG_LOOK_AND_FEEL, JAPConstants.CONFIG_UNKNOWN);
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
								m_Model.setOldMainWindowLocation(p);
								m_Model.setOldMainWindowSize(d);
							}
							tmp = (Element) XMLUtil.getFirstChildByName(elemMainWindow,
								JAPConstants.CONFIG_MOVE_TO_SYSTRAY);
							b = XMLUtil.parseValue(tmp, false);
							setMoveToSystrayOnStartup(b);
							/*if (b)
									{ ///todo: move to systray
							 if (m_View != null)
							 {
							  b=m_View.hideWindowInTaskbar();
							 }
									}
									if(!b)
									{
							 m_View.setVisible(true);
							   m_View.toFront();


									}*/
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
						JAPDebug.getInstance().setLogLevel(XMLUtil.parseValue(
							elemLevel, JAPDebug.getInstance().getLogLevel()));

						Element elemLogDetail = (Element) XMLUtil.getFirstChildByName(elemDebug,
							JAPConstants.CONFIG_LOG_DETAIL);
						LogHolder.setDetailLevel(
							XMLUtil.parseValue(elemLogDetail, LogHolder.getDetailLevel()));

						Element elemType = (Element) XMLUtil.getFirstChildByName(elemDebug,
							JAPConstants.CONFIG_TYPE);
						if (elemType != null)
						{
							int debugtype = LogType.NUL;
							int[] logTypes = LogType.getAvailableLogTypes();
							for (int j = 0; j < logTypes.length; j++)
							{
								if (XMLUtil.parseAttribute(elemType, LogType.getLogTypeName(logTypes[j]), false))
								{
									debugtype |= logTypes[j];
								}
							}
							JAPDebug.getInstance().setLogType(debugtype);
						}
						Node elemOutput = XMLUtil.getFirstChildByName(elemDebug, JAPConstants.CONFIG_OUTPUT);
						if (elemOutput != null)
						{
							String strConsole = XMLUtil.parseValue(elemOutput, "");
							if (strConsole != null && getView() != null)
							{
								strConsole.trim();
								JAPDebug.showConsole(strConsole.equalsIgnoreCase(JAPConstants.CONFIG_CONSOLE),
									getView());
							}
							Node elemFile = XMLUtil.getLastChildByName(elemOutput, JAPConstants.CONFIG_FILE);
							JAPDebug.setLogToFile(XMLUtil.parseValue(elemFile, null));
						}
					}
					catch (Exception ex)
					{
						LogHolder.log(LogLevel.INFO, LogType.MISC,
									  " Error loading Debug Settings.");
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
				/* As some old Versions of JAP have problems with writing the correct values, we include the
				 * default certs if config version is less than 0.20 */
				if (strVersion == null || strVersion.compareTo("0.20") < 0)
				{
					addDefaultCertificates();
				}

				/* load the infoservice management settings */
				try
				{
					Element infoserviceManagementNode = (Element) (XMLUtil.getFirstChildByName(root,
						InfoServiceHolder.getXmlSettingsRootNodeName()));
					JAPModel.getInstance().allowInfoServiceViaDirectConnection(
					   XMLUtil.parseAttribute(infoserviceManagementNode,
											  XML_ALLOW_NON_ANONYMOUS_CONNECTION,
											  JAPConstants.DEFAULT_ALLOW_INFOSERVICE_NON_ANONYMOUS_CONNECTION));
					if (infoserviceManagementNode != null)
					{
						InfoServiceHolder.getInstance().loadSettingsFromXml(infoserviceManagementNode);
					}
					else
					{
						throw (new Exception("No InfoServiceManagement node found. Using default settings for infoservice management in InfoServiceHolder."));
					}
				}
				catch (Exception e)
				{
					LogHolder.log(LogLevel.ERR, LogType.MISC, e);
				}

				/* load Payment settings */
				try
				{
					if (loadPay)
					{
						Element elemPay = (Element) XMLUtil.getFirstChildByName(root,
							JAPConstants.CONFIG_PAYMENT);
						JAPModel.getInstance().allowPaymentViaDirectConnection(
											  XMLUtil.parseAttribute(elemPay, XML_ALLOW_NON_ANONYMOUS_CONNECTION,
							JAPConstants.DEFAULT_ALLOW_PAYMENT_NON_ANONYMOUS_CONNECTION));

						Element elemAccounts = (Element) XMLUtil.getFirstChildByName(elemPay,
							JAPConstants.CONFIG_PAY_ACCOUNTS_FILE);

						//Load known Payment instances
						Node nodePIs = XMLUtil.getFirstChildByName(elemPay,
							JAPConstants.CONFIG_PAYMENT_INSTANCES);
						if (nodePIs != null)
						{
							Node nodePI = nodePIs.getFirstChild();
							while (nodePI != null)
							{
								PayAccountsFile.getInstance().addKnownPI( (Element) nodePI);
								nodePI = nodePI.getNextSibling();
							}
						}
						/** @todo implement password reader for console */
						IMiscPasswordReader passwordReader;
						final Hashtable cachedPasswords = new Hashtable();
						JAPDialog tempDialog = null;

						if (JAPDialog.isConsoleOnly())
						{
							passwordReader = new IMiscPasswordReader()
							{
								public String readPassword(Object a_message)
								{
									return null;
								}
							};
						}
						else
						{
							final JAPDialog dialog = new JAPDialog(a_splash,
								JAPMessages.getString(MSG_ACCPASSWORDENTERTITLE), true);
							tempDialog = dialog;
							dialog.setDefaultCloseOperation(JAPDialog.HIDE_ON_CLOSE);
							PasswordContentPane temp = new PasswordContentPane(
								dialog, PasswordContentPane.PASSWORD_ENTER,
								JAPMessages.getString(
									MSG_ACCPASSWORDENTER, new Long(Long.MAX_VALUE)));
							temp.updateDialog();
							dialog.pack();

							passwordReader = new IMiscPasswordReader()
							{
								private Vector passwordsToTry = new Vector();

								public String readPassword(Object a_message)
								{
									a_splash.dispose();

									PasswordContentPane panePassword;
									String password;
									panePassword = new PasswordContentPane(
										dialog, PasswordContentPane.PASSWORD_ENTER,
										JAPMessages.getString(MSG_ACCPASSWORDENTER, a_message));
									panePassword.setDefaultButtonOperation(PasswordContentPane.
																		   ON_CLICK_HIDE_DIALOG);
									if (passwordsToTry == null)
									{
										return null;
									}

									if (cachedPasswords.containsKey(a_message) || cachedPasswords.size() == 0)
									{
										while (true)
										{
											password = panePassword.readPassword(null);
											if (password == null)
											{
												if (JAPDialog.showYesNoDialog(
													dialog, JAPMessages.getString(MSG_LOSEACCOUNTDATA)))
												{
													// user clicked cancel
													passwordsToTry = null;
													// do not use the password from this account
													cachedPasswords.remove(a_message);
													break;
												}
												else
												{
													continue;
												}
											}
											else
											{
												break;
											}
										}
										if (password != null)
										{
											cachedPasswords.put(a_message, password);
										}
									}
									else
									{
										if (passwordsToTry.size() == 0)
										{
											Enumeration enumCachedPasswords = cachedPasswords.elements();
											while (enumCachedPasswords.hasMoreElements())
											{
												passwordsToTry.addElement(enumCachedPasswords.nextElement());
											}
										}
										password = (String) passwordsToTry.elementAt(passwordsToTry.size() -
											1);
										passwordsToTry.removeElementAt(passwordsToTry.size() - 1);

										if (passwordsToTry.size() == 0)
										{
											cachedPasswords.put(a_message, password);
										}
									}

									return password;
								}
							};
						}
						PayAccountsFile.init(elemAccounts, passwordReader);
						if (tempDialog != null)
						{
							tempDialog.dispose();
						}
						if (cachedPasswords.size() > 0)
						{
							// choose any password from the working ones
							setPaymentPassword((String)cachedPasswords.elements().nextElement());
						}
					}
				}
				catch (Exception e)
				{
					LogHolder.log(LogLevel.INFO, LogType.MISC, "Error loading Payment configuration.");
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
					elem = (Element) XMLUtil.getFirstChildByName(elemTor,
						JAPConstants.CONFIG_TOR_PRECREATE_ANON_ROUTES);
					setPreCreateAnonRoutes(XMLUtil.parseValue(elem, JAPModel.isPreCreateAnonRoutesEnabled()));

				}
				catch (Exception ex)
				{
					LogHolder.log(LogLevel.INFO, LogType.MISC,
								  "JAPController: loadConfigFile: Error loading Tor configuration.");
				}

				/*loading Mixminion settings*/
				try
				{
					Element elemMixminion = (Element) XMLUtil.getFirstChildByName(root,
						JAPConstants.CONFIG_Mixminion);
					Element elemMM = (Element) XMLUtil.getFirstChildByName(elemMixminion,
						JAPConstants.CONFIG_ROUTE_LEN);
					int routeLen = XMLUtil.parseValue(elemMM, JAPModel.getMixminionRouteLen());
					setMixminionRouteLen(routeLen);
				}
				catch (Exception ex)
				{
					LogHolder.log(LogLevel.INFO, LogType.MISC,
								  "JAPController: loadConfigFile: Error loading Mixminion configuration.");
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
			try
			{
				f.close();
			}
			catch (Exception e1)
			{}
			return true;
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.ERR, LogType.MISC,
						  "Configuration file \"" + a_configFile +  "\" not found.");
			return false;
		}
	}

	/**
	 * Tries to load a config file in OS-depended locations
	 * @return boolean
	 */
	private boolean loadConfigFileOSdependent()
	{
		String japConfFile = AbstractOS.getInstance().getConfigPath() + JAPConstants.XMLCONFFN;
		LogHolder.log(LogLevel.INFO, LogType.MISC,
					  "Trying to load configuration from: " + japConfFile);
		try
		{
			FileInputStream f = new FileInputStream(japConfFile);
			/* if we are successful, use this config file also for storing the configuration */
			JAPModel.getInstance().setConfigFile(japConfFile);
			try
			{
				f.close();
			}
			catch (Exception e1)
			{}
			return true;
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.ERR, LogType.MISC,
						  "JAPController: loadConfigFileOSdependent: Configuration file \"" + japConfFile +
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
		String japConfFile = System.getProperty("user.home", "") + File.separator + JAPConstants.XMLCONFFN;
		LogHolder.log(LogLevel.INFO, LogType.MISC,
					  "JAPController: loadConfigFile: Trying to load configuration from: " + japConfFile);
		try
		{
			FileInputStream f = new FileInputStream(japConfFile);
			/* if we are successful, use this config file also for storing the configuration */
			JAPModel.getInstance().setConfigFile(japConfFile);
			try
			{
				f.close();
			}
			catch (Exception e1)
			{}
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
			try
			{
				f.close();
			}
			catch (Exception e1)
			{}
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
	 * @param a_bUseAuth indicates whether porxy authentication should be used
	 */
	public synchronized void changeProxyInterface(ProxyInterface a_proxyInterface, boolean a_bUseAuth)
	{
		if (a_proxyInterface != null &&
			(m_Model.getProxyInterface() == null ||
			 !m_Model.getProxyInterface().equals(a_proxyInterface)))
		{
			// change settings
			m_Model.setProxyListener(a_proxyInterface);

			applyProxySettingsToInfoService(a_bUseAuth);
			applyProxySettingsToAnonService();

			notifyJAPObservers();
		}
	}

	public boolean saveConfigFile()
	{
		boolean error = false;
		LogHolder.log(LogLevel.INFO, LogType.MISC, "Try saving configuration.");
		try
		{
			String sb = getConfigurationAsXmlString();
			if (sb == null)
			{
				LogHolder.log(LogLevel.ERR, LogType.MISC,
							  "Could not transform the configuration to a string.");
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
		// Achtung!! Fehler im Sun-XML --> NULL-Attribute koennen hinzugefuegt werden,
		// beim Abspeichern gibt es dann aber einen Fehler!
		try
		{
			Document doc = XMLUtil.createDocument();
			Element e = doc.createElement("JAP");
			doc.appendChild(e);
			XMLUtil.setAttribute(e, JAPConstants.CONFIG_VERSION, "0.23");
			XMLUtil.setAttribute(e, m_Model.DLL_VERSION_UPDATE, m_Model.getDLLupdate());

			XMLUtil.setAttribute(e, XML_ALLOW_NON_ANONYMOUS_UPDATE,
								 JAPModel.getInstance().isUpdateViaDirectConnectionAllowed());
			XMLUtil.setAttribute(e, JAPModel.XML_REMIND_OPTIONAL_UPDATE,
								 JAPModel.getInstance().isReminderForOptionalUpdateActivated());
			XMLUtil.setAttribute(e, XML_ATTR_AUTO_CHOOSE_CASCADES,
								 JAPModel.getInstance().isCascadeConnectionChosenAutomatically());

			/* save payment configuration */
			try
			{
				PayAccountsFile accounts = PayAccountsFile.getInstance();
				if (accounts != null)
				{
					Element elemPayment = doc.createElement(JAPConstants.CONFIG_PAYMENT);
					XMLUtil.setAttribute(elemPayment, XML_ALLOW_NON_ANONYMOUS_CONNECTION,
										 JAPModel.getInstance().isPaymentViaDirectConnectionAllowed());
					e.appendChild(elemPayment);

					//Save the known PIs
					Element elemPIs = doc.createElement(JAPConstants.CONFIG_PAYMENT_INSTANCES);
					elemPayment.appendChild(elemPIs);
					Enumeration pis = accounts.getKnownPIs();

					while (pis.hasMoreElements())
					{
						elemPIs.appendChild( ( (BI) pis.nextElement()).toXmlElement(doc));
					}
					elemPayment.appendChild(accounts.toXmlElement(doc, getPaymentPassword()));
				}
			}
			catch (Exception ex)
			{
				LogHolder.log(LogLevel.EXCEPTION, LogType.MISC, "Error saving payment configuration", ex);
				return null;
			}

			//
			XMLUtil.setAttribute(e, JAPConstants.CONFIG_PORT_NUMBER, JAPModel.getHttpListenerPortNumber());
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
									 m_Model.getProxyInterface().getPort());
				XMLUtil.setAttribute(e, JAPConstants.CONFIG_PROXY_AUTHORIZATION,
									 m_Model.getProxyInterface().isAuthenticationUsed());
				XMLUtil.setAttribute(e, JAPConstants.CONFIG_PROXY_AUTH_USER_ID,
									 m_Model.getProxyInterface().getAuthenticationUserID());
			}
			/* infoservice configuration options */
			XMLUtil.setAttribute(e, JAPConstants.CONFIG_INFOSERVICE_DISABLED, JAPModel.isInfoServiceDisabled());
			XMLUtil.setAttribute(e, JAPConstants.CONFIG_INFOSERVICE_TIMEOUT,
								 HTTPConnectionFactory.getInstance().getTimeout());

			XMLUtil.setAttribute(e, JAPConstants.CONFIG_DUMMY_TRAFFIC_INTERVALL,
								 JAPModel.getDummyTraffic());
			XMLUtil.setAttribute(e, JAPConstants.CONFIG_AUTO_CONNECT, JAPModel.getAutoConnect());
			XMLUtil.setAttribute(e, JAPConstants.CONFIG_AUTO_RECONNECT, JAPModel.getAutoReConnect());
			XMLUtil.setAttribute(e, JAPConstants.CONFIG_MINIMIZED_STARTUP, JAPModel.getMinimizeOnStartup());
			XMLUtil.setAttribute(e, JAPConstants.CONFIG_NEVER_REMIND_ACTIVE_CONTENT,
								 mbActCntMessageNeverRemind);
			XMLUtil.setAttribute(e, JAPConstants.CONFIG_NEVER_EXPLAIN_FORWARD, m_bForwarderNotExplain);
			XMLUtil.setAttribute(e, JAPConstants.CONFIG_NEVER_ASK_PAYMENT, m_bPayCascadeNoAsk);
			XMLUtil.setAttribute(e, JAPConstants.CONFIG_DO_NOT_ABUSE_REMINDER, mbDoNotAbuseReminder);
			XMLUtil.setAttribute(e, JAPConstants.CONFIG_NEVER_REMIND_GOODBYE,
								 JAPModel.getInstance().isNeverRemindGoodbye());
			XMLUtil.setAttribute(e, JAPConstants.CONFIG_LOCALE, m_Locale.getLanguage());
			XMLUtil.setAttribute(e, JAPConstants.CONFIG_LOOK_AND_FEEL,
								 UIManager.getLookAndFeel().getClass().getName());

			/*stores MixCascades*/
			Element elemCascades = doc.createElement(MixCascade.XML_ELEMENT_CONTAINER_NAME);
			e.appendChild(elemCascades);
			Enumeration enumer = Database.getInstance(MixCascade.class).getEntrySnapshotAsEnumeration();
			while (enumer.hasMoreElements())
			{
				elemCascades.appendChild(((MixCascade) enumer.nextElement()).toXmlElement(doc));
			}
			/*stores mixes */
			Element elemMixes = doc.createElement(MixInfo.XML_ELEMENT_CONTAINER_NAME);
			e.appendChild(elemMixes);
			Enumeration enumerMixes = Database.getInstance(MixInfo.class).getEntrySnapshotAsEnumeration();
			while (enumerMixes.hasMoreElements())
			{
					elemMixes.appendChild(((MixInfo) enumerMixes.nextElement()).toXmlElement(doc));
			}
			/* store the current MixCascade */
			MixCascade defaultMixCascade = getCurrentMixCascade();
			if (defaultMixCascade != null)
			{
				Element elem = defaultMixCascade.toXmlElement(doc);
				e.appendChild(elem);
			}

			// adding GUI-Element
			Element elemGUI = doc.createElement(JAPConstants.CONFIG_GUI);
			e.appendChild(elemGUI);
			Element elemMainWindow = doc.createElement(JAPConstants.CONFIG_MAIN_WINDOW);
			elemGUI.appendChild(elemMainWindow);
			if (JAPModel.getSaveMainWindowPosition() && getView() != null)
			{
				Element tmp = doc.createElement(JAPConstants.CONFIG_SET_ON_STARTUP);
				elemMainWindow.appendChild(tmp);
				XMLUtil.setValue(tmp, true);
				tmp = doc.createElement(JAPConstants.CONFIG_LOCATION);
				elemMainWindow.appendChild(tmp);
				Point p = getView().getLocation();
				tmp.setAttribute(JAPConstants.CONFIG_X, Integer.toString(p.x));
				tmp.setAttribute(JAPConstants.CONFIG_Y, Integer.toString(p.y));
				tmp = doc.createElement(JAPConstants.CONFIG_SIZE);
				elemMainWindow.appendChild(tmp);
				Dimension d = getView().getSize();
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
			tmp = doc.createElement(JAPConstants.CONFIG_LOG_DETAIL);
			XMLUtil.setValue(tmp, LogHolder.getDetailLevel());
			elemDebug.appendChild(tmp);
			tmp = doc.createElement(JAPConstants.CONFIG_TYPE);
			int debugtype = JAPDebug.getInstance().getLogType();
			int[] availableLogTypes = LogType.getAvailableLogTypes();
			for (int i = 1; i < availableLogTypes.length; i++)
			{
				XMLUtil.setAttribute(tmp, LogType.getLogTypeName(availableLogTypes[i]),
									 ( (debugtype & availableLogTypes[i]) != 0));
			}

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
			Element elemIS = InfoServiceHolder.getInstance().getSettingsAsXml(doc);
			XMLUtil.setAttribute(elemIS, XML_ALLOW_NON_ANONYMOUS_CONNECTION,
								 JAPModel.getInstance().isInfoServiceViaDirectConnectionAllowed());
			e.appendChild(elemIS);


			/** add tor*/
			Element elemTor = doc.createElement(JAPConstants.CONFIG_TOR);
			Element elem = doc.createElement(JAPConstants.CONFIG_MAX_CONNECTIONS_PER_ROUTE);
			XMLUtil.setValue(elem, JAPModel.getTorMaxConnectionsPerRoute());
			elemTor.appendChild(elem);
			elem = doc.createElement(JAPConstants.CONFIG_ROUTE_LEN);
			XMLUtil.setAttribute(elem, JAPConstants.CONFIG_MIN, JAPModel.getTorMinRouteLen());
			XMLUtil.setAttribute(elem, JAPConstants.CONFIG_MAX, JAPModel.getTorMaxRouteLen());
			elemTor.appendChild(elem);
			elem = doc.createElement(JAPConstants.CONFIG_TOR_PRECREATE_ANON_ROUTES);
			XMLUtil.setValue(elem, JAPModel.isPreCreateAnonRoutesEnabled());
			elemTor.appendChild(elem);
			e.appendChild(elemTor);

			/** add mixminion*/
			Element elemMixminion = doc.createElement(JAPConstants.CONFIG_Mixminion);
			Element elemMM = doc.createElement(JAPConstants.CONFIG_ROUTE_LEN);
			XMLUtil.setValue(elemMM, JAPModel.getMixminionRouteLen());
			elemMixminion.appendChild(elemMM);
			e.appendChild(elemMixminion);

			e.appendChild(JAPModel.getInstance().getRoutingSettings().getSettingsAsXml(doc));
			XMLUtil.formatHumanReadable(doc);
			return XMLUtil.toString(doc);
			//((XmlDocument)doc).write(f);
		}
		catch (Throwable ex)
		{
			LogHolder.log(LogLevel.EXCEPTION, LogType.MISC, ex);
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
		JAPMessages.init(l, JAPConstants.MESSAGESFN);
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
		if (newMixCascade == null)
		{
			return;
		}

		if (!m_currentMixCascade.equals(newMixCascade))
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
								  "MixCascade changed while in anonymity mode.");
					setAnonMode(true);
				}
				else
				{
					m_currentMixCascade = newMixCascade;
				}
			}
			notifyJAPObservers();
		}
		else
		{
			m_currentMixCascade = newMixCascade;
		}

	}

	/**
	 * Returns the active MixCascade.
	 *
	 * @return The active MixCascade.
	 */
	public MixCascade getCurrentMixCascade()
	{
		//synchronized (this) // Deadly for JDK 1.1.8 and possible Deadlock...
		{
			/* return only consistent values */
			return m_currentMixCascade;
		}
	}

	public void applyProxySettingsToInfoService(boolean a_bUseAuth)
	{
		if (m_Model.getProxyInterface() != null && m_Model.getProxyInterface().isValid())
		{
			HTTPConnectionFactory.getInstance().setNewProxySettings(m_Model.getProxyInterface(), a_bUseAuth);
		}
		else
		{
			//no Proxy should be used....
			HTTPConnectionFactory.getInstance().setNewProxySettings(null, false);
		}
	}

	private void applyProxySettingsToAnonService()
	{
		if (JAPModel.getInstance().getProxyInterface() != null &&
			JAPModel.getInstance().getProxyInterface().isValid() && getAnonMode())
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

	public void setInfoServiceDisabled(boolean b)
	{
		m_Model.setInfoServiceDisabled(b);
		synchronized (this)
		{
			setChanged();
			notifyObservers(new JAPControllerMessage(JAPControllerMessage.INFOSERVICE_POLICY_CHANGED));
		}
	}

	public static void setPreCreateAnonRoutes(boolean b)
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
	private final class SetAnonModeAsync extends Thread
	{
		private boolean m_startServer;
		private Thread m_waitForThread;
		private IAIEventListener m_caller;

		public SetAnonModeAsync(boolean a_startServer, Thread a_waitForThread, IAIEventListener a_caller)
		{
			super("SetAnonModeAsync");
			m_startServer = a_startServer;
			m_waitForThread = a_waitForThread;
			m_caller = a_caller;
		}

		public boolean isStartServerJob()
		{
			return m_startServer;
		}


		public void run()
		{
			boolean bRetryOnError;
			synchronized (m_inititalRunSync)
			{
				bRetryOnError = m_bInitialRun && JAPModel.getAutoConnect();
				m_bInitialRun = false;
			}
			if (m_waitForThread != null)
			{
				try
				{
					m_waitForThread.join();
				}
				catch (InterruptedException e)
				{
					LogHolder.log(LogLevel.DEBUG, LogType.MISC,
								  "Job for changing the anonymity mode to '" +
								  (new Boolean(m_startServer)).toString() + "' was canceled.");
				}
			}
			if (!isInterrupted())
			{
				/* job was not canceled -> we have to do it */
				try
				{
					setServerMode(m_startServer, bRetryOnError);
				}
				catch (Throwable a_e)
				{
					LogHolder.log(LogLevel.EXCEPTION, LogType.NET,
								  "Error while setting server mode to " + m_startServer + "!", a_e);
				}
				synchronized (m_changeAnonModeJobs)
				{
					/* remove ourself from the job-queue */
					m_changeAnonModeJobs.removeElement(this);
				}
				LogHolder.log(LogLevel.DEBUG, LogType.MISC,
							  "Job for changing the anonymity mode to '" +
							  (new Boolean(m_startServer)).toString() + "' was executed.");
			}
		}

		/**
		 * @param anonModeSelected true, if anonymity should be started; false otherwise
		 * @param a_bRetryOnConnectionError if in case of a connection error it is retried to
		 * establish the connection
		 */
		private synchronized void setServerMode(boolean anonModeSelected, boolean a_bRetryOnConnectionError)
		{
			//JAPWaitSplash splash = null;
			int msgIdConnect = 0;
			boolean canStartService = true;
			int versionCheck = 1;

			//setAnonMode--> async!!
			LogHolder.log(LogLevel.DEBUG, LogType.MISC, "setAnonMode(" + anonModeSelected + ")");
			if ( (m_proxyAnon == null) && (anonModeSelected))
			{ //start Anon Mode
				if (getView() != null)
				{
					getView().setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				}
				msgIdConnect = m_View.addStatusMsg(JAPMessages.getString("setAnonModeSplashConnect"),
					JAPDialog.MESSAGE_TYPE_INFORMATION, false);
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
					versionCheck = versionCheck(true);
					if (versionCheck == -1)
					{
						// update failed or new mandatory release version available
						m_bAlreadyCheckedForNewDevVersion = true;
					}
					else if (versionCheck == 0)
					{
						m_bAlreadyCheckedForNewVersion = true;
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
						if (JAPModel.getInstance().getProxyInterface() != null &&
							JAPModel.getInstance().getProxyInterface().isValid())
						{
							m_proxyAnon = new AnonProxy(
								m_socketHTTPListener, JAPModel.getInstance().getProxyInterface());
						}
						else
						{
							m_proxyAnon = new AnonProxy(m_socketHTTPListener, null);
						}
					}
					m_proxyAnon.addEventListener(JAPController.getInstance());

					//m_proxyAnon.setMixCascade(new SimpleMixCascadeContainer(
						//			   m_Controller.getCurrentMixCascade()));
					m_proxyAnon.setMixCascade(new AutoSwitchedMixCascadeContainer());
					m_proxyAnon.setAutoReConnect(JAPModel.getAutoReConnect());
					TorAnonServerDescription td = new TorAnonServerDescription(true,
						JAPModel.isPreCreateAnonRoutesEnabled());
					td.setMaxRouteLen(JAPModel.getTorMaxRouteLen());
					td.setMinRouteLen(JAPModel.getTorMinRouteLen());
					td.setMaxConnectionsPerRoute(JAPModel.getTorMaxConnectionsPerRoute());
					m_proxyAnon.setTorParams(td);
					m_proxyAnon.setMixminionParams(new MixminionServiceDescription(JAPModel.
						getMixminionRouteLen()));
					m_proxyAnon.setProxyListener(m_Controller);
					m_proxyAnon.setDummyTraffic(JAPModel.getDummyTraffic());
					// -> we can try to start anonymity
					if (m_proxyDirect != null)
					{
						m_proxyDirect.stopService();
					}
					m_proxyDirect = null;
					LogHolder.log(LogLevel.DEBUG, LogType.NET, "Try to start AN.ON service...");

					int ret = m_proxyAnon.start(a_bRetryOnConnectionError);

					if (ret == AnonProxy.E_BIND)
					{
						canStartService = false;
						m_proxyAnon = null;

						Object[] args =
							{
							new Integer(JAPModel.getHttpListenerPortNumber())};
						String msg = MessageFormat.format(JAPMessages.getString("errorListenerPort"),
							args);
						JAPDialog.showErrorDialog(getView(), msg, LogType.NET);
						JAPController.m_View.disableSetAnonMode();
					}
					else if (ret == AnonProxy.E_MIX_PROTOCOL_NOT_SUPPORTED)
					{
						canStartService = false;
						m_proxyAnon = null;
						JAPDialog.showErrorDialog(getView(),
												  JAPMessages.getString("errorMixProtocolNotSupported"),
												  LogType.NET);
					}
					//otte
					else if (ret == AnonProxy.E_SIGNATURE_CHECK_FIRSTMIX_FAILED)
					{
						canStartService = false;
						m_proxyAnon = null;
						JAPDialog.showErrorDialog(getView(),
												  JAPMessages.getString("errorMixFirstMixSigCheckFailed"),
												  LogType.NET);
					}

					else if (ret == AnonProxy.E_SIGNATURE_CHECK_OTHERMIX_FAILED)
					{
						canStartService = false;
						m_proxyAnon = null;
						JAPDialog.showErrorDialog(getView(),
												  JAPMessages.getString("errorMixOtherMixSigCheckFailed"),
												  LogType.NET);
					}
					else if (ret == ErrorCodes.E_SUCCESS ||
							 (ret != ErrorCodes.E_INTERRUPTED && a_bRetryOnConnectionError))
					{
						final AnonProxy proxyAnon = m_proxyAnon;
						AnonServiceEventAdapter adapter = new AnonServiceEventAdapter()
						{
							boolean bWaitingForConnection = true;
							public synchronized void connectionEstablished(
								AnonServerDescription a_serverDescription)
							{
								if (bWaitingForConnection)
								{
									try
									{
										proxyAnon.getAnonService().getPay().getAIControlChannel().
											addAIListener(m_caller);
									}
									catch (Exception a_e)
									{
										// do nothing
									}
									JAPController.getInstance().removeEventListener(this);
									bWaitingForConnection = false;
								}
							}
						};

						if (ret == ErrorCodes.E_SUCCESS)
						{
							LogHolder.log(LogLevel.DEBUG, LogType.NET, "AN.ON service started successfully");
							adapter.connectionEstablished(proxyAnon.getMixCascade());

							if (!mbActCntMessageNotRemind && !JAPModel.isSmallDisplay())
							{
								SwingUtilities.invokeLater(new Runnable()
								{
									public void run()
									{
										JAPDialog.LinkedCheckBox checkBox = new JAPDialog.LinkedCheckBox(false);
										JAPDialog.showWarningDialog(getView(),
											JAPMessages.getString("disableActCntMessage"),
											JAPMessages.getString("disableActCntMessageTitle"),
											checkBox);
										// show a Reminder message that active contents should be disabled

										mbActCntMessageNeverRemind = checkBox.getState();
										mbDoNotAbuseReminder = checkBox.getState();
										if (mbActCntMessageNeverRemind)
										{
											mbActCntMessageNotRemind = true;
										}
									}
								});

							}
						}
						else
						{
							JAPController.getInstance().addEventListener(adapter);
							LogHolder.log(LogLevel.INFO, LogType.NET,
										  "AN.ON service not connected. Trying reconnect...");
						}

						// start feedback thread
						//m_feedback.update();
					}
					// ootte
					else
					{
						canStartService = false;
						m_proxyAnon = null;
						if (!JAPModel.isSmallDisplay() && ret != ErrorCodes.E_INTERRUPTED)
						{
							LogHolder.log(LogLevel.ERR, LogType.NET,
										  "Error starting AN.ON service! - ErrorCode: " +
										  Integer.toString(ret));
							JAPDialog.showErrorDialog(getView(), JAPMessages.getString("errorConnectingFirstMix"),
								JAPMessages.getString("errorConnectingFirstMixTitle"),
								LogType.NET);
						}
					}
				}
				if (getView() != null)
				{
					getView().setCursor(Cursor.getDefaultCursor());
				}
				notifyJAPObservers();
				//splash.abort();
				m_View.removeStatusMsg(msgIdConnect);
				if (!canStartService)
				{
					setAnonMode(false);
				}
				else
				{
					if (versionCheck == 1 && !m_bAlreadyCheckedForNewVersion &&
						!JAPModel.isInfoServiceDisabled())
					{
						versionCheck = versionCheck(true);
						if (versionCheck == -1)
						{
							// update failed, check for optional update will not work, either
							m_bAlreadyCheckedForNewDevVersion = true;
							// -> we must stop anonymity
							//setAnonMode(false);
						}
						else if (versionCheck == 0)
						{
							m_bAlreadyCheckedForNewVersion = true;
						}
					}
				}

				if (versionCheck != -1 && !m_bAlreadyCheckedForNewDevVersion &&
					JAPModel.getInstance().isReminderForOptionalUpdateActivated())
				{
					if (versionCheck(false) != 1)
					{
						m_bAlreadyCheckedForNewDevVersion = true;
					}
				}

				if (canStartService && !JAPModel.isInfoServiceDisabled())
				{
					MixCascade cascade = null;
					try
					{
						cascade = m_proxyAnon.getMixCascade();
					}
					catch (NullPointerException a_e)
					{
					}
					if (cascade != null && !cascade.isUserDefined())
					{
						if (Database.getInstance(MixCascade.class).getEntryById(cascade.getId()) == null)
						{
							Database.getInstance(MixCascade.class).update(
								InfoServiceHolder.getInstance().getMixCascadeInfo(cascade.getId()));
						}
					}
				}
			}
			else if ( (m_proxyDirect == null) && (!anonModeSelected))
			{
				if (m_proxyAnon != null)
				{
					msgIdConnect = m_View.addStatusMsg(JAPMessages.getString("setAnonModeSplashDisconnect"),
						JAPDialog.MESSAGE_TYPE_INFORMATION, false);
					m_proxyAnon.stop();
				}

				synchronized (m_finishSync)
				{
					m_proxyAnon = null;
					m_finishSync.notifyAll();
				}

				m_proxyDirect = new DirectProxy(m_socketHTTPListener);
				m_proxyDirect.startService();

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

	public boolean isAnonConnected()
	{
		return m_proxyAnon != null && m_proxyAnon.isConnected();
	}

	public void setAnonMode(final boolean a_anonModeSelected)
	{
		final JAPController controller = this;


		synchronized (m_changeAnonModeJobs)
		{
			if (a_anonModeSelected && m_bShutdown)
			{
				// do not make new connection during shutdown
				return;
			}
			boolean newJob = true;
			if (m_changeAnonModeJobs.size() > 0)
			{
				/* check whether this is job is different to the last one */
				SetAnonModeAsync lastJob = (SetAnonModeAsync) (m_changeAnonModeJobs.lastElement());
				if (lastJob.isStartServerJob() == a_anonModeSelected)
				{
					/* it's the same (enabling server / disabling server) as the last job */
					newJob = false;
				}
			}
			if (newJob)
			{
				/* it's a new job -> do something */
				if ( (!a_anonModeSelected && (m_changeAnonModeJobs.size() >= 2)) ||
					 (m_changeAnonModeJobs.size() >= 3))
				{
					/* because of enough previous jobs in the queue, we can ignore this job, if we also
					 * interrupt and remove the previous one
					 */
					SetAnonModeAsync previousJob = (SetAnonModeAsync) (m_changeAnonModeJobs.lastElement());
					previousJob.interrupt();
					m_changeAnonModeJobs.removeElement(previousJob);
				}

				else
				{
					/* we have to schedule this job */
					if (!a_anonModeSelected && (m_changeAnonModeJobs.size() == 1))
					{
						/* there is a start-server job currently running -> try to interrupt it */
						SetAnonModeAsync previousJob = (SetAnonModeAsync)m_changeAnonModeJobs.lastElement();
						previousJob.interrupt();
					}
					SetAnonModeAsync currentJob = null;
					if (m_changeAnonModeJobs.size() > 0)
					{
						/* wait until the previous job is done */
						currentJob = new SetAnonModeAsync(a_anonModeSelected,
							( (SetAnonModeAsync) (m_changeAnonModeJobs.lastElement())), controller);
					}
					else
					{
						/* we don't have to wait for any previous job */
						currentJob = new SetAnonModeAsync(
							a_anonModeSelected, null, controller);
					}

					currentJob.setDaemon(true);
					currentJob.start();
					m_changeAnonModeJobs.addElement(currentJob);
					LogHolder.log(LogLevel.DEBUG, LogType.MISC,
								  "JAPController: setAnonMode: Added a job for changing the anonymity mode to '" +
								  (new Boolean(a_anonModeSelected)).toString() +
								  "' to the job queue.");
				}
			}
		}
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
			//if (m_currentMixCascade.isCertified())
			{
				setAnonMode(true);
			}
			//else
			{
				/** @todo ask if user wants to connect nevertheless!! */
			}
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
				JAPDialog.showMessageDialog(getView(),
											JAPMessages.getString("confmessageListernPortChanged"));
			}
			m_Controller.notifyJAPObservers();
		}
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

	public static void setMixminionRouteLen(int len)
	{
		m_Model.setMixminionRouteLen(len);
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
				LogHolder.log(LogLevel.INFO, LogType.NET, "Started listener on port " + port + ".");
				/*
				try
				{
					s.setSoTimeout(2000);
				}
				catch (Exception e1)
				{
					LogHolder.log(LogLevel.DEBUG, LogType.NET,
								  "Could not set listener accept timeout: Exception: " +
								  e1.getMessage());
				}*/
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
	 *	@param bShowConfigSaveErrorMsg if true shows an error message if saving of
	 * 			the current configuration goes wrong
	 */
	public static void goodBye(final boolean bShowConfigSaveErrorMsg)
	{
		Thread stopThread = new Thread()
		{
			public void run()
			{
				int returnValue;
				JAPDialog.LinkedCheckBox checkBox;
				if (!JAPModel.getInstance().isNeverRemindGoodbye() && bShowConfigSaveErrorMsg)
				{
					// show a Reminder message that active contents should be disabled
					checkBox = new JAPDialog.LinkedCheckBox(false);
					returnValue = JAPDialog.showConfirmDialog(getView(),
						JAPMessages.getString(MSG_DISABLE_GOODBYE),
						JAPDialog.OPTION_TYPE_OK_CANCEL, JAPDialog.MESSAGE_TYPE_INFORMATION, checkBox);
					if (returnValue == JAPDialog.RETURN_VALUE_OK)
					{
						getView().setEnabled(false);
						JAPModel.getInstance().setNeverRemindGoodbye(checkBox.getState());
					}
				}
				else
				{
					returnValue = JAPDialog.RETURN_VALUE_OK;
				}

				if (returnValue == JAPDialog.RETURN_VALUE_OK)
				{
					if (getView() != null)
					{
						getView().setEnabled(false);
					}
					//JAPDll.checkDllVersion(false);
					boolean error = m_Controller.saveConfigFile();
					if (error && bShowConfigSaveErrorMsg)
					{
						JAPDialog.showErrorDialog(getView(), JAPMessages.getString(MSG_ERROR_SAVING_CONFIG,
							JAPModel.getInstance().getConfigFile()), LogType.MISC);
					}
					JAPDialog.setConsoleOnly(true); // do not show any dialogs now
					m_Controller.m_bShutdown = true;
					// disallow InfoService traffic
					JAPModel.getInstance().setInfoServiceDisabled(true);
					m_Controller.m_feedback.stop();
					m_Controller.m_MixCascadeUpdater.stop();
					m_Controller.m_InfoServiceUpdater.stop();
					// do not show direct connection warning dialog
					DirectProxy.setAllowUnprotectedConnectionCallback(null);

					try
					{
						m_Controller.setAnonMode(false);
						// Wait until anon mode is disabled
						synchronized (m_Controller.m_finishSync)
						{
							if (m_Controller.getAnonMode() || m_Controller.isAnonConnected())
							{
								try
								{
									m_Controller.m_finishSync.wait();
								}
								catch (InterruptedException a_e)
								{

								}
							}
						}


						//Wait until all Jobs are finished....
						for (int i = 0; i < 5 && m_Controller.m_changeAnonModeJobs.size() > 0; i++)
						{
							Vector vecJobs = (Vector)m_Controller.m_changeAnonModeJobs.clone();
							Enumeration jobs = vecJobs.elements();
							SetAnonModeAsync job;
							while (jobs.hasMoreElements())
							{
								job = (SetAnonModeAsync)jobs.nextElement();
								if (job.isStartServerJob())
								{
									( (SetAnonModeAsync) jobs.nextElement()).interrupt();
								}
							}

							try
							{
								sleep(500);
							}
							catch (InterruptedException a_e)
							{
								// ignore
							}
						}
					}
					catch (Throwable a_e)
					{
						LogHolder.log(LogLevel.EMERG, LogType.MISC, a_e);
					}
					try
					{
						m_Controller.m_proxyDirect.stopService();
					}
					catch (NullPointerException a_e)
					{
						// ignore
					}

					// do not show any dialogs in this state
					if (getView() != null)
					{
						getView().dispose();
					}
					LogHolder.log(LogLevel.INFO, LogType.GUI, "View has been disposed. Finishing...");
					System.exit(0);
				}
			}
		};
		if (!JAPDialog.isConsoleOnly() && SwingUtilities.isEventDispatchThread())
		{
			stopThread.start();
		}
		else
		{
			stopThread.run();
		}
	}

	/** Shows the About dialog
	 */
	public static void aboutJAP()
	{
		try
		{
			new JAPAbout(getView());
		}
		catch (Throwable t)
		{
			LogHolder.log(LogLevel.EXCEPTION, LogType.GUI, t);
		}
	}

	/**
	 * Updates the list of known InfoServices.
	 * @return true if the update was successful; false otherwise
	 */
	public boolean updateInfoServices()
	{
		return m_InfoServiceUpdater.update();
	}

	/**
	 * Get all available mixcascades from the infoservice and store it in the database.
	 * @param bShowError should an Error Message be displayed if something goes wrong ?
	 */
	public void fetchMixCascades(boolean bShowError, Component a_view)
	{
		LogHolder.log(LogLevel.INFO, LogType.MISC, "Trying to fetch mixcascades from infoservice.");

		while (!m_MixCascadeUpdater.update())
		{
			LogHolder.log(LogLevel.ERR, LogType.NET, "No connection to infoservices.");
			if (!JAPModel.isSmallDisplay() &&
				(bShowError || Database.getInstance(MixCascade.class).getNumberofEntries() == 0))
			{
				if (!JAPModel.getInstance().isInfoServiceViaDirectConnectionAllowed() && !isAnonConnected())
				{
					int returnValue =
						JAPDialog.showConfirmDialog(a_view, JAPMessages.getString(MSG_IS_NOT_ALLOWED),
						JAPDialog.OPTION_TYPE_YES_NO, JAPDialog.MESSAGE_TYPE_ERROR);
					if (returnValue == JAPDialog.RETURN_VALUE_YES)
					{
						JAPModel.getInstance().allowInfoServiceViaDirectConnection(true);
						updateInfoServices();
						continue;
					}
				}
				else
				{
					JAPDialog.showErrorDialog(a_view, JAPMessages.getString("errorConnectingInfoService"),
											  LogType.NET);
				}
			}
			break;
		}
	}

	/**
	 * Performs the Versioncheck.
	 *
	 * @return  0, if the local JAP version is up to date.
	 *         -1, if version check says that anonymity mode should not be enabled. Reasons can be:
	 *             new version found, version check failed.
	 *          1, if no version check could be done
	 */
	public int versionCheck(boolean a_bForced)
	{
		LogHolder.log(LogLevel.INFO, LogType.MISC, "Checking for new version of JAP...");
		JAPVersionInfo vi = null;
		String updateVersionNumber = null;
		if (a_bForced)
		{
			updateVersionNumber = InfoServiceHolder.getInstance().getNewVersionNumber();
		}
		else
		{
			if (JAPConstants.m_bReleasedVersion)
			{
				vi = InfoServiceHolder.getInstance().getJAPVersionInfo(JAPVersionInfo.JAP_RELEASE_VERSION);
			}
			else
			{
				vi = InfoServiceHolder.getInstance().getJAPVersionInfo(JAPVersionInfo.JAP_DEVELOPMENT_VERSION);
			}
			if (vi != null)
			{
				updateVersionNumber = vi.getJapVersion();
			}
		}


		if (updateVersionNumber != null)
		{
			updateVersionNumber = updateVersionNumber.trim();
			LogHolder.log(LogLevel.DEBUG, LogType.MISC, "Local version: " + JAPConstants.aktVersion);
			if (updateVersionNumber.compareTo(JAPConstants.aktVersion) <= 0)
			{
				/* the local JAP version is up to date -> exit */
				return 0;
			}
			/* local version is not up to date, new version is available -> ask the user whether to
			 * download the new version or not
			 */
			String message;
			JAPDialog.LinkedCheckBox checkbox = null;
			if (a_bForced)
			{
				message = JAPMessages.getString("newVersionAvailable", updateVersionNumber);
			}
			else
			{
				String dev = ")";
				if (!JAPConstants.m_bReleasedVersion)
				{
					dev = "-dev)";
				}
				message = JAPMessages.getString(MSG_NEW_OPTIONAL_VERSION, updateVersionNumber + dev);
				checkbox = new JAPDialog.LinkedCheckBox(false);
			}
			JAPDll.setWindowOnTop(getView(), true);
			boolean bAnswer = JAPDialog.showYesNoDialog(getView(),
														message,
														JAPMessages.getString("newVersionAvailableTitle"),
														checkbox);
			JAPDll.setWindowOnTop(getView(), false);
			if (checkbox != null)
			{
				JAPModel.getInstance().setReminderForOptionalUpdate(!checkbox.getState());
			}
			if (bAnswer)
			{
				/* User has selected to download new version of JAP -> Download, Alert, exit program */
				if (a_bForced)
				{
					vi = InfoServiceHolder.getInstance().getJAPVersionInfo(JAPVersionInfo.JAP_RELEASE_VERSION);
				}
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
						LogHolder.log(LogLevel.ERR, LogType.MISC, "Some update problem.");
						JAPDialog.showErrorDialog(getView(),
												  JAPMessages.getString("downloadFailed") +
												  JAPMessages.getString("infoURL"), LogType.MISC);
						if (a_bForced)
						{
							notifyJAPObservers();
						}
						/* update failed -> exit */
						return -1;
					}
					/* should never be reached, because if update was successful, the JAPUpdateWizard closes
					 * JAP
					 */
					//goodBye(false);
					return 0;
				}
				/* update was not successful, because we could not get the JAPVersionInfo -> alert, and
				 * reset anon mode to false
				 */
				LogHolder.log(LogLevel.ERR, LogType.MISC, "Could not get JAPVersionInfo.");
				JAPDialog.showErrorDialog(getView(),
										  JAPMessages.getString("downloadFailed") +
										  JAPMessages.getString("infoURL"), LogType.MISC);
				if (a_bForced)
				{
					notifyJAPObservers();
				}
				/* update failed -> exit */
				return -1;
			}
			else
			{
				/* User has selected not to download -> Alert, we should'nt start the system due to
				 * possible compatibility problems
				 */
				if (a_bForced)
				{


					JAPDialog.showWarningDialog(getView(), JAPMessages.getString("youShouldUpdate"),
												JAPUtil.createDialogBrowserLink(JAPMessages.getString("infoURL")));
					//notifyJAPObservers();
					return -1;
				}
				return 0;
			}
		}
		else
		{
			/* can't get the current version number from the infoservices. Ignore this,
			 * as this is not a problem!
			 */
			LogHolder.log(LogLevel.ERR, LogType.MISC,
						  "Could not get the current JAP version number from infoservice.");
			/*
				JAPDialog.showErrorDialog(m_View, JAPMessages.getString("errorConnectingInfoService"),
					LogType.NET);*/
			//notifyJAPObservers();
			return 1;
		}
		/* this line should never be reached */
	}

	//---------------------------------------------------------------------
	public void registerMainView(IJAPMainView v)
	{
		m_View = v;
		if (getView() != null)
		{
			JAPHelp.init(getView(), AbstractOS.getInstance());
		}
	}

	public static AbstractJAPMainView getView()
	{
		if (m_View instanceof AbstractJAPMainView)
		{
			return (AbstractJAPMainView)JAPController.m_View;
		}
		return null;
	}

	public void removeEventListener(AnonServiceEventListener a_listener)
	{
		m_anonServiceListener.removeElement(a_listener);
	}

	public void addEventListener(AnonServiceEventListener a_listener)
	{
		synchronized (m_anonServiceListener)
		{
			Enumeration e = m_anonServiceListener.elements();
			while (e.hasMoreElements())
			{
				if (a_listener.equals(e.nextElement()))
				{
					return;
				}
			}
			m_anonServiceListener.addElement(a_listener);
		}
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

	public synchronized void transferedBytes(long bytes, int protocolType)
	{
		long b;
		if (protocolType == IProxyListener.PROTOCOL_WWW)
		{
			m_nrOfBytesWWW += bytes;
			b = m_nrOfBytesWWW;
		}
		else if (protocolType == IProxyListener.PROTOCOL_OTHER)
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
		if (!m_bForwarderNotExplain && a_activate)
		{
			/* show a message box with the explanation of the forwarding stuff */
			JAPDialog.LinkedCheckBox checkbox = new JAPDialog.LinkedCheckBox(false);
			JAPDialog.showMessageDialog(getView(), JAPMessages.getString("forwardingExplainMessage"),
				JAPMessages.getString("forwardingExplainMessageTitle"), checkbox);

			m_bForwarderNotExplain = checkbox.getState();
		}
		if (m_iStatusPanelMsgIdForwarderServerStatus != -1)
		{
			/* remove old forwarding server messages from the status bar */
			m_View.removeStatusMsg(m_iStatusPanelMsgIdForwarderServerStatus);
			m_iStatusPanelMsgIdForwarderServerStatus = -1;
		}
		if (JAPModel.getInstance().getRoutingSettings().getRoutingMode() !=
			JAPRoutingSettings.ROUTING_MODE_CLIENT)
		{
			/* don't allow to interrupt the client forwarding mode */
			if (a_activate)
			{
				/* start the server */
				if (JAPModel.getInstance().getRoutingSettings().setRoutingMode(JAPRoutingSettings.
					ROUTING_MODE_SERVER) == true)
				{
					/* starting the server was successful -> start propaganda with blocking in a separate
					 * thread
					 */
					Thread startPropagandaThread = new Thread(new Runnable()
					{
						public void run()
						{
							int msgId = m_View.addStatusMsg(JAPMessages.getString(
								"controllerStatusMsgRoutingStartServer"), JAPDialog.MESSAGE_TYPE_INFORMATION, false);
							int registrationStatus = JAPModel.getInstance().getRoutingSettings().
								startPropaganda(true);
							m_View.removeStatusMsg(msgId);
							/* if there occured an error while registration, show a message box */
							switch (registrationStatus)
							{
								case JAPRoutingSettings.REGISTRATION_NO_INFOSERVICES:
								{
									JAPDialog.showErrorDialog(getView(),
										JAPMessages.getString(
											"settingsRoutingServerRegistrationEmptyListError"),
										LogType.MISC);
									break;
								}
								case JAPRoutingSettings.REGISTRATION_UNKNOWN_ERRORS:
								{
									JAPDialog.showErrorDialog(getView(),
										JAPMessages.getString("settingsRoutingServerRegistrationUnknownError"),
										LogType.MISC);
									break;
								}
								case JAPRoutingSettings.REGISTRATION_INFOSERVICE_ERRORS:
								{
									JAPDialog.showErrorDialog(getView(),
										JAPMessages.getString(
											"settingsRoutingServerRegistrationInfoservicesError"),
										LogType.MISC);
									break;
								}
								case JAPRoutingSettings.REGISTRATION_VERIFY_ERRORS:
								{
									JAPDialog.showErrorDialog(getView(),
										JAPMessages.getString(
											"settingsRoutingServerRegistrationVerificationError"),
										LogType.MISC);
									break;
								}
								case JAPRoutingSettings.REGISTRATION_SUCCESS:
								{
									/* show a success message in the status bar */
									m_iStatusPanelMsgIdForwarderServerStatus = m_View.addStatusMsg(
										JAPMessages.getString("controllerStatusMsgRoutingStartServerSuccess"),
										JAPDialog.MESSAGE_TYPE_INFORMATION, true);
								}
							}
						}
					});
					startPropagandaThread.setDaemon(true);
					startPropagandaThread.start();
				}
				else
				{
					/* opening the server port was not successful -> show an error message */
					m_iStatusPanelMsgIdForwarderServerStatus = m_View.addStatusMsg(JAPMessages.getString(
						"controllerStatusMsgRoutingStartServerError"), JAPDialog.MESSAGE_TYPE_ERROR, true);
					JAPDialog.showErrorDialog(getView(), JAPMessages.getString("settingsRoutingStartServerError"),
											  LogType.MISC);

				}
			}
			else
			{
				/* stop the server -> the following call will stop all forwarding server activities
				 * immediately
				 */
				JAPModel.getInstance().getRoutingSettings().setRoutingMode(JAPRoutingSettings.
					ROUTING_MODE_DISABLED);
				m_iStatusPanelMsgIdForwarderServerStatus = m_View.addStatusMsg(JAPMessages.getString(
					"controllerStatusMsgRoutingServerStopped"), JAPDialog.MESSAGE_TYPE_INFORMATION, true);
			}
		}
	}

	static public InfoServiceDBEntry createDefaultInfoService() throws Exception
	{
		Vector listeners = new Vector();
		for (int i = 0; i < JAPConstants.DEFAULT_INFOSERVICE_PORT_NUMBERS.length; i++)
		{
			listeners.addElement(new ListenerInterface(JAPConstants.DEFAULT_INFOSERVICE_HOSTNAME,
				JAPConstants.DEFAULT_INFOSERVICE_PORT_NUMBERS[i]));
		}

		InfoServiceDBEntry defaultInfoService = new InfoServiceDBEntry(JAPConstants.
			DEFAULT_INFOSERVICE_NAME, listeners, true, true);
		defaultInfoService.setId(JAPConstants.DEFAULT_INFOSERVICE_ID);
		defaultInfoService.setUserDefined(false);
		return defaultInfoService;
	}

	/** load the default certificates */
	static public void addDefaultCertificates()
	{
		JAPCertificate defaultRootCert = null;
		/* each certificate in the directory for the default mix-certs is loaded */
	    Enumeration mixCertificates = JAPCertificate.getInstance(JAPConstants.CERTSPATH + JAPConstants.MIX_CERTSPATH, true).elements();
		while(mixCertificates.hasMoreElements())
		{
			defaultRootCert = (JAPCertificate)mixCertificates.nextElement();
			SignatureVerifier.getInstance().getVerificationCertificateStore().
			addCertificateWithoutVerification(defaultRootCert, JAPCertificate.CERTIFICATE_TYPE_ROOT_MIX, true, true);
		}
		/* no elements were found */
		if (defaultRootCert == null)
		{
			LogHolder.log(LogLevel.ERR, LogType.MISC,
						  "Error loading default Mix root certificates.");
		}

		defaultRootCert = JAPCertificate.getInstance(ResourceLoader.loadResource(JAPConstants.
			CERTSPATH + JAPConstants.TRUSTEDINFOSERVICEROOTCERT));
		if (defaultRootCert != null)
		{
			SignatureVerifier.getInstance().getVerificationCertificateStore().
				addCertificateWithoutVerification(defaultRootCert,
												  JAPCertificate.CERTIFICATE_TYPE_ROOT_INFOSERVICE, true, true);
		}
		else
		{
			LogHolder.log(LogLevel.ERR, LogType.MISC,
						  "Error loading default InfoService root certificate.");
		}

		JAPCertificate updateMessagesCert = JAPCertificate.getInstance(ResourceLoader.loadResource(
			JAPConstants.CERTSPATH + JAPConstants.CERT_JAPINFOSERVICEMESSAGES));
		if (updateMessagesCert != null)
		{
			SignatureVerifier.getInstance().getVerificationCertificateStore().
				addCertificateWithoutVerification(updateMessagesCert, JAPCertificate.CERTIFICATE_TYPE_UPDATE, true, true);
		}
		else
		{
			LogHolder.log(LogLevel.ERR, LogType.MISC,
						  "Error loading default update messages certificate.");
		}
	}

	public void connecting(AnonServerDescription a_serverDescription)
	{
		if (a_serverDescription instanceof MixCascade &&
			!m_currentMixCascade.equals(a_serverDescription))
		{
			m_currentMixCascade = (MixCascade)a_serverDescription;
			notifyJAPObservers();
		}
		synchronized (m_anonServiceListener)
		{
			Enumeration e = m_anonServiceListener.elements();
			while (e.hasMoreElements())
			{
				( (AnonServiceEventListener) e.nextElement()).connecting(
								a_serverDescription);
			}
		}
	}


	public void connectionEstablished(AnonServerDescription a_serverDescription)
	{
		new Thread()
		{
			public void run()
			{
				m_feedback.update();
			}
		}.start();

		synchronized (m_anonServiceListener)
		{
			Enumeration e = m_anonServiceListener.elements();
			while (e.hasMoreElements())
			{
				( (AnonServiceEventListener) e.nextElement()).connectionEstablished(
								a_serverDescription);
			}
		}
	}

	public void dataChainErrorSignaled()
	{
		connectionError();
		synchronized (m_anonServiceListener)
		{
			Enumeration e = m_anonServiceListener.elements();
			while (e.hasMoreElements())
			{
				( (AnonServiceEventListener) e.nextElement()).dataChainErrorSignaled();
			}
		}
	}


	public void disconnected()
	{
		synchronized (m_finishSync)
		{
			m_proxyAnon = null;
			synchronized (m_anonServiceListener)
			{
				Enumeration e = m_anonServiceListener.elements();
				while (e.hasMoreElements())
				{
					( (AnonServiceEventListener) e.nextElement()).disconnected();
				}
			}
			m_finishSync.notifyAll();
		}
	}

	public void connectionError()
	{
		LogHolder.log(LogLevel.ERR, LogType.NET, "JAPController received connectionError");
		if (!m_Model.getAutoReConnect())
		{
			this.setAnonMode(false);
		}

		synchronized (m_anonServiceListener)
		{
			Enumeration e = m_anonServiceListener.elements();
			while (e.hasMoreElements())
			{
				( (AnonServiceEventListener) e.nextElement()).connectionError();
			}
		}
	}

	/**
	 * Gets the default Payment Instance
	 * @return BI
	 */
	public BI getDefaultPI()
	{
		ListenerInterface li = new ListenerInterface(JAPConstants.PI_HOST, JAPConstants.PI_PORT);
		try
		{
			return new BI(JAPConstants.PI_ID, JAPConstants.PI_NAME, li.toVector(),
						  JAPCertificate.getInstance(ResourceLoader.loadResource(JAPConstants.CERTSPATH +
				JAPConstants.PI_CERT)));
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.PAY, "Could not create default PI: " + e.getMessage());
			return null;
		}
	}

	public void unrealisticBytes(long a_bytes)
	{
		JAPDll.setWindowOnTop(getView(), true);
		boolean choice = JAPDialog.showYesNoDialog(
			getView(),
			JAPMessages.getString("unrealBytesDesc") + "<p>" +
			JAPMessages.getString("unrealBytesDifference") + " " + a_bytes,
			JAPMessages.getString("unrealBytesTitle")
			);
		JAPDll.setWindowOnTop(getView(),false);
		if (!choice)
		{
			this.setAnonMode(false);
		}
	}

	/**
	 * Gets the password for payment data encryption
	 * @return String
	 */
	public String getPaymentPassword()
	{
		return JAPModel.getInstance().getPaymentPassword();
	}

	/**
	 * Sets the password for payment data encryption
	 * @param a_password Strign
	 */
	public void setPaymentPassword(String a_password)
	{
		JAPModel.getInstance().setPaymentPassword(a_password);
	}

	/**
	 * Fetches new statements for all accounts from the payment instance
	 */
	public void updateAccountStatements()
	{
		Runnable doIt = new Runnable()
		{
			public void run()
			{
				Enumeration accounts = PayAccountsFile.getInstance().getAccounts();

				while (accounts.hasMoreElements())
				{
					PayAccount account = (PayAccount) accounts.nextElement();
					try
					{
						LogHolder.log(LogLevel.DEBUG, LogType.PAY,
									  "Fetching statement for account: " + account.getAccountNumber());
						account.fetchAccountInfo(JAPModel.getInstance().getPaymentProxyInterface(), false);
					}
					catch (Exception e)
					{
						LogHolder.log(LogLevel.ERR, LogType.PAY,
									  "Could not fetch statement for account: " + account.getAccountNumber());
					}
				}
			}
		};
		new Thread(doIt).start();
	}

	public void packetMixed(long a_totalBytes)
	{
		JAPModel.getInstance().setMixedBytes(a_totalBytes);
		synchronized (m_anonServiceListener)
		{
			Enumeration e = m_anonServiceListener.elements();
			while (e.hasMoreElements())
			{
				( (AnonServiceEventListener) e.nextElement()).packetMixed(a_totalBytes);
			}
		}
	}

	public long getMixedBytes()
	{
		return JAPModel.getInstance().getMixedBytes();
	}

	public boolean getDontAskPayment()
	{
		return m_bPayCascadeNoAsk;
	}

	public void setDontAskPayment(boolean a_payCascadeNoAsk)
	{
		m_bPayCascadeNoAsk = a_payCascadeNoAsk;
	}

	/**
	 * This class returns a new random cascade from all currently available cascades every time
	 * getNextCascade() is called. If all available cascades have been returned once, this class starts
	 * again by choosing the random cascades from all available ones.
	 * @author Rolf Wendolsky
	 */
	private class AutoSwitchedMixCascadeContainer extends AbstractMixCascadeContainer
	{
		private Hashtable m_alreadyTriedCascades;
		private Random m_random;
		private MixCascade m_initialCascade;
		private MixCascade m_currentCascade;
		private boolean m_bKeepCurrentCascade;

		public AutoSwitchedMixCascadeContainer()
		{
			m_alreadyTriedCascades = new Hashtable();
			m_random = new Random();
			m_initialCascade = JAPController.getInstance().getCurrentMixCascade();
			m_bKeepCurrentCascade = false;
		}
		public MixCascade getNextMixCascade()
		{
			synchronized (m_alreadyTriedCascades)
			{
				if (!JAPModel.getAutoReConnect() ||
					!JAPModel.getInstance().isCascadeConnectionChosenAutomatically())
				{
					m_alreadyTriedCascades.clear();
					m_bKeepCurrentCascade = false;
					if (m_currentCascade == null)
					{
						m_currentCascade = m_initialCascade;
					}
				}
				else if (m_bKeepCurrentCascade)
				{
					// do not check if this cascade has been used before
					m_bKeepCurrentCascade = false;
					if (m_currentCascade == null)
					{
						m_currentCascade = m_initialCascade;
					}
					if (m_currentCascade != null)
					{
						m_alreadyTriedCascades.put(m_currentCascade.getId(), m_currentCascade);
					}
				}
				else if (m_initialCascade == null ||
						 m_alreadyTriedCascades.containsKey(m_initialCascade.getId()))
				{
					MixCascade currentCascade = null;
					Vector availableCascades = Database.getInstance(MixCascade.class).getEntryList();
					if (availableCascades.size() > 0)
					{
						int chosenCascadeIndex = m_random.nextInt();
						if (chosenCascadeIndex < 0)
						{
							// only positive numers are allowed
							chosenCascadeIndex *= -1;
						}
						// chose an index from the vector
						chosenCascadeIndex %= availableCascades.size();
						/* Go through all indices until a suitable MixCascade is found or the original index
						 * is reached.
						 */
						for (int i = 0; i < availableCascades.size(); i++)
						{
							currentCascade = (MixCascade) availableCascades.elementAt(chosenCascadeIndex);
							// this is the logic that decides whether to use a cascade or not
							if (!m_alreadyTriedCascades.containsKey(currentCascade.getId()))
							{
								m_alreadyTriedCascades.put(currentCascade.getId(), currentCascade);
								if (!(currentCascade.isPayment() &&
									  (!JAPController.getInstance().getDontAskPayment() ||
									   PayAccountsFile.getInstance().getNumAccounts() == 0)))
								{
									break;
								}
								currentCascade = null;
							}
							chosenCascadeIndex = (chosenCascadeIndex + 1) % availableCascades.size();
						}
					}
					else if (m_initialCascade == null)
					{
						// no cascade is available
						return null;
					}
					if (currentCascade == null)
					{
						m_alreadyTriedCascades.clear();
						m_currentCascade = getNextMixCascade();
					}
					m_currentCascade = currentCascade;
				}
				else
				{
					m_alreadyTriedCascades.put(m_initialCascade.getId(), m_initialCascade);
					m_currentCascade = m_initialCascade;
				}
			}

			return m_currentCascade;
		}
		public MixCascade getCurrentMixCascade()
		{
			return m_currentCascade;
		}

		public void keepCurrentCascade(boolean a_bKeepCurrentCascade)
		{
			synchronized (m_alreadyTriedCascades)
			{
				m_bKeepCurrentCascade = a_bKeepCurrentCascade;
			}
		}
	}
}
