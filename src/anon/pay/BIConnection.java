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

/**
 * Diese Klasse kapselt eine Verbindung zur BI, fuehrt die Authentikation durch
 * und enth\uFFFDlt Methoden, um Kontoaufladungs- und andere Requests an die BI zu
 * schicken.
 *
 * @author Grischan Glaenzel, Bastian Voigt, Tobias Bayer
 */
import java.net.Socket;

import org.w3c.dom.Document;

import anon.crypto.JAPSignature;
import anon.pay.xml.XMLAccountCertificate;
import anon.pay.xml.XMLAccountInfo;
import anon.pay.xml.XMLBalance;
import anon.pay.xml.XMLChallenge;
import anon.pay.xml.XMLErrorMessage;
import anon.pay.xml.XMLJapPublicKey;
import anon.pay.xml.XMLResponse;
import anon.pay.xml.XMLTransCert;
import anon.crypto.tinytls.TinyTLS;
import anon.util.XMLUtil;
import anon.crypto.XMLSignature;
import anon.pay.xml.XMLPaymentOptions;

public class BIConnection
{
	private BI m_theBI;

	private Socket m_socket;
	private HttpClient m_httpClient;

	/**
	 * Constructor
	 *
	 * @param BI the BI to which we connect
	 */
	public BIConnection(BI theBI)
	{
		m_theBI = theBI;
	}

	/**
	 * Baut eine TCP-Verbindung zur Bezahlinstanz auf und initialisiert den
	 * HttpClient.
	 *
	 * @throws Exception Wenn Fehler beim Verbindungsaufbau
	 */
	public void connect() throws Exception
	{
		TinyTLS tls = new TinyTLS(m_theBI.getHostName(), m_theBI.getPortNumber());
		tls.setSoTimeout(30000);
		tls.setRootKey(m_theBI.getCertificate().getPublicKey());
		tls.startHandshake();
		m_socket = tls;
		m_httpClient = new HttpClient(m_socket);
	}

	/**
	 * Schliesst die Verbindung zur Bezahlinstanz.
	 *
	 * @throws IOException Wenn Fehler beim Verbindungsabbau
	 */
	public void disconnect() throws Exception
	{
		m_httpClient.close();
	}

	/**
	 * Fetches a transfer certificate from the BI.
	 * @return XMLTransCert the transfer certificate
	 */
	public XMLTransCert charge() throws Exception
	{
		m_httpClient.writeRequest("GET", "charge", null);
		Document doc = m_httpClient.readAnswer();
		if (!XMLSignature.verifyFast(doc, m_theBI.getCertificate().getPublicKey()))
		{
			throw new Exception("The BI's signature under the transfer certificate is invalid");
		}

		return new XMLTransCert(doc);
	}

	/**
	 * Fetches an account statement (balance cert. + costconfirmations)
	 * from the BI.
	 * @return the statement in XMLAccountInfo format
	 * @throws IOException
	 */
	public XMLAccountInfo getAccountInfo() throws Exception
	{
		XMLAccountInfo info = null;
		m_httpClient.writeRequest("GET", "balance", null);
		Document doc = m_httpClient.readAnswer();
		info = new XMLAccountInfo(doc);
		XMLBalance bal = info.getBalance();
		if (m_theBI.getVerifier().verifyXML(XMLUtil.toXMLDocument(bal)) == false)
		{
			throw new Exception("The BI's signature under the balance certificate is Invalid!");
		}
		return info;
	}

	/**
	 * Fetches payment options
	 * @return XMLPaymentOptions
	 * @throws Exception
	 */
	public XMLPaymentOptions getPaymentOptions() throws Exception
	{
		XMLPaymentOptions options;
		m_httpClient.writeRequest("GET", "paymentoptions", null);
		Document doc = m_httpClient.readAnswer();
		options = new XMLPaymentOptions(doc);
		return options;
	}

	/** performs challenge-response authentication */
	public void authenticate(XMLAccountCertificate accountCert, JAPSignature signer) throws Exception
	{
		String StrAccountCert = XMLUtil.toString(XMLUtil.toXMLDocument(accountCert));
		m_httpClient.writeRequest("POST", "authenticate", StrAccountCert);
		Document doc = m_httpClient.readAnswer();
		String tagname = doc.getDocumentElement().getTagName();
		if (tagname.equals(XMLChallenge.XML_ELEMENT_NAME))
		{
			XMLChallenge xmlchallenge = new XMLChallenge(doc);
			byte[] challenge = xmlchallenge.getChallengeForSigning();
			byte[] response = signer.signBytes(challenge);
			XMLResponse xmlResponse = new XMLResponse(response);
			String strResponse = XMLUtil.toString(XMLUtil.toXMLDocument(xmlResponse));
			m_httpClient.writeRequest("POST", "response", strResponse);
			doc = m_httpClient.readAnswer();
		}
		else if (tagname.equals(XMLErrorMessage.XML_ELEMENT_NAME))
		{
			/** @todo handle errormessage properly */
			throw new Exception("The BI sent an errormessage: " +
								new XMLErrorMessage(doc).getErrorDescription());
		}
	}

	/**
	 * Registers a new account using the specified keypair.
	 * Checks the signature and the public key of the accountCertificate
	 * that is received.
	 *
	 * @param pubKey public key
	 * @param privKey private key
	 * @return XMLAccountCertificate the certificate issued by the BI
	 * @throws Exception if an error occurs or the signature or public key is wrong
	 */
	public XMLAccountCertificate register(XMLJapPublicKey pubKey, JAPSignature signKey) throws Exception
	{
		// send our public key
		m_httpClient.writeRequest(
			"POST", "register",
			XMLUtil.toString(XMLUtil.toXMLDocument(pubKey))
			);
		Document doc = m_httpClient.readAnswer();
		String tagname = doc.getDocumentElement().getTagName();

		// perform challenge-response authentication
		XMLAccountCertificate xmlCert = null;
		if (tagname.equals(XMLChallenge.XML_ELEMENT_NAME))
		{
			XMLChallenge xmlchallenge = new XMLChallenge(doc);
			byte[] challenge = xmlchallenge.getChallengeForSigning();
			byte[] response = signKey.signBytes(challenge);
			XMLResponse xmlResponse = new XMLResponse(response);
			String strResponse = XMLUtil.toString(XMLUtil.toXMLDocument(xmlResponse));
			m_httpClient.writeRequest("POST", "response", strResponse);
			doc = m_httpClient.readAnswer();
			// check signature
			if (!XMLSignature.verifyFast(doc, m_theBI.getCertificate().getPublicKey()))
			{
				throw new Exception("AccountCertificate: Wrong signature!");
			}
			xmlCert = new XMLAccountCertificate(doc.getDocumentElement());
			if (!xmlCert.getPublicKey().equals(pubKey.getPublicKey()))
			{
				throw new Exception(
					"The JPI is evil (sent a valid certificate, but with a wrong publickey)");

			}
		}
		else if (tagname.equals(XMLErrorMessage.XML_ELEMENT_NAME))
		{
			throw new Exception("The BI sent an errormessage: " +
								new XMLErrorMessage(doc).getErrorDescription() +
								" Authentication failed.");
		}
		return xmlCert;
	}

	public XMLPaymentOptions fetchPaymentOptions() throws Exception
	{
		m_httpClient.writeRequest("GET", "paymentoptions", null);
		Document doc = m_httpClient.readAnswer();
		XMLPaymentOptions paymentoptions = new XMLPaymentOptions(doc.getDocumentElement());
		return paymentoptions;
	}
}
