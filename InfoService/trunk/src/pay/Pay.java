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
package pay;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Enumeration;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.ASN1TaggedObject;
import org.bouncycastle.asn1.BERInputStream;
import org.bouncycastle.asn1.DERInputStream;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.SignedData;
import org.bouncycastle.asn1.x509.X509CertificateStructure;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.generators.RSAKeyPairGenerator;
import org.bouncycastle.crypto.params.RSAKeyGenerationParameters;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters;
import anon.crypto.JAPCertificate;
import anon.crypto.JAPSignature;
import anon.crypto.MyRSAPrivateKey;
import anon.crypto.MyRSAPublicKey;
import anon.util.Base64;
import jap.JAPModel;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import payxml.XMLAccountCertificate;
import payxml.XMLAccountInfo;
import payxml.XMLJapPublicKey;
import payxml.XMLTransCert;
import org.bouncycastle.crypto.generators.DSAKeyPairGenerator;
import org.bouncycastle.crypto.params.DSAKeyGenerationParameters;
import org.bouncycastle.crypto.params.DSAParameters;
import org.bouncycastle.crypto.generators.DSAParametersGenerator;
import anon.crypto.MyDSAPublicKey;
import anon.crypto.MyDSAPrivateKey;
import org.bouncycastle.crypto.params.DSAPublicKeyParameters;
import org.bouncycastle.crypto.params.DSAPrivateKeyParameters;
import java.security.PublicKey;
import java.security.PrivateKey;
import anon.crypto.IMyPublicKey;
import anon.crypto.IMyPrivateKey;
import anon.util.XMLUtil;
import jap.JAPUtil;
import jap.JAPConstants;
import java.security.*;

/**
 * This class is the high-level part of the communication with the BI.
 * It contains functions for creating accounts, charging, etc.
 *
 * @author Andreas Mueller, Grischan Glaenzel, Bastian Voigt
 * @todo switch SSL on when everything works
 */

// PS: Das diese Klasse mittlerweile schon 3 Autoren hat, spricht fuer sich :)

public class Pay
{
	/** the one and only payoff */
	private static Pay ms_Pay;

	/** the accounts file, an object that holds accounts configuration data */
	private PayAccountsFile m_AccountsFile;

	/** for checking the JPI signatures */
	private JAPSignature m_verifyingInstance;

	/** Returns the verifying instance for checking the JPI signatures */
	public JAPSignature getVerifyingInstance()
	{
		return m_verifyingInstance;
	}

	/**
	 * make default constructor private: singleton
	 */
	private Pay()
	{
		// read JPI Certificate from resource file
		byte[] barCert = JAPUtil.loadRessource(JAPConstants.CERTSPATH + JAPConstants.CERT_BI);
		JAPCertificate cert = JAPCertificate.getInstance(barCert);
		m_verifyingInstance = new JAPSignature();
		try
		{
			m_verifyingInstance.initVerify(cert.getPublicKey());
		}
		catch (InvalidKeyException ex)
		{
			LogHolder.log(LogLevel.ERR, LogType.PAY, "Could not load BI Certificate");
			m_verifyingInstance = null;
		}

		// read PayAccountsFile from disk
		LogHolder.log(LogLevel.DEBUG, LogType.PAY, "Pay(): Calling PayAccountsFile...");
		m_AccountsFile = PayAccountsFile.getInstance();
	}

	/**
	 * Returns the one and only Pay
	 */
	public static Pay getInstance()
	{
		if (ms_Pay == null)
		{
			ms_Pay = new Pay();
		}
		return ms_Pay;
	}

