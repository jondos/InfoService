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

import java.io.ByteArrayInputStream;
import java.math.BigInteger;
import java.util.Date;
import java.util.Enumeration;
import java.util.Vector;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import anon.crypto.JAPSignature;
import anon.crypto.MyRSAPrivateKey;
import anon.crypto.MyRSAPublicKey;
import anon.util.Base64;
import anon.util.XMLUtil;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import payxml.XMLAccountCertificate;
import payxml.XMLAccountInfo;
import payxml.XMLDocument;
import payxml.XMLEasyCC;
import payxml.XMLTransCert;
import java.security.PrivateKey;
import anon.crypto.IMyPrivateKey;
import anon.crypto.IMyPublicKey;
import anon.crypto.MyDSAPrivateKey;
import org.bouncycastle.crypto.params.DSAPrivateKeyParameters;
import org.bouncycastle.crypto.params.DSAParameters;
import payxml.XMLBalance;

/**
 *  Diese Klasse ist f?r die verwaltung eines Accounts zut?ndig, sie kapselt eine XML Struktur innerhalb der Klasse
 *	und Mithilfe von Klassen des payxml Packages
 *  Die Struktur ist wie folgend:
 *  <Account version="1.0">
 * 		<AccountCertificate>...</AccountCertificate> // Kontozertiufkat von der BI unterschrieben
 * 		<RSAPrivateKey>...</RSAPrivateKey> // der geheime RSA-Schl?ssel zum Zugriff auf das Konto
 *      <DSAPrivateKey>...</DSAPrivateKey> // alternativ: der geheime DSA-Schluessel fuer das Konto
 * 		<TransferCertificates> //offenen Transaktionsummern
 * 			....
 * 		</TransferCertifcates>
 * 		<AccountInfo>...</AccountInfo> //Kontostand (siehe XMLAccountInfo)
 *  </Account>
 *	* @author Andreas Mueller, Grischan Gl&auml;nzel, Bastian Voigt
 */
