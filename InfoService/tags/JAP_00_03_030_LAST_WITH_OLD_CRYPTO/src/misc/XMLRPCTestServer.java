package misc;

import anon.*;
import anon.xmlrpc.server.AnonServiceImplRemote;

import anon.infoservice.MixCascade;

public class XMLRPCTestServer
{
	public static void main(String[] args)
    {
      try{
      AnonService lokal=AnonServiceFactory.getAnonServiceInstance("AN.ON");
      AnonServiceImplRemote remote=new AnonServiceImplRemote(lokal);
      lokal.initialize(new MixCascade(null,null,"mix.inf.tu-dresden.de",6544));
      remote.startService();}
      catch(Exception e)
        {
          e.printStackTrace();
        }
    }
}