	/**
	 * Reads the JPI's X509 certificate from a file
	 * and initializes the signature verifying instance
	 *
	 * @author Bastian Voigt
	 */
	/*	public void readJpiCertificate(String fileName)
	 {
	  // the following code was copied
	  // from MixConfig.java ( openFile() and readCertificate() )
	  // maybe not a too good solution...
	  // TODO: remove this copied code
	  byte[] cert;
	  java.io.File file = new java.io.File(fileName);
	  try
	  {
	   cert = new byte[ (int) file.length()];
	   java.io.FileInputStream fin = new java.io.FileInputStream(file);
	   fin.read(cert);
	   fin.close();
	  }
	  catch (Exception e)
	  {
	   LogHolder.log(LogLevel.ALERT, LogType.PAY,
			"Pay.readJpiCertificate(): Error reading certificate file " + fileName
			);
	   e.printStackTrace();
	   return;
	  }

	  java.io.ByteArrayInputStream bin = null;
	  X509CertificateStructure x509 = null;

	  try
	  {
	   // start readCertificate(cert[])

	   if (cert[0] != (DERInputStream.SEQUENCE | DERInputStream.CONSTRUCTED))
	   {
		// Probably a Base64 encoded certificate
		java.io.BufferedReader in =
		 new java.io.BufferedReader(
		 new java.io.InputStreamReader(new java.io.ByteArrayInputStream(cert)));
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
		bin = new java.io.ByteArrayInputStream(Base64.decode(sbuf.toString()));
	   }

	   if (bin == null && cert[1] == 0x80)
	   {
		// a BER encoded certificate
		BERInputStream in =
		 new BERInputStream(new java.io.ByteArrayInputStream(cert));
		ASN1Sequence seq = (ASN1Sequence) in.readObject();
		DERObjectIdentifier oid = (DERObjectIdentifier) seq.getObjectAt(0);
		if (oid.equals(PKCSObjectIdentifiers.signedData))
		{
		 x509 = new X509CertificateStructure(
		  (ASN1Sequence)new SignedData(
		  (ASN1Sequence) ( (DERTaggedObject) seq
			  .getObjectAt(1))
		  .getObject())
		  .getCertificates()
		  .getObjectAt(0));
		}
	   }
	   else
	   {
		if (bin == null)
		{
		 bin = new java.io.ByteArrayInputStream(cert);
		 // DERInputStream
		}
		DERInputStream in = new DERInputStream(bin);
		ASN1Sequence seq = (ASN1Sequence) in.readObject();
		if (seq.size() > 1
		 && seq.getObjectAt(1) instanceof DERObjectIdentifier
		 && seq.getObjectAt(0).equals(PKCSObjectIdentifiers.signedData))
		{
		 x509 = X509CertificateStructure.getInstance(
		  new SignedData(
		  ASN1Sequence.getInstance(
		  (ASN1TaggedObject) seq.getObjectAt(1),
		  true))
		  .getCertificates()
		  .getObjectAt(0));
		}
		else
		{
		 x509 = X509CertificateStructure.getInstance(seq);
		}
	   }
	  }
	  catch (Exception e)
	  {
	   e.printStackTrace();
	   x509 = null;
	  }

	  if (x509 == null)
	  {
	   LogHolder.log(LogLevel.ALERT, LogType.PAY,
			"Pay.readJpiCertificate(): Error decoding certificate!"
			);
	   return;
	  }

	  try
	  {
	   LogHolder.log(LogLevel.DEBUG, LogType.PAY,
			"Pay.readJpiCertificate(): reading JPI Certificate from file '" + fileName + "'."
			);
	   JAPCertificate japCert = JAPCertificate.getInstance(x509);
	   m_verifyingInstance = new JAPSignature();
	   m_verifyingInstance.initVerify(japCert.getPublicKey());
	  }
	  catch (Exception e)
	  {
	   e.printStackTrace();
	  }
	 }*/

	/**
	 * Request a transfer certificate from the BI
	 *
	 * @param accountNumber account number
	 * @return xml transfer certificate
	 * @throws Exception
	 */
	public XMLTransCert chargeAccount(long accountNumber) throws Exception
	{
		PayAccount account = m_AccountsFile.getAccount(accountNumber);
		if (account == null)
		{
			throw new Exception("Invalid Account Number: " + accountNumber);
		}

		// TODO: switch SSL on when everything works
		BIConnection biConn = new BIConnection(
			JAPModel.getBIHost(), JAPModel.getBIPort(),
			false // ssl off
			);
		biConn.connect();
		biConn.authenticate(account.getAccountCertificate(), account.getSigningInstance());
		XMLTransCert transcert = biConn.charge();
		biConn.disconnect();
		account.addTransCert(transcert);
//		m_AccountsFile.save();
		return transcert;
	}

