/*
 Copyright (c) 2000 - 2003, The JAP-Team
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

/* Hint: This file may be only a copy of the original file which is always in the JAP source tree!
 * If you change something - do not forget to add the changes also to the JAP source tree!
 */

package anon.crypto;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.SignatureException;
import java.util.Date;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.BERInputStream;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.DEROutputStream;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x509.TBSCertificateStructure;
import org.bouncycastle.asn1.x509.X509CertificateStructure;
import org.bouncycastle.asn1.x509.X509Name;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import anon.util.Base64;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
/**
 * A certificate class.
 *
 */
final public class JAPCertificate
{
	private X509CertificateStructure m_x509cert;
	private PublicKey m_PubKey;
	private boolean m_bEnabled;

	private JAPCertificate()
	{
	}

	public static JAPCertificate getInstance(X509CertificateStructure x509cert) throws JAPCertificateException
	{
		try
		{
			JAPCertificate r_japcert = new JAPCertificate();
			try
			{
				r_japcert.m_PubKey = new MyDSAPublicKey(x509cert.getSubjectPublicKeyInfo());
			}
			catch(Exception e)
			{
				try
				{
					r_japcert.m_PubKey = new MyRSAPublicKey(x509cert.getSubjectPublicKeyInfo());
				}
				catch(Exception e1)
				{
					throw new JAPCertificateException();
				}
			}
			r_japcert.m_x509cert = x509cert;

			return r_japcert;
		}
		catch (Exception e)
		{
			throw new JAPCertificateException();
		}
	}

	/** Creates a certificate instance by using an inputstream.
	 *
	 * @param a_in Inputstream that holds the certificate
	 * @return Certificate
	 */
	public static JAPCertificate getInstance(InputStream a_in) throws JAPCertificateException
	{
		try
		{
			BERInputStream bis = new BERInputStream(a_in);
			ASN1Sequence seq = (ASN1Sequence) bis.readObject();
			X509CertificateStructure x509cert = new X509CertificateStructure(seq);
			return getInstance(x509cert);
		}
		catch (Exception e)
		{
			throw new JAPCertificateException();
		}
	}


	/** Creates a certificate instance by using a XML Node as input.
	 *
	 * @param a_NodeRoot X509Certificate XML Node
	 * @return Certificate
	 */
	public static JAPCertificate getInstance(Node a_NodeRoot) throws IOException
	{
		if (!a_NodeRoot.getNodeName().equals("X509Certificate"))
		{
			return null;
		}

		Element elemX509Cert = (Element) a_NodeRoot;
		Text txtX509Cert = (Text) elemX509Cert.getFirstChild();
		String strValue = txtX509Cert.getNodeValue();
		byte[] bytecert = Base64.decode(strValue.toCharArray());

		try
		{
			return getInstance(bytecert);
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.ERR, LogType.MISC,
						  "JAPCertificate:getInstance(Node) failed!");
		}
		return null;
	}

	/** Creates a certificate instance by using a file.
	 *
	 * @param a_file File that holds the certificate
	 * @return Certificate
	 */
	public static JAPCertificate getInstance(File a_file) throws JAPCertificateException
	{
		if (a_file != null)
		{
			try
			{
				byte[] buff = new byte[ (int) a_file.length()];
				FileInputStream fin = new FileInputStream(a_file);
				fin.read(buff);
				fin.close();
				return JAPCertificate.getInstance(buff);
			}
			catch (JAPCertificateException e)
			{
				throw new JAPCertificateException();
			}
			catch (FileNotFoundException e)
			{
				LogHolder.log(LogLevel.EXCEPTION, LogType.MISC,
							  "JAPCertificate:getInstance(File) - Certificate file not found!");
			}
			catch (Exception e)
			{
				LogHolder.log(LogLevel.EXCEPTION, LogType.MISC,
							  "JAPCertificate:getInstance(File) failed!");
			}
		}
		return null;
	}

	/** Creates a certificate instance by using a file name.
	 *
	 * @param a_strFileName Name of File that holds the certificate
	 * @return Certificate
	 */
	public static JAPCertificate getInstance(String a_strFileName) throws JAPCertificateException
	{
		return getInstance(new File(a_strFileName));
	}

	/** Creates a certificate instance by using the encoded variant of the certificate
	 *
	 * @param a_encoded Byte Array of the Certificate
	 * @return Certificate
	 */
	public static JAPCertificate getInstance(byte[] a_encoded) throws JAPCertificateException
	{
		try
		{
			return getInstance(new ByteArrayInputStream(a_encoded));
		}
		catch (JAPCertificateException e)
		{
			throw new JAPCertificateException();
		}
		catch (Exception e)
		{
			return null;
		}
	}

