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
import java.math.BigInteger;
import java.security.PrivateKey;
import java.security.interfaces.DSAPrivateKey;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.DEREncodableVector;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DEROutputStream;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.TBSCertificateStructure;
import org.bouncycastle.asn1.x509.V3TBSCertificateGenerator;
import org.bouncycastle.asn1.x509.X509CertificateStructure;
import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.asn1.x9.X9ObjectIdentifiers;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.params.DSAPrivateKeyParameters;
import org.bouncycastle.crypto.signers.DSASigner;

final public class X509CertGenerator extends V3TBSCertificateGenerator
{
	public X509CertGenerator()
	{
	}

	public X509CertGenerator(X509CertificateStructure cert)
	{
		this(cert.getTBSCertificate());
	}

	public X509CertGenerator(TBSCertificateStructure tbs)
	{
		setEndDate(tbs.getEndDate());
		setExtensions(tbs.getExtensions());
		setIssuer(tbs.getIssuer());
		setSerialNumber(tbs.getSerialNumber());
		setSignature(tbs.getSignature());
		setStartDate(tbs.getStartDate());
		setSubject(tbs.getSubject());
		setSubjectPublicKeyInfo(tbs.getSubjectPublicKeyInfo());
	}

	public X509CertificateStructure sign(X509Name issuer, PrivateKey k)
	{
		try
		{
			boolean bRSA = false;
			boolean bDSA = false;
			setIssuer(issuer);
			if (k instanceof DSAPrivateKey)
			{
				bDSA = true;
			}
			else if (k instanceof MyRSAPrivateKey)
			{
				bRSA = true;
			}
			else
			{
				return null;
			}
			AlgorithmIdentifier algID = null;
			if (bDSA)
			{
				algID = new AlgorithmIdentifier(X9ObjectIdentifiers.id_dsa_with_sha1);
			}
			else
			{
				algID = new AlgorithmIdentifier(new DERObjectIdentifier("1.2.840.113549.1.1.5")); //RSAwithsha1
			}
			setSignature(algID);

			//calculate the digest
			SHA1Digest digest = new SHA1Digest();
			TBSCertificateStructure tbsCert = generateTBSCertificate();
			ByteArrayOutputStream bOut = new ByteArrayOutputStream();
			(new DEROutputStream(bOut)).writeObject(tbsCert);
			digest.update(bOut.toByteArray(), 0, bOut.size());
			byte[] hash = new byte[digest.getDigestSize()];
			digest.doFinal(hash, 0);
//generate signature
			DERBitString sig = null;
			if (bDSA)
			{
				DSASigner signer = new DSASigner();
				DSAPrivateKey key = (DSAPrivateKey) k;
				DSAPrivateKeyParameters signkey = new DSAPrivateKeyParameters(key.getX(), new MyDSAParams(key));
				signer.init(true, signkey);
				BigInteger[] r_and_s = signer.generateSignature(hash);
				DEREncodableVector sigvalue = new DEREncodableVector();
				sigvalue.add(new DERInteger(r_and_s[0]));
				sigvalue.add(new DERInteger(r_and_s[1]));
				sig = new DERBitString(new DERSequence(sigvalue));

			}
			else
			{
				/** @todo Construct signature... */
				byte[] sigvalue = new byte[128];
				sig = new DERBitString(sigvalue);
			}

//construct cert
			DEREncodableVector seqv = new DEREncodableVector();
			seqv.add(tbsCert);
			seqv.add(algID);
			seqv.add(sig);
			return new X509CertificateStructure(new DERSequence(seqv));
		}

		catch (Throwable t)
		{
			return null;
		}
	}

}
