package anon.tor.test;

import java.util.Vector;

import anon.tor.Tor;
import anon.tor.TorAnonServerDescription;
import logging.LogHolder;
import logging.SystemErrLog;

/**
 * @author stefan
 */
public class DNSTest
{

	public static void main(String[] args) throws Exception
	{
		LogHolder.setLogInstance(new SystemErrLog());
		Tor tor = Tor.getInstance();
		Vector v = new Vector();
		tor.initialize(new TorAnonServerDescription());
		tor.testDNS();
		tor.shutdown();
	}

}
