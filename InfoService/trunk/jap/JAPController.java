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
import java.util.Vector;
import javax.xml.parsers.DocumentBuilderFactory;
import java.awt.Cursor;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Point;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.UIManager;
import javax.swing.UIManager$LookAndFeelInfo;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Text;
import anon.crypto.JAPCertificateStore;
import anon.infoservice.HTTPConnectionFactory;
import anon.infoservice.InfoService;
import anon.infoservice.InfoServiceDatabase;
import anon.infoservice.InfoServiceHolder;
import anon.infoservice.JAPVersionInfo;
import anon.infoservice.MixCascade;
import anon.server.impl.XMLUtil;
import logging.LogHolder;
import proxy.AnonProxy;
import proxy.DirectProxy;
import proxy.ProxyListener;
import update.JAPUpdateWizard;

/* This is the Model of All. It's a Singelton!*/
public final class JAPController
	implements ProxyListener
{

	/**
	 * Stores all MixCascades we know (information comes from infoservice).
	 */
	private Vector mixCascadeDatabase = null;

	/**
	 * Stores the active MixCascade.
	 */
	private MixCascade currentMixCascade = null;

	private ServerSocket m_socketHTTPListener = null; // listener object
	private DirectProxy m_proxyDirect = null; // service object for direct access (bypass anon service)
	private AnonProxy m_proxyAnon = null; // service object for anon access

	private boolean isRunningListener = false; // true if a listener is running

	//private boolean  canStartService             = false; // indicates if anon service can be started
	private boolean m_bAlreadyCheckedForNewVersion = false; // indicates if check for new version has already been done
	private boolean mbActCntMessageNotRemind = false; // indicates if Warning message in setAnonMode has been deactivated for the session
	private boolean mbActCntMessageNeverRemind = false; // indicates if Warning message in setAnonMode has been deactivated forever
	private boolean mbDoNotAbuseReminder = false; // indicates if new warning message in setAnonMode (containing Do no abuse) has been shown
	private boolean mbGoodByMessageNeverRemind = false; // indicates if Warning message before exit has been deactivated forever

	private static final Object oSetAnonModeSyncObject = new Object();

	public String status1 = " ";
	public String status2 = " ";

	private int nrOfChannels = 0;
	private int nrOfBytes = 0;

	private static JAPView m_View = null;
	private static JAPController m_Controller = null;
	private static JAPModel m_Model = null;
	private static JAPFeedback feedback = null;
	private Locale m_Locale = null;
	private Vector observerVector = null;

	private static Font m_fontControls;

	private JAPController()
	{
		m_Model = JAPModel.create();
		// Create observer object
		observerVector = new Vector();

		/* set a default mixcascade */
		try
		{
			currentMixCascade = new MixCascade(JAPConstants.defaultAnonHost,
											   JAPConstants.defaultAnonPortNumber);
		}
		catch (Exception e)
		{
			JAPDebug.out(JAPDebug.EMERG, JAPDebug.NET, "JAPController: Constructor: " + e.getMessage());
		}
		/* set a default infoservice */
		try
		{
			InfoService defaultInfoService = new InfoService(JAPConstants.defaultInfoServiceHostName,
				JAPConstants.defaultInfoServicePortNumber);
			InfoServiceHolder.getInstance().setPreferedInfoService(defaultInfoService);
		}
		catch (Exception e)
		{
			JAPDebug.out(JAPDebug.EMERG, JAPDebug.NET, "JAPController: Constructor: " + e.getMessage());
		}

		mixCascadeDatabase = new Vector();
		m_proxyDirect = null;
		m_proxyAnon = null;
		m_Locale = Locale.getDefault();
		/* set JAPDebug as Log implementation in LogHolder -> Log of AnonLib visible */
		LogHolder.getInstance().setLogInstance(JAPDebug.create());
	}

	/** Creates the Controller - as Singleton.
	 *  @return The one and only JAPController
	 */
	public static JAPController create()
	{
		if (m_Controller == null)
		{
			m_Controller = new JAPController();
		}
		return m_Controller;
	}

	public static JAPController getController()
	{
		return m_Controller;
	}

	//---------------------------------------------------------------------
	public void initialRun()
	{
		JAPDebug.out(JAPDebug.INFO, JAPDebug.MISC, "JAPModel:initial run of JAP...");
		// start http listener object
		if (!startHTTPListener())
		{ // start was not sucessful
			Object[] args =
				{
				new Integer(JAPModel.getHttpListenerPortNumber())};
			String msg = MessageFormat.format(JAPMessages.getString("errorListenerPort"), args);
			// output error message
			JOptionPane.showMessageDialog(m_Controller.getView(),
										  msg,
										  JAPMessages.getString("errorListenerPortTitle"),
										  JOptionPane.ERROR_MESSAGE);
			JAPDebug.out(JAPDebug.EMERG, JAPDebug.NET, "Cannot start listener!");
			m_Controller.status1 = JAPMessages.getString("statusCannotStartListener");
			m_Controller.getView().disableSetAnonMode();
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
	 * First tries to read the configuration file in the users home directory
	 * and then in the JAP install directory.
	 * The configuration is a XML-File with the following structure:
	 *  <JAP
	 *     version="0.3"                  // version of the xml struct (DTD) used for saving the configuration
	 *    portNumber=""                  // Listener-Portnumber
	 *    portNumberSocks=""            // Listener-Portnumber for SOCKS
	 *    supportSocks=""                // Will we support SOCKS ?
	 *    listenerIsLocal="true"/"false"// Listener lauscht nur an localhost ?
	 *    proxyMode="true"/"false"      // Using a HTTP-Proxy??
	 *    proxyType="SOCKS"/"HTTP"      // which kind of proxy
	 *    proxyHostName="..."            // the Hostname of the Proxy
	 *    proxyPortNumber="..."          // port number of the Proxy
	 *    proxyAuthorization="true"/"false" //Need authorization to acces the proxy ?
	 *    porxyAuthUserID="..."         //UserId for the Proxy if Auth is neccessary
	 *    infoServiceHostName="..."      // hostname of the infoservice
	 *    infoServicePortnumber=".."    // the portnumber of the info service
	 *    infoServiceDisabled="true/false"    // disable use of InfoService
	 *    certCheckDisabled="true/false"  // disable checking of certificates
	 * 		biHost="..." // Host for BI
	 * 		biPort=".." //Port for BI
	 *    autoConnect="true"/"false"    // should we start the anon service immedialy after programm launch ?
	 *    autoReConnect="true"/"false"    // should we automatically reconnect to mix if connection was lost ?
	 *    DummyTrafficIntervall=".."    //Time of inactivity in milli seconds after which a dummy is send
	 *    minimizedStartup="true"/"false" // should we start minimized ???
	 *    neverRemindActiveContent="true"/"false" // should we remind the user about active content ?
	 *    Locale="LOCALE_IDENTIFIER" (two letter iso 639 code) //the Language for the UI to use
	 *    LookAndFeel="..."             //the LookAndFeel class name
	 *  >
	 * <MixCascade id=..">                     //info about the used AnonServer (since version 0.1) [equal to the general MixCascade struct]
	 *   <Name>..</Name>
	 *   <Network>
	 *     <ListenerInterfaces>
	 *       <ListenerInterface> ... </ListenerInterface>
	 *     </ListenerInterfaces>
	 *   </Network>
	 * </MixCascade>
	 * <GUI> //since version 0.2 --> store the position and size of JAP on the Desktop
	 *     <MainWindow> //for the Main Window
	 *       <SetOnStartup>"true/false"</SetOnStartup> //remember Position ?
	 *       <Location x=".." y=".."> //Location of the upper left corner
	 *       <Size dx=".." dy=.."> //Size of the Main window
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
	 *    <Output>..</Output>            //the kind of Output, at the moment only: Console
	 *   </Debug>
	 *   <InfoServices>                                           // info about all known infoservices (since config version 0.3)
	 *     <InfoService id="...">...</InfoService>                // the same format as from infoservice, without signature, if expired, it is removed from infoservice list
	 *     <InfoService id="...">...</InfoService>
	 *   </InfoServices>
	 *   <PreferedInfoService>                                    // info about the prefered infoservice, only one infoservice is supported here (since config version 0.3)
	 *     <InfoService id="...">...</InfoService>                // the same format as from infoservice, without signature, expire time does not matter
	 *   </PreferedInfoService>
	 *  </JAP>


	 *  @param strJapConfFile - file containing the Configuration. If null $(user.home)/jap.conf or ./jap.conf is used.
	 */
	public synchronized void loadConfigFile(String strJapConfFile)
	{
		// Load config from xml file
		JAPDebug.out(JAPDebug.INFO, JAPDebug.MISC,
					 "JAPModel:try loading configuration from " + JAPConstants.XMLCONFFN);
		FileInputStream f = null;
		if (strJapConfFile == null)
		{
			try
			{
				String dir = System.getProperty("user.home", "");
				//first tries in user.home
				try
				{
					f = new FileInputStream(dir + "/" + JAPConstants.XMLCONFFN);
				}
				catch (Exception e)
				{
					f = new FileInputStream(JAPConstants.XMLCONFFN); //and then in the current directory
				}
			}
			catch (Exception e2)
			{
				JAPDebug.out(JAPDebug.INFO, JAPDebug.MISC,
							 "JAPModel:Error loading configuration! " + e2.toString());
			}
		}
		else
		{
			try
			{
				f = new FileInputStream(strJapConfFile);
			}
			catch (Exception e2)
			{
				JAPDebug.out(JAPDebug.INFO, JAPDebug.MISC,
							 "JAPModel:Error loading configuration! " + e2.toString());
			}
		}
		if (f != null)
		{
			try
			{
				Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(f);
				Element root = doc.getDocumentElement();
				NamedNodeMap n = root.getAttributes();
				//
				String strVersion = XMLUtil.parseNodeString(n.getNamedItem("version"), null);
				int port = XMLUtil.parseElementAttrInt(root, "portNumber", JAPModel.getHttpListenerPortNumber());
				boolean bListenerIsLocal = XMLUtil.parseNodeBoolean(n.getNamedItem("listenerIsLocal"), true);
				setHTTPListener(port, bListenerIsLocal, false);
				//portSocksListener=JAPUtil.parseElementAttrInt(root,"portNumberSocks",portSocksListener);
				//setUseSocksPort(JAPUtil.parseNodeBoolean(n.getNamedItem("supportSocks"),false));
				//setUseProxy(JAPUtil.parseNodeBoolean(n.getNamedItem("proxyMode"),false));
				setUseFirewallAuthorization(XMLUtil.parseNodeBoolean(n.getNamedItem("proxyAuthorization"), false));
				// load settings for the reminder message in setAnonMode
				mbActCntMessageNeverRemind = XMLUtil.parseNodeBoolean(n.getNamedItem(
					"neverRemindActiveContent"), false);
				mbDoNotAbuseReminder = XMLUtil.parseNodeBoolean(n.getNamedItem("doNotAbuseReminder"), false);
				if (mbActCntMessageNeverRemind && mbDoNotAbuseReminder)
				{
					mbActCntMessageNotRemind = true;
					// load settings for the reminder message before goodBye
				}
				mbGoodByMessageNeverRemind = XMLUtil.parseNodeBoolean(n.getNamedItem("neverRemindGoodBye"), false);
				// load settings for Info Service
				setInfoServiceDisabled(XMLUtil.parseNodeBoolean(n.getNamedItem("infoServiceDisabled"), false));
				//settings for Certificates
				setCertCheckDisabled(XMLUtil.parseNodeBoolean(n.getNamedItem("certCheckDisabled"),
					JAPModel.isCertCheckDisabled()));

				try
				{
					// setCertificateStore(XMLUtil.parseNodeString(n.getNamedItem("acceptedCertList"), ""));
					// JAPCertificateStore jcs = null;
					Element xmlCA = (Element) XMLUtil.getFirstChildByName(root, "CertificateAuthorities");
					if (xmlCA != null)
					{
						NodeList nlX509Certs = xmlCA.getElementsByTagName("X509Certificate");
						if (
							(nlX509Certs != null) &&
							(nlX509Certs.getLength() >= 1)
							)
						{
							System.out.println("<CA> gefunden ...");
							// certificate store found in jap.conf
							System.out.println("<X509Cert> gefunden ...");
							JAPCertificateStore jcs = new JAPCertificateStore(nlX509Certs);
							try
							{
								setCertificateStore(jcs);
							}
							catch (Exception e)
							{
								System.out.println("fehler beim setzen certstore!");
							}

						}
					}
					else
					{
						System.out.println("ok, CA aber kein NL oder kein CA");
						JAPCertificateStore jcs = new JAPCertificateStore(JAPConstants.CERTSPATH +
							JAPConstants.TRUSTEDROOTCERT);
						setCertificateStore(jcs);
					}
				}
				catch (NullPointerException ex_np)
				{
					//
				}
				catch (Exception e)
				{
					System.out.println("load config file");
					e.printStackTrace();
				}
				//load settings for Payment
				setBIHost(XMLUtil.parseNodeString(n.getNamedItem("biHost"), JAPModel.getBIHost()));
				setBIPort(XMLUtil.parseElementAttrInt(root, "biPort", JAPModel.getBIPort()));

				// load settings for proxy
				String proxyHost = XMLUtil.parseNodeString(n.getNamedItem("proxyHostName"),
					m_Model.getFirewallHost());
				int proxyPort = XMLUtil.parseElementAttrInt(root, "proxyPortNumber", m_Model.getFirewallPort());
				boolean bUseProxy = XMLUtil.parseNodeBoolean(n.getNamedItem("proxyMode"), false);
				String type = XMLUtil.parseNodeString(n.getNamedItem("proxyType"), "HTTP");
				if (type.equalsIgnoreCase("SOCKS"))
				{
					setProxy(JAPConstants.FIREWALL_TYPE_SOCKS, proxyHost, proxyPort, bUseProxy);
				}
				else
				{
					setProxy(JAPConstants.FIREWALL_TYPE_HTTP, proxyHost, proxyPort, bUseProxy);
				}
				String proxyUserId = XMLUtil.parseNodeString(n.getNamedItem("proxyAuthUserID"),
					JAPModel.getFirewallAuthUserID());
				setFirewallAuthUserID(proxyUserId);

				/* try to get the info from the MixCascade node */
				MixCascade defaultMixCascade = null;
				Node mixCascadeNode = XMLUtil.getFirstChildByName(root, "MixCascade");
				try
				{
					defaultMixCascade = new MixCascade( (Element) mixCascadeNode);
				}
				catch (Exception e)
				{
					/* take the current mixcascade as the default */
					defaultMixCascade = getCurrentMixCascade();
				}
				setCurrentMixCascade(defaultMixCascade);

				setDummyTraffic(XMLUtil.parseElementAttrInt(root, "DummyTrafficIntervall", -1));
				setAutoConnect(XMLUtil.parseNodeBoolean(n.getNamedItem("autoConnect"), false));
				setAutoReConnect(XMLUtil.parseNodeBoolean(n.getNamedItem("autoReConnect"), false));
				m_Model.setMinimizeOnStartup(XMLUtil.parseNodeBoolean(n.getNamedItem("minimizedStartup"), false));
				//Load Locale-Settings
				String strLocale = XMLUtil.parseNodeString(n.getNamedItem("Locale"), m_Locale.getLanguage());
				Locale locale = new Locale(strLocale, "");
				setLocale(locale);
				//Load look-and-feel settings (not changed if SmmallDisplay!
				if (!m_Model.isSmallDisplay())
				{
					String lf = XMLUtil.parseNodeString(n.getNamedItem("LookAndFeel"), "unknown");
					LookAndFeelInfo[] lfi = UIManager.getInstalledLookAndFeels();
					for (int i = 0; i < lfi.length; i++)
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
								JAPDebug.out(JAPDebug.WARNING, JAPDebug.GUI,
											 "JAPModel:Exception while setting look-and-feel");
							}
							break;
						}
					}
				}
				//Loading GUI Setting
				Element elemGUI = (Element) XMLUtil.getFirstChildByName(root, "GUI");
				if (elemGUI != null)
				{
					Element elemMainWindow = (Element) XMLUtil.getFirstChildByName(elemGUI, "MainWindow");
					if (elemMainWindow != null)
					{
						Element tmp = (Element) XMLUtil.getFirstChildByName(elemMainWindow, "SetOnStartup");
						m_Controller.setSaveMainWindowPosition(XMLUtil.parseNodeBoolean(tmp, false));
						tmp = (Element) XMLUtil.getFirstChildByName(elemMainWindow, "Location");
						Point p = new Point();
						p.x = XMLUtil.parseElementAttrInt(tmp, "x", -1);
						p.y = XMLUtil.parseElementAttrInt(tmp, "y", -1);
						Dimension d = new Dimension();
						tmp = (Element) XMLUtil.getFirstChildByName(elemMainWindow, "Size");
						d.width = XMLUtil.parseElementAttrInt(tmp, "dx", -1);
						d.height = XMLUtil.parseElementAttrInt(tmp, "dy", -1);
						m_Model.m_OldMainWindowLocation = p;
						m_Model.m_OldMainWindowSize = d;
					}
				}
				//Loading debug settings
				Element elemDebug = (Element) XMLUtil.getFirstChildByName(root, "Debug");
				if (elemDebug != null)
				{
					Element elemLevel = (Element) XMLUtil.getFirstChildByName(elemDebug, "Level");
					if (elemLevel != null)
					{
						JAPDebug.setDebugLevel(Integer.parseInt(elemLevel.getFirstChild().getNodeValue().trim()));
					}
					Element elemType = (Element) XMLUtil.getFirstChildByName(elemDebug, "Type");
					if (elemType != null)
					{
						int debugtype = JAPDebug.NUL;
						if (elemType.getAttribute("GUI").equals("true"))
						{
							debugtype += JAPDebug.GUI;
						}
						if (elemType.getAttribute("NET").equals("true"))
						{
							debugtype += JAPDebug.NET;
						}
						if (elemType.getAttribute("THREAD").equals("true"))
						{
							debugtype += JAPDebug.THREAD;
						}
						if (elemType.getAttribute("MISC").equals("true"))
						{
							debugtype += JAPDebug.MISC;
						}
						JAPDebug.setDebugType(debugtype);
					}
					Element elemOutput = (Element) XMLUtil.getFirstChildByName(elemDebug, "Output");
					if (elemOutput != null)
					{
						JAPDebug.showConsole(elemOutput.getFirstChild().getNodeValue().trim().
											 equalsIgnoreCase("Console"), m_View);
					}
				}

				/* loading infoservice settings */
				/* infoservice list */
				NodeList infoServicesNodes = root.getElementsByTagName("InfoServices");
				if (infoServicesNodes.getLength() > 0)
				{
					Element infoServicesNode = (Element) (infoServicesNodes.item(0));
					InfoServiceDatabase.getInstance().loadFromXml(infoServicesNode);
				}
				/* prefered infoservice */
				NodeList preferedInfoServiceNodes = root.getElementsByTagName("PreferedInfoService");
				if (preferedInfoServiceNodes.getLength() > 0)
				{
					Element preferedInfoServiceNode = (Element) (preferedInfoServiceNodes.item(0));
					NodeList infoServiceNodes = preferedInfoServiceNode.getElementsByTagName("InfoService");
					if (infoServiceNodes.getLength() > 0)
					{
						Element infoServiceNode = (Element) (infoServiceNodes.item(0));
						InfoService preferedInfoService = new InfoService(infoServiceNode);
						InfoServiceHolder.getInstance().setPreferedInfoService(preferedInfoService);
					}
				}

			}
			catch (Exception e)
			{
				JAPDebug.out(JAPDebug.INFO, JAPDebug.MISC,
							 "JAPModel:Error loading configuration! " + e.toString());
			}
		} //end if f!=null
		// fire event
		notifyJAPObservers();
	}

	public void saveConfigFile()
	{
		boolean error = false;
		JAPDebug.out(JAPDebug.INFO, JAPDebug.MISC,
					 "JAPModel:try saving configuration to " + JAPConstants.XMLCONFFN);
		try
		{
			String sb = getConfigurationAsXmlString();
			if (sb == null)
			{
				error = true;
			}
			else
			{
				String dir = System.getProperty("user.home", "");
				FileOutputStream f = null;
				//first tries in user.home
				try
				{
					f = new FileOutputStream(dir + "/" + JAPConstants.XMLCONFFN);
				}
				catch (Exception e)
				{
					f = new FileOutputStream(JAPConstants.XMLCONFFN); //and then in the current directory
				}
				f.write(sb.getBytes());
				f.flush();
				f.close();
			}
		}
		catch (Throwable e)
		{
			error = true;
		}
		if (error)
		{
			JAPDebug.out(JAPDebug.ERR, JAPDebug.MISC,
						 "JAPModel:error saving configuration to " + JAPConstants.XMLCONFFN);
			JOptionPane.showMessageDialog(m_Controller.getView(),
										  JAPMessages.getString("errorSavingConfig"),
										  JAPMessages.getString("errorSavingConfigTitle"),
										  JOptionPane.ERROR_MESSAGE);
		}
	}

	protected String getConfigurationAsXmlString()
	{
		// Save config to xml file
		// Achtung!! Fehler im Sun-XML --> NULL-Attributte koennen hinzugefuegt werden,
		// beim Abspeichern gibt es dann aber einen Fehler!
		try
		{
			Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
			Element e = doc.createElement("JAP");
			doc.appendChild(e);
			e.setAttribute("version", "0.4");
			//
			e.setAttribute("portNumber", Integer.toString(JAPModel.getHttpListenerPortNumber()));
			//e.setAttribute("portNumberSocks",Integer.toString(portSocksListener));
			//e.setAttribute("supportSocks",(getUseSocksPort()?"true":"false"));
			e.setAttribute("listenerIsLocal", (JAPModel.getHttpListenerIsLocal() ? "true" : "false"));
			e.setAttribute("proxyMode", (JAPModel.getUseFirewall() ? "true" : "false"));
			e.setAttribute("proxyType",
						   (JAPModel.getFirewallType() == JAPConstants.FIREWALL_TYPE_SOCKS ? "SOCKS" : "HTTP"));
			String tmpStr = m_Model.getFirewallHost();
			e.setAttribute("proxyHostName", ( (tmpStr == null) ? "" : tmpStr));
			int tmpInt = m_Model.getFirewallPort();
			e.setAttribute("proxyPortNumber", Integer.toString(tmpInt));
			e.setAttribute("proxyAuthorization", (JAPModel.getUseFirewallAuthorization() ? "true" : "false"));
			tmpStr = m_Model.getFirewallAuthUserID();
			e.setAttribute("proxyAuthUserID", ( (tmpStr == null) ? "" : tmpStr));
			e.setAttribute("biHost", JAPModel.getBIHost());
			e.setAttribute("biPort", Integer.toString(JAPModel.getBIPort()));

			e.setAttribute("infoServiceDisabled", (JAPModel.isInfoServiceDisabled() ? "true" : "false"));
			e.setAttribute("certCheckDisabled", (JAPModel.isCertCheckDisabled() ? "true" : "false"));
			e.setAttribute("DummyTrafficIntervall", Integer.toString(JAPModel.getDummyTraffic()));
			e.setAttribute("autoConnect", (JAPModel.getAutoConnect() ? "true" : "false"));
			e.setAttribute("autoReConnect", (JAPModel.getAutoReConnect() ? "true" : "false"));
			e.setAttribute("minimizedStartup", (JAPModel.getMinimizeOnStartup() ? "true" : "false"));
			e.setAttribute("neverRemindActiveContent", (mbActCntMessageNeverRemind ? "true" : "false"));
			e.setAttribute("doNotAbuseReminder", (mbDoNotAbuseReminder ? "true" : "false"));
			e.setAttribute("neverRemindGoodBye", (mbGoodByMessageNeverRemind ? "true" : "false"));
			e.setAttribute("Locale", m_Locale.getLanguage());
			e.setAttribute("LookAndFeel", UIManager.getLookAndFeel().getClass().getName());

			/* store the trusted CAs */
			try
			{
				JAPCertificateStore jcs = JAPModel.getCertificateStore();
				// System.out.println("jcs : " + jcs.size());
				e.appendChild(jcs.toXmlNode(doc));
			}
			catch (Exception ex)
			{
				ex.printStackTrace();
			}
			/* store the current MixCascade */
			MixCascade defaultMixCascade = getCurrentMixCascade();
			if (defaultMixCascade != null)
			{
				e.appendChild(defaultMixCascade.toXmlNode(doc));
			}

			// adding GUI-Element
			if (JAPModel.getSaveMainWindowPosition())
			{
				Element elemGUI = doc.createElement("GUI");
				e.appendChild(elemGUI);
				Element elemMainWindow = doc.createElement("MainWindow");
				elemGUI.appendChild(elemMainWindow);
				Element tmp = doc.createElement("SetOnStartup");
				elemMainWindow.appendChild(tmp);
				XMLUtil.setNodeValue(tmp, "true");
				tmp = doc.createElement("Location");
				elemMainWindow.appendChild(tmp);
				Point p = m_View.getLocation();
				tmp.setAttribute("x", Integer.toString(p.x));
				tmp.setAttribute("y", Integer.toString(p.y));
				tmp = doc.createElement("Size");
				elemMainWindow.appendChild(tmp);
				Dimension d = m_View.getSize();
				tmp.setAttribute("dx", Integer.toString(d.width));
				tmp.setAttribute("dy", Integer.toString(d.height));
			}
			// adding Debug-Element
			Element elemDebug = doc.createElement("Debug");
			e.appendChild(elemDebug);
			Element tmp = doc.createElement("Level");
			Text txt = doc.createTextNode(Integer.toString(JAPDebug.getDebugLevel()));
			tmp.appendChild(txt);
			elemDebug.appendChild(tmp);
			tmp = doc.createElement("Type");
			int debugtype = JAPDebug.getDebugType();
			tmp.setAttribute("GUI", (debugtype & JAPDebug.GUI) != 0 ? "true" : "false");
			tmp.setAttribute("NET", (debugtype & JAPDebug.NET) != 0 ? "true" : "false");
			tmp.setAttribute("THREAD", (debugtype & JAPDebug.THREAD) != 0 ? "true" : "false");
			tmp.setAttribute("MISC", (debugtype & JAPDebug.MISC) != 0 ? "true" : "false");
			elemDebug.appendChild(tmp);
			if (JAPDebug.isShowConsole())
			{
				tmp = doc.createElement("Output");
				txt = doc.createTextNode("Console");
				tmp.appendChild(txt);
				elemDebug.appendChild(tmp);
			}
			/* adding infoservice settings */
			/* infoservice list */
			e.appendChild(InfoServiceDatabase.getInstance().toXmlNode(doc));
			/* prefered infoservice */
			InfoService preferedInfoService = InfoServiceHolder.getInstance().getPreferedInfoService();
			Element preferedInfoServiceNode = doc.createElement("PreferedInfoService");
			if (preferedInfoService != null)
			{
				preferedInfoServiceNode.appendChild(preferedInfoService.toXmlNode(doc));
			}
			e.appendChild(preferedInfoServiceNode);
			return JAPUtil.XMLDocumentToString(doc);
			//((XmlDocument)doc).write(f);
		}
		catch (Throwable ex)
		{
			JAPDebug.out(JAPDebug.EXCEPTION, JAPDebug.MISC, "JAPModel:save() Exception: " + ex.getMessage());
		}
		return null;
	}

	//---------------------------------------------------------------------
	public Locale getLocale()
	{
		return m_Locale;
	}

	public void setLocale(Locale l)
	{
		if (l == null)
		{
			return;
		}
		if (m_Locale != null && m_Locale.equals(l))
		{
			return;
		}
		JAPMessages.init(l);
		m_Locale = l;
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

	/**
	 * Changes the active MixCascade.
	 *
	 * @param newMixCascade The MixCascade which is activated.
	 */
	public void setCurrentMixCascade(MixCascade newMixCascade)
	{
		if (newMixCascade != null)
		{
			synchronized (this)
			{
				/* we need consistent states */
				if ( (getAnonMode() == true) && (currentMixCascade != null) &&
					(!currentMixCascade.getId().equals(newMixCascade.getId())))
				{
					/* we are running in anonymity mode */
					setAnonMode(false);
					currentMixCascade = newMixCascade;
					JAPDebug.out(JAPDebug.DEBUG, JAPDebug.MISC,
								 "JAPController: setCurrentMixCascade: MixCascade changed while in anonymity mode.");
					setAnonMode(true);
				}
				else
				{
					currentMixCascade = newMixCascade;
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
			return currentMixCascade;
		}
	}

	public Vector getMixCascadeDatabase()
	{
		return mixCascadeDatabase;
	}

	//---------------------------------------------------------------------
	//May be the host and port changed - or if we should us it or not
	// or both....
	public boolean setProxy(int type, String host, int port, boolean bUseProxy)
	{
		if (bUseProxy)
		{ //only if we want to use this proxy....
			if (!JAPUtil.isPort(port))
			{
				return false;
			}
			if (host == null)
			{
				return false;
			}
		}
		if (host == null)
		{
			host = "";
			// check if values have changed
		}
		if ( (type != m_Model.getFirewallType()) || (bUseProxy != m_Model.getUseFirewall()) ||
			(!host.equals(m_Model.getFirewallHost())) || (port != m_Model.getFirewallPort()))
		{
			// reset firewall authentication password
			m_Model.setFirewallAuthPasswd(null);
			// change settings
			synchronized (this)
			{
				m_Model.setFirewallHost(host);
				m_Model.setFirewallPort(port);
				m_Model.setUseFirewall(bUseProxy);
				m_Model.setFirewallType(type);
				applyProxySettingsToInfoService();
				applyProxySettingsToAnonService();
			}
			notifyJAPObservers();
		}
		return true;
	}

	private void applyProxySettingsToInfoService()
	{
		if (JAPModel.getUseFirewall())
		{
			if (JAPModel.getUseFirewallAuthorization())
			{
				HTTPConnectionFactory.getInstance().setNewProxySettings(m_Model.getFirewallType(),
					m_Model.getFirewallHost(), m_Model.getFirewallPort(), JAPModel.getFirewallAuthUserID(),
					getFirewallAuthPasswd());
			}
			else
			{
				HTTPConnectionFactory.getInstance().setNewProxySettings(m_Model.getFirewallType(),
					m_Model.getFirewallHost(), m_Model.getFirewallPort(), null, null);
			}
		}
		else
		{
			//no Proxy should be used....
			HTTPConnectionFactory.getInstance().setNewProxySettings(HTTPConnectionFactory.PROXY_TYPE_NONE, null,
				-1, null, null);
		}
	}

	private void applyProxySettingsToAnonService()
	{
		if (JAPModel.getUseFirewall() && getAnonMode())
		{
			// anon service is running
			Object[] options =
				{
				JAPMessages.getString("later"), JAPMessages.getString("reconnect")};
			int ret = JOptionPane.showOptionDialog(m_Controller.getView(),
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

	public void setFirewallAuthUserID(String userid)
	{
		// check if values have changed
		if (!userid.equals(JAPModel.getFirewallAuthUserID()))
		{
			m_Model.setFirewallAuthPasswd(null); // reset firewall authentication password
			m_Model.setFirewallAuthUserID(userid); // change settings
		}
	}

	public void setUseFirewallAuthorization(boolean b)
	{
		m_Model.setUseFirewallAuthorization(b);
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

	public static String getFirewallAuthPasswd()
	{
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
		}
	}

	//---------------------------------------------------------------------
	/*  public boolean setInfoService(String host,int port)
	  {
	   if(!JAPUtil.isPort(port))
	 return false;
	   if(host==null)
	 return false;
	   synchronized(this)
	 {
	  if(m_InfoService!=null)
	   this.getInfoService().setInfoService(host,port); //todo Error check
	  m_Model.setInfoServiceHost(host);
	  m_Model.setInfoServicePort(port);
	  notifyJAPObservers();
	  return true;
	 }
	  }

	 public static InfoService getInfoService()
	  {
	   if(m_Controller.m_InfoService==null)
	 {
	 m_Controller.m_InfoService=new InfoService(JAPModel.getInfoServiceHost(),JAPModel.getInfoServicePort());
	  m_Controller.m_InfoService.setLogging(JAPDebug.create());
	  m_Controller.applyProxySettingsToInfoService();
	 }
	  return m_Controller.m_InfoService;
	 }*/

	public static void setInfoServiceDisabled(boolean b)
	{
		m_Model.setInfoServiceDisabled(b);
	}

	public void setCertCheckDisabled(boolean b)
	{
		m_Model.setCertCheckDisabled(b);
		if (m_proxyAnon != null)
		{
			m_proxyAnon.setMixCertificationCheck(!b, m_Model.getCertificateStore());
		}
	}

	public static void setCertificateStore(JAPCertificateStore jcs)
	{
		m_Model.setCertificateStore(jcs);
	}

	public static void setSaveMainWindowPosition(boolean b)
	{
		m_Model.setSaveMainWindowPosition(b);
	}

	//---------------------------------------------------------------------
	/*
	 public void setSocksPortNumber (int p)
	  {
	   portSocksListener = p;
	  }
	 public int getSocksPortNumber()
	  {
	   return portSocksListener;
	  }
	 public void setUseSocksPort(boolean b)
	  {
	   mbSocksListener=b;
	  }
	 public boolean getUseSocksPort()
	  {
	   return mbSocksListener;
	  }
	 */

	//---------------------------------------------------------------------
	//---------------------------------------------------------------------
	//---------------------------------------------------------------------
	private final class SetAnonModeAsync
		implements Runnable
	{
		private boolean anonModeSelected = false;
		public SetAnonModeAsync(boolean b)
		{
			anonModeSelected = b;
		}

		public void run()
		{
			synchronized (oSetAnonModeSyncObject)
			{
				JAPWaitSplash splash = null;
				boolean canStartService = true;
				//setAnonMode--> async!!
				JAPDebug.out(JAPDebug.DEBUG, JAPDebug.MISC, "JAPModel:setAnonMode(" + anonModeSelected + ")");
				if ( (m_proxyAnon == null) && (anonModeSelected == true))
				{ //start Anon Mode
					m_View.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
					splash = JAPWaitSplash.start(JAPMessages.getString("setAnonModeSplashConnect"),
												 JAPMessages.getString("setAnonModeSplashTitle"));
					if (m_bAlreadyCheckedForNewVersion == false && !JAPModel.isInfoServiceDisabled())
					{
						// Check for a new Version of JAP if not already done
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
					if (!canStartService)
					{
						m_View.setCursor(Cursor.getDefaultCursor());
						splash.abort();
						return;
					}
					// starting MUX --> Success ???
					m_proxyAnon = new AnonProxy(m_socketHTTPListener);
					MixCascade currentMixCascade = m_Controller.getCurrentMixCascade();
					m_proxyAnon.setMixCascade(currentMixCascade);
					if (JAPModel.getUseFirewall())
					{
						//try all possible ListenerInterfaces...
						m_proxyAnon.setFirewall(m_Model.getFirewallType(), m_Model.getFirewallHost(),
												m_Model.getFirewallPort());
						if (JAPModel.getUseFirewallAuthorization())
						{
							m_proxyAnon.setFirewallAuthorization(JAPModel.getFirewallAuthUserID(),
								m_Controller.getFirewallAuthPasswd());
						}
					}

					m_proxyAnon.setMixCertificationCheck(!m_Model.isCertCheckDisabled(),
						m_Model.getCertificateStore());
					// -> we can try to start anonymity
					if (m_proxyDirect != null)
					{
						m_proxyDirect.stopService();
					}
					m_proxyDirect = null;

					int ret = m_proxyAnon.start();
					if (ret == AnonProxy.E_SUCCESS)
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
							ret = JOptionPane.showOptionDialog(m_Controller.getView(),
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
						m_proxyAnon.setProxyListener(m_Controller);
						m_proxyAnon.setDummyTraffic(m_Model.getDummyTraffic());
						m_proxyAnon.setAutoReConnect(m_Model.getAutoReConnect());
						// start feedback thread
						feedback = new JAPFeedback();
						feedback.startRequests();
						m_View.setCursor(Cursor.getDefaultCursor());
						notifyJAPObservers();
						splash.abort();
						return;
					}
					else if (ret == AnonProxy.E_BIND)
					{
						Object[] args =
							{
							new Integer(JAPModel.getHttpListenerPortNumber())};
						String msg = MessageFormat.format(JAPMessages.getString("errorListenerPort"), args);
						JOptionPane.showMessageDialog(m_Controller.getView(),
							msg,
							JAPMessages.getString("errorListenerPortTitle"),
							JOptionPane.ERROR_MESSAGE);
						JAPDebug.out(JAPDebug.EMERG, JAPDebug.NET, "Listener could not be started!");
						m_Controller.getView().disableSetAnonMode();
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
					// ootte
					else if (ret == AnonProxy.E_INVALID_KEY)
					{
						JOptionPane.showMessageDialog
							(
							getView(),
							JAPMessages.getString("errorMixInvalidKey"),
							JAPMessages.getString("errorMixInvalidTitle"),
							JOptionPane.ERROR_MESSAGE
							);
					}

					else if (ret == AnonProxy.E_INVALID_CERTIFICATE)
					{
						JOptionPane.showMessageDialog
							(
							getView(),
							JAPMessages.getString("errorCertificateInvalid"),
							JAPMessages.getString("errorCertificateInvalidTitle"),
							JOptionPane.ERROR_MESSAGE
							);
					}

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
					m_proxyAnon = null;
					m_View.setCursor(Cursor.getDefaultCursor());
					notifyJAPObservers();
					splash.abort();
					setAnonMode(false);
				}
				else if ( (m_proxyDirect == null) && (anonModeSelected == false))
				{
					if (m_proxyAnon != null)
					{
						splash = JAPWaitSplash.start(JAPMessages.getString("setAnonModeSplashDisconnect"),
							JAPMessages.getString("setAnonModeSplashTitle"));
						m_proxyAnon.stop();
					}
					m_proxyAnon = null;
					if (feedback != null)
					{
						feedback.stopRequests();
						feedback = null;
					}
					m_proxyDirect = new DirectProxy(m_socketHTTPListener);
					m_proxyDirect.startService();

					getCurrentMixCascade().resetCurrentStatus();
					notifyJAPObservers();
					if (splash != null)
					{
						splash.abort();
					}
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
		Thread t = new Thread(new SetAnonModeAsync(anonModeSelected), "JAP - SetAnonModeAsync");
		t.start();
	}

	public void setDummyTraffic(int msIntervall)
	{
		m_Model.setDummyTraffic(msIntervall);
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
		if ( (JAPModel.getHttpListenerPortNumber() != port) || (JAPModel.getHttpListenerIsLocal() != isLocal))
		{
			m_Model.setHttpListenerPortNumber(port);
			synchronized (this)
			{
				m_Model.setHttpListenerIsLocal(isLocal);
			}
			JAPDebug.out(JAPDebug.DEBUG, JAPDebug.MISC, "JAPModel:HTTP listener settings changed");
			if (bShowWarning)
			{
				JOptionPane.showMessageDialog(m_Controller.getView(),
											  JAPMessages.getString("confmessageListernPortChanged"));
			}
			m_Controller.notifyJAPObservers();
		}
	}

	public static void setBIHost(String host)
	{
		m_Model.setBIHost(host);
		m_Controller.notifyJAPObservers();
	}

	public static void setBIPort(int port)
	{
		m_Model.setBIPort(port);
		m_Controller.notifyJAPObservers();
	}

	//---------------------------------------------------------------------
	private boolean startHTTPListener()
	{
		JAPDebug.out(JAPDebug.DEBUG, JAPDebug.MISC, "JAPModel:startListener");
		if (isRunningListener == false)
		{
			boolean bindOk = false;
			for (int i = 0; i < 10; i++) //HAck for Mac!!
			{
				try
				{
					if (JAPModel.getHttpListenerIsLocal())
					{
						//InetAddress[] a=InetAddress.getAllByName("localhost");
						InetAddress[] a = InetAddress.getAllByName("127.0.0.1");
						JAPDebug.out(JAPDebug.DEBUG, JAPDebug.NET,
									 "Try binding Listener on localhost: " + a[0]);
						m_socketHTTPListener = new ServerSocket(JAPModel.getHttpListenerPortNumber(), 50, a[0]);
					}
					else
					{
						m_socketHTTPListener = new ServerSocket(JAPModel.getHttpListenerPortNumber());
					}
					JAPDebug.out(JAPDebug.INFO, JAPDebug.NET,
								 "Listener on port " + JAPModel.getHttpListenerPortNumber() + " started.");
					try
					{
						m_socketHTTPListener.setSoTimeout(2000);
					}
					catch (Exception e1)
					{
						JAPDebug.out(JAPDebug.DEBUG, JAPDebug.NET,
									 "Could not set listener accept timeout: Exception: " + e1.getMessage());
					}
					bindOk = true;
					break;
				}
				catch (Exception e)
				{
					JAPDebug.out(JAPDebug.DEBUG, JAPDebug.NET, "Exception: " + e.getMessage());
					m_socketHTTPListener = null;
				}
			}
			isRunningListener = bindOk;
		}
		return isRunningListener;
	}

	private void stopHTTPListener()
	{
		JAPDebug.out(JAPDebug.DEBUG, JAPDebug.MISC, "JAPModel:stopListener");
		if (isRunningListener)
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
			isRunningListener = false;
		}
	}

	//---------------------------------------------------------------------

	/** This (and only this) is the final exit procedure of JAP!
	 *
	 */
	public void goodBye()
	{
		try
		{
			//stopListener();
			//view.setVisible(false);
			//iconifiedView.setVisible(false);
			// show a Reminder message that active contents should be disabled
			Object[] options =
				{
				JAPMessages.getString("okButton")};
			JCheckBox checkboxRemindNever = new JCheckBox(JAPMessages.getString(
				"disableGoodByMessageNeverRemind"));
			Object[] message =
				{
				JAPMessages.getString("disableGoodByMessage"), checkboxRemindNever};
			if (!mbGoodByMessageNeverRemind)
			{
				JOptionPane.showOptionDialog(m_Controller.getView(),
											 (Object) message,
											 JAPMessages.getString("disableGoodByMessageTitle"),
											 JOptionPane.DEFAULT_OPTION,
											 JOptionPane.WARNING_MESSAGE,
											 null, options, options[0]);
				mbGoodByMessageNeverRemind = checkboxRemindNever.isSelected();
			}
			m_Controller.saveConfigFile();
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
	 * Get all available mixcascades from the infoservice and store it in the database.
	 */
	public void fetchMixCascades()
	{
		JAPDebug.out(JAPDebug.INFO, JAPDebug.MISC,
					 "JAPController: fetchMixCascades: Trying to fetch mixcascades from infoservice.");
		Vector newMixCascades = InfoServiceHolder.getInstance().getMixCascades();
		if (newMixCascades == null)
		{
			JAPDebug.out(JAPDebug.ERR, JAPDebug.NET,
						 "JAPController: fetchMixCascades: No connection to infoservices.");
			if (!JAPModel.isSmallDisplay())
			{
				JOptionPane.showMessageDialog(m_View, JAPMessages.getString("errorConnectingInfoService"),
											  JAPMessages.getString("errorConnectingInfoServiceTitle"),
											  JOptionPane.ERROR_MESSAGE);
			}
		}
		else
		{
			JAPDebug.out(JAPDebug.DEBUG, JAPDebug.NET, "JAPController: fetchMixCascades: success!");
			mixCascadeDatabase = newMixCascades;
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
		JAPDebug.out(JAPDebug.INFO, JAPDebug.MISC,
					 "JAPController: versionCheck: Checking for new version of JAP...");
		int result = 0;
		String currentVersionNumber = InfoServiceHolder.getInstance().getNewVersionNumber();
		if (currentVersionNumber != null)
		{
			currentVersionNumber = currentVersionNumber.trim();
			JAPDebug.out(JAPDebug.DEBUG, JAPDebug.MISC,
						 "JAPController: versionCheck: Local version: " + JAPConstants.aktVersion);
			if (currentVersionNumber.compareTo(JAPConstants.aktVersion) <= 0)
			{
				/* the local JAP version is up to date -> exit */
				return 0;
			}
			/* local version is not up to date, new version is available -> ask the user whether to
			 * download the new version or not
			 */
			int answer = JOptionPane.showConfirmDialog(m_View, JAPMessages.getString("newVersionAvailable"),
				JAPMessages.getString("newVersionAvailableTitle"),
				JOptionPane.YES_NO_OPTION);
			if (answer == JOptionPane.YES_OPTION)
			{
				/* User has elected to download new version of JAP -> Download, Alert, exit program */
				JAPVersionInfo vi = InfoServiceHolder.getInstance().getJAPVersionInfo(JAPVersionInfo.
					JAP_RELEASE_VERSION);
				if (vi != null)
				{
					JAPUpdateWizard wz = new JAPUpdateWizard(vi);
					/* we got the JAPVersionInfo from the infoservice */
					/* Assumption: If we are here, the download failed for some resaons, otherwise the
					 * program would quit
					 */
					//TODO: Do this in a better way!!
					if (wz.getStatus() != wz.UPDATESTATUS_SUCCESS)
					{
						/* Download failed -> alert, and reset anon mode to false */
						JAPDebug.out(JAPDebug.ERR, JAPDebug.MISC,
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
					goodBye();
					return 0;
				}
				/* update was not successful, because we could not get the JAPVersionInfo -> alert, and
				 * reset anon mode to false
				 */
				JAPDebug.out(JAPDebug.ERR, JAPDebug.MISC,
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
			JAPDebug.out(JAPDebug.ERR, JAPDebug.MISC,
						 "JAPController: versionCheck: Could not get the current JAP version number from infoservice.");
			JAPUtil.showMessageBox(m_View, "errorConnectingInfoService", "errorConnectingInfoServiceTitle",
								   JOptionPane.ERROR_MESSAGE);
			notifyJAPObservers();
			return -1;
		}
		/* this line should never be reached */
	}

	//---------------------------------------------------------------------
	public void registerMainView(JAPView v)
	{
		m_View = v;
	}

	public static JAPView getView()
	{
		return m_Controller.m_View;
	}

	//---------------------------------------------------------------------
	public void addJAPObserver(JAPObserver o)
	{
		observerVector.addElement(o);
	}

	public void notifyJAPObservers()
	{
		JAPDebug.out(JAPDebug.DEBUG, JAPDebug.MISC, "JAPModel:notifyJAPObservers()");
		synchronized (observerVector)
		{
			try
			{
				Enumeration enum = observerVector.elements();
				int i = 0;
				while (enum.hasMoreElements())
				{
					JAPObserver listener = (JAPObserver) enum.nextElement();
					JAPDebug.out(JAPDebug.DEBUG, JAPDebug.MISC, "JAPModel:notifyJAPObservers: " + i);
					listener.valuesChanged();
					i++;
				}
			}
			catch (Throwable t)
			{
				JAPDebug.out(JAPDebug.EMERG, JAPDebug.MISC,
							 "JAPModel:notifyJAPObservers - critical exception: " + t.getMessage());
			}
		}
		JAPDebug.out(JAPDebug.DEBUG, JAPDebug.MISC, "JAPModel:notifyJAPObservers()-ended");
	}

	//---------------------------------------------------------------------
	public void channelsChanged(int channels)
	{
		nrOfChannels = channels;
		Enumeration enum = observerVector.elements();
		while (enum.hasMoreElements())
		{
			JAPObserver listener = (JAPObserver) enum.nextElement();
			listener.channelsChanged(channels);
		}
	}

	public void transferedBytes(int bytes)
	{
		nrOfBytes += bytes;
		Enumeration enum = observerVector.elements();
		while (enum.hasMoreElements())
		{
			JAPObserver listener = (JAPObserver) enum.nextElement();
			listener.transferedBytes(nrOfBytes);
		}
	}

}
