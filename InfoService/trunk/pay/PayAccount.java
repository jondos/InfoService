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
import org.bouncycastle.crypto.params.RSAKeyParameters;
import org.bouncycastle.crypto.params.RSAPrivateCrtKeyParameters;
import org.w3c.dom.CharacterData;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import payxml.XMLBalance;
import payxml.XMLAccountCertificate;
import payxml.XMLCostConfirmations;
import payxml.XMLDocument;
import payxml.XMLTransCert;
import anon.util.Base64;
import anon.util.XMLUtil;
import org.w3c.dom.*;
import anon.crypto.MyRSAPrivateKey;
/**
 *  Diese Klasse ist für die verwaltung eines Accounts zutändig, sie kapselt eine XML Struktur innerhalb der Klasse
 *	und Mithilfe von Klassen des payxml Packages
 *  Die Struktur ist wie folgend:
 *  <Account version="1.0">
 * 		<AccountCertificate>...</AccountCertificate> // Kontozertiufkat von der BI unterschrieben
 * 		<RSAPrivateKey>...</RSAPrivateKey> //der geheime Schlüssel zum Zugriff auf das Konto
 * 		<TransferCertificates> //offenen Transaktionsummern
 * 			....
 * 		</TransferCertifcates>
 * 		<Balance>...</Balance> //Kontostand (siehe XMLBalance)
 * 		<CostConfirmations>  //Kostenbestätigungen pro AI
 * 		</CostConfirmations>
 *  </Account>
 *	* @author Andreas Mueller, Grischan Glänzel
 */
