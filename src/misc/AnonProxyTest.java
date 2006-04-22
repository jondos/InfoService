package misc;

import java.net.ServerSocket;
import anon.proxy.AnonProxy;
import anon.infoservice.MixCascade;
import logging.LogHolder;
import logging.SystemErrLog;
import logging.LogType;
import logging.LogLevel;
import anon.crypto.SignatureVerifier;

public class AnonProxyTest
{
	public static void main(String[] args)
	{
		try
		{
			//just to ensure that we see some debug messages...
			SystemErrLog log=new SystemErrLog();
			log.setLogType(LogType.ALL);
			log.setLogLevel(LogLevel.DEBUG);
			LogHolder.setLogInstance(new SystemErrLog());
			ServerSocket ss = new ServerSocket(4005);
			AnonProxy theProxy = new AnonProxy(ss, null);

			//we need to disbale certificate checks (better: set valid root certifcates for productive environments!)
			SignatureVerifier.getInstance().setCheckSignatures(false);

			theProxy.setMixCascade(new MixCascade(null, null, "mix.inf.tu-dresden.de", 6544));
			theProxy.start();
			synchronized(theProxy)
				{
					theProxy.wait();
				}
		}
		catch (Exception e)
		{
			System.out.println(e);
		}
	}
}
