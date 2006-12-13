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

import java.sql.Timestamp;
import java.util.Enumeration;
import java.util.Vector;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import anon.client.PacketCounter;
import anon.crypto.IMyPrivateKey;
import anon.crypto.IMyPublicKey;
import anon.crypto.MyDSAPrivateKey;
import anon.crypto.MyRSAPrivateKey;
import anon.crypto.XMLEncryption;
import anon.pay.xml.XMLAccountCertificate;
import anon.pay.xml.XMLAccountInfo;
import anon.pay.xml.XMLBalance;
import anon.pay.xml.XMLEasyCC;
import anon.pay.xml.XMLTransCert;
import anon.util.IMiscPasswordReader;
import anon.util.IXMLEncodable;
import anon.util.XMLParseException;
import anon.util.XMLUtil;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import anon.infoservice.IMutableProxyInterface;

/**
 * This class encapsulates one account and all additional data associated to one
 * account. This includes the key pair, the account number, the transfer certificates
 * for charging the account, cost confirmations and a balance certificate.

 *
 * For storing the account data in a file the {@link toXmlElement()}
 * method is provided. It is recommended to encrypt the output of this method
 * before storing it to disk, because it includes the secret private key.
 *
 * The XML structure is as follows:
 *
 *  <Account version="1.1" active="true|false">
 * 		<AccountCertificate>...</AccountCertificate> // see anon.pay.xml.XMLAccountCertificate
 * 		<RSAPrivateKey>...</RSAPrivateKey> // the secret key. this can be either RSA or DSA
 *      <DSAPrivateKey>...</DSAPrivateKey>
 * 		<TransferCertificates> // see anon.pay.xml.XMLTransCert
 * 			....
 * 		</TransferCertifcates>
 * 		<AccountInfo>...</AccountInfo> // see anon.pay.xml.XMLAccountInfo
 *  </Account>
 *	@author Andreas Mueller, Grischan Gl&auml;nzel, Bastian Voigt, Tobias Bayer
 */
public class PayAccount implements IXMLEncodable
{
	public static final String XML_ELEMENT_NAME = "Account";
	private static final String XML_ATTR_ACTIVE = "active";
	private static final String XML_BACKUP_DONE = "backupDone";

	private static final String VERSION = "1.1";

	/** contains zero or more xml transfer certificates as XMLTransCert */
	private Vector m_transCerts;

	/** contains the account certificate */
	private XMLAccountCertificate m_accountCertificate;

	/** contains the current account info (balance and cost confirmations) */
	private XMLAccountInfo m_accountInfo;

	/** contains the private key associated with this account */
	private IMyPrivateKey m_privateKey;

	private Document m_encryptedPrivateKey;

	/** the number of bytes which have been used bot not confirmed yet */
	private long m_currentBytes;

	private Vector m_accountListeners = new Vector();

	private boolean m_bBackupDone = false;

	/**
	 * internal value for spent bytes. Basically this is the same as spent in
	 * {@link anon.pay.xml.XMLBalance}, but the value in XMLBalance is calculated
	 * by the BI while this here is calculated by the Jap. So the value here might
	 * be more up to date in case the XMLBalance certificate is old.
	 */
	private long m_mySpent;

	private BI m_theBI;

	private String m_strBiID;

	public PayAccount(Element elemRoot, IMiscPasswordReader a_passwordReader) throws Exception
	{
		setValues(elemRoot, a_passwordReader);
	}

	/**
	 * Creates a {@link PayAccount} Objekt from the account certificate and the
	 * private key.
	 *
	 * @param certificate
	 *          account certificate issued by the BI
	 * @param privateKey
	 *          the private key
	 */
	public PayAccount(XMLAccountCertificate certificate, IMyPrivateKey privateKey, BI theBI) throws Exception
	{
		m_accountCertificate = certificate;
		m_privateKey = privateKey;
		m_transCerts = new Vector();
		m_theBI = theBI;
	}

