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

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.InvalidKeyException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.interfaces.DSAPrivateKey;
import java.util.Enumeration;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1Set;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.BERConstructedOctetString;
import org.bouncycastle.asn1.BERInputStream;
import org.bouncycastle.asn1.BEROutputStream;
import org.bouncycastle.asn1.DERBMPString;
import org.bouncycastle.asn1.DERConstructedSet;
import org.bouncycastle.asn1.DEREncodableVector;
import org.bouncycastle.asn1.DERInputStream;
import org.bouncycastle.asn1.DERObject;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DEROutputStream;
import org.bouncycastle.asn1.DERSequence;
import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.asn1.pkcs.AuthenticatedSafe;
import org.bouncycastle.asn1.pkcs.CertBag;
import org.bouncycastle.asn1.pkcs.ContentInfo;
import org.bouncycastle.asn1.pkcs.EncryptedData;
import org.bouncycastle.asn1.pkcs.EncryptedPrivateKeyInfo;
import org.bouncycastle.asn1.pkcs.MacData;
import org.bouncycastle.asn1.pkcs.PKCS12PBEParams;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.Pfx;
import org.bouncycastle.asn1.pkcs.PrivateKeyInfo;
import org.bouncycastle.asn1.pkcs.SafeBag;
import org.bouncycastle.asn1.pkcs.SignedData;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.DigestInfo;
import org.bouncycastle.asn1.x509.SubjectKeyIdentifier;
import org.bouncycastle.asn1.x509.X509CertificateStructure;
import org.bouncycastle.asn1.x509.X509ObjectIdentifiers;
import org.bouncycastle.crypto.BlockCipher;
import org.bouncycastle.crypto.BufferedBlockCipher;
import org.bouncycastle.crypto.CipherParameters;
import org.bouncycastle.crypto.Mac;
import org.bouncycastle.crypto.PBEParametersGenerator;
import org.bouncycastle.crypto.digests.SHA1Digest;
import org.bouncycastle.crypto.engines.DESedeEngine;
import org.bouncycastle.crypto.engines.RC2Engine;
import org.bouncycastle.crypto.generators.PKCS12ParametersGenerator;
import org.bouncycastle.crypto.macs.HMac;
import org.bouncycastle.crypto.modes.CBCBlockCipher;
import org.bouncycastle.crypto.paddings.PaddedBufferedBlockCipher;
import org.bouncycastle.crypto.params.DESParameters;
import org.bouncycastle.crypto.params.KeyParameter;
import org.bouncycastle.crypto.params.ParametersWithIV;
import org.bouncycastle.crypto.params.ParametersWithRandom;

final public class PKCS12 implements PKCSObjectIdentifiers, X509ObjectIdentifiers
{
	private static final int SALT_SIZE = 20;
	private static final int MIN_ITERATIONS = 100;

	//
	// SHA-1 and 3-key-triple DES.
	//
	private static final String KEY_ALGORITHM = "1.2.840.113549.1.12.1.3";

	//
	// SHA-1 and 40 bit RC2.
	//
	private static final String CERT_ALGORITHM = "1.2.840.113549.1.12.1.6";

	//
	// SHA-1 HMAC
	//
	// private static final String MAC_ALGORITHM = "1.3.14.3.2.26";

	protected SecureRandom random = new SecureRandom();

	private String alias;
	private PrivateKey m_privKey;

//	private PublicKey pubKey;
	private X509CertificateStructure x509cert;

	public PKCS12(
		String al,
		PrivateKey privkey,
		X509CertificateStructure cert
		/*,PublicKey pubkey*/
		)
	{
		alias = al;
		m_privKey = privkey;
		//pubKey = pubkey;
		x509cert = cert;
	}

	private SubjectKeyIdentifier createSubjectKeyId()
	{
		try
		{
			return new SubjectKeyIdentifier(x509cert.getSubjectPublicKeyInfo());
			/*			ByteArrayInputStream bIn =
			 new ByteArrayInputStream(pubKey.getEncoded());
			   SubjectPublicKeyInfo info =
			 new SubjectPublicKeyInfo(
			 (ASN1Sequence)new DERInputStream(bIn).readObject());
			   return new SubjectKeyIdentifier(info);*/
		}
		catch (Exception e)
		{
			System.out.println(e.toString());
			e.printStackTrace();
			throw new RuntimeException("error creating key");
		}
	}

