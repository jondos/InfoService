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

import java.net.InetAddress;

final class JAPInfoService 
	{
		private static final String DP = "%3A"; // Doppelpunkt 

		JAPModel model;
		HTTPConnection conInfoService=null;
		public JAPInfoService() 
			{
				model = JAPModel.getModel();
				setInfoService(model.getInfoServiceHost(),model.getInfoServicePort());
				if(model.getUseProxy())
					setProxy(model.getProxyHost(),model.getProxyPort());
				else
					setProxy(null,0);
			}
			
		public int setInfoService(String host,int port)
			{
				String proxy=null;
 				int proxyport=0;
				if(conInfoService!=null)
					{ //get the old Proxy....
						proxy=conInfoService.getProxyHost();
						proxyport=conInfoService.getProxyPort();
					}
				conInfoService=new HTTPConnection(host,port);
				conInfoService.setProxyServer(proxy,proxyport);
				conInfoService.setAllowUserInteraction(false);
				NVPair[] headers=new NVPair[2];
				headers[0]=new NVPair("Proxy-Connection","");
				headers[1]=new NVPair("Pragma","no-cache");
				conInfoService.setDefaultHeaders(headers);
				return 0;
			}
		
		public int setProxy(String host,int port)
			{	
				if(conInfoService==null)
					return -1;
				conInfoService.setCurrentProxy(host,port);
				return 0;
			}
		
		public void fetchAnonServers() throws Exception
			{
				try
					{
						HTTPResponse resp=conInfoService.Get("/servers");
					// XML stuff
						Document doc=DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(resp.getInputStream());
						model.anonServerDatabase.removeAllElements();
						NodeList nodelist=doc.getElementsByTagName("MixCascade");
						for(int i=0;i<nodelist.getLength();i++)
							{
								Element elem=(Element)nodelist.item(i);
								NodeList nl=elem.getElementsByTagName("Name");
								String name=nl.item(0).getFirstChild().getNodeValue().trim();
								nl=elem.getElementsByTagName("IP");
								String ip=nl.item(0).getFirstChild().getNodeValue().trim();
								nl=elem.getElementsByTagName("Port");
								String port=nl.item(0).getFirstChild().getNodeValue().trim();
								nl=elem.getElementsByTagName("ProxyPort");
								String proxyPort=nl.item(0).getFirstChild().getNodeValue().trim();
								
								AnonServerDBEntry e=new AnonServerDBEntry(name,ip,Integer.parseInt(port),Integer.parseInt(proxyPort));
/*								
								nl=elem.getElementsByTagName("nrOfActiveUsers");
								String nrOfActiveUsers=nl.item(0).getFirstChild().getNodeValue().trim();
								e.setNrOfActiveUsers(Integer.parseInt(nrOfActiveUsers));
								nl=elem.getElementsByTagName("currentRisk");
								String currentRisk=nl.item(0).getFirstChild().getNodeValue().trim();
								e.setCurrentRisk(Integer.parseInt(currentRisk));
								nl=elem.getElementsByTagName("trafficSituation");
								String trafficSituation=nl.item(0).getFirstChild().getNodeValue().trim();
								e.setTrafficSituation(Integer.parseInt(trafficSituation));
								nl=elem.getElementsByTagName("mixedPackets");
								String mixedPackets=nl.item(0).getFirstChild().getNodeValue().trim();
								e.setMixedPackets(Integer.parseInt(mixedPackets));
*/													 
								model.anonServerDatabase.addElement(e);
								
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
			boolean error=false;
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
							error=true;
							JAPDebug.out(JAPDebug.ERR,JAPDebug.NET,"JAPFeedback: Bad response from server: "+resp.getReasonLine());
						}
					else
						{
							// XML stuff
							Document doc=DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(resp.getInputStream());
							NamedNodeMap n=doc.getDocumentElement().getAttributes();
											
							//s                = n.getNamedItem("anonServer").getNodeValue();
							nrOfActiveUsers  = Integer.valueOf(n.getNamedItem("nrOfActiveUsers").getNodeValue()).intValue();
							trafficSituation = Integer.valueOf(n.getNamedItem("currentRisk").getNodeValue()).intValue();
							currentRisk      = Integer.valueOf(n.getNamedItem("trafficSituation").getNodeValue()).intValue();
							mixedPackets     = Integer.valueOf(n.getNamedItem("mixedPackets").getNodeValue()).intValue();
						}
					// close streams and socket
				}
			catch(Exception e)
				{
					error = true;
					JAPDebug.out(JAPDebug.ERR,JAPDebug.NET,"JAPInfoService - Feedback: "+e);
				}
			if (!error)
				{
					service.setNrOfActiveUsers(nrOfActiveUsers);
					service.setTrafficSituation(trafficSituation);
					service.setCurrentRisk(currentRisk);
					service.setMixedPackets(mixedPackets);
				}
			else
				{
					service.setNrOfActiveUsers(-1);
					service.setTrafficSituation(-1);
					service.setCurrentRisk(-1);
					service.setMixedPackets(-1);
				}
			JAPDebug.out(JAPDebug.DEBUG,JAPDebug.MISC,"JAPFeedback: "+nrOfActiveUsers+"/"+trafficSituation+"/"+currentRisk+"/"+mixedPackets);
		}
		
	}