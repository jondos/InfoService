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

import java.net.InetAddress;
import org.w3c.dom.Node;
import org.w3c.dom.Element;
import org.w3c.dom.DocumentFragment;
import org.w3c.dom.Document;
import javax.xml.parsers.DocumentBuilderFactory;

import anon.server.impl.XMLUtil;
final public class ListenerInterface
	{
		public InetAddress m_Addr;
		public String m_strHost;
		public String m_strIP;
		public int m_iPort;
		public int m_Type;

		public  ListenerInterface(String host, String ip,int port) throws Exception
			{
				setListenerInterface(host, ip,port);
			}

		private void setListenerInterface(String host, String ip,int port) throws Exception
			{
				m_strIP=ip;
				m_strHost=host;
				m_Addr=null;
				try
					{
						m_Addr=InetAddress.getByName(ip);
					}
				catch(Throwable t)
					{
						m_Addr=null;
						m_strIP=null;
					}
				if(m_Addr==null)
					{
						try
							{
								m_Addr=InetAddress.getByName(host);
							}
						catch(Throwable t)
							{
								m_Addr=null;
							}
					}
				if(m_Addr==null||port<1||port>0xFFFF)
					throw new Exception("Invalid ListenerInterface");
				if(m_strHost==null)
					m_strHost=m_strIP;
				m_iPort=port;
				m_Type=-1;
			}

		public ListenerInterface(Node n) throws Exception
			{
				try
					{
						Element e=(Element)n;
						if(!e.getNodeName().equals("ListenerInterface"))
							throw new Exception("Invalid ListenerInterface");
						Node elem=XMLUtil.getFirstChildByName(e,"Host");
						String host=XMLUtil.parseNodeString(elem,null);
						elem=XMLUtil.getFirstChildByName(e,"IP");
						String ip=XMLUtil.parseNodeString(elem,null);
						elem=XMLUtil.getFirstChildByName(e,"Port");
						int port=XMLUtil.parseNodeInt(elem,-1);
						setListenerInterface(host,ip,port);
					}
				catch(Throwable t)
					{
						throw new Exception("Invalid ListenerInterface");
					}
			}

		public Node toXmlNode(Document owner)
			{
				try
					{
						DocumentFragment docFrag=owner.createDocumentFragment();
						Element elemListener=owner.createElement("ListenerInterface");
						docFrag.appendChild(elemListener);
						if(m_strIP!=null)
							{
								Element elemIP=owner.createElement("IP");
								XMLUtil.setNodeValue(elemIP,m_strIP);
								elemListener.appendChild(elemIP);
							}
						if(m_strHost!=null)
							{
								Element elemHost=owner.createElement("Host");
								XMLUtil.setNodeValue(elemHost,m_strHost);
								elemListener.appendChild(elemHost);
							}
						Element elemPort=owner.createElement("Port");
						XMLUtil.setNodeValue(elemPort,Integer.toString(m_iPort));
						elemListener.appendChild(elemPort);
						return docFrag;
					}
				catch(Throwable t)
					{
						return null;
					}
			}

	}
