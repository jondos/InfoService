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

import java.io.IOException;
import java.math.BigInteger;
import java.security.SecureRandom;
import java.util.Enumeration;
import java.util.Vector;
import org.bouncycastle.crypto.AsymmetricCipherKeyPair;
import org.bouncycastle.crypto.generators.RSAKeyPairGenerator;
import org.bouncycastle.crypto.params.RSAKeyGenerationParameters;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters;
import org.bouncycastle.asn1.x509.X509CertificateStructure;
import org.bouncycastle.asn1.BERInputStream;
import org.bouncycastle.asn1.DERInputStream;
import org.bouncycastle.asn1.ASN1Sequence;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.pkcs.SignedData;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DERTaggedObject;
import org.bouncycastle.asn1.ASN1TaggedObject;

import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import pay.event.ModelEventFirerer;
import pay.event.ModelListener;
import payxml.XMLAccountCertificate;
import payxml.XMLAccountInfo;
import payxml.XMLEasyCC;
import payxml.XMLTransCert;

import anon.crypto.JAPSignature;
import anon.crypto.JAPCertificate;
import anon.crypto.*;

import anon.util.Base64;
import payxml.XMLJapPublicKey;

/**
 * Dies ist die Hauptklasse f\uFFFDr die Payfunktionalti\uFFFDt,
 * hier werden Funktionen zum kommunizieren mit der BI,
 * und dem Speichern der Konten angeboten.
 * Pay ist ausserdem ein ModelListener des Pay Bereiches:
 * sobald sich etwas an Pay oder den von Pay verwalteten Klassen \uFFFDndert,
 * wird ein ModelEvent geworfen.
 * Auf PayAccountFile und PayInstance und PayAccountsControl sollte aus diesem
 * Grund nur \uFFFDber Pay zugegriffen werden.
 *
 * @author Andreas Mueller, Grischan Glaenzel, Bastian Voigt
 */

// PS: Das diese Klasse mittlerweile schon 3 Autoren hat, spricht f\uFFFDr sich :)

public class Pay
{
	private static Pay pay;

	//	private UserDaten user;

//	private BIConnection payInstance;
	private PayAccountFile m_accountFile;
	private PayAccountsControl m_accountsControl;
	private String host;
	private int port;
	private boolean sslOn;
	private String filename;
	private boolean connectedWithPi = false;
	private boolean accountFileOpened = false;

	/** for checking the JPI signatures */
	private JAPSignature m_verifyingInstance;
	public JAPSignature getVerifyingInstance()
	{
		return m_verifyingInstance;
	}

	private ModelEventFirerer fire = new ModelEventFirerer(this);

	/**
	 * make default constructor private: singleton
	 */
	private Pay()
	{
		m_accountsControl = new PayAccountsControl();
	}

	/**
	 * Erzeugt eine Instanz der Klasse Pay
	 *
	 * @return Instanz der Klasse Pay
	 */
	public static Pay getInstance()
	{
		if (pay == null)
		{
			pay = new Pay();
		}
		return pay;
	}

	/**
	 * Gucken ob die aufrufe von {@link #setPayInstance} und
	 * {@link #openAccountFile} erfolgreich absolviert wurden
	 *
	 * @return true wenn initialisierung erfolgreich verlaufen ist
	 */

	public boolean initDone()
	{
		return (connectedWithPi && accountFileOpened);
	}

	public boolean connectedWithPi()
	{
		return connectedWithPi;
	}

	public boolean accountFileOpened()
	{
		return accountFileOpened;
	}

