/*
 * Created on Mar 29, 2004
 *
 */
package anon.tor.tinytls;

/**
 * @author stefan
 *
 * TLS_DHE_RSA_WITH_DES_CBC_SHA Ciphersuite that can be used in TinyTLS
 */
public class DHE_RSA_WITH_DES_CBC_SHA extends CipherSuite {

	/**
	 * Constuctor for the Ciphersuite
	 * @throws TLSException
	 */
	public DHE_RSA_WITH_DES_CBC_SHA() throws TLSException {
		super(new byte[]{0x00,0x15});
		this.setKeyExchangeAlgorithm(new DHE_RSA_Key_Exchange());
	}

	/**
	 * encodes a message
	 * @param header header
	 * @param message message to encode
	 * @return encoded message
	 */
	public void encode(TLSRecord msg) throws TLSException {
	}

	/**
	 * decodes a message with a symmetric key
	 * @param header header
	 * @param message message
	 * @return decoded message
	 */
	public void decode(TLSRecord msg) throws TLSException {
	}

	/**
	 * calculates the server and client write keys
	 * @param array of bytes that contains the keys (see RFC2246)
	 */
	protected void calculateKeys(byte[] keys) {
	}

}
