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
package anon.crypto.test;

import org.bouncycastle.asn1.ASN1OctetString;
import org.bouncycastle.asn1.x509.SubjectKeyIdentifier;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.security.SecureRandom;
import java.util.GregorianCalendar;
import java.util.Vector;

import anon.crypto.AsymmetricCryptoKeyPair;
import anon.crypto.JAPCertificate;
import anon.crypto.PKCS12;
import anon.util.Util;
import junitx.framework.extension.XtendedPrivateTestCase;
import anon.util.ByteArrayUtil;


public class JAPCertificateTest extends XtendedPrivateTestCase
{
	private SecureRandom m_random;

	public JAPCertificateTest(String a_name)
	{
		super(a_name);
		m_random = new SecureRandom();
	}

	/**
	 * Test if certificates can be verified with the DSA algorithm.
	 * @exception Exception if an error occurs
	 */
	public void testVerifyCertificateDSA() throws Exception
	{
		m_random.setSeed(692859929);
		testVerifyCertificate(new DSATestKeyPairGenerator(m_random));
	}

	/**
	 * Test if certificates can be verified with the RSA algorithm.
	 * @exception Exception if an error occurs
	 */
	public void testVerifyCertificateRSA() throws Exception
	{
		m_random.setSeed(47989202);
		testVerifyCertificate(new RSATestKeyPairGenerator(m_random));
	}

	/**
	 * Test if certificates can be verified with the Dummy algorithm.
	 * @exception Exception if an error occurs
	 */
	public void testVerifyCertificateDummy() throws Exception
	{
		m_random.setSeed(38959105);
		testVerifyCertificate(new DummyTestKeyPairGenerator(m_random));
	}


	private void testVerifyCertificate(AbstractTestKeyPairGenerator a_keyPairGenerator)
		throws Exception
	{
		AsymmetricCryptoKeyPair keyPair;
		JAPCertificate certificate = null;
		Vector certificateStore = new Vector();
		Vector pkcs12Certificates = new Vector();
		PKCS12 signingCertificate, signingCertificate2;
		ByteArrayOutputStream testOutput;
		ByteArrayInputStream testInput;

		// generate some certificates
		for (int i = 0; i < 5; i++)
		{
			keyPair = a_keyPairGenerator.createKeyPair();
			signingCertificate = new PKCS12("DummyOwner" + i, keyPair, new GregorianCalendar(), 1);

			// test if we can store and load PKCS12 certificates to/from an output stream
			testOutput = new ByteArrayOutputStream();
			signingCertificate.store(testOutput, new char[0]);
			testInput = new ByteArrayInputStream(testOutput.toByteArray());
			signingCertificate = PKCS12.getInstance(testInput, new char[0]);

			certificate = signingCertificate.getX509Certificate();
			//certificate.setEnabled(true); // X509 certs derived from PKCS12 must be enabled by default!!
			certificateStore.addElement(certificate);
			pkcs12Certificates.addElement(signingCertificate);
		}

		// use the last certificate for testing
		assertTrue(certificateStore.removeElement(certificate));

		// this certificate cannot be verified
		assertFalse(certificate.verify(certificateStore));

		// sign the certificate with the first pkcs12 certificate
		signingCertificate = (PKCS12)pkcs12Certificates.elementAt(0);
		certificate = certificate.sign(signingCertificate);

		// the certificate can be verified!
		assertTrue(certificate.verify(certificateStore));

		// put the signing certificate at the end of the list; the certificate can still be verified
		certificateStore.removeElement(signingCertificate.getX509Certificate());
		certificateStore.addElement(signingCertificate.getX509Certificate());
		assertTrue(certificate.verify(certificateStore));

		// the signing certificate is removed completely; now the certificate cannot be verified!
		certificateStore.removeElement(signingCertificate.getX509Certificate());
		assertFalse(certificate.verify(certificateStore));

		// sign with two other pkcs12 certificates and verify the signature
		signingCertificate = (PKCS12)pkcs12Certificates.elementAt(3);
		certificate = certificate.sign(signingCertificate);
		signingCertificate2 = (PKCS12)pkcs12Certificates.elementAt(2);
		certificate = certificate.sign(signingCertificate2);
		assertTrue(certificate.verify(certificateStore));

		// remove the first of the signing certificates (never mind; its signature has been overwritten)
		certificateStore.removeElement(signingCertificate.getX509Certificate());
		assertTrue(certificate.verify(certificateStore));
	}

}
