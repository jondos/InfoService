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
import java.security.PublicKey;
import java.security.interfaces.DSAPublicKey;
import java.util.Vector;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.w3c.dom.Element;

import anon.ErrorCodes;

public class JAPCertPath
{

  private JAPCertPath() {
  }

  /** Validates the XML Signature over root done by nodeSig according to certsTrustedRoots
   * ATTENTION: All certificates must include DSAPublicKeys!!!
   * @return ErrorCodes.E_SUCCESS if ok
   * @return ErrorCodes.E_INVALID_KEY if the provides key does not match to the signature
   * @return ErrorCodes.E_INVALID_CERTIFICATE if the trustwortyness of the key could not verified
   * @return ErrorCodes.E_UNKNOWN otherwise
   */
  public static int validate(Node nRoot, Node nSig, JAPCertificateStore certsTrustedRoots) {
    int r_errorCode = ErrorCodes.E_UNKNOWN;
    try {
    	if(!(nRoot instanceof Element)||!(nSig instanceof Element))
		{
			return ErrorCodes.E_UNKNOWN;
		}
		Element nodeSig=(Element)nSig;
		Element root=(Element)nRoot;
		NodeList keyInfoNodes = nodeSig.getElementsByTagName("KeyInfo");
      if (keyInfoNodes.getLength() > 0) {
        /* there is a certificate appended */
        JAPCertificate appendedCertificate = JAPCertificate.getInstance((Element)(keyInfoNodes.item(0)));
        if (appendedCertificate != null) {
          /* check the signature against the public key from the certificate */
          JAPSignature signatureInstance = new JAPSignature();
          signatureInstance.initVerify(appendedCertificate.getPublicKey());
          if (signatureInstance.verifyXML(root) == true) {
            /* signature is valid, try to verify the appended certificate against the trusted
             * root certificates
             */
            Vector allCertificates = certsTrustedRoots.getAllEnabledCertificates();
            boolean certificateVerified = false;
            while ((allCertificates.size() > 0) && (certificateVerified == false)) {
              PublicKey currentPublicKey = ((JAPCertificate)(allCertificates.firstElement())).getPublicKey();
              allCertificates.removeElementAt(0);
              try {
                if (currentPublicKey.equals(appendedCertificate.getPublicKey()) ||
                    (appendedCertificate.verify(currentPublicKey))) {
                  /* the appended certificate is identical to the current trusted root certificate
                   * (same public key) or could be derived from it
                   */
                  certificateVerified = true;
                }
              }
              catch (Exception e) {
                /* there was an error while comparing the certificate-keys or while checking the
                 * signature of a certificate -> ignore that certificate
                 */
              }
            }
            if (certificateVerified == true) {
              /* appended certificate is in the list of enabled trusted certificates or could be
               * derived from one of them
               */
              r_errorCode = ErrorCodes.E_SUCCESS;
            }
            else {
              /* appended certificate could not be derived from one of the trusted root
               * certificates
               */
              r_errorCode = ErrorCodes.E_INVALID_CERTIFICATE;
            }
          }
          else {
            /* signature is invalid -> return E_INVALID_KEY */
            r_errorCode = ErrorCodes.E_INVALID_KEY;
          }
        }
        else {
          /* something is wrong with the appended certificate -> return E_UNKNOWN */
          r_errorCode = ErrorCodes.E_UNKNOWN;
        }
      }
      else {
        /* there was no appended certificate, check the signature against all public keys from
         * the store of trusted certificates
         */
        Vector allCertificates = certsTrustedRoots.getAllEnabledCertificates();
        boolean signatureVerified = false;
        while ((allCertificates.size() > 0) && (signatureVerified == false)) {
          JAPSignature signatureInstance = new JAPSignature();
          /* take the first certificate */
          signatureInstance.initVerify(((JAPCertificate)(allCertificates.firstElement())).getPublicKey());
          allCertificates.removeElementAt(0);
          signatureVerified = signatureInstance.verifyXML(root);
        }
        if (signatureVerified == true) {
          /* signature could be verified directly against a trusted root */
          r_errorCode = ErrorCodes.E_SUCCESS;
        }
        else {
          /* we could not verify the signature (without certificate) against a trusted root */
          r_errorCode = ErrorCodes.E_INVALID_KEY;
        }
      }
      return r_errorCode;
    }
    catch (Exception e) {
    }
    return ErrorCodes.E_UNKNOWN;
  }

}
