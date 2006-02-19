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
package anon.crypto.tinytls.ciphersuites;

import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.engines.DESedeEngine;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import anon.crypto.MyRandom;
import anon.crypto.tinytls.TLSException;
import anon.crypto.tinytls.TLSRecord;
import anon.crypto.tinytls.keyexchange.DHE_RSA_Key_Exchange;
import anon.util.ByteArrayUtil;

/**
 * @author stefan
 *
 */
public class DHE_RSA_WITH_3DES_CBC_SHA extends CipherSuite
{

	private CBCBlockCipher m_encryptcipher;
	private MyRandom m_rand;

	/**
	 * Constuctor
	 * @throws Exception
	 */
	public DHE_RSA_WITH_3DES_CBC_SHA() throws TLSException
	{
		super(new byte[]
			  {0x00, 0x16});
		m_ciphersuitename = "TLS_DHE_RSA_WITH_3DES_CBC_SHA";
		this.setKeyExchangeAlgorithm(new DHE_RSA_Key_Exchange());
		m_rand = new MyRandom();
	}

	public void encode(TLSRecord msg)
	{
		HMac hmac = new HMac(new SHA1Digest());
		hmac.reset();
		hmac.init(new KeyParameter(m_clientmacsecret));
		hmac.update(ByteArrayUtil.inttobyte(m_writesequenznumber, 8), 0, 8);
		m_writesequenznumber++;
		hmac.update(msg.m_Header, 0, msg.m_Header.length);
		hmac.update(msg.m_Data, 0, msg.m_dataLen);
		hmac.doFinal(msg.m_Data, msg.m_dataLen);
		msg.m_dataLen += hmac.getMacSize();
		//add padding as described in RFC2246 (6.2.3.2)
		int paddingsize = m_rand.nextInt(240);
		paddingsize = paddingsize +
			(m_encryptcipher.getBlockSize() -
			 ( (msg.m_dataLen + 1 + paddingsize) % m_encryptcipher.getBlockSize()));
		for (int i = 0; i < paddingsize + 1; i++)
		{
			msg.m_Data[msg.m_dataLen++] = (byte) paddingsize;
		}
		for (int i = 0; i < msg.m_dataLen; i += m_encryptcipher.getBlockSize())
		{
			this.m_encryptcipher.processBlock(msg.m_Data, i, msg.m_Data, i);
		}
		msg.setLength(msg.m_dataLen);
	}


	protected void calculateKeys(byte[] keys, boolean forclient)
	{
		this.m_clientwritekey = ByteArrayUtil.copy(keys, 40, 24);
		this.m_serverwritekey = ByteArrayUtil.copy(keys, 64, 24);
		this.m_clientwriteIV = ByteArrayUtil.copy(keys, 88, 8);
		this.m_serverwriteIV = ByteArrayUtil.copy(keys, 96, 8);
		if (forclient)
		{
			this.m_clientmacsecret = ByteArrayUtil.copy(keys, 0, 20);
			this.m_servermacsecret = ByteArrayUtil.copy(keys, 20, 20);
			this.m_encryptcipher = new CBCBlockCipher(new DESedeEngine());
			this.m_encryptcipher.init(true,
									  new ParametersWithIV(new KeyParameter(this.m_clientwritekey), this.m_clientwriteIV));
			this.m_decryptcipher = new CBCBlockCipher(new DESedeEngine());
			this.m_decryptcipher.init(false,
									  new ParametersWithIV(new KeyParameter(this.m_serverwritekey), this.m_serverwriteIV));
		}
		else
		{
			this.m_servermacsecret = ByteArrayUtil.copy(keys, 0, 20);
			this.m_clientmacsecret = ByteArrayUtil.copy(keys, 20, 20);
			this.m_encryptcipher = new CBCBlockCipher(new DESedeEngine());
			this.m_encryptcipher.init(true,
									  new ParametersWithIV(new KeyParameter(this.m_serverwritekey), this.m_serverwriteIV));
			this.m_decryptcipher = new CBCBlockCipher(new DESedeEngine());
			this.m_decryptcipher.init(false,
									  new ParametersWithIV(new KeyParameter(this.m_clientwritekey), this.m_clientwriteIV));
		}
	}

}
