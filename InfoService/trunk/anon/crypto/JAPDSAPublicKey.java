/*
Copyright (c) 2000 - 2003, The JAP-Team
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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.interfaces.DSAParams;
import java.security.interfaces.DSAPublicKey;

import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DEROutputStream;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.DSAParameter;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x9.X9ObjectIdentifiers;
import org.bouncycastle.crypto.params.DSAPublicKeyParameters;



import misc.*;

public class JAPDSAPublicKey implements DSAPublicKey
{
		private BigInteger      y;
		private DSAParams       dsaSpec;

		JAPDSAPublicKey(JAPDSAPublicKeySpec spec)
		{
				this.y = spec.getY();
				this.dsaSpec = new JAPDSAParameterSpec(spec.getP(), spec.getQ(), spec.getG());
		}


		JAPDSAPublicKey(JAPDSAPublicKey key)
		{
				this.y = key.getY();
				this.dsaSpec = key.getParams();
		}

		JAPDSAPublicKey(DSAPublicKeyParameters  params)
		{
				this.y = params.getY();
				this.dsaSpec = new JAPDSAParameterSpec(params.getParameters().getP(), params.getParameters().getQ(), params.getParameters().getG());
		}


		JAPDSAPublicKey(BigInteger y, JAPDSAParameterSpec dsaSpec)
		{
				this.y = y;
				this.dsaSpec = dsaSpec;
		}


		JAPDSAPublicKey(SubjectPublicKeyInfo info) throws IllegalArgumentException
		{
				DSAParameter params = new DSAParameter((ASN1Sequence)info.getAlgorithmId().getParameters());
				DERInteger derY = null;

				try
				{
						derY = (DERInteger)info.getPublicKey();
				}
				catch (IOException e)
				{
						throw new IllegalArgumentException("invalid info structure in DSA public key");
				}

				this.y = derY.getValue();
				this.dsaSpec = new JAPDSAParameterSpec(params.getP(), params.getQ(), params.getG());
		}

		public String getAlgorithm()
		{
				return "DSA";
		}

		public String getFormat()
		{
				return "X.509";
		}

		public byte[] getEncoded()
		{
				ByteArrayOutputStream   bOut = new ByteArrayOutputStream();
				DEROutputStream         dOut = new DEROutputStream(bOut);
				SubjectPublicKeyInfo    info = new SubjectPublicKeyInfo(new AlgorithmIdentifier(X9ObjectIdentifiers.id_dsa, new DSAParameter(dsaSpec.getP(), dsaSpec.getQ(), dsaSpec.getG()).getDERObject()), new DERInteger(y));

				try
				{
						dOut.writeObject(info);
						dOut.close();
				}
				catch (IOException e)
				{
						throw new RuntimeException("Error encoding DSA public key");
				}

				return bOut.toByteArray();

		}

		public DSAParams getParams()
		{
				return dsaSpec;
		}

		public BigInteger getY()
		{
				return y;
		}

		public String toString()
		{
				StringBuffer    buf = new StringBuffer();
				String          nl = System.getProperty("line.separator");

				buf.append("DSA Public Key" + nl);
				buf.append("            y: " + this.getY().toString(16) + nl);

				return buf.toString();
		}
}