package anon.pay;

import anon.crypto.JAPSignature;
import anon.crypto.JAPCertificate;
import java.security.InvalidKeyException;

/**
 * This class represents a known BI with its unique name and a public key (verifyer).
 *
 * @author Bastian Voigt
 * @version 1.0
 */
public class BI
{
	private String m_biName;
	private String m_hostName;
	private int m_portNumber;

	private JAPSignature m_veryfire;

	public BI(String biName, String hostName, int portNumber, JAPSignature verifier)
	{
		m_veryfire = verifier;
		m_biName = biName;
		m_hostName = hostName;
		m_portNumber = portNumber;
	}

	public BI(byte[] barCert, String biName, String hostName, int portNumber)
	{
		JAPCertificate cert = JAPCertificate.getInstance(barCert);

		/** @todo does this work? i don't believe it... */
		m_biName = cert.getSubject().CN.toString();
		m_veryfire = new JAPSignature();
		try
		{
			m_veryfire.initVerify(cert.getPublicKey());
		}
		catch (InvalidKeyException ex)
		{
			m_veryfire = null;
		}
	}

	public String getName()
	{
		return m_biName;
	}

	public JAPSignature getVerifier()
	{
		return m_veryfire;
	}

	public String getHostName()
	{
		return m_hostName;
	}

	public int getPortNumber()
	{
		return m_portNumber;
	}
}
