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
import anon.util.XMLUtil;
import anon.util.IXMLEncodable;
//import javax.swing.event.ChangeListener;
//import javax.swing.event.ChangeEvent;

/**
 * This class contains the accounts configuration .
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
public class PayAccountsFile implements IXMLEncodable
{
	private boolean m_bIsInitialized = false;

	/** contains a vector of PayAccount objects, one for each account */
	protected Vector m_Accounts = new Vector();

	/** the active account */
	protected PayAccount m_ActiveAccount = null;

	/** the one and only accountsfile */
	private static PayAccountsFile ms_AccountsFile = null;

	private Vector m_changeListeners = new Vector();

	// singleton!
	private PayAccountsFile()
	{
	}

	/**
	 * returns the one and only accountsfile.
	 * If it was not yet initialized, null is returned.
	 */
	public static PayAccountsFile getInstance()
	{
		return ms_AccountsFile;

	}



	/**
	 * Performs the initialization. If necessary, the user will be asked for a
	 * password to decrypt the accounts data
	 *
	 * @return boolean succeeded?
	 * @todo cancel-Fall abfangen und vernuenftig behandeln (-> m_bIsEncrypted = true lassen und abbrechen)
	 */
	public static boolean init(Element elemAccountsFile)
	{
		ms_AccountsFile = new PayAccountsFile();
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
				ms_AccountsFile.m_Accounts.addElement(new PayAccount(elemAccount));
				elemAccount = (Element) elemAccount.getNextSibling();
			}
			catch (Exception ex1)
			{
				ex1.printStackTrace();
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
					ms_AccountsFile.m_ActiveAccount = current;
					//ms_AccountsFile.fireChangeEvent(ms_AccountsFile.m_ActiveAccount);
					break;
				}
			}
		}
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
	 * @return Element
	 */
	public Element toXmlElement(Document a_doc)
	{
//		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		Element elemRoot = a_doc.createElement("Root");
		elemRoot.setAttribute("filetype", "JapAccountsFile");
		elemRoot.setAttribute("version", "1.0");

		Element elemAccountsFile = a_doc.createElement("PayAccountsFile");
		elemAccountsFile.setAttribute("version", "1.0");
		elemRoot.appendChild(elemAccountsFile);

		Element elem = a_doc.createElement("ActiveAccountNumber");
		XMLUtil.setNodeValue(elem, Long.toString(getActiveAccountNumber()));
		elemAccountsFile.appendChild(elem);

		elem = a_doc.createElement("Accounts");
		elemAccountsFile.appendChild(elem);
		for (int i = 0; i < m_Accounts.size(); i++)
		{
			PayAccount account = (PayAccount) m_Accounts.elementAt(i);
			elem.appendChild(account.toXmlElement(a_doc));
		}
		return elemRoot;
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
			//fireChangeEvent(m_ActiveAccount);
		}
	}

	public void setActiveAccount(PayAccount account) throws Exception
	{
		if (account != null)
		{
			m_ActiveAccount = account;
			//fireChangeEvent(m_ActiveAccount);
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
		//fireChangeEvent(tmp);
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
		//account.addChangeListener(m_MyChangeListener);
		if (m_ActiveAccount == null)
		{
			m_ActiveAccount = account;
		}
		//fireChangeEvent(account);
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



/*	public void addChangeListener(ChangeListener listener)
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
			Enumeration enumListeners = m_changeListeners.elements();
			ChangeEvent e = new ChangeEvent(source);
			while (enumListeners.hasMoreElements())
			{
				( (ChangeListener) enumListeners.nextElement()).stateChanged(e);
			}
		}
	}
*/
	//private MyChangeListener m_MyChangeListener = new MyChangeListener();

	/**
	 * Listens to changes
	 * inside the accounts and forwards the events to our changeListeners
	 */
/*	private class MyChangeListener implements ChangeListener
	{
		public void stateChanged(ChangeEvent e)
		{
			fireChangeEvent(e.getSource());
		}
	}*/
}
