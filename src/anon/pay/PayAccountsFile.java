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

import java.util.Enumeration;
import java.util.Vector;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;
import anon.crypto.AsymmetricCryptoKeyPair;
import anon.crypto.JAPCertificate;
import anon.infoservice.ImmutableProxyInterface;
import anon.infoservice.InfoServiceHolder;
import anon.infoservice.ListenerInterface;
import anon.pay.xml.XMLAccountCertificate;
import anon.pay.xml.XMLErrorMessage;
import anon.pay.xml.XMLJapPublicKey;
import anon.util.IMiscPasswordReader;
import anon.util.IXMLEncodable;
import anon.util.XMLUtil;
import anon.util.captcha.ICaptchaSender;
import anon.util.captcha.IImageEncodedCaptcha;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import anon.infoservice.IMutableProxyInterface;
import anon.infoservice.PaymentInstanceDBEntry;

/**
 * This class encapsulates a collection of accounts. One of the accounts in the collection
 * is always active, except when the collection is empty.
 *
 * GUI classes can register a IPaymentListener with this class to be informed about all
 * payment specific events.
 *
 * The class can be initialized from an
 * XML structure and can also save all internal information in an XML structure before
 * shutdown.
 * For saving the accounts information, the following XML structure is used:
 * <pre>
 * &lt;PayAccountsFile version="1.0"&gt;
 *    &lt;MainAccountNumber&gt;123465&lt;/MainAccountNumber&gt;
 *    &lt;Accounts&gt;
 *       &lt;Account version="1.0"&gt;
 * 		    &lt;AccountCertificate&gt;...&lt;/AccountCertificate&gt; // Kontozertiufkat von der BI unterschrieben
 * 		    &lt;RSAPrivateKey&gt;...&lt;/RSAPrivateKey&gt; //der geheime Schl?ssel zum Zugriff auf das Konto
 * 		    &lt;TransferCertificates&gt; //offenen Transaktionsummern
 * 			    ....
 * 		    &lt;/TransferCertifcates&gt;
 * 		    &lt;AccountInfo&gt;...&lt;/AccountInfo&gt; //Kontostand (siehe XMLAccountInfo)
 *       &lt;/Account&gt;
 *        .
 *        .
 *        .
 *    &lt;/Accounts&gt;
 * &lt;/PayAccountsFile&gt;
 * </pre>
 *
 * @author Bastian Voigt, Tobias Bayer
 * @version 1.0
 */
public class PayAccountsFile implements IXMLEncodable, IBIConnectionListener
{
	public static final String XML_ELEMENT_NAME = "PayAccountsFile";

	private static final String XML_ATTR_IGNORE_AI_ERRORS = "ignoreAIErrors";
	private static final String XML_ATTR_ENABLE_BALANCE_AUTO_UPDATE = "enableBalanceAutoUpdate";

	private boolean m_bIsInitialized = false;
	private boolean m_bIgnoreAIAccountErrorMessages = false;
	private boolean m_bEnableBalanceAutoUpdate = true;


	/** contains a vector of PayAccount objects, one for each account */
	private Vector m_Accounts = new Vector();

	/** the active account */
	private PayAccount m_ActiveAccount = null;

	/** the one and only accountsfile */
	private static PayAccountsFile ms_AccountsFile = null;

	private Vector m_paymentListeners = new Vector();
	private Vector m_knownPIs = new Vector();

	private MyAccountListener m_MyAccountListener = new MyAccountListener();

	/**
	 * At this time, the implementation supports only one single BI. In the future
	 * a feature should be added to have support for multiple BIs, so that the
	 * AccountCertificate also contains a BIName. The infoservice should then
	 * publish information about the known BIs and also which MixCascade works
	 * with which BI.
	 *
	 * However, at the moment there is only one static BI which is used for all
	 * cascades and all accounts. This is the reason why we have this field in
	 * the singleton class.
	 */
	//private BI m_theBI;

