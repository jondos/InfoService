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
 * Created on Mar 25, 2004
 *
 */
package anon.tor.tinytls.util;

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

	public static byte[] sha(byte[] input)
	{
		SHA1Digest master_sha1 = new SHA1Digest();
		master_sha1.update(input, 0, input.length);
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
