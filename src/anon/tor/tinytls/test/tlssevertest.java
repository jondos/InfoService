package anon.tor.tinytls.test;

import java.io.FileInputStream;
import java.net.Socket;

import anon.crypto.JAPCertificate;
import anon.crypto.MyDSAPrivateKey;
import anon.crypto.PKCS12;
import anon.tor.tinytls.TinyTLSServer;
import logging.LogHolder;
import logging.SystemErrLog;

public class tlssevertest {

	public static void main(String[] args) throws Exception 
	{

		LogHolder.setLogInstance(new SystemErrLog());

		
		 FileInputStream fs = new FileInputStream("/home/stefan/mykey.pfx");
		PKCS12 pkcs = PKCS12.load(fs,"".toCharArray());
		MyDSAPrivateKey key = (MyDSAPrivateKey)pkcs.getPrivKey();
		JAPCertificate cert = JAPCertificate.getInstance(pkcs.getX509cert());

		TinyTLSServer tlsserver = new TinyTLSServer(3456);
		tlsserver.setDSSParameters(cert,key);
		Socket tls = tlsserver.accept();
		tls.getOutputStream().write('H');
		tls.getOutputStream().write('I');
		tls.getOutputStream().write('!');
		tls.close();
		tlsserver.close();
	}
}
