
package anon.xmlrpc;
import com.tm.xmlrpc.CallManager;
import com.tm.xmlrpc.TCPServer;
import com.tm.xmlrpc.SerializerFactory;
import java.net.InetAddress;
import JAPModel;
import JAPInfoService;
import AnonServerDBEntry;
/**
 * Überschrift:
 * Beschreibung:
 * Copyright:     Copyright (c) 2001
 * Organisation:
 * @author
 * @version 1.0
 */
/**This is only a temporaly class and will be removed in the near future*/

public class Server
{
	private int m_ServerPort;
	final static int defaultServerPort=3333;
   public Server()
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

	public int getLocalListeningPort()
		{
			JAPModel m=JAPModel.getModel();
			return m.getHTTPListenerPortNumber();
		}

	public MixCascade[] loadMixCascadesFromTheNet() throws Exception
		{
			JAPModel m=JAPModel.getModel();
			JAPInfoService info=m.getInfoService();
			AnonServerDBEntry[] servers=info.getAvailableAnonServers();
			MixCascade[] cascades=new MixCascade[servers.length];
			for(int i=0;i<servers.length;i++)
				cascades[i]=new MixCascade(servers[i].getName(),servers[i].getHost(),servers[i].getPort());
			return cascades;
		}

	public MixCascade getMixCascadeCurrentlyUsed() throws Exception
		{
			JAPModel m=JAPModel.getModel();
		  AnonServerDBEntry server=m.getAnonServer();
			return new MixCascade(server.getName(),server.getHost(),server.getPort());
		}

	public boolean setMixCascadeCurrentlyUsed(MixCascade cascade) throws Exception
		{
			JAPModel m=JAPModel.getModel();
		  AnonServerDBEntry server=new AnonServerDBEntry(cascade.getName(),cascade.getHost(),cascade.getPort());
		  m.setAnonServer(server);
			if(!m.getAnonMode())
				m.setAnonMode(true);
		  return true;
		}

}