	static public byte[] codeData(
		boolean encrypt,
		byte[] data,
		PKCS12PBEParams pbeParams,
		char[] password,
		BlockCipher cipher,
		int keySize) throws IOException
	{
		byte[] my_out;

		try
		{
			BufferedBlockCipher my_cipher = new PaddedBufferedBlockCipher(new CBCBlockCipher(cipher));
			CipherParameters my_param = makePBEParameters(password,
				pbeParams,
				my_cipher.getUnderlyingCipher().getAlgorithmName(),
				keySize,
				64);

			my_param = new ParametersWithRandom(my_param, new SecureRandom());
			my_cipher.init(encrypt, my_param);

			byte[] my_input = data;
			int my_inputlen = my_input.length;
			int my_len = 0;
			byte[] my_tmp = new byte[my_cipher.getOutputSize(my_inputlen)];

			if (my_inputlen != 0)
			{
				my_len = my_cipher.processBytes(my_input, 0, my_inputlen, my_tmp, 0);

			}
			try
			{
				my_len += my_cipher.doFinal(my_tmp, my_len);
			}
			catch (Exception e)
			{
			}

			my_out = new byte[my_len];
			System.arraycopy(my_tmp, 0, my_out, 0, my_len);
		}
		catch (Exception e)
		{
			throw new IOException(
				"exception encrypting data - " + e.toString());
		}

		return my_out;
	}

