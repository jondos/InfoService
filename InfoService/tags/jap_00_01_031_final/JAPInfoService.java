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
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.NamedNodeMap;
import HTTPClient.HTTPConnection;
import HTTPClient.HTTPResponse;
import HTTPClient.NVPair;
import HTTPClient.AuthorizationInfo;

import java.net.InetAddress;

import HTTPClient.Codecs;

final class JAPInfoService
	{
		private static final String DP = "%3A"; // Doppelpunkt

		JAPModel model;
		HTTPConnection conInfoService=null;
		public JAPInfoService()
			{
				model = JAPModel.getModel();
				setInfoService(model.getInfoServiceHost(),model.getInfoServicePort());
			}

		/** This will set the InfoService to use. It also sets the Proxy-Configuration and autorization.
		 */
		public int setInfoService(String host,int port)
			{
				conInfoService=new HTTPConnection(host,port);
				if(model.getUseFirewall())
					setProxy(model.getFirewallHost(),model.getFirewallPort(),
									 model.getFirewallAuthUserID(),model.getFirewallAuthPasswd());
				else
					setProxy(null,0,null,null);
				conInfoService.setAllowUserInteraction(false);
				conInfoService.setTimeout(10000);
				NVPair[] headers=new NVPair[2];
				headers[0]=new NVPair("Cache-Control","no-cache");
				headers[1]=new NVPair("Pragma","no-cache");
				replaceHeader(conInfoService,headers[0]);
			  replaceHeader(conInfoService,headers[1]);
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
						conInfoService.setDefaultHeaders(tmpHeaders);
						return 0;
					}
			}

		public int setProxy(String host,int port,String authUserID,String authPasswd)
			{
				if(conInfoService==null)
					return -1;
				conInfoService.setProxyServer(host,port);
				conInfoService.setCurrentProxy(host,port);
				if(authUserID!=null) //setting Proxy authorization...
					{
						String tmpPasswd=Codecs.base64Encode(authUserID+":"+authPasswd);
						NVPair authoHeader=new NVPair("Proxy-Authorization","Basic "+tmpPasswd);
						replaceHeader(conInfoService,authoHeader);
					}
				return 0;
			}

		public void fetchAnonServers() throws Exception
			{
				try
					{
						HTTPResponse resp=conInfoService.Get("/servers");
					// XML stuff
						Document doc=DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(resp.getInputStream());
						model.anonServerDatabase.clean();
						NodeList nodelist=doc.getElementsByTagName("MixCascade");
						for(int i=0;i<nodelist.getLength();i++)
							{
								Element elem=(Element)nodelist.item(i);
								NodeList nl=elem.getElementsByTagName("Name");
								String name=nl.item(0).getFirstChild().getNodeValue().trim();
								nl=elem.getElementsByTagName("IP");
								String ip=nl.item(0).getFirstChild().getNodeValue().trim();

								int port=JAPUtil.parseNodeInt(elem,"Port",-1);
								int proxyPort=JAPUtil.parseNodeInt(elem,"ProxyPort",-1);

								AnonServerDBEntry e=new AnonServerDBEntry(name,ip,port,proxyPort);

								nl=elem.getElementsByTagName("CurrentStatus");
								if(nl!=null&&nl.getLength()>0)
									{
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
								model.anonServerDatabase.addEntry(e);

							}
					}
				catch(Exception e)
					{
						throw e;
					}
			}
/*
	public void getFeedback() {
		AnonServerDBEntry service = new AnonServerDBEntry(null,model.anonHostName,model.anonPortNumber);
		this.getFeedback(service);
		model.nrOfActiveUsers  = service.getNrOfActiveUsers();
		model.trafficSituation = service.getTrafficSituation();
		model.currentRisk      = service.getCurrentRisk();
		model.mixedPackets     = service.getMixedPackets();
	}
*/

	public void getFeedback(AnonServerDBEntry service)
		{
			int nrOfActiveUsers = -1;
			int trafficSituation = -1;
			int currentRisk = -1;
			int mixedPackets = -1;
			int iAnonLevel=-1;
			try
				{
					byte[] addr=InetAddress.getByName(service.getHost()).getAddress();
					String strGET="/feedback/"+Integer.toString((int)addr[0]&0xFF)+"."+
																		 Integer.toString((int)addr[1]&0xFF)+"."+
																		 Integer.toString((int)addr[2]&0xFF)+"."+
																		 Integer.toString((int)addr[3]&0xFF)+DP+
																		 Integer.toString(service.getPort());
	//				JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"String GET: "+strGET);
					HTTPResponse resp=conInfoService.Get(strGET);
					if (resp.getStatusCode()!=200)
						{
							JAPDebug.out(JAPDebug.ERR,JAPDebug.NET,"JAPFeedback: Bad response from server: "+resp.getReasonLine());
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
			JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"JAPFeedback: "+nrOfActiveUsers+"/"+trafficSituation+"/"+currentRisk+"/"+mixedPackets+"/"+iAnonLevel);
		}


	public String getNewVersionNumber() throws Exception
		{
			try
				{
					HTTPResponse resp=conInfoService.Get(model.aktJAPVersionFN);
					if (resp==null || resp.getStatusCode()!=200)
						throw new Exception("Versioncheck bad response from server: "+resp.getReasonLine());
					// read remaining header lines
					byte[] buff=resp.getData();
					String s=new String(buff).trim();
					JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"New Version: "+s);
					if ( (s.charAt(2) == '.') && (s.charAt(5) == '.') )
						return s;
					throw new Exception("Versionfile has wrong format! Found: \""+s+ "\". Should be \"nn.nn.nnn\"!");
				}
			catch(Exception e)
				{
					throw new Exception("Versioncheck getNewVersionnumberFromNet() failed: "+e);
				}
		}

	}