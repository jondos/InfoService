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

import java.util.Enumeration;
import java.util.Vector;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.swing.JOptionPane;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import anon.crypto.XMLEncryption;
import anon.util.XMLUtil;
import jap.JAPController;
import jap.AbstractJAPMainView;
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;

/**
 * This class contains the accounts configuration and the functionality to read
 * and write the configuration file. Because the configuration file contains
 * sensitive information such as the private keys for each account, the file can be
 * encrypted. For that purpose the CryptFile class is used.
 *
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
 * @author Bastian Voigt
 * @version 1.0
 */
public class PayAccountsFile
{
	private String m_Password = null;
	private boolean m_bIsInitialized = false;
	private boolean m_bIsEncrypted = false;
	private boolean m_bWasEncrypted = false;
	private boolean m_bFirstTime = false;
	private Document m_TheDocument = null;

	/** contains a vector of PayAccount objects, one for each account */
	protected Vector m_Accounts = new Vector();

	/** the active account */
	protected PayAccount m_ActiveAccount = null;

	/** the one and only accountsfile */
	private static PayAccountsFile ms_AccountsFile;
	private String m_strWasPassword;

	// singleton!
	private PayAccountsFile()
	{
	}

	/**
	 * returns the one and only accountsfile.
	 * If no instance was created yet, an instance will be created and the
	 * file be read from disk.
	 */
	public static PayAccountsFile getInstance()
	{
		LogHolder.log(LogLevel.DEBUG, LogType.PAY, "PayAccountsFile.getInstance()");
		if (ms_AccountsFile == null)
		{
			ms_AccountsFile = new PayAccountsFile();
		}
		else
		{
			if (ms_AccountsFile.m_bIsInitialized)
			{
				if (ms_AccountsFile.m_bIsEncrypted)
				{
					LogHolder.log(LogLevel.DEBUG, LogType.PAY,
								  "PayAccountsFile: isEncrypted.. calling doInit()");
					ms_AccountsFile.doInit();
				}
			}
			else
			{
				ms_AccountsFile.m_bFirstTime = true;
				ms_AccountsFile.m_bIsInitialized = true;
				ms_AccountsFile.m_bIsEncrypted = false;
			}
		}
		return ms_AccountsFile;
	}

	/**
	 * Initializes the accountsFile with encrypted XML data from the config
	 * file. The user will be asked for a password for decryption later
	 * on demand, because at the time this is called by JAPController, the
	 * main window does not exist.
	 *
	 * @param elemCrypt Element encrypted Accounts structure from configfile
	 * @return boolean succeeded?
	 */
	public boolean initEncrypted(Element elemCrypt)
	{
		if (m_bIsInitialized)
		{
			return false;
		}
		try
		{
			m_TheDocument = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
			Element elemRoot = m_TheDocument.createElement("Root");
			m_TheDocument.appendChild(elemRoot);
			Element myCrypt = (Element) XMLUtil.importNode(m_TheDocument, elemCrypt, true);
			elemRoot.appendChild(myCrypt);
		}
		catch (Exception ex)
		{
			return false;
		}
		m_bIsEncrypted = true;
		m_bIsInitialized = true;
		m_bFirstTime = false;
		return true;
	}

	/**
	 * Initializes the accountsFile with plaintext xml data
	 *
	 * @param elemPlain Element
	 * @return boolean succeeded?
	 */
	public boolean initPlaintext(Element elemPlain)
	{
		if (m_bIsInitialized)
		{
			return false;
		}
		try
		{
			m_TheDocument = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
			Element elemRoot = m_TheDocument.createElement("Root");
			m_TheDocument.appendChild(elemRoot);
			Element myCrypt = (Element) XMLUtil.importNode(m_TheDocument, elemPlain, true);
			elemRoot.appendChild(myCrypt);
		}
		catch (Exception ex)
		{
			return false;
		}
		doInit();
		m_bIsEncrypted = false;
		m_bIsInitialized = true;
		m_bFirstTime = false;
		return true;
	}

