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
package jap;

import java.text.MessageFormat;
import java.awt.Point;
import java.util.Enumeration;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.Vector;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Text;
import org.w3c.dom.NodeList;
import org.w3c.dom.Node;
import org.w3c.dom.NamedNodeMap;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.awt.Dimension;
import java.awt.Window;
import java.awt.Font;
import java.awt.Frame;
import java.awt.Cursor;
import java.awt.MediaTracker;
import java.awt.Toolkit;
import java.awt.event.ActionListener;
import java.awt.event.ActionEvent;
import java.net.ServerSocket;
import java.net.InetAddress;
import java.net.UnknownServiceException;
import javax.swing.ImageIcon;
import javax.swing.JCheckBox;
import javax.swing.JOptionPane;
import javax.swing.JDialog;
import javax.swing.JPanel;
import javax.swing.JButton;
import javax.swing.JPasswordField;
import javax.swing.JLabel;
import javax.swing.UIManager.LookAndFeelInfo;
import javax.swing.UIManager;

import anon.AnonServer;
import anon.ListenerInterface;
import anon.infoservice.InfoService;
import anon.infoservice.JAPVersionInfo;
import anon.server.impl.XMLUtil;
import proxy.ProxyListener;
import proxy.DirectProxy;
import proxy.AnonProxy;
import update.JAPUpdateWizard;
import anon.crypto.*;

/* This is the Model of All. It's a Singelton!*/
public final class JAPController implements ProxyListener {

	private JAPAnonServerDB  m_anonServerDatabase   = null; // vector of all available mix cascades (fetched from InfoService)
																												//never null, maybe empty
																												//m_currentAnonService may be a member or not
	private AnonServer      m_currentAnonService   = null; // current anon service data object
																											//This would NEVER be null, if JAP is running

	private ServerSocket    m_socketHTTPListener    = null; // listener object
	private DirectProxy  m_proxyDirect           = null; // service object for direct access (bypass anon service)
	private AnonProxy    m_proxyAnon             = null; // service object for anon access

	private boolean  isRunningListener           = false; // true if a listener is running
	//private boolean  canStartService             = false; // indicates if anon service can be started
	private boolean  m_bAlreadyCheckedForNewVersion = false; // indicates if check for new version has already been done
	private boolean  mbActCntMessageNotRemind    = false; // indicates if Warning message in setAnonMode has been deactivated for the session
	private boolean  mbActCntMessageNeverRemind  = false; // indicates if Warning message in setAnonMode has been deactivated forever
	private boolean  mbDoNotAbuseReminder        = false; // indicates if new warning message in setAnonMode (containing Do no abuse) has been shown
	private boolean  mbGoodByMessageNeverRemind  = false; // indicates if Warning message before exit has been deactivated forever

	private	static final Object oSetAnonModeSyncObject=new Object();

	public  String   status1           = " ";
	public  String   status2           = " ";

	private int      nrOfChannels      = 0;
	private int      nrOfBytes         = 0;

	private static  JAPView           m_View          = null;
	private static  InfoService    m_InfoService  = null;
	private static  JAPController     m_Controller         = null;
	private static  JAPModel          m_Model         = null;
	private static  JAPFeedback       feedback      = null;
	private Locale m_Locale=null;
	private Vector observerVector=null;

	private static Font m_fontControls;

	private JAPController ()
		{
			m_Model=JAPModel.create();
			// Create observer object
			observerVector = new Vector();
			try
				{
					m_currentAnonService = new AnonServer(JAPConstants.defaultAnonHost,JAPConstants.defaultAnonPortNumber);
				}
			catch(Exception e)
				{
					try
						{
							m_currentAnonService=new AnonServer(null,null,JAPConstants.defaultAnonHost,
																									JAPConstants.defaultAnonIP,
																									JAPConstants.defaultAnonPortNumber,-1);
						}
					catch(Exception e1)
						{
							JAPDebug.out(JAPDebug.NET,JAPDebug.EMERG,"Should NEVER be there: JAPController() exception: "+e1.getMessage());
						}
				}
			m_anonServerDatabase = new JAPAnonServerDB();
			m_proxyDirect=null;
			m_proxyAnon=null;
			m_Locale=Locale.getDefault();
		}

	/** Creates the Controller - as Singleton.
	 *  @return The one and only JAPController
	 */
	public static JAPController create()
		{
			if(m_Controller==null)
				m_Controller=new JAPController();
			return m_Controller;
		}

	public static JAPController getController()
		{
			return m_Controller;
		}