/*
	public static class IllegalCertificateException extends RuntimeException
	{
		public IllegalCertificateException(String str)
		{
			super(str);
		}
	};
*/

	/** Returns the start date of the certificate.
	 *
	 * @return Date (start)
	 */
	public Date getStartDate()
	{
		return m_x509cert.getStartDate().getDate();
	}

	/** Returns the date when certificate expires.
	 *
	 * @return Date (expire)
	 */
	public Date getEndDate()
	{
		return m_x509cert.getEndDate().getDate();
	}

	/** Returns the TBS certificate structure of a certificate.
	 *
	 * @return TBSCertificateStructure
	 */
	public TBSCertificateStructure getTBSCertificate()
	{
		return m_x509cert.getTBSCertificate();
	}

	/** Returns the serial number of the certificate.
	 *
	 * @return Serial Number
	 */
	public BigInteger getSerialNumber()
	{
		return m_x509cert.getSerialNumber().getPositiveValue();
	}

	/** Returns the signature of the certificate.
	 *
	 * @return Signature
	 */
	public DERBitString getSignature()
	{
		return m_x509cert.getSignature();
	}

	/** Returns the algorithm identifier for the signature algorithm of certificate.
	 *
	 * @return AlgorithmIdentifier
	 */
	public AlgorithmIdentifier getSignatureAlgorithm()
	{
		return m_x509cert.getSignatureAlgorithm();
	}

	/** Returns the subject public key info of the certificate.
	 *
	 * @return SubjectPublicKeyInfo
	 */
	public SubjectPublicKeyInfo getSubjectPublicKeyInfo()
	{
		return m_x509cert.getSubjectPublicKeyInfo();
	}

	/** Returns the issuer of the certificate as an X509Name object.
	 *
	 * @return issuer (X509Name)
	 */
	public X509Name getIssuer()
	{
		return m_x509cert.getIssuer();
	}

	/** Returns the subject of the certificate as an X509Name object.
	 *
	 * @return subject (X509Name)
	 */
	public X509Name getSubject()
	{
		return m_x509cert.getSubject();
	}

	/** Returns the version number.
	 *
	 * @return version
	 */
	public int getVersion()
	{
		return m_x509cert.getVersion();
	}

	/** Returns the public key of the certificate.
	 *
	 * @return public key
	 */
	public PublicKey getPublicKey()
	{
		return m_PubKey;
	}

	/** Returns the encoded form of the certificate (char array).
	 *
	 * @return encoded certificate
	 */
	public char[] getEncoded()
	{
		ByteArrayOutputStream bArrOStream = new ByteArrayOutputStream();
		DEROutputStream dOStream = new DEROutputStream(bArrOStream);
		try
		{
			dOStream.writeObject(this.m_x509cert);
			dOStream.close();
		}
		catch (IOException e)
		{
			LogHolder.log(LogLevel.EXCEPTION, LogType.MISC,
						  "JAPCertificate:getEncoded() failed (IOException)");
		}
		return Base64.encode(bArrOStream.toByteArray());
	}

	/** Checks if the certificate starting date is not before a given date and
	 *  date of is not beyond the given date
	 * @param a_date (Date)
	 * @return true if certificate dates are within range of the given date
	 * @return false if that's not the case
	 */
	public boolean isDateValid(Date a_date)
	{
		boolean bValid = true;
		bValid = (a_date.before(getStartDate()) || a_date.after(getEndDate()));
		return bValid;
	}

	/** Changes the status of the certificate.
	 * @param a_bEnabled (Status)
	 */
	public void setEnabled(boolean a_bEnabled)
	{
		m_bEnabled = a_bEnabled;
	}

	/** Returns the status of the certificate.
	 * @return status
	 */
	public boolean getEnabled()
	{
		return m_bEnabled;
	}

	/** Verifies the certificate by using the public key.
	 * @param a_pubkey given public key
	 * @return true if it could be verified
	 * @return false if that's not the case
	 */
	public boolean verify(PublicKey a_pubkey) throws NoSuchAlgorithmException,
		InvalidKeyException, SignatureException, JAPCertificateException
	{
		try
		{
			ByteArrayOutputStream bArrOStream = new ByteArrayOutputStream();

			JAPSignature sig = new JAPSignature();
			sig.initVerify(a_pubkey);

			(new DEROutputStream(bArrOStream)).writeObject(this.getTBSCertificate());

			byte[] bArrSigToVerify = this.getSignature().getBytes();
			return sig.verify(bArrOStream.toByteArray(), bArrSigToVerify);
		}
		catch (IOException e)
		{
			LogHolder.log(LogLevel.EXCEPTION, LogType.MISC,
						  "JAPCertificate:verify() failed (IOException)");
		}
		return false;
	}

	/**
	 * Creates XML node of certificate
	 *
	 * @param doc The XML document, which is the environment for the created XML node.
	 *
	 * @return Certificate as XML node.
	 */

	public Element toXmlNode(Document a_doc)
	{

		Element elemKeyInfo = a_doc.createElement("KeyInfo");
		Element elemX509Data = a_doc.createElement("X509Data");
		elemKeyInfo.appendChild(elemX509Data);
		Element elemX509Cert = a_doc.createElement("X509Certificate");
		elemX509Data.appendChild(elemX509Cert);
		elemX509Cert.appendChild(a_doc.createTextNode(String.valueOf(getEncoded())));

		return elemKeyInfo;
	}

}
