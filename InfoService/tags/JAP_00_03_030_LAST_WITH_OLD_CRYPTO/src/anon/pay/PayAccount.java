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

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.util.Enumeration;
import java.util.Vector;
import javax.xml.parsers.DocumentBuilderFactory;

import org.bouncycastle.crypto.params.DSAParameters;
import org.bouncycastle.crypto.params.DSAPrivateKeyParameters;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import anon.crypto.IMyPrivateKey;
import anon.crypto.IMyPublicKey;
import anon.crypto.JAPSignature;
import anon.crypto.MyDSAPrivateKey;
import anon.crypto.MyRSAPrivateKey;
import anon.pay.xml.XMLAccountCertificate;
import anon.pay.xml.XMLAccountInfo;
import anon.pay.xml.XMLBalance;
import anon.pay.xml.XMLEasyCC;
import anon.pay.xml.XMLTransCert;
import anon.util.Base64;
import anon.util.IXMLEncodable;
import anon.util.XMLUtil;
import anon.server.impl.MuxSocket;
import java.sql.Timestamp;

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
 *  <Account version="1.0">
 * 		<AccountCertificate>...</AccountCertificate> // see anon.pay.xml.XMLAccountCertificate
 * 		<RSAPrivateKey>...</RSAPrivateKey> // the secret key. this can be either RSA or DSA
 *      <DSAPrivateKey>...</DSAPrivateKey>
 * 		<TransferCertificates> // see anon.pay.xml.XMLTransCert
 * 			....
 * 		</TransferCertifcates>
 * 		<AccountInfo>...</AccountInfo> // see anon.pay.xml.XMLAccountInfo
 *  </Account>
 *	@author Andreas Mueller, Grischan Gl&auml;nzel, Bastian Voigt
 */
public class PayAccount implements IXMLEncodable
{
	/** contains zero ore more xml transfer certificates as XMLTransCert */
	private Vector m_transCerts;

	/** contains the account certificate */
	private XMLAccountCertificate m_accountCertificate;

	/** contains the current account info (balance and cost confirmations) */
	private XMLAccountInfo m_accountInfo;

	/** contains the private key associated with this account */
	private IMyPrivateKey m_privateKey;

	/** the signing instance */
	private JAPSignature m_signingInstance;

	/** the number of bytes which have been used bot not confirmed yet */
	private long m_currentBytes;

	private Vector m_accountListeners = new Vector();

	/**
	 * internal value for spent bytes. Basically this is the same as spent in {@link anon.pay.xml.XMLBalance},
	 * but the value in XMLBalance is calculated by the BI while this here is calculated
	 * by the Jap. So the value here might be more up to date in case the XMLBalance
	 * certificate is old.
	 */
	private long m_mySpent;
	private BI m_theBI;
	private JAPSignature m_verifyingInstance;

	public PayAccount(byte[] xmlData) throws Exception
	{
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(new
			ByteArrayInputStream(xmlData));
		Element elemRoot = doc.getDocumentElement();
		setValues(elemRoot);
	}

	public PayAccount(Element elemRoot) throws Exception
	{
		setValues(elemRoot);
	}

	public PayAccount(Document doc) throws Exception
	{
		Element elemRoot = doc.getDocumentElement();
		setValues(elemRoot);
	}

	/**
	 * Creates a {@link PayAccount} Objekt from the account certificate and
	 * the private key.
	 *
	 * @param certificate account certificate issued by the BI
	 * @param privateKey the private key
	 */
	public PayAccount(XMLAccountCertificate certificate,
					  IMyPrivateKey privateKey,
					  JAPSignature signingInstance,
					  BI theBI) throws Exception
	{
		m_accountCertificate = certificate;
		m_signingInstance = signingInstance;
		m_privateKey = privateKey;
		m_transCerts = new Vector();
		m_theBI = theBI;
	}

