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