	/**
	 * Performs the initialization. If necessary, the user will be asked for a
	 * password to decrypt the accounts data
	 *
	 * @return boolean succeeded?
	 * @todo cancel-Fall abfangen und vernuenftig behandeln (-> m_bIsEncrypted = true lassen und abbrechen)
	 */
	private boolean doInit()
	{
		LogHolder.log(LogLevel.DEBUG, LogType.PAY, "PayAccountsFile: doInit()");
		Element elemRoot = m_TheDocument.getDocumentElement();
		Element elemAccountsFile = null;

		// decrypt if necessary
		if (m_bIsEncrypted)
		{
			AbstractJAPMainView mainView = JAPController.getView();
			String strPassword = "";
			String strMessage = "<html>Bitte geben Sie das Passwort f&uuml;r die<br>Entschl&uuml;sselung der Kontendatei ein:</html>";
			Element elemCrypt = (Element) XMLUtil.getFirstChildByName(elemRoot, "EncryptedData");

			while (true)
			{
				// ask for password
				strPassword = JOptionPane.showInputDialog(
					mainView, strMessage,
					"JAP Bezahlsystem",
					JOptionPane.QUESTION_MESSAGE | JOptionPane.OK_CANCEL_OPTION
					);

				try
				{
					elemAccountsFile = XMLEncryption.decryptElement(elemCrypt, strPassword);
				}
				catch (Exception ex)
				{
					strMessage = "Falsches Passwort. Bitte geben Sie das Passwort " +
						"f&uuml;r die Entschl&uuml;sselung ein";
					continue;
				}
				m_strWasPassword = strPassword;
				break;
			}
			m_bIsEncrypted = false;
			m_bWasEncrypted = true;
		}
		else
		{
			elemAccountsFile = (Element) XMLUtil.getFirstChildByName(elemRoot, "PayAccountsFile");
		}

		// set values
		Element elemActiveAccount = (Element) XMLUtil.getFirstChildByName(elemAccountsFile,
			"ActiveAccountNumber");
		long activeAccountNumber = Long.parseLong(XMLUtil.parseNodeString(elemActiveAccount, "0"));

		Element elemAccounts = (Element) XMLUtil.getFirstChildByName(elemAccountsFile, "Accounts");
		Element elemAccount = (Element) elemAccounts.getFirstChild();
		while (elemAccount != null)
		{
			try
			{
				m_Accounts.addElement(new PayAccount(elemAccount));
				elemAccount = (Element) elemAccount.getNextSibling();
			}
			catch (Exception ex1)
			{
				LogHolder.log(LogLevel.ERR, LogType.PAY, "Could not read accounts data: " + ex1.toString());
				ex1.printStackTrace();
				return false;
			}
		}

		// find activeAccount
		if (activeAccountNumber > 0)
		{
			Enumeration e = m_Accounts.elements();
			while (e.hasMoreElements())
			{
				PayAccount current = (PayAccount) e.nextElement();
				if (current.getAccountNumber() == activeAccountNumber)
				{
					m_ActiveAccount = current;
					fireChangeEvent(m_ActiveAccount);
					break;
				}
			}
		}
		m_TheDocument = null;
		return true;
	}

