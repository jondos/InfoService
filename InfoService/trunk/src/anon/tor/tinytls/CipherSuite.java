/*
 Copyright (c) 2004, The JAP-Team
 All rights reserved.
 Redistribution and use in source and binary forms, with or without modification,
 are permitted provided that the following conditions are met:

 - Redistributions of source code must retain the above copyright notice,
   this list of conditions and the following disclaimer.

 - Redistributions in binary form must reproduce the above copyright notice,
   this list of conditions and the following disclaimer in the documentation and/or
  other materials provided with the distribution.

 - Neither the name of the University of Technology Dresden, Germany nor the names of its contributors
   may be used to endorse or promote products derived from this software without specific
  prior written permission.


 THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS ``AS IS'' AND ANY EXPRESS
 OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY
 AND FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS
 BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER
 IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY
 OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE
 */
/*
 * Created on Mar 16, 2004
 *
 */
package anon.tor.tinytls;

import java.math.BigInteger;

import anon.crypto.JAPCertificate;

/**
 * @author stefan
 *
 * A abstract ciphersuite that can used TinyTLS
 * this is the parent class for all ciphersuites
 */
public abstract class CipherSuite{

	private byte[] m_ciphersuitecode;
	protected String m_ciphersuitename = "Name not set";
	private Key_Exchange m_keyexchangealgorithm = null;
	private JAPCertificate m_servercertificate = null;

	/**
	 * writesequenznumber for packages
	 */
	protected long m_writesequenznumber;
	/**
	 * readsequenznumber for packages
	 */
	protected long m_readsequenznumber;
	/**
	 * client write key
	 */
	protected byte[] m_clientwritekey = null;
	/**
	 * client write mac secret
	 */
	protected byte[] m_clientmacsecret = null;
	/**
	 * client write IV, only used for block ciphers
	 */
	protected byte[] m_clientwriteIV = null;
	/**
	 * server write key
	 */
	protected byte[] m_serverwritekey = null;
	/**
	 * server write mac secret
	 */
	protected byte[] m_servermacsecret = null;
	/**
	 * server write IV, only used for block ciphers
	 */
	protected byte[] m_serverwriteIV = null;

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
		this.m_ciphersuitecode = code;
		this.m_writesequenznumber = 0;
		this.m_readsequenznumber = 0;
	}

	/**
	 * sets the key exchange algorithm
	 * @param ke Key Exchange Algorithm
	 */
	protected void setKeyExchangeAlgorithm(Key_Exchange ke)
	{
		this.m_keyexchangealgorithm = ke;
	}
	
	public Key_Exchange getKeyExchangeAlgorithm()
	{
		return this.m_keyexchangealgorithm;
	}

	/**
	 * set the Server Certificate
	 * @param cert server certificate
	 */
	public void setServerCertificate(JAPCertificate cert)
	{
		this.m_servercertificate = cert;
	}

	/**
	 * returns the code of a ciphersuite (see RFC2246)
	 * @return ciphersuitecode
	 */
	public byte[] getCipherSuiteCode()
	{
		return this.m_ciphersuitecode;
	}

	public void processClientKeyExchange(BigInteger dh_y)
	{
		this.m_keyexchangealgorithm.processClientKeyExchange(dh_y);
		calculateKeys(this.m_keyexchangealgorithm.calculateKeys(),false);
		
	}

	/**
	 * calculate the client keys (see RFC2246 Client Key Exchange)
	 * @return client key exchange message
	 * @throws TLSException
	 */
	public byte[] calculateClientKeyExchange() throws TLSException
	{
		byte[] b = this.m_keyexchangealgorithm.calculateClientKeyExchange();
		calculateKeys(this.m_keyexchangealgorithm.calculateKeys(),true);
		return b;
	}

	/**
	 * validates the finishedmessage and throws a Exception if a error occure
	 * @param finishedmessage the message that have to be valideted
	 * @throws TLSException
	 */
	public void processServerFinished(TLSRecord msg,byte[] handshakemessages) throws TLSException
	{
		decode(msg);
		m_keyexchangealgorithm.processServerFinished(msg.m_Data,msg.m_dataLen,handshakemessages);
	}

	/**
	 * encodes a message with a symmetric key
	 * @param message message
	 * @return encoded message
	 */
	public abstract void encode(TLSRecord msg) throws TLSException;

	/**
	 * decodes a message with a symmetric key
	 * @param message message
	 * @return decoded message
	 */
	public abstract void decode(TLSRecord msg) throws TLSException;

	/**
	 * calculate server and client write keys (see RFC2246 TLS Record Protocoll)
	 * @param keys array of bytes(see RFC how it is calculated)
	 */
	protected abstract void calculateKeys(byte[] keys,boolean forclient);

	public String toString()
	{
		return m_ciphersuitename;
	}

}
