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

/* Hint: This file may be only a copy of the original file which is always in the JAP source tree!
 * If you change something - do not forget to add the changes also to the JAP source tree!
 */
import java.util.Vector;
import org.w3c.dom.Node;
import anon.ErrorCodes;

/**
 * @deprecated use {@link anon.crypto.XMLSignature} instead; scheduled for removal on 04/12/12
 */
public abstract class JAPCertPath
{

	private JAPCertPath()
	{
	}

	/** Validates the XML Signature over root done by nodeSig according to certsTrustedRoots
	 * If a Certifcate is embedded within the Signature we try to verify also this certifcate chain
	 * If root is a Document then the Root Element of that Document is taken.
	 * ATTENTION: All certificates must include DSAPublicKeys!!!
	 * @return ErrorCodes.E_SUCCESS if ok
	 * @return ErrorCodes.E_INVALID_KEY if the provides key does not match to the signature
	 * @return ErrorCodes.E_UNKNOWN otherwise
	 * @deprecated use {@link anon.crypto.XMLSignature#verify(Node,JAPCertificateStore)} instead;
	 *             scheduled for removal on 04/12/12
	 */
	public static int validate(Node root, Node nSig, JAPCertificateStore certsTrustedRoots)
	{
		try
		{
			JAPCertificate[] appendedCerts = JAPSignature.getAppendedCertificates(nSig);
			if (appendedCerts != null)
			{
				/* there is a certificate appended */
				JAPCertificate appendedCertificate = appendedCerts[0];
				if (appendedCertificate != null)
				{
					/* check the signature against the public key from the certificate */
					try
					{
						JAPSignature signatureInstance = new JAPSignature();
						signatureInstance.initVerify(appendedCertificate.getPublicKey());
						if (signatureInstance.verifyXML(root) == true)
						{
							/* signature is valid, try to verify the appended certificate against the trusted
							 * root certificates
							 */
							if (appendedCertificate.verify(certsTrustedRoots))
							{
								return ErrorCodes.E_SUCCESS;
							}

						}
					}
					catch (Exception ta)
					{
					}
				}
			}
			/* there was no appended certificate, check the signature against all public keys from
			 * the store of trusted certificates
			 */
			return validate(root, certsTrustedRoots);
		}
		catch (Exception e)
		{
		}
		return ErrorCodes.E_UNKNOWN;
	}

	/** Checks if a given Certificate could be directly verified agains a set a trusted Certificates
	 * @return ErrorCodes.E_SUCCESS if this check was ok
	 * @return ErrorCodes.E_UNKNOWN otherwise
	 * @deprecated use {@link anon.crypto.JAPCertificate#verify(JAPCertificateStore)} instead;
	 *             scheduled for removal on 04/12/12
	 */
	public static int validate(JAPCertificate cert, JAPCertificateStore certsTrustedRoots)
	{
		if (cert.verify(certsTrustedRoots))
		{
			return ErrorCodes.E_SUCCESS;
		}
		else
		{
			return ErrorCodes.E_UNKNOWN;
		}
	}

	/* check the signature directly against all public keys from the store of trusted certificates.
	 * Certs included within the Signature are ignored!
	 * @return ErrorCodes.E_SUCCESS if ok
	 * @return ErrorCodes.E_INVALID_KEY if no key matches
	 * @deprecated use {@link anon.crypto.XMLSignature#verify(Node,JAPCertificateStore)} instead;
	 *             scheduled for removal on 04/12/12
	 */
	public static int validate(Node root, JAPCertificateStore certsTrustedRoots)
	{
		if (certsTrustedRoots == null)
		{
			return ErrorCodes.E_INVALID_KEY;
		}
		Vector allCertificates = certsTrustedRoots.getAllEnabledCertificates();
		JAPSignature signatureInstance = new JAPSignature();
		for (int i = 0; i < allCertificates.size(); i++)
		{
			/* take the first certificate */
			try
			{
				signatureInstance.initVerify( ( (JAPCertificate) (allCertificates.elementAt(i))).
											 getPublicKey());
				if (signatureInstance.verifyXML(root))
				{

					/* signature could be verified directly against a trusted root */
					return ErrorCodes.E_SUCCESS;
				}
			}
			catch (Exception e)
			{
			}

		}
		/* we could not verify the signature (without certificate) against a trusted root */
		return ErrorCodes.E_INVALID_KEY;
	}
}
