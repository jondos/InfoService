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
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import pay.crypto.tinyssl.TinySSL;
import pay.util.HttpClient;
import payxml.XMLAccountCertificate;
import payxml.XMLAccountInfo;
import payxml.XMLChallenge;
import payxml.XMLJapPublicKey;
import payxml.XMLResponse;
import payxml.XMLTransCert;
import java.security.PublicKey;
import anon.crypto.IMyPublicKey;
import anon.util.XMLUtil;
import anon.crypto.MyDSAPublicKey;
import anon.crypto.MyRSAPublicKey;
import payxml.XMLBalance;

public class BIConnection
{
	private String m_BIHostName;
	private int m_BIUserPort;
	private Socket m_socket;
	private HttpClient m_httpClient;
	private boolean m_bIsSslOn;

	/*	private XMLAccountCertificate m_accountCert;
	 private MyRSAPrivateKey m_privateKey;*/


	/**
	 * Konstruktor
	 *
	 * @param host Hostname der Bezahlinstanz
	 * @param port Port der Bezahlinstanz
	 * @param privateKey Private Key f\uFFFDr die Challenge-Response-Authentikation
	 * @param accountCert Kontozertifikat f\uFFFDr die Anmeldung an der BI
	 * @param sslOn SSL ein?
	 */
	public BIConnection(String biHostname,
						int biPort,
						boolean sslOn
						/*						XMLAccountCertificate accountCert,
						 MyRSAPrivateKey privKey*/
						)
	{
		m_BIHostName = biHostname;
		m_BIUserPort = biPort;
		m_bIsSslOn = sslOn;
		/*		m_accountCert = accountCert;
		  m_privateKey =  privKey;*/
	}

	/**
	 * Baut eine TCP-Verbindung zur Bezahlinstanz auf und initialisiert den
	 * HttpClient.
	 *
	 * @throws Exception Wenn Fehler beim Verbindungsaufbau
	 */
	public void connect() throws Exception
	{
		LogHolder.log(
			LogLevel.DEBUG, LogType.PAY,
			"BIConnection.connect() .. connecting to " + m_BIHostName + ":" + m_BIUserPort
			);
		try
		{
			if (m_bIsSslOn == false)
			{
				m_socket = new Socket(m_BIHostName, m_BIUserPort);
			}
			else
			{
				m_socket = new TinySSL(m_BIHostName, m_BIUserPort);
			}
			m_httpClient = new HttpClient(m_socket);
		}
		catch (Exception ex)
		{
			throw new Exception(
				"Could not connect to BI at " + m_BIHostName + ":" + m_BIUserPort +
				" (" + ex.getMessage() + ")");
		}
	}

	/**
	 * Schliesst die Verbindung zur Bezahlinstanz.
	 *
	 * @throws IOException Wenn Fehler beim Verbindungsabbau
	 */
	public void disconnect() throws IOException
	{
		LogHolder.log(LogLevel.DEBUG, LogType.PAY,
					  "PayInstance.disconnect() .. closing http connection"
					  );
		m_httpClient.close();
	}

	/**
	 * Fordert eine Transaktionsnummer bei der BI an.
	 * Fuehrt erst die Challenge-Response-Authentikation durch und
	 * fordert dann ein Transaktionsnummer-Zertifikat bei der BI an.
	 * Die Signatur wird gepr\uFFFDft und bei ung\uFFFDltiger Signatur wird eine
	 * Exception geworfen.
	 * Vorher muss {@link connect()} aufgerufen werden.
	 *
	 * @param accountcert XMLAccountCertificate Kontozertifikat f\uFFFDr die Anmeldung
	 * @param privKey MyRSAPrivateKey PrivateKey f\uFFFDr Challenge-Response
	 * @throws Exception bei ung\uFFFDltiger Signatur oder anderen IO-Fehlern
	 * @return XMLTransCert das Zertifikat mit der neuen Transaktionsnummer
	 */
	public XMLTransCert charge() throws Exception
	{
		m_httpClient.writeRequest("GET", "charge", null);
		String answer = m_httpClient.readAnswer();
		XMLTransCert xmltrcert = new XMLTransCert(answer);
		if (Pay.getInstance().getVerifyingInstance().verifyXML(xmltrcert.getDomDocument()))
		{
			return xmltrcert;
		}
		else
		{
			throw new Exception("invalid signature");
		}
	}

	/**
	 * Fordert einen Kontoauszug bei der BI an.
	 *
	 *
	 * @param accountcert Kontozertifikat
	 * @param privKey geheimer Schl?ssel des Kontos
	 * @return Guthaben und Kostenbest?tigungen (in XMLBalConf gekapselt)
	 * @throws IOException
	 */
	public XMLAccountInfo getAccountInfo() throws Exception
	{
		String answer;
		XMLAccountInfo info = null;
		m_httpClient.writeRequest("GET", "balance", null);
		answer = m_httpClient.readAnswer();
		info = new XMLAccountInfo(answer);
		XMLBalance bal = info.getBalance();
		if(Pay.getInstance().getVerifyingInstance().verifyXML(bal.getXmlEncoded())==false)
			throw new Exception("Invalid Signature");
		return info;
	}

