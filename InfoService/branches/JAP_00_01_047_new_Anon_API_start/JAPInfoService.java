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
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.DocumentBuilder;
import javax.swing.JProgressBar;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.NamedNodeMap;
import HTTPClient.HTTPConnection;
import HTTPClient.HTTPResponse;
import HTTPClient.NVPair;
//import HTTPClient.AuthorizationInfo;
import java.util.Enumeration;
import java.util.Vector;
import java.net.InetAddress;
import java.io.*;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.lang.Runnable;
import java.lang.Thread;

import HTTPClient.Codecs;
import HTTPClient.ModuleException;
import HTTPClient.ParseException;

final public class JAPInfoService
	{
		private static final String DP = "%3A"; // Doppelpunkt
		String proxyHost  = null;
		int    proxyPort  = 0;
		String proxyAuthUserID = null;
		String proxyAuthPasswd = null;
		HTTPConnection conInfoService=null;
                private int count =0;
                public boolean ready = false;

		public JAPInfoService(String host,int port) {
			this.setInfoService(host,port);
		}
		/** This will set the InfoService to use. It also sets the Proxy-Configuration and autorization.
		 */
		public int setInfoService(String host,int port) {
			//We are doing authorization on our own - so remove....
			try{conInfoService.removeDefaultModule(Class.forName("HTTPClient.AuthorizationModule"));}
			catch(Exception e){};
			conInfoService=new HTTPConnection(host,port);
			NVPair[] headers=new NVPair[2];
			headers[0]=new NVPair("Cache-Control","no-cache");
			headers[1]=new NVPair("Pragma","no-cache");
			replaceHeader(conInfoService,headers[0]);
			replaceHeader(conInfoService,headers[1]);
//				if(model.getUseFirewall())
//					setProxy(model.getFirewallHost(),model.getFirewallPort(),
//									 model.getFirewallAuthUserID(),model.getFirewallAuthPasswd());
//				else
//					setProxy(null,0,null,null);
			conInfoService.setAllowUserInteraction(false);
			conInfoService.setTimeout(10000);
			return 0;
		}
		public void setProxy(String proxyHost,int proxyPort,String proxyAuthUserID,String proxyAuthPasswd) {
			this.proxyHost  = proxyHost;
			this.proxyPort  = proxyPort;
			this.proxyAuthUserID = proxyAuthUserID;
			this.proxyAuthPasswd = proxyAuthPasswd;
			setProxyEnabled(true);
		}
		public int setProxyEnabled(boolean b) {
			String tmpProxyHost  = null;
			int    tmpProxyPort  = 0;
			String tmpAuthUserID = null;
			String tmpAuthPasswd = null;
			if(conInfoService==null)
				return -1;
			if(b) {
				tmpProxyHost  = proxyHost;
				tmpProxyPort  = proxyPort;
				tmpAuthUserID = proxyAuthUserID;
				tmpAuthPasswd = proxyAuthPasswd;
			}
			conInfoService.setProxyServer(tmpProxyHost,tmpProxyPort);
			conInfoService.setCurrentProxy(tmpProxyHost,tmpProxyPort);
			//setting Proxy authorization...
			if(tmpAuthUserID!=null) {
				String tmpPasswd=Codecs.base64Encode(tmpAuthUserID+":"+tmpAuthPasswd);
				NVPair authoHeader=new NVPair("Proxy-Authorization","Basic "+tmpPasswd);
				replaceHeader(conInfoService,authoHeader);
			}
			return 0;
		}
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
		public AnonServerDBEntry[] getAvailableAnonServers() throws Exception {
			Vector v = new Vector();
			try {
				HTTPResponse resp=conInfoService.Get("/servers");
				try {
					Enumeration enum=resp.listHeaders();
					JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"HTTPResponse: "+Integer.toString(resp.getStatusCode()));
					while(enum.hasMoreElements()) {
						String header=(String)enum.nextElement();
						JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,header+": "+resp.getHeader(header));
					}
				} catch(Throwable tor) {
				}
				// XML stuff
				Document doc=DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(resp.getInputStream());
//				model.anonServerDatabase.clean();
				NodeList nodelist=doc.getElementsByTagName("MixCascade");
				for(int i=0;i<nodelist.getLength();i++) {
					Element elem=(Element)nodelist.item(i);
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
					int port=JAPUtil.parseNodeInt(elem,"Port",-1);
					int proxyPort=JAPUtil.parseNodeInt(elem,"ProxyPort",-1);

					AnonServerDBEntry e=new AnonServerDBEntry(name,host,ip,port,proxyPort);

					nl=elem.getElementsByTagName("CurrentStatus");
					if(nl!=null&&nl.getLength()>0) {
						Element elem1=(Element)nl.item(0);
						int nrOfActiveUsers=JAPUtil.parseElementAttrInt(elem1,"ActiveUsers",-1);
						e.setNrOfActiveUsers(nrOfActiveUsers);
						int currentRisk=JAPUtil.parseElementAttrInt(elem1,"CurrentRisk",-1);
						e.setCurrentRisk(currentRisk);
						int trafficSituation=JAPUtil.parseElementAttrInt(elem1,"TrafficSituation",-1);
						e.setTrafficSituation(trafficSituation);
						int mixedPackets=JAPUtil.parseElementAttrInt(elem1,"MixedPackets",-1);
						e.setMixedPackets(mixedPackets);
					}
					v.addElement(e);
//					model.anonServerDatabase.addEntry(e);
				}
			} catch(Exception e) {
				throw e;
			}
			if((v==null)||(v.size()==0)) {
				return null;
			} else {
				AnonServerDBEntry[] db = new AnonServerDBEntry[v.size()];
				for(int i=0;i<db.length;i++) {
					db[i]=(AnonServerDBEntry)v.elementAt(i);
				}
				return db;
			}
		}

	public void getFeedback(AnonServerDBEntry service)
		{
			int nrOfActiveUsers = -1;
			int trafficSituation = -1;
			int currentRisk = -1;
			int mixedPackets = -1;
			int iAnonLevel=-1;
			try
				{
					String strIP=service.getIP();
					byte[] addr=null;
					if(strIP==null) //try to get it from host
						{
							addr=InetAddress.getByName(service.getHost()).getAddress();
						  strIP=Integer.toString((int)addr[0]&0xFF)+"."+
																		 Integer.toString((int)addr[1]&0xFF)+"."+
																		 Integer.toString((int)addr[2]&0xFF)+"."+
																		 Integer.toString((int)addr[3]&0xFF);
						}
					String strGET="/feedback/"+strIP+DP+Integer.toString(service.getPort());
					HTTPResponse resp=conInfoService.Get(strGET);
                                        if (resp.getStatusCode()!=200)
						{
							JAPDebug.out(JAPDebug.ERR,JAPDebug.NET,"JAPInfoService:Bad response from server: "+resp.getReasonLine());
						}
					else
						{
							// XML stuff
							Document doc=DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(resp.getInputStream());
							Element root=doc.getDocumentElement();
							nrOfActiveUsers   = JAPUtil.parseElementAttrInt(root,"nrOfActiveUsers",-1);
							trafficSituation  = JAPUtil.parseElementAttrInt(root,"trafficSituation",-1);
							currentRisk       = JAPUtil.parseElementAttrInt(root,"currentRisk",-1);
							mixedPackets      = JAPUtil.parseElementAttrInt(root,"mixedPackets",-1);
							iAnonLevel        = JAPUtil.parseElementAttrInt(root,"anonLevel",-1);
						}
					// close streams and socket
				}
			catch(Exception e)
				{
					JAPDebug.out(JAPDebug.ERR,JAPDebug.NET,"JAPInfoService - Feedback: "+e);
				}
			service.setNrOfActiveUsers(nrOfActiveUsers);
			service.setTrafficSituation(trafficSituation);
			service.setCurrentRisk(currentRisk);
			service.setMixedPackets(mixedPackets);
			service.setAnonLevel(iAnonLevel);
			JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"JAPInfoService:"+nrOfActiveUsers+"/"+trafficSituation+"/"+currentRisk+"/"+mixedPackets+"/"+iAnonLevel);
		}
	public String getNewVersionNumber() throws Exception
		{
			try
				{
					HTTPResponse resp=conInfoService.Get("/aktVersion");
					if (resp==null || resp.getStatusCode()!=200)
						throw new Exception("Versioncheck bad response from server: "+resp.getReasonLine());
					// read remaining header lines
					byte[] buff=resp.getData();
					String s=new String(buff).trim();
					JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"JAPInfoService:New Version: "+s);
					if ( (s.charAt(2) == '.') && (s.charAt(5) == '.') )
						return s;
					throw new Exception("Versionfile has wrong format! Found: \""+s+ "\". Should be \"nn.nn.nnn\"!");
				}
			catch(Exception e)
				{
					throw new Exception("Versioncheck failed: "+e);
				}
		}

           /////////////////////////////////////////////////////////////////////
           //connect for JAPUpdate .jnlp-Files

        public HTTPConnection getConInfoService()
          {
             return this.conInfoService;
          }
// inner class LoadThread needed for showing Download-Progress using JProgressBar

//public class LoadThread extends Thread{

//  public void run(){}
          // get the jarfiles
  public void connect(String codeBase, String jarPath, String jarFileName, JProgressBar progressBar)
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
                   count ++;
                   progressBar.setValue(count);
                   progressBar.repaint();
                 //  System.out.println(count);
                   fos.write(data);
                 }
              fos.flush();
              fos.close();
              is.close();
              ready = true;
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
  public int getCount()
  {
    return count;
  }
           /////////////////////////////////////////////////////////////////////
	public static void main(String[] argv) {
		JAPDebug.setDebugLevel(JAPDebug.WARNING);
		JAPInfoService is=new JAPInfoService("infoservice.inf.tu-dresden.de",6543);
//		is.setInfoService("infoservice.inf.tu-dresden.de",6543);
		is.setProxy("www-proxy.t-online.de",80,null,null);
		is.setProxyEnabled(false/*true*/);
		try {
			System.out.println("Version:"+is.getNewVersionNumber());
			AnonServerDBEntry[] d = is.getAvailableAnonServers();
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

}