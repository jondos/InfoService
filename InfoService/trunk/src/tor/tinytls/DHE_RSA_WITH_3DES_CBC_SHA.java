/*
 * Created on Mar 16, 2004
 *
 */
package tor.tinytls;

import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.engines.DESedeEngine;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;

import tor.util.helper;

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
	public byte[] encode(byte[] header, byte[] message)
	{
		HMac hmac = new HMac(new SHA1Digest());
		hmac.reset();
		hmac.init(new KeyParameter(this.m_clientmacsecret));
		hmac.update(helper.conc(helper.inttobyte(this.m_writesequenznumber,8),helper.conc(header,message)),0,message.length+header.length+8);
		byte[] mac = new byte[hmac.getMacSize()];
		hmac.doFinal(mac,0);
		this.m_writesequenznumber++;
		byte[] uncompressed = helper.conc(message,mac);
		//TODO : zuf?lliges padding hinzuf?gen
		//add padding as described in RFC2246 (6.2.3.2)
		int paddingsize=this.m_encryptcipher.getBlockSize()-((uncompressed.length+1)%this.m_encryptcipher.getBlockSize());
		byte[] padding = new byte[paddingsize];
		for(int i=0;i<padding.length;i++)
		{
			padding[i] = (byte)padding.length;
		}
		uncompressed = helper.conc(uncompressed,padding);
		uncompressed = helper.conc(uncompressed,helper.inttobyte(paddingsize,1));
		byte[] compressed = new byte[uncompressed.length];
		for(int i=0;i<uncompressed.length;i+=this.m_encryptcipher.getBlockSize())
		{
			this.m_encryptcipher.processBlock(uncompressed,i,compressed,i);
		}
		return compressed;
	}

	/**
	 * decodes a message with a symmetric key
	 * @param header header
	 * @param message message
	 * @return decoded message
	 */
	public byte[] decode(byte[] header, byte[] message) throws TLSException {
		byte[] decrypted = new byte[message.length];
		for(int i=0;i<message.length;i+=this.m_decryptcipher.getBlockSize())
		{
			this.m_decryptcipher.processBlock(message,i,decrypted,i);
		}
		//remove padding and mac
		int length = ((header[3] & 0xFF) <<8) |(header[4] & 0xFF);
		length-=21;//mac
		length-=decrypted[decrypted.length-1];//padding
		byte[] b= helper.inttobyte(length,2);
		header[3]=b[0];
		header[4]=b[1];
		byte[] uncompressed = helper.copybytes(decrypted,0,decrypted.length-21-decrypted[decrypted.length-1]);
		
		//TODO :auf richtiges padding vergleichen 
		
		HMac hmac = new HMac(new SHA1Digest());
		hmac.reset();
		hmac.init(new KeyParameter(this.m_servermacsecret));
		hmac.update(helper.conc(helper.inttobyte(this.m_readsequenznumber,8),helper.conc(header,uncompressed)),0,uncompressed.length+header.length+8);
		byte[] mac = new byte[hmac.getMacSize()];
		hmac.doFinal(mac,0);
		this.m_readsequenznumber++;
		
		for(int i=0;i<mac.length;i++)
		{
			if(decrypted[uncompressed.length+i]!=mac[i])
			{
				throw new TLSException("Wrong MAC detected!!!");
			}
		}
		
		return uncompressed;
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
