/*
 Copyright (c) 2000 - 2004, The JAP-Team
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

/* Hint: This file may be only a copy of the original file which is always in the JAP source tree!
 * If you change something - do not forget to add the changes also to the JAP source tree!
 */

package anon.crypto;

import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.PublicKey;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.engines.RSAEngine;
import org.bouncycastle.asn1.x509.DigestInfo;
import org.bouncycastle.crypto.encodings.PKCS1Encoding;
import java.io.*;
import org.bouncycastle.asn1.*;
import org.bouncycastle.asn1.x509.*;
/*** SHA1withRSA Signature as descripted in RFC 2313 */
final class MyRSASignature implements IMySignature
{
	private PKCS1Encoding m_SignatureAlgorithm;
	private SHA1Digest m_Digest;
	private final static AlgorithmIdentifier ms_AlgID=new AlgorithmIdentifier(X509ObjectIdentifiers.id_SHA1,null);

	MyRSASignature()
	{
		m_SignatureAlgorithm = new PKCS1Encoding(new RSAEngine());
		m_Digest=new SHA1Digest();
	}

	synchronized public void initVerify(IMyPublicKey k) throws InvalidKeyException
	{
		m_SignatureAlgorithm.init(false, ( (MyRSAPublicKey) k).getParams());
	}

	synchronized public void initSign(IMyPrivateKey k) throws InvalidKeyException
	{
		m_SignatureAlgorithm.init(true, ( (MyRSAPrivateKey) k).getParams());
	}

	synchronized public boolean verify(byte[] message, byte[] sig)
	{
		try
		{
			m_Digest.reset();
			m_Digest.update(message,0,message.length);
			byte[]  hash = new byte[m_Digest.getDigestSize()];
			m_Digest.doFinal(hash, 0);

			byte[]	 decryptedSig = m_SignatureAlgorithm.processBlock(sig, 0, sig.length);
			ByteArrayInputStream    bIn = new ByteArrayInputStream(decryptedSig);
 			DERInputStream          dIn = new DERInputStream(bIn);


			DigestInfo	 digInfo = new DigestInfo((ASN1Sequence)new DERInputStream(bIn).readObject());

			 if (!digInfo.getAlgorithmId().getObjectId().equals(ms_AlgID.getObjectId()))
			 {
				 return false;
			 }

			Object o=digInfo.getAlgorithmId().getParameters();
			if(o!=null&&!(o instanceof ASN1Null))
				return false;

			 byte[]  sigHash = digInfo.getDigest();

			 if (hash.length != sigHash.length)
			 {
				 return false;
			 }

			 for (int i = 0; i < hash.length; i++)
			 {
				 if (sigHash[i] != hash[i])
				 {
					 return false;
				 }
			 }

			 return true;
	 		}
		catch (Exception e)
		{
			return false;
		}
	}

	synchronized public byte[] sign(byte[] bytesToSign)
	{
		try
		{
			byte[]  hash = new byte[m_Digest.getDigestSize()];
			m_Digest.reset();
			m_Digest.update(bytesToSign,0,bytesToSign.length);
			m_Digest.doFinal(hash, 0);

			ByteArrayOutputStream   bOut = new ByteArrayOutputStream();
			DEROutputStream         dOut = new DEROutputStream(bOut);
			DigestInfo              dInfo = new DigestInfo(ms_AlgID, hash);
	  	  	dOut.writeObject(dInfo);
	  		byte[]  bytes= bOut.toByteArray();

			return m_SignatureAlgorithm.processBlock(bytes, 0, bytes.length);
		}
		catch (Throwable t)
		{
			return null;
		}
	}

}