	/**
	 * Initialisiert die Pay Klasse. Ruft {@link #setPayInstance} und
	 * {@link #openAccountFile}
	 * auf. Die Kontendatei wird eingelesen und entschl\uFFFDsselt. Hostname und
	 * Hostport der Bezahlinstanz werden gesetzt.
	 *
	 * @param filename Dateiname der Kontendatei
	 * @param password Passwort zur Ver- bzw. Entschl\uFFFDsselung der Kontendatei
	 * @param host Hostname der Bezahlinstanz
	 * @param port Hostport der Bezahlinstanz
	 * @param sslOn schaltet SSL ein oder aus (Debug-stuff)
	 * @param jpiCertFileName Name der Datei, die das Public
	 * X509 Certificate der Bezahlinstanz enth\uFFFDlt
	 * @return -1 wenn noch keine Kontendatei unter filename existiert, 0 sonst
	 * @throws Exception Wenn ein Fehler beim Dateizugriff oder der
	 * Entschl\uFFFDsselung auftrat
	 *
	 * NOTE: THIS IS NEVER CALLED. REMOVE???
	 */
	/* public void init(String host, int port, boolean sslOn, String jpiCertFileName)
	  throws Exception
	  /* public void init(String host, int port, boolean sslOn, String jpiCertFileName)
		throws Exception
		{
	   readJpiCertificate(certFileName); // new by Bastian Voigt
	   setPayInstance(host, port, sslOn);
	   openAccountFile();
		}*/

	 /**
	  * Reads the JPI's X509 certificate from a file
	  * and initializes the signature verifying instance
	  *
	  * @author Bastian Voigt
	  */
	 public void readJpiCertificate(String fileName)
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
	  catch (Exception e) {
	   LogHolder.log(LogLevel.ALERT, LogType.PAY,
	  "Pay.readJpiCertificate(): Error reading certificate file " + fileName
	 );
	   return;
	  }

	  java.io.ByteArrayInputStream bin = null;
	  X509CertificateStructure x509 = null;

