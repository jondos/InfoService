package anon.crypto;

import java.security.PublicKey;
import java.util.Enumeration;
import org.w3c.dom.Node;
import anon.server.impl.XMLUtil;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import anon.ErrorCodes;

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
				PublicKey pkc = c.getPublicKey();
				if (pkc.equals(pk) || cert.verify(pkc))
				{
					LogHolder.log(LogLevel.DEBUG, LogType.MISC,
								  "JAPCertPath: validation of cert paht ok");
					return ErrorCodes.E_SUCCESS;
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
