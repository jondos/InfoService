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
import anon.AnonServiceFactory;
import anon.AnonChannel;
import anon.AnonServer;
import anon.ToManyOpenChannelsException;

import anon.server.AnonServiceImpl;
import JAPDebug;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.Socket;
import java.net.SocketException;
final public class AnonProxy implements Runnable
{
  public static final int E_SUCCESS=0;
  public static final int E_BIND=-2;
  private AnonService m_Anon;
  private Thread threadRun;
  private volatile boolean m_bIsRunning;
  private ServerSocket m_socketListener;
  private ProxyListener m_ProxyListener;
  private volatile int m_numChannels=0;
  private AnonServer m_AnonServer;
  public AnonProxy(ServerSocket listener)
    {
      m_socketListener=listener;
      m_Anon=AnonServiceFactory.create();
      m_Anon.setLogging(JAPDebug.create());
    }

  public void setAnonService(AnonServer server)
    {
      m_AnonServer=server;
    }

  public void setFirewall(String host,int port)
    {
      ((AnonServiceImpl)m_Anon).setFirewall(host,port);
    }

  public void setFirewallAuthorization(String id,String passwd)
    {
      ((AnonServiceImpl)m_Anon).setFirewallAuthorization(id,passwd);
    }

  public void setDummyTraffic(int msIntervall)
    {
      ((AnonServiceImpl)m_Anon).setDummyTraffic(msIntervall);
    }

  public int start()
    {
      m_numChannels=0;
      int ret=m_Anon.connect(m_AnonServer);
      if(ret!=ErrorCodes.E_SUCCESS)
        return ret;
      threadRun=new Thread(this);
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
                        newChannel=m_Anon.createChannel(AnonChannel.HTTP);
                        break;
                      }
                    catch(ToManyOpenChannelsException te)
                      {
                        JAPDebug.out(JAPDebug.ERR,JAPDebug.NET,"JAPAnonPrxy.run() ToMaynOpenChannelsExeption");
                        Thread.sleep(1000);
                      }
                  }
                if(newChannel!=null)
                  try
                    {
                      new Request(socket,newChannel);
                    }
                  catch(Exception e)
                    {
                      JAPDebug.out(JAPDebug.ERR,JAPDebug.NET,"JAPAnonPrxy.run() Exception: " +e);
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
      InputStream m_In;
      OutputStream m_Out;
      Socket m_clientSocket;
      Thread threadResponse;
      AnonChannel channel;
      Request(Socket clientSocket,AnonChannel c)
        {
          try{
          m_clientSocket=clientSocket;
          m_In=clientSocket.getInputStream();
          m_Out=c.getOutputStream();
          channel=c;
          //c.directOutputTo(m_clientSocket.getOutputStream());
          Thread t=new Thread(this);
          threadResponse=new Thread(new Response(clientSocket.getOutputStream(),c.getInputStream(),t,clientSocket));
          threadResponse.start();
          t.start();
           }
          catch(Exception e)
            {
            }
        }

       public void run()
          {
            incNumChannels();
            int len=0;
            byte[] buff=new byte[2900];
            try{
            while((len=m_In.read(buff,0,900))>0)
              {
                m_Out.write(buff,0,len);
                transferredBytes(len);
              }
              }
             catch(Exception e)
             {
              //e.printStackTrace();
             }
             try{channel.close();}catch(Exception e){}

             try{threadResponse.join(5000);}catch(Exception e){/*e.printStackTrace();*/}
            decNumChannels();

              //try{m_In.close();}catch(Exception e){}
              //try{m_Out.close();}catch(Exception e){}
              //try{m_clientSocket.close();}catch(Exception e){}

          }
}
final class Response implements Runnable
        {
          InputStream m_In1;
          OutputStream m_Out1;
          Thread threadConnection;
          Socket clientSocket;
          Response(OutputStream out,InputStream in,Thread t,Socket s)
          {
            m_In1=in;
            m_Out1=out;
            threadConnection=t;
            clientSocket=s;
             }

          public void run()
            {
              int len=0;
              byte[] buff=new byte[2900];
              try{
              while((len=m_In1.read(buff,0,1000))>0)
                {
                  m_Out1.write(buff,0,len);
                  transferredBytes(len);
                }
                }
              catch(Exception e)
                {}
              try{clientSocket.close();}catch(Exception e){}
              try{threadConnection.interrupt();}catch(Exception e){}
              }

        }

}