	//---------------------------------------------------------------------
	public void initialRun() {
		JAPDebug.out(JAPDebug.INFO,JAPDebug.MISC,"JAPModel:initial run of JAP...");
		// start http listener object
		if(!startHTTPListener())
			{	// start was not sucessful
				Object[] args={new Integer(JAPModel.getHttpListenerPortNumber())};
				String msg=MessageFormat.format(JAPMessages.getString("errorListenerPort"),args);
				// output error message
				JOptionPane.showMessageDialog(m_Controller.getView(),
										msg,
										JAPMessages.getString("errorListenerPortTitle"),
										JOptionPane.ERROR_MESSAGE);
				JAPDebug.out(JAPDebug.EMERG,JAPDebug.NET,"Cannot start listener!");
				m_Controller.status1 = JAPMessages.getString("statusCannotStartListener");
				m_Controller.getView().disableSetAnonMode();
				notifyJAPObservers();
			}
			else
				{	// listender has started correctly
					m_Controller.status1 = JAPMessages.getString("statusRunning");
					// read root certificate, if anything goes wrong: just shrug!
					m_Controller.m_Model.setRootCertificate();
					// initial setting of anonMode
					setAnonMode(JAPModel.getAutoConnect());
				}
	}
	//---------------------------------------------------------------------
	/** Loads the Configuration.
	 * First tries to read the configuration file in the users home directory
	 * and then in the JAP install directory.
	 * The configuration is a XML-File with the following structure:
	 *	<JAP
	 * 		version="0.2"									// version of the xml struct (DTD) used for saving the configuration
	 *		portNumber=""									// Listener-Portnumber
	 *		portNumberSocks=""						// Listener-Portnumber for SOCKS
	 *		supportSocks=""								// Will we support SOCKS ?
	 *    listenerIsLocal="true"/"false"// Listener lauscht nur an localhost ?
	 *		proxyMode="true"/"false"			// Using a HTTP-Proxy??
	 *    proxyType="SOCKS"/"HTTP"      // which kind of proxy
	 *		proxyHostName="..."						// the Hostname of the Proxy
	 *		proxyPortNumber="..."					// port number of the Proxy
	 *    proxyAuthorization="true"/"false" //Need authorization to acces the proxy ?
	 *    porxyAuthUserID="..."         //UserId for the Proxy if Auth is neccessary
	 *		infoServiceHostName="..."			// hostname of the infoservice
	 *		infoServicePortnumber=".."		// the portnumber of the info service
	 *		infoServiceDisabled="true/false"		// disable use of InfoService
	 *		certCheckDisabled="true/false"  // disable checking of certificates
	 * 		acceptedCertList="????"					//????
	 *  	anonserviceID=".."            //the Id of the anonService [since version 0.1 in a separate node]
	 *    anonserviceName=".."          //the name of the anon-service [since version 0.1 in a separate node]
	 *		anonHostName=".."							// the hostname of the anon-service [since version 0.1 in a separate node]
	 *		anonHostIP=".."							  // the ip of the anon-service [since version 0.1 in a separate node]
	 *		anonPortNumber=".."						// the portnumber of the anon-service [since version 0.1 in a separate node]
	 *    anonSSLPortNumber=".."        /the "proxy" port number of anon-service [since version 0.1 in a separate node]
	 *		autoConnect="true"/"false"		// should we start the anon service immedialy after programm launch ?
	 *		autoReConnect="true"/"false"		// should we automatically reconnect to mix if connection was lost ?
	 *		DummyTrafficIntervall=".."    //Time of inactivity in milli seconds after which a dummy is send
	 *    minimizedStartup="true"/"false" // should we start minimized ???
	 *		neverRemindActiveContent="true"/"false" // should we remind the user about active content ?
	 *    Locale="LOCALE_IDENTIFIER" (two letter iso 639 code) //the Language for the UI to use
	 *    LookAndFeel="..."             //the LookAndFeel class name
	 *	>
	 * <MixCascade id=..">                     //info about the used AnonServer (since version 0.1) [equal to the general MixCascade struct]
	 * 	<Name>..</Name>
	 * 	<Network>
	 * 		<ListenerInterfaces>
	 * 			<ListenerInterface> ... </ListenerInterface>
	 * 		</ListenerInterfaces>
	 * 	</Network>
	 * </MixCascade>
	 * <GUI> //since version 0.2 --> store the position and size of JAP on the Desktop
	 *	 	<MainWindow> //for the Main Window
	 * 			<SetOnStartup>"true/false"</SetOnStartup> //remember Position ?
	 * 			<Location x=".." y=".."> //Location of the upper left corner
	 * 			<Size dx=".." dy=.."> //Size of the Main window
	 * 		</MainWindow>
	 * </GUI>
	 * <Debug>													//info about debug output
	 *		<Level>..</Level>							// the amount of output (0 means less.. 7 means max)
	 *		<Type													// which type of messages should be logged
	 *			GUI="true"/"false"					// messages related to the user interface
	 *			NET="true"/"false"					// messages related to the network
	 *			THREAD="true"/"false"				// messages related to threads
	 *			MISC="true"/"false"					// all the others
	 *		>
	 *		</Type>
	 *		<Output>..</Output>						//the kind of Output, at the moment only: Console
	 * 	</Debug>
	 *	</JAP>
	 *  @param strJapConfFile - file containing the Configuration. If null $(user.home)/jap.conf or ./jap.conf is used.
	 */
	public synchronized void loadConfigFile(String strJapConfFile) {
		// Load config from xml file
		JAPDebug.out(JAPDebug.INFO,JAPDebug.MISC,"JAPModel:try loading configuration from "+JAPConstants.XMLCONFFN);
		FileInputStream f=null;
		if(strJapConfFile==null)
			{
				try
					{
						String dir=System.getProperty("user.home","");
						//first tries in user.home
						try
							{
								f=new FileInputStream(dir+"/"+JAPConstants.XMLCONFFN);
							}
						catch(Exception e)
							{
								f=new FileInputStream(JAPConstants.XMLCONFFN); //and then in the current directory
							}
						}
				catch(Exception e2)
					{
						JAPDebug.out(JAPDebug.INFO,JAPDebug.MISC,"JAPModel:Error loading configuration! "+e2.toString());
					}
			}
		else
			{
				try
					{
						 f=new FileInputStream(strJapConfFile);
					}
				catch(Exception e2)
					{
						JAPDebug.out(JAPDebug.INFO,JAPDebug.MISC,"JAPModel:Error loading configuration! "+e2.toString());
					}
			}
	 if(f!=null){
		try{
			Document doc=DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(f);
			Element root=doc.getDocumentElement();
			NamedNodeMap n=root.getAttributes();
			//
			String strVersion=XMLUtil.parseNodeString(n.getNamedItem("version"),null);
			int port=XMLUtil.parseElementAttrInt(root,"portNumber",JAPModel.getHttpListenerPortNumber());
			boolean bListenerIsLocal=XMLUtil.parseNodeBoolean(n.getNamedItem("listenerIsLocal"),true);
			setHTTPListener(port,bListenerIsLocal,false);
			//portSocksListener=JAPUtil.parseElementAttrInt(root,"portNumberSocks",portSocksListener);
			//setUseSocksPort(JAPUtil.parseNodeBoolean(n.getNamedItem("supportSocks"),false));
			//setUseProxy(JAPUtil.parseNodeBoolean(n.getNamedItem("proxyMode"),false));
			setUseFirewallAuthorization(XMLUtil.parseNodeBoolean(n.getNamedItem("proxyAuthorization"),false));
			// load settings for the reminder message in setAnonMode
			mbActCntMessageNeverRemind=XMLUtil.parseNodeBoolean(n.getNamedItem("neverRemindActiveContent"),false);
			mbDoNotAbuseReminder      =XMLUtil.parseNodeBoolean(n.getNamedItem("doNotAbuseReminder"),false);
			if(mbActCntMessageNeverRemind && mbDoNotAbuseReminder)
				mbActCntMessageNotRemind=true;
			// load settings for the reminder message before goodBye
			mbGoodByMessageNeverRemind=XMLUtil.parseNodeBoolean(n.getNamedItem("neverRemindGoodBye"),false);
			// load settings for Info Service
			String host;
			host=XMLUtil.parseNodeString(n.getNamedItem("infoServiceHostName"),JAPModel.getInfoServiceHost());
			port=XMLUtil.parseElementAttrInt(root,"infoServicePortNumber",JAPModel.getInfoServicePort());
			setInfoService(host,port);
			setInfoServiceDisabled(XMLUtil.parseNodeBoolean(n.getNamedItem("infoServiceDisabled"),false));
			//settings for Certificates
			setCertCheckDisabled(XMLUtil.parseNodeBoolean(n.getNamedItem("certCheckDisabled"),JAPModel.isCertCheckDisabled()));
			setCertificateStore(XMLUtil.parseNodeString(n.getNamedItem("acceptedCertList"), ""));


			// load settings for proxy
			host=XMLUtil.parseNodeString(n.getNamedItem("proxyHostName"),m_Model.getFirewallHost());
			port=XMLUtil.parseElementAttrInt(root,"proxyPortNumber",m_Model.getFirewallPort());
			if(host.equalsIgnoreCase("ikt.inf.tu-dresden.de"))
				host="";
			boolean bUseProxy=XMLUtil.parseNodeBoolean(n.getNamedItem("proxyMode"),false);
			String type=XMLUtil.parseNodeString(n.getNamedItem("proxyType"),"HTTP");
			if(type.equalsIgnoreCase("SOCKS"))
				setProxy(JAPConstants.FIREWALL_TYPE_SOCKS,host,port,bUseProxy);
			else
				setProxy(JAPConstants.FIREWALL_TYPE_HTTP,host,port,bUseProxy);
		 String userid=XMLUtil.parseNodeString(n.getNamedItem("proxyAuthUserID"),JAPModel.getFirewallAuthUserID());
			setFirewallAuthUserID(userid);

			AnonServer server=null;
			//Try to get AnonServer info from MixCascade node
			Node nodeMixCascade=XMLUtil.getFirstChildByName(root,"MixCascade");
			try
				{
					server=new AnonServer(nodeMixCascade);
				}
			catch(UnknownServiceException e)
				{
					//we could not load enough info for the saved AnonService
					server=null;
				}
			if(server==null)
				{
					String anonserviceId  = XMLUtil.parseNodeString(n.getNamedItem("anonserviceID"),null);
					String anonserviceName   = XMLUtil.parseNodeString(n.getNamedItem("anonserviceName"),null);
					String anonHostName      = XMLUtil.parseNodeString(n.getNamedItem("anonHostName"),null);
					String anonHostIP      = XMLUtil.parseNodeString(n.getNamedItem("anonHostIP"),null);
					int anonPortNumber    = XMLUtil.parseElementAttrInt(root,"anonPortNumber",-1);
					int anonSSLPortNumber = XMLUtil.parseElementAttrInt(root,"anonSSLPortNumber",-1);
					try
						{
							server=new AnonServer(anonserviceId,anonserviceName,anonHostName,anonHostIP,anonPortNumber,anonSSLPortNumber);
						}
					catch(UnknownServiceException e)
						{
							//we could not load enough info for the saved AnonService --> take current (default)
							server=m_Controller.getAnonServer();
						}
				}
			m_Controller.setAnonServer(server);
			setDummyTraffic(XMLUtil.parseElementAttrInt(root,"DummyTrafficIntervall",-1));
			setAutoConnect(XMLUtil.parseNodeBoolean(n.getNamedItem("autoConnect"),false));
			setAutoReConnect(XMLUtil.parseNodeBoolean(n.getNamedItem("autoReConnect"),false));
			m_Model.setMinimizeOnStartup(XMLUtil.parseNodeBoolean(n.getNamedItem("minimizedStartup"),false));
			//Load Locale-Settings
			String strLocale=XMLUtil.parseNodeString(n.getNamedItem("Locale"),m_Locale.getLanguage());
			Locale locale=new Locale(strLocale,"");
			setLocale(locale);
			//Load look-and-feel settings (not changed if SmmallDisplay!
			if(!m_Model.isSmallDisplay())
				{
				String lf=XMLUtil.parseNodeString(n.getNamedItem("LookAndFeel"),"unknown");
				LookAndFeelInfo[] lfi=UIManager.getInstalledLookAndFeels();
				for(int i=0;i<lfi.length;i++) {
					if(lfi[i].getName().equals(lf)||lfi[i].getClassName().equals(lf)) {
						try {
							UIManager.setLookAndFeel(lfi[i].getClassName());
	//				SwingUtilities.updateComponentTreeUI(m_frmParent);
	//				SwingUtilities.updateComponentTreeUI(SwingUtilities.getRoot(((JComboBox)e.getItemSelectable())));
						}
						catch(Exception lfe) {
							JAPDebug.out(JAPDebug.WARNING,JAPDebug.GUI,"JAPModel:Exception while setting look-and-feel");
						}
						break;
					}
				}
			}
				//Loading GUI Setting
			Element elemGUI=(Element)XMLUtil.getFirstChildByName(root,"GUI");
			if(elemGUI!=null)
				{
					Element elemMainWindow=(Element)XMLUtil.getFirstChildByName(elemGUI,"MainWindow");
					if(elemMainWindow!=null)
						{
							Element tmp=(Element)XMLUtil.getFirstChildByName(elemMainWindow,"SetOnStartup");
							m_Controller.setSaveMainWindowPosition(XMLUtil.parseNodeBoolean(tmp,false));
							tmp=(Element)XMLUtil.getFirstChildByName(elemMainWindow,"Location");
							Point p=new Point();
							p.x=XMLUtil.parseElementAttrInt(tmp,"x",-1);
							p.y=XMLUtil.parseElementAttrInt(tmp,"y",-1);
							Dimension d=new Dimension();
							tmp=(Element)XMLUtil.getFirstChildByName(elemMainWindow,"Size");
							d.width=XMLUtil.parseElementAttrInt(tmp,"dx",-1);
							d.height=XMLUtil.parseElementAttrInt(tmp,"dy",-1);
							m_Model.m_OldMainWindowLocation=p;
							m_Model.m_OldMainWindowSize=d;
						}
				}
				//Loading debug settings
			Element elemDebug=(Element)XMLUtil.getFirstChildByName(root,"Debug");
			if(elemDebug!=null)
				{
					Element elemLevel=(Element)XMLUtil.getFirstChildByName(elemDebug,"Level");
					if(elemLevel!=null)
						{
							JAPDebug.setDebugLevel(Integer.parseInt(elemLevel.getFirstChild().getNodeValue().trim()));
						}
					Element elemType=(Element)XMLUtil.getFirstChildByName(elemDebug,"Type");
					if(elemType!=null)
						{
							int debugtype=JAPDebug.NUL;
							if(elemType.getAttribute("GUI").equals("true"))
								debugtype+=JAPDebug.GUI;
							if(elemType.getAttribute("NET").equals("true"))
								debugtype+=JAPDebug.NET;
							if(elemType.getAttribute("THREAD").equals("true"))
								debugtype+=JAPDebug.THREAD;
							if(elemType.getAttribute("MISC").equals("true"))
								debugtype+=JAPDebug.MISC;
							JAPDebug.setDebugType(debugtype);
						}
					Element elemOutput=(Element)XMLUtil.getFirstChildByName(elemDebug,"Output");
					if(elemOutput!=null)
						{
							JAPDebug.showConsole(elemOutput.getFirstChild().getNodeValue().trim().equalsIgnoreCase("Console"),m_View);
						}
				}
		}
		catch(Exception e) {
			JAPDebug.out(JAPDebug.INFO,JAPDebug.MISC,"JAPModel:Error loading configuration! "+e.toString());
		}
		}//end if f!=null
		// fire event
		notifyJAPObservers();
	}
	public void saveConfigFile()
		{
			boolean error=false;
			JAPDebug.out(JAPDebug.INFO,JAPDebug.MISC,"JAPModel:try saving configuration to "+JAPConstants.XMLCONFFN);
			try
				{
					String sb=getConfigurationAsXmlString();
					if(sb==null)
						error=true;
					else
						{
							String dir=System.getProperty("user.home","");
							FileOutputStream f=null;
							//first tries in user.home
							try
								{
									f=new FileOutputStream(dir+"/"+JAPConstants.XMLCONFFN);
								}
							catch(Exception e)
								{
									f=new FileOutputStream(JAPConstants.XMLCONFFN); //and then in the current directory
								}
							f.write(sb.getBytes());
							f.flush();
							f.close();
						}
				}
			catch(Throwable e)
				{
					error=true;
				}
			if(error)
				{
					JAPDebug.out(JAPDebug.ERR,JAPDebug.MISC,"JAPModel:error saving configuration to "+JAPConstants.XMLCONFFN);
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
		try {
			Document doc=DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
			Element e=doc.createElement("JAP");
			doc.appendChild(e);
			e.setAttribute("version","0.1");
			//
			e.setAttribute("portNumber",Integer.toString(JAPModel.getHttpListenerPortNumber()));
			//e.setAttribute("portNumberSocks",Integer.toString(portSocksListener));
			//e.setAttribute("supportSocks",(getUseSocksPort()?"true":"false"));
			e.setAttribute("listenerIsLocal",(JAPModel.getHttpListenerIsLocal()?"true":"false"));
			e.setAttribute("proxyMode",(JAPModel.getUseFirewall()?"true":"false"));
			e.setAttribute("proxyType",(JAPModel.getFirewallType()==JAPConstants.FIREWALL_TYPE_SOCKS?"SOCKS":"HTTP"));
			String tmpStr=m_Model.getFirewallHost();
			e.setAttribute("proxyHostName",((tmpStr==null)?"":tmpStr));
			int tmpInt=m_Model.getFirewallPort();
			e.setAttribute("proxyPortNumber",Integer.toString(tmpInt));
			e.setAttribute("proxyAuthorization",(JAPModel.getUseFirewallAuthorization()?"true":"false"));
			tmpStr=m_Model.getFirewallAuthUserID();
			e.setAttribute("proxyAuthUserID",((tmpStr==null)?"":tmpStr));
			tmpStr=m_Model.getInfoServiceHost();
			e.setAttribute("infoServiceHostName",((tmpStr==null)?"":tmpStr));
			tmpInt=m_Model.getInfoServicePort();
			e.setAttribute("infoServicePortNumber",Integer.toString(tmpInt));
			e.setAttribute("infoServiceDisabled",(JAPModel.isInfoServiceDisabled()?"true":"false"));
			e.setAttribute("certCheckDisabled", (JAPModel.isCertCheckDisabled()?"true":"false"));
			e.setAttribute("acceptedCertList", (JAPModel.getCertificateStore().dumpStoreData()));
			//e.setAttribute("anonserviceID",((e1.getID()==null)?"":e1.getID()));
			//e.setAttribute("anonserviceName",((e1.getName()==null)?"":e1.getName()));
			//ListenerInterface[] listenerInterfaces=e1.getListenerInterfaces();
			//ListenerInterface defaultListener=listenerInterfaces[0];
			//e.setAttribute("anonHostName",   ((defaultListener.m_strHost==null)?"":defaultListener.m_strHost));
			//e.setAttribute("anonHostIP",   ((defaultListener.m_strIP==null)?"":defaultListener.m_strIP));
			//e.setAttribute("anonPortNumber",   Integer.toString(defaultListener.m_iPort));
			//if(listenerInterfaces.length>1)
			//	{
			//		ListenerInterface secondListener=listenerInterfaces[1];
			//		e.setAttribute("anonSSLPortNumber",Integer.toString(secondListener.m_iPort));
			//	}
			e.setAttribute("DummyTrafficIntervall",Integer.toString(JAPModel.getDummyTraffic()));
			e.setAttribute("autoConnect",(JAPModel.getAutoConnect()?"true":"false"));
			e.setAttribute("autoReConnect",(JAPModel.getAutoReConnect()?"true":"false"));
			e.setAttribute("minimizedStartup",(JAPModel.getMinimizeOnStartup()?"true":"false"));
			e.setAttribute("neverRemindActiveContent",(mbActCntMessageNeverRemind?"true":"false"));
			e.setAttribute("doNotAbuseReminder",(mbDoNotAbuseReminder?"true":"false"));
			e.setAttribute("neverRemindGoodBye",(mbGoodByMessageNeverRemind?"true":"false"));
			e.setAttribute("Locale",m_Locale.getLanguage());
			e.setAttribute("LookAndFeel",UIManager.getLookAndFeel().getClass().getName());
			//adding (new) AnonServer description element
			AnonServer e1 = m_Controller.getAnonServer();
			e.appendChild(e1.toXmlNode(doc));

			// adding GUI-Element
			if(JAPModel.getSaveMainWindowPosition())
				{
					Element elemGUI = doc.createElement("GUI");
					e.appendChild(elemGUI);
					Element elemMainWindow = doc.createElement("MainWindow");
					elemGUI.appendChild(elemMainWindow);
					Element tmp=doc.createElement("SetOnStartup");
					elemMainWindow.appendChild(tmp);
					XMLUtil.setNodeValue(tmp,"true");
					tmp=doc.createElement("Location");
					elemMainWindow.appendChild(tmp);
					Point p=m_View.getLocation();
					tmp.setAttribute("x",Integer.toString(p.x));
					tmp.setAttribute("y",Integer.toString(p.y));
					tmp=doc.createElement("Size");
					elemMainWindow.appendChild(tmp);
					Dimension d=m_View.getSize();
					tmp.setAttribute("dx",Integer.toString(d.width));
					tmp.setAttribute("dy",Integer.toString(d.height));
				}
		// adding Debug-Element
			Element elemDebug=doc.createElement("Debug");
			e.appendChild(elemDebug);
			Element tmp=doc.createElement("Level");
			Text txt=doc.createTextNode(Integer.toString(JAPDebug.getDebugLevel()));
			tmp.appendChild(txt);
			elemDebug.appendChild(tmp);
			tmp=doc.createElement("Type");
			int debugtype=JAPDebug.getDebugType();
			tmp.setAttribute("GUI",(debugtype&JAPDebug.GUI)!=0?"true":"false");
			tmp.setAttribute("NET",(debugtype&JAPDebug.NET)!=0?"true":"false");
			tmp.setAttribute("THREAD",(debugtype&JAPDebug.THREAD)!=0?"true":"false");
			tmp.setAttribute("MISC",(debugtype&JAPDebug.MISC)!=0?"true":"false");
			elemDebug.appendChild(tmp);
			if(JAPDebug.isShowConsole()){
					tmp=doc.createElement("Output");
					txt=doc.createTextNode("Console");
					tmp.appendChild(txt);
					elemDebug.appendChild(tmp);
			}
			return JAPUtil.XMLDocumentToString(doc);
			//((XmlDocument)doc).write(f);
		}
		catch(Throwable ex) {
			JAPDebug.out(JAPDebug.EXCEPTION,JAPDebug.MISC,"JAPModel:save() Exception: "+ex.getMessage());
			//ex.printStackTrace();
		}
		return null;
	}
	//---------------------------------------------------------------------
	public Locale getLocale() {
		return m_Locale;
	}
	public void setLocale(Locale l) {
		if(l==null)
			return;
		if(m_Locale!=null&&m_Locale.equals(l))
			return;
		JAPMessages.init(l);
		m_Locale=l;
		Locale.setDefault(l);
		if(m_View!=null)
			m_View.localeChanged();
	}
	//---------------------------------------------------------------------
	public void setMinimizeOnStartup(boolean b)
		{
			synchronized(this) {
			m_Model.setMinimizeOnStartup(b);
		}
	}

	//---------------------------------------------------------------------
	public synchronized void setAnonServer(AnonServer s)
		{
			if(s==null)
				return;
			AnonServer current=getAnonServer();
			if(getAnonMode()&&current!=null&&!current.equals(s)) //Anon is running --> maybe we have to change....
				{
					setAnonMode(false);
					JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"JAPModel:currentAnonService changed");
					setAnonMode(true);
				}
			m_currentAnonService = s;
			m_Controller.notifyJAPObservers();
		}