	// singleton!
	private PayAccountsFile()
	{

	}

	/**
	 * returns the one and only accountsfile.
	 * Note: If {@link init(BI, Element)} was not yet called,
	 * you get an empty instance which is not really useful.
	 */
	public static PayAccountsFile getInstance()
	{
		if (ms_AccountsFile == null)
		{
			ms_AccountsFile = new PayAccountsFile();
		}
		return ms_AccountsFile;
	}

	/**
	 * Defined if error messages from the AI should be ignored. The connection mightbe closed, but the
	 * listeners will not get informed about the problem.
	 * @param a_bIgnore boolean
	 */
	public void setIgnoreAIAccountError(boolean a_bIgnore)
	{
		m_bIgnoreAIAccountErrorMessages = a_bIgnore;
	}

	/**
	 * Returns if account balances are automatically updated.
	 * @return if account balances are automatically updated
	 */
	public boolean isBalanceAutoUpdateEnabled()
	{
		return m_bEnableBalanceAutoUpdate;
	}

	public void setBalanceAutoUpdateEnabled(boolean a_bEnable)
	{
		m_bEnableBalanceAutoUpdate = a_bEnable;
	}

	/**
	 * Returns if error messages from the AI should be ignored.
	 * @return boolean
	 */
	public boolean isAIAccountErrorIgnored()
	{
		return m_bIgnoreAIAccountErrorMessages;
	}

	/**
	 * Performs the initialization.
	 * @param a_passwordReader  a password reader for encrypted account files; message: AccountNumber
	 * @return boolean succeeded?
	 */
	public static boolean init(Element elemAccountsFile, IMiscPasswordReader a_passwordReader)
	{
		if (ms_AccountsFile == null)
		{
			ms_AccountsFile = new PayAccountsFile();
		}
		//ms_AccountsFile.m_theBI = theBI;
		if (elemAccountsFile != null && elemAccountsFile.getNodeName().equals(XML_ELEMENT_NAME))
		{
			ms_AccountsFile.m_bIgnoreAIAccountErrorMessages =
				XMLUtil.parseAttribute(elemAccountsFile, XML_ATTR_IGNORE_AI_ERRORS, false);
			ms_AccountsFile.m_bEnableBalanceAutoUpdate =
				XMLUtil.parseAttribute(elemAccountsFile, XML_ATTR_ENABLE_BALANCE_AUTO_UPDATE, true);
			Element elemActiveAccount = (Element) XMLUtil.getFirstChildByName(elemAccountsFile,
				"ActiveAccountNumber");
			long activeAccountNumber = Long.parseLong(XMLUtil.parseValue(elemActiveAccount, "0"));

			Element elemAccounts = (Element) XMLUtil.getFirstChildByName(elemAccountsFile, "Accounts");
			Element elemAccount = (Element) elemAccounts.getFirstChild();
			while (elemAccount != null)
			{
				try
				{
					PayAccount theAccount = new PayAccount(elemAccount, a_passwordReader);
					ms_AccountsFile.addAccount(theAccount);
					elemAccount = (Element) elemAccount.getNextSibling();
				}
				catch (Exception e)
				{
					return false;
				}
			}

			// find activeAccount
			if (activeAccountNumber > 0)
			{
				Enumeration e = ms_AccountsFile.m_Accounts.elements();
				while (e.hasMoreElements())
				{
					PayAccount current = (PayAccount) e.nextElement();
					if (current.getAccountNumber() == activeAccountNumber)
					{
						try
						{
							ms_AccountsFile.setActiveAccount(current);
						}
						catch (Exception ex)
						{
						}
						break ;
					}
				}
			}
		}
		ms_AccountsFile.m_bIsInitialized = true;
		return true;
	}

	/**
	 * Thrown if an account with same account number was already existing when trying to add it.
	 */
	public static class AccountAlreadyExisting extends Exception
	{
	}

