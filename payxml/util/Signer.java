/*
Copyright (c) 2000, The JAP-Team
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
package payxml.util;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERInputStream;
import org.bouncycastle.asn1.DEROutputStream;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.DigestInfo;
import org.bouncycastle.asn1.x509.X509ObjectIdentifiers;
import org.bouncycastle.crypto.AsymmetricBlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.encodings.PKCS1Encoding;
import org.bouncycastle.crypto.engines.RSAEngine;


/**
 * Signaturklasse zum Erzeugen und Testen von Signaturen mit
 * RSAwithSHA1/PKCS1/DER-kodierten Hashwert.
 */
public class Signer
{
	//~ Instance fields ********************************************************

	private AlgorithmIdentifier algId;
	private AsymmetricBlockCipher cipher;
	private Digest digest;
	private byte[] digestValue;

	//~ Constructors ***********************************************************

	/**
	 * Signaturklasse zum Erzeugen und Testen von Signaturen mit
	 * RSAwithSHA1/PKCS1/DER-kodierten Hashwert.
	 */
	public Signer()
	{
		cipher = new PKCS1Encoding(new RSAEngine());
		digest = new SHA1Digest();
		digestValue = new byte[20];

		//algId = new AlgorithmIdentifier(new DERObjectIdentifier("1.3.14.3.2.26"));
		algId = new AlgorithmIdentifier(X509ObjectIdentifiers.id_SHA1);
	}

	//~ Methods ****************************************************************

	/**
	 * generates a signature. Call init(true, privateKey) and update() first
	 *
	 * @return the binary signature value
	 */
	public byte[] generateSignature() throws CryptoException, IOException
	{
		digest.doFinal(digestValue, 0);

		byte[] derEncodedDigest = derEncode(digestValue);
		byte[] sunsig = cipher.processBlock(derEncodedDigest, 0,
				derEncodedDigest.length
			);

		return sunsig;
	}


	public void init(boolean forSigning, CipherParameters param)
	{
		cipher.init(forSigning, param);
		reset();
	}


	public void reset()
	{
		digest.reset();
	}


	/**
			 */
	public void update(byte[] in)
	{
		digest.update(in, 0, in.length);
	}


	/**
	 * verifies a signature. Call init(false, publicKey) and update() first
	 *
	 * @param signature the binary signature value
	 *
	 * @return true if signature is valid, false otherwise
	 */
	public boolean verify(byte[] signature)
	{
		try {
			byte[] derEncodedDigest = cipher.processBlock(signature, 0,
					signature.length
				);
			DigestInfo digestInfo = derDecode(derEncodedDigest);
			byte[] hash = digestInfo.getDigest();

			digest.doFinal(digestValue, 0);
			if(hash==null||digestValue==null||hash.length!=digestValue.length)
				return false;
			for(int i=0;i<hash.length;i++)
				if(hash[i]!=digestValue[i])
					return false;
			return true;
		} catch (Exception e) {
			e.printStackTrace();

			return false;
		}
	}


	private DigestInfo derDecode(byte[] encoding) throws IOException
	{
		ByteArrayInputStream bIn = new ByteArrayInputStream(encoding);
		DERInputStream dIn = new DERInputStream(bIn);

		return new DigestInfo((ASN1Sequence) dIn.readObject());
	}


	private byte[] derEncode(byte[] hash) throws IOException
	{
		ByteArrayOutputStream bOut = new ByteArrayOutputStream();
		DEROutputStream dOut = new DEROutputStream(bOut);
		DigestInfo dInfo = new DigestInfo(algId, hash);
		dOut.writeObject(dInfo);

		return bOut.toByteArray();
	}
}