	private void setValues(Element elemRoot) throws Exception
	{
		if (! (elemRoot.getTagName().equals("Account") && (elemRoot.getAttribute("version").equals("1.0"))))
		{
			throw new Exception("PayAccount wrong XML format");
		}

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

		// set private key
		Element elemRsaKey = (Element) XMLUtil.getFirstChildByName(elemRoot, "RSAPrivateKey");
		Element elemDsaKey = (Element) XMLUtil.getFirstChildByName(elemRoot, "DSAPrivateKey");
		if (elemRsaKey != null)
		{
			Element elem = (Element) XMLUtil.getFirstChildByName(elemRsaKey, "Modulus");
			String str = XMLUtil.parseNodeString(elem, null);
			BigInteger modulus = new BigInteger(Base64.decode(str));

			elem = (Element) XMLUtil.getFirstChildByName(elemRsaKey, "PublicExponent");
			str = XMLUtil.parseNodeString(elem, null);
			BigInteger publicExponent = new BigInteger(Base64.decode(str));

			elem = (Element) XMLUtil.getFirstChildByName(elemRsaKey, "PrivateExponent");
			str = XMLUtil.parseNodeString(elem, null);
			BigInteger privateExponent = new BigInteger(Base64.decode(str));

			elem = (Element) XMLUtil.getFirstChildByName(elemRsaKey, "P");
			str = XMLUtil.parseNodeString(elem, null);
			BigInteger p = new BigInteger(Base64.decode(str));

			elem = (Element) XMLUtil.getFirstChildByName(elemRsaKey, "Q");
			str = XMLUtil.parseNodeString(elem, null);
			BigInteger q = new BigInteger(Base64.decode(str));

			elem = (Element) XMLUtil.getFirstChildByName(elemRsaKey, "dP");
			str = XMLUtil.parseNodeString(elem, null);
			BigInteger dP = new BigInteger(Base64.decode(str));

			elem = (Element) XMLUtil.getFirstChildByName(elemRsaKey, "dQ");
			str = XMLUtil.parseNodeString(elem, null);
			BigInteger dQ = new BigInteger(Base64.decode(str));

			elem = (Element) XMLUtil.getFirstChildByName(elemRsaKey, "QInv");
			str = XMLUtil.parseNodeString(elem, null);
			BigInteger qInv = new BigInteger(Base64.decode(str));

			m_privateKey = new MyRSAPrivateKey(modulus, publicExponent, privateExponent, p, q, dP, dQ,
											   qInv);
		}
		else if (elemDsaKey != null)
		{
			Element elem = (Element) XMLUtil.getFirstChildByName(elemDsaKey, "G");
			String str = XMLUtil.parseNodeString(elem, null);
			BigInteger g = new BigInteger(Base64.decode(str));

			elem = (Element) XMLUtil.getFirstChildByName(elemDsaKey, "P");
			str = XMLUtil.parseNodeString(elem, null);
			BigInteger p = new BigInteger(Base64.decode(str));

			elem = (Element) XMLUtil.getFirstChildByName(elemDsaKey, "Q");
			str = XMLUtil.parseNodeString(elem, null);
			BigInteger q = new BigInteger(Base64.decode(str));

			elem = (Element) XMLUtil.getFirstChildByName(elemDsaKey, "X");
			str = XMLUtil.parseNodeString(elem, null);
			BigInteger x = new BigInteger(Base64.decode(str));
			DSAPrivateKeyParameters param = new DSAPrivateKeyParameters(
				x, new DSAParameters(p, q, g));
			m_privateKey = new MyDSAPrivateKey(param);
		}
		else
		{
			throw new Exception("No RSA and no DSA private key found");
		}

		// set signing instance
		m_signingInstance = new JAPSignature();
		m_signingInstance.initSign(m_privateKey);

		/** @todo add BIName here when multiple BIs are supported */
		m_theBI = PayAccountsFile.getInstance().getBI();
	}

