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
import java.security.InvalidKeyException;
import java.security.interfaces.DSAParams;
import java.security.interfaces.DSAPrivateKey;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DEROutputStream;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.DSAParameter;
import org.bouncycastle.asn1.x9.X9ObjectIdentifiers;
import org.bouncycastle.crypto.params.DSAPrivateKeyParameters;

public class MyDSAPrivateKey implements DSAPrivateKey
{
	private BigInteger m_X;
	private DSAParams m_params;

	public MyDSAPrivateKey(PrivateKeyInfo privKeyInfo) throws InvalidKeyException
	{
		try
		{
			//					ByteArrayInputStream bIn =new ByteArrayInputStream(encoded);
			//					DERInputStream dIn = new DERInputStream(bIn);
			//				PrivateKeyInfo privKeyInfo=new PrivateKeyInfo((ASN1Sequence)dIn.readObject());
			AlgorithmIdentifier algId = privKeyInfo.getAlgorithmId();
			DERInteger X = (DERInteger) privKeyInfo.getPrivateKey();
			m_X = X.getValue();
			m_params = new MyDSAParams(new DSAParameter( (ASN1Sequence) algId.getParameters()));
		}

		catch (Exception e)
		{
			throw new InvalidKeyException("IOException while decoding private key");
		}

	}

	public MyDSAPrivateKey(DSAPrivateKeyParameters keyParams)
	{
		m_X = keyParams.getX();
		m_params = new MyDSAParams(keyParams.getParameters());
	}

	public String getAlgorithm()
	{
		return "DSA";
	}

	public String getFormat()
	{
		return "PKCS#8";
	}

	public BigInteger getX()
	{
		return m_X;
	}

	public byte[] getEncoded()
	{
		ByteArrayOutputStream bOut = new ByteArrayOutputStream();
		DEROutputStream dOut = new DEROutputStream(bOut);

		try
		{
			DERObject derParam =
				new DSAParameter(
				m_params.getP(),
				m_params.getQ(),
				m_params.getG())
				.getDERObject();

			dOut.writeObject(
				new PrivateKeyInfo(
				new AlgorithmIdentifier(
				X9ObjectIdentifiers.id_dsa,
				derParam),
				new DERInteger(getX())));
			dOut.close();
		}
		catch (IOException e)
		{
			throw new RuntimeException("IOException while encoding private key");
		}
		return bOut.toByteArray();
	}

	public DSAParams getParams()
	{
		return m_params;
	}
}