	public void store(OutputStream stream, char[] password) throws IOException
	{
		ByteArrayOutputStream bOut = new ByteArrayOutputStream();
		DEROutputStream dOut;

		//
		// handle the key
		//
		BERConstructedOctetString keyString;
		{

			byte[] kSalt = new byte[SALT_SIZE];
			random.nextBytes(kSalt);
			PKCS12PBEParams kParams = new PKCS12PBEParams(kSalt, MIN_ITERATIONS);
			byte[] kBytes = codeData(true,
									 m_privKey.getEncoded(),
									 kParams,
									 password,
									 new DESedeEngine(),
									 192);
			AlgorithmIdentifier kAlgId = new AlgorithmIdentifier(
				new DERObjectIdentifier(KEY_ALGORITHM),
				kParams.getDERObject());
			EncryptedPrivateKeyInfo kInfo = new EncryptedPrivateKeyInfo(kAlgId, kBytes);
			DERConstructedSet kName = new DERConstructedSet();

			//
			// set a default friendly name (from the key id) and local id
			//
			DEREncodableVector kSeq = new DEREncodableVector();
			kSeq.add(pkcs_9_at_localKeyId);
			kSeq.add(new DERSet(createSubjectKeyId()));
			kName.addObject(new DERSequence(kSeq));
			kSeq = new DEREncodableVector();
			kSeq.add(pkcs_9_at_friendlyName);
			kSeq.add(new DERSet(new DERBMPString(alias)));
			kName.addObject(new DERSequence(kSeq));

			keyString = new BERConstructedOctetString(new DERSequence(
				new SafeBag(pkcs8ShroudedKeyBag, kInfo.getDERObject(), kName)));
		}

		//
		// certficate processing
		//
		EncryptedData cInfo;
		{
			byte[] cSalt = new byte[SALT_SIZE];
			random.nextBytes(cSalt);
			PKCS12PBEParams cParams = new PKCS12PBEParams(cSalt, MIN_ITERATIONS);
			AlgorithmIdentifier cAlgId = new AlgorithmIdentifier(new DERObjectIdentifier(CERT_ALGORITHM),
				cParams);
			CertBag cBag = new CertBag(x509certType, new DEROctetString(x509cert));
			DERConstructedSet fName = new DERConstructedSet();
			DEREncodableVector fSeq = new DEREncodableVector();
			fSeq.add(pkcs_9_at_localKeyId);
			fSeq.add(new DERSet(createSubjectKeyId()));
			fName.addObject(new DERSequence(fSeq));

			fSeq = new DEREncodableVector();
			fSeq.add(pkcs_9_at_friendlyName);
			fSeq.add(new DERSet(new DERBMPString(alias)));
			fName.addObject(new DERSequence(fSeq));

			SafeBag sBag = new SafeBag(certBag, cBag.getDERObject(), fName);

			bOut.reset();
			dOut = new DEROutputStream(bOut);
			dOut.writeObject(new DERSequence(sBag));
			dOut.close();

			byte[] certBytes = codeData(true, bOut.toByteArray(), cParams, password, new RC2Engine(), 40);
			cInfo = new EncryptedData(data, cAlgId, new BERConstructedOctetString(certBytes));
		}

		ContentInfo[] c = new ContentInfo[2];
		c[0] = new ContentInfo(data, keyString);
		c[1] = new ContentInfo(encryptedData, cInfo);

		/*
		 bOut.reset();
		 AuthenticatedSafe auth = new AuthenticatedSafe(c);
		 BEROutputStream berOut = new BEROutputStream(bOut);
		 berOut.writeObject(auth);
		 byte[] pkg = bOut.toByteArray();

		 ContentInfo mainInfo =
		   new ContentInfo(data, new BERConstructedOctetString(pkg));
		 */
		ContentInfo mainInfo = new ContentInfo(data, new BERConstructedOctetString(new AuthenticatedSafe(c)));
		//
		// create the mac
		//
		byte[] mSalt = new byte[SALT_SIZE];
		int itCount = MIN_ITERATIONS;
		random.nextBytes(mSalt);
		byte[] contentData = ( (DEROctetString) mainInfo.getContent()).getOctets();
		MacData mData = null;

		try
		{
			Mac certMac = new HMac(new SHA1Digest());
			CipherParameters cParam = makePBEMacParameters(password, new PKCS12PBEParams(mSalt, itCount), 160);
			certMac.init(cParam);
			certMac.update(contentData, 0, contentData.length);
			byte[] my_res = new byte[certMac.getMacSize()];
			certMac.doFinal(my_res, 0);
			AlgorithmIdentifier my_algId = new AlgorithmIdentifier(id_SHA1, null);
			DigestInfo my_dInfo = new DigestInfo(my_algId, my_res);

			mData = new MacData(my_dInfo, mSalt, itCount);
		}
		catch (Exception e)
		{
			throw new IOException("error constructing MAC: " + e.toString());
		}

		//
		// output the Pfx
		//
		Pfx pfx = new Pfx(mainInfo, mData);
		BEROutputStream berOut = new BEROutputStream(stream);
		berOut.writeObject(pfx);
	}

	public static final class IllegalCertificateException extends RuntimeException
	{
		public IllegalCertificateException(String str)
		{
			super(str);
		}
	};

	private static class MyCipher
	{
		public BlockCipher cipher;
		public int keysize;
		MyCipher(BlockCipher c, int ks)
		{
			cipher = c;
			keysize = ks;
		}
	};

	private static MyCipher getCipher(String algId)
	{
		if (algId.equals("1.2.840.113549.1.12.1.3"))
		{

			// PBE with SHA and 3-Key TripleDES-CBC
			return new MyCipher(new DESedeEngine(), 192);
		}
		else if (algId.equals("1.2.840.113549.1.12.1.4"))
		{

			// PBE with SHA and 2-Key TripleDES-CBC
			return new MyCipher(new DESedeEngine(), 128);
		}
		else if (algId.equals("1.2.840.113549.1.12.1.5"))
		{

			// PBE with SHA and 128 Bit-RC2-CBC
			return new MyCipher(new RC2Engine(), 128);
		}
		else if (algId.equals("1.2.840.113549.1.12.1.6"))
		{

			// PBE with SHA and 40 Bit-RC2-CBC
			return new MyCipher(new RC2Engine(), 40);
		}
		else
		{
			return null;
		}
	}

