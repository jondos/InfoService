package anon.tor.tinytls.test;

import java.io.InputStream;

import logging.LogHolder;
import logging.SystemErrLog;
import anon.tor.tinytls.TinyTLS;

public class tlsclienttest {

	public static void main(String[] args) throws Exception{
		LogHolder.setLogInstance(new SystemErrLog());
		TinyTLS tls = new TinyTLS("localhost",3456);
//		TinyTLS tls = new TinyTLS("anon.inf.tu-dresden.de",443);
		tls.checkRootCertificate(false);
		tls.startHandshake();
//		OutputStream out=tls.getOutputStream();
//		out.write("GET /index.html HTTP/1.0\r\n\r\n".getBytes());
//		out.flush();
		InputStream in=tls.getInputStream();
		int i;
		try
		{
			while((i=in.read())>0)
				System.out.print((char)i);
		}
		catch(Exception e)
		{
		}
		tls.close();
	}
}
