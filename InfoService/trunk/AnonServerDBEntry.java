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
public final class AnonServerDBEntry 
	{
		private String host  = null;
		private int    port  = -1;
		private String name  = null;
		private int    port2 = -1;
		private int    users = -1;
		private int    traffic = -1;
		private int    risk    = -1;
		private int    packets = -1;
		private long   delay = -1;
		private String status = null;
		private JAPAnonService service = null;
	
		public AnonServerDBEntry (String n,String h, int p)
			{
				host = h;
				port = p;
				name=n;
				port2 = -1;
			}
			
		public AnonServerDBEntry (String n,String h, int p, int p2)
			{
				host = h;
				port = p;
				name=n;
				port2 = p2;
			}
		
		public String getName() {
				return name;//host+":"+Integer.toString(port);
		}
		public void setName(String name) {
				this.name=name;
		}
		public int getPort() {
				return port;
		}
		public void setPort(int port) {
				this.port=port;
		}
		public String getHost(){
				return host;
		}
		public void setHost(String host) {
				this.host=host;
		}
		public int getSSLPort() {
				return port2;
		}
		public void setSSLPort(int port) {
		    		this.port2=port;
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
		public int getCurrentRisk() {
				return risk;
		}		
		public void setCurrentRisk(int risk) {
				this.risk=risk;
		}
		public int getMixedPackets() {
				return packets;
		}		
		public void setMixedPackets(int packets) {
				this.packets=packets;
		}
		public long getDelay() {
				return delay;
		}
		public void setDelay(long delay) {
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
