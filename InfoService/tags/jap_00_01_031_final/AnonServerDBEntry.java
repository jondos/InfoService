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
import anon.JAPAnonService;
import java.io.Serializable;
public final class AnonServerDBEntry implements Serializable
	{
		private String  m_strHost  = null;
		private int     m_iPort  = -1;
		private String  m_strName  = null;
		private int     port2 = -1;
		private int     users = -1;
		private int     traffic = -1;
		private int     m_iRisk   = -1;
		private int     m_iAnonLevel =-1;
		private long    m_lMixedPackets = -1;
		private String delay  = null;
		private String status = null;
		private JAPAnonService service = null;

		public AnonServerDBEntry ()
			{
			}

		public AnonServerDBEntry (String h, int p)
			{
				m_strName = h+":"+Integer.toString(p);
				m_strHost = h;
				m_iPort = p;
			}

		public AnonServerDBEntry (String n,String h, int p)
			{
				m_strName = n;
				m_strHost = h;
				m_iPort = p;
			}

		public AnonServerDBEntry (String n,String h, int p, int p2)
			{
				m_strName = n;
				m_strHost = h;
				m_iPort = p;
				port2 = p2;
			}

		public boolean equals(AnonServerDBEntry e) {
		    if (
			m_strHost.equals(e.getHost()) &&
			(m_iPort == e.getPort()) &&
			(port2 == e.getSSLPort())
			)
		    	return true;
		    else
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
/* it's forbidden to change Portnumber
		public void setPort(int m_iPort) {
				this.m_iPort=m_iPort;
		}
*/
		public String getHost(){
				return m_strHost;
		}
/* it's forbidden to change Hostname
		public void setHost(String m_strHost) {
				this.m_strHost=m_strHost;
		}
*/
		public int getSSLPort() {
				return port2;
		}
/* dito.
		public void setSSLPort(int m_iPort) {
		    		this.port2=m_iPort;
		}
*/
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
		public JAPAnonService getAnonService() {
				return service;
		}
		public void setAnonService(JAPAnonService service) {
				this.service=service;
		}
	}
