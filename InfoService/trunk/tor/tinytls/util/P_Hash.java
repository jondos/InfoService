/*
 * Created on Mar 25, 2004
 *
 */
package tor.tinytls.util;

import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.params.KeyParameter;

import tor.util.helper;

/**
 * @author stefan
 *
 * a P_Hash function as described in RFC2246
 */
public class P_Hash {

	private byte[] secret;
	private byte[] seed;
	private Digest digest;

	/**
	 * Constructor
	 * @param secret a secret
	 * @param seed a seed
	 * @param digest a digest
	 */
	public P_Hash(byte[] secret, byte[] seed, Digest digest)
	{
		this.secret = secret;
		this.seed = seed;
		this.digest = digest;
	}

	/**
	 * returns a hash with a variabel length
	 * @param length length of the hash
	 * @return hash
	 */
	public byte[] getHash(int length)
	{
		byte[] a;
		byte[] b = null;
		byte[] c;
		int counter = 0;
		HMac hm = new HMac(this.digest);
		hm.reset();
		hm.init(new KeyParameter(secret));
		hm.update(seed,0,seed.length);
		a = new byte[hm.getMacSize()];
		hm.doFinal(a,0);

		do
		{
			//HMAC_HASH(secret,a+seed)
			hm.reset();
			hm.init(new KeyParameter(secret));
			hm.update(helper.conc(a,seed),0,a.length+seed.length);
			c = new byte[hm.getMacSize()];
			hm.doFinal(c,0);
			if(b==null)
			{
				b = c;
			} else
			{
				b = helper.conc(b,c);
			}

			//compute next a
			hm.reset();
			hm.init(new KeyParameter(secret));
			hm.update(a,0,a.length);
			a = new byte[hm.getMacSize()];
			hm.doFinal(a,0);
		}	while(b.length<length);

		return helper.copybytes(b,0,length);
	}

}
