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

/* Hint: This file may be only a copy of the original file which is always in the JAP source tree!
 * If you change something - do not forget to add the changes also to the JAP source tree!
 */

package anon.crypto;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Calendar;
import java.util.Date;
import java.util.Enumeration;
import java.util.Vector;

import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.BERInputStream;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.DEREncodableVector;
import org.bouncycastle.asn1.DERInputStream;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DEROutputStream;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.asn1.DERTags;
import org.bouncycastle.asn1.DERUTCTime;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.SignedData;
import org.bouncycastle.asn1.x509.SubjectPublicKeyInfo;
import org.bouncycastle.asn1.x509.TBSCertificateStructure;
import org.bouncycastle.asn1.x509.V3TBSCertificateGenerator;
import org.bouncycastle.asn1.x509.X509CertificateStructure;
import org.bouncycastle.asn1.x509.X509Name;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.Text;

import anon.util.Base64;
import anon.util.IXMLEncodable;
import anon.util.XMLUtil;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/**
 * A certificate class.
 */
final public class JAPCertificate extends X509CertificateStructure implements IXMLEncodable, Cloneable,
	ICertificate
{

	/**
	 * This are the certificate type constant for root certificates. Root certificates are used to
	 * verify other certificates (at the moment only one indirection is supported, so root
	 * certificates cannot verify other root certificates).
	 * At the moment we have root certificates for Mixes and for InfoServices.
	 */
	public static final int CERTIFICATE_TYPE_ROOT_MIX = 1;
	public static final int CERTIFICATE_TYPE_ROOT_INFOSERVICE = 5;

	/**
	 * This is the certificate type constant for mix certificates. Mix certificates are used to
	 * create or verify the signature of mix, mixcascade or cascade-state XML structures.
	 */
	public static final int CERTIFICATE_TYPE_MIX = 2;

	/**
	 * This is the certificate type constant for infoservice certificates. InfoService certificates
	 * are used to create or verify the signature of an infoservice XML structure.
	 */
	public static final int CERTIFICATE_TYPE_INFOSERVICE = 3;

	/**
	 * This is the certificate type constant for update certificates. Update certificates are used
	 * to create or verify the signature of all JAP update related structures like the minimum
	 * required JAP version or the Java WebStart files for the release or development version of
	 * JAP.
	 */
	public static final int CERTIFICATE_TYPE_UPDATE = 4;

	public static final String XML_ELEMENT_NAME = "X509Certificate";
	public static final String XML_ELEMENT_CONTAINER_NAME = "X509Data";

	/**
	 * This is the time (in minutes) a temporary certificate is valid.
	 */
	private static final int TEMPORARY_VALIDITY_IN_MINUTES = 10;

	/**
	 * The dummy private key is used to create temporary certificates.
	 */
	private static IMyPrivateKey ms_dummyPrivateKey;

	private IMyPublicKey m_PubKey;
	private String m_id;

	/**
	 * Creates a new certificate from a valid X509 certificate structure.
	 * @param x509cert a valid X509 certificate structure
	 * @exception IllegalArgumentException if the certificate structure is invalid
	 */
	private JAPCertificate(X509CertificateStructure x509cert) throws IllegalArgumentException
	{
		super(ASN1Sequence.getInstance(new DERTaggedObject(true, DERTags.BIT_STRING, x509cert), true));

		try
		{
			m_PubKey = AsymmetricCryptoKeyPair.createPublicKey(x509cert.getSubjectPublicKeyInfo());
		}
		catch (Exception a_e)
		{
			LogHolder.log(LogLevel.EXCEPTION, LogType.MISC, "Exception in JAOCertifciate():" + a_e.getMessage());
			LogHolder.log(LogLevel.EXCEPTION, LogType.MISC, a_e);
			throw new IllegalArgumentException(
				"Certificate structure contains invalid public key! " + a_e);
		}

		m_id = generateId();
	}

	/**
	 * Creates a new certificate from a valid X509 certificate structure.
	 * @param x509cert a valid X509 certificate structure
	 * @return null if no certificate could be created from the certificate structure
	 */
	public static JAPCertificate getInstance(X509CertificateStructure x509cert)
	{
		JAPCertificate certificate;

		try
		{
			certificate = new JAPCertificate(x509cert);
		}
		catch (IllegalArgumentException a_e)
		{
			return null;
		}

		return certificate;
	}

	/**
	 * Creates a certificate instance by using the encoded variant of the certificate.
	 * @param a_certificate Byte Array of the Certificate
	 * @return Certificate
	 */
	public static JAPCertificate getInstance(byte[] a_certificate)
	{
		ByteArrayInputStream bin = null;

		try
		{
			if (a_certificate[0] != (DERInputStream.SEQUENCE | DERInputStream.CONSTRUCTED))
			{
				// Probably a Base64 encoded certificate
				BufferedReader in =
					new BufferedReader(
						new InputStreamReader(new ByteArrayInputStream(a_certificate)));
				StringBuffer sbuf = new StringBuffer();
				String line;

				while ( (line = in.readLine()) != null)
				{
					if (line.equals("-----BEGIN CERTIFICATE-----")
						|| line.equals("-----BEGIN X509 CERTIFICATE-----"))
					{
						break;
					}
				}

				while ( (line = in.readLine()) != null)
				{
					if (line.equals("-----END CERTIFICATE-----")
						|| line.equals("-----END X509 CERTIFICATE-----"))
					{
						break;
					}
					sbuf.append(line);
				}
				bin = new ByteArrayInputStream(Base64.decode(sbuf.toString()));
			}

			if (bin == null && a_certificate[1] == 0x80)
			{
				// a BER encoded certificate
				BERInputStream in = new BERInputStream(new ByteArrayInputStream(a_certificate));
				ASN1Sequence seq = (ASN1Sequence) in.readObject();
				return getInstance(new X509CertificateStructure(seq));
			}
			else
			{
				if (bin == null)
				{
					bin = new ByteArrayInputStream(a_certificate);
					// DERInputStream
				}
				DERInputStream in = new DERInputStream(bin);
				ASN1Sequence seq = (ASN1Sequence) in.readObject();
				if (seq.size() > 1
					&& seq.getObjectAt(1) instanceof DERObjectIdentifier
					&& seq.getObjectAt(0).equals(PKCSObjectIdentifiers.signedData))
				{
					return getInstance(X509CertificateStructure.getInstance(
						new SignedData(
							ASN1Sequence.getInstance(
								(ASN1TaggedObject) seq.getObjectAt(1),
								true)).getCertificates()
						.getObjectAt(0)));
				}
				return getInstance(new X509CertificateStructure(seq));
			}
		}
		catch (Exception a_e)
		{
			LogHolder.log(LogLevel.EXCEPTION, LogType.MISC,
						  "Exception while createing JAPCertifcate: " + a_e.getMessage());
			LogHolder.log(LogLevel.EXCEPTION, LogType.MISC, a_e);
			return null;
		}
	}

	/** Creates a certificate by using an input stream.
	 *
	 * @param a_in Inputstream that holds the certificate
	 * @return Certificate
	 */
	public static JAPCertificate getInstance(InputStream a_in)
	{
		StringBuffer sbuf = new StringBuffer();
		BufferedReader reader = new BufferedReader(new InputStreamReader(a_in));
		String line;

		try
		{
			while ( (line = reader.readLine()) != null)
			{
				sbuf.append(line);
			}
		}
		catch (IOException a_e)
		{
			return null;
		}

		return getInstance(sbuf.toString().getBytes());
	}

	/**
	 * Creates a certificate instance by using a XML Node as input.
	 * @param a_NodeRoot <X509Certificate> XML Node
	 * @return Certificate
	 */
	public static JAPCertificate getInstance(Node a_NodeRoot)
	{
		try
		{
			if (!a_NodeRoot.getNodeName().equals(XML_ELEMENT_NAME))
			{
				return null;
			}
			Element elemX509Cert = (Element) a_NodeRoot;
			String strValue = XMLUtil.parseValue(elemX509Cert, null);
			byte[] bytecert = Base64.decode(strValue);
			return getInstance(bytecert);
		}
		catch (Exception e)
		{
			return null;
		}
	}

	/** Creates a certificate instance by using a file (either DER encoded or PEM).
	 *
	 * @param a_file File that holds the certificate
	 * @return Certificate
	 */
	public static JAPCertificate getInstance(File a_file)
	{
		if (a_file == null)
		{
			return null;
		}
		byte[] buff = null;
		try
		{
			buff = new byte[ (int) a_file.length()];
			FileInputStream fin = new FileInputStream(a_file);
			fin.read(buff);
			fin.close();
		}
		catch (Exception e)
		{
			return null;
		}

		return JAPCertificate.getInstance(buff);
	}

	/** Creates a certificate instance by using a file name.
	 *
	 * @param a_strFileName Name of File that holds the certificate
	 * @return Certificate
	 */
	public static JAPCertificate getInstance(String a_strFileName)
	{
		try
		{
			return getInstance(new File(a_strFileName));
		}
		catch (Exception e)
		{
			return null;
		}
	}

	/**
	 * Creates an X509 certificate from a key pair.
	 * @param a_ownerAlias The owner of the certificate. The name is set as the common name (CN).
	 * @param a_keyPair a key pair
	 * @param a_validFrom The date from which the certificate is valid.
	 * @param a_validityInYears the number of years the certificate is valid;
	 *                          if the amount is 0 or less, the validity will
	 *                          only be ahead from a_validFrom for a very small amount of time
	 *                          (some minutes)
	 * @return an X509 certificate
	 */
	public static JAPCertificate getInstance(String a_ownerAlias, AsymmetricCryptoKeyPair a_keyPair,
											 Calendar a_validFrom, int a_validityInYears)
	{
		return getInstance(a_ownerAlias, a_keyPair, a_validFrom,
						   createValidTo(a_validFrom, a_validityInYears));
	}

	/**
	 * Creates an X509 certificate from a key pair.
	 * @param a_ownerAlias The owner of the certificate. The name is set as the common name (CN).
	 * @param a_keyPair a key pair
	 * @param a_validFrom The date from which the certificate is valid.
	 * @param a_validTo The date until which the certificate is valid.
	 * @return an X509 certificate
	 */
	public static JAPCertificate getInstance(String a_ownerAlias, AsymmetricCryptoKeyPair a_keyPair,
											 Calendar a_validFrom, Calendar a_validTo)
	{
		return getInstance(a_ownerAlias, a_keyPair.getPrivate(), a_keyPair.getPublic(),
						   a_validFrom, a_validTo);
	}

	/**
	 * For a given Calendar object that represents a start date (valid from), this method
	 * creates an end date (valid to) that lies a specified amount of years ahead from the start
	 * date.
	 * @param a_validFrom a Calendar object that represents a start date
	 * @param a_validityInYears an amount of years that the created date should lie ahead from the
	 *                          start date; if the amount is 0 or less, the created date will
	 *                          only be ahead from the start date for a very small time
	 *                          (some minutes)
	 * @return an end date (valid to) that lies a specified amount of years ahead from the start
	 *         date
	 */
	public static Calendar createValidTo(Calendar a_validFrom, int a_validityInYears)
	{
		Calendar validTo = (Calendar) a_validFrom.clone();

		if (a_validityInYears <= 0)
		{
			validTo.add(Calendar.MINUTE, TEMPORARY_VALIDITY_IN_MINUTES);
		}
		else
		{
			validTo.add(Calendar.YEAR, a_validityInYears);
		}

		return validTo;
	}

	/**
	 * Creates an X509 certificate with a short validity from a public key.
	 * The certificate has no owner an no valid signature, and it is not enabled.
	 * But this method is useful if there is a trusted public key, but no corresponding
	 * certificate available. Use this method with care, and enable the certificate only if the
	 * public key is really trusted!
	 * @param a_publicKey IMyPublicKey
	 * @param a_validFrom The date from which the certificate is valid.
	 * @return JAPCertificate
	 */
	public static final JAPCertificate getInstance(IMyPublicKey a_publicKey, Calendar a_validFrom)
	{
		return getInstance("void", getDummyPrivateKey(), a_publicKey,
						   a_validFrom, createValidTo(a_validFrom, 0));
	}

	public boolean equals(Object a_certificate)
	{
		JAPCertificate certificate;

		if (a_certificate == null || ! (a_certificate instanceof JAPCertificate))
		{
			return false;
		}

		// ok, this is a certificate
		certificate = (JAPCertificate) a_certificate;

		if (!getId().equals(certificate.getId()))
		{
			return false;
		}

		// is is almost impossible that the id and the dates are identical (they are part of the id)
		if (getStartDate().getDate().getTime() != certificate.getStartDate().getDate().getTime() ||
			getEndDate().getDate().getTime() != certificate.getEndDate().getDate().getTime())
		{
			return false;
		}

		return true;
	}

	public int hashCode()
	{
		return getId().hashCode();
	}

	public Object clone()
	{
		JAPCertificate cert = JAPCertificate.getInstance(this);
		if (cert == null)
		{
			return null;
		}
		return cert;
	}

	/**
	 * Generates a unique id for this certificate.
	 * @return a unique id for this certificate
	 */
	private String generateId()
	{
		byte[] digest;
		MessageDigest sha1;

		StringBuffer r_strBuffId = new StringBuffer();
		Enumeration enumer = getSubject().getValues().elements();
		while (enumer.hasMoreElements())
		{
			r_strBuffId.append( (String) enumer.nextElement());
		}
		r_strBuffId.append(getStartDate().getDate().getTime());
		r_strBuffId.append(getEndDate().getDate().getTime());
		r_strBuffId.append(new String(getPublicKey().getEncoded()));
		digest = r_strBuffId.toString().getBytes();
		try
		{
			sha1 = MessageDigest.getInstance("SHA-1");
			digest = sha1.digest(digest);
		}
		catch (NoSuchAlgorithmException a_e)
		{
			// doesn`t matter; if the algorithm does not exist, we take the whole String
		}

		return new String(digest);
	}

	/**
	 * Returns a unique id for this certificate.
	 * @return a unique id for this certificate
	 */
	public String getId()
	{
		return m_id;
	}

	/** Returns the public key of the certificate.
	 *
	 * @return public key
	 */
	public IMyPublicKey getPublicKey()
	{
		return m_PubKey;
	}

	public JAPCertificate getX509Certificate()
	{
		return this;
	}

	/**
	 * Converts the certificate to a byte array.
	 * @throws IOException
	 * @return the certificate as a byte array
	 */
	public byte[] toByteArray()
	{
		ByteArrayOutputStream out = new ByteArrayOutputStream();

		try
		{
			new DEROutputStream(out).writeObject(this);
		}
		catch (IOException a_e)
		{
			// I don`t think this is possible
		}

		return out.toByteArray();
	}

	/**
	 * Converts the certificate to a byte array.
	 * @param a_Base64Encoded if the certificate is converted to a Base64 encoded form.
	 * @throws IOException
	 * @return the certificate as a byte array
	 */
	public byte[] toByteArray(boolean a_Base64Encoded)
	{
		if (a_Base64Encoded)
		{
			ByteArrayOutputStream out = new ByteArrayOutputStream();

			try
			{
				out.write("-----BEGIN CERTIFICATE-----\n".getBytes());
				out.write(Base64.encode(toByteArray(), true).getBytes());
				out.write("\n-----END CERTIFICATE-----\n".getBytes());
			}
			catch (IOException a_e)
			{
				// should not be possible
			}

			return out.toByteArray();
		}
		else
		{
			return toByteArray();
		}
	}

	/**
	 * Writes this certificate to an output stream.
	 * @param a_ostream a OutputStream
	 * @exception IOException if the certificate could not be written to the output stream
	 */
	public void store(OutputStream a_ostream) throws IOException
	{
		DEROutputStream derOutputStream = new DEROutputStream(a_ostream);
		derOutputStream.writeObject(this);
	}

	/**
	 * Writes this certificate to an output stream.
	 * @param a_ostream a OutputStream
	 * @param a_bBase64Encoded if the certificate should be written Base64 encoded.
	 * @exception IOException if the certificate could not be written to the output stream
	 */
	public void store(OutputStream a_ostream, boolean a_bBase64Encoded) throws IOException
	{
		a_ostream.write(toByteArray(a_bBase64Encoded));
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
		bValid = (a_date.before(getStartDate().getDate()) || a_date.after(getEndDate().getDate()));
		return bValid;
	}

	/**
	 * Checks if a given Certificate could be directly verified against a set of other certificates.
	 * @param a_verifyingCertificates A Vector of JAPCertificates to verify this JAPCertificate.
	 * @return True, if this certificate could be verified.
	 * @todo do not accept expired certificates?
	 */
	public synchronized boolean verify(Vector a_verifyingCertificates)
	{
		if (a_verifyingCertificates == null)
		{
			return false;
		}

		Enumeration certificatesEnumerator = a_verifyingCertificates.elements();
		while (certificatesEnumerator.hasMoreElements())
		{
			JAPCertificate currentCertificate = (JAPCertificate) (certificatesEnumerator.nextElement());

			if (verify(currentCertificate))
			{
				return true;
			}
		}
		return false;
	}

	/**
	 * Verifies the certificate using an other X509 certificate.
	 *
	 * @param a_certificate an X509 certificate
	 * @return true if it could be verified; false otherwise
	 * @todo do not accept expired certificates?
	 */
	public synchronized boolean verify(JAPCertificate a_certificate)
	{
		if (a_certificate == null)
		{
			return false;
		}
		return (verify(a_certificate.getPublicKey()));
	}

	/** Verifies the certificate by using the public key.
	 * @param a_publicKey given public key
	 * @return true if it could be verified; false otherwise
	 */
	public synchronized boolean verify(IMyPublicKey a_publicKey)
	{
		if (a_publicKey == null)
		{
			return false;
		}

		// the cert is verified, too, if the public key is the same as the test key
		if (getPublicKey().equals(a_publicKey))
		{
			return true;
		}

		try
		{
			ByteArrayOutputStream bArrOStream = new ByteArrayOutputStream();
			(new DEROutputStream(bArrOStream)).writeObject(getTBSCertificate());

			return ByteSignature.verify(bArrOStream.toByteArray(), getSignature().getBytes(),
										a_publicKey);
		}
		catch (IOException a_e)
		{
			// should not happen
		}

		return false;
	}

	/**
	 * Creates a duplicate of this certificate that is signed with a the PKCS12 certificate.
	 * A certificate can have only one signature.
	 * @param a_pkcs12Certificate a PKCS12 certificate
	 * @return a duplicate of this certificate that is signed with a the PKCS12 certificate
	 */
	public JAPCertificate sign(PKCS12 a_pkcs12Certificate)
	{
		JAPCertificate certificate;
		X509CertificateStructure x509cert;
		X509CertificateGenerator certgen = new X509CertificateGenerator(getTBSCertificate());
		x509cert = certgen.sign(a_pkcs12Certificate);
		certificate = getInstance(x509cert);
		return certificate;
	}

	/**
	 * Creates XML element of certificate consisting of:
	 * <X509Certificate>
	 *  Base64 encocded cert
	 * </X509Certificate>
	 *
	 * @param a_doc The XML document, which is the environment for the created XML element.
	 *
	 * @return Certificate as XML element.
	 */
	public Element toXmlElement(Document a_doc)
	{
		Element elemX509Cert = a_doc.createElement(XML_ELEMENT_NAME);
		elemX509Cert.setAttribute("xml:space", "preserve");
		XMLUtil.setValue(elemX509Cert, Base64.encode(toByteArray(), true));
		return elemX509Cert;
	}

	/**
	 * Gets a dummy private key for creating X509 certificates from trusted public keys.
	 * @return a dummy private key for creating X509 certificates from trusted public keys
	 */
	private static IMyPrivateKey getDummyPrivateKey()
	{
		SecureRandom random;

		if (ms_dummyPrivateKey == null)
		{
			random = new SecureRandom();
			random.setSeed(58921787);
			ms_dummyPrivateKey = DSAKeyPair.getInstance(random, 256, 100).getPrivate();
		}

		return ms_dummyPrivateKey;
	}

	/**
	 * Creates an X509 certificate from a private and a public key. This method is private as
	 * it does not assure that private and public key correspond to each other. This is
	 * only assured by a key pair.
	 * @param a_ownerAlias The owner of the certificate. The name is set as the common name (CN).
	 * @param a_privateKey a private key
	 * @param a_publicKey a public key
	 * @param a_validFrom The date from which the certificate is valid.
	 * @param a_validTo The date until which the certificate is valid.
	 * @return an X509 certificate
	 */
	private static JAPCertificate getInstance(String a_ownerAlias, IMyPrivateKey a_privateKey,
											  IMyPublicKey a_publicKey,
											  Calendar a_validFrom, Calendar a_validTo)
	{
		X509CertificateGenerator v3CertGen;
		try
		{
			v3CertGen = new X509CertificateGenerator(a_ownerAlias, a_validFrom.getTime(),
				a_validTo.getTime(), a_publicKey);
		}
		catch (IOException a_e)
		{
			// should not happen
			v3CertGen = null;
		}
		X509CertificateStructure x509Cert =
			v3CertGen.sign(new X509Name("CN=" + a_ownerAlias), a_privateKey);
		return getInstance(x509Cert);
	}

	/**
	 * This class is used to create, duplicate and sign X509 certificates.
	 */
	private static final class X509CertificateGenerator extends V3TBSCertificateGenerator
	{
		public X509CertificateGenerator(String a_ownerAlias, Date a_validFrom, Date a_validTo,
										IMyPublicKey a_publicKey) throws IOException
		{
			setStartDate(new DERUTCTime(a_validFrom));
			setEndDate(new DERUTCTime(a_validTo));
			setSerialNumber(new DERInteger(1));
			setSubject(new X509Name("CN=" + a_ownerAlias));
			setSubjectPublicKeyInfo(new SubjectPublicKeyInfo( (ASN1Sequence) (new DERInputStream(new
				ByteArrayInputStream(a_publicKey.getEncoded()))).readObject()));
		}

		public X509CertificateGenerator(X509CertificateStructure cert)
		{
			this(cert.getTBSCertificate());
		}

		public X509CertificateGenerator(TBSCertificateStructure tbs)
		{
			setEndDate(tbs.getEndDate());
			setExtensions(tbs.getExtensions());
			setIssuer(tbs.getIssuer());
			setSerialNumber(tbs.getSerialNumber());
			setSignature(tbs.getSignature());
			setStartDate(tbs.getStartDate());
			setSubject(tbs.getSubject());
			setSubjectPublicKeyInfo(tbs.getSubjectPublicKeyInfo());
		}

		public X509CertificateStructure sign(PKCS12 a_pkcs12Certificate)
		{
			return sign(a_pkcs12Certificate.getX509Certificate().getSubject(),
						a_pkcs12Certificate.getPrivateKey());
		}

		public X509CertificateStructure sign(X509Name a_issuer, IMyPrivateKey a_privateKey)
		{
			try
			{
				TBSCertificateStructure tbsCert;
				DEREncodableVector seqv;
				ByteArrayOutputStream bOut;
				byte[] signature;

				setIssuer(a_issuer);
				setSignature(a_privateKey.getSignatureAlgorithm().getIdentifier());

				/* generate signature */
				bOut = new ByteArrayOutputStream();
				tbsCert = generateTBSCertificate();
				(new DEROutputStream(bOut)).writeObject(tbsCert);
				signature = ByteSignature.sign(bOut.toByteArray(), a_privateKey);

				/* construct certificate */
				seqv = new DEREncodableVector();
				seqv.add(tbsCert);
				seqv.add(a_privateKey.getSignatureAlgorithm().getIdentifier());
				seqv.add(new DERBitString(signature));

				return new X509CertificateStructure(new DERSequence(seqv));
			}

			catch (Throwable t)
			{
				t.printStackTrace();
				return null;
			}
		}
	}

}
