package anon.tor.test;

import anon.tor.Tor;
import anon.tor.TorAnonServerDescription;
import logging.*;
import java.util.Vector;

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
		v.addElement("peacetime");
		tor.setExitNodes(v);
		tor.initialize(new TorAnonServerDescription());
		tor.testDNS();
		tor.shutdown();
	}

}