	  try {
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
	  x509 = X509CertificateStructure.getInstance(seq);
	   }
	  }
	  catch(Exception e) {
	   e.printStackTrace();
	   x509=null;
	  }

	  if(x509 == null) {
	   LogHolder.log(LogLevel.ALERT, LogType.PAY,
	  "Pay.readJpiCertificate(): Error decoding certificate!"
	 );
	   return;
	  }

	  try {
	   LogHolder.log(LogLevel.DEBUG, LogType.PAY,
	  "Pay.readJpiCertificate(): reading JPI Certificate from file '"+fileName+"'."
	 );
	   JAPCertificate japCert = JAPCertificate.getInstance(x509);
	   m_verifyingInstance = new JAPSignature();
	   m_verifyingInstance.initVerify(japCert.getPublicKey());
	  }
	  catch(Exception e) {
	   e.printStackTrace();
	  }
	 }*/

	/**
	 * Hostname und Hostport der Bezahlinstanz werden gesetzt.
	 *
	 * @param host Hostname der Bezahlinstanz
	 * @param port Hostport der Bezahlinstanz
	 * @param sslOn SSL ein / aus (Debug-Stuff)
	 */
	public void setPayInstance(String host, int port, boolean sslOn)
	{
		this.host = host;
		this.port = port;
		this.sslOn = sslOn;
		connectedWithPi = true;
	}

	/**
	 * Einlesen und entschl\uFFFDsseln der Kontendatei.
	 *
	 * @param filename Dateiname
	 * @param password Passwort
	 * @return 0 wenn Kontendatei neu angelegt wurde, 1 wenn bestehende
	 * Kontendatei ge\uFFFDffnet wurde. -1 Wenn ein Fehler beim Dateizugriff oder der
	 * Entschl\uFFFDsselung auftrat. 2 wenn dieses AccountFile bereits ge\uFFFDffnet ist
	 */
	public void openAccountFile() throws Exception
	{
		if ( (m_accountFile == null))
		{
			try
			{
				m_accountFile = m_accountsControl.open();
				accountFileOpened = true;
				fire.fireModelEvent();
			}
			catch (PayAccountsControl.WrongPasswordException wp)
			{
				LogHolder.log(LogLevel.DEBUG, LogType.PAY,
							  "\uFFFDffnen des AccountFiles ging nicht => falsches Passwort angegeben " + wp);
			}
			catch (IOException io)
			{
				LogHolder.log(LogLevel.DEBUG, LogType.PAY,
							  "\uFFFDffnen des AccountFiles ging nicht => allgemiener IOFehler! ");
			}
		}
	}

	public boolean accountFileHasUsedAccount()
	{
		return (accountFileOpened() && m_accountFile.hasUsedAccount());
	}

	public void storeAccountFile()
	{
		try
		{
			m_accountsControl.store(m_accountFile);
		}
		catch (Exception ex)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.PAY, "speichern des Accout FIles ging nicht");
		}
	}

	public void changeAccountFileEncryptMode() throws Exception
	{
		fire.fireModelEvent();
		m_accountsControl.changeEncryptMode(m_accountFile);
	}

	public boolean isAccountFileEncrypted()
	{
		return m_accountFile.getPassword() != null;
	}

	public void exportAccountFile(String filename) throws Exception
	{
		try
		{
			PayAccountFile accounts = new PayAccountFile(m_accountFile.getXML(), m_accountFile.getPassword());
			m_accountsControl.store(accounts, filename);
		}
		catch (IOException io)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.PAY, "exportAccountFile: speichern geht nicht: IOException");
		}
	}

	public void importAccountFile(String filename)
	{
		LogHolder.log(LogLevel.DEBUG, LogType.PAY, "importAccountFile start");
		try
		{
			PayAccountFile accounts = m_accountsControl.open(filename);
			Enumeration en = accounts.getAccounts();

			while (en.hasMoreElements())
			{
				LogHolder.log(LogLevel.DEBUG, LogType.PAY, "en.hasMoreElements()");
				PayAccount account = (PayAccount) en.nextElement();
				if (m_accountFile.getAccount(account.getAccountNumber()) == null)
				{
					try
					{
						m_accountsControl.store(m_accountFile.makeNewAccount(account));
					}
					catch (Exception ex)
					{
						LogHolder.log(LogLevel.DEBUG, LogType.PAY,
									  "account No: " + account.getAccountNumber() + " bereits vorhanden");
					}
				}
			}
		}
		catch (Exception ex)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.PAY, "importAccountFile ging nicht");
		}
		fire.fireModelEvent();
	}

	public boolean changeAccountFilePassword()
	{
		fire.fireModelEvent();
		return m_accountsControl.changePW(m_accountFile);
	}

	/**
	 * Setzt das aktuell zum Bezahlen benutze Konto
	 **/
	public void setUsedAccount(long accountNumber)
	{
		if (m_accountFile == null)
		{
			throw new IllegalStateException("accountfile not set");
		}
		try
		{
			m_accountFile.setUsedAccount(accountNumber);
			m_accountsControl.store(m_accountFile);
			fire.fireModelEvent();
		}
		catch (Exception ex)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.PAY, "Setzen des Benutzten Accounts geht nicht");
		}
	}

	/**
	 * Liefert Das aktuell zum Bezahlen benutze Konto
	 **/
	public long getUsedAccount()
	{
		if (m_accountFile == null)
		{
			throw new IllegalStateException("accountfile not set");
		}
		return m_accountFile.getUsedAccount();
	}

	/**
	 * Liefert alle gespeicherten Konten.
	 *
	 * @return Vector von {@link PayAccount}s
	 * @throws IllegalStateException Wenn die Kontendatei nicht gesetzt ist.
	 */
	public Vector getAccountVec() throws IllegalStateException
	{
		if (m_accountFile == null)
		{
			throw new IllegalStateException("accountfile not set");
		}
		else
		{
			return m_accountFile.getAccountVec();
		}
	}

	/**
	 * Liefert alle gespeicherten Konten.
	 *
	 * @return Enumeration von {@link PayAccount}s
	 * @throws IllegalStateException Wenn die Kontendatei nicht gesetzt ist.
	 */
	public Enumeration getAccounts() throws IllegalStateException
	{
		if (m_accountFile == null)
		{
			throw new IllegalStateException("accountfile not set");
		}
		else
		{
			return m_accountFile.getAccounts();
		}
	}

	/**
	 * Liefert die Kontoinformation des Kontos mit der angegeben Kontonummer.
	 *
	 * @param accountNumber accountnumber
	 * @return Kontodaten
	 * @throws IllegalStateException Wenn die Kontendatei nicht gesetzt ist.
	 */
	public PayAccount getAccount(long accountNumber) throws IllegalStateException
	{
		if (m_accountFile == null)
		{
			throw new IllegalStateException("accountfile not set");
		}
		else
		{
			return m_accountFile.getAccount(accountNumber);
		}
	}

	/**
	 * L\uFFFDscht das Konto mit der angebenen Kontonummer.
	 *
	 * @param accountNumber Kontonummer
	 * @throws IllegalStateException Wenn die Kontendatei nicht gesetzt ist.
	 */
	public boolean deleteAccount(long accountNumber) throws IllegalStateException
	{
		try
		{
			if (!m_accountFile.deleteAccount(accountNumber))
			{
				return false;
			}
			m_accountsControl.store(m_accountFile);
			fire.fireModelEvent();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			return false;
		}
		return true;
	}

	/**
	 * Aufladen des Kontos mit angebener Kontonummer.
	 * Die Bezahlinstanz wird kontaktiert und ein Kontozertifikat angefragt.
	 * Das Kontozertifikat wird den Kontoinformationen hinzugef\uFFFDgt und
	 * gespeichert.
	 *
	 * @param accountNumber Kontonummer
	 * @return \uFFFDberweisungsnummer (Transaktionspeudonym)
	 * @throws IllegalStateException Wenn die Kontendatei oder die
	 * Bezahlinstanz nicht gesetzt sind.
	 */
	public long chargeAccount(long accountNumber) throws IllegalStateException, IOException
	{
		try
		{
			PayAccount account = m_accountFile.getAccount(accountNumber);
			BIConnection biConn = new BIConnection(host, port, sslOn);
			biConn.connect();
			XMLTransCert transcert = biConn.charge();
			biConn.disconnect();
			account.addTransCert(transcert);
			m_accountsControl.store(m_accountFile.modifyAccount(account));
			fire.fireModelEvent();
			return transcert.getTransferNumber();
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.PAY, "Fehler: " + e.getMessage());
			return -1;
		}
	}

	/**
	 * F\uFFFDr jede in der aktuelle KontoDatei gespeichert Konten wird getBalance aufgerufen
	 */

	public void updateAllBalance()
	{
		try
		{
			Enumeration accounts = getAccounts();
			while (accounts.hasMoreElements())
			{
				PayAccount ac = (PayAccount) accounts.nextElement();
				pay.updateBalance(ac.getAccountNumber());
			}
		}
		catch (IllegalStateException ise)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.PAY, "getAllBalance kunktioniert nicht");
		}
	}

	/**
	 * Abfrage des Kontostandes des Kontos mit angebener Kontonummer.
	 * Die Bezahlinstanz wird kontaktiert und der Kontostand angefragt.
	 * Dar \uFFFDbermittelte Kontoschnappschuss und die Kostenbest\uFFFDtigungen werden
	 * den Kontoinformationen hinzugef\uFFFDgt und gespeichert.
	 *
	 * @param accountNumber Kontonummer
	 * @return Kontoschnappschuss und Kostenbest\uFFFDtigungen
	 */
	public XMLAccountInfo updateBalance(long accountNumber) throws IllegalStateException
	{
		try
		{
			XMLAccountInfo info;
			PayAccount account = m_accountFile.getAccount(accountNumber);
			BIConnection biConn = new BIConnection(host, port, sslOn);
			biConn.connect();
			biConn.authenticate(account.getAccountCertificate(), account.getSigningInstance());
			info = biConn.getAccountInfo();
			biConn.disconnect();

			// save in the account object
			account.setAccountInfo(info);

			// save the modified accountsfile
			m_accountsControl.store(m_accountFile.modifyAccount(account));
			fire.fireModelEvent();
			return info;
		}
		catch (Exception e)
		{
			return null;
		}
	}

	public XMLAccountInfo getAccountInfo(long accountNumber) throws IllegalStateException
	{
		try
		{
			PayAccount account = m_accountFile.getAccount(accountNumber);
			return account.getAccountInfo();
		}
		catch (Exception e)
		{
			return null;
		}
	}

	public XMLEasyCC addCosts(String aiName, long accountNumber, long plusCosts) throws IllegalStateException
	{
		try
		{
			PayAccount account = m_accountFile.getAccount(accountNumber);
			XMLEasyCC cc = account.addCostConfirmation(aiName, accountNumber, plusCosts);
// todo: move to account			m_accountsControl.store(m_accountFile.modifyAccount(account));
			return cc;
		}
		catch (Exception e)
		{
			return null;
		}
	}

	/**
	 * Funktion zur Kontoer\uFFFDffnung. Ein RSA-Schl\uFFFDsselpaar wird generiert und
	 * der \uFFFDffentliche Schl\uFFFDssel an die Bezahlinstanz \uFFFDbermittelt. Das
	 * erhaltene Kontozertifikat wird im neuen Kontodatensatz lokal
	 * gespeichert.
	 */
	public void addAccount() throws IllegalStateException, Exception
	{
		LogHolder.log(LogLevel.DEBUG, LogType.PAY,
					  "Pay.addAccount() called. Generating key.."
					  );
		RSAKeyPairGenerator pGen = new RSAKeyPairGenerator();

		// generate Key pair
		//TODO: check RSAKeyGeneration
		// TODO: show confirmation dialog before generateing key
		RSAKeyGenerationParameters genParam = new RSAKeyGenerationParameters(
			BigInteger.valueOf(0x11), new SecureRandom(), 512, 25);
		pGen.init(genParam);
		AsymmetricCipherKeyPair pair = pGen.generateKeyPair();
		MyRSAPrivateKey rsaprivkey = new MyRSAPrivateKey( (RSAPrivateCrtKeyParameters) pair.getPrivate());
		MyRSAPublicKey rsapubkey = new MyRSAPublicKey( (RSAKeyParameters) pair.getPublic());

		JAPSignature signingInstance = new JAPSignature();
		signingInstance.initSign(rsaprivkey);

		XMLJapPublicKey pubKey = new XMLJapPublicKey(rsapubkey);

		LogHolder.log(LogLevel.DEBUG, LogType.PAY,
					  "Pay.addAccount() Key successfully generated"
					  );

		// send it to the JPI
		BIConnection biConn = new BIConnection(host, port, sslOn);

		biConn.connect();
		XMLAccountCertificate xmlCert = biConn.register(pubKey, signingInstance);
		biConn.disconnect();

		PayAccount newAccount = new PayAccount(xmlCert, rsaprivkey, signingInstance);
		m_accountsControl.store(m_accountFile.makeNewAccount(newAccount));
		fire.fireModelEvent();
	}

	/**
	 * Funktion zum hinzuf\uFFFDgen eines ModelListeners.
	 * An diesen wird ein ModelEvent gefeuert wenn sich das am
	 * PayAccountFile Object oder an der speicherung deselben durch PayAccountsControl etwas ge\uFFFDndert hat
	 *
	 */
	public void addModelListener(ModelListener ml)
	{
		fire.addModelListener(ml);
	}

	/**
	 * Funktion zum l\uFFFDschen eines ModelListeners.
	 */
	public void removeModelListener(ModelListener ml)
	{
		fire.removeModelListener(ml);
	}
}
