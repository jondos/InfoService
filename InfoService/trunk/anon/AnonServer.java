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
public final class AnonServer implements Serializable
	{
		private String  m_strHost  = null; //this may be the hostname or an ip string
		private String  m_strIP    =null; //this MUST be an IP-String, if not null
		private int     m_iPort  = -1;
		private String  m_strName  = null;
		private int     m_iProxyPort = -1;
		private int     users = -1;
		private int     traffic = -1;
		private int     m_iRisk   = -1;
		private int     m_iAnonLevel =-1;
		private long    m_lMixedPackets = -1;
		private String delay  = null;
		private String status = null;
		//private JAPAnonService service = null;

		public AnonServer (String host, int port)
			{
				this(null,host,null,port,-1);
			}

		public AnonServer (String name,String host, int port)
			{
				this(name,host,null,port,-1);
			}

		public AnonServer (String name,String host,String strip, int port, int proxyport)
			{
				if(name==null)
				  m_strName = host+":"+Integer.toString(port);
				m_strName = name;
				m_strHost = host;
				m_strIP=strip;
				if(m_strIP!=null&&m_strIP.trim().equals(""))
					m_strIP=null;
				m_iPort = port;
				m_iProxyPort = proxyport;

			}

		public boolean equals(AnonServer e) //TODO: Buggy!
			{
		    if( m_strHost.equalsIgnoreCase(e.getHost()) &&
					  m_iPort == e.getPort() &&
					  m_iProxyPort == e.getSSLPort())
						return true;
			  return false;
		  }

		public String getName() {
				return m_strName;
		}
		public void setName(String m_strName) {
				this.m_strName=m_strName;
		}
		public int getPort() {
				return m_iPort;
		}
		public String getHost(){
				return m_strHost;
		}

		public String getIP()
			{
				return m_strIP;
			}

		public int getSSLPort() {
				return m_iProxyPort;
		}
		public int getNrOfActiveUsers() {
				return users;
		}
		public void setNrOfActiveUsers(int users) {
				this.users=users;
		}
		public int getTrafficSituation() {
				return traffic;
		}
		public void setTrafficSituation(int traffic) {
				this.traffic=traffic;
		}
		public int getAnonLevel() {
				return m_iAnonLevel;
		}
		public void setAnonLevel(int iAnonLevel) {
				m_iAnonLevel=iAnonLevel;
		}

		public int getCurrentRisk() {
				return m_iRisk;
		}
		public void setCurrentRisk(int risk) {
				m_iRisk=risk;
		}
		public long getMixedPackets() {
				return m_lMixedPackets;
		}
		public void setMixedPackets(int m_lMixedPackets) {
				this.m_lMixedPackets=m_lMixedPackets;
		}
		public String getDelay() {
				return delay;
		}
		public void setDelay(String delay) {
				this.delay=delay;
		}
		public String getStatus() {
				return status;
		}
		public void setStatus(String status) {
				this.status=status;
		}
/*		public JAPAnonService getAnonService() {
				return service;
		}
		public void setAnonService(JAPAnonService service) {
				this.service=service;
		}*/
	}
