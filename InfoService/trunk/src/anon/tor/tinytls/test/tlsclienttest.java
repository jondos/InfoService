package anon.tor.tinytls.test;

import logging.LogHolder;
import logging.SystemErrLog;

import anon.tor.tinytls.TinyTLS;

public class tlsclienttest {

	public static void main(String[] args) throws Exception{
		LogHolder.setLogInstance(new SystemErrLog());
		TinyTLS tls = new TinyTLS("localhost",3456);
		tls.startHandshake();
		System.out.println(tls.getInputStream().read());
		System.out.println(tls.getInputStream().read());
		System.out.println(tls.getInputStream().read());
		tls.close();
	}
}
