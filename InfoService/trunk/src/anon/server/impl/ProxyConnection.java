package anon.server.impl;

import java.net.Socket;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.SocketException;
import anon.server.AnonServiceImpl;
import HTTPClient.Codecs;
import logging.Log;
import logging.LogLevel;
import logging.LogType;

final public class ProxyConnection
  {
 		private final static int FIREWALL_METHOD_HTTP_1_1=11;
		private final static int FIREWALL_METHOD_HTTP_1_0=10;
		private static final String CRLF="\r\n";

    private Socket m_ioSocket;
    private InputStream m_In;
    private OutputStream m_Out;
    private Log m_Log;
    public ProxyConnection(Log log, int fwType,String fwHost,int fwPort,
                            String fwUserID,String fwPasswd,
                            String host, int port)
                            throws Exception
      {
        m_Log=log;
        if(fwType==AnonServiceImpl.FIREWALL_TYPE_NONE)
          m_ioSocket=new Socket(host,port);
        else
          {
 			      m_Log.log(LogLevel.DEBUG,LogType.NET,"ProxyConnection: Try to connect via Firewall ("+fwHost+":"+fwPort+") to Server ("+host+":"+port+")");
            m_ioSocket=new Socket(fwHost,fwPort);
          }
        m_ioSocket.setSoTimeout(10000);
        m_In=m_ioSocket.getInputStream();
        m_Out=m_ioSocket.getOutputStream();
        if(fwType==AnonServiceImpl.FIREWALL_TYPE_SOCKS)
          {
            doSOCKS(host,port);
          }
        else if(fwType==AnonServiceImpl.FIREWALL_TYPE_HTTP)
          {
            OutputStreamWriter writer=new OutputStreamWriter(m_Out);
            try
              {
                sendHTTPProxyCommands(FIREWALL_METHOD_HTTP_1_1,writer,host,port,fwUserID,fwPasswd);
              }
            catch(Exception e1)
              {
                sendHTTPProxyCommands(FIREWALL_METHOD_HTTP_1_0,writer,host,port,fwUserID,fwPasswd);
              }
            String tmp=readLine(m_In);
            m_Log.log(LogLevel.DEBUG,LogType.NET,"ProxyConnection: Firewall response is: "+tmp);
            if(tmp.indexOf("200")!=-1)
              {
                while(!(tmp=readLine(m_In)).equals(""))
                  m_Log.log(LogLevel.DEBUG,LogType.NET,"ProxyConnection: Firewall response is: "+tmp);
              }
            else
              throw new Exception("HTTP-Proxy response: "+tmp);
          }
        m_ioSocket.setSoTimeout(0);
      }

    private void doSOCKS(String host,int port) throws Exception
      {
        byte[] buff=new byte[10+host.length()];
        buff[0]=5; //SOCKS Version 5
        buff[1]=1; //NO Auth
        buff[2]=0;

        buff[3]=5;
        buff[4]=1; //CMD=Connect
        buff[5]=0; //RSV
        buff[6]=3; //Addr=Host-String
        buff[7]=(byte)host.length();

        System.arraycopy(host.getBytes(),0,buff,8,host.length());
        buff[8+host.length()]=(byte)(port>>8);
        buff[9+host.length()]=(byte)(port&0xFF);
        m_Out.write(buff,0,10+host.length());
        m_Out.flush();
        int len=12;
        while(len>0)
          {
            len-=m_In.read(buff,0,len);
          }
      }


    public Socket getSocket()
      {
        return m_ioSocket;
      }

    public InputStream getInputStream()
      {
        return m_In;
      }

    public OutputStream getOutputStream()
      {
        return m_Out;
      }

    public void setSoTimeout(int ms) throws SocketException
      {
        m_ioSocket.setSoTimeout(ms);
      }

    public void close()
      {
        try{m_In.close();}catch(Exception e){}
        try{m_Out.close();}catch(Exception e){}
        try{m_ioSocket.close();}catch(Exception e){}
      }

    		//Write stuff for connecting over proxy/firewall
		// should look like this example
		//   CONNECT www.inf.tu-dresden.de:443 HTTP/1.0
		//   Connection: Keep-Alive
		//   Proxy-Connection: Keep-Alive
		//differs a little bit for HTTP/1.0 and HTTP/1.1
		private void sendHTTPProxyCommands(int httpMethod,OutputStreamWriter out,String host,int port,String user,String passwd)
			throws Exception
			{
        if(httpMethod==FIREWALL_METHOD_HTTP_1_1)
          out.write("CONNECT "+host+":"+Integer.toString(port)+" HTTP/1.1"+CRLF);
        else
          out.write("CONNECT "+host+":"+Integer.toString(port)+" HTTP/1.0"+CRLF);
        if(user!=null&&passwd!=null) // proxy authentication required...
          {
            String str=Codecs.base64Encode(user+":"+passwd);
            out.write("Proxy-Authorization: Basic "+str+CRLF);
          }
        out.write("Connection: Keep-Alive"+CRLF);
        out.write("Keep-Alive: max=20, timeout=100"+CRLF);
        out.write("Proxy-Connection: Keep-Alive"+CRLF);
        out.write(CRLF);
        out.flush();
			}

	  private String readLine(InputStream inputStream) throws Exception
		  {
				StringBuffer strBuff=new StringBuffer(256);
				try
					{
						int byteRead = inputStream.read();
						while (byteRead != 10 && byteRead != -1)
							{
								if (byteRead != 13)
									strBuff.append((char)byteRead);
								byteRead = inputStream.read();
							}
					}
				catch (Exception e)
					{
						throw e;
					}
				return strBuff.toString();
			}
  }