	/**
	 * constructs the xml structure
	 *
	 * @return Element
	 */
	public Element toXmlElement(Document a_doc)
	{
		return this.toXmlElement(a_doc, null);
	}

	public Element toXmlElement(Document a_doc, String a_password)
	{
		try
			{
				Element elemAccount;
				Element elemAccountsFile = a_doc.createElement(XML_ELEMENT_NAME);

				elemAccountsFile.setAttribute(XML_VERSION, "1.0");
				XMLUtil.setAttribute(elemAccountsFile, XML_ATTR_IGNORE_AI_ERRORS,
									 m_bIgnoreAIAccountErrorMessages);
				XMLUtil.setAttribute(elemAccountsFile, XML_ATTR_ENABLE_BALANCE_AUTO_UPDATE,
									 m_bEnableBalanceAutoUpdate);


				Element elem = a_doc.createElement("ActiveAccountNumber");
				XMLUtil.setValue(elem, Long.toString(getActiveAccountNumber()));
				elemAccountsFile.appendChild(elem);

				elem = a_doc.createElement("Accounts");
				elemAccountsFile.appendChild(elem);

				for (int i = 0; i < m_Accounts.size(); i++)
				{
					PayAccount account = (PayAccount) m_Accounts.elementAt(i);
					elemAccount = account.toXmlElement(a_doc, a_password);
					elem.appendChild(elemAccount);
				}

				return elemAccountsFile;
			}
			catch (Exception ex)
			{
				LogHolder.log(LogLevel.EXCEPTION, LogType.PAY,
							  "Exception while creating PayAccountsFile XML: " + ex);
				return null;
			}
	}

	public boolean hasActiveAccount()
	{
		return m_ActiveAccount != null;
	}

	public PayAccount getActiveAccount()
	{
		return m_ActiveAccount;
	}

	public void setActiveAccount(long accountNumber)
	{
		setActiveAccount(getAccount(accountNumber));
	}

	public void setActiveAccount(PayAccount account)
	{
		if (account != null && account.getPrivateKey() != null)
		{
			m_ActiveAccount = account;
			synchronized (m_paymentListeners)
			{
				Enumeration enumListeners = m_paymentListeners.elements();
				while (enumListeners.hasMoreElements())
				{
					( (IPaymentListener) enumListeners.nextElement()).accountActivated(m_ActiveAccount);
				}
			}
		}
		else if (account == null)
		{
			m_ActiveAccount = null;
			synchronized (m_paymentListeners)
			{
				Enumeration enumListeners = m_paymentListeners.elements();
				while (enumListeners.hasMoreElements())
				{
					( (IPaymentListener) enumListeners.nextElement()).accountActivated(m_ActiveAccount);
				}
			}
		}
	}

	public long getActiveAccountNumber()
	{
		if (m_ActiveAccount != null)
		{
			return m_ActiveAccount.getAccountNumber();
		}
		else
		{
			return -1;
		}
	}

	/**
	 * Liefert PayAccount zur angegebenen Kontonummer.
	 *
	 * @param accountNumber Kontonummer
	 * @return {@link PayAccount} oder null, wenn kein Konto unter der angebenen
	 * Kontonummer vorhanden ist
	 */
	public PayAccount getAccount(long accountNumber)
	{
		PayAccount tmp;
		Enumeration enumer = m_Accounts.elements();
		while (enumer.hasMoreElements())
		{
			tmp = (PayAccount) enumer.nextElement();
			if (tmp.getAccountNumber() == accountNumber)
			{
				return tmp;
			}
		}
		return null;
	}

