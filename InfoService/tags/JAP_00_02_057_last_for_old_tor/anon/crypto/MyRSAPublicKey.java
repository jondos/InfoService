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
import java.security.PublicKey;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DEROutputStream;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.RSAPublicKeyStructure;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.CipherParameters;

final public class MyRSAPublicKey implements PublicKey
{
	private BigInteger m_n;
	private BigInteger m_e;

	public MyRSAPublicKey(BigInteger modulus, BigInteger exponent)
	{
		m_n = modulus;
		m_e = exponent;
	}

	public MyRSAPublicKey(CipherParameters cipherparams) throws Exception
	{
		RSAKeyParameters p=(RSAKeyParameters)cipherparams;
		m_n=p.getModulus();
		m_e=p.getExponent();
	}

	MyRSAPublicKey(SubjectPublicKeyInfo info) throws IllegalArgumentException
	{
		try
		{
			RSAPublicKeyStructure en = RSAPublicKeyStructure.getInstance(info.getPublicKey());
			m_n = en.getModulus();
			m_e = en.getPublicExponent();
		}
		catch (IOException e)
		{
			throw new IllegalArgumentException("invalid info structure in DSA public key");
		}

	}

	public BigInteger getModulus()
	{
		return m_n;
	}

	public BigInteger getPublicExponent()
	{
		return m_e;
	}

	public String getAlgorithm()
	{
		return "RSA";
	}

	public String getFormat()
	{
		return "X.509";
	}

	public SubjectPublicKeyInfo getAsSubjectPublicKeyInfo()
	{
		AlgorithmIdentifier algID = new AlgorithmIdentifier(new DERObjectIdentifier("1.2.840.113549.1.1.1")); //RSA
		return new SubjectPublicKeyInfo(algID, new RSAPublicKeyStructure(m_n, m_e).getDERObject());
	}

	public byte[] getEncoded()
	{
		ByteArrayOutputStream bOut = new ByteArrayOutputStream();
		DEROutputStream dOut = new DEROutputStream(bOut);
		try
		{
			dOut.writeObject(getAsSubjectPublicKeyInfo());
			dOut.close();
		}
		catch (IOException e)
		{
			throw new RuntimeException("IOException while encoding public key");
		}
		return bOut.toByteArray();
	}

	public boolean equals(Object o)
	{
		if (o == null)
		{
			return false;
		}
		if (! (o instanceof PublicKey))
		{
			return false;
		}
		if (! (o instanceof MyRSAPublicKey))
		{
			return false;
		}
		MyRSAPublicKey r = (MyRSAPublicKey) o;
		return r.getModulus().equals(m_n) && r.getPublicExponent().equals(m_e);
	}

}