	/**
	 *
	 * @param elemRoot Element
	 * @param a_passwordReader a password reader; this method adds the account number as message object
	 * @throws Exception
	 */
	private void setValues(Element elemRoot, final IMiscPasswordReader a_passwordReader) throws Exception
	{
		if (elemRoot == null || !(elemRoot.getTagName().equals(XML_ELEMENT_NAME) ))
//								||  (elemRoot.getAttribute(XML_VERSION).compareTo(VERSION)) > 0))
		{
			throw new XMLParseException("PayAccount wrong XML format");
		}
		boolean bActive = XMLUtil.parseAttribute(elemRoot, XML_ATTR_ACTIVE, true);
		m_bBackupDone = XMLUtil.parseAttribute(elemRoot, XML_BACKUP_DONE, false);

		// fill vector with transfer certificates
		m_transCerts = new Vector();
		Element elemTrs = (Element) XMLUtil.getFirstChildByName(elemRoot, "TransferCertificates");
		Element elemTr = (Element) elemTrs.getFirstChild();
		while (elemTr != null)
		{
			m_transCerts.addElement(new XMLTransCert(elemTr));
			elemTr = (Element) elemTr.getNextSibling();
		}

		// set account certificate
		Element elemAccCert = (Element) XMLUtil.getFirstChildByName(elemRoot, "AccountCertificate");
		m_accountCertificate = new XMLAccountCertificate(elemAccCert);

		// set account info
		Element elemAccInfo = (Element) XMLUtil.getFirstChildByName(elemRoot, "AccountInfo");
		if (elemAccInfo != null)
		{
			m_accountInfo = new XMLAccountInfo(elemAccInfo);
		}

		/** @todo get BI by supplying a bi-id */
		Element biid = (Element) XMLUtil.getFirstChildByName(elemAccCert, "BiID");
		m_strBiID = XMLUtil.parseValue(biid, "-1");
		m_theBI = null;

		decryptPrivateKey(elemRoot, a_passwordReader, !bActive);
	}

	/**
	 * Returns the xml representation of the account
	 *
	 * @return XML-Document
	 */
	public Element toXmlElement(Document a_doc)
	{
		return this.toXmlElement(a_doc, null);
	}

