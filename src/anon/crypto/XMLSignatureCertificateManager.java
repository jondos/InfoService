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

/**
 * Manages all certificates needed by a user of the XMLSignature class.
 * @see anon.crypto.XMLSignature
 * @author Rolf Wendolsky
 */
public final class XMLSignatureCertificateManager
{
	private PKCS12 m_pkcs12Certificate;
	private JAPCertificateStore m_trustedCertificates;
	private JAPCertificateStore m_collectedCertificates;

	/**
	 * Creates a new certificate manager. With this instance of the manager, it is possible to
	 * sign and verify XML documents. Documents can either be verified by trusted or collected
	 * certificates. If certificates are contained in an XML signature, and these certificates can
	 * be verified by a trusted certificate, they are added to the collected certificates.
	 * These collected certificates are also used to verify signatures.
	 *
	 * @param a_privateCertificate a private PKCS12 certificate to sign documents
	 * @param a_trustedCertificates trusted certificates to verify documents
	 * @param a_collectedCertificates verified certificates collected from verified XML signatures,
	 *                                used to verify other XML signatures
	 * @throws IllegalArgumentException if collected but no trusted certificates are given
	 */
	public XMLSignatureCertificateManager(PKCS12 a_privateCertificate,
										  JAPCertificateStore a_trustedCertificates,
										  JAPCertificateStore a_collectedCertificates)
		throws IllegalArgumentException
	{
		m_pkcs12Certificate = a_privateCertificate;
		m_trustedCertificates = a_trustedCertificates;
		m_collectedCertificates = a_collectedCertificates;


		if (!isSignatureCheckPossible() && m_collectedCertificates != null)
		{
			throw new IllegalArgumentException(
						 "Collected certificates cannot be used without trusted certificates!");
		}
	}


	/**
	 * Creates a new certificate manager. With this instance of the manager, it is only possible to
	 * verify XML documents. Documents can either be verified by trusted or collected
	 * certificates. If certificates are contained in an XML signature, and these certificates can
	 * be verified by a trusted certificate, they are added to the collected certificates.
	 * These collected certificates are also used to verify signatures.
	 *
	 * @param a_trustedCertificates trusted certificates to verify documents
	 * @param a_collectedCertificates verified certificates collected from verified XML signatures,
	 *                                used to verify other XML signatures
	 * @throws IllegalArgumentException if collected but no trusted certificates are given
	 *
	 */
	public XMLSignatureCertificateManager(JAPCertificateStore a_trustedCertificates,
										  JAPCertificateStore a_collectedCertificates)
		throws IllegalArgumentException
	{
		this(null, a_trustedCertificates, a_collectedCertificates);
	}

	/**
	 * Creates a new certificate manager. With this instance of the manager, it is possible to
	 * sign and verify XML documents. Documents can be verified by the trusted certificates only.
	 *
	 * @param a_privateCertificate a private PKCS12 certificate to sign documents
	 * @param a_trustedCertificates trusted certificates to verify documents
	 */
	public XMLSignatureCertificateManager(PKCS12 a_privateCertificate,
										  JAPCertificateStore a_trustedCertificates)
	{
		this(a_privateCertificate, a_trustedCertificates, null);
	}

	/**
	 * Creates a new certificate manager. With this instance of the manager, it is only possible to
	 * verify XML documents.  Documents can be verified by the trusted certificates only.
	 *
	 * @param a_trustedCertificates trusted certificates to verify documents
	 */
	public XMLSignatureCertificateManager(JAPCertificateStore a_trustedCertificates)
	{
		this(null, a_trustedCertificates, null);
	}

	/**
	 * Creates a new empty certificate manager. With this instance of the manager, it is not
	 * possible to perform any signature operation. <Code> isSignatureCheckPossible() </Code> will
	 * return <Code> false </Code>.
	 */
	public XMLSignatureCertificateManager()
	{
		this(null);
	}

	/**
	 * Returns the private certificate that is used to sign documents.
	 * @return the private certificate that is used to sign documents
	 */
	public PKCS12 getPrivateCertificate()
	{
		return this.m_pkcs12Certificate;
	}

	/**
	 * Returns the trusted certificates that are used to verify documents and other certificates.
	 * @return the trusted certificates that are used to verify documents and other certificates
	 */
	public JAPCertificateStore getTrustedCertificates()
	{
		return this.m_trustedCertificates;
	}

	/**
	 * Returns the collected certificates that are used to verify documents.
	 * @return the collected certificates that are used to verify documents
	 */
	public JAPCertificateStore getCollectedCertificates()
	{
		return this.m_collectedCertificates;
	}

	/**
	 * Returns if this certificate manager can be used to verify documents.
	 * @return true if this certificate manager can be used to verify documents; false otherwise
	 */
	public boolean isSignatureCheckPossible()
	{
		return (m_trustedCertificates != null && m_trustedCertificates.size() > 0);
	}
}
