package anon.tor.tinytls;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

import anon.crypto.JAPCertificate;
import anon.crypto.MyRSAPrivateKey;

/**
 * @author stefan
 */
public class TinyTLSServer extends ServerSocket {

	private JAPCertificate m_Certificate = null;
	private MyRSAPrivateKey m_PrivateKey = null;

	public TinyTLSServer(int port) throws IOException {
		super(port);
	}
	
	public void setServerCertificate(JAPCertificate cert)
	{
		m_Certificate = cert;
	}
	
	public void setServerPrivateKey(MyRSAPrivateKey key)
	{
		m_PrivateKey = key;
	}
	
	public Socket accept() throws IOException
	{
		if(m_Certificate==null)
		{
			throw new TLSException("No ServerCertificate set");
		}
		if(m_PrivateKey==null)
		{
			throw new TLSException("No ServerPrivateKey set");
		}
		Socket s = super.accept();

		TinyTLSServerSocket tls;
		tls = new TinyTLSServerSocket(s,m_Certificate,m_PrivateKey);
		tls.startHandshake();
		return tls;
	}

}
