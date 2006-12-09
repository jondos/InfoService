
package anon.xmlrpc;
import com.tm.xmlrpc.TCPTransport;
import com.tm.xmlrpc.Transport;
import com.tm.xmlrpc.Call;
import com.tm.xmlrpc.Response;
import com.tm.xmlrpc.SerializerFactory;
/**
 * �berschrift:
 * Beschreibung:
 * Copyright:     Copyright (c) 2001
 * Organisation:
 * @author
 * @version 1.0
 */
/**This is only a temporaly class and will be removed in the near future*/
public class Client
{
	final static int defaultPort=3333;
  private int m_ServerPort;
	public Client()
    {
			m_ServerPort=defaultPort;
    }

	   // Configuration for the local computer
    public int getLocalListeningPort()   throws Exception
			{
				try
					{
					  Transport trans=new TCPTransport("localhost",m_ServerPort);
						Call call=new Call(trans);
						call.setMethodName("ANONSERVICE.getLocalListeningPort");
						Response resp=call.sendCall();
						if(resp.isFault())
							throw new Exception("Error!");
					  Object o=resp.getResponse();
						if(!(o instanceof Integer))
							throw new Exception("Error!");
						return ((Integer)o).intValue();
					}
				catch(Exception e)
					{
						throw e;
					}
			}
 /*   public boolean getLocallyListeningOnly() throws RemoteException;
    public boolean setLocalListeningPort(int port) throws RemoteException;
    public boolean setLocallyListeningOnly(boolean listenOnlyLocally) throws RemoteException;

    // Configuration for the Info Service
    public String  getInfoServiceServerName() throws RemoteException;
    public int     getInfoServiceServerPort() throws RemoteException;
    public boolean setInfoServiceServerName(String name, int port) throws RemoteException;
*/
    // Configuration for the mixCascades
    public MixCascade[] loadMixCascadesFromTheNet() throws Exception
			{
				try
					{
					  Transport trans=new TCPTransport("localhost",m_ServerPort);
						Call call=new Call(trans);
						call.setMethodName("ANONSERVICE.loadMixCascadesFromTheNet");
						Response resp=call.sendCall();
						if(resp.isFault())
							throw new Exception("Error!");
					  Object o=resp.getResponse();
					  return (MixCascade[])new MixCascade().deserialize(resp.getResponse(), MixCascade.class);

				}
				catch(Exception e)
					{
						throw e;
					}
			}

		public MixCascade getMixCascadeCurrentlyUsed() throws Exception
			{
			try
					{
					  Transport trans=new TCPTransport("localhost",m_ServerPort);
						Call call=new Call(trans);
						call.setMethodName("ANONSERVICE.getMixCascadeCurrentlyUsed");
						Response resp=call.sendCall();
						if(resp.isFault())
							throw new Exception("Error!");
					  Object o=resp.getResponse();
					  return (MixCascade)new MixCascade().deserialize(resp.getResponse(), MixCascade.class);

				}
				catch(Exception e)
					{
						throw e;
					}
		  }

		public boolean setMixCascadeCurrentlyUsed(MixCascade mixCascade) throws Exception
			{
				try
					{
						SerializerFactory.getInstance().addSerializer(
						MixCascade.class,
						new MixCascade()
						);

					  Transport trans=new TCPTransport("localhost",m_ServerPort);
						Call call=new Call(trans);
						call.setMethodName("ANONSERVICE.setMixCascadeCurrentlyUsed");
						call.addParameter(mixCascade);
						Response resp=call.sendCall();
						if(resp.isFault())
							throw new Exception("Error!");
					  Object o=resp.getResponse();
					  return ((Boolean)o).booleanValue();
				}
				catch(Exception e)
					{
						throw e;
					}
			}
    // Configuration for a proxy
   /* public String  getProxyServerName() throws RemoteException;
    public int     getProxyServerPort() throws RemoteException;
    public boolean setProxyServerName(String name, int port) throws RemoteException;
*/}