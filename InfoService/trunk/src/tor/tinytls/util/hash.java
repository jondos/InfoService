/*
 * Created on Mar 25, 2004
 *
 */
package tor.tinytls.util;

import org.bouncycastle.crypto.digests.MD5Digest;
import org.bouncycastle.crypto.digests.SHA1Digest;

/**
 * @author stefan
 *
 *	this class implements often used hash functions
 */
public class hash {
	
	/**
	 * generates a sha hash
	 * @param inputs input
	 * @return hash
	 */
	public static byte[] sha(byte[][] inputs)
	{
		SHA1Digest master_sha1 = new SHA1Digest();
		master_sha1.reset();
		for (int i = 0; i < inputs.length; i++)
		{
			master_sha1.update(inputs[i], 0, inputs[i].length);
		}
		byte[] ret = new byte[master_sha1.getDigestSize()];
		master_sha1.doFinal(ret, 0);
		return ret;
	}

	/**
	 * generates a md5 hash
	 * @param inputs input
	 * @return hash
	 */
	public static byte[] md5(byte[][] inputs)
	{
		MD5Digest master_md5 = new MD5Digest();
		master_md5.reset();
		for (int i = 0; i < inputs.length; i++)
		{
			master_md5.update(inputs[i], 0, inputs[i].length);
		}
		byte[] ret = new byte[master_md5.getDigestSize()];
		master_md5.doFinal(ret, 0);
		return ret;
	}


}
