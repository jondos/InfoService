/*
 * Created on Mar 16, 2004
 *
 */
package tor.tinytls;

import anon.crypto.JAPCertificate;

/**
 * @author stefan
 *
 * A abstract ciphersuite that can used TinyTLS
 * this is the parent class for all ciphersuites
 */
public abstract class CipherSuite{

	private byte[] ciphersuitecode;
	private Key_Exchange keyexchangealgorithm = null;
	private JAPCertificate servercertificate = null;

	/**
	 * writesequenznumber for packages
	 */
	protected int writesequenznumber;
	/**
	 * readsequenznumber for packages
	 */
	protected int readsequenznumber;
	/**
	 * client write key
	 */
	protected byte[] clientwritekey = null;
	/**
	 * client write mac secret
	 */
	protected byte[] clientmacsecret = null;
	/**
	 * client write IV, only used for block ciphers
	 */
	protected byte[] clientwriteIV = null;
	/**
	 * server write key
	 */
	protected byte[] serverwritekey = null;
	/**
	 * server write mac secret
	 */
	protected byte[] servermacsecret = null;
	/**
	 * server write IV, only used for block ciphers
	 */
	protected byte[] serverwriteIV = null;

	/**
	 * Constructor for a ciphersuite
	 * @param code Code of the ciphersuite (see RFC2246)
	 * @throws TLSException
	 */
	public CipherSuite(byte[] code) throws TLSException
	{
		if(code.length!=2)
		{
			throw new TLSException("wrong CipherSuiteCode ");
		}
		this.ciphersuitecode = code;
	}

	/**
	 * sets the key exchange algorithm
	 * @param ke Key Exchange Algorithm
	 */
	protected void setKeyExchangeAlgorithm(Key_Exchange ke)
	{
		this.keyexchangealgorithm = ke;
	}

	/**
	 * set the Server Certificate
	 * @param cert server certificate
	 */
	public void setServerCertificate(JAPCertificate cert)
	{
		this.servercertificate = cert;
	}

	/**
	 * returns the code of a ciphersuite (see RFC2246)
	 * @return ciphersuitecode
	 */
	public byte[] getCipherSuiteCode()
	{
		return this.ciphersuitecode;
	}

	/**
	 * process the key data that was recieved from the server (see RFC2246 Server Key Exchange)
	 * @param b key data from the server
	 * @param clientrandom clientrandom
	 * @param serverrandom serverrandom
	 * @throws TLSException
	 */
	public void serverKeyExchange(byte[] b, byte[] clientrandom, byte[] serverrandom) throws TLSException
	{
		if(this.keyexchangealgorithm!=null)
		{
			keyexchangealgorithm.serverKeyExchange(b,clientrandom,serverrandom,this.servercertificate);
		}
	}

	/**
	 * calculate the client keys (see RFC2246 Client Key Exchange)
	 * @return client key exchange message
	 * @throws TLSException
	 */
	public byte[] clientKeyExchange() throws TLSException
	{
		byte[] b = keyexchangealgorithm.clientKeyExchange();
		calculateKeys(this.keyexchangealgorithm.calculateKeys());
		this.writesequenznumber = 0;
		this.readsequenznumber = 0;
		return b;
	}

	/**
	 * calculate the client finished message (see RFC2246 Client Finished)
	 * @param handshakemessages all messages, that have been send before this message
	 * @return client finished message
	 * @throws TLSException
	 */
	public byte[] clientFinished(byte[] handshakemessages) throws TLSException
	{
		return keyexchangealgorithm.clientFinished(handshakemessages);
	}
	
	/**
	 * validates the finishedmessage and throws a Exception if a error occure
	 * @param finishedmessage the message that have to be valideted
	 * @throws TLSException
	 */
	public void serverFinished(byte[] header, byte[] finishedmessage) throws TLSException
	{
		keyexchangealgorithm.serverFinished(decode(header, finishedmessage));
	}

	/**
	 * encodes a message with a symmetric key
	 * @param message message
	 * @return encoded message
	 */
	public abstract byte[] encode(byte[] header, byte[] message);

	/**
	 * decodes a message with a symmetric key
	 * @param message message
	 * @return decoded message
	 */
	public abstract byte[] decode(byte[] header, byte[] message) throws TLSException;

	/**
	 * calculate server and client write keys (see RFC2246 TLS Record Protocoll)
	 * @param keys array of bytes(see RFC how it is calculated)
	 */
	protected abstract void calculateKeys(byte[] keys);

}