	/**
	 * Deletes the account from the accountsfile and saves the file to disk.
	 * If the deleted account was the active account, the first remaining account
	 * will become the active account.
	 *
	 * @param accountNumber account number
	 * @throws Exception Wenn ein Fehler bei Dateizugriff auftrat
	 */
	public void deleteAccount(long accountNumber)
	{
		PayAccount tmp = getAccount(accountNumber);
		if (tmp != null)
		{
			for (int i = 0; i < m_Accounts.size(); i++)
			{
				tmp = (PayAccount) m_Accounts.elementAt(i);
				if (tmp.getAccountNumber() == accountNumber)
				{
					m_Accounts.removeElementAt(i);
					if (m_ActiveAccount == tmp)
					{
						m_ActiveAccount = null;
						if (m_Accounts.size() > 0)
						{
							for (int j = 0; j < m_Accounts.size(); j++)
							{
								if (((PayAccount)m_Accounts.elementAt(j)).getPrivateKey() != null)
								{
									setActiveAccount((PayAccount)m_Accounts.elementAt(j));
								}
							}
						}
						else
						{
							setActiveAccount(null);
						}
					}
					break;
				}
			}
		}
		synchronized (m_paymentListeners)
		{
			Enumeration enumListeners = m_paymentListeners.elements();
			while (enumListeners.hasMoreElements())
			{
				( (IPaymentListener) enumListeners.nextElement()).accountRemoved(m_ActiveAccount);
			}
		}
	}

	/**
	 * Liefert alle Kontennummern der Kontendatei.
	 *
	 * @return Enumeration von Long-Werten
	 */
	/*	public Enumeration getAccountNumbers()
	 {
	  PayAccount tmpAccount;
	  Vector tmp = new Vector();
	  Enumeration enumer = m_Accounts.elements();
	  while (enumer.hasMoreElements())
	  {
	   tmpAccount = (PayAccount) enumer.nextElement();
	   tmp.addElement(new Long(tmpAccount.getAccountNumber()));
	  }
	  return tmp.elements();
	 }*/

	/**
	 * Returns an enumeration of all accounts
	 *
	 * @return Enumeration of {@link PayAccount}
	 */
	public Enumeration getAccounts()
	{
		return ((Vector)m_Accounts .clone()).elements();
	}

	/**
	 * Adds a new account
	 *
	 * @param account new account
	 * @throws Exception If the same account was already added
	 */
	public void addAccount(PayAccount account) throws Exception
	{
		PayAccount tmp;
		boolean activeChanged = false;
		Enumeration enumer = m_Accounts.elements();
		while (enumer.hasMoreElements())
		{
			tmp = (PayAccount) enumer.nextElement();
			if (tmp.getAccountNumber() == account.getAccountNumber())
			{
				throw new AccountAlreadyExisting();
			}
		}
		account.addAccountListener(m_MyAccountListener);
		m_Accounts.addElement(account);

		if (m_ActiveAccount == null && account.getPrivateKey() != null)
		{
			m_ActiveAccount = account;
			activeChanged = true;
		}

		// fire event
		synchronized (m_paymentListeners)
		{
			Enumeration enumListeners = m_paymentListeners.elements();
			IPaymentListener pl;
			while (enumListeners.hasMoreElements())
			{
				pl = (IPaymentListener) enumListeners.nextElement();
				pl.accountAdded(account);
				if (activeChanged == true)
				{
					pl.accountActivated(account);
				}
			}
			enumListeners = null;
			pl = null;
		}
	}

	/**
	 * getNumAccounts
	 *
	 * @return int
	 */
	public int getNumAccounts()
	{
		return m_Accounts.size();
	}

	/**
	 * getAccountAt
	 *
	 * @param rowIndex int
	 * @return PayAccount
	 */
	public PayAccount getAccountAt(int rowIndex)
	{
		return (PayAccount) m_Accounts.elementAt(rowIndex);
	}

	/**
	 * isInitialized
	 *
	 * @return boolean
	 */
	public boolean isInitialized()
	{
		return m_bIsInitialized;
	}

	public void addPaymentListener(IPaymentListener listener)
	{
		synchronized (m_paymentListeners)
		{
			if (listener != null)
			{
				m_paymentListeners.addElement(listener);
			}
		}
	}