	public AnonServer getAnonServer()
		{
			return m_currentAnonService;
		}

	public JAPAnonServerDB getAnonServerDB()
		{
			return m_anonServerDatabase;
		}

	//---------------------------------------------------------------------
	//May be the host and port changed - or if we should us it or not
	// or both....
	public boolean setProxy(int type,String host,int port,boolean bUseProxy)
		{
			if(bUseProxy)
				{//only if we want to use this proxy....
					if(!JAPUtil.isPort(port))
						return false;
					if(host==null)
						return false;
				}
			if(host==null)
				host="";
			// check if values have changed
			if((type!=m_Model.getFirewallType())||(bUseProxy!=m_Model.getUseFirewall())||(!host.equals(m_Model.getFirewallHost()))||(port!=m_Model.getFirewallPort()))
				{
					// reset firewall authentication password
					m_Model.setFirewallAuthPasswd(null);
					// change settings
					synchronized(this)
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

 /* public void setUseProxy(boolean b)
		{
			synchronized(this)
				{
					// if service already runs in proxy/firewall mode, we do not have to
					// change settings of InfoService and AnonService, since setProxy()
					// has already done this.
					if(!JAPModel.getUseFirewall())
						{
							m_Model.setUseFirewall(b);
							// apply changes to infoservice
							applyProxySettingsToInfoService();
							// apply changes to anonservice
							applyProxySettingsToAnonService();
						}
				}
			notifyJAPObservers();
	}*/

	private void applyProxySettingsToInfoService()
		{
			if(m_InfoService!=null)
				if(JAPModel.getUseFirewall())
					{
						if(JAPModel.getUseFirewallAuthorization())
							m_InfoService.setProxy(m_Model.getFirewallType(),m_Model.getFirewallHost(),
																		m_Model.getFirewallPort(),
																		JAPModel.getFirewallAuthUserID(),
																		getFirewallAuthPasswd());
						else
							m_InfoService.setProxy(m_Model.getFirewallType(),m_Model.getFirewallHost(),
																		m_Model.getFirewallPort(),null,null);
					}
				else //not Proxy should be used....
					m_InfoService.setProxy(0,null,-1,null,null);
		}


	private void applyProxySettingsToAnonService()
		{
			if(JAPModel.getUseFirewall() && getAnonMode())
				{
					// anon service is running
					Object[] options = { JAPMessages.getString("later"),JAPMessages.getString("reconnect") };
					int ret = JOptionPane.showOptionDialog(m_Controller.getView(),
																JAPMessages.getString("reconnectAfterProxyChangeMsg"),
																JAPMessages.getString("reconnectAfterProxyChangeTitle"),
																JOptionPane.DEFAULT_OPTION,
																JOptionPane.WARNING_MESSAGE,
																null, options, options[0]);
						if(ret==1) {
							// reconnect
							setAnonMode(false);
							setAnonMode(true);
						}
					}
		}

	public void setFirewallAuthUserID(String userid)
		{
			// check if values have changed
			if(!userid.equals(JAPModel.getFirewallAuthUserID()))
				{
					m_Model.setFirewallAuthPasswd(null);   // reset firewall authentication password
					m_Model.setFirewallAuthUserID(userid); // change settings
				}
		}

	public void setUseFirewallAuthorization(boolean b) {
			m_Model.setUseFirewallAuthorization(b);
	}
	//---------------------------------------------------------------------
	/*public boolean getUseFirewall() {
		synchronized(this) {
			return mbUseProxy;
		}
	}

	public boolean getUseFirewallAuthorization() {
			return mb_UseProxyAuthentication;
	}*/
	public static Font getDialogFont()
		{
			if(m_fontControls!=null)
				return m_fontControls;
			m_fontControls=new JButton().getFont();
			if(JAPModel.isSmallDisplay())
				m_fontControls=new Font(m_fontControls.getName(),JAPConstants.SMALL_FONT_STYLE,JAPConstants.SMALL_FONT_SIZE);
			return m_fontControls;
		}

	public static String getFirewallAuthPasswd()
		{
			if(JAPModel.getUseFirewallAuthorization())
				{
					if(JAPModel.getFirewallAuthPasswd()==null)
						m_Model.setFirewallAuthPasswd(JAPFirewallPasswdDlg.getPasswd());
				return JAPModel.getFirewallAuthPasswd();
				}
		 else
			return null;
	}
	//---------------------------------------------------------------------
	public boolean setInfoService(String host,int port)
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
	}

	public static void setInfoServiceDisabled(boolean b)
		{
			m_Model.setInfoServiceDisabled(b);
		}

	public static void setCertCheckDisabled(boolean b)
		{
			m_Model.setCertCheckDisabled(b);
		}

	public static void setCertificateStore(String certlist)
		{
			m_Model.setCertificateStore(certlist);
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
private final class SetAnonModeAsync implements Runnable
{
	private boolean anonModeSelected=false;
	public SetAnonModeAsync(boolean b)
		{
			anonModeSelected=b;
		}
/** oldRun!*/
/*
	public void run() {
		synchronized(oSetAnonModeSyncObject)
		{
	//setAnonMode--> async!!
		JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"JAPModel:setAnonMode("+anonModeSelected+")");
		if ((proxyAnon == null) && (anonModeSelected == true))
			{
				view.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				//JAPSetAnonModeSplash.start(true);
				 if (alreadyCheckedForNewVersion == false)
					{
						// Check for a new Version of JAP if not already done
						int ok = versionCheck();
						if (ok == -1) {
						// -> at the moment nothing to do
						//canStartService = false; // no necessary to set this variable
						} else {
						// -> we can start anonymity
						canStartService = true;
						alreadyCheckedForNewVersion = true;
						}
					}
				if (canStartService)
					{
						// -> we can start anonymity
						if(proxyDirect!=null)
							proxyDirect.stopService();
						proxyDirect=null;
						// starting MUX --> Success ???
						proxyAnon=new JAPAnonService(m_socketHTTPListener,JAPAnonService.PROTO_HTTP);
						AnonServerDBEntry e = m_Controller.getAnonServer();
						//2001-02-20(HF)
						if (m_Controller.getUseFirewall()) {
							// connect vi proxy to first mix (via ssl portnumber)
							if (e.getSSLPort() == -1) {
								JOptionPane.showMessageDialog(m_Controller.getView(),
									JAPMessages.getString("errorFirewallModeNotSupported"),
									JAPMessages.getString("errorFirewallModeNotSupportedTitle"),
									JOptionPane.ERROR_MESSAGE);
								return; //TODO: Maybe need to check what to do...--> anonmode=false =?
							} else {
								proxyAnon.setAnonService(e.getHost(),e.getSSLPort());
								proxyAnon.setFirewall(m_Controller.getFirewallHost(),m_Controller.getFirewallPort());
								if(m_Controller.getUseFirewallAuthorization())
									{
										proxyAnon.setFirewallAuthorization(m_Controller.getFirewallAuthUserID(),
																												m_Controller.getFirewallAuthPasswd());
									}
								proxyAnon.connectViaFirewall(true);
							}
						} else {
							// connect directly to first mix
							proxyAnon.setAnonService(e.getHost(),e.getPort());
						}
						int ret=proxyAnon.start();
						if(ret==JAPAnonService.E_SUCCESS)
							{
								// show a Reminder message that active contents should be disabled
								Object[] options = {  JAPMessages.getString("okButton") };
								JCheckBox checkboxRemindNever=new JCheckBox(JAPMessages.getString("disableActCntMessageNeverRemind"));
								Object[] message={JAPMessages.getString("disableActCntMessage"),checkboxRemindNever};
								if (!mbActCntMessageNotRemind)
									{
										ret=0;
										ret= JOptionPane.showOptionDialog(m_Controller.getView(),
																		(Object)message,
																		JAPMessages.getString("disableActCntMessageTitle"),
																		JOptionPane.DEFAULT_OPTION,
																		JOptionPane.WARNING_MESSAGE,
																		null, options, options[0]);
										mbActCntMessageNeverRemind = checkboxRemindNever.isSelected();
										mbDoNotAbuseReminder       = checkboxRemindNever.isSelected();
										if(mbActCntMessageNeverRemind)
											mbActCntMessageNotRemind=true;
									}
								m_Controller.status2 = JAPMessages.getString("statusRunning");
								proxyAnon.setAnonServiceListener(m_Controller);
								// start feedback thread
								feedback=new JAPFeedback();
								feedback.startRequests();
								view.setCursor(Cursor.getDefaultCursor());
								notifyJAPObservers();
								JAPSetAnonModeSplash.abort();
								return;
							}
						if (ret==JAPAnonService.E_BIND)
							{
								Object[] args={new Integer(portNumber)};
								String msg=MessageFormat.format(JAPMessages.getString("errorListenerPort"),args);
								JOptionPane.showMessageDialog(m_Controller.getView(),
																							msg,
																							JAPMessages.getString("errorListenerPortTitle"),
																							JOptionPane.ERROR_MESSAGE);
								JAPDebug.out(JAPDebug.EMERG,JAPDebug.NET,"Listener could not be started!");
								m_Controller.getView().disableSetAnonMode();
							}
						else
							{
								JOptionPane.showMessageDialog
									(
									 getView(),
									 JAPMessages.getString("errorConnectingFirstMix")+"\n"+JAPMessages.getString("errorCode")+": "+Integer.toString(ret),
									 JAPMessages.getString("errorConnectingFirstMixTitle"),
									 JOptionPane.ERROR_MESSAGE
									);
							}
						proxyAnon=null;
						//proxyAnonSocks=null;
						view.setCursor(Cursor.getDefaultCursor());
						m_Controller.status2 = JAPMessages.getString("statusNotRunning");
						notifyJAPObservers();
						JAPSetAnonModeSplash.abort();
						setAnonMode(false);
					}
				else
					{
						view.setCursor(Cursor.getDefaultCursor());
						JAPSetAnonModeSplash.abort();
				}
		}
		else if ((proxyDirect==null) && (anonModeSelected == false))
			{
				if(proxyAnon!=null)
					{
						JAPSetAnonModeSplash.start(false);
						proxyAnon.stop();
					}
				proxyAnon=null;
				//if(proxyAnonSocks!=null)
				//	proxyAnonSocks.stop();
				//proxyAnonSocks=null;
				if(feedback!=null)
					{
						feedback.stopRequests();
						feedback=null;
					}
				m_Controller.status2 = JAPMessages.getString("statusNotRunning");
				proxyDirect=new JAPDirectProxy(m_socketHTTPListener);
				proxyDirect.startService();

				m_Controller.getAnonServer().setMixedPackets(-1);
				m_Controller.getAnonServer().setNrOfActiveUsers(-1);
				m_Controller.getAnonServer().setTrafficSituation(-1);
				m_Controller.getAnonServer().setCurrentRisk(-1);
				notifyJAPObservers();
				JAPSetAnonModeSplash.abort();
			}
	}
}*/

/*new Run!!*/
	public void run()
		{
			synchronized(oSetAnonModeSyncObject)
				{
					boolean canStartService=true;
					//setAnonMode--> async!!
					JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"JAPModel:setAnonMode("+anonModeSelected+")");
					if ((m_proxyAnon == null) && (anonModeSelected == true))
						{//start Anon Mode
							m_View.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
							JAPSetAnonModeSplash.start(true);
							if (m_bAlreadyCheckedForNewVersion == false&&!JAPModel.isInfoServiceDisabled())
								{
									// Check for a new Version of JAP if not already done
									int ok = versionCheck();
									if (ok != -1)
										{
											// -> we can start anonymity
											m_bAlreadyCheckedForNewVersion = true;
										}
									else
										canStartService = false;
								}
							if(!canStartService)
								{
									m_View.setCursor(Cursor.getDefaultCursor());
									JAPSetAnonModeSplash.abort();
									return;
								}
							// starting MUX --> Success ???
							m_proxyAnon=new AnonProxy(m_socketHTTPListener);
							AnonServer e = m_Controller.getAnonServer();
							m_proxyAnon.setAnonService(e);
							if (JAPModel.getUseFirewall())
								{
									//try all possible ListenerInterfaces...
									m_proxyAnon.setFirewall(m_Model.getFirewallType(),m_Model.getFirewallHost(),m_Model.getFirewallPort());
									if(JAPModel.getUseFirewallAuthorization())
										{
											m_proxyAnon.setFirewallAuthorization(JAPModel.getFirewallAuthUserID(),
																														m_Controller.getFirewallAuthPasswd());
										}
								}

							// -> we can try to start anonymity
							if(m_proxyDirect!=null)
								m_proxyDirect.stopService();
							m_proxyDirect=null;

							int ret=m_proxyAnon.start();
							if(ret==AnonProxy.E_SUCCESS)
								{
									if (!mbActCntMessageNotRemind&&!JAPModel.isSmallDisplay())
										{
											// show a Reminder message that active contents should be disabled
											Object[] options = {  JAPMessages.getString("okButton") };
											JCheckBox checkboxRemindNever=new JCheckBox(JAPMessages.getString("disableActCntMessageNeverRemind"));
											Object[] message={JAPMessages.getString("disableActCntMessage"),checkboxRemindNever};
											ret=0;
											ret= JOptionPane.showOptionDialog(m_Controller.getView(),
																			(Object)message,
																			JAPMessages.getString("disableActCntMessageTitle"),
																			JOptionPane.DEFAULT_OPTION,
																			JOptionPane.WARNING_MESSAGE,
																			null, options, options[0]);
											mbActCntMessageNeverRemind = checkboxRemindNever.isSelected();
											mbDoNotAbuseReminder       = checkboxRemindNever.isSelected();
											if(mbActCntMessageNeverRemind)
												mbActCntMessageNotRemind=true;
										}
									m_proxyAnon.setProxyListener(m_Controller);
									m_proxyAnon.setDummyTraffic(m_Model.getDummyTraffic());
									m_proxyAnon.setAutoReConnect(m_Model.getAutoReConnect());
									// start feedback thread
									feedback=new JAPFeedback();
									feedback.startRequests();
									m_View.setCursor(Cursor.getDefaultCursor());
									notifyJAPObservers();
									JAPSetAnonModeSplash.abort();
									return;
								}
							else if (ret==AnonProxy.E_BIND)
								{
									Object[] args={new Integer(JAPModel.getHttpListenerPortNumber())};
									String msg=MessageFormat.format(JAPMessages.getString("errorListenerPort"),args);
									JOptionPane.showMessageDialog(m_Controller.getView(),
																								msg,
																								JAPMessages.getString("errorListenerPortTitle"),
																								JOptionPane.ERROR_MESSAGE);
									JAPDebug.out(JAPDebug.EMERG,JAPDebug.NET,"Listener could not be started!");
									m_Controller.getView().disableSetAnonMode();
								}
							else if(ret==AnonProxy.E_MIX_PROTOCOL_NOT_SUPPORTED)
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
							 else if(ret==AnonProxy.E_INVALID_KEY)
								 {
									 JOptionPane.showMessageDialog
										 (
											 getView(),
											 JAPMessages.getString("errorMixInvalidKey"),
											 JAPMessages.getString("errorMixInvalidTitle"),
											 JOptionPane.ERROR_MESSAGE
										 );
								 }

							 else if(ret==AnonProxy.E_INVALID_CERTIFICATE)
								 {
									 JOptionPane.showMessageDialog
										 (
											 getView(),
											 JAPMessages.getString("errorCertificateInvalid"),
											 JAPMessages.getString("errorCertificateInvalidTitle"),
											 JOptionPane.ERROR_MESSAGE
										 );
								 }


							 else if(ret==AnonProxy.E_SIGNATURE_CHECK_FIRSTMIX_FAILED)
								 {
									 JOptionPane.showMessageDialog
										 (
											 getView(),
											 JAPMessages.getString("errorMixFirstMixSigCheckFailed"),
											 JAPMessages.getString("errorMixFirstMixSigCheckFailedTitle"),
											 JOptionPane.ERROR_MESSAGE
										 );
								 }

							 else if(ret==AnonProxy.E_SIGNATURE_CHECK_OTHERMIX_FAILED)
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
									if(!JAPModel.isSmallDisplay())
										{
											JOptionPane.showMessageDialog
												(
													getView(),
													JAPMessages.getString("errorConnectingFirstMix")+"\n"+JAPMessages.getString("errorCode")+": "+Integer.toString(ret),
													JAPMessages.getString("errorConnectingFirstMixTitle"),
													JOptionPane.ERROR_MESSAGE
												);
										}
								}
							m_proxyAnon=null;
							m_View.setCursor(Cursor.getDefaultCursor());
							notifyJAPObservers();
							JAPSetAnonModeSplash.abort();
							setAnonMode(false);
					}
				else if ((m_proxyDirect==null) && (anonModeSelected == false))
					{
						if(m_proxyAnon!=null)
							{
								JAPSetAnonModeSplash.start(false);
								m_proxyAnon.stop();
							}
						m_proxyAnon=null;
						if(feedback!=null)
							{
								feedback.stopRequests();
								feedback=null;
							}
						m_proxyDirect=new DirectProxy(m_socketHTTPListener);
						m_proxyDirect.startService();

						m_Controller.getAnonServer().setMixedPackets(-1);
						m_Controller.getAnonServer().setNrOfActiveUsers(-1);
						m_Controller.getAnonServer().setTrafficSituation(-1);
						m_Controller.getAnonServer().setCurrentRisk(-1);
						notifyJAPObservers();
						JAPSetAnonModeSplash.abort();
					}
			}
	}

}//end of class SetAnonModeAsync
	//---------------------------------------------------------------------
	//---------------------------------------------------------------------
	//---------------------------------------------------------------------
	public boolean getAnonMode() {
		return m_proxyAnon!=null;
	}
	public synchronized void setAnonMode(boolean anonModeSelected) {
		Thread t=new Thread(new SetAnonModeAsync(anonModeSelected),"JAP - SetAnonModeAsync");
		t.start();
	}