public class PayAccount extends XMLDocument
{
	private XMLAccountCertificate m_AccountCertificate;
	private XMLBalance m_Balance;
	private MyRSAPrivateKey m_privateKey;
	private Vector m_transCerts;
	private XMLCostConfirmations m_costConfirms;

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
		m_transCerts = new Vector();
		m_costConfirms = new XMLCostConfirmations();
		setXMLCertificate();
		setXMLBalance();
		setRSAPrivateKey();
		setXMLTransCert();
		setXMLCostConfirms();
		LogHolder.log(LogLevel.DEBUG, LogType.PAY,
					  "PayAccount Object AccountNr." + m_AccountCertificate.getAccountNumber() + " complete");
	}

	/**
	 * Erzeugt ein {@link PayAccount} Objekt aus einem Kontozertifikat und dem
	 * zugehörigen geheimen Schlüssel.
	 *
	 * @param certificate Kontozertifikat
	 * @param privateKey geheimer Schlüssel
	 */
	public PayAccount(XMLAccountCertificate certificate, MyRSAPrivateKey privateKey) throws
		Exception
	{
		m_AccountCertificate = certificate;
		m_privateKey = privateKey;
		m_transCerts = new Vector();
		m_costConfirms = new XMLCostConfirmations();
		constructXMLDocument();
	}

	/**
	 * Liefert die XML-Präsentation des Kontos.
	 *
	 * @param withHead Mit XML-Kopf?
	 * @return XML-Dokument als String
	 */
	private void constructXMLDocument() throws Exception
	{
		m_theDocument = getDocumentBuilder().newDocument();
		Element elemRoot = m_theDocument.createElement("Account");
		elemRoot.setAttribute("version", "1.0");
		m_theDocument.appendChild(elemRoot);
		Document tmpDoc = m_AccountCertificate.getDomDocument();
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

		Element elemTransCerts = m_theDocument.createElement("TransferCertificates");
		elemRoot.appendChild(elemTransCerts);
		Enumeration enum = m_transCerts.elements();
		while (enum.hasMoreElements())
		{
			XMLTransCert cert = (XMLTransCert) enum.nextElement();
			Node n1 = XMLUtil.importNode(m_theDocument, cert.getDomDocument().getDocumentElement(), true);
			elemTransCerts.appendChild(n1);
		}

		if (m_Balance != null)
		{
			Node n1 = XMLUtil.importNode(m_theDocument, m_Balance.getDomDocument().getDocumentElement(), true);
			elemRoot.appendChild(n1);
			n1 = XMLUtil.importNode(m_theDocument, m_costConfirms.getDomDocument().getDocumentElement(), true);
			elemRoot.appendChild(n1);
		}
	}

	/**
	 * Hinzufügen eines Transfer-Zertifikats.
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
	public void setBalance(XMLBalance balance) throws Exception
	{
		m_Balance = balance;
		constructXMLDocument();
	}

	public void setCostConfirms(XMLCostConfirmations costConfirms) throws Exception
	{
		m_costConfirms = costConfirms;
		constructXMLDocument();
	}

	private void setXMLCertificate() throws Exception
	{
		Element elemRoot = m_theDocument.getDocumentElement();
		Element elem = (Element) XMLUtil.getFirstChildByName(elemRoot, "AccountCertificate");
		m_AccountCertificate = new XMLAccountCertificate(elem);
	}

	private void setXMLBalance()
	{
		try
		{
			Element elemRoot = m_theDocument.getDocumentElement();
			Element elem = (Element) XMLUtil.getFirstChildByName(elemRoot, "Balance");
			m_Balance = new XMLBalance(elem);
		}
		catch (Exception e)
		{
			m_Balance = null;
		}
	}

	private void setXMLTransCert() throws Exception
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

	private void setXMLCostConfirms() throws Exception
	{
		try
		{
			Element elemRoot = m_theDocument.getDocumentElement();
			Element elemConfirms = (Element) XMLUtil.getFirstChildByName(elemRoot, "Confirmations");
			m_costConfirms = new XMLCostConfirmations(elemConfirms);
		}
		catch (Exception ex)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.PAY, "noch keine CostConfirmations vorhanden");
			m_costConfirms = new XMLCostConfirmations();
		}
	}

	private void setRSAPrivateKey() throws Exception
	{
		Element elemRoot = m_theDocument.getDocumentElement();
		Element elemPrivKey=(Element)XMLUtil.getFirstChildByName(elemRoot,"RSAPrivateKey");
		Element elem=(Element)XMLUtil.getFirstChildByName(elemPrivKey,"Modulus");
		String str=XMLUtil.parseNodeString(elem,null);
		BigInteger modulus =new BigInteger(Base64.decode(str));

		elem=(Element)XMLUtil.getFirstChildByName(elemPrivKey,"PublicExponent");
		str=XMLUtil.parseNodeString(elem,null);
		BigInteger publicExponent =new BigInteger(Base64.decode(str));

		elem=(Element)XMLUtil.getFirstChildByName(elemPrivKey,"PrivateExponent");
		str=XMLUtil.parseNodeString(elem,null);
		BigInteger privateExponent =new BigInteger(Base64.decode(str));

		elem=(Element)XMLUtil.getFirstChildByName(elemPrivKey,"P");
		str=XMLUtil.parseNodeString(elem,null);
		BigInteger p =new BigInteger(Base64.decode(str));

		elem=(Element)XMLUtil.getFirstChildByName(elemPrivKey,"Q");
		str=XMLUtil.parseNodeString(elem,null);
		BigInteger q =new BigInteger(Base64.decode(str));

		elem=(Element)XMLUtil.getFirstChildByName(elemPrivKey,"dP");
		str=XMLUtil.parseNodeString(elem,null);
		BigInteger dP=new BigInteger(Base64.decode(str));

		elem=(Element)XMLUtil.getFirstChildByName(elemPrivKey,"dQ");
		str=XMLUtil.parseNodeString(elem,null);
		BigInteger dQ =new BigInteger(Base64.decode(str));

		elem=(Element)XMLUtil.getFirstChildByName(elemPrivKey,"QInv");
		str=XMLUtil.parseNodeString(elem,null);
		BigInteger qInv =new BigInteger(Base64.decode(str));

		m_privateKey = new MyRSAPrivateKey(modulus, publicExponent, privateExponent, p, q, dP, dQ,
			qInv);

	}

	/**
	 * Liefert Kontonummer des Kontos.
	 *
	 * @return Kontonummer
	 */
	public long getAccountNumber()
	{
		return m_AccountCertificate.getAccountNumber();
	}

	/**
	 * gibt an ob überhaupt ein Kontostanddokument existiert
	 */
	public boolean hasBalance()
	{
		return m_Balance != null;
	}

	/**
	 * Liefert das Kontozertifikat.
	 *
	 * @return Kontozertifikat
	 */
	public XMLAccountCertificate getAccountCertificate()
	{
		return m_AccountCertificate;
	}

	/**
	 * Liefert das Gültigkeitsdatum des Kontos.
	 *
	 * @return Gültigkeitsende
	 */
	public Date getValidFrom()
	{
		return m_AccountCertificate.getCreationTime();
	}

	public Date getValidTo()
	{
		if (m_Balance != null)
		{
			return m_Balance.getValidTime();
		}
		return m_AccountCertificate.getCreationTime();
	}

	/**
	 * Liefert den geheimen Schlüssel des Kontos.
	 *
	 * @return Geheimer Schlüssel
	 */
	public MyRSAPrivateKey getPrivateKey()
	{
		return m_privateKey;
	}

	/**
	 * Liefert den öffentlichen Schlüssel des Kontos.
	 *
	 * @return Öffentlicher Schlüssel
	 */
	public RSAKeyParameters getPublicKey()
	{
		return new RSAKeyParameters(false, m_AccountCertificate.getPublicKey().getModulus(),
									m_AccountCertificate.getPublicKey().getPublicExponent());
	}

	/**
	 * Liefert die Gesamtsumme des eingezahlten Geldes.
	 *
	 * @return Gesamtsumme
	 */
	public long getCreditMax()
	{
		if (m_Balance != null)
		{
			return m_Balance.getCreditMax();
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
		if (m_Balance != null)
		{
			return m_Balance.getCredit();
		}
		return 0L;
	}

	/**
	 * Liefert die Kostenbestätigungen.
	 *
	 * @return Vector von CostConfirms
	 */
	public XMLCostConfirmations getCostConfirms()
	{
		return m_costConfirms;
	}

	public XMLBalance getBalance()
	{
		return m_Balance;
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
}