	/**
	 * Reads the accountsfile from disk. (Import Function)
	 * @param fileName String
	 */
	/*	public void readFromFile(String fileName) throws Exception
	 {
	  LogHolder.log(LogLevel.DEBUG, LogType.PAY,
		"pay.PayAccountsFile.readFromFile: Reading PayAccounts from file " + fileName);
	  // delete old accountdata
	  if (m_bIsInitialized)
	  {
	   m_Accounts.removeAllElements();
	   m_ActiveAccount = null;
	  }

	  // read file from disk
	  File f = new File(fileName);
	  FileInputStream inStream = new FileInputStream(f);
	  m_TheDocument = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inStream);
	  inStream.close();
	  m_bIsInitialized = true;

	  Element elemRoot = m_TheDocument.getDocumentElement();
	  Element elemAccountsFile = (Element) XMLUtil.getFirstChildByName(elemRoot, "PayAccountsFile");
	  if (elemAccountsFile == null)
	  {
	   elemAccountsFile = (Element) XMLUtil.getFirstChildByName(elemRoot, "EncryptedData");
	   if (elemAccountsFile == null)
	   {
	 throw new Exception("Wrong XML Format");
	   }
	   m_bIsEncrypted = true;
	  }
	  else
	  {
	   m_bIsEncrypted = false;
	  }

	  m_bFirstTime = true;
	  doInit();
	 }
	 */
	/**
	 * Save the accountfile to disk (export function)
	 *
	 * @param fileName String
	 * @param saveEncrypted boolean if true, file will be encrypted
	 * @param password String can be null if not encrypted
	 * @throws Exception
	 */
	/*	public void saveToFile(String fileName, boolean saveEncrypted, String password) throws Exception
	 {
	  LogHolder.log(LogLevel.DEBUG, LogType.PAY, "Saving accountsfile to " + fileName + "...");
	  Document doc = constructXmlDocument(saveEncrypted, password);

	  // convert it to a bytearray
	  byte[] xmlByteArray = XMLUtil.XMLDocumentToString(doc).getBytes();

	  // encrypt the ByteArray using the supplied password
	  if (saveEncrypted)
	  {
	   if (password == null)
	   {
	 throw new Exception("Cannot encrypt with null password");
	   }
	   // TODO: Implement encryption
	  }

	  // and finally save to disk
	  FileOutputStream outStream = new FileOutputStream(fileName);
	  outStream.write(xmlByteArray);
	  outStream.flush();
	  outStream.close();
	  LogHolder.log(LogLevel.DEBUG, LogType.PAY, "Saving Was successful");
	 }*/

	/**
	 * constructs the xml structure
	 *
	 * @param encrypt boolean if true it will be encrypted
	 * @param password String password for encrypting
	 * @throws Exception
	 * @return Document
	 */
	private Document constructXmlDocument(boolean encrypt, String password) throws Exception
	{
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		Element elemRoot = doc.createElement("Root");
		elemRoot.setAttribute("filetype", "JapAccountsFile");
		elemRoot.setAttribute("version", "1.0");
		doc.appendChild(elemRoot);

		Element elemAccountsFile = doc.createElement("PayAccountsFile");
		elemAccountsFile.setAttribute("version", "1.0");
		elemRoot.appendChild(elemAccountsFile);

		Element elem = doc.createElement("ActiveAccountNumber");
		XMLUtil.setNodeValue(elem, Long.toString(getActiveAccountNumber()));
		elemAccountsFile.appendChild(elem);

		elem = doc.createElement("Accounts");
		elemAccountsFile.appendChild(elem);
		for (int i = 0; i < m_Accounts.size(); i++)
		{
			PayAccount account = (PayAccount) m_Accounts.elementAt(i);
			Node n = XMLUtil.importNode(doc, account.getDomDocument().getDocumentElement(), true);
			elem.appendChild(n);
		}

		if (encrypt)
		{
			XMLEncryption.encryptElement(elemAccountsFile, password);
		}
		return doc;
	}

	public boolean hasActiveAccount()
	{
		return m_ActiveAccount != null;
	}

	public PayAccount getActiveAccount()
	{
		return m_ActiveAccount;
	}

	public void setActiveAccount(long accountNumber) throws Exception
	{
		PayAccount account = getAccount(accountNumber);
		if (account != null)
		{
			m_ActiveAccount = account;
			fireChangeEvent(m_ActiveAccount);
		}
	}