	public void setDummyTraffic(int msIntervall)
		{
			m_Model.setDummyTraffic(msIntervall);
			if(m_proxyAnon!=null)
				m_proxyAnon.setDummyTraffic(msIntervall);
		}

	public void setAutoConnect(boolean b)
		{
			m_Model.setAutoConnect(b);
		}

	public void setAutoReConnect(boolean b)
		{
			m_Model.setAutoReConnect(b);
			if(m_proxyAnon!=null)
				m_proxyAnon.setAutoReConnect(b);
		}
	/*public synchronized void setAnonMode(boolean anonModeSelected)
	{
		JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"JAPModel:setAnonMode("+anonModeSelected+")");
		if ((proxyAnon == null) && (anonModeSelected == true))
			{
				view.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
	//			JAPSetAnonMode.start();
//				java.awt.Toolkit.getDefaultToolkit().sync();
//				try{javax.swing.SwingUtilities.invokeAndWait(new JAPSetAnonMode());}catch(Exception e){};
				if (alreadyCheckedForNewVersion == false)
					{
						// Check for a new Version of JAP if not already done
						int ok = this.versionCheck();
						if (ok == -1) {
						// -> at the moment nothing to do
						//canStartService = false; // no necessary to set this variable
						} else {
						// -> we can start anonymity
						canStartService = true;
						alreadyCheckedForNewVersion = true;
						}
					}
				if (canStartService)
					{
						// -> we can start anonymity
						if(proxyDirect!=null)
							proxyDirect.stopService();
						proxyDirect=null;
						// starting MUX --> Success ???
						proxyAnon=new JAPAnonService(m_socketHTTPListener,JAPAnonService.PROTO_HTTP);
						//2001-02-20(HF)
						if (m_Controller.getUseProxy()) {
							// connect vi proxy to first mix (via ssl portnumber)
							if (m_Controller.anonSSLPortNumber == -1) {
								JOptionPane.showMessageDialog(m_Controller.getView(),
									JAPMessages.getString("errorFirewallModeNotSupported"),
									JAPMessages.getString("errorFirewallModeNotSupportedTitle"),
									JOptionPane.ERROR_MESSAGE);
								proxyAnon.setAnonService(m_Controller.anonHostName,m_Controller.anonPortNumber);
								proxyAnon.setFirewall(m_Controller.getProxyHost(),m_Controller.getProxyPort());
								proxyAnon.connectViaFirewall(true);
							} else {
								proxyAnon.setAnonService(m_Controller.anonHostName,m_Controller.anonSSLPortNumber);
								proxyAnon.setFirewall(m_Controller.getProxyHost(),m_Controller.getProxyPort());
								proxyAnon.connectViaFirewall(true);
							}
						} else {
							// connect directly to first mix
							proxyAnon.setAnonService(m_Controller.anonHostName,m_Controller.anonPortNumber);
						}
						int ret=proxyAnon.start();
						if(ret==JAPAnonService.E_SUCCESS)
							{
								// show a Reminder message that active contents should be disabled
								Object[] options = { JAPMessages.getString("disableActCntMessageDontRemind"), JAPMessages.getString("okButton") };
								JCheckBox checkboxRemindNever=new JCheckBox(JAPMessages.getString("disableActCntMessageNeverRemind"));
								Object[] message={JAPMessages.getString("disableActCntMessage"),checkboxRemindNever};
								if (!mbActCntMessageNotRemind)
									{
										ret=0;
										ret= JOptionPane.showOptionDialog(m_Controller.getView(),
																		(Object)message,
																		JAPMessages.getString("disableActCntMessageTitle"),
																		JOptionPane.DEFAULT_OPTION,
																		JOptionPane.WARNING_MESSAGE,
																		null, options, options[1]);
										mbActCntMessageNeverRemind=checkboxRemindNever.isSelected();
										if(ret==0||mbActCntMessageNeverRemind)
											mbActCntMessageNotRemind=true;
									}
								if(mbSocksListener)
									{
										proxyAnonSocks=new JAPAnonService(1080,JAPAnonService.PROTO_SOCKS,m_Controller.mblistenerIsLocal);
										proxyAnonSocks.start();
									}
								m_Controller.status2 = JAPMessages.getString("statusRunning");
								proxyAnon.setAnonServiceListener(this);
								// start feedback thread
								feedback=new JAPFeedback();
								feedback.startRequests();
								view.setCursor(Cursor.getDefaultCursor());
								notifyJAPObservers();
								return;
							}
						if (ret==JAPAnonService.E_BIND)
							{
								Object[] args={new Integer(portNumber)};
								String msg=MessageFormat.format(JAPMessages.getString("errorListenerPort"),args);
								JOptionPane.showMessageDialog(m_Controller.getView(),
																							msg,
																							JAPMessages.getString("errorListenerPortTitle"),
																							JOptionPane.ERROR_MESSAGE);
								JAPDebug.out(JAPDebug.EMERG,JAPDebug.NET,"Listener could not be started!");
								m_Controller.getView().disableSetAnonMode();
							}
						else
							{
								JOptionPane.showMessageDialog
									(
									 getView(),
									 getString("errorConnectingFirstMix")+"\n"+getString("errorCode")+": "+Integer.toString(ret),
									 getString("errorConnectingFirstMixTitle"),
									 JOptionPane.ERROR_MESSAGE
									);
							}
						proxyAnon=null;
						proxyAnonSocks=null;
						view.setCursor(Cursor.getDefaultCursor());
						m_Controller.status2 = JAPMessages.getString("statusNotRunning");
						notifyJAPObservers();
						setAnonMode(false);
					}
				else
					view.setCursor(Cursor.getDefaultCursor());

		}
		else if ((proxyDirect==null) && (anonModeSelected == false))
			{
				if(proxyAnon!=null)
					proxyAnon.stop();
				proxyAnon=null;
				if(proxyAnonSocks!=null)
					proxyAnonSocks.stop();
				proxyAnonSocks=null;
				if(feedback!=null)
					{
						feedback.stopRequests();
						feedback=null;
					}
				m_Controller.status2 = JAPMessages.getString("statusNotRunning");
				proxyDirect=new JAPDirectProxy(m_socketHTTPListener);
				proxyDirect.startService();
				m_Controller.mixedPackets = -1;
				m_Controller.nrOfActiveUsers = -1;
				m_Controller.trafficSituation = -1;
				m_Controller.currentRisk = -1;
				notifyJAPObservers();
			}
	}
*/
	//---------------------------------------------------------------------
	public void setHTTPListener(int port, boolean isLocal,boolean bShowWarning)
		{
			if(JAPModel.getHttpListenerPortNumber()==port)
				bShowWarning=false;
			if((JAPModel.getHttpListenerPortNumber()!=port)||(JAPModel.getHttpListenerIsLocal()!=isLocal)) {
			m_Model.setHttpListenerPortNumber(port);
			synchronized(this) {
				m_Model.setHttpListenerIsLocal(isLocal);
			}
			JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"JAPModel:HTTP listener settings changed");
			if(bShowWarning)
				JOptionPane.showMessageDialog(m_Controller.getView(),JAPMessages.getString("confmessageListernPortChanged"));
			m_Controller.notifyJAPObservers();
		}
	}
	/*public int getHTTPListenerPortNumber() {
		return portNumber;
	}*/
	/*public boolean getHTTPListenerIsLocal() {
		synchronized(this) {
			return mblistenerIsLocal;
		}
	}*/
	//---------------------------------------------------------------------
	private boolean startHTTPListener()
		{
			JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"JAPModel:startListener");
			if (isRunningListener == false)
				{
					boolean bindOk=false;
					for(int i=0;i<10;i++) //HAck for Mac!!
						try
							{
								if(JAPModel.getHttpListenerIsLocal())
									{
										//InetAddress[] a=InetAddress.getAllByName("localhost");
										InetAddress[] a=InetAddress.getAllByName("127.0.0.1");
										JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"Try binding Listener on localhost: "+a[0]);
										m_socketHTTPListener = new ServerSocket (JAPModel.getHttpListenerPortNumber(),50,a[0]);
									}
								else
									m_socketHTTPListener = new ServerSocket (JAPModel.getHttpListenerPortNumber());
								JAPDebug.out(JAPDebug.INFO,JAPDebug.NET,"Listener on port " + JAPModel.getHttpListenerPortNumber() + " started.");
								try
									{
										m_socketHTTPListener.setSoTimeout(2000);
									}
								catch(Exception e1)
									{
										JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"Could not set listener accept timeout: Exception: "+e1.getMessage());
									}
								bindOk=true;
								break;
							}
						catch(Exception e)
							{
								JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"Exception: "+e.getMessage());
								m_socketHTTPListener=null;
							}
						isRunningListener=bindOk;
				}
			return isRunningListener;
		}
	private void stopHTTPListener()
		{
			JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"JAPModel:stopListener");
			if (isRunningListener)
				{
					setAnonMode(false);
					try{m_socketHTTPListener.close();}catch(Exception e){};
					m_socketHTTPListener=null;
					isRunningListener = false;
				}
		}
	//---------------------------------------------------------------------

	/** This (and only this) is the final exit procedure of JAP!
	 *
	 */
	public void goodBye() {
		try
		{
		//stopListener();
		//view.setVisible(false);
		//iconifiedView.setVisible(false);
		// show a Reminder message that active contents should be disabled
		Object[] options = { JAPMessages.getString("okButton") };
		JCheckBox checkboxRemindNever=new JCheckBox(JAPMessages.getString("disableGoodByMessageNeverRemind"));
		Object[] message = { JAPMessages.getString("disableGoodByMessage"),checkboxRemindNever };
		if (!mbGoodByMessageNeverRemind) {
				JOptionPane.showOptionDialog(m_Controller.getView(),
												(Object)message,
												JAPMessages.getString("disableGoodByMessageTitle"),
												JOptionPane.DEFAULT_OPTION,
												JOptionPane.WARNING_MESSAGE,
												null, options, options[0]);
				mbGoodByMessageNeverRemind = checkboxRemindNever.isSelected();
		}
		m_Controller.saveConfigFile();
		}
		catch(Throwable t)
			{
			}
		System.exit(0);
	}

	/** Shows the About dialog
	 */
	public static void aboutJAP() {
		try {
			new JAPAbout(m_View);
		} catch(Throwable t) {
			t.printStackTrace();
		}
	}

	/** Try to load all available MIX-Cascades form the InfoService...
	 */
	public void fetchAnonServers()
		{
			JAPDebug.out(JAPDebug.INFO,JAPDebug.MISC,"JAPModel:Trying to fetch anon servers from InfoService");
			AnonServer[] db=null;
			try
				{
					db=getInfoService().getAvailableAnonServers();
				}
			catch (Exception e)
				{
					JAPDebug.out(JAPDebug.ERR,JAPDebug.NET,"JAPModel:fetchAnonServers: "+e);
					if(!JAPModel.isSmallDisplay())
						{
							JOptionPane.showMessageDialog(m_View,
											JAPMessages.getString("errorConnectingInfoService"),
											JAPMessages.getString("errorConnectingInfoServiceTitle"),
											JOptionPane.ERROR_MESSAGE);
						}
				}
			if((db!=null)&&(db.length>=1))
				{
					JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"JAPModel:fetchAnonServers: success!");
					JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"JAPModel:fetchAnonServers: removing old entries");
					m_anonServerDatabase.clean();
					for(int i=0;i<db.length;i++)
						m_anonServerDatabase.addEntry(db[i]);
					JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"JAPModel:fetchAnonServers: adding new entries finished");
					JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"JAPModel:fetchAnonServers: notify observers");
					notifyJAPObservers();
				}
		}

	/** Performs the Versioncheck.
	 *  @return -1, if version check says that anonymity mode should not be enabled.
	 *          Reasons can be: new version found, version check failed
	 */
	public int versionCheck()
		{
			JAPDebug.out(JAPDebug.INFO,JAPDebug.MISC,"JAPModel:Checking for new version of JAP...");
			try
				{
					int result = 0;
					String s = getInfoService().getNewVersionNumber();
					if(s==null)
						return -1;
					s=s.trim();
					// temporary changed due to stability.... (sk13)
					//String s = vc.getNewVersionnumberFromNet("http://anon.inf.tu-dresden.de:80"+aktJAPVersionFN);
					JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"JAPModel:Version:"+JAPConstants.aktVersion);
					if ( s.compareTo(JAPConstants.aktVersion) <= 0 )
						return 0; //ok --> no new version available
					// OK, new version available
					// ->Ask user if he/she wants to download new version
					//	Object[] options = { JAPMessages.getString("newVersionNo"), JAPMessages.getString("newVersionYes") };
					//	ImageIcon   icon = loadImageIcon(DOWNLOADFN,true);
					int answer;
					answer=JOptionPane.showConfirmDialog( m_View,
																							JAPMessages.getString("newVersionAvailable"),
																							JAPMessages.getString("newVersionAvailableTitle"),
																							JOptionPane.YES_NO_OPTION);
					if (answer==JOptionPane.YES_OPTION)
						{
							// User has elected to download new version of JAP
							// ->Download, Alert, exit program
							JAPVersionInfo vi=getInfoService().getJAPVersionInfo(InfoService.JAP_RELEASE_VERSION);
							JAPUpdateWizard wz=new JAPUpdateWizard(vi);
							//Assumption: If we are here, the download failed for some resaons
							//otherwise the programm would quit
							//TODO: Do this in a better way!!
							if(wz.getStatus()!=wz.UPDATESTATUS_SUCCESS)
								{
									// Download failed
									// Alert, and reset anon mode to false
									JAPDebug.out(JAPDebug.ERR,JAPDebug.MISC,"JAPModel:versionCheck(): Some update problem");
									JOptionPane.showMessageDialog(m_View,
																					JAPMessages.getString("downloadFailed")+JAPMessages.getString("infoURL"),
																					JAPMessages.getString("downloadFailedTitle"),
																					JOptionPane.ERROR_MESSAGE);
									notifyJAPObservers();
									return -1;
								}
							goodBye(); //restart JAP after Update
							return 0;
						}
					else
						{
							// User has elected not to download
							// ->Alert, we should'nt start the system due to possible compatibility problems
							JOptionPane.showMessageDialog(m_View,
																					JAPMessages.getString("youShouldUpdate")+JAPMessages.getString("infoURL"),
																					JAPMessages.getString("youShouldUpdateTitle"),
																					JOptionPane.WARNING_MESSAGE);
							notifyJAPObservers();
							return -1;
						}
				}
		catch (Exception e) {
			// Verson check failed
			// ->Alert, and reset anon mode to false
			JAPDebug.out(JAPDebug.ERR,JAPDebug.MISC,"JAPModel: "+e);
			JAPUtil.showMessageBox(m_View,"errorConnectingInfoService","errorConnectingInfoServiceTitle",JOptionPane.ERROR_MESSAGE);
			//JOptionPane.showMessageDialog(view,
			//															JAPMessages.getString("errorConnectingInfoService"),
			//															JAPMessages.getString("errorConnectingInfoServiceTitle"),
			//															JOptionPane.ERROR_MESSAGE);
			notifyJAPObservers();
			return -1;
		}
		// this line should never be reached
	}

	//---------------------------------------------------------------------
	public void registerMainView(JAPView v) {
			m_View=v;
	}
	public static JAPView getView() {
			return m_Controller.m_View;
	}
	//---------------------------------------------------------------------
	public void addJAPObserver(JAPObserver o)
		{
			observerVector.addElement(o);
		}

	public void notifyJAPObservers()
		{
			JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"JAPModel:notifyJAPObservers()");
			synchronized(observerVector)
				{
					try
						{
							Enumeration enum = observerVector.elements();
							int i=0;
							while (enum.hasMoreElements())
								{
									JAPObserver listener = (JAPObserver)enum.nextElement();
									JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"JAPModel:notifyJAPObservers: "+i);
									listener.valuesChanged();
									i++;
								}
						}
					catch(Throwable t)
						{
							JAPDebug.out(JAPDebug.EMERG,JAPDebug.MISC,"JAPModel:notifyJAPObservers - critical exception: "+t.getMessage());
						}
				}
			JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"JAPModel:notifyJAPObservers()-ended");
		}
	//---------------------------------------------------------------------
	public void channelsChanged(int channels)
		{
			nrOfChannels=channels;
					Enumeration enum = observerVector.elements();
					while (enum.hasMoreElements())
						{
							JAPObserver listener = (JAPObserver)enum.nextElement();
							listener.channelsChanged(channels);
						}
		}
	public void transferedBytes(int bytes)
		{
			nrOfBytes+=bytes;
					Enumeration enum = observerVector.elements();
					while (enum.hasMoreElements())
						{
							JAPObserver listener = (JAPObserver)enum.nextElement();
							listener.transferedBytes(nrOfBytes);
						}
		}

}

