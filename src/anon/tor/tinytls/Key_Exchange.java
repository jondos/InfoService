/*
 * Created on Mar 29, 2004
 *
 */
package anon.tor.tinytls;

import anon.crypto.JAPCertificate;

/**
 * @author stefan
 *
 * Abstract Class which is performs the key exchange
 */
public abstract class Key_Exchange{

	/**
	 * Constructor
	 *
	 */
	public Key_Exchange()
	{
	}

	/**
	 * Decode the server keys and check the certificate
	 * @param bytes server keys
	 * @param clientrandom clientrandom
	 * @param serverrandom serverrandom
	 * @param servercertificate servercertificate
	 * @throws TLSException
	 */
	public abstract void  serverKeyExchange(byte[] b, byte[] clientrandom, byte[] serverrandom,JAPCertificate cert) throws TLSException;

	/**
	 * checks the server finished message
	 * @param b server finished message
	 * @throws TLSException
	 */
	public abstract void serverFinished(byte[] b,int len) throws TLSException;

	/**
	 * generates the client key exchange message (see RFC2246)
	 * @return client key exchange message
	 * @throws TLSException
	 */
	public abstract byte[] clientKeyExchange() throws TLSException;

	/**
	 * generate the client finished message (see RFC2246)
	 * @param handshakemessages all handshakemessages that have been send before this
	 * @return client finished message
	 */
	public abstract byte[] clientFinished(byte[] handshakemessages) throws TLSException;

	/**
	 * calculates the key material (see RFC2246 TLS Record Protocoll)
	 * @return key material
	 */
	public abstract byte[] calculateKeys();

}