	public void setActiveAccount(PayAccount account) throws Exception
	{
		if (account != null)
		{
			m_ActiveAccount = account;
			fireChangeEvent(m_ActiveAccount);
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
						if (m_Accounts.size() > 0)
						{
							m_ActiveAccount = (PayAccount) m_Accounts.firstElement();
						}
						else
						{
							m_ActiveAccount = null;
						}
					}
					break;
				}
			}
		}
		fireChangeEvent(tmp);
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
		return m_Accounts.elements();
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
		Enumeration enumer = m_Accounts.elements();
		while (enumer.hasMoreElements())
		{
			tmp = (PayAccount) enumer.nextElement();
			if (tmp.getAccountNumber() == account.getAccountNumber())
			{
				throw new Exception("Account with same accountnumber was already added");
			}
		}
		m_Accounts.addElement(account);
		account.addChangeListener(m_MyChangeListener);
		if (m_ActiveAccount == null)
		{
			m_ActiveAccount = account;
		}
		fireChangeEvent(account);
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

	/**
	 * getConfigurationData - returns the XML PayAccountsFile element
	 * so that it can be saved in the Jap.conf
	 *
	 * @return Element
	 */
	public Element getConfigurationData()
	{
		Document doc = null;
		if (m_bIsInitialized && m_bIsEncrypted)
		{
			doc = m_TheDocument;
		}
		else
		{
			String strPassword = "";
			boolean encrypt = false;

			// is it the first time we save the accountsdata? then we should
			// ask the user for a new password
			if (m_bFirstTime)
			{
				AbstractJAPMainView mainView = JAPController.getView();
				int choice = JOptionPane.showOptionDialog(
					mainView,
					"<html>Sie haben w&auml;hrend dieser JAP-Sitzung zum ersten Mal<br> " +
					"Konten angelegt. Zu jedem Konto geh&ouml;rt auch ein<br> " +
					"privater Schl&uuml;ssel, der sicher verwahrt werden muss.<br> " +
					"Sie haben deshalb jetzt die M&ouml;glichkeit, Ihre<br> " +
					"Kontendaten verschl&uuml;sselt zu speichern.<br> " +
					"Falls Ihre Kontendaten verschl&uuml;sselt sind,<br> " +
					"m&uuml;ssen Sie von nun an bei jedem JAP-Start das Passwort<br> " +
					"zum Entschl&uuml;sseln eingeben.<br><br>" +
					"M&ouml;chten Sie Ihre Kontendaten jetzt verschl&uuml;sseln?</html>",
					"Verschluesselung der Kontendaten",
					JOptionPane.YES_NO_OPTION,
					JOptionPane.QUESTION_MESSAGE,
					null, null, null
					);
				if (choice == JOptionPane.YES_OPTION)
				{
					encrypt = true;
					strPassword = JOptionPane.showInputDialog("Geben Sie ein Passwort ein:");
				}
			}

			// it is not the first time
			else
			{
				encrypt = m_bWasEncrypted;
				strPassword = m_strWasPassword;
			}

			// save it
			try
			{
				doc = constructXmlDocument(encrypt, strPassword);
			}
			catch (Exception ex)
			{
				LogHolder.log(LogLevel.ERR, LogType.PAY,
							  "Error constructing PayAccountsFile XML Document");
				return null;
			}
		}

		Element elemRoot = doc.getDocumentElement();
		Element elemConfig = (Element) XMLUtil.getFirstChildByName(elemRoot, "PayAccountsFile");
		if (elemConfig == null)
		{
			elemConfig = (Element) XMLUtil.getFirstChildByName(elemRoot, "EncryptedData");
		}
		return elemConfig;
	}

	private Vector m_changeListeners = new Vector();

	public void addChangeListener(ChangeListener listener)
	{
		synchronized (m_changeListeners)
		{
			if (listener != null)
			{
				m_changeListeners.addElement(listener);
			}
		}
	}

	private void fireChangeEvent(Object source)
	{
		synchronized (m_changeListeners)
		{
			LogHolder.log(LogLevel.DEBUG, LogType.PAY,
						  "PayAccountsFile: FireChangeEvent....................................");
			Enumeration enumListeners = m_changeListeners.elements();
			ChangeEvent e = new ChangeEvent(source);
			while (enumListeners.hasMoreElements())
			{
				( (ChangeListener) enumListeners.nextElement()).stateChanged(e);
			}
		}
	}

	private MyChangeListener m_MyChangeListener = new MyChangeListener();
	/**
	 * Listens to changes
	 * inside the accounts and forwards the events to our changeListeners
	 */
	private class MyChangeListener implements ChangeListener
	{
		public void stateChanged(ChangeEvent e)
		{
			fireChangeEvent(e.getSource());
		}
	}
}
