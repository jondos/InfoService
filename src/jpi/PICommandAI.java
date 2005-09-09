package jpi;

import java.io.*;
import java.util.*;
import javax.xml.parsers.*;

import org.w3c.dom.*;
import anon.crypto.*;
import anon.pay.xml.*;
import jpi.db.*;
import anon.util.IXMLEncodable;
import logging.*;
import anon.util.*;

/**
 * This class implements the high-level communication with the AI.
 * At the moment "settling" of costconfirmations is only possible.
 *
 * @author Bastian Voigt
 * @version 1.0
 * @todo implement challenge-response
 */
public class PICommandAI implements PICommand
{
	private int m_iState;

	/** takes the challenge we sent to the AI in order to check it later */
	private byte[] m_arbChallenge;

	/** @todo use one system-wide random source */
	private Random m_SecureRandom;

	/** interface to the database */
	private DBInterface m_Database;

	/** the name of the AI we are talking to */
	//private String m_aiName;

	static final int INIT = 0;
	static final int CHALLENGE_SENT = 1;
	static final int AUTHENTICATION_OK = 2;
	static final int AUTHENTICATION_BAD = 3;

	/**
	 * Erzeugt und initialisiert ein {@link PICommandAI} Objekt.
	 */
	public PICommandAI()
	{
		init();
		m_SecureRandom = new Random(System.currentTimeMillis());
	}

	void init()
	{
		m_iState = INIT;
		m_arbChallenge = null;
	}

	public PIAnswer next(PIRequest request)
	{
		PIAnswer reply = null;
		try
		{
			m_Database = DBSupplier.getDataBase();
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.ALERT, LogType.PAY, "settle(): could not get Database connection!");
			return PIAnswer.getErrorAnswer(XMLErrorMessage.ERR_INTERNAL_SERVER_ERROR);
		}

		if (request.method.equals("GET") &&
			request.url.equals("/close"))
		{
			return new PIAnswer(PIAnswer.TYPE_CLOSE, new XMLCloseAck());
		}

