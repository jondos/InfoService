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
package anon;

import java.io.Serializable;
import java.net.UnknownServiceException;
import java.net.InetAddress;
import java.util.Vector;

import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Document;
import javax.xml.parsers.DocumentBuilderFactory;

import anon.server.impl.XMLUtil;

public final class AnonServer implements Serializable
	{
		private String  m_strId    = null; //the ID od this Service
		private ListenerInterface[] m_ListenerInterfaces=null;
		private String  m_strName  = null;
		private int     m_iUsers = -1;
		private int     m_iTraffic = -1;
		private int     m_iRisk   = -1;
		private int     m_iAnonLevel =-1;
		private long    m_lMixedPackets = -1;
		private String  m_strDelay  = null;
		private int     m_iStatus = 0;

		public AnonServer (String host, int port) throws UnknownServiceException
			{
				this(null,null,host,null,port,-1);
			}

		public AnonServer (String name,String host, int port) throws UnknownServiceException
			{
				this(null,name,host,null,port,-1);
			}

		public AnonServer (String id,String name,String host,String strip, int port, int proxyport) throws UnknownServiceException
			{
				if(host==null||(port==-1&&proxyport==-1))
					throw new UnknownServiceException("Wrong AnonService Info");
				if(port==-1)
					port=proxyport;
				try
					{
						if(name==null)
							m_strName = host+":"+Integer.toString(port);
						else
							m_strName = name;
						if(proxyport>0)
							{
								m_ListenerInterfaces=new ListenerInterface[2];
								m_ListenerInterfaces[1]=new ListenerInterface(host,strip,proxyport);
							}
						else
							m_ListenerInterfaces=new ListenerInterface[1];
						m_ListenerInterfaces[0]=new ListenerInterface(host,strip,port);
						if(id==null)
							{
								if(strip==null) //try to get it from host
									{
										byte[] addr=null;
										addr=InetAddress.getByName(host).getAddress();
										String tmp=Integer.toString((int)addr[0]&0xFF)+"."+
																							Integer.toString((int)addr[1]&0xFF)+"."+
																							Integer.toString((int)addr[2]&0xFF)+"."+
																							Integer.toString((int)addr[3]&0xFF);
										m_strId=tmp+"%3A"+Integer.toString(port);
									}
								else
									m_strId=strip+"%3A"+Integer.toString(port);
							}
						else
							m_strId=id;
					}
				catch(Exception e)
					{
						throw new UnknownServiceException("Wrong AnonService Info "+e.getMessage());
					}
			}

		public AnonServer(Node n) throws UnknownServiceException
			{
				try
					{
						Element elem=(Element)n;
						m_strId=elem.getAttribute("id");
						NodeList nl=elem.getElementsByTagName("Name");
						m_strName=nl.item(0).getFirstChild().getNodeValue().trim();
						nl=elem.getElementsByTagName("ListenerInterface");
						Vector listeners=new Vector();
						for(int i=0;i<nl.getLength();i++)
							{
								try
									{
										ListenerInterface l=new ListenerInterface(nl.item(i));
										listeners.addElement(l);
									}
								catch(Throwable t)
									{
									}
							}
						if(listeners.size()<1)
							throw new UnknownServiceException("No useable ListenerInterface Info");
						m_ListenerInterfaces=new ListenerInterface[listeners.size()];
						listeners.copyInto(m_ListenerInterfaces);
						nl=elem.getElementsByTagName("CurrentStatus");
						if(nl!=null&&nl.getLength()>0)
							{
								Element elem1=(Element)nl.item(0);
								int nrOfActiveUsers=XMLUtil.parseElementAttrInt(elem1,"ActiveUsers",-1);
								setNrOfActiveUsers(nrOfActiveUsers);
								int currentRisk=XMLUtil.parseElementAttrInt(elem1,"CurrentRisk",-1);
								setCurrentRisk(currentRisk);
								int trafficSituation=XMLUtil.parseElementAttrInt(elem1,"TrafficSituation",-1);
								setTrafficSituation(trafficSituation);
								int mixedPackets=XMLUtil.parseElementAttrInt(elem1,"MixedPackets",-1);
								setMixedPackets(mixedPackets);
							}
					}
				catch(Throwable t)
					{
						throw new UnknownServiceException("Wrong AnonServiceInfo: "+t.getMessage());
					}
			}


		public boolean equals(AnonServer e) //TODO: Buggy!
			{
				if(e==null)
					return false;
				return m_strId.equals(e.getID());
			}

		public String getID()
			{
				return m_strId;
			}

		public String getName()
			{
				return m_strName;
			}

		public ListenerInterface[] getListenerInterfaces()
			{
				return m_ListenerInterfaces;
			}
		/*public void setName(String m_strName)
			{
				m_strName=m_strName;
			}*/

/*    public int getPort()
			{
				return m_ListenerInterfaces[0].m_iPort;
			}

		public String getHost()
			{
				return m_strHost;
			}

		public String getIP()
			{
				return m_strIP;
			}

		public int getSSLPort()
			{
				return m_iProxyPort;
			}
*/
		public int getNrOfActiveUsers()
			{
				return m_iUsers;
			}

		public void setNrOfActiveUsers(int users)
			{
				m_iUsers=users;
			}

		public int getTrafficSituation()
			{
				return m_iTraffic;
			}

		public void setTrafficSituation(int traffic)
			{
				m_iTraffic=traffic;
			}

		public int getAnonLevel()
			{
				return m_iAnonLevel;
			}

		public void setAnonLevel(int iAnonLevel)
			{
				m_iAnonLevel=iAnonLevel;
			}

		public int getCurrentRisk()
			{
				return m_iRisk;
			}

		public void setCurrentRisk(int risk)
			{
				m_iRisk=risk;
			}

		public long getMixedPackets()
			{
				return m_lMixedPackets;
			}

		public void setMixedPackets(int m_lMixedPackets)
			{
				m_lMixedPackets=m_lMixedPackets;
			}

		public String getDelay()
			{
				return m_strDelay;
			}

		public void setDelay(String delay)
			{
				m_strDelay=delay;
			}

		public String toString()
			{
				return m_strName;
			}

		public Node toXmlNode(Document owner)
			{
				try
					{

						DocumentFragment docFrag=owner.createDocumentFragment();
						Element elemCascade=owner.createElement("MixCascade");
						docFrag.appendChild(elemCascade);
						if(m_strId!=null)
							elemCascade.setAttribute("id",m_strId);
						if(m_strName!=null)
							{
								Element elemName=owner.createElement("Name");
								XMLUtil.setNodeValue(elemName,m_strName);
								elemCascade.appendChild(elemName);
							}
						Element elemNetwork=owner.createElement("Network");
						elemCascade.appendChild(elemNetwork);
						Element elemListeners=owner.createElement("ListenerInterfaces");
						elemNetwork.appendChild(elemListeners);
						if(m_ListenerInterfaces!=null)
							for(int i=0;i<m_ListenerInterfaces.length;i++)
								{
									elemListeners.appendChild(m_ListenerInterfaces[i].toXmlNode(owner));
								}
						return docFrag;
					}
				catch(Throwable t)
					{
						return null;
					}
			}
	}
