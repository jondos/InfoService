package anon.tor.tinytls.test;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.net.Socket;

import org.bouncycastle.asn1.DERConstructedSequence;
import org.bouncycastle.asn1.DERInputStream;
import org.bouncycastle.asn1.pkcs.RSAPrivateKeyStructure;

import anon.crypto.JAPCertificate;
import anon.crypto.MyRSAPrivateKey;
import anon.tor.tinytls.TinyTLSServer;
import anon.util.Base64;
import logging.LogHolder;
import logging.SystemErrLog;

public class tlssevertest {

	public static void main(String[] args) throws Exception 
	{

		LogHolder.setLogInstance(new SystemErrLog());

		JAPCertificate cert = JAPCertificate.getInstance("/home/stefan/tlscert.pem");
		
		MyRSAPrivateKey key = null;
		FileInputStream fs = new FileInputStream("/home/stefan/key.pem");
		byte[] b = new byte[fs.available()];
		fs.read(b);
		b = Base64.decode(b,0,b.length);
		DERInputStream dIn = new DERInputStream(new ByteArrayInputStream(b,0, b.length));
		try
		{
			RSAPrivateKeyStructure rsa = new RSAPrivateKeyStructure((DERConstructedSequence) dIn.readObject());
			key = new MyRSAPrivateKey(rsa.getModulus(),rsa.getPublicExponent(),rsa.getPrivateExponent(),rsa.getPrime1(),rsa.getPrime2(),rsa.getExponent1(),rsa.getExponent2(),rsa.getCoefficient());
		} catch (Exception ex)
		{
			ex.printStackTrace();
		}
		
		TinyTLSServer tlsserver = new TinyTLSServer(3456);
		tlsserver.setServerCertificate(cert);
		tlsserver.setServerPrivateKey(key);
		Socket tls = tlsserver.accept();
		tls.getOutputStream().write(20);
		tls.getOutputStream().write(21);
		tls.getOutputStream().write(22);
		Thread.sleep(5000);
		tls.close();
		tlsserver.close();
	}
}
