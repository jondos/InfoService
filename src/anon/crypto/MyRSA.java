package anon.crypto;

import org.bouncycastle.crypto.engines.RSAEngine;
import org.bouncycastle.crypto.encodings.OAEPEncoding;

/** Encryption/Decryption using RSA*/
public class MyRSA
{
	RSAEngine m_RSAEngine;
	MyRSA()
	{
		m_RSAEngine = new RSAEngine();
	}

	/** inits the cipher for encryption*/
	public void init(MyRSAPublicKey key) throws Exception
	{
		synchronized (m_RSAEngine)
		{
			m_RSAEngine.init(true, key.getParams());
		}
	}

	/** encrypts one plaintext block using OAEP padding*/
	public byte[] encryptOAEP(byte[] plain, int offset, int len) throws Exception
	{
		synchronized (m_RSAEngine)
		{
			OAEPEncoding oaep = new OAEPEncoding(m_RSAEngine);
			return oaep.encodeBlock(plain, offset, len);
		}
	}

}
