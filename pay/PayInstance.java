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
import java.net.Socket;
import java.util.Date;
import org.bouncycastle.crypto.params.RSAKeyParameters;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import pay.crypto.tinyssl.RootCertificates;
import pay.crypto.tinyssl.TinySSL;
import pay.util.HttpClient;
import pay.util.PayText;
import payxml.XMLBalConf;
import payxml.XMLAccountCertificate;
import payxml.XMLChallenge;
import payxml.XMLJapPublicKey;
import payxml.XMLResponse;
import payxml.XMLTransCert;
import payxml.util.Signer;

import anon.crypto.JAPSignature;
import anon.crypto.*;

/**
 * Hauptklasse für die Verbindung von Pay zur BI kümmert sich inhaltlich um die Kommunikation
 * also In welcher Reihenfolge Die Challenge-Response abläuft etc.
 * @author Grischan Glänzel
 */

public class PayInstance
{
	private String host;
	private int port;
	private Socket socket;
	private HttpClient httpClient;
	private boolean sslOn;

	/**
	 * Konstruktor.
	 *
	 * @param host Hostname der Bezahlinstanz
	 * @param port Port der Bezahlinstanz
	 * @param sslOn SSL ein?
	 */
	public PayInstance(String host, int port, boolean sslOn)
	{
		LogHolder.log(LogLevel.DEBUG, LogType.PAY,
				"PayInstance() initializing"
			);

		this.host = host;
		this.port = port;
		this.sslOn = sslOn;
	}

	/**
	 * Baut eine Verbindung zur Bezahlinstanz auf.
	 *
	 * @throws IOException Wenn Fehler beim Verbindungsaufbau
	 */
	public void connect() throws IOException
	{
		LogHolder.log(LogLevel.DEBUG, LogType.PAY,
				"PayInstance.connect() .. connecting to "+host+":"+port
			);
		try
		{
			if (sslOn == false)
			{
				socket = new Socket(host, port);
			}
			else
			{
				socket = new TinySSL(host, port);
			}
			httpClient = new HttpClient(socket);
		}
		catch (Exception ex)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.PAY, PayText.get("piServerError"));
			ex.printStackTrace();
		}
	}

	/**
	 * Schließt die Verbindung zur Bezahlinstanz.
	 *
	 * @throws IOException Wenn Fehler beim Verbindungsabbau
	 */
	public void disconnect() throws IOException
	{
		LogHolder.log(LogLevel.DEBUG, LogType.PAY,
				"PayInstance.disconnect() .. closing http connection"
			);
		httpClient.close();
	}

	/**
	 * Aufladen des Kontos mit einer Geldüberweisung.
	 *
	 * @param accountcert Kontozertifikat
	 * @param privKey geheimer Schlüssel des Kontos
	 * @return Transfer-Zertifikat
	 * @throws IOException
	 */
	public XMLTransCert chargeBankTransfer(XMLAccountCertificate accountcert, MyRSAPrivateKey privKey) throws IOException
	{
		String type = "<ChargeMethod>Banktransfer</ChargeMethod>";
		return charge(accountcert, privKey, type);
	}

	/**
	 * Aufladen des Kontos mit Kreditkarte.
	 *
	 * @param accountcert Kontozertifikat
	 * @param privKey geheimer Schlüssel
	 * @param amount Geldbetrag
	 * @param number Kreditkartennummer
	 * @param valid Gültigkeitsdatum der Kreditkarte
	 * @return Transfer-Zertifikat
	 * @throws IOException
	 */
