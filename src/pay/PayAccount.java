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
import java.util.Date;
import java.util.Enumeration;
import java.util.Vector;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import payxml.XMLAccountInfo;
import payxml.XMLAccountCertificate;
import payxml.XMLAccountInfo;
import payxml.XMLDocument;
import payxml.XMLTransCert;
import anon.util.Base64;
import anon.util.XMLUtil;
import org.w3c.dom.*;
import anon.crypto.MyRSAPrivateKey;
import anon.crypto.MyRSAPublicKey;
import anon.crypto.JAPSignature;
import payxml.XMLEasyCC;

/**
 *  Diese Klasse ist f?r die verwaltung eines Accounts zut?ndig, sie kapselt eine XML Struktur innerhalb der Klasse
 *	und Mithilfe von Klassen des payxml Packages
 *  Die Struktur ist wie folgend:
 *  <Account version="1.0">
 * 		<AccountCertificate>...</AccountCertificate> // Kontozertiufkat von der BI unterschrieben
 * 		<RSAPrivateKey>...</RSAPrivateKey> //der geheime Schl?ssel zum Zugriff auf das Konto
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
	private MyRSAPrivateKey m_privateKey;

	/** the signing instance */
	private JAPSignature m_signingInstance;

	// Exceptions
	public static class WrongCertificateException extends Exception
	{};
	public static class WrongCCsException extends Exception
	{};

	/**
	 * Erzeugt ein PayAccount Objekt aus einem XML-Dokument.
	 *
	 * @param xmlData XML-Dokument
	 * @throws Exception Wenn XML-Dokument fehlerhaft
	 */
	public PayAccount(byte[] xmlData) throws Exception
	{
		setDocument(xmlData);
		setValues();
	}

	/**
	 * Erzeugt ein PayAccount Objekt aus einem XML-Dokument.
	 *
	 * @param xml XML-Dokument
	 * @throws Exception Wenn XML-Dokument fehlerhaft
	 */
	public PayAccount(Node xml) throws Exception
	{
		m_theDocument = getDocumentBuilder().newDocument();
		Node n = XMLUtil.importNode(m_theDocument, xml, true);
		m_theDocument.appendChild(n);
		setValues();
	}

	private void setValues() throws Exception
	{

		// fill vector with transfer certificates
		m_transCerts = new Vector();
		Element elemRoot = m_theDocument.getDocumentElement();
		Element elemTrs = (Element) XMLUtil.getFirstChildByName(elemRoot, "TransferCertfificates");
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
		m_accountInfo = new XMLAccountInfo(elemAccInfo, null);

		// set private key
		setRSAPrivateKey();

		// set signing instance
		m_signingInstance = new JAPSignature();
		m_signingInstance.initSign(m_privateKey);
	}

	public void setAccountInfo(XMLAccountInfo info) throws Exception
	{
		m_accountInfo = info;
		constructXMLDocument();
	}

	/**
	 * Erzeugt ein {@link PayAccount} Objekt aus einem Kontozertifikat und dem
	 * zugeh?rigen geheimen Schl?ssel.
	 *
	 * @param certificate Kontozertifikat
	 * @param privateKey geheimer Schl?ssel
	 */
	public PayAccount(XMLAccountCertificate certificate,
					  MyRSAPrivateKey privateKey,
					  JAPSignature signingInstance) throws Exception
	{
		m_signingInstance = signingInstance;
		m_privateKey = privateKey;


		m_theDocument = getDocumentBuilder().newDocument();
		Element elemRoot = m_theDocument.createElement("Account");
		m_theDocument.appendChild(elemRoot);

	}

	/**
	 * Liefert die XML-Repr&auml;sentation des Kontos.
	 *
	 * @return XML-Dokument als String
	 */
	private void constructXMLDocument() throws Exception
	{
		m_theDocument = getDocumentBuilder().newDocument();
		Element elemRoot = m_theDocument.createElement("Account");
		elemRoot.setAttribute("version", "1.0");
		m_theDocument.appendChild(elemRoot);
		Document tmpDoc = m_accountCertificate.getDomDocument();
		Node n = XMLUtil.importNode(m_theDocument, tmpDoc.getDocumentElement(), true);
		elemRoot.appendChild(n);

		Element elemPrivKey = m_theDocument.createElement("RSAPrivateKey");
		elemRoot.appendChild(elemPrivKey);
		Element elem = m_theDocument.createElement("Modulus");
		elemPrivKey.appendChild(elem);
		XMLUtil.setNodeValue(elem, Base64.encodeBytes(m_privateKey.getModulus().toByteArray()));
		elem = m_theDocument.createElement("PublicExponent");
		elemPrivKey.appendChild(elem);
		XMLUtil.setNodeValue(elem, Base64.encodeBytes(m_privateKey.getPublicExponent().toByteArray()));
		elem = m_theDocument.createElement("PrivateExponent");
		elemPrivKey.appendChild(elem);
		XMLUtil.setNodeValue(elem, Base64.encodeBytes(m_privateKey.getPrivateExponent().toByteArray()));
		elem = m_theDocument.createElement("P");
		elemPrivKey.appendChild(elem);
		XMLUtil.setNodeValue(elem, Base64.encodeBytes(m_privateKey.getP().toByteArray()));
		elem = m_theDocument.createElement("Q");
		elemPrivKey.appendChild(elem);
		XMLUtil.setNodeValue(elem, Base64.encodeBytes(m_privateKey.getQ().toByteArray()));
		elem = m_theDocument.createElement("dP");
		elemPrivKey.appendChild(elem);
		XMLUtil.setNodeValue(elem, Base64.encodeBytes(m_privateKey.getDP().toByteArray()));
		elem = m_theDocument.createElement("dQ");
		elemPrivKey.appendChild(elem);
		XMLUtil.setNodeValue(elem, Base64.encodeBytes(m_privateKey.getDQ().toByteArray()));
		elem = m_theDocument.createElement("QInv");
		elemPrivKey.appendChild(elem);
		XMLUtil.setNodeValue(elem, Base64.encodeBytes(m_privateKey.getQInv().toByteArray()));

		// add transfer certificates
		Element elemTransCerts = m_theDocument.createElement("TransferCertificates");
		elemRoot.appendChild(elemTransCerts);
		Enumeration enum = m_transCerts.elements();
		while (enum.hasMoreElements())
		{
			XMLTransCert cert = (XMLTransCert) enum.nextElement();
			Node n1 = XMLUtil.importNode(m_theDocument, cert.getDomDocument().getDocumentElement(), true);
			elemTransCerts.appendChild(n1);
		}

		if (m_accountInfo != null)
		{
			Node n1 = XMLUtil.importNode(m_theDocument, m_accountInfo.getDomDocument().getDocumentElement(), true);
			elemRoot.appendChild(n1);
		}
	}

	/**
	 * Hinzuf?gen eines Transfer-Zertifikats.
	 *
	 * @param cert Transfer-Zertifikat
	 */
	public void addTransCert(XMLTransCert cert) throws Exception
	{
		m_transCerts.addElement(cert);
		constructXMLDocument();
	}

	/**
	 * Setzen des Kontoguthabens.
	 *
	 * @param balance Kontoguthaben
	 */
	public void setBalance(XMLAccountInfo info) throws Exception
	{
		m_accountInfo = info;
		constructXMLDocument();
	}

	private void setXMLAccountCertificate() throws Exception
	{
		Element elemRoot = m_theDocument.getDocumentElement();
		Element elem = (Element) XMLUtil.getFirstChildByName(elemRoot, "AccountCertificate");
		m_accountCertificate = new XMLAccountCertificate(elem);
	}

	private void setXMLBalance()
	{
		try
		{
			Element elemRoot = m_theDocument.getDocumentElement();
			Element elem = (Element) XMLUtil.getFirstChildByName(elemRoot, "Balance");

			// todo: add signature verifying
			m_accountInfo = new XMLAccountInfo(elem, null);
		}
		catch (Exception e)
		{
			m_accountInfo = null;
		}
	}

	private void setXMLTransCertificates() throws Exception
	{
		try
		{
			Element elemRoot = m_theDocument.getDocumentElement();
			Element elemTransCerts = (Element) XMLUtil.getFirstChildByName(elemRoot, "TransferCertificates");
			Node n = elemTransCerts.getFirstChild();
			while (n != null)
			{
				try
				{
					XMLTransCert cert = new XMLTransCert(n);
					m_transCerts.addElement(cert);
				}
				catch (Exception e)
				{
				}
				n = n.getNextSibling();
			}
		}
		catch (Exception e)
		{
			return;
		}
	}

	private void setRSAPrivateKey() throws Exception
	{
		Element elemRoot = m_theDocument.getDocumentElement();
		Element elemPrivKey = (Element) XMLUtil.getFirstChildByName(elemRoot, "RSAPrivateKey");
		Element elem = (Element) XMLUtil.getFirstChildByName(elemPrivKey, "Modulus");
		String str = XMLUtil.parseNodeString(elem, null);
		BigInteger modulus = new BigInteger(Base64.decode(str));

		elem = (Element) XMLUtil.getFirstChildByName(elemPrivKey, "PublicExponent");
		str = XMLUtil.parseNodeString(elem, null);
		BigInteger publicExponent = new BigInteger(Base64.decode(str));

		elem = (Element) XMLUtil.getFirstChildByName(elemPrivKey, "PrivateExponent");
		str = XMLUtil.parseNodeString(elem, null);
		BigInteger privateExponent = new BigInteger(Base64.decode(str));

		elem = (Element) XMLUtil.getFirstChildByName(elemPrivKey, "P");
		str = XMLUtil.parseNodeString(elem, null);
		BigInteger p = new BigInteger(Base64.decode(str));

		elem = (Element) XMLUtil.getFirstChildByName(elemPrivKey, "Q");
		str = XMLUtil.parseNodeString(elem, null);
		BigInteger q = new BigInteger(Base64.decode(str));

		elem = (Element) XMLUtil.getFirstChildByName(elemPrivKey, "dP");
		str = XMLUtil.parseNodeString(elem, null);
		BigInteger dP = new BigInteger(Base64.decode(str));

		elem = (Element) XMLUtil.getFirstChildByName(elemPrivKey, "dQ");
		str = XMLUtil.parseNodeString(elem, null);
		BigInteger dQ = new BigInteger(Base64.decode(str));

		elem = (Element) XMLUtil.getFirstChildByName(elemPrivKey, "QInv");
		str = XMLUtil.parseNodeString(elem, null);
		BigInteger qInv = new BigInteger(Base64.decode(str));

		m_privateKey = new MyRSAPrivateKey(modulus, publicExponent, privateExponent, p, q, dP, dQ,
										   qInv);
		m_signingInstance = new JAPSignature();
		m_signingInstance.initSign(m_privateKey);

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
			return m_accountInfo.getValidTime();
		}
		return m_accountCertificate.getCreationTime();
	}

	/**
	 * Liefert den geheimen Schl?ssel des Kontos.
	 *
	 * @return Geheimer Schl?ssel
	 */
	public MyRSAPrivateKey getPrivateKey()
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
	public MyRSAPublicKey getPublicKey()
	{
		return m_accountCertificate.getPublicKey();
	}

	/**
	 * Liefert die Gesamtsumme des eingezahlten Geldes.
	 *
	 * @return Gesamtsumme
	 */
	public long getCreditMax()
	{
		if (m_accountInfo != null)
		{
			return m_accountInfo.getCreditMax();
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
			return m_accountInfo.getCredit();
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
	 */
	public XMLEasyCC addCostConfirmation(String aiName, long accountNumber, long plusCosts)
	{
		return null;
	}
}
