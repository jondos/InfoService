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
package anon.crypto;

/* Hint: This file may be only a copy of the original file which is always in the JAP source tree!
 * If you change something - do not forget to add the changes also to the JAP source tree!
 */

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.PrivateKey;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DEROutputStream;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.pkcs.RSAPrivateKeyStructure;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters;

final public class MyRSAPrivateKey implements PrivateKey
{
	private RSAPrivateCrtKeyParameters m_Params;

	public MyRSAPrivateKey(CipherParameters cipherparams) throws Exception
	{
		m_Params=(RSAPrivateCrtKeyParameters)cipherparams;
	}

	public MyRSAPrivateKey(PrivateKeyInfo privKeyInfo) throws Exception
	{
		DERObject d = privKeyInfo.getPrivateKey();
		RSAPrivateKeyStructure gh = new RSAPrivateKeyStructure( (ASN1Sequence) d);
		m_Params = new RSAPrivateCrtKeyParameters(gh.getModulus(), gh.getPublicExponent(),
												  gh.getPrivateExponent(),
												  gh.getPrime1(), gh.getPrime2(), gh.getExponent1(),
												  gh.getExponent2(),
												  gh.getCoefficient());

	}

	public MyRSAPrivateKey(
		BigInteger modulus,
		BigInteger publicExponent,
		BigInteger privateExponent,
		BigInteger p,
		BigInteger q,
		BigInteger dP,
		BigInteger dQ,
		BigInteger qInv) throws Exception
	{
		m_Params = new RSAPrivateCrtKeyParameters(modulus, publicExponent, privateExponent,
												  p, q, dP, dQ, qInv);

	}

	public CipherParameters getParams()
	{
		return m_Params;
	}

	public BigInteger getModulus()
	{
		return m_Params.getModulus();
	}

	public BigInteger getPrivateExponent()
	{
		return m_Params.getExponent();
	}

	public String getAlgorithm()
	{
		return "RSA";
	}

	public String getFormat()
	{
		return "PKCS#8";
	}

	public byte[] getEncoded()
	{
		ByteArrayOutputStream bOut = new ByteArrayOutputStream();
		DEROutputStream dOut = new DEROutputStream(bOut);

		try
		{
			PrivateKeyInfo privKey =
				new PrivateKeyInfo(
				new AlgorithmIdentifier(
				new DERObjectIdentifier("1.2.840.113549.1.1.1")
				),
				new RSAPrivateKeyStructure(m_Params.getModulus(), m_Params.getPublicExponent(),
										   m_Params.getExponent(), m_Params.getP(), m_Params.getQ(),
										   m_Params.getDP(), m_Params.getDQ(), m_Params.getQInv()).
				getDERObject());
			dOut.writeObject(privKey);

			dOut.close();
		}
		catch (IOException e)
		{
			throw new RuntimeException("IOException while encoding private key");
		}
		return bOut.toByteArray();
	}

}
