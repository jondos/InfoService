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
package anon.crypto;

import java.io.IOException;
import java.io.OutputStream;
import java.security.SecureRandom;
import java.util.Date;
import java.util.GregorianCalendar;

/**
 * This class implements the generation of a DSA key pair.
 * @deprecated this class is replaced by {@link anon.crypto.DSAKeyPair}
 */
final public class JAPDsaKeyGenerator
{
	private DSAKeyPair m_keyPair;

	/**
	 * This creates a new instance of JAPDsaKeyGenerator. It is exactly one DSA key pair generated.
	 * The keys can be obtained by a call of getDsaPublicKey() or getDsaPrivateKey().
	 *
	 * @param a_secureRandom A random number as initialization for the key generator.
	 * @param a_keyLength The length of the key in bits. For the current implementation of
	 *                    bouncycastle it must be a number between 512 and 1024 which is a multiple
	 *                    of 64.
	 * @param a_keyParamCertainty Measure of robustness of prime. For FIPS 186-2 compliance this
	 *                             value should be at least 80.
	 * @deprecated use {@link anon.crypto.DSAKeyPair#getInstance(SecureRandom,int,int)} instead;
	 *             scheduled for removal on 04/12/12
	 */
	public JAPDsaKeyGenerator(SecureRandom a_secureRandom, int a_keyLength, int a_keyParamCertainty)
	{
		m_keyPair = DSAKeyPair.getInstance(a_secureRandom, a_keyLength, a_keyParamCertainty);
	}

	/**
	 * Returns the generated public DSA key.
	 *
	 * @return The public key.
	 */
	public MyDSAPublicKey getDsaPublicKey()
	{
		return (MyDSAPublicKey)m_keyPair.getPublic();
	}

	/**
	 * Returns the generated private DSA key.
	 *
	 * @return The private key.
	 */
	public MyDSAPrivateKey getDsaPrivateKey()
	{
		return (MyDSAPrivateKey)m_keyPair.getPrivate();
	}

	/**
	 * Writes the DSA private key as an PKCS12 certificate to an OutputStream.
	 *
	 * @param a_outputStream The OutputStream for writing the certificate.
	 * @param a_password A password for encrypting the certificate. It must be entered when the
	 *                   certificate is loaded.
	 * @param a_ownerAlias The owner of the certificate. The name is set as the common name (CN).
	 * @param a_validFrom The date from which the certificate is valid.
	 * @param a_validTo The date until which the certificate is valid.
	 * @throws IOException if the key could not be written to the output stream
	 * @deprecated use {@link anon.crypto.DSAKeyPair#getInstance(SecureRandom,int,int)},
	 * {@link anon.crypto.PKCS12#PKCS12(String,AsymmetricCryptoKeyPair,Calendar,Calendar)},
	 * {@link anon.crypto.PKCS12#store(OutputStream,char[])}; scheduled for removal on 04/12/12
     *
	 */
	public void storeDsaPrivateKeyAsPkcs12(OutputStream a_outputStream, String a_password,
										   String a_ownerAlias, Date a_validFrom, Date a_validTo)
		throws IOException
	{
		GregorianCalendar validFrom, validTo;
		PKCS12 privateCertificate;

		validFrom = new GregorianCalendar();
		validFrom.setTime(a_validFrom);

		validTo = new GregorianCalendar();
		validTo.setTime(a_validTo);

		privateCertificate = new PKCS12(a_ownerAlias, m_keyPair, validFrom, validTo);
		privateCertificate.store(a_outputStream, a_password.toCharArray());
	}

	/**
	 * Writes the DSA public key as an X509 certificate to an OutputStream.
	 *
	 * @param a_outputStream The OutputStream for writing the certificate.
	 * @param a_ownerAlias The owner of the certificate. The name is set as the common name (CN).
	 * @param a_validFrom The date from which the certificate is valid.
	 * @param a_validTo The date until which the certificate is valid.
	 * @throws IOException if the key could not be written to the output stream
	 * @deprecated use {@link anon.crypto.DSAKeyPair#getInstance(SecureRandom,int,int)},
	 * {@link anon.crypto.JAPCertificate#getInstance(String,AsymmetricCryptoKeyPair,Calendar,Calendar)},
	 * {@link anon.crypto.JAPCertificate#store(OutputStream)}; scheduled for removal on 04/12/12
	 */
	public void storeDsaPublicKeyAsX509(OutputStream a_outputStream, String a_ownerAlias,
										Date a_validFrom, Date a_validTo) throws IOException
	{
		GregorianCalendar validFrom, validTo;
		JAPCertificate certificate;

		validFrom = new GregorianCalendar();
		validFrom.setTime(a_validFrom);

		validTo = new GregorianCalendar();
		validTo.setTime(a_validTo);

		certificate = JAPCertificate.getInstance(a_ownerAlias, m_keyPair, validFrom, validTo);
		certificate.store(a_outputStream);
	}

}