	public static PKCS12 load(InputStream stream, char[] password) throws IOException, InvalidKeyException
	{
		String alias = null;
		PrivateKey privKey = null;
		PublicKey pubKey = null;
		X509CertificateStructure x509cert = null;

		BERInputStream is = new BERInputStream(stream);
		ASN1Sequence dcs = (ASN1Sequence) is.readObject();
		Pfx pfx = new Pfx(dcs);
		ContentInfo cinfo = pfx.getAuthSafe();
		// TODO: Check MAC
		// Look at JDKPKCS12KeyStore.engineLoad (starting with bag.getMacData())

		if (!cinfo.getContentType().equals(PKCSObjectIdentifiers.data))
		{
			throw (new IllegalCertificateException("No certificates found."));
		}

		is = new BERInputStream(new ByteArrayInputStream( ( (DEROctetString) cinfo.getContent()).getOctets()));
		ContentInfo[] cinfos = (new AuthenticatedSafe( (ASN1Sequence) is.readObject())).getContentInfo();

		for (int i = 0; i < cinfos.length; i++)
		{
			ASN1Sequence cseq;
			if (cinfos[i].getContentType().equals(PKCSObjectIdentifiers.data))
			{
				DERInputStream dis = new DERInputStream(new ByteArrayInputStream(
					( (DEROctetString) cinfos[i].getContent())
					.getOctets()));
				cseq = (ASN1Sequence) dis.readObject();
			}
			else if (cinfos[i].getContentType().equals(PKCSObjectIdentifiers.encryptedData))
			{
				EncryptedData ed = new EncryptedData( (ASN1Sequence) cinfos[i].getContent());
				String algId = ed.getEncryptionAlgorithm().getObjectId().getId();
				MyCipher cipher = getCipher(algId);
				if (cipher == null)
				{
					throw (new IllegalCertificateException("Encryption Algorithm '" + algId
						+ "' is currently not supported."));
				}
				PKCS12PBEParams pbeParams = new PKCS12PBEParams( (ASN1Sequence) ed.getEncryptionAlgorithm()
					.getParameters());
				BERInputStream bis = new BERInputStream(new ByteArrayInputStream(
					PKCS12.codeData(false, ed.getContent().getOctets(), pbeParams, password,
									cipher.cipher, cipher.keysize)));
				cseq = (ASN1Sequence) bis.readObject();
			}
			else
			{
				continue;
			}

			for (int j = 0; j < cseq.size(); j++)
			{
				SafeBag sb = new SafeBag( (ASN1Sequence) cseq.getObjectAt(j));

				if (sb.getBagId().equals(PKCSObjectIdentifiers.certBag))
				{
					DERInputStream cin = new BERInputStream(
						new ByteArrayInputStream( ( (DEROctetString)new CertBag( (ASN1Sequence)
						sb.getBagValue()).getCertValue()).getOctets()));

					ASN1Sequence xseq = (ASN1Sequence) cin.readObject();
					if (xseq.size() > 1
						&& xseq.getObjectAt(1) instanceof DERObjectIdentifier
						&& xseq.getObjectAt(0).equals(PKCSObjectIdentifiers.signedData))
					{
						x509cert = X509CertificateStructure.getInstance(
							new SignedData(
							ASN1Sequence.getInstance(
							(ASN1TaggedObject) xseq.getObjectAt(1),
							true))
							.getCertificates()
							.getObjectAt(0));
					}
					else
					{
						x509cert = X509CertificateStructure.getInstance(xseq);
					}
				}
				else if (sb.getBagId().equals(PKCSObjectIdentifiers.pkcs8ShroudedKeyBag))
				{
					EncryptedPrivateKeyInfo ePrivKey = new EncryptedPrivateKeyInfo( (ASN1Sequence) sb.
						getBagValue());
					MyCipher cipher = getCipher(ePrivKey.getEncryptionAlgorithm().getObjectId().getId());
					if (cipher == null)
					{
						throw (new IllegalCertificateException(
							"Encryption Algorithm '"
							+ ePrivKey.getEncryptionAlgorithm().getObjectId().getId()
							+ "' is currently not supported."));
					}
					PKCS12PBEParams pbeParams = new PKCS12PBEParams( (ASN1Sequence) ePrivKey
						.getEncryptionAlgorithm().getParameters());
					byte[] pkData = PKCS12.codeData(false, ePrivKey.getEncryptedData(),
						pbeParams, password, cipher.cipher, cipher.keysize);
					ByteArrayInputStream bIn = new ByteArrayInputStream(pkData);
					DERInputStream dIn = new DERInputStream(bIn);
					PrivateKeyInfo privKeyInfo = new PrivateKeyInfo( (ASN1Sequence) dIn.readObject());
					/**@todo check for Alg --> now done by try catch */
					try
					{
						privKey = new MyDSAPrivateKey(privKeyInfo);

					}
					catch (Exception e)
					{
						try
						{
							privKey = new MyRSAPrivateKey(privKeyInfo);
						}
						catch (Exception e1)
						{
							throw new InvalidKeyException("Key currentlic not supported");
						}
					}
				}

				if (alias == null && sb.getBagAttributes() != null)
				{
					Enumeration e = sb.getBagAttributes().getObjects();
					while (e.hasMoreElements())
					{
						ASN1Sequence ba = (ASN1Sequence) e.nextElement();
						DERObjectIdentifier oid = (DERObjectIdentifier) ba.getObjectAt(0);
						DERObject att = (DERObject) ( (ASN1Set) ba.getObjectAt(1)).getObjectAt(0);
						if (oid.equals(pkcs_9_at_friendlyName))
						{
							alias = ( (DERBMPString) att).getString();
						}
					}
				}
			}
		}
		if (alias != null && x509cert != null) //????
		{
			return new PKCS12(alias, privKey, x509cert);
		}
		return null;
	}

