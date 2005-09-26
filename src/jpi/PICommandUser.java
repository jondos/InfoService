/**********************************************************************
 This file is part of the Java Payment instance (JPI)
 Anonymity and Privacy on the Internet

 http://anon.inf.tu-dresden.de

 **********************************************************************/

package jpi;

import java.io.*;
import java.security.*;
import java.util.*;

import anon.crypto.*;
import anon.pay.xml.*;
import anon.util.*;
import jpi.db.*;
import logging.*;

/**
 * This class contains the functionality for talking to a JAP. For
 * each JAP connecting to us, one {@link PICommandUser} instance is
 * created to answer this user's requests
 */
public class PICommandUser implements PICommand
{
	private int m_iCurrentState;
	private byte[] m_arbChallenge;
	private Random m_SecureRandom;
	private DBInterface m_Database;

	private XMLAccountCertificate m_accountCertificate;
	private IMyPublicKey m_publicKey;

	// constants for the connection state
	static final int STATE_INIT = 0; // connection just initiated
	static final int STATE_REG_SEND = 10; // ?
	static final int STATE_REG_CHA_SENT = 12; // waiting for response to challenge from user
	static final int STATE_AUTH_SEND = 20; // ?
	static final int STATE_AUTH_CHA_SENT = 22; // waiting for response to challenge from user
	static final int STATE_AUTH_OK = 24; // login procedure done, auth ok

	/**
	 * Erzeugt und initialisiert ein {@link PICommandUser} Objekt.
	 *
	 */
	public PICommandUser()
	{
		init();
		m_SecureRandom = new Random(System.currentTimeMillis());
	}

	void init()
	{
		m_iCurrentState = STATE_INIT;
		m_arbChallenge = null;
		m_publicKey = null;
		m_accountCertificate = null;
	}

	/**
	 * Generates the appropriate answer for a http request. This is
	 * called by the {@link PIConnection} instance for each incoming
	 * request from a JAP.
	 *
	 * In the initial state, the user can authenticate or register a
	 * new account. After authentication is complete, the user can
	 * send one and only one @c /charge or @c /balance request. Then
	 * the connection is reset to initial state. Users can then
	 * authenticate again (e.g. using a different account num) or
	 * close the connection by sending a @c /close request.
	 *
	 * @return answer as {@link PIAnswer} object
	 */
	public PIAnswer next(PIRequest request)
	{
		PIAnswer reply = null;

		// open the DB
		try
		{
			m_Database = DBSupplier.getDataBase();
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.ALERT, LogType.PAY, "PICommandUser: could not get Database connection!");
			return new PIAnswer(
				PIAnswer.TYPE_CLOSE,
				new XMLErrorMessage(XMLErrorMessage.ERR_INTERNAL_SERVER_ERROR, "database problem")
				);
		}

		// send back a simple CloseAck. PIConnection will recognize
		// this special answer and will close the socket
		if (request.method.equals("GET") &&
			request.url.equals("/close"))
		{
			return new PIAnswer(PIAnswer.TYPE_CLOSE, new XMLCloseAck());
		}

