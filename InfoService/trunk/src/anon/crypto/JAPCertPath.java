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
import java.security.PublicKey;
import java.util.Enumeration;
import org.w3c.dom.Node;
import anon.ErrorCodes;
import anon.util.XMLUtil;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

public class JAPCertPath
{
	private JAPCertPath()
	{}

	/** Validates the XML Signature over root done by nodeSig according to certsTrustedRoots
	 * @return ErrorCodes.E_SUCCESS if ok
	 * @return ErrorCodes.E_INVALID_KEY if the provides key does not match to the signature
	 * @return ErrorCodes.E_INVALID_CERTIFICATE if the trustwortyness of the key could not verified
	 * @return ErrorCodes.E_UNKNOWN otherwise
	 */

	public static int validate(Node root, Node nodeSig, JAPCertificateStore certsTrustedRoots)
	{
		try
		{
			LogHolder.log(LogLevel.DEBUG, LogType.MISC, "JAPCertPath: beginn ok validation");
			JAPCertificate cert = JAPCertificate.getInstance(XMLUtil.getFirstChildByNameUsingDeepSearch(
				nodeSig,
				"X509Certificate"));
			PublicKey pk = cert.getPublicKey();
			//check Signature of root
			JAPSignature sig = new JAPSignature();
			sig.initVerify(pk);
			if (!sig.verifyXML(root))
			{
				LogHolder.log(LogLevel.DEBUG, LogType.MISC, "JAPCertPath: signasture NOT ok!");
				return ErrorCodes.E_INVALID_KEY;
			}

			// sig is ok --> verify certificate(s)
			LogHolder.log(LogLevel.DEBUG, LogType.MISC, "JAPCertPath: signasture ok --> checking cert path");
			Enumeration certs = certsTrustedRoots.elements();
			while (certs.hasMoreElements())
			{
				JAPCertificate c = (JAPCertificate) certs.nextElement();
				// ignore disabled certificate within the certificate store!
				if (c.getEnabled())
				{
					PublicKey pkc = c.getPublicKey();
					if (pkc.equals(pk) || cert.verify(pkc))
					{
						LogHolder.log(LogLevel.DEBUG, LogType.MISC,
									  "JAPCertPath: validation of cert paht ok");
						return ErrorCodes.E_SUCCESS;
					}
				}
			}
			return ErrorCodes.E_INVALID_CERTIFICATE;
		}
		catch (Exception ex)
		{
		}

		return ErrorCodes.E_UNKNOWN;
	}

}
