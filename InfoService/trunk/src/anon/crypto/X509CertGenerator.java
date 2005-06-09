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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Date;

import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.DEREncodableVector;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DEROutputStream;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERUTCTime;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x509.TBSCertificateStructure;
import org.bouncycastle.asn1.x509.V3TBSCertificateGenerator;
import org.bouncycastle.asn1.x509.X509CertificateStructure;
import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.asn1.ASN1InputStream;

/**
 * @deprecated use {@link anon.crypto.JAPCertificate} instead; scheduled for removal on 04/12/13
 */
public final class X509CertGenerator extends V3TBSCertificateGenerator
{
	public X509CertGenerator(String a_ownerAlias, Date a_validFrom, Date a_validTo,
							 IMyPublicKey a_publicKey)
		throws IOException
	{
		setStartDate(new DERUTCTime(a_validFrom));
		setEndDate(new DERUTCTime(a_validTo));
		setSerialNumber(new DERInteger(1));
		setSubject(new X509Name("CN=" + a_ownerAlias));
		setSubjectPublicKeyInfo(new SubjectPublicKeyInfo( (ASN1Sequence) (new ASN1InputStream(new
			ByteArrayInputStream(a_publicKey.getEncoded()))).readObject()));
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

	public X509CertificateStructure sign(PKCS12 a_pkcs12Certificate)
	{
		return sign(a_pkcs12Certificate.getX509Certificate().getSubject(),
					a_pkcs12Certificate.getPrivateKey());
	}


	public X509CertificateStructure sign(X509Name a_issuer, IMyPrivateKey a_privateKey)
	{
		try
		{
			TBSCertificateStructure tbsCert;
			DEREncodableVector seqv;
			ByteArrayOutputStream bOut;
			byte[] signa;

			setIssuer(a_issuer);
			setSignature(a_privateKey.getSignatureAlgorithm().getIdentifier());

			/* generate signature */
			bOut = new ByteArrayOutputStream();
			tbsCert = generateTBSCertificate();
			(new DEROutputStream(bOut)).writeObject(tbsCert);
			signa = ByteSignature.sign(bOut.toByteArray(), a_privateKey);

			/* construct certificate */
			seqv = new DEREncodableVector();
			seqv.add(tbsCert);
			seqv.add(a_privateKey.getSignatureAlgorithm().getIdentifier());
			seqv.add(new DERBitString(signa));

			return new X509CertificateStructure(new DERSequence(seqv));
		}

		catch (Throwable t)
		{t.printStackTrace();
			return null;
		}
	}
}
