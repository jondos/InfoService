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
import logging.LogHolder;
import logging.LogLevel;
import logging.LogType;
import pay.util.PayText;
import org.w3c.dom.*;
import javax.xml.parsers.*;
import anon.util.*;
import java.io.*;
import jap.JAPModel;

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
 * @todo implement encryption and decryption
 * @author Bastian Voigt
 * @version 1.0
 */
public class PayAccountsFile
{
	/** contains a vector of PayAccount objects, one for each account */
	protected Vector m_Accounts;

	/** the active account */
	protected PayAccount m_ActiveAccount;

	/** the one and only accountsfile */
	private static PayAccountsFile ms_AccountsFile;

	// singleton!
	private PayAccountsFile()
	{
		m_Accounts = new Vector();
		m_ActiveAccount = null;
	}

	/**
	 * returns the one and only accountsfile.
	 * If no instance was created yet, an instance will be created and the
	 * file be read from disk.
	 */
	public static PayAccountsFile getInstance()
	{
		if (ms_AccountsFile == null)
		{
			ms_AccountsFile = new PayAccountsFile();
			try
			{
				// TODO: implement encryption
				ms_AccountsFile.readFromFile(JAPModel.getPayAccountsFileName(),
											 false, null);
			}
			catch (Exception e) // if the file could not be read
			{
				LogHolder.log(LogLevel.DEBUG, LogType.PAY,
							  "Could not read accountFile.. perhaps it does not yet exist.. trying to save:"
							  );
				try
				{
					ms_AccountsFile.save();
				}
				catch (Exception ex)
				{
					LogHolder.log(LogLevel.DEBUG, LogType.PAY, "Could not save file!");
					ex.printStackTrace();
				}
			}
		}
		return ms_AccountsFile;
	}

	/**
	 * Reads the accountsfile from disk.
	 * @param fileName String
	 * @param isEncrypted boolean
	 * @param password String can be null if not encrypted
	 */
	public void readFromFile(String fileName, boolean isEncrypted, String password) throws Exception
	{
		LogHolder.log(LogLevel.DEBUG, LogType.PAY,
					  "pay.PayAccountsFile.readFromFile: Reading PayAccounts from file "+fileName);

		// read file from disk
		File f = new File(fileName);
		int l = (int) f.length();
		byte[] xmlByteArray = new byte[l];
		FileInputStream inStream = new FileInputStream(f);
		inStream.read(xmlByteArray);
		inStream.close();

		// decrypt the ByteArray using the supplied password
		if (isEncrypted)
		{
			if (password == null)
			{
				throw new Exception("Cannot encrypt with null password");
			}
			// TODO: Implement decryption
		}

		// construct Dom Document
		ByteArrayInputStream bai = new ByteArrayInputStream(xmlByteArray);
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(bai);
		Element elemRoot = doc.getDocumentElement();

		// finally set values
		Element elemActiveAccount = (Element) XMLUtil.getFirstChildByName(elemRoot, "ActiveAccountNumber");
		long activeAccountNumber = Long.parseLong(XMLUtil.parseNodeString(elemActiveAccount, "0"));

		Element elemAccounts = (Element) XMLUtil.getFirstChildByName(elemRoot, "Accounts");
		Element elemAccount = (Element) elemAccounts.getFirstChild();
		while (elemAccount != null)
		{
			m_Accounts.add(new PayAccount(elemAccounts));
		}

		// find activeAccount
		if (activeAccountNumber != 0)
		{
			Enumeration e = m_Accounts.elements();
			while (e.hasMoreElements())
			{
				PayAccount current = (PayAccount) e.nextElement();
				if (current.getAccountNumber() == activeAccountNumber)
				{
					m_ActiveAccount = current;
					break;
				}
			}
		}
	}

	public void save() throws Exception
	{
		// TODO: Implement encryption
		String fileName = JAPModel.getPayAccountsFileName();
		LogHolder.log(LogLevel.DEBUG, LogType.PAY, "Got accountsfilename from JapModel: " + fileName);
		saveToFile(fileName, false, null);
	}

	/**
	 * Save the accountfile to disk
	 *
	 * @param fileName String
	 * @param saveEncrypted boolean if true, file will be encrypted
	 * @param password String can be null if not encrypted
	 * @throws Exception
	 */
	public void saveToFile(String fileName, boolean saveEncrypted, String password) throws Exception
	{
		LogHolder.log(LogLevel.DEBUG, LogType.PAY, "Saving accountsfile to " + fileName + "...");
		// construct xml document
		Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
		Element elemRoot = doc.createElement("PayAccountsFile");
		doc.appendChild(elemRoot);
		elemRoot.setAttribute("version", "1.0");

		Element elem = doc.createElement("ActiveAccountNumber");
		XMLUtil.setNodeValue(elem, Long.toString(getActiveAccountNumber()));
		elemRoot.appendChild(elem);

		elem = doc.createElement("Accounts");
		elemRoot.appendChild(elem);
		for (int i = 0; i < m_Accounts.size(); i++)
		{
			PayAccount account = (PayAccount) m_Accounts.elementAt(i);
			Node n = XMLUtil.importNode(doc, account.getDomDocument().getDocumentElement(), true);
			elem.appendChild(n);
		}

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
		m_ActiveAccount = getAccount(accountNumber);
		save();
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
		Enumeration enum = m_Accounts.elements();
		while (enum.hasMoreElements())
		{
			tmp = (PayAccount) enum.nextElement();
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
	public boolean deleteAccount(long accountNumber) throws Exception
	{

		PayAccount tmp = getAccount(accountNumber);
		if (tmp.getCredit() > 0)
		{
			return false;
		}
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
				}
			}
		}
		save();
		return true;
	}

	/**
	 * Liefert alle Kontennummern der Kontendatei.
	 *
	 * @return Enumeration von Long-Werten
	 */
	public Enumeration getAccountNumbers()
	{
		PayAccount tmpAccount;
		Vector tmp = new Vector();
		Enumeration enum = m_Accounts.elements();
		while (enum.hasMoreElements())
		{
			tmpAccount = (PayAccount) enum.nextElement();
			tmp.addElement(new Long(tmpAccount.getAccountNumber()));
		}
		return tmp.elements();
	}

	/**
	 * Liefert alle Konten der Kontendatei.
	 *
	 * @return Enumeration von {@link PayAccount}s
	 */
	public Enumeration getAccounts()
	{
		return m_Accounts.elements();
	}

	/**
	 * Schreibt ein neues Konto in die Kontendatei.
	 *
	 * @param account zu speicherndes Konto
	 * @throws Exception Wenn ein Konto unter der Kontonummer schon in der
	 * Kontendatei enthalten ist. Wenn ein Dateizugriffsfehler auftrat.
	 */
	public void addAccount(PayAccount account) throws Exception
	{
		PayAccount tmp;
		Enumeration enum = m_Accounts.elements();
		while (enum.hasMoreElements())
		{
			tmp = (PayAccount) enum.nextElement();
			if (tmp.getAccountNumber() == account.getAccountNumber())
			{
				throw new IllegalArgumentException();
			}
		}
		m_Accounts.addElement(account);
		if (m_ActiveAccount == null)
		{
			m_ActiveAccount = account;
		}
		save();
	}
}
