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
package anon.infoservice;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Attr;
import org.w3c.dom.NodeList;
import org.w3c.dom.NamedNodeMap;
import HTTPClient.HTTPConnection;
import HTTPClient.HTTPResponse;
import HTTPClient.NVPair;
//import HTTPClient.AuthorizationInfo;
import java.util.Enumeration;
import java.util.Vector;
import java.net.InetAddress;
import java.net.URL;


import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.Runnable;
import java.lang.Thread;

import HTTPClient.Codecs;
import HTTPClient.ModuleException;
import HTTPClient.ParseException;
import anon.AnonServer;
//import JAPDebug;
//import JAPUtil;
import logging.DummyLog;
import logging.Log;
import logging.LogLevel;
import logging.LogType;
final public class InfoService
	{
	//	private static final String DP = "%3A"; // Doppelpunkt
		final public static int JAP_RELEASE_VERSION=1;
		final public static int JAP_DEVELOPMENT_VERSION=2;
		final public static int PROXY_TYPE_NONE=0;
		final public static int PROXY_TYPE_HTTP=1;
		final public static int PROXY_TYPE_SOCKS=2;

		private int       m_proxyType = PROXY_TYPE_NONE;
		private String    m_proxyHost  = null;
		private int       m_proxyPort  = 0;
		private String    m_proxyAuthUserID = null;
		private String    m_proxyAuthPasswd = null;
		private String    m_infoserviceHost=null;
		private int m_infoservicePort=-1;
		private HTTPConnection m_conInfoService=null;
		private int       m_count =0;
		private boolean   m_ready = false;
		private Log       m_Log;

		public InfoService(String host,int port)
			{
				m_Log=new DummyLog();
				try{HTTPConnection.removeDefaultModule(Class.forName("HTTPClient.AuthorizationModule"));} //????
				catch(Exception e){};
				setInfoService(host,port);
			}

		public void setLogging(Log log)
			{
				if(log==null)
					m_Log=new DummyLog();
			 else
					m_Log=log;
			}

		/** This will set the InfoService to use. It also sets the Proxy-Configuration and autorization.
		 */
		public int setInfoService(String host,int port)
			{

				//We are doing authorization on our own - so remove....
				m_infoserviceHost=host;
				m_infoservicePort=port;
				m_conInfoService=createInfoServiceConnection();
				return 0;
			}

		private HTTPConnection createInfoServiceConnection()
			{
				//First: new static ProxySettings
				if(m_proxyType==PROXY_TYPE_NONE)
					{
						HTTPConnection.setProxyServer(null,-1);
						HTTPConnection.setSocksServer(null,-1);
					}
				else if(m_proxyType==PROXY_TYPE_HTTP)
					{
						HTTPConnection.setProxyServer(m_proxyHost,m_proxyPort);
						HTTPConnection.setSocksServer(null,-1);
					}
				else if(m_proxyType==PROXY_TYPE_SOCKS)
					{
						HTTPConnection.setSocksServer(m_proxyHost,m_proxyPort);
						HTTPConnection.setProxyServer(null,-1);
					}
				//Second: new HTTPConnection Instance
				HTTPConnection conn=new HTTPConnection(m_infoserviceHost,m_infoservicePort);

				//Third: Proxy authorization if neccessary
				if(m_proxyType==PROXY_TYPE_HTTP&&m_proxyAuthUserID!=null)
					{
						String tmpPasswd=Codecs.base64Encode(m_proxyAuthUserID+":"+m_proxyAuthPasswd);
						NVPair authoHeader=new NVPair("Proxy-Authorization","Basic "+tmpPasswd);
						replaceHeader(conn,authoHeader);
					}

				NVPair[] headers=new NVPair[2];
				headers[0]=new NVPair("Cache-Control","no-cache");
				headers[1]=new NVPair("Pragma","no-cache");
				replaceHeader(conn,headers[0]);
				replaceHeader(conn,headers[1]);
				conn.setAllowUserInteraction(false);
				conn.setTimeout(10000);
				return conn;
			}

		/*private int applyInfoServiceSettings(HTTPConnection conn)
			{
				NVPair[] headers=new NVPair[2];
				headers[0]=new NVPair("Cache-Control","no-cache");
				headers[1]=new NVPair("Pragma","no-cache");
				replaceHeader(conn,headers[0]);
				replaceHeader(conn,headers[1]);
				conn.setAllowUserInteraction(false);
				conn.setTimeout(10000);
				applyProxySettings(conn);
				return 0;
			}*/

		public void setProxy(int proxyType,String proxyHost,int proxyPort,String proxyAuthUserID,String proxyAuthPasswd)
			{
				m_proxyType  = proxyType;
				m_proxyHost  = proxyHost;
				m_proxyPort  = proxyPort;
				if(proxyHost==null)
					m_proxyType=PROXY_TYPE_NONE;
				m_proxyAuthUserID = proxyAuthUserID;
				m_proxyAuthPasswd = proxyAuthPasswd;
				m_conInfoService=createInfoServiceConnection();
			}

	/*	private void applyProxySettings(HTTPConnection conn)
			{
				if(conn==null)
					return;
				if(m_proxyType==PROXY_TYPE_HTTP)
					{
						conn.setProxyServer(m_proxyHost,m_proxyPort);
						conn.setCurrentProxy(m_proxyHost,m_proxyPort);
						//setting Proxy authorization...
						if(m_proxyAuthUserID!=null)
							{
								String tmpPasswd=Codecs.base64Encode(m_proxyAuthUserID+":"+m_proxyAuthPasswd);
								NVPair authoHeader=new NVPair("Proxy-Authorization","Basic "+tmpPasswd);
								replaceHeader(conn,authoHeader);
							}
					}
				else
					{
						conn.setProxyServer(null,-1);
						conn.setCurrentProxy(null,-1);
						conn.setSocksServer(m_proxyHost,m_proxyPort);
					}
			}
*/
		private int replaceHeader(HTTPConnection con,NVPair header)
			{
				NVPair headers[]=con.getDefaultHeaders();
				if(headers==null||headers.length==0)
					{
						headers=new NVPair[1];
						headers[0]=header;
						con.setDefaultHeaders(headers);
						return 0;
					}
				else
					{
						int len=headers.length;
						for(int i=0;i<len;i++)
							{
								if(headers[i].getName().equalsIgnoreCase(header.getName()))
									{
										headers[i]=header;
										con.setDefaultHeaders(headers);
										return 0;
									}
							}
						NVPair tmpHeaders[]=new NVPair[len+1];
						for(int i=0;i<len;i++)
							tmpHeaders[i]=headers[i];
						tmpHeaders[len]=header;
						con.setDefaultHeaders(tmpHeaders);
						return 0;
					}
			}

		synchronized public AnonServer[] getAvailableAnonServers() throws Exception
			{
				Vector v = new Vector();
				try
					{
						HTTPResponse resp=m_conInfoService.Get("/servers");
						try
							{
								Enumeration enum=resp.listHeaders();
								m_Log.log(LogLevel.DEBUG,LogType.NET,"HTTPResponse: "+Integer.toString(resp.getStatusCode()));
								while(enum.hasMoreElements())
									{
										String header=(String)enum.nextElement();
										m_Log.log(LogLevel.DEBUG,LogType.NET,header+": "+resp.getHeader(header));
									}
								if(resp.getStatusCode()!=200)
									{
										m_Log.log(LogLevel.DEBUG,LogType.NET,"Body: "+resp.getText());
									}
							}
						catch(Throwable tor)
							{
							}
						// XML stuff
						Document doc=DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(resp.getInputStream());
						NodeList nodelist=doc.getElementsByTagName("MixCascade");
						for(int i=0;i<nodelist.getLength();i++)
							{
								Element elem=(Element)nodelist.item(i);
								String id=elem.getAttribute("id");
								NodeList nl=elem.getElementsByTagName("Name");
								String name=nl.item(0).getFirstChild().getNodeValue().trim();
								nl=elem.getElementsByTagName("IP");
								String ip=nl.item(0).getFirstChild().getNodeValue().trim();
								nl=elem.getElementsByTagName("Host");
								String host=null;
								if(nl!=null&&nl.getLength()>0)
									host=nl.item(0).getFirstChild().getNodeValue().trim();
								if(host==null) //we have no host --> old mix there host is in <ip>
									{
										host=ip;
										ip=null;
									}
								int port=parseNodeInt(elem,"Port",-1);
								int proxyPort=parseNodeInt(elem,"ProxyPort",-1);

								AnonServer e=new AnonServer(id,name,host,ip,port,proxyPort);

								nl=elem.getElementsByTagName("CurrentStatus");
								if(nl!=null&&nl.getLength()>0)
									{
										Element elem1=(Element)nl.item(0);
										int nrOfActiveUsers=parseElementAttrInt(elem1,"ActiveUsers",-1);
										e.setNrOfActiveUsers(nrOfActiveUsers);
										int currentRisk=parseElementAttrInt(elem1,"CurrentRisk",-1);
										e.setCurrentRisk(currentRisk);
										int trafficSituation=parseElementAttrInt(elem1,"TrafficSituation",-1);
										e.setTrafficSituation(trafficSituation);
										int mixedPackets=parseElementAttrInt(elem1,"MixedPackets",-1);
										e.setMixedPackets(mixedPackets);
									}
								v.addElement(e);
							}//End for all Mixes
					}
				catch(Exception e)
					{
						throw e;
					}
				if((v==null)||(v.size()==0))
					{
						return null;
					}
				else
					{
						AnonServer[] db = new AnonServer[v.size()];
						for(int i=0;i<db.length;i++)
							{
								db[i]=(AnonServer)v.elementAt(i);
							}
						return db;
					}
			}

		synchronized public void getFeedback(AnonServer service)
			{
				int nrOfActiveUsers = -1;
				int trafficSituation = -1;
				int currentRisk = -1;
				int mixedPackets = -1;
				int iAnonLevel=-1;
				try
					{
						String strGET="/feedback/"+service.getID();
						HTTPResponse resp=m_conInfoService.Get(strGET);
						if (resp.getStatusCode()!=200)
							{
								m_Log.log(LogLevel.ERR,LogType.NET,"JAPInfoService:Bad response from server: "+resp.getReasonLine());
							}
					else
						{
							// XML stuff
							Document doc=DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(resp.getInputStream());
							Element root=doc.getDocumentElement();
							nrOfActiveUsers   = parseElementAttrInt(root,"nrOfActiveUsers",-1);
							trafficSituation  = parseElementAttrInt(root,"trafficSituation",-1);
							currentRisk       = parseElementAttrInt(root,"currentRisk",-1);
							mixedPackets      = parseElementAttrInt(root,"mixedPackets",-1);
							iAnonLevel        = parseElementAttrInt(root,"anonLevel",-1);
						}
					// close streams and socket
				}
			catch(Exception e)
				{
					m_Log.log(LogLevel.ERR,LogType.NET,"JAPInfoService - Feedback: "+e);
				}
			service.setNrOfActiveUsers(nrOfActiveUsers);
			service.setTrafficSituation(trafficSituation);
			service.setCurrentRisk(currentRisk);
			service.setMixedPackets(mixedPackets);
			service.setAnonLevel(iAnonLevel);
			m_Log.log(LogLevel.DEBUG,LogType.MISC,"JAPInfoService:"+nrOfActiveUsers+"/"+trafficSituation+"/"+currentRisk+"/"+mixedPackets+"/"+iAnonLevel);
		}
	synchronized public String getNewVersionNumber() throws Exception
		{
			try
				{
					HTTPResponse resp=m_conInfoService.Get("/aktVersion");
					if (resp==null || resp.getStatusCode()!=200)
						throw new Exception("Versioncheck bad response from server: "+resp.getReasonLine());
					// read remaining header lines
					byte[] buff=resp.getData();
					String s=new String(buff).trim();
					m_Log.log(LogLevel.DEBUG,LogType.NET,"JAPInfoService:New Version: "+s);
					if ( (s.charAt(2) == '.') && (s.charAt(5) == '.') )
						return s;
					throw new Exception("Versionfile has wrong format! Found: \""+s+ "\". Should be \"nn.nn.nnn\"!");
				}
			catch(Exception e)
				{
					throw new Exception("Versioncheck failed: "+e);
				}
		}

	synchronized public JAPVersionInfo getJAPVersionInfo(int type)
		{
			String strQuery=null;
			if(type==JAP_RELEASE_VERSION)
				strQuery="/japRelease.jnlp";
			else if(type==JAP_DEVELOPMENT_VERSION)
				strQuery="/japDevelopment.jnlp";
			else
				return null;
			try
				{
					HTTPResponse resp=m_conInfoService.Get(strQuery);
					if (resp==null || resp.getStatusCode()!=200)
						return null;
					// read remaining header lines
					byte[] buff=resp.getData();
					return new JAPVersionInfo(buff,type);
				}
			catch(Exception e)
				{
					return null;
				}
		}

	/** This will retrieve the data of an URL from the InfoService. This is done asynchronous. The DownloadListener will
	 *  be informed if some new data is available or if an error occured etc.
	 *  @param url URL, which should be retrieved
	 *  @param listener, downloadListener, which is informed about new data etc.
	 */
	synchronized public void retrieveURL(URL url,DownloadListener listener)
		{
			new AsyncDownload(url,listener);
		}

		private final class AsyncDownload implements Runnable
			{
				private final URL url;
				private final DownloadListener listener;
				private HTTPConnection conn;
				public AsyncDownload(URL u,DownloadListener l)
					{
						url=u;
						listener=l;
						conn=createInfoServiceConnection();
						//applyInfoServiceSettings(conn);
						new Thread(this).start();
					}

				public void run()
					{
						try
							{
								HTTPResponse resp=conn.Get("/forward/"+url.toExternalForm());
								if(resp.getStatusCode()!=200)
									{
										listener.progress(null,0,0,DownloadListener.STATE_ABORTED);
										return;
									}
								int lenTotal=resp.getHeaderAsInt("Content-Length");
								InputStream in=resp.getInputStream();
								byte[] buff=new byte[2048];
								int len=0;
								while((len=in.read(buff))>0)
									{
										if(listener.progress(buff,len,lenTotal,DownloadListener.STATE_IN_PROGRESS)!=0)
											return;
									}
							 listener.progress(null,0,lenTotal,DownloadListener.STATE_FINISHED);
							}
						catch(Exception e)
							{
								e.printStackTrace();
								listener.progress(null,0,0,DownloadListener.STATE_ABORTED);
							}
					}
			}
					 /////////////////////////////////////////////////////////////////////
					 //connect for JAPUpdate .jnlp-Files

				/*public HTTPConnection getConInfoService()
					{
						 return m_conInfoService;
					}*/
// inner class LoadThread needed for showing Download-Progress using JProgressBar

//public class LoadThread extends Thread{

//  public void run(){}
					// get the jarfiles
 /* public void connect(String codeBase, String jarPath, String jarFileName, JProgressBar progressBar)
	{

	 byte[]data = new byte[1024];

	 String jnlpRelease = "";
	 File newVersionJarFile = new File(jarFileName);
	 FileOutputStream fos;
	 int i;
	 InputStream is = null;

	try
		{
	HTTPConnection con = new HTTPConnection(codeBase);
	HTTPResponse   rsp = con.Get(jarPath);
	if (rsp.getStatusCode() >= 300)
	{
			System.err.println("Received Error: "+rsp.getReasonLine());
			System.err.println(rsp.getText());
				//    rsp.
	}
	else{
						 System.out.println(rsp.getReasonLine()+rsp.getStatusCode());
			// data = rsp.getData();
						 fos = new FileOutputStream(newVersionJarFile);
						 is = rsp.getInputStream();
						 while((is.read(data))!= -1)
								 {
									 //count ++;
									 //progressBar.setValue(count);
									 progressBar.repaint();
								 //  System.out.println(count);
									 fos.write(data);
								 }
							fos.flush();
							fos.close();
							is.close();
							//ready = true;
						// jnlpRelease = new String(data);
						 System.out.println(jnlpRelease);

						// parseFile(typeLate, data);
						 }

		}catch(ModuleException me)
		{
		System.err.println(me);
		}catch(ParseException pe)
		{
		System.err.println(pe);
		}
		catch (IOException ioe)
		{
	System.err.println(ioe.toString());
		}
	}
//}//End LoadThread

*/
					 /////////////////////////////////////////////////////////////////////
	public static void main(String[] argv) {
	//	InfoService is=new InfoService("localhost",6543);
		InfoService is=new InfoService("infoservice.inf.tu-dresden.de",6543);
	//	is.setProxy(is.PROXY_TYPE_SOCKS,"dud14.inf.tu-dresden.de",1080,null,null);
		//is.setProxyEnabled(false/*true*/);
		try {
			JAPVersionInfo info=is.getJAPVersionInfo(is.JAP_RELEASE_VERSION);
			URL codeBase=info.getCodeBase();

			is.retrieveURL(new URL(codeBase,info.getJAPJarFileName()+"?version-id="+info.getVersion()),new DownloadListener()
			{public int progress(byte[] data,int lenData,int lenTotal,int state)
			{
				System.out.println("Progress - State: "+state+" Len: "+lenData+" Totla-Len: "+lenTotal);
				return 0;
			}});
			System.out.println("Version:"+is.getNewVersionNumber());
			AnonServer[] d = is.getAvailableAnonServers();
			if(d!=null) {
				for(int i=0;i<d.length;i++) {
					is.getFeedback(d[i]);
					System.out.println("Service:"+d[i].getName()+" Users:"+d[i].getNrOfActiveUsers());
				}
			}
		}
		catch(Exception e) {
			e.printStackTrace();
		}
	}
	private int parseNodeInt(Element parent,String name,int defaultValue)
		{
			int i=defaultValue;
			if(parent!=null)
				try
					{
						NodeList nl=parent.getElementsByTagName(name);
						i=Integer.parseInt(nl.item(0).getFirstChild().getNodeValue());
					}
				catch(Exception e)
					{
					}
			return i;
		}

		private int parseElementAttrInt(Element e,String attr,int defaultValue)
		{
			int i=defaultValue;
			if(e!=null)
				try
					{
						Attr at=e.getAttributeNode(attr);
						i=Integer.parseInt(at.getValue());
					}
				catch(Exception ex)
					{
					}
			return i;
		}

}