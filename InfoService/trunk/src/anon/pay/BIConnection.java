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
 * This class encapsulates a connection to the Payment Instance, performs authentication
 * and contains other methods for interaction with the Payment Instance.
 *
 * @author Grischan Glaenzel, Bastian Voigt, Tobias Bayer
 */
import java.net.Socket;
import java.util.Date;
import java.util.Vector;

import org.w3c.dom.Document;
import anon.crypto.JAPSignature;
import anon.crypto.XMLSignature;
import anon.crypto.tinytls.TinyTLS;
import anon.pay.xml.XMLAccountCertificate;
import anon.pay.xml.XMLAccountInfo;
import anon.pay.xml.XMLBalance;
import anon.pay.xml.XMLChallenge;
import anon.pay.xml.XMLErrorMessage;
import anon.pay.xml.XMLJapPublicKey;
import anon.pay.xml.XMLPassivePayment;
import anon.pay.xml.XMLPaymentOptions;
import anon.pay.xml.XMLResponse;
import anon.pay.xml.XMLTransCert;
import anon.pay.xml.XMLTransactionOverview;
import anon.util.XMLUtil;
import anon.util.captcha.ICaptchaSender;
import anon.util.captcha.IImageEncodedCaptcha;
import anon.util.captcha.ZipBinaryImageCaptchaClient;
import anon.infoservice.ImmutableProxyInterface;

public class BIConnection implements ICaptchaSender
{
	private BI m_theBI;

	private Socket m_socket;
	private HttpClient m_httpClient;

	private Vector m_biConnectionListeners;

	private byte[] m_captchaSolution;

	private boolean m_bSendNewCaptcha;
	private boolean m_bFirstCaptcha = true;

	ImmutableProxyInterface m_proxy = null;

	/**
	 * Constructor
	 *
	 * @param BI the BI to which we connect
	 */
	public BIConnection(BI theBI)
	{
		m_theBI = theBI;
		m_biConnectionListeners = new Vector();
	}

	/**
	 * Connects to the Payment Instance via TCP and inits the HttpClient.
	 *
	 * @throws Exception Wenn Fehler beim Verbindungsaufbau
	 */
	public void connect(ImmutableProxyInterface a_proxy) throws Exception
	{
		TinyTLS tls;
		m_proxy = a_proxy;
		if (a_proxy == null)
		{
			tls = new TinyTLS(m_theBI.getHostName(), m_theBI.getPortNumber());
		}
		else
		{
			tls = new TinyTLS(m_theBI.getHostName(), m_theBI.getPortNumber(), a_proxy);
		}
		tls.setSoTimeout(30000);
		tls.setRootKey(m_theBI.getCertificate().getPublicKey());
		tls.startHandshake();
		m_socket = tls;
		m_httpClient = new HttpClient(m_socket);
	}

	/**
	 * Closes the connection.
	 *
	 * @throws IOException
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

		XMLTransCert cert = new XMLTransCert(doc);
		cert.setReceivedDate(new Date());
		return cert;
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
	 * Fetches payment options.
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
		Document doc;
		m_bSendNewCaptcha = true;
		while (m_bSendNewCaptcha)
		{
			if (!m_bFirstCaptcha)
			{
				this.disconnect();
				this.connect(m_proxy);
			}
			// send our public key
			m_httpClient.writeRequest(
				"POST", "register",
				XMLUtil.toString(XMLUtil.toXMLDocument(pubKey))
				);
			doc = m_httpClient.readAnswer();

			//Answer document should contain a captcha, let the user solve it and extract the XMLChallenge
			IImageEncodedCaptcha captcha = new ZipBinaryImageCaptchaClient(doc.getDocumentElement());
			m_bSendNewCaptcha = false;
			fireGotCaptcha(captcha);
		}
		/** Cut off everything beyond the last ">" to extract only the XML challenge
		 *  without the cipher padding.
		 */
		if (m_captchaSolution != null)
		{
			String challengeString = new String(m_captchaSolution);
			int pos = challengeString.lastIndexOf(">");
			challengeString = challengeString.substring(0, pos + 1);
			Document challengeDoc = XMLUtil.toXMLDocument(challengeString);
			XMLChallenge xmlchallenge = new XMLChallenge(challengeDoc);

			// perform challenge-response authentication
			XMLAccountCertificate xmlCert = null;

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
			return xmlCert;
		}
		else
		{
			throw new Exception("CAPTCHA");
		}
	}

	/**
	 * Gets the payment options the PI provides.
	 * @return XMLPaymentOptions
	 * @throws Exception
	 */
	public XMLPaymentOptions fetchPaymentOptions() throws Exception
	{
		m_httpClient.writeRequest("GET", "paymentoptions", null);
		Document doc = m_httpClient.readAnswer();
		XMLPaymentOptions paymentoptions = new XMLPaymentOptions(doc.getDocumentElement());
		return paymentoptions;
	}

	/**
	 * Asks the PI to fill an XMLTransactionOverview
	 * @param a_overview XMLTransactionOverview
	 * @return XMLTransactionOverview
	 * @throws Exception
	 */
	public XMLTransactionOverview fetchTransactionOverview(XMLTransactionOverview a_overview) throws
		Exception
	{
		m_httpClient.writeRequest("POST", "transactionoverview",
								  XMLUtil.toString(a_overview.toXmlElement(XMLUtil.createDocument())));
		Document doc = m_httpClient.readAnswer();
		XMLTransactionOverview overview = new XMLTransactionOverview(doc.getDocumentElement());
		return overview;
	}

	/**
	 * Sends data the user has entered for a passive payment to the payment
	 * instance.
	 * @param a_passivePayment XMLPassivePayment
	 * @throws Exception
	 */
	public boolean sendPassivePayment(XMLPassivePayment a_passivePayment) throws Exception
	{
		m_httpClient.writeRequest("POST", "passivepayment",
								  XMLUtil.toString(a_passivePayment.toXmlElement(XMLUtil.createDocument()
			)));
		Document doc = m_httpClient.readAnswer();
		XMLErrorMessage err = new XMLErrorMessage(doc.getDocumentElement());
		if (err.getErrorCode() == XMLErrorMessage.ERR_OK)
		{
			return true;
		}
		else
		{
			return false;
		}
	}

	/**
	 * Adds an IBIConnectionListener
	 * @param a_listener IBIConnectionListener
	 */
	public void addConnectionListener(IBIConnectionListener a_listener)
	{
		if (!m_biConnectionListeners.contains(a_listener))
		{
			m_biConnectionListeners.addElement(a_listener);
		}
	}

	/**
	 * Signals a received captcha to all registered IBICOnnectionListeners.
	 * @param a_captcha IImageEncodedCaptcha
	 */
	private void fireGotCaptcha(IImageEncodedCaptcha a_captcha)
	{
		for (int i = 0; i < m_biConnectionListeners.size(); i++)
		{
			( (IBIConnectionListener) m_biConnectionListeners.elementAt(i)).gotCaptcha(this, a_captcha);
		}
	}

	/**
	 * Sets the solution of a captcha for registering an account.
	 * @param a_solution byte[]
	 */
	public void setCaptchaSolution(byte[] a_solution)
	{
		m_captchaSolution = a_solution;
	}

	public void getNewCaptcha()
	{
		m_bSendNewCaptcha = true;
		m_bFirstCaptcha = false;
	}

}
