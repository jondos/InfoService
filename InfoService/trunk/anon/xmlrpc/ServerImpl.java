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
package anon.xmlrpc;
import com.tm.xmlrpc.CallManager;
import com.tm.xmlrpc.TCPServer;
import com.tm.xmlrpc.SerializerFactory;
import java.net.InetAddress;

import anon.infoservice.InfoService;
import anon.AnonServer;
/**This is only a temporaly class and will be removed in the near future*/

public class ServerImpl extends Server
{
	private int m_ServerPort;
	final static int defaultServerPort=3333;
   public ServerImpl()
  {
		m_ServerPort=defaultServerPort;
  }

	public int start()
		{
			try
				{
					CallManager callManager = new CallManager();
					callManager.addHandler("ANONSERVICE", this);
					SerializerFactory.getInstance().addSerializer(
						      MixCascade.class,
						      new MixCascade()
						      );
					SerializerFactory.getInstance().addSerializer(
						      MixCascade[].class,
						      new MixCascade()
						      );
					// Create a WebServer to listen for requests.
					TCPServer server = new TCPServer(m_ServerPort,InetAddress.getByName("localhost"), callManager);
					// Start the WebServer.
					server.start();
					// Done!
				}
			catch(Exception e)
				{
					return -1;
				}
			return 0;
		}

	public void stop()
		{
		}

/*	public int getLocalListeningPort()
		{
			JAPController m=JAPController.getController();
			return m.getHTTPListenerPortNumber();
		}*/

	public MixCascade[] loadMixCascadesFromTheNet() throws Exception
		{
			/*JAPController m=JAPController.getController();
			InfoService info=m.getInfoService();
			AnonServer[] servers=info.getAvailableAnonServers();
			MixCascade[] cascades=new MixCascade[servers.length];
			for(int i=0;i<servers.length;i++)
				cascades[i]=new MixCascade(servers[i].getName(),servers[i].getHost(),servers[i].getPort());
			return cascades;*/
      return null;
		}

	public MixCascade getMixCascadeCurrentlyUsed() throws Exception
		{
    /*
			JAPController m=JAPController.getController();
		  AnonServer server=m.getAnonServer();
			return new MixCascade(server.getName(),server.getHost(),server.getPort());
      */
      return null;
		}

	public boolean setMixCascadeCurrentlyUsed(MixCascade cascade) throws Exception
		{
    /*
			JAPController m=JAPController.getController();
		  AnonServer server=new AnonServer(cascade.getName(),cascade.getHost(),cascade.getPort());
		  m.setAnonServer(server);
			if(!m.getAnonMode())
				m.setAnonMode(true);
		  return true;*/
      return false;
		}

}