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
package proxy;
import java.net.ServerSocket;
import anon.ErrorCodes;
import anon.AnonService;
import anon.AnonServiceEventListener;
import anon.AnonServiceFactory;
import anon.AnonChannel;

import anon.ToManyOpenChannelsException;
import anon.NotConnectedToMixException;

import anon.infoservice.MixCascade;
import anon.server.AnonServiceImpl;
import anon.crypto.JAPCertificateStore;
import jap.JAPDebug;
import jap.JAPConstants;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.Socket;
import java.net.SocketException;
final public class AnonProxy implements Runnable/*,AnonServiceEventListener*/
{
	public static final int E_SUCCESS=0;
	public static final int E_BIND=-2;
	public final static int E_MIX_PROTOCOL_NOT_SUPPORTED=ErrorCodes.E_MIX_PROTOCOL_NOT_SUPPORTED;

	// ootte
	public final static int E_INVALID_KEY = ErrorCodes.E_INVALID_KEY;
	public final static int E_SIGNATURE_CHECK_FIRSTMIX_FAILED = ErrorCodes.E_SIGNATURE_CHECK_FIRSTMIX_FAILED;
	public final static int E_SIGNATURE_CHECK_OTHERMIX_FAILED = ErrorCodes.E_SIGNATURE_CHECK_OTHERMIX_FAILED;
	public final static int E_INVALID_CERTIFICATE = ErrorCodes.E_INVALID_CERTIFICATE;
	// ootte

	private AnonService m_Anon;
	private Thread threadRun;
	private volatile boolean m_bIsRunning;
	private ServerSocket m_socketListener;
	private ProxyListener m_ProxyListener;
	private volatile int m_numChannels=0;
	
	/**
   * Stores the MixCascade we are connected to.
   */
  private MixCascade currentMixCascade;
  
	private boolean m_bAutoReconnect=false;
	public AnonProxy(ServerSocket listener)
		{
			m_socketListener=listener;
			m_Anon=AnonServiceFactory.create();
			m_Anon.setLogging(JAPDebug.create());
			setFirewall(JAPConstants.FIREWALL_TYPE_HTTP,null,-1);
			setFirewallAuthorization(null,null);
			setDummyTraffic(-1);
		}

 /**
   * Sets a new MixCascade.
   *
   * @param newMixCascade The new MixCascade we are connected to.
   */
  public void setMixCascade(MixCascade newMixCascade) {
    currentMixCascade = newMixCascade;
  }

	public void setFirewall(int type,String host,int port)
		{
			if(type==JAPConstants.FIREWALL_TYPE_SOCKS)
				type=AnonServiceImpl.FIREWALL_TYPE_SOCKS;
			else
				type=AnonServiceImpl.FIREWALL_TYPE_HTTP;
			((AnonServiceImpl)m_Anon).setFirewall(type,host,port);
		}

	public void setFirewallAuthorization(String id,String passwd)
		{
			((AnonServiceImpl)m_Anon).setFirewallAuthorization(id,passwd);
		}

	public void setDummyTraffic(int msIntervall)
		{
			((AnonServiceImpl)m_Anon).setDummyTraffic(msIntervall);
		}

	public void setAutoReConnect(boolean b)
		{
			m_bAutoReconnect=b;
		}

	public void setMixCertificationCheck(boolean enabled,JAPCertificateStore trustedRoots)
		{
			((AnonServiceImpl)m_Anon).seteMixCertificationAuthorities(trustedRoots);
			((AnonServiceImpl)m_Anon).setEnableMixCertificationCheck(enabled);
		}

	public int start()
		{
			m_numChannels=0;
			int ret=m_Anon.connect(currentMixCascade);
			if(ret!=ErrorCodes.E_SUCCESS)
				return ret;
			threadRun=new Thread(this,"JAP - AnonProxy");
			threadRun.start();
			return E_SUCCESS;
		}

	public void stop()
		{
			m_Anon.disconnect();
			m_bIsRunning=false;
			try{threadRun.join();}catch(Exception e){}
		}

	public void setProxyListener(ProxyListener l)
		{
			m_ProxyListener=l;
		}

