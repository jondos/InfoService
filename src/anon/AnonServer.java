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
import java.net.InetAddress;
import java.net.UnknownServiceException;

public final class AnonServer implements Serializable
	{
		private String  m_strId    = null; //the ID od this Service
    private String  m_strHost  = null; //this may be the hostname or an ip string
		private String  m_strIP    =null; //this MUST be an IP-String, if not null
		private int     m_iPort  = -1;
		private String  m_strName  = null;
		private int     m_iProxyPort = -1;
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
            m_strHost = host;
            m_strIP=strip;
            if(m_strIP!=null&&m_strIP.trim().equals(""))
              m_strIP=null;
            m_iPort = port;
            m_iProxyPort = proxyport;
            if(id==null)
              {
                if(m_strIP==null) //try to get it from host
                  {
                    byte[] addr=null;
                    addr=InetAddress.getByName(m_strHost).getAddress();
                    String tmp=Integer.toString((int)addr[0]&0xFF)+"."+
                                              Integer.toString((int)addr[1]&0xFF)+"."+
                                              Integer.toString((int)addr[2]&0xFF)+"."+
                                              Integer.toString((int)addr[3]&0xFF);
                    m_strId=tmp+"%3A"+Integer.toString(m_iPort);
                  }
                else
                  m_strId=m_strIP+"%3A"+Integer.toString(m_iPort);
              }
            else
              m_strId=id;
          }
        catch(Exception e)
          {
            throw new UnknownServiceException("Wrong AnonService Info "+e.getMessage());
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

    /*public void setName(String m_strName)
      {
				m_strName=m_strName;
      }*/

    public int getPort()
      {
				return m_iPort;
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

		public int getStatus()
      {
				return m_iStatus;
		  }

		public void setStatus(int status)
      {
				m_iStatus=status;
		  }

		public void setStatus(String status)
      {

		  }

    public String toString()
      {
        return m_strName;
      }
	}
