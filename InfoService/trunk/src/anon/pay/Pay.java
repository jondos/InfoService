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
package anon.pay;

import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Enumeration;
import java.util.Vector;

import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.generators.DSAKeyPairGenerator;
import org.bouncycastle.crypto.generators.DSAParametersGenerator;
import org.bouncycastle.crypto.generators.RSAKeyPairGenerator;
import org.bouncycastle.crypto.params.DSAKeyGenerationParameters;
import org.bouncycastle.crypto.params.DSAPrivateKeyParameters;
import org.bouncycastle.crypto.params.DSAPublicKeyParameters;
import org.bouncycastle.crypto.params.RSAKeyGenerationParameters;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters;
import org.w3c.dom.Element;
import anon.crypto.IMyPrivateKey;
import anon.crypto.IMyPublicKey;
import anon.crypto.JAPSignature;
import anon.crypto.MyDSAPrivateKey;
import anon.crypto.MyDSAPublicKey;
import anon.crypto.MyRSAPrivateKey;
import anon.crypto.MyRSAPublicKey;
import anon.pay.xml.XMLAccountCertificate;
import anon.pay.xml.XMLAccountInfo;
import anon.pay.xml.XMLJapPublicKey;
import anon.pay.xml.XMLTransCert;

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
	/** the one and only pay */
	private static Pay ms_Pay = null;

	/** the accounts file, an object that holds accounts configuration data */
	private PayAccountsFile m_AccountsFile;

	/** the known BIs */
	private Vector m_KnownBIs;

	/**
	 * make default constructor private: singleton
	 * @param thBI BI
	 * @param accountsData Element the xml account configuration.
	 */
	private Pay(BI theBI, Element accountsData) throws
		Exception
	{
		m_KnownBIs = new Vector();
		m_KnownBIs.addElement(theBI);

		PayAccountsFile.init(accountsData);
		m_AccountsFile = PayAccountsFile.getInstance();
	}

	/**
	 * Initializes the payment functionality.
	 * @param biCert JAPCertificate the BI's certificate
	 */
	public static void init(BI theBI, Element accountsData) throws
		Exception
	{
		ms_Pay = new Pay(theBI, accountsData);
	}

	/**
	 * Returns the one and only Pay
	 */
	public static Pay getInstance()
	{
		return ms_Pay;
	}

	/**
	 * Request a transfer certificate from the BI
	 *
	 * @param accountNumber account number
	 * @return xml transfer certificate
	 * @throws Exception
	 * @todo switch SSL on
	 */
	public XMLTransCert chargeAccount(long accountNumber) throws Exception
	{
		PayAccount account = m_AccountsFile.getAccount(accountNumber);
		if (account == null)
		{
			throw new Exception("Invalid Account Number: " + accountNumber);
		}
		BIConnection biConn = new BIConnection( (BI) m_KnownBIs.get(0), false /* ssl off*/);
		biConn.connect();
		biConn.authenticate(account.getAccountCertificate(), account.getSigningInstance());
		XMLTransCert transcert = biConn.charge();
		biConn.disconnect();
		account.addTransCert(transcert);
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
	 * @todo switch SSL on
	 */
	public XMLAccountInfo fetchAccountInfo(long accountNumber) throws Exception
	{
		XMLAccountInfo info;
		PayAccount account = m_AccountsFile.getAccount(accountNumber);

		BIConnection biConn = new BIConnection( (BI) m_KnownBIs.get(0),
											   false
											   /* ssl off! */
											   );
		biConn.connect();
		biConn.authenticate(account.getAccountCertificate(), account.getSigningInstance());
		info = biConn.getAccountInfo();
		biConn.disconnect();

		// save in the account object
		account.setAccountInfo(info);
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
	 * @todo check RSA keygen implementation, switch SSL on
	 */
	public void createAccount(boolean useDSA) throws Exception
	{
		IMyPublicKey pubKey = null;
		IMyPrivateKey privKey = null;

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

		BIConnection biConn = new BIConnection( (BI) m_KnownBIs.get(0),
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
