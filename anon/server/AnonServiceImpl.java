package anonnew.server;

import anonnew.AnonService;
import anonnew.AnonServer;
import anonnew.AnonChannel;
import anonnew.AnonServiceEventListener;

import anonnew.server.impl.MuxSocket;
import java.net.ConnectException;
import java.util.Vector;
import java.util.Enumeration;

final public class AnonServiceImpl implements AnonService
  {
    private static AnonServiceImpl m_AnonServiceImpl=null;
    private MuxSocket m_MuxSocket=null;
    private Vector m_AnonServiceListener;
    private String m_FirewallHost;
    private int m_FirewallPort;
    private String m_FirewallUserID;
    private String m_FirewallPasswd;

    protected AnonServiceImpl()
      {
        m_AnonServiceListener=new Vector();
        m_FirewallHost=null;
        m_FirewallPort=-1;
        m_FirewallUserID=null;
        m_FirewallPasswd=null;
        m_MuxSocket=MuxSocket.create();

      }

    public static AnonService create()
      {
        if(m_AnonServiceImpl==null)
          {
            m_AnonServiceImpl=new AnonServiceImpl();
          }
        return m_AnonServiceImpl;
      }

    public void connect(AnonServer anonService)
      {
        m_MuxSocket.connectViaFirewall(anonService.getHost(),anonService.getPort(),
                                        m_FirewallHost,m_FirewallPort,
                                        m_FirewallUserID,m_FirewallPasswd);
        m_MuxSocket.startService();
      }

    public void disconnect(){}

    public AnonChannel createChannel(int type) throws ConnectException
      {
        return m_MuxSocket.newChannel(type);
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


    //special local Service functions
    public void setEnableDummyTraffic(boolean b)
      {
        m_MuxSocket.setEnableDummyTraffic(b);
      }

    public void setFirewall(String host,int port)
      {
        m_FirewallHost=host;
        m_FirewallPort=port;
      }

    public void setFirewallAuthorization(String user,String passwd)
      {
        m_FirewallUserID=user;
        m_FirewallPasswd=passwd;
      }

    public static void init()
      {
        anonnew.server.impl.KeyPool.start();
      }
  }
