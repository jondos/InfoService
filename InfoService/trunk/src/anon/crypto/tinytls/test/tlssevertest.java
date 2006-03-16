package anon.crypto.tinytls.test;

import java.io.FileInputStream;
import java.net.Socket;

import anon.crypto.JAPCertificate;
import anon.crypto.MyDSAPrivateKey;
import anon.crypto.PKCS12;
import anon.crypto.tinytls.TinyTLSServer;
import logging.LogHolder;
import logging.SystemErrLog;
import java.net.ServerSocket;

import javax.net.ssl.*;
import java.security.*;
public class tlssevertest
{

	public static void main(String[] args) throws Exception
	{

		LogHolder.setLogInstance(new SystemErrLog());

/*		FileInputStream fs = new FileInputStream("testkey.pfx");
		PKCS12 pkcs = PKCS12.getInstance(fs, "".toCharArray());
		MyDSAPrivateKey key = (MyDSAPrivateKey) pkcs.getPrivateKey();
		JAPCertificate cert = JAPCertificate.getInstance(pkcs.getX509Certificate());

*/

	KeyStore store=KeyStore.getInstance("PKCS12");
	store.load(new FileInputStream("g:/projects/JAP/bi.pfx"),"654321".toCharArray());
	KeyManagerFactory fac=KeyManagerFactory.getInstance(KeyManagerFactory.getDefaultAlgorithm());
	fac.init(store,"654321".toCharArray());
	SSLContext context=SSLContext.getInstance("TLS");
	context.init(fac.getKeyManagers(),null,null);

	SSLServerSocket tlsserver = (SSLServerSocket)context.getServerSocketFactory().createServerSocket(3456);//  new SSLServerSocket(3456);//new TinyTLSServer(3456);
	System.out.println(tlsserver.getSupportedCipherSuites());
	System.out.println(tlsserver.getEnabledCipherSuites());
	//tlsserver.setDSSParameters(cert, key);
		while (true)
		{
			try
			{
				SSLSocket tls = (SSLSocket)tlsserver.accept();
				tls.startHandshake();
				System.out.println("Accepted");
//				Thread.sleep(10000000);
				tls.getOutputStream().write('H');
				tls.getOutputStream().write('I');
				tls.getOutputStream().write('!');
				tls.close();
			}
			catch (Exception e)
			{
				e.printStackTrace();
			}
			System.out.println("uups Accepted");
		}
	}
}
