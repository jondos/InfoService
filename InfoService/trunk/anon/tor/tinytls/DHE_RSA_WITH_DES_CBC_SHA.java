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

import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.engines.DESEngine;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;

import anon.tor.util.helper;

/**
 * @author stefan
 *
 */
public class DHE_RSA_WITH_DES_CBC_SHA extends CipherSuite {

	private CBCBlockCipher m_encryptcipher;
	private CBCBlockCipher m_decryptcipher;

	/**
	 * Constuctor
	 * @throws Exception
	 */
	public DHE_RSA_WITH_DES_CBC_SHA() throws Exception
	{
		super(new byte[]{0x00,0x15});
		this.setKeyExchangeAlgorithm(new DHE_RSA_Key_Exchange());
	}

	public void encode(TLSRecord msg)
	{
		HMac hmac = new HMac(new SHA1Digest());
		hmac.reset();
		hmac.init(new KeyParameter(m_clientmacsecret));
		byte[]  b = helper.conc(new byte[7],this.m_writesequenznumber.toByteArray());
		hmac.update(b,b.length-8,8);
		m_writesequenznumber = m_writesequenznumber.add(new BigInteger("1"));
		hmac.update(msg.m_Header,0,msg.m_Header.length);
		hmac.update(msg.m_Data,0,msg.m_dataLen);
		hmac.doFinal(msg.m_Data,msg.m_dataLen);
		msg.m_dataLen+=hmac.getMacSize();
		//TODO : zuf?lliges padding hinzuf?gen
		//add padding as described in RFC2246 (6.2.3.2)
		int paddingsize=m_encryptcipher.getBlockSize()-((msg.m_dataLen+1)%m_encryptcipher.getBlockSize());
		for(int i=0;i<paddingsize+1;i++)
		{
			msg.m_Data[msg.m_dataLen++] = (byte)paddingsize;
		}
		for(int i=0;i<msg.m_dataLen;i+=m_encryptcipher.getBlockSize())
		{
			this.m_encryptcipher.processBlock(msg.m_Data,i,msg.m_Data,i);
		}
		msg.setLength(msg.m_dataLen);
	}

	public void decode(TLSRecord msg) throws TLSException {
		for(int i=0;i<msg.m_dataLen;i+=m_decryptcipher.getBlockSize())
		{
			m_decryptcipher.processBlock(msg.m_Data,i,msg.m_Data,i);
		}
		//remove padding and mac
		HMac hmac = new HMac(new SHA1Digest());
		int len=msg.m_dataLen-hmac.getMacSize()-1;
		len-=msg.m_Data[msg.m_dataLen-1];//padding
		msg.setLength(len);

		//TODO :auf richtiges padding vergleichen

		hmac.reset();
		hmac.init(new KeyParameter(m_servermacsecret));
		byte[]  b = helper.conc(new byte[7],this.m_readsequenznumber.toByteArray());
		hmac.update(b,b.length-8,8);
		hmac.update(msg.m_Header,0,msg.m_Header.length);
		hmac.update(msg.m_Data,0,len);
		byte[] mac = new byte[hmac.getMacSize()];
		hmac.doFinal(mac,0);
		m_readsequenznumber = m_readsequenznumber.add(new BigInteger("1"));

		for(int i=0;i<mac.length;i++)
		{
			if(msg.m_Data[len+i]!=mac[i])
			{
				throw new TLSException("Wrong MAC detected!!!");
			}
		}
	}

	protected void calculateKeys(byte[] keys)
	{
		this.m_clientmacsecret = helper.copybytes(keys,0,20);
		this.m_servermacsecret = helper.copybytes(keys,20,20);
		this.m_clientwritekey	 = helper.copybytes(keys,40,8);
		this.m_serverwritekey = helper.copybytes(keys,48,8);
		this.m_clientwriteIV = helper.copybytes(keys,56,8);
		this.m_serverwriteIV = helper.copybytes(keys,64,8);
		this.m_encryptcipher = new CBCBlockCipher(new DESEngine());
		this.m_encryptcipher.init(true,new ParametersWithIV(new KeyParameter(this.m_clientwritekey),this.m_clientwriteIV));
		this.m_decryptcipher = new CBCBlockCipher(new DESEngine());
		this.m_decryptcipher.init(false,new ParametersWithIV(new KeyParameter(this.m_serverwritekey),this.m_serverwriteIV));
	}


}
