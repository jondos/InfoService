package misc;

import anon.*;
import anon.xmlrpc.server.AnonServiceImplRemote;
public class XMLRPCTestServer
{
	public static void main(String[] args)
    {
      try{
      AnonService lokal=AnonServiceFactory.create();
      AnonServiceImplRemote remote=new AnonServiceImplRemote(lokal);
      lokal.connect(new AnonServer("mix.inf.tu-dresden.de",6544));
      remote.startService();}
      catch(Exception e)
        {
          e.printStackTrace();
        }
    }
}