		switch (m_iState)
		{
			case INIT:
/*				// authentication for a known account
				if (request.method.equals("POST") && request.url.equals("/authenticate"))
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
						LogHolder.log(LogLevel.EXCEPTION, LogType.PAY,e);
						reply = PIAnswer.getErrorAnswer(XMLErrorMessage.ERR_INTERNAL_SERVER_ERROR);
					}
				}
				break;

			case CHALLENGE_SENT:
				break;

			case AUTHENTICATION_BAD:
				reply = new PIAnswer(PIAnswer.TYPE_CLOSE, new XMLErrorMessage(XMLErrorMessage.ERR_BAD_REQUEST));
				break;

			case AUTHENTICATION_OK:*/
				if (request.method.equals("POST") && request.url.equals("/settle"))
				{
					LogHolder.log(LogLevel.DEBUG, LogType.MISC, "got settle request");
					reply = new PIAnswer(PIAnswer.TYPE_CLOSE, settle(request.data));
					break;
				}

				/*else if (request.method.equals("POST") &&
				 request.url.equals("/update"))
				   {
				 piAnswer = new PIAnswer(200,PIAnswer.TYPE_ACCOUNT_SNAPSHOT,
				 getAccountSnapshots(request.data));
				 break;
				   }*/

				/*					else if (request.method.equals("POST") &&
				   request.url.equals("/payoff"))
				  {
				   piAnswer = new PIAnswer(200,PIAnswer.TYPE_PAYOFF,
				   payoff(request.data));
				   break;
				  }
				 */
				/*					else if (request.method.equals("POST") &&
				   request.url.equals("/confirm"))
				  {
				   piAnswer = new PIAnswer(200,PIAnswer.TYPE_CONFIRM,
				   confirm(request.data));
				   break;
				  }
				 */
				reply = new PIAnswer(PIAnswer.TYPE_CLOSE,
									 new XMLErrorMessage(XMLErrorMessage.ERR_BAD_REQUEST,
					"I cannot understand your request"));
				break;

		}
		return reply;
	}

	/**
	 * Processes a collection of cost confirmations, stores them in the Database
	 * and updates the account credit for affected accounts.
	 *
	 * @param data byte[] the collection of CostConfirmations in XML format
	 * @return XMLErrorMessage an xml structure indicating success or failure
	 */
	private IXMLEncodable settle(byte[] data)
	{
		XMLEasyCC cc = null;
		try
		{
			cc = new XMLEasyCC(data);
			LogHolder.log(LogLevel.DEBUG, LogType.MISC, "Parsed incoming CC: "+XMLUtil.toString(XMLUtil.toXMLDocument(cc)));
		}
		catch (Exception ex)
		{
			LogHolder.log(LogLevel.EXCEPTION, LogType.PAY,ex);
			return new XMLErrorMessage(XMLErrorMessage.ERR_WRONG_FORMAT,
									   "could not parse CC:" + ex.getMessage());
		}
/*		if (! (cc.getAIName().equals(m_aiName)))
		{
			LogHolder.log(LogLevel.DEBUG, LogType.MISC, "settle request: wrong AI name '"+cc.getAIName()+"'");
			return new XMLErrorMessage(XMLErrorMessage.ERR_WRONG_DATA,
									   "CC for wrong AI '" + cc.getAIName() + "' found, '" + m_aiName +
									   "' was expected");
		}*/
		LogHolder.log(LogLevel.DEBUG, LogType.MISC, "settle request: AI name '"+cc.getAIName()+"'");

		// check CC signature
		XMLJapPublicKey keyParser = null;
		try
		{
			keyParser = new XMLJapPublicKey(m_Database.getXmlPublicKey(cc.getAccountNumber()));
		}
		catch (Exception ex2)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.MISC, "settle request: Could not parse key");
			LogHolder.log(LogLevel.EXCEPTION, LogType.PAY,ex2);
			return new XMLErrorMessage(XMLErrorMessage.ERR_KEY_NOT_FOUND,
									   "Could not find a key for account " + cc.getAccountNumber() +
									   " in my DB");
		}
		LogHolder.log(LogLevel.DEBUG, LogType.MISC, "Parsed Key: "+XMLUtil.toString(XMLUtil.toXMLDocument(keyParser.getPublicKey())));
		try
		{
			JAPSignature verifier = new JAPSignature();
			verifier.initVerify(keyParser.getPublicKey());
			if (!cc.verifySignature(verifier))
			{
				// sig was bad
				LogHolder.log(LogLevel.DEBUG, LogType.MISC, "settle request: Bad signature");
				return new XMLErrorMessage(XMLErrorMessage.ERR_BAD_SIGNATURE);
			}
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.ALERT, LogType.PAY,"settle(): Error while verifying signature");
			LogHolder.log(LogLevel.EXCEPTION, LogType.PAY,e);
			return new XMLErrorMessage(XMLErrorMessage.ERR_INTERNAL_SERVER_ERROR,
									   "Error while verifying signature");
		}

		try
		{
			XMLEasyCC oldCC = m_Database.getCC(cc.getAccountNumber(), cc.getAIName());
			LogHolder.log(LogLevel.DEBUG, LogType.MISC, "settle request: Now storing CC in database");
			if (oldCC == null)
			{
				m_Database.insertCC(cc);
			}
			else if (oldCC.getTransferredBytes() < cc.getTransferredBytes())
			{
				m_Database.updateCC(cc);
			}
			return new XMLErrorMessage(XMLErrorMessage.ERR_OK);
		}
		catch (Exception ex3)
		{
			LogHolder.log(LogLevel.EXCEPTION, LogType.PAY,ex3);
			return new XMLErrorMessage(XMLErrorMessage.ERR_INTERNAL_SERVER_ERROR);
		}
	}

	/*	private byte[] getAccountSnapshots(byte[] data) throws RequestException
	 { //TODO make it work again...
	  int begin;
	  int end;
	  int firstIndex;
	  int lastIndex;
	  int tmpIndex;
	  StringBuffer snapshots = new StringBuffer();

	  String balances = new String(data);
	  System.out.println("::" + balances);

	  if ( ( (begin = balances.indexOf("<Balances>")) == -1) ||
	   ( (end = balances.indexOf("</Balances>")) == -1) ||
	   (begin >= end))
	  {
	   throw new RequestException(409, XML_WRONG_CONFIRMATIONS);
	  }

	  firstIndex = begin + 10;
	  while (true)
	  {
	   if ( ( (tmpIndex = balances.indexOf(XMLBalance.docStartTag, firstIndex)) != -1) &&
	 ( (lastIndex = balances.indexOf(XMLBalance.docEndTag, tmpIndex)) != -1))
	   {
	 lastIndex = lastIndex + XMLBalance.docEndTag.length();
	 try
	 {
	  XMLBalance xmlbal = new XMLBalance(balances.substring(tmpIndex, lastIndex));
	  long accountNumber = xmlbal.getAccountNumber();

	  AccountSnapshot ksa = m_Database.getAccountSnapshot(accountNumber, m_aiName);

	  // create new XML account snapshot structure
	  XMLAccountSnapshot xmlksa =
	   new XMLAccountSnapshot(m_aiName,
	  accountNumber, ksa.creditMax,
	  ksa.credit, ksa.costs,
	  new java.sql.Timestamp(System.currentTimeMillis())
	  );

	  // sign it
	  Document dom = xmlksa.getDomDocument();
	  JAPSignature japSig = Configuration.getSigningInstance();
	  japSig.signXmlDoc(dom);
	  String signed = XMLUtil.XMLDocumentToString(dom);

	  // and add it to the answer
	  snapshots.append(signed);

	 }
	 catch (Exception e)
	 {

	 }
	 firstIndex = lastIndex;
	   }
	   else
	   {
	 break;
	   }
	  }

	  return (XML_HEAD + "<AccountSnapshots>" + snapshots.toString() +
	 "</AccountSnapshots>").getBytes();

	 return null;
	 }
	 */
	/*	private byte[] payoff(byte[] data) throws RequestException
	 {
	  int begin;
	  int end;
	  int firstIndex;
	  int lastIndex;
	  int tmpIndex;
	  long sum = 0;

	  StringBuffer answer = new StringBuffer();
	  String accounts = new String(data);
	  System.out.println("::" + accounts);

	  if ( ( (begin = accounts.indexOf("<AccountNumbers>")) == -1) ||
	   ( (end = accounts.indexOf("</AccountNumbers>")) == -1) ||
	   (begin >= end))
	  {
	   throw new RequestException(409, XML_WRONG_CONFIRMATIONS);
	  }

	  firstIndex = begin + 16;
	  while (true)
	  {
	   if ( ( (tmpIndex = accounts.indexOf("<AccountNumber>", firstIndex)) != -1) &&
	 ( (lastIndex = accounts.indexOf("</AccountNumber>", tmpIndex)) != -1))
	   {
	 firstIndex = tmpIndex + 15;
	 try
	 {
	  long accountNumber = Long.parseLong(accounts.substring(firstIndex, lastIndex));
	  long costs = m_Database.getCosts(accountNumber, m_aiName);
	  if (costs > 0)
	  {
	   long payCosts = m_Database.getPayCosts(accountNumber, m_aiName);
	   if (costs <= payCosts)
	   {
	 continue;
	   }
	   sum = sum + costs - payCosts;
	  }
	 }
	 catch (Exception e)
	 {

	 }
	 firstIndex = lastIndex;
	   }
	   else
	   {
	 break;
	   }
	  }

	  return (XML_HEAD + "<Amount>" + sum + "</Amount>").getBytes();
	 }
	 */
	/*	private byte[] confirm(byte[] data)
	 {
	  return null;
	 }

	 private int countTicks(byte[] begin, byte[] last) throws RequestException
	 {
	  int maxTickCounter = 1000;
	  MessageDigest md;
	  try
	  {
	   md = MessageDigest.getInstance("SHA");
	  }
	  catch (Exception e)
	  {
	   throw new RequestException(500);
	  }
	  int tickCounter;
	  byte[] tmp;
	  for (tickCounter = 0; tickCounter < maxTickCounter + 1; tickCounter++)
	  {
	   tmp = md.digest(begin);
	   if (Arrays.equals(tmp, last))
	   {
	 break;
	   }
	   begin = tmp;
	  }
	  if (tickCounter == maxTickCounter)
	  {
	   return 0;
	  }
	  else
	  {
	   return tickCounter + 1;
	  }
	 }*/
}
