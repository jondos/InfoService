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

//import pay.crypto.tinyssl.RootCertificates;


/**
 * Diese Klasse kapselt eine Verbindung zur BI, fuehrt die Authentikation durch
 * und enth\uFFFDlt Methoden, um Kontoaufladungs- und andere Requests an die BI zu
 * schicken.
 *
 * @author Grischan Glaenzel, Bastian Voigt
 */
import java.io.IOException;
import java.net.Socket;

import anon.crypto.JAPSignature;
import anon.pay.xml.XMLAccountCertificate;
import anon.pay.xml.XMLAccountInfo;
import anon.pay.xml.XMLBalance;
import anon.pay.xml.XMLChallenge;
import anon.pay.xml.XMLJapPublicKey;
import anon.pay.xml.XMLTransCert;
import anon.util.XMLUtil;
import logging.*;
import anon.tor.tinytls.*;
import anon.pay.xml.*;


public class BIConnection
{
	private BI m_theBI;

	private Socket m_socket;
	private HttpClient m_httpClient;
	private boolean m_bIsSslOn;

	/**
	 * Constructor
	 *
	 * @param BI the BI to which we connect
	 */
	public BIConnection(BI theBI, boolean sslOn)
	{
		m_theBI = theBI;
		m_bIsSslOn = sslOn;
	}

	/**
	 * Baut eine TCP-Verbindung zur Bezahlinstanz auf und initialisiert den
	 * HttpClient.
	 *
	 * @throws Exception Wenn Fehler beim Verbindungsaufbau
	 */
	public void connect() throws Exception
	{
		if (m_bIsSslOn == false)
		{
			m_socket = new Socket(m_theBI.getHostName(), m_theBI.getPortNumber());
		}
		else
		{
			TinyTLS tls = new TinyTLS(m_theBI.getHostName(), m_theBI.getPortNumber());
			tls.setRootKey(m_theBI.getCertificate());
			tls.startHandshake();
			m_socket = tls;
		}
		m_httpClient = new HttpClient(m_socket);
	}

	/**
	 * Schliesst die Verbindung zur Bezahlinstanz.
	 *
	 * @throws IOException Wenn Fehler beim Verbindungsabbau
	 */
	public void disconnect() throws IOException
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
		String answer = m_httpClient.readAnswer();
		XMLTransCert xmltrcert = new XMLTransCert(answer);
		if (xmltrcert.verifySignature(m_theBI.getVerifier())==true)
		{
			return xmltrcert;
		}
		else
		{
			throw new Exception("The BI's signature under the transfer certificate is invalid");
		}
	}

	/**
	 * Fetches an account statement (balance cert. + costconfirmations)
	 * from the BI.
	 * @return the statement in XMLAccountInfo format
	 * @throws IOException
	 */
	public XMLAccountInfo getAccountInfo() throws Exception
	{
		String answer;
		XMLAccountInfo info = null;
		m_httpClient.writeRequest("GET", "balance", null);
		answer = m_httpClient.readAnswer();
		LogHolder.log(LogLevel.DEBUG, LogType.PAY, "Received answer: " + answer);
		info = new XMLAccountInfo(answer);
		XMLBalance bal = info.getBalance();
		if (m_theBI.getVerifier().verifyXML(XMLUtil.toXMLDocument(bal)) == false)
		{
			throw new Exception("The BI's signature under the balance certificate is Invalid!");
		}
		return info;
	}

	/** performs challenge-response authentication */
	public void authenticate(XMLAccountCertificate accountCert, JAPSignature signer) throws Exception
	{
		if (accountCert.isSigned() == false)
		{
			throw new Exception("BIConnection.authenticate: Your account certificate is not signed!");
		}

		String StrAccountCert = XMLUtil.toString(XMLUtil.toXMLDocument(accountCert));
		m_httpClient.writeRequest("POST", "authenticate", StrAccountCert);
		String answer = m_httpClient.readAnswer();

		XMLChallenge xmlchallenge = new XMLChallenge(answer);
		byte[] challenge = xmlchallenge.getChallengeForSigning();
		byte[] response = signer.signBytes(challenge);
		XMLResponse xmlResponse = new XMLResponse(response);
		String strResponse = XMLUtil.toString(XMLUtil.toXMLDocument(xmlResponse));

		m_httpClient.writeRequest("POST", "response", strResponse);
		answer = m_httpClient.readAnswer();
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
		String answer = m_httpClient.readAnswer();

		// perform challenge-response authentication
		XMLChallenge xmlchallenge = new XMLChallenge(answer);
		byte[] challenge = xmlchallenge.getChallengeForSigning();
		byte[] response = signKey.signBytes(challenge);
		XMLResponse xmlResponse = new XMLResponse(response);
		String strResponse = XMLUtil.toString(XMLUtil.toXMLDocument(xmlResponse));
		m_httpClient.writeRequest("POST", "response", strResponse);
		answer = m_httpClient.readAnswer();
		LogHolder.log(LogLevel.DEBUG, LogType.PAY, "Received answer: " + answer);
		XMLAccountCertificate xmlCert = new XMLAccountCertificate(answer);

		// check signature
		if (!xmlCert.verifySignature(m_theBI.getVerifier()))
		{
			throw new Exception("AccountCertificate: Wrong signature!");
		}
		if (!xmlCert.getPublicKey().equals(pubKey.getPublicKey()))
		{
			throw new Exception(
				"The JPI is evil (sent a valid certificate, but with a wrong publickey)");

		}
		return xmlCert;
	}
}
