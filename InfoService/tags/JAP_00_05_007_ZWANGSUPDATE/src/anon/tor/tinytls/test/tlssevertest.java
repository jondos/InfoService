package anon.tor.tinytls.test;

import java.io.FileInputStream;
import java.net.Socket;

import anon.crypto.JAPCertificate;
import anon.crypto.MyDSAPrivateKey;
import anon.crypto.PKCS12;
import anon.tor.tinytls.TinyTLSServer;
import logging.LogHolder;
import logging.SystemErrLog;

public class tlssevertest
{

	public static void main(String[] args) throws Exception
	{

		LogHolder.setLogInstance(new SystemErrLog());

		FileInputStream fs = new FileInputStream("/home/stefan/mykey.pfx");
		PKCS12 pkcs = PKCS12.getInstance(fs, "".toCharArray());
		MyDSAPrivateKey key = (MyDSAPrivateKey) pkcs.getPrivateKey();
		JAPCertificate cert = JAPCertificate.getInstance(pkcs.getX509Certificate());

		TinyTLSServer tlsserver = new TinyTLSServer(3456);
		tlsserver.setDSSParameters(cert, key);
		Socket tls = tlsserver.accept();
		tls.getOutputStream().write('H');
		tls.getOutputStream().write('I');
		tls.getOutputStream().write('!');
		tls.close();
		tlsserver.close();
	}
}