	public void removePaymentListener(IPaymentListener a_listener)
	{
		synchronized (m_paymentListeners)
		{
			if (m_paymentListeners.contains(a_listener))
			{
				m_paymentListeners.removeElement(a_listener);
			}
		}
	}

	/**
	 * getBI
	 *
	 * @return BI
	 */
	/*	public BI getBI()
	{
		return m_theBI;
	 }*/

	/**
	 * Listens to changes
	 * inside the accounts and forwards the events to our paymentListeners
	 */
	private class MyAccountListener implements IAccountListener
	{
		/**
		 * accountChanged
		 *
		 * @param acc PayAccount
		 */
		public void accountChanged(PayAccount acc)
		{
			// fire event
			Enumeration enumListeners;
			//synchronized (m_paymentListeners) // deadly for jdk 1.1.8...
			{
				/*
				 *  Clone the vector and leave synchronisation block as otherwise there
				 * would be a deadlock with fireChangeEvent in PayAccount:
				 * PayAccount.m_accountListeners:Vector,
				 * PayAccountsFile.m_paymentListeners:Vector
				 */
				enumListeners = ((Vector)m_paymentListeners.clone()).elements();
			}
			while (enumListeners.hasMoreElements())
			{
				( (IPaymentListener) enumListeners.nextElement()).creditChanged(m_ActiveAccount);
			}
		}
	}

	/**
	 * Creates a new Account.
	 * Generates an RSA or DSA key pair and then registers a new account with the BI.
	 * This can take a while, so the user should be notified before calling this.
	 *
	 * At the moment, only DSA should be used, because RSA is not supported by the
	 * AI implementation
	 * @param a_keyPair RSA should not be used at the moment
	 *
	 */
	public PayAccount createAccount(BI a_bi, IMutableProxyInterface a_proxys,
									AsymmetricCryptoKeyPair a_keyPair) throws
		Exception
	{
		XMLJapPublicKey xmlKey = new XMLJapPublicKey(a_keyPair.getPublic());

		LogHolder.log(LogLevel.DEBUG, LogType.PAY,
					  "Attempting to create account at PI " + a_bi.getName());

		BIConnection biConn = new BIConnection(a_bi);
		biConn.addConnectionListener(this);
		biConn.connect(a_proxys);
		XMLAccountCertificate cert = biConn.register(xmlKey, a_keyPair.getPrivate());
		biConn.disconnect();

		//Add PI to the list of known PIs
		addKnownPI(a_bi);

		// add the new account to the accountsFile
		PayAccount newAccount = new PayAccount(cert, a_keyPair.getPrivate(), a_bi);
		addAccount(newAccount);
		return newAccount;
	}

	/**
	 * signalAccountRequest
	 */
	public void signalAccountRequest()
	{
		synchronized (m_paymentListeners)
		{
			Enumeration enumListeners = m_paymentListeners.elements();
			while (enumListeners.hasMoreElements())
			{
				( (IPaymentListener) enumListeners.nextElement()).accountCertRequested(false);
			}
		}
	}

	/**
	 * signalAccountError
	 *
	 * @param msg XMLErrorMessage
	 */
	public void signalAccountError(XMLErrorMessage msg)
	{
		if (m_bIgnoreAIAccountErrorMessages)
		{
			return;
		}

		synchronized (m_paymentListeners)
		{
			Enumeration enumListeners = m_paymentListeners.elements();
			while (enumListeners.hasMoreElements())
			{
				( (IPaymentListener) enumListeners.nextElement()).accountError(msg);
			}
		}
	}

	/**
	 * Gets the list of known Payment Instances
	 * @return Enumeration
	 */
	public Enumeration getKnownPIs()
	{
		return m_knownPIs.elements();
	}

	/**
	 * Adds a payment instance to the list of known payment instances
	 */
	public void addKnownPI(BI a_bi)
	{
		boolean exists = false;

		for (int i = 0; i < m_knownPIs.size(); i++)
		{
			if ( ( (BI) m_knownPIs.elementAt(i)).getID().equals(a_bi.getID()))
			{
				exists = true;
			}
		}
		if (!exists)
		{
			m_knownPIs.addElement(a_bi);
		}
	}

