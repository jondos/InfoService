/*
Copyright (c) 2000 - 2004, The JAP-Team
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
package anon.server;

import anon.AnonService;
import anon.AnonChannel;
import anon.ErrorCodes;
import anon.AnonServiceEventListener;

import anon.server.impl.MuxSocket;
import anon.server.impl.KeyPool;
import anon.crypto.JAPCertificateStore;

import logging.Log;
import logging.DummyLog;

import anon.infoservice.MixCascade;
import java.net.InetAddress;
import java.net.ConnectException;
import java.util.Vector;
import java.util.Enumeration;
import java.io.InputStream;
import java.io.OutputStream;
final public class AnonServiceImpl implements AnonService
	{
		public static final int FIREWALL_TYPE_NONE         = 0;
		public static final int FIREWALL_TYPE_HTTP         = 1;
		public static final int FIREWALL_TYPE_SOCKS        = 2;

		//private static AnonServiceImpl m_AnonServiceImpl=null;
		private MuxSocket m_MuxSocket=null;
		private Vector m_AnonServiceListener;
		private int m_FirewallType;
		private String m_FirewallHost;
		private int m_FirewallPort;
		private String m_FirewallUserID;
		private String m_FirewallPasswd;
		private Log m_Log;

		private boolean m_bMixCertCheckEnabled;
		private JAPCertificateStore m_certsTrustedRoots;
		protected AnonServiceImpl()
			{
				m_AnonServiceListener=new Vector();
				m_FirewallType=FIREWALL_TYPE_NONE;
				m_FirewallHost=null;
				m_FirewallPort=-1;
				m_FirewallUserID=null;
				m_FirewallPasswd=null;
				m_Log=new DummyLog();
				m_bMixCertCheckEnabled=false;
				m_certsTrustedRoots=null;
				m_MuxSocket=MuxSocket.create(m_Log);
			}

		public static AnonService create()
			{
			/*  if(m_AnonServiceImpl==null)
					{
						m_AnonServiceImpl=new AnonServiceImpl();
					}
				return m_AnonServiceImpl;*/
				return new AnonServiceImpl();
			}

		public int connect(MixCascade mixCascade)
			{
				int ret=m_MuxSocket.connectViaFirewall(mixCascade,
																				m_FirewallType,m_FirewallHost,m_FirewallPort,
																				m_FirewallUserID,m_FirewallPasswd,
																				m_bMixCertCheckEnabled,m_certsTrustedRoots);
				if(ret!=ErrorCodes.E_SUCCESS)
					return ret;
				return m_MuxSocket.startService();
			}

		public void disconnect()
			{
				m_MuxSocket.stopService();
			}

		public AnonChannel createChannel(int type) throws ConnectException
			{
				return m_MuxSocket.newChannel(type);
			}

		public AnonChannel createChannel(InetAddress addr,int port) throws ConnectException
			{
				byte[]buff=new byte[13];
				AnonChannel c=null;
				try
					{
						c=createChannel(AnonChannel.SOCKS);
						InputStream in=c.getInputStream();
						OutputStream out=c.getOutputStream();
						buff[0]=5;
						buff[1]=1;
						buff[2]=0;

						buff[3]=5;
						buff[4]=1;
						buff[5]=0;
						buff[6]=1;
						System.arraycopy(addr.getAddress(),0,buff,7,4); //7,8,9,10
						buff[11]=(byte)(port>>8);
						buff[12]=(byte)(port&0xFF);
						out.write(buff,0,13);
						out.flush();
						int len=12;
						while(len>0)
							{
								len-=in.read(buff,0,len);
							}
					}
				catch(ConnectException ec)
					{
						throw ec;
					}
				catch(Exception e)
					{
						throw new ConnectException("createChannel(): "+e.getMessage());
					}
				if(buff[3]!=0)// failure!
					throw new ConnectException("SOCKS Server reports an error!");
				return c;
			}

		public synchronized void addEventListener(AnonServiceEventListener l)
			{
				Enumeration e=m_AnonServiceListener.elements();
				while(e.hasMoreElements())
					if(l.equals(e.nextElement()))
						return;
				m_AnonServiceListener.addElement(l);
			}

		public synchronized void removeEventListener(AnonServiceEventListener l)
			{
				m_AnonServiceListener.removeElement(l);
			}


		public void setLogging(Log log)
			{
				if(log==null)
					m_Log=new DummyLog();
				else
					m_Log=log;
				 m_MuxSocket.setLogging(m_Log);
			}
		//special local Service functions
		public void setDummyTraffic(int intervall)
			{
				m_MuxSocket.setDummyTraffic(intervall);
			}

		public void setFirewall(int type,String host,int port)
			{
				m_FirewallType=type;
				m_FirewallHost=host;
				m_FirewallPort=port;
				if(host==null)
					m_FirewallType=FIREWALL_TYPE_NONE;
			}

		public void setFirewallAuthorization(String user,String passwd)
			{
				m_FirewallUserID=user;
				m_FirewallPasswd=passwd;
			}

		public void setEnableMixCertificationCheck(boolean b)
			{
				m_bMixCertCheckEnabled=b;
			}

		public boolean isMixCertificationCheckEnabled()
			{
					return m_bMixCertCheckEnabled;
			}

		public void seteMixCertificationAuthorities(JAPCertificateStore trustedRoots)
			{
				m_certsTrustedRoots=trustedRoots;
			}

		public static void init()
			{
				KeyPool.start(new DummyLog());
			}
	}
