/*
 * Created on Mar 16, 2004
 *
 */
package anon.tor.tinytls;

import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.engines.DESedeEngine;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;

import anon.tor.util.helper;

/**
 * @author stefan
 *
 */
public class DHE_RSA_WITH_3DES_CBC_SHA extends CipherSuite {

	private CBCBlockCipher m_encryptcipher;
	private CBCBlockCipher m_decryptcipher;

	/**
	 * Constuctor
	 * @throws Exception
	 */
	public DHE_RSA_WITH_3DES_CBC_SHA() throws Exception
	{
		super(new byte[]{0x00,0x16});
		this.setKeyExchangeAlgorithm(new DHE_RSA_Key_Exchange());
	}

	/**
	 * encode a message with a symmetric key
	 * @param header header
	 * @param message message
	 * @return encoded message
	 */
	public void encode(TLSRecord msg)
	{
		HMac hmac = new HMac(new SHA1Digest());
		hmac.reset();
		hmac.init(new KeyParameter(m_clientmacsecret));
		hmac.update(helper.inttobyte(this.m_writesequenznumber,8),0,8);
		hmac.update(msg.m_Header,0,msg.m_Header.length);
		hmac.update(msg.m_Data,0,msg.m_dataLen);
		hmac.doFinal(msg.m_Data,msg.m_dataLen);
		msg.m_dataLen+=hmac.getMacSize();
		m_writesequenznumber++;
		//TODO : zuf?lliges padding hinzuf?gen
		//add padding as described in RFC2246 (6.2.3.2)
		int paddingsize=m_encryptcipher.getBlockSize()-((msg.m_dataLen)%m_encryptcipher.getBlockSize());
		for(int i=0;i<paddingsize;i++)
		{
			msg.m_Data[msg.m_dataLen++] = (byte)paddingsize;
		}
		for(int i=0;i<msg.m_dataLen;i+=m_encryptcipher.getBlockSize())
		{
			this.m_encryptcipher.processBlock(msg.m_Data,i,msg.m_Data,i);
		}
		msg.setLength(msg.m_dataLen);
	}

	/**
	 * decodes a message with a symmetric key
	 * @param header header
	 * @param message message
	 * @return decoded message
	 */
	public void decode(TLSRecord msg) throws TLSException {
		for(int i=0;i<msg.m_dataLen;i+=m_decryptcipher.getBlockSize())
		{
			m_decryptcipher.processBlock(msg.m_Data,i,msg.m_Data,i);
		}
		//remove padding and mac
		int len=msg.m_dataLen-21;
		len-=msg.m_Data[len-1];//padding

		//TODO :auf richtiges padding vergleichen

		HMac hmac = new HMac(new SHA1Digest());
		hmac.reset();
		hmac.init(new KeyParameter(m_servermacsecret));
		hmac.update(helper.inttobyte(this.m_readsequenznumber,8),0,8);
		hmac.update(msg.m_Header,0,msg.m_Header.length);
		hmac.update(msg.m_Data,0,len);
		byte[] mac = new byte[hmac.getMacSize()];
		hmac.doFinal(mac,0);
		m_readsequenznumber++;

		for(int i=0;i<mac.length;i++)
		{
			if(msg.m_Data[len+i]!=mac[i])
			{
				throw new TLSException("Wrong MAC detected!!!");
			}
		}
		msg.setLength(len);
	}

	/**
	 * extract the keys (see RFC 2246 TLS Record Protocoll)
	 * @param keys array of bytes that contains the keys
	 */
	protected void calculateKeys(byte[] keys)
	{
		this.m_clientmacsecret = helper.copybytes(keys,0,20);
		this.m_servermacsecret = helper.copybytes(keys,20,20);
		this.m_clientwritekey	 = helper.copybytes(keys,40,24);
		this.m_serverwritekey = helper.copybytes(keys,64,24);
		this.m_clientwriteIV = helper.copybytes(keys,88,8);
		this.m_serverwriteIV = helper.copybytes(keys,96,8);
		this.m_encryptcipher = new CBCBlockCipher(new DESedeEngine());
		this.m_encryptcipher.init(true,new ParametersWithIV(new KeyParameter(this.m_clientwritekey),this.m_clientwriteIV));
		this.m_decryptcipher = new CBCBlockCipher(new DESedeEngine());
		this.m_decryptcipher.init(false,new ParametersWithIV(new KeyParameter(this.m_serverwritekey),this.m_serverwriteIV));
	}


}
