package payxml.util;

import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERInputStream;
import org.bouncycastle.asn1.DEROutputStream;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.DigestInfo;
import org.bouncycastle.asn1.x509.X509ObjectIdentifiers;

import org.bouncycastle.crypto.AsymmetricBlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.CryptoException;
import org.bouncycastle.crypto.Digest;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.encodings.PKCS1Encoding;
import org.bouncycastle.crypto.engines.RSAEngine;

import java.io.*;

import java.util.*;


/**
 * Signaturklasse zum Erzeugen und Testen von Signaturen mit
 * RSAwithSHA1/PKCS1/DER-kodierten Hashwert.
 */
public class Signer
{
	//~ Instance fields ********************************************************

	private AlgorithmIdentifier algId;
	private AsymmetricBlockCipher cipher;
	private Digest digest;
	private byte[] digestValue;

	//~ Constructors ***********************************************************

	/**
	 * Signaturklasse zum Erzeugen und Testen von Signaturen mit
	 * RSAwithSHA1/PKCS1/DER-kodierten Hashwert.
	 */
	public Signer()
	{
		cipher = new PKCS1Encoding(new RSAEngine());
		digest = new SHA1Digest();
		digestValue = new byte[20];

		//algId = new AlgorithmIdentifier(new DERObjectIdentifier("1.3.14.3.2.26"));
		algId = new AlgorithmIdentifier(X509ObjectIdentifiers.id_SHA1);
	}

	//~ Methods ****************************************************************

	/**
	 * generates a signature. Call init(true, privateKey) and update() first
	 *
	 * @return the binary signature value
	 */
	public byte[] generateSignature() throws CryptoException, IOException
	{
		digest.doFinal(digestValue, 0);

		byte[] derEncodedDigest = derEncode(digestValue);
		byte[] sunsig = cipher.processBlock(derEncodedDigest, 0,
				derEncodedDigest.length
			);

		return sunsig;
	}


	public void init(boolean forSigning, CipherParameters param)
	{
		cipher.init(forSigning, param);
		reset();
	}


	public void reset()
	{
		digest.reset();
	}


	/**
	     */
	public void update(byte[] in)
	{
		digest.update(in, 0, in.length);
	}


	/**
	 * verifies a signature. Call init(false, publicKey) and update() first
	 *
	 * @param signature the binary signature value
	 *
	 * @return true if signature is valid, false otherwise
	 */
	public boolean verify(byte[] signature)
	{
		try {
			byte[] derEncodedDigest = cipher.processBlock(signature, 0,
					signature.length
				);
			DigestInfo digestInfo = derDecode(derEncodedDigest);
			byte[] hash = digestInfo.getDigest();

			digest.doFinal(digestValue, 0);

			if (Arrays.equals(hash, digestValue)) {
				return true;
			} else {
				return false;
			}
		} catch (Exception e) {
			e.printStackTrace();

			return false;
		}
	}


	private DigestInfo derDecode(byte[] encoding) throws IOException
	{
		ByteArrayInputStream bIn = new ByteArrayInputStream(encoding);
		DERInputStream dIn = new DERInputStream(bIn);

		return new DigestInfo((ASN1Sequence) dIn.readObject());
	}


	private byte[] derEncode(byte[] hash) throws IOException
	{
		ByteArrayOutputStream bOut = new ByteArrayOutputStream();
		DEROutputStream dOut = new DEROutputStream(bOut);
		DigestInfo dInfo = new DigestInfo(algId, hash);
		dOut.writeObject(dInfo);

		return bOut.toByteArray();
	}
}