	static private PBEParametersGenerator makePBEGenerator()
	{
		return new PKCS12ParametersGenerator(new SHA1Digest());
	}

	static CipherParameters makePBEMacParameters(char[] password, PKCS12PBEParams pbeParams, int keySize)
	{
		PBEParametersGenerator generator = makePBEGenerator();
		byte[] key = PBEParametersGenerator.PKCS12PasswordToBytes(password);
		CipherParameters param;

		generator.init(key, pbeParams.getIV(), pbeParams.getIterations().intValue());

		param = generator.generateDerivedMacParameters(keySize);

		for (int i = 0; i != key.length; i++)
		{
			key[i] = 0;
		}

		return param;
	}

	static CipherParameters makePBEParameters(char[] password, PKCS12PBEParams pbeParams,
											  String targetAlgorithm,
											  int keySize, int ivSize)
	{
		PBEParametersGenerator generator = makePBEGenerator();
		byte[] key = PBEParametersGenerator.PKCS12PasswordToBytes(password);
		CipherParameters param;

		generator.init(key, pbeParams.getIV(), pbeParams.getIterations().intValue());

		if (ivSize != 0)
		{
			param = generator.generateDerivedParameters(keySize, ivSize);
		}
		else
		{
			param = generator.generateDerivedParameters(keySize);
		}

		if (targetAlgorithm.startsWith("DES"))
		{
			if (param instanceof ParametersWithIV)
			{
				KeyParameter kParam = (KeyParameter) ( (ParametersWithIV) param).getParameters();
				DESParameters.setOddParity(kParam.getKey());
			}
			else
			{
				KeyParameter kParam = (KeyParameter) param;
				DESParameters.setOddParity(kParam.getKey());
			}
		}

		for (int i = 0; i != key.length; i++)
		{
			key[i] = 0;
		}

		return param;
	}

	public String getAlias()
	{
		return alias;
	}

	public PrivateKey getPrivKey()
	{
		return m_privKey;
	}

	/*	public PublicKey getPubKey()
	 {
	  return pubKey;
	 }
	 */
	public X509CertificateStructure getX509cert()
	{
		return x509cert;
	}

	public void setAlias(String string)
	{
		alias = string;
	}

	public void setPrivKey(DSAPrivateKey key)
	{
		m_privKey = key;
	}

	/*	public void setPubKey(PublicKey key)
	 {
	  pubKey = key;
	 }
	 */
	public void setX509cert(X509CertificateStructure structure)
	{
		x509cert = structure;
	}

}
