/*
 * Created on Mar 16, 2004
 *
 */
package tor.tinytls;

/**
 * @author stefan
 *	
 */
public class DHE_RSA_WITH_AES_128_CBC_SHA extends CipherSuite {
	
	/**
	 * Constructor
	 * @throws Exception
	 */
	public DHE_RSA_WITH_AES_128_CBC_SHA() throws Exception
	{
		super(new byte[]{0x00,0x033});
		this.setKeyExchangeAlgorithm(new DHE_RSA_Key_Exchange());
	}

	/**
	 * encode a message with a symmetric key
	 * @param header header
	 * @param message message
	 * @return encoded message
	 */
	public byte[] encode(byte[] header, byte[] message) 
	{
		return null;
	}

	/**
	 * decodes a message with a symmetric key
	 * @param header header
	 * @param message message
	 * @return decoded message
	 */
	public byte[] decode(byte[] header, byte[] message) throws TLSException {
		return null;
	}

	/**
	 * extract the keys (see RFC 2246 TLS Record Protocoll)
	 * @param keys array of bytes that contains the keys
	 */
	protected void calculateKeys(byte[] keys) 
	{
	
	}

}