	/**
	 * Returns the xml representation of the account
	 *
	 * @return XML-Document
	 */
	public Element toXmlElement(Document a_doc)
	{
		Element elemRoot = a_doc.createElement("Account");
		elemRoot.setAttribute("version", "1.0");
		Element elemTmp;

		// import AccountCertificate XML Representation
		elemTmp = m_accountCertificate.toXmlElement(a_doc);
		elemRoot.appendChild(elemTmp);

		// import Private Key XML Representation
		elemTmp = m_privateKey.toXmlElement(a_doc);
		elemRoot.appendChild(elemTmp);

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

	public void addTransCert(XMLTransCert cert) throws Exception
	{
		m_transCerts.addElement(cert);
	}

	/**
	 * This is not just a setter method. If an accountInfo is already present,
	 * only older information is overwritten with newer information.
	 * @param info XMLAccountInfo
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
				if ( (newCC != null) && (newCC.getTransferredBytes()>myCC.getTransferredBytes()) )
				{
					if(newCC.verifySignature(getVerifyingInstance()))
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
		if(fire==true)
		{
			fireChangeEvent();
		}
	}

	/**
	 * getVerifyingInstance
	 *
	 * @return JAPSignature
	 */
	private JAPSignature getVerifyingInstance() throws Exception
	{
		if(m_verifyingInstance==null)
		{
			m_verifyingInstance = new JAPSignature();
			m_verifyingInstance.initVerify(m_accountCertificate.getPublicKey());
		}
		return m_verifyingInstance;
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

	/**
	 * Returns true when an accountInfo object exists.
	 * New accounts don't have this, it is created when we first
	 * fetch a balance certificate or sign the first CC.
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

	public IMyPrivateKey getPrivateKey()
	{
		return m_privateKey;
	}

	public JAPSignature getSigningInstance()
	{
		return m_signingInstance;
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
	 * Returns the initial amount of the account (i. e. the sum of all
	 * incoming payment)
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
	 * It is possible that this value is outdated, so it may be a good idea to call
	 * {@link Pay.fetchAccountInfo(long)} first.
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
	 * itself. It is possible that this value is outdated, so it may be a good idea
	 * to call {@link updateCurrentBytes()} first.
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
	 * Asks the MuxSocket for the current number of transferred bytes and
	 * updates the internal value.
	 * @return the updated currentBytes value
	 */
	public long updateCurrentBytes(MuxSocket currentMuxSock) throws Exception
	{
		// am I the active account?
		if (PayAccountsFile.getInstance().getActiveAccount() != this)
		{
			throw new Exception("Error: Inactive account called to count used bytes!");
		}

		long tmp = currentMuxSock.getAndResetTransferredBytes();
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
		synchronized (m_accountListeners)
		{
			Enumeration enumListeners = m_accountListeners.elements();
			while (enumListeners.hasMoreElements())
			{
				( (IAccountListener) enumListeners.nextElement()).accountChanged(this);
			}
		}
	}

	/**
	 * getBalance
	 *
	 * @return XMLBalance
	 */
	public XMLBalance getBalance()
	{
		if (m_accountInfo == null )
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
	 *
	 * @throws Exception
	 * @return XMLAccountInfo
	 * @todo switch SSL on
	 */
	public XMLAccountInfo fetchAccountInfo() throws Exception
	{
		XMLAccountInfo info;
		BIConnection biConn = new BIConnection(m_theBI,
											   false
											   /* ssl off! */
											   );
		biConn.connect();
		biConn.authenticate(m_accountCertificate, m_signingInstance);
		info = biConn.getAccountInfo();
		biConn.disconnect();

		// save in the account object
		setAccountInfo(info); // do not access field directly here!!
		return info;
	}

	/**
	 * Request a transfer certificate from the BI
	 *
	 * @param accountNumber account number
	 * @return xml transfer certificate
	 * @throws Exception
	 * @todo switch SSL on
	 */
	public XMLTransCert charge() throws Exception
	{
		BIConnection biConn = new BIConnection(m_theBI, false /* ssl off*/);
		biConn.connect();
		biConn.authenticate(m_accountCertificate, m_signingInstance);
		XMLTransCert transcert = biConn.charge();
		biConn.disconnect();
		m_transCerts.addElement(transcert); //addTransCert(transcert);
		return transcert;
	}

}
