/*
 * Created on Mar 25, 2004
 *
 */
package tor.tinytls.util;

import org.bouncycastle.crypto.digests.MD5Digest;
import org.bouncycastle.crypto.digests.SHA1Digest;

import tor.util.helper;

/**
 * @author stefan
 *a Pseudo Radnom Function as described in RFC2246
 */
public class PRF {
	
	private byte[] secret;
	private byte[] seed;
	private byte[] label;
	/**
	 * Constructor for a Pseudo Random Function
	 * @param secret a secret
	 * @param label a label
	 * @param seed a seed
	 */
	public PRF(byte[] secret,byte[] label, byte[] seed)
	{
		this.secret = secret;
		this.seed = seed;
		this.label = label;
	}
	
	/**
	 * calculates the result of a pseudo random function
	 * @param length length of the result
	 * @return result of a PRF with variable length
	 */
	public byte[] calculate(int length)
	{ 
		byte[] a;
		byte[] b;
		byte[] c = new byte[length];
		int splitsize = this.secret.length / 2;
		if((splitsize*2)<this.secret.length)
		{
			splitsize++;
		}
		byte[] s1 = helper.copybytes(this.secret,0,splitsize);
		byte[] s2 = helper.copybytes(this.secret,this.secret.length - splitsize,splitsize);
		P_Hash phash = new P_Hash(s1,helper.conc(this.label,this.seed),new MD5Digest());
		a = phash.getHash(length);
		phash = new P_Hash(s2,helper.conc(this.label,this.seed),new SHA1Digest());
		b = phash.getHash(length);
		for(int i=0;i<length;i++)
		{
			c[i] = (byte)((a[i] ^ b[i]) & 0xFF) ;
		}
		
		return c;
	}

}
