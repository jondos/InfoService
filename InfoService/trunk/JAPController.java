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
import java.text.MessageFormat;
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
import anon.infoservice.InfoService;
import proxy.ProxyListener;
import proxy.DirectProxy;
import proxy.AnonProxy;
/* jh5 */ import anon.xmlrpc.Server;

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
	private boolean  canStartService             = false; // indicates if anon service can be started
	private boolean  alreadyCheckedForNewVersion = false; // indicates if check for new version has already been done
	private boolean  mbActCntMessageNotRemind    = false; // indicates if Warning message in setAnonMode has been deactivated for the session
	private boolean  mbActCntMessageNeverRemind  = false; // indicates if Warning message in setAnonMode has been deactivated forever
	private boolean  mbDoNotAbuseReminder        = false; // indicates if new warning message in setAnonMode (containing Do no abuse) has been shown
	private boolean  mbGoodByMessageNeverRemind  = false; // indicates if Warning message before exit has been deactivated forever

	private	static final Object oSetAnonModeSyncObject=new Object();

	public  String   status1           = " ";
	public  String   status2           = " ";

	private int      nrOfChannels      = 0;
	private int      nrOfBytes         = 0;

	private static  JAPView           view          = null;
	private static  InfoService    m_InfoService  = null;
	private static  JAPController     m_Controller         = null;
	private static  JAPModel          m_Model         = null;
	private static  JAPFeedback       feedback      = null;
	private Locale m_Locale=null;
	private Vector observerVector=null;

	private static Server anonServiceRMIServer= null;

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
	 *		portNumber=""									// Listener-Portnumber
	 *		portNumberSocks=""						// Listener-Portnumber for SOCKS
	 *		supportSocks=""								// Will we support SOCKS ?
	 *    listenerIsLocal="true"/"false"// Listener lasucht nur an localhost ?
	 *		proxyMode="true"/"false"			// Using a HTTP-Proxy??
   *    proxyType="SOCKS"/"HTTP"      // which kind of proxy
	 *		proxyHostName="..."						// the Name of the HTTP-Proxy
	 *		proxyPortNumber="..."					// port number of the HTTP-proxy
	 *    proxyAuthorization="true"/"false" //Need authorization to acces the proxy ?
	 *    porxyAuthUserID="..."         //UserId for the Proxy if Auth is neccesary
	 *		infoServiceHostName="..."			// hostname of the infoservice
	 *		infoServicePortnumber=".."		// the portnumber of the info service
   *    anonserviceID=".."            //the Id of the anonService
	 *    anonserviceName=".."          //the name of the anon-service
	 *		anonHostName=".."							// the hostname of the anon-service
	 *		anonHostIP=".."							  // the ip of the anon-service
	 *		anonPortNumber=".."						// the portnumber of the anon-service
	 *    anonSSLPortNumber=".."        /the "proxy" port number of anon-service
	 *		autoConnect="true"/"false"		// should we start the anon service immedialy after programm launch ?
	 *		autoReConnect="true"/"false"		// should we automatically reconnect to mix if connection was lost ?
	 *		DummyTrafficIntervall=".."    //Time of inactivity in milli seconds after which a dummy is send
   *    minimizedStartup="true"/"false" // should we start minimized ???
	 *		neverRemindActiveContent="true"/"false" // should we remind the user about active content ?
	 *    Locale="LOCALE_IDENTIFIER (two letter iso 639 code)" //the Language for the UI to use
	 *    LookAndFeel="..."             //the LookAndFeel
	 *	>
	 *	<Debug>
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
	 */
	public synchronized void loadConfigFile() {
		// Load default anon services
	//		anonServerDatabase.addElement(new AnonServerDBEntry(anonHostName, anonPortNumber));
//		anonServerDatabase.addElement(new AnonServerDBEntry(proxyHostName, proxyPortNumber));
//		anonServerDatabase.addElement(new AnonServerDBEntry("anon.inf.tu-dresden.de", 6543));
//		anonServerDatabase.addElement(new AnonServerDBEntry("passat.mesh.de", 6543));
		// Load config from xml file
		JAPDebug.out(JAPDebug.INFO,JAPDebug.MISC,"JAPModel:try loading configuration from "+JAPConstants.XMLCONFFN);
		try {
			String dir=System.getProperty("user.home","");
			FileInputStream f=null;
			//first tries in user.home
			try {
				f=new FileInputStream(dir+"/"+JAPConstants.XMLCONFFN);
			} catch(Exception e) {
				f=new FileInputStream(JAPConstants.XMLCONFFN); //and then in the current directory
			}
			Document doc=DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(f);
			Element root=doc.getDocumentElement();
			NamedNodeMap n=root.getAttributes();
			//
			int port=JAPUtil.parseElementAttrInt(root,"portNumber",JAPModel.getHttpListenerPortNumber());
			boolean bListenerIsLocal=JAPUtil.parseNodeBoolean(n.getNamedItem("listenerIsLocal"),true);
			setHTTPListener(port,bListenerIsLocal,false);
      //portSocksListener=JAPUtil.parseElementAttrInt(root,"portNumberSocks",portSocksListener);
			//setUseSocksPort(JAPUtil.parseNodeBoolean(n.getNamedItem("supportSocks"),false));
			//setUseProxy(JAPUtil.parseNodeBoolean(n.getNamedItem("proxyMode"),false));
			setUseFirewallAuthorization(JAPUtil.parseNodeBoolean(n.getNamedItem("proxyAuthorization"),false));
			// load settings for the reminder message in setAnonMode
			mbActCntMessageNeverRemind=JAPUtil.parseNodeBoolean(n.getNamedItem("neverRemindActiveContent"),false);
			mbDoNotAbuseReminder      =JAPUtil.parseNodeBoolean(n.getNamedItem("doNotAbuseReminder"),false);
			if(mbActCntMessageNeverRemind && mbDoNotAbuseReminder)
				mbActCntMessageNotRemind=true;
			// load settings for the reminder message before goodBye
			mbGoodByMessageNeverRemind=JAPUtil.parseNodeBoolean(n.getNamedItem("neverRemindGoodBye"),false);
			// load settings for Info Service
			String host;
			host=JAPUtil.parseNodeString(n.getNamedItem("infoServiceHostName"),JAPModel.getInfoServiceHost());
			port=JAPUtil.parseElementAttrInt(root,"infoServicePortNumber",JAPModel.getInfoServicePort());
			setInfoService(host,port);
			// load settings for proxy
			host=JAPUtil.parseNodeString(n.getNamedItem("proxyHostName"),m_Model.getFirewallHost());
			port=JAPUtil.parseElementAttrInt(root,"proxyPortNumber",m_Model.getFirewallPort());
			if(host.equalsIgnoreCase("ikt.inf.tu-dresden.de"))
				host="";
      boolean bUseProxy=JAPUtil.parseNodeBoolean(n.getNamedItem("proxyMode"),false);
			String type=JAPUtil.parseNodeString(n.getNamedItem("proxyType"),"HTTP");
			if(type.equalsIgnoreCase("SOCKS"))
        setProxy(JAPConstants.FIREWALL_TYPE_SOCKS,host,port,bUseProxy);
			else
        setProxy(JAPConstants.FIREWALL_TYPE_HTTP,host,port,bUseProxy);
     String userid=JAPUtil.parseNodeString(n.getNamedItem("proxyAuthUserID"),JAPModel.getFirewallAuthUserID());
			setFirewallAuthUserID(userid);

      String anonserviceId  = JAPUtil.parseNodeString(n.getNamedItem("anonserviceID"),null);
			String anonserviceName   = JAPUtil.parseNodeString(n.getNamedItem("anonserviceName"),null);
			String anonHostName      = JAPUtil.parseNodeString(n.getNamedItem("anonHostName"),null);
			String anonHostIP      = JAPUtil.parseNodeString(n.getNamedItem("anonHostIP"),null);
			int anonPortNumber    = JAPUtil.parseElementAttrInt(root,"anonPortNumber",-1);
			int anonSSLPortNumber = JAPUtil.parseElementAttrInt(root,"anonSSLPortNumber",-1);
      AnonServer server=null;
      try
        {
          server=new AnonServer(anonserviceId,anonserviceName,anonHostName,anonHostIP,anonPortNumber,anonSSLPortNumber);
	      }
      catch(UnknownServiceException e)
        {
          //we could not load enough info for the saved AnonService --> take current (default)
          server=m_Controller.getAnonServer();
        }
			m_Controller.setAnonServer(server);
      setDummyTraffic(JAPUtil.parseElementAttrInt(root,"DummyTrafficIntervall",-1));
			setAutoConnect(JAPUtil.parseNodeBoolean(n.getNamedItem("autoConnect"),false));
			setAutoReConnect(JAPUtil.parseNodeBoolean(n.getNamedItem("autoReConnect"),false));
			m_Model.setMinimizeOnStartup(JAPUtil.parseNodeBoolean(n.getNamedItem("minimizedStartup"),false));
			//Load Locale-Settings
			String locale=JAPUtil.parseNodeString(n.getNamedItem("Locale"),m_Locale.getLanguage());
			setLocale(new Locale(locale,""));
			//Load look-and-feel settings (not changed if SmmallDisplay!
      if(!m_Model.isSmallDisplay())
        {
        String lf=JAPUtil.parseNodeString(n.getNamedItem("LookAndFeel"),"unknown");
        LookAndFeelInfo[] lfi=UIManager.getInstalledLookAndFeels();
        for(int i=0;i<lfi.length;i++) {
          if(lfi[i].getName().equals(lf)) {
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
        //Loading debug settings
			NodeList nl=root.getElementsByTagName("Debug");
			if(nl!=null&&nl.getLength()>0)
				{
					Element elemDebug=(Element)nl.item(0);
					nl=elemDebug.getElementsByTagName("Level");
					if(nl!=null&&nl.getLength()>0)
						{
							Element elemLevel=(Element)nl.item(0);
							JAPDebug.setDebugLevel(Integer.parseInt(elemLevel.getFirstChild().getNodeValue().trim()));
						}
					nl=elemDebug.getElementsByTagName("Type");
					if(nl!=null&&nl.getLength()>0)
						{
							Element elemType=(Element)nl.item(0);
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
					nl=elemDebug.getElementsByTagName("Output");
					if(nl!=null&&nl.getLength()>0)
						{
							Element elemOutput=(Element)nl.item(0);
							JAPDebug.showConsole(elemOutput.getFirstChild().getNodeValue().trim().equalsIgnoreCase("Console"),view);
						}
				}
		}
		catch(Exception e) {
			JAPDebug.out(JAPDebug.INFO,JAPDebug.MISC,"JAPModel:Error loading configuration! "+e.toString());
		}
		// fire event
		notifyJAPObservers();
	}
	public void saveConfigFile() {
		boolean error=false;
		JAPDebug.out(JAPDebug.INFO,JAPDebug.MISC,"JAPModel:try saving configuration to "+JAPConstants.XMLCONFFN);
		try {
			String dir=System.getProperty("user.home","");
			FileOutputStream f=null;
			//first tries in user.home
			try  {
				f=new FileOutputStream(dir+"/"+JAPConstants.XMLCONFFN);
			} catch(Exception e) {
				f=new FileOutputStream(JAPConstants.XMLCONFFN); //and then in the current directory
			}
			String sb=getConfigurationAsXML();
			if(sb!=null) {
				f.write(sb.getBytes());
				f.flush();
				f.close();
			} else
				error=true;
		} catch(Exception e) {
			error=true;
		}
		if(error) {
			JAPDebug.out(JAPDebug.ERR,JAPDebug.MISC,"JAPModel:error saving configuration to "+JAPConstants.XMLCONFFN);
			JOptionPane.showMessageDialog(m_Controller.getView(),
											JAPMessages.getString("errorSavingConfig"),
											JAPMessages.getString("errorSavingConfigTitle"),
											JOptionPane.ERROR_MESSAGE);
		}
	}
	protected String getConfigurationAsXML() {
		// Save config to xml file
		// Achtung!! Fehler im Sun-XML --> NULL-Attributte koennen hinzugefuegt werden,
		// beim Abspeichern gibt es dann aber einen Fehler!
		try {
			Document doc=DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
			Element e=doc.createElement("JAP");
			doc.appendChild(e);
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
			AnonServer e1 = m_Controller.getAnonServer();
			e.setAttribute("anonserviceID",((e1.getID()==null)?"":e1.getID()));
			e.setAttribute("anonserviceName",((e1.getName()==null)?"":e1.getName()));
			e.setAttribute("anonHostName",   ((e1.getHost()==null)?"":e1.getHost()));
			e.setAttribute("anonHostIP",   ((e1.getIP()==null)?"":e1.getIP()));
			e.setAttribute("anonPortNumber",   Integer.toString(e1.getPort()));
			e.setAttribute("anonSSLPortNumber",Integer.toString(e1.getSSLPort()));
			e.setAttribute("DummyTrafficIntervall",Integer.toString(JAPModel.getDummyTraffic()));
      e.setAttribute("autoConnect",(JAPModel.getAutoConnect()?"true":"false"));
      e.setAttribute("autoReConnect",(JAPModel.getAutoReConnect()?"true":"false"));
			e.setAttribute("minimizedStartup",(JAPModel.getMinimizeOnStartup()?"true":"false"));
			e.setAttribute("neverRemindActiveContent",(mbActCntMessageNeverRemind?"true":"false"));
			e.setAttribute("doNotAbuseReminder",(mbDoNotAbuseReminder?"true":"false"));
			e.setAttribute("neverRemindGoodBye",(mbGoodByMessageNeverRemind?"true":"false"));
			e.setAttribute("Locale",m_Locale.getLanguage());
			e.setAttribute("LookAndFeel",UIManager.getLookAndFeel().getName());
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
		catch(Exception ex) {
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
		JAPMessages.init(l);
		m_Locale=l;
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
	        //setAnonMode--> async!!
		      JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"JAPModel:setAnonMode("+anonModeSelected+")");
		      if ((m_proxyAnon == null) && (anonModeSelected == true))
			      {//start Anon Mode
				      view.setCursor(Cursor.getPredefinedCursor(Cursor.WAIT_CURSOR));
				      JAPSetAnonModeSplash.start(true);
	            if (alreadyCheckedForNewVersion == false)
					      {
						      // Check for a new Version of JAP if not already done
						      int ok = versionCheck();
						      if (ok != -1)
                    {
						          // -> we can start anonymity
						          canStartService = true;
						          alreadyCheckedForNewVersion = true;
						        }
					      }
              if(!canStartService)
                {
                  view.setCursor(Cursor.getDefaultCursor());
						      JAPSetAnonModeSplash.abort();
                  return;
                }
              // starting MUX --> Success ???
              m_proxyAnon=new AnonProxy(m_socketHTTPListener);
              AnonServer e = m_Controller.getAnonServer();
              m_proxyAnon.setAnonService(e);
              if (JAPModel.getUseFirewall())
                {
                  // connect vi proxy to first mix (via ssl portnumber, if HTTP(S) proxy)
                  if (e.getSSLPort() == -1&&m_Model.getFirewallType()==JAPConstants.FIREWALL_TYPE_HTTP)
                    {
                      JOptionPane.showMessageDialog(m_Controller.getView(),
                                                    JAPMessages.getString("errorFirewallModeNotSupported"),
                                                    JAPMessages.getString("errorFirewallModeNotSupportedTitle"),
                                                    JOptionPane.ERROR_MESSAGE);
                      view.setCursor(Cursor.getDefaultCursor());
                      JAPSetAnonModeSplash.abort();
                      return;
                    }
                  else
                    {
                      m_proxyAnon.setFirewall(m_Model.getFirewallType(),m_Model.getFirewallHost(),m_Model.getFirewallPort());
                      if(JAPModel.getUseFirewallAuthorization())
                        {
                          m_proxyAnon.setFirewallAuthorization(JAPModel.getFirewallAuthUserID(),
                                                                m_Controller.getFirewallAuthPasswd());
                        }
                    }
                }

              // -> we can try to start anonymity
              if(m_proxyDirect!=null)
                m_proxyDirect.stopService();
              m_proxyDirect=null;

              int ret=m_proxyAnon.start();
              if(ret==AnonProxy.E_SUCCESS)
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
                  m_proxyAnon.setProxyListener(m_Controller);
                  m_proxyAnon.setDummyTraffic(m_Model.getDummyTraffic());
                  m_proxyAnon.setAutoReConnect(m_Model.getAutoReConnect());
                  // start feedback thread
                  feedback=new JAPFeedback();
                  feedback.startRequests();
                  view.setCursor(Cursor.getDefaultCursor());
                  notifyJAPObservers();
                  JAPSetAnonModeSplash.abort();
                  return;
                }
              if (ret==AnonProxy.E_BIND)
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
              m_proxyAnon=null;
              view.setCursor(Cursor.getDefaultCursor());
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
		Thread t=new Thread(new SetAnonModeAsync(anonModeSelected));
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

	/** This (and only this!) is the final exit procedure of JAP!
	 *
	 */
	public void goodBye() {
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
		System.exit(0);
	}

	/** Shows the About dialog
	 */
	public static void aboutJAP() {
		try {
			new JAPAbout(view);
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
			    JOptionPane.showMessageDialog(view,
                      JAPMessages.getString("errorConnectingInfoService"),
											JAPMessages.getString("errorConnectingInfoServiceTitle"),
											JOptionPane.ERROR_MESSAGE);
		    }
		  if((db!=null)&&(db.length>=1))
        {
			    m_anonServerDatabase.clean();
			    for(int i=0;i<db.length;i++)
				    m_anonServerDatabase.addEntry(db[i]);
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
					Versionchecker vc = new Versionchecker();
					String s = getInfoService().getNewVersionNumber();
					if(s==null)
						return -1;
					s=s.trim();
					// temporary changed due to stability.... (sk13)
					//String s = vc.getNewVersionnumberFromNet("http://anon.inf.tu-dresden.de:80"+aktJAPVersionFN);
					JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"JAPModel:Version:"+JAPConstants.aktVersion);
					if ( s.compareTo(JAPConstants.aktVersion) > 0 )
						{
							// OK, new version available
							// ->Ask user if he/she wants to download new version
							//	Object[] options = { JAPMessages.getString("newVersionNo"), JAPMessages.getString("newVersionYes") };
							//	ImageIcon   icon = loadImageIcon(DOWNLOADFN,true);
							String answer;
							JAPLoading japLoading = new JAPLoading(view);
								answer = japLoading.message(JAPMessages.getString("newVersionAvailableTitle"),
						   JAPMessages.getString("newVersionAvailable"),
						   JAPMessages.getString("newVersionNo"),
						   JAPMessages.getString("newVersionYes"),
						   true,false);
					if (answer.equals(JAPMessages.getString("newVersionYes"))) {
						// User has elected to download new version of JAP
						// ->Download, Alert, exit program
						// To do: show busy message
						try {
							vc.registerProgress(japLoading);
							vc.getVersionFromNet(JAPConstants.urlJAPNewVersionDownload,JAPConstants.JAPLocalFilename);
							Thread t = new Thread(vc);
							t.start();
							answer = japLoading.message(JAPMessages.getString("downloadingProgressTitle"),
						  JAPMessages.getString("downloadingProgress"),
						   null,
						   null,
						   true,true);
							t.join();
							result = vc.getResult();
							if (result == 0) {
							//
								answer = japLoading.message(JAPMessages.getString("newVersionAvailableTitle"),
							  JAPMessages.getString("newVersionLoaded"),
							  null,
							  "OK",
							  true,false);
								goodBye();
						} else {
							throw new Exception("Error loading new version");
						}
					}
					catch (Exception e) {
						// Download failed
						// Alert, and reset anon mode to false
						JAPDebug.out(JAPDebug.ERR,JAPDebug.MISC,"JAPModel:versionCheck(): Exception" + e);
						JOptionPane.showMessageDialog(view,
																					JAPMessages.getString("downloadFailed")+JAPMessages.getString("infoURL"),
																					JAPMessages.getString("downloadFailedTitle"),
																					JOptionPane.ERROR_MESSAGE);
						notifyJAPObservers();
						return -1;
					}
				} else {
					// User has elected not to download
					// ->Alert, we should'nt start the system due to possible compatibility problems
					answer = japLoading.message(JAPMessages.getString("youShouldUpdateTitle"),
						   JAPMessages.getString("youShouldUpdate")+JAPMessages.getString("infoURL"),
						   null,
						   "OK",
						   true,false);
					notifyJAPObservers();
					return -1;
				}
			}
			//endif ( s.compareTo(JAPConstants.aktVersion) > 0 )
			// --> no new version available, i.e. you are running the newest version of JAP
			return 0; // meaning: version check says that anonymity service can be started
		}
		catch (Exception e) {
			// Verson check failed
			// ->Alert, and reset anon mode to false
			JAPDebug.out(JAPDebug.ERR,JAPDebug.MISC,"JAPModel: "+e);
			JOptionPane.showMessageDialog(view,
																		JAPMessages.getString("errorConnectingInfoService"),
																		JAPMessages.getString("errorConnectingInfoServiceTitle"),
																		JOptionPane.ERROR_MESSAGE);
			notifyJAPObservers();
			return -1;
		}
		// this line should never be reached
	}
	//---------------------------------------------------------------------
	public void registerView(JAPView v) {
			view=v;
	}
	public static JAPView getView() {
			return m_Controller.view;
	}
	//---------------------------------------------------------------------
	public void addJAPObserver(JAPObserver o)
		{
			observerVector.addElement(o);
		}
	public void notifyJAPObservers()
		{
	//		JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"JAPModel:notifyJAPObservers()");
			synchronized(observerVector)
				{
					Enumeration enum = observerVector.elements();
					while (enum.hasMoreElements())
						{
							JAPObserver listener = (JAPObserver)enum.nextElement();
							listener.valuesChanged();
						}
				}
		//	JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"JAPModel:notifyJAPObservers()-ended");
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
	//---------------------------------------------------------------------
	public int setRMISupport(boolean b) {
		if(b) {
			if(anonServiceRMIServer==null)
				anonServiceRMIServer = Server.generateServer();
				anonServiceRMIServer.start();
		} else {
			if(anonServiceRMIServer!=null)
				anonServiceRMIServer.stop();
			anonServiceRMIServer=null;
		}
		return 0;
	}
}

