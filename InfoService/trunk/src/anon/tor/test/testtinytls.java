
package anon.tor.test;

import java.io.IOException;

import logging.LogHolder;
import logging.SystemErrLog;

import anon.tor.FirstOnionRouterConnectionThread;
import anon.tor.tinytls.TinyTLS;

public class testtinytls {

	public static void main(String[] args) throws IOException{
		LogHolder.setLogInstance(new SystemErrLog());
		LogHolder.setDetailLevel(LogHolder.DETAIL_LEVEL_HIGHEST);
		FirstOnionRouterConnectionThread forcs= new FirstOnionRouterConnectionThread("192.168.0.222",443,1000);
//		TinyTLS s = new TinyTLS("192.168.0.222",443);
		TinyTLS s = forcs.getConnection();
		s.startHandshake();
		s.close();
	}
}