		LogHolder.log(LogLevel.DEBUG, LogType.PAY,
					  "received " + request.method + " command from user: " + request.url);
		if (request.data != null && request.data.length > 0)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.PAY, "request data: " + new String(request.data));
		}
		switch (m_iCurrentState)
		{
			// Initial state: the ssl connection was just
			// established.  now JAP can either send a /register
			// request to register a new account or /auth to
			// authenticate with a public key that is known
			// already
			case STATE_INIT:

				// register a new account
				if (request.method.equals("POST") && request.url.equals("/register"))
				{
					XMLJapPublicKey keyParser = null;
					try
					{
						keyParser = new XMLJapPublicKey(request.data);
						m_publicKey = keyParser.getPublicKey();
					}
					catch (Exception e)
					{
						LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, e);
						reply = PIAnswer.getErrorAnswer(XMLErrorMessage.ERR_BAD_REQUEST,
							"Error while generating challenge: " + e.getMessage());
					}
					try
					{
						reply = new PIAnswer(PIAnswer.TYPE_CHALLENGE_REQUEST, getChallengeXML());
						m_iCurrentState = STATE_REG_CHA_SENT;
					}
					catch (Exception e)
					{
						LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, e);
						reply = PIAnswer.getErrorAnswer(XMLErrorMessage.ERR_INTERNAL_SERVER_ERROR);
					}
				}

				// authentication for a known account
				else if (request.method.equals("POST") && request.url.equals("/authenticate"))
				{
					try
					{
						testCertificate(request.data);
						reply = new PIAnswer(PIAnswer.TYPE_CHALLENGE_REQUEST,
											 getChallengeXML()); // send challenge for challenge-response authentication
						m_iCurrentState = STATE_AUTH_CHA_SENT;
					}
					catch (Exception e)
					{
						LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, e);
						reply = PIAnswer.getErrorAnswer(XMLErrorMessage.ERR_INTERNAL_SERVER_ERROR);
					}
				}
				break;

				// A new account was created and we now got
				// the response to our challenge.
			case STATE_REG_CHA_SENT:
				if (request.method.equals("POST") && request.url.equals("/response"))
				{
					if (!verifyResponse(request.data))
					{
						reply = PIAnswer.getErrorAnswer(XMLErrorMessage.ERR_BAD_SIGNATURE,
							"Invalid response to challenge");
					}
					else
					{
						try
						{
							// Response was correct. Send back the signed certificate
							reply = new PIAnswer(PIAnswer.TYPE_ACCOUNT_CERTIFICATE,
												 generateAccountCertificate()
								);
							init(); // registration complete, goto state init again, user can now authenticate
						}
						catch (Exception e)
						{
							LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, e);
							reply = PIAnswer.getErrorAnswer(XMLErrorMessage.ERR_INTERNAL_SERVER_ERROR);
						}
					}
				}
				break;

				// A user has authenticated with his certificate and
				// we now got the response to our challenge
			case STATE_AUTH_CHA_SENT:
				if (request.method.equals("POST") && request.url.equals("/response"))
				{
					if (!verifyResponse(request.data))
					{
						reply = PIAnswer.getErrorAnswer(XMLErrorMessage.ERR_BAD_SIGNATURE,
							"Invalid response to challenge");
					}
					else
					{
						reply = new PIAnswer(PIAnswer.TYPE_AUTHENTICATION_SUCCESS,
											 new XMLErrorMessage(XMLErrorMessage.ERR_OK));
						m_iCurrentState = STATE_AUTH_OK;
					}
				}
				break;

				// authenticaten is complete. The user can now send the real requests
			case STATE_AUTH_OK:

				// The user has requested a transfer number (TAN)
				if (request.method.equals("GET") && request.url.equals("/charge"))
				{
					try
					{
						reply = new PIAnswer(PIAnswer.TYPE_TRANSFER_CERTIFICATE,
											 getTransCert());
						// go to state init again, user can authenticate again with different account
						init();
					}
					catch (Exception e)
					{
						LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, e);
						reply = PIAnswer.getErrorAnswer(XMLErrorMessage.ERR_INTERNAL_SERVER_ERROR);
					}
				}

				else if (request.method.equals("GET") && request.url.equals("/balance"))
				{
					try
					{
						reply = new PIAnswer(PIAnswer.TYPE_BALANCE, getBalance());
						// go to state init again, user can authenticate again with different account
						init();
					}
					catch (Exception ex)
					{
						LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, ex);
						reply = PIAnswer.getErrorAnswer(XMLErrorMessage.ERR_INTERNAL_SERVER_ERROR);
					}
				}
				break;
		}
		if (reply == null)
		{
			reply = PIAnswer.getErrorAnswer(XMLErrorMessage.ERR_BAD_REQUEST);
		}
		return reply;
	}

	/**
	 * Generates a random challenge in {@link XMLChallenge} format.
	 * @return challenge XML data as array of bytes
	 */
	private IXMLEncodable getChallengeXML() throws Exception
	{
		m_arbChallenge = new byte[222];
		m_SecureRandom.nextBytes(m_arbChallenge);
		XMLChallenge xmlChallenge = new XMLChallenge(m_arbChallenge);
		String chStr = XMLUtil.toString(XMLUtil.toXMLDocument(xmlChallenge));
		m_arbChallenge = xmlChallenge.getChallengeForSigning();
		return xmlChallenge;
	}

	/**
	 * Verifies the response from the challenge-response authentication
	 * @retval true if the response is correct
	 * @retval false if the response is invalid
	 */
	boolean verifyResponse(byte[] data)
	{
		try
		{
			XMLResponse response = new XMLResponse(new String(data));
			JAPSignature sigTester = new JAPSignature();
			sigTester.initVerify(m_publicKey);
			boolean b = sigTester.verify(m_arbChallenge, response.getResponse());
			return b;
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, e);
			return false;
		}
	}

	/**
	 * generates a new
	 * certificate and stores it in the DB.
	 * @return certificate as {@link XMLAccountCertificate}
	 */
	private IXMLEncodable generateAccountCertificate() throws Exception
	{
		String strCert = null;
		long accountNum = m_Database.getNextAccountNumber();
		LogHolder.log(LogLevel.DEBUG, LogType.PAY, "generateCertificate(): new accountnr is: " + accountNum);
		java.sql.Timestamp creationTime = new java.sql.Timestamp(System.currentTimeMillis());

		XMLAccountCertificate xmlcert = new XMLAccountCertificate(
			m_publicKey, accountNum, creationTime,
			Configuration.getBiID());

		xmlcert.sign(Configuration.getPrivateKey());

		XMLJapPublicKey keyFormatter = new XMLJapPublicKey(m_publicKey);
		String strXmlKey = XMLUtil.toString(XMLUtil.toXMLDocument(keyFormatter));

		// debugging
		strCert = XMLUtil.toString(XMLUtil.toXMLDocument(xmlcert));
		m_Database.addAccount(accountNum, strXmlKey, creationTime,
							  strCert);
		return xmlcert;
	}

	/**
	 * Generates a tranfer certificate (TAN) as answer to a /charge
	 * request.  Stores the transfer number and the corresponding
	 * account no. in the db
	 * @param data request xml data, not used in this version
	 * @return transfer certificate xml data
	 * @todo Pruefen ob noch eine offen transaktion l\uFFFDuft --> wenn ja certificat nochmal senden
	 */
	private IXMLEncodable getTransCert() throws Exception
	{
		long accountNum = m_accountCertificate.getAccountNumber();
		Balance bal = m_Database.getBalance(accountNum);
		LogHolder.log(LogLevel.DEBUG, LogType.MISC, "Got balance. deposit=" + bal.deposit);

		// generate a transfer number (TAN)
		long transNumber = m_Database.getNextTransferNumber();
		LogHolder.log(LogLevel.DEBUG, LogType.MISC, "Transnumber=" + bal.deposit);

		// transfer cert is valid for 30 days
		long time = System.currentTimeMillis() +
			30l * 24l * 60l * 60l * 1000l;
		java.sql.Timestamp validto = new java.sql.Timestamp(time);

		// store transfer number in DB
		m_Database.storeTransferNumber(transNumber, accountNum, bal.deposit, validto);

		// generate xml tranfer cert and sign it
		XMLTransCert xmlcert =
			new XMLTransCert(accountNum, transNumber, bal.deposit, validto);
		xmlcert.setBaseUrl(Configuration.getPayUrl());
		xmlcert.sign(Configuration.getPrivateKey());
		return xmlcert;
	}

	/**
	 * generates a XMLAccountInfo certificate as response to a /balance request
	 */
	IXMLEncodable getBalance() throws Exception
	{
		long accountnumber = m_accountCertificate.getAccountNumber();
		Balance bal = m_Database.getBalance(accountnumber);
		XMLBalance xmlbal = new XMLBalance(
			accountnumber, bal.deposit, bal.spent,
			bal.timestamp, bal.validTime,
			Configuration.getPrivateKey()
			);
		XMLAccountInfo info = new XMLAccountInfo(xmlbal);

		for (Enumeration e = bal.confirms.elements(); e.hasMoreElements(); )
		{
			info.addCC(new XMLEasyCC( (String) e.nextElement()));
		}

		return info;
	}

	/**
	 * Checks whether a certificate with this public key is contained in our DB.
	 * @todo real checking of signature and timestamp !!
	 */
	private boolean testCertificate(byte[] data) //throws Exception
	{
		JAPSignature sigTester = new JAPSignature();
		try
		{
			sigTester.initVerify(Configuration.getOwnCertificate().getPublicKey());
		}
		catch (InvalidKeyException ex)
		{
			LogHolder.log(LogLevel.ALERT, LogType.PAY,
						  "Internal Error in PICommandUser.testCertificate(): wrong own certificate!");
			LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, ex);
			return false;
		}
		if (!sigTester.verifyXML(new ByteArrayInputStream(data)))
		{
			LogHolder.log(LogLevel.INFO, LogType.PAY, "Wrong certificate (invalid signature)");
			// error: certificate not contained in our DB
			return false;
		}
		XMLAccountCertificate xmlcert = null;
		try
		{
			xmlcert = new XMLAccountCertificate(new String(data));
		}
		catch (Exception ex1)
		{
			LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, ex1);
			return false;
		}
		m_accountCertificate = xmlcert;
		m_publicKey = xmlcert.getPublicKey();
		return true;
	}
}