public class PayAccount extends XMLDocument
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

	public PayAccount(byte[] xmlData) throws Exception
	{
		Document doc = getDocumentBuilder().parse(new ByteArrayInputStream(xmlData));
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
	}

	public void setAccountInfo(XMLAccountInfo info)
	{
		m_accountInfo = info;
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
					  JAPSignature signingInstance) throws Exception
	{
		m_accountCertificate = certificate;
		m_signingInstance = signingInstance;
		m_privateKey = privateKey;
		m_transCerts = new Vector();
	}

	/**
	 * Returns the xml representation of the account
	 *
	 * @return XML-Document
	 */
	public Document getDomDocument()
	{
		Document doc = null;
		try
		{
			doc = getDocumentBuilder().newDocument();
		}
		catch (Exception ex)
		{
			return null;
		}
		Element elemRoot = doc.createElement("Account");
		Element elemTmp;

		elemRoot.setAttribute("version", "1.0");
		doc.appendChild(elemRoot);

		// import AccountCertificate XML Representation
		elemTmp = m_accountCertificate.toXmlElement(doc);
		elemRoot.appendChild(elemTmp);

		// import Private Key XML Representation
		elemTmp = m_privateKey.toXmlElement(doc);
		elemRoot.appendChild(elemTmp);


		// add transfer certificates
		Element elemTransCerts = doc.createElement("TransferCertificates");
		elemRoot.appendChild(elemTransCerts);
		if (m_transCerts != null)
		{
			Enumeration enumer = m_transCerts.elements();
			while (enumer.hasMoreElements())
			{
				XMLTransCert cert = (XMLTransCert) enumer.nextElement();
				Node n1 = null;
				try
				{
					n1 = XMLUtil.importNode(doc, cert.getDomDocument().getDocumentElement(), true);
					elemTransCerts.appendChild(n1);
				}
				catch (Exception ex2)
				{
				}
			}
		}

		if (m_accountInfo != null) {
			elemTmp = m_accountInfo.toXmlElement(doc);
			elemRoot.appendChild(elemTmp);
		}

		return doc;
	}

	/**
	 * Hinzuf?gen eines Transfer-Zertifikats.
	 *
	 * @param cert Transfer-Zertifikat
	 */
	public void addTransCert(XMLTransCert cert) throws Exception
	{
		m_transCerts.addElement(cert);
//		getDomDocument();
	}

	/**
	 * Setzen des Kontoguthabens.
	 *
	 * @param balance Kontoguthaben
	 */
	public void setBalance(XMLAccountInfo info) throws Exception
	{
		m_accountInfo = info;
//		getDomDocument();
	}

	private void setRSAPrivateKey() throws Exception
	{
	}

	/**
	 * Liefert Kontonummer des Kontos.
	 *
	 * @return Kontonummer
	 */
	public long getAccountNumber()
	{
		return m_accountCertificate.getAccountNumber();
	}

	/**
	 * gibt an ob ?berhaupt ein Kontostanddokument existiert
	 */
	public boolean hasAccountInfo()
	{
		return m_accountInfo != null;
	}

	/**
	 * Liefert das Kontozertifikat.
	 *
	 * @return Kontozertifikat
	 */
	public XMLAccountCertificate getAccountCertificate()
	{
		return m_accountCertificate;
	}

	/**
	 * Liefert das Erstellungsdatum des Kontos.
	 *
	 * @return Erstellungsdatum
	 */
	public Date getCreationDate()
	{
		return m_accountCertificate.getCreationTime();
	}

	/**
	 * liefert zurueck, wann das Guthaben ungueltig wird (bei flatrate modellen)
	 *
	 * @return Date Gueltigkeitsdatum
	 */
	public Date getBalanceValidTime()
	{
		if (m_accountInfo != null)
		{
			return m_accountInfo.getBalance().getValidTime();
		}
		return m_accountCertificate.getCreationTime();
	}

	/**
	 * Liefert den geheimen Schl?ssel des Kontos.
	 *
	 * @return Geheimer Schl?ssel
	 */
	public IMyPrivateKey getPrivateKey()
	{
		return m_privateKey;
	}

	public JAPSignature getSigningInstance()
	{
		return m_signingInstance;
	}

	/**
	 * Liefert den ?ffentlichen Schl?ssel des Kontos.
	 *
	 * @return ?ffentlicher Schl?ssel
	 */
	public IMyPublicKey getPublicKey()
	{
		return m_accountCertificate.getPublicKey();
	}

	/**
	 * Liefert die Gesamtsumme des ausgegebenen Geldes.
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
	 * Liefert die Gesamtsumme des eingezahlten Geldes.
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
	 * Liefert das noch verbleibene Guthaben.
	 *
	 * @return Guthaben
	 */
	public long getCredit()
	{
		if (m_accountInfo != null)
		{
			return m_accountInfo.getBalance().getCredit();
		}
		return 0L;
	}

	/**
	 * Liefert die Kostenbest?tigungen.
	 *
	 * @return Vector von CostConfirms
	 */
	/* obsolete	public XMLCostConfirmations getCostConfirms()
	 {
	  return m_costConfirms;
	 }*/

	public XMLAccountInfo getAccountInfo()
	{
		return m_accountInfo;
	}

	/**
	 * Liefert alle Transfer-Zertifikate.
	 *
	 * @return Vector von {@link XMLTransCert}
	 */
	public Vector getTransCerts()
	{
		return m_transCerts;
	}

	/**
	 * addCostConfirmation
	 *
	 * @param aiName String
	 * @param accountNumber long
	 * @param plusCosts long
	 * @return XMLEasyCC
	 * @todo implement
	 */
	public XMLEasyCC addCostConfirmation(String aiName, long accountNumber, long plusCosts)
	{
		return null;
	}

	private Vector m_changeListeners = new Vector();

	public void addChangeListener(ChangeListener listener)
	{
		synchronized (m_changeListeners)
		{
			if (listener != null)
			{
				m_changeListeners.addElement(listener);
			}
		}
	}

	private void fireChangeEvent(Object source)
	{
		synchronized (m_changeListeners)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.PAY, "PayAccountsFile: FireChangeEvent..");
			Enumeration enumListeners = m_changeListeners.elements();
			ChangeEvent e = new ChangeEvent(source);
			while (enumListeners.hasMoreElements())
			{
				( (ChangeListener) enumListeners.nextElement()).stateChanged(e);
			}
		}
	}
}