	/**
	 * Adds a payment instance to the list of known payment instances
	 */
	public void addKnownPI(Element a_elemPI)
	{
		Vector listeners = new Vector();
		ListenerInterface l = null;
		String biID, biName;
		JAPCertificate biCert;

		Element elem = (Element) XMLUtil.getFirstChildByName(a_elemPI, "BIID");
		biID = XMLUtil.parseValue(elem, "-1");

		elem = (Element) XMLUtil.getFirstChildByName(a_elemPI, "BIName");
		biName = XMLUtil.parseValue(elem, "-1");

		elem = (Element) XMLUtil.getFirstChildByName(a_elemPI, "ListenerInterfaces");
		if (elem != null)
		{
			NodeList listenerNodes = elem.getChildNodes();
			for (int i = 0; i < listenerNodes.getLength(); i++)
			{
				try
				{
					l = new ListenerInterface( (Element) listenerNodes.item(i));
				}
				catch (Exception e)
				{
					LogHolder.log(LogLevel.EXCEPTION, LogType.PAY,
								  "Could not create listener interface while adding to known PIs");
				}
				if (l != null)
				{
					listeners.addElement(l);
				}
			}
		}
		elem = (Element) XMLUtil.getFirstChildByName(a_elemPI, "TestCertificate");
		elem = (Element) XMLUtil.getFirstChildByName(elem, JAPCertificate.XML_ELEMENT_NAME);

		biCert = JAPCertificate.getInstance(elem);

		if (listeners.size() > 0)
		{
		BI bi = null;
		try
		{

				bi = new BI(biID, biName, listeners, biCert);
		}
		catch (Exception e)
		{
			LogHolder.log(LogLevel.EXCEPTION, LogType.PAY, "Cannot create PI: " + e.getMessage());
		}

		addKnownPI(bi);
		}
		else
		{
			LogHolder.log(LogLevel.ERR, LogType.PAY,
						  "Cannot create PI without listeners while adding known PIs ");
		}
	}

	public BI getBI(String a_piID) throws Exception
	{
		BI theBI = null;
		//First, try to get the BI from the Infoservice
		LogHolder.log(LogLevel.DEBUG, LogType.PAY, "Trying to get " + a_piID + " from InfoService");

		try
		{
			theBI = new BI(InfoServiceHolder.getInstance().getPaymentInstance(a_piID));
			LogHolder.log(LogLevel.DEBUG, LogType.PAY, "Got BI " + theBI.getID() + " from InfoService");
			return theBI;
		}
		catch (Exception e)
		{
			// ignore
		}

		//If no infoservice could give us information about the PI, get it from the list of known PIs
		LogHolder.log(LogLevel.DEBUG, LogType.PAY,
					  "Could not get " + a_piID + " from InfoService, trying config file");

		for (int i = 0; i < m_knownPIs.size(); i++)
		{
			BI possibleBI = (BI) m_knownPIs.elementAt(i);
			if (possibleBI.getID().equals(a_piID))
			{
				theBI = possibleBI;
			}
		}
		if (theBI == null)
		{
			throw new Exception("Cannot get payment instance neither from InfoService nor from config file");
		}

		return theBI;
	}

	/**
	 * This method is called whenever a captcha has been received from the
	 * Payment Instance.
	 * @param a_source Object
	 * @param a_captcha IImageEncodedCaptcha
	 */
	public void gotCaptcha(ICaptchaSender a_source, final IImageEncodedCaptcha a_captcha)
	{
		synchronized (m_paymentListeners)
		{
			Enumeration enumListeners = m_paymentListeners.elements();
			while (enumListeners.hasMoreElements())
			{
				( (IPaymentListener) enumListeners.nextElement()).gotCaptcha(a_source, a_captcha);
			}
		}

	}
}
