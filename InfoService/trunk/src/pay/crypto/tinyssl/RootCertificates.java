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
package pay.crypto.tinyssl;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.math.BigInteger;
import org.bouncycastle.asn1.DERConstructedSequence;
import org.bouncycastle.asn1.DERInputStream;
import org.bouncycastle.asn1.x509.RSAPublicKeyStructure;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x509.X509CertificateStructure;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.util.encoders.Base64;
import pay.util.Log;


/**
 * In dieser Klasse sind die Root-Zertifikate zur Authentisierung der
 * Bezahlinstanz und der Abrechnungsinstanzen enthalten.
 *
 * @author Andreas Mueller
 */
public class RootCertificates
{
		SubjectPublicKeyInfo[] trusted_CA_public_keys;
		String[] trusted_CA_public_key_identifiers;
		private int numberOfCerts;

		/**
		 * Initialisiert die Klasse. Liest die Zertifikate für einen bequemen
		 * Zugriff in Arrays ein.
		 */
		public void init()
		{
				try {
						if (Log.on) Log.log(TinySSL.class, "reading in trusted root public keys...");
						numberOfCerts=RootCertificates.base64_CA_certs.length;
						trusted_CA_public_keys = new SubjectPublicKeyInfo[numberOfCerts];
						trusted_CA_public_key_identifiers = new String[numberOfCerts];

						// read my cert
						for(int i=0; i<numberOfCerts;i++)
						{
								byte [] b = Base64.decode(RootCertificates.base64_CA_certs[i]);
								DERInputStream dIn = new DERInputStream(new ByteArrayInputStream(b));
								X509CertificateStructure mycertx509 = new
								X509CertificateStructure((DERConstructedSequence)dIn.readObject());
								trusted_CA_public_key_identifiers[i] =
								mycertx509.getSubject().toString();
								trusted_CA_public_keys[i] = mycertx509.getSubjectPublicKeyInfo();

								Log.log(TinySSL.class,"subject: "+mycertx509.getSubject().toString());
						}
				} catch (Exception e) {
						if (Log.on) Log.log(TinySSL.class, e);
				}
		}

		/**
		 * Liefert den öffentlichen Schlüssel des Rootzertifikats mit angegebenen
		 * Subjectnamen.
		 *
		 * @param identifier Subjectname
		 * @return öffentlicher Schlüssel
		 */
		public RSAKeyParameters getPublicKey(String identifier)
		{
				for(int i=0;i<numberOfCerts;i++)
						if(trusted_CA_public_key_identifiers[i].lastIndexOf(identifier)!=-1)
						{
								try
								{
										RSAPublicKeyStructure rsa_pks = new RSAPublicKeyStructure((DERConstructedSequence)trusted_CA_public_keys[i].getPublicKey());
										BigInteger modulus = rsa_pks.getModulus();
										BigInteger exponent = rsa_pks.getPublicExponent();
										return new RSAKeyParameters(false, modulus, exponent);
								} catch (IOException e){ return null;}
						}
				return null;
		}

		static String[] base64_CA_certs = new String[]
		{
				"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"
		};
}