/*	public XMLTransCert chargeCreditCard(XMLAccountCertificate accountcert, RSAKeyParameters privKey, int amount,
										 String number, Date valid) throws IOException
	{
		String type = "<ChargeMethod>Creditcard<CreditCard><Number>" +
			number + "</Number><ValidDate>" + valid + "</ValidDate>" +
			"</CreditCard><Amount>" + amount + "</Amount></ChargeMethod>";
		return charge(accountcert, privKey, type);
	}
*/
	private XMLTransCert charge(XMLAccountCertificate accountcert, MyRSAPrivateKey privKey, String type) throws IOException
	{
		try
		{
			authenticate(accountcert, privKey);
			httpClient.writeRequest("POST", "charge", type);
			String answer = httpClient.readAnswer();

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
		catch (Exception e)
		{
			throw new IOException(e.getMessage());
		}
	}

	/**
	 * Liefert den Kontostand zum angebenen  Konto.
	 *
	 * @param accountcert Kontozertifikat
	 * @param privKey geheimer Schlüssel des Kontos
	 * @return Guthaben und Kostenbestätigungen (in XMLBalConf gekapselt)
	 * @throws IOException
	 */
	public XMLBalConf getBalance(XMLAccountCertificate accountcert, MyRSAPrivateKey privKey) throws IOException
	{
		//XMLBalance xmlBalance;
		//XMLCostConfirmation[] xmlConfirms;
		String answer;
		XMLBalConf conf = null;
		try
		{
			authenticate(accountcert, privKey);
			httpClient.writeRequest("GET", "balance", null);
			answer = httpClient.readAnswer();
			conf = new XMLBalConf(answer);


			if (!Pay.getInstance().getVerifyingInstance().verifyXML(conf.balance.getDomDocument()))
			{
				throw new Exception("invalid signature");
			}
		}
		catch (Exception e)
		{
			throw new IOException(e.getMessage());
		}
		return conf;
	}

	private void authenticate(XMLAccountCertificate accountcert, MyRSAPrivateKey privKey) throws Exception
	{
		try
		{
			//Log.log(this,accountcert,Log.TEST);
			httpClient.writeRequest("POST", "authenticate", accountcert.getXMLString());
			String answer = httpClient.readAnswer();
			XMLChallenge xmlchallenge = new XMLChallenge(answer);
			byte[] challenge = xmlchallenge.getChallengeForSigning();

			Signer signer = new Signer();
			signer.init(true, privKey.getParams());
			signer.update(challenge);
			byte[] sig = signer.generateSignature();

			XMLResponse xmlResponse = new XMLResponse(sig);
			String response = xmlResponse.getXMLString();

			httpClient.writeRequest("POST", "response", response);
			answer = httpClient.readAnswer();
		}
		catch (Exception e)
		{
			e.printStackTrace();
			throw e;
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
	 * Eröffnet ein neues Konto bei der Bezahlinstanz.
	 *
	 * @param pubKey öffentlicher Schlüssel
	 * @param privKey geheimer Schlüssel
	 * @return Kontozertifikat
	 * @throws Exception
	 */
	public XMLAccountCertificate register(RSAKeyParameters pubKey, MyRSAPrivateKey privKey) throws Exception
	{
		XMLJapPublicKey xmlPubKey = new XMLJapPublicKey(new MyRSAPublicKey(pubKey));
		String xmlkey = xmlPubKey.getXMLString();
		if (xmlkey == null)
		{
			return null;
		}

		httpClient.writeRequest("POST", "register", xmlkey);
		String answer = httpClient.readAnswer();

		XMLChallenge xmlchallenge = new XMLChallenge(answer);
		byte[] challenge = xmlchallenge.getChallengeForSigning();

		Signer signer = new Signer();
		signer.init(true, privKey.getParams());
		signer.update(challenge);
		byte[] sig = signer.generateSignature();

		XMLResponse xmlResponse = new XMLResponse(sig);
		String response = xmlResponse.getXMLString();

		httpClient.writeRequest("POST", "response", response);
		answer = httpClient.readAnswer();

		RootCertificates rootCerts = new RootCertificates();
		rootCerts.init();
		RSAKeyParameters testkey = rootCerts.getPublicKey("test server");

		LogHolder.log(LogLevel.DEBUG, LogType.PAY, answer);

		boolean sigOK = Pay.getInstance().getVerifyingInstance().verifyXML(
				new java.io.ByteArrayInputStream(answer.getBytes())
			);
		XMLAccountCertificate xmlCert = new XMLAccountCertificate(answer);
		boolean modOK = xmlCert.getPublicKey().getModulus().equals(pubKey.getModulus());
		boolean expOK = xmlCert.getPublicKey().getPublicExponent().equals(pubKey.getExponent());
		if(sigOK && modOK && expOK) return xmlCert;
		else throw new Exception("wrong signature on accountCertificate or wrong key");
	}
}