	public void run()
			{
				m_bIsRunning=true;
				int oldTimeOut=0;
				try{oldTimeOut=m_socketListener.getSoTimeout();}catch(Exception e){}
				try
					{
						m_socketListener.setSoTimeout(2000);
					}
				catch(Exception e1)
					{
						JAPDebug.out(JAPDebug.DEBUG,JAPDebug.NET,"Could not set accept time out: Exception: "+e1.getMessage());
					}
				try
					{
						while(m_bIsRunning)
							{
								Socket socket=null;
								try
									{
										socket = m_socketListener.accept();
									}
								catch(InterruptedIOException e)
									{
										continue;
									}
								try
									{
										socket.setSoTimeout(0); //ensure that socket is blocking!
									}
								catch(SocketException soex)
									{
										socket=null;
										JAPDebug.out(JAPDebug.ERR,JAPDebug.NET,"JAPAnonProxy.run() Could not set non-Blocking mode for Channel-Socket! Exception: " +soex);
										continue;
									}
								//2001-04-04(HF)
								AnonChannel newChannel=null;
								while(m_bIsRunning)
									{
										try
											{
												newChannel=null;
												newChannel=m_Anon.createChannel(AnonChannel.HTTP);
												break;
											}
										catch(ToManyOpenChannelsException te)
											{
												JAPDebug.out(JAPDebug.ERR,JAPDebug.NET,"JAPAnonProxy.run() ToManyOpenChannelsExeption");
												Thread.sleep(1000);
											}
										catch(NotConnectedToMixException ec)
											{
												JAPDebug.out(JAPDebug.ERR,JAPDebug.NET,"JAPAnonProxy.run() Connection to Mix lost");
												if(!m_bAutoReconnect)
													{
														m_bIsRunning=false;
														break;
													}
												while(m_bIsRunning&&m_bAutoReconnect)
													{
														JAPDebug.out(JAPDebug.ERR,JAPDebug.NET,"JAPAnonProxy.run() Try reconnect to Mix");
														int ret=m_Anon.connect(currentMixCascade);
														if(ret==ErrorCodes.E_SUCCESS)
															break;
														Thread.sleep(10000);
													}
											}
									}
								if(newChannel!=null)
									try
										{
											new Request(socket,newChannel);
										}
									catch(Exception e)
										{
											JAPDebug.out(JAPDebug.ERR,JAPDebug.NET,"JAPAnonPrxoy.run() Exception: " +e);
										}
							}
					}
				catch (Exception e)
					{
						JAPDebug.out(JAPDebug.ERR,JAPDebug.NET,"JAPProxyServer:ProxyServer.run1() Exception: " +e);
					}
				try{m_socketListener.setSoTimeout(oldTimeOut);}catch(Exception e4){}
				JAPDebug.out(JAPDebug.INFO,JAPDebug.NET,"JAPAnonProxyServer stopped.");
				m_bIsRunning=false;
			}

	protected synchronized void incNumChannels()
		{
			m_numChannels++;
			m_ProxyListener.channelsChanged(m_numChannels);
		}

	protected synchronized void decNumChannels()
		{
			m_numChannels--;
			m_ProxyListener.channelsChanged(m_numChannels);
		}

	protected synchronized void transferredBytes(int bytes)
		{
			m_ProxyListener.transferedBytes(bytes);
		}

final class Request  implements Runnable
		{
			InputStream m_InChannel;
			OutputStream m_OutChannel;
			InputStream m_InSocket;
			OutputStream m_OutSocket;
			Socket m_clientSocket;
			Thread m_threadResponse;
			Thread m_threadRequest;
			AnonChannel m_Channel;
			volatile boolean m_bRequestIsAlive;

			Request(Socket clientSocket,AnonChannel c)
				{
					try{
					m_clientSocket=clientSocket;
					m_clientSocket.setSoTimeout(1000); //just to ensure that threads will stop
					m_InSocket=clientSocket.getInputStream();
					m_OutSocket=clientSocket.getOutputStream();
					m_InChannel=c.getInputStream();
					m_OutChannel=c.getOutputStream();
					m_Channel=c;
					m_threadRequest=new Thread(this,"JAP - AnonProxy Request");
					m_threadResponse=new Thread(new Response(),"JAP - AnonProxy Response");
					m_threadResponse.start();
					m_threadRequest.start();
					 }
					catch(Exception e)
						{
						}
				}

			 public void run()
					{
						m_bRequestIsAlive=true;
						incNumChannels();
						int len=0;
						byte[] buff=new byte[1900];
						try
							{
								for(;;)
									{
										try
											{
												len=m_InSocket.read(buff,0,900);
											}
										catch(InterruptedIOException ioe)
											{
												continue;
											}
										if(len<=0)
											break;
										m_OutChannel.write(buff,0,len);
										transferredBytes(len);
									}
							}
						 catch(Exception e)
							{
								//e.printStackTrace();
							}
						m_bRequestIsAlive=false;
						try{m_Channel.close();}catch(Exception e){}
						//if(m_bResponseIsAlive)
						//  {
						//  }
						//try{m_threadResponse.interrupt();}catch(Exception e){}
						 //try{threadResponse.join(5000);}catch(Exception e){
						 //e.printStackTrace();}
						decNumChannels();
					}
		final class Response implements Runnable
				{
					Response()
					{
					 }

					public void run()
						{
							int len=0;
							byte[] buff=new byte[2900];
							try{
								while((len=m_InChannel.read(buff,0,1000))>0)
									{
										int count=0;
										for(;;)
											{
												try
													{
														m_OutSocket.write(buff,0,len);
														break;
													}
												catch(InterruptedIOException ioe)
													{
														JAPDebug.out(JAPDebug.EMERG,JAPDebug.NET,"Should never be here: Timeout in sending to Browser!");
													}
												count++;
												if(count>3)
													throw new Exception("Could not send to Browser...");
											}
										transferredBytes(len);
									}
								}
							catch(Exception e)
								{}
							try{m_clientSocket.close();}catch(Exception e){}
							try{Thread.sleep(500);}catch(Exception e){}
							if(m_bRequestIsAlive)
								try{m_threadRequest.interrupt();}catch(Exception e){}
							}

				}
	}
}