	public Element toXmlElement(Document a_doc, String a_password)
	{
		try
		{
			if (a_password != null && a_password.trim().equals(""))
			{
				return this.toXmlElement(a_doc, null);
			}
			Element elemRoot = a_doc.createElement(XML_ELEMENT_NAME);
			elemRoot.setAttribute(XML_ATTR_VERSION, VERSION);
			Element elemTmp;

			// import AccountCertificate XML Representation
			elemTmp = m_accountCertificate.toXmlElement(a_doc);
			elemRoot.appendChild(elemTmp);

			XMLUtil.setAttribute(elemRoot, XML_BACKUP_DONE, m_bBackupDone);

			// import Private Key XML Representation
			if (m_encryptedPrivateKey != null)
			{
				XMLUtil.setAttribute(elemRoot, XML_ATTR_ACTIVE, false);
				elemTmp = (Element)XMLUtil.importNode(
					a_doc, m_encryptedPrivateKey.getDocumentElement(), true);
				elemRoot.appendChild(elemTmp);
			}
			else
			{
				XMLUtil.setAttribute(elemRoot, XML_ATTR_ACTIVE, true);
				elemTmp = m_privateKey.toXmlElement(a_doc);
				elemRoot.appendChild(elemTmp);
				// Encrypt account key if password is given
				if (a_password != null)
				{
					try
					{
						XMLEncryption.encryptElement(elemTmp, a_password);
					}
					catch (Exception e)
					{
						LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, "Could not encrypt account key: " + e);
					}
				}
			}

			// add transfer certificates
			Element elemTransCerts = a_doc.createElement("TransferCertificates");
			elemRoot.appendChild(elemTransCerts);
			if (m_transCerts != null)
			{
				Enumeration enumer = m_transCerts.elements();
				while (enumer.hasMoreElements())
				{
					XMLTransCert cert = (XMLTransCert) enumer.nextElement();
					elemTransCerts.appendChild(cert.toXmlElement(a_doc));
				}
			}

			if (m_accountInfo != null)
			{
				elemTmp = m_accountInfo.toXmlElement(a_doc);
				elemRoot.appendChild(elemTmp);
			}

			return elemRoot;
		}
		catch (Exception ex)
		{
			LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, "Exception while creating PayAccount XML: " + ex);
			return null;
		}
	}

	public void addTransCert(XMLTransCert cert) throws Exception
	{
		m_transCerts.addElement(cert);
	}

	/**
	 * This is not just a setter method. If an accountInfo is already present,
	 * only older information is overwritten with newer information.
	 *
	 * @param info
	 *          XMLAccountInfo
	 */
	public void setAccountInfo(XMLAccountInfo info) throws Exception
	{
		boolean fire = false;
		if (m_accountInfo == null)
		{
			m_accountInfo = info;
			fire = true;
		}
		else
		{
			// compare balance timestamps, use the newer one
			XMLBalance b1 = info.getBalance();
			XMLBalance b2 = m_accountInfo.getBalance();
			if (b1.getTimestamp().after(b2.getTimestamp()))
			{
				m_accountInfo.setBalance(b1);
				fire = true;
			}

			// compare CCs
			Enumeration en = m_accountInfo.getCCs();
			while (en.hasMoreElements())
			{
				XMLEasyCC myCC = (XMLEasyCC) en.nextElement();
				XMLEasyCC newCC = info.getCC(myCC.getAIName());
				if ( (newCC != null) && (newCC.getTransferredBytes() > myCC.getTransferredBytes()))
				{
					if (newCC.verify(m_accountCertificate.getPublicKey()))
					{
						addCostConfirmation(newCC);
						fire = false; // the event is fired by ^^
					}
					else
					{
						throw new Exception("The BI is trying to betray you with faked CostConfirmations");
					}
				}
			}
		}
		if (fire == true)
		{
			fireChangeEvent();
		}
	}

	/**
	 * Returns the account's accountnumber
	 *
	 * @return accountnumber
	 */
	public long getAccountNumber()
	{
		return m_accountCertificate.getAccountNumber();
	}

	public boolean isBackupDone()
	{
		return m_bBackupDone;
	}

	public void setBackupDone(boolean a_bDone)
	{
		m_bBackupDone = a_bDone;
	}

	/**
	 * Returns true when an accountInfo object exists. New accounts don't have
	 * this, it is created when we first fetch a balance certificate or sign the
	 * first CC.
	 */
	public boolean hasAccountInfo()
	{
		return m_accountInfo != null;
	}

	public XMLAccountCertificate getAccountCertificate()
	{
		return m_accountCertificate;
	}

	/**
	 * Liefert das Erstellungsdatum des Kontos.
	 *
	 * @return Erstellungsdatum
	 */
	public Timestamp getCreationTime()
	{
		return m_accountCertificate.getCreationTime();
	}

	/**
	 * liefert zurueck, wann das Guthaben ungueltig wird (bei flatrate modellen)
	 *
	 * @return Date Gueltigkeitsdatum
	 */
	public Timestamp getBalanceValidTime()
	{
		if (m_accountInfo != null)
		{
			return m_accountInfo.getBalance().getValidTime();
		}
		return m_accountCertificate.getCreationTime();
	}

	/**
	 * Returns the private key or null if the account is encrypted and not usable.
	 * @return the private key or null if the account is encrypted and not usable
	 */
	public IMyPrivateKey getPrivateKey()
	{
		return m_privateKey;
	}

	public IMyPublicKey getPublicKey()
	{
		return m_accountCertificate.getPublicKey();
	}

	/**
	 * Returns the amount already spent.
	 *
	 * @return Gesamtsumme
	 */
	public long getSpent()
	{
		if (m_accountInfo != null)
		{
			return m_accountInfo.getBalance().getSpent();
		}
		return 0L;
	}

	/**
	 * Returns the initial amount of the account (i. e. the sum of all incoming
	 * payment)
	 *
	 * @return Gesamtsumme
	 */
	public long getDeposit()
	{
		if (m_accountInfo != null)
		{
			return m_accountInfo.getBalance().getDeposit();
		}
		return 0L;
	}

	/**
	 * Returns the current credit (i. e. deposit - spent) as certified by the BI.
	 * It is possible that this value is outdated, so it may be a good idea to
	 * call {@link Pay.fetchAccountInfo(long)} first.
	 *
	 * @return Guthaben
	 */
	public long getCertifiedCredit()
	{
		if (m_accountInfo != null)
		{
			return m_accountInfo.getBalance().getCredit();
		}
		return 0L;
	}

	/**
	 * Returns the current credit (i. e. deposit - spent) as counted by the Jap
	 * itself. It is possible that this value is outdated, so it may be a good
	 * idea to call {@link updateCurrentBytes()} first.
	 *
	 * @return long
	 */
	public long getCurrentCredit()
	{
		if (m_accountInfo != null)
		{
			return m_accountInfo.getBalance().getDeposit() - m_mySpent;
		}
		return 0L;
	}

	public XMLAccountInfo getAccountInfo()
	{
		return m_accountInfo;
	}

	/**
	 * Returns a vector with all transfer certificates
	 *
	 * @return Vector von {@link XMLTransCert}
	 */
	public Vector getTransCerts()
	{
		return m_transCerts;
	}



	/**
	 * Asks the PacketCounter for the current number of transferred bytes and
	 * updates the internal value.
	 *
	 * @return the updated currentBytes value
	 */
	public long updateCurrentBytes(PacketCounter a_packetCounter) throws Exception
	{
		// am I the active account?
		if (PayAccountsFile.getInstance().getActiveAccount() != this)
		{
			throw new Exception("Error: Inactive account called to count used bytes!");
		}

		long tmp = a_packetCounter.getAndResetBytesForPayment();
		if (tmp > 0)
		{
			m_currentBytes += tmp;
			fireChangeEvent();
		}
		return m_currentBytes;
	}

	/**
	 * addCostConfirmation
	 */
	public void addCostConfirmation(XMLEasyCC cc) throws Exception
	{
		if (m_accountInfo == null)
		{
			m_accountInfo = new XMLAccountInfo();
		}
		m_mySpent += m_accountInfo.addCC(cc);
		fireChangeEvent();
	}

	public void addAccountListener(IAccountListener listener)
	{
		synchronized (m_accountListeners)
		{
			if (listener != null)
			{
				m_accountListeners.addElement(listener);
			}
		}
	}

	private void fireChangeEvent()
	{
		Enumeration enumListeners;

		// synchronized (m_accountListeners) // deadly for jdk 1.1.8...
		{
			/*
			 * Clone the Vector as otherwise there would be a deadlock with at least
			 * PayAccountsFile because of mutual listeners.
			 */
			enumListeners = ( (Vector) m_accountListeners.clone()).elements();
		}

		while (enumListeners.hasMoreElements())
		{
			( (IAccountListener) enumListeners.nextElement()).accountChanged(this);
		}
	}

	/**
	 * getBalance
	 *
	 * @return XMLBalance
	 */
	public XMLBalance getBalance()
	{
		if (m_accountInfo == null)
		{
			return null;
		}
		else
		{
			return m_accountInfo.getBalance();
		}
	}

	/**
	 * Requests an AccountInfo XML structure from the BI.
	 * @param a_bForce if the update is forced; if not, it might be prevented by
	 * PayAccountsFile.isBalanceAutoUpdateEnabled()
	 * @throws Exception
	 * @return XMLAccountInfo
	 * @todo switch SSL on
	 * @throws java.lang.SecurityException if the account is encrypted an not usable
	 */
	public XMLAccountInfo fetchAccountInfo(IMutableProxyInterface a_proxys, boolean a_bForce)
		throws SecurityException, Exception
	{
		if (!a_bForce && !PayAccountsFile.getInstance().isBalanceAutoUpdateEnabled())
		{
			return null;
		}

		if (getPrivateKey() == null)
		{
			throw new SecurityException("Account is encrypted and not usable!");
		}

		XMLAccountInfo info;
		m_theBI = this.getBI();
		BIConnection biConn = new BIConnection(m_theBI);
		biConn.connect(a_proxys);
		biConn.authenticate(m_accountCertificate, m_privateKey);
		info = biConn.getAccountInfo();
		biConn.disconnect();

		// save in the account object
		setAccountInfo(info); // do not access field directly here!!
		return info;
	}

	/**
	 * Request a transfer certificate from the BI
	 *
	 * @param accountNumber
	 *          account number
	 * @return xml transfer certificate
	 * @throws Exception
	 * @throws java.lang.SecurityException if the account is encrypted an not usable
	 */
	public XMLTransCert charge(IMutableProxyInterface a_proxys) throws SecurityException, Exception
	{
		if (getPrivateKey() == null)
		{
			throw new SecurityException("Account is encrypted and not usable!");
		}

		BIConnection biConn = new BIConnection(m_theBI);
		biConn.connect(a_proxys);
		biConn.authenticate(m_accountCertificate, m_privateKey);
		XMLTransCert transcert = biConn.charge();
		biConn.disconnect();
		m_transCerts.addElement(transcert);
		return transcert;
	}

	/**
	 * Marks the account as updated so a ChangeEvent gets fired
	 */
	public void updated()
	{
		fireChangeEvent();
	}

	public BI getBI()
	{
		if (m_theBI == null)
		{
			try
			{
				m_theBI = PayAccountsFile.getInstance().getBI(m_strBiID);
			}
			catch (Exception e)
			{
			}
		}

		return m_theBI;
	}

	public void decryptPrivateKey(IMiscPasswordReader a_passwordReader) throws Exception
	{
		if (m_encryptedPrivateKey != null)
		{
			decryptPrivateKey(m_encryptedPrivateKey, a_passwordReader, false);
		}
	}

	private void decryptPrivateKey(Node a_elemRoot, final IMiscPasswordReader a_passwordReader,
								   boolean a_bDeactivated)
		throws Exception
	{
		if (m_privateKey != null || a_elemRoot == null)
		{
			return;
		}

		Element elemKey = (Element) XMLUtil.getFirstChildByName(a_elemRoot, XMLEncryption.XML_ELEMENT_NAME);
		if (elemKey != null)
		{
			try
			{
				if (a_bDeactivated)
				{
					deactivate(elemKey);
					return;
				}

				IMiscPasswordReader passwordReader;
				if (a_passwordReader != null)
				{
					passwordReader = new IMiscPasswordReader()
					{
						public String readPassword(Object a_message)
						{
							return a_passwordReader.readPassword(
								new String("" + m_accountCertificate.getAccountNumber()));
						}
					};
				}
				else
				{
					passwordReader = a_passwordReader;
				}
				LogHolder.log(LogLevel.DEBUG, LogType.PAY,
							  "Decrypting account " + m_accountCertificate.getAccountNumber());
				XMLEncryption.decryptElement(elemKey, passwordReader);
			}
			catch (Exception a_e)
			{
				deactivate(elemKey);
				return;
			}
		}

		// set private key
		Element elemRsaKey = (Element) XMLUtil.getFirstChildByName(a_elemRoot, MyRSAPrivateKey.XML_ELEMENT_NAME);
		Element elemDsaKey = (Element) XMLUtil.getFirstChildByName(a_elemRoot, MyDSAPrivateKey.XML_ELEMENT_NAME);
		if (elemRsaKey != null)
		{
			if (a_bDeactivated)
			{
				deactivate(elemRsaKey);
				return;
			}

			m_privateKey = new MyRSAPrivateKey(elemRsaKey);
		}
		else if (elemDsaKey != null)
		{
			if (a_bDeactivated)
			{
				deactivate(elemDsaKey);
				return;
			}

			m_privateKey = new MyDSAPrivateKey(elemDsaKey);
		}
		else
		{
			throw new XMLParseException("No RSA and no DSA private key found");
		}
		m_encryptedPrivateKey = null;
	}

	private void deactivate(Element elemKey) throws Exception
	{
		m_privateKey = null;
		m_encryptedPrivateKey = XMLUtil.createDocument();
		m_encryptedPrivateKey.appendChild(XMLUtil.importNode(m_encryptedPrivateKey, elemKey, true));
	}
}