	public void authenticate(XMLAccountCertificate accountCert, JAPSignature signer) throws Exception
	{
		if(accountCert.isSigned()==false)
		{
			throw new Exception("BIConnection.authenticate: Your account certificate is not signed!");
		}
		String answer = null;
		try
		{
			String StrAccountCert = XMLUtil.XMLDocumentToString(accountCert.getXmlEncoded());
			m_httpClient.writeRequest("POST", "authenticate", StrAccountCert);
			answer = m_httpClient.readAnswer();
		}
		catch (IOException ex)
		{
			throw new Exception("Error in http communication: " + ex.getMessage());
		}
		XMLChallenge xmlchallenge = new XMLChallenge(answer);
		byte[] challenge = xmlchallenge.getChallengeForSigning();
		byte[] response = signer.signBytes(challenge);
		XMLResponse xmlResponse = new XMLResponse(response);
		String strResponse = xmlResponse.getXMLString();

		try
		{
			m_httpClient.writeRequest("POST", "response", strResponse);
			answer = m_httpClient.readAnswer();
		}
		catch (IOException ex1)
		{
			throw new Exception("Error in http communication: " + ex1.getMessage());
		}
	}

	/*
	 public AICert connectAI(long accountNumber,RSAKeyParameters pubKey) throws Exception
	  {
	  try{
	  AIHello xmlhello = new AIHello(accountNumber, new XMLJapPublicKey(pubKey));
	   //PrintWriter pw = new PrintWriter(socket.getOutputStream());
	 //pw.print(pay.test.PayAITest.head+"hello"); pw.flush();
	  //httpClient.writeRequest("GET","hello","");//xmlhello.getXMLString(true));
	  //String answer = httpClient.readAnswer();
	  }
	  catch(Exception ex){
	   System.out.println("PayInstance: Fehler in connectAI");
	  }

	 XMLChallenge xmlchallenge=new XMLChallenge(answer);
	 byte[] challenge = xmlchallenge.getChallenge();

	 Signer signer = new Signer();
	 signer.init(true, privKey);
	 signer.update(challenge);
	 byte[] sig = signer.generateSignature();

	 XMLResponse xmlResponse = new XMLResponse(sig);
	 String response = xmlResponse.getXMLString(true);

	 httpClient.writeRequest("POST","response",response);
	 answer = httpClient.readAnswer();

	 RootCertificates rootCerts = new RootCertificates();
	 rootCerts.init();
	 RSAKeyParameters testkey = rootCerts.getPublicKey("test server");

	 XMLCertificate xmlCert = new XMLCertificate(answer);
	 XMLSignature xmlSig = new XMLSignature(answer.getBytes());
	 xmlSig.initVerify(testkey);
	 if (xmlSig.verifyXML()&&
	  xmlCert.getPublicKey().getModulus().equals(pubKey.getModulus())&&
	  xmlCert.getPublicKey().getPublicExponent().equals(pubKey.getExponent()))
	 {
	   return xmlCert;
	 }
	 else throw new Exception("wrong signatur on accountcertificate or wrong key");
	  return new AICert();
	  }
	 */

	/**
	 * Registers a new account using the specified keypair.
	 *
	 * @param pubKey public key
	 * @param privKey private key
	 * @return XMLAccountCertificate the certificate issued by the BI
	 * @throws Exception
	 */
	public XMLAccountCertificate register(XMLJapPublicKey pubKey, JAPSignature signKey) throws Exception
	{
		// send our public key
		m_httpClient.writeRequest(
			"POST", "register",
			XMLUtil.XMLDocumentToString(pubKey.getXmlDocument())
			);
		String answer = m_httpClient.readAnswer();

		// perform challenge-response authentication
		XMLChallenge xmlchallenge = new XMLChallenge(answer);
		byte[] challenge = xmlchallenge.getChallengeForSigning();
		byte[] response = signKey.signBytes(challenge);
		XMLResponse xmlResponse = new XMLResponse(response);
		String strResponse = xmlResponse.getXMLString();
		m_httpClient.writeRequest("POST", "response", strResponse);
		answer = m_httpClient.readAnswer();
		LogHolder.log(LogLevel.DEBUG, LogType.PAY, "Received cert: "+answer);

		// check signature
		boolean sigOK = Pay.getInstance().getVerifyingInstance().verifyXML(
			new java.io.ByteArrayInputStream(answer.getBytes())
			);

		XMLAccountCertificate xmlCert = new XMLAccountCertificate(answer);
		// check if the certificate signed by the BI contains the right
		// public key
		if (xmlCert.getPublicKey().equals(pubKey.getPublicKey()))
		{
			return xmlCert;
		}
		else
		{
			throw new Exception(
				"The JPI is evil (sent a valid certificate, but with a wrong publickey)");

		}
	}
}
