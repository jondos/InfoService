/*
 * Created on Mar 25, 2004
 *
 */
package anon.tor.tinytls.util;

import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.params.KeyParameter;

import anon.tor.util.helper;

/**
 * @author stefan
 *
 * a P_Hash function as described in RFC2246
 */
public class P_Hash {

	private byte[] m_secret;
	private byte[] m_seed;
	private Digest m_digest;

	/**
	 * Constructor
	 * @param secret a secret
	 * @param seed a seed
	 * @param digest a digest
	 */
	public P_Hash(byte[] secret, byte[] seed, Digest digest)
	{
		this.m_secret = secret;
		this.m_seed = seed;
		this.m_digest = digest;
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
		HMac hm = new HMac(this.m_digest);
		hm.reset();
		hm.init(new KeyParameter(this.m_secret));
		hm.update(this.m_seed,0,this.m_seed.length);
		a = new byte[hm.getMacSize()];
		hm.doFinal(a,0);

		do
		{
			//HMAC_HASH(secret,a+seed)
			hm.reset();
			hm.init(new KeyParameter(this.m_secret));
			hm.update(helper.conc(a,this.m_seed),0,a.length+this.m_seed.length);
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
			hm.init(new KeyParameter(this.m_secret));
			hm.update(a,0,a.length);
			a = new byte[hm.getMacSize()];
			hm.doFinal(a,0);
		}	while(b.length<length);

		return helper.copybytes(b,0,length);
	}

}