	/**
	 * Fetches AccountInfo XML structure for each account in the accountsFile.
	 * @todo do not connect/disconnect everytime
	 */
	public void fetchAccountInfoForAllAccounts() throws Exception
	{
		Enumeration accounts = m_AccountsFile.getAccounts();
		while (accounts.hasMoreElements())
		{
			PayAccount ac = (PayAccount) accounts.nextElement();
			ms_Pay.fetchAccountInfo(ac.getAccountNumber());
		}
	}

	/**
	 * Requests an AccountInfo XML structure from the BI.
	 *
	 * @param accountNumber long
	 * @throws IllegalStateException
	 * @return XMLAccountInfo
	 */
	public XMLAccountInfo fetchAccountInfo(long accountNumber) throws Exception
	{
		XMLAccountInfo info;
		PayAccount account = m_AccountsFile.getAccount(accountNumber);

		// TODO: Switch SSL on when everything works
		BIConnection biConn = new BIConnection(JAPModel.getBIHost(),
											   JAPModel.getBIPort(),
											   false
											   /* ssl off! */
											   );
		biConn.connect();
		biConn.authenticate(account.getAccountCertificate(), account.getSigningInstance());
		info = biConn.getAccountInfo();
		biConn.disconnect();

		// save in the account object
		account.setAccountInfo(info);

		// save the modified accountsfile
//			m_AccountsFile.save();
		return info;
	}

	/**
	 * Creates a new Account.
	 * Generates an RSA or DSA key pair and then registers a new account with the BI.
	 * This can take a while, so the user should be notified before calling this.
	 *
	 * At the moment, only DSA should be used, because RSA is not supported by the
	 * AI implementation
	 *
	 */
	public void createAccount(boolean useDSA) throws Exception
	{
		IMyPublicKey pubKey = null;
		IMyPrivateKey privKey = null;

		// generate key pair
		// TODO: show confirmation dialog before generateing key
		if (useDSA)
		{
			SecureRandom random = new SecureRandom();
			DSAParametersGenerator pGen = new DSAParametersGenerator();
			DSAKeyPairGenerator kpGen = new DSAKeyPairGenerator();
			pGen.init(1024, 20, random);
			kpGen.init(new DSAKeyGenerationParameters(random, pGen.generateParameters()));
			AsymmetricCipherKeyPair ackp = kpGen.generateKeyPair();
			pubKey = new MyDSAPublicKey( (DSAPublicKeyParameters) ackp.getPublic());
			privKey = new MyDSAPrivateKey( (DSAPrivateKeyParameters) ackp.getPrivate());
		}
		else // use RSA (should not be used at the moment)
		{
			//TODO: check RSAKeyGeneration
			RSAKeyPairGenerator pGen = new RSAKeyPairGenerator();
			RSAKeyGenerationParameters genParam = new RSAKeyGenerationParameters(
				BigInteger.valueOf(0x11), new SecureRandom(), 512, 25);
			pGen.init(genParam);
			AsymmetricCipherKeyPair pair = pGen.generateKeyPair();
			privKey = new MyRSAPrivateKey( (RSAPrivateCrtKeyParameters) pair.getPrivate());
			pubKey = new MyRSAPublicKey( (RSAKeyParameters) pair.getPublic());
		}

		JAPSignature signingInstance = new JAPSignature();
		signingInstance.initSign(privKey);
		XMLJapPublicKey xmlKey = new XMLJapPublicKey(pubKey);

		// send it to the JPI TODO: switch SSL on
		BIConnection biConn = new BIConnection(JAPModel.getBIHost(),
											   JAPModel.getBIPort(),
											   false
											   /* ssl off! */
											   );
		biConn.connect();
		XMLAccountCertificate cert = biConn.register(xmlKey, signingInstance);
		biConn.disconnect();

		// add the new account to the accountsFile
		PayAccount newAccount = new PayAccount(cert, privKey, signingInstance);
		m_AccountsFile.addAccount(newAccount);
	}
}
